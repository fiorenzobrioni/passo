package com.callbackdev.passo.feature.guide

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.BarChart
import com.callbackdev.passo.core.designsystem.components.ChartBar
import com.callbackdev.passo.core.designsystem.components.MetricTile
import com.callbackdev.passo.core.designsystem.components.MetricTrack
import com.callbackdev.passo.core.designsystem.components.ProgressRing
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.currentLocale
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.weekdayShort
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.settings.firstDayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun GuideRoute(onBack: () -> Unit, viewModel: GuideViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GuideScreen(state = state, onBack = onBack)
}

/**
 * The guide, in Chiaro's shape (its VISION §5.7): a tour of the three screens and the outings,
 * what each one answers, and the things a screen cannot say out loud (that steps arrive in
 * batches, that a day keeps its goal, that Passo wakes the phone only for an outing), closing
 * on where the numbers come from. Re-openable forever from Settings: a definition offered
 * before the reader has met the thing does not stick.
 *
 * Chiaro's two rules hold it in shape. It **never teaches a control**: it says what a screen is
 * for, never which button to press, because a control that needs explaining is a bug. And it
 * **never justifies an absence**: it says what Passo does, not what it is not. It teaches by
 * showing the app's own components (the ring, two metric tiles, History's bars), each with a
 * caption saying it is an example, in the reader's units, so no sample reads as their data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(state: GuideUiState?, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.guide_back))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the settings are read the examples would be in the wrong units for a frame.
        if (state != null) GuideContent(state, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun GuideContent(state: GuideUiState, modifier: Modifier) {
    val format = rememberMeasureFormatter(state.units)
    Column(
        modifier = modifier
            .testTag(GuideTags.CONTENT)
            .verticalScroll(rememberScrollState())
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Paragraph(stringResource(R.string.guide_intro))

        // The map first: the three glyphs of the bottom bar, so every chapter is placed before
        // it starts.
        Chapter(null, stringResource(R.string.guide_map_title))
        TabMap()
        Paragraph(stringResource(R.string.guide_map_note))

        Chapter(PassoIcons.Steps, stringResource(R.string.guide_counting_title))
        Paragraph(stringResource(R.string.guide_counting_p1))
        Feature(R.string.guide_counting_notification_title, R.string.guide_counting_notification_body)
        Feature(R.string.guide_counting_late_title, R.string.guide_counting_late_body)
        Feature(R.string.guide_counting_restart_title, R.string.guide_counting_restart_body)
        Feature(R.string.guide_counting_stops_title, R.string.guide_counting_stops_body)

        Chapter(PassoIcons.Today, stringResource(R.string.guide_today_title))
        Paragraph(stringResource(R.string.guide_today_p1))
        RingSample(format)
        Caption(stringResource(R.string.guide_today_ring_caption))
        Feature(R.string.guide_today_sentence_title, R.string.guide_today_sentence_body)
        Feature(R.string.guide_today_day_title, R.string.guide_today_day_body)
        Feature(R.string.guide_today_walks_title, R.string.guide_today_walks_body)
        Feature(R.string.guide_today_estimates_title, R.string.guide_today_estimates_body)
        MetricSample(format)
        Caption(stringResource(R.string.guide_today_estimates_caption))

        Chapter(PassoIcons.Outing, stringResource(R.string.guide_outings_title))
        Paragraph(stringResource(R.string.guide_outings_p1))
        Feature(R.string.guide_outings_signals_title, R.string.guide_outings_signals_body)
        Feature(R.string.guide_outings_motion_title, R.string.guide_outings_motion_body)
        Feature(R.string.guide_outings_end_title, R.string.guide_outings_end_body)
        Feature(R.string.guide_outings_battery_title, R.string.guide_outings_battery_body)
        Feature(R.string.guide_outings_start_title, R.string.guide_outings_start_body)

        Chapter(PassoIcons.History, stringResource(R.string.guide_history_title))
        Paragraph(stringResource(R.string.guide_history_p1))
        WeekSample(state, format)
        Caption(stringResource(R.string.guide_history_week_caption))
        Feature(R.string.guide_history_goal_title, R.string.guide_history_goal_body)
        Feature(R.string.guide_history_frozen_title, R.string.guide_history_frozen_body)
        Feature(R.string.guide_history_uncounted_title, R.string.guide_history_uncounted_body)

        Chapter(PassoIcons.Trophy, stringResource(R.string.guide_insights_title))
        Paragraph(stringResource(R.string.guide_insights_p1))
        Feature(R.string.guide_insights_streak_title, R.string.guide_insights_streak_body)

        Chapter(PassoIcons.Widgets, stringResource(R.string.guide_widgets_title))
        Feature(R.string.guide_widgets_two_title, R.string.guide_widgets_two_body)
        Feature(R.string.guide_widgets_wait_title, R.string.guide_widgets_wait_body)

        Chapter(PassoIcons.Shield, stringResource(R.string.guide_data_title))
        Paragraph(stringResource(R.string.guide_data_p1))
        Paragraph(stringResource(R.string.guide_data_p2))
        Paragraph(stringResource(R.string.guide_data_p3))
    }
}

// The prose kit: Chiaro's, value for value.

@Composable
private fun Chapter(icon: ImageVector?, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(top = 20.dp),
    ) {
        if (icon != null) {
            // Decoration: the title beside it says it all.
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
        }
        Text(text, style = MaterialTheme.typography.titleLarge)
    }
}

/** Prose to be read, not scanned: bodyLarge. */
@Composable
private fun Paragraph(text: String) {
    Text(text, style = MaterialTheme.typography.bodyLarge)
}

/**
 * One thing a screen does: its name, then what it is for. No icon of its own: the chapter's
 * glyph already says which screen this is.
 */
@Composable
private fun Feature(@StringRes title: Int, @StringRes body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
    }
}

/** What the sample above it stands for: always said, so no example reads as the reader's data. */
@Composable
private fun Caption(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// The samples: the app's own components, shown as themselves.

/** The bottom bar spelled out: the same three glyphs, in the same order. */
@Composable
private fun TabMap() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        TabRow(PassoIcons.Today, R.string.guide_map_today_name, R.string.guide_map_today)
        TabRow(PassoIcons.History, R.string.guide_map_history_name, R.string.guide_map_history)
        TabRow(PassoIcons.Trophy, R.string.guide_map_insights_name, R.string.guide_map_insights)
    }
}

@Composable
private fun TabRow(icon: ImageVector, @StringRes name: Int, @StringRes body: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp),
        )
        Column {
            Text(stringResource(name), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Today's ring, smaller: a day a little ahead of the notch where a usual day stands by now. */
@Composable
private fun RingSample(format: MeasureFormatter) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        ProgressRing(
            progress = SAMPLE_STEPS.toFloat() / SAMPLE_GOAL,
            usualProgress = SAMPLE_USUAL.toFloat() / SAMPLE_GOAL,
            reached = false,
            // Never the bloom: that is the reader's own goal's, not an example's.
            celebrate = false,
            onCelebrated = {},
            strokeWidth = 12.dp,
            modifier = Modifier.size(168.dp).testTag(GuideTags.RING),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    format.steps(SAMPLE_STEPS),
                    style = PassoTheme.type.heroNumber.copy(fontSize = 36.sp, lineHeight = 39.sp),
                )
                Text(
                    stringResource(R.string.guide_sample_of_goal, format.steps(SAMPLE_GOAL)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Two of Today's tiles: a value, and the line that says what it rests on or what is left. */
@Composable
private fun MetricSample(format: MeasureFormatter) {
    val unit = SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val distance = format.distance(SAMPLE_STEPS * SAMPLE_STEP_LENGTH_M)
        val step = format.stepLength(SAMPLE_STEP_LENGTH_M).text()
        val distanceLabel = stringResource(R.string.guide_sample_distance)
        val distanceMeaning = stringResource(R.string.guide_sample_distance_meaning, step)
        MetricTile(
            icon = PassoIcons.Distance,
            label = distanceLabel,
            value = distance.annotated(unit),
            meaning = distanceMeaning,
            spoken = "$distanceLabel: ${distance.text()}. $distanceMeaning",
            modifier = Modifier.weight(1f),
        )
        val brisk = format.minutes(SAMPLE_BRISK)
        val left = MetricsConstants.DAILY_BRISK_SHARE_MINUTES - SAMPLE_BRISK
        val briskLabel = stringResource(R.string.guide_sample_brisk)
        val briskMeaning = pluralStringResource(R.plurals.guide_sample_brisk_left, left, left)
        MetricTile(
            icon = PassoIcons.Bolt,
            label = briskLabel,
            value = brisk.annotated(unit),
            meaning = briskMeaning,
            spoken = "$briskLabel: ${brisk.text()}. $briskMeaning",
            modifier = Modifier.weight(1f),
            track = {
                MetricTrack(
                    value = SAMPLE_BRISK.toFloat(),
                    range = 0f..MetricsConstants.DAILY_BRISK_SHARE_MINUTES.toFloat(),
                    colors = listOf(MaterialTheme.colorScheme.primary),
                )
            },
        )
    }
}

/**
 * A week of History's bars, drawn by History's chart: the goal a line that steps where it
 * changed, the days at their goal in its color. The weekdays are the reader's, from their first
 * day of the week; only the counts are the example. A touch reads a bar, as in History.
 */
@Composable
private fun WeekSample(state: GuideUiState, format: MeasureFormatter) {
    val locale = currentLocale()
    val start = LocalDate.now()
        .minusWeeks(1)
        .with(TemporalAdjusters.previousOrSame(firstDayOfWeek(state.firstDayOfWeek, locale)))
    val days = (0 until DAYS_IN_WEEK).map { start.plusDays(it.toLong()) }
    val bars = SAMPLE_WEEK.map { (steps, goal) -> ChartBar(value = steps, goal = goal, met = steps >= goal) }
    val labels = days.map { weekdayShort(it) }
    val met = stringResource(R.string.guide_sample_goal_met)
    val short = stringResource(R.string.guide_sample_goal_short)
    val readings = SAMPLE_WEEK.mapIndexed { i, (steps, goal) ->
        stringResource(R.string.guide_sample_bar, labels[i], format.steps(steps), if (steps >= goal) met else short)
    }
    var selected by remember { mutableStateOf<Int?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = selected?.let { readings[it] } ?: stringResource(R.string.guide_sample_touch),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BarChart(
            bars = bars,
            axisLabels = labels.withIndex().map { (i, label) -> i to label },
            description = stringResource(R.string.guide_history_week_description),
            barDescription = { readings[it] },
            selected = selected,
            onSelect = { selected = it },
            scaleLabel = format::steps,
            plotHeight = 132.dp,
            modifier = Modifier.testTag(GuideTags.WEEK),
        )
    }
}

object GuideTags {
    const val CONTENT = "guide_content"
    const val RING = "guide_ring"
    const val WEEK = "guide_week"
}

private const val SAMPLE_GOAL = 8_000
private const val SAMPLE_STEPS = 4_960
private const val SAMPLE_USUAL = 4_400
private const val SAMPLE_STEP_LENGTH_M = 0.70
private const val SAMPLE_BRISK = 14
private const val DAYS_IN_WEEK = 7

/** A week's steps and goals: the goal raised midweek, so the line has a step to show. */
private val SAMPLE_WEEK = listOf(
    9_120 to 8_000,
    6_480 to 8_000,
    8_350 to 8_000,
    10_240 to 10_000,
    5_210 to 10_000,
    11_870 to 10_000,
    7_400 to 10_000,
)
