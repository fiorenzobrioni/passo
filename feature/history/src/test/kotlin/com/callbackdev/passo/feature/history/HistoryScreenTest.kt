package com.callbackdev.passo.feature.history

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.history.PeriodScale
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** History's pages, drawn from sample months rather than a database (PLANNING.md §11 Phase 5). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class HistoryScreenTest {
    @get:Rule val compose = createComposeRule()

    private val today = HistorySamples.today

    private fun show(
        state: HistoryUiState? = HistorySamples.state(),
        dark: Boolean = false,
        target: HistoryTarget? = null,
        walkDetection: Boolean = true,
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                HistoryScreen(
                    state = state,
                    dayDetail = { HistorySamples.detail(it, walkDetection) },
                    onOpenSettings = {},
                    target = target,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun page() = compose.onAllNodesWithTag(HistoryTags.PAGE).onFirst()

    @Test
    fun `a week opens on this week with its sentence and its bars`() {
        show()
        compose.onNodeWithText("This week").assertIsDisplayed()
        compose.onNodeWithText("so far", substring = true).assertIsDisplayed()
        compose.onNode(hasContentDescription("Thursday, September 24", substring = true)).assertExists()
        // Days still to come are drawn as nothing, and said as not counted.
        compose.onNode(hasContentDescription("Friday, September 25: not counted")).assertExists()
        snapshot("history_week")
    }

    @Test
    fun `the arrows page back, and Latest comes home`() {
        show()
        compose.onNodeWithContentDescription("Earlier").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Last week").assertIsDisplayed()
        compose.onNodeWithTag(HistoryTags.LATEST).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("This week").assertIsDisplayed()
    }

    @Test
    fun `a bar reads its day and opens it`() {
        show()
        compose.onNode(hasContentDescription("Tuesday, September 22", substring = true))
            .performSemanticsAction(SemanticsActions.OnClick)
        compose.waitForIdle()
        compose.onNodeWithText("Open day").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Tuesday, September 22").assertIsDisplayed()
        compose.onNodeWithText("Steps by hour").assertExists()
    }

    @Test
    fun `a month has its calendar, and a day of it opens`() {
        show(target = HistoryTarget(PeriodScale.MONTH, today))
        compose.onNodeWithText("September 2026").assertIsDisplayed()
        page().performScrollToNode(hasTestTag(HistoryTags.CALENDAR))
        snapshot("history_month_calendar")
        compose.onNode(
            hasContentDescription("Monday, September 14", substring = true) and
                hasAnyAncestor(hasTestTag(HistoryTags.CALENDAR)),
        )
            .performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Monday, September 14").assertIsDisplayed()
    }

    @Test
    fun `a year draws its months`() {
        show(target = HistoryTarget(PeriodScale.YEAR, today))
        compose.onNodeWithText("This year").assertIsDisplayed()
        compose.onNodeWithText("Daily average by month").assertExists()
        // Before counting began, a month is not counted.
        compose.onNode(hasContentDescription("January 2026: not counted")).assertExists()
        snapshot("history_year")
    }

    @Test
    fun `a day shows its walks on the chart and in a list`() {
        show(target = HistoryTarget(PeriodScale.DAY, today.minusDays(1)))
        compose.onNodeWithText("Yesterday").assertIsDisplayed()
        compose.onNodeWithText("in all", substring = true).assertIsDisplayed()
        snapshot("history_day")
        page().performScrollToNode(hasTestTag(HistoryTags.WALKS))
        compose.onAllNodes(hasContentDescription("Walk from", substring = true)).assertCountEquals(3)
        snapshot("history_day_walks")
    }

    @Test
    fun `with walk detection off no walk appears`() {
        show(
            state = HistorySamples.state(walkDetection = false),
            target = HistoryTarget(PeriodScale.DAY, today.minusDays(1)),
            walkDetection = false,
        )
        compose.onNodeWithText("in all", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Walks").assertDoesNotExist()
        compose.onNodeWithTag(HistoryTags.WALKS).assertDoesNotExist()
    }

    @Test
    fun `nothing recorded yet is said, with no empty chart`() {
        show(state = HistorySamples.state(days = emptyMap()))
        compose.onNodeWithTag(HistoryTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithTag(HistoryTags.CHART).assertDoesNotExist()
    }

    @Test
    fun `a week in the dark`() {
        show(dark = true)
        compose.onNodeWithText("This week").assertIsDisplayed()
        snapshot("history_week_dark")
    }

    @Test
    fun `the page stops at the first recorded week`() {
        show(state = HistorySamples.state(days = HistorySamples.days(weeks = 1)))
        compose.onNodeWithContentDescription("Earlier").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Last week").assertIsDisplayed()
        compose.onNodeWithContentDescription("Earlier").assertIsNotEnabled()
    }

    /**
     * Writes what the screen looks like to `build/screenshots`, for a person to look at: not an
     * assertion, and skipped where the graphics runtime cannot draw.
     */
    private fun snapshot(name: String) {
        compose.waitForIdle()
        runCatching {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
