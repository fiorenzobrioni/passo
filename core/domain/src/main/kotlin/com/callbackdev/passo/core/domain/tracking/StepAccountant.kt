package com.callbackdev.passo.core.domain.tracking

import com.callbackdev.passo.core.domain.tracking.TrackingConstants.DEFAULT_CADENCE
import com.callbackdev.passo.core.domain.tracking.TrackingConstants.JUMP_SLACK_STEPS
import com.callbackdev.passo.core.domain.tracking.TrackingConstants.MAX_CADENCE
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.StepSample
import com.callbackdev.passo.core.model.SystemSnapshot
import com.callbackdev.passo.core.model.TrackerState
import java.time.Instant
import java.time.ZoneId

/** How a sample relates to the stored state. */
enum class SessionChange {
    /** No state yet: the first sample ever becomes the baseline. */
    FIRST_RUN,

    /** Same boot session, the counter moved forward (or not at all). */
    SAME_SESSION,

    /** The device rebooted since the last sample: the counter restarted from zero. */
    NEW_BOOT,

    /** Same boot session, but the counter went backwards: the sensor was reset. */
    COUNTER_RESET,
}

/**
 * The result of accounting one sample.
 *
 * @property increments the steps to add, one entry per minute, oldest first. Their sum is the
 *   accepted delta.
 * @property cappedFromSteps the raw delta when it was implausible and got capped, else null.
 * @property usedArrivalTime true when the sensor's timestamp was rejected (in the future, or
 *   before the previous sample) and the arrival time stood in for it.
 */
data class Accounting(
    val increments: List<MinuteSteps>,
    val newState: TrackerState,
    val change: SessionChange,
    val acceptedSteps: Long,
    val cappedFromSteps: Long?,
    val usedArrivalTime: Boolean,
)

/**
 * Turns hardware counter readings into steps per minute (PLANNING.md §4.4). Pure: the clocks,
 * the time zone and the boot count come in through [SystemSnapshot].
 *
 * The counter is cumulative since boot, so every delta is computed against the stored
 * [TrackerState], never against the previous event in memory: a lost event, a dropped FIFO
 * entry or a crash before a write moves steps between minutes, but never changes a total.
 */
object StepAccountant {
    private const val NANOS_PER_MILLI = 1_000_000L
    private const val MILLIS_PER_MINUTE = 60_000L

    fun account(state: TrackerState?, sample: StepSample, snapshot: SystemSnapshot): Accounting {
        val change = changeOf(state, sample, snapshot)
        val sameSession = change == SessionChange.SAME_SESSION || change == SessionChange.COUNTER_RESET

        // Everything below is measured on the monotonic clock. The earliest a sample can have
        // happened is the previous sample (same session) or the boot (new session).
        val lowerElapsed = if (sameSession && state != null) state.lastSampleElapsedNanos else 0L
        val timestampPlausible = sample.eventElapsedNanos in lowerElapsed..snapshot.elapsedRealtimeNanos
        val eventElapsed = if (timestampPlausible) sample.eventElapsedNanos else snapshot.elapsedRealtimeNanos
        val eventWall = snapshot.wallClockMillis - (snapshot.elapsedRealtimeNanos - eventElapsed) / NANOS_PER_MILLI
        val gapMillis = (eventElapsed - lowerElapsed) / NANOS_PER_MILLI

        val rawDelta = when (change) {
            SessionChange.FIRST_RUN -> 0L
            SessionChange.NEW_BOOT, SessionChange.COUNTER_RESET -> sample.counterValue
            SessionChange.SAME_SESSION -> sample.counterValue - (state?.lastCounterValue ?: sample.counterValue)
        }
        val gapMinutesCeil = (gapMillis + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE
        val plausibleMax = MAX_CADENCE * gapMinutesCeil + JUMP_SLACK_STEPS
        val delta = minOf(rawDelta, plausibleMax)

        val increments = attribute(
            steps = delta,
            fromWall = eventWall - gapMillis,
            toWall = eventWall,
            zoneId = snapshot.zoneId,
        )

        return Accounting(
            increments = increments,
            newState = TrackerState(
                bootCount = snapshot.bootCount,
                // The raw value, even when the delta was capped: a sensor jump is dropped, not
                // carried into the next sample.
                lastCounterValue = sample.counterValue,
                lastSampleElapsedNanos = eventElapsed,
                lastSampleWallMillis = eventWall,
            ),
            change = change,
            acceptedSteps = delta,
            cappedFromSteps = rawDelta.takeIf { it > delta },
            usedArrivalTime = !timestampPlausible,
        )
    }

    private fun changeOf(state: TrackerState?, sample: StepSample, snapshot: SystemSnapshot): SessionChange = when {
        state == null -> SessionChange.FIRST_RUN

        // The boot count is the primary signal; the elapsed clock going backwards catches a
        // reboot on a device whose boot count is missing or stuck.
        snapshot.bootCount != state.bootCount -> SessionChange.NEW_BOOT

        snapshot.elapsedRealtimeNanos < state.lastSampleElapsedNanos -> SessionChange.NEW_BOOT

        sample.counterValue < state.lastCounterValue -> SessionChange.COUNTER_RESET

        else -> SessionChange.SAME_SESSION
    }

    /**
     * Spreads [steps] over the minutes of `[fromWall, toWall]`, by the time each minute holds of
     * the stretch they were walked in.
     *
     * The stretch ends at the sample and lasts as long as [steps] take at [DEFAULT_CADENCE]
     * (steps come in walks, and the walk is most likely what ended just before the sensor
     * reported), never longer than the gap; steps more than that cadence fills are spread
     * evenly over the whole gap, at their own average. One rule for every gap, measured to the
     * millisecond: a sensor that hands over a minute and a half of walking at once (one with no
     * hardware FIFO, whose events are dropped while the processor sleeps) puts it across the two
     * minutes it was walked in, not 170 steps in one and 25 in the other, which would read as a
     * minute of running. A step reported alone, as most are, falls in its own minute. The sum is
     * always exactly [steps].
     */
    private fun attribute(steps: Long, fromWall: Long, toWall: Long, zoneId: ZoneId): List<MinuteSteps> {
        if (steps <= 0L) return emptyList()
        val atCadence = (steps * MILLIS_PER_MINUTE + DEFAULT_CADENCE - 1) / DEFAULT_CADENCE
        val span = minOf(toWall - fromWall, atCadence)
        if (span <= 0L) return listOf(minuteSteps(Math.floorDiv(toWall, MILLIS_PER_MINUTE), steps, zoneId))
        val start = toWall - span
        val increments = ArrayList<MinuteSteps>()
        var minute = Math.floorDiv(start, MILLIS_PER_MINUTE)
        var covered = 0L
        var laid = 0L
        while (covered < span) {
            val end = minOf((minute + 1) * MILLIS_PER_MINUTE, toWall)
            covered += end - maxOf(start, minute * MILLIS_PER_MINUTE)
            // Shared by the time covered so far, rounded down: the parts add up to the whole, and
            // a lone step, whose time is mostly before its own instant, still lands at it.
            val upTo = steps * covered / span
            if (upTo > laid) increments += minuteSteps(minute, upTo - laid, zoneId)
            laid = upTo
            minute++
        }
        return increments
    }

    private fun minuteSteps(epochMinute: Long, steps: Long, zoneId: ZoneId): MinuteSteps = MinuteSteps(
        epochMinute = epochMinute,
        localEpochDay = Instant.ofEpochMilli(epochMinute * MILLIS_PER_MINUTE).atZone(zoneId).toLocalDate().toEpochDay(),
        steps = steps.toInt(),
    )
}
