package com.callbackdev.passo.feature.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.PlaceText
import com.callbackdev.passo.core.designsystem.components.WayMapView
import com.callbackdev.passo.core.designsystem.format.unbrokenText
import com.callbackdev.passo.core.designsystem.format.wayHeadline
import com.callbackdev.passo.core.designsystem.format.wayNextStage
import com.callbackdev.passo.core.designsystem.format.wayProgressText
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.ways.wayNameRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.ways.WayToday
import com.callbackdev.passo.core.model.WayId

/**
 * The way under way, at the foot of the page (PLANNING.md §15 «The way under way on Today»): not
 * where the reader stands, which Insights and the way's page say, but what today did for it.
 * The sentence is today's stretch; the bar ends with it, in the day's own primary (the ring's,
 * the chart's line), set apart from the days before in the goal's green. The whole card opens the way.
 */
@Composable
internal fun WayCard(way: WayToday, format: MeasureFormatter, onOpen: (WayId, Long) -> Unit) {
    val progress = way.progress
    val headline = wayHeadline(progress)
    val today = format.distance(way.todayMeters).unbrokenText()
    val sentence = when {
        way.movedToday && !progress.finished -> stringResource(R.string.today_way_moved, today)
        else -> headline
    }
    val detail = when {
        progress.finished -> if (way.movedToday) stringResource(R.string.today_way_last, today) else null

        else -> listOfNotNull(headline.takeIf { way.movedToday }, wayNextStage(progress, format))
            .joinToString(DETAIL_SEPARATOR)
            .ifEmpty { null }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(TodayTags.WAY),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { onOpen(progress.way.id, progress.journey.id) }
                .padding(16.dp),
        ) {
            WayMapView(
                way = progress.way,
                walkedMeters = progress.walkedMeters,
                reached = progress.reached.size,
                contentDescription = "",
                detailed = false,
                ratio = 1f,
                modifier = Modifier.size(MAP_SIZE),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(wayNameRes(progress.way.id)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PlaceText(sentence, style = MaterialTheme.typography.titleMedium)
                    detail?.let {
                        PlaceText(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        wayProgressText(progress, format),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WayBar(way)
            }
        }
    }
}

/**
 * The way's length as a bar: the days before today in the goal's green, today's stretch after
 * them in primary, set apart by a small gap on each side. The gap, not the colour, is what tells
 * the piece apart: the two are about as light, and with the system's colours primary may be a
 * green itself (owner, 17 Oct 2026, from a phone where the blue piece was barely seen). A
 * stretch too short to see on a long way is drawn a few dp long, since the sentence above already
 * gives its figure.
 */
@Composable
private fun WayBar(way: WayToday) {
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val before = PassoTheme.colors.goal
    val today = MaterialTheme.colorScheme.primary
    val length = way.progress.way.lengthMeters.toDouble()
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .testTag(TodayTags.WAY_BAR)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(way.progress.fraction.toFloat(), 0f..1f) },
    ) {
        // Round caps reach half the stroke past each end: the line runs inside them.
        val stroke = size.height
        val y = size.height / 2
        val start = stroke / 2
        val end = size.width - stroke / 2
        fun x(meters: Double): Float = start + (end - start) * (meters / length).coerceIn(0.0, 1.0).toFloat()
        fun bar(color: Color, from: Float, to: Float) =
            drawLine(color, Offset(from, y), Offset(to, y), strokeWidth = stroke, cap = StrokeCap.Round)

        val walkedBefore = way.beforeTodayMeters > 0.0
        var past = x(way.beforeTodayMeters)
        // From one line's end to the next one's start: both caps and the gap between them.
        val step = stroke + PIECE_GAP.toPx()
        if (way.movedToday) {
            val least = MIN_TODAY.toPx()
            var from = if (walkedBefore) past + step else start
            var to = maxOf(x(way.progress.walkedMeters), from + least)
            // At the end of the way (the day of arrival) the days before give way to today's
            // piece, so it keeps its length and its gap there too.
            if (to > end) {
                to = end
                from = minOf(from, end - least).coerceAtLeast(start)
                if (walkedBefore) past = minOf(past, from - step)
            }
            if (to + step <= end) bar(track, to + step, end)
            bar(today, from, to)
        } else {
            bar(track, start, end)
        }
        if (walkedBefore && past >= start) bar(before, start, past)
    }
}

private const val DETAIL_SEPARATOR = " · "
private val MAP_SIZE = 72.dp
private val BAR_HEIGHT = 6.dp
private val MIN_TODAY = 4.dp
private val PIECE_GAP = 2.dp
