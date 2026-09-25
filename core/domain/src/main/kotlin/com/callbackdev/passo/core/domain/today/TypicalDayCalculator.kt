package com.callbackdev.passo.core.domain.today

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.TYPICAL_DAY_MIN_STEPS
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.TYPICAL_DAY_MIN_VALID_DAYS
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.TYPICAL_DAY_SLOT_MINUTES
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.TYPICAL_DAY_WEEKS

/**
 * "A usual Thursday" (PLANNING.md §6.2): the mean running total of the same weekday over the
 * last weeks, slot by slot. Computed when Today opens and every 15 minutes while it stays
 * visible; never in the background.
 *
 * @property curve the mean running total, one sample per 15-minute slot boundary (97 of them).
 * @property daysUsed how many past days made it, after leaving out the near-empty ones.
 */
class TypicalDay(val curve: DayCurve, val daysUsed: Int) {
    /** The usual running total at [minuteOfDay]. */
    fun at(minuteOfDay: Double): Double = curve.at(minuteOfDay)
}

object TypicalDayCalculator {
    /** The past days that make today's typical day: the same weekday, one to four weeks ago. */
    fun candidateDays(today: Long): List<Long> = (1..TYPICAL_DAY_WEEKS).map { today - it * DAYS_PER_WEEK }

    /**
     * The typical day made of [days], each the minutes of one past day; null when fewer than
     * two of them reach [TYPICAL_DAY_MIN_STEPS], so the line is hidden until there is a usual
     * to compare with, and appears by itself the day there is.
     */
    fun typical(days: List<List<DayMinute>>): TypicalDay? {
        val curves = days
            .map { DayCurve.of(it, TYPICAL_DAY_SLOT_MINUTES) }
            .filter { it.total >= TYPICAL_DAY_MIN_STEPS }
        if (curves.size < TYPICAL_DAY_MIN_VALID_DAYS) return null
        val slots = curves.first().size
        val mean = IntArray(slots) { i -> (curves.sumOf { it[i].toDouble() } / curves.size).toInt() }
        return TypicalDay(DayCurve.fromTotals(TYPICAL_DAY_SLOT_MINUTES, mean), curves.size)
    }

    private const val DAYS_PER_WEEK = 7
}
