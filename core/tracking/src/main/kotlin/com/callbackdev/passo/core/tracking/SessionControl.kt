package com.callbackdev.passo.core.tracking

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * The one way to start, pause, resume, stop or keep going an outing (PLANNING.md §11 Phase 10):
 * from Today, from the Outings page, from a launcher shortcut, from the notifications. Each is a
 * command to the tracking service, which is the one that measures the outing; it starts the
 * service if the system had stopped it, as opening the app does.
 */
object SessionControl {
    /** Starts the outing of plan [planId]. */
    fun start(context: Context, planId: Long): Boolean = send(context, startIntent(context, planId))

    /** Starts an outing for the rest of today's goal (the evening reminder's "Walk now"). */
    fun startRestOfDay(context: Context): Boolean = send(context, restOfDayIntent(context))

    fun pause(context: Context): Boolean = send(context, command(context, ACTION_PAUSE))

    fun resume(context: Context): Boolean = send(context, command(context, ACTION_RESUME))

    fun stop(context: Context): Boolean = send(context, command(context, ACTION_STOP))

    fun keepGoing(context: Context): Boolean = send(context, command(context, ACTION_KEEP_GOING))

    internal fun startIntent(context: Context, planId: Long): Intent =
        command(context, ACTION_START).putExtra(EXTRA_PLAN_ID, planId)

    internal fun restOfDayIntent(context: Context): Intent = command(context, ACTION_START_REST_OF_DAY)

    /** A notification's button: the command, delivered to the service. */
    internal fun pendingIntent(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getForegroundService(
            context,
            requestCode,
            command(context, action),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    internal fun command(context: Context, action: String): Intent =
        Intent(context, StepTrackingService::class.java).setAction(action)

    private fun send(context: Context, intent: Intent): Boolean {
        if (StepTracking.readiness(context) != TrackingReadiness.READY) return false
        return try {
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (e: IllegalStateException) {
            // Only from a context that may not start it; the caller's screen stays as it was.
            false
        }
    }

    internal const val ACTION_START = "com.callbackdev.passo.action.SESSION_START"
    internal const val ACTION_START_REST_OF_DAY = "com.callbackdev.passo.action.SESSION_START_REST_OF_DAY"
    internal const val ACTION_PAUSE = "com.callbackdev.passo.action.SESSION_PAUSE"
    internal const val ACTION_RESUME = "com.callbackdev.passo.action.SESSION_RESUME"
    internal const val ACTION_STOP = "com.callbackdev.passo.action.SESSION_STOP"
    internal const val ACTION_KEEP_GOING = "com.callbackdev.passo.action.SESSION_KEEP_GOING"
    internal const val EXTRA_PLAN_ID = "com.callbackdev.passo.extra.PLAN_ID"

    /**
     * On the app's launch intent, from a launcher shortcut: the plan to start. The app starts it
     * from the activity, and the reader sees it begin on Today.
     */
    const val EXTRA_START_PLAN: String = "com.callbackdev.passo.extra.START_PLAN"
}
