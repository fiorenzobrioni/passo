package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.WayId
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SessionTrackerTest {
    private val lengths = StepLengths(walkingMeters = 0.7, runningMeters = 0.9)
    private val start = 1_000_000_000L

    private fun session(
        kind: SessionGoalKind = SessionGoalKind.TIME,
        value: Int = 20,
        intensity: SessionIntensity = SessionIntensity.BRISK,
        milestones: Set<SessionMilestone> = setOf(SessionMilestone.HALF),
    ) = Session(
        planId = 1,
        name = null,
        goalKind = kind,
        goalValue = value,
        intensity = intensity,
        milestones = milestones,
        vibrate = true,
        localEpochDay = 20_000,
        startedAtMillis = start,
    )

    private fun tracker(session: Session = session()) = SessionTracker(session, lengths, weightKg = 70.0)

    /** One step every [intervalMillis] from [from] for [millis]; the signals, in order. */
    private fun SessionTracker.walk(from: Long, millis: Long, intervalMillis: Long = 550): List<SessionSignal> {
        val signals = mutableListOf<SessionSignal>()
        var t = from + intervalMillis
        while (t <= from + millis) {
            signals += onSteps(t, 1)
            t += intervalMillis
        }
        return signals
    }

    @Test
    fun `steady walking is all moving time, at the cadence it was walked`() {
        val tracker = tracker()
        tracker.walk(start, 60_000, intervalMillis = 550)
        val totals = tracker.session.totals
        assertThat(totals.steps).isEqualTo(109)
        assertThat(totals.movingMillis).isEqualTo(109 * 550L)
        assertThat(tracker.cadenceAt(start + 60_000)).isIn(105..112)
        // Brisk from the first stated cadence on: only the first seconds, before a pace, are out.
        assertThat(totals.zoneMillis).isAtLeast(totals.movingMillis - 11_000)
    }

    @Test
    fun `standing still adds no time, and one step after it adds a step's worth`() {
        val tracker = tracker()
        tracker.walk(start, 30_000)
        val before = tracker.session.totals.movingMillis
        tracker.onSteps(start + 30_000 + 90_000, 1)
        assertThat(tracker.session.totals.movingMillis - before).isEqualTo(SessionConstants.MAX_MILLIS_PER_STEP)
    }

    @Test
    fun `a batch the hardware merged counts its steps' worth of time, never the gap`() {
        val tracker = tracker()
        // 20 steps delivered as one event after 5 minutes: 30 seconds of walking at most.
        tracker.onSteps(start + 5 * 60_000, 20)
        assertThat(tracker.session.totals.movingMillis).isEqualTo(20 * SessionConstants.MAX_MILLIS_PER_STEP)
        // 1,100 steps over 10 minutes: walked the whole time.
        tracker.onSteps(start + 15 * 60_000, 1_100)
        assertThat(tracker.session.totals.movingMillis)
            .isEqualTo(20 * SessionConstants.MAX_MILLIS_PER_STEP + 10 * 60_000)
    }

    @Test
    fun `a slow stretch is out of the zone of a brisk outing`() {
        val tracker = tracker()
        tracker.walk(start, 120_000, intervalMillis = 900) // about 67 spm
        val totals = tracker.session.totals
        assertThat(totals.zoneMillis).isEqualTo(0)
        assertThat(totals.movingMillis).isGreaterThan(100_000)
    }

    @Test
    fun `a free outing is all in its zone`() {
        val tracker = tracker(session(intensity = SessionIntensity.FREE))
        tracker.walk(start, 60_000, intervalMillis = 900)
        assertThat(tracker.session.totals.zoneMillis).isEqualTo(tracker.session.totals.movingMillis)
    }

    @Test
    fun `running steps are longer`() {
        val walk = tracker(session(intensity = SessionIntensity.FREE)).apply { walk(start, 60_000, 550) }
        val run = tracker(session(intensity = SessionIntensity.FREE)).apply { walk(start, 60_000, 380) }
        val walkPerStep = walk.session.totals.distanceMeters / walk.session.totals.steps
        val runPerStep = run.session.totals.distanceMeters / run.session.totals.steps
        assertThat(walkPerStep).isWithin(0.01).of(0.7)
        assertThat(runPerStep).isGreaterThan(0.85)
    }

    @Test
    fun `milestones are told once, then the goal ends the outing`() {
        val tracker = tracker(session(value = 10, milestones = setOf(SessionMilestone.QUARTER, SessionMilestone.HALF)))
        val signals = tracker.walk(start, 11 * 60_000)
        val told = signals.filterIsInstance<SessionSignal.Milestone>().map { it.milestone }
        assertThat(told).containsExactly(SessionMilestone.QUARTER, SessionMilestone.HALF, SessionMilestone.GOAL)
            .inOrder()
        val finished = signals.filterIsInstance<SessionSignal.Finished>().single()
        assertThat(finished.kept).isTrue()
        assertThat(finished.session.end).isEqualTo(SessionEnd.GOAL)
        assertThat(finished.session.reachedAtMillis).isNotNull()
        assertThat(tracker.session.state).isEqualTo(SessionState.FINISHED)
        // Ten minutes in motion, not a step more counted in the outing.
        assertThat(tracker.session.totals.movingMillis).isAtLeast(10 * 60_000L)
        assertThat(tracker.session.totals.movingMillis).isLessThan(10 * 60_000L + 1_000)
    }

    @Test
    fun `several milestones crossed at once are told as the highest`() {
        val tracker = tracker(
            session(
                kind = SessionGoalKind.STEPS,
                value = 1_000,
                milestones = setOf(SessionMilestone.QUARTER, SessionMilestone.HALF, SessionMilestone.THREE_QUARTERS),
            ),
        )
        val signals = tracker.onSteps(start + 8 * 60_000, 800)
        assertThat(signals).containsExactly(SessionSignal.Milestone(SessionMilestone.THREE_QUARTERS))
        assertThat(tracker.session.toldMilestones)
            .containsExactly(SessionMilestone.QUARTER, SessionMilestone.HALF, SessionMilestone.THREE_QUARTERS)
        assertThat(tracker.onSteps(start + 9 * 60_000, 10)).isEmpty()
    }

    @Test
    fun `a distance goal counts the estimated distance`() {
        val tracker = tracker(session(kind = SessionGoalKind.DISTANCE, value = 700, milestones = emptySet()))
        val signals = tracker.walk(start, 11 * 60_000, intervalMillis = 550)
        assertThat(signals.last()).isInstanceOf(SessionSignal.Finished::class.java)
        assertThat(tracker.session.totals.steps).isEqualTo(1_000)
    }

    @Test
    fun `a pause is neither counted nor held against it`() {
        val tracker = tracker()
        tracker.walk(start, 60_000)
        val before = tracker.session.totals
        assertThat(tracker.pause(start + 60_000)).isTrue()
        assertThat(tracker.onSteps(start + 90_000, 30)).isEmpty()
        assertThat(tracker.session.totals).isEqualTo(before)
        assertThat(tracker.cadenceAt(start + 90_000)).isNull()
        tracker.resume(start + 20 * 60_000)
        // Twenty minutes after the last step, but paused: not over.
        tracker.onSteps(start + 20 * 60_000 + 550, 1)
        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(tracker.session.totals.movingMillis - before.movingMillis).isEqualTo(550)
    }

    @Test
    fun `a long stillness ends the outing at its last step`() {
        val tracker = tracker()
        tracker.walk(start, 60_000)
        val lastStep = tracker.session.lastStepAtMillis
        val signals = tracker.onSteps(lastStep + SessionConstants.IDLE_END_MILLIS + 1, 1)
        val finished = signals.single() as SessionSignal.Finished
        assertThat(finished.session.end).isEqualTo(SessionEnd.IDLE)
        assertThat(finished.session.endedAtMillis).isEqualTo(lastStep)
        assertThat(finished.kept).isTrue()
    }

    @Test
    fun `check ends a forgotten outing without a step`() {
        val tracker = tracker()
        tracker.walk(start, 60_000)
        assertThat(tracker.check(start + 10 * 60_000)).isEmpty()
        val finished = tracker.check(start + 60_000 + SessionConstants.IDLE_END_MILLIS + 1).single()
        assertThat((finished as SessionSignal.Finished).session.end).isEqualTo(SessionEnd.IDLE)
    }

    @Test
    fun `a long pause ends it where it was paused`() {
        val tracker = tracker()
        tracker.walk(start, 60_000)
        tracker.pause(start + 70_000)
        val finished = tracker.check(start + 70_000 + SessionConstants.PAUSE_END_MILLIS + 1).single()
        assertThat((finished as SessionSignal.Finished).session.endedAtMillis).isEqualTo(start + 70_000)
    }

    @Test
    fun `an outing with almost no steps is not kept`() {
        val tracker = tracker()
        tracker.onSteps(start + 5_000, 3)
        val finished = tracker.stop(start + 60_000)
        assertThat(finished?.kept).isFalse()
        assertThat(tracker.stop(start + 70_000)).isNull()
    }

    @Test
    fun `keep going reopens it with the steps taken since the goal`() {
        val tracker = tracker(session(kind = SessionGoalKind.STEPS, value = 100, milestones = emptySet()))
        tracker.walk(start, 100 * 550L)
        assertThat(tracker.session.end).isEqualTo(SessionEnd.GOAL)
        val ended = tracker.session.endedAtMillis!!
        tracker.walk(ended, 50 * 550L)
        assertThat(tracker.session.totals.steps).isEqualTo(100)
        assertThat(tracker.keepGoing(ended + 60_000)).isEmpty()
        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(tracker.session.totals.steps).isEqualTo(150)
        assertThat(tracker.session.reached).isTrue()
        // No second goal, no second finish: it ends when the reader stops it.
        assertThat(tracker.walk(ended + 60_000, 30_000)).isEmpty()
        val stopped = tracker.stop(ended + 120_000)!!
        assertThat(stopped.session.end).isEqualTo(SessionEnd.STOPPED)
        assertThat(stopped.session.reached).isTrue()
    }

    @Test
    fun `keep going is offered only for a while after the goal`() {
        val tracker = tracker(session(kind = SessionGoalKind.STEPS, value = 100, milestones = emptySet()))
        tracker.walk(start, 100 * 550L)
        val ended = tracker.session.endedAtMillis!!
        assertThat(tracker.canKeepGoing(ended + SessionConstants.KEEP_GOING_MILLIS)).isTrue()
        assertThat(tracker.keepGoing(ended + SessionConstants.KEEP_GOING_MILLIS + 1)).isNull()
    }

    @Test
    fun `an outing stopped by the reader cannot be kept going`() {
        val tracker = tracker()
        tracker.walk(start, 60_000)
        tracker.stop(start + 61_000)
        assertThat(tracker.keepGoing(start + 62_000)).isNull()
    }

    @Test
    fun `an end by stillness can be taken back, with the steps since and without the stillness`() {
        val tracker = tracker(session(kind = SessionGoalKind.STEPS, value = 1_000, milestones = emptySet()))
        tracker.walk(start, 100 * 550L)
        val lastStep = tracker.session.lastStepAtMillis
        val before = tracker.session.totals
        // Twenty minutes of chat, then the walk goes on: the first batch notices the end.
        val back = lastStep + 20 * 60_000
        val finished = tracker.onSteps(back, 10).single() as SessionSignal.Finished
        assertThat(finished.session.end).isEqualTo(SessionEnd.IDLE)
        assertThat(finished.session.endedAtMillis).isEqualTo(lastStep)
        assertThat(finished.session.totals).isEqualTo(before)
        // Offered for a while from when it was noticed, not from the last step.
        assertThat(tracker.canKeepGoing(back + SessionConstants.KEEP_GOING_MILLIS)).isTrue()
        tracker.walk(back, 20 * 550L)
        assertThat(tracker.keepGoing(back + 60_000)).isEmpty()
        val reopened = tracker.session
        assertThat(reopened.state).isEqualTo(SessionState.ACTIVE)
        assertThat(reopened.end).isNull()
        assertThat(reopened.endedAtMillis).isNull()
        assertThat(reopened.totals.steps).isEqualTo(before.steps + 10 + 20)
        // The chat is not time in motion: the ten steps that ended it count ten steps' worth.
        assertThat(reopened.totals.movingMillis - before.movingMillis)
            .isEqualTo(10 * SessionConstants.MAX_MILLIS_PER_STEP + 20 * 550L)
        // It counts on as before.
        tracker.onSteps(back + 60_000 + 550, 1)
        assertThat(tracker.session.totals.steps).isEqualTo(before.steps + 31)
        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
    }

    @Test
    fun `an end by stillness found by the screen can be taken back too`() {
        val tracker = tracker()
        tracker.walk(start, 60_000)
        val noticed = start + 60_000 + SessionConstants.IDLE_END_MILLIS + 5 * 60_000
        assertThat(tracker.check(noticed)).hasSize(1)
        assertThat(tracker.canKeepGoing(noticed + SessionConstants.KEEP_GOING_MILLIS)).isTrue()
        assertThat(tracker.keepGoing(noticed + SessionConstants.KEEP_GOING_MILLIS + 1)).isNull()
        assertThat(tracker.session.state).isEqualTo(SessionState.FINISHED)
    }

    @Test
    fun `taking back an end by stillness tells what the steps since crossed`() {
        val tracker =
            tracker(session(kind = SessionGoalKind.STEPS, value = 100, milestones = setOf(SessionMilestone.HALF)))
        tracker.walk(start, 40 * 550L)
        val lastStep = tracker.session.lastStepAtMillis
        val back = lastStep + 20 * 60_000
        tracker.onSteps(back, 20)
        tracker.walk(back, 50 * 550L)
        // 110 steps: past halfway and past the goal, which ends it at once, as a goal.
        val signals = tracker.keepGoing(back + 60_000)!!
        assertThat((signals.first() as SessionSignal.Milestone).milestone).isEqualTo(SessionMilestone.GOAL)
        val finished = signals.last() as SessionSignal.Finished
        assertThat(finished.session.end).isEqualTo(SessionEnd.GOAL)
        assertThat(finished.session.reached).isTrue()
        assertThat(finished.session.totals.steps).isEqualTo(110)
        assertThat(tracker.canKeepGoing(back + 61_000)).isTrue()
    }

    @Test
    fun `a pause left too long and the longest outing are not taken back`() {
        val paused = tracker()
        paused.walk(start, 60_000)
        paused.pause(start + 70_000)
        val at = start + 70_000 + SessionConstants.PAUSE_END_MILLIS + 1
        assertThat(paused.check(at)).hasSize(1)
        assertThat(paused.canKeepGoing(at)).isFalse()

        val long = tracker(session(kind = SessionGoalKind.STEPS, value = 30_000))
        var t = start
        while (t < start + SessionConstants.MAX_SESSION_MILLIS - 60_000) {
            long.onSteps(t + 60_000, 100)
            t += 60_000
        }
        long.onSteps(start + SessionConstants.MAX_SESSION_MILLIS + 1_000, 100)
        assertThat(long.canKeepGoing(start + SessionConstants.MAX_SESSION_MILLIS + 2_000)).isFalse()
    }

    @Test
    fun `no outing stays open past the longest one`() {
        val tracker = tracker(session(kind = SessionGoalKind.STEPS, value = 30_000))
        var t = start
        while (t < start + SessionConstants.MAX_SESSION_MILLIS - 60_000) {
            tracker.onSteps(t + 60_000, 100)
            t += 60_000
        }
        assertThat(tracker.session.live).isTrue()
        val signals = tracker.onSteps(start + SessionConstants.MAX_SESSION_MILLIS + 1_000, 100)
        assertThat((signals.single() as SessionSignal.Finished).session.end).isEqualTo(SessionEnd.CLOSED)
    }

    private fun walkTracker(from: Int = 0): SessionTracker {
        val walk = Ways.of(WayId.MILAN_DUOMO_NAVIGLI)
        val session = checkNotNull(SessionPlans.startWalk(walk, from, SessionVoice.OFF, start, 20_000))
        return SessionTracker(session, lengths, weightKg = 70.0)
    }

    @Test
    fun `a walk tells its places as the distance reaches them, never the one it starts at`() {
        val tracker = walkTracker()
        // 0.7 m a step at a walking cadence: 600 steps are 420 m, past the Galleria (about 210 m).
        val signals = tracker.walk(start, 600 * 550L)
        val places = signals.filterIsInstance<SessionSignal.Places>().flatMap { it.places }.map { it.key }
        assertThat(places).contains("milan_galleria")
        assertThat(places).doesNotContain("milan_duomo")
        assertThat(places).containsNoDuplicates()
        assertThat(signals.filterIsInstance<SessionSignal.Milestone>()).isEmpty()
    }

    @Test
    fun `a walk continued from the middle goes on from there, and its end is the goal`() {
        val walk = Ways.of(WayId.MILAN_DUOMO_NAVIGLI)
        val from = walk.stops.single { it.key == "milan_darsena" }.distanceMeters
        val tracker = walkTracker(from)
        assertThat(tracker.session.goalValue).isEqualTo(walk.lengthMeters - from)
        val signals = tracker.walk(start, 2_000 * 550L)
        val places = signals.filterIsInstance<SessionSignal.Places>().flatMap { it.places }.map { it.key }
        assertThat(places).containsExactly("milan_naviglio_grande")
        assertThat(signals.filterIsInstance<SessionSignal.Milestone>().map { it.milestone })
            .containsExactly(SessionMilestone.GOAL)
        assertThat(signals.filterIsInstance<SessionSignal.Finished>().single().session.end).isEqualTo(SessionEnd.GOAL)
    }

    @Test
    fun `a walk with nothing left does not start`() {
        val walk = Ways.of(WayId.LONDON_PALACE_TOWER)
        assertThat(SessionPlans.startWalk(walk, walk.lengthMeters - 10, SessionVoice.OFF, start, 20_000)).isNull()
    }
}
