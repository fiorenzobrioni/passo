package com.callbackdev.passo.core.domain.rounds

/**
 * The constants of the usual round (PLANNING.md §11 Phase 20), each with its reason. The match's
 * are the plan's, reviewed by the owner on 19 Nov 2026; like `RouteConstants`, they are written
 * before a phone has walked them, and the device check says whether they hold.
 */
object RoundConstants {
    /**
     * Two walks of one round start within this of each other: the same door, give or take the
     * receiver's first fixes, which come a few seconds after the start and from a few dozen metres
     * further on. A loop begun at another point of it starts further away, and is another round.
     */
    const val START_METERS: Double = 100.0

    /**
     * Their lengths are within this share of the longer one's: a shortcut that leaves out a tenth
     * is still the round, and is compared at the round's usual length, not called faster for it.
     */
    const val LENGTH_SHARE: Double = 0.1

    /**
     * A walk lies on a line within this of it: a street's width and the receiver's honest wander
     * between tall buildings (25 m is the worst fix accepted), well under the block between two
     * parallel streets of most cities.
     */
    const val NEAR_METERS: Double = 40.0

    /**
     * Four fifths of each walk lies near the other, in order: a detour of a few hundred metres on
     * a walk of three kilometres is still the round, a different way home is not.
     */
    const val SHARE: Double = 0.8

    /**
     * Walks are compared a sample every this many metres along their lines: under the forty metres
     * they are compared at, and about the grain of a route's own points (three to five metres) put
     * together, so a share of samples is a share of the walk's length.
     */
    const val SAMPLE_METERS: Double = 10.0

    /**
     * No walk is cut into more samples than this: a day's hike is compared at a coarser grain, so
     * a year of outings is matched in about the same time whatever their lengths.
     */
    const val MAX_SAMPLES: Int = 2_000

    /**
     * How far behind the furthest place reached along the other line a sample may fall and still be
     * in order: the receiver's wander from one second to the next, along the street. A walk the
     * other way round falls further behind at every sample, and stops being in order within this.
     */
    const val ORDER_SLACK_METERS: Double = 40.0

    /**
     * No walk shorter than this is a round: a trip round the block, which the minutes of a day
     * already say. The same length as the card's line needs before it says the round.
     */
    const val MIN_METERS: Double = 500.0

    /**
     * A round is kept from its second walk, the first two that match: Passo remembers the pair, and
     * the card of an open outing with GPS can compare the third with them while it counts.
     */
    const val MIN_WALKS: Int = 2

    /**
     * It is shown from its third walk: Your rounds, and the outing's page. The third time a round is
     * walked Passo knows it without being told (the phase's acceptance); a pair is not yet a usual.
     */
    const val KNOWN_WALKS: Int = 3

    /**
     * The card says the round once the outing has walked this far on it (four fifths of it near the
     * round's line, in order): too soon, and every walk out of the door is on every round from it.
     */
    const val FOUND_METERS: Double = 500.0

    /** Off the round's line for this far in a row, the outing has left it: said once, and the line stops. */
    const val LEFT_METERS: Double = 200.0

    /**
     * A walk within this share of the usual time, or within [EVEN_MIN_MILLIS], is said to be at its
     * usual: a traffic light more or less is not a faster walk.
     */
    const val EVEN_SHARE: Double = 0.02

    /** A quarter of a minute: under it, the difference is a crossing's wait, not the reader's pace. */
    const val EVEN_MIN_MILLIS: Long = 15_000L

    /**
     * A box of a route grown by this on each side before two are compared: a narrow route (a street
     * out and back) is not told from itself by a few metres of the receiver's wander across it.
     */
    const val BOX_MARGIN_METERS: Double = 100.0

    /**
     * Two routes whose boxes, so grown, overlap by less than this share of their union are not read
     * at all: they lie elsewhere, or are another size. What lets a year of outings cost a few reads.
     */
    const val BOX_OVERLAP: Double = 0.5
}
