package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.tracking.TrackingConstants
import com.callbackdev.passo.core.model.SessionIntensity

/**
 * The outings' constants (PLANNING.md §11 Phase 10, docs/adr/0009-sessions.md), each with its
 * reason. The cadences are the ones the rest of the app already uses, from [MetricsConstants].
 */
object SessionConstants {
    /**
     * A step is credited with at most this much time in motion: the length of one step at 40
     * steps a minute, the pace below which a minute is not an active one
     * ([MetricsConstants.ACTIVE_MINUTE_THRESHOLD]). Between two steps a second apart the whole
     * second is moving; one step after a minute at a traffic light adds a second and a half,
     * not the minute. It is also what keeps a batch the hardware merged (steps without their own
     * timestamps) from counting a pause as motion.
     */
    const val MAX_MILLIS_PER_STEP: Long = 60_000L / MetricsConstants.ACTIVE_MINUTE_THRESHOLD

    /**
     * The cadence is read over the last half minute: long enough that a traffic light or the
     * hardware's own delivery of a few steps at once does not make it jump, short enough that
     * slowing down shows within the time it takes to notice it yourself.
     */
    const val CADENCE_WINDOW_MILLIS: Long = 30_000L

    /** Under this much walked time the cadence is not stated: a few steps are not a pace. */
    const val MIN_CADENCE_SPAN_MILLIS: Long = 10_000L

    /**
     * An outing with no step for this long is over: it ends at its last step, as if it had been
     * stopped there. A forgotten outing then costs nothing more (the next step, or the screen,
     * closes it) and never counts the evening as part of the afternoon's walk.
     */
    const val IDLE_END_MILLIS: Long = 15 * 60_000L

    /** A pause longer than this ends the outing where it was paused. */
    const val PAUSE_END_MILLIS: Long = 60 * 60_000L

    /** No outing stays open longer than this; past it, it ends at its last step. */
    const val MAX_SESSION_MILLIS: Long = 4 * 60 * 60_000L

    /**
     * After the goal, "Keep going" can reopen the outing for this long, with the steps taken
     * since: the reader who felt the long vibration takes a minute to get the phone out.
     */
    const val KEEP_GOING_MILLIS: Long = 15 * 60_000L

    /** An outing that ends with fewer steps than this is not kept: a start pressed by mistake. */
    const val MIN_KEPT_STEPS: Int = 30

    /** The goals the editor offers, and their steps. */
    val STEPS_RANGE: IntRange = 500..30_000
    const val STEPS_STEP: Int = 500

    /**
     * Distances in metres: about a quarter mile to a marathon, by half kilometres, or by quarter
     * miles for a reader who walks in miles.
     */
    val DISTANCE_RANGE: IntRange = 400..42_200
    const val DISTANCE_STEP: Int = 500
    const val DISTANCE_STEP_IMPERIAL: Double = 402.336

    /** Minutes in motion: five minutes to three hours, by five minutes. */
    val TIME_RANGE: IntRange = 5..180
    const val TIME_STEP: Int = 5

    /** A rest of the day smaller than this is not an outing worth starting. */
    const val MIN_REST_OF_DAY_STEPS: Int = 100

    /**
     * An interval's minutes (Phase 13): one to five, three by default, as the protocol has them.
     * Under a minute a change would come before the reader has found the pace; over five, the
     * fast minutes stop being intervals.
     */
    val INTERVAL_MINUTES_RANGE: IntRange = 1..5

    /** Sets: three to ten, five by default ("five sets or more", Nemoto et al., 2007). */
    val INTERVAL_SETS_RANGE: IntRange = 3..10

    /**
     * The cadence the editor's estimate assumes for the slow minutes: an easy stroll, below the
     * brisk band (100). An estimate, said so; the outing itself measures what was walked.
     */
    const val SLOW_INTERVAL_CADENCE: Int = 90

    /**
     * How close to a change of interval, in time in motion, the step counter reports every
     * [INTERVAL_NEAR_LATENCY_MILLIS] rather than every half minute (docs/adr/0013-interval-walks.md,
     * option A): the half-minute window and a margin, so the last report before the change is
     * never a half-minute one.
     */
    const val INTERVAL_WINDOW_MILLIS: Long = 40_000L

    /**
     * The report latency near a change: the counter's own delay is up to 10 s, so a shorter one
     * buys almost nothing and costs a wake every step.
     */
    const val INTERVAL_NEAR_LATENCY_MILLIS: Long = 2_000L

    /**
     * A change predicted this close, while walking, is told at this report rather than at the
     * next one: up to two seconds early rather than up to two seconds late. Never the goal, which
     * ends the outing and waits for the step that reaches it.
     */
    const val EARLY_CHANGE_MILLIS: Long = 2_000L

    /**
     * A screen's countdown carries on from the last step for at most this long: the counter hands
     * steps over in clusters, a few seconds apart, and a countdown that waited for each would
     * stutter. A stop longer than this holds it, as it holds the clock in motion.
     */
    const val COUNTDOWN_GLIDE_MILLIS: Long = 5_000L

    /** How many outings the launcher's long press offers. */
    const val SHORTCUTS: Int = 3
}

/**
 * The cadence an intensity asks to stay at or above; null for [SessionIntensity.FREE]. Brisk and
 * vigorous are the CADENCE-adults bands (100 and 130), running Passo's own threshold (140), the
 * one that switches to the running step length.
 */
val SessionIntensity.cadenceFloor: Int?
    get() = when (this) {
        SessionIntensity.FREE -> null
        SessionIntensity.BRISK -> MetricsConstants.BRISK_MINUTE_THRESHOLD
        SessionIntensity.VIGOROUS -> MetricsConstants.VIGOROUS_CADENCE
        SessionIntensity.RUN -> MetricsConstants.RUNNING_CADENCE
    }

/**
 * The cadence an estimate assumes for an intensity: its floor, and an ordinary walk (the
 * accountant's usual cadence) when there is none.
 */
val SessionIntensity.typicalCadence: Int
    get() = cadenceFloor ?: TrackingConstants.DEFAULT_CADENCE
