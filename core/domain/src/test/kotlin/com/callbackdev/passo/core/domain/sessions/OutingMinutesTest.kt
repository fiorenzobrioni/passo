package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionPause
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * An outing minute by minute (PLANNING.md §11 Phase 21, step 21.3): the grouping, the pauses as
 * gaps, the pace, across midnight and over twelve hours. Each row of the step's edge cases that is
 * the minutes' says so in its name.
 */
class OutingMinutesTest {
    // 10:00 UTC on a day of November 2026, on a whole minute.
    private val start = 1_795_168_800_000L
    private val firstMinute = start / MINUTE

    private fun outing(
        minutes: Int,
        state: SessionState = SessionState.FINISHED,
        intensity: SessionIntensity = SessionIntensity.FREE,
        pauses: List<SessionPause> = emptyList(),
        pausedAt: Long? = null,
        reachedAt: Long? = null,
        kept: Boolean = true,
        startedAt: Long = start,
    ) = Session(
        planId = null,
        name = null,
        goalKind = SessionGoalKind.OPEN,
        goalValue = 0,
        intensity = intensity,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = startedAt / DAY,
        startedAtMillis = startedAt,
        state = state,
        endedAtMillis = if (state == SessionState.FINISHED) startedAt + minutes * MINUTE else null,
        lastStepAtMillis = startedAt + minutes * MINUTE,
        lastEventAtMillis = startedAt + minutes * MINUTE,
        pausedAtMillis = pausedAt,
        reachedAtMillis = reachedAt,
        pauses = pauses,
        pausesKept = kept,
    )

    /** [steps] for each minute from the outing's first, on the day each minute falls in. */
    private fun rows(vararg steps: Int, from: Long = firstMinute) = steps.mapIndexed { index, value ->
        MinuteSteps(from + index, (from + index) * MINUTE / DAY, value)
    }

    @Test
    fun `one value a minute, the steps in it, from the first minute to the last`() {
        val minutes = checkNotNull(OutingMinutes.of(outing(5), rows(80, 110, 120, 0, 95), nowMillis = 0))
        assertThat(minutes.steps).containsExactly(80, 110, 120, 0, 95).inOrder()
        assertThat(minutes.paused).containsExactly(false, false, false, false, false).inOrder()
    }

    @Test
    fun `a minute with no row is a minute of no steps, and rows outside the outing are not its own`() {
        val before = MinuteSteps(firstMinute - 1, firstMinute / (DAY / MINUTE), 300)
        val after = MinuteSteps(firstMinute + 4, firstMinute / (DAY / MINUTE), 300)
        val minutes = checkNotNull(
            OutingMinutes.of(outing(3), listOf(before, after) + rows(100, from = firstMinute + 1), nowMillis = 0),
        )
        assertThat(minutes.steps).containsExactly(0, 100, 0).inOrder()
    }

    @Test
    fun `an outing at any pace is at the pace from brisk walking, one at a pace from its own`() {
        val any = checkNotNull(OutingMinutes.of(outing(3), rows(99, 100, 129), nowMillis = 0))
        assertThat(any.floor).isEqualTo(100)
        assertThat(any.atPaceMinutes).isEqualTo(2)
        val vigorous = checkNotNull(
            OutingMinutes.of(outing(3, intensity = SessionIntensity.VIGOROUS), rows(99, 100, 130), nowMillis = 0),
        )
        assertThat(vigorous.floor).isEqualTo(130)
        assertThat(vigorous.atPaceMinutes).isEqualTo(1)
    }

    @Test
    fun `a pause is a gap in the strip, its steps the day's`() {
        val pause = SessionPause(start + 2 * MINUTE, start + 5 * MINUTE)
        val minutes = checkNotNull(
            OutingMinutes.of(outing(7, pauses = listOf(pause)), rows(100, 100, 40, 40, 40, 110, 110), nowMillis = 0),
        )
        assertThat(minutes.paused).containsExactly(false, false, true, true, true, false, false).inOrder()
        assertThat(minutes.steps).containsExactly(100, 100, 0, 0, 0, 110, 110).inOrder()
        assertThat(minutes.pauses).isEqualTo(1)
        val bars = minutes.bars(maxBars = 100)
        assertThat(bars.map { it.paused }).containsExactly(false, false, true, true, true, false, false).inOrder()
    }

    @Test
    fun `a pause of a few seconds is no gap, a minute mostly paused is one`() {
        val short = SessionPause(start + 2 * MINUTE + 10_000, start + 2 * MINUTE + 25_000)
        val long = SessionPause(start + 4 * MINUTE + 15_000, start + 5 * MINUTE)
        val minutes = checkNotNull(
            OutingMinutes.of(outing(6, pauses = listOf(short, long)), rows(1, 1, 1, 1, 1, 1), nowMillis = 0),
        )
        assertThat(minutes.paused).containsExactly(false, false, false, false, true, false).inOrder()
    }

    @Test
    fun `the pause under way is a gap to now`() {
        val pausedAt = start + 3 * MINUTE
        val session = outing(3, state = SessionState.PAUSED, pausedAt = pausedAt)
        val minutes = checkNotNull(
            OutingMinutes.of(session, rows(100, 100, 100, 60, 60), nowMillis = start + 5 * MINUTE),
        )
        assertThat(minutes.count).isEqualTo(5)
        assertThat(minutes.paused).containsExactly(false, false, false, true, true).inOrder()
        assertThat(minutes.pauses).isEqualTo(1)
        assertThat(minutes.live).isTrue()
    }

    @Test
    fun `an outing under way runs to now, and keeps half an hour of room to grow into`() {
        val session = outing(4, state = SessionState.ACTIVE)
        val minutes = checkNotNull(OutingMinutes.of(session, rows(100, 100, 100, 100), nowMillis = start + 4 * MINUTE))
        assertThat(minutes.count).isEqualTo(4)
        assertThat(minutes.spanMinutes).isEqualTo(OutingMinutes.MIN_LIVE_SPAN_MINUTES)
        val over = checkNotNull(OutingMinutes.of(outing(4), rows(100, 100, 100, 100), nowMillis = 0))
        assertThat(over.spanMinutes).isEqualTo(OutingMinutes.MIN_SPAN_MINUTES)
        val long = checkNotNull(OutingMinutes.of(outing(45), rows(*IntArray(45) { 100 }), nowMillis = 0))
        assertThat(long.spanMinutes).isEqualTo(45)
    }

    @Test
    fun `bars are grouped by one, two, five, ten or fifteen minutes, the smallest that fits`() {
        val hour = checkNotNull(OutingMinutes.of(outing(60), rows(*IntArray(60) { 100 }), nowMillis = 0))
        assertThat(hour.groupFor(maxBars = 110)).isEqualTo(1)
        assertThat(hour.groupFor(maxBars = 59)).isEqualTo(2)
        assertThat(hour.groupFor(maxBars = 29)).isEqualTo(5)
        assertThat(hour.groupFor(maxBars = 11)).isEqualTo(10)
        assertThat(hour.groupFor(maxBars = 5)).isEqualTo(15)
        assertThat(hour.groupFor(maxBars = 1)).isEqualTo(15)
    }

    @Test
    fun `a bar is its minutes' mean, at the pace when half of them are, the last one still filling`() {
        val minutes = checkNotNull(
            OutingMinutes.of(outing(7), rows(120, 120, 60, 60, 100, 90, 130), nowMillis = 0),
        )
        val bars = minutes.bars(maxBars = 5)
        assertThat(minutes.groupFor(5)).isEqualTo(2)
        assertThat(bars.map { it.minutes }).containsExactly(2, 2, 2, 1).inOrder()
        assertThat(bars.map { it.from }).containsExactly(0, 2, 4, 6).inOrder()
        assertThat(bars.map { it.stepsPerMinute }).containsExactly(120.0, 60.0, 95.0, 130.0).inOrder()
        assertThat(bars.map { it.atPace }).containsExactly(true, false, true, true).inOrder()
    }

    @Test
    fun `a bar partly paused is the mean of its minutes walked, a gap only when mostly paused`() {
        val pause = SessionPause(start + 4 * MINUTE, start + 7 * MINUTE)
        val minutes = checkNotNull(
            OutingMinutes.of(
                outing(10, pauses = listOf(pause)),
                rows(100, 100, 100, 100, 9, 9, 9, 120, 120, 120),
                nowMillis = 0,
            ),
        )
        val bars = minutes.bars(maxBars = 2)
        assertThat(minutes.groupFor(2)).isEqualTo(5)
        assertThat(bars[0].paused).isFalse()
        assertThat(bars[0].stepsPerMinute).isEqualTo(100.0)
        assertThat(bars[1].paused).isFalse()
        assertThat(bars[1].stepsPerMinute).isEqualTo(120.0)
        val threeOfFive = checkNotNull(
            OutingMinutes.of(
                outing(5, pauses = listOf(SessionPause(start + MINUTE, start + 4 * MINUTE))),
                rows(100, 1, 1, 1, 100),
                nowMillis = 0,
            ),
        )
        assertThat(threeOfFive.bars(maxBars = 1).single().paused).isTrue()
    }

    @Test
    fun `across midnight, the minutes of both days`() {
        // 23:50 local in UTC, ten minutes before the day ends, and twenty after.
        val lateStart = (start / DAY + 1) * DAY - 10 * MINUTE
        val first = lateStart / MINUTE
        val session = outing(30, startedAt = lateStart)
        val minutes = checkNotNull(OutingMinutes.of(session, rows(*IntArray(30) { 100 + it }, from = first), 0))
        assertThat(minutes.count).isEqualTo(30)
        assertThat(minutes.steps.first()).isEqualTo(100)
        assertThat(minutes.steps.last()).isEqualTo(129)
        assertThat(rows(*IntArray(30) { 1 }, from = first).map { it.localEpochDay }.distinct()).hasSize(2)
    }

    @Test
    fun `a twelve-hour walk is grouped, and read in a moment`() {
        val twelveHours = 12 * 60
        val began = System.nanoTime()
        val minutes = checkNotNull(
            OutingMinutes.of(outing(twelveHours), rows(*IntArray(twelveHours) { 90 + it % 40 }), nowMillis = 0),
        )
        // A phone's strip, some 330 dp wide, a bar of two and a gap of one.
        val bars = minutes.bars(maxBars = 110)
        val took = (System.nanoTime() - began) / 1_000_000.0
        println("OutingMinutes: twelve hours read and grouped in %.1f ms".format(took))
        assertThat(minutes.groupFor(110)).isEqualTo(10)
        assertThat(bars).hasSize(72)
        assertThat(bars.all { it.minutes == 10 }).isTrue()
    }

    @Test
    fun `the minute the goal was reached is kept, where the strip marks it`() {
        val minutes = checkNotNull(
            OutingMinutes.of(outing(50, reachedAt = start + 30 * MINUTE + 30_000), rows(*IntArray(50) { 110 }), 0),
        )
        assertThat(minutes.goalAt).isWithin(1e-9).of(30.5)
        val none = checkNotNull(OutingMinutes.of(outing(5), rows(100), 0))
        assertThat(none.goalAt).isNull()
    }

    @Test
    fun `an outing walked before its pauses were kept has no minutes to draw`() {
        assertThat(OutingMinutes.of(outing(10, kept = false), rows(100, 100), nowMillis = 0)).isNull()
    }

    @Test
    fun `the strip's top is the tallest bar, and never under a quarter above the floor`() {
        val slow = checkNotNull(OutingMinutes.of(outing(2), rows(60, 70), 0))
        assertThat(slow.scaleTop(slow.bars(10))).isEqualTo(125.0)
        val run = checkNotNull(OutingMinutes.of(outing(2), rows(160, 170), 0))
        assertThat(run.scaleTop(run.bars(10))).isEqualTo(170.0)
    }

    private companion object {
        const val MINUTE = 60_000L
        const val DAY = 86_400_000L
    }
}
