package com.callbackdev.passo.core.domain.goals

import com.callbackdev.passo.core.domain.history.Period
import com.callbackdev.passo.core.domain.history.PeriodScale
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * When the evening reminder and the weekly summary are next due (PLANNING.md §8). Always the
 * reader's local time, worked out again after every clock or time-zone change: an alarm is an
 * instant, and 20:00 in Rome is not 20:00 in Lisbon.
 *
 * A time that does not exist on a day (the hour the clocks skip in spring) moves forward by
 * the length of the gap, as `java.time` resolves it; a time that happens twice (the hour
 * repeated in autumn) is the first of the two.
 */
object GoalSchedule {
    /**
     * The summary of a week comes the morning after it ends, when it is whole: on the first day
     * of the next one, at nine, once the reader is up and before the day's own steps matter.
     */
    val WEEKLY_SUMMARY_TIME: LocalTime = LocalTime.of(9, 0)

    /** How late an evening reminder may come: two hours, still the evening it was for. */
    val REMINDER_LATE_LIMIT: Duration = Duration.ofHours(2)

    /**
     * The next evening reminder: [time] on the first day from [earliestDay] whose [time] is
     * still after [now]. [earliestDay] is tomorrow once today's goal is met, so a met day does
     * not wake the phone for a reminder it would then decline.
     */
    fun nextReminder(now: Instant, zone: ZoneId, time: LocalTime, earliestDay: LocalDate? = null): Instant {
        val today = now.atZone(zone).toLocalDate()
        var day = maxOf(today, earliestDay ?: today)
        while (true) {
            val at = at(day, time, zone)
            if (at.isAfter(now)) return at
            day = day.plusDays(1)
        }
    }

    /** The next weekly summary: [WEEKLY_SUMMARY_TIME] on the next [firstDayOfWeek] still ahead of [now]. */
    fun nextWeeklySummary(now: Instant, zone: ZoneId, firstDayOfWeek: DayOfWeek): Instant {
        val today = now.atZone(zone).toLocalDate()
        val thisWeek = today.with(TemporalAdjusters.nextOrSame(firstDayOfWeek))
        val at = at(thisWeek, WEEKLY_SUMMARY_TIME, zone)
        return if (at.isAfter(now)) at else at(thisWeek.plusWeeks(1), WEEKLY_SUMMARY_TIME, zone)
    }

    /**
     * Whether an evening reminder due at [dueAt] still makes sense at [now]: the same evening,
     * and at most [REMINDER_LATE_LIMIT] late. Doze or a phone that was off can hold an alarm
     * back; a reminder to walk that arrives near midnight, or on the next day, is noise.
     */
    fun reminderStillTimely(dueAt: Instant, now: Instant, zone: ZoneId): Boolean = !now.isBefore(dueAt) &&
        Duration.between(dueAt, now) <= REMINDER_LATE_LIMIT &&
        dueAt.atZone(zone).toLocalDate() == now.atZone(zone).toLocalDate()

    /**
     * Whether a weekly summary due at [dueAt] still makes sense at [now]: the week it opens is
     * still the current one, so the week it sums is still "last week".
     */
    fun summaryStillTimely(dueAt: Instant, now: Instant, zone: ZoneId, firstDayOfWeek: DayOfWeek): Boolean {
        if (now.isBefore(dueAt)) return false
        val due = Period.containing(PeriodScale.WEEK, dueAt.atZone(zone).toLocalDate(), firstDayOfWeek)
        return now.atZone(zone).toLocalDate() in due
    }

    private fun at(day: LocalDate, time: LocalTime, zone: ZoneId): Instant =
        ZonedDateTime.of(day, time, zone).toInstant()
}
