package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionState
import kotlin.math.ceil

/**
 * An amount of an outing's own quantity: steps, metres or minutes, as its goal is counted in.
 * Minutes are whole, rounded up when they are what is left: "1 minute to go" until it is done.
 */
data class SessionAmount(val kind: SessionGoalKind, val value: Double)

/** The share of the goal reached so far: 0 at the start, 1 at the goal, beyond after "Keep going". */
fun Session.progress(): Double {
    val target = goalValue.toDouble()
    if (target <= 0) return 0.0
    return when (goalKind) {
        SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> totals.steps / target
        SessionGoalKind.DISTANCE -> totals.distanceMeters / target
        SessionGoalKind.TIME -> totals.movingMillis / (target * MILLIS_PER_MINUTE)
    }
}

/** How much is done, in the goal's own quantity. */
fun Session.done(): SessionAmount = when (goalKind) {
    SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> SessionAmount(goalKind, totals.steps.toDouble())
    SessionGoalKind.DISTANCE -> SessionAmount(goalKind, totals.distanceMeters)
    SessionGoalKind.TIME -> SessionAmount(goalKind, (totals.movingMillis / MILLIS_PER_MINUTE).toDouble())
}

/** How much is left to the goal, never below zero; whole minutes, rounded up. */
fun Session.remaining(): SessionAmount = when (goalKind) {
    SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY ->
        SessionAmount(goalKind, (goalValue - totals.steps).coerceAtLeast(0).toDouble())

    SessionGoalKind.DISTANCE -> SessionAmount(goalKind, (goalValue - totals.distanceMeters).coerceAtLeast(0.0))

    SessionGoalKind.TIME -> {
        val left = (goalValue * MILLIS_PER_MINUTE - totals.movingMillis).coerceAtLeast(0)
        SessionAmount(goalKind, ceil(left.toDouble() / MILLIS_PER_MINUTE))
    }
}

/** The goal, in its own quantity. */
fun Session.goal(): SessionAmount = SessionAmount(goalKind, goalValue.toDouble())

/** The share of its moving time spent at the outing's cadence; null before it has moved. */
fun Session.zoneShare(): Double? =
    if (totals.movingMillis <= 0) null else totals.zoneMillis.toDouble() / totals.movingMillis

/**
 * The sentence an outing is told with, before any of its numbers (Chiaro's rule): on Today's
 * card and in the notification alike.
 */
sealed interface SessionHeadline {
    /** Just started: nothing to measure yet. */
    data class Starting(val goal: SessionAmount) : SessionHeadline

    /** Under way, [left] to go. */
    data class Going(val left: SessionAmount) : SessionHeadline

    /** Past half, [left] to go. */
    data class PastHalf(val left: SessionAmount) : SessionHeadline

    /** The last stretch, [left] to go. */
    data class AlmostThere(val left: SessionAmount) : SessionHeadline

    data object Paused : SessionHeadline

    /** The goal was reached; the reader may be going on past it. */
    data class Reached(val goal: SessionAmount) : SessionHeadline

    /** Over before its goal, at [progress] of it. */
    data class Ended(val progress: Double, val end: SessionEnd) : SessionHeadline

    companion object {
        fun of(session: Session): SessionHeadline {
            val progress = session.progress()
            return when {
                session.reached -> Reached(session.goal())
                session.state == SessionState.FINISHED -> Ended(progress, session.end ?: SessionEnd.STOPPED)
                session.state == SessionState.PAUSED -> Paused
                progress < STARTING_SHARE -> Starting(session.goal())
                progress < HALF -> Going(session.remaining())
                progress < ALMOST -> PastHalf(session.remaining())
                else -> AlmostThere(session.remaining())
            }
        }

        /** Under this share the outing has only just begun: its goal is the news, not what is left. */
        private const val STARTING_SHARE = 0.05
        private const val HALF = 0.5

        /** From here "almost there": the last stretch, about a sixth of a 20-minute walk. */
        private const val ALMOST = 0.85
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
