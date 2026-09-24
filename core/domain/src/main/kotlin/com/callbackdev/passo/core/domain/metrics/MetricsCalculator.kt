package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.ACTIVE_MINUTE_THRESHOLD
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.BRISK_MINUTE_THRESHOLD
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.DEFAULT_WEIGHT_KG
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.FAST_WALK_KCAL_PER_KG_KM
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUN_KCAL_PER_KG_KM
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.WALK_KCAL_PER_KG_KM
import com.callbackdev.passo.core.model.Profile
import kotlin.math.roundToInt

/**
 * What a set of minutes adds up to (PLANNING.md §6).
 *
 * @property activeSteps the steps taken in active minutes, what the average cadence is made of.
 */
data class DayMetrics(
    val steps: Int,
    val distanceMeters: Double,
    val activeKcal: Double,
    val activeMinutes: Int,
    val briskMinutes: Int,
    val activeSteps: Int,
) {
    /** Steps per minute over the active minutes; null when there were none. */
    val averageCadence: Int?
        get() = if (activeMinutes == 0) null else (activeSteps.toDouble() / activeMinutes).roundToInt()

    operator fun plus(other: DayMetrics): DayMetrics = DayMetrics(
        steps = steps + other.steps,
        distanceMeters = distanceMeters + other.distanceMeters,
        activeKcal = activeKcal + other.activeKcal,
        activeMinutes = activeMinutes + other.activeMinutes,
        briskMinutes = briskMinutes + other.briskMinutes,
        activeSteps = activeSteps + other.activeSteps,
    )

    operator fun minus(other: DayMetrics): DayMetrics = DayMetrics(
        steps = steps - other.steps,
        distanceMeters = distanceMeters - other.distanceMeters,
        activeKcal = activeKcal - other.activeKcal,
        activeMinutes = activeMinutes - other.activeMinutes,
        briskMinutes = briskMinutes - other.briskMinutes,
        activeSteps = activeSteps - other.activeSteps,
    )

    companion object {
        val ZERO = DayMetrics(0, 0.0, 0.0, 0, 0, 0)
    }
}

/**
 * The estimates of PLANNING.md §6, minute by minute. A minute's step count is its cadence:
 * the buckets are one minute wide, so a minute of 120 steps was walked at 120 steps per minute
 * (or in part of it at a higher one, which the bucket cannot tell).
 */
object MetricsCalculator {
    /** The metrics of every minute in [minuteSteps], added up, for [profile]. */
    fun day(minuteSteps: Iterable<Int>, profile: Profile): DayMetrics {
        val lengths = StepLengths.of(profile)
        val weight = weightKg(profile)
        return minuteSteps.fold(DayMetrics.ZERO) { total, steps -> total + minute(steps, lengths, weight) }
    }

    /** One minute of [steps]: distance at the step length of its cadence, energy at its cost. */
    fun minute(steps: Int, lengths: StepLengths, weightKg: Double): DayMetrics {
        if (steps <= 0) return DayMetrics.ZERO
        val distance = steps * lengths.forCadence(steps)
        val active = steps >= ACTIVE_MINUTE_THRESHOLD
        return DayMetrics(
            steps = steps,
            distanceMeters = distance,
            activeKcal = distance / METERS_PER_KM * weightKg * kcalPerKgPerKm(steps),
            activeMinutes = if (active) 1 else 0,
            briskMinutes = if (steps >= BRISK_MINUTE_THRESHOLD) 1 else 0,
            activeSteps = if (active) steps else 0,
        )
    }

    /**
     * Net energy per kilogram per kilometre at [stepsPerMinute]. Flat below a brisk cadence:
     * a low count in a one-minute bucket is mostly a minute walked only in part, not a slow
     * gait, so the slow-walking rise of the energy curve is deliberately not modelled.
     */
    fun kcalPerKgPerKm(stepsPerMinute: Int): Double = when {
        stepsPerMinute >= RUNNING_CADENCE -> RUN_KCAL_PER_KG_KM

        stepsPerMinute > BRISK_MINUTE_THRESHOLD -> {
            val share =
                (stepsPerMinute - BRISK_MINUTE_THRESHOLD).toDouble() / (RUNNING_CADENCE - BRISK_MINUTE_THRESHOLD)
            WALK_KCAL_PER_KG_KM + share * (FAST_WALK_KCAL_PER_KG_KM - WALK_KCAL_PER_KG_KM)
        }

        else -> WALK_KCAL_PER_KG_KM
    }

    /** The reader's weight, or [DEFAULT_WEIGHT_KG] when not given (or not plausible). */
    fun weightKg(profile: Profile): Double = profile.sanitized().weightKg ?: DEFAULT_WEIGHT_KG

    private const val METERS_PER_KM = 1_000.0
}
