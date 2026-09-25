package com.callbackdev.passo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * The colors Material has no role for, per dress and per theme, never flipped: a dark value is
 * chosen for dark. Only two families of them, both taken from Chiaro so a meaning keeps its
 * color across the two apps:
 *
 * - **goal**: a met goal is Chiaro's "pass" verdict pair (ink 7.7:1 on the light surface,
 *   10.5:1 or more on the dark one; ink on container 5.6:1 or better). Like a verdict it is
 *   never the color alone: a met goal also says so in words and with a check.
 * - **effort**: a quantity from easy to hard (cadence), one hue light to dark, monotonic in
 *   luminance: Chiaro's UV ramp, the warm one, for marks only. It never paints a figure.
 */
@Immutable
data class PassoColors(val goal: Color, val goalContainer: Color, val effortRamp: List<Color>)

internal val PaperLightColors = PassoColors(
    goal = Color(0xFF005D2D),
    goalContainer = Color(0xFFD1EDD9),
    effortRamp = listOf(Color(0xFFFDE8B0), Color(0xFFFAC66A), Color(0xFFF29A2E), Color(0xFFD9661A), Color(0xFFA8400F)),
)

internal val PaperDarkColors = PassoColors(
    goal = Color(0xFF54DC88),
    goalContainer = Color(0xFF003F23),
    effortRamp = listOf(Color(0xFF4A2A08), Color(0xFF7A4210), Color(0xFFB8621A), Color(0xFFE8872A), Color(0xFFFFB55C)),
)

internal val VividLightColors = PassoColors(
    goal = Color(0xFF005D2D),
    goalContainer = Color(0xFFBFF2CE),
    effortRamp = listOf(Color(0xFFFFE8AA), Color(0xFFFFC559), Color(0xFFF89700), Color(0xFFDE6300), Color(0xFFAC3D00)),
)

internal val VividDarkColors = PassoColors(
    goal = Color(0xFF00E079),
    goalContainer = Color(0xFF003F23),
    effortRamp = listOf(Color(0xFF4D2900), Color(0xFF7E4000), Color(0xFFBD5F00), Color(0xFFEF8300), Color(0xFFFFB55C)),
)

val LocalPassoColors = staticCompositionLocalOf { VividLightColors }
