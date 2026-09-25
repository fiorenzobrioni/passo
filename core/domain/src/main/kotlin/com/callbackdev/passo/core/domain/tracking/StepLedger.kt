package com.callbackdev.passo.core.domain.tracking

import com.callbackdev.passo.core.domain.tracking.TrackingConstants.FLUSH_STEP_THRESHOLD
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.StepSample
import com.callbackdev.passo.core.model.SystemSnapshot
import com.callbackdev.passo.core.model.TrackerState

/**
 * What one write must persist, in one transaction (PLANNING.md §4.5): the minute increments,
 * the state they bring the counter to, and the log lines gathered meanwhile.
 *
 * @property state null only when no sample has been seen yet and the batch carries log lines.
 */
data class LedgerBatch(
    val increments: List<MinuteSteps>,
    val state: TrackerState?,
    val diagnostics: List<DiagnosticsEvent>,
)

/**
 * The service's in-memory buffer (PLANNING.md §4.5): runs each sample through the
 * [StepAccountant], keeps the increments per minute, and says when they must be written.
 *
 * Not thread-safe: the service drives it from one thread. The invariant it protects is that the
 * state handed out by [drain] is exactly the one the drained increments lead to, so a batch
 * that fails to write is [restore]d and nothing is lost or counted twice.
 */
class StepLedger(initialState: TrackerState?) {
    var state: TrackerState? = initialState
        private set

    private val pending = sortedMapOf<Long, MinuteSteps>()
    private val diagnostics = mutableListOf<DiagnosticsEvent>()
    private var stateDirty = false
    private var loggedTimestampFallback = false

    /** Steps accounted but not yet written. */
    val pendingSteps: Int
        get() = pending.values.sumOf { it.steps }

    /** Steps accounted but not yet written, for one local day. */
    fun pendingStepsOn(localEpochDay: Long): Int =
        pending.values.filter { it.localEpochDay == localEpochDay }.sumOf { it.steps }

    /** The buffered minutes of one local day, oldest first. */
    fun pendingOn(localEpochDay: Long): List<MinuteSteps> = pending.values.filter { it.localEpochDay == localEpochDay }

    /**
     * Accounts [sample]. Returns true when the buffer should be written now: the first
     * baseline, a new boot session or a counter reset (the state they establish is too
     * valuable to keep only in memory), an anomaly, [FLUSH_STEP_THRESHOLD] buffered steps, or
     * a minute boundary crossed while events are flowing.
     */
    fun record(sample: StepSample, snapshot: SystemSnapshot): Boolean {
        val accounting = StepAccountant.account(state, sample, snapshot)
        state = accounting.newState
        stateDirty = true
        accounting.increments.forEach(::add)
        log(accounting, sample, snapshot)

        val sampleMinute = Math.floorDiv(accounting.newState.lastSampleWallMillis, 60_000L)
        val minuteCrossed = pending.isNotEmpty() && pending.firstKey() < sampleMinute
        return accounting.change != SessionChange.SAME_SESSION ||
            accounting.cappedFromSteps != null ||
            pendingSteps >= FLUSH_STEP_THRESHOLD ||
            minuteCrossed
    }

    /** Adds a log line, written with the next batch. */
    fun note(event: DiagnosticsEvent) {
        diagnostics += event
    }

    /** Hands out everything not yet written and empties the buffer; null if there is nothing. */
    fun drain(): LedgerBatch? {
        if (pending.isEmpty() && diagnostics.isEmpty() && !stateDirty) return null
        val batch = LedgerBatch(
            increments = pending.values.toList(),
            state = state.takeIf { stateDirty },
            diagnostics = diagnostics.toList(),
        )
        pending.clear()
        diagnostics.clear()
        stateDirty = false
        return batch
    }

    /**
     * Puts back a batch that could not be written. The live state stays (it already includes
     * the batch's steps and anything recorded since); it is marked dirty so the next write
     * carries it again.
     */
    fun restore(batch: LedgerBatch) {
        batch.increments.forEach(::add)
        diagnostics.addAll(0, batch.diagnostics)
        if (batch.state != null) stateDirty = true
    }

    private fun add(increment: MinuteSteps) {
        val existing = pending[increment.epochMinute]
        pending[increment.epochMinute] = existing?.copy(steps = existing.steps + increment.steps) ?: increment
    }

    private fun log(accounting: Accounting, sample: StepSample, snapshot: SystemSnapshot) {
        val now = snapshot.wallClockMillis
        when (accounting.change) {
            SessionChange.FIRST_RUN ->
                note(DiagnosticsEvent(now, DiagnosticsType.BASELINE, "counter=${sample.counterValue}"))

            SessionChange.NEW_BOOT ->
                note(
                    DiagnosticsEvent(
                        now,
                        DiagnosticsType.BOOT,
                        "bootCount=${snapshot.bootCount} counter=${sample.counterValue} accepted=${accounting.acceptedSteps}",
                    ),
                )

            SessionChange.COUNTER_RESET ->
                note(
                    DiagnosticsEvent(
                        now,
                        DiagnosticsType.COUNTER_RESET,
                        "counter=${sample.counterValue} accepted=${accounting.acceptedSteps}",
                    ),
                )

            SessionChange.SAME_SESSION -> Unit
        }
        accounting.cappedFromSteps?.let { raw ->
            note(DiagnosticsEvent(now, DiagnosticsType.ANOMALY, "delta=$raw capped=${accounting.acceptedSteps}"))
        }
        // Once per run: a device with a broken timebase would otherwise fill the log.
        if (accounting.usedArrivalTime && !loggedTimestampFallback) {
            loggedTimestampFallback = true
            note(
                DiagnosticsEvent(
                    now,
                    DiagnosticsType.TIMESTAMP_FALLBACK,
                    "event=${sample.eventElapsedNanos} now=${snapshot.elapsedRealtimeNanos}",
                ),
            )
        }
    }
}
