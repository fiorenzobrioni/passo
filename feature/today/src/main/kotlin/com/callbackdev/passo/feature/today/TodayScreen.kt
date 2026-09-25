package com.callbackdev.passo.feature.today

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.DayTrend
import com.callbackdev.passo.core.designsystem.components.DayTrendChart
import com.callbackdev.passo.core.designsystem.components.MetricTile
import com.callbackdev.passo.core.designsystem.components.MetricTrack
import com.callbackdev.passo.core.designsystem.components.ProgressRing
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.components.TrendPoint
import com.callbackdev.passo.core.designsystem.components.OutingList
import com.callbackdev.passo.core.designsystem.components.SessionCard
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.axisHour
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.longDate
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.today.CadenceBand
import com.callbackdev.passo.core.domain.today.Headline
import com.callbackdev.passo.core.domain.today.Pace
import com.callbackdev.passo.core.domain.sessions.Outing
import java.time.LocalDate

/** Today, with its state from [TodayViewModel] and the permission request it may need. */
@Composable
fun TodayRoute(
    onOpenSettings: () -> Unit,
    onOpenSessions: () -> Unit = {},
    bottomPadding: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalActivity.current
    val context = LocalContext.current
    // The system stops showing its dialog after the second refusal; from then on only the
    // app's settings page can grant the permission.
    var askInSettings by rememberSaveable { mutableStateOf(false) }
    val permissionRequest = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        viewModel.refreshReadiness()
        val granted = it[Manifest.permission.ACTIVITY_RECOGNITION] == true
        val rationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)
        if (!granted && rationale == false) {
            askInSettings = true
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.refreshReadiness()
        onPauseOrDispose { }
    }

    val current = state ?: return Box(Modifier.fillMaxSize())
    TodayScreen(
        state = current,
        askInSettings = askInSettings,
        onOpenSettings = onOpenSettings,
        onAllow = {
            if (askInSettings) {
                openAppSettings(activity ?: return@TodayScreen, context.packageName)
            } else {
                permissionRequest.launch(
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.POST_NOTIFICATIONS),
                )
            }
        },
        onResume = viewModel::resumeTracking,
        onCelebrated = viewModel::celebrated,
        bottomPadding = bottomPadding,
        onOpenSessions = onOpenSessions,
        sessionActions = { id ->
            SessionCardActions(
                onPause = viewModel::pauseSession,
                onResume = viewModel::resumeSession,
                onStop = viewModel::stopSession,
                onKeepGoing = viewModel::keepGoing,
                onClose = { viewModel.closeSession(id) },
            )
        },
    )
}

private fun openAppSettings(activity: Activity, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    activity.startActivity(intent)
}

/**
 * Today (PLANNING.md §11 Phase 3), in Chiaro's order: the state of counting if it needs the
 * reader, then **one sentence before any number** under the ring, then the day as a shape, then
 * the metrics, each with the line that says what it means.
 */
@Composable
fun TodayScreen(
    state: TodayUiState,
    askInSettings: Boolean,
    onOpenSettings: () -> Unit,
    onAllow: () -> Unit,
    onResume: () -> Unit,
    onCelebrated: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    bottomPadding: Dp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    onOpenSessions: () -> Unit = {},
    sessionActions: (Long) -> SessionCardActions = { SessionCardActions() },
) {
    val format = rememberMeasureFormatter(state.units)
    val bottom = bottomPadding
    // Its own ground and ink, so the page reads right wherever it is drawn.
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag(TodayTags.LIST),
            contentPadding = PaddingValues(bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                Hero(state, format, askInSettings, onOpenSettings, onAllow, onResume, onCelebrated)
            }
            val session = state.session
            if (session != null) {
                item(key = "session") {
                    SessionCard(
                        session = session.session,
                        cadence = session.cadence,
                        canKeepGoing = session.canKeepGoing,
                        format = format,
                        actions = sessionActions(session.session.id),
                        modifier = Modifier.padding(horizontal = ScreenMargin),
                    )
                }
            } else if (state.status == TrackingStatus.COUNTING) {
                item(key = "start-outing") { StartOuting(onOpenSessions) }
            }
            if (state.firstDay && state.status == TrackingStatus.COUNTING) {
                item(key = "first-day") {
                    StatusCard(
                        icon = PassoIcons.Info,
                        title = stringResource(R.string.today_first_day_title),
                        body = stringResource(R.string.today_first_day_body),
                        tone = StatusTone.NOTE,
                        modifier = Modifier.padding(horizontal = ScreenMargin),
                    )
                }
            }
            item(key = "trend") { TrendCard(state, format) }
            if (state.outings.isNotEmpty()) item(key = "walks") { WalksCard(state.outings, format) }
            item(key = "metrics") { Metrics(state, format) }
        }
    }
}

/**
 * The top of the page: the date and the gear, the state of counting when it needs the reader,
 * the ring and the sentence. It stands on a glow of the day's color, primary while the goal is
 * ahead and the goal's green once it is met, fading into the page: the color of the screen is
 * the first thing it says.
 */
@Composable
private fun Hero(
    state: TodayUiState,
    format: MeasureFormatter,
    askInSettings: Boolean,
    onOpenSettings: () -> Unit,
    onAllow: () -> Unit,
    onResume: () -> Unit,
    onCelebrated: (LocalDate) -> Unit,
) {
    val overview = state.overview
    val reached = overview.goalReachedAt != null
    val glow by animateColorAsState(
        targetValue = if (reached) PassoTheme.colors.goalContainer else MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(600),
        label = "glow",
    )
    val surface = MaterialTheme.colorScheme.surface
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to glow.copy(alpha = 0.75f),
                    0.55f to glow.copy(alpha = 0.18f),
                    1f to surface,
                ),
            )
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp, top = 4.dp),
        ) {
            Text(
                text = longDate(state.date),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            IconButton(onClick = onOpenSettings) {
                Icon(PassoIcons.Settings, contentDescription = stringResource(R.string.today_settings))
            }
        }
        StatusBanner(state.status, askInSettings, onAllow, onResume)
        Ring(state, format, onCelebrated)
        Headline(state, format)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatusBanner(status: TrackingStatus, askInSettings: Boolean, onAllow: () -> Unit, onResume: () -> Unit) {
    val modifier = Modifier.padding(horizontal = ScreenMargin, vertical = 8.dp).testTag(TodayTags.STATUS)
    when (status) {
        TrackingStatus.COUNTING -> Unit

        TrackingStatus.PERMISSION_NEEDED -> StatusCard(
            icon = PassoIcons.Warning,
            title = stringResource(R.string.today_status_permission_title),
            body = stringResource(R.string.today_status_permission_body),
            tone = StatusTone.PROBLEM,
            action = if (askInSettings) {
                stringResource(R.string.today_status_permission_settings)
            } else {
                stringResource(R.string.today_status_permission_action)
            },
            onAction = onAllow,
            modifier = modifier,
        )

        TrackingStatus.PAUSED -> StatusCard(
            icon = PassoIcons.Pause,
            title = stringResource(R.string.today_status_paused_title),
            body = stringResource(R.string.today_status_paused_body),
            tone = StatusTone.CHOICE,
            action = stringResource(R.string.today_status_paused_action),
            onAction = onResume,
            modifier = modifier,
        )
    }
}

@Composable
private fun Ring(state: TodayUiState, format: MeasureFormatter, onCelebrated: (LocalDate) -> Unit) {
    val overview = state.overview
    val reached = overview.goalReachedAt != null
    val description = buildString {
        if (reached) append(stringResource(R.string.today_ring_description_reached)).append(' ')
        append(
            stringResource(
                R.string.today_ring_description,
                format.steps(overview.steps),
                format.steps(overview.goalSteps),
                format.percent(overview.progress),
            ),
        )
        overview.usualNow?.let {
            append(' ').append(stringResource(R.string.today_ring_description_usual, format.steps(it)))
        }
    }
    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        val diameter = minOf(maxWidth - 64.dp, 300.dp)
        ProgressRing(
            progress = overview.progress.toFloat(),
            usualProgress = overview.usualProgress?.toFloat(),
            reached = reached,
            celebrate = state.celebrate,
            onCelebrated = { onCelebrated(state.date) },
            modifier = Modifier
                .size(diameter)
                .clearAndSetSemantics { contentDescription = description }
                .testTag(TodayTags.RING),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CountUp(overview.steps, format)
                Text(
                    text = stringResource(R.string.today_of_goal, format.steps(overview.goalSteps)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                if (reached) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            PassoIcons.Check,
                            contentDescription = null,
                            tint = PassoTheme.colors.goal,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.today_goal_reached),
                            style = MaterialTheme.typography.labelLarge,
                            color = PassoTheme.colors.goal,
                        )
                    }
                } else {
                    Text(
                        text = format.percent(overview.progress),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * The count, in the hero's voice, counting up from zero the first time the screen shows it and
 * then rolling to each new value. The size steps down with the digits so a long day still fits
 * the ring. Straight to the value under reduced motion.
 */
@Composable
private fun CountUp(steps: Int, format: MeasureFormatter) {
    val reduced = reducedMotion()
    var counted by rememberSaveable { mutableStateOf(false) }
    val shown = remember { Animatable(if (counted || reduced) steps.toFloat() else 0f) }
    LaunchedEffect(steps, reduced) {
        if (reduced) {
            shown.snapTo(steps.toFloat())
        } else {
            shown.animateTo(steps.toFloat(), tween(if (counted) 500 else 1_100, easing = FastOutSlowInEasing))
        }
        counted = true
    }
    val digits = steps.toString().length
    val size = when {
        digits <= 4 -> 60.sp
        digits == 5 -> 52.sp
        else -> 42.sp
    }
    Text(
        text = format.steps(shown.value.toInt()),
        style = PassoTheme.type.heroNumber.copy(fontSize = size, lineHeight = size * 1.08f),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
    )
}

@Composable
private fun Headline(state: TodayUiState, format: MeasureFormatter) {
    val overview = state.overview
    val headline = overview.headline
    val icon: ImageVector = when (headline) {
        Headline.NoStepsYet -> PassoIcons.Steps
        is Headline.GoalReached -> PassoIcons.Flag
        is Headline.ToGo -> PassoIcons.Flag
        is Headline.VersusUsual -> paceIcon(headline.pace)
    }
    val tint = when {
        headline is Headline.GoalReached -> PassoTheme.colors.goal
        headline is Headline.VersusUsual && headline.pace is Pace.Ahead -> PassoTheme.colors.goal
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .semantics(mergeDescendants = true) { }
            .testTag(TodayTags.HEADLINE),
    ) {
        // The mark rides the sentence as its first word, so it wraps with it instead of standing
        // beside a paragraph.
        val sentence = headlineText(headline, format)
        Text(
            text = buildAnnotatedString {
                appendInlineContent(HEADLINE_MARK, " ")
                append(' ')
                append(sentence)
            },
            inlineContent = mapOf(
                HEADLINE_MARK to InlineTextContent(
                    Placeholder(1.1.em, 1.em, PlaceholderVerticalAlign.TextCenter),
                ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.fillMaxSize()) },
            ),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        val detail = overview.detail
        val detailText = when {
            detail != null -> headlineText(detail, format)

            headline is Headline.GoalReached && headline.over > 0 ->
                pluralStringResource(R.plurals.today_headline_over, headline.over, format.steps(headline.over))

            else -> null
        }
        if (detailText != null) {
            Text(
                text = detailText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private const val HEADLINE_MARK = "mark"

private fun paceIcon(pace: Pace): ImageVector = when (pace) {
    is Pace.Ahead -> PassoIcons.TrendUp
    is Pace.Behind -> PassoIcons.TrendDown
    Pace.OnPace -> PassoIcons.TrendFlat
}

@Composable
private fun TrendCard(state: TodayUiState, format: MeasureFormatter) {
    val overview = state.overview
    val curve = overview.curve
    val nowIndex = (state.nowMinute / curve.stepMinutes).toInt().coerceIn(0, curve.size - 1)
    val trend = remember(overview, state.nowMinute) {
        DayTrend(
            today = (0..nowIndex).map { curve[it] } + listOf(overview.steps),
            stepMinutes = curve.stepMinutes,
            nowMinute = state.nowMinute.toFloat(),
            usual = overview.typical?.curve?.toList(),
            usualStepMinutes = overview.typical?.curve?.stepMinutes ?: MetricsConstants.TYPICAL_DAY_SLOT_MINUTES,
            goal = overview.goalSteps,
        )
    }
    val description = buildString {
        append(pluralStringResource(R.plurals.today_trend_description, overview.steps, format.steps(overview.steps)))
        when (val pace = overview.pace) {
            is Pace.Ahead -> append(' ').append(
                pluralStringResource(R.plurals.today_trend_description_ahead, pace.steps, format.steps(pace.steps)),
            )

            is Pace.Behind -> append(' ').append(
                pluralStringResource(R.plurals.today_trend_description_behind, pace.steps, format.steps(pace.steps)),
            )

            Pace.OnPace -> append(' ').append(stringResource(R.string.today_trend_description_on_pace))

            null -> Unit
        }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(TodayTags.TREND),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.today_trend_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                Legend(hasUsual = trend.usual != null)
            }
            val hours = listOf(0, 6, 12, 18).map { it * 60 to axisHour(it) }
            DayTrendChart(
                trend = trend,
                goalLabel = format.steps(overview.goalSteps),
                hourLabels = hours,
                description = description,
                readout = { point -> Readout(point, overview.steps, overview.usualNow, format) },
                modifier = Modifier.testTag(TodayTags.CHART),
            )
            Text(
                text = if (trend.usual != null) {
                    stringResource(R.string.today_trend_caption)
                } else {
                    stringResource(R.string.today_trend_caption_no_usual)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The line above the plot: now, or the minute under the finger. */
@Composable
private fun Readout(point: TrendPoint?, stepsNow: Int, usualNow: Int?, format: MeasureFormatter) {
    val time = point?.let { clockTime(it.minuteOfDay) } ?: stringResource(R.string.today_trend_now)
    val steps = if (point == null) stepsNow else point.steps
    val usual = if (point == null) usualNow else point.usual
    val stepsText = steps?.let { pluralStringResource(R.plurals.today_trend_steps, it, format.steps(it)) }
    val usualText = usual?.let { stringResource(R.string.today_trend_usual, format.steps(it)) }
    val primary = MaterialTheme.colorScheme.primary
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(time) }
            if (stepsText != null) {
                append("  ·  ")
                withStyle(SpanStyle(color = primary, fontWeight = FontWeight.SemiBold)) { append(stepsText) }
            }
            if (usualText != null) {
                append("  ·  ")
                append(usualText)
            }
        },
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun Legend(hasUsual: Boolean) {
    val today = MaterialTheme.colorScheme.primary
    val usual = MaterialTheme.colorScheme.onSurfaceVariant
    val goal = PassoTheme.colors.goal
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(stringResource(R.string.today_trend_legend_today)) {
            drawSwatch(today, dashed = false, width = 2.5f)
        }
        if (hasUsual) {
            LegendItem(stringResource(R.string.today_trend_legend_usual)) {
                drawSwatch(usual, dashed = true, width = 1.5f)
            }
        }
        LegendItem(stringResource(R.string.today_trend_legend_goal)) { drawSwatch(goal, dashed = false, width = 1.5f) }
    }
}

@Composable
private fun LegendItem(label: String, swatch: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.width(14.dp).height(8.dp)) { swatch() }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSwatch(color: Color, dashed: Boolean, width: Float) {
    drawLine(
        color = color,
        start = Offset(0f, size.height / 2),
        end = Offset(size.width, size.height / 2),
        strokeWidth = width * density,
        cap = StrokeCap.Round,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(3 * density, 2.5f * density)) else null,
    )
}

/**
 * The way to an outing (PLANNING.md §11 Phase 10): one quiet button under the day, to the page
 * where they are kept and started. Not while one is under way: its card stands here instead.
 */
@Composable
private fun StartOuting(onOpenSessions: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
        FilledTonalButton(onClick = onOpenSessions, modifier = Modifier.testTag(TodayTags.START_OUTING)) {
            Icon(PassoIcons.Outing, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(stringResource(R.string.today_start_outing))
        }
    }
}

/** Today's walks and finished outings, found in the minutes already counted (PLANNING.md §6.1). */
@Composable
private fun WalksCard(outings: List<Outing>, format: MeasureFormatter) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(TodayTags.WALKS),
    ) {
        Column {
            Text(
                text = stringResource(R.string.today_walks_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp).semantics { heading() },
            )
            OutingList(outings, format)
        }
    }
}

/** The metrics, two to a row; the cadence across the page, with its scale. */
@Composable
private fun Metrics(state: TodayUiState, format: MeasureFormatter) {
    val metrics = state.overview.metrics
    val unit = SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = ScreenMargin).testTag(TodayTags.METRICS),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val distance = format.distance(metrics.distanceMeters)
            val stepLength = format.stepLength(state.walkingStepLength).text()
            Tile(
                icon = PassoIcons.Distance,
                label = stringResource(R.string.today_metric_distance),
                value = distance.annotated(unit),
                spokenValue = distance.text(),
                meaning = stringResource(R.string.today_metric_distance_meaning, stepLength),
                modifier = Modifier.weight(1f),
            )
            val energy = format.energy(metrics.activeKcal)
            Tile(
                icon = PassoIcons.Flame,
                label = stringResource(R.string.today_metric_calories),
                value = energy.annotated(unit),
                spokenValue = energy.text(),
                meaning = stringResource(R.string.today_metric_calories_meaning),
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val active = format.minutes(metrics.activeMinutes)
            Tile(
                icon = PassoIcons.Clock,
                label = stringResource(R.string.today_metric_active),
                value = active.annotated(unit),
                spokenValue = active.text(),
                meaning = stringResource(R.string.today_metric_active_meaning),
                modifier = Modifier.weight(1f),
            )
            val brisk = format.minutes(metrics.briskMinutes)
            val left = state.overview.briskShareLeft
            val done = left == 0
            val shareColor = if (done) PassoTheme.colors.goal else MaterialTheme.colorScheme.primary
            Tile(
                icon = PassoIcons.Bolt,
                label = stringResource(R.string.today_metric_brisk),
                value = brisk.annotated(unit),
                spokenValue = brisk.text(),
                meaning = if (done) {
                    stringResource(R.string.today_metric_brisk_done)
                } else {
                    pluralStringResource(R.plurals.today_metric_brisk_left, left, left)
                },
                modifier = Modifier.weight(1f),
                track = {
                    MetricTrack(
                        value = metrics.briskMinutes.toFloat(),
                        range = 0f..MetricsConstants.DAILY_BRISK_SHARE_MINUTES.toFloat(),
                        colors = listOf(shareColor),
                    )
                },
            )
        }
        val cadence = metrics.averageCadence
        val band = state.overview.cadenceBand
        if (cadence != null && band != null) {
            val measure = format.cadence(cadence)
            Tile(
                icon = PassoIcons.Pulse,
                label = stringResource(R.string.today_metric_cadence),
                value = measure.annotated(unit),
                spokenValue = measure.text(),
                meaning = stringResource(
                    when (band) {
                        CadenceBand.RELAXED -> R.string.today_metric_cadence_relaxed
                        CadenceBand.BRISK -> R.string.today_metric_cadence_brisk
                        CadenceBand.VIGOROUS -> R.string.today_metric_cadence_vigorous
                    },
                ),
                modifier = Modifier.fillMaxWidth(),
                track = {
                    MetricTrack(
                        value = cadence.toFloat(),
                        range = CADENCE_SCALE,
                        colors = PassoTheme.colors.effortRamp,
                        thresholds = listOf(
                            MetricsConstants.BRISK_MINUTE_THRESHOLD.toFloat(),
                            MetricsConstants.VIGOROUS_CADENCE.toFloat(),
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun Tile(
    icon: ImageVector,
    label: String,
    value: androidx.compose.ui.text.AnnotatedString,
    spokenValue: String,
    meaning: String,
    modifier: Modifier = Modifier,
    track: (@Composable () -> Unit)? = null,
) {
    MetricTile(
        icon = icon,
        label = label,
        value = value,
        meaning = meaning,
        spoken = stringResource(R.string.today_metric_spoken, label, spokenValue, meaning),
        modifier = modifier,
        track = track,
    )
}

/** Cadence drawn from a stroll to a run, so brisk and vigorous sit where a reader expects them. */
private val CADENCE_SCALE = 60f..160f

/** Hooks for the UI tests. */
object TodayTags {
    const val LIST = "today_list"
    const val RING = "today_ring"
    const val HEADLINE = "today_headline"
    const val STATUS = "today_status"
    const val TREND = "today_trend"
    const val CHART = "today_chart"
    const val METRICS = "today_metrics"
    const val WALKS = "today_walks"
    const val START_OUTING = "today_start_outing"
}
