package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.domain.ways.GeoBox
import com.callbackdev.passo.core.domain.ways.GeoPoint
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.Measure
import com.callbackdev.passo.core.model.MeasureUnit
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.UnitSystem
import kotlin.math.atan2
import kotlin.math.cos

/** A point of the compass, for the one sentence a route is told with. */
enum class Compass {
    NORTH,
    NORTH_EAST,
    EAST,
    SOUTH_EAST,
    SOUTH,
    SOUTH_WEST,
    WEST,
    NORTH_WEST,
    ;

    val opposite: Compass get() = entries[(ordinal + entries.size / 2) % entries.size]
}

/**
 * One line of a route as it is drawn: the points from [from] to [to], joined; [laid] where the
 * stretch was walked with no fix and laid from the steps (a tunnel, an arcade), which is drawn
 * dashed, the honest mark.
 */
data class RouteLine(val from: Int, val to: Int, val laid: Boolean)

/**
 * An outing's route as it is drawn (PLANNING.md §11 Phase 16, «RouteMapView»;
 * docs/adr/0016-location.md, decision 11): the frame it fits in, its lines (none across a pause
 * or a ride), where it began and ended, and what one sentence can say of it. Pure: the map is
 * Canvas code that only draws this.
 */
class RouteShape private constructor(val points: List<RoutePoint>) {
    val start: RoutePoint get() = points.first()
    val end: RoutePoint get() = points.last()

    /**
     * The frame the route fits in, with a margin, and never smaller than [MIN_SPAN_METERS]
     * across: a walk around the block is not drawn as large as a walk across a city.
     */
    val frame: GeoBox by lazy {
        val south = points.minOf { it.latitude }
        val north = points.maxOf { it.latitude }
        val west = points.minOf { it.longitude }
        val east = points.maxOf { it.longitude }
        val middle = (south + north) / 2
        val metersPerLongitude = METERS_PER_DEGREE * cos(Math.toRadians(middle)).coerceAtLeast(MIN_COSINE)
        val halfHeight = maxOf((north - south) * (1 + MARGIN), MIN_SPAN_METERS / METERS_PER_DEGREE) / 2
        val halfWidth = maxOf((east - west) * (1 + MARGIN), MIN_SPAN_METERS / metersPerLongitude) / 2
        val centre = (west + east) / 2
        GeoBox(middle - halfHeight, centre - halfWidth, middle + halfHeight, centre + halfWidth)
    }

    /**
     * The route's lines, in order: a new one at each point where a stretch begins, and wherever
     * measured gives way to laid from the steps or back.
     */
    val lines: List<RouteLine> by lazy {
        val out = ArrayList<RouteLine>()
        var from = 0
        for (index in 1 until points.size) {
            val kind = points[index].kind
            val laid = kind == RoutePointKind.LAID
            val before = out.lastOrNull()?.takeIf { it.to == index - 1 }
            when {
                // No line reaches a point where a stretch begins.
                kind == RoutePointKind.START -> from = index

                before != null && before.laid == laid -> out[out.lastIndex] = before.copy(to = index)

                else -> out += RouteLine(maxOf(from, index - 1), index, laid)
            }
        }
        out
    }

    /** The length of the lines as drawn: the route's own geometry, not the outing's fused distance. */
    val lengthMeters: Double by lazy {
        lines.sumOf { line -> (line.from until line.to).sumOf { meters(points[it], points[it + 1]) } }
    }

    /**
     * Which way it went, from where it began to where it ended; null for a loop, which ends
     * within [LOOP_SHARE] of its length (or [LOOP_METERS]) from its start.
     */
    val heading: Compass? by lazy {
        val apart = meters(start, end)
        if (apart <= maxOf(LOOP_METERS, lengthMeters * LOOP_SHARE)) {
            null
        } else {
            val north = end.latitude - start.latitude
            val east = (end.longitude - start.longitude) * cos(Math.toRadians((start.latitude + end.latitude) / 2))
            val degrees = (Math.toDegrees(atan2(east, north)) + FULL_TURN) % FULL_TURN
            Compass.entries[((degrees + SECTOR / 2) / SECTOR).toInt() % Compass.entries.size]
        }
    }

    /**
     * How many times the reader stopped on the way: the pauses and the rides (a stretch begins
     * anew), and the stops long enough for the receiver to rest ([STOP_SECONDS] between two
     * points of one stretch).
     */
    val stops: Int by lazy {
        (1 until points.size).count { index ->
            points[index].kind == RoutePointKind.START ||
                points[index].epochSecond - points[index - 1].epochSecond >= STOP_SECONDS
        }
    }

    /**
     * Where the reader was at [atMillis], between the two points around that moment: where a
     * kilometre's bead stands. Clamped to the route's ends.
     */
    fun at(atMillis: Long): GeoPoint {
        val second = atMillis / MILLIS_PER_SECOND.toDouble()
        val after = points.indexOfFirst { it.epochSecond >= second }
        if (after <= 0) return if (after == 0) start.geo() else end.geo()
        val a = points[after - 1]
        val b = points[after]
        // Across a break (a pause, a ride) the reader was at one end or the other, not between.
        if (b.kind == RoutePointKind.START) return a.geo()
        val span = (b.epochSecond - a.epochSecond).toDouble()
        val t = if (span <= 0) 1.0 else ((second - a.epochSecond) / span).coerceIn(0.0, 1.0)
        return GeoPoint(a.latitude + t * (b.latitude - a.latitude), a.longitude + t * (b.longitude - a.longitude))
    }

    /** The altitude of each point that has one, against the distance walked to it along the lines. */
    val profile: List<Pair<Double, Double>> by lazy {
        val out = ArrayList<Pair<Double, Double>>()
        var walked = 0.0
        points.forEachIndexed { index, point ->
            if (index > 0 && point.kind != RoutePointKind.START) walked += meters(points[index - 1], point)
            point.altitudeDm?.let { out += walked to it / DECIMETERS }
        }
        out
    }

    companion object {
        /** The shape of [points]; null for fewer than two, which draw no line. */
        fun of(points: List<RoutePoint>): RouteShape? = if (points.size < 2) null else RouteShape(points)

        private fun RoutePoint.geo() = GeoPoint(latitude, longitude)

        private fun meters(a: RoutePoint, b: RoutePoint) =
            Geodesy.meters(a.latitude, a.longitude, b.latitude, b.longitude)

        /** The frame is never smaller than this across: two blocks of a city. */
        const val MIN_SPAN_METERS: Double = 250.0

        /** Room around the route inside its frame: a tenth of its span, shared by the two sides. */
        private const val MARGIN = 0.1

        /** A route that ends this near its start, or within a tenth of its length, is a loop. */
        const val LOOP_METERS: Double = 150.0
        private const val LOOP_SHARE = 0.1

        /** Two minutes between two points: the receiver rested, so the reader had stopped. */
        const val STOP_SECONDS: Long = 120

        private const val METERS_PER_DEGREE = 111_320.0
        private const val MIN_COSINE = 0.01
        private const val FULL_TURN = 360.0
        private const val SECTOR = 45.0
        private const val MILLIS_PER_SECOND = 1_000L
        private const val DECIMETERS = 10.0
    }
}

/**
 * The ground under a route (docs/adr/0016-location.md, decision 11): where at least
 * [CITY_SHARE] of its points lie inside the frame of a city Passo carries, that city's map
 * (water, parks, streets, as on the walk's page); elsewhere none, a plain ground, since Passo
 * carries no street map and has no network to fetch one with.
 */
object RouteGround {
    /** Under four fifths, a city's map would end in the middle of the route. */
    const val CITY_SHARE: Double = 0.8

    /**
     * The city walk whose map lies under [points], or null. Of the walks that qualify (a city
     * has two, and their frames overlap) the one that holds the most of the route, then the
     * smaller frame: the finer ground.
     */
    fun cityOf(points: List<RoutePoint>, walks: List<Way> = Ways.walks): Way? {
        if (points.isEmpty()) return null
        return walks
            .map { walk -> walk to points.count { walk.frame.holds(it) }.toDouble() / points.size }
            .filter { it.second >= CITY_SHARE }
            .sortedWith(compareByDescending<Pair<Way, Double>> { it.second }.thenBy { it.first.frame.area() })
            .firstOrNull()?.first
    }

    private fun GeoBox.holds(point: RoutePoint): Boolean =
        point.latitude in south..north && point.longitude in west..east

    private fun GeoBox.area(): Double = (north - south) * (east - west)
}

/**
 * A map's scale bar: a round length in the reader's units, the longest that fits the room it is
 * given, with its word («200 m», «1 km», «500 yd», «1 mi»).
 *
 * @property meters the length it stands for, on the ground.
 * @property label that length, as it is written under the bar.
 */
data class ScaleBar(val meters: Double, val label: Measure) {
    companion object {
        private val METRIC = listOf(10, 20, 50, 100, 200, 500, 1_000, 2_000, 5_000, 10_000, 20_000, 50_000)
        private val YARDS = listOf(10, 20, 50, 100, 200, 500)
        private val MILES = listOf(1, 2, 5, 10, 20, 50)
        private const val METERS_PER_KM = 1_000

        /**
         * The bar for a map on which [maxMeters] is the longest a bar may be. Null when not
         * even the shortest fits (a map zoomed to a doorstep).
         */
        fun of(maxMeters: Double, units: UnitSystem): ScaleBar? = when (units) {
            UnitSystem.METRIC -> METRIC.lastOrNull { it <= maxMeters }?.let { length ->
                if (length < METERS_PER_KM) {
                    ScaleBar(length.toDouble(), Measure(length.toString(), MeasureUnit.METER))
                } else {
                    ScaleBar(length.toDouble(), Measure((length / METERS_PER_KM).toString(), MeasureUnit.KILOMETER))
                }
            }

            UnitSystem.IMPERIAL -> {
                val miles = MILES.lastOrNull { it * UnitConversions.METERS_PER_MILE <= maxMeters }
                if (miles != null) {
                    ScaleBar(miles * UnitConversions.METERS_PER_MILE, Measure(miles.toString(), MeasureUnit.MILE))
                } else {
                    YARDS.lastOrNull { it * METERS_PER_YARD <= maxMeters }?.let {
                        ScaleBar(it * METERS_PER_YARD, Measure(it.toString(), MeasureUnit.YARD))
                    }
                }
            }
        }

        private const val METERS_PER_YARD = 0.9144
    }
}
