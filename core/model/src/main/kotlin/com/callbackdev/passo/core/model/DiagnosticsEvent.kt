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

    /**
     * A tracker state restored from another installation (a backup) was dropped: the next
     * sample is a baseline.
     */
    RESTORED,

    /**
     * An interval outing's changes, in one row when it ends (Phase 13): how many were told, how
     * late the median and the latest one were against the step that crossed each (negative when
     * told ahead of it), and the goal's, so a field test reads the delay instead of guessing it
     * (docs/adr/0013-interval-walks.md). Until 5 Oct 2026, one row a change.
     */
    INTERVAL_CHANGE,

    /**
     * The processor kept awake for an outing's signals, on a phone whose step counter cannot wake
     * it (docs/adr/0013-interval-walks.md): when it was let go, and how long it was held, so an
     * export shows it never outlives the outing. Until 5 Oct 2026, also a row when it was taken.
     */
    SIGNAL_WAKE,
}
