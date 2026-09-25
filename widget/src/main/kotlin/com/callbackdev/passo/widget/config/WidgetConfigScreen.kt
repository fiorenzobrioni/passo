package com.callbackdev.passo.widget.config

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.GroupHeader
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.SwitchRow
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.passo.core.designsystem.theme.widgetCardContainer
import com.callbackdev.passo.widget.R
import com.callbackdev.passo.widget.WidgetArrangement
import com.callbackdev.passo.widget.WidgetBackground
import com.callbackdev.passo.widget.WidgetKind
import com.callbackdev.passo.widget.WidgetLook
import com.callbackdev.passo.widget.WidgetModel
import com.callbackdev.passo.widget.isNight
import com.callbackdev.passo.widget.widgetCardFill
import com.callbackdev.passo.widget.widgetDressFor
import kotlin.math.roundToInt

/** Tags for the tests. */
object WidgetConfigTags {
    const val LIST = "widget_config_list"
}

/**
 * One widget's settings, reached from the launcher's reconfigure flow (Chiaro's
 * `WidgetConfigActivity`, the same groups in the same order): the card at the top, at the sizes
 * it can be given; then what it is painted on and how solid; then what it carries. Every choice
 * is saved as it is made and repaints this one card; «Done» only closes the door.
 *
 * Only the switches that change something on [kind] are offered: a switch that changes nothing
 * is the screen lying about the card.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetConfigScreen(
    kind: WidgetKind,
    look: WidgetLook,
    model: WidgetModel?,
    placed: DpSize?,
    onLook: (WidgetLook) -> Unit,
    onOpacityDrag: (Int) -> Unit,
    onOpacityDone: () -> Unit,
    onDone: () -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.widget_config_title)) }) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
                .testTag(WidgetConfigTags.LIST),
        ) {
            WidgetPreviewSection(kind, model?.copy(look = look), placed)

            GroupHeader(stringResource(R.string.widget_config_background))
            SettingsGroup {
                BackgroundChoices(look, model, onLook)
                GroupDivider()
                OpacityRow(look.opacityPct, onOpacityDrag, onOpacityDone)
            }

            GroupHeader(stringResource(R.string.widget_config_content))
            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.widget_config_show_sentence),
                    note = stringResource(R.string.widget_config_show_sentence_note),
                    checked = look.showSentence,
                    onChange = { onLook(look.copy(showSentence = it)) },
                )
                when (kind) {
                    WidgetKind.GLANCE -> {
                        GroupDivider()
                        SwitchRow(
                            label = stringResource(R.string.widget_config_show_hours),
                            note = stringResource(R.string.widget_config_show_hours_note),
                            checked = look.showHours,
                            onChange = { onLook(look.copy(showHours = it)) },
                        )
                    }

                    WidgetKind.WORDS -> {
                        GroupDivider()
                        SwitchRow(
                            label = stringResource(R.string.widget_config_show_goal),
                            note = stringResource(R.string.widget_config_show_goal_note),
                            checked = look.showGoal,
                            onChange = { onLook(look.copy(showGoal = it)) },
                        )
                        GroupDivider()
                        SwitchRow(
                            label = stringResource(R.string.widget_config_show_metrics),
                            note = stringResource(R.string.widget_config_show_metrics_note),
                            checked = look.showMetrics,
                            onChange = { onLook(look.copy(showMetrics = it)) },
                        )
                        GroupDivider()
                        SwitchRow(
                            label = stringResource(R.string.widget_config_show_details),
                            note = stringResource(R.string.widget_config_show_details_note),
                            checked = look.showDetails,
                            onChange = { onLook(look.copy(showDetails = it)) },
                        )
                    }
                }
            }

            if (kind == WidgetKind.GLANCE) {
                GroupHeader(stringResource(R.string.widget_config_arrangement))
                SettingsGroup {
                    Column(Modifier.selectableGroup()) {
                        listOf(
                            WidgetArrangement.RING_START to R.string.widget_arrangement_ring_start,
                            WidgetArrangement.RING_END to R.string.widget_arrangement_ring_end,
                        ).forEach { (arrangement, label) ->
                            ChoiceRow(
                                label = stringResource(label),
                                selected = look.arrangement == arrangement,
                                onPick = { onLook(look.copy(arrangement = arrangement)) },
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 24.dp),
            ) {
                Text(stringResource(R.string.widget_config_done))
            }
        }
    }
}

/** What the background question offers, in Chiaro's order less its sky. Every [WidgetBackground] has its row. */
internal val WidgetBackgroundChoices: List<Pair<WidgetBackground, Int>> = listOf(
    WidgetBackground.LIGHT to R.string.widget_bg_light,
    WidgetBackground.DARK to R.string.widget_bg_dark,
    WidgetBackground.SYSTEM to R.string.widget_bg_system,
    WidgetBackground.COLOR to R.string.widget_bg_color,
)

/** Chiaro's six, in Chiaro's order. Every [WidgetCardColor] has its swatch. */
internal val WidgetCardColorChoices: List<Pair<WidgetCardColor, Int>> = listOf(
    WidgetCardColor.BLUE to R.string.widget_color_blue,
    WidgetCardColor.AZURE to R.string.widget_color_azure,
    WidgetCardColor.GREEN to R.string.widget_color_green,
    WidgetCardColor.TEAL to R.string.widget_color_teal,
    WidgetCardColor.PLUM to R.string.widget_color_plum,
    WidgetCardColor.CLAY to R.string.widget_color_clay,
)

/**
 * What kind of card, each row with a swatch of the ground it paints; and, once «A colour» is
 * picked, which colour, as a strip of swatches with the chosen one's name under it (a swatch is
 * never the only label).
 */
@Composable
private fun BackgroundChoices(look: WidgetLook, model: WidgetModel?, onLook: (WidgetLook) -> Unit) {
    val context = LocalContext.current
    val settings = model?.settings ?: com.callbackdev.passo.core.model.UserSettings()
    val dress = widgetDressFor(context, settings)
    val night = isNight(context)
    Column(Modifier.selectableGroup()) {
        WidgetBackgroundChoices.forEach { (background, label) ->
            ChoiceRow(
                label = stringResource(label),
                selected = look.background == background,
                onPick = { onLook(look.copy(background = background)) },
            ) {
                val swatch = Modifier
                    .size(width = 44.dp, height = 30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                val full = look.copy(background = background, opacityPct = 100)
                if (background == WidgetBackground.SYSTEM) {
                    // The phone's choice, drawn as both of its answers on a diagonal.
                    Canvas(swatch) {
                        drawRect(dress.lightScheme.surface)
                        drawPath(
                            Path().apply {
                                moveTo(size.width, 0f)
                                lineTo(size.width, size.height)
                                lineTo(0f, size.height)
                                close()
                            },
                            dress.darkScheme.surface,
                        )
                    }
                } else {
                    Box(swatch.background(widgetCardFill(full, dress, night)))
                }
            }
        }
    }
    if (look.background == WidgetBackground.COLOR) {
        ColorSwatches(look.cardColor, start = 56.dp, end = 16.dp) { onLook(look.copy(cardColor = it)) }
    }
}

@Composable
private fun ColorSwatches(selected: WidgetCardColor, start: Dp, end: Dp, onPick: (WidgetCardColor) -> Unit) {
    Column(modifier = Modifier.padding(start = start, end = end, bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.selectableGroup()) {
            WidgetCardColorChoices.forEach { (color, label) ->
                val chosen = color == selected
                val name = stringResource(label)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(
                            width = if (chosen) 2.dp else 0.dp,
                            color = if (chosen) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape,
                        )
                        .padding(if (chosen) 4.dp else 0.dp)
                        .clip(CircleShape)
                        .background(widgetCardContainer(color))
                        .selectable(selected = chosen, onClick = { onPick(color) }, role = Role.RadioButton)
                        .semantics { contentDescription = name },
                ) {
                    if (chosen) {
                        // Every card colour is a dark ground under white ink.
                        Icon(
                            PassoIcons.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = stringResource(WidgetCardColorChoices.first { it.first == selected }.second),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onPick: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onPick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        trailing?.invoke()
    }
}

/** How solid the card is: the name and the value on one line, the slider under them, in steps of 5%. */
@Composable
private fun OpacityRow(pct: Int, onChange: (Int) -> Unit, onDone: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.widget_config_opacity),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = when (pct) {
                    100 -> stringResource(R.string.widget_opacity_full)
                    0 -> stringResource(R.string.widget_opacity_transparent)
                    else -> stringResource(R.string.widget_opacity_percent, pct)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = pct.toFloat(),
            onValueChange = { raw -> onChange((raw / 5f).roundToInt() * 5) },
            onValueChangeFinished = onDone,
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}
