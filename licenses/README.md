# Third-party licenses

Passo itself is GPL-3.0 (`LICENSE` at the repo root). What ships inside the APK and is not ours:

| What | License | Where |
|---|---|---|
| [Google Sans](https://fonts.google.com/specimen/Google+Sans), the app's default typeface, bundled as `core/designsystem/src/main/res/font/google_sans_variable.ttf`: the upstream `ofl/googlesans` variable font cut down by Chiaro's `tools/import_google_sans.py`, copied from Chiaro so the two apps share one drawing | SIL Open Font License 1.1 | `GoogleSans-OFL.txt` |
| [Inter](https://github.com/rsms/inter), the second typeface, bundled as `core/designsystem/src/main/res/font/inter_variable.ttf` | SIL Open Font License 1.1 | `Inter-OFL.txt` |
| The ways' lines and the city walks (Phase 11): OpenStreetMap route relations; the walks routed over OpenStreetMap with [BRouter](https://brouter.de) (the routes kept in `tools/walks/`), with the cities' water, canals and parks; simplified by `tools/build_ways.py` into `core/domain/.../ways/WayData.kt`, a derived database offered under the same licence | [Open Database License 1.0](https://opendatacommons.org/licenses/odbl/1-0/), © OpenStreetMap contributors | the header of `WayData.kt` |
| The land, lakes, rivers and borders behind the ways (not the cities), and the land of the continents the cities are grouped by (`ContinentData.kt`), from [Natural Earth](https://www.naturalearthdata.com) | Public domain | (none needed) |

The typefaces are credited in Settings → Credits, with the one in use named, and so are the maps
(Credits, the Ways page's foot, the guide), because a licence that asks for credit is not
satisfied by a file the reader never opens. The icons are drawn in the app
(`PassoIcons`), not taken from an icon set.
