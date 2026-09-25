package com.callbackdev.passo.core.domain.walks

import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.MIXED_WALK_SHARE
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.WALK_MAX_GAP_MINUTES
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.WALK_MINUTE_THRESHOLD
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.MINUTES_PER_DAY
import com.callbackdev.passo.core.model.Profile
import kotlin.math.roundToInt

enum class WalkType {
    WALK,
    RUN,

    /** Both walking and running, each for a good part of it (PLANNING.md §6.1). */
    MIXED,
}

/**
 * A stretch of sustained stepping, found in a day's minutes (PLANNING.md §6.1).
 *
 * @property startMinute the first minute of the walk, counted from local midnight.
 * @property endMinute the minute after its last one: a walk from 10:12 to 10:47 has 35 minutes.
 * @property averageCadence steps per minute over the whole walk, pauses included, the way
 *   the walker lived it: «35 minutes, 3,420 steps, 98 spm».
 */
data class Walk(
    val startMinute: Int,
    val endMinute: Int,
    val steps: Int,
    val distanceMeters: Double,
    val activeKcal: Double,
    val averageCadence: Int,
    val type: WalkType,
) {
    val minutes: Int get() = endMinute - startMinute
}

/**
 * Walks from the minute buckets of one local day (PLANNING.md §6.1), computed on read: no
 * table, no sensor, no background work. Only when a screen that shows them is open.
 *
 * 1. A minute is **moving** from [WALK_MINUTE_THRESHOLD] steps.
 * 2. Moving minutes join into one run across up to [WALK_MAX_GAP_MINUTES] quieter minutes; the
 *    steps in a bridged pause belong to the walk.
 * 3. A run shorter than the reader's minimum is not a walk.
 * 4. Each walk is measured with the calculators every other number uses.
 * 5. It is a run from an average [RUNNING_CADENCE], mixed when both its walking and its running
 *    minutes are at least [MIXED_WALK_SHARE] of it.
 *
 * The input is one day, so a walk that crosses midnight is two: one ending at midnight, one
 * starting from it, like every other daily number.
 */
object WalkDetector {
    fun detect(minutes: Iterable<DayMinute>, profile: Profile, minWalkMinutes: Int): List<Walk> {
        val day = IntArray(MINUTES_PER_DAY)
        for (minute in minutes) {
            if (minute.steps > 0) day[minute.minuteOfDay.coerceIn(0, MINUTES_PER_DAY - 1)] += minute.steps
        }
        val lengths = StepLengths.of(profile)
        val weight = MetricsCalculator.weightKg(profile)
        val walks = mutableListOf<Walk>()
        var start = -1
        var last = -1
        for (minute in 0 until MINUTES_PER_DAY) {
            if (day[minute] < WALK_MINUTE_THRESHOLD) continue
            if (start >= 0 && minute - last - 1 > WALK_MAX_GAP_MINUTES) {
                measure(day, start, last + 1, minWalkMinutes, lengths, weight)?.let(walks::add)
                start = -1
            }
            if (start < 0) start = minute
            last = minute
        }
        if (start >= 0) measure(day, start, last + 1, minWalkMinutes, lengths, weight)?.let(walks::add)
        return walks
    }

    private fun measure(
        day: IntArray,
        start: Int,
        end: Int,
        minWalkMinutes: Int,
        lengths: StepLengths,
        weightKg: Double,
    ): Walk? {
        val length = end - start
        if (length < minWalkMinutes) return null
        var steps = 0
        var distance = 0.0
        var kcal = 0.0
        var walking = 0
        var running = 0
        for (minute in start until end) {
            val count = day[minute]
            val metrics = MetricsCalculator.minute(count, lengths, weightKg)
            steps += count
            distance += metrics.distanceMeters
            kcal += metrics.activeKcal
            when {
                count >= RUNNING_CADENCE -> running++
                count >= WALK_MINUTE_THRESHOLD -> walking++
            }
        }
        val cadence = (steps.toDouble() / length).roundToInt()
        val type = when {
            walking >= length * MIXED_WALK_SHARE && running >= length * MIXED_WALK_SHARE -> WalkType.MIXED
            cadence >= RUNNING_CADENCE -> WalkType.RUN
            else -> WalkType.WALK
        }
        return Walk(start, end, steps, distance, kcal, cadence, type)
    }
}
