package com.callbackdev.passo.core.domain.year

import com.callbackdev.passo.core.domain.insights.Streak
import com.callbackdev.passo.core.domain.insights.Trend
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayKind
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

/** One counted day of the year, for the goal's calendar: its steps against its own goal. */
data class YearDay(val date: LocalDate, val steps: Int, val goalSteps: Int)

/** A month of the year: its steps, and how many of its days were counted (none: not drawn). */
data class MonthTotal(val month: YearMonth, val steps: Long, val countedDays: Int)

/** The year's distance as a way the reader knows: [times] its length, or a [fraction] of it. */
sealed interface WayMeasure {
    val way: WayId

    /** More than the way, [times] over when it is 2 or more. */
    data class MoreThan(override val way: WayId, val times: Int) : WayMeasure

    /** A share of the shortest way, in quarters: 1 a quarter, 2 half, 3 three quarters. */
    data class Part(override val way: WayId, val quarters: Int) : WayMeasure
}

/** What a way gave the year: the distance walked on it, the stamps earned, and if it arrived. */
data class WayYear(val way: WayId, val meters: Double, val stamps: Int, val arrived: Boolean)

/** How the year ends, told from what happened in it (ADR 0010's rule: never a phrase at random). */
sealed interface YearClose {
    data class Arrived(val way: WayId) : YearClose

    data class GoalMostDays(val days: Int) : YearClose

    data class MoreThanLastYear(val lastYear: Int) : YearClose

    data class LongStreak(val days: Int) : YearClose

    data class ManyOutings(val count: Int) : YearClose

    data class EveryStep(val steps: Long) : YearClose
}

/** One page of the year, one thing each (PLANNING.md §11 Phase 12). */
sealed interface YearPage {
    /** The year in steps and in distance (estimated), and that distance as a way. */
    data class Distance(val steps: Long, val distanceMeters: Double, val averageSteps: Int, val asWay: WayMeasure?) :
        YearPage

    /** The months as twelve bars, the best one named (the earlier on a tie). */
    data class Months(val months: List<MonthTotal>, val best: YearMonth) : YearPage

    /** The best day (the earlier on a tie), and the outing or the walk that made it. */
    data class BestDay(val date: LocalDate, val steps: Int, val outing: Session?, val walk: Walk?) : YearPage

    /**
     * The weekday walked most, on average over its counted days, and the hour of the clock with
     * the most steps: the weekly rhythm, once a year.
     *
     * @property byWeekday the average steps of each weekday, Monday first.
     * @property byHour the year's steps in each hour of the clock, midnight first.
     */
    data class Rhythm(val weekday: DayOfWeek, val hour: Int, val byWeekday: List<Int>, val byHour: List<Long>) :
        YearPage

    /** The days the goal was met, the year's longest streak, and the calendar. */
    data class Goal(val daysMet: Int, val longest: Streak, val days: List<YearDay>) : YearPage

    /** The outings: how many, their time in motion, the longest, and how many were interval walks. */
    data class Outings(val count: Int, val movingMillis: Long, val longest: Session, val intervals: Int) : YearPage

    /** The ways walked on this year, and the cities walked. */
    data class Ways(val ways: List<WayYear>, val cities: List<WayId>) : YearPage

    /** Against the year before, as averages per counted day. */
    data class VersusLastYear(
        val lastYear: Int,
        val average: Int,
        val lastAverage: Int,
        val change: Double,
        val trend: Trend,
    ) : YearPage

    /** The close: one sentence chosen from the year. */
    data class Close(val close: YearClose) : YearPage
}

/**
 * Your year on foot (PLANNING.md §11 Phase 12): a year told as pages, from the days already
 * stored. Computed on the phone, on read, never stored.
 *
 * A **counted day** is Insights' (every day from the first recorded one to today, a day without
 * steps counting as zero), within the year. A year counted in part (begun after 1 January, or not
 * over) says so, and is compared with another by its average per counted day, never by totals.
 * Every number is the days' own, the same History adds up for the year. A page with nothing to
 * say is left out.
 *
 * @property countedFrom the first counted day of the year: 1 January, or the first day recorded.
 * @property countedTo the last: 31 December, or today.
 * @property soFar the year is not over.
 */
data class YearInReview(
    val year: Int,
    val countedFrom: LocalDate,
    val countedTo: LocalDate,
    val countedDays: Int,
    val soFar: Boolean,
    val pages: List<YearPage>,
) {
    /** Counted from after 1 January: the pages say since when. */
    val begunLate: Boolean get() = countedFrom != LocalDate.of(year, 1, 1)

    companion object {
        /** A year is offered from this many counted days, and compared with one that has as many. */
        const val MIN_COUNTED_DAYS: Int = 30

        /** The first and last counted days of [year], or null if none of it was counted. */
        fun countedSpan(year: Int, firstRecorded: LocalDate?, today: LocalDate): ClosedRange<LocalDate>? {
            firstRecorded ?: return null
            val from = maxOf(LocalDate.of(year, 1, 1), firstRecorded)
            val to = minOf(LocalDate.of(year, 12, 31), today)
            return if (from.isAfter(to)) null else from..to
        }

        /** How many days of [year] were counted. */
        fun countedDays(year: Int, firstRecorded: LocalDate?, today: LocalDate): Int =
            countedSpan(year, firstRecorded, today)?.let {
                (it.endInclusive.toEpochDay() - it.start.toEpochDay() + 1).toInt()
            }
                ?: 0

        /**
         * The years offered, newest first: each with [MIN_COUNTED_DAYS] counted days or more. The
         * current year is offered as "so far" from its thirtieth counted day.
         */
        fun offeredYears(firstRecorded: LocalDate?, today: LocalDate): List<Int> {
            firstRecorded ?: return emptyList()
            return (today.year downTo firstRecorded.year).filter {
                countedDays(it, firstRecorded, today) >=
                    MIN_COUNTED_DAYS
            }
        }

        /**
         * The year in season (owner, 1 Oct 2026): from 1 December, the year ending; in January,
         * the year just over. Null the rest of the year, or when that year is not offered.
         */
        fun inSeason(firstRecorded: LocalDate?, today: LocalDate): Int? {
            val year = when (today.month) {
                Month.DECEMBER -> today.year
                Month.JANUARY -> today.year - 1
                else -> return null
            }
            return year.takeIf { countedDays(it, firstRecorded, today) >= MIN_COUNTED_DAYS }
        }

        /**
         * The year [year] as pages; null when it is not offered (fewer than [MIN_COUNTED_DAYS]
         * counted days).
         *
         * @param days every recorded day by epoch day, today's with its live count.
         * @param hourSteps the year's steps in each of the clock's 24 hours, from its minutes.
         * @param outings the outings kept, any year: the ones of [year] are taken.
         * @param journeys every way and city walk the reader began.
         * @param walksOn the walks found on a day, for the best day's page when no outing made it.
         */
        fun of(
            year: Int,
            today: LocalDate,
            days: Map<Long, DailySummary>,
            hourSteps: List<Long>,
            outings: List<Session>,
            journeys: List<WayJourney>,
            walksOn: (LocalDate) -> List<Walk> = { emptyList() },
        ): YearInReview? {
            val first = days.keys.minOrNull()?.let(LocalDate::ofEpochDay)
            val span = countedSpan(year, first, today) ?: return null
            val counted = generateSequence(span.start) {
                it.plusDays(1)
            }.takeWhile { !it.isAfter(span.endInclusive) }.toList()
            if (counted.size < MIN_COUNTED_DAYS) return null
            fun day(date: LocalDate) = days[date.toEpochDay()]

            val steps = counted.sumOf { (day(it)?.steps ?: 0).toLong() }
            val distance = counted.sumOf { day(it)?.distanceMeters ?: 0.0 }
            val average = (steps / counted.size).toInt()
            val yearOutings = outings.filter {
                it.state == SessionState.FINISHED && LocalDate.ofEpochDay(it.localEpochDay).year == year
            }

            val pages = buildList {
                add(YearPage.Distance(steps, distance, average, measure(distance)))
                add(months(year, counted, ::day))
                bestDay(counted, ::day, yearOutings, walksOn)?.let(::add)
                rhythm(counted, ::day, hourSteps)?.let(::add)
                add(goal(counted, ::day))
                outingsPage(yearOutings)?.let(::add)
                waysPage(year, today, days, journeys, yearOutings)?.let(::add)
                versus(year, today, first, days, average)?.let(::add)
            }
            return YearInReview(
                year = year,
                countedFrom = span.start,
                countedTo = span.endInclusive,
                countedDays = counted.size,
                soFar = today.year == year && today.isBefore(LocalDate.of(year, 12, 31)),
                pages = pages + YearPage.Close(close(pages, steps)),
            )
        }

        /**
         * The year's distance as a way: the longest way it covers, or a share of the shortest.
         * Null for no distance at all.
         */
        internal fun measure(distanceMeters: Double): WayMeasure? {
            if (distanceMeters <= 0.0) return null
            val ways = Ways.all.sortedBy { it.lengthMeters }
            val covered = ways.lastOrNull { it.lengthMeters <= distanceMeters }
            if (covered != null) {
                return WayMeasure.MoreThan(covered.id, (distanceMeters / covered.lengthMeters).toInt())
            }
            val shortest = ways.first()
            val quarters = (distanceMeters / shortest.lengthMeters * QUARTERS).toInt()
            return if (quarters >= 1) WayMeasure.Part(shortest.id, quarters) else null
        }

        private fun months(year: Int, counted: List<LocalDate>, day: (LocalDate) -> DailySummary?): YearPage.Months {
            val byMonth = counted.groupBy { YearMonth.from(it) }
            val months = (1..MONTHS).map { number ->
                val month = YearMonth.of(year, number)
                val dates = byMonth[month].orEmpty()
                MonthTotal(month, dates.sumOf { (day(it)?.steps ?: 0).toLong() }, dates.size)
            }
            // The earlier on a tie: maxBy keeps the first of equals.
            val best = months.filter { it.countedDays > 0 }.maxBy { it.steps }.month
            return YearPage.Months(months, best)
        }

        private fun bestDay(
            counted: List<LocalDate>,
            day: (LocalDate) -> DailySummary?,
            outings: List<Session>,
            walksOn: (LocalDate) -> List<Walk>,
        ): YearPage.BestDay? {
            val best = counted.maxBy { day(it)?.steps ?: 0 }
            val steps = day(best)?.steps ?: 0
            if (steps <= 0) return null
            val outing = outings.filter { it.localEpochDay == best.toEpochDay() }.maxByOrNull { it.totals.steps }
            val walk = if (outing == null) walksOn(best).maxByOrNull { it.steps } else null
            return YearPage.BestDay(best, steps, outing, walk)
        }

        private fun rhythm(
            counted: List<LocalDate>,
            day: (LocalDate) -> DailySummary?,
            hourSteps: List<Long>,
        ): YearPage.Rhythm? {
            if (hourSteps.size != HOURS || hourSteps.all { it <= 0 }) return null
            val byWeekday = DayOfWeek.entries.map { weekday ->
                val dates = counted.filter { it.dayOfWeek == weekday }
                if (dates.isEmpty()) 0 else (dates.sumOf { (day(it)?.steps ?: 0).toLong() } / dates.size).toInt()
            }
            val weekday = DayOfWeek.entries[byWeekday.indices.maxBy { byWeekday[it] }]
            val hour = hourSteps.indices.maxBy { hourSteps[it] }
            return YearPage.Rhythm(weekday, hour, byWeekday, hourSteps)
        }

        private fun goal(counted: List<LocalDate>, day: (LocalDate) -> DailySummary?): YearPage.Goal {
            var longest = Streak.NONE
            var start: LocalDate? = null
            var run = 0
            var met = 0
            for (date in counted) {
                if (day(date)?.goalReached == true) {
                    met++
                    if (run == 0) start = date
                    run++
                    if (run > longest.days) longest = Streak(run, start, date)
                } else {
                    run = 0
                }
            }
            val days = counted.map { date ->
                val summary = day(date)
                YearDay(date, summary?.steps ?: 0, summary?.goalSteps ?: 0)
            }
            return YearPage.Goal(met, longest, days)
        }

        private fun outingsPage(outings: List<Session>): YearPage.Outings? {
            if (outings.isEmpty()) return null
            val longest = outings.maxBy { it.totals.movingMillis }
            return YearPage.Outings(
                count = outings.size,
                movingMillis = outings.sumOf { it.totals.movingMillis },
                longest = longest,
                intervals = outings.count { it.goalKind == SessionGoalKind.INTERVALS },
            )
        }

        private fun waysPage(
            year: Int,
            today: LocalDate,
            days: Map<Long, DailySummary>,
            journeys: List<WayJourney>,
            outings: List<Session>,
        ): YearPage.Ways? {
            val distances = days.mapValues { it.value.distanceMeters }
            val yearStart = LocalDate.of(year, 1, 1).toEpochDay()
            val yearEnd = minOf(LocalDate.of(year, 12, 31), today).toEpochDay()
            val ways = journeys.filter { it.way.kind == WayKind.WAY && it.startEpochDay <= yearEnd }
                .mapNotNull { journey ->
                    val way = Ways.of(journey.way)
                    val atEnd = WayProgress.of(way, journey, distances, yearEnd)
                    val before = if (journey.startEpochDay < yearStart) {
                        WayProgress.of(way, journey, distances, yearStart - 1)
                    } else {
                        null
                    }
                    val meters = atEnd.walkedMeters - (before?.walkedMeters ?: 0.0)
                    // The start's stamp is a stage reached by beginning, not by walking.
                    val stamps = atEnd.stamps.count {
                        it.epochDay in yearStart..yearEnd && it.stop != way.stops.first()
                    }
                    val arrived = atEnd.finishedEpochDay?.let { it in yearStart..yearEnd } == true
                    WayYear(journey.way, meters, stamps, arrived).takeIf { meters > 0.0 || stamps > 0 || arrived }
                }
                .groupBy { it.way }
                .map { (way, parts) ->
                    WayYear(way, parts.sumOf { it.meters }, parts.sumOf { it.stamps }, parts.any { it.arrived })
                }
                .sortedByDescending { it.meters }
            val cities = outings.mapNotNull { it.walk }.distinct()
            if (ways.isEmpty() && cities.isEmpty()) return null
            return YearPage.Ways(ways, cities)
        }

        private fun versus(
            year: Int,
            today: LocalDate,
            first: LocalDate?,
            days: Map<Long, DailySummary>,
            average: Int,
        ): YearPage.VersusLastYear? {
            val last = year - 1
            val span = countedSpan(last, first, today) ?: return null
            val counted = (span.endInclusive.toEpochDay() - span.start.toEpochDay() + 1).toInt()
            if (counted < MIN_COUNTED_DAYS) return null
            val steps = (span.start.toEpochDay()..span.endInclusive.toEpochDay()).sumOf {
                (days[it]?.steps ?: 0).toLong()
            }
            val lastAverage = (steps / counted).toInt()
            if (lastAverage <= 0) return null
            val change = average.toDouble() / lastAverage - 1
            return YearPage.VersusLastYear(last, average, lastAverage, change, Trend.of(change))
        }

        /** The close, from the facts in order: an arrival, the goal most days, more than last year, a long streak, many outings. */
        private fun close(pages: List<YearPage>, steps: Long): YearClose {
            pages.filterIsInstance<YearPage.Ways>().firstOrNull()?.ways?.firstOrNull { it.arrived }?.let {
                return YearClose.Arrived(it.way)
            }
            val goal = pages.filterIsInstance<YearPage.Goal>().first()
            if (goal.daysMet * 2 >= goal.days.size) return YearClose.GoalMostDays(goal.daysMet)
            pages.filterIsInstance<YearPage.VersusLastYear>().firstOrNull()?.takeIf { it.trend == Trend.UP }?.let {
                return YearClose.MoreThanLastYear(it.lastYear)
            }
            if (goal.longest.days >= LONG_STREAK_DAYS) return YearClose.LongStreak(goal.longest.days)
            pages.filterIsInstance<YearPage.Outings>().firstOrNull()?.takeIf { it.count >= MANY_OUTINGS }?.let {
                return YearClose.ManyOutings(it.count)
            }
            return YearClose.EveryStep(steps)
        }

        private const val MONTHS = 12
        private const val HOURS = 24
        private const val QUARTERS = 4

        /** A week at the goal without a gap is worth the last word. */
        private const val LONG_STREAK_DAYS = 7

        /** Ten outings: about one every five weeks, a habit rather than a try. */
        private const val MANY_OUTINGS = 10
    }
}
