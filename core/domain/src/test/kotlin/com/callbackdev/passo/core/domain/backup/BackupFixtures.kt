package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UserSettings

internal object BackupFixtures {
    const val DAY: Long = 20_720L
    val profile = Profile(heightMeters = 1.80, weightKg = 75.0)

    /** The first UTC minute of [day], as if the phone were in UTC. */
    fun minuteOf(day: Long, minuteOfDay: Int): Long = day * 1_440 + minuteOfDay

    fun minutes(day: Long, vararg counts: Pair<Int, Int>): List<MinuteSteps> =
        counts.map { (minute, steps) -> MinuteSteps(minuteOf(day, minute), day, steps) }

    fun day(
        day: Long,
        minutes: List<MinuteSteps>,
        profile: Profile = this.profile,
        goal: Int = 9_000,
        finalized: Boolean = true,
    ): BackupDay = BackupDay(DaySummaries.summarize(day, minutes.map { it.steps }, profile, goal, finalized), minutes)

    val plan = SessionPlan(
        id = 4,
        name = "Park loop",
        goalKind = SessionGoalKind.TIME,
        goalValue = 25,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        position = 2,
        lastUsedAtMillis = 1_790_000_000_000,
    )

    fun session(startedAt: Long, planId: Long? = 4, state: SessionState = SessionState.FINISHED) = Session(
        planId = planId,
        name = null,
        goalKind = SessionGoalKind.TIME,
        goalValue = 25,
        intensity = SessionIntensity.BRISK,
        milestones = setOf(SessionMilestone.HALF),
        vibrate = true,
        localEpochDay = DAY,
        startedAtMillis = startedAt,
        state = state,
        endedAtMillis = if (state == SessionState.FINISHED) startedAt + 1_500_000 else null,
        end = if (state == SessionState.FINISHED) SessionEnd.GOAL else null,
        totals = SessionTotals(
            steps = 2_600,
            movingMillis = 1_500_000,
            zoneMillis = 1_400_000,
            distanceMeters = 1_950.0,
            activeKcal = 88.0,
        ),
        lastStepAtMillis = startedAt + 1_490_000,
        lastEventAtMillis = startedAt + 1_490_000,
        reachedAtMillis = if (state == SessionState.FINISHED) startedAt + 1_500_000 else null,
        toldMilestones = setOf(SessionMilestone.HALF),
    )

    fun backup(days: List<BackupDay>, profile: Profile = this.profile, sessions: List<Session> = emptyList()) = Backup(
        exportedAtMillis = 1_790_100_000_000,
        zone = "Europe/Rome",
        appVersion = "0.9.0",
        profile = profile,
        settings = UserSettings(dailyGoalSteps = 9_000),
        days = days,
        plans = listOf(plan),
        sessions = sessions,
    )
}
