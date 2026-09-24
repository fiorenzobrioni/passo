package com.callbackdev.passo.core.model

import java.time.ZoneId

/**
 * The clocks and the boot session, read together when a sample is processed. Injected rather
 * than read inside the accounting, so every edge case of PLANNING.md §4.6 is a plain unit test.
 *
 * @property bootCount `Settings.Global.BOOT_COUNT`, or [UNKNOWN_BOOT_COUNT] if the device does
 *   not provide it (a reboot is then still recognized by the elapsed clock going backwards).
 */
data class SystemSnapshot(
    val bootCount: Int,
    val elapsedRealtimeNanos: Long,
    val wallClockMillis: Long,
    val zoneId: ZoneId,
) {
    companion object {
        const val UNKNOWN_BOOT_COUNT = -1
    }
}
