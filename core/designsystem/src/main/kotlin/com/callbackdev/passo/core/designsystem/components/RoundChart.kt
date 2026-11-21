package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme

/** One walk of a round on its chart: its day, and its time in motion brought to the round's usual length. */
class RoundChartPoint(val epochDay: Long, val millis: Long)

/**
 * A usual round's times over the weeks (PLANNING.md §11 Phase 20, «Your rounds»): a dot for each
 * walk at its day, as high as it was long, joined by a quiet line, and the usual as a dashed line
 * across, in the goal's colour. Lower is faster. The latest walk is the larger dot. Its slowest and
 * fastest times stand at the left, its first and last days under it; one image to TalkBack, said
 * by [description] («Six walks since March; the last three faster than usual»).
 *
 * Never a record board: no fastest mark, no medal (docs/adr/0016-location.md, decision 16).
 */
@Composable
fun RoundTimesChart(
    points: List<RoundChartPoint>,
    usualMillis: Long,
    slowest: String,
    fastest: String,
    firstDay: String,
    lastDay: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return
    val accent = MaterialTheme.colorScheme.primary
    val usual = PassoTheme.colors.goal
    val halo = MaterialTheme.colorScheme.surfaceContainerLow
    val rule = MaterialTheme.colorScheme.outlineVariant
    val labels = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag(RoundChartTags.TIMES).clearAndSetSemantics { contentDescription = description },
    ) {
        Text(slowest, style = labels, color = ink)
        Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT)) {
            val times = points.map { it.millis } + usualMillis
            val low = times.min()
            val high = times.max()
            // A round walked in about the same time each day is drawn as near as it was, not as noise
            // stretched to the box's height.
            val span = maxOf(high - low, MIN_SPAN_MILLIS).toFloat()
            val middle = (high + low) / 2f
            val inset = DOT_LARGE.toPx() + 2.dp.toPx()
            val first = points.first().epochDay
            val days = (points.last().epochDay - first).toFloat()
            fun x(index: Int, day: Long): Float {
                // By the day where the walks span days; evenly where they were all on one.
                val share = when {
                    days > 0f -> (day - first) / days
                    points.size > 1 -> index / (points.size - 1f)
                    else -> 0.5f
                }
                return inset + share * (size.width - 2 * inset)
            }
            fun y(millis: Long): Float = size.height / 2 - (millis - middle) / span * (size.height - 2 * inset)
            drawLine(rule, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            val usualY = y(usualMillis)
            drawLine(
                usual,
                Offset(0f, usualY),
                Offset(size.width, usualY),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
            )
            if (points.size > 1) {
                val path = Path()
                points.forEachIndexed { index, point ->
                    val px = x(index, point.epochDay)
                    val py = y(point.millis)
                    if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(
                    path,
                    accent.copy(alpha = LINE_ALPHA),
                    style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
            points.forEachIndexed { index, point ->
                val center = Offset(x(index, point.epochDay), y(point.millis))
                val latest = index == points.lastIndex
                val radius = if (latest) DOT_LARGE.toPx() else DOT.toPx()
                drawCircle(halo, radius + 1.5.dp.toPx(), center)
                drawCircle(accent, radius, center)
            }
        }
        Text(fastest, style = labels, color = ink)
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(firstDay, style = labels, color = ink)
            Text(lastDay, style = labels, color = ink)
        }
    }
}

private val CHART_HEIGHT = 120.dp
private val DOT = 3.5.dp
private val DOT_LARGE = 5.dp

/** Under two minutes between the slowest and the fastest, the dots stand as close as they were. */
private const val MIN_SPAN_MILLIS = 120_000L
private const val LINE_ALPHA = 0.45f

/** Hooks for the UI tests. */
object RoundChartTags {
    const val TIMES = "round_times"
}
