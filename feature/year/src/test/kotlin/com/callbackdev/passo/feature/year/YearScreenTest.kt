package com.callbackdev.passo.feature.year

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.year.YearPage
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Your year on foot, drawn from a made-up 2025 (PLANNING.md §11 Phase 12). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class YearScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: YearUiState? = YearSamples.state(), dark: Boolean = false, onBack: () -> Unit = {}) {
        compose.setContent { PassoTheme(darkTheme = dark) { YearScreen(state = state, onBack = onBack) } }
        compose.waitForIdle()
    }

    private fun next() {
        compose.onNodeWithTag(YearTags.PAGE).performTouchInput { click(centerRight) }
        compose.waitForIdle()
    }

    @Test
    fun `the year opens on its steps, as far as a way, and a touch goes on page by page`() {
        val pages = checkNotNull(YearSamples.state().review).pages
        assertThat(pages.map { it::class }).containsExactly(
            YearPage.Distance::class,
            YearPage.Months::class,
            YearPage.BestDay::class,
            YearPage.Rhythm::class,
            YearPage.Goal::class,
            YearPage.Outings::class,
            YearPage.Ways::class,
            YearPage.VersusLastYear::class,
            YearPage.Close::class,
        ).inOrder()
        show()

        compose.onNodeWithContentDescription("Page 1 of 9").assertExists()
        compose.onNodeWithContentDescription("Your 2025 on foot", substring = true).assertExists()
        snapshot("year_1")
        for (page in 2..pages.size) {
            next()
            compose.onNodeWithContentDescription("Page $page of ${pages.size}").assertExists()
            snapshot("year_$page")
        }
    }

    @Test
    fun `the best day is named with the outing that made it`() {
        show()
        repeat(2) { next() }
        compose.onNodeWithContentDescription("Your best day: Saturday, September 13", substring = true).assertExists()
        compose.onNodeWithContentDescription("With an outing: Japanese walking", substring = true).assertExists()
    }

    @Test
    fun `a touch on the leading third goes back`() {
        show()
        next()
        compose.onNodeWithContentDescription("Page 2 of 9").assertExists()
        compose.onNodeWithTag(YearTags.PAGE).performTouchInput { click(centerLeft) }
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Page 1 of 9").assertExists()
    }

    @Test
    fun `close goes back`() {
        var back = false
        show(onBack = { back = true })
        compose.onNodeWithContentDescription("Close").performClick()
        assertThat(back).isTrue()
    }

    @Test
    fun `a year with too few days says so, with no page`() {
        val thin = YearSamples.state(
            year = 2025,
            days = YearSamples.days(LocalDate.of(2025, 12, 15), LocalDate.of(2025, 12, 31)),
        )
        show(thin)
        compose.onNodeWithText("Not enough days yet", substring = true).assertIsDisplayed()
        compose.assertAccessible()
    }

    @Test
    fun `the year so far, and since when when it began late`() {
        val late = YearSamples.state(year = 2025, days = YearSamples.days(LocalDate.of(2025, 10, 1)))
        show(late)
        compose.onNodeWithContentDescription("Your 2025 on foot · Since Oct 1, 2025", substring = true).assertExists()
    }

    @Test
    fun `in the dark theme`() {
        show(dark = true)
        snapshot("year_dark_1")
        repeat(4) { next() }
        snapshot("year_dark_5")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        show()
        compose.onNodeWithContentDescription("Il tuo 2025 a piedi", substring = true).assertExists()
        next()
        compose.onNodeWithContentDescription("è stato il tuo mese migliore", substring = true).assertExists()
        snapshot("year_it_2")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, a page scrolls`() {
        show()
        repeat(4) { next() }
        compose.walkPage(hasTestTag(YearTags.PAGE), "year_large_text", maxScreens = 8)
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the pages are a column in the middle`() {
        show()
        compose.walkPage(hasTestTag(YearTags.PAGE), "year_foldable", maxScreens = 2)
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
