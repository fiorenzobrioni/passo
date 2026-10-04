package com.callbackdev.passo.core.tracking

import android.os.PowerManager
import android.os.SystemClock
import com.callbackdev.passo.core.domain.sessions.SignalWake

/**
 * The processor kept awake for an outing's signals (docs/adr/0013-interval-walks.md, amended
 * 4 Oct 2026): a partial wake lock, the screen untouched, taken and let go only as
 * [SignalWake.needed] says. Not reference counted: taking it twice is holding it once, and one
 * release lets it go. Bounded by [SignalWake.TIMEOUT_MILLIS] whatever happens. [log] hears every
 * take and release.
 *
 * Main thread only, like the tracking service that drives it.
 */
internal class SignalWakeLock(powerManager: PowerManager, private val log: (String) -> Unit) {
    private val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG).apply {
        setReferenceCounted(false)
    }
    private var heldSinceElapsed: Long? = null

    val held: Boolean get() = lock.isHeld

    fun hold(on: Boolean) {
        if (on && !lock.isHeld) {
            lock.acquire(SignalWake.TIMEOUT_MILLIS)
            heldSinceElapsed = SystemClock.elapsedRealtime()
            log("held")
        } else if (!on && heldSinceElapsed != null) {
            if (lock.isHeld) lock.release()
            val seconds = (SystemClock.elapsedRealtime() - (heldSinceElapsed ?: 0L)) / MILLIS_PER_SECOND
            heldSinceElapsed = null
            log("released after ${seconds}s")
        }
    }

    private companion object {
        const val TAG = "Passo:outingSignals"
        const val MILLIS_PER_SECOND = 1_000L
    }
}
