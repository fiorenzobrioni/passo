package com.callbackdev.passo.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.callbackdev.passo.core.designsystem.components.ChartBar
import com.callbackdev.passo.core.designsystem.components.ChartSpan
import com.callbackdev.passo.core.designsystem.components.WalkList
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.axisHour
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.today.HourlySteps

/**
 * One day (PLANNING.md §11 Phase 5): its sentence, its hours as bars with the walks marked on
 * them, its measures, and the walks themselves. With walk detection off there is no walk in
 * the chart, the sentence or the list: the switch means none of it.
 */
@Composable
internal fun DayPage(detail: DayDetail?, minWalkMinutes: Int, format: MeasureFormatter, bottomPadding: Dp) {
    // Until the day's minutes are read, a bare page: a chart of zeros would be a day nobody walked.
    if (detail == null) {
        Box(Modifier.fillMaxSize())
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(HistoryTags.PAGE),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "headline") { PageHeadline(dayHeadline(detail, format), dayDetailLine(detail, minWalkMinutes)) }
        item(key = "hours") { HoursCard(detail, format) }
        item(key = "metrics") { DayMetrics(detail, format) }
        val walks = detail.walks.orEmpty()
        if (walks.isNotEmpty()) {
            item(key = "walks") {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = GroupShape,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(HistoryTags.WALKS),
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.history_walks_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).semantics {
                                heading()
                            },
                        )
                        WalkList(walks, format)
                        Text(
                            text = stringResource(R.string.history_walks_caption),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HoursCard(detail: DayDetail, format: MeasureFormatter) {
    var selected by rememberSaveable { mutableStateOf<Int?>(null) }
    val hourly = detail.hourly
    val bars = (0 until HourlySteps.HOURS).map { hour ->
        val future = detail.currentHour != null && hour > detail.currentHour
        ChartBar(value = if (future) null else hourly[hour], current = hour == detail.currentHour)
    }
    val walks = detail.walks.orEmpty()
    val stepsOf = @Composable { steps: Int ->
        pluralStringResource(R.plurals.history_steps, steps, format.steps(steps))
    }
    val hourRange = @Composable { hour: Int ->
        stringResource(
            R.string.history_range,
            clockTime(hour * MINUTES_PER_HOUR),
            clockTime(((hour + 1) % 24) * MINUTES_PER_HOUR),
        )
    }
    val barTexts = (0 until HourlySteps.HOURS).map { hour ->
        val value = bars[hour].value
        stringResource(
            R.string.history_bar,
            hourRange(hour),
            if (value == null) stringResource(R.string.history_not_counted) else stepsOf(value),
        )
    }
    val busiest = (0 until HourlySteps.HOURS).maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0 }
    val title = stringResource(R.string.history_chart_hours)
    val description = stringResource(R.string.history_chart_description, title, dayHeadline(detail, format))
    ChartCard(
        title = title,
        caption = stringResource(R.string.history_caption_hours),
        legend = { if (walks.isNotEmpty()) ChartLegend(goal = false, walks = true) },
        readout = {
            val hour = selected
            val text = when {
                hour != null -> readoutText(hourRange(hour), bars[hour].value?.let { stepsOf(it) })

                busiest != null -> readoutText(
                    stringResource(R.string.history_readout_busiest, hourRange(busiest)),
                    stepsOf(hourly[busiest]),
                )

                else -> AnnotatedString(stringResource(R.string.history_readout_hint))
            }
            ChartReadout(text)
        },
    ) {
        BarChart(
            bars = bars,
            axisLabels = listOf(0, 6, 12, 18).map { it to axisHour(it) },
            description = description,
            barDescription = { barTexts[it] },
            selected = selected,
            onSelect = { selected = it },
            scaleLabel = format::steps,
            spans = walks.map { ChartSpan(it.startMinute / 60f, it.endMinute / 60f) },
            scaleFloor = HOUR_SCALE_FLOOR,
            modifier = Modifier.testTag(HistoryTags.CHART),
        )
    }
}

@Composable
private fun DayMetrics(detail: DayDetail, format: MeasureFormatter) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val distance = format.distance(detail.distanceMeters)
            HistoryTile(
                icon = PassoIcons.Distance,
                label = stringResource(R.string.history_metric_distance),
                value = distance.annotated(UnitStyle),
                spokenValue = distance.text(),
                meaning = stringResource(R.string.history_metric_distance_day_meaning),
                modifier = Modifier.weight(1f),
            )
            val energy = format.energy(detail.activeKcal)
            HistoryTile(
                icon = PassoIcons.Flame,
                label = stringResource(R.string.history_metric_calories),
                value = energy.annotated(UnitStyle),
                spokenValue = energy.text(),
                meaning = stringResource(R.string.history_metric_calories_meaning),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val active = format.minutes(detail.activeMinutes)
            HistoryTile(
                icon = PassoIcons.Clock,
                label = stringResource(R.string.history_metric_active),
                value = active.annotated(UnitStyle),
                spokenValue = active.text(),
                meaning = stringResource(R.string.history_metric_active_meaning),
                modifier = Modifier.weight(1f),
            )
            val brisk = format.minutes(detail.briskMinutes)
            HistoryTile(
                icon = PassoIcons.Bolt,
                label = stringResource(R.string.history_metric_brisk),
                value = brisk.annotated(UnitStyle),
                spokenValue = brisk.text(),
                meaning = stringResource(R.string.history_metric_brisk_day),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val MINUTES_PER_HOUR = 60

/**
 * The hours' scale reaches 1,000 steps at least: about ten minutes of walking. The world's
 * anchor, not the data's, so an hour of 200 steps looks like the little it was.
 */
private const val HOUR_SCALE_FLOOR = 1_000
