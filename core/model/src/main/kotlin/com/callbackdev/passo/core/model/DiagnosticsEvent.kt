package com.callbackdev.passo.core.model

/**
 * A line of the tracking log (PLANNING.md §5, `diagnostics_event`): what the engine did at the
 * moments that matter when steps go missing. Rare by construction, written with the steps.
 */
data class DiagnosticsEvent(val wallMillis: Long, val type: DiagnosticsType, val detail: String)

enum class DiagnosticsType {
    /** The service started; the detail names the sensor, its FIFO and its wake-up mode. */
    SERVICE_START,

    /** The very first sample: the counter's value becomes the baseline, nothing is counted. */
    BASELINE,

    /** A new boot session: the counter restarted from zero. */
    BOOT,

    /** The counter went backwards without a reboot (a sensor or HAL reset). */
    COUNTER_RESET,

    /** The shutdown flush ran; the detail says whether the sensor answered in time. */
    SHUTDOWN_FLUSH,

    /** An implausible jump, capped (PLANNING.md §4.4). */
    ANOMALY,

    /** A sensor timestamp outside the plausible window; the arrival time was used instead. */
    TIMESTAMP_FALLBACK,

    /** The wall clock or the time zone changed. */
    TIME_CHANGED,
}
