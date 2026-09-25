package com.callbackdev.passo.core.data.settings

import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The profile and the settings, as the screens read and change them. Changes go through
 * [TrackingRepository], which knows what a new profile or goal may and may not touch in the
 * recorded days.
 */
@Singleton
class SettingsRepository
@Inject
constructor(
    preferences: UserPreferencesDataSource,
    private val tracking: TrackingRepository,
) {
    val profile: Flow<Profile> = preferences.data.map { it.profile }.distinctUntilChanged()

    val settings: Flow<UserSettings> = preferences.data.map { it.settings }.distinctUntilChanged()

    /** Changes the profile: today's estimates follow, past days keep theirs. */
    suspend fun updateProfile(transform: (Profile) -> Profile): Profile = tracking.changeProfile(transform)

    /** Changes the settings: a new goal is today's goal, past days keep theirs. */
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings): UserSettings =
        tracking.changeSettings(transform)

    /** Recomputes every recorded day's estimates with the current profile, on the reader's request. */
    suspend fun applyProfileToPastDays() = tracking.applyProfileToPastDays()
}
