package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.RouteBox
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A route's points as bytes and back (PLANNING.md §11 Phase 15; docs/adr/0016-location.md decision 8). */
class RouteCodecTest {
    /** An hour of walking north-east from Milan's cathedral: a point every three metres, every two seconds. */
    private fun hourOfWalking(): List<RoutePoint> = List(1_800) { index ->
        RoutePoint(
            epochSecond = 1_795_000_000L + index * 2L,
            latitudeE6 = 45_464_200 + index * 19,
            longitudeE6 = 9_191_400 + index * 27,
            altitudeDm = 1_220 + (index % 40) - 20,
            accuracyMeters = 4 + index % 5,
            kind = if (index == 0) RoutePointKind.START else RoutePointKind.MEASURED,
        )
    }

    @Test
    fun `the round trip is exact`() {
        val points = hourOfWalking()

        assertThat(RouteCodec.decode(RouteCodec.encode(points))).isEqualTo(points)
    }

    @Test
    fun `an hour of walking is about ten kilobytes`() {
        val bytes = RouteCodec.encode(hourOfWalking())

        assertThat(bytes.size).isAtMost(12_000)
        assertThat(bytes.size).isAtLeast(6_000)
    }

    @Test
    fun `every kind, a point with no altitude, and places south and west survive`() {
        val points = listOf(
            RoutePoint(1_795_000_000L, -33_868_800, 151_209_300, null, 12, RoutePointKind.START),
            RoutePoint(1_795_000_003L, -33_868_830, 151_209_260, -15, 25, RoutePointKind.MEASURED),
            RoutePoint(1_795_000_090L, -33_869_900, 151_208_100, null, 3, RoutePointKind.LAID),
            RoutePoint(1_795_000_090L, 40_712_800, -74_006_000, 87_000, 0, RoutePointKind.START),
        )

        assertThat(RouteCodec.decode(RouteCodec.encode(points))).isEqualTo(points)
    }

    @Test
    fun `no point at all is a chunk too`() {
        assertThat(RouteCodec.decode(RouteCodec.encode(emptyList()))).isEmpty()
    }

    @Test
    fun `a chunk stands by itself, so a lost one loses only its own points`() {
        val points = hourOfWalking()
        val first = RouteCodec.encode(points.subList(0, 600))
        val third = RouteCodec.encode(points.subList(1_200, 1_800))

        // The second chunk never reached the disk.
        val read = RouteCodec.decode(first) + RouteCodec.decode(third)

        assertThat(read).isEqualTo(points.subList(0, 600) + points.subList(1_200, 1_800))
    }

    @Test
    fun `a chunk cut short gives the points it holds whole, and never throws`() {
        val points = hourOfWalking().take(50)
        val bytes = RouteCodec.encode(points)

        for (length in bytes.indices) {
            val read = RouteCodec.decode(bytes.copyOf(length))
            assertThat(read.size).isAtMost(points.size)
            assertThat(read).isEqualTo(points.take(read.size))
        }
    }

    @Test
    fun `bytes that are no route give no points`() {
        assertThat(RouteCodec.decode(ByteArray(0))).isEmpty()
        assertThat(RouteCodec.decode(byteArrayOf(9, 3, 1, 2))).isEmpty()
        assertThat(RouteCodec.decode(ByteArray(16) { 0xFF.toByte() })).isEmpty()
    }

    @Test
    fun `a route's box holds every point`() {
        val points = hourOfWalking()

        val box = checkNotNull(RouteBox.of(points))

        assertThat(box.southE6).isEqualTo(points.first().latitudeE6)
        assertThat(box.northE6).isEqualTo(points.last().latitudeE6)
        assertThat(box.westE6).isEqualTo(points.first().longitudeE6)
        assertThat(box.eastE6).isEqualTo(points.last().longitudeE6)
        assertThat(RouteBox.of(emptyList())).isNull()
    }

    @Test
    fun `a point says its place in degrees`() {
        val point = RoutePoint(0, 45_464_200, -9_191_400, null, 5, RoutePointKind.START)

        assertThat(point.latitude).isWithin(1e-9).of(45.4642)
        assertThat(point.longitude).isWithin(1e-9).of(-9.1914)
    }
}
