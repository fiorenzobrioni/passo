package com.callbackdev.passo.core.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.callbackdev.passo.core.designsystem.components.placeLabels
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A name on the map goes beside its point, on the side its own way leaves free: never over the
 * line it names, where another side is clear, and never over a name placed before it.
 */
class WayMapLabelsTest {
    // One pixel a dp: a 300 by 200 map, and a pill 80 by 20.
    private val density = Density(1f)
    private val size = Size(300f, 200f)
    private val pill = Size(80f, 20f)

    private fun place(points: List<Offset>, way: List<Offset>): List<Rect> =
        with(density) { placeLabels(points.map { pill to it }, size, way) }

    private fun Rect.covers(way: List<Offset>) = way.any { contains(it) }

    @Test
    fun `with nothing in the way, a name goes to the right of its point`() {
        val at = Offset(100f, 100f)
        val box = place(listOf(at), way = listOf(at)).single()
        assertThat(box.left).isEqualTo(108f)
        assertThat(box.center.y).isEqualTo(100f)
    }

    @Test
    fun `a way that leaves to the right sends its name to a side that is clear`() {
        val at = Offset(100f, 100f)
        val way = (0..20).map { Offset(100f + it * 8f, 100f) }
        val box = place(listOf(at), way).single()
        assertThat(box.covers(way.drop(1))).isFalse()
    }

    @Test
    fun `a way that leaves upwards keeps its name beside the point`() {
        val at = Offset(100f, 150f)
        val way = (0..15).map { Offset(100f, 150f - it * 8f) }
        val box = place(listOf(at), way).single()
        assertThat(box.left).isEqualTo(108f)
        assertThat(box.covers(way.drop(1))).isFalse()
    }

    @Test
    fun `two points close together get two names that do not overlap`() {
        val first = Offset(100f, 100f)
        val second = Offset(104f, 102f)
        val (a, b) = place(listOf(first, second), way = listOf(first, second))
        assertThat(a.overlaps(b)).isFalse()
    }

    @Test
    fun `a name stays inside the map`() {
        val at = Offset(290f, 195f)
        val box = place(listOf(at), way = listOf(at)).single()
        assertThat(box.right).isAtMost(292f)
        assertThat(box.bottom).isAtMost(192f)
    }
}
