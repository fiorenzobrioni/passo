package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.pow

/**
 * The heights of a route's points as they are written, and the pressure readings they are given
 * them from (PLANNING.md §11 Phase 16, «The climb»).
 */
class RouteHeightsTest {
    private fun pressure(meters: Double): Float = (1013.25 * (1.0 - meters / 44_330.0).pow(5.255)).toFloat()

    private fun point(second: Long, satelliteMeters: Double?, kind: RoutePointKind = RoutePointKind.MEASURED) =
        RoutePoint(
            epochSecond = second,
            latitudeE6 = 45_464_200 + second.toInt() * 12,
            longitudeE6 = 9_190_000,
            altitudeDm = satelliteMeters?.let { (it * 10).toInt() },
            accuracyMeters = 5,
            kind = kind,
        )

    /** A reading a second, from [from] to [to], at the true height of each second. */
    private fun PressureTrack.read(from: Int, to: Int, truth: (Int) -> Double) {
        for (second in from..to) add(second * 1_000L, pressure(truth(second)))
    }

    private fun List<Climbed>.up() = sumOf { it.upMeters }

    private fun List<Climbed>.down() = sumOf { it.downMeters }

    @Test
    fun `a point's pressure is the mean of the readings around its second`() {
        val track = PressureTrack()
        track.add(8_000, 1000.0f)
        track.add(9_000, 1000.2f)
        track.add(10_000, 1000.4f)
        track.add(11_000, 1000.6f)
        track.add(14_000, 1002.0f)

        assertThat(track.at(10_000)).isWithin(1e-3f).of(1000.3f)
        assertThat(track.latestMillis).isEqualTo(14_000)
        // No reading within two and a half seconds: none to give.
        assertThat(track.at(20_000)).isNull()
        assertThat(PressureTrack().at(0)).isNull()
    }

    @Test
    fun `readings older than twenty minutes are let go`() {
        val track = PressureTrack()
        track.add(0, 1000f)
        track.add(PressureTrack.KEEP_MILLIS + 1_000, 1001f)

        assertThat(track.at(0)).isNull()
        assertThat(track.at(PressureTrack.KEEP_MILLIS + 1_000)).isEqualTo(1001f)
    }

    @Test
    fun `a hill - each point keeps its blended height, and the climb is told as it is walked`() {
        val heights = RouteHeights(barometer = true)
        val track = PressureTrack()
        // Up 60 m in ten minutes; the satellites say the same, eight metres too high.
        val truth = { second: Int -> 100.0 + second / 10.0 }
        track.read(0, 600, truth)
        val points = (0..600 step 2).map { point(it.toLong(), truth(it) + 8.0) }

        val result = heights.take(points, track, nowMillis = 600_000)

        assertThat(result.points).hasSize(points.size)
        assertThat(heights.source).isEqualTo(AltitudeSource.BAROMETER)
        assertThat(result.climbed.up()).isWithin(4.0).of(60.0)
        assertThat(result.climbed.down()).isEqualTo(0.0)
        // The first entry says only that the height is known.
        assertThat(result.climbed.first()).isEqualTo(Climbed(0, 0.0, 0.0))
        // Each climb at the second it was walked: three metres are thirty seconds of this hill.
        assertThat(result.climbed[1].atMillis.toDouble()).isWithin(4_000.0).of(30_000.0)
        val last = checkNotNull(result.points.last().altitudeDm) / 10.0
        assertThat(last - checkNotNull(result.points.first().altitudeDm) / 10.0).isWithin(1.0).of(60.0)
    }

    @Test
    fun `points wait for the readings that are on their way`() {
        val heights = RouteHeights(barometer = true)
        val track = PressureTrack()
        track.read(0, 30) { 100.0 }
        // The step batch brought points up to the 60th second; the pressure's batch is behind.
        val points = (0..60 step 2).map { point(it.toLong(), 100.0) }

        val first = heights.take(points, track, nowMillis = 60_000)

        assertThat(first.points.last().epochSecond).isEqualTo(32)
        assertThat(heights.hasWaiting).isTrue()
        // The readings arrive: the points that waited are given theirs, with none from the
        // satellites alone.
        track.read(31, 60) { 100.0 }
        val second = heights.take(emptyList(), track, nowMillis = 61_000)
        assertThat(second.points.map { it.epochSecond }).containsExactlyElementsIn((34L..60L step 2)).inOrder()
        assertThat(heights.hasWaiting).isFalse()
        assertThat((first.climbed + second.climbed).up()).isEqualTo(0.0)
    }

    @Test
    fun `a sensor that does not answer keeps no point waiting past a minute and a half`() {
        val heights = RouteHeights(barometer = true)
        val points = (0..60 step 2).map { point(it.toLong(), 100.0) }

        assertThat(heights.take(points, PressureTrack(), nowMillis = 60_000).points).isEmpty()
        val late = heights.take(emptyList(), PressureTrack(), nowMillis = 60_000 + RouteHeights.MAX_WAIT_MILLIS + 1)

        // Written with the satellites' height: a route is not held for a sensor.
        assertThat(late.points).hasSize(points.size)
        assertThat(late.points.all { it.altitudeDm != null }).isTrue()
    }

    @Test
    fun `the outing's end takes every point, with the readings there are`() {
        val heights = RouteHeights(barometer = true)
        val track = PressureTrack()
        track.read(0, 30) { 100.0 }
        val points = (0..60 step 2).map { point(it.toLong(), 100.0) }

        val result = heights.take(points, track, nowMillis = 60_000, all = true)

        assertThat(result.points).hasSize(points.size)
        assertThat(heights.hasWaiting).isFalse()
    }

    @Test
    fun `a phone with no barometer - no waiting, the satellites' height, the higher threshold`() {
        val heights = RouteHeights(barometer = false)
        val truth = { second: Int -> 100.0 + second / 10.0 }
        val points = (0..600 step 2).map { point(it.toLong(), truth(it)) }

        val result = heights.take(points, PressureTrack(), nowMillis = 600_000)

        assertThat(result.points).hasSize(points.size)
        assertThat(heights.source).isEqualTo(AltitudeSource.SATELLITES)
        assertThat(result.climbed.up()).isWithin(15.0).of(60.0)
        // Counted ten metres at a time, not three.
        assertThat(result.climbed.filter { it.upMeters > 0 }.all { it.upMeters >= 10.0 }).isTrue()
    }

    @Test
    fun `a pause, a funicular, a walk again - the ride is not a climb`() {
        val heights = RouteHeights(barometer = true)
        val track = PressureTrack()
        track.read(0, 120) { 100.0 }
        track.read(600, 720) { 180.0 }
        val before = (0..120 step 2).map { point(it.toLong(), 100.0) }
        val after = (600..720 step 2).mapIndexed { index, second ->
            point(second.toLong(), 180.0, if (index == 0) RoutePointKind.START else RoutePointKind.MEASURED)
        }

        val result = heights.take(before + after, track, nowMillis = 720_000)

        assertThat(result.climbed.up()).isEqualTo(0.0)
        assertThat(result.climbed.down()).isEqualTo(0.0)
        // The points after it stand where they are: eighty metres higher.
        assertThat(checkNotNull(result.points.last().altitudeDm) / 10.0).isWithin(2.0).of(180.0)
    }

    @Test
    fun `fixes with no altitude at all - points with none, and no climb`() {
        val heights = RouteHeights(barometer = true)
        val track = PressureTrack()
        track.read(0, 60) { 100.0 }
        val points = (0..60 step 2).map { point(it.toLong(), satelliteMeters = null) }

        val result = heights.take(points, track, nowMillis = 60_000)

        assertThat(result.points.all { it.altitudeDm == null }).isTrue()
        assertThat(result.climbed).isEmpty()
        assertThat(heights.source).isNull()
    }
}
