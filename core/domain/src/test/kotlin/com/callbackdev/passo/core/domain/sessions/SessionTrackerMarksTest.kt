package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.routes.Fix
import com.callbackdev.passo.core.domain.routes.RouteMeter
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionMarks
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * An open outing's marks on the way (PLANNING.md §11 Phase 21, step 21.2; docs/adr/0009-sessions.md,
 * decision 3): a kilometre, or ten minutes in motion, each told once, on time, and never after the
 * end. Each test is a row of the step's edge cases, or what the step says of the marks.
 */
class SessionTrackerMarksTest {
    private val lengths = StepLengths(walkingMeters = 0.7, runningMeters = 0.9)
    private val start = 1_000_000_000_000L

    private fun plan(gps: Boolean = false, marks: SessionMarks? = null): SessionPlan =
        SessionPlans.OPEN_WALK.copy(id = 1, gps = gps, marks = marks)

    private fun open(plan: SessionPlan = plan(), withGps: Boolean = false, miles: Boolean = false): Session =
        checkNotNull(SessionPlans.start(plan, start, 20_000, 0, 8_000, withGps = withGps, miles = miles))

    private fun tracker(session: Session = open()) = SessionTracker(session, lengths, weightKg = 70.0)

    /**
     * A walk at 110 steps a minute, handed over every half minute as a wake-up counter does, from
     * [from] for [millis]; the signals, in order. 77 m a minute at the walking step.
     */
    private fun SessionTracker.walk(from: Long, millis: Long): List<SessionSignal> {
        val signals = mutableListOf<SessionSignal>()
        var t = from + BATCH
        while (t <= from + millis) {
            signals += onSteps(t, STEPS_A_BATCH)
            t += BATCH
        }
        return signals
    }

    private fun List<SessionSignal>.marks() = filterIsInstance<SessionSignal.Mark>()

    /** A reader with GPS, a second at a time: 110 steps a minute handed over every ten seconds, a fix a second. */
    private inner class Walker(val tracker: SessionTracker) {
        var at = start
        var north = 0.0
        val signals = mutableListOf<SessionSignal>()
        private var owed = 0.0

        fun go(seconds: Int, speed: Double = 1.4, fixes: Boolean = true, settle: Boolean = true) {
            repeat(seconds) {
                at += 1_000
                north += speed
                if (fixes) tracker.onFix(Fix(at, LATITUDE + north / METERS_PER_DEGREE, LONGITUDE, accuracyMeters = 5f))
                owed += CADENCE / 60.0
                if ((at - start) % 10_000 == 0L) {
                    val count = owed.toInt()
                    owed -= count
                    signals += tracker.onSteps(at, count)
                    if (settle) signals += tracker.settle(at, at)
                }
            }
        }
    }

    private fun walker(session: Session) = Walker(SessionTracker(session, lengths, weightKg = 70.0, RouteMeter()))

    @Test
    fun `a plan that has not chosen tells kilometres with GPS and ten minutes without`() {
        assertThat(plan(gps = true).marksInForce).isEqualTo(SessionMarks.DISTANCE)
        assertThat(plan(gps = false).marksInForce).isEqualTo(SessionMarks.TIME)
        assertThat(open(plan(gps = true), withGps = true).marks).isEqualTo(SessionMarks.DISTANCE)
        assertThat(open(plan(gps = false)).marks).isEqualTo(SessionMarks.TIME)
    }

    @Test
    fun `the plan's GPS switch changed, no choice made - the mark follows the switch`() {
        val chosenNothing = plan(gps = true)
        assertThat(chosenNothing.copy(gps = false).marksInForce).isEqualTo(SessionMarks.TIME)
        assertThat(chosenNothing.copy(gps = false).copy(gps = true).marksInForce).isEqualTo(SessionMarks.DISTANCE)
    }

    @Test
    fun `the plan's GPS switch changed after a choice - the reader's choice stays`() {
        assertThat(plan(gps = true, marks = SessionMarks.TIME).copy(gps = false).marksInForce)
            .isEqualTo(SessionMarks.TIME)
        assertThat(plan(gps = false, marks = SessionMarks.DISTANCE).copy(gps = true).marksInForce)
            .isEqualTo(SessionMarks.DISTANCE)
        assertThat(plan(gps = true, marks = SessionMarks.NONE).marksInForce).isEqualTo(SessionMarks.NONE)
    }

    @Test
    fun `a goal outing has no marks, whatever its plan says`() {
        val goal = SessionPlans.NEW.copy(marks = SessionMarks.TIME)
        val session = checkNotNull(SessionPlans.start(goal, start, 20_000, 0, 8_000))
        assertThat(session.marks).isEqualTo(SessionMarks.NONE)
        assertThat(tracker(session).walk(start, 25 * MINUTE).marks()).isEmpty()
    }

    @Test
    fun `without GPS, a mark every ten minutes in motion, each told once and in order`() {
        val tracker = tracker()
        val signals = tracker.walk(start, 35 * MINUTE)
        assertThat(signals).containsExactly(
            SessionSignal.Mark(1),
            SessionSignal.Mark(2),
            SessionSignal.Mark(3),
        ).inOrder()
        assertThat(tracker.session.toldMarks).isEqualTo(3)
    }

    @Test
    fun `ten minutes with a pause in them - minutes in motion, the pause adds nothing`() {
        val tracker = tracker()
        assertThat(tracker.walk(start, 6 * MINUTE)).isEmpty()
        tracker.pause(start + 6 * MINUTE)
        tracker.resume(start + 16 * MINUTE)
        // Sixteen minutes on the clock, nine in motion: not yet.
        assertThat(tracker.walk(start + 16 * MINUTE, 3 * MINUTE).marks()).isEmpty()
        // A traffic light: two minutes still add a step's worth, not two minutes.
        assertThat(tracker.onSteps(start + 21 * MINUTE, 2)).isEmpty()
        assertThat(tracker.walk(start + 21 * MINUTE, 90_000L).marks()).containsExactly(SessionSignal.Mark(1))
    }

    @Test
    fun `a batch crossing two kilometres is told once, as the second`() {
        val tracker = tracker(open(plan(marks = SessionMarks.DISTANCE)))
        // 962 m, then a sensor with no FIFO hands over thirteen minutes and a half at once: 1,500
        // steps, 1,050 m more, across the first kilometre and the second.
        assertThat(tracker.walk(start, 25 * BATCH)).isEmpty()
        val signals = tracker.onSteps(start + 25 * BATCH + 27 * BATCH, 1_500)
        assertThat(signals).containsExactly(SessionSignal.Mark(2))
        assertThat(tracker.session.toldMarks).isEqualTo(2)
        // And the next, when it comes, is the third.
        val more = tracker.onSteps(start + 25 * BATCH + 47 * BATCH, 1_500)
        assertThat(more).containsExactly(SessionSignal.Mark(3))
    }

    @Test
    fun `with GPS, a kilometre is told on the measured distance`() {
        val walker = walker(open(plan(gps = true), withGps = true))
        // 700 s at 1.4 m/s: 980 m on the ground, though the steps say 898.
        walker.go(700)
        assertThat(walker.signals.marks()).isEmpty()
        walker.go(40)
        assertThat(walker.signals.marks()).containsExactly(SessionSignal.Mark(1))
    }

    @Test
    fun `a kilometre whose fixes are not in yet is told when its metres are settled`() {
        val walker = walker(open(plan(gps = true), withGps = true))
        // Thirteen minutes of steps in hand, a kilometre and more by their estimate, no fix delivered.
        walker.go(800, fixes = false, settle = false)
        assertThat(walker.signals.marks()).isEmpty()
        // Settled (here with no fix at all: the steps', for good), it is told.
        walker.signals += walker.tracker.settle(walker.at, walker.at)
        assertThat(walker.signals.marks()).containsExactly(SessionSignal.Mark(1))
    }

    @Test
    fun `a plan with GPS started on the steps tells kilometres, estimated - about, and no pace`() {
        val session = open(plan(gps = true), withGps = false)
        assertThat(session.gps).isFalse()
        assertThat(session.marks).isEqualTo(SessionMarks.DISTANCE)
        val tracker = tracker(session)
        // 77 m a minute: the first kilometre at the thirteenth minute's last batch.
        val signals = tracker.walk(start, 13 * MINUTE)
        assertThat(signals).containsExactly(SessionSignal.Mark(1))
        val said = SessionAnnouncement.mark(tracker.session, 1) as SessionAnnouncement.DistanceMark
        assertThat(said.measured).isFalse()
        assertThat(said.paceSecondsPerKm).isNull()
        assertThat(said.movingMinutes).isEqualTo(13)
    }

    @Test
    fun `measured, a kilometre says the pace of that kilometre, from its legs`() {
        val walker = walker(open(plan(gps = true), withGps = true))
        walker.go(400, speed = 1.0)
        walker.go(400, speed = 2.0)
        val count = walker.signals.marks().last().count
        assertThat(count).isEqualTo(1)
        val said = SessionAnnouncement.mark(walker.tracker.session, count) as SessionAnnouncement.DistanceMark
        assertThat(said.measured).isTrue()
        // 400 m at 1 m/s, then 600 m at 2: 700 s for the kilometre, not the average of the walk so far.
        assertThat(checkNotNull(said.paceSecondsPerKm)).isWithin(15.0).of(700.0)
        assertThat(said.movingMinutes).isEqualTo(12)
    }

    @Test
    fun `the distance measured under nine tenths - no pace said`() {
        val walker = walker(open(plan(gps = true), withGps = true))
        // A tunnel for half the walk: its metres are the steps'.
        walker.go(400, fixes = false)
        walker.go(500)
        val count = walker.signals.marks().last().count
        val said = SessionAnnouncement.mark(walker.tracker.session, count) as SessionAnnouncement.DistanceMark
        assertThat(walker.tracker.session.measuredShare).isLessThan(0.9)
        assertThat(said.measured).isFalse()
        assertThat(said.paceSecondsPerKm).isNull()
    }

    @Test
    fun `the units changed during the walk - the unit frozen at the start`() {
        val tracker = tracker(open(plan(marks = SessionMarks.DISTANCE), miles = true))
        assertThat(tracker.session.markMiles).isTrue()
        // 1.54 km: past a kilometre, short of a mile.
        assertThat(tracker.walk(start, 20 * MINUTE)).isEmpty()
        assertThat(tracker.walk(start + 20 * MINUTE, 2 * MINUTE)).containsExactly(SessionSignal.Mark(1))
        assertThat(tracker.session.totals.distanceMeters).isAtLeast(UnitConversions.METERS_PER_MILE)
        val said = SessionAnnouncement.mark(tracker.session, 1) as SessionAnnouncement.DistanceMark
        assertThat(said.miles).isTrue()
    }

    @Test
    fun `marks set to none - no signal for them, nothing to say`() {
        val tracker = tracker(open(plan(marks = SessionMarks.NONE)))
        assertThat(tracker.walk(start, 45 * MINUTE)).isEmpty()
        assertThat(tracker.session.toldMarks).isEqualTo(0)
        assertThat(SessionAnnouncement.mark(tracker.session, 1)).isNull()
    }

    @Test
    fun `ended, while it can still be taken back - no mark`() {
        val tracker = tracker()
        assertThat(tracker.walk(start, 9 * MINUTE)).isEmpty()
        // A long stop, then the walk on: ended at the last step, the steps after it kept aside.
        val again = start + 9 * MINUTE + SessionConstants.IDLE_END_MILLIS + MINUTE
        val ended = tracker.onSteps(again, STEPS_A_BATCH)
        assertThat(ended.filterIsInstance<SessionSignal.Finished>().single().session.end).isEqualTo(SessionEnd.IDLE)
        assertThat(tracker.walk(again, 5 * MINUTE)).isEmpty()
        assertThat(tracker.session.toldMarks).isEqualTo(0)
    }

    @Test
    fun `taken back with Resume - marks go on from the next, none told twice`() {
        val tracker = tracker()
        tracker.walk(start, 9 * MINUTE)
        val again = start + 9 * MINUTE + SessionConstants.IDLE_END_MILLIS + MINUTE
        tracker.onSteps(again, STEPS_A_BATCH)
        tracker.walk(again, 5 * MINUTE)
        // The steps aside take it past ten minutes: passed in silence at the touch of «Resume».
        val taken = checkNotNull(tracker.keepGoing(again + 5 * MINUTE))
        assertThat(taken.marks()).isEmpty()
        assertThat(tracker.session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(tracker.session.toldMarks).isEqualTo(1)
        // The next is the second, and the first is never told.
        val on = tracker.walk(again + 5 * MINUTE, 7 * MINUTE)
        assertThat(on).containsExactly(SessionSignal.Mark(2))
    }

    @Test
    fun `the process restarted - the count of marks told read from what was stored`() {
        val first = tracker()
        first.walk(start, 25 * MINUTE)
        val stored = first.session
        assertThat(stored.toldMarks).isEqualTo(2)

        val again = tracker(stored)
        val on = again.walk(start + 25 * MINUTE, 10 * MINUTE)
        assertThat(on).containsExactly(SessionSignal.Mark(3))
    }

    @Test
    fun `a kilometre settled at a pause is told there, none at the end`() {
        val walker = walker(open(plan(gps = true), withGps = true))
        // 1,050 m on the ground, 962 m by the steps, none of it settled yet.
        walker.go(750, settle = false)
        // The pause cuts the line: the metres it settles are told as any settled metres.
        walker.tracker.pause(walker.at)
        assertThat(walker.tracker.takeHeldSignals().marks()).containsExactly(SessionSignal.Mark(1))
        walker.tracker.resume(walker.at)
        walker.go(750, settle = false)
        // At the reader's stop the last metres are the outing's, and tell nothing.
        val stopped = checkNotNull(walker.tracker.stop(walker.at))
        assertThat(stopped.session.totals.distanceMeters).isAtLeast(1_900.0)
        assertThat(stopped.session.toldMarks).isEqualTo(1)
        assertThat(walker.tracker.takeHeldSignals()).isEmpty()
    }

    @Test
    fun `a time mark says its minutes and the steps they came to`() {
        val tracker = tracker()
        tracker.walk(start, 20 * MINUTE)
        val said = SessionAnnouncement.mark(tracker.session, 2) as SessionAnnouncement.TimeMark
        assertThat(said.minutes).isEqualTo(20)
        assertThat(said.steps).isEqualTo(tracker.session.totals.steps)
    }

    @Test
    fun `an outing from before the marks has none`() {
        val before = Session(
            planId = null,
            name = null,
            goalKind = SessionGoalKind.OPEN,
            goalValue = 0,
            intensity = SessionPlans.OPEN_WALK.intensity,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = 20_000,
            startedAtMillis = start,
        )
        assertThat(before.marks).isEqualTo(SessionMarks.NONE)
        assertThat(tracker(before).walk(start, 25 * MINUTE)).isEmpty()
    }

    private companion object {
        const val MINUTE = 60_000L
        const val BATCH = 30_000L
        const val STEPS_A_BATCH = 55
        const val CADENCE = 110
        const val LATITUDE = 45.4642
        const val LONGITUDE = 9.1914
        const val METERS_PER_DEGREE = 111_195.08
    }
}
