package com.callbackdev.passo.core.tracking

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UnitPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS")
class SessionNotificationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = TrackingNotifications(context)

    private val session = Session(
        id = 3,
        planId = 1,
        name = null,
        goalKind = SessionGoalKind.TIME,
        goalValue = 20,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        vibrate = true,
        localEpochDay = 20_000,
        startedAtMillis = 0,
        totals = SessionTotals(
            steps = 1_240,
            movingMillis = 12 * 60_000,
            zoneMillis = 11 * 60_000,
            distanceMeters = 868.0,
            activeKcal = 31.0,
        ),
    )

    private fun ongoing(session: Session, cadence: Int? = 108) =
        notifications.build(NotificationContent(6_000, session = SessionNotice(session, cadence, UnitPreference.METRIC)))

    @Test
    fun `under way, the counting notification is the outing's`() {
        val notification = ongoing(session)

        assertThat(NotificationCompat.getContentTitle(notification).toString()).isEqualTo("Brisk walk")
        assertThat(NotificationCompat.getContentText(notification).toString()).startsWith("Past halfway: 8 min to go.")
        assertThat(NotificationCompat.getSubText(notification).toString()).isEqualTo("12 of 20 min")
        val expanded = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertThat(expanded).contains("108 steps/min: on pace · 1,240 steps")
        assertThat(expanded).contains("Estimated: 0.86 km · 31 kcal")
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS)).isEqualTo(600)
        assertThat(notification.actions.map { it.title.toString() }).containsExactly("Pause", "Stop").inOrder()
        assertThat(notification.category).isEqualTo(Notification.CATEGORY_WORKOUT)
        assertThat(NotificationCompat.getOngoing(notification)).isTrue()
    }

    @Test
    fun `below the outing's cadence it says so`() {
        val expanded = ongoing(session, cadence = 92).extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertThat(expanded).contains("92 steps/min: below your pace")
    }

    @Test
    fun `paused, it offers to resume and states no pace`() {
        val notification = ongoing(session.copy(state = SessionState.PAUSED, pausedAtMillis = 1))

        assertThat(NotificationCompat.getContentText(notification).toString())
            .isEqualTo("Paused. Resume when you are ready.")
        assertThat(notification.actions.map { it.title.toString() }).containsExactly("Resume", "Stop").inOrder()
        val expanded = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertThat(expanded).doesNotContain("steps/min")
    }

    @Test
    fun `the goal reached says what was walked, and keeps going while it can`() {
        val done = session.copy(
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            reachedAtMillis = 1,
            totals = session.totals.copy(movingMillis = 20 * 60_000, zoneMillis = 17 * 60_000, steps = 2_140),
        )
        val sessions = SessionNotifications(context)
        val notification = sessions.goalReached(done, UnitPreference.METRIC, withKeepGoing = true)

        assertThat(NotificationCompat.getContentTitle(notification).toString()).isEqualTo("Brisk walk: goal reached")
        assertThat(NotificationCompat.getContentText(notification).toString()).isEqualTo("20 min at a brisk pace")
        val expanded = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertThat(expanded).contains("2,140 steps · 17 of 20 min at the pace you set")
        assertThat(notification.actions.single().title.toString()).isEqualTo("Keep going")
        assertThat(sessions.goalReached(done, UnitPreference.METRIC, withKeepGoing = false).actions).isNull()
    }

    @Test
    fun `the outings' channel makes no sound and no vibration of its own`() {
        SessionNotifications(context).ensureChannel()
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(SessionNotifications.CHANNEL_ID)
        assertThat(channel.sound).isNull()
        assertThat(channel.shouldVibrate()).isFalse()
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
    }

    @Test
    fun `each signal vibrates in its own count`() {
        assertThat(SessionHaptics.pattern(SessionMilestone.QUARTER).toList()).containsExactly(0L, 180L).inOrder()
        assertThat(SessionHaptics.pattern(SessionMilestone.HALF).toList())
            .containsExactly(0L, 180L, 220L, 180L).inOrder()
        assertThat(SessionHaptics.pattern(SessionMilestone.THREE_QUARTERS).toList())
            .containsExactly(0L, 180L, 220L, 180L, 220L, 180L).inOrder()
        assertThat(SessionHaptics.pattern(SessionMilestone.GOAL).toList()).containsExactly(0L, 900L).inOrder()
    }
}
