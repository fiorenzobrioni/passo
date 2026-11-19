package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode

/**
 * The values a profile field may hold. Anything outside is treated as not given, so a typo
 * (a weight of 7 kg, a step of 7 m) falls back to the default instead of distorting every
 * estimate.
 */
object ProfileLimits {
    val HEIGHT_M: ClosedFloatingPointRange<Double> = 0.5..2.5
    val WEIGHT_KG: ClosedFloatingPointRange<Double> = 20.0..350.0
    val WALKING_STEP_LENGTH_M: ClosedFloatingPointRange<Double> = 0.2..1.5
    val RUNNING_STEP_LENGTH_M: ClosedFloatingPointRange<Double> = 0.2..2.5

    /** The cadences a learnt step's points may sit at: the learner's bins, 80 to 180. */
    val LEARNT_CADENCE: ClosedFloatingPointRange<Double> = 80.0..180.0

    /** What a step length at [cadence] may be: the walking step's limits below the running cadence. */
    fun stepLength(cadence: Double): ClosedFloatingPointRange<Double> =
        if (cadence < RUNNING_CADENCE) WALKING_STEP_LENGTH_M else RUNNING_STEP_LENGTH_M
}

/**
 * This profile with every out-of-range value dropped. A learnt step is kept only in use and only
 * whole (a walking part, every point a step someone walks with); a learnt mode without one falls
 * back to the height's estimate, as a mode not known does.
 */
fun Profile.sanitized(): Profile {
    val walking = walkingStepLengthMeters.within(ProfileLimits.WALKING_STEP_LENGTH_M)
    val learned = learnedStep?.takeIf { stepLengthMode == StepLengthMode.LEARNED }?.sanitized(walking)
    return copy(
        heightMeters = heightMeters.within(ProfileLimits.HEIGHT_M),
        weightKg = weightKg.within(ProfileLimits.WEIGHT_KG),
        walkingStepLengthMeters = walking,
        runningStepLengthMeters = runningStepLengthMeters.within(ProfileLimits.RUNNING_STEP_LENGTH_M),
        stepLengthMode = if (stepLengthMode == StepLengthMode.LEARNED && learned == null) {
            StepLengthMode.AUTO
        } else {
            stepLengthMode
        },
        learnedStep = learned,
    )
}

/**
 * This learnt step with every point no one walks with dropped, slowest first; null when no
 * walking point is left. A way back to a length that is not there is the height's estimate.
 */
fun LearnedStep.sanitized(walkingStepLengthMeters: Double?): LearnedStep? {
    val kept = points
        .filter { it.cadence.isFinite() && it.cadence in ProfileLimits.LEARNT_CADENCE }
        .filter { it.meters.isFinite() && it.meters in ProfileLimits.stepLength(it.cadence) }
        .sortedBy { it.cadence }
    if (kept.none { it.cadence < RUNNING_CADENCE }) return null
    val back = when (replaced) {
        StepLengthMode.MANUAL, StepLengthMode.CALIBRATED ->
            replaced.takeIf { walkingStepLengthMeters != null } ?: StepLengthMode.AUTO

        StepLengthMode.AUTO, StepLengthMode.LEARNED -> StepLengthMode.AUTO
    }
    return copy(
        points = kept,
        usualCadence = usualCadence.coerceIn(1, RUNNING_CADENCE - 1),
        learntMeters = learntMeters.takeIf { it.isFinite() && it >= 0 } ?: 0.0,
        outings = outings.coerceAtLeast(0),
        replaced = back,
    )
}

private fun Double?.within(range: ClosedFloatingPointRange<Double>): Double? = this?.takeIf { it in range }
