package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionState

/**
 * When the satellite receiver is on (docs/adr/0016-location.md, decisions 5, 10 and 14): only
 * for an outing measured with GPS, and in it only while the reader walks. Pure, as `SignalWake`
 * is for the wake lock: the service asks at whatever arrives, a step batch or a batch of fixes,
 * and only obeys. No timer runs for it.
 */
object GpsDuty {
    /**
     * The receiver sleeps once the reader has been still this long: a traffic light and a
     * window's glance are under it and keep the route whole, while a bench, a shop or a chat are
     * over it and cost nothing. The next step wakes it (the outing's own wake-up step counter,
     * or the processor kept awake on a phone without one, brings that step on time).
     */
    const val SLEEP_AFTER_MILLIS: Long = 2 * 60_000L

    enum class State {
        /** No outing with GPS, or it is paused or over for good: the receiver and its service type are off. */
        OFF,

        /** The reader is walking: fixes are asked for. */
        AWAKE,

        /** Still for [SLEEP_AFTER_MILLIS]: the updates are removed until the next step. */
        ASLEEP,
    }

    /**
     * What the receiver does at [nowMillis].
     *
     * @param canKeepGoing the outing is over and can still be taken back («Keep going»,
     *   «Resume»): the reader is by definition still walking when it ends by its goal, by its cap
     *   or by a stillness noticed at a step, so the receiver is steered as while it counted, and
     *   a route taken back has no hole. For at most the fifteen minutes that lasts.
     * @param lastMotionAtMillis the last step seen, the ones kept aside after the end included.
     */
    fun state(session: Session?, canKeepGoing: Boolean, lastMotionAtMillis: Long, nowMillis: Long): State {
        if (session == null || !session.gps) return State.OFF
        val counting = session.state == SessionState.ACTIVE
        val takenBackStill = session.state == SessionState.FINISHED && canKeepGoing
        if (!counting && !takenBackStill) return State.OFF
        return if (nowMillis - lastMotionAtMillis > SLEEP_AFTER_MILLIS) State.ASLEEP else State.AWAKE
    }
}
