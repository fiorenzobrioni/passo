package com.callbackdev.passo.core.data.db

import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayJourney
import com.callbackdev.passo.core.model.WayJourneyState

/** Null for a way a newer build knows and this one does not: it is left out. */
internal fun WayJourneyEntity.toModel(): WayJourney? {
    val id = WayId.entries.firstOrNull { it.name == way } ?: return null
    return WayJourney(
        id = this.id,
        way = id,
        startEpochDay = startEpochDay,
        startedAtMillis = startedAtMillis,
        // An unknown state from a newer build is no longer under way.
        state = WayJourneyState.entries.firstOrNull { it.name == state } ?: WayJourneyState.LEFT,
        endedEpochDay = endedEpochDay,
        toldMeters = toldMeters,
    )
}

internal fun WayJourney.toEntity() = WayJourneyEntity(
    id = id,
    way = way.name,
    startEpochDay = startEpochDay,
    startedAtMillis = startedAtMillis,
    state = state.name,
    endedEpochDay = endedEpochDay,
    toldMeters = toldMeters,
)
