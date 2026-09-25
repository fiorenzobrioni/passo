package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.model.AppFont
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.ThemeMode
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.time.DateTimeException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** What reading a file came to. */
sealed interface BackupRead {
    data class Ok(val backup: Backup) : BackupRead

    /** Readable JSON, or not JSON at all, but not a file Passo wrote. */
    data object NotPasso : BackupRead

    /** A Passo backup in a format this version does not know yet: the app needs an update. */
    data class TooNew(val version: Int) : BackupRead

    /** A Passo backup that cannot be read to its end (cut short, edited by hand). */
    data object Damaged : BackupRead
}

/**
 * The backup file (ADR 0011): JSON, UTF-8, one object. What it says is written down here and in
 * the ADR, so the file can be read without the app.
 *
 * - `format` is always `passo-backup`; `version` is [VERSION], raised only for a change an older
 *   reader would misread. Added fields do not raise it: a reader skips what it does not know.
 * - Instants are epoch milliseconds (`…AtMillis`), days are ISO dates (`2026-09-25`), a
 *   minute is `[epochMinute, steps]` (the UTC minute since 1970), nested under the day it was
 *   recorded on, which is kept as recorded even where a time-zone change put it elsewhere.
 * - Lengths are metres, weights kilograms, energy kilocalories, whatever units the app shows.
 * - Enum values are their names; a name this version does not know reads as the default, or
 *   leaves out the plan or outing it belongs to.
 *
 * Reading is lenient where a value can be dropped without lying (an unknown setting, a minute
 * with a negative count) and strict where it cannot (the format, the version, broken JSON).
 */
object BackupCodec {
    const val FORMAT: String = "passo-backup"
    const val VERSION: Int = 1

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    /** The name offered for the file, dated so that several backups sort and tell apart. */
    fun fileName(date: LocalDate): String = "passo-backup-$date.json"

    fun encode(backup: Backup): String = json.encodeToString(BackupFile.serializer(), backup.toFile())

    fun decode(text: String): BackupRead {
        val root = try {
            json.parseToJsonElement(text) as? JsonObject ?: return BackupRead.NotPasso
        } catch (_: SerializationException) {
            // Not JSON: a file of something else, or one of Passo's cut short on its way here.
            return if (text.contains("\"$FORMAT\"")) BackupRead.Damaged else BackupRead.NotPasso
        }
        val format = runCatching { root["format"]?.jsonPrimitive?.content }.getOrNull()
        if (format != FORMAT) return BackupRead.NotPasso
        val version = runCatching { root["version"]?.jsonPrimitive?.intOrNull }.getOrNull() ?: return BackupRead.Damaged
        if (version > VERSION) return BackupRead.TooNew(version)
        return try {
            BackupRead.Ok(json.decodeFromJsonElement(BackupFile.serializer(), root).toBackup())
        } catch (_: SerializationException) {
            BackupRead.Damaged
        } catch (_: IllegalArgumentException) {
            BackupRead.Damaged
        }
    }
}

private fun Backup.toFile() = BackupFile(
    format = BackupCodec.FORMAT,
    version = BackupCodec.VERSION,
    exportedAtMillis = exportedAtMillis,
    zone = zone,
    app = appVersion,
    profile = ProfileDto(
        heightMeters = profile.heightMeters,
        weightKg = profile.weightKg,
        sex = profile.sex?.name,
        stepLengthMode = profile.stepLengthMode.name,
        walkingStepLengthMeters = profile.walkingStepLengthMeters,
        runningStepLengthMeters = profile.runningStepLengthMeters,
    ),
    settings = SettingsDto(
        dailyGoalSteps = settings.dailyGoalSteps,
        units = settings.units.name,
        firstDayOfWeek = settings.firstDayOfWeek?.name,
        theme = settings.theme.name,
        palette = settings.palette.name,
        font = settings.font.name,
        dynamicColor = settings.dynamicColor,
        goalReachedNotification = settings.goalReachedNotification,
        eveningReminder = settings.eveningReminder,
        eveningReminderTime = settings.eveningReminderTime.withSecond(0).withNano(0).toString(),
        eveningReminderThresholdPercent = settings.eveningReminderThresholdPercent,
        weeklySummary = settings.weeklySummary,
        walkDetection = settings.walkDetection,
        minWalkMinutes = settings.minWalkMinutes,
        typicalDayLine = settings.typicalDayLine,
        startOutingButton = settings.startOutingButton,
    ),
    days = days.sortedBy { it.summary.localEpochDay }.map { day ->
        val summary = day.summary
        DayDto(
            date = LocalDate.ofEpochDay(summary.localEpochDay).toString(),
            steps = summary.steps,
            goalSteps = summary.goalSteps,
            distanceMeters = summary.distanceMeters,
            activeKcal = summary.activeKcal,
            activeMinutes = summary.activeMinutes,
            briskMinutes = summary.briskMinutes,
            finalized = summary.finalized,
            minutes = day.minutes.sortedBy { it.epochMinute }.map { listOf(it.epochMinute, it.steps.toLong()) },
        )
    },
    plans = plans.map { plan ->
        PlanDto(
            id = plan.id,
            name = plan.name,
            goalKind = plan.goalKind.name,
            goalValue = plan.goalValue,
            intensity = plan.intensity.name,
            milestones = plan.milestones.percents(),
            vibrate = plan.vibrate,
            voice = plan.voice.name,
            position = plan.position,
            lastUsedAtMillis = plan.lastUsedAtMillis,
        )
    },
    outings = sessions.sortedBy { it.startedAtMillis }.map { session ->
        OutingDto(
            planId = session.planId,
            name = session.name,
            goalKind = session.goalKind.name,
            goalValue = session.goalValue,
            restOfDay = session.restOfDay,
            intensity = session.intensity.name,
            milestones = session.milestones.percents(),
            vibrate = session.vibrate,
            voice = session.voice.name,
            date = LocalDate.ofEpochDay(session.localEpochDay).toString(),
            startedAtMillis = session.startedAtMillis,
            state = session.state.name,
            endedAtMillis = session.endedAtMillis,
            end = session.end?.name,
            steps = session.totals.steps,
            movingMillis = session.totals.movingMillis,
            zoneMillis = session.totals.zoneMillis,
            distanceMeters = session.totals.distanceMeters,
            activeKcal = session.totals.activeKcal,
            lastStepAtMillis = session.lastStepAtMillis,
            lastEventAtMillis = session.lastEventAtMillis,
            pausedAtMillis = session.pausedAtMillis,
            reachedAtMillis = session.reachedAtMillis,
            toldMilestones = session.toldMilestones.percents(),
        )
    },
    diagnostics = diagnostics.map { DiagnosticDto(it.wallMillis, it.type.name, it.detail) },
)

private fun BackupFile.toBackup(): Backup {
    val defaults = UserSettings()
    return Backup(
        exportedAtMillis = exportedAtMillis ?: 0L,
        zone = zone.orEmpty(),
        appVersion = app.orEmpty(),
        profile = Profile(
            heightMeters = profile.heightMeters,
            weightKg = profile.weightKg,
            sex = profile.sex.toEnumOrNull<Sex>(),
            stepLengthMode = profile.stepLengthMode.toEnumOrNull<StepLengthMode>() ?: StepLengthMode.AUTO,
            walkingStepLengthMeters = profile.walkingStepLengthMeters,
            runningStepLengthMeters = profile.runningStepLengthMeters,
        ),
        settings = UserSettings(
            dailyGoalSteps = settings.dailyGoalSteps ?: defaults.dailyGoalSteps,
            units = settings.units.toEnumOrNull<UnitPreference>() ?: defaults.units,
            firstDayOfWeek = settings.firstDayOfWeek.toEnumOrNull<DayOfWeek>(),
            theme = settings.theme.toEnumOrNull<ThemeMode>() ?: defaults.theme,
            palette = settings.palette.toEnumOrNull<AppPalette>() ?: defaults.palette,
            font = settings.font.toEnumOrNull<AppFont>() ?: defaults.font,
            dynamicColor = settings.dynamicColor ?: defaults.dynamicColor,
            goalReachedNotification = settings.goalReachedNotification ?: defaults.goalReachedNotification,
            eveningReminder = settings.eveningReminder ?: defaults.eveningReminder,
            eveningReminderTime = settings.eveningReminderTime?.toLocalTimeOrNull() ?: defaults.eveningReminderTime,
            eveningReminderThresholdPercent = settings.eveningReminderThresholdPercent
                ?: defaults.eveningReminderThresholdPercent,
            weeklySummary = settings.weeklySummary ?: defaults.weeklySummary,
            walkDetection = settings.walkDetection ?: defaults.walkDetection,
            minWalkMinutes = settings.minWalkMinutes ?: defaults.minWalkMinutes,
            typicalDayLine = settings.typicalDayLine ?: defaults.typicalDayLine,
            startOutingButton = settings.startOutingButton ?: defaults.startOutingButton,
        ),
        days = days.mapNotNull { it.toDay() }
            // One entry per day: a file edited by hand may repeat one, and the fuller one is kept.
            .groupBy { it.summary.localEpochDay }
            .map { (_, same) -> same.maxBy { it.summary.steps } }
            .sortedBy { it.summary.localEpochDay },
        plans = plans.mapNotNull { it.toPlan() },
        sessions = outings.mapNotNull { it.toSession() },
        diagnostics = diagnostics.mapNotNull { row ->
            row.type.toEnumOrNull<DiagnosticsType>()?.let { DiagnosticsEvent(row.atMillis, it, row.detail) }
        },
    )
}

private fun DayDto.toDay(): BackupDay? {
    val day = date.toEpochDayOrNull() ?: return null
    val minutes = minutes.mapNotNull { pair ->
        val epochMinute = pair.getOrNull(0) ?: return@mapNotNull null
        val steps = pair.getOrNull(1)?.takeIf { it in 0..Int.MAX_VALUE }?.toInt() ?: return@mapNotNull null
        MinuteSteps(epochMinute, day, steps).takeIf { steps > 0 }
    }
        // A minute is one row: if a hand-edited file repeats one, the larger count stands.
        .groupBy { it.epochMinute }
        .map { (_, same) -> same.maxBy { it.steps } }
    return BackupDay(
        summary = DailySummary(
            localEpochDay = day,
            steps = steps.coerceAtLeast(0),
            distanceMeters = distanceMeters.nonNegative(),
            activeKcal = activeKcal.nonNegative(),
            activeMinutes = activeMinutes.coerceAtLeast(0),
            briskMinutes = briskMinutes.coerceAtLeast(0),
            goalSteps =
            goalSteps.takeIf { it in UserSettings.DAILY_GOAL_RANGE } ?: UserSettings.DEFAULT_DAILY_GOAL_STEPS,
            finalized = finalized,
        ),
        minutes = minutes,
    )
}

private fun PlanDto.toPlan(): SessionPlan? {
    val kind = goalKind.toEnumOrNull<SessionGoalKind>() ?: return null
    return SessionPlan(
        id = id,
        name = name?.trim()?.takeIf { it.isNotEmpty() },
        goalKind = kind,
        goalValue = goalValue.coerceAtLeast(0),
        intensity = intensity.toEnumOrNull<SessionIntensity>() ?: SessionIntensity.FREE,
        milestones = milestones.toMilestones() - SessionMilestone.GOAL,
        vibrate = vibrate,
        voice = voice.toEnumOrNull<SessionVoice>() ?: SessionVoice.OFF,
        position = position,
        lastUsedAtMillis = lastUsedAtMillis,
    )
}

private fun OutingDto.toSession(): Session? {
    val kind = goalKind.toEnumOrNull<SessionGoalKind>()?.takeIf { it != SessionGoalKind.REST_OF_DAY } ?: return null
    val day = date.toEpochDayOrNull() ?: return null
    return Session(
        planId = planId,
        name = name?.trim()?.takeIf { it.isNotEmpty() },
        goalKind = kind,
        goalValue = goalValue.coerceAtLeast(0),
        restOfDay = restOfDay,
        intensity = intensity.toEnumOrNull<SessionIntensity>() ?: SessionIntensity.FREE,
        milestones = milestones.toMilestones(),
        vibrate = vibrate,
        voice = voice.toEnumOrNull<SessionVoice>() ?: SessionVoice.OFF,
        localEpochDay = day,
        startedAtMillis = startedAtMillis,
        // An unknown state from a newer build is over, as in the database.
        state = state.toEnumOrNull<SessionState>() ?: SessionState.FINISHED,
        endedAtMillis = endedAtMillis,
        end = end.toEnumOrNull<SessionEnd>(),
        totals = SessionTotals(
            steps = steps.coerceAtLeast(0),
            movingMillis = movingMillis.coerceAtLeast(0),
            zoneMillis = zoneMillis.coerceAtLeast(0),
            distanceMeters = distanceMeters.nonNegative(),
            activeKcal = activeKcal.nonNegative(),
        ),
        lastStepAtMillis = lastStepAtMillis ?: startedAtMillis,
        lastEventAtMillis = lastEventAtMillis ?: startedAtMillis,
        pausedAtMillis = pausedAtMillis,
        reachedAtMillis = reachedAtMillis,
        toldMilestones = toldMilestones.toMilestones(),
    )
}

private fun Set<SessionMilestone>.percents(): List<Int> = map { it.percent }.sorted()

private fun List<Int>.toMilestones(): Set<SessionMilestone> =
    mapNotNull { percent -> SessionMilestone.entries.firstOrNull { it.percent == percent } }.toSet()

private fun Double.nonNegative(): Double = if (isNaN() || this < 0) 0.0 else this

private fun String.toEpochDayOrNull(): Long? = try {
    LocalDate.parse(this).toEpochDay()
} catch (_: DateTimeException) {
    null
}

private fun String.toLocalTimeOrNull(): LocalTime? = try {
    LocalTime.parse(this).withSecond(0).withNano(0)
} catch (_: DateTimeException) {
    null
}

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
