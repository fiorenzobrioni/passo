package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.domain.goals.EveningNudge
import com.callbackdev.passo.core.domain.goals.WeekHeadline
import com.callbackdev.passo.core.domain.goals.WeeklySummary
import com.callbackdev.passo.core.domain.insights.Trend
import com.callbackdev.passo.core.model.UnitPreference
import java.time.format.TextStyle
import kotlin.math.abs

/** "Goal reached": the day's goal, the minute it was met, and the streak it extends. */
internal data class GoalReachedContent(val goalSteps: Int, val reachedAtMinute: Int, val streakDays: Int)

/**
 * The goal notifications (PLANNING.md §8): goal reached, the evening reminder, the weekly
 * summary. One channel, `goals`, at the default importance, because each of them is something
 * the reader asked for in Settings; the counting notification keeps its own quiet one.
 *
 * Each says one sentence first (Chiaro's rule) and the numbers after it, in the same words as
 * Today and History, with the estimates saying they are. Tapping one opens the app.
 */
internal class GoalNotifications(private val context: Context) {
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.goals_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = context.getString(R.string.goals_channel_description) }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    fun goalReached(content: GoalReachedContent, units: UnitPreference): Notification {
        val format = context.measureFormatter(units)
        val res = context.resources
        val at = context.clockTime(content.reachedAtMinute)
        val text = if (content.streakDays >= MIN_STREAK_TO_TELL) {
            res.getQuantityString(R.plurals.goal_reached_streak, content.streakDays, at, content.streakDays)
        } else {
            res.getString(R.string.goal_reached_at, at)
        }
        return builder(REQUEST_GOAL_REACHED)
            .setContentTitle(
                res.getQuantityString(R.plurals.goal_reached_title, content.goalSteps, format.steps(content.goalSteps)),
            )
            .setContentText(text)
            .build()
    }

    fun eveningReminder(nudge: EveningNudge, units: UnitPreference): Notification {
        val format = context.measureFormatter(units)
        val res = context.resources
        val text = res.getString(
            R.string.evening_reminder_text,
            context.walkDuration(nudge.minutes),
            format.steps(nudge.goalSteps),
        )
        val soFar = res.getString(
            R.string.evening_reminder_so_far,
            format.steps(nudge.steps),
            format.percent(nudge.steps.toDouble() / nudge.goalSteps),
        )
        return builder(REQUEST_EVENING_REMINDER)
            .setContentTitle(
                res.getQuantityString(R.plurals.evening_reminder_title, nudge.remaining, format.steps(nudge.remaining)),
            )
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n$soFar"))
            .setProgress(nudge.goalSteps, nudge.steps, false)
            .build()
    }

    fun weeklySummary(summary: WeeklySummary, units: UnitPreference): Notification {
        val format = context.measureFormatter(units)
        val res = context.resources
        val title = when (val headline = summary.headline) {
            WeekHeadline.EveryDay -> res.getString(R.string.weekly_title_every_day)

            is WeekHeadline.VersusWeekBefore -> when (headline.trend) {
                Trend.UP -> res.getString(R.string.weekly_title_up, format.percent(abs(headline.change)))
                Trend.DOWN -> res.getString(R.string.weekly_title_down, format.percent(abs(headline.change)))
                Trend.STEADY -> res.getString(R.string.weekly_title_steady)
            }

            is WeekHeadline.GoalDays ->
                res.getQuantityString(
                    R.plurals.weekly_title_goal_days,
                    headline.goalDays,
                    headline.goalDays,
                    headline.countedDays,
                )
        }
        val totals = res.getString(
            R.string.weekly_totals,
            format.integer(summary.steps),
            format.steps(summary.dailyAverage),
        )
        val lines = buildList {
            add(totals)
            // Said once: in the title when it is the news, here otherwise.
            if (summary.headline is WeekHeadline.VersusWeekBefore) {
                add(
                    res.getQuantityString(
                        R.plurals.weekly_goal_days,
                        summary.goalDays,
                        summary.goalDays,
                        summary.countedDays,
                    ),
                )
            }
            val locale = res.configuration.locales[0]
            add(
                res.getString(
                    R.string.weekly_best_day,
                    summary.bestDay.dayOfWeek.getDisplayName(TextStyle.FULL, locale),
                    format.steps(summary.bestDaySteps),
                ),
            )
            add(
                res.getString(
                    R.string.tracking_notification_estimates,
                    res.format(format.distance(summary.distanceMeters)),
                    res.format(format.energy(summary.activeKcal)),
                ),
            )
        }
        return builder(REQUEST_WEEKLY_SUMMARY)
            .setContentTitle(title)
            .setContentText(totals)
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .build()
    }

    /** Posts [notification] as [id]; a no-op without the notification permission. */
    fun post(id: Int, notification: Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    private fun builder(requestCode: Int): NotificationCompat.Builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_steps)
        .setContentIntent(context.openAppIntent(requestCode))
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)

    companion object {
        const val CHANNEL_ID = "goals"
        const val ID_GOAL_REACHED = 2
        const val ID_EVENING_REMINDER = 3
        const val ID_WEEKLY_SUMMARY = 4

        // One launch intent each, so no tap ever carries another's.
        private const val REQUEST_GOAL_REACHED = 2
        private const val REQUEST_EVENING_REMINDER = 3
        private const val REQUEST_WEEKLY_SUMMARY = 4

        /** One day at the goal is a day, not a streak (as Insights says it). */
        private const val MIN_STREAK_TO_TELL = 2
    }
}
