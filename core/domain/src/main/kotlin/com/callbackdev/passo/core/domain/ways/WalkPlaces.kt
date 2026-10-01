package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.Session
import kotlin.math.roundToInt

/** The place ahead on a walk, and how far it is. */
data class NextPlace(val stop: WayStop, val inMeters: Int)

/**
 * Where an outing on a city walk stands among the walk's places (PLANNING.md §11 Phase 11):
 * read from where it began on the walk and the distance it has measured, the same two numbers
 * the tracker tells the places by, so the line "Next: the Duomo, 600 m" and the signal agree.
 */
object WalkPlaces {
    /** How far along its walk [session] stands, in metres; null for an outing on no walk. */
    fun along(session: Session): Double? {
        val walk = session.walk ?: return null
        return (session.walkFromMeters + session.totals.distanceMeters).coerceAtMost(
            Ways.of(walk).lengthMeters.toDouble(),
        )
    }

    /** The first place past where [session] stands, and how far; null at the end or on no walk. */
    fun next(session: Session): NextPlace? {
        val walk = session.walk ?: return null
        val along = along(session) ?: return null
        val stop = Ways.of(walk).stops.firstOrNull { it.distanceMeters > along } ?: return null
        return NextPlace(stop, (stop.distanceMeters - along).roundToInt().coerceAtLeast(1))
    }

    /** The place [fromMeters] along [walk] stands at or past: where an outing from there begins. */
    fun at(walk: Way, fromMeters: Int): WayStop = walk.stops.last { it.distanceMeters <= fromMeters }

    /** The first place after [fromMeters] along [walk], and how far; null when none is left. */
    fun after(walk: Way, fromMeters: Int): NextPlace? = walk.stops.firstOrNull { it.distanceMeters > fromMeters }
        ?.let { NextPlace(it, (it.distanceMeters - fromMeters).coerceAtLeast(1)) }
}
