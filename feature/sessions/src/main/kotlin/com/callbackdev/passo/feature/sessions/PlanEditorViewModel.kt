package com.callbackdev.passo.feature.sessions

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.SessionAmount
import com.callbackdev.passo.core.domain.sessions.SessionAnnouncement
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.sessions.cadenceFloor
import com.callbackdev.passo.core.domain.settings.resolve
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UnitSystem
import com.callbackdev.passo.core.tracking.SessionHaptics
import com.callbackdev.passo.core.tracking.SessionShortcuts
import com.callbackdev.passo.core.tracking.SessionSpeech
import com.callbackdev.passo.core.tracking.VoiceAvailability
import com.callbackdev.passo.core.tracking.spokenSample
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.ceil

/**
 * The editor of one outing.
 *
 * @property original the plan as stored (or the new one's defaults): what "discard" goes back to.
 * @property draft the plan as edited so far.
 * @property imperial distances step by quarter miles.
 * @property canVibrate the phone has a vibrator: without one the switch is not offered.
 * @property voiceAvailability whether the phone can speak in the app's language, once asked
 *   (the engine is bound only when the outing speaks, or the reader picks a voice).
 */
@Immutable
data class PlanEditorState(
    val original: SessionPlan,
    val draft: SessionPlan,
    val isNew: Boolean,
    val units: UnitPreference,
    val imperial: Boolean,
    val lengths: StepLengths,
    val restOfDaySteps: Int,
    val canVibrate: Boolean,
    val voiceAvailability: VoiceAvailability = VoiceAvailability.UNKNOWN,
) {
    val changed: Boolean get() = draft != original
}

/**
 * The outing editor (PLANNING.md §11 Phase 10). One draft at a time: [open] starts it from the
 * stored plan or a new one; nothing is written until [save].
 */
@HiltViewModel
class PlanEditorViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val sessions: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val tracking: TrackingRepository,
    private val liveSteps: LiveSteps,
) : ViewModel() {
    private val editor = MutableStateFlow<PlanEditorState?>(null)

    // Bound only while the editor shows a plan that speaks; released with the view model.
    private val speech = SessionSpeech(context)

    val state: StateFlow<PlanEditorState?> = combine(editor, speech.availability) { state, voice ->
        state?.copy(voiceAvailability = voice)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Starts editing plan [id], or a new plan when it is null. */
    fun open(id: Long?) {
        editor.value = null
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val profile = settingsRepository.profile.first()
            val stored = id?.let { sessions.plan(it) }
            val plan = stored ?: SessionPlans.NEW
            val day = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
            val steps = liveSteps.today.value?.takeIf { it.localEpochDay == day }?.steps ?: tracking.stepsOn(day)
            if (plan.voice != SessionVoice.OFF) speech.prepare()
            editor.value = PlanEditorState(
                original = plan,
                draft = plan,
                isNew = stored == null,
                units = settings.units,
                imperial = settings.units.resolve(systemRegion()) == UnitSystem.IMPERIAL,
                lengths = StepLengths.of(profile),
                restOfDaySteps = SessionPlans.restOfDay(steps, settings.dailyGoalSteps),
                canVibrate = SessionHaptics.available(context),
            )
        }
    }

    fun rename(name: String) = edit { it.draft.copy(name = name.take(MAX_NAME)) }

    /** A new goal kind keeps the same outing, in the new quantity. */
    fun goalKind(kind: SessionGoalKind) = edit { state ->
        val current = state.draft
        val value = if (kind == SessionGoalKind.REST_OF_DAY) {
            0
        } else {
            val converted = SessionPlans.convert(current, kind, state.lengths, state.restOfDaySteps)
            SessionPlans.snapForEditor(kind, converted.toDouble(), state.imperial)
        }
        current.copy(goalKind = kind, goalValue = value)
    }

    fun goalValue(value: Double) = edit { state ->
        state.draft.copy(goalValue = SessionPlans.snapForEditor(state.draft.goalKind, value, state.imperial))
    }

    fun nudge(up: Boolean) = edit { state ->
        state.draft.copy(
            goalValue = SessionPlans.nudge(state.draft.goalKind, state.draft.goalValue, up, state.imperial),
        )
    }

    fun intensity(intensity: SessionIntensity) = edit { it.draft.copy(intensity = intensity) }

    fun milestone(milestone: SessionMilestone, on: Boolean) = edit {
        it.draft.copy(milestones = if (on) it.draft.milestones + milestone else it.draft.milestones - milestone)
    }

    fun vibrate(on: Boolean) = edit { it.draft.copy(vibrate = on) }

    fun tryVibration(milestone: SessionMilestone) = SessionHaptics.play(context, milestone)

    fun voice(voice: SessionVoice) {
        if (voice != SessionVoice.OFF) speech.prepare()
        edit { it.draft.copy(voice = voice) }
    }

    /** The outing's halfway, as it would be said: the same words, the reader's own numbers. */
    fun tryVoice() {
        val state = editor.value ?: return
        val plan = state.draft
        val half = when (plan.goalKind) {
            SessionGoalKind.REST_OF_DAY -> SessionAmount(
                SessionGoalKind.STEPS,
                (state.restOfDaySteps / 2).coerceAtLeast(1).toDouble(),
            )

            SessionGoalKind.TIME -> SessionAmount(plan.goalKind, ceil(plan.goalValue / 2.0))

            else -> SessionAmount(plan.goalKind, plan.goalValue / 2.0)
        }
        val cadence = plan.intensity.cadenceFloor?.plus(SAMPLE_CADENCE_MARGIN)
        val sample = SessionAnnouncement.Milestone(
            SessionMilestone.HALF,
            half,
            cadence,
            SessionAnnouncement.verdict(plan.intensity, cadence),
        )
        speech.preview(context.spokenSample(sample, state.units))
    }

    /** The editor is left: the engine is let go (the view model outlives the page). */
    fun close() = speech.release()

    override fun onCleared() {
        speech.release()
    }

    /** Writes the draft, then [done]. */
    fun save(done: () -> Unit) {
        val state = editor.value ?: return
        viewModelScope.launch {
            sessions.savePlan(state.draft)
            refreshShortcuts(state.units)
            done()
        }
    }

    fun delete(done: () -> Unit) {
        val state = editor.value ?: return
        viewModelScope.launch {
            if (!state.isNew) sessions.deletePlan(state.draft.id)
            refreshShortcuts(state.units)
            done()
        }
    }

    private suspend fun refreshShortcuts(units: UnitPreference) =
        SessionShortcuts.update(context, sessions.plansByUse(), units)

    private fun edit(transform: (PlanEditorState) -> SessionPlan) {
        editor.update { state -> state?.copy(draft = transform(state)) }
    }

    private fun systemRegion(): String? = android.content.res.Resources.getSystem().configuration.locales[0]?.country

    private companion object {
        const val MAX_NAME = 40

        /** The sample walks a little above its pace, as a reader keeping it would. */
        const val SAMPLE_CADENCE_MARGIN = 8
    }
}
