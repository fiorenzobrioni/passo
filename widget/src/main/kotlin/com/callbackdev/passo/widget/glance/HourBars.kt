package com.callbackdev.passo.widget.glance

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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

/** The air between two bars: a fifth of a bar's slot, between 1 and 3 dp. */
internal fun barGap(width: Dp): Dp = (width / HOURS / 5).coerceIn(1.dp, 3.dp)

internal val BarMin = 3.dp
internal val BarBaseline = 2.dp

internal const val HOURS = 24
internal const val GROUPS = 4
internal const val HOURS_PER_GROUP = HOURS / GROUPS
internal const val MINUTES_PER_HOUR = 60
