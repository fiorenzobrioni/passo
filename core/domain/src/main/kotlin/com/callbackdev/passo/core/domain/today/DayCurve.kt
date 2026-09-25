package com.callbackdev.passo.core.domain.today

/**
 * A day's steps as a running total (the Today sparkline, PLANNING.md §6.2), sampled every
 * [stepMinutes] from midnight: index i is the total at minute `i * stepMinutes`, and the last
 * index is the day's end. Steps of a minute are counted at the end of that minute.
 */
class DayCurve private constructor(val stepMinutes: Int, private val totals: IntArray) {
    /** How many samples: one per [stepMinutes] from 00:00, plus the one at 24:00. */
    val size: Int get() = totals.size

    operator fun get(index: Int): Int = totals[index]

    /** The day's running total at [minuteOfDay] (0 to 1440), interpolated between samples. */
    fun at(minuteOfDay: Double): Double {
        val position = (minuteOfDay / stepMinutes).coerceIn(0.0, (size - 1).toDouble())
        val lower = position.toInt()
        val upper = minOf(lower + 1, size - 1)
        val fraction = position - lower
        return totals[lower] + (totals[upper] - totals[lower]) * fraction
    }

    val total: Int get() = totals.last()

    fun toList(): List<Int> = totals.toList()

    companion object {
        fun of(minutes: Iterable<DayMinute>, stepMinutes: Int = 5): DayCurve {
            require(MINUTES_PER_DAY % stepMinutes == 0) { "A day must split into whole samples" }
            val perMinute = IntArray(MINUTES_PER_DAY)
            for (minute in minutes) {
                if (minute.steps > 0) perMinute[minute.minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)] += minute.steps
            }
            val totals = IntArray(MINUTES_PER_DAY / stepMinutes + 1)
            var running = 0
            for (m in 0 until MINUTES_PER_DAY) {
                running += perMinute[m]
                if ((m + 1) % stepMinutes == 0) totals[(m + 1) / stepMinutes] = running
            }
            return DayCurve(stepMinutes, totals)
        }

        /** A curve from totals already sampled every [stepMinutes] (the typical day's mean). */
        internal fun fromTotals(stepMinutes: Int, totals: IntArray): DayCurve {
            require(totals.size == MINUTES_PER_DAY / stepMinutes + 1) { "One total per sample of the day" }
            return DayCurve(stepMinutes, totals.copyOf())
        }

        /** The first minute of the day by whose end [minutes] add up to [target]; null if they never do. */
        fun minuteReaching(minutes: Iterable<DayMinute>, target: Int): Int? {
            if (target <= 0) return null
            var running = 0
            for (minute in minutes.sortedBy { it.minuteOfDay }) {
                running += minute.steps.coerceAtLeast(0)
                if (running >= target) return minute.minuteOfDay
            }
            return null
        }
    }
}
