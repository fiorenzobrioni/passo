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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.sessionGoalDescription
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.sessionOutcome
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.Outing
import com.callbackdev.passo.core.domain.today.MINUTES_PER_DAY
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.domain.walks.WalkType
import kotlin.math.roundToInt

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

/**
 * A day's walks and outings in one list, by start (PLANNING.md §11 Phase 10): an outing stands in
 * for the walk found in its minutes, with its goal and what came of it.
 */
@Composable
fun OutingList(outings: List<Outing>, format: MeasureFormatter, modifier: Modifier = Modifier) {
    Column(modifier) {
        outings.forEachIndexed { index, outing ->
            if (index > 0) GroupDivider()
            when (outing) {
                is Outing.Detected -> WalkRow(outing.walk, format)
                is Outing.Planned -> SessionRow(outing, format)
            }
        }
    }
}

@Composable
private fun SessionRow(outing: Outing.Planned, format: MeasureFormatter) {
    val session = outing.session
    val res = LocalResources.current
    val name = res.sessionName(session)
    val from = clockTime(outing.startMinute)
    val to = clockTime(outing.endMinute.coerceAtMost(MINUTES_PER_DAY - 1))
    val length = duration(outing.endMinute - outing.startMinute)
    val outcome = res.sessionOutcome(session, format)
    val goal = res.sessionGoalDescription(session, format)
    val steps = pluralStringResource(R.plurals.walk_steps, session.totals.steps, format.steps(session.totals.steps))
    val distance = format.distance(session.totals.distanceMeters).text()
    val movingMinutes = session.totals.movingMillis / MILLIS_PER_MINUTE.toDouble()
    val cadence = if (movingMinutes >=
        1
    ) {
        format.cadence((session.totals.steps / movingMinutes).roundToInt()).text()
    } else {
        null
    }
    val energy = format.energy(session.totals.activeKcal).text()
    val figures = listOfNotNull(steps, distance, cadence, energy).joinToString("  ·  ")
    val spoken = stringResource(R.string.session_spoken, name, from, to, outcome, goal, figures)
    val reached = session.reached
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Surface(
            color = if (reached) PassoTheme.colors.goalContainer else MaterialTheme.colorScheme.primaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = sessionIcon(session.intensity),
                    contentDescription = null,
                    tint = if (reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.onPrimaryContainer,
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "·",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (reached) {
                    Icon(
                        PassoIcons.Check,
                        contentDescription = null,
                        tint = PassoTheme.colors.goal,
                        modifier = Modifier.size(14.dp),
                    )
                }
                Text(
                    text = outcome,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                    color = if (reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = goal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = figures,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

private const val MILLIS_PER_MINUTE = 60_000L

/** A length of time: «35 minutes», «1 h 20 min», «2 h». */
@Composable
fun duration(minutes: Int): String = when {
    minutes < 60 -> pluralStringResource(R.plurals.duration_minutes, minutes, minutes)
    minutes % 60 == 0 -> stringResource(R.string.duration_hours, minutes / 60)
    else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
}
