package com.callbackdev.passo.feature.ways

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.components.SessionCardTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.ways.WayStartChoice
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.callbackdev.passo.core.testing.writeScreenshot
import com.callbackdev.passo.core.tracking.VoiceAvailability
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
    fun `each way opens its page, and a finished way is kept`() {
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
    fun `the Camino Portugués, the fifth way, is listed by its length`() {
        var opened: Pair<WayId, Long?>? = null
        showList(onOpen = { way, journey -> opened = way to journey })
        compose.onNodeWithTag(WaysTags.LIST).performScrollToNode(hasTestTag(WaysTags.way(WayId.CAMINO_PORTUGUES)))
        compose.onNodeWithText("Porto to Santiago de Compostela", substring = true).assertExists()
        compose.onNodeWithTag(WaysTags.way(WayId.CAMINO_PORTUGUES)).performClick()
        assertThat(opened).isEqualTo(WayId.CAMINO_PORTUGUES to null)
    }

    @Test
    fun `the Camino Portugués from Porto, drawn over Portugal and Galicia`() {
        showWay(WayId.CAMINO_PORTUGUES, state = WaysSamples.state(active = null))
        compose.onNodeWithText("Camino Portugués").assertIsDisplayed()
        snapshot("way_portugues")
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasText("Barcelos"))
        compose.onNodeWithText("a roast cock stood up", substring = true).assertExists()
        snapshot("way_portugues_stages")
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

    // --- City walks (Phase 11, second part) ---------------------------------------------------

    @Test
    fun `the cities are listed with where each walk stands`() {
        var opened: Pair<WayId, Long?>? = null
        showList(onOpen = { way, journey -> opened = way to journey })
        compose.onNodeWithTag(WaysTags.LIST).performScrollToNode(hasTestTag(WaysTags.CITIES))
        compose.onNodeWithText("Under way:", substring = true).assertExists()
        // In its city's row, and in Your ways, further down past the five cities.
        compose.onNodeWithTag(WaysTags.way(WayId.MILAN_DUOMO_NAVIGLI))
            .assert(hasText("Walked on Sep 12", substring = true))
        snapshot("ways_cities")
        compose.onNodeWithTag(WaysTags.way(WayId.LONDON_PALACE_TOWER)).performClick()
        assertThat(opened).isEqualTo(WayId.LONDON_PALACE_TOWER to null)
        compose.onNodeWithTag(WaysTags.LIST).performScrollToNode(hasTestTag(WaysTags.YOURS))
        compose.onAllNodes(hasText("Walked on Sep 12", substring = true) and hasAnyAncestor(hasTestTag(WaysTags.YOURS)))
            .assertCountEquals(1)
    }

    @Test
    fun `the five cities each have their walk`() {
        var opened: Pair<WayId, Long?>? = null
        showList(onOpen = { way, journey -> opened = way to journey })
        for (walk in listOf(
            WayId.MILAN_DUOMO_NAVIGLI,
            WayId.ROME_COLOSSEUM_VATICAN,
            WayId.PARIS_VOSGES_EIFFEL,
            WayId.LONDON_PALACE_TOWER,
            WayId.MADRID_DEBOD_RETIRO,
        )) {
            compose.onNodeWithTag(WaysTags.LIST).performScrollToNode(hasTestTag(WaysTags.way(walk)))
            compose.onNodeWithTag(WaysTags.way(walk)).assertExists()
        }
        snapshot("ways_five_cities")
        compose.onNodeWithTag(WaysTags.way(WayId.MADRID_DEBOD_RETIRO)).performClick()
        assertThat(opened).isEqualTo(WayId.MADRID_DEBOD_RETIRO to null)
    }

    @Test
    fun `Rome's walk, with the Tiber and its parks`() {
        showWay(WayId.ROME_COLOSSEUM_VATICAN, state = WaysSamples.state(walks = emptyList(), live = null))
        compose.onNodeWithText("From the Colosseum to St Peter’s", substring = true).assertExists()
        snapshot("walk_rome")
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasText("Piazza Navona"))
        compose.onNodeWithText("Domitian’s stadium", substring = true).assertExists()
        snapshot("walk_rome_places")
    }

    @Test
    fun `Paris's walk, with the Seine, in the dark`() {
        showWay(WayId.PARIS_VOSGES_EIFFEL, state = WaysSamples.state(walks = emptyList(), live = null), dark = true)
        compose.onNodeWithText("From Place des Vosges to the Eiffel Tower", substring = true).assertExists()
        snapshot("walk_paris_dark")
    }

    @Test
    fun `Madrid's walk, with the Retiro`() {
        showWay(WayId.MADRID_DEBOD_RETIRO, state = WaysSamples.state(walks = emptyList(), live = null))
        compose.onNodeWithText("From the Temple of Debod to the Retiro", substring = true).assertExists()
        snapshot("walk_madrid")
    }

    @Test
    fun `a walk under way shows its outing, with the place ahead`() {
        showWay(WayId.LONDON_PALACE_TOWER)
        compose.onNodeWithTag(SessionCardTags.CARD).assertExists()
        compose.onNodeWithText("Next: Royal Festival Hall, 230 m").assertExists()
        // The page has the walk's map: the card does not draw its own.
        compose.onNodeWithTag(SessionCardTags.MAP).assertDoesNotExist()
        snapshot("walk_live")
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasText("Royal Festival Hall"))
        snapshot("walk_places")
    }

    @Test
    fun `a walk begun continues from where it stands, or begins again after asking`() {
        var started: Boolean? = null
        showWay(
            WayId.LONDON_PALACE_TOWER,
            state = WaysSamples.state(outings = listOf(WaysSamples.londonYesterday), live = null),
            actions = WayActions(walk = WalkActions(start = { started = it })),
        )
        compose.onNodeWithText("Past Houses of Parliament").assertIsDisplayed()
        compose.onNodeWithText("Next: Westminster Bridge, 200 m", substring = true).assertExists()
        snapshot("walk_under_way")
        compose.onNodeWithText("Continue from Houses of Parliament").performClick()
        assertThat(started).isFalse()
        compose.onNodeWithTag(WaysTags.WALK_AGAIN).performClick()
        compose.onNodeWithText("Start again from the beginning?").assertIsDisplayed()
        snapshot("walk_again")
        compose.onNode(hasText("Start again") and !hasTestTag(WaysTags.WALK_AGAIN)).performClick()
        assertThat(started).isTrue()
    }

    @Test
    fun `a walk not begun says its length, its places and its steps, and keeps its voice`() {
        var started: Boolean? = null
        var voice: SessionVoice? = null
        showWay(
            WayId.MILAN_DUOMO_NAVIGLI,
            state = WaysSamples.state(walks = listOf(WaysSamples.london), live = null),
            actions = WayActions(walk = WalkActions(start = { started = it }, voice = { voice = it })),
        )
        compose.onNodeWithText("9.32 km · 14 places").assertIsDisplayed()
        compose.onNodeWithText("About 13,300 steps, in one outing or a few.").assertExists()
        snapshot("walk_preview")
        compose.onNodeWithTag(WaysTags.WALK_START).performClick()
        assertThat(started).isFalse()
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasTestTag(WaysTags.WALK_VOICE))
        compose.onNodeWithTag("${WaysTags.WALK_VOICE}-${SessionVoice.ALWAYS.name}").performClick()
        assertThat(voice).isEqualTo(SessionVoice.ALWAYS)
    }

    @Test
    fun `a walk walked to its end says when, and can be walked again`() {
        showWay(WayId.MILAN_DUOMO_NAVIGLI, journeyId = WaysSamples.milan.id)
        compose.onNodeWithText("The walk is done: Naviglio Grande.").assertIsDisplayed()
        compose.onNodeWithText("Walked on Sep 12: 9.32 km.").assertExists()
        compose.onNodeWithText("Walk it again").assertExists()
        snapshot("walk_finished")
    }

    @Test
    fun `one outing at a time, a walk included`() {
        showWay(
            WayId.MILAN_DUOMO_NAVIGLI,
            state = WaysSamples.state(walks = emptyList(), live = WaysSamples.live(WaysSamples.brisk)),
        )
        compose.onNodeWithText("An outing is under way", substring = true).assertExists()
        compose.onNodeWithTag(WaysTags.WALK_START).assertIsNotEnabled()
    }

    @Test
    fun `a walk in the dark`() {
        showWay(WayId.LONDON_PALACE_TOWER, dark = true)
        snapshot("walk_dark")
    }

    @Test
    fun `a city's parks and canals in the dark`() {
        showWay(WayId.MILAN_DUOMO_NAVIGLI, state = WaysSamples.state(walks = emptyList(), live = null), dark = true)
        snapshot("walk_milan_dark")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, a walk still reads`() {
        showWay(WayId.LONDON_PALACE_TOWER)
        compose.walkPage(hasTestTag(WaysTags.PAGE), "walk_large_text", maxScreens = 8)
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable a walk is a column in the middle`() {
        showWay(WayId.MILAN_DUOMO_NAVIGLI, state = WaysSamples.state(walks = emptyList(), live = null))
        compose.walkPage(hasTestTag(WaysTags.PAGE), "walk_foldable", maxScreens = 6)
    }

    @Test
    fun `a way finished or left can be deleted from Your ways, after a warning`() {
        var deleted: Long? = null
        showWay(WayId.VIA_DEGLI_DEI, journeyId = WaysSamples.dei.id, actions = WayActions(delete = { deleted = it }))
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasTestTag(WaysTags.DELETE))
        compose.onNodeWithTag(WaysTags.DELETE).performClick()
        compose.onNodeWithText("Delete this way?").assertIsDisplayed()
        compose.onNodeWithText("for good", substring = true).assertExists()
        snapshot("way_delete")
        compose.onNodeWithText("Delete").performClick()
        assertThat(deleted).isEqualTo(WaysSamples.dei.id)
    }

    @Test
    fun `the way under way has no delete, only leave`() {
        showWay(WayId.VIA_FRANCIGENA)
        compose.onNodeWithTag(WaysTags.DELETE).assertDoesNotExist()
    }

    @Test
    fun `a walk walked to its end can be deleted, its outings kept`() {
        var deleted: Long? = null
        showWay(
            WayId.MILAN_DUOMO_NAVIGLI,
            journeyId = WaysSamples.milan.id,
            actions = WayActions(walk = WalkActions(delete = { deleted = it })),
        )
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasTestTag(WaysTags.DELETE))
        compose.onNodeWithTag(WaysTags.DELETE).performClick()
        compose.onNodeWithText("Its outings stay in History", substring = true).assertExists()
        compose.onNodeWithText("Delete").performClick()
        assertThat(deleted).isEqualTo(WaysSamples.milan.id)
    }

    @Test
    fun `a walk's voice can be heard before it starts, and changed`() {
        var heard = false
        showWay(
            WayId.MILAN_DUOMO_NAVIGLI,
            state = WaysSamples.state(
                walks = emptyList(),
                live = null,
            ).copy(voiceAvailability = VoiceAvailability.READY),
            actions = WayActions(walk = WalkActions(tryVoice = { heard = true })),
        )
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasTestTag(WaysTags.WALK_TRY_VOICE))
        compose.onNodeWithText("Change voice").assertExists()
        snapshot("walk_voice")
        compose.onNodeWithTag(WaysTags.WALK_TRY_VOICE).performClick()
        assertThat(heard).isTrue()
    }

    @Test
    fun `with no offline voice, a walk's page says so`() {
        showWay(
            WayId.MILAN_DUOMO_NAVIGLI,
            state = WaysSamples.state(walks = emptyList(), live = null)
                .copy(voiceAvailability = VoiceAvailability.NO_OFFLINE_VOICE),
        )
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasText("Install a voice"))
        compose.onNodeWithTag(WaysTags.WALK_TRY_VOICE).assertDoesNotExist()
    }
}
