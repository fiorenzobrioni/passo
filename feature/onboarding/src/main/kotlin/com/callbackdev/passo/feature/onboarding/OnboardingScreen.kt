package com.callbackdev.passo.feature.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.ValueStepper
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.padding
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.settings.InputScale
import com.callbackdev.passo.core.domain.settings.ProfileInputs
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.model.Sex

/** The first run, driven by [OnboardingViewModel]. */
@Composable
fun OnboardingRoute(viewModel: OnboardingViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(Unit) {
        viewModel.refreshPermission()
        onPauseOrDispose { }
    }
    OnboardingScreen(
        state = state,
        actions = OnboardingActions(
            next = viewModel::next,
            back = viewModel::back,
            skipProfile = viewModel::skipProfile,
            setHeight = viewModel::setHeight,
            setWeight = viewModel::setWeight,
            setSex = viewModel::setSex,
            setGoal = viewModel::setGoal,
            permissionAnswered = viewModel::permissionAnswered,
            finish = viewModel::finish,
        ),
    )
}

/** What the pages can ask for, as functions, so the screen is a plain composable a test can draw. */
class OnboardingActions(
    val next: () -> Unit = {},
    val back: () -> Unit = {},
    val skipProfile: () -> Unit = {},
    val setHeight: (Double) -> Unit = {},
    val setWeight: (Double) -> Unit = {},
    val setSex: (Sex?) -> Unit = {},
    val setGoal: (Int) -> Unit = {},
    val permissionAnswered: () -> Unit = {},
    val finish: () -> Unit = {},
)

/**
 * The first run, one page at a time (the family's frame, owner, 18 Oct 2026): the page's words
 * centered in the room above the buttons, and the buttons at the bottom, in the same place on every
 * page. No picture and no progress bar; a screen reader hears where the run stands on each page's
 * heading. Pages slide the way the app's pages do, a fade under reduced motion; the way back is
 * the system's.
 */
@Composable
fun OnboardingScreen(state: OnboardingState, actions: OnboardingActions, modifier: Modifier = Modifier) {
    val order = state.steps
    val index = order.indexOf(state.step)
    val last = index == order.lastIndex
    val reduced = reducedMotion()
    BackHandler(enabled = index > 0, onBack = actions.back)
    val gutter = pageGutter()

    Surface(modifier = modifier.fillMaxSize().testTag(OnboardingTags.ROOT)) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            AnimatedContent(
                targetState = state.step,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    if (reduced) {
                        fadeIn(tween(100)) togetherWith fadeOut(tween(100))
                    } else {
                        val spec = tween<androidx.compose.ui.unit.IntOffset>(300, easing = FastOutSlowInEasing)
                        (
                            fadeIn(tween(300)) + slideInHorizontally(spec) {
                                if (forward) it / 6 else -it / 6
                            }
                            ) togetherWith
                            (fadeOut(tween(300)) + slideOutHorizontally(spec) { if (forward) -it / 6 else it / 6 })
                    }
                },
                label = "onboarding",
                modifier = Modifier.weight(1f),
            ) { step ->
                // Centered in the room above the buttons; a page taller than the room scrolls from its top.
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val room = maxHeight
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .heightIn(min = room)
                            .padding(gutter)
                            .padding(horizontal = 24.dp, vertical = 24.dp)
                            .testTag(OnboardingTags.PAGE),
                    ) {
                        val where = stringResource(R.string.onboarding_progress, order.indexOf(step) + 1, order.size)
                        when (step) {
                            OnboardingStep.WELCOME -> WelcomePage(where)
                            OnboardingStep.PROFILE -> ProfilePage(state, actions, where)
                            OnboardingStep.GOAL -> GoalPage(state, actions, where)
                            OnboardingStep.PERMISSIONS -> PermissionsPage(state, where)
                            OnboardingStep.BATTERY -> BatteryPage(state, where)
                        }
                    }
                }
            }
            Box(Modifier.padding(gutter)) { BottomBar(state, last, actions) }
        }
    }
}

@Composable
private fun BottomBar(state: OnboardingState, last: Boolean, actions: OnboardingActions) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val primaryLabel = when {
            state.step == OnboardingStep.WELCOME -> stringResource(R.string.onboarding_welcome_start)
            last -> stringResource(R.string.onboarding_done)
            else -> stringResource(R.string.onboarding_continue)
        }
        // On the permissions page the button asks; the page moves on once it is granted.
        if (state.step != OnboardingStep.PERMISSIONS || state.activityGranted) {
            Button(
                onClick = if (last) actions.finish else actions.next,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(OnboardingTags.PRIMARY),
            ) {
                Text(primaryLabel)
            }
        } else {
            PermissionButton(actions)
        }
        when {
            state.step == OnboardingStep.PROFILE -> QuietButton(
                text = stringResource(R.string.onboarding_skip),
                onClick = actions.skipProfile,
                modifier = Modifier.testTag(OnboardingTags.SKIP),
            )

            state.step == OnboardingStep.PERMISSIONS && !state.activityGranted -> QuietButton(
                text = stringResource(R.string.onboarding_permissions_later),
                onClick = if (last) actions.finish else actions.next,
            )

            // The tip's own road, under the button that ends the run: an alternative, not a step.
            state.step == OnboardingStep.BATTERY -> OutlinedButton(
                onClick = { context.startActivity(appDetails(context.packageName)) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Text(stringResource(R.string.onboarding_battery_settings))
            }
        }
    }
}

@Composable
private fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.heightIn(min = 48.dp)) { Text(text) }
}

private fun appDetails(packageName: String) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))

@Composable
private fun PermissionButton(actions: OnboardingActions) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    var refused by rememberSaveable { mutableStateOf(false) }
    val request = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        actions.permissionAnswered()
        refused = it[Manifest.permission.ACTIVITY_RECOGNITION] != true &&
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION) == false
    }
    Button(
        onClick = {
            if (refused) {
                context.startActivity(appDetails(context.packageName))
            } else {
                request.launch(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS),
                )
            }
        },
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag(OnboardingTags.PRIMARY),
    ) {
        Text(
            stringResource(
                if (refused) R.string.onboarding_permissions_open_settings else R.string.onboarding_permissions_allow,
            ),
        )
    }
}

/** A page's heading, announced with where the run stands ([where]). */
private fun Modifier.stepHeading(where: String): Modifier = semantics {
    heading()
    stateDescription = where
}

@Composable
private fun PageTitle(text: String, where: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier.stepHeading(where),
    )
}

@Composable
private fun PageBody(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** The name and its sentence, then the three promises in words only: no picture, no badges. */
@Composable
private fun WelcomePage(where: String) {
    Text(
        text = stringResource(R.string.onboarding_app_name),
        style = MaterialTheme.typography.displaySmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.stepHeading(where),
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.onboarding_welcome_tagline),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(36.dp))
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Promise(R.string.onboarding_welcome_counts_title, R.string.onboarding_welcome_counts_body)
        Promise(R.string.onboarding_welcome_private_title, R.string.onboarding_welcome_private_body)
        Promise(R.string.onboarding_welcome_battery_title, R.string.onboarding_welcome_battery_body)
    }
}

@Composable
private fun Promise(title: Int, body: Int) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfilePage(state: OnboardingState, actions: OnboardingActions, where: String) {
    val format = rememberMeasureFormatter(state.units)
    PageTitle(stringResource(R.string.onboarding_profile_title), where)
    PageBody(stringResource(R.string.onboarding_profile_body))
    Spacer(Modifier.height(24.dp))
    val stepLength = StepLengths.estimatedWalkingStepLength(state.heightMeters, state.sex)
    PickerCard(
        title = stringResource(R.string.onboarding_height),
        scale = ProfileInputs.height(format.units),
        value = state.heightMeters,
        text = format.height(state.heightMeters).text(),
        caption = stringResource(R.string.onboarding_step_estimate, format.stepLength(stepLength).text()),
        onChange = actions.setHeight,
    )
    Spacer(Modifier.height(12.dp))
    PickerCard(
        title = stringResource(R.string.onboarding_weight),
        scale = ProfileInputs.weight(format.units),
        value = state.weightKg,
        text = format.weight(state.weightKg).text(),
        caption = null,
        onChange = actions.setWeight,
    )
    Spacer(Modifier.height(16.dp))
    Text(
        stringResource(R.string.onboarding_sex),
        style = MaterialTheme.typography.titleSmall,
        textAlign = TextAlign.Center,
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        SexChip(state.sex, Sex.FEMALE, R.string.onboarding_sex_female, actions.setSex)
        SexChip(state.sex, Sex.MALE, R.string.onboarding_sex_male, actions.setSex)
        SexChip(state.sex, null, R.string.onboarding_sex_unsaid, actions.setSex)
    }
}

@Composable
private fun SexChip(current: Sex?, value: Sex?, label: Int, onSelect: (Sex?) -> Unit) {
    FilterChip(
        selected = current == value,
        onClick = { onSelect(value) },
        label = { Text(stringResource(label)) },
        colors = chipColors(),
    )
}

/** A picker on its own ground: what it is, then the stepper. */
@Composable
private fun PickerCard(
    title: String,
    scale: InputScale,
    value: Double,
    text: String,
    caption: String?,
    onChange: (Double) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ValueStepper(
                value = value.toFloat(),
                range = scale.range.start.toFloat()..scale.range.endInclusive.toFloat(),
                text = text,
                onValueChange = { onChange(scale.snap(it.toDouble())) },
                onDecrease = { onChange(scale.down(value)) },
                onIncrease = { onChange(scale.up(value)) },
                decreaseLabel = stringResource(R.string.onboarding_decrease),
                increaseLabel = stringResource(R.string.onboarding_increase),
                caption = caption,
            )
        }
    }
}

@Composable
private fun GoalPage(state: OnboardingState, actions: OnboardingActions, where: String) {
    val format = rememberMeasureFormatter(state.units)
    PageTitle(stringResource(R.string.onboarding_goal_title), where)
    Spacer(Modifier.height(24.dp))
    val profile = com.callbackdev.passo.core.model.Profile(
        heightMeters = state.heightMeters.takeUnless { state.profileSkipped },
        sex = state.sex,
    )
    val distance = format.distance(state.goalSteps * StepLengths.of(profile).walkingMeters)
    val minutes = TodayOverview.minutesToWalk(state.goalSteps)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ValueStepper(
                value = state.goalSteps.toFloat(),
                range = ProfileInputs.goal.range.start.toFloat()..ProfileInputs.goal.range.endInclusive.toFloat(),
                text = format.steps(state.goalSteps),
                onValueChange = { actions.setGoal(ProfileInputs.goal.snap(it.toDouble()).toInt()) },
                onDecrease = { actions.setGoal(ProfileInputs.goal.down(state.goalSteps.toDouble()).toInt()) },
                onIncrease = { actions.setGoal(ProfileInputs.goal.up(state.goalSteps.toDouble()).toInt()) },
                decreaseLabel = stringResource(R.string.onboarding_decrease),
                increaseLabel = stringResource(R.string.onboarding_increase),
                caption = stringResource(R.string.onboarding_goal_consequence, distance.text(), duration(minutes)),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    // Quick picks: the goals people actually choose, one tap each.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth(),
    ) {
        for (preset in GOAL_PRESETS) {
            FilterChip(
                selected = state.goalSteps == preset,
                onClick = { actions.setGoal(preset) },
                label = { Text(format.steps(preset)) },
                colors = chipColors(),
            )
        }
    }
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.onboarding_goal_note),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/** A picked chip in the accent, like the stepper beside it (Material's default is the amber secondary). */
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

private val GOAL_PRESETS = listOf(5_000, 8_000, 10_000, 12_000)

@Composable
private fun duration(minutes: Int): String = if (minutes < 60) {
    pluralStringResource(R.plurals.onboarding_minutes, minutes, minutes)
} else {
    stringResource(R.string.onboarding_hours_minutes, minutes / 60, minutes % 60)
}

@Composable
private fun PermissionsPage(state: OnboardingState, where: String) {
    PageTitle(stringResource(R.string.onboarding_permissions_title), where)
    Spacer(Modifier.height(24.dp))
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Promise(R.string.onboarding_permissions_activity_title, R.string.onboarding_permissions_activity_body)
        Promise(R.string.onboarding_permissions_notifications_title, R.string.onboarding_permissions_notifications_body)
    }
    if (state.activityGranted) {
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PassoIcons.Check, contentDescription = null, tint = PassoTheme.colors.goal)
            Text(
                text = stringResource(R.string.onboarding_permissions_granted),
                style = MaterialTheme.typography.titleSmall,
                color = PassoTheme.colors.goal,
                modifier = Modifier.testTag(OnboardingTags.GRANTED),
            )
        }
    }
    // A refusal is the reader's answer, not a fault: said in the page's quiet ink, with the way on.
    if (!state.activityGranted && state.permissionAsked) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.onboarding_permissions_denied),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BatteryPage(state: OnboardingState, where: String) {
    PageTitle(stringResource(R.string.onboarding_battery_title, state.manufacturer), where)
    PageBody(stringResource(R.string.onboarding_battery_body))
}

/** Hooks for the UI tests. */
object OnboardingTags {
    const val ROOT = "onboarding"
    const val PAGE = "onboarding_page"
    const val PRIMARY = "onboarding_primary"
    const val SKIP = "onboarding_skip"
    const val GRANTED = "onboarding_granted"
}
