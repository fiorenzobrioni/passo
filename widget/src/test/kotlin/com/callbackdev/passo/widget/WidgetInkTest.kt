package com.callbackdev.passo.widget

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Chiaro's ink rule, carried over: `WidgetInkTest` there, the same cases here. */
class WidgetInkTest {
    @Test
    fun `a solid card is its own ground`() {
        assertThat(widgetInk(WidgetBackground.COLOR, 100, night = false, wallpaperCarriesDarkInk = true))
            .isEqualTo(WidgetInk.OVER_COLOR)
        assertThat(widgetInk(WidgetBackground.LIGHT, 100, night = true, wallpaperCarriesDarkInk = false))
            .isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(widgetInk(WidgetBackground.DARK, 100, night = false, wallpaperCarriesDarkInk = true))
            .isEqualTo(WidgetInk.ON_DARK)
    }

    @Test
    fun `the phone's card follows the phone's mode`() {
        assertThat(widgetInk(WidgetBackground.SYSTEM, 100, night = true, wallpaperCarriesDarkInk = false))
            .isEqualTo(WidgetInk.ON_DARK)
        assertThat(widgetInk(WidgetBackground.SYSTEM, 100, night = false, wallpaperCarriesDarkInk = false))
            .isEqualTo(WidgetInk.ON_LIGHT)
    }

    @Test
    fun `a see-through light or dark card keeps the ink the reader named`() {
        assertThat(widgetInk(WidgetBackground.LIGHT, 0, night = true, wallpaperCarriesDarkInk = false))
            .isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(widgetInk(WidgetBackground.DARK, 20, night = false, wallpaperCarriesDarkInk = true))
            .isEqualTo(WidgetInk.ON_DARK)
    }

    @Test
    fun `a see-through colour or phone card asks the wallpaper, and only a yes means dark ink`() {
        assertThat(widgetInk(WidgetBackground.COLOR, 20, night = false, wallpaperCarriesDarkInk = true))
            .isEqualTo(WidgetInk.ON_LIGHT)
        assertThat(widgetInk(WidgetBackground.COLOR, 20, night = false, wallpaperCarriesDarkInk = false))
            .isEqualTo(WidgetInk.ON_DARK)
        assertThat(widgetInk(WidgetBackground.SYSTEM, 0, night = false, wallpaperCarriesDarkInk = false))
            .isEqualTo(WidgetInk.ON_DARK)
    }

    @Test
    fun `the floor is where the card stops deciding`() {
        assertThat(
            widgetInk(WidgetBackground.COLOR, INK_TRUST_FLOOR_PCT, night = false, wallpaperCarriesDarkInk = true),
        )
            .isEqualTo(WidgetInk.OVER_COLOR)
        assertThat(
            widgetInk(WidgetBackground.COLOR, INK_TRUST_FLOOR_PCT - 1, night = false, wallpaperCarriesDarkInk = true),
        )
            .isEqualTo(WidgetInk.ON_LIGHT)
    }
}
