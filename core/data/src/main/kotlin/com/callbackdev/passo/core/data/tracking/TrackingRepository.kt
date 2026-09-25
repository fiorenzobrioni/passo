package com.callbackdev.passo.core.data.tracking

import com.callbackdev.passo.core.data.db.DiagnosticsEventEntity
import com.callbackdev.passo.core.data.db.MinuteStepsEntity
import com.callbackdev.passo.core.data.db.TrackerStateEntity
import com.callbackdev.passo.core.data.db.TrackingDao
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.time.TodaySource
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.TypicalDay
import com.callbackdev.passo.core.domain.today.TypicalDayCalculator
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.tracking.LedgerBatch
import com.callbackdev.passo.core.domain.tracking.TrackingConstants
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.TrackerState
import com.callbackdev.passo.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The step data as the tracking engine and the screens see it, and the one writer of the daily
 * summaries (PLANNING.md §5).
 *
 * The profile and the goal are changed through here, not directly in DataStore, because a
 * change has to reach the days in the right order: the days already over are frozen with the
 * old values first, then the new values are stored, then today is recomputed with them. One
 * lock covers that sequence and the service's writes, so a write never reads a profile that
 * is halfway through changing.
 */
@Singleton
class TrackingRepository
@Inject
constructor(
    private val dao: TrackingDao,
    private val preferences: UserPreferencesDataSource,
    private val todaySource: TodaySource,
) {
    private val summaryLock = Mutex()

    suspend fun trackerState(): TrackerState? = dao.trackerState()?.let {
        TrackerState(
            bootCount = it.bootCount,
            lastCounterValue = it.lastCounterValue,
            lastSampleElapsedNanos = it.lastSampleElapsedNanos,
            lastSampleWallMillis = it.lastSampleWallMillis,
        )
    }

    /**
     * Writes a batch from the ledger in one transaction (PLANNING.md §4.5), with the summaries
     * of the days it touches recomputed for the current profile and goal.
     */
    suspend fun persist(batch: LedgerBatch, nowWallMillis: Long) = summaryLock.withLock {
        val current = preferences.current()
        dao.writeBatch(
            increments = batch.increments.map { MinuteStepsEntity(it.epochMinute, it.localEpochDay, it.steps) },
            state = batch.state?.let {
                TrackerStateEntity(
                    bootCount = it.bootCount,
                    lastCounterValue = it.lastCounterValue,
                    lastSampleElapsedNanos = it.lastSampleElapsedNanos,
                    lastSampleWallMillis = it.lastSampleWallMillis,
                    updatedAtMillis = nowWallMillis,
                )
            },
            diagnostics = batch.diagnostics.map {
                DiagnosticsEventEntity(wallMillis = it.wallMillis, type = it.type.name, detail = it.detail)
            },
            profile = current.profile,
            goalSteps = current.settings.dailyGoalSteps,
            today = todaySource.epochDay(),
            diagnosticsKept = TrackingConstants.DIAGNOSTICS_LOG_SIZE,
        )
    }

    /**
     * Changes the profile. Today's estimates follow it; the days already over keep the ones
     * they had (VISION.md: past days are frozen) unless [applyProfileToPastDays] is asked for.
     */
    suspend fun changeProfile(transform: (Profile) -> Profile): Profile = summaryLock.withLock {
        val today = todaySource.epochDay()
        val before = preferences.current()
        dao.finalizeDaysBefore(today, before.profile)
        val profile = preferences.updateProfile(transform)
        if (profile != before.profile) dao.refreshOpenDays(today, profile, before.settings.dailyGoalSteps)
        profile
    }

    /**
     * Changes the settings. A new daily goal becomes today's goal; every day already over keeps
     * the goal it had, so a streak or a record never changes because the goal did.
     */
    suspend fun changeSettings(transform: (UserSettings) -> UserSettings): UserSettings = summaryLock.withLock {
        val today = todaySource.epochDay()
        val before = preferences.current()
        dao.finalizeDaysBefore(today, before.profile)
        val settings = preferences.updateSettings(transform)
        if (settings.dailyGoalSteps != before.settings.dailyGoalSteps) {
            dao.refreshOpenDays(today, before.profile, settings.dailyGoalSteps)
        }
        settings
    }

    /**
     * Forgets where the counter stood, so the next sample is a new baseline, as at the first
     * run. Called when the reader resumes a pause: the hardware counter kept counting while
     * the service was stopped, and without this the first sample after the pause would add
     * every step of it. A pause is the reader saying "don't count these" (PLANNING.md §15).
     * Only while the service is stopped; under the lock, so it never lands between a write's
     * steps and its state.
     */
    suspend fun forgetBaseline() = summaryLock.withLock { dao.deleteTrackerState() }

    /** "Apply profile to past data": every recorded day recomputed with the current profile. */
    suspend fun applyProfileToPastDays() = summaryLock.withLock {
        dao.recomputeAll(todaySource.epochDay(), preferences.current().profile)
    }

    suspend fun stepsOn(localEpochDay: Long): Int = dao.stepsOn(localEpochDay)

    fun observeStepsOn(localEpochDay: Long): Flow<Int> = dao.observeStepsOn(localEpochDay)

    /** The minutes of one day, as they are written; oldest first. */
    fun observeMinutesOn(localEpochDay: Long): Flow<List<MinuteSteps>> = dao.observeMinutesOn(localEpochDay)
        .map { rows -> rows.map { MinuteSteps(it.epochMinute, it.localEpochDay, it.steps) } }

    /** The minutes of several days, grouped by day; a day without steps is absent. */
    suspend fun minutesOn(days: List<Long>): Map<Long, List<MinuteSteps>> = dao.minutesOnDays(days)
        .map { MinuteSteps(it.epochMinute, it.localEpochDay, it.steps) }
        .groupBy { it.localEpochDay }

    /**
     * The usual day for [today]: the mean of the same weekday over the past weeks
     * ([TypicalDayCalculator], PLANNING.md §6.2), with each minute placed in [zone]. Null until
     * enough of those days are recorded.
     */
    suspend fun typicalDay(today: Long, zone: ZoneId): TypicalDay? {
        val candidates = TypicalDayCalculator.candidateDays(today)
        val byDay = minutesOn(candidates)
        return TypicalDayCalculator.typical(
            candidates.map { day ->
                byDay[day].orEmpty().map { DayMinute(minuteOfDay(it.epochMinute, it.localEpochDay, zone), it.steps) }
            },
        )
    }

    /** The first day with steps recorded, null before the first: the day tracking began. */
    fun observeFirstRecordedDay(): Flow<Long?> = dao.observeFirstRecordedDay()

    /** One day's summary; null for a day with no steps recorded. */
    fun observeSummary(localEpochDay: Long): Flow<DailySummary?> =
        dao.observeSummary(localEpochDay).map { it?.toModel() }

    /** The summaries from [fromDay] to [toDay] included, oldest first; a day without steps is absent. */
    fun observeSummaries(fromDay: Long, toDay: Long): Flow<List<DailySummary>> =
        dao.observeSummaries(fromDay, toDay).map { rows -> rows.map { it.toModel() } }

    /** The tracking log, oldest first. Unknown types (from a newer build) are skipped. */
    suspend fun diagnostics(): List<DiagnosticsEvent> = dao.diagnostics().mapNotNull { row ->
        DiagnosticsType.entries.firstOrNull {
            it.name == row.type
        }?.let { DiagnosticsEvent(row.wallMillis, it, row.detail) }
    }
}
