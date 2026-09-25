package com.callbackdev.passo.feature.settings

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.backup.DataContents
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.backup.Backup
import com.callbackdev.passo.core.domain.backup.BackupDay
import com.callbackdev.passo.core.domain.calibration.CalibratedStep
import com.callbackdev.passo.core.domain.calibration.StepCalibration
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.feature.settings.calibration.CalibrationActions
import com.callbackdev.passo.feature.settings.calibration.CalibrationPhase
import com.callbackdev.passo.feature.settings.calibration.CalibrationScreen
import com.callbackdev.passo.feature.settings.calibration.CalibrationUiState
import com.callbackdev.passo.feature.settings.data.DataOutcome
import com.callbackdev.passo.feature.settings.data.DataTags
import com.callbackdev.passo.feature.settings.data.DataUiState
import com.callbackdev.passo.feature.settings.data.ImportPreview
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The README's pictures of Settings, the step calibration and the data rows (docs/screenshots). Run with
 * `./gradlew :feature:settings:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("passo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    @Test
    fun appearance() {
        val state = SettingsUiState(
            settings = UserSettings(dailyGoalSteps = 10_000, units = UnitPreference.METRIC),
            profile = Profile(heightMeters = 1.78, weightKg = 74.0),
            version = "0.1.0",
        )
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Wallpaper colors"))
        compose.waitForIdle()
        save("settings-appearance")
    }

    @Test
    fun notifications() {
        // Allowed, as on a phone that said yes in the first run: no "notifications are off" card.
        shadowOf(ApplicationProvider.getApplicationContext<Application>())
            .grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val state = SettingsUiState(
            settings = UserSettings(
                dailyGoalSteps = 10_000,
                units = UnitPreference.METRIC,
                firstDayOfWeek = DayOfWeek.MONDAY,
                goalReachedNotification = true,
                eveningReminder = true,
                eveningReminderTime = LocalTime.of(20, 30),
                eveningReminderThresholdPercent = 75,
                weeklySummary = true,
            ),
            profile = Profile(heightMeters = 1.78, weightKg = 74.0),
            version = "0.1.0",
        )
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.WEEKLY_SUMMARY))
        compose.waitForIdle()
        save("settings-notifications")
    }

    @Test
    fun calibration() {
        // 100 metres walked in 131 steps at an everyday pace, by a reader 1.78 m tall.
        val state = CalibrationUiState(
            step = CalibratedStep.WALKING,
            distanceMeters = 100.0,
            units = UnitPreference.METRIC,
            phase = CalibrationPhase.RESULT,
            permission = true,
            counterReady = true,
            steps = 131,
            elapsedMillis = 76_000,
            result = StepCalibration.measure(CalibratedStep.WALKING, 100.0, 48_210, 48_341, 76_000),
            profile = Profile(heightMeters = 1.78, weightKg = 74.0),
        )
        compose.setContent { PassoTheme { CalibrationScreen(state, onBack = {}, actions = CalibrationActions()) } }
        compose.waitForIdle()
        save("calibration")
    }

    @Test
    fun data() {
        val state = SettingsUiState(
            settings = UserSettings(dailyGoalSteps = 10_000, units = UnitPreference.METRIC),
            profile = Profile(heightMeters = 1.78, weightKg = 74.0),
            version = "0.1.0",
        )
        val data = DataUiState(
            contents = DataContents(days = 412, outings = 38),
            outcome = DataOutcome.BackupWritten("passo-backup-2026-09-25.json", days = 412, outings = 38),
        )
        compose.setContent {
            PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions(), data = data) }
        }
        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(DataTags.TABLE))
        compose.waitForIdle()
        save("settings-data")
    }

    @Test
    fun import() {
        val state = SettingsUiState(
            settings = UserSettings(dailyGoalSteps = 10_000, units = UnitPreference.METRIC),
            profile = Profile(),
            version = "0.1.0",
        )
        val first = LocalDate.of(2025, 8, 9).toEpochDay()
        val last = LocalDate.of(2026, 9, 25).toEpochDay()
        val backup = Backup(
            exportedAtMillis = LocalDate.of(2026, 9, 25).atTime(21, 40).atZone(ZoneId.systemDefault())
                .toInstant().toEpochMilli(),
            zone = "Europe/Rome",
            appVersion = "0.1.0",
            profile = Profile(),
            settings = UserSettings(),
            days = (first..last).map { BackupDay(DailySummary(it, 9_000, 0.0, 0.0, 0, 0, 10_000, true), emptyList()) },
            plans = emptyList(),
            sessions = List(38) { sampleOuting(first + it * 10) },
        )
        val data = DataUiState(
            contents = DataContents(days = 1, outings = 0),
            preview = ImportPreview(backup, "passo-backup-2026-09-25.json"),
        )
        compose.setContent {
            PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions(), data = data) }
        }
        compose.waitForIdle()
        save("data-import")
    }

    private fun sampleOuting(day: Long) = Session(
        planId = null,
        name = null,
        goalKind = SessionGoalKind.TIME,
        goalValue = 20,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        vibrate = true,
        localEpochDay = day,
        startedAtMillis = day * 86_400_000,
        state = SessionState.FINISHED,
    )

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
