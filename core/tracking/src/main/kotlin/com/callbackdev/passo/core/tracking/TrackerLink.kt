package com.callbackdev.passo.core.tracking

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The running tracking service, as the rest of the process can reach it: today only to catch
 * up with the sensor before a reminder reads the count. With the screen off the sensor hub
 * holds up to ten minutes of steps (PLANNING.md §4.3), and a reminder at 20:00 that forgot the
 * walk just finished would say the wrong thing at the worst moment.
 */
@Singleton
class TrackerLink
@Inject
constructor() {
    @Volatile private var catchUp: (suspend () -> Unit)? = null

    /** Set by the service while it runs, cleared when it stops. */
    internal fun attach(catchUp: (suspend () -> Unit)?) {
        this.catchUp = catchUp
    }

    /**
     * Asks the service, if it runs, to take in what the sensor holds; returns once it has (the
     * service bounds the wait itself, as for the shutdown flush). A no-op without a service.
     */
    suspend fun catchUp() {
        catchUp?.invoke()
    }
}
