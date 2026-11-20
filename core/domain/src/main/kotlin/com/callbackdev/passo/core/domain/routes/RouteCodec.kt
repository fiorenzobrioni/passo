package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import java.io.ByteArrayOutputStream

/**
 * A route's points as bytes and back (docs/adr/0016-location.md, decision 8): each point as its
 * difference from the one before, in variable-length integers. A walker moves a few metres and
 * a second or two between points, so most differences fit one byte: about six bytes a point,
 * some ten kilobytes an hour of walking.
 *
 * The round trip is exact: a [RoutePoint] already holds whole seconds, millionths of a degree,
 * decimetres and metres, the resolution written here.
 *
 * A chunk stands by itself: its first point is written whole. The service appends one with each
 * batch of steps, so a chunk lost to a crash loses its own points and no other's.
 *
 * Layout: the version, the count, then for each point the time (seconds), the latitude and the
 * longitude (millionths of a degree), one number holding the accuracy, whether an altitude
 * follows and the point's kind, and the altitude (decimetres) if there is one. Everything but
 * the first point's values is a difference from the point before, zigzag-encoded.
 */
object RouteCodec {
    private const val VERSION = 1
    private const val KIND_MASK = 0b11
    private const val ALTITUDE_BIT = 0b100
    private const val FLAG_BITS = 3

    fun encode(points: List<RoutePoint>): ByteArray {
        val out = ByteArrayOutputStream(points.size * BYTES_A_POINT + HEADER_BYTES)
        out.writeVarint(VERSION.toLong())
        out.writeVarint(points.size.toLong())
        var second = 0L
        var latitude = 0L
        var longitude = 0L
        var altitude = 0L
        for (point in points) {
            out.writeSigned(point.epochSecond - second)
            out.writeSigned(point.latitudeE6 - latitude)
            out.writeSigned(point.longitudeE6 - longitude)
            val altitudeDm = point.altitudeDm
            val flags = (point.accuracyMeters.coerceAtLeast(0).toLong() shl FLAG_BITS) or
                (if (altitudeDm != null) ALTITUDE_BIT.toLong() else 0L) or
                point.kind.ordinal.toLong()
            out.writeVarint(flags)
            if (altitudeDm != null) {
                out.writeSigned(altitudeDm - altitude)
                altitude = altitudeDm.toLong()
            }
            second = point.epochSecond
            latitude = point.latitudeE6.toLong()
            longitude = point.longitudeE6.toLong()
        }
        return out.toByteArray()
    }

    /**
     * The points of [bytes]. A chunk cut short (a write the phone did not finish) gives the
     * points it holds whole; one of a version this build does not know gives none. Never an
     * exception: a damaged route is a shorter line, not a crash.
     */
    fun decode(bytes: ByteArray): List<RoutePoint> {
        val reader = Reader(bytes)
        val version = reader.varint() ?: return emptyList()
        if (version != VERSION.toLong()) return emptyList()
        val count = reader.varint() ?: return emptyList()
        val points = ArrayList<RoutePoint>(count.coerceIn(0, MAX_PREALLOCATED).toInt())
        var second = 0L
        var latitude = 0L
        var longitude = 0L
        var altitude = 0L
        var read = 0L
        while (read < count) {
            read++
            second += reader.signed() ?: break
            latitude += reader.signed() ?: break
            longitude += reader.signed() ?: break
            val flags = reader.varint() ?: break
            val kind = RoutePointKind.entries.getOrNull((flags and KIND_MASK.toLong()).toInt()) ?: break
            var altitudeDm: Int? = null
            if (flags and ALTITUDE_BIT.toLong() != 0L) {
                altitude += reader.signed() ?: break
                altitudeDm = altitude.toInt()
            }
            points += RoutePoint(
                epochSecond = second,
                latitudeE6 = latitude.toInt(),
                longitudeE6 = longitude.toInt(),
                altitudeDm = altitudeDm,
                accuracyMeters = (flags ushr FLAG_BITS).toInt(),
                kind = kind,
            )
        }
        return points
    }

    /** Unsigned, seven bits a byte, the eighth saying that another follows. */
    private fun ByteArrayOutputStream.writeVarint(value: Long) {
        var rest = value
        while (rest and PAYLOAD.inv() != 0L) {
            write(((rest and PAYLOAD) or CONTINUE).toInt())
            rest = rest ushr PAYLOAD_BITS
        }
        write(rest.toInt())
    }

    /** Zigzag: small differences either way stay small numbers. */
    private fun ByteArrayOutputStream.writeSigned(value: Long) = writeVarint((value shl 1) xor (value shr SIGN_SHIFT))

    private class Reader(private val bytes: ByteArray) {
        private var at = 0

        /** The next number, or null where the bytes end before it does. */
        fun varint(): Long? {
            var value = 0L
            var shift = 0
            while (at < bytes.size && shift <= MAX_SHIFT) {
                val byte = bytes[at++].toLong() and BYTE
                value = value or ((byte and PAYLOAD) shl shift)
                if (byte and CONTINUE == 0L) return value
                shift += PAYLOAD_BITS
            }
            return null
        }

        fun signed(): Long? = varint()?.let { (it ushr 1) xor -(it and 1) }
    }

    private const val PAYLOAD = 0x7FL
    private const val CONTINUE = 0x80L
    private const val BYTE = 0xFFL
    private const val PAYLOAD_BITS = 7
    private const val MAX_SHIFT = 63
    private const val SIGN_SHIFT = 63
    private const val BYTES_A_POINT = 6
    private const val HEADER_BYTES = 4
    private const val MAX_PREALLOCATED = 4_096L
}
