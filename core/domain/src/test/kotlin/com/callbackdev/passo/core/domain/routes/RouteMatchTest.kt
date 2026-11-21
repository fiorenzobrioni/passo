package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.rounds.Ground
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Whether two outings with GPS are the same round (PLANNING.md §11 Phase 20, «The match»): the
 * phase's edge cases, each a test that says so in its name, on routes written by hand.
 */
class RouteMatchTest {
    private fun walked(corners: List<Pair<Double, Double>>, seed: Int, shift: Pair<Double, Double> = 0.0 to 0.0) =
        WalkedRoute(Ground.walk(corners, seed = seed, shift = shift), Ground.length(corners))

    private fun compare(a: WalkedRoute, b: WalkedRoute) = RouteMatch.compare(a, b)

    @Test
    fun `the same loop walked twice is the same round, whatever the receiver's wander`() {
        val result = compare(walked(Ground.LOOP, seed = 1), walked(Ground.LOOP, seed = 2, shift = 12.0 to -9.0))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.SAME)
        assertThat(result.inOrder).isAtLeast(0.95)
    }

    @Test
    fun `the same streets walked the other way are not the same round`() {
        val result = compare(walked(Ground.LOOP, seed = 1), walked(Ground.LOOP_OTHER_WAY, seed = 2))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.OTHER_WAY)
    }

    @Test
    fun `the other way round is not the same round whichever of the two is asked first`() {
        val result = compare(walked(Ground.LOOP_OTHER_WAY, seed = 3), walked(Ground.LOOP, seed = 4))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.OTHER_WAY)
    }

    @Test
    fun `a loop started from another point of it is another round, rounds begin at the door`() {
        val result = compare(walked(Ground.LOOP, seed = 1), walked(Ground.LOOP_FROM_ELSEWHERE, seed = 2))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.STARTS_APART)
    }

    @Test
    fun `a round with a detour of 300 m one day is still the round, by four fifths`() {
        val result = compare(walked(Ground.LOOP, seed = 1), walked(Ground.LOOP_WITH_DETOUR, seed = 2))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.SAME)
    }

    @Test
    fun `a shortcut that leaves out a tenth is still the round, within the lengths' tenth`() {
        val round = walked(Ground.LOOP_WITH_SPUR, seed = 1)
        val shortcut = walked(Ground.LOOP, seed = 2)
        assertThat(1 - shortcut.meters / round.meters).isWithin(0.01).of(0.09)
        assertThat(compare(round, shortcut).verdict).isEqualTo(RouteMatch.Verdict.SAME)
        assertThat(compare(shortcut, round).verdict).isEqualTo(RouteMatch.Verdict.SAME)
    }

    @Test
    fun `two rounds from the same door are told apart by their lines, not by their start`() {
        val result = compare(walked(Ground.LOOP, seed = 1), walked(Ground.OTHER_LOOP, seed = 2))
        assertThat(Ground.length(Ground.LOOP)).isEqualTo(Ground.length(Ground.OTHER_LOOP))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.APART)
    }

    @Test
    fun `a walk that shares only its first streets with the round is not the round`() {
        val result = compare(walked(Ground.LOOP, seed = 1), walked(Ground.PARK_THEN_NORTH, seed = 2))
        assertThat(result.same).isFalse()
    }

    @Test
    fun `lengths more than a tenth apart are not the same round`() {
        val result = RouteMatch.compare(
            WalkedRoute(Ground.walk(Ground.LOOP, seed = 1), 3_200.0),
            WalkedRoute(Ground.walk(Ground.LOOP, seed = 2), 3_600.0),
        )
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.LENGTHS_APART)
        assertThat(RouteMatch.lengthsAlike(3_200.0, 3_520.0)).isTrue()
        assertThat(RouteMatch.lengthsAlike(3_200.0, 3_560.0)).isFalse()
    }

    @Test
    fun `a street walked out and back is the same round as itself, the way back after the way out`() {
        val result = compare(walked(Ground.STATION, seed = 1), walked(Ground.STATION, seed = 2))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.SAME)
    }

    @Test
    fun `a ride is not a line, the round on either side of it still matches`() {
        // The same loop, with a stretch begun anew half way (a tram for a few stops would lay none).
        val loop = Ground.walk(Ground.LOOP, seed = 5)
        val broken = loop.mapIndexed { index, point ->
            if (index == loop.size / 2) point.copy(kind = RoutePointKind.START) else point
        }
        val result = RouteMatch.compare(WalkedRoute(loop, 3_200.0), WalkedRoute(broken, 3_200.0))
        assertThat(result.verdict).isEqualTo(RouteMatch.Verdict.SAME)
    }

    @Test
    fun `no route, or a single point, is never the same round`() {
        val one: List<RoutePoint> = Ground.walk(Ground.LOOP).take(1)
        assertThat(RouteMatch.compare(WalkedRoute(emptyList(), 0.0), walked(Ground.LOOP, 1)).same).isFalse()
        assertThat(RouteMatch.compare(WalkedRoute(one, 3_200.0), walked(Ground.LOOP, 1)).same).isFalse()
    }

    @Test
    fun `a year of comparisons takes moments, not minutes`() {
        val a = walked(Ground.LOOP, seed = 1)
        val b = walked(Ground.LOOP, seed = 2)
        val started = System.nanoTime()
        repeat(365) { compare(a, b) }
        val millis = (System.nanoTime() - started) / 1_000_000
        println("365 comparisons of a 3.2 km round: $millis ms")
        assertThat(millis).isLessThan(15_000)
    }
}
