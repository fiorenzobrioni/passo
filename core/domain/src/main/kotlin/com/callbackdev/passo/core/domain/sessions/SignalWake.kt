package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionVoice

/**
 * When Passo keeps the processor awake for an outing's signals (docs/adr/0013-interval-walks.md,
 * amended 4 Oct 2026): on a phone whose step counter cannot wake it, while an outing that tells
 * its signals (a vibration or a voice) is counting, and only then. Elsewhere the wake-up counter
 * brings the steps on time by itself (ADR 0009), and an outing with no signal has nothing to tell
 * on time: its end and its numbers come from the steps' own timestamps whenever they arrive.
 *
 * Awake, the processor receives the counter's steps as they are taken, and the signals ride on
 * them as they always do: no timer, no alarm. Paused, over or with no outing, it sleeps.
 */
object SignalWake {
    /**
     * Whether [session] keeps the processor awake now.
     *
     * @param wakeUpCounter the phone has a wake-up step counter: then never.
     * @param signalsAllowed the signals would reach the reader (notifications and the outings'
     *   channel on): a silenced outing tells nothing, on time or late.
     */
    fun needed(session: Session?, wakeUpCounter: Boolean, signalsAllowed: Boolean): Boolean = session != null &&
        session.state == SessionState.ACTIVE &&
        !wakeUpCounter &&
        signalsAllowed &&
        (session.vibrate || session.voice != SessionVoice.OFF)

    /** Whether an outing started from [plan] would, on this phone: what the editor says. */
    fun neededFor(plan: SessionPlan, wakeUpCounter: Boolean): Boolean =
        !wakeUpCounter && (plan.vibrate || plan.voice != SessionVoice.OFF)

    /**
     * The longest it is held at once, whatever happens: the longest outing Passo keeps open, and
     * ten minutes over. Released long before by the outing's end; this bounds a fault.
     */
    const val TIMEOUT_MILLIS: Long = SessionConstants.MAX_SESSION_MILLIS + 10 * 60_000L
}
