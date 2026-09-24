package com.callbackdev.passo.core.model

/**
 * What the estimates are computed from (PLANNING.md §5, VISION.md "Settings"). Every field is
 * optional: a missing one falls back to a documented default in `MetricsConstants`, so the app
 * counts and estimates from the first minute without asking anything.
 *
 * Lengths are in metres and weights in kilograms whatever units the reader chose: the units
 * are a way of showing numbers, never a way of storing them.
 *
 * @property sex used only to pick the default step length from the height.
 * @property walkingStepLengthMeters the reader's own walking step length, read when
 *   [stepLengthMode] is not [StepLengthMode.AUTO].
 * @property runningStepLengthMeters the reader's own running step length; when null it follows
 *   the walking one.
 */
data class Profile(
    val heightMeters: Double? = null,
    val weightKg: Double? = null,
    val sex: Sex? = null,
    val stepLengthMode: StepLengthMode = StepLengthMode.AUTO,
    val walkingStepLengthMeters: Double? = null,
    val runningStepLengthMeters: Double? = null,
)

enum class Sex {
    FEMALE,
    MALE,
}

/** Where the walking step length comes from. */
enum class StepLengthMode {
    /** Estimated from the height (or a default when the height is not known). */
    AUTO,

    /** Typed in by the reader. */
    MANUAL,

    /** Measured by walking a known distance (the calibration wizard, Phase 7). */
    CALIBRATED,
}
