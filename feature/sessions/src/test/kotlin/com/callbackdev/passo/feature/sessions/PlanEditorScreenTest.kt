package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.UnitPreference
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
                editor = editor.copy(draft = editor.draft.copy(goalKind = kind, goalValue = SessionPlans.convert(editor.draft, kind, lengths, 2_400)))
            },
            intensity = { editor = editor.copy(draft = editor.draft.copy(intensity = it)) },
            milestone = { m, on ->
                editor = editor.copy(draft = editor.draft.copy(milestones = if (on) editor.draft.milestones + m else editor.draft.milestones - m))
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
    fun `leaving with changes asks first`() {
        val changed = state().let { it.copy(draft = it.draft.copy(goalValue = 45)) }
        var left = false
        compose.setContent { PassoTheme { PlanEditorScreen(changed, onBack = { left = true }, actions = PlanEditorActions()) } }

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

    private fun snapshot(name: String) {
        compose.waitForIdle()
        runCatching {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
