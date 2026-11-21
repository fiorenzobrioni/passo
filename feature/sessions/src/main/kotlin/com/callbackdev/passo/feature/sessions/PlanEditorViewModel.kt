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
import com.callbackdev.passo.core.domain.routes.GpsAnswer
import com.callbackdev.passo.core.domain.routes.GpsReadiness
import com.callbackdev.passo.core.domain.routes.GpsStart
import com.callbackdev.passo.core.domain.routes.GpsSwitchNote
import com.callbackdev.passo.core.domain.sessions.OutingMarks
import com.callbackdev.passo.core.domain.sessions.SessionAmount
import com.callbackdev.passo.core.domain.sessions.SessionAnnouncement
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.sessions.cadenceFloor
import com.callbackdev.passo.core.domain.sessions.typicalCadence
import com.callbackdev.passo.core.domain.settings.resolve
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMarks
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UnitSystem
import com.callbackdev.passo.core.tracking.Gps
import com.callbackdev.passo.core.tracking.OutingStarter
import com.callbackdev.passo.core.tracking.SessionHaptics
import com.callbackdev.passo.core.tracking.SessionShortcuts
import com.callbackdev.passo.core.tracking.SessionSpeech
import com.callbackdev.passo.core.tracking.StepTracking
import com.callbackdev.passo.core.tracking.VoiceAvailability
import com.callbackdev.passo.core.tracking.spokenIntervalSample
import com.callbackdev.passo.core.tracking.spokenMarkSample
import com.callbackdev.passo.core.tracking.spokenOpenSample
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
import kotlin.math.roundToInt

/**
 * The editor of one outing.
 *
 * @property original the plan as stored (or the new one's defaults): what "discard" goes back to.
 * @property draft the plan as edited so far.
 * @property imperial distances step by quarter miles.
 * @property canVibrate the phone has a vibrator: without one the switch is not offered.
 * @property voiceAvailability whether the phone can speak in the app's language, once asked
 *   (the engine is bound only when the outing speaks, or the reader picks a voice).
 * @property wakeUpCounter the phone has a wake-up step counter: an outing's signals reach it on
 *   time with the screen off by themselves; without it, Passo keeps the phone awake for them
 *   while the outing counts (docs/adr/0013-interval-walks.md), and the editor says so.
 * @property canMeasureWithGps the phone has a satellite receiver: without one «Measure with GPS»
 *   is not offered (docs/adr/0016-location.md).
 * @property gpsReadiness whether the phone could measure an outing now, read when the editor
 *   opens, and [gpsAnswer] what the reader last answered when asked for the permission: what
 *   the switch says under it comes from the two.
 * @property gpsTurnedOn the reader turned «Measure with GPS» on in this editor, with their own
 *   hand: after a refusal, that is them asking to be asked again. A plan opened with the switch
 *   already on is not.
 * @property keptGoal the goal the draft had before «None» was picked (Phase 21), so «One goal»
 *   finds it again, as «Intervals» keeps its sets; null until then.
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
    val wakeUpCounter: Boolean = true,
    val canMeasureWithGps: Boolean = false,
    val gpsReadiness: GpsReadiness = GpsReadiness.READY,
    val gpsAnswer: GpsAnswer = GpsAnswer.NEVER_ASKED,
    val gpsTurnedOn: Boolean = false,
    val keptGoal: SessionPlan? = null,
) {
    val changed: Boolean get() = draft != original

    /** What the switch says under it: never on, without a word, over an outing left to the steps. */
    val gpsNote: GpsSwitchNote
        get() = GpsStart.switchNote(wasOn = !gpsTurnedOn, isOn = draft.gps, gpsReadiness, gpsAnswer)

    /**
     * Whether an outing of this draft would be measured, as far as the editor can tell: the
     * switch on, a receiver to measure with, and no refusal standing. The notes on what the
     * estimate is and on what keeps the phone awake read this, not the switch alone.
     */
    val measuresWithGps: Boolean
        get() = draft.gps && canMeasureWithGps && gpsNote != GpsSwitchNote.NOT_ALLOWED

    /** «Measure with GPS» touched: the plan's wish, and whose hand turned it on. */
    fun withGps(on: Boolean): PlanEditorState = copy(draft = draft.copy(gps = on), gpsTurnedOn = on)
}

/** What the editor's «Kind» says a plan is: one goal, none (Phase 21), or intervals (Phase 13). */
enum class EditorKind {
    GOAL,
    OPEN,
    INTERVALS,
}

val SessionPlan.editorKind: EditorKind
    get() = when {
        open -> EditorKind.OPEN
        goalKind == SessionGoalKind.INTERVALS -> EditorKind.INTERVALS
        else -> EditorKind.GOAL
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
    private val outings: OutingStarter,
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
                wakeUpCounter = StepTracking.hasWakeUpStepCounter(context),
                canMeasureWithGps = Gps.hasReceiver(context),
                gpsReadiness = Gps.readiness(context),
                gpsAnswer = outings.gpsAnswer(),
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

    /**
     * One goal, none (Phase 21) or intervals (Phase 13). Intervals keep the reader's sets, or the
     * protocol's; none keeps the goal it leaves, which one goal finds again; back to one goal from
     * intervals, the same outing in minutes, and from none with no goal kept, half an hour.
     */
    fun kind(kind: EditorKind) = editor.update { state ->
        state ?: return@update null
        val current = state.draft
        if (current.editorKind == kind) return@update state
        when (kind) {
            EditorKind.OPEN -> state.copy(
                draft = current.copy(goalKind = SessionGoalKind.OPEN, goalValue = 0),
                keptGoal = current.takeIf { it.editorKind == EditorKind.GOAL } ?: state.keptGoal,
            )

            EditorKind.INTERVALS -> state.copy(
                draft = SessionPlans.withIntervals(current, current.intervals),
                keptGoal = current.takeIf { it.editorKind == EditorKind.GOAL } ?: state.keptGoal,
            )

            EditorKind.GOAL -> state.copy(draft = goalAgain(state))
        }
    }

    /** The draft back to one goal: the one kept when it left, else the same outing in a goal's terms. */
    private fun goalAgain(state: PlanEditorState): SessionPlan {
        val current = state.draft
        val kept = state.keptGoal
        if (kept != null) return current.copy(goalKind = kept.goalKind, goalValue = kept.goalValue)
        val minutes = if (current.goalKind == SessionGoalKind.INTERVALS) {
            current.intervals.totalMinutes
        } else {
            SessionPlans.convert(current, SessionGoalKind.TIME, state.lengths, state.restOfDaySteps)
        }
        return current.copy(
            goalKind = SessionGoalKind.TIME,
            goalValue = SessionPlans.snapForEditor(SessionGoalKind.TIME, minutes.toDouble(), state.imperial),
        )
    }

    /** The sets changed by [change], kept in the editor's ranges. */
    fun sets(change: (IntervalSets) -> IntervalSets) = edit { state ->
        SessionPlans.withIntervals(state.draft, change(state.draft.intervals))
    }

    fun tryInterval(fast: Boolean) = SessionHaptics.playInterval(context, fast)

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

    /**
     * «Measure with GPS». Only the plan's wish is kept here: the permission is asked when an
     * outing with it first starts, in context, never from the editor. Turned on again after a
     * refusal and saved, it is the reader asking to be asked again ([save]).
     */
    fun gps(on: Boolean) = editor.update { it?.withGps(on) }

    fun vibrate(on: Boolean) = edit { it.draft.copy(vibrate = on) }

    fun tryVibration(milestone: SessionMilestone) = SessionHaptics.play(context, milestone)

    fun tryEndedStill() = SessionHaptics.playEndedStill(context)

    /**
     * An open plan's mark on the way (Phase 21.2): a touch is a choice, kept whatever the GPS switch
     * does after it. Until the first, the plan follows the switch.
     */
    fun marks(marks: SessionMarks) = edit { it.draft.copy(marks = marks) }

    /** The mark's pulse, felt before it is chosen. */
    fun tryMark() = SessionHaptics.playMark(context)

    fun voice(voice: SessionVoice) {
        if (voice != SessionVoice.OFF) speech.prepare()
        edit { it.draft.copy(voice = voice) }
    }

    /** The outing's halfway, as it would be said: the same words, the reader's own numbers. */
    fun tryVoice() {
        val state = editor.value ?: return
        val plan = state.draft
        // An open outing says its marks on the way (Phase 21.2): its first, as it would be said.
        // With none, it says nothing on the way: what it says when the reader stops it.
        if (plan.open) {
            speech.preview(openSample(state))
            return
        }
        // An interval walk says its changes, not shares of the goal: its first fast interval.
        if (plan.goalKind == SessionGoalKind.INTERVALS) {
            speech.preview(context.spokenIntervalSample(plan.intervals.fastMinutes))
            return
        }
        val half = when (plan.goalKind) {
            SessionGoalKind.REST_OF_DAY -> SessionAmount(
                SessionGoalKind.STEPS,
                (state.restOfDaySteps / 2).coerceAtLeast(1).toDouble(),
            )

            SessionGoalKind.TIME, SessionGoalKind.INTERVALS -> SessionAmount(plan.goalKind, ceil(plan.goalValue / 2.0))

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

    /**
     * What an open plan's first mark would say: a kilometre (or a mile) at the plan's pace with the
     * reader's own step, measured where the plan would be measured, about where not; or ten minutes
     * and the steps they come to at that pace.
     */
    private fun openSample(state: PlanEditorState): String {
        val plan = state.draft
        val cadence = plan.intensity.typicalCadence
        return when (plan.marksInForce) {
            SessionMarks.DISTANCE -> {
                val meters = OutingMarks.meters(state.imperial)
                val seconds = meters / state.lengths.forCadence(cadence) / cadence * SECONDS_PER_MINUTE
                val measured = state.measuresWithGps
                context.spokenMarkSample(
                    SessionAnnouncement.DistanceMark(
                        count = 1,
                        miles = state.imperial,
                        movingMinutes = (seconds / SECONDS_PER_MINUTE).roundToInt().coerceAtLeast(1),
                        measured = measured,
                        paceSecondsPerKm = if (measured) seconds / (meters / METERS_PER_KM) else null,
                    ),
                )
            }

            SessionMarks.TIME -> context.spokenMarkSample(
                SessionAnnouncement.TimeMark(SAMPLE_MARK_MINUTES, SAMPLE_MARK_MINUTES * cadence),
                state.units,
            )

            SessionMarks.NONE -> context.spokenOpenSample(state.units)
        }
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
            outings.planSaved(wasOn = !state.gpsTurnedOn, isOn = state.draft.gps)
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

        /** A time mark's minutes, as the outing tells them ([OutingMarks.TIME_MARK_MILLIS]). */
        const val SAMPLE_MARK_MINUTES = (OutingMarks.TIME_MARK_MILLIS / 60_000L).toInt()
        const val SECONDS_PER_MINUTE = 60.0
        const val METERS_PER_KM = 1_000.0
    }
}
