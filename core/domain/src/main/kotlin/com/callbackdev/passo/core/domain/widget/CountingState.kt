package com.callbackdev.passo.core.domain.widget

/**
 * Whether today's count on a home-screen widget is live, and if not, what the reader can do
 * about it (PLANNING.md §7: the paused and "permission needed" states). A widget that kept
 * showing a number as if it were still moving would be the screen lying.
 */
enum class CountingState {
    /** The service is running: the number moves. */
    COUNTING,

    /** The reader paused counting in the app. Tap to resume. */
    PAUSED,

    /** "Physical activity" is not granted. Tap to open the app and allow it. */
    PERMISSION_NEEDED,

    /**
     * Counting is on and allowed, yet the service is not running: the system or a task killer
     * stopped it. Nothing is lost unless the phone restarts before it runs again (the counter is
     * cumulative, PLANNING.md §4.2); opening the app starts it.
     */
    STOPPED,

    /** The first run has not been through yet: nothing has ever been counted. */
    NOT_SET_UP,

    /** This phone has no step counter: nothing can ever be counted. */
    NO_SENSOR,
    ;

    /** Whether there is a day's count to draw at all; otherwise the card is one message. */
    val hasCount: Boolean get() = this != NOT_SET_UP && this != NO_SENSOR

    companion object {
        /** The first reason that applies, in the order the reader has to deal with them. */
        fun of(
            hasSensor: Boolean,
            onboarded: Boolean,
            hasPermission: Boolean,
            enabled: Boolean,
            serviceRunning: Boolean,
        ): CountingState = when {
            !hasSensor -> NO_SENSOR
            !onboarded -> NOT_SET_UP
            !hasPermission -> PERMISSION_NEEDED
            !enabled -> PAUSED
            !serviceRunning -> STOPPED
            else -> COUNTING
        }
    }
}
