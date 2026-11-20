package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.designsystem.components.MinuteStripTags
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SessionCardTags
import com.callbackdev.passo.core.designsystem.components.StillRouteTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.routes.GpsLine
import com.callbackdev.passo.core.domain.routes.GpsLineAction
import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.domain.sessions.OutingMinutes
import com.callbackdev.passo.core.domain.sessions.OutingShape
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.MinuteSteps
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
 * The shape on an outing's card (PLANNING.md §11 Phase 21, step 21.3): in the bar's place, the
 * route so far with GPS and the minutes without, then the day's share; and past a goal, the shape
 * where the bar stood full. Each row of the step's edge cases that is the card's says so in its
 * name.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class OutingShapeCardTest {
    @get:Rule val compose = createComposeRule()

    private val plans = SessionPlans.PRESETS.mapIndexed { i, plan -> plan.copy(id = i + 1L) }

    /** Twenty-six minutes of an open walk, four of them paused, 2,820 steps. */
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
            movingMillis = 25 * MINUTE,
            zoneMillis = 25 * MINUTE,
            distanceMeters = 2_100.0,
            activeKcal = 96.0,
        ),
        lastStepAtMillis = 26 * MINUTE,
        lastEventAtMillis = 26 * MINUTE,
        pauses = listOf(SessionPause(10 * MINUTE, 14 * MINUTE)),
        pausesKept = true,
    )

    // Each minute's steps, a walk that warms up, waits at a pause, and goes on brisk.
    private val walking = listOf(
        84, 96, 104, 108, 112, 110, 106, 102, 98, 104, 40, 0, 0, 30,
        108, 114, 118, 116, 112, 96, 88, 102, 110, 112, 108, 104,
    )

    private fun minutes(session: Session, now: Long = 26 * MINUTE, steps: List<Int> = walking) = checkNotNull(
        OutingMinutes.of(session, steps.mapIndexed { i, s -> MinuteSteps(i.toLong(), 0, s) }, nowMillis = now),
    )

    private fun shape(session: Session = open, route: Boolean = false, goal: Int = 8_000, today: Boolean = true) =
        OutingShape(
            sessionId = session.id,
            minutes = minutes(session),
            route = if (route) OutingSamples.route().shape else null,
            dayGoalSteps = goal,
            today = today,
        )

    private fun state(session: Session, shape: OutingShape?, measure: LiveMeasure = LiveMeasure()) = SessionsUiState(
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
        shape = shape,
    )

    private fun show(
        session: Session,
        shape: OutingShape?,
        dark: Boolean = false,
        measure: LiveMeasure = LiveMeasure(),
        card: SessionCardActions = SessionCardActions(onOpen = {}),
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                SessionsScreen(
                    state = state(session, shape, measure),
                    signalsBlock = GoalNotificationsBlock.NONE,
                    onBack = {},
                    onEdit = {},
                    actions = SessionsActions(card = card),
                )
            }
        }
    }

    @Test
    fun `without GPS, the strip in the bar's place, then since when, then the day's share`() {
        show(open, shape())

        compose.onNodeWithTag(MinuteStripTags.STRIP).assertIsDisplayed()
            .assertContentDescriptionEquals("25 minutes on your feet, 17 of them brisk, one pause")
        compose.onNodeWithTag(StillRouteTags.ROUTE).assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.OPEN_ROW).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.SHARE).assertTextContains("35% of today’s goal")
        compose.assertAccessible()
        snapshot("shape_card_strip")
    }

    @Test
    fun `with GPS and no fix yet, the strip, and the route in its place once it has a line`() {
        val measured = open.copy(gps = true, measuredShare = 0.0)
        var current by mutableStateOf(state(measured, shape(measured, route = false)))
        compose.setContent {
            PassoTheme {
                SessionsScreen(
                    state = current,
                    signalsBlock = GoalNotificationsBlock.NONE,
                    onBack = {},
                    onEdit = {},
                    actions = SessionsActions(card = SessionCardActions(onOpen = {})),
                )
            }
        }
        compose.onNodeWithTag(MinuteStripTags.STRIP).assertIsDisplayed()
        compose.onNodeWithTag(StillRouteTags.ROUTE).assertDoesNotExist()

        current = state(measured.copy(measuredShare = 0.95), shape(measured, route = true))
        compose.mainClock.advanceTimeBy(FADE_DONE)
        compose.onNodeWithTag(StillRouteTags.ROUTE).assertIsDisplayed()
            .assertContentDescriptionEquals("Your route so far: 2.10 km")
        compose.onNodeWithTag(MinuteStripTags.STRIP).assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.SHARE).assertIsDisplayed()
        compose.assertAccessible()
        snapshot("shape_card_route")
    }

    @Test
    fun `a touch on the route opens the outing's page`() {
        var opened = false
        val measured = open.copy(gps = true, measuredShare = 0.95)
        show(measured, shape(measured, route = true), card = SessionCardActions(onOpen = { opened = true }))
        compose.onNodeWithTag(StillRouteTags.ROUTE).performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `GPS stopped during the walk - the route as it stands, the GPS line says why`() {
        val measured = open.copy(gps = true, measuredShare = 0.9)
        show(
            measured,
            shape(measured, route = true),
            measure = LiveMeasure(line = GpsLine.STOPPED),
        )
        compose.onNodeWithTag(StillRouteTags.ROUTE).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.GPS_LINE).assertIsDisplayed()
        assertThat(GpsLine.STOPPED.action).isEqualTo(GpsLineAction.RESUME_GPS)
    }

    @Test
    fun `the goal already met before the walk - the share is the walk's own steps`() {
        // A day well past its goal: the walk's 2,820 steps are still a third of it, not «all of it».
        show(open, shape(goal = 8_000))
        compose.onNodeWithTag(SessionCardTags.SHARE).assertTextContains("35% of today’s goal")
    }

    @Test
    fun `a walk of a whole day's goal says so, by itself`() {
        show(open, shape(goal = 2_500))
        compose.onNodeWithTag(SessionCardTags.SHARE).assertTextContains("All of today’s goal, by itself")
    }

    @Test
    fun `across midnight - the share against the start day's goal, said as the day's`() {
        show(open, shape(today = false))
        compose.onNodeWithTag(SessionCardTags.SHARE).assertTextContains("35% of the day’s goal")
    }

    @Test
    fun `stopped, its shape stays on its card, with its share and no line of since when`() {
        val over = open.copy(state = SessionState.FINISHED, end = SessionEnd.STOPPED, endedAtMillis = 26 * MINUTE)
        show(over, shape(over))
        compose.onNodeWithTag(MinuteStripTags.STRIP).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.OPEN_ROW).assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.SHARE).assertIsDisplayed()
    }

    @Test
    fun `a goal outing past its goal - the shape in place of the bar, the sentence its goal's`() {
        // Thirty minutes of brisk walking, and twenty more.
        val brisk = checkNotNull(SessionPlans.start(plans[1], 0, 0, 0, 8_000)).copy(
            id = 22,
            goalKind = SessionGoalKind.TIME,
            goalValue = 30,
            totals = SessionTotals(steps = 5_600, movingMillis = 50 * MINUTE, distanceMeters = 4_100.0),
            reachedAtMillis = 30 * MINUTE,
            toldMilestones = emptySet(),
            lastStepAtMillis = 50 * MINUTE,
            lastEventAtMillis = 50 * MINUTE,
        )
        val past = OutingShape(
            sessionId = brisk.id,
            minutes = minutes(brisk, now = 50 * MINUTE, steps = List(50) { 104 + it % 9 }),
            dayGoalSteps = 8_000,
        )
        show(brisk, past)

        compose.onNodeWithTag(SessionCardTags.SENTENCE).assertTextContains("Goal reached, and 20 minutes more.")
        compose.onNodeWithTag(MinuteStripTags.STRIP).assertIsDisplayed()
            .assertContentDescriptionEquals(
                "50 minutes on your feet, 50 of them brisk, the goal reached at minute 30",
            )
        // No bar full and still: nothing on the card is a progress any more.
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.SHARE).assertDoesNotExist()
        snapshot("shape_card_past_goal")
    }

    @Test
    fun `a goal outing before its goal keeps its bar`() {
        val brisk = checkNotNull(SessionPlans.start(plans[1], 0, 0, 0, 8_000)).copy(
            id = 23,
            totals = SessionTotals(steps = 1_200, movingMillis = 11 * MINUTE, distanceMeters = 880.0),
        )
        show(brisk, null)
        compose.onNodeWithTag(MinuteStripTags.STRIP).assertDoesNotExist()
        compose.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).assertExists()
    }

    @Test
    fun `an outing from before its pauses were kept - no strip, its line as before`() {
        val old = open.copy(pausesKept = false, pauses = emptyList())
        show(old, OutingShape(sessionId = old.id, minutes = null, dayGoalSteps = 8_000))
        compose.onNodeWithTag(MinuteStripTags.STRIP).assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.OPEN_ROW).assertIsDisplayed()
    }

    @Test
    fun `in the dark, with its route`() {
        val measured = open.copy(gps = true, measuredShare = 0.95)
        show(measured, shape(measured, route = true), dark = true)
        snapshot("shape_card_route_dark")
    }

    @Test
    fun `in the dark, with its minutes`() {
        show(open, shape(), dark = true)
        snapshot("shape_card_strip_dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian on a small phone, the article apart before a vowel`() {
        show(open, shape())
        compose.onNodeWithTag(SessionCardTags.SHARE).assertTextContains("Il 35% del tuo obiettivo di oggi")
        compose.onNodeWithTag(MinuteStripTags.STRIP)
            .assertContentDescriptionEquals("25 minuti in cammino, 17 a passo svelto, una pausa")
        compose.assertAccessible()
        snapshot("shape_card_it")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian, eight percent is l’8 percent`() {
        show(open.copy(totals = open.totals.copy(steps = 700)), shape())
        compose.onNodeWithTag(SessionCardTags.SHARE).assertTextContains("L’8% del tuo obiettivo di oggi")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size the card still reads`() {
        val measured = open.copy(gps = true, measuredShare = 0.95)
        show(measured, shape(measured, route = true))
        compose.walkPage(hasTestTag(SessionsTags.LIST), "shape_card_large")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h900dp-xxhdpi")
    fun `on an open foldable the card is a column in the middle`() {
        show(open, shape())
        compose.walkPage(hasTestTag(SessionsTags.LIST), "shape_card_foldable")
    }

    private fun snapshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private companion object {
        const val MINUTE = 60_000L
        const val FADE_DONE = 1_000L
    }
}
