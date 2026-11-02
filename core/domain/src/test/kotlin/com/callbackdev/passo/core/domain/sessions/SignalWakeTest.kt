package com.callbackdev.passo.core.domain.sessions

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

    private companion object {
        const val MINUTE = 60_000L
    }
}
