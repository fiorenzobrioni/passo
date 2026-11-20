package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.sessions.SessionConstants
import com.callbackdev.passo.core.domain.sessions.capMillis
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionState

/** What an outing's distance is, and so which word stands under it. */
enum class DistanceKind {
    /** From the steps and the step length: every outing without GPS. */
    ESTIMATED,

    /** At least nine tenths from fixes: the one distance the app calls «measured». */
    MEASURED,

    /** With GPS, and more than a tenth of it from the steps: said with that share. */
    PARTLY_MEASURED,
}

/**
 * «Measured» is a word for a distance at least [RouteConstants.MEASURED_SHARE] from fixes
 * (docs/adr/0016-location.md, decision 6); under it an outing with GPS says how much of its
 * distance the steps gave, and any other outing's distance is an estimate, as ever.
 */
fun Session.distanceKind(): DistanceKind = when {
    !gps -> DistanceKind.ESTIMATED
    (measuredShare ?: 0.0) >= RouteConstants.MEASURED_SHARE -> DistanceKind.MEASURED
    else -> DistanceKind.PARTLY_MEASURED
}

/** The share of an outing with GPS's distance that came from the steps, 0 to 1; null without GPS. */
fun Session.stepsShare(): Double? = if (gps) 1.0 - (measuredShare ?: 0.0).coerceIn(0.0, 1.0) else null

/**
 * The outing's pace, in seconds in motion a kilometre: only where the distance was measured
 * (docs/adr/0016-location.md, decision 7), since over an estimate it would be the cadence in
 * disguise. Null for any other outing, and under [RouteConstants.PACE_METERS].
 */
fun Session.pace(): Double? =
    if (distanceKind() == DistanceKind.MEASURED) Pace.secondsPerKm(totals.movingMillis, totals.distanceMeters) else null

/**
 * «Still walking?»: the minutes left to the cap of an outing that has asked and not been
 * answered (docs/adr/0016-location.md, decision 14), for the card to ask it too while it is on
 * screen; null when there is no question open. At least one: «ends in 0 minutes» is the end.
 */
fun Session.capQuestionMinutes(nowMillis: Long): Int? {
    val cap = capMillis
    if (state != SessionState.ACTIVE || capAskedForMillis != cap || nowMillis > cap) return null
    val left = (cap - nowMillis).coerceAtMost(SessionConstants.CAP_WARNING_MILLIS)
    return ((left + MILLIS_PER_MINUTE - 1) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1)
}

private const val MILLIS_PER_MINUTE = 60_000L
