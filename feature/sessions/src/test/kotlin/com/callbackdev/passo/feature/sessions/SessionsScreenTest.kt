package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.designsystem.components.SessionCardTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
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
        goalKind = plans[1].goalKind,
        goalValue = plans[1].goalValue,
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
        kept: KeptOutingsDoor? = null,
        rounds: RoundsDoor? = null,
    ) = SessionsUiState(
        plans = list,
        live = live,
        units = UnitPreference.METRIC,
        lengths = lengths,
        restOfDaySteps = restOfDay,
        status = status,
        alertsWhileScreenOff = true,
        kept = kept,
        rounds = rounds,
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

        // The open walk first (Phase 21): what it is, no estimate for a walk with no goal, and its
        // mark on the way (21.2): ten minutes, its plan not measuring with GPS.
        compose.onNodeWithText("Walk").assertIsDisplayed()
        compose.onNodeWithText("A signal every 10 minutes, with vibration").assertIsDisplayed()
        compose.onNodeWithText("No goal, at any pace").assertIsDisplayed()
        compose.onNodeWithText("Brisk walk").assertIsDisplayed()
        compose.onNodeWithText("20 min at a brisk\u00A0pace").assertIsDisplayed()
        compose.onNodeWithText("About 2,000 steps · 1.46 km, estimated").assertIsDisplayed()
        compose.onAllNodesWithText("Signals at 50% and at the goal, with vibration").assertCountEquals(3)
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.PLAN}-5"))
        compose.onNodeWithText("Japanese walking").assertIsDisplayed()
        compose.onNodeWithText("5 sets: 3 min slow, 3\u00A0min\u00A0brisk").assertIsDisplayed()
        compose.onNodeWithText("A signal at each change, with vibration").assertIsDisplayed()
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.PLAN}-4"))
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
    @Config(qualifiers = "it-rIT-w384dp-h832dp-xxhdpi")
    fun `an outing started far down the list brings its card into view`() {
        var current by mutableStateOf(state())
        val japanese = walking.copy(planId = 5, goalKind = plans[4].goalKind, goalValue = plans[4].goalValue)
        compose.setContent {
            PassoTheme {
                SessionsScreen(
                    state = current,
                    signalsBlock = GoalNotificationsBlock.NONE,
                    onBack = {},
                    onEdit = {},
                    actions = SessionsActions(start = {
                        current =
                            state(
                                live = LiveSessionState(
                                    japanese,
                                    112,
                                    canKeepGoing = false,
                                    alertsWhileScreenOff = true,
                                ),
                            )
                    }),
                )
            }
        }
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.START}-5"))
        compose.onNodeWithTag("${SessionsTags.START}-5").performClick()

        compose.onNodeWithTag(SessionCardTags.CARD).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.STOP).assertIsDisplayed()
        snapshot("sessions_started")
    }

    @Test
    fun `a card opens its editor, and the bar's button a new one`() {
        val opened = mutableListOf<Long?>()
        show(state(), onEdit = { opened += it })

        compose.onNodeWithText("20 min at a brisk\u00A0pace").performClick()
        // In the bar, so in reach wherever the list stands: nothing to scroll to.
        compose.onNodeWithContentDescription("New outing").assertIsDisplayed()
        compose.onNodeWithTag(SessionsTags.NEW).performClick()
        assertThat(opened).containsExactly(2L, null).inOrder()
    }

    @Test
    fun `each outing starts from a button that says which`() {
        show(state())

        compose.onNodeWithContentDescription("Start Brisk walk").assertIsDisplayed()
        compose.onNodeWithContentDescription("Start Run").assertIsDisplayed()
    }

    @Test
    fun `with no outing kept, the page says how to make one, there too`() {
        val opened = mutableListOf<Long?>()
        show(state(list = emptyList()), onEdit = { opened += it })

        compose.onNodeWithText("No outings kept", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(SessionsTags.NEW_FIRST).performClick()
        assertThat(opened).containsExactly(null)
        snapshot("sessions_empty")
    }

    @Test
    fun `with one under way, it is on top and nothing else starts`() {
        show(
            state(live = LiveSessionState(walking, cadence = 108, canKeepGoing = false, alertsWhileScreenOff = true)),
            dark = true,
        )

        compose.onNodeWithText("Past halfway: 8 min to go.").assertIsDisplayed()
        compose.onNodeWithText("Under way · on pace").assertIsDisplayed()
        // The numbers are three columns for the eye and the card's two sentences for TalkBack.
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("108 steps/min: on pace · 1,240 steps", substring = true)
        // How far it is and where it ends, said by the bar they stand under.
        compose.onNodeWithContentDescription("12 of 20 min", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.START}-1"))
        compose.onNodeWithTag("${SessionsTags.START}-1").assertIsNotEnabled()
        snapshot("sessions_live_dark")
    }

    private val japanese = checkNotNull(SessionPlans.start(plans[4], 0, 0, 0, 8_000)).copy(id = 10, planId = 5)

    @Test
    fun `an interval walk under way says its interval, its set and its countdown`() {
        // Four minutes and twenty seconds in, the last step long ago: the countdown is held.
        val going = japanese.copy(totals = SessionTotals(steps = 470, movingMillis = 260_000), lastStepAtMillis = 0)
        show(state(live = LiveSessionState(going, cadence = 114, canKeepGoing = false, alertsWhileScreenOff = true)))

        compose.onNodeWithText("Japanese walking").assertIsDisplayed()
        compose.onNodeWithContentDescription("Set 1 of 5", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Fast: 1:35 left.").assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("114 steps/min: on pace · 470 steps", substring = true)
        snapshot("sessions_intervals_live")
    }

    @Test
    fun `an interval walk over shows each fast interval against its pace`() {
        val splits = (0 until 10).map { index ->
            val fast = index % 2 == 1
            val cadence = when {
                index == 5 -> 94
                fast -> 108 + index
                else -> 88
            }
            IntervalSplit(
                index,
                fast,
                steps = cadence * 3,
                movingMillis = 180_000,
                zoneMillis = if (fast &&
                    cadence >= 100
                ) {
                    170_000
                } else {
                    0
                },
            )
        }
        val done = japanese.copy(
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            endedAtMillis = 31 * 60_000L,
            reachedAtMillis = 31 * 60_000L,
            totals = SessionTotals(
                steps = splits.sumOf { it.steps },
                movingMillis = 30 * 60_000L,
                zoneMillis = splits.filter { it.fast }.sumOf { it.zoneMillis },
                distanceMeters = 2_200.0,
                activeKcal = 96.0,
            ),
            lastStepAtMillis = 30 * 60_000L,
            splits = splits,
        )
        show(
            state(live = LiveSessionState(done, cadence = null, canKeepGoing = true, alertsWhileScreenOff = true)),
            dark = true,
        )

        compose.onNodeWithText("4 fast intervals of 5 at pace", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.INTERVAL_BARS).assertIsDisplayed()
        snapshot("sessions_intervals_done_dark")
    }

    @Test
    fun `the outings walked are a door under the plans, from the first one over`() {
        var opened = 0
        val today = java.time.LocalDate.of(2026, 11, 21)
        val latest = walking.copy(
            localEpochDay = today.minusDays(1).toEpochDay(),
            state = SessionState.FINISHED,
            endedAtMillis = 20 * 60_000L,
        )
        show(
            state(kept = KeptOutingsDoor(count = 48, latest = latest, route = null, today = today)),
            actions = SessionsActions(openOutings = { opened++ }),
        )

        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag(SessionsTags.KEPT))
        compose.onNodeWithText("Your outings").assertIsDisplayed()
        compose.onNodeWithText("48 outings · the latest yesterday").assertIsDisplayed()
        // Beside the cities' door, on one ground.
        compose.onNodeWithTag(SessionsTags.CITIES).assertExists()
        compose.onNodeWithTag(SessionsTags.KEPT).performClick()
        assertThat(opened).isEqualTo(1)
        snapshot("sessions_kept")
    }

    @Test
    fun `the usual rounds are a door beside the outings', from the first one known`() {
        var opened = 0
        val today = java.time.LocalDate.of(2026, 11, 21)
        val latest = walking.copy(localEpochDay = today.toEpochDay(), state = SessionState.FINISHED)
        show(
            state(
                kept = KeptOutingsDoor(count = 48, latest = latest, route = null, today = today),
                rounds = RoundsDoor(rounds = 2, walks = 14, route = null),
            ),
            actions = SessionsActions(openRounds = { opened++ }),
        )
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag(SessionsTags.ROUNDS))
        compose.onNodeWithText("Your rounds").assertIsDisplayed()
        compose.onNodeWithText("2 rounds · 14 walks").assertIsDisplayed()
        compose.onNodeWithTag(SessionsTags.ROUNDS).performClick()
        assertThat(opened).isEqualTo(1)
        snapshot("sessions_rounds")
    }

    @Test
    fun `no round known, no door to them`() {
        show(state())
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag(SessionsTags.CITIES))
        compose.onNodeWithTag(SessionsTags.ROUNDS).assertDoesNotExist()
    }

    @Test
    fun `the door says when the latest was, and none is shown before the first`() {
        val today = java.time.LocalDate.of(2026, 11, 21)
        fun latestOn(day: java.time.LocalDate) =
            walking.copy(localEpochDay = day.toEpochDay(), state = SessionState.FINISHED)
        var current by mutableStateOf(state())
        compose.setContent {
            PassoTheme {
                SessionsScreen(current, GoalNotificationsBlock.NONE, onBack = {
                }, onEdit = {}, actions = SessionsActions())
            }
        }
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag(SessionsTags.CITIES))
        compose.onNodeWithTag(SessionsTags.KEPT).assertDoesNotExist()

        current = state(kept = KeptOutingsDoor(1, latestOn(today), null, today))
        compose.onNodeWithText("One outing, today").assertExists()
        current = state(kept = KeptOutingsDoor(3, latestOn(today.minusDays(3)), null, today))
        compose.onNodeWithText("3 outings · the latest on Wednesday").assertExists()
        current = state(kept = KeptOutingsDoor(3, latestOn(java.time.LocalDate.of(2026, 11, 8)), null, today))
        compose.onNodeWithText("3 outings · the latest on Nov 8").assertExists()
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian the door elides the article where the day asks it`() {
        val today = java.time.LocalDate.of(2026, 11, 21)
        val latest = walking.copy(
            localEpochDay = java.time.LocalDate.of(2026, 11, 8).toEpochDay(),
            state = SessionState.FINISHED,
        )
        show(state(kept = KeptOutingsDoor(12, latest, null, today)))

        // The plans are «ready to start»: «Your outings» are the ones walked.
        compose.onNodeWithText("Pronte da avviare").assertIsDisplayed()
        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag(SessionsTags.KEPT))
        compose.onNodeWithText("Le tue uscite").assertIsDisplayed()
        compose.onNodeWithText("12 uscite · l’ultima l’8 nov").assertIsDisplayed()
        snapshot("sessions_kept_it")
    }

    @Test
    fun `a met day has no rest to walk`() {
        show(state(restOfDay = 0))

        compose.onNodeWithTag(SessionsTags.LIST).performScrollToNode(hasTestTag("${SessionsTags.START}-4"))
        compose.onNodeWithText("Today’s goal is met: nothing left to walk").assertIsDisplayed()
        compose.onNodeWithTag("${SessionsTags.START}-4").assertIsNotEnabled()
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

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, every outing still reads`() {
        show(state())
        compose.walkPage(hasTestTag(SessionsTags.LIST), "sessions_large_text")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the outings are a column in the middle`() {
        show(state())
        compose.walkPage(hasTestTag(SessionsTags.LIST), "sessions_foldable")
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
