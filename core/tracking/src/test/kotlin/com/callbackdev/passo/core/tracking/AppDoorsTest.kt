package com.callbackdev.passo.core.tracking

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.goals.EveningReminder
import com.callbackdev.passo.core.domain.goals.WeekHeadline
import com.callbackdev.passo.core.domain.goals.WeeklySummary
import com.callbackdev.passo.core.domain.history.Period
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Every door into the app from outside it opens the page that holds what it shows (PLANNING.md
 * §11 Phase 23, step 23.1): the intent each surface builds, with its extras.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS")
class AppDoorsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** The app's launcher activity, which this module does not have: what the doors open. */
    @Before
    fun withLauncher() {
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        val info = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = context.packageName
                name = "com.callbackdev.passo.MainActivity"
            }
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(launcher, info)
    }

    private val outing = Session(
        id = 7,
        planId = 1,
        name = null,
        goalKind = SessionGoalKind.OPEN,
        goalValue = 0,
        intensity = SessionIntensity.FREE,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = 20_000,
        startedAtMillis = 0,
    )

    private val PendingIntent?.opened: Intent get() = shadowOf(checkNotNull(this)).savedIntent

    private val Notification.opened: Intent get() = contentIntent.opened

    private fun Intent.asksForToday() = getBooleanExtra(AppDoors.EXTRA_OPEN_TODAY, false)

    @Test
    fun `the counting notification opens Today, and during an outing the outing`() {
        val notifications = TrackingNotifications(context)
        val day = notifications.build(NotificationContent(6_000)).opened
        assertThat(day.asksForToday()).isTrue()
        assertThat(day.hasExtra(SessionControl.EXTRA_OPEN_OUTING)).isFalse()

        val during = notifications.build(
            NotificationContent(6_000, session = SessionNotice(outing, null, UnitPreference.METRIC)),
        ).opened
        assertThat(during.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L)).isEqualTo(7L)
        assertThat(during.asksForToday()).isFalse()
    }

    @Test
    fun `the day's goal and the evening reminder open Today`() {
        val goals = GoalNotifications(context)
        val reached = goals.goalReached(GoalReachedContent(8_000, 17 * 60, 3), UnitPreference.METRIC).opened
        assertThat(reached.asksForToday()).isTrue()

        val nudge = checkNotNull(EveningReminder.check(steps = 5_660, goalSteps = 8_000, thresholdPercent = 100))
        val evening = goals.eveningReminder(nudge, UnitPreference.METRIC).opened
        assertThat(evening.asksForToday()).isTrue()
    }

    @Test
    fun `the weekly summary opens its own week in History`() {
        val week = Period.containing(PeriodScale.WEEK, LocalDate.of(2026, 11, 11), DayOfWeek.MONDAY)
        val summary = WeeklySummary(
            week = week,
            steps = 57_540,
            countedDays = 7,
            goalDays = 4,
            dailyAverage = 8_220,
            previousAverage = 7_000,
            bestDay = LocalDate.of(2026, 11, 12),
            bestDaySteps = 12_040,
            distanceMeters = 41_230.0,
            activeKcal = 2_140.0,
            headline = WeekHeadline.GoalDays(4, 7),
        )
        val opened = GoalNotifications(context).weeklySummary(summary, UnitPreference.METRIC).opened

        assertThat(opened.getLongExtra(AppDoors.EXTRA_OPEN_WEEK, 0L)).isEqualTo(LocalDate.of(2026, 11, 9).toEpochDay())
        assertThat(opened.asksForToday()).isFalse()
    }

    @Test
    fun `a stage reached opens its own way, on its journey`() {
        val way = Ways.of(WayId.VIA_FRANCIGENA)
        val journey = WayJourney(42, way.id, 20_500L, 0, WayJourneyState.ACTIVE, null, 0)
        val siena = way.stops.single { it.key == "siena" }
        val progress = WayProgress.of(way, journey, mapOf(20_500L to siena.distanceMeters + 300.0), 20_500L)
        val opened = WayNotifications(context).stage(siena, progress, UnitPreference.METRIC).opened

        assertThat(opened.getStringExtra(AppDoors.EXTRA_OPEN_WAY)).isEqualTo("VIA_FRANCIGENA")
        assertThat(opened.getLongExtra(AppDoors.EXTRA_OPEN_JOURNEY, 0L)).isEqualTo(42L)
    }

    @Test
    fun `the tile opens Today, a paused tile Today asking to resume, an outing's tile the outing`() {
        val counting = context.tileTapIntent(CountingState.COUNTING, null).opened
        assertThat(counting.asksForToday()).isTrue()
        assertThat(counting.hasExtra(TrackingControl.EXTRA_RESUME)).isFalse()

        val paused = context.tileTapIntent(CountingState.PAUSED, null).opened
        assertThat(paused.asksForToday()).isTrue()
        assertThat(paused.getBooleanExtra(TrackingControl.EXTRA_RESUME, false)).isTrue()

        val during = context.tileTapIntent(CountingState.COUNTING, 7L).opened
        assertThat(during.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L)).isEqualTo(7L)
        assertThat(during.asksForToday()).isFalse()
    }

    @Test
    fun `two doors in the shade at once each keep their own page`() {
        // An outing's notification and a stage's shared one request code until 23.1: the later one's
        // extras went to both, and the outing's touch opened the Ways.
        val sessions = SessionNotifications(context)
        val ended = sessions.ended(outing.copy(endedAtMillis = 1L), UnitPreference.METRIC, canReopen = true)
        val way = Ways.of(WayId.VIA_FRANCIGENA)
        val journey = WayJourney(42, way.id, 20_500L, 0, WayJourneyState.ACTIVE, null, 0)
        val siena = way.stops.single { it.key == "siena" }
        val progress = WayProgress.of(way, journey, mapOf(20_500L to siena.distanceMeters + 300.0), 20_500L)
        WayNotifications(context).stage(siena, progress, UnitPreference.METRIC)
        val counting = TrackingNotifications(context).build(NotificationContent(6_000))

        assertThat(ended.opened.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L)).isEqualTo(7L)
        assertThat(ended.opened.hasExtra(AppDoors.EXTRA_OPEN_WAY)).isFalse()
        assertThat(counting.opened.hasExtra(SessionControl.EXTRA_OPEN_OUTING)).isFalse()
        assertThat(DoorRequest.entries.map { it.code }.toSet()).hasSize(DoorRequest.entries.size)
    }
}
