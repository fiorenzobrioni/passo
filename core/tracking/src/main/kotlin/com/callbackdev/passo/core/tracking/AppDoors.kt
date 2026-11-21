package com.callbackdev.passo.core.tracking

import android.content.Intent

/**
 * What a touch outside the app asks it to open (PLANNING.md §11 Phase 23, step 23.1): extras on
 * the app's launch intent, read by the activity once per touch.
 *
 * The launch intent alone brings the app's `singleTask` task back as it was left, on a Way or on
 * Settings: so each surface names the page that holds what it shows. The tile, the widgets, the
 * counting notification and the goal's notifications open Today; the weekly summary opens its
 * week in History; a stage reached opens its way. While a surface shows an outing, it opens the
 * outing's own page instead ([SessionControl.EXTRA_OPEN_OUTING]); a paused count's touch asks to
 * resume too ([TrackingControl.EXTRA_RESUME]), and Today is where its button is.
 */
object AppDoors {
    /** Today, wherever the app was left (docs/adr/0008-goals-and-system-surfaces.md, decision 8). */
    const val EXTRA_OPEN_TODAY: String = "com.callbackdev.passo.extra.OPEN_TODAY"

    /** History on the week that holds this epoch day: the weekly summary's week. */
    const val EXTRA_OPEN_WEEK: String = "com.callbackdev.passo.extra.OPEN_WEEK"

    /** One way's page, by its `WayId`'s name: the way a stage's notification speaks of. */
    const val EXTRA_OPEN_WAY: String = "com.callbackdev.passo.extra.OPEN_WAY"

    /** With [EXTRA_OPEN_WAY]: the journey on it, by its id, so a way just finished opens on its stamps. */
    const val EXTRA_OPEN_JOURNEY: String = "com.callbackdev.passo.extra.OPEN_JOURNEY"
}

/** Asks for Today: what a surface opens when it shows the day, not an outing. */
fun Intent.openingToday(): Intent = putExtra(AppDoors.EXTRA_OPEN_TODAY, true)

/**
 * The request code of each door into the app. Android keeps one `PendingIntent` per intent and
 * code, whatever its extras, and `FLAG_UPDATE_CURRENT` gives the last one's extras to every
 * notification that holds it: two doors on one code open each other's page (a stage's
 * notification and an outing's shared one until 21 Nov 2026). One entry each, so no two can.
 */
internal enum class DoorRequest {
    COUNTING,
    GOAL_REACHED,
    EVENING_REMINDER,
    WEEKLY_SUMMARY,
    TILE_TODAY,
    TILE_RESUME,
    TILE_OUTING,
    OUTING,
    WAY,
    ;

    val code: Int get() = ordinal
}
