package com.callbackdev.passo.core.domain.tracking

import com.callbackdev.passo.core.model.DiagnosticsEvent
import com.callbackdev.passo.core.model.DiagnosticsType
import com.callbackdev.passo.core.model.StepSample
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The in-memory buffer and its write triggers (PLANNING.md §4.5). */
class StepLedgerTest {
    private val device = Device(bootWall = wallOf("2026-09-24T06:00:00"))
    private val start = wallOf("2026-09-24T10:00:05")

    private fun StepLedger.recordAt(counter: Long, wall: Long): Boolean =
        record(device.sample(counter, wall), device.snapshotAt(wall))

    @Test
    fun `the first baseline is written at once`() {
        val ledger = StepLedger(initialState = null)

        assertThat(ledger.recordAt(1_000, start)).isTrue()
        val batch = ledger.drain()

        assertThat(batch?.increments).isEmpty()
        assertThat(batch?.state?.lastCounterValue).isEqualTo(1_000)
        assertThat(batch?.diagnostics?.map { it.type }).containsExactly(DiagnosticsType.BASELINE)
    }

    @Test
    fun `a few steps inside the same minute stay in memory`() {
        val ledger = StepLedger(device.stateAt(1_000, start))

        assertThat(ledger.recordAt(1_010, start + 10 * SECOND)).isFalse()
        assertThat(ledger.recordAt(1_030, start + 30 * SECOND)).isFalse()

        assertThat(ledger.pendingSteps).isEqualTo(30)
    }

    @Test
    fun `crossing a minute boundary asks for a write`() {
        val ledger = StepLedger(device.stateAt(1_000, start))
        ledger.recordAt(1_010, start + 10 * SECOND)

        assertThat(ledger.recordAt(1_050, start + 60 * SECOND)).isTrue()
    }

    @Test
    fun `a hundred buffered steps ask for a write`() {
        val ledger = StepLedger(device.stateAt(1_000, start))

        assertThat(ledger.recordAt(1_099, start + 40 * SECOND)).isFalse()
        assertThat(ledger.recordAt(1_100, start + 45 * SECOND)).isTrue()
    }

    @Test
    fun `a new boot session is written at once and logged`() {
        val ledger = StepLedger(device.stateAt(5_000, start))
        val nextBoot = Device(bootWall = start + 3_600 * SECOND, bootCount = 2)
        val at = nextBoot.bootWall + 30 * SECOND

        assertThat(ledger.record(nextBoot.sample(12, at), nextBoot.snapshotAt(at))).isTrue()
        assertThat(ledger.drain()?.diagnostics?.map { it.type }).containsExactly(DiagnosticsType.BOOT)
    }

    @Test
    fun `an anomaly is written at once and logged`() {
        val ledger = StepLedger(device.stateAt(1_000, start))

        assertThat(ledger.recordAt(900_000, start + 10 * SECOND)).isTrue()
        assertThat(ledger.drain()?.diagnostics?.map { it.type }).containsExactly(DiagnosticsType.ANOMALY)
    }

    @Test
    fun `drain hands out everything once and empties the buffer`() {
        val ledger = StepLedger(device.stateAt(1_000, start))
        ledger.recordAt(1_040, start + 20 * SECOND)
        ledger.note(DiagnosticsEvent(start, DiagnosticsType.SERVICE_START, "test"))

        val batch = ledger.drain()

        assertThat(batch?.increments?.sumOf { it.steps }).isEqualTo(40)
        assertThat(batch?.state?.lastCounterValue).isEqualTo(1_040)
        assertThat(batch?.diagnostics).hasSize(1)
        assertThat(ledger.pendingSteps).isEqualTo(0)
        assertThat(ledger.drain()).isNull()
    }

    @Test
    fun `a batch that failed to write is restored without losing or doubling steps`() {
        val ledger = StepLedger(device.stateAt(1_000, start))
        ledger.recordAt(1_040, start + 20 * SECOND)
        val failed = checkNotNull(ledger.drain())

        // More steps arrive while the failed write was in flight.
        ledger.recordAt(1_070, start + 40 * SECOND)
        ledger.restore(failed)
        val retry = checkNotNull(ledger.drain())

        assertThat(retry.increments.sumOf { it.steps }).isEqualTo(70)
        assertThat(retry.state?.lastCounterValue).isEqualTo(1_070)
    }

    @Test
    fun `pending steps are reported per local day`() {
        val lateEvening = wallOf("2026-09-24T23:59:30")
        val ledger = StepLedger(device.stateAt(0, lateEvening))
        ledger.recordAt(30, lateEvening + 10 * SECOND)
        ledger.recordAt(50, lateEvening + 40 * SECOND)

        assertThat(ledger.pendingStepsOn(dayOf("2026-09-24T00:00:00"))).isEqualTo(30)
        assertThat(ledger.pendingStepsOn(dayOf("2026-09-25T00:00:00"))).isEqualTo(20)
    }

    @Test
    fun `a broken timebase is logged once, not on every event`() {
        val ledger = StepLedger(device.stateAt(0, start))
        repeat(3) { i ->
            val at = start + (i + 1) * 10 * SECOND
            val broken = StepSample(counterValue = (i + 1) * 5L, eventElapsedNanos = Long.MAX_VALUE)
            ledger.record(broken, device.snapshotAt(at))
        }

        assertThat(ledger.drain()?.diagnostics?.map { it.type }).containsExactly(DiagnosticsType.TIMESTAMP_FALLBACK)
    }
}
