package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.model.UnitPreference
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * What the ongoing notification says.
 *
 * @property steps today's count; null until the stored count has been read, so no zero is ever
 *   shown by mistake.
 * @property day today in full, for the expanded form; null until today's minutes and the
 *   settings have been read, and then the notification is the collapsed form only.
 */
internal data class NotificationContent(
    val steps: Int?,
    val day: TodayOverview? = null,
    val units: UnitPreference = UnitPreference.SYSTEM,
)

/**
 * The ongoing notification the foreground service requires (PLANNING.md §8). Collapsed, the
 * count; expanded, the day as Today opens it: the sentence, the goal and the estimates, over a
 * bar towards the goal. Both forms are built at once, in the same update, so the expanded one
 * costs no update of its own.
 */
internal class TrackingNotifications(private val context: Context) {
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.tracking_channel_description)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun build(content: NotificationContent): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(context.getString(R.string.tracking_notification_title))
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        val day = content.day
        val steps = day?.steps ?: content.steps ?: return builder.build()
        val format = context.measureFormatter(content.units)
        val count = context.resources.getQuantityString(
            R.plurals.tracking_notification_steps,
            steps,
            format.steps(steps),
        )
        builder.setContentText(count)
        if (day != null) {
            builder
                .setStyle(
                    NotificationCompat.BigTextStyle().setBigContentTitle(count).bigText(expandedText(day, format)),
                )
                .setProgress(day.goalSteps, day.steps.coerceAtMost(day.goalSteps), false)
        }
        return builder.build()
    }

    /** Replaces the posted notification; a no-op without the notification permission. */
    fun update(content: NotificationContent) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, build(content))
    }

    /**
     * The expanded lines: the day's sentence, the goal with the active minutes, the estimates.
     * One sentence before any number, and the estimates say they are (Chiaro's rules).
     */
    fun expandedText(day: TodayOverview, format: MeasureFormatter): String {
        val res = context.resources
        val sentence =
            day.goalReachedAt?.let { res.getString(R.string.tracking_notification_goal_reached, clockTime(it)) }
                ?: res.getQuantityString(
                    R.plurals.tracking_notification_to_go,
                    day.remaining,
                    format.steps(day.remaining),
                    duration(TodayOverview.minutesToWalk(day.remaining)),
                )
        val goal = res.getString(
            R.string.tracking_notification_pair,
            res.getString(
                R.string.tracking_notification_goal,
                format.percent(day.progress),
                format.steps(day.goalSteps),
            ),
            res.getQuantityString(
                R.plurals.tracking_notification_active,
                day.metrics.activeMinutes,
                format.integer(day.metrics.activeMinutes.toLong()),
            ),
        )
        val estimates = res.getString(
            R.string.tracking_notification_estimates,
            res.format(format.distance(day.metrics.distanceMeters)),
            res.format(format.energy(day.metrics.activeKcal)),
        )
        return listOf(sentence, goal, estimates).joinToString("\n")
    }

    /** «25 minutes», or «1 h 20 min» from an hour up, as Today writes it. */
    private fun duration(minutes: Int): String = if (minutes < MINUTES_PER_HOUR) {
        context.resources.getQuantityString(R.plurals.tracking_duration_minutes, minutes, minutes)
    } else {
        context.getString(
            R.string.tracking_duration_hours_minutes,
            minutes / MINUTES_PER_HOUR,
            minutes % MINUTES_PER_HOUR,
        )
    }

    /** A clock time the way the phone shows times: 24-hour or not, as the reader set it. */
    private fun clockTime(minuteOfDay: Int): String {
        val locale = context.resources.configuration.locales[0]
        val skeleton = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"
        val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
        return LocalTime.of((minuteOfDay / MINUTES_PER_HOUR) % HOURS_PER_DAY, minuteOfDay % MINUTES_PER_HOUR)
            .format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    // The launcher intent: this module does not know the app's activity, and should not.
    private fun openAppIntent(): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

    companion object {
        const val CHANNEL_ID = "tracking"
        const val NOTIFICATION_ID = 1
        private const val MINUTES_PER_HOUR = 60
        private const val HOURS_PER_DAY = 24
    }
}
