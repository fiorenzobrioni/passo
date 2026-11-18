package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.routes.RouteConstants.HOLE_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionState
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * What an outing's card says of the satellites (PLANNING.md §11 Phase 15, «What the reader sees
 * while it counts»): every state of the receiver has its line, and one only.
 */
class GpsLinesTest {
    private val plan = SessionPlans.NEW.copy(gps = true)
    private val measured = checkNotNull(SessionPlans.start(plan, 0, 0, 0, 8_000, withGps = true))
    private val onSteps = checkNotNull(SessionPlans.start(plan, 0, 0, 0, 8_000, withGps = false))

    private fun receiver(
        typeHeld: Boolean = true,
        readiness: GpsReadiness = GpsReadiness.READY,
        duty: GpsDuty.State = GpsDuty.State.AWAKE,
        awakeSinceMillis: Long? = 0,
        lastFixAtMillis: Long? = null,
        settledThroughMillis: Long? = null,
    ) = ReceiverState(typeHeld, readiness, duty, awakeSinceMillis, lastFixAtMillis, settledThroughMillis)

    @Test
    fun `finding the satellites until the first fix`() {
        assertThat(GpsLines.of(measured, receiver())).isEqualTo(GpsLine.FINDING)
        // However long it takes, and however far the steps have gone meanwhile.
        assertThat(GpsLines.of(measured, receiver(settledThroughMillis = 10 * MINUTE))).isEqualTo(GpsLine.FINDING)
    }

    @Test
    fun `measuring says nothing of its own - the distance says measured`() {
        val walking = receiver(lastFixAtMillis = 5 * MINUTE, settledThroughMillis = 5 * MINUTE + 1_000)

        assertThat(GpsLines.of(measured, walking)).isNull()
    }

    @Test
    fun `asleep while the reader is still - nothing said, the numbers stand`() {
        val asleep = receiver(
            duty = GpsDuty.State.ASLEEP,
            awakeSinceMillis = null,
            lastFixAtMillis = 5 * MINUTE,
            settledThroughMillis = 5 * MINUTE,
        )

        assertThat(GpsLines.of(measured, asleep)).isNull()
    }

    @Test
    fun `asleep before any fix - still nothing said`() {
        assertThat(GpsLines.of(measured, receiver(duty = GpsDuty.State.ASLEEP, awakeSinceMillis = null))).isNull()
    }

    @Test
    fun `the signal lost - a tunnel, steps coming and no fix for a quarter of a minute`() {
        val fix = 5 * MINUTE

        assertThat(GpsLines.of(measured, receiver(lastFixAtMillis = fix, settledThroughMillis = fix + HOLE_MILLIS)))
            .isNull()
        assertThat(GpsLines.of(measured, receiver(lastFixAtMillis = fix, settledThroughMillis = fix + HOLE_MILLIS + 1)))
            .isEqualTo(GpsLine.SIGNAL_LOST)
    }

    @Test
    fun `fixes the receiver still holds are not a tunnel`() {
        // The screen off: the last fix in hand is half a minute old, and so is the last batch
        // of steps settled. Nothing is known of the time since, so nothing is said of it.
        val batched = receiver(lastFixAtMillis = 5 * MINUTE, settledThroughMillis = 5 * MINUTE)

        assertThat(GpsLines.of(measured, batched)).isNull()
    }

    @Test
    fun `waking after a stop is not a tunnel either`() {
        // Ten minutes on a bench: the last fix is old, and the receiver has just been asked
        // again. The seconds it takes to find its place are counted from when it woke.
        val woke = 15 * MINUTE
        val waking =
            receiver(awakeSinceMillis = woke, lastFixAtMillis = 5 * MINUTE, settledThroughMillis = woke + 5_000)
        val stillNone = waking.copy(settledThroughMillis = woke + HOLE_MILLIS + 1)

        assertThat(GpsLines.of(measured, waking)).isNull()
        assertThat(GpsLines.of(measured, stillNone)).isEqualTo(GpsLine.SIGNAL_LOST)
    }

    @Test
    fun `location turned off on the phone, mid-outing - said, with the system's switch`() {
        val off = receiver(readiness = GpsReadiness.LOCATION_OFF, lastFixAtMillis = MINUTE)

        assertThat(GpsLines.of(measured, off)).isEqualTo(GpsLine.LOCATION_OFF)
        assertThat(GpsLine.LOCATION_OFF.action).isEqualTo(GpsLineAction.LOCATION_SETTINGS)
        // Whether or not the service still holds its type: «Resume GPS» would do nothing there.
        assertThat(GpsLines.of(measured, off.copy(typeHeld = false))).isEqualTo(GpsLine.LOCATION_OFF)
    }

    @Test
    fun `the process restarted by the system - GPS stopped, with Resume GPS`() {
        val restarted = receiver(typeHeld = false, duty = GpsDuty.State.AWAKE, awakeSinceMillis = null)

        assertThat(GpsLines.of(measured, restarted)).isEqualTo(GpsLine.STOPPED)
        assertThat(GpsLine.STOPPED.action).isEqualTo(GpsLineAction.RESUME_GPS)
    }

    @Test
    fun `the permission revoked mid-outing - never a button that would be refused`() {
        val gone = receiver(typeHeld = false, readiness = GpsReadiness.PERMISSION_NEEDED, awakeSinceMillis = null)
        val approximate = gone.copy(readiness = GpsReadiness.APPROXIMATE_ONLY)

        assertThat(GpsLines.of(measured, gone)).isEqualTo(GpsLine.PERMISSION_GONE)
        assertThat(GpsLines.of(measured, approximate)).isEqualTo(GpsLine.PERMISSION_GONE)
        assertThat(GpsLine.PERMISSION_GONE.action).isEqualTo(GpsLineAction.ALLOW_LOCATION)
    }

    @Test
    fun `paused or over, the receiver is off by the reader's word - no line`() {
        val lost = receiver(typeHeld = false, awakeSinceMillis = null)
        val paused = measured.copy(state = SessionState.PAUSED, pausedAtMillis = MINUTE)
        val over = measured.copy(state = SessionState.FINISHED, end = SessionEnd.STOPPED, endedAtMillis = MINUTE)

        assertThat(GpsLines.of(paused, lost)).isNull()
        assertThat(GpsLines.of(over, lost)).isNull()
    }

    @Test
    fun `an outing without GPS has no receiver to speak of`() {
        assertThat(
            GpsLines.of(onSteps, receiver(typeHeld = false, readiness = GpsReadiness.PERMISSION_NEEDED)),
        ).isNull()
    }

    @Test
    fun `started on the steps - approximate only, refused, location off - each with its way out`() {
        assertThat(GpsLines.onSteps(onSteps, GpsReadiness.PERMISSION_NEEDED)).isEqualTo(GpsLine.ON_STEPS_NO_PERMISSION)
        assertThat(GpsLines.onSteps(onSteps, GpsReadiness.APPROXIMATE_ONLY)).isEqualTo(GpsLine.ON_STEPS_APPROXIMATE)
        assertThat(GpsLines.onSteps(onSteps, GpsReadiness.LOCATION_OFF)).isEqualTo(GpsLine.ON_STEPS_LOCATION_OFF)
        assertThat(GpsLine.ON_STEPS_NO_PERMISSION.action).isEqualTo(GpsLineAction.ALLOW_LOCATION)
        assertThat(GpsLine.ON_STEPS_APPROXIMATE.action).isEqualTo(GpsLineAction.ALLOW_LOCATION)
        assertThat(GpsLine.ON_STEPS_LOCATION_OFF.action).isEqualTo(GpsLineAction.LOCATION_SETTINGS)
    }

    @Test
    fun `started on the steps - nothing said with nothing in the way, or on a phone with no receiver`() {
        assertThat(GpsLines.onSteps(onSteps, null)).isNull()
        assertThat(GpsLines.onSteps(onSteps, GpsReadiness.READY)).isNull()
        assertThat(GpsLines.onSteps(onSteps, GpsReadiness.NO_RECEIVER)).isNull()
    }

    @Test
    fun `started on the steps - said while it is under way or paused, not once it is over`() {
        val paused = onSteps.copy(state = SessionState.PAUSED, pausedAtMillis = MINUTE)
        val over = onSteps.copy(state = SessionState.FINISHED, end = SessionEnd.STOPPED, endedAtMillis = MINUTE)

        assertThat(GpsLines.onSteps(paused, GpsReadiness.PERMISSION_NEEDED)).isEqualTo(GpsLine.ON_STEPS_NO_PERMISSION)
        assertThat(GpsLines.onSteps(over, GpsReadiness.PERMISSION_NEEDED)).isNull()
        // An outing that did start with GPS is not one that started on the steps.
        assertThat(GpsLines.onSteps(measured, GpsReadiness.PERMISSION_NEEDED)).isNull()
    }

    @Test
    fun `a refusal is told once, location off each time it happens`() {
        assertThat(GpsLines.toldOnce(GpsReadiness.PERMISSION_NEEDED)).isTrue()
        assertThat(GpsLines.toldOnce(GpsReadiness.APPROXIMATE_ONLY)).isTrue()
        assertThat(GpsLines.toldOnce(GpsReadiness.LOCATION_OFF)).isFalse()
    }

    private companion object {
        const val MINUTE = 60_000L
    }
}
