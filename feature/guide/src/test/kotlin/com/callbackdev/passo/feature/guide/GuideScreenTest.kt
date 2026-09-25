package com.callbackdev.passo.feature.guide

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
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
import java.time.DayOfWeek

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class GuideScreenTest {
    @get:Rule val compose = createComposeRule()

    private val metric = GuideUiState(UnitPreference.METRIC, DayOfWeek.MONDAY)

    private fun show(state: GuideUiState? = metric, dark: Boolean = false, onBack: () -> Unit = {}) {
        compose.setContent { PassoTheme(darkTheme = dark) { GuideScreen(state = state, onBack = onBack) } }
    }

    @Test
    fun `the guide opens on what Passo does, then the map of the screens`() {
        var back = false
        show(onBack = { back = true })

        compose.onNodeWithText("The guide").assertIsDisplayed()
        compose.onNodeWithText("Passo counts your steps", substring = true).assertIsDisplayed()
        compose.onNodeWithText("The three screens").assertIsDisplayed()
        snapshot("guide_top")
        compose.onNodeWithContentDescription("Back").performClick()
        assertThat(back).isTrue()
    }

    @Test
    fun `every chapter is there, each with what it answers`() {
        show()

        // One line each chapter alone has: a chapter's title can share its name with a tab.
        val chapters = listOf(
            "The notification that stays",
            "The sentence",
            "Time in motion",
            "Every day keeps its goal",
            "The streak waits for midnight",
            "Two widgets",
            "Where the numbers come from",
        )
        chapters.forEachIndexed { index, line ->
            compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasText(line, substring = true))
            compose.onNodeWithText(line, substring = true).assertIsDisplayed()
            snapshot("guide_${index + 1}")
        }
    }

    @Test
    fun `the examples are drawn by the app's own components, in the reader's units`() {
        show(GuideUiState(UnitPreference.IMPERIAL, null))

        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(GuideTags.RING))
        compose.onNodeWithText("of 8,000 steps").assertIsDisplayed()
        // A tile reads as one sentence: its label, its value and what it means.
        compose.onNodeWithTag(GuideTags.CONTENT)
            .performScrollToNode(hasContentDescription("Brisk minutes", substring = true))
        // 4,960 steps of 70 cm: 3,472 m, 2.15 miles.
        compose.onNodeWithContentDescription("Distance: 2.15 mi", substring = true).assertExists()
        compose.onNodeWithContentDescription("8 more for today’s share of the WHO’s 150 a week", substring = true)
            .assertExists()
        snapshot("guide_samples_imperial")
    }

    @Test
    fun `a bar of the example week reads when touched`() {
        show()

        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(GuideTags.WEEK))
        compose.onNodeWithText("Touch a bar to read it").assertIsDisplayed()
        // The fourth of seven bars, Thursday on a Monday week: 10,240 steps, over its 10,000.
        compose.onNodeWithTag(GuideTags.WEEK).performTouchInput {
            click(Offset(width * 3.5f / 7f, height / 2f))
        }
        compose.onNodeWithText("10,240 steps, goal met", substring = true).assertIsDisplayed()
        snapshot("guide_week")
    }

    @Test
    fun `nothing is drawn before the settings are read`() {
        show(state = null)

        compose.onNodeWithText("The guide").assertIsDisplayed()
        compose.onNodeWithTag(GuideTags.CONTENT).assertDoesNotExist()
    }

    @Test
    fun `the guide reads in the dark`() {
        show(dark = true)

        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(GuideTags.RING))
        snapshot("guide_dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `the guide speaks Italian`() {
        show()

        compose.onNodeWithText("La guida").assertIsDisplayed()
        compose.onNodeWithText("Le tre schermate").assertIsDisplayed()
        compose.onNodeWithTag(GuideTags.CONTENT).performScrollToNode(hasTestTag(GuideTags.RING))
        compose.onNodeWithText("su 8.000 passi").assertIsDisplayed()
        snapshot("guide_it")
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
