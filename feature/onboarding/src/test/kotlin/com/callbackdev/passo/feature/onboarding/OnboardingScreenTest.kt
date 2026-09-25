package com.callbackdev.passo.feature.onboarding

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.model.UnitPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The first run, page by page (PLANNING.md §11 Phase 3: UI tests), driven by a state held here
 * the way the ViewModel holds it, so what is tested is the screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class OnboardingScreenTest {
    @get:Rule val compose = createComposeRule()

    private var state by mutableStateOf(OnboardingState(units = UnitPreference.METRIC))
    private var finished = false

    private fun move(delta: Int) {
        val order = state.steps
        state = state.copy(step = order[(order.indexOf(state.step) + delta).coerceIn(0, order.lastIndex)])
    }

    private val actions = OnboardingActions(
        next = { move(+1) },
        back = { move(-1) },
        skipProfile = {
            state = state.copy(profileSkipped = true)
            move(+1)
        },
        setHeight = { state = state.copy(heightMeters = it) },
        setGoal = { state = state.copy(goalSteps = it) },
        finish = { finished = true },
    )

    private fun show() {
        compose.setContent { PassoTheme { OnboardingScreen(state, actions) } }
    }

    @Test
    fun `the welcome makes its three promises and starts`() {
        show()
        compose.onNodeWithText("It counts by itself").assertIsDisplayed()
        compose.onNodeWithText("It stays on your phone").assertIsDisplayed()
        snapshot("onboarding_welcome")

        compose.onNodeWithText("Get started").performClick()
        compose.onNodeWithText("Two numbers for the estimates").assertIsDisplayed()
    }

    @Test
    fun `the profile can be skipped, and shows the step it leads to`() {
        state = state.copy(step = OnboardingStep.PROFILE)
        show()
        compose.onNodeWithText("Estimated step length: 71 cm").assertIsDisplayed()
        snapshot("onboarding_profile")

        compose.onNodeWithTag(OnboardingTags.SKIP).performClick()
        assertThat(state.profileSkipped).isTrue()
        compose.onNodeWithText("Your daily goal").assertIsDisplayed()
    }

    @Test
    fun `the goal says what it means and moves in steps of 500`() {
        state = state.copy(step = OnboardingStep.GOAL)
        show()
        snapshot("onboarding_goal")
        compose.onNodeWithText("About 5.64 km, or 1 h 20 min of brisk walking").assertIsDisplayed()

        compose.onNodeWithContentDescription("More").performClick()
        assertThat(state.goalSteps).isEqualTo(8_500)
        compose.onNodeWithText("10,000").performClick()
        assertThat(state.goalSteps).isEqualTo(10_000)
    }

    @Test
    fun `permissions ask first, and once granted the flow can finish`() {
        state = state.copy(step = OnboardingStep.PERMISSIONS)
        show()
        compose.onNodeWithText("Allow").assertIsDisplayed()
        compose.onNodeWithText("Later").assertIsDisplayed()
        snapshot("onboarding_permissions")

        state = state.copy(permissionAsked = true)
        compose.onAllNodesWithText("Turned down", substring = true).onFirst().assertIsDisplayed()

        state = state.copy(activityGranted = true)
        compose.onNodeWithText("Allowed: Passo is counting.").assertIsDisplayed()
        compose.onNodeWithText("Start counting").performClick()
        assertThat(finished).isTrue()
    }

    @Test
    fun `the battery tip comes only on the phones that need it`() {
        assertThat(OnboardingState().steps).doesNotContain(OnboardingStep.BATTERY)
        state = state.copy(step = OnboardingStep.BATTERY, oemSlug = "xiaomi", manufacturer = "Xiaomi")
        show()
        compose.onNodeWithText("One last thing on Xiaomi phones").assertIsDisplayed()
        compose.onNodeWithText("Guide for Xiaomi phones").assertIsDisplayed()
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
