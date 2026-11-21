package com.callbackdev.passo.core.data.rounds

import com.callbackdev.passo.core.data.db.RoundDao
import com.callbackdev.passo.core.data.db.SessionDao
import com.callbackdev.passo.core.data.db.toModel
import com.callbackdev.passo.core.data.prefs.UserPreferencesDataSource
import com.callbackdev.passo.core.data.routes.RouteRepository
import com.callbackdev.passo.core.domain.rounds.KeptRound
import com.callbackdev.passo.core.domain.rounds.RoundCandidate
import com.callbackdev.passo.core.domain.rounds.RoundChoice
import com.callbackdev.passo.core.domain.rounds.RoundConstants
import com.callbackdev.passo.core.domain.rounds.RoundFinder
import com.callbackdev.passo.core.domain.rounds.RoundGuide
import com.callbackdev.passo.core.domain.rounds.RoundMerge
import com.callbackdev.passo.core.domain.rounds.RoundTimes
import com.callbackdev.passo.core.domain.rounds.YourRounds
import com.callbackdev.passo.core.domain.routes.WalkedRoute
import com.callbackdev.passo.core.model.Round
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

/**
 * The reader's usual rounds (PLANNING.md §11 Phase 20): found among their own outings with GPS
 * without being told, as each ends, and once among the ones walked before rounds were kept.
 *
 * What is kept is numbers and names, in `passo.db` (`RoundDao`); a round's line and start are its
 * walks' routes, read from `routes.db` only when an outing is searched or an open one starts, and
 * never by a read that would make the file ([RouteRepository]'s own check). Searching is the
 * domain's ([RoundFinder]); this reads what it needs, cheapest first (the outings' numbers, then the
 * routes' boxes, then the few routes left), and writes what it decides.
 */
@Singleton
class RoundRepository
@Inject
constructor(
    private val dao: RoundDao,
    private val sessions: SessionDao,
    private val routes: RouteRepository,
    private val preferences: UserPreferencesDataSource,
) {
    // One search at a time: an outing's end and the first search of the history may meet.
    private val lock = Mutex()

    /** The rounds kept, each with its walks' ids, as they change. */
    val rounds: Flow<List<Round>> = combine(dao.observeRounds(), dao.observeMembers()) { rows, members ->
        val walks = members.groupBy({ it.roundId }) { it.sessionId }
        rows.mapNotNull { row ->
            walks[row.id]?.takeIf { it.size >= RoundConstants.MIN_WALKS }?.let { Round(row.id, row.name, it.toSet()) }
        }
    }

    /**
     * The rounds as the reader sees them, from their third walk (PLANNING.md §11 Phase 20, «Your
     * rounds»): each with its walks over, its usual and its picture's key, the latest walked first.
     * Numbers alone, the rounds' own walks and no other outing, grouped off the main thread; read
     * only while a page collects it. An outing under way writes its row at each batch: the rounds,
     * whose walks are over, are grouped again only when what they hold changed.
     */
    val kept: Flow<List<KeptRound>> = combine(
        rounds,
        sessions.observeRoundWalks().map { rows -> rows.mapNotNull { it.toModel() } }.distinctUntilChanged(),
        routes.keys,
    ) { found, outings, keys -> YourRounds.of(found, outings, keys) }
        .flowOn(Dispatchers.Default)
        .distinctUntilChanged()

    /**
     * The pairs of outings the reader said are not the same round, each the smaller id first: what
     * keeps two rounds they said are not one from being asked about again ([RoundMerge]).
     */
    val apart: Flow<Set<Pair<Long, Long>>> = dao.observeApart()
        .map { rows -> rows.mapTo(HashSet()) { it.firstId to it.secondId } }
        .distinctUntilChanged()

    /**
     * Finds the round of the outing [sessionId], which has just ended and whose route is written
     * whole: among the outings with GPS before it, a round one of whose walks it is the same as, or
     * a new pair with an outing in none. Searched again if it ends again (an end taken back): it
     * leaves the round it was found in first.
     */
    suspend fun find(sessionId: Long) = lock.withLock { findLocked(sessionId, history = null, again = true) }

    /**
     * Searches, once, the outings with GPS walked before the rounds were kept, oldest first, each
     * among the ones before it, as if each had just ended. At the tracking service's start, where the
     * routes' file is already there; a phone that searched once never searches again. An outing in a
     * round already (one a backup brought with its round) keeps it: a search adds, and never undoes
     * a grouping, nor the name the reader gave it.
     */
    suspend fun searchOnce() = lock.withLock {
        if (preferences.roundsSearched()) return@withLock
        searchLocked(sessions.finishedWithGps().map { it.id })
        preferences.markRoundsSearched()
    }

    /** Searches the outings [ids] an import brought that are in no round yet, oldest first. */
    suspend fun searchAmong(ids: Collection<Long>) = lock.withLock {
        if (ids.isEmpty()) return@withLock
        val wanted = ids.toSet()
        searchLocked(sessions.finishedWithGps().map { it.id }.filter { it in wanted })
    }

    /**
     * The rounds an open outing with GPS may walk, read once at its start: each round of two walks
     * over or more that still has a route, its lines its latest walks' (up to
     * [RoundConstants.WALKS_ASKED]), and its usual time at each kilometre in [unitMeters] (or mile),
     * from its walks' legs. Runs only for a run, walks for a walk.
     *
     * Only walks over: an outing taken back with «Keep going» is still its round's walk until it
     * ends again, and read after a restart of the process it would be compared with itself, its own
     * route so far the round's line.
     */
    suspend fun guides(running: Boolean, unitMeters: Double): List<RoundGuide> = withContext(Dispatchers.Default) {
        val members = dao.members().groupBy({ it.roundId }) { it.sessionId }
        members.mapNotNull { (roundId, ids) ->
            if (ids.size < RoundConstants.MIN_WALKS) return@mapNotNull null
            // A round walked daily for years is read a slice at a time: a query's parameters are bounded.
            val walks = ids.chunked(IDS_A_READ).flatMap { sessions.sessions(it) }
                .mapNotNull { it.toModel() }
                .filter { it.state == SessionState.FINISHED }
                .sortedBy { it.startedAtMillis }
            if (walks.size < RoundConstants.MIN_WALKS || walks.all { it.running } != running) return@mapNotNull null
            val lines = latestLines(walks).map { it.points }
            if (lines.isEmpty()) return@mapNotNull null
            val usual = RoundTimes.usual(walks) ?: return@mapNotNull null
            // A kilometre past the usual length, for a walk a little longer than the round.
            val count = ceil(usual.meters / unitMeters).toInt() + 1
            RoundGuide(roundId, lines, RoundTimes.usualAtMarks(walks, unitMeters, count), walks.size)
        }
    }

    /**
     * After an import (PLANNING.md §15, «Phase 20: the last open choices»): two rounds with a line, one
     * of them holding an outing the import brought ([brought]), that are one by the rule every walk is
     * matched by ([RoundMerge.sameByLine]), are made one, with the phone's name. A file of older walks
     * of a round the phone has makes a round of its own: its walks are searched among the ones before
     * them, and the phone's are after. No question: both have a line, and Passo can tell. Rounds the
     * reader said are not one are left as they are.
     */
    suspend fun joinAlike(brought: Collection<Long>) = lock.withLock {
        if (brought.isEmpty()) return@withLock
        val wanted = brought.toHashSet()
        val outings = sessions.finishedWithGps().mapNotNull { it.toModel() }.associateBy { it.id }
        val apart = dao.apart().mapTo(HashSet()) { it.firstId to it.secondId }
        val lines = HashMap<Long, List<WalkedRoute>>()
        while (true) {
            val rounds = dao.members()
                .groupBy({ it.roundId }) { it.sessionId }
                .mapValues { (_, ids) -> ids.mapNotNull { outings[it] }.sortedBy { it.startedAtMillis } }
                .filterValues { it.size >= RoundConstants.MIN_WALKS }
            val ids = rounds.keys.sorted()
            var joined: Pair<Long, Long>? = null
            search@ for (a in ids) {
                for (b in ids) {
                    if (b <= a) continue
                    val first = rounds.getValue(a)
                    val second = rounds.getValue(b)
                    if (first.none { it.id in wanted } && second.none { it.id in wanted }) continue
                    if (!RoundMerge.mayBeOne(first, second)) continue
                    if (RoundMerge.saidApart(first.map { it.id }, second.map { it.id }, apart)) continue
                    val linesA = lines.getOrPut(a) { latestLines(first).map { it.walked } }
                    val linesB = lines.getOrPut(b) { latestLines(second).map { it.walked } }
                    val same = withContext(Dispatchers.Default) { RoundMerge.sameByLine(linesA, linesB) }
                    if (same) {
                        joined = a to b
                        break@search
                    }
                }
            }
            val (into, from) = joined ?: break
            // The older round is the phone's: the import's are made after it. It keeps its name.
            val names = dao.rounds().associate { it.id to it.name }
            dao.merge(into, from, names[into] ?: names[from])
            lines.remove(into)
            lines.remove(from)
        }
    }

    /** «Not the same round»: the outing leaves its round, never to be matched with its walks again. */
    suspend fun notTheSame(sessionId: Long) = lock.withLock { dao.notTheSame(sessionId) }

    /** «Forget this round»: the round and its pairs go; the outings stay, as they are. */
    suspend fun forget(roundId: Long) = lock.withLock { dao.forget(roundId) }

    /**
     * «The same round»: the two rounds the reader said are one (PLANNING.md §15, «Phase 20: two rounds
     * made one»), [from]'s walks into [into], which is named [name] ([RoundMerge.name]).
     */
    suspend fun merge(into: Long, from: Long, name: String?) = lock.withLock {
        dao.merge(into, from, name?.trim()?.takeIf { it.isNotEmpty() })
    }

    /** «Another round»: the two rounds are not one, and are never asked about again. */
    suspend fun notOneRound(first: Long, second: Long) = lock.withLock { dao.apartRounds(first, second) }

    /** The reader's own name for the round [roundId]; none, or a blank one, names it from its length. */
    suspend fun rename(roundId: Long, name: String?) = dao.rename(roundId, name?.trim()?.takeIf { it.isNotEmpty() })

    private suspend fun searchLocked(ids: List<Long>) {
        // The outings' rows once, for the whole search: each is searched among those before it.
        val all = sessions.finishedWithGps().mapNotNull { it.toModel() }
        // In a round already when its turn comes: it was brought with it, and stays as it is. An
        // outing earlier in the search may have been taken into one since; it was searched before.
        for (id in ids) if (dao.roundOf(id) == null) findLocked(id, all, again = false)
    }

    /**
     * [history] the outings with GPS over, oldest first, read once for a search of many; read here
     * for the search of one. [again]: the outing ended once more (an end taken back), and leaves the
     * round it was found in first, to be searched with its whole route.
     */
    private suspend fun findLocked(sessionId: Long, history: List<Session>?, again: Boolean) {
        val outings = history ?: sessions.finishedWithGps().mapNotNull { it.toModel() }
        val outing = outings.firstOrNull { it.id == sessionId } ?: return
        if (again) dao.leave(sessionId)
        if (!RoundFinder.eligible(outing)) return
        val boxes = routes.boxes()
        val box = boxes[sessionId]?.takeIf { it.sessionStartedAtMillis == outing.startedAtMillis } ?: return
        val route = routes.route(sessionId, outing.startedAtMillis) ?: return
        val pairs = dao.apart()
        val apart = pairs.mapNotNull {
            when (sessionId) {
                it.firstId -> it.secondId
                it.secondId -> it.firstId
                else -> null
            }
        }.toSet()
        val roundOf = dao.members().associate { it.sessionId to it.roundId }
        // A round with a walk the reader said is not the same as this one is not this one's.
        val closed = roundOf.filterKeys { it in apart }.values.toSet()
        // Cheapest first: the numbers, then the boxes; the routes of what is left, newest first.
        val near = outings
            .asSequence()
            .filter { it.startedAtMillis < outing.startedAtMillis && it.id !in apart }
            .filter { RoundFinder.eligible(it) && RoundFinder.mayBeSame(outing, it) }
            .filter { roundOf[it.id] !in closed }
            .filter { other ->
                boxes[other.id]?.takeIf { it.sessionStartedAtMillis == other.startedAtMillis }
                    ?.let { RoundFinder.boxesAlike(box.box, it.box) } == true
            }
            .toList()
            .asReversed()
        // A round is asked through its latest walks only: a reader who walks it every day has a year
        // of walks of it, and the newest say what it is now. An outing in no round, alone.
        val asked = near
            .groupBy { roundOf[it.id] }
            .flatMap { (round, walks) ->
                if (round == null) walks.take(MAX_ALONE) else walks.take(RoundConstants.WALKS_ASKED)
            }
        val candidates = asked.mapNotNull { other ->
            routes.route(other.id, other.startedAtMillis)?.let { RoundCandidate(other, roundOf[other.id], it.points) }
        }
        val choice = withContext(Dispatchers.Default) {
            RoundFinder.choose(
                WalkedRoute(route.points, outing.totals.distanceMeters),
                candidates,
                apart = pairs.mapTo(HashSet()) { it.firstId to it.secondId },
            )
        }
        when (choice) {
            is RoundChoice.Join -> dao.join(choice.roundId, sessionId)
            is RoundChoice.New -> dao.start(choice.sessionIds + sessionId)
            RoundChoice.None -> Unit
        }
    }

    private val Session.running: Boolean get() = intensity == SessionIntensity.RUN

    /** One of a round's latest walks with its route: as the card follows it, as two rounds are compared. */
    private class Line(val walk: Session, val points: List<RoutePoint>) {
        val walked: WalkedRoute get() = WalkedRoute(points, walk.totals.distanceMeters)
    }

    /**
     * The routes of the latest of [walks] (oldest first) that keep one, newest first, up to
     * [RoundConstants.WALKS_ASKED].
     */
    private suspend fun latestLines(walks: List<Session>): List<Line> {
        val lines = ArrayList<Line>(RoundConstants.WALKS_ASKED)
        for (walk in walks.asReversed()) {
            if (lines.size == RoundConstants.WALKS_ASKED) break
            val points = routes.route(walk.id, walk.startedAtMillis)?.points ?: continue
            if (points.size >= 2) lines += Line(walk, points)
        }
        return lines
    }

    private companion object {
        /**
         * The outings in no round a new one is compared with, newest first: a bound on the reads of a
         * reader who walks a new way each day near the same door, far past what a usual round needs.
         */
        const val MAX_ALONE = 60

        /** Outings read by their ids at once: well under SQLite's bound on a query's parameters. */
        const val IDS_A_READ = 500
    }
}
