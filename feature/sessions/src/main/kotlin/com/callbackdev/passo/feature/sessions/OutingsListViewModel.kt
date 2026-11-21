package com.callbackdev.passo.feature.sessions

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.data.routes.RouteThumbnails
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.time.TodaySource
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.sessions.YourOutings
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.UnitPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/**
 * «Your outings».
 *
 * @property today the reader's date when the list was read: what the sentence's year and the
 *   months' headers are said against.
 */
@Immutable
data class OutingsListUiState(val outings: YourOutings, val units: UnitPreference, val today: LocalDate)

/**
 * Where a list's row finds its route's small picture (PLANNING.md §11 Phase 23, step 23.2): read
 * only when the row is composed, which a lazy list does only for the rows on screen, and drawn at
 * once from what was kept when the row comes back.
 */
interface RoutePictures {
    /** The picture kept for [key], for the row's first frame; null when it must be read. */
    fun peek(key: RouteKey): RouteShape?

    /** The picture of the route [key] names; null when there is none to draw. */
    suspend fun of(key: RouteKey): RouteShape?

    companion object {
        /** No picture at all: a test's page, or a preview's. */
        val None = object : RoutePictures {
            override fun peek(key: RouteKey): RouteShape? = null

            override suspend fun of(key: RouteKey): RouteShape? = null
        }
    }
}

/**
 * «Your outings» (PLANNING.md §11 Phase 23, step 23.2): the outings over, by month, and which
 * of them hold a route. One read of the outings' own rows and one of the routes' keys, grouped off
 * the main thread, and only while the page is collected; the routes themselves are read by the rows
 * on screen ([pictures]). The filter is the page's own, so every page open on it keeps its own.
 */
@HiltViewModel
class OutingsListViewModel
@Inject
constructor(
    sessions: SessionRepository,
    routes: RouteRepository,
    settings: SettingsRepository,
    thumbnails: RouteThumbnails,
    todaySource: TodaySource,
) : ViewModel() {
    val state: StateFlow<OutingsListUiState?> = combine(
        sessions.finished,
        routes.keys,
        settings.settings.map { it.units }.distinctUntilChanged(),
    ) { finished, keys, units ->
        val today = LocalDate.ofEpochDay(todaySource.epochDay())
        OutingsListUiState(YourOutings.of(finished, keys, today), units, today)
    }
        .flowOn(Dispatchers.Default)
        // An outing under way writes its row at each batch: the list, which leaves it out, is unchanged.
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    val pictures: RoutePictures = object : RoutePictures {
        override fun peek(key: RouteKey): RouteShape? = thumbnails.peek(key)

        override suspend fun of(key: RouteKey): RouteShape? = thumbnails.of(key)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
