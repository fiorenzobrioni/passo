# Adding a city walk

How a city walk is added, step by step, as the cities of Europe, the Americas, Asia and Oceania, and Africa were.
The decisions behind it are in `docs/adr/0015-city-walks.md`; this is the procedure. The data is
written by `tools/build_ways.py` from `tools/ways_content.py`, never by hand;
`tools/city_walks.py` helps to find and check what goes into the content, and adds the new
walks to `WayData.kt` (`python3 tools/city_walks.py` alone prints its commands).

Add the mirror init script to every Gradle command in the cloud sandbox:
`--init-script gradle/google-maven-mirror.init.gradle.kts`.

## The owner's rules

- **Two cities at a time** (owner): a session adds two, checks them well, and stops. The first
  session of a new continent sets the continent up and adds its first two.
- **About ten cities a continent**: about seven long walks and three short ones. A long walk is
  8 to 12 km with about 14 places, a short one 4 to 6 km with about 8; the city chooses, and a
  compact centre is never padded (ADR 0015 decision 10). Places every 500 to 800 m or so.
- **Cities only**: no parks, mountain paths or natural sites as walks of their own.
- **Every sentence checked in two sources**: two Wikipedias in different languages, or one
  Wikipedia and an official source (a state agency, the site's own institution). A fact in one
  source only, or where the two disagree (a year, a number, a superlative), is left out or
  rewritten so it says only what both say. A hedge in the sources stays in the sentence ("may
  have been modelled on"). Better a shorter sentence than one fact from one source.
- **The user's e-mail is never sent anywhere**: not in a User-Agent, a URL or a query. The
  helpers' User-Agent names the repository only.
- Propose the cities first and record the proposal in PLANNING.md §11 (long, short, and a few
  reserves in case one does not work out), then add them in the order the owner chooses.

## One city, step by step

1. **The places.** Pick the walk's places in walking order, with the start and the end the
   route's name will say ("From the Ferry Building to the Palace of Fine Arts, by ...").
   `locate "Place, City"` gives each one's coordinates (check them: Nominatim may match a
   namesake; prefer the entrance or the square in front, on a street). `route name:LAT,LON ...`
   gives the walking length and each place's distance along the line: adjust the places or the
   order until the length is in its range and every place is within about 50 m of the line (the
   build fails beyond 150 m, or out of order).
2. **The sentences.** For each place, `wiki-find en "Place"` gives the article and its title in
   the other languages; `wiki LANG "Title" "pattern" ...` prints the sentences matching each
   pattern (the article is cached in `tools/ways-cache/wiki/`). Write the English sentence from
   what both sources say, then the Italian, and a `# Source:` comment under the stop naming both
   articles and anything left out and why. A place with no second source has no sentence: find
   another source, or drop the place and re-route. Keep each sentence short, one breath: it is
   read aloud in the reader's headphones. Add the city and the languages used to the line in
   the `ways_content.py` docstring that lists them.
3. **The map.** `features LAT,LON [RADIUS_M]` around the rivers, lakes, harbours and parks the
   page will show lists their OpenStreetMap ids:
   - `water`: water areas (`natural=water`, riverbanks, a river relation);
   - `canals`: waterways drawn as lines;
   - `parks`: the parks and woods the route passes or that frame it (a few, the larger first).
   A city on the sea, where `features` shows `coastline` near the centre, has `coast=True`: its
   land is cut from the sea by the coastline in its street tiles (ADR 0015 decision 13). A wide
   river or estuary that is a water area, not coastline (Québec's St Lawrence), stays `water`,
   without `coast`. A centre mapped mostly as residential streets (Milan, Buenos Aires, Québec)
   takes `more_streets=("residential", "unclassified", "living_street")` with
   `more_streets_min_metres=400`, or its map is a few lines.
4. **The content.** A `Walk(...)` in `tools/ways_content.py` (copy a city's of the same kind):
   id `CITY_START_END`, the city key, its names and route in English and Italian, the outing's
   name, the country (and a `LOCATORS` box for a new country), the continent, the map's sources,
   the stops. Add it to `WALKS` after its continent's cities, long and short in the order the
   continent's page should list them. Add the same id to `WayId` in
   `core/model/.../Way.kt`, in the same order.
5. **The data.**
   - `python3 tools/build_ways.py fetch ID ...` routes the new walks with BRouter into
     `tools/walks/<id>.geojson` (committed: the route is fetched once; delete the file to
     route again after moving a place) and fetches their OpenStreetMap sources into the cache.
   - `python3 tools/city_walks.py splice ID ...` adds them to `WayData.kt`, rewrites the place
     strings (EN, IT, `WayPlaceStrings.kt`) and the continents' maps. It prints each place's
     distance along the route and the map's counts: check them. To splice again after changing
     the content, `git checkout` `WayData.kt` first.
   - Never run a full `fetch` and a full build to add a city: with a fresh cache every other
     city's streets would move with OpenStreetMap's edits since.
6. **The tests.**
   - `core/domain/.../WaysDataTest.kt`: the lists of cities (all, per continent, the short
     ones, the coastal ones). Places of a coastal city must stand on its land, or within 150 m
     of it (a pier).
   - `feature/ways/.../WaysScreenTest.kt`: the counts said on the Ways page and the continent's
     map ("10 cities"), the cities per continent, and one test per new city that draws its page
     with a snapshot (in the dark for one of the two).
7. **Look at the screenshots** in `feature/ways/build/screenshots/`: the walk's page (the
   route on its streets, water and parks recognisable, the start's and the end's names clear of
   each other and of the points) and the continent's map (every city's name placed). Fix what
   looks wrong in the content (a missing park, sparse streets), never in the generated files.
8. **The docs.** `CHANGELOG.md` (the city walks' entry), the root `README.md` (the cities
   list; no em or en dashes in that file), `PLANNING.md` §11 (tick the city, update the
   continent's list) and §15 (a decision entry: the sources, what was left out and why, any
   map oddity), and ADR 0015 if a rule changed.
9. **The checks**, then commit and push:
   `./gradlew spotlessApply spotlessCheck :app:checkForbiddenPermissions test :app:lintDebug`.

## A new continent

On top of its first two cities:

- `CONTINENTS` in `tools/ways_content.py`: the continent's frame (south, west, north, east),
  wide enough for every city it will hold, so later cities do not move it.
- `Continent` in `core/model/.../Way.kt`, in the order the Ways page lists them (Europe, the
  Americas, Asia and Oceania, Africa).
- Its name in `ways_continent_*` (`feature/ways` strings, EN and IT) and in
  `continentNameRes` (`ContinentScreen.kt`).
- The texts that name the continents: the guide (`guide_ways_walks_body`) and Insights' door
  (`insights_ways_door_body`), in both languages. They never count the cities, so a new city
  does not make them wrong.
- `python3 tools/build_ways.py continents` (or the splice) writes the continent's land
  (Natural Earth) into `ContinentData.kt`; the build fails on a continent with no cities.
- The tests that list the continents and their cities, and a snapshot of the new continent's
  page and map.

## Things learned

- **Wikipedia throttles** (HTTP 429) when asked fast: the helper waits and retries, and caches
  every article; ask for several patterns in one `wiki` call rather than many calls. Wikidata
  throttles too; `wiki-find` reads the language links from the article page instead.
- A redirect is followed in each Wikipedia's own word (`#REDIRECT`, `#RINVIA`,
  `#REDIRECCIÓN`, `#REDIRECIONAMENTO`, `#WEITERLEITUNG`).
- A place on a pier (San Francisco's Ferry Building) stands past the coastline: within 150 m
  is accepted.
- A walk that ends near its start (Cartagena) puts both names close: the map places each name
  clear of the ones placed before it.
- A city near a simplified Natural Earth shore (Rio) may fall just outside the continent's
  land: the continent test allows 30 km.
- An Asian city is best checked in its own language and English (Tokyo: Japanese); `wiki`
  follows the Japanese and Chinese redirects (`#転送`, `#重定向`) and splits their sentences at
  「。」. A redirect cached before that fix holds only the `#転送` line: delete it from the cache.
- A harbour may be a water area rather than coastline (Sydney's, whose coastline runs out at the
  Heads): `features` shows no `coastline` near the centre, so the walk lists the harbour as
  `water` and is not `coast`, as Québec's St Lawrence.
- A river mapped as many small areas (Tokyo's Sumida and its banks): list the larger relations
  and add `water_from_tiles=True` for the rest. Set it before the first `fetch`, or the tiles
  are fetched a second time for their water.
- A stream too narrow to show as an area (Seoul's Cheonggyecheon): list its waterway line in
  `canals`, drawn as wide as `canal_width`.
- A street BRouter will not route (Beijing's Guozijian Street, as mapped): `route` shows its
  places 50 m or more off the line, and the walk takes another street or drops them.
- A market, a stairway or a small square may have no article in any language: then it has no
  sentence, and usually no place on the walk.
- An African city is checked in English and French, Arabic or German (Cairo), or German, Dutch or
  Afrikaans (Cape Town); `wiki-find` lists the Dutch and Arabic titles, and `wiki` follows their
  redirects (`#DOORVERWIJZING`, `#تحويل`). Afrikaans has no place in the listing: ask `wiki af`
  for its title directly.
- A crossing BRouter will not take (Cairo's Al-Muizz Street over Al-Azhar Street) shows as a
  detour of several hundred metres between two places close together: route the leg alone, see
  where it goes, and order the places so the walk takes the way it can.
- A disambiguation page (Green Point Lighthouse) comes back from `wiki` as a short list: ask for
  the full title.
