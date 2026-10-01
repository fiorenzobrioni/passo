package com.callbackdev.passo.feature.insights

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.domain.insights.Insights
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/** Insights, drawn from made-up days rather than a database (PLANNING.md §11 Phase 5). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class InsightsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val today = InsightsSamples.today

    private fun sample() = InsightsSamples.days()

    private fun state(days: Map<Long, DailySummary> = sample()) = InsightsSamples.state(days)

    private fun show(
        state: InsightsUiState?,
        dark: Boolean = false,
        onOpen: (PeriodScale, LocalDate) -> Unit = { _, _ -> },
        onOpenWays: () -> Unit = {},
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                InsightsScreen(state = state, onOpenSettings = {}, onOpenPeriod = onOpen, onOpenWays = onOpenWays)
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `a running streak is the sentence and the number`() {
        show(state())
        compose.onNodeWithText("4 days in a row at your goal").assertIsDisplayed()
        compose.onNodeWithText("Reach 8,000 steps today to make it 5.").assertIsDisplayed()
        compose.onNode(hasContentDescription("Thursday, September 24: under way")).assertExists()
        snapshot("insights")
    }

    @Test
    fun `a record opens its day in History`() {
        var opened: Pair<PeriodScale, LocalDate>? = null
        show(state(), onOpen = { scale, date -> opened = scale to date })
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.RECORDS))
        compose.onNodeWithText("Best day").performClick()
        assertThat(opened).isEqualTo(PeriodScale.DAY to today.minusDays(11))
        snapshot("insights_records")
    }

    @Test
    fun `averages and totals say what they rest on`() {
        show(state())
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.TOTALS))
        compose.onNodeWithText("than the 7 days before", substring = true).assertExists()
        compose.onNode(hasContentDescription("Estimate, each day with its own step length", substring = true))
            .assertExists()
        snapshot("insights_totals")
    }

    @Test
    fun `with no way under way, the door to the Ways closes the page`() {
        var opened = false
        show(state(), onOpenWays = { opened = true })
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.WAYS_DOOR))
        compose.onNodeWithText("Walk a pilgrim way").performClick()
        assertThat(opened).isTrue()
        compose.onNodeWithTag(InsightsTags.WAY).assertDoesNotExist()
        snapshot("insights_ways_door")
    }

    @Test
    fun `the way under way follows the streak`() {
        val days = sample()
        val journey = WayJourney(
            id = 1,
            way = WayId.VIA_DI_FRANCESCO,
            startEpochDay = today.minusDays(40).toEpochDay(),
            startedAtMillis = 0,
            state = WayJourneyState.ACTIVE,
            endedEpochDay = null,
            toldMeters = 0,
        )
        val progress = WayProgress.of(
            Ways.of(journey.way),
            journey,
            days.mapValues { it.value.distanceMeters },
            today.toEpochDay(),
        )
        var opened = false
        show(state(days).copy(way = progress), onOpenWays = { opened = true })
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.WAY))
        compose.onNodeWithText("Past", substring = true).assertExists()
        snapshot("insights_way")
        compose.onNodeWithTag(InsightsTags.WAY).performClick()
        compose.onNodeWithTag(InsightsTags.WAYS_DOOR).assertDoesNotExist()
    }

    @Test
    fun `nothing recorded yet is said, and nothing else is drawn`() {
        show(state(days = emptyMap()))
        compose.onNodeWithText("Insights start with your first day").assertIsDisplayed()
        compose.onNodeWithTag(InsightsTags.STREAK).assertDoesNotExist()
    }

    @Test
    fun `insights in the dark`() {
        show(state(), dark = true)
        compose.onNodeWithTag(InsightsTags.STREAK).assertIsDisplayed()
        snapshot("insights_dark")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, every insight still reads`() {
        show(state())
        compose.walkPage(hasTestTag(InsightsTags.LIST), "insights_large_text")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the insights are a column in the middle`() {
        show(state())
        compose.walkPage(hasTestTag(InsightsTags.LIST), "insights_foldable")
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
