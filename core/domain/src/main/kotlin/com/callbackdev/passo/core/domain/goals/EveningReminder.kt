package com.callbackdev.passo.core.domain.goals

import com.callbackdev.passo.core.domain.today.TodayOverview

/**
 * What the evening reminder says: the steps still missing and the brisk walk they take.
 *
 * @property minutes minutes of brisk walking (100 steps a minute) that [remaining] take.
 */
data class EveningNudge(val steps: Int, val goalSteps: Int, val remaining: Int, val minutes: Int)

/**
 * The evening reminder (VISION.md, goals: "when today's progress is below a threshold"). It
 * speaks only while the day is below [thresholdPercent] of its goal, so a reader who asked to
 * be reminded only below half of it is not told about the last few hundred steps; a goal met
 * is never reminded, whatever the threshold.
 */
object EveningReminder {
    fun check(steps: Int, goalSteps: Int, thresholdPercent: Int): EveningNudge? {
        if (goalSteps <= 0 || steps >= goalSteps) return null
        // In whole numbers: 5,999 of 8,000 is below 75%, 6,000 is not.
        if (steps.toLong() * PERCENT >= goalSteps.toLong() * thresholdPercent) return null
        val remaining = goalSteps - steps
        return EveningNudge(steps, goalSteps, remaining, TodayOverview.minutesToWalk(remaining))
    }

    private const val PERCENT = 100L
}
