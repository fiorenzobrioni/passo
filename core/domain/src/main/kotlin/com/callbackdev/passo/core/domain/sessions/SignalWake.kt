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
 * An outing measured with GPS needs its steps on time too, signals or not
 * (docs/adr/0016-location.md decision 5, amended 18 Nov 2026): there the steps steer the
 * receiver, which sleeps when they stop and wakes at the next one. On a phone nothing can wake,
 * a receiver put to sleep with no signal to keep the processor up would stay asleep until the
 * screen came on, and the rest of the walk would be laid from the steps.
 *
 * Awake, the processor receives the counter's steps as they are taken, and the signals ride on
 * them as they always do: no alarm. Paused, over or with no outing, it sleeps.
 *
 * Still for [SessionConstants.IDLE_END_MILLIS], an outing is over (it ends at its last step), even
 * though it is closed only at the next step or when the reader looks, as on every phone: from
 * that moment it has nothing left to tell on time, and the processor is let go ([letGoAt]). A
 * forgotten outing on the table kept it awake until the screen came on, up to four hours (owner,
 * 2 Nov 2026). The service's one timer with the screen off says when: it runs only while this
 * lock already keeps the processor awake, so it shortens a wake and never makes one.
 */
object SignalWake {
    /**
     * Whether [session] keeps the processor awake at [nowMillis].
     *
     * @param wakeUpCounter the phone has a wake-up step counter: then never.
     * @param signalsAllowed the signals would reach the reader (notifications and the outings'
     *   channel on): a silenced outing tells nothing, on time or late. An outing with GPS holds
     *   it either way: its steps are for the receiver, not for the reader's ear.
     */
    fun needed(session: Session?, wakeUpCounter: Boolean, signalsAllowed: Boolean, nowMillis: Long): Boolean =
        session != null &&
            session.state == SessionState.ACTIVE &&
            !wakeUpCounter &&
            (session.gps || (signalsAllowed && (session.vibrate || session.voice != SessionVoice.OFF))) &&
            nowMillis < letGoAt(session)

    /**
     * When a counting [session] stops keeping the processor awake: [SessionConstants.IDLE_END_MILLIS]
     * after its last step, when it is over by stillness, or at the outing's own cap
     * ([capMillis]: [SessionConstants.MAX_SESSION_MILLIS] from its start until the reader says to
     * go on); whichever comes first.
     */
    fun letGoAt(session: Session): Long = minOf(
        session.lastStepAtMillis + SessionConstants.IDLE_END_MILLIS,
        session.capMillis,
    )

    /**
     * How long the lock may be held from [nowMillis] at the most, whatever happens: to
     * [TIMEOUT_MARGIN_MILLIS] past the outing's own cap, which «Keep going» moves on (the lock is
     * then taken again with the new bound). It bounds a fault; [letGoAt] is what lets it go.
     */
    fun timeoutMillis(session: Session, nowMillis: Long): Long =
        (session.capMillis + TIMEOUT_MARGIN_MILLIS - nowMillis).coerceAtLeast(TIMEOUT_MARGIN_MILLIS)

    /** Whether an outing started from [plan] would, on this phone: what the editor says. */
    fun neededFor(plan: SessionPlan, wakeUpCounter: Boolean): Boolean =
        !wakeUpCounter && (plan.gps || plan.vibrate || plan.voice != SessionVoice.OFF)

    /**
     * The longest it is held at once, whatever happens: the longest outing Passo keeps open, and
     * ten minutes over. Released long before by the outing's end or its stillness ([letGoAt]);
     * this bounds a fault.
     */
    const val TIMEOUT_MILLIS: Long = SessionConstants.MAX_SESSION_MILLIS + 10 * 60_000L

    /** Past the cap, the lock's bound leaves this much room for the end to be noticed. */
    const val TIMEOUT_MARGIN_MILLIS: Long = 10 * 60_000L
}
