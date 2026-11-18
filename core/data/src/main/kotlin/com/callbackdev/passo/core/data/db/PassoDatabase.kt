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
 * - v6: the interval walk (Phase 13): the sets of a plan and of an outing (`slowMinutes`,
 *   `fastMinutes`, `sets`, columns with defaults: a plan the protocol's 3, 3 and 5, an outing
 *   none), and each interval as walked (`session_interval`, a new table).
 * - v7: the measured outing (Phase 15, docs/adr/0016-location.md): whether a plan and an outing
 *   measure with GPS (`gps`, off for every one before it), an outing's distance by the steps
 *   alone and the share that was measured (`stepsMeters`, `measuredShare`, none without GPS),
 *   its own cap and the cap it was last asked about (`capAtMillis`, `capAskedForMillis`: none
 *   for an outing from before, which reads as the longest outing from its start), and what its
 *   clean stretches said of the step (`step_length_sample`, a new table). Numbers only: the
 *   places are in another database (`routes.db`), which Android's cloud backup never carries.
 * - v8: the outing's page (Phase 16): what an outing with GPS climbed and came down
 *   (`ascentMeters`, `descentMeters`, none before it or without GPS), and the outing a hundred
 *   metres at a time (`session_leg`, a new table), from which its kilometres or its miles are
 *   cut. Numbers still. A version of its own although Phase 15's was not released: a debug
 *   build installed to try Phase 15 on a phone must not meet a second, different v7.
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
        SessionIntervalEntity::class,
        StepLengthSampleEntity::class,
        SessionLegEntity::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
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
