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
    /**
     * One round of the outing, the rounds [rounds] and the walks in no round [walks], each the same
     * as the outing, closest first (§15, «Phase 20: one rule, one threshold, a recent usual»): a
     * round is the walks linked by being the same, so a walk the same as walks of two rounds makes
     * them one, and one the same as two walks in no round makes a round of the three. [rounds] may be
     * empty, then [walks] are two at least; a single round and no walk is a walk joining it.
     */
    data class Into(val rounds: List<Long>, val walks: List<Long>) : RoundChoice

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
     * The round [outing] is, among [candidates] (§15, «Phase 20: one rule, one threshold, a recent
     * usual»): one rule, a round is the walks linked by being the same. The outing takes with it every
     * round one of whose walks it is the same as and every walk in no round it is the same as, each by
     * the whole rule with the outing, closest first: two walks of one round may lie too far apart to be
     * matched with each other (one with a detour, one with a shortcut) and still be each the same as a
     * third, and a walk the same as walks of two rounds makes them one.
     *
     * A round is [RoundConstants.WALKS] walks: with no round among them, the outing makes one only
     * with two walks in no round or more. Nothing the reader said is not the same ([apart], each pair
     * the smaller id first) is put in one round: a round or a walk with such a pair to what is taken
     * already is left. Rounds are taken first, the closest first, then the walks in none: a round is
     * the reader's usual, and a walk they took out of it must not keep the next walk of it out too.
     * [members] holds the walks of the candidates' rounds.
     */
    fun choose(
        outing: WalkedRoute,
        candidates: List<RoundCandidate>,
        apart: Set<Pair<Long, Long>> = emptySet(),
        members: Map<Long, Set<Long>> = emptyMap(),
    ): RoundChoice {
        val same = candidates.mapNotNull { candidate ->
            val result = RouteMatch.compare(
                outing,
                WalkedRoute(candidate.points, candidate.session.totals.distanceMeters),
            )
            if (result.same) candidate to result.inOrder else null
        }
        val rounds = ArrayList<Long>()
        val walks = ArrayList<Long>()
        val taken = HashSet<Long>()
        val ordered = same.sortedWith(
            compareByDescending<Pair<RoundCandidate, Double>> {
                it.first.roundId != null
            }.thenByDescending { it.second },
        )
        for ((candidate, _) in ordered) {
            val round = candidate.roundId
            if (round != null && round in rounds) continue
            val group = round?.let { members[it] } ?: setOf(candidate.session.id)
            if (RoundMerge.saidApart(taken, group, apart)) continue
            taken += group
            if (round != null) rounds += round else walks += candidate.session.id
        }
        return when {
            rounds.isNotEmpty() -> RoundChoice.Into(rounds, walks)
            walks.size >= RoundConstants.WALKS - 1 -> RoundChoice.Into(emptyList(), walks)
            else -> RoundChoice.None
        }
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
