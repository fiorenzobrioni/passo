package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.tracking.GoalNotificationsBlock
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** The Outings page and its editor, drawn from a state (PLANNING.md §11 Phase 10). */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class SessionsScreenTest {
    @get:Rule val compose = createComposeRule()

    private val lengths = StepLengths(walkingMeters = 0.73, runningMeters = 0.95)
    private val plans = SessionPlans.PRESETS.mapIndexed { i, plan -> plan.copy(id = i + 1L) }

    private val walking = Session(
        id = 9,
        planId = 1,
        name = null,
        goalKind = plans[0].goalKind,
        goalValue = plans[0].goalValue,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        vibrate = true,
        localEpochDay = 0,
        startedAtMillis = 0,
        totals = SessionTotals(
            steps = 1_240,
            movingMillis = 12 * 60_000,
            zoneMillis = 11 * 60_000,
            distanceMeters = 905.0,
            activeKcal = 33.0,
        ),
    )

    private fun state(
        live: LiveSessionState? = null,
        status: SessionsStatus = SessionsStatus.READY,
        restOfDay: Int = 2_400,
        list: List<SessionPlan> = plans,
    ) = SessionsUiState(
        plans = list,
        live = live,
        units = UnitPreference.METRIC,
        lengths = lengths,
        restOfDaySteps = restOfDay,
        status = status,
        alertsWhileScreenOff = true,
    )

    private fun show(
        state: SessionsUiState,
        block: GoalNotificationsBlock = GoalNotificationsBlock.NONE,
        dark: Boolean = false,
        actions: SessionsActions = SessionsActions(),
        onEdit: (Long?) -> Unit = {},
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                SessionsScreen(state = state, signalsBlock = block, onBack = {}, onEdit = onEdit, actions = actions)
            }
        }
    }

    @Test
    fun `the presets say what they are, what they come to and their signals`() {
        show(state())

        compose.onNodeWithText("Brisk walk").assertIsDisplayed()
        compose.onNodeWithText("20 min at a brisk pace").assertIsDisplayed()
        compose.onNodeWithText("About 2,000 steps · 1.46 km, estimated").assertIsDisplayed()
        compose.onAllNodesWithText("Signals at 50% and at the goal, with vibration").assertCountEquals(3)
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.PLAN}-3"))
        compose.onNodeWithText("Finish the day").assertIsDisplayed()
        compose.onNodeWithText("2,400 steps left today · 1.75 km, estimated").assertIsDisplayed()
        snapshot("sessions")
    }

    @Test
    fun `start starts that outing`() {
        var started: Long? = null
        show(state(), actions = SessionsActions(start = { started = it }))

        compose.onNodeWithTag("${SessionsTags.START}-2").performClick()
        assertThat(started).isEqualTo(2L)
    }

    @Test
    fun `a card opens its editor, and the last button a new one`() {
        val opened = mutableListOf<Long?>()
        show(state(), onEdit = { opened += it })

        compose.onNodeWithText("20 min at a brisk pace").performClick()
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag(SessionsTags.NEW))
        compose.onNodeWithTag(SessionsTags.NEW).performClick()
        assertThat(opened).containsExactly(1L, null).inOrder()
    }

    @Test
    fun `with one under way, it is on top and nothing else starts`() {
        show(
            state(live = LiveSessionState(walking, cadence = 108, canKeepGoing = false, alertsWhileScreenOff = true)),
            dark = true,
        )

        compose.onNodeWithText("Past halfway: 8 min to go.").assertIsDisplayed()
        compose.onNodeWithText("108 steps/min: on pace · 1,240 steps").assertIsDisplayed()
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.START}-1"))
        compose.onNodeWithTag("${SessionsTags.START}-1").assertIsNotEnabled()
        snapshot("sessions_live_dark")
    }

    @Test
    fun `a met day has no rest to walk`() {
        show(state(restOfDay = 0))

        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.START}-3"))
        compose.onNodeWithText("Today’s goal is met: nothing left to walk").assertIsDisplayed()
        compose.onNodeWithTag("${SessionsTags.START}-3").assertIsNotEnabled()
    }

    @Test
    fun `a paused count and silenced signals are stated, with the way back`() {
        var resumed = false
        show(
            state(status = SessionsStatus.PAUSED),
            block = GoalNotificationsBlock.CHANNEL,
            actions = SessionsActions(resumeTracking = { resumed = true }),
        )

        compose.onNodeWithText("The outings’ signals are turned off").assertIsDisplayed()
        compose.onNodeWithText("Counting is paused").assertIsDisplayed()
        compose.onNodeWithText("Resume").performClick()
        assertThat(resumed).isTrue()
        compose.onNodeWithTag("${SessionsTags.START}-1").assertIsNotEnabled()
        snapshot("sessions_blocked")
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
