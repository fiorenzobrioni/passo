package com.callbackdev.passo.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.callbackdev.passo.core.domain.metrics.sanitized
import com.callbackdev.passo.core.model.AppFont
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/** Everything the reader set, read together. */
data class UserPreferences(val profile: Profile = Profile(), val settings: UserSettings = UserSettings())

/**
 * The profile and the settings in DataStore (PLANNING.md §5). Only what differs from the
 * default is stored: a field at its default has no key, so a default improved in a later
 * version reaches everyone who has not moved away from it. A value that cannot be read back (out of range, an
 * enum from a newer build) reads as its default.
 *
 * Plain storage: the rules about what a change does to the recorded days live in
 * `TrackingRepository`, the only caller allowed to change the profile or the goal.
 */
@Singleton
class UserPreferencesDataSource
@Inject
constructor(private val dataStore: DataStore<Preferences>) {
    val data: Flow<UserPreferences> = dataStore.data
        // An unreadable file is not a reason to stop counting: the defaults stand in.
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { UserPreferences(readProfile(it), readSettings(it)) }

    suspend fun current(): UserPreferences = data.first()

    /** Applies [transform] to the stored profile atomically; returns the profile now stored. */
    suspend fun updateProfile(transform: (Profile) -> Profile): Profile {
        var updated = Profile()
        dataStore.edit { prefs ->
            val old = readProfile(prefs)
            val new = transform(old).sanitized()
            writeProfile(prefs, old, new)
            updated = new
        }
        return updated
    }

    /** Applies [transform] to the stored settings atomically; returns the settings now stored. */
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings {
        var updated = UserSettings()
        dataStore.edit { prefs ->
            val old = readSettings(prefs)
            val new = transform(old).sanitized()
            writeSettings(prefs, old, new)
            updated = new
        }
        return updated
    }

    /**
     * The installation the stored tracker state belongs to: the first-install time of the app
     * that wrote it. It lives here, next to the settings, so that a backup carries it together
     * with the database it describes; null until a build that knows about it has run.
     */
    suspend fun trackerInstallation(): Long? = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .first()[Keys.TRACKER_INSTALLATION]

    suspend fun setTrackerInstallation(installedAtMillis: Long) {
        dataStore.edit { it[Keys.TRACKER_INSTALLATION] = installedAtMillis }
    }

    private fun readProfile(prefs: Preferences) = Profile(
        heightMeters = prefs[Keys.HEIGHT_M],
        weightKg = prefs[Keys.WEIGHT_KG],
        sex = prefs[Keys.SEX].toEnumOrNull<Sex>(),
        stepLengthMode = prefs[Keys.STEP_LENGTH_MODE].toEnumOrNull<StepLengthMode>() ?: StepLengthMode.AUTO,
        walkingStepLengthMeters = prefs[Keys.WALKING_STEP_LENGTH_M],
        runningStepLengthMeters = prefs[Keys.RUNNING_STEP_LENGTH_M],
    ).sanitized()

    private fun writeProfile(prefs: MutablePreferences, old: Profile, new: Profile) {
        prefs.write(Keys.HEIGHT_M, old.heightMeters, new.heightMeters)
        prefs.write(Keys.WEIGHT_KG, old.weightKg, new.weightKg)
        prefs.write(Keys.SEX, old.sex?.name, new.sex?.name)
        prefs.write(Keys.STEP_LENGTH_MODE, old.stepLengthMode.name, new.stepLengthMode.name, StepLengthMode.AUTO.name)
        prefs.write(Keys.WALKING_STEP_LENGTH_M, old.walkingStepLengthMeters, new.walkingStepLengthMeters)
        prefs.write(Keys.RUNNING_STEP_LENGTH_M, old.runningStepLengthMeters, new.runningStepLengthMeters)
    }

    private fun readSettings(prefs: Preferences): UserSettings {
        val defaults = UserSettings()
        return UserSettings(
            dailyGoalSteps = prefs[Keys.DAILY_GOAL] ?: defaults.dailyGoalSteps,
            units = prefs[Keys.UNITS].toEnumOrNull<UnitPreference>() ?: defaults.units,
            firstDayOfWeek = prefs[Keys.FIRST_DAY_OF_WEEK].toEnumOrNull<DayOfWeek>(),
            theme = prefs[Keys.THEME].toEnumOrNull<ThemeMode>() ?: defaults.theme,
            palette = prefs[Keys.PALETTE].toEnumOrNull<AppPalette>() ?: defaults.palette,
            font = prefs[Keys.FONT].toEnumOrNull<AppFont>() ?: defaults.font,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            goalReachedNotification = prefs[Keys.GOAL_REACHED_NOTIFICATION] ?: defaults.goalReachedNotification,
            eveningReminder = prefs[Keys.EVENING_REMINDER] ?: defaults.eveningReminder,
            eveningReminderTime = prefs[Keys.EVENING_REMINDER_MINUTE]?.toLocalTimeOrNull()
                ?: defaults.eveningReminderTime,
            weeklySummary = prefs[Keys.WEEKLY_SUMMARY] ?: defaults.weeklySummary,
            trackingEnabled = prefs[Keys.TRACKING_ENABLED] ?: defaults.trackingEnabled,
            walkDetection = prefs[Keys.WALK_DETECTION] ?: defaults.walkDetection,
            minWalkMinutes = prefs[Keys.MIN_WALK_MINUTES] ?: defaults.minWalkMinutes,
            typicalDayLine = prefs[Keys.TYPICAL_DAY_LINE] ?: defaults.typicalDayLine,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
        ).sanitized()
    }

    private fun writeSettings(prefs: MutablePreferences, old: UserSettings, new: UserSettings) {
        val defaults = UserSettings()
        prefs.write(Keys.DAILY_GOAL, old.dailyGoalSteps, new.dailyGoalSteps, defaults.dailyGoalSteps)
        prefs.write(Keys.UNITS, old.units.name, new.units.name, defaults.units.name)
        prefs.write(Keys.FIRST_DAY_OF_WEEK, old.firstDayOfWeek?.name, new.firstDayOfWeek?.name)
        prefs.write(Keys.THEME, old.theme.name, new.theme.name, defaults.theme.name)
        prefs.write(Keys.PALETTE, old.palette.name, new.palette.name, defaults.palette.name)
        prefs.write(Keys.FONT, old.font.name, new.font.name, defaults.font.name)
        prefs.write(Keys.DYNAMIC_COLOR, old.dynamicColor, new.dynamicColor, defaults.dynamicColor)
        prefs.write(
            Keys.GOAL_REACHED_NOTIFICATION,
            old.goalReachedNotification,
            new.goalReachedNotification,
            defaults.goalReachedNotification,
        )
        prefs.write(Keys.EVENING_REMINDER, old.eveningReminder, new.eveningReminder, defaults.eveningReminder)
        prefs.write(
            Keys.EVENING_REMINDER_MINUTE,
            old.eveningReminderTime.minuteOfDay(),
            new.eveningReminderTime.minuteOfDay(),
            defaults.eveningReminderTime.minuteOfDay(),
        )
        prefs.write(Keys.WEEKLY_SUMMARY, old.weeklySummary, new.weeklySummary, defaults.weeklySummary)
        prefs.write(Keys.TRACKING_ENABLED, old.trackingEnabled, new.trackingEnabled, defaults.trackingEnabled)
        prefs.write(Keys.WALK_DETECTION, old.walkDetection, new.walkDetection, defaults.walkDetection)
        prefs.write(Keys.MIN_WALK_MINUTES, old.minWalkMinutes, new.minWalkMinutes, defaults.minWalkMinutes)
        prefs.write(Keys.TYPICAL_DAY_LINE, old.typicalDayLine, new.typicalDayLine, defaults.typicalDayLine)
        prefs.write(
            Keys.ONBOARDING_COMPLETED,
            old.onboardingCompleted,
            new.onboardingCompleted,
            defaults.onboardingCompleted,
        )
    }

    /** Writes only a field that changed; back at its default, the key goes. */
    private fun <T : Any> MutablePreferences.write(key: Preferences.Key<T>, old: T?, new: T?, default: T? = null) {
        if (old == new) return
        if (new == null || new == default) remove(key) else set(key, new)
    }

    private object Keys {
        val HEIGHT_M = doublePreferencesKey("profile_height_m")
        val WEIGHT_KG = doublePreferencesKey("profile_weight_kg")
        val SEX = stringPreferencesKey("profile_sex")
        val STEP_LENGTH_MODE = stringPreferencesKey("profile_step_length_mode")
        val WALKING_STEP_LENGTH_M = doublePreferencesKey("profile_walking_step_length_m")
        val RUNNING_STEP_LENGTH_M = doublePreferencesKey("profile_running_step_length_m")

        val DAILY_GOAL = intPreferencesKey("daily_goal_steps")
        val UNITS = stringPreferencesKey("units")
        val FIRST_DAY_OF_WEEK = stringPreferencesKey("first_day_of_week")
        val THEME = stringPreferencesKey("theme")
        val PALETTE = stringPreferencesKey("palette")
        val FONT = stringPreferencesKey("font")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val GOAL_REACHED_NOTIFICATION = booleanPreferencesKey("notify_goal_reached")
        val EVENING_REMINDER = booleanPreferencesKey("notify_evening_reminder")
        val EVENING_REMINDER_MINUTE = intPreferencesKey("evening_reminder_minute_of_day")
        val WEEKLY_SUMMARY = booleanPreferencesKey("notify_weekly_summary")
        val TRACKING_ENABLED = booleanPreferencesKey("tracking_enabled")
        val WALK_DETECTION = booleanPreferencesKey("walk_detection")
        val MIN_WALK_MINUTES = intPreferencesKey("min_walk_minutes")
        val TYPICAL_DAY_LINE = booleanPreferencesKey("typical_day_line")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        val TRACKER_INSTALLATION = longPreferencesKey("tracker_installation")
    }

    companion object {
        /** The DataStore file's name, under the app's files directory. */
        const val FILE_NAME = "settings"
    }
}

/** These settings with every value the app would not accept replaced by its default. */
internal fun UserSettings.sanitized(): UserSettings {
    val defaults = UserSettings()
    return copy(
        dailyGoalSteps = dailyGoalSteps.takeIf { it in UserSettings.DAILY_GOAL_RANGE } ?: defaults.dailyGoalSteps,
        minWalkMinutes = minWalkMinutes.takeIf { it in UserSettings.MIN_WALK_MINUTES_CHOICES }
            ?: defaults.minWalkMinutes,
        // Stored to the minute: seconds would only make two equal times differ.
        eveningReminderTime = eveningReminderTime.withSecond(0).withNano(0),
    )
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }

private fun LocalTime.minuteOfDay(): Int = hour * MINUTES_PER_HOUR + minute

private fun Int.toLocalTimeOrNull(): LocalTime? =
    takeIf { it in 0 until MINUTES_PER_DAY }?.let { LocalTime.of(it / MINUTES_PER_HOUR, it % MINUTES_PER_HOUR) }

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
