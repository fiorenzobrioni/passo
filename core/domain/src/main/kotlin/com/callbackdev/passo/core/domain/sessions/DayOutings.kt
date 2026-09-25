package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.domain.today.MINUTES_PER_DAY
import com.callbackdev.passo.core.domain.today.minuteOfDay
import com.callbackdev.passo.core.domain.walks.Walk
import com.callbackdev.passo.core.model.Session
import java.time.ZoneId

/** A day's walking, as its list shows it: the walks found in the minutes, and the outings walked. */
sealed interface Outing {
    val startMinute: Int

    data class Detected(val walk: Walk) : Outing {
        override val startMinute: Int get() = walk.startMinute
    }

    /**
     * An outing the reader started, placed on its day: [startMinute] to [endMinute] (exclusive)
     * from local midnight, the end still moving while it is under way.
     */
    data class Planned(val session: Session, override val startMinute: Int, val endMinute: Int) : Outing
}

/**
 * One list for a day (PLANNING.md §11 Phase 10): an outing replaces the walk found in the same
 * minutes (it is that walk, with its goal and its own measures), and walks elsewhere keep their
 * place. By start, earliest first.
 */
object DayOutings {
    /**
     * @param walks the day's detected walks; null when walk detection is off, and then only the
     *   outings are listed: the reader asked for those.
     * @param nowMillis the end of an outing still under way.
     */
    fun of(walks: List<Walk>?, sessions: List<Session>, zone: ZoneId, nowMillis: Long): List<Outing> {
        val planned = sessions.map { session ->
            val start = minuteOfDay(session.startedAtMillis / MILLIS_PER_MINUTE, session.localEpochDay, zone)
            val endMillis = session.endedAtMillis ?: maxOf(nowMillis, session.startedAtMillis)
            val endMinute = if (endMillis == session.startedAtMillis) {
                start + 1
            } else {
                minuteOfDay((endMillis - 1) / MILLIS_PER_MINUTE, session.localEpochDay, zone) + 1
            }
            Outing.Planned(session, start, endMinute.coerceIn(start + 1, MINUTES_PER_DAY))
        }
        val detected = walks.orEmpty()
            .filter { walk -> planned.none { walk.startMinute < it.endMinute && it.startMinute < walk.endMinute } }
            .map(Outing::Detected)
        return (planned + detected).sortedBy { it.startMinute }
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}
