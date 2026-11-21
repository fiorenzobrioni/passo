package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.model.Round
import com.callbackdev.passo.core.model.RouteKey
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity

/**
 * One of the reader's usual rounds, as «Your rounds» and its page show it (PLANNING.md §11 Phase 20).
 *
 * @property walks the outings that are this round, oldest first.
 * @property usual its usual length and time, from all of them.
 * @property route the latest walk's route that can be drawn: the round's map and its small picture;
 *   null when every walk's route was deleted, and the round keeps its history without a line.
 * @property running its walks are runs: «Your running round».
 */
data class KeptRound(
    val round: Round,
    val walks: List<Session>,
    val usual: RoundUsual,
    val route: RouteKey?,
    val running: Boolean,
) {
    val id: Long get() = round.id
    val latest: Session get() = walks.last()

    /** Each walk's day, and its time in motion brought to the usual length: what the chart draws. */
    val times: List<RoundTime>
        get() = walks.map {
            RoundTime(it.id, it.localEpochDay, RoundTimes.brought(it, usual.meters), it.totals.distanceMeters)
        }

    /**
     * How the latest walks went against the usual: the run of walks at the end that were all faster,
     * all slower or all at the usual time, and which. What the chart's one sentence says («Six walks
     * since March; the last three faster than usual»).
     */
    val trend: RoundTrend
        get() {
            val verdicts = times.map {
                RoundVerdict.of(RoundTimes.even(it.millis - usual.movingMillis, usual.movingMillis))
            }
            val last = verdicts.last()
            val run = verdicts.asReversed().takeWhile { it == last }.size
            return RoundTrend(walks = walks.size, sinceDay = walks.first().localEpochDay, lastRun = run, last = last)
        }
}

/** One walk of a round, as its chart and its list show it: [millis] brought to the usual length. */
data class RoundTime(val sessionId: Long, val localEpochDay: Long, val millis: Long, val meters: Double)

/** The latest walks of a round against its usual: the last [lastRun] were all [last]. */
data class RoundTrend(val walks: Int, val sinceDay: Long, val lastRun: Int, val last: RoundVerdict)

/**
 * The reader's usual rounds (PLANNING.md §11 Phase 20, «Your rounds»): from their third walk, the
 * one most recently walked first. Numbers and names: the routes are keys here, read by the rows that
 * draw them, and only while they are on screen, as «Your outings» does.
 */
object YourRounds {
    /**
     * [rounds] as listed, with their walks among [outings] (the outings over) and their pictures
     * among [keys]. A round whose walks the reader's outings no longer hold enough of is not listed.
     */
    fun of(rounds: List<Round>, outings: List<Session>, keys: List<RouteKey>): List<KeptRound> {
        val byId = outings.associateBy { it.id }
        val drawable = keys.filter { it.pointCount >= 2 }.associateBy { it.sessionId }
        return rounds
            .mapNotNull { round ->
                val walks = round.walks.mapNotNull { byId[it] }.sortedBy { it.startedAtMillis }
                if (walks.size < RoundConstants.KNOWN_WALKS) return@mapNotNull null
                val usual = RoundTimes.usual(walks) ?: return@mapNotNull null
                val route = walks.asReversed().firstNotNullOfOrNull { walk ->
                    drawable[walk.id]?.takeIf { it.sessionStartedAtMillis == walk.startedAtMillis }
                }
                KeptRound(round, walks, usual, route, running = walks.all { it.intensity == SessionIntensity.RUN })
            }
            .sortedByDescending { it.latest.startedAtMillis }
    }

    /** The round [sessionId] is a walk of, among [rounds], once it is known; null otherwise. */
    fun of(sessionId: Long, rounds: List<KeptRound>): KeptRound? = rounds.firstOrNull { round ->
        round.walks.any { it.id == sessionId }
    }
}
