package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.routes.Fix
import com.callbackdev.passo.core.domain.routes.RouteMeter
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * An outing measured with GPS, as the tracker counts it (PLANNING.md §11 Phase 15;
 * docs/adr/0016-location.md, decisions 5, 6 and 14): its distance from the meter, told only on
 * metres the fixes cannot take back; everything else the steps', as ever.
 */
class SessionTrackerRouteTest {
    private val lengths = StepLengths(walkingMeters = 0.7, runningMeters = 0.9)
    private val start = 1_000_000_000_000L

    private fun plan(kind: SessionGoalKind, value: Int, milestones: Set<SessionMilestone> = emptySet()) = SessionPlan(
        id = 1,
        goalKind = kind,
        goalValue = value,
        intensity = SessionIntensity.FREE,
        milestones = milestones,
        gps = true,
    )

    private fun started(plan: SessionPlan, withGps: Boolean = true): Session =
        checkNotNull(SessionPlans.start(plan, start, 20_000, 0, 8_000, withGps))

    /** A reader, a second at a time: 110 steps a minute handed over every ten seconds, and a fix a second. */
    private inner class Walker(val tracker: SessionTracker) {
        var at = start
        var north = 0.0
        val signals = mutableListOf<SessionSignal>()
        private var owed = 0.0

        fun go(
            seconds: Int,
            speed: Double = 1.4,
            fixes: Boolean = true,
            settle: Boolean = true,
            steps: Boolean = true,
            cadence: Int = 110,
        ) {
            repeat(seconds) {
                at += 1_000
                north += speed
                if (fixes) tracker.onFix(Fix(at, LATITUDE + north / METERS_PER_DEGREE, LONGITUDE, accuracyMeters = 5f))
                if (steps) owed += cadence / 60.0
                if (steps && (at - start) % 10_000 == 0L) {
                    val count = owed.toInt()
                    owed -= count
                    signals += tracker.onSteps(at, count)
                    if (settle) signals += tracker.settle(at, at)
                }
            }
        }
    }

    private fun walker(session: Session, meter: RouteMeter? = RouteMeter()) =
        Walker(SessionTracker(session, lengths, weightKg = 70.0, meter))

    @Test
    fun `it says when its last fix came and how far its steps are settled, for the card's line`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))
        assertThat(walker.tracker.lastFixAtMillis).isNull()
        assertThat(walker.tracker.settledThroughMillis).isNull()

        walker.go(60)
        assertThat(walker.tracker.lastFixAtMillis).isEqualTo(walker.at)
        assertThat(walker.tracker.settledThroughMillis).isEqualTo(walker.at)

        // A tunnel: the steps go on and are settled, the last fix stays where it was.
        val before = walker.at
        walker.go(30, fixes = false)
        assertThat(walker.tracker.lastFixAtMillis).isEqualTo(before)
        assertThat(walker.tracker.settledThroughMillis).isEqualTo(walker.at)

        // A pause cuts the line: the next stretch finds the satellites again.
        walker.tracker.pause(walker.at)
        assertThat(walker.tracker.lastFixAtMillis).isNull()
    }

    @Test
    fun `an outing without GPS has neither`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60), withGps = false))

        walker.go(60)

        assertThat(walker.tracker.lastFixAtMillis).isNull()
        assertThat(walker.tracker.settledThroughMillis).isNull()
    }

    @Test
    fun `it is laid a hundred metres at a time, and the legs add up to its distance, steps and time`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))

        walker.go(600)

        val session = walker.tracker.session
        // 840 m: eight legs of a hundred and the one under way.
        assertThat(session.legs.size).isAtLeast(8)
        assertThat(session.legs.dropLast(1).map { it.meters }.distinct().single()).isWithin(1e-6).of(100.0)
        assertThat(session.legs.sumOf { it.meters }).isWithin(1e-6).of(session.totals.distanceMeters)
        assertThat(session.legs.sumOf { it.steps }).isWithin(1e-6).of(session.totals.steps.toDouble())
        assertThat(session.legs.sumOf { it.movingMillis }.toDouble())
            .isWithin(1_000.0).of(session.totals.movingMillis.toDouble())
        // Each leg ends later than the one before, on the steps' clock.
        assertThat(session.legs.map { it.endedAtMillis }).isInOrder()
    }

    @Test
    fun `a pause and a stop lose no metre of the legs`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))

        walker.go(125)
        walker.tracker.pause(walker.at)
        walker.tracker.resume(walker.at)
        walker.go(125)
        val finished = checkNotNull(walker.tracker.stop(walker.at)).session

        assertThat(finished.legs.sumOf { it.meters }).isWithin(1e-6).of(finished.totals.distanceMeters)
    }

    @Test
    fun `an outing without GPS has no legs`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60), withGps = false))

        walker.go(600)

        assertThat(walker.tracker.session.legs).isEmpty()
    }

    @Test
    fun `what was walked after the goal is laid on the legs only if the outing is taken back`() {
        val walker = walker(started(plan(SessionGoalKind.DISTANCE, 500)))
        walker.go(600)
        val ended = walker.tracker.session
        assertThat(ended.state).isEqualTo(SessionState.FINISHED)
        val legsAtGoal = ended.legs.sumOf { it.meters }
        assertThat(legsAtGoal).isWithin(1e-6).of(ended.totals.distanceMeters)

        checkNotNull(walker.tracker.keepGoing(walker.at))

        val reopened = walker.tracker.session
        assertThat(reopened.legs.sumOf { it.meters }).isGreaterThan(legsAtGoal + 50)
        assertThat(reopened.legs.sumOf { it.meters }).isWithin(1e-6).of(reopened.totals.distanceMeters)
    }

    @Test
    fun `a climb is the outing's, the leg's it was walked in, and its energy's`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))
        walker.go(600)
        val before = walker.tracker.session
        assertThat(before.ascentMeters).isNull()

        // The first altitude known: a height, and nothing climbed yet.
        walker.tracker.addClimb(start + 10_000, 0.0, 0.0)
        assertThat(walker.tracker.session.ascentMeters).isEqualTo(0.0)
        // Twelve metres up in the third minute, four down in the eighth.
        walker.tracker.addClimb(start + 150_000, 12.0, 0.0)
        walker.tracker.addClimb(start + 450_000, 0.0, 4.0)

        val session = walker.tracker.session
        assertThat(session.ascentMeters).isEqualTo(12.0)
        assertThat(session.descentMeters).isEqualTo(4.0)
        assertThat(session.legs.sumOf { it.ascentMeters }).isEqualTo(12.0)
        assertThat(session.legs.first { it.ascentMeters > 0 }.endedAtMillis).isAtLeast(start + 150_000)
        // 0.009 kcal a kilogram a metre climbed; nothing for coming down.
        assertThat(session.totals.activeKcal - before.totals.activeKcal).isWithin(1e-9).of(12.0 * 70.0 * 0.009)
        // The distance, the steps and the time are untouched.
        assertThat(session.totals.copy(activeKcal = 0.0)).isEqualTo(before.totals.copy(activeKcal = 0.0))
    }

    @Test
    fun `a climb run up and told after the walk began again is priced as the run it was`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))
        // Two minutes up a hill at a run, then a walk: the climb is told a minute and a half
        // later, its points having waited for their pressure.
        walker.go(120, speed = 3.0, cadence = 160)
        walker.go(120)
        val before = walker.tracker.session

        walker.tracker.addClimb(start + 60_000, 30.0, 0.0)

        // A run's 0.0045 kcal a kilogram a metre, not the walk's 0.009.
        assertThat(walker.tracker.session.totals.activeKcal - before.totals.activeKcal)
            .isWithin(1e-9).of(30.0 * 70.0 * 0.0045)
    }

    @Test
    fun `cut as the service goes, an outing keeps every metre its meter held, and counts on`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))
        walker.go(115, settle = false)

        walker.tracker.cutRoute()

        val session = walker.tracker.session
        assertThat(session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(session.totals.distanceMeters).isWithin(12.0).of(1.4 * 115)
        assertThat(walker.tracker.takeRoute().points).isNotEmpty()
        assertThat(walker.tracker.openMeters).isEqualTo(0.0)
    }

    @Test
    fun `an outing without GPS takes no climb - its energy is the steps' alone`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60), withGps = false))
        walker.go(300)
        val before = walker.tracker.session

        walker.tracker.addClimb(start + 100_000, 20.0, 0.0)

        assertThat(walker.tracker.session).isEqualTo(before)
    }

    @Test
    fun `its distance is the measured one, and what the steps alone said is kept beside it`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))

        walker.go(600)

        val session = walker.tracker.session
        // 840 m walked; the step length (0.70) says 770.
        assertThat(session.totals.distanceMeters).isWithin(8.0).of(840.0)
        assertThat(session.stepsMeters).isWithin(2.0).of(770.0)
        assertThat(session.measuredShare).isAtLeast(0.99)
        assertThat(session.gps).isTrue()
    }

    @Test
    fun `its energy is priced on the measured distance`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))

        walker.go(600)

        val session = walker.tracker.session
        val perKm = 70.0 * MetricsCalculator.kcalPerKgPerKm(110)
        assertThat(session.totals.activeKcal).isWithin(1.5).of(session.totals.distanceMeters / 1_000 * perKm)
    }

    @Test
    fun `steps and time in motion are the steps', told with them, settled or not`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))

        walker.go(120, settle = false)

        val session = walker.tracker.session
        // 110 a minute for two minutes, less the fraction of a step still owed by the counter.
        assertThat(session.totals.steps).isIn(219..220)
        assertThat(session.totals.movingMillis).isEqualTo(120_000)
        // The distance waits for its fixes to be known in: shown as open, told on nothing.
        assertThat(session.totals.distanceMeters).isEqualTo(0.0)
        assertThat(walker.tracker.openMeters).isWithin(1e-6).of(session.totals.steps * 0.7)
    }

    @Test
    fun `the goal 20 m away on the steps' estimate is not reached while the fixes say 45`() {
        // Half a kilometre to walk. This reader's step is shorter than the profile's: the steps
        // run ahead of the ground.
        val walker = walker(started(plan(SessionGoalKind.DISTANCE, 500)))

        // 420 s at 1.1 m/s: 462 m on the ground, 539 m by the steps.
        walker.go(420, speed = 1.1)

        assertThat(walker.tracker.session.stepsMeters).isGreaterThan(500.0)
        assertThat(walker.tracker.session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(walker.signals.filterIsInstance<SessionSignal.Finished>()).isEmpty()

        // It ends where the ground says: 38 m further on.
        walker.go(60, speed = 1.1)

        val ended = walker.signals.filterIsInstance<SessionSignal.Finished>().single()
        assertThat(ended.session.end).isEqualTo(SessionEnd.GOAL)
        assertThat(ended.session.totals.distanceMeters).isWithin(12.0).of(500.0)
    }

    @Test
    fun `nothing is told on metres whose fixes are not in yet`() {
        val walker = walker(started(plan(SessionGoalKind.DISTANCE, 500, setOf(SessionMilestone.HALF))))

        // Four minutes of steps in hand, 308 m by their estimate, and no fix delivered yet.
        walker.go(240, fixes = false, settle = false)

        assertThat(walker.signals).isEmpty()

        // Told once they are settled (here with no fix at all: a hole, the steps' for good).
        walker.signals += walker.tracker.settle(walker.at, walker.at)

        assertThat(walker.signals).containsExactly(SessionSignal.Milestone(SessionMilestone.HALF))
    }

    @Test
    fun `a milestone of a distance goal is told on the measured distance`() {
        val walker = walker(started(plan(SessionGoalKind.DISTANCE, 1_000, setOf(SessionMilestone.HALF))))

        // 350 s at 1.4: 490 m on the ground. Not halfway yet.
        walker.go(350)
        assertThat(walker.signals).isEmpty()

        walker.go(20)
        assertThat(walker.signals).containsExactly(SessionSignal.Milestone(SessionMilestone.HALF))
    }

    @Test
    fun `an outing that did not start with GPS ignores the meter it was given`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60), withGps = false))

        walker.go(120)

        val session = walker.tracker.session
        assertThat(walker.tracker.measuring).isFalse()
        assertThat(session.totals.distanceMeters).isWithin(0.01).of(session.totals.steps * 0.7)
        assertThat(session.stepsMeters).isNull()
        assertThat(session.measuredShare).isNull()
        assertThat(walker.tracker.takeRoute().isEmpty).isTrue()
    }

    @Test
    fun `an interval outing with GPS changes interval when the steps say, as one without`() {
        fun changes(gps: Boolean): List<SessionSignal.IntervalChange> {
            val walker = walker(started(SessionPlans.JAPANESE_WALKING.copy(gps = true), withGps = gps))
            walker.go(900)
            return walker.signals.filterIsInstance<SessionSignal.IntervalChange>()
        }

        val measured = changes(gps = true)

        assertThat(measured).isNotEmpty()
        assertThat(measured).isEqualTo(changes(gps = false))
    }

    @Test
    fun `the route's points are handed over once, to be written`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))

        walker.go(60)
        val first = walker.tracker.takeRoute()
        walker.go(60)
        val second = walker.tracker.takeRoute()

        assertThat(first.points.first().kind).isEqualTo(RoutePointKind.START)
        assertThat(second.points).isNotEmpty()
        assertThat(second.points.first().epochSecond).isGreaterThan(first.points.last().epochSecond)
        assertThat(walker.tracker.takeRoute().isEmpty).isTrue()
    }

    @Test
    fun `a pause cuts the line, and the outing is measured again after it`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))
        walker.go(120)

        walker.tracker.pause(walker.at)
        val paused = walker.tracker.session.totals.distanceMeters
        // Paused, the reader goes somewhere else: nothing is heard, nothing is counted.
        walker.go(120, steps = false)
        assertThat(walker.tracker.session.totals.distanceMeters).isEqualTo(paused)

        walker.tracker.resume(walker.at)
        walker.go(120)

        assertThat(walker.tracker.session.totals.distanceMeters).isWithin(10.0).of(paused + 168.0)
        val points = walker.tracker.takeRoute().points
        assertThat(points.count { it.kind == RoutePointKind.START }).isEqualTo(2)
    }

    @Test
    fun `the goal reached with GPS and taken back ten minutes later, the route has no hole`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 5)))

        walker.go(300)
        val ended = walker.signals.filterIsInstance<SessionSignal.Finished>().single()
        assertThat(ended.session.end).isEqualTo(SessionEnd.GOAL)
        val atGoal = walker.tracker.session.totals.distanceMeters
        val kept = walker.tracker.takeRoute().points

        // The reader walks on, the receiver with them; what they walk waits aside.
        walker.go(600)
        assertThat(walker.tracker.session.totals.distanceMeters).isEqualTo(atGoal)
        assertThat(walker.tracker.takeRoute().isEmpty).isTrue()

        assertThat(walker.tracker.keepGoing(walker.at)).isNotNull()

        val session = walker.tracker.session
        assertThat(session.state).isEqualTo(SessionState.ACTIVE)
        assertThat(session.totals.distanceMeters).isWithin(12.0).of(atGoal + 840.0)
        assertThat(session.measuredShare).isAtLeast(0.98)
        // The ten minutes' points, joined to the route: a new line from the goal on, no gap in time.
        val joined = walker.tracker.takeRoute().points
        assertThat(joined.size).isAtLeast(150)
        assertThat(joined.first().epochSecond - kept.last().epochSecond).isAtMost(15)
    }

    @Test
    fun `not taken back, what was walked after the end is nobody's`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 5)))
        walker.go(300)
        walker.tracker.takeRoute()
        val atGoal = walker.tracker.session.totals.distanceMeters

        // Twenty minutes: past the fifteen in which it could be taken back.
        walker.go(1_200)

        assertThat(walker.tracker.canKeepGoing(walker.at)).isFalse()
        assertThat(walker.tracker.keepGoing(walker.at)).isNull()
        assertThat(walker.tracker.session.totals.distanceMeters).isEqualTo(atGoal)
        assertThat(walker.tracker.takeRoute().isEmpty).isTrue()
    }

    @Test
    fun `the process restarted, the outing goes on from the steps with what it had measured`() {
        val first = walker(started(plan(SessionGoalKind.TIME, 60)))
        first.go(300)
        val stored = first.tracker.session

        // A new process: a new meter, no receiver until the reader brings the app forward.
        val second = Walker(SessionTracker(stored, lengths, weightKg = 70.0, RouteMeter()))
        second.at = first.at
        second.north = first.north
        second.go(300, fixes = false)

        val session = second.tracker.session
        // 420 m measured, then 385 m by the steps (0.70 a step).
        assertThat(session.totals.distanceMeters).isWithin(8.0).of(420.0 + 385.0)
        assertThat(session.measuredShare).isWithin(0.02).of(420.0 / 805.0)
    }

    @Test
    fun `stopped by the reader, the last steps are settled with what fixes there are`() {
        val walker = walker(started(plan(SessionGoalKind.TIME, 60)))
        walker.go(115, settle = false)

        val ended = checkNotNull(walker.tracker.stop(walker.at))

        // Nothing was settled on the way: everything is, at the end. No lost metres.
        assertThat(ended.session.totals.distanceMeters).isWithin(12.0).of(1.4 * 115)
        assertThat(ended.session.end).isEqualTo(SessionEnd.STOPPED)
        assertThat(walker.tracker.takeRoute().points).isNotEmpty()
    }

    private companion object {
        const val LATITUDE = 45.4642
        const val LONGITUDE = 9.1914
        const val METERS_PER_DEGREE = 111_195.08
    }
}
