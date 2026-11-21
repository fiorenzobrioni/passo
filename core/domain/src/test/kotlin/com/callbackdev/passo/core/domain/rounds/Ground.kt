package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.Geodesy
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Walks drawn by hand on a piece of a city, in metres east and north of a door: the routes the
 * rounds' tests compare. A point every three metres at a walk's 1.4 m/s, as the meter keeps them,
 * each moved by the receiver's honest wander: [wander] metres at most, drifting slowly from one
 * point to the next as a receiver's error does, so the same walk drawn twice with two seeds never
 * lies on itself, and its drawn length stays the walk's.
 */
internal object Ground {
    const val LATITUDE = 45.4642
    const val LONGITUDE = 9.19
    private const val METERS_PER_DEGREE = 111_320.0
    const val SPACING = 3.0
    private const val SPEED = 1.4

    /** How far the receiver's error moves from one point to the next, at most. */
    private const val DRIFT = 0.6

    /** The park's loop from the door, east first: 3.2 km. */
    val LOOP = listOf(0.0 to 0.0, 800.0 to 0.0, 800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0)

    /** The same streets, the other way round: north first. */
    val LOOP_OTHER_WAY = LOOP.asReversed()

    /** The loop with a dead end of 160 m on its east side, walked out and back: 3.52 km. */
    val LOOP_WITH_SPUR =
        listOf(
            0.0 to 0.0,
            800.0 to 0.0,
            800.0 to 400.0,
            960.0 to 400.0,
            800.0 to 400.0,
            800.0 to 800.0,
            0.0 to 800.0,
            0.0 to 0.0,
        )

    /** The loop with a detour of 300 m: 150 m south of its first street, and back. */
    val LOOP_WITH_DETOUR =
        listOf(
            0.0 to 0.0,
            300.0 to 0.0,
            300.0 to -150.0,
            300.0 to 0.0,
            800.0 to 0.0,
            800.0 to 800.0,
            0.0 to 800.0,
            0.0 to 0.0,
        )

    /** The loop begun at another point of it, half way up its east side, 900 m from the door. */
    val LOOP_FROM_ELSEWHERE =
        listOf(800.0 to 400.0, 800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0, 800.0 to 0.0, 800.0 to 400.0)

    /** Another loop from the same door, south and west: as long as the park's. */
    val OTHER_LOOP = listOf(0.0 to 0.0, 0.0 to -800.0, -800.0 to -800.0, -800.0 to 0.0, 0.0 to 0.0)

    /** To the station and back, along one street: 3 km. */
    val STATION = listOf(0.0 to 0.0, 1_500.0 to 0.0, 0.0 to 0.0)

    /** The park's first street, then north up a street of its own: shares 600 m with the loop. */
    val PARK_THEN_NORTH = listOf(0.0 to 0.0, 600.0 to 0.0, 600.0 to 1_200.0, 0.0 to 1_200.0, 0.0 to 0.0)

    /**
     * The route of a walk along [corners], from [startSecond]; its first point a stretch's start, as
     * an outing's is. [shift] moves the whole walk, the door's own fix landing elsewhere.
     */
    fun walk(
        corners: List<Pair<Double, Double>>,
        wander: Double = 6.0,
        seed: Int = 1,
        startSecond: Long = 1_800_000_000L,
        shift: Pair<Double, Double> = 0.0 to 0.0,
    ): List<RoutePoint> {
        val random = Random(seed)
        val out = ArrayList<RoutePoint>()
        var driftEast = (random.nextDouble() * 2 - 1) * wander
        var driftNorth = (random.nextDouble() * 2 - 1) * wander
        var second = startSecond.toDouble()
        for (index in 1 until corners.size) {
            val (x0, y0) = corners[index - 1]
            val (x1, y1) = corners[index]
            val length = hypot(x1 - x0, y1 - y0)
            val steps = (length / SPACING).toInt().coerceAtLeast(1)
            val first = if (index == 1) 0 else 1
            for (step in first..steps) {
                val t = step.toDouble() / steps
                driftEast = (driftEast + (random.nextDouble() * 2 - 1) * DRIFT).coerceIn(-wander, wander)
                driftNorth = (driftNorth + (random.nextDouble() * 2 - 1) * DRIFT).coerceIn(-wander, wander)
                val east = x0 + (x1 - x0) * t + shift.first + driftEast
                val north = y0 + (y1 - y0) * t + shift.second + driftNorth
                out +=
                    point(
                        east,
                        north,
                        second.toLong(),
                        if (out.isEmpty()) RoutePointKind.START else RoutePointKind.MEASURED,
                    )
                second += SPACING / SPEED
            }
        }
        return out
    }

    fun point(east: Double, north: Double, second: Long, kind: RoutePointKind = RoutePointKind.MEASURED): RoutePoint =
        RoutePoint(
            epochSecond = second,
            latitudeE6 = ((LATITUDE + north / METERS_PER_DEGREE) * 1e6).toInt(),
            longitudeE6 = ((LONGITUDE + east / (METERS_PER_DEGREE * cos(Math.toRadians(LATITUDE)))) * 1e6).toInt(),
            altitudeDm = null,
            accuracyMeters = 5,
            kind = kind,
        )

    /** The length of [corners], as walked. */
    fun length(corners: List<Pair<Double, Double>>): Double = (1 until corners.size).sumOf {
        hypot(
            corners[it].first - corners[it - 1].first,
            corners[it].second - corners[it - 1].second,
        )
    }

    /** The length of a route's lines, as the receiver drew it. */
    fun drawnLength(points: List<RoutePoint>): Double = (1 until points.size).sumOf {
        Geodesy.meters(points[it - 1].latitude, points[it - 1].longitude, points[it].latitude, points[it].longitude)
    }

    /**
     * An outing over, measured with GPS: [meters] in [movingMinutes] in motion, at a steady pace, with
     * its legs of a hundred metres, started [day] days from the first.
     */
    fun outing(
        id: Long,
        meters: Double,
        movingMinutes: Double,
        day: Long = 0,
        measuredShare: Double = 1.0,
        intensity: SessionIntensity = SessionIntensity.FREE,
        pausedMinutes: Double = 0.0,
    ): Session {
        val started = 1_800_000_000_000L + day * 86_400_000L + id * 1_000L
        val moving = (movingMinutes * 60_000).toLong()
        val legs = ArrayList<OutingLeg>()
        var laid = 0.0
        var index = 0
        while (laid < meters - 1e-6) {
            val take = minOf(100.0, meters - laid)
            laid += take
            legs += OutingLeg(
                index = index++,
                meters = take,
                movingMillis = (moving * take / meters).toLong(),
                steps = take / 0.7,
                endedAtMillis = started + (moving * laid / meters).toLong(),
            )
        }
        val ended = started + moving + (pausedMinutes * 60_000).toLong()
        return Session(
            id = id,
            planId = null,
            name = null,
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            intensity = intensity,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = 20_000 + day,
            startedAtMillis = started,
            state = SessionState.FINISHED,
            endedAtMillis = ended,
            totals = SessionTotals(steps = (meters / 0.7).toInt(), movingMillis = moving, distanceMeters = meters),
            lastStepAtMillis = ended,
            lastEventAtMillis = ended,
            gps = true,
            stepsMeters = meters,
            measuredShare = measuredShare,
            legs = legs,
        )
    }
}
