package com.callbackdev.passo.core.designsystem.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Today's ring: the day's steps against the goal, and a notch where a usual day of the same
 * weekday stands at this hour. Ahead of the notch is ahead of your usual self; the gap between
 * the two is the pace the headline puts in words, readable at a glance before it is read.
 *
 * The arc sweeps in from zero when the ring first appears and follows the count with Chiaro's
 * spatial spring; a met goal turns it to the goal color, and the moment it is met (with the
 * screen on, or the first time the screen sees it that day, [celebrate]) the ring blooms once:
 * a wave out from the ring, gone in a second. None of it under reduced motion, where the ring
 * simply is where it should be.
 *
 * Silent to a screen reader: the caller gives the whole ring one description.
 *
 * @param progress the share of the goal walked, not capped (1.2 is a goal passed by a fifth).
 * @param usualProgress where a usual day stands by now, as a share of the goal; null hides the notch.
 */
@Composable
fun ProgressRing(
    progress: Float,
    usualProgress: Float?,
    reached: Boolean,
    celebrate: Boolean,
    onCelebrated: () -> Unit,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 18.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduced = reducedMotion()
    val sweep = remember { Animatable(if (reduced) progress.coerceIn(0f, 1f) else 0f) }
    val tint = remember { Animatable(if (reached) 1f else 0f) }
    val bloom = remember { Animatable(0f) }
    val currentOnCelebrated = rememberUpdatedState(onCelebrated)

    LaunchedEffect(progress, reduced) {
        val target = progress.coerceIn(0f, 1f)
        if (reduced) sweep.snapTo(target) else sweep.animateTo(target, spring(dampingRatio = 0.85f, stiffness = 120f))
    }
    LaunchedEffect(reached, reduced) {
        val target = if (reached) 1f else 0f
        if (reduced) tint.snapTo(target) else tint.animateTo(target, tween(600, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(celebrate, reduced) {
        if (!celebrate) return@LaunchedEffect
        if (!reduced) {
            // After the arc has closed, so the bloom reads as its consequence.
            while (sweep.value < 0.99f) kotlinx.coroutines.delay(16)
            bloom.snapTo(0f)
            bloom.animateTo(1f, tween(BLOOM_MILLIS, easing = FastOutSlowInEasing))
            bloom.snapTo(0f)
        }
        currentOnCelebrated.value()
    }

    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val accent = MaterialTheme.colorScheme.primary
    val goal = PassoTheme.colors.goal
    val goalTrack = PassoTheme.colors.goalContainer
    val notch = MaterialTheme.colorScheme.onSurface
    val ground = MaterialTheme.colorScheme.surface

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2 + BLOOM_REACH_DP.dp.toPx()
            val diameter = size.minDimension - inset * 2
            val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
            val arcSize = Size(diameter, diameter)
            val t = tint.value
            val arcColor = lerp(accent, goal, t)

            drawArc(lerp(track, goalTrack, t), 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
            if (sweep.value > 0f) {
                drawArc(
                    arcColor,
                    -90f,
                    360f * sweep.value,
                    false,
                    topLeft,
                    arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                drawHead(center, diameter / 2, sweep.value, stroke, ground)
            }
            if (usualProgress != null && usualProgress > 0f && usualProgress <= 1f && t < 1f) {
                drawNotch(center, diameter / 2, usualProgress, stroke, notch.copy(alpha = 1f - t), ground)
            }
            if (bloom.value > 0f) {
                val grow = bloom.value
                drawCircle(
                    color = goal.copy(alpha = (1f - grow) * 0.5f),
                    radius = diameter / 2 + grow * BLOOM_REACH_DP.dp.toPx(),
                    center = center,
                    style = Stroke(stroke * (1f - grow * 0.7f)),
                )
            }
        }
        Box(
            modifier = Modifier.padding(strokeWidth + BLOOM_REACH_DP.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** A dot of the ground at the arc's end: the ring's leading edge, so a moving ring reads as moving. */
private fun DrawScope.drawHead(center: Offset, radius: Float, share: Float, stroke: Float, ground: Color) {
    val angle = (share * 360f - 90f) * DEG
    val at = Offset(center.x + radius * cos(angle), center.y + radius * sin(angle))
    drawCircle(ground.copy(alpha = 0.9f), radius = stroke * 0.18f, center = at)
}

/** The usual-day notch: a bar across the track, on a halo of the ground so it reads over any arc. */
private fun DrawScope.drawNotch(center: Offset, radius: Float, share: Float, stroke: Float, ink: Color, ground: Color) {
    val angle = (share * 360f - 90f) * DEG
    val direction = Offset(cos(angle), sin(angle))
    val reach = stroke / 2 + 4.dp.toPx()
    val from = center + direction * (radius - reach)
    val to = center + direction * (radius + reach)
    drawLine(ground, from, to, strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
    drawLine(ink, from, to, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
}

private const val DEG = (PI / 180).toFloat()
private const val BLOOM_MILLIS = 1_100
private const val BLOOM_REACH_DP = 14
