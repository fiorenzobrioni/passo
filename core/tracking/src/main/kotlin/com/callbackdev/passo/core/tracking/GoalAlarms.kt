package com.callbackdev.passo.core.tracking

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/**
 * The two alarms of the goal notifications, one each, never exact (PLANNING.md §9: no exact
 * alarm, and no permission for one).
 *
 * - The **evening reminder** is `setAndAllowWhileIdle` on the wall clock with a wakeup, as
 *   Chiaro's sky reminders: one wake a day, only for a reader who asked for it, and let through
 *   Doze, because a reminder to walk that Doze holds until 22:00 is worth nothing. It may come a
 *   few minutes late; it never comes early.
 * - The **weekly summary** does not wake the phone at all (`RTC`): it is posted the first time
 *   the phone is awake after nine on the first day of the week, which is when someone looks.
 *
 * Each alarm carries the instant it was due, so a late delivery can tell how late it is.
 */
internal class GoalAlarms(private val context: Context) {
    private val manager: AlarmManager? = context.getSystemService(AlarmManager::class.java)

    fun setEveningReminder(at: Instant) {
        manager?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent(ACTION_EVENING_REMINDER, at))
    }

    fun cancelEveningReminder() {
        manager?.cancel(intent(ACTION_EVENING_REMINDER, null))
    }

    fun setWeeklySummary(at: Instant) {
        manager?.set(AlarmManager.RTC, at.toEpochMilli(), intent(ACTION_WEEKLY_SUMMARY, at))
    }

    fun cancelWeeklySummary() {
        manager?.cancel(intent(ACTION_WEEKLY_SUMMARY, null))
    }

    // One fixed request code per alarm with FLAG_UPDATE_CURRENT: setting it again replaces it,
    // so the app never collects a queue it forgot about. Extras do not count in the match, so
    // the same intent cancels it.
    private fun intent(action: String, dueAt: Instant?): PendingIntent {
        val intent = Intent(context, GoalAlarmReceiver::class.java).setAction(action)
        dueAt?.let { intent.putExtra(EXTRA_DUE_AT, it.toEpochMilli()) }
        val code = if (action == ACTION_EVENING_REMINDER) REQUEST_EVENING_REMINDER else REQUEST_WEEKLY_SUMMARY
        return PendingIntent.getBroadcast(
            context,
            code,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_EVENING_REMINDER = "com.callbackdev.passo.action.EVENING_REMINDER"
        const val ACTION_WEEKLY_SUMMARY = "com.callbackdev.passo.action.WEEKLY_SUMMARY"
        const val EXTRA_DUE_AT = "com.callbackdev.passo.extra.DUE_AT"
        private const val REQUEST_EVENING_REMINDER = 0x601
        private const val REQUEST_WEEKLY_SUMMARY = 0x602
    }
}
