package com.callbackdev.passo.core.domain.routes

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * One position from the satellite receiver, as the domain reads it.
 *
 * @property atMillis when it was taken, **on the steps' clock**: the fix's elapsed time turned to
 *   wall time the way a step sample's is, never the satellites' own time, which can stand
 *   seconds from the phone's. Steps and fixes are interleaved on this one clock.
 * @property accuracyMeters the radius the receiver gives it; null when it gives none.
 * @property altitudeMeters above the WGS84 ellipsoid, as the receiver gives it (tens of metres
 *   from the height above mean sea level: the geoid's separation); null when the fix has none.
 *   Only the points kept are turned to mean sea level, as they are written.
 * @property mock set by a test provider (the system's mock-location setting), not by satellites.
 */
data class Fix(
    val atMillis: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val altitudeMeters: Double? = null,
    val mock: Boolean = false,
)

/** Distances on the ground. */
object Geodesy {
    /** The mean radius of the Earth (IUGG): good to a few parts in a thousand anywhere on it. */
    private const val EARTH_RADIUS_METERS = 6_371_008.8

    /** The great-circle distance between two points given in degrees (the haversine formula). */
    fun meters(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val phi1 = Math.toRadians(latitude1)
        val phi2 = Math.toRadians(latitude2)
        val dPhi = phi2 - phi1
        val dLambda = Math.toRadians(longitude2 - longitude1)
        val h = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    fun meters(from: Fix, to: Fix): Double = meters(from.latitude, from.longitude, to.latitude, to.longitude)
}
