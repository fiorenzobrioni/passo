package com.callbackdev.passo.core.domain.today

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Steps in one minute of a local day, the minute counted from local midnight (0 to 1439). */
data class DayMinute(val minuteOfDay: Int, val steps: Int)

const val MINUTES_PER_DAY: Int = 24 * 60

/**
 * The minute of [localEpochDay] that UTC [epochMinute] falls on, in [zone].
 *
 * A minute is stored with the local day it was first written in, and a time-zone change since
 * can put its UTC minute on another date in today's zone. Such a minute is kept on its day and
 * clamped to that day's first or last minute: the day's total stays right, and only a chart of
 * a day recorded in another zone is approximate at its edges.
 */
fun minuteOfDay(epochMinute: Long, localEpochDay: Long, zone: ZoneId): Int {
    val local = Instant.ofEpochSecond(epochMinute * SECONDS_PER_MINUTE).atZone(zone).toLocalDateTime()
    val day = LocalDate.ofEpochDay(localEpochDay)
    return when {
        local.toLocalDate().isBefore(day) -> 0
        local.toLocalDate().isAfter(day) -> MINUTES_PER_DAY - 1
        else -> local.hour * MINUTES_PER_HOUR + local.minute
    }
}

private const val SECONDS_PER_MINUTE = 60L
private const val MINUTES_PER_HOUR = 60
