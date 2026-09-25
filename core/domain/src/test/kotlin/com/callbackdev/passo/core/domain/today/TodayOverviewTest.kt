package com.callbackdev.passo.core.domain.today

import com.callbackdev.passo.core.model.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TodayOverviewTest {
    private val noon = 12 * 60.0

    private fun typicalOf(total: Int): TypicalDay? =
        // Two days walking `total` steps each, all before 06:00: by noon a usual day has them all.
        TypicalDayCalculator.typical(listOf(listOf(DayMinute(300, total)), listOf(DayMinute(300, total))))

    private fun overview(minutes: List<DayMinute>, goal: Int = 8_000, typical: TypicalDay? = null, live: Int? = null) =
        TodayOverview.of(minutes, Profile(), goal, noon, typical, live)

    @Test
    fun `a day with no steps says so and how far the goal is`() {
        val today = overview(emptyList())

        assertThat(today.headline).isEqualTo(Headline.NoStepsYet)
        assertThat(today.detail).isEqualTo(Headline.ToGo(8_000, 80))
        assertThat(today.progress).isEqualTo(0.0)
        assertThat(today.cadenceBand).isNull()
    }

    @Test
    fun `without a usual day the headline is what is left to walk`() {
        val today = overview(listOf(DayMinute(600, 2_550)))

        assertThat(today.headline).isEqualTo(Headline.ToGo(5_450, 55))
        assertThat(today.detail).isNull()
        assertThat(today.pace).isNull()
        assertThat(today.usualNow).isNull()
    }

    @Test
    fun `with a usual day the headline compares, and the detail says what is left`() {
        val today = overview(listOf(DayMinute(600, 4_000)), typical = typicalOf(3_000))

        assertThat(today.usualNow).isEqualTo(3_000)
        assertThat(today.headline).isEqualTo(Headline.VersusUsual(Pace.Ahead(1_000)))
        assertThat(today.detail).isEqualTo(Headline.ToGo(4_000, 40))
        assertThat(today.usualProgress).isWithin(1e-9).of(3_000 / 8_000.0)
    }

    @Test
    fun `a small difference from usual is on pace`() {
        assertThat(TodayOverview.paceOf(3_100, 3_000)).isEqualTo(Pace.OnPace)
        assertThat(TodayOverview.paceOf(100, 0)).isEqualTo(Pace.OnPace)
        assertThat(TodayOverview.paceOf(10_000, 11_000)).isEqualTo(Pace.Behind(1_000))
    }

    @Test
    fun `a reached goal says when, and by how much it is passed`() {
        val minutes = listOf(DayMinute(480, 5_000), DayMinute(610, 3_500))
        val today = overview(minutes, typical = typicalOf(3_000))

        assertThat(today.headline).isEqualTo(Headline.GoalReached(610, 500))
        assertThat(today.detail).isEqualTo(Headline.VersusUsual(Pace.Ahead(5_500)))
        assertThat(today.remaining).isEqualTo(0)
        assertThat(today.progress).isWithin(1e-9).of(8_500 / 8_000.0)
    }

    @Test
    fun `the service's live count wins when it is ahead of the stored minutes`() {
        val today = overview(listOf(DayMinute(600, 1_000)), live = 1_040)

        assertThat(today.steps).isEqualTo(1_040)
        assertThat(overview(listOf(DayMinute(600, 1_000)), live = 900).steps).isEqualTo(1_000)
    }

    @Test
    fun `a goal reached only by live steps is reached now`() {
        val today = overview(listOf(DayMinute(600, 7_990)), live = 8_010)

        assertThat(today.goalReachedAt).isEqualTo(720)
    }

    @Test
    fun `the brisk share counts down to zero`() {
        assertThat(overview(emptyList()).briskShareLeft).isEqualTo(22)
        assertThat(overview(List(10) { DayMinute(600 + it, 110) }).briskShareLeft).isEqualTo(12)
        assertThat(overview(List(30) { DayMinute(600 + it, 110) }).briskShareLeft).isEqualTo(0)
    }

    @Test
    fun `the average cadence is put in words`() {
        assertThat(TodayOverview.cadenceBand(99)).isEqualTo(CadenceBand.RELAXED)
        assertThat(TodayOverview.cadenceBand(100)).isEqualTo(CadenceBand.BRISK)
        assertThat(TodayOverview.cadenceBand(130)).isEqualTo(CadenceBand.VIGOROUS)
    }

    @Test
    fun `minutes to walk round up`() {
        assertThat(TodayOverview.minutesToWalk(0)).isEqualTo(0)
        assertThat(TodayOverview.minutesToWalk(1)).isEqualTo(1)
        assertThat(TodayOverview.minutesToWalk(2_300)).isEqualTo(23)
    }
}
