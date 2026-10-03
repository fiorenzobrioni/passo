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
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
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
        val plan = plans[3]
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
        val done = checkNotNull(SessionPlans.start(plans[3], start, 0, 0, 8_000)).copy(
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

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
