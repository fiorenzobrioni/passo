package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.format.UnitConversions

/**
 * A pace: time in motion over distance (docs/adr/0016-location.md, decision 7). Shown for an
 * outing whose distance was measured, where it is a measure; with an estimated distance it would
 * be the cadence in disguise, with the step length's error in it (docs/adr/0009-sessions.md).
 * Never a goal.
 */
object Pace {
    /**
     * The seconds in motion a kilometre takes at this pace, or null under
     * [RouteConstants.PACE_METERS]: over a few dozen metres a pace is the receiver's wander
     * more than the reader's walk.
     */
    fun secondsPerKm(movingMillis: Long, meters: Double): Double? {
        if (meters < RouteConstants.PACE_METERS || movingMillis <= 0) return null
        return movingMillis / MILLIS_PER_SECOND / (meters / METERS_PER_KM)
    }

    /** The same pace by the mile, for a reader who walks in miles. */
    fun secondsPerMile(secondsPerKm: Double): Double = secondsPerKm * UnitConversions.METERS_PER_MILE / METERS_PER_KM

    private const val MILLIS_PER_SECOND = 1_000.0
    private const val METERS_PER_KM = 1_000.0
}
