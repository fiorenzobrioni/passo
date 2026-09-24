package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.DEFAULT_WALKING_STEP_LENGTH_M
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_STEP_LENGTH_FACTOR
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.STEP_LENGTH_HEIGHT_RATIO
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.STEP_LENGTH_HEIGHT_RATIO_FEMALE
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode

/**
 * The two step lengths distance is measured with (PLANNING.md §6): one for walking minutes,
 * one for running minutes. A step, not a stride: a stride is two steps (VISION.md glossary).
 */
data class StepLengths(val walkingMeters: Double, val runningMeters: Double) {
    /** The length of one step taken in a minute of [stepsPerMinute]. */
    fun forCadence(stepsPerMinute: Int): Double =
        if (stepsPerMinute >= RUNNING_CADENCE) runningMeters else walkingMeters

    companion object {
        /**
         * The step lengths [profile] leads to. The reader's own walking length wins when they
         * set or calibrated one, the height's estimate otherwise; the running length is the
         * reader's own, or the walking one × [RUNNING_STEP_LENGTH_FACTOR].
         */
        fun of(profile: Profile): StepLengths {
            val valid = profile.sanitized()
            val walking = valid.walkingStepLengthMeters.takeIf { valid.stepLengthMode != StepLengthMode.AUTO }
                ?: estimatedWalkingStepLength(valid.heightMeters, valid.sex)
            val running = valid.runningStepLengthMeters ?: (walking * RUNNING_STEP_LENGTH_FACTOR)
            return StepLengths(walkingMeters = walking, runningMeters = running)
        }

        /** The walking step length estimated from the height, or the default without one. */
        fun estimatedWalkingStepLength(heightMeters: Double?, sex: Sex?): Double {
            if (heightMeters == null) return DEFAULT_WALKING_STEP_LENGTH_M
            val ratio = if (sex == Sex.FEMALE) STEP_LENGTH_HEIGHT_RATIO_FEMALE else STEP_LENGTH_HEIGHT_RATIO
            return heightMeters * ratio
        }
    }
}
