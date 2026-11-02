package com.callbackdev.passo.widget.glance

import androidx.compose.ui.unit.dp
import com.callbackdev.passo.widget.WeekDay
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

class HourBarsTest {
    private val hours = List(24) { hour ->
        when (hour) {
            8 -> 2_000
            12 -> 4_000
            13 -> 10
            else -> 0
        }
    }

    @Test
    fun `the busiest hour fills the height and the others follow it`() {
        val bars = hourBars(hours, nowHour = 15, height = 60.dp)
        assertThat(bars[12].height).isEqualTo(60.dp)
        assertThat(bars[8].height).isEqualTo(30.dp)
        assertThat(bars[8].kind).isEqualTo(BarKind.PAST)
    }

    @Test
    fun `a quiet hour still reads as walked`() {
        assertThat(hourBars(hours, nowHour = 15, height = 60.dp)[13].height).isEqualTo(BarMin)
    }

    @Test
    fun `hours without steps and hours to come are the baseline`() {
        val bars = hourBars(hours, nowHour = 15, height = 60.dp)
        assertThat(bars[3]).isEqualTo(Bar(BarBaseline, BarKind.EMPTY))
        assertThat(bars[20]).isEqualTo(Bar(BarBaseline, BarKind.EMPTY))
    }

    @Test
    fun `the hour under way is marked even before its first step`() {
        assertThat(hourBars(hours, nowHour = 15, height = 60.dp)[15]).isEqualTo(Bar(BarBaseline, BarKind.NOW))
        assertThat(hourBars(hours, nowHour = 12, height = 60.dp)[12]).isEqualTo(Bar(60.dp, BarKind.NOW))
    }

    @Test
    fun `an empty day draws only baselines`() {
        val bars = hourBars(List(24) { 0 }, nowHour = 7, height = 60.dp)
        assertThat(bars.map { it.height }.toSet()).containsExactly(BarBaseline)
    }

    @Test
    fun `the gap between bars stays between one and three dp`() {
        assertThat(barGap(120.dp)).isEqualTo(1.dp)
        assertThat(barGap(312.dp).value).isWithin(0.01f).of(2.6f)
        assertThat(barGap(800.dp)).isEqualTo(3.dp)
    }

    private fun week(vararg steps: Int?, goal: Int = 10_000): List<WeekDay> =
        steps.mapIndexed { index, count -> WeekDay(LocalDate.of(2026, 9, 18).plusDays(index.toLong()), count, goal) }

    @Test
    fun `the week stands on the world's scale, the goal the top until a day passes it`() {
        val quiet = weekBars(week(2_000, 3_000, 4_000, 5_000, 2_500, 1_000, 500), height = 60.dp)
        // The goal is the top: a quiet week looks quiet, and the line is at the top.
        assertThat(quiet.goalHeight).isEqualTo(60.dp)
        assertThat(quiet.bars[3].height).isEqualTo(30.dp)

        val busy = weekBars(week(20_000, 3_000, 4_000, 5_000, 2_500, 1_000, 500), height = 60.dp)
        assertThat(busy.bars[0].height).isEqualTo(60.dp)
        assertThat(busy.goalHeight).isEqualTo(30.dp)
    }

    @Test
    fun `today is last and highlighted, a day before counting is the baseline`() {
        val bars = weekBars(week(null, null, 4_000, 0, 6_000, 8_000, 0), height = 60.dp).bars
        assertThat(bars.map { it.kind }).containsExactly(
            BarKind.EMPTY,
            BarKind.EMPTY,
            BarKind.PAST,
            BarKind.EMPTY,
            BarKind.PAST,
            BarKind.PAST,
            BarKind.NOW,
        ).inOrder()
        assertThat(bars[0].height).isEqualTo(BarBaseline)
        assertThat(bars[6].height).isEqualTo(BarBaseline)
    }
}
