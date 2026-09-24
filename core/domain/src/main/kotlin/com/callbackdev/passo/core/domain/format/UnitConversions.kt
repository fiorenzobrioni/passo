package com.callbackdev.passo.core.domain.format

import kotlin.math.roundToInt

/**
 * Conversions between the metric values Passo stores and the imperial units it can show. The
 * factors are the exact international definitions (1959 international yard and pound).
 */
object UnitConversions {
    const val METERS_PER_MILE: Double = 1_609.344
    const val METERS_PER_FOOT: Double = 0.3048
    const val METERS_PER_INCH: Double = 0.0254
    const val KG_PER_POUND: Double = 0.453_592_37
    const val INCHES_PER_FOOT: Int = 12

    fun metersToMiles(meters: Double): Double = meters / METERS_PER_MILE

    fun milesToMeters(miles: Double): Double = miles * METERS_PER_MILE

    fun metersToInches(meters: Double): Double = meters / METERS_PER_INCH

    fun inchesToMeters(inches: Double): Double = inches * METERS_PER_INCH

    fun kgToPounds(kg: Double): Double = kg / KG_PER_POUND

    fun poundsToKg(pounds: Double): Double = pounds * KG_PER_POUND

    /** A height in whole feet and inches, the way it is said: 1.75 m is 5 ft 9 in. */
    fun metersToFeetAndInches(meters: Double): FeetAndInches {
        val totalInches = metersToInches(meters).roundToInt()
        return FeetAndInches(feet = totalInches / INCHES_PER_FOOT, inches = totalInches % INCHES_PER_FOOT)
    }

    fun feetAndInchesToMeters(feet: Int, inches: Double): Double = inchesToMeters(feet * INCHES_PER_FOOT + inches)
}

data class FeetAndInches(val feet: Int, val inches: Int)
