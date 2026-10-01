package com.callbackdev.passo.feature.ways

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.WayMapView
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.wayDetail
import com.callbackdev.passo.core.designsystem.format.wayDuration
import com.callbackdev.passo.core.designsystem.format.wayHeadline
import com.callbackdev.passo.core.designsystem.format.wayMapSpoken
import com.callbackdev.passo.core.designsystem.format.wayProgressText
import com.callbackdev.passo.core.designsystem.format.wayStages
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.ways.wayNameRes
import com.callbackdev.passo.core.designsystem.ways.wayRouteRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.ways.WalkPlaces
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.domain.ways.WayForecast
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourneyState

/** The Ways page, with its state from [WaysViewModel]. */
@Composable
fun WaysRoute(onBack: () -> Unit, onOpenWay: (WayId, Long?) -> Unit, viewModel: WaysViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WaysScreen(state, onBack, onOpenWay)
}

/**
 * The Ways page (PLANNING.md §11 Phase 11): what a way is, in one sentence; the way under way,
 * with its map; the four ways, each with what it would take at the reader's pace; the cities,
 * each walk with where it stands; and the ways finished or left, the cities walked. Each opens
 * its own page.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaysScreen(
    state: WaysUiState?,
    onBack: () -> Unit,
    onOpenWay: (WayId, Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ways_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.ways_back))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the days are read, a bare page: a list without them would say nothing is walked.
        if (state != null) WaysList(state, onOpenWay, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun WaysList(state: WaysUiState, onOpenWay: (WayId, Long?) -> Unit, modifier: Modifier) {
    val format = rememberMeasureFormatter(state.units)
    LazyColumn(
        modifier = modifier.testTag(WaysTags.LIST),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Text(
                text = stringResource(R.string.ways_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        state.active?.let { active ->
            item(key = "active") {
                ActiveCard(active, format) { onOpenWay(active.journey.way, active.journey.id) }
            }
        }
        item(key = "ways-header") { Header(stringResource(R.string.ways_group_ways)) }
        item(key = "ways") {
            SettingsGroup(modifier = Modifier.testTag(WaysTags.CATALOGUE)) {
                Ways.all.forEachIndexed { index, way ->
                    if (index > 0) GroupDivider()
                    WayRow(way, state.pace, format) { onOpenWay(way.id, null) }
                }
            }
        }
        item(key = "cities-header") {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Header(stringResource(R.string.ways_group_cities))
                Text(
                    text = stringResource(R.string.ways_cities_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        item(key = "cities") {
            SettingsGroup(modifier = Modifier.testTag(WaysTags.CITIES)) {
                state.walks.forEachIndexed { index, walk ->
                    if (index > 0) GroupDivider()
                    WalkRow(walk, state, format) { onOpenWay(walk.way.id, null) }
                }
            }
        }
        if (state.past.isNotEmpty()) {
            item(key = "yours-header") { Header(stringResource(R.string.ways_group_yours)) }
            item(key = "yours") {
                SettingsGroup(modifier = Modifier.testTag(WaysTags.YOURS)) {
                    state.past.forEachIndexed { index, view ->
                        if (index > 0) GroupDivider()
                        PastRow(view, state, format) { onOpenWay(view.journey.way, view.journey.id) }
                    }
                }
            }
        }
        item(key = "footer") { Footer() }
    }
}

/** The way under way: its map, where the reader stands, how far, and the way to its page. */
@Composable
private fun ActiveCard(view: JourneyView, format: MeasureFormatter, onOpen: () -> Unit) {
    val progress = view.progress
    val way = progress.way
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .clip(GroupShape)
            .clickable(onClick = onOpen, role = Role.Button)
            .testTag(WaysTags.ACTIVE),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            WayMapView(
                way = way,
                walkedMeters = progress.walkedMeters,
                reached = progress.reached.size,
                contentDescription = wayMapSpoken(way, progress, format),
                detailed = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.ways_under_way) + " · " + stringResource(wayNameRes(way.id)),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(wayHeadline(progress), style = MaterialTheme.typography.titleLarge)
                wayDetail(progress, format)?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            WayProgressBar(view, format)
        }
    }
}

/** The share walked, as a bar, and the same in words beside it. */
@Composable
internal fun WayProgressBar(view: JourneyView, format: MeasureFormatter, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LinearProgressIndicator(
            progress = { view.progress.fraction.toFloat() },
            color = PassoTheme.colors.goal,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = wayProgressText(view.progress, format),
            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** One of the four: its small map, its name and route, its length, and its time at the reader's pace. */
@Composable
private fun WayRow(way: Way, pace: Double?, format: MeasureFormatter, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = ScreenMargin, vertical = 14.dp)
            .testTag(WaysTags.way(way.id)),
    ) {
        WayMapView(
            way = way,
            walkedMeters = null,
            reached = 0,
            contentDescription = "",
            detailed = false,
            ratio = 1f,
            modifier = Modifier.size(THUMBNAIL),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(wayNameRes(way.id)), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(wayRouteRes(way.id)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.ways_row_facts,
                    format.distance(way.lengthMeters.toDouble()).text(),
                    wayStages(way),
                ),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (pace != null) {
                Text(
                    text = stringResource(
                        R.string.ways_row_pace,
                        wayDuration(WayForecast.daysFor(way.lengthMeters.toDouble(), pace)),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * A city walk: its small map (with the part walked, once begun), its city and route, its
 * length and places, and where it stands: under way, or when it was last walked to its end.
 */
@Composable
private fun WalkRow(walk: WalkView, state: WaysUiState, format: MeasureFormatter, onClick: () -> Unit) {
    val way = walk.way
    val current = walk.current?.progress
    val walked = walk.live?.session?.let { WalkPlaces.along(it) } ?: current?.walkedMeters
    val status = when {
        walked != null -> stringResource(
            R.string.ways_row_walk_under_way,
            wayProgressText(way, walked, format),
        )

        walk.lastFinished != null -> stringResource(
            R.string.ways_row_walk_walked,
            dayText(walk.lastFinished.journey.endedEpochDay ?: state.today.toEpochDay(), state.today),
        )

        else -> null
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = ScreenMargin, vertical = 14.dp)
            .testTag(WaysTags.way(way.id)),
    ) {
        WayMapView(
            way = way,
            walkedMeters = walked,
            reached = walked?.let { at -> way.stops.count { it.distanceMeters <= at } } ?: 0,
            contentDescription = "",
            detailed = false,
            ratio = 1f,
            modifier = Modifier.size(THUMBNAIL),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(wayNameRes(way.id)), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(wayRouteRes(way.id)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(
                    R.string.ways_row_facts,
                    format.distance(way.lengthMeters.toDouble()).text(),
                    wayStages(way),
                ),
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            status?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A way finished or left, or a city walked to its end: its name, and when, or where it was left. */
@Composable
private fun PastRow(view: JourneyView, state: WaysUiState, format: MeasureFormatter, onClick: () -> Unit) {
    val journey = view.journey
    val start = dayText(journey.startEpochDay, state.today)
    val end = dayText(journey.endedEpochDay ?: state.today.toEpochDay(), state.today)
    val line = if (journey.state == WayJourneyState.FINISHED) {
        // A city walked in a day is walked "on" it, not "from" it "to" it.
        if (start == end) {
            stringResource(R.string.ways_row_walk_walked, end)
        } else {
            stringResource(R.string.ways_row_walked, start, end)
        }
    } else {
        stringResource(R.string.ways_row_left, end, wayProgressText(view.progress, format))
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, role = Role.Button)
            .padding(horizontal = ScreenMargin, vertical = 14.dp),
    ) {
        Icon(
            if (journey.state == WayJourneyState.FINISHED) PassoIcons.Flag else PassoIcons.Way,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(wayNameRes(journey.way)), style = MaterialTheme.typography.bodyLarge)
            Text(line, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun Header(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 20.dp, end = ScreenMargin, top = 8.dp).semantics { heading() },
    )
}

/** What the numbers are, and where the lines come from (ODbL asks for the credit). */
@Composable
internal fun Footer() {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.ways_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.ways_credit),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(WaysTags.CREDIT),
        )
    }
}

private val THUMBNAIL = 64.dp

/** Hooks for the UI tests. */
object WaysTags {
    const val LIST = "ways_list"
    const val ACTIVE = "ways_active"
    const val CATALOGUE = "ways_catalogue"
    const val YOURS = "ways_yours"
    const val CITIES = "ways_cities"
    const val WALK_START = "walk_start"
    const val WALK_AGAIN = "walk_again"
    const val WALK_VOICE = "walk_voice"
    const val CREDIT = "ways_credit"
    const val PAGE = "way_page"
    const val START = "way_start"
    const val LEAVE = "way_leave"
    const val STAMPS = "way_stamps"
    const val STAGES = "way_stages"
    const val START_DIALOG = "way_start_dialog"

    fun way(id: WayId) = "ways_way_${id.name.lowercase()}"
}
