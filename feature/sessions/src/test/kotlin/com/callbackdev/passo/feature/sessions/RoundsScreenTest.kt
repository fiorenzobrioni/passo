package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.components.RoundChartTags
import com.callbackdev.passo.core.designsystem.components.RouteMapTags
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.rounds.KeptRound
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * «Your rounds» and a round's own page (PLANNING.md §11 Phase 20), in each of their states, in both
 * languages, at twice the text size and on an open foldable.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class RoundsScreenTest {
    @get:Rule val compose = createComposeRule()

    private var opened: Long? = null
    private var outing: Long? = null
    private var forgot = false
    private var back = false
    private var same: KeptRound? = null
    private var another: KeptRound? = null

    private fun showList(state: RoundsUiState? = RoundSamples.list(), dark: Boolean = false) {
        compose.setContent {
            PassoTheme(darkTheme = dark) { RoundsScreen(state = state, onBack = {}, onOpenRound = { opened = it }) }
        }
    }

    private fun showPage(state: RoundUiState? = RoundSamples.page(), dark: Boolean = false) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                RoundScreen(
                    state = state,
                    onBack = { back = true },
                    onOpenOuting = { outing = it },
                    onRename = {},
                    onForget = { forgot = true },
                    onSame = { same = it },
                    onAnother = { another = it },
                )
            }
        }
    }

    @Test
    fun `the rounds, the latest walked first, each a door to its page`() {
        showList()
        compose.onNodeWithTag(
            RoundsTags.SUMMARY,
        ).assertTextContains("2 walks you take again and again", substring = true)
        // Named by the reader, or by its usual length until it is.
        compose.onNodeWithTag("${RoundsTags.ROW}-4")
            .assertContentDescriptionContains("Your round of 3.20 km. 3 walks. Usually", substring = true)
        compose.onNodeWithTag("${RoundsTags.ROW}-3")
            .assertContentDescriptionContains(
                "Navigli. 4.26 km · 8 walks. Usually 52 minutes in motion.",
                substring = true,
            )
            .assert(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick))
            .performClick()
        assertThat(opened).isEqualTo(3L)
        snapshot("rounds_list")
    }

    @Test
    fun `how a round is found and compared is said once, under the rounds`() {
        showList()
        compose.onNodeWithTag(RoundsTags.LIST).performScrollToNode(hasTestTag(RoundsTags.SUMMARY))
        compose.onNodeWithText("from the same door and the same way round", substring = true).assertExists()
    }

    @Test
    fun `no round left, the page says how one is found`() {
        showList(RoundSamples.list().copy(rounds = emptyList()))
        compose.onNodeWithTag(RoundsTags.SUMMARY).assertTextContains("No usual round now.", substring = true)
    }

    @Test
    fun `in the dark, the rounds`() {
        showList(dark = true)
        snapshot("rounds_list_dark")
    }

    @Test
    fun `a round's page - its sentence first, its usual, its map`() {
        showPage(RoundSamples.page(name = "Navigli"))
        compose.onNodeWithText("Navigli").assertIsDisplayed()
        compose.onNodeWithTag(
            RoundsTags.SENTENCE,
        ).assertTextEquals("8 walks since September; the last 4 faster than usual.")
        compose.onNodeWithText("Usually", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(
            RouteMapTags.MAP,
        ).assertContentDescriptionContains("Your round: 4.26 km", substring = true)
        snapshot("round_page")
    }

    @Test
    fun `its times over the weeks, one sentence for TalkBack, and the usual as a line`() {
        showPage()
        compose.onNodeWithTag(RoundsTags.PAGE).performScrollToNode(hasTestTag(RoundChartTags.TIMES))
        compose.onNodeWithTag(RoundChartTags.TIMES)
            .assertContentDescriptionContains("8 walks since September; the last 4 faster than usual.")
        compose.onNodeWithText("lower is faster", substring = true).assertExists()
        snapshot("round_times")
    }

    @Test
    fun `each walk with its day and its time at the usual length, a door to its page, the newest first`() {
        showPage()
        compose.onNodeWithTag(RoundsTags.PAGE).performScrollToNode(hasTestTag("${RoundsTags.WALK}-12"))
        compose.onNodeWithTag("${RoundsTags.WALK}-12")
            .assertContentDescriptionContains("Wednesday, November 18:", substring = true)
            .performClick()
        assertThat(outing).isEqualTo(12L)
    }

    @Test
    fun `every walk's route deleted, the page keeps its history and says why it has no map`() {
        showPage(RoundSamples.page(map = false))
        compose.onNodeWithTag(RoundsTags.NO_MAP).assertIsDisplayed()
        compose.onNodeWithTag(RouteMapTags.MAP).assertDoesNotExist()
        compose.onNodeWithTag(RoundsTags.PAGE).performScrollToNode(hasTestTag(RoundChartTags.TIMES))
    }

    @Test
    fun `the round the walks since made asks whether it is the one with no line, and is answered`() {
        val lost = RoundSamples.lost()
        showPage(RoundSamples.page(same = lost))
        compose.onNodeWithText("Is this “Along the canals”?").assertIsDisplayed()
        compose.onNodeWithText("lost the routes of its walks", substring = true).assertIsDisplayed()
        snapshot("round_same")
        compose.onNodeWithTag(RoundsTags.SAME_NO).performClick()
        assertThat(another?.id).isEqualTo(lost.id)
        compose.onNodeWithTag(RoundsTags.SAME_YES).performClick()
        assertThat(same?.id).isEqualTo(lost.id)
    }

    @Test
    fun `the round with no line asks back whether the round its walks since made is it`() {
        val since = RoundSamples.round()
        showPage(RoundSamples.page(name = "Along the canals", map = false, same = since))
        compose.onNodeWithText("Is this “Your round of 4.26 km”?").assertIsDisplayed()
        compose.onNodeWithText("since its routes were deleted may have made", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(RoundsTags.NO_MAP).assertIsDisplayed()
        snapshot("round_same_lost")
    }

    @Test
    fun `with nothing to ask, no question`() {
        showPage()
        compose.onNodeWithTag(RoundsTags.SAME).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h640dp-xhdpi")
    fun `the question in Italian on a small phone`() {
        showPage(RoundSamples.page(same = RoundSamples.lost("Lungo i navigli")))
        compose.onNodeWithText("È «Lungo i navigli»?").assertIsDisplayed()
        compose.onNodeWithTag(RoundsTags.SAME_YES).assertTextContains("Lo stesso")
        compose.onNodeWithTag(RoundsTags.SAME_NO).assertTextContains("Un altro giro")
        snapshot("round_same_it")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi", fontScale = 2f)
    fun `the question at twice the text size`() {
        showPage(RoundSamples.page(same = RoundSamples.lost()))
        compose.walkPage(hasTestTag(RoundsTags.PAGE), "round_same_large_text")
    }

    @Test
    fun `in the dark, the question`() {
        showPage(RoundSamples.page(same = RoundSamples.lost()), dark = true)
        snapshot("round_same_dark")
    }

    /*
     * The rename dialog itself is not drawn here: a text field in a dialog's window never lets
     * Robolectric's clock go idle, focused or not. Its rules are the repository's, tested there (a
     * name trimmed, a blank one naming the round by its length again); its door is tested below,
     * and the dialog is the device check's.
     */
    @Test
    fun `rename is in the bar, a door of its own`() {
        showPage(RoundSamples.page(name = "Navigli"))
        compose.onNodeWithTag(RoundsTags.RENAME).assertIsDisplayed().assertTextContains("Rename")
    }

    @Test
    fun `forget this round is asked first, and the outings stay`() {
        showPage()
        compose.onNodeWithTag(RoundsTags.PAGE).performScrollToNode(hasTestTag(RoundsTags.FORGET))
        compose.onNodeWithTag(RoundsTags.FORGET).performClick()
        assertThat(forgot).isFalse()
        compose.onNodeWithText("Its walks stay as they are, in Your outings.", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(RoundsTags.FORGET_CONFIRM).performClick()
        assertThat(forgot).isTrue()
    }

    @Test
    fun `a round no longer one of the reader's goes back`() {
        showPage(RoundSamples.page().copy(round = null))
        compose.waitForIdle()
        assertThat(back).isTrue()
    }

    @Test
    fun `in the dark, a round's page`() {
        showPage(dark = true)
        snapshot("round_page_dark")
        compose.onNodeWithTag(RoundsTags.PAGE).performScrollToNode(hasTestTag(RoundChartTags.TIMES))
        snapshot("round_times_dark")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h640dp-xhdpi")
    fun `in Italian on a small phone`() {
        showList()
        compose.onNodeWithTag(RoundsTags.SUMMARY).assertTextContains("2 giri che fai e rifai", substring = true)
        compose.onNodeWithTag("${RoundsTags.ROW}-4")
            .assertContentDescriptionContains("Il tuo giro di 3,20 km. 3 uscite.", substring = true)
        snapshot("rounds_list_it")
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h640dp-xhdpi")
    fun `a round's page in Italian on a small phone`() {
        showPage()
        compose.onNodeWithTag(
            RoundsTags.SENTENCE,
        ).assertTextEquals("8 uscite da settembre; le ultime 4 più veloci del solito.")
        snapshot("round_page_it")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi", fontScale = 2f)
    fun `at twice the text size`() {
        showList()
        compose.walkPage(hasTestTag(RoundsTags.LIST), "rounds_large_text")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h640dp-xhdpi", fontScale = 2f)
    fun `a round's page at twice the text size`() {
        showPage()
        compose.walkPage(hasTestTag(RoundsTags.PAGE), "round_large_text")
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h900dp-xhdpi")
    fun `on an open foldable`() {
        showPage()
        compose.walkPage(hasTestTag(RoundsTags.PAGE), "round_foldable")
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
