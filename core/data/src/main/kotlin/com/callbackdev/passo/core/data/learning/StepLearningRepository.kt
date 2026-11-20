package com.callbackdev.passo.core.data.learning

import com.callbackdev.passo.core.data.db.SessionDao
import com.callbackdev.passo.core.data.db.TrackingDao
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.time.TodaySource
import com.callbackdev.passo.core.data.tracking.TrackingRepository
import com.callbackdev.passo.core.domain.calibration.LearnedSteps
import com.callbackdev.passo.core.domain.calibration.OutingSamples
import com.callbackdev.passo.core.domain.calibration.StepLearning
import com.callbackdev.passo.core.domain.calibration.StepLengthLearner
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The step that learns (PLANNING.md §11 Phase 17, docs/adr/0016-location.md decision 12): what
 * the outings with GPS have taught of the reader's step, and the reader's answers to it.
 *
 * Read only while a page shows it (Settings, the calibration page, an outing's page): nothing
 * here runs by itself, and a curve in use never changes behind the reader's back. Every change
 * of the profile goes through [TrackingRepository.changeProfile], which freezes the days already
 * over first, so accepting moves today and the days after, never History.
 */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class StepLearningRepository
@Inject
constructor(
    private val sessions: SessionDao,
    private val days: TrackingDao,
    private val preferences: UserPreferencesDataSource,
    private val tracking: TrackingRepository,
    private val todaySource: TodaySource,
) {
    /** What the samples teach today, as the samples, the profile or the curve put away change. */
    val learning: Flow<StepLearning> = combine(
        sessions.observeStepLengthSamples().map { rows ->
            rows.groupBy { it.sessionId }.map { (id, samples) ->
                OutingSamples(
                    id,
                    samples.first().startedAtMillis,
                    samples.map {
                        StepLengthSample(it.cadence, it.steps, it.meters)
                    },
                )
            }
        },
        preferences.data.map { it.profile }.distinctUntilChanged(),
        preferences.stepPutAway.distinctUntilChanged(),
    ) { outings, profile, putAway -> Triple(outings, profile, putAway) }
        .mapLatest { (outings, profile, putAway) ->
            if (outings.isEmpty()) return@mapLatest StepLearning.NOTHING
            val today = todaySource.epochDay()
            // The minutes of the usual pace are read as the page opens, not followed: a page does
            // not change its proposal under the reader's eyes because a minute was written.
            val minutes = days.stepCounts(today - StepLengthLearner.USUAL_DAYS + 1, today)
                .associate { it.steps to it.minutes }
            withContext(Dispatchers.Default) { StepLengthLearner.learn(outings, minutes, profile, putAway) }
        }
        .distinctUntilChanged()

    /**
     * The step one outing measured, its samples' metres over their steps: «Your step at this
     * pace» on its page. Null for an outing with no sample (no GPS, no clean stretch).
     */
    fun stepOf(sessionId: Long): Flow<Double?> = sessions.observeStepLengthSamples()
        .map { rows ->
            val own = rows.filter { it.sessionId == sessionId && it.steps > 0 && it.meters > 0 }
            val steps = own.sumOf { it.steps }
            if (steps == 0) null else own.sumOf { it.meters } / steps
        }
        .distinctUntilChanged()

    /** «Use it»: [curve] in use from today; the past keeps its own. */
    suspend fun use(curve: LearnedStep) {
        tracking.changeProfile { LearnedSteps.accept(it, curve) }
        // The curve in use is now what a new proposal is measured from.
        preferences.setStepPutAway(null)
    }

    /** «Not now»: [curve] put away, until the outings move it by another 2%. */
    suspend fun notNow(curve: LearnedStep) = preferences.setStepPutAway(curve)

    /** «Go back»: the step in use before, and the curve gone back from put away. */
    suspend fun goBack() {
        var gone: LearnedStep? = null
        tracking.changeProfile { profile ->
            gone = profile.learnedStep
            LearnedSteps.goBack(profile)
        }
        gone?.let { preferences.setStepPutAway(it) }
    }

    /**
     * Another walking length chosen (typed, the height's estimate, or the wizard's): the last one
     * chosen is in use, and the curve that counts today is put away as «Not now» does, or the card
     * would come back at once.
     */
    suspend fun chooseLength(transform: (Profile) -> Profile): Profile {
        val before = preferences.current().profile
        val after = tracking.changeProfile(transform)
        if (LearnedSteps.walkingChanged(before, after)) {
            learning.first().offer?.curve?.let { preferences.setStepPutAway(it) }
        }
        return after
    }
}
