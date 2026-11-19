package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.ACTIVE_MINUTE_THRESHOLD
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.ProfileLimits
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.metrics.usualMeters
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepLengthSample
import com.callbackdev.passo.core.model.StepPoint
import kotlin.math.abs
import kotlin.math.roundToInt

/** What one outing with GPS said of the step, with the outing it came from. */
data class OutingSamples(val sessionId: Long, val startedAtMillis: Long, val samples: List<StepLengthSample>)

/**
 * A learnt step offered to the reader: [curve], ready to be accepted, and what it would change.
 *
 * @property share how much longer (or, below zero, shorter) the last 28 days' distance reads with
 *   it than with the lengths in use: the calories move by the same share, being the distance's.
 * @property lengthNow the curve's step at the usual cadence, the figure the card names.
 * @property lengthInUse the walking step in use now, the one it would replace.
 */
data class StepOffer(val curve: LearnedStep, val share: Double, val lengthNow: Double, val lengthInUse: Double)

/**
 * What the outings with GPS have taught of the reader's step (PLANNING.md §11 Phase 17).
 *
 * @property offer the curve, once it counts at the reader's usual pace: the bin holding their usual
 *   walking cadence counts, or the two around it. What the calibration page's quiet row opens.
 * @property proposed the offer is put forward unasked (the card in Settings, the line on an
 *   outing's page): it moves the distances by 2% or more, and by 2% or more from the curve last
 *   proposed.
 * @property readyOutingId the outing whose samples made the proposal ready: the first, in the order
 *   they were walked, with which it would have been proposed. Null when nothing is proposed.
 */
data class StepLearning(val offer: StepOffer? = null, val proposed: Boolean = false, val readyOutingId: Long? = null) {
    companion object {
        val NOTHING = StepLearning()
    }
}

/**
 * The step that learns (docs/adr/0016-location.md decision 12): the samples of the outings with
 * GPS turned into a step length by cadence, and when it is worth proposing.
 *
 * The samples are binned by 10 steps a minute from 80 to 180; a bin counts with
 * [MIN_BIN_METERS] in all from [MIN_BIN_OUTINGS] outings or more, so one long outing, or one bad
 * day of satellites, never makes a step alone. A bin's step is the median of its samples', which
 * one stretch read wrong does not move; its cadence is their mean, weighted by their steps. The
 * curve is then made non-decreasing with the cadence (pooled, weighted by steps, where the data
 * says otherwise): a step does not shorten as the pace rises, and a curve that did would be
 * reading noise.
 */
object StepLengthLearner {
    /** The first bin's slowest cadence: below it a minute is mostly ambling, and a stretch at it rare. */
    const val FIRST_CADENCE: Int = 80

    /** Past the last bin, a cadence is a sprint no outing measures cleanly. */
    const val LAST_CADENCE: Int = 180

    /** Ten steps a minute: about 6 cm of step for most walkers, the finest the samples can tell. */
    const val BIN_WIDTH: Int = 10

    /**
     * 400 m: a lap of a track, some 550 steps. Under it, a few metres of GPS error at each end
     * of a stretch move the step by more than a centimetre.
     */
    const val MIN_BIN_METERS: Double = 400.0

    /** Two outings: one may have had a bad sky, or a pocket that swung differently. */
    const val MIN_BIN_OUTINGS: Int = 2

    /** Four weeks: every weekday four times over, a usual pace and not a week's. */
    const val USUAL_DAYS: Int = 28

    /**
     * 2%: a hundred metres in five kilometres. Less is under what a step's estimate can claim,
     * and a card for it would be noise.
     */
    const val PROPOSAL_SHARE: Double = 0.02

    /**
     * What the samples teach, for a reader with [profile] who walked [minutes] in the last
     * [USUAL_DAYS] days (each minute's step count to how many minutes had it), with [putAway] the
     * curve last proposed and put away or gone back from, if any.
     */
    fun learn(
        outings: List<OutingSamples>,
        minutes: Map<Int, Int>,
        profile: Profile,
        putAway: LearnedStep?,
    ): StepLearning {
        val usual = usualCadence(minutes) ?: return StepLearning.NOTHING
        val reference = if (profile.stepLengthMode == StepLengthMode.LEARNED) profile.learnedStep else putAway
        val offer = offer(outings, usual, minutes, profile) ?: return StepLearning.NOTHING
        if (!proposes(offer, reference, minutes, profile)) return StepLearning(offer)
        // The outings in the order they were walked, from the first after the curve last proposed:
        // the first whose samples, with those before it, would have made the proposal.
        val ordered = outings.sortedBy { it.startedAtMillis }
        val after = reference?.throughMillis ?: Long.MIN_VALUE
        val ready = ordered.withIndex().firstOrNull { (index, outing) ->
            outing.startedAtMillis > after &&
                outing.samples.isNotEmpty() &&
                offer(ordered.subList(0, index + 1), usual, minutes, profile)
                    ?.let { proposes(it, reference, minutes, profile) } == true
        }?.value?.sessionId
        return StepLearning(offer, proposed = true, readyOutingId = ready)
    }

    /**
     * The curve the samples make, read at [usualCadence]; null when no bin counts. Its way back
     * and its running part are set when it is accepted ([LearnedSteps.accept]).
     */
    fun curve(outings: List<OutingSamples>, usualCadence: Int): LearnedStep? {
        val bins = bins(outings).filter { it.counts }
        if (bins.isEmpty()) return null
        val lengths = nonDecreasing(bins.map { it.median }, bins.map { it.steps })
        val used = bins.flatMap { it.samples }
        return LearnedStep(
            points = bins.mapIndexed { index, bin -> StepPoint(bin.cadence, lengths[index]) },
            usualCadence = usualCadence,
            learntMeters = used.sumOf { it.sample.meters },
            outings = used.map { it.sessionId }.distinct().size,
            throughMillis = used.maxOf { it.startedAtMillis },
        )
    }

    /**
     * The reader's usual walking cadence: the median step count of their active minutes below the
     * running cadence (a run is not a pace to name a walking step at). Null with none.
     */
    fun usualCadence(minutes: Map<Int, Int>): Int? {
        val walking = minutes.filter { (steps, count) ->
            steps in ACTIVE_MINUTE_THRESHOLD until RUNNING_CADENCE &&
                count > 0
        }
            .toSortedMap()
        val total = walking.values.sum()
        if (total == 0) return null
        val lower = valueAt(walking, (total - 1) / 2)
        val upper = valueAt(walking, total / 2)
        return ((lower + upper) / 2.0).roundToInt()
    }

    /**
     * Whether [curve] says something at [usualCadence]: the bin holding it counts, or it falls
     * between two that do. A usual cadence under the first bin is read in it: the curve is flat
     * there, so the length named at it is that bin's.
     */
    fun covers(curve: LearnedStep, usualCadence: Int): Boolean {
        val cadence = usualCadence.coerceIn(FIRST_CADENCE, RUNNING_CADENCE - 1)
        val walking = curve.points.filter { it.cadence < RUNNING_CADENCE }
        if (walking.any { binOf(it.cadence) == binOf(cadence.toDouble()) }) return true
        return walking.any { it.cadence < cadence } && walking.any { it.cadence > cadence }
    }

    /** The distance [minutes] come to with [lengths]: what a share between two curves is taken on. */
    fun distance(minutes: Map<Int, Int>, lengths: StepLengths): Double =
        minutes.entries.sumOf { (steps, count) -> if (steps > 0) count * steps * lengths.forCadence(steps) else 0.0 }

    private fun offer(outings: List<OutingSamples>, usual: Int, minutes: Map<Int, Int>, profile: Profile): StepOffer? {
        val curve = curve(outings, usual)?.takeIf { covers(it, usual) } ?: return null
        val inUse = StepLengths.of(profile)
        val accepted = StepLengths.of(LearnedSteps.accept(profile, curve))
        val before = distance(minutes, inUse)
        if (before <= 0) return null
        val lengthNow = accepted.learned?.usualMeters ?: return null
        return StepOffer(curve, distance(minutes, accepted) / before - 1, lengthNow, inUse.walkingMeters)
    }

    private fun proposes(offer: StepOffer, reference: LearnedStep?, minutes: Map<Int, Int>, profile: Profile): Boolean {
        if (abs(offer.share) < PROPOSAL_SHARE) return false
        if (reference == null) return true
        val withOffer = distance(minutes, StepLengths.of(LearnedSteps.accept(profile, offer.curve)))
        val withReference = distance(minutes, StepLengths.of(LearnedSteps.accept(profile, reference)))
        return withReference > 0 && abs(withOffer / withReference - 1) >= PROPOSAL_SHARE
    }

    private class Kept(val sessionId: Long, val startedAtMillis: Long, val sample: StepLengthSample) {
        val length: Double get() = sample.meters / sample.steps
    }

    private class Bin(val samples: List<Kept>) {
        val steps: Double = samples.sumOf { it.sample.steps.toDouble() }
        val cadence: Double = samples.sumOf { it.sample.cadence.toDouble() * it.sample.steps } / steps
        val counts: Boolean = samples.sumOf { it.sample.meters } >= MIN_BIN_METERS &&
            samples.map { it.sessionId }.distinct().size >= MIN_BIN_OUTINGS
        val median: Double = samples.map { it.length }.sorted().let { (it[(it.size - 1) / 2] + it[it.size / 2]) / 2 }
    }

    /** The samples that say something, in their bins, slowest first. */
    private fun bins(outings: List<OutingSamples>): List<Bin> = outings
        .flatMap { outing -> outing.samples.map { Kept(outing.sessionId, outing.startedAtMillis, it) } }
        .filter { it.sample.steps > 0 && it.sample.meters.isFinite() && it.sample.meters > 0 }
        .filter { it.sample.cadence in FIRST_CADENCE until LAST_CADENCE }
        // A stretch whose step no one walks with is a bad stretch (a fix far off, a phone handed
        // over), not a step: left out, each by the limits of its own gait.
        .filter { it.length in ProfileLimits.stepLength(it.sample.cadence.toDouble()) }
        .groupBy { binOf(it.sample.cadence.toDouble()) }
        .toSortedMap()
        .values
        .map { Bin(it) }

    private fun binOf(cadence: Double): Int = ((cadence - FIRST_CADENCE) / BIN_WIDTH).toInt()

    /**
     * [values] made non-decreasing, each block that went down pooled into its mean weighted by
     * [weights] (pool adjacent violators): the closest non-decreasing curve to what was measured.
     */
    internal fun nonDecreasing(values: List<Double>, weights: List<Double>): List<Double> {
        val means = ArrayList<Double>()
        val masses = ArrayList<Double>()
        val sizes = ArrayList<Int>()
        values.forEachIndexed { index, value ->
            means += value
            masses += weights[index]
            sizes += 1
            while (means.size > 1 && means[means.size - 2] > means[means.size - 1]) {
                val last = means.size - 1
                val mass = masses[last - 1] + masses[last]
                means[last - 1] = (means[last - 1] * masses[last - 1] + means[last] * masses[last]) / mass
                masses[last - 1] = mass
                sizes[last - 1] += sizes[last]
                means.removeAt(last)
                masses.removeAt(last)
                sizes.removeAt(last)
            }
        }
        return means.flatMapIndexed { index, mean -> List(sizes[index]) { mean } }
    }

    private fun valueAt(histogram: Map<Int, Int>, position: Int): Int {
        var seen = 0
        for ((steps, count) in histogram) {
            seen += count
            if (position < seen) return steps
        }
        return histogram.keys.last()
    }
}
