package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.ways.ContinentMap
import com.callbackdev.passo.core.domain.ways.GeoBox
import com.callbackdev.passo.core.domain.ways.WayProjection
import kotlin.math.hypot

/** Where a city's walk stands, as a continent's map marks it; drawn in this order, the last on top. */
enum class CityMark {
    NOT_BEGUN,
    WALKED,
    UNDER_WAY,
}

/** A city on a continent's map: where its walk begins, its name, and where the walk stands. */
@Immutable
data class ContinentCity(val latitude: Double, val longitude: Double, val name: String, val mark: CityMark)

/**
 * The map of a continent the cities are grouped by (docs/adr/0015-city-walks.md, decision 12),
 * drawn in code like a way's: the land cut out of the sea, and each city a point. A ring before
 * its walk is begun, a bead in the goal's colour once walked to its end, the reader's own point
 * with its halo while it is under way.
 *
 * [detailed] names the cities wherever a name finds room (the ones under way first, then the
 * ones walked, then the rest, so a crowded corner keeps the names that matter to the reader),
 * and a touch names the nearest one, over the others; a thumbnail keeps only the points. The map
 * is one image to TalkBack, said by [contentDescription]: the page lists the cities in words.
 *
 * @param ratio the box's width over height; by default the frame's own shape, held between
 *   square and wide ([continentMapRatio]), 1 for a thumbnail that sits in a row.
 */
@Composable
fun ContinentMapView(
    map: ContinentMap,
    cities: List<ContinentCity>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    detailed: Boolean = true,
    ratio: Float? = null,
) {
    val scheme = MaterialTheme.colorScheme
    val water = PassoTheme.colors.water
    val land = scheme.surfaceContainerHigh
    val ring = scheme.onSurfaceVariant
    val goal = PassoTheme.colors.goal
    val halo = scheme.surface
    val ink = scheme.onSurface
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    var touched by remember(cities) { mutableStateOf<Int?>(null) }
    val touch = if (detailed) {
        Modifier.pointerInput(map, cities) {
            detectTapGestures { tap ->
                val projection = WayProjection(map.frame, size.width.toFloat(), size.height.toFloat(), inset(size))
                val nearest = cities.indices.minByOrNull { i -> distance(projection, cities[i], tap) }
                touched = nearest?.takeIf {
                    distance(projection, cities[it], tap) <= TOUCH_RADIUS.toPx() && touched != it
                }
            }
        }
    } else {
        Modifier
    }
    Spacer(
        modifier
            .aspectRatio(ratio ?: continentMapRatio(map.frame))
            .clip(MaterialTheme.shapes.large)
            // An empty description makes it decoration: a thumbnail beside the continent's name.
            .clearAndSetSemantics { if (contentDescription.isNotEmpty()) this.contentDescription = contentDescription }
            .then(touch)
            .drawWithCache {
                val projection = WayProjection(map.frame, size.width, size.height, inset(size))
                val ground = map.land.map { it.toPath(projection, close = true) }
                val points = cities.map { Offset(projection.x(it.longitude), projection.y(it.latitude)) }
                val radius = (if (detailed) CITY_RADIUS else SMALL_CITY_RADIUS).toPx()
                val labels = if (detailed) {
                    val names = cities.map { measurer.measure(it.name, labelStyle) }
                    placeLabels(cities, points, names, touched, radius, size)
                } else {
                    emptyList()
                }
                // The reader's cities over the others, should two points touch.
                val order = cities.indices.sortedBy { cities[it].mark.ordinal }
                onDrawBehind {
                    drawRect(water)
                    ground.forEach { drawPath(it, land) }
                    for (i in order) {
                        val at = points[i]
                        when (cities[i].mark) {
                            CityMark.NOT_BEGUN -> {
                                drawCircle(land, radius, at)
                                drawCircle(ring, radius, at, style = Stroke(1.5.dp.toPx()))
                            }

                            CityMark.WALKED -> {
                                drawCircle(halo, radius + 1.dp.toPx(), at)
                                drawCircle(goal, radius, at)
                            }

                            CityMark.UNDER_WAY -> drawHere(at, detailed, goal, halo)
                        }
                    }
                    labels.forEach { (text, box) -> drawLabel(text, box, halo, ink) }
                }
            },
    )
}

/** The map box's width over height: the frame's own shape, never taller than a square. */
fun continentMapRatio(frame: GeoBox): Float = (1.0 / frame.aspect).coerceIn(1.0, MAX_RATIO).toFloat()

/** The reader's point, as a way's map draws it: the goal's colour, ringed, in its halo. */
private fun DrawScope.drawHere(at: Offset, detailed: Boolean, goal: Color, halo: Color) {
    val radius = (if (detailed) HERE_RADIUS else SMALL_HERE_RADIUS).toPx()
    drawCircle(goal.copy(alpha = HALO_ALPHA), (if (detailed) HERE_HALO else SMALL_HERE_HALO).toPx(), at)
    drawCircle(halo, radius + (if (detailed) 2.dp else 1.5.dp).toPx(), at)
    drawCircle(goal, radius, at)
}

/**
 * Each city's name where it finds room: beside its point on the right, on the left, above or
 * below, inside the map and clear of the other names and points. The touched city is placed
 * first and always shown; then the cities under way, the ones walked (which may cover another
 * city's point when nothing else is free), the rest, in their order.
 */
private fun Density.placeLabels(
    cities: List<ContinentCity>,
    points: List<Offset>,
    names: List<TextLayoutResult>,
    touched: Int?,
    radius: Float,
    size: Size,
): List<Pair<TextLayoutResult, Rect>> {
    val gap = 6.dp.toPx()
    val margin = 4.dp.toPx()
    val dots = points.map { Rect(it, radius + 2.dp.toPx()) }
    val placed = mutableListOf<Pair<TextLayoutResult, Rect>>()
    val map = Rect(margin, margin, size.width - margin, size.height - margin)
    fun inside(box: Rect) =
        box.left >= map.left && box.top >= map.top && box.right <= map.right && box.bottom <= map.bottom
    fun clear(box: Rect) = placed.none { (_, other) -> other.overlaps(box) }
    val order = listOfNotNull(touched) +
        cities.indices.filter { it != touched }.sortedByDescending { cities[it].mark.ordinal }
    for (i in order) {
        val at = points[i]
        val width = names[i].size.width + 2 * LABEL_PAD_X.toPx()
        val height = names[i].size.height + 2 * LABEL_PAD_Y.toPx()
        val candidates = listOf(
            Rect(Offset(at.x + radius + gap, at.y - height / 2), Size(width, height)),
            Rect(Offset(at.x - radius - gap - width, at.y - height / 2), Size(width, height)),
            Rect(Offset(at.x - width / 2, at.y - radius - gap - height), Size(width, height)),
            Rect(Offset(at.x - width / 2, at.y + radius + gap), Size(width, height)),
        )
        // The reader's own cities keep their names in a crowded corner, over another city's point.
        val mine = cities[i].mark != CityMark.NOT_BEGUN
        val free = candidates.firstOrNull { box -> inside(box) && clear(box) && dots.none { it.overlaps(box) } }
            ?: candidates.firstOrNull { box -> mine && inside(box) && clear(box) }
        when {
            free != null -> placed += names[i] to free

            // A touched city is named even where nothing is free: kept inside the map, over the rest.
            i == touched -> {
                val box = candidates.first()
                val x = (if (box.right <= size.width - margin) box.left else candidates[1].left)
                    .coerceIn(margin, (size.width - width - margin).coerceAtLeast(margin))
                val y = box.top.coerceIn(margin, (size.height - height - margin).coerceAtLeast(margin))
                placed += names[i] to Rect(Offset(x, y), Size(width, height))
            }
        }
    }
    // The touched name last, so it is drawn over any other.
    return placed.drop(if (touched != null) 1 else 0) + placed.take(if (touched != null) 1 else 0)
}

private fun distance(projection: WayProjection, city: ContinentCity, at: Offset): Float =
    hypot(projection.x(city.longitude) - at.x, projection.y(city.latitude) - at.y)

/** The margin around the frame: room for the points at its edges. */
private fun Density.inset(size: IntSize): Float = inset(Size(size.width.toFloat(), size.height.toFloat()))

private fun Density.inset(size: Size): Float = INSET.toPx().coerceAtMost(size.minDimension / 8f)

private const val MAX_RATIO = 1.6
private const val HALO_ALPHA = 0.22f
private val INSET = 12.dp
private val CITY_RADIUS = 5.dp
private val SMALL_CITY_RADIUS = 2.5.dp
private val HERE_RADIUS = 6.dp
private val HERE_HALO = 14.dp
private val SMALL_HERE_RADIUS = 3.dp
private val SMALL_HERE_HALO = 6.dp
private val TOUCH_RADIUS = 28.dp
