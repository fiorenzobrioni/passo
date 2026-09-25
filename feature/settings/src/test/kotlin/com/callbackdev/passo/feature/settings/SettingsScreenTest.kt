package com.callbackdev.passo.feature.settings

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class SettingsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val state = SettingsUiState(
        settings = UserSettings(units = UnitPreference.METRIC),
        profile = Profile(heightMeters = 1.76),
        version = "0.1.0",
    )

    @Test
    fun `the profile says what each estimate rests on`() {
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }

        compose.onNodeWithText("176 cm").assertIsDisplayed()
        compose.onNodeWithText("Not set: average values are used").assertIsDisplayed()
        compose.onNodeWithText("Estimated from your height: 73 cm").assertIsDisplayed()
        snapshot("settings_top")
    }

    @Test
    fun `applying the profile to past days asks first`() {
        var applied = false
        compose.setContent {
            PassoTheme {
                SettingsScreen(
                    state,
                    onBack = {},
                    actions = SettingsActions(applyProfileToPastDays = {
                        applied =
                            true
                    }),
                )
            }
        }

        compose.onNodeWithText("Apply the profile to past days").performClick()
        compose.onNodeWithText("Recompute every past day?").assertIsDisplayed()
        assertThat(applied).isFalse()
        compose.onNodeWithText("Recompute").performClick()
        assertThat(applied).isTrue()
    }

    @Test
    fun `walk detection is a switch, and its minimum a choice of three`() {
        var settings = state.settings
        compose.setContent {
            PassoTheme {
                SettingsScreen(
                    state.copy(settings = settings),
                    onBack = {},
                    actions = SettingsActions(updateSettings = { settings = it(settings) }),
                )
            }
        }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Shortest walk"))
        compose.onNodeWithText("Shortest walk").performClick()
        compose.onNodeWithText("15 minutes").performClick()
        assertThat(settings.minWalkMinutes).isEqualTo(15)
        compose.onNodeWithTag(SettingsTags.WALKS).performClick()
        assertThat(settings.walkDetection).isFalse()
    }

    @Test
    fun `the first day of the week follows the phone until chosen`() {
        var settings = state.settings
        compose.setContent {
            PassoTheme {
                SettingsScreen(state, onBack = {
                }, actions = SettingsActions(updateSettings = { settings = it(settings) }))
            }
        }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("First day of the week"))
        compose.onNodeWithText("First day of the week").performClick()
        compose.onNodeWithText("Monday").performClick()
        assertThat(settings.firstDayOfWeek).isEqualTo(java.time.DayOfWeek.MONDAY)
    }

    @Test
    fun `pausing is a switch that says what it means`() {
        var tracking: Boolean? = null
        compose.setContent {
            PassoTheme {
                SettingsScreen(state, onBack = {}, actions = SettingsActions(setTracking = { tracking = it }))
            }
        }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Count steps"))
        compose.onNodeWithText("On, in the background").assertIsDisplayed()
        snapshot("settings_middle")
        compose.onNodeWithTag(SettingsTags.TRACKING).performClick()
        assertThat(tracking).isFalse()
    }

    @Test
    fun `the notification row says how it shows, and that counting goes on without it`() {
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.NOTIFICATION))
        compose.onNodeWithText("Notification").assertIsDisplayed()
        compose.onNodeWithText(
            "In the status bar: Android requires it while Passo counts. Turn it off and counting goes on",
        ).assertIsDisplayed()
        snapshot("settings_notification")
    }

    @Test
    fun `the goal notifications are switches, and the reminder's time and threshold follow its switch`() {
        grantNotifications()
        var settings by mutableStateOf(state.settings)
        compose.setContent {
            PassoTheme {
                SettingsScreen(
                    state.copy(settings = settings),
                    onBack = {},
                    actions = SettingsActions(updateSettings = { settings = it(settings) }),
                )
            }
        }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.WEEKLY_SUMMARY))
        compose.onNodeWithText("At 8:00 PM, if the goal is not met yet: the steps left, and the walk they take.")
            .assertIsDisplayed()
        compose.onNodeWithText("at 9:00 AM: last week’s steps", substring = true).assertIsDisplayed()
        // Off, the reminder's rows say what they would do but cannot be changed.
        compose.onNodeWithText("Remind me").assertIsNotEnabled()

        compose.onNodeWithTag(SettingsTags.EVENING_REMINDER).performClick()
        assertThat(settings.eveningReminder).isTrue()
        compose.onNodeWithText("Remind me").performClick()
        compose.onNodeWithText("Below half of the goal").performClick()
        assertThat(settings.eveningReminderThresholdPercent).isEqualTo(50)
        compose.onNodeWithText("At 8:00 PM, if the day is below half of the goal", substring = true)
            .assertIsDisplayed()

        compose.onNodeWithTag(SettingsTags.GOAL_REACHED).performClick()
        compose.onNodeWithTag(SettingsTags.WEEKLY_SUMMARY).performClick()
        assertThat(settings.goalReachedNotification).isTrue()
        assertThat(settings.weeklySummary).isTrue()
        compose.onNodeWithTag(SettingsTags.NOTIFICATIONS_BLOCKED).assertDoesNotExist()
        snapshot("settings_goal_notifications")
    }

    @Test
    fun `the reminder's time is picked on the clock`() {
        grantNotifications()
        var settings by mutableStateOf(state.settings.copy(eveningReminder = true))
        compose.setContent {
            PassoTheme {
                SettingsScreen(
                    state.copy(settings = settings),
                    onBack = {},
                    actions = SettingsActions(updateSettings = { settings = it(settings) }),
                )
            }
        }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Time"))
        compose.onNodeWithText("Time").performClick()
        compose.onNodeWithText("Passo uses no exact alarms", substring = true).assertIsDisplayed()
        snapshot("settings_reminder_time")
        compose.onNodeWithText("Save").performClick()
        assertThat(settings.eveningReminderTime).isEqualTo(LocalTime.of(20, 0))
    }

    @Test
    fun `a goal notification that Android would drop says so, with the way to fix it`() {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        val on = state.copy(settings = state.settings.copy(eveningReminder = true))
        compose.setContent { PassoTheme { SettingsScreen(on, onBack = {}, actions = SettingsActions()) } }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.NOTIFICATIONS_BLOCKED))
        compose.onNodeWithText("Notifications are off for Passo").assertIsDisplayed()
        compose.onNodeWithText("Turn on").assertHasClickAction()
        snapshot("settings_notifications_blocked")
    }

    @Test
    fun `with every goal notification off, a block is nobody's business`() {
        shadowOf(context.getSystemService(NotificationManager::class.java)).setNotificationsEnabled(false)
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.WEEKLY_SUMMARY))
        compose.onNodeWithTag(SettingsTags.NOTIFICATIONS_BLOCKED).assertDoesNotExist()
    }

    @Test
    fun `the tile row says what the tile does and what it costs`() {
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasTestTag(SettingsTags.TILE))
        compose.onNodeWithText("Quick Settings tile").assertIsDisplayed()
        compose.onNodeWithText("read only while it is open", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the licence and the typefaces are credited`() {
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Typefaces"))
        compose.onNodeWithText("in use: Google Sans", substring = true).assertIsDisplayed()
        compose.onNodeWithText("free to use, study", substring = true).assertExists()
    }

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun grantNotifications() {
        shadowOf(context as Application).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        runCatching {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
