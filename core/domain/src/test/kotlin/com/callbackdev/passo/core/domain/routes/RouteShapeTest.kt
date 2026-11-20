package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.Measure
import com.callbackdev.passo.core.model.MeasureUnit
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A route as it is drawn (PLANNING.md §11 Phase 16, «RouteMapView»). */
class RouteShapeTest {
    /** A point [north] and [east] metres from Milan's cathedral, [second] seconds in. */
    private fun point(
        second: Long,
        north: Double,
        east: Double = 0.0,
        kind: RoutePointKind = RoutePointKind.MEASURED,
        altitude: Double? = null,
    ) = RoutePoint(
        epochSecond = second,
        latitudeE6 = (45_464_200 + north / 0.11132).toInt(),
        longitudeE6 = (9_191_400 + east / 0.07808).toInt(),
        altitudeDm = altitude?.let { (it * 10).toInt() },
        accuracyMeters = 5,
        kind = kind,
    )

    /** A walk north, three metres every two seconds. */
    private fun north(meters: Int, from: Int = 0, startSecond: Long = 0) = (from..meters step 3).mapIndexed { i, at ->
        point(startSecond + i * 2L, at.toDouble(), kind = if (i == 0) RoutePointKind.START else RoutePointKind.MEASURED)
    }

    private fun shape(points: List<RoutePoint>) = checkNotNull(RouteShape.of(points))

    @Test
    fun `fewer than two points draw nothing`() {
        assertThat(RouteShape.of(emptyList())).isNull()
        assertThat(RouteShape.of(listOf(point(0, 0.0)))).isNull()
    }

    @Test
    fun `one walk is one line, as long as it was walked`() {
        val shape = shape(north(900))

        assertThat(shape.lines).containsExactly(RouteLine(0, 300, laid = false))
        assertThat(shape.lengthMeters).isWithin(2.0).of(900.0)
    }

    @Test
    fun `a pause of ten minutes - no line drawn across a place not walked`() {
        val after = north(900, from = 600, startSecond = 1_000)
        val shape = shape(north(300) + after)

        assertThat(shape.lines).containsExactly(RouteLine(0, 100, laid = false), RouteLine(101, 201, laid = false))
        // The 300 m between the two stretches were not walked, and are not in its length.
        assertThat(shape.lengthMeters).isWithin(2.0).of(600.0)
    }

    @Test
    fun `a tunnel is a line of its own, laid from the steps`() {
        val before = north(300)
        val far = point(400, 600.0, kind = RoutePointKind.LAID)
        val after = (603..900 step 3).mapIndexed { i, at -> point(402 + i * 2L, at.toDouble()) }
        val shape = shape(before + far + after)

        assertThat(shape.lines).containsExactly(
            RouteLine(0, 100, laid = false),
            RouteLine(100, 101, laid = true),
            RouteLine(101, 201, laid = false),
        ).inOrder()
        assertThat(shape.lengthMeters).isWithin(2.0).of(900.0)
    }

    @Test
    fun `the frame holds the route with room around it`() {
        val shape = shape(north(900))
        val frame = shape.frame

        assertThat(frame.south).isLessThan(shape.start.latitude)
        assertThat(frame.north).isGreaterThan(shape.end.latitude)
        // A tenth of its span, shared by its two sides.
        assertThat((frame.north - frame.south) * 111_320).isWithin(5.0).of(990.0)
    }

    @Test
    fun `a walk around the block is not drawn as large as a walk across a city`() {
        val frame = shape(north(30)).frame

        assertThat((frame.north - frame.south) * 111_320).isWithin(1.0).of(RouteShape.MIN_SPAN_METERS)
        assertThat((frame.east - frame.west) * 78_080).isWithin(5.0).of(RouteShape.MIN_SPAN_METERS)
    }

    @Test
    fun `it says which way it went, from where it began to where it ended`() {
        assertThat(shape(north(900)).heading).isEqualTo(Compass.NORTH)
        assertThat(shape(north(900).reversed()).heading).isEqualTo(Compass.SOUTH)
        assertThat(shape(listOf(point(0, 0.0), point(600, 10.0, east = 800.0))).heading).isEqualTo(Compass.EAST)
        assertThat(shape(listOf(point(0, 0.0), point(600, -500.0, east = -500.0))).heading)
            .isEqualTo(Compass.SOUTH_WEST)
        assertThat(Compass.SOUTH_WEST.opposite).isEqualTo(Compass.NORTH_EAST)
    }

    @Test
    fun `a round that ends where it began has no heading`() {
        val out = north(900)
        val back = out.reversed().mapIndexed { i, p ->
            p.copy(epochSecond = 700 + i * 2L, kind = RoutePointKind.MEASURED)
        }

        assertThat(shape(out + back).heading).isNull()
    }

    @Test
    fun `the stops are the pauses, and the stops long enough for the receiver to rest`() {
        val first = north(300)
        // Three minutes on a bench, the line going on from where it stopped.
        val second = (303..600 step 3).mapIndexed { i, at -> point(380 + i * 2L, at.toDouble()) }
        // Then a pause: a stretch begins anew.
        val third = north(900, from = 603, startSecond = 2_000)

        assertThat(shape(first).stops).isEqualTo(0)
        assertThat(shape(first + second).stops).isEqualTo(1)
        assertThat(shape(first + second + third).stops).isEqualTo(2)
    }

    @Test
    fun `where the reader was at a moment - a kilometre's bead on the line`() {
        val shape = shape(north(900))

        // Four hundred seconds in, at 1.5 m/s: 600 m north.
        val at = shape.at(400_000)
        assertThat((at.latitude - shape.start.latitude) * 111_320).isWithin(2.0).of(600.0)
        // Before the start and after the end: its ends.
        assertThat(shape.at(-5_000).latitude).isEqualTo(shape.start.latitude)
        assertThat(shape.at(9_000_000).latitude).isEqualTo(shape.end.latitude)
    }

    @Test
    fun `during a pause the reader was where the pause began, not between its two ends`() {
        val shape = shape(north(300) + north(900, from = 600, startSecond = 1_000))

        assertThat((shape.at(600_000).latitude - shape.start.latitude) * 111_320).isWithin(2.0).of(300.0)
    }

    @Test
    fun `the profile is each known altitude against the distance walked to it`() {
        val points = listOf(
            point(0, 0.0, kind = RoutePointKind.START, altitude = 120.0),
            point(60, 90.0, altitude = 126.0),
            point(120, 180.0),
            point(180, 270.0, altitude = 131.0),
        )

        val profile = shape(points).profile

        assertThat(profile.map { it.second }).containsExactly(120.0, 126.0, 131.0).inOrder()
        assertThat(profile[1].first).isWithin(1.0).of(90.0)
        assertThat(profile[2].first).isWithin(1.0).of(270.0)
    }

    @Test
    fun `it knows whether some stretch is drawn dashed`() {
        val far = point(400, 600.0, kind = RoutePointKind.LAID)
        val after = (603..900 step 3).mapIndexed { i, at -> point(402 + i * 2L, at.toDouble()) }

        assertThat(shape(north(900)).hasLaid).isFalse()
        assertThat(shape(north(300) + far + after).hasLaid).isTrue()
    }

    @Test
    fun `the scale bar is the longest round length that fits, in the reader's units`() {
        assertThat(ScaleBar.of(260.0, UnitSystem.METRIC)).isEqualTo(ScaleBar(200.0, Measure("200", MeasureUnit.METER)))
        assertThat(ScaleBar.of(1_400.0, UnitSystem.METRIC))
            .isEqualTo(ScaleBar(1_000.0, Measure("1", MeasureUnit.KILOMETER)))
        assertThat(ScaleBar.of(260.0, UnitSystem.IMPERIAL)?.label).isEqualTo(Measure("200", MeasureUnit.YARD))
        assertThat(ScaleBar.of(4_000.0, UnitSystem.IMPERIAL)?.label).isEqualTo(Measure("2", MeasureUnit.MILE))
        assertThat(ScaleBar.of(4_000.0, UnitSystem.IMPERIAL)?.meters).isWithin(0.01).of(3_218.69)
        assertThat(ScaleBar.of(5.0, UnitSystem.METRIC)).isNull()
    }
}
