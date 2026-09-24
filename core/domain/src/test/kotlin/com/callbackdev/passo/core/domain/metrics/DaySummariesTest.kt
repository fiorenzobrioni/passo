package com.callbackdev.passo.core.domain.metrics

import com.callbackdev.passo.core.model.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DaySummariesTest {
    private val day = 20_720L
    private val profile = Profile(heightMeters = 1.75, weightKg = 70.0)

    @Test
    fun `a summary carries the metrics, the goal and the state it was given`() {
        val summary = DaySummaries.summarize(day, listOf(100, 50, 10), profile, goalSteps = 9_000, finalized = true)
        val metrics = MetricsCalculator.day(listOf(100, 50, 10), profile)

        assertThat(summary.localEpochDay).isEqualTo(day)
        assertThat(summary.steps).isEqualTo(160)
        assertThat(summary.distanceMeters).isEqualTo(metrics.distanceMeters)
        assertThat(summary.activeKcal).isEqualTo(metrics.activeKcal)
        assertThat(summary.activeMinutes).isEqualTo(2)
        assertThat(summary.briskMinutes).isEqualTo(1)
        assertThat(summary.goalSteps).isEqualTo(9_000)
        assertThat(summary.finalized).isTrue()
        assertThat(summary.goalReached).isFalse()
    }

    @Test
    fun `an empty day is a summary of zeros`() {
        val summary = DaySummaries.summarize(day, emptyList(), Profile(), goalSteps = 8_000, finalized = false)

        assertThat(summary.steps).isEqualTo(0)
        assertThat(summary.distanceMeters).isEqualTo(0.0)
        assertThat(summary.activeKcal).isEqualTo(0.0)
    }

    @Test
    fun `late steps with the same profile give what a full recomputation gives`() {
        val frozen = DaySummaries.summarize(day, listOf(100, 30, 0), profile, 8_000, finalized = true)

        val late = DaySummaries.withLateSteps(
            frozen,
            listOf(MinuteChange(30, 60), MinuteChange(0, 145), MinuteChange(0, 20)),
            profile,
        )
        val whole = DaySummaries.summarize(day, listOf(100, 60, 145, 20), profile, 8_000, finalized = true)

        assertThat(late.steps).isEqualTo(whole.steps)
        assertThat(late.distanceMeters).isWithin(1e-9).of(whole.distanceMeters)
        assertThat(late.activeKcal).isWithin(1e-9).of(whole.activeKcal)
        assertThat(late.activeMinutes).isEqualTo(whole.activeMinutes)
        assertThat(late.briskMinutes).isEqualTo(whole.briskMinutes)
        assertThat(late.finalized).isTrue()
        assertThat(late.goalSteps).isEqualTo(8_000)
    }

    @Test
    fun `late steps after a profile change leave the rest of the day as it was`() {
        val frozen = DaySummaries.summarize(day, listOf(100), profile, 8_000, finalized = true)
        val heavier = profile.copy(weightKg = 140.0)

        val late = DaySummaries.withLateSteps(frozen, listOf(MinuteChange(0, 100)), heavier)

        // The frozen minute keeps its 70 kg calories; the late one is priced at 140 kg.
        val ownShare = MetricsCalculator.day(listOf(100), profile).activeKcal
        assertThat(late.activeKcal).isWithin(1e-9).of(ownShare + ownShare * 2)
    }
}
