package com.callbackdev.passo.feature.sessions

import com.callbackdev.passo.core.domain.routes.LegPiece
import com.callbackdev.passo.core.domain.routes.Legs
import com.callbackdev.passo.core.domain.routes.RouteGround
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.sin

/**
 * Outings for the page's tests and its README picture: a brisk hour through Milan measured with
 * GPS, on the streets Passo carries, with an arcade laid from the steps and a gentle rise; the
 * same walk far from any city Passo carries; and the same hour counted from the steps alone.
 */
internal object OutingSamples {
    private val zone: ZoneId = ZoneId.systemDefault()

    /** Wednesday 18 November 2026, 17:40, in the reader's own time. */
    val startedAt: Long = LocalDateTime.of(2026, 11, 18, 17, 40).atZone(zone).toInstant().toEpochMilli()

    private const val METERS = 4_260.0
    private const val SPEED = 1.5

    /** A point every three metres along Milan's walk, two seconds apart: [north] degrees moved. */
    fun points(north: Double = 0.0): List<RoutePoint> {
        val line = Ways.of(WayId.MILAN_DUOMO_NAVIGLI).map.line
        val out = ArrayList<RoutePoint>()
        var meters = 0.0
        var first = true
        while (meters <= METERS) {
            // An arcade between 1.9 and 2.2 km: no fix, the far side laid from the steps.
            val arcade = meters > 1_900 && meters < 2_200
            if (!arcade) {
                val at = line.pointAt(meters)
                val afterArcade = out.isNotEmpty() && meters - 3.0 <= 2_200 && meters >= 2_200
                out += RoutePoint(
                    epochSecond = startedAt / 1_000 + (meters / SPEED).toLong(),
                    latitudeE6 = ((at.latitude + north) * 1e6).toInt(),
                    longitudeE6 = (at.longitude * 1e6).toInt(),
                    // A rise of some thirty metres to the middle, and down again, with a ripple.
                    altitudeDm = ((121 + 30 * sin(meters / METERS * Math.PI) + 2 * sin(meters / 180)) * 10).toInt(),
                    accuracyMeters = 5,
                    kind = when {
                        first -> RoutePointKind.START
                        afterArcade -> RoutePointKind.LAID
                        else -> RoutePointKind.MEASURED
                    },
                )
                first = false
            }
            meters += 3.0
        }
        return out
    }

    /** The legs of that walk: the third kilometre a little slower, the fourth the fastest. */
    val legs: List<OutingLeg> = run {
        var legs = emptyList<OutingLeg>()
        var at = startedAt
        var walked = 0.0
        while (walked < METERS) {
            val km = (walked / 1_000).toInt()
            val speed = when (km) {
                2 -> 1.32
                3 -> 1.68
                else -> SPEED
            }
            val meters = minOf(speed * 10, METERS - walked)
            legs =
                Legs.lay(
                    legs,
                    LegPiece(
                        meters,
                        steps = 19.0,
                        movingMillis = 10_000,
                        fromMillis = at,
                        toMillis =
                        at + 10_000,
                    ),
                )
            walked += meters
            at += 10_000
        }
        // The rise is in the first two kilometres, the way down in the last two.
        legs = Legs.climb(legs, legs[6].endedAtMillis, 12.0, 0.0)
        legs = Legs.climb(legs, legs[15].endedAtMillis, 19.0, 0.0)
        legs = Legs.climb(legs, legs[28].endedAtMillis, 0.0, 14.0)
        Legs.climb(legs, legs[39].endedAtMillis, 0.0, 16.0)
    }

    private val moving = legs.sumOf { it.movingMillis }

    val measured: Session = Session(
        id = 12,
        planId = 1,
        name = null,
        goalKind = SessionPlans.PRESETS[0].goalKind,
        goalValue = 45,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        vibrate = true,
        localEpochDay = LocalDateTime.of(2026, 11, 18, 17, 40).toLocalDate().toEpochDay(),
        startedAtMillis = startedAt,
        state = SessionState.FINISHED,
        end = SessionEnd.GOAL,
        endedAtMillis = startedAt + moving + 4 * 60_000,
        reachedAtMillis = startedAt + moving + 4 * 60_000,
        lastStepAtMillis = startedAt + moving + 4 * 60_000,
        totals = SessionTotals(
            steps = legs.sumOf { it.steps }.toInt(),
            movingMillis = moving,
            zoneMillis = moving - 5 * 60_000,
            distanceMeters = METERS,
            activeKcal = 171.0,
        ),
        gps = true,
        stepsMeters = 4_105.0,
        measuredShare = 0.93,
        ascentMeters = 31.0,
        descentMeters = 30.0,
        legs = legs,
    )

    /** The same hour with no GPS: an estimate, and nothing of the route's. */
    val onSteps: Session = measured.copy(
        gps = false,
        stepsMeters = null,
        measuredShare = null,
        ascentMeters = null,
        descentMeters = null,
        legs = emptyList(),
        totals = measured.totals.copy(distanceMeters = 4_105.0, activeKcal = 152.0),
    )

    fun route(north: Double = 0.0, altitudes: AltitudeSource? = AltitudeSource.BAROMETER): DrawnRoute {
        val points = points(north)
        return DrawnRoute(checkNotNull(RouteShape.of(points)), RouteGround.cityOf(points)?.map, altitudes)
    }

    fun state(
        session: Session = measured,
        route: DrawnRoute? = route(),
        units: UnitPreference = UnitPreference.METRIC,
    ) = OutingUiState(session, route, units)
}
