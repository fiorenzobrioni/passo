package com.callbackdev.passo.feature.year

import android.content.res.Resources
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.sessions.SessionRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.data.tracking.LiveSteps
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.data.tracking.byDayWithLive
import com.callbackdev.passo.core.data.ways.WayRepository
import com.callbackdev.passo.core.domain.settings.firstDayOfWeek
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.domain.walks.WalkDetector
import com.callbackdev.passo.core.domain.year.YearInReview
import com.callbackdev.passo.core.domain.year.YearPage
import com.callbackdev.passo.core.model.UnitPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * What the year's pages draw.
 *
 * @property review null when the year is not offered (fewer than 30 days counted).
 */
@Immutable
data class YearUiState(
    val year: Int,
    val review: YearInReview?,
    val units: UnitPreference,
    val firstDayOfWeek: DayOfWeek,
    val today: LocalDate,
)

/**
 * Your year on foot (PLANNING.md §11 Phase 12): the year asked for, read once when its pages
 * open, from the days, a year of minutes (for the hour), the outings and the ways. Nothing runs
 * once the pages close (`WhileSubscribed`); nothing is kept but what is on screen.
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class YearViewModel
@Inject
constructor(
    private val tracking: TrackingRepository,
    private val settingsRepository: SettingsRepository,
    private val liveSteps: LiveSteps,
    private val sessions: SessionRepository,
    private val ways: WayRepository,
) : ViewModel() {
    private val year = MutableStateFlow<Int?>(null)

    val state: StateFlow<YearUiState?> = year.filterNotNull()
        .flatMapLatest { asked -> flow { emit(read(asked)) }.flowOn(Dispatchers.Default) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    /** The pages of [asked]: read when they open, again only for another year. */
    fun open(asked: Int) {
        year.value = asked
    }

    private suspend fun read(asked: Int): YearUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val settings = settingsRepository.settings.first()
        val profile = settingsRepository.profile.first()
        val days = tracking.observeAllSummaries().first()
            .byDayWithLive(
                liveSteps.today.value?.takeIf {
                    it.localEpochDay == today.toEpochDay()
                },
                settings.dailyGoalSteps,
            )
        val from = LocalDate.of(asked, 1, 1).toEpochDay()
        val to = LocalDate.of(asked, 12, 31).toEpochDay()
        val hours = tracking.stepsByHour(from, to, zone)
        val outings = sessions.finishedBetween(from, to)
        val journeys = ways.journeys.first()
        fun review(walks: Map<LocalDate, List<Walk>>) =
            YearInReview.of(asked, today, days, hours, outings, journeys) { walks[it].orEmpty() }
        var review = review(emptyMap())
        // The best day's walk, when no outing made it: found in that day's minutes, as History does.
        val best = review?.pages?.filterIsInstance<YearPage.BestDay>()?.firstOrNull()
        if (best != null && best.outing == null && settings.walkDetection) {
            val minutes = tracking.minutesOn(listOf(best.date.toEpochDay()))[best.date.toEpochDay()].orEmpty()
                .map { DayMinute(minuteOfDay(it.epochMinute, it.localEpochDay, zone), it.steps) }
            val walks = WalkDetector.detect(minutes, profile, settings.minWalkMinutes)
            if (walks.isNotEmpty()) review = review(mapOf(best.date to walks))
        }
        return YearUiState(
            year = asked,
            review = review,
            units = settings.units,
            firstDayOfWeek = firstDayOfWeek(settings.firstDayOfWeek, Resources.getSystem().configuration.locales[0]),
            today = today,
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
