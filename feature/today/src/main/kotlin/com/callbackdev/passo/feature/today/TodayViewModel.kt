package com.callbackdev.passo.feature.today

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.LiveToday
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.withPending
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.sessions.DayOutings
import com.callbackdev.passo.core.domain.sessions.Outing
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDay
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.walks.WalkDetector
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.tracking.SessionControl
import com.callbackdev.passo.core.tracking.StepTracking
import com.callbackdev.passo.core.tracking.TrackingControl
import com.callbackdev.passo.core.tracking.TrackingReadiness
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * Today (PLANNING.md §11 Phase 3). Everything here runs only while the screen is collected
 * (`WhileSubscribed`), which is only while it is visible, so with the screen off nothing ticks
 * (§9): the minute clock, the typical day recomputed every 15 minutes (§6.2), the live count.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val tracking: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val control: TrackingControl,
    private val sessions: SessionRepository,
    liveSteps: LiveSteps,
    liveSession: LiveSession,
) : ViewModel() {
    private val readiness = MutableStateFlow(StepTracking.readiness(context))
    private val celebratedOn = MutableStateFlow<LocalDate?>(null)

    private val clock: Flow<Moment> = flow {
        while (true) {
            emit(Moment.now())
            delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
        }
    }.distinctUntilChanged().shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    private val day: Flow<LocalDate> = clock.map { it.date }.distinctUntilChanged()

    private val minutes: Flow<List<MinuteSteps>> = day.flatMapLatest { tracking.observeMinutesOn(it.toEpochDay()) }

    private val typical: Flow<TypicalDay?> = combine(
        day,
        clock.map { it.minute.toInt() / TYPICAL_REFRESH_MINUTES }.distinctUntilChanged(),
        settingsRepository.settings.map { it.typicalDayLine }.distinctUntilChanged(),
    ) { date, _, shown -> date to shown }
        .map { (date, shown) -> if (shown) typicalFor(date) else null }

    private val inputs: Flow<Inputs> =
        combine(clock, minutes, liveSteps.today, typical) { moment, stored, live, usual ->
            Inputs(moment, stored, live, usual)
        }

    private val preferences: Flow<Preferences> = combine(
        settingsRepository.settings,
        settingsRepository.profile,
        readiness,
        tracking.observeFirstRecordedDay(),
        celebratedOn,
    ) { settings, profile, ready, firstDay, celebrated -> Preferences(settings, profile, ready, firstDay, celebrated) }

    /**
     * Today's outings, and the one the card shows: the service's while it runs (ahead of what it
     * wrote), the stored one while the system has it stopped; else the last one over today, until
     * the reader puts it away.
     */
    private val outings: Flow<Outings> = combine(
        day.flatMapLatest { sessions.observeSessionsOn(it.toEpochDay()) },
        liveSession.current,
        sessions.observeLatestFinished(),
        sessions.summarySeen,
    ) { today, live, latest, seen ->
        val stored = today.lastOrNull { it.live }
        val card =
            live?.takeIf { it.session.live || it.session.id != seen }
                ?: stored?.let {
                    LiveSessionState(it, cadence = null, canKeepGoing = false, alertsWhileScreenOff = true)
                }
                ?: latest?.takeIf { it.id != seen && today.any { same -> same.id == it.id } }
                    ?.let { LiveSessionState(it, cadence = null, canKeepGoing = false, alertsWhileScreenOff = true) }
        Outings(today, card)
    }

    val state: StateFlow<TodayUiState?> = combine(inputs, preferences, outings, ::build)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** On every return to the screen: a permission can change in the system's settings. */
    fun refreshReadiness() {
        readiness.value = StepTracking.readiness(context)
    }

    /**
     * Resumes a pause: counting starts again from now. The baseline is forgotten first, so the
     * steps the hardware counted during the pause are not added in one go at the first sample.
     */
    fun resumeTracking() {
        viewModelScope.launch {
            control.resume()
            refreshReadiness()
        }
    }

    fun celebrated(date: LocalDate) {
        celebratedOn.value = date
    }

    fun pauseSession() {
        SessionControl.pause(context)
    }

    fun resumeSession() {
        SessionControl.resume(context)
    }

    fun stopSession() {
        SessionControl.stop(context)
    }

    fun keepGoing() {
        SessionControl.keepGoing(context)
    }

    /** The finished outing's card is put away; the outing stays in the day's list. */
    fun closeSession(id: Long) {
        viewModelScope.launch { sessions.setSummarySeen(id) }
    }

    private fun build(inputs: Inputs, prefs: Preferences, outings: Outings): TodayUiState {
        val zone = ZoneId.systemDefault()
        val date = inputs.moment.date
        val epochDay = date.toEpochDay()
        val live = inputs.live?.takeIf { it.localEpochDay == epochDay }
        val dayMinutes = inputs.stored.withPending(live?.pending.orEmpty()).map {
            DayMinute(minuteOfDay(it.epochMinute, it.localEpochDay, zone), it.steps)
        }
        val overview = TodayOverview.of(
            minutes = dayMinutes,
            profile = prefs.profile,
            goalSteps = prefs.settings.dailyGoalSteps,
            nowMinute = inputs.moment.minute,
            typical = inputs.typical,
            liveSteps = live?.steps,
        )
        val walks = if (prefs.settings.walkDetection) {
            WalkDetector.detect(dayMinutes, prefs.profile, prefs.settings.minWalkMinutes)
        } else {
            null
        }
        val status = when {
            prefs.readiness == TrackingReadiness.PERMISSION_NEEDED -> TrackingStatus.PERMISSION_NEEDED
            !prefs.settings.trackingEnabled -> TrackingStatus.PAUSED
            else -> TrackingStatus.COUNTING
        }
        return TodayUiState(
            date = date,
            nowMinute = inputs.moment.minute,
            overview = overview,
            status = status,
            units = prefs.settings.units,
            walkingStepLength = StepLengths.of(prefs.profile).walkingMeters,
            firstDay = prefs.firstRecordedDay == null || prefs.firstRecordedDay >= epochDay,
            celebrate = overview.goalReachedAt != null && prefs.celebratedOn != date,
            walks = walks,
            session = outings.card,
            // The outing under way is the card's; the list holds the ones that are over.
            outings = DayOutings.of(walks, outings.today, zone, System.currentTimeMillis())
                .filterNot { it is Outing.Planned && it.session.live },
        )
    }

    private suspend fun typicalFor(date: LocalDate): TypicalDay? =
        tracking.typicalDay(date.toEpochDay(), ZoneId.systemDefault())

    private data class Moment(val date: LocalDate, val minute: Double) {
        companion object {
            fun now(): Moment {
                val zone = ZoneId.systemDefault()
                val time = LocalTime.now(zone)
                return Moment(LocalDate.now(zone), time.hour * 60.0 + time.minute)
            }
        }
    }

    private data class Inputs(
        val moment: Moment,
        val stored: List<MinuteSteps>,
        val live: LiveToday?,
        val typical: TypicalDay?,
    )

    private data class Outings(val today: List<Session>, val card: LiveSessionState?)

    private data class Preferences(
        val settings: UserSettings,
        val profile: Profile,
        val readiness: TrackingReadiness,
        val firstRecordedDay: Long?,
        val celebratedOn: LocalDate?,
    )

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val TYPICAL_REFRESH_MINUTES = 15
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
