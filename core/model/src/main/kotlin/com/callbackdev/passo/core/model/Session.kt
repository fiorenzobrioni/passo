package com.callbackdev.passo.core.model

/**
 * What an outing is measured against (PLANNING.md §11 Phase 10). One quantity per outing, never
 * two: with steps and minutes both, "halfway" would have no single meaning, and neither would the
 * vibration that says it.
 */
enum class SessionGoalKind {
    /** A number of steps. */
    STEPS,

    /** A distance, in metres; an estimate, from the steps and the step length. */
    DISTANCE,

    /** Minutes in motion: the time the steps say the reader was walking, pauses left out. */
    TIME,

    /**
     * The steps still missing from today's goal when the outing starts. Resolved to [STEPS] at
     * the start, so the outing keeps the number it began with.
     */
    REST_OF_DAY,

    /**
     * Sets of slow and fast minutes in motion, slow first (the Japanese interval walk, Phase 13):
     * the goal is the end of the last set, and [SessionPlan.intervals] says how they are cut. Its
     * value is the minutes of all the sets together, so it is measured as [TIME] is.
     */
    INTERVALS,
}

/**
 * How an interval outing is cut (Phase 13, docs/adr/0013-interval-walks.md): [sets] of
 * [slowMinutes] of slow walking followed by [fastMinutes] of fast walking, in minutes in motion.
 * The fast pace is the outing's intensity; the slow minutes have none to keep. The default is
 * the protocol's (Nemoto et al., 2007): five sets of three and three.
 */
data class IntervalSets(val slowMinutes: Int = 3, val fastMinutes: Int = 3, val sets: Int = 5) {
    /** Every minute of it: the outing's goal. */
    val totalMinutes: Int get() = sets * (slowMinutes + fastMinutes)
}

/**
 * One interval of an interval outing, as it was walked (Phase 13): the [index]th from 0, slow
 * when even and fast when odd.
 *
 * @property zoneMillis the part of [movingMillis] at or above the fast pace.
 */
data class IntervalSplit(
    val index: Int,
    val fast: Boolean,
    val steps: Int = 0,
    val movingMillis: Long = 0,
    val zoneMillis: Long = 0,
)

/**
 * How the outing is walked: none, or a cadence to stay at or above (the CADENCE-adults bands,
 * and Passo's own running threshold). The cadence is what the phone measures; a pace in minutes
 * per kilometre would be this cadence in disguise, with the step length's error in it.
 */
enum class SessionIntensity {
    FREE,
    BRISK,
    VIGOROUS,
    RUN,
}

/** A share of the goal worth a signal: three chosen by the reader, and the goal itself. */
enum class SessionMilestone(val percent: Int) {
    QUARTER(25),
    HALF(50),
    THREE_QUARTERS(75),
    GOAL(100),
    ;

    companion object {
        /** The ones the reader can choose; [GOAL] is always told. */
        val CHOOSABLE: List<SessionMilestone> = listOf(QUARTER, HALF, THREE_QUARTERS)

        /** Half and the goal, until the reader picks otherwise. */
        val DEFAULT: Set<SessionMilestone> = setOf(HALF)
    }
}

/**
 * Whether an outing also speaks its signals (the second iteration of Phase 10): never, only
 * through headphones (or other private audio: earbuds, a hearing aid, a car's Bluetooth), or
 * also through the phone's speaker when the phone is not silenced.
 */
enum class SessionVoice {
    OFF,
    HEADPHONES,
    ALWAYS,
}

/**
 * An outing the reader keeps, ready to start (the Outings page).
 *
 * @property name the reader's own name for it; null to be named from its goal and intensity.
 * @property goalValue steps for [SessionGoalKind.STEPS], metres for [SessionGoalKind.DISTANCE],
 *   minutes for [SessionGoalKind.TIME]; ignored for [SessionGoalKind.REST_OF_DAY].
 * @property milestones the shares told on the way, among [SessionMilestone.CHOOSABLE]; the goal
 *   is always told.
 * @property vibrate each signal also vibrates, in its own pattern, for a phone in a pocket.
 * @property voice each signal is also spoken, and where.
 * @property intervals the sets, for [SessionGoalKind.INTERVALS]; kept with any other kind, so a
 *   reader who switches away and back finds their own.
 */
data class SessionPlan(
    val id: Long = 0,
    val name: String? = null,
    val goalKind: SessionGoalKind,
    val goalValue: Int,
    val intensity: SessionIntensity,
    val milestones: Set<SessionMilestone> = SessionMilestone.DEFAULT,
    val vibrate: Boolean = true,
    val voice: SessionVoice = SessionVoice.OFF,
    val intervals: IntervalSets = IntervalSets(),
    val position: Int = 0,
    val lastUsedAtMillis: Long? = null,
)

/** Where an outing stands. */
enum class SessionState {
    /** Counting. */
    ACTIVE,

    /** Paused by the reader: steps meanwhile belong to the day, not to the outing. */
    PAUSED,

    /** Over: by the goal, by the reader, or by itself after a long stillness. */
    FINISHED,
}

/** Why an outing ended. */
enum class SessionEnd {
    /** The goal was reached. */
    GOAL,

    /** The reader stopped it. */
    STOPPED,

    /** No steps for long enough that it was over already; it ends at its last step. */
    IDLE,

    /** Counting was paused, or the outing ran past the longest one Passo keeps open. */
    CLOSED,
}

/**
 * What an outing has added up to so far.
 *
 * @property movingMillis time in motion, from the steps' own timestamps: a traffic light or a
 *   chat at a corner is not in it.
 * @property zoneMillis the part of [movingMillis] at or above the outing's cadence; all of it
 *   when the outing has no intensity.
 */
data class SessionTotals(
    val steps: Int = 0,
    val movingMillis: Long = 0,
    val zoneMillis: Long = 0,
    val distanceMeters: Double = 0.0,
    val activeKcal: Double = 0.0,
) {
    operator fun plus(other: SessionTotals): SessionTotals = SessionTotals(
        steps = steps + other.steps,
        movingMillis = movingMillis + other.movingMillis,
        zoneMillis = zoneMillis + other.zoneMillis,
        distanceMeters = distanceMeters + other.distanceMeters,
        activeKcal = activeKcal + other.activeKcal,
    )
}

/**
 * One outing, started from a plan (or from the evening reminder) and measured from the steps.
 * Its goal and its estimates are its own, copied at the start and computed as it went: changing
 * the plan or the profile later never rewrites it (VISION.md, past days are frozen).
 *
 * @property goalKind never [SessionGoalKind.REST_OF_DAY]: that is resolved to steps at the start,
 *   and [restOfDay] remembers it for the name.
 * @property goalValue steps, metres, or minutes, as [goalKind] says.
 * @property localEpochDay the day it started on: an outing across midnight belongs to its start.
 * @property lastStepAtMillis the last step counted in it; the start until the first.
 * @property lastEventAtMillis the last moment it was measured to: a step, the start or a resume.
 * @property reachedAtMillis when the goal was met, if it was.
 * @property toldMilestones the signals already given, so none is given twice.
 * @property walk the city walk it walks (Phase 11), if any: its goal is then the distance left
 *   of the walk, and its signals are the walk's places, reached at [walkFromMeters] plus the
 *   distance walked.
 * @property walkFromMeters where on the walk it started: 0, or where the last outing on it ended.
 * @property intervals the sets of an interval outing (Phase 13); null for any other.
 * @property splits an interval outing's intervals as walked so far, in order: the one under way
 *   last.
 */
data class Session(
    val id: Long = 0,
    val planId: Long?,
    val name: String?,
    val goalKind: SessionGoalKind,
    val goalValue: Int,
    val restOfDay: Boolean = false,
    val intensity: SessionIntensity,
    val milestones: Set<SessionMilestone>,
    val vibrate: Boolean,
    val voice: SessionVoice = SessionVoice.OFF,
    val localEpochDay: Long,
    val startedAtMillis: Long,
    val state: SessionState = SessionState.ACTIVE,
    val endedAtMillis: Long? = null,
    val end: SessionEnd? = null,
    val totals: SessionTotals = SessionTotals(),
    val lastStepAtMillis: Long = startedAtMillis,
    val lastEventAtMillis: Long = startedAtMillis,
    val pausedAtMillis: Long? = null,
    val reachedAtMillis: Long? = null,
    val toldMilestones: Set<SessionMilestone> = emptySet(),
    val walk: WayId? = null,
    val walkFromMeters: Int = 0,
    val intervals: IntervalSets? = null,
    val splits: List<IntervalSplit> = emptyList(),
) {
    val reached: Boolean get() = reachedAtMillis != null
    val live: Boolean get() = state != SessionState.FINISHED
}
