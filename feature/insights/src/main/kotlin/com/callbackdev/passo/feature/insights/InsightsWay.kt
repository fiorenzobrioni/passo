package com.callbackdev.passo.feature.insights

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.PlaceText
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.WayMapView
import com.callbackdev.passo.core.designsystem.format.unorphaned
import com.callbackdev.passo.core.designsystem.format.wayHeadline
import com.callbackdev.passo.core.designsystem.format.wayNextStage
import com.callbackdev.passo.core.designsystem.format.wayProgressText
import com.callbackdev.passo.core.designsystem.format.wayToEnd
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.ways.wayNameRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.ways.WayProgress

/**
 * The way under way (PLANNING.md §11 Phase 11): its small map, where the reader stands and the
 * stage ahead, then the share walked as a bar the card's width, with how far the reader has come
 * and how far the end is at its two ends, as an outing's card sets them. The whole card opens
 * the Ways, and says so with the chevron every other door on the page has.
 */
@Composable
internal fun WayCard(progress: WayProgress, format: MeasureFormatter, onOpen: () -> Unit) {
    val ahead = wayNextStage(progress, format)
    // Once the end is the stage ahead, the line above already says how far it is.
    val toEnd = wayToEnd(progress, format).takeIf { progress.nextStage != progress.way.stops.last() }
    SettingsGroup(modifier = Modifier.testTag(InsightsTags.WAY)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen, role = Role.Button)
                .padding(start = ScreenMargin, end = ScreenMargin, top = 14.dp, bottom = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                WayMapView(
                    way = progress.way,
                    walkedMeters = progress.walkedMeters,
                    reached = progress.reached.size,
                    contentDescription = "",
                    detailed = false,
                    ratio = 1f,
                    modifier = Modifier.size(72.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(wayNameRes(progress.way.id)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    PlaceText(wayHeadline(progress), style = MaterialTheme.typography.titleMedium)
                    ahead?.let {
                        PlaceText(
                            it.unorphaned(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Icon(
                    PassoIcons.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { progress.fraction.toFloat() },
                    color = PassoTheme.colors.goal,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        wayProgressText(progress, format),
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    toEnd?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

/** With no way under way: the door to the Ways, after what the steps add up to. */
@Composable
internal fun WaysDoor(onOpen: () -> Unit) {
    SettingsGroup(modifier = Modifier.padding(top = 16.dp).testTag(InsightsTags.WAYS_DOOR)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen, role = Role.Button)
                .padding(horizontal = ScreenMargin, vertical = 14.dp),
        ) {
            // The row icons' quiet ink: the accent is the titles'.
            Icon(PassoIcons.Way, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.insights_ways_door_title).unorphaned(),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.insights_ways_door_body).unorphaned(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
