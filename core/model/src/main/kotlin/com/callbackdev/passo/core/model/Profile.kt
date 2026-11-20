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
 *   [stepLengthMode] is [StepLengthMode.MANUAL] or [StepLengthMode.CALIBRATED]. Kept while a
 *   learnt step is in use, so the way back finds it.
 * @property runningStepLengthMeters the reader's own running step length; when null it follows
 *   the walking one.
 * @property learnedStep the step learnt from the outings with GPS, in use when [stepLengthMode]
 *   is [StepLengthMode.LEARNED] (PLANNING.md §11 Phase 17).
 */
data class Profile(
    val heightMeters: Double? = null,
    val weightKg: Double? = null,
    val sex: Sex? = null,
    val stepLengthMode: StepLengthMode = StepLengthMode.AUTO,
    val walkingStepLengthMeters: Double? = null,
    val runningStepLengthMeters: Double? = null,
    val learnedStep: LearnedStep? = null,
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

    /** Learnt from the outings with GPS, cadence by cadence, and accepted (Phase 17). */
    LEARNED,
}

/**
 * One point of a learnt step: at [cadence] steps a minute, a step of [meters]. The cadence is
 * the mean of the samples that made the point, weighted by their steps, so it is not a round
 * number.
 */
data class StepPoint(val cadence: Double, val meters: Double)

/**
 * The reader's step as their outings with GPS measured it, cadence by cadence
 * (docs/adr/0016-location.md decision 12, PLANNING.md §11 Phase 17). Numbers, never places.
 *
 * @property points the curve, slowest cadence first, never shorter as the cadence rises; those
 *   below the running cadence are the walking part, the others the running part.
 * @property usualCadence the reader's usual walking cadence when it was learnt: where the one
 *   walking length a screen names is read on the curve.
 * @property learntMeters the metres of outings it was learnt from, and [outings] how many.
 * @property throughMillis the start of the latest outing whose samples it holds: a proposal that
 *   comes after it is made by the outings that followed.
 * @property replaced the step in use before it was accepted, for the way back; never
 *   [StepLengthMode.LEARNED].
 * @property running whether its running part is in use: a running length the reader chose after
 *   accepting wins over it, gait by gait.
 */
data class LearnedStep(
    val points: List<StepPoint>,
    val usualCadence: Int,
    val learntMeters: Double,
    val outings: Int,
    val throughMillis: Long,
    val replaced: StepLengthMode = StepLengthMode.AUTO,
    val running: Boolean = true,
)
