package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.routes.DistanceKind
import com.callbackdev.passo.core.domain.routes.distanceKind
import com.callbackdev.passo.core.model.RoundStanding
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionState
import kotlin.math.ceil

/**
 * An amount of an outing's own quantity: steps, metres or minutes, as its goal is counted in.
 * Minutes are whole, rounded up when they are what is left: "1 minute to go" until it is done.
 * An open outing (Phase 21) has no quantity of its own: what it has done is said in minutes in
 * motion ([SessionGoalKind.TIME]), and its goal, never shown, is [SessionGoalKind.OPEN]'s nothing.
 */
data class SessionAmount(val kind: SessionGoalKind, val value: Double)

/**
 * The share of the goal reached so far: 0 at the start, 1 at the goal, beyond after "Keep going".
 * Always 0 for an open outing, which has no goal to take a share of: nothing crosses a milestone.
 */
fun Session.progress(): Double {
    val target = goalValue.toDouble()
    if (target <= 0) return 0.0
    return when (goalKind) {
        SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> totals.steps / target
        SessionGoalKind.DISTANCE -> totals.distanceMeters / target
        SessionGoalKind.TIME, SessionGoalKind.INTERVALS -> totals.movingMillis / (target * MILLIS_PER_MINUTE)
        SessionGoalKind.OPEN -> 0.0
    }
}

/** How much is done, in the goal's own quantity; an open outing's, in minutes in motion. */
fun Session.done(): SessionAmount = when (goalKind) {
    SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> SessionAmount(goalKind, totals.steps.toDouble())

    SessionGoalKind.DISTANCE -> SessionAmount(goalKind, totals.distanceMeters)

    SessionGoalKind.TIME, SessionGoalKind.INTERVALS ->
        SessionAmount(goalKind, (totals.movingMillis / MILLIS_PER_MINUTE).toDouble())

    SessionGoalKind.OPEN -> SessionAmount(SessionGoalKind.TIME, (totals.movingMillis / MILLIS_PER_MINUTE).toDouble())
}

/** How much is left to the goal, never below zero; whole minutes, rounded up. Nothing for an open outing. */
fun Session.remaining(): SessionAmount = when (goalKind) {
    SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY ->
        SessionAmount(goalKind, (goalValue - totals.steps).coerceAtLeast(0).toDouble())

    SessionGoalKind.DISTANCE -> SessionAmount(goalKind, (goalValue - totals.distanceMeters).coerceAtLeast(0.0))

    SessionGoalKind.TIME, SessionGoalKind.INTERVALS -> {
        val left = (goalValue * MILLIS_PER_MINUTE - totals.movingMillis).coerceAtLeast(0)
        SessionAmount(goalKind, ceil(left.toDouble() / MILLIS_PER_MINUTE))
    }

    SessionGoalKind.OPEN -> SessionAmount(SessionGoalKind.OPEN, 0.0)
}

/** The goal, in its own quantity; [SessionGoalKind.OPEN]'s nothing for an open outing. */
fun Session.goal(): SessionAmount = SessionAmount(goalKind, goalValue.toDouble())

/**
 * A goal outing taken on past its goal with «Keep going» (Phase 21.3), and counting still: open in
 * all but its name, so its card shows its shape where the bar stood full. A city walk past its end
 * keeps its walk's map, which says more of it than its minutes.
 */
val Session.pastGoal: Boolean get() = reached && live && !open && walk == null

/**
 * How far an outing taken on past its goal went beyond it, in the goal's own quantity: the minutes
 * in motion past a time goal, the steps past a goal of steps, the distance past one of distance.
 * Null before the goal, under one of its units beyond it (a minute, a step, ten metres), for an
 * open outing, which has no goal to go beyond, and for one that ended at its goal: the batch that
 * crossed it may run a few steps past, which the reader did not choose to walk on. The time in
 * motion at the moment of the goal is not kept, so a goal of steps says its steps beyond, not its
 * minutes: the quantity its bar was counted in.
 */
fun Session.beyond(): SessionAmount? {
    if (!reached || open || (state == SessionState.FINISHED && end == SessionEnd.GOAL)) return null
    return when (goalKind) {
        SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY ->
            (totals.steps - goalValue).takeIf { it >= 1 }?.let { SessionAmount(goalKind, it.toDouble()) }

        SessionGoalKind.DISTANCE -> (totals.distanceMeters - goalValue).takeIf { it >= MIN_BEYOND_METERS }
            ?.let { SessionAmount(goalKind, it) }

        SessionGoalKind.TIME, SessionGoalKind.INTERVALS ->
            ((totals.movingMillis - goalValue * MILLIS_PER_MINUTE) / MILLIS_PER_MINUTE).takeIf { it >= 1 }
                ?.let { SessionAmount(SessionGoalKind.TIME, it.toDouble()) }

        SessionGoalKind.OPEN -> null
    }
}

/** The share of its moving time spent at the outing's cadence; null before it has moved. */
fun Session.zoneShare(): Double? =
    if (totals.movingMillis <= 0) null else totals.zoneMillis.toDouble() / totals.movingMillis

/**
 * How long the outing has been paused by [nowMillis]: its pauses that ended inside it, and the one
 * under way (Phase 21). An outing from before the pauses were kept says only the one under way.
 */
fun Session.pausedMillis(nowMillis: Long = lastEventAtMillis): Long {
    val ended = pauses.sumOf { it.millis }
    val now = pausedAtMillis?.let { (nowMillis - it).coerceAtLeast(0L) } ?: 0L
    return ended + now
}

/**
 * The sentence an outing is told with, before any of its numbers: on Today's
 * card and in the notification alike.
 */
sealed interface SessionHeadline {
    /** Just started: nothing to measure yet. */
    data class Starting(val goal: SessionAmount) : SessionHeadline

    /**
     * An interval outing under way (Phase 13): the interval it is in, with what is left of it.
     * Its own sentence from start to end: the interval says more than the minutes of the whole.
     */
    data class Interval(val position: IntervalPosition) : SessionHeadline

    /** Under way, [left] to go. */
    data class Going(val left: SessionAmount) : SessionHeadline

    /** Past half, [left] to go. */
    data class PastHalf(val left: SessionAmount) : SessionHeadline

    /** The last stretch, [left] to go. */
    data class AlmostThere(val left: SessionAmount) : SessionHeadline

    data object Paused : SessionHeadline

    /**
     * The goal was reached; the reader may be going on past it, [beyond] it so far (Phase 21.3),
     * null until a whole unit of it.
     */
    data class Reached(val goal: SessionAmount, val beyond: SessionAmount? = null) : SessionHeadline

    /** Over before its goal, at [progress] of it. */
    data class Ended(val progress: Double, val end: SessionEnd) : SessionHeadline

    /**
     * An open outing (Phase 21) in its first minute in motion: nothing to say of it yet but that
     * everything counts from here.
     */
    data object OpenStarting : SessionHeadline

    /**
     * An open outing under way: [movingMillis] in motion, and its distance where it is a measure
     * ([measuredMeters], at least nine tenths from fixes); null where it is an estimate, which
     * the numbers under the sentence say as one.
     */
    data class OpenGoing(val movingMillis: Long, val measuredMeters: Double?) : SessionHeadline

    /**
     * An open outing with GPS on one of the reader's usual rounds (Phase 20), from its first
     * kilometre: its time in motion there against the round's usual, [gapMillis] (below zero ahead,
     * above behind, zero at the usual), as Today says a day against a usual one.
     */
    data class OnRound(val gapMillis: Long) : SessionHeadline

    /** The same outing, off its usual round: said once, until the next kilometre. */
    data object OffRound : SessionHeadline

    /**
     * An open outing over: what it came to, [movingMillis] in motion and [distanceMeters], a
     * measure where [measured], an estimate otherwise; and how it ended.
     */
    data class OpenEnded(
        val end: SessionEnd,
        val movingMillis: Long,
        val distanceMeters: Double,
        val measured: Boolean,
    ) : SessionHeadline

    companion object {
        /** [nowMillis] places an interval outing's countdown; any other outing has none. */
        fun of(session: Session, nowMillis: Long = session.lastStepAtMillis): SessionHeadline {
            if (session.open) return open(session)
            val progress = session.progress()
            val interval = session.intervalAt(nowMillis)
            return when {
                session.reached -> Reached(session.goal(), session.beyond())
                session.state == SessionState.FINISHED -> Ended(progress, session.end ?: SessionEnd.STOPPED)
                session.state == SessionState.PAUSED -> Paused
                interval != null -> Interval(interval)
                progress < STARTING_SHARE -> Starting(session.goal())
                progress < HALF -> Going(session.remaining())
                progress < ALMOST -> PastHalf(session.remaining())
                else -> AlmostThere(session.remaining())
            }
        }

        private fun open(session: Session): SessionHeadline {
            val measured = session.distanceKind() == DistanceKind.MEASURED
            val moving = session.totals.movingMillis
            return when {
                session.state == SessionState.FINISHED -> OpenEnded(
                    end = session.end ?: SessionEnd.STOPPED,
                    movingMillis = moving,
                    distanceMeters = session.totals.distanceMeters,
                    measured = measured,
                )

                session.state == SessionState.PAUSED -> Paused

                moving < OPEN_STARTING_MILLIS -> OpenStarting

                else -> onRound(session) ?: OpenGoing(moving, session.totals.distanceMeters.takeIf { measured })
            }
        }

        /** An open outing's sentence on a usual round, while it counts; null off one, and before its first kilometre. */
        private fun onRound(session: Session): SessionHeadline? {
            val line = session.roundLine ?: return null
            return when (line.standing) {
                RoundStanding.ON -> line.gapMillis?.let(::OnRound)
                RoundStanding.LEFT -> OffRound
                RoundStanding.GONE -> null
            }
        }

        /** Under this share the outing has only just begun: its goal is the news, not what is left. */
        private const val STARTING_SHARE = 0.05
        private const val HALF = 0.5

        /** From here "almost there": the last stretch, about a sixth of a 20-minute walk. */
        private const val ALMOST = 0.85

        /**
         * An open outing's first minute in motion: «25 seconds on your feet» would be a number for
         * its own sake, and a minute is the smallest the sentence counts in.
         */
        private const val OPEN_STARTING_MILLIS = 60_000L
    }
}

private const val MILLIS_PER_MINUTE = 60_000L

/** Ten metres: the least a distance beyond its goal is said in, its smallest written step. */
private const val MIN_BEYOND_METERS = 10.0
