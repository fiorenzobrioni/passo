package com.callbackdev.passo.core.tracking

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * The goal alarms going off, and the clock moving under them (PLANNING.md §8). An alarm is an
 * instant, so a new time zone or a clock set by hand would leave 20:00 at the old 20:00: both
 * broadcasts arm the alarms again for the new local time. They reach a manifest receiver
 * because Android exempts them from the background limits on implicit broadcasts.
 */
class GoalAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val dueAt = intent.getLongExtra(GoalAlarms.EXTRA_DUE_AT, -1L).takeIf { it >= 0 }?.let(Instant::ofEpochMilli)
        val goals = EntryPointAccessors.fromApplication(context.applicationContext, TrackingEntryPoint::class.java)
            .goals()
        val pending = goAsync()
        Work.launch {
            try {
                when (action) {
                    GoalAlarms.ACTION_EVENING_REMINDER -> goals.eveningReminder(dueAt)
                    GoalAlarms.ACTION_WEEKLY_SUMMARY -> goals.weeklySummary(dueAt)
                    Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> goals.reschedule()
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val Work = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
