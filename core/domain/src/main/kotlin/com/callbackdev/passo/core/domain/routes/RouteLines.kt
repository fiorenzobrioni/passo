package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A small piece of the ground made flat around [originLatitude] and [originLongitude], in metres:
 * east is x, north is y. Over the few kilometres of a walk the error is a few parts in ten
 * thousand, far under the forty metres two walks of one round are compared at.
 */
internal class Plane(private val originLatitude: Double, private val originLongitude: Double) {
    private val metersPerLongitude = METERS_PER_DEGREE * cos(Math.toRadians(originLatitude)).coerceAtLeast(MIN_COSINE)

    fun x(longitude: Double): Double = (longitude - originLongitude) * metersPerLongitude

    fun y(latitude: Double): Double = (latitude - originLatitude) * METERS_PER_DEGREE

    private companion object {
        const val METERS_PER_DEGREE = 111_320.0
        const val MIN_COSINE = 0.01
    }
}

/**
 * A route walked evenly: a sample every [step] metres along its lines, none across a pause or a
 * ride (a [RoutePointKind.START] begins a stretch of its own). Each sample stands for the same
 * length of the walk, so a share of them is a share of its metres, whatever the receiver's own
 * rhythm was: a stop at a light, where the points crowd, weighs nothing more than a street walked.
 *
 * Built incrementally ([add]), so a walk under way can be fed as its points are kept.
 */
internal class EvenSamples(private val plane: Plane, private val step: Double) {
    val xs = DoubleList()
    val ys = DoubleList()

    /** The samples where a stretch begins: no line reaches them from the one before. */
    val breaks = BooleanList()

    // The last point taken, in the plane, and how far along its segment the next sample falls.
    private var lastX = 0.0
    private var lastY = 0.0
    private var started = false
    private var carried = 0.0

    val size: Int get() = xs.size

    /** Takes [point], and calls [onSample] with the index of each sample it adds, in order. */
    fun add(point: RoutePoint, onSample: (Int) -> Unit = {}) {
        val x = plane.x(point.longitude)
        val y = plane.y(point.latitude)
        if (!started || point.kind == RoutePointKind.START) {
            started = true
            lastX = x
            lastY = y
            carried = 0.0
            push(x, y, newStretch = true, onSample)
            return
        }
        val length = hypot(x - lastX, y - lastY)
        var along = step - carried
        while (along <= length) {
            val t = along / length
            push(lastX + (x - lastX) * t, lastY + (y - lastY) * t, newStretch = false, onSample)
            along += step
        }
        carried = length - (along - step)
        lastX = x
        lastY = y
    }

    private fun push(x: Double, y: Double, newStretch: Boolean, onSample: (Int) -> Unit) {
        xs.add(x)
        ys.add(y)
        breaks.add(newStretch)
        onSample(xs.size - 1)
    }

    companion object {
        /**
         * The samples of [points], every [step] metres, or more for a walk so long that they would
         * pass [maxSamples]: a day's hike is compared at a coarser grain, in the same time.
         */
        fun of(points: List<RoutePoint>, plane: Plane, step: Double, maxSamples: Int): EvenSamples {
            val length = lengthOf(points)
            val grain = max(step, length / maxSamples)
            return EvenSamples(plane, grain).also { samples -> points.forEach { samples.add(it) } }
        }

        /** The length of [points]' lines, none across a break. */
        fun lengthOf(points: List<RoutePoint>): Double {
            var total = 0.0
            for (index in 1 until points.size) {
                val point = points[index]
                if (point.kind == RoutePointKind.START) continue
                val before = points[index - 1]
                total += Geodesy.meters(before.latitude, before.longitude, point.latitude, point.longitude)
            }
            return total
        }
    }
}

/**
 * A route's line as segments between its samples, with where along the line each begins, in a
 * grid, so that what lies within [near] of a point is found by looking in nine cells, not along
 * the whole line: a year of outings is compared in moments.
 *
 * Along the line is a length that only grows as the route was walked: across a break it grows by
 * the jump, so a place walked later always stands further along than one walked before it.
 */
internal class LineIndex(private val samples: EvenSamples, private val near: Double) {
    private val segmentFrom: IntArray
    private val along: DoubleArray
    private val cell: Double
    private val grid = HashMap<Long, IntList>()

    init {
        val size = samples.size
        along = DoubleArray(size)
        val from = IntList()
        var longest = 0.0
        for (index in 1 until size) {
            val length = hypot(samples.xs[index] - samples.xs[index - 1], samples.ys[index] - samples.ys[index - 1])
            along[index] = along[index - 1] + length
            if (!samples.breaks[index]) {
                from.add(index - 1)
                longest = max(longest, length)
            }
        }
        segmentFrom = from.toArray()
        // A segment with a point within [near] of a place has both its ends within [near] plus its
        // own length: a cell that wide puts them in the nine around the place's own.
        cell = near + longest + 1.0
        for (segment in segmentFrom.indices) {
            val start = segmentFrom[segment]
            put(cellOf(samples.xs[start]), cellOf(samples.ys[start]), segment)
            val endX = cellOf(samples.xs[start + 1])
            val endY = cellOf(samples.ys[start + 1])
            if (endX != cellOf(samples.xs[start]) || endY != cellOf(samples.ys[start])) put(endX, endY, segment)
        }
    }

    /** How long the line is, from its first sample to its last. */
    val length: Double get() = along.lastOrNull() ?: 0.0

    /** The line has at least one segment to be near. */
    val drawn: Boolean get() = segmentFrom.isNotEmpty()

    /**
     * Where along the line the place ([x], [y]) is, as walked in order: the nearest pass within
     * [near] that is not behind [notBefore], the earliest of them when the line passes there more
     * than once (a street walked out and back). [NOT_NEAR] when no part of the line is within
     * [near]; [ONLY_BEHIND] when only passes behind [notBefore] are.
     */
    fun alongAt(x: Double, y: Double, notBefore: Double): Double {
        val cx = cellOf(x)
        val cy = cellOf(y)
        var best = Double.MAX_VALUE
        var anyNear = false
        for (dx in -1L..1L) {
            for (dy in -1L..1L) {
                val segments = grid[key(cx + dx, cy + dy)] ?: continue
                for (i in 0 until segments.size) {
                    val segment = segments[i]
                    val start = segmentFrom[segment]
                    val position = project(start, x, y) ?: continue
                    anyNear = true
                    if (position >= notBefore && position < best) best = position
                }
            }
        }
        return when {
            best != Double.MAX_VALUE -> best
            anyNear -> ONLY_BEHIND
            else -> NOT_NEAR
        }
    }

    /** Where along the line ([x], [y]) falls on the segment from sample [start]; null beyond [near] of it. */
    private fun project(start: Int, x: Double, y: Double): Double? {
        val ax = samples.xs[start]
        val ay = samples.ys[start]
        val bx = samples.xs[start + 1]
        val by = samples.ys[start + 1]
        val dx = bx - ax
        val dy = by - ay
        val squared = dx * dx + dy * dy
        val t = if (squared <= 0.0) 0.0 else (((x - ax) * dx + (y - ay) * dy) / squared).coerceIn(0.0, 1.0)
        val px = ax + dx * t
        val py = ay + dy * t
        val apart = hypot(x - px, y - py)
        if (apart > near) return null
        return along[start] + t * sqrt(squared)
    }

    private fun cellOf(value: Double): Long = floor(value / cell).toLong()

    private fun key(cx: Long, cy: Long): Long = (cx shl CELL_BITS) xor (cy and CELL_MASK)

    private fun put(cx: Long, cy: Long, segment: Int) {
        grid.getOrPut(key(cx, cy)) { IntList() }.add(segment)
    }

    companion object {
        const val NOT_NEAR: Double = -1.0
        const val ONLY_BEHIND: Double = -2.0
        private const val CELL_BITS = 32
        private const val CELL_MASK = 0xFFFF_FFFFL
    }
}

/**
 * A walk laid along a line in the order it was walked: each of its samples takes the nearest pass
 * of the line within reach that is not more than [slack] behind the furthest one taken so far, so
 * that a line walked the other way round finds only its first and last few metres in order.
 */
internal class InOrder(private val index: LineIndex, private val slack: Double) {
    private var furthest = 0.0

    /** Samples laid so far, and those that found the line in order. */
    var taken = 0
        private set
    var inOrder = 0
        private set

    /** Samples near the line, whatever the order: what «within forty metres» alone would count. */
    var near = 0
        private set

    /** Whether the last sample laid found the line in order. */
    var lastInOrder = false
        private set

    fun take(x: Double, y: Double) {
        taken++
        val position = index.alongAt(x, y, notBefore = furthest - slack)
        lastInOrder = position >= 0.0
        if (position != LineIndex.NOT_NEAR) near++
        if (lastInOrder) {
            inOrder++
            if (position > furthest) furthest = position
        }
    }

    val inOrderShare: Double get() = if (taken == 0) 0.0 else inOrder.toDouble() / taken

    val nearShare: Double get() = if (taken == 0) 0.0 else near.toDouble() / taken
}

/** A growable list of doubles, without boxing a walk's thousands of samples. */
internal class DoubleList {
    private var values = DoubleArray(INITIAL)
    var size = 0
        private set

    fun add(value: Double) {
        if (size == values.size) values = values.copyOf(size * 2)
        values[size++] = value
    }

    operator fun get(index: Int): Double = values[index]
}

/** The same, for whole numbers. */
internal class IntList {
    private var values = IntArray(INITIAL_SMALL)
    var size = 0
        private set

    fun add(value: Int) {
        if (size == values.size) values = values.copyOf(size * 2)
        values[size++] = value
    }

    operator fun get(index: Int): Int = values[index]

    fun toArray(): IntArray = values.copyOf(size)
}

/** The same, for yes and no. */
internal class BooleanList {
    private var values = BooleanArray(INITIAL)
    var size = 0
        private set

    fun add(value: Boolean) {
        if (size == values.size) values = values.copyOf(size * 2)
        values[size++] = value
    }

    operator fun get(index: Int): Boolean = values[index]
}

private const val INITIAL = 256
private const val INITIAL_SMALL = 4
