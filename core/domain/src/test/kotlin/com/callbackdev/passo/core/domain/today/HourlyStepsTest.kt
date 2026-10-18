package com.callbackdev.passo.core.domain.today

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

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

    @Test
    fun `on the day the clocks go back both two o'clocks are in the 02 bar, and the day adds up`() {
        // Rome, 25 October 2026 has 25 hours; the bars are the clock's 24, so the repeated hour
        // shares its bar and no step is lost.
        val zone = ZoneId.of("Europe/Rome")
        val day = LocalDate.parse("2026-10-25")
        val first = ZonedDateTime.of(day, LocalTime.of(2, 30), zone).withEarlierOffsetAtOverlap()
        val second = first.withLaterOffsetAtOverlap()
        val minutes = listOf(first, second, ZonedDateTime.of(day, LocalTime.of(23, 59), zone)).map {
            DayMinute(minuteOfDay(it.toEpochSecond() / 60, day.toEpochDay(), zone), 100)
        }
        val hours = HourlySteps.of(minutes)
        assertThat(second.toEpochSecond() - first.toEpochSecond()).isEqualTo(3_600)
        assertThat(hours[2]).isEqualTo(200)
        assertThat(hours[23]).isEqualTo(100)
        assertThat(hours.total).isEqualTo(300)
    }

    @Test
    fun `on the day the clocks go forward the 02 bar stays empty`() {
        // Rome, 28 March 2027 has 23 hours: 01:59 is followed by 03:00.
        val zone = ZoneId.of("Europe/Rome")
        val day = LocalDate.parse("2027-03-28")
        val before = ZonedDateTime.of(day, LocalTime.of(1, 59), zone)
        val minutes = listOf(before, before.plusMinutes(1)).map {
            DayMinute(minuteOfDay(it.toEpochSecond() / 60, day.toEpochDay(), zone), 50)
        }
        val hours = HourlySteps.of(minutes)
        assertThat(hours[1]).isEqualTo(50)
        assertThat(hours[2]).isEqualTo(0)
        assertThat(hours[3]).isEqualTo(50)
    }
}
