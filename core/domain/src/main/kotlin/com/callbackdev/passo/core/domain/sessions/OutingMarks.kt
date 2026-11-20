package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.format.UnitConversions
import com.callbackdev.passo.core.domain.routes.DistanceKind
import com.callbackdev.passo.core.domain.routes.Splits
import com.callbackdev.passo.core.domain.routes.distanceKind
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionMarks
import kotlin.math.floor

/**
 * An open outing's marks on the way (PLANNING.md §11 Phase 21, step 21.2; docs/adr/0009-sessions.md,
 * decision 3): a kilometre or a mile, or ten minutes in motion. Pure: the tracker asks how many an
 * outing has crossed as it stands, and the voice what the last of them took.
 */
object OutingMarks {
    /**
     * A time mark: ten minutes in motion. Long enough that a walk of an hour is told six times and
     * not sixty, short enough that a reader without GPS hears about as often as one with it, at an
     * ordinary walk's ten or twelve minutes a kilometre.
     */
    const val TIME_MARK_MILLIS: Long = 10 * 60_000L

    /** A distance mark's length: a mile for a reader who walks in miles, a kilometre otherwise. */
    fun meters(miles: Boolean): Double = if (miles) UnitConversions.METERS_PER_MILE else METERS_PER_KM

    /**
     * How many marks [session] has crossed as it stands: the distance's kilometres or miles, or its
     * ten minutes in motion; none for an outing with no marks, which is every outing but an open
     * one that has them. The distance is the outing's own: settled metres with GPS, the steps'
     * estimate without.
     */
    fun crossed(session: Session): Int = when (session.marks) {
        SessionMarks.DISTANCE ->
            floor((session.totals.distanceMeters + EPSILON_METERS) / meters(session.markMiles)).toInt()

        SessionMarks.TIME -> (session.totals.movingMillis / TIME_MARK_MILLIS).toInt()

        SessionMarks.NONE -> 0
    }.coerceAtLeast(0)

    /**
     * The time in motion [session] took to reach its [count]th distance mark: from its legs where
     * it has them (an outing with GPS, whose kilometre is told when its metres are settled, and
     * may by then have walked on a little), from the outing's own time otherwise, which the batch
     * that crossed the mark brought to it.
     */
    fun movingMillisTo(session: Session, count: Int): Long {
        if (count <= 0) return 0L
        val splits = Splits.of(session.legs, meters(session.markMiles))
        val whole = splits.take(count)
        return if (whole.size == count && whole.all { it.whole }) {
            whole.sumOf { it.movingMillis }
        } else {
            session.totals.movingMillis
        }
    }

    /**
     * The pace of the stretch that ended at the [count]th distance mark, in seconds in motion a
     * kilometre: said only where the outing's distance is measured, at nine tenths or more
     * (docs/adr/0016-location.md, decision 7), and from that stretch's own legs, not the outing's
     * average. Null for any other outing, and for a stretch the legs do not hold whole.
     */
    fun paceOf(session: Session, count: Int): Double? {
        if (count <= 0 || session.distanceKind() != DistanceKind.MEASURED) return null
        val split = Splits.of(session.legs, meters(session.markMiles)).getOrNull(count - 1) ?: return null
        return split.takeIf { it.whole }?.secondsPerKm
    }

    private const val METERS_PER_KM = 1_000.0

    /** A kilometre summed from many pieces may fall a hair short of a thousand: it is a thousand. */
    private const val EPSILON_METERS = 1e-6
}
