package com.callbackdev.passo.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.callbackdev.passo.core.designsystem.components.locatorBox
import com.callbackdev.passo.core.domain.ways.GeoBox
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The locator never covers a way's names: at a large text size the names grow, and the locator
 * is drawn smaller in a corner they leave free, or not at all.
 */
class WayMapLocatorTest {
    // One pixel a dp: a 300 by 200 map, a square country, so the locator is 90 a side at most.
    private val density = Density(1f)
    private val size = Size(300f, 200f)
    private val square = GeoBox(0.0, 0.0, 1.0, 1.0)
    private val along = (0..10).map { Offset(50f + it * 20f, 100f) }
    private val start = along.first()

    private fun box(names: List<Rect>) = with(density) { locatorBox(size, start, along, square, names) }

    @Test
    fun `with the names clear of every corner, the locator keeps its full size`() {
        val locator = box(emptyList())
        assertThat(locator?.width).isEqualTo(90f)
    }

    @Test
    fun `names in every corner make it smaller, still clear of them`() {
        // Each name reaches into a full-size corner, but not into the smaller one.
        val names = listOf(
            Rect(80f, 80f, 95f, 95f),
            Rect(205f, 80f, 220f, 95f),
            Rect(80f, 105f, 95f, 120f),
            Rect(205f, 105f, 220f, 120f),
        )
        val locator = box(names)
        assertThat(locator?.width).isEqualTo(67.5f)
        names.forEach { assertThat(locator?.overlaps(it)).isFalse() }
    }

    @Test
    fun `with no corner free of names, there is no locator`() {
        assertThat(box(listOf(Rect(0f, 0f, 300f, 200f)))).isNull()
    }
}
