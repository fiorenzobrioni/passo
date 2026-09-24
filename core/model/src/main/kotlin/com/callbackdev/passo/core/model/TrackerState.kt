package com.callbackdev.passo.core.model

/**
 * Where the counter stood at the last accounted sample, persisted with the steps it produced
 * (PLANNING.md §4.5): the stored steps plus this state are always consistent, so a sample that
 * arrives after a crash recomputes its delta from here and nothing is counted twice.
 *
 * @property lastSampleElapsedNanos the last sample's time on the monotonic clock. Gaps inside a
 *   boot session are measured on this clock, never on the wall clock, which the user can move.
 * @property lastSampleWallMillis the same instant on the wall clock, as it read then.
 */
data class TrackerState(
    val bootCount: Int,
    val lastCounterValue: Long,
    val lastSampleElapsedNanos: Long,
    val lastSampleWallMillis: Long,
)
