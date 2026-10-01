package com.callbackdev.passo.feature.ways

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.model.WayId
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's pictures of the Ways (docs/screenshots), from the sample days of the tests: the
 * Francigena under way since March, past Monteriggioni; London walked today, past the London
 * Eye. Run with `-PupdateScreenshots`;
 * skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val output = System.getProperty("passo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    @Test
    fun way() {
        compose.setContent {
            PassoTheme {
                WayScreen(WaysSamples.state(), WayId.VIA_FRANCIGENA, null, onBack = {}, actions = WayActions())
            }
        }
        save("way")
    }

    @Test
    fun credential() {
        compose.setContent {
            PassoTheme {
                WayScreen(WaysSamples.state(), WayId.VIA_FRANCIGENA, null, onBack = {}, actions = WayActions())
            }
        }
        compose.onNodeWithTag(WaysTags.PAGE).performScrollToNode(hasTestTag(WaysTags.STAMPS))
        save("way-credential")
    }

    @Test
    fun start() {
        val state = WaysSamples.state(active = null)
        compose.setContent {
            PassoTheme { WayScreen(state, WayId.VIA_FRANCIGENA, null, onBack = {}, actions = WayActions()) }
        }
        compose.onNodeWithTag(WaysTags.START).performClick()
        save("way-start")
    }

    /** London during a walk: the map with the reader's point past the London Eye, the outing's card. */
    @Test
    fun walk() {
        compose.setContent {
            PassoTheme {
                WayScreen(WaysSamples.state(), WayId.LONDON_PALACE_TOWER, null, onBack = {}, actions = WayActions())
            }
        }
        save("walk")
    }

    private fun save(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
