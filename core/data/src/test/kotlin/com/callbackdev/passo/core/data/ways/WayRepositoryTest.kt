package com.callbackdev.passo.core.data.ways

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.DailySummaryEntity
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourneyState
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
import java.time.LocalDate

/** The ways' storage (PLANNING.md §11 Phase 11), on a real Room database. */
@RunWith(AndroidJUnit4::class)
class WayRepositoryTest {
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
    private lateinit var repository: WayRepository

    private val today = LocalDate.of(2026, 10, 1)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = TestDataStore(folder.root)
        val tracking =
            TrackingRepository(database.trackingDao(), UserPreferencesDataSource(store.dataStore)) {
                today.toEpochDay()
            }
        repository = WayRepository(database.wayDao(), tracking)
    }

    @After
    fun tearDown() {
        database.close()
        store.scope.cancel()
    }

    private suspend fun walked(vararg days: Pair<LocalDate, Double>) {
        database.trackingDao().importDays(
            minutes = emptyList(),
            summaries = days.map { (date, meters) ->
                DailySummaryEntity(date.toEpochDay(), (meters / 0.7).toInt(), meters, 0.0, 0, 0, 8_000, true)
            },
        )
    }

    @Test
    fun `a start in the past is told up to where the reader already stands`() = runTest {
        walked(today.minusDays(2) to 12_000.0, today.minusDays(1) to 15_000.0)
        val id = repository.start(WayId.VIA_DEGLI_DEI, today.minusDays(2), today, nowMillis = 1_000)
        val journey = repository.active.first()
        assertThat(journey?.id).isEqualTo(id)
        assertThat(journey?.toldMeters).isEqualTo(27_000)
        assertThat(journey?.state).isEqualTo(WayJourneyState.ACTIVE)
    }

    @Test
    fun `one way at a time`() = runTest {
        assertThat(repository.start(WayId.VIA_DEGLI_DEI, today, today, 1_000)).isNotNull()
        assertThat(repository.start(WayId.CAMINO_FRANCES, today, today, 2_000)).isNull()
        assertThat(repository.journeys.first()).hasSize(1)
    }

    @Test
    fun `a way left or finished ends once, and frees the place`() = runTest {
        val id = checkNotNull(repository.start(WayId.VIA_DEGLI_DEI, today, today, 1_000))
        repository.leave(id, today)
        repository.finish(id, today.plusDays(3).toEpochDay()) // too late: already left
        val left = repository.journeys.first().single()
        assertThat(left.state).isEqualTo(WayJourneyState.LEFT)
        assertThat(left.endedEpochDay).isEqualTo(today.toEpochDay())
        assertThat(repository.active.first()).isNull()
        assertThat(repository.start(WayId.VIA_FRANCIGENA, today, today, 2_000)).isNotNull()
    }

    @Test
    fun `a stage told stays told, whoever writes last`() = runTest {
        val id = checkNotNull(repository.start(WayId.VIA_DEGLI_DEI, today, today, 1_000))
        val stage = Ways.of(WayId.VIA_DEGLI_DEI).stages[2].distanceMeters
        assertThat(repository.claimTold(id, stage)).isTrue()
        assertThat(repository.claimTold(id, stage)).isFalse()
        assertThat(repository.claimTold(id, stage - 10_000)).isFalse()
        assertThat(repository.activeNow()?.toldMeters).isEqualTo(stage)
    }

    @Test
    fun `version 3 migrates to 4 with its outings kept and no way started`() {
        migrations.createDatabase(MIGRATION_DB, 3).use { db ->
            db.execSQL(
                "INSERT INTO session_plan (name, goalKind, goalValue, intensity, milestones, vibrate, position, " +
                    "lastUsedAtMillis, voice) VALUES ('Park', 'TIME', 20, 'BRISK', 2, 1, 0, NULL, 'OFF')",
            )
        }
        migrations.runMigrationsAndValidate(MIGRATION_DB, 4, true).use { db ->
            db.query("SELECT name FROM session_plan").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getString(0)).isEqualTo("Park")
            }
            db.query("SELECT COUNT(*) FROM way_journey").use { cursor ->
                cursor.moveToFirst()
                assertThat(cursor.getInt(0)).isEqualTo(0)
            }
        }
    }

    @Test
    fun `a city walk is not a way, never blocks one, and keeps one journey of its own`() = runTest {
        assertThat(repository.start(WayId.VIA_DEGLI_DEI, today, today, 1_000)).isNotNull()
        val milan = repository.walkJourney(WayId.MILAN_DUOMO_NAVIGLI, again = false, today = today, nowMillis = 2_000)
        assertThat(
            repository.walkJourney(WayId.MILAN_DUOMO_NAVIGLI, again = false, today = today, nowMillis = 3_000).id,
        )
            .isEqualTo(milan.id)
        val london = repository.walkJourney(WayId.LONDON_PALACE_TOWER, again = false, today = today, nowMillis = 4_000)
        assertThat(london.id).isNotEqualTo(milan.id)
        assertThat(repository.active.first()?.way).isEqualTo(WayId.VIA_DEGLI_DEI)
        assertThat(repository.activeNow()?.way).isEqualTo(WayId.VIA_DEGLI_DEI)
    }

    @Test
    fun `a walk begun again puts the one under way down first`() = runTest {
        val first = repository.walkJourney(WayId.MILAN_DUOMO_NAVIGLI, again = false, today = today, nowMillis = 2_000)
        val again = repository.walkJourney(WayId.MILAN_DUOMO_NAVIGLI, again = true, today = today, nowMillis = 5_000)
        assertThat(again.id).isNotEqualTo(first.id)
        assertThat(again.startedAtMillis).isEqualTo(5_000)
        val journeys = repository.journeys.first()
        assertThat(journeys.single { it.id == first.id }.state).isEqualTo(WayJourneyState.LEFT)
        assertThat(journeys.single { it.id == again.id }.state).isEqualTo(WayJourneyState.ACTIVE)
    }

    @Test
    fun `a walk walked to its end stays finished, and the next outing begins it anew`() = runTest {
        val first = repository.walkJourney(WayId.LONDON_PALACE_TOWER, again = false, today = today, nowMillis = 2_000)
        repository.finishWalk(WayId.LONDON_PALACE_TOWER, today.toEpochDay())
        val next = repository.walkJourney(WayId.LONDON_PALACE_TOWER, again = false, today = today, nowMillis = 6_000)
        assertThat(next.id).isNotEqualTo(first.id)
        val journeys = repository.journeys.first()
        assertThat(journeys.single { it.id == first.id }.state).isEqualTo(WayJourneyState.FINISHED)
        assertThat(journeys.single { it.id == first.id }.endedEpochDay).isEqualTo(today.toEpochDay())
    }

    @Test
    fun `a journey deleted is gone, and another way can start`() = runTest {
        val id = checkNotNull(repository.start(WayId.VIA_DEGLI_DEI, today, today, nowMillis = 1_000))
        repository.delete(id)
        assertThat(repository.journeys.first()).isEmpty()
        assertThat(repository.start(WayId.VIA_DI_FRANCESCO, today, today, nowMillis = 2_000)).isNotNull()
    }

    @Test
    fun `version 4 migrates to 5 with every outing walking no walk`() {
        migrations.createDatabase(MIGRATION_DB, 4).use { db ->
            db.execSQL(
                "INSERT INTO session (planId, name, goalKind, goalValue, restOfDay, intensity, milestones, vibrate, " +
                    "localEpochDay, startedAtMillis, state, endedAtMillis, endReason, steps, movingMillis, " +
                    "zoneMillis, distanceMeters, activeKcal, lastStepAtMillis, lastEventAtMillis, pausedAtMillis, " +
                    "reachedAtMillis, toldMilestones, voice) VALUES (NULL, NULL, 'TIME', 20, 0, 'BRISK', 2, 1, " +
                    "20000, 1000, 'FINISHED', 2000, 'GOAL', 2400, 1200000, 1200000, 1700.0, 90.0, 2000, 2000, " +
                    "NULL, 2000, 3, 'OFF')",
            )
        }
        migrations.runMigrationsAndValidate(MIGRATION_DB, 5, true).use { db ->
            db.query("SELECT walk, walkFromMeters, steps FROM session").use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.isNull(0)).isTrue()
                assertThat(cursor.getInt(1)).isEqualTo(0)
                assertThat(cursor.getInt(2)).isEqualTo(2_400)
            }
        }
    }

    private companion object {
        const val MIGRATION_DB = "migration-ways.db"
    }
}
