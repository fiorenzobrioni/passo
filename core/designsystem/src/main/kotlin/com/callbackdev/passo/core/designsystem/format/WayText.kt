package com.callbackdev.passo.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.designsystem.ways.routeOf
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.model.WayJourneyState
import com.callbackdev.passo.core.model.WayKind

/**
 * Where the reader stands on a way, in one line (Chiaro's rule: a sentence before a number):
 * at the start, past the last stop reached, arrived, or left there.
 */
@Composable
fun wayHeadline(progress: WayProgress): String {
    val last = stringResource(placeNameRes(progress.last.stop.key))
    return when {
        progress.finished && progress.way.kind == WayKind.WALK -> stringResource(R.string.walk_done, last)
        progress.finished -> stringResource(R.string.way_headline_arrived, last)
        progress.journey.state == WayJourneyState.LEFT -> stringResource(R.string.way_headline_left, last)
        progress.reached.size == 1 -> stringResource(R.string.way_headline_start, last)
        else -> stringResource(R.string.way_headline_past, last)
    }
}

/**
 * The way's distance left to its end, and the next stage (on a city walk, the next place, as
 * a passer-by would say how far): the line under the headline.
 */
@Composable
fun wayDetail(progress: WayProgress, format: MeasureFormatter): String? {
    val next = progress.nextStage ?: return null
    val end = progress.way.stops.last()
    val toEnd = stringResource(
        R.string.way_to_end,
        format.distance(progress.metersLeft).text(),
        stringResource(placeNameRes(end.key)),
    )
    if (next == end) return toEnd
    if (progress.way.kind == WayKind.WALK) {
        val toNext = stringResource(
            R.string.walk_next,
            stringResource(placeNameRes(next.key)),
            format.aheadDistance(progress.metersTo(next)).text(),
        )
        return "$toEnd. $toNext"
    }
    val toNext = stringResource(
        R.string.way_next_stage,
        stringResource(placeNameRes(next.key)),
        format.distance(progress.metersTo(next)).text(),
    )
    return "$toEnd. $toNext"
}

/** "741 of 1,020 km". */
@Composable
fun wayProgressText(progress: WayProgress, format: MeasureFormatter): String =
    wayProgressText(progress.way, progress.walkedMeters, format)

/** "3.21 of 9.33 km": [walkedMeters] along [way], as an outing under way has it. */
@Composable
fun wayProgressText(way: Way, walkedMeters: Double, format: MeasureFormatter): String = stringResource(
    R.string.way_progress,
    format.distance(walkedMeters).text(),
    format.distance(way.lengthMeters.toDouble()).text(),
)

/** "47 stages"; for a city walk, "14 places". */
@Composable
fun wayStages(way: Way): String = if (way.kind == WayKind.WALK) {
    pluralStringResource(R.plurals.way_places, way.stops.size, way.stops.size)
} else {
    pluralStringResource(R.plurals.way_stages, way.stages.size, way.stages.size)
}

/**
 * A stretch of days as the reader thinks of it: days up to two weeks, then weeks up to two
 * months, then months up to two years, then years. Always "about": it is an estimate.
 */
@Composable
fun wayDuration(days: Int): String = when {
    days < DAYS_IN_TWO_WEEKS -> pluralStringResource(R.plurals.way_duration_days, days, days)

    days < DAYS_IN_TWO_MONTHS -> (days / DAYS_PER_WEEK).let {
        pluralStringResource(R.plurals.way_duration_weeks, it, it)
    }

    days < DAYS_IN_TWO_YEARS -> (days / DAYS_PER_MONTH).let {
        pluralStringResource(R.plurals.way_duration_months, it, it)
    }

    else -> (days / DAYS_PER_YEAR).let { pluralStringResource(R.plurals.way_duration_years, it, it) }
}

/** What TalkBack says of a way's map: the way, and how far along it the reader is. */
@Composable
fun wayMapSpoken(way: Way, progress: WayProgress?, format: MeasureFormatter): String {
    val name = stringResource(routeOf(way.id).first)
    return if (progress == null) {
        stringResource(R.string.way_map_spoken, name, stringResource(routeOf(way.id).second))
    } else {
        stringResource(
            R.string.way_map_spoken_walked,
            name,
            format.distance(progress.walkedMeters).text(),
            format.distance(way.lengthMeters.toDouble()).text(),
            stringResource(placeNameRes(progress.last.stop.key)),
        )
    }
}

private const val DAYS_PER_WEEK = 7
private const val DAYS_PER_MONTH = 30
private const val DAYS_PER_YEAR = 365
private const val DAYS_IN_TWO_WEEKS = 14
private const val DAYS_IN_TWO_MONTHS = 60
private const val DAYS_IN_TWO_YEARS = 730
