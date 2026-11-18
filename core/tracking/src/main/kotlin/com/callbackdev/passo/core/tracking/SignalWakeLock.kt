package com.callbackdev.passo.core.tracking

import android.os.PowerManager
import android.os.SystemClock
import com.callbackdev.passo.core.domain.sessions.SignalWake

/**
 * The processor kept awake for an outing's signals (docs/adr/0013-interval-walks.md, amended
 * 4 Oct 2026): a partial wake lock, the screen untouched, taken and let go only as
 * [SignalWake.needed] says. Not reference counted: taking it twice is holding it once, and one
 * release lets it go. Bounded whatever happens: by [SignalWake.TIMEOUT_MILLIS] for an outing
 * that starts, and to ten minutes past the outing's own cap once «Keep going» has moved it
 * ([SignalWake.timeoutMillis], [renew]). [log] hears each release, with how long it was held:
 * one row an outing, enough to see it never outlived one.
 *
 * Main thread only, like the tracking service that drives it.
 */
internal class SignalWakeLock(powerManager: PowerManager, private val log: (String) -> Unit) {
    private val lock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG).apply {
        setReferenceCounted(false)
    }
    private var heldSinceElapsed: Long? = null
    private var boundMillis = SignalWake.TIMEOUT_MILLIS

    val held: Boolean get() = lock.isHeld

    /**
     * Takes it or lets it go. Taken, it is bounded by [timeoutMillis] from now: the outing's cap
     * and ten minutes, which the caller reads from the outing.
     */
    fun hold(on: Boolean, timeoutMillis: Long = SignalWake.TIMEOUT_MILLIS) {
        val since = heldSinceElapsed
        if (since != null && !lock.isHeld) {
            // Let go by its own timeout, unnoticed until now: logged as what it was, and taken
            // anew below only if it is still needed.
            heldSinceElapsed = null
            log("timed out after ${boundMillis / MILLIS_PER_SECOND}s")
        }
        if (on && !lock.isHeld) {
            lock.acquire(timeoutMillis)
            boundMillis = timeoutMillis
            heldSinceElapsed = SystemClock.elapsedRealtime()
        } else if (!on && heldSinceElapsed != null) {
            lock.release()
            val seconds = (SystemClock.elapsedRealtime() - (since ?: 0L)) / MILLIS_PER_SECOND
            heldSinceElapsed = null
            log("released after ${seconds}s")
        }
    }

    /**
     * The outing's cap moved («Keep going»): held, the lock is taken again with the new bound,
     * or it would go at the old one in the middle of the walk the reader said they are on. Not
     * held, nothing: a bound is not a reason to take it.
     */
    fun renew(timeoutMillis: Long) {
        if (heldSinceElapsed == null || !lock.isHeld) return
        lock.acquire(timeoutMillis)
        boundMillis = timeoutMillis
    }

    private companion object {
        const val TAG = "Passo:outingSignals"
        const val MILLIS_PER_SECOND = 1_000L
    }
}
