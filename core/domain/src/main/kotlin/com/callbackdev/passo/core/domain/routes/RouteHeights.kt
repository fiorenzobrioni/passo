package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import kotlin.math.roundToInt

/**
 * The pressure sensor's readings of the last while, on the steps' clock: what a route's points
 * are given their height from (PLANNING.md §11 Phase 16, «The climb»). A point's pressure is
 * the mean of the readings around its own second, which takes the sensor's flutter (a tenth of
 * a hectopascal, a metre) down to a few decimetres.
 *
 * Pure; the service adds the readings as they come, in order. Not thread-safe.
 */
class PressureTrack {
    private val times = ArrayDeque<Long>()
    private val values = ArrayDeque<Float>()

    /** The last reading's time; null before the first. */
    val latestMillis: Long? get() = times.lastOrNull()

    fun add(atMillis: Long, hpa: Float) {
        if (hpa <= 0f) return
        // Kept in order: a reading that comes out of it (a clock set back) starts the track anew.
        if (times.isNotEmpty() && atMillis < times.last()) clear()
        times.addLast(atMillis)
        values.addLast(hpa)
        while (times.first() < atMillis - KEEP_MILLIS) {
            times.removeFirst()
            values.removeFirst()
        }
    }

    /** The pressure at [atMillis]: the mean of the readings within [WINDOW_MILLIS] of it, or null with none. */
    fun at(atMillis: Long): Float? {
        var sum = 0.0
        var count = 0
        for (index in times.indices) {
            val time = times[index]
            if (time > atMillis + WINDOW_MILLIS) break
            if (time >= atMillis - WINDOW_MILLIS) {
                sum += values[index]
                count++
            }
        }
        return if (count == 0) null else (sum / count).toFloat()
    }

    fun clear() {
        times.clear()
        values.clear()
    }

    companion object {
        /** A point's pressure is the mean of the readings this near its second, either side. */
        const val WINDOW_MILLIS: Long = 2_500L

        /**
         * How long a reading is kept: the points walked after an end that can be taken back
         * (fifteen minutes) are given their height when it is, and their readings must still
         * be there.
         */
        const val KEEP_MILLIS: Long = 20 * 60_000L
    }
}

/** A climb counted at a moment of the outing, for the leg that was being walked then. */
data class Climbed(val atMillis: Long, val upMeters: Double, val downMeters: Double)

/**
 * The heights of a route's points as they are written, and the climb counted on them
 * (PLANNING.md §11 Phase 16, «The climb»): each point's altitude above mean sea level is
 * replaced by the blended one ([Altitude]), and what was climbed and come down is told with
 * the moment it was ([Climb]).
 *
 * The two streams arrive apart: the points with the step batches, the pressure in batches of
 * its own. A point waits here until the readings reach its second, so that it is never given
 * its height from the satellites alone for want of a reading that was on its way; a point that
 * has waited [MAX_WAIT_MILLIS], or the outing's end, takes what there is.
 *
 * Pure; not thread-safe.
 *
 * @param barometer the phone has a pressure sensor: with none, the satellites are alone, and
 *   the climb is counted past the higher threshold.
 */
class RouteHeights(private val barometer: Boolean) {
    private val altitude = Altitude()
    private val climb = Climb(
        if (barometer) Climb.BAROMETER_THRESHOLD_METERS else Climb.SATELLITE_THRESHOLD_METERS,
    )
    private val waiting = ArrayList<RoutePoint>()
    private var known = false

    /** Where the heights given so far came from; null before the first. */
    val source: AltitudeSource? get() = altitude.source

    /** Whether points are held back, waiting for their pressure. */
    val hasWaiting: Boolean get() = waiting.isNotEmpty()

    /**
     * The points to write and what they climbed.
     *
     * @property points with their blended altitude, or none where no height is known yet.
     * @property climbed in order; its first entry, both metres zero, says the height is known.
     */
    class Result(val points: List<RoutePoint>, val climbed: List<Climbed>)

    /**
     * Takes [points] (in the order of time, their altitude above mean sea level as the
     * satellites gave it) and gives back the ones whose pressure is in, the earlier ones that
     * waited among them.
     *
     * @param all the outing is ending: every point is taken now, with the readings there are.
     */
    fun take(points: List<RoutePoint>, pressure: PressureTrack, nowMillis: Long, all: Boolean = false): Result {
        waiting += points
        val reach = pressure.latestMillis
        val out = ArrayList<RoutePoint>()
        val climbed = ArrayList<Climbed>()
        while (waiting.isNotEmpty()) {
            val point = waiting.first()
            val atMillis = point.epochSecond * MILLIS_PER_SECOND
            val covered = reach != null && atMillis <= reach + PressureTrack.WINDOW_MILLIS
            if (barometer && !all && !covered && nowMillis - atMillis <= MAX_WAIT_MILLIS) break
            waiting.removeAt(0)
            // A stretch begins: no climb across a pause or a ride.
            if (point.kind == RoutePointKind.START) climb.cut()
            val height = altitude.next(
                atMillis = atMillis,
                pressureHpa = if (barometer) pressure.at(atMillis) else null,
                satelliteMeters = point.altitudeDm?.let { it / DECIMETERS },
            )
            if (height == null) {
                out += point.copy(altitudeDm = null)
                continue
            }
            if (!known) {
                known = true
                climbed += Climbed(atMillis, 0.0, 0.0)
            }
            val step = climb.next(height.relative)
            if (!step.isNone) climbed += Climbed(atMillis, step.up, step.down)
            out += point.copy(altitudeDm = (height.meters * DECIMETERS).roundToInt())
        }
        return Result(out, climbed)
    }

    companion object {
        /**
         * A point waits this long for its pressure and no longer: a minute and a half, past the
         * longest the sensor holds its readings with the screen off. After it the sensor is
         * not answering, and the point is written with the satellites' height.
         */
        const val MAX_WAIT_MILLIS: Long = 90_000L

        private const val MILLIS_PER_SECOND = 1_000L
        private const val DECIMETERS = 10.0
    }
}
