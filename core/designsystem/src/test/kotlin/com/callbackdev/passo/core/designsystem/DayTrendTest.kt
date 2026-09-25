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
}
