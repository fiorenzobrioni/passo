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
        onOpenYear: (Int) -> Unit = {},
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                InsightsScreen(
                    state = state,
                    onOpenSettings = {},
                    onOpenPeriod = onOpen,
                    onOpenWays = onOpenWays,
                    onOpenYear = onOpenYear,
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `a running streak is the sentence and the number`() {
        show(state())
        compose.onNodeWithText("4 days in a row at your\u00A0goal").assertIsDisplayed()
        compose.onNodeWithText("Reach 8,000 steps today to make it\u00A05.").assertIsDisplayed()
        // The card under the sentence says since when, not the sentence again.
        compose.onNodeWithText("days in a row, since Sep 20").assertIsDisplayed()
        compose.onNodeWithText("days in a row at your goal").assertDoesNotExist()
        compose.onNode(hasContentDescription("Thursday, September 24: under way")).assertExists()
        snapshot("insights")
    }

    @Test
    fun `your year opens by hand, and in season it is the first card`() {
        var opened: Int? = null
        show(state().copy(offeredYears = listOf(2026, 2025), seasonYear = 2025), onOpenYear = { opened = it })

        compose.onNodeWithTag(InsightsTags.YEAR_SEASON).assertIsDisplayed()
        compose.onNodeWithText("Your 2025 on foot").assertIsDisplayed()
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.YEARS))
        compose.onNodeWithText("2026 so far").assertIsDisplayed()
        snapshot("insights_years")
        compose.onNodeWithText("2026 so far").performClick()
        assertThat(opened).isEqualTo(2026)
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
        compose.onNodeWithText("fewer than the 7", substring = true).assertExists()
        compose.onNode(hasContentDescription("Estimate, from each day’s step", substring = true))
            .assertExists()
        snapshot("insights_totals")
    }

    @Test
    fun `with no way under way, the door to the Ways closes the page`() {
        var opened = false
        show(state(), onOpenWays = { opened = true })
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.WAYS_DOOR))
        compose.onNodeWithText("Walk a way or a\u00A0city").performClick()
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
        compose.onNodeWithText("Insights start with your first\u00A0day").assertIsDisplayed()
        compose.onNodeWithTag(InsightsTags.STREAK).assertDoesNotExist()
    }

    @Test
    fun `a week in one month says the month once, and the averages stand side by side`() {
        show(state())
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.RECORDS))
        // «Sep 7 – 13», as History writes it, each date whole on its line.
        compose.onNodeWithText("67,480 steps · Sep\u00A07\u00A0– 13").assertExists()
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.AVERAGES))
        compose.onNode(hasContentDescription("last 7 days: 8,426 steps a day")).assertExists()
        compose.onNode(hasContentDescription("last 30 days: 8,747 steps a day")).assertExists()
        compose.onNode(hasContentDescription("since Jul\u00A026: 8,685 steps a day")).assertExists()
    }

    @Test
    fun `with no streak the card is a title and its line`() {
        val days = sample().mapValues { (day, summary) ->
            if (today.toEpochDay() - day in 1..2) summary.copy(steps = 4_100) else summary
        }
        show(state(days))
        compose.onNodeWithText("No streak right now").assertIsDisplayed()
        compose.onNodeWithText("A day at your goal starts\u00A0one.").assertIsDisplayed()
        snapshot("insights_no_streak")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian on a small phone the dates and the tiles keep to their lines`() {
        show(state().copy(offeredYears = listOf(2026, 2025)))
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.RECORDS))
        compose.onNodeWithText("67.480 passi · 7\u00A0– 13\u00A0set").assertExists()
        // The month in the language's own case, as the day above it is.
        compose.onNodeWithText("269.040 passi · agosto 2026").assertExists()
        compose.onNodeWithTag(InsightsTags.LIST).performScrollToNode(hasTestTag(InsightsTags.TOTALS))
        // Short beside the mark, whole for a screen reader.
        compose.onNode(hasContentDescription("Giorni all’obiettivo: 40 giorni.", substring = true)).assertExists()
        compose.walkPage(hasTestTag(InsightsTags.LIST), "insights_it")
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
