package com.callbackdev.passo.core.tracking

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.altitude.AltitudeConverter
import android.util.Log
import com.callbackdev.passo.core.domain.routes.Fix
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.SystemSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

/**
 * This fix as the domain reads it, **on the steps' clock** (PLANNING.md §11 Phase 15, «One clock
 * for the two streams»): its elapsed time turned to wall time by the same [snapshot] of the two
 * clocks a step sample is read with, never `Location.getTime()`. That is the satellites' time,
 * which can stand seconds from the phone's: steps and fixes laid on two clocks would be
 * interleaved wrongly, and a clock set by hand mid-outing would move one stream and not the other.
 */
internal fun Location.toFix(snapshot: SystemSnapshot): Fix = Fix(
    atMillis = snapshot.wallClockMillis - (snapshot.elapsedRealtimeNanos - elapsedRealtimeNanos) / NANOS_PER_MILLI,
    latitude = latitude,
    longitude = longitude,
    accuracyMeters = if (hasAccuracy()) accuracy else null,
    // As the receiver gives it, above the ellipsoid: turned to mean sea level for the points
    // that are kept, off the main thread ([RouteAltitudes]).
    altitudeMeters = if (hasAltitude()) altitude else null,
    mock = isMock,
)

/**
 * The altitude of a route's points, from the height above the ellipsoid a receiver gives to the
 * one above mean sea level a map states (docs/adr/0016-location.md, the platform's facts:
 * `AltitudeConverter`, from data on the phone, no network). Done for the points that are kept
 * only, not for every fix, and never on the main thread: the first conversion reads from disk.
 */
internal object RouteAltitudes {
    private const val TAG = "RouteAltitudes"
    private const val DECIMETERS = 10.0

    private val converter by lazy { AltitudeConverter() }

    /**
     * [points] with their altitude above mean sea level. A point whose altitude cannot be
     * converted loses it: no altitude is better than one on another datum, some tens of metres
     * off and looking just as sure.
     */
    suspend fun aboveMeanSeaLevel(context: Context, points: List<RoutePoint>): List<RoutePoint> {
        if (points.none { it.altitudeDm != null }) return points
        return withContext(Dispatchers.Default) { points.map { convert(context, it) } }
    }

    private fun convert(context: Context, point: RoutePoint): RoutePoint {
        val ellipsoidal = point.altitudeDm ?: return point
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = point.latitude
            longitude = point.longitude
            altitude = ellipsoidal / DECIMETERS
        }
        val converted = try {
            converter.addMslAltitudeToLocation(context, location)
            location.hasMslAltitude()
        } catch (e: IOException) {
            Log.w(TAG, "The geoid's data could not be read", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "A point the converter does not take", e)
            false
        }
        return point.copy(altitudeDm = if (converted) (location.mslAltitudeMeters * DECIMETERS).roundToInt() else null)
    }
}

private const val NANOS_PER_MILLI = 1_000_000L
