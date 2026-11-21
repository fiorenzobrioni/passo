package com.callbackdev.passo.core.domain.rounds

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
    fun `a walk that matches a round's walk joins the round rather than pairing with an outing alone`() {
        val choice = RoundFinder.choose(
            walked(Ground.LOOP),
            listOf(candidate(1, Ground.LOOP, roundId = null), candidate(2, Ground.LOOP, roundId = 7)),
        )
        assertThat(choice).isEqualTo(RoundChoice.Join(7))
    }

    @Test
    fun `a walk that matches an outing in no round makes a round of the two`() {
        val choice = RoundFinder.choose(
            walked(Ground.LOOP),
            listOf(candidate(1, Ground.OTHER_LOOP, roundId = 5), candidate(2, Ground.LOOP, roundId = null)),
        )
        assertThat(choice).isEqualTo(RoundChoice.Pair(2))
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
