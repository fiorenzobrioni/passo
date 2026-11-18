package com.callbackdev.passo.core.domain.routes

/**
 * The constants of an outing measured with GPS (PLANNING.md §11 Phase 15;
 * docs/adr/0016-location.md, decision 5), each with its reason. Like `MetricsConstants`, they
 * are written before a phone has walked them: the ones a field test may move say so.
 */
object RouteConstants {
    /**
     * A fix is accepted at this accuracy or better. Under an open sky a phone gives 3 to 10 m;
     * between buildings 10 to 25; beyond that the receiver itself says it is guessing, and a
     * route drawn through guesses is longer than the walk. To be validated on the device.
     */
    const val MAX_ACCURACY_METERS: Float = 25f

    /**
     * No fix may imply a speed above this from the one before, below the running cadence: three
     * and a half metres a second is a run, well past the fastest walk (about 2.5), with room for
     * the metre or two two honest fixes disagree by. What is faster is a jump of the receiver's,
     * or a vehicle.
     */
    const val MAX_WALKING_SPEED: Double = 3.5

    /** The same at or above the running cadence: eight metres a second, a sprint few hold. */
    const val MAX_RUNNING_SPEED: Double = 8.0

    /**
     * The route's line moves on once the reader is this far from its last point. Shorter, and
     * the receiver's own second-to-second wander (a metre, a little more) is added up as if it
     * were walked; longer, and corners are cut. It is also the grain of what is kept: a point
     * every three metres is about two seconds of walking.
     */
    const val MIN_MOVE_METERS: Double = 3.0

    /**
     * Where the receiver is less sure, the grain is coarser: this share of the fix's own
     * accuracy, when that is more than [MIN_MOVE_METERS]. Two fixes a second apart err together,
     * by a fraction of the radius the receiver states for each; half of it keeps that wander
     * under the grain between tall buildings without cutting the corners of a walk under an
     * open sky. To be validated on the device.
     */
    const val ACCURACY_GRAIN: Float = 0.5f

    /**
     * After a ride, the receiver is believed again once this many fixes in a row each agree
     * with the one before: five seconds of a steady walk, which no jump of the receiver's
     * imitates. Until then a fix far from the last one accepted is refused, as one bad fix is.
     */
    const val RESEED_FIXES: Int = 5

    /**
     * A reader who moves more slowly than that still gets a point after this long, if they
     * moved at all ([MIN_SLOW_MOVE_METERS]): a slow line is still a line.
     */
    const val SLOW_POINT_MILLIS: Long = 5_000L

    /** Under this, after [SLOW_POINT_MILLIS], the reader has not moved: it is the receiver. */
    const val MIN_SLOW_MOVE_METERS: Double = 1.0

    /**
     * With no accepted fix for this long while steps come, the stretch is a hole (a tunnel, an
     * arcade, tall streets): it is the steps', never less than the straight line across it. A
     * fix a second is the rule, so a quarter of a minute without one is no accident of delivery.
     */
    const val HOLE_MILLIS: Long = 15_000L

    /**
     * The step counter tells a step up to this long after it was taken (the counter's own delay,
     * docs/adr/0013-interval-walks.md): the walking part of a batch is taken to begin this much
     * before its time in motion says, so the first metres after a stop are not left out.
     */
    const val STEP_DELAY_MILLIS: Long = 10_000L

    /**
     * No step is longer than this, running included: the longest running step a profile may
     * hold (`ProfileLimits.RUNNING_STEP_LENGTH_M`), so no reader's own stride is ever taken for
     * a ride. What the fixes say beyond the steps taken times this, and [CARRIED_SLACK_METERS],
     * was not walked: a tram, a lift, a moving walkway.
     */
    const val MAX_METERS_PER_STEP: Double = 2.5

    /** The room two fixes at the accepted accuracy may add to a distance that was walked. */
    const val CARRIED_SLACK_METERS: Double = 25.0

    /**
     * A stretch teaches the step's length when it was measured from end to end for this long in
     * motion or more: under a minute, the three metres the line's grain may owe at each end are
     * too much of it.
     */
    const val SAMPLE_MIN_MOVING_MILLIS: Long = 60_000L

    /** ...and at a steady cadence: within this many steps a minute from its slowest to its fastest. */
    const val SAMPLE_CADENCE_SPAN: Int = 8

    /** The pace now is the pace of the last so many metres; under them there is none to say. */
    const val PACE_METERS: Double = 200.0

    /**
     * The pace now is shown for this long after the last step: half a minute, the span the
     * cadence itself is read over. Past it the reader has stopped, and the pace of the last
     * stretch is no longer the pace now.
     */
    const val PACE_NOW_MILLIS: Long = 30_000L

    /**
     * «Measured» is said of a distance at least this much from fixes
     * (docs/adr/0016-location.md, decision 6): nine tenths. Under it the outing says how much
     * the steps gave, so the word is never spent on a walk half of which was a tunnel.
     */
    const val MEASURED_SHARE: Double = 0.9
}
