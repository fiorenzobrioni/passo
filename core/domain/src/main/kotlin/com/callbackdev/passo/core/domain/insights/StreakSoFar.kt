package com.callbackdev.passo.core.domain.insights

import com.callbackdev.passo.core.model.DailySummary
import java.time.LocalDate

/**
 * The streak Today shows beside its date, split where its two halves change at different rates:
 * the days up to yesterday are frozen and change only at midnight, today joins once its goal is
 * met. The same streak as [Insights.currentStreak]: every day measured against its own goal, a
 * day with nothing recorded is a day the goal was not met, and today not yet at the goal has not
 * broken it, only not joined it.
 */
object StreakSoFar {
    /** The days in a row with the goal met that end yesterday; 0 when yesterday missed it. */
    fun endingYesterday(days: List<DailySummary>, today: LocalDate): Int {
        val met = days.asSequence().filter { it.goalReached }.map { it.localEpochDay }.toHashSet()
        var count = 0
        var cursor = today.toEpochDay() - 1
        while (cursor in met) {
            count++
            cursor--
        }
        return count
    }

    /** The streak with today in it once [todayMet]. */
    fun withToday(endingYesterday: Int, todayMet: Boolean): Int = if (todayMet) endingYesterday + 1 else endingYesterday

    /**
     * Shown from two days: one day at the goal is the goal reached, which the ring and the
     * sentence already say.
     */
    const val SHOWN_FROM_DAYS: Int = 2
}
