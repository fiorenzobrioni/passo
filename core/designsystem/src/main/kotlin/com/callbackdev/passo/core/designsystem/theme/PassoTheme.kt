package com.callbackdev.passo.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import com.callbackdev.passo.core.model.AppFont
import com.callbackdev.passo.core.model.AppPalette

/**
 * Passo's theme: Chiaro's design language (`docs/adr/0004-design-language.md`), so the family
 * reads as one app. The defaults are Chiaro's: the vivid dress, Google Sans, the app's own
 * colors. [dynamicColor] takes Material's roles from the wallpaper instead; the semantic
 * colors ([PassoColors]) keep following the dress, as in Chiaro, because a met goal must
 * not change color with a photo.
 */
@Composable
fun PassoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    palette: AppPalette = AppPalette.VIVID,
    font: AppFont = AppFont.GOOGLE_SANS,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val dress = paletteFor(palette)
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(context)
        dynamicColor -> dynamicLightColorScheme(context)
        else -> dress.scheme(darkTheme)
    }
    CompositionLocalProvider(
        LocalPassoColors provides dress.colors(darkTheme),
        LocalPassoType provides passoType(font),
        LocalReducedMotion provides rememberReducedMotion(context),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = passoTypography(font),
            shapes = PassoShapes,
            content = content,
        )
    }
}

/** The design system's own values, next to [MaterialTheme]'s. */
object PassoTheme {
    val colors: PassoColors
        @Composable @ReadOnlyComposable
        get() = LocalPassoColors.current

    val type: PassoType
        @Composable @ReadOnlyComposable
        get() = LocalPassoType.current
}
