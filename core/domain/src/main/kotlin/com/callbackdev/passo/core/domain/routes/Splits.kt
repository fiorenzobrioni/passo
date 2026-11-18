package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.UnitSystem
import kotlin.math.roundToInt

/**
 * What a settled batch of steps adds to an outing measured with GPS: so many [meters] of its
 * fused distance, walked with so many [steps] in so much time in motion, between two moments on
 * the steps' clock. What the legs are laid from.
 */
data class LegPiece(
    val meters: Double,
    val steps: Double,
    val movingMillis: Long,
    val fromMillis: Long,
    val toMillis: Long,
)

/**
 * An outing measured with GPS, a hundred metres at a time (PLANNING.md §11 Phase 16, «Splits»):
 * the grain its kilometres or its miles are cut from afterwards, whichever the reader walks in.
 *
 * Laid as the outing goes, from what the meter settles: the distance is the fused one, and the
 * time in motion and the steps are the steps' own (docs/adr/0016-location.md, decision 5), so a
 * split's pace is the one the outing's own pace is made of, and its cadence is counted, not
 * guessed from a line of points. Numbers, never places: they live with the outing, and stay
 * when its route is deleted.
 */
object Legs {
    /** A leg's length: fine enough to cut a kilometre or a mile within a few metres. */
    const val LEG_METERS: Double = 100.0

    /**
     * [legs] with [piece] laid on them: the leg under way is filled to [LEG_METERS], and what is
     * left of the piece opens the next, its steps and its time shared by the metres. Steps that
     * went nowhere (a piece with no metres) belong to the leg under way.
     */
    fun lay(legs: List<OutingLeg>, piece: LegPiece): List<OutingLeg> {
        if (piece.meters <= 0.0 && piece.steps <= 0.0 && piece.movingMillis <= 0L) return legs
        val out = legs.toMutableList()
        if (piece.meters <= 0.0) {
            val last = out.removeLastOrNull() ?: OutingLeg(index = 0, meters = 0.0, endedAtMillis = piece.toMillis)
            out += last.copy(
                steps = last.steps + piece.steps,
                movingMillis = last.movingMillis + piece.movingMillis,
                endedAtMillis = maxOf(last.endedAtMillis, piece.toMillis),
            )
            return out
        }
        var left = piece.meters
        var laid = 0.0
        while (left > EPSILON_METERS) {
            val open = out.lastOrNull()?.takeIf { it.meters < LEG_METERS - EPSILON_METERS }
            val leg = open ?: OutingLeg(index = out.size, meters = 0.0, endedAtMillis = piece.fromMillis)
            if (open != null) out.removeLast()
            val take = minOf(left, LEG_METERS - leg.meters)
            val share = take / piece.meters
            laid += take
            // Where in the piece's own time this part of it ends: its metres laid evenly over it.
            val endsAt = piece.fromMillis + ((piece.toMillis - piece.fromMillis) * (laid / piece.meters)).toLong()
            out += leg.copy(
                meters = leg.meters + take,
                steps = leg.steps + piece.steps * share,
                movingMillis = leg.movingMillis + (piece.movingMillis * share).toLong(),
                endedAtMillis = maxOf(leg.endedAtMillis, endsAt),
            )
            left -= take
        }
        return out
    }

    /**
     * [legs] with a climb counted at [atMillis] added to the leg that was being walked then: the
     * first that ended at or after it, or the last, the altitudes coming a moment after the
     * metres they belong to.
     */
    fun climb(legs: List<OutingLeg>, atMillis: Long, upMeters: Double, downMeters: Double): List<OutingLeg> {
        if (legs.isEmpty() || (upMeters <= 0.0 && downMeters <= 0.0)) return legs
        val at = legs.indexOfFirst { it.endedAtMillis >= atMillis }.takeIf { it >= 0 } ?: legs.lastIndex
        return legs.mapIndexed { index, leg ->
            if (index != at) {
                leg
            } else {
                leg.copy(ascentMeters = leg.ascentMeters + upMeters, descentMeters = leg.descentMeters + downMeters)
            }
        }
    }

    private const val EPSILON_METERS = 1e-6
}

/**
 * One kilometre, or one mile, of an outing measured with GPS.
 *
 * @property index from 0.
 * @property meters its length: the unit's, but for the last one, which says its own ([whole]
 *   is then false).
 * @property endedAtMillis when its last metre was walked: where on the route its bead stands.
 */
data class Split(
    val index: Int,
    val meters: Double,
    val movingMillis: Long,
    val steps: Double,
    val ascentMeters: Double,
    val descentMeters: Double,
    val endedAtMillis: Long,
    val whole: Boolean,
) {
    /** Its pace, in seconds in motion a kilometre; null for a split too short to have one. */
    val secondsPerKm: Double?
        get() = if (meters < MIN_PACE_METERS || movingMillis <= 0) {
            null
        } else {
            movingMillis / MILLIS_PER_SECOND / (meters / METERS_PER_KM)
        }

    /** Its cadence, in steps a minute of motion; null before a minute's worth is behind it. */
    val cadence: Int?
        get() = if (movingMillis < MIN_CADENCE_MILLIS) null else (steps * MILLIS_PER_MINUTE / movingMillis).roundToInt()

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
        const val MILLIS_PER_MINUTE = 60_000.0
        const val METERS_PER_KM = 1_000.0

        /** Under a hundred metres a pace is noise: the last, short split shows its length alone. */
        const val MIN_PACE_METERS = 100.0
        const val MIN_CADENCE_MILLIS = 30_000L
    }
}

/**
 * The kilometres or the miles of an outing measured with GPS, cut from its legs (PLANNING.md §11
 * Phase 16, «Splits»): one a unit, each with its time in motion, its steps and its climb; the
 * last, partial one says its length.
 */
object Splits {
    /** The length of a split for a reader who walks in [units]. */
    fun unitMeters(units: UnitSystem): Double = when (units) {
        UnitSystem.METRIC -> METERS_PER_KM
        UnitSystem.IMPERIAL -> UnitConversions.METERS_PER_MILE
    }

    /**
     * [legs] cut every [unitMeters]. A leg that crosses a mark is shared between the two splits
     * by its metres, its time and its steps with them. A last split under [MIN_LAST_METERS] is
     * not one: a few steps past the mark belong to the split before.
     */
    fun of(legs: List<OutingLeg>, unitMeters: Double): List<Split> {
        if (unitMeters <= 0.0) return emptyList()
        val splits = ArrayList<Split>()
        var open = Open(0)
        var previousEnd = legs.firstOrNull()?.endedAtMillis ?: 0L
        for (leg in legs) {
            var left = leg.meters
            var taken = 0.0
            if (left <= 0.0) {
                open.add(leg, share = 1.0, leg.endedAtMillis)
                continue
            }
            while (left > EPSILON_METERS) {
                val take = minOf(left, unitMeters - open.meters)
                taken += take
                val endsAt = previousEnd + ((leg.endedAtMillis - previousEnd) * (taken / leg.meters)).toLong()
                open.add(leg, share = take / leg.meters, endsAt)
                left -= take
                if (open.meters >= unitMeters - EPSILON_METERS) {
                    splits += open.split(whole = true)
                    open = Open(splits.size)
                }
            }
            previousEnd = leg.endedAtMillis
        }
        if (open.meters >= MIN_LAST_METERS || (splits.isEmpty() && open.meters > 0.0)) {
            splits += open.split(whole = false)
        } else if (open.meters > 0.0 && splits.isNotEmpty()) {
            // A few steps past the last mark: the split before takes them.
            val last = splits.removeLast()
            splits += last.copy(
                meters = last.meters + open.meters,
                movingMillis = last.movingMillis + open.movingMillis,
                steps = last.steps + open.steps,
                ascentMeters = last.ascentMeters + open.ascent,
                descentMeters = last.descentMeters + open.descent,
                endedAtMillis = maxOf(last.endedAtMillis, open.endedAtMillis),
            )
        }
        return splits
    }

    /**
     * The fastest of [splits], by its pace: among the whole ones only, and only when there are
     * at least two to compare. A walk of one kilometre and a bit has no "fastest".
     */
    fun fastest(splits: List<Split>): Split? {
        val whole = splits.filter { it.whole && it.secondsPerKm != null }
        if (whole.size < 2) return null
        return whole.minBy { it.secondsPerKm ?: Double.MAX_VALUE }
    }

    private class Open(val index: Int) {
        var meters = 0.0
        var movingMillis = 0L
        var steps = 0.0
        var ascent = 0.0
        var descent = 0.0
        var endedAtMillis = 0L

        fun add(leg: OutingLeg, share: Double, endsAt: Long) {
            meters += leg.meters * share
            movingMillis += (leg.movingMillis * share).toLong()
            steps += leg.steps * share
            ascent += leg.ascentMeters * share
            descent += leg.descentMeters * share
            endedAtMillis = maxOf(endedAtMillis, endsAt)
        }

        fun split(whole: Boolean) = Split(index, meters, movingMillis, steps, ascent, descent, endedAtMillis, whole)
    }

    private const val METERS_PER_KM = 1_000.0
    private const val EPSILON_METERS = 1e-6

    /** Under this a last stretch is not a split of its own: thirty metres, a street's width or two. */
    const val MIN_LAST_METERS: Double = 30.0
}
