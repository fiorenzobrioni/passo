package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

/**
 * The still route's picture (PLANNING.md §11 Phase 21, step 21.3): projected into its box,
 * thinned to its pixels, its stretches and its dashes kept, and a twelve-hour route read, shaped
 * and thinned in a moment.
 */
class RouteTraceTest {
    private fun point(second: Long, lat: Double, lon: Double, kind: RoutePointKind = RoutePointKind.MEASURED) =
        RoutePoint(second, (lat * 1e6).toInt(), (lon * 1e6).toInt(), null, 5, kind)

    /** A walk north from Milan's Duomo, a point a second about a metre and a half apart. */
    private fun walk(seconds: Int, from: Long = 0) = (0 until seconds).map {
        point(
            from + it,
            45.4642 + it * 0.0000135,
            9.1900,
            if (it == 0) RoutePointKind.START else RoutePointKind.MEASURED,
        )
    }

    @Test
    fun `a route is drawn inside its box, north up`() {
        val shape = checkNotNull(RouteShape.of(walk(600)))
        val trace = RouteTrace.of(shape, width = 400f, height = 250f, inset = 20f)
        val (startX, startY) = trace.start
        val (endX, endY) = trace.end
        assertThat(endY).isLessThan(startY)
        assertThat(endX).isWithin(0.5f).of(startX)
        trace.lines.forEach { line ->
            for (index in 0 until line.size) {
                assertThat(line.xy[index * 2]).isIn(com.google.common.collect.Range.closed(0f, 400f))
                assertThat(line.xy[index * 2 + 1]).isIn(com.google.common.collect.Range.closed(0f, 250f))
            }
        }
    }

    @Test
    fun `points closer than a pixel to the last one kept are dropped, never a line's ends`() {
        val shape = checkNotNull(RouteShape.of(walk(600)))
        // Some 900 m of walk in about 200 pixels: four or five points to a pixel.
        val trace = RouteTrace.of(shape, width = 400f, height = 250f, inset = 20f)
        val line = trace.lines.single()
        assertThat(line.size).isLessThan(300)
        assertThat(line.size).isGreaterThan(100)
        assertThat(line.xy[0]).isEqualTo(trace.start.first)
        assertThat(line.xy[1]).isEqualTo(trace.start.second)
        assertThat(line.xy[line.xy.size - 2]).isEqualTo(trace.end.first)
        assertThat(line.xy[line.xy.size - 1]).isEqualTo(trace.end.second)
    }

    @Test
    fun `a pause stays a break and a laid stretch stays dashed`() {
        val first = walk(120)
        val laid = (1..30).map { point(200L + it, 45.4642 + (120 + it) * 0.0000135, 9.1900, RoutePointKind.LAID) }
        val after = walk(120, from = 400).map { it.copy(latitudeE6 = it.latitudeE6 + 3_000) }
        val shape = checkNotNull(RouteShape.of(first + laid + after))
        val trace = RouteTrace.of(shape, width = 400f, height = 250f, inset = 20f)
        assertThat(trace.lines.map { it.laid }).containsExactly(false, true, false).inOrder()
    }

    @Test
    fun `a twelve-hour route is read, shaped and thinned in a moment`() {
        // A fix a second for twelve hours, wandering around a park and back: 43,200 points.
        val seconds = 12 * 60 * 60
        val points = (0 until seconds).map {
            val turn = it / 900.0
            point(
                it.toLong(),
                45.47 + 0.004 * sin(turn) + it * 0.0000001,
                9.18 + 0.006 * cos(turn * 0.7),
                if (it == 0) RoutePointKind.START else RoutePointKind.MEASURED,
            )
        }
        val bytes = RouteCodec.encode(points)
        val began = System.nanoTime()
        val read = RouteCodec.decode(bytes)
        val decoded = System.nanoTime()
        val shape = checkNotNull(RouteShape.of(read))
        shape.frame
        shape.lines
        val shaped = System.nanoTime()
        // A card on a phone at three pixels to a dp, the route's box some 330 by 206 dp.
        val trace = RouteTrace.of(shape, width = 990f, height = 618f, inset = 36f)
        val thinned = System.nanoTime()
        println(
            "RouteTrace: %d points read in %.1f ms, shaped in %.1f ms, thinned to %d in %.1f ms".format(
                read.size,
                (decoded - began) / 1e6,
                (shaped - decoded) / 1e6,
                trace.pointCount,
                (thinned - shaped) / 1e6,
            ),
        )
        assertThat(read).hasSize(seconds)
        // No more points than the line has pixels to cross, far fewer than its fixes.
        assertThat(trace.pointCount).isLessThan(seconds / 4)
    }
}
