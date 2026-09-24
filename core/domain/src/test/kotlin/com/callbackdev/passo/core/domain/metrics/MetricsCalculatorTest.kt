package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.domain.metrics.MetricsConstants.DEFAULT_WALKING_STEP_LENGTH_M
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.DEFAULT_WEIGHT_KG
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Sex
import com.callbackdev.passo.core.model.StepLengthMode
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class MetricsCalculatorTest {
    private val noProfile = Profile()

    @Test
    fun `no minutes add up to nothing`() {
        val metrics = MetricsCalculator.day(emptyList(), noProfile)

        assertThat(metrics).isEqualTo(DayMetrics.ZERO)
        assertThat(metrics.averageCadence).isNull()
    }

    @Test
    fun `minutes of zero steps add nothing`() {
        assertThat(MetricsCalculator.day(listOf(0, 0, 0), noProfile)).isEqualTo(DayMetrics.ZERO)
    }

    @Test
    fun `a negative count, which the database never holds, adds nothing`() {
        assertThat(MetricsCalculator.day(listOf(-5), noProfile)).isEqualTo(DayMetrics.ZERO)
    }

    @Test
    fun `without a profile the defaults apply`() {
        val metrics = MetricsCalculator.day(List(10) { 100 }, noProfile)

        assertThat(metrics.steps).isEqualTo(1_000)
        assertThat(metrics.distanceMeters).isWithin(1e-9).of(1_000 * DEFAULT_WALKING_STEP_LENGTH_M)
        // 0.7 km × 70 kg × 0.5 kcal/kg/km
        assertThat(metrics.activeKcal).isWithin(1e-9).of(0.7 * DEFAULT_WEIGHT_KG * 0.5)
        assertThat(metrics.activeMinutes).isEqualTo(10)
        assertThat(metrics.briskMinutes).isEqualTo(10)
        assertThat(metrics.averageCadence).isEqualTo(100)
    }

    @Test
    fun `the step length follows the height, by sex`() {
        assertThat(StepLengths.of(Profile(heightMeters = 1.80)).walkingMeters).isWithin(1e-9).of(0.747)
        assertThat(StepLengths.of(Profile(heightMeters = 1.80, sex = Sex.MALE)).walkingMeters)
            .isWithin(1e-9).of(0.747)
        assertThat(StepLengths.of(Profile(heightMeters = 1.80, sex = Sex.FEMALE)).walkingMeters)
            .isWithin(1e-9).of(0.7434)
    }

    @Test
    fun `without a height the default step length applies, whatever the sex`() {
        assertThat(StepLengths.of(Profile(sex = Sex.FEMALE)).walkingMeters).isEqualTo(DEFAULT_WALKING_STEP_LENGTH_M)
    }

    @Test
    fun `a manual or calibrated step length wins over the height`() {
        val manual = Profile(heightMeters = 1.80, stepLengthMode = StepLengthMode.MANUAL, walkingStepLengthMeters = 0.8)
        val calibrated = manual.copy(stepLengthMode = StepLengthMode.CALIBRATED, walkingStepLengthMeters = 0.77)

        assertThat(StepLengths.of(manual).walkingMeters).isEqualTo(0.8)
        assertThat(StepLengths.of(calibrated).walkingMeters).isEqualTo(0.77)
    }

    @Test
    fun `in auto mode a stored manual length is ignored`() {
        val profile = Profile(heightMeters = 1.80, walkingStepLengthMeters = 0.5)

        assertThat(StepLengths.of(profile).walkingMeters).isWithin(1e-9).of(0.747)
    }

    @Test
    fun `manual mode without a length falls back to the height`() {
        val profile = Profile(heightMeters = 1.80, stepLengthMode = StepLengthMode.MANUAL)

        assertThat(StepLengths.of(profile).walkingMeters).isWithin(1e-9).of(0.747)
    }

    @Test
    fun `the running step length is 1_3 times the walking one unless set`() {
        assertThat(StepLengths.of(noProfile).runningMeters).isWithin(1e-9).of(0.91)
        assertThat(StepLengths.of(Profile(runningStepLengthMeters = 1.1)).runningMeters).isEqualTo(1.1)
    }

    @Test
    fun `a running minute uses the running step length and cost`() {
        val metrics = MetricsCalculator.day(listOf(150), noProfile)

        assertThat(metrics.distanceMeters).isWithin(1e-9).of(150 * 0.91)
        assertThat(metrics.activeKcal).isWithin(1e-9).of(0.1365 * DEFAULT_WEIGHT_KG * 1.0)
    }

    @Test
    fun `active and brisk minutes start exactly at their thresholds`() {
        val metrics = MetricsCalculator.day(listOf(39, 40, 99, 100), noProfile)

        assertThat(metrics.activeMinutes).isEqualTo(3)
        assertThat(metrics.briskMinutes).isEqualTo(1)
    }

    @Test
    fun `the average cadence counts active minutes only`() {
        val metrics = MetricsCalculator.day(listOf(100, 121, 10, 0), noProfile)

        assertThat(metrics.activeMinutes).isEqualTo(2)
        assertThat(metrics.activeSteps).isEqualTo(221)
        assertThat(metrics.averageCadence).isEqualTo(111)
    }

    @Test
    fun `the energy cost is flat when walking, rises when brisk and doubles when running`() {
        assertThat(MetricsCalculator.kcalPerKgPerKm(30)).isEqualTo(0.5)
        assertThat(MetricsCalculator.kcalPerKgPerKm(100)).isEqualTo(0.5)
        assertThat(MetricsCalculator.kcalPerKgPerKm(120)).isWithin(1e-9).of(0.55)
        assertThat(MetricsCalculator.kcalPerKgPerKm(139)).isLessThan(0.6)
        assertThat(MetricsCalculator.kcalPerKgPerKm(140)).isEqualTo(1.0)
        assertThat(MetricsCalculator.kcalPerKgPerKm(200)).isEqualTo(1.0)
    }

    @Test
    fun `calories scale with the weight`() {
        val default = MetricsCalculator.day(listOf(80), noProfile).activeKcal
        val heavier = MetricsCalculator.day(listOf(80), Profile(weightKg = 105.0)).activeKcal

        assertThat(heavier).isWithin(1e-9).of(default * 1.5)
    }

    @Test
    fun `implausible profile values fall back to the defaults`() {
        val typo = Profile(
            heightMeters = 17.5,
            weightKg = 7.0,
            stepLengthMode = StepLengthMode.MANUAL,
            walkingStepLengthMeters = 7.0,
            runningStepLengthMeters = -1.0,
        )

        assertThat(StepLengths.of(typo)).isEqualTo(StepLengths.of(noProfile))
        assertThat(MetricsCalculator.weightKg(typo)).isEqualTo(DEFAULT_WEIGHT_KG)
    }

    @Test
    fun `sanitized keeps valid values and drops the rest`() {
        val profile = Profile(heightMeters = 1.7, weightKg = 400.0, sex = Sex.FEMALE)

        assertThat(profile.sanitized()).isEqualTo(Profile(heightMeters = 1.7, sex = Sex.FEMALE))
    }
}
