package com.callbackdev.passo.feature.onboarding

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.model.UnitPreference
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's pictures of the first run (docs/screenshots). Run with
 * `./gradlew :feature:onboarding:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("passo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private fun show(state: OnboardingState) {
        compose.setContent { PassoTheme { OnboardingScreen(state, OnboardingActions()) } }
        compose.waitForIdle()
    }

    @Test
    fun welcome() {
        show(OnboardingState(units = UnitPreference.METRIC))
        save("onboarding-welcome")
    }

    @Test
    fun goal() {
        show(
            OnboardingState(
                step = OnboardingStep.GOAL,
                heightMeters = 1.78,
                goalSteps = 10_000,
                units = UnitPreference.METRIC,
            ),
        )
        save("onboarding-goal")
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
