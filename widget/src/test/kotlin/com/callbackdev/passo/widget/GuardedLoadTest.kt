package com.callbackdev.passo.widget

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

/** A card must always reach `provideContent`: Glance shows its spinner until it does. */
class GuardedLoadTest {
    private val sample = WidgetSamples.model()

    @Test
    fun `a read that works is the card`() = runTest {
        assertThat(guardedLoad(1_000, "test") { sample }).isEqualTo(sample)
    }

    @Test
    fun `a read that fails is a card that says so`() = runTest {
        val model = guardedLoad(1_000, "test") { error("the database is gone") }
        assertThat(model.unavailable).isTrue()
        assertThat(model.day).isNull()
    }

    @Test
    fun `a read that never ends is a card that says so, in time`() = runTest {
        val model = guardedLoad(1_000, "test") { awaitCancellation() }
        assertThat(model.unavailable).isTrue()
    }

    @Test
    fun `cancellation is not swallowed`() {
        assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { guardedLoad(1_000, "test") { throw CancellationException("gone") } }
        }
    }
}
