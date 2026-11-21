package com.callbackdev.passo.core.data.routes

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.db.RouteDatabase
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What «Your outings» knows of the routes (PLANNING.md §11 Phase 23, step 23.2): their keys, read
 * without a point and never from a file that is not there, and each small picture read once, when
 * its row asks, then drawn again from what was kept.
 */
@RunWith(AndroidJUnit4::class)
class RouteThumbnailsTest {
    private lateinit var database: RouteDatabase
    private lateinit var routes: RouteRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RouteDatabase::class.java,
        ).allowMainThreadQueries().build()
        routes = RouteRepository(database.routeDao())
    }

    @After
    fun tearDown() = database.close()

    /** [count] points north from Milan's cathedral, one a second, from [from]. */
    private fun points(count: Int, from: Int = 0) = List(count) { offset ->
        val index = from + offset
        RoutePoint(
            epochSecond = START_SECOND + index,
            latitudeE6 = 45_464_200 + index * 14,
            longitudeE6 = 9_191_400 + (index % 40) * 3,
            altitudeDm = null,
            accuracyMeters = 5,
            kind = if (index == 0) RoutePointKind.START else RoutePointKind.MEASURED,
        )
    }

    @Test
    fun `each route's key says its outing and its size, and no point of it`() = runTest {
        routes.append(7, START_SECOND * 1_000, points(300))
        routes.append(9, START_SECOND * 1_000 + 3_600_000, points(40))

        assertThat(routes.keys.first()).containsExactly(
            RouteKey(7, START_SECOND * 1_000, 300),
            RouteKey(9, START_SECOND * 1_000 + 3_600_000, 40),
        )
    }

    @Test
    fun `with no routes' file there are no keys, and nothing is opened to say so`() = runTest {
        // A route in the database, behind a file that says it is not there: never read.
        routes.append(7, START_SECOND * 1_000, points(300))
        val none = RouteRepository(database.routeDao(), RouteFile { false })

        assertThat(none.keys.first()).isEmpty()
        assertThat(RouteThumbnails(none).of(RouteKey(7, START_SECOND * 1_000, 300))).isNull()
    }

    @Test
    fun `a picture is read once, thinned, and drawn again from what was kept`() = runTest {
        // A long walk: twelve thousand fixes.
        routes.append(7, START_SECOND * 1_000, points(12_000))
        val key = routes.keys.first().single()
        val thumbnails = RouteThumbnails(routes)

        assertThat(thumbnails.peek(key)).isNull()
        val shape = checkNotNull(thumbnails.of(key))
        assertThat(shape.points.size).isLessThan(2_000)
        assertThat(shape.start).isEqualTo(points(1).single())
        assertThat(thumbnails.peek(key)).isSameInstanceAs(shape)

        // Drawn again from what was kept, the database not asked: deleted meanwhile, it still draws
        // for the frame its row is on, and its key is gone from the list's next read.
        routes.delete(7)
        assertThat(thumbnails.of(key)).isSameInstanceAs(shape)
        assertThat(routes.keys.first()).isEmpty()
    }

    @Test
    fun `a route that grew is read again, and one of another outing is not drawn`() = runTest {
        routes.append(7, START_SECOND * 1_000, points(100))
        val thumbnails = RouteThumbnails(routes)
        val before = checkNotNull(thumbnails.of(routes.keys.first().single()))

        // «Keep going»: the line goes on, and its key with it.
        routes.append(7, START_SECOND * 1_000, points(100, from = 100))
        val after = checkNotNull(thumbnails.of(routes.keys.first().single()))
        assertThat(after.end).isNotEqualTo(before.end)

        // The same id, another start: not this outing's line.
        assertThat(thumbnails.of(RouteKey(7, START_SECOND * 1_000 - 86_400_000, 200))).isNull()
    }

    private companion object {
        const val START_SECOND = 1_795_000_000L
    }
}
