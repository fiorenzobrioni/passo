package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * A route as a GPX 1.1 file and back (PLANNING.md §11 Phase 16, «Sharing and saving»;
 * docs/adr/0016-location.md, decision 9): one track, a segment for each stretch walked (a new
 * one after a pause or a ride, so no map draws a line across a place not walked), each point
 * with its time and, where it has one, its elevation above mean sea level. Written by hand:
 * the format is a few tags, and a library would be a dependency for them.
 *
 * What the file holds is what the route holds, from the first step to the last: nothing is
 * trimmed, and the page that offers it says so.
 */
object GpxCodec {
    /**
     * [points] as a GPX document named [name] (the outing's own name, in the reader's words).
     * Coordinates in degrees to six decimals (a decimetre), elevations in metres to one, times
     * in UTC, as the schema asks.
     */
    fun encode(name: String, points: List<RoutePoint>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"Passo\" xmlns=\"http://www.topografix.com/GPX/1/1\"")
        append(" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"")
        append(" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">\n")
        append("  <metadata>\n")
        append("    <name>").append(escape(name)).append("</name>\n")
        points.firstOrNull()?.let { append("    <time>").append(time(it.epochSecond)).append("</time>\n") }
        append("  </metadata>\n")
        append("  <trk>\n")
        append("    <name>").append(escape(name)).append("</name>\n")
        var open = false
        for (point in points) {
            if (point.kind == RoutePointKind.START || !open) {
                if (open) append("    </trkseg>\n")
                append("    <trkseg>\n")
                open = true
            }
            append("      <trkpt lat=\"").append(degrees(point.latitude))
            append("\" lon=\"").append(degrees(point.longitude)).append("\">")
            point.altitudeDm?.let { append("<ele>").append(meters(it)).append("</ele>") }
            append("<time>").append(time(point.epochSecond)).append("</time>")
            append("</trkpt>\n")
        }
        if (open) append("    </trkseg>\n")
        append("  </trk>\n")
        append("</gpx>\n")
    }

    /**
     * The track points of [gpx], in order, the first of each segment a [RoutePointKind.START]:
     * any GPX with tracks, not only Passo's own. A point without a readable place is skipped;
     * one without a time takes the time of the point before. Never throws: a file that is not
     * a GPX gives no points.
     */
    fun decode(gpx: String): List<RoutePoint> {
        val points = ArrayList<RoutePoint>()
        var lastSecond = 0L
        for (segment in SEGMENT.findAll(gpx)) {
            var first = true
            for (match in POINT.findAll(segment.groupValues[1])) {
                val attributes = match.groupValues[1]
                val body = match.groupValues[2]
                val latitude = attribute(attributes, "lat")?.toDoubleOrNull()
                val longitude = attribute(attributes, "lon")?.toDoubleOrNull()
                if (latitude == null || longitude == null || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) {
                    continue
                }
                val second = tag(body, "time")?.let(::second) ?: lastSecond
                lastSecond = second
                points += RoutePoint(
                    epochSecond = second,
                    latitudeE6 = (latitude * MICRO).roundToLong().toInt(),
                    longitudeE6 = (longitude * MICRO).roundToLong().toInt(),
                    altitudeDm = tag(body, "ele")?.toDoubleOrNull()?.let { (it * DECIMETERS).roundToInt() },
                    accuracyMeters = 0,
                    kind = if (first) RoutePointKind.START else RoutePointKind.MEASURED,
                )
                first = false
            }
        }
        return points
    }

    private fun attribute(attributes: String, name: String): String? =
        Regex("""\b$name\s*=\s*["']([^"']*)["']""").find(attributes)?.groupValues?.get(1)

    private fun tag(body: String, name: String): String? =
        Regex("""<$name\b[^>]*>([^<]*)</$name>""").find(body)?.groupValues?.get(1)?.trim()

    private fun second(text: String): Long? = try {
        Instant.parse(text).epochSecond
    } catch (e: DateTimeParseException) {
        null
    }

    private fun time(epochSecond: Long): String = Instant.ofEpochSecond(epochSecond).toString()

    private fun degrees(value: Double): String = String.format(Locale.ROOT, "%.6f", value)

    private fun meters(decimeters: Int): String = String.format(Locale.ROOT, "%.1f", decimeters / DECIMETERS)

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private val SEGMENT = Regex("""<trkseg\b[^>]*>(.*?)</trkseg>""", RegexOption.DOT_MATCHES_ALL)
    private val POINT = Regex("""<trkpt\b([^>]*?)(?:/>|>(.*?)</trkpt>)""", RegexOption.DOT_MATCHES_ALL)
    private const val MICRO = 1_000_000.0
    private const val DECIMETERS = 10.0
}
