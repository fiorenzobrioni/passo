package com.callbackdev.passo.feature.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.GroupHeader
import com.callbackdev.passo.core.designsystem.components.MetricTile
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.datePattern
import com.callbackdev.passo.core.designsystem.format.monthYear
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.shortDate
import com.callbackdev.passo.core.designsystem.format.shortDateWithYear
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.weekdayLetter
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.domain.insights.InsightsHeadline
import com.callbackdev.passo.core.domain.insights.Record
import com.callbackdev.passo.core.domain.insights.Streak
import com.callbackdev.passo.core.domain.insights.Trend
import java.time.LocalDate

/** Insights, with its state from [InsightsViewModel]. */
@Composable
fun InsightsRoute(
    onOpenSettings: () -> Unit,
    onOpenPeriod: (PeriodScale, LocalDate) -> Unit,
    bottomPadding: Dp,
    viewModel: InsightsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InsightsScreen(state, onOpenSettings, onOpenPeriod, bottomPadding = bottomPadding)
}

/**
 * Insights (PLANNING.md §11 Phase 5): one sentence first, then the streak with the last seven
 * days, the records (each opens its day, week or month in History), the averages and what it
 * all adds up to. Every goal is the one of its own day, so nothing here moves when the goal does.
 */
@Composable
fun InsightsScreen(
    state: InsightsUiState?,
    onOpenSettings: () -> Unit,
    onOpenPeriod: (PeriodScale, LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = onOpenSettings) {
                    Icon(PassoIcons.Settings, contentDescription = stringResource(R.string.insights_settings))
                }
            }
            if (state != null) InsightsList(state, onOpenPeriod, bottomPadding)
        }
    }
}

@Composable
private fun InsightsList(state: InsightsUiState, onOpenPeriod: (PeriodScale, LocalDate) -> Unit, bottomPadding: Dp) {
    val format = rememberMeasureFormatter(state.units)
    val insights = state.insights
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag(InsightsTags.LIST),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomPadding + 24.dp),
    ) {
        item(key = "headline") { Headline(state, format) }
        val firstDay = insights.firstDay ?: return@LazyColumn
        item(key = "streak") { StreakCard(state) }

        item(key = "records-header") { GroupHeader(stringResource(R.string.insights_group_records)) }
        item(key = "records") { Records(state, format, onOpenPeriod) }

        item(key = "averages-header") { GroupHeader(stringResource(R.string.insights_group_averages)) }
        item(key = "averages") { Averages(state, format) }

        item(key = "totals-header") {
            GroupHeader(stringResource(R.string.insights_group_totals, shortDateWithYear(firstDay)))
        }
        item(key = "totals") { Totals(state, format) }
    }
}

@Composable
private fun Headline(state: InsightsUiState, format: MeasureFormatter) {
    val (sentence, detail) = when (val headline = state.insights.headline) {
        InsightsHeadline.NothingYet ->
            stringResource(R.string.insights_headline_nothing) to stringResource(R.string.insights_detail_nothing)

        is InsightsHeadline.BestDayToday -> stringResource(R.string.insights_headline_best_today) to
            pluralStringResource(R.plurals.insights_detail_best_today, headline.steps, format.steps(headline.steps))

        is InsightsHeadline.OnAStreak ->
            pluralStringResource(R.plurals.insights_headline_streak, headline.days, headline.days) to
                if (headline.includesToday) {
                    stringResource(R.string.insights_detail_streak_today)
                } else {
                    pluralStringResource(
                        R.plurals.insights_detail_streak_open,
                        headline.days + 1,
                        format.steps(state.goalSteps),
                        headline.days + 1,
                    )
                }

        is InsightsHeadline.LastWeek -> {
            val average = format.steps(headline.average)
            val change = format.percent(kotlin.math.abs(headline.change))
            when (headline.trend) {
                Trend.UP -> stringResource(R.string.insights_headline_week_up) to
                    stringResource(R.string.insights_detail_week_up, average, change)

                Trend.DOWN -> stringResource(R.string.insights_headline_week_down) to
                    stringResource(R.string.insights_detail_week_down, average, change)

                Trend.STEADY -> stringResource(R.string.insights_headline_week_steady) to
                    stringResource(R.string.insights_detail_week_steady, average)
            }
        }

        is InsightsHeadline.Average ->
            pluralStringResource(
                R.plurals.insights_headline_average,
                headline.average,
                format.steps(headline.average),
            ) to
                pluralStringResource(R.plurals.insights_detail_average, headline.days, headline.days)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            .semantics(mergeDescendants = true) { }
            .testTag(InsightsTags.HEADLINE),
    ) {
        Text(sentence, style = MaterialTheme.typography.titleLarge)
        Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * The streak as a number in the hero's voice, on the goal's ground while it runs, with the last
 * seven days as marks under it: met, missed, under way, or not counted.
 */
@Composable
private fun StreakCard(state: InsightsUiState) {
    val insights = state.insights
    val streak = insights.currentStreak
    val running = streak.days > 0
    val ground = if (running) PassoTheme.colors.goalContainer else MaterialTheme.colorScheme.surfaceContainerLow
    val longest = insights.longestStreak
    val longestLine = when {
        longest.days == 0 -> null

        running && streak.days == longest.days && streak.end == longest.end ->
            stringResource(R.string.insights_streak_longest_now)

        else -> pluralStringResource(
            R.plurals.insights_streak_longest,
            longest.days,
            longest.days,
            streakRange(longest, state.today),
        )
    }
    val words = if (running) {
        pluralStringResource(R.plurals.insights_streak_days, streak.days)
    } else {
        stringResource(R.string.insights_streak_none)
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(InsightsTags.STREAK),
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(ground, MaterialTheme.colorScheme.surfaceContainerLow)))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.semantics(mergeDescendants = true) { },
            ) {
                Icon(
                    PassoIcons.Streak,
                    contentDescription = null,
                    tint = if (running) PassoTheme.colors.goal else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
                Column(Modifier.weight(1f)) {
                    if (running) {
                        Text(
                            text = streak.days.toString(),
                            style = PassoTheme.type.heroNumber.copy(fontSize = 44.sp, lineHeight = 48.sp),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = words,
                        style = if (running) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.bodyLarge
                        },
                    )
                    if (longestLine != null) {
                        Text(
                            longestLine,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = stringResource(R.string.insights_streak_last_days),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                state.lastDays.forEach { DayMarkView(it) }
            }
        }
    }
}

@Composable
private fun DayMarkView(mark: DayMark) {
    val name = datePattern(mark.date, "EEEEdMMMM")
    val spoken = when {
        !mark.counted -> stringResource(R.string.insights_day_not_counted, name)
        mark.met -> stringResource(R.string.insights_day_met, name)
        mark.today -> stringResource(R.string.insights_day_today_open, name)
        else -> stringResource(R.string.insights_day_not_met, name)
    }
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        val shape = CircleShape
        val base = Modifier.size(32.dp).clip(shape)
        val look = when {
            mark.met -> base.background(PassoTheme.colors.goal)
            !mark.counted -> base.border(1.dp, scheme.outlineVariant, shape)
            mark.today -> base.border(2.dp, scheme.primary, shape)
            else -> base.background(scheme.surfaceContainerHighest)
        }
        Box(look, contentAlignment = Alignment.Center) {
            if (mark.met) {
                Icon(
                    PassoIcons.Check,
                    contentDescription = null,
                    tint = scheme.surface,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = weekdayLetter(mark.date),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (mark.today) FontWeight.Bold else null),
            color = if (mark.today) scheme.primary else scheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun streakRange(streak: Streak, today: LocalDate): String {
    val start = streak.start ?: return ""
    val end = streak.end ?: return ""
    if (start == end) return dateText(start, today)
    return stringResource(R.string.insights_range, dateText(start, today), dateText(end, today))
}

@Composable
private fun dateText(date: LocalDate, today: LocalDate): String =
    if (date.year == today.year) shortDate(date) else shortDateWithYear(date)

@Composable
private fun Records(state: InsightsUiState, format: MeasureFormatter, onOpenPeriod: (PeriodScale, LocalDate) -> Unit) {
    val insights = state.insights
    val today = state.today
    val stepsOf = @Composable { record: Record ->
        pluralStringResource(R.plurals.insights_steps, record.steps.toQuantity(), format.integer(record.steps))
    }
    val rows = buildList {
        insights.bestDay?.let {
            val skeleton = if (it.start.year == today.year) "EEEdMMM" else "EEEdMMMyyyy"
            val date = datePattern(it.start, skeleton, capitalized = false)
            add(
                RecordLine(
                    R.string.insights_record_day,
                    PassoIcons.Trophy,
                    stepsOf(it),
                    date,
                    PeriodScale.DAY,
                    it.start,
                ),
            )
        }
        insights.bestWeek?.let {
            val range = stringResource(R.string.insights_range, dateText(it.start, today), dateText(it.end, today))
            add(
                RecordLine(
                    R.string.insights_record_week,
                    PassoIcons.Trophy,
                    stepsOf(it),
                    range,
                    PeriodScale.WEEK,
                    it.start,
                ),
            )
        }
        insights.bestMonth?.let {
            val month = monthYear(it.start)
            add(
                RecordLine(
                    R.string.insights_record_month,
                    PassoIcons.Trophy,
                    stepsOf(it),
                    month,
                    PeriodScale.MONTH,
                    it.start,
                ),
            )
        }
        val longest = insights.longestStreak
        val end = longest.end
        if (longest.days > 0 && end != null) {
            val days = pluralStringResource(R.plurals.insights_days, longest.days, longest.days)
            val range = streakRange(longest, today)
            add(RecordLine(R.string.insights_record_streak, PassoIcons.Streak, days, range, PeriodScale.DAY, end))
        }
    }
    SettingsGroup(modifier = Modifier.testTag(InsightsTags.RECORDS)) {
        rows.forEachIndexed { index, row ->
            if (index > 0) GroupDivider()
            RecordRow(
                icon = row.icon,
                label = stringResource(row.label),
                value = stringResource(R.string.insights_record_value, row.amount, row.time),
                onClick = { onOpenPeriod(row.scale, row.date) },
            )
        }
    }
}

/** One record, ready to draw: the tap opens [date]'s [scale] in History. */
private class RecordLine(
    val label: Int,
    val icon: ImageVector,
    val amount: String,
    val time: String,
    val scale: PeriodScale,
    val date: LocalDate,
)

/** A record: what, how much and when; the tap opens it in History. */
@Composable
private fun RecordRow(icon: ImageVector, label: String, value: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = ScreenMargin, vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Averages(state: InsightsUiState, format: MeasureFormatter) {
    val insights = state.insights
    val aDay = @Composable { steps: Int -> pluralStringResource(R.plurals.insights_a_day, steps, format.steps(steps)) }
    Column {
        SettingsGroup(modifier = Modifier.testTag(InsightsTags.AVERAGES)) {
            val last7 = insights.averageLast7
            val previous = insights.averagePrevious7
            AverageRow(
                label = stringResource(R.string.insights_average_7),
                value = when {
                    last7 == null -> pluralStringResource(R.plurals.insights_not_yet, 7, 7)

                    previous == null || previous == 0 || last7 == previous -> aDay(last7)

                    last7 > previous -> stringResource(
                        R.string.insights_vs_before_more,
                        aDay(last7),
                        format.percent((last7 - previous).toDouble() / previous),
                    )

                    else -> stringResource(
                        R.string.insights_vs_before_less,
                        aDay(last7),
                        format.percent((previous - last7).toDouble() / previous),
                    )
                },
            )
            GroupDivider()
            AverageRow(
                label = stringResource(R.string.insights_average_30),
                value =
                insights.averageLast30?.let {
                    aDay(it)
                } ?: pluralStringResource(R.plurals.insights_not_yet, 30, 30),
            )
            val first = insights.firstDay
            val all = insights.averageAll
            if (first != null && all != null) {
                GroupDivider()
                AverageRow(
                    label = stringResource(R.string.insights_average_all, dateText(first, state.today)),
                    value = aDay(all),
                )
            }
        }
        Text(
            text = stringResource(R.string.insights_averages_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun AverageRow(label: String, value: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = ScreenMargin, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Totals(state: InsightsUiState, format: MeasureFormatter) {
    val insights = state.insights
    val unit = SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin).testTag(InsightsTags.TOTALS),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val steps = format.integer(insights.totalSteps)
            Tile(
                icon = PassoIcons.Steps,
                label = stringResource(R.string.insights_total_steps),
                value = AnnotatedString(steps),
                spokenValue = steps,
                meaning = pluralStringResource(
                    R.plurals.insights_total_steps_meaning,
                    insights.countedDays,
                    insights.countedDays,
                ),
                modifier = Modifier.weight(1f),
            )
            val distance = format.distance(insights.totalDistanceMeters)
            Tile(
                icon = PassoIcons.Distance,
                label = stringResource(R.string.insights_total_distance),
                value = distance.annotated(unit),
                spokenValue = distance.text(),
                meaning = stringResource(R.string.insights_total_distance_meaning),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val energy = format.energy(insights.totalActiveKcal)
            Tile(
                icon = PassoIcons.Flame,
                label = stringResource(R.string.insights_total_calories),
                value = energy.annotated(unit),
                spokenValue = energy.text(),
                meaning = stringResource(R.string.insights_total_calories_meaning),
                modifier = Modifier.weight(1f),
            )
            val days = pluralStringResource(R.plurals.insights_days, insights.goalDays, insights.goalDays)
            val share = if (insights.countedDays == 0) 0.0 else insights.goalDays.toDouble() / insights.countedDays
            Tile(
                icon = PassoIcons.Flag,
                label = stringResource(R.string.insights_total_goal_days),
                value = AnnotatedString(days),
                spokenValue = days,
                meaning = stringResource(R.string.insights_total_goal_days_meaning, format.percent(share)),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun Tile(
    icon: ImageVector,
    label: String,
    value: AnnotatedString,
    spokenValue: String,
    meaning: String,
    modifier: Modifier = Modifier,
) {
    MetricTile(
        icon = icon,
        label = label,
        value = value,
        meaning = meaning,
        spoken = stringResource(R.string.insights_metric_spoken, label, spokenValue, meaning),
        modifier = modifier,
    )
}

/** A plural's quantity for a count that may exceed an Int: past that, "other" is right anyway. */
private fun Long.toQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

/** Hooks for the UI tests. */
object InsightsTags {
    const val LIST = "insights_list"
    const val HEADLINE = "insights_headline"
    const val STREAK = "insights_streak"
    const val RECORDS = "insights_records"
    const val AVERAGES = "insights_averages"
    const val TOTALS = "insights_totals"
}
