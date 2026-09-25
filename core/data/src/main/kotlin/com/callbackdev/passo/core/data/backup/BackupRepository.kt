package com.callbackdev.passo.core.data.backup

import com.callbackdev.passo.core.data.db.SessionDao
import com.callbackdev.passo.core.data.db.TrackingDao
import com.callbackdev.passo.core.data.db.toEntity
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.backup.Backup
import com.callbackdev.passo.core.domain.backup.BackupDay
import com.callbackdev.passo.core.domain.backup.BackupMerge
import com.callbackdev.passo.core.domain.backup.CsvExport
import com.callbackdev.passo.core.domain.backup.CsvTable
import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** What this phone holds, as the data rows of Settings say it before an export. */
data class DataContents(val days: Int, val outings: Int)

/**
 * What an import did (ADR 0011).
 *
 * @property addedDays days the phone did not have.
 * @property mergedDays days both had, where the file added steps.
 * @property unchangedDays days the phone already had whole.
 * @property preferences the file's profile and settings were taken.
 */
data class ImportReport(
    val addedDays: Int,
    val mergedDays: Int,
    val unchangedDays: Int,
    val addedOutings: Int,
    val addedPlans: Int,
    val preferences: Boolean,
)

/**
 * The export and the import (PLANNING.md §11 Phase 7, ADR 0011): a whole backup to a file and
 * back, and the tables a spreadsheet can read. Files are the caller's (the Storage Access
 * Framework hands the screen a stream; no storage permission): this class makes and takes
 * their contents.
 */
@Singleton
class BackupRepository
@Inject
constructor(
    private val trackingDao: TrackingDao,
    private val sessionDao: SessionDao,
    private val preferences: UserPreferencesDataSource,
    private val tracking: TrackingRepository,
) {
    val contents: Flow<DataContents> =
        combine(tracking.observeDayCount(), sessionDao.observeSessionCount()) { days, outings ->
            DataContents(days, outings)
        }

    /** Everything a new phone could not rebuild, read as it stands now. */
    suspend fun backup(appVersion: String, nowMillis: Long, zone: ZoneId): Backup {
        val current = preferences.current()
        val history = trackingDao.history()
        val minutesByDay = history.minutes
            .map { MinuteSteps(it.epochMinute, it.localEpochDay, it.steps) }
            .groupBy { it.localEpochDay }
        val summaries = history.summaries.associate { it.localEpochDay to it.toModel() }
        val days = (summaries.keys + minutesByDay.keys).sorted().map { day ->
            val minutes = minutesByDay[day].orEmpty()
            // A day always has its summary; should one be missing, it is made from its minutes
            // rather than leaving the minutes without a day.
            val summary = summaries[day] ?: DaySummaries.summarize(
                day,
                minutes.map { it.steps },
                current.profile,
                current.settings.dailyGoalSteps,
                finalized = false,
            )
            BackupDay(summary, minutes)
        }
        return Backup(
            exportedAtMillis = nowMillis,
            zone = zone.id,
            appVersion = appVersion,
            profile = current.profile,
            settings = current.settings,
            days = days,
            plans = sessionDao.plans().mapNotNull { it.toModel() },
            sessions = sessionDao.allSessions().mapNotNull { it.toModel() },
            diagnostics = tracking.diagnostics(),
        )
    }

    /**
     * Takes [backup] in: when [withPreferences], its profile and settings first (through the
     * rules that freeze this phone's past days before a profile or goal changes), then its days,
     * then its plans and outings. Nothing on the phone is deleted.
     */
    suspend fun import(backup: Backup, withPreferences: Boolean): ImportReport {
        if (withPreferences) {
            tracking.changeProfile { backup.profile }
            tracking.changeSettings { BackupMerge.settings(it, backup.settings) }
        }
        val days = tracking.importDays(backup)

        val localPlans = sessionDao.plans().mapNotNull { it.toModel() }
        val plans = BackupMerge.plans(localPlans, backup.plans)
        val localSessions = sessionDao.allSessions().mapNotNull { it.toModel() }
        val newIds = sessionDao.importOutings(
            plans = plans.toAdd.map { it.id to it.copy(id = 0).toEntity() },
            planIds = plans.matched,
        ) { ids -> BackupMerge.sessions(localSessions, backup.sessions, ids).map { it.toEntity() } }
        // An outing walked on the other phone today is history here, not one just over: Today's
        // card for the last outing stays put away.
        if (newIds.isNotEmpty()) {
            val latest = sessionDao.allSessions().filter { it.endedAtMillis != null }.maxByOrNull {
                it.endedAtMillis
                    ?: 0
            }
            if (latest != null && latest.id in newIds) preferences.setSessionSummarySeen(latest.id)
        }
        return ImportReport(
            addedDays = days.addedDays,
            mergedDays = days.mergedDays,
            unchangedDays = days.unchangedDays,
            addedOutings = newIds.size,
            addedPlans = plans.toAdd.size,
            preferences = withPreferences,
        )
    }

    /** One table for a spreadsheet, in [units] and with times in [zone]. */
    suspend fun csv(table: CsvTable, units: UnitSystem, zone: ZoneId): String = when (table) {
        CsvTable.DAYS -> CsvExport.days(trackingDao.allSummaries().map { it.toModel() }, units)

        CsvTable.MINUTES -> CsvExport.minutes(
            trackingDao.allMinutes().map { MinuteSteps(it.epochMinute, it.localEpochDay, it.steps) },
            zone,
        )

        CsvTable.OUTINGS -> CsvExport.outings(sessionDao.allSessions().mapNotNull { it.toModel() }, units, zone)
    }
}
