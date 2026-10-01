package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.WayId
import com.callbackdev.passo.core.model.WayKind

/**
 * A stop on a way: a stage (a stamp in the credential, a notification when reached) or, when
 * [stage] is false, a place on the way worth its line (the sanctuary of San Luca, the Cisa
 * Pass). [key] names its strings; [distanceMeters] is where it falls along the way's line.
 */
data class WayStop(
    val key: String,
    val distanceMeters: Int,
    val latitude: Double,
    val longitude: Double,
    val stage: Boolean,
)

/** One way as `tools/build_ways.py` writes it: encoded, decoded only when a map is drawn. */
internal class WaySource(
    val id: WayId,
    val lengthMeters: Int,
    val stops: List<WayStop>,
    val frame: GeoBox,
    val line: String,
    val land: List<String>,
    val lakes: List<String>,
    val rivers: List<String>,
    val borders: List<String>,
    val locatorFrame: GeoBox,
    val locatorLand: List<String>,
    val locatorLine: String,
    val parks: List<String> = emptyList(),
    val riverWidthMeters: Double = 0.0,
)

/**
 * A way or a city walk (PLANNING.md §11 Phase 11): its length as mapped on OpenStreetMap (or
 * as routed through its places), its stops in order (the first at 0, the last at
 * [lengthMeters]), and the map to draw it on.
 */
class Way internal constructor(private val source: WaySource) {
    val id: WayId get() = source.id
    val kind: WayKind get() = source.id.kind
    val lengthMeters: Int get() = source.lengthMeters
    val stops: List<WayStop> get() = source.stops

    /** The stops that are stages: the credential's stamps. */
    val stages: List<WayStop> by lazy { stops.filter { it.stage } }

    /** The map, decoded the first time it is asked for (a few thousand points). */
    val map: WayMap by lazy {
        val (latitudes, longitudes, meters) = Polyline.decode(source.line, 3, Polyline.LINE)
        WayMap(
            frame = source.frame,
            line = WayLine(latitudes, longitudes, meters),
            land = source.land.map(Polyline::decodePath),
            lakes = source.lakes.map(Polyline::decodePath),
            rivers = source.rivers.map(Polyline::decodePath),
            borders = source.borders.map(Polyline::decodePath),
            locatorFrame = source.locatorFrame,
            locatorLand = source.locatorLand.map(Polyline::decodePath),
            locatorLine = Polyline.decodePath(source.locatorLine),
            parks = source.parks.map(Polyline::decodePath),
            riverWidthMeters = source.riverWidthMeters,
        )
    }
}

/**
 * What a way's map draws: the [frame] the line fits in; the ground behind it (land, lakes,
 * rivers and borders, cut to a square around the frame, so that a box of another shape still
 * finds ground to its edges); and the locator, the whole country with the way on it. A city's
 * map has its water as [lakes], its canals as [rivers] drawn [riverWidthMeters] wide (a
 * country's rivers are hairlines, at 0), and its largest [parks].
 */
class WayMap(
    val frame: GeoBox,
    val line: WayLine,
    val land: List<GeoPath>,
    val lakes: List<GeoPath>,
    val rivers: List<GeoPath>,
    val borders: List<GeoPath>,
    val locatorFrame: GeoBox,
    val locatorLand: List<GeoPath>,
    val locatorLine: GeoPath,
    val parks: List<GeoPath> = emptyList(),
    val riverWidthMeters: Double = 0.0,
)

/** The five ways, shortest first, as the Ways page lists them; and the city walks. */
object Ways {
    private val everything: List<Way> by lazy { WayId.entries.map { Way(WayData.source(it)) } }

    /** The ways, walked over months. */
    val all: List<Way> by lazy { everything.filter { it.kind == WayKind.WAY } }

    /** The city walks, walked in outings, grouped by city in the order they are listed. */
    val walks: List<Way> by lazy { everything.filter { it.kind == WayKind.WALK } }

    fun of(id: WayId): Way = everything.first { it.id == id }
}
