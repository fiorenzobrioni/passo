package com.callbackdev.passo.feature.sessions

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.learning.StepLearningRepository
import com.callbackdev.passo.core.data.rounds.RoundRepository
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.sessions.OutingShapes
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.domain.rounds.KeptRound
import com.callbackdev.passo.core.domain.rounds.RoundComparison
import com.callbackdev.passo.core.domain.rounds.RoundMerge
import com.callbackdev.passo.core.domain.rounds.RoundTimes
import com.callbackdev.passo.core.domain.rounds.YourRounds
import com.callbackdev.passo.core.domain.routes.GpsLineAction
import com.callbackdev.passo.core.domain.routes.GpxCodec
import com.callbackdev.passo.core.domain.routes.LiveMeasure
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.sessions.OutingShape
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.tracking.OutingStarter
import com.callbackdev.passo.core.tracking.SessionControl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * An outing's route as its page draws it.
 *
 * @property shape its lines, its frame and what one sentence says of it.
 * @property altitudes where its heights came from, for the line under the climb.
 */
@Immutable
class DrawnRoute(val shape: RouteShape, val altitudes: AltitudeSource?) {
    val points: List<RoutePoint> get() = shape.points
}

/**
 * The usual round an outing over is a walk of (PLANNING.md §11 Phase 20), once the round is known
 * (its third walk), and how this walk compares with the walks of it before.
 */
@Immutable
data class OutingRound(val round: KeptRound, val comparison: RoundComparison)

/**
 * An outing's own page.
 *
 * @property route its route, for an outing measured with GPS that still has one; null for any
 *   other (no GPS, never a fix, a route the reader deleted, a restore from the cloud copy).
 * @property over the outing is over for good: not counting, and no «Keep going» or «Resume»
 *   can take it back any more. Only then is its route the reader's to share, save or delete:
 *   before, the line may still grow.
 * @property stepAtPace the step this outing measured, its samples' metres over their steps; null
 *   without a sample (PLANNING.md §11 Phase 17).
 * @property stepReady this is the outing whose samples made the learnt step's proposal ready, and
 *   the proposal still waits: its page offers it.
 * @property measure what the service says of an outing under way beyond its stored numbers (the
 *   line about the satellites, the pace now, the stretch whose fixes are not in yet); null for an
 *   outing that is not the one under way (Phase 21: the page carries the card's lines).
 * @property shape its minutes and its day's goal, for «The shape of the walk» (Phase 21.3); its
 *   minutes are null for an outing from before its pauses were kept, and the section is not shown.
 * @property round the usual round it is a walk of (Phase 20); null for every other outing.
 * @property lost a round whose walks all lost their routes this outing may be a walk of, asked on
 *   its page (§15, «Phase 20: a round with no line asked at its first walk»); null for none.
 */
@Immutable
data class OutingUiState(
    val session: Session,
    val route: DrawnRoute?,
    val units: UnitPreference,
    val over: Boolean = !session.live,
    val stepAtPace: Double? = null,
    val stepReady: Boolean = false,
    val measure: LiveMeasure? = null,
    val shape: OutingShape? = null,
    val round: OutingRound? = null,
    val lost: KeptRound? = null,
)

/**
 * An outing's page (PLANNING.md §11 Phase 16). The pages share this one model, as every page of
 * the app does: it is told which outing is open ([open]), and its state runs only while a page
 * collects it. The route is read again only when it changes (a route still being written, one
 * the reader deletes), off the main thread.
 *
 * While the outing counts (Phase 21) the page follows the service's own state of it, as the card
 * does, so the two never say different numbers, and sends the card's commands.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class OutingViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val outings: OutingStarter,
    private val sessions: SessionRepository,
    private val routes: RouteRepository,
    settingsRepository: SettingsRepository,
    live: LiveSession,
    learning: StepLearningRepository,
    shapes: OutingShapes,
    private val rounds: RoundRepository,
) : ViewModel() {
    private val opened = MutableStateFlow<Long?>(null)

    val state: StateFlow<OutingUiState?> = opened
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                val route = routes.observePointCount(id).distinctUntilChanged().mapLatest { read(id) }
                val units = settingsRepository.settings.map { it.units }.distinctUntilChanged()
                // The service's own state of it while it counts, or while an end can be taken back:
                // what the card shows, a step batch fresher than what is stored.
                val running = live.current.map { it?.takeIf { state -> state.session.id == id } }
                    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)
                val stored = sessions.observeSession(id)
                    .shareIn(viewModelScope, SharingStarted.WhileSubscribed(), replay = 1)
                // Its minutes and its day's goal (Phase 21.3); the route is the map's, read above.
                val shape = shapes.of(combine(stored, running) { s, l -> l?.session ?: s }, withRoute = false)
                val step = combine(
                    learning.stepOf(id),
                    learning.learning.map { it.proposed && it.readyOutingId == id }.distinctUntilChanged(),
                ) { atPace, ready -> atPace to ready }
                // Its usual round (Phase 20), once known, and the pairs said not the same: read only
                // while the page is seen.
                val known = combine(rounds.kept, rounds.apart, ::Pair)
                combine(stored, route, combine(units, shape, known, ::Triple), running, step) {
                        saved,
                        drawn,
                        (unit, outingShape, rounding),
                        liveState,
                        (atPace, ready),
                    ->
                    val (kept, apart) = rounding
                    val outingRound = YourRounds.of(id, kept)?.let { found ->
                        found.walks.firstOrNull { it.id == id }?.let {
                            OutingRound(found, RoundTimes.comparison(it, found.walks))
                        }
                    }
                    (liveState?.session ?: saved)?.let {
                        // A round with no line it may give its line back to: only with its route.
                        val lost = it.takeIf { outingRound == null && drawn != null }
                            ?.let { outing -> RoundMerge.lostFor(outing, kept, apart) }
                        OutingUiState(
                            it,
                            drawn,
                            unit,
                            // Ended a moment ago, and the service can still take it back: not over yet.
                            over = !it.live && liveState?.canKeepGoing != true,
                            stepAtPace = atPace,
                            stepReady = ready,
                            measure = liveState?.measure,
                            shape = outingShape?.takeIf { shape -> shape.sessionId == it.id },
                            round = outingRound,
                            lost = lost,
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun open(sessionId: Long) {
        opened.value = sessionId
    }

    fun close() {
        opened.value = null
    }

    fun pause() = SessionControl.pause(context)

    fun resume() = SessionControl.resume(context)

    fun stop() = SessionControl.stop(context)

    fun keepGoing() = SessionControl.keepGoing(context)

    /** What the line about the satellites asks for, through the outings' one door, as on the card. */
    fun gps(action: GpsLineAction) = outings.onCardAction(action)

    /**
     * Forgets the open outing's route: the line and its places, never to be written again. The
     * outing and its numbers stay. Only once it is over for good, as its page offers it.
     */
    fun deleteRoute() {
        val session = state.value?.takeIf { it.over }?.session ?: return
        viewModelScope.launch { routes.forget(session.id, session.startedAtMillis) }
    }

    /**
     * «Not the same round»: the open outing leaves its round, never to be matched with its walks
     * again (PLANNING.md §11 Phase 20). Only once it is over for good, as its page offers it.
     */
    fun notTheSameRound() {
        val session = state.value?.takeIf { it.over && it.round != null }?.session ?: return
        viewModelScope.launch { rounds.notTheSame(session.id) }
    }

    /**
     * «It was»: the open outing is a walk of [round], which had lost every route; it gives it its line
     * back. Only once it is over for good, as its page asks it.
     */
    fun wasLostRound(round: KeptRound) {
        val session = state.value?.takeIf { it.over && it.lost?.id == round.id }?.session ?: return
        viewModelScope.launch { rounds.joinLost(round.id, session.id) }
    }

    /** «It wasn't»: the open outing is not a walk of [round], and its page asks no more. */
    fun wasNotLostRound(round: KeptRound) {
        val session = state.value?.takeIf { it.over && it.lost?.id == round.id }?.session ?: return
        viewModelScope.launch { rounds.notThisRound(session.id, round.id) }
    }

    /** The open outing's route as a GPX document named [name]; null when it has none. */
    suspend fun gpx(name: String): String? {
        val points = state.value?.route?.points ?: return null
        return withContext(Dispatchers.Default) { GpxCodec.encode(name, points) }
    }

    private suspend fun read(sessionId: Long): DrawnRoute? {
        val session = sessions.session(sessionId) ?: return null
        val route = routes.route(sessionId, session.startedAtMillis) ?: return null
        return withContext(Dispatchers.Default) {
            RouteShape.of(route.points)?.let { shape -> DrawnRoute(shape, route.altitudes) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
