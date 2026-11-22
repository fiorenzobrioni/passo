package com.callbackdev.passo.core.data.backup

import com.callbackdev.passo.core.data.db.RoundDao
import com.callbackdev.passo.core.data.db.SessionDao
import com.callbackdev.passo.core.data.db.StepLengthSampleEntity
import com.callbackdev.passo.core.data.db.TrackingDao
import com.callbackdev.passo.core.data.db.WayDao
import com.callbackdev.passo.core.data.db.toEntity
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.rounds.RoundRepository
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.backup.Backup
import com.callbackdev.passo.core.domain.backup.BackupDay
import com.callbackdev.passo.core.domain.backup.BackupMerge
import com.callbackdev.passo.core.domain.backup.BackupRound
import com.callbackdev.passo.core.domain.backup.CsvExport
import com.callbackdev.passo.core.domain.backup.CsvTable
import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.domain.rounds.RoundConstants
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.StepLengthSample
import com.callbackdev.passo.core.model.UnitSystem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * What this phone holds, as the data rows of Settings say it before an export.
 *
 * @property routes the routes kept, the one of an outing under way included: what «With the
 *   routes» would put in a backup.
 * @property deletableRoutes of them, those «Delete all routes» deletes: every one but the route
 *   still being written, of an outing under way or one that can still be taken back;
 *   [deletableBytes] what they weigh.
 * @property rounds the usual rounds kept (Phase 20): «Delete all routes» leaves them, their walks
 *   and their times, and its dialog says so where there are some.
 */
data class DataContents(
    val days: Int,
    val outings: Int,
    val routes: Int = 0,
    val deletableRoutes: Int = 0,
    val deletableBytes: Long = 0,
    val rounds: Int = 0,
)

/**
 * What an import did (ADR 0011).
 *
 * @property addedDays days the phone did not have.
 * @property mergedDays days both had, where the file added steps.
 * @property unchangedDays days the phone already had whole.
 * @property preferences the file's profile and settings were taken.
 * @property stepFallback the profile taken had a step length this version cannot read (a newer
 *   Passo's): the height's estimate is in use instead, and the summary says so.
 */
data class ImportReport(
    val addedDays: Int,
    val mergedDays: Int,
    val unchangedDays: Int,
    val addedOutings: Int,
    val addedPlans: Int,
    val preferences: Boolean,
    val addedWays: Int = 0,
    val addedRoutes: Int = 0,
    val stepFallback: Boolean = false,
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
    private val wayDao: WayDao,
    private val preferences: UserPreferencesDataSource,
    private val tracking: TrackingRepository,
    private val routes: RouteRepository,
    private val live: LiveSession,
    private val roundDao: RoundDao,
    private val rounds: RoundRepository,
) {
    // The outing with GPS whose route may still grow: under way (as written), or ended a moment
    // ago and still to be taken back (as the service holds it). Its route is not the reader's
    // to delete yet.
    private val growing: Flow<Long?> = combine(
        sessionDao.observeLiveSession().map { row -> row?.session?.takeIf { it.gps }?.id },
        live.current.map { state -> state?.session?.takeIf { it.gps }?.id },
    ) { written, held -> written ?: held }.distinctUntilChanged()

    val contents: Flow<DataContents> = combine(
        tracking.observeDayCount(),
        sessionDao.observeSessionCount(),
        routes.routeIds,
        growing,
        // The rounds the reader can see: every round kept, three walks at least.
        rounds.rounds.map { it.size }.distinctUntilChanged(),
    ) { days, outings, kept, underWay, roundCount ->
        // The route still being written is not the reader's to delete yet: «Delete all routes»
        // counts and weighs the others, and is not offered for that one alone.
        val deletable = kept.count { it != underWay }
        DataContents(
            days = days,
            outings = outings,
            routes = kept.size,
            deletableRoutes = deletable,
            deletableBytes = if (deletable > 0) routes.bytes(except = underWay) else 0,
            rounds = roundCount,
        )
    }

    /**
     * Forgets every route kept on this phone but the one that may still grow (an outing with GPS
     * under way, or one that can still be taken back); the outings and their numbers stay.
     */
    suspend fun deleteRoutes() = routes.deleteAll(keep = growing.first())

    /**
     * Everything a new phone could not rebuild, read as it stands now.
     *
     * @param withRoutes the reader ticked «With the routes»: where the outings measured with
     *   GPS went is in the file too. Off unless they say so (docs/adr/0016-location.md,
     *   decision 9): a backup is numbers, and a route is a place.
     */
    suspend fun backup(appVersion: String, nowMillis: Long, zone: ZoneId, withRoutes: Boolean = false): Backup {
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
        // What the outings with GPS said of the step, by the outing's start: its id is this
        // phone's alone.
        val starts = sessionDao.sessionStarts().associate { it.id to it.startedAtMillis }
        val samples = sessionDao.allStepLengthSamples()
            .groupBy({ starts[it.sessionId] }) { StepLengthSample(it.cadence, it.steps, it.meters) }
            .mapNotNull { (start, said) -> start?.let { it to said } }
            .toMap()
        val kept = if (withRoutes) {
            starts.mapNotNull { (id, start) -> routes.route(id, start)?.let { start to it } }.toMap()
        } else {
            emptyMap()
        }
        // The usual rounds, by their walks' starts: names and numbers, so in every backup.
        val members = roundDao.members().groupBy({ it.roundId }) { it.sessionId }
        val keptRounds = roundDao.rounds().mapNotNull { round ->
            members[round.id]?.mapNotNull { starts[it] }?.takeIf { it.size >= RoundConstants.WALKS }?.let {
                BackupRound(round.name, it)
            }
        }
        val apart = roundDao.apart().mapNotNull { pair ->
            val first = starts[pair.firstId] ?: return@mapNotNull null
            val second = starts[pair.secondId] ?: return@mapNotNull null
            first to second
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
            journeys = wayDao.journeys().mapNotNull { it.toModel() },
            stepLengthSamples = samples,
            routes = kept,
            rounds = keptRounds,
            roundsApart = apart,
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
        ) { ids -> BackupMerge.sessions(localSessions, backup.sessions, ids) }
        // What the outings that came said of the step comes with them; an outing this phone
        // already had keeps its own, and one the import left out leaves its samples out.
        if (newIds.isNotEmpty() && backup.stepLengthSamples.isNotEmpty()) {
            val came = sessionDao.sessionStarts().filter { it.id in newIds }
            sessionDao.insertStepLengthSamples(
                came.flatMap { outing ->
                    backup.stepLengthSamples[outing.startedAtMillis].orEmpty().map {
                        StepLengthSampleEntity(
                            sessionId = outing.id,
                            cadence = it.cadence,
                            steps = it.steps,
                            meters = it.meters,
                        )
                    }
                },
            )
        }
        // The file's routes, each on the outing that started at its instant, where that outing
        // has none: the ones that came with their outings, and the ones of outings this phone
        // had without their route (a restore from the cloud copy). Never one taken away.
        val added = if (backup.routes.isEmpty()) {
            emptyList()
        } else {
            BackupMerge.routes(
                outings = sessionDao.sessionStarts().associate { it.startedAtMillis to it.id },
                having = routes.routeIds.first(),
                incoming = backup.routes,
            )
        }
        for ((sessionId, route) in added) {
            routes.restore(sessionId, route.sessionStartedAtMillis, route.points, route.altitudes)
        }
        // The file's usual rounds, on this phone's outings; then the outings that came with their
        // route and no round are searched for theirs, as each would have been as it ended here.
        if (backup.rounds.isNotEmpty() || backup.roundsApart.isNotEmpty()) {
            val (fileRounds, pairs) = BackupMerge.rounds(
                outings = sessionDao.sessionStarts().associate { it.startedAtMillis to it.id },
                incoming = backup.rounds,
                apart = backup.roundsApart,
            )
            roundDao.importRounds(fileRounds, pairs)
        }
        if (added.isNotEmpty()) {
            val inRounds = roundDao.members().mapTo(HashSet()) { it.sessionId }
            rounds.searchAmong(added.map { it.first }.filter { it !in inRounds })
            // Older walks of a round the phone has, come with their routes, make a round of their own
            // (each is searched among the ones before it): joined to the phone's, by their lines.
            rounds.joinAlike(newIds + added.map { it.first })
        }
        // An outing walked on the other phone today is history here, not one just over: Today's
        // card for the last outing stays put away.
        if (newIds.isNotEmpty()) {
            val latest = sessionDao.allSessions().map { it.session }.filter { it.endedAtMillis != null }.maxByOrNull {
                it.endedAtMillis
                    ?: 0
            }
            if (latest != null && latest.id in newIds) preferences.setSessionSummarySeen(latest.id)
        }
        // The file's day as the phone that wrote it saw it; an unreadable zone reads as this one's.
        val zone = runCatching { ZoneId.of(backup.zone) }.getOrDefault(ZoneId.systemDefault())
        val fileDay = LocalDate.ofInstant(Instant.ofEpochMilli(backup.exportedAtMillis), zone).toEpochDay()
        val ways = BackupMerge.journeys(wayDao.journeys().mapNotNull { it.toModel() }, backup.journeys, fileDay)
        wayDao.insertAll(ways.map { it.toEntity() })
        return ImportReport(
            addedDays = days.addedDays,
            mergedDays = days.mergedDays,
            unchangedDays = days.unchangedDays,
            addedOutings = newIds.size,
            addedPlans = plans.toAdd.size,
            preferences = withPreferences,
            addedWays = ways.size,
            addedRoutes = added.size,
            stepFallback = withPreferences && backup.stepFallback,
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
