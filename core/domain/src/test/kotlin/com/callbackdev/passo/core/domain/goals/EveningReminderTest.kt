package com.callbackdev.passo.core.domain.goals

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class EveningReminderTest {
    @Test
    fun `below the goal it says the steps left and the brisk minutes they take`() {
        val nudge = EveningReminder.check(steps = 5_660, goalSteps = 8_000, thresholdPercent = 100)

        assertThat(nudge).isEqualTo(EveningNudge(steps = 5_660, goalSteps = 8_000, remaining = 2_340, minutes = 24))
    }

    @Test
    fun `a goal met is never reminded, whatever the threshold`() {
        assertThat(EveningReminder.check(8_000, 8_000, 100)).isNull()
        assertThat(EveningReminder.check(12_000, 8_000, 50)).isNull()
    }

    @Test
    fun `with a lower threshold a day close to the goal is left alone`() {
        assertThat(EveningReminder.check(steps = 6_000, goalSteps = 8_000, thresholdPercent = 75)).isNull()
        assertThat(EveningReminder.check(steps = 5_999, goalSteps = 8_000, thresholdPercent = 75)).isNotNull()
        assertThat(EveningReminder.check(steps = 4_000, goalSteps = 8_000, thresholdPercent = 50)).isNull()
        assertThat(EveningReminder.check(steps = 3_999, goalSteps = 8_000, thresholdPercent = 50)).isNotNull()
    }

    @Test
    fun `a day with no steps at all is reminded`() {
        assertThat(EveningReminder.check(0, 8_000, 50)?.remaining).isEqualTo(8_000)
    }
}
