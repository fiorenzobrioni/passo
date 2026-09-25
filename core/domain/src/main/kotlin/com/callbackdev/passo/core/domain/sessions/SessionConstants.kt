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
