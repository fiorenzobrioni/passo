package com.callbackdev.passo.core.model

import java.time.DayOfWeek
import java.time.LocalTime

/**
 * The reader's settings (PLANNING.md §5), apart from the [Profile]. Defaults are the values a
 * fresh install starts with; `null` where a field follows the system instead.
 *
 * The appearance defaults are Chiaro's (the family reads as one, `docs/adr/0004-design-language.md`):
 * the vivid dress, Google Sans, and the app's own colors rather than the wallpaper's.
 *
 * @property firstDayOfWeek null follows the locale.
 * @property eveningReminderThresholdPercent the evening reminder comes only while today's steps
 *   are below this share of the goal: 100 is "until the goal is met".
 * @property onboardingCompleted the first-run flow has been through to its end, or skipped.
 */
data class UserSettings(
    val dailyGoalSteps: Int = DEFAULT_DAILY_GOAL_STEPS,
    val units: UnitPreference = UnitPreference.SYSTEM,
    val firstDayOfWeek: DayOfWeek? = null,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val palette: AppPalette = AppPalette.VIVID,
    val font: AppFont = AppFont.GOOGLE_SANS,
    val dynamicColor: Boolean = false,
    val goalReachedNotification: Boolean = false,
    val eveningReminder: Boolean = false,
    val eveningReminderTime: LocalTime = DEFAULT_EVENING_REMINDER_TIME,
    val eveningReminderThresholdPercent: Int = DEFAULT_EVENING_REMINDER_THRESHOLD,
    val weeklySummary: Boolean = false,
    val trackingEnabled: Boolean = true,
    val walkDetection: Boolean = true,
    val minWalkMinutes: Int = DEFAULT_MIN_WALK_MINUTES,
    val typicalDayLine: Boolean = true,
    val onboardingCompleted: Boolean = false,
) {
    companion object {
        /**
         * 8 000: where the mortality benefit of daily steps starts to level off in adults under
         * 60 (8 000 to 10 000), and where it already has over 60 (6 000 to 8 000). Paluch et
         * al., Lancet Public Health 2022, a meta-analysis of 15 cohorts.
         */
        const val DEFAULT_DAILY_GOAL_STEPS: Int = 8_000

        /** The goals the settings accept; a stored value outside falls back to the default. */
        val DAILY_GOAL_RANGE: IntRange = 500..100_000

        /**
         * 20:00: late enough that the day has mostly been walked, early enough that a walk
         * after dinner still fits in it.
         */
        val DEFAULT_EVENING_REMINDER_TIME: LocalTime = LocalTime.of(20, 0)

        /**
         * The shares of the goal the evening reminder can wait below: until it is met, below
         * three quarters, below half. 100 by default: the reminder is for the goal.
         */
        val EVENING_REMINDER_THRESHOLDS: List<Int> = listOf(100, 75, 50)
        const val DEFAULT_EVENING_REMINDER_THRESHOLD: Int = 100

        /** The minimum walk durations the settings offer (VISION.md, walk detection). */
        val MIN_WALK_MINUTES_CHOICES: List<Int> = listOf(5, 10, 15)
        const val DEFAULT_MIN_WALK_MINUTES: Int = 10
    }
}

/** The units the reader picked; [SYSTEM] follows the region of the phone's locale. */
enum class UnitPreference {
    SYSTEM,
    METRIC,
    IMPERIAL,
}

/** The units numbers are shown in, once [UnitPreference.SYSTEM] has been resolved. */
enum class UnitSystem {
    METRIC,
    IMPERIAL,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

/**
 * Which of the two dresses the app wears when it is not wearing the wallpaper's, as in Chiaro:
 * [PAPER], warm white and amber; [VIVID], cool white and azure, the default.
 */
enum class AppPalette {
    PAPER,
    VIVID,
}

/**
 * The typeface, as in Chiaro: two bundled faces, the same drawing on every phone, and the
 * phone's own sans as the third answer.
 */
enum class AppFont {
    GOOGLE_SANS,
    INTER,
    SYSTEM,
}
