package com.callbackdev.passo.feature.today

import com.callbackdev.passo.core.domain.today.DayMinute
import java.util.Random

/**
 * Realistic days for the README's screenshots: a weekday of an office worker who walks to the
 * station, takes a lunch walk and walks back, with the scattered steps of a house and an office
 * in between. Seeded, so every run draws the same day.
 */
internal object SampleDays {
    fun workday(seed: Long, lunchWalkMinutes: Int = 22, until: Int = 24 * 60): List<DayMinute> {
        val random = Random(seed)
        val steps = sortedMapOf<Int, Int>()
        fun add(minute: Int, count: Int) {
            if (minute < until && count > 0) steps.merge(minute, count, Int::plus)
        }
        fun walk(start: Int, length: Int, cadence: Int) {
            for (i in 0 until length) add(start + i, cadence + random.nextInt(-7, 8))
        }
        fun scatter(from: Int, to: Int, minutesPerHour: Int) {
            for (minute in from until to) if (random.nextInt(60) < minutesPerHour) add(minute, 6 + random.nextInt(34))
        }
        scatter(6 * 60 + 50, 7 * 60 + 50, 22)
        walk(7 * 60 + 55 + random.nextInt(-4, 5), 18 + random.nextInt(-2, 3), 108)
        scatter(8 * 60 + 20, 12 * 60 + 30, 12)
        walk(12 * 60 + 35, lunchWalkMinutes, 104)
        scatter(12 * 60 + 35 + lunchWalkMinutes, 17 * 60 + 35, 12)
        walk(17 * 60 + 40 + random.nextInt(-5, 6), 20 + random.nextInt(-2, 3), 110)
        scatter(18 * 60 + 5, 22 * 60 + 30, 14)
        return steps.map { (minute, count) -> DayMinute(minute, count) }
    }

    /** Four past Thursdays, each a little different: the usual day is their average. */
    val pastThursdays: List<List<DayMinute>> = listOf(11L, 23L, 37L, 41L).map { workday(it) }
}
