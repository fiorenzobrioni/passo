package com.callbackdev.passo.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/** The ways the reader started (PLANNING.md §11 Phase 11). */
@Dao
abstract class WayDao {
    /** Every journey, the latest started first: the one under way, then "Your ways". */
    @Query("SELECT * FROM way_journey ORDER BY startedAtMillis DESC, id DESC")
    abstract fun observeJourneys(): Flow<List<WayJourneyEntity>>

    @Query("SELECT * FROM way_journey ORDER BY startedAtMillis DESC, id DESC")
    abstract suspend fun journeys(): List<WayJourneyEntity>

    /** The journeys under way among [ways]: one way at a time, one journey a walk. */
    @Query("SELECT * FROM way_journey WHERE state = 'ACTIVE' AND way IN (:ways) ORDER BY startedAtMillis DESC")
    abstract suspend fun activeAmong(ways: List<String>): List<WayJourneyEntity>

    @Insert
    abstract suspend fun insert(journey: WayJourneyEntity): Long

    @Insert
    abstract suspend fun insertAll(journeys: List<WayJourneyEntity>)

    /** Ends the journey [id] if it is still under way; 0 when it was not. */
    @Query(
        "UPDATE way_journey SET state = :state, endedEpochDay = :endedEpochDay " +
            "WHERE id = :id AND state = 'ACTIVE'",
    )
    abstract suspend fun end(id: Long, state: String, endedEpochDay: Long): Int

    /**
     * Only forwards: a stage told is never told again, whoever writes last. Returns 1 when this
     * call moved it, 0 when it was already there: the one that moved it is the one that tells.
     */
    @Query("UPDATE way_journey SET toldMeters = :meters WHERE id = :id AND toldMeters < :meters")
    abstract suspend fun markTold(id: Long, meters: Int): Int

    /**
     * A new journey, unless one is already under way among [among] (every way, for a way: one
     * at a time). Returns its id, or null when another one is under way.
     */
    @Transaction
    open suspend fun startIfNoneActive(journey: WayJourneyEntity, among: List<String>): Long? =
        if (activeAmong(among).isNotEmpty()) null else insert(journey)

    /**
     * The walk [walk]'s journey under way, begun now if there is none; with [again], the one
     * under way is put down on [day] first and a new one begun: the walk from its start.
     */
    @Transaction
    open suspend fun walkJourney(walk: String, again: Boolean, day: Long, nowMillis: Long): WayJourneyEntity {
        val current = activeAmong(listOf(walk)).firstOrNull()
        if (current != null && !again) return current
        if (current != null) end(current.id, "LEFT", day)
        val fresh = WayJourneyEntity(
            way = walk,
            startEpochDay = day,
            startedAtMillis = nowMillis,
            state = "ACTIVE",
            endedEpochDay = null,
            toldMeters = 0,
        )
        return fresh.copy(id = insert(fresh))
    }
}
