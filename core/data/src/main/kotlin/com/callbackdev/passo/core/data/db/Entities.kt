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

/**
 * An outing the reader keeps, ready to start (PLANNING.md §11 Phase 10). Enums are stored by
 * name; `milestones` is a bit set of `SessionMilestone` ordinals.
 */
@Entity(tableName = "session_plan")
data class SessionPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?,
    val goalKind: String,
    val goalValue: Int,
    val intensity: String,
    val milestones: Int,
    val vibrate: Boolean,
    val position: Int,
    val lastUsedAtMillis: Long?,
)

/**
 * One outing: its goal as it was when it started, and what it added up to, measured from the
 * steps. Written with the step batches while it is under way, in the same transaction as the
 * counter state, so the two never disagree after a crash (PLANNING.md §4.5).
 */
@Entity(tableName = "session", indices = [Index("localEpochDay"), Index("state")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long?,
    val name: String?,
    val goalKind: String,
    val goalValue: Int,
    val restOfDay: Boolean,
    val intensity: String,
    val milestones: Int,
    val vibrate: Boolean,
    val localEpochDay: Long,
    val startedAtMillis: Long,
    val state: String,
    val endedAtMillis: Long?,
    val endReason: String?,
    val steps: Int,
    val movingMillis: Long,
    val zoneMillis: Long,
    val distanceMeters: Double,
    val activeKcal: Double,
    val lastStepAtMillis: Long,
    val lastEventAtMillis: Long,
    val pausedAtMillis: Long?,
    val reachedAtMillis: Long?,
    val toldMilestones: Int,
)
