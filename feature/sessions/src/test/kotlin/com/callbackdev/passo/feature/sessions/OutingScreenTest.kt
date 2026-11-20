package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.components.OutingChartTags
import com.callbackdev.passo.core.designsystem.components.RouteMapTags
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SessionCardTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.routes.GpsLine
import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionPause
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * An outing's own page, in each of its forms (PLANNING.md §11 Phase 16): with GPS, with a
 * stretch laid from the steps and with none, without GPS, with a route deleted, with never a fix.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class OutingScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun show(state: OutingUiState?, dark: Boolean = false, actions: OutingActions = OutingActions()) {
        compose.setContent {
            PassoTheme(darkTheme = dark) { OutingScreen(state = state, onBack = {}, actions = actions) }
        }
    }

    private fun page() = compose.onNodeWithTag(OutingTags.PAGE)

    @Test
    fun `an outing with GPS - the sentence first, then where it went`() {
        show(OutingSamples.state())

        compose.onNodeWithText("Brisk walk").assertIsDisplayed()
        compose.onNodeWithTag(OutingTags.SENTENCE).assertIsDisplayed()
        compose.onNodeWithText("Goal reached: 45 min.").assertIsDisplayed()
        compose.onNodeWithText("Its goal: 45 min at a brisk pace").assertIsDisplayed()
        // The map is one sentence to TalkBack: how far, which way, how many stops.
        compose.onNodeWithTag(RouteMapTags.MAP)
            .assertContentDescriptionContains("Your route: 4.26 km, from", substring = true)
        // An arcade laid from the steps, drawn dashed: the line under the map says what a dash is,
        // and nothing more.
        compose.onNodeWithText("Dashes are stretches your steps covered, with no satellite.").assertIsDisplayed()
        compose.onNodeWithText("street map", substring = true).assertDoesNotExist()
        snapshot("outing_route")
    }

    @Test
    fun `its numbers say which is a measure and which an estimate`() {
        show(OutingSamples.state())

        page().performScrollToNode(hasTestTag(OutingTags.NUMBERS))
        compose.onNode(hasContentDescription("Distance: 4.26 km. Measured by satellite. Your steps said 4.10 km."))
            .assertExists()
        compose.onNode(hasContentDescription("Pace: 11:", substring = true)).assertExists()
        compose.onNode(
            hasContentDescription("Climb: ≈", substring = true) and hasContentDescription("31 m", substring = true),
        )
            .assertExists()
        compose.onNode(
            hasContentDescription("An estimate, from the distance, the climb and your weight.", substring = true),
        ).assertExists()
        snapshot("outing_numbers")
    }

    @Test
    fun `its kilometres one by one, the fastest marked, the last saying its length`() {
        show(OutingSamples.state())

        page().performScrollToNode(hasTestTag(OutingChartTags.SPLITS))
        compose.onNodeWithText("Kilometre by kilometre").assertIsDisplayed()
        compose.onNode(hasContentDescription("Kilometre 4: 9 minutes 5", substring = true)).assertExists()
        compose.onNode(
            hasContentDescription("Kilometre 4:", substring = true) and
                hasContentDescription("The fastest.", substring = true),
        ).assertExists()
        compose.onNode(hasContentDescription("The last 0.26 km:", substring = true)).assertExists()
        snapshot("outing_splits")
    }

    @Test
    fun `how it climbed, as a shape with its words`() {
        show(OutingSamples.state())

        page().performScrollToNode(hasTestTag(OutingChartTags.PROFILE))
        compose.onNodeWithTag(OutingChartTags.PROFILE)
            .assertContentDescriptionContains("The climb over the distance: from 121 m to", substring = true)
        snapshot("outing_profile")
    }

    @Test
    fun `what the route can do - share, save, delete - and the GPX says what it holds`() {
        val asked = mutableListOf<String>()
        show(
            OutingSamples.state(),
            actions = OutingActions(
                shareImage = { asked += "image" },
                shareGpx = { asked += "gpx" },
                saveGpx = { asked += "save" },
                deleteRoute = { asked += "delete" },
            ),
        )

        page().performScrollToNode(hasTestTag(OutingTags.DELETE_ROUTE))
        compose.onNodeWithText("The file says where you walked, from the first step to the last.").assertIsDisplayed()
        snapshot("outing_route_actions")
        compose.onNodeWithTag(OutingTags.SHARE_IMAGE).performClick()
        compose.onNodeWithTag(OutingTags.SHARE_GPX).performClick()
        compose.onNodeWithTag(OutingTags.SAVE_GPX).performClick()
        // Deleting asks first, and says what stays.
        compose.onNodeWithTag(OutingTags.DELETE_ROUTE).performClick()
        compose.onNodeWithText("The outing and its numbers stay.", substring = true).assertIsDisplayed()
        assertThat(asked).containsExactly("image", "gpx", "save").inOrder()
        compose.onNodeWithTag(OutingTags.DELETE_CONFIRM).performClick()
        assertThat(asked).containsExactly("image", "gpx", "save", "delete").inOrder()
    }

    @Test
    fun `while the outing counts, the page follows its route, and offers nothing to do with it yet`() {
        val live = OutingSamples.measured.copy(
            state = SessionState.ACTIVE,
            end = null,
            endedAtMillis = null,
            reachedAtMillis = null,
        )
        show(OutingSamples.state(session = live), actions = OutingActions())

        compose.onNodeWithTag(RouteMapTags.MAP).assertExists()
        // To the page's last item: what a route can do would be there.
        val items = page().fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount
        page().performScrollToIndex(items - 1)
        for (tag in listOf(
            OutingTags.SHARE_IMAGE,
            OutingTags.SHARE_GPX,
            OutingTags.SAVE_GPX,
            OutingTags.DELETE_ROUTE,
        )) {
            compose.onNodeWithTag(tag).assertDoesNotExist()
        }
    }

    @Test
    fun `ended a moment ago and still to be taken back, its route is not offered yet`() {
        show(OutingSamples.state().copy(over = false))

        val items = page().fetchSemanticsNode().config[SemanticsProperties.CollectionInfo].rowCount
        page().performScrollToIndex(items - 1)
        compose.onNodeWithTag(OutingTags.DELETE_ROUTE).assertDoesNotExist()
        compose.onNodeWithTag(OutingTags.SHARE_IMAGE).assertDoesNotExist()
    }

    @Test
    fun `paused before any satellite answered, the page promises no route`() {
        val paused = OutingSamples.onSteps.copy(
            gps = true,
            stepsMeters = OutingSamples.onSteps.totals.distanceMeters,
            measuredShare = 0.0,
            state = SessionState.PAUSED,
            end = null,
            endedAtMillis = null,
            reachedAtMillis = null,
        )
        show(OutingSamples.state(session = paused, route = null))

        compose.onNodeWithText("No satellite has answered so far", substring = true).assertIsDisplayed()
        compose.onNodeWithText("will appear", substring = true).assertDoesNotExist()
    }

    @Test
    fun `while the outing counts and no satellite has answered, the page says the route is to come`() {
        // A quarter of an hour in, under a sky the receiver has not found yet.
        val waiting = OutingSamples.onSteps.copy(
            gps = true,
            totals = SessionTotals(
                steps = 1_820,
                movingMillis = 16 * 60_000L,
                zoneMillis = 14 * 60_000L,
                distanceMeters = 1_300.0,
                activeKcal = 48.0,
            ),
            stepsMeters = 1_300.0,
            measuredShare = 0.0,
            state = SessionState.ACTIVE,
            end = null,
            endedAtMillis = null,
            reachedAtMillis = null,
        )
        show(OutingSamples.state(session = waiting, route = null))

        compose.onNodeWithText("the route will appear here as soon as one does", substring = true).assertIsDisplayed()
        compose.onNodeWithText("never answered", substring = true).assertDoesNotExist()
        // Its distance said once, as the estimate it is.
        compose.onNode(hasContentDescription("Distance: 1.30 km. An estimate, from your steps and your step length."))
            .assertExists()
        compose.onNode(hasContentDescription("of it from your steps", substring = true)).assertDoesNotExist()
        snapshot("outing_no_fix_yet")
    }

    @Test
    fun `the image to share is the route's own card, recorded out of sight`() = runTest {
        lateinit var layer: GraphicsLayer
        compose.setContent {
            layer = rememberGraphicsLayer()
            PassoTheme {
                OutingScreen(state = OutingSamples.state(), onBack = {}, actions = OutingActions(), shareLayer = layer)
            }
        }
        // A frame drawn, as the page's is before anyone can touch «Share image»: the test's own
        // window draws only when asked for its picture.
        compose.onRoot().captureToImage()

        val image = layer.toImageBitmap()

        // Laid out at its own width (360 dp at this density), whatever the page's, with the
        // name, the sentence, the map and the numbers under one another.
        assertThat(image.width).isEqualTo(1_080)
        assertThat(image.height).isGreaterThan(image.width)
        // Something was drawn on it: the card's own ground is not the transparent of an empty layer.
        val pixels = image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, false)
        assertThat(pixels.getPixel(pixels.width / 2, 40) ushr 24).isEqualTo(0xFF)
        runCatching {
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "outing_share_card.png").outputStream().use {
                pixels.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        // And nothing of it is on the page: no second map, no second sentence for TalkBack.
        compose.onAllNodesWithTag(RouteMapTags.MAP).assertCountEquals(1)
    }

    @Test
    fun `every fix answered - no dash, and no line under the map`() {
        show(OutingSamples.state(route = OutingSamples.route(arcade = false)))

        compose.onNodeWithTag(RouteMapTags.MAP).assertIsDisplayed()
        compose.onNodeWithText("Dashes are", substring = true).assertDoesNotExist()
        snapshot("outing_route_no_dash")
    }

    @Test
    fun `in the dark`() {
        show(OutingSamples.state(), dark = true)
        snapshot("outing_route_dark")
    }

    @Test
    fun `a reader who walks in miles - mile by mile, feet climbed`() {
        show(OutingSamples.state(units = UnitPreference.IMPERIAL))

        page().performScrollToNode(hasTestTag(OutingChartTags.SPLITS))
        compose.onNodeWithText("Mile by mile").assertIsDisplayed()
        compose.onNode(hasContentDescription("Mile 1:", substring = true)).assertExists()
        compose.onNode(
            hasContentDescription("Climb: ≈", substring = true) and hasContentDescription("102 ft", substring = true),
        )
            .assertExists()
    }

    @Test
    fun `a phone with no barometer says its climb is a rough one`() {
        show(OutingSamples.state(route = OutingSamples.route(altitudes = AltitudeSource.SATELLITES)))

        compose.onNode(hasContentDescription("from the satellites alone", substring = true)).assertExists()
    }

    @Test
    fun `an outing with GPS says the step it measured at its pace`() {
        show(OutingSamples.state().copy(stepAtPace = 0.79))

        page().performScrollToNode(hasTestTag(OutingTags.STEP))
        compose.onNodeWithText("Your step at this pace: 79 cm").assertIsDisplayed()
        // Not the outing that made the proposal ready: no way to it here.
        compose.onNodeWithTag(OutingTags.STEP_READY).assertDoesNotExist()
    }

    @Test
    fun `an outing with GPS and no sample has no step line`() {
        show(OutingSamples.state().copy(stepAtPace = null))

        compose.onNodeWithTag(OutingTags.STEP).assertDoesNotExist()
    }

    @Test
    fun `the outing that made the curve ready opens the card, while the proposal waits`() {
        var opened = false
        show(
            OutingSamples.state().copy(stepAtPace = 0.79, stepReady = true),
            actions = OutingActions(openStepCard = {
                opened =
                    true
            }),
        )

        page().performScrollToNode(hasTestTag(OutingTags.STEP_READY))
        compose.onNodeWithText("Passo can now use your measured step for every estimate").assertIsDisplayed()
        snapshot("outing_step_ready")
        compose.onNodeWithTag(OutingTags.STEP_READY).performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `once the proposal is used or put away, the line to it is gone`() {
        show(OutingSamples.state().copy(stepAtPace = 0.79, stepReady = false))

        page().performScrollToNode(hasTestTag(OutingTags.STEP))
        compose.onNodeWithTag(OutingTags.STEP_READY).assertDoesNotExist()
    }

    @Test
    fun `while the outing can still be taken back, its step is not told yet`() {
        show(OutingSamples.state().copy(stepAtPace = 0.79, stepReady = true, over = false))

        compose.onNodeWithTag(OutingTags.STEP).assertDoesNotExist()
        compose.onNodeWithTag(OutingTags.STEP_READY).assertDoesNotExist()
    }

    @Test
    fun `an outing without GPS - the same page without the route's parts, and no empty box`() {
        show(OutingSamples.state(session = OutingSamples.onSteps, route = null))

        compose.onNodeWithText("Goal reached: 45 min.").assertIsDisplayed()
        compose.onNode(hasContentDescription("Distance: 4.10 km. An estimate, from your steps and your step length."))
            .assertExists()
        compose.onNodeWithTag(RouteMapTags.MAP).assertDoesNotExist()
        compose.onNodeWithTag(OutingChartTags.SPLITS).assertDoesNotExist()
        compose.onNodeWithTag(OutingChartTags.PROFILE).assertDoesNotExist()
        compose.onNodeWithTag(OutingTags.SHARE_IMAGE).assertDoesNotExist()
        compose.onNode(hasContentDescription("Pace:", substring = true)).assertDoesNotExist()
        compose.onNode(hasContentDescription("Climb:", substring = true)).assertDoesNotExist()
        compose.onNodeWithText("The satellites never answered", substring = true).assertDoesNotExist()
        snapshot("outing_no_gps")
    }

    @Test
    fun `an outing whose route was deleted - its numbers stay, the page has no map and no empty box`() {
        show(OutingSamples.state(route = null))

        compose.onNodeWithTag(RouteMapTags.MAP).assertDoesNotExist()
        compose.onNodeWithTag(OutingTags.SHARE_IMAGE).assertDoesNotExist()
        compose.onNodeWithText("The satellites never answered", substring = true).assertDoesNotExist()
        // The numbers are the outing's, not the route's: measured still, and its kilometres too.
        compose.onNode(hasContentDescription("Measured by satellite.", substring = true)).assertExists()
        page().performScrollToNode(hasTestTag(OutingChartTags.SPLITS))
        compose.onNodeWithTag(OutingChartTags.SPLITS).assertIsDisplayed()
        snapshot("outing_route_deleted")
    }

    @Test
    fun `a route with no accepted fix at all - no map, and a line saying the satellites never answered`() {
        val never = OutingSamples.onSteps.copy(gps = true, stepsMeters = 4_105.0, measuredShare = 0.0)
        show(OutingSamples.state(session = never, route = null))

        compose.onNodeWithText("The satellites never answered during this outing", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(RouteMapTags.MAP).assertDoesNotExist()
        compose.onNode(hasContentDescription("Distance: 4.10 km. An estimate, from your steps and your step length."))
            .assertExists()
        snapshot("outing_no_fix")
    }

    @Test
    fun `an interval outing shows its intervals, as its card has them`() {
        val plan = SessionPlans.PRESETS.first { it.goalKind.name == "INTERVALS" }
        val started = checkNotNull(SessionPlans.start(plan, OutingSamples.startedAt, 20_775, 0, 8_000))
        val splits = (0 until 10).map { index ->
            val fast = index % 2 == 1
            IntervalSplit(
                index,
                fast,
                steps = if (fast) 336 else 264,
                movingMillis = 180_000,
                zoneMillis = if (fast) 170_000 else 0,
            )
        }
        val done = started.copy(
            id = 14,
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            endedAtMillis = OutingSamples.startedAt + 31 * 60_000L,
            reachedAtMillis = OutingSamples.startedAt + 31 * 60_000L,
            lastStepAtMillis = OutingSamples.startedAt + 31 * 60_000L,
            totals = SessionTotals(3_000, 30 * 60_000L, 850_000, 2_200.0, 96.0),
            splits = splits,
        )
        show(OutingSamples.state(session = done, route = null))

        page().performScrollToNode(hasTestTag(SessionCardTags.INTERVAL_BARS))
        compose.onNodeWithTag(SessionCardTags.INTERVAL_BARS).assertIsDisplayed()
        snapshot("outing_intervals")
    }

    // --- The open outing, and the page of an outing that counts (Phase 21) ------------------

    private val openLive = OutingSamples.measured.copy(
        goalKind = SessionGoalKind.OPEN,
        goalValue = 0,
        intensity = SessionIntensity.FREE,
        milestones = emptySet(),
        state = SessionState.ACTIVE,
        end = null,
        endedAtMillis = null,
        reachedAtMillis = null,
    )

    @Test
    fun `an open outing under way - its sentence, no goal line, and Stop and Pause at its foot`() {
        var stopped = false
        var paused = false
        show(
            OutingSamples.state(session = openLive),
            actions = OutingActions(
                live = SessionCardActions(onStop = {
                    stopped = true
                }, onPause = { paused = true }),
            ),
        )

        compose.onNodeWithText("4.26 km measured in", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Its goal", substring = true).assertDoesNotExist()
        compose.onNodeWithTag(OutingTags.LIVE_BUTTONS).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.PAUSE).performClick()
        compose.onNodeWithTag(SessionCardTags.STOP).performClick()
        assertThat(paused).isTrue()
        assertThat(stopped).isTrue()
        snapshot("outing_open_live")
    }

    @Test
    fun `while it counts, the page asks what its card asks - the receiver's line, the question before the cap`() {
        val cap = System.currentTimeMillis() + 10 * 60_000
        val asked = openLive.copy(capAtMillis = cap, capAskedForMillis = cap)
        var keptGoing = false
        show(
            OutingSamples.state(session = asked).copy(measure = LiveMeasure(line = GpsLine.SIGNAL_LOST)),
            actions = OutingActions(live = SessionCardActions(onKeepGoing = { keptGoing = true })),
        )

        compose.onNodeWithTag(SessionCardTags.GPS_LINE).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.CAP_QUESTION).assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.CAP_KEEP_GOING).performClick()
        assertThat(keptGoing).isTrue()
    }

    @Test
    fun `paused, the page offers Resume at its foot`() {
        show(
            OutingSamples.state(
                session = openLive.copy(state = SessionState.PAUSED, pausedAtMillis = OutingSamples.startedAt),
            ),
        )
        compose.onNodeWithTag(SessionCardTags.RESUME).assertIsDisplayed()
    }

    @Test
    fun `an outing over has no buttons at its foot`() {
        show(OutingSamples.state())
        compose.onNodeWithTag(OutingTags.LIVE_BUTTONS).assertDoesNotExist()
    }

    @Test
    fun `an open outing over says its time paused, and at a brisk pace the pace it kept`() {
        val start = OutingSamples.startedAt
        val over = OutingSamples.onSteps.copy(
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            milestones = emptySet(),
            reachedAtMillis = null,
            end = SessionEnd.STOPPED,
            pauses = listOf(SessionPause(start + 10 * 60_000, start + 18 * 60_000)),
        )
        show(OutingSamples.state(session = over, route = null))

        // The time paused kept whole, as the time it lasted is: «8 minutes» never breaks.
        compose.onNodeWithText("8 minutes paused", substring = true).assertIsDisplayed()
        compose.onNodeWithText("No goal, at a brisk pace").assertIsDisplayed()
        compose.onNodeWithText("A walk of 47 minutes, about 4.1", substring = true).assertIsDisplayed()
        snapshot("outing_open_over")
    }

    @Test
    fun `an open outing at any pace says nothing of a goal at all`() {
        val over = OutingSamples.onSteps.copy(
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            intensity = SessionIntensity.FREE,
            milestones = emptySet(),
            reachedAtMillis = null,
            end = SessionEnd.STOPPED,
        )
        show(OutingSamples.state(session = over, route = null))
        compose.onNodeWithText("No goal", substring = true).assertDoesNotExist()
        compose.onNodeWithText("paused", substring = true).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `an open outing under way, in Italian on a small phone`() {
        show(OutingSamples.state(session = openLive))
        compose.onNodeWithText("misurati in", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Termina").assertIsDisplayed()
        snapshot("outing_open_live_it")
    }

    @Test
    fun `until the outing is read, a bare page`() {
        show(null)

        compose.onNodeWithTag(OutingTags.PAGE).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `in Italian`() {
        show(OutingSamples.state())

        compose.onNodeWithTag(
            RouteMapTags.MAP,
        ).assertContentDescriptionContains("Il tuo percorso: 4,26 km, da", substring = true)
        page().performScrollToNode(hasTestTag(OutingChartTags.SPLITS))
        compose.onNodeWithText("Chilometro per chilometro").assertIsDisplayed()
        snapshot("outing_route_it")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, the page still reads`() {
        show(OutingSamples.state())
        compose.walkPage(hasTestTag(OutingTags.PAGE), "outing_large_text")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the page is a column in the middle`() {
        show(OutingSamples.state())
        compose.walkPage(hasTestTag(OutingTags.PAGE), "outing_foldable")
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
