package com.callbackdev.passo.feature.sessions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.SessionCard
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.components.sessionIcon
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.planDescription
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.SessionConstants
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.tracking.GoalNotificationsBlock
import com.callbackdev.passo.core.tracking.SessionSignalsAccess

/** The Outings page with its state, and the notification permission it may ask for. */
@Composable
fun SessionsRoute(onBack: () -> Unit, onEdit: (Long?) -> Unit, viewModel: SessionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var block by remember { mutableStateOf(SessionSignalsAccess.block(context)) }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshReadiness()
        block = SessionSignalsAccess.block(context)
        onPauseOrDispose {}
    }
    // Asked the first time an outing starts without it: its signals are notifications. The
    // outing starts either way; the page then says what the refusal costs.
    var pendingStart by rememberSaveable { mutableStateOf<Long?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        block = SessionSignalsAccess.block(context)
        pendingStart?.let(viewModel::start)
        pendingStart = null
    }
    SessionsScreen(
        state = state,
        signalsBlock = block,
        onBack = onBack,
        onEdit = onEdit,
        actions = SessionsActions(
            start = { id ->
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.start(id)
                } else {
                    pendingStart = id
                    permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            card = SessionCardActions(
                onPause = viewModel::pause,
                onResume = viewModel::resume,
                onStop = viewModel::stop,
                onKeepGoing = viewModel::keepGoing,
            ),
            resumeTracking = viewModel::resumeTracking,
            openSignalSettings = {
                runCatching { context.startActivity(SessionSignalsAccess.settingsIntent(context, block)) }
            },
        ),
    )
}

/** What the page can ask for, as functions: the screen is a plain composable a test can draw. */
class SessionsActions(
    val start: (Long) -> Unit = {},
    val card: SessionCardActions = SessionCardActions(),
    val resumeTracking: () -> Unit = {},
    val openSignalSettings: () -> Unit = {},
)

/**
 * The Outings page (PLANNING.md §11 Phase 10): one sentence of what an outing is, what stands in
 * the way if anything does (a paused count, silenced signals), the outing under way, then the
 * reader's outings, each with what it is, what it comes to and its signals, one touch from
 * starting. Editing is a tap on the card; a new one is at the end of the list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    state: SessionsUiState?,
    signalsBlock: GoalNotificationsBlock,
    onBack: () -> Unit,
    onEdit: (Long?) -> Unit,
    actions: SessionsActions,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sessions_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.sessions_back))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the plans are read, a bare page: an empty list would say there are none.
        if (state != null) SessionsList(state, signalsBlock, onEdit, actions, Modifier.fillMaxSize().padding(padding))
    }
}

@Composable
private fun SessionsList(
    state: SessionsUiState,
    signalsBlock: GoalNotificationsBlock,
    onEdit: (Long?) -> Unit,
    actions: SessionsActions,
    modifier: Modifier,
) {
    val format = rememberMeasureFormatter(state.units)
    val live = state.live
    val busy = live?.session?.live == true
    val canStart = state.status == SessionsStatus.READY && !busy
    LazyColumn(
        modifier = modifier.testTag(SessionsTags.LIST),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Text(
                text = stringResource(R.string.sessions_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        when (state.status) {
            SessionsStatus.READY -> Unit

            SessionsStatus.PAUSED -> item(key = "paused") {
                StatusCard(
                    icon = PassoIcons.Pause,
                    title = stringResource(R.string.sessions_paused_title),
                    body = stringResource(R.string.sessions_paused_body),
                    tone = StatusTone.CHOICE,
                    action = stringResource(R.string.sessions_paused_action),
                    onAction = actions.resumeTracking,
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                )
            }

            SessionsStatus.PERMISSION_NEEDED -> item(key = "permission") {
                StatusCard(
                    icon = PassoIcons.Warning,
                    title = stringResource(R.string.sessions_permission_title),
                    body = stringResource(R.string.sessions_permission_body),
                    tone = StatusTone.PROBLEM,
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                )
            }
        }
        if (signalsBlock != GoalNotificationsBlock.NONE) {
            item(key = "blocked") {
                StatusCard(
                    icon = PassoIcons.Bell,
                    title = stringResource(
                        if (signalsBlock == GoalNotificationsBlock.CHANNEL) {
                            R.string.sessions_blocked_channel_title
                        } else {
                            R.string.sessions_blocked_title
                        },
                    ),
                    body = stringResource(R.string.sessions_blocked_body),
                    tone = StatusTone.PROBLEM,
                    action = stringResource(R.string.sessions_blocked_action),
                    onAction = actions.openSignalSettings,
                    modifier = Modifier.padding(horizontal = ScreenMargin).testTag(SessionsTags.BLOCKED),
                )
            }
        }
        if (live != null) {
            item(key = "live") {
                SessionCard(
                    session = live.session,
                    cadence = live.cadence,
                    canKeepGoing = live.canKeepGoing,
                    format = format,
                    actions = actions.card,
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                )
            }
        }
        item(key = "header") {
            Text(
                text = stringResource(R.string.sessions_yours),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 20.dp, end = ScreenMargin, top = 8.dp).semantics { heading() },
            )
        }
        if (busy) {
            item(key = "busy") {
                Text(
                    text = stringResource(R.string.sessions_live_busy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        if (state.plans.isEmpty()) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.sessions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
        items(state.plans, key = { "plan-${it.id}" }) { plan ->
            val restMet = plan.goalKind == SessionGoalKind.REST_OF_DAY &&
                state.restOfDaySteps < SessionConstants.MIN_REST_OF_DAY_STEPS
            PlanCard(
                plan = plan,
                state = state,
                format = format,
                canStart = canStart && !restMet,
                onStart = { actions.start(plan.id) },
                onEdit = { onEdit(plan.id) },
            )
        }
        item(key = "new") {
            OutlinedButton(
                onClick = { onEdit(null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(SessionsTags.NEW),
            ) {
                Icon(PassoIcons.Plus, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.sessions_new))
            }
        }
        item(key = "footer") {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.sessions_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!state.alertsWhileScreenOff) {
                    Text(
                        text = stringResource(R.string.sessions_footer_late),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** One kept outing: what it is, what it comes to, its signals; tap to edit, Start to go. */
@Composable
private fun PlanCard(
    plan: SessionPlan,
    state: SessionsUiState,
    format: MeasureFormatter,
    canStart: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
) {
    val res = LocalResources.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .testTag("${SessionsTags.PLAN}-${plan.id}"),
    ) {
        Column(
            modifier = Modifier.clickable(
                onClick = onEdit,
            ).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PlanMark(sessionIcon(plan.intensity))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(res.sessionName(plan), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = res.planDescription(plan, format),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = estimateLine(plan, state, format),
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = signalsLine(plan, format),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.sessions_edit)) }
                FilledTonalButton(
                    onClick = onStart,
                    enabled = canStart,
                    modifier = Modifier.testTag("${SessionsTags.START}-${plan.id}"),
                ) {
                    Icon(PassoIcons.Play, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                    Text(stringResource(R.string.sessions_start))
                }
            }
        }
    }
}

@Composable
private fun PlanMark(icon: ImageVector) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** «About 2,000 steps · 1.40 km, estimated»; for the rest of the day, what it is right now. */
@Composable
private fun estimateLine(plan: SessionPlan, state: SessionsUiState, format: MeasureFormatter): String {
    val res = LocalResources.current
    val estimate = SessionPlans.estimate(plan, state.lengths, state.restOfDaySteps)
    val distance = res.format(format.distance(estimate.distanceMeters))
    val steps = res.getQuantityString(
        com.callbackdev.passo.core.designsystem.R.plurals.session_amount_steps,
        estimate.steps,
        format.steps(estimate.steps),
    )
    val minutes = res.format(format.minutes(estimate.minutes))
    // The two quantities the goal is not in.
    return when {
        plan.goalKind == SessionGoalKind.TIME -> stringResource(R.string.sessions_estimate, steps, distance)

        plan.goalKind == SessionGoalKind.STEPS -> stringResource(R.string.sessions_estimate, minutes, distance)

        plan.goalKind == SessionGoalKind.DISTANCE -> stringResource(R.string.sessions_estimate, steps, minutes)

        state.restOfDaySteps < SessionConstants.MIN_REST_OF_DAY_STEPS -> stringResource(R.string.sessions_rest_met)

        else -> stringResource(
            R.string.sessions_rest_now,
            steps,
            distance,
        )
    }
}

/** «Signals at 25%, 50% and at the goal, with vibration and voice». */
@Composable
private fun signalsLine(plan: SessionPlan, format: MeasureFormatter): String {
    val shares = plan.milestones.sortedBy { it.percent }.joinToString(", ") { format.percent(it.percent / 100.0) }
    val signals = if (shares.isEmpty()) {
        stringResource(R.string.sessions_signals_goal_only)
    } else {
        stringResource(R.string.sessions_signals, shares)
    }
    val voice = plan.voice != SessionVoice.OFF
    return when {
        plan.vibrate && voice -> stringResource(R.string.sessions_signals_vibrate_voice, signals)
        plan.vibrate -> stringResource(R.string.sessions_signals_vibrate, signals)
        voice -> stringResource(R.string.sessions_signals_voice, signals)
        else -> signals
    }
}

/** Hooks for the UI tests. */
object SessionsTags {
    const val LIST = "sessions_list"
    const val PLAN = "sessions_plan"
    const val START = "sessions_start"
    const val NEW = "sessions_new"
    const val BLOCKED = "sessions_blocked"
}
