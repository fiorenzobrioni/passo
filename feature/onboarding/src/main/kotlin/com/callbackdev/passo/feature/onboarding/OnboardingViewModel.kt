package com.callbackdev.passo.feature.onboarding

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callbackdev.passo.core.data.settings.SettingsRepository
import com.callbackdev.passo.core.domain.onboarding.OemTips
import com.callbackdev.passo.core.domain.settings.ProfileInputs
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.core.tracking.StepTracking
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The pages of the first run, in order; [BATTERY] only on the phones that need it. */
enum class OnboardingStep {
    WELCOME,
    PROFILE,
    GOAL,
    PERMISSIONS,
    BATTERY,
}

/**
 * The first run's draft. Nothing is stored until the end, so leaving halfway leaves nothing
 * half-set; the profile is stored only if the reader went through its page rather than skipping.
 *
 * @property batteryTip whether this phone's maker stops background apps, so the tip is shown.
 */
data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val heightMeters: Double = ProfileInputs.DEFAULT_HEIGHT_M,
    val weightKg: Double = ProfileInputs.DEFAULT_WEIGHT_KG,
    val sex: Sex? = null,
    val profileSkipped: Boolean = false,
    val goalSteps: Int = UserSettings.DEFAULT_DAILY_GOAL_STEPS,
    val units: UnitPreference = UnitPreference.SYSTEM,
    val activityGranted: Boolean = false,
    val permissionAsked: Boolean = false,
    val batteryTip: Boolean = false,
    val manufacturer: String = "",
) {
    val steps: List<OnboardingStep>
        get() = OnboardingStep.entries.filter { it != OnboardingStep.BATTERY || batteryTip }
}

/**
 * The first run (PLANNING.md §11 Phase 3): welcome, profile (skippable), goal, permissions and,
 * on the phones whose battery manager stops background apps, a tip. Built to take under a
 * minute from install to counting: every page has one obvious button.
 */
@HiltViewModel
class OnboardingViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val manufacturer = Build.MANUFACTURER.orEmpty()
    private val mutableState = MutableStateFlow(
        OnboardingState(
            activityGranted = StepTracking.hasActivityRecognition(context),
            batteryTip = OemTips.needsTip(manufacturer),
            manufacturer = manufacturer.replaceFirstChar { it.uppercase() },
        ),
    )
    val state: StateFlow<OnboardingState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settings.settings.first()
            mutableState.update { it.copy(units = current.units, goalSteps = current.dailyGoalSteps) }
        }
    }

    fun next() = move(+1)

    fun back() = move(-1)

    fun skipProfile() {
        mutableState.update { it.copy(profileSkipped = true) }
        move(+1)
    }

    fun setHeight(meters: Double) = mutableState.update { it.copy(heightMeters = meters, profileSkipped = false) }

    fun setWeight(kg: Double) = mutableState.update { it.copy(weightKg = kg, profileSkipped = false) }

    fun setSex(sex: Sex?) = mutableState.update { it.copy(sex = sex, profileSkipped = false) }

    fun setGoal(steps: Int) = mutableState.update { it.copy(goalSteps = steps) }

    fun refreshPermission() = mutableState.update {
        it.copy(activityGranted = StepTracking.hasActivityRecognition(context))
    }

    /** The system dialog was answered: a refusal is then said on the page, with the way on. */
    fun permissionAnswered() = mutableState.update {
        it.copy(activityGranted = StepTracking.hasActivityRecognition(context), permissionAsked = true)
    }

    /** Stores what was chosen and marks the first run done; the shell then shows Today. */
    fun finish() {
        val draft = mutableState.value
        viewModelScope.launch {
            if (!draft.profileSkipped) {
                settings.updateProfile {
                    it.copy(heightMeters = draft.heightMeters, weightKg = draft.weightKg, sex = draft.sex)
                }
            }
            settings.updateSettings { it.copy(dailyGoalSteps = draft.goalSteps, onboardingCompleted = true) }
            StepTracking.start(context)
        }
    }

    private fun move(delta: Int) {
        mutableState.update { current ->
            val order = current.steps
            val index = (order.indexOf(current.step) + delta).coerceIn(0, order.lastIndex)
            current.copy(step = order[index])
        }
    }
}
