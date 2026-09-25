package com.callbackdev.passo.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.passo.widget.config.WidgetConfigScreen
import com.callbackdev.passo.widget.glance.GlanceWidgetContent
import com.callbackdev.passo.widget.words.WordsWidgetContent
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * The README's pictures of the widgets (docs/screenshots), drawn from the realistic sample day.
 * Run with `./gradlew :widget:testDebugUnitTest -PupdateScreenshots`; skipped otherwise.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w393dp-h852dp-xhdpi")
class ReadmeScreenshots {
    @get:Rule val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val output = System.getProperty("passo.readmeScreenshots")

    @Before
    fun onlyOnRequest() = assumeTrue("Run with -PupdateScreenshots", output != null)

    @Test
    fun widgets() {
        val row = DpSize(361.dp, 85.dp)
        val half = DpSize(172.dp, 189.dp)
        val panel = DpSize(361.dp, 189.dp)
        val clay = WidgetLook(cardColor = WidgetCardColor.CLAY)
        val board = HomeBoard(context, widthDp = 393)
            .row(row to renderCard(context, row) { GlanceWidgetContent(WidgetSamples.model()) })
            .row(row to renderCard(context, row) { WordsWidgetContent(WidgetSamples.model()) })
            .row(
                half to renderCard(context, half) { GlanceWidgetContent(WidgetSamples.model(clay)) },
                half to renderCard(context, half) { WordsWidgetContent(WidgetSamples.model(clay)) },
            )
            .row(panel to renderCard(context, panel) { GlanceWidgetContent(WidgetSamples.model()) })
        board.draw().saveTo(File(checkNotNull(output)), "widgets")
    }

    @Test
    fun widgetSettings() {
        compose.setContent {
            PassoTheme {
                WidgetConfigScreen(
                    kind = WidgetKind.GLANCE,
                    look = WidgetLook(),
                    model = WidgetSamples.model(),
                    placed = null,
                    onLook = {},
                    onOpacityDrag = {},
                    onOpacityDone = {},
                    onDone = {},
                )
            }
        }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File(checkNotNull(output)).apply { mkdirs() }
        File(dir, "widget-settings.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
