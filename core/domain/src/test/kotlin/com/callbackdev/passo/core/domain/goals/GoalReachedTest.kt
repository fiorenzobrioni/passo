package com.callbackdev.passo.core.domain.goals

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GoalReachedTest {
    private val today = 20_720L

    @Test
    fun `the first time the goal is reached today is news`() {
        assertThat(GoalReached.isNews(today, steps = 8_000, goalSteps = 8_000, lastToldDay = today - 1)).isTrue()
        assertThat(GoalReached.isNews(today, steps = 8_000, goalSteps = 8_000, lastToldDay = null)).isTrue()
    }

    @Test
    fun `below the goal it is not`() {
        assertThat(GoalReached.isNews(today, steps = 7_999, goalSteps = 8_000, lastToldDay = null)).isFalse()
    }

    @Test
    fun `a day already told is not told again, as after a reboot reads the goal met once more`() {
        assertThat(GoalReached.isNews(today, steps = 9_500, goalSteps = 8_000, lastToldDay = today)).isFalse()
    }

    @Test
    fun `a time-zone change that brings back yesterday's date does not tell yesterday again`() {
        // Told at 23:30 in Rome; flying west, the phone's date is the day before for a while.
        assertThat(GoalReached.isNews(today - 1, steps = 12_000, goalSteps = 8_000, lastToldDay = today)).isFalse()
        // And once the date comes round again, that day was already told.
        assertThat(GoalReached.isNews(today, steps = 12_500, goalSteps = 8_000, lastToldDay = today)).isFalse()
    }

    @Test
    fun `the next day is news again`() {
        assertThat(GoalReached.isNews(today + 1, steps = 8_000, goalSteps = 8_000, lastToldDay = today)).isTrue()
    }

    @Test
    fun `a goal raised after it was reached is not a second goal the same day`() {
        assertThat(GoalReached.isNews(today, steps = 10_000, goalSteps = 10_000, lastToldDay = today)).isFalse()
    }
}
