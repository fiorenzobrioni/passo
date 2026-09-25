package com.callbackdev.passo.core.data.sessions

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
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

    private val day = 20_720L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = TestDataStore(folder.root)
        preferences = UserPreferencesDataSource(store.dataStore)
        repository = SessionRepository(database.sessionDao(), preferences)
    }

    @After
    fun tearDown() {
        database.close()
        store.scope.cancel()
    }

    @Test
    fun `the presets are written once, and deleting them is the reader's choice`() = runTest {
        val first = repository.plans.first()
        assertThat(first.map { it.goalKind })
            .containsExactly(SessionGoalKind.TIME, SessionGoalKind.TIME, SessionGoalKind.REST_OF_DAY).inOrder()
        first.forEach { repository.deletePlan(it.id) }
        assertThat(repository.plans.first()).isEmpty()
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
            .containsExactly(plans[2].id, plans[1].id, plans[0].id).inOrder()
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

    private companion object {
        const val MIGRATION_DB = "migration-test.db"
    }
}
