package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.routes.RouteConstants.HOLE_MILLIS
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionState

/** What a touch on a [GpsLine] can do about it. */
enum class GpsLineAction {
    /** Give the receiver back to an outing that lost it: a touch, when location may be read. */
    RESUME_GPS,

    /** The system's own switch for location. */
    LOCATION_SETTINGS,

    /** Ask for the precise location: the system's request, or its page once that no longer comes. */
    ALLOW_LOCATION,
}

/**
 * What an outing's card says of the satellites (PLANNING.md §11 Phase 15, «What the reader sees
 * while it counts»): every state of the receiver has its line, and one only. Two states have
 * none of their own, on purpose: while it measures the distance says «measured» under itself,
 * and while the receiver sleeps at a stop the numbers stand and nothing is said.
 *
 * @property action the way out the line offers, if it has one.
 */
enum class GpsLine(val action: GpsLineAction? = null) {
    /** No fix yet since the line began (a start, a resume): the outing counts from the steps meanwhile. */
    FINDING,

    /** Fixes came, and none for a while as the reader walks (a tunnel, an arcade): the steps cover it. */
    SIGNAL_LOST,

    /** Location was turned off on the phone during the outing: as the tunnel, until it is back. */
    LOCATION_OFF(GpsLineAction.LOCATION_SETTINGS),

    /** The receiver was lost to a process the system restarted; the outing goes on from the steps. */
    STOPPED(GpsLineAction.RESUME_GPS),

    /** The permission went during the outing: it can only be asked for in the app. */
    PERMISSION_GONE(GpsLineAction.ALLOW_LOCATION),

    /** The plan asked for GPS and the reader had refused the permission: it started on the steps. */
    ON_STEPS_NO_PERMISSION(GpsLineAction.ALLOW_LOCATION),

    /** The plan asked for GPS and only the approximate location is granted: it started on the steps. */
    ON_STEPS_APPROXIMATE(GpsLineAction.ALLOW_LOCATION),

    /** The plan asked for GPS and location was off on the phone: it started on the steps. */
    ON_STEPS_LOCATION_OFF(GpsLineAction.LOCATION_SETTINGS),
}

/**
 * The receiver as the service holds it, for [GpsLines.of].
 *
 * @property typeHeld the service holds its `location` type in this process: a restarted one
 *   comes back without it.
 * @property readiness what the reader and the phone allow right now.
 * @property duty what the receiver is asked to do ([GpsDuty]).
 * @property awakeSinceMillis when the receiver was last asked for fixes, having been off or
 *   asleep; null while it is not on.
 * @property lastFixAtMillis the last fix accepted since the line began; null before the first.
 * @property settledThroughMillis the end of the last batch of steps whose fixes are known to
 *   be in: what «no fix for a while» is counted to, so that fixes still held by the receiver
 *   (the screen off) never read as a tunnel.
 */
data class ReceiverState(
    val typeHeld: Boolean,
    val readiness: GpsReadiness,
    val duty: GpsDuty.State,
    val awakeSinceMillis: Long?,
    val lastFixAtMillis: Long?,
    val settledThroughMillis: Long?,
)

/** Which [GpsLine] an outing's card shows. Pure: the service gives it what it knows. */
object GpsLines {
    /**
     * The line of an outing measured with GPS, while it counts. Paused or over it has none: the
     * receiver is off because the reader said so, or the outing ended.
     *
     * In order: what no touch on «Resume GPS» could mend comes first (the permission, the
     * phone's own switch), then the receiver lost, then what it is doing.
     */
    fun of(session: Session, receiver: ReceiverState): GpsLine? {
        if (!session.gps || session.state != SessionState.ACTIVE) return null
        return when (receiver.readiness) {
            GpsReadiness.PERMISSION_NEEDED, GpsReadiness.APPROXIMATE_ONLY -> GpsLine.PERMISSION_GONE

            GpsReadiness.LOCATION_OFF -> GpsLine.LOCATION_OFF

            // An outing brought by a backup to a phone with no receiver: nothing to offer.
            GpsReadiness.NO_RECEIVER -> null

            GpsReadiness.READY -> when {
                !receiver.typeHeld -> GpsLine.STOPPED
                receiver.duty != GpsDuty.State.AWAKE -> null
                receiver.lastFixAtMillis == null -> GpsLine.FINDING
                lost(receiver) -> GpsLine.SIGNAL_LOST
                else -> null
            }
        }
    }

    /**
     * The line of an outing that started on the steps although its plan asked for GPS: said
     * while it is under way or paused, with the way to change it for the next one.
     *
     * @param readinessAtStart what stood in the way when it started; null when nothing did, or
     *   it has been said already (a refusal is told once).
     */
    fun onSteps(session: Session, readinessAtStart: GpsReadiness?): GpsLine? {
        if (session.gps || !session.live) return null
        return when (readinessAtStart) {
            GpsReadiness.PERMISSION_NEEDED -> GpsLine.ON_STEPS_NO_PERMISSION
            GpsReadiness.APPROXIMATE_ONLY -> GpsLine.ON_STEPS_APPROXIMATE
            GpsReadiness.LOCATION_OFF -> GpsLine.ON_STEPS_LOCATION_OFF
            GpsReadiness.READY, GpsReadiness.NO_RECEIVER, null -> null
        }
    }

    /**
     * Whether a refusal that kept an outing on the steps is one to remember as told: the
     * permission's, which the reader decided. Location turned off on the phone is a passing
     * state, said each time it happens.
     */
    fun toldOnce(readinessAtStart: GpsReadiness): Boolean =
        readinessAtStart == GpsReadiness.PERMISSION_NEEDED || readinessAtStart == GpsReadiness.APPROXIMATE_ONLY

    /**
     * Walked for [HOLE_MILLIS] with no fix, as the meter itself counts a hole: from the last
     * fix, or from when the receiver woke if that is later (the seconds it takes to find its
     * place again after a stop are not a tunnel).
     */
    private fun lost(receiver: ReceiverState): Boolean {
        val fix = receiver.lastFixAtMillis ?: return false
        val through = receiver.settledThroughMillis ?: return false
        val since = maxOf(fix, receiver.awakeSinceMillis ?: fix)
        return through - since > HOLE_MILLIS
    }
}

/**
 * What an outing measured with GPS shows beyond its stored numbers, while a card or the
 * notification draws it.
 *
 * @property openMeters what the steps estimate for the stretch whose fixes are not in yet:
 *   shown with the distance, never told on (PLANNING.md §11 Phase 15, «Steps ahead of their
 *   fixes»).
 * @property paceNow the pace of the last stretch, in seconds in motion a kilometre; null before
 *   enough has been measured, and for an outing without GPS.
 * @property line what the card says of the receiver, if anything.
 */
data class LiveMeasure(val openMeters: Double = 0.0, val paceNow: Double? = null, val line: GpsLine? = null)
