package com.callbackdev.passo.widget

import android.content.Context
import android.icu.text.CompactDecimalFormat
import android.text.format.DateFormat
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.today.Headline
import com.callbackdev.passo.core.domain.today.Pace
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.UserSettings
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * The words the two cards print. The day's sentence is Today's headline (the same
 * `TodayOverview`, so the widget and the screen never tell two stories about one afternoon) in
 * its brief register: a home-screen card has a column where the screen has a paragraph.
 */

internal fun widgetFormatter(context: Context, settings: UserSettings): MeasureFormatter =
    context.measureFormatter(settings.units)

/** The day's sentence, brief: the one sentence before any number (Chiaro's rule, Today's headline). */
internal fun sentence(context: Context, overview: TodayOverview, format: MeasureFormatter): String {
    val res = context.resources
    return when (val headline = overview.headline) {
        Headline.NoStepsYet -> res.getString(R.string.widget_sentence_no_steps)

        is Headline.GoalReached -> res.getString(
            R.string.widget_sentence_goal_reached,
            clockTime(context, headline.minuteOfDay),
        )

        is Headline.VersusUsual -> when (val pace = headline.pace) {
            is Pace.Ahead -> res.getQuantityString(
                R.plurals.widget_sentence_ahead,
                pace.steps,
                format.steps(pace.steps),
            )

            is Pace.Behind -> res.getQuantityString(
                R.plurals.widget_sentence_behind,
                pace.steps,
                format.steps(pace.steps),
            )

            Pace.OnPace -> res.getString(R.string.widget_sentence_on_pace)
        }

        is Headline.ToGo -> res.getQuantityString(
            R.plurals.widget_sentence_to_go,
            headline.steps,
            format.steps(headline.steps),
        )
    }
}

/**
 * What a card that is not counting says, with what a tap does; null while counting. It takes the
 * sentence's place wherever a card has one (a count that is not moving is the first thing to
 * know about it, and the sentence's own switch cannot hide it); [short] is the footnote of a card
 * with no such place, where only the word fits.
 */
internal fun statusText(context: Context, state: CountingState, short: Boolean = false): String? {
    val (long, word) = when (state) {
        CountingState.PAUSED -> R.string.widget_status_paused to R.string.widget_status_paused_short
        CountingState.STOPPED -> R.string.widget_status_stopped to R.string.widget_status_stopped_short
        CountingState.PERMISSION_NEEDED -> R.string.widget_status_permission to R.string.widget_status_permission_short
        CountingState.COUNTING, CountingState.NOT_SET_UP, CountingState.NO_SENSOR -> return null
    }
    return context.getString(if (short) word else long)
}

/** The whole card's message when there is no day to draw: none to read yet, none readable, or no sensor. */
internal fun messageTitle(context: Context, model: WidgetModel): String = context.getString(
    when {
        model.unavailable -> R.string.widget_unavailable_title
        model.state == CountingState.NO_SENSOR -> R.string.widget_no_sensor_title
        else -> R.string.widget_setup_title
    },
)

internal fun messageHint(context: Context, model: WidgetModel): String = context.getString(
    when {
        model.unavailable -> R.string.widget_unavailable_hint
        model.state == CountingState.NO_SENSOR -> R.string.widget_no_sensor_hint
        else -> R.string.widget_setup_hint
    },
)

/** «of 10,000 steps», the line under the number on «At a glance»; [short] drops the word. */
internal fun goalOfText(context: Context, goalSteps: Int, format: MeasureFormatter, short: Boolean): String =
    if (short) {
        context.getString(R.string.widget_goal_of_short, format.steps(goalSteps))
    } else {
        context.resources.getQuantityString(R.plurals.widget_goal_of, goalSteps, format.steps(goalSteps))
    }

/** «84% of 10,000», the goal as a fact on «In words». Floored, so 100% is never shown early. */
internal fun goalShareText(context: Context, overview: TodayOverview, format: MeasureFormatter): String =
    context.getString(R.string.widget_goal_share, format.percent(overview.progress), format.steps(overview.goalSteps))

/** «84%»: the same fact where only the figure fits. */
internal fun goalPercentText(overview: TodayOverview, format: MeasureFormatter): String =
    format.percent(overview.progress)

/** «≈ 6.12 km · 312 kcal»: estimates, and the sign says so where a line has no room for the word. */
internal fun metricsText(context: Context, overview: TodayOverview, format: MeasureFormatter): String {
    val res = context.resources
    return context.getString(
        R.string.widget_metrics_line,
        res.format(format.distance(overview.metrics.distanceMeters)),
        res.format(format.energy(overview.metrics.activeKcal)),
    )
}

/** «Steps today», the eyebrow of «In words»; [short] is «Today», for the one-cell card. */
internal fun eyebrowText(context: Context, short: Boolean): String =
    context.getString(if (short) R.string.widget_eyebrow_short else R.string.widget_eyebrow)

/** A clock time the way the phone shows times: 24-hour or not, as the reader set it. */
internal fun clockTime(context: Context, minuteOfDay: Int): String {
    val locale = context.widgetLocale()
    val pattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "Hm" else "hm")
    return LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60).format(DateTimeFormatter.ofPattern(pattern, locale))
}

/** An hour on the bars' axis: «06» or «6 AM». */
internal fun axisHour(context: Context, hour: Int): String {
    val locale = context.widgetLocale()
    val pattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "HH" else "ha")
    return LocalTime.of(hour % 24, 0).format(DateTimeFormatter.ofPattern(pattern, locale))
}

/**
 * The count where a card has one cell for it: «8.4K» in English, and in Italian the plain
 * figure, because the language does not shorten thousands (CLDR's short pattern for it is «0»).
 * The locale's own rule, not an abbreviation invented here. Below a thousand it is the number.
 */
internal fun compactCount(context: Context, steps: Int): String =
    CompactDecimalFormat.getInstance(context.widgetLocale(), CompactDecimalFormat.CompactStyle.SHORT)
        .apply { maximumFractionDigits = 1 }
        .format(steps.toLong())

/** What the ring says to a screen reader: the count, the goal and the share, or that it is met. */
internal fun ringDescription(context: Context, overview: TodayOverview, format: MeasureFormatter): String {
    val res = context.resources
    val count = res.getQuantityString(
        R.plurals.widget_ring_description,
        overview.steps,
        format.steps(overview.steps),
        format.steps(overview.goalSteps),
        format.percent(overview.progress),
    )
    return if (overview.goalReachedAt != null) "$count ${res.getString(R.string.widget_ring_reached)}" else count
}
