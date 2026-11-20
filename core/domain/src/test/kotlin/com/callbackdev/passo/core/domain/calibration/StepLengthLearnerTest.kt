package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.metrics.runningAt
import com.callbackdev.passo.core.domain.metrics.walkingAt
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepLengthSample
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.roundToInt

class StepLengthLearnerTest {
    // 1.78 m: a walking step of 73.9 cm estimated from the height.
    private val profile = Profile(heightMeters = 1.78)

    // Four weeks of a reader whose usual walking pace is 105 steps a minute.
    private val minutes = mapOf(60 to 200, 105 to 300, 120 to 100)

    private fun sample(cadence: Int, meters: Double, step: Double) =
        StepLengthSample(cadence = cadence, steps = (meters / step).roundToInt(), meters = meters)

    private fun outing(id: Long, vararg samples: StepLengthSample) = OutingSamples(id, id * 1_000, samples.toList())

    /** Two outings at the usual pace, 300 m each at a step of 80 cm: one bin that counts. */
    private val twoOutings = listOf(
        outing(1, sample(105, 300.0, 0.80)),
        outing(2, sample(104, 300.0, 0.80)),
    )

    @Test
    fun `one long outing only proposes nothing, a bin needs two outings`() {
        val learning = StepLengthLearner.learn(listOf(outing(1, sample(105, 3_000.0, 0.80))), minutes, profile, null)

        assertThat(learning.offer).isNull()
        assertThat(learning.proposed).isFalse()
        assertThat(StepLengthLearner.curve(listOf(outing(1, sample(105, 3_000.0, 0.80))), 105)).isNull()
    }

    @Test
    fun `a bin needs 400 metres in all`() {
        val short = listOf(outing(1, sample(105, 190.0, 0.80)), outing(2, sample(105, 190.0, 0.80)))

        assertThat(StepLengthLearner.curve(short, 105)).isNull()
        assertThat(StepLengthLearner.curve(twoOutings, 105)).isNotNull()
    }

    @Test
    fun `two outings at the usual pace make a proposal with its share of the distance`() {
        val learning = StepLengthLearner.learn(twoOutings, minutes, profile, null)

        val offer = requireNotNull(learning.offer)
        assertThat(learning.proposed).isTrue()
        assertThat(offer.lengthNow).isWithin(1e-3).of(0.80)
        assertThat(offer.lengthInUse).isWithin(1e-3).of(StepLengths.of(profile).walkingMeters)
        assertThat(offer.share).isWithin(1e-3).of(0.80 / StepLengths.of(profile).walkingMeters - 1)
        assertThat(offer.curve.learntMeters).isWithin(1e-9).of(600.0)
        assertThat(offer.curve.outings).isEqualTo(2)
    }

    @Test
    fun `samples only at a brisk cadence make a curve of one bin, flat up to 140, running keeps its own`() {
        val brisk = listOf(outing(1, sample(125, 300.0, 0.82)), outing(2, sample(126, 300.0, 0.82)))

        val curve = requireNotNull(StepLengthLearner.curve(brisk, 105))
        assertThat(curve.points).hasSize(1)
        assertThat(curve.walkingAt(85.0)).isWithin(1e-3).of(0.82)
        assertThat(curve.walkingAt(139.0)).isEqualTo(curve.walkingAt(85.0))
        assertThat(curve.runningAt(160.0)).isNull()
        // The usual pace, 105, is not in a bin that counts: nothing is offered.
        assertThat(StepLengthLearner.learn(brisk, minutes, profile, null).offer).isNull()
    }

    @Test
    fun `a bin whose median is shorter than a slower bin's is pooled, the curve never goes down`() {
        val samples = listOf(
            outing(1, sample(95, 300.0, 0.78), sample(115, 300.0, 0.74)),
            outing(2, sample(95, 300.0, 0.78), sample(115, 300.0, 0.74)),
        )

        val curve = requireNotNull(StepLengthLearner.curve(samples, 105))
        assertThat(curve.points).hasSize(2)
        assertThat(curve.points[0].meters).isAtMost(curve.points[1].meters)
        // Pooled by steps: the slower bin has more steps for the same metres.
        val slow = 600.0 / 0.78
        val fast = 600.0 / 0.74
        val pooled = (0.78 * slow + 0.74 * fast) / (slow + fast)
        assertThat(curve.points[0].meters).isWithin(1e-3).of(pooled)
        assertThat(curve.points[1].meters).isWithin(1e-3).of(pooled)
    }

    @Test
    fun `pooling weighs each value by its weight`() {
        assertThat(StepLengthLearner.nonDecreasing(listOf(1.0, 3.0, 2.0, 4.0), listOf(1.0, 1.0, 3.0, 1.0)))
            .containsExactly(1.0, 2.25, 2.25, 4.0)
            .inOrder()
    }

    @Test
    fun `a sample outside the profile limits is left out`() {
        // 300 m in 100 steps: a step of 3 m, a stretch where a fix was far off.
        val bad = StepLengthSample(cadence = 105, steps = 100, meters = 300.0)
        val samples = listOf(outing(1, sample(105, 300.0, 0.80), bad), outing(2, sample(105, 300.0, 0.80)))

        val curve = requireNotNull(StepLengthLearner.curve(samples, 105))
        assertThat(curve.points.single().meters).isWithin(1e-9).of(0.80)
        assertThat(curve.learntMeters).isWithin(1e-9).of(600.0)
    }

    @Test
    fun `a running sample is held to the running step's limits`() {
        // 1.8 m a step: no walker, a runner's long stride.
        val run = listOf(outing(1, sample(165, 600.0, 1.8)), outing(2, sample(165, 600.0, 1.8)))

        val curve = requireNotNull(StepLengthLearner.curve(run, 105))
        assertThat(curve.runningAt(165.0)).isWithin(0.01).of(1.8)
    }

    @Test
    fun `the running bins empty, running is not learnt and keeps its own length or the factor`() {
        val accepted = LearnedSteps.accept(profile, requireNotNull(StepLengthLearner.curve(twoOutings, 105)))
        assertThat(accepted.learnedStep?.running).isFalse()
        assertThat(StepLengths.of(accepted).forCadence(160)).isWithin(1e-9).of(0.80 * 1.3)

        val own = LearnedSteps.accept(profile.copy(runningStepLengthMeters = 1.1), accepted.learnedStep!!)
        assertThat(StepLengths.of(own).forCadence(160)).isWithin(1e-9).of(1.1)
    }

    @Test
    fun `the usual cadence is the median of the walking active minutes`() {
        assertThat(StepLengthLearner.usualCadence(minutes)).isEqualTo(105)
        // Minutes under 40 steps are not active, and runs are not a walking pace.
        assertThat(StepLengthLearner.usualCadence(mapOf(20 to 900, 100 to 3, 110 to 3, 160 to 400))).isEqualTo(105)
        assertThat(StepLengthLearner.usualCadence(mapOf(20 to 900))).isNull()
    }

    @Test
    fun `no active minute in four weeks, nothing to name a step at`() {
        assertThat(StepLengthLearner.learn(twoOutings, emptyMap(), profile, null)).isEqualTo(StepLearning.NOTHING)
    }

    @Test
    fun `a usual cadence under the first bin is read in it`() {
        val slow = listOf(outing(1, sample(84, 300.0, 0.66)), outing(2, sample(86, 300.0, 0.66)))
        val curve = requireNotNull(StepLengthLearner.curve(slow, 70))

        assertThat(StepLengthLearner.covers(curve, 70)).isTrue()
        assertThat(StepLengthLearner.covers(curve, 95)).isFalse()
    }

    @Test
    fun `a usual cadence between two bins that count is covered`() {
        val around = listOf(
            outing(1, sample(92, 300.0, 0.76), sample(118, 300.0, 0.82)),
            outing(2, sample(93, 300.0, 0.76), sample(117, 300.0, 0.82)),
        )
        val curve = requireNotNull(StepLengthLearner.curve(around, 105))

        assertThat(StepLengthLearner.covers(curve, 105)).isTrue()
        assertThat(curve.walkingAt(105.0)!!).isWithin(0.005).of(0.79)
    }

    @Test
    fun `a curve within 2 percent of the step in use is offered but not proposed`() {
        val near = listOf(outing(1, sample(105, 300.0, 0.745)), outing(2, sample(105, 300.0, 0.745)))

        val learning = StepLengthLearner.learn(near, minutes, profile, null)
        assertThat(learning.offer).isNotNull()
        assertThat(learning.proposed).isFalse()
        assertThat(learning.readyOutingId).isNull()
    }

    @Test
    fun `not now, then new outings, no card until the curve moves 2 percent from the one put away`() {
        val putAway = requireNotNull(StepLengthLearner.learn(twoOutings, minutes, profile, null).offer).curve
        // A third outing at the same step: the curve does not move.
        val same = twoOutings + outing(3, sample(105, 300.0, 0.80))
        val after = StepLengthLearner.learn(same, minutes, profile, putAway)
        assertThat(after.proposed).isFalse()
        // The calibration page's row stays: the curve still counts and is not in use.
        assertThat(after.offer).isNotNull()

        // Two outings at a longer step: the median moves past 2%.
        val moved = same + outing(4, sample(105, 300.0, 0.84)) + outing(5, sample(105, 300.0, 0.84)) +
            outing(6, sample(105, 300.0, 0.84)) + outing(7, sample(105, 300.0, 0.84))
        val again = StepLengthLearner.learn(moved, minutes, profile, putAway)
        assertThat(again.proposed).isTrue()
        assertThat(again.readyOutingId).isEqualTo(6)
    }

    @Test
    fun `not now, then a change of mind, the offer is today's curve`() {
        val putAway = requireNotNull(StepLengthLearner.learn(twoOutings, minutes, profile, null).offer).curve
        val more = twoOutings + outing(3, sample(105, 300.0, 0.81))

        val offer = requireNotNull(StepLengthLearner.learn(more, minutes, profile, putAway).offer)
        assertThat(offer.curve.outings).isEqualTo(3)
    }

    @Test
    fun `the outing that made the curve ready is the first with which it would have been proposed`() {
        val outings = listOf(
            outing(1, sample(105, 300.0, 0.80)),
            outing(2, sample(150, 300.0, 1.0)),
            outing(3, sample(105, 300.0, 0.80)),
            outing(4, sample(105, 300.0, 0.80)),
        )

        val learning = StepLengthLearner.learn(outings, minutes, profile, null)
        assertThat(learning.proposed).isTrue()
        assertThat(learning.readyOutingId).isEqualTo(3)
    }

    @Test
    fun `go back, a new proposal waits for a new 2 percent from the curve gone back from`() {
        val curve = requireNotNull(StepLengthLearner.learn(twoOutings, minutes, profile, null).offer).curve
        val accepted = LearnedSteps.accept(profile, curve)
        val gone = LearnedSteps.goBack(accepted)

        assertThat(gone).isEqualTo(profile.copy(stepLengthMode = StepLengthMode.AUTO))
        // The samples stay; put away with the curve gone back from, nothing is proposed.
        assertThat(StepLengthLearner.learn(twoOutings, minutes, gone, accepted.learnedStep).proposed).isFalse()
    }

    @Test
    fun `a curve in use is compared with itself, never proposed again unchanged`() {
        val curve = requireNotNull(StepLengthLearner.learn(twoOutings, minutes, profile, null).offer).curve
        val accepted = LearnedSteps.accept(profile, curve)

        val learning = StepLengthLearner.learn(twoOutings, minutes, accepted, null)
        assertThat(learning.proposed).isFalse()
        assertThat(requireNotNull(learning.offer).share).isWithin(1e-9).of(0.0)
    }

    @Test
    fun `an outing deleted takes its samples, the curve in use is not recomputed`() {
        val curve = requireNotNull(StepLengthLearner.learn(twoOutings, minutes, profile, null).offer).curve
        val accepted = LearnedSteps.accept(profile, curve)

        // One outing gone: the bin no longer counts, and nothing is offered; the profile keeps its curve.
        val learning = StepLengthLearner.learn(twoOutings.take(1), minutes, accepted, null)
        assertThat(learning.offer).isNull()
        assertThat(StepLengths.of(accepted).walkingMeters).isWithin(1e-9).of(0.80)
    }

    @Test
    fun `the curve remembers the latest outing it was learnt from`() {
        val curve = requireNotNull(StepLengthLearner.curve(twoOutings + outing(9, sample(60, 300.0, 0.6)), 105))

        // Outing 9 walked under the first bin: it taught nothing.
        assertThat(curve.throughMillis).isEqualTo(2_000)
    }
}
