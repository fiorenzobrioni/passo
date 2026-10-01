package com.callbackdev.passo.core.domain.ways

/**
 * Google's encoded polyline, generalised to [dimensions] values a point: each value as a
 * zig-zag delta from the previous point's, five bits a character from `?`. The ways' data is
 * written this way by `tools/build_ways.py` (its `encode`), which keeps a thousand-point line
 * in a few kilobytes of source.
 */
internal object Polyline {
    /** The points of [encoded], each as [dimensions] values divided by [scales]. */
    fun decode(encoded: String, dimensions: Int, scales: DoubleArray): Array<DoubleArray> {
        require(scales.size == dimensions)
        val columns = Array(dimensions) { DoubleArrayBuilder() }
        val running = LongArray(dimensions)
        var index = 0
        while (index < encoded.length) {
            for (dimension in 0 until dimensions) {
                var result = 0L
                var shift = 0
                while (true) {
                    check(index < encoded.length) { "A polyline cut in the middle of a point" }
                    val chunk = encoded[index++].code - CHAR_OFFSET
                    result = result or ((chunk and LOW_BITS).toLong() shl shift)
                    shift += BITS_PER_CHAR
                    if (chunk < CONTINUE_BIT) break
                }
                val delta = if (result and 1L == 1L) (result shr 1).inv() else result shr 1
                running[dimension] += delta
                columns[dimension].add(running[dimension] / scales[dimension])
            }
        }
        return Array(dimensions) { columns[it].toArray() }
    }

    /** Latitudes and longitudes at 1e-5 degrees: a background path. */
    fun decodePath(encoded: String): GeoPath {
        val (latitudes, longitudes) = decode(encoded, 2, COORDINATES)
        return GeoPath(latitudes, longitudes)
    }

    private const val CHAR_OFFSET = 63
    private const val LOW_BITS = 0x1F
    private const val CONTINUE_BIT = 0x20
    private const val BITS_PER_CHAR = 5
    private const val COORDINATE_SCALE = 1e5
    val COORDINATES = doubleArrayOf(COORDINATE_SCALE, COORDINATE_SCALE)
    val LINE = doubleArrayOf(COORDINATE_SCALE, COORDINATE_SCALE, 1.0)
}

/** A growing [DoubleArray], without boxing a thousand points. */
private class DoubleArrayBuilder {
    private var values = DoubleArray(INITIAL)
    private var size = 0

    fun add(value: Double) {
        if (size == values.size) values = values.copyOf(size * 2)
        values[size++] = value
    }

    fun toArray(): DoubleArray = values.copyOf(size)

    private companion object {
        const val INITIAL = 64
    }
}
