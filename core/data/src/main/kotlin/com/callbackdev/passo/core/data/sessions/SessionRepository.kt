package com.callbackdev.passo.core.data.sessions

import com.callbackdev.passo.core.data.db.SessionDao
import com.callbackdev.passo.core.data.db.toEntity
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionPlan
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
constructor(private val dao: SessionDao, private val preferences: UserPreferencesDataSource) {
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
            .sortedWith(compareByDescending<SessionPlan> { it.lastUsedAtMillis ?: Long.MIN_VALUE }.thenBy { it.position })
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

    /** The outings started on one local day, earliest first. */
    fun observeSessionsOn(localEpochDay: Long): Flow<List<Session>> =
        dao.observeSessionsOn(localEpochDay).map { rows -> rows.mapNotNull { it.toModel() } }

    fun observeLatestFinished(): Flow<Session?> = dao.observeLatestFinished().map { it?.toModel() }

    /** A new outing; returns it with its id. */
    suspend fun insert(session: Session): Session = session.copy(id = dao.insertSession(session.toEntity()))

    /** Writes an outing's state outside a step batch: a pause, a resume, its end. */
    suspend fun save(session: Session) = dao.upsertSession(session.toEntity())

    /** Forgets an outing too short to keep. */
    suspend fun delete(id: Long) = dao.deleteSession(id)

    /** The last outing whose summary the reader put away on Today. */
    val summarySeen: Flow<Long?> = preferences.sessionSummarySeen

    suspend fun setSummarySeen(sessionId: Long) = preferences.setSessionSummarySeen(sessionId)

    private suspend fun seedPresets() = seedLock.withLock {
        if (preferences.sessionPlansSeeded()) return@withLock
        // A database restored from a backup already has the reader's plans.
        if (dao.plans().isEmpty()) dao.insertPlans(SessionPlans.PRESETS.map { it.toEntity() })
        preferences.markSessionPlansSeeded()
    }
}
