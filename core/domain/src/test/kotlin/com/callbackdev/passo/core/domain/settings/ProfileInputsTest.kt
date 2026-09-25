package com.callbackdev.passo.core.domain.settings

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ProfileInputsTest {
    @Test
    fun `a metric height moves by whole centimetres`() {
        val scale = ProfileInputs.height(UnitSystem.METRIC)

        assertThat(scale.snap(1.7549)).isWithin(1e-9).of(1.75)
        assertThat(scale.up(1.75)).isWithin(1e-9).of(1.76)
        assertThat(scale.down(1.75)).isWithin(1e-9).of(1.74)
    }

    @Test
    fun `an imperial height moves by whole inches and stays metric`() {
        val scale = ProfileInputs.height(UnitSystem.IMPERIAL)
        val fiveNine = UnitConversions.feetAndInchesToMeters(5, 9.0)

        assertThat(scale.snap(1.75)).isWithin(1e-9).of(fiveNine)
        assertThat(UnitConversions.metersToInches(scale.up(fiveNine))).isWithin(1e-9).of(70.0)
    }

    @Test
    fun `pounds are stored as kilograms`() {
        val scale = ProfileInputs.weight(UnitSystem.IMPERIAL)

        assertThat(UnitConversions.kgToPounds(scale.snap(70.0))).isWithin(1e-9).of(154.0)
    }

    @Test
    fun `values stay inside the picker's range`() {
        assertThat(ProfileInputs.goal.up(30_000.0)).isEqualTo(30_000.0)
        assertThat(ProfileInputs.goal.down(1_000.0)).isEqualTo(1_000.0)
        assertThat(ProfileInputs.goal.snap(8_240.0)).isEqualTo(8_000.0)
        assertThat(ProfileInputs.weight(UnitSystem.METRIC).snap(500.0)).isEqualTo(180.0)
    }
}
