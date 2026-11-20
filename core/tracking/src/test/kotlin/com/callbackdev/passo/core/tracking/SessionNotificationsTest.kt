package com.callbackdev.passo.core.tracking

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.routes.GpsLine
import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.sessions.capMillis
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
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

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

    private val open = session.copy(
        id = 7,
        goalKind = SessionGoalKind.OPEN,
        goalValue = 0,
        intensity = SessionIntensity.FREE,
        milestones = emptySet(),
        totals = session.totals.copy(movingMillis = 25 * 60_000, distanceMeters = 2_100.0),
    )

    @Test
    fun `an open outing's notification has no bar - its sentence, and how far it has come`() {
        val notification = ongoing(open)

        assertThat(NotificationCompat.getContentTitle(notification).toString()).isEqualTo("Walk")
        assertThat(NotificationCompat.getContentText(notification).toString()).startsWith("25 minutes on your feet.")
        assertThat(NotificationCompat.getSubText(notification).toString()).isEqualTo("2.10 km, 25 min")
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX)).isEqualTo(0)
        assertThat(notification.actions.map { it.title.toString() }).containsExactly("Pause", "Stop").inOrder()
        assertThat(NotificationCompat.getOngoing(notification)).isTrue()
    }

    /** The app's launcher activity, which this module does not have: what the notifications' touches open. */
    private fun withLauncher() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = context.packageName
                name = "com.callbackdev.passo.MainActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(launcher, info)
    }

    @Test
    fun `touched while an outing counts, the notification opens the outing's own page`() {
        withLauncher()
        val intent = shadowOf(ongoing(open).contentIntent).savedIntent
        assertThat(intent.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L)).isEqualTo(7L)
        // A goal outing's too.
        val goal = shadowOf(ongoing(session).contentIntent).savedIntent
        assertThat(goal.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L)).isEqualTo(3L)
    }

    @Test
    fun `the question before the cap and GPS stopped open the outing's page, where they are answered`() {
        withLauncher()
        val sessions = SessionNotifications(context)
        val asked = sessions.capAhead(open, nowMillis = open.capMillis - 10 * 60_000L)
        assertThat(shadowOf(asked.contentIntent).savedIntent.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L))
            .isEqualTo(7L)
        val stopped = sessions.gpsStopped(open.copy(gps = true), canResume = true)
        assertThat(shadowOf(stopped.contentIntent).savedIntent.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L))
            .isEqualTo(7L)
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
    fun `before the longest an outing stays open it asks, with keep going`() {
        val sessions = SessionNotifications(context)
        val cap = session.capMillis
        val notification = sessions.capAhead(session, nowMillis = cap - 10 * 60_000L)

        assertThat(NotificationCompat.getContentTitle(notification).toString())
            .isEqualTo("Brisk walk: still walking?")
        val clock = context.clockTime(Instant.ofEpochMilli(cap).atZone(ZoneId.systemDefault()).toLocalTime())
        assertThat(NotificationCompat.getContentText(notification).toString())
            .isEqualTo("This outing ends at $clock. Keep going to carry on.")
        assertThat(notification.actions.single().title.toString()).isEqualTo("Keep going")
    }

    @Test
    fun `the question's time is the cap's own - a countdown to it, and gone when it comes`() {
        val cap = session.capMillis
        val notification = SessionNotifications(context).capAhead(session, nowMillis = cap - 10 * 60_000L)

        // However late it is read, it is not a figure counted when it was posted.
        assertThat(notification.`when`).isEqualTo(cap)
        assertThat(notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)).isTrue()
        assertThat(notification.extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN)).isTrue()
        assertThat(notification.timeoutAfter).isEqualTo(10 * 60_000L)
    }

    @Test
    fun `the question unanswered, the end says so and offers to keep going while it can`() {
        val closed = session.copy(state = SessionState.FINISHED, end = SessionEnd.CLOSED, endedAtMillis = 1)
        val sessions = SessionNotifications(context)
        val notification = sessions.ended(closed, UnitPreference.METRIC, canReopen = true)

        assertThat(NotificationCompat.getContentTitle(notification).toString())
            .isEqualTo("Brisk walk: ended at its time limit")
        // The clock stopped it, not the reader: they keep going, they do not resume.
        assertThat(notification.actions.single().title.toString()).isEqualTo("Keep going")
        assertThat(sessions.ended(closed, UnitPreference.METRIC, canReopen = false).actions).isNull()
    }

    @Test
    fun `the question has an id of its own, so the end's notification never hides it`() {
        assertThat(SessionNotifications.ID_CAP_AHEAD).isNotEqualTo(SessionNotifications.ID_END)
        assertThat(SessionNotifications.ID_CAP_AHEAD).isNotEqualTo(TrackingNotifications.NOTIFICATION_ID)
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
        // An open outing's mark, a kilometre or ten minutes: one short pulse of 180 ms, a quarter's
        // own, which an open outing cannot be taken for: it has no quarters.
        assertThat(SessionHaptics.markPattern().toList()).containsExactly(0L, 180L).inOrder()
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

    /** The same walk measured with GPS: 1.19 km in twelve minutes of motion, 10:05 a kilometre. */
    private val measured = session.copy(
        totals = session.totals.copy(distanceMeters = 1_190.0, activeKcal = 44.0),
        gps = true,
        stepsMeters = 868.0,
        measuredShare = 0.97,
    )

    private fun ongoing(session: Session, measure: LiveMeasure) = notifications.build(
        NotificationContent(6_000, session = SessionNotice(session, 108, UnitPreference.METRIC, measure = measure)),
    )

    private fun expanded(notification: Notification) =
        notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()

    @Test
    fun `an outing with GPS says the same as its card, in words - measured, the pace and the pace now`() {
        val notification = ongoing(measured, LiveMeasure(paceNow = 588.0))

        assertThat(expanded(notification)).contains("Measured: 1.19 km · estimated: 44 kcal")
        assertThat(expanded(notification)).contains("Pace 10:05\u00A0min/km · now 9:48\u00A0min/km")
        assertThat(expanded(notification)).doesNotContain("Estimated: 1.19")
        // Measuring has no line of its own, and nothing more to touch.
        assertThat(notification.actions.map { it.title.toString() }).containsExactly("Pause", "Stop").inOrder()
    }

    @Test
    fun `finding the satellites - the notification says it, and the steps' stretch is in the distance`() {
        val starting = measured.copy(
            totals = SessionTotals(steps = 70, movingMillis = 40_000, zoneMillis = 40_000),
            stepsMeters = 51.0,
            measuredShare = 0.0,
        )
        val notification = ongoing(starting, LiveMeasure(openMeters = 51.0, line = GpsLine.FINDING))

        assertThat(expanded(notification)).contains("0.05 km from your steps · estimated: 0 kcal")
        assertThat(expanded(notification)).contains("Finding the satellites. Your steps count meanwhile.")
        assertThat(expanded(notification)).doesNotContain("Pace ")
    }

    @Test
    fun `more than a tenth from the steps - the share, and no pace of the whole`() {
        val notification = ongoing(measured.copy(measuredShare = 0.7), LiveMeasure(line = GpsLine.SIGNAL_LOST))

        assertThat(expanded(notification)).contains("1.19 km, 30% from your steps · estimated: 44 kcal")
        assertThat(expanded(notification)).contains("No GPS signal here. Your steps cover this stretch.")
        assertThat(expanded(notification)).doesNotContain("Pace ")
    }

    @Test
    fun `GPS stopped - said on the counting notification, with Resume GPS as its third button`() {
        val notification = ongoing(measured, LiveMeasure(line = GpsLine.STOPPED))

        assertThat(expanded(notification)).contains("GPS stopped. Your outing goes on from your steps.")
        assertThat(notification.actions.map { it.title.toString() })
            .containsExactly("Pause", "Stop", "Resume GPS")
            .inOrder()
    }

    @Test
    fun `the permission gone or location off - said, with no button that would be refused`() {
        val gone = ongoing(measured, LiveMeasure(line = GpsLine.PERMISSION_GONE))
        val off = ongoing(measured, LiveMeasure(line = GpsLine.LOCATION_OFF))

        assertThat(expanded(gone)).contains("Passo can no longer read your location.")
        assertThat(expanded(off)).contains("Location is off on this phone.")
        assertThat(gone.actions.map { it.title.toString() }).containsExactly("Pause", "Stop").inOrder()
        assertThat(off.actions.map { it.title.toString() }).containsExactly("Pause", "Stop").inOrder()
    }

    @Test
    fun `started on the steps - the notification says why, and the distance is an estimate`() {
        val onSteps = session.copy(gps = false)
        val notification = ongoing(onSteps, LiveMeasure(line = GpsLine.ON_STEPS_NO_PERMISSION))

        assertThat(expanded(notification)).contains("Estimated: 0.86 km · 31 kcal")
        assertThat(expanded(notification)).contains("This outing counts from your steps")
    }

    @Test
    fun `the goal of an outing with GPS is told with its measure and its pace`() {
        val done = measured.copy(
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            endedAtMillis = 22 * 60_000L,
            reachedAtMillis = 22 * 60_000L,
        )
        val notification = SessionNotifications(context).goalReached(done, UnitPreference.METRIC, withKeepGoing = true)

        assertThat(expanded(notification)).contains("Measured: 1.19 km · estimated: 44 kcal")
        assertThat(expanded(notification)).contains("Pace 10:05\u00A0min/km")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian - misurati, andatura`() {
        val notification = ongoing(measured, LiveMeasure(paceNow = 588.0, line = GpsLine.SIGNAL_LOST))

        assertThat(expanded(notification)).contains("Misurati: 1,19 km · stima: 44 kcal")
        assertThat(expanded(notification)).contains("Andatura 10:05\u00A0min/km · ora 9:48\u00A0min/km")
        assertThat(expanded(notification)).contains("Qui il segnale GPS non arriva.")
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
