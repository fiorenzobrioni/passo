package com.callbackdev.passo.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.callbackdev.passo.core.designsystem.format.clockTime
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.today.Headline
import com.callbackdev.passo.core.domain.today.Pace

/** A walk's length: «23 minutes», or «1 h 20 min» from an hour up. */
@Composable
internal fun duration(minutes: Int): String = if (minutes < 60) {
    pluralStringResource(R.plurals.today_duration_minutes, minutes, minutes)
} else {
    stringResource(R.string.today_duration_hours_minutes, minutes / 60, minutes % 60)
}

/** The sentence for [headline], or for the detail line under it. */
@Composable
internal fun headlineText(headline: Headline, format: MeasureFormatter): String = when (headline) {
    Headline.NoStepsYet -> stringResource(R.string.today_headline_no_steps)

    is Headline.GoalReached -> stringResource(R.string.today_headline_goal_reached, clockTime(headline.minuteOfDay))

    is Headline.VersusUsual -> when (val pace = headline.pace) {
        is Pace.Ahead -> pluralStringResource(R.plurals.today_headline_ahead, pace.steps, format.steps(pace.steps))
        is Pace.Behind -> pluralStringResource(R.plurals.today_headline_behind, pace.steps, format.steps(pace.steps))
        Pace.OnPace -> stringResource(R.string.today_headline_on_pace)
    }

    is Headline.ToGo -> pluralStringResource(
        R.plurals.today_headline_to_go,
        headline.steps,
        format.steps(headline.steps),
        duration(headline.minutes),
    )
}
