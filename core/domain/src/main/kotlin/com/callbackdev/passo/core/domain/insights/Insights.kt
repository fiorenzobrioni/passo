package com.callbackdev.passo.core.domain.insights

import com.callbackdev.passo.core.domain.history.Period
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.model.DailySummary
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

/** Consecutive days with the goal met; [days] 0 means none, with no dates. */
data class Streak(val days: Int, val start: LocalDate?, val end: LocalDate?) {
    companion object {
        val NONE = Streak(0, null, null)
    }
}

/** A record: the steps of the best day, week or month, and when. */
data class Record(val steps: Long, val start: LocalDate, val end: LocalDate)

/** How the last seven days compare with the seven before. */
enum class Trend {
    UP,
    DOWN,
    STEADY,
}

/** The one sentence Insights opens with. */
sealed interface InsightsHeadline {
    /** No day recorded yet. */
    data object NothingYet : InsightsHeadline

    /** Today is already the best day on record. */
    data class BestDayToday(val steps: Int) : InsightsHeadline

    /** [days] days in a row at the goal; [includesToday] once today has joined. */
    data class OnAStreak(val days: Int, val includesToday: Boolean) : InsightsHeadline

    /** The last seven complete days against the seven before. */
    data class LastWeek(val average: Int, val trend: Trend, val change: Double) : InsightsHeadline

    /** Not enough days for a comparison yet: the average so far. */
    data class Average(val average: Int, val days: Int) : InsightsHeadline
}

/**
 * Everything Insights shows (PLANNING.md §6, §11 Phase 5): streaks, records, averages and
 * lifetime totals, from the recorded days.
 *
 * A **counted day** is any day from the first recorded one to today: one with no steps counts
 * as zero, because that is what it was. Every goal is the goal of its own day, so a streak or
 * a record never changes because the goal did.
 *
 * @property currentStreak the goal met every day up to yesterday, and today too once met: today
 *   not yet at the goal does not break a streak, it just has not joined it.
 * @property averageLast7 the daily average of the last seven complete days (today is not over,
 *   and would pull it down every morning); null until there are seven.
 * @property averagePrevious7 the seven days before those; null until there are fourteen.
 * @property averageLast30 the same over thirty days; null until there are thirty.
 * @property averageAll every counted day, today included; null before the first.
 */
data class Insights(
    val headline: InsightsHeadline,
    val currentStreak: Streak,
    val todayInStreak: Boolean,
    val longestStreak: Streak,
    val bestDay: Record?,
    val bestWeek: Record?,
    val bestMonth: Record?,
    val averageLast7: Int?,
    val averagePrevious7: Int?,
    val averageLast30: Int?,
    val averageAll: Int?,
    val totalSteps: Long,
    val totalDistanceMeters: Double,
    val totalActiveKcal: Double,
    val countedDays: Int,
    val goalDays: Int,
    val firstDay: LocalDate?,
) {
    companion object {
        /**
         * @param days the recorded days by epoch day, today's with its live count if a screen
         *   has it.
         */
        fun of(days: Map<Long, DailySummary>, today: LocalDate, firstDayOfWeek: DayOfWeek): Insights {
            val first = days.keys.minOrNull()?.let(LocalDate::ofEpochDay)?.takeIf { !it.isAfter(today) }
                ?: return empty()
            val all = generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.toList()
            fun steps(date: LocalDate) = days[date.toEpochDay()]?.steps ?: 0
            fun met(date: LocalDate) = days[date.toEpochDay()]?.goalReached == true

            // Streaks: one pass, oldest first.
            var longest = Streak.NONE
            var runStart: LocalDate? = null
            var runDays = 0
            for (date in all) {
                if (met(date)) {
                    if (runDays == 0) runStart = date
                    runDays++
                    if (runDays > longest.days) longest = Streak(runDays, runStart, date)
                } else {
                    runDays = 0
                }
            }
            val todayMet = met(today)
            val streakEnd = if (todayMet) today else today.minusDays(1)
            var current = 0
            var cursor = streakEnd
            while (!cursor.isBefore(first) && met(cursor)) {
                current++
                cursor = cursor.minusDays(1)
            }
            val currentStreak = if (current ==
                0
            ) {
                Streak.NONE
            } else {
                Streak(current, streakEnd.minusDays(current - 1L), streakEnd)
            }

            // Records.
            val bestDay = all.filter { steps(it) > 0 }
                .maxWithOrNull(compareBy<LocalDate> { steps(it) }.thenBy { it })
                ?.let { Record(steps(it).toLong(), it, it) }
            val bestWeek = bestPeriod(all, PeriodScale.WEEK, firstDayOfWeek, ::steps)
            val bestMonth = bestPeriod(all, PeriodScale.MONTH, firstDayOfWeek, ::steps)

            // Averages over complete days: yesterday backwards.
            val complete = all.filter { it.isBefore(today) }
            fun averageOf(window: List<LocalDate>) = window.sumOf { steps(it).toLong() }.toDouble() / window.size
            val last7 = complete.takeLast(WEEK).takeIf { it.size == WEEK }
            val previous7 = complete.dropLast(WEEK).takeLast(WEEK).takeIf { it.size == WEEK }
            val last30 = complete.takeLast(MONTH_DAYS).takeIf { it.size == MONTH_DAYS }

            val totalSteps = all.sumOf { steps(it).toLong() }
            val averageAll = (totalSteps.toDouble() / all.size).roundToInt()
            val averageLast7 = last7?.let { averageOf(it) }
            val averagePrevious7 = previous7?.let { averageOf(it) }

            val previousBest = all.filter { it != today }.maxOfOrNull { steps(it) } ?: 0
            val headline = when {
                all.size > WEEK && steps(today) > previousBest && previousBest > 0 ->
                    InsightsHeadline.BestDayToday(steps(today))

                current >= MIN_STREAK_TO_TELL -> InsightsHeadline.OnAStreak(current, todayMet)

                averageLast7 != null && averagePrevious7 != null && averagePrevious7 > 0 -> {
                    val change = (averageLast7 - averagePrevious7) / averagePrevious7
                    val trend = when {
                        change >= STEADY_BAND -> Trend.UP
                        change <= -STEADY_BAND -> Trend.DOWN
                        else -> Trend.STEADY
                    }
                    InsightsHeadline.LastWeek(averageLast7.roundToInt(), trend, change)
                }

                else -> InsightsHeadline.Average(averageAll, all.size)
            }

            return Insights(
                headline = headline,
                currentStreak = currentStreak,
                todayInStreak = todayMet,
                longestStreak = longest,
                bestDay = bestDay,
                bestWeek = bestWeek,
                bestMonth = bestMonth,
                averageLast7 = averageLast7?.roundToInt(),
                averagePrevious7 = averagePrevious7?.roundToInt(),
                averageLast30 = last30?.let { averageOf(it).roundToInt() },
                averageAll = averageAll,
                totalSteps = totalSteps,
                totalDistanceMeters = all.sumOf { days[it.toEpochDay()]?.distanceMeters ?: 0.0 },
                totalActiveKcal = all.sumOf { days[it.toEpochDay()]?.activeKcal ?: 0.0 },
                countedDays = all.size,
                goalDays = all.count(::met),
                firstDay = first,
            )
        }

        private fun bestPeriod(
            all: List<LocalDate>,
            scale: PeriodScale,
            firstDayOfWeek: DayOfWeek,
            steps: (LocalDate) -> Int,
        ): Record? = all.groupBy { Period.containing(scale, it, firstDayOfWeek) }
            .map { (period, dates) -> period to dates.sumOf { steps(it).toLong() } }
            .filter { it.second > 0 }
            .maxWithOrNull(compareBy<Pair<Period, Long>> { it.second }.thenBy { it.first.start })
            ?.let { (period, total) -> Record(total, period.start, period.end) }

        private fun empty() = Insights(
            headline = InsightsHeadline.NothingYet,
            currentStreak = Streak.NONE,
            todayInStreak = false,
            longestStreak = Streak.NONE,
            bestDay = null,
            bestWeek = null,
            bestMonth = null,
            averageLast7 = null,
            averagePrevious7 = null,
            averageLast30 = null,
            averageAll = null,
            totalSteps = 0,
            totalDistanceMeters = 0.0,
            totalActiveKcal = 0.0,
            countedDays = 0,
            goalDays = 0,
            firstDay = null,
        )

        private const val WEEK = 7
        private const val MONTH_DAYS = 30

        /** One day at the goal is a day, not a streak: the sentence speaks of one from two. */
        private const val MIN_STREAK_TO_TELL = 2

        /** Within 5% either way the week is "about the same": a smaller difference is noise. */
        private const val STEADY_BAND = 0.05
    }
}
