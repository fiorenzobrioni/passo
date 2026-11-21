package com.callbackdev.passo.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.unit.dp
import com.callbackdev.passo.core.designsystem.theme.GroupShape
import com.callbackdev.passo.core.designsystem.theme.PassoTheme
import com.callbackdev.passo.core.domain.routes.RouteShape
import com.callbackdev.passo.core.domain.routes.RouteTrace

/**
 * A route drawn still (PLANNING.md §11 Phase 21, step 21.3): as [RouteMapView] draws it, on plain
 * ground, its line in the goal's colour on an edge of the page's, dashed where the steps laid it,
 * a ring where it began and a full point where it ends, or stands while the outing counts; but
 * with no gesture, no bead, no scale and no compass, small enough for a card or a list. One
 * component, for an outing's card under way and for the lists of outings to come.
 *
 * Thinned to the pixels it is drawn in ([RouteTrace]), once for each size and each route: a
 * twelve-hour walk draws with no more points than its line has pixels to cross, so it costs no
 * more a frame than a short one.
 *
 * A small one, a list's (Phase 23.2), draws its line thinner, so the route stays a line and not a
 * ribbon: below [FULL_SIZE] across, every stroke and point narrows with the box, down to
 * [MIN_SCALE] of a card's.
 *
 * @param ratio the box's width over its height; the route's own shape, held to one that sits on
 *   a page, when null.
 * @param onClick what a touch opens (on a card, the outing's page), said to TalkBack as
 *   [clickLabel]; null for a picture alone.
 * @param clip the box's corners: a card's group shape, or a list's smaller one.
 */
@Composable
fun StillRouteView(
    shape: RouteShape,
    contentDescription: String,
    modifier: Modifier = Modifier,
    ratio: Float? = null,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    clip: Shape = GroupShape,
) {
    val scheme = MaterialTheme.colorScheme
    val land = scheme.surfaceContainerHigh
    val halo = scheme.surface
    val line = PassoTheme.colors.goal
    Spacer(
        modifier
            .aspectRatio(ratio ?: routeMapRatio(shape))
            .clip(clip)
            .testTag(StillRouteTags.ROUTE)
            // One node: the route's sentence, and the door it is on a card.
            .clearAndSetSemantics {
                this.contentDescription = contentDescription
                if (onClick != null) {
                    role = Role.Button
                    onClick(label = clickLabel) {
                        onClick()
                        true
                    }
                }
            }
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawWithCache {
                val trace = RouteTrace.of(
                    shape,
                    size.width,
                    size.height,
                    inset = INSET.toPx().coerceAtMost(size.minDimension / 6f),
                    step = THIN_STEP,
                )
                val paths = trace.lines.map { traced ->
                    Path().apply {
                        moveTo(traced.xy[0], traced.xy[1])
                        for (index in 1 until traced.size) lineTo(traced.xy[index * 2], traced.xy[index * 2 + 1])
                    } to traced.laid
                }
                val start = Offset(trace.start.first, trace.start.second)
                val end = Offset(trace.end.first, trace.end.second)
                val scale = (size.minDimension / FULL_SIZE.toPx()).coerceIn(MIN_SCALE, 1f)
                fun px(dp: Float) = dp.dp.toPx() * scale
                fun stroke(width: Float, dashed: Boolean = false) = Stroke(
                    width = width,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                    pathEffect = if (dashed) {
                        PathEffect.dashPathEffect(floatArrayOf(px(DASH), px(DASH_GAP)))
                    } else {
                        null
                    },
                )
                val casing = stroke(px(CASING))
                val measured = stroke(px(LINE))
                val laid = stroke(px(LAID_LINE), dashed = true)
                onDrawBehind {
                    drawRect(land)
                    paths.forEach { (path, _) -> drawPath(path, halo, style = casing) }
                    paths.forEach { (path, isLaid) -> drawPath(path, line, style = if (isLaid) laid else measured) }
                    // Where it began, a ring; where it ends, or stands, a full point.
                    val radius = px(END)
                    drawCircle(halo, radius, start)
                    drawCircle(line, radius, start, style = Stroke(px(RING)))
                    drawCircle(halo, radius + px(EDGE), end)
                    drawCircle(line, radius, end)
                }
            },
    )
}

private const val LINE = 3.5f
private const val LAID_LINE = 2.5f
private const val CASING = 7f
private const val DASH = 4f
private const val DASH_GAP = 5f
private const val END = 4.5f
private const val RING = 2f
private const val EDGE = 2f
private val INSET = 20.dp

/** A box at least this across draws a card's strokes; a smaller one narrows them with it. */
private val FULL_SIZE = 120.dp

/** How thin a list's small route goes: a line still, with its ring and its point. */
private const val MIN_SCALE = 0.55f

/** A point within a pixel of the last one kept adds nothing a screen can show. */
private const val THIN_STEP = 1f

/** Hooks for the UI tests. */
object StillRouteTags {
    const val ROUTE = "still_route"
}
