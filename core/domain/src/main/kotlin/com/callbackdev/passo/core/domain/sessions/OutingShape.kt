package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.routes.RouteShape

/**
 * What an outing's card and page draw of its shape (PLANNING.md §11 Phase 21, step 21.3), beside
 * the outing itself: its minutes, its route so far, and the goal of its day.
 *
 * @property sessionId the outing it is the shape of: a card shows it only on that outing's card,
 *   never for a moment on the next one's.
 * @property minutes its minutes with their gaps; null where its pauses are not known (an outing
 *   from before they were kept), or until they are read.
 * @property route its route so far, for an outing with GPS, once it has a line (two fixes of one
 *   stretch); null before, and for any other outing.
 * @property dayGoalSteps the goal of the day the outing belongs to (its start's, across midnight)
 *   as Today shows it: the share of the day is the outing's steps over it. Null while unknown.
 * @property today that day is today: «of today's goal»; any other, «of the day's goal».
 */
data class OutingShape(
    val sessionId: Long = 0,
    val minutes: OutingMinutes? = null,
    val route: RouteShape? = null,
    val dayGoalSteps: Int? = null,
    val today: Boolean = true,
) {
    /**
     * The share of its day's goal [steps] make, the outing's own and nothing else: a day already
     * at its goal before the walk does not lend it any. Null without a goal to measure against.
     */
    fun dayShare(steps: Int): Double? = dayGoalSteps?.takeIf { it > 0 }?.let { steps.toDouble() / it }
}
