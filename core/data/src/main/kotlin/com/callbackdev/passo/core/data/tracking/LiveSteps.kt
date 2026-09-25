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

    private val running = MutableStateFlow(false)

    /** Null while the service is not running, or has not read today's stored count yet. */
    val today: StateFlow<LiveToday?> = state.asStateFlow()

    /**
     * Whether the tracking service is alive in this process. A widget drawn by a process that
     * was started without it (after the system stopped the service) says so, rather than
     * showing a count that has stopped moving as if it still moved.
     */
    val serviceRunning: StateFlow<Boolean> = running.asStateFlow()

    fun publish(today: LiveToday?) {
        state.value = today
    }

    fun setServiceRunning(alive: Boolean) {
        running.value = alive
    }
}

/**
 * A day's stored minutes plus the ones the service still holds, added per minute: what a
 * screen or a widget shows is the stored truth and the buffer, never one of the two.
 */
fun List<MinuteSteps>.withPending(pending: List<MinuteSteps>): List<MinuteSteps> {
    if (pending.isEmpty()) return this
    val byMinute = LinkedHashMap<Long, MinuteSteps>()
    for (minute in this + pending) {
        val existing = byMinute[minute.epochMinute]
        byMinute[minute.epochMinute] = existing?.copy(steps = existing.steps + minute.steps) ?: minute
    }
    return byMinute.values.toList()
}
