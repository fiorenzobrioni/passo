package com.callbackdev.passo.feature.settings.calibration

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.GroupHeader
import com.callbackdev.passo.core.designsystem.components.InfoRow
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.components.ValueStepper
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.tabular
import com.callbackdev.passo.core.domain.calibration.CalibratedStep
import com.callbackdev.passo.core.domain.calibration.CalibrationResult
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.settings.ProfileInputs
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.feature.settings.R
import java.util.Locale
import kotlin.math.abs

/** The calibration page for [step], as the navigation opens it. */
@Composable
fun CalibrationRoute(step: CalibratedStep, onDone: () -> Unit, viewModel: CalibrationViewModel = hiltViewModel()) {
    LaunchedEffect(step) { viewModel.open(step) }
    // The counter is read only while the page is on screen (PLANNING.md §9).
    LifecycleStartEffect(Unit) {
        viewModel.onVisible()
        onStopOrDispose { viewModel.onHidden() }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalibrationScreen(
        state = state,
        onBack = onDone,
        actions = CalibrationActions(
            setStep = viewModel::setStep,
            setDistance = viewModel::setDistance,
            start = viewModel::start,
            stop = viewModel::stop,
            again = viewModel::again,
            save = { viewModel.save(onDone) },
            permissionResult = viewModel::onPermissionResult,
        ),
    )
}

/** What the page can ask for. */
class CalibrationActions(
    val setStep: (CalibratedStep) -> Unit = {},
    val setDistance: (Double) -> Unit = {},
    val start: () -> Unit = {},
    val stop: () -> Unit = {},
    val again: () -> Unit = {},
    val save: () -> Unit = {},
    val permissionResult: () -> Unit = {},
)

/**
 * Measure your step (PLANNING.md §11 Phase 7), in the order it is done: what to measure and over
 * which distance, with how it goes; then the walk, the count moving while it is looked at; then
 * the length, what it changes, and Save. A result that cannot be a step is said with its reason
 * and the way to try again, never saved. The one action of each moment is the button at the
 * bottom, where the thumb is at the start line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    state: CalibrationUiState?,
    onBack: () -> Unit,
    actions: CalibrationActions,
    modifier: Modifier = Modifier,
) {
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    val unsaved = state != null &&
        (
            state.phase == CalibrationPhase.WALKING ||
                state.phase == CalibrationPhase.MEASURING ||
                state.result is CalibrationResult.Measured
            )
    val leave = { if (unsaved) confirmLeave = true else onBack() }
    BackHandler(onBack = leave)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.calibration_title)) },
                navigationIcon = {
                    IconButton(onClick = leave) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.settings_back))
                    }
                },
            )
        },
        bottomBar = { if (state != null) BottomAction(state, actions) },
    ) { padding ->
        if (state != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
                    .testTag(CalibrationTags.PAGE),
            ) {
                when (state.phase) {
                    CalibrationPhase.SETUP -> Setup(state, actions)
                    CalibrationPhase.WALKING, CalibrationPhase.MEASURING -> Walking(state, actions)
                    CalibrationPhase.RESULT -> Result(state)
                }
            }
        }
    }
    if (confirmLeave && state != null) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            title = { Text(stringResource(R.string.calibration_leave_title)) },
            text = {
                Text(
                    stringResource(
                        if (state.phase == CalibrationPhase.RESULT) {
                            R.string.calibration_leave_result
                        } else {
                            R.string.calibration_leave_walking
                        },
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onBack()
                }) { Text(stringResource(R.string.calibration_leave)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.calibration_stay)) }
            },
        )
    }
}

@Composable
private fun Setup(state: CalibrationUiState, actions: CalibrationActions) {
    val format = rememberMeasureFormatter(state.units)
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin + 4.dp, vertical = 8.dp),
    ) {
        Text(
            stringResource(R.string.calibration_intro),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            stringResource(R.string.calibration_intro_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (!state.permission) PermissionCard(actions.permissionResult)

    GroupHeader(stringResource(R.string.calibration_what))
    val steps = listOf(
        CalibratedStep.WALKING to R.string.calibration_walking,
        CalibratedStep.RUNNING to R.string.calibration_running,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin)) {
        steps.forEachIndexed { index, (step, label) ->
            SegmentedButton(
                selected = state.step == step,
                onClick = { actions.setStep(step) },
                shape = SegmentedButtonDefaults.itemShape(index, steps.size),
                label = { Text(stringResource(label), maxLines = 1) },
                modifier = Modifier.testTag("${CalibrationTags.STEP}-${step.name}"),
            )
        }
    }
    Text(
        text = stringResource(R.string.calibration_now) + " · " + currentStep(state.step, state.profile, format),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = ScreenMargin + 4.dp, end = ScreenMargin, top = 10.dp),
    )

    GroupHeader(stringResource(R.string.calibration_distance))
    val scale = ProfileInputs.calibrationDistance(format.units)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(16.dp)) {
            ValueStepper(
                value = state.distanceMeters.toFloat(),
                range = scale.range.start.toFloat()..scale.range.endInclusive.toFloat(),
                text = format.shortDistance(state.distanceMeters).text(),
                onValueChange = { actions.setDistance(scale.snap(it.toDouble())) },
                onDecrease = { actions.setDistance(scale.down(state.distanceMeters)) },
                onIncrease = { actions.setDistance(scale.up(state.distanceMeters)) },
                decreaseLabel = stringResource(R.string.settings_decrease),
                increaseLabel = stringResource(R.string.settings_increase),
                modifier = Modifier.testTag(CalibrationTags.DISTANCE),
            )
            Text(
                stringResource(
                    R.string.calibration_distance_hint,
                    format.shortDistance(TRACK_LAP_METERS).text(),
                    format.shortDistance(PITCH_LENGTH_METERS).text(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    GroupHeader(stringResource(R.string.calibration_how))
    SettingsGroup {
        HowStep(1, stringResource(R.string.calibration_how_start))
        GroupDivider()
        HowStep(
            2,
            stringResource(
                if (state.step ==
                    CalibratedStep.RUNNING
                ) {
                    R.string.calibration_how_run
                } else {
                    R.string.calibration_how_walk
                },
            ),
        )
        GroupDivider()
        HowStep(3, stringResource(R.string.calibration_how_stop))
    }
    if (state.permission && !state.counterReady) {
        Text(
            stringResource(R.string.calibration_waiting_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = ScreenMargin + 4.dp, end = ScreenMargin, top = 12.dp),
        )
    }
}

/** One step of how it goes: its number in a round mark, then what to do. */
@Composable
private fun HowStep(number: Int, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 14.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(28.dp).clearAndSetSemantics { },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), style = MaterialTheme.typography.labelLarge)
            }
        }
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/**
 * The walk under way: the steps since Start, large, and the time; they move while the page is
 * looked at, and catch up when it is looked at again.
 */
@Composable
private fun Walking(state: CalibrationUiState, actions: CalibrationActions) {
    val format = rememberMeasureFormatter(state.units)
    val distance = format.shortDistance(state.distanceMeters).text()
    val time = elapsed(state.elapsedMillis)
    val stepsLabel = pluralStringResource(R.plurals.calibration_steps, state.steps)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 24.dp),
    ) {
        Text(
            stringResource(
                if (state.step ==
                    CalibratedStep.RUNNING
                ) {
                    R.string.calibration_running_to
                } else {
                    R.string.calibration_walking_to
                },
                distance,
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 16.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription =
                        "${format.steps(state.steps)} $stepsLabel, $time"
                }
                .testTag(CalibrationTags.COUNT),
        ) {
            Text(
                format.steps(state.steps),
                style = PassoTheme.type.heroNumber.copy(fontSize = 88.sp, lineHeight = 96.sp),
                textAlign = TextAlign.Center,
            )
            Text(stepsLabel, style = MaterialTheme.typography.titleMedium)
            Text(
                time,
                style = MaterialTheme.typography.titleLarge.tabular(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
    StatusCard(
        icon = PassoIcons.Walk,
        title = stringResource(R.string.calibration_how_stop),
        body = stringResource(R.string.calibration_pocket),
        tone = StatusTone.NOTE,
        modifier = Modifier.padding(horizontal = ScreenMargin),
    )
    TextButton(
        onClick = actions.again,
        enabled = state.phase == CalibrationPhase.WALKING,
        modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp).testTag(CalibrationTags.RESTART),
    ) { Text(stringResource(R.string.calibration_restart)) }
}

/** The length, large, what it came from, and what it changes; or why there is none. */
@Composable
private fun Result(state: CalibrationUiState) {
    val format = rememberMeasureFormatter(state.units)
    when (val result = state.result) {
        is CalibrationResult.Measured -> Measured(state, result, format)

        is CalibrationResult.TooFewSteps -> Problem(
            title = stringResource(R.string.calibration_too_few_title),
            body = pluralStringResource(R.plurals.calibration_too_few_body, result.steps, format.steps(result.steps)),
        )

        is CalibrationResult.Implausible -> Problem(
            title = stringResource(R.string.calibration_implausible_title),
            body = pluralStringResource(
                R.plurals.calibration_implausible_body,
                result.steps,
                format.steps(result.steps),
                format.shortDistance(state.distanceMeters).text(),
                format.stepLength(result.stepLengthMeters).text(),
            ),
        )

        CalibrationResult.CounterReset -> Problem(
            title = stringResource(R.string.calibration_reset_title),
            body = stringResource(R.string.calibration_reset_body),
        )

        null -> Unit
    }
}

@Composable
private fun Measured(state: CalibrationUiState, result: CalibrationResult.Measured, format: MeasureFormatter) {
    val steps = format.steps(result.steps)
    val distance = format.shortDistance(result.distanceMeters).text()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin, vertical = 24.dp),
    ) {
        Text(
            stringResource(
                if (result.step == CalibratedStep.RUNNING) {
                    R.string.calibration_result_running
                } else {
                    R.string.calibration_result_walking
                },
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            format.stepLength(result.stepLengthMeters).text(),
            style = PassoTheme.type.heroNumber.copy(fontSize = 72.sp, lineHeight = 80.sp),
            modifier = Modifier.testTag(CalibrationTags.RESULT),
        )
        val cadence = result.cadence
        Text(
            text = if (cadence != null) {
                pluralStringResource(
                    R.plurals.calibration_result_detail_cadence,
                    result.steps,
                    steps,
                    distance,
                    format.cadence(cadence).text(),
                )
            } else {
                pluralStringResource(R.plurals.calibration_result_detail, result.steps, steps, distance)
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
    val before = when (result.step) {
        CalibratedStep.WALKING -> StepLengths.of(state.profile).walkingMeters
        CalibratedStep.RUNNING -> StepLengths.of(state.profile).runningMeters
    }
    SettingsGroup {
        InfoRow(stringResource(R.string.calibration_now), currentStep(result.step, state.profile, format))
        GroupDivider()
        InfoRow(stringResource(R.string.calibration_change), change(result.stepLengthMeters / before - 1, format))
    }
    val measuredCadence = result.cadence
    if (result.paceMismatch && measuredCadence != null) {
        val cadence = format.cadence(measuredCadence).text()
        StatusCard(
            icon = PassoIcons.Info,
            title = stringResource(
                if (result.step == CalibratedStep.WALKING) {
                    R.string.calibration_pace_run_title
                } else {
                    R.string.calibration_pace_walk_title
                },
            ),
            body = if (result.step == CalibratedStep.WALKING) {
                stringResource(R.string.calibration_pace_run_body, cadence)
            } else {
                stringResource(
                    R.string.calibration_pace_walk_body,
                    cadence,
                    format.cadence(MetricsConstants.RUNNING_CADENCE).text(),
                )
            },
            tone = StatusTone.NOTE,
            modifier = Modifier.padding(start = ScreenMargin, end = ScreenMargin, top = 12.dp),
        )
    }
    Text(
        stringResource(R.string.calibration_saved_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = ScreenMargin + 4.dp, end = ScreenMargin, top = 12.dp),
    )
}

@Composable
private fun Problem(title: String, body: String) {
    StatusCard(
        icon = PassoIcons.Warning,
        title = title,
        body = body,
        tone = StatusTone.PROBLEM,
        modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 16.dp).testTag(CalibrationTags.PROBLEM),
    )
}

/** Asked in context, where it is missing: the system's prompt, or its page once it stops asking. */
@Composable
private fun PermissionCard(onResult: () -> Unit) {
    val context = LocalContext.current
    var asked by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onResult() }
    StatusCard(
        icon = PassoIcons.Warning,
        title = stringResource(R.string.calibration_permission_title),
        body = stringResource(R.string.calibration_permission_body),
        tone = StatusTone.PROBLEM,
        action = stringResource(R.string.calibration_permission_action),
        onAction = {
            if (!asked) {
                asked = true
                launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            } else {
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }
        },
        modifier = Modifier.padding(start = ScreenMargin, end = ScreenMargin, top = 12.dp),
    )
}

/** The one action of the moment, at the bottom; on a result, trying again beside keeping it. */
@Composable
private fun BottomAction(state: CalibrationUiState, actions: CalibrationActions) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = ScreenMargin, vertical = 12.dp),
        ) {
            when (state.phase) {
                CalibrationPhase.SETUP -> Button(
                    onClick = actions.start,
                    enabled = state.permission && state.counterReady,
                    modifier = Modifier.weight(1f).testTag(CalibrationTags.START),
                ) {
                    Icon(PassoIcons.Play, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(
                            if (!state.permission || state.counterReady) {
                                R.string.calibration_start
                            } else {
                                R.string.calibration_waiting
                            },
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                CalibrationPhase.WALKING, CalibrationPhase.MEASURING -> Button(
                    onClick = actions.stop,
                    enabled = state.phase == CalibrationPhase.WALKING,
                    modifier = Modifier.weight(1f).testTag(CalibrationTags.STOP),
                ) {
                    Icon(PassoIcons.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(
                            if (state.phase == CalibrationPhase.MEASURING) {
                                R.string.calibration_measuring
                            } else {
                                R.string.calibration_stop
                            },
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                CalibrationPhase.RESULT -> if (state.result is CalibrationResult.Measured) {
                    OutlinedButton(
                        onClick = actions.again,
                        modifier = Modifier.weight(1f).testTag(CalibrationTags.AGAIN),
                    ) { Text(stringResource(R.string.calibration_again)) }
                    Button(onClick = actions.save, modifier = Modifier.weight(1f).testTag(CalibrationTags.SAVE)) {
                        Text(stringResource(R.string.calibration_save))
                    }
                } else {
                    Button(onClick = actions.again, modifier = Modifier.weight(1f).testTag(CalibrationTags.AGAIN)) {
                        Text(stringResource(R.string.calibration_again))
                    }
                }
            }
        }
    }
}

/** The step in use now, and where it comes from: what a measure would replace. */
@Composable
private fun currentStep(step: CalibratedStep, profile: Profile, format: MeasureFormatter): String {
    val lengths = StepLengths.of(profile)
    return when (step) {
        CalibratedStep.WALKING -> {
            val shown = format.stepLength(lengths.walkingMeters).text()
            when {
                profile.walkingStepLengthMeters != null && profile.stepLengthMode == StepLengthMode.CALIBRATED ->
                    stringResource(R.string.calibration_now_measured, shown)

                profile.walkingStepLengthMeters != null && profile.stepLengthMode == StepLengthMode.MANUAL ->
                    stringResource(R.string.calibration_now_manual, shown)

                profile.heightMeters != null -> stringResource(R.string.calibration_now_height, shown)

                else -> stringResource(R.string.calibration_now_default, shown)
            }
        }

        CalibratedStep.RUNNING -> {
            val shown = format.stepLength(lengths.runningMeters).text()
            if (profile.runningStepLengthMeters != null) {
                stringResource(R.string.calibration_now_manual, shown)
            } else {
                stringResource(R.string.calibration_now_running_auto, shown)
            }
        }
    }
}

/** What the new length does to every distance: longer or shorter by its share, or about the same. */
@Composable
private fun change(ratio: Double, format: MeasureFormatter): String = when {
    abs(ratio) < SAME_WITHIN -> stringResource(R.string.calibration_change_same)
    ratio > 0 -> stringResource(R.string.calibration_change_longer, format.percent(ratio))
    else -> stringResource(R.string.calibration_change_shorter, format.percent(-ratio))
}

/** Minutes and seconds, as a stopwatch reads: 1:32, 12:05. */
private fun elapsed(millis: Long): String {
    val seconds = millis / 1_000
    return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
}

/** Under 1% the new length changes no distance anyone would notice. */
private const val SAME_WITHIN = 0.01

/** The inside lane of a standard running track, and a football pitch's usual length. */
private const val TRACK_LAP_METERS = 400.0
private const val PITCH_LENGTH_METERS = 105.0

/** Hooks for the UI tests. */
object CalibrationTags {
    const val PAGE = "calibration_page"
    const val STEP = "calibration_step"
    const val DISTANCE = "calibration_distance"
    const val START = "calibration_start"
    const val COUNT = "calibration_count"
    const val STOP = "calibration_stop"
    const val RESTART = "calibration_restart"
    const val RESULT = "calibration_result"
    const val PROBLEM = "calibration_problem"
    const val AGAIN = "calibration_again"
    const val SAVE = "calibration_save"
}
