package com.callbackdev.passo.feature.insights

import android.content.res.Resources
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.byDayWithLive
import com.callbackdev.passo.core.domain.insights.Insights
import com.callbackdev.passo.core.domain.settings.firstDayOfWeek
import com.callbackdev.passo.core.model.UnitPreference
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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Everything Insights draws.
 *
 * @property lastDays the last seven days, oldest first, for the streak's row of marks.
 * @property goalSteps today's goal: what today needs to join a streak.
 */
@Immutable
data class InsightsUiState(
    val today: LocalDate,
    val insights: Insights,
    val lastDays: List<DayMark>,
    val goalSteps: Int,
    val units: UnitPreference,
    val firstDayOfWeek: DayOfWeek,
)

/** One day of the streak's row: [counted] false before counting began. */
@Immutable
data class DayMark(val date: LocalDate, val counted: Boolean, val met: Boolean, val today: Boolean)

/**
 * Insights (PLANNING.md §11 Phase 5): computed from every recorded day each time one changes,
 * only while the screen is collected. A few thousand rows at most, on a background thread.
 */
@HiltViewModel
class InsightsViewModel
@Inject
constructor(
    tracking: TrackingRepository,
    settingsRepository: SettingsRepository,
    liveSteps: LiveSteps,
) : ViewModel() {
    private val day: Flow<LocalDate> = flow {
        while (true) {
            emit(LocalDate.now(ZoneId.systemDefault()))
            delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
        }
    }.distinctUntilChanged()

    val state: StateFlow<InsightsUiState?> = combine(
        day,
        tracking.observeAllSummaries(),
        liveSteps.today,
        settingsRepository.settings,
    ) { today, summaries, live, settings ->
        val days = summaries.byDayWithLive(
            live?.takeIf {
                it.localEpochDay == today.toEpochDay()
            },
            settings.dailyGoalSteps,
        )
        val weekStart = firstDayOfWeek(settings.firstDayOfWeek, Resources.getSystem().configuration.locales[0])
        val insights = Insights.of(days, today, weekStart)
        InsightsUiState(
            today = today,
            insights = insights,
            lastDays = (LAST_DAYS - 1 downTo 0).map { back ->
                val date = today.minusDays(back.toLong())
                val first = insights.firstDay
                DayMark(
                    date = date,
                    counted = first != null && !date.isBefore(first),
                    met = days[date.toEpochDay()]?.goalReached == true,
                    today = back == 0,
                )
            },
            goalSteps = days[today.toEpochDay()]?.goalSteps ?: settings.dailyGoalSteps,
            units = settings.units,
            firstDayOfWeek = weekStart,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val LAST_DAYS = 7
        const val MILLIS_PER_MINUTE = 60_000L
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
