package com.callbackdev.passo.feature.today

import androidx.compose.runtime.Immutable
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.model.UnitPreference
import java.time.LocalDate

/** Whether the day on screen is being counted, and if not, why (PLANNING.md §11 Phase 3). */
enum class TrackingStatus {
    COUNTING,

    /** "Physical activity" is not granted: nothing is counted until it is. */
    PERMISSION_NEEDED,

    /** The reader paused tracking in Settings. */
    PAUSED,
}

/**
 * Everything Today draws, computed once per change and immutable.
 *
 * @property walkingStepLength the step length distance is estimated with, in metres, shown under
 *   the distance so the estimate states what it rests on.
 * @property firstDay today is the first day counted: steps from before the install are not in it.
 * @property celebrate the goal is met and the ring has not bloomed for it yet today.
 */
@Immutable
data class TodayUiState(
    val date: LocalDate,
    val nowMinute: Double,
    val overview: TodayOverview,
    val status: TrackingStatus,
    val units: UnitPreference,
    val walkingStepLength: Double,
    val firstDay: Boolean,
    val celebrate: Boolean,
)
