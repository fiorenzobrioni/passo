package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.WayId
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/** The generated data (`tools/build_ways.py`) decodes, and holds what the screens rely on. */
class WaysDataTest {
    @Test
    fun `the five ways, shortest first, and the city walks apart`() {
        assertThat(Ways.all.map { it.id })
            .containsExactly(
                WayId.VIA_DEGLI_DEI,
                WayId.CAMINO_PORTUGUES,
                WayId.VIA_DI_FRANCESCO,
                WayId.CAMINO_FRANCES,
                WayId.VIA_FRANCIGENA,
            )
            .inOrder()
        assertThat(Ways.all.map { it.lengthMeters }).isInOrder()
        assertThat(Ways.walks.map { it.id })
            .containsExactly(
                WayId.MILAN_DUOMO_NAVIGLI,
                WayId.ROME_COLOSSEUM_VATICAN,
                WayId.PARIS_VOSGES_EIFFEL,
                WayId.LONDON_PALACE_TOWER,
                WayId.MADRID_DEBOD_RETIRO,
            )
            .inOrder()
        // The owner's five, and no more planned (PLANNING.md §11 Phase 11, later).
        assertThat(Ways.walks.map { it.id.city })
            .containsExactly("milan", "rome", "paris", "london", "madrid")
            .inOrder()
    }

    @Test
    fun `a city walk is an afternoon's walk, every place on it`() {
        for (walk in Ways.walks) {
            assertThat(walk.lengthMeters).isIn(8_000..12_000)
            assertThat(walk.stops.size).isAtLeast(12)
            assertThat(walk.stops.all { it.stage }).isTrue()
            assertThat(walk.map.riverWidthMeters).isGreaterThan(0.0)
            assertThat(walk.map.parks).isNotEmpty()
        }
    }

    @Test
    fun `lengths as mapped`() {
        fun km(id: WayId) = Ways.of(id).lengthMeters / 1000
        assertThat(km(WayId.VIA_DEGLI_DEI)).isIn(115..130)
        assertThat(km(WayId.CAMINO_PORTUGUES)).isIn(230..260)
        assertThat(km(WayId.VIA_DI_FRANCESCO)).isIn(400..560)
        assertThat(km(WayId.CAMINO_FRANCES)).isIn(740..800)
        assertThat(km(WayId.VIA_FRANCIGENA)).isIn(950..1_050)
    }

    @Test
    fun `stops run from the start to the end, in order, ending on stages`() {
        for (way in Ways.all + Ways.walks) {
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
        for (way in Ways.all + Ways.walks) {
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

    @Test
    fun `the Camino Portugués runs from Porto, and ends where the French Way does`() {
        val way = Ways.of(WayId.CAMINO_PORTUGUES)
        assertThat(way.stops.first().key).isEqualTo("porto")
        assertThat(way.stops.last().key).isEqualTo(Ways.of(WayId.CAMINO_FRANCES).stops.last().key)
        assertThat(way.map.locatorFrame).isEqualTo(Ways.of(WayId.CAMINO_FRANCES).map.locatorFrame)
    }
}
