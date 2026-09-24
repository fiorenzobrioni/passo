package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.model.Profile

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
}

/** This profile with every out-of-range value dropped. */
fun Profile.sanitized(): Profile = copy(
    heightMeters = heightMeters.within(ProfileLimits.HEIGHT_M),
    weightKg = weightKg.within(ProfileLimits.WEIGHT_KG),
    walkingStepLengthMeters = walkingStepLengthMeters.within(ProfileLimits.WALKING_STEP_LENGTH_M),
    runningStepLengthMeters = runningStepLengthMeters.within(ProfileLimits.RUNNING_STEP_LENGTH_M),
)

private fun Double?.within(range: ClosedFloatingPointRange<Double>): Double? = this?.takeIf { it in range }
