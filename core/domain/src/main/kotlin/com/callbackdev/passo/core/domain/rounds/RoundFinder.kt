package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.DistanceKind
import com.callbackdev.passo.core.domain.routes.RouteMatch
import com.callbackdev.passo.core.domain.routes.WalkedRoute
import com.callbackdev.passo.core.domain.routes.distanceKind
import com.callbackdev.passo.core.model.RouteBox
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min

/**
 * An outing a new one may be the same round as: its numbers, the round it is in already (null when
 * it is in none), and its route's points.
 */
class RoundCandidate(val session: Session, val roundId: Long?, val points: List<RoutePoint>)

/** Where a new outing goes among the reader's rounds. */
sealed interface RoundChoice {
    /** Into the round [roundId]: it is the same as one of its walks. */
    data class Join(val roundId: Long) : RoundChoice

    /**
     * A new round with the outings [sessionIds], the walks in no round it is the same as, closest
     * first: every one of them, so that the third walk of a round makes it known even when the first
     * two were too far apart to be matched with each other (§15, «Phase 20: the last open choices»).
     */
    data class New(val sessionIds: List<Long>) : RoundChoice

    /** Into none. */
    data object None : RoundChoice
}

/**
 * Finds the round an outing with GPS is, when it ends (PLANNING.md §11 Phase 20, «The match»): among
 * the reader's own outings, never anyone else's (docs/adr/0016-location.md, decision 16). Pure: the
 * repository reads the candidates, cheapest test first, and writes what this decides.
 */
object RoundFinder {
    /**
     * Whether [session] can be a round's walk: over, measured with GPS (nine tenths from fixes: a
     * line laid from the steps is too much theirs to be compared), and long enough to be a walk.
     */
    fun eligible(session: Session): Boolean = session.state == SessionState.FINISHED &&
        session.distanceKind() == DistanceKind.MEASURED &&
        session.totals.distanceMeters >= RoundConstants.MIN_METERS

    /**
     * Whether [a] and [b] may be the same round by their numbers alone, before a point is read:
     * lengths within a tenth, and both walks or both runs. A run round the park and a walk round it
     * are two rounds: «faster than usual» would otherwise be the running, not the reader's pace.
     */
    fun mayBeSame(a: Session, b: Session): Boolean =
        (a.intensity == SessionIntensity.RUN) == (b.intensity == SessionIntensity.RUN) &&
            RouteMatch.lengthsAlike(a.totals.distanceMeters, b.totals.distanceMeters)

    /**
     * Whether two routes' boxes, each grown by [RoundConstants.BOX_MARGIN_METERS], overlap by at
     * least [RoundConstants.BOX_OVERLAP] of their union: only then are their points worth reading.
     */
    fun boxesAlike(a: RouteBox, b: RouteBox): Boolean {
        val latitude = (a.southE6 + a.northE6 + b.southE6 + b.northE6) / 4 / MICRO
        val growLat = RoundConstants.BOX_MARGIN_METERS / METERS_PER_DEGREE * MICRO
        val growLon = growLat / cos(Math.toRadians(latitude)).coerceAtLeast(MIN_COSINE)
        val ga = Grown(a, growLat, growLon)
        val gb = Grown(b, growLat, growLon)
        val south = max(ga.south, gb.south)
        val north = min(ga.north, gb.north)
        val west = max(ga.west, gb.west)
        val east = min(ga.east, gb.east)
        if (north <= south || east <= west) return false
        val overlap = (north - south) * (east - west)
        val union = ga.area + gb.area - overlap
        return union > 0 && overlap / union >= RoundConstants.BOX_OVERLAP
    }

    /**
     * The round [outing] is, among [candidates]: a round one of whose walks it is the same as (the
     * closest, should it match walks of two), before a new round with the outings in none it is the
     * same as. A round is the reader's usual once it is one: a new walk joins it rather than starting
     * another.
     *
     * A new round takes every walk in no round the outing is the same as, closest first, each by the
     * whole rule with the outing: two walks of one round may lie too far apart to be matched with each
     * other (one with a detour, one with a shortcut) and still be each the same as a third. No two the
     * reader said are not the same ([apart], each pair the smaller id first) are put in one round:
     * of such two, the closer to the outing.
     */
    fun choose(
        outing: WalkedRoute,
        candidates: List<RoundCandidate>,
        apart: Set<Pair<Long, Long>> = emptySet(),
    ): RoundChoice {
        val same = candidates.mapNotNull { candidate ->
            val result = RouteMatch.compare(
                outing,
                WalkedRoute(candidate.points, candidate.session.totals.distanceMeters),
            )
            if (result.same) candidate to result.inOrder else null
        }
        same.filter { it.first.roundId != null }.maxByOrNull { it.second }?.let { (candidate, _) ->
            return RoundChoice.Join(checkNotNull(candidate.roundId))
        }
        val walks = ArrayList<Long>()
        for ((candidate, _) in same.sortedByDescending { it.second }) {
            val id = candidate.session.id
            if (walks.none { minOf(it, id) to maxOf(it, id) in apart }) walks += id
        }
        return if (walks.isEmpty()) RoundChoice.None else RoundChoice.New(walks)
    }

    private class Grown(box: RouteBox, lat: Double, lon: Double) {
        val south = box.southE6 - lat
        val north = box.northE6 + lat
        val west = box.westE6 - lon
        val east = box.eastE6 + lon
        val area: Double get() = (north - south) * (east - west)
    }

    private const val MICRO = 1_000_000.0
    private const val METERS_PER_DEGREE = 111_320.0
    private const val MIN_COSINE = 0.01
}
