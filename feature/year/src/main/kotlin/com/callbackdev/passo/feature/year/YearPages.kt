package com.callbackdev.passo.feature.year

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.BarChart
import com.callbackdev.passo.core.designsystem.components.ChartBar
import com.callbackdev.passo.core.designsystem.components.HeatLevel
import com.callbackdev.passo.core.designsystem.components.heatColor
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.currentLocale
import com.callbackdev.passo.core.designsystem.format.datePattern
import com.callbackdev.passo.core.designsystem.format.longDate
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.shortDate
import com.callbackdev.passo.core.designsystem.format.shortDateWithYear
import com.callbackdev.passo.core.designsystem.format.unbrokenText
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.ways.wayNameRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.insights.Streak
import com.callbackdev.passo.core.domain.insights.Trend
import com.callbackdev.passo.core.domain.year.WayMeasure
import com.callbackdev.passo.core.domain.year.YearClose
import com.callbackdev.passo.core.domain.year.YearDay
import com.callbackdev.passo.core.domain.year.YearInReview
import com.callbackdev.passo.core.domain.year.YearPage
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.abs

/**
 * What a page says, in the order it is read: the sentence first, then the number and what it is,
 * then the lines under it. TalkBack reads it as one ([sentence]); the share image draws it.
 */
@Immutable
internal data class PageWords(val headline: String, val figure: String?, val unit: String?, val details: List<String>) {
    val sentence: String get() = (
        listOf(headline) +
            listOfNotNull(listOfNotNull(figure, unit).joinToString(" ").ifBlank { null }) +
            details
        )
        .joinToString(". ") { it.trimEnd('.') } + "."
}

/** The eyebrow over every page, so a page shared alone still says whose year and which. */
@Composable
internal fun yearEyebrow(review: YearInReview): String {
    val title = if (review.soFar) {
        stringResource(R.string.year_title_so_far, review.year)
    } else {
        stringResource(R.string.year_title_full, review.year)
    }
    return if (review.begunLate) {
        "$title · ${stringResource(R.string.year_since, shortDateWithYear(review.countedFrom))}"
    } else {
        title
    }
}

@Composable
internal fun pageWords(page: YearPage, review: YearInReview, format: MeasureFormatter): PageWords = when (page) {
    is YearPage.Distance -> PageWords(
        headline = page.asWay?.let { measureSentence(it) } ?: stringResource(R.string.year_distance_plain),
        figure = format.integer(page.steps),
        unit = pluralStringResource(R.plurals.year_steps_unit, page.steps.toQuantity()),
        details = listOf(
            stringResource(
                R.string.year_distance_detail,
                format.distance(page.distanceMeters).unbrokenText(),
                stepsADay(page.averageSteps, format),
            ),
        ),
    )

    is YearPage.Months -> {
        val best = page.months.first { it.month == page.best }
        PageWords(
            headline = stringResource(R.string.year_months_title, monthName(page.best)),
            figure = format.integer(best.steps),
            unit = pluralStringResource(R.plurals.year_steps_unit, best.steps.toQuantity()),
            details = listOf(stringResource(R.string.year_months_detail)),
        )
    }

    is YearPage.BestDay -> PageWords(
        headline = stringResource(R.string.year_best_title, longDate(page.date)),
        figure = format.steps(page.steps),
        unit = pluralStringResource(R.plurals.year_steps_unit, page.steps),
        details = listOfNotNull(
            page.outing?.let { outing ->
                stringResource(
                    R.string.year_best_outing,
                    LocalContext.current.resources.sessionName(outing),
                    stepsCount(outing.totals.steps.toLong(), format),
                )
            },
            page.walk?.let { walk ->
                stringResource(
                    R.string.year_best_walk,
                    duration(walk.minutes.toLong() * MILLIS_PER_MINUTE),
                    stepsCount(walk.steps.toLong(), format),
                )
            },
        ),
    )

    is YearPage.Rhythm -> {
        val weekday = page.weekday.getDisplayName(TextStyle.FULL, currentLocale())
        PageWords(
            headline = stringResource(R.string.year_rhythm_title, weekday, clockTime(page.hour * MINUTES_PER_HOUR)),
            figure = null,
            unit = null,
            details = listOf(
                stringResource(
                    R.string.year_rhythm_detail,
                    weekday.replaceFirstChar { it.titlecase(currentLocale()) },
                    stepsCount(page.byWeekday[page.weekday.ordinal].toLong(), format),
                ),
            ),
        )
    }

    is YearPage.Goal -> PageWords(
        // The sentence says what the number is, not the number: it follows, large.
        headline = if (page.daysMet > 0) {
            stringResource(R.string.year_goal_title)
        } else {
            stringResource(R.string.year_goal_none)
        },
        figure = format.integer(page.daysMet.toLong()),
        unit = pluralStringResource(R.plurals.year_goal_unit, page.daysMet, page.days.size),
        details = listOfNotNull(
            page.longest.takeIf { it.days > 0 }?.let { streak ->
                pluralStringResource(R.plurals.year_goal_streak, streak.days, streak.days, streakRange(streak))
            },
            stringResource(R.string.year_goal_calendar),
        ),
    )

    is YearPage.Outings -> PageWords(
        headline = pluralStringResource(R.plurals.year_outings_title, page.count, page.count),
        figure = null,
        unit = null,
        details = listOfNotNull(
            stringResource(
                R.string.year_outings_detail,
                duration(page.movingMillis),
                LocalContext.current.resources.sessionName(page.longest),
                duration(page.longest.totals.movingMillis),
            ),
            page.intervals.takeIf { it > 0 }?.let {
                pluralStringResource(R.plurals.year_outings_intervals, it, it)
            },
        ),
    )

    is YearPage.Ways -> {
        val total = page.ways.sumOf { it.meters }
        val wayLines = page.ways.map { way ->
            val parts = listOfNotNull(
                format.distance(way.meters).unbrokenText(),
                way.stamps.takeIf { it > 0 }?.let { pluralStringResource(R.plurals.year_way_stamps, it, it) },
                stringResource(R.string.year_way_arrived).takeIf { way.arrived },
            )
            stringResource(R.string.year_way_line, stringResource(wayNameRes(way.way)), parts.joinToString(" · "))
        }
        val cityNames = page.cities.map { stringResource(wayNameRes(it)) }
        PageWords(
            headline = if (page.ways.isNotEmpty()) {
                stringResource(R.string.year_ways_title, format.distance(total).unbrokenText())
            } else {
                pluralStringResource(R.plurals.year_cities_title, page.cities.size, page.cities.size)
            },
            figure = null,
            unit = null,
            details = wayLines + listOfNotNull(
                cityNames.takeIf {
                    it.isNotEmpty()
                }?.let { stringResource(R.string.year_cities, it.joinToString(", ")) },
            ),
        )
    }

    is YearPage.VersusLastYear -> {
        val last = stepsADay(page.lastAverage, format)
        val change = format.percent(abs(page.change))
        PageWords(
            headline = when (page.trend) {
                Trend.UP -> stringResource(R.string.year_versus_up, page.lastYear)
                Trend.DOWN -> stringResource(R.string.year_versus_down, page.lastYear)
                Trend.STEADY -> stringResource(R.string.year_versus_steady, page.lastYear)
            },
            figure = format.steps(page.average),
            unit = pluralStringResource(R.plurals.year_steps_a_day_unit, page.average),
            details = listOf(
                when (page.trend) {
                    Trend.UP -> stringResource(R.string.year_versus_detail_up, last, page.lastYear, change)
                    Trend.DOWN -> stringResource(R.string.year_versus_detail_down, last, page.lastYear, change)
                    Trend.STEADY -> stringResource(R.string.year_versus_detail_steady, last, page.lastYear)
                },
            ),
        )
    }

    is YearPage.Close -> PageWords(
        headline = closeSentence(page.close, format),
        figure = null,
        unit = null,
        details = emptyList(),
    )
}

/** What a page draws under its words, if anything: the months, the week, the calendar. */
@Composable
internal fun PageVisual(page: YearPage, state: YearUiState, format: MeasureFormatter) {
    when (page) {
        is YearPage.Months -> StillChart(
            bars = page.months.map { month ->
                ChartBar(
                    value = month.steps.toInt().takeIf { month.countedDays > 0 },
                    current = month.month == page.best,
                )
            },
            labels = page.months.mapIndexed { index, month -> index to monthLetter(month.month) },
            description = stringResource(R.string.year_months_chart, state.year),
            format = format,
        )

        is YearPage.Rhythm -> {
            val order = DayOfWeek.entries.let { all ->
                all.drop(state.firstDayOfWeek.ordinal) +
                    all.take(state.firstDayOfWeek.ordinal)
            }
            StillChart(
                bars = order.map { ChartBar(value = page.byWeekday[it.ordinal], current = it == page.weekday) },
                labels = order.mapIndexed { index, day ->
                    index to day.getDisplayName(TextStyle.NARROW, currentLocale())
                },
                description = stringResource(R.string.year_rhythm_chart),
                format = format,
            )
        }

        is YearPage.Goal -> YearCalendar(page.days, state.year)

        else -> Unit
    }
}

/**
 * A chart to look at, not to touch: on a page that moves on at a touch and goes back with a
 * swipe, a chart that took both would hold the reader on the page. Its words are the page's.
 */
@Composable
private fun StillChart(
    bars: List<ChartBar>,
    labels: List<Pair<Int, String>>,
    description: String,
    format: MeasureFormatter,
) {
    Box(Modifier.fillMaxWidth()) {
        BarChart(
            bars = bars,
            axisLabels = labels,
            description = description,
            barDescription = { "" },
            selected = null,
            onSelect = {},
            scaleLabel = format::steps,
            plotHeight = 140.dp,
        )
        // Over the chart, taking its touches: the page's own reach it through this one.
        Box(Modifier.matchParentSize().pointerInput(Unit) { })
    }
}

/**
 * The year as twelve small months, each day's ground its level against its own goal: the same
 * ramp as History's calendar, so a day reads the same in both. Drawn, not read: TalkBack hears
 * the page's sentence, not 365 days.
 */
@Composable
private fun YearCalendar(days: List<YearDay>, year: Int) {
    val byDate = days.associateBy { it.date }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        (1..12).chunked(MONTHS_A_ROW).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { number ->
                    MiniMonth(YearMonth.of(year, number), byDate, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MiniMonth(month: YearMonth, byDate: Map<LocalDate, YearDay>, modifier: Modifier) {
    val empty = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.35f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            monthShort(month),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Seven a row from the month's first day: a strip, not a weekday grid, so twelve fit.
        (1..month.lengthOfMonth()).chunked(DAYS_A_ROW).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                (0 until DAYS_A_ROW).forEach { i ->
                    val dayOfMonth = week.getOrNull(i)
                    val day = dayOfMonth?.let { byDate[month.atDay(it)] }
                    val fill = when {
                        dayOfMonth == null -> Color.Transparent
                        day == null -> empty
                        else -> heatColor(HeatLevel.of(day.steps, day.goalSteps))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(2.dp))
                            .background(fill),
                    )
                }
            }
        }
    }
}

/** The words over a page's number, centred, in the hero's voice. */
@Composable
internal fun PageText(words: PageWords, eyebrow: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            eyebrow,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Text(words.headline, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        if (words.figure != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    words.figure,
                    style = PassoTheme.type.heroNumber,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                words.unit?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        words.details.forEach {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun measureSentence(measure: WayMeasure): String {
    val name = stringResource(wayNameRes(measure.way))
    return when (measure) {
        is WayMeasure.MoreThan -> if (measure.times >= 2) {
            pluralStringResource(R.plurals.year_distance_times, measure.times, measure.times, name)
        } else {
            stringResource(R.string.year_distance_more, name)
        }

        is WayMeasure.Part -> when (measure.quarters) {
            1 -> stringResource(R.string.year_distance_quarter, name)
            2 -> stringResource(R.string.year_distance_half, name)
            else -> stringResource(R.string.year_distance_three_quarters, name)
        }
    }
}

@Composable
private fun closeSentence(close: YearClose, format: MeasureFormatter): String = when (close) {
    is YearClose.Arrived -> stringResource(R.string.year_close_arrived, stringResource(wayNameRes(close.way)))

    is YearClose.GoalMostDays -> pluralStringResource(R.plurals.year_close_goal, close.days, close.days)

    is YearClose.MoreThanLastYear -> stringResource(R.string.year_close_more, close.lastYear)

    is YearClose.LongStreak -> pluralStringResource(R.plurals.year_close_streak, close.days, close.days)

    is YearClose.ManyOutings -> pluralStringResource(R.plurals.year_close_outings, close.count, close.count)

    is YearClose.EveryStep -> pluralStringResource(
        R.plurals.year_close_steps,
        close.steps.toQuantity(),
        format.integer(close.steps),
    )
}

@Composable
private fun stepsADay(steps: Int, format: MeasureFormatter): String =
    pluralStringResource(R.plurals.year_steps_a_day, steps, format.steps(steps))

@Composable
private fun stepsCount(steps: Long, format: MeasureFormatter): String =
    pluralStringResource(R.plurals.year_steps_count, steps.toQuantity(), format.integer(steps))

@Composable
private fun duration(millis: Long): String {
    val minutes = (millis / MILLIS_PER_MINUTE).toInt()
    val hours = minutes / MINUTES_PER_HOUR
    return if (hours > 0) {
        stringResource(R.string.year_hours_minutes, hours, minutes % MINUTES_PER_HOUR)
    } else {
        stringResource(R.string.year_minutes, minutes)
    }
}

@Composable
private fun streakRange(streak: Streak): String {
    val start = streak.start ?: return ""
    val end = streak.end ?: return ""
    if (start == end) return shortDate(start)
    return stringResource(R.string.year_range, shortDate(start), shortDate(end))
}

@Composable
private fun monthName(month: YearMonth): String = datePattern(month.atDay(1), "MMMM")

@Composable
private fun monthShort(month: YearMonth): String = datePattern(month.atDay(1), "MMM")

@Composable
private fun monthLetter(month: YearMonth): String =
    month.month.getDisplayName(TextStyle.NARROW_STANDALONE, currentLocale())

/** A plural's quantity for a count that may pass an Int: past it, "other" is what every language wants. */
private fun Long.toQuantity(): Int = coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

private const val MILLIS_PER_MINUTE = 60_000L
private const val MINUTES_PER_HOUR = 60
private const val MONTHS_A_ROW = 4
private const val DAYS_A_ROW = 7
