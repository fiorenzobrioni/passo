package com.callbackdev.passo.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.tracking.TrackingControl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** What Settings shows: the reader's settings and profile, together. */
data class SettingsUiState(val settings: UserSettings, val profile: Profile, val version: String)

/**
 * Settings (PLANNING.md §11 Phase 3). A profile or goal change goes through the repository that
 * freezes the days already over first, so only today follows it; "Apply to past days" is the
 * one way to reach them, and asks first.
 */
@HiltViewModel
class SettingsViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val repository: SettingsRepository,
    private val control: TrackingControl,
) : ViewModel() {
    private val version: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    val state: StateFlow<SettingsUiState?> = combine(repository.settings, repository.profile) { settings, profile ->
        SettingsUiState(settings, profile, version)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun updateProfile(transform: (Profile) -> Profile) {
        viewModelScope.launch { repository.updateProfile(transform) }
    }

    fun applyProfileToPastDays() {
        viewModelScope.launch { repository.applyProfileToPastDays() }
    }

    /**
     * Pauses or resumes counting. A pause stops the service, which writes its buffer as it goes;
     * a resume forgets the baseline first, so the steps of the pause are not counted in one go
     * by the first sample after it.
     */
    fun setTracking(enabled: Boolean) {
        viewModelScope.launch { if (enabled) control.resume() else control.pause() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
