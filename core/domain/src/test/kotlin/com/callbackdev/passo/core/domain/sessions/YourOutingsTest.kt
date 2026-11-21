package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * «Your outings» (PLANNING.md §11 Phase 23, step 23.2): by month, newest first, the outings that
 * hold a route told apart by their id and their start, and the numbers of the page's sentence.
 */
class YourOutingsTest {
    private val today = LocalDate.of(2026, 11, 21)
    private var nextId = 1L

    private fun outing(
        day: LocalDate,
        hour: Int = 18,
        state: SessionState = SessionState.FINISHED,
        movingMinutes: Int = 30,
    ): Session {
        val start = day.atTime(hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return Session(
            id = nextId++,
            planId = 1,
            name = null,
            goalKind = SessionGoalKind.TIME,
            goalValue = 30,
            intensity = SessionIntensity.BRISK,
            milestones = emptySet(),
            vibrate = true,
            localEpochDay = day.toEpochDay(),
            startedAtMillis = start,
            state = state,
            endedAtMillis = if (state == SessionState.FINISHED) start + movingMinutes * 60_000L else null,
            totals = SessionTotals(steps = movingMinutes * 110, movingMillis = movingMinutes * 60_000L),
        )
    }

    private fun routeOf(session: Session, points: Int = 1_800) = RouteKey(session.id, session.startedAtMillis, points)

    @Test
    fun `the outings come by month, newest first, and newest first within each`() {
        val march = outing(LocalDate.of(2026, 3, 14))
        val novemberEarly = outing(LocalDate.of(2026, 11, 2))
        val novemberMorning = outing(LocalDate.of(2026, 11, 18), hour = 8)
        val novemberEvening = outing(LocalDate.of(2026, 11, 18), hour = 19)
        val june = outing(LocalDate.of(2026, 6, 30))

        val list = YourOutings.of(
            listOf(march, novemberEarly, novemberMorning, novemberEvening, june),
            emptyList(),
            today,
        )

        assertThat(list.all.map { it.month }).containsExactly(
            YearMonth.of(2026, Month.NOVEMBER),
            YearMonth.of(2026, Month.JUNE),
            YearMonth.of(2026, Month.MARCH),
        ).inOrder()
        assertThat(list.all.first().outings.map { it.session })
            .containsExactly(novemberEvening, novemberMorning, novemberEarly).inOrder()
        assertThat(list.count).isEqualTo(5)
    }

    @Test
    fun `an outing under way or paused is left to its card`() {
        val done = outing(LocalDate.of(2026, 11, 20))
        val going = outing(today, state = SessionState.ACTIVE)
        val paused = outing(today, hour = 9, state = SessionState.PAUSED)

        val list = YourOutings.of(listOf(done, going, paused), emptyList(), today)

        assertThat(list.all.flatMap { it.outings }.map { it.session }).containsExactly(done)
        assertThat(list.summary?.count).isEqualTo(1)
    }

    @Test
    fun `nothing kept is an empty list with no sentence`() {
        val list = YourOutings.of(listOf(outing(today, state = SessionState.ACTIVE)), emptyList(), today)

        assertThat(list.all).isEmpty()
        assertThat(list.withRoute).isEmpty()
        assertThat(list.summary).isNull()
        assertThat(list.count).isEqualTo(0)
    }

    @Test
    fun `a route is an outing's only by its id and its start`() {
        val walked = outing(LocalDate.of(2026, 11, 10))
        val other = outing(LocalDate.of(2026, 11, 12))
        // Another phone's route under this phone's id: a restored database beside routes that stayed.
        val stranger = RouteKey(other.id, other.startedAtMillis - 86_400_000L, 900)

        val list = YourOutings.of(listOf(walked, other), listOf(routeOf(walked), stranger), today)

        val rows = list.all.single().outings.associateBy { it.session.id }
        assertThat(rows.getValue(walked.id).route).isEqualTo(routeOf(walked))
        assertThat(rows.getValue(other.id).hasRoute).isFalse()
        assertThat(list.withRoute.flatMap { it.outings }.map { it.session }).containsExactly(walked)
    }

    @Test
    fun `a route of one point draws no line, and is not listed as one`() {
        val single = outing(LocalDate.of(2026, 11, 10))
        val list = YourOutings.of(listOf(single), listOf(routeOf(single, points = 1)), today)

        assertThat(list.all.single().outings.single().hasRoute).isFalse()
        assertThat(list.withRoute).isEmpty()
        assertThat(list.summary?.routes).isEqualTo(RouteShare.NONE)
    }

    @Test
    fun `the filter keeps the outings with a route, by their own months`() {
        val october = outing(LocalDate.of(2026, 10, 3))
        val novemberWith = outing(LocalDate.of(2026, 11, 4))
        val novemberWithout = outing(LocalDate.of(2026, 11, 9))
        val august = outing(LocalDate.of(2026, 8, 22))

        val list = YourOutings.of(
            listOf(october, novemberWith, novemberWithout, august),
            listOf(routeOf(novemberWith), routeOf(august)),
            today,
        )

        val routed = list.months(OutingsFilter.WITH_ROUTE)
        assertThat(routed.map { it.month })
            .containsExactly(YearMonth.of(2026, Month.NOVEMBER), YearMonth.of(2026, Month.AUGUST)).inOrder()
        assertThat(routed.first().outings.map { it.session }).containsExactly(novemberWith)
        assertThat(list.months(OutingsFilter.ALL)).isEqualTo(list.all)
        assertThat(list.summary?.withRoute).isEqualTo(2)
    }

    @Test
    fun `the sentence says since when, with the year only when it is not this one`() {
        val thisYear = YourOutings.of(listOf(outing(LocalDate.of(2026, 3, 14))), emptyList(), today)
        assertThat(thisYear.summary)
            .isEqualTo(OutingsSummary(count = 1, withRoute = 0, since = YearMonth.of(2026, 3), sinceThisYear = true))

        val lastYear = YourOutings.of(
            listOf(outing(LocalDate.of(2025, 12, 30)), outing(LocalDate.of(2026, 1, 2))),
            emptyList(),
            today,
        )
        assertThat(lastYear.summary?.since).isEqualTo(YearMonth.of(2025, 12))
        assertThat(lastYear.summary?.sinceThisYear).isFalse()
    }

    @Test
    fun `the sentence ends with none, some or all of the routes`() {
        val a = outing(LocalDate.of(2026, 11, 1))
        val b = outing(LocalDate.of(2026, 11, 2))

        assertThat(YourOutings.of(listOf(a, b), emptyList(), today).summary?.routes).isEqualTo(RouteShare.NONE)
        assertThat(YourOutings.of(listOf(a, b), listOf(routeOf(a)), today).summary?.routes)
            .isEqualTo(RouteShare.SOME)
        assertThat(YourOutings.of(listOf(a, b), listOf(routeOf(a), routeOf(b)), today).summary?.routes)
            .isEqualTo(RouteShare.ALL)
    }

    @Test
    fun `a month's time in motion is its outings' own`() {
        val list = YourOutings.of(
            listOf(
                outing(LocalDate.of(2026, 11, 1), movingMinutes = 42),
                outing(LocalDate.of(2026, 11, 3), movingMinutes = 18),
                outing(LocalDate.of(2026, 10, 3), movingMinutes = 25),
            ),
            emptyList(),
            today,
        )

        assertThat(list.all.map { it.movingMillis / 60_000 }).containsExactly(60L, 25L).inOrder()
    }

    @Test
    fun `a few thousand outings are grouped in a moment`() {
        // Two outings a day for five years: some 3,650.
        val sessions = (0 until 1_825).flatMap { day ->
            val date = today.minusDays(day.toLong())
            listOf(outing(date, hour = 8), outing(date, hour = 18))
        }
        val routes = sessions.filterIndexed { index, _ -> index % 3 == 0 }.map { routeOf(it) }

        val began = System.nanoTime()
        val list = YourOutings.of(sessions, routes, today)
        val took = (System.nanoTime() - began) / 1e6
        println("YourOutings: %d outings grouped in %.1f ms".format(sessions.size, took))

        assertThat(list.count).isEqualTo(3_650)
        assertThat(list.all.size).isEqualTo(61)
        assertThat(list.summary?.withRoute).isEqualTo(routes.size)
    }

    @Test
    fun `a day is said against today`() {
        assertThat(RelativeDay.of(today, today)).isEqualTo(RelativeDay.TODAY)
        assertThat(RelativeDay.of(today.minusDays(1), today)).isEqualTo(RelativeDay.YESTERDAY)
        assertThat(RelativeDay.of(today.minusDays(2), today)).isEqualTo(RelativeDay.THIS_WEEK)
        assertThat(RelativeDay.of(today.minusDays(6), today)).isEqualTo(RelativeDay.THIS_WEEK)
        assertThat(RelativeDay.of(today.minusDays(7), today)).isEqualTo(RelativeDay.DATE)
        // A clock set back: the date, never «tomorrow».
        assertThat(RelativeDay.of(today.plusDays(1), today)).isEqualTo(RelativeDay.DATE)
    }
}
