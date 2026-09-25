package com.callbackdev.passo.core.domain.goals

/**
 * "Goal reached", once a day (PLANNING.md §8, §11 Phase 6).
 *
 * The tracking service sees the count cross the goal; whether that is news depends on the last
 * day already told, which is stored. Days only move forward here: a day at or before the last
 * one told is never told again, so a reboot (the service reads the goal met again from the
 * first sample), a time-zone change that brings back yesterday's date, or a lower goal later
 * the same day cannot make a second notification. A day seen reached with the notification
 * off is recorded all the same, so turning it on that evening does not announce the morning.
 */
object GoalReached {
    /** Whether [steps] on [epochDay] is a goal reached that nobody has been told about yet. */
    fun isNews(epochDay: Long, steps: Int, goalSteps: Int, lastToldDay: Long?): Boolean =
        steps >= goalSteps && isLaterDay(epochDay, lastToldDay)

    /** Whether [epochDay] comes after the last day told: the rule that keeps it once a day. */
    fun isLaterDay(epochDay: Long, lastToldDay: Long?): Boolean = lastToldDay == null || epochDay > lastToldDay
}
