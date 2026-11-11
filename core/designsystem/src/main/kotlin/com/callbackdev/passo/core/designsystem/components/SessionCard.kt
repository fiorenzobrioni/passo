package com.callbackdev.passo.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.designsystem.format.annotated
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.sessionAmount
import com.callbackdev.passo.core.designsystem.format.sessionCadence
import com.callbackdev.passo.core.designsystem.format.sessionEstimates
import com.callbackdev.passo.core.designsystem.format.sessionHeadline
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.sessionPaceWord
import com.callbackdev.passo.core.designsystem.format.sessionPlaceAhead
import com.callbackdev.passo.core.designsystem.format.sessionProgress
import com.callbackdev.passo.core.designsystem.format.sessionStanding
import com.callbackdev.passo.core.designsystem.format.sessionSteps
import com.callbackdev.passo.core.designsystem.format.sessionZone
import com.callbackdev.passo.core.designsystem.format.unbrokenText
import com.callbackdev.passo.core.designsystem.format.unitOf
import com.callbackdev.passo.core.designsystem.format.walkNextIn
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.IntervalSchedule
import com.callbackdev.passo.core.domain.sessions.cadence
import com.callbackdev.passo.core.domain.sessions.cadenceFloor
import com.callbackdev.passo.core.domain.sessions.fraction
import com.callbackdev.passo.core.domain.sessions.goal
import com.callbackdev.passo.core.domain.sessions.intervalAt
import com.callbackdev.passo.core.domain.sessions.progress
import com.callbackdev.passo.core.domain.ways.WalkPlaces
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId

/** What an outing's card can ask for. */
class SessionCardActions(
    val onPause: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onStop: () -> Unit = {},
    val onKeepGoing: () -> Unit = {},
    val onClose: () -> Unit = {},
)

/**
 * An outing on Today (PLANNING.md §11 Phase 10): under way, paused, or just over. In order:
 * what it is and how it stands, one sentence as its headline, the shape of it (a bar with the
 * reader's milestones marked, how far it is and where it ends at its two ends), then the numbers
 * each over what it is, then the buttons.
 *
 * - Under way: the pace now against the outing's own, the steps, the estimates; Stop and Pause.
 * - Paused: the numbers standing still; Stop and Resume.
 * - Over: when it was and for how long, what it came to, the time at its pace; Close, and "Keep
 *   going" for a while after a goal ([canKeepGoing]), "Resume" after an end by a long stillness.
 *
 * On a city walk (Phase 11) the bar is the walk's small map, the reader's point on it: drawn
 * only while the card is on screen, like every chart here.
 *
 * @param cadence the last half minute's pace, for an outing under way.
 * @param showMap false where the walk's own map is already on the page.
 */
@Composable
fun SessionCard(
    session: Session,
    cadence: Int?,
    canKeepGoing: Boolean,
    format: MeasureFormatter,
    actions: SessionCardActions,
    modifier: Modifier = Modifier,
    showMap: Boolean = true,
) {
    val res = LocalResources.current
    val reached = session.reached
    val finished = session.state == SessionState.FINISHED
    val accent = if (reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.primary
    val container = if (reached) PassoTheme.colors.goalContainer else MaterialTheme.colorScheme.primaryContainer
    val mark = if (reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.onPrimaryContainer
    val name = res.sessionName(session)
    // An interval outing under way counts down its interval while the card is on screen: a
    // ticker of the screen's own, which stops with it (PLANNING.md §9.4).
    val ticking = session.intervals != null && session.state == SessionState.ACTIVE
    val now by produceState(System.currentTimeMillis(), ticking, session) {
        value = System.currentTimeMillis()
        while (ticking) {
            delay(COUNTDOWN_TICK_MILLIS - System.currentTimeMillis() % COUNTDOWN_TICK_MILLIS)
            value = System.currentTimeMillis()
        }
    }
    val sentence = res.sessionHeadline(session, format, now)
    val interval = session.intervalAt(now)
    val state = when (session.state) {
        SessionState.ACTIVE -> stringResource(R.string.session_card_under_way)
        SessionState.PAUSED -> stringResource(R.string.session_card_paused)
        SessionState.FINISHED -> finishedWhen(session)
    }
    // A slow interval has no pace to keep: its cadence is said in words.
    val pace = if (interval?.fast == false) SessionIntensity.FREE else session.intensity
    // Under way, how the pace stands is said beside the state, in a word: the number is below.
    val paceWord = if (session.state == SessionState.ACTIVE) res.sessionPaceWord(cadence, pace) else null
    val stateLabel = paceWord?.let { stringResource(R.string.session_card_state_pace, state, it) } ?: state
    // An outing that starts or pauses is said once by TalkBack, wherever its card appears: a pane
    // title is announced when it comes and when it changes. Not a finished one, which would be
    // said again each time Today opens; and not the pace's word, which changes as it is walked.
    val pane = if (finished) null else stringResource(R.string.session_card_pane, name, state)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier
            .fillMaxWidth()
            .then(if (pane != null) Modifier.semantics { paneTitle = pane } else Modifier)
            .testTag(SessionCardTags.CARD),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // The same mark, at the same size, as an outing's card on the Outings page: the
                // names under one another start on one line.
                Surface(color = container, shape = CircleShape, modifier = Modifier.size(SessionMarkSize)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = sessionIcon(session.intensity, session.goalKind),
                            contentDescription = null,
                            tint = mark,
                            modifier = Modifier.size(SessionMarkIconSize),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics {
                            heading()
                        },
                    )
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = accent,
                    )
                }
            }
            // The sentence is the card's headline: on a city walk, the place walked towards, with
            // how far it is under it.
            val ahead = sessionPlaceAhead(session, now)
            if (ahead != null) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.testTag(SessionCardTags.SENTENCE).semantics(mergeDescendants = true) { },
                ) {
                    PlaceText(
                        stringResource(placeNameRes(ahead.stop.key)),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        res.walkNextIn(ahead, format),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    sentence,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag(SessionCardTags.SENTENCE),
                )
            }
            val progressText = res.sessionProgress(session, format)
            val described = Modifier.clearAndSetSemantics {
                contentDescription = res.getString(R.string.session_card_progress_description, progressText, sentence)
                progressBarRangeInfo = ProgressBarRangeInfo(session.progress().toFloat().coerceIn(0f, 1f), 0f..1f)
            }
            val walk = session.walk?.let(Ways::of)
            val along = WalkPlaces.along(session)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (walk != null && along != null && showMap) {
                    WayMapView(
                        way = walk,
                        walkedMeters = along,
                        reached = walk.stops.count { it.distanceMeters <= along },
                        contentDescription = "",
                        detailed = false,
                        // Wider than the walk's own frame: a card on Today, not a page.
                        ratio = CARD_MAP_RATIO,
                        modifier = Modifier.fillMaxWidth().testTag(SessionCardTags.MAP).then(described),
                    )
                } else if (session.intervals != null) {
                    IntervalTrack(session = session, color = accent, nowMillis = now, modifier = described)
                } else {
                    SessionTrack(session = session, color = accent, modifier = described)
                }
                // Where it stands and where it ends, at the two ends of its bar: said by the bar
                // itself to TalkBack, so here they are for the eye only.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                ) {
                    val ends = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")
                    Text(
                        text = res.sessionStanding(session, format),
                        style = ends,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = res.sessionAmount(session.goal(), format),
                        style = ends,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
            if (finished && session.intervals != null) IntervalBars(session, accent)
            SessionStats(session, cadence, pace, format)
            if (session.state != SessionState.ACTIVE) {
                res.sessionZone(session, format)?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            when {
                finished -> Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = actions.onClose, modifier = Modifier.testTag(SessionCardTags.CLOSE)) {
                        Text(stringResource(R.string.session_card_close))
                    }
                    if (canKeepGoing) {
                        FilledTonalButton(
                            onClick = actions.onKeepGoing,
                            modifier = Modifier.testTag(SessionCardTags.KEEP_GOING),
                        ) {
                            ButtonIcon(PassoIcons.Play)
                            Text(
                                stringResource(
                                    if (session.end == SessionEnd.IDLE) {
                                        R.string.session_card_resume
                                    } else {
                                        R.string.session_card_keep_going
                                    },
                                ),
                            )
                        }
                    }
                }

                // Under way, the two buttons share the card's width and stand a little taller:
                // they are touched while walking.
                else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val live = Modifier.weight(1f).heightIn(min = LIVE_BUTTON_HEIGHT)
                    OutlinedButton(
                        onClick = actions.onStop,
                        contentPadding = LIVE_BUTTON_PADDING,
                        modifier = live.testTag(SessionCardTags.STOP),
                    ) {
                        ButtonIcon(PassoIcons.Stop)
                        SegmentLabel(stringResource(R.string.session_card_stop))
                    }
                    if (session.state == SessionState.PAUSED) {
                        FilledTonalButton(
                            onClick = actions.onResume,
                            contentPadding = LIVE_BUTTON_PADDING,
                            modifier = live.testTag(SessionCardTags.RESUME),
                        ) {
                            ButtonIcon(PassoIcons.Play)
                            SegmentLabel(stringResource(R.string.session_card_resume))
                        }
                    } else {
                        FilledTonalButton(
                            onClick = actions.onPause,
                            contentPadding = LIVE_BUTTON_PADDING,
                            modifier = live.testTag(SessionCardTags.PAUSE),
                        ) {
                            ButtonIcon(PassoIcons.Pause)
                            SegmentLabel(stringResource(R.string.session_card_pause))
                        }
                    }
                }
            }
        }
    }
}

/** The mark of an outing's card, here and on the Outings page: one size, so their names line up. */
val SessionMarkSize = 40.dp
val SessionMarkIconSize = 22.dp

private val LIVE_BUTTON_HEIGHT = 48.dp
private val LIVE_BUTTON_PADDING = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/** A third of the card narrower than this, at the reader's text size, and the numbers go back to lines. */
private val STAT_MIN_WIDTH = 80.dp

/**
 * An outing's numbers, each over what it is: under way the pace now, the steps and the distance
 * (with the calories beside the word that says both are estimates); paused or over, the steps
 * and the two estimates. Three columns between two hairlines, the figures tabular, so a number
 * that changes does not move its neighbours.
 *
 * One node for TalkBack, read as the two sentences the card always said. Where a third of the
 * card is too narrow for a number at the reader's text size, those sentences are what is shown.
 */
@Composable
private fun SessionStats(session: Session, cadence: Int?, pace: SessionIntensity, format: MeasureFormatter) {
    val res = LocalResources.current
    val active = session.state == SessionState.ACTIVE
    val lines = listOf(
        if (active) {
            listOf(res.sessionCadence(cadence, pace, format), res.sessionSteps(session, format)).joinToString(" · ")
        } else {
            res.sessionSteps(session, format)
        },
        res.sessionEstimates(session, format),
    )
    val spoken = lines.joinToString(". ")
    val unit = SpanStyle(fontSize = MaterialTheme.typography.bodyMedium.fontSize)
    val steps = Stat(
        AnnotatedString(format.steps(session.totals.steps)),
        pluralStringResource(R.plurals.session_stat_steps, session.totals.steps),
    )
    val distance = format.distance(session.totals.distanceMeters).annotated(unit)
    val energy = format.energy(session.totals.activeKcal)
    val estimate = stringResource(R.string.session_stat_estimate)
    val stats = if (active) {
        val now = format.cadence(cadence ?: 0)
        listOf(
            Stat(AnnotatedString(if (cadence == null) NO_VALUE else now.number), res.unitOf(now)),
            steps,
            // A little wider than the other two: its label carries the calories as well.
            Stat(distance, stringResource(R.string.session_stat_estimate_with, energy.unbrokenText()), WIDE_STAT),
        )
    } else {
        listOf(steps, Stat(distance, estimate), Stat(energy.annotated(unit), estimate))
    }
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SessionCardTags.STATS)
            .clearAndSetSemantics { contentDescription = spoken },
    ) {
        if (maxWidth / stats.size / fontScale < STAT_MIN_WIDTH) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lines.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val hairline = MaterialTheme.colorScheme.outlineVariant.copy(alpha = HAIRLINE_ALPHA)
            Column {
                HorizontalDivider(color = hairline)
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    stats.forEachIndexed { index, stat ->
                        if (index > 0) VerticalDivider(color = hairline)
                        Column(
                            modifier = Modifier
                                .weight(stat.weight)
                                .padding(
                                    start = if (index > 0) 12.dp else 0.dp,
                                    end = if (index < stats.lastIndex) 8.dp else 0.dp,
                                    top = 8.dp,
                                    bottom = 8.dp,
                                ),
                        ) {
                            Text(
                                stat.value,
                                style = PassoTheme.type.readingValue.copy(
                                    fontSize = 20.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.Normal,
                                ),
                            )
                            Text(
                                stat.label,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                HorizontalDivider(color = hairline)
            }
        }
    }
}

/** A number of an outing and the word under it, in a column [weight] wide against the others. */
private class Stat(val value: AnnotatedString, val label: String, val weight: Float = 1f)

private const val WIDE_STAT = 1.3f

/** A pace not measured yet: the first seconds of an outing. */
private const val NO_VALUE = "–"
private const val HAIRLINE_ALPHA = 0.6f

@Composable
private fun ButtonIcon(icon: ImageVector) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    androidx.compose.foundation.layout.Spacer(Modifier.size(ButtonDefaults.IconSpacing))
}

/** «18:02–18:24 · 22 min». */
@Composable
private fun finishedWhen(session: Session): String {
    val end = session.endedAtMillis ?: session.lastStepAtMillis
    val range =
        stringResource(
            R.string.walk_time_range,
            clockTime(minuteOfDay(session.startedAtMillis)),
            clockTime(minuteOfDay(end)),
        )
    val minutes = ((end - session.startedAtMillis) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
    return stringResource(R.string.session_card_when, range, duration(minutes))
}

internal fun minuteOfDay(millis: Long): Int {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalTime()
    return time.hour * 60 + time.minute
}

/**
 * The mark of an outing: an interval walk is its slow and fast line, a run is a bolt, as in the
 * walk list; any other walk is the way to a flag. On the Outings page the interval walk no longer
 * wore the same mark as the brisk walk above it (owner's review, 2 Nov 2026).
 */
fun sessionIcon(intensity: SessionIntensity, goalKind: SessionGoalKind? = null): ImageVector = when {
    goalKind == SessionGoalKind.INTERVALS -> PassoIcons.Intervals
    intensity == SessionIntensity.RUN -> PassoIcons.Bolt
    else -> PassoIcons.Outing
}

/**
 * The way to the goal as a bar: the part walked in the accent, the reader's milestones as gaps
 * cut into it, so "halfway" is a place on it before it is a vibration. Grows to its value, or
 * jumps there under reduced motion.
 */
@Composable
fun SessionTrack(session: Session, color: Color, modifier: Modifier = Modifier) {
    val target = session.progress().toFloat().coerceIn(0f, 1f)
    val reduced = reducedMotion()
    val shown by animateFloatAsState(target, animationSpec = tween(if (reduced) 0 else 500), label = "track")
    val ground = MaterialTheme.colorScheme.surfaceContainerLow
    val rest = MaterialTheme.colorScheme.surfaceContainerHighest
    val milestones = session.milestones.map { it.fraction.toFloat() }
    Canvas(modifier = modifier.fillMaxWidth().height(10.dp)) {
        val height = size.height
        val radius = CornerRadius(height / 2)
        drawRoundRect(rest, Offset.Zero, Size(size.width, height), radius)
        drawRoundRect(color, Offset.Zero, Size(size.width * shown, height), radius)
        val gap = 3.dp.toPx()
        for (at in milestones) {
            drawRect(ground, Offset(size.width * at - gap / 2, 0f), Size(gap, height))
        }
    }
}

/**
 * An interval outing's way as a bar (Phase 13): each interval a block, the fast ones full height
 * and the slow ones thinner, cut apart at every change; the part walked in the accent. The shape
 * says which comes next before any word does.
 */
@Composable
fun IntervalTrack(session: Session, color: Color, nowMillis: Long, modifier: Modifier = Modifier) {
    val schedule = IntervalSchedule.of(session) ?: return
    val moving = session.intervalAt(nowMillis)?.let { schedule.startOf(it.index) + it.elapsedMillis }
        ?: session.totals.movingMillis
    val target = (moving.toFloat() / schedule.totalMillis).coerceIn(0f, 1f)
    val reduced = reducedMotion()
    val shown by animateFloatAsState(target, animationSpec = tween(if (reduced) 0 else 500), label = "intervals")
    IntervalBlocks(schedule.sets, shown, color, modifier.testTag(SessionCardTags.INTERVALS))
}

/**
 * The shape of an interval outing: its intervals as blocks, fast full height and slow thinner,
 * [progress] of it (a share of the whole) in [color]. A [plan] not yet walked is drawn whole, the
 * fast blocks in [color] and the slow ones in a lighter tint of it.
 */
@Composable
fun IntervalBlocks(
    sets: IntervalSets,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    plan: Boolean = false,
) {
    val schedule = IntervalSchedule(sets)
    val rest = MaterialTheme.colorScheme.surfaceContainerHighest
    Canvas(modifier = modifier.fillMaxWidth().height(14.dp)) {
        val gap = 3.dp.toPx()
        val total = schedule.totalMillis.toFloat()
        val walkedTo = size.width * progress
        for (index in 0 until schedule.count) {
            val start = size.width * schedule.startOf(index) / total + if (index > 0) gap / 2 else 0f
            val end = size.width * schedule.endOf(index) / total - if (index < schedule.count - 1) gap / 2 else 0f
            if (end <= start) continue
            val height = if (schedule.isFast(index)) size.height else size.height * SLOW_HEIGHT
            val top = (size.height - height) / 2
            val radius = CornerRadius(height / 2)
            val ground = when {
                !plan -> rest
                schedule.isFast(index) -> color
                else -> color.copy(alpha = PLAN_SLOW_ALPHA)
            }
            drawRoundRect(ground, Offset(start, top), Size(end - start, height), radius)
            val walked = minOf(end, walkedTo) - start
            if (walked > 0) drawRoundRect(color, Offset(start, top), Size(walked, height), radius)
        }
    }
}

/**
 * How each fast interval went (Phase 13): one bar a fast interval, its height its cadence, the
 * line across them the pace it was to be walked at. A bar at the pace or above is in the accent;
 * below it, in a quieter ink, counted and shown, never hidden.
 */
@Composable
fun IntervalBars(session: Session, color: Color, modifier: Modifier = Modifier) {
    val res = LocalResources.current
    val floor = session.intensity.cadenceFloor ?: return
    val cadences = session.splits.filter { it.fast }.mapNotNull { it.cadence() }
    if (cadences.isEmpty()) return
    val below = MaterialTheme.colorScheme.outline
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    val card = MaterialTheme.colorScheme.surfaceContainerLow
    val labels = MaterialTheme.typography.labelSmall.copy(fontFeatureSettings = "tnum")
    val measurer = rememberTextMeasurer()
    val description = res.getString(
        R.string.session_intervals_chart_description,
        cadences.joinToString(", "),
        floor,
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier.testTag(SessionCardTags.INTERVAL_BARS).clearAndSetSemantics {
            contentDescription = description
        },
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            val low = (minOf(cadences.min(), floor) - CHART_MARGIN).coerceAtLeast(0)
            val high = maxOf(cadences.max(), floor) + CHART_MARGIN
            val labelHeight = 14.dp.toPx()
            val chart = size.height - labelHeight
            fun y(cadence: Int) = labelHeight + chart * (1f - (cadence - low).toFloat() / (high - low))
            val slot = size.width / cadences.size
            val bar = minOf(slot * 0.5f, 28.dp.toPx())
            // The pace first, under the bars and their numbers: a number is never crossed out.
            val at = y(floor)
            drawLine(
                line,
                Offset(0f, at),
                Offset(size.width, at),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())),
            )
            cadences.forEachIndexed { index, cadence ->
                val left = slot * index + (slot - bar) / 2
                val top = y(cadence)
                drawRoundRect(
                    color = if (cadence >= floor) color else below,
                    topLeft = Offset(left, top),
                    size = Size(bar, size.height - top),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                val text = measurer.measure(cadence.toString(), labels.copy(color = ink))
                val corner = Offset(left + (bar - text.size.width) / 2, top - text.size.height)
                // On the card's own ground, so the pace's line never runs through a number.
                drawRect(card, corner, Size(text.size.width.toFloat(), text.size.height.toFloat()))
                drawText(text, topLeft = corner)
            }
        }
        Text(
            text = res.getString(R.string.session_intervals_chart_legend, floor),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val CARD_MAP_RATIO = 1.6f
private const val COUNTDOWN_TICK_MILLIS = 1_000L

/** A plan's slow blocks: the accent, lighter, so fast and slow read apart before any walking. */
private const val PLAN_SLOW_ALPHA = 0.35f

/** A slow interval's block, against a fast one's full height. */
private const val SLOW_HEIGHT = 0.5f

/** Steps a minute of room above and below the bars, so the lowest still shows. */
private const val CHART_MARGIN = 12

/** Hooks for the UI tests. */
object SessionCardTags {
    const val CARD = "session_card"
    const val SENTENCE = "session_card_sentence"
    const val PAUSE = "session_card_pause"
    const val RESUME = "session_card_resume"
    const val STOP = "session_card_stop"
    const val KEEP_GOING = "session_card_keep_going"
    const val CLOSE = "session_card_close"
    const val MAP = "session_card_map"
    const val INTERVALS = "session_card_intervals"
    const val INTERVAL_BARS = "session_card_interval_bars"
    const val STATS = "session_card_stats"
}
