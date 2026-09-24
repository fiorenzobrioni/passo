package com.callbackdev.passo.core.tracking

import android.content.ContentResolver
import android.os.SystemClock
import android.provider.Settings
import com.callbackdev.passo.core.model.SystemSnapshot
import java.time.ZoneId

/** Reads the clocks, the time zone and the boot count the accounting needs, all at once. */
internal class SystemSnapshots(private val contentResolver: ContentResolver) {
    fun current(): SystemSnapshot = SystemSnapshot(
        bootCount = Settings.Global.getInt(
            contentResolver,
            Settings.Global.BOOT_COUNT,
            SystemSnapshot.UNKNOWN_BOOT_COUNT,
        ),
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos(),
        wallClockMillis = System.currentTimeMillis(),
        // Re-read on every call: the framework replaces the default zone when it changes.
        zoneId = ZoneId.systemDefault(),
    )
}
