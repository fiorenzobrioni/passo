package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.model.RoutePoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Which round an open outing with GPS is walking, as its route comes (PLANNING.md §11 Phase 20,
 * «The card, while it counts»): begun near the round's start, on it after half a kilometre, off it
 * after 200 m away from its line.
 */
class RoundWatchTest {
    private val park = RoundGuide(1, Ground.walk(Ground.LOOP, seed = 1), listOf(600_000L, 1_200_000L), walks = 4)

    /** [points], a walk's, fed to the watch as far as [meters] along it; how far that is. */
    private fun RoundWatch.walkTo(points: List<RoutePoint>, meters: Double): Double {
        val count = minOf(points.size, (meters / Ground.SPACING).toInt() + 1)
        add(points.take(count))
        return (count - 1) * Ground.SPACING
    }

    @Test
    fun `nothing is said before the first half kilometre`() {
        val watch = RoundWatch(listOf(park))
        val walked = watch.walkTo(Ground.walk(Ground.LOOP, seed = 2), 400.0)
        assertThat(watch.on(null, walked)).isNull()
    }

    @Test
    fun `on the round from its door, it is found after half a kilometre`() {
        val watch = RoundWatch(listOf(park))
        val walked = watch.walkTo(Ground.walk(Ground.LOOP, seed = 2), 520.0)
        assertThat(watch.on(null, walked)).isEqualTo(1L)
    }

    @Test
    fun `begun more than 100 m from where the round begins, it is not on the round`() {
        val watch = RoundWatch(listOf(park))
        val walked = watch.walkTo(Ground.walk(Ground.LOOP, seed = 2, shift = 0.0 to 130.0), 900.0)
        assertThat(watch.on(null, walked)).isNull()
    }

    @Test
    fun `the same streets walked the other way are never on the round`() {
        val watch = RoundWatch(listOf(park))
        val walked = watch.walkTo(Ground.walk(Ground.LOOP_OTHER_WAY, seed = 2), 1_500.0)
        assertThat(watch.on(null, walked)).isNull()
    }

    @Test
    fun `off the round's line for 200 m, it has left it`() {
        val watch = RoundWatch(listOf(park))
        val walk = Ground.walk(Ground.PARK_THEN_NORTH, seed = 2)
        assertThat(watch.on(null, watch.walkTo(walk, 600.0))).isEqualTo(1L)
        // North up its own street from the 600th metre: 160 m of it, some 120 off the line.
        val further = RoundWatch(listOf(park))
        assertThat(further.on(1L, further.walkTo(walk, 760.0))).isEqualTo(1L)
        val left = RoundWatch(listOf(park))
        assertThat(left.on(1L, left.walkTo(walk, 900.0))).isNull()
    }

    @Test
    fun `a round no walk of the reader's has a line for is never followed`() {
        val bare = RoundGuide(2, emptyList(), emptyList(), walks = 3)
        val watch = RoundWatch(listOf(bare))
        assertThat(watch.on(null, watch.walkTo(Ground.walk(Ground.LOOP, seed = 2), 900.0))).isNull()
    }

    @Test
    fun `two rounds from the same door are told apart as the walk goes`() {
        val north = RoundGuide(2, Ground.walk(Ground.PARK_THEN_NORTH, seed = 3), emptyList(), walks = 2)
        val walk = Ground.walk(Ground.PARK_THEN_NORTH, seed = 4)
        val watch = RoundWatch(listOf(park, north))
        // Their first 600 m are one street: on the one walked more.
        assertThat(watch.on(null, watch.walkTo(walk, 550.0))).isEqualTo(1L)
        // Then north, where only the other goes: on it, the first left behind.
        val later = RoundWatch(listOf(park, north))
        val walked = later.walkTo(walk, 1_100.0)
        assertThat(later.on(1L, walked)).isEqualTo(2L)
    }

    @Test
    fun `the usual at each kilometre is the round's own`() {
        val watch = RoundWatch(listOf(park))
        assertThat(watch.usualAt(1, 1)).isEqualTo(600_000L)
        assertThat(watch.usualAt(1, 2)).isEqualTo(1_200_000L)
        assertThat(watch.usualAt(1, 3)).isNull()
        assertThat(watch.usualAt(9, 1)).isNull()
    }
}
