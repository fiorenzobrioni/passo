package com.callbackdev.passo.core.domain.format

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class UnitConversionsTest {
    @Test
    fun `a mile is 1609_344 metres`() {
        assertThat(UnitConversions.metersToMiles(1_609.344)).isWithin(1e-12).of(1.0)
        assertThat(UnitConversions.milesToMeters(2.0)).isWithin(1e-9).of(3_218.688)
    }

    @Test
    fun `a pound is 0_45359237 kilograms`() {
        assertThat(UnitConversions.poundsToKg(1.0)).isEqualTo(0.45359237)
        assertThat(UnitConversions.kgToPounds(70.0)).isWithin(1e-3).of(154.324)
    }

    @Test
    fun `inches and metres round-trip`() {
        assertThat(UnitConversions.inchesToMeters(UnitConversions.metersToInches(0.73))).isWithin(1e-12).of(0.73)
    }

    @Test
    fun `a height splits into whole feet and inches`() {
        assertThat(UnitConversions.metersToFeetAndInches(1.75)).isEqualTo(FeetAndInches(5, 9))
        // 71.9 inches round to 72: six feet, not five feet twelve.
        assertThat(UnitConversions.metersToFeetAndInches(1.826)).isEqualTo(FeetAndInches(6, 0))
        assertThat(UnitConversions.feetAndInchesToMeters(5, 9.0)).isWithin(1e-9).of(1.7526)
    }
}
