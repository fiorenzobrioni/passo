package com.callbackdev.passo.core.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.sessionCadence
import com.callbackdev.passo.core.designsystem.format.sessionEstimates
import com.callbackdev.passo.core.designsystem.format.sessionHeadline
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.sessionProgress
import com.callbackdev.passo.core.designsystem.format.sessionSteps
import com.callbackdev.passo.core.designsystem.format.sessionZone
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.fraction
import com.callbackdev.passo.core.domain.sessions.progress
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
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
 * An outing on Today (PLANNING.md §11 Phase 10): under way, paused, or just over. Chiaro's order:
 * what it is, one sentence, the shape of it (a bar with the reader's milestones marked), then the
 * numbers each with what they mean, then the buttons.
 *
 * - Under way: the pace now against the outing's own, the steps, the estimates; Pause and Stop.
 * - Paused: the same numbers standing still; Resume and Stop.
 * - Over: when it was and for how long, what it came to, the time at its pace; Close, and "Keep
 *   going" for a while after a goal ([canKeepGoing]).
 *
 * @param cadence the last half minute's pace, for an outing under way.
 */
@Composable
fun SessionCard(
    session: Session,
    cadence: Int?,
    canKeepGoing: Boolean,
    format: MeasureFormatter,
    actions: SessionCardActions,
    modifier: Modifier = Modifier,
) {
    val res = LocalResources.current
    val reached = session.reached
    val finished = session.state == SessionState.FINISHED
    val accent = if (reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.primary
    val container = if (reached) PassoTheme.colors.goalContainer else MaterialTheme.colorScheme.primaryContainer
    val mark = if (reached) PassoTheme.colors.goal else MaterialTheme.colorScheme.onPrimaryContainer
    val name = res.sessionName(session)
    val sentence = res.sessionHeadline(session, format)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth().testTag(SessionCardTags.CARD),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(color = container, shape = CircleShape, modifier = Modifier.size(36.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = sessionIcon(session.intensity),
                            contentDescription = null,
                            tint = mark,
                            modifier = Modifier.size(20.dp),
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
                        text = when (session.state) {
                            SessionState.ACTIVE -> stringResource(R.string.session_card_under_way)
                            SessionState.PAUSED -> stringResource(R.string.session_card_paused)
                            SessionState.FINISHED -> finishedWhen(session)
                        },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                        color = accent,
                    )
                }
                Text(
                    text = res.sessionProgress(session, format),
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                sentence,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(SessionCardTags.SENTENCE),
            )
            SessionTrack(
                session = session,
                color = accent,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = res.getString(
                        R.string.session_card_progress_description,
                        res.sessionProgress(session, format),
                        sentence,
                    )
                    progressBarRangeInfo = ProgressBarRangeInfo(session.progress().toFloat().coerceIn(0f, 1f), 0f..1f)
                },
            )
            val lines = buildList {
                if (session.state == SessionState.ACTIVE) {
                    add(
                        listOf(
                            res.sessionCadence(cadence, session.intensity, format),
                            res.sessionSteps(session, format),
                        ).joinToString(" · "),
                    )
                } else {
                    add(
                        listOfNotNull(
                            res.sessionSteps(session, format),
                            res.sessionZone(session, format),
                        ).joinToString(" · "),
                    )
                }
                add(res.sessionEstimates(session, format))
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                lines.forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                when {
                    finished -> {
                        TextButton(onClick = actions.onClose, modifier = Modifier.testTag(SessionCardTags.CLOSE)) {
                            Text(stringResource(R.string.session_card_close))
                        }
                        if (canKeepGoing) {
                            FilledTonalButton(
                                onClick = actions.onKeepGoing,
                                modifier = Modifier.testTag(SessionCardTags.KEEP_GOING),
                            ) {
                                ButtonIcon(PassoIcons.Play)
                                Text(stringResource(R.string.session_card_keep_going))
                            }
                        }
                    }

                    else -> {
                        OutlinedButton(onClick = actions.onStop, modifier = Modifier.testTag(SessionCardTags.STOP)) {
                            ButtonIcon(PassoIcons.Stop)
                            Text(stringResource(R.string.session_card_stop))
                        }
                        if (session.state == SessionState.PAUSED) {
                            FilledTonalButton(
                                onClick = actions.onResume,
                                modifier = Modifier.testTag(SessionCardTags.RESUME),
                            ) {
                                ButtonIcon(PassoIcons.Play)
                                Text(stringResource(R.string.session_card_resume))
                            }
                        } else {
                            FilledTonalButton(
                                onClick = actions.onPause,
                                modifier = Modifier.testTag(SessionCardTags.PAUSE),
                            ) {
                                ButtonIcon(PassoIcons.Pause)
                                Text(stringResource(R.string.session_card_pause))
                            }
                        }
                    }
                }
            }
        }
    }
}

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

/** The mark of an outing: a run is a bolt, as in the walk list; a walk is the way to a flag. */
fun sessionIcon(intensity: SessionIntensity): ImageVector =
    if (intensity == SessionIntensity.RUN) PassoIcons.Bolt else PassoIcons.Outing

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

private const val MILLIS_PER_MINUTE = 60_000L

/** Hooks for the UI tests. */
object SessionCardTags {
    const val CARD = "session_card"
    const val SENTENCE = "session_card_sentence"
    const val PAUSE = "session_card_pause"
    const val RESUME = "session_card_resume"
    const val STOP = "session_card_stop"
    const val KEEP_GOING = "session_card_keep_going"
    const val CLOSE = "session_card_close"
}
