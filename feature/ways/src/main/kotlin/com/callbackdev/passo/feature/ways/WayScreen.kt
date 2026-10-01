package com.callbackdev.passo.feature.ways

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.WayMapView
import com.callbackdev.passo.core.designsystem.components.WayStamp
import com.callbackdev.passo.core.designsystem.components.stampSeed
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.shortDate
import com.callbackdev.passo.core.designsystem.format.shortDateWithYear
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.wayDetail
import com.callbackdev.passo.core.designsystem.format.wayDuration
import com.callbackdev.passo.core.designsystem.format.wayHeadline
import com.callbackdev.passo.core.designsystem.format.wayMapSpoken
import com.callbackdev.passo.core.designsystem.format.wayProgressText
import com.callbackdev.passo.core.designsystem.format.wayStages
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.designsystem.ways.placeNoteRes
import com.callbackdev.passo.core.designsystem.ways.wayNameRes
import com.callbackdev.passo.core.designsystem.ways.wayRouteRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.ways.StopReached
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.domain.ways.WayForecast
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.WayStartChoice
import com.callbackdev.passo.core.domain.ways.WayStop
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourneyState
import java.time.LocalDate

/** One way's page, or one city walk's, with its state from [WaysViewModel]. */
@Composable
fun WayRoute(way: WayId, journeyId: Long?, onBack: () -> Unit, viewModel: WaysViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(Unit) {
        viewModel.refreshReadiness()
        onPauseOrDispose {}
    }
    // Asked the first time a walk starts without it: its places are told as notifications'
    // vibrations. The walk starts either way.
    var pendingAgain by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingAgain?.let { viewModel.startWalk(way, it) }
        pendingAgain = null
    }
    WayScreen(
        state = state,
        way = way,
        journeyId = journeyId,
        onBack = onBack,
        actions = WayActions(
            start = { choice, chosen -> viewModel.start(way, choice, chosen) },
            leave = viewModel::leave,
            walk = WalkActions(
                start = { again ->
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.startWalk(way, again)
                    } else {
                        pendingAgain = again
                        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                voice = viewModel::setWalkVoice,
                card = SessionCardActions(
                    onPause = viewModel::pauseWalk,
                    onResume = viewModel::resumeWalk,
                    onStop = viewModel::stopWalk,
                    onKeepGoing = viewModel::keepGoing,
                ),
            ),
        ),
    )
}

/** What the page can ask for, as functions: the screen is a plain composable a test can draw. */
class WayActions(
    val start: (WayStartChoice, LocalDate?) -> Unit = { _, _ -> },
    val leave: (Long) -> Unit = {},
    val walk: WalkActions = WalkActions(),
)

/**
 * One way (PLANNING.md §11 Phase 11). Its map first, then one sentence of where the reader
 * stands (or, for a way not started, what it would take), the credential with a stamp for
 * each stage reached, and the stages in words: the map's equivalent for TalkBack, each place
 * said once it is near. A journey opened from "Your ways" shows that journey; the way opened
 * from the list shows the journey under way on it, or the way itself, ready to start.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WayScreen(
    state: WaysUiState?,
    way: WayId,
    journeyId: Long?,
    onBack: () -> Unit,
    actions: WayActions,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(wayNameRes(way))) },
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
        val walk = state?.walk(way)
        when {
            state == null -> Unit

            walk != null -> WalkPage(
                state = state,
                walk = walk,
                opened = journeyId?.let(state::journey),
                actions = actions.walk,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            else -> {
                val view = journeyId?.let(state::journey) ?: state.active?.takeIf { it.journey.way == way }
                WayPage(state, Ways.of(way), view, actions, Modifier.fillMaxSize().padding(padding))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WayPage(state: WaysUiState, way: Way, view: JourneyView?, actions: WayActions, modifier: Modifier) {
    val format = rememberMeasureFormatter(state.units)
    val progress = view?.progress
    var starting by rememberSaveable { mutableStateOf(false) }
    var leaving by rememberSaveable { mutableStateOf(false) }
    LazyColumn(
        modifier = modifier.testTag(WaysTags.PAGE),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "route") {
            Text(
                text = stringResource(wayRouteRes(way.id)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        item(key = "map") {
            WayMapView(
                way = way,
                walkedMeters = progress?.walkedMeters,
                reached = progress?.reached?.size ?: 0,
                contentDescription = wayMapSpoken(way, progress, format),
                modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
            )
        }
        item(key = "status") {
            if (view == null) {
                NotStarted(state, way, format, onStart = { starting = true })
            } else {
                Standing(state, view, format)
            }
        }
        if (progress != null) {
            item(key = "credential-header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Header(stringResource(R.string.way_group_credential))
                    Text(
                        text = stringResource(R.string.way_credential_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            item(key = "credential") {
                Credential(state, way, progress)
            }
        }
        item(key = "stages-header") { Header(stringResource(R.string.way_group_stages)) }
        item(key = "stages") { Stages(state, way, progress, format) }
        if (view != null && view.journey.state == WayJourneyState.ACTIVE && !view.progress.finished) {
            item(key = "leave") {
                OutlinedButton(
                    onClick = { leaving = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(WaysTags.LEAVE),
                ) { Text(stringResource(R.string.way_leave)) }
            }
        }
        item(key = "footer") { Footer() }
    }
    if (starting) {
        StartDialog(
            state = state,
            way = way,
            onStart = { choice, chosen ->
                starting = false
                actions.start(choice, chosen)
            },
            onDismiss = { starting = false },
        )
    }
    if (leaving && view != null) {
        AlertDialog(
            onDismissRequest = { leaving = false },
            title = { Text(stringResource(R.string.way_leave_title)) },
            text = { Text(stringResource(R.string.way_leave_body)) },
            confirmButton = {
                TextButton(onClick = {
                    leaving = false
                    actions.leave(view.journey.id)
                }) { Text(stringResource(R.string.way_leave_confirm)) }
            },
            dismissButton = { TextButton(onClick = { leaving = false }) { Text(stringResource(R.string.way_cancel)) } },
        )
    }
}

/** A way not started: its length, its time at the reader's pace, and Start (or why not now). */
@Composable
private fun NotStarted(state: WaysUiState, way: Way, format: MeasureFormatter, onStart: () -> Unit) {
    val busy = state.active
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 4.dp).semantics(mergeDescendants = true) { },
        ) {
            Text(
                text = stringResource(
                    R.string.ways_row_facts,
                    format.distance(way.lengthMeters.toDouble()).text(),
                    wayStages(way),
                ),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = state.pace?.let {
                    stringResource(R.string.way_pace, wayDuration(WayForecast.daysFor(way.lengthMeters.toDouble(), it)))
                } ?: stringResource(R.string.way_no_pace),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (busy != null) {
            Text(
                text = stringResource(R.string.way_busy, stringResource(wayNameRes(busy.journey.way))),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Button(
            onClick = onStart,
            enabled = busy == null,
            modifier = Modifier.fillMaxWidth().testTag(WaysTags.START),
        ) {
            Icon(PassoIcons.Way, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.way_start))
        }
    }
}

/** A journey: where the reader stands, how far, since when, and when they would arrive. */
@Composable
private fun Standing(state: WaysUiState, view: JourneyView, format: MeasureFormatter) {
    val progress = view.progress
    val journey = view.journey
    val start = dayText(journey.startEpochDay, state.today)
    val detail = when {
        progress.finished -> {
            val end = progress.finishedEpochDay ?: state.today.toEpochDay()
            val days = (end - journey.startEpochDay + 1).toInt()
            pluralStringResource(
                R.plurals.way_finished_detail,
                days,
                format.distance(progress.way.lengthMeters.toDouble()).text(),
                days,
                start,
                dayText(end, state.today),
            )
        }

        journey.state == WayJourneyState.LEFT -> stringResource(
            R.string.way_left_detail,
            wayProgressText(progress, format),
            start,
            dayText(journey.endedEpochDay ?: state.today.toEpochDay(), state.today),
        )

        else -> wayDetail(progress, format)
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Text(wayHeadline(progress), style = MaterialTheme.typography.titleLarge)
            detail?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (journey.state == WayJourneyState.ACTIVE && !progress.finished) {
            WayProgressBar(view, format)
            val days = (state.today.toEpochDay() - journey.startEpochDay + 1).toInt()
            Text(
                text =
                stringResource(R.string.way_since, start) + ", " +
                    pluralStringResource(R.plurals.way_day, days, days),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            view.forecast?.let { forecast ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) { },
                ) {
                    Text(
                        text = stringResource(R.string.way_forecast, dateText(forecast.arrival, state.today)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.way_forecast_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** The credential: a stamp for each stage reached, then the stages ahead as their places. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun Credential(state: WaysUiState, way: Way, progress: WayProgress) {
    val reached = progress.reached.associateBy { it.stop.key }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(WaysTags.STAMPS),
    ) {
        way.stages.forEach { stage ->
            val name = stringResource(placeNameRes(stage.key))
            val at = reached[stage.key]
            val date = at?.let { dayText(it.epochDay, state.today) }
            WayStamp(
                name = name,
                date = date,
                seed = stampSeed(stage.key),
                spoken = if (date != null) {
                    stringResource(R.string.way_stamp_reached, name, date)
                } else {
                    stringResource(R.string.way_stamp_ahead, name)
                },
            )
        }
    }
}

/**
 * The stops in words: each with where it falls, and when it was reached or how far it is. A
 * stop's sentence is said once it is reached, or when it is the next: the way is discovered
 * as it is walked.
 */
@Composable
internal fun Stages(state: WaysUiState, way: Way, progress: WayProgress?, format: MeasureFormatter) {
    val reached = progress?.reached.orEmpty().associateBy { it.stop.key }
    val next = progress?.next
    SettingsGroup(modifier = Modifier.testTag(WaysTags.STAGES)) {
        way.stops.forEachIndexed { index, stop ->
            if (index > 0) GroupDivider()
            StopRow(
                stop = stop,
                reached = reached[stop.key],
                isNext = stop == next,
                // Before a start every sentence is there to read: nothing is being discovered yet.
                showNote = progress == null || stop.key in reached || stop == next,
                progress = progress,
                today = state.today,
                format = format,
            )
        }
    }
}

@Composable
private fun StopRow(
    stop: WayStop,
    reached: StopReached?,
    isNext: Boolean,
    showNote: Boolean,
    progress: WayProgress?,
    today: LocalDate,
    format: MeasureFormatter,
) {
    val goal = PassoTheme.colors.goal
    val scheme = MaterialTheme.colorScheme
    val status = when {
        reached != null -> stringResource(R.string.way_stop_reached, dayText(reached.epochDay, today))

        isNext && progress != null -> stringResource(
            R.string.way_stop_ahead,
            format.distance(progress.metersTo(stop)).text(),
        )

        stop.distanceMeters == 0 -> stringResource(R.string.way_stop_start)

        else -> stringResource(R.string.way_stop_mark, format.distance(stop.distanceMeters.toDouble()).text())
    }
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .padding(horizontal = ScreenMargin, vertical = 12.dp),
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            val size = if (stop.stage) 12.dp else 8.dp
            val dot = Modifier.size(size).clip(CircleShape)
            Box(
                if (reached != null) {
                    dot.background(goal)
                } else {
                    dot.border(1.5.dp, if (isNext) goal else scheme.outline, CircleShape)
                },
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(placeNameRes(stop.key)),
                style = if (stop.stage) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (stop.stage) status else stringResource(R.string.way_stop_place) + " · " + status,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = scheme.onSurfaceVariant,
            )
            val note = placeNoteRes(stop.key)
            if (showNote && note != null) {
                Text(
                    text = stringResource(note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** A day of this year without its year, another year's with it. */
@Composable
internal fun dayText(epochDay: Long, today: LocalDate): String = dateText(LocalDate.ofEpochDay(epochDay), today)

@Composable
internal fun dateText(date: LocalDate, today: LocalDate): String =
    if (date.year == today.year) shortDate(date) else shortDateWithYear(date)
