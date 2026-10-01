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

    /** The journey under way, if there is one. */
    val active: Flow<WayJourney?> = dao.observeActive().map { it?.toModel() }

    suspend fun activeNow(): WayJourney? = dao.active()?.toModel()

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
        )
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

/** Each day's estimated distance in metres, by epoch day: what moves a way. */
fun List<DailySummary>.distances(): Map<Long, Double> = associate { it.localEpochDay to it.distanceMeters }
