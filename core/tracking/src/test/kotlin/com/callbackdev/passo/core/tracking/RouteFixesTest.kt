package com.callbackdev.passo.core.tracking

import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.SystemSnapshot
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId

/**
 * A fix on the steps' clock (PLANNING.md §11 Phase 15, «One clock for the two streams»), and the
 * altitude of the points kept.
 */
@RunWith(AndroidJUnit4::class)
class RouteFixesTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** The phone's two clocks read together: 10 s since boot, a wall clock that says [wall]. */
    private fun snapshot(wall: Long = 1_795_000_100_000L) = SystemSnapshot(
        bootCount = 1,
        elapsedRealtimeNanos = 10_000_000_000L,
        wallClockMillis = wall,
        zoneId = ZoneId.of("Europe/Rome"),
    )

    private fun location(elapsedNanos: Long, satelliteTime: Long = 0) = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = 45.4642
        longitude = 9.1914
        accuracy = 6f
        altitude = 162.0
        elapsedRealtimeNanos = elapsedNanos
        time = satelliteTime
    }

    @Test
    fun `a fix is laid by its elapsed time, as a step sample is`() {
        // Taken three seconds before the clocks were read.
        val fix = location(elapsedNanos = 7_000_000_000L).toFix(snapshot())

        assertThat(fix.atMillis).isEqualTo(1_795_000_100_000L - 3_000)
        assertThat(fix.latitude).isEqualTo(45.4642)
        assertThat(fix.longitude).isEqualTo(9.1914)
        assertThat(fix.accuracyMeters).isEqualTo(6f)
        assertThat(fix.altitudeMeters).isEqualTo(162.0)
        assertThat(fix.mock).isFalse()
    }

    @Test
    fun `a fix whose satellite time is five seconds from the phone's still falls in its place among the steps`() {
        val honest = location(elapsedNanos = 7_000_000_000L, satelliteTime = 1_795_000_097_000L)
        val drifted = location(elapsedNanos = 7_000_000_000L, satelliteTime = 1_795_000_102_000L)

        // The satellites' time is not read: the two are the same moment on the steps' clock.
        assertThat(drifted.toFix(snapshot()).atMillis).isEqualTo(honest.toFix(snapshot()).atMillis)
    }

    @Test
    fun `a clock set by hand moves the fixes with the steps`() {
        val fix = location(elapsedNanos = 7_000_000_000L)
        val anHourOn = snapshot(wall = 1_795_000_100_000L + 3_600_000)

        assertThat(fix.toFix(anHourOn).atMillis - fix.toFix(snapshot()).atMillis).isEqualTo(3_600_000)
    }

    @Test
    fun `a fix with no accuracy or no altitude says so`() {
        val bare = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 45.0
            longitude = 9.0
            elapsedRealtimeNanos = 9_000_000_000L
        }

        val fix = bare.toFix(snapshot())

        assertThat(fix.accuracyMeters).isNull()
        assertThat(fix.altitudeMeters).isNull()
    }

    @Test
    fun `points with no altitude are left as they are`() = runTest {
        val points = listOf(
            RoutePoint(1_795_000_000, 45_464_200, 9_191_400, null, 5, RoutePointKind.START),
            RoutePoint(1_795_000_002, 45_464_230, 9_191_430, null, 5, RoutePointKind.MEASURED),
        )

        assertThat(RouteAltitudes.aboveMeanSeaLevel(context, points)).isEqualTo(points)
    }

    @Test
    fun `an altitude is turned to mean sea level, or dropped, and never left on another datum`() = runTest {
        val point = RoutePoint(1_795_000_000, 45_464_200, 9_191_400, 1_620, 5, RoutePointKind.START)

        val converted = RouteAltitudes.aboveMeanSeaLevel(context, listOf(point)).single()

        // Where the geoid's data cannot be read (as under test) the altitude is dropped; where
        // it can, the height above the ellipsoid is not what is kept. Either way, never a throw,
        // and everything else of the point is untouched.
        assertThat(converted.copy(altitudeDm = point.altitudeDm)).isEqualTo(point)
        if (converted.altitudeDm != null) assertThat(converted.altitudeDm).isNotEqualTo(point.altitudeDm)
    }
}
