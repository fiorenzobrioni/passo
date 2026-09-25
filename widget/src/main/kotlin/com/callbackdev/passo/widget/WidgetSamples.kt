package com.callbackdev.passo.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.HourlySteps
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDayCalculator
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UserSettings
import java.util.Random

/**
 * A realistic afternoon for the picker's generated previews (API 35+), where no reading exists
 * yet: the launcher asks for a card before the widget is placed, and a preview is a drawing of
 * the product, never a reading. Seeded, so the picker always shows the same day: a walk to the
 * station, a lunch walk, the scattered steps of a house and an office in between, at 15:40.
 */
internal object WidgetSamples {
    val FourByOne = DpSize(340.dp, 85.dp)
    val FourByTwo = DpSize(340.dp, 189.dp)

    const val NOW_MINUTE: Int = 15 * 60 + 40
    const val GOAL_STEPS: Int = 10_000

    fun model(look: WidgetLook = WidgetLook(), state: CountingState = CountingState.COUNTING): WidgetModel {
        val minutes = workday(seed = 5, lunchWalkMinutes = 36, until = NOW_MINUTE)
        val usual = TypicalDayCalculator.typical(listOf(11L, 23L, 37L, 41L).map { workday(it) })
        val overview = TodayOverview.of(
            minutes = minutes,
            profile = Profile(heightMeters = 1.78, weightKg = 74.0),
            goalSteps = GOAL_STEPS,
            nowMinute = NOW_MINUTE.toDouble(),
            typical = usual,
        )
        return WidgetModel(
            look = look,
            settings = UserSettings(dailyGoalSteps = GOAL_STEPS, onboardingCompleted = true),
            state = state,
            day = WidgetDay(overview, HourlySteps.of(minutes), NOW_MINUTE),
        )
    }

    /** The README's sample day (`feature/today`'s `SampleDays`), the same generator. */
    fun workday(seed: Long, lunchWalkMinutes: Int = 22, until: Int = 24 * 60): List<DayMinute> {
        val random = Random(seed)

        // Random.nextInt(from, until) is API 35. This is its algorithm (the JDK's
        // RandomSupport.boundedNextInt), so the day is the very one Today's screenshots show.
        fun between(from: Int, until: Int): Int {
            val n = until - from
            val m = n - 1
            var r = random.nextInt()
            if (n and m == 0) return (r and m) + from
            var u = r ushr 1
            while (true) {
                r = u % n
                if (u + m - r >= 0) return r + from
                u = random.nextInt() ushr 1
            }
        }
        val steps = sortedMapOf<Int, Int>()
        fun add(minute: Int, count: Int) {
            if (minute < until && count > 0) steps.merge(minute, count, Int::plus)
        }
        fun walk(start: Int, length: Int, cadence: Int) {
            for (i in 0 until length) add(start + i, cadence + between(-7, 8))
        }
        fun scatter(from: Int, to: Int, minutesPerHour: Int) {
            for (minute in from until to) if (random.nextInt(60) < minutesPerHour) add(minute, 6 + random.nextInt(34))
        }
        scatter(6 * 60 + 50, 7 * 60 + 50, 22)
        walk(7 * 60 + 55 + between(-4, 5), 18 + between(-2, 3), 108)
        scatter(8 * 60 + 20, 12 * 60 + 30, 12)
        walk(12 * 60 + 35, lunchWalkMinutes, 104)
        scatter(12 * 60 + 35 + lunchWalkMinutes, 17 * 60 + 35, 12)
        walk(17 * 60 + 40 + between(-5, 6), 20 + between(-2, 3), 110)
        scatter(18 * 60 + 5, 22 * 60 + 30, 14)
        return steps.map { (minute, count) -> DayMinute(minute, count) }
    }
}
