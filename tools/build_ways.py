#!/usr/bin/env python3
"""Builds the Ways' data (PLANNING.md §11 Phase 11): each way's line, its stops, and the map
behind it, from OpenStreetMap and Natural Earth.

    python3 tools/build_ways.py fetch   # downloads what is missing into tools/ways-cache/ (not
                                        # committed), and routes the walks into tools/walks/
    python3 tools/build_ways.py         # writes the app's files from that cache

The content (which relations, which stops, what is said of them) is tools/ways_content.py.
Re-running this script IS the data: its outputs are not edited by hand.

- core/domain/.../ways/WayData.kt: each way's line (simplified, with the distance along it at
  every point), its stops snapped to it, and the map behind it, as encoded polylines.
- core/designsystem/src/main/res/values{,-it}/strings_ways_places.xml: the names and the notes.
- core/designsystem/.../ways/WayPlaceStrings.kt: from a stop's key to its strings.

Sources and licences:
- The lines are OpenStreetMap route relations, read through the Waymarked Trails API, which
  hands each relation over in order with its geometry. © OpenStreetMap contributors, ODbL 1.0:
  the derived data says so in its header, and the app credits it (About, the guide).
- Land, lakes, rivers and borders are Natural Earth (public domain), 1:10m; the locator maps
  1:50m.

The line is the shortest path from the first stop to the last over the relation's main ways
(variants and appendices left out), with the gaps where the mapping breaks joined straight and
reported. The build fails if a stop is far from the line or out of order. Only the standard
library is used, so any Python 3.10+ runs it.
"""

import heapq
import http.client
import json
import math
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

import xml.etree.ElementTree as ElementTree

from ways_content import LOCATORS, WALKS, WAYS

ROOT = Path(__file__).resolve().parent.parent
CACHE = Path(__file__).resolve().parent / "ways-cache"
# The walks' routes, as BRouter drew them: committed, so a build needs no router (ODbL data).
ROUTES = Path(__file__).resolve().parent / "walks"
DOMAIN_OUT = ROOT / "core/domain/src/main/kotlin/com/callbackdev/passo/core/domain/ways/WayData.kt"
STRINGS_EN = ROOT / "core/designsystem/src/main/res/values/strings_ways_places.xml"
STRINGS_IT = ROOT / "core/designsystem/src/main/res/values-it/strings_ways_places.xml"
STRINGS_KT = ROOT / "core/designsystem/src/main/kotlin/com/callbackdev/passo/core/designsystem/ways/WayPlaceStrings.kt"

WMT = "https://hiking.waymarkedtrails.org/api/v1/details/relation/{}"
OSM = "https://api.openstreetmap.org/api/0.6/{}/{}/full"
# BRouter's walking profile: footways, parks and pedestrian streets first.
BROUTER = "https://brouter.de/brouter?lonlats={}&profile=hiking-mountain&alternativeidx=0&format=geojson"
WALK_MAX_SNAP_METRES = 150  # a walk is routed through its places: one farther off is a mistake
# A city's streets come from the OpenStreetMap API's map call, which answers a small box at a
# time (50,000 nodes at most): the walk's ground is fetched in tiles of this size, in degrees.
OSM_MAP = "https://api.openstreetmap.org/api/0.6/map?bbox={:.4f},{:.4f},{:.4f},{:.4f}"
STREET_TILE = (0.007, 0.010)  # latitude, longitude
# The streets drawn, in two weights: the arteries, and the streets that give a centre its shape.
MAIN_STREETS = {"trunk", "primary", "secondary"}
MINOR_STREETS = {"tertiary", "pedestrian"}
# The smaller classes a walk can add to its minor streets (Walk.more_streets), for a centre
# mapped mostly in those. The cache keeps them for every walk, so choosing them needs no fetch.
LANES = {"residential", "unclassified", "living_street"}
MIN_STREET_METRES = 150
MAP_MIN_ASPECT, MAP_MAX_ASPECT = 0.62, 1.2  # WayMapView's own bounds for a map box's shape
STREET_MARGIN = 1.3  # around what the page shows: the inset, and the box a little wider
NE = "https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/{}.geojson"
NE_LAYERS = [
    "ne_10m_land",
    "ne_10m_lakes",
    "ne_10m_rivers_lake_centerlines",
    "ne_10m_admin_0_boundary_lines_land",
    "ne_50m_land",
]

MAX_SNAP_METRES = 2000  # a stop farther than this from the line is a mistake in the content
MAX_GAP_METRES = 3000  # a break in the mapping wider than this is not joined
LINE_TOLERANCE = 1 / 4000  # of the frame's diagonal: under a pixel on the largest phone
MAP_TOLERANCE = 1 / 1500
LOCATOR_TOLERANCE_METRES = 2500
RIVER_MAX_SCALERANK = 8  # Natural Earth's rank: lower is larger; small streams left out
EARTH = 6371008.8
MERCATOR = 6378137.0


# --- Fetch ------------------------------------------------------------------------------------

def fetch():
    CACHE.mkdir(exist_ok=True)
    for way in WAYS:
        for relation in way.relations:
            download(WMT.format(relation), CACHE / f"relation-{relation}.json")
    for layer in NE_LAYERS:
        download(NE.format(layer), CACHE / f"{layer}.geojson")
    ROUTES.mkdir(exist_ok=True)
    for walk in WALKS:
        lonlats = "|".join(f"{stop.lon:.6f},{stop.lat:.6f}" for stop in walk.stops)
        route = ROUTES / f"{walk.id.lower()}.geojson"
        if not route.exists():
            raw = CACHE / f"brouter-{walk.id.lower()}.geojson"
            download(BROUTER.format(lonlats), raw)
            # Only the line is kept: BRouter's turn-by-turn table would triple the file.
            geometry = json.loads(raw.read_text())["features"][0]["geometry"]
            geometry["coordinates"] = [[round(c[0], 6), round(c[1], 6)] for c in geometry["coordinates"]]
            route.write_text(json.dumps({
                "type": "FeatureCollection",
                "features": [{
                    "type": "Feature",
                    "properties": {"source": "BRouter, profile hiking-mountain; © OpenStreetMap contributors, ODbL"},
                    "geometry": geometry,
                }],
            }, separators=(",", ":")) + "\n")
        for ref in walk.water + walk.canals + walk.parks:
            kind, number = ref.split("/")
            download(OSM.format(kind, number), CACHE / f"osm-{kind}-{number}.xml")
        if walk.streets:
            for tile, name in street_tiles(walk):
                fetch_streets(tile, CACHE / name, MAIN_STREETS | MINOR_STREETS | LANES)


def fetch_streets(tile, target, classes):
    """One tile's streets, kept as they are drawn (each way's class and points), not as the
    map call answers (every building too: a hundred times the size). A tile too dense for the
    call is fetched in four."""
    if target.exists():
        return
    south, west, north, east = tile
    url = OSM_MAP.format(west, south, east, north)
    print(f"  {url}")
    request = urllib.request.Request(url, headers={"User-Agent": "passo-build-ways (github.com/fiorenzobrioni/passo)"})
    try:
        root = ElementTree.fromstring(read_retrying(request))
    except urllib.error.HTTPError as error:
        if error.code != 400:
            raise
        middle = ((south + north) / 2, (west + east) / 2)
        quarters = [
            (south, west, middle[0], middle[1]), (south, middle[1], middle[0], east),
            (middle[0], west, north, middle[1]), (middle[0], middle[1], north, east),
        ]
        ways = {}
        for i, quarter in enumerate(quarters):
            part = target.with_suffix(f".{i}.json")
            fetch_streets(quarter, part, classes)
            ways.update(json.loads(part.read_text()))
            part.unlink()
        target.write_text(json.dumps(ways))
        return
    nodes = {n.get("id"): (float(n.get("lat")), float(n.get("lon"))) for n in root.iter("node")}
    ways = {}
    for w in root.iter("way"):
        tags = {t.get("k"): t.get("v") for t in w.iter("tag")}
        kind = tags.get("highway")
        # A pedestrian area (a square) is a shape, not a street.
        if kind not in classes or tags.get("area") == "yes":
            continue
        points = [nodes[nd.get("ref")] for nd in w.iter("nd") if nd.get("ref") in nodes]
        if len(points) >= 2:
            ways[w.get("id")] = [kind, [[round(a, 7), round(b, 7)] for a, b in points]]
    target.write_text(json.dumps(ways))


def read_retrying(request, attempts=4):
    """A shared server drops a connection now and then: try again, a little later each time.
    An HTTP error is an answer, not a drop, and is passed on."""
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(request, timeout=300) as response:
                return response.read()
        except urllib.error.HTTPError:
            raise
        except (OSError, http.client.HTTPException):
            if attempt == attempts - 1:
                raise
            time.sleep(10 * 2 ** attempt)


def street_box(walk):
    """What a walk's page can show of its map: the frame stretched to the box's shape (held
    between MAP_MIN_ASPECT and MAP_MAX_ASPECT, as WayMapView does), with room for its inset.
    Only this is fetched and drawn: the ground behind is cut much wider."""
    frame, ground, local = frame_of(walk_line(walk))
    south, west, north, east = frame
    width = (east - west) * local.kx
    height = (north - south) * local.ky
    aspect = min(max(height / width, MAP_MIN_ASPECT), MAP_MAX_ASPECT)
    half_width = STREET_MARGIN * max(width, height / aspect) / 2
    half_height = STREET_MARGIN * max(height, width * aspect) / 2
    middle = ((south + north) / 2, (west + east) / 2)
    return (
        max(ground[0], middle[0] - half_height / local.ky),
        max(ground[1], middle[1] - half_width / local.kx),
        min(ground[2], middle[0] + half_height / local.ky),
        min(ground[3], middle[1] + half_width / local.kx),
    )


def street_tiles(walk):
    """The walk's street box cut into tiles the map call accepts, with their cache names."""
    south, west, north, east = street_box(walk)
    rows = math.ceil((north - south) / STREET_TILE[0])
    columns = math.ceil((east - west) / STREET_TILE[1])
    for row in range(rows):
        for column in range(columns):
            tile = (
                south + row * STREET_TILE[0],
                west + column * STREET_TILE[1],
                min(north, south + (row + 1) * STREET_TILE[0]),
                min(east, west + (column + 1) * STREET_TILE[1]),
            )
            # "roads": a cache from before LANES were kept would silently lack them.
            yield tile, f"roads-{walk.id.lower()}-{row}-{column}.json"


def download(url, target):
    # Only what is missing: the sources are big, and the servers are shared. Delete a file to
    # fetch it again.
    if target.exists():
        return
    print(f"  {url}")
    request = urllib.request.Request(url, headers={"User-Agent": "passo-build-ways (github.com/fiorenzobrioni/passo)"})
    with urllib.request.urlopen(request, timeout=300) as response:
        target.write_bytes(response.read())


# --- Geometry ---------------------------------------------------------------------------------

def from_mercator(x, y):
    return (math.degrees(2 * math.atan(math.exp(y / MERCATOR)) - math.pi / 2), math.degrees(x / MERCATOR))


def haversine(a, b):
    la1, lo1, la2, lo2 = map(math.radians, (a[0], a[1], b[0], b[1]))
    h = math.sin((la2 - la1) / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin((lo2 - lo1) / 2) ** 2
    return 2 * EARTH * math.asin(math.sqrt(h))


class Local:
    """An equirectangular projection in metres around a latitude: enough for simplifying."""

    def __init__(self, lat):
        self.kx = math.cos(math.radians(lat)) * math.pi * EARTH / 180
        self.ky = math.pi * EARTH / 180

    def xy(self, p):
        return (p[1] * self.kx, p[0] * self.ky)


def simplify(points, tolerance, local, closed=False):
    """Douglas-Peucker, iterative, in metres. Keeps the ends (and, closed, two far points)."""
    return [points[i] for i in simplified_indices(points, tolerance, local, closed)]


def simplified_indices(points, tolerance, local, closed=False):
    if len(points) < 3:
        return list(range(len(points)))
    xy = [local.xy(p) for p in points]
    keep = [False] * len(points)
    keep[0] = keep[-1] = True
    stack = [(0, len(points) - 1)]
    while stack:
        first, last = stack.pop()
        ax, ay = xy[first]
        bx, by = xy[last]
        dx, dy = bx - ax, by - ay
        length = math.hypot(dx, dy)
        worst, index = -1.0, -1
        for i in range(first + 1, last):
            px, py = xy[i]
            if length == 0:
                d = math.hypot(px - ax, py - ay)
            else:
                d = abs(dy * px - dx * py + bx * ay - by * ax) / length
            if d > worst:
                worst, index = d, i
        if worst > tolerance and index > 0:
            keep[index] = True
            stack.append((first, index))
            stack.append((index, last))
    if closed:
        far = max(range(len(points)), key=lambda i: math.hypot(xy[i][0] - xy[0][0], xy[i][1] - xy[0][1]))
        keep[far] = True
    return [i for i, k in enumerate(keep) if k]


def clip_polygon(ring, box):
    """Sutherland-Hodgman against a (south, west, north, east) box: the fill stays right."""
    south, west, north, east = box
    edges = [
        (lambda p: p[1] >= west, lambda a, b: cut_lon(a, b, west)),
        (lambda p: p[1] <= east, lambda a, b: cut_lon(a, b, east)),
        (lambda p: p[0] >= south, lambda a, b: cut_lat(a, b, south)),
        (lambda p: p[0] <= north, lambda a, b: cut_lat(a, b, north)),
    ]
    out = ring
    for inside, cut in edges:
        if not out:
            break
        source, out = out, []
        previous = source[-1]
        for point in source:
            if inside(point):
                if not inside(previous):
                    out.append(cut(previous, point))
                out.append(point)
            elif inside(previous):
                out.append(cut(previous, point))
            previous = point
    return out


def cut_lon(a, b, lon):
    t = (lon - a[1]) / (b[1] - a[1])
    return (a[0] + t * (b[0] - a[0]), lon)


def cut_lat(a, b, lat):
    t = (lat - a[0]) / (b[0] - a[0])
    return (lat, a[1] + t * (b[1] - a[1]))


def clip_line(line, box):
    """The pieces of a polyline inside the box (Liang-Barsky on each segment)."""
    south, west, north, east = box
    pieces, current = [], []
    for a, b in zip(line, line[1:]):
        segment = clip_segment(a, b, box)
        if segment is None:
            if len(current) > 1:
                pieces.append(current)
            current = []
            continue
        p, q = segment
        if not current or current[-1] != p:
            if len(current) > 1:
                pieces.append(current)
            current = [p]
        current.append(q)
    if len(current) > 1:
        pieces.append(current)
    return pieces


def clip_segment(a, b, box):
    south, west, north, east = box
    t0, t1 = 0.0, 1.0
    dx, dy = b[1] - a[1], b[0] - a[0]
    for p, q in ((-dx, a[1] - west), (dx, east - a[1]), (-dy, a[0] - south), (dy, north - a[0])):
        if p == 0:
            if q < 0:
                return None
            continue
        r = q / p
        if p < 0:
            if r > t1:
                return None
            t0 = max(t0, r)
        else:
            if r < t0:
                return None
            t1 = min(t1, r)
    return ((a[0] + t0 * dy, a[1] + t0 * dx), (a[0] + t1 * dy, a[1] + t1 * dx))


def bbox_of(points):
    lats = [p[0] for p in points]
    lons = [p[1] for p in points]
    return (min(lats), min(lons), max(lats), max(lons))


def intersects(a, b):
    return not (a[2] < b[0] or b[2] < a[0] or a[3] < b[1] or b[3] < a[1])


# --- The line ---------------------------------------------------------------------------------

def base_ways(node, out):
    """Every way of a Waymarked Trails route, in its order, leaving the appendices out."""
    if isinstance(node, list):
        for child in node:
            base_ways(child, out)
    elif isinstance(node, dict):
        if node.get("route_type") == "base" and "geometry" in node:
            geometry = node["geometry"]
            if geometry["type"] == "LineString":
                out.append(geometry["coordinates"])
            elif geometry["type"] == "MultiLineString":
                out.extend(geometry["coordinates"])
        for key in ("main", "ways"):
            if key in node:
                base_ways(node[key], out)


def way_line(way):
    lines = []
    for relation in way.relations:
        data = json.loads((CACHE / f"relation-{relation}.json").read_text())
        base_ways(data["route"]["main"], lines)
    nodes, edges = {}, {}

    def connect(a, b, weight):
        edges.setdefault(a, []).append((b, weight))
        edges.setdefault(b, []).append((a, weight))

    for line in lines:
        previous = None
        for x, y in line:
            key = (round(x, 1), round(y, 1))
            nodes[key] = from_mercator(x, y)
            if previous is not None and previous != key:
                connect(previous, key, haversine(nodes[previous], nodes[key]))
            previous = key
    gaps = join_gaps(nodes, edges)
    first, last = way.stops[0], way.stops[-1]
    start = min(nodes, key=lambda k: haversine(nodes[k], (first.lat, first.lon)))
    end = min(nodes, key=lambda k: haversine(nodes[k], (last.lat, last.lon)))
    path = [nodes[k] for k in shortest_path(edges, start, end)]
    if way.join_end:
        path.append((last.lat, last.lon))
    return path, gaps


def join_gaps(nodes, edges):
    """Joins the pieces of a broken mapping, nearest ends first; a joined gap costs triple."""
    joined = []
    while True:
        component = {}
        for count, seed in enumerate(edges):
            if seed in component:
                continue
            component[seed] = count
            stack = [seed]
            while stack:
                for neighbour, _ in edges[stack.pop()]:
                    if neighbour not in component:
                        component[neighbour] = count
                        stack.append(neighbour)
        if len(set(component.values())) == 1:
            return joined
        ends = [n for n in edges if len(edges[n]) <= 1]
        best = None
        for a in ends:
            for b in ends:
                if component[a] < component[b]:
                    d = haversine(nodes[a], nodes[b])
                    if d < MAX_GAP_METRES and (best is None or d < best[0]):
                        best = (d, a, b)
        if best is None:
            return joined
        d, a, b = best
        edges[a].append((b, 3 * d))
        edges[b].append((a, 3 * d))
        joined.append(d)


def shortest_path(edges, start, end):
    distance, previous, queue = {start: 0.0}, {}, [(0.0, start)]
    while queue:
        d, node = heapq.heappop(queue)
        if node == end:
            break
        if d > distance[node]:
            continue
        for neighbour, weight in edges[node]:
            nd = d + weight
            if nd < distance.get(neighbour, math.inf):
                distance[neighbour] = nd
                previous[neighbour] = node
                heapq.heappush(queue, (nd, neighbour))
    if end not in distance:
        sys.exit("no path from the first stop to the last: the relation is broken beyond repair")
    path = [end]
    while path[-1] != start:
        path.append(previous[path[-1]])
    return path[::-1]


def cumulative(path):
    out = [0.0]
    for a, b in zip(path, path[1:]):
        out.append(out[-1] + haversine(a, b))
    return out


def snap(way, path, along):
    """Each stop's distance along the line, at the nearest point of it; checked in order."""
    stops = []
    for index, stop in enumerate(way.stops):
        if index == 0:
            i = 0
        elif index == len(way.stops) - 1:
            i = len(path) - 1
        else:
            i = min(range(len(path)), key=lambda j: haversine(path[j], (stop.lat, stop.lon)))
        off = haversine(path[i], (stop.lat, stop.lon))
        if off > MAX_SNAP_METRES:
            sys.exit(f"{way.id}: {stop.key} is {off:.0f} m from the line")
        if stops and along[i] <= stops[-1][1]:
            sys.exit(f"{way.id}: {stop.key} comes before {stops[-1][0].key} on the line")
        stops.append((stop, along[i], path[i]))
    return stops


# --- The map behind it ------------------------------------------------------------------------

def frame_of(path):
    """The line's box, with a margin, and a square around it for the ground to cover."""
    south, west, north, east = bbox_of(path)
    mid = (south + north) / 2
    local = Local(mid)
    width = (east - west) * local.kx
    height = (north - south) * local.ky
    margin = 0.08 * max(width, height)
    frame = (
        south - margin / local.ky,
        west - margin / local.kx,
        north + margin / local.ky,
        east + margin / local.kx,
    )
    side = 1.7 * max(width, height) / 2
    cx, cy = (west + east) / 2, mid
    ground = (cy - side / local.ky, cx - side / local.kx, cy + side / local.ky, cx + side / local.kx)
    return frame, ground, local


def features(layer):
    return json.loads((CACHE / f"{layer}.geojson").read_text())["features"]


def rings(geometry):
    if geometry["type"] == "Polygon":
        return [geometry["coordinates"][0]]
    if geometry["type"] == "MultiPolygon":
        return [polygon[0] for polygon in geometry["coordinates"]]
    return []


def strings(geometry):
    if geometry["type"] == "LineString":
        return [geometry["coordinates"]]
    if geometry["type"] == "MultiLineString":
        return geometry["coordinates"]
    return []


def polygons(layer, box, tolerance, local, keep=lambda properties: True):
    out = []
    for feature in features(layer):
        if not keep(feature["properties"]):
            continue
        for ring in rings(feature["geometry"]):
            points = [(lat, lon) for lon, lat in ring]
            if not intersects(bbox_of(points), box):
                continue
            clipped = clip_polygon(points, box)
            if len(clipped) < 3:
                continue
            simple = simplify(clipped, tolerance, local, closed=True)
            if len(simple) >= 3:
                out.append(simple)
    return out


def polylines(layer, box, tolerance, local, keep=lambda properties: True):
    out = []
    for feature in features(layer):
        if not keep(feature["properties"]):
            continue
        for line in strings(feature["geometry"]):
            points = [(lat, lon) for lon, lat in line]
            if len(points) < 2 or not intersects(bbox_of(points), box):
                continue
            for piece in clip_line(points, box):
                simple = simplify(piece, tolerance, local)
                if len(simple) >= 2:
                    out.append(simple)
    return out


def river(properties):
    rank = properties.get("scalerank")
    return properties.get("featurecla") in ("River", "Lake Centerline") and rank is not None and rank <= RIVER_MAX_SCALERANK


# --- Encoding ---------------------------------------------------------------------------------

def encode(rows, scales):
    """Google's polyline encoding, generalised: each row's values, scaled, as deltas."""
    out, previous = [], [0] * len(scales)
    for row in rows:
        for i, (value, scale) in enumerate(zip(row, scales)):
            v = round(value * scale)
            delta, previous[i] = v - previous[i], v
            delta = ~(delta << 1) if delta < 0 else delta << 1
            while delta >= 0x20:
                out.append(chr((0x20 | (delta & 0x1F)) + 63))
                delta >>= 5
            out.append(chr(delta + 63))
    return "".join(out)


def kotlin_string(text, indent, lead=0):
    """A string literal split into lines under the 120-column limit; the compiler joins them.
    [lead] is what stands before it on its first line, after the indent."""
    width = 116 - indent - 4
    pieces, current, room = [], "", width - lead
    for char in text:
        escaped = {"\\": "\\\\", "$": "\\$", '"': '\\"'}.get(char, char)
        if len(current) + len(escaped) > room:
            pieces.append(current)
            current, room = "", width
        current += escaped
    pieces.append(current)
    literals = [f'"{piece}"' for piece in pieces]
    if len(literals) == 1:
        return literals[0]
    pad = " " * (indent + 4)
    return literals[0] + " +\n" + " +\n".join(pad + lit for lit in literals[1:])


def kotlin_paths(paths, indent, scales=(1e5, 1e5)):
    if not paths:
        return "emptyList()"
    pad = " " * (indent + 4)
    body = "".join(f"{pad}{kotlin_string(encode(p, scales), indent + 4)},\n" for p in paths)
    return "listOf(\n" + body + " " * indent + ")"


def box_literal(box):
    return "GeoBox({:.5f}, {:.5f}, {:.5f}, {:.5f})".format(*box)


# --- Outputs ----------------------------------------------------------------------------------

def build():
    blocks, places, notes = [], {}, {}
    for way in WAYS:
        path, gaps = way_line(way)
        along = cumulative(path)
        length = along[-1]
        stops = snap(way, path, along)
        frame, ground, local = frame_of(path)
        diagonal = math.hypot((frame[3] - frame[1]) * local.kx, (frame[2] - frame[0]) * local.ky)
        line = simplified_indices(path, diagonal * LINE_TOLERANCE, local)
        rows = [(path[i][0], path[i][1], along[i]) for i in line]
        land = polygons("ne_10m_land", ground, diagonal * MAP_TOLERANCE, local)
        lakes = polygons("ne_10m_lakes", ground, diagonal * MAP_TOLERANCE, local)
        rivers = polylines("ne_10m_rivers_lake_centerlines", ground, diagonal * MAP_TOLERANCE, local, river)
        borders = polylines("ne_10m_admin_0_boundary_lines_land", ground, diagonal * MAP_TOLERANCE, local)
        locator_box = LOCATORS[way.country]
        locator_local = Local((locator_box[0] + locator_box[2]) / 2)
        locator_land = polygons("ne_50m_land", locator_box, LOCATOR_TOLERANCE_METRES, locator_local)
        locator_line = simplify(path, LOCATOR_TOLERANCE_METRES, locator_local)
        print(
            f"{way.id}: {length / 1000:.1f} km, {len(path)} points -> {len(line)}, "
            f"{len(stops)} stops, gaps joined {[round(g) for g in gaps]} m, "
            f"land {sum(map(len, land))}, lakes {sum(map(len, lakes))}, rivers {sum(map(len, rivers))}, "
            f"borders {sum(map(len, borders))}, locator {sum(map(len, locator_land))}",
        )
        for stop, distance, point in stops:
            print(f"    {stop.key:24s} {distance / 1000:7.1f} km")
            name = (stop.en, stop.it)
            if places.get(stop.key, name) != name:
                sys.exit(f"{stop.key} has two different names")
            places[stop.key] = name
            if stop.note_en:
                note = (stop.note_en, stop.note_it)
                if notes.get(stop.key, note) != note:
                    sys.exit(f"{stop.key} has two different notes")
                notes[stop.key] = note
        stop_lines = "".join(
            f'            WayStop("{stop.key}", {round(distance)}, {point[0]:.5f}, {point[1]:.5f}, '
            f"stage = {'true' if stop.stage else 'false'}),\n"
            for stop, distance, point in stops
        )
        blocks.append(f"""
    private fun {camel(way.id)}() = WaySource(
        id = WayId.{way.id},
        lengthMeters = {round(length)},
        stops = listOf(
{stop_lines}        ),
        frame = {box_literal(frame)},
        line = {kotlin_string(encode(rows, (1e5, 1e5, 1)), 8, len('line = '))},
        land = {kotlin_paths(land, 8)},
        lakes = {kotlin_paths(lakes, 8)},
        rivers = {kotlin_paths(rivers, 8)},
        borders = {kotlin_paths(borders, 8)},
        locatorFrame = {box_literal(locator_box)},
        locatorLand = {kotlin_paths(locator_land, 8)},
        locatorLine = {kotlin_string(encode(locator_line, (1e5, 1e5)), 8, len('locatorLine = '))},
    )
""")
    for walk in WALKS:
        block, walk_places, walk_notes = build_walk(walk)
        blocks.append(block)
        for key, name in walk_places.items():
            if places.get(key, name) != name:
                sys.exit(f"{key} has two different names")
            places[key] = name
        notes.update(walk_notes)
    write_domain(blocks)
    write_strings(places, notes)


# --- The walks --------------------------------------------------------------------------------

def walk_line(walk):
    data = json.loads((ROUTES / f"{walk.id.lower()}.geojson").read_text())
    coordinates = data["features"][0]["geometry"]["coordinates"]
    path = []
    for lon, lat, *_ in coordinates:
        if not path or path[-1] != (lat, lon):
            path.append((lat, lon))
    return path


def osm_shapes(ref):
    """An OpenStreetMap way or relation as polygons (closed rings) and lines."""
    kind, number = ref.split("/")
    root = ElementTree.parse(CACHE / f"osm-{kind}-{number}.xml").getroot()
    nodes = {n.get("id"): (float(n.get("lat")), float(n.get("lon"))) for n in root.iter("node")}
    ways = {w.get("id"): [nodes[nd.get("ref")] for nd in w.iter("nd") if nd.get("ref") in nodes] for w in root.iter("way")}
    if kind == "way":
        points = ways[number]
        return ([points], []) if points[0] == points[-1] else ([], [points])
    relation = next(r for r in root.iter("relation") if r.get("id") == number)
    tags = {t.get("k"): t.get("v") for t in relation.iter("tag")}
    members = [m for m in relation.iter("member") if m.get("type") == "way" and m.get("ref") in ways]
    if tags.get("type") != "multipolygon":
        return [], [ways[m.get("ref")] for m in members]
    return join_rings([ways[m.get("ref")] for m in members if m.get("role") in ("outer", "")]), []


def join_rings(pieces):
    """Joins a multipolygon's outer ways end to end into closed rings."""
    pieces = [list(p) for p in pieces if len(p) > 1]
    rings = []
    while pieces:
        ring = pieces.pop(0)
        while ring[0] != ring[-1]:
            for i, piece in enumerate(pieces):
                if piece[0] == ring[-1]:
                    ring += piece[1:]
                elif piece[-1] == ring[-1]:
                    ring += piece[-2::-1]
                elif piece[-1] == ring[0]:
                    ring = piece[:-1] + ring
                elif piece[0] == ring[0]:
                    ring = piece[:0:-1] + ring
                else:
                    continue
                pieces.pop(i)
                break
            else:
                break  # an open ring, where the mapping is cut: closed straight, as a fill does
        rings.append(ring)
    return rings


def city_polygons(refs, box, tolerance, local):
    out = []
    for ref in refs:
        rings, _ = osm_shapes(ref)
        for ring in rings:
            if len(ring) < 3 or not intersects(bbox_of(ring), box):
                continue
            clipped = clip_polygon(ring, box)
            simple = simplify(clipped, tolerance, local, closed=True) if len(clipped) >= 3 else []
            if len(simple) >= 3:
                out.append(simple)
    return out


def city_lines(refs, box, tolerance, local):
    out = []
    for ref in refs:
        _, lines = osm_shapes(ref)
        for line in lines:
            if len(line) < 2 or not intersects(bbox_of(line), box):
                continue
            for piece in clip_line(line, box):
                simple = simplify(piece, tolerance, local)
                if len(simple) >= 2:
                    out.append(simple)
    return out


def build_walk(walk):
    path = walk_line(walk)
    along = cumulative(path)
    length = along[-1]
    stops = []
    for index, stop in enumerate(walk.stops):
        start = stops[-1][1] if stops else 0.0
        candidates = [i for i in range(len(path)) if along[i] >= start]
        if index == len(walk.stops) - 1:
            i = len(path) - 1
        elif index == 0:
            i = 0
        else:
            i = min(candidates, key=lambda j: haversine(path[j], (stop.lat, stop.lon)))
        off = haversine(path[i], (stop.lat, stop.lon))
        if off > WALK_MAX_SNAP_METRES:
            sys.exit(f"{walk.id}: {stop.key} is {off:.0f} m from the route")
        if stops and along[i] <= stops[-1][1]:
            sys.exit(f"{walk.id}: {stop.key} comes before {stops[-1][0].key} on the route")
        stops.append((stop, along[i], path[i]))
    frame, ground, local = frame_of(path)
    diagonal = math.hypot((frame[3] - frame[1]) * local.kx, (frame[2] - frame[0]) * local.ky)
    line = simplified_indices(path, diagonal * LINE_TOLERANCE, local)
    rows = [(path[i][0], path[i][1], along[i]) for i in line]
    tolerance = diagonal * MAP_TOLERANCE
    south, west, north, east = ground
    land = [[(south, west), (south, east), (north, east), (north, west)]]
    water = city_polygons(walk.water, ground, tolerance, local)
    canals = city_lines(walk.canals, ground, tolerance, local)
    parks = city_polygons(walk.parks, ground, tolerance, local)
    main_streets, minor_streets = city_streets(walk, tolerance, local) if walk.streets else ([], [])
    locator_box = LOCATORS[walk.country]
    locator_local = Local((locator_box[0] + locator_box[2]) / 2)
    locator_land = polygons("ne_50m_land", locator_box, LOCATOR_TOLERANCE_METRES, locator_local)
    locator_line = simplify(path, LOCATOR_TOLERANCE_METRES, locator_local)
    print(
        f"{walk.id}: {length / 1000:.2f} km, {len(path)} points -> {len(line)}, {len(stops)} places, "
        f"water {sum(map(len, water))}, canals {sum(map(len, canals))}, parks {sum(map(len, parks))}, "
        f"streets {sum(map(len, main_streets))} + {sum(map(len, minor_streets))}",
    )
    places, notes = {}, {}
    for stop, distance, _ in stops:
        print(f"    {stop.key:28s} {distance / 1000:6.2f} km")
        places[stop.key] = (stop.en, stop.it)
        if stop.note_en:
            notes[stop.key] = (stop.note_en, stop.note_it)
    stop_lines = "".join(
        f'            WayStop("{stop.key}", {round(distance)}, {point[0]:.5f}, {point[1]:.5f}, stage = true),\n'
        for stop, distance, point in stops
    )
    block = f"""
    private fun {camel(walk.id)}() = WaySource(
        id = WayId.{walk.id},
        lengthMeters = {round(length)},
        stops = listOf(
{stop_lines}        ),
        frame = {box_literal(frame)},
        line = {kotlin_string(encode(rows, (1e5, 1e5, 1)), 8, len('line = '))},
        land = {kotlin_paths(land, 8)},
        lakes = {kotlin_paths(water, 8)},
        rivers = {kotlin_paths(canals, 8)},
        borders = emptyList(),
        locatorFrame = {box_literal(locator_box)},
        locatorLand = {kotlin_paths(locator_land, 8)},
        locatorLine = {kotlin_string(encode(locator_line, (1e5, 1e5)), 8, len('locatorLine = '))},
        parks = {kotlin_paths(parks, 8)},
        riverWidthMeters = {walk.canal_width},{streets_block(main_streets, minor_streets)}
    )
"""
    return block, places, notes


def city_streets(walk, tolerance, local):
    """The walk's main and minor streets, from its tiles: each OpenStreetMap way once, joined
    end to end where they meet, cut to the street box and simplified like the rest of the map."""
    ways = {}
    for _, name in street_tiles(walk):
        ways.update(json.loads((CACHE / name).read_text()))
    main, minor, lanes = [], [], []
    for kind, points in ways.values():
        if kind in MAIN_STREETS:
            main.append([tuple(p) for p in points])
        elif kind in MINOR_STREETS:
            minor.append([tuple(p) for p in points])
        elif kind in walk.more_streets:
            # Without a minimum of their own, they are joined with the minor streets.
            (lanes if walk.more_streets_min_metres else minor).append([tuple(p) for p in points])
    box = street_box(walk)
    lane_min = walk.more_streets_min_metres or MIN_STREET_METRES
    out = []
    for groups in ([(main, MIN_STREET_METRES)], [(minor, MIN_STREET_METRES), (lanes, lane_min)]):
        pieces = []
        for lines, shortest in groups:
            for line in join_lines(lines):
                for piece in clip_line(line, box):
                    # A stray piece (a crossing, a bit of a square) reads as noise, not as a street.
                    if cumulative(piece)[-1] < shortest:
                        continue
                    simple = simplify(piece, tolerance, local)
                    if len(simple) >= 2:
                        pieces.append(simple)
        out.append(pieces)
    return out


def join_lines(lines):
    """Lines that share an end, joined into one: fewer, longer paths to encode and to draw."""
    lines = [list(line) for line in lines]
    by_end = {}
    for i, line in enumerate(lines):
        by_end.setdefault(line[0], set()).add(i)
        by_end.setdefault(line[-1], set()).add(i)
    alive = set(range(len(lines)))
    out = []
    while alive:
        i = alive.pop()
        line = lines[i]
        for end in (-1, 0):
            while True:
                point = line[end]
                others = [j for j in by_end.get(point, ()) if j in alive]
                if not others:
                    break
                j = others[0]
                alive.discard(j)
                other = lines[j]
                if end == -1:
                    line = line + (other[1:] if other[0] == point else other[-2::-1])
                else:
                    line = (other[:-1] if other[-1] == point else other[:0:-1]) + line
        out.append(line)
    return out


def streets_block(main, minor):
    if not main and not minor:
        return ""
    return (
        f"\n        mainStreets = {kotlin_paths(main, 8)},"
        f"\n        streets = {kotlin_paths(minor, 8)},"
    )


def camel(constant):
    head, *tail = constant.lower().split("_")
    return head + "".join(part.capitalize() for part in tail)


GENERATED = "Generated by tools/build_ways.py from tools/ways_content.py: do not edit, run it again."


def write_domain(blocks):
    cases = "".join(f"        WayId.{way.id} -> {camel(way.id)}()\n" for way in WAYS + WALKS)
    DOMAIN_OUT.parent.mkdir(parents=True, exist_ok=True)
    DOMAIN_OUT.write_text(f"""// {GENERATED}
//
// The ways' and the walks' lines, and the cities' water and parks: © OpenStreetMap
// contributors, under the Open Database License 1.0 (https://opendatacommons.org/licenses/odbl/1-0/);
// this derived data is available under the same licence. The walks were routed by BRouter. The
// ways' land, lakes, rivers and borders, and every locator: Natural Earth, public domain.
//
// Every path is an encoded polyline (latitude and longitude at 1e-5 degrees; the line also
// carries the metres along the way at each point), decoded by [Polyline].
package com.callbackdev.passo.core.domain.ways

import com.callbackdev.passo.core.model.WayId

internal object WayData {{
    fun source(id: WayId): WaySource = when (id) {{
{cases}    }}
{"".join(blocks)}}}
""")


def xml_text(text):
    # The app's strings write the typographic apostrophe, as every other string of Passo does.
    return (
        text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\\", "\\\\").replace("'", "’").replace('"', '\\"')
    )


def write_strings(places, notes):
    for path, index, language in ((STRINGS_EN, 0, "English"), (STRINGS_IT, 1, "Italian")):
        lines = [
            '<?xml version="1.0" encoding="utf-8"?>',
            f"<!-- {GENERATED} -->",
            f"<!-- The Ways' names, routes and places in {language}. -->",
            "<resources>",
        ]
        for way in WAYS:
            names = (way.name_en, way.name_it)
            routes = (way.route_en, way.route_it)
            lines.append(f'    <string name="way_name_{way.id.lower()}">{xml_text(names[index])}</string>')
            lines.append(f'    <string name="way_route_{way.id.lower()}">{xml_text(routes[index])}</string>')
        for walk in WALKS:
            cities = (walk.city_en, walk.city_it)
            routes = (walk.route_en, walk.route_it)
            outings = (walk.outing_en, walk.outing_it)
            lines.append(f'    <string name="way_name_{walk.id.lower()}">{xml_text(cities[index])}</string>')
            lines.append(f'    <string name="way_route_{walk.id.lower()}">{xml_text(routes[index])}</string>')
            lines.append(f'    <string name="way_outing_{walk.id.lower()}">{xml_text(outings[index])}</string>')
        for key, name in sorted(places.items()):
            lines.append(f'    <string name="way_place_{key}">{xml_text(name[index])}</string>')
        for key, note in sorted(notes.items()):
            lines.append(f'    <string name="way_note_{key}">{xml_text(note[index])}</string>')
        lines.append("</resources>")
        path.write_text("\n".join(lines) + "\n")
    names = "".join(f'    "{key}" -> R.string.way_place_{key}\n' for key in sorted(places))
    note_cases = "".join(f'    "{key}" -> R.string.way_note_{key}\n' for key in sorted(notes))
    way_names = "".join(f"    WayId.{way.id} -> R.string.way_name_{way.id.lower()}\n" for way in WAYS + WALKS)
    way_routes = "".join(f"    WayId.{way.id} -> R.string.way_route_{way.id.lower()}\n" for way in WAYS + WALKS)
    walk_outings = "".join(f"    WayId.{walk.id} -> R.string.way_outing_{walk.id.lower()}\n" for walk in WALKS)
    STRINGS_KT.parent.mkdir(parents=True, exist_ok=True)
    STRINGS_KT.write_text(f"""// {GENERATED}
package com.callbackdev.passo.core.designsystem.ways

import androidx.annotation.StringRes
import com.callbackdev.passo.core.designsystem.R
import com.callbackdev.passo.core.model.WayId

/** A way's name, as the reader knows it. */
@StringRes
fun wayNameRes(id: WayId): Int = when (id) {{
{way_names}}}

/** Where a way runs, in one line. */
@StringRes
fun wayRouteRes(id: WayId): Int = when (id) {{
{way_routes}}}

/** A walk's outing, as History and Today name it; null for a way, which is not an outing. */
@StringRes
fun walkOutingRes(id: WayId): Int? = when (id) {{
{walk_outings}    else -> null
}}

/** A stop's name, from its key in the ways' data. */
@StringRes
fun placeNameRes(key: String): Int = when (key) {{
{names}    else -> error("No name for the stop $key")
}}

/** The one sentence said of a stop, if it has one. */
@StringRes
fun placeNoteRes(key: String): Int? = when (key) {{
{note_cases}    else -> null
}}
""")


if __name__ == "__main__":
    if sys.argv[1:] == ["fetch"]:
        fetch()
    elif not sys.argv[1:]:
        build()
    else:
        sys.exit(__doc__)
