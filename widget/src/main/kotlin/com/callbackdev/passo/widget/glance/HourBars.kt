package com.callbackdev.passo.widget.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.widget.WeekDay

/** An hour on the bars: the one under way, one gone by with steps, or one with none (yet). */
internal enum class BarKind { NOW, PAST, EMPTY }

internal data class Bar(val height: Dp, val kind: BarKind)

/**
 * The 24 bars' heights, against the day's busiest hour at [height]. An hour with steps is never
 * shorter than [BarMin], so a quiet hour still reads as walked; an hour with none, or still to
 * come, is the [BarBaseline] of the track. The hour under way is highlighted even with no steps
 * yet, as a baseline: it marks where the day stands.
 */
internal fun hourBars(hours: List<Int>, nowHour: Int, height: Dp): List<Bar> {
    val busiest = hours.maxOrNull()?.takeIf { it > 0 }
    return hours.mapIndexed { hour, steps ->
        val scaled = if (busiest == null || steps <= 0) {
            BarBaseline
        } else {
            (height * (steps.toFloat() / busiest)).coerceIn(BarMin, height)
        }
        val kind = when {
            hour == nowHour -> BarKind.NOW
            hour < nowHour && steps > 0 -> BarKind.PAST
            else -> BarKind.EMPTY
        }
        Bar(if (hour > nowHour) BarBaseline else scaled, kind)
    }
}

/** The last seven days' bars, and where today's goal stands on them. */
internal data class WeekBars(val bars: List<Bar>, val goalHeight: Dp)

/**
 * The last seven days on the world's scale: from zero to the busiest day or the highest goal,
 * whichever is more, so a quiet week looks quiet and the goal line stands where the goal is
 * (PLANNING.md §15, «The last seven days on At a glance»). Today, last, is highlighted even with
 * no steps yet; a day with none, or from before counting began, is the track's baseline.
 */
internal fun weekBars(days: List<WeekDay>, height: Dp): WeekBars {
    val top = maxOf(days.maxOfOrNull { it.steps ?: 0 } ?: 0, days.maxOfOrNull { it.goalSteps } ?: 0).coerceAtLeast(1)
    val bars = days.mapIndexed { index, day ->
        val steps = day.steps ?: 0
        val kind = when {
            index == days.lastIndex -> BarKind.NOW
            steps > 0 -> BarKind.PAST
            else -> BarKind.EMPTY
        }
        val scaled = if (steps <= 0) BarBaseline else (height * (steps.toFloat() / top)).coerceIn(BarMin, height)
        Bar(scaled, kind)
    }
    val goal = days.lastOrNull()?.goalSteps ?: 0
    return WeekBars(bars, (height * (goal.toFloat() / top)).coerceIn(0.dp, height))
}

/** The air between two bars: a fifth of a bar's slot, between 1 and 3 dp. */
internal fun barGap(width: Dp): Dp = (width / HOURS / 5).coerceIn(1.dp, 3.dp)

internal val BarMin = 3.dp
internal val BarBaseline = 2.dp

internal const val HOURS = 24
internal const val WEEK_DAYS = 7
internal const val GROUPS = 4
internal const val HOURS_PER_GROUP = HOURS / GROUPS
internal const val MINUTES_PER_HOUR = 60
