package com.callbackdev.passo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import kotlin.math.abs

/** One minute of the day as the chart reads it: today's total by then (null ahead of now) and a usual day's. */
@Immutable
data class TrendPoint(val minuteOfDay: Int, val steps: Int?, val usual: Int?)

/** The chart's data: running totals sampled every few minutes from midnight. */
@Immutable
data class DayTrend(
    /** Today's running total every [stepMinutes], from 00:00 up to now. */
    val today: List<Int>,
    val stepMinutes: Int,
    val nowMinute: Float,
    /** A usual day's running total every [usualStepMinutes], the whole day; null to hide it. */
    val usual: List<Int>?,
    val usualStepMinutes: Int,
    val goal: Int,
) {
    fun todayAt(minute: Float): Int? {
        if (minute > nowMinute + 0.5f || today.isEmpty()) return null
        return interpolate(today, stepMinutes, minute)
    }

    fun usualAt(minute: Float): Int? = usual?.let { interpolate(it, usualStepMinutes, minute) }

    private fun interpolate(totals: List<Int>, step: Int, minute: Float): Int {
        val position = (minute / step).coerceIn(0f, (totals.size - 1).toFloat())
        val lower = position.toInt()
        val upper = minOf(lower + 1, totals.size - 1)
        return (totals[lower] + (totals[upper] - totals[lower]) * (position - lower)).toInt()
    }
}

/**
 * The day so far as a shape (PLANNING.md §6.2, VISION "Day trend sparkline"): the running total
 * since midnight on a whole day's width, the goal as a line, a usual day of the same weekday as
 * a dashed line that goes on past now (what usually comes later), and the rest of the day
 * shaded, so the room left to walk in is visible too.
 *
 * The scale is the world's, not the data's: the whole day across, and up to the goal at least,
 * so a quiet morning looks quiet instead of filling the chart.
 *
 * **Touch**: a finger on it reads it. A tap pins the readout at that minute; a horizontal drag
 * moves it, with a light tick at each hour crossed; a vertical move is left to the page's
 * scroll. Past now it reads the usual day only. [readout] draws the line above the plot:
 * the pinned point, or null for now.
 *
 * It **draws itself in** the first time it is shown, left to right along time, the way the day
 * went (Chiaro's rain chart, 900 ms). Never grown up from the floor, which would draw for half
 * a second a day nobody walked. Not under reduced motion.
 *
 * One node for a screen reader, read as [description].
 */
@Composable
fun DayTrendChart(
    trend: DayTrend,
    goalLabel: String,
    hourLabels: List<Pair<Int, String>>,
    description: String,
    readout: @Composable (TrendPoint?) -> Unit,
    modifier: Modifier = Modifier,
    plotHeight: Dp = 148.dp,
) {
    val reduced = reducedMotion()
    var drawn by rememberSaveable { mutableStateOf(false) }
    val reveal = remember { Animatable(if (drawn || reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!drawn && !reduced) reveal.animateTo(1f, tween(REVEAL_MILLIS, easing = FastOutSlowInEasing))
        reveal.snapTo(1f)
        drawn = true
    }

    var pinned by remember { mutableStateOf<Float?>(null) }
    val point = pinned?.let { minute ->
        TrendPoint(minute.toInt(), trend.todayAt(minute), trend.usualAt(minute))
    }

    val haptics = LocalHapticFeedback.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val colors = MaterialTheme.colorScheme
    val goalInk = PassoTheme.colors.goal

    Column(modifier = modifier.clearAndSetSemantics { contentDescription = description }) {
        readout(point)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(plotHeight)
                .pointerInput(trend.nowMinute) {
                    val gutter = GUTTER_DP.dp.toPx()
                    fun minuteAt(x: Float) =
                        (x / (size.width - gutter) * MINUTES_PER_DAY).coerceIn(0f, MINUTES_PER_DAY.toFloat())
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val start = down.position
                        var scrubbing = false
                        var lastHour = -1
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (!scrubbing) pinned = minuteAt(start.x)
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
                                val minute = minuteAt(change.position.x)
                                pinned = minute
                                val hour = (minute / 60).toInt()
                                if (lastHour != -1 &&
                                    hour != lastHour
                                ) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                lastHour = hour
                                change.consume()
                            }
                        }
                    }
                },
        ) {
            val gutter = GUTTER_DP.dp.toPx()
            val axis = AXIS_DP.dp.toPx()
            val top = 6.dp.toPx()
            val plotW = size.width - gutter
            val plotBottom = size.height - axis
            val plotH = plotBottom - top
            val maxToday = trend.today.maxOrNull() ?: 0
            val maxUsual = trend.usual?.maxOrNull() ?: 0
            val yMax = maxOf(trend.goal, maxToday, maxUsual, 1) * HEADROOM
            fun x(minute: Float) = plotW * minute / MINUTES_PER_DAY
            fun y(value: Int) = plotBottom - plotH * value / yMax

            // The part of the day still ahead.
            val nowX = x(trend.nowMinute.coerceIn(0f, MINUTES_PER_DAY.toFloat()))
            drawRect(colors.surfaceContainerHigh.copy(alpha = 0.55f), Offset(nowX, top), Size(plotW - nowX, plotH))
            // Baseline.
            drawLine(
                colors.outlineVariant,
                Offset(0f, plotBottom),
                Offset(plotW, plotBottom),
                strokeWidth = 1.dp.toPx(),
            )

            // The goal, labelled in the gutter.
            val goalY = y(trend.goal)
            drawLine(goalInk.copy(alpha = 0.7f), Offset(0f, goalY), Offset(plotW, goalY), strokeWidth = 1.5.dp.toPx())
            val goalText = measurer.measure(goalLabel, labelStyle.copy(color = goalInk))
            drawText(goalText, topLeft = Offset(plotW + 6.dp.toPx(), goalY - goalText.size.height / 2))

            // Hours under the axis.
            for ((minute, label) in hourLabels) {
                val text = measurer.measure(label, labelStyle.copy(color = colors.onSurfaceVariant))
                val cx = x(minute.toFloat())
                val left = (cx - text.size.width / 2).coerceIn(0f, plotW - text.size.width)
                drawLine(
                    colors.outlineVariant,
                    Offset(cx, plotBottom),
                    Offset(cx, plotBottom + 3.dp.toPx()),
                    strokeWidth = 1.dp.toPx(),
                )
                drawText(text, topLeft = Offset(left, plotBottom + 5.dp.toPx()))
            }

            // A usual day, dashed, the whole day long.
            trend.usual?.let { usual ->
                val path = Path()
                usual.forEachIndexed { i, total ->
                    val px = x((i * trend.usualStepMinutes).toFloat())
                    if (i == 0) path.moveTo(px, y(total)) else path.lineTo(px, y(total))
                }
                drawPath(
                    path,
                    colors.onSurfaceVariant.copy(alpha = 0.8f),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                        cap = StrokeCap.Round,
                    ),
                )
            }

            // Today, revealed along time.
            if (trend.today.isNotEmpty()) {
                val line = Path()
                val area = Path()
                area.moveTo(0f, plotBottom)
                trend.today.forEachIndexed { i, total ->
                    val minute = (i * trend.stepMinutes).toFloat().coerceAtMost(trend.nowMinute)
                    val px = x(minute)
                    if (i == 0) line.moveTo(px, y(total)) else line.lineTo(px, y(total))
                    area.lineTo(px, y(total))
                }
                val endX = x(minOf(((trend.today.size - 1) * trend.stepMinutes).toFloat(), trend.nowMinute))
                area.lineTo(endX, plotBottom)
                area.close()
                clipRect(right = nowX * reveal.value + 2.dp.toPx()) {
                    drawPath(
                        area,
                        Brush.verticalGradient(
                            listOf(colors.primary.copy(alpha = 0.28f), colors.primary.copy(alpha = 0.03f)),
                            top,
                            plotBottom,
                        ),
                    )
                    drawPath(
                        line,
                        colors.primary,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
                if (reveal.value >= 1f) {
                    val head = Offset(endX, y(trend.today.last()))
                    drawCircle(colors.surface, radius = 6.dp.toPx(), center = head)
                    drawCircle(colors.primary, radius = 4.dp.toPx(), center = head)
                }
            }

            // The pinned minute.
            point?.let { p ->
                val px = x(p.minuteOfDay.toFloat())
                drawLine(colors.outline, Offset(px, top), Offset(px, plotBottom), strokeWidth = 1.dp.toPx())
                p.usual?.let { drawCircle(colors.onSurfaceVariant, radius = 3.5.dp.toPx(), center = Offset(px, y(it))) }
                p.steps?.let {
                    drawCircle(colors.surface, radius = 6.dp.toPx(), center = Offset(px, y(it)))
                    drawCircle(colors.primary, radius = 4.dp.toPx(), center = Offset(px, y(it)))
                }
            }
        }
    }
}

private const val MINUTES_PER_DAY = 1_440
private const val HEADROOM = 1.12f
private const val GUTTER_DP = 44
private const val AXIS_DP = 20
private const val REVEAL_MILLIS = 900
