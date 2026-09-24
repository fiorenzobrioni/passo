package com.callbackdev.passo.core.data.prefs

import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.passo.core.data.TestDataStore
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.DayOfWeek
import java.time.LocalTime

class UserPreferencesDataSourceTest {
    @get:Rule val folder = TemporaryFolder()

    private lateinit var store: TestDataStore
    private lateinit var source: UserPreferencesDataSource

    @Before
    fun setUp() {
        store = TestDataStore(folder.root)
        source = UserPreferencesDataSource(store.dataStore)
    }

    @After
    fun tearDown() {
        store.scope.cancel()
    }

    @Test
    fun `a fresh install reads the defaults`() = runTest {
        assertThat(source.current()).isEqualTo(UserPreferences(Profile(), UserSettings()))
    }

    @Test
    fun `the profile round-trips`() = runTest {
        val profile = Profile(
            heightMeters = 1.78,
            weightKg = 74.5,
            sex = Sex.FEMALE,
            stepLengthMode = StepLengthMode.MANUAL,
            walkingStepLengthMeters = 0.74,
            runningStepLengthMeters = 1.02,
        )

        assertThat(source.updateProfile { profile }).isEqualTo(profile)
        assertThat(source.current().profile).isEqualTo(profile)
    }

    @Test
    fun `the settings round-trip`() = runTest {
        val settings = UserSettings(
            dailyGoalSteps = 10_000,
            units = UnitPreference.IMPERIAL,
            firstDayOfWeek = DayOfWeek.SUNDAY,
            theme = ThemeMode.DARK,
            dynamicColor = false,
            goalReachedNotification = true,
            eveningReminder = true,
            eveningReminderTime = LocalTime.of(21, 30),
            weeklySummary = true,
            trackingEnabled = false,
            walkDetection = false,
            minWalkMinutes = 15,
            typicalDayLine = false,
        )

        assertThat(source.updateSettings { settings }).isEqualTo(settings)
        assertThat(source.current().settings).isEqualTo(settings)
    }

    @Test
    fun `only what differs from the default is stored`() = runTest {
        source.updateSettings { it.copy(dailyGoalSteps = 12_000, theme = ThemeMode.DARK) }
        source.updateProfile { it.copy(weightKg = 80.0) }
        assertThat(storedKeys()).containsExactly("daily_goal_steps", "theme", "profile_weight_kg")

        source.updateSettings { it.copy(theme = ThemeMode.SYSTEM) }
        source.updateProfile { it.copy(weightKg = null) }
        assertThat(storedKeys()).containsExactly("daily_goal_steps")
    }

    @Test
    fun `values the app would not accept read as defaults`() = runTest {
        store.dataStore.edit {
            it[intPreferencesKey("daily_goal_steps")] = 3
            it[intPreferencesKey("min_walk_minutes")] = 7
            it[intPreferencesKey("evening_reminder_minute_of_day")] = 5_000
            it[doublePreferencesKey("profile_weight_kg")] = 7.0
            it[doublePreferencesKey("profile_height_m")] = 175.0
            it[stringPreferencesKey("theme")] = "SEPIA"
            it[stringPreferencesKey("profile_sex")] = "OTHER"
        }

        assertThat(source.current()).isEqualTo(UserPreferences(Profile(), UserSettings()))
    }

    @Test
    fun `an implausible profile value is not stored`() = runTest {
        val stored = source.updateProfile { it.copy(heightMeters = 175.0, weightKg = 70.0) }

        assertThat(stored).isEqualTo(Profile(weightKg = 70.0))
        assertThat(storedKeys()).containsExactly("profile_weight_kg")
    }

    @Test
    fun `the reminder time is kept to the minute`() = runTest {
        source.updateSettings { it.copy(eveningReminderTime = LocalTime.of(19, 45, 30)) }

        assertThat(source.current().settings.eveningReminderTime).isEqualTo(LocalTime.of(19, 45))
    }

    private suspend fun storedKeys(): List<String> = store.dataStore.data.first().asMap().keys.map { it.name }
}
