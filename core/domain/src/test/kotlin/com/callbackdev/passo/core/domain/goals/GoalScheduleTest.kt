package com.callbackdev.passo.core.domain.goals

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class GoalScheduleTest {
    private val rome = ZoneId.of("Europe/Rome")
    private val eight = LocalTime.of(20, 0)

    private fun at(text: String, zone: ZoneId = rome): Instant = LocalDateTime.parse(text).atZone(zone).toInstant()

    private fun local(instant: Instant, zone: ZoneId = rome): LocalDateTime =
        ZonedDateTime.ofInstant(instant, zone).toLocalDateTime()

    @Test
    fun `before the time, the reminder is today`() {
        val next = GoalSchedule.nextReminder(at("2026-09-24T17:10"), rome, eight)
        assertThat(local(next)).isEqualTo(LocalDateTime.parse("2026-09-24T20:00"))
    }

    @Test
    fun `at the time or after, it is tomorrow`() {
        assertThat(local(GoalSchedule.nextReminder(at("2026-09-24T20:00"), rome, eight)))
            .isEqualTo(LocalDateTime.parse("2026-09-25T20:00"))
        assertThat(local(GoalSchedule.nextReminder(at("2026-09-24T22:45"), rome, eight)))
            .isEqualTo(LocalDateTime.parse("2026-09-25T20:00"))
    }

    @Test
    fun `a day with its goal met skips to tomorrow`() {
        val next = GoalSchedule.nextReminder(at("2026-09-24T11:00"), rome, eight, LocalDate.parse("2026-09-25"))
        assertThat(local(next)).isEqualTo(LocalDateTime.parse("2026-09-25T20:00"))
    }

    @Test
    fun `after a time-zone change the reminder keeps the local hour`() {
        val lisbon = ZoneId.of("Europe/Lisbon")
        val now = at("2026-09-24T17:10")
        val next = GoalSchedule.nextReminder(now, lisbon, eight)
        assertThat(local(next, lisbon)).isEqualTo(LocalDateTime.parse("2026-09-24T20:00"))
        // An hour later on the absolute clock than the same reminder in Rome.
        assertThat(next).isEqualTo(GoalSchedule.nextReminder(now, rome, eight).plusSeconds(3_600))
    }

    @Test
    fun `a time the clocks skip moves forward by the gap`() {
        // 29 March 2026, Rome: 02:00 becomes 03:00.
        val next = GoalSchedule.nextReminder(at("2026-03-28T23:00"), rome, LocalTime.of(2, 30))
        assertThat(local(next)).isEqualTo(LocalDateTime.parse("2026-03-29T03:30"))
    }

    @Test
    fun `the weekly summary is the first day of the week at nine`() {
        // Thursday 24 September 2026.
        val next = GoalSchedule.nextWeeklySummary(at("2026-09-24T12:00"), rome, DayOfWeek.MONDAY)
        assertThat(local(next)).isEqualTo(LocalDateTime.parse("2026-09-28T09:00"))
    }

    @Test
    fun `on the first day of the week, before nine it is today and after nine next week`() {
        assertThat(local(GoalSchedule.nextWeeklySummary(at("2026-09-28T07:30"), rome, DayOfWeek.MONDAY)))
            .isEqualTo(LocalDateTime.parse("2026-09-28T09:00"))
        assertThat(local(GoalSchedule.nextWeeklySummary(at("2026-09-28T09:00"), rome, DayOfWeek.MONDAY)))
            .isEqualTo(LocalDateTime.parse("2026-10-05T09:00"))
    }

    @Test
    fun `the week can start on Sunday or Saturday`() {
        assertThat(local(GoalSchedule.nextWeeklySummary(at("2026-09-24T12:00"), rome, DayOfWeek.SUNDAY)))
            .isEqualTo(LocalDateTime.parse("2026-09-27T09:00"))
        assertThat(local(GoalSchedule.nextWeeklySummary(at("2026-09-24T12:00"), rome, DayOfWeek.SATURDAY)))
            .isEqualTo(LocalDateTime.parse("2026-09-26T09:00"))
    }

    @Test
    fun `a reminder held back by Doze still comes within two hours, the same evening`() {
        val due = at("2026-09-24T20:00")
        assertThat(GoalSchedule.reminderStillTimely(due, at("2026-09-24T20:07"), rome)).isTrue()
        assertThat(GoalSchedule.reminderStillTimely(due, at("2026-09-24T22:00"), rome)).isTrue()
        assertThat(GoalSchedule.reminderStillTimely(due, at("2026-09-24T22:01"), rome)).isFalse()
    }

    @Test
    fun `a late reminder never crosses midnight`() {
        val due = at("2026-09-24T23:30")
        assertThat(GoalSchedule.reminderStillTimely(due, at("2026-09-24T23:59"), rome)).isTrue()
        assertThat(GoalSchedule.reminderStillTimely(due, at("2026-09-25T00:10"), rome)).isFalse()
    }

    @Test
    fun `a weekly summary is still due as long as the week it opens is the current one`() {
        val due = at("2026-09-28T09:00") // Monday
        assertThat(GoalSchedule.summaryStillTimely(due, at("2026-09-28T09:00"), rome, DayOfWeek.MONDAY)).isTrue()
        assertThat(GoalSchedule.summaryStillTimely(due, at("2026-10-04T23:00"), rome, DayOfWeek.MONDAY)).isTrue()
        assertThat(GoalSchedule.summaryStillTimely(due, at("2026-10-05T09:30"), rome, DayOfWeek.MONDAY)).isFalse()
    }
}
