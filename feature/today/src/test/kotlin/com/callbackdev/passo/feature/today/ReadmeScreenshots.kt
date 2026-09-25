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
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDayCalculator
import com.callbackdev.passo.core.model.Profile
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

    private fun state(nowMinute: Int) = TodayUiState(
        date = LocalDate.of(2026, 9, 24),
        nowMinute = nowMinute.toDouble(),
        overview = TodayOverview.of(
            minutes = SampleDays.workday(seed = 5, lunchWalkMinutes = 36, until = nowMinute),
            profile = profile,
            goalSteps = 10_000,
            nowMinute = nowMinute.toDouble(),
            typical = usual,
        ),
        status = TrackingStatus.COUNTING,
        units = UnitPreference.METRIC,
        walkingStepLength = StepLengths.of(profile).walkingMeters,
        firstDay = false,
        celebrate = false,
    )

    private fun show(state: TodayUiState, dark: Boolean = false) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                TodayScreen(state, askInSettings = false, onOpenSettings = {
                }, onAllow = {}, onResume = {}, onCelebrated = {})
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

    @Test
    fun todayChartRead() {
        show(state(nowMinute = 15 * 60 + 40))
        compose.onNodeWithTag(TodayTags.LIST).performScrollToNode(hasTestTag(TodayTags.CHART))
        compose.onNodeWithTag(TodayTags.CHART).performTouchInput {
            // A tap at 12:55, the end of the lunch walk, pins the readout there.
            val plotWidth = width - 44.dp.toPx()
            click(Offset(plotWidth * (12 * 60 + 55) / 1_440f, height - 60.dp.toPx()))
        }
        compose.waitForIdle()
        save("today-chart")
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
