package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.WayId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/** The generated data (`tools/build_ways.py`) decodes, and holds what the screens rely on. */
class WaysDataTest {
    @Test
    fun `the four ways, shortest first`() {
        assertThat(Ways.all.map { it.id }).containsExactlyElementsIn(WayId.entries).inOrder()
        assertThat(Ways.all.map { it.lengthMeters }).isInOrder()
    }

    @Test
    fun `lengths as mapped`() {
        fun km(id: WayId) = Ways.of(id).lengthMeters / 1000
        assertThat(km(WayId.VIA_DEGLI_DEI)).isIn(115..130)
        assertThat(km(WayId.VIA_DI_FRANCESCO)).isIn(400..560)
        assertThat(km(WayId.CAMINO_FRANCES)).isIn(740..800)
        assertThat(km(WayId.VIA_FRANCIGENA)).isIn(950..1_050)
    }

    @Test
    fun `stops run from the start to the end, in order, ending on stages`() {
        for (way in Ways.all) {
            val stops = way.stops
            assertThat(stops.first().distanceMeters).isEqualTo(0)
            assertThat(stops.last().distanceMeters).isEqualTo(way.lengthMeters)
            assertThat(stops.first().stage).isTrue()
            assertThat(stops.last().stage).isTrue()
            assertThat(stops.map { it.distanceMeters }).isInStrictOrder()
            assertThat(stops.map { it.key }.toSet()).hasSize(stops.size)
        }
    }

    @Test
    fun `the line decodes to the way's length, and each stop lies on it`() {
        for (way in Ways.all) {
            val line = way.map.line
            assertThat(line.meters.first()).isEqualTo(0.0)
            assertThat(abs(line.lengthMeters - way.lengthMeters)).isLessThan(1.0)
            assertThat(line.meters.toList()).isInOrder()
            for (stop in way.stops) {
                val point = line.pointAt(stop.distanceMeters.toDouble())
                // The stop's own point is on the full line; the drawn one is simplified.
                assertThat(abs(point.latitude - stop.latitude)).isLessThan(0.01)
                assertThat(abs(point.longitude - stop.longitude)).isLessThan(0.01)
            }
        }
    }

    @Test
    fun `the line fits its frame, and the ground covers it`() {
        for (way in Ways.all) {
            val map = way.map
            val frame = map.frame
            for (i in 0 until map.line.size) {
                assertThat(map.line.latitudes[i] in frame.south..frame.north).isTrue()
                assertThat(map.line.longitudes[i] in frame.west..frame.east).isTrue()
            }
            assertThat(map.land).isNotEmpty()
            assertThat(map.locatorLand).isNotEmpty()
            assertThat(map.locatorLine.size).isAtLeast(2)
        }
    }

    @Test
    fun `the Francigena is its Italian part, from the Great St Bernard to Rome`() {
        val way = Ways.of(WayId.VIA_FRANCIGENA)
        assertThat(way.stops.first().key).isEqualTo("gran_san_bernardo")
        assertThat(way.stops.last().key).isEqualTo("roma_san_pietro")
    }
}
