package com.callbackdev.passo.feature.insights

import com.callbackdev.passo.core.domain.insights.Insights
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.UnitPreference
import java.time.DayOfWeek
import java.time.LocalDate

/** Made-up days for Insights' tests and screenshots. */
internal object InsightsSamples {
    val today: LocalDate = LocalDate.of(2026, 9, 24)

    private fun day(date: LocalDate, steps: Int, goal: Int = 8_000) = DailySummary(
        localEpochDay = date.toEpochDay(),
        steps = steps,
        distanceMeters = steps * 0.74,
        activeKcal = steps * 0.037,
        activeMinutes = steps / 180,
        briskMinutes = steps / 450,
        goalSteps = goal,
        finalized = date.isBefore(today),
    )

    /** Sixty days: mostly around the goal, a streak running into today, one big Sunday. */
    fun days(): Map<Long, DailySummary> = (0..60).associate { back ->
        val date = today.minusDays(back.toLong())
        val steps = when {
            back == 0 -> 5_300
            back in 1..4 -> 8_400 + back * 310
            back == 11 -> 16_920
            back % 3 == 0 || back == 5 -> 6_100 + back * 20
            else -> 8_900 + (back * 137) % 1_500
        }
        date.toEpochDay() to day(date, steps)
    }

    fun state(days: Map<Long, DailySummary> = days()): InsightsUiState {
        val insights = Insights.of(days, today, DayOfWeek.MONDAY)
        return InsightsUiState(
            today = today,
            insights = insights,
            lastDays = (6 downTo 0).map { back ->
                val date = today.minusDays(back.toLong())
                DayMark(
                    date = date,
                    counted = insights.firstDay?.let { !date.isBefore(it) } == true,
                    met = days[date.toEpochDay()]?.goalReached == true,
                    today = back == 0,
                )
            },
            goalSteps = 8_000,
            units = UnitPreference.METRIC,
            firstDayOfWeek = DayOfWeek.MONDAY,
        )
    }
}
