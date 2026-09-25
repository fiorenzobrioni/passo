package com.callbackdev.passo.feature.today

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
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SessionCardTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDayCalculator
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.util.Locale

/** Today's states, drawn from a state rather than a database (PLANNING.md §11 Phase 3: UI tests). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class TodayScreenTest {
    @get:Rule val compose = createComposeRule()

    private val date = LocalDate.of(2026, 9, 24)
    private val noon = 12 * 60.0 + 30

    /** A morning walk and scattered steps, on a phone with four weeks of Thursdays behind it. */
    private val morning = buildList {
        for (m in 0 until 35) add(DayMinute(7 * 60 + 40 + m, 112))
        for (h in 9..11) for (m in 0 until 60 step 7) add(DayMinute(h * 60 + m, 35))
        for (m in 0 until 12) add(DayMinute(12 * 60 + 10 + m, 96))
    }

    private val usual = TypicalDayCalculator.typical(
        listOf(
            listOf(DayMinute(8 * 60, 1_800), DayMinute(13 * 60, 1_500), DayMinute(18 * 60, 3_000)),
            listOf(DayMinute(8 * 60, 1_200), DayMinute(12 * 60, 1_700), DayMinute(19 * 60, 2_600)),
        ),
    )

    private fun state(
        minutes: List<DayMinute> = morning,
        status: TrackingStatus = TrackingStatus.COUNTING,
        typical: Boolean = true,
        goal: Int = 8_000,
        firstDay: Boolean = false,
    ) = TodayUiState(
        date = date,
        nowMinute = noon,
        overview = TodayOverview.of(
            minutes,
            Profile(heightMeters = 1.76, weightKg = 72.0),
            goal,
            noon,
            usual.takeIf {
                typical
            },
        ),
        status = status,
        units = UnitPreference.METRIC,
        walkingStepLength = 0.73,
        firstDay = firstDay,
        celebrate = false,
    )

    private fun show(
        state: TodayUiState,
        dark: Boolean = false,
        onResume: () -> Unit = {},
        onAllow: () -> Unit = {},
        onOpenSessions: () -> Unit = {},
        sessionActions: SessionCardActions = SessionCardActions(),
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                TodayScreen(
                    state = state,
                    askInSettings = false,
                    onOpenSettings = {},
                    onAllow = onAllow,
                    onResume = onResume,
                    onCelebrated = {},
                    onOpenSessions = onOpenSessions,
                    sessionActions = { sessionActions },
                )
            }
        }
    }

    private val outing = Session(
        id = 4,
        planId = 1,
        name = null,
        goalKind = SessionGoalKind.TIME,
        goalValue = 20,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        vibrate = true,
        localEpochDay = date.toEpochDay(),
        startedAtMillis = 0,
        totals = SessionTotals(steps = 1_240, movingMillis = 12 * 60_000, zoneMillis = 11 * 60_000, distanceMeters = 905.0, activeKcal = 33.0),
    )

    @Test
    fun `counting, Today offers an outing`() {
        var opened = false
        show(state(), onOpenSessions = { opened = true })

        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(TodayTags.START_OUTING))
        compose.onNodeWithText("Start an outing").performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `an outing under way has its card, with pause and stop`() {
        var paused = false
        val live = LiveSessionState(outing, cadence = 108, canKeepGoing = false, alertsWhileScreenOff = true)
        show(state().copy(session = live), sessionActions = SessionCardActions(onPause = { paused = true }))

        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(SessionCardTags.CARD))
        compose.onNodeWithText("Past halfway: 8 min to go.").assertIsDisplayed()
        compose.onNodeWithText("Start an outing").assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.PAUSE).performClick()
        assertThat(paused).isTrue()
        snapshot("today_outing")
    }

    @Test
    fun `an outing that reached its goal can keep going, or be put away`() {
        var kept = false
        var closed = false
        val done = outing.copy(
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            startedAtMillis = 1_000L * 60 * (12 * 60),
            endedAtMillis = 1_000L * 60 * (12 * 60 + 22),
            reachedAtMillis = 1,
            totals = outing.totals.copy(movingMillis = 20 * 60_000, zoneMillis = 17 * 60_000, steps = 2_140),
        )
        val live = LiveSessionState(done, cadence = null, canKeepGoing = true, alertsWhileScreenOff = true)
        show(
            state().copy(session = live),
            dark = true,
            sessionActions = SessionCardActions(onKeepGoing = { kept = true }, onClose = { closed = true }),
        )

        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(SessionCardTags.CARD))
        compose.onNodeWithText("Goal reached: 20 min.").assertIsDisplayed()
        compose.onNodeWithText("2,140 steps · 17 of 20 min at the pace you set").assertIsDisplayed()
        snapshot("today_outing_done_dark")
        compose.onNodeWithTag(SessionCardTags.KEEP_GOING).performClick()
        compose.onNodeWithTag(SessionCardTags.CLOSE).performClick()
        assertThat(kept).isTrue()
        assertThat(closed).isTrue()
    }

    private fun us(value: Int) = String.format(Locale.US, "%,d", value)

    @Test
    fun `the ring says the count, the goal and the usual pace`() {
        val state = state()
        show(state)

        compose.onNode(
            hasContentDescription("${us(state.overview.steps)} steps of 8,000", substring = true),
        ).assertIsDisplayed()
        compose.onNode(hasContentDescription("A usual day is at", substring = true)).assertExists()
        snapshot("today_light")
    }

    @Test
    fun `the headline compares with a usual day and says what is left`() {
        val state = state()
        show(state)

        compose.onNodeWithText("steps ahead of your usual pace", substring = true).assertIsDisplayed()
        compose.onNodeWithText(toGo(state), substring = true).assertIsDisplayed()
    }

    @Test
    fun `without a usual day the headline is what is left`() {
        val state = state(typical = false)
        show(state)

        compose.onNodeWithText(toGo(state), substring = true).assertIsDisplayed()
        compose.onNodeWithText("ahead of your usual pace", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a met goal says when`() {
        show(state(goal = 5_000))

        compose.onNodeWithText("Goal reached at", substring = true).assertIsDisplayed()
        compose.onNode(hasContentDescription("Goal reached.", substring = true)).assertIsDisplayed()
    }

    @Test
    fun `a met goal in the dark`() {
        show(state(goal = 5_000), dark = true)

        compose.onNode(hasContentDescription("Goal reached.", substring = true)).assertIsDisplayed()
        snapshot("today_goal_dark")
    }

    @Test
    fun `a missing permission is stated, with the way back`() {
        var asked = false
        show(state(status = TrackingStatus.PERMISSION_NEEDED), onAllow = { asked = true })

        compose.onNodeWithText("Passo is not counting").assertIsDisplayed()
        compose.onNodeWithText("Allow").performClick()
        assertThat(asked).isTrue()
    }

    @Test
    fun `a pause is stated, with resume`() {
        var resumed = false
        show(state(status = TrackingStatus.PAUSED), onResume = { resumed = true })

        compose.onNodeWithText("Counting is paused").assertIsDisplayed()
        compose.onNodeWithText("Resume").performClick()
        assertThat(resumed).isTrue()
    }

    @Test
    fun `the first day explains why it starts from zero`() {
        show(state(firstDay = true))

        compose.onNodeWithText("Counting starts today").assertExists()
    }

    @Test
    fun `an empty day has no cadence tile and says so`() {
        show(state(minutes = emptyList(), typical = false))

        compose.onNodeWithText("No steps yet today", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(
            TodayTags.LIST,
        ).performScrollToNode(hasContentDescription("Brisk minutes", substring = true))
        compose.onNode(hasContentDescription("Average cadence", substring = true)).assertDoesNotExist()
        snapshot("today_empty")
    }

    @Test
    fun `the metrics say what they mean`() {
        show(state())

        compose.onNodeWithTag(
            TodayTags.LIST,
        ).performScrollToNode(hasContentDescription("Average cadence", substring = true))
        compose.onNode(hasContentDescription("Estimate, with a step of 73 cm", substring = true)).assertExists()
        compose.onNode(
            hasContentDescription("Today’s share of the WHO’s 150 a week: done", substring = true),
        ).assertExists()
        snapshot("today_metrics")
    }

    /**
     * Writes what the screen looks like to `build/screenshots`, for a person to look at: not an
     * assertion, and skipped where the graphics runtime cannot draw.
     */
    private fun toGo(state: TodayUiState): String {
        val left = state.overview.remaining
        return "${us(left)} steps to go: about ${TodayOverview.minutesToWalk(left)} minutes of brisk walking"
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
