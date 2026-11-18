package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme

/**
 * One kilometre (or mile) of an outing, as its row says it.
 *
 * @property name which one: «1», «2», or the last one's own length («0.52 km»).
 * @property pace its pace, «9:40 min/km»; null for a last split too short to have one.
 * @property detail what else is known of it, in a quiet line: its cadence, its climb.
 * @property share its bar's length against the fastest's: the faster, the longer.
 * @property fastest the one to mark: the fastest of the whole ones, when there are two to compare.
 * @property spoken the whole row as TalkBack says it.
 */
class SplitRow(
    val name: String,
    val pace: String?,
    val detail: String?,
    val share: Float,
    val fastest: Boolean,
    val spoken: String,
)

/**
 * An outing's kilometres one by one, as bars (PLANNING.md §11 Phase 16): each a row, its bar as
 * long as it was fast against the fastest, its pace at the bar's end, its cadence and its climb
 * under it. The fastest is in the goal's colour, the others in the accent. Rows, not columns:
 * a walk has as many as it has kilometres, and at any text size a row still holds its numbers.
 * Each row is one node for a screen reader.
 */
@Composable
fun SplitList(splits: List<SplitRow>, fastestLabel: String, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val best = PassoTheme.colors.goal
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier.testTag(OutingChartTags.SPLITS), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        splits.forEach { split ->
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = split.spoken },
            ) {
                // The name and the pace part ways when the two do not fit one line (large text).
                FlowRow(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            split.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                        )
                        if (split.fastest) {
                            Text(
                                fastestLabel,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = best,
                            )
                        }
                    }
                    split.pace?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                    val radius = CornerRadius(size.height / 2)
                    drawRoundRect(track, Offset.Zero, size, radius)
                    val width = size.width * split.share.coerceIn(0f, 1f)
                    if (width > 0f) {
                        drawRoundRect(
                            if (split.fastest) best else accent,
                            Offset.Zero,
                            Size(width, size.height),
                            radius,
                        )
                    }
                }
                split.detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * How an outing climbed, as a shape (PLANNING.md §11 Phase 16): the altitude of its route over
 * the distance walked, an area under a line, with its lowest and highest at the left and how
 * far it went at the right. An estimate, as the sentence over it says; one image to TalkBack,
 * said by [description].
 *
 * @param profile each known altitude, in metres, against the metres walked to it.
 */
@Composable
fun ClimbProfile(
    profile: List<Pair<Double, Double>>,
    lowest: String,
    highest: String,
    length: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    if (profile.size < 2) return
    val line = PassoTheme.colors.goal
    val rule = MaterialTheme.colorScheme.outlineVariant
    val labels = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag(OutingChartTags.PROFILE).clearAndSetSemantics { contentDescription = description },
    ) {
        Text(highest, style = labels, color = ink)
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val low = profile.minOf { it.second }
            val high = profile.maxOf { it.second }
            // A walk on the flat is a line along the floor of the box, not noise stretched to its height.
            val span = maxOf(high - low, MIN_SPAN_METERS)
            val far = profile.last().first.coerceAtLeast(1.0)
            fun x(meters: Double) = (meters / far * size.width).toFloat()
            fun y(altitude: Double) = (size.height - (altitude - low) / span * size.height * FILL).toFloat()
            val top = Path().apply {
                profile.forEachIndexed { index, (meters, altitude) ->
                    if (index == 0) moveTo(x(meters), y(altitude)) else lineTo(x(meters), y(altitude))
                }
            }
            val area = Path().apply {
                addPath(top)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(area, line.copy(alpha = AREA_ALPHA))
            drawPath(top, line, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawLine(rule, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text(lowest, style = labels, color = ink)
            Text(length, style = labels, color = ink)
        }
    }
}

/** Under this many metres between its lowest and highest, the profile is drawn as flat as it was. */
private const val MIN_SPAN_METERS = 30.0

/** The highest point stands a little under the box's top. */
private const val FILL = 0.9
private const val AREA_ALPHA = 0.22f

/** Hooks for the UI tests. */
object OutingChartTags {
    const val SPLITS = "outing_splits"
    const val PROFILE = "outing_profile"
}
