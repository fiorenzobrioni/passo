package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepPoint
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LearnedStepTextTest {
    private val step = LearnedStep(
        points = listOf(StepPoint(94.6, 0.7612), StepPoint(116.25, 0.81), StepPoint(162.0, 1.04)),
        usualCadence = 104,
        learntMeters = 14_230.5,
        outings = 6,
        throughMillis = 1_763_550_000_000,
        replaced = StepLengthMode.CALIBRATED,
        running = false,
    )

    @Test
    fun `a learnt step reads back as it was written`() {
        assertThat(LearnedStepText.decode(LearnedStepText.encode(step))).isEqualTo(step)
    }

    @Test
    fun `a line that cannot be read is no learnt step`() {
        assertThat(LearnedStepText.decode(null)).isNull()
        assertThat(LearnedStepText.decode("")).isNull()
        assertThat(LearnedStepText.decode("2;104;1;1;1;AUTO;1;100.0:0.8")).isNull()
        assertThat(LearnedStepText.decode("1;x;1;1;1;AUTO;1;100.0:0.8")).isNull()
        assertThat(LearnedStepText.decode("1;104;1;1;1;AUTO;1;100.0-0.8")).isNull()
    }

    @Test
    fun `a newer build's fields at the end are skipped`() {
        assertThat(LearnedStepText.decode(LearnedStepText.encode(step) + ";more")).isEqualTo(step)
    }
}
