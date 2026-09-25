package com.callbackdev.passo.feature.sessions

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.tracking.SessionControl
import com.callbackdev.passo.core.tracking.SessionShortcuts
import com.callbackdev.passo.core.tracking.StepTracking
import com.callbackdev.passo.core.tracking.TrackingControl
import com.callbackdev.passo.core.tracking.TrackingReadiness
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Whether an outing can start now, and if not, why. */
enum class SessionsStatus {
    READY,

    /** Counting is paused: an outing is measured from steps. */
    PAUSED,

    /** No physical activity permission: nothing is counted. */
    PERMISSION_NEEDED,
}

/**
 * The Outings page.
 *
 * @property live the outing under way, paused, or just ended by its goal.
 * @property restOfDaySteps what "the rest of the day" means right now.
 * @property alertsWhileScreenOff the phone's step counter can wake it for a signal.
 */
@Immutable
data class SessionsUiState(
    val plans: List<SessionPlan>,
    val live: LiveSessionState?,
    val units: UnitPreference,
    val lengths: StepLengths,
    val restOfDaySteps: Int,
    val status: SessionsStatus,
    val alertsWhileScreenOff: Boolean,
)

/**
 * The Outings page (PLANNING.md §11 Phase 10). Runs only while collected: the day's count it
 * reads for "the rest of the day" is the live one the service already publishes.
 */
@HiltViewModel
class SessionsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val sessions: SessionRepository,
    private val settingsRepository: SettingsRepository,
    private val control: TrackingControl,
    tracking: TrackingRepository,
    liveSteps: LiveSteps,
    liveSession: LiveSession,
) : ViewModel() {
    private val readiness = MutableStateFlow(StepTracking.readiness(context))

    // Today's count: the service's live one, the stored one while it is not running. The day is
    // the one the page opened on; the page is not kept open across midnight.
    private val today = LocalDate.now(ZoneId.systemDefault()).toEpochDay()
    private val todaySteps: Flow<Int> = combine(liveSteps.today, tracking.observeStepsOn(today)) { live, stored ->
        live?.takeIf { it.localEpochDay == today }?.steps ?: stored
    }

    /** The service's outing when it runs; the stored one when the system has stopped it. */
    private val live: Flow<LiveSessionState?> = combine(liveSession.current, sessions.observeLiveSession()) { live, stored ->
        live ?: stored?.let { LiveSessionState(it, cadence = null, canKeepGoing = false, alertsWhileScreenOff = true) }
    }

    val state: StateFlow<SessionsUiState?> = combine(
        sessions.plans,
        live,
        combine(settingsRepository.settings, settingsRepository.profile, ::Pair),
        todaySteps,
        readiness,
    ) { plans, live, (settings, profile), steps, ready ->
        SessionsUiState(
            plans = plans,
            live = live,
            units = settings.units,
            lengths = StepLengths.of(profile),
            restOfDaySteps = (settings.dailyGoalSteps - steps).coerceAtLeast(0),
            status = when {
                ready == TrackingReadiness.PERMISSION_NEEDED -> SessionsStatus.PERMISSION_NEEDED
                !settings.trackingEnabled -> SessionsStatus.PAUSED
                else -> SessionsStatus.READY
            },
            alertsWhileScreenOff = StepTracking.hasWakeUpStepCounter(context),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun refreshReadiness() {
        readiness.value = StepTracking.readiness(context)
    }

    fun start(planId: Long) {
        SessionControl.start(context, planId)
    }

    fun pause() {
        SessionControl.pause(context)
    }

    fun resume() {
        SessionControl.resume(context)
    }

    fun stop() {
        SessionControl.stop(context)
    }

    fun keepGoing() {
        SessionControl.keepGoing(context)
    }

    fun resumeTracking() {
        viewModelScope.launch {
            control.resume()
            refreshReadiness()
        }
    }

    /** The launcher's shortcuts, after the plans changed. */
    fun refreshShortcuts() {
        viewModelScope.launch {
            SessionShortcuts.update(context, sessions.plansByUse(), settingsRepository.settings.first().units)
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
