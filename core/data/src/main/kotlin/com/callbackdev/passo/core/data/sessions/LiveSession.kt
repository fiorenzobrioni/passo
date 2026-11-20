package com.callbackdev.passo.core.data.sessions

import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The outing as the tracking service holds it right now, ahead of what it has written (the
 * steps are written in batches): what Today's card moves with while it is visible, as
 * `LiveSteps` is for the day.
 *
 * @property cadence the last half minute's steps per minute; null before it is a pace, or paused.
 * @property canKeepGoing ended by its goal a moment ago: "Keep going" would reopen it.
 * @property alertsWhileScreenOff this phone's step counter can wake it for a signal; without it
 *   a signal with the screen off waits for the phone to wake for another reason.
 * @property measure what an outing measured with GPS shows beyond its stored numbers (the
 *   stretch whose fixes are not in yet, the pace now) and what the card says of the receiver,
 *   or of an outing that started on the steps although its plan asked for GPS. Empty for an
 *   outing read from what was stored: only the service knows the receiver.
 */
data class LiveSessionState(
    val session: Session,
    val cadence: Int?,
    val canKeepGoing: Boolean,
    val alertsWhileScreenOff: Boolean,
    val measure: LiveMeasure = LiveMeasure(),
)

@Singleton
class LiveSession
@Inject
constructor() {
    private val state = MutableStateFlow<LiveSessionState?>(null)

    /** Null while there is no outing under way, paused, or just ended by its goal. */
    val current: StateFlow<LiveSessionState?> = state.asStateFlow()

    fun publish(live: LiveSessionState?) {
        state.value = live
    }
}
