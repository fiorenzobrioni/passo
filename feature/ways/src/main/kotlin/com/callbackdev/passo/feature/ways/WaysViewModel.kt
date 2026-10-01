package com.callbackdev.passo.feature.ways

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.ways.WayRepository
import com.callbackdev.passo.core.data.ways.distances
import com.callbackdev.passo.core.domain.ways.WayForecast
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.WayStartChoice
import com.callbackdev.passo.core.domain.ways.WayStarts
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
 * @property past the ways finished or left, the latest first.
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
) {
    fun journey(id: Long): JourneyView? = active?.takeIf { it.journey.id == id } ?: past.firstOrNull {
        it.journey.id == id
    }
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
    private val ways: WayRepository,
    tracking: TrackingRepository,
    settings: SettingsRepository,
) : ViewModel() {
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
        settings.settings,
    ) { today, journeys, summaries, current ->
        val distances = summaries.distances()
        val views = journeys.map { journey ->
            val progress = WayProgress.of(Ways.of(journey.way), journey, distances, today.toEpochDay())
            val forecast = if (journey.state == WayJourneyState.ACTIVE && !progress.finished) {
                WayForecast.of(distances, today, progress.metersLeft)
            } else {
                null
            }
            JourneyView(journey, progress, forecast)
        }
        WaysUiState(
            today = today,
            units = current.units,
            firstCounted = summaries.minOfOrNull { it.localEpochDay }?.let(LocalDate::ofEpochDay),
            distances = distances,
            pace = WayForecast.pace(distances, today),
            active = views.firstOrNull { it.journey.state == WayJourneyState.ACTIVE },
            past = views.filter { it.journey.state != WayJourneyState.ACTIVE },
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

    /** Puts the way [journeyId] down, today, where the reader stands. */
    fun leave(journeyId: Long) {
        viewModelScope.launch { ways.leave(journeyId, LocalDate.now(ZoneId.systemDefault())) }
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
