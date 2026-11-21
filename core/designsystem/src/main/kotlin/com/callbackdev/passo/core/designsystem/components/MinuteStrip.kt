package com.callbackdev.passo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.sessions.OutingMinutes

/**
 * An outing minute by minute (PLANNING.md §11 Phase 21, step 21.3): a bar for each minute, or
 * for each few once the walk is long ([OutingMinutes.bars]), its height the steps a minute, in
 * [accent] where it was at the outing's pace and in a quiet ink where it was not, counted and
 * shown, never hidden. A pause is a gap; a minute walked with no steps, a stub, because it did
 * happen. Past a goal, a thin line in the goal's colour marks the minute it was reached.
 *
 * While the outing counts the strip keeps room on its right and grows into it, the newest minute
 * sliding in from the last; under reduced motion it jumps there. Canvas code, drawn only while it
 * is on screen, like every chart here. To TalkBack it is one sentence, [description].
 */
@Composable
fun MinuteStrip(
    minutes: OutingMinutes,
    description: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    height: Dp = MinuteStripHeight,
) {
    val quiet = MaterialTheme.colorScheme.outline
    val goal = PassoTheme.colors.goal
    val ground = MaterialTheme.colorScheme.surfaceContainerLow
    val reduced = reducedMotion()
    // How far the bars are drawn, in minutes: it follows the outing's last minute, from nothing on
    // the first frame, so the strip grows to the right as it is walked.
    val shown = remember { Animatable(if (reduced) minutes.count.toFloat() else 0f) }
    LaunchedEffect(minutes.count, reduced) {
        val target = minutes.count.toFloat()
        if (reduced) {
            shown.snapTo(target)
        } else {
            val far = target - shown.value > GROW_FAR_MINUTES
            shown.animateTo(target, tween(if (far) GROW_FIRST_MILLIS else GROW_MILLIS, easing = FastOutSlowInEasing))
        }
    }
    Spacer(
        modifier
            .fillMaxWidth()
            .height(height)
            .testTag(MinuteStripTags.STRIP)
            .clearAndSetSemantics { contentDescription = description }
            .drawWithCache {
                val gap = BAR_GAP.toPx()
                val bars = minutes.bars(maxBars = (size.width / (BAR_MIN.toPx() + gap)).toInt())
                val top = minutes.scaleTop(bars).toFloat()
                val perMinute = size.width / minutes.spanMinutes
                val stub = STUB.toPx()
                val radius = BAR_RADIUS.toPx()
                onDrawBehind {
                    val until = shown.value * perMinute
                    for (bar in bars) {
                        if (bar.paused) continue
                        val left = bar.from * perMinute
                        if (left >= until) break
                        val right = minOf((bar.from + bar.minutes) * perMinute - gap, until)
                        if (right <= left) continue
                        val tall = maxOf(size.height * (bar.stepsPerMinute / top).toFloat(), stub)
                        drawRoundRect(
                            color = if (bar.atPace) accent else quiet,
                            topLeft = Offset(left, size.height - tall),
                            size = Size(right - left, tall),
                            cornerRadius = CornerRadius(minOf(radius, (right - left) / 2)),
                        )
                    }
                    // The goal's minute, over the bars on an edge of the card's own colour, so it
                    // reads as a line across a bar of any ink.
                    val goalAt = minutes.goalAt
                    if (goalAt != null && goalAt <= shown.value) {
                        val x = (goalAt * perMinute).toFloat().coerceIn(0f, size.width)
                        val line = GOAL_LINE.toPx()
                        drawLine(
                            ground,
                            Offset(x, 0f),
                            Offset(x, size.height),
                            strokeWidth =
                            line + 2 * GOAL_EDGE.toPx(),
                        )
                        drawLine(goal, Offset(x, GOAL_DOT.toPx()), Offset(x, size.height), strokeWidth = line)
                        drawCircle(ground, GOAL_DOT.toPx() + GOAL_EDGE.toPx(), Offset(x, GOAL_DOT.toPx()))
                        drawCircle(goal, GOAL_DOT.toPx(), Offset(x, GOAL_DOT.toPx()))
                    }
                }
            },
    )
}

/** The strip's height on a card; its page draws it taller. */
val MinuteStripHeight = 56.dp

/**
 * How many bars a strip [width] wide holds, each at least two dp wide with one between: what a
 * caller reads [OutingMinutes.groupFor] with, to say how many minutes a bar is.
 */
fun minuteStripBars(width: Dp): Int = (width / (BAR_MIN + BAR_GAP)).toInt()

private val BAR_MIN = 2.dp
private val BAR_GAP = 1.dp
private val BAR_RADIUS = 2.dp
private val STUB = 1.5.dp
private val GOAL_LINE = 1.5.dp
private val GOAL_EDGE = 1.dp
private val GOAL_DOT = 2.5.dp

/** A minute more slides in quickly; the first draw, or a long wait, sweeps in from the left. */
private const val GROW_MILLIS = 400
private const val GROW_FIRST_MILLIS = 700
private const val GROW_FAR_MINUTES = 3f

/** Hooks for the UI tests. */
object MinuteStripTags {
    const val STRIP = "minute_strip"
}
