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
    val ways: List<WayDto> = emptyList(),
    val rounds: List<RoundDto> = emptyList(),
    val roundsApart: List<RoundApartDto> = emptyList(),
)

/**
 * A usual round (Phase 20): the reader's name for it, if any, and its walks by their outings'
 * `startedAtMillis`. An added field, no new version; a name and numbers, never a place.
 */
@Serializable
internal data class RoundDto(val name: String? = null, val walks: List<Long> = emptyList())

/** Two outings, by their starts, the reader said are not the same round (Phase 20). */
@Serializable
internal data class RoundApartDto(val first: Long = 0, val second: Long = 0)

@Serializable
internal data class ProfileDto(
    val heightMeters: Double? = null,
    val weightKg: Double? = null,
    val sex: String? = null,
    val stepLengthMode: String? = null,
    val walkingStepLengthMeters: Double? = null,
    val runningStepLengthMeters: Double? = null,
    val learnedStep: LearnedStepDto? = null,
)

/**
 * The step learnt from the outings with GPS and accepted (Phase 17): an added field, no new
 * version. 1.1.0 skips it and reads the mode `LEARNED`, which it does not know, as the height's
 * estimate: a downgrade costs the curve, and nothing else. Numbers, never places.
 */
@Serializable
internal data class LearnedStepDto(
    val points: List<StepPointDto> = emptyList(),
    val usualCadence: Int = 0,
    val learntMeters: Double = 0.0,
    val outings: Int = 0,
    val throughMillis: Long = 0,
    val replaced: String? = null,
    val running: Boolean = true,
)

@Serializable
internal data class StepPointDto(val cadence: Double = 0.0, val meters: Double = 0.0)

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
    val walkVoice: String? = null,
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
    val intervals: IntervalsDto? = null,
    val gps: Boolean = false,
    val marks: String? = null,
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
    val walk: String? = null,
    val walkFromMeters: Int = 0,
    val intervals: IntervalsDto? = null,
    val splits: List<SplitDto> = emptyList(),
    val gps: Boolean = false,
    val stepsMeters: Double? = null,
    val measuredShare: Double? = null,
    val stepLengthSamples: List<StepLengthSampleDto> = emptyList(),
    val ascentMeters: Double? = null,
    val descentMeters: Double? = null,
    val legs: List<LegDto> = emptyList(),
    val route: RouteDto? = null,
    val pauses: List<PauseDto> = emptyList(),
    val marks: String? = null,
    val markMiles: Boolean = false,
    val toldMarks: Int = 0,
    val pausesKept: Boolean = false,
)

/**
 * A pause of an outing, from its Pause to its Resume (Phase 21), in order: an added field of the
 * outing, no new version. Instants, never places.
 */
@Serializable
internal data class PauseDto(val fromMillis: Long = 0, val toMillis: Long = 0)

/**
 * A hundred metres of an outing with GPS as it was walked (Phase 16), in order: an added field
 * of the outing, no new version. Numbers, never places.
 */
@Serializable
internal data class LegDto(
    val meters: Double = 0.0,
    val movingMillis: Long = 0,
    val steps: Double = 0.0,
    val endedAtMillis: Long = 0,
    val ascentMeters: Double = 0.0,
    val descentMeters: Double = 0.0,
)

/**
 * Where an outing with GPS went (Phase 16): only in a file the reader asked to hold the routes.
 * The points as the app keeps them (`RouteCodec`: differences in variable-length integers), in
 * Base64: a route is a few thousand points, and the GPX the outing's page saves is the form to
 * read one in.
 */
@Serializable
internal data class RouteDto(val altitudes: String? = null, val points: String = "")

/**
 * What a clean stretch of an outing with GPS said of the step (Phase 15): an added field of the
 * outing, no new version. A number, never a place.
 */
@Serializable
internal data class StepLengthSampleDto(val cadence: Int = 0, val steps: Int = 0, val meters: Double = 0.0)

/** An interval outing's sets (Phase 13): an added field, no new version. */
@Serializable
internal data class IntervalsDto(val slowMinutes: Int = 3, val fastMinutes: Int = 3, val sets: Int = 5)

/** One interval as it was walked: its index from 0 (slow when even), its steps and times. */
@Serializable
internal data class SplitDto(
    val index: Int = 0,
    val fast: Boolean = false,
    val steps: Int = 0,
    val movingMillis: Long = 0,
    val zoneMillis: Long = 0,
)

@Serializable
internal data class DiagnosticDto(val atMillis: Long = 0, val type: String = "", val detail: String = "")

/** A way started (Phase 11): which, from which day, where it stands. An added field: no new version. */
@Serializable
internal data class WayDto(
    val way: String = "",
    val startDate: String = "",
    val startedAtMillis: Long = 0,
    val state: String = "",
    val endedDate: String? = null,
    val toldMeters: Int = 0,
)
