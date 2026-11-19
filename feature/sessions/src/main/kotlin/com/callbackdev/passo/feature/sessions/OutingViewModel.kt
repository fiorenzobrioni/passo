package com.callbackdev.passo.feature.sessions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.sessions.LiveSession
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.domain.routes.GpxCodec
import com.callbackdev.passo.core.domain.routes.RouteGround
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.ways.WayMap
import com.callbackdev.passo.core.model.AltitudeSource
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.UnitPreference
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * An outing's route as its page draws it.
 *
 * @property shape its lines, its frame and what one sentence says of it.
 * @property ground the map of the city it lies in, where Passo carries one; null for a plain ground.
 * @property altitudes where its heights came from, for the line under the climb.
 */
@Immutable
class DrawnRoute(val shape: RouteShape, val ground: WayMap?, val altitudes: AltitudeSource?) {
    val points: List<RoutePoint> get() = shape.points
}

/**
 * An outing's own page.
 *
 * @property route its route, for an outing measured with GPS that still has one; null for any
 *   other (no GPS, never a fix, a route the reader deleted, a restore from the cloud copy).
 * @property over the outing is over for good: not counting, and no «Keep going» or «Resume»
 *   can take it back any more. Only then is its route the reader's to share, save or delete:
 *   before, the line may still grow.
 */
@Immutable
data class OutingUiState(
    val session: Session,
    val route: DrawnRoute?,
    val units: UnitPreference,
    val over: Boolean = !session.live,
)

/**
 * An outing's page (PLANNING.md §11 Phase 16). The pages share this one model, as every page of
 * the app does: it is told which outing is open ([open]), and its state runs only while a page
 * collects it. The route is read again only when it changes (a route still being written, one
 * the reader deletes), off the main thread, with the city's map under it if there is one.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class OutingViewModel
@Inject
constructor(
    private val sessions: SessionRepository,
    private val routes: RouteRepository,
    settingsRepository: SettingsRepository,
    live: LiveSession,
) : ViewModel() {
    private val opened = MutableStateFlow<Long?>(null)

    val state: StateFlow<OutingUiState?> = opened
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                val route = routes.observePointCount(id).distinctUntilChanged().mapLatest { read(id) }
                val units = settingsRepository.settings.map { it.units }.distinctUntilChanged()
                // Ended a moment ago, and the service can still take it back: not over yet.
                val reopenable = live.current.map { it?.session?.id == id && it.canKeepGoing }.distinctUntilChanged()
                combine(sessions.observeSession(id), route, units, reopenable) { session, drawn, unit, canReopen ->
                    session?.let { OutingUiState(it, drawn, unit, over = !it.live && !canReopen) }
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

    /**
     * Forgets the open outing's route: the line and its places, never to be written again. The
     * outing and its numbers stay. Only once it is over for good, as its page offers it.
     */
    fun deleteRoute() {
        val session = state.value?.takeIf { it.over }?.session ?: return
        viewModelScope.launch { routes.forget(session.id, session.startedAtMillis) }
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
            RouteShape.of(route.points)?.let { shape ->
                // The city's map is decoded here, the first time: a few thousand points.
                DrawnRoute(shape, RouteGround.cityOf(route.points)?.map, route.altitudes)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
