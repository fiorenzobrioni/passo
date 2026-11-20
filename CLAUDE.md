# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in
this repository.

## What this repository is

**Passo** ("step") is a private, battery-friendly Android pedometer (Kotlin / Jetpack Compose
Material 3, Glance widget). It counts every step with the phone's own hardware step counter,
even if the app is never opened, keeps the data on the device, and derives everything else
from those steps: distance, active calories, active and brisk minutes, goals, streaks, walks,
trends. No account, no ads, no internet permission at all. It belongs to a small family of
single-purpose apps by callbackdev, with **Chiaro** (weather), **Tempo** (clock and agenda),
**Saldo** (finance) and **Tetto** (budget): same developer identity, same build and release
setup, and the same visual language.

Source of truth:

- `VISION.md`: the product. Principles, v1.0 scope, non-goals, key decisions, success criteria,
  glossary.
- `PLANNING.md`: the live plan. How it is made, the state, what is open, the architecture,
  the tracking engine design (§4), the data model (§5), the metrics (§6), widget (§7), battery
  rules (§9), permissions (§10), the phases not done yet with checkable steps (§11), and the log
  of the current period (§15). **Keep it updated as work progresses**, recording every decision
  and deviation with its reason (and as an ADR in `docs/adr/` when it is architectural). Since
  2 Nov 2026 it is split, as the family's other plans are:
  - **Write only in `PLANNING.md`**: a new entry at the end of §15's log, dated, with who asked.
    A box opened or ticked goes in its «Open» list (a phase's own boxes stay in its section).
  - **`docs/planning/NN-<period>.md` is the archive**, the phases done and the log's past
    periods, **frozen**: never edit it. A decision that changes an archived one is written in
    `PLANNING.md`, naming the archived file and the entry it changes.
  - **To find a section** cited by name in the code («§11 Phase 10», «§15 "Phase 11's device
    checks done"»), grep `PLANNING.md` first, then `docs/planning/README.md`, the index of every
    archived title and log entry with its file and line, and read from that line. Never load an
    archive file whole.
  - **Archive** when §15's log passes ~1,000 lines or at a release, whichever first, as
    `PLANNING.md` «How it is made» says, then run `python3 tools/planning_index.py` (`--check`
    tells if the index is behind).

Work one phase at a time, starting from the phase's section in PLANNING.md §11. Phases 15 to 20
(location, `docs/adr/0016-location.md`) each name what to read first and hold «The bar» their
work is measured against: read the ADR and the phase's section before planning. **Phases 15
and 16 are built** (18 Nov 2026), on branches in cascade, and wait for a phone: what was built
and decided is in their boxes and in archive 04 (`docs/planning/README.md`, from «Location, for
an outing that asks for it» on); their proofs on a phone (the boxes in «Open») come before any
release. **Phase 17 is built** (19 Nov 2026), on the branch of PR #74 at the owner's choice, and
waits for a phone too (PLANNING.md §15, «Phase 17 built: the step that learns»). **Phase 21, the
open outing** (planned 20 Nov 2026: an outing with no goal, in three steps, a session each;
PLANNING.md §11 Phase 21 and §15 «The open outing planned»): **step 21.1 is built** (20 Nov 2026,
waits for a phone; §15 «Phase 21.1 built: the open outing»), **21.2 is next**, then 21.3, then 23,
the doors (the ways in that were missing, and a list of the outings), then 18, then 20, which
builds on 21; Phase 22, the walk already begun, later. On 19 Nov 2026 the owner dropped Phases
18 and 19 «On the spot»:
Phase 18 is now a city walk measured with GPS (small), Phase 19 is dropped, and Phase 20 is the
usual round alone: no route is followed, no GPX imported (PLANNING.md §15, «The places on the spot
dropped, the city walk measured» and «Phase 20 reviewed: the usual round alone»).

## Build and commands

Stack: Kotlin 2.4 (compiled by AGP 9's built-in Kotlin), Compose Material 3, Hilt, Room,
DataStore, Glance; Gradle 9.8 / AGP 9.4, version catalog in `gradle/libs.versions.toml`,
convention plugins in `build-logic/`. Package/applicationId: `com.callbackdev.passo`.
minSdk 34, compile/targetSdk 37. Java 21.

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- All unit tests: `./gradlew test`
- One module: `./gradlew :core:domain:test`; one class: `--tests "com.callbackdev.passo.core.domain.tracking.StepAccountantTest"`
- Lint (every module, via `checkDependencies`): `./gradlew :app:lintDebug`
- Format: `./gradlew spotlessApply` (CI runs `spotlessCheck`; rules in `.editorconfig`)
- Permission check on the merged manifests (an allowlist): `./gradlew :app:checkForbiddenPermissions`
- Installable minified build: `./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`
- On a machine with no system JDK, prepend `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- **Maven Central answers HTTP 429** (Too Many Requests; it happens in the Claude Code cloud
  sandbox): add `--init-script gradle/google-maven-mirror.init.gradle.kts` to any command. It
  puts Google's mirror of Maven Central first, and points Robolectric's own `android-all`
  download at it, for that run only; nothing in the build changes.

**Convention plugins** (`build-logic/convention`): `passo.android.application`,
`passo.android.library`, `passo.android.compose`, `passo.android.feature`, `passo.android.hilt`,
`passo.android.room`, `passo.jvm.library`. SDK levels and the Java version live in
`PassoSdk` / `Passo.kt`; a module's own build file only says what is specific to it. The
permissions Passo may hold are `AllowedPermissions` in `ForbiddenPermissions.kt`, and the ones it
never may `ForbiddenPermissionPatterns`, beside it; the gate's rules have their own tests there,
which `./gradlew test` runs.

**Version**: `passo.versionName` in `gradle.properties` is the only place it is written;
`versionCode` is derived (major * 10000 + minor * 100 + patch). `release.yml` refuses a tag
that does not match it.

## Modules (PLANNING.md §2)

| Module | Kind | Holds |
|---|---|---|
| `:core:model` | pure Kotlin/JVM | data classes shared by everything (a route's points, an outing's legs and its pauses, and a learnt step among them) |
| `:core:domain` | pure Kotlin/JVM | `StepAccountant`, metric calculators, `WalkDetector`, `TypicalDayCalculator`, `PeriodOverview`, `Insights` (streaks, records, averages), `SessionTracker` and `SessionPlans` (outings), `IntervalSchedule` (the interval walk), `YearInReview` (Your year on foot), the backup file and its merge (`BackupCodec`, `BackupMerge`, `CsvExport`), `StepCalibration`, the step that learns (`StepLengthLearner`, `LearnedSteps`, `LearnedStepText`), the Ways and the city walks (`Ways`, `WayProgress`, `WalkDays`, `WalkPlaces`, `WayForecast`, `WayAnnouncement`, `WayProjection`, `Continents`, the generated `WayData` and `ContinentData`), the measured outing (`routes/`: `RouteMeter`, the fusion of steps and fixes, `RouteFilter`, `RouteCodec`, `GpsDuty`, `GpsStart`, `Pace`; `GpsLines`, the card's line for each state of the receiver; `Altitude`, `Climb` and `RouteHeights`, the climb; `Legs` and `Splits`, the kilometres; `GpxCodec`; `RouteShape` and `ScaleBar`, what a route's map draws, on plain ground) |
| `:core:data` | Android library | Room (steps, tracker state, outings, ways), DataStore (settings, profile), repositories exposing `Flow`, `BackupRepository` (export, import), `WayRepository`, `StepLearningRepository` (what the outings with GPS taught of the step, and the reader's answers); a second Room database, `routes.db` (`RouteDatabase`, `RouteRepository`), where the outings measured with GPS went, and which no read ever makes (`RouteFile`) |
| `:core:tracking` | Android library | `StepTrackingService` (FGS type `health`), sensor source, receivers, ongoing notification, `StepCounterProbe` (the calibration's direct read), `WayNotifier` (a stage reached), `LocationSource` (the satellite receiver, for an outing that asks for it), `PressureSource` (the barometer, on only with that receiver, for the climb), `OutingStarter` (the one door every start of a plan goes through) |
| `:core:designsystem` | Android library | M3 theme, typography, shared components, the Canvas charts (`DayTrendChart`, `BarChart`), `CalendarHeatmap`, `WalkList` and `OutingList`, `SessionCard`, the ways' and the continents' maps and stamps (`WayMapView`, `ContinentMapView`, `WayStamp`) and place names, a route's map, the one that moves (`RouteMapView`, `MapViewport`), `SplitList` and `ClimbProfile`, `StepCurveChart` (a learnt step against the step in use), the one file provider for what is shared (`SharedFiles`, `ShareImage`), date formatting |
| `:core:testing` | Android library, test-only | `assertAccessible()` (labels, 48dp touch targets) and `walkPage()`, shared by the UI tests as `testImplementation` |
| `:feature:*` | Android library | `today`, `history`, `insights`, `settings` (with your data and the step calibration), `onboarding`, `sessions` (the Outings page and editor, and an outing's own page, `OutingScreen`), `ways` (the Ways, each continent's cities, each way's page and each city walk's), `guide` (the guide, in Chiaro's shape), `year` (Your year on foot: the year's pages, shared as an image) |
| `:widget` | Android library | the two Glance widgets («At a glance», «In words»), their settings screen, the update coordinator |
| `:app` | application | `Application`, `MainActivity`, navigation (the bottom bar: Today, History, Insights), DI entry points; wires everything |

Rules: `:core:model` and `:core:domain` stay pure Kotlin/JVM; if a class there needs a `Context`
or a `Resources`, it is in the wrong module. Feature modules depend on `core:*`, never on each
other. All business logic lives in `:core:domain`, with unit tests.

## Invariants (non-negotiable)

- **The permissions are an allowlist** (PLANNING.md §10): the merged manifest must request
  exactly the permissions written in `AllowedPermissions`; the build fails on one more, and on
  one fewer. **Never, by name**: `INTERNET`, `ACCESS_BACKGROUND_LOCATION`,
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`,
  `HIGH_SAMPLING_RATE_SENSORS`, `BODY_SENSORS*`. A library that adds a permission is removed or
  patched with `tools:node="remove"`; a permission added to the list is the owner's decision,
  written in §10 first.
- **Location is a measuring tape, never a tracker** (`docs/adr/0016-location.md`; owner,
  18 Nov 2026). `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` and the service's `location`
  type are held for one thing: an outing the reader started with «Measure with GPS» on, from
  its start to its pause or end, and for at most 15 minutes after an end that can still be
  taken back. Never for the day's count, never with no outing, never in the background, never
  by a timer or a geofence. Satellites only, through the platform's `LocationManager`: no
  Google Play services, no fused provider. Asked in context, the first time such an outing
  starts; never in the first run. A way from home never reads them, nor does a city walk
  unless its page's «Measure with GPS» is on (Phase 18, as re-planned on 19 Nov 2026): then it
  is an outing with GPS, measured and its route kept, and the city's map shows the reader's
  point on the walk by the distance walked, never their real position. Nothing in the app
  depends on it: with the switch off, every screen, number and wake is as before.
  Say "Passo sends nothing" and "cannot send"; never "Passo cannot know where the phone is".
- **The day stays the steps'.** A day's distance, History, Insights, the ways from home and
  Your year on foot are computed from the steps, with or without outings measured with GPS.
  Only an outing shows a measure, and «measured» is a word for a distance at least nine
  tenths from fixes.
- **The hardware step counter is the only step source.** No Health Connect, Google Fit,
  Samsung Health or Play services for step data; no accelerometer.
- **Battery first** (PLANNING.md §9): never poll or run timers while the screen is off; no
  wakelocks (except inside `goAsync()` for the shutdown flush), no exact alarms, no periodic
  workers for tracking; database writes are batched. Widget, notification and tile update
  only when someone can see them. Two exceptions, both for an outing the reader started, while
  it counts. The first: the wake-up step counter at 30 s, and at 2 s in the 40 s before a change
  of an interval walk; on a phone with no wake-up counter, a partial wake lock instead, for an
  outing with signals or with GPS, bounded ten minutes past the outing's own cap and let go
  after 15 minutes still by the one timer that runs while it is held (`SignalWake`;
  `docs/adr/0009-sessions.md`, `docs/adr/0013-interval-walks.md`). The second: the satellite
  receiver of an outing with GPS, on while the reader walks, asleep 2 minutes after they stop,
  off at a pause and the end (`GpsDuty`; `docs/adr/0016-location.md`, decisions 5, 10 and 14).
  Never widen either.
- **No lost steps.** Don't change the tracking engine without updating the tests for every
  edge case in PLANNING.md §4.6.
- **Honest estimates.** Distance, calories and active time are shown as estimates; formulas
  live in `MetricsConstants.kt` with their sources.
- **Past days are frozen.** A profile change never silently rewrites history. Every day is
  measured against its own stored goal (streaks, records, the goal line).
- **Backup is an allowlist** (`app/src/main/res/xml/data_extraction_rules.xml`,
  `docs/adr/0007-backup.md`): the database and the settings file, nothing else; `routes.db`,
  where the outings measured with GPS went, only in the phone-to-phone transfer, never in the
  cloud copy. A route is deleted before its outing; no place outlives its outing. Android sends
  the copy, so the no-`INTERNET` rule stands; say "Passo sends nothing", never "nothing leaves
  the phone". A tracker state from another installation is never trusted
  (`TrackingRepository.adoptTrackerState`).
- **Every user-facing string is a resource, in English and Italian** (`values/`,
  `values-it/`), with plurals where counts appear. The app language follows the system
  per-app language picker (`locales_config.xml`).
- **No new third-party dependency without approval**, and its license must be
  GPL-3.0-compatible (Apache-2.0, MIT, BSD). Charts are custom Compose `Canvas` code, never
  a chart library.

## Code style

Kotlin official style (ktlint `intellij_idea` via Spotless), no `!!`, `Flow` over `LiveData`,
immutable UI state, MVVM with unidirectional data flow. Code and documentation in English.
Comments explain *why*, not what.

**A page's ViewModel lives as long as the activity** (the pages are `NavDisplay` entries with no
ViewModel store of their own): its state is a `stateIn(WhileSubscribed)` that stops when no page
collects it, and nothing in it collects in `init` or keeps a flow running on its own, or that
state never stops, screen off included (PLANNING.md §9, §15 «Phase 11's device checks done, and
the Ways reviewed»). Work that must run without a page belongs with the process (a notifier, the
service), not in a ViewModel.

## Design

Passo keeps the same visual language as Chiaro, for the app screens and for the widget, so the
family reads as one (`docs/adr/0004-design-language.md`). `:core:designsystem` holds it as roles
(color, type, shape), never as hexes inside a composable: Chiaro's two generated dresses
(`theme/Scheme.kt`, copied, never hand-edited), Google Sans / Inter / system type, Chiaro's
shapes and springs, and every animation collapses to a fade under reduced motion. Its principles
hold here too: one sentence before any number, every number with the line that says what it
means, estimates that say so, no dead tab and no switch for a feature that has not shipped.
Icons are `PassoIcons`, drawn in code. The launcher icon (two prints walking to the upper
right, the one behind white and the step being taken amber, on the app's green: the family's
series of 9 Nov 2026, its three layers), the status-bar icon (the same prints in one ink,
`ic_stat_steps`, also the Quick Settings tile's), the outing's walker (`ic_progress_walker`)
and the ground of the outings' shortcuts, adaptive icons (`ic_shortcut_ground`), are written by
`tools/draw_launcher_icon.py`: change the script and re-run it, never the XML. Likewise the Ways' and the city walks' data (`docs/adr/0014-ways.md`, `docs/adr/0015-city-walks.md`):
`tools/build_ways.py` writes `WayData.kt` and the place strings from `tools/ways_content.py`
(OpenStreetMap lines, ODbL, credited wherever they are read): edit the content and re-run it
(`fetch` first, for the sources; a walk's route is fetched once and committed in `tools/walks/`; `maps` redraws only the ways' maps, keeping their lines), never the outputs.
**To add a city walk or a continent, follow `tools/CITY_WALKS.md`**: the owner's rules (two cities
at a time, every sentence checked in two sources), the steps, and `tools/city_walks.py`, the
helpers that locate places, try a route, list a map's water and parks, read Wikipedia without
being throttled, and splice new walks into `WayData.kt` without moving the other cities. The Compose UI tests write screenshots to each module's
`build/screenshots`: look at them after changing a screen.

**The Ways' words** (owner, 16 Oct 2026; PLANNING.md §11 Phase 14): nothing that describes the
Ways names a religion, calls a way devotional or the reader a pilgrim, in English or Italian, in
the app, the README, VISION or the CHANGELOG. A place's own sentence may say what the place is (a
cathedral, a temple, a shrine) and a fact of its history, as a guidebook does. It may also name
the pilgrims who walked or walk by the place, «pellegrini» / "pilgrims", then or today («i
pellegrini lasciano una pietra portata da casa», "a bridge built for pilgrims") (owner, 17 Oct
2026). That is the one exception: never the reader, and no other religious word beyond the
place's own sentence.

**Accessibility and foldables** (`docs/adr/0012-foldables-and-accessibility.md`): a screen's
tests call `assertAccessible()` on what they draw, and walk the page at twice the text size and
on an open foldable (`walkPage()`); text inks are pinned at 4.5:1 by `ContrastTest`. A page
leaves `pageGutter()` on each side (in a list's content padding, never by narrowing the list),
so an open foldable shows a centred column and a phone upright is untouched. Two tiles side by
side are a `TilePair`, a segmented button's label a `SegmentLabel`. No tablet layout (owner).

**Widgets** (`docs/adr/0005-widgets.md`): Chiaro's card, colours and ink rule, carried over; the
forms' arithmetic is pure (`GlanceLayout.kt`, `WordsLayout.kt`) and pinned by tests at Chiaro's
reference grants, and `WidgetGalleryTest` draws every form to `widget/build/screenshots`: look at
them after changing a card. Glance drops the eleventh child of a container silently: count them.

**README screenshots** (`docs/screenshots/`, shown in the root `README.md`): drawn by the
`ReadmeScreenshots` test classes of the feature modules and `:widget`, from realistic sample
data, in English, and only on request: `./gradlew test -PupdateScreenshots` (plus the mirror init script in the
sandbox). Two standing rules (owner's):
- **Regenerate them** whenever a change alters what an existing one shows, and look at them
  before committing.
- **Add one** when a phase brings something worth showing (a new screen, the widget), with its
  caption in the README's table; keep the set small, the meaningful views only.

## Signing and CI

- **Debug signing**: `keystore/debug.keystore` is intentionally committed (alias `passo-debug`,
  passwords `android`) so debug APKs from CI and any machine share one signature. Do not
  regenerate it. Debug builds carry `applicationIdSuffix ".debug"`. Debug builds and the
  debug-signed release carry a crash page (`app/src/crashpage`): an uncaught exception shows its
  stack trace, to copy or share, so a test device can report a crash without adb; from the
  background, where no page can open, a notification opens it (the minified one's trace is
  retraced with that run's `mapping.txt`). Never in a release signed with the release key.
- **Release signing**: the real keystore lives OUTSIDE the repo; the `release` signingConfig is
  created only when the four `PASSO_KEYSTORE*` / `PASSO_KEY_*` properties are all set (from
  `~/.gradle/gradle.properties` locally, from `ORG_GRADLE_PROJECT_*` env vars in CI). Without
  them the release build is unsigned; `-PsignReleaseWithDebugKey` signs it with the debug key
  for testing only.
- **Release key in CI**: the four repository secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
  `KEY_ALIAS`, `KEY_PASSWORD` (the same names as Chiaro's); `release.yml` decodes the
  keystore into the runner's temp folder and fails by name if one is missing. Never commit
  the real key (`keystore/README.md`).
- **CI** (`.github/workflows/android-ci.yml`, every push and PR): formatting, forbidden
  permissions, unit tests and lint run *before* the APKs; a red suite must never produce an
  installable artifact. **Release** (`release.yml`, on `v*` tags): the same gates, then the
  signed APK, its SHA-256 and the R8 mapping on a GitHub Release, with the body taken from
  the tag's `CHANGELOG.md` section (write it before tagging).

## The family in the code

The sister apps (Chiaro, Saldo, Tempo, Tetto) are named **only** in the root `README.md` («The family»)
and in the project's documents (VISION, PLANNING, the ADRs, this file): never in a string the
app shows, its guide included, and never in the code, comments and tests included (owner,
10 Oct 2026). A comment says why a value is what it is, not which sister has it too; where a
value's provenance matters (the generated schemes), it is written in its ADR.

## Writing `README.md` (root file only)

**No em dashes (`—`) or en dashes (`–`) in the root `README.md`.** Rewrite the sentence rather
than swapping in a hyphen: use a colon when the clause explains, a full stop when the thoughts
are separate, parentheses for an aside. Same house style as Chiaro, deliberately scoped to
that one file: every other file keeps normal punctuation.
