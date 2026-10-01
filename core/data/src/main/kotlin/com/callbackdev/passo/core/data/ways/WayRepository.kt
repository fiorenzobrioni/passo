package com.callbackdev.passo.core.data.ways

import com.callbackdev.passo.core.data.db.WayDao
import com.callbackdev.passo.core.data.db.WayJourneyEntity
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.callbackdev.passo.core.model.WayKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The ways the reader started (PLANNING.md §11 Phase 11). A journey keeps where it began and
 * how far it has been told; where the reader stands is computed from the days, every time.
 */
@Singleton
class WayRepository
@Inject
constructor(private val dao: WayDao, private val tracking: TrackingRepository) {
    /** Every journey, the latest started first. */
    val journeys: Flow<List<WayJourney>> = dao.observeJourneys().map { rows -> rows.mapNotNull { it.toModel() } }

    /** The way under way, if there is one (a city walk is not a way: it is walked in outings). */
    val active: Flow<WayJourney?> = journeys.map { all -> all.firstOrNull { it.isActiveWay() } }

    suspend fun activeNow(): WayJourney? = dao.journeys().mapNotNull { it.toModel() }.firstOrNull { it.isActiveWay() }

    /**
     * Starts [way] from [startDay]. A start in the past places the reader at once, and the
     * stages behind them are stamped, never announced: the journey is told up to there. Null
     * when another way is under way: one at a time.
     */
    suspend fun start(way: WayId, startDay: LocalDate, today: LocalDate, nowMillis: Long): Long? {
        val pending = WayJourney(0, way, startDay.toEpochDay(), nowMillis, WayJourneyState.ACTIVE, null, 0)
        val walked = WayProgress.of(
            Ways.of(way),
            pending,
            tracking.observeAllSummaries().first().distances(),
            today.toEpochDay(),
        )
        return dao.startIfNoneActive(
            WayJourneyEntity(
                way = way.name,
                startEpochDay = pending.startEpochDay,
                startedAtMillis = nowMillis,
                state = WayJourneyState.ACTIVE.name,
                endedEpochDay = null,
                toldMeters = walked.walkedMeters.toInt(),
            ),
            among = WAYS,
        )
    }

    /**
     * The city walk [walk]'s journey to walk on now: the one under way, or a new one begun
     * [today]. With [again], the one under way is put down first: the walk from its start, its
     * earlier outings kept in History (Phase 11's second part).
     */
    suspend fun walkJourney(walk: WayId, again: Boolean, today: LocalDate, nowMillis: Long): WayJourney {
        require(walk.kind == WayKind.WALK)
        return checkNotNull(dao.walkJourney(walk.name, again, today.toEpochDay(), nowMillis).toModel())
    }

    /**
     * The city walk [walk] was walked to its last place on [day]: its journey is over, kept as
     * finished; the next outing on it begins a new one, from the start.
     */
    suspend fun finishWalk(walk: WayId, day: Long) {
        require(walk.kind == WayKind.WALK)
        dao.activeAmong(listOf(walk.name)).forEach { dao.end(it.id, WayJourneyState.FINISHED.name, day) }
    }

    /** The reader puts the way down where it stands on [day]. */
    suspend fun leave(id: Long, day: LocalDate) {
        dao.end(id, WayJourneyState.LEFT.name, day.toEpochDay())
    }

    /** The end was reached on [day]: the days after it no longer count. */
    suspend fun finish(id: Long, day: Long) {
        dao.end(id, WayJourneyState.FINISHED.name, day)
    }

    /**
     * Claims the stages up to [meters] along the way as told; true when this call claimed
     * them, false when they already were (the caller does not tell them again).
     */
    suspend fun claimTold(id: Long, meters: Int): Boolean = dao.markTold(id, meters) > 0

    /** The days' summaries from [fromDay] on, as they are written: what moves a journey. */
    fun observeDaysFrom(fromDay: Long): Flow<Map<Long, Double>> =
        tracking.observeSummaries(fromDay, Long.MAX_VALUE).map { it.distances() }
}

private fun WayJourney.isActiveWay() = state == WayJourneyState.ACTIVE && way.kind == WayKind.WAY

/** The ways' names: among them, one is under way at a time. */
private val WAYS = WayId.entries.filter { it.kind == WayKind.WAY }.map { it.name }

/** Each day's estimated distance in metres, by epoch day: what moves a way. */
fun List<DailySummary>.distances(): Map<Long, Double> = associate { it.localEpochDay to it.distanceMeters }
