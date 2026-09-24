package com.callbackdev.passo.core.data.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.domain.tracking.DEFAULT_GOAL_STEPS
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.domain.tracking.TrackingConstants
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.TrackerState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The transactional write of PLANNING.md §4.5, on a real (in-memory) Room database. */
@RunWith(AndroidJUnit4::class)
class TrackingRepositoryTest {
    private lateinit var database: PassoDatabase
    private lateinit var repository: TrackingRepository

    private val day = 20_720L
    private val state =
        TrackerState(
            bootCount = 3,
            lastCounterValue = 1_234,
            lastSampleElapsedNanos = 99L,
            lastSampleWallMillis = 1_000L,
        )

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), PassoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TrackingRepository(database.trackingDao())
    }

    @After
    fun tearDown() {
        database.close()
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
        val summary = database.trackingDao().summary(day)
        assertThat(summary?.steps).isEqualTo(100)
        assertThat(summary?.goalSteps).isEqualTo(DEFAULT_GOAL_STEPS)
        assertThat(summary?.finalized).isFalse()
        assertThat(repository.diagnostics().map { it.type }).containsExactly(DiagnosticsType.BOOT)
    }

    @Test
    fun `a minute written twice adds up`() = runTest {
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day, 40)), state, emptyList()), 0L)
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day, 25)), state, emptyList()), 0L)

        assertThat(database.trackingDao().minutesOn(day).single().steps).isEqualTo(65)
        assertThat(database.trackingDao().summary(day)?.steps).isEqualTo(65)
    }

    @Test
    fun `a recorded minute keeps its day after a time-zone change`() = runTest {
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day, 40)), state, emptyList()), 0L)
        // Same UTC minute, now read in a zone where it falls on the next day.
        repository.persist(LedgerBatch(listOf(MinuteSteps(100, day + 1, 10)), state, emptyList()), 0L)

        assertThat(repository.stepsOn(day)).isEqualTo(50)
        assertThat(repository.stepsOn(day + 1)).isEqualTo(0)
    }

    @Test
    fun `steps across midnight update both days' summaries`() = runTest {
        repository.persist(
            LedgerBatch(listOf(MinuteSteps(100, day, 30), MinuteSteps(101, day + 1, 20)), state, emptyList()),
            0L,
        )

        assertThat(database.trackingDao().summary(day)?.steps).isEqualTo(30)
        assertThat(database.trackingDao().summary(day + 1)?.steps).isEqualTo(20)
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
}
