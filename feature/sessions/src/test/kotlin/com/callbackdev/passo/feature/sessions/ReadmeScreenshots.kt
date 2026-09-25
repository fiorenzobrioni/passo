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
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.tracking.GoalNotificationsBlock
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

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
        val plan = plans[0].copy(
            milestones = setOf(SessionMilestone.QUARTER, SessionMilestone.HALF, SessionMilestone.THREE_QUARTERS),
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
        )
        compose.setContent { PassoTheme { PlanEditorScreen(state, onBack = {}, actions = PlanEditorActions()) } }
        compose.onNodeWithTag(EditorTags.LIST).performScrollToNode(hasTestTag(EditorTags.VIBRATE))
        save("outing-editor")
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
