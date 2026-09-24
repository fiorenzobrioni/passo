package com.callbackdev.passo.core.domain.tracking

import com.callbackdev.passo.core.model.StepSample
import com.callbackdev.passo.core.model.SystemSnapshot
import com.callbackdev.passo.core.model.TrackerState
import java.time.LocalDateTime
import java.time.ZoneId

internal val Rome: ZoneId = ZoneId.of("Europe/Rome")
internal val NewYork: ZoneId = ZoneId.of("America/New_York")

internal const val MINUTE = 60_000L
internal const val SECOND = 1_000L

internal fun wallOf(text: String, zone: ZoneId = Rome): Long =
    LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

internal fun dayOf(text: String): Long = LocalDateTime.parse(text).toLocalDate().toEpochDay()

internal fun minuteOf(wallMillis: Long): Long = Math.floorDiv(wallMillis, MINUTE)

internal fun Long.msToNanos(): Long = this * 1_000_000L

/**
 * A device whose boot happened at [bootWall] on the wall clock. Elapsed time is derived from
 * the wall clock, so a test reads in local times; [clockShiftMillis] moves the wall clock
 * without moving the elapsed one, as a manual clock change does.
 */
internal data class Device(
    val bootWall: Long,
    val bootCount: Int = 1,
    val zone: ZoneId = Rome,
    val clockShiftMillis: Long = 0L,
) {
    fun elapsedAt(trueWall: Long): Long = (trueWall - bootWall).msToNanos()

    fun snapshotAt(trueWall: Long): SystemSnapshot = SystemSnapshot(
        bootCount = bootCount,
        elapsedRealtimeNanos = elapsedAt(trueWall),
        wallClockMillis = trueWall + clockShiftMillis,
        zoneId = zone,
    )

    fun sample(counter: Long, trueWall: Long): StepSample =
        StepSample(counterValue = counter, eventElapsedNanos = elapsedAt(trueWall))

    fun stateAt(counter: Long, trueWall: Long): TrackerState = TrackerState(
        bootCount = bootCount,
        lastCounterValue = counter,
        lastSampleElapsedNanos = elapsedAt(trueWall),
        lastSampleWallMillis = trueWall + clockShiftMillis,
    )
}
