package com.callbackdev.passo.core.domain.tracking

/** The tracking engine's constants (PLANNING.md §4.4, §4.5), in one place. */
object TrackingConstants {
    /**
     * A sample at most this far from the previous one puts all its steps in its own minute.
     * Beyond it, the steps are spread backwards over the gap: a delta that arrives after ten
     * minutes of batching was not all walked in the last one.
     */
    const val SHORT_GAP_MILLIS: Long = 2 * 60_000L

    /**
     * The cadence a long gap is back-filled at, from the sample's minute backwards. About
     * 110 steps per minute is a typical free-walking cadence in adults (Tudor-Locke et al.,
     * "How fast is fast enough?", Br J Sports Med 2020: 100 spm is the moderate-intensity
     * threshold, habitual walking sits a little above it).
     */
    const val DEFAULT_CADENCE: Int = 110

    /**
     * No human sustains more than this for a whole minute: elite sprinters touch about 250 to
     * 280 spm for seconds, distance runners stay under 200. A delta above
     * `MAX_CADENCE * gapMinutes + JUMP_SLACK_STEPS` is a sensor fault, capped and logged.
     */
    const val MAX_CADENCE: Int = 250

    /** Headroom on the cap, so a handful of steps over a very short gap is never cut. */
    const val JUMP_SLACK_STEPS: Int = 50

    /** Buffered steps that force a write even inside the same minute (PLANNING.md §4.5). */
    const val FLUSH_STEP_THRESHOLD: Int = 100

    /** Rows kept in the diagnostics log, oldest dropped first. */
    const val DIAGNOSTICS_LOG_SIZE: Int = 500
}
