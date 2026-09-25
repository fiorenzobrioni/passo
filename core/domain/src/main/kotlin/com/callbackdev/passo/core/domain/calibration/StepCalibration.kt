package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.ProfileLimits
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import kotlin.math.roundToInt

/** Which of the two step lengths a calibration measures (PLANNING.md §6). */
enum class CalibratedStep {
    WALKING,
    RUNNING,
}

/** What a walk of a known distance came to (PLANNING.md §11 Phase 7, the calibration wizard). */
sealed interface CalibrationResult {
    /**
     * A step length, measured.
     *
     * @property cadence the steps a minute between Start and Stop; null for a walk under ten
     *   seconds, too short for a cadence that means anything.
     * @property paceMismatch the pace was not the one [CalibratedStep] is used for: a walking step
     *   measured at a running cadence, or a running step below it. Worth saying, not refusing:
     *   the length is still what the reader walked.
     */
    data class Measured(
        val step: CalibratedStep,
        val stepLengthMeters: Double,
        val steps: Int,
        val distanceMeters: Double,
        val cadence: Int?,
        val paceMismatch: Boolean,
    ) : CalibrationResult

    /** Too few steps to divide a distance by: the counter missed the walk, or it was not walked. */
    data class TooFewSteps(val steps: Int) : CalibrationResult

    /** A length no one walks with: the distance was set wrong, or not the whole of it was walked. */
    data class Implausible(val stepLengthMeters: Double, val steps: Int) : CalibrationResult

    /** The counter went back (a reboot, a sensor reset) between Start and Stop: nothing to measure. */
    data object CounterReset : CalibrationResult
}

/**
 * The step length from a known distance and the hardware counter's values at Start and at Stop.
 * No GPS: the reader brings the distance, the sensor the steps (VISION.md, "Settings and
 * personalization"). The counter is cumulative, so the two values are enough however the steps
 * were delivered in between: batched in a pocket with the screen off, or one by one on screen.
 */
object StepCalibration {
    /**
     * 30: fewer steps than a 20-metre walk. Below it a missed or extra step moves the result by
     * more than 3%, and a counter that has not woken up to the walk looks like a very long step.
     */
    const val MIN_STEPS: Int = 30

    /** Under this a cadence is mostly the time it took to tap Start and Stop. */
    private const val MIN_CADENCE_MILLIS: Long = 10_000

    private const val MILLIS_PER_MINUTE = 60_000.0

    fun measure(
        step: CalibratedStep,
        distanceMeters: Double,
        startCount: Long,
        endCount: Long,
        durationMillis: Long,
    ): CalibrationResult {
        if (endCount < startCount) return CalibrationResult.CounterReset
        val steps = (endCount - startCount).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        if (steps < MIN_STEPS) return CalibrationResult.TooFewSteps(steps)
        val length = distanceMeters / steps
        if (length !in limits(step)) return CalibrationResult.Implausible(length, steps)
        val cadence = durationMillis.takeIf { it >= MIN_CADENCE_MILLIS }
            ?.let { (steps * MILLIS_PER_MINUTE / it).roundToInt() }
        val mismatch = cadence != null &&
            when (step) {
                CalibratedStep.WALKING -> cadence >= RUNNING_CADENCE
                CalibratedStep.RUNNING -> cadence < RUNNING_CADENCE
            }
        return CalibrationResult.Measured(step, length, steps, distanceMeters, cadence, mismatch)
    }

    /**
     * [profile] with the measured length in place: the walking one is marked as measured, so
     * Settings can say where it came from; the running one is the reader's own, like one typed.
     */
    fun apply(profile: Profile, result: CalibrationResult.Measured): Profile = when (result.step) {
        CalibratedStep.WALKING -> profile.copy(
            stepLengthMode = StepLengthMode.CALIBRATED,
            walkingStepLengthMeters = result.stepLengthMeters,
        )

        CalibratedStep.RUNNING -> profile.copy(runningStepLengthMeters = result.stepLengthMeters)
    }

    /** What the app accepts for each step (`ProfileLimits`): a result outside would not be kept. */
    private fun limits(step: CalibratedStep) = when (step) {
        CalibratedStep.WALKING -> ProfileLimits.WALKING_STEP_LENGTH_M
        CalibratedStep.RUNNING -> ProfileLimits.RUNNING_STEP_LENGTH_M
    }
}
