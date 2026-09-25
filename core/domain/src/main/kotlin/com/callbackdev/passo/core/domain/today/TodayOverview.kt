package com.callbackdev.passo.core.domain.today

import com.callbackdev.passo.core.domain.metrics.DayMetrics
import com.callbackdev.passo.core.domain.metrics.MetricsCalculator
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.BRISK_MINUTE_THRESHOLD
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.DAILY_BRISK_SHARE_MINUTES
import com.callbackdev.passo.core.domain.metrics.MetricsConstants.VIGOROUS_CADENCE
import com.callbackdev.passo.core.model.Profile
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** How the day compares with a usual one at the same time. */
sealed interface Pace {
    /** More steps than usual by now. */
    data class Ahead(val steps: Int) : Pace

    /** Fewer steps than usual by now. */
    data class Behind(val steps: Int) : Pace

    /** Close enough to usual that a difference would be noise ([ON_PACE_TOLERANCE]). */
    data object OnPace : Pace
}

/** The one sentence Today opens with (Chiaro's "one sentence before any number"). */
sealed interface Headline {
    /** Nothing walked yet today. */
    data object NoStepsYet : Headline

    /** The goal was met at [minuteOfDay], and the day is [over] steps past it. */
    data class GoalReached(val minuteOfDay: Int, val over: Int) : Headline

    /** Compared with a usual day of the same weekday. */
    data class VersusUsual(val pace: Pace) : Headline

    /** [steps] still to walk, about [minutes] of brisk walking. */
    data class ToGo(val steps: Int, val minutes: Int) : Headline
}

/** What an average cadence means, in words (the CADENCE-adults bands). */
enum class CadenceBand {
    /** Under 100 spm: a relaxed pace, below moderate intensity. */
    RELAXED,

    /** 100 to 129 spm: brisk, moderate intensity. */
    BRISK,

    /** 130 spm and above: vigorous. */
    VIGOROUS,
}

/**
 * Everything Today shows about the day, computed at once from its minutes (PLANNING.md §6).
 *
 * @property headline the sentence the screen opens with.
 * @property detail the second line under it, or null when the headline already said it all.
 * @property usualNow the usual running total at this time; null without a typical day.
 * @property goalReachedAt the minute of the day the goal was met, or null.
 * @property briskShareLeft brisk minutes still missing from today's share of the WHO's weekly
 *   150, zero once it is done.
 */
data class TodayOverview(
    val steps: Int,
    val goalSteps: Int,
    val metrics: DayMetrics,
    val curve: DayCurve,
    val typical: TypicalDay?,
    val usualNow: Int?,
    val pace: Pace?,
    val goalReachedAt: Int?,
    val headline: Headline,
    val detail: Headline?,
    val cadenceBand: CadenceBand?,
    val briskShareLeft: Int,
) {
    /** Progress towards the goal, not capped: 1.2 is 120%. */
    val progress: Double get() = if (goalSteps <= 0) 0.0 else steps.toDouble() / goalSteps

    /** Where a usual day stands by now, as a share of the goal; null without a typical day. */
    val usualProgress: Double? get() = usualNow?.let { if (goalSteps <= 0) null else it.toDouble() / goalSteps }

    val remaining: Int get() = (goalSteps - steps).coerceAtLeast(0)

    companion object {
        /**
         * A pace difference under this share of the usual total, or under
         * [ON_PACE_MIN_STEPS], is reported as on pace: a hundred steps either way is a trip to
         * the kitchen, not a trend.
         */
        const val ON_PACE_TOLERANCE: Double = 0.05
        const val ON_PACE_MIN_STEPS: Int = 150

        /**
         * Builds the overview of a day made of [minutes], at [nowMinute] of it.
         *
         * @param liveSteps the service's own count when it runs, which includes the steps not
         *   yet written; it wins over the minutes' sum when higher, so the number on screen
         *   never goes back while a batch is being written.
         */
        fun of(
            minutes: List<DayMinute>,
            profile: Profile,
            goalSteps: Int,
            nowMinute: Double,
            typical: TypicalDay?,
            liveSteps: Int? = null,
        ): TodayOverview {
            val metrics = MetricsCalculator.day(minutes.map { it.steps }, profile)
            val steps = maxOf(metrics.steps, liveSteps ?: 0)
            val usualNow = typical?.at(nowMinute)?.roundToInt()
            val pace = usualNow?.let { paceOf(steps, it) }
            val reachedAt = if (steps >=
                goalSteps
            ) {
                DayCurve.minuteReaching(minutes, goalSteps) ?: nowMinute.toInt()
            } else {
                null
            }
            val toGo = (goalSteps - steps).coerceAtLeast(0)
            val walk = Headline.ToGo(toGo, minutesToWalk(toGo))
            val (headline, detail) = when {
                reachedAt != null -> Headline.GoalReached(reachedAt, steps - goalSteps) to
                    pace?.let(Headline::VersusUsual)

                steps == 0 -> Headline.NoStepsYet to walk

                pace != null -> Headline.VersusUsual(pace) to walk

                else -> walk to null
            }
            return TodayOverview(
                steps = steps,
                goalSteps = goalSteps,
                metrics = metrics,
                curve = DayCurve.of(minutes),
                typical = typical,
                usualNow = usualNow,
                pace = pace,
                goalReachedAt = reachedAt,
                headline = headline,
                detail = detail,
                cadenceBand = metrics.averageCadence?.let(::cadenceBand),
                briskShareLeft = (DAILY_BRISK_SHARE_MINUTES - metrics.briskMinutes).coerceAtLeast(0),
            )
        }

        fun paceOf(steps: Int, usual: Int): Pace {
            val delta = steps - usual
            val tolerance = maxOf(ON_PACE_MIN_STEPS.toDouble(), usual * ON_PACE_TOLERANCE)
            return when {
                abs(delta) < tolerance -> Pace.OnPace
                delta > 0 -> Pace.Ahead(delta)
                else -> Pace.Behind(-delta)
            }
        }

        /** Minutes of brisk walking (100 steps a minute) that [steps] take, rounded up. */
        fun minutesToWalk(steps: Int): Int = ceil(steps.toDouble() / BRISK_MINUTE_THRESHOLD).toInt()

        fun cadenceBand(stepsPerMinute: Int): CadenceBand = when {
            stepsPerMinute >= VIGOROUS_CADENCE -> CadenceBand.VIGOROUS
            stepsPerMinute >= BRISK_MINUTE_THRESHOLD -> CadenceBand.BRISK
            else -> CadenceBand.RELAXED
        }
    }
}
