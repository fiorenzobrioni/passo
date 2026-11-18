package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.designsystem.format.format
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.theme.reducedMotion
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.routes.ScaleBar
import com.callbackdev.passo.core.domain.ways.GeoPoint
import com.callbackdev.passo.core.domain.ways.WayMap
import com.callbackdev.passo.core.domain.ways.WayProjection
import com.callbackdev.passo.core.model.UnitSystem
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * A mark on a route worth a name: where a kilometre (or a mile) ended, said with its pace
 * («3 km · 9:40 min/km») when it is touched.
 */
class RouteBead(val at: GeoPoint, val label: String)

/**
 * An outing's route, drawn in code like every map of Passo (PLANNING.md §11 Phase 16;
 * docs/adr/0016-location.md, decision 11): no tiles, since there is no network to fetch one
 * with. North is up. The line is in the goal's colour, dashed where it was laid from the steps
 * (a tunnel, an arcade: the honest mark), with a ring where it began, a full point where it
 * ended and a small bead at each kilometre; a scale bar in the reader's units says how large it
 * all is.
 *
 * Where the route lies inside a city Passo carries, that city's map is the [ground] (its water,
 * parks and streets, as on the walk's page); elsewhere the ground is plain, and the page says
 * so in a line.
 *
 * Passo's first map that moves ([MapViewport], [mapGestures]): two fingers zoom, one drags it
 * once it is zoomed, a double tap zooms where it lands, and a button brings the whole route
 * back. A touch on a bead names its kilometre and its pace. To TalkBack it is one sentence,
 * [contentDescription]: the splits under it are its words.
 *
 * @param still for an image to share: no gesture, no button, the whole route.
 */
@Composable
fun RouteMapView(
    shape: RouteShape,
    ground: WayMap?,
    beads: List<RouteBead>,
    units: UnitSystem,
    contentDescription: String,
    modifier: Modifier = Modifier,
    viewport: MapViewport = remember(shape) { MapViewport() },
    still: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val light = scheme.surface.luminance() > 0.5f
    val colors = RouteMapColors(
        water = PassoTheme.colors.water,
        land = scheme.surfaceContainerHigh,
        park = PassoTheme.colors.park,
        // A street is a lighter line on the land: white on the light grey, a step up in the dark.
        street = if (light) scheme.surfaceContainerLowest else scheme.surfaceBright,
        outline = scheme.outlineVariant,
        line = PassoTheme.colors.goal,
        halo = scheme.surface,
        ink = scheme.onSurface,
        quiet = scheme.onSurfaceVariant,
    )
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val scaleStyle = MaterialTheme.typography.labelSmall
    val resources = LocalResources.current
    val north = stringResource(R.string.route_map_north)
    val reduced = reducedMotion()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var touched by remember(shape) { mutableStateOf<Int?>(null) }
    var mapSize by remember { mutableStateOf(Size.Zero) }
    // The gestures outlive a recomposition: they read the route and its beads as they are now.
    val currentShape by rememberUpdatedState(shape)
    val currentBeads by rememberUpdatedState(beads)
    val gestures = if (still) {
        Modifier
    } else {
        // A tap names the bead it lands on; on the same bead again, or anywhere else, it puts
        // the name away.
        Modifier.mapGestures(viewport, scope, reduced) { tap ->
            val nearest = with(density) { nearestBead(currentShape, currentBeads, viewport, tap, mapSize) }
            touched = nearest.takeIf { it != touched }
        }
    }
    Box(
        modifier
            .aspectRatio(routeMapRatio(shape))
            .clip(GroupShape),
    ) {
        Spacer(
            Modifier
                .fillMaxSize()
                .onSizeChanged { mapSize = it.toSize() }
                .testTag(RouteMapTags.MAP)
                .clearAndSetSemantics { this.contentDescription = contentDescription }
                .then(gestures)
                .drawWithCache {
                    val projection = WayProjection(shape.frame, size.width, size.height, inset(size))
                    val land = ground?.land?.map { it.toPath(projection, close = true) }.orEmpty()
                    val parks = ground?.parks?.map { it.toPath(projection, close = true) }.orEmpty()
                    val lakes = ground?.lakes?.map { it.toPath(projection, close = true) }.orEmpty()
                    val canals = ground?.rivers?.map { it.toPath(projection, close = false) }.orEmpty()
                    val canalWidth = ((ground?.riverWidthMeters ?: 0.0).toFloat() * projection.pixelsPerMeter())
                    val streets = ground?.streets?.map { it.toPath(projection, close = false) }.orEmpty()
                    val mainStreets = ground?.mainStreets?.map { it.toPath(projection, close = false) }.orEmpty()
                    fun at(latitude: Double, longitude: Double) =
                        Offset(projection.x(longitude), projection.y(latitude))
                    val lines = shape.lines.map { line ->
                        Path().apply {
                            for (index in line.from..line.to) {
                                val p = at(shape.points[index].latitude, shape.points[index].longitude)
                                if (index == line.from) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                            }
                        } to line.laid
                    }
                    val start = at(shape.start.latitude, shape.start.longitude)
                    val end = at(shape.end.latitude, shape.end.longitude)
                    val marks = beads.map { at(it.at.latitude, it.at.longitude) }
                    val names = beads.map { measurer.measure(it.label, labelStyle) }
                    val northText = measurer.measure(north, scaleStyle)
                    onDrawBehind {
                        val zoom = viewport.scale
                        val offset = viewport.offset

                        // Lines keep their width on the screen however far the map is zoomed.
                        fun px(dp: Float) = dp.dp.toPx() / zoom
                        fun stroke(dp: Float, dashed: Boolean = false) = Stroke(
                            width = px(dp),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                            pathEffect = if (dashed) {
                                PathEffect.dashPathEffect(floatArrayOf(px(DASH), px(DASH_GAP)))
                            } else {
                                null
                            },
                        )
                        drawRect(if (ground?.sea == true) colors.water else colors.land)
                        withTransform({
                            translate(offset.x, offset.y)
                            scale(zoom, zoom, pivot = Offset.Zero)
                        }) {
                            land.forEach { drawPath(it, colors.land) }
                            parks.forEach { drawPath(it, colors.park) }
                            lakes.forEach { drawPath(it, colors.water) }
                            val side = colors.street.copy(alpha = SIDE_STREET)
                            streets.forEach { drawPath(it, side, style = stroke(STREET)) }
                            mainStreets.forEach { drawPath(it, colors.street, style = stroke(MAIN_STREET)) }
                            if (canalWidth > 0f) {
                                val width = Stroke(maxOf(canalWidth, px(STREET)), cap = StrokeCap.Round)
                                canals.forEach { drawPath(it, colors.water, style = width) }
                            }
                            // The line lies on an edge of the page's colour: over streets, water
                            // and parks alike it reads as one line, apart from the ground.
                            lines.forEach { (path, _) -> drawPath(path, colors.halo, style = stroke(CASING)) }
                            lines.forEach { (path, laid) ->
                                drawPath(
                                    path,
                                    colors.line,
                                    style = stroke(if (laid) LAID_LINE else LINE, dashed = laid),
                                )
                            }
                            marks.forEachIndexed { index, mark ->
                                val radius = px(if (index == touched) TOUCHED_BEAD else BEAD)
                                drawCircle(colors.halo, radius, mark)
                                drawCircle(colors.line, radius, mark, style = Stroke(px(1.5f)))
                            }
                            // Where it began, a ring; where it ended, a full point.
                            drawCircle(colors.halo, px(END), start)
                            drawCircle(colors.line, px(END), start, style = Stroke(px(2f)))
                            drawCircle(colors.halo, px(END + 2f), end)
                            drawCircle(colors.line, px(END), end)
                        }
                        // Over the map, at the size of the screen: the touched bead's name, north
                        // and the scale.
                        touched?.let { index ->
                            val on = marks[index] * zoom + offset
                            val text = names[index]
                            val pill = Size(
                                text.size.width + 2 * LABEL_PAD_X.toPx(),
                                text.size.height + 2 * LABEL_PAD_Y.toPx(),
                            )
                            val box = placeLabels(listOf(pill to on), size, emptyList()).single()
                            drawLabel(text, box, colors.halo, colors.ink, colors.outline)
                        }
                        val corner = EDGE.dp.toPx()
                        val compass = Offset(size.width - corner - NORTH.dp.toPx(), corner + NORTH.dp.toPx())
                        drawCircle(colors.halo.copy(alpha = PILL), NORTH.dp.toPx(), compass)
                        drawCircle(colors.outline, NORTH.dp.toPx(), compass, style = Stroke(1.dp.toPx()))
                        val tip = NORTH.dp.toPx() * 0.72f
                        drawPath(
                            Path().apply {
                                moveTo(compass.x, compass.y - tip)
                                lineTo(compass.x - tip * 0.42f, compass.y - tip * 0.18f)
                                lineTo(compass.x + tip * 0.42f, compass.y - tip * 0.18f)
                                close()
                            },
                            colors.ink,
                        )
                        drawText(
                            northText,
                            colors.ink,
                            Offset(compass.x - northText.size.width / 2f, compass.y - tip * 0.12f),
                        )
                        val metersPerPixel = 1f / (projection.pixelsPerMeter() * zoom)
                        ScaleBar.of((size.width * SCALE_SHARE * metersPerPixel).toDouble(), units)?.let { bar ->
                            val length = (bar.meters / metersPerPixel).toFloat()
                            val text = measurer.measure(resources.format(bar.label), scaleStyle)
                            val left = corner
                            val bottom = size.height - corner
                            val tick = 4.dp.toPx()
                            val plate = Size(
                                maxOf(length, text.size.width.toFloat()) + 2 * LABEL_PAD_X.toPx(),
                                text.size.height + tick + 3 * LABEL_PAD_Y.toPx(),
                            )
                            val plateTop = Offset(left, bottom - plate.height)
                            drawRoundRect(
                                colors.halo.copy(alpha = PILL),
                                plateTop,
                                plate,
                                androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                            )
                            val x = left + LABEL_PAD_X.toPx()
                            val y = bottom - LABEL_PAD_Y.toPx() - 1.dp.toPx()
                            val rule = Path().apply {
                                moveTo(x, y - tick)
                                lineTo(x, y)
                                lineTo(x + length, y)
                                lineTo(x + length, y - tick)
                            }
                            drawPath(rule, colors.ink, style = Stroke(1.5.dp.toPx(), join = StrokeJoin.Round))
                            drawText(text, colors.quiet, Offset(x, plateTop.y + LABEL_PAD_Y.toPx()))
                        }
                    }
                },
        )
        if (!still && viewport.zoomed) {
            FilledTonalButton(
                onClick = {
                    touched = null
                    if (reduced) {
                        viewport.reset()
                    } else {
                        scope.launch { viewport.glideTo(1f, Offset.Zero, mapSize) }
                    }
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .heightIn(min = 48.dp)
                    .testTag(RouteMapTags.WHOLE),
            ) {
                Text(stringResource(R.string.route_map_whole))
            }
        }
    }
}

/** The bead nearest [tap] on the screen, if it is within a finger's reach of it. */
private fun Density.nearestBead(
    shape: RouteShape,
    beads: List<RouteBead>,
    viewport: MapViewport,
    tap: Offset,
    size: Size,
): Int? {
    if (beads.isEmpty() || size == Size.Zero) return null
    val projection = WayProjection(shape.frame, size.width, size.height, inset(size))
    val (index, far) = beads.indices
        .map { index ->
            val at = beads[index].at
            val on = Offset(projection.x(at.longitude), projection.y(at.latitude)) * viewport.scale + viewport.offset
            index to hypot(on.x - tap.x, on.y - tap.y)
        }
        .minBy { it.second }
    return index.takeIf { far <= TOUCH.dp.toPx() }
}

/**
 * The map box's width over height: the route's own shape, held to one that sits on a page (a
 * long, thin walk along a river still gets a box that reads).
 */
fun routeMapRatio(shape: RouteShape): Float =
    (1.0 / WayProjection.boxAspect(shape.frame, MIN_ASPECT, MAX_ASPECT)).toFloat()

private fun Density.inset(size: Size): Float = INSET.dp.toPx().coerceAtMost(size.minDimension / 8f)

private class RouteMapColors(
    val water: Color,
    val land: Color,
    val park: Color,
    val street: Color,
    val outline: Color,
    val line: Color,
    val halo: Color,
    val ink: Color,
    val quiet: Color,
)

private const val MIN_ASPECT = 0.7
private const val MAX_ASPECT = 1.1
private const val INSET = 24f
private const val EDGE = 10f
private const val NORTH = 13f
private const val LINE = 4f
private const val LAID_LINE = 3f
private const val CASING = 8f
private const val DASH = 5f
private const val DASH_GAP = 6f
private const val BEAD = 3.2f
private const val TOUCHED_BEAD = 5f
private const val END = 5.5f
private const val STREET = 1f
private const val MAIN_STREET = 2.5f
private const val SIDE_STREET = 0.6f
private const val PILL = 0.92f

/** How near a bead a tap must land to name it. */
private const val TOUCH = 28f

/** The scale bar is at most this share of the map's width. */
private const val SCALE_SHARE = 0.3f

/** Hooks for the UI tests. */
object RouteMapTags {
    const val MAP = "route_map"
    const val WHOLE = "route_map_whole"
}
