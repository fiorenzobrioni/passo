package com.callbackdev.passo.feature.history

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.history.PeriodScale
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's pictures of History (docs/screenshots), drawn from realistic sample months. Run
 * with `./gradlew :feature:history:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("passo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    private fun show(target: HistoryTarget) {
        compose.setContent {
            PassoTheme {
                HistoryScreen(
                    state = HistorySamples.state(),
                    dayDetail = { HistorySamples.detail(it) },
                    onOpenSettings = {},
                    target = target,
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun day() {
        show(HistoryTarget(PeriodScale.DAY, HistorySamples.today.minusDays(1)))
        save("history-day")
    }

    @Test
    fun month() {
        show(HistoryTarget(PeriodScale.MONTH, HistorySamples.today))
        compose.onAllNodesWithTag(HistoryTags.PAGE).onFirst().performScrollToNode(hasTestTag(HistoryTags.CALENDAR))
        save("history-month")
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
