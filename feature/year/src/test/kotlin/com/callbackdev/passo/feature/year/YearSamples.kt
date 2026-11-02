package com.callbackdev.passo.feature.year

import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.domain.year.YearInReview
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import java.time.DayOfWeek
import java.time.LocalDate

/** A realistic 2025, made up: weekdays and weekends, a summer, a way, a few outings and cities. */
internal object YearSamples {
    val today: LocalDate = LocalDate.of(2026, 1, 12)

    private fun steps(date: LocalDate): Int {
        val base = when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> 11_200
            DayOfWeek.SUNDAY -> 9_600
            else -> 7_400
        }
        val season = when (date.monthValue) {
            6, 7, 8 -> 1_300
            12, 1, 2 -> -900
            else -> 0
        }
        // A spread that repeats, so the days are not all alike.
        val spread = ((date.dayOfYear * 37) % 11 - 5) * 260
        return if (date == LocalDate.of(2025, 9, 13)) 23_480 else (base + season + spread).coerceAtLeast(1_200)
    }

    fun days(from: LocalDate = LocalDate.of(2024, 3, 1), to: LocalDate = today): Map<Long, DailySummary> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.associate { date ->
            val count = steps(date)
            date.toEpochDay() to DailySummary(date.toEpochDay(), count, count * 0.74, count * 0.041, 0, 0, 8_000, true)
        }

    private val hours = List(24) { hour ->
        when (hour) {
            in 0..6 -> 2_000L
            8 -> 260_000L
            12, 13 -> 300_000L
            18 -> 410_000L
            in 7..21 -> 150_000L
            else -> 30_000L
        }
    }

    private fun outings(): List<com.callbackdev.passo.core.model.Session> {
        val brisk = SessionPlans.PRESETS.first()
        return (1..23).mapNotNull { n ->
            val day = LocalDate.of(2025, 3, 1).plusDays(n * 12L)
            SessionPlans.start(brisk, 0, day.toEpochDay(), 0, 8_000)
                ?.copy(
                    state = SessionState.FINISHED,
                    totals = SessionTotals(
                        steps = 2_300,
                        movingMillis =
                        (20 + n % 5) * 60_000L,
                    ),
                )
        } + listOfNotNull(
            SessionPlans.start(SessionPlans.JAPANESE_WALKING, 0, LocalDate.of(2025, 9, 13).toEpochDay(), 0, 8_000)
                ?.copy(
                    state = SessionState.FINISHED,
                    totals = SessionTotals(steps = 3_400, movingMillis = 32 * 60_000L),
                ),
            SessionPlans.startWalk(
                Ways.of(WayId.PORTO_SE_PILAR),
                0,
                SessionVoice.OFF,
                0,
                LocalDate.of(2025, 5, 3).toEpochDay(),
            )
                ?.copy(
                    state = SessionState.FINISHED,
                    totals = SessionTotals(steps = 7_100, movingMillis = 70 * 60_000L),
                ),
        )
    }

    private val journeys = listOf(
        WayJourney(
            1,
            WayId.VIA_FRANCIGENA,
            LocalDate.of(2025, 3, 1).toEpochDay(),
            0,
            WayJourneyState.FINISHED,
            LocalDate.of(2025, 9, 20).toEpochDay(),
            0,
        ),
        WayJourney(
            2,
            WayId.CAMINO_PORTUGUES,
            LocalDate.of(2025, 12, 1).toEpochDay(),
            0,
            WayJourneyState.ACTIVE,
            null,
            0,
        ),
    )

    fun state(year: Int = 2025, days: Map<Long, DailySummary> = days()): YearUiState = YearUiState(
        year = year,
        review = YearInReview.of(year, today, days, hours, outings(), journeys),
        units = UnitPreference.METRIC,
        firstDayOfWeek = DayOfWeek.MONDAY,
        today = today,
    )
}
