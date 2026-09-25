package com.callbackdev.passo.core.tracking

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/*
 * What every notification of Passo writes the same way as the screens: a clock time, a walk's
 * length, the door back into the app.
 */

/** A clock time the way the phone shows times: 24-hour or not, as the reader set it. */
internal fun Context.clockTime(minuteOfDay: Int): String = clockTime(
    LocalTime.of((minuteOfDay / MINUTES_PER_HOUR) % HOURS_PER_DAY, minuteOfDay % MINUTES_PER_HOUR),
)

internal fun Context.clockTime(time: LocalTime): String {
    val locale = resources.configuration.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(this)) "Hm" else "hm"
    return time.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
}

/** «25 minutes», or «1 h 20 min» from an hour up, as Today writes it. */
internal fun Context.walkDuration(minutes: Int): String = if (minutes < MINUTES_PER_HOUR) {
    resources.getQuantityString(R.plurals.tracking_duration_minutes, minutes, minutes)
} else {
    getString(R.string.tracking_duration_hours_minutes, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
}

/**
 * The app's launch intent, with [extras] on it: this module does not know the app's activity,
 * and should not. [requestCode] keeps intents with different extras apart.
 */
internal fun Context.openAppIntent(requestCode: Int = 0, extras: Intent.() -> Unit = {}): PendingIntent? =
    packageManager.getLaunchIntentForPackage(packageName)?.let {
        PendingIntent.getActivity(
            this,
            requestCode,
            it.apply(extras),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

private const val MINUTES_PER_HOUR = 60
private const val HOURS_PER_DAY = 24
