package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.model.Round
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.SessionIntensity
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The reader's usual rounds as «Your rounds» lists them (PLANNING.md §11 Phase 20). */
class YourRoundsTest {
    private val walks = (1L..6L).map { Ground.outing(it, 3_200.0, 40.0 - it * 2, day = it * 7) }
    private fun key(id: Long, points: Int = 500) = RouteKey(id, walks.first { it.id == id }.startedAtMillis, points)

    @Test
    fun `a round is shown from its third walk, a pair is not yet a usual`() {
        val rounds = listOf(Round(1, null, setOf(1, 2)), Round(2, "The park", setOf(3, 4, 5)))
        val listed = YourRounds.of(rounds, walks, emptyList())
        assertThat(listed.map { it.id }).containsExactly(2L)
    }

    @Test
    fun `the round most recently walked comes first`() {
        val rounds = listOf(Round(1, null, setOf(4, 5, 6)), Round(2, null, setOf(1, 2, 3)))
        assertThat(YourRounds.of(rounds, walks, emptyList()).map { it.id }).containsExactly(1L, 2L).inOrder()
        val reversed = listOf(Round(1, null, setOf(1, 2, 3)), Round(2, null, setOf(4, 5, 6)))
        assertThat(YourRounds.of(reversed, walks, emptyList()).map { it.id }).containsExactly(2L, 1L).inOrder()
    }

    @Test
    fun `a round's map is its latest walk's route, and another's once that one is deleted`() {
        val round = listOf(Round(1, null, setOf(1, 2, 3)))
        assertThat(YourRounds.of(round, walks, listOf(key(1), key(3))).single().route?.sessionId).isEqualTo(3L)
        assertThat(YourRounds.of(round, walks, listOf(key(1), key(2))).single().route?.sessionId).isEqualTo(2L)
        // A route of one point draws no line; one of another outing that had the id is not this one's.
        assertThat(YourRounds.of(round, walks, listOf(key(3, points = 1))).single().route).isNull()
        assertThat(YourRounds.of(round, walks, listOf(key(3).copy(sessionStartedAtMillis = 1))).single().route).isNull()
    }

    @Test
    fun `every walk's route deleted, the round keeps its history without a map`() {
        val kept = YourRounds.of(listOf(Round(1, null, setOf(1, 2, 3))), walks, emptyList()).single()
        assertThat(kept.route).isNull()
        assertThat(kept.walks).hasSize(3)
        assertThat(kept.usual.walks).isEqualTo(3)
    }

    @Test
    fun `a walk's outing deleted leaves the round, and under three it is no longer shown`() {
        val round = listOf(Round(1, null, setOf(1, 2, 3)))
        assertThat(YourRounds.of(round, walks.filter { it.id != 2L }, emptyList())).isEmpty()
    }

    @Test
    fun `the trend says how the last walks went against the usual`() {
        // 38, 36, 34, 32, 30, 28 minutes: the usual is 33; the last three are under it by more than its fiftieth.
        val kept = YourRounds.of(listOf(Round(1, null, walks.map { it.id }.toSet())), walks, emptyList()).single()
        assertThat(
            kept.trend,
        ).isEqualTo(
            RoundTrend(walks = 6, sinceDay = walks.first().localEpochDay, lastRun = 3, last = RoundVerdict.FASTER),
        )
        assertThat(kept.times.map { it.millis / 60_000 }).containsExactly(38L, 36L, 34L, 32L, 30L, 28L).inOrder()
    }

    @Test
    fun `a round of runs says so`() {
        val runs = (1L..3L).map { Ground.outing(it, 5_000.0, 25.0, day = it, intensity = SessionIntensity.RUN) }
        assertThat(YourRounds.of(listOf(Round(1, null, setOf(1, 2, 3))), runs, emptyList()).single().running).isTrue()
    }

    @Test
    fun `the round of an outing is found among the rounds shown`() {
        val listed = YourRounds.of(listOf(Round(4, null, setOf(1, 2, 3))), walks, emptyList())
        assertThat(YourRounds.of(2, listed)?.id).isEqualTo(4L)
        assertThat(YourRounds.of(6, listed)).isNull()
    }
}
