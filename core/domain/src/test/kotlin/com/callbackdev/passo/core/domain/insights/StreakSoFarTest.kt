package com.callbackdev.passo.core.domain.insights

import com.callbackdev.passo.core.model.DailySummary
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class StreakSoFarTest {
    private val today = LocalDate.of(2026, 9, 24)

    private fun day(daysAgo: Int, steps: Int, goal: Int = 8_000) = DailySummary(
        localEpochDay = today.minusDays(daysAgo.toLong()).toEpochDay(),
        steps = steps,
        distanceMeters = 0.0,
        activeKcal = 0.0,
        activeMinutes = 0,
        briskMinutes = 0,
        goalSteps = goal,
        finalized = daysAgo > 0,
    )

    @Test
    fun `nothing recorded is no streak`() {
        assertThat(StreakSoFar.endingYesterday(emptyList(), today)).isEqualTo(0)
    }

    @Test
    fun `the days in a row up to yesterday`() {
        val days = listOf(day(4, 9_000), day(3, 2_000), day(2, 8_000), day(1, 12_000))
        assertThat(StreakSoFar.endingYesterday(days, today)).isEqualTo(2)
    }

    @Test
    fun `a yesterday below its goal ends it`() {
        val days = listOf(day(3, 9_000), day(2, 9_000), day(1, 7_999))
        assertThat(StreakSoFar.endingYesterday(days, today)).isEqualTo(0)
    }

    @Test
    fun `a day with nothing recorded ends it`() {
        val days = listOf(day(3, 9_000), day(1, 9_000))
        assertThat(StreakSoFar.endingYesterday(days, today)).isEqualTo(1)
    }

    @Test
    fun `each day is measured against its own goal`() {
        val days = listOf(day(2, 6_000, goal = 5_000), day(1, 9_000, goal = 10_000))
        assertThat(StreakSoFar.endingYesterday(days, today)).isEqualTo(0)
        assertThat(StreakSoFar.endingYesterday(listOf(day(2, 6_000, goal = 5_000)), today.minusDays(1))).isEqualTo(1)
    }

    @Test
    fun `today joins once met, and does not break it before`() {
        assertThat(StreakSoFar.withToday(5, todayMet = false)).isEqualTo(5)
        assertThat(StreakSoFar.withToday(5, todayMet = true)).isEqualTo(6)
        assertThat(StreakSoFar.withToday(0, todayMet = true)).isEqualTo(1)
    }

    @Test
    fun `it is the streak Insights counts`() {
        val days = listOf(day(5, 1_000), day(4, 9_000), day(3, 9_000), day(2, 8_500), day(1, 8_000), day(0, 8_100))
        val insights = Insights.of(days.associateBy { it.localEpochDay }, today, DayOfWeek.MONDAY)
        val shown = StreakSoFar.withToday(StreakSoFar.endingYesterday(days, today), days.last().goalReached)
        assertThat(shown).isEqualTo(insights.currentStreak.days)
    }
}
