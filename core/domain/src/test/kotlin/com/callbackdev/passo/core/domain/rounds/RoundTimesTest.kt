package com.callbackdev.passo.core.domain.rounds

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A round's times, honestly compared (PLANNING.md §11 Phase 20, «The comparison, honest»): by time
 * in motion, each brought to the round's usual length, the usual the middle of the walks before.
 */
class RoundTimesTest {
    private fun outing(id: Long, meters: Double, minutes: Double, day: Long = id, pausedMinutes: Double = 0.0) =
        Ground.outing(id, meters, minutes, day = day, pausedMinutes = pausedMinutes)

    @Test
    fun `the usual is the middle length, and the middle time brought to it`() {
        val walks = listOf(outing(1, 3_200.0, 40.0), outing(2, 3_000.0, 36.0), outing(3, 3_300.0, 45.0))
        val usual = checkNotNull(RoundTimes.usual(walks))
        assertThat(usual.meters).isEqualTo(3_200.0)
        // 40 min; 36 × 3200 / 3000 = 38.4 min; 45 × 3200 / 3300 = 43.6 min: the middle is 40.
        assertThat(usual.movingMillis).isEqualTo(40 * 60_000L)
        assertThat(usual.walks).isEqualTo(3)
    }

    @Test
    fun `a shortcut that leaves out a tenth, brought to the usual length, is not faster for it`() {
        val walks = listOf(outing(1, 3_520.0, 44.0), outing(2, 3_520.0, 44.0))
        // The same pace on 3.2 km instead of 3.52: four minutes less, and no faster a walk.
        val shortcut = outing(3, 3_200.0, 40.0)
        val compared = RoundTimes.comparison(shortcut, walks + shortcut)
        assertThat(compared.thisMillis).isEqualTo(44 * 60_000L)
        assertThat(compared.gapMillis).isEqualTo(0L)
    }

    @Test
    fun `a round with a detour of 300 m one day is compared at the usual length, not called slower for it`() {
        val walks = listOf(outing(1, 3_200.0, 40.0), outing(2, 3_200.0, 40.0))
        val detour = outing(3, 3_500.0, 43.75)
        val compared = RoundTimes.comparison(detour, walks + detour)
        assertThat(compared.gapMillis).isEqualTo(0L)
    }

    @Test
    fun `a round walked with a long pause is compared by time in motion, as every outing`() {
        val walks = listOf(outing(1, 3_200.0, 40.0), outing(2, 3_200.0, 40.0))
        val paused = outing(3, 3_200.0, 37.0, pausedMinutes = 25.0)
        val compared = RoundTimes.comparison(paused, walks + paused)
        assertThat(compared.gapMillis).isEqualTo(-3 * 60_000L)
    }

    @Test
    fun `the first two walks of a round have no usual, the third has the first two's`() {
        val first = outing(1, 3_200.0, 40.0)
        val second = outing(2, 3_200.0, 42.0)
        val third = outing(3, 3_200.0, 38.0)
        val walks = listOf(first, second, third)
        assertThat(RoundTimes.comparison(first, walks)).isEqualTo(RoundComparison(1, null, null, null))
        assertThat(RoundTimes.comparison(second, walks).usual).isNull()
        val compared = RoundTimes.comparison(third, walks)
        assertThat(compared.nth).isEqualTo(3)
        assertThat(compared.usual?.movingMillis).isEqualTo(41 * 60_000L)
        assertThat(compared.gapMillis).isEqualTo(-3 * 60_000L)
    }

    @Test
    fun `a walk is compared with the walks before it, not after`() {
        val walks =
            listOf(
                outing(1, 3_200.0, 40.0),
                outing(2, 3_200.0, 40.0),
                outing(3, 3_200.0, 40.0),
                outing(4, 3_200.0, 30.0),
            )
        assertThat(RoundTimes.comparison(walks[2], walks).gapMillis).isEqualTo(0L)
    }

    @Test
    fun `a difference under a fiftieth of the usual, or a quarter of a minute, is the usual time`() {
        assertThat(RoundTimes.even(-40_000, 40 * 60_000L)).isEqualTo(0L)
        assertThat(RoundTimes.even(-60_000, 40 * 60_000L)).isEqualTo(-60_000L)
        assertThat(RoundTimes.even(14_000, 5 * 60_000L)).isEqualTo(0L)
        assertThat(RoundTimes.even(16_000, 5 * 60_000L)).isEqualTo(16_000L)
    }

    @Test
    fun `the usual at each kilometre is the middle of the walks' own times there, from their legs`() {
        val walks = listOf(outing(1, 3_200.0, 32.0), outing(2, 3_200.0, 48.0), outing(3, 3_000.0, 30.0))
        val usual = RoundTimes.usualAtMarks(walks, 1_000.0, 4)
        // Ten, fifteen and ten minutes a kilometre: the middle is ten at each.
        assertThat(usual[0]).isEqualTo(10 * 60_000L)
        assertThat(usual[1]).isEqualTo(20 * 60_000L)
        assertThat(usual[2]).isEqualTo(30 * 60_000L)
        // No walk reached a fourth whole kilometre.
        assertThat(usual[3]).isNull()
    }

    @Test
    fun `a kilometre fewer than two walks reached has no usual`() {
        val walks = listOf(outing(1, 3_200.0, 32.0), outing(2, 2_900.0, 29.0))
        assertThat(RoundTimes.usualAtMarks(walks, 1_000.0, 3)[2]).isNull()
    }

    @Test
    fun `a member whose route was deleted still gives its times, from its legs`() {
        // Its legs are numbers in the outing, not in the route: nothing here reads a point.
        val walks = listOf(outing(1, 2_000.0, 20.0), outing(2, 2_000.0, 24.0))
        assertThat(RoundTimes.usualAtMarks(walks, 1_000.0, 2)).containsExactly(11 * 60_000L, 22 * 60_000L).inOrder()
    }

    @Test
    fun `the middle of an even count is the mean of its two middles`() {
        assertThat(RoundTimes.median(listOf(4.0, 1.0, 3.0, 2.0))).isEqualTo(2.5)
        assertThat(RoundTimes.median(listOf(5.0))).isEqualTo(5.0)
    }
}
