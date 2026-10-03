package com.callbackdev.passo.core.domain.tracking

import com.callbackdev.passo.core.model.StepSample
import com.callbackdev.passo.core.model.SystemSnapshot
import com.callbackdev.passo.core.model.TrackerState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.ZoneOffset
import java.util.Random

/** Every edge case of PLANNING.md §4.6, plus the attribution rules of §4.4. */
class StepAccountantTest {
    private val boot = wallOf("2026-09-24T06:00:00")
    private val device = Device(bootWall = boot)

    // --- First install mid-day -------------------------------------------------------------

    @Test
    fun `first sample ever is the baseline and counts nothing`() {
        val at = wallOf("2026-09-24T14:00:00")

        val result = StepAccountant.account(null, device.sample(5_432, at), device.snapshotAt(at))

        assertThat(result.change).isEqualTo(SessionChange.FIRST_RUN)
        assertThat(result.increments).isEmpty()
        assertThat(result.acceptedSteps).isEqualTo(0)
        assertThat(result.newState).isEqualTo(device.stateAt(5_432, at))
    }

    @Test
    fun `steps after the baseline are counted from it`() {
        val first = wallOf("2026-09-24T14:00:00")
        val second = first + 30 * SECOND
        val baseline = StepAccountant.account(null, device.sample(5_432, first), device.snapshotAt(first)).newState

        val result = StepAccountant.account(baseline, device.sample(5_470, second), device.snapshotAt(second))

        assertThat(result.acceptedSteps).isEqualTo(38)
        assertThat(result.increments.single().epochMinute).isEqualTo(minuteOf(second))
    }

    // --- Attribution ---------------------------------------------------------------------------

    @Test
    fun `a step reported alone falls in its own minute`() {
        val last = wallOf("2026-09-24T10:00:59.500")
        val at = wallOf("2026-09-24T10:01:00.050")

        val result = StepAccountant.account(
            device.stateAt(1_000, last),
            device.sample(1_001, at),
            device.snapshotAt(at),
        )

        assertThat(result.increments.single().epochMinute).isEqualTo(minuteOf(at))
    }

    @Test
    fun `a short gap is spread over the time its steps took, minute by minute`() {
        val last = wallOf("2026-09-24T10:00:10")
        val at = last + 90 * SECOND

        val result = StepAccountant.account(
            device.stateAt(1_000, last),
            device.sample(1_150, at),
            device.snapshotAt(at),
        )

        // 150 steps take 81.8 s at 110 a minute: from 10:00:18 to 10:01:40, 41.8 s and 40 s.
        assertThat(result.increments.map { it.epochMinute to it.steps }).containsExactly(
            minuteOf(at) - 1 to 76,
            minuteOf(at) to 74,
        ).inOrder()
        assertThat(result.increments.last().localEpochDay).isEqualTo(dayOf("2026-09-24T10:01:40"))
    }

    @Test
    fun `a sensor that hands over walking in clumps makes no minute of running`() {
        // A phone with no hardware FIFO, at 100 steps a minute: every minute and a half or so
        // the processor wakes and the counter's latest value arrives, a clump of steps at once.
        var state = device.stateAt(0, wallOf("2026-09-24T07:40:00"))
        var counter = 0L
        val perMinute = HashMap<Long, Int>()
        for (gapSeconds in listOf(95, 80, 110, 70, 100, 85, 90, 105)) {
            val at = state.lastSampleWallMillis + gapSeconds * SECOND
            counter += gapSeconds * 100L / 60
            val result = StepAccountant.account(state, device.sample(counter, at), device.snapshotAt(at))
            result.increments.forEach { perMinute.merge(it.epochMinute, it.steps, Int::plus) }
            state = result.newState
        }
        // Every whole minute walked reads about as it was walked; none reaches a run (140).
        val whole = perMinute.toSortedMap().values.drop(1).dropLast(1)
        assertThat(whole.max()).isAtMost(TrackingConstants.DEFAULT_CADENCE)
        assertThat(whole.min()).isAtLeast(80)
        assertThat(perMinute.values.sum().toLong()).isEqualTo(counter)
    }

    @Test
    fun `a batched event lands at its own timestamp, not at its arrival`() {
        val last = wallOf("2026-09-24T10:00:00")
        val happened = wallOf("2026-09-24T10:01:30")
        val delivered = wallOf("2026-09-24T10:09:30")

        val result = StepAccountant.account(
            device.stateAt(1_000, last),
            device.sample(1_080, happened),
            device.snapshotAt(delivered),
        )

        assertThat(result.increments.last().epochMinute).isEqualTo(minuteOf(happened))
        assertThat(result.increments.map { it.epochMinute }).doesNotContain(minuteOf(delivered))
        assertThat(result.newState.lastSampleWallMillis).isEqualTo(happened)
        assertThat(result.usedArrivalTime).isFalse()
    }

    @Test
    fun `a long gap is back-filled from the event at the default cadence`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = wallOf("2026-09-24T10:10:30")

        val result = StepAccountant.account(
            device.stateAt(1_000, last),
            device.sample(1_300, at),
            device.snapshotAt(at),
        )

        // 300 steps at 110 a minute: the 2 min 43.6 s before 10:10:30.
        assertThat(result.increments.map { it.epochMinute to it.steps }).containsExactly(
            minuteOf(at) - 3 to 25,
            minuteOf(at) - 2 to 110,
            minuteOf(at) - 1 to 110,
            minuteOf(at) to 55,
        ).inOrder()
    }

    @Test
    fun `what does not fit at the default cadence is spread evenly over the gap`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = wallOf("2026-09-24T10:10:00")

        val result = StepAccountant.account(device.stateAt(0, last), device.sample(2_000, at), device.snapshotAt(at))

        // The ten minutes from 10:00 to 10:10, at the gap's own average of 200.
        assertThat(result.increments).hasSize(10)
        assertThat(result.increments.sumOf { it.steps }).isEqualTo(2_000)
        assertThat(result.increments.first().epochMinute).isEqualTo(minuteOf(last))
        assertThat(result.increments.last().epochMinute).isEqualTo(minuteOf(at) - 1)
        assertThat(result.increments.map { it.steps }.toSet()).containsExactly(200)
        assertThat(result.cappedFromSteps).isNull()
    }

    @Test
    fun `back-fill never reaches before the previous sample`() {
        val last = wallOf("2026-09-24T10:00:30")
        val at = wallOf("2026-09-24T10:03:00")

        val result = StepAccountant.account(device.stateAt(0, last), device.sample(400, at), device.snapshotAt(at))

        assertThat(result.increments.first().epochMinute).isAtLeast(minuteOf(last))
        assertThat(result.increments.sumOf { it.steps }).isEqualTo(400)
    }

    @Test
    fun `no movement produces no increments but advances the state`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = wallOf("2026-09-24T10:20:00")

        val result = StepAccountant.account(device.stateAt(700, last), device.sample(700, at), device.snapshotAt(at))

        assertThat(result.increments).isEmpty()
        assertThat(result.newState.lastSampleWallMillis).isEqualTo(at)
    }

    // --- Nightly shutdown, abrupt power loss ---------------------------------------------------

    @Test
    fun `after a reboot the whole counter belongs to the new session`() {
        val beforeShutdown = device.stateAt(9_870, wallOf("2026-09-23T23:30:00"))
        val nextBoot = Device(bootWall = wallOf("2026-09-24T07:00:00"), bootCount = 2)
        val at = wallOf("2026-09-24T07:05:00")

        val result = StepAccountant.account(beforeShutdown, nextBoot.sample(120, at), nextBoot.snapshotAt(at))

        assertThat(result.change).isEqualTo(SessionChange.NEW_BOOT)
        assertThat(result.acceptedSteps).isEqualTo(120)
        // Five minutes since boot: back-filled, and never before the boot itself.
        assertThat(result.increments.map { it.steps }).containsExactly(10, 110).inOrder()
        assertThat(result.increments.first().epochMinute).isAtLeast(minuteOf(nextBoot.bootWall))
        assertThat(result.newState.bootCount).isEqualTo(2)
    }

    @Test
    fun `a reboot is recognized by the elapsed clock when the boot count is unknown`() {
        val unknown = SystemSnapshot.UNKNOWN_BOOT_COUNT
        val old = Device(bootWall = wallOf("2026-09-20T08:00:00"), bootCount = unknown)
        val new = Device(bootWall = wallOf("2026-09-24T08:00:00"), bootCount = unknown)
        val state = old.stateAt(20_000, wallOf("2026-09-23T22:00:00"))
        val at = wallOf("2026-09-24T08:01:00")

        val result = StepAccountant.account(state, new.sample(60, at), new.snapshotAt(at))

        assertThat(result.change).isEqualTo(SessionChange.NEW_BOOT)
        assertThat(result.acceptedSteps).isEqualTo(60)
    }

    @Test
    fun `an abrupt power loss loses only what was never persisted`() {
        // Persisted at 18:00 with 4 000; the phone walked to 4 500, then died without a flush.
        val persisted = device.stateAt(4_000, wallOf("2026-09-24T18:00:00"))
        val nextBoot = Device(bootWall = wallOf("2026-09-24T19:00:00"), bootCount = 2)
        val at = wallOf("2026-09-24T19:30:00")

        val result = StepAccountant.account(persisted, nextBoot.sample(200, at), nextBoot.snapshotAt(at))

        assertThat(result.acceptedSteps).isEqualTo(200)
    }

    // --- Sensor resets and jumps ---------------------------------------------------------------

    @Test
    fun `a counter that goes backwards without a reboot restarts from zero`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = last + 40 * SECOND

        val result = StepAccountant.account(device.stateAt(8_000, last), device.sample(25, at), device.snapshotAt(at))

        assertThat(result.change).isEqualTo(SessionChange.COUNTER_RESET)
        assertThat(result.acceptedSteps).isEqualTo(25)
        assertThat(result.newState.lastCounterValue).isEqualTo(25)
    }

    @Test
    fun `an implausible jump is capped at the maximum cadence and reported`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = last + MINUTE

        val result = StepAccountant.account(
            device.stateAt(1_000, last),
            device.sample(1_001_000, at),
            device.snapshotAt(at),
        )

        assertThat(
            result.acceptedSteps,
        ).isEqualTo(TrackingConstants.MAX_CADENCE + TrackingConstants.JUMP_SLACK_STEPS.toLong())
        assertThat(result.cappedFromSteps).isEqualTo(1_000_000)
        assertThat(result.increments.sumOf { it.steps }.toLong()).isEqualTo(result.acceptedSteps)
        // The jump is dropped, not carried into the next delta.
        assertThat(result.newState.lastCounterValue).isEqualTo(1_001_000)
    }

    @Test
    fun `a fast but human pace is never capped`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = wallOf("2026-09-24T10:30:00")

        // 30 minutes of running at 180 spm, delivered in one batch.
        val result = StepAccountant.account(device.stateAt(0, last), device.sample(5_400, at), device.snapshotAt(at))

        assertThat(result.cappedFromSteps).isNull()
        assertThat(result.acceptedSteps).isEqualTo(5_400)
    }

    // --- Timestamps ----------------------------------------------------------------------------

    @Test
    fun `a timestamp in the future falls back to the arrival time`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = last + 30 * SECOND
        val future = StepSample(counterValue = 1_020, eventElapsedNanos = device.elapsedAt(at + 3_600 * SECOND))

        val result = StepAccountant.account(device.stateAt(1_000, last), future, device.snapshotAt(at))

        assertThat(result.usedArrivalTime).isTrue()
        assertThat(result.newState.lastSampleWallMillis).isEqualTo(at)
        assertThat(result.increments.single().epochMinute).isEqualTo(minuteOf(at))
    }

    @Test
    fun `a timestamp before the previous sample falls back to the arrival time`() {
        val last = wallOf("2026-09-24T10:00:00")
        val at = last + 30 * SECOND
        val stale = device.sample(1_020, last - 10 * MINUTE)

        val result = StepAccountant.account(device.stateAt(1_000, last), stale, device.snapshotAt(at))

        assertThat(result.usedArrivalTime).isTrue()
        assertThat(result.acceptedSteps).isEqualTo(20)
    }

    // --- Midnight ------------------------------------------------------------------------------

    @Test
    fun `steps just after midnight belong to the new day`() {
        val last = wallOf("2026-09-24T23:59:40")
        val at = wallOf("2026-09-25T00:00:20")

        // 20 steps take 11 s: walked after midnight.
        val result = StepAccountant.account(device.stateAt(0, last), device.sample(20, at), device.snapshotAt(at))

        assertThat(result.increments.single().localEpochDay).isEqualTo(dayOf("2026-09-25T00:00:20"))
    }

    @Test
    fun `a walk across midnight is split at it, as it was walked`() {
        val last = wallOf("2026-09-24T23:59:40")
        val at = wallOf("2026-09-25T00:00:20")

        // 60 steps take 32.7 s: 12.7 s of them before midnight.
        val result = StepAccountant.account(device.stateAt(0, last), device.sample(60, at), device.snapshotAt(at))

        assertThat(result.increments.map { it.localEpochDay to it.steps }).containsExactly(
            dayOf("2026-09-24T23:59:50") to 23,
            dayOf("2026-09-25T00:00:20") to 37,
        ).inOrder()
    }

    @Test
    fun `a long gap across midnight is split between the two days`() {
        val last = wallOf("2026-09-24T23:55:00")
        val at = wallOf("2026-09-25T00:05:00")

        val result = StepAccountant.account(device.stateAt(0, last), device.sample(1_000, at), device.snapshotAt(at))

        val byDay = result.increments.groupBy { it.localEpochDay }.mapValues { (_, v) -> v.sumOf { it.steps } }
        // 1,000 steps take 9 min 5 s at 110 a minute: 4 min 5 s of them before midnight.
        assertThat(byDay).containsExactly(
            dayOf("2026-09-24T23:55:00"),
            450,
            dayOf("2026-09-25T00:05:00"),
            550,
        )
    }

    // --- Time zone and clock changes -----------------------------------------------------------

    @Test
    fun `the local day is computed in the time zone in effect when the sample is accounted`() {
        // 23:30 UTC: already tomorrow in Rome, still today in New York.
        val at = wallOf("2026-09-24T23:30:00", zone = ZoneOffset.UTC)
        val last = at - 30 * SECOND
        val inRome = Device(bootWall = boot, zone = Rome)
        val inNewYork = Device(bootWall = boot, zone = NewYork)

        val rome = StepAccountant.account(inRome.stateAt(0, last), inRome.sample(10, at), inRome.snapshotAt(at))
        val newYork = StepAccountant.account(
            inNewYork.stateAt(0, last),
            inNewYork.sample(10, at),
            inNewYork.snapshotAt(at),
        )

        assertThat(rome.increments.single().epochMinute).isEqualTo(newYork.increments.single().epochMinute)
        assertThat(rome.increments.single().localEpochDay).isEqualTo(dayOf("2026-09-25T01:30:00"))
        assertThat(newYork.increments.single().localEpochDay).isEqualTo(dayOf("2026-09-24T19:30:00"))
    }

    @Test
    fun `setting the clock back does not cap real steps`() {
        val last = wallOf("2026-09-24T10:00:00")
        val state = device.stateAt(1_000, last)
        val movedBack = device.copy(clockShiftMillis = -3_600 * SECOND)
        val at = last + 50 * SECOND

        val result = StepAccountant.account(state, movedBack.sample(1_090, at), movedBack.snapshotAt(at))

        assertThat(result.acceptedSteps).isEqualTo(90)
        assertThat(result.cappedFromSteps).isNull()
        assertThat(result.increments.single().epochMinute).isEqualTo(minuteOf(at - 3_600 * SECOND))
    }

    @Test
    fun `setting the clock forward does not smear steps over the jump`() {
        val last = wallOf("2026-09-24T10:00:00")
        val state = device.stateAt(1_000, last)
        val movedForward = device.copy(clockShiftMillis = 24 * 3_600 * SECOND)
        val at = last + 50 * SECOND

        val result = StepAccountant.account(state, movedForward.sample(1_090, at), movedForward.snapshotAt(at))

        assertThat(result.increments).hasSize(1)
        assertThat(result.increments.single().steps).isEqualTo(90)
    }

    // --- Conservation --------------------------------------------------------------------------

    @Test
    fun `the increments always add up to the counter's progress`() {
        var state: TrackerState? = null
        var wall = wallOf("2026-09-24T08:00:00")
        var counter = 3_000L
        var baseline = -1L
        var total = 0L
        val random = Random(42)
        repeat(2_000) {
            wall += (1 + random.nextInt(900)) * SECOND
            counter += random.nextInt(200)
            val result = StepAccountant.account(state, device.sample(counter, wall), device.snapshotAt(wall))
            if (baseline < 0) baseline = counter
            assertThat(result.cappedFromSteps).isNull()
            total += result.increments.sumOf { it.steps }
            state = result.newState
        }
        assertThat(total).isEqualTo(counter - baseline)
    }

    // --- Float precision -----------------------------------------------------------------------

    @Test
    fun `the float counter converts exactly up to two to the twenty-fourth`() {
        assertThat(stepSampleOf(16_777_216f, 5L)?.counterValue).isEqualTo(16_777_216L)
        assertThat(stepSampleOf(12_345f, 5L)?.counterValue).isEqualTo(12_345L)
        assertThat(stepSampleOf(0f, 5L)?.eventElapsedNanos).isEqualTo(5L)
    }

    @Test
    fun `a reading that cannot be a count is ignored`() {
        assertThat(stepSampleOf(Float.NaN, 0L)).isNull()
        assertThat(stepSampleOf(-3f, 0L)).isNull()
        assertThat(stepSampleOf(Float.POSITIVE_INFINITY, 0L)).isNull()
    }
}
