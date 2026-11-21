package com.callbackdev.passo.core.data.db

import androidx.room.AutoMigration
import androidx.room.ColumnInfo
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
import kotlinx.coroutines.flow.Flow

/**
 * An outing's route, as a whole: which outing, how many points, and the box they lie in. The
 * points themselves are in [RouteChunkEntity].
 *
 * @property sessionId the outing's id in `passo.db`.
 * @property sessionStartedAtMillis the outing's start: the one thing of an outing that is the
 *   same on every phone, and what tells a route apart from a stranger's with the same id (a
 *   `passo.db` restored from elsewhere beside a `routes.db` that stayed).
 * @property altitudeSource where its points' heights came from (`AltitudeSource`, by name):
 *   the pressure sensor and the satellites together, or the satellites alone; null when no
 *   point has a height, and for a route from before the heights were kept.
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
    @ColumnInfo(defaultValue = "NULL") val altitudeSource: String? = null,
)

/**
 * A run of a route's points, as `RouteCodec` writes them: appended as the outing goes, one with
 * each batch of steps, each standing by itself. Not a data class: its bytes have no value
 * equality worth the name.
 */
@Entity(tableName = "route_chunk", primaryKeys = ["sessionId", "position"])
class RouteChunkEntity(val sessionId: Long, val position: Int, val bytes: ByteArray)

/** A route's outing and its size, without its points: [RouteDao.observeRouteKeys]'s rows. */
data class RouteKeyRow(val sessionId: Long, val sessionStartedAtMillis: Long, val pointCount: Int)

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
        altitudeSource: String? = null,
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
                // The latest chunk's word: a pressure sensor that came in late is still one.
                altitudeSource = altitudeSource ?: existing?.altitudeSource,
            ),
        )
    }

    /** How many routes are kept, and how many bytes their points take: for Your data. */
    @Query("SELECT COUNT(*) FROM route")
    abstract suspend fun routeCount(): Int

    @Query("SELECT COALESCE(SUM(LENGTH(bytes)), 0) FROM route_chunk")
    abstract suspend fun routeBytes(): Long

    /** The same but for the route of the outing [sessionId]. */
    @Query("SELECT COALESCE(SUM(LENGTH(bytes)), 0) FROM route_chunk WHERE sessionId != :sessionId")
    abstract suspend fun routeBytesExcept(sessionId: Long): Long

    /** The same, as they change: a route written or deleted. */
    @Query("SELECT COUNT(*) FROM route")
    abstract fun observeRouteCount(): Flow<Int>

    /** How many points the route of [sessionId] holds, as it grows or goes; null with no route. */
    @Query("SELECT pointCount FROM route WHERE sessionId = :sessionId")
    abstract fun observePointCount(sessionId: Long): Flow<Int?>

    /** The outings that have a route, as they change: an outing's page asks for its own. */
    @Query("SELECT sessionId FROM route")
    abstract fun observeRouteIds(): Flow<List<Long>>

    /**
     * Each route's outing, by its id and its start, and how many points it holds, as they change:
     * what «Your outings» reads to know which outings have a route, without a point of it
     * (PLANNING.md §11 Phase 23, step 23.2).
     */
    @Query("SELECT sessionId, sessionStartedAtMillis, pointCount FROM route")
    abstract fun observeRouteKeys(): Flow<List<RouteKeyRow>>

    @Query("DELETE FROM route")
    protected abstract suspend fun deleteAllRoutes()

    @Query("DELETE FROM route_chunk")
    protected abstract suspend fun deleteAllChunks()

    /** Every route and its points, gone together; the outings are in another database, untouched. */
    @Transaction
    open suspend fun deleteAll() {
        deleteAllChunks()
        deleteAllRoutes()
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
 * - v2: where a route's heights came from (`altitudeSource`, Phase 16): none for a route from
 *   before.
 */
@Database(
    entities = [RouteEntity::class, RouteChunkEntity::class],
    version = 2,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class RouteDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao

    companion object {
        const val NAME = "routes.db"
    }
}
