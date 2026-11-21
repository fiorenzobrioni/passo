package com.callbackdev.passo.core.model

/**
 * What an outing is measured against (PLANNING.md §11 Phase 10). One quantity per outing, never
 * two: with steps and minutes both, "halfway" would have no single meaning, and neither would the
 * vibration that says it. Or none: the open outing ([OPEN], Phase 21).
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

    /**
     * No goal (Phase 21, «None» in the editor; the open outing in the documents): it counts until
     * the reader stops it, or ends by itself as every outing does, at its last step after a long
     * stillness. Its value is 0 and never read; it has no share, no milestone and no end by goal.
     */
    OPEN,
}

/**
 * A pause of an outing, from the reader's Pause to their Resume (Phase 21): what an outing's page
 * says of the time paused, and where its minutes have a gap. Only a pause that ended inside the
 * outing: the one under way is the outing's [Session.pausedAtMillis], and an outing stopped or
 * ended while paused ends where it was paused, so that pause was never part of it.
 */
data class SessionPause(val fromMillis: Long, val toMillis: Long) {
    val millis: Long get() = (toMillis - fromMillis).coerceAtLeast(0L)
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
 * A hundred metres of an outing measured with GPS, as walked (Phase 16): what its kilometres or
 * its miles are cut from, whichever the reader walks in. Numbers, never places.
 *
 * @property index the [index]th from 0.
 * @property meters of the outing's fused distance: a hundred, but for the last one, which is
 *   under way or was left short.
 * @property movingMillis the time in motion it took, from the steps' own timestamps.
 * @property steps the steps it took; a part of a batch's, so not whole.
 * @property endedAtMillis when its last metre was walked, on the steps' clock.
 * @property ascentMeters what was climbed in it, and [descentMeters] what was come down.
 */
data class OutingLeg(
    val index: Int,
    val meters: Double,
    val movingMillis: Long = 0,
    val steps: Double = 0.0,
    val endedAtMillis: Long,
    val ascentMeters: Double = 0.0,
    val descentMeters: Double = 0.0,
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
 * An open outing's signal on the way (Phase 21, step 21.2): a mark at each kilometre, or each mile
 * for a reader who walks in miles, at each ten minutes in motion, or none. An open outing has no
 * share of a goal to tell, so its marks are its signals (docs/adr/0009-sessions.md, decision 3).
 */
enum class SessionMarks {
    /** A kilometre, or a mile: measured where the outing is measured with GPS, estimated otherwise. */
    DISTANCE,

    /** Ten minutes in motion: a pause, or a wait at a light, adds nothing to them. */
    TIME,

    /** No signal on the way. */
    NONE,
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
 * @property gps «Measure with GPS» (docs/adr/0016-location.md): an outing started from it reads
 *   the satellites while it counts, if the reader lets it. Off unless the reader turns it on.
 * @property marks the open outing's signal on the way, as the reader chose it (Phase 21.2); null
 *   until they do, and then it follows [gps] ([marksInForce]). Read only for an open plan.
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
    val gps: Boolean = false,
    val marks: SessionMarks? = null,
) {
    /** It has no goal: the open outing (Phase 21). */
    val open: Boolean get() = goalKind == SessionGoalKind.OPEN

    /**
     * The mark an open outing of this plan tells: the reader's choice, or, with none made, the
     * kilometre where the plan measures with GPS and ten minutes where it does not. A kilometre
     * told from the steps alone carries the step length's error; ten minutes are the steps' own.
     * The plan's switch decides, not the phone: a plan with GPS that starts on the steps still
     * tells its kilometres, estimated, as the reader expects from it.
     */
    val marksInForce: SessionMarks
        get() = marks ?: if (gps) SessionMarks.DISTANCE else SessionMarks.TIME
}

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
 * @property gps it is measured with GPS (docs/adr/0016-location.md): decided at the start, from
 *   the plan and from what the reader and the phone allowed then, and frozen like the goal. Its
 *   [totals]' distance is then the one fused from the fixes and the steps.
 * @property stepsMeters what the steps alone would have said of an outing with GPS, kept beside
 *   the fused distance; null without GPS, where the distance already is the steps'.
 * @property measuredShare the share of the distance that came from fixes, 0 to 1; null without
 *   GPS. The app says «measured» only at nine tenths or more.
 * @property capAtMillis when it ends by itself if nobody says to go on: asked 15 minutes before,
 *   and moved on by «Keep going». Null for an outing from before it was kept: the longest outing
 *   from its start.
 * @property capAskedForMillis the cap the question was last asked for, so it is asked once a cap
 *   and not again by a process that restarted.
 * @property ascentMeters what an outing with GPS climbed, an estimate from the blended
 *   altitudes of its route (Phase 16), and [descentMeters] what it came down; null without GPS,
 *   and until an altitude is known.
 * @property legs an outing with GPS, a hundred metres at a time: what its splits are cut from.
 * @property pauses its pauses that ended inside it, in order (Phase 21): none for an outing from
 *   before they were kept, which only [pausesKept] tells from one never paused.
 * @property pausesKept its pauses are known (Phase 21.3): it was walked by a Passo that keeps them,
 *   so an empty [pauses] means it was never paused, and its minutes can be drawn with their gaps.
 *   False for an outing from before, whose minutes in a pause would be drawn as walked.
 * @property marks an open outing's signal on the way (Phase 21.2), frozen at its start as the goal
 *   is; [SessionMarks.NONE] for every other outing, which tells shares of its goal instead.
 * @property markMiles its distance marks are miles, the reader walking in miles when it started:
 *   frozen, so a change of units on the way does not change the mark under the reader's feet.
 * @property toldMarks the marks already told, so none is told twice, not even by a process that
 *   restarted.
 * @property roundLine an open outing with GPS on one of the reader's usual rounds (Phase 20): where
 *   it stands on it, as last told; null for every other outing, and until it is found on one.
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
    val gps: Boolean = false,
    val stepsMeters: Double? = null,
    val measuredShare: Double? = null,
    val capAtMillis: Long? = null,
    val capAskedForMillis: Long? = null,
    val ascentMeters: Double? = null,
    val descentMeters: Double? = null,
    val legs: List<OutingLeg> = emptyList(),
    val pauses: List<SessionPause> = emptyList(),
    val pausesKept: Boolean = false,
    val marks: SessionMarks = SessionMarks.NONE,
    val markMiles: Boolean = false,
    val toldMarks: Int = 0,
    val roundLine: RoundLine? = null,
) {
    val reached: Boolean get() = reachedAtMillis != null
    val live: Boolean get() = state != SessionState.FINISHED

    /** It has no goal: the open outing (Phase 21). */
    val open: Boolean get() = goalKind == SessionGoalKind.OPEN
}
