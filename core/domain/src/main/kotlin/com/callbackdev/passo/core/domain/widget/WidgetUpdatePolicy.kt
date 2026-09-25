package com.callbackdev.passo.core.domain.widget

/** What can make a home-screen widget worth repainting (PLANNING.md §7, update strategy). */
enum class WidgetEvent {
    /** The screen came on, or the phone was unlocked: the sensor has just been flushed. */
    SCREEN_ON,

    /** The screen went off: nothing is repainted until it comes back. */
    SCREEN_OFF,

    /** The count moved. Arrives with every sensor event while the screen is on. */
    STEPS,

    /** The local date changed (the screen-on ticker, a clock or time-zone change). */
    DAY_CHANGED,

    /** Counting started or stopped, or the permission changed. */
    TRACKING_STATE,

    /** A setting the widgets draw with (goal, units, appearance), or one widget's own look. */
    SETTINGS,
}

/** The answer for one event. */
sealed interface WidgetDecision {
    /** Repaint now. */
    data object Now : WidgetDecision

    /** Repaint in [delayMillis], unless a repaint is already waiting: the trailing edge of the throttle. */
    data class Later(val delayMillis: Long) : WidgetDecision

    /** Nothing to do. */
    data object Skip : WidgetDecision
}

/**
 * When the widgets repaint (PLANNING.md §7, §9): as soon as the screen comes on, at most every
 * [intervalMillis] while it stays on and only if the count moved, at once for a new day, a goal
 * just reached, a setting or a change in tracking, and never while the screen is off.
 *
 * Two events repaint with the screen off too, because waiting would leave the widget wrong for
 * good: a change in tracking (a stopped service is not there to repaint at the next screen-on,
 * so its widget must say it stopped on its way out) and a setting (which only changes with
 * someone in the app anyway). Both are rare, one repaint each, and wake nothing: a repaint is
 * work done where the event already woke the processor.
 *
 * Pure and single-threaded: the caller serializes the calls and owns the timer a [WidgetDecision.Later]
 * asks for, and calls [pushed] once a repaint has really gone out.
 */
class WidgetUpdatePolicy(private val intervalMillis: Long = MIN_INTERVAL_MILLIS, interactive: Boolean = true) {
    /** Whether someone may be looking. Starts from what the phone said when the policy was made. */
    var interactive: Boolean = interactive
        private set

    private var lastPushElapsed: Long? = null
    private var lastPushedSteps: Int? = null

    /**
     * @param nowElapsed the monotonic clock, in milliseconds.
     * @param steps today's count as it stands now; only [WidgetEvent.STEPS] reads it.
     * @param goalSteps today's goal, to repaint at once the moment it is reached.
     */
    fun decide(event: WidgetEvent, nowElapsed: Long, steps: Int = 0, goalSteps: Int = Int.MAX_VALUE): WidgetDecision =
        when (event) {
            WidgetEvent.SCREEN_OFF -> {
                interactive = false
                WidgetDecision.Skip
            }

            WidgetEvent.SCREEN_ON -> {
                interactive = true
                WidgetDecision.Now
            }

            WidgetEvent.TRACKING_STATE, WidgetEvent.SETTINGS -> WidgetDecision.Now

            WidgetEvent.DAY_CHANGED -> if (interactive) WidgetDecision.Now else WidgetDecision.Skip

            WidgetEvent.STEPS -> stepsMoved(nowElapsed, steps, goalSteps)
        }

    private fun stepsMoved(nowElapsed: Long, steps: Int, goalSteps: Int): WidgetDecision {
        val shown = lastPushedSteps
        val last = lastPushElapsed
        return when {
            !interactive || steps == shown -> WidgetDecision.Skip
            shown != null && shown < goalSteps && steps >= goalSteps -> WidgetDecision.Now
            last == null || nowElapsed - last >= intervalMillis -> WidgetDecision.Now
            else -> WidgetDecision.Later(intervalMillis - (nowElapsed - last))
        }
    }

    /** A repaint went out at [nowElapsed] showing [steps]. */
    fun pushed(nowElapsed: Long, steps: Int) {
        lastPushElapsed = nowElapsed
        lastPushedSteps = steps
    }

    companion object {
        /** "Never more than about 60 seconds stale while the screen is on" (VISION.md, success criteria). */
        const val MIN_INTERVAL_MILLIS: Long = 60_000L
    }
}
