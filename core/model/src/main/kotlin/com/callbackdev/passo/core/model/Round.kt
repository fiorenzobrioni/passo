package com.callbackdev.passo.core.model

/**
 * A usual round (PLANNING.md §11 Phase 20; docs/adr/0016-location.md, decision 16): a walk the
 * reader repeats, from their door and the same way round, found among their own outings with GPS
 * without being told. A number and a name, never a place: its line and its start are read from its
 * walks' routes, so no place outlives the outing it belongs to.
 *
 * @property name the reader's own name for it; null until they give it one, when it is named from
 *   its length.
 * @property walks the outings that are this round, by their id: two at least.
 */
data class Round(val id: Long, val name: String?, val walks: Set<Long>)

/**
 * Where an open outing with GPS stands on a usual round while it counts (Phase 20, «The card,
 * while it counts»). Found after the first half kilometre; then told at each kilometre, or mile.
 */
enum class RoundStanding {
    /** On the round: the card says how far ahead of the usual, or behind, from the first kilometre. */
    ON,

    /** Off the round for long enough: the card says so, until the next kilometre's words say it. */
    LEFT,

    /** Left, and said: the line stops, and the outing goes on as any. */
    GONE,
}

/**
 * An open outing's line on a usual round, as it was last told: kept with the outing, as its marks
 * are, so a process that restarts tells nothing twice and the card, the notification and the page
 * say the same.
 *
 * @property roundId the round the outing was found on.
 * @property marks the kilometres (or miles) whose comparison is the one held: when [standing] is
 *   [RoundStanding.GONE], the one whose words said the round was left.
 * @property gapMillis the outing's time in motion to its [marks]th kilometre, less the usual's
 *   there: below zero ahead, above behind, zero within what a walk's own wander is. Null until the
 *   first kilometre.
 */
data class RoundLine(
    val roundId: Long,
    val standing: RoundStanding = RoundStanding.ON,
    val marks: Int = 0,
    val gapMillis: Long? = null,
)
