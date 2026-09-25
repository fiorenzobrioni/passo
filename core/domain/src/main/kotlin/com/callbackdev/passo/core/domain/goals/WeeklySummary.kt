package com.callbackdev.passo.core.domain.goals

import com.callbackdev.passo.core.domain.history.Period
import com.callbackdev.passo.core.domain.history.PeriodOverview
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.domain.insights.Trend
import com.callbackdev.passo.core.model.DailySummary
import java.time.DayOfWeek
import java.time.LocalDate

/** The sentence the weekly summary opens with: the one thing worth knowing about the week. */
sealed interface WeekHeadline {
    /** The goal met on every day of a whole week. */
    data object EveryDay : WeekHeadline

    /** More, fewer or about as many steps a day as the week before. */
    data class VersusWeekBefore(val trend: Trend, val change: Double) : WeekHeadline

    /** No week before to compare with (the first one counted): the days at the goal. */
    data class GoalDays(val goalDays: Int, val countedDays: Int) : WeekHeadline
}

/**
 * The weekly summary notification (VISION.md, goals): the week that has just ended, in the
 * numbers History would show for it, so the notification and the app never disagree.
 *
 * @property countedDays the days of the week that were counted: all seven, except in the week
 *   counting began.
 * @property dailyAverage steps per counted day.
 * @property bestDay the week's best day and its steps; null would mean no steps, and then there
 *   is no summary at all.
 * @property distanceMeters and [activeKcal]: estimates, as each day froze them.
 */
data class WeeklySummary(
    val week: Period,
    val steps: Long,
    val countedDays: Int,
    val goalDays: Int,
    val dailyAverage: Int,
    val previousAverage: Int?,
    val bestDay: LocalDate,
    val bestDaySteps: Int,
    val distanceMeters: Double,
    val activeKcal: Double,
    val headline: WeekHeadline,
) {
    companion object {
        /**
         * The summary of the week before the one holding [today], from the recorded [days] by
         * epoch day; null when nothing was walked in it (before counting began, or a week
         * paused from start to end): a summary of nothing is not sent.
         */
        fun ofLastWeek(
            days: Map<Long, DailySummary>,
            today: LocalDate,
            firstDayOfWeek: DayOfWeek,
            firstRecordedDay: LocalDate?,
            currentGoal: Int,
        ): WeeklySummary? {
            val week = Period.containing(PeriodScale.WEEK, today, firstDayOfWeek).previous()
            val overview = PeriodOverview.of(week, days, today, firstRecordedDay, currentGoal)
            val best = overview.bestBar?.let { overview.bars[it] } ?: return null
            val average = overview.dailyAverage ?: return null
            val change = overview.change
            val headline = when {
                overview.countedDays == DAYS_IN_WEEK && overview.goalDays == DAYS_IN_WEEK -> WeekHeadline.EveryDay
                change != null -> WeekHeadline.VersusWeekBefore(Trend.of(change), change)
                else -> WeekHeadline.GoalDays(overview.goalDays, overview.countedDays)
            }
            return WeeklySummary(
                week = week,
                steps = overview.totals.steps,
                countedDays = overview.countedDays,
                goalDays = overview.goalDays,
                dailyAverage = average,
                previousAverage = overview.previousAverage,
                bestDay = best.start,
                bestDaySteps = best.steps ?: 0,
                distanceMeters = overview.totals.distanceMeters,
                activeKcal = overview.totals.activeKcal,
                headline = headline,
            )
        }

        private const val DAYS_IN_WEEK = 7
    }
}
