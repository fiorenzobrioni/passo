package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import kotlin.math.roundToInt

/**
 * Where an interval outing stands at a time in motion (Phase 13).
 *
 * @property index the interval from 0: slow when even, fast when odd.
 * @property set the set it belongs to, from 1.
 * @property elapsedMillis how far into it, in time in motion.
 * @property leftMillis how much of it is left, in time in motion.
 */
data class IntervalPosition(
    val index: Int,
    val fast: Boolean,
    val set: Int,
    val sets: Int,
    val elapsedMillis: Long,
    val leftMillis: Long,
) {
    /** The last set: its fast interval is the last one, and its end the goal. */
    val lastSet: Boolean get() = set == sets
}

/**
 * Where each change of an interval outing falls (Phase 13, docs/adr/0013-interval-walks.md), on
 * the clock of time in motion: slow first, as the protocol does, then fast, [IntervalSets.sets]
 * times. A change is the start of every interval after the first; the end of the last one is the
 * goal, never a change ("slower" there would be a signal for nothing).
 */
class IntervalSchedule(val sets: IntervalSets) {
    /** How many intervals: two a set. */
    val count: Int = sets.sets * 2

    /** The whole outing, in time in motion. */
    val totalMillis: Long = sets.totalMinutes * MILLIS_PER_MINUTE

    fun isFast(index: Int): Boolean = index % 2 == 1

    fun lengthOf(index: Int): Long = (if (isFast(index)) sets.fastMinutes else sets.slowMinutes) * MILLIS_PER_MINUTE

    /** Where interval [index] begins, in time in motion. */
    fun startOf(index: Int): Long {
        val set = index / 2
        val inSet = if (isFast(index)) sets.slowMinutes * MILLIS_PER_MINUTE else 0L
        return set * (sets.slowMinutes + sets.fastMinutes) * MILLIS_PER_MINUTE + inSet
    }

    fun endOf(index: Int): Long = startOf(index) + lengthOf(index)

    /** The interval under way at [movingMillis]: a change belongs to the interval it begins. */
    fun indexAt(movingMillis: Long): Int {
        if (movingMillis <= 0) return 0
        if (movingMillis >= totalMillis) return count - 1
        val setLength = (sets.slowMinutes + sets.fastMinutes) * MILLIS_PER_MINUTE
        val set = (movingMillis / setLength).toInt()
        val inSet = movingMillis - set * setLength
        return set * 2 + if (inSet >= sets.slowMinutes * MILLIS_PER_MINUTE) 1 else 0
    }

    fun at(movingMillis: Long): IntervalPosition {
        val index = indexAt(movingMillis)
        val clamped = movingMillis.coerceIn(0, totalMillis)
        return IntervalPosition(
            index = index,
            fast = isFast(index),
            set = index / 2 + 1,
            sets = sets.sets,
            elapsedMillis = clamped - startOf(index),
            leftMillis = endOf(index) - clamped,
        )
    }

    /** The intervals whose start lies in ([fromMillis], [toMillis]]: the changes crossed, in order. */
    fun changesBetween(fromMillis: Long, toMillis: Long): List<Int> =
        (1 until count).filter { startOf(it) in (fromMillis + 1)..toMillis }

    /**
     * The time in motion until the next change after [movingMillis]; null from the start of the
     * last interval on, where what comes next is the goal.
     */
    fun untilChange(movingMillis: Long): Long? {
        val next = indexAt(movingMillis) + 1
        if (next >= count) return null
        return startOf(next) - movingMillis
    }

    /** The time in motion until the next moment worth telling on time: a change, or the goal. */
    fun untilSignal(movingMillis: Long): Long? =
        untilChange(movingMillis) ?: (totalMillis - movingMillis).takeIf { it > 0 }

    /**
     * [splits] with [added] laid on them from [fromMillis] of time in motion: its time cut at
     * every change it crosses, its steps and its time at pace shared as its time is. Nothing past
     * the end of the last interval: the minutes after the goal ("Keep going") are the outing's,
     * not a set's. Time at pace is kept in every interval, and counts for the fast ones.
     */
    fun lay(splits: List<IntervalSplit>, fromMillis: Long, added: SessionTotals): List<IntervalSplit> {
        if (added.steps <= 0 && added.movingMillis <= 0) return splits
        if (fromMillis >= totalMillis) return splits
        val byIndex = splits.associateBy { it.index }.toMutableMap()
        if (added.movingMillis <= 0) {
            // Steps with no time of their own (the outing's first): the interval they fall in.
            val index = indexAt(fromMillis)
            val split = byIndex[index] ?: IntervalSplit(index, isFast(index))
            byIndex[index] = split.copy(steps = split.steps + added.steps)
            return byIndex.values.sortedBy { it.index }
        }
        val to = minOf(fromMillis + added.movingMillis, totalMillis)
        var at = fromMillis
        var stepsLaid = 0
        while (at < to) {
            val index = indexAt(at)
            val end = minOf(endOf(index), to)
            val part = end - at
            val share = (end - fromMillis).toDouble() / added.movingMillis
            // Shared by the time walked so far, so the parts add up to the whole.
            val steps = (added.steps * share).roundToInt() - stepsLaid
            stepsLaid += steps
            val split = byIndex[index] ?: IntervalSplit(index, isFast(index))
            byIndex[index] = split.copy(
                steps = split.steps + steps,
                movingMillis = split.movingMillis + part,
                zoneMillis = split.zoneMillis + (added.zoneMillis * part.toDouble() / added.movingMillis).toLong(),
            )
            at = end
        }
        return byIndex.values.sortedBy { it.index }
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L

        /** The schedule of [session], if it is an interval outing. */
        fun of(session: Session): IntervalSchedule? =
            session.intervals?.takeIf { session.goalKind == SessionGoalKind.INTERVALS }?.let(::IntervalSchedule)
    }
}

/**
 * The interval [this] stands in at [nowMillis], for the countdown on a screen: the time in motion
 * of its last step, carried on for up to [SessionConstants.COUNTDOWN_GLIDE_MILLIS] while it is
 * walking (the counter delivers steps in clusters, and a countdown that waits for each would
 * stutter), never past the change still to be told. Null for any other outing.
 */
fun Session.intervalAt(nowMillis: Long): IntervalPosition? {
    val schedule = IntervalSchedule.of(this) ?: return null
    var moving = totals.movingMillis
    if (state == SessionState.ACTIVE) {
        val glide = (nowMillis - lastStepAtMillis).coerceIn(0, SessionConstants.COUNTDOWN_GLIDE_MILLIS)
        val boundary = schedule.untilSignal(moving)?.let { moving + it } ?: moving
        moving = minOf(moving + glide, boundary)
    }
    val position = schedule.at(moving)
    // On the boundary itself the change is not told yet: still the last moment of the interval.
    if (moving > totals.movingMillis && position.elapsedMillis == 0L && position.index > 0) {
        return schedule.at(moving - 1).copy(leftMillis = 0)
    }
    return position
}

/** The cadence an interval was walked at: its steps over its time in motion; null before a minute's worth. */
fun IntervalSplit.cadence(): Int? = if (movingMillis < MIN_CADENCE_MILLIS) {
    null
} else {
    (steps * MILLIS_PER_MINUTE_D / movingMillis).roundToInt()
}

/**
 * How an interval outing's fast intervals went (Phase 13): [atPace] of [judged] at the fast pace
 * or above, a fast interval being at pace when its own cadence is. Judged are the fast intervals
 * walked for at least half their length: the last one of an outing stopped a few seconds into it
 * says nothing either way.
 */
data class IntervalResult(val atPace: Int, val judged: Int, val sets: Int)

fun Session.intervalResult(): IntervalResult? {
    val schedule = IntervalSchedule.of(this) ?: return null
    val floor = intensity.cadenceFloor ?: return null
    val judged = splits.filter { it.fast && it.movingMillis * 2 >= schedule.lengthOf(it.index) }
    return IntervalResult(
        atPace = judged.count { (it.cadence() ?: 0) >= floor },
        judged = judged.size,
        sets = schedule.sets.sets,
    )
}

private const val MIN_CADENCE_MILLIS = 30_000L
private const val MILLIS_PER_MINUTE_D = 60_000.0
