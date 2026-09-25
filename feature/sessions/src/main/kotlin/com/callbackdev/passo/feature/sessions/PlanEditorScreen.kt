package com.callbackdev.passo.feature.sessions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupHeader
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.SwitchRow
import com.callbackdev.passo.core.designsystem.components.ValueStepper
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.intensityDetail
import com.callbackdev.passo.core.designsystem.format.intensityLabel
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionAmount
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.SessionAmount
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.sessions.typicalCadence
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.tracking.SessionSpeech
import com.callbackdev.passo.core.tracking.VoiceAvailability

/** The outing editor for plan [planId] (null: a new one), with its draft in [viewModel]. */
@Composable
fun PlanEditorRoute(planId: Long?, onDone: () -> Unit, viewModel: PlanEditorViewModel = hiltViewModel()) {
    LaunchedEffect(planId) { viewModel.open(planId) }
    DisposableEffect(Unit) { onDispose { viewModel.close() } }
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val current = state?.takeIf { it.isNew == (planId == null) && (planId == null || it.original.id == planId) }
    PlanEditorScreen(
        state = current,
        onBack = onDone,
        actions = PlanEditorActions(
            rename = viewModel::rename,
            goalKind = viewModel::goalKind,
            goalValue = viewModel::goalValue,
            nudge = viewModel::nudge,
            intensity = viewModel::intensity,
            milestone = viewModel::milestone,
            vibrate = viewModel::vibrate,
            tryVibration = viewModel::tryVibration,
            voice = viewModel::voice,
            tryVoice = viewModel::tryVoice,
            openVoiceSettings = { runCatching { context.startActivity(SessionSpeech.settingsIntent()) } },
            save = { viewModel.save(onDone) },
            delete = { viewModel.delete(onDone) },
        ),
    )
}

/** What the editor can ask for. */
class PlanEditorActions(
    val rename: (String) -> Unit = {},
    val goalKind: (SessionGoalKind) -> Unit = {},
    val goalValue: (Double) -> Unit = {},
    val nudge: (Boolean) -> Unit = {},
    val intensity: (SessionIntensity) -> Unit = {},
    val milestone: (SessionMilestone, Boolean) -> Unit = { _, _ -> },
    val vibrate: (Boolean) -> Unit = {},
    val tryVibration: (SessionMilestone) -> Unit = {},
    val voice: (SessionVoice) -> Unit = {},
    val tryVoice: () -> Unit = {},
    val openVoiceSettings: () -> Unit = {},
    val save: () -> Unit = {},
    val delete: () -> Unit = {},
)

/**
 * One outing, made or changed (PLANNING.md §11 Phase 10), in the order it is thought of: its
 * name, its goal (one quantity, picked, never typed), its pace, what it comes to with the
 * reader's own step, then its signals, which can be felt here before they are chosen. Nothing is
 * written until Save; leaving with changes asks first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(
    state: PlanEditorState?,
    onBack: () -> Unit,
    actions: PlanEditorActions,
    modifier: Modifier = Modifier,
) {
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val leave = { if (state?.changed == true) confirmDiscard = true else onBack() }
    BackHandler(onBack = leave)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state?.isNew != false) R.string.editor_title_new else R.string.editor_title_edit,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.sessions_back))
                    }
                },
                actions = {
                    if (state != null && !state.isNew) {
                        IconButton(onClick = { confirmDelete = true }, modifier = Modifier.testTag(EditorTags.DELETE)) {
                            Icon(PassoIcons.Trash, contentDescription = stringResource(R.string.editor_delete))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state != null) {
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Button(
                        onClick = actions.save,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = ScreenMargin, vertical = 12.dp)
                            .testTag(EditorTags.SAVE),
                    ) {
                        Text(stringResource(R.string.editor_save))
                    }
                }
            }
        },
    ) { padding ->
        if (state != null) EditorList(state, actions, Modifier.fillMaxSize().padding(padding))
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.editor_discard_title)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text(stringResource(R.string.editor_discard)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.editor_discard_keep)) }
            },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.editor_delete_title)) },
            text = { Text(stringResource(R.string.editor_delete_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    actions.delete()
                }, modifier = Modifier.testTag(EditorTags.CONFIRM_DELETE)) {
                    Text(stringResource(R.string.editor_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.editor_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditorList(state: PlanEditorState, actions: PlanEditorActions, modifier: Modifier) {
    val res = LocalResources.current
    val format = rememberMeasureFormatter(state.units)
    val plan = state.draft
    LazyColumn(modifier = modifier.testTag(EditorTags.LIST), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "name") {
            OutlinedTextField(
                value = plan.name.orEmpty(),
                onValueChange = actions.rename,
                label = { Text(stringResource(R.string.editor_name)) },
                placeholder = { Text(res.sessionName(plan.copy(name = null))) },
                supportingText = { Text(stringResource(R.string.editor_name_note)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth().padding(
                    horizontal = ScreenMargin,
                    vertical = 8.dp,
                ).testTag(EditorTags.NAME),
            )
        }

        item(key = "goal-header") { GroupHeader(stringResource(R.string.editor_goal)) }
        item(key = "goal-kind") {
            val kinds = listOf(
                SessionGoalKind.TIME to R.string.editor_goal_time,
                SessionGoalKind.STEPS to R.string.editor_goal_steps,
                SessionGoalKind.DISTANCE to R.string.editor_goal_distance,
                SessionGoalKind.REST_OF_DAY to R.string.editor_goal_day,
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(EditorTags.GOAL_KIND),
            ) {
                kinds.forEachIndexed { index, (kind, label) ->
                    SegmentedButton(
                        selected = plan.goalKind == kind,
                        onClick = { actions.goalKind(kind) },
                        shape = SegmentedButtonDefaults.itemShape(index, kinds.size),
                        label = { Text(stringResource(label), maxLines = 1) },
                    )
                }
            }
        }
        item(key = "goal-value") { GoalValue(state, format, actions) }

        item(key = "pace-header") { GroupHeader(stringResource(R.string.editor_pace)) }
        item(key = "pace") {
            SettingsGroup(modifier = Modifier.selectableGroup()) {
                SessionIntensity.entries.forEach { intensity ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = plan.intensity == intensity,
                                onClick = { actions.intensity(intensity) },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = ScreenMargin, vertical = 12.dp)
                            .testTag("${EditorTags.PACE}-${intensity.name}"),
                    ) {
                        RadioButton(selected = plan.intensity == intensity, onClick = null)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(res.intensityLabel(intensity), style = MaterialTheme.typography.bodyLarge)
                            Text(
                                res.intensityDetail(intensity),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item(key = "estimate-header") { GroupHeader(stringResource(R.string.editor_estimate_title)) }
        item(key = "estimate") { Estimate(state, format) }

        item(key = "signals-header") { GroupHeader(stringResource(R.string.editor_signals)) }
        item(key = "signals") {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = ScreenMargin),
            ) {
                Text(
                    stringResource(R.string.editor_signals_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SessionMilestone.CHOOSABLE.forEach { milestone ->
                        val on = milestone in plan.milestones
                        FilterChip(
                            selected = on,
                            onClick = { actions.milestone(milestone, !on) },
                            label = { Text(format.percent(milestone.percent / 100.0)) },
                            leadingIcon = if (on) {
                                { Icon(PassoIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                            modifier = Modifier.testTag("${EditorTags.MILESTONE}-${milestone.percent}"),
                        )
                    }
                    // The goal is always told: said, not offered as a choice (a disabled chip
                    // would read as a signal this outing cannot have).
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.height(48.dp).padding(horizontal = 8.dp),
                    ) {
                        Icon(
                            PassoIcons.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.editor_signal_goal),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        if (state.canVibrate) {
            item(key = "vibrate") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                    SettingsGroup {
                        SwitchRow(
                            label = stringResource(R.string.editor_vibrate),
                            note = stringResource(R.string.editor_vibrate_note),
                            checked = plan.vibrate,
                            onChange = actions.vibrate,
                            icon = PassoIcons.Vibrate,
                            modifier = Modifier.testTag(EditorTags.VIBRATE),
                        )
                    }
                    if (plan.vibrate) TryVibrations(format, actions.tryVibration)
                }
            }
        }
        item(key = "voice") { VoiceChoice(state, actions) }
    }
}

/** The goal's value, picked on its steps; for the rest of the day, what it means now. */
@Composable
private fun GoalValue(state: PlanEditorState, format: MeasureFormatter, actions: PlanEditorActions) {
    val res = LocalResources.current
    val plan = state.draft
    Column(modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp)) {
        if (plan.goalKind == SessionGoalKind.REST_OF_DAY) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = GroupShape,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.editor_day_note), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (state.restOfDaySteps >=
                            com.callbackdev.passo.core.domain.sessions.SessionConstants.MIN_REST_OF_DAY_STEPS
                        ) {
                            stringResource(
                                R.string.editor_day_now,
                                res.sessionAmount(
                                    SessionAmount(SessionGoalKind.STEPS, state.restOfDaySteps.toDouble()),
                                    format,
                                ),
                            )
                        } else {
                            stringResource(R.string.editor_day_met)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }
        val range = SessionPlans.range(plan.goalKind)
        val amount = res.sessionAmount(SessionAmount(plan.goalKind, plan.goalValue.toDouble()), format)
        ValueStepper(
            value = plan.goalValue.toFloat(),
            range = range.first.toFloat()..range.last.toFloat(),
            text = amount,
            onValueChange = { actions.goalValue(it.toDouble()) },
            onDecrease = { actions.nudge(false) },
            onIncrease = { actions.nudge(true) },
            decreaseLabel = stringResource(R.string.editor_less),
            increaseLabel = stringResource(R.string.editor_more),
            caption = stringResource(
                when (plan.goalKind) {
                    SessionGoalKind.TIME -> R.string.editor_time_note
                    SessionGoalKind.DISTANCE -> R.string.editor_distance_note
                    else -> R.string.editor_steps_note
                },
            ),
            modifier = Modifier.testTag(EditorTags.VALUE),
        )
    }
}

/** «About 2,000 steps and 1.40 km, with your step of 73 cm», in the quantities the goal is not in. */
@Composable
private fun Estimate(state: PlanEditorState, format: MeasureFormatter) {
    val res = LocalResources.current
    val plan = state.draft
    val estimate = SessionPlans.estimate(plan, state.lengths, state.restOfDaySteps)
    val steps = res.sessionAmount(SessionAmount(SessionGoalKind.STEPS, estimate.steps.toDouble()), format)
    val distance = res.format(format.distance(estimate.distanceMeters))
    val minutes = res.format(format.minutes(estimate.minutes))
    val (first, second) = when (plan.goalKind) {
        SessionGoalKind.TIME -> steps to distance
        SessionGoalKind.DISTANCE -> steps to minutes
        SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> distance to minutes
    }
    val running =
        plan.intensity.typicalCadence >= com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
    val step = res.format(format.stepLength(state.lengths.forCadence(plan.intensity.typicalCadence)))
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(EditorTags.ESTIMATE),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(
                    if (running) R.string.editor_estimate_run else R.string.editor_estimate,
                    first,
                    second,
                    step,
                ),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.editor_estimate_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Whether the outing also speaks, and where: never, through headphones, or out loud when the
 * phone is not silenced. What the phone can do about it is said under it: the voice to hear
 * first, or the one to install.
 */
@Composable
private fun VoiceChoice(state: PlanEditorState, actions: PlanEditorActions) {
    val voice = state.draft.voice
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
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
                        Text(stringResource(R.string.editor_voice), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = stringResource(
                                when (voice) {
                                    SessionVoice.OFF -> R.string.editor_voice_off_note
                                    SessionVoice.HEADPHONES -> R.string.editor_voice_headphones_note
                                    SessionVoice.ALWAYS -> R.string.editor_voice_always_note
                                },
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val choices = listOf(
                    SessionVoice.OFF to R.string.editor_voice_off,
                    SessionVoice.HEADPHONES to R.string.editor_voice_headphones,
                    SessionVoice.ALWAYS to R.string.editor_voice_always,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testTag(EditorTags.VOICE)) {
                    choices.forEachIndexed { index, (choice, label) ->
                        SegmentedButton(
                            selected = voice == choice,
                            onClick = { actions.voice(choice) },
                            shape = SegmentedButtonDefaults.itemShape(index, choices.size),
                            // No check mark: three words must fit a narrow phone, and the fill
                            // already says which is chosen (and so does the semantics).
                            icon = {},
                            label = { Text(stringResource(label), maxLines = 1) },
                            modifier = Modifier.testTag("${EditorTags.VOICE}-${choice.name}"),
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
                when (state.voiceAvailability) {
                    VoiceAvailability.READY, VoiceAvailability.UNKNOWN -> {
                        Text(
                            stringResource(R.string.editor_voice_offline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AssistChip(
                            onClick = actions.tryVoice,
                            enabled = state.voiceAvailability == VoiceAvailability.READY,
                            label = { Text(stringResource(R.string.editor_voice_try)) },
                            leadingIcon = {
                                Icon(
                                    PassoIcons.Voice,
                                    contentDescription = null,
                                    modifier = Modifier.size(AssistChipDefaults.IconSize),
                                )
                            },
                            modifier = Modifier.testTag(EditorTags.TRY_VOICE),
                        )
                    }

                    VoiceAvailability.NO_OFFLINE_VOICE -> {
                        Text(
                            stringResource(R.string.editor_voice_missing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = actions.openVoiceSettings) {
                            Text(stringResource(R.string.editor_voice_install))
                        }
                    }

                    VoiceAvailability.NO_ENGINE -> Text(
                        stringResource(R.string.editor_voice_no_engine),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Each signal, felt before it is chosen: the patterns are learned here, not on the road. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TryVibrations(format: MeasureFormatter, onTry: (SessionMilestone) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            stringResource(R.string.editor_try),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SessionMilestone.entries.forEach { milestone ->
                AssistChip(
                    onClick = { onTry(milestone) },
                    label = {
                        Text(
                            if (milestone == SessionMilestone.GOAL) {
                                stringResource(R.string.editor_try_goal)
                            } else {
                                format.percent(milestone.percent / 100.0)
                            },
                        )
                    },
                    leadingIcon = {
                        Icon(
                            PassoIcons.Vibrate,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                    modifier = Modifier.testTag("${EditorTags.TRY}-${milestone.percent}"),
                )
            }
        }
    }
}

/** Hooks for the UI tests. */
object EditorTags {
    const val LIST = "editor_list"
    const val NAME = "editor_name"
    const val GOAL_KIND = "editor_goal_kind"
    const val VALUE = "editor_value"
    const val PACE = "editor_pace"
    const val ESTIMATE = "editor_estimate"
    const val MILESTONE = "editor_milestone"
    const val VIBRATE = "editor_vibrate"
    const val TRY = "editor_try"
    const val VOICE = "editor_voice"
    const val TRY_VOICE = "editor_try_voice"
    const val SAVE = "editor_save"
    const val DELETE = "editor_delete"
    const val CONFIRM_DELETE = "editor_confirm_delete"
}
