package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.sessions.OutingsFilter
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.sessions.YourOutings
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.tracking.GoalNotificationsBlock
import com.callbackdev.passo.core.tracking.VoiceAvailability
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * The README's pictures of the outings (docs/screenshots), with the presets a first visit finds
 * and the profile of the other pictures. Run with `-PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("passo.readmeScreenshots")
    private val lengths = StepLengths.of(Profile(heightMeters = 1.78, weightKg = 74.0))
    private val plans = SessionPlans.PRESETS.mapIndexed { i, plan -> plan.copy(id = i + 1L) }

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    @Test
    fun outings() {
        val state = SessionsUiState(
            plans = plans,
            live = null,
            units = UnitPreference.METRIC,
            lengths = lengths,
            restOfDaySteps = 2_145,
            status = SessionsStatus.READY,
            alertsWhileScreenOff = true,
        )
        compose.setContent {
            PassoTheme {
                SessionsScreen(state, GoalNotificationsBlock.NONE, onBack = {
                }, onEdit = {}, actions = SessionsActions())
            }
        }
        save("outings")
    }

    @Test
    fun outingEditor() {
        val plan = plans[1].copy(
            milestones = setOf(SessionMilestone.QUARTER, SessionMilestone.HALF, SessionMilestone.THREE_QUARTERS),
            voice = SessionVoice.HEADPHONES,
        )
        val state = PlanEditorState(
            original = plan,
            draft = plan,
            isNew = false,
            units = UnitPreference.METRIC,
            imperial = false,
            lengths = lengths,
            restOfDaySteps = 2_145,
            canVibrate = true,
            voiceAvailability = VoiceAvailability.READY,
        )
        compose.setContent { PassoTheme { PlanEditorScreen(state, onBack = {}, actions = PlanEditorActions()) } }
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.TRY_VOICE))
        save("outing-editor")
    }

    @Test
    fun intervals() {
        val plan = plans[4]
        val state = PlanEditorState(
            original = plan,
            draft = plan,
            isNew = false,
            units = UnitPreference.METRIC,
            imperial = false,
            lengths = lengths,
            restOfDaySteps = 2_145,
            canVibrate = true,
        )
        compose.setContent { PassoTheme { PlanEditorScreen(state, onBack = {}, actions = PlanEditorActions()) } }
        save("intervals")
    }

    @Test
    fun intervalsDone() {
        val cadences = listOf(92, 112, 90, 114, 88, 97, 91, 116, 89, 118)
        val splits = cadences.mapIndexed { index, cadence ->
            val fast = index % 2 == 1
            val zone = if (cadence >= 100) 172_000L else 0L
            IntervalSplit(index, fast, steps = cadence * 3, movingMillis = 180_000, zoneMillis = zone)
        }
        val start = 18 * 3_600_000L - java.util.TimeZone.getDefault().getOffset(0L)
        val done = checkNotNull(SessionPlans.start(plans[4], start, 0, 0, 8_000)).copy(
            id = 12,
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            endedAtMillis = start + 32 * 60_000L,
            reachedAtMillis = start + 32 * 60_000L,
            lastStepAtMillis = start + 32 * 60_000L,
            totals = SessionTotals(
                steps = splits.sumOf { it.steps },
                movingMillis = 30 * 60_000L,
                zoneMillis = splits.filter { it.fast }.sumOf { it.zoneMillis },
                distanceMeters = 2_230.0,
                activeKcal = 104.0,
            ),
            splits = splits,
        )
        val state = SessionsUiState(
            plans = plans,
            live = LiveSessionState(done, cadence = null, canKeepGoing = true, alertsWhileScreenOff = true),
            units = UnitPreference.METRIC,
            lengths = lengths,
            restOfDaySteps = 0,
            status = SessionsStatus.READY,
            alertsWhileScreenOff = true,
        )
        compose.setContent {
            PassoTheme {
                SessionsScreen(state, GoalNotificationsBlock.NONE, onBack = {
                }, onEdit = {}, actions = SessionsActions())
            }
        }
        save("intervals-done")
    }

    /** The page of an outing measured with GPS, with a stretch laid from the steps. */
    @Test
    fun outingRoute() {
        compose.setContent {
            PassoTheme { OutingScreen(OutingSamples.state(), onBack = {}, actions = OutingActions()) }
        }
        save("outing-route")
    }

    /**
     * «Your outings»: 48 outings since March, 21 with their route, the newest of November on top,
     * each route drawn small at its row's start.
     */
    @Test
    fun outingsList() {
        val today = LocalDate.of(2026, 11, 21)
        val zone = ZoneId.systemDefault()
        var nextId = 200L
        val shapes = HashMap<Long, RouteShape>()

        fun outing(
            day: LocalDate,
            hour: Int,
            minute: Int,
            plan: SessionPlan,
            movingMinutes: Int,
            meters: Double,
            route: WayId? = null,
            reached: Boolean = true,
        ): Session {
            val start = day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
            val begun =
                checkNotNull(SessionPlans.start(plan, start, day.toEpochDay(), 0, 8_000, withGps = route != null))
            val end = start + (movingMinutes + 4) * 60_000L
            val session = begun.copy(
                id = nextId++,
                state = SessionState.FINISHED,
                endedAtMillis = end,
                end = if (reached && !begun.open) SessionEnd.GOAL else SessionEnd.STOPPED,
                reachedAtMillis = if (reached && !begun.open) end else null,
                totals = SessionTotals(
                    steps = (meters / 0.74).toInt(),
                    movingMillis = movingMinutes * 60_000L,
                    zoneMillis = movingMinutes * 54_000L,
                    distanceMeters = meters,
                    activeKcal = meters / 22,
                ),
                gps = route != null,
                measuredShare = if (route != null) 0.96 else null,
                stepsMeters = if (route != null) meters * 0.97 else null,
            )
            if (route != null) shapes[session.id] = checkNotNull(RouteShape.of(walkPoints(route, meters, start)))
            return session
        }

        val walk = plans[0]
        val brisk = plans[1]
        val run = plans[2]
        val japanese = plans[4]
        val november = listOf(
            outing(LocalDate.of(2026, 11, 20), 7, 40, walk, 52, 4_120.0, WayId.VIENNA_BELVEDERE_PRATER),
            outing(LocalDate.of(2026, 11, 18), 17, 40, brisk, 47, 4_260.0, WayId.MILAN_DUOMO_NAVIGLI),
            outing(LocalDate.of(2026, 11, 16), 9, 10, run, 30, 4_480.0),
            outing(LocalDate.of(2026, 11, 13), 18, 20, japanese, 30, 2_640.0),
            outing(LocalDate.of(2026, 11, 10), 7, 55, walk, 38, 3_050.0, WayId.PORTO_SE_PILAR),
            outing(LocalDate.of(2026, 11, 6), 18, 30, brisk, 14, 1_090.0, reached = false),
            outing(LocalDate.of(2026, 11, 2), 10, 15, walk, 66, 5_340.0, WayId.PRAGUE_CASTLE_WENCESLAS),
        )
        // The months before, back to March: one outing every five days or so, every other one
        // measured with GPS until 21 hold a route.
        val lines = listOf(WayId.ROME_COLOSSEUM_VATICAN, WayId.LONDON_PALACE_TOWER, WayId.KYOTO_KIYOMIZU_NISHIKI)
        var routed = november.count { it.gps }
        val older = (0 until 41).map { index ->
            val day = LocalDate.of(2026, 10, 30).minusDays(index * 5L + index % 3)
            val gps = index % 2 == 0 && routed < 21
            if (gps) routed++
            outing(
                day,
                hour = if (index % 3 == 0) 7 else 18,
                minute = (index * 7) % 60,
                plan = if (index % 4 == 1) brisk else walk,
                movingMinutes = 28 + (index * 11) % 40,
                meters = 2_300.0 + (index * 370) % 2_900,
                route = if (gps) lines[index % lines.size] else null,
            )
        }
        val sessions = november + older
        val keys = sessions.filter { it.gps }.map { RouteKey(it.id, it.startedAtMillis, 1_000) }
        val state = OutingsListUiState(YourOutings.of(sessions, keys, today), UnitPreference.METRIC, today)
        val pictures = object : RoutePictures {
            override fun peek(key: RouteKey): RouteShape? = shapes[key.sessionId]

            override suspend fun of(key: RouteKey): RouteShape? = shapes[key.sessionId]
        }
        compose.setContent {
            PassoTheme {
                OutingsListScreen(
                    state = state,
                    filter = OutingsFilter.ALL,
                    onFilter = {},
                    onBack = {},
                    onOpenOuting = {},
                    pictures = pictures,
                )
            }
        }
        save("outings-list")
    }

    /**
     * The brisk hour through Milan, the eighth time it was walked (PLANNING.md §11 Phase 20): its
     * page says it, sentence first, against the usual of the walks before.
     */
    @Test
    fun outingRound() {
        compose.setContent {
            PassoTheme { OutingScreen(state = RoundSamples.outingState(), onBack = {}, actions = OutingActions()) }
        }
        save("outing-round")
    }

    /** That round's own page: eight walks since September, the last four faster than usual. */
    @Test
    fun round() {
        compose.setContent {
            PassoTheme {
                RoundScreen(
                    state = RoundSamples.page(name = "Along the canals"),
                    onBack = {},
                    onOpenOuting = {},
                    onRename = {},
                    onForget = {},
                )
            }
        }
        save("round")
    }

    /** A point every ten metres along [way]'s line for [meters], from its start, at a walking pace. */
    private fun walkPoints(way: WayId, meters: Double, startMillis: Long): List<RoutePoint> {
        val line = Ways.of(way).map.line
        val length = minOf(meters, line.lengthMeters)
        return (0..(length / 10).toInt()).map { step ->
            val at = line.pointAt(step * 10.0)
            RoutePoint(
                epochSecond = startMillis / 1_000 + step * 7L,
                latitudeE6 = (at.latitude * 1e6).toInt(),
                longitudeE6 = (at.longitude * 1e6).toInt(),
                altitudeDm = null,
                accuracyMeters = 5,
                kind = if (step == 0) RoutePointKind.START else RoutePointKind.MEASURED,
            )
        }
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
