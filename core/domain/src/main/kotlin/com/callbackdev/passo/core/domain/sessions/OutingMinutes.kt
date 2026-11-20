package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionState

/**
 * One bar of an outing's strip: a few of its minutes, side by side.
 *
 * @property from its first minute, counted from the outing's first.
 * @property minutes how many minutes it holds: the group's, or fewer for the last, still filling.
 * @property stepsPerMinute its height: the mean steps a minute of its minutes not paused.
 * @property atPace at least half of those minutes are at or above the outing's cadence floor.
 * @property paused most of it was a pause: a gap, drawn as nothing.
 */
data class MinuteBar(
    val from: Int,
    val minutes: Int,
    val stepsPerMinute: Double,
    val atPace: Boolean,
    val paused: Boolean,
)

/**
 * An outing minute by minute (PLANNING.md §11 Phase 21, step 21.3): the steps of each minute
 * from its first to its last, or to now while it counts, with its pauses as gaps. What its strip
 * draws, on its card and on its page. Pure: the minutes come from `minute_steps` and the pauses
 * from the outing, both read by the caller.
 *
 * A minute's steps are the day's, as `minute_steps` keeps them: the first and the last minute
 * may hold a few steps from just before or just after, which at a minute's resolution is the
 * honest reading. A minute in a pause holds none: its steps were the day's, not the outing's.
 *
 * @property steps each minute's steps, from the outing's first minute; 0 in a pause.
 * @property paused whether each minute was mostly a pause.
 * @property floor the cadence a minute is «at the pace» from: the outing's own, or brisk walking
 *   (100 steps a minute, the day's brisk minute) for an outing at any pace.
 * @property goalAt where the goal was reached, in minutes from the first minute's start; null
 *   for an outing that has not reached one, or has none.
 * @property live it still counts: its strip keeps room to grow into ([spanMinutes]).
 * @property pauses how many pauses it had, the one under way included.
 */
data class OutingMinutes(
    val steps: List<Int>,
    val paused: List<Boolean>,
    val floor: Int,
    val goalAt: Double? = null,
    val live: Boolean = false,
    val pauses: Int = 0,
) {
    val count: Int get() = steps.size

    /**
     * The minutes the strip is wide for: the outing's own, and never fewer than
     * [MIN_LIVE_SPAN_MINUTES] while it counts (the bars fill it from the left, as it is walked) or
     * [MIN_SPAN_MINUTES] once over (a ten-minute outing is not drawn as ten pillars).
     */
    val spanMinutes: Int get() = maxOf(count, if (live) MIN_LIVE_SPAN_MINUTES else MIN_SPAN_MINUTES)

    /** The minutes not paused at or above [floor]: «30 of them brisk». */
    val atPaceMinutes: Int get() = steps.indices.count { !paused[it] && steps[it] >= floor }

    /**
     * The minutes a bar holds, so that no more than [maxBars] fit the strip's span: one, two,
     * five, ten or fifteen, the smallest that fits; fifteen past that, a day and a night of walking.
     */
    fun groupFor(maxBars: Int): Int = GROUPS.firstOrNull { ceilDiv(spanMinutes, it) <= maxBars.coerceAtLeast(1) }
        ?: GROUPS.last()

    /** The bars of a strip that holds at most [maxBars], in order, the last one still filling. */
    fun bars(maxBars: Int): List<MinuteBar> {
        val group = groupFor(maxBars)
        return (0 until count step group).map { from ->
            val minutes = minOf(group, count - from)
            val walked = (from until from + minutes).filterNot { paused[it] }
            val atPace = walked.count { steps[it] >= floor }
            MinuteBar(
                from = from,
                minutes = minutes,
                stepsPerMinute = if (walked.isEmpty()) 0.0 else walked.sumOf { steps[it] }.toDouble() / walked.size,
                atPace = walked.isNotEmpty() && atPace * 2 >= walked.size,
                paused = (minutes - walked.size) * 2 > minutes,
            )
        }
    }

    /**
     * The height the strip's top stands for: the tallest bar, and never under a little above the
     * floor, so a bar at the pace is never drawn as tall as a walk can go.
     */
    fun scaleTop(bars: List<MinuteBar>): Double =
        maxOf(bars.maxOfOrNull { it.stepsPerMinute } ?: 0.0, floor * SCALE_HEADROOM)

    companion object {
        /**
         * [session]'s minutes from [minutes] (the rows of the days it spans; any others are
         * ignored), up to its end, or to [nowMillis] while it counts. Null where its pauses are not
         * known ([Session.pausesKept]): a pause would be drawn as walking.
         */
        fun of(session: Session, minutes: List<MinuteSteps>, nowMillis: Long): OutingMinutes? {
            if (!session.pausesKept) return null
            val start = session.startedAtMillis
            val live = session.state != SessionState.FINISHED
            val end = if (live) {
                maxOf(nowMillis, session.lastEventAtMillis)
            } else {
                session.endedAtMillis ?: session.lastStepAtMillis
            }.coerceAtLeast(start + 1)
            val first = Math.floorDiv(start, MILLIS_PER_MINUTE)
            val last = Math.floorDiv(end - 1, MILLIS_PER_MINUTE)
            val count = (last - first + 1).toInt()
            val gaps = buildList {
                session.pauses.forEach { add(it.fromMillis to it.toMillis) }
                val now = session.pausedAtMillis
                if (session.state == SessionState.PAUSED && now != null) add(now to end)
            }.filter { (from, to) -> to > start && from < end }
            val byMinute = HashMap<Long, Int>(minutes.size * 2)
            minutes.forEach { if (it.epochMinute in first..last) byMinute[it.epochMinute] = it.steps }
            val paused = List(count) { index ->
                val from = (first + index) * MILLIS_PER_MINUTE
                val to = from + MILLIS_PER_MINUTE
                gaps.sumOf { (a, b) -> (minOf(b, to) - maxOf(a, from)).coerceAtLeast(0L) } * 2 > MILLIS_PER_MINUTE
            }
            val steps = List(count) { index -> if (paused[index]) 0 else byMinute[first + index] ?: 0 }
            val goalAt = session.reachedAtMillis?.let {
                ((it - first * MILLIS_PER_MINUTE).toDouble() / MILLIS_PER_MINUTE).coerceIn(0.0, count.toDouble())
            }
            return OutingMinutes(
                steps = steps,
                paused = paused,
                floor = session.intensity.cadenceFloor ?: MetricsConstants.BRISK_MINUTE_THRESHOLD,
                goalAt = goalAt,
                live = live,
                pauses = gaps.size,
            )
        }

        /** The minutes a bar may hold, smallest first: each a round share of an hour. */
        val GROUPS: List<Int> = listOf(1, 2, 5, 10, 15)

        /** While it counts the strip is half an hour wide at least, and fills from the left. */
        const val MIN_LIVE_SPAN_MINUTES: Int = 30

        /** Once over, ten minutes at least: the shortest outing is not drawn as a few pillars. */
        const val MIN_SPAN_MINUTES: Int = 10

        /** The strip's top is at least this much above the floor: a quarter of it. */
        private const val SCALE_HEADROOM = 1.25

        private const val MILLIS_PER_MINUTE = 60_000L

        private fun ceilDiv(a: Int, b: Int): Int = (a + b - 1) / b
    }
}
