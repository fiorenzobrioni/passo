package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.NotificationManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.data.db.PassoDatabase
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.time.TodaySource
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.LiveToday
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.goals.GoalSchedule
import com.callbackdev.passo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSensor
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * The goal notifications end to end, on a real database and settings file: once a day across
 * restarts, the alarms armed for what the settings and today say, the reminder silent when the
 * count is not live (PLANNING.md §11 Phase 6, acceptance).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "en-rUS")
class GoalNotifierTest {
    @get:Rule val folder = TemporaryFolder()

    private val app: Application = ApplicationProvider.getApplicationContext()
    private val zone = ZoneId.systemDefault()
    private val today = LocalDate.now(zone).toEpochDay()
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var database: PassoDatabase
    private lateinit var preferences: UserPreferencesDataSource
    private lateinit var repository: TrackingRepository
    private val liveSteps = LiveSteps()

    private val alarms get() = shadowOf(app.getSystemService(AlarmManager::class.java))
    private val posted get() = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(app, PassoDatabase::class.java).allowMainThreadQueries().build()
        preferences = UserPreferencesDataSource(
            PreferenceDataStoreFactory.create(scope = storeScope) { File(folder.root, "settings.preferences_pb") },
        )
        repository = TrackingRepository(database.trackingDao(), preferences, TodaySource.System)
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.ACTIVITY_RECOGNITION)
        shadowOf(app.getSystemService(SensorManager::class.java))
            .addSensor(ShadowSensor.newInstance(Sensor.TYPE_STEP_COUNTER))
    }

    @After
    fun tearDown() {
        database.close()
        storeScope.cancel()
    }

    // A notifier as a fresh process would make it: same store, same database.
    private fun notifier() = GoalNotifier(app, preferences, repository, liveSteps, TrackerLink())

    private suspend fun settings(transform: (UserSettings) -> UserSettings) {
        preferences.updateSettings { transform(it.copy(onboardingCompleted = true)) }
    }

    private fun live(steps: Int) {
        liveSteps.setServiceRunning(true)
        liveSteps.publish(LiveToday(today, steps, emptyList()))
    }

    @Test
    fun `goal reached is told once a day, even by a process started again`() = runTest {
        settings { it.copy(goalReachedNotification = true) }
        live(8_100)

        notifier().tellGoalReached(today)
        notifier().tellGoalReached(today)

        assertThat(posted).hasSize(1)
        assertThat(posted.single().extras.getString("android.title")).isEqualTo("Goal reached: 8,000 steps")
    }

    @Test
    fun `a goal seen reached with the notification off is not announced once it is turned on`() = runTest {
        live(8_100)
        notifier().tellGoalReached(today)
        settings { it.copy(goalReachedNotification = true) }

        notifier().tellGoalReached(today)

        assertThat(posted).isEmpty()
    }

    @Test
    fun `the evening reminder is armed for its local time, and not at all when off`() = runTest {
        settings { it.copy(eveningReminder = true, eveningReminderTime = LocalTime.of(20, 30)) }
        live(1_000)

        notifier().reschedule()

        val alarm = alarms.scheduledAlarms.single()
        assertThat(alarm.type).isEqualTo(AlarmManager.RTC_WAKEUP)
        assertThat(alarm.isAllowWhileIdle).isTrue()
        assertThat(alarm.triggerAtMs)
            .isEqualTo(GoalSchedule.nextReminder(Instant.now(), zone, LocalTime.of(20, 30)).toEpochMilli())

        settings { it.copy(eveningReminder = false) }
        notifier().reschedule()
        assertThat(alarms.scheduledAlarms).isEmpty()
    }

    @Test
    fun `a day already at its goal moves the reminder to tomorrow`() = runTest {
        settings { it.copy(eveningReminder = true) }
        live(9_000)

        notifier().reschedule()

        val due = Instant.ofEpochMilli(alarms.scheduledAlarms.single().triggerAtMs).atZone(zone).toLocalDate()
        assertThat(due).isEqualTo(LocalDate.ofEpochDay(today + 1))
    }

    @Test
    fun `a paused count arms no reminder`() = runTest {
        settings { it.copy(eveningReminder = true, trackingEnabled = false) }

        notifier().reschedule()

        assertThat(alarms.scheduledAlarms).isEmpty()
    }

    @Test
    fun `the weekly summary does not wake the phone`() = runTest {
        settings { it.copy(weeklySummary = true) }

        notifier().reschedule()

        assertThat(alarms.scheduledAlarms.single().type).isEqualTo(AlarmManager.RTC)
    }

    @Test
    fun `the reminder speaks below the goal, and only while the count is live`() = runTest {
        settings { it.copy(eveningReminder = true) }

        // The service is not running: the count may be stale, so nothing is said.
        notifier().eveningReminder(dueAt = null)
        assertThat(posted).isEmpty()

        live(5_660)
        notifier().eveningReminder(dueAt = null)
        assertThat(posted.single().extras.getString("android.title")).isEqualTo("2,340 steps to go")
        // And the next one is armed.
        assertThat(alarms.scheduledAlarms).hasSize(1)
    }

    @Test
    fun `a reminder that comes too late says nothing`() = runTest {
        settings { it.copy(eveningReminder = true) }
        live(1_000)

        notifier().eveningReminder(dueAt = Instant.now().minusSeconds(3 * 3_600))

        assertThat(posted).isEmpty()
    }
}
