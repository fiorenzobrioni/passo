package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.WayId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * A goal outing past its goal (PLANNING.md §11 Phase 21, step 21.3, «Past its goal»): its shape in
 * the bar's place, and its sentence its goal's with what went beyond it.
 */
class PastGoalTest {
    private val start = 1_000_000_000L

    private fun outing(
        kind: SessionGoalKind = SessionGoalKind.TIME,
        goal: Int = 30,
        totals: SessionTotals = SessionTotals(movingMillis = 50 * MINUTE),
        state: SessionState = SessionState.ACTIVE,
        end: SessionEnd? = null,
        reached: Boolean = true,
        walk: WayId? = null,
    ) = Session(
        planId = 1,
        name = null,
        goalKind = kind,
        goalValue = goal,
        intensity = SessionIntensity.BRISK,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = 20_000,
        startedAtMillis = start,
        state = state,
        end = end,
        endedAtMillis = if (state == SessionState.FINISHED) start + 60 * MINUTE else null,
        totals = totals,
        reachedAtMillis = if (reached) start + 30 * MINUTE else null,
        walk = walk,
    )

    @Test
    fun `a goal outing past its goal, 30 minutes and 20 more, says its goal and the 20 beyond`() {
        val session = outing()
        assertThat(session.pastGoal).isTrue()
        assertThat(session.beyond()).isEqualTo(SessionAmount(SessionGoalKind.TIME, 20.0))
        assertThat(SessionHeadline.of(session))
            .isEqualTo(
                SessionHeadline.Reached(
                    SessionAmount(SessionGoalKind.TIME, 30.0),
                    SessionAmount(SessionGoalKind.TIME, 20.0),
                ),
            )
    }

    @Test
    fun `a goal of steps says the steps beyond it, one of distance its distance`() {
        val steps = outing(kind = SessionGoalKind.STEPS, goal = 3_000, totals = SessionTotals(steps = 4_240))
        assertThat(steps.beyond()).isEqualTo(SessionAmount(SessionGoalKind.STEPS, 1_240.0))
        val distance =
            outing(kind = SessionGoalKind.DISTANCE, goal = 2_000, totals = SessionTotals(distanceMeters = 2_800.0))
        assertThat(distance.beyond()).isEqualTo(SessionAmount(SessionGoalKind.DISTANCE, 800.0))
    }

    @Test
    fun `under a whole unit beyond, nothing is said of it`() {
        assertThat(outing(totals = SessionTotals(movingMillis = 30 * MINUTE + 59_000)).beyond()).isNull()
        val near =
            outing(kind = SessionGoalKind.DISTANCE, goal = 2_000, totals = SessionTotals(distanceMeters = 2_009.0))
        assertThat(near.beyond()).isNull()
    }

    @Test
    fun `an outing that ended at its goal says no steps beyond, though its last batch ran past`() {
        val ended = outing(
            kind = SessionGoalKind.STEPS,
            goal = 3_000,
            totals = SessionTotals(steps = 3_012),
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
        )
        assertThat(ended.pastGoal).isFalse()
        assertThat(ended.beyond()).isNull()
    }

    @Test
    fun `stopped after it was taken on, it still says what went beyond, but is past its goal no more`() {
        val stopped = outing(state = SessionState.FINISHED, end = SessionEnd.STOPPED)
        assertThat(stopped.pastGoal).isFalse()
        assertThat(stopped.beyond()).isEqualTo(SessionAmount(SessionGoalKind.TIME, 20.0))
    }

    @Test
    fun `before its goal, an open outing and a city walk are never past a goal`() {
        assertThat(outing(reached = false).pastGoal).isFalse()
        assertThat(outing(reached = false).beyond()).isNull()
        assertThat(outing(kind = SessionGoalKind.OPEN, goal = 0).pastGoal).isFalse()
        assertThat(outing(kind = SessionGoalKind.OPEN, goal = 0).beyond()).isNull()
        assertThat(
            outing(walk = WayId.entries.first(), kind = SessionGoalKind.DISTANCE, goal = 2_000).pastGoal,
        ).isFalse()
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
