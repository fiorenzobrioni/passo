package com.callbackdev.passo.core.domain.backup

import kotlinx.serialization.Serializable

/*
 * The backup file's shape (ADR 0011), apart from the app's model so the model can change without
 * changing what a file says. Every field has a default: a file from an older build that lacks
 * one still reads, and a value that cannot be read falls back rather than failing the file.
 */

@Serializable
internal data class BackupFile(
    val format: String = "",
    val version: Int = 0,
    val exportedAtMillis: Long? = null,
    val zone: String? = null,
    val app: String? = null,
    val profile: ProfileDto = ProfileDto(),
    val settings: SettingsDto = SettingsDto(),
    val days: List<DayDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val outings: List<OutingDto> = emptyList(),
    val diagnostics: List<DiagnosticDto> = emptyList(),
)

@Serializable
internal data class ProfileDto(
    val heightMeters: Double? = null,
    val weightKg: Double? = null,
    val sex: String? = null,
    val stepLengthMode: String? = null,
    val walkingStepLengthMeters: Double? = null,
    val runningStepLengthMeters: Double? = null,
)

/** The reader's choices; not whether this phone is counting, nor whether its first run is done. */
@Serializable
internal data class SettingsDto(
    val dailyGoalSteps: Int? = null,
    val units: String? = null,
    val firstDayOfWeek: String? = null,
    val theme: String? = null,
    val palette: String? = null,
    val font: String? = null,
    val dynamicColor: Boolean? = null,
    val goalReachedNotification: Boolean? = null,
    val eveningReminder: Boolean? = null,
    val eveningReminderTime: String? = null,
    val eveningReminderThresholdPercent: Int? = null,
    val weeklySummary: Boolean? = null,
    val walkDetection: Boolean? = null,
    val minWalkMinutes: Int? = null,
    val typicalDayLine: Boolean? = null,
    val startOutingButton: Boolean? = null,
)

/** One day: its summary as it stood, and its minutes as `[epochMinute, steps]`. */
@Serializable
internal data class DayDto(
    val date: String = "",
    val steps: Int = 0,
    val goalSteps: Int = 0,
    val distanceMeters: Double = 0.0,
    val activeKcal: Double = 0.0,
    val activeMinutes: Int = 0,
    val briskMinutes: Int = 0,
    val finalized: Boolean = false,
    val minutes: List<List<Long>> = emptyList(),
)

@Serializable
internal data class PlanDto(
    val id: Long = 0,
    val name: String? = null,
    val goalKind: String = "",
    val goalValue: Int = 0,
    val intensity: String = "",
    val milestones: List<Int> = emptyList(),
    val vibrate: Boolean = true,
    val voice: String = "",
    val position: Int = 0,
    val lastUsedAtMillis: Long? = null,
)

@Serializable
internal data class OutingDto(
    val planId: Long? = null,
    val name: String? = null,
    val goalKind: String = "",
    val goalValue: Int = 0,
    val restOfDay: Boolean = false,
    val intensity: String = "",
    val milestones: List<Int> = emptyList(),
    val vibrate: Boolean = true,
    val voice: String = "",
    val date: String = "",
    val startedAtMillis: Long = 0,
    val state: String = "",
    val endedAtMillis: Long? = null,
    val end: String? = null,
    val steps: Int = 0,
    val movingMillis: Long = 0,
    val zoneMillis: Long = 0,
    val distanceMeters: Double = 0.0,
    val activeKcal: Double = 0.0,
    val lastStepAtMillis: Long? = null,
    val lastEventAtMillis: Long? = null,
    val pausedAtMillis: Long? = null,
    val reachedAtMillis: Long? = null,
    val toldMilestones: List<Int> = emptyList(),
)

@Serializable
internal data class DiagnosticDto(val atMillis: Long = 0, val type: String = "", val detail: String = "")
