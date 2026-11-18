package com.callbackdev.passo.feature.sessions

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.callbackdev.passo.core.designsystem.components.MapViewport
import com.callbackdev.passo.core.designsystem.components.RouteBead
import com.callbackdev.passo.core.designsystem.components.RouteMapTags
import com.callbackdev.passo.core.designsystem.components.RouteMapView
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.routes.Splits
import com.callbackdev.passo.core.model.AppPalette
import com.callbackdev.passo.core.model.UnitSystem
import com.callbackdev.passo.core.testing.assertAccessible
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * An outing's route on its map, drawn in every theme and dress, on a city Passo carries and on
 * the plain ground (PLANNING.md §11 Phase 16, «RouteMapView»), and the map as it moves.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class RouteMapGalleryTest {
    @get:Rule val compose = createComposeRule()

    private fun show(
        route: DrawnRoute,
        dark: Boolean = false,
        palette: AppPalette = AppPalette.VIVID,
        units: UnitSystem = UnitSystem.METRIC,
        viewport: MapViewport? = null,
    ) {
        val splits = Splits.of(OutingSamples.legs, Splits.unitMeters(units)).filter { it.whole }
        compose.setContent {
            PassoTheme(darkTheme = dark, palette = palette) {
                Surface {
                    Box(Modifier.padding(16.dp)) {
                        RouteMapView(
                            shape = route.shape,
                            ground = route.ground,
                            beads = splits.map {
                                RouteBead(route.shape.at(it.endedAtMillis), "${it.index + 1} km · 11:07 min/km")
                            },
                            units = units,
                            contentDescription = "Your route: 4.26 km, from south-east to north-west, with no stop.",
                            modifier = Modifier.fillMaxWidth(),
                            viewport = viewport ?: remember { MapViewport() },
                        )
                    }
                }
            }
        }
    }

    /** The map in each dress and each theme, one after the other on one page. */
    private fun gallery(route: DrawnRoute, name: String) {
        var dark by mutableStateOf(false)
        var palette by mutableStateOf(AppPalette.entries.first())
        compose.setContent {
            PassoTheme(darkTheme = dark, palette = palette) {
                Surface {
                    Box(Modifier.padding(16.dp)) {
                        RouteMapView(
                            shape = route.shape,
                            ground = route.ground,
                            beads = emptyList(),
                            units = UnitSystem.METRIC,
                            contentDescription = "Your route",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        for (dress in AppPalette.entries) {
            for (night in listOf(false, true)) {
                palette = dress
                dark = night
                compose.onNodeWithTag(RouteMapTags.MAP).assertIsDisplayed()
                snapshot("${name}_${dress.name.lowercase()}_${if (night) "dark" else "light"}")
            }
        }
    }

    @Test
    fun `in a city Passo carries, in every theme and dress`() {
        gallery(OutingSamples.route(), "route_map_city")
    }

    @Test
    fun `on the plain ground, in every theme and dress`() {
        gallery(OutingSamples.route(north = 1.2), "route_map_plain")
    }

    @Test
    fun `it is one sentence to TalkBack`() {
        show(OutingSamples.route())

        compose.onNodeWithTag(RouteMapTags.MAP)
            .assertContentDescriptionEquals("Your route: 4.26 km, from south-east to north-west, with no stop.")
    }

    @Test
    fun `the scale bar is in the reader's units`() {
        show(OutingSamples.route(), units = UnitSystem.IMPERIAL)
        snapshot("route_map_miles")
    }

    @Test
    fun `two fingers zoom it, and a button brings the whole route back`() {
        val viewport = MapViewport()
        show(OutingSamples.route(), viewport = viewport)
        // Whole, there is nothing to go back to.
        compose.onNodeWithTag(RouteMapTags.WHOLE).assertDoesNotExist()

        compose.onNodeWithTag(RouteMapTags.MAP).performTouchInput {
            pinch(
                start0 = center - Offset(40f, 0f),
                end0 = center - Offset(240f, 0f),
                start1 = center + Offset(40f, 0f),
                end1 = center + Offset(240f, 0f),
            )
        }
        compose.waitForIdle()

        assertThat(viewport.zoomed).isTrue()
        snapshot("route_map_zoomed")
        compose.onNodeWithTag(RouteMapTags.WHOLE).assertIsDisplayed().performClick()
        compose.waitForIdle()
        assertThat(viewport.zoomed).isFalse()
    }

    @Test
    fun `a touch on a kilometre's bead names it with its pace`() {
        val route = OutingSamples.route()
        show(route)

        // The second kilometre's bead, wherever the map put it: found as the map finds it.
        compose.onNodeWithTag(RouteMapTags.MAP).performTouchInput {
            val split = Splits.of(OutingSamples.legs, 1_000.0)[1]
            val at = route.shape.at(split.endedAtMillis)
            val frame = route.shape.frame
            // The same fit the map makes: the frame inside the box, with its inset, centred.
            val inset = minOf(24.dp.toPx(), minOf(width, height) / 8f)
            val xScale = kotlin.math.cos(Math.toRadians(frame.middleLatitude))
            val scale = minOf(
                (width - 2 * inset) / ((frame.east - frame.west) * xScale),
                (height - 2 * inset) / (frame.north - frame.south),
            )
            val x =
                (width - (frame.east - frame.west) * xScale * scale) / 2 + (at.longitude - frame.west) * xScale * scale
            val y = (height - (frame.north - frame.south) * scale) / 2 + (frame.north - at.latitude) * scale
            click(Offset(x.toFloat(), y.toFloat()))
        }
        // A tap is a tap once no second one followed it.
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()

        compose.onNodeWithTag(RouteMapTags.MAP)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "2 km · 11:07 min/km"))
        snapshot("route_map_bead_named")
    }

    @Test
    fun `a viewport is never dragged past the map's own edges`() {
        val viewport = MapViewport()
        val size = Size(1_000f, 800f)

        viewport.zoomBy(2f, Offset(500f, 400f), size)
        assertThat(viewport.scale).isEqualTo(2f)
        // The point under the fingers stays under them.
        assertThat(viewport.offset).isEqualTo(Offset(-500f, -400f))
        viewport.panBy(Offset(5_000f, 5_000f), size)
        assertThat(viewport.offset).isEqualTo(Offset.Zero)
        viewport.panBy(Offset(-5_000f, -5_000f), size)
        assertThat(viewport.offset).isEqualTo(Offset(-1_000f, -800f))
        // No closer than eight times, no farther than whole.
        viewport.zoomBy(100f, Offset.Zero, size)
        assertThat(viewport.scale).isEqualTo(MapViewport.MAX_SCALE)
        viewport.zoomBy(0.001f, Offset.Zero, size)
        assertThat(viewport.scale).isEqualTo(1f)
        assertThat(viewport.offset).isEqualTo(Offset.Zero)
    }

    @Test
    fun `a double tap goes closer, and from far in back to whole`() {
        val viewport = MapViewport()
        val size = Size(1_000f, 800f)

        val (closer, _) = viewport.doubleTapTarget(Offset(250f, 200f))
        assertThat(closer).isEqualTo(2f)
        viewport.set(4f, Offset(-1_000f, -800f), size)
        assertThat(viewport.doubleTapTarget(Offset(250f, 200f))).isEqualTo(1f to Offset.Zero)
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        compose.assertAccessible()
        runCatching {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val dir = File("build/screenshots").apply { mkdirs() }
            File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
}
