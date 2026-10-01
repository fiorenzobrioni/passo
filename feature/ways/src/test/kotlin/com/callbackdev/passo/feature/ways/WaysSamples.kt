package com.callbackdev.passo.feature.ways

import com.callbackdev.passo.core.data.sessions.LiveSessionState
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.ways.WalkDays
import com.callbackdev.passo.core.domain.ways.WayForecast
import com.callbackdev.passo.core.domain.ways.WayProgress
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
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

    private fun millisOf(day: LocalDate, hour: Int): Long = day.toEpochDay() * MILLIS_PER_DAY + hour * MILLIS_PER_HOUR

    /** London begun yesterday, to Westminster Bridge and a little past; on it again today. */
    val london = WayJourney(
        id = 3,
        way = WayId.LONDON_PALACE_TOWER,
        startEpochDay = today.minusDays(1).toEpochDay(),
        startedAtMillis = millisOf(today.minusDays(1), 15),
        state = WayJourneyState.ACTIVE,
        endedEpochDay = null,
        toldMeters = 0,
    )

    private fun walkOuting(
        id: Long,
        walk: WayId,
        day: LocalDate,
        from: Int,
        meters: Double,
        steps: Int,
        state: SessionState,
    ): Session = checkNotNull(
        SessionPlans.startWalk(Ways.of(walk), from, SessionVoice.HEADPHONES, millisOf(day, 15), day.toEpochDay()),
    ).copy(
        id = id,
        state = state,
        totals = SessionTotals(
            steps = steps,
            movingMillis = steps * 560L,
            distanceMeters = meters,
            activeKcal =
            steps * 0.04,
        ),
        end = if (state == SessionState.FINISHED) SessionEnd.STOPPED else null,
        endedAtMillis = if (state == SessionState.FINISHED) millisOf(day, 16) else null,
        lastStepAtMillis = millisOf(day, 15) + steps * 560L,
    )

    val londonYesterday =
        walkOuting(10, WayId.LONDON_PALACE_TOWER, today.minusDays(1), 0, 3_600.0, 4_980, SessionState.FINISHED)

    /** Today's outing, under way: past the London Eye, the Royal Festival Hall ahead. */
    val londonNow = walkOuting(11, WayId.LONDON_PALACE_TOWER, today, 3_600, 1_100.0, 1_520, SessionState.ACTIVE)

    /** Milan, walked in one afternoon a few weeks ago. */
    private val milanDay: LocalDate = LocalDate.of(2026, 9, 12)

    val milan = WayJourney(
        id = 4,
        way = WayId.MILAN_DUOMO_NAVIGLI,
        startEpochDay = milanDay.toEpochDay(),
        startedAtMillis = millisOf(milanDay, 15),
        state = WayJourneyState.FINISHED,
        endedEpochDay = milanDay.toEpochDay(),
        toldMeters = 0,
    )

    private val milanOuting =
        walkOuting(12, WayId.MILAN_DUOMO_NAVIGLI, milanDay, 0, 9_340.0, 12_910, SessionState.FINISHED)

    /** An outing that is not a walk, for "one outing at a time". */
    val brisk = Session(
        id = 20,
        planId = 1,
        name = null,
        goalKind = SessionGoalKind.TIME,
        goalValue = 30,
        intensity = SessionIntensity.BRISK,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = today.toEpochDay(),
        startedAtMillis = millisOf(today, 9),
    )

    fun live(session: Session) =
        LiveSessionState(session, cadence = 104, canKeepGoing = false, alertsWhileScreenOff = true)

    fun state(
        active: WayJourney? = francigena,
        past: List<WayJourney> = listOf(dei),
        walks: List<WayJourney> = listOf(london, milan),
        outings: List<Session> = listOf(londonYesterday, londonNow, milanOuting),
        live: LiveSessionState? = live(londonNow),
    ): WaysUiState {
        val walkViews = walks.map { journey ->
            val progress = WayProgress.of(
                Ways.of(journey.way),
                journey,
                WalkDays.of(journey, outings),
                today.toEpochDay(),
            )
            JourneyView(journey, progress, null)
        }
        return WaysUiState(
            today = today,
            units = UnitPreference.METRIC,
            firstCounted = firstDay,
            distances = distances,
            pace = WayForecast.pace(distances, today),
            active = active?.let(::view),
            past = past.map(::view) + walkViews.filter { it.journey.state == WayJourneyState.FINISHED },
            walks = Ways.walks.map { walk ->
                val mine = walkViews.filter { it.journey.way == walk.id }
                WalkView(
                    way = walk,
                    current = mine.firstOrNull { it.journey.state == WayJourneyState.ACTIVE },
                    lastFinished = mine.firstOrNull { it.journey.state == WayJourneyState.FINISHED },
                    live = live?.takeIf { it.session.walk == walk.id },
                )
            },
            live = live,
        )
    }

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MILLIS_PER_HOUR = 3_600_000L
}
