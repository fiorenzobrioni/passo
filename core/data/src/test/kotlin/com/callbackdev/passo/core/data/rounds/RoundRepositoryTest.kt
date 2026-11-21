package com.callbackdev.passo.core.data.rounds

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.RouteDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.routes.RouteFile
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.Round
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import kotlin.math.cos
import kotlin.math.hypot

/**
 * The usual rounds' storage and search (PLANNING.md §11 Phase 20), on real Room databases: the
 * repository's rows of the phase's edge cases, each a test that says so in its name.
 */
@RunWith(AndroidJUnit4::class)
class RoundRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: PassoDatabase
    private lateinit var routeDatabase: RouteDatabase
    private lateinit var store: TestDataStore
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var routes: RouteRepository
    private lateinit var sessions: SessionRepository
    private lateinit var rounds: RoundRepository
    private var routesFile = true

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, PassoDatabase::class.java).allowMainThreadQueries().build()
        routeDatabase =
            Room.inMemoryDatabaseBuilder(context, RouteDatabase::class.java).allowMainThreadQueries().build()
        store = TestDataStore(folder.root)
        preferences = UserPreferencesDataSource(store.dataStore)
        routes = RouteRepository(routeDatabase.routeDao(), RouteFile { routesFile })
        sessions = SessionRepository(database.sessionDao(), preferences, routes)
        rounds = RoundRepository(database.roundDao(), database.sessionDao(), routes, preferences)
    }

    @After
    fun tearDown() {
        database.close()
        routeDatabase.close()
        store.scope.cancel()
    }

    /** An outing over along [corners], measured with GPS, and its route; its id. */
    private suspend fun walked(
        corners: List<Pair<Double, Double>>,
        day: Long,
        minutes: Double = 40.0,
        measuredShare: Double = 1.0,
        intensity: SessionIntensity = SessionIntensity.FREE,
        withRoute: Boolean = true,
    ): Long {
        val meters = length(corners)
        val started = START + day * DAY
        val moving = (minutes * 60_000).toLong()
        val legs = (0 until (meters / 100).toInt()).map {
            OutingLeg(
                it,
                100.0,
                moving / (meters / 100).toLong(),
                140.0,
                started + moving * (it + 1) / (meters / 100).toLong(),
            )
        }
        val outing = sessions.insert(
            Session(
                planId = null,
                name = null,
                goalKind = SessionGoalKind.OPEN,
                goalValue = 0,
                intensity = intensity,
                milestones = emptySet(),
                vibrate = true,
                localEpochDay = 20_000 + day,
                startedAtMillis = started,
                state = SessionState.FINISHED,
                endedAtMillis = started + moving,
                totals = SessionTotals(steps = (meters / 0.7).toInt(), movingMillis = moving, distanceMeters = meters),
                gps = true,
                stepsMeters = meters,
                measuredShare = measuredShare,
            ),
        )
        sessions.save(outing.copy(legs = legs))
        if (withRoute) routes.append(outing.id, started, points(corners, day.toInt()))
        return outing.id
    }

    private suspend fun kept(): List<Round> = rounds.rounds.first()

    @Test
    fun `the second walk of a round makes the pair, the third joins it`() = runTest {
        val first = walked(LOOP, day = 0)
        rounds.find(first)
        assertThat(kept()).isEmpty()
        val second = walked(LOOP, day = 1)
        rounds.find(second)
        assertThat(kept().single().walks).containsExactly(first, second)
        val third = walked(LOOP, day = 2, minutes = 38.0)
        rounds.find(third)
        assertThat(kept().single().walks).containsExactly(first, second, third)
    }

    @Test
    fun `the same streets walked the other way are not the same round`() = runTest {
        rounds.find(walked(LOOP, day = 0))
        rounds.find(walked(LOOP.asReversed(), day = 1))
        assertThat(kept()).isEmpty()
    }

    @Test
    fun `an outing measured under nine tenths is not matched`() = runTest {
        rounds.find(walked(LOOP, day = 0))
        rounds.find(walked(LOOP, day = 1, measuredShare = 0.8))
        assertThat(kept()).isEmpty()
    }

    @Test
    fun `a run round the park is not the walk round it`() = runTest {
        rounds.find(walked(LOOP, day = 0))
        rounds.find(walked(LOOP, day = 1, minutes = 20.0, intensity = SessionIntensity.RUN))
        assertThat(kept()).isEmpty()
    }

    @Test
    fun `a member's route deleted, it stays a member for its times`() = runTest {
        val first = walked(LOOP, day = 0)
        val second = walked(LOOP, day = 1)
        rounds.find(second)
        routes.forget(first, START)
        assertThat(kept().single().walks).containsExactly(first, second)
        // A new walk still finds the round, through the walk that kept its route.
        val third = walked(LOOP, day = 2)
        rounds.find(third)
        assertThat(kept().single().walks).containsExactly(first, second, third)
    }

    @Test
    fun `every member's route deleted, the history stays and no new walk is matched until one with GPS is kept`() =
        runTest {
            val first = walked(LOOP, day = 0)
            val second = walked(LOOP, day = 1)
            rounds.find(second)
            routes.deleteAll()
            assertThat(kept().single().walks).containsExactly(first, second)
            val third = walked(LOOP, day = 2)
            rounds.find(third)
            assertThat(kept().single().walks).containsExactly(first, second)
            // The walk after it has a route to be matched with: a round of their own.
            val fourth = walked(LOOP, day = 3)
            rounds.find(fourth)
            assertThat(kept().map { it.walks }).containsExactly(setOf(first, second), setOf(third, fourth))
        }

    @Test
    fun `a member's outing deleted leaves the round, and under two members the round is forgotten`() = runTest {
        val first = walked(LOOP, day = 0)
        val second = walked(LOOP, day = 1)
        val third = walked(LOOP, day = 2)
        rounds.find(second)
        rounds.find(third)
        sessions.delete(third)
        assertThat(kept().single().walks).containsExactly(first, second)
        sessions.delete(second)
        assertThat(kept()).isEmpty()
        assertThat(database.roundDao().rounds()).isEmpty()
    }

    @Test
    fun `not the same round keeps the pair, and the two are never matched again`() = runTest {
        val first = walked(LOOP, day = 0)
        val second = walked(LOOP, day = 1)
        val third = walked(LOOP, day = 2)
        rounds.find(second)
        rounds.find(third)
        rounds.notTheSame(third)
        assertThat(kept().single().walks).containsExactly(first, second)
        // Searched again (an end taken back, an import): still apart.
        rounds.find(third)
        assertThat(kept().single().walks).containsExactly(first, second)
        // A walk after it is matched as ever.
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        assertThat(kept().single().walks).containsExactly(first, second, fourth)
    }

    @Test
    fun `forgetting a round lets its outings be, and a walk of it to come finds it again`() = runTest {
        val first = walked(LOOP, day = 0)
        val second = walked(LOOP, day = 1)
        rounds.find(second)
        rounds.rename(kept().single().id, "The park")
        rounds.forget(kept().single().id)
        assertThat(kept()).isEmpty()
        assertThat(sessions.session(first)).isNotNull()
        val third = walked(LOOP, day = 2)
        rounds.find(third)
        assertThat(kept().single().walks).containsExactly(second, third)
        assertThat(kept().single().name).isNull()
    }

    @Test
    fun `a name is the reader's, and a blank one names the round from its length again`() = runTest {
        walked(LOOP, day = 0)
        rounds.find(walked(LOOP, day = 1))
        val id = kept().single().id
        rounds.rename(id, "  The park  ")
        assertThat(kept().single().name).isEqualTo("The park")
        rounds.rename(id, " ")
        assertThat(kept().single().name).isNull()
    }

    @Test
    fun `the walks before the rounds were kept are searched once, oldest first`() = runTest {
        val walks = (0L..3L).map { walked(LOOP, day = it) }
        val other = walked(OTHER_LOOP, day = 4)
        rounds.searchOnce()
        assertThat(kept().single().walks).containsExactlyElementsIn(walks)
        rounds.forget(kept().single().id)
        rounds.searchOnce()
        assertThat(kept()).isEmpty()
        assertThat(other).isNotIn(walks)
    }

    @Test
    fun `the history's search keeps a round a backup brought, with its name, and adds to it`() = runTest {
        val first = walked(LOOP, day = 0)
        val second = walked(LOOP, day = 1)
        val third = walked(LOOP, day = 2)
        val dao = database.roundDao()
        val brought = dao.pair(first, second)
        dao.rename(brought, "The park")
        rounds.searchOnce()
        val round = kept().single()
        assertThat(round.id).isEqualTo(brought)
        assertThat(round.name).isEqualTo("The park")
        assertThat(round.walks).containsExactly(first, second, third)
    }

    @Test
    fun `no routes' file, no search and no file made`() = runTest {
        routesFile = false
        walked(LOOP, day = 0, withRoute = false)
        rounds.find(walked(LOOP, day = 1, withRoute = false))
        assertThat(kept()).isEmpty()
        assertThat(rounds.guides(running = false, unitMeters = 1_000.0)).isEmpty()
    }

    @Test
    fun `an open outing's start reads each round's line and its usual at each kilometre`() = runTest {
        walked(LOOP, day = 0, minutes = 32.0)
        val second = walked(LOOP, day = 1, minutes = 48.0)
        rounds.find(second)
        val guide = rounds.guides(running = false, unitMeters = 1_000.0).single()
        assertThat(guide.walks).isEqualTo(2)
        assertThat(guide.line).isNotEmpty()
        // Ten and fifteen minutes a kilometre: the middle is twelve and a half.
        assertThat(guide.usualAtMarks.first()).isEqualTo(750_000L)
        assertThat(rounds.guides(running = true, unitMeters = 1_000.0)).isEmpty()
    }

    private companion object {
        const val START = 1_800_000_000_000L
        const val DAY = 86_400_000L
        const val LATITUDE = 45.4642
        const val LONGITUDE = 9.19
        const val METERS_PER_DEGREE = 111_320.0

        val LOOP = listOf(0.0 to 0.0, 800.0 to 0.0, 800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0)
        val OTHER_LOOP = listOf(0.0 to 0.0, 0.0 to -800.0, -800.0 to -800.0, -800.0 to 0.0, 0.0 to 0.0)

        fun length(corners: List<Pair<Double, Double>>): Double = (1 until corners.size).sumOf {
            hypot(corners[it].first - corners[it - 1].first, corners[it].second - corners[it - 1].second)
        }

        /** A point every five metres along [corners], each a few metres off the line, as a receiver draws it. */
        fun points(corners: List<Pair<Double, Double>>, seed: Int): List<RoutePoint> {
            val out = ArrayList<RoutePoint>()
            var second = START / 1_000
            for (index in 1 until corners.size) {
                val (x0, y0) = corners[index - 1]
                val (x1, y1) = corners[index]
                val steps = (hypot(x1 - x0, y1 - y0) / 5).toInt()
                for (step in (if (index == 1) 0 else 1)..steps) {
                    val t = step.toDouble() / steps
                    val wander = ((step * 7 + seed * 3) % 9 - 4).toDouble()
                    val east = x0 + (x1 - x0) * t + wander
                    val north = y0 + (y1 - y0) * t - wander
                    out += RoutePoint(
                        epochSecond = second,
                        latitudeE6 = ((LATITUDE + north / METERS_PER_DEGREE) * 1e6).toInt(),
                        longitudeE6 = ((LONGITUDE + east / (METERS_PER_DEGREE * cos(Math.toRadians(LATITUDE)))) * 1e6)
                            .toInt(),
                        altitudeDm = null,
                        accuracyMeters = 5,
                        kind = if (out.isEmpty()) RoutePointKind.START else RoutePointKind.MEASURED,
                    )
                    second += 4
                }
            }
            return out
        }
    }
}
