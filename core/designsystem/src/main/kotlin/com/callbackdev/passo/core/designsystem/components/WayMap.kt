package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.designsystem.ways.placeNameRes
import com.callbackdev.passo.core.domain.ways.GeoBox
import com.callbackdev.passo.core.domain.ways.GeoPath
import com.callbackdev.passo.core.domain.ways.Way
import com.callbackdev.passo.core.domain.ways.WayLine
import com.callbackdev.passo.core.domain.ways.WayProjection
import com.callbackdev.passo.core.model.WayKind
import kotlin.math.hypot

/**
 * The map of a way or a city walk (PLANNING.md §11 Phase 11), drawn in code like every chart of
 * Passo: the ground (land, lakes, rivers, borders; a city's parks, water and canals), the whole
 * way faint, the part walked in the goal's
 * colour up to the reader's point, and the stops, filled once reached. It knows nothing of where
 * the reader is: their point is their distance along the way.
 *
 * [detailed] adds the start's and the end's names, the locator (the country, with the way on
 * it, in the corner the line leaves free) and a touch that names the nearest stop; the small
 * card in Insights leaves them out. The map is one image to TalkBack, said by
 * [contentDescription]: the page lists the stops in words beside it.
 *
 * @param walkedMeters null for a way not started: the whole line plain, no point.
 * @param reached how many of the way's stops, from the first, are reached.
 * @param ratio the box's width over height; by default the frame's own shape ([wayMapRatio]),
 *   1 for a thumbnail that sits in a row.
 */
@Composable
fun WayMapView(
    way: Way,
    walkedMeters: Double?,
    reached: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    detailed: Boolean = true,
    ratio: Float? = null,
) {
    val map = way.map
    val scheme = MaterialTheme.colorScheme
    val colors = WayMapColors(
        water = PassoTheme.colors.water,
        land = scheme.surfaceContainerHigh,
        park = PassoTheme.colors.park,
        // A street is a lighter line on the land: white on the light grey, a step up in the dark.
        street = if (scheme.surface.luminance() > 0.5f) scheme.surfaceContainerLowest else scheme.surfaceBright,
        river = lerp(PassoTheme.colors.water, scheme.onSurfaceVariant, RIVER_INK),
        border = scheme.outline.copy(alpha = BORDER_ALPHA),
        way = scheme.onSurfaceVariant.copy(alpha = WAY_ALPHA),
        walked = PassoTheme.colors.goal,
        stopRing = scheme.onSurfaceVariant,
        halo = scheme.surface,
        ink = scheme.onSurface,
    )
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    val resources = LocalResources.current
    val names = remember(way.id, resources) { way.stops.map { resources.getString(placeNameRes(it.key)) } }
    var touched by remember(way.id) { mutableStateOf<Int?>(null) }
    val touch = if (detailed) {
        Modifier.pointerInput(way.id) {
            detectTapGestures { tap ->
                val projection = WayProjection(map.frame, size.width.toFloat(), size.height.toFloat(), inset(size))
                val nearest = way.stops.indices.minBy { i ->
                    val p = map.line.pointAt(way.stops[i].distanceMeters.toDouble())
                    hypot(projection.x(p.longitude) - tap.x, projection.y(p.latitude) - tap.y)
                }
                val p = map.line.pointAt(way.stops[nearest].distanceMeters.toDouble())
                val far = hypot(projection.x(p.longitude) - tap.x, projection.y(p.latitude) - tap.y)
                touched = if (far <= TOUCH_RADIUS.toPx() && touched != nearest) nearest else null
            }
        }
    } else {
        Modifier
    }
    Spacer(
        modifier
            .aspectRatio(ratio ?: wayMapRatio(map.frame))
            .clip(MaterialTheme.shapes.large)
            // An empty description makes it decoration: a thumbnail beside the way's own name.
            .clearAndSetSemantics { if (contentDescription.isNotEmpty()) this.contentDescription = contentDescription }
            .then(touch)
            .drawWithCache {
                val projection = WayProjection(map.frame, size.width, size.height, inset(size))
                val land = map.land.map { it.toPath(projection, close = true) }
                val parks = map.parks.map { it.toPath(projection, close = true) }
                val lakes = map.lakes.map { it.toPath(projection, close = true) }
                // A country's rivers are hairlines; a city's canals as wide as they are.
                val riverWidth = (map.riverWidthMeters.toFloat() * projection.pixelsPerMeter())
                    .coerceAtLeast(RIVER_WIDTH.toPx())
                val riverInk = if (map.riverWidthMeters > 0) colors.water else colors.river
                val rivers = map.rivers.map { it.toPath(projection, close = false) }
                // Streets only on the walk's own page: on a thumbnail they would be noise.
                val mainStreets = if (detailed) {
                    map.mainStreets.map {
                        it.toPath(projection, close = false)
                    }
                } else {
                    emptyList()
                }
                val streets = if (detailed) map.streets.map { it.toPath(projection, close = false) } else emptyList()
                val borders = map.borders.map { it.toPath(projection, close = false) }
                val whole = map.line.toPath(projection, upTo = null)
                val walked = walkedMeters?.let { map.line.toPath(projection, upTo = it) }
                val stops = way.stops.map {
                    val p = map.line.pointAt(it.distanceMeters.toDouble())
                    Offset(projection.x(p.longitude), projection.y(p.latitude))
                }
                val here = walkedMeters?.let {
                    val p = map.line.pointAt(it)
                    Offset(projection.x(p.longitude), projection.y(p.latitude))
                }
                val labels = if (detailed) {
                    listOfNotNull(
                        names.first() to stops.first(),
                        names.last() to stops.last(),
                        touched?.takeIf { it != 0 && it != stops.lastIndex }?.let { names[it] to stops[it] },
                    ).map { (text, at) -> Label(measurer.measure(text, labelStyle), at) }
                } else {
                    emptyList()
                }
                // A city's walk fills its frame: no corner is free for the country, and the page
                // names the city anyway. The ends' names are kept clear of it (a touched stop's
                // is not, or the locator would jump at every touch).
                val locator = if (detailed && way.kind == WayKind.WAY) {
                    locatorBox(size, stops.first(), whole, map.locatorFrame, labels.take(2).map { labelRect(it, size) })
                } else {
                    null
                }
                onDrawBehind {
                    // A country's map is cut out of the sea, and so is a city's on the coast;
                    // another city has no sea around it.
                    drawRect(if (way.kind == WayKind.WALK && !map.sea) colors.land else colors.water)
                    land.forEach { drawPath(it, colors.land) }
                    parks.forEach { drawPath(it, colors.park) }
                    lakes.forEach { drawPath(it, colors.water) }
                    // Over a river the streets are its bridges; a canal, drawn as a line, stays on
                    // top of the streets along its banks.
                    streets.forEach { drawPath(it, colors.street, style = line(STREET_WIDTH)) }
                    mainStreets.forEach { drawPath(it, colors.street, style = line(MAIN_STREET_WIDTH)) }
                    rivers.forEach { drawPath(it, riverInk, style = line(riverWidth)) }
                    val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))
                    borders.forEach { drawPath(it, colors.border, style = Stroke(1.dp.toPx(), pathEffect = dash)) }
                    // Not started, the line is the way itself, at full ink; under way, it steps back
                    // for the part walked.
                    drawPath(whole, if (walkedMeters == null) colors.stopRing else colors.way, style = line(WAY_WIDTH))
                    walked?.let { drawPath(it, colors.walked, style = line(WALKED_WIDTH)) }
                    stops.forEachIndexed { i, at ->
                        val stop = way.stops[i]
                        val end = i == 0 || i == stops.lastIndex
                        val done = walkedMeters != null && i < reached
                        when {
                            // A small map keeps only its line and its ends: dots would crowd it.
                            !detailed && !end -> Unit

                            // The two ends always show, as rings, filled once reached.
                            end -> {
                                val radius = (if (detailed) END_RADIUS else SMALL_END_RADIUS).toPx()
                                drawCircle(if (done) colors.walked else colors.land, radius, at)
                                drawCircle(
                                    if (done) colors.walked else colors.stopRing,
                                    radius,
                                    at,
                                    style = Stroke(1.5.dp.toPx()),
                                )
                            }

                            // A stage reached is a bead on the walked line; one ahead, a ring.
                            done -> if (stop.stage) drawCircle(colors.land, BEAD_RADIUS.toPx(), at)

                            else -> {
                                val radius = (if (stop.stage) STAGE_RADIUS else PLACE_RADIUS).toPx()
                                drawCircle(colors.land, radius, at)
                                drawCircle(colors.stopRing, radius, at, style = Stroke(1.dp.toPx()))
                            }
                        }
                    }
                    here?.let {
                        // A thumbnail's point is smaller: at full size it would cover a city.
                        val radius = (if (detailed) HERE_RADIUS else SMALL_HERE_RADIUS).toPx()
                        val halo = (if (detailed) HERE_HALO else SMALL_HERE_HALO).toPx()
                        drawCircle(colors.walked.copy(alpha = HALO_ALPHA), halo, it)
                        drawCircle(colors.halo, radius + (if (detailed) 2.dp else 1.5.dp).toPx(), it)
                        drawCircle(colors.walked, radius, it)
                    }
                    labels.forEach { drawLabel(it, colors) }
                    locator?.let { drawLocator(it, map.locatorLand, map.locatorLine, map.locatorFrame, colors) }
                }
            },
    )
}

/** The map box's width over height: the frame's own shape, held to one that sits on a page. */
fun wayMapRatio(frame: GeoBox): Float = (1.0 / WayProjection.boxAspect(frame, MIN_ASPECT, MAX_ASPECT)).toFloat()

private class WayMapColors(
    val water: Color,
    val land: Color,
    val park: Color,
    val street: Color,
    val river: Color,
    val border: Color,
    val way: Color,
    val walked: Color,
    val stopRing: Color,
    val halo: Color,
    val ink: Color,
)

private class Label(val text: TextLayoutResult, val at: Offset)

/** The margin around the way's frame: some room for the end points' names and dots. */
private fun Density.inset(size: IntSize): Float = inset(Size(size.width.toFloat(), size.height.toFloat()))

private fun Density.inset(size: Size): Float = INSET.toPx().coerceAtMost(size.minDimension / 8f)

private fun DrawScope.line(width: Dp) = line(width.toPx())

private fun line(width: Float) = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round)

internal fun GeoPath.toPath(projection: WayProjection, close: Boolean): Path = Path().also { path ->
    for (i in 0 until size) {
        val x = projection.x(longitudes[i])
        val y = projection.y(latitudes[i])
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (close) path.close()
}

/** The way's line, whole or up to [upTo] metres along it. */
private fun WayLine.toPath(projection: WayProjection, upTo: Double?): Path = Path().also { path ->
    val last = upTo?.let { segmentAt(it) } ?: (size - 1)
    for (i in 0..last) {
        val x = projection.x(longitudes[i])
        val y = projection.y(latitudes[i])
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (upTo != null) {
        val end = pointAt(upTo)
        path.lineTo(projection.x(end.longitude), projection.y(end.latitude))
    }
}

/**
 * The locator's box: in the corner the way's line leaves most free, away from the start and
 * clear of the ends' names ([names], where [labelRect] puts them), a third of the map's width at
 * most.
 */
private fun Density.locatorBox(size: Size, start: Offset, whole: Path, frame: GeoBox, names: List<Rect>): Rect {
    val width = (size.width * LOCATOR_SHARE).coerceAtMost(LOCATOR_MAX.toPx())
    val height = width * frame.aspect.toFloat()
    val margin = 8.dp.toPx()
    val corners = listOf(
        Rect(Offset(size.width - width - margin, margin), Size(width, height)),
        Rect(Offset(margin, size.height - height - margin), Size(width, height)),
        Rect(Offset(margin, margin), Size(width, height)),
        Rect(Offset(size.width - width - margin, size.height - height - margin), Size(width, height)),
    )
    val bounds = whole.getBounds()
    return corners.minBy { corner ->
        val overlap = corner.intersect(bounds).let { if (it.isEmpty) 0f else it.width * it.height }
        val covered = corner.inflate(LABEL_ROOM.toPx()).contains(start) || names.any { it.overlaps(corner) }
        overlap + if (covered) size.width * size.height else 0f
    }
}

private fun DrawScope.drawLocator(box: Rect, land: List<GeoPath>, line: GeoPath, frame: GeoBox, colors: WayMapColors) {
    val radius = CornerRadius(8.dp.toPx())
    drawRoundRect(colors.halo, box.topLeft, box.size, radius)
    drawRoundRect(colors.water, box.topLeft, box.size, radius)
    val projection = WayProjection(frame, box.width, box.height)
    translate(box.left, box.top) {
        clipRect(0f, 0f, box.width, box.height) {
            land.forEach { drawPath(it.toPath(projection, close = true), colors.land) }
            val path = line.toPath(projection, close = false)
            val bounds = path.getBounds()
            // A city's walk is a few pixels on its country: a dot says where it is.
            if (bounds.maxDimension < LOCATOR_DOT.toPx() * 2) {
                drawCircle(colors.walked, LOCATOR_DOT.toPx(), bounds.center)
            } else {
                drawPath(path, colors.walked, style = line(2.5.dp))
            }
        }
    }
    drawRoundRect(colors.border, box.topLeft, box.size, radius, style = Stroke(1.dp.toPx()))
}

/** Where a name's pill goes: beside its point, on the right if it fits, kept inside the map. */
private fun Density.labelRect(label: Label, size: Size): Rect {
    val gap = 8.dp.toPx()
    val text = label.text.size
    val width = text.width + 2 * LABEL_PAD_X.toPx()
    val height = text.height + 2 * LABEL_PAD_Y.toPx()
    val right = label.at.x + gap + width <= size.width - gap
    val x = (if (right) label.at.x + gap else label.at.x - gap - width)
        .coerceIn(gap, (size.width - width - gap).coerceAtLeast(gap))
    val y = (label.at.y - height / 2f).coerceIn(gap, (size.height - height - gap).coerceAtLeast(gap))
    return Rect(Offset(x, y), Size(width, height))
}

/** A name beside its point, on a small pill of the page's colour. */
private fun DrawScope.drawLabel(label: Label, colors: WayMapColors) =
    drawLabel(label.text, labelRect(label, size), colors.halo, colors.ink)

/** A name on a small pill of the page's colour ([halo]), in [box]. */
internal fun DrawScope.drawLabel(text: TextLayoutResult, box: Rect, halo: Color, ink: Color) {
    drawRoundRect(halo.copy(alpha = PILL_ALPHA), box.topLeft, box.size, CornerRadius(box.height / 2))
    drawText(text, ink, Offset(box.left + LABEL_PAD_X.toPx(), box.top + LABEL_PAD_Y.toPx()))
}

private const val MIN_ASPECT = 0.62
private const val MAX_ASPECT = 1.2
private const val RIVER_INK = 0.3f
private const val BORDER_ALPHA = 0.55f
private const val WAY_ALPHA = 0.4f
private const val HALO_ALPHA = 0.22f
private const val PILL_ALPHA = 0.88f
private const val LOCATOR_SHARE = 0.3f
private val LOCATOR_MAX = 120.dp
private val LOCATOR_DOT = 3.5.dp
private val INSET = 20.dp
private val RIVER_WIDTH = 1.dp
private val WAY_WIDTH = 3.dp
private val WALKED_WIDTH = 4.dp
private val END_RADIUS = 5.dp
private val SMALL_END_RADIUS = 3.5.dp
private val STAGE_RADIUS = 3.dp
private val PLACE_RADIUS = 2.dp
private val BEAD_RADIUS = 1.4.dp
private val HERE_RADIUS = 6.dp
private val HERE_HALO = 14.dp
private val SMALL_HERE_RADIUS = 3.5.dp
private val SMALL_HERE_HALO = 7.dp
private val LABEL_ROOM = 40.dp
private val STREET_WIDTH = 1.5.dp
private val MAIN_STREET_WIDTH = 2.5.dp
internal val LABEL_PAD_X = 5.dp
internal val LABEL_PAD_Y = 2.dp
private val TOUCH_RADIUS = 28.dp
