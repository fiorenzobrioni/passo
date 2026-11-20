package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.routes.GpsDuty.SLEEP_AFTER_MILLIS
import com.callbackdev.passo.core.domain.routes.GpsDuty.State
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** When the receiver is on (PLANNING.md §11 Phase 15; docs/adr/0016-location.md decisions 5, 10, 14). */
class GpsDutyTest {
    private val plan = SessionPlans.NEW.copy(gps = true)
    private val measured = checkNotNull(SessionPlans.start(plan, 0, 0, 0, 8_000, withGps = true))
    private val onSteps = checkNotNull(SessionPlans.start(plan, 0, 0, 0, 8_000, withGps = false))

    private fun state(
        session: com.callbackdev.passo.core.model.Session?,
        nowMillis: Long,
        canKeepGoing: Boolean = false,
        lastMotionAtMillis: Long = session?.lastStepAtMillis ?: 0,
    ) = GpsDuty.state(session, canKeepGoing, lastMotionAtMillis, nowMillis)

    @Test
    fun `no outing, no receiver`() {
        assertThat(state(null, nowMillis = 0)).isEqualTo(State.OFF)
    }

    @Test
    fun `an outing that does not measure with GPS never turns it on`() {
        assertThat(state(onSteps, nowMillis = 0)).isEqualTo(State.OFF)
        assertThat(state(SessionPlans.start(SessionPlans.NEW, 0, 0, 0, 8_000, withGps = true), 0)).isEqualTo(State.OFF)
    }

    @Test
    fun `awake from the start, before the first step`() {
        assertThat(state(measured, nowMillis = 0)).isEqualTo(State.AWAKE)
        assertThat(state(measured, nowMillis = SLEEP_AFTER_MILLIS)).isEqualTo(State.AWAKE)
    }

    @Test
    fun `asleep after two minutes without a step`() {
        val walked = measured.copy(lastStepAtMillis = 10 * MINUTE)

        assertThat(state(walked, nowMillis = 10 * MINUTE + SLEEP_AFTER_MILLIS)).isEqualTo(State.AWAKE)
        assertThat(state(walked, nowMillis = 10 * MINUTE + SLEEP_AFTER_MILLIS + 1)).isEqualTo(State.ASLEEP)
    }

    @Test
    fun `awake at the next step`() {
        val stopped = measured.copy(lastStepAtMillis = 10 * MINUTE)
        val walking = stopped.copy(lastStepAtMillis = 20 * MINUTE)

        assertThat(state(stopped, nowMillis = 20 * MINUTE)).isEqualTo(State.ASLEEP)
        assertThat(state(walking, nowMillis = 20 * MINUTE)).isEqualTo(State.AWAKE)
    }

    @Test
    fun `off at a pause`() {
        val paused = measured.copy(state = SessionState.PAUSED, pausedAtMillis = MINUTE)

        assertThat(state(paused, nowMillis = MINUTE)).isEqualTo(State.OFF)
    }

    @Test
    fun `off at an end that cannot be taken back`() {
        val over = measured.copy(state = SessionState.FINISHED, endedAtMillis = MINUTE)

        assertThat(state(over, nowMillis = MINUTE, canKeepGoing = false)).isEqualTo(State.OFF)
    }

    @Test
    fun `steered as while it counted, as long as the end can be taken back`() {
        val reached = measured.copy(
            state = SessionState.FINISHED,
            endedAtMillis = 30 * MINUTE,
            lastStepAtMillis = 30 * MINUTE,
        )

        // The reader walks on after the goal: the route of an outing taken back has no hole.
        assertThat(state(reached, nowMillis = 31 * MINUTE, canKeepGoing = true)).isEqualTo(State.AWAKE)
        // They sat down: asleep two minutes after the last step, as while it counted.
        assertThat(state(reached, nowMillis = 33 * MINUTE, canKeepGoing = true)).isEqualTo(State.ASLEEP)
        // A step kept aside after the end wakes it.
        assertThat(
            state(reached, nowMillis = 40 * MINUTE, canKeepGoing = true, lastMotionAtMillis = 40 * MINUTE),
        ).isEqualTo(State.AWAKE)
        // The window closed, or the card was put away.
        assertThat(state(reached, nowMillis = 46 * MINUTE, canKeepGoing = false)).isEqualTo(State.OFF)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
