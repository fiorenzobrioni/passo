package com.callbackdev.passo.core.tracking

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.quicksettings.Tile
import androidx.core.app.NotificationCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.goals.EveningReminder
import com.callbackdev.passo.core.domain.goals.WeekHeadline
import com.callbackdev.passo.core.domain.goals.WeeklySummary
import com.callbackdev.passo.core.domain.history.Period
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.domain.insights.Trend
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.UnitPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS")
class GoalNotificationsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = GoalNotifications(context)

    private fun Notification.title() = NotificationCompat.getContentTitle(this).toString()

    private fun Notification.text() = NotificationCompat.getContentText(this).toString()

    private fun Notification.bigText() = extras.getCharSequence(Notification.EXTRA_BIG_TEXT).toString()

    private fun week(headline: WeekHeadline) = WeeklySummary(
        week = Period.containing(PeriodScale.WEEK, LocalDate.of(2026, 9, 21), DayOfWeek.MONDAY),
        steps = 57_540,
        countedDays = 7,
        goalDays = 4,
        dailyAverage = 8_220,
        previousAverage = 7_000,
        bestDay = LocalDate.of(2026, 9, 24),
        bestDaySteps = 12_040,
        distanceMeters = 41_230.0,
        activeKcal = 2_140.0,
        headline = headline,
    )

    @Test
    fun `the channel is one, at the default importance, apart from the counting one`() {
        notifications.ensureChannel()

        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel("goals")
        assertThat(channel.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
        assertThat(channel.name.toString()).isEqualTo("Goals and reminders")
    }

    @Test
    fun `goal reached says the goal, the time and the streak it extends`() {
        val notification = notifications.goalReached(GoalReachedContent(8_000, 17 * 60 + 42, 5), UnitPreference.METRIC)

        assertThat(notification.title()).isEqualTo("Goal reached: 8,000 steps")
        assertThat(notification.text()).isEqualTo("Reached at 5:42 PM: 5 days in a row at the goal.")
        assertThat(notification.channelId).isEqualTo("goals")
        assertThat(notification.flags and Notification.FLAG_AUTO_CANCEL).isNotEqualTo(0)
    }

    @Test
    fun `one day at the goal is not called a streak`() {
        val notification = notifications.goalReached(GoalReachedContent(8_000, 9 * 60 + 5, 1), UnitPreference.METRIC)

        assertThat(notification.text()).isEqualTo("Reached at 9:05 AM.")
    }

    @Test
    fun `the evening reminder says the steps left, then the walk they take`() {
        val nudge = checkNotNull(EveningReminder.check(steps = 5_660, goalSteps = 8_000, thresholdPercent = 100))
        val notification = notifications.eveningReminder(nudge, UnitPreference.METRIC)

        assertThat(notification.title()).isEqualTo("2,340 steps to go")
        assertThat(notification.text()).isEqualTo("About 24 minutes of brisk walking gets you to 8,000.")
        assertThat(notification.bigText().lines()).containsExactly(
            "About 24 minutes of brisk walking gets you to 8,000.",
            "So far 5,660: 70% of the goal.",
        ).inOrder()
        assertThat(notification.extras.getInt(Notification.EXTRA_PROGRESS)).isEqualTo(5_660)
    }

    @Test
    fun `the weekly summary opens with the week against the one before`() {
        val notification = notifications.weeklySummary(
            week(WeekHeadline.VersusWeekBefore(Trend.UP, 0.174)),
            UnitPreference.METRIC,
        )

        assertThat(notification.title()).isEqualTo("Last week: 17% more steps than the week before")
        assertThat(notification.text()).isEqualTo("57,540 steps, 8,220 a day on average")
        val lines = notification.bigText().lines()
        assertThat(lines.subList(0, 3)).containsExactly(
            "57,540 steps, 8,220 a day on average",
            "4 of 7 days at the goal",
            "Best day: Thursday, 12,040 steps",
        ).inOrder()
        assertThat(lines[3]).matches("Estimated: 41\\.2[0-9]? km · 2,140 kcal")
    }

    @Test
    fun `a week at the goal every day, or a first week, does not repeat the days in the body`() {
        val every = notifications.weeklySummary(week(WeekHeadline.EveryDay), UnitPreference.METRIC)
        assertThat(every.title()).isEqualTo("Last week: the goal every day")
        assertThat(every.bigText()).doesNotContain("days at the goal")

        val first = notifications.weeklySummary(week(WeekHeadline.GoalDays(2, 4)), UnitPreference.METRIC)
        assertThat(first.title()).isEqualTo("Last week: 2 of 4 days at the goal")
        assertThat(first.bigText().lines()).hasSize(3)
    }

    @Test
    fun `fewer steps and about the same are said as such`() {
        val down = notifications.weeklySummary(
            week(WeekHeadline.VersusWeekBefore(Trend.DOWN, -0.08)),
            UnitPreference.METRIC,
        )
        assertThat(down.title()).isEqualTo("Last week: 8% fewer steps than the week before")

        val steady = notifications.weeklySummary(
            week(WeekHeadline.VersusWeekBefore(Trend.STEADY, 0.02)),
            UnitPreference.METRIC,
        )
        assertThat(steady.title()).isEqualTo("Last week: about as many steps as the week before")
    }

    @Test
    fun `the tile shows the count and the goal while counting`() {
        val face = TileFace.of(context, CountingState.COUNTING, 6_240, 8_000, UnitPreference.METRIC)

        assertThat(face.state).isEqualTo(Tile.STATE_ACTIVE)
        assertThat(face.label).isEqualTo("6,240 steps")
        assertThat(face.subtitle).isEqualTo("78% of the goal")
        assertThat(TileFace.of(context, CountingState.COUNTING, 9_000, 8_000, UnitPreference.METRIC).subtitle)
            .isEqualTo("Goal reached")
    }

    @Test
    fun `a tile that is not counting says why`() {
        val paused = TileFace.of(context, CountingState.PAUSED, 3_100, 8_000, UnitPreference.METRIC)
        assertThat(paused.state).isEqualTo(Tile.STATE_INACTIVE)
        assertThat(paused.label).isEqualTo("3,100 steps")
        assertThat(paused.subtitle).isEqualTo("Paused")

        val permission = TileFace.of(context, CountingState.PERMISSION_NEEDED, 0, 8_000, UnitPreference.METRIC)
        assertThat(permission.label).isEqualTo("Steps today")
        assertThat(permission.subtitle).isEqualTo("No permission")

        val sensor = TileFace.of(context, CountingState.NO_SENSOR, 0, 8_000, UnitPreference.METRIC)
        assertThat(sensor.state).isEqualTo(Tile.STATE_UNAVAILABLE)
    }
}
