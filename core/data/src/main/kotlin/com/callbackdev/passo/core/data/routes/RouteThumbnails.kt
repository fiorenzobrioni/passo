package com.callbackdev.passo.core.data.routes

import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.routes.thinnedTo
import com.callbackdev.passo.core.model.RouteKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The small pictures of routes a list draws (PLANNING.md §11 Phase 23, step 23.2): a route read
 * from `routes.db` when its row comes on screen, never before, and kept thinned to what a small
 * picture shows, so that a row scrolled away and back draws at once and a long walk costs a few
 * hundred points, not its every fix.
 *
 * A picture is kept by its route's [RouteKey], point count included: a route that grew (an outing
 * taken on again by «Keep going») is read again, and one the reader deleted is no longer asked
 * for, since its key is gone. Only so many are kept, the least recently drawn going first. The
 * routes' file is never made here: [RouteRepository.route] asks for it first.
 */
@Singleton
class RouteThumbnails
@Inject
constructor(private val routes: RouteRepository) {
    // Least recently drawn first: a LinkedHashMap in access order, held under its own lock.
    private val kept = object : LinkedHashMap<RouteKey, RouteShape>(CAPACITY, LOAD_FACTOR, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<RouteKey, RouteShape>?): Boolean =
            size > CAPACITY
    }

    /** The picture drawn before for [key], for a row's first frame; null when it must be read. */
    fun peek(key: RouteKey): RouteShape? = synchronized(kept) { kept[key] }

    /**
     * The picture of the route [key] names, read and thinned off the main thread the first time;
     * null when there is none to draw (deleted meanwhile, or of another outing).
     */
    suspend fun of(key: RouteKey): RouteShape? {
        peek(key)?.let { return it }
        val shape = withContext(Dispatchers.Default) {
            routes.route(key.sessionId, key.sessionStartedAtMillis)
                ?.let { RouteShape.of(it.points) }
                ?.thinnedTo(PIXELS)
                // Its lines and its frame made here, off the main thread, not on a frame.
                ?.also {
                    it.lines
                    it.frame
                }
        } ?: return null
        synchronized(kept) { kept[key] = shape }
        return shape
    }

    private companion object {
        /**
         * The pixels across the largest picture a list draws: 56dp at four pixels a dp. A point
         * within one of them of the last one kept adds nothing a screen can show.
         */
        const val PIXELS = 224

        /** Some four screens of rows: scrolling back and forth draws from here. */
        const val CAPACITY = 64
        const val LOAD_FACTOR = 0.75f
    }
}
