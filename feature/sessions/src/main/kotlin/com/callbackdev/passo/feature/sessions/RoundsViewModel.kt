package com.callbackdev.passo.feature.sessions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.rounds.RoundRepository
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.routes.RouteThumbnails
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.time.TodaySource
import com.callbackdev.passo.core.domain.rounds.KeptRound
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.model.RouteKey
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
import java.time.LocalDate
import javax.inject.Inject

/** «Your rounds»: the reader's usual rounds, the latest walked first. */
@Immutable
data class RoundsUiState(val rounds: List<KeptRound>, val units: UnitPreference, val today: LocalDate)

/**
 * «Your rounds» (PLANNING.md §11 Phase 20): the rounds as numbers, grouped off the main thread and
 * only while the page is collected; their small maps read by the rows as the page draws them
 * ([pictures]), kept thinned as «Your outings» keeps its routes. The rounds are one group on the page,
 * a few rows: drawn together, not as they scroll on screen.
 */
@HiltViewModel
class RoundsViewModel
@Inject
constructor(
    rounds: RoundRepository,
    settings: SettingsRepository,
    thumbnails: RouteThumbnails,
    todaySource: TodaySource,
) : ViewModel() {
    val state: StateFlow<RoundsUiState?> = combine(
        rounds.kept,
        settings.settings.map { it.units }.distinctUntilChanged(),
    ) { kept, units -> RoundsUiState(kept, units, LocalDate.ofEpochDay(todaySource.epochDay())) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val pictures: RoutePictures = object : RoutePictures {
        override fun peek(key: RouteKey): RouteShape? = thumbnails.peek(key)

        override suspend fun of(key: RouteKey): RouteShape? = thumbnails.of(key)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

/**
 * A round's page.
 *
 * @property round null once it is no longer one of the reader's usual rounds (forgotten, or left with
 *   fewer than three walks): the page has nothing true to show, and goes back.
 * @property map its latest walk's whole route, for the map with its gestures; null with no route left.
 */
@Immutable
data class RoundUiState(val round: KeptRound?, val map: RouteShape?, val units: UnitPreference, val today: LocalDate)

/**
 * A round's page (PLANNING.md §11 Phase 20): told which round is open ([open]), as the outing's page
 * is; its map is read again only when the walk it is drawn from changes, off the main thread.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RoundViewModel
@Inject
constructor(
    private val rounds: RoundRepository,
    private val routes: RouteRepository,
    settings: SettingsRepository,
    todaySource: TodaySource,
) : ViewModel() {
    private val opened = MutableStateFlow<Long?>(null)

    val state: StateFlow<RoundUiState?> = opened
        .flatMapLatest { id ->
            if (id == null) {
                flowOf(null)
            } else {
                val round = rounds.kept.map { kept -> kept.firstOrNull { it.id == id } }.distinctUntilChanged()
                val map = round.map { it?.route }.distinctUntilChanged().mapLatest { key -> key?.let { read(it) } }
                combine(round, map, settings.settings.map { it.units }.distinctUntilChanged()) { kept, drawn, units ->
                    RoundUiState(kept, drawn, units, LocalDate.ofEpochDay(todaySource.epochDay()))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun open(roundId: Long) {
        opened.value = roundId
    }

    fun close() {
        opened.value = null
    }

    /** The reader's own name for the open round; empty, it is named by its length again. */
    fun rename(name: String) {
        val id = opened.value ?: return
        viewModelScope.launch { rounds.rename(id, name) }
    }

    /** «Forget this round»: the round and its pairs go; its outings stay, as they are. */
    fun forget() {
        val id = opened.value ?: return
        viewModelScope.launch { rounds.forget(id) }
    }

    private suspend fun read(key: RouteKey): RouteShape? {
        val route = routes.route(key.sessionId, key.sessionStartedAtMillis) ?: return null
        return withContext(Dispatchers.Default) { RouteShape.of(route.points) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
