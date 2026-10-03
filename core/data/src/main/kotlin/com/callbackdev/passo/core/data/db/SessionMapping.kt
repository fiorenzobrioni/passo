package com.callbackdev.passo.core.data.db

import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.IntervalSplit
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.SessionVoice
import com.callbackdev.passo.core.model.WayId

/** Null for a row a newer build wrote with a kind this one does not know: it is left out. */
internal fun SessionPlanEntity.toModel(): SessionPlan? {
    val kind = goalKind.toEnumOrNull<SessionGoalKind>() ?: return null
    return SessionPlan(
        id = id,
        name = name,
        goalKind = kind,
        goalValue = goalValue,
        intensity = intensity.toEnumOrNull<SessionIntensity>() ?: SessionIntensity.FREE,
        milestones = milestones.toMilestones(),
        vibrate = vibrate,
        voice = voice.toEnumOrNull<SessionVoice>() ?: SessionVoice.OFF,
        position = position,
        lastUsedAtMillis = lastUsedAtMillis,
        intervals = IntervalSets(slowMinutes, fastMinutes, sets),
    )
}

internal fun SessionPlan.toEntity() = SessionPlanEntity(
    id = id,
    name = name?.trim()?.takeIf { it.isNotEmpty() },
    goalKind = goalKind.name,
    goalValue = goalValue,
    intensity = intensity.name,
    milestones = milestones.toBits(),
    vibrate = vibrate,
    position = position,
    lastUsedAtMillis = lastUsedAtMillis,
    voice = voice.name,
    slowMinutes = intervals.slowMinutes,
    fastMinutes = intervals.fastMinutes,
    sets = intervals.sets,
)

internal fun SessionRow.toModel(): Session? = session.toModel(intervals)

/**
 * Null for a row a newer build wrote with a kind this one does not know, or an interval outing
 * without its sets: it is left out.
 */
internal fun SessionEntity.toModel(intervals: List<SessionIntervalEntity> = emptyList()): Session? {
    val kind = goalKind.toEnumOrNull<SessionGoalKind>() ?: return null
    val sets = IntervalSets(slowMinutes, fastMinutes, sets).takeIf { kind == SessionGoalKind.INTERVALS }
    if (sets != null && (sets.sets <= 0 || sets.slowMinutes <= 0 || sets.fastMinutes <= 0)) return null
    return Session(
        id = id,
        planId = planId,
        name = name,
        goalKind = kind,
        goalValue = goalValue,
        restOfDay = restOfDay,
        intensity = intensity.toEnumOrNull<SessionIntensity>() ?: SessionIntensity.FREE,
        milestones = milestones.toMilestones(),
        vibrate = vibrate,
        voice = voice.toEnumOrNull<SessionVoice>() ?: SessionVoice.OFF,
        localEpochDay = localEpochDay,
        startedAtMillis = startedAtMillis,
        // An unknown state from a newer build is over: nothing here would know how to go on.
        state = state.toEnumOrNull<SessionState>() ?: SessionState.FINISHED,
        endedAtMillis = endedAtMillis,
        end = endReason.toEnumOrNull<SessionEnd>(),
        totals = SessionTotals(
            steps = steps,
            movingMillis = movingMillis,
            zoneMillis = zoneMillis,
            distanceMeters = distanceMeters,
            activeKcal = activeKcal,
        ),
        lastStepAtMillis = lastStepAtMillis,
        lastEventAtMillis = lastEventAtMillis,
        pausedAtMillis = pausedAtMillis,
        reachedAtMillis = reachedAtMillis,
        toldMilestones = toldMilestones.toMilestones(),
        // A walk a newer build knows and this one does not: the outing stays, without its walk.
        walk = walk.toEnumOrNull<WayId>(),
        walkFromMeters = walkFromMeters,
        intervals = sets,
        splits = if (sets == null) {
            emptyList()
        } else {
            intervals.sortedBy { it.intervalIndex }.map {
                IntervalSplit(it.intervalIndex, it.fast, it.steps, it.movingMillis, it.zoneMillis)
            }
        },
    )
}

internal fun Session.toEntity() = SessionEntity(
    id = id,
    planId = planId,
    name = name,
    goalKind = goalKind.name,
    goalValue = goalValue,
    restOfDay = restOfDay,
    intensity = intensity.name,
    milestones = milestones.toBits(),
    vibrate = vibrate,
    localEpochDay = localEpochDay,
    startedAtMillis = startedAtMillis,
    state = state.name,
    endedAtMillis = endedAtMillis,
    endReason = end?.name,
    steps = totals.steps,
    movingMillis = totals.movingMillis,
    zoneMillis = totals.zoneMillis,
    distanceMeters = totals.distanceMeters,
    activeKcal = totals.activeKcal,
    lastStepAtMillis = lastStepAtMillis,
    lastEventAtMillis = lastEventAtMillis,
    pausedAtMillis = pausedAtMillis,
    reachedAtMillis = reachedAtMillis,
    toldMilestones = toldMilestones.toBits(),
    voice = voice.name,
    walk = walk?.name,
    walkFromMeters = walkFromMeters,
    slowMinutes = intervals?.slowMinutes ?: 0,
    fastMinutes = intervals?.fastMinutes ?: 0,
    sets = intervals?.sets ?: 0,
)

/** An outing's intervals as rows; none for any other outing, or one not yet written. */
internal fun Session.intervalEntities(): List<SessionIntervalEntity> = if (id == 0L) {
    emptyList()
} else {
    splits.map { SessionIntervalEntity(id, it.index, it.fast, it.steps, it.movingMillis, it.zoneMillis) }
}

private fun Set<SessionMilestone>.toBits(): Int = fold(0) { bits, milestone -> bits or (1 shl milestone.ordinal) }

private fun Int.toMilestones(): Set<SessionMilestone> =
    SessionMilestone.entries.filter { this and (1 shl it.ordinal) != 0 }.toSet()

private inline fun <reified E : Enum<E>> String?.toEnumOrNull(): E? =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } }
