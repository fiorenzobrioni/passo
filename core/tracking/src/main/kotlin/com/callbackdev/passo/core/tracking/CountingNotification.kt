package com.callbackdev.passo.core.tracking

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** How the counting notification shows, as the reader set it in the system's settings. */
enum class NotificationVisibility {
    /** In the list, with its icon in the status bar: the default. */
    SHOWN,

    /** One line at the bottom of the list, and no icon in the status bar. */
    MINIMIZED,

    /** Not shown at all; Passo still counts, and Android lists it among the active apps. */
    OFF,
}

/**
 * The counting notification as a setting. How it shows is the reader's to choose, and only in
 * the system's page for it: Android raises a foreground service's notification back to normal
 * when the app itself tries to minimize it, and keeps it where the reader put it. So Passo reads
 * the choice and opens the page, and never stores a copy of its own (PLANNING.md §8, §15).
 */
object CountingNotification {
    fun visibility(context: Context): NotificationVisibility {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return NotificationVisibility.OFF
        // No channel yet (the service has never run): it will be created at its default.
        val channel = manager.getNotificationChannel(TrackingNotifications.CHANNEL_ID)
            ?: return NotificationVisibility.SHOWN
        return when {
            channel.importance == NotificationManager.IMPORTANCE_NONE -> NotificationVisibility.OFF
            channel.importance == NotificationManager.IMPORTANCE_MIN -> NotificationVisibility.MINIMIZED
            else -> NotificationVisibility.SHOWN
        }
    }

    /**
     * The system's page for the counting notification; the app's notification page while the
     * app's notifications are off (the channel's own page cannot turn them on) or the channel
     * does not exist yet.
     */
    fun settingsIntent(context: Context): Intent {
        val manager = NotificationManagerCompat.from(context)
        val channelPage = manager.areNotificationsEnabled() &&
            manager.getNotificationChannel(TrackingNotifications.CHANNEL_ID) != null
        return if (channelPage) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, TrackingNotifications.CHANNEL_ID)
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
    }
}

/** Why the goal notifications cannot show, if they cannot. */
enum class GoalNotificationsBlock {
    /** They can show. */
    NONE,

    /** Passo may not notify at all: the permission was refused, or its notifications are off. */
    APP,

    /** Passo may notify, but the reader turned off the goals' channel. */
    CHANNEL,
}

/**
 * Whether the goal notifications the reader turned on can reach them: a switch that is on while
 * Android drops what it sends would be the screen lying, so Settings says so and opens the page
 * that fixes it.
 */
object GoalNotificationsAccess {
    fun block(context: Context): GoalNotificationsBlock {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return GoalNotificationsBlock.APP
        val channel = manager.getNotificationChannel(GoalNotifications.CHANNEL_ID)
        return if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            GoalNotificationsBlock.CHANNEL
        } else {
            GoalNotificationsBlock.NONE
        }
    }

    /** The system's page that lifts [block]: the goals' channel, or the app's notifications. */
    fun settingsIntent(context: Context, block: GoalNotificationsBlock): Intent =
        if (block == GoalNotificationsBlock.CHANNEL) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, GoalNotifications.CHANNEL_ID)
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
}

/**
 * Whether an outing's signals can reach the reader (PLANNING.md §11 Phase 10): its goal
 * notification and, with it, its vibrations, which follow the outings' channel. The Outings
 * page says so when they cannot, with the page that fixes it.
 */
object SessionSignalsAccess {
    fun block(context: Context): GoalNotificationsBlock {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return GoalNotificationsBlock.APP
        val channel = manager.getNotificationChannel(SessionNotifications.CHANNEL_ID)
        return if (channel?.importance == NotificationManager.IMPORTANCE_NONE) {
            GoalNotificationsBlock.CHANNEL
        } else {
            GoalNotificationsBlock.NONE
        }
    }

    fun settingsIntent(context: Context, block: GoalNotificationsBlock): Intent =
        if (block == GoalNotificationsBlock.CHANNEL) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, SessionNotifications.CHANNEL_ID)
        } else {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
}
