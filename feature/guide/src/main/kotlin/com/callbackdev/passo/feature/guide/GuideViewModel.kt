package com.callbackdev.passo.feature.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.model.UnitPreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import javax.inject.Inject

/**
 * What the guide's examples are drawn in: the reader's units and first day of the week, so an
 * example reads like the app they have, never like someone else's.
 *
 * @property firstDayOfWeek null follows the locale.
 */
data class GuideUiState(val units: UnitPreference, val firstDayOfWeek: DayOfWeek?)

@HiltViewModel
class GuideViewModel
@Inject
constructor(settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<GuideUiState?> = settings.settings
        .map { GuideUiState(it.units, it.firstDayOfWeek) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
