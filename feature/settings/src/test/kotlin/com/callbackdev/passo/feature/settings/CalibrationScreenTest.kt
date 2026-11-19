package com.callbackdev.passo.feature.settings

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.components.STEP_CURVE_TAG
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.calibration.CalibratedStep
import com.callbackdev.passo.core.domain.calibration.CalibrationResult
import com.callbackdev.passo.core.domain.calibration.LearnedSteps
import com.callbackdev.passo.core.domain.calibration.StepCalibration
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.callbackdev.passo.feature.settings.calibration.CalibrationActions
import com.callbackdev.passo.feature.settings.calibration.CalibrationPhase
import com.callbackdev.passo.feature.settings.calibration.CalibrationScreen
import com.callbackdev.passo.feature.settings.calibration.CalibrationTags
import com.callbackdev.passo.feature.settings.calibration.CalibrationUiState
import com.callbackdev.passo.feature.settings.learning.StepLearningTags
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
        dark: Boolean = false,
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) { CalibrationScreen(state, onBack = onBack, actions = actions) }
        }
    }

    private val proposed = setup.copy(learning = LearningFixtures.proposed(setup.profile))
    private val putAway = setup.copy(learning = LearningFixtures.proposed(setup.profile).copy(proposed = false))

    @Test
    fun `a proposal waiting opens the page with its card, the sentence first`() {
        var used: LearnedStep? = null
        var notNow: LearnedStep? = null
        show(proposed, CalibrationActions(useStep = { used = it }, notNow = { notNow = it }))

        compose.onNodeWithTag(StepLearningTags.SENTENCE).assertTextEquals(
            "Passo has measured your step on 14.2\u00A0km of your outings: " +
                "at your usual pace it is 78\u00A0cm, not 73\u00A0cm, " +
                "so distances and calories read 6% higher.",
        )
        compose.onNodeWithTag(STEP_CURVE_TAG).assertExists()
        snapshot("calibration_learning_card")
        compose.onNodeWithTag(StepLearningTags.USE).performScrollTo().performClick()
        assertThat(used).isEqualTo(LearningFixtures.curve)
        compose.onNodeWithTag(StepLearningTags.NOT_NOW).performScrollTo().performClick()
        assertThat(notNow).isEqualTo(LearningFixtures.curve)
    }

    @Test
    fun `the card in the dark`() {
        show(proposed, dark = true)
        snapshot("calibration_learning_card_dark")
    }

    @Test
    fun `not now leaves a quiet row, and touching it opens the card again`() {
        var opened = false
        show(putAway, CalibrationActions(openCard = { opened = true }))

        compose.onNodeWithTag(StepLearningTags.CARD).assertDoesNotExist()
        compose.onNodeWithText(
            "Passo has learnt your step on 14.2\u00A0km of your outings with GPS",
        ).assertIsDisplayed()
        snapshot("calibration_learning_row")
        compose.onNodeWithTag(StepLearningTags.ROW).performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `the card asked for opens with the curve as it is today`() {
        show(putAway.copy(cardOpen = true))

        compose.onNodeWithTag(StepLearningTags.CARD).assertExists()
        compose.onNodeWithTag(StepLearningTags.ROW).assertDoesNotExist()
    }

    @Test
    fun `with no curve that counts there is no row and no card`() {
        show(setup)

        compose.onNodeWithTag(StepLearningTags.ROW).assertDoesNotExist()
        compose.onNodeWithTag(StepLearningTags.CARD).assertDoesNotExist()
    }

    @Test
    fun `use it says what changed, and the past days are offered, asked first`() {
        var applied = false
        show(proposed.copy(stepUsed = true), CalibrationActions(applyPast = { applied = true }))

        compose.onNodeWithText(
            "From today, distances use your measured step. Past days keep theirs.",
        ).assertIsDisplayed()
        snapshot("calibration_learning_used")
        compose.onNodeWithTag(StepLearningTags.APPLY_PAST).performClick()
        assertThat(applied).isFalse()
        compose.onNodeWithText("Recompute").performClick()
        assertThat(applied).isTrue()
    }

    @Test
    fun `the learnt step in use says where it comes from, with the way back named as it was`() {
        var back = false
        val measured =
            Profile(heightMeters = 1.76, stepLengthMode = StepLengthMode.CALIBRATED, walkingStepLengthMeters = 0.76)
        val learned = LearnedSteps.accept(measured, LearningFixtures.curve)
        show(
            setup.copy(profile = learned, learning = LearningFixtures.proposed(learned).copy(proposed = false)),
            CalibrationActions(goBackStep = { back = true }),
        )

        compose.onNodeWithText("Now · 78 cm, learnt from 14.2 km of your outings").assertIsDisplayed()
        // A curve in use has no quiet row: it is the step in use.
        compose.onNodeWithTag(StepLearningTags.ROW).assertDoesNotExist()
        snapshot("calibration_learning_in_use")
        compose.onNodeWithText("Go back to your measured step (76 cm)").performClick()
        assertThat(back).isTrue()
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size the card still reads`() {
        show(proposed)
        compose.walkPage(hasTestTag(CalibrationTags.PAGE), "calibration_learning_large_text")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian on a small phone the card reads`() {
        show(proposed)
        compose.walkPage(hasTestTag(CalibrationTags.PAGE), "calibration_learning_it")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the card is in the middle column`() {
        show(proposed)
        compose.walkPage(hasTestTag(CalibrationTags.PAGE), "calibration_learning_foldable")
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

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, the measure still reads`() {
        show(setup)
        compose.walkPage(hasTestTag(CalibrationTags.PAGE), "calibration_large_text")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the measure is a column in the middle`() {
        show(setup)
        compose.walkPage(hasTestTag(CalibrationTags.PAGE), "calibration_foldable")
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        compose.assertAccessible()
        runCatching {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
