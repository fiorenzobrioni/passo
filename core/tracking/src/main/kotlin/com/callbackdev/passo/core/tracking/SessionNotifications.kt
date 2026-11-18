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
import com.callbackdev.passo.core.designsystem.format.countdown
import com.callbackdev.passo.core.designsystem.format.gpsLineText
import com.callbackdev.passo.core.designsystem.format.intervalWord
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.designsystem.format.sessionCadence
import com.callbackdev.passo.core.designsystem.format.sessionEstimates
import com.callbackdev.passo.core.designsystem.format.sessionGoalDescription
import com.callbackdev.passo.core.designsystem.format.sessionHeadline
import com.callbackdev.passo.core.designsystem.format.sessionName
import com.callbackdev.passo.core.designsystem.format.sessionPace
import com.callbackdev.passo.core.designsystem.format.sessionProgress
import com.callbackdev.passo.core.designsystem.format.sessionSteps
import com.callbackdev.passo.core.designsystem.format.sessionZone
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.routes.GpsLine
import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.domain.sessions.IntervalSchedule
import com.callbackdev.passo.core.domain.sessions.fraction
import com.callbackdev.passo.core.domain.sessions.intervalAt
import com.callbackdev.passo.core.domain.sessions.progress
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.UnitPreference
import kotlin.math.roundToInt

/**
 * An outing under way, as the counting notification shows it while it lasts.
 *
 * @property cadence the last half minute's pace; null before there is one, or paused.
 * @property nowMillis when it is drawn: an interval outing's countdown is counted to it.
 * @property measure what an outing measured with GPS shows beyond its stored numbers, and the
 *   line about the satellites: the same in words as on its card.
 */
internal data class SessionNotice(
    val session: Session,
    val cadence: Int?,
    val units: UnitPreference,
    val nowMillis: Long = session.lastStepAtMillis,
    val measure: LiveMeasure = LiveMeasure(),
)

/**
 * The outings' notifications (PLANNING.md §11 Phase 10).
 *
 * - **Under way**, the counting notification itself changes: one ongoing notification, never a
 *   second. Collapsed, the outing's name, its sentence and where it stands («12 of 20 min»);
 *   expanded, the pace against the outing's own, the steps and the estimates, over a bar with
 *   the milestones marked on it. From Android 16 it asks to be a Live Update (a chip in the
 *   status bar and on the lock screen), which the reader can turn off in the system's settings.
 *   An outing measured with GPS says what its card says, in the same words: the distance as a
 *   measure, the pace by the kilometre and the pace now, and the one line about the satellites;
 *   with «Resume GPS» as a third button when the receiver was lost.
 * - **The goal** is its own notification, on the `sessions` channel, which stays as a record:
 *   what was walked, and "Keep going" for a while after. So is **an end by stillness**, with
 *   "Resume" for as long, found by the reader who walks on. The milestones on the way are not
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
        // Paused after "Keep going", the sentence would still be the goal's: Today's card says
        // "Paused" in its own line above it, the notification has no such line.
        val sentence = if (session.state == SessionState.PAUSED && session.reached) {
            context.getString(R.string.session_paused_reached)
        } else {
            res.sessionHeadline(session, format)
        }
        val where = res.sessionProgress(session, format)
        val detail = detailLines(notice, format)
        // An interval outing under way says its interval first, with what is left of it: «Fast ·
        // 1:40». The countdown moves once a second while the screen is on, and is not needed off.
        val interval = session.intervalAt(notice.nowMillis)?.takeIf { session.state == SessionState.ACTIVE }
        val title = interval?.let {
            context.getString(R.string.session_interval_title, res.intervalWord(it.fast), countdown(it.leftMillis))
        } ?: res.sessionName(session)
        builder
            .setContentTitle(title)
            .setContentText(if (interval != null) res.sessionName(session) else sentence)
            .setSubText(where)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(
                when {
                    session.state == SessionState.PAUSED -> context.getString(R.string.session_chip_paused)
                    interval != null -> res.intervalWord(interval.fast)
                    else -> format.percent(progress.coerceAtMost(1.0))
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
        // The receiver lost to a restarted process: one touch gives it back, a touch on a
        // notification being a moment location may be read from (ADR 0016, decision 4).
        if (notice.measure.line == GpsLine.STOPPED) {
            builder.addAction(
                0,
                context.getString(R.string.session_action_resume_gps),
                SessionControl.pendingIntent(context, SessionControl.ACTION_RESUME_GPS, REQUEST_RESUME_GPS),
            )
        }
        val scaled = (progress.coerceIn(0.0, 1.0) * PROGRESS_MAX).roundToInt()
        val schedule = IntervalSchedule.of(session)
        val text = if (interval != null) {
            listOf(res.sessionName(session), sentence, detail).joinToString("\n")
        } else {
            listOf(sentence, detail).joinToString("\n")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // The system draws the milestones on the bar, and the walker riding it; an interval
            // outing's bar is its intervals, slow and fast in two colours, a point at each change.
            builder.setContentText(text)
            val style = NotificationCompat.ProgressStyle()
            if (schedule != null) {
                style
                    .setProgressSegments(intervalSegments(schedule))
                    .setProgressPoints(
                        (1 until schedule.count).map {
                            NotificationCompat.ProgressStyle.Point(scaledAt(schedule.startOf(it), schedule))
                        },
                    )
                    .setProgress(scaledAt(session.totals.movingMillis, schedule))
            } else {
                style
                    .setProgressSegments(listOf(NotificationCompat.ProgressStyle.Segment(PROGRESS_MAX)))
                    .setProgressPoints(
                        session.milestones.sortedBy { it.percent }.map {
                            NotificationCompat.ProgressStyle.Point((it.fraction * PROGRESS_MAX).roundToInt())
                        },
                    )
                    .setProgress(scaled)
            }
            builder.setStyle(
                style.setProgressTrackerIcon(IconCompat.createWithResource(context, R.drawable.ic_progress_walker)),
            )
        } else {
            builder
                .setStyle(NotificationCompat.BigTextStyle().setBigContentTitle(title).bigText(text))
                .setProgress(PROGRESS_MAX, scaled, false)
        }
        return builder
    }

    /** Each interval a segment of its length, the slow ones grey, the fast ones the accent. */
    private fun intervalSegments(schedule: IntervalSchedule): List<NotificationCompat.ProgressStyle.Segment> {
        val slow = ContextCompat.getColor(context, R.color.session_interval_slow)
        var laid = 0
        return (0 until schedule.count).map { index ->
            // Laid by their ends, so the lengths add up to the bar's whole whatever the rounding.
            val end = scaledAt(schedule.endOf(index), schedule)
            NotificationCompat.ProgressStyle.Segment(end - laid).also { segment ->
                if (!schedule.isFast(index)) segment.setColor(slow)
                laid = end
            }
        }
    }

    private fun scaledAt(movingMillis: Long, schedule: IntervalSchedule): Int =
        (movingMillis.coerceIn(0, schedule.totalMillis).toDouble() / schedule.totalMillis * PROGRESS_MAX).roundToInt()

    /**
     * «108 steps/min: on pace · 1,240 steps», then the estimates; for an outing with GPS the
     * distance as what it is, its pace, and the line about the satellites if there is one.
     */
    private fun detailLines(notice: SessionNotice, format: MeasureFormatter): String {
        val res = context.resources
        val session = notice.session
        // A slow interval has no pace to keep: its cadence is said in words.
        val slow = session.intervalAt(notice.nowMillis)?.fast == false
        val pace = when {
            session.state == SessionState.PAUSED -> null
            slow -> res.sessionCadence(notice.cadence, SessionIntensity.FREE, format)
            else -> res.sessionCadence(notice.cadence, session.intensity, format)
        }
        val first = listOfNotNull(pace, res.sessionSteps(session, format)).joinToString(" · ")
        return listOfNotNull(
            first,
            res.sessionEstimates(session, format, notice.measure.openMeters),
            res.sessionPace(session, notice.measure.paceNow, format),
            notice.measure.line?.let(res::gpsLineText),
        ).joinToString("\n")
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
                    listOfNotNull(
                        res.sessionGoalDescription(session, format),
                        summary,
                        res.sessionEstimates(session, format),
                        res.sessionPace(session, paceNow = null, format),
                    ).joinToString("\n"),
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

    /**
     * «Still walking?»: the outing is within a quarter of an hour of the longest it stays open,
     * and the reader is walking (docs/adr/0016-location.md, decision 14). «Keep going» moves that
     * on; unanswered, the outing ends there, says so, and can still be taken back.
     */
    fun capAhead(session: Session, minutes: Int): android.app.Notification {
        val res = context.resources
        val text = res.getQuantityString(R.plurals.session_cap_ahead_text, minutes, minutes)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(context.getString(R.string.session_cap_ahead_title, res.sessionName(session)))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(context.openAppIntent(REQUEST_GOAL))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .addAction(
                0,
                context.getString(R.string.session_action_keep_going),
                SessionControl.pendingIntent(context, SessionControl.ACTION_KEEP_GOING, REQUEST_KEEP_GOING),
            )
            .build()
    }

    /**
     * The receiver of an outing with GPS stopped: told once, with the way back. «Resume GPS»
     * where the permission is there ([canResume]), since a touch on a notification is a moment
     * location may be read from; where it is gone, the app, the one place it can be asked.
     */
    fun gpsStopped(session: Session, canResume: Boolean): android.app.Notification {
        val text = context.getString(
            if (canResume) R.string.session_gps_stopped_text else R.string.session_gps_stopped_text_open,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(
                context.getString(R.string.session_gps_stopped_title, context.resources.sessionName(session)),
            )
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(context.openAppIntent(REQUEST_GOAL))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        if (canResume) {
            builder.addAction(
                0,
                context.getString(R.string.session_action_resume_gps),
                SessionControl.pendingIntent(context, SessionControl.ACTION_RESUME_GPS, REQUEST_RESUME_GPS),
            )
        }
        return builder.build()
    }

    fun postGpsStopped(notification: android.app.Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(ID_GPS_STOPPED, notification)
    }

    /** The receiver is back, or the outing is over. */
    fun cancelGpsStopped() {
        NotificationManagerCompat.from(context).cancel(ID_GPS_STOPPED)
    }

    /** The question, in the shade: under its own id, so the end's notification never hides it. */
    fun postCapAhead(notification: android.app.Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(ID_CAP_AHEAD, notification)
    }

    /** Answered, or overtaken by the outing's end. */
    fun cancelCapAhead() {
        NotificationManagerCompat.from(context).cancel(ID_CAP_AHEAD)
    }

    /**
     * An end the outing came to by itself, noticed as the reader walks on (or looks): after a
     * long stillness, or at the longest it stays open. What it came to, and "Resume" or "Keep
     * going" while it can still be taken back ([withResume]).
     */
    fun endedStill(session: Session, units: UnitPreference, withResume: Boolean): android.app.Notification {
        val atCap = session.end == SessionEnd.CLOSED
        val format = context.measureFormatter(units)
        val res = context.resources
        val summary = listOfNotNull(
            res.sessionSteps(session, format),
            res.sessionZone(session, format),
        ).joinToString(" · ")
        val sentence = res.sessionHeadline(session, format)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(
                context.getString(
                    if (atCap) R.string.session_ended_cap_title else R.string.session_ended_still_title,
                    res.sessionName(session),
                ),
            )
            .setContentText(sentence)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    listOfNotNull(
                        sentence,
                        summary,
                        res.sessionEstimates(session, format),
                        res.sessionPace(session, paceNow = null, format),
                    ).joinToString("\n"),
                ),
            )
            .setContentIntent(context.openAppIntent(REQUEST_GOAL))
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        if (withResume) {
            builder.addAction(
                0,
                // The reader who stopped resumes; the one the clock stopped keeps going.
                context.getString(if (atCap) R.string.session_action_keep_going else R.string.session_action_resume),
                SessionControl.pendingIntent(context, SessionControl.ACTION_KEEP_GOING, REQUEST_KEEP_GOING),
            )
        }
        return builder.build()
    }

    /** The notification of an outing's end that can be taken back: its goal, a long stillness, or its cap. */
    fun ended(session: Session, units: UnitPreference, canReopen: Boolean): android.app.Notification =
        if (session.end == SessionEnd.GOAL) {
            goalReached(session, units, withKeepGoing = canReopen)
        } else {
            endedStill(session, units, withResume = canReopen)
        }

    fun post(notification: android.app.Notification) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(context).notify(ID_END, notification)
    }

    /** Whether the end's notification is still in the shade: one put away is not brought back. */
    fun endShowing(): Boolean = context.getSystemService(NotificationManager::class.java)
        ?.activeNotifications?.any { it.id == ID_END } == true

    fun cancelEnd() {
        NotificationManagerCompat.from(context).cancel(ID_END)
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

    private fun canPost(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    companion object {
        const val CHANNEL_ID = "sessions"

        /** The goal's notification, or the end by stillness's: one outing's end at a time. */
        const val ID_END = 5

        /** «Still walking?», before the longest an outing stays open. */
        const val ID_CAP_AHEAD = 7

        /** «GPS stopped», for an outing with GPS whose receiver a restarted process lost. */
        const val ID_GPS_STOPPED = 8

        private const val PROGRESS_MAX = 1_000
        private const val REQUEST_PAUSE = 10
        private const val REQUEST_RESUME = 11
        private const val REQUEST_STOP = 12
        private const val REQUEST_KEEP_GOING = 13
        private const val REQUEST_GOAL = 14
        private const val REQUEST_RESUME_GPS = 15
    }
}
