package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.routes.RouteConstants.ACCURACY_GRAIN
import com.callbackdev.passo.core.domain.routes.RouteConstants.CARRIED_SLACK_METERS
import com.callbackdev.passo.core.domain.routes.RouteConstants.HOLE_MILLIS
import com.callbackdev.passo.core.domain.routes.RouteConstants.MAX_METERS_PER_STEP
import com.callbackdev.passo.core.domain.routes.RouteConstants.MIN_MOVE_METERS
import com.callbackdev.passo.core.domain.routes.RouteConstants.MIN_SLOW_MOVE_METERS
import com.callbackdev.passo.core.domain.routes.RouteConstants.PACE_METERS
import com.callbackdev.passo.core.domain.routes.RouteConstants.RESEED_FIXES
import com.callbackdev.passo.core.domain.routes.RouteConstants.SAMPLE_CADENCE_SPAN
import com.callbackdev.passo.core.domain.routes.RouteConstants.SAMPLE_MIN_MOVING_MILLIS
import com.callbackdev.passo.core.domain.routes.RouteConstants.SLOW_POINT_MILLIS
import com.callbackdev.passo.core.domain.routes.RouteConstants.STEP_DELAY_MILLIS
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.callbackdev.passo.core.model.StepLengthSample
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * A stretch of an outing as the meter settled it.
 *
 * @property measured it came from fixes; otherwise from the steps (before the first fix, in a
 *   hole, or where what the fixes said was not walked).
 * @property cadence the steps a minute it was walked at, for the energy it cost.
 */
data class RouteStretch(val meters: Double, val measured: Boolean, val cadence: Int?)

/**
 * What a call to the meter settled: distance, the points worth keeping, what was learnt of the
 * step, and the same distance batch by batch with the steps and the time in motion it took
 * ([pieces]), from which an outing's legs are laid.
 */
data class RouteSettlement(
    val stretches: List<RouteStretch> = emptyList(),
    val points: List<RoutePoint> = emptyList(),
    val samples: List<StepLengthSample> = emptyList(),
    val pieces: List<LegPiece> = emptyList(),
) {
    val meters: Double get() = stretches.sumOf { it.meters }

    val measuredMeters: Double get() = stretches.sumOf { if (it.measured) it.meters else 0.0 }

    val isEmpty: Boolean get() = stretches.isEmpty() && points.isEmpty() && samples.isEmpty() && pieces.isEmpty()
}

/**
 * The steps keep the GPS honest, and the GPS the steps (docs/adr/0016-location.md, decision 5):
 * the distance of an outing measured with GPS, fused from the accepted fixes and the accounted
 * steps. Pure: the tracker feeds it both, each with its own time on the one clock they share,
 * in any interleaving, and asks it to settle what both have covered.
 *
 * The steps say *when* the reader walks, the fixes *how far*:
 *
 * - **The walk does not wait for the satellites.** Steps before the first fix are the steps'.
 * - **Still means still.** A fix that falls outside a batch's time in motion moves the line's
 *   end and adds no distance: the wander of a phone on a bench is not a walk. And steps that go
 *   nowhere (marching on the spot) are measured as what they are: the fixes say so.
 * - **A bus is not a walk.** What the fixes say beyond what the steps taken could cover
 *   ([MAX_METERS_PER_STEP], [CARRIED_SLACK_METERS]) is not counted: those steps keep their own
 *   estimate, and the line starts anew where the reader walks again.
 * - **No lost metres.** With no accepted fix for [HOLE_MILLIS] while steps come, the stretch is
 *   the steps', counted as it grows; when a fix comes back it is made at least the straight line
 *   across the hole, and drawn as laid from the steps.
 * - **The line moves by a grain.** Its end moves on when the reader is [MIN_MOVE_METERS] from
 *   it, more where the receiver is less sure ([ACCURACY_GRAIN]), or has moved at all after
 *   [SLOW_POINT_MILLIS]: the receiver's second-to-second wander is not added up as walked.
 *   Those ends are the points kept.
 *
 * **Steps ahead of their fixes.** The two streams arrive in batches of their own. A batch of
 * steps is held *open* until the fixes are known to be in through its end ([settle]); only then
 * is it measured. Nothing is told, and no outing ended, on metres the fixes could take back, and
 * what is settled never goes down.
 *
 * Not thread-safe: the tracker drives it from one thread.
 *
 * @param allowMock a debug build: a route can be replayed on a desk with the system's
 *   mock-location setting. Never in a release.
 */
class RouteMeter(private val allowMock: Boolean = false) {
    private class Batch(
        val fromMillis: Long,
        val toMillis: Long,
        val steps: Int,
        val movingMillis: Long,
        val stepsMeters: Double,
        val cadence: Int?,
    )

    private val fixes = ArrayDeque<Fix>()
    private val open = ArrayDeque<Batch>()

    // For the filter: the last fix accepted, and the last one that was accurate and in order
    // whatever its verdict, with how many in a row agreed with the one before them.
    private var lastAccepted: Fix? = null
    private var lastHeard: Fix? = null
    private var agreeing = 0

    // The line: its end, the last point kept, and the last fix it has seen.
    private var anchor: Fix? = null
    private var lastKept: Fix? = null
    private var lastFix: Fix? = null
    private var settledThroughMillis = Long.MIN_VALUE

    // The steps walked since the line's end last moved, and what they estimate: provisional,
    // until a fix measures them or a hole makes them the steps' for good. [carryCounted] is the
    // part already settled as the steps', while a hole lasts.
    private var carrySteps = 0.0
    private var carryMeters = 0.0
    private var carryCounted = 0.0

    // A run of batches measured from end to end at a steady cadence: a step-length sample.
    private var cleanSteps = 0.0
    private var cleanMeters = 0.0
    private var cleanMovingMillis = 0L
    private var cleanCadenceSteps = 0.0
    private var cleanSlowest = 0
    private var cleanFastest = 0

    // The latest settled batches, back to [PACE_METERS]: the pace now.
    private val recent = ArrayDeque<Pair<Double, Long>>()
    private var recentMeters = 0.0
    private var recentMovingMillis = 0L

    /** How many fixes were accepted and refused, for the outing's log row. */
    var taken: Int = 0
        private set
    var refused: Int = 0
        private set

    /** What the steps estimate for the batches still open: shown, never told on. */
    val openMeters: Double get() = open.sumOf { it.stepsMeters }

    /** The last fix accepted since the line began; null before the first, and after the line is cut. */
    val lastFixAtMillis: Long? get() = lastAccepted?.atMillis

    /** The end of the last batch of steps settled, its fixes known to be in; null before the first. */
    val settledThrough: Long? get() = settledThroughMillis.takeIf { it != Long.MIN_VALUE }

    /**
     * A fix from the receiver, at the reader's [cadence] now. Refused ones are only counted.
     *
     * A fix is judged against the last one accepted, so one bad fix never bends the line. But a
     * reader set down by a bus is far from the last fix accepted, and every good fix after it
     * would read as too fast from there for minutes. So once [RESEED_FIXES] fixes in a row each
     * agree with the one before, the receiver is believed: the reader is where it says, however
     * they got there. Whether that stretch was walked is the steps' to say, not the filter's.
     */
    fun onFix(fix: Fix, cadence: Int?): RouteFilter.Verdict {
        var verdict = RouteFilter.judge(fix, lastAccepted, cadence, allowMock)
        if (verdict == RouteFilter.Verdict.TOO_FAST) {
            val agrees = RouteFilter.judge(fix, lastHeard, cadence, allowMock) == RouteFilter.Verdict.ACCEPTED
            agreeing = if (agrees) agreeing + 1 else 0
            if (agreeing >= RESEED_FIXES) verdict = RouteFilter.Verdict.ACCEPTED
        }
        if (verdict == RouteFilter.Verdict.ACCEPTED || verdict == RouteFilter.Verdict.TOO_FAST) lastHeard = fix
        if (verdict == RouteFilter.Verdict.ACCEPTED) {
            fixes.addLast(fix)
            lastAccepted = fix
            agreeing = 0
            taken++
        } else {
            refused++
        }
        return verdict
    }

    /**
     * A batch of accounted steps: [steps] taken between [fromMillis] and [toMillis], of which
     * [movingMillis] in motion (the tracker's own measure), estimated at [stepsMeters] by the
     * step length. Held open until [settle] knows the fixes are in through its end.
     */
    fun onSteps(fromMillis: Long, toMillis: Long, steps: Int, movingMillis: Long, stepsMeters: Double, cadence: Int?) {
        if (steps <= 0) return
        open.addLast(Batch(minOf(fromMillis, toMillis), toMillis, steps, movingMillis, stepsMeters, cadence))
    }

    /**
     * Settles every open batch that ends at or before [fixesThroughMillis], the moment through
     * which the receiver's fixes are known to be in: a flush it completed, or, where it does not
     * batch, now.
     */
    fun settle(fixesThroughMillis: Long): RouteSettlement {
        val out = Out()
        while (open.isNotEmpty() && open.first().toMillis <= fixesThroughMillis) settle(open.removeFirst(), out)
        return out.settlement()
    }

    /**
     * Ends the stretch under way: at a pause, at the outing's end. Every open batch is settled
     * with the fixes in hand, the steps since the line's end last moved are counted (no lost
     * metres), and the line is cut: what comes next starts a new one.
     */
    fun close(): RouteSettlement {
        val out = Out()
        while (open.isNotEmpty()) settle(open.removeFirst(), out)
        val end = anchor
        val seen = lastFix
        val before = out.meters
        if (end == null || seen == null || carryCounted > 0 || settledThroughMillis - seen.atMillis > HOLE_MILLIS) {
            // No fix covered them: the steps'.
            out.add(carryMeters - carryCounted, measured = false, cadence = null)
        } else if (carrySteps > 0) {
            // The fixes covered them and the line's end did not move a grain: what it did move.
            out.add(minOf(Geodesy.meters(end, seen), carryMeters), measured = true, cadence = null)
        }
        // The last metres, whose steps and time their batches have already given.
        if (out.meters > before) {
            out.pieces += LegPiece(out.meters - before, 0.0, 0L, settledThroughMillis, settledThroughMillis)
        }
        endSample(out)
        fixes.clear()
        lastAccepted = null
        lastHeard = null
        agreeing = 0
        anchor = null
        lastKept = null
        lastFix = null
        clearCarry()
        return out.settlement()
    }

    /**
     * The pace of the last [PACE_METERS], in seconds in motion a kilometre; null before that
     * much has been walked.
     */
    fun paceNow(): Double? = Pace.secondsPerKm(recentMovingMillis, recentMeters)

    private fun settle(batch: Batch, out: Out) {
        // The part of the batch the reader walked in: its time in motion, counted back from its
        // end, and the counter's own delay before it.
        val motionFrom = maxOf(batch.fromMillis, batch.toMillis - batch.movingMillis - STEP_DELAY_MILLIS)
        val span = (batch.toMillis - motionFrom).coerceAtLeast(1L).toDouble()
        val metersBefore = out.meters
        val unmeasuredBefore = out.unmeasured
        var cursor = motionFrom
        var walked = 0.0
        var walkedSteps = 0.0
        var walkedEstimate = 0.0
        val pending = ArrayList<RoutePoint>()

        while (fixes.isNotEmpty() && fixes.first().atMillis <= batch.toMillis) {
            val fix = fixes.removeFirst()
            val walking = fix.atMillis > motionFrom
            if (walking) {
                // The steps are laid evenly over the batch's time in motion, as the tracker lays them.
                val share = (fix.atMillis - cursor).coerceAtLeast(0L) / span
                carrySteps += batch.steps * share
                carryMeters += batch.stepsMeters * share
                cursor = maxOf(cursor, fix.atMillis)
            }
            val end = anchor
            val seen = lastFix
            when {
                // The first fix of a stretch: what was walked before it is the steps'.
                end == null -> {
                    out.add(carryMeters - carryCounted, measured = false, batch.cadence)
                    place(fix, RoutePointKind.START, out.points)
                }

                // The far side of a hole the reader walked through: no fix for long, or steps
                // already settled as the steps' while it lasted (never to be measured again).
                carrySteps > 0 && (carryCounted > 0 || beyondHole(seen, fix)) -> {
                    val line = Geodesy.meters(end, fix)
                    if (line <= walkable(carrySteps)) {
                        out.add(maxOf(line, carryMeters) - carryCounted, measured = false, batch.cadence)
                        carried(end)?.let { out.points += it }
                        place(fix, RoutePointKind.LAID, out.points)
                    } else {
                        // Farther than those steps could take them: carried part of the way.
                        out.add(carryMeters - carryCounted, measured = false, batch.cadence)
                        place(fix, RoutePointKind.START, out.points)
                    }
                }

                // Not walking: the line's end follows the phone, and nothing is added.
                !walking -> anchor = fix

                else -> {
                    val moved = Geodesy.meters(end, fix)
                    val grain = maxOf(MIN_MOVE_METERS, ((fix.accuracyMeters ?: 0f) * ACCURACY_GRAIN).toDouble())
                    // A slow reader's point: after the time a stroll takes to cross the grain,
                    // if they moved a third of it. Both grow with the grain, or a vaguer receiver
                    // would be given finer points by this door than by the grain's own.
                    val coarser = grain / MIN_MOVE_METERS
                    val slow = fix.atMillis - end.atMillis >= SLOW_POINT_MILLIS * coarser &&
                        moved >= MIN_SLOW_MOVE_METERS * coarser
                    // As far as the steps since the line's end could reach, and the two fixes' own
                    // radii: beyond it, it is the receiver that moved (a reflection off a wall
                    // that lasted), not the reader.
                    val reach =
                        carrySteps * MAX_METERS_PER_STEP + (fix.accuracyMeters ?: 0f) + (end.accuracyMeters ?: 0f)
                    if (moved > reach) {
                        out.add(carryMeters - carryCounted, measured = false, batch.cadence)
                        place(fix, RoutePointKind.START, out.points)
                    } else if (moved >= grain || slow) {
                        walked += moved
                        walkedSteps += carrySteps
                        walkedEstimate += carryMeters
                        carried(end)?.let { pending += it }
                        anchor = fix
                        lastKept = fix
                        pending += point(fix, RoutePointKind.MEASURED)
                        clearCarry()
                    }
                }
            }
            lastFix = fix
        }

        // The steps after the last fix of the batch: measured by the next fix, or a hole's.
        val tail = (batch.toMillis - cursor).coerceAtLeast(0L) / span
        carrySteps += batch.steps * tail
        carryMeters += batch.stepsMeters * tail
        settledThroughMillis = maxOf(settledThroughMillis, batch.toMillis)

        var clean = walked > 0
        if (walked > 0) {
            if (walked <= walkable(walkedSteps)) {
                out.add(walked, measured = true, batch.cadence)
                out.points += pending
            } else {
                // The fixes say more than those steps could cover: the reader was carried. The
                // steps keep their own estimate, and the line starts anew where they are now.
                out.add(walkedEstimate, measured = false, batch.cadence)
                anchor?.let { out.points += point(it, RoutePointKind.START) }
            }
        }
        // A hole: no fix for long while the reader walks. The steps' estimate counts as it grows.
        val seen = lastFix
        if (anchor == null || seen == null || batch.toMillis - seen.atMillis > HOLE_MILLIS) {
            out.add(carryMeters - carryCounted, measured = false, batch.cadence)
            carryCounted = carryMeters
        }
        if (out.unmeasured > unmeasuredBefore) clean = false

        sample(batch, walked, walkedSteps, clean, out)
        remember(out.meters - metersBefore, batch.movingMillis)
        out.pieces += LegPiece(
            meters = out.meters - metersBefore,
            steps = batch.steps.toDouble(),
            movingMillis = batch.movingMillis,
            fromMillis = motionFrom,
            toMillis = batch.toMillis,
        )
    }

    /** Whether [fix] comes after too long with no fix for the stretch to it to count as measured. */
    private fun beyondHole(seen: Fix?, fix: Fix): Boolean = seen != null && fix.atMillis - seen.atMillis > HOLE_MILLIS

    /** Moves the line's end to [fix], keeping it as a point: what was provisional is settled. */
    private fun place(fix: Fix, kind: RoutePointKind, points: MutableList<RoutePoint>) {
        anchor = fix
        lastKept = fix
        points += point(fix, kind)
        clearCarry()
    }

    /**
     * The line's end, as the start of a new stretch, when it was carried there: it followed the
     * phone with no step, farther from the last point kept than a still phone wanders. Without
     * it the line would be drawn across the ride. Null when the end is where the line left it.
     */
    private fun carried(end: Fix): RoutePoint? {
        val kept = lastKept ?: return null
        if (end === kept || Geodesy.meters(kept, end) <= CARRIED_SLACK_METERS) return null
        return point(end, RoutePointKind.START)
    }

    private fun clearCarry() {
        carrySteps = 0.0
        carryMeters = 0.0
        carryCounted = 0.0
    }

    /** The farthest so many steps can have taken the reader, with the room two fixes may add. */
    private fun walkable(steps: Double): Double = steps * MAX_METERS_PER_STEP + CARRIED_SLACK_METERS

    private fun sample(batch: Batch, walked: Double, walkedSteps: Double, clean: Boolean, out: Out) {
        val cadence = batch.cadence
        if (!clean || cadence == null) {
            endSample(out)
            return
        }
        val steady = cleanSteps == 0.0 ||
            maxOf(cleanFastest, cadence) - minOf(cleanSlowest, cadence) <= SAMPLE_CADENCE_SPAN
        if (!steady) endSample(out)
        if (cleanSteps == 0.0) {
            cleanSlowest = cadence
            cleanFastest = cadence
        } else {
            cleanSlowest = minOf(cleanSlowest, cadence)
            cleanFastest = maxOf(cleanFastest, cadence)
        }
        cleanSteps += walkedSteps
        cleanMeters += walked
        cleanMovingMillis += batch.movingMillis
        cleanCadenceSteps += cadence * walkedSteps
    }

    /** Closes the clean run under way, and keeps it as a sample if it was long enough to teach. */
    private fun endSample(out: Out) {
        val steps = cleanSteps.roundToInt()
        if (cleanMovingMillis >= SAMPLE_MIN_MOVING_MILLIS && steps > 0) {
            out.samples += StepLengthSample(
                cadence = (cleanCadenceSteps / cleanSteps).roundToInt(),
                steps = steps,
                meters = cleanMeters,
            )
        }
        cleanSteps = 0.0
        cleanMeters = 0.0
        cleanMovingMillis = 0L
        cleanCadenceSteps = 0.0
    }

    private fun remember(meters: Double, movingMillis: Long) {
        if (meters <= 0 && movingMillis <= 0) return
        recent.addLast(meters to movingMillis)
        recentMeters += meters
        recentMovingMillis += movingMillis
        while (recent.size > 1 && recentMeters - recent.first().first >= PACE_METERS) {
            val (oldMeters, oldMillis) = recent.removeFirst()
            recentMeters -= oldMeters
            recentMovingMillis -= oldMillis
        }
    }

    private fun point(fix: Fix, kind: RoutePointKind) = RoutePoint(
        epochSecond = Math.floorDiv(fix.atMillis, MILLIS_PER_SECOND),
        latitudeE6 = (fix.latitude * MICRO).roundToLong().toInt(),
        longitudeE6 = (fix.longitude * MICRO).roundToLong().toInt(),
        altitudeDm = fix.altitudeMeters?.let { (it * DECIMETERS).roundToInt() },
        accuracyMeters = fix.accuracyMeters?.roundToInt() ?: 0,
        kind = kind,
    )

    private class Out {
        val stretches = ArrayList<RouteStretch>()
        val points = ArrayList<RoutePoint>()
        val samples = ArrayList<StepLengthSample>()
        val pieces = ArrayList<LegPiece>()
        var meters = 0.0
        var unmeasured = 0.0

        fun add(meters: Double, measured: Boolean, cadence: Int?) {
            if (meters <= NEGLIGIBLE_METERS) return
            stretches += RouteStretch(meters, measured, cadence)
            this.meters += meters
            if (!measured) unmeasured += meters
        }

        fun settlement() = RouteSettlement(stretches, points, samples, pieces)
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val MICRO = 1_000_000.0
        const val DECIMETERS = 10.0

        /** Under a millimetre: the dust of sharing a batch's steps by time, not a stretch. */
        const val NEGLIGIBLE_METERS = 0.001
    }
}
