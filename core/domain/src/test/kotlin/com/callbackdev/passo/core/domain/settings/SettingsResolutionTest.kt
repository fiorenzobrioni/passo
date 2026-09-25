package com.callbackdev.passo.core.domain.settings

import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.util.Locale

class SettingsResolutionTest {
    @Test
    fun `system units follow the region`() {
        assertThat(UnitPreference.SYSTEM.resolve("IT")).isEqualTo(UnitSystem.METRIC)
        assertThat(UnitPreference.SYSTEM.resolve("US")).isEqualTo(UnitSystem.IMPERIAL)
        assertThat(UnitPreference.SYSTEM.resolve("gb")).isEqualTo(UnitSystem.IMPERIAL)
        assertThat(UnitPreference.SYSTEM.resolve("")).isEqualTo(UnitSystem.METRIC)
        assertThat(UnitPreference.SYSTEM.resolve(null)).isEqualTo(UnitSystem.METRIC)
    }

    @Test
    fun `a chosen unit system ignores the region`() {
        assertThat(UnitPreference.METRIC.resolve("US")).isEqualTo(UnitSystem.METRIC)
        assertThat(UnitPreference.IMPERIAL.resolve("IT")).isEqualTo(UnitSystem.IMPERIAL)
    }

    @Test
    fun `the week starts where the locale says unless chosen`() {
        assertThat(firstDayOfWeek(null, Locale.ITALY)).isEqualTo(DayOfWeek.MONDAY)
        assertThat(firstDayOfWeek(null, Locale.US)).isEqualTo(DayOfWeek.SUNDAY)
        assertThat(firstDayOfWeek(DayOfWeek.SATURDAY, Locale.ITALY)).isEqualTo(DayOfWeek.SATURDAY)
    }
}
