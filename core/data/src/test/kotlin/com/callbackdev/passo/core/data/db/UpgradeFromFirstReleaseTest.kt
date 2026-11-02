package com.callbackdev.passo.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The database as 1.0.0 wrote it (schema 3, `v1.0.0`) carried to the current one in one go, as
 * an update from the first release does: every step, every day, the counter's state, the log,
 * the reader's outings and what they walked, all kept (PLANNING.md «Open», the install and update
 * path). Each step of the chain has its own test; this is the whole road.
 */
@RunWith(AndroidJUnit4::class)
class UpgradeFromFirstReleaseTest {
    @get:Rule
    val migrations = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PassoDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun `the first release's database reaches today's with everything kept`() {
        migrations.createDatabase(DB, FIRST_RELEASE_SCHEMA).use { db ->
            db.execSQL(
                "INSERT INTO tracker_state (id, bootCount, lastCounterValue, lastSampleElapsedNanos, " +
                    "lastSampleWallMillis, updatedAtMillis) " +
                    "VALUES (0, 41, 123456, 9000000000, 1790000000000, 1790000000000)",
            )
            db.execSQL("INSERT INTO minute_steps (epochMinute, localEpochDay, steps) VALUES (29833333, 20716, 112)")
            db.execSQL(
                "INSERT INTO daily_summary (localEpochDay, steps, distanceMeters, activeKcal, activeMinutes, " +
                    "briskMinutes, goalSteps, finalized) VALUES (20716, 9120, 6710.5, 251.0, 64, 41, 8000, 1)",
            )
            db.execSQL(
                "INSERT INTO diagnostics_event (wallMillis, type, detail) " +
                    "VALUES (1790000000000, 'BOOT', 'bootCount=41')",
            )
            db.execSQL(
                "INSERT INTO session_plan (name, goalKind, goalValue, intensity, milestones, vibrate, position, " +
                    "lastUsedAtMillis, voice) VALUES ('Park', 'TIME', 20, 'BRISK', 2, 1, 0, NULL, 'OFF')",
            )
            db.execSQL(
                "INSERT INTO session (planId, name, goalKind, goalValue, restOfDay, intensity, milestones, " +
                    "vibrate, localEpochDay, startedAtMillis, state, endedAtMillis, endReason, steps, " +
                    "movingMillis, zoneMillis, distanceMeters, activeKcal, lastStepAtMillis, lastEventAtMillis, " +
                    "pausedAtMillis, reachedAtMillis, " +
                    "toldMilestones, voice) VALUES (1, 'Park', 'TIME', 20, 0, 'BRISK', 2, 1, 20716, 1789990000000, " +
                    "'FINISHED', 1789991200000, 'GOAL', 2210, 1200000, 1100000, 1630.0, 61.0, 1789991200000, " +
                    "1789991200000, NULL, 1789991200000, 6, 'OFF')",
            )
        }

        migrations.runMigrationsAndValidate(DB, CURRENT_SCHEMA, true).use { db ->
            fun single(sql: String): String = db.query(sql).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                (0 until cursor.columnCount).joinToString("|") { cursor.getString(it) ?: "null" }
            }
            assertThat(single("SELECT bootCount, lastCounterValue FROM tracker_state")).isEqualTo("41|123456")
            assertThat(single("SELECT localEpochDay, steps FROM minute_steps")).isEqualTo("20716|112")
            assertThat(single("SELECT steps, goalSteps, finalized FROM daily_summary")).isEqualTo("9120|8000|1")
            assertThat(single("SELECT type, detail FROM diagnostics_event")).isEqualTo("BOOT|bootCount=41")
            assertThat(single("SELECT name, goalKind, goalValue FROM session_plan")).isEqualTo("Park|TIME|20")
            assertThat(single("SELECT name, state, endReason, steps, walk FROM session"))
                .isEqualTo("Park|FINISHED|GOAL|2210|null")
            assertThat(single("SELECT COUNT(*) FROM way_journey")).isEqualTo("0")
        }
    }

    private companion object {
        const val DB = "upgrade-from-1.0.0.db"

        /** The schema `v1.0.0` shipped (core/data/schemas, 3.json). */
        const val FIRST_RELEASE_SCHEMA = 3

        /** [PassoDatabase]'s version now. */
        const val CURRENT_SCHEMA = 6
    }
}
