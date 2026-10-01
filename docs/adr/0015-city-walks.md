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
   Cusco (9.4 km, 12), whose map has parks and no water.
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
   lanes: `more_streets` in the content).
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

## Consequences

- No new permission, no new dependency, no new wake: an outing on a walk is an ordinary outing
  (ADR 0009). The APK grows by a few tens of kilobytes of encoded lines.
- Rome, Paris and Madrid were content only, as foreseen: a `Walk` in `tools/ways_content.py`
  each, its route fetched and committed, its places' sentences checked in two languages; so
  were Lima and Cusco. The set has no limit (owner). A city's second walk would be the same, with
  the second level of the page (decision 1) still to build; a coastal walk (Lima's Costa Verde)
  also needs a sea built from OpenStreetMap's coastline, which the script does not do yet.
- To be checked on a device (owner): Milan in one outing and London over two, the voice through
  headphones and with the screen off, once on a treadmill.
