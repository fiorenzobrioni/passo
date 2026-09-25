package com.callbackdev.passo.feature.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

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
    fun `the licence and the typefaces are credited`() {
        compose.setContent { PassoTheme { SettingsScreen(state, onBack = {}, actions = SettingsActions()) } }

        compose.onNodeWithTag(SettingsTags.LIST).performScrollToNode(hasText("Typefaces"))
        compose.onNodeWithText("in use: Google Sans", substring = true).assertIsDisplayed()
        compose.onNodeWithText("free to use, study", substring = true).assertExists()
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
