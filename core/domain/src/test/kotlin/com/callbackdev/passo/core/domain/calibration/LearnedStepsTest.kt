package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearnedStepsTest {
    private val curve = LearnedStep(
        points = listOf(StepPoint(105.0, 0.79), StepPoint(160.0, 1.05)),
        usualCadence = 105,
        learntMeters = 14_000.0,
        outings = 5,
        throughMillis = 5_000,
    )
    private val measured = Profile(
        heightMeters = 1.78,
        stepLengthMode = StepLengthMode.CALIBRATED,
        walkingStepLengthMeters = 0.76,
    )
    private val typed = measured.copy(stepLengthMode = StepLengthMode.MANUAL)
    private val estimated = Profile(heightMeters = 1.78)

    @Test
    fun `use it keeps the lengths the reader had and remembers the mode it replaced`() {
        val accepted = LearnedSteps.accept(measured, curve)

        assertThat(accepted.stepLengthMode).isEqualTo(StepLengthMode.LEARNED)
        assertThat(accepted.walkingStepLengthMeters).isEqualTo(0.76)
        assertThat(accepted.learnedStep?.replaced).isEqualTo(StepLengthMode.CALIBRATED)
        assertThat(accepted.learnedStep?.running).isTrue()
    }

    @Test
    fun `a curve accepted over a curve keeps the first one's way back`() {
        val first = LearnedSteps.accept(typed, curve)
        val second = LearnedSteps.accept(first, curve.copy(points = listOf(StepPoint(105.0, 0.80))))

        assertThat(second.learnedStep?.replaced).isEqualTo(StepLengthMode.MANUAL)
    }

    @Test
    fun `go back returns to the measured step`() {
        assertThat(LearnedSteps.goBack(LearnedSteps.accept(measured, curve))).isEqualTo(measured)
        assertThat(LearnedSteps.wayBack(LearnedSteps.accept(measured, curve))).isEqualTo(StepLengthMode.CALIBRATED)
    }

    @Test
    fun `go back returns to the step typed`() {
        assertThat(LearnedSteps.goBack(LearnedSteps.accept(typed, curve))).isEqualTo(typed)
    }

    @Test
    fun `go back returns to the estimate from the height`() {
        assertThat(LearnedSteps.goBack(LearnedSteps.accept(estimated, curve))).isEqualTo(estimated)
    }

    @Test
    fun `go back keeps a running length chosen meanwhile`() {
        val accepted = LearnedSteps.chooseRunning(LearnedSteps.accept(estimated, curve), 1.2)

        assertThat(LearnedSteps.goBack(accepted)).isEqualTo(estimated.copy(runningStepLengthMeters = 1.2))
    }

    @Test
    fun `the wizard while a curve is in use puts it away, the measured step is in use`() {
        val result = CalibrationResult.Measured(CalibratedStep.WALKING, 0.78, 128, 100.0, 105, false)
        val after = StepCalibration.apply(LearnedSteps.accept(measured, curve), result)

        assertThat(after.stepLengthMode).isEqualTo(StepLengthMode.CALIBRATED)
        assertThat(after.learnedStep).isNull()
        assertThat(StepLengths.of(after).walkingMeters).isEqualTo(0.78)
    }

    @Test
    fun `the wizard's running walk changes no mode, it wins over the curve's running part`() {
        val result = CalibrationResult.Measured(CalibratedStep.RUNNING, 1.15, 200, 230.0, 160, false)
        val after = StepCalibration.apply(LearnedSteps.accept(measured, curve), result)

        assertThat(after.stepLengthMode).isEqualTo(StepLengthMode.LEARNED)
        assertThat(after.learnedStep?.running).isFalse()
        assertThat(StepLengths.of(after).forCadence(160)).isEqualTo(1.15)
        assertThat(StepLengths.of(after).forCadence(105)).isEqualTo(0.79)
    }

    @Test
    fun `a typed length while a curve is in use is the length in use`() {
        val after = LearnedSteps.chooseWalking(LearnedSteps.accept(measured, curve), 0.81)

        assertThat(after.stepLengthMode).isEqualTo(StepLengthMode.MANUAL)
        assertThat(after.walkingStepLengthMeters).isEqualTo(0.81)
        assertThat(after.learnedStep).isNull()
        assertThat(LearnedSteps.walkingChanged(LearnedSteps.accept(measured, curve), after)).isTrue()
    }

    @Test
    fun `the measured length kept in the dialog over a curve is the way back to it`() {
        val after = LearnedSteps.chooseWalking(LearnedSteps.accept(measured, curve), 0.76)

        assertThat(after).isEqualTo(measured)
    }

    @Test
    fun `the learnt step kept in the dialog changes nothing`() {
        val accepted = LearnedSteps.accept(measured, curve)

        assertThat(LearnedSteps.chooseWalking(accepted, null, keepLearned = true)).isEqualTo(accepted)
        assertThat(LearnedSteps.walkingChanged(accepted, accepted)).isFalse()
    }

    @Test
    fun `a measured length saved unchanged stays measured`() {
        assertThat(LearnedSteps.chooseWalking(measured, 0.76)).isEqualTo(measured)
    }

    @Test
    fun `the estimate chosen over a curve is the height's`() {
        val after = LearnedSteps.chooseWalking(LearnedSteps.accept(measured, curve), null)

        assertThat(after.stepLengthMode).isEqualTo(StepLengthMode.AUTO)
        assertThat(after.learnedStep).isNull()
    }

    @Test
    fun `the learnt running part chosen again is in use again`() {
        val own = LearnedSteps.chooseRunning(LearnedSteps.accept(measured, curve), 1.2)
        val back = LearnedSteps.chooseRunning(own, null, learnedRunning = true)

        assertThat(StepLengths.of(back).forCadence(160)).isEqualTo(1.05)
    }
}
