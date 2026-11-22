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
import com.callbackdev.passo.core.domain.rounds.RoundMerge
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

    /** Three walks round the park, [from] days on, each searched as it ends: a round; their ids. */
    private suspend fun aRound(from: Long = 0): List<Long> = (from until from + 3).map { day ->
        walked(LOOP, day = day).also { rounds.find(it) }
    }

    @Test
    fun `a round is born at its third walk, the walk the same as two others, and the fourth joins it`() = runTest {
        val first = walked(LOOP, day = 0)
        rounds.find(first)
        val second = walked(LOOP, day = 1)
        rounds.find(second)
        // Two walks are not yet a round: nothing kept that the reader could not see.
        assertThat(kept()).isEmpty()
        assertThat(database.roundDao().rounds()).isEmpty()
        val third = walked(LOOP, day = 2, minutes = 38.0)
        rounds.find(third)
        assertThat(kept().single().walks).containsExactly(first, second, third)
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        assertThat(kept().single().walks).containsExactly(first, second, third, fourth)
    }

    @Test
    fun `the same streets walked the other way are not the same round`() = runTest {
        rounds.find(walked(LOOP, day = 0))
        rounds.find(walked(LOOP, day = 1))
        rounds.find(walked(LOOP.asReversed(), day = 2))
        assertThat(kept()).isEmpty()
    }

    @Test
    fun `an outing measured under nine tenths is not matched`() = runTest {
        rounds.find(walked(LOOP, day = 0))
        rounds.find(walked(LOOP, day = 1))
        rounds.find(walked(LOOP, day = 2, measuredShare = 0.8))
        assertThat(kept()).isEmpty()
    }

    @Test
    fun `a run round the park is not the walk round it`() = runTest {
        rounds.find(walked(LOOP, day = 0))
        rounds.find(walked(LOOP, day = 1))
        rounds.find(walked(LOOP, day = 2, minutes = 20.0, intensity = SessionIntensity.RUN))
        assertThat(kept()).isEmpty()
    }

    @Test
    fun `a walk the same as walks of two rounds makes them one`() = runTest {
        val dao = database.roundDao()
        // Two rounds of the same streets, kept apart until now (an import, a round forgotten).
        val first = checkNotNull(dao.start((0L..2L).map { walked(LOOP, day = it) }))
        dao.rename(first, "The park")
        val second = checkNotNull(dao.start((3L..6L).map { walked(LOOP, day = it) }))
        val next = walked(LOOP, day = 7)
        rounds.find(next)
        val one = kept().single()
        // The one with more walks keeps its id; the name is the one the reader gave.
        assertThat(one.id).isEqualTo(second)
        assertThat(one.name).isEqualTo("The park")
        assertThat(one.walks).hasSize(8)
    }

    @Test
    fun `a member's route deleted, it stays a member for its times`() = runTest {
        val (first, second, third) = aRound()
        routes.forget(first, START)
        assertThat(kept().single().walks).containsExactly(first, second, third)
        // A new walk still finds the round, through the walks that kept their route.
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        assertThat(kept().single().walks).containsExactly(first, second, third, fourth)
    }

    @Test
    fun `every member's route deleted, the history stays and no new walk is matched until one with GPS is kept`() =
        runTest {
            val old = aRound().toSet()
            routes.deleteAll()
            assertThat(kept().single().walks).isEqualTo(old)
            // The next walks have nothing with a route to be matched with but each other: a round of
            // their own at the third.
            val next = (3L..5L).map { day -> walked(LOOP, day = day).also { rounds.find(it) } }
            assertThat(kept().map { it.walks }).containsExactly(old, next.toSet())
        }

    @Test
    fun `the third walk makes the round known though the first two were too far apart to match`() = runTest {
        // A detour of 300 m one day, a shortcut the next: more than a tenth apart, no round yet.
        val detour = listOf(0.0 to 0.0, 300.0 to 0.0, 300.0 to -150.0, 300.0 to 0.0, 800.0 to 0.0) +
            listOf(800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0)
        val shortcut = listOf(0.0 to 0.0, 640.0 to 0.0, 800.0 to 160.0, 800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0)
        val first = walked(detour, day = 0)
        val second = walked(shortcut, day = 1)
        rounds.find(second)
        assertThat(kept()).isEmpty()
        // The round as a rule: the same as each, and the three are one round, known.
        val third = walked(LOOP, day = 2)
        rounds.find(third)
        assertThat(kept().single().walks).containsExactly(first, second, third)
        assertThat(rounds.kept.first().single().walks).hasSize(3)
    }

    @Test
    fun `a round with no line and the round its next walks made, made one as the reader said`() = runTest {
        // «Along the canals»: three walks, named, then every route deleted.
        walked(LOOP, day = 0)
        rounds.find(walked(LOOP, day = 1))
        rounds.find(walked(LOOP, day = 2))
        val canals = kept().single().id
        rounds.rename(canals, "Along the canals")
        routes.deleteAll()
        // Three walks of it with GPS: a round of their own, beside it.
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        rounds.find(walked(LOOP, day = 4))
        rounds.find(walked(LOOP, day = 5))
        val since = kept().single { it.id != canals }
        assertThat(since.walks).hasSize(3)
        val shown = rounds.kept.first()
        val lost = shown.single { it.id == canals }
        val lined = shown.single { it.id == since.id }
        assertThat(RoundMerge.proposal(lined, shown, rounds.apart.first())?.id).isEqualTo(canals)
        // «The same»: from the new round's page, into it, with the old round's name.
        rounds.merge(into = since.id, from = canals, name = RoundMerge.name(lined, lost))
        val one = kept().single()
        assertThat(one.id).isEqualTo(since.id)
        assertThat(one.name).isEqualTo("Along the canals")
        assertThat(one.walks).hasSize(6)
        assertThat(database.roundDao().rounds().map { it.id }).containsExactly(since.id)
        // Its map and the card's line are the walks with GPS since; its usual is all six.
        val kept = rounds.kept.first().single()
        assertThat(kept.walks).hasSize(6)
        assertThat(kept.route).isNotNull()
        assertThat(rounds.guides(running = false, unitMeters = 1_000.0).single().walks).isEqualTo(6)
    }

    @Test
    fun `after an import, a round of older walks with their routes is joined to the phone's by its line`() = runTest {
        val dao = database.roundDao()
        // The phone's round, named: three walks round the park.
        val phone = checkNotNull(dao.start((10L..12L).map { walked(LOOP, day = it) }))
        dao.rename(phone, "The park")
        // Another round of the phone's, as long, from the same door: not this one.
        val other = checkNotNull(dao.start((13L..15L).map { walked(OTHER_LOOP, day = it) }))
        // The file's: three older walks round the park, a round of their own.
        val brought = (0L..2L).map { walked(LOOP, day = it) }
        val file = checkNotNull(dao.start(brought))
        rounds.joinAlike(brought)
        val after = kept()
        assertThat(after.map { it.id }).containsExactly(phone, other)
        val park = after.single { it.id == phone }
        assertThat(park.name).isEqualTo("The park")
        assertThat(park.walks).hasSize(6)
        assertThat(after.single { it.id == other }.walks).hasSize(3)
        assertThat(dao.rounds().map { it.id }).doesNotContain(file)
    }

    @Test
    fun `rounds the import did not touch, or the reader said are not one, are left as they are`() = runTest {
        val dao = database.roundDao()
        val first = checkNotNull(dao.start((0L..2L).map { walked(LOOP, day = it) }))
        val second = checkNotNull(dao.start((3L..5L).map { walked(LOOP, day = it) }))
        // Nothing of either came with the file.
        rounds.joinAlike(listOf(walked(OTHER_LOOP, day = 9)))
        assertThat(kept().map { it.id }).containsExactly(first, second)
        // «Another round», said before: kept two.
        rounds.notOneRound(first, second)
        rounds.joinAlike(dao.walksOf(second))
        assertThat(kept().map { it.id }).containsExactly(first, second)
    }

    @Test
    fun `another round, and the two are never asked about again, whatever walk is deleted`() = runTest {
        walked(LOOP, day = 0)
        (1L..3L).forEach { rounds.find(walked(LOOP, day = it)) }
        val canals = kept().single().id
        routes.deleteAll()
        val fourth = walked(LOOP, day = 4)
        rounds.find(fourth)
        (5L..7L).forEach { rounds.find(walked(LOOP, day = it)) }
        val since = kept().single { it.id != canals }.id
        val before = rounds.kept.first()
        assertThat(RoundMerge.proposal(before.single { it.id == since }, before, rounds.apart.first())).isNotNull()
        rounds.notOneRound(since, canals)
        val shown = rounds.kept.first()
        assertThat(shown).hasSize(2)
        shown.forEach { assertThat(RoundMerge.proposal(it, shown, rounds.apart.first())).isNull() }
        // One of the walks deleted: the answer stands through the others.
        sessions.delete(fourth)
        val left = rounds.kept.first()
        assertThat(left).hasSize(2)
        left.forEach { assertThat(RoundMerge.proposal(it, left, rounds.apart.first())).isNull() }
    }

    @Test
    fun `a member's outing deleted leaves the round, and under three walks the round is forgotten`() = runTest {
        val walks = aRound()
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        sessions.delete(fourth)
        assertThat(kept().single().walks).containsExactlyElementsIn(walks)
        sessions.delete(walks[2])
        assertThat(kept()).isEmpty()
        assertThat(database.roundDao().rounds()).isEmpty()
    }

    @Test
    fun `not the same round keeps the pair, and the two are never matched again`() = runTest {
        val (first, second, third) = aRound()
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        rounds.notTheSame(fourth)
        assertThat(kept().single().walks).containsExactly(first, second, third)
        // Searched again (an end taken back, an import): still apart.
        rounds.find(fourth)
        assertThat(kept().single().walks).containsExactly(first, second, third)
        // A walk after it is matched as ever.
        val fifth = walked(LOOP, day = 4)
        rounds.find(fifth)
        assertThat(kept().single().walks).containsExactly(first, second, third, fifth)
    }

    @Test
    fun `forgetting a round lets its outings be, and a walk of it to come finds it again`() = runTest {
        val (first, second, third) = aRound()
        rounds.rename(kept().single().id, "The park")
        rounds.forget(kept().single().id)
        assertThat(kept()).isEmpty()
        assertThat(sessions.session(first)).isNotNull()
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        // Found again as any round is: with every walk in no round it is the same as, its name gone.
        assertThat(kept().single().walks).containsExactly(first, second, third, fourth)
        assertThat(kept().single().name).isNull()
    }

    @Test
    fun `a name is the reader's, and a blank one names the round from its length again`() = runTest {
        aRound()
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
        val fourth = walked(LOOP, day = 3)
        val dao = database.roundDao()
        val brought = checkNotNull(dao.start(listOf(first, second, third)))
        dao.rename(brought, "The park")
        rounds.searchOnce()
        val round = kept().single()
        assertThat(round.id).isEqualTo(brought)
        assertThat(round.name).isEqualTo("The park")
        assertThat(round.walks).containsExactly(first, second, third, fourth)
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
        rounds.find(walked(LOOP, day = 1, minutes = 48.0))
        rounds.find(walked(LOOP, day = 2, minutes = 40.0))
        val guide = rounds.guides(running = false, unitMeters = 1_000.0).single()
        assertThat(guide.walks).isEqualTo(3)
        assertThat(guide.lines).hasSize(3)
        // Ten, fifteen and twelve and a half minutes a kilometre: the middle is twelve and a half.
        assertThat(guide.usualAtMarks.first()).isEqualTo(750_000L)
        assertThat(rounds.guides(running = true, unitMeters = 1_000.0)).isEmpty()
    }

    @Test
    fun `an open outing's start follows the lines of the round's latest three walks with a route`() = runTest {
        walked(LOOP, day = 0)
        val second = walked(LOOP, day = 1)
        rounds.find(second)
        val third = walked(LOOP, day = 2)
        rounds.find(third)
        rounds.find(walked(LOOP, day = 3))
        val newest = walked(LOOP, day = 4)
        rounds.find(newest)
        assertThat(kept().single().walks).hasSize(5)
        // Three lines at most, the newest walks' first.
        assertThat(rounds.guides(running = false, unitMeters = 1_000.0).single().lines).hasSize(3)
        // A walk without its route gives none: the three newest that keep one.
        routes.forget(newest, START + 4 * DAY)
        routes.forget(third, START + 2 * DAY)
        val guide = rounds.guides(running = false, unitMeters = 1_000.0).single()
        assertThat(guide.walks).isEqualTo(5)
        assertThat(guide.lines).hasSize(3)
        routes.forget(second, START + DAY)
        assertThat(rounds.guides(running = false, unitMeters = 1_000.0).single().lines).hasSize(2)
    }

    @Test
    fun `an outing taken back with Keep going is not its own round's walk at a restart`() = runTest {
        aRound()
        val fourth = walked(LOOP, day = 3)
        rounds.find(fourth)
        // Ended, found, then taken back: under way again, still its round's walk until it ends.
        val going = checkNotNull(sessions.session(fourth))
        sessions.save(going.copy(state = SessionState.ACTIVE, endedAtMillis = null))
        assertThat(kept().single().walks).contains(fourth)
        val guide = rounds.guides(running = false, unitMeters = 1_000.0).single()
        assertThat(guide.walks).isEqualTo(3)
        assertThat(guide.lines).hasSize(3)
    }

    @Test
    fun `an end taken back and ended again keeps its round while it is still the same, name and all`() = runTest {
        val walks = aRound()
        val id = kept().single().id
        rounds.rename(id, "The park")
        // The third walk ends again, a little longer: still the round, which is not undone.
        rounds.find(walks[2])
        val round = kept().single()
        assertThat(round.id).isEqualTo(id)
        assertThat(round.name).isEqualTo("The park")
        assertThat(round.walks).containsExactlyElementsIn(walks)
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
