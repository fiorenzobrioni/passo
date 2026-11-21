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

    @Query("UPDATE session_plan SET position = position + 1")
    protected abstract suspend fun movePlansDown()

    /** [plan] first of the list, every other one a place further down, in one transaction. */
    @Transaction
    open suspend fun prependPlan(plan: SessionPlanEntity): Long {
        movePlansDown()
        return insertPlan(plan.copy(position = 0))
    }

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

    /** One outing as it changes: its own page, while it is open. */
    @Transaction
    @Query("SELECT * FROM session WHERE id = :id")
    abstract fun observeSession(id: Long): Flow<SessionRow?>

    @Transaction
    @Query("SELECT * FROM session WHERE localEpochDay = :localEpochDay ORDER BY startedAtMillis")
    abstract fun observeSessionsOn(localEpochDay: Long): Flow<List<SessionRow>>

    /** The outings over, of the days from [fromDay] to [toDay] included. */
    @Transaction
    @Query(
        "SELECT * FROM session WHERE state = 'FINISHED' AND localEpochDay BETWEEN :fromDay AND :toDay " +
            "ORDER BY startedAtMillis",
    )
    abstract suspend fun finishedBetween(fromDay: Long, toDay: Long): List<SessionRow>

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

    /**
     * The outings over, newest first, as they change: «Your outings» (PLANNING.md §11 Phase 23,
     * step 23.2). Their own rows alone, without the intervals, the legs or the pauses, which the
     * list does not draw: a few thousand outings are then one read of numbers.
     */
    @Query("SELECT * FROM session WHERE state = 'FINISHED' ORDER BY startedAtMillis DESC, id DESC")
    abstract fun observeFinished(): Flow<List<SessionEntity>>

    /** How many outings are over, as it changes: what the door to «Your outings» counts. */
    @Query("SELECT COUNT(*) FROM session WHERE state = 'FINISHED'")
    abstract fun observeFinishedCount(): Flow<Int>

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
            upsertLegs(session.copy(id = id).legEntities())
            upsertPauses(session.copy(id = id).pauseEntities())
            id
        }
    }

    @Upsert
    protected abstract suspend fun upsertSessionRow(session: SessionEntity)

    @Upsert
    protected abstract suspend fun upsertIntervals(intervals: List<SessionIntervalEntity>)

    @Upsert
    protected abstract suspend fun upsertLegs(legs: List<SessionLegEntity>)

    @Upsert
    protected abstract suspend fun upsertPauses(pauses: List<SessionPauseEntity>)

    /** An outing with its intervals, its legs and its pauses, in one transaction. */
    @Transaction
    open suspend fun upsertSession(session: Session) {
        upsertSessionRow(session.toEntity())
        upsertIntervals(session.intervalEntities())
        upsertLegs(session.legEntities())
        upsertPauses(session.pauseEntities())
    }

    @Query("DELETE FROM session WHERE id = :id")
    protected abstract suspend fun deleteSessionRow(id: Long)

    @Query("DELETE FROM session_interval WHERE sessionId = :id")
    protected abstract suspend fun deleteIntervals(id: Long)

    @Query("DELETE FROM session_leg WHERE sessionId = :id")
    protected abstract suspend fun deleteLegs(id: Long)

    @Query("DELETE FROM step_length_sample WHERE sessionId = :id")
    protected abstract suspend fun deleteStepLengthSamples(id: Long)

    @Query("DELETE FROM session_pause WHERE sessionId = :id")
    protected abstract suspend fun deletePauses(id: Long)

    @Query("DELETE FROM round_member WHERE sessionId = :id")
    protected abstract suspend fun deleteRoundMember(id: Long)

    @Query("DELETE FROM round_apart WHERE firstId = :id OR secondId = :id")
    protected abstract suspend fun deleteRoundApart(id: Long)

    /** The walks of a round left with fewer than two: a round is a pair at least. */
    @Query(
        "DELETE FROM round_member WHERE roundId IN " +
            "(SELECT roundId FROM round_member GROUP BY roundId HAVING COUNT(*) < 2)",
    )
    protected abstract suspend fun deleteLoneRoundMembers()

    @Query("DELETE FROM round WHERE id NOT IN (SELECT DISTINCT roundId FROM round_member)")
    protected abstract suspend fun deleteEmptyRounds()

    /**
     * The outings over that were measured with GPS, oldest first, their own rows alone: what a new
     * one is searched among for its round (PLANNING.md §11 Phase 20). Numbers, a few hundred at most.
     */
    @Query("SELECT * FROM session WHERE state = 'FINISHED' AND gps = 1 ORDER BY startedAtMillis, id")
    abstract suspend fun finishedWithGps(): List<SessionEntity>

    /** These outings whole, with their legs: what a round's usual at each kilometre is read from. */
    @Transaction
    @Query("SELECT * FROM session WHERE id IN (:ids) ORDER BY startedAtMillis")
    abstract suspend fun sessions(ids: List<Long>): List<SessionRow>

    /** What an outing's clean stretches said of the step: added as it goes (Phase 15, for 17). */
    @Insert
    abstract suspend fun insertStepLengthSamples(samples: List<StepLengthSampleEntity>)

    @Query("SELECT * FROM step_length_sample WHERE sessionId = :sessionId ORDER BY id")
    abstract suspend fun stepLengthSamples(sessionId: Long): List<StepLengthSampleEntity>

    /** Every sample, oldest first: for the learner, and for the reader's own export. */
    @Query("SELECT * FROM step_length_sample ORDER BY id")
    abstract suspend fun allStepLengthSamples(): List<StepLengthSampleEntity>

    /**
     * Every sample with its outing's start, as they change: what the step that learns reads
     * (PLANNING.md §11 Phase 17). A sample goes with its outing.
     */
    @Query(
        "SELECT s.sessionId, se.startedAtMillis, s.cadence, s.steps, s.meters FROM step_length_sample s " +
            "JOIN session se ON se.id = s.sessionId ORDER BY se.startedAtMillis, s.id",
    )
    abstract fun observeStepLengthSamples(): Flow<List<OutingSampleRow>>

    /** Each outing's id with its start: what the routes' database is checked against. */
    @Query("SELECT id, startedAtMillis FROM session")
    abstract suspend fun sessionStarts(): List<SessionStart>

    /**
     * An outing with what belongs to it alone: its intervals, its legs, its pauses, what it said of
     * the step, and its place in a round (Phase 20): it leaves its round, which is forgotten once
     * fewer than two walks are left, and any pair said not the same with it goes.
     */
    @Transaction
    open suspend fun deleteSession(id: Long) {
        deleteIntervals(id)
        deleteLegs(id)
        deletePauses(id)
        deleteStepLengthSamples(id)
        deleteRoundMember(id)
        deleteRoundApart(id)
        deleteLoneRoundMembers()
        deleteEmptyRounds()
        deleteSessionRow(id)
    }
}
