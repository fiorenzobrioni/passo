package com.callbackdev.passo.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.callbackdev.passo.core.domain.rounds.RoundConstants
import com.callbackdev.passo.core.domain.rounds.RoundMerge
import kotlinx.coroutines.flow.Flow

/**
 * The usual rounds (PLANNING.md §11 Phase 20): which outings are a round's walks, the names the
 * reader gave them, and the pairs they said are not the same. Numbers and names: a round's line is
 * its walks' routes, in `routes.db`. Every change that can leave a round with fewer than
 * [RoundConstants.WALKS] walks forgets it in the same transaction: a round is three walks at least
 * (§15, «Phase 20: one rule, one threshold, a recent usual»).
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
            "(SELECT roundId FROM round_member GROUP BY roundId HAVING COUNT(*) < ${RoundConstants.WALKS})",
    )
    protected abstract suspend fun deleteLoneMembers()

    @Query("DELETE FROM round WHERE id NOT IN (SELECT DISTINCT roundId FROM round_member)")
    protected abstract suspend fun deleteEmptyRounds()

    /**
     * One round of the rounds [rounds] and the outings [walks] (§15, «Phase 20: one rule, one
     * threshold, a recent usual»): the round that keeps its id is the one with the most walks, the
     * oldest of equals; its name is its own, or the first of the others' in [rounds]' order, or
     * [name]. With no round, a new one of [walks], [RoundConstants.WALKS] of them at least. Its id;
     * null when there is no round.
     */
    @Transaction
    open suspend fun unite(rounds: List<Long>, walks: List<Long>, name: String? = null): Long? {
        val held = rounds.distinct().map { it to walksOf(it) }.filter { it.second.isNotEmpty() }
        val kept = held
            .maxWithOrNull(compareBy<Pair<Long, List<Long>>> { it.second.size }.thenByDescending { it.first })
            ?.first
        val all = (held.flatMap { it.second } + walks).distinct()
        if (kept == null && all.size < RoundConstants.WALKS) return null
        val into = kept ?: insertRound(RoundEntity())
        val names = (listOfNotNull(kept) + held.map { it.first }).firstNotNullOfOrNull { nameOf(it) }
        upsertMembers(all.map { RoundMemberEntity(it, into) })
        rename(into, names ?: name)
        prune()
        return into.takeIf { walksOf(it).isNotEmpty() }
    }

    /** A new round of the outings [walks], [RoundConstants.WALKS] of them at least, unnamed; its id. */
    open suspend fun start(walks: List<Long>): Long? = unite(emptyList(), walks)

    /** The outing [sessionId] is a walk of no round any more; a round left under three walks is forgotten. */
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
     * The rounds an import brings, each with its walks by their ids on this phone, after the pairs
     * said not the same. By the one rule (§15, «Phase 20: one rule, one threshold, a recent usual»),
     * a round of the file is one with every round here that holds one of its walks, and with its walks
     * in none: they become one round, this phone's name kept where it has one, the file's otherwise.
     * Nothing said not the same is put in one round: a round or the file's other walks with such a
     * pair to what is taken already stay apart, the phone's round with the most walks taken first.
     */
    @Transaction
    open suspend fun importRounds(rounds: List<Pair<String?, List<Long>>>, pairs: List<Pair<Long, Long>>) {
        insertApart(pairs.filter { it.first != it.second }.map { apart(it.first, it.second) })
        val said = apart().mapTo(HashSet()) { it.firstId to it.secondId }
        for ((name, walks) in rounds) {
            val distinct = walks.distinct()
            if (distinct.size < RoundConstants.WALKS) continue
            val here = distinct.mapNotNull { roundOf(it) }.distinct()
                .map { it to walksOf(it) }
                .sortedWith(compareByDescending<Pair<Long, List<Long>>> { it.second.size }.thenBy { it.first })
            val free = distinct.filter { roundOf(it) == null }
            val taken = HashSet<Long>()
            val joined = ArrayList<Long>()
            for ((id, members) in here) {
                if (RoundMerge.saidApart(taken, members, said)) continue
                taken += members
                joined += id
            }
            if (RoundMerge.saidApart(taken, free, said)) {
                if (joined.isNotEmpty()) unite(joined, emptyList(), name)
                unite(emptyList(), free, name)
            } else {
                unite(joined, free, name)
            }
        }
        prune()
    }

    private fun apart(a: Long, b: Long) = RoundApartEntity(minOf(a, b), maxOf(a, b))

    private suspend fun prune() {
        deleteLoneMembers()
        deleteEmptyRounds()
    }
}
