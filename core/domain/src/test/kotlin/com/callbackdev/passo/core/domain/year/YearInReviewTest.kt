package com.callbackdev.passo.core.domain.year

import com.callbackdev.passo.core.domain.insights.Trend
import com.callbackdev.passo.core.domain.sessions.SessionPlans
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.domain.walks.WalkType
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Your year on foot (PLANNING.md §11 Phase 12): the edge cases of its plan. */
class YearInReviewTest {
    private val hours = List(24) { if (it == 18) 900L else 100L }

    private fun day(date: LocalDate, steps: Int, goal: Int = 8_000) =
        DailySummary(date.toEpochDay(), steps, steps * 0.75, steps * 0.04, 0, 0, goal, finalized = true)

    private fun days(from: LocalDate, to: LocalDate, steps: (LocalDate) -> Int): Map<Long, DailySummary> =
        generateSequence(from) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }
            .associate { it.toEpochDay() to day(it, steps(it)) }

    private fun review(
        year: Int,
        today: LocalDate,
        recorded: Map<Long, DailySummary>,
        outings: List<com.callbackdev.passo.core.model.Session> = emptyList(),
        journeys: List<WayJourney> = emptyList(),
        walksOn: (LocalDate) -> List<Walk> = { emptyList() },
    ) = YearInReview.of(year, today, recorded, hours, outings, journeys, walksOn)

    private inline fun <reified T : YearPage> YearInReview.page(): T? = pages.filterIsInstance<T>().firstOrNull()

    @Test
    fun `a whole year adds up what History adds up, page by page`() {
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { 9_000 }

        val year = checkNotNull(review(2025, LocalDate.of(2026, 3, 1), recorded))

        assertThat(year.countedDays).isEqualTo(365)
        assertThat(year.soFar).isFalse()
        assertThat(year.begunLate).isFalse()
        val distance = checkNotNull(year.page<YearPage.Distance>())
        assertThat(distance.steps).isEqualTo(9_000L * 365)
        assertThat(distance.distanceMeters).isWithin(0.01).of(9_000 * 0.75 * 365)
        assertThat(distance.averageSteps).isEqualTo(9_000)
        assertThat(year.page<YearPage.Months>()?.months?.sumOf { it.steps }).isEqualTo(distance.steps)
        assertThat(year.page<YearPage.Goal>()?.daysMet).isEqualTo(365)
        assertThat(year.pages.last()).isInstanceOf(YearPage.Close::class.java)
    }

    @Test
    fun `a year counted in part says since when, and counts only its days`() {
        val recorded = days(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 31)) { 5_000 }

        val year = checkNotNull(review(2026, LocalDate.of(2027, 1, 10), recorded))

        assertThat(year.countedFrom).isEqualTo(LocalDate.of(2026, 10, 1))
        assertThat(year.begunLate).isTrue()
        assertThat(year.countedDays).isEqualTo(92)
        val months = checkNotNull(year.page<YearPage.Months>()).months
        assertThat(months.filter { it.countedDays > 0 }.map { it.month.monthValue }).containsExactly(10, 11, 12)
    }

    @Test
    fun `a leap year has 366 days`() {
        val recorded = days(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)) { 1_000 }

        assertThat(review(2024, LocalDate.of(2025, 6, 1), recorded)?.countedDays).isEqualTo(366)
    }

    @Test
    fun `ties go to the earlier day and the earlier month`() {
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { date ->
            when (date) {
                LocalDate.of(2025, 3, 3), LocalDate.of(2025, 7, 7) -> 20_000
                else -> if (date.month.value in listOf(1, 3)) 6_000 else 5_000
            }
        }

        val year = checkNotNull(review(2025, LocalDate.of(2026, 2, 1), recorded))

        assertThat(year.page<YearPage.BestDay>()?.date).isEqualTo(LocalDate.of(2025, 3, 3))
        // March has the best day on top of January's level, and as many days: March wins.
        assertThat(year.page<YearPage.Months>()?.best).isEqualTo(YearMonth.of(2025, 3))
    }

    @Test
    fun `fewer than thirty days counted is not offered`() {
        val recorded = days(LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 31)) { 5_000 }

        assertThat(review(2026, LocalDate.of(2027, 1, 2), recorded)).isNull()
        assertThat(YearInReview.offeredYears(LocalDate.of(2026, 12, 5), LocalDate.of(2027, 1, 2))).isEmpty()
    }

    @Test
    fun `the current year is offered by hand as so far, before December too`() {
        val today = LocalDate.of(2026, 9, 24)
        val recorded = days(LocalDate.of(2026, 6, 1), today) { 7_000 }

        val year = checkNotNull(review(2026, today, recorded))

        assertThat(year.soFar).isTrue()
        assertThat(year.countedTo).isEqualTo(today)
        assertThat(YearInReview.offeredYears(LocalDate.of(2026, 6, 1), today)).containsExactly(2026)
        assertThat(YearInReview.inSeason(LocalDate.of(2026, 6, 1), today)).isNull()
    }

    @Test
    fun `in season from December to January, for the year ending`() {
        val first = LocalDate.of(2026, 1, 1)
        assertThat(YearInReview.inSeason(first, LocalDate.of(2026, 12, 1))).isEqualTo(2026)
        assertThat(YearInReview.inSeason(first, LocalDate.of(2027, 1, 31))).isEqualTo(2026)
        assertThat(YearInReview.inSeason(first, LocalDate.of(2027, 2, 1))).isNull()
    }

    @Test
    fun `a year with no outings and no way leaves their pages out`() {
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { 3_000 }

        val year = checkNotNull(review(2025, LocalDate.of(2026, 1, 5), recorded))

        assertThat(year.page<YearPage.Outings>()).isNull()
        assertThat(year.page<YearPage.Ways>()).isNull()
        assertThat(year.page<YearPage.VersusLastYear>()).isNull()
    }

    @Test
    fun `the year before with too few days gives no comparison, with enough it does, by averages`() {
        val thin = days(LocalDate.of(2025, 12, 10), LocalDate.of(2026, 12, 31)) { 6_000 }
        assertThat(review(2026, LocalDate.of(2027, 1, 3), thin)?.page<YearPage.VersusLastYear>()).isNull()

        val both = days(LocalDate.of(2025, 10, 3), LocalDate.of(2025, 12, 31)) { 5_000 } +
            days(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)) { 6_000 }
        val versus = checkNotNull(review(2026, LocalDate.of(2027, 1, 3), both)?.page<YearPage.VersusLastYear>())

        assertThat(versus.lastYear).isEqualTo(2025)
        assertThat(versus.average).isEqualTo(6_000)
        assertThat(versus.lastAverage).isEqualTo(5_000)
        assertThat(versus.trend).isEqualTo(Trend.UP)
    }

    @Test
    fun `the best day names the outing that made it, or the walk when there was none`() {
        val best = LocalDate.of(2025, 5, 10)
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { if (it == best) 18_000 else 4_000 }
        val walk = Walk(600, 660, 6_000, 4_500.0, 200.0, 100, WalkType.WALK)

        val withWalk = checkNotNull(review(2025, LocalDate.of(2026, 1, 2), recorded, walksOn = { listOf(walk) }))
        assertThat(withWalk.page<YearPage.BestDay>()?.walk).isEqualTo(walk)

        val outing = checkNotNull(SessionPlans.start(SessionPlans.REST_OF_DAY, 0, best.toEpochDay(), 4_000, 8_000))
            .copy(state = SessionState.FINISHED, totals = SessionTotals(steps = 5_000, movingMillis = 50 * 60_000L))
        val withOuting = checkNotNull(
            review(2025, LocalDate.of(2026, 1, 2), recorded, outings = listOf(outing), walksOn = { listOf(walk) }),
        )
        assertThat(withOuting.page<YearPage.BestDay>()?.outing).isEqualTo(outing)
        assertThat(withOuting.page<YearPage.BestDay>()?.walk).isNull()
        assertThat(withOuting.page<YearPage.Outings>()?.count).isEqualTo(1)
    }

    @Test
    fun `the rhythm is the weekday walked most on average, and the busiest hour`() {
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) {
            if (it.dayOfWeek == DayOfWeek.SATURDAY) 12_000 else 6_000
        }

        val rhythm = checkNotNull(review(2025, LocalDate.of(2026, 1, 2), recorded)?.page<YearPage.Rhythm>())

        assertThat(rhythm.weekday).isEqualTo(DayOfWeek.SATURDAY)
        assertThat(rhythm.hour).isEqualTo(18)
        assertThat(rhythm.byWeekday[DayOfWeek.SATURDAY.ordinal]).isEqualTo(12_000)
    }

    @Test
    fun `no minutes, no rhythm`() {
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { 6_000 }

        val year = YearInReview.of(2025, LocalDate.of(2026, 1, 2), recorded, List(24) { 0L }, emptyList(), emptyList())

        assertThat(year?.page<YearPage.Rhythm>()).isNull()
    }

    @Test
    fun `the goal's longest streak is the year's own`() {
        val recorded = days(LocalDate.of(2024, 12, 20), LocalDate.of(2025, 12, 31)) { date ->
            // Met every day from 20 December 2024 to 10 January 2025, then never again.
            if (date.isBefore(LocalDate.of(2025, 1, 11))) 9_000 else 2_000
        }

        val goal = checkNotNull(review(2025, LocalDate.of(2026, 1, 2), recorded)?.page<YearPage.Goal>())

        assertThat(goal.daysMet).isEqualTo(10)
        assertThat(goal.longest.days).isEqualTo(10)
        assertThat(goal.longest.start).isEqualTo(LocalDate.of(2025, 1, 1))
    }

    @Test
    fun `the way's page counts only the year's share of a way begun before it`() {
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31)) { 10_000 }
        val journey = WayJourney(
            id = 1,
            way = WayId.VIA_FRANCIGENA,
            startEpochDay = LocalDate.of(2025, 12, 1).toEpochDay(),
            startedAtMillis = 0,
            state = WayJourneyState.ACTIVE,
            endedEpochDay = null,
            toldMeters = 0,
        )

        val year = checkNotNull(review(2026, LocalDate.of(2026, 3, 31), recorded, journeys = listOf(journey)))
        val way = checkNotNull(year.page<YearPage.Ways>()).ways.single()

        // 1 January to 31 March 2026: 90 days of 7.5 km.
        assertThat(way.meters).isWithin(0.1).of(90 * 7_500.0)
        assertThat(way.stamps).isGreaterThan(0)
    }

    @Test
    fun `the cities walked are named`() {
        val recorded = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { 6_000 }
        val walk = checkNotNull(
            SessionPlans.startWalk(
                Ways.of(WayId.PORTO_SE_PILAR),
                0,
                SessionVoice.OFF,
                0,
                LocalDate.of(2025, 4, 2).toEpochDay(),
            ),
        )
            .copy(state = SessionState.FINISHED)

        val year = checkNotNull(review(2025, LocalDate.of(2026, 1, 2), recorded, outings = listOf(walk)))

        assertThat(year.page<YearPage.Ways>()?.cities).containsExactly(WayId.PORTO_SE_PILAR)
    }

    @Test
    fun `the distance as a way`() {
        val shortest = Ways.all.minBy { it.lengthMeters }
        val longest = Ways.all.maxBy { it.lengthMeters }

        assertThat(YearInReview.measure(longest.lengthMeters * 2.5)).isEqualTo(WayMeasure.MoreThan(longest.id, 2))
        assertThat(YearInReview.measure(shortest.lengthMeters * 0.5)).isEqualTo(WayMeasure.Part(shortest.id, 2))
        assertThat(YearInReview.measure(0.0)).isNull()
    }

    @Test
    fun `the close is chosen from the facts`() {
        val atGoal = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { 9_000 }
        assertThat(review(2025, LocalDate.of(2026, 1, 2), atGoal)?.pages?.last())
            .isEqualTo(YearPage.Close(YearClose.GoalMostDays(365)))

        val quiet = days(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)) { 3_000 }
        assertThat(review(2025, LocalDate.of(2026, 1, 2), quiet)?.pages?.last())
            .isEqualTo(YearPage.Close(YearClose.EveryStep(3_000L * 365)))
    }
}
