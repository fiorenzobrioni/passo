package com.callbackdev.passo.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import com.callbackdev.passo.core.model.Session
import kotlinx.coroutines.flow.Flow

/** The outings and the plans they start from (PLANNING.md §11 Phase 10). */
@Dao
abstract class SessionDao {
    @Query("SELECT * FROM session_plan ORDER BY position, id")
    abstract fun observePlans(): Flow<List<SessionPlanEntity>>

    @Query("SELECT * FROM session_plan ORDER BY position, id")
    abstract suspend fun plans(): List<SessionPlanEntity>

    @Query("SELECT * FROM session_plan WHERE id = :id")
    abstract suspend fun plan(id: Long): SessionPlanEntity?

    @Insert
    abstract suspend fun insertPlan(plan: SessionPlanEntity): Long

    @Insert
    abstract suspend fun insertPlans(plans: List<SessionPlanEntity>)

    @Update
    abstract suspend fun updatePlan(plan: SessionPlanEntity)

    @Query("DELETE FROM session_plan WHERE id = :id")
    abstract suspend fun deletePlan(id: Long)

    @Query("UPDATE session_plan SET lastUsedAtMillis = :atMillis WHERE id = :id")
    abstract suspend fun markPlanUsed(id: Long, atMillis: Long)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM session_plan")
    abstract suspend fun nextPlanPosition(): Int

    /** A new plan, placed after the others. */
    @Transaction
    open suspend fun appendPlan(plan: SessionPlanEntity): Long = insertPlan(plan.copy(position = nextPlanPosition()))

    /** The outing under way (or paused), if there is one; there is never more than one. */
    @Transaction
    @Query("SELECT * FROM session WHERE state != 'FINISHED' ORDER BY startedAtMillis DESC LIMIT 1")
    abstract suspend fun liveSession(): SessionRow?

    @Transaction
    @Query("SELECT * FROM session WHERE state != 'FINISHED' ORDER BY startedAtMillis DESC LIMIT 1")
    abstract fun observeLiveSession(): Flow<SessionRow?>

    @Transaction
    @Query("SELECT * FROM session WHERE id = :id")
    abstract suspend fun session(id: Long): SessionRow?

    @Transaction
    @Query("SELECT * FROM session WHERE localEpochDay = :localEpochDay ORDER BY startedAtMillis")
    abstract fun observeSessionsOn(localEpochDay: Long): Flow<List<SessionRow>>

    /** The last outing to have ended, for the summary Today shows once. */
    @Transaction
    @Query("SELECT * FROM session WHERE state = 'FINISHED' ORDER BY endedAtMillis DESC LIMIT 1")
    abstract fun observeLatestFinished(): Flow<SessionRow?>

    @Insert
    abstract suspend fun insertSession(session: SessionEntity): Long

    /** Every outing, oldest first: for the export. */
    @Transaction
    @Query("SELECT * FROM session ORDER BY startedAtMillis")
    abstract suspend fun allSessions(): List<SessionRow>

    @Query("SELECT COUNT(*) FROM session")
    abstract fun observeSessionCount(): Flow<Int>

    /** The outings walked on city walks, oldest first: what moves a walk (Phase 11). */
    @Transaction
    @Query("SELECT * FROM session WHERE walk IS NOT NULL ORDER BY startedAtMillis")
    abstract fun observeWalkSessions(): Flow<List<SessionRow>>

    @Transaction
    @Query("SELECT * FROM session WHERE walk IS NOT NULL ORDER BY startedAtMillis")
    abstract suspend fun walkSessions(): List<SessionRow>

    /**
     * The outings and plans an import brings (`BackupMerge`), in one transaction: the plans
     * first, after the phone's own, then the outings with their plan ids made this phone's.
     * Returns the ids the new outings got.
     */
    @Transaction
    open suspend fun importOutings(
        plans: List<Pair<Long, SessionPlanEntity>>,
        planIds: Map<Long, Long>,
        sessions: (Map<Long, Long>) -> List<Session>,
    ): List<Long> {
        val ids = HashMap(planIds)
        for ((fileId, plan) in plans) ids[fileId] = appendPlan(plan)
        return sessions(ids).map { session ->
            val id = insertSession(session.toEntity())
            upsertIntervals(session.copy(id = id).intervalEntities())
            id
        }
    }

    @Upsert
    protected abstract suspend fun upsertSessionRow(session: SessionEntity)

    @Upsert
    protected abstract suspend fun upsertIntervals(intervals: List<SessionIntervalEntity>)

    /** An outing and its intervals, in one transaction. */
    @Transaction
    open suspend fun upsertSession(session: Session) {
        upsertSessionRow(session.toEntity())
        upsertIntervals(session.intervalEntities())
    }

    @Query("DELETE FROM session WHERE id = :id")
    protected abstract suspend fun deleteSessionRow(id: Long)

    @Query("DELETE FROM session_interval WHERE sessionId = :id")
    protected abstract suspend fun deleteIntervals(id: Long)

    @Transaction
    open suspend fun deleteSession(id: Long) {
        deleteIntervals(id)
        deleteSessionRow(id)
    }
}
