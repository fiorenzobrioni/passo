package com.callbackdev.passo.core.data.widget

import com.callbackdev.passo.core.domain.widget.WidgetEvent

/**
 * Where the tracking service and the app tell the home-screen widgets that something they draw
 * may have changed (PLANNING.md §7). Declared here, below both, so the service never depends on
 * the widget module: `:widget` binds the implementation, which decides with
 * [com.callbackdev.passo.core.domain.widget.WidgetUpdatePolicy] whether to repaint now, later
 * or not at all. Calls are cheap and never block: the caller may be the sensor's thread.
 */
interface WidgetUpdates {
    /** @param steps today's live count, for [WidgetEvent.STEPS]; ignored otherwise. */
    fun notify(event: WidgetEvent, steps: Int = 0)
}
