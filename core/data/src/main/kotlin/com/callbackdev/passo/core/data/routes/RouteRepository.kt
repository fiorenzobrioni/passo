package com.callbackdev.passo.core.data.routes

import com.callbackdev.passo.core.data.db.RouteDao
import com.callbackdev.passo.core.domain.routes.RouteCodec
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.Route
import com.callbackdev.passo.core.model.RouteBox
import com.callbackdev.passo.core.model.RoutePoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    /**
     * Adds [points] to the route of the outing [sessionId], which started at
     * [sessionStartedAtMillis]; [altitudes] says where their heights came from, if they have any.
     */
    suspend fun append(
        sessionId: Long,
        sessionStartedAtMillis: Long,
        points: List<RoutePoint>,
        altitudes: AltitudeSource? = null,
    ) {
        val box = RouteBox.of(points) ?: return
        dao.append(sessionId, sessionStartedAtMillis, RouteCodec.encode(points), points.size, box, altitudes?.name)
    }

    /**
     * The route of the outing [sessionId], or null when it has none: an outing without GPS, one
     * whose route the reader deleted, one a restore brought without `routes.db`, or one whose id
     * a route of another outing holds ([sessionStartedAtMillis] says which outing is meant).
     */
    suspend fun route(sessionId: Long, sessionStartedAtMillis: Long): Route? {
        val row = dao.route(sessionId)?.takeIf { it.sessionStartedAtMillis == sessionStartedAtMillis } ?: return null
        val points = dao.chunks(row.sessionId).flatMap(RouteCodec::decode)
        val altitudes = AltitudeSource.entries.firstOrNull { it.name == row.altitudeSource }
        return Route(sessionStartedAtMillis, points, altitudes).takeIf { points.isNotEmpty() }
    }

    /**
     * How many points the route of the outing [sessionId] holds, as it changes: what its page
     * reads the route again at. Null with no route.
     */
    fun observePointCount(sessionId: Long): Flow<Int?> = dao.observePointCount(sessionId)

    /** The outings that have a route, as they change: a route written, or deleted by the reader. */
    val routeIds: Flow<Set<Long>> = dao.observeRouteIds().map { it.toSet() }

    /** How many routes are kept, as it changes. */
    val count: Flow<Int> = dao.observeRouteCount()

    /** The bytes the routes' points take: what Your data says they weigh. */
    suspend fun bytes(): Long = dao.routeBytes()

    /** Forgets every route; the outings and their numbers are untouched. */
    suspend fun deleteAll() = dao.deleteAll()

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
