package com.callbackdev.passo.feature.history

import androidx.compose.runtime.Immutable
import com.callbackdev.passo.core.domain.today.HourlySteps
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.UnitPreference
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * What every page of History is drawn from, read once per change.
 *
 * @property days every recorded day by epoch day, today's with the steps the service has not
 *   written yet, so History and Today never show two counts for one day.
 * @property firstDay the first recorded day: History pages back to it and no further.
 * @property walkDetection the reader's switch: off, no walk appears anywhere.
 */
@Immutable
data class HistoryUiState(
    val today: LocalDate,
    val nowMinute: Int,
    val days: Map<Long, DailySummary>,
    val firstDay: LocalDate?,
    val goalSteps: Int,
    val firstDayOfWeek: DayOfWeek,
    val units: UnitPreference,
    val walkDetection: Boolean,
    val minWalkMinutes: Int,
)

/**
 * One day in detail (PLANNING.md §11 Phase 5).
 *
 * @property distanceMeters and the other estimates are the day's as it froze them (PLANNING.md
 *   §5: past days are frozen); today's are computed with the current profile, as Today's are.
 * @property averageCadence over the day's active minutes; it does not depend on the profile.
 * @property walks null when walk detection is off: then no walk is drawn and none is listed.
 */
@Immutable
data class DayDetail(
    val date: LocalDate,
    val steps: Int,
    val goalSteps: Int,
    val distanceMeters: Double,
    val activeKcal: Double,
    val activeMinutes: Int,
    val briskMinutes: Int,
    val averageCadence: Int?,
    val hourly: HourlySteps,
    val walks: List<Walk>?,
    val isToday: Boolean,
    val currentHour: Int?,
) {
    val goalReached: Boolean get() = steps >= goalSteps
}
