package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.WalkedRoute
import com.callbackdev.passo.core.model.Round
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.SessionIntensity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Two rounds that may be one, asked of the reader (PLANNING.md §15, «Phase 20: two rounds made
 * one»): a round whose walks all lost their routes, and the round its next walks made.
 */
class RoundMergeTest {
    /** [count] walks of [meters], a week apart from [firstDay], ids from [firstId]. */
    private fun round(
        id: Long,
        firstId: Long,
        count: Int,
        meters: Double = 4_260.0,
        lined: Boolean,
        name: String? = null,
        firstDay: Long = 0,
        intensity: SessionIntensity = SessionIntensity.FREE,
    ): KeptRound {
        val walks = (0 until count).map {
            Ground.outing(firstId + it, meters, 52.0 - it, day = firstDay + it * 7L, intensity = intensity)
        }
        val latest = walks.last()
        return KeptRound(
            round = Round(id, name, walks.map { it.id }.toSet()),
            walks = walks,
            usual = checkNotNull(RoundTimes.usual(walks)),
            route = if (lined) RouteKey(latest.id, latest.startedAtMillis, 1_400) else null,
            running = intensity == SessionIntensity.RUN,
        )
    }

    // «Along the canals»: ten walks, every route deleted; then three walks of it with GPS.
    private val canals = round(1, firstId = 1, count = 10, lined = false, name = "Along the canals")
    private val since = round(2, firstId = 20, count = 3, lined = true, firstDay = 100)

    @Test
    fun `the round its next walks made asks whether it is the one with no line, and that one asks back`() {
        val rounds = listOf(since, canals)
        assertThat(RoundMerge.proposal(since, rounds, emptySet())?.id).isEqualTo(1L)
        assertThat(RoundMerge.proposal(canals, rounds, emptySet())?.id).isEqualTo(2L)
    }

    @Test
    fun `two rounds with a line, or two with none, are never asked about`() {
        val other = round(3, firstId = 40, count = 3, lined = true, firstDay = 200)
        assertThat(RoundMerge.proposal(since, listOf(since, other), emptySet())).isNull()
        val lost = round(4, firstId = 60, count = 4, lined = false)
        assertThat(RoundMerge.proposal(canals, listOf(canals, lost), emptySet())).isNull()
    }

    @Test
    fun `lengths more than a tenth apart, or a run and a walk, are not asked about`() {
        val longer = round(3, firstId = 40, count = 3, meters = 4_800.0, lined = true, firstDay = 200)
        assertThat(RoundMerge.proposal(canals, listOf(canals, longer), emptySet())).isNull()
        val run = round(3, firstId = 40, count = 3, lined = true, firstDay = 200, intensity = SessionIntensity.RUN)
        assertThat(RoundMerge.proposal(canals, listOf(canals, run), emptySet())).isNull()
    }

    @Test
    fun `once the reader answered another round, any pair of their walks keeps it unasked`() {
        val rounds = listOf(since, canals)
        val answered = setOf(5L to 21L)
        assertThat(RoundMerge.proposal(since, rounds, answered)).isNull()
        assertThat(RoundMerge.proposal(canals, rounds, answered)).isNull()
        // A pair of other outings says nothing of these two.
        assertThat(RoundMerge.proposal(since, rounds, setOf(5L to 99L))?.id).isEqualTo(1L)
    }

    @Test
    fun `of two that may be it, the closest in length is asked first, then the one walked more`() {
        val near = round(3, firstId = 40, count = 3, meters = 4_250.0, lined = false)
        val far = round(4, firstId = 60, count = 6, meters = 4_000.0, lined = false)
        assertThat(RoundMerge.proposal(since, listOf(since, far, near), emptySet())?.id).isEqualTo(3L)
        val same = round(5, firstId = 80, count = 12, meters = 4_250.0, lined = false)
        assertThat(RoundMerge.proposal(since, listOf(since, near, same), emptySet())?.id).isEqualTo(5L)
    }

    @Test
    fun `two rounds with a line are one by the rule every walk is matched by, not by their lengths`() {
        val loop = listOf(WalkedRoute(Ground.walk(Ground.LOOP, seed = 1), Ground.length(Ground.LOOP)))
        val again = listOf(
            WalkedRoute(Ground.walk(Ground.LOOP_OTHER_WAY, seed = 2), Ground.length(Ground.LOOP)),
            WalkedRoute(Ground.walk(Ground.LOOP, seed = 3), Ground.length(Ground.LOOP)),
        )
        assertThat(RoundMerge.sameByLine(loop, again)).isTrue()
        // As long, from the same door, and another round: the other loop, and the same one the other way.
        val other = listOf(
            WalkedRoute(Ground.walk(Ground.OTHER_LOOP, seed = 4), Ground.length(Ground.OTHER_LOOP)),
            WalkedRoute(Ground.walk(Ground.LOOP_OTHER_WAY, seed = 5), Ground.length(Ground.LOOP)),
        )
        assertThat(RoundMerge.sameByLine(loop, other)).isFalse()
        assertThat(RoundMerge.sameByLine(loop, emptyList())).isFalse()
    }

    @Test
    fun `two rounds may be one by their numbers when both are walks or runs, their lengths within a tenth`() {
        assertThat(RoundMerge.mayBeOne(canals.walks, since.walks)).isTrue()
        val longer = round(3, firstId = 40, count = 3, meters = 4_800.0, lined = true)
        assertThat(RoundMerge.mayBeOne(canals.walks, longer.walks)).isFalse()
        val run = round(3, firstId = 40, count = 3, lined = true, intensity = SessionIntensity.RUN)
        assertThat(RoundMerge.mayBeOne(canals.walks, run.walks)).isFalse()
        assertThat(RoundMerge.mayBeOne(canals.walks, emptyList())).isFalse()
    }

    @Test
    fun `the first walk after every route was lost is asked whether it was the round, once`() {
        // «Along the canals» lost its routes; a walk of it with GPS, weeks after its last.
        val walk = Ground.outing(100, 4_250.0, 50.0, day = 200)
        assertThat(RoundMerge.lostFor(walk, listOf(canals, since), emptySet())?.id).isEqualTo(1L)
        // «It wasn't»: a pair with any of its walks, and the page asks no more.
        assertThat(RoundMerge.lostFor(walk, listOf(canals), setOf(5L to 100L))).isNull()
    }

    @Test
    fun `only a walk that can give the round its line back is asked`() {
        val rounds = listOf(canals, since)
        // Another kind, another length, measured under nine tenths, or walked before the round's last.
        val run = Ground.outing(100, 4_250.0, 25.0, day = 200, intensity = SessionIntensity.RUN)
        assertThat(RoundMerge.lostFor(run, rounds, emptySet())).isNull()
        assertThat(RoundMerge.lostFor(Ground.outing(100, 5_000.0, 60.0, day = 200), rounds, emptySet())).isNull()
        val estimated = Ground.outing(100, 4_250.0, 50.0, day = 200, measuredShare = 0.8)
        assertThat(RoundMerge.lostFor(estimated, rounds, emptySet())).isNull()
        assertThat(RoundMerge.lostFor(Ground.outing(100, 4_250.0, 50.0, day = 10), rounds, emptySet())).isNull()
        // A walk of a round already: the rounds' own question is for that.
        assertThat(RoundMerge.lostFor(since.latest, rounds, emptySet())).isNull()
        // No round with no line, nothing to ask.
        assertThat(RoundMerge.lostFor(Ground.outing(100, 4_250.0, 50.0, day = 200), listOf(since), emptySet()))
            .isNull()
    }

    @Test
    fun `of two rounds with no line, the walk is asked about the closest in length`() {
        val near = round(3, firstId = 40, count = 3, meters = 4_248.0, lined = false)
        val walk = Ground.outing(100, 4_250.0, 50.0, day = 200)
        assertThat(RoundMerge.lostFor(walk, listOf(canals, near), emptySet())?.id).isEqualTo(3L)
        // As close, the one walked more.
        val asClose = round(3, firstId = 40, count = 3, meters = 4_240.0, lined = false)
        assertThat(RoundMerge.lostFor(walk, listOf(canals, asClose), emptySet())?.id).isEqualTo(1L)
    }

    @Test
    fun `made one, they keep the name of the round with no line, else the other's`() {
        assertThat(RoundMerge.name(since, canals)).isEqualTo("Along the canals")
        assertThat(RoundMerge.name(canals, since)).isEqualTo("Along the canals")
        val unnamed = round(1, firstId = 1, count = 10, lined = false)
        val named = round(2, firstId = 20, count = 3, lined = true, firstDay = 100, name = "Canals again")
        assertThat(RoundMerge.name(named, unnamed)).isEqualTo("Canals again")
        assertThat(RoundMerge.name(since, unnamed)).isNull()
    }
}
