package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.Profile

/** One minute's count before and after a write added steps to it. */
data class MinuteChange(val stepsBefore: Int, val stepsAfter: Int)

/**
 * How a day's summary is made and kept (PLANNING.md §5): computed whole while the day is open,
 * frozen once it is over.
 */
object DaySummaries {
    /** The summary of a day made of [minuteSteps], every estimate computed with [profile]. */
    fun summarize(
        localEpochDay: Long,
        minuteSteps: Iterable<Int>,
        profile: Profile,
        goalSteps: Int,
        finalized: Boolean,
    ): DailySummary {
        val metrics = MetricsCalculator.day(minuteSteps, profile)
        return DailySummary(
            localEpochDay = localEpochDay,
            steps = metrics.steps,
            distanceMeters = metrics.distanceMeters,
            activeKcal = metrics.activeKcal,
            activeMinutes = metrics.activeMinutes,
            briskMinutes = metrics.briskMinutes,
            goalSteps = goalSteps,
            finalized = finalized,
        )
    }

    /**
     * A frozen day that steps reached late (a batch delivered after midnight, a gap back-filled
     * over it). The day keeps what it had; only the late steps are measured, with [profile],
     * as the difference they make to their minute. The profile of that day is not kept, so
     * this is exact when the profile has not changed since, and otherwise changes only the
     * share of the late steps, never the rest of the day.
     */
    fun withLateSteps(summary: DailySummary, changes: Iterable<MinuteChange>, profile: Profile): DailySummary {
        val lengths = StepLengths.of(profile)
        val weight = MetricsCalculator.weightKg(profile)
        val delta = changes.fold(DayMetrics.ZERO) { total, change ->
            total + MetricsCalculator.minute(change.stepsAfter, lengths, weight) -
                MetricsCalculator.minute(change.stepsBefore, lengths, weight)
        }
        return summary.copy(
            steps = summary.steps + delta.steps,
            distanceMeters = (summary.distanceMeters + delta.distanceMeters).coerceAtLeast(0.0),
            activeKcal = (summary.activeKcal + delta.activeKcal).coerceAtLeast(0.0),
            activeMinutes = (summary.activeMinutes + delta.activeMinutes).coerceAtLeast(0),
            briskMinutes = (summary.briskMinutes + delta.briskMinutes).coerceAtLeast(0),
        )
    }
}
