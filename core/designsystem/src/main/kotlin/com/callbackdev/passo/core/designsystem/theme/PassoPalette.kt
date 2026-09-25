package com.callbackdev.passo.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import com.callbackdev.passo.core.model.AppPalette

/** One dress: a scheme and its semantic colors, measured together and never mixed. */
@Immutable
internal data class PassoPalette(
    val lightScheme: ColorScheme,
    val darkScheme: ColorScheme,
    val lightColors: PassoColors,
    val darkColors: PassoColors,
) {
    fun scheme(dark: Boolean): ColorScheme = if (dark) darkScheme else lightScheme

    fun colors(dark: Boolean): PassoColors = if (dark) darkColors else lightColors
}

/** Warm paper and amber. */
private val Paper = PassoPalette(PaperLightScheme, PaperDarkScheme, PaperLightColors, PaperDarkColors)

/** Daylight white and azure: the default, as in Chiaro. */
private val Vivid = PassoPalette(VividLightScheme, VividDarkScheme, VividLightColors, VividDarkColors)

internal fun paletteFor(choice: AppPalette): PassoPalette = when (choice) {
    AppPalette.PAPER -> Paper
    AppPalette.VIVID -> Vivid
}
