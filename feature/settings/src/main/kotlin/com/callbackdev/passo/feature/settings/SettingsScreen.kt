package com.callbackdev.passo.feature.settings

import android.Manifest
import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.callbackdev.passo.core.designsystem.components.GroupDivider
import com.callbackdev.passo.core.designsystem.components.GroupHeader
import com.callbackdev.passo.core.designsystem.components.InfoRow
import com.callbackdev.passo.core.designsystem.components.ProgressRing
import com.callbackdev.passo.core.designsystem.components.RadioDialog
import com.callbackdev.passo.core.designsystem.components.SettingsGroup
import com.callbackdev.passo.core.designsystem.components.StatusCard
import com.callbackdev.passo.core.designsystem.components.StatusTone
import com.callbackdev.passo.core.designsystem.components.SwitchRow
import com.callbackdev.passo.core.designsystem.components.ValueRow
import com.callbackdev.passo.core.designsystem.components.ValueStepper
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.designsystem.format.rememberMeasureFormatter
import com.callbackdev.passo.core.designsystem.format.text
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.ScreenMargin
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.goals.GoalSchedule
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
import com.callbackdev.passo.core.tracking.CountingNotification
import com.callbackdev.passo.core.tracking.GoalNotificationsAccess
import com.callbackdev.passo.core.tracking.GoalNotificationsBlock
import com.callbackdev.passo.core.tracking.NotificationVisibility
import com.callbackdev.passo.core.tracking.StepsTile
import com.callbackdev.passo.core.tracking.TileRequestResult
import java.time.DayOfWeek
import java.time.LocalTime
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
    REMINDER_TIME,
    REMINDER_THRESHOLD,
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

        item { GroupHeader(stringResource(R.string.settings_group_notifications)) }
        item {
            GoalNotificationsGroup(
                settings = settings,
                onChange = actions.updateSettings,
                onPickTime = { dialog = Dialog.REMINDER_TIME },
                onPickThreshold = { dialog = Dialog.REMINDER_THRESHOLD },
            )
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
                GroupDivider()
                NotificationRow()
                GroupDivider()
                TileRow()
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

        Dialog.REMINDER_TIME -> TimeDialog(
            title = stringResource(R.string.settings_evening_reminder),
            initial = settings.eveningReminderTime,
            note = stringResource(R.string.settings_reminder_time_note),
            onSave = { time -> actions.updateSettings { it.copy(eveningReminderTime = time) } },
            onDismiss = close,
        )

        Dialog.REMINDER_THRESHOLD -> RadioDialog(
            title = stringResource(R.string.settings_reminder_threshold),
            options = UserSettings.EVENING_REMINDER_THRESHOLDS.map { it to thresholdLabel(it) },
            selected = settings.eveningReminderThresholdPercent,
            explanation = stringResource(R.string.settings_reminder_threshold_note),
            onSelect = { percent ->
                actions.updateSettings { it.copy(eveningReminderThresholdPercent = percent) }
                dialog = null
            },
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

/**
 * The counting notification: how it shows now, and the system's page that changes it. Read again
 * on every return to the screen, since the change is made there (`CountingNotification`).
 */
@Composable
private fun NotificationRow() {
    val context = LocalContext.current
    var visibility by remember { mutableStateOf(CountingNotification.visibility(context)) }
    LifecycleResumeEffect(Unit) {
        visibility = CountingNotification.visibility(context)
        onPauseOrDispose {}
    }
    ValueRow(
        label = stringResource(R.string.settings_notification),
        value = stringResource(
            when (visibility) {
                NotificationVisibility.SHOWN -> R.string.settings_notification_shown
                NotificationVisibility.MINIMIZED -> R.string.settings_notification_minimized
                NotificationVisibility.OFF -> R.string.settings_notification_off
            },
        ),
        trailing = true,
        onClick = { runCatching { context.startActivity(CountingNotification.settingsIntent(context)) } },
        modifier = Modifier.testTag(SettingsTags.NOTIFICATION),
    )
}

/**
 * Goal reached, the evening reminder and the weekly summary (PLANNING.md §8). Turning one on asks
 * for the notification permission where Android still can; where it cannot, or the reader
 * silenced them in the system, a card over the group says so and opens the page that fixes it,
 * so a switch that is on never promises what Android drops.
 */
@Composable
private fun GoalNotificationsGroup(
    settings: UserSettings,
    onChange: ((UserSettings) -> UserSettings) -> Unit,
    onPickTime: () -> Unit,
    onPickThreshold: () -> Unit,
) {
    val context = LocalContext.current
    var block by remember { mutableStateOf(GoalNotificationsAccess.block(context)) }
    LifecycleResumeEffect(Unit) {
        block = GoalNotificationsAccess.block(context)
        onPauseOrDispose {}
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        block = GoalNotificationsAccess.block(context)
    }
    val turn: (Boolean, (UserSettings, Boolean) -> UserSettings) -> Unit = { on, transform ->
        onChange { transform(it, on) }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (on && !granted) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
    val anyOn = settings.goalReachedNotification || settings.eveningReminder || settings.weeklySummary
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (anyOn && block != GoalNotificationsBlock.NONE) {
            StatusCard(
                icon = PassoIcons.Bell,
                title = stringResource(
                    if (block == GoalNotificationsBlock.CHANNEL) {
                        R.string.settings_notifications_blocked_channel_title
                    } else {
                        R.string.settings_notifications_blocked_title
                    },
                ),
                body = stringResource(R.string.settings_notifications_blocked_body),
                tone = StatusTone.PROBLEM,
                action = stringResource(R.string.settings_notifications_blocked_action),
                onAction = {
                    runCatching { context.startActivity(GoalNotificationsAccess.settingsIntent(context, block)) }
                },
                modifier = Modifier.padding(horizontal = ScreenMargin).testTag(SettingsTags.NOTIFICATIONS_BLOCKED),
            )
        }
        SettingsGroup {
            SwitchRow(
                label = stringResource(R.string.settings_goal_reached),
                note = stringResource(R.string.settings_goal_reached_note),
                checked = settings.goalReachedNotification,
                onChange = { on -> turn(on) { s, v -> s.copy(goalReachedNotification = v) } },
                modifier = Modifier.testTag(SettingsTags.GOAL_REACHED),
            )
            GroupDivider()
            SwitchRow(
                label = stringResource(R.string.settings_evening_reminder),
                note = stringResource(
                    R.string.settings_evening_reminder_note,
                    clockTime(settings.eveningReminderTime.minuteOfDay()),
                    thresholdClause(settings.eveningReminderThresholdPercent),
                ),
                checked = settings.eveningReminder,
                onChange = { on -> turn(on) { s, v -> s.copy(eveningReminder = v) } },
                modifier = Modifier.testTag(SettingsTags.EVENING_REMINDER),
            )
            GroupDivider()
            ValueRow(
                label = stringResource(R.string.settings_reminder_time),
                value = clockTime(settings.eveningReminderTime.minuteOfDay()),
                onClick = onPickTime,
                enabled = settings.eveningReminder,
            )
            GroupDivider()
            ValueRow(
                label = stringResource(R.string.settings_reminder_threshold),
                value = thresholdLabel(settings.eveningReminderThresholdPercent),
                onClick = onPickThreshold,
                enabled = settings.eveningReminder,
            )
            GroupDivider()
            SwitchRow(
                label = stringResource(R.string.settings_weekly_summary),
                note = stringResource(
                    R.string.settings_weekly_summary_note,
                    weekdayName(firstDayOfWeek(settings.firstDayOfWeek, systemLocale())),
                    clockTime(GoalSchedule.WEEKLY_SUMMARY_TIME.minuteOfDay()),
                ),
                checked = settings.weeklySummary,
                onChange = { on -> turn(on) { s, v -> s.copy(weeklySummary = v) } },
                modifier = Modifier.testTag(SettingsTags.WEEKLY_SUMMARY),
            )
        }
    }
}

/**
 * The Quick Settings tile: one system prompt adds it. The row says where it stands once the
 * system has answered; before that Passo cannot know, and does not guess.
 */
@Composable
private fun TileRow() {
    val context = LocalContext.current
    var result by rememberSaveable { mutableStateOf<TileRequestResult?>(null) }
    ValueRow(
        label = stringResource(R.string.settings_tile),
        value = stringResource(
            when (result) {
                TileRequestResult.ADDED, TileRequestResult.ALREADY_ADDED -> R.string.settings_tile_added
                TileRequestResult.FAILED -> R.string.settings_tile_failed
                TileRequestResult.NOT_ADDED, null -> R.string.settings_tile_note
            },
        ),
        onClick = { StepsTile.requestAdd(context) { result = it } },
        modifier = Modifier.testTag(SettingsTags.TILE),
    )
}

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

/** A time of day on the clock dial, 12 or 24 hours as the phone shows them; saved on confirm. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDialog(
    title: String,
    initial: LocalTime,
    note: String,
    onSave: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                TimePicker(state = state)
                Text(
                    note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(LocalTime.of(state.hour, state.minute))
                onDismiss()
            }) { Text(stringResource(R.string.settings_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_cancel)) } },
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

@Composable
private fun thresholdLabel(percent: Int): String = stringResource(
    when (percent) {
        75 -> R.string.settings_reminder_threshold_75
        50 -> R.string.settings_reminder_threshold_50
        else -> R.string.settings_reminder_threshold_100
    },
)

/** The threshold as it reads inside the reminder's sentence: «if the goal is not met yet». */
@Composable
private fun thresholdClause(percent: Int): String = stringResource(
    when (percent) {
        75 -> R.string.settings_reminder_when_75
        50 -> R.string.settings_reminder_when_50
        else -> R.string.settings_reminder_when_100
    },
)

/** A weekday as a sentence uses it: «Monday», «lunedì». */
@Composable
private fun weekdayName(day: DayOfWeek): String =
    day.getDisplayName(TextStyle.FULL, LocalConfiguration.current.locales[0])

/** The phone's own region, which the first day of the week follows until the reader picks one. */
private fun systemLocale() = android.content.res.Resources.getSystem().configuration.locales[0]

private fun LocalTime.minuteOfDay(): Int = hour * 60 + minute

/** The first day of the week in words; «Same as the phone (Monday)» when it follows the region. */
@Composable
private fun firstDayLabel(day: DayOfWeek?): String {
    val locale = LocalConfiguration.current.locales[0]
    fun name(of: DayOfWeek) =
        of.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    return if (day == null) {
        stringResource(R.string.settings_first_day_system, name(firstDayOfWeek(null, systemLocale())))
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
    const val NOTIFICATION = "settings_notification"
    const val WALKS = "settings_walks"
    const val GOAL_REACHED = "settings_goal_reached"
    const val EVENING_REMINDER = "settings_evening_reminder"
    const val WEEKLY_SUMMARY = "settings_weekly_summary"
    const val NOTIFICATIONS_BLOCKED = "settings_notifications_blocked"
    const val TILE = "settings_tile"
}
