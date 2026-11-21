package com.callbackdev.passo.core.model

/** How a point of a route is joined to the one before it. */
enum class RoutePointKind {
    /**
     * A stretch begins here, and no line reaches it from the point before: the first point of an
     * outing, the first after a pause, or the first after the reader was carried (a tram, a
     * lift). A route never draws a line across a place that was not walked.
     */
    START,

    /** Reached on foot under the satellites: the line to it from the point before was measured. */
    MEASURED,

    /**
     * Reached on foot with no fix on the way (a tunnel, an arcade, tall streets): the stretch to
     * it was laid from the steps, and is drawn as such. The honest mark of a route.
     */
    LAID,
}

/**
 * One point of an outing's route (PLANNING.md §11 Phase 15; docs/adr/0016-location.md, decision
 * 8), at the resolution it is stored with, so that what is read back is what was written: whole
 * seconds, millionths of a degree (about a tenth of a metre), decimetres of altitude, metres of
 * accuracy.
 *
 * @property epochSecond when the fix was taken, on the steps' own clock.
 * @property altitudeDm above mean sea level, in decimetres; null when the fix had none.
 * @property accuracyMeters the radius the receiver gave the fix.
 */
data class RoutePoint(
    val epochSecond: Long,
    val latitudeE6: Int,
    val longitudeE6: Int,
    val altitudeDm: Int?,
    val accuracyMeters: Int,
    val kind: RoutePointKind,
) {
    val latitude: Double get() = latitudeE6 / MICRO

    val longitude: Double get() = longitudeE6 / MICRO

    private companion object {
        const val MICRO = 1_000_000.0
    }
}

/** The box a route lies in, in millionths of a degree. */
data class RouteBox(val southE6: Int, val westE6: Int, val northE6: Int, val eastE6: Int) {
    /** This box grown to hold [point]. */
    fun with(point: RoutePoint): RouteBox = RouteBox(
        southE6 = minOf(southE6, point.latitudeE6),
        westE6 = minOf(westE6, point.longitudeE6),
        northE6 = maxOf(northE6, point.latitudeE6),
        eastE6 = maxOf(eastE6, point.longitudeE6),
    )

    companion object {
        /** The box of [points]; null for none. */
        fun of(points: List<RoutePoint>): RouteBox? {
            val first = points.firstOrNull() ?: return null
            val start = RouteBox(first.latitudeE6, first.longitudeE6, first.latitudeE6, first.longitudeE6)
            return points.fold(start) { box, point -> box.with(point) }
        }
    }
}

/**
 * A kept route without its points: which outing it belongs to, by its id and its start, and how
 * many points it holds. What a list of outings reads to know which of them have a route, and
 * whether a small picture of it drawn before is still the whole line (PLANNING.md §11 Phase 23,
 * step 23.2).
 */
data class RouteKey(val sessionId: Long, val sessionStartedAtMillis: Long, val pointCount: Int)

/** Where the altitudes of a route came from, and so how far its climb can be trusted. */
enum class AltitudeSource {
    /** The pressure sensor for the short term, the satellites for the long: metres are metres. */
    BAROMETER,

    /** The satellites alone, on a phone with no pressure sensor: smoothed, and counted coarsely. */
    SATELLITES,
}

/**
 * An outing's route: where it went, kept apart from the days (in a database of its own, which
 * Android's cloud backup never carries).
 *
 * @property sessionStartedAtMillis the outing it belongs to, by its start: the one thing of an
 *   outing that is the same on every phone a backup reaches.
 * @property altitudes where its points' heights came from; null when none has one.
 */
data class Route(
    val sessionStartedAtMillis: Long,
    val points: List<RoutePoint>,
    val altitudes: AltitudeSource? = null,
) {
    val box: RouteBox? get() = RouteBox.of(points)
}

/**
 * What a clean stretch of an outing with GPS said of the reader's step (docs/adr/0016-location.md,
 * decision 12): measured from end to end at a steady [cadence], so many [steps] over so many
 * [meters]. A number, never a place.
 */
data class StepLengthSample(val cadence: Int, val steps: Int, val meters: Double)
