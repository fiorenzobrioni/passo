package com.callbackdev.passo.core.data.tracking

import com.callbackdev.passo.core.model.MinuteSteps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Today as the tracking service holds it right now, including the steps it has not written
 * yet. The database is written in batches (PLANNING.md §4.5), so a screen reading only the
 * database would move once a minute; with this it moves with the sensor while the screen is
 * on (one-second latency, §4.3), at no cost to the battery: the service publishes what it
 * already computes for its notification, and nobody reads it with the screen off.
 *
 * @property steps today's total: stored plus buffered.
 * @property pending the buffered minutes of today, not in the database yet.
 */
data class LiveToday(val localEpochDay: Long, val steps: Int, val pending: List<MinuteSteps>)

/** Shared by the service and the screens, which live in one process. */
@Singleton
class LiveSteps
@Inject
constructor() {
    private val state = MutableStateFlow<LiveToday?>(null)

    /** Null while the service is not running, or has not read today's stored count yet. */
    val today: StateFlow<LiveToday?> = state.asStateFlow()

    fun publish(today: LiveToday?) {
        state.value = today
    }
}
