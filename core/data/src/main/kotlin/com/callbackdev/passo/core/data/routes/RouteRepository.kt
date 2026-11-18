package com.callbackdev.passo.core.data.routes

import com.callbackdev.passo.core.data.db.RouteDao
import com.callbackdev.passo.core.domain.routes.RouteCodec
import com.callbackdev.passo.core.model.Route
import com.callbackdev.passo.core.model.RouteBox
import com.callbackdev.passo.core.model.RoutePoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where the outings measured with GPS went (PLANNING.md §11 Phase 15; docs/adr/0016-location.md,
 * decisions 8 and 9): written by the tracking service as an outing goes, read by the outing's
 * page, and never sent anywhere by Passo.
 *
 * The outing in `passo.db` is the truth; this is beside it. A chunk is appended after the step
 * batch's transaction, never inside it, and a route is deleted before its outing.
 */
@Singleton
class RouteRepository
@Inject
constructor(private val dao: RouteDao) {
    /** Adds [points] to the route of the outing [sessionId], which started at [sessionStartedAtMillis]. */
    suspend fun append(sessionId: Long, sessionStartedAtMillis: Long, points: List<RoutePoint>) {
        val box = RouteBox.of(points) ?: return
        dao.append(sessionId, sessionStartedAtMillis, RouteCodec.encode(points), points.size, box)
    }

    /**
     * The route of the outing [sessionId], or null when it has none: an outing without GPS, one
     * whose route the reader deleted, one a restore brought without `routes.db`, or one whose id
     * a route of another outing holds ([sessionStartedAtMillis] says which outing is meant).
     */
    suspend fun route(sessionId: Long, sessionStartedAtMillis: Long): Route? {
        val row = dao.route(sessionId)?.takeIf { it.sessionStartedAtMillis == sessionStartedAtMillis } ?: return null
        val points = dao.chunks(row.sessionId).flatMap(RouteCodec::decode)
        return Route(sessionStartedAtMillis, points).takeIf { points.isNotEmpty() }
    }

    /** Forgets the route of the outing [sessionId]; the outing and its numbers are untouched. */
    suspend fun delete(sessionId: Long) = dao.delete(sessionId)

    /**
     * Deletes every route whose outing is not among [outings] (each outing's id with its start):
     * what a crash between the deletion of a route and of its outing never leaves, since the
     * route goes first, and what a `passo.db` put back from a backup beside a newer `routes.db`
     * could. Places with no outing are not kept. Returns how many went.
     */
    suspend fun deleteOrphans(outings: Map<Long, Long>): Int {
        val orphans = dao.routes().filter { outings[it.sessionId] != it.sessionStartedAtMillis }
        orphans.forEach { dao.delete(it.sessionId) }
        return orphans.size
    }
}
