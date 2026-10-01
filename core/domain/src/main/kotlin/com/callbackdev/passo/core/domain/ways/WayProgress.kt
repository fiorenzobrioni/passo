package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.min

/** A stop the reader has reached, and the day the distance got there. */
data class StopReached(val stop: WayStop, val epochDay: Long)

/**
 * Where the reader stands on a way (PLANNING.md §11 Phase 11), from the days' own estimated
 * distances since the journey's start: each finished day's frozen one and today's. Computed on
 * read and never stored, so History, Insights and the way always agree, and "Apply profile to
 * past data" or an import move it with everything else.
 *
 * @property walkedMeters the distance walked on the way, at most its length.
 * @property reached the stops reached, in order, each with its day; the first stop on the
 *   start day, even with nothing walked.
 * @property finishedEpochDay the day the end was reached, if it was.
 */
class WayProgress(
    val way: Way,
    val journey: WayJourney,
    val walkedMeters: Double,
    val reached: List<StopReached>,
    val finishedEpochDay: Long?,
) {
    val finished: Boolean get() = finishedEpochDay != null

    val metersLeft: Double get() = way.lengthMeters - walkedMeters

    val fraction: Double get() = (walkedMeters / way.lengthMeters).coerceIn(0.0, 1.0)

    /** The last stop reached: where the reader is, or has just passed. */
    val last: StopReached get() = reached.last()

    /** The next stop, of either kind; null once the way is finished. */
    val next: WayStop? get() = way.stops.getOrNull(reached.size)

    /** The next stage, skipping the places between; null once the way is finished. */
    val nextStage: WayStop? get() = way.stops.drop(reached.size).firstOrNull { it.stage }

    /** The stages reached: the stamps in the credential. */
    val stamps: List<StopReached> get() = reached.filter { it.stop.stage }

    fun metersTo(stop: WayStop): Double = (stop.distanceMeters - walkedMeters).coerceAtLeast(0.0)

    companion object {
        /**
         * [distances] is each day's estimated distance in metres, by epoch day; days without a
         * row walked nothing. A journey under way counts up to [today]; one finished or left
         * counts up to the day it ended, so a finished way does not keep moving.
         */
        fun of(way: Way, journey: WayJourney, distances: Map<Long, Double>, today: Long): WayProgress {
            val last = when (journey.state) {
                WayJourneyState.ACTIVE -> today
                WayJourneyState.FINISHED, WayJourneyState.LEFT -> min(journey.endedEpochDay ?: today, today)
            }
            val stops = way.stops
            val reached = mutableListOf(StopReached(stops.first(), journey.startEpochDay))
            var walked = 0.0
            var finishedOn: Long? = null
            val days = distances.keys.filter { it in journey.startEpochDay..last }.sorted()
            for (day in days) {
                walked += distances.getValue(day).coerceAtLeast(0.0)
                while (reached.size < stops.size && stops[reached.size].distanceMeters <= walked) {
                    reached += StopReached(stops[reached.size], day)
                }
                if (walked >= way.lengthMeters) {
                    finishedOn = day
                    break
                }
            }
            // A way finished stays finished: should its days be measured shorter since (the
            // profile applied to past data), the arrival is not taken back. Its last stops are
            // reached on the day it ended, as they were then.
            val ended = journey.endedEpochDay
            if (journey.state == WayJourneyState.FINISHED && finishedOn == null && ended != null) {
                while (reached.size < stops.size) reached += StopReached(stops[reached.size], ended)
                walked = way.lengthMeters.toDouble()
                finishedOn = ended
            }
            return WayProgress(
                way = way,
                journey = journey,
                walkedMeters = walked.coerceAtMost(way.lengthMeters.toDouble()),
                reached = reached,
                finishedEpochDay = finishedOn,
            )
        }
    }
}

/**
 * When the reader would arrive at their usual pace: the average estimated distance a day over
 * the last [WINDOW_DAYS] days before today (today is not over), counting only the days Passo
 * counted. An estimate, and said so; none with fewer than [MIN_DAYS] days to go on, or a pace
 * too slow to promise anything.
 */
data class WayForecast(val metersPerDay: Double, val daysLeft: Int, val arrival: LocalDate) {
    companion object {
        const val WINDOW_DAYS = 28
        const val MIN_DAYS = 7

        /** Under 100 m a day the arrival would be years away: no forecast. */
        private const val MIN_METERS_PER_DAY = 100.0

        fun of(distances: Map<Long, Double>, today: LocalDate, metersLeft: Double): WayForecast? {
            if (metersLeft <= 0.0) return null
            val perDay = pace(distances, today) ?: return null
            val days = ceil(metersLeft / perDay).toInt()
            return WayForecast(perDay, days, today.plus(days.toLong(), ChronoUnit.DAYS))
        }

        /** The reader's usual distance a day, as the forecast reads it; null when it cannot say. */
        fun pace(distances: Map<Long, Double>, today: LocalDate): Double? {
            val end = today.toEpochDay() - 1
            val start = end - WINDOW_DAYS + 1
            val counted = distances.filterKeys { it in start..end }.values
            if (counted.size < MIN_DAYS) return null
            val perDay = counted.sumOf { it.coerceAtLeast(0.0) } / counted.size
            return perDay.takeIf { it >= MIN_METERS_PER_DAY }
        }

        /** Days to walk [meters] at [perDay], at least one. */
        fun daysFor(meters: Double, perDay: Double): Int = ceil(meters / perDay).toInt().coerceAtLeast(1)
    }
}

/**
 * The stage to tell (PLANNING.md §11 Phase 11): the furthest stage passed beyond [toldMeters],
 * never the first (the start is no news), never a place between stages. Several passed at once,
 * by one batch of steps or a day written late, are told as the furthest.
 */
object WayAnnouncement {
    fun next(way: Way, walkedMeters: Double, toldMeters: Int): WayStop? = way.stages
        .drop(1)
        .lastOrNull { it.distanceMeters > toldMeters && it.distanceMeters <= walkedMeters }
}

/** Where a journey can start from, on the start sheet. */
enum class WayStartChoice {
    TODAY,

    /** 1 January of this year. */
    THIS_YEAR,

    /** The first day Passo counted. */
    FIRST_DAY,

    /** A day the reader picks. */
    CHOSEN,
}

object WayStarts {
    /**
     * The start day of [choice]: never after today, never before the first day counted (a day
     * with no data would add nothing, and the start would read as a date the reader was not
     * walking with Passo). [firstCounted] is null before the first day is written.
     */
    fun dayOf(choice: WayStartChoice, today: LocalDate, firstCounted: LocalDate?, chosen: LocalDate?): LocalDate {
        val day = when (choice) {
            WayStartChoice.TODAY -> today
            WayStartChoice.THIS_YEAR -> today.withDayOfYear(1)
            WayStartChoice.FIRST_DAY -> firstCounted ?: today
            WayStartChoice.CHOSEN -> chosen ?: today
        }
        val earliest = firstCounted ?: today
        return day.coerceIn(minOf(earliest, today), today)
    }
}
