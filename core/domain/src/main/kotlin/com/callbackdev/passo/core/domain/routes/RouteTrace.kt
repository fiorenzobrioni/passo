package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.domain.ways.WayProjection
import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import kotlin.math.hypot

/**
 * One line of a route in a box's pixels: x and y one after the other ([xy]), dashed where it was
 * laid from the steps ([laid]).
 */
class TraceLine(val xy: FloatArray, val laid: Boolean) {
    val size: Int get() = xy.size / 2
}

/**
 * A route as a still picture draws it (PLANNING.md §11 Phase 21, step 21.3, «The still route»):
 * its lines projected into a box of pixels and thinned to them, so a twelve-hour walk, some
 * 43,000 fixes, is drawn with no more points than its line has pixels to cross. Pure, and cheap
 * enough to make on the frame it is drawn in: one pass over the points.
 *
 * Thinning drops a point that lands within [RouteTrace.of]'s step of the last one kept, never the
 * first or the last of a line, so every stretch still starts and ends where it did, and a dash
 * still falls where the steps laid it.
 *
 * @property start where the route began, and [end] where it ends (or stands, while it counts), as
 *   x and y.
 */
class RouteTrace private constructor(
    val lines: List<TraceLine>,
    val start: Pair<Float, Float>,
    val end: Pair<Float, Float>,
) {
    /** How many points are drawn, all lines together. */
    val pointCount: Int get() = lines.sumOf { it.size }

    companion object {
        /**
         * [shape] in a [width] by [height] box with [inset] on every side, north up, as the route's
         * map projects it; points closer than [step] pixels to the last one kept are dropped.
         */
        fun of(shape: RouteShape, width: Float, height: Float, inset: Float, step: Float = 1f): RouteTrace {
            val projection = WayProjection(shape.frame, width, height, inset)
            val points = shape.points
            val lines = shape.lines.map { line ->
                val out = FloatArray((line.to - line.from + 1) * 2)
                var kept = 0
                var lastX = 0f
                var lastY = 0f
                for (index in line.from..line.to) {
                    val point = points[index]
                    val x = projection.x(point.longitude)
                    val y = projection.y(point.latitude)
                    val first = index == line.from
                    val final = index == line.to
                    if (first || final || hypot(x - lastX, y - lastY) >= step) {
                        out[kept * 2] = x
                        out[kept * 2 + 1] = y
                        kept++
                        lastX = x
                        lastY = y
                    }
                }
                TraceLine(out.copyOf(kept * 2), line.laid)
            }
            fun at(index: Int) = projection.x(points[index].longitude) to projection.y(points[index].latitude)
            return RouteTrace(lines, at(0), at(points.lastIndex))
        }
    }
}

/**
 * This route with only the points a picture [pixels] across can show (PLANNING.md §11 Phase 23,
 * step 23.2): what a list keeps of each route it has drawn, so that scrolling back to a row draws
 * it at once without holding every fix of a long walk. A point within a pixel of the last one kept
 * is dropped, as [RouteTrace] drops it; never one where a stretch begins or ends, or where
 * measured gives way to laid from the steps, so the lines, their breaks and their dashes are the
 * route's own. A route already that small is itself.
 */
fun RouteShape.thinnedTo(pixels: Int): RouteShape {
    val points = points
    if (points.size <= pixels) return this
    val projection = WayProjection(frame, pixels.toFloat(), pixels.toFloat(), 0f)
    val kept = ArrayList<RoutePoint>(pixels * 2)
    var lastX = 0f
    var lastY = 0f
    for (index in points.indices) {
        val point = points[index]
        val x = projection.x(point.longitude)
        val y = projection.y(point.latitude)
        val next = points.getOrNull(index + 1)
        val edge = index == 0 ||
            index == points.lastIndex ||
            point.kind == RoutePointKind.START ||
            point.kind != points[index - 1].kind ||
            (next != null && (next.kind != point.kind || next.kind == RoutePointKind.START))
        if (edge || hypot(x - lastX, y - lastY) >= 1f) {
            kept += point
            lastX = x
            lastY = y
        }
    }
    return RouteShape.of(kept) ?: this
}
