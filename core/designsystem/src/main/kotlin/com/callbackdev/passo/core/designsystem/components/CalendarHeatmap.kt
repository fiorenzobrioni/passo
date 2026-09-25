package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.icons.PassoIcons
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * How far a day went towards its goal, in the calendar's five steps. One hue for a quantity,
 * getting stronger with it; a met goal in the goal's color, with a check, never the color alone.
 */
enum class HeatLevel {
    /** Not counted: to come, or from before counting began. No ground at all. */
    NONE,

    /** Counted, and no steps. */
    ZERO,

    /** Under a third of the goal. */
    LOW,

    /** A third to two thirds. */
    MID,

    /** Two thirds or more, not yet the goal. */
    HIGH,

    /** The goal met. */
    MET,

    ;

    companion object {
        /** The level of a counted day of [steps] against its own [goal]. */
        fun of(steps: Int, goal: Int): HeatLevel {
            if (steps <= 0) return ZERO
            if (goal <= 0 || steps >= goal) return MET
            val share = steps.toDouble() / goal
            return when {
                share < 1.0 / 3 -> LOW
                share < 2.0 / 3 -> MID
                else -> HIGH
            }
        }
    }
}

/** One counted day of the calendar: its level and what a screen reader says about it. */
@Immutable
data class CalendarCell(val level: HeatLevel, val description: String)

/**
 * A month as a calendar, each day's ground saying how far it went towards its goal (PLANNING.md
 * §11 Phase 5): weeks as rows from the reader's first day of the week, the weekdays' letters on
 * top, today ringed. A day not in [cells] was not counted and has no ground. Each day is a node
 * of its own for a screen reader; with [onDayClick] a counted day opens.
 *
 * @param month any day of the month to draw.
 */
@Composable
fun CalendarHeatmap(
    month: LocalDate,
    firstDayOfWeek: DayOfWeek,
    cells: Map<LocalDate, CalendarCell>,
    today: LocalDate,
    weekdayLabel: @Composable (LocalDate) -> String,
    modifier: Modifier = Modifier,
    onDayClick: ((LocalDate) -> Unit)? = null,
) {
    val first = month.withDayOfMonth(1)
    val gridStart = first.with(TemporalAdjusters.previousOrSame(firstDayOfWeek))
    val last = first.plusMonths(1).minusDays(1)
    val weeks = generateSequence(gridStart) { it.plusWeeks(1) }.takeWhile { !it.isAfter(last) }.toList()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CELL_GAP)) {
        Row(Modifier.fillMaxWidth().clearAndSetSemantics { }, horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
            for (offset in 0 until DAYS_PER_WEEK) {
                Text(
                    text = weekdayLabel(gridStart.plusDays(offset.toLong())),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (weekStart in weeks) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(CELL_GAP)) {
                for (offset in 0 until DAYS_PER_WEEK) {
                    val date = weekStart.plusDays(offset.toLong())
                    val inMonth = date.month == first.month
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (inMonth) {
                            DayCell(date, cells[date], date == today, onDayClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, cell: CalendarCell?, today: Boolean, onDayClick: ((LocalDate) -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    val level = cell?.level ?: HeatLevel.NONE
    val fill = heatColor(level)
    val ink = when (level) {
        HeatLevel.NONE -> scheme.outline
        HeatLevel.ZERO -> scheme.onSurfaceVariant
        else -> inkOn(fill)
    }
    val shape = RoundedCornerShape(10.dp)
    var modifier = Modifier.fillMaxSize().clip(shape).background(fill)
    if (today) modifier = modifier.border(2.dp, scheme.onSurface, shape)
    if (cell != null && onDayClick != null) modifier = modifier.clickable(role = Role.Button) { onDayClick(date) }
    modifier = if (cell != null) {
        modifier.clearAndSetSemantics { contentDescription = cell.description }
    } else {
        modifier.clearAndSetSemantics { }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = ink,
        )
        if (level == HeatLevel.MET) {
            Icon(
                PassoIcons.Check,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(10.dp),
            )
        }
    }
}

private fun Modifier.fillMaxSize() = this.then(Modifier.fillMaxWidth().aspectRatio(1f))

/** The ground of a level: a ramp of the accent from the quiet container to the accent, then the goal's color. */
@Composable
fun heatColor(level: HeatLevel): Color {
    val scheme = MaterialTheme.colorScheme
    val base = scheme.surfaceContainerHighest
    return when (level) {
        HeatLevel.NONE -> Color.Transparent
        HeatLevel.ZERO -> scheme.surfaceContainerHigh
        HeatLevel.LOW -> lerp(base, scheme.primary, 0.28f)
        HeatLevel.MID -> lerp(base, scheme.primary, 0.55f)
        HeatLevel.HIGH -> lerp(base, scheme.primary, 0.82f)
        HeatLevel.MET -> PassoTheme.colors.goal
    }
}

/** Whichever of the theme's two text inks reads on [fill]. */
@Composable
private fun inkOn(fill: Color): Color {
    val scheme = MaterialTheme.colorScheme
    val light = if (scheme.onSurface.luminance() >
        scheme.inverseOnSurface.luminance()
    ) {
        scheme.onSurface
    } else {
        scheme.inverseOnSurface
    }
    val dark = if (light == scheme.onSurface) scheme.inverseOnSurface else scheme.onSurface
    return if (fill.luminance() < INK_SWITCH_LUMINANCE) light else dark
}

/** The calendar's key: the ramp between its two words, and the goal's mark. */
@Composable
fun HeatLegend(less: String, more: String, met: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.semantics(mergeDescendants = true) { },
    ) {
        Text(less, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        for (level in listOf(HeatLevel.ZERO, HeatLevel.LOW, HeatLevel.MID, HeatLevel.HIGH)) {
            Box(
                Modifier.padding(
                    horizontal = 1.5.dp,
                ).size(12.dp).clip(RoundedCornerShape(3.dp)).background(heatColor(level)),
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(more, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        val goalFill = heatColor(HeatLevel.MET)
        Box(
            Modifier.size(14.dp).clip(RoundedCornerShape(3.dp)).background(goalFill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(PassoIcons.Check, contentDescription = null, tint = inkOn(goalFill), modifier = Modifier.size(10.dp))
        }
        Spacer(Modifier.width(6.dp))
        Text(met, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val CELL_GAP = 4.dp
private const val DAYS_PER_WEEK = 7
private const val INK_SWITCH_LUMINANCE = 0.4f
