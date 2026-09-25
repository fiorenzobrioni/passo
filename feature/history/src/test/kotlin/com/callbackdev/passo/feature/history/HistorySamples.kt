package com.callbackdev.passo.feature.history

import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.HourlySteps
import com.callbackdev.passo.core.domain.sessions.Outing
import com.callbackdev.passo.core.domain.walks.WalkDetector
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitPreference
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Random

/**
 * Realistic weeks for History's tests and screenshots: an office worker's weekdays (a walk to
 * the station, one at lunch, one back) and freer weekends, some longer, some lazy. Seeded, so
 * every run draws the same months.
 */
internal object HistorySamples {
    val today: LocalDate = LocalDate.of(2026, 9, 24) // a Thursday
    val profile = Profile(heightMeters = 1.78, weightKg = 74.0)
    const val GOAL = 10_000
    const val NOW_MINUTE = 15 * 60 + 40

    fun minutes(date: LocalDate, until: Int = 24 * 60): List<DayMinute> {
        val random = Random(date.toEpochDay())
        val steps = sortedMapOf<Int, Int>()
        fun add(minute: Int, count: Int) {
            if (minute < until && count > 0) steps.merge(minute, count, Int::plus)
        }
        fun walk(start: Int, length: Int, cadence: Int) {
            for (i in 0 until length) add(start + i, cadence + random.nextInt(-7, 8))
        }
        fun scatter(from: Int, to: Int, minutesPerHour: Int) {
            for (minute in from until to) if (random.nextInt(60) < minutesPerHour) add(minute, 6 + random.nextInt(34))
        }
        val weekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY
        val mood = random.nextInt(10)
        if (weekend) {
            scatter(8 * 60, 22 * 60, 16)
            if (mood > 2) walk(10 * 60 + random.nextInt(-30, 30), 40 + random.nextInt(50), 112)
            if (mood > 6) walk(17 * 60 + random.nextInt(-20, 20), 25 + random.nextInt(20), 106)
            if (mood == 9) walk(7 * 60 + 30, 32, 158)
        } else {
            scatter(6 * 60 + 50, 7 * 60 + 50, 22)
            walk(7 * 60 + 55 + random.nextInt(-4, 5), 18 + random.nextInt(-2, 3), 108)
            scatter(8 * 60 + 20, 12 * 60 + 30, 12)
            if (mood > 2) walk(12 * 60 + 35, 15 + random.nextInt(30), 104)
            scatter(13 * 60 + 10, 17 * 60 + 35, 12)
            walk(17 * 60 + 40 + random.nextInt(-5, 6), 20 + random.nextInt(-2, 3), 110)
            scatter(18 * 60 + 5, 22 * 60 + 30, 14)
        }
        return steps.map { (minute, count) -> DayMinute(minute, count) }
    }

    /** [weeks] weeks of days up to today, today to [NOW_MINUTE]. */
    fun days(weeks: Int = 30, goal: (LocalDate) -> Int = { GOAL }): Map<Long, DailySummary> {
        val first = today.minusWeeks(weeks.toLong())
        return generateSequence(first) { it.plusDays(1) }.takeWhile { !it.isAfter(today) }.associate { date ->
            val until = if (date == today) NOW_MINUTE else 24 * 60
            val summary = DaySummaries.summarize(
                localEpochDay = date.toEpochDay(),
                minuteSteps = minutes(date, until).map { it.steps },
                profile = profile,
                goalSteps = goal(date),
                finalized = date.isBefore(today),
            )
            date.toEpochDay() to summary
        }
    }

    fun state(
        days: Map<Long, DailySummary> = days(),
        walkDetection: Boolean = true,
        firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ) = HistoryUiState(
        today = today,
        nowMinute = NOW_MINUTE,
        days = days,
        firstDay = days.keys.minOrNull()?.let(LocalDate::ofEpochDay),
        goalSteps = GOAL,
        firstDayOfWeek = firstDayOfWeek,
        units = UnitPreference.METRIC,
        walkDetection = walkDetection,
        minWalkMinutes = 10,
    )

    /**
     * One day in detail; with [outing], its last walk was walked as an outing: a brisk 20
     * minutes reached (the list shows the outing in its place).
     */
    fun detail(date: LocalDate, walkDetection: Boolean = true, outing: Boolean = false): DayDetail {
        val isToday = date == today
        val minutes = minutes(date, if (isToday) NOW_MINUTE else 24 * 60)
        val metrics = MetricsCalculator.day(minutes.map { it.steps }, profile)
        val walks = if (walkDetection) WalkDetector.detect(minutes, profile, 10) else null
        return DayDetail(
            date = date,
            steps = metrics.steps,
            goalSteps = GOAL,
            distanceMeters = metrics.distanceMeters,
            activeKcal = metrics.activeKcal,
            activeMinutes = metrics.activeMinutes,
            briskMinutes = metrics.briskMinutes,
            averageCadence = metrics.averageCadence,
            hourly = HourlySteps.of(minutes),
            walks = walks,
            isToday = isToday,
            currentHour = if (isToday) NOW_MINUTE / 60 else null,
            outings = walks.orEmpty().map { walk ->
                if (outing && walk == walks?.last()) plannedFrom(walk, date) else Outing.Detected(walk)
            },
        )
    }

    private fun plannedFrom(walk: Walk, date: LocalDate): Outing.Planned {
        val session = Session(
            id = 1,
            planId = 1,
            name = null,
            goalKind = SessionGoalKind.TIME,
            goalValue = 20,
            intensity = SessionIntensity.BRISK,
            milestones = setOf(SessionMilestone.HALF),
            vibrate = true,
            localEpochDay = date.toEpochDay(),
            startedAtMillis = 0,
            state = SessionState.FINISHED,
            end = SessionEnd.GOAL,
            reachedAtMillis = 1,
            totals = SessionTotals(
                steps = walk.steps,
                movingMillis = walk.minutes * 60_000L,
                zoneMillis = (walk.minutes - 2) * 60_000L,
                distanceMeters = walk.distanceMeters,
                activeKcal = walk.activeKcal,
            ),
        )
        return Outing.Planned(session, walk.startMinute, walk.endMinute)
    }
}
