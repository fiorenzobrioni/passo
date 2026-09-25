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
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.UnitPreference
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

    private val today = LocalDate.of(2026, 9, 24)

    private fun day(date: LocalDate, steps: Int, goal: Int = 8_000) = DailySummary(
        localEpochDay = date.toEpochDay(),
        steps = steps,
        distanceMeters = steps * 0.74,
        activeKcal = steps * 0.037,
        activeMinutes = steps / 180,
        briskMinutes = steps / 450,
        goalSteps = goal,
        finalized = date.isBefore(today),
    )

    /** Sixty days: mostly around the goal, a streak running into today, one big Sunday. */
    private fun sample(): Map<Long, DailySummary> = (0..60).associate { back ->
        val date = today.minusDays(back.toLong())
        val steps = when {
            back == 0 -> 5_300
            back in 1..4 -> 8_400 + back * 310
            back == 11 -> 16_920
            back % 3 == 0 || back == 5 -> 6_100 + back * 20
            else -> 8_900 + (back * 137) % 1_500
        }
        date.toEpochDay() to day(date, steps)
    }

    private fun state(days: Map<Long, DailySummary> = sample()): InsightsUiState {
        val insights = Insights.of(days, today, DayOfWeek.MONDAY)
        return InsightsUiState(
            today = today,
            insights = insights,
            lastDays = (6 downTo 0).map { back ->
                val date = today.minusDays(back.toLong())
                DayMark(
                    date = date,
                    counted = insights.firstDay?.let { !date.isBefore(it) } == true,
                    met = days[date.toEpochDay()]?.goalReached == true,
                    today = back == 0,
                )
            },
            goalSteps = 8_000,
            units = UnitPreference.METRIC,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )
    }

    private fun show(
        state: InsightsUiState?,
        dark: Boolean = false,
        onOpen: (PeriodScale, LocalDate) -> Unit = { _, _ -> },
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                InsightsScreen(state = state, onOpenSettings = {}, onOpenPeriod = onOpen)
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

    private fun snapshot(name: String) {
        compose.waitForIdle()
        runCatching {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
