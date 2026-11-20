package com.callbackdev.passo.feature.settings.learning

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.StepCurveChart
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.format.unbrokenText
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.calibration.LearnedSteps
import com.callbackdev.passo.core.domain.calibration.StepOffer
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.UnitSystem
import com.callbackdev.passo.feature.settings.R
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The proposal (PLANNING.md §11 Phase 17): the sentence first, what the outings measured and
 * what it would change, then the curve drawn against the step in use, then the choice. Never a
 * notification: it waits where the step is set, for the reader to come.
 */
@Composable
fun StepProposalCard(
    offer: StepOffer,
    profile: Profile,
    format: MeasureFormatter,
    onUse: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inUse = StepLengths.of(profile)
    val learnt = format.distance(offer.curve.learntMeters).unbrokenText()
    val now = format.stepLength(offer.lengthNow).unbrokenText()
    val before = format.stepLength(offer.lengthInUse).unbrokenText()
    val sentence = when {
        abs(offer.share) < SAME_WITHIN -> stringResource(R.string.learning_same, learnt, now, before)
        offer.share > 0 -> stringResource(R.string.learning_longer, learnt, now, before, format.percent(offer.share))
        else -> stringResource(R.string.learning_shorter, learnt, now, before, format.percent(-offer.share))
    }
    val inUseName = inUseName(profile)
    // The scale's top number carries the unit, as a chart's axis says what it counts in.
    val resources = LocalResources.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(StepLearningTags.CARD),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    PassoIcons.Gps,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.learning_title),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        sentence,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(StepLearningTags.SENTENCE),
                    )
                }
            }
            StepCurveChart(
                curve = offer.curve,
                inUse = inUse,
                lengthLabel = { format.stepLength(it).number },
                topLengthLabel = { resources.format(format.stepLength(it)) },
                lengthTick = if (format.units ==
                    UnitSystem.IMPERIAL
                ) {
                    TICK_INCHES * UnitConversions.METERS_PER_INCH
                } else {
                    TICK_METERS
                },
                curveName = stringResource(R.string.learning_curve_name),
                inUseName = "$inUseName · ${format.stepLength(inUse.walkingMeters).text()}",
                description = chartDescription(offer, inUseName, inUse, format),
            )
            Text(
                stringResource(R.string.learning_estimates_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onNotNow, modifier = Modifier.testTag(StepLearningTags.NOT_NOW)) {
                    Text(stringResource(R.string.learning_not_now))
                }
                FilledTonalButton(onClick = onUse, modifier = Modifier.testTag(StepLearningTags.USE)) {
                    Text(stringResource(R.string.learning_use))
                }
            }
        }
    }
}

/**
 * What «Use it» changed and what it did not, with the one way to the past days: offered, asked as
 * Settings asks it, never done unasked. Without it, History would show a step the reader could not
 * explain between yesterday and today.
 */
@Composable
fun StepUsedNote(onApplyPast: () -> Unit, onDone: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenMargin).testTag(StepLearningTags.USED),
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(
                    PassoIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    stringResource(R.string.learning_used),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                TextButton(onClick = onDone, modifier = Modifier.testTag(StepLearningTags.DONE)) {
                    Text(stringResource(R.string.learning_done))
                }
                FilledTonalButton(onClick = onApplyPast, modifier = Modifier.testTag(StepLearningTags.APPLY_PAST)) {
                    Text(stringResource(R.string.learning_apply_past))
                }
            }
        }
    }
}

/**
 * The calibration page's quiet row: a curve that counts and is not in use, put away from Settings,
 * found again here, where the reader comes to look at their step. Touching it opens the card.
 */
@Composable
fun StepLearntRow(offer: StepOffer, format: MeasureFormatter, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onOpen)
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(StepLearningTags.ROW),
        ) {
            Icon(
                PassoIcons.Gps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.learning_row, format.distance(offer.curve.learntMeters).unbrokenText()),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    stringResource(R.string.learning_row_note),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(PassoIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Where the walking step in use comes from, as the chart's dashed line is named. */
@Composable
fun inUseName(profile: Profile): String = stringResource(
    when (profile.stepLengthMode) {
        StepLengthMode.LEARNED -> R.string.learning_in_use_learned

        StepLengthMode.CALIBRATED -> R.string.learning_in_use_measured

        StepLengthMode.MANUAL -> R.string.learning_in_use_manual

        StepLengthMode.AUTO ->
            if (profile.heightMeters != null) R.string.learning_in_use_height else R.string.learning_in_use_default
    },
)

/**
 * The way back from a learnt step, named as what was in use before it: the step measured, the one
 * typed, or the height's estimate. Null when no learnt step is in use.
 */
@Composable
fun wayBackLabel(profile: Profile, format: MeasureFormatter): String? {
    val back = LearnedSteps.goBack(profile)
    val length = format.stepLength(StepLengths.of(back).walkingMeters).text()
    return when (LearnedSteps.wayBack(profile)) {
        null, StepLengthMode.LEARNED -> null

        StepLengthMode.CALIBRATED -> stringResource(R.string.settings_step_back_measured, length)

        StepLengthMode.MANUAL -> stringResource(R.string.settings_step_back_manual, length)

        StepLengthMode.AUTO ->
            if (profile.heightMeters != null) {
                stringResource(R.string.settings_step_back_height)
            } else {
                stringResource(R.string.settings_step_back_default, length)
            }
    }
}

/** The chart in one sentence, for a screen reader. */
@Composable
private fun chartDescription(
    offer: StepOffer,
    inUseName: String,
    inUse: StepLengths,
    format: MeasureFormatter,
): String {
    val walking = offer.curve.points.filter { it.cadence < RUNNING_CADENCE }
    val first = walking.firstOrNull() ?: offer.curve.points.first()
    val last = walking.lastOrNull() ?: first
    val inUseText = format.stepLength(inUse.walkingMeters).text()
    return if (first == last || format.stepLength(first.meters) == format.stepLength(last.meters)) {
        stringResource(
            R.string.learning_chart_one,
            format.stepLength(first.meters).text(),
            first.cadence.roundToInt().toString(),
            inUseName,
            inUseText,
        )
    } else {
        stringResource(
            R.string.learning_chart_span,
            format.stepLength(first.meters).text(),
            first.cadence.roundToInt().toString(),
            format.stepLength(last.meters).text(),
            last.cadence.roundToInt().toString(),
            inUseName,
            inUseText,
        )
    }
}

/** Under half a percent the share reads 0%: the sentence says it would hardly change. */
private const val SAME_WITHIN = 0.005

/** The chart's scale: every 5 cm, or every 2 inches. */
private const val TICK_METERS = 0.05
private const val TICK_INCHES = 2.0

/** Hooks for the UI tests. */
object StepLearningTags {
    const val CARD = "learning_card"
    const val SENTENCE = "learning_sentence"
    const val USE = "learning_use"
    const val NOT_NOW = "learning_not_now"
    const val USED = "learning_used"
    const val APPLY_PAST = "learning_apply_past"
    const val DONE = "learning_done"
    const val ROW = "learning_row"
    const val WAY_BACK = "learning_way_back"
}
