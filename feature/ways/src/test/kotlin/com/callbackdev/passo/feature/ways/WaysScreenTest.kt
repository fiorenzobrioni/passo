package com.callbackdev.passo.feature.ways

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.ways.WayStartChoice
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.callbackdev.passo.core.testing.writeScreenshot
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** The Ways (PLANNING.md §11 Phase 11), drawn from made-up days rather than a database. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class WaysScreenTest {
    @get:Rule val compose = createComposeRule()

    private fun showList(
        state: WaysUiState = WaysSamples.state(),
        dark: Boolean = false,
        onOpen: (WayId, Long?) -> Unit = {
                _,
                _,
            ->
        },
    ) {
        compose.setContent { PassoTheme(darkTheme = dark) { WaysScreen(state, onBack = {}, onOpenWay = onOpen) } }
        compose.waitForIdle()
    }

    private fun showWay(
        way: WayId,
        journeyId: Long? = null,
        state: WaysUiState = WaysSamples.state(),
        dark: Boolean = false,
        actions: WayActions = WayActions(),
    ) {
        compose.setContent {
            PassoTheme(darkTheme = dark) { WayScreen(state, way, journeyId, onBack = {}, actions = actions) }
        }
        compose.waitForIdle()
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        compose.assertAccessible()
        compose.writeScreenshot(name)
    }

    @Test
    fun `the way under way leads the page, with where the reader stands`() {
        showList()
        compose.onNodeWithTag(WaysTags.ACTIVE).assertIsDisplayed()
        compose.onNodeWithText("Past Monteriggioni").assertIsDisplayed()
        compose.onNodeWithText("km to Rome", substring = true).assertIsDisplayed()
        snapshot("ways")
    }

    @Test
    fun `each of the four opens its page, and a finished way is kept`() {
        var opened: Pair<WayId, Long?>? = null
        showList(onOpen = { way, journey -> opened = way to journey })
        compose.onNodeWithTag(WaysTags.LIST).performScrollToNode(hasTestTag(WaysTags.way(WayId.CAMINO_FRANCES)))
        compose.onNodeWithTag(WaysTags.way(WayId.CAMINO_FRANCES)).performClick()
        assertThat(opened).isEqualTo(WayId.CAMINO_FRANCES to null)
        compose.onNodeWithTag(WaysTags.LIST).performScrollToNode(hasTestTag(WaysTags.YOURS))
        compose.onNodeWithText("Walked from Sep 1, 2025 to", substring = true).assertExists()
        compose.onNodeWithTag(WaysTags.LIST).performScrollToNode(hasTestTag(WaysTags.CREDIT))
        compose.onNodeWithText("OpenStreetMap contributors", substring = true).assertExists()
        snapshot("ways_catalogue")
    }

    @Test
    fun `a way under way shows the map, the credential and the stages`() {
        showWay(WayId.VIA_FRANCIGENA)
        compose.onNodeWithText("Past Monteriggioni").assertIsDisplayed()
        snapshot("way")
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasTestTag(WaysTags.STAMPS))
        snapshot("way_credential")
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasText("Siena"))
        compose.onNodeWithText("Siena’s Piazza del Campo", substring = true).assertExists()
        snapshot("way_stages")
    }

    @Test
    fun `a way not started says what it would take and where a start would place the reader`() {
        var started: Pair<WayStartChoice, LocalDate?>? = null
        showWay(
            WayId.VIA_FRANCIGENA,
            state = WaysSamples.state(active = null),
            actions = WayActions(start = { choice, day -> started = choice to day }),
        )
        compose.onNodeWithText("at your usual pace", substring = true, ignoreCase = true).assertExists()
        snapshot("way_preview")
        compose.onNodeWithTag(WaysTags.START).performClick()
        compose.onNodeWithText("From 1 January").assertIsDisplayed()
        compose.onNodeWithText("You would already be past", substring = true).assertExists()
        snapshot("way_start")
        compose.onNodeWithText("From 1 January").performClick()
        compose.onNodeWithText("Start").performClick()
        assertThat(started).isEqualTo(WayStartChoice.THIS_YEAR to null)
    }

    @Test
    fun `one way at a time`() {
        showWay(WayId.CAMINO_FRANCES)
        compose.onNodeWithText("You are walking the Via Francigena", substring = true).assertExists()
        compose.onNodeWithTag(WaysTags.START).assertIsNotEnabled()
    }

    @Test
    fun `leaving asks first`() {
        var left: Long? = null
        showWay(WayId.VIA_FRANCIGENA, actions = WayActions(leave = { left = it }))
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasTestTag(WaysTags.LEAVE))
        compose.onNodeWithTag(WaysTags.LEAVE).performClick()
        compose.onNodeWithText("Leave this way?").assertIsDisplayed()
        snapshot("way_leave")
        compose.onNodeWithText("Leave").performClick()
        assertThat(left).isEqualTo(WaysSamples.francigena.id)
    }

    @Test
    fun `a finished way says when it was walked`() {
        showWay(WayId.VIA_DEGLI_DEI, journeyId = WaysSamples.dei.id)
        compose.onNodeWithText("Arrived in Florence").assertIsDisplayed()
        compose.onNodeWithText("days, from Sep 1, 2025", substring = true).assertExists()
        snapshot("way_finished")
    }

    @Test
    fun `the ways in the dark`() {
        showWay(WayId.VIA_FRANCIGENA, dark = true)
        snapshot("way_dark")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, every way still reads`() {
        showWay(WayId.CAMINO_FRANCES, state = WaysSamples.state(active = null))
        compose.walkPage(hasTestTag(WaysTags.PAGE), "way_large_text", maxScreens = 6)
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the ways are a column in the middle`() {
        showList()
        compose.walkPage(hasTestTag(WaysTags.LIST), "ways_foldable", maxScreens = 4)
    }
}
