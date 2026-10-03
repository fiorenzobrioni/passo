package com.callbackdev.passo.core.designsystem.format

import android.content.res.Resources
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.designsystem.ways.walkOutingRes
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.metrics.MetricsConstants
import com.callbackdev.passo.core.domain.sessions.SessionAmount
import com.callbackdev.passo.core.domain.sessions.SessionHeadline
import com.callbackdev.passo.core.domain.sessions.cadenceFloor
import com.callbackdev.passo.core.domain.sessions.done
import com.callbackdev.passo.core.domain.sessions.goal
import com.callbackdev.passo.core.domain.sessions.intervalAt
import com.callbackdev.passo.core.domain.sessions.intervalResult
import com.callbackdev.passo.core.domain.sessions.progress
import com.callbackdev.passo.core.domain.ways.NextPlace
import com.callbackdev.passo.core.domain.ways.WalkPlaces
import com.callbackdev.passo.core.domain.ways.Ways
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionEnd
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionPlan
import com.callbackdev.passo.core.model.SessionState
import kotlin.math.roundToInt

/*
 * An outing in words (PLANNING.md §11 Phase 10), the same on Today, in History, on the Outings
 * page and in the notifications: plain functions over Resources, so a notification and a
 * composable say it alike.
 */

/**
 * The reader's name for it, or its intensity's ("Brisk walk"), or "Finish the day"; an interval
 * outing is "Japanese walking" with the protocol's three and three minutes, "Interval walk" with
 * any other.
 */
fun Resources.sessionName(
    name: String?,
    intensity: SessionIntensity,
    restOfDay: Boolean,
    intervals: IntervalSets? = null,
): String = name?.takeIf { it.isNotBlank() } ?: when {
    restOfDay -> getString(R.string.session_name_rest_of_day)

    intervals != null -> getString(
        if (intervals.slowMinutes == PROTOCOL_MINUTES && intervals.fastMinutes == PROTOCOL_MINUTES) {
            R.string.session_name_japanese
        } else {
            R.string.session_name_intervals
        },
    )

    else -> getString(
        when (intensity) {
            SessionIntensity.FREE -> R.string.session_name_free
            SessionIntensity.BRISK -> R.string.session_name_brisk
            SessionIntensity.VIGOROUS -> R.string.session_name_vigorous
            SessionIntensity.RUN -> R.string.session_name_run
        },
    )
}

fun Resources.sessionName(plan: SessionPlan): String = sessionName(
    plan.name,
    plan.intensity,
    plan.goalKind == SessionGoalKind.REST_OF_DAY,
    plan.intervals.takeIf { plan.goalKind == SessionGoalKind.INTERVALS },
)

/** An outing on a city walk is named by its walk («A walk in Milan»), any other as above. */
fun Resources.sessionName(session: Session): String = session.walk?.let(::walkOutingRes)?.let(::getString)
    ?: sessionName(session.name, session.intensity, session.restOfDay, session.intervals)

/** «Next: the Duomo, 600 m»: the place ahead on a walk. */
fun Resources.walkNext(next: NextPlace, format: MeasureFormatter): String = getString(
    R.string.walk_next,
    getString(placeNameRes(next.stop.key)),
    format(format.aheadDistance(next.inMeters.toDouble())),
)

fun Resources.intensityLabel(intensity: SessionIntensity): String = getString(
    when (intensity) {
        SessionIntensity.FREE -> R.string.session_intensity_free
        SessionIntensity.BRISK -> R.string.session_intensity_brisk
        SessionIntensity.VIGOROUS -> R.string.session_intensity_vigorous
        SessionIntensity.RUN -> R.string.session_intensity_run
    },
)

fun Resources.intensityDetail(intensity: SessionIntensity): String = getString(
    when (intensity) {
        SessionIntensity.FREE -> R.string.session_intensity_free_detail
        SessionIntensity.BRISK -> R.string.session_intensity_brisk_detail
        SessionIntensity.VIGOROUS -> R.string.session_intensity_vigorous_detail
        SessionIntensity.RUN -> R.string.session_intensity_run_detail
    },
)

/** An amount with its unit: «20 min», «3,000 steps», «1.50 km». */
fun Resources.sessionAmount(amount: SessionAmount, format: MeasureFormatter): String = when (amount.kind) {
    SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> {
        val steps = amount.value.roundToInt()
        getQuantityString(R.plurals.session_amount_steps, steps, format.steps(steps))
    }

    SessionGoalKind.DISTANCE -> format(format.distance(amount.value))

    SessionGoalKind.TIME, SessionGoalKind.INTERVALS -> format(format.minutes(amount.value.roundToInt()))
}

/** An amount's number alone, as the first half of «12 of 20 min». */
private fun sessionNumber(amount: SessionAmount, format: MeasureFormatter): String = when (amount.kind) {
    SessionGoalKind.STEPS, SessionGoalKind.REST_OF_DAY -> format.steps(amount.value.roundToInt())
    SessionGoalKind.DISTANCE -> format.distance(amount.value).number
    SessionGoalKind.TIME, SessionGoalKind.INTERVALS -> format.integer(amount.value.toLong())
}

/** What a plan is: «20 min at a brisk pace», «The rest of today's goal at a brisk pace». */
fun Resources.planDescription(plan: SessionPlan, format: MeasureFormatter): String {
    if (plan.goalKind == SessionGoalKind.INTERVALS) return intervalsPhrase(plan.intervals, plan.intensity, format)
    val amount = if (plan.goalKind == SessionGoalKind.REST_OF_DAY) {
        getString(R.string.session_amount_rest_of_day)
    } else {
        sessionAmount(SessionAmount(plan.goalKind, plan.goalValue.toDouble()), format)
    }
    return intensityPhrase(plan.intensity, amount)
}

/** What an outing set out to do: «20 min at a brisk pace». */
fun Resources.sessionGoalDescription(session: Session, format: MeasureFormatter): String =
    session.intervals?.let { intervalsPhrase(it, session.intensity, format) }
        ?: intensityPhrase(session.intensity, sessionAmount(session.goal(), format))

/** An interval outing's sets: «5 sets: 3 min slow, 3 min brisk». */
fun Resources.intervalsPhrase(intervals: IntervalSets, intensity: SessionIntensity, format: MeasureFormatter): String =
    getQuantityString(
        when (intensity) {
            SessionIntensity.VIGOROUS -> R.plurals.session_intervals_vigorous
            SessionIntensity.RUN -> R.plurals.session_intervals_run
            SessionIntensity.FREE, SessionIntensity.BRISK -> R.plurals.session_intervals_brisk
        },
        intervals.sets,
        intervals.sets,
        format(format.minutes(intervals.slowMinutes)),
        format(format.minutes(intervals.fastMinutes)),
    )

/** [amount] at [intensity]'s pace: «20 min at a brisk pace». Shared with the spoken signals. */
fun Resources.intensityPhrase(intensity: SessionIntensity, amount: String): String = getString(
    when (intensity) {
        SessionIntensity.FREE -> R.string.session_plan_free
        SessionIntensity.BRISK -> R.string.session_plan_brisk
        SessionIntensity.VIGOROUS -> R.string.session_plan_vigorous
        SessionIntensity.RUN -> R.string.session_plan_run
    },
    amount,
).replaceFirstChar { it.uppercase(configuration.locales[0]) }

/** «12 of 20 min», «1,240 of 3,000 steps»; an interval outing's set, «Set 2 of 5». */
fun Resources.sessionProgress(session: Session, format: MeasureFormatter): String {
    session.intervalAt(session.lastStepAtMillis)?.let {
        return getString(R.string.session_interval_set, it.set, it.sets)
    }
    return getString(
        R.string.session_progress,
        sessionNumber(session.done(), format),
        sessionAmount(session.goal(), format),
    )
}

/** An interval's word: «Fast», «Slow». */
fun Resources.intervalWord(fast: Boolean): String =
    getString(if (fast) R.string.session_interval_fast else R.string.session_interval_slow)

/** What is left of an interval, as a countdown: «1:40», whole seconds rounded up. */
fun countdown(millis: Long): String {
    val seconds = (millis.coerceAtLeast(0) + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    return "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
}

/**
 * The one sentence an outing is told with. On a city walk, while it goes, the place ahead: what
 * the reader walks towards says more than the metres left.
 */
fun Resources.sessionHeadline(
    session: Session,
    format: MeasureFormatter,
    nowMillis: Long = session.lastStepAtMillis,
): String {
    val headline = SessionHeadline.of(session, nowMillis)
    val going = headline is SessionHeadline.Starting || headline is SessionHeadline.Going ||
        headline is SessionHeadline.PastHalf || headline is SessionHeadline.AlmostThere
    if (going) WalkPlaces.next(session)?.let { return walkNext(it, format) }
    val walk = session.walk
    if (headline is SessionHeadline.Reached && walk != null) {
        return getString(R.string.walk_done, getString(placeNameRes(Ways.of(walk).stops.last().key)))
    }
    return when (headline) {
        is SessionHeadline.Starting -> getString(
            R.string.session_headline_starting,
            sessionAmount(headline.goal, format),
        )

        is SessionHeadline.Going -> amountPlural(R.plurals.session_headline_going, headline.left, format)

        is SessionHeadline.PastHalf -> amountPlural(R.plurals.session_headline_past_half, headline.left, format)

        is SessionHeadline.AlmostThere -> amountPlural(R.plurals.session_headline_almost, headline.left, format)

        SessionHeadline.Paused -> getString(R.string.session_headline_paused)

        is SessionHeadline.Interval -> getString(
            when {
                !headline.position.fast -> R.string.session_headline_slow
                headline.position.lastSet -> R.string.session_headline_last_fast
                else -> R.string.session_headline_fast
            },
            countdown(headline.position.leftMillis),
        )

        is SessionHeadline.Reached -> getString(R.string.session_headline_reached, sessionAmount(headline.goal, format))

        is SessionHeadline.Ended -> getString(
            when (headline.end) {
                SessionEnd.IDLE -> R.string.session_headline_idle
                SessionEnd.CLOSED -> R.string.session_headline_closed
                SessionEnd.GOAL, SessionEnd.STOPPED -> R.string.session_headline_stopped
            },
            format.percent(headline.progress.coerceAtMost(1.0)),
        )
    }
}

private fun Resources.amountPlural(id: Int, amount: SessionAmount, format: MeasureFormatter): String {
    // A distance reads as many whatever its value: «mancano 1,20 km».
    val quantity = if (amount.kind == SessionGoalKind.DISTANCE) 2 else amount.value.roundToInt()
    return getQuantityString(id, quantity, sessionAmount(amount, format))
}

/**
 * The cadence now against the outing's own: «108 steps/min: on pace», «92 steps/min: below
 * your pace»; with no cadence to keep, its band in words.
 */
fun Resources.sessionCadence(cadence: Int?, intensity: SessionIntensity, format: MeasureFormatter): String {
    if (cadence == null) return getString(R.string.session_cadence_waiting)
    val measure = format(format.cadence(cadence))
    val floor = intensity.cadenceFloor
    return when {
        floor == null -> getString(R.string.session_cadence_free, measure, cadenceBandWord(cadence))
        cadence >= floor -> getString(R.string.session_cadence_on_pace, measure)
        else -> getString(R.string.session_cadence_below, measure)
    }
}

private fun Resources.cadenceBandWord(cadence: Int): String = getString(
    when {
        cadence >= MetricsConstants.RUNNING_CADENCE -> R.string.session_cadence_running
        cadence >= MetricsConstants.VIGOROUS_CADENCE -> R.string.session_cadence_vigorous
        cadence >= MetricsConstants.BRISK_MINUTE_THRESHOLD -> R.string.session_cadence_brisk
        else -> R.string.session_cadence_relaxed
    },
)

/** «17 of 20 min at the pace you set»; null for an outing with no pace to keep, or not moved yet. */
fun Resources.sessionZone(session: Session, format: MeasureFormatter): String? {
    session.intervalResult()?.let { result ->
        if (result.judged == 0) return null
        return getQuantityString(R.plurals.session_intervals_at_pace, result.atPace, result.atPace, result.judged)
    }
    if (session.intensity.cadenceFloor == null || session.totals.movingMillis <= 0) return null
    val zone = (session.totals.zoneMillis / MILLIS_PER_MINUTE).toInt()
    val moving = (session.totals.movingMillis / MILLIS_PER_MINUTE).toInt()
    return getString(R.string.session_zone, format.integer(zone.toLong()), format(format.minutes(moving)))
}

/** «Goal reached», or «72% of the goal». */
fun Resources.sessionOutcome(session: Session, format: MeasureFormatter): String = if (session.reached) {
    getString(R.string.session_outcome_reached)
} else {
    getString(R.string.session_outcome_share, format.percent(session.progress().coerceAtMost(1.0)))
}

/** «Estimated: 1.52 km · 91 kcal». */
fun Resources.sessionEstimates(session: Session, format: MeasureFormatter): String = getString(
    R.string.session_estimated,
    format(format.distance(session.totals.distanceMeters)),
    format(format.energy(session.totals.activeKcal)),
)

/**
 * An outing in one line, for a widget or a tile: «Brisk walk: 12 of 20 min», «Run: paused». Where
 * it wraps, it wraps at the colon: the progress is kept whole.
 */
fun Resources.sessionBrief(session: Session, format: MeasureFormatter): String =
    if (session.state == SessionState.PAUSED) {
        getString(R.string.session_brief_paused, sessionName(session))
    } else {
        val where = session.intervalAt(session.lastStepAtMillis)
            ?.let { getString(R.string.session_interval_set_inline, it.set, it.sets) }
            ?: sessionProgress(session, format)
        getString(R.string.session_brief, sessionName(session), where.replace(' ', NO_BREAK))
    }

private const val NO_BREAK = '\u00A0'

/** «2,140 steps». */
fun Resources.sessionSteps(session: Session, format: MeasureFormatter): String =
    getQuantityString(R.plurals.session_amount_steps, session.totals.steps, format.steps(session.totals.steps))

private const val MILLIS_PER_MINUTE = 60_000L
private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L

/** The protocol's minutes, slow and fast (Nemoto et al., 2007): what makes an interval walk Japanese. */
private const val PROTOCOL_MINUTES = 3
