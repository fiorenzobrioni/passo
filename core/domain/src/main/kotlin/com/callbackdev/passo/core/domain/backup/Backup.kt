package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.StepLengthSample
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.model.WayJourney

/** One recorded day as a backup carries it: its summary as it stood, and every minute of it. */
data class BackupDay(val summary: DailySummary, val minutes: List<MinuteSteps>)

/**
 * Everything Passo knows that a new phone could not rebuild (PLANNING.md §11 Phase 7, ADR 0011):
 * the step history to the minute, each day's summary as it was frozen, the outings and their
 * plans, the ways started (Phase 11), the profile and the settings the reader chose. Not the
 * tracker state, which belongs to one sensor in one boot session (ADR 0007), and not what only
 * this phone decides (whether it is counting, whether the first run is done).
 *
 * The diagnostics log travels out, so a field problem can be read from the file, and never
 * back in: it is the log of the phone that wrote it.
 *
 * @property zone the time zone the phone was in when the file was written, for a reader of the
 *   file; nothing is converted with it.
 * @property stepLengthSamples what the clean stretches of the outings with GPS said of the
 *   reader's step (docs/adr/0016-location.md, decision 12), by the outing's start: numbers,
 *   never places. The routes themselves are not here (they travel only when the reader says
 *   so, Phase 16).
 */
data class Backup(
    val exportedAtMillis: Long,
    val zone: String,
    val appVersion: String,
    val profile: Profile,
    val settings: UserSettings,
    val days: List<BackupDay>,
    val plans: List<SessionPlan>,
    val sessions: List<Session>,
    val diagnostics: List<DiagnosticsEvent> = emptyList(),
    val journeys: List<WayJourney> = emptyList(),
    val stepLengthSamples: Map<Long, List<StepLengthSample>> = emptyMap(),
) {
    /** The first and last day with steps, null for a backup without any. */
    val firstDay: Long? get() = days.minOfOrNull { it.summary.localEpochDay }
    val lastDay: Long? get() = days.maxOfOrNull { it.summary.localEpochDay }

    /** All the steps it holds, for a sentence about it. */
    val totalSteps: Long get() = days.sumOf { it.summary.steps.toLong() }
}
