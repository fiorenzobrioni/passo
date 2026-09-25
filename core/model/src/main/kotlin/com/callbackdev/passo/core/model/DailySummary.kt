package com.callbackdev.passo.core.model

/**
 * One local day's totals (PLANNING.md §5, `daily_summary`).
 *
 * @property goalSteps the goal in effect that day, kept with the day so history stays right
 *   when the goal changes.
 * @property finalized true once the day is over: its estimates were computed with the profile
 *   of that day and are never silently recomputed with a later one (VISION.md, "Past days are
 *   frozen"). Steps that reach a finalized day late only add their own share.
 */
data class DailySummary(
    val localEpochDay: Long,
    val steps: Int,
    val distanceMeters: Double,
    val activeKcal: Double,
    val activeMinutes: Int,
    val briskMinutes: Int,
    val goalSteps: Int,
    val finalized: Boolean,
) {
    val goalReached: Boolean
        get() = steps >= goalSteps
}
