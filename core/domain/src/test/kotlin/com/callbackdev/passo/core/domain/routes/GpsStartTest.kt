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
        for (readiness in listOf(GpsReadiness.LOCATION_OFF, GpsReadiness.NO_RECEIVER)) {
            for (answer in GpsAnswer.entries) {
                assertThat(GpsStart.step(true, readiness, answer)).isEqualTo(GpsStartStep.START)
            }
        }
    }

    @Test
    fun `approximate alone is not asked again by itself, whether it was a refusal or a choice`() {
        // The approximate one picked in Passo's own request, and the precise one taken back in
        // the system's page: each is the reader's answer.
        for (answer in listOf(GpsAnswer.REFUSED, GpsAnswer.GRANTED)) {
            assertThat(GpsStart.step(true, GpsReadiness.APPROXIMATE_ONLY, answer)).isEqualTo(GpsStartStep.START)
        }
    }

    @Test
    fun `approximate alone with no answer to remember is asked as the first time`() {
        assertThat(GpsStart.step(true, GpsReadiness.APPROXIMATE_ONLY, GpsAnswer.NEVER_ASKED))
            .isEqualTo(GpsStartStep.EXPLAIN)
    }

    @Test
    fun `the switch turned on again after a refusal is the reader asking again`() {
        for (readiness in listOf(GpsReadiness.PERMISSION_NEEDED, GpsReadiness.APPROXIMATE_ONLY)) {
            assertThat(GpsStart.asksAgain(wasOn = false, isOn = true, readiness, GpsAnswer.REFUSED)).isTrue()
            // And then the next start asks, as the first did.
            assertThat(GpsStart.step(true, readiness, GpsAnswer.NEVER_ASKED)).isEqualTo(GpsStartStep.EXPLAIN)
        }
        // The precise one taken back in the system's page, down to the approximate.
        assertThat(GpsStart.asksAgain(false, true, GpsReadiness.APPROXIMATE_ONLY, GpsAnswer.GRANTED)).isTrue()
    }

    @Test
    fun `a plan saved with the switch as it was asks nothing again`() {
        for (readiness in GpsReadiness.entries) {
            for (answer in GpsAnswer.entries) {
                // On before and still on: renaming a plan is not asking for the permission.
                assertThat(GpsStart.asksAgain(wasOn = true, isOn = true, readiness, answer)).isFalse()
                assertThat(GpsStart.asksAgain(wasOn = true, isOn = false, readiness, answer)).isFalse()
                assertThat(GpsStart.asksAgain(wasOn = false, isOn = false, readiness, answer)).isFalse()
            }
        }
    }

    @Test
    fun `nothing is forgotten where Passo asks anyway, or where the permission is not what is missing`() {
        assertThat(GpsStart.asksAgain(false, true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.NEVER_ASKED)).isFalse()
        // «Only this time»: the system's request alone, without Passo's screen a second time.
        assertThat(GpsStart.asksAgain(false, true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.GRANTED)).isFalse()
        for (readiness in listOf(GpsReadiness.READY, GpsReadiness.LOCATION_OFF, GpsReadiness.NO_RECEIVER)) {
            for (answer in GpsAnswer.entries) {
                assertThat(GpsStart.asksAgain(false, true, readiness, answer)).isFalse()
            }
        }
    }

    @Test
    fun `the switch's note - plain when off, or when the phone is allowed`() {
        for (readiness in GpsReadiness.entries) {
            for (answer in GpsAnswer.entries) {
                assertThat(GpsStart.switchNote(wasOn = true, isOn = false, readiness, answer))
                    .isEqualTo(GpsSwitchNote.PLAIN)
            }
        }
        for (readiness in listOf(GpsReadiness.READY, GpsReadiness.LOCATION_OFF, GpsReadiness.NO_RECEIVER)) {
            assertThat(GpsStart.switchNote(true, true, readiness, GpsAnswer.REFUSED)).isEqualTo(GpsSwitchNote.PLAIN)
        }
    }

    @Test
    fun `the switch's note - Passo asks at the start, wherever it will`() {
        // The first time, a grant taken back, and the switch just turned on after a refusal.
        assertThat(GpsStart.switchNote(false, true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.NEVER_ASKED))
            .isEqualTo(GpsSwitchNote.ASKS_AT_START)
        assertThat(GpsStart.switchNote(true, true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.GRANTED))
            .isEqualTo(GpsSwitchNote.ASKS_AT_START)
        assertThat(GpsStart.switchNote(false, true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.REFUSED))
            .isEqualTo(GpsSwitchNote.ASKS_AT_START)
        assertThat(GpsStart.switchNote(false, true, GpsReadiness.APPROXIMATE_ONLY, GpsAnswer.REFUSED))
            .isEqualTo(GpsSwitchNote.ASKS_AT_START)
    }

    @Test
    fun `the switch's note - on since before a refusal, it says the outing counts from the steps`() {
        assertThat(GpsStart.switchNote(wasOn = true, isOn = true, GpsReadiness.PERMISSION_NEEDED, GpsAnswer.REFUSED))
            .isEqualTo(GpsSwitchNote.NOT_ALLOWED)
        assertThat(GpsStart.switchNote(true, true, GpsReadiness.APPROXIMATE_ONLY, GpsAnswer.REFUSED))
            .isEqualTo(GpsSwitchNote.NOT_ALLOWED)
        assertThat(GpsStart.switchNote(true, true, GpsReadiness.APPROXIMATE_ONLY, GpsAnswer.GRANTED))
            .isEqualTo(GpsSwitchNote.NOT_ALLOWED)
    }

    @Test
    fun `approximate alone is remembered as a refusal of what was asked`() {
        assertThat(GpsStart.answer(preciseGranted = true)).isEqualTo(GpsAnswer.GRANTED)
        assertThat(GpsStart.answer(preciseGranted = false)).isEqualTo(GpsAnswer.REFUSED)
    }
}
