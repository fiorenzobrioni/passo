package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SessionCardTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionPause
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.callbackdev.passo.core.tracking.GoalNotificationsBlock
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The card of an open outing (PLANNING.md §11 Phase 21, step 21.1): no goal, so no bar; its
 * sentence says what it has come to, the line under it since when and its time in motion, and a
 * card that counts is a door to the outing's own page, an open outing's or any other's.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class OpenOutingCardTest {
    @get:Rule val compose = createComposeRule()

    private val plans = SessionPlans.PRESETS.mapIndexed { i, plan -> plan.copy(id = i + 1L) }

    /** Twenty-five minutes of an open walk, about 2.10 km from the steps. */
    private val open = Session(
        id = 21,
        planId = 1,
        name = null,
        goalKind = SessionGoalKind.OPEN,
        goalValue = 0,
        intensity = SessionIntensity.FREE,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = 0,
        startedAtMillis = 0,
        totals = SessionTotals(
            steps = 2_820,
            movingMillis = 25 * 60_000,
            zoneMillis = 25 * 60_000,
            distanceMeters = 2_100.0,
            activeKcal = 96.0,
        ),
        lastStepAtMillis = 26 * 60_000,
        lastEventAtMillis = 26 * 60_000,
    )

    private fun show(
        session: Session,
        measure: LiveMeasure = LiveMeasure(),
        dark: Boolean = false,
        card: SessionCardActions = SessionCardActions(),
    ) {
        val state = SessionsUiState(
            plans = plans,
            live = LiveSessionState(
                session,
                cadence = 112,
                canKeepGoing = false,
                alertsWhileScreenOff = true,
                measure = measure,
            ),
            units = UnitPreference.METRIC,
            lengths = StepLengths(walkingMeters = 0.73, runningMeters = 0.95),
            restOfDaySteps = 2_400,
            status = SessionsStatus.READY,
            alertsWhileScreenOff = true,
        )
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                SessionsScreen(
                    state = state,
                    signalsBlock = GoalNotificationsBlock.NONE,
                    onBack = {},
                    onEdit = {},
                    actions = SessionsActions(card = card),
                )
            }
        }
    }

    @Test
    fun `an open outing under way - what it has come to, since when, and no bar`() {
        show(open, card = SessionCardActions(onOpen = {}))

        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("25 minutes on your feet.")
        compose.onNodeWithTag(SessionCardTags.OPEN_ROW).assertTextContains("25 min in motion", substring = true)
        compose.onNodeWithText("of the goal", substring = true).assertDoesNotExist()
        compose.onNodeWithText("to go", substring = true).assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.STOP).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.PAUSE).assertIsDisplayed()
        compose.assertAccessible()
        snapshot("open_card_under_way")
    }

    @Test
    fun `in its first minute, everything counts from here`() {
        show(open.copy(totals = SessionTotals(steps = 40, movingMillis = 25_000)))
        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("Off you go: everything counts from here.")
    }

    @Test
    fun `measured, its sentence says the distance it measured`() {
        show(open.copy(gps = true, measuredShare = 0.96, stepsMeters = 2_050.0))
        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("2.10 km measured in 25 minutes.")
        snapshot("open_card_measured")
    }

    @Test
    fun `a run with no goal says it runs`() {
        show(open.copy(intensity = SessionIntensity.RUN))
        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("25 minutes of running.")
    }

    @Test
    fun `paused, its line says the time paused`() {
        val paused = open.copy(
            state = SessionState.PAUSED,
            pausedAtMillis = 27 * 60_000,
            pauses = listOf(SessionPause(10 * 60_000, 14 * 60_000)),
        )
        show(paused)
        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("Paused. Resume when you are ready.")
        compose.onNodeWithTag(SessionCardTags.OPEN_ROW).assertTextContains("paused", substring = true)
        compose.onNodeWithTag(SessionCardTags.RESUME).assertIsDisplayed()
    }

    @Test
    fun `see the outing opens its page while it counts`() {
        var opened = false
        show(open, card = SessionCardActions(onOpen = { opened = true }))
        compose.onNodeWithTag(SessionCardTags.OPEN).assertIsDisplayed().performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `a goal outing's card under way is a door to its page too`() {
        var opened = false
        val brisk = checkNotNull(SessionPlans.start(plans[1], 0, 0, 0, 8_000)).copy(
            id = 22,
            totals = SessionTotals(steps = 1_200, movingMillis = 11 * 60_000, distanceMeters = 880.0),
        )
        show(brisk, card = SessionCardActions(onOpen = { opened = true }))
        compose.onNodeWithTag(SessionCardTags.OPEN).performClick()
        assertThat(opened).isTrue()
        // Its bar is its own still: an open outing's line is not on it.
        compose.onNodeWithTag(SessionCardTags.OPEN_ROW).assertDoesNotExist()
    }

    @Test
    fun `stopped, it says what it came to, and its line goes`() {
        val over = open.copy(
            state = SessionState.FINISHED,
            end = SessionEnd.STOPPED,
            endedAtMillis = 41 * 60_000,
            totals = open.totals.copy(movingMillis = 41 * 60_000, distanceMeters = 3_200.0),
        )
        show(over, card = SessionCardActions(onOpen = {}))
        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("A walk of 41 minutes, about 3.20 km.")
        compose.onNodeWithTag(SessionCardTags.OPEN_ROW).assertDoesNotExist()
        snapshot("open_card_over")
    }

    @Test
    fun `ended by itself after a long stop, it says so with what it came to`() {
        val idle = open.copy(state = SessionState.FINISHED, end = SessionEnd.IDLE, endedAtMillis = 26 * 60_000)
        show(idle)
        compose.onNodeWithTag(SessionCardTags.SENTENCE)
            .assertTextContains("Ended by itself after a long stop: 25 minutes, about 2.10 km.")
    }

    @Test
    fun `in the dark`() {
        show(open, dark = true, card = SessionCardActions(onOpen = {}))
        snapshot("open_card_dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian on a small phone`() {
        show(open, card = SessionCardActions(onOpen = {}))
        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("25 minuti in cammino.")
        compose.onNodeWithText("Vedi l’uscita").assertIsDisplayed()
        compose.assertAccessible()
        snapshot("open_card_it")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size the card still reads`() {
        show(open, card = SessionCardActions(onOpen = {}))
        compose.walkPage(hasTestTag(SessionsTags.LIST), "open_card_large")
    }

    private fun snapshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
