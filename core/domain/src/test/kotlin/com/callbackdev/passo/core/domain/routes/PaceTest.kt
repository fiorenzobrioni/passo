package com.callbackdev.passo.core.domain.routes

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** A pace per kilometre, shown, never a goal (docs/adr/0016-location.md, decision 7). */
class PaceTest {
    @Test
    fun `time in motion over distance`() {
        // A kilometre in ten minutes of motion.
        assertThat(Pace.secondsPerKm(movingMillis = 600_000, meters = 1_000.0)).isWithin(1e-9).of(600.0)
        // Five kilometres in fifty-two: ten minutes twenty-four a kilometre.
        assertThat(Pace.secondsPerKm(movingMillis = 52 * 60_000L, meters = 5_000.0)).isWithin(1e-9).of(624.0)
    }

    @Test
    fun `none under two hundred metres`() {
        assertThat(Pace.secondsPerKm(movingMillis = 120_000, meters = 199.9)).isNull()
        assertThat(Pace.secondsPerKm(movingMillis = 120_000, meters = 200.0)).isNotNull()
    }

    @Test
    fun `none with no time in motion`() {
        assertThat(Pace.secondsPerKm(movingMillis = 0, meters = 500.0)).isNull()
    }

    @Test
    fun `by the mile it is the same pace, over a longer distance`() {
        val perKm = 600.0

        assertThat(Pace.secondsPerMile(perKm)).isWithin(0.01).of(965.61)
    }
}
