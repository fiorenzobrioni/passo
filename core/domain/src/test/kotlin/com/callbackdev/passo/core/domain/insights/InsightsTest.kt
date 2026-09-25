package com.callbackdev.passo.core.domain.insights

import com.callbackdev.passo.core.model.DailySummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

class InsightsTest {
    private val today = LocalDate.of(2026, 9, 24) // a Thursday

    private fun day(daysAgo: Int, steps: Int, goal: Int = 8_000): DailySummary {
        val date = today.minusDays(daysAgo.toLong())
        return DailySummary(
            localEpochDay = date.toEpochDay(),
            steps = steps,
            distanceMeters = steps * 0.7,
            activeKcal = steps * 0.03,
            activeMinutes = 0,
            briskMinutes = 0,
            goalSteps = goal,
            finalized = daysAgo > 0,
        )
    }

    private fun insights(vararg days: DailySummary, firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY) =
        Insights.of(days.associateBy { it.localEpochDay }, today, firstDayOfWeek)

    @Test
    fun `nothing recorded yet`() {
        val insights = insights()
        assertThat(insights.headline).isEqualTo(InsightsHeadline.NothingYet)
        assertThat(insights.currentStreak).isEqualTo(Streak.NONE)
        assertThat(insights.bestDay).isNull()
        assertThat(insights.averageAll).isNull()
    }

    @Test
    fun `today not yet at the goal does not break the streak`() {
        val insights = insights(day(3, 9_000), day(2, 8_500), day(1, 8_000), day(0, 1_200))
        assertThat(insights.currentStreak.days).isEqualTo(3)
        assertThat(insights.currentStreak.start).isEqualTo(today.minusDays(3))
        assertThat(insights.currentStreak.end).isEqualTo(today.minusDays(1))
        assertThat(insights.todayInStreak).isFalse()
        assertThat(insights.headline).isEqualTo(InsightsHeadline.OnAStreak(3, includesToday = false))
    }

    @Test
    fun `today joins the streak once its goal is met`() {
        val insights = insights(day(1, 8_000), day(0, 8_200))
        assertThat(insights.currentStreak.days).isEqualTo(2)
        assertThat(insights.currentStreak.end).isEqualTo(today)
        assertThat(insights.todayInStreak).isTrue()
    }

    @Test
    fun `a day with no record breaks a streak`() {
        val insights = insights(day(4, 9_000), day(3, 9_000), day(1, 9_000))
        assertThat(insights.currentStreak.days).isEqualTo(1)
        assertThat(insights.longestStreak.days).isEqualTo(2)
        assertThat(insights.longestStreak.start).isEqualTo(today.minusDays(4))
    }

    @Test
    fun `a goal change across days, each day keeping its own goal`() {
        // Met at 6,000, then the goal rose to 10,000 and 9,000 no longer meets it.
        val insights = insights(
            day(4, 6_500, goal = 6_000),
            day(3, 6_100, goal = 6_000),
            day(2, 9_000, goal = 10_000),
            day(1, 10_400, goal = 10_000),
            day(0, 11_000, goal = 10_000),
        )
        assertThat(insights.longestStreak.days).isEqualTo(2)
        assertThat(insights.longestStreak.start).isEqualTo(today.minusDays(4))
        assertThat(insights.currentStreak.days).isEqualTo(2)
        assertThat(insights.goalDays).isEqualTo(4)
        // Lowering the goal later would not rewrite a day: the goal is stored with it.
        val lowered = insights(day(2, 9_000, goal = 10_000), day(1, 9_000, goal = 8_000), day(0, 0, goal = 8_000))
        assertThat(lowered.currentStreak.days).isEqualTo(1)
    }

    @Test
    fun `the longest streak is the earliest of equals`() {
        val insights = insights(day(6, 9_000), day(5, 9_000), day(4, 0), day(2, 9_000), day(1, 9_000))
        assertThat(insights.longestStreak.days).isEqualTo(2)
        assertThat(insights.longestStreak.start).isEqualTo(today.minusDays(6))
    }

    @Test
    fun `records of day, week and month`() {
        val insights = insights(
            day(40, 12_000), // 15 August
            day(39, 3_000),
            day(10, 14_280), // 14 September, a Monday
            day(9, 9_000),
            day(3, 7_000),
            day(0, 2_000),
        )
        assertThat(insights.bestDay).isEqualTo(Record(14_280, today.minusDays(10), today.minusDays(10)))
        val week = checkNotNull(insights.bestWeek)
        assertThat(week.steps).isEqualTo(23_280)
        assertThat(week.start).isEqualTo(LocalDate.of(2026, 9, 14))
        assertThat(week.end).isEqualTo(LocalDate.of(2026, 9, 20))
        val month = checkNotNull(insights.bestMonth)
        assertThat(month.steps).isEqualTo(14_280L + 9_000 + 7_000 + 2_000)
        assertThat(month.start).isEqualTo(LocalDate.of(2026, 9, 1))
    }

    @Test
    fun `a week's record follows the first day of the week`() {
        // Sunday 13 and Monday 14 September: one week from Sunday, two from Monday.
        val days = arrayOf(day(11, 10_000), day(10, 10_000))
        assertThat(insights(*days, firstDayOfWeek = DayOfWeek.SUNDAY).bestWeek?.steps).isEqualTo(20_000)
        assertThat(insights(*days, firstDayOfWeek = DayOfWeek.MONDAY).bestWeek?.steps).isEqualTo(10_000)
    }

    @Test
    fun `averages leave today out and count empty days as zero`() {
        val days = (1..14).map { day(it, if (it <= 7) 8_000 else 6_000) } + day(0, 500)
        val insights = insights(*days.toTypedArray())
        assertThat(insights.averageLast7).isEqualTo(8_000)
        assertThat(insights.averagePrevious7).isEqualTo(6_000)
        assertThat(insights.averageLast30).isNull()
        assertThat(insights.countedDays).isEqualTo(15)
        assertThat(insights.averageAll).isEqualTo(((7 * 8_000 + 7 * 6_000 + 500) / 15.0).roundToInt())
        val gap = insights(day(8, 7_000), day(0, 7_000))
        assertThat(gap.averageLast7).isEqualTo(0)
    }

    @Test
    fun `the headline compares the last week with the one before`() {
        // A streak would win, so none of these days meets the goal.
        val days = (1..14).map { day(it, if (it <= 7) 6_600 else 6_000, goal = 20_000) }
        val headline = insights(*days.toTypedArray()).headline as InsightsHeadline.LastWeek
        assertThat(headline.average).isEqualTo(6_600)
        assertThat(headline.trend).isEqualTo(Trend.UP)
        assertThat(headline.change).isWithin(1e-9).of(0.1)

        val steady = (1..14).map { day(it, if (it <= 7) 6_100 else 6_000, goal = 20_000) }
        assertThat((insights(*steady.toTypedArray()).headline as InsightsHeadline.LastWeek).trend)
            .isEqualTo(Trend.STEADY)
    }

    @Test
    fun `today beating every day before is the headline, once there is a history`() {
        val days = (1..10).map { day(it, 7_000) } + day(0, 15_000)
        assertThat(insights(*days.toTypedArray()).headline).isEqualTo(InsightsHeadline.BestDayToday(15_000))
        // With two days behind it, a "best day ever" would be a small claim.
        assertThat(insights(day(1, 3_000), day(0, 4_000)).headline).isEqualTo(InsightsHeadline.Average(3_500, 2))
    }

    @Test
    fun `lifetime totals`() {
        val insights = insights(day(2, 1_000), day(0, 3_000))
        assertThat(insights.totalSteps).isEqualTo(4_000)
        assertThat(insights.totalDistanceMeters).isWithin(1e-9).of(2_800.0)
        assertThat(insights.countedDays).isEqualTo(3)
        assertThat(insights.firstDay).isEqualTo(today.minusDays(2))
    }
}
