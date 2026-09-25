package com.callbackdev.passo.widget.glance

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

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
}
