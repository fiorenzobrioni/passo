package com.callbackdev.passo.core.data.routes

import com.callbackdev.passo.core.data.db.RouteDao
import com.callbackdev.passo.core.domain.routes.RouteCodec
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.Route
import com.callbackdev.passo.core.model.RouteBox
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.RoutePoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** A route's box, and the start of the outing it belongs to. */
data class KeptBox(val sessionStartedAtMillis: Long, val box: RouteBox)

/** Whether the routes' own file is on the phone: it is made by the first route written, and by nothing else. */
fun interface RouteFile {
    fun exists(): Boolean
}

/**
 * Where the outings measured with GPS went (PLANNING.md §11 Phase 15; docs/adr/0016-location.md,
 * decisions 8 and 9): written by the tracking service as an outing goes, read by the outing's
 * page, and never sent anywhere by Passo.
 *
 * The outing in `passo.db` is the truth; this is beside it. A chunk is appended after the step
 * batch's transaction, never inside it, and a route is deleted before its outing.
 *
 * A reader who never measures with GPS never has the routes' file: only a route written makes
 * it. Every read, and every deletion, asks [file] first and answers "nothing" without opening
 * a database that is not there; what is watched watches for the file too, so a page open as the
 * first route is written sees it come.
 */
@Singleton
class RouteRepository
@Inject
constructor(private val dao: RouteDao, private val file: RouteFile = RouteFile { true }) {
    // The routes the reader deleted in this process, each by its outing's id and start: never
    // written again, should the service still hold points of it (an outing reopened by «Keep
    // going», a chunk on its way as the reader tapped). By the start too, as an id can come back.
    private val forgotten: MutableSet<Pair<Long, Long>> = ConcurrentHashMap.newKeySet()

    // Whether the file is known to be there: from [file] when someone watches, and from the
    // first chunk this process appends, which makes it.
    private val made = MutableStateFlow(false)

    private val present: Flow<Boolean> = flow {
        if (!made.value && file.exists()) made.value = true
        emitAll(made)
    }.distinctUntilChanged()

    /** [read] once the file is there, [none] until then: never a database opened to make it. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> whenPresent(none: T, read: () -> Flow<T>): Flow<T> =
        present.flatMapLatest { if (it) read() else flowOf(none) }

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
        if (sessionId to sessionStartedAtMillis in forgotten) return
        dao.append(sessionId, sessionStartedAtMillis, RouteCodec.encode(points), points.size, box, altitudes?.name)
        made.value = true
    }

    /**
     * The route of the outing [sessionId], or null when it has none: an outing without GPS, one
     * whose route the reader deleted, one a restore brought without `routes.db`, or one whose id
     * a route of another outing holds ([sessionStartedAtMillis] says which outing is meant).
     */
    suspend fun route(sessionId: Long, sessionStartedAtMillis: Long): Route? {
        if (!file.exists()) return null
        val row = dao.route(sessionId)?.takeIf { it.sessionStartedAtMillis == sessionStartedAtMillis } ?: return null
        val points = dao.chunks(row.sessionId).flatMap(RouteCodec::decode)
        val altitudes = AltitudeSource.entries.firstOrNull { it.name == row.altitudeSource }
        return Route(sessionStartedAtMillis, points, altitudes).takeIf { points.isNotEmpty() }
    }

    /**
     * How many points the route of the outing [sessionId] holds, as it changes: what its page
     * reads the route again at. Null with no route.
     */
    fun observePointCount(sessionId: Long): Flow<Int?> = whenPresent(null) { dao.observePointCount(sessionId) }

    /** The outings that have a route, as they change: a route written, or deleted by the reader. */
    val routeIds: Flow<Set<Long>> = whenPresent(emptySet()) { dao.observeRouteIds().map { it.toSet() } }

    /** How many routes are kept, as it changes. */
    val count: Flow<Int> = whenPresent(0) { dao.observeRouteCount() }

    /**
     * Each route kept, by its outing's id and start, with how many points it holds, as they change:
     * what «Your outings» knows of the routes before a row on screen reads one (PLANNING.md §11
     * Phase 23, step 23.2). Nothing, and no database opened, until a route has been written.
     */
    val keys: Flow<List<RouteKey>> = whenPresent(emptyList()) {
        dao.observeRouteKeys().map { rows ->
            rows.map { RouteKey(it.sessionId, it.sessionStartedAtMillis, it.pointCount) }
        }
    }

    /**
     * Each route's box, by its outing's id, with the outing's start it belongs to: what the usual
     * rounds (Phase 20) read before any point, so only the routes that may be the same are opened.
     * Nothing, and no database opened, until a route has been written.
     */
    suspend fun boxes(): Map<Long, KeptBox> {
        if (!file.exists()) return emptyMap()
        return dao.routes().associate {
            it.sessionId to KeptBox(it.sessionStartedAtMillis, RouteBox(it.southE6, it.westE6, it.northE6, it.eastE6))
        }
    }

    /** The bytes the routes' points take: what Your data says they weigh. */
    suspend fun bytes(except: Long? = null): Long = when {
        !file.exists() -> 0L
        except == null -> dao.routeBytes()
        else -> dao.routeBytesExcept(except)
    }

    /**
     * The reader forgets every route but [keep]'s, the outing under way, whose route is still
     * being written and is theirs to delete once it is over. The outings and their numbers are
     * untouched.
     */
    suspend fun deleteAll(keep: Long? = null) {
        if (!file.exists()) return
        val gone = dao.routes().filter { it.sessionId != keep }
        gone.forEach { forgotten += it.sessionId to it.sessionStartedAtMillis }
        if (keep == null) dao.deleteAll() else gone.forEach { dao.delete(it.sessionId) }
    }

    /**
     * The reader forgets the route of the outing [sessionId], which started at
     * [sessionStartedAtMillis]: deleted, and never written again. The outing and its numbers
     * are untouched.
     */
    suspend fun forget(sessionId: Long, sessionStartedAtMillis: Long) {
        forgotten += sessionId to sessionStartedAtMillis
        delete(sessionId)
    }

    /**
     * The reader puts a route back from a file of their own: written even if they deleted it in
     * this process, which is what the file is for. [append]'s arguments.
     */
    suspend fun restore(
        sessionId: Long,
        sessionStartedAtMillis: Long,
        points: List<RoutePoint>,
        altitudes: AltitudeSource?,
    ) {
        forgotten -= sessionId to sessionStartedAtMillis
        append(sessionId, sessionStartedAtMillis, points, altitudes)
    }

    /** Deletes the route of the outing [sessionId]; the outing and its numbers are untouched. */
    suspend fun delete(sessionId: Long) {
        if (file.exists()) dao.delete(sessionId)
    }

    /**
     * Deletes every route whose outing is not among [outings] (each outing's id with its start):
     * what a crash between the deletion of a route and of its outing never leaves, since the
     * route goes first, and what a `passo.db` put back from a backup beside a newer `routes.db`
     * could. Places with no outing are not kept. Returns how many went.
     */
    suspend fun deleteOrphans(outings: Map<Long, Long>): Int {
        if (!file.exists()) return 0
        val orphans = dao.routes().filter { outings[it.sessionId] != it.sessionStartedAtMillis }
        orphans.forEach { dao.delete(it.sessionId) }
        return orphans.size
    }
}
