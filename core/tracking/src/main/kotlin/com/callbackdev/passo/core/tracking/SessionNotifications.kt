package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionCadence
import com.callbackdev.passo.core.designsystem.format.sessionEstimates
import com.callbackdev.passo.core.designsystem.format.sessionGoalDescription
import com.callbackdev.passo.core.designsystem.format.sessionHeadline
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.sessionProgress
import com.callbackdev.passo.core.designsystem.format.sessionSteps
import com.callbackdev.passo.core.designsystem.format.sessionZone
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.sessions.fraction
import com.callbackdev.passo.core.domain.sessions.progress
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.UnitPreference
import kotlin.math.roundToInt

/**
 * An outing under way, as the counting notification shows it while it lasts.
 *
 * @property cadence the last half minute's pace; null before there is one, or paused.
 */
internal data class SessionNotice(val session: Session, val cadence: Int?, val units: UnitPreference)

/**
 * The outings' notifications (PLANNING.md §11 Phase 10).
 *
 * - **Under way**, the counting notification itself changes: one ongoing notification, never a
 *   second. Collapsed, the outing's name, its sentence and where it stands («12 of 20 min»);
 *   expanded, the pace against the outing's own, the steps and the estimates, over a bar with
 *   the milestones marked on it. From Android 16 it asks to be a Live Update (a chip in the
 *   status bar and on the lock screen), which the reader can turn off in the system's settings.
 * - **The goal** is its own notification, on the `sessions` channel, which stays as a record:
 *   what was walked, and "Keep going" for a while after. The milestones on the way are not
 *   notifications (the shade would fill with them): the ongoing one moves, and the phone
 *   vibrates.
 *
 * The `sessions` channel makes no sound and no vibration of its own: the outing's vibrations are
 * Passo's patterns ([SessionHaptics]), and a channel buzz on top would blur them. The reader can
 * give it a sound in the system's settings.
 */
internal class SessionNotifications(private val context: Context) {
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.sessions_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.sessions_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /** The counting notification while [notice]'s outing is under way or paused. */
    fun ongoing(builder: NotificationCompat.Builder, notice: SessionNotice): NotificationCompat.Builder {
        val session = notice.session
        val format = context.measureFormatter(notice.units)
        val res = context.resources
        val progress = session.progress()
        val sentence = res.sessionHeadline(session, format)
        val where = res.sessionProgress(session, format)
        val detail = detailLines(notice, format)
        builder
            .setContentTitle(res.sessionName(session))
            .setContentText(sentence)
            .setSubText(where)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(
                if (session.state == SessionState.PAUSED) {
                    context.getString(R.string.session_chip_paused)
                } else {
                    format.percent(progress.coerceAtMost(1.0))
                },
            )
        if (session.state == SessionState.PAUSED) {
            builder.addAction(
                0,
                context.getString(R.string.session_action_resume),
                SessionControl.pendingIntent(context, SessionControl.ACTION_RESUME, REQUEST_RESUME),
            )
        } else {
            builder.addAction(
                0,
                context.getString(R.string.session_action_pause),
                SessionControl.pendingIntent(context, SessionControl.ACTION_PAUSE, REQUEST_PAUSE),
            )
        }
        builder.addAction(
            0,
            context.getString(R.string.session_action_stop),
            SessionControl.pendingIntent(context, SessionControl.ACTION_STOP, REQUEST_STOP),
        )
        val scaled = (progress.coerceIn(0.0, 1.0) * PROGRESS_MAX).roundToInt()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // The system draws the milestones on the bar, and the walker riding it.
            builder.setContentText(listOf(sentence, detail).joinToString("\n"))
            builder.setStyle(
                NotificationCompat.ProgressStyle()
                    .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(PROGRESS_MAX)))
                    .setProgressPoints(
                        session.milestones.sortedBy { it.percent }.map {
                            NotificationCompat.ProgressStyle.Point((it.fraction * PROGRESS_MAX).roundToInt())
                        },
                    )
                    .setProgress(scaled)
                    .setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_stat_steps)),
            )
        } else {
            builder
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .setBigContentTitle(res.sessionName(session))
                        .bigText(listOf(sentence, detail).joinToString("\n")),
                )
                .setProgress(PROGRESS_MAX, scaled, false)
        }
        return builder
    }

    /** «108 steps/min: on pace · 1,240 steps», then the estimates. */
    private fun detailLines(notice: SessionNotice, format: MeasureFormatter): String {
        val res = context.resources
        val session = notice.session
        val pace = if (session.state == SessionState.PAUSED) null else res.sessionCadence(notice.cadence, session.intensity, format)
        val first = listOfNotNull(pace, res.sessionSteps(session, format)).joinToString(" · ")
        return listOf(first, res.sessionEstimates(session, format)).joinToString("\n")
    }

    /**
     * The goal reached: what the outing set out to do, what it came to, and "Keep going" while
     * it can still reopen it ([withKeepGoing]).
     */
    fun goalReached(session: Session, units: UnitPreference, withKeepGoing: Boolean): android.app.Notification {
        val format = context.measureFormatter(units)
        val res = context.resources
        val summary = listOfNotNull(
            res.sessionSteps(session, format),
            res.sessionZone(session, format),
        ).joinToString(" · ")
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(context.getString(R.string.session_goal_title, res.sessionName(session)))
            .setContentText(res.sessionGoalDescription(session, format))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    listOf(res.sessionGoalDescription(session, format), summary, res.sessionEstimates(session, format))
                        .joinToString("\n"),
                ),
            )
            .setContentIntent(context.openAppIntent(REQUEST_GOAL))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        if (withKeepGoing) {
            builder.addAction(
                0,
                context.getString(R.string.session_action_keep_going),
                SessionControl.pendingIntent(context, SessionControl.ACTION_KEEP_GOING, REQUEST_KEEP_GOING),
            )
        }
        return builder.build()
    }

    fun post(notification: android.app.Notification) {
        if (!canPost()) return
        NotificationManagerCompat.from(context).notify(ID_GOAL, notification)
    }

    /** Whether the goal's notification is still in the shade: one put away is not brought back. */
    fun goalShowing(): Boolean = context.getSystemService(NotificationManager::class.java)
        ?.activeNotifications?.any { it.id == ID_GOAL } == true

    fun cancelGoal() {
        NotificationManagerCompat.from(context).cancel(ID_GOAL)
    }

    /**
     * Whether a signal would reach the reader: the notification permission, Passo's notifications
     * and the outings' channel all on. The vibrations follow it too: a reader who silenced the
     * outings in the system's settings does not want them buzzing either.
     */
    fun signalsAllowed(): Boolean {
        val manager = NotificationManagerCompat.from(context)
        if (!canPost() || !manager.areNotificationsEnabled()) return false
        return manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    private fun canPost(): Boolean = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "sessions"
        const val ID_GOAL = 5

        private const val PROGRESS_MAX = 1_000
        private const val REQUEST_PAUSE = 10
        private const val REQUEST_RESUME = 11
        private const val REQUEST_STOP = 12
        private const val REQUEST_KEEP_GOING = 13
        private const val REQUEST_GOAL = 14
    }
}
