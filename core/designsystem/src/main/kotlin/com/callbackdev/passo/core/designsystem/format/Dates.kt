package com.callbackdev.passo.core.designsystem.format

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/*
 * Dates and times the way the reader's phone writes them: the app's language, the phone's 12 or
 * 24 hours, the locale's own order of day and month. Shared by every screen that shows a date,
 * so Today, History and Insights never write the same day two ways.
 */

/** The app's current locale: the per-app language when one is picked, else the phone's. */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** A clock time the way the phone shows times: 24-hour or not, as the reader set it. */
@Composable
fun clockTime(minuteOfDay: Int): String {
    val context = LocalContext.current
    val locale = currentLocale()
    val pattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "Hm" else "hm")
    return LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60).format(DateTimeFormatter.ofPattern(pattern, locale))
}

/** An hour on a chart's axis: «06» or «6 AM». */
@Composable
fun axisHour(hour: Int): String {
    val context = LocalContext.current
    val locale = currentLocale()
    val pattern = DateFormat.getBestDateTimePattern(locale, if (DateFormat.is24HourFormat(context)) "HH" else "ha")
    return LocalTime.of(hour % 24, 0).format(DateTimeFormatter.ofPattern(pattern, locale))
}

/** A date in the locale's words from a skeleton: "EEEEdMMMM" is «Thursday, 25 September». */
@Composable
fun datePattern(date: LocalDate, skeleton: String, capitalized: Boolean = true): String {
    val locale = currentLocale()
    val text = date.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
    return if (capitalized) text.replaceFirstChar { it.titlecase(locale) } else text
}

/** «Thursday, 25 September». */
@Composable
fun longDate(date: LocalDate): String = datePattern(date, "EEEEdMMMM")

/** «25 Sep». */
@Composable
fun shortDate(date: LocalDate): String = datePattern(date, "dMMM", capitalized = false)

/** «25 Sep 2025», for a date that may not be this year's. */
@Composable
fun shortDateWithYear(date: LocalDate): String = datePattern(date, "dMMMyyyy", capitalized = false)

/** «September 2026». */
@Composable
fun monthYear(date: LocalDate): String = datePattern(date, "MMMMyyyy")

/** «Sep», a month on an axis. */
@Composable
fun shortMonth(date: LocalDate): String = datePattern(date, "MMM")

/** «T» or «M»: a weekday in one letter, for an axis or a calendar's head. */
@Composable
fun weekdayLetter(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, currentLocale()).uppercase(currentLocale())

/** «Thu»: a weekday, short. */
@Composable
fun weekdayShort(date: LocalDate): String =
    date.dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, currentLocale()).replaceFirstChar {
        it.titlecase(currentLocale())
    }
