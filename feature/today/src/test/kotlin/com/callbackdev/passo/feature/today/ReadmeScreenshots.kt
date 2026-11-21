package com.callbackdev.passo.feature.today

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.designsystem.components.SessionCardActions
import com.callbackdev.passo.core.designsystem.components.SessionCardTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.insights.StreakSoFar
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.sessions.OutingMinutes
import com.callbackdev.passo.core.domain.sessions.OutingShape
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDayCalculator
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.RoundLine
import com.callbackdev.passo.core.model.RoundStanding
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The README's pictures of Today (docs/screenshots), drawn from realistic sample days. Run with
 * `./gradlew :feature:today:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("passo.readmeScreenshots")
    private val profile = Profile(heightMeters = 1.78, weightKg = 74.0)
    private val usual = TypicalDayCalculator.typical(SampleDays.pastThursdays)

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    /** Twelve days in a row at the goal up to yesterday: today joins the streak once it is met. */
    private fun state(nowMinute: Int): TodayUiState {
        val overview = TodayOverview.of(
            minutes = SampleDays.workday(seed = 5, lunchWalkMinutes = 36, until = nowMinute),
            profile = profile,
            goalSteps = 10_000,
            nowMinute = nowMinute.toDouble(),
            typical = usual,
        )
        return TodayUiState(
            date = LocalDate.of(2026, 9, 24),
            nowMinute = nowMinute.toDouble(),
            overview = overview,
            status = TrackingStatus.COUNTING,
            units = UnitPreference.METRIC,
            walkingStepLength = StepLengths.of(profile).walkingMeters,
            firstDay = false,
            celebrate = false,
            streakDays = StreakSoFar.withToday(12, overview.goalReachedAt != null),
        )
    }

    private fun show(state: TodayUiState, dark: Boolean = false) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                TodayScreen(
                    state,
                    askInSettings = false,
                    onOpenSettings = {},
                    onAllow = {},
                    onResume = {},
                    onCelebrated = {},
                    // As the app gives it: a card under way is a door to its outing's page.
                    sessionActions = { SessionCardActions(onOpen = {}) },
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun today() {
        show(state(nowMinute = 15 * 60 + 40))
        save("today")
    }

    @Test
    fun todayGoalReachedDark() {
        show(state(nowMinute = 19 * 60 + 25), dark = true)
        save("today-goal-dark")
    }

    /** A brisk walk after work, twelve minutes in, as Today shows it while it lasts. */
    @Test
    fun todayOuting() {
        val walk = Session(
            id = 1,
            planId = 1,
            name = null,
            goalKind = SessionGoalKind.TIME,
            goalValue = 20,
            intensity = SessionIntensity.BRISK,
            milestones = setOf(SessionMilestone.HALF),
            vibrate = true,
            localEpochDay = 0,
            startedAtMillis = 0,
            totals = SessionTotals(
                steps = 1_312,
                movingMillis = 12 * 60_000L + 20_000,
                zoneMillis = 11 * 60_000L,
                distanceMeters = 1_312 * StepLengths.of(profile).walkingMeters,
                activeKcal = 51.0,
            ),
        )
        val state = state(nowMinute = 18 * 60 + 14).copy(
            session = LiveSessionState(walk, cadence = 112, canKeepGoing = false, alertsWhileScreenOff = true),
        )
        show(state)
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(SessionCardTags.CARD))
        compose.waitForIdle()
        save("today-outing")
    }

    /**
     * A walk with no goal round a city park, measured with GPS, looked at halfway (PLANNING.md §11
     * Phase 21, step 21.3): its route so far on its card, since when, and its share of the day.
     */
    @Test
    fun todayOpenOuting() {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.of(2026, 9, 24, 10, 12).atZone(zone).toInstant().toEpochMilli()
        val seconds = 27 * 60
        val points = parkWalk(start / 1_000, seconds)
        val walk = Session(
            id = 1,
            planId = 1,
            name = null,
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            intensity = SessionIntensity.FREE,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = LocalDate.of(2026, 9, 24).toEpochDay(),
            startedAtMillis = start,
            totals = SessionTotals(
                steps = 3_040,
                movingMillis = seconds * 1_000L,
                zoneMillis = seconds * 1_000L,
                distanceMeters = 2_270.0,
                activeKcal = 118.0,
            ),
            lastStepAtMillis = start + seconds * 1_000L,
            lastEventAtMillis = start + seconds * 1_000L,
            gps = true,
            stepsMeters = 2_190.0,
            measuredShare = 0.97,
            pausesKept = true,
        )
        val minutes = (0 until 28).map { MinuteSteps(start / 60_000 + it, walk.localEpochDay, 104 + (it * 7) % 15) }
        val state = state(nowMinute = 10 * 60 + 40).copy(
            session = LiveSessionState(walk, cadence = 114, canKeepGoing = false, alertsWhileScreenOff = true),
            sessionShape = OutingShape(
                sessionId = walk.id,
                minutes = OutingMinutes.of(walk, minutes, nowMillis = walk.lastEventAtMillis),
                route = RouteShape.of(points),
                dayGoalSteps = 10_000,
            ),
        )
        show(state)
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(SessionCardTags.CARD))
        compose.waitForIdle()
        save("outing-open")
    }

    /**
     * The same walk round the park, the reader's usual round (PLANNING.md §11 Phase 20): past its
     * second kilometre, the card says how far ahead of the usual it is, in the day's own words.
     */
    @Test
    fun todayRoundOuting() {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.of(2026, 9, 24, 10, 12).atZone(zone).toInstant().toEpochMilli()
        val seconds = 27 * 60
        val points = parkWalk(start / 1_000, seconds)
        val walk = Session(
            id = 1,
            planId = 1,
            name = null,
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            intensity = SessionIntensity.FREE,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = LocalDate.of(2026, 9, 24).toEpochDay(),
            startedAtMillis = start,
            totals = SessionTotals(
                steps = 3_040,
                movingMillis = seconds * 1_000L,
                zoneMillis = seconds * 1_000L,
                distanceMeters = 2_270.0,
                activeKcal = 118.0,
            ),
            lastStepAtMillis = start + seconds * 1_000L,
            lastEventAtMillis = start + seconds * 1_000L,
            gps = true,
            stepsMeters = 2_190.0,
            measuredShare = 0.97,
            pausesKept = true,
            roundLine = RoundLine(roundId = 3, standing = RoundStanding.ON, marks = 2, gapMillis = -36_000),
        )
        val minutes = (0 until 28).map { MinuteSteps(start / 60_000 + it, walk.localEpochDay, 104 + (it * 7) % 15) }
        val state = state(nowMinute = 10 * 60 + 40).copy(
            session = LiveSessionState(walk, cadence = 114, canKeepGoing = false, alertsWhileScreenOff = true),
            sessionShape = OutingShape(
                sessionId = walk.id,
                minutes = OutingMinutes.of(walk, minutes, nowMillis = walk.lastEventAtMillis),
                route = RouteShape.of(points),
                dayGoalSteps = 10_000,
            ),
        )
        show(state)
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(SessionCardTags.CARD))
        compose.waitForIdle()
        save("today-round")
    }

    /**
     * Three quarters of a loop round a park in Milan, a fix a second at a steady walk, with the
     * small wander of a receiver under trees: a line a reader would recognise as their own.
     */
    private fun parkWalk(fromSecond: Long, seconds: Int): List<RoutePoint> {
        val centreLat = 45.4728
        val centreLon = 9.1760
        val metersPerLat = 111_320.0
        val metersPerLon = metersPerLat * cos(centreLat * PI / 180)
        fun shape(t: Double): Pair<Double, Double> {
            val r = 420.0 * (1 + 0.16 * sin(3 * t + 0.6) + 0.07 * cos(5 * t))
            return r * cos(t) * 1.25 to r * sin(t)
        }
        // The loop's arc length, so that each second walks the same 1.4 m along it.
        val steps = 4_000
        val along = DoubleArray(steps + 1)
        for (i in 1..steps) {
            val (x0, y0) = shape(2 * PI * (i - 1) / steps)
            val (x1, y1) = shape(2 * PI * i / steps)
            along[i] = along[i - 1] + hypot(x1 - x0, y1 - y0)
        }
        var index = 0
        return (0 until seconds).map { second ->
            val walked = second * 1.4
            while (index < steps && along[index + 1] < walked) index++
            val (x, y) = shape(2 * PI * index / steps + PI * 0.9)
            val wander = 2.5 * sin(second / 17.0) + 1.5 * cos(second / 7.0)
            RoutePoint(
                epochSecond = fromSecond + second,
                latitudeE6 = ((centreLat + (y + wander) / metersPerLat) * 1e6).roundToInt(),
                longitudeE6 = ((centreLon + (x - wander) / metersPerLon) * 1e6).roundToInt(),
                altitudeDm = 1_220,
                accuracyMeters = 6,
                kind = if (second == 0) RoutePointKind.START else RoutePointKind.MEASURED,
            )
        }
    }

    @Test
    fun todayChartRead() {
        show(state(nowMinute = 15 * 60 + 40))
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(TodayTags.CHART))
        compose.onNodeWithTag(TodayTags.CHART).performTouchInput {
            // A tap at 12:55, the end of the lunch walk, pins the readout there. The sample day's
            // first steps come after five, so the plot runs from 05:00 to midnight.
            val plotWidth = width - 44.dp.toPx()
            click(Offset(plotWidth * (12 * 60 + 55 - 5 * 60) / (1_440f - 5 * 60), height - 60.dp.toPx()))
        }
        compose.waitForIdle()
        save("today-chart")
    }

    /** An open foldable (the inner screen of a Pixel 9 Pro Fold): the page as a column in the middle. */
    @Test
    @Config(qualifiers = "en-rUS-w851dp-h882dp-xhdpi")
    fun todayFoldable() {
        show(state(nowMinute = 15 * 60 + 40))
        save("today-foldable")
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
