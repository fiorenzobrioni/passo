package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.RouteMatch
import com.callbackdev.passo.core.domain.routes.WalkedRoute
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionIntensity
import kotlin.math.abs

/**
 * Two of the reader's rounds that may be one, asked of the reader and never guessed (PLANNING.md
 * §15, «Phase 20: two rounds made one»).
 *
 * A round whose walks all lost their routes keeps its walks and times, but has no line: no new walk
 * can be compared with it, and no place outlives its outing, so its next walks with GPS make a new
 * round. Passo cannot know the two are one (their lengths alone would be a guess: two rounds of one
 * length from the same door are two rounds), so it asks, on either round's page, once the new one is
 * known: a round with no line and a round with one, of the same kind (walks or runs), their usual
 * lengths within a tenth, and no answer «Another round» between them.
 *
 * Two rounds that both have a line are another matter: Passo can tell whether they are one, by the
 * rule every walk is matched by ([sameByLine]), and an import that brought older walks of a round the
 * phone has, with their routes, is joined to it without a question.
 *
 * Pure: the repository reads the rounds and the pairs said not the same, and writes the answer.
 */
object RoundMerge {
    /**
     * The round [round] may be the same as, among [rounds] (the rounds shown, [round] among them), or
     * null: for a round with a line, the closest in length of those with none; for one with none, the
     * closest of those with one. [apart] holds the pairs of outings said not the same round, each the
     * smaller id first: any between the two rounds' walks, and they are not asked about again.
     */
    fun proposal(round: KeptRound, rounds: List<KeptRound>, apart: Set<Pair<Long, Long>>): KeptRound? {
        val lined = round.route != null
        return rounds
            .asSequence()
            .filter { it.id != round.id && (it.route != null) != lined }
            .filter { it.running == round.running }
            .filter { RouteMatch.lengthsAlike(it.usual.meters, round.usual.meters) }
            .filter { !saidApart(round, it, apart) }
            .minWithOrNull(
                compareBy<KeptRound> { abs(it.usual.meters - round.usual.meters) }.thenByDescending { it.walks.size },
            )
    }

    /**
     * The round with no line [outing] may be a walk of, asked on its page at once (§15, «Phase 20: a
     * round with no line asked at its first walk»), or null. A round whose walks all lost their
     * routes is not found again by a walk of it, and its next walks would make a new round from the
     * third; asked at the first, it takes the walk and its route, and is known again from the next.
     *
     * Asked only of a walk that can give the round its line back: [outing] over, measured, long
     * enough ([RoundFinder.eligible]), with a route the caller says it has, and in no round
     * ([rounds], the rounds shown). Of the rounds with no line, those of its kind (walks or runs),
     * their usual length within a tenth of its, and all their walks before it, with no pair said not
     * the same between it and them ([apart]); the closest in length first, then the one walked more.
     */
    fun lostFor(outing: Session, rounds: List<KeptRound>, apart: Set<Pair<Long, Long>>): KeptRound? {
        if (!RoundFinder.eligible(outing)) return null
        if (rounds.any { round -> round.walks.any { it.id == outing.id } }) return null
        val running = outing.intensity == SessionIntensity.RUN
        return rounds
            .asSequence()
            .filter { it.route == null && it.running == running }
            .filter { RouteMatch.lengthsAlike(it.usual.meters, outing.totals.distanceMeters) }
            .filter { it.latest.startedAtMillis < outing.startedAtMillis }
            .filter { !saidApart(listOf(outing.id), it.walks.map { walk -> walk.id }, apart) }
            .minWithOrNull(
                compareBy<KeptRound> { abs(it.usual.meters - outing.totals.distanceMeters) }
                    .thenByDescending { it.walks.size },
            )
    }

    /**
     * The name the two keep once they are one: the reader's for the round with no line, the one they
     * walked for months; else the other's; else none, and the round is named by its length.
     */
    fun name(a: KeptRound, b: KeptRound): String? {
        val (old, new) = if (a.route == null) a to b else b to a
        return old.round.name ?: new.round.name
    }

    /** Any pair of their walks said not the same: the reader answered «Another round». */
    fun saidApart(a: KeptRound, b: KeptRound, apart: Set<Pair<Long, Long>>): Boolean =
        saidApart(a.walks.map { it.id }, b.walks.map { it.id }, apart)

    /** Any pair of the outings [a] and [b] said not the same round. */
    fun saidApart(a: Collection<Long>, b: Collection<Long>, apart: Set<Pair<Long, Long>>): Boolean {
        if (apart.isEmpty()) return false
        val first = a.toHashSet()
        val second = b.toHashSet()
        return apart.any { (x, y) -> (x in first && y in second) || (x in second && y in first) }
    }

    /**
     * Whether two rounds may be one by their numbers alone, before a point is read: walks or runs
     * both, and their usual lengths, the middle of their walks', within a tenth.
     */
    fun mayBeOne(a: List<Session>, b: List<Session>): Boolean {
        val first = RoundTimes.usual(a) ?: return false
        val second = RoundTimes.usual(b) ?: return false
        return a.all { it.intensity == SessionIntensity.RUN } == b.all { it.intensity == SessionIntensity.RUN } &&
            RouteMatch.lengthsAlike(first.meters, second.meters)
    }

    /**
     * Whether two rounds with a line are one by the rule every walk is matched by ([RouteMatch]): a
     * walk of one, among its latest with a route ([a]), the same as a walk of the other ([b]). Not a
     * guess, so not asked: what an import that brought a round with its routes is joined by to the
     * phone's own (§15, «Phase 20: the last open choices»).
     */
    fun sameByLine(a: List<WalkedRoute>, b: List<WalkedRoute>): Boolean =
        a.any { first -> b.any { second -> RouteMatch.compare(first, second).same } }
}
