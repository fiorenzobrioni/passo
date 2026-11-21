package com.callbackdev.passo.core.data.sessions

import com.callbackdev.passo.core.data.db.SessionDao
import com.callbackdev.passo.core.data.db.StepLengthSampleEntity
import com.callbackdev.passo.core.data.db.toEntity
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.StepLengthSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The outings and their plans (PLANNING.md §11 Phase 10). The screens read and edit the plans;
 * the outings themselves are written only by the tracking service, which is the one that
 * measures them (with the step batches while under way, `TrackingRepository.persist`).
 */
@Singleton
class SessionRepository
@Inject
constructor(
    private val dao: SessionDao,
    private val preferences: UserPreferencesDataSource,
    private val routes: RouteRepository,
) {
    private val seedLock = Mutex()

    /** The plans, in the reader's order; the presets are written the first time anyone looks. */
    val plans: Flow<List<SessionPlan>> = dao.observePlans()
        .map { rows -> rows.mapNotNull { it.toModel() } }
        .onStart { seedPresets() }

    suspend fun plan(id: Long): SessionPlan? {
        seedPresets()
        return dao.plan(id)?.toModel()
    }

    /** The plans most recently started first, then in the reader's order: the launcher's shortcuts. */
    suspend fun plansByUse(): List<SessionPlan> {
        seedPresets()
        return dao.plans().mapNotNull { it.toModel() }
            .sortedWith(
                compareByDescending<SessionPlan> {
                    it.lastUsedAtMillis ?: Long.MIN_VALUE
                }.thenBy { it.position },
            )
    }

    /** Saves [plan]: a new one goes after the others. Returns its id. */
    suspend fun savePlan(plan: SessionPlan): Long = if (plan.id == 0L) {
        dao.appendPlan(plan.toEntity())
    } else {
        dao.updatePlan(plan.toEntity())
        plan.id
    }

    suspend fun deletePlan(id: Long) = dao.deletePlan(id)

    suspend fun markPlanUsed(id: Long, atMillis: Long) = dao.markPlanUsed(id, atMillis)

    /** The outing under way or paused, as last written. */
    suspend fun liveSession(): Session? = dao.liveSession()?.toModel()

    fun observeLiveSession(): Flow<Session?> = dao.observeLiveSession().map { it?.toModel() }

    suspend fun session(id: Long): Session? = dao.session(id)?.toModel()

    /** One outing as it changes, for its own page; null once it is gone. */
    fun observeSession(id: Long): Flow<Session?> = dao.observeSession(id).map { it?.toModel() }

    /** The outings walked on city walks, oldest first (Phase 11). */
    val walkSessions: Flow<List<Session>> = dao.observeWalkSessions().map { rows -> rows.mapNotNull { it.toModel() } }

    suspend fun walkSessionsNow(): List<Session> = dao.walkSessions().mapNotNull { it.toModel() }

    /** The outings started on one local day, earliest first. */
    fun observeSessionsOn(localEpochDay: Long): Flow<List<Session>> =
        dao.observeSessionsOn(localEpochDay).map { rows -> rows.mapNotNull { it.toModel() } }

    fun observeLatestFinished(): Flow<Session?> = dao.observeLatestFinished().map { it?.toModel() }

    /** The outings over from [fromDay] to [toDay] included, oldest first: a year's (Your year on foot). */
    suspend fun finishedBetween(fromDay: Long, toDay: Long): List<Session> =
        dao.finishedBetween(fromDay, toDay).mapNotNull { it.toModel() }

    /** A new outing; returns it with its id. */
    suspend fun insert(session: Session): Session = session.copy(id = dao.insertSession(session.toEntity()))

    /** Writes an outing's state outside a step batch: a pause, a resume, its end. */
    suspend fun save(session: Session) = dao.upsertSession(session)

    /**
     * Forgets an outing (one too short to keep), **its route first**: the two are in databases
     * that share no transaction, and a crash between them must leave an outing without its
     * route, a state the app already draws, never places with no outing
     * (docs/adr/0016-location.md, decision 8).
     */
    suspend fun delete(id: Long) {
        routes.delete(id)
        dao.deleteSession(id)
    }

    /** What an outing with GPS said of the reader's step on its clean stretches: numbers, no place. */
    suspend fun addStepLengthSamples(sessionId: Long, samples: List<StepLengthSample>) {
        if (samples.isEmpty()) return
        dao.insertStepLengthSamples(
            samples.map {
                StepLengthSampleEntity(
                    sessionId = sessionId,
                    cadence = it.cadence,
                    steps = it.steps,
                    meters = it.meters,
                )
            },
        )
    }

    suspend fun stepLengthSamples(sessionId: Long): List<StepLengthSample> =
        dao.stepLengthSamples(sessionId).map { StepLengthSample(it.cadence, it.steps, it.meters) }

    /**
     * Deletes every route whose outing is no longer here: at the service's start, one read of
     * each database and no wake of its own. Returns how many went.
     */
    suspend fun deleteOrphanRoutes(): Int =
        routes.deleteOrphans(dao.sessionStarts().associate { it.id to it.startedAtMillis })

    /** The last outing whose summary the reader put away on Today. */
    val summarySeen: Flow<Long?> = preferences.sessionSummarySeen

    suspend fun setSummarySeen(sessionId: Long) = preferences.setSessionSummarySeen(sessionId)

    private suspend fun seedPresets() = seedLock.withLock {
        if (!preferences.sessionPlansSeeded()) {
            // A database restored from a backup already has the reader's plans.
            if (dao.plans().isEmpty()) dao.insertPlans(SessionPlans.PRESETS.map { it.toEntity() })
            preferences.markSessionPlansSeeded()
            preferences.markIntervalPresetSeeded()
            preferences.markOpenPresetSeeded()
        }
        // Plans seeded before the interval walk (Phase 13) get it once, after theirs; a reader
        // who already made an interval plan, or deleted this one, is not given it again.
        if (!preferences.intervalPresetSeeded()) {
            val plans = dao.plans().mapNotNull { it.toModel() }
            if (plans.none { it.goalKind == SessionGoalKind.INTERVALS }) {
                dao.appendPlan(SessionPlans.JAPANESE_WALKING.toEntity())
            }
            preferences.markIntervalPresetSeeded()
        }
        // Plans seeded before the open outing (Phase 21) get its walk once, at the top of their
        // list, where a first visit finds it; a reader who already keeps an open plan, or deleted
        // this one, is not given it again.
        if (!preferences.openPresetSeeded()) {
            val plans = dao.plans().mapNotNull { it.toModel() }
            if (plans.none { it.open }) dao.prependPlan(SessionPlans.OPEN_WALK.toEntity())
            preferences.markOpenPresetSeeded()
        }
    }
}
