package com.callbackdev.passo.core.domain.tracking

import com.callbackdev.passo.core.model.StepSample

/**
 * Turns the sensor's raw reading into a [StepSample], or null when it cannot be a count.
 *
 * The counter is reported as a `Float`: it is exact up to 2^24 (16 777 216) steps in one boot
 * session, far beyond what anyone walks between two reboots, so a plain `toLong()` is lossless
 * in practice (PLANNING.md §4.6).
 */
fun stepSampleOf(value: Float, timestampNanos: Long): StepSample? {
    if (value.isNaN() || value.isInfinite() || value < 0f) return null
    return StepSample(counterValue = value.toLong(), eventElapsedNanos = timestampNanos)
}
