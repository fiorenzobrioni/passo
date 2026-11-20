package com.callbackdev.passo.core.data.sessions

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.RouteDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.OutingLeg
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.TrackerState
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

/** The outings' storage (PLANNING.md §11 Phase 10), on a real Room database. */
@RunWith(AndroidJUnit4::class)
class SessionRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    @get:Rule
    val migrations = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PassoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    private lateinit var database: PassoDatabase
    private lateinit var store: TestDataStore
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var repository: SessionRepository
    private lateinit var routeDatabase: RouteDatabase
    private lateinit var routes: RouteRepository

    private val day = 20_720L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = TestDataStore(folder.root)
        preferences = UserPreferencesDataSource(store.dataStore)
        routeDatabase = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RouteDatabase::class.java,
        ).allowMainThreadQueries().build()
        routes = RouteRepository(routeDatabase.routeDao())
        repository = SessionRepository(database.sessionDao(), preferences, routes)
    }

    @After
    fun tearDown() {
        database.close()
        routeDatabase.close()
        store.scope.cancel()
    }

    @Test
    fun `the presets are written once, and deleting them is the reader's choice`() = runTest {
        val first = repository.plans.first()
        assertThat(first.map { it.goalKind })
            .containsExactly(
                SessionGoalKind.TIME,
                SessionGoalKind.TIME,
                SessionGoalKind.REST_OF_DAY,
                SessionGoalKind.INTERVALS,
            ).inOrder()
        first.forEach { repository.deletePlan(it.id) }
        assertThat(repository.plans.first()).isEmpty()
    }

    @Test
    fun `plans seeded before the interval walk get its preset once, after theirs`() = runTest {
        // Seeded by a build without it: the three first presets only.
        preferences.markSessionPlansSeeded()
        SessionPlans.PRESETS.take(3).forEach { repository.savePlan(it) }
        val plans = repository.plans.first()
        assertThat(plans.last().goalKind).isEqualTo(SessionGoalKind.INTERVALS)
        assertThat(plans.last().intervals).isEqualTo(IntervalSets())
        repository.deletePlan(plans.last().id)
        assertThat(repository.plans.first().map { it.goalKind }).doesNotContain(SessionGoalKind.INTERVALS)
    }

    @Test
    fun `an interval outing is written with its intervals, and deleted with them`() = runTest {
        val tracking = TrackingRepository(database.trackingDao(), preferences) { day }
        val started = repository.insert(SessionPlans.start(SessionPlans.JAPANESE_WALKING, 1_000L, day, 0, 8_000)!!)
        val moved = started.copy(
            totals = SessionTotals(steps = 600, movingMillis = 200_000),
            splits = listOf(
                IntervalSplit(0, fast = false, steps = 270, movingMillis = 180_000),
                IntervalSplit(1, fast = true, steps = 40, movingMillis = 20_000, zoneMillis = 12_000),
            ),
        )
        tracking.persist(
            LedgerBatch(listOf(MinuteSteps(100, day, 600)), TrackerState(1, 500, 2L, 3L), emptyList()),
            nowWallMillis = 210_000L,
            session = moved,
        )
        assertThat(repository.liveSession()).isEqualTo(moved)

        val further = moved.copy(splits = moved.splits.dropLast(1) + moved.splits.last().copy(steps = 300))
        repository.save(further)
        assertThat(repository.session(moved.id)?.splits?.last()?.steps).isEqualTo(300)

        repository.delete(moved.id)
        assertThat(repository.session(moved.id)).isNull()
        assertThat(repository.insert(started.copy(id = 0)).splits).isEmpty()
    }

    @Test
    fun `version 5 migrates to 6 with every plan the protocol's sets, and every outing none`() {
        migrations.createDatabase(MIGRATION_DB, 5).use { db ->
            db.execSQL(
                "INSERT INTO session_plan (name, goalKind, goalValue, intensity, milestones, vibrate, position, " +
                    "lastUsedAtMillis, voice) VALUES (NULL, 'TIME', 20, 'BRISK', 2, 1, 0, NULL, 'OFF')",
            )
        }
        migrations.runMigrationsAndValidate(MIGRATION_DB, 6, true).use { db ->
            db.query("SELECT slowMinutes, fastMinutes, sets FROM session_plan").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(listOf(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2))).containsExactly(3, 3, 5)
            }
        }
    }

    @Test
    fun `version 7 migrates to 8 with every outing's climb unknown, and no legs`() {
        migrations.createDatabase(MIGRATION_DB, 7).use { db ->
            db.execSQL(
                "INSERT INTO session (planId, name, goalKind, goalValue, restOfDay, intensity, milestones, " +
                    "vibrate, localEpochDay, startedAtMillis, state, endedAtMillis, endReason, steps, " +
                    "movingMillis, zoneMillis, distanceMeters, activeKcal, lastStepAtMillis, lastEventAtMillis, " +
                    "pausedAtMillis, reachedAtMillis, toldMilestones, voice, gps, stepsMeters, measuredShare) " +
                    "VALUES (NULL, NULL, 'TIME', 20, 0, 'BRISK', 2, 1, 20716, 1789990000000, 'FINISHED', " +
                    "1789991200000, 'GOAL', 2210, 1200000, 1100000, 1630.0, 61.0, 1789991200000, " +
                    "1789991200000, NULL, 1789991200000, 6, 'OFF', 1, 1590.0, 0.96)",
            )
        }
        migrations.runMigrationsAndValidate(MIGRATION_DB, 8, true).use { db ->
            db.query("SELECT gps, measuredShare, ascentMeters, descentMeters FROM session").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(1)
                assertThat(cursor.getDouble(1)).isEqualTo(0.96)
                assertThat(cursor.isNull(2)).isTrue()
                assertThat(cursor.isNull(3)).isTrue()
            }
            db.query("SELECT COUNT(*) FROM session_leg").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    @Test
    fun `an outing with GPS keeps its climb and its legs, and loses them with itself`() = runTest {
        val plan = SessionPlans.NEW.copy(gps = true)
        val started = repository.insert(checkNotNull(SessionPlans.start(plan, 1_000, 20_720, 0, 8_000, withGps = true)))
        val legs = listOf(
            OutingLeg(0, 100.0, 71_000, 130.5, endedAtMillis = 72_000, ascentMeters = 4.0),
            OutingLeg(1, 100.0, 70_000, 128.0, endedAtMillis = 143_000, descentMeters = 3.5),
            OutingLeg(2, 40.0, 29_000, 52.25, endedAtMillis = 172_000),
        )
        repository.save(started.copy(ascentMeters = 4.0, descentMeters = 3.5, legs = legs))

        val read = checkNotNull(repository.session(started.id))
        assertThat(read.ascentMeters).isEqualTo(4.0)
        assertThat(read.descentMeters).isEqualTo(3.5)
        assertThat(read.legs).isEqualTo(legs)

        // The leg under way grows, and a new one opens: written over, never twice.
        val grown = legs.dropLast(1) + legs.last().copy(meters = 100.0, endedAtMillis = 215_000) +
            OutingLeg(3, 12.0, 9_000, 16.0, endedAtMillis = 224_000)
        repository.save(read.copy(legs = grown))
        assertThat(checkNotNull(repository.session(started.id)).legs).isEqualTo(grown)

        repository.delete(started.id)
        assertThat(
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM session_leg").use {
                it.moveToFirst()
                it.getInt(0)
            },
        ).isEqualTo(0)
    }

    @Test
    fun `an outing without GPS has no climb and no legs`() = runTest {
        val started = repository.insert(checkNotNull(SessionPlans.start(SessionPlans.NEW, 1_000, 20_720, 0, 8_000)))

        val read = checkNotNull(repository.session(started.id))
        assertThat(read.ascentMeters).isNull()
        assertThat(read.descentMeters).isNull()
        assertThat(read.legs).isEmpty()
    }

    @Test
    fun `a new plan goes last, and an edit stays in place`() = runTest {
        val presets = repository.plans.first()
        val id = repository.savePlan(
            SessionPlan(
                name = "  Park  ",
                goalKind = SessionGoalKind.DISTANCE,
                goalValue = 3_000,
                intensity = SessionIntensity.VIGOROUS,
                milestones = setOf(SessionMilestone.QUARTER, SessionMilestone.THREE_QUARTERS),
                vibrate = false,
            ),
        )
        val saved = repository.plans.first().last()
        assertThat(saved.id).isEqualTo(id)
        assertThat(saved.name).isEqualTo("Park")
        assertThat(saved.position).isEqualTo(presets.size)
        assertThat(saved.milestones).containsExactly(SessionMilestone.QUARTER, SessionMilestone.THREE_QUARTERS)
        assertThat(saved.vibrate).isFalse()

        repository.savePlan(saved.copy(goalValue = 5_000))
        assertThat(repository.plan(id)?.goalValue).isEqualTo(5_000)
        assertThat(repository.plans.first().last().id).isEqualTo(id)
    }

    @Test
    fun `the most recently started plan comes first for the shortcuts`() = runTest {
        val plans = repository.plans.first()
        repository.markPlanUsed(plans[2].id, 5_000L)
        repository.markPlanUsed(plans[1].id, 1_000L)
        assertThat(repository.plansByUse().map { it.id })
            .containsExactly(plans[2].id, plans[1].id, plans[0].id, plans[3].id).inOrder()
    }

    @Test
    fun `an outing is written with the steps that moved it, in one transaction`() = runTest {
        val tracking = TrackingRepository(database.trackingDao(), preferences) { day }
        val started = repository.insert(SessionPlans.start(SessionPlans.PRESETS[0], 1_000L, day, 0, 8_000)!!)
        assertThat(repository.liveSession()).isEqualTo(started)

        val moved = started.copy(totals = SessionTotals(steps = 120, movingMillis = 66_000))
        tracking.persist(
            LedgerBatch(listOf(MinuteSteps(100, day, 120)), TrackerState(1, 500, 2L, 3L), emptyList()),
            nowWallMillis = 70_000L,
            session = moved,
        )
        assertThat(repository.liveSession()?.totals?.steps).isEqualTo(120)
        assertThat(tracking.stepsOn(day)).isEqualTo(120)

        val finished = moved.copy(state = SessionState.FINISHED, end = SessionEnd.STOPPED, endedAtMillis = 80_000L)
        repository.save(finished)
        assertThat(repository.liveSession()).isNull()
        assertThat(repository.observeLatestFinished().first()).isEqualTo(finished)
        assertThat(repository.observeSessionsOn(day).first()).containsExactly(finished)

        repository.delete(finished.id)
        assertThat(repository.observeSessionsOn(day).first()).isEmpty()
    }

    @Test
    fun `a batch writes every leg changed since the last, however many and however far back`() = runTest {
        val tracking = TrackingRepository(database.trackingDao(), preferences) { day }
        val plan = SessionPlans.NEW.copy(gps = true)
        val started = repository.insert(checkNotNull(SessionPlans.start(plan, 1_000, day, 0, 8_000, withGps = true)))
        fun leg(index: Int, ascent: Double = 0.0) =
            OutingLeg(index, 100.0, 70_000, 130.0, endedAtMillis = 70_000L * (index + 1), ascentMeters = ascent)
        val batch = LedgerBatch(listOf(MinuteSteps(100, day, 120)), TrackerState(1, 500, 2L, 3L), emptyList())

        tracking.persist(batch, nowWallMillis = 70_000L, session = started.copy(legs = (0..2).map { leg(it) }))
        // «Keep going» lays a quarter of an hour's legs at once, and a climb lands on the first.
        val grown = listOf(leg(0, ascent = 6.0)) + (1..14).map { leg(it) }
        tracking.persist(batch, nowWallMillis = 140_000L, session = started.copy(legs = grown))

        assertThat(checkNotNull(repository.session(started.id)).legs).isEqualTo(grown)
    }

    @Test
    fun `the summary seen is remembered`() = runTest {
        assertThat(repository.summarySeen.first()).isNull()
        repository.setSummarySeen(42)
        assertThat(repository.summarySeen.first()).isEqualTo(42)
    }

    @Test
    fun `version 1 migrates to 2 with its steps kept`() {
        migrations.createDatabase(MIGRATION_DB, 1).use { db ->
            db.execSQL("INSERT INTO minute_steps (epochMinute, localEpochDay, steps) VALUES (100, $day, 42)")
        }
        migrations.runMigrationsAndValidate(MIGRATION_DB, 2, true).use { db ->
            db.query("SELECT steps FROM minute_steps").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getInt(0)).isEqualTo(42)
            }
            db.query("SELECT COUNT(*) FROM session").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    @Test
    fun `version 2 migrates to 3 with every plan silent`() {
        migrations.createDatabase(MIGRATION_DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO session_plan (name, goalKind, goalValue, intensity, milestones, vibrate, position, " +
                    "lastUsedAtMillis) VALUES (NULL, 'TIME', 20, 'BRISK', 2, 1, 0, NULL)",
            )
        }
        migrations.runMigrationsAndValidate(MIGRATION_DB, 3, true).use { db ->
            db.query("SELECT voice FROM session_plan").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("OFF")
            }
        }
    }

    @Test
    fun `a plan keeps its voice`() = runTest {
        val id = repository.savePlan(
            SessionPlan(
                goalKind = SessionGoalKind.TIME,
                goalValue = 30,
                intensity = SessionIntensity.RUN,
                voice = SessionVoice.ALWAYS,
            ),
        )
        assertThat(repository.plan(id)?.voice).isEqualTo(SessionVoice.ALWAYS)
        val started = repository.insert(SessionPlans.start(repository.plan(id)!!, 0, day, 0, 8_000)!!)
        assertThat(repository.liveSession()?.voice).isEqualTo(SessionVoice.ALWAYS)
        assertThat(started.voice).isEqualTo(SessionVoice.ALWAYS)
    }

    private companion object {
        const val MIGRATION_DB = "migration-test.db"
    }
}
