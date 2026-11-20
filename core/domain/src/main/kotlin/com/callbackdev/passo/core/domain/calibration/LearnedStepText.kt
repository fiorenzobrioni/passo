package com.callbackdev.passo.core.domain.calibration

import com.callbackdev.passo.core.model.LearnedStep
import com.callbackdev.passo.core.model.StepLengthMode
import com.callbackdev.passo.core.model.StepPoint

/**
 * A learnt step as one line of text, for the settings file (DataStore keeps strings, not
 * objects): `1;usual;metres;outings;through;replaced;running;cadence:metres,…`. Read leniently:
 * a line that cannot be read is no learnt step, never a failure, and a newer build's extra fields
 * at the end are skipped.
 */
object LearnedStepText {
    private const val VERSION = "1"
    private const val FIELDS = 8

    fun encode(step: LearnedStep): String = listOf(
        VERSION,
        step.usualCadence.toString(),
        step.learntMeters.toString(),
        step.outings.toString(),
        step.throughMillis.toString(),
        step.replaced.name,
        if (step.running) "1" else "0",
        step.points.joinToString(",") { "${it.cadence}:${it.meters}" },
    ).joinToString(";")

    fun decode(text: String?): LearnedStep? {
        val fields = text?.split(';') ?: return null
        if (fields.size < FIELDS || fields[0] != VERSION) return null
        val points = fields[7].split(',').filter { it.isNotEmpty() }.map { point ->
            val (cadence, meters) = point.split(':').takeIf { it.size == 2 } ?: return null
            StepPoint(cadence.toDoubleOrNull() ?: return null, meters.toDoubleOrNull() ?: return null)
        }
        return LearnedStep(
            points = points,
            usualCadence = fields[1].toIntOrNull() ?: return null,
            learntMeters = fields[2].toDoubleOrNull() ?: return null,
            outings = fields[3].toIntOrNull() ?: return null,
            throughMillis = fields[4].toLongOrNull() ?: return null,
            replaced = StepLengthMode.entries.firstOrNull { it.name == fields[5] } ?: StepLengthMode.AUTO,
            running = fields[6] == "1",
        )
    }
}
