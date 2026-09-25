package com.callbackdev.passo.feature.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.calibration.CalibratedStep
import com.callbackdev.passo.core.domain.calibration.CalibrationResult
import com.callbackdev.passo.core.domain.calibration.StepCalibration
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.feature.settings.calibration.CalibrationActions
import com.callbackdev.passo.feature.settings.calibration.CalibrationPhase
import com.callbackdev.passo.feature.settings.calibration.CalibrationScreen
import com.callbackdev.passo.feature.settings.calibration.CalibrationTags
import com.callbackdev.passo.feature.settings.calibration.CalibrationUiState
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The step calibration (PLANNING.md §11 Phase 7), moment by moment. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class CalibrationScreenTest {
    @get:Rule val compose = createComposeRule()

    private val setup = CalibrationUiState(
        step = CalibratedStep.WALKING,
        distanceMeters = 100.0,
        units = UnitPreference.METRIC,
        phase = CalibrationPhase.SETUP,
        permission = true,
        counterReady = true,
        steps = 0,
        elapsedMillis = 0,
        result = null,
        profile = Profile(heightMeters = 1.76),
    )

    private fun show(
        state: CalibrationUiState,
        actions: CalibrationActions = CalibrationActions(),
        onBack: () -> Unit = {
        },
    ) {
        compose.setContent { PassoTheme { CalibrationScreen(state, onBack = onBack, actions = actions) } }
    }

    @Test
    fun `the start line says what is measured, over what, and how`() {
        var started = false
        var distance = 0.0
        show(setup, CalibrationActions(start = { started = true }, setDistance = { distance = it }))

        compose.onNodeWithText("Now · 73 cm, estimated from your height").assertIsDisplayed()
        compose.onNodeWithText("100 m").assertIsDisplayed()
        compose.onNodeWithText("Stand at the start and tap Start.").assertIsDisplayed()
        snapshot("calibration_setup")
        compose.onNodeWithContentDescription("More").performClick()
        assertThat(distance).isWithin(1e-9).of(110.0)
        compose.onNodeWithTag(CalibrationTags.START).performClick()
        assertThat(started).isTrue()
    }

    @Test
    fun `Start waits for the step counter's first answer`() {
        show(setup.copy(counterReady = false))

        compose.onNodeWithText("Waiting for the step counter…").assertIsDisplayed()
        compose.onNodeWithTag(CalibrationTags.START).assertIsNotEnabled()
    }

    @Test
    fun `without the permission, the card asks for it where it is needed`() {
        show(setup.copy(permission = false, counterReady = false))

        compose.onNodeWithText("Passo can’t read the step counter").assertIsDisplayed()
        compose.onNodeWithText("Allow").assertIsDisplayed()
    }

    @Test
    fun `imperial readers pick the distance in yards`() {
        show(setup.copy(units = UnitPreference.IMPERIAL, distanceMeters = 91.44))

        compose.onNodeWithText("100 yd").assertIsDisplayed()
        compose.onNodeWithText("A running track is 437 yd around its inside lane", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the walk shows its steps and its time, and Stop ends it`() {
        var stopped = false
        show(
            setup.copy(phase = CalibrationPhase.WALKING, steps = 128, elapsedMillis = 92_000),
            CalibrationActions(stop = { stopped = true }),
        )

        compose.onNodeWithText("Walking 100 m").assertIsDisplayed()
        compose.onNodeWithText("128").assertIsDisplayed()
        compose.onNodeWithText("1:32").assertIsDisplayed()
        snapshot("calibration_walking")
        compose.onNodeWithTag(CalibrationTags.STOP).assertIsEnabled().performClick()
        assertThat(stopped).isTrue()
    }

    @Test
    fun `leaving during the walk asks first`() {
        var left = false
        show(setup.copy(phase = CalibrationPhase.WALKING, steps = 40), onBack = { left = true })

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Leave without saving?").assertIsDisplayed()
        assertThat(left).isFalse()
        compose.onNodeWithText("Leave").performClick()
        assertThat(left).isTrue()
    }

    @Test
    fun `the result says the length, what it came from and what it changes`() {
        var saved = false
        val result = StepCalibration.measure(CalibratedStep.WALKING, 100.0, 0, 131, 77_000)
        show(
            setup.copy(phase = CalibrationPhase.RESULT, result = result),
            CalibrationActions(save = { saved = true }),
        )

        compose.onNodeWithText("Your walking step").assertIsDisplayed()
        compose.onNodeWithText("76 cm").assertIsDisplayed()
        compose.onNodeWithText("131 steps over 100 m, at 102 steps/min.").assertIsDisplayed()
        compose.onNodeWithText("73 cm, estimated from your height").assertIsDisplayed()
        compose.onNodeWithText("Distances read 4% longer").assertIsDisplayed()
        snapshot("calibration_result")
        compose.onNodeWithTag(CalibrationTags.SAVE).performClick()
        assertThat(saved).isTrue()
    }

    @Test
    fun `a walking step measured at a run says so, and can still be kept`() {
        val result = StepCalibration.measure(CalibratedStep.WALKING, 100.0, 0, 100, 40_000)
        show(setup.copy(phase = CalibrationPhase.RESULT, result = result))

        compose.onNodeWithText("That was a running pace").assertIsDisplayed()
        compose.onNodeWithTag(CalibrationTags.SAVE).assertIsDisplayed()
    }

    @Test
    fun `too few steps is said with the way to try again, and nothing to save`() {
        var again = false
        show(
            setup.copy(phase = CalibrationPhase.RESULT, result = CalibrationResult.TooFewSteps(12)),
            CalibrationActions(again = { again = true }),
        )

        compose.onNodeWithText("Too few steps to measure").assertIsDisplayed()
        compose.onNodeWithText("Only 12 steps were counted", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(CalibrationTags.SAVE).assertDoesNotExist()
        snapshot("calibration_too_few")
        compose.onNodeWithTag(CalibrationTags.AGAIN).performClick()
        assertThat(again).isTrue()
    }

    @Test
    fun `a length no one walks with is refused, with the arithmetic`() {
        show(setup.copy(phase = CalibrationPhase.RESULT, result = CalibrationResult.Implausible(5.0, 20)))

        compose.onNodeWithText(
            "100 m in 20 steps would be 500 cm a step. Check the distance you set, and walk all of it.",
        ).assertIsDisplayed()
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
