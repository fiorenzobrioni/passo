package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionVoice
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SignalWakeTest {
    private val counting = checkNotNull(SessionPlans.start(SessionPlans.JAPANESE_WALKING, 0, 0, 0, 8_000))

    @Test
    fun `only a counting outing with signals, on a phone its counter cannot wake`() {
        assertThat(SignalWake.needed(counting, wakeUpCounter = false, signalsAllowed = true)).isTrue()
        assertThat(SignalWake.needed(counting, wakeUpCounter = true, signalsAllowed = true)).isFalse()
        assertThat(SignalWake.needed(null, wakeUpCounter = false, signalsAllowed = true)).isFalse()
    }

    @Test
    fun `paused or over, it sleeps`() {
        assertThat(SignalWake.needed(counting.copy(state = SessionState.PAUSED), false, true)).isFalse()
        assertThat(SignalWake.needed(counting.copy(state = SessionState.FINISHED), false, true)).isFalse()
    }

    @Test
    fun `an outing with nothing to tell, or silenced, does not keep it awake`() {
        val silent = counting.copy(vibrate = false, voice = SessionVoice.OFF)
        assertThat(SignalWake.needed(silent, wakeUpCounter = false, signalsAllowed = true)).isFalse()
        assertThat(SignalWake.needed(counting, wakeUpCounter = false, signalsAllowed = false)).isFalse()
        val voiceOnly = counting.copy(vibrate = false, voice = SessionVoice.HEADPHONES)
        assertThat(SignalWake.needed(voiceOnly, wakeUpCounter = false, signalsAllowed = true)).isTrue()
    }

    @Test
    fun `the editor says it for a plan with signals, on such a phone only`() {
        val plan = SessionPlans.JAPANESE_WALKING
        assertThat(SignalWake.neededFor(plan, wakeUpCounter = false)).isTrue()
        assertThat(SignalWake.neededFor(plan, wakeUpCounter = true)).isFalse()
        assertThat(SignalWake.neededFor(plan.copy(vibrate = false), wakeUpCounter = false)).isFalse()
    }
}
