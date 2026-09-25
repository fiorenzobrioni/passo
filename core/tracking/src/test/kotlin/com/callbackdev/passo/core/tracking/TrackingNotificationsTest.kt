package com.callbackdev.passo.core.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.format.measureFormatter
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS")
class TrackingNotificationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = TrackingNotifications(context)
    private val manager = context.getSystemService(NotificationManager::class.java)

    /** [minutes] minutes of 100 steps from 9:00, read at 10:00. */
    private fun day(minutes: Int, goal: Int = 8_000): TodayOverview = TodayOverview.of(
        minutes = List(minutes) { DayMinute(9 * 60 + it, 100) },
        profile = Profile(),
        goalSteps = goal,
        nowMinute = 600.0,
        typical = null,
    )

    @Test
    fun `before the count is read it says only that it counts`() {
        val notification = notifications.build(NotificationContent(null))

        assertThat(NotificationCompat.getContentTitle(notification).toString()).isEqualTo("Counting your steps")
        assertThat(NotificationCompat.getContentText(notification)).isNull()
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)).isNull()
    }

    @Test
    fun `with the count alone it has no expanded form`() {
        val notification = notifications.build(NotificationContent(6_000))

        assertThat(NotificationCompat.getContentText(notification).toString()).isEqualTo("6,000 steps today")
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT)).isNull()
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX)).isEqualTo(0)
    }

    @Test
    fun `expanded, it opens with the way to the goal, then the goal and the estimates`() {
        val notification = notifications.build(NotificationContent(6_000, day(60), UnitPreference.METRIC))

        assertThat(NotificationCompat.getContentText(notification).toString()).isEqualTo("6,000 steps today")
        assertThat(notification.extras.getCharSequence(Notification.EXTRA_TITLE_BIG).toString())
            .isEqualTo("6,000 steps today")
        val lines = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString().lines()
        assertThat(lines).hasSize(4)
        assertThat(lines[0]).isEqualTo("2,000 steps to go:")
        assertThat(lines[1]).isEqualTo("about 20 minutes of brisk walking")
        assertThat(lines[2]).isEqualTo("75% of 8,000 · 60 active minutes")
        assertThat(lines[3]).matches("Estimated: [0-9.]+ km · [0-9,]+ kcal")
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX)).isEqualTo(8_000)
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS)).isEqualTo(6_000)
    }

    @Test
    fun `a long way to go is written in hours`() {
        val lines = notifications.expandedText(day(0, goal = 9_000), context.measureFormatter(UnitPreference.METRIC))
            .lines()

        assertThat(lines[0]).isEqualTo("9,000 steps to go:")
        assertThat(lines[1]).isEqualTo("about 1 h 30 min of brisk walking")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian the way to the goal breaks after the colon too`() {
        val lines = notifications.expandedText(day(60), context.measureFormatter(UnitPreference.METRIC)).lines()

        assertThat(lines[0]).isEqualTo("Ne mancano 2.000:")
        assertThat(lines[1]).isEqualTo("circa 20 minuti a passo svelto")
    }

    @Test
    fun `past the goal it says when it was reached, and the bar stays full`() {
        val notification = notifications.build(NotificationContent(10_000, day(100), UnitPreference.METRIC))

        val lines = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString().lines()
        assertThat(lines[0]).startsWith("Goal reached at ")
        assertThat(lines[1]).startsWith("125% of 8,000")
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS)).isEqualTo(8_000)
    }

    @Test
    fun `the visibility is the reader's choice on the channel`() {
        assertThat(CountingNotification.visibility(context)).isEqualTo(NotificationVisibility.SHOWN)

        notifications.ensureChannel()
        assertThat(CountingNotification.visibility(context)).isEqualTo(NotificationVisibility.SHOWN)

        manager.createNotificationChannel(channel(NotificationManager.IMPORTANCE_MIN))
        assertThat(CountingNotification.visibility(context)).isEqualTo(NotificationVisibility.MINIMIZED)

        manager.createNotificationChannel(channel(NotificationManager.IMPORTANCE_NONE))
        assertThat(CountingNotification.visibility(context)).isEqualTo(NotificationVisibility.OFF)
    }

    @Test
    fun `with the app's notifications off it is off, and the app's page opens`() {
        notifications.ensureChannel()
        assertThat(CountingNotification.settingsIntent(context).action)
            .isEqualTo(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)

        shadowOf(manager).setNotificationsEnabled(false)
        assertThat(CountingNotification.visibility(context)).isEqualTo(NotificationVisibility.OFF)
        assertThat(CountingNotification.settingsIntent(context).action)
            .isEqualTo(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
    }

    // Robolectric's manager stores the importance as given; on a phone only the reader lowers it.
    private fun channel(importance: Int) =
        NotificationChannel(TrackingNotifications.CHANNEL_ID, "Step counting", importance)
}
