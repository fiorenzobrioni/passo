package com.callbackdev.passo.core.tracking

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import android.os.Handler
import com.callbackdev.passo.core.domain.tracking.stepSampleOf
import com.callbackdev.passo.core.model.StepSample
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** What the sensor delivers, in the order it delivers it. */
internal sealed interface SensorReading {
    data class Sample(val sample: StepSample) : SensorReading

    /** Everything that was in the FIFO when [StepSensorSource.requestFlush] ran is delivered. */
    data object FlushCompleted : SensorReading
}

/**
 * The hardware step counter (PLANNING.md §4.3), behind one listener.
 *
 * Samples and flush completions travel through the same ordered channel, so whoever consumes
 * [readings] has processed every sample of a flush by the time it sees [SensorReading.FlushCompleted].
 * One consumer only: the tracking service.
 *
 * The sensor is the non-wake-up one (docs/adr/0002-sensor-reporting.md): it never wakes the
 * processor, and a FIFO overflow can only coarsen the minute attribution, since the counter is
 * cumulative and the latest value is always kept. During an outing, and only then, the wake-up
 * one is used where the phone has it (docs/adr/0009-sessions.md), so a signal reaches a phone in
 * a pocket on time: the two report the same counter, so switching loses and doubles nothing.
 */
internal class StepSensorSource(private val sensorManager: SensorManager, private val handler: Handler) {
    val sensor: Sensor? = sensorManager.defaultStepCounter()

    /** The wake-up step counter, when the phone has one besides [sensor]. */
    val wakeUpSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)
        ?.takeIf { it != sensor }

    private val channel = Channel<SensorReading>(Channel.UNLIMITED)
    val readings: Flow<SensorReading> = channel.receiveAsFlow()

    private var registeredLatencyUs: Int? = null
    private var registeredSensor: Sensor? = null

    private val listener = object : SensorEventListener2 {
        override fun onSensorChanged(event: SensorEvent) {
            stepSampleOf(event.values[0], event.timestamp)?.let { channel.trySend(SensorReading.Sample(it)) }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

        override fun onFlushCompleted(sensor: Sensor) {
            channel.trySend(SensorReading.FlushCompleted)
        }
    }

    /**
     * Registers with [maxReportLatencyUs], replacing any earlier registration; the wake-up
     * counter when [wakeUp] is asked and the phone has one, the usual one otherwise.
     */
    fun register(maxReportLatencyUs: Int, wakeUp: Boolean = false): Boolean {
        val chosen = (if (wakeUp) wakeUpSensor else null) ?: sensor ?: return false
        if (registeredLatencyUs == maxReportLatencyUs && registeredSensor == chosen) return true
        if (registeredLatencyUs != null) sensorManager.unregisterListener(listener)
        val registered = sensorManager.registerListener(
            listener,
            chosen,
            SensorManager.SENSOR_DELAY_NORMAL,
            maxReportLatencyUs,
            handler,
        )
        registeredLatencyUs = maxReportLatencyUs.takeIf { registered }
        registeredSensor = chosen.takeIf { registered }
        return registered
    }

    fun unregister() {
        if (registeredLatencyUs == null) return
        sensorManager.unregisterListener(listener)
        registeredLatencyUs = null
        registeredSensor = null
    }

    /**
     * Asks the hardware to deliver what it has batched. Returns false if nothing is registered
     * or the request was refused, in which case no [SensorReading.FlushCompleted] will come.
     */
    fun requestFlush(): Boolean = registeredLatencyUs != null && sensorManager.flush(listener)

    fun close() {
        unregister()
        channel.close()
    }

    /** One line for the log: which sensor this device gave us, and how much it can batch. */
    fun describe(): String = sensor?.let {
        "sensor=${it.name} vendor=${it.vendor} version=${it.version} wakeUp=${it.isWakeUpSensor} " +
            "fifoMax=${it.fifoMaxEventCount} fifoReserved=${it.fifoReservedEventCount} " +
            "wakeUpVariant=${wakeUpSensor != null}"
    } ?: "sensor=none"
}

/**
 * The non-wake-up step counter, which is what `getDefaultSensor` returns for this type on a
 * conforming device. A device that only has a wake-up one still gets it rather than nothing:
 * it costs a wakeup per report window, and the log line records which one was used.
 */
internal fun SensorManager.defaultStepCounter(): Sensor? = getDefaultSensor(Sensor.TYPE_STEP_COUNTER, false)
    ?: getDefaultSensor(Sensor.TYPE_STEP_COUNTER, true)
