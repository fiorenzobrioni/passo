package com.callbackdev.passo.feature.history

import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.byDayWithLive
import com.callbackdev.passo.core.data.tracking.withPending
import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.settings.firstDayOfWeek
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.HourlySteps
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.walks.WalkDetector
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

/**
 * History (PLANNING.md §11 Phase 5). Everything here runs only while the screen is collected
 * (`WhileSubscribed`), which is only while it is visible: no work with the screen off (§9).
 * The recorded days are read whole, one row a day, and each page computes its own numbers from
 * them; walks are found on read from the day's minutes (§6.1), only for the day on screen.
 */
@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    private val tracking: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val liveSteps: LiveSteps,
) : ViewModel() {
    private val clock: Flow<Pair<LocalDate, Int>> = flow {
        while (true) {
            val zone = ZoneId.systemDefault()
            val time = LocalTime.now(zone)
            emit(LocalDate.now(zone) to time.hour * MINUTES_PER_HOUR + time.minute)
            delay(MILLIS_PER_MINUTE - System.currentTimeMillis() % MILLIS_PER_MINUTE)
        }
    }.distinctUntilChanged().shareIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), replay = 1)

    val state: StateFlow<HistoryUiState?> = combine(
        clock,
        tracking.observeAllSummaries(),
        liveSteps.today,
        settingsRepository.settings,
    ) { (today, minute), summaries, live, settings ->
        val days = summaries.byDayWithLive(
            live?.takeIf {
                it.localEpochDay == today.toEpochDay()
            },
            settings.dailyGoalSteps,
        )
        HistoryUiState(
            today = today,
            nowMinute = minute,
            days = days,
            firstDay = days.keys.minOrNull()?.let(LocalDate::ofEpochDay),
            goalSteps = settings.dailyGoalSteps,
            firstDayOfWeek = firstDayOfWeek(settings.firstDayOfWeek, Resources.getSystem().configuration.locales[0]),
            units = settings.units,
            walkDetection = settings.walkDetection,
            minWalkMinutes = settings.minWalkMinutes,
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** One day in detail, live while it is today; its walks found only if the reader wants them. */
    fun day(date: LocalDate): Flow<DayDetail> {
        val epochDay = date.toEpochDay()
        return combine(
            tracking.observeMinutesOn(epochDay),
            tracking.observeSummary(epochDay),
            liveSteps.today,
            settingsRepository.settings,
            settingsRepository.profile,
        ) { stored, summary, live, settings, profile ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val isToday = date == today
            val pending = live?.takeIf { isToday && it.localEpochDay == epochDay }?.pending.orEmpty()
            val minutes = stored.withPending(pending).map {
                DayMinute(minuteOfDay(it.epochMinute, it.localEpochDay, zone), it.steps)
            }
            val computed = MetricsCalculator.day(minutes.map { it.steps }, profile)
            // A past day keeps the estimates it froze with (PLANNING.md §5); today is still open.
            val frozen = summary?.takeIf { !isToday }
            DayDetail(
                date = date,
                steps = frozen?.steps ?: computed.steps,
                goalSteps = summary?.goalSteps ?: settings.dailyGoalSteps,
                distanceMeters = frozen?.distanceMeters ?: computed.distanceMeters,
                activeKcal = frozen?.activeKcal ?: computed.activeKcal,
                activeMinutes = frozen?.activeMinutes ?: computed.activeMinutes,
                briskMinutes = frozen?.briskMinutes ?: computed.briskMinutes,
                averageCadence = computed.averageCadence,
                hourly = HourlySteps.of(minutes),
                walks = if (settings.walkDetection) {
                    WalkDetector.detect(
                        minutes,
                        profile,
                        settings.minWalkMinutes,
                    )
                } else {
                    null
                },
                isToday = isToday,
                currentHour = if (isToday) LocalTime.now(zone).hour else null,
            )
        }.flowOn(Dispatchers.Default)
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val MINUTES_PER_HOUR = 60
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
