package com.callbackdev.passo.shell

import android.content.Intent
import com.callbackdev.passo.core.domain.history.PeriodScale
import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.tracking.AppDoors
import com.callbackdev.passo.core.tracking.SessionControl
import com.callbackdev.passo.feature.history.HistoryTarget
import java.time.LocalDate

/**
 * The page a touch outside the app asks for (PLANNING.md §11 Phase 23, step 23.1): the tile, a
 * widget, a notification, a launcher's shortcut. Each opens over Today, as a fresh way in, so
 * that back from the page it asks for lands on Today and not on whatever the app was last left on.
 */
sealed interface AppDoor {
    /** Today: the tile, the widgets, the counting notification, the day's goal, a shortcut's outing. */
    data object Today : AppDoor

    /** An outing's own page, over Today: a surface that shows it, the notification of its end. */
    data class Outing(val sessionId: Long) : AppDoor

    /** History on the week that holds [date]: the weekly summary. */
    data class Week(val date: LocalDate) : AppDoor

    /** One way's page, over Today: a stage reached on it. */
    data class Way(val way: WayId, val journeyId: Long?) : AppDoor

    companion object {
        /**
         * The door [intent] asks for, if any, with its extras taken off it, so that a rotation or
         * a second read does not open it again. The most particular page wins, should a surface
         * ever name two: an outing, a way, a week, Today.
         */
        fun take(intent: Intent): AppDoor? {
            val outing = intent.getLongExtra(SessionControl.EXTRA_OPEN_OUTING, 0L)
            val way = intent.getStringExtra(AppDoors.EXTRA_OPEN_WAY)?.let { name ->
                WayId.entries.firstOrNull { it.name == name }
            }
            val journey = intent.getLongExtra(AppDoors.EXTRA_OPEN_JOURNEY, 0L).takeIf { it != 0L }
            // The activity is the launcher's, open to any app: a day no calendar holds is no door.
            val week = intent.getLongExtra(AppDoors.EXTRA_OPEN_WEEK, Long.MIN_VALUE)
                .takeIf { it != Long.MIN_VALUE }
                ?.let { runCatching { LocalDate.ofEpochDay(it) }.getOrNull() }
            val today = intent.getBooleanExtra(AppDoors.EXTRA_OPEN_TODAY, false)
            DOOR_EXTRAS.forEach(intent::removeExtra)
            return when {
                outing != 0L -> Outing(outing)
                way != null -> Way(way, journey)
                week != null -> Week(week)
                today -> Today
                else -> null
            }
        }

        private val DOOR_EXTRAS = listOf(
            SessionControl.EXTRA_OPEN_OUTING,
            AppDoors.EXTRA_OPEN_WAY,
            AppDoors.EXTRA_OPEN_JOURNEY,
            AppDoors.EXTRA_OPEN_WEEK,
            AppDoors.EXTRA_OPEN_TODAY,
        )
    }
}

/**
 * Opens what [door] asks for, from wherever the reader was. Today first, its stack cleared, so
 * that back from the page asked for lands on Today: an outing's page shows over the card it
 * belongs to, a way's over the card that follows it. A week is History's own page, with Today
 * beneath it as every tab has.
 *
 * Passo's own words before Android asks for the location wait for an answer, with an outing
 * waiting on it: a door opened meanwhile (the counting notification, from the shade) keeps them
 * over the page it opens, rather than clearing them and leaving the outing unstarted.
 *
 * @return the place in History the door asks for, which History's page takes as its target;
 *   null for any other door.
 */
fun PassoNavigationState.open(door: AppDoor): HistoryTarget? {
    val asking = currentKey == GpsIntroKey
    val target = openPage(door)
    if (asking) navigate(GpsIntroKey)
    return target
}

private fun PassoNavigationState.openPage(door: AppDoor): HistoryTarget? {
    when (door) {
        AppDoor.Today -> showToday()

        is AppDoor.Outing -> {
            showToday()
            navigate(OutingKey(door.sessionId))
        }

        is AppDoor.Way -> {
            showToday()
            navigate(WayKey(door.way, door.journeyId))
        }

        is AppDoor.Week -> {
            show(ShellTab.HISTORY)
            return HistoryTarget(PeriodScale.WEEK, door.date)
        }
    }
    return null
}

/**
 * Whether [this] intent is a touch to act on: not one Android delivers again when the reader
 * opens the app from Recents after leaving it by back. The task's first intent comes back then,
 * extras and all, and a shortcut's outing would start a second time, a notification's page open
 * again.
 */
val Intent.isFreshTouch: Boolean
    get() = flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY == 0
