package com.callbackdev.passo.core.domain.today

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HourlyStepsTest {
    @Test
    fun `an empty day has no busiest hour`() {
        val hours = HourlySteps.of(emptyList())
        assertThat(hours.toList()).hasSize(24)
        assertThat(hours.busiest).isEqualTo(0)
        assertThat(hours.total).isEqualTo(0)
    }

    @Test
    fun `minutes land in their hour`() {
        val hours = HourlySteps.of(
            listOf(
                DayMinute(0, 10),
                DayMinute(59, 5),
                DayMinute(60, 7),
                DayMinute(12 * 60 + 30, 120),
                DayMinute(1_439, 3),
            ),
        )
        assertThat(hours[0]).isEqualTo(15)
        assertThat(hours[1]).isEqualTo(7)
        assertThat(hours[12]).isEqualTo(120)
        assertThat(hours[23]).isEqualTo(3)
        assertThat(hours.busiest).isEqualTo(120)
        assertThat(hours.total).isEqualTo(145)
    }

    @Test
    fun `minutes outside the day are clamped onto it`() {
        val hours = HourlySteps.of(listOf(DayMinute(-5, 4), DayMinute(1_500, 6), DayMinute(30, -2)))
        assertThat(hours[0]).isEqualTo(4)
        assertThat(hours[23]).isEqualTo(6)
        assertThat(hours.total).isEqualTo(10)
    }
}
