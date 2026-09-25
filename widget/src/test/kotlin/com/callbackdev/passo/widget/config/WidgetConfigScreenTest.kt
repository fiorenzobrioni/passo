package com.callbackdev.passo.widget.config

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.passo.widget.WidgetArrangement
import com.callbackdev.passo.widget.WidgetBackground
import com.callbackdev.passo.widget.WidgetKind
import com.callbackdev.passo.widget.WidgetLook
import com.callbackdev.passo.widget.WidgetSamples
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h1400dp-xhdpi")
class WidgetConfigScreenTest {
    @get:Rule val compose = createComposeRule()

    private var look by mutableStateOf(WidgetLook())

    private fun show(kind: WidgetKind, dark: Boolean = false) {
        compose.setContent {
            PassoTheme(darkTheme = dark) {
                WidgetConfigScreen(
                    kind = kind,
                    look = look,
                    model = WidgetSamples.model(),
                    placed = null,
                    onLook = { look = it },
                    onOpacityDrag = { look = look.copy(opacityPct = it) },
                    onOpacityDone = {},
                    onDone = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `at a glance offers its own switches and the arrangement, and nothing it would ignore`() {
        show(WidgetKind.GLANCE)
        scrollTo("The day hour by hour")
        compose.onNodeWithText("The day hour by hour").assertExists()
        compose.onNodeWithText("Distance and calories").assertDoesNotExist()
        compose.onNodeWithText("The day in figures").assertDoesNotExist()
        scrollTo("Ring on the right, sentence beside the number")
        compose.onNodeWithText("Ring on the right, sentence beside the number").performClick()
        assertThat(look.arrangement).isEqualTo(WidgetArrangement.RING_END)
        save("widget-config-glance")
    }

    @Test
    fun `in words offers the goal, the estimates and the figures, and no arrangement`() {
        show(WidgetKind.WORDS)
        scrollTo("The day in figures")
        compose.onNodeWithText("The goal").assertExists()
        compose.onNodeWithText("Distance and calories").assertExists()
        compose.onNodeWithText("The day hour by hour").assertDoesNotExist()
        compose.onNodeWithText("Arrangement").assertDoesNotExist()
        compose.onNodeWithText("The day in figures").performClick()
        assertThat(look.showDetails).isFalse()
    }

    @Test
    fun `the colours appear once a colour is the ground, and a pick is kept`() {
        look = WidgetLook(background = WidgetBackground.LIGHT)
        show(WidgetKind.WORDS)
        compose.onNodeWithContentDescription("Terracotta").assertDoesNotExist()
        compose.onNodeWithText("A colour").performClick()
        compose.onNodeWithContentDescription("Terracotta").performClick()
        assertThat(look.background).isEqualTo(WidgetBackground.COLOR)
        assertThat(look.cardColor).isEqualTo(WidgetCardColor.CLAY)
        compose.onNodeWithText("Terracotta").assertExists()
        save("widget-config-words")
    }

    @Test
    fun `every ground and every colour has its row`() {
        assertThat(WidgetBackgroundChoices.map { it.first }).containsExactlyElementsIn(WidgetBackground.entries)
        assertThat(WidgetCardColorChoices.map { it.first }).containsExactlyElementsIn(WidgetCardColor.entries)
    }

    private fun scrollTo(text: String) {
        compose.onNodeWithTag(WidgetConfigTags.LIST).performScrollToNode(hasText(text))
    }

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
