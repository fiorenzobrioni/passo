package com.callbackdev.passo.core.domain.format

import com.callbackdev.passo.core.model.Measure
import com.callbackdev.passo.core.model.MeasureUnit
import com.callbackdev.passo.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.Locale

class MeasureFormatterTest {
    private val english = MeasureFormatter(Locale.US, UnitSystem.METRIC)
    private val italian = MeasureFormatter(Locale.ITALY, UnitSystem.METRIC)
    private val imperial = MeasureFormatter(Locale.US, UnitSystem.IMPERIAL)

    @Test
    fun `steps are grouped the locale's way`() {
        assertThat(english.steps(8_420)).isEqualTo("8,420")
        assertThat(italian.steps(8_420)).isEqualTo("8.420")
        assertThat(english.steps(1_234_567)).isEqualTo("1,234,567")
        assertThat(english.steps(0)).isEqualTo("0")
    }

    @Test
    fun `a distance keeps a fixed number of decimals for its magnitude`() {
        assertThat(english.distance(2_400.0)).isEqualTo(Measure("2.40", MeasureUnit.KILOMETER))
        assertThat(italian.distance(2_400.0)).isEqualTo(Measure("2,40", MeasureUnit.KILOMETER))
        assertThat(english.distance(0.0)).isEqualTo(Measure("0.00", MeasureUnit.KILOMETER))
        assertThat(english.distance(12_345.0)).isEqualTo(Measure("12.3", MeasureUnit.KILOMETER))
        assertThat(english.distance(1_234_567.0)).isEqualTo(Measure("1,234", MeasureUnit.KILOMETER))
    }

    @Test
    fun `a distance is rounded down, never shown as covered before it is`() {
        assertThat(english.distance(4_999.0)).isEqualTo(Measure("4.99", MeasureUnit.KILOMETER))
    }

    @Test
    fun `imperial distances are in miles`() {
        assertThat(imperial.distance(1_609.344)).isEqualTo(Measure("1.00", MeasureUnit.MILE))
        assertThat(imperial.distance(5_000.0)).isEqualTo(Measure("3.10", MeasureUnit.MILE))
    }

    @Test
    fun `energy is whole kilocalories in both systems`() {
        assertThat(english.energy(245.6)).isEqualTo(Measure("246", MeasureUnit.KILOCALORIE))
        assertThat(imperial.energy(1_245.4)).isEqualTo(Measure("1,245", MeasureUnit.KILOCALORIE))
    }

    @Test
    fun `a weight shows a decimal only when it has one`() {
        assertThat(english.weight(72.0)).isEqualTo(Measure("72", MeasureUnit.KILOGRAM))
        assertThat(italian.weight(72.5)).isEqualTo(Measure("72,5", MeasureUnit.KILOGRAM))
        assertThat(imperial.weight(70.0)).isEqualTo(Measure("154.3", MeasureUnit.POUND))
    }

    @Test
    fun `a height is centimetres, or feet and inches`() {
        assertThat(english.height(1.754)).containsExactly(Measure("175", MeasureUnit.CENTIMETER))
        assertThat(imperial.height(1.75))
            .containsExactly(Measure("5", MeasureUnit.FOOT), Measure("9", MeasureUnit.INCH))
            .inOrder()
    }

    @Test
    fun `a step length is centimetres, or inches`() {
        assertThat(english.stepLength(0.726)).isEqualTo(Measure("73", MeasureUnit.CENTIMETER))
        assertThat(imperial.stepLength(0.7)).isEqualTo(Measure("27.6", MeasureUnit.INCH))
    }

    @Test
    fun `a short distance is whole metres, or whole yards`() {
        assertThat(english.shortDistance(400.0)).isEqualTo(Measure("400", MeasureUnit.METER))
        assertThat(italian.shortDistance(1_500.0)).isEqualTo(Measure("1.500", MeasureUnit.METER))
        assertThat(imperial.shortDistance(91.44)).isEqualTo(Measure("100", MeasureUnit.YARD))
    }

    @Test
    fun `minutes and cadence are whole numbers`() {
        assertThat(english.minutes(1_440)).isEqualTo(Measure("1,440", MeasureUnit.MINUTE))
        assertThat(italian.cadence(112)).isEqualTo(Measure("112", MeasureUnit.STEPS_PER_MINUTE))
    }

    @Test
    fun `the goal share is floored and not capped`() {
        assertThat(english.percent(0.999)).isEqualTo("99%")
        assertThat(english.percent(1.2)).isEqualTo("120%")
        assertThat(italian.percent(0.63)).isEqualTo("63%")
    }
}
