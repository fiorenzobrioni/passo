package com.callbackdev.passo.feature.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.passo.core.designsystem.components.MetricTile
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin

/** A page's sentence and the line under it: one sentence before any number (Chiaro's rule). */
@Composable
internal fun PageHeadline(sentence: String, detail: String?, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) { },
    ) {
        Text(sentence, style = MaterialTheme.typography.titleLarge)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** A chart on its ground: title and key, the readout, the plot, the line that says how to read it. */
@Composable
internal fun ChartCard(
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
    legend: @Composable () -> Unit = {},
    readout: @Composable () -> Unit,
    chart: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                legend()
            }
            readout()
            chart()
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * The line above a chart: what the finger is on, or a hint when it is on nothing, and the way
 * into it ([action]) when there is one. Its height does not move with its content, so the chart
 * does not jump under the finger.
 */
@Composable
internal fun ChartReadout(text: AnnotatedString, action: String? = null, onAction: () -> Unit = {}) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 40.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (action != null) {
            TextButton(onClick = onAction) {
                Text(action)
                Icon(
                    PassoIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.padding(start = 2.dp).size(18.dp),
                )
            }
        }
    }
}

/** A readout's parts: the when in bold, the how many in the accent, the rest plain. */
@Composable
internal fun readoutText(time: String, value: String?, extra: String? = null): AnnotatedString {
    val primary = MaterialTheme.colorScheme.primary
    return androidx.compose.ui.text.buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
            append(time)
        }
        if (value != null) {
            append("  ·  ")
            withStyle(SpanStyle(color = primary, fontWeight = FontWeight.SemiBold)) { append(value) }
        }
        if (extra != null) {
            append("  ·  ")
            append(extra)
        }
    }
}

/** The chart's key: a short line for the goal, a shaded block for walks. */
@Composable
internal fun ChartLegend(goal: Boolean, walks: Boolean) {
    val goalInk = PassoTheme.colors.goal
    val walkInk = MaterialTheme.colorScheme.tertiary
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (walks) {
            LegendItem(stringResource(R.string.history_chart_legend_walks)) {
                Canvas(Modifier.width(14.dp).height(8.dp)) {
                    drawRoundRect(
                        walkInk,
                        topLeft = Offset(0f, size.height / 2 - 2.dp.toPx()),
                        size = Size(size.width, 4.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()),
                    )
                }
            }
        }
        if (goal) {
            LegendItem(stringResource(R.string.history_chart_legend_goal)) {
                Canvas(Modifier.width(14.dp).height(8.dp)) {
                    drawLine(
                        goalInk,
                        Offset(0f, size.height / 2),
                        Offset(size.width, size.height / 2),
                        strokeWidth = 1.5.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        swatch()
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A metric tile in History's voice. */
@Composable
internal fun HistoryTile(
    icon: ImageVector,
    label: String,
    value: AnnotatedString,
    spokenValue: String,
    meaning: String,
    modifier: Modifier = Modifier,
    track: (@Composable () -> Unit)? = null,
) {
    MetricTile(
        icon = icon,
        label = label,
        value = value,
        meaning = meaning,
        spoken = stringResource(R.string.history_metric_spoken, label, spokenValue, meaning),
        modifier = modifier,
        track = track,
    )
}

/** The unit beside a reading, set small (Chiaro's reading style). */
internal val UnitStyle = SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
