package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.StepLengthMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StepCalibrationTest {
    @Test
    fun `the step is the distance over the steps counted between Start and Stop`() {
        val result = StepCalibration.measure(CalibratedStep.WALKING, 100.0, 12_000, 12_137, 80_000)

        assertThat(result).isInstanceOf(CalibrationResult.Measured::class.java)
        result as CalibrationResult.Measured
        assertThat(result.steps).isEqualTo(137)
        assertThat(result.stepLengthMeters).isWithin(1e-9).of(100.0 / 137)
        assertThat(result.cadence).isEqualTo(103)
        assertThat(result.paceMismatch).isFalse()
    }

    @Test
    fun `fewer than thirty steps are too few to measure`() {
        assertThat(StepCalibration.measure(CalibratedStep.WALKING, 50.0, 100, 129, 30_000))
            .isEqualTo(CalibrationResult.TooFewSteps(29))
        assertThat(StepCalibration.measure(CalibratedStep.WALKING, 50.0, 100, 100, 30_000))
            .isEqualTo(CalibrationResult.TooFewSteps(0))
    }

    @Test
    fun `a counter that went back cannot measure anything`() {
        assertThat(StepCalibration.measure(CalibratedStep.WALKING, 100.0, 5_000, 40, 60_000))
            .isEqualTo(CalibrationResult.CounterReset)
    }

    @Test
    fun `a step no one walks with is refused, with the length it came to`() {
        // 2 km in 100 steps: the distance was set wrong.
        val long = StepCalibration.measure(CalibratedStep.WALKING, 2_000.0, 0, 100, 60_000)
        assertThat(long).isEqualTo(CalibrationResult.Implausible(20.0, 100))

        // A running step may be longer than a walking one: 1.8 m is a sprinter's, not a mistake.
        val run = StepCalibration.measure(CalibratedStep.RUNNING, 180.0, 0, 100, 40_000)
        assertThat(run).isInstanceOf(CalibrationResult.Measured::class.java)
        val walk = StepCalibration.measure(CalibratedStep.WALKING, 180.0, 0, 100, 40_000)
        assertThat(walk).isInstanceOf(CalibrationResult.Implausible::class.java)
    }

    @Test
    fun `a walking step measured at a running cadence is said, not refused`() {
        val result = StepCalibration.measure(CalibratedStep.WALKING, 100.0, 0, 120, 45_000)
            as CalibrationResult.Measured
        assertThat(result.cadence).isEqualTo(160)
        assertThat(result.paceMismatch).isTrue()

        val slowRun = StepCalibration.measure(CalibratedStep.RUNNING, 100.0, 0, 110, 60_000)
            as CalibrationResult.Measured
        assertThat(slowRun.paceMismatch).isTrue()
    }

    @Test
    fun `a walk too short in time has no cadence and no verdict on the pace`() {
        val result = StepCalibration.measure(CalibratedStep.WALKING, 50.0, 0, 70, 9_000)
            as CalibrationResult.Measured
        assertThat(result.cadence).isNull()
        assertThat(result.paceMismatch).isFalse()
    }

    @Test
    fun `a walking step is saved as measured, and distance follows it`() {
        val result = StepCalibration.measure(CalibratedStep.WALKING, 100.0, 0, 125, 75_000)
            as CalibrationResult.Measured
        val profile = StepCalibration.apply(Profile(heightMeters = 1.80), result)

        assertThat(profile.stepLengthMode).isEqualTo(StepLengthMode.CALIBRATED)
        assertThat(profile.walkingStepLengthMeters).isWithin(1e-9).of(0.8)
        assertThat(StepLengths.of(profile).walkingMeters).isWithin(1e-9).of(0.8)
        // The running step still follows the walking one.
        assertThat(StepLengths.of(profile).runningMeters).isWithin(1e-9).of(0.8 * 1.3)
    }

    @Test
    fun `a running step is saved as the reader's own, leaving the walking one alone`() {
        val result = StepCalibration.measure(CalibratedStep.RUNNING, 400.0, 0, 400, 150_000)
            as CalibrationResult.Measured
        val before = Profile(stepLengthMode = StepLengthMode.MANUAL, walkingStepLengthMeters = 0.7)
        val profile = StepCalibration.apply(before, result)

        assertThat(profile.runningStepLengthMeters).isWithin(1e-9).of(1.0)
        assertThat(profile.stepLengthMode).isEqualTo(StepLengthMode.MANUAL)
        assertThat(profile.walkingStepLengthMeters).isEqualTo(0.7)
    }
}
