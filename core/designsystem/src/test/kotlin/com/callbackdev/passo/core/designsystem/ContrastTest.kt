package com.callbackdev.passo.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import com.callbackdev.passo.core.designsystem.components.HeatLevel
import com.callbackdev.passo.core.designsystem.components.heatFill
import com.callbackdev.passo.core.designsystem.components.heatInk
import com.callbackdev.passo.core.designsystem.theme.PassoColors
import com.callbackdev.passo.core.designsystem.theme.paletteFor
import com.callbackdev.passo.core.model.AppPalette
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Every ink the app sets text in reads on every ground it stands on, in both dresses and both
 * themes (PLANNING.md §11 Phase 7, the accessibility pass): WCAG's 4.5:1 for text, the
 * Accessibility Scanner's threshold. The generated schemes are never hand-edited, so
 * this pins the pairs the screens use, and the colors Passo adds.
 */
class ContrastTest {
    private val dresses = AppPalette.entries.flatMap { palette ->
        listOf(false, true).map { dark ->
            val dress = paletteFor(palette)
            Dress("$palette ${if (dark) "dark" else "light"}", dress.scheme(dark), dress.colors(dark))
        }
    }

    private class Dress(val name: String, val scheme: ColorScheme, val colors: PassoColors)

    @Test
    fun `text reads on every ground it is set on`() {
        for (dress in dresses) {
            val s = dress.scheme
            val grounds = listOf(
                "surface" to s.surface,
                "surfaceContainerLow" to s.surfaceContainerLow,
                "surfaceContainer" to s.surfaceContainer,
                "surfaceContainerHigh" to s.surfaceContainerHigh,
                "surfaceContainerHighest" to s.surfaceContainerHighest,
            )
            for ((ground, color) in grounds) {
                expect(dress, "onSurface on $ground", s.onSurface, color)
                expect(dress, "onSurfaceVariant on $ground", s.onSurfaceVariant, color)
                expect(dress, "primary on $ground", s.primary, color)
            }
            expect(dress, "onPrimary on primary", s.onPrimary, s.primary)
            expect(dress, "onPrimaryContainer on primaryContainer", s.onPrimaryContainer, s.primaryContainer)
            expect(dress, "onSecondaryContainer on secondaryContainer", s.onSecondaryContainer, s.secondaryContainer)
            expect(dress, "onErrorContainer on errorContainer", s.onErrorContainer, s.errorContainer)
            expect(dress, "error on surface", s.error, s.surface)
            expect(dress, "inverseOnSurface on inverseSurface", s.inverseOnSurface, s.inverseSurface)
        }
    }

    @Test
    fun `a status card's body keeps its contrast through its softer ink`() {
        for (dress in dresses) {
            val s = dress.scheme
            for ((name, pair) in listOf(
                "problem" to (s.onErrorContainer to s.errorContainer),
                "choice" to (s.onSecondaryContainer to s.secondaryContainer),
                "note" to (s.onSurface to s.surfaceContainerHigh),
            )) {
                val (ink, ground) = pair
                expect(dress, "$name card body", ink.copy(alpha = STATUS_BODY_ALPHA).compositeOver(ground), ground)
            }
        }
    }

    @Test
    fun `the goal and attention inks read on the page and on the goal's container`() {
        for (dress in dresses) {
            val s = dress.scheme
            val c = dress.colors
            expect(dress, "goal on surface", c.goal, s.surface)
            expect(dress, "goal on surfaceContainerLow", c.goal, s.surfaceContainerLow)
            expect(dress, "onSurface on goalContainer", s.onSurface, c.goalContainer)
            expect(dress, "attention on surface", c.attention, s.surface)
        }
    }

    @Test
    fun `a day's number reads on every level of the calendar`() {
        for (dress in dresses) {
            // The calendar stands on its card: the level's ground over it.
            val card = dress.scheme.surfaceContainerLow
            for (level in HeatLevel.entries) {
                val fill = heatFill(level, dress.scheme, dress.colors).compositeOver(card)
                expect(dress, "day number on $level", heatInk(level, dress.scheme, dress.colors), fill)
            }
        }
    }

    private fun expect(dress: Dress, what: String, ink: Color, ground: Color) {
        val ratio = contrast(ink.compositeOver(ground), ground)
        assertWithMessage("${dress.name}: $what is ${"%.2f".format(ratio)}:1").that(ratio).isAtLeast(TEXT_CONTRAST)
    }

    private fun contrast(a: Color, b: Color): Double {
        val (light, dark) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (light + 0.05) / (dark + 0.05)
    }

    private companion object {
        const val TEXT_CONTRAST = 4.5

        /** `StatusCard`'s body ink, a touch softer than its title. */
        const val STATUS_BODY_ALPHA = 0.86f
    }
}
