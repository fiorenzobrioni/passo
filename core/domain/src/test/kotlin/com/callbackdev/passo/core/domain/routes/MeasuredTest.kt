package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.sessions.SessionConstants.CAP_WARNING_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MAX_SESSION_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** «Measured», the pace and the cap's question, as an outing's card reads them (ADR 0016, decisions 6, 7, 14). */
class MeasuredTest {
    private val plan = SessionPlans.NEW.copy(gps = true)
    private val measured = checkNotNull(SessionPlans.start(plan, 0, 0, 0, 8_000, withGps = true))
    private val onSteps = checkNotNull(SessionPlans.start(plan, 0, 0, 0, 8_000, withGps = false))

    @Test
    fun `measured is a word for a distance at least nine tenths from fixes`() {
        assertThat(measured.copy(measuredShare = 0.9).distanceKind()).isEqualTo(DistanceKind.MEASURED)
        assertThat(measured.copy(measuredShare = 1.0).distanceKind()).isEqualTo(DistanceKind.MEASURED)
        assertThat(measured.copy(measuredShare = 0.89).distanceKind()).isEqualTo(DistanceKind.PARTLY_MEASURED)
    }

    @Test
    fun `before the first fix an outing with GPS is the steps' - not measured yet`() {
        assertThat(measured.distanceKind()).isEqualTo(DistanceKind.PARTLY_MEASURED)
        assertThat(measured.stepsShare()).isEqualTo(1.0)
    }

    @Test
    fun `an outing without GPS is an estimate, whatever its numbers`() {
        assertThat(onSteps.distanceKind()).isEqualTo(DistanceKind.ESTIMATED)
        assertThat(onSteps.copy(measuredShare = 1.0).distanceKind()).isEqualTo(DistanceKind.ESTIMATED)
        assertThat(onSteps.stepsShare()).isNull()
    }

    @Test
    fun `the share from the steps is what the fixes did not give`() {
        assertThat(measured.copy(measuredShare = 0.7).stepsShare()).isWithin(1e-9).of(0.3)
    }

    @Test
    fun `a pace only over a measured distance`() {
        val walked = SessionTotals(steps = 6_500, movingMillis = 52 * MINUTE, distanceMeters = 5_000.0)

        assertThat(measured.copy(totals = walked, measuredShare = 0.97).pace()).isWithin(1e-9).of(624.0)
        // Half of it from the steps: its pace would be the cadence in disguise.
        assertThat(measured.copy(totals = walked, measuredShare = 0.5).pace()).isNull()
        assertThat(onSteps.copy(totals = walked).pace()).isNull()
    }

    @Test
    fun `no pace under two hundred metres`() {
        val short = SessionTotals(steps = 200, movingMillis = 2 * MINUTE, distanceMeters = 150.0)

        assertThat(measured.copy(totals = short, measuredShare = 1.0).pace()).isNull()
    }

    @Test
    fun `the cap's question is open from when it is asked to the cap`() {
        val cap = MAX_SESSION_MILLIS
        val asked = measured.copy(capAskedForMillis = cap)

        assertThat(measured.capQuestionMinutes(cap - CAP_WARNING_MILLIS)).isNull()
        assertThat(asked.capQuestionMinutes(cap - CAP_WARNING_MILLIS)).isEqualTo(15)
        assertThat(asked.capQuestionMinutes(cap - 5 * MINUTE + 1)).isEqualTo(5)
        assertThat(asked.capQuestionMinutes(cap - 1)).isEqualTo(1)
        assertThat(asked.capQuestionMinutes(cap + 1)).isNull()
    }

    @Test
    fun `answered, the question is closed until the next cap asks`() {
        val cap = MAX_SESSION_MILLIS
        val answered = measured.copy(capAskedForMillis = cap, capAtMillis = cap + MAX_SESSION_MILLIS)

        assertThat(answered.capQuestionMinutes(cap - MINUTE)).isNull()
    }

    @Test
    fun `no question for an outing paused or over`() {
        val cap = MAX_SESSION_MILLIS
        val paused = measured.copy(capAskedForMillis = cap, state = SessionState.PAUSED)

        assertThat(paused.capQuestionMinutes(cap - MINUTE)).isNull()
    }

    @Test
    fun `an outing without GPS asks the same question`() {
        val cap = MAX_SESSION_MILLIS

        assertThat(onSteps.copy(capAskedForMillis = cap).capQuestionMinutes(cap - 10 * MINUTE)).isEqualTo(10)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
