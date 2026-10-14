package com.callbackdev.passo.core.designsystem

import com.callbackdev.passo.core.designsystem.components.DayTrend
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DayTrendTest {
    private val trend = DayTrend(
        today = listOf(0, 100, 300),
        stepMinutes = 5,
        nowMinute = 10f,
        usual = List(97) { it * 10 },
        usualStepMinutes = 15,
        goal = 8_000,
    )

    @Test
    fun `today reads up to now, interpolated`() {
        assertThat(trend.todayAt(0f)).isEqualTo(0)
        assertThat(trend.todayAt(7.5f)).isEqualTo(200)
        assertThat(trend.todayAt(10f)).isEqualTo(300)
    }

    @Test
    fun `past now only the usual day reads`() {
        assertThat(trend.todayAt(600f)).isNull()
        assertThat(trend.usualAt(600f)).isEqualTo(400)
    }

    @Test
    fun `a day that starts after five starts the chart at five`() {
        // First steps in the slot ending 07:05; a usual day's in the one ending 06:45.
        val day = trend.copy(
            today = List(85) { 0 } + List(10) { 500 },
            usual = List(27) { 0 } + List(70) { 400 },
            nowMinute = 470f,
        )
        assertThat(day.startMinute).isEqualTo(5 * 60)
    }

    @Test
    fun `an earlier first step moves the start back to its hour`() {
        // Today's first steps in the slot from 03:20 to 03:25.
        val day = trend.copy(today = List(41) { 0 } + List(10) { 120 }, usual = null, nowMinute = 250f)
        assertThat(day.startMinute).isEqualTo(3 * 60)
    }

    @Test
    fun `a day with no steps at all starts at five`() {
        val day = trend.copy(today = List(10) { 0 }, usual = null, nowMinute = 50f)
        assertThat(day.startMinute).isEqualTo(5 * 60)
    }
}
