package com.callbackdev.passo.core.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import com.callbackdev.passo.core.model.RouteBox

/**
 * An outing's route, as a whole: which outing, how many points, and the box they lie in. The
 * points themselves are in [RouteChunkEntity].
 *
 * @property sessionId the outing's id in `passo.db`.
 * @property sessionStartedAtMillis the outing's start: the one thing of an outing that is the
 *   same on every phone, and what tells a route apart from a stranger's with the same id (a
 *   `passo.db` restored from elsewhere beside a `routes.db` that stayed).
 */
@Entity(tableName = "route")
data class RouteEntity(
    @PrimaryKey val sessionId: Long,
    val sessionStartedAtMillis: Long,
    val pointCount: Int,
    val southE6: Int,
    val westE6: Int,
    val northE6: Int,
    val eastE6: Int,
)

/**
 * A run of a route's points, as `RouteCodec` writes them: appended as the outing goes, one with
 * each batch of steps, each standing by itself. Not a data class: its bytes have no value
 * equality worth the name.
 */
@Entity(tableName = "route_chunk", primaryKeys = ["sessionId", "position"])
class RouteChunkEntity(val sessionId: Long, val position: Int, val bytes: ByteArray)

@Dao
abstract class RouteDao {
    @Query("SELECT * FROM route WHERE sessionId = :sessionId")
    abstract suspend fun route(sessionId: Long): RouteEntity?

    @Query("SELECT * FROM route")
    abstract suspend fun routes(): List<RouteEntity>

    @Query("SELECT bytes FROM route_chunk WHERE sessionId = :sessionId ORDER BY position")
    abstract suspend fun chunks(sessionId: Long): List<ByteArray>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM route_chunk WHERE sessionId = :sessionId")
    protected abstract suspend fun nextPosition(sessionId: Long): Int

    @Upsert
    protected abstract suspend fun upsertRoute(route: RouteEntity)

    @Insert
    protected abstract suspend fun insertChunk(chunk: RouteChunkEntity)

    /**
     * Adds a chunk of [count] points lying in [box] to the route of outing [sessionId], and
     * brings the route's own row up to date, in one transaction of this database.
     */
    @Transaction
    open suspend fun append(
        sessionId: Long,
        sessionStartedAtMillis: Long,
        bytes: ByteArray,
        count: Int,
        box: RouteBox,
    ) {
        val existing = route(sessionId)?.takeIf { it.sessionStartedAtMillis == sessionStartedAtMillis }
        // A row left by another outing that once had this id: its points are not this one's.
        if (existing == null) deleteChunks(sessionId)
        insertChunk(RouteChunkEntity(sessionId, nextPosition(sessionId), bytes))
        upsertRoute(
            RouteEntity(
                sessionId = sessionId,
                sessionStartedAtMillis = sessionStartedAtMillis,
                pointCount = (existing?.pointCount ?: 0) + count,
                southE6 = minOf(existing?.southE6 ?: box.southE6, box.southE6),
                westE6 = minOf(existing?.westE6 ?: box.westE6, box.westE6),
                northE6 = maxOf(existing?.northE6 ?: box.northE6, box.northE6),
                eastE6 = maxOf(existing?.eastE6 ?: box.eastE6, box.eastE6),
            ),
        )
    }

    @Query("DELETE FROM route WHERE sessionId = :sessionId")
    protected abstract suspend fun deleteRoute(sessionId: Long)

    @Query("DELETE FROM route_chunk WHERE sessionId = :sessionId")
    protected abstract suspend fun deleteChunks(sessionId: Long)

    /** A route and its points, gone together. */
    @Transaction
    open suspend fun delete(sessionId: Long) {
        deleteChunks(sessionId)
        deleteRoute(sessionId)
    }
}

/**
 * The routes of the outings measured with GPS, in a database of their own
 * (docs/adr/0016-location.md, decisions 8 and 9): `routes.db`, kept apart from `passo.db` so
 * that the days and the outings' numbers can travel in Android's cloud backup while where the
 * reader walked does not. It is named only under the direct phone-to-phone transfer
 * (`data_extraction_rules.xml`).
 *
 * `passo.db` is the truth: an outing, its distance and what share of it was measured live
 * there, written with the steps. A route is written after that, and one that lost its last
 * chunk to a crash is a shorter line, never a lost outing. The two share no transaction, so a
 * route is always deleted before its outing: what a crash between the two leaves is an outing
 * without its route, never places with no outing.
 *
 * - v1: `route` and `route_chunk` (Phase 15).
 */
@Database(entities = [RouteEntity::class, RouteChunkEntity::class], version = 1, exportSchema = true)
abstract class RouteDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao

    companion object {
        const val NAME = "routes.db"
    }
}
