package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.EvenSamples
import com.callbackdev.passo.core.domain.routes.Geodesy
import com.callbackdev.passo.core.domain.routes.InOrder
import com.callbackdev.passo.core.domain.routes.LineIndex
import com.callbackdev.passo.core.domain.routes.Plane
import com.callbackdev.passo.core.model.RoutePoint

/**
 * A round an open outing with GPS may be walking, as the tracking service reads it once, at the
 * outing's start (PLANNING.md §11 Phase 20, «The card, while it counts»): its lines, from its latest
 * walks' routes, and its usual time at each kilometre, from its walks' legs.
 *
 * @property lines the routes of its latest walks that keep one, newest first, up to
 *   [RoundConstants.WALKS_ASKED]: one walk's alone would carry its own shortcut or detour, and a
 *   walk of the round as usual would leave it there (§15, «Phase 20 reviewed»).
 * @property usualAtMarks the usual time in motion at the 1st, 2nd… kilometre (or mile, the
 *   outing's frozen unit), from index 0; null where too few walks reached it.
 * @property walks how many times it was walked: of two rounds the walk is on alike, the usual one.
 */
class RoundGuide(val roundId: Long, val lines: List<List<RoutePoint>>, val usualAtMarks: List<Long?>, val walks: Int)

/**
 * Which of the reader's rounds an open outing with GPS is walking, as its route comes (Phase 20). Fed
 * the route's points as the meter settles them, it says whether the walk is on a round: begun within
 * [RoundConstants.START_METERS] of where one of its lines begins, and, from
 * [RoundConstants.FOUND_METERS] on, four fifths of it near that line in order; and whether it has
 * left it: off every one of its lines for [RoundConstants.LEFT_METERS] in a row.
 *
 * Each of a round's lines is followed on its own, the walk laid along each in order: the round is
 * as near as the nearest of them. A walk of it that cut a corner last time does not have the round,
 * walked whole the next time, told as left at that corner.
 *
 * Pure, and fed by the tracker alone: no timer, no read, nothing but the points the outing keeps
 * anyway. Two rounds from the same door are told apart as the walk goes: on the first of them while
 * they share their streets, on the other once it leaves the first for the other's.
 */
class RoundWatch(guides: List<RoundGuide>) {
    private val follows = guides.flatMap { guide ->
        guide.lines.filter { it.size >= 2 }.map { line -> Follow(guide, line) }
    }
    private var first: RoutePoint? = null

    /** Takes the points the outing has kept since the last call, in order. */
    fun add(points: List<RoutePoint>) {
        for (point in points) {
            if (first == null) {
                first = point
                follows.forEach { it.startsNear(point) }
            }
            follows.forEach { it.take(point) }
        }
    }

    /**
     * The round the walk is on, having walked [walkedMeters]: [current] while it stays on it, or the
     * one it is found on (the closest, then the most walked); null before the first half kilometre,
     * and once it is on none. Never a round it was not found on from its start.
     */
    fun on(current: Long?, walkedMeters: Double): Long? {
        if (walkedMeters < RoundConstants.FOUND_METERS) return null
        val still = follows.filter { it.still }
        still.firstOrNull { it.guide.roundId == current }?.let { return current }
        return still
            .filter { it.found }
            .maxWithOrNull(compareBy<Follow>({ it.share }, { it.guide.walks }))
            ?.guide
            ?.roundId
    }

    /** The usual time in motion at the [mark]th kilometre of round [roundId]; null where it has none. */
    fun usualAt(roundId: Long, mark: Int): Long? =
        follows.firstOrNull { it.guide.roundId == roundId }?.guide?.usualAtMarks?.getOrNull(mark - 1)

    /** One of a round's lines followed: the outing's walk laid along it, in the line's own plane. */
    private class Follow(val guide: RoundGuide, points: List<RoutePoint>) {
        private val start = points.first()
        private val plane = Plane(start.latitude, start.longitude)
        private val line = LineIndex(
            EvenSamples.of(points, plane, RoundConstants.SAMPLE_METERS, RoundConstants.MAX_SAMPLES),
            RoundConstants.NEAR_METERS,
        )
        private val order = InOrder(line, RoundConstants.ORDER_SLACK_METERS)
        private val samples = EvenSamples(plane, RoundConstants.SAMPLE_METERS)
        private var near = false
        private var offInARow = 0

        fun startsNear(point: RoutePoint) {
            near = Geodesy.meters(point.latitude, point.longitude, start.latitude, start.longitude) <=
                RoundConstants.START_METERS
        }

        fun take(point: RoutePoint) {
            if (!near || !line.drawn) return
            samples.add(point) { index ->
                order.take(samples.xs[index], samples.ys[index])
                offInARow = if (order.lastInOrder) 0 else offInARow + 1
            }
        }

        val share: Double get() = order.inOrderShare

        /** Begun at its door and not off its line for long: the walk may still be on it. */
        val still: Boolean
            get() = near && line.drawn && order.taken > 0 &&
                offInARow * RoundConstants.SAMPLE_METERS < RoundConstants.LEFT_METERS

        /** Four fifths of the walk so far near its line, in order: on it. */
        val found: Boolean get() = still && share >= RoundConstants.SHARE
    }
}
