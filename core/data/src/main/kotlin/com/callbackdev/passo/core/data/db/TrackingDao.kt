package com.callbackdev.passo.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.domain.metrics.MinuteChange
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.Profile
import kotlinx.coroutines.flow.Flow

/**
 * The step data's reads and its transactional writes (PLANNING.md §4.5, §5).
 *
 * The summary rules, applied by every write: a day is open until it is over, and an open day is
 * recomputed whole from its minutes with the current profile. The first write that finds a day
 * over finalizes it, with the profile and goal in effect until then. A finalized day is never
 * recomputed except by [recomputeAll] (the reader's "Apply profile to past data"): steps that
 * reach it late only add their own share (`DaySummaries.withLateSteps`).
 */
@Dao
abstract class TrackingDao {
    @Query("SELECT * FROM tracker_state WHERE id = 0")
    abstract suspend fun trackerState(): TrackerStateEntity?

    @Query("SELECT COALESCE(SUM(steps), 0) FROM minute_steps WHERE localEpochDay = :localEpochDay")
    abstract suspend fun stepsOn(localEpochDay: Long): Int

    @Query("SELECT COALESCE(SUM(steps), 0) FROM minute_steps WHERE localEpochDay = :localEpochDay")
    abstract fun observeStepsOn(localEpochDay: Long): Flow<Int>

    @Query("SELECT * FROM minute_steps WHERE localEpochDay = :localEpochDay ORDER BY epochMinute")
    abstract suspend fun minutesOn(localEpochDay: Long): List<MinuteStepsEntity>

    @Query("SELECT * FROM minute_steps WHERE localEpochDay = :localEpochDay ORDER BY epochMinute")
    abstract fun observeMinutesOn(localEpochDay: Long): Flow<List<MinuteStepsEntity>>

    @Query("SELECT * FROM minute_steps WHERE localEpochDay IN (:days) ORDER BY epochMinute")
    abstract suspend fun minutesOnDays(days: List<Long>): List<MinuteStepsEntity>

    /** The first day with steps recorded; null before the first. */
    @Query("SELECT MIN(localEpochDay) FROM daily_summary")
    abstract fun observeFirstRecordedDay(): Flow<Long?>

    @Query("SELECT * FROM daily_summary WHERE localEpochDay = :localEpochDay")
    abstract suspend fun summary(localEpochDay: Long): DailySummaryEntity?

    @Query("SELECT * FROM daily_summary WHERE localEpochDay = :localEpochDay")
    abstract fun observeSummary(localEpochDay: Long): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summary WHERE localEpochDay BETWEEN :fromDay AND :toDay ORDER BY localEpochDay")
    abstract fun observeSummaries(fromDay: Long, toDay: Long): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summary ORDER BY localEpochDay")
    abstract fun observeAllSummaries(): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM diagnostics_event ORDER BY id")
    abstract suspend fun diagnostics(): List<DiagnosticsEventEntity>

    /**
     * Writes one batch atomically: minute increments, the counter state they lead to, the
     * summaries of the days they touch, the log lines, and the outing under way as those steps
     * leave it. Either all of it lands or none of it does, which is what keeps "stored steps +
     * stored counter (+ stored outing)" consistent after any crash. Days that are over by
     * [today] are finalized in the same transaction.
     */
    @Transaction
    open suspend fun writeBatch(
        increments: List<MinuteStepsEntity>,
        state: TrackerStateEntity?,
        diagnostics: List<DiagnosticsEventEntity>,
        profile: Profile,
        goalSteps: Int,
        today: Long,
        diagnosticsKept: Int,
        session: SessionEntity? = null,
    ) {
        val changes = linkedMapOf<Long, MutableList<MinuteChange>>()
        for (increment in increments) {
            // Adds to an existing minute, keeping the local day it was first written with: a
            // time-zone change never moves steps already recorded.
            val existing = minute(increment.epochMinute)
            val day = existing?.localEpochDay ?: increment.localEpochDay
            val before = existing?.steps ?: 0
            if (existing == null) insertMinute(increment) else addToMinute(increment.epochMinute, increment.steps)
            changes.getOrPut(day) { mutableListOf() } += MinuteChange(before, before + increment.steps)
        }
        for ((day, dayChanges) in changes) {
            val existing = summary(day)
            val updated = if (existing?.finalized == true) {
                DaySummaries.withLateSteps(existing.toModel(), dayChanges, profile)
            } else {
                summarizeOpen(day, existing, profile, goalSteps, today)
            }
            upsertSummary(updated.toEntity())
        }
        finalizeDaysBefore(today, profile)
        if (state != null) upsertTrackerState(state)
        if (diagnostics.isNotEmpty()) {
            insertDiagnostics(diagnostics)
            trimDiagnostics(diagnosticsKept)
        }
        if (session != null) upsertSession(session)
    }

    /**
     * Finalizes every open day before [today] with [profile], the one in effect until now, and
     * the goal each day was written with. Run before the profile or the goal changes, so a
     * change never reaches a day that is already over.
     */
    @Transaction
    open suspend fun finalizeDaysBefore(today: Long, profile: Profile) {
        for (open in openSummariesBefore(today)) {
            upsertSummary(
                summarizeFromMinutes(open.localEpochDay, profile, open.goalSteps, finalized = true).toEntity(),
            )
        }
    }

    /**
     * Recomputes the days still open (today, and a later one a time-zone change may have left)
     * with [profile] and [goalSteps], after either changed. Days already over are finalized
     * first, with the same profile: call [finalizeDaysBefore] with the old one beforehand.
     */
    @Transaction
    open suspend fun refreshOpenDays(today: Long, profile: Profile, goalSteps: Int) {
        finalizeDaysBefore(today, profile)
        for (open in openSummariesFrom(today)) {
            upsertSummary(summarizeFromMinutes(open.localEpochDay, profile, goalSteps, finalized = false).toEntity())
        }
    }

    /**
     * "Apply profile to past data": every day recomputed from its minutes with [profile],
     * keeping the goal it had. The one path that rewrites a finalized day, on the reader's
     * explicit request.
     */
    @Transaction
    open suspend fun recomputeAll(today: Long, profile: Profile) {
        for (day in allSummaries()) {
            val finalized = day.finalized || day.localEpochDay < today
            upsertSummary(summarizeFromMinutes(day.localEpochDay, profile, day.goalSteps, finalized).toEntity())
        }
    }

    private suspend fun summarizeOpen(
        day: Long,
        existing: DailySummaryEntity?,
        profile: Profile,
        goalSteps: Int,
        today: Long,
    ): DailySummary {
        // An open day that is already over keeps the goal it was written with; it is being
        // finalized now. A day not over yet follows the current goal.
        val over = day < today
        val goal = if (over) existing?.goalSteps ?: goalSteps else goalSteps
        return summarizeFromMinutes(day, profile, goal, finalized = over)
    }

    private suspend fun summarizeFromMinutes(day: Long, profile: Profile, goalSteps: Int, finalized: Boolean) =
        DaySummaries.summarize(day, minuteCountsOn(day), profile, goalSteps, finalized)

    @Query("DELETE FROM tracker_state")
    abstract suspend fun deleteTrackerState()

    @Query("SELECT * FROM minute_steps WHERE epochMinute = :epochMinute")
    protected abstract suspend fun minute(epochMinute: Long): MinuteStepsEntity?

    @Query("SELECT steps FROM minute_steps WHERE localEpochDay = :localEpochDay")
    protected abstract suspend fun minuteCountsOn(localEpochDay: Long): List<Int>

    @Query("SELECT * FROM daily_summary WHERE finalized = 0 AND localEpochDay < :today")
    protected abstract suspend fun openSummariesBefore(today: Long): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summary WHERE finalized = 0 AND localEpochDay >= :today")
    protected abstract suspend fun openSummariesFrom(today: Long): List<DailySummaryEntity>

    @Query("SELECT * FROM daily_summary ORDER BY localEpochDay")
    protected abstract suspend fun allSummaries(): List<DailySummaryEntity>

    @Query("UPDATE minute_steps SET steps = steps + :steps WHERE epochMinute = :epochMinute")
    protected abstract suspend fun addToMinute(epochMinute: Long, steps: Int): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertMinute(minute: MinuteStepsEntity)

    @Upsert
    protected abstract suspend fun upsertSummary(summary: DailySummaryEntity)

    @Upsert
    protected abstract suspend fun upsertSession(session: SessionEntity)

    @Upsert
    protected abstract suspend fun upsertTrackerState(state: TrackerStateEntity)

    @Insert
    protected abstract suspend fun insertDiagnostics(events: List<DiagnosticsEventEntity>)

    @Query(
        "DELETE FROM diagnostics_event WHERE id NOT IN (SELECT id FROM diagnostics_event ORDER BY id DESC LIMIT :kept)",
    )
    protected abstract suspend fun trimDiagnostics(kept: Int)
}
