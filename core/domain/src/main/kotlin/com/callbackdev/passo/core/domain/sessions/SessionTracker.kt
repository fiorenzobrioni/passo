package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionConstants.CADENCE_WINDOW_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.IDLE_END_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.KEEP_GOING_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MAX_MILLIS_PER_STEP
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MAX_SESSION_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MIN_CADENCE_SPAN_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MIN_KEPT_STEPS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.PAUSE_END_MILLIS
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import kotlin.math.roundToInt

/** What the tracker has to tell after a step or a check. */
sealed interface SessionSignal {
    /**
     * A share of the goal was crossed: told once. When one batch of steps crosses several, only
     * the highest is told (three vibrations and then two would say nothing clear).
     */
    data class Milestone(val milestone: SessionMilestone) : SessionSignal

    /**
     * The outing is over. [kept] is false for one too short to be worth keeping
     * ([SessionConstants.MIN_KEPT_STEPS]), which the caller deletes rather than records.
     */
    data class Finished(val session: Session, val kept: Boolean) : SessionSignal
}

/**
 * An outing, measured from the steps as they come (PLANNING.md §11 Phase 10). Pure: the tracking
 * service feeds it every accounted sample with its own time, and it says what to tell.
 *
 * - **Time in motion** is made of the gaps between steps, each credited with at most
 *   [MAX_MILLIS_PER_STEP] per step: no timer runs, and standing still adds nothing.
 * - **The cadence** is the last [CADENCE_WINDOW_MILLIS] of steps; it decides the step length and
 *   the energy cost of each step, as the minute's cadence does for the day, and whether the time
 *   is at the outing's intensity.
 * - **Signals** come from the steps: the milestones the reader chose, then the goal, which ends
 *   the outing. For [KEEP_GOING_MILLIS] after it the steps are still counted aside, so "Keep
 *   going" reopens it with them.
 * - **It ends by itself** after [IDLE_END_MILLIS] without a step (at its last step), a pause of
 *   [PAUSE_END_MILLIS], or [MAX_SESSION_MILLIS] open; noticed at the next step or [check].
 *
 * Not thread-safe: the service drives it from one thread.
 */
class SessionTracker(initial: Session, private val lengths: StepLengths, private val weightKg: Double) {
    var session: Session = initial
        private set

    private val window = ArrayDeque<Mark>()
    private var windowFrom: Long = initial.lastEventAtMillis

    // After the goal: the steps since, and the time they reach, for "Keep going".
    private var overtime = SessionTotals()
    private var overtimeLastEvent: Long? = null

    /** Accounts [steps] taken at [atMillis]; returns what to tell, in order. */
    fun onSteps(atMillis: Long, steps: Int): List<SessionSignal> {
        if (steps <= 0) return emptyList()
        return when (session.state) {
            SessionState.PAUSED -> emptyList()
            SessionState.FINISHED -> {
                countOvertime(atMillis, steps)
                emptyList()
            }
            SessionState.ACTIVE -> countActive(atMillis, steps)
        }
    }

    /**
     * The cadence at [nowMillis], over the last [CADENCE_WINDOW_MILLIS]; null before there is
     * enough of it to be a pace, or while paused.
     */
    fun cadenceAt(nowMillis: Long): Int? {
        if (session.state == SessionState.PAUSED) return null
        val from = maxOf(nowMillis - CADENCE_WINDOW_MILLIS, windowFrom)
        val span = nowMillis - from
        if (span < MIN_CADENCE_SPAN_MILLIS) return null
        val steps = window.filter { it.atMillis > from && it.atMillis <= nowMillis }.sumOf { it.steps }
        return (steps * MILLIS_PER_MINUTE.toDouble() / span).roundToInt()
    }

    /** Ends it by itself when it has been still, paused or open for too long. */
    fun check(nowMillis: Long): List<SessionSignal> {
        val current = session
        return when (current.state) {
            SessionState.ACTIVE -> when {
                nowMillis - current.lastStepAtMillis > IDLE_END_MILLIS ->
                    listOf(finish(current.lastStepAtMillis, SessionEnd.IDLE))

                nowMillis - current.startedAtMillis > MAX_SESSION_MILLIS ->
                    listOf(finish(current.lastStepAtMillis, SessionEnd.CLOSED))

                else -> emptyList()
            }

            SessionState.PAUSED -> {
                val pausedAt = current.pausedAtMillis ?: nowMillis
                if (nowMillis - pausedAt > PAUSE_END_MILLIS) listOf(finish(pausedAt, SessionEnd.IDLE)) else emptyList()
            }

            SessionState.FINISHED -> emptyList()
        }
    }

    fun pause(nowMillis: Long): Boolean {
        if (session.state != SessionState.ACTIVE) return false
        session = session.copy(state = SessionState.PAUSED, pausedAtMillis = nowMillis)
        return true
    }

    /** Counting again from now: the pause is neither motion nor stillness. */
    fun resume(nowMillis: Long): Boolean {
        if (session.state != SessionState.PAUSED) return false
        session = session.copy(
            state = SessionState.ACTIVE,
            pausedAtMillis = null,
            lastEventAtMillis = nowMillis,
            lastStepAtMillis = nowMillis,
        )
        window.clear()
        windowFrom = nowMillis
        return true
    }

    /** The reader stops it: now, or where it was paused. Null if it is already over. */
    fun stop(nowMillis: Long): SessionSignal.Finished? {
        val current = session
        if (!current.live) return null
        val at = if (current.state == SessionState.PAUSED) current.pausedAtMillis ?: nowMillis else nowMillis
        return finish(maxOf(at, current.startedAtMillis), SessionEnd.STOPPED)
    }

    /** Whether "Keep going" can still reopen it: after its goal, and not too long after. */
    fun canKeepGoing(nowMillis: Long): Boolean {
        val current = session
        val ended = current.endedAtMillis ?: return false
        return current.state == SessionState.FINISHED && current.end == SessionEnd.GOAL &&
            nowMillis - ended <= KEEP_GOING_MILLIS
    }

    /** Reopens an outing ended by its goal, with the steps taken since. */
    fun keepGoing(nowMillis: Long): Boolean {
        if (!canKeepGoing(nowMillis)) return false
        val current = session
        val lastEvent = overtimeLastEvent ?: current.endedAtMillis ?: nowMillis
        session = current.copy(
            state = SessionState.ACTIVE,
            end = null,
            endedAtMillis = null,
            totals = current.totals + overtime,
            lastEventAtMillis = lastEvent,
            lastStepAtMillis = maxOf(lastEvent, nowMillis),
        )
        overtime = SessionTotals()
        overtimeLastEvent = null
        return true
    }

    private fun countActive(atMillis: Long, steps: Int): List<SessionSignal> {
        val current = session
        // A step after a long stillness belongs to the day, not to an outing that was over.
        if (atMillis - current.lastStepAtMillis > IDLE_END_MILLIS) {
            return listOf(finish(current.lastStepAtMillis, SessionEnd.IDLE))
        }
        if (atMillis - current.startedAtMillis > MAX_SESSION_MILLIS) {
            return listOf(finish(current.lastStepAtMillis, SessionEnd.CLOSED))
        }
        val added = measure(atMillis, steps, current.lastEventAtMillis)
        val before = current.progress()
        var next = current.copy(
            totals = current.totals + added,
            lastEventAtMillis = maxOf(atMillis, current.lastEventAtMillis),
            lastStepAtMillis = maxOf(atMillis, current.lastStepAtMillis),
        )
        val after = next.progress()
        val crossed = (next.milestones + SessionMilestone.GOAL).filter {
            it !in next.toldMilestones && before < it.fraction && after >= it.fraction
        }
        val signals = mutableListOf<SessionSignal>()
        if (crossed.isNotEmpty()) {
            next = next.copy(toldMilestones = next.toldMilestones + crossed)
            signals += SessionSignal.Milestone(crossed.maxBy { it.percent })
        }
        session = next
        if (SessionMilestone.GOAL in crossed && !next.reached) {
            session = next.copy(reachedAtMillis = atMillis)
            signals += finish(atMillis, SessionEnd.GOAL)
        }
        return signals
    }

    private fun countOvertime(atMillis: Long, steps: Int) {
        val ended = session.endedAtMillis ?: return
        if (session.end != SessionEnd.GOAL || atMillis - ended > KEEP_GOING_MILLIS) return
        val last = overtimeLastEvent ?: ended
        overtime += measure(atMillis, steps, last)
        overtimeLastEvent = maxOf(atMillis, last)
    }

    /** What [steps] at [atMillis] add, [lastEventAtMillis] being the moment measured to before. */
    private fun measure(atMillis: Long, steps: Int, lastEventAtMillis: Long): SessionTotals {
        val gap = (atMillis - lastEventAtMillis).coerceAtLeast(0)
        val moving = minOf(gap, steps * MAX_MILLIS_PER_STEP)
        window.addLast(Mark(atMillis, steps))
        while (window.isNotEmpty() && window.first().atMillis <= atMillis - CADENCE_WINDOW_MILLIS) window.removeFirst()
        val cadence = cadenceAt(atMillis)
        val floor = session.intensity.cadenceFloor
        val inZone = floor == null || (cadence != null && cadence >= floor)
        val distance = steps * lengths.forCadence(cadence ?: 0)
        return SessionTotals(
            steps = steps,
            movingMillis = moving,
            zoneMillis = if (inZone) moving else 0,
            distanceMeters = distance,
            activeKcal = distance / METERS_PER_KM * weightKg * MetricsCalculator.kcalPerKgPerKm(cadence ?: 0),
        )
    }

    private fun finish(atMillis: Long, end: SessionEnd): SessionSignal.Finished {
        val current = session
        val finished = current.copy(
            state = SessionState.FINISHED,
            end = end,
            endedAtMillis = maxOf(atMillis, current.startedAtMillis),
            pausedAtMillis = null,
        )
        session = finished
        return SessionSignal.Finished(finished, kept = finished.reached || finished.totals.steps >= MIN_KEPT_STEPS)
    }

    private data class Mark(val atMillis: Long, val steps: Int)

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val METERS_PER_KM = 1_000.0
    }
}

/** The share of the goal this milestone stands for. */
val SessionMilestone.fraction: Double get() = percent / PERCENT

private const val PERCENT = 100.0
