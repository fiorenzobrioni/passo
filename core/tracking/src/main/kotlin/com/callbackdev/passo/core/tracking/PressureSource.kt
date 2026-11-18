package com.callbackdev.passo.core.tracking

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Handler
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** What the pressure sensor delivers, in the order it delivers it. */
internal sealed interface PressureReading {
    /** The air pressure, in hectopascals, at [elapsedNanos] of the phone's own clock. */
    data class Sample(val elapsedNanos: Long, val hpa: Float) : PressureReading

    /** Everything the sensor held when [PressureSource.requestFlush] ran has been delivered. */
    data object FlushCompleted : PressureReading
}

/**
 * The pressure sensor (PLANNING.md §11 Phase 16, «The climb»), behind one listener, beside
 * [StepSensorSource] and [LocationSource] and shaped like them. `TYPE_PRESSURE` asks for no
 * permission: it says how high the air is, not where the phone is.
 *
 * Battery: registered only while the satellite receiver of an outing with GPS is on, and
 * removed with it. The sensor is the phone's usual, non-wake-up one: with the screen off it
 * holds its readings ([SCREEN_OFF_LATENCY_US]) and never wakes the processor by itself; the
 * step batch, which has already woken it, asks for them in its own wake ([requestFlush]). A
 * reading a second is enough for a walker, who climbs a few decimetres in that time.
 */
internal class PressureSource(private val sensorManager: SensorManager, private val handler: Handler) {
    /** The phone's pressure sensor; null on a phone with none, where the satellites are alone. */
    val sensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)

    val available: Boolean get() = sensor != null

    private val channel = Channel<PressureReading>(Channel.UNLIMITED)
    val readings: Flow<PressureReading> = channel.receiveAsFlow()

    /** The latency the sensor is registered with; null while it is not. */
    private var registeredLatencyUs: Int? = null

    val active: Boolean get() = registeredLatencyUs != null

    private val listener = object : SensorEventListener2 {
        override fun onSensorChanged(event: SensorEvent) {
            channel.trySend(PressureReading.Sample(event.timestamp, event.values[0]))
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

        override fun onFlushCompleted(sensor: Sensor) {
            channel.trySend(PressureReading.FlushCompleted)
        }
    }

    /** Asks for readings, replacing any earlier registration; [batched] with the screen off. */
    fun start(batched: Boolean): Boolean {
        val pressure = sensor ?: return false
        val latency = if (batched) SCREEN_OFF_LATENCY_US else 0
        if (registeredLatencyUs == latency) return true
        if (registeredLatencyUs != null) sensorManager.unregisterListener(listener)
        val registered = sensorManager.registerListener(listener, pressure, SAMPLING_PERIOD_US, latency, handler)
        registeredLatencyUs = latency.takeIf { registered }
        return registered
    }

    fun stop() {
        if (registeredLatencyUs == null) return
        sensorManager.unregisterListener(listener)
        registeredLatencyUs = null
    }

    /**
     * Asks the sensor for the readings it holds. Returns false when it is not registered, or
     * holds nothing back, in which case no [PressureReading.FlushCompleted] will come.
     */
    fun requestFlush(): Boolean {
        val latency = registeredLatencyUs ?: return false
        if (latency == 0) return false
        return sensorManager.flush(listener)
    }

    fun close() {
        stop()
        channel.close()
    }

    private companion object {
        const val SAMPLING_PERIOD_US = 1_000_000

        /** With the screen off the sensor may hold its readings a minute, as the receiver its fixes. */
        const val SCREEN_OFF_LATENCY_US = 60 * 1_000_000
    }
}
