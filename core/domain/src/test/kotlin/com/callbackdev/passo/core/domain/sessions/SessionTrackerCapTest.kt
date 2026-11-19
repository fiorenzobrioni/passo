package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.routes.capQuestionMinutes
import com.callbackdev.passo.core.domain.sessions.SessionConstants.CAP_EXTENSION_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.CAP_WARNING_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.KEEP_GOING_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MAX_SESSION_MILLIS
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * The longest outing asks before it ends, and the reader may go on (PLANNING.md §11 Phase 15;
 * docs/adr/0016-location.md, decision 14): for every outing, with GPS or not.
 */
class SessionTrackerCapTest {
    private val lengths = StepLengths(walkingMeters = 0.7, runningMeters = 0.9)
    private val start = 1_000_000_000L
    private val cap = start + MAX_SESSION_MILLIS

    /** A day's walk: a marathon's distance, which no eight hours of this reach, so only the cap can end it. */
    private val plan = SessionPlan(
        id = 1,
        goalKind = SessionGoalKind.DISTANCE,
        goalValue = 42_200,
        intensity = SessionIntensity.FREE,
        milestones = emptySet(),
    )

    private fun started(): Session = checkNotNull(SessionPlans.start(plan, start, 20_000, 0, 8_000))

    private fun tracker(session: Session = started()) = SessionTracker(session, lengths, weightKg = 70.0)

    /** A hundred steps a minute from [from] to [to]; every signal on the way. */
    private fun SessionTracker.walk(from: Long, to: Long): List<SessionSignal> {
        val signals = mutableListOf<SessionSignal>()
        var at = from
        while (at + MINUTE <= to) {
            at += MINUTE
            signals += onSteps(at, 100)
        }
        return signals
    }

    @Test
    fun `an outing starts with its cap four hours on`() {
        assertThat(started().capAtMillis).isEqualTo(cap)
        assertThat(started().capMillis).isEqualTo(cap)
    }

    @Test
    fun `an outing from before the cap was kept reads as four hours from its start`() {
        assertThat(started().copy(capAtMillis = null).capMillis).isEqualTo(cap)
    }

    @Test
    fun `three hours forty-five into an outing, walking, the reader is asked once and nothing stops`() {
        val tracker = tracker()

        val before = tracker.walk(start, cap - CAP_WARNING_MILLIS - MINUTE)
        val asked = tracker.walk(cap - CAP_WARNING_MILLIS - MINUTE, cap - MINUTE)

        assertThat(before.filterIsInstance<SessionSignal.CapAhead>()).isEmpty()
        assertThat(asked.filterIsInstance<SessionSignal.CapAhead>()).containsExactly(SessionSignal.CapAhead(cap))
        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(tracker.session.capAskedForMillis).isEqualTo(cap)
    }

    @Test
    fun `keep going before the cap moves it four hours on, and asks again before that one`() {
        val tracker = tracker()
        tracker.walk(start, cap - 10 * MINUTE)

        assertThat(tracker.goOn(cap - 10 * MINUTE)).isTrue()

        val next = cap + CAP_EXTENSION_MILLIS
        assertThat(tracker.session.capAtMillis).isEqualTo(next)
        // Past the old cap, nothing ends.
        val onward = tracker.walk(cap - 10 * MINUTE, next - CAP_WARNING_MILLIS - MINUTE)
        assertThat(onward).isEmpty()
        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
        // And fifteen minutes before the new one, the question again.
        val again = tracker.walk(next - CAP_WARNING_MILLIS - MINUTE, next - MINUTE)
        assertThat(again).containsExactly(SessionSignal.CapAhead(next))
    }

    @Test
    fun `asked late, after a sit at a café, the question says the minutes really left`() {
        val tracker = tracker()
        tracker.walk(start, cap - 16 * MINUTE)

        // Thirteen minutes at a table, under the end by stillness; then the walk again.
        val asked = tracker.onSteps(cap - 3 * MINUTE, 100)

        assertThat(asked).containsExactly(SessionSignal.CapAhead(cap))
        assertThat(SessionAnnouncement.capAhead(tracker.session, cap - 3 * MINUTE))
            .isEqualTo(SessionAnnouncement.CapAhead(minutes = 3))
    }

    @Test
    fun `paused across the cap, the reader who resumes is asked before it ends, not ended unasked`() {
        val tracker = tracker()
        tracker.walk(start, cap - 20 * MINUTE)
        tracker.pause(cap - 20 * MINUTE)

        // Forty minutes at lunch, then on.
        assertThat(tracker.resume(cap + 20 * MINUTE)).isTrue()
        val moved = cap + 40 * MINUTE
        assertThat(tracker.session.capAtMillis).isEqualTo(moved)

        val onward = tracker.walk(cap + 20 * MINUTE, moved - CAP_WARNING_MILLIS - MINUTE)
        assertThat(onward).isEmpty()
        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
        val asked = tracker.walk(moved - CAP_WARNING_MILLIS - MINUTE, moved - MINUTE)
        assertThat(asked).containsExactly(SessionSignal.CapAhead(moved))
    }

    @Test
    fun `a question open before a pause stays open after it, and is not asked twice`() {
        val tracker = tracker()
        tracker.walk(start, cap - 10 * MINUTE)
        assertThat(tracker.session.capAskedForMillis).isEqualTo(cap)

        tracker.pause(cap - 10 * MINUTE)
        tracker.resume(cap - 5 * MINUTE)
        val moved = cap + 5 * MINUTE

        assertThat(tracker.session.capAskedForMillis).isEqualTo(moved)
        assertThat(tracker.walk(cap - 5 * MINUTE, moved - MINUTE)).isEmpty()
        assertThat(tracker.goOn(moved - MINUTE)).isTrue()
    }

    @Test
    fun `after an hour's pause the open question says the minutes left before it, not more`() {
        val tracker = tracker()
        tracker.walk(start, cap - 10 * MINUTE)
        tracker.pause(cap - 10 * MINUTE)

        // An hour at lunch: the cap moves on by the hour, and ten minutes are still left.
        tracker.resume(cap + 50 * MINUTE)

        assertThat(tracker.session.capQuestionMinutes(cap + 50 * MINUTE)).isEqualTo(10)
    }

    @Test
    fun `a batch handed over after the cap still asks, at one minute`() {
        val tracker = tracker()
        tracker.walk(start, cap - 16 * MINUTE)
        tracker.onSteps(cap - MINUTE / 6, 20)

        assertThat(SessionAnnouncement.capAhead(tracker.session, cap + MINUTE / 3))
            .isEqualTo(SessionAnnouncement.CapAhead(minutes = 1))
    }

    @Test
    fun `there is no cap to move before the question is asked`() {
        val tracker = tracker()
        tracker.walk(start, start + 60 * MINUTE)

        assertThat(tracker.goOn(start + 60 * MINUTE)).isFalse()
        assertThat(tracker.session.capAtMillis).isEqualTo(cap)
    }

    @Test
    fun `unanswered, it ends at four hours and is told, and keep going takes it back with the steps since`() {
        val tracker = tracker()
        tracker.walk(start, cap)
        val steps = tracker.session.totals.steps

        val ended = tracker.onSteps(cap + MINUTE, 100).single() as SessionSignal.Finished

        assertThat(ended.session.end).isEqualTo(SessionEnd.CLOSED)
        assertThat(ended.kept).isTrue()
        // Told, and reopenable: it was silent, and for good.
        assertThat(tracker.canKeepGoing(cap + MINUTE)).isTrue()

        // The reader walks on for ten minutes, then says so.
        tracker.walk(cap + MINUTE, cap + 10 * MINUTE)
        assertThat(tracker.keepGoing(cap + 10 * MINUTE)).isNotNull()

        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(tracker.session.totals.steps).isEqualTo(steps + 100 + 900)
        assertThat(tracker.session.capAtMillis).isEqualTo(cap + CAP_EXTENSION_MILLIS)
    }

    @Test
    fun `not taken back within fifteen minutes, it is over for good`() {
        val tracker = tracker()
        tracker.walk(start, cap)
        tracker.onSteps(cap + MINUTE, 100)

        val late = cap + MINUTE + KEEP_GOING_MILLIS + 1

        assertThat(tracker.canKeepGoing(late)).isFalse()
        assertThat(tracker.keepGoing(late)).isNull()
    }

    @Test
    fun `noticed by a look at the phone rather than a step, the end at the cap can be taken back too`() {
        val tracker = tracker()
        tracker.walk(start, cap - MINUTE)

        val ended = tracker.check(cap + 1).single() as SessionSignal.Finished

        assertThat(ended.session.end).isEqualTo(SessionEnd.CLOSED)
        assertThat(tracker.canKeepGoing(cap + 2)).isTrue()
    }

    @Test
    fun `sitting since three thirty-five, the reader is not asked, and the stillness ends it as always`() {
        val tracker = tracker()
        val satDown = cap - 25 * MINUTE
        val walked = tracker.walk(start, satDown)

        // No step, no question: nothing arrives to carry it.
        assertThat(walked.filterIsInstance<SessionSignal.CapAhead>()).isEmpty()
        assertThat(tracker.check(satDown + 14 * MINUTE)).isEmpty()

        val ended = tracker.check(satDown + 15 * MINUTE + 1).single() as SessionSignal.Finished

        assertThat(ended.session.end).isEqualTo(SessionEnd.IDLE)
        assertThat(ended.session.endedAtMillis).isEqualTo(satDown)
    }

    @Test
    fun `a process restarted after the question does not ask it again`() {
        val first = tracker()
        first.walk(start, cap - 10 * MINUTE)
        assertThat(first.session.capAskedForMillis).isEqualTo(cap)

        // The outing as it was stored, in a new process.
        val second = tracker(first.session)

        assertThat(second.walk(cap - 10 * MINUTE, cap - 5 * MINUTE)).isEmpty()
        // And the answer still counts.
        assertThat(second.goOn(cap - 5 * MINUTE)).isTrue()
    }

    @Test
    fun `a process restarted after keep going reads the moved cap from what was stored`() {
        val first = tracker()
        first.walk(start, cap - 10 * MINUTE)
        first.goOn(cap - 10 * MINUTE)

        val second = tracker(first.session)

        assertThat(second.walk(cap - 10 * MINUTE, cap + 30 * MINUTE)).isEmpty()
        assertThat(second.session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(second.session.capMillis).isEqualTo(cap + CAP_EXTENSION_MILLIS)
    }

    @Test
    fun `an end by the goal or by stillness taken back keeps its own cap`() {
        val goal = SessionTracker(
            checkNotNull(
                SessionPlans.start(
                    plan.copy(goalKind = SessionGoalKind.STEPS, goalValue = 500),
                    start,
                    20_000,
                    0,
                    8_000,
                ),
            ),
            lengths,
            weightKg = 70.0,
        )
        goal.walk(start, start + 5 * MINUTE)
        assertThat(goal.session.end).isEqualTo(SessionEnd.GOAL)

        goal.keepGoing(start + 6 * MINUTE)

        assertThat(goal.session.capAtMillis).isEqualTo(cap)
    }

    @Test
    fun `the processor is let go at the outing's own cap, wherever keep going moved it`() {
        val walking = started().copy(lastStepAtMillis = cap - MINUTE)
        val moved = walking.copy(capAtMillis = cap + CAP_EXTENSION_MILLIS)

        assertThat(SignalWake.letGoAt(walking)).isEqualTo(cap)
        // Still walking past the old cap: let go by stillness, not by a cap that is no longer its.
        assertThat(SignalWake.letGoAt(moved)).isEqualTo(cap - MINUTE + SessionConstants.IDLE_END_MILLIS)
    }

    @Test
    fun `the lock's own bound is ten minutes past the cap, and follows it`() {
        val session = started()
        val moved = session.copy(capAtMillis = cap + CAP_EXTENSION_MILLIS)

        assertThat(SignalWake.timeoutMillis(session, start)).isEqualTo(SignalWake.TIMEOUT_MILLIS)
        assertThat(SignalWake.timeoutMillis(moved, cap - 10 * MINUTE))
            .isEqualTo(CAP_EXTENSION_MILLIS + 20 * MINUTE)
        // Never nothing: past the cap it still leaves room for the end to be noticed.
        assertThat(SignalWake.timeoutMillis(session, cap + 60 * MINUTE)).isEqualTo(SignalWake.TIMEOUT_MARGIN_MILLIS)
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
