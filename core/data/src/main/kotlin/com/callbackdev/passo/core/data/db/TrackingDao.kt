package com.callbackdev.passo.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** The tracking engine's reads and its one transactional write (PLANNING.md §4.5). */
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

    @Query("SELECT * FROM daily_summary WHERE localEpochDay = :localEpochDay")
    abstract suspend fun summary(localEpochDay: Long): DailySummaryEntity?

    @Query("SELECT * FROM diagnostics_event ORDER BY id")
    abstract suspend fun diagnostics(): List<DiagnosticsEventEntity>

    /**
     * Writes one batch atomically: minute increments, the counter state they lead to, the
     * summaries of the days they touch, the log lines. Either all of it lands or none of it
     * does, which is what keeps "stored steps + stored counter" consistent after any crash.
     */
    @Transaction
    open suspend fun writeBatch(
        increments: List<MinuteStepsEntity>,
        state: TrackerStateEntity?,
        diagnostics: List<DiagnosticsEventEntity>,
        goalSteps: Int,
        diagnosticsKept: Int,
    ) {
        for (increment in increments) {
            // Adds to an existing minute, keeping the local day it was first written with: a
            // time-zone change never moves steps already recorded.
            if (addToMinute(increment.epochMinute, increment.steps) == 0) insertMinute(increment)
        }
        val touchedDays = increments.map { it.epochMinute }.chunked(MAX_BIND_ARGS).flatMap { daysOf(it) }.toSet()
        for (day in touchedDays) {
            val steps = stepsOn(day)
            val existing = summary(day)
            upsertSummary(
                existing?.copy(steps = steps) ?: DailySummaryEntity(
                    localEpochDay = day,
                    steps = steps,
                    distanceMeters = 0.0,
                    activeKcal = 0.0,
                    activeMinutes = 0,
                    briskMinutes = 0,
                    goalSteps = goalSteps,
                    finalized = false,
                ),
            )
        }
        if (state != null) upsertTrackerState(state)
        if (diagnostics.isNotEmpty()) {
            insertDiagnostics(diagnostics)
            trimDiagnostics(diagnosticsKept)
        }
    }

    @Query("UPDATE minute_steps SET steps = steps + :steps WHERE epochMinute = :epochMinute")
    protected abstract suspend fun addToMinute(epochMinute: Long, steps: Int): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertMinute(minute: MinuteStepsEntity)

    @Query("SELECT DISTINCT localEpochDay FROM minute_steps WHERE epochMinute IN (:epochMinutes)")
    protected abstract suspend fun daysOf(epochMinutes: List<Long>): List<Long>

    @Upsert
    protected abstract suspend fun upsertSummary(summary: DailySummaryEntity)

    @Upsert
    protected abstract suspend fun upsertTrackerState(state: TrackerStateEntity)

    @Insert
    protected abstract suspend fun insertDiagnostics(events: List<DiagnosticsEventEntity>)

    @Query(
        "DELETE FROM diagnostics_event WHERE id NOT IN (SELECT id FROM diagnostics_event ORDER BY id DESC LIMIT :kept)",
    )
    protected abstract suspend fun trimDiagnostics(kept: Int)

    private companion object {
        // SQLite's default limit on bound parameters is 999; stay well under it.
        const val MAX_BIND_ARGS = 500
    }
}
