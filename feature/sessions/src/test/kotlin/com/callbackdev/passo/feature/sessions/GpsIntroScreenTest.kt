package com.callbackdev.passo.feature.sessions

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.callbackdev.passo.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Passo's own words before Android asks for the location (PLANNING.md §11 Phase 15): what the
 * page says, what its three ways out do, and that it reads at a large text size, on an open
 * foldable, in Italian and in the dark.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class GpsIntroScreenTest {
    @get:Rule val compose = createComposeRule()

    private val answers = mutableListOf<String>()

    private fun show(dark: Boolean = false) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                GpsIntroScreen(
                    onContinue = { answers += "continue" },
                    onNotNow = { answers += "not now" },
                    onBack = { answers += "back" },
                )
            }
        }
    }

    @Test
    fun `it says when location is read, that it is never sent, and what it costs`() {
        show()

        compose.onNodeWithText("Measure this outing with GPS").assertIsDisplayed()
        compose.onNodeWithText("Only during an outing that asks for it").assertIsDisplayed()
        compose.onNodeWithText("Never sent").assertIsDisplayed()
        // A sentence's last two words are kept on one line, so it is matched up to them.
        compose.onNodeWithText("Passo cannot use the internet: it sends nothing", substring = true)
            .assertIsDisplayed()
        compose.onNodeWithText("More battery, only while you walk").assertIsDisplayed()
        compose.assertAccessible()
        compose.writeScreenshot("gps_intro")
    }

    @Test
    fun `it never says that nothing leaves the phone`() {
        // The receiver may fetch its own assistance data, and Android moves its own backup:
        // the words are "Passo sends nothing" (docs/adr/0007-backup.md, docs/adr/0016-location.md).
        show()

        compose.onNodeWithText("leaves the phone", substring = true, ignoreCase = true).assertDoesNotExist()
        compose.onNodeWithText("stays on the phone", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun `continue, not now and back are three different answers`() {
        show()

        compose.onNodeWithTag(GpsIntroTags.CONTINUE).performClick()
        compose.onNodeWithTag(GpsIntroTags.NOT_NOW).performClick()
        compose.onNodeWithContentDescription("Back").performClick()

        assertThat(answers).containsExactly("continue", "not now", "back").inOrder()
    }

    @Test
    fun `in the dark it reads as well`() {
        show(dark = true)

        compose.assertAccessible()
        compose.writeScreenshot("gps_intro_dark")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, every line can be reached`() {
        show()

        compose.walkPage(hasTestTag(GpsIntroTags.PAGE), "gps_intro_large_text")
        compose.onNodeWithTag(GpsIntroTags.CONTINUE).assertIsDisplayed()
        compose.onNodeWithTag(GpsIntroTags.NOT_NOW).assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable it is a column in the middle`() {
        show()

        compose.walkPage(hasTestTag(GpsIntroTags.PAGE), "gps_intro_foldable")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian it fits a small phone`() {
        show()

        compose.onNodeWithText("Misura questa uscita con il GPS").assertIsDisplayed()
        compose.onNodeWithText("Mai inviata").assertIsDisplayed()
        compose.walkPage(hasTestTag(GpsIntroTags.PAGE), "gps_intro_it")
    }
}
