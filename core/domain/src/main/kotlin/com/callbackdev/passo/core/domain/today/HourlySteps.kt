package com.callbackdev.passo.core.domain.today

/**
 * A day's steps per hour of the clock, index 0 being 00:00 to 01:00: the widget's mini chart
 * (PLANNING.md §7) and, from Phase 5, the day detail's bars. A minute outside the day is clamped
 * onto it, as [minuteOfDay] does, so the hours always add up to the day.
 */
class HourlySteps private constructor(private val hours: IntArray) {
    operator fun get(hour: Int): Int = hours[hour]

    val total: Int get() = hours.sum()

    /** The busiest hour's steps, the scale a chart of the day is drawn against; 0 on a day without steps. */
    val busiest: Int get() = hours.max()

    fun toList(): List<Int> = hours.toList()

    companion object {
        const val HOURS: Int = 24

        fun of(minutes: Iterable<DayMinute>): HourlySteps {
            val hours = IntArray(HOURS)
            for (minute in minutes) {
                if (minute.steps <= 0) continue
                hours[minute.minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1) / MINUTES_PER_HOUR] += minute.steps
            }
            return HourlySteps(hours)
        }
    }
}

private const val MINUTES_PER_HOUR = 60
