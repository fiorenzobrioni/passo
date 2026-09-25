package com.callbackdev.passo.core.domain.history

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class PeriodTest {
    private val thursday = LocalDate.of(2026, 9, 24)

    @Test
    fun `a week starts on the reader's first day`() {
        val monday = Period.containing(PeriodScale.WEEK, thursday, DayOfWeek.MONDAY)
        assertThat(monday.start).isEqualTo(LocalDate.of(2026, 9, 21))
        assertThat(monday.end).isEqualTo(LocalDate.of(2026, 9, 27))
        val sunday = Period.containing(PeriodScale.WEEK, thursday, DayOfWeek.SUNDAY)
        assertThat(sunday.start).isEqualTo(LocalDate.of(2026, 9, 20))
        assertThat(sunday.days).isEqualTo(7)
    }

    @Test
    fun `the first day of the week is its own week's start`() {
        val week = Period.containing(PeriodScale.WEEK, LocalDate.of(2026, 9, 21), DayOfWeek.MONDAY)
        assertThat(week.start).isEqualTo(LocalDate.of(2026, 9, 21))
    }

    @Test
    fun `months and years hold their own days`() {
        val february = Period.containing(PeriodScale.MONTH, LocalDate.of(2028, 2, 10), DayOfWeek.MONDAY)
        assertThat(february.days).isEqualTo(29)
        val year = Period.containing(PeriodScale.YEAR, thursday, DayOfWeek.MONDAY)
        assertThat(year.start).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(year.end).isEqualTo(LocalDate.of(2026, 12, 31))
    }

    @Test
    fun `shifting keeps the scale and lands on whole periods`() {
        val january = Period.containing(PeriodScale.MONTH, LocalDate.of(2026, 1, 31), DayOfWeek.MONDAY)
        val february = january.shifted(1)
        assertThat(february.start).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(february.end).isEqualTo(LocalDate.of(2026, 2, 28))
        assertThat(january.previous().start).isEqualTo(LocalDate.of(2025, 12, 1))
    }

    @Test
    fun `the page count runs from the first recorded period to today's`() {
        val first = LocalDate.of(2026, 8, 30) // a Sunday
        assertThat(Period.count(PeriodScale.DAY, first, thursday, DayOfWeek.MONDAY)).isEqualTo(26)
        // Monday weeks: 24-30 Aug holds the first day, 21-27 Sep holds today.
        assertThat(Period.count(PeriodScale.WEEK, first, thursday, DayOfWeek.MONDAY)).isEqualTo(5)
        // Sunday weeks: 30 Aug is a week's first day.
        assertThat(Period.count(PeriodScale.WEEK, first, thursday, DayOfWeek.SUNDAY)).isEqualTo(4)
        assertThat(Period.count(PeriodScale.MONTH, first, thursday, DayOfWeek.MONDAY)).isEqualTo(2)
        assertThat(Period.count(PeriodScale.YEAR, first, thursday, DayOfWeek.MONDAY)).isEqualTo(1)
    }

    @Test
    fun `a first day after today still gives one page`() {
        assertThat(Period.count(PeriodScale.DAY, thursday.plusDays(3), thursday, DayOfWeek.MONDAY)).isEqualTo(1)
    }

    @Test
    fun `pages run from the first recorded period to today's, and find a date`() {
        val pages = PeriodPages(PeriodScale.WEEK, LocalDate.of(2026, 8, 30), thursday, DayOfWeek.MONDAY)
        assertThat(pages.count).isEqualTo(5)
        assertThat(pages.periodAt(pages.lastPage).start).isEqualTo(LocalDate.of(2026, 9, 21))
        assertThat(pages.periodAt(0).start).isEqualTo(LocalDate.of(2026, 8, 24))
        assertThat(pages.pageOf(LocalDate.of(2026, 9, 1))).isEqualTo(1)
        // Before the first page or after today: held to the pages there are.
        assertThat(pages.pageOf(LocalDate.of(2025, 1, 1))).isEqualTo(0)
        assertThat(pages.pageOf(thursday.plusDays(30))).isEqualTo(pages.lastPage)
    }

    @Test
    fun `with nothing recorded there is one page, today's`() {
        val pages = PeriodPages(PeriodScale.MONTH, null, thursday, DayOfWeek.MONDAY)
        assertThat(pages.count).isEqualTo(1)
        assertThat(pages.periodAt(0).start).isEqualTo(LocalDate.of(2026, 9, 1))
    }
}
