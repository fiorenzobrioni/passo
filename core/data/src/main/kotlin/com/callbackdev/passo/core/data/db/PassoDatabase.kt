package com.callbackdev.passo.core.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Passo's database (PLANNING.md §5). The schema is exported to `core/data/schemas` and
 * committed, so every later version gets an auto-migration generated from a schema that is in
 * the history.
 *
 * - v1: steps, summaries, the tracker state, the log.
 * - v2: the outings and their plans (Phase 10); two new tables, nothing else touched.
 * - v3: whether an outing speaks (`voice`, Phase 10's second iteration), a column with a default
 *   on each of the two; every plan and outing before it is silent, as it was.
 * - v4: the ways the reader started (`way_journey`, Phase 11); one new table, nothing else touched.
 * - v5: the city walk an outing walks, and where on it it began (`walk`, `walkFromMeters` on
 *   `session`, Phase 11's second part): every outing before it walks none, from 0.
 */
@Database(
    entities = [
        TrackerStateEntity::class,
        MinuteStepsEntity::class,
        DailySummaryEntity::class,
        DiagnosticsEventEntity::class,
        SessionPlanEntity::class,
        SessionEntity::class,
        WayJourneyEntity::class,
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
abstract class PassoDatabase : RoomDatabase() {
    abstract fun trackingDao(): TrackingDao

    abstract fun sessionDao(): SessionDao

    abstract fun wayDao(): WayDao

    companion object {
        const val NAME = "passo.db"
    }
}
