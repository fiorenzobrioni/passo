package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.domain.walks.WalkType

/**
 * The walks of a day, one row each (VISION.md, automatic walk detection): what it was and when,
 * how long it lasted, then its steps, distance, cadence and calories, the estimates as the rest
 * of the app computes them. Each row is one node for a screen reader. Rows only: the caller puts
 * them on their ground.
 */
@Composable
fun WalkList(walks: List<Walk>, format: MeasureFormatter, modifier: Modifier = Modifier) {
    Column(modifier) {
        walks.forEachIndexed { index, walk ->
            if (index > 0) GroupDivider()
            WalkRow(walk, format)
        }
    }
}

@Composable
private fun WalkRow(walk: Walk, format: MeasureFormatter) {
    val kind = walkTypeLabel(walk.type)
    val from = clockTime(walk.startMinute)
    val to = clockTime(walk.endMinute)
    val length = duration(walk.minutes)
    val steps = pluralStringResource(R.plurals.walk_steps, walk.steps, format.steps(walk.steps))
    val distance = format.distance(walk.distanceMeters).text()
    val cadence = format.cadence(walk.averageCadence).text()
    val energy = format.energy(walk.activeKcal).text()
    val spoken = stringResource(R.string.walk_spoken, kind, from, to, length, steps, distance, cadence, energy)
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (walk.type == WalkType.WALK) PassoIcons.Walk else PassoIcons.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.walk_time_range, from, to),
                    style = MaterialTheme.typography.titleSmall.copy(fontFeatureSettings = "tnum"),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = length,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = kind,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.tertiary,
            )
            Text(
                text = listOf(steps, distance, cadence, energy).joinToString("  ·  "),
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun walkTypeLabel(type: WalkType): String = stringResource(
    when (type) {
        WalkType.WALK -> R.string.walk_type_walk
        WalkType.RUN -> R.string.walk_type_run
        WalkType.MIXED -> R.string.walk_type_mixed
    },
)

/** A length of time: «35 minutes», «1 h 20 min», «2 h». */
@Composable
fun duration(minutes: Int): String = when {
    minutes < 60 -> pluralStringResource(R.plurals.duration_minutes, minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}
