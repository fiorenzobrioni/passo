package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.RouteMatch
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
     * The name the two keep once they are one: the reader's for the round with no line, the one they
     * walked for months; else the other's; else none, and the round is named by its length.
     */
    fun name(a: KeptRound, b: KeptRound): String? {
        val (old, new) = if (a.route == null) a to b else b to a
        return old.round.name ?: new.round.name
    }

    /** Any pair of their walks said not the same: the reader answered «Another round». */
    fun saidApart(a: KeptRound, b: KeptRound, apart: Set<Pair<Long, Long>>): Boolean {
        if (apart.isEmpty()) return false
        val first = a.walks.mapTo(HashSet()) { it.id }
        val second = b.walks.mapTo(HashSet()) { it.id }
        return apart.any { (x, y) -> (x in first && y in second) || (x in second && y in first) }
    }
}
