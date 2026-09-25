package com.callbackdev.passo.core.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import com.callbackdev.passo.core.domain.tracking.stepSampleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/** What the step counter says to the calibration: its value, or that a flush has been delivered. */
sealed interface ProbeReading {
    /** The hardware counter: steps since the phone started, cumulative. */
    data class Count(val value: Long) : ProbeReading

    /** Everything the sensor held when [StepCounterProbe.flush] was asked has arrived. */
    data object Flushed : ProbeReading
}

/**
 * The hardware step counter read directly, for the step calibration (PLANNING.md §11 Phase 7):
 * its value at Start and at Stop is all a known distance needs. The counter is cumulative, so
 * the difference is exact however the steps were delivered in between, and it does not depend
 * on the tracking service, a pause, or midnight.
 *
 * The listener exists only while [readings] is collected, which the calibration page does only
 * while it is on screen: registered with no batching (the screen is on, the processor awake),
 * the same non-wake-up sensor the service uses, so it never wakes the phone (PLANNING.md §9).
 * Android delivers an on-change sensor's current value when a listener registers, so the page
 * has its starting point before the first step.
 */
@Singleton
class StepCounterProbe
@Inject
constructor(@ApplicationContext context: Context) {
    private val manager: SensorManager? = context.getSystemService(SensorManager::class.java)

    @Volatile private var listener: SensorEventListener2? = null

    /** Whether the phone has a step counter at all (without one the app never gets this far). */
    val available: Boolean get() = manager?.defaultStepCounter() != null

    fun readings(): Flow<ProbeReading> = callbackFlow {
        val sensor: Sensor? = manager?.defaultStepCounter()
        if (manager == null || sensor == null) {
            close()
            return@callbackFlow
        }
        val callback = object : SensorEventListener2 {
            override fun onSensorChanged(event: SensorEvent) {
                stepSampleOf(event.values[0], event.timestamp)?.let { trySend(ProbeReading.Count(it.counterValue)) }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

            override fun onFlushCompleted(sensor: Sensor) {
                trySend(ProbeReading.Flushed)
            }
        }
        manager.registerListener(callback, sensor, SensorManager.SENSOR_DELAY_NORMAL, 0)
        listener = callback
        // Some counters only answer a registration once asked: a flush delivers what they hold.
        manager.flush(callback)
        awaitClose {
            manager.unregisterListener(callback)
            if (listener === callback) listener = null
        }
    }

    /**
     * Asks the sensor for what it still holds (a phone in a pocket batches in its hub); a
     * [ProbeReading.Flushed] follows on [readings]. False when nothing is listening.
     */
    fun flush(): Boolean {
        val current = listener ?: return false
        return manager?.flush(current) == true
    }
}
