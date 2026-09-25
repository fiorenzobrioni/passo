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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme

/**
 * A metric (Chiaro's §8.6): its mark and label, the value as a reading, where the world has a
 * scale for it that scale as a track, and always the line that says what the value means. A
 * metric with no honest second line does not get a tile.
 *
 * The tile is one node for a screen reader, read as [spoken].
 */
@Composable
fun MetricTile(
    icon: ImageVector,
    label: String,
    value: AnnotatedString,
    meaning: String,
    spoken: String,
    modifier: Modifier = Modifier,
    track: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.clearAndSetSemantics { contentDescription = spoken },
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = CircleShape,
                    modifier = Modifier.size(28.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(value, style = PassoTheme.type.readingValue, color = MaterialTheme.colorScheme.onSurface)
            if (track != null) {
                Box(modifier = Modifier.padding(vertical = 4.dp)) { track() }
            }
            Text(
                meaning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * A metric's own scale (Chiaro §8.6): the whole range drawn faint, the part up to the value at
 * full strength, the thresholds the meaning line switches at cut in as 2dp gaps, and a disc on
 * the value, ringed in the tile's ground with a hairline so the pale end of a ramp keeps an edge.
 *
 * @param colors the scale's colors, low to high; one color for a plain amount.
 */
@Composable
fun MetricTrack(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    thresholds: List<Float> = emptyList(),
) {
    val ground = MaterialTheme.colorScheme.surfaceContainerLow
    val outline = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp),
    ) {
        val barHeight = 6.dp.toPx()
        val disc = 12.dp.toPx()
        val left = disc / 2
        val width = size.width - disc
        val top = (size.height - barHeight) / 2
        fun xOf(v: Float) = left + width * ((v - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
        val brush = if (colors.size ==
            1
        ) {
            Brush.horizontalGradient(listOf(colors[0], colors[0]))
        } else {
            Brush.horizontalGradient(
                colors,
                startX = left,
                endX =
                left + width,
            )
        }
        val radius = CornerRadius(barHeight / 2)
        drawRoundRect(brush, Offset(left, top), Size(width, barHeight), radius, alpha = 0.35f)
        val x = xOf(value)
        drawRoundRect(brush, Offset(left, top), Size((x - left).coerceAtLeast(0f), barHeight), radius)
        val gap = 2.dp.toPx()
        for (threshold in thresholds) {
            drawRect(ground, Offset(xOf(threshold) - gap / 2, top), Size(gap, barHeight))
        }
        val discColor = if (colors.size == 1) colors[0] else lerpRamp(colors, (x - left) / width)
        drawCircle(ground, radius = disc / 2, center = Offset(x, size.height / 2))
        drawCircle(discColor, radius = disc / 2 - 2.dp.toPx(), center = Offset(x, size.height / 2))
        drawCircle(
            outline.copy(alpha = 0.6f),
            radius = disc / 2 - 2.dp.toPx(),
            center = Offset(x, size.height / 2),
            style = Stroke(1f),
        )
    }
}

private fun lerpRamp(colors: List<Color>, at: Float): Color {
    val position = at.coerceIn(0f, 1f) * (colors.size - 1)
    val index = position.toInt().coerceAtMost(colors.size - 2)
    return androidx.compose.ui.graphics.lerp(colors[index], colors[index + 1], position - index)
}
