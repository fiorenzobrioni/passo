package com.callbackdev.passo.core.model

/**
 * One event from the hardware step counter (PLANNING.md §4.4).
 *
 * @property counterValue steps since the device booted, as the sensor reports them.
 * @property eventElapsedNanos the event's own timestamp, on the `elapsedRealtimeNanos` clock:
 *   batched events are delivered late, and this is what puts their steps in the right minute.
 */
data class StepSample(val counterValue: Long, val eventElapsedNanos: Long)
