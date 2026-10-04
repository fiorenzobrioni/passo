package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.testing.assertAccessible
import com.callbackdev.passo.core.testing.walkPage
import com.callbackdev.passo.core.tracking.VoiceAvailability
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class PlanEditorScreenTest {
    @get:Rule val compose = createComposeRule()

    private val lengths = StepLengths(walkingMeters = 0.73, runningMeters = 0.95)

    private fun state(plan: SessionPlan = SessionPlans.NEW, isNew: Boolean = true) = PlanEditorState(
        original = plan,
        draft = plan,
        isNew = isNew,
        units = UnitPreference.METRIC,
        imperial = false,
        lengths = lengths,
        restOfDaySteps = 2_400,
        canVibrate = true,
    )

    @Test
    fun `a new outing opens as a brisk half hour, with what it comes to`() {
        compose.setContent { PassoTheme { PlanEditorScreen(state(), onBack = {}, actions = PlanEditorActions()) } }

        compose.onNodeWithText("New outing").assertIsDisplayed()
        compose.onNodeWithText("30 min").assertIsDisplayed()
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.ESTIMATE))
        compose.onNodeWithText("About 3,000 steps and 2.19 km, with your step of 73 cm.").assertIsDisplayed()
        snapshot("editor_new")
    }

    @Test
    fun `the editor follows the draft and saves it`() {
        var editor by mutableStateOf(state())
        var saved = false
        val actions = PlanEditorActions(
            goalKind = { kind ->
                editor =
                    editor.copy(
                        draft = editor.draft.copy(
                            goalKind = kind,
                            goalValue = SessionPlans.convert(editor.draft, kind, lengths, 2_400),
                        ),
                    )
            },
            intensity = { editor = editor.copy(draft = editor.draft.copy(intensity = it)) },
            milestone = { m, on ->
                editor =
                    editor.copy(
                        draft = editor.draft.copy(
                            milestones = if (on) {
                                editor.draft.milestones + m
                            } else {
                                editor.draft.milestones -
                                    m
                            },
                        ),
                    )
            },
            save = { saved = true },
        )
        compose.setContent { PassoTheme { PlanEditorScreen(editor, onBack = {}, actions = actions) } }

        compose.onNodeWithText("Steps").performClick()
        compose.onNodeWithText("3,000 steps").assertIsDisplayed()
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag("${EditorTags.PACE}-RUN"))
        compose.onNodeWithTag("${EditorTags.PACE}-RUN").performClick()
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag("${EditorTags.MILESTONE}-25"))
        compose.onNodeWithTag("${EditorTags.MILESTONE}-25").performClick()
        compose.onNodeWithTag(EditorTags.SAVE).performClick()

        assertThat(editor.draft.goalKind).isEqualTo(SessionGoalKind.STEPS)
        assertThat(editor.draft.intensity).isEqualTo(SessionIntensity.RUN)
        assertThat(editor.draft.milestones).containsExactly(SessionMilestone.QUARTER, SessionMilestone.HALF)
        assertThat(saved).isTrue()
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.VIBRATE))
        snapshot("editor_signals")
    }

    @Test
    fun `every vibration can be felt first, the end by a long stop too`() {
        val felt = mutableListOf<String>()
        val actions = PlanEditorActions(
            tryVibration = { felt += it.name },
            tryEndedStill = { felt += "STILL" },
        )
        compose.setContent { PassoTheme { PlanEditorScreen(state(), onBack = {}, actions = actions) } }

        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.TRY_ENDED_STILL))
        compose.onNodeWithTag("${EditorTags.TRY}-100").performClick()
        compose.onNodeWithTag(EditorTags.TRY_ENDED_STILL).assertIsDisplayed()
        compose.onNodeWithText("Long stop").performClick()
        assertThat(felt).containsExactly("GOAL", "STILL").inOrder()
    }

    @Test
    fun `the voice is chosen, heard first, and says where it speaks`() {
        var editor by mutableStateOf(state().copy(voiceAvailability = VoiceAvailability.READY))
        var heard = false
        val actions = PlanEditorActions(
            voice = { editor = editor.copy(draft = editor.draft.copy(voice = it)) },
            tryVoice = { heard = true },
        )
        compose.setContent { PassoTheme { PlanEditorScreen(editor, onBack = {}, actions = actions) } }

        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.VOICE))
        compose.onNodeWithText("The signals are vibrations only.").assertIsDisplayed()
        compose.onNodeWithTag("${EditorTags.VOICE}-HEADPHONES").performClick()
        assertThat(editor.draft.voice).isEqualTo(SessionVoice.HEADPHONES)
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.TRY_VOICE))
        compose.onNodeWithText("Through headphones only", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(EditorTags.TRY_VOICE).performClick()
        assertThat(heard).isTrue()
        snapshot("editor_voice")
    }

    @Test
    fun `without an offline voice it says how to get one`() {
        var opened = false
        val missing = state().let {
            it.copy(
                draft = it.draft.copy(voice = SessionVoice.ALWAYS),
                voiceAvailability = VoiceAvailability.NO_OFFLINE_VOICE,
            )
        }
        compose.setContent {
            PassoTheme(darkTheme = true) {
                PlanEditorScreen(missing, onBack = {
                }, actions = PlanEditorActions(openVoiceSettings = { opened = true }))
            }
        }

        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasText("Install a voice"))
        compose.onNodeWithText("no offline voice in this language", substring = true).assertIsDisplayed()
        snapshot("editor_voice_missing_dark")
        compose.onNodeWithText("Install a voice").performClick()
        assertThat(opened).isTrue()
    }

    @Test
    fun `leaving with changes asks first`() {
        val changed = state().let { it.copy(draft = it.draft.copy(goalValue = 45)) }
        var left = false
        compose.setContent {
            PassoTheme { PlanEditorScreen(changed, onBack = { left = true }, actions = PlanEditorActions()) }
        }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Discard your changes?").assertIsDisplayed()
        assertThat(left).isFalse()
        compose.onNodeWithText("Discard").performClick()
        assertThat(left).isTrue()
    }

    @Test
    fun `a kept outing can be deleted, after asking`() {
        var deleted = false
        val kept = state(SessionPlans.PRESETS[2].copy(id = 3), isNew = false)
        compose.setContent {
            PassoTheme(darkTheme = true) {
                PlanEditorScreen(kept, onBack = {}, actions = PlanEditorActions(delete = { deleted = true }))
            }
        }

        compose.onNodeWithText("Edit outing").assertIsDisplayed()
        compose.onNodeWithText("Right now: 2,400 steps.").assertIsDisplayed()
        snapshot("editor_day_dark")
        compose.onNodeWithTag(EditorTags.DELETE).performClick()
        compose.onNodeWithTag(EditorTags.CONFIRM_DELETE).performClick()
        assertThat(deleted).isTrue()
    }

    @Test
    fun `with the keyboard up, save rides on it and the last settings are still within reach`() {
        lateinit var view: View
        compose.setContent {
            view = LocalView.current
            PassoTheme { PlanEditorScreen(state(), onBack = {}, actions = PlanEditorActions()) }
        }
        val keyboardPx = 900
        compose.runOnIdle {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, keyboardPx))
                .setVisible(WindowInsetsCompat.Type.ime(), true)
                .build()
            ViewCompat.dispatchApplyWindowInsets(view, insets)
        }
        compose.waitForIdle()
        val screen = compose.onRoot().getBoundsInRoot().bottom
        val keyboardTop = screen - with(compose.density) { keyboardPx.toDp() }
        val save = compose.onNodeWithTag(EditorTags.SAVE).getBoundsInRoot()
        assertThat(save.bottom.value).isAtMost(keyboardTop.value + 0.5f)
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.VOICE))
        compose.onNodeWithTag(EditorTags.VOICE).assertIsDisplayed()
        assertThat(compose.onNodeWithTag(EditorTags.VOICE).getBoundsInRoot().bottom.value)
            .isAtMost(save.top.value)
        snapshot("editor_keyboard")
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size on a small phone, the editor still reads`() {
        compose.setContent { PassoTheme { PlanEditorScreen(state(), onBack = {}, actions = PlanEditorActions()) } }
        compose.walkPage(hasTestTag(EditorTags.LIST), "editor_large_text", maxScreens = 20)
    }

    @Test
    @Config(qualifiers = "en-rUS-w841dp-h701dp-xhdpi")
    fun `on an open foldable the editor is a column in the middle`() {
        compose.setContent { PassoTheme { PlanEditorScreen(state(), onBack = {}, actions = PlanEditorActions()) } }
        compose.walkPage(hasTestTag(EditorTags.LIST), "editor_foldable")
    }

    // --- The interval walk (Phase 13) -------------------------------------------------------

    @Test
    fun `intervals say what the Japanese walk is, and pick its sets`() {
        var editor by mutableStateOf(state())
        val actions = PlanEditorActions(
            intervals = { on ->
                if (on) editor = editor.copy(draft = SessionPlans.withIntervals(editor.draft, editor.draft.intervals))
            },
            sets = { change ->
                editor = editor.copy(draft = SessionPlans.withIntervals(editor.draft, change(editor.draft.intervals)))
            },
        )
        compose.setContent { PassoTheme { PlanEditorScreen(editor, onBack = {}, actions = actions) } }

        compose.onNodeWithTag("${EditorTags.KIND}-true").performClick()
        compose.onNodeWithText("The Japanese interval walk").assertIsDisplayed()
        compose.onNodeWithText("Slow and fast walking in turns", substring = true).assertIsDisplayed()
        snapshot("editor_intervals")
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.SETS))
        compose.onNodeWithTag("${EditorTags.SETS}-more").performClick()
        compose.onNodeWithTag("${EditorTags.FAST}-less").performClick()
        assertThat(editor.draft.intervals).isEqualTo(IntervalSets(slowMinutes = 3, fastMinutes = 2, sets = 6))
        assertThat(editor.draft.goalValue).isEqualTo(30)
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.PROTOCOL))
        compose.onNodeWithText("30 min in all", substring = true).assertIsDisplayed()
        compose.onNodeWithTag(EditorTags.PROTOCOL).performClick()
        assertThat(editor.draft.intervals).isEqualTo(IntervalSets())
        // The fast pace is never free, and the shares of the goal are not offered.
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag("${EditorTags.PACE}-BRISK"))
        compose.onNodeWithTag("${EditorTags.PACE}-FREE").assertDoesNotExist()
        compose.onNodeWithTag("${EditorTags.MILESTONE}-50").assertDoesNotExist()
    }

    @Test
    fun `faster and slower can be felt first`() {
        val felt = mutableListOf<Boolean>()
        val japanese = state(SessionPlans.JAPANESE_WALKING.copy(id = 4), isNew = false)
        compose.setContent {
            PassoTheme {
                PlanEditorScreen(japanese, onBack = {}, actions = PlanEditorActions(tryInterval = { felt += it }))
            }
        }

        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag("${EditorTags.TRY_INTERVAL}-false"))
        compose.onNodeWithText("Faster").performClick()
        compose.onNodeWithText("Slower").performClick()
        assertThat(felt).containsExactly(true, false).inOrder()
        compose.onNodeWithTag("${EditorTags.TRY}-50").assertDoesNotExist()
        snapshot("editor_intervals_signals")
    }

    @Test
    fun `on a phone its counter cannot wake, it says once that the outing keeps it awake`() {
        val japanese = state(SessionPlans.JAPANESE_WALKING.copy(id = 4), isNew = false).copy(wakeUpCounter = false)
        compose.setContent {
            PassoTheme(darkTheme = true) { PlanEditorScreen(japanese, onBack = {}, actions = PlanEditorActions()) }
        }

        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.AWAKE))
        compose.onNodeWithText("Passo keeps it awake during the outing", substring = true).assertIsDisplayed()
        snapshot("editor_awake_dark")
    }

    @Test
    fun `with no signal, or a counter that wakes the phone, nothing is said`() {
        val silent = SessionPlans.JAPANESE_WALKING.copy(id = 4, vibrate = false)
        compose.setContent {
            PassoTheme {
                PlanEditorScreen(
                    state(silent, isNew = false).copy(wakeUpCounter = false),
                    onBack = {},
                    actions = PlanEditorActions(),
                )
            }
        }
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.VOICE))
        compose.onNodeWithTag(EditorTags.AWAKE).assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "en-rUS-w360dp-h740dp-xxhdpi", fontScale = 2f)
    fun `at twice the text size, the interval editor still reads`() {
        val japanese = state(SessionPlans.JAPANESE_WALKING.copy(id = 4), isNew = false)
        compose.setContent { PassoTheme { PlanEditorScreen(japanese, onBack = {}, actions = PlanEditorActions()) } }
        compose.walkPage(hasTestTag(EditorTags.LIST), "editor_intervals_large_text", maxScreens = 20)
    }

    @Test
    @Config(qualifiers = "it-rIT-w360dp-h740dp-xxhdpi")
    fun `in Italian, the interval editor fits a small phone`() {
        val japanese = state(SessionPlans.JAPANESE_WALKING.copy(id = 4), isNew = false)
        compose.setContent { PassoTheme { PlanEditorScreen(japanese, onBack = {}, actions = PlanEditorActions()) } }
        compose.onNodeWithText("La camminata giapponese a intervalli").assertIsDisplayed()
        compose.walkPage(hasTestTag(EditorTags.LIST), "editor_intervals_it", maxScreens = 12)
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
