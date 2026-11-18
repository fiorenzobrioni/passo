package com.callbackdev.passo.core.tracking

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** An outing with GPS in one log row (PLANNING.md §11 Phase 15): what a field test reads. */
class GpsLogTest {
    private val log = GpsLog()

    @Test
    fun `an outing that never asked for the receiver logs nothing`() {
        assertThat(log.used).isFalse()
    }

    @Test
    fun `the row counts the fixes, the good ones, and the ones that came with the screen off`() {
        log.onType(added = true)
        log.onAsked(elapsedMillis = 1_000, granted = true)
        log.onFixes(listOf(9_000L), listOf(12f), screenOn = true)
        log.onFixes(listOf(10_000L, 11_000L, 12_000L), listOf(8f, 40f, 25f), screenOn = false)

        val row = log.drain(wakeUpCounter = true)

        assertThat(row).contains("fixes=4 good=3 taken=0 refused=0 screenOff=3")
        assertThat(row).contains("batches=2 largestBatch=3")
        assertThat(row).contains("firstFix=8000ms")
        assertThat(row).contains("typeAdded=1 typeRefused=0")
        assertThat(row).contains("wakeUp=true")
    }

    @Test
    fun `the longest gap is between two fixes of one stretch, never across a sleep`() {
        log.onType(added = true)
        log.onAsked(0, granted = true)
        log.onFixes(listOf(1_000L, 2_000L, 5_000L), listOf(5f, 5f, 5f), screenOn = false)
        // Asleep for ten minutes at a bench: not a delay of the phone's.
        log.onStopped(asleep = true)
        log.onAsked(600_000, granted = true)
        log.onFixes(listOf(601_000L, 602_000L), listOf(5f, 5f), screenOn = false)

        val row = log.drain(wakeUpCounter = false)

        assertThat(row).contains("longestGap=3000ms")
        assertThat(row).contains("sleeps=1")
        assertThat(row).contains("wakeUp=false")
    }

    @Test
    fun `a refused type is counted, with what refused it`() {
        log.onType(added = false, refusal = SecurityException("not from here"))

        assertThat(log.used).isTrue()
        val row = log.drain(wakeUpCounter = true)
        assertThat(row).contains("typeAdded=0 typeRefused=1")
        assertThat(row).contains("refusal=SecurityException")
        assertThat(row).contains("firstFix=none")
    }

    @Test
    fun `refused updates are counted apart from a refused type`() {
        log.onType(added = true)
        log.onAsked(0, granted = false)

        assertThat(log.drain(wakeUpCounter = true)).contains("updatesRefused=1")
    }

    @Test
    fun `the row holds counts and delays only, never a place`() {
        log.onType(added = true)
        log.onAsked(0, granted = true)
        log.onFixes(listOf(1_000L), listOf(5f), screenOn = true)

        val row = log.drain(wakeUpCounter = true)

        // This log is in the database Android's backup carries: no name a coordinate would be
        // written under, and no number with a coordinate's decimals.
        assertThat(row).doesNotContainMatch("(lat|lon|latitude|longitude|alt)=|[0-9]+\\.[0-9]{4,}")
    }

    @Test
    fun `the row says what the route was built from, and how much of the distance was measured`() {
        log.onType(added = true)

        val row = log.drain(wakeUpCounter = true, taken = 1_780, refused = 22, measuredShare = 0.964)

        assertThat(row).contains("taken=1780 refused=22")
        assertThat(row).contains("measured=96%")
    }

    @Test
    fun `drained, it starts clean for the next outing`() {
        log.onType(added = true)
        log.onAsked(0, granted = true)
        log.onFixes(listOf(1_000L), listOf(5f), screenOn = true)
        log.drain(wakeUpCounter = true)

        assertThat(log.used).isFalse()
        log.onType(added = true)
        assertThat(log.drain(wakeUpCounter = true)).contains("fixes=0 good=0 taken=0 refused=0 screenOff=0")
    }
}
