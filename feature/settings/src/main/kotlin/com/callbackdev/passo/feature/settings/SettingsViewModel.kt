package com.callbackdev.passo.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.learning.StepLearningRepository
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.domain.calibration.LearnedSteps
import com.callbackdev.passo.core.domain.calibration.StepLearning
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.tracking.TrackingControl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * What Settings shows: the reader's settings and profile, together.
 *
 * @property learning what the outings with GPS have taught of the step (Phase 17): its card shows
 *   when it is proposed.
 * @property stepUsed «Use it» was just touched: the card gives way to what it changed, with the
 *   way to the past days.
 */
data class SettingsUiState(
    val settings: UserSettings,
    val profile: Profile,
    val version: String,
    val learning: StepLearning = StepLearning.NOTHING,
    val stepUsed: Boolean = false,
)

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
    private val learning: StepLearningRepository,
) : ViewModel() {
    private val stepUsed = MutableStateFlow(false)

    private val version: String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty()

    val state: StateFlow<SettingsUiState?> = combine(
        repository.settings,
        repository.profile,
        learning.learning,
        stepUsed,
    ) { settings, profile, learnt, used ->
        SettingsUiState(settings, profile, version, learnt, used)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.updateSettings(transform) }
    }

    fun updateProfile(transform: (Profile) -> Profile) {
        viewModelScope.launch { repository.updateProfile(transform) }
    }

    fun applyProfileToPastDays() {
        stepUsed.value = false
        viewModelScope.launch { repository.applyProfileToPastDays() }
    }

    /**
     * The walking step chosen in its dialog: [own], the height's estimate (null), or the learnt step
     * kept. Another length chosen puts a curve that counts away, as «Not now» does.
     */
    fun chooseWalking(own: Double?, keepLearned: Boolean) {
        viewModelScope.launch { learning.chooseLength { LearnedSteps.chooseWalking(it, own, keepLearned) } }
    }

    /** The running step chosen in its dialog: [own], the factor (null), or the learnt running part. */
    fun chooseRunning(own: Double?, learnedRunning: Boolean) {
        viewModelScope.launch { repository.updateProfile { LearnedSteps.chooseRunning(it, own, learnedRunning) } }
    }

    /** «Use it»: today and the days after follow the learnt step; past days keep theirs. */
    fun useStep(curve: LearnedStep) {
        stepUsed.value = true
        viewModelScope.launch { learning.use(curve) }
    }

    fun notNow(curve: LearnedStep) {
        viewModelScope.launch { learning.notNow(curve) }
    }

    /** Back to the step in use before the learnt one, named as it was. */
    fun goBackStep() {
        stepUsed.value = false
        viewModelScope.launch { learning.goBack() }
    }

    /** The note after «Use it» put away. */
    fun dismissStepUsed() {
        stepUsed.value = false
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
