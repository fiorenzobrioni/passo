package com.callbackdev.passo.core.domain.today

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DayCurveTest {
    @Test
    fun `an empty day is flat at zero`() {
        val curve = DayCurve.of(emptyList())

        assertThat(curve.size).isEqualTo(289)
        assertThat(curve.total).isEqualTo(0)
        assertThat(curve.at(720.0)).isEqualTo(0.0)
    }

    @Test
    fun `steps count at the end of their minute`() {
        val curve = DayCurve.of(listOf(DayMinute(0, 10), DayMinute(4, 20), DayMinute(5, 30)))

        assertThat(curve[0]).isEqualTo(0)
        assertThat(curve[1]).isEqualTo(30) // 00:05, after minutes 0-4
        assertThat(curve[2]).isEqualTo(60)
        assertThat(curve.total).isEqualTo(60)
    }

    @Test
    fun `between samples the total is interpolated`() {
        val curve = DayCurve.of(listOf(DayMinute(9, 100)), stepMinutes = 10)

        assertThat(curve.at(5.0)).isWithin(1e-9).of(50.0)
        assertThat(curve.at(10_000.0)).isEqualTo(100.0)
        assertThat(curve.at(-3.0)).isEqualTo(0.0)
    }

    @Test
    fun `the goal is reached in the minute the running total gets there`() {
        val minutes = listOf(DayMinute(600, 50), DayMinute(480, 60), DayMinute(700, 10))

        assertThat(DayCurve.minuteReaching(minutes, 100)).isEqualTo(600)
        assertThat(DayCurve.minuteReaching(minutes, 121)).isNull()
        assertThat(DayCurve.minuteReaching(minutes, 0)).isNull()
    }

    @Test
    fun `a minute is placed on its local day, clamped when a zone change moved it`() {
        val rome = ZoneId.of("Europe/Rome")
        val day = LocalDate.of(2026, 9, 24).toEpochDay()
        fun epochMinute(text: String, zone: ZoneId) = LocalDateTime.parse(text).atZone(zone).toEpochSecond() / 60

        assertThat(minuteOfDay(epochMinute("2026-09-24T10:30", rome), day, rome)).isEqualTo(630)
        // Written in New York late on the 24th: in Rome it is already the 25th.
        val lateInNewYork = epochMinute("2026-09-24T22:00", ZoneId.of("America/New_York"))
        assertThat(minuteOfDay(lateInNewYork, day, rome)).isEqualTo(1439)
        // Written in Tokyo early on the 24th: in Rome it is still the 23rd.
        val earlyInTokyo = epochMinute("2026-09-24T02:00", ZoneId.of("Asia/Tokyo"))
        assertThat(minuteOfDay(earlyInTokyo, day, rome)).isEqualTo(0)
    }
}
