package com.callbackdev.passo.feature.today

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.today.Headline
import com.callbackdev.passo.core.domain.today.Pace
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The app's current locale: the per-app language when one is picked, else the phone's. */
@Composable
@ReadOnlyComposable
internal fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** A clock time the way the phone shows times: 24-hour or not, as the reader set it. */
@Composable
internal fun clockTime(minuteOfDay: Int): String {
    val context = LocalContext.current
    val pattern = DateFormat.getBestDateTimePattern(
        currentLocale(),
        if (DateFormat.is24HourFormat(context)) "Hm" else "hm",
    )
    return LocalTime.of(
        (minuteOfDay / 60) % 24,
        minuteOfDay % 60,
    ).format(DateTimeFormatter.ofPattern(pattern, currentLocale()))
}

/** An hour on the chart's axis: «06» or «6 AM». */
@Composable
internal fun axisHour(hour: Int): String {
    val context = LocalContext.current
    val pattern = DateFormat.getBestDateTimePattern(
        currentLocale(),
        if (DateFormat.is24HourFormat(context)) "HH" else "ha",
    )
    return LocalTime.of(hour % 24, 0).format(DateTimeFormatter.ofPattern(pattern, currentLocale()))
}

/** «Thursday, 25 September», the way the locale writes a day, capitalised as a heading. */
@Composable
internal fun longDate(date: LocalDate): String {
    val locale = currentLocale()
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return date.format(DateTimeFormatter.ofPattern(pattern, locale)).replaceFirstChar { it.titlecase(locale) }
}

/** A walk's length: «23 minutes», or «1 h 20 min» from an hour up. */
@Composable
internal fun duration(minutes: Int): String = if (minutes < 60) {
    pluralStringResource(R.plurals.today_duration_minutes, minutes, minutes)
} else {
    stringResource(R.string.today_duration_hours_minutes, minutes / 60, minutes % 60)
}

/** The sentence for [headline], or for the detail line under it. */
@Composable
internal fun headlineText(headline: Headline, format: MeasureFormatter): String = when (headline) {
    Headline.NoStepsYet -> stringResource(R.string.today_headline_no_steps)

    is Headline.GoalReached -> stringResource(R.string.today_headline_goal_reached, clockTime(headline.minuteOfDay))

    is Headline.VersusUsual -> when (val pace = headline.pace) {
        is Pace.Ahead -> pluralStringResource(R.plurals.today_headline_ahead, pace.steps, format.steps(pace.steps))
        is Pace.Behind -> pluralStringResource(R.plurals.today_headline_behind, pace.steps, format.steps(pace.steps))
        Pace.OnPace -> stringResource(R.string.today_headline_on_pace)
    }

    is Headline.ToGo -> pluralStringResource(
        R.plurals.today_headline_to_go,
        headline.steps,
        format.steps(headline.steps),
        duration(headline.minutes),
    )
}
