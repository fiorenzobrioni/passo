package com.callbackdev.passo.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * The usual rounds (PLANNING.md §11 Phase 20): which outings are a round's walks, the names the
 * reader gave them, and the pairs they said are not the same. Numbers and names: a round's line is
 * its walks' routes, in `routes.db`. Every change that can leave a round with fewer than two walks
 * forgets it in the same transaction: a round is a pair at least.
 */
@Dao
abstract class RoundDao {
    @Query("SELECT * FROM round ORDER BY id")
    abstract fun observeRounds(): Flow<List<RoundEntity>>

    @Query("SELECT * FROM round_member")
    abstract fun observeMembers(): Flow<List<RoundMemberEntity>>

    @Query("SELECT * FROM round ORDER BY id")
    abstract suspend fun rounds(): List<RoundEntity>

    @Query("SELECT * FROM round_member")
    abstract suspend fun members(): List<RoundMemberEntity>

    @Query("SELECT * FROM round_apart")
    abstract suspend fun apart(): List<RoundApartEntity>

    @Query("SELECT * FROM round_apart")
    abstract fun observeApart(): Flow<List<RoundApartEntity>>

    @Query("SELECT roundId FROM round_member WHERE sessionId = :sessionId")
    abstract suspend fun roundOf(sessionId: Long): Long?

    @Query("SELECT name FROM round WHERE id = :roundId")
    protected abstract suspend fun nameOf(roundId: Long): String?

    @Query("SELECT sessionId FROM round_member WHERE roundId = :roundId")
    abstract suspend fun walksOf(roundId: Long): List<Long>

    @Insert
    protected abstract suspend fun insertRound(round: RoundEntity): Long

    @Upsert
    protected abstract suspend fun upsertMembers(members: List<RoundMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertApart(pairs: List<RoundApartEntity>)

    @Query("UPDATE round SET name = :name WHERE id = :roundId")
    abstract suspend fun rename(roundId: Long, name: String?)

    @Query("DELETE FROM round_member WHERE sessionId = :sessionId")
    protected abstract suspend fun deleteMember(sessionId: Long)

    @Query("DELETE FROM round_member WHERE roundId = :roundId")
    protected abstract suspend fun deleteMembers(roundId: Long)

    @Query("DELETE FROM round WHERE id = :roundId")
    protected abstract suspend fun deleteRound(roundId: Long)

    @Query("DELETE FROM round_apart WHERE firstId IN (:sessionIds) OR secondId IN (:sessionIds)")
    protected abstract suspend fun deleteApartOf(sessionIds: List<Long>)

    @Query(
        "DELETE FROM round_member WHERE roundId IN " +
            "(SELECT roundId FROM round_member GROUP BY roundId HAVING COUNT(*) < 2)",
    )
    protected abstract suspend fun deleteLoneMembers()

    @Query("DELETE FROM round WHERE id NOT IN (SELECT DISTINCT roundId FROM round_member)")
    protected abstract suspend fun deleteEmptyRounds()

    /** A new round of the outings [first] and [second], unnamed; its id. */
    @Transaction
    open suspend fun pair(first: Long, second: Long): Long {
        val id = insertRound(RoundEntity())
        upsertMembers(listOf(RoundMemberEntity(first, id), RoundMemberEntity(second, id)))
        // Should either have been another round's walk, that round may be one walk now.
        prune()
        return id
    }

    /** The outing [sessionId] becomes a walk of the round [roundId]. */
    @Transaction
    open suspend fun join(roundId: Long, sessionId: Long) {
        upsertMembers(listOf(RoundMemberEntity(sessionId, roundId)))
        prune()
    }

    /** The outing [sessionId] is a walk of no round any more; a round left with one walk is forgotten. */
    @Transaction
    open suspend fun leave(sessionId: Long) {
        deleteMember(sessionId)
        prune()
    }

    /**
     * «Not the same round»: the outing [sessionId] leaves its round, and the pairs it makes with each
     * of the round's walks are kept, so the two are never matched again.
     */
    @Transaction
    open suspend fun notTheSame(sessionId: Long) {
        val round = roundOf(sessionId) ?: return
        val pairs = walksOf(round).filter { it != sessionId }.map { other -> apart(sessionId, other) }
        insertApart(pairs)
        deleteMember(sessionId)
        prune()
    }

    /**
     * «Forget this round»: the round and its pairs go; its outings stay, as they are, and are found
     * again by a walk of it to come, as any outing is.
     */
    @Transaction
    open suspend fun forget(roundId: Long) {
        val walks = walksOf(roundId)
        if (walks.isNotEmpty()) deleteApartOf(walks)
        deleteMembers(roundId)
        deleteRound(roundId)
    }

    /**
     * Two rounds made one, as the reader said (PLANNING.md §15, «Phase 20: two rounds made one»):
     * the walks of [from] become [into]'s, which takes [name]; [from], left with none, is forgotten.
     * Two rounds with a pair of walks said not the same are never asked about, so none is undone.
     */
    @Transaction
    open suspend fun merge(into: Long, from: Long, name: String?) {
        if (into == from) return
        val walks = walksOf(from)
        if (walks.isEmpty() || walksOf(into).isEmpty()) return
        upsertMembers(walks.map { RoundMemberEntity(it, into) })
        rename(into, name)
        prune()
    }

    /**
     * «Another round»: the two rounds the reader said are not one. Each walk of one is kept not the
     * same as each walk of the other, so that no walk deleted later lets the question come back.
     */
    @Transaction
    open suspend fun apartRounds(first: Long, second: Long) {
        val a = walksOf(first)
        val b = walksOf(second)
        insertApart(a.flatMap { x -> b.filter { it != x }.map { y -> apart(x, y) } })
    }

    /**
     * The rounds an import brings, each with its walks by their ids on this phone: a round one of
     * whose walks is a walk of a round here already joins that one, keeping this phone's name where it
     * has one; any other is a new round. Then the pairs said not the same.
     */
    @Transaction
    open suspend fun importRounds(rounds: List<Pair<String?, List<Long>>>, pairs: List<Pair<Long, Long>>) {
        for ((name, walks) in rounds) {
            val distinct = walks.distinct()
            if (distinct.size < 2) continue
            val here = distinct.firstNotNullOfOrNull { roundOf(it) }
            val id = here ?: insertRound(RoundEntity(name = name))
            if (here != null && name != null && nameOf(here) == null) rename(here, name)
            // A walk of another round here stays in it: the phone's own grouping is not undone.
            val free = distinct.filter { roundOf(it) == null || roundOf(it) == id }
            upsertMembers(free.map { RoundMemberEntity(it, id) })
        }
        insertApart(pairs.filter { it.first != it.second }.map { apart(it.first, it.second) })
        prune()
    }

    private fun apart(a: Long, b: Long) = RoundApartEntity(minOf(a, b), maxOf(a, b))

    private suspend fun prune() {
        deleteLoneMembers()
        deleteEmptyRounds()
    }
}
