package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MIN_REST_OF_DAY_STEPS
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionVoice
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
     * The Japanese interval walk as the protocol has it (Nemoto et al., 2007): five sets of three
     * slow minutes and three fast ones, at a brisk pace, the changes its only signals (Phase 13).
     */
    val JAPANESE_WALKING: SessionPlan = SessionPlan(
        goalKind = SessionGoalKind.INTERVALS,
        goalValue = IntervalSets().totalMinutes,
        intensity = SessionIntensity.BRISK,
        milestones = emptySet(),
        intervals = IntervalSets(),
        position = 4,
    )

    /**
     * A walk with no goal, at any pace (Phase 21): the open outing, the shortest way in. First of
     * the presets, as the known apps put it first: started, walked as it comes, stopped at the end.
     */
    val OPEN_WALK: SessionPlan = SessionPlan(
        goalKind = SessionGoalKind.OPEN,
        goalValue = 0,
        intensity = SessionIntensity.FREE,
        position = 0,
    )

    /**
     * The five outings a first visit finds, ready to start and to change: a walk with no goal
     * (Phase 21), a brisk walk of twenty minutes (the WHO's weekly 150 is about 20 a day), a
     * half-hour run, the rest of the day, and the Japanese interval walk (Phase 13). Unnamed, so
     * they are called in the reader's language.
     */
    val PRESETS: List<SessionPlan> = listOf(
        OPEN_WALK,
        SessionPlan(goalKind = SessionGoalKind.TIME, goalValue = 20, intensity = SessionIntensity.BRISK, position = 1),
        SessionPlan(goalKind = SessionGoalKind.TIME, goalValue = 30, intensity = SessionIntensity.RUN, position = 2),
        SessionPlan(
            goalKind = SessionGoalKind.REST_OF_DAY,
            goalValue = 0,
            intensity = SessionIntensity.BRISK,
            position = 3,
        ),
        JAPANESE_WALKING,
    )

    /** A new outing in the editor: a brisk walk, the kind most people start with. */
    val NEW: SessionPlan =
        SessionPlan(goalKind = SessionGoalKind.TIME, goalValue = 30, intensity = SessionIntensity.BRISK)

    /**
     * The outing the evening reminder's "Walk now" starts when the reader keeps none for the rest
     * of the day: the steps left, at a brisk pace, with the default signals.
     */
    val REST_OF_DAY: SessionPlan = PRESETS.first { it.goalKind == SessionGoalKind.REST_OF_DAY }

    /** A value the editor accepts for [kind], on its steps and inside its range. */
    fun clampValue(kind: SessionGoalKind, value: Int): Int = when (kind) {
        SessionGoalKind.STEPS -> snap(value, SessionConstants.STEPS_RANGE, SessionConstants.STEPS_STEP)
        SessionGoalKind.DISTANCE -> snap(value, SessionConstants.DISTANCE_RANGE, SessionConstants.DISTANCE_STEP)
        SessionGoalKind.TIME -> snap(value, SessionConstants.TIME_RANGE, SessionConstants.TIME_STEP)
        SessionGoalKind.REST_OF_DAY, SessionGoalKind.OPEN -> 0
        SessionGoalKind.INTERVALS -> value.coerceIn(INTERVALS_RANGE)
    }

    /** The editor's step for [kind]: 500 steps, half a kilometre or a quarter mile, 5 minutes. */
    fun editorStep(kind: SessionGoalKind, imperial: Boolean): Double = when (kind) {
        SessionGoalKind.STEPS -> SessionConstants.STEPS_STEP.toDouble()

        SessionGoalKind.DISTANCE ->
            if (imperial) SessionConstants.DISTANCE_STEP_IMPERIAL else SessionConstants.DISTANCE_STEP.toDouble()

        SessionGoalKind.TIME -> SessionConstants.TIME_STEP.toDouble()

        SessionGoalKind.REST_OF_DAY, SessionGoalKind.INTERVALS, SessionGoalKind.OPEN -> 1.0
    }

    /** [value] on the editor's steps for [kind] (quarter miles for [imperial] distances), in range. */
    fun snapForEditor(kind: SessionGoalKind, value: Double, imperial: Boolean): Int {
        if (kind == SessionGoalKind.REST_OF_DAY || kind == SessionGoalKind.OPEN) return 0
        val step = editorStep(kind, imperial)
        val snapped = ((value / step).roundToInt() * step).roundToInt()
        val range = range(kind)
        // The lowest step inside the range, so the first press up from it is one step.
        val floor = (ceil(range.first / step) * step).roundToInt()
        return snapped.coerceIn(floor, range.last)
    }

    /** One step up or down from [value] in the editor. */
    fun nudge(kind: SessionGoalKind, value: Int, up: Boolean, imperial: Boolean): Int {
        val step = editorStep(kind, imperial)
        return snapForEditor(kind, value + if (up) step else -step, imperial)
    }

    fun range(kind: SessionGoalKind): IntRange = when (kind) {
        SessionGoalKind.STEPS -> SessionConstants.STEPS_RANGE
        SessionGoalKind.DISTANCE -> SessionConstants.DISTANCE_RANGE
        SessionGoalKind.TIME -> SessionConstants.TIME_RANGE
        SessionGoalKind.REST_OF_DAY, SessionGoalKind.OPEN -> 0..0
        SessionGoalKind.INTERVALS -> INTERVALS_RANGE
    }

    /** Every interval outing the editor can make, in minutes: the least sets of the shortest to the most of the longest. */
    private val INTERVALS_RANGE: IntRange =
        IntervalSets(
            SessionConstants.INTERVAL_MINUTES_RANGE.first,
            SessionConstants.INTERVAL_MINUTES_RANGE.first,
            SessionConstants.INTERVAL_SETS_RANGE.first,
        ).totalMinutes..IntervalSets(
            SessionConstants.INTERVAL_MINUTES_RANGE.last,
            SessionConstants.INTERVAL_MINUTES_RANGE.last,
            SessionConstants.INTERVAL_SETS_RANGE.last,
        ).totalMinutes

    /** [sets] inside the editor's ranges. */
    fun clampIntervals(sets: IntervalSets): IntervalSets = IntervalSets(
        slowMinutes = sets.slowMinutes.coerceIn(SessionConstants.INTERVAL_MINUTES_RANGE),
        fastMinutes = sets.fastMinutes.coerceIn(SessionConstants.INTERVAL_MINUTES_RANGE),
        sets = sets.sets.coerceIn(SessionConstants.INTERVAL_SETS_RANGE),
    )

    /**
     * [plan] made an interval outing with [sets] (clamped): the goal follows the sets, the fast
     * pace is never free (a fast interval with no pace to keep says nothing), and the shares of
     * the goal are not signals, since the changes are.
     */
    fun withIntervals(plan: SessionPlan, sets: IntervalSets): SessionPlan {
        val clamped = clampIntervals(sets)
        return plan.copy(
            goalKind = SessionGoalKind.INTERVALS,
            goalValue = clamped.totalMinutes,
            intervals = clamped,
            intensity = if (plan.intensity == SessionIntensity.FREE) SessionIntensity.BRISK else plan.intensity,
        )
    }

    /**
     * [value] inside [kind]'s range, not snapped: a distance set in quarter miles stays one. What
     * an outing starts with.
     */
    fun inRange(kind: SessionGoalKind, value: Int): Int = value.coerceIn(range(kind))

    /**
     * The value to show when the reader switches the goal to [kind]: the same outing, roughly,
     * in the new quantity, so switching from 20 minutes to steps offers about 2,000 steps. An
     * open outing has no quantity to carry over: from one, the new plan's half hour at its pace,
     * in the new quantity.
     */
    fun convert(from: SessionPlan, kind: SessionGoalKind, lengths: StepLengths, restOfDaySteps: Int): Int {
        if (kind == from.goalKind) return from.goalValue
        if (kind == SessionGoalKind.INTERVALS) return from.intervals.totalMinutes
        if (kind == SessionGoalKind.OPEN) return 0
        if (from.open) return convert(NEW.copy(intensity = from.intensity), kind, lengths, restOfDaySteps)
        val estimate = estimate(from, lengths, restOfDaySteps)
        return clampValue(
            kind,
            when (kind) {
                SessionGoalKind.STEPS -> estimate.steps
                SessionGoalKind.DISTANCE -> estimate.distanceMeters.roundToInt()
                SessionGoalKind.TIME -> estimate.minutes
                SessionGoalKind.REST_OF_DAY, SessionGoalKind.INTERVALS, SessionGoalKind.OPEN -> 0
            },
        )
    }

    /**
     * What [plan] adds up to, walked at its intensity's cadence with [lengths]. Nothing for an
     * open outing: it adds up to what is walked, and the editor says no estimate for it.
     */
    fun estimate(plan: SessionPlan, lengths: StepLengths, restOfDaySteps: Int): SessionEstimate {
        if (plan.goalKind == SessionGoalKind.INTERVALS) return estimateIntervals(plan, lengths)
        if (plan.open) return SessionEstimate(steps = 0, distanceMeters = 0.0, minutes = 0)
        val cadence = plan.intensity.typicalCadence
        val stepLength = lengths.forCadence(cadence)
        val steps = when (plan.goalKind) {
            SessionGoalKind.STEPS -> plan.goalValue
            SessionGoalKind.DISTANCE -> ceil(plan.goalValue / stepLength).toInt()
            SessionGoalKind.TIME -> plan.goalValue * cadence
            SessionGoalKind.REST_OF_DAY -> restOfDaySteps.coerceAtLeast(0)
            SessionGoalKind.INTERVALS, SessionGoalKind.OPEN -> 0
        }
        val minutes = when (plan.goalKind) {
            SessionGoalKind.TIME -> plan.goalValue
            else -> ceil(steps.toDouble() / cadence).toInt()
        }
        return SessionEstimate(steps = steps, distanceMeters = steps * stepLength, minutes = minutes)
    }

    /** The slow minutes at an easy stroll, the fast ones at their pace, each with its step. */
    private fun estimateIntervals(plan: SessionPlan, lengths: StepLengths): SessionEstimate {
        val sets = clampIntervals(plan.intervals)
        val slow = SessionConstants.SLOW_INTERVAL_CADENCE
        val fast = plan.intensity.typicalCadence
        val slowSteps = sets.sets * sets.slowMinutes * slow
        val fastSteps = sets.sets * sets.fastMinutes * fast
        return SessionEstimate(
            steps = slowSteps + fastSteps,
            distanceMeters = slowSteps * lengths.forCadence(slow) + fastSteps * lengths.forCadence(fast),
            minutes = sets.totalMinutes,
        )
    }

    /**
     * The steps "the rest of the day" means now: today's goal less today's count; zero once it
     * is met, and too small to start under [MIN_REST_OF_DAY_STEPS].
     */
    fun restOfDay(todaySteps: Int, dailyGoalSteps: Int): Int = (dailyGoalSteps - todaySteps).coerceAtLeast(0)

    /**
     * The outing [plan] starts as, now: its goal copied (the rest of the day resolved to steps),
     * so changing the plan later does not change an outing under way or past. Null when there is
     * nothing to walk: the rest of a day whose goal is met, or all but met. An open outing starts
     * with no goal and no milestone, and with the open outing's longer cap; its marks are frozen
     * as the plan has them in force (step 21.2), with their unit.
     *
     * @param withGps it starts measured with GPS (docs/adr/0016-location.md): the plan asks for
     *   it, and the reader and the phone allow it right now. The caller knows the second half;
     *   it is frozen in the outing either way, like the goal.
     * @param miles the reader walks in miles now: an open outing's distance marks are then miles,
     *   to its end.
     */
    fun start(
        plan: SessionPlan,
        nowMillis: Long,
        localEpochDay: Long,
        todaySteps: Int,
        dailyGoalSteps: Int,
        withGps: Boolean = false,
        miles: Boolean = false,
    ): Session? {
        val restOfDay = plan.goalKind == SessionGoalKind.REST_OF_DAY
        val kind = if (restOfDay) SessionGoalKind.STEPS else plan.goalKind
        val value = if (restOfDay) restOfDay(todaySteps, dailyGoalSteps) else inRange(kind, plan.goalValue)
        if (restOfDay && value < MIN_REST_OF_DAY_STEPS) return null
        if (kind == SessionGoalKind.OPEN) {
            return Session(
                planId = plan.id.takeIf { it != 0L },
                name = plan.name?.takeIf { it.isNotBlank() },
                goalKind = SessionGoalKind.OPEN,
                goalValue = 0,
                intensity = plan.intensity,
                milestones = emptySet(),
                vibrate = plan.vibrate,
                voice = plan.voice,
                localEpochDay = localEpochDay,
                startedAtMillis = nowMillis,
                gps = withGps && plan.gps,
                capAtMillis = nowMillis + SessionConstants.OPEN_MAX_SESSION_MILLIS,
                pausesKept = true,
                // The plan's switch decides the mark, not what the phone allowed: a plan with GPS
                // started on the steps still tells its kilometres, estimated.
                marks = plan.marksInForce,
                markMiles = miles,
            )
        }
        if (kind == SessionGoalKind.INTERVALS) {
            val intervals = withIntervals(plan, plan.intervals)
            return Session(
                planId = plan.id.takeIf { it != 0L },
                name = plan.name?.takeIf { it.isNotBlank() },
                goalKind = kind,
                goalValue = intervals.goalValue,
                intensity = intervals.intensity,
                milestones = emptySet(),
                vibrate = plan.vibrate,
                voice = plan.voice,
                localEpochDay = localEpochDay,
                startedAtMillis = nowMillis,
                intervals = intervals.intervals,
                gps = withGps && plan.gps,
                capAtMillis = nowMillis + SessionConstants.MAX_SESSION_MILLIS,
                pausesKept = true,
            )
        }
        return Session(
            planId = plan.id.takeIf { it != 0L },
            name = plan.name?.takeIf { it.isNotBlank() },
            goalKind = kind,
            goalValue = value,
            restOfDay = restOfDay,
            intensity = plan.intensity,
            milestones = plan.milestones.filter { it in SessionMilestone.CHOOSABLE }.toSet(),
            vibrate = plan.vibrate,
            voice = plan.voice,
            localEpochDay = localEpochDay,
            startedAtMillis = nowMillis,
            gps = withGps && plan.gps,
            capAtMillis = nowMillis + SessionConstants.MAX_SESSION_MILLIS,
            pausesKept = true,
        )
    }

    /**
     * An outing on a city walk (Phase 11), from [fromMeters] along it to its end: its goal the
     * distance left, its signals the walk's places rather than shares of the goal (the goal, at
     * the walk's end, is still told). Null when nothing is left of the walk. Never with GPS: a
     * walk from home is walked on the steps (docs/adr/0016-location.md, decision 13).
     */
    fun startWalk(walk: Way, fromMeters: Int, voice: SessionVoice, nowMillis: Long, localEpochDay: Long): Session? {
        val left = walk.lengthMeters - fromMeters
        if (left < MIN_WALK_LEFT_METERS) return null
        return Session(
            planId = null,
            name = null,
            goalKind = SessionGoalKind.DISTANCE,
            goalValue = left,
            intensity = SessionIntensity.FREE,
            milestones = emptySet(),
            vibrate = true,
            voice = voice,
            localEpochDay = localEpochDay,
            startedAtMillis = nowMillis,
            walk = walk.id,
            walkFromMeters = fromMeters.coerceIn(0, walk.lengthMeters),
            capAtMillis = nowMillis + SessionConstants.MAX_SESSION_MILLIS,
            pausesKept = true,
        )
    }

    /** Under this, a walk is done: its last place is a few steps away. */
    const val MIN_WALK_LEFT_METERS = 50

    private fun snap(value: Int, range: IntRange, step: Int): Int {
        val snapped = ((value.toDouble() / step).roundToInt() * step)
        return snapped.coerceIn(range)
    }
}
