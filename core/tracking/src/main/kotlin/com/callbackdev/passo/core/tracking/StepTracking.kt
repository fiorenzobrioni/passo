package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat

/** Whether tracking can run on this device, right now. */
enum class TrackingReadiness {
    READY,

    /** `ACTIVITY_RECOGNITION` is not granted: the service may not start (PLANNING.md §4.6). */
    PERMISSION_NEEDED,

    /** The device has no hardware step counter: nothing can be counted, ever (§4.6). */
    NO_SENSOR,
}

/** The one way into the tracking service, for the app, the boot receivers and later the UI. */
object StepTracking {
    private const val TAG = "StepTracking"

    fun readiness(context: Context): TrackingReadiness = when {
        !hasStepCounter(context) -> TrackingReadiness.NO_SENSOR
        !hasActivityRecognition(context) -> TrackingReadiness.PERMISSION_NEEDED
        else -> TrackingReadiness.READY
    }

    fun hasStepCounter(context: Context): Boolean =
        context.getSystemService(SensorManager::class.java)?.defaultStepCounter() != null

    fun hasActivityRecognition(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Starts (or re-confirms) the tracking service if it can run. The check comes first on
     * purpose: a service started with `startForegroundService` must call `startForeground`,
     * and without the permission the `health` type would refuse it.
     *
     * Callers must be allowed to start a foreground service: the app in the foreground, or one
     * of the exempt broadcasts (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`). The `health` type is
     * not among those Android 15 forbids from `BOOT_COMPLETED`.
     */
    fun start(context: Context): Boolean {
        if (readiness(context) != TrackingReadiness.READY) return false
        return try {
            ContextCompat.startForegroundService(context, Intent(context, StepTrackingService::class.java))
            true
        } catch (e: ForegroundServiceStartNotAllowedException) {
            // Only reachable from a context that is not exempt; the next app open retries.
            Log.w(TAG, "Foreground service start not allowed", e)
            false
        }
    }
}
