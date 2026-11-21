package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
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
import com.callbackdev.passo.core.domain.routes.GpsLine
import com.callbackdev.passo.core.domain.routes.GpsLineAction
import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.domain.sessions.SessionConstants
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
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
 * The card of an outing measured with GPS, in every state of the receiver (PLANNING.md §11
 * Phase 15, «What the reader sees while it counts»): each has its line, and one only; the
 * distance says what it is, the pace is in words, and the cap's question is on the card too.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class MeasuredOutingCardTest {
    @get:Rule val compose = createComposeRule()

    private val plans = SessionPlans.PRESETS.mapIndexed { i, plan -> plan.copy(id = i + 1L) }

    /** Twelve minutes of a brisk walk, 1.19 km of it measured: 10:05 a kilometre. */
    private val measured = Session(
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
            steps = 1_340,
            movingMillis = 12 * 60_000,
            zoneMillis = 11 * 60_000,
            distanceMeters = 1_190.0,
            activeKcal = 44.0,
        ),
        gps = true,
        stepsMeters = 1_150.0,
        measuredShare = 0.97,
    )

    /** The same walk, counted from the steps: its plan asked for GPS, and it could not have it. */
    private val onSteps = measured.copy(
        gps = false,
        stepsMeters = null,
        measuredShare = null,
        totals = measured.totals.copy(distanceMeters = 1_150.0),
    )

    private fun show(
        session: Session,
        measure: LiveMeasure = LiveMeasure(),
        cadence: Int? = 112,
        canKeepGoing: Boolean = false,
        dark: Boolean = false,
        units: UnitPreference = UnitPreference.METRIC,
        card: SessionCardActions = SessionCardActions(),
    ) {
        val state = SessionsUiState(
            plans = plans,
            live = LiveSessionState(session, cadence, canKeepGoing, alertsWhileScreenOff = true, measure = measure),
            units = units,
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
    fun `finding the satellites - the walk does not wait, and the distance is the steps' meanwhile`() {
        val starting = measured.copy(
            totals = SessionTotals(steps = 70, movingMillis = 40_000, zoneMillis = 40_000, activeKcal = 0.0),
            stepsMeters = 51.0,
            measuredShare = 0.0,
        )
        show(starting, LiveMeasure(openMeters = 51.0, line = GpsLine.FINDING), cadence = 104)

        compose.onNodeWithText("Finding the satellites. Your steps count meanwhile.").assertIsDisplayed()
        // The stretch whose fixes are not in yet is shown, as the steps estimate it.
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("0.05 km from your steps · estimated: 0 kcal", substring = true)
        compose.onNodeWithTag(SessionCardTags.GPS_ACTION).assertDoesNotExist()
        compose.onNodeWithTag(SessionCardTags.PACE).assertDoesNotExist()
        snapshot("outing_gps_finding")
    }

    @Test
    fun `measuring - measured under the distance, the pace and the pace now in words`() {
        show(measured, LiveMeasure(paceNow = 588.0))

        // For the eye, three numbers with «measured · ≈ 44 kcal» under the distance and «Pace
        // 10:05 min/km · now 9:48 min/km» under them (the screenshot); for TalkBack, sentences.
        compose.onNodeWithTag(SessionCardTags.PACE).assertContentDescriptionContains(
            "Pace: 10 minutes 5 seconds a kilometre. Now: 9 minutes 48 seconds a kilometre",
        )
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Measured: 1.19 km · estimated: 44 kcal", substring = true)
        // Measuring has no line of its own: the word under the distance says it.
        compose.onNodeWithTag(SessionCardTags.GPS_LINE).assertDoesNotExist()
        snapshot("outing_gps_measuring")
    }

    @Test
    fun `measuring, in the dark`() {
        show(measured, LiveMeasure(paceNow = 588.0), dark = true)
        snapshot("outing_gps_measuring_dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xxhdpi")
    fun `measuring, in Italian - misurati, andatura`() {
        show(measured, LiveMeasure(paceNow = 588.0))

        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Misurati: 1,19 km · stima: 44 kcal", substring = true)
        compose.onNodeWithTag(SessionCardTags.PACE).assertContentDescriptionContains(
            "Andatura: 10 minuti e 5 secondi al chilometro. Ora: 9 minuti e 48 secondi al chilometro",
        )
        snapshot("outing_gps_measuring_it")
    }

    @Test
    fun `a reader who walks in miles reads the pace by the mile`() {
        show(measured, LiveMeasure(paceNow = 588.0), units = UnitPreference.IMPERIAL)

        compose.onNodeWithTag(SessionCardTags.PACE).assertContentDescriptionContains(
            "Pace: 16 minutes 14 seconds a mile. Now: 15 minutes 46 seconds a mile",
        )
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Measured: 0.73 mi", substring = true)
        snapshot("outing_gps_measuring_miles")
    }

    @Test
    fun `more than a tenth from the steps - the share is said, and measured is not`() {
        show(measured.copy(measuredShare = 0.7), LiveMeasure(paceNow = 588.0))

        // No pace of the whole over a distance that is not a measure; the last stretch has one.
        compose.onNodeWithTag(SessionCardTags.PACE)
            .assertContentDescriptionContains("Pace now: 9 minutes 48 seconds a kilometre")
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("1.19 km, 30% from your steps · estimated: 44 kcal", substring = true)
        snapshot("outing_gps_partly")
    }

    @Test
    fun `asleep while the reader is still - nothing said, the numbers stand`() {
        show(measured, LiveMeasure(), cadence = 0)

        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Measured: 1.19 km · estimated: 44 kcal", substring = true)
        // The pace of the whole stands; the pace now is not said of a reader who has stopped.
        compose.onNodeWithTag(SessionCardTags.PACE)
            .assertContentDescriptionContains("Pace: 10 minutes 5 seconds a kilometre")
        compose.onNodeWithTag(SessionCardTags.GPS_LINE).assertDoesNotExist()
        snapshot("outing_gps_asleep")
    }

    @Test
    fun `the signal lost - a tunnel, said in a line with nothing to touch`() {
        show(measured, LiveMeasure(openMeters = 30.0, line = GpsLine.SIGNAL_LOST))

        compose.onNodeWithText("No GPS signal here. Your steps cover this stretch.").assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.GPS_ACTION).assertDoesNotExist()
        snapshot("outing_gps_signal_lost")
    }

    @Test
    fun `location turned off on the phone - said, with the button to the system's switch`() {
        val asked = mutableListOf<GpsLineAction>()
        show(measured, LiveMeasure(line = GpsLine.LOCATION_OFF), card = SessionCardActions(onGps = { asked += it }))

        compose.onNodeWithText("Location is off on this phone. Your steps count until it is back on.")
            .assertIsDisplayed()
        compose.onNodeWithText("Turn on location").performClick()
        assertThat(asked).containsExactly(GpsLineAction.LOCATION_SETTINGS)
        snapshot("outing_gps_location_off")
    }

    @Test
    fun `GPS stopped - said, with Resume GPS`() {
        val asked = mutableListOf<GpsLineAction>()
        show(measured, LiveMeasure(line = GpsLine.STOPPED), card = SessionCardActions(onGps = { asked += it }))

        compose.onNodeWithText("GPS stopped. Your outing goes on from your steps.").assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.GPS_ACTION).performClick()
        compose.onNodeWithText("Resume GPS").assertIsDisplayed()
        assertThat(asked).containsExactly(GpsLineAction.RESUME_GPS)
        snapshot("outing_gps_stopped")
    }

    @Test
    fun `GPS stopped, in the dark`() {
        show(measured, LiveMeasure(line = GpsLine.STOPPED), dark = true)
        snapshot("outing_gps_stopped_dark")
    }

    @Test
    fun `the permission gone mid-outing - the way back asks for it, never a button that would be refused`() {
        val asked = mutableListOf<GpsLineAction>()
        show(measured, LiveMeasure(line = GpsLine.PERMISSION_GONE), card = SessionCardActions(onGps = { asked += it }))

        compose.onNodeWithText("Passo can no longer read your location. Your outing goes on from your steps.")
            .assertIsDisplayed()
        compose.onNodeWithText("Resume GPS").assertDoesNotExist()
        compose.onNodeWithText("Allow location").performClick()
        assertThat(asked).containsExactly(GpsLineAction.ALLOW_LOCATION)
        snapshot("outing_gps_permission_gone")
    }

    @Test
    fun `started on the steps, the permission refused - said once, with the way to change it`() {
        val asked = mutableListOf<GpsLineAction>()
        show(
            onSteps,
            LiveMeasure(line = GpsLine.ON_STEPS_NO_PERMISSION),
            card = SessionCardActions(onGps = { asked += it }),
        )

        compose.onNodeWithText("This outing counts from your steps", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Allow it, and your next outing is measured.", substring = true).assertIsDisplayed()
        // Its distance is an estimate, and says so, as any outing's without GPS.
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Estimated: 1.15 km · 44 kcal", substring = true)
        compose.onNodeWithTag(SessionCardTags.PACE).assertDoesNotExist()
        compose.onNodeWithText("Allow location").performClick()
        assertThat(asked).containsExactly(GpsLineAction.ALLOW_LOCATION)
        snapshot("outing_gps_on_steps_refused")
    }

    @Test
    fun `started on the steps, approximate only - the way to the precise one`() {
        val asked = mutableListOf<GpsLineAction>()
        show(
            onSteps,
            LiveMeasure(line = GpsLine.ON_STEPS_APPROXIMATE),
            card = SessionCardActions(onGps = { asked += it }),
        )

        compose.onNodeWithText("an approximate location cannot measure a walk", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Allow precise location").performClick()
        assertThat(asked).containsExactly(GpsLineAction.ALLOW_LOCATION)
        snapshot("outing_gps_on_steps_approximate")
    }

    @Test
    fun `started on the steps, location off on the phone - the button to the system's switch`() {
        val asked = mutableListOf<GpsLineAction>()
        show(
            onSteps,
            LiveMeasure(line = GpsLine.ON_STEPS_LOCATION_OFF),
            card = SessionCardActions(onGps = { asked += it }),
        )

        compose.onNodeWithText("location is off on this phone", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Turn on location").performClick()
        assertThat(asked).containsExactly(GpsLineAction.LOCATION_SETTINGS)
        snapshot("outing_gps_on_steps_location_off")
    }

    @Test
    fun `paused - the receiver is off by the reader's word, and the numbers say what they are`() {
        val paused = measured.copy(state = SessionState.PAUSED, pausedAtMillis = 13 * 60_000L)
        show(paused, cadence = null)

        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Measured: 1.19 km · estimated: 44 kcal", substring = true)
        compose.onNodeWithTag(SessionCardTags.PACE)
            .assertContentDescriptionContains("Pace: 10 minutes 5 seconds a kilometre")
        compose.onNodeWithTag(SessionCardTags.GPS_LINE).assertDoesNotExist()
        snapshot("outing_gps_paused")
    }

    @Test
    fun `over - the measured distance, its pace, and Keep going while it can be taken back`() {
        val done = measured.copy(
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            endedAtMillis = 22 * 60_000L,
            reachedAtMillis = 22 * 60_000L,
            lastStepAtMillis = 22 * 60_000L,
            totals = SessionTotals(
                steps = 2_240,
                movingMillis = 20 * 60_000,
                zoneMillis = 19 * 60_000,
                distanceMeters = 1_985.0,
                activeKcal = 73.0,
            ),
            stepsMeters = 1_910.0,
            measuredShare = 0.98,
        )
        show(done, cadence = null, canKeepGoing = true)

        compose.onNodeWithTag(SessionCardTags.PACE)
            .assertContentDescriptionContains("Pace: 10 minutes 5 seconds a kilometre")
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Measured: 1.98 km · estimated: 73 kcal", substring = true)
        compose.onNodeWithTag(SessionCardTags.KEEP_GOING).assertIsDisplayed()
        snapshot("outing_gps_done")
    }

    /** An outing ten minutes from the longest it stays open, the question asked and not answered. */
    private fun nearItsCap(session: Session): Session {
        val startedAt = System.currentTimeMillis() - SessionConstants.MAX_SESSION_MILLIS + 10 * 60_000L - 30_000L
        val cap = startedAt + SessionConstants.MAX_SESSION_MILLIS
        return session.copy(startedAtMillis = startedAt, capAtMillis = cap, capAskedForMillis = cap)
    }

    @Test
    fun `before the longest outing ends, the card asks too - Still walking, with Keep going`() {
        var kept = 0
        show(nearItsCap(measured), LiveMeasure(paceNow = 588.0), card = SessionCardActions(onKeepGoing = { kept++ }))

        compose.onNodeWithText("Still walking? This outing ends in 10 minutes.").assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.CAP_KEEP_GOING).performClick()
        assertThat(kept).isEqualTo(1)
        snapshot("outing_cap_question")
    }

    @Test
    fun `an outing without GPS is asked the same question`() {
        show(nearItsCap(onSteps), dark = true)

        compose.onNodeWithText("Still walking? This outing ends in 10 minutes.").assertIsDisplayed()
        compose.onNodeWithTag(SessionCardTags.STATS)
            .assertContentDescriptionContains("Estimated: 1.15 km · 44 kcal", substring = true)
        snapshot("outing_cap_question_dark")
    }

    @Test
    fun `an outing not near its cap is asked nothing`() {
        show(measured.copy(startedAtMillis = System.currentTimeMillis() - 60 * 60_000L))

        compose.onNodeWithTag(SessionCardTags.CAP_QUESTION).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size, a stopped receiver and the question still read and can be touched`() {
        show(nearItsCap(measured), LiveMeasure(line = GpsLine.STOPPED))
        compose.walkPage(hasTestTag(SessionsTags.LIST), "outing_gps_large_text")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size in Italian, the longest line still reads`() {
        show(onSteps, LiveMeasure(line = GpsLine.ON_STEPS_APPROXIMATE))
        compose.walkPage(hasTestTag(SessionsTags.LIST), "outing_gps_large_text_it")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the measured outing's card is in the page's column`() {
        show(measured, LiveMeasure(paceNow = 588.0, line = GpsLine.SIGNAL_LOST))
        compose.walkPage(hasTestTag(SessionsTags.LIST), "outing_gps_foldable")
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
