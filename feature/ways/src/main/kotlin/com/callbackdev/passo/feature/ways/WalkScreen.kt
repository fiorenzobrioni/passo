package com.callbackdev.passo.feature.ways

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.SegmentLabel
import com.callbackdev.passo.core.designsystem.components.SessionCard
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.WayMapView
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.wayDetail
import com.callbackdev.passo.core.designsystem.format.wayHeadline
import com.callbackdev.passo.core.designsystem.format.wayMapSpoken
import com.callbackdev.passo.core.designsystem.format.wayStages
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.designsystem.ways.wayRouteRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.WayJourneyState
import com.callbackdev.passo.core.tracking.VoiceAvailability
import kotlin.math.roundToInt

/** What a walk's page can ask for, as functions: the screen is a plain composable a test can draw. */
class WalkActions(
    val start: (again: Boolean) -> Unit = {},
    val voice: (SessionVoice) -> Unit = {},
    val card: SessionCardActions = SessionCardActions(),
    val delete: (Long) -> Unit = {},
    val prepareVoice: () -> Unit = {},
    val tryVoice: () -> Unit = {},
    val openVoiceSettings: () -> Unit = {},
)

/**
 * A city walk (PLANNING.md §11 Phase 11, second part). Its map first, as a way's; then where it
 * stands, in the order the reader lives it:
 *
 * - **Not begun**: its length, its places and its steps at the reader's step; Start.
 * - **Under way, no outing**: where the last outing left it, the place ahead; Continue from
 *   there, or Start again (which asks: the walk so far is put down).
 * - **An outing on it**: the outing's own card, without its map (the page has the walk's).
 * - **Walked to its end** (opened from Your ways, or the walk's last journey): when, and Walk
 *   it again.
 *
 * Then the voice the places are told in, how it works, the stamps, and the places in words.
 */
@Composable
internal fun WalkPage(
    state: WaysUiState,
    walk: WalkView,
    opened: JourneyView?,
    actions: WalkActions,
    modifier: Modifier = Modifier,
) {
    val format = rememberMeasureFormatter(state.units)
    val way = walk.way
    val live = walk.live?.takeIf { it.session.live }
    // A finished journey opened from Your ways shows that one; otherwise the walk as it stands.
    val view = opened?.takeIf { it.journey.state == WayJourneyState.FINISHED } ?: walk.current
    val progress = view?.progress
    var again by rememberSaveable { mutableStateOf(false) }
    // With the voice on, the engine is asked once whether it can speak: the page says so first.
    LaunchedEffect(state.walkVoice) { if (state.walkVoice != SessionVoice.OFF) actions.prepareVoice() }
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
            when {
                live != null -> SessionCard(
                    session = live.session,
                    cadence = live.cadence,
                    canKeepGoing = live.canKeepGoing,
                    format = format,
                    actions = actions.card,
                    showMap = false,
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                )

                view != null && view.journey.state == WayJourneyState.FINISHED ->
                    Walked(state, walk, view, format, onAgain = { actions.start(false) })

                view != null -> UnderWay(state, view, format, onContinue = { actions.start(false) }, onAgain = {
                    again = true
                })

                else -> NotBegun(state, walk, format, onStart = { actions.start(false) })
            }
        }
        if (live == null && view?.journey?.state != WayJourneyState.FINISHED) {
            item(key = "voice") { VoiceChoice(state.walkVoice, state.voiceAvailability, actions) }
            item(key = "how") {
                Text(
                    text = stringResource(R.string.walk_how),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        if (progress != null) {
            item(key = "credential-header") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Header(stringResource(R.string.way_group_credential))
                    Text(
                        text = stringResource(R.string.walk_credential_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }
            }
            item(key = "credential") { Credential(state, way, progress) }
        }
        item(key = "places-header") { Header(stringResource(R.string.walk_group_places)) }
        item(key = "places") { Stages(state, way, progress, format) }
        // Opened from Your ways, a walk walked to its end can go from there.
        if (opened != null && opened == view) {
            item(key = "delete") { DeleteJourney(walk = true, onDelete = { actions.delete(opened.journey.id) }) }
        }
        item(key = "footer") { Footer() }
    }
    if (again) {
        AlertDialog(
            onDismissRequest = { again = false },
            title = { Text(stringResource(R.string.walk_again_title)) },
            text = {
                Text(stringResource(R.string.walk_again_body, stringResource(placeNameRes(way.stops.first().key))))
            },
            confirmButton = {
                TextButton(onClick = {
                    again = false
                    actions.start(true)
                }) { Text(stringResource(R.string.walk_again)) }
            },
            dismissButton = { TextButton(onClick = { again = false }) { Text(stringResource(R.string.way_cancel)) } },
        )
    }
}

/** Not begun: its length and places, its steps at the reader's step, and Start (or why not now). */
@Composable
private fun NotBegun(state: WaysUiState, walk: WalkView, format: MeasureFormatter, onStart: () -> Unit) {
    val way = walk.way
    // Hundreds: a count of steps for a walk not yet walked is an estimate, and says so.
    val steps = ((way.lengthMeters / state.stepMeters) / STEPS_ROUNDING).roundToInt() * STEPS_ROUNDING
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
                text = stringResource(R.string.walk_steps, format.steps(steps)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val blocked = blockedNote(state)
        Button(
            onClick = onStart,
            enabled = !blocked,
            modifier = Modifier.fillMaxWidth().testTag(WaysTags.WALK_START),
        ) {
            Icon(PassoIcons.Outing, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.walk_start))
        }
    }
}

/** Begun, with no outing on it now: where it stands, the place ahead, Continue or Start again. */
@Composable
private fun UnderWay(
    state: WaysUiState,
    view: JourneyView,
    format: MeasureFormatter,
    onContinue: () -> Unit,
    onAgain: () -> Unit,
) {
    val progress = view.progress
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
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
        val blocked = blockedNote(state)
        Button(
            onClick = onContinue,
            enabled = !blocked,
            modifier = Modifier.fillMaxWidth().testTag(WaysTags.WALK_START),
        ) {
            Icon(PassoIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(8.dp))
            Text(stringResource(R.string.walk_continue, stringResource(placeNameRes(progress.last.stop.key))))
        }
        TextButton(
            onClick = onAgain,
            enabled = !blocked,
            modifier = Modifier.align(Alignment.End).testTag(WaysTags.WALK_AGAIN),
        ) { Text(stringResource(R.string.walk_again)) }
    }
}

/** Walked to its end: when, how far, and the way to walk it again. */
@Composable
private fun Walked(
    state: WaysUiState,
    walk: WalkView,
    view: JourneyView,
    format: MeasureFormatter,
    onAgain: () -> Unit,
) {
    val journey = view.journey
    val end = journey.endedEpochDay ?: state.today.toEpochDay()
    val days = (end - journey.startEpochDay + 1).toInt()
    val length = format.distance(walk.way.lengthMeters.toDouble()).text()
    val detail = if (days <= 1) {
        stringResource(R.string.walk_walked_on, dayText(end, state.today), length)
    } else {
        pluralStringResource(
            R.plurals.way_finished_detail,
            days,
            length,
            days,
            dayText(journey.startEpochDay, state.today),
            dayText(end, state.today),
        )
    }
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.semantics(mergeDescendants = true) { },
        ) {
            Text(wayHeadline(view.progress), style = MaterialTheme.typography.titleLarge)
            Text(
                detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // Walked again from here only when it is not already being walked again.
        if (walk.current == null) {
            Button(
                onClick = onAgain,
                enabled = !blockedNote(state),
                modifier = Modifier.fillMaxWidth().testTag(WaysTags.WALK_START),
            ) {
                Icon(PassoIcons.Outing, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.walk_walk_again))
            }
        }
    }
}

/**
 * Why a walk cannot start now, said above its button, which it disables: another outing under
 * way, counting paused, or no permission to count. Returns whether anything is in the way.
 */
@Composable
private fun blockedNote(state: WaysUiState): Boolean {
    val reason = when {
        state.live?.session?.live == true -> R.string.walk_busy
        state.canWalk == WalkReadiness.PAUSED -> R.string.walk_paused
        state.canWalk == WalkReadiness.PERMISSION_NEEDED -> R.string.walk_permission
        else -> null
    } ?: return false
    Text(
        text = stringResource(reason),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
    return true
}

/**
 * Whether the places are said aloud, and where: the walks' own choice, kept for the next one.
 * Under it, as in the outing editor, what the phone can do about it: the next place to hear
 * now, the voice to change, or the one to install.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceChoice(voice: SessionVoice, availability: VoiceAvailability, actions: WalkActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        SettingsGroup {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 14.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        PassoIcons.Voice,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.walk_voice), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(
                                when (voice) {
                                    SessionVoice.OFF -> R.string.walk_voice_off_note
                                    SessionVoice.HEADPHONES -> R.string.walk_voice_headphones_note
                                    SessionVoice.ALWAYS -> R.string.walk_voice_always_note
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val choices = listOf(
                    SessionVoice.OFF to R.string.walk_voice_off,
                    SessionVoice.HEADPHONES to R.string.walk_voice_headphones,
                    SessionVoice.ALWAYS to R.string.walk_voice_always,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testTag(WaysTags.WALK_VOICE)) {
                    choices.forEachIndexed { index, (choice, label) ->
                        SegmentedButton(
                            selected = voice == choice,
                            onClick = { actions.voice(choice) },
                            shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                            // No check mark, as in the outing editor: three words on a narrow phone.
                            icon = {},
                            label = { SegmentLabel(stringResource(label)) },
                            modifier = Modifier.testTag("${WaysTags.WALK_VOICE}-${choice.name}"),
                        )
                    }
                }
            }
        }
        if (voice != SessionVoice.OFF) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp),
            ) {
                when (availability) {
                    VoiceAvailability.READY, VoiceAvailability.UNKNOWN -> {
                        Text(
                            stringResource(R.string.walk_voice_offline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            AssistChip(
                                onClick = actions.tryVoice,
                                enabled = availability == VoiceAvailability.READY,
                                label = { Text(stringResource(R.string.walk_voice_try)) },
                                leadingIcon = {
                                    Icon(
                                        PassoIcons.Voice,
                                        contentDescription = null,
                                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                                    )
                                },
                                modifier = Modifier.testTag(WaysTags.WALK_TRY_VOICE),
                            )
                            // Which voice is the system's choice, made by ear with its samples.
                            TextButton(onClick = actions.openVoiceSettings) {
                                Text(stringResource(R.string.walk_voice_change))
                            }
                        }
                    }

                    VoiceAvailability.NO_OFFLINE_VOICE -> {
                        Text(
                            stringResource(R.string.walk_voice_missing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = actions.openVoiceSettings) {
                            Text(stringResource(R.string.walk_voice_install))
                        }
                    }

                    VoiceAvailability.NO_ENGINE -> Text(
                        stringResource(R.string.walk_voice_no_engine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private const val STEPS_ROUNDING = 100
