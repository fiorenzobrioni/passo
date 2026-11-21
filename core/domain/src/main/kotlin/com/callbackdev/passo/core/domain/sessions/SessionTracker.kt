package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.metrics.StepLengths
import com.callbackdev.passo.core.domain.rounds.RoundTimes
import com.callbackdev.passo.core.domain.rounds.RoundWatch
import com.callbackdev.passo.core.domain.routes.DistanceKind
import com.callbackdev.passo.core.domain.routes.Fix
import com.callbackdev.passo.core.domain.routes.LegPiece
import com.callbackdev.passo.core.domain.routes.Legs
import com.callbackdev.passo.core.domain.routes.RouteConstants.PACE_NOW_MILLIS
import com.callbackdev.passo.core.domain.routes.RouteFilter
import com.callbackdev.passo.core.domain.routes.RouteMeter
import com.callbackdev.passo.core.domain.routes.RouteSettlement
import com.callbackdev.passo.core.domain.routes.distanceKind
import com.callbackdev.passo.core.domain.sessions.SessionConstants.CADENCE_WINDOW_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.CAP_EXTENSION_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.CAP_WARNING_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.IDLE_END_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.KEEP_GOING_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MAX_MILLIS_PER_STEP
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MIN_CADENCE_SPAN_MILLIS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.MIN_KEPT_STEPS
import com.callbackdev.passo.core.domain.sessions.SessionConstants.PAUSE_END_MILLIS
import com.callbackdev.passo.core.domain.ways.WayStop
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.RoundLine
import com.callbackdev.passo.core.model.RoundStanding
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionPause
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.StepLengthSample
import kotlin.math.roundToInt

/** What the tracker has to tell after a step or a check. */
sealed interface SessionSignal {
    /**
     * A share of the goal was crossed: told once. When one batch of steps crosses several, only
     * the highest is told (three vibrations and then two would say nothing clear).
     */
    data class Milestone(val milestone: SessionMilestone) : SessionSignal

    /**
     * A city walk's places, reached by these steps (Phase 11), in the order they come: each told
     * once, as the walk's distance passes it. Several at once are all named, the last one said.
     */
    data class Places(val places: List<WayStop>) : SessionSignal

    /**
     * An open outing crossed a mark (Phase 21, step 21.2): its [count]th kilometre or mile, or its
     * [count]th ten minutes in motion. When one batch crosses several, only the last is told: two
     * pulses in a row would read as one mark, or as a quarter of a goal it does not have.
     */
    data class Mark(val count: Int) : SessionSignal

    /**
     * An open outing's line on a usual round changed (Phase 20): found, compared at a kilometre, or
     * left. Told silently: the card and the notification say it, the kilometre's own words say it
     * where the outing speaks ([Mark]), and nothing vibrates or wakes for it.
     */
    data class RoundLineChanged(val line: RoundLine) : SessionSignal

    /**
     * An interval outing changed interval (Phase 13): [position] is the one that begins. When one
     * batch crosses several changes (a long stillness of the reports), only the latest is told;
     * the splits are right either way. Never the end of the last interval: that is the goal.
     *
     * @property changedAtMillis when the change fell, from the steps' own timestamps; for an
     *   [early] one, when it is predicted to fall.
     * @property early told at the report before the one that would cross it, the change being
     *   due before the next report (docs/adr/0013-interval-walks.md).
     */
    data class IntervalChange(val position: IntervalPosition, val changedAtMillis: Long, val early: Boolean = false) :
        SessionSignal

    /**
     * The outing is within [SessionConstants.CAP_WARNING_MILLIS] of its cap, and the reader is
     * walking: «Still walking?», asked once a cap (docs/adr/0016-location.md, decision 14). No
     * cap ends a walk unasked; unanswered, it ends at [capAtMillis] and can still be taken back.
     */
    data class CapAhead(val capAtMillis: Long) : SessionSignal

    /**
     * The outing is over. [kept] is false for one too short to be worth keeping
     * ([SessionConstants.MIN_KEPT_STEPS]), which the caller deletes rather than records.
     */
    data class Finished(val session: Session, val kept: Boolean) : SessionSignal
}

/**
 * What an outing with GPS has to keep since it was last asked: the route's points and what its
 * clean stretches said of the step. Only what belongs to the outing for good: what was walked
 * after an end that can still be taken back waits aside, and comes here if it is.
 */
data class RouteHarvest(val points: List<RoutePoint>, val samples: List<StepLengthSample>) {
    val isEmpty: Boolean get() = points.isEmpty() && samples.isEmpty()
}

/**
 * An outing, measured from the steps as they come (PLANNING.md §11 Phase 10). Pure: the tracking
 * service feeds it every accounted sample with its own time, and it says what to tell.
 *
 * - **Time in motion** is made of the gaps between steps, each credited with at most
 *   [MAX_MILLIS_PER_STEP] per step: no timer runs, and standing still adds nothing.
 * - **The cadence** is the last [CADENCE_WINDOW_MILLIS] of steps, over the time in motion they
 *   took; it decides the step length and the energy cost of each step, as the minute's cadence
 *   does for the day, and whether the time is at the outing's intensity.
 * - **Signals** come from the steps: the milestones the reader chose, then the goal, which ends
 *   the outing. For [KEEP_GOING_MILLIS] after it the steps are still counted aside, so "Keep
 *   going" reopens it with them. An open outing (Phase 21) has neither: its progress is always
 *   0, so it crosses no share, and it ends only by the reader, the stillness, a long pause or its
 *   cap, which is [SessionConstants.OPEN_MAX_SESSION_MILLIS] from its start. Its signals are its
 *   marks ([OutingMarks], step 21.2): a kilometre on settled metres, as a distance goal's
 *   milestones are, or ten minutes in motion; counted in [Session.toldMarks], so none is told
 *   twice, and none while it is over.
 * - **It ends by itself** after [IDLE_END_MILLIS] without a step (at its last step), a pause of
 *   [PAUSE_END_MILLIS], or at its cap ([capMillis]); noticed at the next step or [check]. An end
 *   by stillness or by the cap can be taken back like the goal's, for [KEEP_GOING_MILLIS] from
 *   the moment it was noticed: a chat with a friend is a stop, not the end of the walk.
 * - **Before its cap it asks** ([SessionSignal.CapAhead]), at the first steps within
 *   [CAP_WARNING_MILLIS] of it, and [goOn] moves the cap [CAP_EXTENSION_MILLIS] on: no cap ends
 *   a walk unasked (docs/adr/0016-location.md, decision 14). A reader who is sitting is not
 *   asked: no step brings the question, and the stillness ends the outing as always.
 * - **An interval outing** (Phase 13) cuts the same time in motion into its intervals
 *   ([IntervalSchedule]): each batch is laid on the splits, cut where it crosses a change, and
 *   the latest change it crossed is told; one due before the next report, while walking, is told
 *   at this one ([SessionConstants.EARLY_CHANGE_MILLIS]).
 * - **An outing measured with GPS** (Phase 15, docs/adr/0016-location.md) has its distance, and
 *   the energy with it, from a [RouteMeter]: the steps are handed to it with the fixes ([onFix]),
 *   and the distance is added when it is settled ([settle]), so its goal, its milestones and a
 *   walk's places are told on metres the fixes cannot take back. Steps, time in motion and the
 *   intervals are the steps' as ever, and told with them.
 * - **An open outing with GPS on a usual round** (Phase 20) has its line: the points the meter
 *   settles are laid along the reader's rounds ([RoundWatch]); once on one, from the first
 *   kilometre (or mile, the marks' unit) it says how far ahead of the usual or behind, again at
 *   each, and once that the round was left. It rides on the batch that settles the kilometre, as
 *   the kilometre's mark does: no signal of its own, no wake ([SessionSignal.RoundLineChanged]).
 *
 * Not thread-safe: the service drives it from one thread.
 *
 * @param meter the meter of an outing with GPS; ignored for any other outing.
 * @param rounds the reader's rounds, for an open outing with GPS; ignored for any other, whose
 *   sentence is its goal's (a goal outing on a round is told so on its page, afterwards).
 */
class SessionTracker(
    initial: Session,
    private val lengths: StepLengths,
    private val weightKg: Double,
    meter: RouteMeter? = null,
    rounds: RoundWatch? = null,
) {
    var session: Session = initial
        private set

    private val route: RouteMeter? = meter.takeIf { initial.gps }

    private val watch: RoundWatch? = rounds.takeIf { initial.gps && initial.open && route != null }

    private val window = ArrayDeque<Mark>()
    private var windowFrom: Long = initial.lastEventAtMillis

    // The cadence the last measured batch was walked at: the meter's batches carry it.
    private var measuredCadence: Int? = null

    // After the goal or a long stillness: from when "Keep going" can reopen it (null when it
    // cannot), the steps since, and the time they reach.
    private var reopenableSince: Long? = null
    private var overtime = SessionTotals()
    private var overtimeLastEvent: Long? = null

    // An outing with GPS: the metres of its distance that came from fixes; what is to be kept
    // of its route; and what was walked after an end that can be taken back, kept aside.
    private var measuredMeters: Double = (initial.measuredShare ?: 0.0) * initial.totals.distanceMeters
    private val keptPoints = ArrayList<RoutePoint>()
    private val keptSamples = ArrayList<StepLengthSample>()
    private var overtimeMeasuredMeters = 0.0
    private var overtimeStepsMeters = 0.0
    private val asidePoints = ArrayList<RoutePoint>()
    private val asideSamples = ArrayList<StepLengthSample>()
    private val asidePieces = ArrayList<LegPiece>()

    // What a cut of the line crossed (a pause, the service going), for the service to tell.
    private val held = ArrayList<SessionSignal>()

    // The last interval whose start was told: the one under way when the tracker was made (its
    // start was told then, or by the process before this one), then each change as it is told.
    private var toldInterval: Int = IntervalSchedule.of(initial)?.indexAt(initial.totals.movingMillis) ?: 0

    /** Whether this outing is measured with GPS, by a meter. */
    val measuring: Boolean get() = route != null

    /**
     * The time in motion to the next moment of an interval outing worth telling on time (a change
     * or the goal), while it counts; null for any other outing, or when nothing is left to tell.
     * What sets the step counter's report latency (docs/adr/0013-interval-walks.md).
     */
    fun untilIntervalSignal(): Long? {
        val current = session
        if (current.state != SessionState.ACTIVE) return null
        return IntervalSchedule.of(current)?.untilSignal(current.totals.movingMillis)
    }

    /**
     * The last step seen, the ones kept aside after an end that can be taken back included: what
     * says whether the reader is walking now, whichever side of the end they are on.
     */
    val lastMotionAtMillis: Long get() = overtimeLastEvent ?: session.lastStepAtMillis

    /**
     * The pace of the last stretch of an outing with GPS, in seconds in motion a kilometre; null
     * for any other outing, before enough has been measured, while it does not count, and once
     * the reader has stopped for [RouteConstants.PACE_NOW_MILLIS]: the pace of a stretch walked
     * a while ago is not the pace now.
     */
    fun paceNowAt(nowMillis: Long): Double? {
        if (session.state != SessionState.ACTIVE || nowMillis - session.lastStepAtMillis > PACE_NOW_MILLIS) return null
        return route?.paceNow()
    }

    /** What the steps estimate for the stretch whose fixes are not in yet: shown, never told on. */
    val openMeters: Double get() = route?.openMeters ?: 0.0

    /** The last fix the meter accepted since its line began; null before the first. */
    val lastFixAtMillis: Long? get() = route?.lastFixAtMillis

    /** The end of the last batch of steps the meter settled, its fixes known to be in. */
    val settledThroughMillis: Long? get() = route?.settledThrough

    /** How many fixes the meter took, for the outing's log row. */
    val fixesTaken: Int get() = route?.taken ?: 0

    /** How many it refused. */
    val fixesRefused: Int get() = route?.refused ?: 0

    /** Accounts [steps] taken at [atMillis]; returns what to tell, in order. */
    fun onSteps(atMillis: Long, steps: Int): List<SessionSignal> {
        if (steps <= 0) return emptyList()
        return when (session.state) {
            SessionState.PAUSED -> emptyList()

            SessionState.FINISHED -> {
                countOvertime(atMillis, steps)
                emptyList()
            }

            SessionState.ACTIVE -> countActive(atMillis, steps)
        }
    }

    /**
     * A fix of an outing with GPS, while it counts or can still be taken back; null where there
     * is nothing to give it to (another outing, a paused one, one over for good).
     */
    fun onFix(fix: Fix): RouteFilter.Verdict? {
        val meter = route ?: return null
        val listening = session.state == SessionState.ACTIVE ||
            (session.state == SessionState.FINISHED && reopenableSince != null)
        return if (listening) meter.onFix(fix, cadenceAt(fix.atMillis)) else null
    }

    /**
     * Settles the distance of an outing with GPS through [fixesThroughMillis], the moment through
     * which the receiver's fixes are known to be in, and says what the settled metres crossed: a
     * milestone, a walk's places, the goal, which ends it at [atMillis].
     */
    fun settle(fixesThroughMillis: Long, atMillis: Long): List<SessionSignal> {
        val meter = route ?: return emptyList()
        return take(meter.settle(fixesThroughMillis), atMillis)
    }

    /**
     * What an outing with GPS climbed and came down since it was last told, counted on the
     * altitudes of the points it keeps ([upMeters], [downMeters]; both zero says only that its
     * height is now known). The climb goes to the leg that was being walked at [atMillis], and
     * what it cost to the outing's energy: the slope's term, over and above the flat's
     * (PLANNING.md §11 Phase 16, «Energy on a slope»), at the cadence that leg was walked at, the
     * climb being told up to a minute and a half after it (its points wait for their pressure)
     * and the reader may have gone from a run to a walk meanwhile. Nothing for an outing without
     * GPS: its energy, like the day's, is the steps' alone.
     */
    fun addClimb(atMillis: Long, upMeters: Double, downMeters: Double) {
        if (route == null) return
        val up = upMeters.coerceAtLeast(0.0)
        val down = downMeters.coerceAtLeast(0.0)
        val current = session
        val cadence = Legs.cadenceAt(current.legs, atMillis) ?: measuredCadence
        val cost = up * weightKg * MetricsCalculator.kcalPerKgPerMeterClimbed(cadence ?: 0)
        session = current.copy(
            ascentMeters = (current.ascentMeters ?: 0.0) + up,
            descentMeters = (current.descentMeters ?: 0.0) + down,
            legs = Legs.climb(current.legs, atMillis, up, down),
            totals = current.totals.copy(activeKcal = current.totals.activeKcal + cost),
        )
    }

    /** What is to be kept of the route since the last call: for the service to write. */
    fun takeRoute(): RouteHarvest {
        val harvest = RouteHarvest(keptPoints.toList(), keptSamples.toList())
        keptPoints.clear()
        keptSamples.clear()
        return harvest
    }

    /**
     * The cadence at [nowMillis], from the steps of the last [CADENCE_WINDOW_MILLIS]; null before
     * there is enough of it to be a pace, or while paused.
     *
     * Each sample's steps are measured against the time in motion they took, not against the
     * window: a sensor with no FIFO hands over a minute and a half of walking in one sample, and
     * those 170 steps inside a half-minute window would read as 340 a minute, a run, pricing the
     * outing's distance with the running step. The time since the last sample is added, so the
     * cadence falls when the reader stops.
     */
    fun cadenceAt(nowMillis: Long): Int? {
        if (session.state == SessionState.PAUSED) return null
        val from = maxOf(nowMillis - CADENCE_WINDOW_MILLIS, windowFrom)
        val marks = window.filter { it.atMillis > from && it.atMillis <= nowMillis }
        val idle = nowMillis - (marks.lastOrNull()?.atMillis ?: from)
        val span = marks.sumOf { it.movingMillis } + idle
        if (span < MIN_CADENCE_SPAN_MILLIS) return null
        return (marks.sumOf { it.steps } * MILLIS_PER_MINUTE.toDouble() / span).roundToInt()
    }

    /** Ends it by itself when it has been still, paused or open for too long. */
    fun check(nowMillis: Long): List<SessionSignal> {
        val current = session
        return when (current.state) {
            SessionState.ACTIVE -> when {
                nowMillis - current.lastStepAtMillis > IDLE_END_MILLIS ->
                    listOf(finish(current.lastStepAtMillis, SessionEnd.IDLE, reopenableFrom = nowMillis))

                nowMillis > current.capMillis ->
                    listOf(finish(current.lastStepAtMillis, SessionEnd.CLOSED, reopenableFrom = nowMillis))

                else -> emptyList()
            }

            SessionState.PAUSED -> {
                val pausedAt = current.pausedAtMillis ?: nowMillis
                if (nowMillis - pausedAt > PAUSE_END_MILLIS) listOf(finish(pausedAt, SessionEnd.IDLE)) else emptyList()
            }

            SessionState.FINISHED -> emptyList()
        }
    }

    fun pause(nowMillis: Long): Boolean {
        if (session.state != SessionState.ACTIVE) return false
        // The line is cut at a pause: what was walked up to it is settled with the fixes in hand.
        // Its last metres may reach the goal: the outing then ended there, and is not paused.
        cutLine(nowMillis)
        if (session.state != SessionState.ACTIVE) return true
        session = session.copy(state = SessionState.PAUSED, pausedAtMillis = nowMillis)
        return true
    }

    /**
     * The service is going (stopped by the system, or the phone shutting down): the line of an
     * outing with GPS is cut where it stands, every step up to it settled with the fixes in hand,
     * as at a pause, so no metre the meter held in memory goes with it. The outing counts on, and
     * the next start draws a line of its own.
     */
    fun cutRoute(atMillis: Long) = cutLine(atMillis)

    /**
     * What the last cut of the line crossed, as the metres it settled are told: a milestone, a
     * walk's place, the goal, which ends the outing there. Taken once.
     */
    fun takeHeldSignals(): List<SessionSignal> = held.toList().also { held.clear() }

    /**
     * Cuts the line of an outing with GPS that counts: every step up to [atMillis] settled with
     * the fixes in hand, and taken as any settled metres are, so what they cross is told (held
     * for the service, [takeHeldSignals]) and a goal they reach ends the outing at it.
     */
    private fun cutLine(atMillis: Long) {
        val meter = route ?: return
        if (session.state != SessionState.ACTIVE) return
        held += take(meter.close(), atMillis)
    }

    /**
     * Counting again from now: the pause is neither motion nor stillness, nor time towards the
     * cap. The cap moves on by the pause, so a reader who paused across it is asked as any other
     * before it ends (no cap ends a walk unasked). A question already open stays open: the cap
     * is as far after the pause as it was before it, within the quarter of an hour it was asked in.
     * The pause is kept with the outing (Phase 21): its page says the time paused, and its
     * minutes have a gap there.
     */
    fun resume(nowMillis: Long): Boolean {
        val current = session
        if (current.state != SessionState.PAUSED) return false
        val pausedAt = current.pausedAtMillis ?: nowMillis
        val pausedFor = (nowMillis - pausedAt).coerceAtLeast(0L)
        val cap = current.capMillis + pausedFor
        session = current.copy(
            state = SessionState.ACTIVE,
            pausedAtMillis = null,
            pauses = if (pausedFor > 0) current.pauses + SessionPause(pausedAt, nowMillis) else current.pauses,
            lastEventAtMillis = nowMillis,
            lastStepAtMillis = nowMillis,
            capAtMillis = cap,
            capAskedForMillis = if (current.capAskedForMillis == current.capMillis) cap else current.capAskedForMillis,
        )
        window.clear()
        windowFrom = nowMillis
        return true
    }

    /** The reader stops it: now, or where it was paused. Null if it is already over. */
    fun stop(nowMillis: Long): SessionSignal.Finished? {
        val current = session
        if (!current.live) return null
        val at = if (current.state == SessionState.PAUSED) current.pausedAtMillis ?: nowMillis else nowMillis
        return finish(maxOf(at, current.startedAtMillis), SessionEnd.STOPPED)
    }

    /**
     * Whether "Keep going" can still reopen it: after its goal, a long stillness or its cap (not
     * a pause left too long, nor the reader's own stop), and not too long after.
     */
    fun canKeepGoing(nowMillis: Long): Boolean {
        val since = reopenableSince ?: return false
        return session.state == SessionState.FINISHED && nowMillis - since <= KEEP_GOING_MILLIS
    }

    /**
     * «Keep going», said to the question before the cap: the cap moves [CAP_EXTENSION_MILLIS]
     * on, and nothing stops. False when there is no question to answer (not counting, or not
     * asked yet for this cap): the cap is the reader's to move, not anyone's by accident.
     */
    fun goOn(nowMillis: Long): Boolean {
        val current = session
        val cap = current.capMillis
        if (current.state != SessionState.ACTIVE || current.capAskedForMillis != cap || nowMillis > cap) return false
        session = current.copy(capAtMillis = cap + CAP_EXTENSION_MILLIS)
        return true
    }

    /**
     * Reopens an outing ended by its goal, by a long stillness or by its cap, with the steps
     * taken since; the stillness itself adds nothing. One ended by its cap gets its cap moved
     * [CAP_EXTENSION_MILLIS] on, as if the question had been answered in time. Returns what
     * those steps crossed on the way (a milestone, or the goal, which ends it again), or null
     * when it cannot be reopened.
     */
    fun keepGoing(nowMillis: Long): List<SessionSignal>? {
        if (!canKeepGoing(nowMillis)) return null
        val current = session
        val lastEvent = overtimeLastEvent ?: current.endedAtMillis ?: nowMillis
        val cap = if (current.end ==
            SessionEnd.CLOSED
        ) {
            current.capMillis + CAP_EXTENSION_MILLIS
        } else {
            current.capAtMillis
        }
        val reopened = current.copy(
            state = SessionState.ACTIVE,
            end = null,
            endedAtMillis = null,
            lastEventAtMillis = lastEvent,
            lastStepAtMillis = maxOf(lastEvent, nowMillis),
            capAtMillis = cap,
            stepsMeters = current.stepsMeters?.let { it + overtimeStepsMeters },
            legs = asidePieces.fold(current.legs, Legs::lay),
        )
        val since = overtime
        // What was kept aside is the outing's again: the route taken back has no hole.
        measuredMeters += overtimeMeasuredMeters
        watch?.add(asidePoints)
        keptPoints += asidePoints
        keptSamples += asideSamples
        clearOvertime()
        reopenableSince = null
        // The marks the steps aside crossed are passed by in silence: they were walked while the
        // outing was over, and a pulse under the reader's own touch of «Resume» would tell them
        // nothing. The next is told as it comes.
        val signals = advance(reopened, since, nowMillis).filterNot { it is SessionSignal.Mark }
        withMeasuredShare()
        return signals
    }

    private fun countActive(atMillis: Long, steps: Int): List<SessionSignal> {
        val current = session
        // A step after a long stillness belongs to the day, not to an outing that was over; it is
        // kept aside, as after the goal, in case the reader takes the end back.
        if (atMillis - current.lastStepAtMillis > IDLE_END_MILLIS) {
            val finished = finish(current.lastStepAtMillis, SessionEnd.IDLE, reopenableFrom = atMillis)
            countOvertime(atMillis, steps)
            return listOf(finished)
        }
        // Past its cap, unanswered: over, and told. The reader who walks on can take it back.
        if (atMillis > current.capMillis) {
            val finished = finish(current.lastStepAtMillis, SessionEnd.CLOSED, reopenableFrom = atMillis)
            countOvertime(atMillis, steps)
            return listOf(finished)
        }
        var added = measure(atMillis, steps, current.lastEventAtMillis)
        var moved = current.copy(
            lastEventAtMillis = maxOf(atMillis, current.lastEventAtMillis),
            lastStepAtMillis = maxOf(atMillis, current.lastStepAtMillis),
        )
        val meter = route
        if (meter != null) {
            // The distance, and the energy with it, come when the meter settles these steps
            // with their fixes; what the steps alone say is kept beside it.
            meter.onSteps(
                fromMillis = current.lastEventAtMillis,
                toMillis = atMillis,
                steps = steps,
                movingMillis = added.movingMillis,
                stepsMeters = added.distanceMeters,
                cadence = measuredCadence,
            )
            moved = moved.copy(stepsMeters = (moved.stepsMeters ?: 0.0) + added.distanceMeters)
            added = added.copy(distanceMeters = 0.0, activeKcal = 0.0)
        }
        val signals = advance(moved, added, atMillis)
        return signals + listOfNotNull(capAhead(atMillis))
    }

    /**
     * «Still walking?», once a cap: at the first steps within [CAP_WARNING_MILLIS] of it. It
     * rides on a step batch, as every signal does: no timer, and a reader who is not walking is
     * not asked.
     */
    private fun capAhead(atMillis: Long): SessionSignal.CapAhead? {
        val current = session
        if (current.state != SessionState.ACTIVE) return null
        val cap = current.capMillis
        if (atMillis < cap - CAP_WARNING_MILLIS || current.capAskedForMillis == cap) return null
        session = current.copy(capAskedForMillis = cap)
        return SessionSignal.CapAhead(cap)
    }

    /**
     * Adds [added] to [moved] (the outing with its times already brought forward) and says what
     * it crossed: the highest milestone, and the goal, which ends it at [atMillis].
     *
     * @param steps [added] is a batch of steps; false for metres settled by the meter, which
     *   move no interval: an interval outing's changes ride on the steps alone, and the fixes
     *   change nothing of their timing.
     */
    private fun advance(
        moved: Session,
        added: SessionTotals,
        atMillis: Long,
        steps: Boolean = true,
    ): List<SessionSignal> {
        val before = moved.progress()
        var next = moved.copy(totals = moved.totals + added)
        val schedule = if (steps) IntervalSchedule.of(moved) else null
        if (schedule != null) {
            val splits = schedule.lay(moved.splits, moved.totals.movingMillis, added)
            // The time at pace of an interval outing is the fast minutes' only: the slow ones
            // have no pace to keep, and the minutes after its goal no interval.
            next = next.copy(
                splits = splits,
                totals = next.totals.copy(zoneMillis = splits.filter { it.fast }.sumOf { it.zoneMillis }),
            )
        }
        val after = next.progress()
        val crossed = (next.milestones + SessionMilestone.GOAL).filter {
            it !in next.toldMilestones && before < it.fraction && after >= it.fraction
        }
        val signals = mutableListOf<SessionSignal>()
        placesPassed(moved, next)?.let { signals += it }
        if (crossed.isNotEmpty()) {
            next = next.copy(toldMilestones = next.toldMilestones + crossed)
            signals += SessionSignal.Milestone(crossed.maxBy { it.percent })
        }
        // Before the kilometre's mark: its words say the round's line as it stands at that kilometre.
        val followed = followRound(next)
        if (followed.roundLine != next.roundLine) {
            next = followed
            followed.roundLine?.let { signals += SessionSignal.RoundLineChanged(it) }
        }
        val marks = OutingMarks.crossed(next)
        if (marks > next.toldMarks) {
            next = next.copy(toldMarks = marks)
            signals += SessionSignal.Mark(marks)
        }
        if (schedule != null && SessionMilestone.GOAL !in crossed) {
            intervalChange(schedule, moved.totals.movingMillis, next, atMillis)?.let { signals += it }
        }
        session = next
        if (SessionMilestone.GOAL in crossed && !next.reached) {
            session = next.copy(reachedAtMillis = atMillis)
            signals += finish(atMillis, SessionEnd.GOAL, reopenableFrom = atMillis)
        }
        return signals
    }

    /**
     * The change [next] crossed since [fromMillis] of time in motion, the latest if several, or
     * the one due before the next report while the reader walks; null when there is none to tell.
     */
    private fun intervalChange(
        schedule: IntervalSchedule,
        fromMillis: Long,
        next: Session,
        atMillis: Long,
    ): SessionSignal.IntervalChange? {
        val moving = next.totals.movingMillis
        val crossed = schedule.changesBetween(fromMillis, moving).filter { it > toldInterval }
        if (crossed.isNotEmpty()) {
            val latest = crossed.last()
            toldInterval = latest
            // The batch's time in motion ends at its last step: the change fell that much earlier.
            val fellAt = atMillis - (moving - schedule.startOf(latest))
            return SessionSignal.IntervalChange(schedule.at(moving), fellAt)
        }
        if (next.state != SessionState.ACTIVE) return null
        val until = schedule.untilChange(moving) ?: return null
        val coming = schedule.indexAt(moving) + 1
        if (until > SessionConstants.EARLY_CHANGE_MILLIS || coming <= toldInterval) return null
        val walking = (cadenceAt(atMillis) ?: 0) >= MetricsConstants.ACTIVE_MINUTE_THRESHOLD
        if (!walking) return null
        toldInterval = coming
        return SessionSignal.IntervalChange(schedule.at(schedule.startOf(coming)), atMillis + until, early = true)
    }

    /**
     * Where [next], an open outing with GPS, stands on the reader's rounds (Phase 20): found on one
     * from its first half kilometre, compared with its usual at each kilometre it crosses, and left
     * once it has been off its line for a few hundred metres; the kilometre after that says so, and
     * the line stops. Any other outing, and one paused or over, is left as it is.
     */
    private fun followRound(next: Session): Session {
        val watch = watch ?: return next
        if (next.state != SessionState.ACTIVE) return next
        val line = next.roundLine
        val crossed = OutingMarks.distanceCrossed(next)
        val walked = next.totals.distanceMeters
        val followed = when (line?.standing) {
            null -> watch.on(null, walked)?.let { compared(next, RoundLine(it), crossed) }

            RoundStanding.ON -> when (val on = watch.on(line.roundId, walked)) {
                // Left: said on the card from now, and by the next kilometre's words. Counted from
                // the kilometres already behind, so that next one is the next to come.
                null -> line.copy(standing = RoundStanding.LEFT, marks = crossed)

                // Two rounds from the same door, and the walk took the other's streets.
                line.roundId -> if (crossed > line.marks) compared(next, line, crossed) else line

                else -> compared(next, RoundLine(on), crossed)
            }

            RoundStanding.LEFT ->
                if (crossed > line.marks) line.copy(standing = RoundStanding.GONE, marks = crossed) else line

            RoundStanding.GONE -> line
        }
        return if (followed == line) next else next.copy(roundLine = followed)
    }

    /**
     * [line] compared at the [crossed]th kilometre of [session]: its time in motion to it, less the
     * round's usual there. Only for a kilometre measured (nine tenths from fixes: a comparison of
     * times over an estimate is the cadence in disguise) that the round's walks reached; otherwise
     * [line] as it was, its last comparison held.
     */
    private fun compared(session: Session, line: RoundLine, crossed: Int): RoundLine {
        val watch = watch ?: return line
        if (crossed <= 0 || session.distanceKind() != DistanceKind.MEASURED) return line
        val usual = watch.usualAt(line.roundId, crossed) ?: return line
        val mine = OutingMarks.movingMillisTo(session, crossed)
        return line.copy(marks = crossed, gapMillis = RoundTimes.even(mine - usual, usual))
    }

    /**
     * The walk's places between where [before] and [after] stand on it: never the one it started
     * at, which is where the reader already is. Measured from the totals, which are stored with
     * every batch, so a restarted process tells nothing twice.
     */
    private fun placesPassed(before: Session, after: Session): SessionSignal.Places? {
        val walk = after.walk ?: return null
        val from = after.walkFromMeters.toDouble()
        val was = from + before.totals.distanceMeters
        val now = from + after.totals.distanceMeters
        val places = Ways.of(walk).stops.filter { it.distanceMeters > was && it.distanceMeters <= now }
        return places.takeIf { it.isNotEmpty() }?.let { SessionSignal.Places(it) }
    }

    private fun countOvertime(atMillis: Long, steps: Int) {
        val since = reopenableSince ?: return
        val ended = session.endedAtMillis ?: return
        if (atMillis - since > KEEP_GOING_MILLIS) return
        // After a stillness the first gap is the stillness: [measure] credits it a step's worth.
        val last = overtimeLastEvent ?: ended
        var added = measure(atMillis, steps, last)
        val meter = route
        if (meter != null) {
            meter.onSteps(last, atMillis, steps, added.movingMillis, added.distanceMeters, measuredCadence)
            overtimeStepsMeters += added.distanceMeters
            added = added.copy(distanceMeters = 0.0, activeKcal = 0.0)
        }
        overtime += added
        overtimeLastEvent = maxOf(atMillis, last)
    }

    /** What [steps] at [atMillis] add, [lastEventAtMillis] being the moment measured to before. */
    private fun measure(atMillis: Long, steps: Int, lastEventAtMillis: Long): SessionTotals {
        val gap = (atMillis - lastEventAtMillis).coerceAtLeast(0)
        val moving = minOf(gap, steps * MAX_MILLIS_PER_STEP)
        window.addLast(Mark(atMillis, steps, moving))
        while (window.isNotEmpty() && window.first().atMillis <= atMillis - CADENCE_WINDOW_MILLIS) window.removeFirst()
        val cadence = cadenceAt(atMillis)
        measuredCadence = cadence
        val floor = session.intensity.cadenceFloor
        val inZone = floor == null || (cadence != null && cadence >= floor)
        val distance = steps * lengths.forCadence(cadence ?: 0)
        return SessionTotals(
            steps = steps,
            movingMillis = moving,
            zoneMillis = if (inZone) moving else 0,
            distanceMeters = distance,
            activeKcal = energy(distance, cadence),
        )
    }

    /** What walking [meters] at [cadence] cost, as the day's own estimate prices it. */
    private fun energy(meters: Double, cadence: Int?): Double =
        meters / METERS_PER_KM * weightKg * MetricsCalculator.kcalPerKgPerKm(cadence ?: 0)

    /** What the meter settled, as totals: the distance, and the energy each stretch cost. */
    private fun totals(settled: RouteSettlement) = SessionTotals(
        distanceMeters = settled.meters,
        activeKcal = settled.stretches.sumOf { energy(it.meters, it.cadence) },
    )

    /**
     * Takes what the meter settled: into the outing while it counts, telling what it crossed;
     * aside while an ended outing can still be taken back; nowhere once it cannot.
     */
    private fun take(settled: RouteSettlement, atMillis: Long): List<SessionSignal> {
        if (settled.isEmpty) return emptyList()
        return when {
            session.state == SessionState.ACTIVE -> {
                measuredMeters += settled.measuredMeters
                watch?.add(settled.points)
                keptPoints += settled.points
                keptSamples += settled.samples
                val laid = session.copy(legs = settled.pieces.fold(session.legs, Legs::lay))
                val signals = advance(laid, totals(settled), atMillis, steps = false)
                withMeasuredShare()
                signals
            }

            session.state == SessionState.FINISHED && reopenableSince != null -> {
                overtime += totals(settled)
                overtimeMeasuredMeters += settled.measuredMeters
                asidePoints += settled.points
                asideSamples += settled.samples
                asidePieces += settled.pieces
                emptyList()
            }

            else -> emptyList()
        }
    }

    /** What the meter gave as its line was cut: added with no signal, the outing pausing or ending. */
    private fun absorb(settled: RouteSettlement) {
        if (settled.isEmpty) return
        measuredMeters += settled.measuredMeters
        // Laid on the rounds as every point the route keeps: a restart reads them from the route.
        watch?.add(settled.points)
        keptPoints += settled.points
        keptSamples += settled.samples
        session = session.copy(
            totals = session.totals + totals(settled),
            legs = settled.pieces.fold(session.legs, Legs::lay),
        )
        withMeasuredShare()
    }

    /** The share of the distance that came from fixes, as the outing stands. */
    private fun withMeasuredShare() {
        if (route == null) return
        val distance = session.totals.distanceMeters
        val share = if (distance > 0) (measuredMeters / distance).coerceIn(0.0, 1.0) else 0.0
        session = session.copy(measuredShare = share)
    }

    private fun clearOvertime() {
        overtime = SessionTotals()
        overtimeLastEvent = null
        overtimeMeasuredMeters = 0.0
        overtimeStepsMeters = 0.0
        asidePoints.clear()
        asideSamples.clear()
        asidePieces.clear()
    }

    /** Ends it at [atMillis]; [reopenableFrom] is when "Keep going" starts to count, if it may. */
    private fun finish(atMillis: Long, end: SessionEnd, reopenableFrom: Long? = null): SessionSignal.Finished {
        // The line is cut at the end: every step up to it settled with the fixes in hand, none
        // lost. What is walked after it, while it can be taken back, starts a line of its own.
        val meter = route
        if (meter != null && session.state == SessionState.ACTIVE) absorb(meter.close())
        val current = session
        reopenableSince = reopenableFrom
        clearOvertime()
        val finished = current.copy(
            state = SessionState.FINISHED,
            end = end,
            endedAtMillis = maxOf(atMillis, current.startedAtMillis),
            pausedAtMillis = null,
        )
        session = finished
        return SessionSignal.Finished(finished, kept = finished.reached || finished.totals.steps >= MIN_KEPT_STEPS)
    }

    private data class Mark(val atMillis: Long, val steps: Int, val movingMillis: Long)

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val METERS_PER_KM = 1_000.0
    }
}

/** The share of the goal this milestone stands for. */
val SessionMilestone.fraction: Double get() = percent / PERCENT

private const val PERCENT = 100.0
