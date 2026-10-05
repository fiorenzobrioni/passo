package com.callbackdev.passo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.domain.today.DayMinute
import com.callbackdev.passo.core.domain.today.HourlySteps
import com.callbackdev.passo.core.domain.today.TodayOverview
import com.callbackdev.passo.core.domain.today.TypicalDayCalculator
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.IntervalSets
import com.callbackdev.passo.core.model.Profile
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.core.model.UserSettings
import com.callbackdev.passo.widget.glance.GlanceWidgetContent
import com.callbackdev.passo.widget.words.WordsWidgetContent
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Nothing a card prints is cut, at any reference grant, in either arrangement, in English and
 * Italian, at three text sizes, with counts past ten thousand and the longest sentences, and
 * with every line drawn 5% wider than measured ([STRETCH]): what a launcher on another face may
 * do (the owner's Samsung, 5 Oct 2026). Every line is whole, or in fewer words, or not there.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class WidgetFitTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private val now = 15 * 60 + 40

    /** A day of [steps] by now, all of it walked from 6:00, its goal [goal]; against a usual day if [usual]. */
    private fun day(steps: Int, goal: Int, usual: Boolean): WidgetModel {
        val minutes = (6 * 60 until now).let { range ->
            val each = steps / range.count()
            range.mapIndexed { i, m -> DayMinute(m, if (i == 0) steps - each * (range.count() - 1) else each) }
        }
        val typical = if (usual) {
            TypicalDayCalculator.typical(listOf(11L, 23L, 37L, 41L).map { WidgetSamples.workday(it) })
        } else {
            null
        }
        val overview = TodayOverview.of(
            minutes = minutes,
            profile = Profile(heightMeters = 1.72, weightKg = 73.0),
            goalSteps = goal,
            nowMinute = now.toDouble(),
            typical = typical,
        )
        return WidgetModel(
            look = WidgetLook(),
            settings = UserSettings(dailyGoalSteps = goal, onboardingCompleted = true),
            state = CountingState.COUNTING,
            day = WidgetDay(overview, HourlySteps.of(minutes), now),
        )
    }

    private val outing = Session(
        planId = 1,
        name = null,
        goalKind = SessionGoalKind.INTERVALS,
        goalValue = 30,
        intensity = SessionIntensity.BRISK,
        milestones = emptySet(),
        vibrate = true,
        localEpochDay = 0,
        startedAtMillis = 0,
        totals = SessionTotals(steps = 12_345, movingMillis = 14 * 60_000L),
        intervals = IntervalSets(),
    )

    private val days = listOf(
        day(9_630, 8_000, usual = false),
        day(12_345, 10_000, usual = true),
        day(28_888, 25_000, usual = false),
        day(18_765, 25_000, usual = true),
        day(4_321, 30_000, usual = false),
        day(0, 10_000, usual = false),
        day(99_999, 50_000, usual = false),
        day(12_345, 10_000, usual = false).copy(state = CountingState.PAUSED),
        day(12_345, 30_000, usual = false).copy(session = outing),
    )

    private val sizes = listOf(
        Grants.OneByOne,
        Grants.TwoByOne,
        Grants.ThreeByOne,
        Grants.FourByOne,
        Grants.OneByTwo,
        Grants.TwoByTwo,
        Grants.ThreeByTwo,
        Grants.FourByTwo,
        Grants.FourByThree,
    )

    private fun cuts(scale: Float): List<String> {
        // The Words card under way shows an outing; the Glance one too, in the sentence's place.
        RuntimeEnvironment.setFontScale(scale)
        val found = mutableListOf<String>()
        days.forEach { model ->
            val mirrored = model.copy(look = WidgetLook(arrangement = WidgetArrangement.RING_END))
            sizes.forEach { size ->
                val at = "${size.width.value.toInt()}x${size.height.value.toInt()} @$scale"
                cutTexts(context, size, STRETCH) { GlanceWidgetContent(model) }.forEach { found += "glance $at: $it" }
                cutTexts(context, size, STRETCH) { GlanceWidgetContent(mirrored) }.forEach {
                    found +=
                        "mirrored $at: $it"
                }
                cutTexts(context, size, STRETCH) { WordsWidgetContent(model) }.forEach { found += "words $at: $it" }
            }
        }
        return found
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `nothing is cut in Italian, on a wider face, at the default text size`() {
        val found = cuts(1f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xhdpi")
    fun `nothing is cut in English, on a wider face, at the default text size`() {
        val found = cuts(1f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `nothing is cut in Italian, on a wider face, at larger text`() {
        val found = cuts(1.15f) + cuts(1.3f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    @Test
    @Config(qualifiers = "en-rUS-w411dp-h891dp-xhdpi")
    fun `nothing is cut in English, on a wider face, at larger text`() {
        val found = cuts(1.15f) + cuts(1.3f)
        assertWithMessage(found.joinToString("\n")).that(found).isEmpty()
    }

    private companion object {
        const val STRETCH = 1.05f
    }
}
