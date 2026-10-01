package com.callbackdev.passo.feature.ways

import com.callbackdev.passo.core.domain.ways.WayForecast
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.UnitPreference
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import java.time.LocalDate

/** Made-up days and journeys for the Ways' tests and screenshots. */
internal object WaysSamples {
    val today: LocalDate = LocalDate.of(2026, 10, 1)
    private val firstDay: LocalDate = LocalDate.of(2025, 8, 20)

    /** Every day since the first: about 3 km, more on Sundays, less on a few rainy ones. */
    val distances: Map<Long, Double> = generateSequence(firstDay) { it.plusDays(1) }
        .takeWhile { !it.isAfter(today) }
        .associate { date ->
            val base = 2_650.0 + (date.dayOfMonth * 53) % 900
            val meters = when {
                date.dayOfWeek.value == 7 -> base + 3_500
                date.dayOfMonth % 11 == 0 -> 1_100.0
                else -> base
            }
            date.toEpochDay() to meters
        }

    private fun view(journey: WayJourney): JourneyView {
        val progress = WayProgress.of(Ways.of(journey.way), journey, distances, today.toEpochDay())
        val forecast = if (journey.state == WayJourneyState.ACTIVE) {
            WayForecast.of(distances, today, progress.metersLeft)
        } else {
            null
        }
        return JourneyView(journey, progress, forecast)
    }

    /** The Francigena under way since 1 March; the Via degli Dei walked last autumn. */
    val francigena = WayJourney(
        id = 2,
        way = WayId.VIA_FRANCIGENA,
        startEpochDay = LocalDate.of(2026, 3, 1).toEpochDay(),
        startedAtMillis = 2_000,
        state = WayJourneyState.ACTIVE,
        endedEpochDay = null,
        toldMeters = 0,
    )

    val dei: WayJourney = WayJourney(
        id = 1,
        way = WayId.VIA_DEGLI_DEI,
        startEpochDay = LocalDate.of(2025, 9, 1).toEpochDay(),
        startedAtMillis = 1_000,
        state = WayJourneyState.ACTIVE,
        endedEpochDay = null,
        toldMeters = 0,
    ).let { walking ->
        // Finished on the day its days reached the end, as the app would have marked it.
        val end = checkNotNull(
            WayProgress.of(Ways.of(walking.way), walking, distances, today.toEpochDay()).finishedEpochDay,
        )
        walking.copy(
            state = WayJourneyState.FINISHED,
            endedEpochDay = end,
            toldMeters = Ways.of(walking.way).lengthMeters,
        )
    }

    fun state(active: WayJourney? = francigena, past: List<WayJourney> = listOf(dei)) = WaysUiState(
        today = today,
        units = UnitPreference.METRIC,
        firstCounted = firstDay,
        distances = distances,
        pace = WayForecast.pace(distances, today),
        active = active?.let(::view),
        past = past.map(::view),
    )
}
