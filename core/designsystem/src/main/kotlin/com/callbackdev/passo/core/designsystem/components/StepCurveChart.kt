package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.model.LearnedStep
import kotlin.math.ceil
import kotlin.math.floor

/**
 * A learnt step drawn against the step in use (PLANNING.md §11 Phase 17): the cadence across,
 * the step's length up; the learnt bins as points with the line through them, flat beyond its
 * ends within each gait, and the length in use as a dashed line, a step at the running cadence.
 * The running half is drawn only when the curve has a running part. One sentence for a screen
 * reader, [description]; the two lines are named under the chart.
 *
 * @param lengthLabel a length in the reader's units, for the scale on the right, and
 *   [topLengthLabel] the top one, which carries the unit.
 * @param lengthTick the scale's step, in metres: 5 cm, or 2 inches.
 * @param curveName what the solid line is («Measured on your outings»), and [inUseName] the
 *   dashed one, named by where it comes from.
 */
@Composable
fun StepCurveChart(
    curve: LearnedStep,
    inUse: StepLengths,
    lengthLabel: (Double) -> String,
    lengthTick: Double,
    topLengthLabel: (Double) -> String = lengthLabel,
    curveName: String,
    inUseName: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val line = colors.primary
    val dashed = colors.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    val withRunning = curve.points.any { it.cadence >= RUNNING_CADENCE }
    val lastCadence = if (withRunning) LAST_CADENCE else RUNNING_CADENCE
    val cadences = (FIRST_CADENCE..lastCadence step CADENCE_TICK).toList()
    // The step in use read at every cadence: a learnt step in use is a line too, not one length.
    val walkingInUse = (FIRST_CADENCE until RUNNING_CADENCE).map { it to inUse.forCadence(it) }
    val runningInUse = if (withRunning) {
        (RUNNING_CADENCE..lastCadence).map {
            it to inUse.forCadence(it)
        }
    } else {
        emptyList()
    }
    val lengths = curve.points.map { it.meters } + (walkingInUse + runningInUse).map { it.second }
    val low = floor(lengths.min() / lengthTick) * lengthTick
    val high = maxOf(ceil(lengths.max() / lengthTick) * lengthTick, low + lengthTick)
    val ticks = generateSequence(low) { it + lengthTick }.takeWhile { it <= high + lengthTick / 2 }.toList()
    val label: (Double) -> String = { if (it == ticks.last()) topLengthLabel(it) else lengthLabel(it) }
    val (gutterDp, axisDp) = chartMargins(measurer, labelStyle, ticks.map(label).maxBy { it.length })

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.testTag(STEP_CURVE_TAG).clearAndSetSemantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxWidth().height(PLOT_DP.dp + axisDp)) {
            val gutter = gutterDp.toPx()
            val top = 8.dp.toPx()
            val plotW = size.width - gutter
            val plotBottom = top + PLOT_DP.dp.toPx() - 8.dp.toPx()
            val plotH = plotBottom - top
            fun x(cadence: Double) = ((cadence - FIRST_CADENCE) / (lastCadence - FIRST_CADENCE) * plotW).toFloat()
            fun y(meters: Double) = (plotBottom - (meters - low) / (high - low) * plotH).toFloat()

            // The scale: a faint rule at each tick, its length on the right.
            for (tick in ticks) {
                drawLine(colors.outlineVariant, Offset(0f, y(tick)), Offset(plotW, y(tick)), 1.dp.toPx())
                val text = measurer.measure(label(tick), labelStyle.copy(color = colors.onSurfaceVariant))
                drawText(text, topLeft = Offset(plotW + 6.dp.toPx(), y(tick) - text.size.height / 2))
            }
            // Where walking ends and running begins, when both are drawn.
            if (withRunning) {
                drawLine(
                    colors.outlineVariant,
                    Offset(x(RUNNING_CADENCE.toDouble()), top),
                    Offset(x(RUNNING_CADENCE.toDouble()), plotBottom),
                    1.dp.toPx(),
                )
            }

            // The step in use, dashed: the walking length up to the running cadence, then the running one.
            val inUsePath = Path().apply {
                walkingInUse.forEachIndexed { index, (cadence, meters) ->
                    if (index ==
                        0
                    ) {
                        moveTo(x(cadence.toDouble()), y(meters))
                    } else {
                        lineTo(x(cadence.toDouble()), y(meters))
                    }
                }
                lineTo(x(RUNNING_CADENCE.toDouble()), y(walkingInUse.last().second))
                runningInUse.forEachIndexed { index, (cadence, meters) ->
                    if (index ==
                        0
                    ) {
                        moveTo(x(cadence.toDouble()), y(meters))
                    } else {
                        lineTo(x(cadence.toDouble()), y(meters))
                    }
                }
            }
            drawPath(
                inUsePath,
                dashed,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())),
                ),
            )

            // The learnt step: each gait's part, flat beyond its points up to its own edge.
            val parts = listOf(
                curve.points.filter { it.cadence < RUNNING_CADENCE } to (FIRST_CADENCE to RUNNING_CADENCE),
                curve.points.filter { it.cadence >= RUNNING_CADENCE } to (RUNNING_CADENCE to lastCadence),
            )
            for ((points, edges) in parts) {
                if (points.isEmpty()) continue
                val path = Path().apply {
                    moveTo(x(edges.first.toDouble()), y(points.first().meters))
                    points.forEach { lineTo(x(it.cadence), y(it.meters)) }
                    lineTo(x(edges.second.toDouble()), y(points.last().meters))
                }
                drawPath(path, line, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                for (point in points) {
                    drawCircle(colors.surface, 5.5.dp.toPx(), Offset(x(point.cadence), y(point.meters)))
                    drawCircle(line, 4.dp.toPx(), Offset(x(point.cadence), y(point.meters)))
                }
            }

            drawLine(colors.outlineVariant, Offset(0f, plotBottom), Offset(plotW, plotBottom), 1.dp.toPx())
            // The cadences under the axis; one that would touch the one before is left out (large text).
            var right = -Float.MAX_VALUE
            for (cadence in cadences) {
                val text = measurer.measure(cadence.toString(), labelStyle.copy(color = colors.onSurfaceVariant))
                val left = (x(cadence.toDouble()) - text.size.width / 2).coerceIn(0f, plotW - text.size.width)
                if (left < right + LABEL_GAP_DP.dp.toPx()) continue
                drawText(text, topLeft = Offset(left, plotBottom + 5.dp.toPx()))
                right = left + text.size.width
            }
        }
        Legend(line, curveName, dashed = false)
        Legend(dashed, inUseName, dashed = true)
    }
}

/** One line of the legend: its mark, then its name. */
@Composable
private fun Legend(color: Color, name: String, dashed: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 24.dp, height = 12.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val y = size.height / 2
                drawLine(
                    color,
                    Offset(0f, y),
                    Offset(size.width, y),
                    strokeWidth = if (dashed) 1.5.dp.toPx() else 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = if (dashed) {
                        PathEffect.dashPathEffect(
                            floatArrayOf(5.dp.toPx(), 4.dp.toPx()),
                        )
                    } else {
                        null
                    },
                )
                if (!dashed) drawCircle(color, 4.dp.toPx(), Offset(size.width / 2, y))
            }
        }
        Text(
            name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** The steps a minute along the axis: the learner's bins, from 80 to 180, labelled every 20. */
private const val FIRST_CADENCE = 80
private const val LAST_CADENCE = 180
private const val CADENCE_TICK = 20
private const val PLOT_DP = 152

/** Hook for the UI tests. */
const val STEP_CURVE_TAG = "step_curve"
