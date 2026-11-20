package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.DEFAULT_WALKING_STEP_LENGTH_M
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_STEP_LENGTH_FACTOR
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.STEP_LENGTH_HEIGHT_RATIO
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.STEP_LENGTH_HEIGHT_RATIO_FEMALE
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepPoint

/**
 * The step lengths distance is measured with (PLANNING.md §6): one for walking minutes, one for
 * running minutes, or a learnt step's curve where it has a part for the gait (Phase 17). A step,
 * not a stride: a stride is two steps (VISION.md glossary).
 *
 * @property walkingMeters the one walking length a screen names: with a learnt step, the curve's
 *   length at the reader's usual cadence, the figure its card named.
 * @property runningMeters the one running length a screen names.
 * @property learned the learnt step in use, read by [forCadence] where it has a part.
 */
data class StepLengths(val walkingMeters: Double, val runningMeters: Double, val learned: LearnedStep? = null) {
    /** The length of one step taken in a minute of [stepsPerMinute]. */
    fun forCadence(stepsPerMinute: Int): Double = if (stepsPerMinute >= RUNNING_CADENCE) {
        learned?.takeIf { it.running }?.runningAt(stepsPerMinute.toDouble()) ?: runningMeters
    } else {
        learned?.walkingAt(stepsPerMinute.toDouble()) ?: walkingMeters
    }

    companion object {
        /**
         * The step lengths [profile] leads to. A learnt step wins where it has a part; the reader's
         * own walking length when they set or calibrated one; the height's estimate otherwise. The
         * running length is the learnt one when in use, the reader's own, or the walking one ×
         * [RUNNING_STEP_LENGTH_FACTOR].
         */
        fun of(profile: Profile): StepLengths {
            val valid = profile.sanitized()
            val learned = valid.learnedStep?.takeIf { valid.stepLengthMode == StepLengthMode.LEARNED }
            val walking = when (valid.stepLengthMode) {
                StepLengthMode.LEARNED -> learned?.usualMeters
                StepLengthMode.AUTO -> null
                StepLengthMode.MANUAL, StepLengthMode.CALIBRATED -> valid.walkingStepLengthMeters
            } ?: estimatedWalkingStepLength(valid.heightMeters, valid.sex)
            val running = learned?.takeIf { it.running }?.namedRunning
                ?: valid.runningStepLengthMeters
                ?: (walking * RUNNING_STEP_LENGTH_FACTOR)
            return StepLengths(walkingMeters = walking, runningMeters = running, learned = learned)
        }

        /** The walking step length estimated from the height, or the default without one. */
        fun estimatedWalkingStepLength(heightMeters: Double?, sex: Sex?): Double {
            if (heightMeters == null) return DEFAULT_WALKING_STEP_LENGTH_M
            val ratio = if (sex == Sex.FEMALE) STEP_LENGTH_HEIGHT_RATIO_FEMALE else STEP_LENGTH_HEIGHT_RATIO
            return heightMeters * ratio
        }
    }
}

/** The curve's walking part: its points below the running cadence. */
val LearnedStep.walkingPoints: List<StepPoint> get() = points.filter { it.cadence < RUNNING_CADENCE }

/** The curve's running part: its points at the running cadence or above. */
val LearnedStep.runningPoints: List<StepPoint> get() = points.filter { it.cadence >= RUNNING_CADENCE }

/** The walking step at [cadence]; null when the walking part was not learnt. */
fun LearnedStep.walkingAt(cadence: Double): Double? = walkingPoints.lengthAt(cadence)

/** The running step at [cadence]; null when the running part was not learnt. */
fun LearnedStep.runningAt(cadence: Double): Double? = runningPoints.lengthAt(cadence)

/** The walking step at the usual cadence: the one walking length the app names. */
val LearnedStep.usualMeters: Double? get() = walkingAt(usualCadence.toDouble())

/**
 * The one running length a screen names: the running part halfway along it. A running part is
 * one or two points for almost everyone (a jog, a run), so its middle is what it says.
 */
val LearnedStep.namedRunning: Double?
    get() = runningPoints.takeIf { it.isNotEmpty() }
        ?.let { part -> part.lengthAt((part.first().cadence + part.last().cadence) / 2) }

/**
 * A part of a curve read at [cadence]: a straight line between two points, flat beyond its ends
 * (the part's own gait goes up to the running cadence or from it, and nothing past the points
 * says the step goes on changing). Null for a part with no point.
 */
private fun List<StepPoint>.lengthAt(cadence: Double): Double? {
    if (isEmpty()) return null
    if (cadence <= first().cadence) return first().meters
    if (cadence >= last().cadence) return last().meters
    val upper = indexOfFirst { it.cadence >= cadence }
    val a = this[upper - 1]
    val b = this[upper]
    val span = b.cadence - a.cadence
    return if (span <= 0) b.meters else a.meters + (b.meters - a.meters) * (cadence - a.cadence) / span
}
