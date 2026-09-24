package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.text.NumberFormat

/**
 * The ongoing notification the foreground service requires (PLANNING.md §8), minimal for now:
 * a fixed title and today's steps. The rich one (progress, distance) is Phase 6.
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

    /** @param stepsToday null until the stored count has been read, so no zero is ever shown by mistake. */
    fun build(stepsToday: Int?): Notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_steps)
        .setContentTitle(context.getString(R.string.tracking_notification_title))
        .apply {
            if (stepsToday != null) {
                val locale = context.resources.configuration.locales[0]
                val formatted = NumberFormat.getIntegerInstance(locale).format(stepsToday)
                setContentText(
                    context.resources.getQuantityString(R.plurals.tracking_notification_steps, stepsToday, formatted),
                )
            }
        }
        .setContentIntent(openAppIntent())
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setShowWhen(false)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .build()

    /** Replaces the posted notification; a no-op without the notification permission. */
    fun update(stepsToday: Int?) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, build(stepsToday))
    }

    // The launcher intent: this module does not know the app's activity, and should not.
    private fun openAppIntent(): PendingIntent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.let {
            PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

    companion object {
        const val CHANNEL_ID = "tracking"
        const val NOTIFICATION_ID = 1
    }
}
