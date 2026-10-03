package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class IntervalScheduleTest {
    private val minute = 60_000L
    private val japanese = IntervalSchedule(IntervalSets())

    @Test
    fun `the protocol is five sets of three slow and three fast minutes, slow first`() {
        assertThat(japanese.count).isEqualTo(10)
        assertThat(japanese.totalMillis).isEqualTo(30 * minute)
        assertThat(japanese.isFast(0)).isFalse()
        assertThat(japanese.isFast(1)).isTrue()
        assertThat(japanese.startOf(1)).isEqualTo(3 * minute)
        assertThat(japanese.startOf(2)).isEqualTo(6 * minute)
        assertThat(japanese.startOf(9)).isEqualTo(27 * minute)
    }

    @Test
    fun `a change belongs to the interval it begins`() {
        assertThat(japanese.indexAt(0)).isEqualTo(0)
        assertThat(japanese.indexAt(3 * minute - 1)).isEqualTo(0)
        assertThat(japanese.indexAt(3 * minute)).isEqualTo(1)
        assertThat(japanese.indexAt(30 * minute)).isEqualTo(9)
        assertThat(japanese.indexAt(40 * minute)).isEqualTo(9)
        val position = japanese.at(4 * minute)
        assertThat(position.fast).isTrue()
        assertThat(position.set).isEqualTo(1)
        assertThat(position.elapsedMillis).isEqualTo(minute)
        assertThat(position.leftMillis).isEqualTo(2 * minute)
    }

    @Test
    fun `uneven intervals fall where their minutes put them`() {
        val schedule = IntervalSchedule(IntervalSets(slowMinutes = 2, fastMinutes = 1, sets = 3))
        assertThat(schedule.totalMillis).isEqualTo(9 * minute)
        assertThat((0 until schedule.count).map { schedule.startOf(it) / minute })
            .containsExactly(0L, 2L, 3L, 5L, 6L, 8L).inOrder()
        assertThat(schedule.indexAt(8 * minute + 30_000)).isEqualTo(5)
    }

    @Test
    fun `the end of the last interval is the goal, not a change`() {
        assertThat(japanese.changesBetween(0, 30 * minute)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9).inOrder()
        assertThat(japanese.untilChange(27 * minute)).isNull()
        assertThat(japanese.untilSignal(27 * minute)).isEqualTo(3 * minute)
        assertThat(japanese.untilSignal(30 * minute)).isNull()
        assertThat(japanese.untilChange(2 * minute + 50_000)).isEqualTo(10_000)
    }

    @Test
    fun `a batch across a change is cut at it, steps shared as its time`() {
        // 20 s and 36 steps, from 10 s before the change: half and half.
        val splits = japanese.lay(
            emptyList(),
            fromMillis = 3 * minute - 10_000,
            added = SessionTotals(steps = 36, movingMillis = 20_000, zoneMillis = 20_000),
        )
        assertThat(splits).containsExactly(
            IntervalSplit(index = 0, fast = false, steps = 18, movingMillis = 10_000, zoneMillis = 10_000),
            IntervalSplit(index = 1, fast = true, steps = 18, movingMillis = 10_000, zoneMillis = 10_000),
        ).inOrder()
    }

    @Test
    fun `a batch across two changes leaves every split right, and nothing past the end`() {
        val splits = japanese.lay(
            emptyList(),
            fromMillis = 23 * minute,
            added = SessionTotals(steps = 1_000, movingMillis = 10 * minute),
        )
        assertThat(splits.map { it.index }).containsExactly(7, 8, 9).inOrder()
        assertThat(splits.map { it.movingMillis }).containsExactly(minute, 3 * minute, 3 * minute).inOrder()
        // Only the seven minutes inside the schedule are laid; the steps follow their share.
        assertThat(splits.sumOf { it.steps }).isEqualTo(700)
    }

    @Test
    fun `a fast interval's cadence is its steps over its time in motion`() {
        assertThat(IntervalSplit(1, true, steps = 330, movingMillis = 3 * minute).cadence()).isEqualTo(110)
        assertThat(IntervalSplit(1, true, steps = 10, movingMillis = 10_000).cadence()).isNull()
    }

    @Test
    fun `a fast interval below its pace is counted, and said as below`() {
        val splits = (0 until 10).map { index ->
            val fast = index % 2 == 1
            // The third fast interval at 92: below the brisk 100.
            val cadence = if (index == 5) {
                92
            } else if (fast) {
                112
            } else {
                90
            }
            IntervalSplit(index, fast, steps = cadence * 3, movingMillis = 3 * minute)
        }
        val result = session(splits = splits).intervalResult()
        assertThat(result).isEqualTo(IntervalResult(atPace = 4, judged = 5, sets = 5))
    }

    @Test
    fun `a fast interval barely begun is not judged`() {
        val splits = listOf(
            IntervalSplit(0, false, 270, 3 * minute),
            IntervalSplit(1, true, 330, 3 * minute),
            IntervalSplit(2, false, 270, 3 * minute),
            IntervalSplit(3, true, 20, 20_000),
        )
        assertThat(session(splits = splits).intervalResult()).isEqualTo(IntervalResult(1, 1, 5))
    }

    @Test
    fun `the countdown glides from the last step for a few seconds, never past the change`() {
        val at = 1_000_000L
        val walking = session(moving = 2 * minute + 50_000, lastStepAt = at)
        assertThat(walking.intervalAt(at + 2_000)?.leftMillis).isEqualTo(8_000)
        // A stop holds it once the glide is spent.
        assertThat(
            walking.intervalAt(at + 60_000)?.leftMillis,
        ).isEqualTo(10_000 - SessionConstants.COUNTDOWN_GLIDE_MILLIS)
        val close = session(moving = 3 * minute - 1_000, lastStepAt = at)
        val position = close.intervalAt(at + 4_000)
        assertThat(position?.index).isEqualTo(0)
        assertThat(position?.leftMillis).isEqualTo(0)
        // Paused, it stands where it was.
        val paused = walking.copy(state = SessionState.PAUSED)
        assertThat(paused.intervalAt(at + 4_000)?.leftMillis).isEqualTo(10_000)
    }

    @Test
    fun `the Japanese walking preset starts as the protocol, its goal the end of the last set`() {
        val started = SessionPlans.start(SessionPlans.JAPANESE_WALKING, 0, 20_000, 0, 8_000)
        assertThat(started?.goalKind).isEqualTo(SessionGoalKind.INTERVALS)
        assertThat(started?.goalValue).isEqualTo(30)
        assertThat(started?.intervals).isEqualTo(IntervalSets())
        assertThat(started?.milestones).isEmpty()
    }

    @Test
    fun `an interval plan is kept in range and never free`() {
        val plan = SessionPlans.withIntervals(
            SessionPlan(goalKind = SessionGoalKind.TIME, goalValue = 20, intensity = SessionIntensity.FREE),
            IntervalSets(slowMinutes = 9, fastMinutes = 0, sets = 20),
        )
        assertThat(plan.intervals).isEqualTo(IntervalSets(slowMinutes = 5, fastMinutes = 1, sets = 10))
        assertThat(plan.goalValue).isEqualTo(60)
        assertThat(plan.intensity).isEqualTo(SessionIntensity.BRISK)
    }

    @Test
    fun `an interval outing is estimated slow and fast, each at its cadence`() {
        val lengths = StepLengths(walkingMeters = 0.7, runningMeters = 0.9)
        val estimate = SessionPlans.estimate(SessionPlans.JAPANESE_WALKING, lengths, 0)
        // 15 minutes at 90 and 15 at 100.
        assertThat(estimate.steps).isEqualTo(15 * 90 + 15 * 100)
        assertThat(estimate.minutes).isEqualTo(30)
    }

    private fun session(splits: List<IntervalSplit> = emptyList(), moving: Long = 0, lastStepAt: Long = 0) = Session(
        planId = null,
        name = null,
        goalKind = SessionGoalKind.INTERVALS,
        goalValue = 30,
        intensity = SessionIntensity.BRISK,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = 20_000,
        startedAtMillis = 0,
        totals = SessionTotals(movingMillis = moving),
        lastStepAtMillis = lastStepAt,
        intervals = IntervalSets(),
        splits = splits,
    )
}
