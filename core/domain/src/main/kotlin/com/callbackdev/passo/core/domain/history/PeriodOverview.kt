package com.callbackdev.passo.core.domain.history

import com.callbackdev.passo.core.model.DailySummary
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * One bar of a week, month or year chart: a day, or in a year a month.
 *
 * @property steps the day's steps, or in a year the month's daily average; null where there is
 *   nothing to draw: a day still to come, or from before counting began.
 * @property goal the goal in effect that day (each day keeps its own, PLANNING.md §5), or in a
 *   year the month's average goal.
 * @property met the day's goal was met; in a year, the month's average reached its average goal.
 * @property goalDays the days of the bar with their goal met: 0 or 1 for a day.
 * @property current the bar holds today, and so is still moving.
 */
data class PeriodBar(
    val start: LocalDate,
    val end: LocalDate,
    val steps: Int?,
    val goal: Int,
    val met: Boolean,
    val goalDays: Int,
    val current: Boolean,
)

/** What the days of a period add up to. Estimates as each day froze them (PLANNING.md §5). */
data class PeriodTotals(
    val steps: Long,
    val distanceMeters: Double,
    val activeKcal: Double,
    val activeMinutes: Int,
    val briskMinutes: Int,
) {
    companion object {
        val ZERO = PeriodTotals(0, 0.0, 0.0, 0, 0)
    }
}

/**
 * A week, a month or a year of History (PLANNING.md §11 Phase 5): its bars, its totals, and
 * the few facts its sentence is made of.
 *
 * @property countedDays the days of the period that were counted: from the first recorded day
 *   to today, both included. A day in there with no steps counts as a day of zero, as it was.
 * @property goalDays the counted days with the goal met.
 * @property dailyAverage the period's steps per counted day; null with none counted.
 * @property previousAverage the same for the period before; null with none counted there.
 * @property inProgress the period holds today, so its numbers are "so far".
 */
data class PeriodOverview(
    val period: Period,
    val bars: List<PeriodBar>,
    val totals: PeriodTotals,
    val countedDays: Int,
    val goalDays: Int,
    val dailyAverage: Int?,
    val previousAverage: Int?,
    val inProgress: Boolean,
) {
    /** The daily average against the previous period's, as a share: 0.12 is 12% more. */
    val change: Double?
        get() {
            val now = dailyAverage ?: return null
            val before = previousAverage?.takeIf { it > 0 } ?: return null
            return (now - before).toDouble() / before
        }

    /** The highest bar, the one a chart can mark; null when nothing was walked. */
    val bestBar: Int?
        get() = bars.indices.filter { (bars[it].steps ?: 0) > 0 }.maxByOrNull { bars[it].steps ?: 0 }

    companion object {
        /**
         * The overview of [period] (a week, a month or a year) from the recorded [days], keyed
         * by epoch day. [currentGoal] stands in for the goal of a day that has no record.
         */
        fun of(
            period: Period,
            days: Map<Long, DailySummary>,
            today: LocalDate,
            firstRecordedDay: LocalDate?,
            currentGoal: Int,
        ): PeriodOverview {
            require(period.scale != PeriodScale.DAY) { "A day is not an overview: it has its own page" }
            val counted = countedRange(period, today, firstRecordedDay)
            val bars = if (period.scale == PeriodScale.YEAR) {
                (0 until MONTHS).map { month ->
                    val start = period.start.plusMonths(month.toLong())
                    monthBar(start, start.plusMonths(1).minusDays(1), days, counted, today, currentGoal)
                }
            } else {
                dates(period.start, period.end).map { dayBar(it, days, counted, today, currentGoal) }.toList()
            }
            var totals = PeriodTotals.ZERO
            var goalDays = 0
            val countedDays = counted?.let { dates(it.start, it.endInclusive).count() } ?: 0
            counted?.let { range ->
                for (date in dates(range.start, range.endInclusive)) {
                    val day = days[date.toEpochDay()] ?: continue
                    totals = totals.plus(day)
                    if (day.goalReached) goalDays++
                }
            }
            return PeriodOverview(
                period = period,
                bars = bars,
                totals = totals,
                countedDays = countedDays,
                goalDays = goalDays,
                dailyAverage = average(totals.steps, countedDays),
                previousAverage = previousAverage(period.previous(), days, today, firstRecordedDay),
                inProgress = today in period,
            )
        }

        private fun previousAverage(
            period: Period,
            days: Map<Long, DailySummary>,
            today: LocalDate,
            firstRecordedDay: LocalDate?,
        ): Int? {
            val range = countedRange(period, today, firstRecordedDay) ?: return null
            val all = dates(range.start, range.endInclusive)
            return average(all.sumOf { days[it.toEpochDay()]?.steps?.toLong() ?: 0L }, all.count())
        }

        private fun dayBar(
            date: LocalDate,
            days: Map<Long, DailySummary>,
            counted: ClosedRange<LocalDate>?,
            today: LocalDate,
            currentGoal: Int,
        ): PeriodBar {
            val day = days[date.toEpochDay()]
            val inRange = counted != null && date in counted
            val steps = if (inRange) day?.steps ?: 0 else null
            val met = day?.goalReached == true && inRange
            return PeriodBar(
                start = date,
                end = date,
                steps = steps,
                goal = day?.goalSteps ?: currentGoal,
                met = met,
                goalDays = if (met) 1 else 0,
                current = date == today,
            )
        }

        private fun monthBar(
            start: LocalDate,
            end: LocalDate,
            days: Map<Long, DailySummary>,
            counted: ClosedRange<LocalDate>?,
            today: LocalDate,
            currentGoal: Int,
        ): PeriodBar {
            val from = counted?.start?.let { maxOf(it, start) }
            val to = counted?.endInclusive?.let { minOf(it, end) }
            if (from == null || to == null || from.isAfter(to)) {
                return PeriodBar(
                    start,
                    end,
                    null,
                    currentGoal,
                    met = false,
                    goalDays = 0,
                    current = today in start..end,
                )
            }
            val records = dates(from, to).mapNotNull { days[it.toEpochDay()] }.toList()
            val countedDays = ChronoUnit.DAYS.between(from, to).toInt() + 1
            val steps = average(records.sumOf { it.steps.toLong() }, countedDays) ?: 0
            val goal = if (records.isEmpty()) currentGoal else records.map { it.goalSteps }.average().roundToInt()
            return PeriodBar(
                start = start,
                end = end,
                steps = steps,
                goal = goal,
                met = steps >= goal,
                goalDays = records.count { it.goalReached },
                current = today in start..end,
            )
        }

        /** The days of [period] that were counted, or null when none were. */
        private fun countedRange(period: Period, today: LocalDate, first: LocalDate?): ClosedRange<LocalDate>? {
            first ?: return null
            val from = maxOf(period.start, first)
            val to = minOf(period.end, today)
            return if (from.isAfter(to)) null else from..to
        }

        private fun average(total: Long, days: Int): Int? = if (days <=
            0
        ) {
            null
        } else {
            (total.toDouble() / days).roundToInt()
        }

        private const val MONTHS = 12
    }
}

internal fun dates(from: LocalDate, to: LocalDate): Sequence<LocalDate> =
    generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }

private fun PeriodTotals.plus(day: DailySummary) = PeriodTotals(
    steps = steps + day.steps,
    distanceMeters = distanceMeters + day.distanceMeters,
    activeKcal = activeKcal + day.activeKcal,
    activeMinutes = activeMinutes + day.activeMinutes,
    briskMinutes = briskMinutes + day.briskMinutes,
)
