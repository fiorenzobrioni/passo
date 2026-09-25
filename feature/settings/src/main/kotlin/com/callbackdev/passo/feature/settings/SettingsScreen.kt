package com.callbackdev.passo.feature.settings

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.GroupHeader
import com.callbackdev.passo.core.designsystem.components.InfoRow
import com.callbackdev.passo.core.designsystem.components.ProgressRing
import com.callbackdev.passo.core.designsystem.components.RadioDialog
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.SwitchRow
import com.callbackdev.passo.core.designsystem.components.ValueRow
import com.callbackdev.passo.core.designsystem.components.ValueStepper
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.settings.InputScale
import com.callbackdev.passo.core.domain.settings.ProfileInputs
import com.callbackdev.passo.core.domain.settings.firstDayOfWeek
import com.callbackdev.passo.core.domain.settings.resolve
import com.callbackdev.passo.core.model.AppFont
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UnitSystem
import com.callbackdev.passo.core.model.UserSettings
import java.time.DayOfWeek
import java.time.format.TextStyle

@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onBack = onBack,
        actions = SettingsActions(
            updateSettings = viewModel::updateSettings,
            updateProfile = viewModel::updateProfile,
            applyProfileToPastDays = viewModel::applyProfileToPastDays,
            setTracking = viewModel::setTracking,
        ),
    )
}

/** What the list can ask for, as functions: the screen is a plain composable a test can draw. */
class SettingsActions(
    val updateSettings: ((UserSettings) -> UserSettings) -> Unit = {},
    val updateProfile: ((Profile) -> Profile) -> Unit = {},
    val applyProfileToPastDays: () -> Unit = {},
    val setTracking: (Boolean) -> Unit = {},
)

/**
 * Settings, laid out as Chiaro's (its design review of 23 set 2026): every group on one rounded
 * ground under a header in the accent, a live preview of the appearance above the choices that
 * change it, the privacy note as a statement, the licence and the credits last. Only what
 * already does something is here: a switch for a feature that has not shipped would be the
 * screen lying about the app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState?,
    onBack: () -> Unit,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
) {
    val scroll = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    Scaffold(
        modifier = modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(PassoIcons.Back, contentDescription = stringResource(R.string.settings_back))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        // Until the store's first answer the list is not drawn: a screen of defaults that may be
        // about to change is a lie with good intentions.
        if (state != null) {
            SettingsList(state, actions, Modifier.fillMaxSize().padding(padding))
        }
    }
}

private enum class Dialog {
    HEIGHT,
    WEIGHT,
    SEX,
    STEP_LENGTH,
    RUNNING_STEP_LENGTH,
    APPLY_PAST,
    GOAL,
    UNITS,
    FIRST_DAY_OF_WEEK,
    MIN_WALK,
    THEME,
    PALETTE,
    FONT,
}

@Composable
private fun SettingsList(state: SettingsUiState, actions: SettingsActions, modifier: Modifier) {
    val context = LocalContext.current
    val settings = state.settings
    val profile = state.profile
    val format = rememberMeasureFormatter(settings.units)
    var dialog by rememberSaveable { mutableStateOf<Dialog?>(null) }
    val lengths = StepLengths.of(profile)

    LazyColumn(modifier = modifier.testTag(SettingsTags.LIST), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { GroupHeader(stringResource(R.string.settings_group_profile)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_height),
                    value =
                    profile.heightMeters?.let { format.height(it).text() }
                        ?: stringResource(R.string.settings_not_set),
                    onClick = { dialog = Dialog.HEIGHT },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_weight),
                    value =
                    profile.weightKg?.let { format.weight(it).text() } ?: stringResource(R.string.settings_not_set),
                    onClick = { dialog = Dialog.WEIGHT },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_sex),
                    value = sexLabel(profile.sex),
                    onClick = { dialog = Dialog.SEX },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_step_length),
                    value = stepLengthValue(profile, lengths, format),
                    onClick = { dialog = Dialog.STEP_LENGTH },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_running_step_length),
                    value = if (profile.runningStepLengthMeters != null) {
                        stringResource(
                            R.string.settings_running_manual,
                            format.stepLength(lengths.runningMeters).text(),
                        )
                    } else {
                        stringResource(R.string.settings_running_auto, format.stepLength(lengths.runningMeters).text())
                    },
                    onClick = { dialog = Dialog.RUNNING_STEP_LENGTH },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_apply_past),
                    value = stringResource(R.string.settings_apply_past_note),
                    onClick = { dialog = Dialog.APPLY_PAST },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_goal)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_goal),
                    value = pluralStringResource(
                        R.plurals.settings_goal_value,
                        settings.dailyGoalSteps,
                        format.steps(settings.dailyGoalSteps),
                    ),
                    onClick = { dialog = Dialog.GOAL },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_units)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_units),
                    value = unitsLabel(settings.units),
                    onClick = { dialog = Dialog.UNITS },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_first_day_of_week),
                    value = firstDayLabel(settings.firstDayOfWeek),
                    onClick = { dialog = Dialog.FIRST_DAY_OF_WEEK },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_today)) }
        item {
            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.settings_typical_line),
                    note = stringResource(R.string.settings_typical_line_note),
                    checked = settings.typicalDayLine,
                    onChange = { on -> actions.updateSettings { it.copy(typicalDayLine = on) } },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_walks)) }
        item {
            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.settings_walks),
                    note = stringResource(R.string.settings_walks_note),
                    checked = settings.walkDetection,
                    onChange = { on -> actions.updateSettings { it.copy(walkDetection = on) } },
                    modifier = Modifier.testTag(SettingsTags.WALKS),
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_min_walk),
                    value = pluralStringResource(
                        R.plurals.settings_minutes,
                        settings.minWalkMinutes,
                        settings.minWalkMinutes,
                    ),
                    onClick = { dialog = Dialog.MIN_WALK },
                    enabled = settings.walkDetection,
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_appearance)) }
        item { AppearancePreview(format) }
        item {
            SettingsGroup {
                ValueRow(stringResource(R.string.settings_theme), themeLabel(settings.theme), { dialog = Dialog.THEME })
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_palette),
                    value = paletteLabel(settings.palette),
                    onClick = { dialog = Dialog.PALETTE },
                    enabled = !settings.dynamicColor,
                )
                GroupDivider()
                ValueRow(stringResource(R.string.settings_font), fontLabel(settings.font), { dialog = Dialog.FONT })
                GroupDivider()
                SwitchRow(
                    label = stringResource(R.string.settings_dynamic_color),
                    note = stringResource(R.string.settings_dynamic_color_note),
                    checked = settings.dynamicColor,
                    onChange = { on -> actions.updateSettings { it.copy(dynamicColor = on) } },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_language)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_language),
                    value = currentLanguageLabel(),
                    trailing = true,
                    onClick = {
                        // The system's per-app language page: one place for it, the same in every app.
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APP_LOCALE_SETTINGS,
                                Uri.fromParts("package", context.packageName, null),
                            ),
                        )
                    },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_tracking)) }
        item {
            SettingsGroup {
                SwitchRow(
                    label = stringResource(R.string.settings_tracking),
                    note = stringResource(
                        if (settings.trackingEnabled) R.string.settings_tracking_on else R.string.settings_tracking_off,
                    ),
                    checked = settings.trackingEnabled,
                    onChange = actions.setTracking,
                    modifier = Modifier.testTag(SettingsTags.TRACKING),
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_privacy)) }
        item { PrivacyCard() }

        item { GroupHeader(stringResource(R.string.settings_group_about)) }
        item {
            SettingsGroup {
                InfoRow(stringResource(R.string.settings_version), state.version)
                GroupDivider()
                InfoRow(stringResource(R.string.settings_developer), stringResource(R.string.settings_developer_note))
                GroupDivider()
                InfoRow(stringResource(R.string.settings_copyright), stringResource(R.string.settings_copyright_note))
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_license),
                    value = stringResource(R.string.settings_license_note),
                    trailing = true,
                    onClick = { openUrl(context, "https://www.gnu.org/licenses/gpl-3.0.html") },
                )
                GroupDivider()
                ValueRow(
                    label = stringResource(R.string.settings_source_code),
                    value = stringResource(R.string.settings_source_code_note),
                    trailing = true,
                    onClick = { openUrl(context, "https://github.com/fiorenzobrioni/passo") },
                )
            }
        }

        item { GroupHeader(stringResource(R.string.settings_group_credits)) }
        item {
            SettingsGroup {
                ValueRow(
                    label = stringResource(R.string.settings_credit_font),
                    value = fontCreditNote(settings.font),
                    trailing = true,
                    onClick = { openUrl(context, fontCreditUrl(settings.font)) },
                )
                GroupDivider()
                InfoRow(
                    stringResource(R.string.settings_credit_sources),
                    stringResource(R.string.settings_credit_sources_note),
                )
            }
        }
    }

    val close = { dialog = null }
    val clearHeight = { actions.updateProfile { it.copy(heightMeters = null) } }
    val clearWeight = { actions.updateProfile { it.copy(weightKg = null) } }
    when (dialog) {
        Dialog.HEIGHT -> StepperDialog(
            title = stringResource(R.string.settings_height),
            scale = ProfileInputs.height(format.units),
            initial = profile.heightMeters ?: ProfileInputs.DEFAULT_HEIGHT_M,
            text = { format.height(it).text() },
            onSave = { v -> actions.updateProfile { it.copy(heightMeters = v) } },
            onClear = clearHeight.takeIf { profile.heightMeters != null },
            onDismiss = close,
        )

        Dialog.WEIGHT -> StepperDialog(
            title = stringResource(R.string.settings_weight),
            scale = ProfileInputs.weight(format.units),
            initial = profile.weightKg ?: ProfileInputs.DEFAULT_WEIGHT_KG,
            text = { format.weight(it).text() },
            onSave = { v -> actions.updateProfile { it.copy(weightKg = v) } },
            onClear = clearWeight.takeIf { profile.weightKg != null },
            onDismiss = close,
        )

        Dialog.SEX -> RadioDialog(
            title = stringResource(R.string.settings_sex),
            options = listOf(Sex.FEMALE, Sex.MALE, null).map { it to sexLabel(it) },
            selected = profile.sex,
            onSelect = { sex ->
                actions.updateProfile { it.copy(sex = sex) }
                dialog = null
            },
            onDismiss = close,
        )

        Dialog.STEP_LENGTH -> LengthDialog(
            title = stringResource(R.string.settings_step_length),
            scale = ProfileInputs.walkingStepLength(format.units),
            own = profile.walkingStepLengthMeters.takeIf { profile.stepLengthMode != StepLengthMode.AUTO },
            estimate = StepLengths.of(profile.copy(stepLengthMode = StepLengthMode.AUTO)).walkingMeters,
            format = format,
            onSave = { own ->
                actions.updateProfile {
                    if (own == null) {
                        it.copy(stepLengthMode = StepLengthMode.AUTO, walkingStepLengthMeters = null)
                    } else {
                        it.copy(stepLengthMode = StepLengthMode.MANUAL, walkingStepLengthMeters = own)
                    }
                }
            },
            onDismiss = close,
        )

        Dialog.RUNNING_STEP_LENGTH -> LengthDialog(
            title = stringResource(R.string.settings_running_step_length),
            scale = ProfileInputs.runningStepLength(format.units),
            own = profile.runningStepLengthMeters,
            estimate = StepLengths.of(profile.copy(runningStepLengthMeters = null)).runningMeters,
            format = format,
            onSave = { own -> actions.updateProfile { it.copy(runningStepLengthMeters = own) } },
            onDismiss = close,
        )

        Dialog.APPLY_PAST -> AlertDialog(
            onDismissRequest = close,
            title = { Text(stringResource(R.string.settings_apply_past_title)) },
            text = { Text(stringResource(R.string.settings_apply_past_body)) },
            confirmButton = {
                TextButton(onClick = {
                    actions.applyProfileToPastDays()
                    dialog = null
                }) {
                    Text(stringResource(R.string.settings_apply_past_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = close) { Text(stringResource(R.string.settings_cancel)) } },
        )

        Dialog.GOAL -> StepperDialog(
            title = stringResource(R.string.settings_goal),
            scale = ProfileInputs.goal,
            initial = settings.dailyGoalSteps.toDouble(),
            text = { format.steps(it.toInt()) },
            note = stringResource(R.string.settings_goal_note),
            onSave = { v -> actions.updateSettings { it.copy(dailyGoalSteps = v.toInt()) } },
            onClear = null,
            onDismiss = close,
        )

        Dialog.UNITS -> RadioDialog(
            title = stringResource(R.string.settings_units),
            options = UnitPreference.entries.map { it to unitsLabel(it) },
            selected = settings.units,
            onSelect = { units ->
                actions.updateSettings { it.copy(units = units) }
                dialog = null
            },
            onDismiss = close,
        )

        Dialog.FIRST_DAY_OF_WEEK -> RadioDialog(
            title = stringResource(R.string.settings_first_day_of_week),
            options = listOf(null, DayOfWeek.MONDAY, DayOfWeek.SUNDAY, DayOfWeek.SATURDAY).map {
                it to firstDayLabel(it)
            },
            selected = settings.firstDayOfWeek,
            explanation = stringResource(R.string.settings_first_day_of_week_note),
            onSelect = { day ->
                actions.updateSettings { it.copy(firstDayOfWeek = day) }
                dialog = null
            },
            onDismiss = close,
        )

        Dialog.MIN_WALK -> RadioDialog(
            title = stringResource(R.string.settings_min_walk),
            options = UserSettings.MIN_WALK_MINUTES_CHOICES.map {
                it to pluralStringResource(R.plurals.settings_minutes, it, it)
            },
            selected = settings.minWalkMinutes,
            explanation = stringResource(R.string.settings_min_walk_note),
            onSelect = { minutes ->
                actions.updateSettings { it.copy(minWalkMinutes = minutes) }
                dialog = null
            },
            onDismiss = close,
        )

        Dialog.THEME -> RadioDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries.map { it to themeLabel(it) },
            selected = settings.theme,
            onSelect = { theme ->
                actions.updateSettings { it.copy(theme = theme) }
                dialog = null
            },
            onDismiss = close,
        )

        Dialog.PALETTE -> RadioDialog(
            title = stringResource(R.string.settings_palette),
            explanation = stringResource(R.string.settings_palette_note),
            options = AppPalette.entries.map { it to paletteLabel(it) },
            selected = settings.palette,
            onSelect = { palette ->
                actions.updateSettings { it.copy(palette = palette) }
                dialog = null
            },
            onDismiss = close,
        )

        Dialog.FONT -> RadioDialog(
            title = stringResource(R.string.settings_font),
            explanation = stringResource(R.string.settings_font_note),
            options = AppFont.entries.map { it to fontLabel(it) },
            selected = settings.font,
            onSelect = { font ->
                actions.updateSettings { it.copy(font = font) }
                dialog = null
            },
            onDismiss = close,
        )

        null -> Unit
    }
}

@Composable
private fun stepLengthValue(profile: Profile, lengths: StepLengths, format: MeasureFormatter): String {
    val shown = format.stepLength(lengths.walkingMeters).text()
    return when {
        profile.stepLengthMode == StepLengthMode.CALIBRATED && profile.walkingStepLengthMeters != null ->
            stringResource(R.string.settings_step_length_calibrated, shown)

        profile.stepLengthMode == StepLengthMode.MANUAL && profile.walkingStepLengthMeters != null ->
            stringResource(R.string.settings_step_length_manual, shown)

        profile.heightMeters != null -> stringResource(R.string.settings_step_length_auto, shown)

        else -> stringResource(R.string.settings_step_length_auto_default, shown)
    }
}

/**
 * The appearance, drawn with itself (Chiaro's preview): a ring and a count in the reader's
 * palette and typeface, and the pace against a usual day in the goal's color, all consistent
 * with each other (a preview must not lie either). Every choice below changes it the
 * moment it is made. A picture, silent to a screen reader: the rows say every choice in words.
 */
@Composable
private fun AppearancePreview(format: MeasureFormatter) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = GroupShape,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = ScreenMargin, end = ScreenMargin, bottom = 12.dp)
            .clearAndSetSemantics { },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            ProgressRing(
                progress = PREVIEW_PROGRESS,
                usualProgress = PREVIEW_USUAL,
                reached = false,
                celebrate = false,
                onCelebrated = {},
                strokeWidth = 9.dp,
                modifier = Modifier.size(88.dp),
            ) { }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = format.steps(PREVIEW_STEPS),
                    style = PassoTheme.type.heroNumber.copy(fontSize = 34.sp, lineHeight = 38.sp),
                )
                Text(
                    text = stringResource(R.string.settings_preview_caption, format.steps(PREVIEW_GOAL)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Surface(
                    color = PassoTheme.colors.goalContainer,
                    contentColor = PassoTheme.colors.goal,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Icon(PassoIcons.TrendUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = stringResource(R.string.settings_preview_ahead, format.steps(PREVIEW_AHEAD)),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

private const val PREVIEW_STEPS = 6_240
private const val PREVIEW_GOAL = 8_000
private const val PREVIEW_PROGRESS = PREVIEW_STEPS / PREVIEW_GOAL.toFloat()
private const val PREVIEW_USUAL = 0.62f

/** The preview's own arithmetic: 6,240 against a usual 62% of 8,000 (4,960). */
private const val PREVIEW_AHEAD = PREVIEW_STEPS - 4_960

@Composable
private fun PrivacyCard() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        shape = GroupShape,
        modifier = Modifier.fillMaxWidth().padding(horizontal = ScreenMargin),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(16.dp)) {
            Icon(PassoIcons.Shield, contentDescription = null, modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.settings_privacy_headline), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.settings_privacy_body), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A number with a range, picked with the stepper, saved on confirm; [onClear] offers "not set". */
@Composable
private fun StepperDialog(
    title: String,
    scale: InputScale,
    initial: Double,
    text: @Composable (Double) -> String,
    onSave: (Double) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
    note: String? = null,
) {
    var value by remember { mutableDoubleStateOf(scale.snap(initial)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ValueStepper(
                    value = value.toFloat(),
                    range = scale.range.start.toFloat()..scale.range.endInclusive.toFloat(),
                    text = text(value),
                    onValueChange = { value = scale.snap(it.toDouble()) },
                    onDecrease = { value = scale.down(value) },
                    onIncrease = { value = scale.up(value) },
                    decreaseLabel = stringResource(R.string.settings_decrease),
                    increaseLabel = stringResource(R.string.settings_increase),
                )
                if (note != null) {
                    Text(
                        note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(value)
                onDismiss()
            }) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = {
            Row {
                if (onClear != null) {
                    TextButton(onClick = {
                        onClear()
                        onDismiss()
                    }) { Text(stringResource(R.string.settings_clear)) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) }
            }
        },
    )
}

/**
 * A step length: estimated, or the reader's own. The estimate is shown with its value, so the
 * choice is between two numbers rather than between two words.
 */
@Composable
private fun LengthDialog(
    title: String,
    scale: InputScale,
    own: Double?,
    estimate: Double,
    format: MeasureFormatter,
    onSave: (Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var mine by remember { mutableStateOf(own != null) }
    var value by remember { mutableDoubleStateOf(scale.snap(own ?: estimate)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ChoiceRow(
                    selected = !mine,
                    label = "${stringResource(
                        R.string.settings_choice_estimate,
                    )} · ${format.stepLength(estimate).text()}",
                    onClick = { mine = false },
                )
                ChoiceRow(selected = mine, label = stringResource(R.string.settings_choice_mine), onClick = {
                    mine =
                        true
                })
                if (mine) {
                    ValueStepper(
                        value = value.toFloat(),
                        range = scale.range.start.toFloat()..scale.range.endInclusive.toFloat(),
                        text = format.stepLength(value).text(),
                        onValueChange = { value = scale.snap(it.toDouble()) },
                        onDecrease = { value = scale.down(value) },
                        onIncrease = { value = scale.up(value) },
                        decreaseLabel = stringResource(R.string.settings_decrease),
                        increaseLabel = stringResource(R.string.settings_increase),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(if (mine) value else null)
                onDismiss()
            }) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
    )
}

@Composable
private fun ChoiceRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 8.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun sexLabel(sex: Sex?): String = when (sex) {
    Sex.FEMALE -> stringResource(R.string.settings_sex_female)
    Sex.MALE -> stringResource(R.string.settings_sex_male)
    null -> stringResource(R.string.settings_sex_unsaid)
}

/** The first day of the week in words; «Same as the phone (Monday)» when it follows the region. */
@Composable
private fun firstDayLabel(day: DayOfWeek?): String {
    val locale = LocalConfiguration.current.locales[0]
    fun name(of: DayOfWeek) =
        of.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    return if (day == null) {
        val region = android.content.res.Resources.getSystem().configuration.locales[0]
        stringResource(R.string.settings_first_day_system, name(firstDayOfWeek(null, region)))
    } else {
        name(day)
    }
}

@Composable
private fun unitsLabel(units: UnitPreference): String = when (units) {
    UnitPreference.SYSTEM -> {
        val region = android.content.res.Resources.getSystem().configuration.locales[0]?.country
        val resolved = if (UnitPreference.SYSTEM.resolve(region) == UnitSystem.IMPERIAL) {
            stringResource(R.string.settings_units_imperial_short)
        } else {
            stringResource(R.string.settings_units_metric_short)
        }
        stringResource(R.string.settings_units_system, resolved)
    }

    UnitPreference.METRIC -> stringResource(R.string.settings_units_metric)

    UnitPreference.IMPERIAL -> stringResource(R.string.settings_units_imperial)
}

@Composable
private fun themeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
private fun paletteLabel(palette: AppPalette): String = when (palette) {
    AppPalette.PAPER -> stringResource(R.string.settings_palette_paper)
    AppPalette.VIVID -> stringResource(R.string.settings_palette_vivid)
}

@Composable
private fun fontLabel(font: AppFont): String = when (font) {
    AppFont.GOOGLE_SANS -> stringResource(R.string.settings_font_google_sans)
    AppFont.INTER -> stringResource(R.string.settings_font_inter)
    AppFont.SYSTEM -> stringResource(R.string.settings_font_system)
}

/** Both bundled faces travel in the APK, so both are credited; the note says which one is on screen. */
@Composable
private fun fontCreditNote(font: AppFont): String {
    val inUse = when (font) {
        AppFont.SYSTEM -> stringResource(R.string.settings_credit_font_inuse_system)
        else -> stringResource(R.string.settings_credit_font_inuse, fontLabel(font))
    }
    return stringResource(R.string.settings_credit_font_note) + " · " + inUse
}

private fun fontCreditUrl(font: AppFont): String = when (font) {
    AppFont.INTER -> "https://rsms.me/inter/"
    AppFont.GOOGLE_SANS -> "https://fonts.google.com/specimen/Google+Sans"
    AppFont.SYSTEM -> "https://openfontlicense.org"
}

/** What the app is speaking: the reader's pick in the system picker, or the phone's language. */
@Composable
private fun currentLanguageLabel(): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val appLocales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
    if (appLocales == null || appLocales.isEmpty) return stringResource(R.string.settings_language_system)
    val chosen = appLocales[0]
    return chosen.getDisplayLanguage(chosen).replaceFirstChar { it.titlecase(locale) }
}

private fun openUrl(context: Context, url: String) {
    // The browser opens it: Passo itself has no network access. No browser, no crash.
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
}

/** Hooks for the UI tests. */
object SettingsTags {
    const val LIST = "settings_list"
    const val TRACKING = "settings_tracking"
    const val WALKS = "settings_walks"
}
