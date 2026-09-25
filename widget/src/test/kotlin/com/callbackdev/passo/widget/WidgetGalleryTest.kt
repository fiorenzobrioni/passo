package com.callbackdev.passo.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.theme.WidgetCardColor
import com.callbackdev.passo.core.domain.widget.CountingState
import com.callbackdev.passo.core.model.Session
import com.callbackdev.passo.core.model.SessionGoalKind
import com.callbackdev.passo.core.model.SessionIntensity
import com.callbackdev.passo.core.model.SessionMilestone
import com.callbackdev.passo.core.model.SessionState
import com.callbackdev.passo.core.model.SessionTotals
import com.callbackdev.passo.widget.glance.GlanceWidgetContent
import com.callbackdev.passo.widget.words.WordsWidgetContent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Every form of both cards, drawn to `build/screenshots` to be looked at after a change to a
 * layout (CLAUDE.md). Not an assertion: the arithmetic is pinned by the layout tests.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xhdpi")
class WidgetGalleryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val out = File("build/screenshots")

    private val sizes = listOf(
        Grants.OneByOne,
        Grants.TwoByOne,
        Grants.ThreeByOne,
        Grants.FourByOne,
        Grants.TwoByTwo,
        Grants.ThreeByTwo,
        Grants.FourByTwo,
        Grants.FourByThree,
    )

    @Test
    fun `at a glance, every size`() {
        val board = HomeBoard(context, widthDp = 380)
        sizes.forEach { size ->
            board.row(size to renderCard(context, size) { GlanceWidgetContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-glance-sizes")
    }

    @Test
    fun `in words, every size`() {
        val board = HomeBoard(context, widthDp = 380)
        sizes.forEach { size ->
            board.row(size to renderCard(context, size) { WordsWidgetContent(WidgetSamples.model()) })
        }
        board.draw().saveTo(out, "widget-words-sizes")
    }

    @Test
    fun `the pair side by side, in every dress`() {
        val looks = listOf(
            WidgetLook(),
            WidgetLook(cardColor = WidgetCardColor.CLAY),
            WidgetLook(background = WidgetBackground.LIGHT),
            WidgetLook(background = WidgetBackground.DARK),
            WidgetLook(opacityPct = 0, background = WidgetBackground.SYSTEM),
        )
        val board = HomeBoard(context, widthDp = 380)
        looks.forEach { look ->
            board.row(
                Grants.FourByOne to
                    renderCard(context, Grants.FourByOne) { GlanceWidgetContent(WidgetSamples.model(look)) },
            )
            board.row(
                Grants.FourByOne to
                    renderCard(context, Grants.FourByOne) { WordsWidgetContent(WidgetSamples.model(look)) },
            )
        }
        board.draw().saveTo(out, "widget-dresses")
    }

    @Test
    fun `the ring on the right`() {
        val look = WidgetLook(arrangement = WidgetArrangement.RING_END)
        val board = HomeBoard(context, widthDp = 380)
        listOf(Grants.TwoByOne, Grants.ThreeByOne, Grants.FourByOne, Grants.FourByTwo).forEach { size ->
            board.row(size to renderCard(context, size) { GlanceWidgetContent(WidgetSamples.model(look)) })
        }
        board.draw().saveTo(out, "widget-glance-mirrored")
    }

    @Test
    @Config(qualifiers = "it-rIT-w411dp-h891dp-xhdpi")
    fun `in Italian, side by side`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf(Grants.OneByOne to Grants.OneByOne, Grants.TwoByOne to Grants.TwoByOne).forEach { (a, b) ->
            board.row(
                a to renderCard(context, a) { GlanceWidgetContent(WidgetSamples.model()) },
                b to renderCard(context, b) { WordsWidgetContent(WidgetSamples.model()) },
            )
        }
        listOf(Grants.ThreeByOne, Grants.FourByOne, Grants.FourByTwo, Grants.FourByThree).forEach { size ->
            board.row(size to renderCard(context, size) { GlanceWidgetContent(WidgetSamples.model()) })
            board.row(size to renderCard(context, size) { WordsWidgetContent(WidgetSamples.model()) })
        }
        board.row(
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { GlanceWidgetContent(WidgetSamples.model()) },
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsWidgetContent(WidgetSamples.model()) },
        )
        board.draw().saveTo(out, "widget-italian")
    }

    @Test
    fun `states that are not counting`() {
        val board = HomeBoard(context, widthDp = 380)
        listOf(
            CountingState.PAUSED,
            CountingState.STOPPED,
            CountingState.PERMISSION_NEEDED,
            CountingState.NOT_SET_UP,
        ).forEach { state ->
            val model = WidgetSamples.model(state = state).let { if (state.hasCount) it else it.copy(day = null) }
            board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { GlanceWidgetContent(model) })
            board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { WordsWidgetContent(model) })
        }
        board.row(
            Grants.FourByOne to renderCard(context, Grants.FourByOne) {
                GlanceWidgetContent(WidgetModel.unavailable())
            },
        )
        val paused = WidgetSamples.model(state = CountingState.PAUSED)
        board.row(
            Grants.OneByOne to renderCard(context, Grants.OneByOne) { GlanceWidgetContent(paused) },
            Grants.TwoByOne to renderCard(context, Grants.TwoByOne) { GlanceWidgetContent(paused) },
        )
        board.row(
            Grants.OneByOne to renderCard(context, Grants.OneByOne) { WordsWidgetContent(paused) },
            Grants.TwoByOne to renderCard(context, Grants.TwoByOne) { WordsWidgetContent(paused) },
        )
        board.row(
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { GlanceWidgetContent(paused) },
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsWidgetContent(paused) },
        )
        board.row(Grants.FourByTwo to renderCard(context, Grants.FourByTwo) { GlanceWidgetContent(paused) })
        board.draw().saveTo(out, "widget-states")
    }

    @Test
    fun `an outing under way takes the sentence's place`() {
        val outing = Session(
            planId = 1,
            name = null,
            goalKind = SessionGoalKind.TIME,
            goalValue = 20,
            intensity = SessionIntensity.BRISK,
            milestones = setOf(SessionMilestone.HALF),
            vibrate = true,
            localEpochDay = 0,
            startedAtMillis = 0,
            totals = SessionTotals(steps = 1_240, movingMillis = 12 * 60_000L),
        )
        val walking = WidgetSamples.model().copy(session = outing)
        val paused = walking.copy(session = outing.copy(state = SessionState.PAUSED))
        val board = HomeBoard(context, widthDp = 380)
        listOf(walking, paused).forEach { model ->
            board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { GlanceWidgetContent(model) })
            board.row(Grants.FourByOne to renderCard(context, Grants.FourByOne) { WordsWidgetContent(model) })
        }
        board.row(
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { GlanceWidgetContent(walking) },
            Grants.TwoByTwo to renderCard(context, Grants.TwoByTwo) { WordsWidgetContent(walking) },
        )
        board.row(Grants.FourByTwo to renderCard(context, Grants.FourByTwo) { GlanceWidgetContent(walking) })
        board.draw().saveTo(out, "widget-outing")
    }
}
