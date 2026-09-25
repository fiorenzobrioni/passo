package com.callbackdev.passo.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.callbackdev.passo.core.designsystem.components.duration
import com.callbackdev.passo.core.designsystem.format.datePattern
import com.callbackdev.passo.core.designsystem.format.longDate
import com.callbackdev.passo.core.designsystem.format.monthYear
import com.callbackdev.passo.core.designsystem.format.shortDate
import com.callbackdev.passo.core.designsystem.format.shortDateWithYear
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.history.Period
import com.callbackdev.passo.core.domain.history.PeriodOverview
import com.callbackdev.passo.core.domain.history.PeriodScale
import java.time.LocalDate
import kotlin.math.abs

/** The period's name as the navigator shows it: «This week», or «14 – 20 Sep» for an older one. */
@Composable
internal fun periodTitle(period: Period, today: LocalDate): String = when (period.scale) {
    PeriodScale.DAY -> when (period.start) {
        today -> stringResource(R.string.history_today)
        today.minusDays(1) -> stringResource(R.string.history_yesterday)
        else -> longDate(period.start)
    }

    PeriodScale.WEEK -> when {
        today in period -> stringResource(R.string.history_this_week)
        today.minusWeeks(1) in period -> stringResource(R.string.history_last_week)
        else -> dateRange(period, today)
    }

    PeriodScale.MONTH -> monthYear(period.start)

    PeriodScale.YEAR -> period.start.year.toString()
}

/** The line under the title: the dates a relative name stands for, or the year of an old day. */
@Composable
internal fun periodSubtitle(period: Period, today: LocalDate): String? = when (period.scale) {
    PeriodScale.DAY -> when {
        period.start == today || period.start == today.minusDays(1) -> longDate(period.start)
        period.start.year != today.year -> period.start.year.toString()
        else -> null
    }

    PeriodScale.WEEK -> when {
        today in period || today.minusWeeks(1) in period -> dateRange(period, today)
        period.start.year != today.year -> period.start.year.toString()
        else -> null
    }

    PeriodScale.MONTH -> if (today in period) stringResource(R.string.history_this_month) else null

    PeriodScale.YEAR -> if (today in period) stringResource(R.string.history_this_year) else null
}

@Composable
private fun dateRange(period: Period, today: LocalDate): String {
    val sameYear = period.start.year == today.year && period.end.year == today.year
    return stringResource(
        R.string.history_range,
        if (sameYear) shortDate(period.start) else shortDateWithYear(period.start),
        if (sameYear) shortDate(period.end) else shortDateWithYear(period.end),
    )
}

/** A day on a bar or in a readout: «Tue 23 Sep». */
@Composable
internal fun barDay(date: LocalDate): String = datePattern(date, "EEEdMMM")

/** The sentence a day's page opens with. */
@Composable
internal fun dayHeadline(detail: DayDetail, format: MeasureFormatter): String = when {
    detail.steps <= 0 && detail.isToday -> stringResource(R.string.history_day_no_steps_today)

    detail.steps <= 0 -> stringResource(R.string.history_day_no_steps)

    detail.goalReached -> pluralStringResource(R.plurals.history_day_met, detail.steps, format.steps(detail.steps))

    detail.isToday -> {
        val left = detail.goalSteps - detail.steps
        pluralStringResource(R.plurals.history_day_to_go, detail.steps, format.steps(detail.steps), format.steps(left))
    }

    else -> {
        val short = detail.goalSteps - detail.steps
        pluralStringResource(R.plurals.history_day_short, detail.steps, format.steps(detail.steps), format.steps(short))
    }
}

/** The line under it: the day's walks, when the reader has them on and there were steps. */
@Composable
internal fun dayDetailLine(detail: DayDetail, minWalkMinutes: Int): String? {
    val walks = detail.walks ?: return null
    if (detail.steps <= 0) return null
    return if (walks.isEmpty()) {
        pluralStringResource(R.plurals.history_day_no_walks, minWalkMinutes, minWalkMinutes)
    } else {
        pluralStringResource(R.plurals.history_day_walks, walks.size, walks.size, duration(walks.sumOf { it.minutes }))
    }
}

/** The sentence a week, a month or a year opens with. */
@Composable
internal fun periodHeadline(overview: PeriodOverview): String = when {
    overview.countedDays == 0 -> stringResource(R.string.history_period_nothing)

    overview.inProgress -> pluralStringResource(
        R.plurals.history_period_goal_days_so_far,
        overview.goalDays,
        overview.goalDays,
        overview.countedDays,
    )

    else -> pluralStringResource(
        R.plurals.history_period_goal_days,
        overview.goalDays,
        overview.goalDays,
        overview.countedDays,
    )
}

/** «8,240 steps a day on average, 12% more than the week before». */
@Composable
internal fun periodDetailLine(overview: PeriodOverview, format: MeasureFormatter): String? {
    val average = overview.dailyAverage ?: return null
    val first = pluralStringResource(R.plurals.history_period_average, average, format.steps(average))
    val change = overview.change ?: return first
    val scale = overview.period.scale
    val second = when {
        abs(change) < STEADY_BAND -> stringResource(sameRes(scale))
        change > 0 -> stringResource(moreRes(scale), format.percent(change))
        else -> stringResource(lessRes(scale), format.percent(-change))
    }
    return stringResource(R.string.history_clauses, first, second)
}

private fun moreRes(scale: PeriodScale) = when (scale) {
    PeriodScale.MONTH -> R.string.history_vs_month_more
    PeriodScale.YEAR -> R.string.history_vs_year_more
    else -> R.string.history_vs_week_more
}

private fun lessRes(scale: PeriodScale) = when (scale) {
    PeriodScale.MONTH -> R.string.history_vs_month_less
    PeriodScale.YEAR -> R.string.history_vs_year_less
    else -> R.string.history_vs_week_less
}

private fun sameRes(scale: PeriodScale) = when (scale) {
    PeriodScale.MONTH -> R.string.history_vs_month_same
    PeriodScale.YEAR -> R.string.history_vs_year_same
    else -> R.string.history_vs_week_same
}

/** Within 5% either way a period is "about the same" as the one before: less is noise. */
private const val STEADY_BAND = 0.05
