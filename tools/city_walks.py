#!/usr/bin/env python3
"""Helpers for adding a city walk (docs/adr/0015-city-walks.md; the procedure is
tools/CITY_WALKS.md). They find and check what goes into tools/ways_content.py; the app's files
are still written by tools/build_ways.py, never by hand.

    python3 tools/city_walks.py locate "Coit Tower, San Francisco" ...
        Nominatim's best match for each query: its coordinates, OSM id and name.
    python3 tools/city_walks.py route name:LAT,LON name:LAT,LON ...
        BRouter's walking route through the places in order: its length, and where each place
        falls along it and how far off the line. For trying an order or a length before writing
        the walk; nothing is saved.
    python3 tools/city_walks.py features LAT,LON [RADIUS_M] ...
        The water areas, rivers, canals and parks OpenStreetMap maps around each point, with
        their ids ("way/123", "relation/456"), for a walk's water, canals and parks.
    python3 tools/city_walks.py wiki LANG "Title" [PATTERN ...]
        A Wikipedia article's text (redirects followed; links and references stripped), cached in
        tools/ways-cache/wiki/. With patterns, the sentences matching each (case-insensitive
        regexes); without, the path of the cached text, for reading it whole.
    python3 tools/city_walks.py wiki-find LANG "query" ...
        Wikipedia's article for a query, and the same article's title in en, es, fr, it, pt and
        de, so a place can be checked in a second language.
    python3 tools/city_walks.py splice WALK_ID ...
        Adds new walks to WayData.kt without rebuilding the others, then rewrites the place
        strings and the continents' maps (see below).

Why splice: a full `build_ways.py` run rebuilds every walk from the cache, and a fresh cache
would also move every other city's streets with OpenStreetMap's edits since, which is not part
of adding a city. `splice` builds only the walks named and puts each block and its case where a
full run would write them (after the walk before it in WALKS). To redo it after changing the
content, restore WayData.kt first (`git checkout` it) and splice again.

The servers are shared and some throttle (Wikipedia answers 429 when asked too fast; Nominatim
allows one request a second): every call here waits between requests, retries with a growing
pause, and never retries a 404. Only the standard library is used.
"""

import json
import math
import re
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ElementTree
from pathlib import Path

HERE = Path(__file__).resolve().parent
WIKI_CACHE = HERE / "ways-cache" / "wiki"
AGENT = {"User-Agent": "passo-build-ways (github.com/fiorenzobrioni/passo)"}
BROUTER = "https://brouter.de/brouter?lonlats={}&profile=hiking-mountain&alternativeidx=0&format=geojson"
NOMINATIM = "https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q={}"
OSM_MAP = "https://api.openstreetmap.org/api/0.6/map?bbox={:.5f},{:.5f},{:.5f},{:.5f}"
LANGUAGES = ("en", "es", "fr", "it", "pt", "de")
# Redirects as each Wikipedia writes them.
REDIRECT = re.compile(r"\s*#(REDIRECT|WEITERLEITUNG|REDIRECIONAMENTO|RINVIA|REDIRECCIÓN)\s*\[\[([^\]]+)\]\]", re.I)


def get(url, attempts=6, pause=8):
    """The body at [url], or None for a 404; other errors are retried, waiting longer each time."""
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(urllib.request.Request(url, headers=AGENT), timeout=120) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            if error.code == 404:
                return None
            print(f"  ({error.code}, waiting {pause * (attempt + 1)} s)", file=sys.stderr)
        except (urllib.error.URLError, TimeoutError) as error:
            print(f"  ({error}, waiting {pause * (attempt + 1)} s)", file=sys.stderr)
        time.sleep(pause * (attempt + 1))
    sys.exit(f"Gave up on {url}")


def metres(a, b):
    la1, lo1, la2, lo2 = map(math.radians, (a[0], a[1], b[0], b[1]))
    h = math.sin((la2 - la1) / 2) ** 2 + math.cos(la1) * math.cos(la2) * math.sin((lo2 - lo1) / 2) ** 2
    return 2 * 6371008.8 * math.asin(math.sqrt(h))


def point(text):
    lat, lon = text.split(",")
    return float(lat), float(lon)


# --- locate -----------------------------------------------------------------------------------

def locate(queries):
    for query in queries:
        found = json.loads(get(NOMINATIM.format(urllib.parse.quote(query))) or b"[]")
        if found:
            hit = found[0]
            print(f"{query}\n   {float(hit['lat']):.4f}, {float(hit['lon']):.4f}  "
                  f"{hit['osm_type']}/{hit['osm_id']}  {hit['display_name'][:100]}")
        else:
            print(f"{query}\n   not found")
        time.sleep(1.1)  # Nominatim's policy: one request a second


# --- route ------------------------------------------------------------------------------------

def route(args):
    places = []
    for arg in args:
        name, at = arg.rsplit(":", 1)
        places.append((name, *point(at)))
    lonlats = "|".join(f"{lon:.6f},{lat:.6f}" for _, lat, lon in places)
    body = get(BROUTER.format(lonlats))
    if body is None:
        sys.exit("BRouter found no route")
    line = [(c[1], c[0]) for c in json.loads(body)["features"][0]["geometry"]["coordinates"]]
    along = [0.0]
    for i in range(1, len(line)):
        along.append(along[-1] + metres(line[i - 1], line[i]))
    print(f"{along[-1] / 1000:.2f} km, {len(places)} places")
    start, previous = 0, 0.0
    for name, lat, lon in places:
        # The nearest point from the previous place on: a route that passes twice keeps its order.
        i = min(range(start, len(line)), key=lambda j: metres(line[j], (lat, lon)))
        print(f"   {name:28s} {along[i] / 1000:6.2f} km  (+{along[i] - previous:5.0f} m)  "
              f"off the line {metres(line[i], (lat, lon)):4.0f} m")
        start, previous = i, along[i]


# --- features ---------------------------------------------------------------------------------

def features(args):
    radius = float(args[-1]) if len(args) > 1 and "," not in args[-1] else 130.0
    for arg in [a for a in args if "," in a]:
        lat, lon = point(arg)
        d_lat = radius / 111_320
        d_lon = radius / (111_320 * math.cos(math.radians(lat)))
        body = get(OSM_MAP.format(lon - d_lon, lat - d_lat, lon + d_lon, lat + d_lat))
        found = set()
        for element in ElementTree.fromstring(body or b"<osm/>"):
            if element.tag not in ("way", "relation"):
                continue
            tags = {c.get("k"): c.get("v") for c in element if c.tag == "tag"}
            nodes = [c.get("ref") for c in element if c.tag == "nd"]
            closed = element.tag == "relation" or (len(nodes) > 2 and nodes[0] == nodes[-1])
            if tags.get("natural") == "water" or tags.get("waterway") == "riverbank":
                kind = "water" if closed else None
            elif tags.get("leisure") in ("park", "garden") or tags.get("landuse") in ("forest", "grass"):
                kind = "park" if closed else None
            elif tags.get("natural") == "wood":
                kind = "park (wood)" if closed else None
            elif tags.get("waterway") in ("river", "canal"):
                kind = f"line: {tags['waterway']}"
            elif tags.get("natural") == "coastline":
                kind = "coastline"
            else:
                kind = None
            if kind:
                found.add(f"   {kind:12s} {element.tag}/{element.get('id'):12s} {tags.get('name', '')}")
        print(f"{lat:.4f},{lon:.4f} (within {radius:.0f} m)")
        print("\n".join(sorted(found)) or "   nothing")
        time.sleep(1)


# --- wiki -------------------------------------------------------------------------------------

def wiki_text(lang, title):
    """The article's wikitext, links and references stripped, from the cache or fetched."""
    WIKI_CACHE.mkdir(parents=True, exist_ok=True)
    cached = WIKI_CACHE / f"{lang}-{title.replace(' ', '_').replace('/', '_')}.txt"
    if cached.exists():
        return cached
    url_title = title
    for _ in range(3):
        body = get(f"https://{lang}.wikipedia.org/wiki/{urllib.parse.quote(url_title.replace(' ', '_'))}?action=raw")
        time.sleep(2)
        if body is None:
            return None
        text = body.decode()
        redirect = REDIRECT.match(text)
        if not redirect:
            break
        url_title = redirect.group(2)
    text = re.sub(r"<ref[^>]*/>|<ref[^>]*>.*?</ref>", "", text, flags=re.S)
    text = re.sub(r"\[\[(?:[^|\]]*\|)?([^\]]*)\]\]", r"\1", text)
    cached.write_text(text)
    return cached


def wiki(lang, title, patterns):
    path = wiki_text(lang, title)
    if path is None:
        sys.exit(f"No article {lang}:{title}")
    if not patterns:
        print(path)
        return
    sentences = re.split(r"(?<=[.!?])\s+", path.read_text())
    for pattern in patterns:
        hits = [s for s in sentences if re.search(pattern, s, re.I)]
        print(f"[{lang}:{title} /{pattern}/]")
        for hit in hits[:4]:
            print("   " + hit[:300].replace("\n", " "))
        if not hits:
            print("   not found")


def wiki_find(lang, queries):
    for query in queries:
        url = f"https://{lang}.wikipedia.org/w/index.php?search={urllib.parse.quote(query)}&go=Go"
        page = (get(url) or b"").decode()
        # No exact title: the first search result.
        first = re.search(r'mw-search-result-heading"><a href="/wiki/([^"]+)"', page)
        if first:
            time.sleep(2)
            page = (get(f"https://{lang}.wikipedia.org/wiki/{first.group(1)}") or b"").decode()
        title = re.search(r'<h1[^>]*>(?:<[^>]+>)*([^<]+)', page)
        links = {
            m.group(1): urllib.parse.unquote(m.group(2)).replace("_", " ")
            for m in re.finditer(r'href="https://([a-z]+)\.wikipedia\.org/wiki/([^"]+)"[^>]*hreflang', page)
            if m.group(1) in LANGUAGES
        }
        print(f"{lang}:{query} -> {title.group(1).strip() if title else 'not found'}")
        for code in LANGUAGES:
            if code in links:
                print(f"   {code}: {links[code]}")
        time.sleep(3)


# --- splice -----------------------------------------------------------------------------------

def splice(ids):
    sys.path.insert(0, str(HERE))
    import build_ways as build
    import ways_content as content

    known = {walk.id for walk in content.WALKS}
    if set(ids) - known:
        sys.exit(f"No walk {', '.join(sorted(set(ids) - known))} in WALKS")
    path = build.DOMAIN_OUT
    text = path.read_text()
    order = [item.id for item in build.WAYS + build.WALKS]
    for walk in [w for w in content.WALKS if w.id in ids]:
        block, _, _ = build.build_walk(walk)
        case = f"        WayId.{walk.id} -> {build.camel(walk.id)}()\n"
        if case in text:
            sys.exit(f"{walk.id} is already in WayData.kt: git checkout it first")
        before = order[order.index(walk.id) - 1]
        previous = f"        WayId.{before} -> {build.camel(before)}()\n"
        if previous not in text:
            sys.exit(f"{before}, the walk before {walk.id} in WALKS, is not in WayData.kt yet")
        text = text.replace(previous, previous + case, 1)
        start = text.index(f"\n    private fun {build.camel(before)}() = WaySource(")
        end = text.index("\n    )\n", start) + len("\n    )\n")
        text = text[:end] + block + text[end:]
    path.write_text(text)

    places, notes = {}, {}
    for item in build.WAYS + build.WALKS:
        for stop in item.stops:
            places[stop.key] = (stop.en, stop.it)
            if stop.note_en:
                notes[stop.key] = (stop.note_en, stop.note_it)
    build.write_strings(places, notes)
    build.build_continents()


COMMANDS = {
    "locate": lambda args: locate(args),
    "route": lambda args: route(args),
    "features": lambda args: features(args),
    "wiki": lambda args: wiki(args[0], args[1], args[2:]),
    "wiki-find": lambda args: wiki_find(args[0], args[1:]),
    "splice": lambda args: splice(args),
}

if __name__ == "__main__":
    if len(sys.argv) < 3 or sys.argv[1] not in COMMANDS:
        sys.exit(__doc__)
    COMMANDS[sys.argv[1]](sys.argv[2:])
