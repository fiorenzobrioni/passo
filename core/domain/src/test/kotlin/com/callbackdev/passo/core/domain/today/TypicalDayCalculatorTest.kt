package com.callbackdev.passo.core.domain.today

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TypicalDayCalculatorTest {
    private fun day(vararg minutes: Pair<Int, Int>) = minutes.map { (m, s) -> DayMinute(m, s) }

    @Test
    fun `the candidates are the same weekday over the last four weeks`() {
        assertThat(TypicalDayCalculator.candidateDays(100)).containsExactly(93L, 86L, 79L, 72L).inOrder()
    }

    @Test
    fun `fewer than two valid days give no typical day`() {
        assertThat(TypicalDayCalculator.typical(emptyList())).isNull()
        assertThat(TypicalDayCalculator.typical(listOf(day(600 to 3_000)))).isNull()
    }

    @Test
    fun `days under 500 steps are left out`() {
        val typical = TypicalDayCalculator.typical(
            listOf(day(600 to 2_000), day(600 to 499), day(660 to 4_000)),
        )

        assertThat(typical?.daysUsed).isEqualTo(2)
    }

    @Test
    fun `the typical day appears once two valid days exist`() {
        val typical = TypicalDayCalculator.typical(listOf(day(600 to 2_000), day(600 to 4_000)))

        assertThat(typical).isNotNull()
        assertThat(typical?.at(1_440.0)).isEqualTo(3_000.0)
    }

    @Test
    fun `the mean is taken slot by slot`() {
        // One day walks in the morning, the other in the evening.
        val typical = TypicalDayCalculator.typical(listOf(day(480 to 1_000), day(1_200 to 1_000)))

        assertThat(typical?.at(0.0)).isEqualTo(0.0)
        assertThat(typical?.at(12 * 60.0)).isEqualTo(500.0)
        assertThat(typical?.at(1_440.0)).isEqualTo(1_000.0)
    }
}
