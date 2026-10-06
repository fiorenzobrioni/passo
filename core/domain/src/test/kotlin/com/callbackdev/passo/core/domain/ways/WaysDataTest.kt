package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.Continent
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot

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
                WayId.BERLIN_WALL_VICTORY,
                WayId.VIENNA_BELVEDERE_PRATER,
                WayId.PORTO_SE_PILAR,
                WayId.AMSTERDAM_CENTRAAL_WESTERKERK,
                WayId.PRAGUE_CASTLE_WENCESLAS,
                WayId.LIMA_SAN_MARTIN_RESERVA,
                WayId.CUSCO_ARMAS_QORIKANCHA,
                WayId.NEW_YORK_PARK_BRIDGE,
                WayId.RIO_CENTRO_SUGARLOAF,
                WayId.MEXICO_CITY_ZOCALO_CHAPULTEPEC,
                WayId.BUENOS_AIRES_MAYO_RECOLETA,
                WayId.SAN_FRANCISCO_FERRY_PALACE,
                WayId.QUEBEC_PARLEMENT_BASSE_VILLE,
                WayId.HAVANA_CAPITOLIO_PAULA,
                WayId.CARTAGENA_RELOJ_SAN_FELIPE,
                WayId.TOKYO_SENSOJI_PALACE,
                WayId.SYDNEY_LUNA_PARK_GARDEN,
            )
            .inOrder()
        // One walk a city for now (PLANNING.md §11 Phase 11, later).
        assertThat(Ways.walks.map { it.id.city })
            .containsExactly(
                "milan", "rome", "paris", "london", "madrid", "berlin", "vienna", "porto", "amsterdam", "prague",
                "lima", "cusco", "new_york", "rio", "mexico_city", "buenos_aires", "san_francisco", "quebec",
                "havana", "cartagena", "tokyo", "sydney",
            )
            .inOrder()
    }

    @Test
    fun `a city walk is short or long, every place on it`() {
        // Two lengths (ADR 0015 decision 10): about 5 km, an hour in one outing, or about 10 km.
        for (walk in Ways.walks) {
            val short = walk.lengthMeters < 7_000
            assertThat(walk.lengthMeters).isIn(if (short) 4_000..6_000 else 8_000..12_000)
            assertThat(walk.stops.size).isAtLeast(if (short) 7 else 12)
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
        // The short walks.
        assertThat(Ways.walks.filter { it.lengthMeters < 7_000 }.map { it.id })
            .containsExactly(
                WayId.PORTO_SE_PILAR,
                WayId.AMSTERDAM_CENTRAAL_WESTERKERK,
                WayId.PRAGUE_CASTLE_WENCESLAS,
                WayId.QUEBEC_PARLEMENT_BASSE_VILLE,
                WayId.HAVANA_CAPITOLIO_PAULA,
                WayId.CARTAGENA_RELOJ_SAN_FELIPE,
            )
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

    // --- Continents (ADR 0015 decision 12) ------------------------------------------------------

    @Test
    fun `every walk has its continent, a way none, and each continent its cities`() {
        for (way in Ways.all) assertThat(way.id.continent).isNull()
        for (walk in Ways.walks) assertThat(walk.id.continent).isNotNull()
        assertThat(WayId.entries.filter { it.kind == WayKind.WALK }.map { it.continent }.toSet())
            .containsExactlyElementsIn(Continent.entries)
        assertThat(Ways.walksIn(Continent.EUROPE).map { it.id.city })
            .containsExactly(
                "milan", "rome", "paris", "london", "madrid", "berlin", "vienna", "porto", "amsterdam", "prague",
            )
            .inOrder()
        assertThat(
            Ways.walksIn(Continent.AMERICAS).map {
                it.id.city
            },
        ).containsExactly(
            "lima", "cusco", "new_york", "rio", "mexico_city", "buenos_aires", "san_francisco", "quebec",
            "havana", "cartagena",
        )
            .inOrder()
        assertThat(Ways.walksIn(Continent.ASIA_OCEANIA).map { it.id.city })
            .containsExactly("tokyo", "sydney")
            .inOrder()
    }

    @Test
    fun `each city lies inside its continent's frame, with land behind it`() {
        for (continent in Continent.entries) {
            val map = Continents.map(continent)
            val frame = map.frame
            assertThat(map.land).isNotEmpty()
            for (walk in Ways.walksIn(continent)) {
                val start = walk.stops.first()
                assertThat(start.latitude in frame.south..frame.north).isTrue()
                assertThat(start.longitude in frame.west..frame.east).isTrue()
                // On land as drawn, or on a coast within a point's width of it (the continent's
                // shores are simplified by some 13 km, under a dp): a city out at sea would mean
                // the frame or the data is wrong.
                val onLand = map.land.any { it.contains(start.latitude, start.longitude) }
                val nearShore = map.land.minOf { it.kilometresTo(start.latitude, start.longitude) } < COAST_SLACK_KM
                assertThat(onLand || nearShore).isTrue()
            }
        }
    }

    @Test
    fun `a city on the sea is cut out of it, its route and its places on land`() {
        val coastal = Ways.walks.filter { it.map.sea }.map { it.id }
        assertThat(coastal)
            .containsExactly(
                WayId.NEW_YORK_PARK_BRIDGE,
                WayId.RIO_CENTRO_SUGARLOAF,
                WayId.BUENOS_AIRES_MAYO_RECOLETA,
                WayId.SAN_FRANCISCO_FERRY_PALACE,
                WayId.HAVANA_CAPITOLIO_PAULA,
                WayId.CARTAGENA_RELOJ_SAN_FELIPE,
            )
        for (id in coastal) {
            val map = Ways.of(id).map
            // The land from the coastline, not the whole ground: a few shores and islands.
            assertThat(map.land.size).isGreaterThan(1)
            // A place on a pier stands over the water the coastline leaves out (San Francisco's
            // Ferry Building, some 110 m out): a few steps from the land, never out at sea.
            for (stop in Ways.of(id).stops) {
                val onLand = map.land.any { it.contains(stop.latitude, stop.longitude) }
                val onPier = map.land.minOf { it.kilometresTo(stop.latitude, stop.longitude) } < PIER_SLACK_KM
                assertThat(onLand || onPier).isTrue()
            }
        }
        // Every other city stands on its ground, one rectangle with no sea around it.
        for (walk in Ways.walks.filter { !it.map.sea }) assertThat(walk.map.land).hasSize(1)
    }

    /** The distance from the point to the ring's nearest edge, near enough on a small scale. */
    private fun GeoPath.kilometresTo(latitude: Double, longitude: Double): Double {
        val kx = 111.32 * cos(Math.toRadians(latitude))
        val ky = 110.57
        var best = Double.MAX_VALUE
        for (i in 0 until size) {
            val j = (i + 1) % size
            val ax = (longitudes[i] - longitude) * kx
            val ay = (latitudes[i] - latitude) * ky
            val bx = (longitudes[j] - longitude) * kx
            val by = (latitudes[j] - latitude) * ky
            val dx = bx - ax
            val dy = by - ay
            val t = if (dx == 0.0 && dy == 0.0) 0.0 else (-(ax * dx + ay * dy) / (dx * dx + dy * dy)).coerceIn(0.0, 1.0)
            best = minOf(best, hypot(ax + t * dx, ay + t * dy))
        }
        return best
    }

    /** Whether the closed ring holds the point: even-odd, in degrees, as the map fills it. */
    private fun GeoPath.contains(latitude: Double, longitude: Double): Boolean {
        var inside = false
        var j = size - 1
        for (i in 0 until size) {
            val crosses = (latitudes[i] > latitude) != (latitudes[j] > latitude)
            if (crosses) {
                val at = longitudes[i] +
                    (latitude - latitudes[i]) / (latitudes[j] - latitudes[i]) * (longitudes[j] - longitudes[i])
                if (longitude < at) inside = !inside
            }
            j = i
        }
        return inside
    }

    private companion object {
        const val COAST_SLACK_KM = 30.0
        const val PIER_SLACK_KM = 0.15
    }
}
