package com.callbackdev.passo.core.data.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.domain.tracking.TrackingConstants
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.model.TrackerState
import com.callbackdev.passo.core.model.UserSettings
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

/** The transactional write of PLANNING.md §4.5 and the summary rules of §5, on a real Room database. */
@RunWith(AndroidJUnit4::class)
class TrackingRepositoryTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var database: PassoDatabase
    private lateinit var store: TestDataStore
    private lateinit var repository: TrackingRepository

    private val day = 20_720L
    private var today = day
    private val state =
        TrackerState(
            bootCount = 3,
            lastCounterValue = 1_234,
            lastSampleElapsedNanos = 99L,
            lastSampleWallMillis = 1_000L,
        )

    // Minutes of `day` and of the day after, as UTC minutes (only their order matters here).
    private val dayMinute = 100L
    private val nextDayMinute = 2_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = TestDataStore(folder.root)
        repository = TrackingRepository(database.trackingDao(), UserPreferencesDataSource(store.dataStore)) { today }
    }

    @After
    fun tearDown() {
        database.close()
        store.scope.cancel()
    }

    @Test
    fun `a batch writes steps, state and the day's summary together`() = runTest {
        repository.persist(
            LedgerBatch(
                increments = listOf(MinuteSteps(100, day, 40), MinuteSteps(101, day, 60)),
                state = state,
                diagnostics = listOf(DiagnosticsEvent(1_000L, DiagnosticsType.BOOT, "x")),
            ),
            nowWallMillis = 2_000L,
        )

        assertThat(repository.stepsOn(day)).isEqualTo(100)
        assertThat(repository.trackerState()).isEqualTo(state)
        val summary = summary(day)
        assertThat(summary?.steps).isEqualTo(100)
        assertThat(summary?.distanceMeters).isWithin(1e-9).of(100 * 0.70)
        assertThat(summary?.activeMinutes).isEqualTo(2)
        assertThat(summary?.briskMinutes).isEqualTo(0)
        assertThat(summary?.goalSteps).isEqualTo(UserSettings.DEFAULT_DAILY_GOAL_STEPS)
        assertThat(summary?.finalized).isFalse()
        assertThat(repository.diagnostics().map { it.type }).containsExactly(DiagnosticsType.BOOT)
    }

    @Test
    fun `a minute written twice adds up`() = runTest {
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day, 40)), state, emptyList()), 0L)
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day, 25)), state, emptyList()), 0L)

        assertThat(database.trackingDao().minutesOn(day).single().steps).isEqualTo(65)
        assertThat(summary(day)?.steps).isEqualTo(65)
        assertThat(summary(day)?.activeMinutes).isEqualTo(1)
    }

    @Test
    fun `a recorded minute keeps its day after a time-zone change`() = runTest {
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day, 40)), state, emptyList()), 0L)
        // Same UTC minute, now read in a zone where it falls on the next day.
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day + 1, 10)), state, emptyList()), 0L)

        assertThat(repository.stepsOn(day)).isEqualTo(50)
        assertThat(repository.stepsOn(day + 1)).isEqualTo(0)
        assertThat(summary(day + 1)).isNull()
    }

    @Test
    fun `steps across midnight update both days' summaries`() = runTest {
        repository.persist(
            LedgerBatch(listOf(MinuteSteps(100, day, 30), MinuteSteps(101, day + 1, 20)), state, emptyList()),
            0L,
        )

        assertThat(summary(day)?.steps).isEqualTo(30)
        assertThat(summary(day + 1)?.steps).isEqualTo(20)
    }

    @Test
    fun `a batch without a state leaves the stored one alone`() = runTest {
        repository.persist(LedgerBatch(emptyList(), state, emptyList()), 0L)
        repository.persist(
            LedgerBatch(emptyList(), null, listOf(DiagnosticsEvent(0L, DiagnosticsType.SERVICE_START, ""))),
            0L,
        )

        assertThat(repository.trackerState()).isEqualTo(state)
    }

    @Test
    fun `the log keeps only the latest rows`() = runTest {
        val size = TrackingConstants.DIAGNOSTICS_LOG_SIZE
        val events = (1..size + 20).map { DiagnosticsEvent(it.toLong(), DiagnosticsType.ANOMALY, "$it") }
        events.chunked(50).forEach { repository.persist(LedgerBatch(emptyList(), null, it), 0L) }

        val kept = repository.diagnostics()
        assertThat(kept).hasSize(size)
        assertThat(kept.first().detail).isEqualTo("21")
        assertThat(kept.last().detail).isEqualTo("${size + 20}")
    }

    // --- Profile, goal and the frozen past (PLANNING.md §5, Phase 2) ---------------------------

    @Test
    fun `today's summary is computed with the reader's profile and goal`() = runTest {
        val profile = Profile(heightMeters = 1.80, weightKg = 90.0)
        repository.changeProfile { profile }
        repository.changeSettings { it.copy(dailyGoalSteps = 10_000) }

        write(dayMinute, day, 120)

        assertThat(summary(day)).isEqualTo(DaySummaries.summarize(day, listOf(120), profile, 10_000, finalized = false))
    }

    @Test
    fun `the first write after midnight finalizes the day that is over`() = runTest {
        write(dayMinute, day, 100)
        today = day + 1

        write(nextDayMinute, day + 1, 50)

        assertThat(summary(day)?.finalized).isTrue()
        assertThat(summary(day + 1)?.finalized).isFalse()
    }

    @Test
    fun `changing the weight updates today only`() = runTest {
        write(dayMinute, day, 100)
        today = day + 1
        write(nextDayMinute, day + 1, 100)
        val yesterday = summary(day)

        repository.changeProfile { it.copy(weightKg = 140.0) }

        assertThat(summary(day)).isEqualTo(yesterday)
        assertThat(summary(day)?.activeKcal).isWithin(1e-9).of(kcal(100, Profile()))
        assertThat(summary(day + 1)?.activeKcal).isWithin(1e-9).of(kcal(100, Profile()) * 2)
    }

    @Test
    fun `a day still open when the weight changes is frozen with the old weight`() = runTest {
        write(dayMinute, day, 100)
        // Midnight passes with no write: the day is over but not finalized yet.
        today = day + 1

        repository.changeProfile { it.copy(weightKg = 140.0) }
        write(nextDayMinute, day + 1, 100)

        assertThat(summary(day)?.finalized).isTrue()
        assertThat(summary(day)?.activeKcal).isWithin(1e-9).of(kcal(100, Profile()))
        assertThat(summary(day + 1)?.activeKcal).isWithin(1e-9).of(kcal(100, Profile(weightKg = 140.0)))
    }

    @Test
    fun `apply to past data recomputes every day with the current profile`() = runTest {
        write(dayMinute, day, 100)
        today = day + 1
        write(nextDayMinute, day + 1, 100)
        repository.changeProfile { it.copy(weightKg = 140.0) }

        repository.applyProfileToPastDays()

        val heavyKcal = kcal(100, Profile(weightKg = 140.0))
        assertThat(summary(day)?.activeKcal).isWithin(1e-9).of(heavyKcal)
        assertThat(summary(day)?.finalized).isTrue()
        assertThat(summary(day + 1)?.activeKcal).isWithin(1e-9).of(heavyKcal)
        assertThat(summary(day + 1)?.finalized).isFalse()
    }

    @Test
    fun `a new goal is today's goal, and past days keep theirs`() = runTest {
        write(dayMinute, day, 100)
        today = day + 1
        write(nextDayMinute, day + 1, 100)

        repository.changeSettings { it.copy(dailyGoalSteps = 12_000) }

        assertThat(summary(day)?.goalSteps).isEqualTo(UserSettings.DEFAULT_DAILY_GOAL_STEPS)
        assertThat(summary(day + 1)?.goalSteps).isEqualTo(12_000)
    }

    @Test
    fun `a day still open when the goal changes keeps the old goal`() = runTest {
        write(dayMinute, day, 100)
        today = day + 1

        repository.changeSettings { it.copy(dailyGoalSteps = 12_000) }
        write(nextDayMinute, day + 1, 100)

        assertThat(summary(day)?.goalSteps).isEqualTo(UserSettings.DEFAULT_DAILY_GOAL_STEPS)
        assertThat(summary(day)?.finalized).isTrue()
        assertThat(summary(day + 1)?.goalSteps).isEqualTo(12_000)
    }

    @Test
    fun `a setting that is not the goal leaves the summaries alone`() = runTest {
        write(dayMinute, day, 100)
        val before = summary(day)

        repository.changeSettings { it.copy(theme = ThemeMode.DARK) }

        assertThat(summary(day)).isEqualTo(before)
    }

    @Test
    fun `late steps reach a finalized day as their own share`() = runTest {
        write(dayMinute, day, 100)
        today = day + 1
        write(nextDayMinute, day + 1, 10)
        repository.changeProfile { it.copy(weightKg = 140.0) }

        // A batch delivered after midnight: two minutes of the day that is over.
        write(dayMinute, day, 20)
        write(dayMinute + 1, day, 100)

        // The minute of 100 keeps its default-weight share; what the late steps add (100 -> 120,
        // and a new minute of 100) is priced at the new weight.
        val heavy = Profile(weightKg = 140.0)
        val lateShare = (kcal(120, heavy) - kcal(100, heavy)) + kcal(100, heavy)
        val late = summary(day)
        assertThat(late?.steps).isEqualTo(220)
        assertThat(late?.activeKcal).isWithin(1e-9).of(kcal(100, Profile()) + lateShare)
        assertThat(late?.activeMinutes).isEqualTo(2)
        assertThat(late?.briskMinutes).isEqualTo(2)
        assertThat(late?.finalized).isTrue()
    }

    @Test
    fun `days recorded before the estimates existed get them at the next write`() = runTest {
        // What Phase 1 left behind: steps, a provisional goal, zeros, nothing finalized.
        repository.persist(LedgerBatch(listOf(MinuteSteps(dayMinute, day, 100)), state, emptyList()), 0L)
        database.openHelper.writableDatabase.execSQL(
            "UPDATE daily_summary SET distanceMeters = 0, activeKcal = 0, activeMinutes = 0, briskMinutes = 0",
        )
        today = day + 3

        repository.persist(LedgerBatch(emptyList(), state, emptyList()), 0L)

        assertThat(summary(day)).isEqualTo(DaySummaries.summarize(day, listOf(100), Profile(), 8_000, finalized = true))
    }

    @Test
    fun `the summary flow follows the writes`() = runTest {
        write(dayMinute, day, 100)

        assertThat(repository.observeSummary(day).first()?.steps).isEqualTo(100)
        assertThat(repository.observeSummaries(day - 7, day).first().map { it.localEpochDay }).containsExactly(day)
    }

    private suspend fun write(minute: Long, localEpochDay: Long, steps: Int) {
        repository.persist(LedgerBatch(listOf(MinuteSteps(minute, localEpochDay, steps)), state, emptyList()), 0L)
    }

    private fun kcal(minuteSteps: Int, profile: Profile): Double =
        MetricsCalculator.day(listOf(minuteSteps), profile).activeKcal

    private suspend fun summary(localEpochDay: Long): DailySummary? = repository.observeSummary(localEpochDay).first()
}
