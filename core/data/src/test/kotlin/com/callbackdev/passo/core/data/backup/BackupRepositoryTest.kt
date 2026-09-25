package com.callbackdev.passo.core.data.backup

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.db.toEntity
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.backup.BackupCodec
import com.callbackdev.passo.core.domain.backup.BackupRead
import com.callbackdev.passo.core.domain.backup.CsvTable
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.model.TrackerState
import com.callbackdev.passo.core.model.UnitSystem
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
import java.io.File
import java.time.ZoneOffset

/**
 * The export and the import (PLANNING.md §11 Phase 7) between two phones, each a real Room
 * database and a real settings file: the phase's acceptance is the first test.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private val day = 20_720L
    private var today = day + 2

    /** One phone: its database, its settings, and the repositories over them. */
    private inner class Phone(name: String) {
        val database: PassoDatabase =
            Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val store = TestDataStore(File(folder.root, name).apply { mkdirs() })
        val preferences = UserPreferencesDataSource(store.dataStore)
        val tracking = TrackingRepository(database.trackingDao(), preferences) { today }
        val backups = BackupRepository(database.trackingDao(), database.sessionDao(), preferences, tracking)

        suspend fun walk(vararg minutes: MinuteSteps) {
            tracking.persist(
                LedgerBatch(increments = minutes.toList(), state = null, diagnostics = emptyList()),
                nowWallMillis = 0L,
            )
        }

        suspend fun summaries() = database.trackingDao().allSummaries()

        suspend fun minutes() = database.trackingDao().allMinutes()

        fun close() {
            database.close()
            store.scope.cancel()
        }
    }

    private lateinit var old: Phone
    private lateinit var new: Phone

    @Before
    fun setUp() {
        old = Phone("old")
        new = Phone("new")
    }

    @After
    fun tearDown() {
        old.close()
        new.close()
    }

    private fun minute(day: Long, minuteOfDay: Int, steps: Int) = MinuteSteps(day * 1_440 + minuteOfDay, day, steps)

    /** Three days on the old phone, a profile change in the middle, a goal, an outing and its plan. */
    private suspend fun history() {
        old.tracking.changeProfile { it.copy(heightMeters = 1.82, weightKg = 80.0) }
        old.tracking.changeSettings {
            it.copy(dailyGoalSteps = 9_500, theme = ThemeMode.DARK, onboardingCompleted = true)
        }
        today = day
        old.walk(minute(day, 600, 104), minute(day, 601, 98), minute(day, 900, 12))
        today = day + 1
        old.walk(minute(day + 1, 480, 120), minute(day + 1, 481, 133))
        old.tracking.changeProfile {
            it.copy(stepLengthMode = StepLengthMode.CALIBRATED, walkingStepLengthMeters = 0.78)
        }
        today = day + 2
        old.walk(minute(day + 2, 420, 90))
        val planId = old.database.sessionDao().appendPlan(
            SessionPlan(
                name = "Canal",
                goalKind = SessionGoalKind.DISTANCE,
                goalValue = 3_000,
                intensity = SessionIntensity.BRISK,
                milestones = setOf(SessionMilestone.HALF),
            ).toEntity(),
        )
        old.database.sessionDao().insertSession(
            com.callbackdev.passo.core.model.Session(
                planId = planId,
                name = "Canal",
                goalKind = SessionGoalKind.DISTANCE,
                goalValue = 3_000,
                intensity = SessionIntensity.BRISK,
                milestones = setOf(SessionMilestone.HALF),
                vibrate = true,
                localEpochDay = day + 1,
                startedAtMillis = (day + 1) * 86_400_000 + 8 * 3_600_000,
                state = SessionState.FINISHED,
                endedAtMillis = (day + 1) * 86_400_000 + 9 * 3_600_000,
                end = SessionEnd.GOAL,
                totals = SessionTotals(steps = 4_000, movingMillis = 2_400_000, distanceMeters = 3_000.0),
            ).toEntity(),
        )
        old.database.trackingDao().writeBatch(
            increments = emptyList(),
            state = com.callbackdev.passo.core.data.db.TrackerStateEntity(
                bootCount = 7,
                lastCounterValue = 55_000,
                lastSampleElapsedNanos = 1,
                lastSampleWallMillis = 1,
                updatedAtMillis = 1,
            ),
            diagnostics = emptyList(),
            profile = old.preferences.current().profile,
            goalSteps = 9_500,
            today = today,
            diagnosticsKept = 10,
        )
    }

    private suspend fun exported(): String =
        BackupCodec.encode(old.backups.backup(appVersion = "0.9.0", nowMillis = 1L, zone = ZoneOffset.UTC))

    private fun read(text: String) = (BackupCodec.decode(text) as BackupRead.Ok).backup

    @Test
    fun `export and import on a clean install reproduce the history exactly`() = runTest {
        history()

        val report = new.backups.import(read(exported()), withPreferences = true)

        assertThat(new.summaries()).isEqualTo(old.summaries())
        assertThat(new.minutes()).isEqualTo(old.minutes())
        assertThat(new.preferences.current().profile).isEqualTo(old.preferences.current().profile)
        val newSettings = new.preferences.current().settings
        assertThat(newSettings.copy(onboardingCompleted = true)).isEqualTo(old.preferences.current().settings)
        // The new phone's own first run is its own.
        assertThat(newSettings.onboardingCompleted).isFalse()
        val outing = new.database.sessionDao().allSessions().single()
        val plan = new.database.sessionDao().plans().single()
        assertThat(outing.copy(id = 0, planId = null))
            .isEqualTo(old.database.sessionDao().allSessions().single().copy(id = 0, planId = null))
        assertThat(outing.planId).isEqualTo(plan.id)
        assertThat(report).isEqualTo(ImportReport(3, 0, 0, addedOutings = 1, addedPlans = 1, preferences = true))
        // Another phone's counter is never brought over (ADR 0007).
        assertThat(new.database.trackingDao().trackerState()).isNull()
    }

    @Test
    fun `importing the same file again changes nothing`() = runTest {
        history()
        val file = read(exported())
        new.backups.import(file, withPreferences = true)
        val summaries = new.summaries()
        val minutes = new.minutes()

        val again = new.backups.import(file, withPreferences = true)

        assertThat(new.summaries()).isEqualTo(summaries)
        assertThat(new.minutes()).isEqualTo(minutes)
        assertThat(new.database.sessionDao().allSessions()).hasSize(1)
        assertThat(new.database.sessionDao().plans()).hasSize(1)
        assertThat(again).isEqualTo(ImportReport(0, 0, 3, addedOutings = 0, addedPlans = 0, preferences = true))
    }

    @Test
    fun `a new phone that counted today keeps its steps, and the file's are added`() = runTest {
        history()
        new.walk(minute(day + 2, 420, 60), minute(day + 2, 700, 110))

        val report = new.backups.import(read(exported()), withPreferences = false)

        val todayRow = new.summaries().single { it.localEpochDay == day + 2 }
        assertThat(todayRow.steps).isEqualTo(90 + 110)
        assertThat(todayRow.finalized).isFalse()
        assertThat(report.addedDays).isEqualTo(2)
        assertThat(report.mergedDays).isEqualTo(1)
        // Without the preferences, this phone's profile and goal stand.
        assertThat(new.preferences.current().settings.dailyGoalSteps).isEqualTo(8_000)
        assertThat(todayRow.goalSteps).isEqualTo(8_000)
    }

    @Test
    fun `the data rows count what an export would carry`() = runTest {
        history()

        assertThat(old.backups.contents.first()).isEqualTo(DataContents(days = 3, outings = 1))
    }

    @Test
    fun `each spreadsheet table has a row per day, minute or outing`() = runTest {
        history()

        fun rows(csv: String) = csv.trimEnd().lines().size - 1
        assertThat(rows(old.backups.csv(CsvTable.DAYS, UnitSystem.METRIC, ZoneOffset.UTC))).isEqualTo(3)
        assertThat(rows(old.backups.csv(CsvTable.MINUTES, UnitSystem.METRIC, ZoneOffset.UTC))).isEqualTo(6)
        assertThat(rows(old.backups.csv(CsvTable.OUTINGS, UnitSystem.METRIC, ZoneOffset.UTC))).isEqualTo(1)
    }
}
