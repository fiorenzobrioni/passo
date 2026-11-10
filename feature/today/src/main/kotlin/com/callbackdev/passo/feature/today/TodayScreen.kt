package com.callbackdev.passo.feature.today

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
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
import com.callbackdev.passo.core.designsystem.components.MetricTrack
import com.callbackdev.passo.core.designsystem.components.OutingList
import com.callbackdev.passo.core.designsystem.components.ProgressRing
import com.callbackdev.passo.core.designsystem.components.SessionCard
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.components.TrendPoint
import com.callbackdev.passo.core.designsystem.components.reveal
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.axisHour
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.longDate
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PageGutter
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.designsystem.theme.padding
import com.callbackdev.passo.core.designsystem.theme.pageGutter
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.insights.StreakSoFar
import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.sessions.Outing
import com.callbackdev.passo.core.domain.today.CadenceBand
import com.callbackdev.passo.core.domain.today.Headline
import com.callbackdev.passo.core.domain.today.Pace
import com.callbackdev.passo.core.model.WayId
import java.time.LocalDate

/** Today, with its state from [TodayViewModel] and the permission request it may need. */
@Composable
fun TodayRoute(
    onOpenSettings: () -> Unit,
    onOpenSessions: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onOpenWay: (WayId, Long) -> Unit = { _, _ -> },
    onOpenYear: (Int) -> Unit = {},
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
        // Opened from the first-day note, its only door here: the note goes once it is read.
        onOpenGuide = {
            viewModel.firstDayNoteRead()
            onOpenGuide()
        },
        onOpenWay = onOpenWay,
        onOpenYear = onOpenYear,
        onCloseYear = viewModel::closeYearCard,
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
 * Today (PLANNING.md §11 Phase 3), in order: the state of counting if it needs the
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
    onOpenGuide: () -> Unit = {},
    onOpenWay: (WayId, Long) -> Unit = { _, _ -> },
    onOpenYear: (Int) -> Unit = {},
    onCloseYear: (Int) -> Unit = {},
    sessionActions: (Long) -> SessionCardActions = { SessionCardActions() },
) {
    val format = rememberMeasureFormatter(state.units)
    val bottom = bottomPadding
    // Each item takes the gutter itself rather than the list, so the list scrolls from either
    // edge of an open foldable.
    val gutter = pageGutter()
    val inColumn = Modifier.padding(gutter)
    val list = rememberLazyListState()
    val reduced = reducedMotion()
    // An outing that begins while the page is open (a launcher's shortcut brings it forward) is
    // brought into view: its card stands under the sentence, and the page may be scrolled down
    // to the tiles. Not one already under way when the page opens: it is where it always is.
    val sessionId = state.session?.session?.id
    var seenSession by rememberSaveable { mutableStateOf(sessionId) }
    LaunchedEffect(sessionId) {
        if (sessionId != null && sessionId != seenSession) list.reveal(key = SESSION_KEY, index = 0, reduced = reduced)
        seenSession = sessionId
    }
    // Its own ground and ink, so the page reads right wherever it is drawn.
    Surface(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = list,
            modifier = Modifier.fillMaxSize().testTag(TodayTags.LIST),
            contentPadding = PaddingValues(bottom = bottom + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "hero") {
                Hero(state, format, gutter, askInSettings, onOpenSettings, onAllow, onResume, onCelebrated)
            }
            val session = state.session
            if (session != null) {
                item(key = SESSION_KEY) {
                    SessionCard(
                        session = session.session,
                        cadence = session.cadence,
                        canKeepGoing = session.canKeepGoing,
                        format = format,
                        actions = sessionActions(session.session.id),
                        modifier = inColumn.padding(horizontal = ScreenMargin),
                    )
                }
            }
            if (state.firstDay && state.status == TrackingStatus.COUNTING) {
                item(key = "first-day") {
                    StatusCard(
                        icon = PassoIcons.Info,
                        title = stringResource(R.string.today_first_day_title),
                        body = stringResource(R.string.today_first_day_body),
                        tone = StatusTone.NOTE,
                        // The first day is when the questions come: how it counts, what the
                        // ring's notch is. The guide stays in Settings after it.
                        action = stringResource(R.string.today_first_day_guide),
                        onAction = onOpenGuide,
                        modifier = inColumn.padding(horizontal = ScreenMargin),
                    )
                }
            }
            state.yearCard?.let { year ->
                item(key = "year") {
                    // In season, once (owner, 1 Oct 2026): the year ending, told in pages.
                    StatusCard(
                        icon = PassoIcons.Calendar,
                        title = stringResource(R.string.today_year_title, year),
                        body = stringResource(R.string.today_year_body),
                        tone = StatusTone.NOTE,
                        action = stringResource(R.string.today_year_open),
                        onAction = { onOpenYear(year) },
                        dismiss = stringResource(R.string.today_year_close),
                        onDismiss = { onCloseYear(year) },
                        modifier = inColumn.padding(horizontal = ScreenMargin).testTag(TodayTags.YEAR),
                    )
                }
            }
            item(key = "trend") { Box(inColumn) { TrendCard(state, format) } }
            // Under the day rather than above it: the sentence's evidence comes first, so the
            // chart is whole on the first screen of a tall phone, and the way out after it.
            if (session == null && state.status == TrackingStatus.COUNTING && state.startOutingButton) {
                item(key = "start-outing") { StartOuting(onOpenSessions, inColumn) }
            }
            if (state.outings.isNotEmpty()) item(key = "walks") { Box(inColumn) { WalksCard(state.outings, format) } }
            item(key = "metrics") { Box(inColumn) { Intensity(state, format) } }
            // Last: a way lasts weeks, and today's share of it is news only after the day's own.
            state.way?.let { way -> item(key = "way") { Box(inColumn) { WayCard(way, format, onOpenWay) } } }
        }
    }
}

/**
 * The top of the page: the date, the streak and the gear, the state of counting when it needs the
 * reader, the ring, the sentence and the day's three amounts. On the page's own ground: the ring is
 * the one thing in the day's color, primary while the goal is ahead and the goal's green once it
 * is met (the Today review of 10 Nov 2026, which took away the glow that tinted the whole top).
 */
@Composable
private fun Hero(
    state: TodayUiState,
    format: MeasureFormatter,
    gutter: PageGutter,
    askInSettings: Boolean,
    onOpenSettings: () -> Unit,
    onAllow: () -> Unit,
    onResume: () -> Unit,
    onCelebrated: (LocalDate) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(gutter),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 4.dp),
        ) {
            // The streak goes under the date when the two do not fit one line (large text).
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = longDate(state.date),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp).semantics { heading() },
                )
                if (state.streakDays >= StreakSoFar.SHOWN_FROM_DAYS) StreakChip(state.streakDays)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(PassoIcons.Settings, contentDescription = stringResource(R.string.today_settings))
            }
        }
        StatusBanner(state.status, askInSettings, onAllow, onResume)
        Ring(state, format, onCelebrated)
        Headline(state, format)
        Spacer(Modifier.height(16.dp))
        Stats(state, format)
    }
}

/**
 * The days in a row at the goal, beside the date: Insights' streak, said where it is earned. A
 * label, not a button: the streak's own page is Insights, one tab away.
 */
@Composable
private fun StreakChip(days: Int) {
    val spoken = pluralStringResource(R.plurals.today_streak_spoken, days, days)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = CircleShape,
        modifier = Modifier.testTag(TodayTags.STREAK).clearAndSetSemantics { contentDescription = spoken },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.heightIn(min = 32.dp).padding(start = 10.dp, end = 12.dp),
        ) {
            Icon(
                PassoIcons.Streak,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = pluralStringResource(R.plurals.today_streak, days, days),
                style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
        // A larger text size takes back the room the count and the goal need inside the ring.
        val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
        val diameter = minOf(maxWidth - 64.dp, RING_MAX_DIAMETER * fontScale)
        ProgressRing(
            progress = overview.progress.toFloat(),
            usualProgress = overview.usualProgress?.toFloat(),
            reached = reached,
            celebrate = state.celebrate,
            onCelebrated = { onCelebrated(state.date) },
            strokeWidth = RING_STROKE,
            modifier = Modifier
                .size(diameter)
                .clearAndSetSemantics { contentDescription = description }
                .testTag(TodayTags.RING),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CountUp(overview.steps, format)
                // A met goal keeps its check here: the color is never the only sign of it, and
                // the sentence under the ring says it in words.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (reached) {
                        Icon(
                            PassoIcons.Check,
                            contentDescription = null,
                            tint = PassoTheme.colors.goal,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.today_of_goal, format.steps(overview.goalSteps)),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * The ring's size and weight: a line drawn round the count rather than a band, so the number is
 * the heaviest thing on the screen (the Today review of 10 Nov 2026). Inside it only the count
 * and the goal: the distance and the calories have their own row under the sentence.
 */
private val RING_MAX_DIAMETER = 256.dp
private val RING_STROKE = 12.dp

/**
 * The day's three answers to "how much" (distance, active calories, active minutes), under the
 * sentence, so the first screen answers them without a scroll; one line under the row says what
 * the estimates rest on. Each is one node for a screen reader, read with its own meaning, so the
 * shared line is not read again. Three across while each third holds the reader's text size,
 * else one above the other.
 */
@Composable
private fun Stats(state: TodayUiState, format: MeasureFormatter) {
    val metrics = state.overview.metrics
    val unit = SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    val distance = format.distance(metrics.distanceMeters)
    val energy = format.energy(metrics.activeKcal)
    val active = format.minutes(metrics.activeMinutes)
    val stepLength = format.stepLength(state.walkingStepLength).text()
    val stats = listOf(
        StatItem(
            value = distance.annotated(unit),
            label = stringResource(R.string.today_stat_distance),
            spoken = stringResource(
                R.string.today_metric_spoken,
                stringResource(R.string.today_metric_distance),
                distance.text(),
                stringResource(R.string.today_metric_distance_meaning, stepLength),
            ),
        ),
        StatItem(
            value = energy.annotated(unit),
            label = stringResource(R.string.today_stat_calories),
            spoken = stringResource(
                R.string.today_metric_spoken,
                stringResource(R.string.today_metric_calories),
                energy.text(),
                stringResource(R.string.today_metric_calories_meaning),
            ),
        ),
        StatItem(
            value = active.annotated(unit),
            label = stringResource(R.string.today_stat_active),
            spoken = stringResource(
                R.string.today_metric_spoken,
                stringResource(R.string.today_metric_active),
                active.text(),
                stringResource(R.string.today_metric_active_meaning),
            ),
        ),
    )
    val rule = MaterialTheme.colorScheme.outlineVariant
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(TodayTags.STATS),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val across = maxWidth / stats.size / LocalDensity.current.fontScale >= STAT_MIN_WIDTH
            Column {
                HorizontalDivider(color = rule)
                if (across) {
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        stats.forEachIndexed { i, stat ->
                            if (i > 0) VerticalDivider(color = rule)
                            Stat(stat, Modifier.weight(1f).padding(start = if (i == 0) 8.dp else 16.dp, end = 4.dp))
                        }
                    }
                } else {
                    stats.forEachIndexed { i, stat ->
                        if (i > 0) HorizontalDivider(color = rule)
                        Stat(stat, Modifier.fillMaxWidth().padding(horizontal = 8.dp))
                    }
                }
                HorizontalDivider(color = rule)
            }
        }
        Text(
            text = stringResource(R.string.today_stats_note, stepLength),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp).clearAndSetSemantics { },
        )
    }
}

private class StatItem(val value: AnnotatedString, val label: String, val spoken: String)

@Composable
private fun Stat(stat: StatItem, modifier: Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = modifier
            .padding(vertical = 10.dp)
            .clearAndSetSemantics { contentDescription = stat.spoken },
    ) {
        Text(
            stat.value,
            style = PassoTheme.type.readingValue,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
        Text(stat.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A third narrower than this, at the reader's text size, stacks the three amounts. */
private val STAT_MIN_WIDTH = 96.dp

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
    // Green is the met goal's alone: ahead of a usual day is good news, but not that one.
    val tint = if (headline is Headline.GoalReached) PassoTheme.colors.goal else MaterialTheme.colorScheme.primary
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
            // Balanced lines: two of a like length, never a lone word under a full line.
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Medium,
                lineBreak = LineBreak.Heading,
            ),
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
                style = MaterialTheme.typography.bodyLarge.copy(lineBreak = LineBreak.Heading),
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
            // The key goes under the title when the two do not fit one line (large text).
            FlowRow(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.today_trend_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(end = 8.dp).semantics { heading() },
                )
                Legend(hasUsual = trend.usual != null)
            }
            // The axis starts at five or earlier and ends at midnight: the quarters of the day
            // inside it, and no label on the right edge, where midnight would repeat the left's.
            val hours = listOf(6, 12, 18).map { it * 60 to axisHour(it) }
            DayTrendChart(
                trend = trend,
                goalLabel = stringResource(R.string.today_trend_goal, format.steps(overview.goalSteps)),
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

/**
 * The line above the plot: now, or the minute under the finger. Now, it leaves out today's count,
 * which the ring just above says in large, and keeps a usual day's, which nothing else on the
 * screen says; without a usual day the count stays, so the line is not a lone "Now".
 */
@Composable
private fun Readout(point: TrendPoint?, stepsNow: Int, usualNow: Int?, format: MeasureFormatter) {
    val time = point?.let { clockTime(it.minuteOfDay) } ?: stringResource(R.string.today_trend_now)
    val steps = when {
        point != null -> point.steps
        usualNow == null -> stepsNow
        else -> null
    }
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

/** Today's line and a usual day's; the goal is named on its own line, in the chart. */
@Composable
private fun Legend(hasUsual: Boolean) {
    val today = MaterialTheme.colorScheme.primary
    val usual = MaterialTheme.colorScheme.onSurfaceVariant
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(stringResource(R.string.today_trend_legend_today)) {
            drawSwatch(today, dashed = false, width = 2.5f)
        }
        if (hasUsual) {
            LegendItem(stringResource(R.string.today_trend_legend_usual)) {
                drawSwatch(usual, dashed = true, width = 1.5f)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, swatch: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Canvas(Modifier.width(12.dp).height(8.dp)) { swatch() }
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSwatch(color: Color, dashed: Boolean, width: Float) {
    val stroke = width * density
    // The round caps reach half a stroke past each end, so the line is inset by that much and
    // every swatch, solid or dashed, is exactly the canvas wide.
    val length = size.width - stroke
    // Three whole dashes, the gaps twice their length, so the last one is never cut at the edge.
    val dash = length / 7
    drawLine(
        color = color,
        start = Offset(stroke / 2, size.height / 2),
        end = Offset(size.width - stroke / 2, size.height / 2),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
        pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(dash, dash * 2)) else null,
    )
}

/**
 * The way to an outing (PLANNING.md §11 Phase 10): one row under the day, as wide as the cards, to
 * the page where they are kept and started, with the line that says what one is (a centred pill
 * read as left over, the Today review of 10 Nov 2026). Not while one is under way: its card stands
 * here instead. The reader who never walks with a goal can take it away in Settings, where the page
 * stays one row away; an outing started from a shortcut or a reminder still shows its card.
 */
@Composable
private fun StartOuting(onOpenSessions: () -> Unit, modifier: Modifier) {
    // In the page's own primary container, so a quiet way to an outing does not outshout the day.
    Surface(
        onClick = onOpenSessions,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(TodayTags.START_OUTING),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.heightIn(min = 64.dp).padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Icon(PassoIcons.Outing, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.today_start_outing), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.today_start_outing_body), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(PassoIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp))
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

/**
 * The two amounts of effort, each on its own scale with the line that says what it means: brisk
 * minutes against today's share of the WHO's 150 a week, and the cadence from a stroll to a run.
 * One card, where they were two tiles among four (the Today review of 10 Nov 2026): the distance,
 * the calories and the active minutes are under the sentence. The cadence is left out on a day with
 * no walking to average.
 */
@Composable
private fun Intensity(state: TodayUiState, format: MeasureFormatter) {
    val metrics = state.overview.metrics
    val unit = SpanStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(TodayTags.METRICS),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = stringResource(R.string.today_intensity_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )
            val brisk = format.minutes(metrics.briskMinutes)
            val left = state.overview.briskShareLeft
            val done = left == 0
            val shareColor = if (done) PassoTheme.colors.goal else MaterialTheme.colorScheme.primary
            Effort(
                label = stringResource(R.string.today_metric_brisk),
                value = brisk.annotated(unit),
                spokenValue = brisk.text(),
                meaning = if (done) {
                    stringResource(R.string.today_metric_brisk_done)
                } else {
                    pluralStringResource(R.plurals.today_metric_brisk_left, left, left)
                },
            ) {
                MetricTrack(
                    value = metrics.briskMinutes.toFloat(),
                    range = 0f..MetricsConstants.DAILY_BRISK_SHARE_MINUTES.toFloat(),
                    colors = listOf(shareColor),
                )
            }
            val cadence = metrics.averageCadence
            val band = state.overview.cadenceBand
            if (cadence != null && band != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val measure = format.cadence(cadence)
                Effort(
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
                ) {
                    MetricTrack(
                        value = cadence.toFloat(),
                        range = CADENCE_SCALE,
                        colors = PassoTheme.colors.effortRamp,
                        thresholds = listOf(
                            MetricsConstants.BRISK_MINUTE_THRESHOLD.toFloat(),
                            MetricsConstants.VIGOROUS_CADENCE.toFloat(),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * One amount of effort: its name and value on a line (the value under the name at a large text
 * size), its scale, and the line that says what it means; one node for a screen reader.
 */
@Composable
private fun Effort(
    label: String,
    value: AnnotatedString,
    spokenValue: String,
    meaning: String,
    track: @Composable () -> Unit,
) {
    val spoken = stringResource(R.string.today_metric_spoken, label, spokenValue, meaning)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = spoken },
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(value, style = PassoTheme.type.readingValue, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(modifier = Modifier.padding(vertical = 2.dp)) { track() }
        Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Cadence drawn from a stroll to a run, so brisk and vigorous sit where a reader expects them. */
private val CADENCE_SCALE = 60f..160f

/** The outing's card in the list, brought into view when an outing begins. */
private const val SESSION_KEY = "session"

/** Hooks for the UI tests. */
object TodayTags {
    const val YEAR = "today_year"
    const val LIST = "today_list"
    const val RING = "today_ring"
    const val HEADLINE = "today_headline"
    const val STATUS = "today_status"
    const val TREND = "today_trend"
    const val CHART = "today_chart"
    const val METRICS = "today_metrics"
    const val STATS = "today_stats"
    const val STREAK = "today_streak"
    const val WALKS = "today_walks"
    const val START_OUTING = "today_start_outing"
    const val WAY = "today_way"
    const val WAY_BAR = "today_way_bar"
}
