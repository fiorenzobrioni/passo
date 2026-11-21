package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToKey
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.sessions.OutingsFilter
import com.callbackdev.passo.core.domain.sessions.YourOutings
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

/**
 * «Your outings» (PLANNING.md §11 Phase 23, step 23.2), drawn from a state: its sentence, its
 * months newest first, each row a door to its outing, the filter, and the routes read only for the
 * rows on screen.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class OutingsListScreenTest {
    @get:Rule val compose = createComposeRule()

    private val today = LocalDate.of(2026, 11, 21)
    private val zone = ZoneId.systemDefault()
    private var nextId = 100L

    /** A brisk half hour on [day] at [hour], its goal reached, from the steps alone. */
    private fun outing(
        day: LocalDate,
        hour: Int = 18,
        minute: Int = 5,
        goal: SessionGoalKind = SessionGoalKind.TIME,
        reached: Boolean = true,
        meters: Double = 2_430.0,
        movingMinutes: Int = 31,
    ): Session {
        val start = day.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        val end = start + (movingMinutes + 3) * 60_000L
        return Session(
            id = nextId++,
            planId = 2,
            name = null,
            goalKind = goal,
            goalValue = if (goal == SessionGoalKind.OPEN) 0 else 30,
            intensity = if (goal == SessionGoalKind.OPEN) SessionIntensity.FREE else SessionIntensity.BRISK,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = day.toEpochDay(),
            startedAtMillis = start,
            state = SessionState.FINISHED,
            endedAtMillis = end,
            end = if (reached && goal != SessionGoalKind.OPEN) SessionEnd.GOAL else SessionEnd.STOPPED,
            // An outing with no goal has none to reach.
            reachedAtMillis = if (reached && goal != SessionGoalKind.OPEN) end else null,
            totals = SessionTotals(
                steps = movingMinutes * 112,
                movingMillis = movingMinutes * 60_000L,
                zoneMillis = movingMinutes * 55_000L,
                distanceMeters = meters,
                activeKcal = 110.0,
            ),
        )
    }

    private val milan = OutingSamples.measured
    private val milanRoute = RouteKey(milan.id, milan.startedAtMillis, OutingSamples.points().size)
    private val milanShape = checkNotNull(RouteShape.of(OutingSamples.points()))

    /** A November: Milan's walk with GPS on the 18th, a walk with no goal and a brisk one stopped short. */
    private fun november(): List<Session> = listOf(
        milan,
        outing(LocalDate.of(2026, 11, 20), hour = 7, minute = 40, goal = SessionGoalKind.OPEN, movingMinutes = 52),
        outing(LocalDate.of(2026, 11, 3), reached = false, movingMinutes = 21, meters = 1_620.0),
    )

    private fun state(sessions: List<Session>, routes: List<RouteKey> = emptyList()) =
        OutingsListUiState(YourOutings.of(sessions, routes, today), UnitPreference.METRIC, today)

    /** Pictures as the model hands them, counting which routes were asked for. */
    private class CountingPictures(private val shape: (RouteKey) -> RouteShape?) : RoutePictures {
        val asked = mutableListOf<RouteKey>()

        override fun peek(key: RouteKey): RouteShape? = null

        override suspend fun of(key: RouteKey): RouteShape? {
            asked += key
            return shape(key)
        }
    }

    private fun show(
        state: OutingsListUiState?,
        filter: OutingsFilter = OutingsFilter.ALL,
        dark: Boolean = false,
        pictures: RoutePictures = RoutePictures.None,
        onOpen: (Long) -> Unit = {},
    ) {
        compose.setContent {
            var chosen by remember { mutableStateOf(filter) }
            PassoTheme(darkTheme = dark) {
                OutingsListScreen(
                    state = state,
                    filter = chosen,
                    onFilter = { chosen = it },
                    onBack = {},
                    onOpenOuting = onOpen,
                    pictures = pictures,
                )
            }
        }
    }

    @Test
    fun `with no outing yet, the page says where one comes from`() {
        show(state(emptyList()))

        compose.onNodeWithTag(OutingsListTags.EMPTY)
            .assertIsDisplayed()
            .assertTextContains("No outing yet", substring = true)
        compose.onNodeWithTag(OutingsListTags.FILTER).assertDoesNotExist()
        compose.onNodeWithTag(OutingsListTags.SUMMARY).assertDoesNotExist()
        snapshot("outings_empty")
    }

    @Test
    fun `one month says how many and since when, then each outing as the day's list draws it`() {
        val pictures = CountingPictures { if (it == milanRoute) milanShape else null }
        val opened = mutableListOf<Long>()
        show(state(november(), listOf(milanRoute)), pictures = pictures, onOpen = { opened += it })

        compose.onNodeWithText("3 outings since November; 1 with its\u00A0route.").assertIsDisplayed()
        compose.onNodeWithTag("${OutingsListTags.MONTH}-2026-11")
            .assertContentDescriptionContains("November. 3 outings · 2 h in motion")
        // Newest first: the walk with no goal of the 20th, Milan's of the 18th, the one of the 3rd.
        val tops = listOf(100L, 12L, 101L).map {
            compose.onNodeWithTag("${OutingsListTags.ROW}-$it").fetchSemanticsNode().boundsInRoot.top
        }
        assertThat(tops).isInOrder()
        // Milan's: its route read for its tile, measured, a door to its page.
        assertThat(pictures.asked).containsExactly(milanRoute)
        compose.onNodeWithTag("${OutingsListTags.ROW}-12")
            .assertContentDescriptionContains("Brisk walk, Wednesday, November 18 at 5:40", substring = true)
            .assertContentDescriptionContains("Goal reached. 4.26 km measured, 47 minutes in motion", substring = true)
            .assertContentDescriptionContains("With its route", substring = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        // TalkBack says where a touch goes, as on the day's lists.
        val milanRow = compose.onNodeWithTag("${OutingsListTags.ROW}-12")
        assertThat(milanRow.fetchSemanticsNode().config[SemanticsActions.OnClick].label).isEqualTo("See the outing")
        milanRow.performClick()
        assertThat(opened).containsExactly(12L)
        // The walk with no goal says its time once; the one stopped short what share it came to,
        // and its distance as an estimate.
        compose.onNodeWithTag("${OutingsListTags.ROW}-100")
            .assertContentDescriptionContains("Walk, Friday, November 20 at 7:40", substring = true)
            .assertContentDescriptionContains("AM. 2.43 km, estimated, 52 minutes in motion", substring = true)
        compose.onNodeWithTag("${OutingsListTags.ROW}-101")
            .assertContentDescriptionContains(
                "70% of the goal. 1.62 km, estimated, 21 minutes in motion",
                substring = true,
            )
        snapshot("outings_month")
    }

    @Test
    fun `months come newest first, with their year before this one`() {
        val sessions = november() + listOf(
            outing(LocalDate.of(2026, 3, 14)),
            outing(LocalDate.of(2025, 12, 30), hour = 9),
            outing(LocalDate.of(2025, 12, 2)),
        )
        show(state(sessions, listOf(milanRoute)), dark = true)

        compose.onNodeWithText("6 outings since December 2025; 1 with its\u00A0route.").assertIsDisplayed()
        compose.onNodeWithTag("${OutingsListTags.MONTH}-2026-11")
            .assertContentDescriptionContains("November. 3 outings", substring = true)
        compose.onNodeWithTag(OutingsListTags.LIST).performScrollToNode(hasTestTag("${OutingsListTags.MONTH}-2025-12"))
        compose.onNodeWithTag("${OutingsListTags.MONTH}-2025-12")
            .assertIsDisplayed()
            .assertContentDescriptionContains("December 2025. 2 outings", substring = true)
        compose.onNodeWithTag("${OutingsListTags.MONTH}-2026-03")
            .assertContentDescriptionContains("March. 1 outing", substring = true)
        snapshot("outings_months_dark")
    }

    @Test
    fun `the filter keeps the outings that hold a route, and says what one can do with them`() {
        show(state(november(), listOf(milanRoute)))

        compose.onNodeWithTag("${OutingsListTags.FILTER}-ALL").assertIsSelected()
        compose.onNodeWithText("With their route").performClick()

        compose.onNodeWithTag("${OutingsListTags.FILTER}-WITH_ROUTE").assertIsSelected()
        compose.onNodeWithText("Open one to share its route or delete it.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag("${OutingsListTags.ROW}-12").assertIsDisplayed()
        compose.waitForIdle()
        compose.onNodeWithTag("${OutingsListTags.ROW}-101").assertDoesNotExist()
        compose.onNodeWithTag("${OutingsListTags.MONTH}-2026-11")
            .assertContentDescriptionContains("November. 1 outing · 47 minutes in motion")
        compose.onNodeWithTag("${OutingsListTags.ROW}-12").performClick()
        snapshot("outings_with_routes")
    }

    @Test
    fun `with no route at all there is no filter to choose`() {
        val pictures = CountingPictures { milanShape }
        show(state(november()), pictures = pictures)

        compose.onNodeWithText("3 outings since\u00A0November.").assertIsDisplayed()
        compose.onNodeWithTag(OutingsListTags.FILTER).assertDoesNotExist()
        // Every row wears its mark: no route is read for an outing that has none.
        assertThat(pictures.asked).isEmpty()
    }

    @Test
    fun `opened on the routes once none is left, the page says so and keeps its filter`() {
        show(state(november()), filter = OutingsFilter.WITH_ROUTE)

        compose.onNodeWithTag(OutingsListTags.FILTER).assertIsDisplayed()
        compose.onNodeWithTag(OutingsListTags.EMPTY)
            .assertTextContains("None of your outings holds a route", substring = true)
        compose.onNodeWithText("All").performClick()
        compose.onNodeWithTag("${OutingsListTags.ROW}-12").assertIsDisplayed()
    }

    @Test
    fun `a single outing is said as one`() {
        show(state(listOf(milan), listOf(milanRoute)))

        compose.onNodeWithText("One outing so far, with its\u00A0route.").assertIsDisplayed()
    }

    @Test
    fun `the routes are read only for the rows on screen`() {
        // Forty outings with GPS, one an evening for forty evenings: a screen holds a handful.
        val sessions = (0 until 40).map { day ->
            milan.copy(
                id = 1_000L + day,
                localEpochDay = milan.localEpochDay - day,
                startedAtMillis = milan.startedAtMillis - day * 86_400_000L,
            )
        }
        val routes = sessions.map { RouteKey(it.id, it.startedAtMillis, 1_400) }
        val pictures = CountingPictures { milanShape }
        show(state(sessions, routes), pictures = pictures)
        compose.waitForIdle()

        assertThat(pictures.asked.size).isAtLeast(3)
        assertThat(pictures.asked.size).isLessThan(15)
        assertThat(pictures.asked).doesNotContain(routes.last())

        // A jump, as a fast fling is: the rows between are never composed.
        compose.onNodeWithTag(OutingsListTags.LIST).performScrollToKey("outing-1039")
        compose.waitForIdle()
        assertThat(pictures.asked).contains(routes.last())
        assertThat(pictures.asked.toSet().size).isLessThan(routes.size)
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian on a small phone`() {
        show(state(november(), listOf(milanRoute)), pictures = CountingPictures { milanShape })

        compose.onNodeWithText("3 uscite da novembre; 1 con il\u00A0percorso.").assertIsDisplayed()
        compose.onNodeWithTag("${OutingsListTags.MONTH}-2026-11")
            .assertContentDescriptionContains("Novembre. 3 uscite", substring = true)
        compose.onNodeWithTag("${OutingsListTags.ROW}-12")
            .assertContentDescriptionContains("Camminata svelta, mercoledì 18 novembre alle 17:40", substring = true)
        compose.onNodeWithText("Con il percorso").assertIsDisplayed()
        snapshot("outings_it")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, every outing still reads`() {
        show(state(november(), listOf(milanRoute)), pictures = CountingPictures { milanShape })
        compose.walkPage(hasTestTag(OutingsListTags.LIST), "outings_large_text")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the outings are a column in the middle`() {
        show(state(november(), listOf(milanRoute)), pictures = CountingPictures { milanShape })
        compose.walkPage(hasTestTag(OutingsListTags.LIST), "outings_foldable")
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
