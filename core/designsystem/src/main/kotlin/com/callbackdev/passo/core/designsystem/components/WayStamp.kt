package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * One stamp of a way's credential (PLANNING.md §11 Phase 11): the stage's name and the day it
 * was reached, inside a double ring, set down a little askew as a stamp is. A generic stamp,
 * drawn in code, never an official one. Each stage keeps its own tilt and ink ([seed]), so a
 * credential reads the same every time it is opened.
 *
 * A stage not reached yet is its place on the page: a dotted ring and its name, no ink.
 *
 * @param date the day it was reached, null when it is not yet.
 * @param spoken what TalkBack says: the name and the day, or that it is still ahead.
 */
@Composable
fun WayStamp(name: String, date: String?, seed: Int, spoken: String, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val inks = listOf(PassoTheme.colors.goal, scheme.primary, scheme.secondary, scheme.tertiary)
    val reached = date != null
    val ink = if (reached) inks[abs(seed) % inks.size] else scheme.outline
    val text = if (reached) ink else scheme.onSurfaceVariant
    val tilt = if (reached) ((abs(seed) / inks.size) % (2 * MAX_TILT + 1) - MAX_TILT).toFloat() else 0f
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(STAMP_SIZE)
            .clearAndSetSemantics { contentDescription = spoken }
            .graphicsLayer { rotationZ = tilt },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = size.minDimension / 2 - 2.dp.toPx()
            if (reached) {
                drawCircle(ink, outer, style = Stroke(2.dp.toPx()))
                drawCircle(ink, outer - 5.dp.toPx(), style = Stroke(1.dp.toPx()))
                // Small marks between the rings, like the teeth of a rubber stamp.
                val marks = 24
                for (i in 0 until marks) {
                    val angle = 2 * PI * i / marks
                    val r = outer - 2.5.dp.toPx()
                    drawCircle(
                        ink.copy(alpha = MARK_ALPHA),
                        0.9.dp.toPx(),
                        center + Offset(
                            (r * cos(angle)).toFloat(),
                            (r * sin(angle)).toFloat(),
                        ),
                    )
                }
            } else {
                val dots = PathEffect.dashPathEffect(floatArrayOf(2.dp.toPx(), 4.dp.toPx()))
                drawCircle(ink, outer, style = Stroke(1.5.dp.toPx(), pathEffect = dots))
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            StampName(
                text = if (reached) name.uppercase() else name,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = text,
                    fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    letterSpacing = if (reached) 0.5.sp else 0.sp,
                ),
            )
            if (date != null) {
                BasicText(
                    text = date,
                    style = MaterialTheme.typography.labelSmall.copy(color = text, textAlign = TextAlign.Center),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 6.sp, maxFontSize = 11.sp, stepSize = 0.5.sp),
                )
            }
        }
    }
}

/**
 * The stage's name in the largest size, from 12 down to 7 sp, at which it takes three lines at
 * most and no word is cut: "Echevennoz" must not wrap in the middle as a plain auto-size would.
 */
@Composable
private fun StampName(text: String, style: TextStyle) {
    val measurer = rememberTextMeasurer()
    // A hyphen is a place to break ("Pont-Saint- / Martin"), so a long hyphenated name never
    // breaks inside one of its words instead.
    val breakable = text.replace("-", "-\u200B")
    BoxWithConstraints {
        val width = constraints.maxWidth
        val fitted = remember(breakable, style, width) {
            var size = NAME_MAX
            while (size > NAME_MIN) {
                val sized = style.copy(fontSize = size.sp)
                val words = text.split(' ', '-').all { measurer.measure(it, sized).size.width <= width }
                val lines = measurer.measure(breakable, sized, constraints = Constraints(maxWidth = width)).lineCount
                if (words && lines <= NAME_LINES) break
                size -= NAME_STEP
            }
            style.copy(fontSize = size.sp)
        }
        BasicText(text = breakable, style = fitted, maxLines = NAME_LINES, modifier = Modifier.fillMaxWidth())
    }
}

/** A stage's seed for [WayStamp]: its key, so the same stage always looks the same. */
fun stampSeed(key: String): Int = key.hashCode()

private val STAMP_SIZE = 104.dp
private const val MAX_TILT = 7
private const val MARK_ALPHA = 0.6f
private const val NAME_MAX = 12f
private const val NAME_MIN = 7f
private const val NAME_STEP = 0.5f
private const val NAME_LINES = 3
