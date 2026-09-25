package com.callbackdev.passo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * One bar: a day of a week or a month, a month of a year, an hour of a day.
 *
 * @property value what the bar stands for; null draws nothing (a day still to come, or from
 *   before counting began). Zero draws a stub, because a day of zero steps did happen.
 * @property goal the goal the bar is measured against, drawn as a step line over its slot; null
 *   where there is none (the hours of a day).
 * @property met the goal was met: the bar wears the goal's color, and says so in words too.
 * @property current the bar is still moving (today, this hour): its label is marked.
 */
@Immutable
data class ChartBar(val value: Int?, val goal: Int? = null, val met: Boolean = false, val current: Boolean = false)

/** A stretch marked on the chart, in bars: a walk from 10:12 to 10:47 is 10.2 to 10.78 hours. */
@Immutable
data class ChartSpan(val start: Float, val end: Float)

/**
 * The bar chart of History (PLANNING.md §11 Phase 5), for hours, days and months alike.
 *
 * The scale is the world's: from zero, and up to the goal at least ([scaleFloor] where there
 * is no goal), so a quiet week looks quiet. The goal is a line that steps with each bar's own
 * goal, since every day keeps the goal it had. A bar at its goal wears the goal's color.
 * [spans] are shaded across the plot and drawn as capsules under the axis: the walks of a day.
 *
 * **Touch**: a tap selects a bar (again to let it go), a horizontal drag moves along the bars
 * with a light tick at each, a vertical move is left to the page. What the selection shows is
 * the caller's, above the chart: the chart only dims the other bars.
 *
 * **Screen reader**: the plot reads as [description]; each bar is a node of its own, read as
 * [barDescription] and selectable with a double tap.
 */
@Composable
fun BarChart(
    bars: List<ChartBar>,
    axisLabels: List<Pair<Int, String>>,
    description: String,
    barDescription: (Int) -> String,
    selected: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    scaleLabel: (Int) -> String = { it.toString() },
    spans: List<ChartSpan> = emptyList(),
    scaleFloor: Int = 0,
    plotHeight: Dp = 168.dp,
) {
    val reduced = reducedMotion()
    var drawn by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (drawn || reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!drawn && !reduced) reveal.animateTo(1f, tween(REVEAL_MILLIS, easing = FastOutSlowInEasing))
        reveal.snapTo(1f)
        drawn = true
    }

    val haptics = LocalHapticFeedback.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val colors = MaterialTheme.colorScheme
    val goalInk = PassoTheme.colors.goal
    val chosen = selected
    val currentSelected by rememberUpdatedState(selected)
    val select by rememberUpdatedState(onSelect)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(plotHeight + AXIS_DP.dp + if (spans.isEmpty()) 0.dp else LANE_DP.dp)
            .pointerInput(bars.size) {
                val gutter = GUTTER_DP.dp.toPx()
                fun barAt(x: Float): Int = (x / ((size.width - gutter) / bars.size)).toInt().coerceIn(0, bars.size - 1)
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = down.position
                    var scrubbing = false
                    var last = -1
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            if (!scrubbing && start.x < size.width - gutter) {
                                val index = barAt(start.x)
                                select(if (currentSelected == index) null else index)
                            }
                            break
                        }
                        val dx = change.position.x - start.x
                        val dy = change.position.y - start.y
                        if (!scrubbing) {
                            if (abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                                scrubbing = true
                            } else if (abs(dy) > touchSlop) {
                                break // a scroll: the page's, not ours
                            }
                        }
                        if (scrubbing) {
                            val index = barAt(change.position.x)
                            if (index != last) {
                                if (last != -1) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                select(index)
                                last = index
                            }
                            change.consume()
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.matchParentSize().semantics { contentDescription = description }) {
            val gutter = GUTTER_DP.dp.toPx()
            val top = 8.dp.toPx()
            val plotW = size.width - gutter
            val plotBottom = top + plotHeight.toPx() - 8.dp.toPx()
            val plotH = plotBottom - top
            val slot = plotW / bars.size
            val maxValue = bars.maxOf { maxOf(it.value ?: 0, it.goal ?: 0) }
            val yMax = maxOf(maxValue, scaleFloor, 1) * HEADROOM
            fun y(value: Int) = plotBottom - plotH * value / yMax

            // The walks, behind everything.
            for (span in spans) {
                val left = span.start * slot
                val right = maxOf(span.end * slot, left + 2.dp.toPx())
                drawRect(colors.tertiary.copy(alpha = 0.10f), Offset(left, top), Size(right - left, plotH))
            }

            // A scale line where there is no goal to read the bars against.
            val hasGoal = bars.any { it.goal != null }
            if (!hasGoal && maxValue > 0) {
                val tick = niceTick(yMax / HEADROOM)
                val tickY = y(tick)
                drawLine(
                    colors.outlineVariant,
                    Offset(0f, tickY),
                    Offset(plotW, tickY),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx())),
                )
                val text = measurer.measure(scaleLabel(tick), labelStyle.copy(color = colors.onSurfaceVariant))
                drawText(text, topLeft = Offset(plotW + 6.dp.toPx(), tickY - text.size.height / 2))
            }

            // The bars, revealed along the axis the first time.
            val barWidth = slot * if (bars.size > 14) 0.62f else 0.5f
            val radius = minOf(barWidth / 2, 4.dp.toPx())
            val stub = 2.dp.toPx()
            clipRect(right = plotW * reveal.value + 1f) {
                bars.forEachIndexed { i, bar ->
                    val value = bar.value ?: return@forEachIndexed
                    val left = slot * i + (slot - barWidth) / 2
                    val dimmed = chosen != null && chosen != i
                    val alpha = if (dimmed) 0.35f else 1f
                    if (value <= 0) {
                        drawRect(
                            colors.outlineVariant.copy(alpha = alpha),
                            Offset(left, plotBottom - stub),
                            Size(barWidth, stub),
                        )
                    } else {
                        val color = if (bar.met) goalInk else colors.primary
                        val barTop = minOf(y(value), plotBottom - stub)
                        drawTopRounded(color.copy(alpha = alpha), left, barTop, barWidth, plotBottom - barTop, radius)
                    }
                }
            }

            // Baseline.
            drawLine(
                colors.outlineVariant,
                Offset(0f, plotBottom),
                Offset(plotW, plotBottom),
                strokeWidth = 1.dp.toPx(),
            )

            // The goal, stepping with each bar's own, labelled with the latest.
            if (hasGoal) {
                val path = Path()
                var previousY: Float? = null
                bars.forEachIndexed { i, bar ->
                    val goal = bar.goal ?: return@forEachIndexed
                    val gy = y(goal)
                    val x0 = slot * i
                    if (previousY == null) path.moveTo(x0, gy) else path.lineTo(x0, gy)
                    path.lineTo(x0 + slot, gy)
                    previousY = gy
                }
                drawPath(
                    path,
                    goalInk.copy(alpha = 0.75f),
                    style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round),
                )
                bars.lastOrNull { it.goal != null }?.goal?.let { goal ->
                    val text = measurer.measure(scaleLabel(goal), labelStyle.copy(color = goalInk))
                    drawText(text, topLeft = Offset(plotW + 6.dp.toPx(), y(goal) - text.size.height / 2))
                }
            }

            // The walks' lane, under the axis.
            var labelTop = plotBottom + 5.dp.toPx()
            if (spans.isNotEmpty()) {
                val laneY = plotBottom + LANE_DP.dp.toPx() / 2 + 1.dp.toPx()
                val thickness = 4.dp.toPx()
                // Never shorter than a bar is wide: a 15-minute walk is a quarter of an hour's
                // slot, and a mark that small would not be seen.
                val minimum = maxOf(barWidth, thickness * 2)
                for (span in spans) {
                    val middle = (span.start + span.end) / 2 * slot
                    val half = maxOf((span.end - span.start) * slot, minimum) / 2
                    val left = (middle - half).coerceAtLeast(0f)
                    val right = (middle + half).coerceAtMost(plotW)
                    drawRoundRect(
                        colors.tertiary,
                        Offset(left, laneY - thickness / 2),
                        Size(right - left, thickness),
                        CornerRadius(thickness / 2),
                    )
                }
                labelTop += LANE_DP.dp.toPx()
            }

            // Labels under the axis; the moving bar's in the accent, with a mark.
            for ((index, label) in axisLabels) {
                if (index !in bars.indices) continue
                val current = bars[index].current
                val style = if (current) {
                    labelStyle.copy(color = colors.primary, fontWeight = FontWeight.Bold)
                } else {
                    labelStyle.copy(color = colors.onSurfaceVariant)
                }
                val text = measurer.measure(label, style)
                val cx = slot * index + slot / 2
                val left = (cx - text.size.width / 2).coerceIn(0f, plotW - text.size.width)
                drawText(text, topLeft = Offset(left, labelTop))
            }
        }

        // One node per bar for a screen reader, over the plot; they take no touches.
        Row(Modifier.matchParentSize().padding(end = GUTTER_DP.dp)) {
            bars.indices.forEach { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .semantics {
                            contentDescription = barDescription(i)
                            this.selected = chosen == i
                            onClick {
                                select(if (currentSelected == i) null else i)
                                true
                            }
                        },
                )
            }
        }
    }
}

private fun DrawScope.drawTopRounded(
    color: androidx.compose.ui.graphics.Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    radius: Float,
) {
    val r = CornerRadius(minOf(radius, height), minOf(radius, height))
    val path = Path().apply {
        addRoundRect(RoundRect(left, top, left + width, top + height, r, r, CornerRadius.Zero, CornerRadius.Zero))
    }
    drawPath(path, color)
}

/** A round number at or under [max] for the one scale line: 1, 2 or 5 times a power of ten. */
internal fun niceTick(max: Float): Int {
    if (max < 1f) return 1
    val magnitude = 10.0.pow(floor(log10(max.toDouble())))
    val lead = max / magnitude
    val nice = when {
        lead >= 5 -> 5
        lead >= 2 -> 2
        else -> 1
    }
    return (nice * magnitude).toInt()
}

private const val HEADROOM = 1.1f
private const val GUTTER_DP = 44
private const val AXIS_DP = 20
private const val LANE_DP = 10
private const val REVEAL_MILLIS = 700
