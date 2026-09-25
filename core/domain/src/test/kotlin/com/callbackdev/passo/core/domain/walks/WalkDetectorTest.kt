package com.callbackdev.passo.core.domain.walks

import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.model.Profile
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.roundToInt

class WalkDetectorTest {
    private val profile = Profile(heightMeters = 1.76, weightKg = 72.0)

    private fun stretch(from: Int, minutes: Int, steps: Int) = (from until from + minutes).map { DayMinute(it, steps) }

    private fun detect(minutes: List<DayMinute>, min: Int = 10) = WalkDetector.detect(minutes, profile, min)

    @Test
    fun `an empty day has no walks`() {
        assertThat(detect(emptyList())).isEmpty()
    }

    @Test
    fun `scattered steps are not a walk`() {
        val house = (8 * 60 until 20 * 60 step 3).map { DayMinute(it, 45) }
        assertThat(detect(house)).isEmpty()
    }

    @Test
    fun `a sustained stretch is one walk with its own measures`() {
        val minutes = stretch(10 * 60 + 12, 35, 98)
        val walk = detect(minutes).single()
        assertThat(walk.startMinute).isEqualTo(10 * 60 + 12)
        assertThat(walk.endMinute).isEqualTo(10 * 60 + 47)
        assertThat(walk.minutes).isEqualTo(35)
        assertThat(walk.steps).isEqualTo(35 * 98)
        assertThat(walk.averageCadence).isEqualTo(98)
        assertThat(walk.type).isEqualTo(WalkType.WALK)
        val expected = MetricsCalculator.day(minutes.map { it.steps }, profile)
        assertThat(walk.distanceMeters).isWithin(1e-9).of(expected.distanceMeters)
        assertThat(walk.activeKcal).isWithin(1e-9).of(expected.activeKcal)
    }

    @Test
    fun `a pause of two minutes is bridged and its steps belong to the walk`() {
        val minutes = stretch(600, 8, 110) + DayMinute(608, 20) + stretch(610, 8, 110)
        val walk = detect(minutes).single()
        assertThat(walk.startMinute).isEqualTo(600)
        assertThat(walk.endMinute).isEqualTo(618)
        assertThat(walk.steps).isEqualTo(16 * 110 + 20)
        // The pause is part of the walk's duration, so the cadence is the walk's as lived.
        assertThat(walk.averageCadence).isEqualTo(((16 * 110 + 20) / 18.0).roundToInt())
    }

    @Test
    fun `a pause of three minutes ends the walk`() {
        val minutes = stretch(600, 12, 110) + stretch(615, 12, 110)
        val walks = detect(minutes)
        assertThat(walks.map { it.startMinute to it.endMinute }).containsExactly(600 to 612, 615 to 627).inOrder()
    }

    @Test
    fun `a run shorter than the minimum is discarded, and the minimum is the reader's`() {
        val minutes = stretch(600, 7, 110)
        assertThat(detect(minutes, min = 10)).isEmpty()
        assertThat(detect(minutes, min = 5)).hasSize(1)
    }

    @Test
    fun `exactly the minimum is a walk`() {
        assertThat(detect(stretch(600, 10, 100), min = 10)).hasSize(1)
    }

    @Test
    fun `a high average cadence is a run`() {
        val walk = detect(stretch(7 * 60, 30, 162)).single()
        assertThat(walk.type).isEqualTo(WalkType.RUN)
        assertThat(walk.averageCadence).isEqualTo(162)
    }

    @Test
    fun `walking and running each for a good part of it is mixed`() {
        val minutes = stretch(600, 10, 110) + stretch(610, 10, 160) + stretch(620, 10, 110)
        assertThat(detect(minutes).single().type).isEqualTo(WalkType.MIXED)
    }

    @Test
    fun `a short burst of running inside a walk leaves it a walk`() {
        val minutes = stretch(600, 20, 110) + stretch(620, 3, 150) + stretch(623, 7, 110)
        assertThat(detect(minutes).single().type).isEqualTo(WalkType.WALK)
    }

    @Test
    fun `a walk across midnight is split at midnight`() {
        // Each day is detected on its own minutes: the evening ends at 24:00, the night starts at 00:00.
        val evening = stretch(23 * 60 + 45, 15, 105)
        val night = stretch(0, 12, 105)
        val first = detect(evening).single()
        val second = detect(night).single()
        assertThat(first.endMinute).isEqualTo(24 * 60)
        assertThat(second.startMinute).isEqualTo(0)
        assertThat(first.steps + second.steps).isEqualTo(27 * 105)
    }

    @Test
    fun `minutes out of order and split rows add up`() {
        val minutes = (stretch(600, 12, 50) + stretch(600, 12, 50)).shuffled(java.util.Random(3))
        val walk = detect(minutes).single()
        assertThat(walk.steps).isEqualTo(12 * 100)
    }

    @Test
    fun `several walks in a day, in order`() {
        val minutes = stretch(7 * 60 + 40, 25, 112) + stretch(12 * 60 + 10, 36, 118) + stretch(18 * 60, 15, 104)
        val walks = detect(minutes)
        assertThat(walks.map { it.startMinute }).containsExactly(7 * 60 + 40, 12 * 60 + 10, 18 * 60).inOrder()
    }
}
