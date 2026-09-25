package com.callbackdev.passo.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * What no other test reaches, because the launcher reads it (Chiaro's `WidgetPreviewTest`): the
 * two providers, which must be the same size spec so the cards can trade places, and their
 * static previews, which may only use views `RemoteViews` can inflate.
 */
class WidgetProviderTest {
    private val xml = File("src/main/res/xml")
    private val layout = File("src/main/res/layout")

    private val remoteViewsTags = setOf(
        "FrameLayout", "LinearLayout", "RelativeLayout", "GridLayout",
        "TextView", "ImageView", "Button", "ImageButton", "ProgressBar",
    )

    private fun providers() = listOf("widget_glance_info.xml", "widget_words_info.xml").map { File(xml, it).readText() }

    private fun attributes(text: String): Map<String, String> =
        Regex("""android:(\w+)="([^"]*)"""").findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

    @Test
    fun `the two cards share one size spec`() {
        val sizing = listOf(
            "minWidth", "minHeight", "minResizeWidth", "minResizeHeight", "maxResizeWidth", "maxResizeHeight",
            "targetCellWidth", "targetCellHeight", "resizeMode", "updatePeriodMillis",
        )
        val (glance, words) = providers().map(::attributes)
        sizing.forEach { key -> assertThat(glance[key]).isEqualTo(words[key]) }
        assertThat(glance["minResizeWidth"]).isEqualTo("40dp")
        assertThat(glance["minResizeHeight"]).isEqualTo("40dp")
        assertThat(glance["updatePeriodMillis"]).isEqualTo("0")
    }

    @Test
    fun `every provider names a preview that exists and inflates in RemoteViews`() {
        providers().forEach { provider ->
            val name = checkNotNull(attributes(provider)["previewLayout"]).removePrefix("@layout/")
            val preview = File(layout, "$name.xml")
            assertThat(preview.isFile).isTrue()
            val tags = Regex("""<([A-Za-z][A-Za-z0-9_.]*)[\s>]""").findAll(preview.readText())
                .map { it.groupValues[1] }
                .filterNot { it == "xml" }
                .toSet()
            assertThat(remoteViewsTags).containsAtLeastElementsIn(tags)
        }
    }

    @Test
    fun `the static preview's card is the default card`() {
        val colors = File("src/main/res/values/colors.xml").readText()
        assertThat(colors).contains("<color name=\"widget_preview_card\">#FF0F3B6B</color>")
    }
}
