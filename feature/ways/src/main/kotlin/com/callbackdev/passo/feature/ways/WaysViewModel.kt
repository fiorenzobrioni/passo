package com.callbackdev.passo.feature.ways

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.ways.WayRepository
import com.callbackdev.passo.core.data.ways.distances
import com.callbackdev.passo.core.designsystem.components.CityMark
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.ways.WalkDays
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.domain.ways.WayForecast
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.WayStartChoice
import com.callbackdev.passo.core.domain.ways.WayStarts
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.Continent
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.callbackdev.passo.core.model.WayKind
import com.callbackdev.passo.core.tracking.SessionControl
import com.callbackdev.passo.core.tracking.SessionSpeech
import com.callbackdev.passo.core.tracking.StepTracking
import com.callbackdev.passo.core.tracking.TrackingReadiness
import com.callbackdev.passo.core.tracking.VoiceAvailability
import com.callbackdev.passo.core.tracking.spokenWalkSample
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Everything the two pages draw (PLANNING.md §11 Phase 11).
 *
 * @property distances each day's estimated distance, for the start sheet's "you would already
 *   be past Siena": where a start in the past would place the reader.
 * @property pace the reader's usual distance a day, for "about 7 months at your pace"; null
 *   until a week of walking says it.
 * @property active the way under way, if any.
 * @property past the ways finished or left and the city walks finished, the latest first.
 * @property walks the city walks, in the order they are listed.
 * @property live the outing under way, paused or just ended, whichever it is: one at a time.
 * @property walkVoice whether a walk's places are said aloud, and where.
 * @property canWalk whether an outing can start now: counting on, with its permission.
 * @property stepMeters the reader's walking step, for a walk's steps before it is walked.
 * @property voiceAvailability whether the phone can speak the places, once asked.
 * @property wakeUpCounter the phone has a wake-up step counter; without it a walk, which always
 *   tells its places, keeps the phone awake while it counts, and its page says so
 *   (docs/adr/0013-interval-walks.md).
 */
@Immutable
data class WaysUiState(
    val today: LocalDate,
    val units: UnitPreference,
    val firstCounted: LocalDate?,
    val distances: Map<Long, Double>,
    val pace: Double?,
    val active: JourneyView?,
    val past: List<JourneyView>,
    val walks: List<WalkView> = emptyList(),
    val live: LiveSessionState? = null,
    val walkVoice: SessionVoice = SessionVoice.HEADPHONES,
    val canWalk: WalkReadiness = WalkReadiness.READY,
    val stepMeters: Double = DEFAULT_STEP_METERS,
    val voiceAvailability: VoiceAvailability = VoiceAvailability.UNKNOWN,
    val wakeUpCounter: Boolean = true,
) {
    fun journey(id: Long): JourneyView? = active?.takeIf { it.journey.id == id }
        ?: past.firstOrNull { it.journey.id == id }
        ?: walks.firstNotNullOfOrNull { it.current?.takeIf { view -> view.journey.id == id } }

    fun walk(id: WayId): WalkView? = walks.firstOrNull { it.way.id == id }

    /** The walks of [continent]'s cities, in the order they are listed. */
    fun walksIn(continent: Continent): List<WalkView> = walks.filter { it.way.id.continent == continent }

    private companion object {
        const val DEFAULT_STEP_METERS = 0.7
    }
}

/**
 * A city walk as its page and its row show it: the journey under way on it, if any (with an
 * outing live on it, [live]), and the last time it was walked to its end.
 */
@Immutable
data class WalkView(
    val way: Way,
    val current: JourneyView?,
    val lastFinished: JourneyView?,
    val live: LiveSessionState?,
) {
    /** Where the next outing on it starts: where the journey under way stands, or the start. */
    val fromMeters: Double get() = current?.progress?.walkedMeters ?: 0.0

    /**
     * Where it stands, as its continent's map marks it: under way as its row says it (a journey
     * begun, or an outing on it), walked once walked to its end, otherwise not begun.
     */
    val mark: CityMark
        get() = when {
            live != null || current != null -> CityMark.UNDER_WAY
            lastFinished != null -> CityMark.WALKED
            else -> CityMark.NOT_BEGUN
        }
}

/** Whether a walk can start now, and if not, why. */
enum class WalkReadiness {
    READY,

    /** Counting is paused: a walk is measured from steps. */
    PAUSED,

    /** No physical activity permission: nothing is counted. */
    PERMISSION_NEEDED,
}

/** A journey with where it stands, and when it would arrive at the reader's pace. */
@Immutable
data class JourneyView(val journey: WayJourney, val progress: WayProgress, val forecast: WayForecast?)

/**
 * The Ways (PLANNING.md §11 Phase 11): computed from the days each time one changes, only while
 * a page is collected, as Insights is. One for both pages: the Ways page and a way's page show
 * the same journeys.
 */
@HiltViewModel
class WaysViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val ways: WayRepository,
    tracking: TrackingRepository,
    private val settings: SettingsRepository,
    sessions: SessionRepository,
    liveSession: LiveSession,
) : ViewModel() {
    // Read once: a phone's sensors do not change while the page is open.
    private val wakeUpCounter = StepTracking.hasWakeUpStepCounter(context)
    private val readiness = MutableStateFlow(StepTracking.readiness(context))

    /** The service's outing when it runs; the stored one when the system has stopped it. */
    private val live: Flow<LiveSessionState?> =
        combine(liveSession.current, sessions.observeLiveSession()) { live, stored ->
            live ?: stored?.let {
                LiveSessionState(it, cadence = null, canKeepGoing = false, alertsWhileScreenOff = true)
            }
        }

    /** The walks' outings, the live one with its latest totals: they move a walk. */
    private val walkOutings: Flow<Pair<List<Session>, LiveSessionState?>> =
        combine(sessions.walkSessions, live) { stored, current ->
            val outing = current?.session
            val merged = if (outing?.walk == null) {
                stored
            } else {
                stored.filter { it.id != outing.id } + outing
            }
            merged to current
        }

    // Bound only once a walk's page asks (its voice on), for "Hear it" and what the phone can say.
    private val speech = SessionSpeech(context)

    private val walkSettings =
        combine(settings.settings, settings.profile, readiness, speech.availability) { current, profile, ready, voice ->
            WalkSettings(current, StepLengths.of(profile).walkingMeters, ready, voice)
        }

    private val day: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(ZoneId.systemDefault()))
            delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
        }
    }.distinctUntilChanged()

    val state: StateFlow<WaysUiState?> = combine(
        day,
        ways.journeys,
        tracking.observeAllSummaries(),
        walkSettings,
        walkOutings,
    ) { today, journeys, summaries, (current, stepMeters, ready, voice), (outings, liveOuting) ->
        val distances = summaries.distances()
        val views = journeys.map { journey ->
            val way = Ways.of(journey.way)
            if (way.kind == WayKind.WALK) {
                // A walk moves with its outings, not with the days.
                JourneyView(
                    journey,
                    WayProgress.of(way, journey, WalkDays.of(journey, outings), today.toEpochDay()),
                    null,
                )
            } else {
                val progress = WayProgress.of(way, journey, distances, today.toEpochDay())
                val forecast = if (journey.state == WayJourneyState.ACTIVE && !progress.finished) {
                    WayForecast.of(distances, today, progress.metersLeft)
                } else {
                    null
                }
                JourneyView(journey, progress, forecast)
            }
        }
        val (wayViews, walkViews) = views.partition { it.journey.way.kind == WayKind.WAY }
        WaysUiState(
            today = today,
            units = current.units,
            firstCounted = summaries.minOfOrNull { it.localEpochDay }?.let(LocalDate::ofEpochDay),
            distances = distances,
            pace = WayForecast.pace(distances, today),
            active = wayViews.firstOrNull { it.journey.state == WayJourneyState.ACTIVE },
            // A walk begun again leaves its earlier journey behind: only the ones walked to the
            // end are the reader's to keep.
            past = views.filter {
                it.journey.state == WayJourneyState.FINISHED ||
                    (it.journey.state == WayJourneyState.LEFT && it.journey.way.kind == WayKind.WAY)
            },
            walks = Ways.walks.map { walk ->
                val mine = walkViews.filter { it.journey.way == walk.id }
                WalkView(
                    way = walk,
                    current = mine.firstOrNull { it.journey.state == WayJourneyState.ACTIVE },
                    lastFinished = mine.firstOrNull { it.journey.state == WayJourneyState.FINISHED },
                    live = liveOuting?.takeIf { it.session.walk == walk.id },
                )
            },
            live = liveOuting,
            walkVoice = current.walkVoice,
            canWalk = when {
                ready == TrackingReadiness.PERMISSION_NEEDED -> WalkReadiness.PERMISSION_NEEDED
                !current.trackingEnabled -> WalkReadiness.PAUSED
                else -> WalkReadiness.READY
            },
            stepMeters = stepMeters,
            voiceAvailability = voice,
            wakeUpCounter = wakeUpCounter,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    init {
        // A way walked to its end is finished here too, should the page see it before the
        // notifier does: the days after it must not count.
        viewModelScope.launch {
            state.collect { current ->
                val active = current?.active ?: return@collect
                active.progress.finishedEpochDay?.let { ways.finish(active.journey.id, it) }
            }
        }
    }

    /** Starts [way] from the day [choice] means ([chosen] for a day the reader picked). */
    fun start(way: WayId, choice: WayStartChoice, chosen: LocalDate?) {
        val current = state.value ?: return
        viewModelScope.launch {
            val today = LocalDate.now(ZoneId.systemDefault())
            val day = WayStarts.dayOf(choice, today, current.firstCounted, chosen)
            ways.start(way, day, today, System.currentTimeMillis())
        }
    }

    /**
     * Starts an outing on the city walk [walk]: from where its journey stands, or, [again],
     * from its first place on a new one (the tracking service decides, on its own state).
     */
    fun startWalk(walk: WayId, again: Boolean) {
        SessionControl.startWalk(context, walk, again)
    }

    fun pauseWalk() {
        SessionControl.pause(context)
    }

    fun resumeWalk() {
        SessionControl.resume(context)
    }

    fun stopWalk() {
        SessionControl.stop(context)
    }

    fun keepGoing() {
        SessionControl.keepGoing(context)
    }

    fun setWalkVoice(voice: SessionVoice) {
        if (voice != SessionVoice.OFF) speech.prepare()
        viewModelScope.launch { settings.updateSettings { it.copy(walkVoice = voice) } }
    }

    /** A walk's page with its voice on: the engine is bound, to say whether it can speak. */
    fun prepareVoice() = speech.prepare()

    /** The next place of [walk] as it would be told on reaching it: the reader's own walk. */
    fun tryWalkVoice(walk: WayId) {
        val current = state.value ?: return
        val view = current.walk(walk) ?: return
        speech.preview(context.spokenWalkSample(view.way, view.fromMeters.toInt(), current.units))
    }

    override fun onCleared() {
        speech.release()
    }

    fun refreshReadiness() {
        readiness.value = StepTracking.readiness(context)
    }

    /**
     * Puts the way [journeyId] down, today, where the reader stands; with nothing of it walked,
     * there is nothing to keep, and it goes.
     */
    fun leave(journeyId: Long) {
        val walked = state.value?.journey(journeyId)?.progress?.walkedMeters
        viewModelScope.launch {
            if (walked != null && walked < NOTHING_WALKED_METERS) {
                ways.delete(journeyId)
            } else {
                ways.leave(journeyId, LocalDate.now(ZoneId.systemDefault()))
            }
        }
    }

    /** Deletes the journey [journeyId] from Your ways, for good (the page asked first). */
    fun delete(journeyId: Long) {
        viewModelScope.launch { ways.delete(journeyId) }
    }

    private data class WalkSettings(
        val settings: UserSettings,
        val stepMeters: Double,
        val readiness: TrackingReadiness,
        val voice: VoiceAvailability,
    )

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L

        /** Under a metre: nothing walked, a way started by mistake or a moment ago. */
        const val NOTHING_WALKED_METERS = 1.0
    }
}
