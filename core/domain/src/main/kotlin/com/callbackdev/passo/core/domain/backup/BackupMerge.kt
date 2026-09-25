package com.callbackdev.passo.core.domain.backup

import com.callbackdev.passo.core.domain.metrics.DaySummaries
import com.callbackdev.passo.core.domain.metrics.MinuteChange
import com.callbackdev.passo.core.model.DailySummary
import com.callbackdev.passo.core.model.MinuteSteps
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.UserSettings

/**
 * What an import writes: the minutes that change (their full new count), the summaries that
 * change, and how many days were new, merged, or already here.
 */
data class DayMergePlan(
    val minutes: List<MinuteSteps>,
    val summaries: List<DailySummary>,
    val addedDays: Int,
    val mergedDays: Int,
    val unchangedDays: Int,
)

/** The plans an import adds, and which plan of this phone each of the file's plans became. */
data class PlanMerge(val toAdd: List<SessionPlan>, val matched: Map<Long, Long>)

/**
 * How a backup joins what this phone already has (ADR 0011). An import adds and never takes
 * away: nothing on the phone is deleted or lowered, so importing the file of a phone that
 * counted alongside this one, or the same file twice, never loses or doubles a step.
 *
 * - **A minute** is the larger of its two counts, never their sum: one minute walked with two
 *   phones in the pocket is still one minute walked. It keeps the day this phone recorded it on.
 * - **A day only the file has** comes in as the file had it: its minutes, and its summary frozen
 *   with the estimates and goal of that phone. A clean install that imports reproduces the
 *   history exactly.
 * - **A day both have**, where the file adds steps: a day already over keeps its own summary and
 *   takes only the added steps' share, measured with the current profile (the rule for steps
 *   that reach a frozen day late, `DaySummaries.withLateSteps`); a day still open is recomputed
 *   whole, as every write does.
 * - **Today** follows the current profile and goal, as it always does.
 */
object BackupMerge {
    /**
     * @param local this phone's minutes of every day the file touches, and of every day holding
     *   a minute the file also has; grouped by the day each was recorded on.
     * @param localSummaries this phone's summaries of those days.
     * @param today the local day now: from it on, days are open.
     * @param profile the profile in effect now, the one the import itself may have brought.
     * @param goalSteps today's goal now.
     */
    fun days(
        backup: Backup,
        local: Map<Long, List<MinuteSteps>>,
        localSummaries: Map<Long, DailySummary>,
        today: Long,
        profile: Profile,
        goalSteps: Int,
    ): DayMergePlan {
        val localByMinute = HashMap<Long, MinuteSteps>()
        for (minutes in local.values) for (minute in minutes) localByMinute[minute.epochMinute] = minute

        val writes = LinkedHashMap<Long, MinuteSteps>()
        for (day in backup.days) {
            for (incoming in day.minutes) {
                val mine = localByMinute[incoming.epochMinute]
                when {
                    mine == null -> writes[incoming.epochMinute] = incoming
                    incoming.steps > mine.steps -> writes[incoming.epochMinute] = mine.copy(steps = incoming.steps)
                }
            }
        }

        val backupDays = backup.days.associateBy { it.summary.localEpochDay }
        val writesByDay = writes.values.groupBy { it.localEpochDay }
        val touched = (backupDays.keys + writesByDay.keys).toSortedSet()
        val summaries = ArrayList<DailySummary>()
        var added = 0
        var merged = 0
        var unchanged = 0
        for (day in touched) {
            val mine = local[day].orEmpty().associateBy { it.epochMinute }
            val dayWrites = writesByDay[day].orEmpty()
            val existing = localSummaries[day]
            if (dayWrites.isEmpty() && existing != null) {
                unchanged++
                continue
            }
            val incoming = backupDays[day]
            val after = mine + dayWrites.associateBy { it.epochMinute }
            // A day with no minute left to it (its only ones are recorded here on another day)
            // is no day: an empty row would only move where History begins.
            if (after.isEmpty()) continue
            val counts = after.values.map { it.steps }
            // The day is the file's alone when nothing of this phone's is left in it: every
            // minute of it is the file's, at the file's count.
            val fromFileAlone = incoming != null && incoming.minutes.size == after.size &&
                incoming.minutes.all { after[it.epochMinute]?.steps == it.steps }
            if (mine.isEmpty() && existing == null) added++ else merged++
            summaries += when {
                // Open days follow today's profile and goal, as every write leaves them.
                day >= today -> DaySummaries.summarize(day, counts, profile, goalSteps, finalized = false)

                // The file's own day, whole: as it was frozen there, or frozen now with the
                // profile that phone had, which is the one it was being measured with.
                fromFileAlone && incoming.summary.finalized -> incoming.summary

                fromFileAlone -> DaySummaries.summarize(
                    day,
                    counts,
                    backup.profile,
                    incoming.summary.goalSteps,
                    finalized = true,
                )

                // This phone's frozen day, reached by the file's extra steps: only their share.
                existing != null && existing.finalized -> DaySummaries.withLateSteps(
                    existing,
                    dayWrites.map {
                        MinuteChange(stepsBefore = mine[it.epochMinute]?.steps ?: 0, stepsAfter = it.steps)
                    },
                    profile,
                )

                else -> DaySummaries.summarize(
                    day,
                    counts,
                    profile,
                    existing?.goalSteps ?: incoming?.summary?.goalSteps ?: goalSteps,
                    finalized = true,
                )
            }
        }
        return DayMergePlan(writes.values.toList(), summaries, added, merged, unchanged)
    }

    /**
     * The file's plans this phone does not have yet: a plan with the same name, goal, pace,
     * signals and voice is the same plan, whatever its id or place. New ones go after the
     * phone's own, in the file's order.
     */
    fun plans(local: List<SessionPlan>, incoming: List<SessionPlan>): PlanMerge {
        val matched = HashMap<Long, Long>()
        val toAdd = ArrayList<SessionPlan>()
        for (plan in incoming.sortedBy { it.position }) {
            val same = local.firstOrNull { it.sameAs(plan) }
            if (same != null) {
                matched[plan.id] = same.id
            } else if (toAdd.none { it.sameAs(plan) }) {
                toAdd += plan
            }
        }
        return PlanMerge(toAdd, matched)
    }

    /**
     * The file's outings this phone does not have: one started at the same millisecond is the
     * same outing. One that was under way when the file was written is over now, at its last
     * step; [planIds] maps the file's plan ids to this phone's, and an outing whose plan did not
     * come keeps its goal and name but no plan.
     */
    fun sessions(local: List<Session>, incoming: List<Session>, planIds: Map<Long, Long>): List<Session> {
        val known = local.mapTo(HashSet()) { it.startedAtMillis }
        return incoming
            .filter { known.add(it.startedAtMillis) }
            .sortedBy { it.startedAtMillis }
            .map { session ->
                val closed = if (session.live) {
                    session.copy(
                        state = SessionState.FINISHED,
                        end = SessionEnd.CLOSED,
                        endedAtMillis = session.lastStepAtMillis,
                        pausedAtMillis = null,
                    )
                } else {
                    session
                }
                closed.copy(id = 0, planId = session.planId?.let { planIds[it] })
            }
    }

    /**
     * The file's settings in place of this phone's, except what only this phone decides: whether
     * it is counting now, and whether its first run is done.
     */
    fun settings(current: UserSettings, incoming: UserSettings): UserSettings =
        incoming.copy(trackingEnabled = current.trackingEnabled, onboardingCompleted = current.onboardingCompleted)

    private fun SessionPlan.sameAs(other: SessionPlan) = name == other.name &&
        goalKind == other.goalKind &&
        goalValue == other.goalValue &&
        intensity == other.intensity &&
        milestones == other.milestones &&
        vibrate == other.vibrate &&
        voice == other.voice
}
