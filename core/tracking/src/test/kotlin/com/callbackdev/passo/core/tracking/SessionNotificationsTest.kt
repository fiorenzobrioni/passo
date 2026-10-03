package com.callbackdev.passo.core.tracking

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
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

    private fun ongoing(session: Session, cadence: Int? = 108) = notifications.build(
        NotificationContent(6_000, session = SessionNotice(session, cadence, UnitPreference.METRIC)),
    )

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
    fun `on a city walk it is named by its city and tells the place ahead`() {
        val walk = checkNotNull(
            SessionPlans.startWalk(Ways.of(WayId.LONDON_PALACE_TOWER), 0, SessionVoice.HEADPHONES, 0, 20_000),
        ).copy(totals = session.totals.copy(distanceMeters = 1_300.0))
        val notification = ongoing(walk, cadence = null)

        assertThat(NotificationCompat.getContentTitle(notification).toString()).isEqualTo("A walk in London")
        assertThat(NotificationCompat.getContentText(notification).toString())
            .startsWith("Next: Trafalgar Square, 540 m")
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

        // Paused after "Keep going": the pause, and the goal already behind it.
        val pastGoal = ongoing(session.copy(state = SessionState.PAUSED, pausedAtMillis = 1, reachedAtMillis = 1))
        assertThat(NotificationCompat.getContentText(pastGoal).toString()).isEqualTo("Paused, with the goal reached.")
        assertThat(pastGoal.actions.map { it.title.toString() }).containsExactly("Resume", "Stop").inOrder()
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
    fun `an end by stillness says so, and offers to resume while it can`() {
        val still = session.copy(state = SessionState.FINISHED, end = SessionEnd.IDLE, endedAtMillis = 1)
        val sessions = SessionNotifications(context)
        val notification = sessions.ended(still, UnitPreference.METRIC, canReopen = true)

        assertThat(NotificationCompat.getContentTitle(notification).toString())
            .isEqualTo("Brisk walk: ended after a long stop")
        assertThat(NotificationCompat.getContentText(notification).toString())
            .isEqualTo("Ended by itself at 60% of the goal, after a long stop.")
        val expanded = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertThat(expanded).contains("1,240 steps")
        assertThat(notification.actions.single().title.toString()).isEqualTo("Resume")
        assertThat(sessions.ended(still, UnitPreference.METRIC, canReopen = false).actions).isNull()
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
        // The end by stillness: two long pulses, like none of the above.
        assertThat(SessionHaptics.endedStillPattern().toList()).containsExactly(0L, 500L, 300L, 500L).inOrder()
        // A walk's place: one short pulse (a walk has no quarters).
        assertThat(SessionHaptics.placePattern().toList()).containsExactly(0L, 180L).inOrder()
    }

    @Test
    fun `faster and slower are unlike every other signal`() {
        val faster = SessionHaptics.intervalPattern(fast = true).toList()
        val slower = SessionHaptics.intervalPattern(fast = false).toList()
        assertThat(faster).containsExactly(0L, 70L, 80L, 70L, 80L, 70L, 80L, 70L).inOrder()
        assertThat(slower).containsExactly(0L, 450L, 250L, 120L).inOrder()
        val others = SessionMilestone.entries.map { SessionHaptics.pattern(it).toList() } +
            listOf(SessionHaptics.endedStillPattern().toList(), SessionHaptics.placePattern().toList())
        assertThat(others).containsNoneOf(faster, slower)
    }

    @Test
    fun `an interval walk says its interval and its countdown, over a bar of its intervals`() {
        val start = checkNotNull(SessionPlans.start(SessionPlans.JAPANESE_WALKING, 0, 20_000, 0, 8_000))
        // Four minutes and twenty seconds in: the first fast interval, 1:40 left.
        val going = start.copy(
            id = 4,
            totals = SessionTotals(steps = 460, movingMillis = 260_000),
            lastStepAtMillis = 300_000,
        )
        val notification = notifications.build(
            NotificationContent(
                6_000,
                session = SessionNotice(going, 112, UnitPreference.METRIC, nowMillis = 300_000),
            ),
        )

        assertThat(NotificationCompat.getContentTitle(notification).toString()).isEqualTo("Fast · 1:40")
        assertThat(NotificationCompat.getSubText(notification).toString()).isEqualTo("Set 1 of 5")
        val expanded = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertThat(expanded).contains("Japanese walking")
        assertThat(expanded).contains("Fast: 1:40 left.")
        assertThat(expanded).contains("112 steps/min: on pace")
    }

    @Test
    fun `in a slow interval the cadence is told in words, not against the fast pace`() {
        val start = checkNotNull(SessionPlans.start(SessionPlans.JAPANESE_WALKING, 0, 20_000, 0, 8_000))
        val slow = start.copy(
            id = 4,
            totals = SessionTotals(steps = 100, movingMillis = 60_000),
            lastStepAtMillis = 70_000,
        )
        val notification = notifications.build(
            NotificationContent(6_000, session = SessionNotice(slow, 88, UnitPreference.METRIC, nowMillis = 70_000)),
        )
        val expanded = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()
        assertThat(NotificationCompat.getContentTitle(notification).toString()).isEqualTo("Slow · 2:00")
        assertThat(expanded).contains("88 steps/min, relaxed")
        assertThat(expanded).doesNotContain("below your pace")
    }
}
