package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.StepLengthSample
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.sin

/**
 * The steps keep the GPS honest, and the GPS the steps (PLANNING.md §11 Phase 15;
 * docs/adr/0016-location.md, decision 5): each rule of the fusion, and each row of the phase's
 * edge cases that is the meter's.
 */
class RouteMeterTest {
    /**
     * A reader and their phone, a second at a time: where they really are, what the receiver
     * says, and the steps the counter hands over, in batches.
     */
    private class Walk(val meter: RouteMeter = RouteMeter()) {
        var seconds = 0
        var north = 0.0
        var settledMeters = 0.0
        var measuredMeters = 0.0
        var stepsMeters = 0.0
        val points = ArrayList<RoutePoint>()
        val samples = ArrayList<StepLengthSample>()
        private var batchFrom = 0
        private var owedSteps = 0.0

        val nowMillis: Long get() = seconds * 1_000L

        fun fixAt(meters: Double, east: Double = 0.0, accuracy: Float = 5f) = Fix(
            atMillis = nowMillis,
            latitude = LATITUDE + meters / METERS_PER_DEGREE,
            longitude = LONGITUDE + east / METERS_PER_DEGREE_EAST,
            accuracyMeters = accuracy,
        )

        /**
         * [duration] seconds. The reader moves at [speed] and takes [cadence] steps a minute
         * (none at 0), handed over every [batch] seconds; the receiver gives a fix a second
         * unless [fixes] is false, off the true place by [wander] metres to the side.
         */
        fun go(
            duration: Int,
            speed: Double = WALK_SPEED,
            cadence: Int = 110,
            stepLength: Double = 0.75,
            fixes: Boolean = true,
            batch: Int = 10,
            accuracy: Float = 5f,
            wander: (Int) -> Double = { 0.0 },
            settle: Boolean = true,
        ) {
            repeat(duration) {
                seconds++
                north += speed
                if (fixes) {
                    meter.onFix(
                        fixAt(north, east = wander(seconds), accuracy = accuracy),
                        cadence.takeIf {
                            it >
                                0
                        },
                    )
                }
                owedSteps += cadence / 60.0
                if (cadence > 0 && (seconds - batchFrom) >= batch) {
                    val steps = owedSteps.toInt()
                    owedSteps -= steps
                    val estimate = steps * stepLength
                    meter.onSteps(
                        batchFrom * 1_000L,
                        nowMillis,
                        steps,
                        (seconds - batchFrom) * 1_000L,
                        estimate,
                        cadence,
                    )
                    stepsMeters += estimate
                    batchFrom = seconds
                    if (settle) take(meter.settle(nowMillis))
                }
            }
            // Nothing was owed while the reader took no step: the next batch starts from here.
            if (cadence == 0) {
                batchFrom = seconds
                owedSteps = 0.0
            }
        }

        fun take(settled: RouteSettlement) {
            assertThat(settled.meters).isAtLeast(0.0)
            settledMeters += settled.meters
            measuredMeters += settled.measuredMeters
            points += settled.points
            samples += settled.samples
        }

        fun close() = take(meter.close())

        val share: Double get() = measuredMeters / settledMeters
    }

    @Test
    fun `a walk under an open sky is measured, not estimated`() {
        val walk = Walk()

        walk.go(600)
        walk.close()

        // 840 m walked; the step length (0.75) would have said 825.
        assertThat(walk.settledMeters).isWithin(8.0).of(840.0)
        assertThat(walk.stepsMeters).isWithin(1.0).of(825.0)
        assertThat(walk.share).isAtLeast(0.99)
    }

    @Test
    fun `no fix for the first minute, the outing counts from its first step and that stretch is the steps'`() {
        val walk = Walk()

        walk.go(60, fixes = false)

        // Counted as it grows, with no fix in sight: the walk does not wait for the satellites.
        assertThat(walk.settledMeters).isWithin(1e-6).of(walk.stepsMeters)
        assertThat(walk.settledMeters).isWithin(1.0).of(82.5)
        assertThat(walk.measuredMeters).isEqualTo(0.0)
        assertThat(walk.points).isEmpty()

        walk.go(60)
        walk.close()

        assertThat(walk.settledMeters).isWithin(6.0).of(82.5 + 84.0)
        assertThat(walk.share).isWithin(0.03).of(0.5)
        assertThat(walk.points.first().kind).isEqualTo(RoutePointKind.START)
    }

    @Test
    fun `standing ten minutes with the fixes wandering adds no distance and no points`() {
        val walk = Walk()
        walk.go(60)
        val walked = walk.settledMeters
        val kept = walk.points.size

        // A phone on a bench: the receiver wanders four metres around it.
        walk.go(600, speed = 0.0, cadence = 0, wander = { second -> 4.0 * sin(second / 9.0) })
        walk.take(walk.meter.settle(walk.nowMillis))

        assertThat(walk.settledMeters).isEqualTo(walked)
        assertThat(walk.points).hasSize(kept)

        walk.go(60)
        walk.close()

        // Only the two minutes walked, either side of the bench.
        assertThat(walk.settledMeters).isWithin(8.0).of(168.0)
    }

    @Test
    fun `a tram between two walks adds nothing, and the walk resumes where the steps do`() {
        val walk = Walk()
        walk.go(60)

        // Two minutes on a slow tram, the phone in a pocket: movement, and no step.
        walk.go(120, speed = 3.0, cadence = 0)
        walk.go(60)
        walk.close()

        assertThat(walk.settledMeters).isWithin(8.0).of(168.0)
        // The line starts anew where the reader got off: none is drawn across the ride.
        val starts = walk.points.filter { it.kind == RoutePointKind.START }
        assertThat(starts).hasSize(2)
        val across = walk.points.zipWithNext().filter { (_, to) -> to.kind != RoutePointKind.START }
            .maxOf { (from, to) -> Geodesy.meters(from.latitude, from.longitude, to.latitude, to.longitude) }
        assertThat(across).isAtMost(30.0)
    }

    @Test
    fun `a bus too fast for any fix to be taken, then a walk the receiver is believed in again`() {
        val walk = Walk()
        walk.go(60)

        // Ten metres a second: every fix refused, as one bad fix would be.
        walk.go(120, speed = 10.0, cadence = 0)
        assertThat(walk.meter.refused).isAtLeast(100)

        walk.go(120)
        walk.close()

        // The ride's 1200 m are nowhere; the walk after it is measured again within seconds.
        assertThat(walk.settledMeters).isWithin(12.0).of(84.0 + 168.0)
        assertThat(walk.share).isAtLeast(0.9)
    }

    @Test
    fun `a few steps on a moving tram are not the tram's distance`() {
        val walk = Walk()
        walk.go(60)
        val before = walk.settledMeters

        // Walking down the aisle, forty steps a minute, while the tram does three metres a second.
        walk.go(60, speed = 3.0, cadence = 40, batch = 30)
        walk.close()

        // The steps keep their own estimate (40 steps, 30 m): not the 180 m the fixes moved.
        assertThat(walk.settledMeters - before).isWithin(4.0).of(30.0)
    }

    @Test
    fun `a tunnel of 300 m is covered by the steps, never shorter than the straight line across`() {
        val walk = Walk()
        walk.go(60)
        val before = walk.settledMeters

        // 214 s at 1.4 m/s: 300 m with no fix. The steps would say 294.
        walk.go(107, fixes = false)
        val halfway = walk.settledMeters
        walk.go(107, fixes = false)
        walk.go(60)
        walk.close()

        // No lost metres: it counts while the reader is still inside.
        assertThat(halfway - before).isWithin(15.0).of(147.0)
        assertThat(walk.settledMeters).isWithin(10.0).of(84.0 + 300.0 + 84.0)
        // Drawn later as laid from the steps, and not counted as measured.
        assertThat(walk.points.count { it.kind == RoutePointKind.LAID }).isEqualTo(1)
        assertThat(walk.share).isWithin(0.03).of(168.0 / 468.0)
    }

    @Test
    fun `where the steps say more than the straight line across a hole, the steps stand`() {
        val walk = Walk()
        walk.go(60)
        val before = walk.settledMeters

        // An arcade that winds: 300 m as the crow flies, more on foot. A longer step says 364 m.
        walk.go(214, fixes = false, stepLength = 0.93)
        walk.go(10)

        assertThat(walk.settledMeters - before).isWithin(12.0).of(364.0 + 14.0)
    }

    @Test
    fun `a sensor with no FIFO handing 170 steps at once has them laid over their time in motion`() {
        val walk = Walk()

        // A minute and a half of walking, its fixes a second apart, its steps in one sample.
        walk.go(90, cadence = 113, batch = 90)
        walk.close()

        assertThat(walk.settledMeters).isWithin(5.0).of(126.0)
        assertThat(walk.share).isAtLeast(0.97)
    }

    @Test
    fun `steps ahead of their fixes stay open, and are measured when the fixes are in`() {
        val meter = RouteMeter()
        val walk = Walk(meter)
        walk.go(30)

        // The next half minute's steps arrive, their fixes still in the receiver's batch.
        meter.onSteps(30_000, 60_000, 55, 30_000, 41.25, 110)

        assertThat(meter.settle(fixesThroughMillis = 30_000).isEmpty).isTrue()
        assertThat(meter.openMeters).isWithin(1e-9).of(41.25)

        // The receiver hands them over: 45 m, where the steps said 41.
        walk.seconds = 30
        repeat(30) {
            walk.seconds++
            walk.north += 1.5
            meter.onFix(walk.fixAt(walk.north), 110)
        }
        val settled = meter.settle(fixesThroughMillis = 60_000)

        assertThat(meter.openMeters).isEqualTo(0.0)
        assertThat(settled.measuredMeters).isWithin(4.0).of(45.0)
        assertThat(settled.meters).isWithin(4.0).of(45.0)
    }

    @Test
    fun `what is settled never goes down, whatever comes`() {
        val walk = Walk()

        // Every settlement is checked not to be negative as it is taken (Walk.take).
        walk.go(45)
        walk.go(40, fixes = false)
        walk.go(30, speed = 0.0, cadence = 0, wander = { 3.0 * sin(it / 4.0) })
        walk.go(60, speed = 3.0, cadence = 0)
        walk.go(90, accuracy = 40f)
        walk.go(60)
        walk.close()

        assertThat(walk.settledMeters).isGreaterThan(0.0)
    }

    @Test
    fun `fixes too vague to trust are a hole, and the stretch is the steps'`() {
        val walk = Walk()

        // Forty metres of accuracy between tall buildings: none is accepted.
        walk.go(120, accuracy = 40f)
        walk.close()

        assertThat(walk.meter.taken).isEqualTo(0)
        assertThat(walk.measuredMeters).isEqualTo(0.0)
        assertThat(walk.settledMeters).isWithin(1.0).of(walk.stepsMeters)
    }

    @Test
    fun `a fix 60 m off is refused, and the line goes from good to good`() {
        val walk = Walk()
        walk.go(30)

        walk.seconds++
        walk.north += WALK_SPEED
        assertThat(walk.meter.onFix(walk.fixAt(walk.north + 60.0), 110)).isEqualTo(RouteFilter.Verdict.TOO_FAST)
        walk.go(29)
        walk.close()

        assertThat(walk.settledMeters).isWithin(6.0).of(84.0)
        assertThat(walk.meter.refused).isEqualTo(1)
    }

    @Test
    fun `steps that go nowhere are measured as what they are`() {
        val walk = Walk()

        // Marching on the spot under an open sky: 110 steps a minute, and the fixes do not move.
        walk.go(120, speed = 0.0)
        walk.close()

        // The steps alone would have said 165 m.
        assertThat(walk.stepsMeters).isWithin(1.0).of(165.0)
        assertThat(walk.settledMeters).isAtMost(3.0)
    }

    @Test
    fun `the receiver's wander is not added up as walked`() {
        val walk = Walk()

        // A fix a second, each a little to one side or the other of the line walked.
        walk.go(600, wander = { second -> 0.4 * sin(second * 1.7) })
        walk.close()

        assertThat(walk.settledMeters).isWithin(840.0 * 0.03).of(840.0)
    }

    @Test
    fun `where the receiver is less sure, the line moves by a coarser grain`() {
        val sure = Walk().apply { go(300, accuracy = 4f) }
        val unsure = Walk().apply { go(300, accuracy = 20f) }

        // The same walk, fewer points kept: ten metres between them where three were.
        assertThat(unsure.points.size).isLessThan(sure.points.size / 2)
        assertThat(unsure.settledMeters).isWithin(15.0).of(sure.settledMeters)
    }

    @Test
    fun `the points kept are the line's own ends, three metres or more apart`() {
        val walk = Walk()

        walk.go(60)

        assertThat(walk.points.first().kind).isEqualTo(RoutePointKind.START)
        assertThat(walk.points.drop(1).map { it.kind }.toSet()).containsExactly(RoutePointKind.MEASURED)
        // About one every three seconds of walking: 4.2 m.
        assertThat(walk.points.size).isIn(15..30)
        walk.points.zipWithNext().forEach { (from, to) ->
            val apart = Geodesy.meters(from.latitude, from.longitude, to.latitude, to.longitude)
            assertThat(apart).isAtLeast(RouteConstants.MIN_MOVE_METERS - 0.2)
        }
    }

    @Test
    fun `a slow reader still gets a point every five seconds`() {
        val walk = Walk()

        // A stroll at a shop window's pace: 0.4 m/s, two metres in five seconds.
        walk.go(60, speed = 0.4, cadence = 50)
        walk.close()

        assertThat(walk.points.size).isIn(10..14)
        assertThat(walk.settledMeters).isWithin(3.0).of(24.0)
    }

    @Test
    fun `a pause cuts the line, and what comes after starts a new one`() {
        val walk = Walk()
        walk.go(60)
        walk.close()
        val before = walk.settledMeters

        // Paused, and carried half a kilometre: none of it is the outing's.
        walk.north += 500.0
        walk.go(60)
        walk.close()

        assertThat(walk.settledMeters - before).isWithin(5.0).of(84.0)
        assertThat(walk.points.count { it.kind == RoutePointKind.START }).isEqualTo(2)
    }

    @Test
    fun `at the end the last steps are not lost`() {
        val walk = Walk()

        // The last batch's steps, with no fix after them to measure the last metre.
        walk.go(60)
        val beforeClose = walk.settledMeters
        walk.close()

        assertThat(walk.settledMeters).isAtLeast(beforeClose)
        assertThat(walk.settledMeters).isWithin(4.0).of(84.0)
    }

    @Test
    fun `a pause after a few seconds under an arcade keeps the steps the fixes never saw`() {
        val walk = Walk()
        walk.go(60, batch = 1)

        // Twelve seconds with no fix (not long enough to be a hole), then the pause.
        walk.go(12, fixes = false, batch = 1)
        walk.close()

        // 84 m measured, then 12 s of steps at 110 a minute and 0.75 m: 16.5 m.
        assertThat(walk.settledMeters).isWithin(4.0).of(84.0 + 16.5)
    }

    @Test
    fun `fixes that come after their steps were settled still measure the walk`() {
        val meter = RouteMeter()
        val walk = Walk(meter)

        // The screen on: each second's steps are settled through now, and the fix of that
        // second reaches the meter a moment later, the receiver's own delay.
        repeat(120) {
            walk.seconds++
            walk.north += WALK_SPEED
            meter.onSteps(walk.nowMillis - 1_000, walk.nowMillis, 2, 1_000, 1.5, 120)
            walk.take(meter.settle(walk.nowMillis))
            meter.onFix(walk.fixAt(walk.north), 120)
        }
        walk.close()

        // 168 m walked, measured: the steps alone (1.5 m a second) would say 180.
        assertThat(walk.settledMeters).isWithin(5.0).of(168.0)
        assertThat(walk.share).isAtLeast(0.9)
    }

    @Test
    fun `a stretch measured from end to end at a steady cadence says how long the step is`() {
        val walk = Walk()

        walk.go(180)
        walk.close()

        // 1.4 m/s at 110 steps a minute: a step of 76.4 cm, where the profile's said 75.
        val sample = walk.samples.single()
        assertThat(sample.cadence).isEqualTo(110)
        assertThat(sample.steps).isIn(300..335)
        assertThat(sample.meters / sample.steps).isWithin(0.02).of(1.4 / (110 / 60.0))
    }

    @Test
    fun `a change of cadence starts another sample`() {
        val walk = Walk()

        walk.go(120)
        walk.go(120, speed = 1.8, cadence = 128)
        walk.close()

        assertThat(walk.samples.map { it.cadence }).containsExactly(110, 128).inOrder()
        assertThat(walk.samples[1].meters / walk.samples[1].steps).isWithin(0.03).of(1.8 / (128 / 60.0))
    }

    @Test
    fun `under a minute teaches nothing`() {
        val walk = Walk()

        walk.go(40)
        walk.close()

        assertThat(walk.samples).isEmpty()
    }

    @Test
    fun `a stretch with a hole in it teaches nothing of the hole`() {
        val walk = Walk()

        walk.go(40)
        walk.go(60, fixes = false)
        walk.go(40)
        walk.close()

        // Neither side of the hole is a minute long, and the hole is the steps' own word.
        assertThat(walk.samples).isEmpty()
    }

    @Test
    fun `the pace now is the pace of the last two hundred metres`() {
        val walk = Walk()

        walk.go(100)
        assertThat(walk.meter.paceNow()).isNull()

        walk.go(200)
        // 1.4 m/s: eleven minutes fifty-four a kilometre.
        assertThat(walk.meter.paceNow()).isWithin(15.0).of(1_000 / 1.4)

        // Faster for the next three hundred metres: the pace follows, the first stretch forgotten.
        walk.go(170, speed = 1.8, cadence = 128)
        assertThat(walk.meter.paceNow()).isWithin(15.0).of(1_000 / 1.8)
    }

    @Test
    fun `a mock fix is not a walk in a release, and is in a debug build`() {
        val mock = Fix(atMillis = 1_000, latitude = LATITUDE, longitude = LONGITUDE, accuracyMeters = 3f, mock = true)

        assertThat(RouteMeter(allowMock = false).onFix(mock, 110)).isEqualTo(RouteFilter.Verdict.MOCK)
        assertThat(RouteMeter(allowMock = true).onFix(mock, 110)).isEqualTo(RouteFilter.Verdict.ACCEPTED)
    }

    private companion object {
        const val LATITUDE = 45.4642
        const val LONGITUDE = 9.1914
        const val WALK_SPEED = 1.4
        const val METERS_PER_DEGREE = 111_195.08

        /** A degree of longitude at Milan's latitude. */
        const val METERS_PER_DEGREE_EAST = 77_990.0
    }
}
