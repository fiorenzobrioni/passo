package com.callbackdev.passo.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.BarChart
import com.callbackdev.passo.core.designsystem.components.CalendarCell
import com.callbackdev.passo.core.designsystem.components.CalendarHeatmap
import com.callbackdev.passo.core.designsystem.components.ChartBar
import com.callbackdev.passo.core.designsystem.components.HeatLegend
import com.callbackdev.passo.core.designsystem.components.HeatLevel
import com.callbackdev.passo.core.designsystem.components.MetricTrack
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.currentLocale
import com.callbackdev.passo.core.designsystem.format.datePattern
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.weekdayLetter
import com.callbackdev.passo.core.designsystem.format.weekdayShort
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.history.PeriodBar
import com.callbackdev.passo.core.domain.history.PeriodOverview
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * A week, a month or a year (PLANNING.md §11 Phase 5): its sentence, its bars against the goal
 * of each day, for a month the calendar of the goal, and what the period adds up to. A bar or a
 * calendar day opens: the chart is also the way down, from a year to a month to a day.
 */
@Composable
internal fun PeriodPage(
    overview: PeriodOverview,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    format: MeasureFormatter,
    onOpen: (PeriodScale, LocalDate) -> Unit,
    bottomPadding: Dp,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(HistoryTags.PAGE),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "headline") { PageHeadline(periodHeadline(overview), periodDetailLine(overview, format)) }
        item(key = "bars") { BarsCard(overview, format, onOpen) }
        if (overview.period.scale == PeriodScale.MONTH && overview.countedDays > 0) {
            item(key = "calendar") { CalendarCard(overview, today, firstDayOfWeek, format, onOpen) }
        }
        if (overview.countedDays > 0) item(key = "metrics") { PeriodMetrics(overview, format) }
    }
}

@Composable
private fun BarsCard(overview: PeriodOverview, format: MeasureFormatter, onOpen: (PeriodScale, LocalDate) -> Unit) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val scale = overview.period.scale
    val year = scale == PeriodScale.YEAR
    val bars = overview.bars.map { ChartBar(value = it.steps, goal = it.goal, met = it.met, current = it.current) }
    val labels = overview.bars.mapIndexed { index, bar -> barLabel(scale, index, bar) }
    val descriptions = overview.bars.map { barDescription(it, year, format) }
    val title = stringResource(
        when (scale) {
            PeriodScale.YEAR -> R.string.history_chart_months
            else -> R.string.history_chart_days
        },
    )
    ChartCard(
        title = title,
        caption = stringResource(if (year) R.string.history_caption_months else R.string.history_caption_days),
        legend = { ChartLegend(goal = true, walks = false) },
        readout = {
            val index = selected
            val bar = index?.let { overview.bars.getOrNull(it) }
            if (bar == null) {
                ChartReadout(AnnotatedString(stringResource(R.string.history_readout_hint)))
            } else {
                val steps = bar.steps
                ChartReadout(
                    text = readoutText(
                        time = if (year) datePattern(bar.start, "MMMM") else barDay(bar.start),
                        value = steps?.let {
                            if (year) {
                                pluralStringResource(R.plurals.history_steps_a_day, it, format.steps(it))
                            } else {
                                pluralStringResource(R.plurals.history_steps, it, format.steps(it))
                            }
                        } ?: stringResource(R.string.history_not_counted),
                        extra = steps?.let { goalState(bar, year, format) },
                    ),
                    action = when {
                        steps == null -> null
                        year -> stringResource(R.string.history_open_month)
                        else -> stringResource(R.string.history_open_day)
                    },
                    onAction = { onOpen(if (year) PeriodScale.MONTH else PeriodScale.DAY, bar.start) },
                )
            }
        },
    ) {
        BarChart(
            bars = bars,
            axisLabels = labels.withIndex().mapNotNull { (i, label) -> label?.let { i to it } },
            description = stringResource(R.string.history_chart_description, title, periodHeadline(overview)),
            barDescription = { descriptions[it] },
            selected = selected,
            onSelect = { selected = it },
            scaleLabel = format::steps,
            modifier = Modifier.testTag(HistoryTags.CHART),
        )
    }
}

/** The labels under the bars: every weekday, a month's weeks, a year's months in a letter. */
@Composable
private fun barLabel(scale: PeriodScale, index: Int, bar: PeriodBar): String? = when (scale) {
    PeriodScale.WEEK -> weekdayShort(bar.start)
    PeriodScale.MONTH -> if (index % 7 == 0) bar.start.dayOfMonth.toString() else null
    PeriodScale.YEAR -> bar.start.month.getDisplayName(TextStyle.NARROW_STANDALONE, currentLocale())
    PeriodScale.DAY -> null
}

/** «goal met», «1,580 short of the goal», or for a month «goal met on 12 days». */
@Composable
private fun goalState(bar: PeriodBar, year: Boolean, format: MeasureFormatter): String = when {
    year -> pluralStringResource(R.plurals.history_goal_days_short, bar.goalDays, bar.goalDays)

    bar.met -> stringResource(R.string.history_goal_met)

    else -> {
        val short = bar.goal - (bar.steps ?: 0)
        pluralStringResource(R.plurals.history_goal_short, short, format.steps(short))
    }
}

@Composable
private fun barDescription(bar: PeriodBar, year: Boolean, format: MeasureFormatter): String {
    val name = if (year) datePattern(bar.start, "MMMMyyyy") else datePattern(bar.start, "EEEEdMMMM")
    val steps =
        bar.steps ?: return stringResource(R.string.history_bar, name, stringResource(R.string.history_not_counted))
    val amount = if (year) {
        pluralStringResource(R.plurals.history_steps_a_day, steps, format.steps(steps))
    } else {
        pluralStringResource(R.plurals.history_steps, steps, format.steps(steps))
    }
    return stringResource(R.string.history_bar_with_goal, name, amount, goalState(bar, year, format))
}

@Composable
private fun CalendarCard(
    overview: PeriodOverview,
    today: LocalDate,
    firstDayOfWeek: DayOfWeek,
    format: MeasureFormatter,
    onOpen: (PeriodScale, LocalDate) -> Unit,
) {
    val cells = buildMap {
        for (bar in overview.bars) {
            val steps = bar.steps ?: continue
            put(bar.start, CalendarCell(HeatLevel.of(steps, bar.goal), barDescription(bar, year = false, format)))
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(HistoryTags.CALENDAR),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.history_calendar_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            CalendarHeatmap(
                month = overview.period.start,
                firstDayOfWeek = firstDayOfWeek,
                cells = cells,
                today = today,
                weekdayLabel = { weekdayLetter(it) },
                onDayClick = { onOpen(PeriodScale.DAY, it) },
            )
            HeatLegend(
                less = stringResource(R.string.history_calendar_less),
                more = stringResource(R.string.history_calendar_more),
                met = stringResource(R.string.history_calendar_met),
            )
            Text(
                text = stringResource(R.string.history_calendar_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PeriodMetrics(overview: PeriodOverview, format: MeasureFormatter) {
    val totals = overview.totals
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin).testTag(HistoryTags.METRICS),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val steps = format.integer(totals.steps)
            val average = overview.dailyAverage ?: 0
            HistoryTile(
                icon = PassoIcons.Steps,
                label = stringResource(R.string.history_metric_steps),
                value = AnnotatedString(steps),
                spokenValue = steps,
                meaning = pluralStringResource(R.plurals.history_steps_a_day, average, format.steps(average)),
                modifier = Modifier.weight(1f),
            )
            val distance = format.distance(totals.distanceMeters)
            HistoryTile(
                icon = PassoIcons.Distance,
                label = stringResource(R.string.history_metric_distance),
                value = distance.annotated(UnitStyle),
                spokenValue = distance.text(),
                meaning = stringResource(R.string.history_metric_distance_meaning),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val energy = format.energy(totals.activeKcal)
            HistoryTile(
                icon = PassoIcons.Flame,
                label = stringResource(R.string.history_metric_calories),
                value = energy.annotated(UnitStyle),
                spokenValue = energy.text(),
                meaning = stringResource(R.string.history_metric_calories_meaning),
                modifier = Modifier.weight(1f),
            )
            val active = format.minutes(totals.activeMinutes)
            HistoryTile(
                icon = PassoIcons.Clock,
                label = stringResource(R.string.history_metric_active),
                value = active.annotated(UnitStyle),
                spokenValue = active.text(),
                meaning = stringResource(R.string.history_metric_active_meaning),
                modifier = Modifier.weight(1f),
            )
        }
        if (overview.period.scale == PeriodScale.WEEK) {
            // The WHO counts moderate minutes by the week, so a week is where they are measured.
            val brisk = format.minutes(totals.briskMinutes)
            val left = (MetricsConstants.WHO_WEEKLY_MODERATE_MINUTES - totals.briskMinutes).coerceAtLeast(0)
            val color = if (left == 0) PassoTheme.colors.goal else MaterialTheme.colorScheme.primary
            HistoryTile(
                icon = PassoIcons.Bolt,
                label = stringResource(R.string.history_metric_brisk),
                value = brisk.annotated(UnitStyle),
                spokenValue = brisk.text(),
                meaning = if (left == 0) {
                    stringResource(R.string.history_metric_brisk_week_done)
                } else {
                    pluralStringResource(R.plurals.history_metric_brisk_week_left, left, left)
                },
                modifier = Modifier.fillMaxWidth(),
                track = {
                    MetricTrack(
                        value = totals.briskMinutes.toFloat(),
                        range = 0f..MetricsConstants.WHO_WEEKLY_MODERATE_MINUTES.toFloat(),
                        colors = listOf(color),
                    )
                },
            )
        }
    }
}
