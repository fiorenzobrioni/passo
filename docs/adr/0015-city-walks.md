# ADR 0015: City walks (Phase 11, second part)

- Status: accepted
- Date: 2026-10-02

## Context

The owner asked for walks through cities beside the pilgrim ways (PLANNING.md §11 Phase 11, §15):
Milan, Rome, Paris, London and Madrid, Milan and London first; a city may one day have more than
one walk. A way takes months and moves with the days; a city is walked in an afternoon, or a few
outings. As with the ways, there is no location: the reader walks where they are, and the walk
moves with the distance their steps measure.

## Decisions

1. **A walk is a `WayId` of kind `WALK`**, with its city's key. The ways and the walks share the
   data script, the data shape (`WaySource`, `Way`, `WayMap`), the journey table, the map, the
   stamps and the place strings; `Ways.all` stays the ways and `Ways.walks` lists the walks.
   A second walk in a city is data, not a feature: the page shows one row a walk until a city has
   two (not yet).
2. **The routes are drawn once, with BRouter, and committed.** `tools/build_ways.py fetch` asks
   BRouter (the `hiking-mountain` profile, over OpenStreetMap) for a walk through its places in
   order, and writes the trimmed route to `tools/walks/<walk>.geojson`; the build reads only the
   committed file, so a later change in the map or the router never moves a walk silently. Each
   place is snapped to the route and must lie within 150 m of it, in order. Milan is 9.3 km with
   14 places, London 10.7 km with 17 (fewer than the twenty first planned: every place on the
   route, and nothing added to fill a count). Rome, Paris and Madrid followed (2 Oct 2026): 9.6 km
   with 15 places, 10.4 km with 14, 9.6 km with 14; then Lima's historic centre (10.1 km, 15) and
   Cusco (9.4 km, 12), whose map has parks and no water; then the first short walks (decision
   10): Porto (5.3 km, 10), Amsterdam (5.2 km, 9) and Prague (5.2 km, 9); then, with the cities
   grouped by continent (decision 12), Berlin (10.5 km, 13) and Vienna (9.7 km, 13), which
   bring Europe to ten; then, opening Asia and Oceania, Tokyo (11.6 km, 14) and Sydney
   (10.1 km, 14); then Seoul (11.1 km, 12) and Beijing (10.6 km, 15, through the Forbidden City); then Hong Kong
   (11.0 km, 15) and Singapore (8.4 km, 14); then Bangkok (9.8 km, 12) and Kyoto, short
   (4.2 km, 10); then Hanoi (5.9 km, 8) and Melbourne (4.6 km, 9), short, which bring Asia and
   Oceania to ten; then, opening Africa, Cairo (9.9 km, 15) and Cape Town (10.8 km, 15); then Marrakech (8.2 km, 14) and Fez, short
   (4.1 km, 8); then Tunis (8.9 km, 14) and Alexandria (11.8 km, 12); then Dakar (11.6 km, 12) and
   Addis Ababa (11.9 km, 12). `fetch` with walk ids fetches only those walks, so a city is added
   without moving the others with the map's edits since.
3. **The city behind the line is OpenStreetMap too**: the water (the Thames, the Darsena) as
   areas, the canals (the Navigli) as lines drawn as wide as they are (`riverWidthMeters`, with
   `WayProjection.pixelsPerMeter`), the largest parks as areas. Parks are a new colour role,
   `PassoColors.park`, a sage quieter than the goal's green. A city's map has land where a
   country's has sea, and no locator: the line fills its frame and the title names the city.
   *Revised (owner, 2 Oct 2026):* "never a street grid" became the main streets, after a trial on
   Milan the owner judged "recognisable now". The arteries (trunk, primary, secondary) and the
   streets that give a centre its shape (tertiary, pedestrian), no others, no names: a lighter
   line on the land in either theme (Material's lowest container in light, its bright surface in
   dark, no new colour role), above parks and water areas so bridges show, under canals and the
   route, and only on the walk's own page (a thumbnail or the outing's card keeps its line
   alone). The script fetches them with the OpenStreetMap API's map call, tile by tile over what
   the page can show (Overpass is not reachable from the build machines), keeps only the streets
   in its cache, joins them end to end, drops pieces under 150 m (crossings and bits of squares
   read as noise) and simplifies them like the rest of the map: a few tens of kilobytes a city. A city
   mapped mostly in smaller classes adds them as minor streets (Cusco's old centre is residential
   lanes: `more_streets` in the content). Milan's and Rome's centres are mapped the same way
   (Milan's has almost no tertiary street), so their maps came out half as full as London's or
   Paris's (owner, 2 Oct 2026); they add the residential lanes too, but only those at least
   400 m long once joined (`more_streets_min_metres`): every lane would make them busier than
   any other city and drown the arteries. The cache keeps those classes for every city, so
   choosing them needs no new fetch. Prague's Old Town is mapped the same way and adds them too.
   Amsterdam's canals are hundreds of water areas, cut at every bridge: too many to list by id,
   so its walk reads the water areas of the street tiles it already fetches (`water_from_tiles`),
   beside the IJ and the docks listed as usual; it adds no lanes, which along its canals are
   the quays and would draw every canal twice.
4. **A walk is walked in outings.** An outing on a walk (`session.walk`, `session.walkFromMeters`,
   schema v5 by auto-migration) has the distance left as its goal, no quarter signals, and the
   walk's places as its signals. The places are told from the outing's stored totals (where it
   began on the walk plus the distance it has measured), so a process restarted mid-walk tells
   nothing twice; several passed in one batch are named in order, the last with its sentence. One
   short pulse a place (none at the end, where the goal's long one says it), and, with the voice,
   the place's name, its sentence and the next place. The counting notification's sentence and
   the card's are the place ahead, «Next: the Duomo, 600 m», distances under a kilometre in tens
   of metres (`MeasureFormatter.aheadDistance`).
5. **A walk's progress is its outings', computed on read** (`WalkDays`): the outings on it since
   its journey began, by day. The next outing continues from there ("Continue from Sforza
   Castle"); starting again asks, and puts the journey down (its outings stay in History). A walk
   walked to its end is finished by the tracking service at the goal, and stays in Your ways; the
   next outing on it begins a new journey. One walk journey at a time per walk, and any number of
   walks beside the way under way: they never block each other.
6. **The voice is the walks' own setting** (`UserSettings.walkVoice`, headphones by default, like
   a new outing's), chosen on the walk's page and kept for the next one; a walk is not a plan.
   Under it, as in the outing editor, "Hear it" says the next place as it will be told
   (`spokenWalkSample`), "Change voice" opens the system's voices, and a phone with no offline
   voice is told so. One sample, not a touch on every place: the places' sentences are on the
   page to read, and a list that speaks when touched would surprise more than it helps.
7. **Where it shows.** The Ways page has the ways, then the cities (each walk with where it
   stands), then Your ways; the walk's page, its map and, during an outing, the outing's card;
   Today's and the Outings page's card carry the walk's small map instead of the bar (drawn only
   while the card is on screen); the Outings page has a door to the cities; History names the
   outing by its walk («A walk in London»).
8. **Your ways can be tidied** (owner, 2 Oct 2026): a way finished or left, or a walk walked to
   its end, can be deleted from its page, after a dialog that says it is for good; only the
   journey goes, the days and the outings stay in History. A way left before any of it was
   walked is not kept at all (it would only be an empty row). An import adds and never takes
   away, so a backup written before the delete brings the journey back.
9. **The backup carries both fields** on an outing and the setting, as added fields with no new
   format version. One journey under way per walk on import, as one per way slot.
10. **Two lengths: about 5 km and about 10 km** (owner, 2 Oct 2026). The first walks were all
    between 8 and 12 km: about 13,000 steps, past the default goal of 8,000, so a reader new to
    walking met only walks of two outings. A short walk is 4 to 6 km, about 7,000 steps at 5 km,
    walked in an hour or so and stamped the same day; a long one is 8 to 12 km. The city chooses:
    the length is its centre's own, and a compact centre is never padded to 10 km with places
    added to fill it. Places come every 500 to 800 m or so, about 8 on a short walk and about 14
    on a long one. Nothing on screen tells the two apart: each row already says its length and
    its places, and the walk's page its steps; a badge or a filter would be a feature to explain
    for a difference the numbers already show. No third length of 20 km: about four and a half
    hours at 4.5 km/h, past an outing's four-hour limit, it would always be walked in parts, a
    way in miniature on a map too wide for its places; it waits for a walk that asks for it.
    Ten cities is the ceiling of one flat list: past it, the cities need grouping, or the
    city's second level first.
11. **A walk begun can be left** (owner, 2 Oct 2026: a walk once started had no way out but
    walking it to its end, or Start again, which begins another outing at once). «Leave this
    walk» sits where a way's Leave does, after the places, only on a walk begun, not at its
    end and with no outing on it (during one, End comes first); it asks first. The journey is
    put down as Start again puts it down (left, or deleted when under a metre of it was
    walked), so the walk shows as not begun, and the outings stay in History, as the dialog
    says. A walk left is not kept in Your ways, unlike a way left (decision 5's rule stands):
    a way is months, and a credential half stamped is worth keeping; a walk is an afternoon,
    and its outings already keep the record.
12. **The cities by continent** (owner, 5 Oct 2026, from a proposal). Ten cities was the
    ceiling of one flat list (decision 10), and the owner plans walks around the world. The
    Ways page has a row a continent, in the cities' place: its small map with the cities as
    points, how many cities and how many walked, the ones under way by name. Each opens the
    continent's page: its map, then its cities, each row as the Ways page listed it before. A
    row a continent rather than a selector over one list: a selector would hide a walk under way
    on another continent and remember a choice, where the rows say every continent's state at
    once and stay four rows at most. The page has two levels, never three: a city's own second
    level (decision 1) still waits for a second walk.
    A continent is the walk's `WayId.continent` (`Continent`, never stored), its frame in
    `tools/ways_content.py` (`CONTINENTS`), and its land Natural Earth's 1:50m, cut to a square
    around the frame and simplified to under a pixel of the page's map
    (`ContinentData.kt`, about 2,300 points a continent, written by the script; `continents`
    writes only it). The build fails on a continent without a walk (no empty group: one is
    added with its first city) and on a walk whose route leaves its continent's frame.
    The map is `ContinentMapView`, drawn like a way's: the land cut out of the sea, a city a
    point where its walk begins, a ring before it is begun, a bead in the goal's colour once
    walked, the reader's point with its halo while under way. The names go where they find room,
    the reader's cities first (they may cover another city's point at a large text size), the
    rest dropped when crowded; a touch names the nearest. One image to TalkBack, in a sentence:
    "Europe on the map: 8 cities, 1 under way, 1 walked."
    Planned (owner): four continents of about ten cities, about seven of 10 km and three of
    5 km, the city still choosing its length (decision 10). Proposed, for the owner to confirm:
    Europe and the Americas now, then Asia and Oceania (begun with Tokyo and Sydney, 6 Oct 2026,
    its frame from Mumbai to New Zealand), and Africa (begun with Cairo and Cape Town, 6 Oct
    2026, its frame the whole continent). Cities only, for now: a famous park belongs to its city's walk
    (the Retiro is Madrid's), while a mountain path or a natural site would need a map of terrain
    the script does not draw, and would blur what a city walk is. The ways are not grouped: five,
    all in Europe, read best as one list. Many of the cities planned are on the sea (Barcelona,
    Lisbon, Istanbul, New York, Rio, Sydney, Cape Town), so the coastline (decision 13) comes
    before them.
13. **A city on the sea is cut out of it** (owner, 5 Oct 2026: "start with Rio and New York, so
    the script begins to handle coasts"). A walk marked `coast` keeps, from the same street
    tiles, OpenStreetMap's coastline ways; the script joins them end to end, cuts them to the
    tiles' box and closes each piece along the box's edge, counterclockwise, since the
    coastline always has the land on its left; a ring inside the box is an island. That land
    replaces the city's plain ground, and the map's ground is water (`WaySource.sea`), as a
    way's map is cut out of the sea. Natural Earth would not do: at a city's scale its coast is
    hundreds of metres off. The tiles of a city on the sea cover a square around what its page
    shows, so a square thumbnail finds the coast to its edges too. The build fails on a
    coastline that ends inside the map (a tile missing) or on a city marked `coast` with none.
    New York's Hudson and East River are coastline in OpenStreetMap, so Manhattan stands
    between its two rivers with nothing listed; Rio's bay is the Guanabara's, and the
    Sugarloaf and Urca hills, a protected natural monument, are drawn as a park, so the walk's
    end shows on the map.

## Consequences

- No new permission, no new dependency, no new wake: an outing on a walk is an ordinary outing
  (ADR 0009). The APK grows by a few tens of kilobytes of encoded lines.
- Rome, Paris and Madrid were content only, as foreseen: a `Walk` in `tools/ways_content.py`
  each, its route fetched and committed, its places' sentences checked in two languages; so
  were Lima and Cusco, and Porto, Amsterdam and Prague (with the script's one addition,
  Amsterdam's water from the tiles). The set has no limit (owner). A city's second walk would be the same, with
  the second level of the page (decision 1) still to build. A coastal walk is content too, since
  decision 13: `coast=True`, and the sea is built from the coastline in its tiles.
- To be checked on a device (owner): Milan in one outing and London over two, the voice through
  headphones and with the screen off, once on a treadmill.
