package com.callbackdev.passo.core.domain.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WidgetUpdatePolicyTest {
    private val minute = WidgetUpdatePolicy.MIN_INTERVAL_MILLIS

    @Test
    fun `screen on repaints at once`() {
        val policy = WidgetUpdatePolicy(interactive = false)
        assertThat(policy.decide(WidgetEvent.SCREEN_ON, nowElapsed = 0)).isEqualTo(WidgetDecision.Now)
        assertThat(policy.interactive).isTrue()
    }

    @Test
    fun `nothing moves while the screen is off`() {
        val policy = WidgetUpdatePolicy()
        policy.decide(WidgetEvent.SCREEN_OFF, nowElapsed = 0)
        assertThat(policy.decide(WidgetEvent.STEPS, nowElapsed = 10 * minute, steps = 500))
            .isEqualTo(WidgetDecision.Skip)
        assertThat(policy.decide(WidgetEvent.DAY_CHANGED, nowElapsed = 10 * minute)).isEqualTo(WidgetDecision.Skip)
    }

    @Test
    fun `the first count after start is shown at once`() {
        val policy = WidgetUpdatePolicy()
        assertThat(policy.decide(WidgetEvent.STEPS, nowElapsed = 5, steps = 10)).isEqualTo(WidgetDecision.Now)
    }

    @Test
    fun `while the screen is on at most once a minute on the trailing edge`() {
        val policy = WidgetUpdatePolicy()
        policy.pushed(nowElapsed = 1_000, steps = 100)
        assertThat(policy.decide(WidgetEvent.STEPS, nowElapsed = 21_000, steps = 130))
            .isEqualTo(WidgetDecision.Later(40_000))
        assertThat(policy.decide(WidgetEvent.STEPS, nowElapsed = 61_000, steps = 180)).isEqualTo(WidgetDecision.Now)
    }

    @Test
    fun `an unchanged count is not repainted`() {
        val policy = WidgetUpdatePolicy()
        policy.pushed(nowElapsed = 0, steps = 100)
        assertThat(policy.decide(WidgetEvent.STEPS, nowElapsed = 5 * minute, steps = 100))
            .isEqualTo(WidgetDecision.Skip)
    }

    @Test
    fun `reaching the goal skips the throttle`() {
        val policy = WidgetUpdatePolicy()
        policy.pushed(nowElapsed = 0, steps = 7_990)
        assertThat(policy.decide(WidgetEvent.STEPS, nowElapsed = 2_000, steps = 8_004, goalSteps = 8_000))
            .isEqualTo(WidgetDecision.Now)
        policy.pushed(nowElapsed = 2_000, steps = 8_004)
        // Past the goal it is an ordinary count again.
        assertThat(policy.decide(WidgetEvent.STEPS, nowElapsed = 4_000, steps = 8_050, goalSteps = 8_000))
            .isEqualTo(WidgetDecision.Later(minute - 2_000))
    }

    @Test
    fun `a new day repaints at once with the screen on`() {
        val policy = WidgetUpdatePolicy()
        policy.pushed(nowElapsed = 0, steps = 9_000)
        assertThat(policy.decide(WidgetEvent.DAY_CHANGED, nowElapsed = 1_000)).isEqualTo(WidgetDecision.Now)
    }

    @Test
    fun `tracking and settings repaint even with the screen off`() {
        val policy = WidgetUpdatePolicy(interactive = false)
        assertThat(policy.decide(WidgetEvent.TRACKING_STATE, nowElapsed = 0)).isEqualTo(WidgetDecision.Now)
        assertThat(policy.decide(WidgetEvent.SETTINGS, nowElapsed = 0)).isEqualTo(WidgetDecision.Now)
        assertThat(policy.interactive).isFalse()
    }
}
