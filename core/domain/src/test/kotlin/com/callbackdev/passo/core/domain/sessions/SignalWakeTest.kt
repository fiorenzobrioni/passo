package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.SessionMarks
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionVoice
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignalWakeTest {
    private val counting = checkNotNull(SessionPlans.start(SessionPlans.JAPANESE_WALKING, 0, 0, 0, 8_000))

    @Test
    fun `only a counting outing with signals, on a phone its counter cannot wake`() {
        assertThat(SignalWake.needed(counting, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isTrue()
        assertThat(SignalWake.needed(counting, wakeUpCounter = true, signalsAllowed = true, nowMillis = 0)).isFalse()
        assertThat(SignalWake.needed(null, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isFalse()
    }

    @Test
    fun `paused or over, it sleeps`() {
        assertThat(SignalWake.needed(counting.copy(state = SessionState.PAUSED), false, true, 0)).isFalse()
        assertThat(SignalWake.needed(counting.copy(state = SessionState.FINISHED), false, true, 0)).isFalse()
    }

    @Test
    fun `an outing with nothing to tell, or silenced, does not keep it awake`() {
        val silent = counting.copy(vibrate = false, voice = SessionVoice.OFF)
        assertThat(SignalWake.needed(silent, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isFalse()
        assertThat(SignalWake.needed(counting, wakeUpCounter = false, signalsAllowed = false, nowMillis = 0)).isFalse()
        val voiceOnly = counting.copy(vibrate = false, voice = SessionVoice.HEADPHONES)
        assertThat(SignalWake.needed(voiceOnly, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isTrue()
    }

    @Test
    fun `still for fifteen minutes, it is let go`() {
        val walked = counting.copy(lastStepAtMillis = 10 * MINUTE)
        val letGo = 10 * MINUTE + SessionConstants.IDLE_END_MILLIS

        assertThat(SignalWake.letGoAt(walked)).isEqualTo(letGo)
        assertThat(
            SignalWake.needed(walked, wakeUpCounter = false, signalsAllowed = true, nowMillis = letGo - 1),
        ).isTrue()
        assertThat(SignalWake.needed(walked, wakeUpCounter = false, signalsAllowed = true, nowMillis = letGo)).isFalse()
    }

    @Test
    fun `a step moves the moment it is let go`() {
        val first = counting.copy(lastStepAtMillis = 10 * MINUTE)
        val later = first.copy(lastStepAtMillis = 20 * MINUTE)

        assertThat(SignalWake.letGoAt(later) - SignalWake.letGoAt(first)).isEqualTo(10 * MINUTE)
        assertThat(SignalWake.needed(later, false, true, nowMillis = SignalWake.letGoAt(first))).isTrue()
    }

    @Test
    fun `never past the longest outing, walking or not`() {
        val long = counting.copy(lastStepAtMillis = SessionConstants.MAX_SESSION_MILLIS - MINUTE)

        assertThat(SignalWake.letGoAt(long)).isEqualTo(SessionConstants.MAX_SESSION_MILLIS)
        assertThat(SignalWake.needed(long, false, true, nowMillis = SessionConstants.MAX_SESSION_MILLIS)).isFalse()
        assertThat(SignalWake.TIMEOUT_MILLIS).isGreaterThan(SessionConstants.MAX_SESSION_MILLIS)
    }

    @Test
    fun `the editor says it for a plan with signals, on such a phone only`() {
        val plan = SessionPlans.JAPANESE_WALKING
        assertThat(SignalWake.neededFor(plan, wakeUpCounter = false)).isTrue()
        assertThat(SignalWake.neededFor(plan, wakeUpCounter = true)).isFalse()
        assertThat(SignalWake.neededFor(plan.copy(vibrate = false), wakeUpCounter = false)).isFalse()
    }

    @Test
    fun `an outing with GPS keeps it awake with no signal at all, on a phone its counter cannot wake`() {
        // The steps steer the receiver there: asleep at a stop, awake at the next step.
        val silent = counting.copy(gps = true, vibrate = false, voice = SessionVoice.OFF)

        assertThat(SignalWake.needed(silent, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isTrue()
        assertThat(SignalWake.needed(silent, wakeUpCounter = false, signalsAllowed = false, nowMillis = 0)).isTrue()
        assertThat(SignalWake.needed(silent, wakeUpCounter = true, signalsAllowed = true, nowMillis = 0)).isFalse()
        assertThat(SignalWake.needed(silent.copy(state = SessionState.PAUSED), false, true, 0)).isFalse()
        assertThat(SignalWake.needed(silent, false, true, nowMillis = SignalWake.letGoAt(silent))).isFalse()
    }

    @Test
    fun `the editor says it for a silent plan with GPS too, on such a phone only`() {
        val silent = SessionPlans.NEW.copy(gps = true, vibrate = false, voice = SessionVoice.OFF)

        assertThat(SignalWake.neededFor(silent, wakeUpCounter = false)).isTrue()
        assertThat(SignalWake.neededFor(silent, wakeUpCounter = true)).isFalse()
        assertThat(SignalWake.neededFor(silent.copy(gps = false), wakeUpCounter = false)).isFalse()
    }

    @Test
    fun `a phone with no wake-up counter, an open outing with no marks and no GPS keeps no lock`() {
        val plan = SessionPlans.OPEN_WALK.copy(marks = SessionMarks.NONE)
        val open = checkNotNull(SessionPlans.start(plan, 0, 0, 0, 8_000))
        // It vibrates and speaks only for its end and its cap's question, which wait for the steps.
        val speaking = open.copy(vibrate = true, voice = SessionVoice.HEADPHONES)
        assertThat(SignalWake.needed(speaking, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isFalse()
        assertThat(SignalWake.neededFor(plan, wakeUpCounter = false)).isFalse()
    }

    @Test
    fun `an open outing with marks keeps it awake as a goal outing with signals does`() {
        // The open walk tells ten minutes without GPS, unless the reader chose otherwise.
        val open = checkNotNull(SessionPlans.start(SessionPlans.OPEN_WALK, 0, 0, 0, 8_000))
        assertThat(open.marks).isEqualTo(SessionMarks.TIME)
        assertThat(SignalWake.needed(open, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isTrue()
        assertThat(SignalWake.needed(open, wakeUpCounter = true, signalsAllowed = true, nowMillis = 0)).isFalse()
        assertThat(SignalWake.neededFor(SessionPlans.OPEN_WALK, wakeUpCounter = false)).isTrue()
        val kilometres = SessionPlans.OPEN_WALK.copy(marks = SessionMarks.DISTANCE)
        assertThat(SignalWake.neededFor(kilometres, wakeUpCounter = false)).isTrue()
        // Its bound is its own twelve hours, not a goal outing's four.
        assertThat(SignalWake.timeoutMillis(open, nowMillis = 0))
            .isEqualTo(SessionConstants.OPEN_MAX_SESSION_MILLIS + SignalWake.TIMEOUT_MARGIN_MILLIS)
    }

    @Test
    fun `marks set to none, no wake lock for them`() {
        val none =
            checkNotNull(SessionPlans.start(SessionPlans.OPEN_WALK.copy(marks = SessionMarks.NONE), 0, 0, 0, 8_000))
        assertThat(SignalWake.needed(none, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isFalse()
    }

    @Test
    fun `marks with neither vibration nor voice, or the outings' channel silenced, keep nothing awake`() {
        val open = checkNotNull(SessionPlans.start(SessionPlans.OPEN_WALK, 0, 0, 0, 8_000))
        val mute = open.copy(vibrate = false, voice = SessionVoice.OFF)
        assertThat(SignalWake.needed(mute, wakeUpCounter = false, signalsAllowed = true, nowMillis = 0)).isFalse()
        assertThat(SignalWake.neededFor(SessionPlans.OPEN_WALK.copy(vibrate = false), wakeUpCounter = false)).isFalse()
        assertThat(SignalWake.needed(open, wakeUpCounter = false, signalsAllowed = false, nowMillis = 0)).isFalse()
    }

    @Test
    fun `the same with GPS, the lock bounded ten minutes past its own twelve hours`() {
        val open =
            checkNotNull(SessionPlans.start(SessionPlans.OPEN_WALK.copy(gps = true), 0, 0, 0, 8_000, withGps = true))
        assertThat(SignalWake.needed(open, wakeUpCounter = false, signalsAllowed = false, nowMillis = 0)).isTrue()
        assertThat(SignalWake.neededFor(SessionPlans.OPEN_WALK.copy(gps = true), wakeUpCounter = false)).isTrue()
        assertThat(SignalWake.timeoutMillis(open, nowMillis = 0))
            .isEqualTo(SessionConstants.OPEN_MAX_SESSION_MILLIS + SignalWake.TIMEOUT_MARGIN_MILLIS)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
