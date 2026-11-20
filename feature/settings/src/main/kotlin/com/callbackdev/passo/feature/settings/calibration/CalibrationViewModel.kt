package com.callbackdev.passo.feature.settings.calibration

import android.content.Context
import android.content.res.Resources
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.learning.StepLearningRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.domain.calibration.CalibratedStep
import com.callbackdev.passo.core.domain.calibration.CalibrationResult
import com.callbackdev.passo.core.domain.calibration.StepCalibration
import com.callbackdev.passo.core.domain.calibration.StepLearning
import com.callbackdev.passo.core.domain.settings.ProfileInputs
import com.callbackdev.passo.core.domain.settings.resolve
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.tracking.ProbeReading
import com.callbackdev.passo.core.tracking.StepCounterProbe
import com.callbackdev.passo.core.tracking.StepTracking
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/** Where the calibration stands. */
enum class CalibrationPhase {
    /** Choosing the step and the distance, at the start line. */
    SETUP,

    /** Walking: the count moves while the screen is on. */
    WALKING,

    /** Stop was tapped: the sensor is asked for the steps it still holds. */
    MEASURING,

    /** The step length, or why there is none. */
    RESULT,
}

/**
 * The calibration page's state.
 *
 * @property counterReady the step counter has answered since the page opened: Start needs its
 *   value to count from.
 * @property steps the steps since Start, while walking.
 * @property profile the profile now, for what the result is compared with.
 * @property learning what the outings with GPS have taught of the step (Phase 17).
 * @property cardOpen the card was asked for: from the quiet row, or from the outing whose samples
 *   made the proposal ready.
 * @property stepUsed «Use it» was just touched: the card gives way to what it changed.
 */
data class CalibrationUiState(
    val step: CalibratedStep,
    val distanceMeters: Double,
    val units: UnitPreference,
    val phase: CalibrationPhase,
    val permission: Boolean,
    val counterReady: Boolean,
    val steps: Int,
    val elapsedMillis: Long,
    val result: CalibrationResult?,
    val profile: Profile,
    val learning: StepLearning = StepLearning.NOTHING,
    val cardOpen: Boolean = false,
    val stepUsed: Boolean = false,
)

/**
 * The step calibration (PLANNING.md §11 Phase 7): a known distance, walked between Start and
 * Stop, over the steps the hardware counter moved by (`StepCalibration`). The counter is read
 * only while the page is visible ([onVisible], [onHidden]); it is cumulative, so steps taken
 * with the screen off are in the difference all the same. Start, the distance and the step
 * survive the process being stopped in the background (the counter keeps its count until the
 * phone restarts), so a walk with the phone in a pocket is never lost to it.
 */
@HiltViewModel
class CalibrationViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val probe: StepCounterProbe,
    private val settings: SettingsRepository,
    private val learning: StepLearningRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val cardOpen = MutableStateFlow(false)
    private val stepUsed = MutableStateFlow(false)

    private val latest = MutableStateFlow<Long?>(null)
    private val now = MutableStateFlow(SystemClock.elapsedRealtime())
    private val permission = MutableStateFlow(StepTracking.hasActivityRecognition(context))
    private val measuring = MutableStateFlow(false)
    private val flushes = Channel<Unit>(Channel.CONFLATED)
    private var listening: Job? = null

    private val draft = MutableStateFlow(
        Draft(
            step = handle.get<String>(KEY_STEP)?.let { name -> CalibratedStep.entries.firstOrNull { it.name == name } },
            distanceMeters = handle[KEY_DISTANCE],
            startCount = handle[KEY_START_COUNT],
            startedAt = handle[KEY_STARTED_AT],
            endCount = handle[KEY_END_COUNT],
            stoppedAt = handle[KEY_STOPPED_AT],
        ),
    )

    val state: StateFlow<CalibrationUiState?> = combine(
        combine(draft, latest, now, permission, measuring) { draft, latest, now, permission, measuring ->
            Live(draft, latest, now, permission, measuring)
        },
        settings.settings,
        settings.profile,
        learning.learning,
        combine(cardOpen, stepUsed) { open, used -> open to used },
    ) { live, userSettings, profile, learnt, (open, used) ->
        val draft = live.draft
        val step = draft.step ?: return@combine null
        val units = userSettings.units
        val distance = draft.distanceMeters ?: ProfileInputs.defaultCalibrationDistance(units.resolveHere())
        val phase = when {
            live.measuring -> CalibrationPhase.MEASURING
            draft.endCount != null -> CalibrationPhase.RESULT
            draft.startCount != null -> CalibrationPhase.WALKING
            else -> CalibrationPhase.SETUP
        }
        CalibrationUiState(
            step = step,
            distanceMeters = distance,
            units = units,
            phase = phase,
            permission = live.permission,
            counterReady = live.latest != null,
            steps = if (draft.startCount != null && live.latest != null) {
                (live.latest - draft.startCount).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
            } else {
                0
            },
            elapsedMillis = draft.startedAt?.let { ((draft.stoppedAt ?: live.now) - it).coerceAtLeast(0) } ?: 0,
            result = if (phase == CalibrationPhase.RESULT) result(draft, step, distance) else null,
            profile = profile,
            learning = learnt,
            cardOpen = open,
            stepUsed = used,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /**
     * The page opened for [step]; a page restored after the process stopped keeps its own. With
     * [card], it opens on the learnt step's card (the line on the outing that made it ready).
     */
    fun open(step: CalibratedStep, card: Boolean = false) {
        if (draft.value.step == null) change { it.copy(step = step) }
        cardOpen.value = card
        stepUsed.value = false
    }

    /** The quiet row touched: the card, with the curve as it is today. */
    fun openCard() {
        cardOpen.value = true
    }

    /** «Use it»: today and the days after follow the learnt step; past days keep theirs. */
    fun useStep(curve: LearnedStep) {
        cardOpen.value = false
        stepUsed.value = true
        viewModelScope.launch { learning.use(curve) }
    }

    /** «Not now»: the card closes, and its quiet row stays. */
    fun notNow(curve: LearnedStep) {
        cardOpen.value = false
        viewModelScope.launch { learning.notNow(curve) }
    }

    /** Back to the step in use before the learnt one. */
    fun goBackStep() {
        stepUsed.value = false
        viewModelScope.launch { learning.goBack() }
    }

    /** «Apply to past days too», confirmed: every recorded day with the step now in use. */
    fun applyProfileToPastDays() {
        stepUsed.value = false
        viewModelScope.launch { settings.applyProfileToPastDays() }
    }

    fun dismissStepUsed() {
        stepUsed.value = false
    }

    /**
     * The page is on screen: the counter is listened to, and the clock ticks once a second
     * while a walk is timed. Nothing of it runs once the page is gone (PLANNING.md §9).
     */
    fun onVisible() {
        permission.value = StepTracking.hasActivityRecognition(context)
        if (listening?.isActive == true || !permission.value) return
        listening = viewModelScope.launch {
            launch {
                probe.readings().collect { reading ->
                    when (reading) {
                        is ProbeReading.Count -> latest.value = reading.value
                        ProbeReading.Flushed -> flushes.trySend(Unit)
                    }
                }
            }
            // The clock of a walk under way; at the start line and on the result it has nothing to say.
            while (true) {
                val walk = draft.value
                if (walk.startCount != null && walk.endCount == null) now.value = SystemClock.elapsedRealtime()
                delay(TICK_MILLIS)
            }
        }
    }

    fun onHidden() {
        listening?.cancel()
        listening = null
    }

    /** The permission may have been granted meanwhile: the counter can be read now. */
    fun onPermissionResult() {
        onHidden()
        onVisible()
    }

    fun setStep(step: CalibratedStep) = change { it.copy(step = step) }

    fun setDistance(meters: Double) = change { it.copy(distanceMeters = meters) }

    /** At the start line: the counter's value now is where the walk counts from. */
    fun start() {
        val count = latest.value ?: return
        val state = state.value ?: return
        change {
            it.copy(
                distanceMeters = state.distanceMeters,
                startCount = count,
                startedAt = SystemClock.elapsedRealtime(),
                endCount = null,
                stoppedAt = null,
            )
        }
    }

    /**
     * At the finish line. The time is taken at the tap; the count once the sensor has handed over
     * what it still held (bounded, as the shutdown flush is: a sensor that never answers leaves
     * the last value it gave).
     */
    fun stop() {
        val started = draft.value.startCount ?: return
        if (measuring.value) return
        val stoppedAt = SystemClock.elapsedRealtime()
        measuring.value = true
        viewModelScope.launch {
            while (flushes.tryReceive().isSuccess) Unit
            if (probe.flush()) withTimeoutOrNull(FLUSH_WAIT_MILLIS) { flushes.receive() }
            change { it.copy(endCount = latest.value ?: started, stoppedAt = stoppedAt) }
            measuring.value = false
        }
    }

    /** Back to the start line, with the same step and distance. */
    fun again() = change { it.copy(startCount = null, startedAt = null, endCount = null, stoppedAt = null) }

    /** Keeps the measured length in the profile: today's estimates follow it, past days keep theirs. */
    fun save(onSaved: () -> Unit) {
        val result = state.value?.result as? CalibrationResult.Measured ?: return
        viewModelScope.launch {
            // A walking step measured is another length chosen: a curve that counts is put away.
            if (result.step == CalibratedStep.WALKING) {
                learning.chooseLength { StepCalibration.apply(it, result) }
            } else {
                settings.updateProfile { StepCalibration.apply(it, result) }
            }
            onSaved()
        }
    }

    private fun result(draft: Draft, step: CalibratedStep, distance: Double): CalibrationResult? {
        val start = draft.startCount ?: return null
        val end = draft.endCount ?: return null
        val duration = (draft.stoppedAt ?: 0) - (draft.startedAt ?: 0)
        return StepCalibration.measure(step, distance, start, end, duration)
    }

    private fun change(transform: (Draft) -> Draft) {
        val updated = draft.updateAndGet(transform)
        handle[KEY_STEP] = updated.step?.name
        handle[KEY_DISTANCE] = updated.distanceMeters
        handle[KEY_START_COUNT] = updated.startCount
        handle[KEY_STARTED_AT] = updated.startedAt
        handle[KEY_END_COUNT] = updated.endCount
        handle[KEY_STOPPED_AT] = updated.stoppedAt
    }

    private data class Draft(
        val step: CalibratedStep?,
        val distanceMeters: Double?,
        val startCount: Long?,
        val startedAt: Long?,
        val endCount: Long?,
        val stoppedAt: Long?,
    )

    private data class Live(
        val draft: Draft,
        val latest: Long?,
        val now: Long,
        val permission: Boolean,
        val measuring: Boolean,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val TICK_MILLIS = 1_000L

        /** As long as the shutdown flush waits (PLANNING.md §15): a sensor answers well within it. */
        const val FLUSH_WAIT_MILLIS = 1_500L

        const val KEY_STEP = "calibration_step"
        const val KEY_DISTANCE = "calibration_distance"
        const val KEY_START_COUNT = "calibration_start_count"
        const val KEY_STARTED_AT = "calibration_started_at"
        const val KEY_END_COUNT = "calibration_end_count"
        const val KEY_STOPPED_AT = "calibration_stopped_at"
    }
}

/** "System" units follow the phone's region, as everywhere in the app. */
private fun UnitPreference.resolveHere() = resolve(Resources.getSystem().configuration.locales[0]?.country)
