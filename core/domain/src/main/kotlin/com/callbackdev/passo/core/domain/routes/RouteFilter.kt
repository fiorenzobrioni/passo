package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.metrics.MetricsConstants

/**
 * Which fixes an outing's route is built from (docs/adr/0016-location.md, decision 5): accurate
 * enough, later than the one before, and no faster from it than the reader's cadence allows.
 * What is refused is simply not used: the line goes from good fix to good fix.
 */
object RouteFilter {
    enum class Verdict {
        ACCEPTED,

        /** No accuracy given, or worse than [RouteConstants.MAX_ACCURACY_METERS]. */
        INACCURATE,

        /** Not later than the fix before, on the steps' clock: a repeat, or one out of order. */
        NOT_LATER,

        /** It implies a speed the cadence does not allow: a jump of the receiver's, or a vehicle. */
        TOO_FAST,

        /** From a test provider, in a build that does not take them. */
        MOCK,
    }

    /**
     * Judges [fix] against the last one accepted.
     *
     * @param cadence the reader's steps a minute now; null before there is a pace, which is read
     *   as walking: the stricter of the two speeds.
     * @param allowMock a debug build, where a route may be replayed on a desk with the system's
     *   mock-location setting. Never in a release: a walk is not to be faked there.
     */
    fun judge(fix: Fix, previous: Fix?, cadence: Int?, allowMock: Boolean): Verdict {
        if (fix.mock && !allowMock) return Verdict.MOCK
        val accuracy = fix.accuracyMeters
        if (accuracy == null || accuracy.isNaN() || accuracy > RouteConstants.MAX_ACCURACY_METERS) {
            return Verdict.INACCURATE
        }
        if (previous == null) return Verdict.ACCEPTED
        val elapsed = fix.atMillis - previous.atMillis
        if (elapsed <= 0) return Verdict.NOT_LATER
        val speed = Geodesy.meters(previous, fix) / (elapsed / MILLIS_PER_SECOND)
        return if (speed > maxSpeed(cadence)) Verdict.TOO_FAST else Verdict.ACCEPTED
    }

    /** The fastest the reader can be going at [cadence], in metres a second. */
    fun maxSpeed(cadence: Int?): Double = if ((cadence ?: 0) >= MetricsConstants.RUNNING_CADENCE) {
        RouteConstants.MAX_RUNNING_SPEED
    } else {
        RouteConstants.MAX_WALKING_SPEED
    }

    private const val MILLIS_PER_SECOND = 1_000.0
}
