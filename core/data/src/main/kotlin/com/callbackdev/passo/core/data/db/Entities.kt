package com.callbackdev.passo.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The tracker's single row (id 0): where the counter stood at the last written sample
 * (PLANNING.md §5). Written in the same transaction as the steps it accounts for.
 *
 * `lastSampleElapsedNanos` is not in the first draft of §5: gaps inside a boot session are
 * measured on the monotonic clock, because the wall clock can be moved by the user (§15).
 */
@Entity(tableName = "tracker_state")
data class TrackerStateEntity(
    @PrimaryKey val id: Int = SINGLE_ROW_ID,
    val bootCount: Int,
    val lastCounterValue: Long,
    val lastSampleElapsedNanos: Long,
    val lastSampleWallMillis: Long,
    val updatedAtMillis: Long,
) {
    companion object {
        const val SINGLE_ROW_ID = 0
    }
}

/** Steps per UTC minute, with the local day fixed when the row was first written. */
@Entity(tableName = "minute_steps", indices = [Index("localEpochDay")])
data class MinuteStepsEntity(@PrimaryKey val epochMinute: Long, val localEpochDay: Long, val steps: Int)

/**
 * One day's totals and estimates, recomputed from its minutes while the day is open and frozen
 * once it is over (`finalized`). Phase 1 wrote only `steps` and a provisional goal and
 * finalized nothing, so its days get their estimates at the first write after Phase 2 lands.
 */
@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val localEpochDay: Long,
    val steps: Int,
    val distanceMeters: Double,
    val activeKcal: Double,
    val activeMinutes: Int,
    val briskMinutes: Int,
    val goalSteps: Int,
    val finalized: Boolean,
)

/** The tracking log, a ring buffer of the latest rows (PLANNING.md §5). */
@Entity(tableName = "diagnostics_event")
data class DiagnosticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wallMillis: Long,
    val type: String,
    val detail: String,
)
