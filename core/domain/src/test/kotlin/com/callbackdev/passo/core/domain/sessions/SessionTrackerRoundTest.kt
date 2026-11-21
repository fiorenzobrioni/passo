package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.rounds.Ground
import com.callbackdev.passo.core.domain.rounds.RoundGuide
import com.callbackdev.passo.core.domain.rounds.RoundWatch
import com.callbackdev.passo.core.domain.routes.Fix
import com.callbackdev.passo.core.domain.routes.RouteMeter
import com.callbackdev.passo.core.model.RoundLine
import com.callbackdev.passo.core.model.RoundStanding
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionMarks
import com.callbackdev.passo.core.model.SessionPlan
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.hypot

/**
 * An open outing with GPS on one of the reader's usual rounds (PLANNING.md §11 Phase 20, «The card,
 * while it counts»): found after half a kilometre, compared with the usual at each kilometre as the
 * kilometre's mark is told, left once; and nothing of it for any other outing. Each test is a row of
 * the phase's edge cases, or what its section says of the line.
 */
class SessionTrackerRoundTest {
    private val lengths = StepLengths(walkingMeters = 0.7, runningMeters = 0.9)
    private val start = 1_800_000_000_000L

    /** The park's round: walked four times, thirteen minutes a kilometre as a rule. */
    private val park = RoundGuide(
        roundId = 7,
        lines = listOf(Ground.walk(Ground.LOOP, seed = 1)),
        usualAtMarks = listOf(13 * MINUTE, 26 * MINUTE, 39 * MINUTE),
        walks = 4,
    )

    private fun open(marks: SessionMarks? = null): Session = checkNotNull(
        SessionPlans.start(
            SessionPlans.OPEN_WALK.copy(id = 1, gps = true, marks = marks),
            start,
            20_000,
            0,
            8_000,
            withGps = true,
        ),
    )

    private fun goal(): Session = checkNotNull(
        SessionPlans.start(
            SessionPlan(
                id = 2,
                goalKind = SessionGoalKind.TIME,
                goalValue = 60,
                intensity = SessionPlans.OPEN_WALK.intensity,
                gps = true,
            ),
            start,
            20_000,
            0,
            8_000,
            withGps = true,
        ),
    )

    /**
     * A reader walking [corners] at 1.4 m/s, a fix a second on the line drawn by hand, 110 steps a
     * minute handed over every ten seconds and settled with them.
     */
    private inner class Walker(
        session: Session,
        guides: List<RoundGuide> = listOf(park),
        routeSoFar: List<RoutePoint> = emptyList(),
    ) {
        val tracker = SessionTracker(
            session,
            lengths,
            weightKg = 70.0,
            RouteMeter(),
            RoundWatch(guides).also { if (routeSoFar.isNotEmpty()) it.add(routeSoFar) },
        )
        var at = start
        val signals = mutableListOf<SessionSignal>()
        private var owed = 0.0

        fun walk(corners: List<Pair<Double, Double>>, meters: Double, fixes: Boolean = true) {
            var done = 0.0
            var index = 1
            var x = corners[0].first
            var y = corners[0].second
            while (done < meters && index < corners.size) {
                val (tx, ty) = corners[index]
                val left = hypot(tx - x, ty - y)
                if (left < SPEED) {
                    x = tx
                    y = ty
                    index++
                    continue
                }
                x += (tx - x) / left * SPEED
                y += (ty - y) / left * SPEED
                done += SPEED
                at += 1_000
                if (fixes) {
                    val point = Ground.point(x, y, at / 1_000)
                    tracker.onFix(Fix(at, point.latitude, point.longitude, accuracyMeters = 5f))
                }
                owed += CADENCE / 60.0
                if ((at - start) % 10_000 == 0L) {
                    val count = owed.toInt()
                    owed -= count
                    signals += tracker.onSteps(at, count)
                    signals += tracker.settle(at, at)
                }
            }
        }

        val line: RoundLine? get() = tracker.session.roundLine
    }

    private fun List<SessionSignal>.lines() = filterIsInstance<SessionSignal.RoundLineChanged>().map { it.line }

    @Test
    fun `on the round from its door, it is found after half a kilometre, with nothing to say yet`() {
        val walker = Walker(open())
        walker.walk(Ground.LOOP, 450.0)
        assertThat(walker.line).isNull()
        val further = Walker(open())
        further.walk(Ground.LOOP, 600.0)
        assertThat(
            further.line,
        ).isEqualTo(RoundLine(roundId = 7, standing = RoundStanding.ON, marks = 0, gapMillis = null))
        assertThat(SessionHeadline.of(further.tracker.session)).isInstanceOf(SessionHeadline.OpenGoing::class.java)
    }

    @Test
    fun `from the first kilometre, how far ahead of the usual, again at each, told with the kilometre's mark`() {
        val walker = Walker(open())
        walker.walk(Ground.LOOP, 2_050.0)
        val line = checkNotNull(walker.line)
        assertThat(line.marks).isEqualTo(2)
        // 2 km at 1.4 m/s: some 23 minutes 49 seconds against 26: two minutes ahead, and some.
        assertThat(checkNotNull(line.gapMillis)).isLessThan(-120_000L)
        assertThat(walker.signals.lines().map { it.marks }).containsAtLeast(0, 1, 2).inOrder()
        val mark = SessionAnnouncement.mark(walker.tracker.session, 2) as SessionAnnouncement.DistanceMark
        assertThat(mark.round).isEqualTo(RoundSaid.Gap(line.gapMillis ?: 0))
        assertThat(SessionHeadline.of(walker.tracker.session)).isEqualTo(SessionHeadline.OnRound(line.gapMillis ?: 0))
    }

    @Test
    fun `the reader leaves the round at the first kilometre, the card says so once and the line stops`() {
        val walker = Walker(open())
        // The park's first street, then north up a street of its own: off the round from about 640 m.
        walker.walk(Ground.PARK_THEN_NORTH, 950.0)
        assertThat(walker.line?.standing).isEqualTo(RoundStanding.LEFT)
        assertThat(SessionHeadline.of(walker.tracker.session)).isEqualTo(SessionHeadline.OffRound)
        val further = Walker(open())
        further.walk(Ground.PARK_THEN_NORTH, 1_050.0)
        // The kilometre after it says the round was left, once; the sentence is the outing's own again.
        assertThat(further.line?.standing).isEqualTo(RoundStanding.GONE)
        assertThat(further.line?.marks).isEqualTo(1)
        val first = SessionAnnouncement.mark(further.tracker.session, 1) as SessionAnnouncement.DistanceMark
        assertThat(first.round).isEqualTo(RoundSaid.Left)
        assertThat(SessionHeadline.of(further.tracker.session)).isInstanceOf(SessionHeadline.OpenGoing::class.java)
        val farther = Walker(open())
        farther.walk(Ground.PARK_THEN_NORTH, 2_050.0)
        val second = SessionAnnouncement.mark(farther.tracker.session, 2) as SessionAnnouncement.DistanceMark
        assertThat(second.round).isNull()
        assertThat(farther.line?.standing).isEqualTo(RoundStanding.GONE)
    }

    @Test
    fun `the round walked whole after its latest walk cut a corner keeps its line at each kilometre`() {
        // The latest walk cut the park's south-east corner across a diagonal: still the round.
        val cut = listOf(0.0 to 0.0, 600.0 to 0.0, 800.0 to 200.0, 800.0 to 800.0, 0.0 to 800.0, 0.0 to 0.0)
        val alone = Walker(open(), listOf(RoundGuide(7, listOf(Ground.walk(cut, seed = 3)), park.usualAtMarks, 4)))
        alone.walk(Ground.LOOP, 2_050.0)
        // Its line alone: the round walked whole as a rule would be told left at the corner.
        assertThat(alone.line?.standing).isNotEqualTo(RoundStanding.ON)
        val round = RoundGuide(7, listOf(Ground.walk(cut, seed = 3)) + park.lines, park.usualAtMarks, walks = 4)
        val walker = Walker(open(), listOf(round))
        walker.walk(Ground.LOOP, 2_050.0)
        assertThat(walker.line?.standing).isEqualTo(RoundStanding.ON)
        assertThat(walker.line?.marks).isEqualTo(2)
        assertThat(walker.signals.lines().map { it.standing }).doesNotContain(RoundStanding.LEFT)
    }

    @Test
    fun `an open outing with marks every ten minutes still shows the line at each kilometre, silently`() {
        val walker = Walker(open(marks = SessionMarks.TIME))
        walker.walk(Ground.LOOP, 1_100.0)
        assertThat(walker.line?.marks).isEqualTo(1)
        assertThat(walker.signals.filterIsInstance<SessionSignal.Mark>().map { it.count }).doesNotContain(0)
        // The marks are its ten minutes, said as ever; the round's line rides on the kilometre, silent.
        assertThat(walker.signals.lines().map { it.marks }).contains(1)
        val tenMinutes = SessionAnnouncement.mark(walker.tracker.session, 1)
        assertThat(tenMinutes).isInstanceOf(SessionAnnouncement.TimeMark::class.java)
    }

    @Test
    fun `an open outing with no marks still shows the line, and says nothing`() {
        val walker = Walker(open(marks = SessionMarks.NONE))
        walker.walk(Ground.LOOP, 1_100.0)
        assertThat(walker.line?.gapMillis).isNotNull()
        assertThat(walker.signals.filterIsInstance<SessionSignal.Mark>()).isEmpty()
    }

    @Test
    fun `a goal outing with GPS walked on a round has no line on its card, its sentence its goal's`() {
        val walker = Walker(goal())
        walker.walk(Ground.LOOP, 1_100.0)
        assertThat(walker.line).isNull()
        assertThat(walker.signals.lines()).isEmpty()
    }

    @Test
    fun `the same streets walked the other way are never on the round`() {
        val walker = Walker(open())
        walker.walk(Ground.LOOP_OTHER_WAY, 1_500.0)
        assertThat(walker.line).isNull()
    }

    @Test
    fun `no fix at all, no line, nothing is compared on the steps alone`() {
        val walker = Walker(open())
        walker.walk(Ground.LOOP, 1_500.0, fixes = false)
        assertThat(walker.line).isNull()
    }

    @Test
    fun `a kilometre the round's walks did not reach whole keeps the last comparison`() {
        val short = RoundGuide(7, park.lines, listOf(13 * MINUTE), walks = 3)
        val walker = Walker(open(), listOf(short))
        walker.walk(Ground.LOOP, 2_100.0)
        assertThat(walker.line?.marks).isEqualTo(1)
        val mark = SessionAnnouncement.mark(walker.tracker.session, 2) as SessionAnnouncement.DistanceMark
        // Its words are the kilometre's own, with its pace.
        assertThat(mark.round).isNull()
        assertThat(mark.paceSecondsPerKm).isNotNull()
    }

    @Test
    fun `a process restarted mid round goes on from the kilometre told, none told twice`() {
        val walker = Walker(open())
        walker.walk(Ground.LOOP, 1_300.0)
        val written = walker.tracker.session
        val route = walker.tracker.takeRoute().points
        assertThat(written.roundLine?.marks).isEqualTo(1)
        // A new tracker from what was written, its watch fed the route kept so far.
        val again = Walker(written, routeSoFar = route)
        again.at = walker.at
        again.walk(Ground.LOOP.continueFrom(1_300.0), 800.0)
        assertThat(again.signals.filterIsInstance<SessionSignal.Mark>().map { it.count }).containsExactly(2)
        assertThat(again.line?.marks).isEqualTo(2)
        assertThat(again.line?.standing).isEqualTo(RoundStanding.ON)
    }

    /** These corners from [meters] along them on. */
    private fun List<Pair<Double, Double>>.continueFrom(meters: Double): List<Pair<Double, Double>> {
        var left = meters
        for (index in 1 until size) {
            val (x0, y0) = this[index - 1]
            val (x1, y1) = this[index]
            val length = hypot(x1 - x0, y1 - y0)
            if (left <= length) {
                val t = left / length
                return listOf(x0 + (x1 - x0) * t to y0 + (y1 - y0) * t) + drop(index)
            }
            left -= length
        }
        return listOf(last())
    }

    private companion object {
        const val MINUTE = 60_000L
        const val SPEED = 1.4
        const val CADENCE = 110.0
    }
}
