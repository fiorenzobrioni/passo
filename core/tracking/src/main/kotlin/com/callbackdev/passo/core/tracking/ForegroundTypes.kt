package com.callbackdev.passo.core.tracking

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.content.pm.ServiceInfo
import android.util.Log

/**
 * The types the tracking service is in the foreground with: `health` always, and `location`
 * while an outing measured with GPS counts (docs/adr/0016-location.md, decision 4). The one
 * place a `startForeground()` is decided, so that no start of the service takes the second type
 * away by passing the first alone, and a refused second type never stops the count.
 *
 * Apart from the service so that what it does when the system says no can be tested without a
 * phone: what a phone answers to a second type on a running service is the very thing Phase 15
 * proves on a device.
 *
 * @param start the service's own `startForeground()`, with the types to pass.
 * @param onLocationType the `location` type was newly added, or refused ([Throwable] says by
 *   what): for the outing's log row, and for the receiver, which cannot stay on without it.
 */
internal class ForegroundTypes(
    private val start: (notification: Notification, types: Int) -> Unit,
    private val onLocationType: (added: Boolean, refusal: Throwable?) -> Unit,
) {
    /**
     * Whether the service holds its `location` type now, in this process: asked for when an
     * outing with GPS starts or resumes, dropped at its pause or end. A restarted process comes
     * back without it, and cannot take it by itself.
     */
    var locationHeld: Boolean = false
        private set

    /**
     * Puts the service in the foreground as `health`, and as `location` too when [withLocation]
     * asks. Returns false only when `health` itself was refused, and the service cannot run.
     *
     * A refused `location` never stops the count (no lost steps): it is told on, and the
     * service stays `health`, the outing going on from the steps. Any failure is caught there,
     * not only the documented ones.
     */
    fun enter(notification: Notification, withLocation: Boolean): Boolean {
        if (withLocation) {
            try {
                start(
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
                val added = !locationHeld
                locationHeld = true
                if (added) onLocationType(true, null)
                return true
            } catch (e: RuntimeException) {
                Log.w(TAG, "The location service type was refused", e)
                locationHeld = false
                onLocationType(false, e)
            }
        }
        locationHeld = false
        return try {
            start(notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "The health service type was refused", e)
            false
        } catch (e: ForegroundServiceStartNotAllowedException) {
            Log.w(TAG, "Foreground start not allowed from here", e)
            false
        }
    }

    private companion object {
        const val TAG = "StepTracking"
    }
}
