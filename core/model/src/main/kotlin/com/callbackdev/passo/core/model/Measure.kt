package com.callbackdev.passo.core.model

/**
 * A number already formatted for the reader's locale, with the unit it is in. The unit's
 * symbol or word is a string resource, applied by the UI: this type carries no text of its own
 * but the digits.
 */
data class Measure(val number: String, val unit: MeasureUnit)

enum class MeasureUnit {
    KILOMETER,
    MILE,

    /** A short distance, whole: a measured stretch walked to calibrate the step (Phase 7). */
    METER,
    YARD,
    KILOCALORIE,
    KILOGRAM,
    POUND,
    CENTIMETER,
    FOOT,
    INCH,
    STEPS_PER_MINUTE,
    MINUTE,
}
