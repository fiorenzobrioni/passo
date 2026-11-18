package com.callbackdev.passo.core.tracking

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.util.Log
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.Executor

/** What the receiver delivers, in the order it delivers it. */
internal sealed interface LocationReading {
    /** One or more fixes, oldest first: one at a time with the screen on, a batch with it off. */
    data class Fixes(val fixes: List<Location>) : LocationReading

    /** Everything the receiver held when [LocationSource.requestFlush] ran has been delivered. */
    data object FlushCompleted : LocationReading

    /** Location was turned off on the phone, or on again, while the receiver was registered. */
    data class Provider(val enabled: Boolean) : LocationReading
}

/**
 * The satellite receiver (docs/adr/0016-location.md, decisions 3 and 10), behind one listener,
 * beside [StepSensorSource] and shaped like it: fixes and flush completions travel through one
 * ordered channel to one consumer, the tracking service.
 *
 * `LocationManager` with `GPS_PROVIDER` only: no Google Play services, no fused provider. A fix
 * every second, at the receiver's best: a walker moves a metre and a half in that time, and a
 * slower rate saves little once the receiver is tracking, while it costs the route its corners.
 *
 * Battery: it is registered only while an outing with GPS counts and the reader walks
 * (`GpsDuty`); the caller removes it at a stop, a pause and the end. With the screen off the
 * fixes are asked for in batches ([SCREEN_OFF_DELAY_MILLIS]) and the step batch, which has
 * already woken the processor, asks for them in its own wake ([requestFlush]): two streams, one
 * rhythm of wakes. Whether the phone batches at all is its hardware's affair, measured and never
 * assumed: a receiver that cannot delivers each fix as it comes.
 */
internal class LocationSource(private val manager: LocationManager, private val executor: Executor) {
    private val channel = Channel<LocationReading>(Channel.UNLIMITED)
    val readings: Flow<LocationReading> = channel.receiveAsFlow()

    /** The delay the receiver is registered with; null while it is not. */
    private var registeredDelayMillis: Long? = null

    val active: Boolean get() = registeredDelayMillis != null

    private val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            channel.trySend(LocationReading.Fixes(listOf(location)))
        }

        override fun onLocationChanged(locations: List<Location>) {
            if (locations.isNotEmpty()) channel.trySend(LocationReading.Fixes(locations))
        }

        override fun onFlushComplete(requestCode: Int) {
            channel.trySend(LocationReading.FlushCompleted)
        }

        // Location turned off on the phone and on again: the registration stays, and the fixes
        // come back by themselves. Told on, so the outing's card can say it while it lasts.
        override fun onProviderEnabled(provider: String) {
            channel.trySend(LocationReading.Provider(enabled = true))
        }

        override fun onProviderDisabled(provider: String) {
            channel.trySend(LocationReading.Provider(enabled = false))
        }
    }

    /**
     * Asks for fixes, replacing any earlier request; [batched] with the screen off. Returns
     * false when the system refuses (the permission taken back, the receiver gone): the outing
     * then goes on from the steps.
     *
     * The permission is the caller's to hold: the service asks only for an outing that started
     * with it granted, and a refusal here is caught rather than assumed away.
     */
    @SuppressLint("MissingPermission")
    fun start(batched: Boolean): Boolean {
        val delay = if (batched) SCREEN_OFF_DELAY_MILLIS else 0L
        if (registeredDelayMillis == delay) return true
        val request = LocationRequest.Builder(INTERVAL_MILLIS)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .setMinUpdateIntervalMillis(INTERVAL_MILLIS)
            .setMaxUpdateDelayMillis(delay)
            .build()
        return try {
            // A second request with the same listener replaces the first: no gap between them.
            manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, request, executor, listener)
            registeredDelayMillis = delay
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "The location updates were refused", e)
            registeredDelayMillis = null
            false
        } catch (e: IllegalArgumentException) {
            // No such provider on this phone, after all.
            Log.w(TAG, "No satellite receiver to ask", e)
            registeredDelayMillis = null
            false
        }
    }

    fun stop() {
        if (registeredDelayMillis == null) return
        manager.removeUpdates(listener)
        registeredDelayMillis = null
    }

    /**
     * Asks the receiver for the fixes it has batched. Returns false when nothing is registered
     * or batched delivery was never asked for, in which case no
     * [LocationReading.FlushCompleted] will come.
     */
    fun requestFlush(): Boolean {
        val delay = registeredDelayMillis ?: return false
        if (delay == 0L) return false
        return try {
            manager.requestFlush(LocationManager.GPS_PROVIDER, listener, FLUSH_REQUEST)
            true
        } catch (e: IllegalArgumentException) {
            // The registration went between the check and the call (the provider turned off).
            false
        }
    }

    fun close() {
        stop()
        channel.close()
    }

    private companion object {
        const val TAG = "LocationSource"

        const val INTERVAL_MILLIS = 1_000L

        /**
         * With the screen off the receiver may hold its fixes this long: twice the step
         * counter's own half minute during an outing, so that by itself it wakes the phone only
         * when no step batch came to ask first.
         */
        const val SCREEN_OFF_DELAY_MILLIS = 60_000L

        const val FLUSH_REQUEST = 1
    }
}
