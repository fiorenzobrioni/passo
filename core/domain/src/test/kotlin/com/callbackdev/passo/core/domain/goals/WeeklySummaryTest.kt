package com.callbackdev.passo.core.domain.goals

import com.callbackdev.passo.core.domain.insights.Trend
import com.callbackdev.passo.core.model.DailySummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class WeeklySummaryTest {
    // Monday 28 September 2026: the summary is of 21 to 27 September.
    private val today = LocalDate.of(2026, 9, 28)
    private val lastMonday = LocalDate.of(2026, 9, 21)

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

    private fun week(start: LocalDate, vararg steps: Int) = steps.mapIndexed { i, s ->
        day(start.plusDays(i.toLong()), s)
    }

    private fun summary(
        days: List<DailySummary>,
        first: LocalDate? = days.minOfOrNull {
            LocalDate.ofEpochDay(it.localEpochDay)
        },
    ) = WeeklySummary.ofLastWeek(days.associateBy { it.localEpochDay }, today, DayOfWeek.MONDAY, first, 8_000)

    @Test
    fun `it sums the week that ended, and compares its average with the week before`() {
        val before = week(lastMonday.minusWeeks(1), 7_000, 7_000, 7_000, 7_000, 7_000, 7_000, 7_000)
        val last = week(lastMonday, 9_000, 6_000, 8_500, 12_040, 7_000, 10_000, 5_000)
        val summary = summary(before + last + day(today, 1_200))

        checkNotNull(summary)
        assertThat(summary.week.start).isEqualTo(lastMonday)
        assertThat(summary.week.end).isEqualTo(LocalDate.of(2026, 9, 27))
        assertThat(summary.steps).isEqualTo(57_540)
        assertThat(summary.countedDays).isEqualTo(7)
        assertThat(summary.goalDays).isEqualTo(4)
        assertThat(summary.dailyAverage).isEqualTo(8_220)
        assertThat(summary.previousAverage).isEqualTo(7_000)
        assertThat(summary.bestDay).isEqualTo(LocalDate.of(2026, 9, 24))
        assertThat(summary.bestDaySteps).isEqualTo(12_040)
        assertThat(summary.distanceMeters).isWithin(0.01).of(57_540 * 0.7)
        val headline = summary.headline as WeekHeadline.VersusWeekBefore
        assertThat(headline.trend).isEqualTo(Trend.UP)
        assertThat(headline.change).isWithin(0.001).of(0.174)
    }

    @Test
    fun `a week at the goal every day says so first`() {
        val before = week(lastMonday.minusWeeks(1), 20_000, 20_000, 20_000, 20_000, 20_000, 20_000, 20_000)
        val last = week(lastMonday, 8_000, 9_000, 8_100, 8_000, 11_000, 8_500, 8_000)

        assertThat(summary(before + last)?.headline).isEqualTo(WeekHeadline.EveryDay)
    }

    @Test
    fun `each day is measured against its own goal`() {
        val last = week(lastMonday, 8_000, 8_000, 8_000, 8_000, 8_000, 8_000).toMutableList()
        last += day(lastMonday.plusDays(6), 9_000, goal = 10_000)

        val summary = checkNotNull(summary(last, first = lastMonday.minusWeeks(3)))
        assertThat(summary.goalDays).isEqualTo(6)
    }

    @Test
    fun `the week counting began counts from its first day, with no week before to compare`() {
        val last = listOf(day(LocalDate.of(2026, 9, 24), 9_000), day(LocalDate.of(2026, 9, 25), 4_000))
        val summary = checkNotNull(summary(last, first = LocalDate.of(2026, 9, 24)))

        assertThat(summary.countedDays).isEqualTo(4)
        assertThat(summary.dailyAverage).isEqualTo(3_250)
        assertThat(summary.previousAverage).isNull()
        assertThat(summary.headline).isEqualTo(WeekHeadline.GoalDays(goalDays = 1, countedDays = 4))
    }

    @Test
    fun `a week before with no steps is no comparison`() {
        val last = week(lastMonday, 5_000, 5_000, 5_000, 5_000, 5_000, 5_000, 5_000)
        val summary = checkNotNull(summary(last, first = lastMonday.minusWeeks(2)))

        assertThat(summary.headline).isEqualTo(WeekHeadline.GoalDays(goalDays = 0, countedDays = 7))
    }

    @Test
    fun `a week with no steps, or from before counting began, has no summary`() {
        assertThat(summary(emptyList(), first = lastMonday.minusWeeks(2))).isNull()
        assertThat(summary(listOf(day(today, 3_000)))).isNull()
    }
}
