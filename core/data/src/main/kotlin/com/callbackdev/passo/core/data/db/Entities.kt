package com.callbackdev.passo.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

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
    @ColumnInfo(defaultValue = "OFF") val voice: String = "OFF",
    @ColumnInfo(defaultValue = "3") val slowMinutes: Int = 3,
    @ColumnInfo(defaultValue = "3") val fastMinutes: Int = 3,
    @ColumnInfo(defaultValue = "5") val sets: Int = 5,
    @ColumnInfo(defaultValue = "0") val gps: Boolean = false,
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
    @ColumnInfo(defaultValue = "OFF") val voice: String = "OFF",
    @ColumnInfo(defaultValue = "NULL") val walk: String? = null,
    @ColumnInfo(defaultValue = "0") val walkFromMeters: Int = 0,
    @ColumnInfo(defaultValue = "0") val slowMinutes: Int = 0,
    @ColumnInfo(defaultValue = "0") val fastMinutes: Int = 0,
    @ColumnInfo(defaultValue = "0") val sets: Int = 0,
    @ColumnInfo(defaultValue = "0") val gps: Boolean = false,
    @ColumnInfo(defaultValue = "NULL") val stepsMeters: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val measuredShare: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val capAtMillis: Long? = null,
    @ColumnInfo(defaultValue = "NULL") val capAskedForMillis: Long? = null,
    @ColumnInfo(defaultValue = "NULL") val ascentMeters: Double? = null,
    @ColumnInfo(defaultValue = "NULL") val descentMeters: Double? = null,
)

/**
 * What a clean stretch of an outing with GPS said of the reader's step (Phase 15, for Phase 17 to
 * learn from; docs/adr/0016-location.md decision 12): measured from end to end at a steady
 * [cadence], so many [steps] over so many [meters]. Numbers, never places: this table is in
 * `passo.db`, which Android's backup carries, and the route it came from is not. Deleted with
 * its outing.
 */
@Entity(tableName = "step_length_sample", indices = [Index("sessionId")])
data class StepLengthSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val cadence: Int,
    val steps: Int,
    val meters: Double,
)

/** A sample with its outing's start, as the step that learns reads it. */
data class OutingSampleRow(
    val sessionId: Long,
    val startedAtMillis: Long,
    val cadence: Int,
    val steps: Int,
    val meters: Double,
)

/** How many minutes had [steps] steps. */
data class StepCount(val steps: Int, val minutes: Int)

/** An outing's id with its start, the pair a route is matched to its outing by. */
data class SessionStart(val id: Long, val startedAtMillis: Long)

/**
 * One interval of an interval outing as it was walked (Phase 13): `intervalIndex` from 0, slow
 * when even. Written with the outing, in the same transaction; deleted with it.
 */
@Entity(tableName = "session_interval", primaryKeys = ["sessionId", "intervalIndex"])
data class SessionIntervalEntity(
    val sessionId: Long,
    val intervalIndex: Int,
    val fast: Boolean,
    val steps: Int,
    val movingMillis: Long,
    val zoneMillis: Long,
)

/**
 * A hundred metres of an outing measured with GPS as it was walked (Phase 16): `legIndex` from
 * 0, the last one under way or left short. What the outing's kilometres or miles are cut from.
 * Numbers, never places: written with the outing, deleted with it, kept when its route goes.
 */
@Entity(tableName = "session_leg", primaryKeys = ["sessionId", "legIndex"])
data class SessionLegEntity(
    val sessionId: Long,
    val legIndex: Int,
    val meters: Double,
    val movingMillis: Long,
    val steps: Double,
    val endedAtMillis: Long,
    val ascentMeters: Double,
    val descentMeters: Double,
)

/**
 * A pause of an outing, from the reader's Pause to their Resume (PLANNING.md §11 Phase 21):
 * `pauseIndex` from 0, in order. The pause under way is the outing's own `pausedAtMillis`, not a
 * row: only a pause that ended inside its outing is kept. A row is a touch of the reader's, never
 * one Passo makes by itself, so the table grows with the outings kept and far slower than
 * `session_leg`; written with the outing, deleted with it.
 */
@Entity(tableName = "session_pause", primaryKeys = ["sessionId", "pauseIndex"])
data class SessionPauseEntity(val sessionId: Long, val pauseIndex: Int, val fromMillis: Long, val toMillis: Long)

/** An outing with its intervals, its legs and its pauses, read in one go. */
data class SessionRow(
    @Embedded val session: SessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val intervals: List<SessionIntervalEntity>,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val legs: List<SessionLegEntity>,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val pauses: List<SessionPauseEntity> = emptyList(),
)

/**
 * A way the reader started (PLANNING.md §11 Phase 11). Enums by name. No distance: what was
 * walked is the days' own estimates from [startEpochDay], read with them.
 */
@Entity(tableName = "way_journey", indices = [Index("state")])
data class WayJourneyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val way: String,
    val startEpochDay: Long,
    val startedAtMillis: Long,
    val state: String,
    val endedEpochDay: Long?,
    val toldMeters: Int,
)
