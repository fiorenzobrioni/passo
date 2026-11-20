package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.RUNNING_CADENCE
import com.callbackdev.passo.core.domain.metrics.sanitized
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode

/**
 * What the reader's choices about their step do to the profile (PLANNING.md §11 Phase 17). The
 * last length chosen is the one in use, gait by gait; the reader's own lengths stay beside a
 * learnt step, so accepting one loses no length measured or typed, and the way back finds it.
 */
object LearnedSteps {
    /**
     * [profile] with [curve] in use. It remembers the step it replaced, for the way back (a curve
     * replacing a curve keeps the first one's), and uses its running part when it has one.
     */
    fun accept(profile: Profile, curve: LearnedStep): Profile {
        val replaced = if (profile.stepLengthMode == StepLengthMode.LEARNED) {
            profile.learnedStep?.replaced ?: StepLengthMode.AUTO
        } else {
            profile.stepLengthMode
        }
        return profile.copy(
            stepLengthMode = StepLengthMode.LEARNED,
            learnedStep = curve.copy(replaced = replaced, running = curve.points.any { it.cadence >= RUNNING_CADENCE }),
        ).sanitized()
    }

    /** [profile] back on the step in use before the learnt one: measured, typed or the height's. */
    fun goBack(profile: Profile): Profile {
        if (profile.stepLengthMode != StepLengthMode.LEARNED) return profile
        val back = profile.learnedStep?.replaced ?: StepLengthMode.AUTO
        return profile.copy(
            stepLengthMode = back,
            walkingStepLengthMeters = profile.walkingStepLengthMeters.takeIf { back != StepLengthMode.AUTO },
            learnedStep = null,
        ).sanitized()
    }

    /** Where the way back from a learnt step leads; null when no learnt step is in use. */
    fun wayBack(profile: Profile): StepLengthMode? =
        profile.learnedStep?.replaced?.takeIf { profile.stepLengthMode == StepLengthMode.LEARNED }

    /**
     * The walking step chosen in Settings' dialog: [own] typed or kept, or null for the height's
     * estimate. A length unchanged is kept as it was (a measured step stays measured, and the
     * learnt step stays in use when it is the one kept); a measured or typed length chosen again
     * over a learnt step is the way back to it.
     */
    fun chooseWalking(profile: Profile, own: Double?, keepLearned: Boolean = false): Profile {
        val learned = profile.stepLengthMode == StepLengthMode.LEARNED
        return when {
            keepLearned && learned -> profile

            own == null -> profile.copy(
                stepLengthMode = StepLengthMode.AUTO,
                walkingStepLengthMeters = null,
                learnedStep = null,
            )

            own == profile.walkingStepLengthMeters && profile.stepLengthMode in OWN -> profile

            learned && own == profile.walkingStepLengthMeters && wayBack(
                profile,
            ) != StepLengthMode.AUTO -> goBack(profile)

            else -> profile.copy(
                stepLengthMode = StepLengthMode.MANUAL,
                walkingStepLengthMeters = own,
                learnedStep = null,
            )
        }.sanitized()
    }

    /**
     * The running step chosen in Settings' dialog: [own], or null for the walking step's factor,
     * or the learnt running part when [learnedRunning]. A running length chosen after accepting
     * wins over the curve's running part; walking keeps the curve.
     */
    fun chooseRunning(profile: Profile, own: Double?, learnedRunning: Boolean = false): Profile {
        val learned = profile.learnedStep?.takeIf { profile.stepLengthMode == StepLengthMode.LEARNED }
        return if (learnedRunning && learned != null) {
            profile.copy(learnedStep = learned.copy(running = true))
        } else {
            profile.copy(runningStepLengthMeters = own, learnedStep = learned?.copy(running = false))
        }.sanitized()
    }

    /** The modes whose walking length is the reader's own: typed or measured. */
    private val OWN = setOf(StepLengthMode.MANUAL, StepLengthMode.CALIBRATED)

    /** Whether [after] walks with another step than [before]: a choice that puts a curve away. */
    fun walkingChanged(before: Profile, after: Profile): Boolean = before.stepLengthMode != after.stepLengthMode ||
        before.walkingStepLengthMeters != after.walkingStepLengthMeters ||
        before.learnedStep?.points != after.learnedStep?.points
}
