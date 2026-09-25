package com.callbackdev.passo.core.domain.format

import com.callbackdev.passo.core.model.Measure
import com.callbackdev.passo.core.model.MeasureUnit
import com.callbackdev.passo.core.model.UnitSystem
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

/**
 * Formats Passo's numbers for [locale] (digits, separators, grouping) in [units]. Only the
 * number is text here; the unit's symbol is a string resource applied by the UI, so the
 * wording stays translatable.
 *
 * Precision is fixed per magnitude, so a number that updates live does not jump in width:
 * "2.40 km", never "2.4 km" one second and "2.41 km" the next.
 *
 * Thread-safe: a `NumberFormat` is made per call, since it is not.
 */
class MeasureFormatter(private val locale: Locale, val units: UnitSystem) {
    /** A step count, grouped: 8,420 (en) or 8.420 (it). */
    fun steps(steps: Int): String = integer(steps.toLong())

    fun integer(value: Long): String = NumberFormat.getIntegerInstance(locale).format(value)

    /** A share of the goal, e.g. 0.63 as 63%. Not capped: 1.2 is 120%. */
    fun percent(fraction: Double): String = NumberFormat.getPercentInstance(locale)
        .apply { roundingMode = RoundingMode.FLOOR }
        .format(fraction)

    /**
     * Kilometres or miles: two decimals under 10, one under 100, none above. The value is
     * rounded down, so a goal distance is never shown as reached before it is.
     */
    fun distance(meters: Double): Measure {
        val value = when (units) {
            UnitSystem.METRIC -> meters / METERS_PER_KM
            UnitSystem.IMPERIAL -> UnitConversions.metersToMiles(meters)
        }
        val decimals = when {
            abs(value) < 10 -> 2
            abs(value) < 100 -> 1
            else -> 0
        }
        val unit = if (units == UnitSystem.METRIC) MeasureUnit.KILOMETER else MeasureUnit.MILE
        return Measure(decimal(value, decimals, decimals, RoundingMode.FLOOR), unit)
    }

    /**
     * A short stretch, whole metres or yards: the known distance walked to calibrate the step.
     * Rounded to the nearest, since it is a distance the reader chose, not one being covered.
     */
    fun shortDistance(meters: Double): Measure = when (units) {
        UnitSystem.METRIC -> Measure(decimal(meters, 0, 0), MeasureUnit.METER)
        UnitSystem.IMPERIAL -> Measure(decimal(UnitConversions.metersToYards(meters), 0, 0), MeasureUnit.YARD)
    }

    /** Kilocalories, whole: the estimate is not better than that. */
    fun energy(kcal: Double): Measure = Measure(decimal(kcal, 0, 0), MeasureUnit.KILOCALORIE)

    fun minutes(minutes: Int): Measure = Measure(integer(minutes.toLong()), MeasureUnit.MINUTE)

    fun cadence(stepsPerMinute: Int): Measure = Measure(integer(stepsPerMinute.toLong()), MeasureUnit.STEPS_PER_MINUTE)

    /** A body weight: kilograms or pounds, with a decimal only when there is one. */
    fun weight(kg: Double): Measure = when (units) {
        UnitSystem.METRIC -> Measure(decimal(kg, 0, 1), MeasureUnit.KILOGRAM)
        UnitSystem.IMPERIAL -> Measure(decimal(UnitConversions.kgToPounds(kg), 0, 1), MeasureUnit.POUND)
    }

    /** A height: whole centimetres, or feet and inches (two measures, shown side by side). */
    fun height(meters: Double): List<Measure> = when (units) {
        UnitSystem.METRIC -> listOf(Measure(decimal(meters * CM_PER_METER, 0, 0), MeasureUnit.CENTIMETER))

        UnitSystem.IMPERIAL -> {
            val (feet, inches) = UnitConversions.metersToFeetAndInches(meters)
            listOf(
                Measure(integer(feet.toLong()), MeasureUnit.FOOT),
                Measure(integer(inches.toLong()), MeasureUnit.INCH),
            )
        }
    }

    /** A step length: whole centimetres, or inches with a decimal when there is one. */
    fun stepLength(meters: Double): Measure = when (units) {
        UnitSystem.METRIC -> Measure(decimal(meters * CM_PER_METER, 0, 0), MeasureUnit.CENTIMETER)
        UnitSystem.IMPERIAL -> Measure(decimal(UnitConversions.metersToInches(meters), 0, 1), MeasureUnit.INCH)
    }

    private fun decimal(
        value: Double,
        minDecimals: Int,
        maxDecimals: Int,
        rounding: RoundingMode = RoundingMode.HALF_UP,
    ): String = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = minDecimals
        maximumFractionDigits = maxDecimals
        roundingMode = rounding
    }.format(value)

    private companion object {
        const val METERS_PER_KM = 1_000.0
        const val CM_PER_METER = 100.0
    }
}
