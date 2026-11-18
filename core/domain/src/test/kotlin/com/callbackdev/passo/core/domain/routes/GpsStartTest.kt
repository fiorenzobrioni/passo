package com.callbackdev.passo.core.domain.routes

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The permission, asked in context and once (PLANNING.md §11 Phase 15, «One door for every start»). */
class GpsStartTest {
    @Test
    fun `a plan that does not measure with GPS never asks`() {
        for (readiness in GpsReadiness.entries) {
            for (answer in GpsAnswer.entries) {
                assertThat(GpsStart.step(planAsksGps = false, readiness, answer)).isEqualTo(GpsStartStep.START)
            }
        }
    }

    @Test
    fun `the first time, Passo's own screen comes before the system's request`() {
        assertThat(GpsStart.step(true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.NEVER_ASKED))
            .isEqualTo(GpsStartStep.EXPLAIN)
    }

    @Test
    fun `granted, it starts with nothing asked`() {
        for (answer in GpsAnswer.entries) {
            assertThat(GpsStart.step(true, GpsReadiness.READY, answer)).isEqualTo(GpsStartStep.START)
        }
    }

    @Test
    fun `a refusal is never asked again by itself`() {
        assertThat(GpsStart.step(true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.REFUSED))
            .isEqualTo(GpsStartStep.START)
    }

    @Test
    fun `a grant the system took back is asked again, by the system's dialog alone`() {
        // «Only this time»: granted for the last outing, gone by this one.
        assertThat(GpsStart.step(true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.GRANTED))
            .isEqualTo(GpsStartStep.ASK)
    }

    @Test
    fun `nothing is asked where asking cannot help`() {
        for (readiness in listOf(GpsReadiness.APPROXIMATE_ONLY, GpsReadiness.LOCATION_OFF, GpsReadiness.NO_RECEIVER)) {
            for (answer in GpsAnswer.entries) {
                assertThat(GpsStart.step(true, readiness, answer)).isEqualTo(GpsStartStep.START)
            }
        }
    }

    @Test
    fun `approximate alone is remembered as a refusal of what was asked`() {
        assertThat(GpsStart.answer(preciseGranted = true)).isEqualTo(GpsAnswer.GRANTED)
        assertThat(GpsStart.answer(preciseGranted = false)).isEqualTo(GpsAnswer.REFUSED)
    }
}
