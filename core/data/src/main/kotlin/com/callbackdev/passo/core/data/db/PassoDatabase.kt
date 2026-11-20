package com.callbackdev.passo.core.data.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

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
 * - v9: the open outing (Phase 21): an outing's pauses, from Pause to Resume (`session_pause`, a
 *   new table), for every outing; none before it. The open outing itself needs no column: a goal
 *   kind is stored by its name, and `OPEN` is one more name.
 * - v10: the open outing's marks on the way (Phase 21.2): a plan's choice (`marks` on
 *   `session_plan`, none until the reader makes one, the plan then following its GPS switch), and
 *   an outing's mark frozen at its start, its unit and how many were told (`marks`, `markMiles`,
 *   `toldMarks` on `session`: none for every outing before it, as for every goal outing).
 * - v11: the shape of an outing (Phase 21.3): whether its pauses are known (`pausesKept` on
 *   `session`), so its minutes are drawn with their gaps; false for every outing before it, whose
 *   minutes in a pause would be drawn as walked, but for those that could only come from a Passo
 *   that kept them: an open outing, and one with a pause written ([KeptPauses]).
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
        SessionPauseEntity::class,
    ],
    version = 11,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11, spec = PassoDatabase.KeptPauses::class),
    ],
)
abstract class PassoDatabase : RoomDatabase() {
    abstract fun trackingDao(): TrackingDao

    abstract fun sessionDao(): SessionDao

    abstract fun wayDao(): WayDao

    /**
     * An outing from before v11 has its pauses known only where nothing else could have walked
     * it: an open outing came with the pauses (v9), and a pause written proves the Passo that
     * wrote it kept them. Any other outing from before is left unknown, its page with no shape
     * rather than a shape with a pause drawn as walking.
     */
    class KeptPauses : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "UPDATE session SET pausesKept = 1 " +
                    "WHERE goalKind = 'OPEN' OR id IN (SELECT DISTINCT sessionId FROM session_pause)",
            )
        }
    }

    companion object {
        const val NAME = "passo.db"
    }
}
