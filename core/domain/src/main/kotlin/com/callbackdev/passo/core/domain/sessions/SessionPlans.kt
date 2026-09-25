package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MIN_REST_OF_DAY_STEPS
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * What an outing adds up to before it is walked: the editor's line "about 2,200 steps and
 * 1.6 km with your step", from the profile's step length and the intensity's cadence. An
 * estimate, and said so on screen.
 */
data class SessionEstimate(val steps: Int, val distanceMeters: Double, val minutes: Int)

object SessionPlans {
    /**
     * The three outings a first visit finds, ready to start and to change: a brisk walk of twenty
     * minutes (the WHO's weekly 150 is about 20 a day), a half-hour run, and the rest of the day.
     * Unnamed, so they are called in the reader's language.
     */
    val PRESETS: List<SessionPlan> = listOf(
        SessionPlan(goalKind = SessionGoalKind.TIME, goalValue = 20, intensity = SessionIntensity.BRISK, position = 0),
        SessionPlan(goalKind = SessionGoalKind.TIME, goalValue = 30, intensity = SessionIntensity.RUN, position = 1),
        SessionPlan(
            goalKind = SessionGoalKind.REST_OF_DAY,
            goalValue = 0,
            intensity = SessionIntensity.BRISK,
            position = 2,
        ),
    )

    /** A new outing in the editor: a brisk walk, the kind most people start with. */
    val NEW: SessionPlan = SessionPlan(goalKind = SessionGoalKind.TIME, goalValue = 30, intensity = SessionIntensity.BRISK)

    /**
     * The outing the evening reminder's "Walk now" starts when the reader keeps none for the rest
     * of the day: the steps left, at a brisk pace, with the default signals.
     */
    val REST_OF_DAY: SessionPlan = PRESETS.last()

    /** A value the editor accepts for [kind], on its steps and inside its range. */
    fun clampValue(kind: SessionGoalKind, value: Int): Int = when (kind) {
        SessionGoalKind.STEPS -> snap(value, SessionConstants.STEPS_RANGE, SessionConstants.STEPS_STEP)
        SessionGoalKind.DISTANCE -> snap(value, SessionConstants.DISTANCE_RANGE, SessionConstants.DISTANCE_STEP)
        SessionGoalKind.TIME -> snap(value, SessionConstants.TIME_RANGE, SessionConstants.TIME_STEP)
        SessionGoalKind.REST_OF_DAY -> 0
    }

    /**
     * The value to show when the reader switches the goal to [kind]: the same outing, roughly,
     * in the new quantity, so switching from 20 minutes to steps offers about 2,000 steps.
     */
    fun convert(from: SessionPlan, kind: SessionGoalKind, lengths: StepLengths, restOfDaySteps: Int): Int {
        if (kind == from.goalKind) return from.goalValue
        val estimate = estimate(from, lengths, restOfDaySteps)
        return clampValue(
            kind,
            when (kind) {
                SessionGoalKind.STEPS -> estimate.steps
                SessionGoalKind.DISTANCE -> estimate.distanceMeters.roundToInt()
                SessionGoalKind.TIME -> estimate.minutes
                SessionGoalKind.REST_OF_DAY -> 0
            },
        )
    }

    /** What [plan] adds up to, walked at its intensity's cadence with [lengths]. */
    fun estimate(plan: SessionPlan, lengths: StepLengths, restOfDaySteps: Int): SessionEstimate {
        val cadence = plan.intensity.typicalCadence
        val stepLength = lengths.forCadence(cadence)
        val steps = when (plan.goalKind) {
            SessionGoalKind.STEPS -> plan.goalValue
            SessionGoalKind.DISTANCE -> ceil(plan.goalValue / stepLength).toInt()
            SessionGoalKind.TIME -> plan.goalValue * cadence
            SessionGoalKind.REST_OF_DAY -> restOfDaySteps.coerceAtLeast(0)
        }
        val minutes = when (plan.goalKind) {
            SessionGoalKind.TIME -> plan.goalValue
            else -> ceil(steps.toDouble() / cadence).toInt()
        }
        return SessionEstimate(steps = steps, distanceMeters = steps * stepLength, minutes = minutes)
    }

    /**
     * The steps "the rest of the day" means now: today's goal less today's count; zero once it
     * is met, and too small to start under [MIN_REST_OF_DAY_STEPS].
     */
    fun restOfDay(todaySteps: Int, dailyGoalSteps: Int): Int = (dailyGoalSteps - todaySteps).coerceAtLeast(0)

    /**
     * The outing [plan] starts as, now: its goal copied (the rest of the day resolved to steps),
     * so changing the plan later does not change an outing under way or past. Null when there is
     * nothing to walk: the rest of a day whose goal is met, or all but met.
     */
    fun start(
        plan: SessionPlan,
        nowMillis: Long,
        localEpochDay: Long,
        todaySteps: Int,
        dailyGoalSteps: Int,
    ): Session? {
        val restOfDay = plan.goalKind == SessionGoalKind.REST_OF_DAY
        val kind = if (restOfDay) SessionGoalKind.STEPS else plan.goalKind
        val value = if (restOfDay) restOfDay(todaySteps, dailyGoalSteps) else clampValue(kind, plan.goalValue)
        if (restOfDay && value < MIN_REST_OF_DAY_STEPS) return null
        return Session(
            planId = plan.id.takeIf { it != 0L },
            name = plan.name?.takeIf { it.isNotBlank() },
            goalKind = kind,
            goalValue = value,
            restOfDay = restOfDay,
            intensity = plan.intensity,
            milestones = plan.milestones.filter { it in SessionMilestone.CHOOSABLE }.toSet(),
            vibrate = plan.vibrate,
            localEpochDay = localEpochDay,
            startedAtMillis = nowMillis,
        )
    }

    private fun snap(value: Int, range: IntRange, step: Int): Int {
        val snapped = ((value.toDouble() / step).roundToInt() * step)
        return snapped.coerceIn(range)
    }
}
