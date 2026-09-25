package com.callbackdev.passo.core.domain.settings

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.model.UnitSystem
import kotlin.math.roundToInt

/**
 * What the profile pickers offer: the range each one spans and the step it moves by, in the
 * reader's units. Values stay metric; a step of one inch is 2.54 cm, and a value picked in
 * pounds is stored as the kilograms it is.
 *
 * The pickers' ranges are narrower than `ProfileLimits`, which is what the app accepts: the
 * slider covers the people who will use it, and is not stretched to a range so wide that a
 * pixel is two centimetres.
 */
data class InputScale(val range: ClosedFloatingPointRange<Double>, val step: Double) {
    /** [value] on the scale: inside the range and on a whole step, counted from zero. */
    fun snap(value: Double): Double = ((value / step).roundToInt() * step).coerceIn(range)

    fun up(value: Double): Double = snap(snap(value) + step)

    fun down(value: Double): Double = snap(snap(value) - step)
}

object ProfileInputs {
    fun height(units: UnitSystem): InputScale = when (units) {
        UnitSystem.METRIC -> InputScale(1.20..2.20, 0.01)
        UnitSystem.IMPERIAL -> InputScale(inches(48)..inches(86), UnitConversions.METERS_PER_INCH)
    }

    fun weight(units: UnitSystem): InputScale = when (units) {
        UnitSystem.METRIC -> InputScale(35.0..180.0, 1.0)
        UnitSystem.IMPERIAL -> InputScale(pounds(77)..pounds(397), UnitConversions.KG_PER_POUND)
    }

    fun walkingStepLength(units: UnitSystem): InputScale = when (units) {
        UnitSystem.METRIC -> InputScale(0.40..1.10, 0.01)
        UnitSystem.IMPERIAL -> InputScale(inches(16)..inches(43), UnitConversions.METERS_PER_INCH / 2)
    }

    fun runningStepLength(units: UnitSystem): InputScale = when (units) {
        UnitSystem.METRIC -> InputScale(0.50..1.80, 0.01)
        UnitSystem.IMPERIAL -> InputScale(inches(20)..inches(71), UnitConversions.METERS_PER_INCH / 2)
    }

    /**
     * The known distance walked to measure the step (the calibration wizard): 50 m to 2 km by
     * 10 m, or 50 to 2,200 yards by 10. Fifty is about seventy steps, where one step more or
     * less still moves the result by under 2%; a longer walk measures better.
     */
    fun calibrationDistance(units: UnitSystem): InputScale = when (units) {
        UnitSystem.METRIC -> InputScale(50.0..2_000.0, 10.0)
        UnitSystem.IMPERIAL -> InputScale(yards(50)..yards(2_200), UnitConversions.METERS_PER_YARD * 10)
    }

    /**
     * What the distance picker shows first: 100 m, or 100 yards. Long enough to measure well, and
     * the length of a straight on a running track or of a football pitch.
     */
    fun defaultCalibrationDistance(units: UnitSystem): Double = when (units) {
        UnitSystem.METRIC -> 100.0
        UnitSystem.IMPERIAL -> yards(100)
    }

    /** The daily goal: 1,000 to 30,000 in steps of 500. */
    val goal: InputScale = InputScale(1_000.0..30_000.0, 500.0)

    /** What a fresh picker shows before the reader moves it: the average adult. */
    const val DEFAULT_HEIGHT_M: Double = 1.70
    const val DEFAULT_WEIGHT_KG: Double = 70.0

    private fun inches(value: Int) = UnitConversions.inchesToMeters(value.toDouble())

    private fun yards(value: Int) = UnitConversions.yardsToMeters(value.toDouble())

    private fun pounds(value: Int) = UnitConversions.poundsToKg(value.toDouble())
}
