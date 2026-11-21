package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.rounds.RoundConstants
import com.callbackdev.passo.core.model.RoutePoint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * One outing's walk as two are compared: its route's points, and its distance, the fused one the
 * reader reads (a route's own geometry is a little shorter, its corners cut by the grain).
 */
class WalkedRoute(val points: List<RoutePoint>, val meters: Double)

/**
 * Whether two outings with GPS are the same round (PLANNING.md §11 Phase 20, «The match»). They are
 * when, together:
 * - they start within [RoundConstants.START_METERS] of each other: rounds begin at the reader's
 *   door, not anywhere on the loop, so a loop begun at another point of it is another round;
 * - their lengths are within [RoundConstants.LENGTH_SHARE] of each other;
 * - four fifths of each ([RoundConstants.SHARE]) lies within [RoundConstants.NEAR_METERS] of the
 *   other, **in the same order**: the same streets walked the other way round start at the same
 *   door, have the same length and lie wholly within forty metres, and are another round.
 *
 * Both ways: a walk that is the round with a detour is within four fifths of the round, and the
 * round within four fifths of it. Pure: it reads two lists of points and says.
 */
object RouteMatch {
    /** Why two walks are, or are not, one round: the first test they fail, in the order above. */
    enum class Verdict {
        SAME,
        STARTS_APART,
        LENGTHS_APART,

        /** Their lines lie apart: less than four fifths of one is near the other. */
        APART,

        /** Near enough, but not in the same order: the same streets, the other way round. */
        OTHER_WAY,
    }

    /**
     * What the comparison found.
     *
     * @property inOrder the share of the walk that lies in order along the other, the smaller of
     *   the two ways: the closeness of a match, for choosing between two that both are.
     */
    data class Result(val verdict: Verdict, val inOrder: Double = 0.0) {
        val same: Boolean get() = verdict == Verdict.SAME
    }

    fun compare(a: WalkedRoute, b: WalkedRoute): Result {
        val startA = a.points.firstOrNull() ?: return Result(Verdict.APART)
        val startB = b.points.firstOrNull() ?: return Result(Verdict.APART)
        val apart = Geodesy.meters(startA.latitude, startA.longitude, startB.latitude, startB.longitude)
        if (apart > RoundConstants.START_METERS) return Result(Verdict.STARTS_APART)
        if (!lengthsAlike(a.meters, b.meters)) return Result(Verdict.LENGTHS_APART)
        val plane = Plane(startA.latitude, startA.longitude)
        val samplesA = samplesOf(a.points, plane)
        val samplesB = samplesOf(b.points, plane)
        val lineA = LineIndex(samplesA, RoundConstants.NEAR_METERS)
        val lineB = LineIndex(samplesB, RoundConstants.NEAR_METERS)
        if (!lineA.drawn || !lineB.drawn) return Result(Verdict.APART)
        val aOnB = laid(samplesA, lineB)
        val bOnA = laid(samplesB, lineA)
        val near = min(aOnB.nearShare, bOnA.nearShare)
        val inOrder = min(aOnB.inOrderShare, bOnA.inOrderShare)
        val verdict = when {
            near < RoundConstants.SHARE -> Verdict.APART
            inOrder < RoundConstants.SHARE -> Verdict.OTHER_WAY
            else -> Verdict.SAME
        }
        return Result(verdict, inOrder)
    }

    /** Within a tenth of each other, the longer one's tenth: a shortcut of a tenth is still the round. */
    fun lengthsAlike(a: Double, b: Double): Boolean {
        if (a <= 0.0 || b <= 0.0) return false
        return abs(a - b) <= max(a, b) * RoundConstants.LENGTH_SHARE
    }

    private fun samplesOf(points: List<RoutePoint>, plane: Plane): EvenSamples =
        EvenSamples.of(points, plane, RoundConstants.SAMPLE_METERS, RoundConstants.MAX_SAMPLES)

    private fun laid(samples: EvenSamples, line: LineIndex): InOrder {
        val order = InOrder(line, RoundConstants.ORDER_SLACK_METERS)
        for (index in 0 until samples.size) order.take(samples.xs[index], samples.ys[index])
        return order
    }
}
