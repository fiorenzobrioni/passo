package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.RouteMatch
import com.callbackdev.passo.core.domain.routes.WalkedRoute
import com.callbackdev.passo.core.model.RouteBox
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Which round an outing with GPS is, when it ends (PLANNING.md §11 Phase 20, «The match»). */
class RoundFinderTest {
    private fun candidate(id: Long, corners: List<Pair<Double, Double>>, roundId: Long?, seed: Int = id.toInt()) =
        RoundCandidate(Ground.outing(id, Ground.length(corners), 40.0), roundId, Ground.walk(corners, seed = seed))

    private fun walked(corners: List<Pair<Double, Double>>, seed: Int = 99) =
        WalkedRoute(Ground.walk(corners, seed = seed), Ground.length(corners))

    @Test
    fun `an outing measured under nine tenths is not matched, its line too much the steps'`() {
        assertThat(RoundFinder.eligible(Ground.outing(1, 3_200.0, 40.0))).isTrue()
        assertThat(RoundFinder.eligible(Ground.outing(1, 3_200.0, 40.0, measuredShare = 0.85))).isFalse()
    }

    @Test
    fun `an outing under way, or shorter than half a kilometre, is no round's walk`() {
        assertThat(RoundFinder.eligible(Ground.outing(1, 3_200.0, 40.0).copy(state = SessionState.ACTIVE))).isFalse()
        assertThat(RoundFinder.eligible(Ground.outing(1, 450.0, 6.0))).isFalse()
        assertThat(
            RoundFinder.eligible(Ground.outing(1, 3_200.0, 40.0).copy(gps = false, measuredShare = null)),
        ).isFalse()
    }

    @Test
    fun `a run and a walk round the same park are two rounds`() {
        val walk = Ground.outing(1, 3_200.0, 40.0)
        val run = Ground.outing(2, 3_200.0, 20.0, intensity = SessionIntensity.RUN)
        assertThat(RoundFinder.mayBeSame(walk, run)).isFalse()
        assertThat(RoundFinder.mayBeSame(walk, Ground.outing(3, 3_100.0, 38.0, intensity = SessionIntensity.BRISK)))
            .isTrue()
    }

    @Test
    fun `routes whose boxes lie apart are not read at all`() {
        val park = RouteBox(45_464_200, 9_190_000, 45_471_400, 9_200_200)
        assertThat(RoundFinder.boxesAlike(park, park.copy(northE6 = park.northE6 + 1_000))).isTrue()
        // A street out and back, a few metres across: the receiver's wander does not set it apart.
        val street = RouteBox(45_464_200, 9_190_000, 45_464_250, 9_209_000)
        assertThat(RoundFinder.boxesAlike(street, street.copy(northE6 = street.northE6 + 300))).isTrue()
        // Another park, a kilometre away.
        assertThat(
            RoundFinder.boxesAlike(park, park.copy(southE6 = park.southE6 + 9_000, northE6 = park.northE6 + 9_000)),
        )
            .isFalse()
    }

    @Test
    fun `a walk takes with it every round and every walk in no round it is the same as`() {
        val choice = RoundFinder.choose(
            walked(Ground.LOOP),
            listOf(
                candidate(1, Ground.LOOP, roundId = null),
                candidate(2, Ground.LOOP, roundId = 7),
                candidate(3, Ground.LOOP, roundId = 8),
                candidate(4, Ground.OTHER_LOOP, roundId = 9),
            ),
            members = mapOf(7L to setOf(2L, 10L, 11L), 8L to setOf(3L, 12L, 13L), 9L to setOf(4L)),
        )
        // Two rounds of one walk's streets are one: the walk links them, and the walk in none.
        assertThat(choice).isInstanceOf(RoundChoice.Into::class.java)
        assertThat((choice as RoundChoice.Into).rounds).containsExactly(7L, 8L)
        assertThat(choice.walks).containsExactly(1L)
    }

    @Test
    fun `a round is three walks, a walk the same as one walk in no round makes none yet`() {
        val one = RoundFinder.choose(
            walked(Ground.LOOP),
            listOf(candidate(1, Ground.OTHER_LOOP, roundId = 5), candidate(2, Ground.LOOP, roundId = null)),
        )
        assertThat(one).isEqualTo(RoundChoice.None)
        val two = RoundFinder.choose(
            walked(Ground.LOOP),
            listOf(candidate(1, Ground.LOOP, roundId = null, seed = 4), candidate(2, Ground.LOOP, roundId = null)),
        )
        assertThat(two).isInstanceOf(RoundChoice.Into::class.java)
        assertThat((two as RoundChoice.Into).rounds).isEmpty()
        assertThat(two.walks).containsExactly(1L, 2L)
    }

    @Test
    fun `a third walk the same as two walks too far apart to match each other makes one round of the three`() {
        // The first with a detour of 300 m, the second with a shortcut: 3.5 km and 3.1 km, more than a
        // tenth apart, and so never matched with each other; each the same as the walk round the park.
        val detour = Ground.LOOP_WITH_DETOUR
        val shortcut = listOf(0.0 to 0.0, 640.0 to 0.0, 800.0 to 160.0, 800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0)
        assertThat(RouteMatch.compare(walked(detour), walked(shortcut)).same).isFalse()
        val candidates = listOf(candidate(1, detour, roundId = null), candidate(2, shortcut, roundId = null))
        val choice = RoundFinder.choose(walked(Ground.LOOP), candidates)
        assertThat((choice as RoundChoice.Into).walks).containsExactly(1L, 2L)
        // Two the reader said are not the same are never put in one round: and one walk is no round.
        assertThat(RoundFinder.choose(walked(Ground.LOOP), candidates, apart = setOf(1L to 2L)))
            .isEqualTo(RoundChoice.None)
    }

    @Test
    fun `a walk taken out of a round does not keep the next walk of it out, the round comes first`() {
        // Walk 1 was said not the same as the round's walks 10, 11 and 12; the next walk matches both.
        val choice = RoundFinder.choose(
            walked(Ground.LOOP),
            listOf(candidate(1, Ground.LOOP, roundId = null, seed = 99), candidate(10, Ground.LOOP, roundId = 7)),
            apart = setOf(1L to 10L, 1L to 11L, 1L to 12L),
            members = mapOf(7L to setOf(10L, 11L, 12L)),
        )
        assertThat(choice).isEqualTo(RoundChoice.Into(listOf(7L), emptyList()))
    }

    @Test
    fun `nothing said not the same is put in one round, the closer kept`() {
        val choice = RoundFinder.choose(
            walked(Ground.LOOP),
            listOf(candidate(2, Ground.LOOP, roundId = 7), candidate(3, Ground.LOOP, roundId = 8)),
            apart = setOf(10L to 12L),
            members = mapOf(7L to setOf(2L, 10L, 11L), 8L to setOf(3L, 12L, 13L)),
        )
        assertThat((choice as RoundChoice.Into).rounds).hasSize(1)
    }

    @Test
    fun `a walk that matches nothing is in no round`() {
        val choice = RoundFinder.choose(
            walked(Ground.LOOP_OTHER_WAY),
            listOf(candidate(1, Ground.LOOP, roundId = 3), candidate(2, Ground.OTHER_LOOP, roundId = null)),
        )
        assertThat(choice).isEqualTo(RoundChoice.None)
    }
}
