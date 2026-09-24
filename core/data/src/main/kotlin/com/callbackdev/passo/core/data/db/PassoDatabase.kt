package com.callbackdev.passo.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Passo's database, schema v1 (PLANNING.md §5). The schema is exported to
 * `core/data/schemas` and committed, so every later version gets an auto-migration generated
 * from a schema that is in the history.
 */
@Database(
    entities = [
        TrackerStateEntity::class,
        MinuteStepsEntity::class,
        DailySummaryEntity::class,
        DiagnosticsEventEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class PassoDatabase : RoomDatabase() {
    abstract fun trackingDao(): TrackingDao

    companion object {
        const val NAME = "passo.db"
    }
}
