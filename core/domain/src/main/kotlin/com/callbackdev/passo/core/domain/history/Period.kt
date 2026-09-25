package com.callbackdev.passo.core.domain.history

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** How much of the calendar one page of History shows. */
enum class PeriodScale {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}

/**
 * A calendar period: a day, a week that starts on the reader's first day of the week, a
 * month or a year, from [start] to [end] inclusive.
 */
data class Period(val scale: PeriodScale, val start: LocalDate, val end: LocalDate) {
    val days: Int get() = (ChronoUnit.DAYS.between(start, end) + 1).toInt()

    operator fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    /** The period [count] periods later (earlier when negative). */
    fun shifted(count: Long): Period = when (scale) {
        PeriodScale.DAY -> start.plusDays(count).let { Period(scale, it, it) }
        PeriodScale.WEEK -> start.plusWeeks(count).let { Period(scale, it, it.plusDays(6)) }
        PeriodScale.MONTH -> start.plusMonths(count).let { Period(scale, it, it.plusMonths(1).minusDays(1)) }
        PeriodScale.YEAR -> start.plusYears(count).let { Period(scale, it, it.plusYears(1).minusDays(1)) }
    }

    fun previous(): Period = shifted(-1)

    companion object {
        /** The period of [scale] that [date] falls in. */
        fun containing(scale: PeriodScale, date: LocalDate, firstDayOfWeek: DayOfWeek): Period = when (scale) {
            PeriodScale.DAY -> Period(scale, date, date)

            PeriodScale.WEEK -> date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).let {
                Period(scale, it, it.plusDays(6))
            }

            PeriodScale.MONTH -> date.withDayOfMonth(1).let { Period(scale, it, it.plusMonths(1).minusDays(1)) }

            PeriodScale.YEAR -> date.withDayOfYear(1).let { Period(scale, it, it.plusYears(1).minusDays(1)) }
        }

        /**
         * How many periods of [scale] there are from the one holding [first] to the one holding
         * [last], both included: History's page count, so it never offers a page from before
         * counting began or after today.
         */
        fun count(scale: PeriodScale, first: LocalDate, last: LocalDate, firstDayOfWeek: DayOfWeek): Int {
            val from = containing(scale, minOf(first, last), firstDayOfWeek).start
            val to = containing(scale, last, firstDayOfWeek).start
            val between = when (scale) {
                PeriodScale.DAY -> ChronoUnit.DAYS.between(from, to)
                PeriodScale.WEEK -> ChronoUnit.WEEKS.between(from, to)
                PeriodScale.MONTH -> ChronoUnit.MONTHS.between(from, to)
                PeriodScale.YEAR -> ChronoUnit.YEARS.between(from, to)
            }
            return between.toInt() + 1
        }
    }
}

/**
 * History's pages at one [scale]: one period a page, from the one holding the [first] recorded
 * day to the one holding [today], the last page being today's.
 */
class PeriodPages(
    val scale: PeriodScale,
    first: LocalDate?,
    private val today: LocalDate,
    private val firstDayOfWeek: DayOfWeek,
) {
    val count: Int = Period.count(scale, first?.takeIf { !it.isAfter(today) } ?: today, today, firstDayOfWeek)

    private val latest = Period.containing(scale, today, firstDayOfWeek)

    val lastPage: Int get() = count - 1

    fun periodAt(page: Int): Period = latest.shifted((page - lastPage).toLong())

    /** The page holding [date], held to the pages there are. */
    fun pageOf(date: LocalDate): Int =
        (count - Period.count(scale, minOf(date, today), today, firstDayOfWeek)).coerceIn(0, lastPage)
}
