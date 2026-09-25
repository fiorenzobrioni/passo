package com.callbackdev.passo.core.domain.history

import com.callbackdev.passo.core.model.DailySummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PeriodOverviewTest {
    private val today = LocalDate.of(2026, 9, 24) // a Thursday

    private fun day(date: LocalDate, steps: Int, goal: Int = 8_000) = DailySummary(
        localEpochDay = date.toEpochDay(),
        steps = steps,
        distanceMeters = steps * 0.7,
        activeKcal = steps * 0.03,
        activeMinutes = steps / 200,
        briskMinutes = steps / 400,
        goalSteps = goal,
        finalized = date.isBefore(today),
    )

    private fun map(vararg days: DailySummary) = days.associateBy { it.localEpochDay }

    private val week = Period.containing(PeriodScale.WEEK, today, DayOfWeek.MONDAY)

    @Test
    fun `a week in progress draws its days so far and nothing after today`() {
        val days = map(
            day(today.minusDays(3), 9_000), // Monday
            day(today.minusDays(2), 4_000),
            day(today, 2_500),
        )
        val overview = PeriodOverview.of(week, days, today, today.minusDays(30), 8_000)
        assertThat(overview.bars.map { it.steps }).containsExactly(9_000, 4_000, 0, 2_500, null, null, null).inOrder()
        assertThat(
            overview.bars.map {
                it.met
            },
        ).containsExactly(true, false, false, false, false, false, false).inOrder()
        assertThat(overview.bars[3].current).isTrue()
        assertThat(overview.countedDays).isEqualTo(4)
        assertThat(overview.goalDays).isEqualTo(1)
        assertThat(overview.totals.steps).isEqualTo(15_500)
        assertThat(overview.dailyAverage).isEqualTo(3_875)
        assertThat(overview.inProgress).isTrue()
        assertThat(overview.bestBar).isEqualTo(0)
    }

    @Test
    fun `days before counting began are not counted and not drawn`() {
        val first = today.minusDays(1)
        val overview = PeriodOverview.of(week, map(day(first, 6_000), day(today, 1_000)), today, first, 8_000)
        assertThat(overview.bars.map { it.steps }).containsExactly(null, null, 6_000, 1_000, null, null, null).inOrder()
        assertThat(overview.countedDays).isEqualTo(2)
        assertThat(overview.dailyAverage).isEqualTo(3_500)
        assertThat(overview.previousAverage).isNull()
        assertThat(overview.change).isNull()
    }

    @Test
    fun `each day is measured against its own goal`() {
        val days = map(
            day(today.minusDays(3), 7_000, goal = 6_000),
            day(today.minusDays(2), 7_000, goal = 10_000),
        )
        val overview = PeriodOverview.of(week, days, today, today.minusDays(3), 12_000)
        assertThat(overview.bars[0].goal).isEqualTo(6_000)
        assertThat(overview.bars[0].met).isTrue()
        assertThat(overview.bars[1].goal).isEqualTo(10_000)
        assertThat(overview.bars[1].met).isFalse()
        // A day with no record stands on the current goal.
        assertThat(overview.bars[2].goal).isEqualTo(12_000)
        assertThat(overview.goalDays).isEqualTo(1)
    }

    @Test
    fun `the change is against the previous week's daily average`() {
        val lastMonday = week.start.minusDays(7)
        val previous = (0 until 7).map { day(lastMonday.plusDays(it.toLong()), 5_000) }
        val current = (0 until 4).map { day(week.start.plusDays(it.toLong()), 6_000) }
        val overview = PeriodOverview.of(week, map(*(previous + current).toTypedArray()), today, lastMonday, 8_000)
        assertThat(overview.previousAverage).isEqualTo(5_000)
        assertThat(overview.dailyAverage).isEqualTo(6_000)
        assertThat(overview.change).isWithin(1e-9).of(0.2)
    }

    @Test
    fun `a year draws each month's daily average against its average goal`() {
        val year = Period.containing(PeriodScale.YEAR, today, DayOfWeek.MONDAY)
        val first = LocalDate.of(2026, 8, 1)
        val august = (0 until 31).map { day(first.plusDays(it.toLong()), if (it < 10) 10_000 else 0, goal = 8_000) }
        val overview = PeriodOverview.of(year, map(*august.toTypedArray()), today, first, 8_000)
        assertThat(overview.bars).hasSize(12)
        assertThat(overview.bars[6].steps).isNull() // July: before counting began
        assertThat(overview.bars[7].steps).isEqualTo(100_000 / 31 + 1) // 3,225.8 rounds up
        assertThat(overview.bars[7].goalDays).isEqualTo(10)
        assertThat(overview.bars[7].met).isFalse()
        // September so far: 24 days with nothing recorded count as zero.
        assertThat(overview.bars[8].steps).isEqualTo(0)
        assertThat(overview.bars[8].current).isTrue()
        assertThat(overview.bars[9].steps).isNull()
        assertThat(overview.countedDays).isEqualTo(31 + 24)
    }

    @Test
    fun `nothing recorded yet`() {
        val overview = PeriodOverview.of(week, emptyMap(), today, null, 8_000)
        assertThat(overview.bars.all { it.steps == null }).isTrue()
        assertThat(overview.countedDays).isEqualTo(0)
        assertThat(overview.dailyAverage).isNull()
        assertThat(overview.bestBar).isNull()
    }
}
