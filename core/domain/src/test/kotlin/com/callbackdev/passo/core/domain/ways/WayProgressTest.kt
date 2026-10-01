package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate

/** The edge cases of PLANNING.md §11 Phase 11, on a small way of 6 km. */
class WayProgressTest {
    private val start = LocalDate.of(2026, 9, 1).toEpochDay()

    // A at 0, a place B at 1 km, C at 3 km, D (the end) at 6 km.
    private val way = Way(
        WaySource(
            id = WayId.VIA_DEGLI_DEI,
            lengthMeters = 6_000,
            stops = listOf(
                WayStop("a", 0, 44.0, 11.0, stage = true),
                WayStop("b", 1_000, 44.01, 11.0, stage = false),
                WayStop("c", 3_000, 44.03, 11.0, stage = true),
                WayStop("d", 6_000, 44.06, 11.0, stage = true),
            ),
            frame = GeoBox(43.9, 10.9, 44.1, 11.1),
            line = "",
            land = emptyList(),
            lakes = emptyList(),
            rivers = emptyList(),
            borders = emptyList(),
            locatorFrame = GeoBox(36.0, 6.0, 47.0, 19.0),
            locatorLand = emptyList(),
            locatorLine = "",
        ),
    )

    private fun journey(state: WayJourneyState = WayJourneyState.ACTIVE, ended: Long? = null, startDay: Long = start) =
        WayJourney(1, WayId.VIA_DEGLI_DEI, startDay, 0, state, ended, toldMeters = 0)

    private fun progress(distances: Map<Long, Double>, today: Long, journey: WayJourney = journey()) =
        WayProgress.of(way, journey, distances, today)

    @Test
    fun `a new journey stands at the first stop, reached on the start day`() {
        val progress = progress(emptyMap(), start)
        assertThat(progress.walkedMeters).isEqualTo(0.0)
        assertThat(progress.reached).containsExactly(StopReached(way.stops[0], start))
        assertThat(progress.next?.key).isEqualTo("b")
        assertThat(progress.nextStage?.key).isEqualTo("c")
        assertThat(progress.finished).isFalse()
    }

    @Test
    fun `each stop is reached on the day the distance got there`() {
        val progress = progress(mapOf(start to 800.0, start + 1 to 900.0, start + 2 to 1_500.0), start + 2)
        assertThat(progress.walkedMeters).isEqualTo(3_200.0)
        assertThat(progress.reached.map { it.stop.key to it.epochDay })
            .containsExactly("a" to start, "b" to start + 1, "c" to start + 2).inOrder()
        assertThat(progress.stamps.map { it.stop.key }).containsExactly("a", "c").inOrder()
        assertThat(progress.metersTo(way.stops[3])).isEqualTo(2_800.0)
    }

    @Test
    fun `days before the start and a day without steps move nothing`() {
        val progress = progress(mapOf(start - 1 to 5_000.0, start + 1 to 0.0, start + 3 to 500.0), start + 3)
        assertThat(progress.walkedMeters).isEqualTo(500.0)
        assertThat(progress.reached).hasSize(1)
    }

    @Test
    fun `the last stop ends it that day, and what is beyond is not carried`() {
        val progress = progress(mapOf(start to 4_000.0, start + 1 to 4_000.0, start + 2 to 9_000.0), start + 2)
        assertThat(progress.finished).isTrue()
        assertThat(progress.finishedEpochDay).isEqualTo(start + 1)
        assertThat(progress.walkedMeters).isEqualTo(6_000.0)
        assertThat(progress.metersLeft).isEqualTo(0.0)
        assertThat(progress.fraction).isEqualTo(1.0)
        assertThat(progress.next).isNull()
        assertThat(progress.reached.last().stop.key).isEqualTo("d")
    }

    @Test
    fun `a way finished stays finished when its days are measured shorter since`() {
        val finished = journey(WayJourneyState.FINISHED, ended = start + 2)
        val progress = progress(mapOf(start to 2_000.0, start + 1 to 2_000.0), start + 9, finished)
        assertThat(progress.finished).isTrue()
        assertThat(progress.finishedEpochDay).isEqualTo(start + 2)
        assertThat(progress.walkedMeters).isEqualTo(6_000.0)
        assertThat(progress.reached.map { it.stop.key to it.epochDay })
            .containsExactly("a" to start, "b" to start, "c" to start + 1, "d" to start + 2).inOrder()
    }

    @Test
    fun `a way left counts only up to the day it was left`() {
        val left = journey(WayJourneyState.LEFT, ended = start + 1)
        val progress = progress(mapOf(start to 1_000.0, start + 1 to 1_000.0, start + 2 to 3_000.0), start + 5, left)
        assertThat(progress.walkedMeters).isEqualTo(2_000.0)
        assertThat(progress.finished).isFalse()
    }

    @Test
    fun `the stage to tell is the furthest passed, never the start nor a place`() {
        assertThat(WayAnnouncement.next(way, walkedMeters = 500.0, toldMeters = 0)).isNull()
        assertThat(WayAnnouncement.next(way, walkedMeters = 1_200.0, toldMeters = 0)).isNull()
        assertThat(WayAnnouncement.next(way, walkedMeters = 3_000.0, toldMeters = 0)?.key).isEqualTo("c")
        assertThat(WayAnnouncement.next(way, walkedMeters = 6_000.0, toldMeters = 0)?.key).isEqualTo("d")
        assertThat(WayAnnouncement.next(way, walkedMeters = 4_000.0, toldMeters = 3_000)).isNull()
    }

    @Test
    fun `a start in the past tells nothing behind it`() {
        // At the start the journey is told up to where the reader already stands.
        val walked = progress(mapOf(start to 3_500.0), start).walkedMeters
        assertThat(WayAnnouncement.next(way, walked, toldMeters = walked.toInt())).isNull()
        assertThat(WayAnnouncement.next(way, 6_000.0, toldMeters = walked.toInt())?.key).isEqualTo("d")
    }

    @Test
    fun `the forecast takes the last four weeks before today, counted days only`() {
        val today = LocalDate.of(2026, 9, 29)
        val days = (1..10).associate { today.minusDays(it.toLong()).toEpochDay() to 5_000.0 } +
            (today.toEpochDay() to 20_000.0) + // today is not over: left out
            (today.minusDays(40).toEpochDay() to 50_000.0) // outside the window
        val forecast = checkNotNull(WayForecast.of(days, today, metersLeft = 52_000.0))
        assertThat(forecast.metersPerDay).isEqualTo(5_000.0)
        assertThat(forecast.daysLeft).isEqualTo(11)
        assertThat(forecast.arrival).isEqualTo(today.plusDays(11))
    }

    @Test
    fun `no forecast with too few days, too slow a pace, or nothing left`() {
        val today = LocalDate.of(2026, 9, 29)
        val few = (1..6).associate { today.minusDays(it.toLong()).toEpochDay() to 5_000.0 }
        assertThat(WayForecast.of(few, today, 10_000.0)).isNull()
        val slow = (1..10).associate { today.minusDays(it.toLong()).toEpochDay() to 50.0 }
        assertThat(WayForecast.of(slow, today, 10_000.0)).isNull()
        val enough = (1..10).associate { today.minusDays(it.toLong()).toEpochDay() to 5_000.0 }
        assertThat(WayForecast.of(enough, today, 0.0)).isNull()
    }

    @Test
    fun `a start is never after today nor before the first day counted`() {
        val today = LocalDate.of(2026, 10, 1)
        val first = LocalDate.of(2026, 3, 15)
        assertThat(WayStarts.dayOf(WayStartChoice.TODAY, today, first, null)).isEqualTo(today)
        assertThat(WayStarts.dayOf(WayStartChoice.THIS_YEAR, today, first, null)).isEqualTo(first)
        assertThat(WayStarts.dayOf(WayStartChoice.THIS_YEAR, today, LocalDate.of(2025, 6, 1), null))
            .isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(WayStarts.dayOf(WayStartChoice.FIRST_DAY, today, first, null)).isEqualTo(first)
        assertThat(WayStarts.dayOf(WayStartChoice.CHOSEN, today, first, LocalDate.of(2027, 1, 1))).isEqualTo(today)
        assertThat(WayStarts.dayOf(WayStartChoice.CHOSEN, today, first, LocalDate.of(2026, 1, 1))).isEqualTo(first)
        assertThat(WayStarts.dayOf(WayStartChoice.FIRST_DAY, today, null, null)).isEqualTo(today)
    }

    @Test
    fun `the projection fits the frame and keeps its proportions`() {
        val frame = GeoBox(44.0, 11.0, 45.0, 12.0)
        val projection = WayProjection(frame, width = 400f, height = 400f)
        // A degree of longitude is shorter at 44.5°: the frame is taller than wide, centred.
        assertThat(projection.y(45.0)).isWithin(0.01f).of(0f)
        assertThat(projection.y(44.0)).isWithin(0.01f).of(400f)
        val left = projection.x(11.0)
        val right = projection.x(12.0)
        assertThat(left).isGreaterThan(0f)
        assertThat(left + right).isWithin(0.01f).of(400f)
        assertThat((right - left) / 400f).isWithin(0.01f).of(0.713f)
    }

    @Test
    fun `a walk's days are its outings since the journey began`() {
        val walk = WayJourney(3, WayId.MILAN_DUOMO_NAVIGLI, start, 5_000, WayJourneyState.ACTIVE, null, 0)
        fun outing(day: Long, at: Long, meters: Double, on: WayId? = WayId.MILAN_DUOMO_NAVIGLI) = Session(
            planId = null,
            name = null,
            goalKind = SessionGoalKind.DISTANCE,
            goalValue = 9_000,
            intensity = SessionIntensity.FREE,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = day,
            startedAtMillis = at,
            totals = SessionTotals(distanceMeters = meters),
            walk = on,
        )
        val days = WalkDays.of(
            walk,
            listOf(
                outing(start - 3, 1_000, 2_000.0), // before the journey: an earlier walk of it
                outing(start, 6_000, 3_000.0),
                outing(start, 7_000, 1_500.0),
                outing(start + 2, 9_000, 2_500.0),
                outing(start + 2, 9_500, 800.0, on = WayId.LONDON_PALACE_TOWER),
                outing(start + 2, 9_900, 600.0, on = null),
            ),
        )
        assertThat(days).containsExactly(start, 4_500.0, start + 2, 2_500.0)
    }
}
