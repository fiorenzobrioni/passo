package com.callbackdev.passo.core.domain.sessions

import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.Session
import java.time.LocalDate
import java.time.YearMonth

/** Which of the reader's outings «Your outings» shows (PLANNING.md §11 Phase 23, step 23.2). */
enum class OutingsFilter {
    ALL,

    /** Only the outings that still hold a route: the way to see them, and delete one on its own. */
    WITH_ROUTE,
}

/**
 * One outing of the list, with its route as the list knows it: [route] its key, null without a
 * route that can be drawn. The points are not here: a row reads them only once it is on screen.
 */
data class KeptOuting(val session: Session, val route: RouteKey? = null) {
    val hasRoute: Boolean get() = route != null
}

/** A month of outings, newest first, as the list groups them under its header. */
data class OutingMonth(val month: YearMonth, val outings: List<KeptOuting>) {
    /** Their time in motion together: the steps' own measure, never an estimate. */
    val movingMillis: Long get() = outings.sumOf { it.session.totals.movingMillis }
}

/** How many of the outings hold a route, which decides how the page's sentence ends. */
enum class RouteShare {
    NONE,
    SOME,
    ALL,
}

/**
 * The page's sentence, in numbers: «48 outings since March; 21 with their route».
 *
 * @property since the month of the first outing kept; [sinceThisYear] it is in today's year, so
 *   the month says it alone.
 */
data class OutingsSummary(val count: Int, val withRoute: Int, val since: YearMonth, val sinceThisYear: Boolean) {
    val routes: RouteShare
        get() = when (withRoute) {
            0 -> RouteShare.NONE
            count -> RouteShare.ALL
            else -> RouteShare.SOME
        }
}

/**
 * The reader's outings over, by month, newest first (PLANNING.md §11 Phase 23, step 23.2): the one
 * list a walking app is expected to have, where an outing from three weeks ago is two touches
 * from the Outings page rather than a page of History a day.
 *
 * Numbers only: a few thousand outings are one query of the outings' own rows, grouped here in one
 * pass. Whether one has a route is a key read from `routes.db` beside them; its line is read later,
 * by the row that draws it, and only while that row is on screen.
 *
 * @property all every outing, by month; [withRoute] the same with only those that hold a route.
 * @property summary the sentence's numbers; null with no outing.
 */
class YourOutings private constructor(
    val all: List<OutingMonth>,
    val withRoute: List<OutingMonth>,
    val summary: OutingsSummary?,
) {
    /** The months [filter] keeps. */
    fun months(filter: OutingsFilter): List<OutingMonth> = when (filter) {
        OutingsFilter.ALL -> all
        OutingsFilter.WITH_ROUTE -> withRoute
    }

    val count: Int get() = summary?.count ?: 0

    override fun equals(other: Any?): Boolean = other is YourOutings && other.all == all && other.summary == summary

    override fun hashCode(): Int = 31 * all.hashCode() + summary.hashCode()

    companion object {
        val EMPTY = YourOutings(emptyList(), emptyList(), null)

        /**
         * The list of [sessions] (an outing still under way or paused is left out: its card shows
         * it), with the routes [routes] keeps.
         *
         * A route is an outing's only if it was written for that outing: same id and same start, as
         * `RouteRepository` matches them (a restored `passo.db` beside another phone's routes could
         * give an id to someone else's line). One of fewer than two points draws no line, and its
         * outing is listed as one without a route.
         *
         * @param today the reader's date, which says whether the first month needs its year.
         */
        fun of(sessions: List<Session>, routes: List<RouteKey>, today: LocalDate): YourOutings {
            val byId = routes.associateBy { it.sessionId }
            val kept = sessions.asSequence()
                .filter { !it.live }
                .sortedWith(compareByDescending<Session> { it.startedAtMillis }.thenByDescending { it.id })
                .map { session ->
                    val route = byId[session.id]?.takeIf {
                        it.sessionStartedAtMillis == session.startedAtMillis && it.pointCount >= MIN_POINTS
                    }
                    KeptOuting(session, route)
                }
                .toList()
            if (kept.isEmpty()) return EMPTY
            val all = group(kept)
            val routed = group(kept.filter { it.hasRoute })
            val first = YearMonth.from(LocalDate.ofEpochDay(kept.minOf { it.session.localEpochDay }))
            val summary = OutingsSummary(
                count = kept.size,
                withRoute = kept.count { it.hasRoute },
                since = first,
                sinceThisYear = first.year == today.year,
            )
            return YourOutings(all, routed, summary)
        }

        /** Newest first in, newest first out: a month starts where the last one ends. */
        private fun group(outings: List<KeptOuting>): List<OutingMonth> {
            val months = ArrayList<OutingMonth>()
            var month: YearMonth? = null
            var run = ArrayList<KeptOuting>()
            for (outing in outings) {
                val of = YearMonth.from(LocalDate.ofEpochDay(outing.session.localEpochDay))
                if (of != month) {
                    month?.let { months += OutingMonth(it, run) }
                    month = of
                    run = ArrayList()
                }
                run += outing
            }
            month?.let { months += OutingMonth(it, run) }
            return months
        }

        /** A line needs two points; [RouteKey.pointCount] under that draws none. */
        private const val MIN_POINTS = 2
    }
}

/** How a day is said against today: what the door to the list says of its latest outing. */
enum class RelativeDay {
    TODAY,
    YESTERDAY,

    /** In the six days before yesterday: its weekday is enough. */
    THIS_WEEK,

    /** Further back, or a day after today (a clock set back): its date. */
    DATE,
    ;

    companion object {
        fun of(day: LocalDate, today: LocalDate): RelativeDay = when (val ago = today.toEpochDay() - day.toEpochDay()) {
            0L -> TODAY
            1L -> YESTERDAY
            in 2L..WEEK_DAYS - 1 -> THIS_WEEK
            else -> DATE
        }

        private const val WEEK_DAYS = 7L
    }
}
