package com.callbackdev.passo.feature.settings

import com.callbackdev.passo.core.domain.calibration.LearnedSteps
import com.callbackdev.passo.core.domain.calibration.StepLearning
import com.callbackdev.passo.core.domain.calibration.StepLengthLearner
import com.callbackdev.passo.core.domain.calibration.StepOffer
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.metrics.usualMeters
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepPoint

/** A step learnt on six outings with GPS, for the screens that show it (PLANNING.md §11 Phase 17). */
object LearningFixtures {
    val curve = LearnedStep(
        points = listOf(StepPoint(92.4, 0.76), StepPoint(108.7, 0.79), StepPoint(121.3, 0.81)),
        usualCadence = 104,
        learntMeters = 14_230.0,
        outings = 6,
        throughMillis = 1_790_000_000_000,
    )

    /** Four weeks of a reader walking at 104 steps a minute. */
    private val minutes = mapOf(104 to 600, 60 to 300)

    /** The curve as it is proposed to a reader with [profile]. */
    fun proposed(profile: Profile): StepLearning {
        val inUse = StepLengths.of(profile)
        val accepted = StepLengths.of(LearnedSteps.accept(profile, curve))
        val share = StepLengthLearner.distance(minutes, accepted) / StepLengthLearner.distance(minutes, inUse) - 1
        return StepLearning(
            offer = StepOffer(curve, share, requireNotNull(curve.usualMeters), inUse.walkingMeters),
            proposed = true,
            readyOutingId = 7,
        )
    }
}
