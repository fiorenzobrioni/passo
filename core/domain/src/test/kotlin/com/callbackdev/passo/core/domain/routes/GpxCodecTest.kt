package com.callbackdev.passo.core.domain.routes

import com.callbackdev.passo.core.model.RoutePoint
import com.callbackdev.passo.core.model.RoutePointKind
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A route as a GPX file another app reads (PLANNING.md §11 Phase 16, «Sharing and saving»):
 * checked against the schema's shape, and round the trip.
 */
class GpxCodecTest {
    private fun point(second: Long, north: Int, kind: RoutePointKind, altitudeDm: Int? = 1_204) = RoutePoint(
        epochSecond = START + second,
        latitudeE6 = 45_464_200 + north,
        longitudeE6 = 9_190_000,
        altitudeDm = altitudeDm,
        accuracyMeters = 5,
        kind = kind,
    )

    /** Two stretches: a walk, a pause of ten minutes, a walk; a point laid from the steps in the first. */
    private val route = listOf(
        point(0, 0, RoutePointKind.START),
        point(2, 30, RoutePointKind.MEASURED),
        point(40, 400, RoutePointKind.LAID, altitudeDm = null),
        point(42, 430, RoutePointKind.MEASURED, altitudeDm = 1_231),
        point(642, 440, RoutePointKind.START),
        point(644, 470, RoutePointKind.MEASURED),
    )

    private fun parse(gpx: String) = DocumentBuilderFactory.newInstance()
        .apply { isNamespaceAware = true }
        .newDocumentBuilder()
        .parse(InputSource(StringReader(gpx)))

    @Test
    fun `it is well-formed XML with GPX 1_1's root, namespace and creator`() {
        val root = parse(GpxCodec.encode("Brisk walk", route)).documentElement

        assertThat(root.localName).isEqualTo("gpx")
        assertThat(root.namespaceURI).isEqualTo("http://www.topografix.com/GPX/1/1")
        assertThat(root.getAttribute("version")).isEqualTo("1.1")
        assertThat(root.getAttribute("creator")).isEqualTo("Passo")
    }

    @Test
    fun `the schema's shape - metadata, one track, its segments, their points in the schema's order`() {
        val document = parse(GpxCodec.encode("Brisk walk", route))
        val root = document.documentElement
        val children = (0 until root.childNodes.length).map { root.childNodes.item(it) }
            .filter { it.nodeType == org.w3c.dom.Node.ELEMENT_NODE }
            .map { it.localName }

        // gpxType is a sequence: metadata before trk.
        assertThat(children).containsExactly("metadata", "trk").inOrder()
        assertThat(document.getElementsByTagNameNS("*", "trkseg").length).isEqualTo(2)
        val points = document.getElementsByTagNameNS("*", "trkpt")
        assertThat(points.length).isEqualTo(6)
        val first = points.item(0)
        assertThat(first.attributes.getNamedItem("lat").nodeValue).isEqualTo("45.464200")
        assertThat(first.attributes.getNamedItem("lon").nodeValue).isEqualTo("9.190000")
        // wptType is a sequence too: ele, then time.
        val inside = (0 until first.childNodes.length).map { first.childNodes.item(it).localName }
        assertThat(inside).containsExactly("ele", "time").inOrder()
        assertThat(first.childNodes.item(0).textContent).isEqualTo("120.4")
        assertThat(first.childNodes.item(1).textContent).isEqualTo("2026-11-18T08:00:00Z")
    }

    @Test
    fun `a pause of ten minutes - a new segment, and no line across a place not walked`() {
        val document = parse(GpxCodec.encode("Brisk walk", route))
        val segments = document.getElementsByTagNameNS("*", "trkseg")

        assertThat(
            segments.item(0).childNodes.let { nodes ->
                (0 until nodes.length).count {
                    nodes.item(it).localName ==
                        "trkpt"
                }
            },
        )
            .isEqualTo(4)
        assertThat(
            segments.item(1).childNodes.let { nodes ->
                (0 until nodes.length).count {
                    nodes.item(it).localName ==
                        "trkpt"
                }
            },
        )
            .isEqualTo(2)
    }

    @Test
    fun `a point with no altitude has no elevation, not a zero`() {
        val gpx = GpxCodec.encode("Brisk walk", route)
        val third = parse(gpx).getElementsByTagNameNS("*", "trkpt").item(2)

        assertThat(third.childNodes.length).isEqualTo(1)
        assertThat(third.firstChild.localName).isEqualTo("time")
    }

    @Test
    fun `round the trip - every point where it was, when it was, as high as it was`() {
        val back = GpxCodec.decode(GpxCodec.encode("Brisk walk", route))

        assertThat(back).hasSize(route.size)
        assertThat(back.map { it.epochSecond }).isEqualTo(route.map { it.epochSecond })
        assertThat(back.map { it.latitudeE6 }).isEqualTo(route.map { it.latitudeE6 })
        assertThat(back.map { it.longitudeE6 }).isEqualTo(route.map { it.longitudeE6 })
        assertThat(back.map { it.altitudeDm }).isEqualTo(route.map { it.altitudeDm })
        // A stretch begins where a segment does; what a GPX cannot say (laid from the steps)
        // comes back as walked.
        assertThat(
            back.map {
                it.kind == RoutePointKind.START
            },
        ).isEqualTo(route.map { it.kind == RoutePointKind.START })
    }

    @Test
    fun `the outing's name is the reader's own words, whatever they hold`() {
        val gpx = GpxCodec.encode("Tom & Anna <home> \"loop\"", route)

        val name = parse(gpx).getElementsByTagNameNS("*", "name").item(0).textContent
        assertThat(name).isEqualTo("Tom & Anna <home> \"loop\"")
    }

    @Test
    fun `no points, a file with a track and nothing in it - still a GPX`() {
        val document = parse(GpxCodec.encode("Walk", emptyList()))

        assertThat(document.getElementsByTagNameNS("*", "trk").length).isEqualTo(1)
        assertThat(document.getElementsByTagNameNS("*", "trkpt").length).isEqualTo(0)
        assertThat(GpxCodec.decode(GpxCodec.encode("Walk", emptyList()))).isEmpty()
    }

    @Test
    fun `another app's file is read - attributes in any order, points with no time or no body`() {
        val foreign = """
            <?xml version="1.0"?>
            <gpx version="1.1" creator="Elsewhere" xmlns="http://www.topografix.com/GPX/1/1">
              <trk><trkseg>
                <trkpt lon='9.19' lat='45.4642'><ele>120</ele><time>2026-11-18T08:00:00.000Z</time></trkpt>
                <trkpt lat="45.4643" lon="9.19"/>
                <trkpt lat="not a number" lon="9.19"></trkpt>
                <trkpt lat="45.4644" lon="9.19"><time>2026-11-18T08:00:09Z</time></trkpt>
              </trkseg></trk>
            </gpx>
        """.trimIndent()

        val points = GpxCodec.decode(foreign)

        assertThat(points).hasSize(3)
        assertThat(points[0].kind).isEqualTo(RoutePointKind.START)
        assertThat(points[0].altitudeDm).isEqualTo(1_200)
        assertThat(points[1].epochSecond).isEqualTo(points[0].epochSecond)
        assertThat(points[1].altitudeDm).isNull()
        assertThat(points[2].epochSecond).isEqualTo(START + 9)
    }

    @Test
    fun `what is not a GPX gives no points, and never throws`() {
        assertThat(GpxCodec.decode("")).isEmpty()
        assertThat(GpxCodec.decode("{\"steps\": 12}")).isEmpty()
        assertThat(GpxCodec.decode("<gpx><trk><trkseg><trkpt lat=\"45\"")).isEmpty()
    }

    private companion object {
        /** 18 Nov 2026, 08:00 UTC. */
        const val START = 1_794_988_800L
    }
}
