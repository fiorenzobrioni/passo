package com.callbackdev.passo.core.domain.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountingStateTest {
    private fun state(
        hasSensor: Boolean = true,
        onboarded: Boolean = true,
        hasPermission: Boolean = true,
        enabled: Boolean = true,
        serviceRunning: Boolean = true,
    ) = CountingState.of(hasSensor, onboarded, hasPermission, enabled, serviceRunning)

    @Test
    fun `everything in place is counting`() {
        assertThat(state()).isEqualTo(CountingState.COUNTING)
    }

    @Test
    fun `each missing piece names itself`() {
        assertThat(state(serviceRunning = false)).isEqualTo(CountingState.STOPPED)
        assertThat(state(enabled = false, serviceRunning = false)).isEqualTo(CountingState.PAUSED)
        assertThat(state(hasPermission = false, serviceRunning = false)).isEqualTo(CountingState.PERMISSION_NEEDED)
        assertThat(state(onboarded = false, hasPermission = false)).isEqualTo(CountingState.NOT_SET_UP)
        assertThat(state(hasSensor = false, onboarded = false)).isEqualTo(CountingState.NO_SENSOR)
    }

    @Test
    fun `a pause is the reader's choice, so it wins over a stopped service`() {
        assertThat(state(enabled = false, serviceRunning = false)).isEqualTo(CountingState.PAUSED)
    }

    @Test
    fun `only the states with a day behind them draw a count`() {
        assertThat(CountingState.entries.filter { it.hasCount }).containsExactly(
            CountingState.COUNTING,
            CountingState.PAUSED,
            CountingState.PERMISSION_NEEDED,
            CountingState.STOPPED,
        )
    }
}
