package com.callbackdev.passo.core.model

/**
 * The four ways (PLANNING.md §11 Phase 11), in the order the Ways page lists them: the
 * shortest first. Stored by name: the order may change, a name may not.
 */
enum class WayId {
    VIA_DEGLI_DEI,
    VIA_DI_FRANCESCO,
    CAMINO_FRANCES,
    VIA_FRANCIGENA,
}

/** Where a way the reader started stands. Stored by name. */
enum class WayJourneyState {
    /** Under way: the days since [WayJourney.startEpochDay] move the reader along it. */
    ACTIVE,

    /** Walked to its end, on [WayJourney.endedEpochDay]: the days after it no longer count. */
    FINISHED,

    /** Put down by the reader on [WayJourney.endedEpochDay], where it was then. */
    LEFT,
}

/**
 * A way the reader started (`way_journey`). It keeps no distance of its own: what was walked
 * is the sum of the days' own estimates from [startEpochDay], so a way can never disagree with
 * History (PLANNING.md §11 Phase 11).
 *
 * @property toldMeters how far along the way the stages have been told: a stage at or before
 *   it is never notified again. At a start in the past it is set to where the reader already
 *   stands, so the stages behind them are stamped, not announced.
 */
data class WayJourney(
    val id: Long,
    val way: WayId,
    val startEpochDay: Long,
    val startedAtMillis: Long,
    val state: WayJourneyState,
    val endedEpochDay: Long?,
    val toldMeters: Int,
)
