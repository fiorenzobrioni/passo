package com.callbackdev.passo.core.tracking

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

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

    /**
     * Whether an outing's signals can wake a phone with the screen off: a wake-up step counter
     * besides the usual one (docs/adr/0009-sessions.md). Without it they arrive when the phone
     * next wakes for another reason.
     */
    fun hasWakeUpStepCounter(context: Context): Boolean {
        val manager = context.getSystemService(SensorManager::class.java) ?: return false
        val wakeUp = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)
        return wakeUp != null && wakeUp != manager.defaultStepCounter()
    }

    fun hasActivityRecognition(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
            PackageManager.PERMISSION_GRANTED

    /** Stops tracking at the reader's request ("Pause tracking"); the service writes its buffer first. */
    fun stop(context: Context) {
        context.stopService(Intent(context, StepTrackingService::class.java))
    }

    /**
     * Starts tracking from a boot or update broadcast, unless the reader paused it: a pause is
     * a choice, and a reboot must not undo it. The setting is read off the main thread while
     * the broadcast is held open with `goAsync`, which keeps the start inside the broadcast's
     * exemption from the background-start restriction.
     */
    fun startFromBroadcast(receiver: BroadcastReceiver, context: Context) {
        val pending = receiver.goAsync()
        val app = context.applicationContext
        BroadcastWork.launch {
            try {
                val preferences = EntryPointAccessors.fromApplication(app, TrackingEntryPoint::class.java).preferences()
                if (preferences.current().settings.trackingEnabled) start(app)
            } finally {
                pending.finish()
            }
        }
    }

    private val BroadcastWork = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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

/** What the receivers need from the graph; they are not Hilt entry points themselves. */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface TrackingEntryPoint {
    fun preferences(): UserPreferencesDataSource

    fun goals(): GoalNotifier
}
