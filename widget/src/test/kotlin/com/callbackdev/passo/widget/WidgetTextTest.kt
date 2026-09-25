package com.callbackdev.passo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.format.MeasureFormatter
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDayCalculator
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.UnitSystem
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class WidgetTextTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun overview(minutes: List<DayMinute>, now: Int, usual: List<List<DayMinute>>? = null) = TodayOverview.of(
        minutes = minutes,
        profile = Profile(),
        goalSteps = 8_000,
        nowMinute = now.toDouble(),
        typical = usual?.let { TypicalDayCalculator.typical(it) },
    )

    private val english = MeasureFormatter(Locale.US, UnitSystem.METRIC)
    private val italian = MeasureFormatter(Locale.ITALY, UnitSystem.METRIC)

    @Test
    @Config(qualifiers = "en-rUS")
    fun `the sentence is Today's headline, brief`() {
        assertThat(sentence(context, overview(emptyList(), 480), english)).isEqualTo("No steps yet today")
        assertThat(
            sentence(context, overview(listOf(DayMinute(600, 1_000)), 720), english),
        ).isEqualTo("7,000 steps to go")
        val ahead = overview(listOf(DayMinute(600, 3_000)), 720, usual = List(2) { listOf(DayMinute(600, 1_000)) })
        assertThat(sentence(context, ahead, english)).isEqualTo("2,000 steps ahead of usual")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `in Italian too, with its plurals`() {
        assertThat(sentence(context, overview(listOf(DayMinute(600, 7_999)), 720), italian)).isEqualTo("Manca 1 passo")
        assertThat(
            sentence(context, overview(listOf(DayMinute(600, 1_000)), 720), italian),
        ).isEqualTo("Mancano 7.000 passi")
        val behind = overview(listOf(DayMinute(600, 1_000)), 720, usual = List(2) { listOf(DayMinute(600, 3_000)) })
        assertThat(sentence(context, behind, italian)).isEqualTo("2.000 passi in meno del solito")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `the goal reads as a fact, floored`() {
        val day = overview(listOf(DayMinute(600, 7_999)), 720)
        assertThat(goalShareText(context, day, english)).isEqualTo("99% of 8,000")
        assertThat(goalOfText(context, 8_000, english, short = false)).isEqualTo("of 8,000 steps")
        assertThat(goalOfText(context, 8_000, english, short = true)).isEqualTo("of 8,000")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `the estimates say they are estimates`() {
        val day = overview(listOf(DayMinute(600, 100), DayMinute(601, 100)), 720)
        assertThat(metricsText(context, day, english)).startsWith("≈ ")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `only a count that is not moving has a status, and each says what a tap does`() {
        assertThat(statusText(context, CountingState.COUNTING)).isNull()
        assertThat(statusText(context, CountingState.PAUSED)).isEqualTo("Paused · tap to resume")
        assertThat(statusText(context, CountingState.PAUSED, short = true)).isEqualTo("Paused")
        assertThat(statusText(context, CountingState.STOPPED)).contains("tap")
        assertThat(statusText(context, CountingState.PERMISSION_NEEDED)).contains("tap")
    }

    @Test
    @Config(qualifiers = "en-rUS")
    fun `one cell shortens thousands where the language does`() {
        assertThat(compactCount(context, 7_855)).isEqualTo("7.9K")
        assertThat(compactCount(context, 640)).isEqualTo("640")
    }

    @Test
    @Config(qualifiers = "it-rIT")
    fun `Italian does not shorten thousands, so the figure stays whole`() {
        assertThat(compactCount(context, 7_855)).isEqualTo("7855")
    }
}
