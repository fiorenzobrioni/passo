package com.callbackdev.passo.core.domain.ways

import kotlin.math.cos

/** A box of the map, in degrees. */
data class GeoBox(val south: Double, val west: Double, val north: Double, val east: Double) {
    val middleLatitude: Double get() = (south + north) / 2

    /** Height over width as drawn: a degree of longitude is shorter away from the equator. */
    val aspect: Double
        get() = (north - south) / ((east - west) * cos(Math.toRadians(middleLatitude)))
}

/** A point on the map, in degrees. */
data class GeoPoint(val latitude: Double, val longitude: Double)

/** A run of points (a coast, a river, a border), as two parallel arrays. */
class GeoPath(val latitudes: DoubleArray, val longitudes: DoubleArray) {
    init {
        require(latitudes.size == longitudes.size)
    }

    val size: Int get() = latitudes.size
}

/**
 * A way's line, with the metres walked along it at each point, measured on the full-resolution
 * line before it was simplified: a point at a given distance falls where it does on the way.
 */
class WayLine(val latitudes: DoubleArray, val longitudes: DoubleArray, val meters: DoubleArray) {
    init {
        require(latitudes.size == longitudes.size && latitudes.size == meters.size && latitudes.size >= 2)
    }

    val size: Int get() = latitudes.size

    val lengthMeters: Double get() = meters.last()

    /** The point [distance] metres along the way, clamped to its ends. */
    fun pointAt(distance: Double): GeoPoint {
        val at = segmentAt(distance)
        val from = meters[at]
        val span = meters[at + 1] - from
        val t = if (span <= 0.0) 0.0 else ((distance - from) / span).coerceIn(0.0, 1.0)
        return GeoPoint(
            latitudes[at] + t * (latitudes[at + 1] - latitudes[at]),
            longitudes[at] + t * (longitudes[at + 1] - longitudes[at]),
        )
    }

    /**
     * The index of the segment [distance] falls in: the last point at or before it, never the
     * last point of the line (a segment needs two).
     */
    fun segmentAt(distance: Double): Int {
        if (distance <= meters.first()) return 0
        if (distance >= meters.last()) return size - 2
        var low = 0
        var high = size - 1
        while (high - low > 1) {
            val mid = (low + high) ushr 1
            if (meters[mid] <= distance) low = mid else high = mid
        }
        return low
    }
}
