package com.callbackdev.passo.feature.sessions

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.IntervalBlocks
import com.callbackdev.passo.core.designsystem.components.SessionCard
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SessionMarkIconSize
import com.callbackdev.passo.core.designsystem.components.SessionMarkSize
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.components.WayMapView
import com.callbackdev.passo.core.designsystem.components.reveal
import com.callbackdev.passo.core.designsystem.components.sessionIcon
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.planDescription
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.unorphaned
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoMotion
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.padding
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.SessionConstants
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.tracking.GoalNotificationsBlock
import com.callbackdev.passo.core.tracking.SessionSignalsAccess

/** The Outings page with its state, and the notification permission it may ask for. */
@Composable
fun SessionsRoute(
    onBack: () -> Unit,
    onEdit: (Long?) -> Unit,
    onOpenWays: () -> Unit = {},
    viewModel: SessionsViewModel = hiltViewModel(),
) {
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
            openWays = onOpenWays,
        ),
    )
}

/** What the page can ask for, as functions: the screen is a plain composable a test can draw. */
class SessionsActions(
    val start: (Long) -> Unit = {},
    val card: SessionCardActions = SessionCardActions(),
    val resumeTracking: () -> Unit = {},
    val openSignalSettings: () -> Unit = {},
    val openWays: () -> Unit = {},
)

/**
 * The Outings page (PLANNING.md §11 Phase 10): one sentence of what an outing is, what stands in
 * the way if anything does (a paused count, silenced signals), the outing under way, then the
 * reader's outings, each with what it is, what it comes to and its signals, one touch from
 * starting. Editing is a tap on the card; a new one is the bar's own action, in reach wherever
 * the list stands.
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
                actions = {
                    IconButton(onClick = { onEdit(null) }, modifier = Modifier.testTag(SessionsTags.NEW)) {
                        Icon(PassoIcons.Plus, contentDescription = stringResource(R.string.sessions_new))
                    }
                },
                windowInsets = TopAppBarDefaults.windowInsets.add(pageGutter(sideInsets = false).asInsets()),
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
    val list = rememberLazyListState()
    val reduced = reducedMotion()
    val haptics = LocalHapticFeedback.current
    // Started from this page, the outing's card comes in at the top, often far above the Start
    // that was touched: the page glides to it, so the reader sees what Start did. Only then: a
    // page opened on an outing under way is already at its top.
    var startedHere by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(busy) {
        if (busy && startedHere) {
            startedHere = false
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            list.reveal(key = LIVE_KEY, index = 0, reduced = reduced)
        }
    }
    val start = { id: Long ->
        startedHere = true
        actions.start(id)
    }
    // Cards that move when the outing's card comes or goes do so on the page's springs.
    val placement: FiniteAnimationSpec<IntOffset>? = if (reduced) null else PassoMotion.spatial()
    LazyColumn(
        state = list,
        modifier = modifier.testTag(SessionsTags.LIST),
        contentPadding = pageGutter(sideInsets = false).contentPadding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Text(
                text = stringResource(R.string.sessions_intro),
                style = MaterialTheme.typography.bodyMedium,
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
            item(key = LIVE_KEY) {
                SessionCard(
                    session = live.session,
                    cadence = live.cadence,
                    canKeepGoing = live.canKeepGoing,
                    format = format,
                    actions = actions.card,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = PassoMotion.effects(reduced),
                            placementSpec = placement,
                            fadeOutSpec = PassoMotion.effects(reduced),
                        )
                        .padding(horizontal = ScreenMargin),
                )
            }
        }
        item(key = "header") {
            Text(
                text = stringResource(R.string.sessions_yours),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .animateItem(placementSpec = placement)
                    .padding(start = 20.dp, end = ScreenMargin, top = 8.dp)
                    .semantics { heading() },
            )
        }
        if (busy) {
            item(key = "busy") {
                Text(
                    text = stringResource(R.string.sessions_live_busy),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = PassoMotion.effects(reduced),
                            placementSpec = placement,
                            fadeOutSpec = PassoMotion.effects(reduced),
                        )
                        .padding(horizontal = 20.dp),
                )
            }
        }
        if (state.plans.isEmpty()) {
            // With nothing kept, the way to make one is said where the list would be, too.
            item(key = "empty") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(horizontal = ScreenMargin),
                ) {
                    Text(
                        text = stringResource(R.string.sessions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    OutlinedButton(
                        onClick = { onEdit(null) },
                        modifier = Modifier.fillMaxWidth().testTag(SessionsTags.NEW_FIRST),
                    ) {
                        Icon(
                            PassoIcons.Plus,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize),
                        )
                        Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                        Text(stringResource(R.string.sessions_new))
                    }
                }
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
                onStart = { start(plan.id) },
                onEdit = { onEdit(plan.id) },
                modifier = Modifier.animateItem(placementSpec = placement),
            )
        }
        item(key = "cities") { CitiesDoor(actions.openWays) }
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

/**
 * One kept outing: what it is, what it comes to, its signals, all on the line its name starts
 * on; a tap on the card edits it, the button at its end starts it.
 */
@Composable
private fun PlanCard(
    plan: SessionPlan,
    state: SessionsUiState,
    format: MeasureFormatter,
    canStart: Boolean,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    val name = res.sessionName(plan)
    val estimate = estimateLine(plan, state, format)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenMargin)
            .testTag("${SessionsTags.PLAN}-${plan.id}"),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .clickable(onClickLabel = stringResource(R.string.sessions_edit), onClick = onEdit)
                .padding(start = 16.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
        ) {
            PlanMark(sessionIcon(plan.intensity, plan.goalKind))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = res.planDescription(plan, format).unorphaned(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Shown short, with the sign of an estimate; read whole by TalkBack.
                Text(
                    text = estimate.shown,
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clearAndSetSemantics { text = AnnotatedString(estimate.spoken) },
                )
                SignalsLine(plan, format, modifier = Modifier.padding(top = 2.dp))
                // An interval plan shows its shape: slow and fast blocks, read before any number.
                if (plan.goalKind == SessionGoalKind.INTERVALS) {
                    IntervalBlocks(
                        sets = plan.intervals,
                        progress = 0f,
                        color = MaterialTheme.colorScheme.primary,
                        plan = true,
                        modifier = Modifier.padding(top = 10.dp).clearAndSetSemantics {},
                    )
                }
            }
            // The icon alone: with its word the button took a row of its own, or the name's room.
            FilledTonalIconButton(
                onClick = onStart,
                enabled = canStart,
                modifier = Modifier.size(START_SIZE).testTag("${SessionsTags.START}-${plan.id}"),
            ) {
                Icon(PassoIcons.Play, contentDescription = stringResource(R.string.sessions_start_named, name))
            }
        }
    }
}

@Composable
private fun PlanMark(icon: ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = CircleShape,
        modifier = Modifier.size(SessionMarkSize),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(SessionMarkIconSize),
            )
        }
    }
}

private val START_SIZE = 48.dp

/** A line of a plan's card as it is shown and as it is read aloud. */
private class CardLine(val shown: String, val spoken: String)

/**
 * «≈ 2,000 steps · 1.40 km», read as «About 2,000 steps · 1.40 km, estimated»; for the rest of
 * the day, what it is right now.
 */
@Composable
private fun estimateLine(plan: SessionPlan, state: SessionsUiState, format: MeasureFormatter): CardLine {
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
    val (first, second) = when (plan.goalKind) {
        SessionGoalKind.TIME, SessionGoalKind.INTERVALS -> steps to distance

        SessionGoalKind.STEPS -> minutes to distance

        SessionGoalKind.DISTANCE -> steps to minutes

        SessionGoalKind.REST_OF_DAY -> {
            if (state.restOfDaySteps < SessionConstants.MIN_REST_OF_DAY_STEPS) {
                return stringResource(R.string.sessions_rest_met).let { CardLine(it, it) }
            }
            return CardLine(
                shown = stringResource(R.string.sessions_rest_now_short, steps, distance),
                spoken = stringResource(R.string.sessions_rest_now, steps, distance),
            )
        }
    }
    return CardLine(
        shown = stringResource(R.string.sessions_estimate_short, first, second),
        spoken = stringResource(R.string.sessions_estimate, first, second),
    )
}

/**
 * «Signals at 25%, 50% and at the goal», with how they arrive as marks before it (a vibration,
 * a voice) and in words for TalkBack: «…, with vibration and voice».
 */
@Composable
private fun SignalsLine(plan: SessionPlan, format: MeasureFormatter, modifier: Modifier = Modifier) {
    val shares = plan.milestones.sortedBy { it.percent }.joinToString(", ") { format.percent(it.percent / 100.0) }
    val signals = if (plan.goalKind == SessionGoalKind.INTERVALS) {
        stringResource(R.string.sessions_signals_intervals)
    } else if (shares.isEmpty()) {
        stringResource(R.string.sessions_signals_goal_only)
    } else {
        stringResource(R.string.sessions_signals, shares)
    }
    val voice = plan.voice != SessionVoice.OFF
    val spoken = when {
        plan.vibrate && voice -> stringResource(R.string.sessions_signals_vibrate_voice, signals)
        plan.vibrate -> stringResource(R.string.sessions_signals_vibrate, signals)
        voice -> stringResource(R.string.sessions_signals_voice, signals)
        else -> signals
    }
    val style = MaterialTheme.typography.bodySmall
    // The marks grow with the text they stand before.
    val mark = with(LocalDensity.current) { style.lineHeight.toDp() } - 2.dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.clearAndSetSemantics { text = AnnotatedString(spoken) },
    ) {
        if (plan.vibrate) SignalMark(PassoIcons.Vibrate, mark)
        if (voice) SignalMark(PassoIcons.Voice, mark)
        Text(
            text = signals,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = if (plan.vibrate || voice) 2.dp else 0.dp),
        )
    }
}

@Composable
private fun SignalMark(icon: ImageVector, size: Dp) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 1.dp).size(size),
    )
}

/** The outing under way's item in the list: the page glides to it when an outing starts here. */
private const val LIVE_KEY = "live"

/** Hooks for the UI tests. */
object SessionsTags {
    const val LIST = "sessions_list"
    const val PLAN = "sessions_plan"
    const val START = "sessions_start"
    const val NEW = "sessions_new"
    const val NEW_FIRST = "sessions_new_first"
    const val BLOCKED = "sessions_blocked"
    const val CITIES = "sessions_cities"
}

/**
 * The door to the city walks (Phase 11): an outing whose goal is a walk through a city, its
 * places told on the way. They live on the Ways page, with the ways.
 */
@Composable
private fun CitiesDoor(onOpen: () -> Unit) {
    SettingsGroup(modifier = Modifier.padding(top = 4.dp).testTag(SessionsTags.CITIES)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen, role = Role.Button)
                .padding(horizontal = ScreenMargin, vertical = 14.dp),
        ) {
            // A city's walk as its door: the first of the two the line under it names.
            WayMapView(
                way = remember { Ways.of(DOOR_WALK) },
                walkedMeters = null,
                reached = 0,
                contentDescription = "",
                detailed = false,
                ratio = 1f,
                modifier = Modifier.size(DOOR_MAP_SIZE),
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.sessions_cities_title), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.sessions_cities_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private val DOOR_WALK = WayId.MILAN_DUOMO_NAVIGLI
private val DOOR_MAP_SIZE = 56.dp
