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
    fun `the editor steps by half kilometres, or quarter miles for miles`() {
        assertThat(SessionPlans.nudge(SessionGoalKind.DISTANCE, 1_500, up = true, imperial = false)).isEqualTo(2_000)
        assertThat(SessionPlans.nudge(SessionGoalKind.DISTANCE, 500, up = false, imperial = false)).isEqualTo(500)
        // One mile, then a mile and a quarter.
        val mile = SessionPlans.snapForEditor(SessionGoalKind.DISTANCE, 1_609.0, imperial = true)
        assertThat(mile).isEqualTo(1_609)
        assertThat(SessionPlans.nudge(SessionGoalKind.DISTANCE, mile, up = true, imperial = true)).isEqualTo(2_012)
        assertThat(SessionPlans.nudge(SessionGoalKind.TIME, 20, up = true, imperial = false)).isEqualTo(25)
        assertThat(SessionPlans.nudge(SessionGoalKind.STEPS, 30_000, up = true, imperial = false)).isEqualTo(30_000)
    }

    @Test
    fun `an outing starts with its plan's value as set, only kept in range`() {
        val quarterMiles = plan(SessionGoalKind.DISTANCE, 2_012)
        assertThat(SessionPlans.start(quarterMiles, 0, 0, 0, 8_000)!!.goalValue).isEqualTo(2_012)
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
        assertThat(
            SessionHeadline.of(at(4.0)),
        ).isEqualTo(SessionHeadline.Going(SessionAmount(SessionGoalKind.TIME, 16.0)))
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
    fun `the open walk is the first preset, with no goal, at any pace`() {
        val first = SessionPlans.PRESETS.first()
        assertThat(first).isEqualTo(SessionPlans.OPEN_WALK)
        assertThat(first.open).isTrue()
        assertThat(first.intensity).isEqualTo(SessionIntensity.FREE)
        assertThat(first.gps).isFalse()
        assertThat(SessionPlans.PRESETS.map { it.position }).containsExactly(0, 1, 2, 3, 4).inOrder()
    }

    @Test
    fun `the rest of the day is found by its kind, wherever the presets put it`() {
        assertThat(SessionPlans.REST_OF_DAY.goalKind).isEqualTo(SessionGoalKind.REST_OF_DAY)
    }

    @Test
    fun `an open plan adds up to nothing before it is walked`() {
        val estimate = SessionPlans.estimate(SessionPlans.OPEN_WALK, lengths, 4_000)
        assertThat(estimate).isEqualTo(SessionEstimate(steps = 0, distanceMeters = 0.0, minutes = 0))
    }

    @Test
    fun `a plan switched to none and back offers a half hour at its pace in the new quantity`() {
        val brisk = plan(SessionGoalKind.TIME, 20)
        assertThat(SessionPlans.convert(brisk, SessionGoalKind.OPEN, lengths, 0)).isEqualTo(0)
        val open = brisk.copy(goalKind = SessionGoalKind.OPEN, goalValue = 0)
        assertThat(SessionPlans.convert(open, SessionGoalKind.TIME, lengths, 0)).isEqualTo(30)
        assertThat(SessionPlans.convert(open, SessionGoalKind.STEPS, lengths, 0)).isEqualTo(3_000)
        assertThat(SessionPlans.range(SessionGoalKind.OPEN)).isEqualTo(0..0)
        assertThat(SessionPlans.clampValue(SessionGoalKind.OPEN, 40)).isEqualTo(0)
        assertThat(SessionPlans.snapForEditor(SessionGoalKind.OPEN, 40.0, imperial = false)).isEqualTo(0)
    }

    @Test
    fun `an open plan starts with no goal and no milestone, and twelve hours to its cap`() {
        val plan = SessionPlans.OPEN_WALK.copy(id = 9, milestones = setOf(SessionMilestone.HALF), gps = true)
        val session = checkNotNull(SessionPlans.start(plan, 5_000, 20_000, 0, 8_000, withGps = true))
        assertThat(session.goalKind).isEqualTo(SessionGoalKind.OPEN)
        assertThat(session.goalValue).isEqualTo(0)
        assertThat(session.milestones).isEmpty()
        assertThat(session.planId).isEqualTo(9)
        assertThat(session.gps).isTrue()
        assertThat(session.capAtMillis).isEqualTo(5_000 + SessionConstants.OPEN_MAX_SESSION_MILLIS)
        // Every other outing keeps four hours.
        val timed = checkNotNull(SessionPlans.start(plan(SessionGoalKind.TIME, 20), 5_000, 20_000, 0, 8_000))
        assertThat(timed.capAtMillis).isEqualTo(5_000 + SessionConstants.MAX_SESSION_MILLIS)
    }

    @Test
    fun `the zone share is of the moving time`() {
        val base = SessionPlans.start(plan(SessionGoalKind.TIME, 20), 0, 0, 0, 8_000)!!
        assertThat(base.zoneShare()).isNull()
        val walked = base.copy(totals = SessionTotals(movingMillis = 20 * 60_000, zoneMillis = 17 * 60_000))
        assertThat(walked.zoneShare()).isWithin(1e-9).of(0.85)
    }

    @Test
    fun `every outing started now keeps its pauses, so its minutes can be drawn with their gaps`() {
        val started = SessionPlans.PRESETS.mapNotNull { plan ->
            SessionPlans.start(plan, nowMillis = 0, localEpochDay = 0, todaySteps = 0, dailyGoalSteps = 8_000)
        }
        assertThat(started).hasSize(SessionPlans.PRESETS.size)
        assertThat(started.all { it.pausesKept }).isTrue()
        val walk = com.callbackdev.passo.core.domain.ways.Ways.walks.first()
        assertThat(SessionPlans.startWalk(walk, 0, com.callbackdev.passo.core.model.SessionVoice.OFF, 0, 0)?.pausesKept)
            .isTrue()
    }
}
