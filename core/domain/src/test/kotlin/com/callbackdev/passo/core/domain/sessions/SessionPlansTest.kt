package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionPlansTest {
    private val lengths = StepLengths(walkingMeters = 0.7, runningMeters = 0.91)

    private fun plan(kind: SessionGoalKind, value: Int, intensity: SessionIntensity = SessionIntensity.BRISK) =
        SessionPlan(id = 7, goalKind = kind, goalValue = value, intensity = intensity)

    @Test
    fun `a brisk time goal is estimated at 100 steps a minute`() {
        val estimate = SessionPlans.estimate(plan(SessionGoalKind.TIME, 20), lengths, 0)
        assertThat(estimate.steps).isEqualTo(2_000)
        assertThat(estimate.distanceMeters).isWithin(1e-9).of(1_400.0)
        assertThat(estimate.minutes).isEqualTo(20)
    }

    @Test
    fun `a run is estimated with the running step`() {
        val estimate = SessionPlans.estimate(plan(SessionGoalKind.DISTANCE, 5_000, SessionIntensity.RUN), lengths, 0)
        assertThat(estimate.steps).isEqualTo(5_495)
        assertThat(estimate.minutes).isEqualTo(40)
    }

    @Test
    fun `switching the goal keeps the same outing in the new quantity`() {
        val twenty = plan(SessionGoalKind.TIME, 20)
        assertThat(SessionPlans.convert(twenty, SessionGoalKind.STEPS, lengths, 0)).isEqualTo(2_000)
        assertThat(SessionPlans.convert(twenty, SessionGoalKind.DISTANCE, lengths, 0)).isEqualTo(1_500)
        assertThat(SessionPlans.convert(twenty, SessionGoalKind.TIME, lengths, 0)).isEqualTo(20)
    }

    @Test
    fun `values snap to the editor's steps and range`() {
        assertThat(SessionPlans.clampValue(SessionGoalKind.STEPS, 2_260)).isEqualTo(2_500)
        assertThat(SessionPlans.clampValue(SessionGoalKind.STEPS, 10)).isEqualTo(500)
        assertThat(SessionPlans.clampValue(SessionGoalKind.TIME, 999)).isEqualTo(180)
        assertThat(SessionPlans.clampValue(SessionGoalKind.DISTANCE, 1_240)).isEqualTo(1_000)
    }

    @Test
    fun `an outing copies its plan's goal at the start`() {
        val source = plan(SessionGoalKind.TIME, 25).copy(
            name = "Park",
            milestones = setOf(SessionMilestone.QUARTER, SessionMilestone.GOAL),
        )
        val session = SessionPlans.start(source, 1_000L, 20_000, todaySteps = 3_000, dailyGoalSteps = 8_000)!!
        assertThat(session.planId).isEqualTo(7)
        assertThat(session.name).isEqualTo("Park")
        assertThat(session.goalKind).isEqualTo(SessionGoalKind.TIME)
        assertThat(session.goalValue).isEqualTo(25)
        assertThat(session.milestones).containsExactly(SessionMilestone.QUARTER)
        assertThat(session.startedAtMillis).isEqualTo(1_000L)
        assertThat(session.state).isEqualTo(SessionState.ACTIVE)
    }

    @Test
    fun `the rest of the day becomes the steps missing when it starts`() {
        val rest = SessionPlans.REST_OF_DAY
        val session = SessionPlans.start(rest, 1_000L, 20_000, todaySteps = 5_600, dailyGoalSteps = 8_000)!!
        assertThat(session.goalKind).isEqualTo(SessionGoalKind.STEPS)
        assertThat(session.goalValue).isEqualTo(2_400)
        assertThat(session.restOfDay).isTrue()
        assertThat(session.planId).isNull()
    }

    @Test
    fun `there is no rest of a day whose goal is met`() {
        val rest = SessionPlans.REST_OF_DAY
        assertThat(SessionPlans.start(rest, 1_000L, 20_000, todaySteps = 8_100, dailyGoalSteps = 8_000)).isNull()
        assertThat(SessionPlans.start(rest, 1_000L, 20_000, todaySteps = 7_950, dailyGoalSteps = 8_000)).isNull()
    }

    @Test
    fun `the headline follows the outing`() {
        val base = Session(
            planId = null,
            name = null,
            goalKind = SessionGoalKind.TIME,
            goalValue = 20,
            intensity = SessionIntensity.BRISK,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = 0,
            startedAtMillis = 0,
        )
        fun at(minutes: Double) = base.copy(totals = SessionTotals(movingMillis = (minutes * 60_000).toLong()))
        assertThat(SessionHeadline.of(at(0.5))).isInstanceOf(SessionHeadline.Starting::class.java)
        assertThat(SessionHeadline.of(at(4.0))).isEqualTo(SessionHeadline.Going(SessionAmount(SessionGoalKind.TIME, 16.0)))
        assertThat(SessionHeadline.of(at(10.5)))
            .isEqualTo(SessionHeadline.PastHalf(SessionAmount(SessionGoalKind.TIME, 10.0)))
        assertThat(SessionHeadline.of(at(18.0)))
            .isEqualTo(SessionHeadline.AlmostThere(SessionAmount(SessionGoalKind.TIME, 2.0)))
        assertThat(SessionHeadline.of(at(5.0).copy(state = SessionState.PAUSED))).isEqualTo(SessionHeadline.Paused)
        assertThat(SessionHeadline.of(at(20.0).copy(reachedAtMillis = 1)))
            .isInstanceOf(SessionHeadline.Reached::class.java)
        val ended = at(15.0).copy(state = SessionState.FINISHED, end = SessionEnd.STOPPED)
        assertThat(SessionHeadline.of(ended)).isEqualTo(SessionHeadline.Ended(0.75, SessionEnd.STOPPED))
    }

    @Test
    fun `the zone share is of the moving time`() {
        val base = SessionPlans.start(plan(SessionGoalKind.TIME, 20), 0, 0, 0, 8_000)!!
        assertThat(base.zoneShare()).isNull()
        val walked = base.copy(totals = SessionTotals(movingMillis = 20 * 60_000, zoneMillis = 17 * 60_000))
        assertThat(walked.zoneShare()).isWithin(1e-9).of(0.85)
    }
}
