package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.domain.calibration.LearnedSteps
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StepLengthsTest {
    private val profile = Profile(heightMeters = 1.78)
    private val estimate = 1.78 * MetricsConstants.STEP_LENGTH_HEIGHT_RATIO

    private val walkingOnly = LearnedStep(
        points = listOf(StepPoint(95.0, 0.76), StepPoint(115.0, 0.82)),
        usualCadence = 105,
        learntMeters = 14_000.0,
        outings = 5,
        throughMillis = 0,
    )
    private val both = walkingOnly.copy(
        points =
        walkingOnly.points + listOf(StepPoint(155.0, 1.00), StepPoint(165.0, 1.10)),
    )

    @Test
    fun `without a learnt step the two lengths are read as before`() {
        val lengths = StepLengths.of(profile)

        assertThat(lengths.forCadence(100)).isWithin(1e-9).of(estimate)
        assertThat(lengths.forCadence(160)).isWithin(1e-9).of(estimate * MetricsConstants.RUNNING_STEP_LENGTH_FACTOR)
    }

    @Test
    fun `the walking part is read along its line, flat beyond its ends up to the split`() {
        val lengths = StepLengths.of(LearnedSteps.accept(profile, walkingOnly))

        assertThat(lengths.forCadence(60)).isWithin(1e-9).of(0.76)
        assertThat(lengths.forCadence(105)).isWithin(1e-9).of(0.79)
        assertThat(lengths.forCadence(139)).isWithin(1e-9).of(0.82)
    }

    @Test
    fun `the walking length a screen names is the curve's at the usual cadence`() {
        // Today's line («at a step of 79 cm») and a city walk's steps read this one number.
        assertThat(StepLengths.of(LearnedSteps.accept(profile, walkingOnly)).walkingMeters).isWithin(1e-9).of(0.79)
    }

    @Test
    fun `a running part not learnt keeps the running length, or the factor`() {
        val learned = StepLengths.of(LearnedSteps.accept(profile, walkingOnly))
        assertThat(learned.forCadence(160)).isWithin(1e-9).of(0.79 * MetricsConstants.RUNNING_STEP_LENGTH_FACTOR)

        val own = StepLengths.of(LearnedSteps.accept(profile.copy(runningStepLengthMeters = 1.2), walkingOnly))
        assertThat(own.forCadence(160)).isWithin(1e-9).of(1.2)
    }

    @Test
    fun `a running part learnt is read along its own line`() {
        val lengths = StepLengths.of(LearnedSteps.accept(profile.copy(runningStepLengthMeters = 1.2), both))

        assertThat(lengths.forCadence(140)).isWithin(1e-9).of(1.00)
        assertThat(lengths.forCadence(160)).isWithin(1e-9).of(1.05)
        assertThat(lengths.forCadence(175)).isWithin(1e-9).of(1.10)
        assertThat(lengths.runningMeters).isWithin(1e-9).of(1.05)
        // The walking part does not reach past the split.
        assertThat(lengths.forCadence(139)).isWithin(1e-9).of(0.82)
    }

    @Test
    fun `a running length set after accepting wins over the curve's running part, walking keeps the curve`() {
        val accepted = LearnedSteps.accept(profile, both)
        val lengths = StepLengths.of(LearnedSteps.chooseRunning(accepted, 1.25))

        assertThat(lengths.forCadence(160)).isWithin(1e-9).of(1.25)
        assertThat(lengths.forCadence(105)).isWithin(1e-9).of(0.79)
    }

    @Test
    fun `the curve accepted, then the height changed, the curve stays in use`() {
        val accepted = LearnedSteps.accept(profile, walkingOnly)
        val taller = StepLengths.of(accepted.copy(heightMeters = 1.90))

        assertThat(taller.walkingMeters).isWithin(1e-9).of(0.79)
        assertThat(taller.forCadence(115)).isWithin(1e-9).of(0.82)
    }

    @Test
    fun `a learnt step not in use is not read`() {
        val lengths = StepLengths.of(profile.copy(learnedStep = walkingOnly))

        assertThat(lengths.learned).isNull()
        assertThat(lengths.forCadence(105)).isWithin(1e-9).of(estimate)
    }

    @Test
    fun `a learnt mode with no walking point falls back to the height's estimate`() {
        val runningOnly = walkingOnly.copy(points = listOf(StepPoint(155.0, 1.0)))
        val broken = profile.copy(stepLengthMode = StepLengthMode.LEARNED, learnedStep = runningOnly)

        assertThat(broken.sanitized().stepLengthMode).isEqualTo(StepLengthMode.AUTO)
        assertThat(StepLengths.of(broken).walkingMeters).isWithin(1e-9).of(estimate)
    }

    @Test
    fun `a point no one walks with is dropped from a learnt step`() {
        val odd = walkingOnly.copy(points = walkingOnly.points + StepPoint(120.0, 3.0) + StepPoint(Double.NaN, 0.8))

        assertThat(odd.sanitized(null)?.points).isEqualTo(walkingOnly.points)
    }
}
