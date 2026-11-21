package com.callbackdev.passo.core.domain.rounds

import com.callbackdev.passo.core.domain.routes.Splits
import com.callbackdev.passo.core.model.Session
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * A round's usual: its length, and its time in motion at that length, each the middle of its walks'
 * (the median: one walk with a long wait at a crossing does not move it, as a mean would).
 */
data class RoundUsual(val meters: Double, val movingMillis: Long, val walks: Int)

/**
 * How one walk of a round compares with the walks of it before it, as its page says it (PLANNING.md
 * §11 Phase 20, «The outing's page»).
 *
 * @property nth which time this was, from 1, in the order they were walked.
 * @property usual the round's usual before this walk; null before there were two walks to make one.
 * @property thisMillis this walk's time in motion brought to the usual's length; null without a usual.
 * @property gapMillis [thisMillis] less the usual's: below zero faster, above slower, zero within
 *   what a crossing's wait is ([RoundTimes.even]).
 */
data class RoundComparison(val nth: Int, val usual: RoundUsual?, val thisMillis: Long?, val gapMillis: Long?)

/** Where a walk's time stands against a usual one. */
enum class RoundVerdict {
    FASTER,
    EVEN,
    SLOWER,
    ;

    companion object {
        fun of(gapMillis: Long): RoundVerdict = when {
            gapMillis < 0 -> FASTER
            gapMillis > 0 -> SLOWER
            else -> EVEN
        }
    }
}

/**
 * The times of a usual round, honestly compared (PLANNING.md §11 Phase 20, «The comparison,
 * honest»). Every time is a time in motion, the steps' own, so a pause or a chat is not in it. A
 * walk's time is brought to the round's usual length before it is compared (its time × the usual
 * length ÷ its own): a shortcut of three hundred metres is not a faster walk, and a detour not a
 * slower one. By the kilometre, the usual is the middle of the walks' own times at that kilometre,
 * from their legs, which stay when a route is deleted.
 */
object RoundTimes {
    /** [walk]'s time in motion as it would be over [meters] at the pace it walked. */
    fun brought(walk: Session, meters: Double): Long {
        val own = walk.totals.distanceMeters
        if (own <= 0.0 || meters <= 0.0) return walk.totals.movingMillis
        return (walk.totals.movingMillis * meters / own).roundToLong()
    }

    /** The usual of [walks]: their middle length, and their middle time brought to it. Null for none. */
    fun usual(walks: List<Session>): RoundUsual? {
        if (walks.isEmpty()) return null
        val meters = median(walks.map { it.totals.distanceMeters })
        val millis = median(walks.map { brought(it, meters).toDouble() }).roundToLong()
        return RoundUsual(meters, millis, walks.size)
    }

    /**
     * How [outing] compares with the other [walks] of its round walked before it. A usual needs
     * [RoundConstants.MIN_WALKS] of them: against one walk alone, «usual» would be «last time».
     */
    fun comparison(outing: Session, walks: List<Session>): RoundComparison {
        val before = walks.filter { it.id != outing.id && it.startedAtMillis < outing.startedAtMillis }
        val nth = before.size + 1
        val usual = before.takeIf { it.size >= RoundConstants.MIN_WALKS }?.let(::usual)
            ?: return RoundComparison(nth, usual = null, thisMillis = null, gapMillis = null)
        val mine = brought(outing, usual.meters)
        return RoundComparison(nth, usual, mine, even(mine - usual.movingMillis, usual.movingMillis))
    }

    /**
     * The usual time in motion at each of the first [count] kilometres (or miles, [unitMeters]):
     * the middle of the times [walks] reached it in, from their legs. Null at a kilometre fewer than
     * two walks reached whole (the round's last, which a walk a little shorter did not).
     */
    fun usualAtMarks(walks: List<Session>, unitMeters: Double, count: Int): List<Long?> {
        if (count <= 0) return emptyList()
        val reached = walks.map { walk ->
            var total = 0L
            Splits.of(walk.legs, unitMeters).filter { it.whole }.map { split ->
                total += split.movingMillis
                total
            }
        }
        return (1..count).map { mark ->
            val times = reached.mapNotNull { it.getOrNull(mark - 1) }
            if (times.size < RoundConstants.MIN_WALKS) null else median(times.map { it.toDouble() }).roundToLong()
        }
    }

    /**
     * [gapMillis], or zero when it is within what a walk's own wander is against [usualMillis]: a
     * fiftieth of it, and never under a quarter of a minute.
     */
    fun even(gapMillis: Long, usualMillis: Long): Long {
        val tolerance = maxOf(RoundConstants.EVEN_MIN_MILLIS, (usualMillis * RoundConstants.EVEN_SHARE).roundToLong())
        return if (abs(gapMillis) < tolerance) 0L else gapMillis
    }

    /** The middle of [values]: the mean of the two middle ones for an even count. */
    fun median(values: List<Double>): Double {
        require(values.isNotEmpty())
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
    }
}
