package com.callbackdev.passo.feature.sessions

import android.content.res.Resources
import android.icu.text.PluralRules
import androidx.compose.runtime.Composable
import com.callbackdev.passo.core.designsystem.format.datePattern
import com.callbackdev.passo.core.designsystem.format.durationWords
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.format.roundGapWords
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.rounds.KeptRound
import com.callbackdev.passo.core.domain.rounds.RoundTrend
import com.callbackdev.passo.core.domain.rounds.RoundVerdict
import java.time.LocalDate

/**
 * A round's name (PLANNING.md §11 Phase 20): the reader's own, or, until they give one, its usual
 * length: «Your round of 3.20 km», «Your running round of 5.00 km».
 */
fun Resources.roundName(round: KeptRound, format: MeasureFormatter): String = round.round.name ?: getString(
    if (round.running) R.string.round_name_run else R.string.round_name,
    format(format.distance(round.usual.meters)),
)

/**
 * Which time a walk of a round was: «the 6th time», «la 6ª volta», «l’8ª volta». The ending is the
 * language's own, chosen by its ordinal rules (ICU's), each form a resource.
 */
fun Resources.roundTime(nth: Int): String {
    val rules = PluralRules.forLocale(configuration.locales[0], PluralRules.PluralType.ORDINAL)
    val form = when (rules.select(nth.toDouble())) {
        PluralRules.KEYWORD_ONE -> R.string.round_time_one
        PluralRules.KEYWORD_TWO -> R.string.round_time_two
        PluralRules.KEYWORD_FEW -> R.string.round_time_few
        PluralRules.KEYWORD_MANY -> R.string.round_time_many
        else -> R.string.round_time_other
    }
    return getString(form, nth)
}

/** A time in motion of a round's, in words: «38 minutes», «1 h 5 min». */
fun Resources.roundMinutes(millis: Long): String =
    durationWords(((millis + MILLIS_PER_MINUTE / 2) / MILLIS_PER_MINUTE).toInt().coerceAtLeast(1))

/**
 * How a walk went against its round's usual, as its page says it: «2 minutes faster than usual»,
 * «30 seconds slower than usual», «in its usual time».
 */
fun Resources.roundGapPhrase(gapMillis: Long): String = when (RoundVerdict.of(gapMillis)) {
    RoundVerdict.FASTER -> getString(R.string.outing_round_faster, roundGapWords(-gapMillis))
    RoundVerdict.SLOWER -> getString(R.string.outing_round_slower, roundGapWords(gapMillis))
    RoundVerdict.EVEN -> getString(R.string.outing_round_even)
}

/** «2 rounds», «9 walks»: counts written as the app writes its numbers. */
fun Resources.roundsCount(count: Int, format: MeasureFormatter): String =
    getQuantityString(R.plurals.rounds_count, count, format.steps(count))

fun Resources.roundWalks(count: Int, format: MeasureFormatter): String =
    getQuantityString(R.plurals.rounds_walks, count, format.steps(count))

/**
 * The round's sentence, its chart's for TalkBack too: «6 walks since March; the last 3 faster than
 * usual.» The month says its year before this one.
 */
@Composable
fun Resources.roundTrendSentence(trend: RoundTrend, today: LocalDate, format: MeasureFormatter): String {
    val first = LocalDate.ofEpochDay(trend.sinceDay)
    val since = datePattern(first, if (first.year == today.year) "MMMM" else "MMMMyyyy", capitalized = false)
    val walks = roundWalks(trend.walks, format)
    val plural = when (trend.last) {
        RoundVerdict.FASTER -> R.plurals.round_trend_faster
        RoundVerdict.SLOWER -> R.plurals.round_trend_slower
        RoundVerdict.EVEN -> R.plurals.round_trend_even
    }
    return getQuantityString(plural, trend.lastRun, walks, since, trend.lastRun)
}

private const val MILLIS_PER_MINUTE = 60_000L
