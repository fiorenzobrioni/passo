# Passo — Technical Planning

> Companion to [VISION.md](VISION.md). This document defines the architecture, the design of the tracking engine, and the delivery phases.
> It is written to be read by humans and by Claude Code.
>
> How it is kept, and where its closed parts went (the phases done, the log's past periods): «How it is made», below.

---

## How it is made (2 Nov 2026)

The same rules as the family's other plans (owner, 2 Nov 2026: "the same rules and mechanism,
so they are uniform"):

- **This file** is the live plan: how it is made, the state, what is open, the architecture
  (§1 to §10, §12 to §14, cited by number in the code), the phases not yet done (§11), and the
  log of the current period (§15). A new decision or deviation is written **at the end of §15's
  log**, one entry each, with its date and who asked, as always; a box opened or ticked goes in
  «Open» below.
- **`docs/planning/NN-<period>.md`** are the closed parts of the plan (the phases done, the log's
  past periods), **frozen**: never edited. A decision that changes an archived one is written
  here, naming the archived file and the entry it changes.
- **`docs/planning/README.md`** is the index of every archived title (`##`, `###`, `####`) and
  every log entry (its bold title), with its line: a reference by name in the code («§11
  Phase 10», «§15 "Phase 11's device checks done"») that is not in this file is found there. It
  is written by `python3 tools/planning_index.py`, never by hand, and changes only when the plan
  is archived (`--check` says whether it is behind).
- **Archive** when §15's log passes ~1,000 lines, or at a release, whichever comes first: the log
  becomes the next archive file (with the same header as the others), a phase done moves with it,
  here an empty log stays with its start date, «Open» is read again, and the index script is run.

## State

- **Released**: 1.0.0 (1 Oct 2026, `v1.0.0`) and **1.1.0** (2 Nov 2026, `v1.1.0`, tagged by the
  owner on `main` after the device checks below): its notes in `CHANGELOG.md`, its record in
  `docs/CHANGELOG-1.1.0.md`. What comes after it goes in `CHANGELOG.md`, `[Unreleased]`.
- **Done since 1.0.0**: Phase 11 (the Ways and the city walks), Phase 13 (the interval walk), Phase
  14 (more ways, beyond Europe), in archive 01 with Phases 0 to 8 and 10; Phase 12 in archive 03.
- **Done on 2 Nov 2026**: Phase 12, Your year on foot (archive 03), checked on a device by the owner.
- **Not started**: Phase 9, Google Play (optional, later; §11, below).
- **Under way** (owner, 18 Nov 2026): Phase 15, the measured outing, first of Phases 15 to 20,
  location for an outing that asks for it (`docs/adr/0016-location.md`; §11, below; archive 04,
  from «Location, for an outing that asks for it» on). Built, all of it but what a phone must
  answer: the allowlist, the manifest, the receiver, the fusion, the routes' database, the
  backup's fields, the cap's question, the card's and the notification's lines, the
  documents. Left: the proof on a phone and the measure of what it costs (the boxes in
  «Open»), which come before any release.
- **Built, on a branch of its own from Phase 15's** (18 Nov 2026): Phase 16, the outing's
  page (the route on a map that moves, the kilometres, the climb, GPX and image, Your data;
  archive 04, «Phase 16, the outing's page»). Left: its check on a phone (the box in «Open»). 15 and
  16 are released together or not at all.
- **Built, on the branch of PR #74** (19 Nov 2026): Phase 17, the step that learns (§11; §15,
  «Phase 17 built: the step that learns»), committed on the review's branch at the owner's
  choice, merged into `main` once with the rest. Left: its check on a phone (the box in «Open»).
  **Phase 21, the open outing** (planned on 20 Nov 2026, in three steps; §11): **built, its three
  steps** (20 Nov 2026, on the branch of PR #76; §15, «Phase 21.1 built: the open outing», «Phase
  21.2 built: the marks on the way» and «Phase 21.3 built: the shape on the card»), and waits for
  a phone (the boxes in «Open»). **Phase 23, the doors**: **step 23.1 built** (21 Nov 2026, on
  the branch of its pull request; §15, «Phase 23.1 built: the doors that lead where they say»),
  and waits for a phone (the box in «Open»). **Next: 23.2**, Your outings, then **20** (reviewed
  on 19 Nov 2026: the usual round alone; it builds on 21), before 18 at the owner's wish (21 Nov
  2026: the whole of 23 before 20, «which I need more»), then 18, a city walk measured with GPS
  (re-planned on 19 Nov 2026): not started. Phase 19 dropped; Phase 22, the walk already begun,
  later, when the owner asks.

## Open

What is left to do or decide, in one place: a box opens here, naming the entry it comes from,
and closes here, naming the one that closes it. Phase 12's own boxes stay in its section (§11).

- [ ] **Baseline Profiles; R8 full mode; startup check** (archive 01, «Phase 7»).
- [ ] **The accessibility scanner reports no critical issues** (archive 01, «Phase 7»).
- [x] **The full install and update path**: install the last release, then update to the next
  over it, with the data kept (archive 01, «Phase 8»). Done by the owner on 2 Nov 2026, 1.0.0 to
  1.1.0, before the tag («I did the tests and everything works»); the database's road from schema
  3 to 6 is also tested (`UpgradeFromFirstReleaseTest`).
- [x] **On a device (owner): the new icon** (archive 04, «The new icon: two prints on the green», 9 Nov
  2026): the launcher icon in the phone's own mask, the themed icon, the status-bar icon and the
  Quick Settings tile, the Outing shortcut and the walker on an outing's bar. Done by the owner on
  9 Nov 2026 (archive 04, «The new icon checked on the phone»).
- [x] **On a device (owner): the Outing shortcut as an adaptive icon** (archive 04, «The Outing
  shortcut, an adaptive icon», 9 Nov 2026): long press on the icon, in the phone's own mask and
  with the themed icons on. Done by the owner on 9 Nov 2026 (archive 04, «The adaptive shortcut checked
  on the phone, and Tetto in the family»).
- [x] **On a device (owner): each outing's shortcut wears its own mark** (archive 04, «Each outing's
  shortcut wears its own mark», 9 Nov 2026): after starting or saving a plan, a long press on the
  icon shows the interval walk with its slow and fast line, a run with its bolt. Done by the owner
  on 9 Nov 2026 (archive 04, «The outings' own marks checked on the phone»).
- [ ] **On a device (owner): a 30-minute interval outing with the screen off**: each change
  felt, its delay read from the log, the battery check of §9, numbers in `docs/battery/`
  (archive 01, «Phase 13»).
- [x] **On a device (owner): the new ways' maps** in both themes and on an open foldable; the
  Ways page with its groups at twice the text size (archive 01, «Phase 14»). Done by the owner
  (confirmed on 2 Nov 2026).
- [x] **On a device (owner): an outing left still on a phone with no wake-up step counter**: the
  log's `SIGNAL_WAKE` row says it was let go about 15 minutes after the last step (archive 03, «2 Nov
  2026. A review of the wakes»). Done by the owner on 2 Nov 2026, before the tag.
- [x] **Your year on foot names the cities walked that year** (archive 01, «Phase 11»): done
  with Phase 12, on its ways' page (archive 03, «Your year on foot»).
- [x] **On a device (owner): Your year on foot**: the share sheet with two or three apps, the
  image in both themes, a page saved (archive 03, «Phase 12»). Done by the owner on 2 Nov 2026,
  before the tag.

- [ ] **On a device (owner): the outings, the editor and the maps reviewed** (archive 04, «The outings,
  their editor and the city walks' maps, reviewed», 11 Nov 2026): the Outings page and an outing
  under way in both themes and at a large text size; the editor's legend, each signal felt; a
  city walk's map before a start and under way, a way's map; TalkBack on a card and on the legend.

- [ ] **On a device (owner): Phase 15's proof, before the rest is built** (§11 Phase 15, «On a
  device (owner), the proof first»; archive 04, «Phase 15, the slice for the proof on the phone», 18 Nov
  2026). With the debug build of `feature/phase-15-measured-outing`: an outing with «Measure
  with GPS» started from the Outings page and from the launcher's shortcut; the screen off for
  ten minutes; pause and Resume from the notification; the app opened and closed meanwhile; a
  stop of five minutes; "Walk now" from the evening reminder with GPS on that plan; the
  permission as «Only this time»; the process ended mid-outing and the app opened again. Each
  outing's `GPS` row in the log says whether the type was added, and whether fixes came with
  the screen off, in batches or one by one.
  With the card built (archive 04, «Phase 15, what the reader sees, and the documents»), also: the
  card's line in each state (finding the satellites at the start, a tunnel or an arcade,
  location turned off and on from the quick settings, «GPS stopped» after the process is
  ended, with «Resume GPS» from the card and from the notification); «measured» under the
  distance and the pace beside a known one; the permission refused, then allowed from the
  card, and refused twice (the system's page); in both themes, at a large text size, with
  TalkBack.

- [ ] **On a device (owner): Phase 15's measure** (§11 Phase 15, «On a device (owner), the
  measure»): a 30-minute and a 2-hour outing with GPS, the battery an hour, a known distance
  against the fused one; numbers in `docs/battery/`, then the editor's cost line, §9's rule 9
  and the CHANGELOG say the figure.

- [ ] **On a device (owner): Phase 16's page** (§11 Phase 16; archive 04, «Phase 16, the outing's
  page», 18 Nov 2026): a walk with a hill and a tunnel or an arcade, then its page: the line
  where it was walked, the dashed stretch, the climb against a known figure, each kilometre's
  pace; the map pinched, dragged and brought back whole, a bead touched, the page scrolled
  with a finger that starts on the map; the GPX opened in a map app; the image shared to two
  apps; «With the routes» in a backup, imported on the other phone; «Delete this route» and
  «Delete all routes»; the page at a large text size, in both themes, on the open foldable,
  with TalkBack. On a phone with no barometer, if one is at hand: the climb said as rough.

- [ ] **On a device (owner): Phase 17's step that learns** (§11 Phase 17; §15, «Phase 17 built:
  the step that learns», 19 Nov 2026): after three or more outings with GPS at the usual pace,
  the line «Your step at this pace» on each finished page, and on the one that made it ready the
  line to the card; the proposal's figures against the wizard's own measure; «Use it», then a
  walk without GPS on a known distance reads closer than before; «Apply to past days too» and
  History; «Not now», the quiet row on the calibration page, the card opened again; «Go back»
  to the step measured, typed or estimated; the card in both themes, at a large text size and
  with TalkBack on the chart.

- [ ] **On a device (owner): Phase 21.1's open outing** (§11 Phase 21, step 21.1; §15, «Phase
  21.1 built: the open outing», 20 Nov 2026). Step 21.1's own box «On a device (owner)» says it
  all; in short:
  - an open outing with GPS at a park, started from the Outings page and from the shortcut;
  - its card, «See the outing», the route growing on the page, Pause and Stop from the page;
  - the notification and its chip; both widgets and the tile;
  - one forgotten, with a ride of more than 15 minutes and one of less;
  - the doors to its page from the notification, «Still walking?», «GPS stopped», a widget and
    the tile;
  - one without GPS with two pauses; the editor's «None» in Italian;
  - an update over the old build: «Walk» offered once at the top of the list;
  - in both themes, at a large text size, with TalkBack.

- [ ] **On a device (owner): Phase 21.2's marks on the way** (§11 Phase 21, step 21.2; §15, «Phase
  21.2 built: the marks on the way», 20 Nov 2026). Step 21.2's own box «On a device (owner)» says
  it all; in short:
  - an open outing with GPS: the pulse at each kilometre in a pocket with the screen off, and the
    voice in the headphones with that kilometre's pace;
  - one without GPS: the ten minutes, and a pause inside them;
  - the editor's group «Signals» for «None» (in Italian too), its «Try it» felt and heard;
  - the battery note of §9 unchanged: no wake beyond the signals' own.

- [ ] **On a device (owner): Phase 21.3's shape on the card** (§11 Phase 21, step 21.3; §15, «Phase
  21.3 built: the shape on the card», 20 Nov 2026). Step 21.3's own box «On a device (owner)» says
  it all; in short:
  - an open outing with GPS at a park, halfway: the strip before the first fixes, then the route
    taking its place with a fade, growing as you walk; a touch on it opens the page;
  - one without GPS, with a pause: its strip, the pause a gap, the minutes growing to the right;
  - the day's share against Today's ring, and after a change of the day's goal;
  - a goal outing taken on with «Keep going»: «Goal reached, and 20 minutes more», the strip with
    the goal's line where the bar stood;
  - a long walk's card and page (scrolling smooth, the route drawn at once);
  - «The shape of the walk» on an outing's page, and none on an outing from before;
  - in both themes, at a large text size, on the open foldable, with TalkBack on the strip and the
    route.

- [ ] **On a device (owner): Phase 23.1's doors** (§11 Phase 23, step 23.1; §15, «Phase 23.1 built:
  the doors that lead where they say», 21 Nov 2026). Step 23.1's own box «On a device (owner)»
  says it all; in short:
  - with the app left on Settings (or a Way), the tile, each widget and the counting
    notification open Today, and back from Today leaves the app;
  - a paused tile or card: Today, counting again;
  - while an outing counts, the same three open its page, and back lands on Today;
  - the weekly summary opens its week; goal reached and the evening reminder open Today;
  - a stage's notification opens its way, on the new stamp, with an outing's notification in the
    shade at the same time (each opens its own page);
  - from an outing on a city walk, its page's row and its card's small map open the walk;
  - «Walk through a city» lands on the cities;
  - a launcher shortcut's outing, then back out of the app, then Passo from Recents: no second
    outing starts, no page opens again;
  - an outing's row in Today's and History's lists wears its arrow; TalkBack says «See the
    outing» on it, and «See the walk» on the walk's row and map.

Read again on 2 Nov 2026, at archive 01 and 02: the boxes above are the ones the phases done had
left open, and the review's device check. Read again at 1.1.0's tag: the owner's checks of the
update, the outing let go, Your year on foot and the new ways' maps are done; §15 has no open
question.

Read again on 18 Nov 2026, at archive 04: nothing closed. Phase 15's and Phase 16's boxes wait
for a phone, and the entries they name are in archive 04 now.

---

## 1. Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin (latest stable 2.x), with the Compose compiler Gradle plugin |
| Build | Gradle with Kotlin DSL; version catalog (`gradle/libs.versions.toml`); convention plugins in `build-logic/` |
| JDK | Toolchain 21, `jvmTarget` 21 |
| SDK levels | `minSdk 34`, `targetSdk 37`, `compileSdk 37` |
| UI | Jetpack Compose, Material 3 in Chiaro's design language (its generated schemes, dynamic color on request), edge-to-edge |
| Widget | Jetpack Glance (`glance-appwidget` + `glance-material3`), latest stable (1.2.x at the time of writing) |
| Architecture | MVVM with unidirectional data flow; coroutines and `Flow` |
| Dependency injection | Hilt (`hilt-work` only if WorkManager is ever introduced) |
| Persistence | Room for step data and tracker state; DataStore (Preferences) for settings and profile |
| Navigation | Navigation 3 if stable at setup time, otherwise Navigation Compose with type-safe routes |
| Charts | Custom Compose `Canvas` components in `core:designsystem` (no third-party chart library) |
| Time | `java.time` |
| Testing | JUnit 5 or JUnit 4, Turbine, Truth or AssertK, Robolectric (Room and receivers), Compose UI tests, Glance unit test utilities |
| Quality | Android Lint, ktlint (via Spotless) or detekt; Baseline Profiles in Phase 7 |
| CI | GitHub Actions: build, unit tests, lint, formatting, and a forbidden-permission check on the merged manifest. A release workflow on version tags builds the signed APK and publishes it to GitHub Releases |
| License | GPL-3.0. Every new dependency must have a GPL-3.0-compatible license (Apache-2.0, MIT, BSD are fine) |

Always pick the **latest stable** versions at setup time and record them in the version catalog. Avoid alpha versions unless a phase needs them.

---

## 2. Project structure

```
passo/
├── app/                    # Application, MainActivity, navigation, DI entry points
├── build-logic/            # Convention plugins (android-library, compose, hilt, room…)
├── core/
│   ├── model/              # Pure Kotlin data classes (no Android deps)
│   ├── domain/             # Pure Kotlin: StepAccountant, calculators, streaks/records, outings, backup format
│   ├── data/               # Room DB, DAOs, DataStore, repositories
│   ├── tracking/           # StepTrackingService, sensor source, receivers, notification
│   ├── designsystem/       # M3 theme, typography, shared components, charts, icons
│   └── testing/            # test-only: the accessibility checks and the page walk the UI tests share
├── feature/
│   ├── today/
│   ├── history/
│   ├── insights/           # records, streaks, totals
│   ├── settings/           # Settings, your data (export, import), the step calibration
│   ├── onboarding/
│   ├── sessions/           # the Outings page and the outing editor (Phase 10)
│   ├── ways/               # the Ways and the city walks (Phase 11)
│   ├── year/               # Your year on foot (Phase 12)
│   └── guide/              # the guide, in Chiaro's shape
├── widget/                 # Glance widget(s), receiver, update coordinator
├── docs/                   # ADRs, formulas, battery test notes
├── VISION.md
├── PLANNING.md
└── CLAUDE.md
```

Rules:

- `core:model` and `core:domain` are pure Kotlin/JVM modules. All business logic lives there and is unit-tested without Android.
- Feature modules depend on `core:*`, never on each other.
- `app` wires everything together.

---

## 3. Architecture overview

Reviewed at Phase 7 (25 Sep 2026) against the code: the Phase 1 diagram showed the tracking
path alone; the app now also has outings, in-process live state, pushed widget updates, goal
alarms, the tile, the export and import, and the calibration.

```mermaid
flowchart LR
    subgraph PHONE["Android"]
        SENSOR["Hardware step counter<br/>(non-wake-up, batched in the hub)"]
        BCAST["Boot, update, screen,<br/>shutdown, time broadcasts"]
        ABK["Android backup<br/>(allowlist, ADR 0007)"]
        SAF[/"Files, through the<br/>Storage Access Framework"/]
    end

    subgraph TRACKING[":core:tracking"]
        SVC["StepTrackingService<br/>(FGS type health)"]
        SURF["Counting notification<br/>StepsTileService · shortcuts"]
        GOALS["GoalNotifier<br/>(inexact alarms)"]
        PROBE["StepCounterProbe<br/>(calibration page only)"]
    end

    subgraph DOMAIN[":core:domain (pure Kotlin)"]
        ACC["StepAccountant → StepLedger<br/>SessionTracker"]
        READ["TodayOverview · PeriodOverview · Insights<br/>WalkDetector · TypicalDayCalculator"]
        POLICY["WidgetUpdatePolicy"]
        BK["BackupCodec · BackupMerge · CsvExport<br/>StepCalibration"]
    end

    subgraph DATA[":core:data"]
        REPO["Repositories (Flow)<br/>Tracking · Session · Settings · Backup"]
        DB[("Room: minutes, summaries,<br/>tracker state, outings, log")]
        DS[("DataStore: profile, settings")]
        LIVE["LiveSteps · LiveSession<br/>(in process, never stored)"]
    end

    UI["Compose screens (:feature:*)"]
    WIDGET["Glance widgets (:widget)"]

    SENSOR -->|"samples with timestamps"| SVC
    BCAST --> SVC
    SVC <--> ACC
    SVC -->|"one transaction per batch:<br/>minutes, summaries, state, outing"| REPO
    REPO <--> DB
    REPO <--> DS
    SVC -->|"stored + buffered"| LIVE
    SVC -->|"WidgetUpdates"| POLICY
    POLICY -->|"screen on only"| WIDGET
    SVC -->|"screen on, 5 s at most"| SURF
    SVC -->|"goal reached, on samples"| GOALS
    REPO --> READ
    LIVE --> READ
    READ --> UI
    READ --> WIDGET
    READ --> SURF
    UI -->|"profile, goal, settings:<br/>past days frozen first"| REPO
    UI -->|"outing commands (intents)"| SVC
    UI <-->|"export, import"| BK
    BK <--> REPO
    BK <--> SAF
    SENSOR -.->|"Start and Stop"| PROBE
    PROBE -.-> UI
    DB -.-> ABK
    DS -.-> ABK
```

**The write path, and its one writer.** Only the service writes steps. The sensor's samples go
through `StepAccountant` (deltas into minutes, §4.4) and `StepLedger` (the buffer and its write
triggers, §4.5); `SessionTracker` measures the outing under way from the same deltas. A batch
is written by `TrackingRepository.persist` in **one** Room transaction: the minutes, the
summaries of the days they touch, the tracker state, the log lines and the outing. The same
repository is the only way to change the profile or the goal, and the import's only way in: one
lock orders all of them, so a day is frozen before a new profile or goal can reach it (§5,
ADR 0003) and no batch lands inside an import.

**The read path is computed, not stored.** The screens, the widgets and the notification read
the repositories' `Flow`s and derive everything in `:core:domain`: today's sentence and pace
(`TodayOverview`), the periods (`PeriodOverview`), records and streaks (`Insights`), walks and
the usual day. Nothing of it runs in the background or is cached in a table (§5, §6).

**Live state stays in the process.** Between two writes the service publishes today's stored
plus buffered count (`LiveSteps`) and the outing as it stands (`LiveSession`). Today, the tile
and the widgets add it to what is stored (`withPending`, `byDayWithLive`), so every surface
shows the same count and it never goes back during a write. It is never persisted: the
database is the truth after any crash.

**Surfaces update only when someone can see them** (§7, §8, §9). The service tells the widgets
through `WidgetUpdates` (declared in `:core:data`, so the service never depends on `:widget`),
and `WidgetUpdatePolicy` decides now, later or not at all; with the screen off, only a change
of tracking state or of a setting repaints. The notification follows the screen; the tile reads
only between `onStartListening` and `onStopListening`. Goal reached rides on the samples; the
evening reminder and the weekly summary are one inexact alarm each (`GoalNotifier`), after a
catch-up with the sensor (`TrackerLink`).

**Control has two doors.** Pause and resume go through `TrackingControl` (Settings' switch,
Today's card, a paused widget's or tile's tap): a pause stops the service, which writes its
buffer as it goes; a resume forgets the baseline, then starts the service from the activity,
where a foreground service may start (a widget's tap opens the app to do it). An outing's
start, pause, resume, stop and "keep going" are intents to the running service
(`SessionControl`), from the screens, the notification's actions, the launcher's shortcuts and
the evening reminder's "Walk now".

**Data in and out, by the reader or by Android** (Phase 7, ADR 0011; ADR 0007). The reader's
export writes the whole history as JSON, or a table as CSV, to a file the system's picker
chose; the import reads one back and merges it (`BackupMerge`: every minute at its larger
count, never deleting). Android's backup copies the database and the settings file, nothing
else; a tracker state from another installation is dropped (`adoptTrackerState`). Passo itself
has no network access either way.

**The calibration reads the counter directly** (`StepCounterProbe`), only while its page is on
screen: the counter is cumulative, so its values at Start and Stop measure the walk whatever
happened in between, independently of the service.

`:app` wires the graph (Hilt), holds the activity and the navigation (Navigation 3: the three
tabs, Settings, the Outings page and editor, the calibration), and starts the service when the
app is opened.

---

## 4. Step tracking engine (core design)

This is the riskiest part of the app. Build it first, test it in the field, and don't build UI on top of it until it is proven.

### 4.1 Why a foreground service

- `TYPE_STEP_COUNTER` is **cumulative since boot** and **resets to 0 at every boot**.
  - Steps taken while the app isn't running are still counted by the hardware. They're only lost if the device reboots before the app reads them.
- Since Android 9, `ACTION_SHUTDOWN` is delivered **only to receivers registered at runtime**, so a live process is needed to save the last value before power-off.
- Since Android 9, apps in the background don't receive sensor events. A worker waking up periodically can't reliably read the sensor.
- A foreground service of type `health` is the documented mechanism for continuous fitness tracking.
  - Prerequisites: the `FOREGROUND_SERVICE_HEALTH` permission in the manifest, and the runtime permission `ACTIVITY_RECOGNITION`.
  - The service can be started from `BOOT_COMPLETED`. `BOOT_COMPLETED` and `MY_PACKAGE_REPLACED` are exemptions from the background-start restriction, and `health` is not among the types Android 15 forbids from a `BOOT_COMPLETED` receiver (`dataSync`, `camera`, `mediaPlayback`, `phoneCall`, `mediaProjection`, `microphone`). `ACTIVITY_RECOGNITION` is not a while-in-use permission, so it does not block a start from the background either. Established from the documentation in Phase 1 (§15).

### 4.2 Service lifecycle

| Trigger | Action |
|---|---|
| App opened (activity in foreground) | Start the service if it isn't running and permissions are granted |
| `BOOT_COMPLETED` (manifest receiver) | Start the service |
| `MY_PACKAGE_REPLACED` (manifest receiver) | Start the service after an app update |
| Service `onStartCommand` | `startForeground(FOREGROUND_SERVICE_TYPE_HEALTH)` and register the sensor listener; return `START_STICKY` |
| `ACTION_SHUTDOWN`, `QUICKBOOT_POWEROFF` (runtime receiver) | `goAsync()`, then `sensorManager.flush()`, wait for `onFlushCompleted` (at most about 1.5 s), then persist synchronously |
| `ACTION_SCREEN_ON`, `ACTION_USER_PRESENT` (runtime) | Re-register with low latency, flush, refresh notification and widget |
| `ACTION_SCREEN_OFF` (runtime) | Persist the buffer, then re-register with high latency |
| `ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED` (runtime) | Persist the buffer, recompute today's local date, refresh the surfaces |
| User toggles "Pause tracking" | Persist, unregister, `stopSelf()` |

If the user or the OEM stops the service, no data is lost as long as the device doesn't reboot in the meantime, because the counter is cumulative. The UI and widget then show a "Tracking paused" state with tap-to-resume. The limitation is that a force-stopped app doesn't receive `BOOT_COMPLETED` until the user opens it again; this is documented in the FAQ.

### 4.3 Sensor registration strategy

| Screen state | Sensor | `samplingPeriodUs` | `maxReportLatencyUs` |
|---|---|---|---|
| Off | Non-wake-up `TYPE_STEP_COUNTER` | `SENSOR_DELAY_NORMAL` | High, `SCREEN_OFF_LATENCY` (default 10 min) |
| On | Same sensor | `SENSOR_DELAY_NORMAL` | Low, `SCREEN_ON_LATENCY` (default 0 to 2 s) |

- A non-wake-up sensor never wakes the application processor. Events wait in the hardware FIFO and are delivered the next time the processor is awake.
- If the FIFO overflows, intermediate events are dropped, but the **total stays correct** because the counter is cumulative. Only minute-level attribution gets coarser.
- **Non-wake-up, decided from the platform documentation** (`docs/adr/0002-sensor-reporting.md`): the wake-up variant would only bound the loss on an *abrupt* power loss, and would pay for it with a wakeup per report window, every day. A device that has only a wake-up step counter still gets it.
- `sensor.fifoMaxEventCount`, `fifoReservedEventCount`, vendor, name and wake-up mode are written to the diagnostics log at every service start, with whether the phone has a wake-up counter (and its FIFO) and which step detectors it has (read, never registered).
- Some phones have no FIFO at all (`fifoMax=0`, the owner's Samsung, Oct 2026): with the screen off the counter's events are dropped while the processor sleeps, and its latest value arrives in a clump when the phone next wakes. The total stays exact; the attribution of §4.4 lays the clump over the minutes it took.

### 4.4 Accounting algorithm (`StepAccountant`, pure Kotlin)

Input for each sensor event: `counterValue` (total since boot), `eventElapsedNanos` (the event timestamp), plus an injected `SystemSnapshot(bootCount, elapsedRealtimeNanos, wallClockMillis, zoneId)`.
`bootCount` comes from `Settings.Global.BOOT_COUNT`.

```
state = TrackerState(bootCount, lastCounterValue, lastSampleElapsedNanos, lastSampleWallMillis)   // persisted in Room

delta = when {
    state == null                       -> 0                   // first run ever: baseline, don't count steps from before install
    snapshot.bootCount != state.bootCount -> counterValue      // new boot session: counter restarted from 0
    counterValue < state.lastCounterValue -> counterValue      // sensor/HAL reset without reboot
    else                                -> counterValue - state.lastCounterValue
}

eventWall = snapshot.wallClockMillis - (snapshot.elapsedRealtimeNanos - eventElapsedNanos) / 1e6
if (eventWall is in the future or before the lower bound) eventWall = snapshot.wallClockMillis   // guard against buggy timestamps

lowerBound = if (same boot session) state.lastSampleWallMillis else bootWallMillis

// Attribute delta to minute buckets, by the milliseconds each minute holds of the stretch walked:
//  - the stretch ends at eventWall and lasts delta / DEFAULT_CADENCE (≈110 spm), never before
//    lowerBound; a delta faster than that is spread evenly over [lowerBound, eventWall]
//  - one rule for every gap: a lone step lands in its own minute; a clump handed over at once
//    (a sensor with no FIFO, whose events are dropped while the processor sleeps) is laid over
//    the minutes it was walked in, never 170 steps in one minute and 25 in the next
//  (until Oct 2026: a gap ≤ 2 min put everything in the eventWall minute, §15)
// Sanity: if delta > MAX_CADENCE (250 spm) × gapMinutes + 50 → cap, log a diagnostics anomaly

newState = state.copy(bootCount, lastCounterValue = counterValue, lastSampleWallMillis = eventWall)
```

Output: a list of `(epochMinute, localEpochDay, steps)` increments, plus the new state.
All wall-clock and time-zone logic is injected, so the algorithm can be fully unit-tested.

### 4.5 Persistence rules

- Keep an in-memory buffer of minute increments in the service.
- Flush the buffer to Room in **one transaction** that also updates `TrackerState` and today's `DailySummary`. Flush when any of these happens:
  - a minute boundary is crossed while events are flowing
  - the screen turns off
  - shutdown
  - `onDestroy`
  - 100 or more buffered steps
  - Once per drain: a write asked for and still under way carries the samples recorded meanwhile, so a FIFO handed over in one burst asks for one write, not one per sample (2 Nov 2026, §15).
- Invariant: the steps stored in Room plus the stored `lastCounterValue` are always consistent.
  - If the process dies with an unflushed buffer, the next sample recomputes the delta from the stored `lastCounterValue`. Nothing is double-counted and nothing is lost; only the minute attribution may shift.

### 4.6 Edge cases (all need unit tests)

| Case | Expected behavior |
|---|---|
| First install mid-day | Baseline at the current value. Today starts from 0 and the UI explains this once. |
| Nightly shutdown | The shutdown flush saves the last value. After boot, the first sample has a new `bootCount`, so `delta = counterValue`. |
| Abrupt power loss or crash | Steps since the last delivered batch are lost (bounded by the latency window). Documented. |
| Walking across midnight | Event timestamps split the steps between days. The gap rule splits long gaps proportionally. |
| Time-zone change | Buckets are stored in UTC `epochMinute` together with the `localEpochDay` computed at write time. Days already recorded are never re-bucketed. |
| Manual clock change | Same as a time-zone change. Timestamps come from elapsed realtime, so they are monotonic. |
| Sensor jumps (buggy HAL) | Capped by `MAX_CADENCE` and logged. |
| A counter with no FIFO, handing steps over in clumps | Each clump laid back over the time it took at 110 spm, minute by minute: no false minute of running, the total exact. |
| `ACTIVITY_RECOGNITION` revoked | The service stops. The UI, notification and widget show "Permission needed". |
| No step counter sensor | The APK from GitHub installs on any device, so a blocking explanation screen is **required**. `uses-feature required=true` stays in the manifest for a future Play listing, where it filters these devices. |
| Counter float precision | Values are `Float`; convert with `toLong()`. Exact up to 2^24 steps per boot session, which is far beyond realistic use. |

---

## 5. Data model

### Room

```kotlin
@Entity(tableName = "tracker_state")          // single row, id = 0
data class TrackerStateEntity(
    @PrimaryKey val id: Int = 0,
    val bootCount: Int,
    val lastCounterValue: Long,
    val lastSampleElapsedNanos: Long,           // monotonic: gaps are measured on this clock (§15)
    val lastSampleWallMillis: Long,
    val updatedAtMillis: Long,
)

@Entity(tableName = "minute_steps", indices = [Index("localEpochDay")])
data class MinuteStepsEntity(
    @PrimaryKey val epochMinute: Long,         // UTC minute
    val localEpochDay: Long,                    // local date at write time
    val steps: Int,
)

@Entity(tableName = "daily_summary")
data class DailySummaryEntity(
    @PrimaryKey val localEpochDay: Long,
    val steps: Int,
    val distanceMeters: Double,
    val activeKcal: Double,
    val activeMinutes: Int,
    val briskMinutes: Int,
    val goalSteps: Int,                         // goal in effect that day
    val finalized: Boolean,                     // true after the day is over
)

@Entity(tableName = "diagnostics_event")      // ring buffer, ~500 rows
data class DiagnosticsEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val wallMillis: Long,
    val type: String,                           // BOOT, SHUTDOWN_FLUSH, ANOMALY, SERVICE_START…
    val detail: String,
)
```

- Size estimate: about 300 minute rows per day, roughly 110k rows per year, a few MB. Keep minute data indefinitely; it's needed for hourly charts and recalculation.
- Today's `DailySummary` is recomputed from today's minute rows at every flush.
- Past days are **frozen**, so changing the profile doesn't silently rewrite history. Settings offers **"Apply profile to past data"**, which recomputes every summary from the minute rows.
- Use Room auto-migrations with exported schemas (`room.schemaLocation`) committed to the repo.
- **Schema v4 (Phase 11):** `way_journey`, a way the reader started (the way, its start day and instant, its state `ACTIVE` / `FINISHED` / `LEFT`, the day it ended, how far its stages were told). No distance: what was walked is the days' own estimates from the start, read with them (`docs/adr/0014-ways.md`).
- **Schema v5 (Phase 11, part two):** `session.walk` (the city walk an outing walks, by name; null for every other outing) and `session.walkFromMeters` (where along the walk it began). A walk's journey is a `way_journey` row like a way's; its progress is its outings', read with them (`docs/adr/0015-city-walks.md`).
- **No tables for walks or the typical day.** Both are computed on read from `minute_steps` (§6.1, §6.2). Add a cache table only if profiling shows a need.
- **Schema v2 (Phase 10), an auto-migration adding two tables and touching nothing else:**
  - `session_plan`: the reader's outings (name or null, goal kind and value, intensity, milestones as a bit set, vibrate, position, last started).
  - v3: `voice` (`OFF`, `HEADPHONES`, `ALWAYS`) on both, default `OFF` (the voice, ADR 0010).
  - `session`: each outing walked, with its goal copied at the start (frozen like a day), its state (`ACTIVE`, `PAUSED`, `FINISHED`) and end reason, its totals (steps, time in motion, time at its pace, estimated distance and energy), its last step and last measured moment, the milestones already told. The one under way is written in the step batch's transaction (§4.5).

### DataStore (Preferences)

- Profile: height, weight, sex (optional), step length mode (auto, manual, calibrated or learned), walking step length, running step length, and the learnt step in use (`profile_learned_step`, Phase 17: its points of cadence and length, the usual cadence, the metres and outings it came from, the latest outing it holds, the mode it replaced, whether its running part is in use), written as one line of text (`LearnedStepText`); the backup's `ProfileDto` carries it as `learnedStep`.
- The learnt step last proposed and put away (`step_curve_put_away`, Phase 17): the phone's state, never in the backup's file, what a new proposal must move 2% from.
- Goal, units, first day of week, theme, dynamic color, notification opt-ins, reminder time and threshold, tracking enabled.
- The last day whose goal was seen reached (not a setting: the record that keeps "goal reached" once a day).
- Walk detection enabled (default on), minimum walk duration (5, 10 or 15 min; default 10), typical-day line shown (default on), Today's "Start an outing" button shown (default on).
- Whether the outing presets were written once (so deleting them is final), and the last finished outing whose card Today has put away.

---

## 6. Derived metrics (`core:domain`)

All constants live in one file (`MetricsConstants.kt`) with comments that cite their sources. All defaults are to be validated.

- **Default walking step length:** `height × 0.415` (`× 0.413` when the profile says female). If height is unknown, 0.70 m.
- **Running step length:** default `walking × 1.3`, user-editable. It applies to minutes with a cadence of at least `RUNNING_CADENCE` (140 spm).
- **The step learnt** (Phase 17, `StepLengthLearner`): once accepted, `stepLength(cadence)` reads its curve where it has a part for the gait (walking below 140, running from it; a straight line between its points, flat beyond them), and the lengths above elsewhere.
- **Distance:** the sum over minutes of `steps × stepLength(cadence)`.
- **Active calories (net):** the sum over minutes of `distanceKm × weightKg × k(cadence)`.
  - `k_walk ≈ 0.5 kcal/kg/km`, rising slightly for brisk cadences: flat up to 100 spm, then linearly to 0.6 just below 140 spm (Phase 2, §15).
  - `k_run ≈ 1.0 kcal/kg/km`.
  - Default weight if not set: 70 kg.
  - Net means basal metabolism is not included. The UI calls this "active calories".
  - Alternative considered: MET-based tables by speed. Rejected because it is less robust for sparse minutes.
- **Active minutes:** count of minutes with at least `ACTIVE_MINUTE_THRESHOLD` (40) steps.
- **Brisk minutes:** count of minutes with at least 100 steps.
- **Average cadence:** steps in active minutes divided by the number of active minutes.
- **Streak:** consecutive finalized days with `steps ≥ goalSteps`, plus today if its goal is already met.
- **Records:** best day, ISO or locale week, month, and longest streak.

### 6.1 Walk detection (`WalkDetector`, pure Kotlin)

Input: the minute buckets of one local day, plus the settings. Output: a list of `Walk(start, end, steps, distanceM, activeKcal, avgCadence, type)`.

```
1. Mark each minute as "moving" if steps ≥ WALK_MINUTE_THRESHOLD (60).
2. Merge moving minutes into runs, bridging gaps of up to WALK_MAX_GAP_MINUTES (2) non-moving
   minutes (traffic lights, doors). Steps inside a bridged gap belong to the walk.
3. Discard runs shorter than the minimum duration setting (default 10 min).
4. For each remaining run compute the metrics with the same calculators as §6.
5. type = RUN if avgCadence ≥ RUNNING_CADENCE, else WALK.
   (Optional refinement: MIXED if both walking and running minutes exceed 30% of the walk.)
```

- A walk crossing midnight is split at midnight, like every other daily metric.
- Constants live in `MetricsConstants.kt` next to the others.
- Cost: one linear pass over at most 1,440 rows, only when the day detail or Today screen is visible. No background work.
- Accuracy depends on minute attribution (§4.4). With a working FIFO it's minute-accurate; after a FIFO overflow or a service restart, walk boundaries may be off by a few minutes. This is acceptable and documented.

### 6.2 Typical day curve (`TypicalDayCalculator`, pure Kotlin)

- Take the same weekday over the last `TYPICAL_DAY_WEEKS` (4) weeks, skipping days with fewer than 500 steps (phone left at home, tracking paused).
- For each day, build the cumulative curve in 15-minute slots (96 points).
- The typical curve is the mean of those curves, slot by slot. It is shown only if at least 2 valid days exist.
- "Ahead or behind" = today's cumulative value minus the typical value at the current slot. It is shown as a short label, for example "+1,240 vs usual".
- Computed when the Today screen opens and then once every 15 minutes while it stays visible. It never runs in the background.

---

## 7. Widgets (Glance)

Two widgets since Phase 4 (owner's request), in Chiaro's dress so a Passo card and a Chiaro card on one home screen read as one family: «At a glance» / «A colpo d'occhio» (`GlanceWidget`: today's ring where Chiaro's weather glyph was) and «In words» / «In parole» (`WordsWidget`: the day in type, with two small marks). The names were swapped on 14 Nov 2026 and swapped back on 15 Nov 2026 (owner, §15): in the whole family «At a glance» is the card with a picture and «In words» the card in type alone, and the code's names say the same again. «At a glance» is first in the manifest and opens at 4×1; a new card wears the app's green. Decisions and reasons: `docs/adr/0005-widgets.md`.

### Configuration

- `SizeMode.Exact`: every form is read off the size the launcher really granted.
- **One size spec for both cards** (`WidgetProviderTest`): default placement 4×1 (`targetCellWidth` 4, `targetCellHeight` 1, where Chiaro's pair opens), minimum one cell (`minResizeWidth`/`minResizeHeight` 40 dp), `resizeMode="horizontal|vertical"`, and **no maximum**: Launcher3 turns a dp maximum into cells on every grid profile and keeps the smallest, so no value means "four cells" everywhere (ADR 0005). Each layout has a form for any grant instead.
- `updatePeriodMillis = 0`; `configure` is the per-widget settings screen, `reconfigurable|configuration_optional`.
- Reference grants the layout tests measure against (Chiaro's): one row ≈ 85 dp tall, two cells ≈ 159 dp, three ≈ 250, four ≈ 340; two rows ≈ 189 dp.

| «At a glance» (`GlanceWidget`) form | When | Content |
|---|---|---|
| DOT | narrower than 120 dp | The ring with the count inside (compact where the language shortens thousands) |
| NARROW | one row | Ring, count, «of 10,000 steps» |
| WIDE | one row with a sentence column (4 cells) | The same, and the day's sentence at the far edge; or mirrored, the ring on the right |
| TALL | two rows and up | Ring in the top corner; count, sentence and goal from the bottom. At every form and size the ring is the family's circle, the dial of Tempo's «At a glance» at the same size (§15, 4 Nov 2026, «at every size») |
| PANEL | two rows and up, three cells or wider | The row on top, its ring the family's circle, larger without the sentence as the tall card's (the sentence under the count where the ring leaves it no room at the far edge), and under it today's steps by hour (24 bars built from boxes, current hour highlighted) or, as the reader chooses, the last 7 days against the goal (7 bars, today highlighted, a line at today's goal; §15, 2 Nov 2026) |

| «In words» (`WordsWidget`) form | When | Content |
|---|---|---|
| LINE | one row, one or two cells | «Steps today», the count, the share of the goal where it costs the number nothing |
| ROW | one row, three cells and up | The count on the leading side; the sentence, the goal and the distance and calories at the far edge |
| STACK | two rows and up | The eyebrow on top; the count large, the sentence and the facts at the bottom; «the day in figures» with height to spare |
| PANEL | two rows and up, four cells | The count beside the words, on one baseline; the day in figures under both |

### Rendering

- Colours are resolved at render time against the ground the card really has (Chiaro's ink rule, `widgetInk`), never left to the launcher's day/night resolution: white inks on one of Chiaro's six card colours, the dress's own inks on light or dark, and below 50% solidity the wallpaper's hint decides. Material 3 dynamic color when the reader turned wallpaper colours on in the app.
- **The ring is a bitmap** painted at the size shown, capped at 416 px a side (at most 0.7 MB, about 50 KB in a row), one per card: far under the Android 17 RemoteViews bitmap cap. Vector levels were rejected (5% steps for the arc and the notch). The bars are boxes.
- Previews: a static `previewLayout` of the default card, and generated previews on Android 15+ (`providePreview`), published once per app version.
- Tapping a card opens the Today screen. A paused card opens the app asking it to resume (the service is started from the activity). A card without the permission or stopped by the system opens the app, which fixes or restarts it.

### Update strategy (battery-aware)

- Push-based from `StepTrackingService` through `WidgetUpdates` (interface in `:core:data`, `WidgetUpdateCoordinator` in `:widget`), decided by `WidgetUpdatePolicy` (pure, `:core:domain`). There is **no periodic polling**.
  - Screen on or user present: flush the sensor, then update immediately.
  - While the screen is on: update at most every 60 s, and only if the steps changed (a trailing timer, cancelled at screen-off).
  - Day rollover (detected by the screen-on ticker), goal reached: update immediately.
  - Settings or profile changed, tracking started or stopped, an outing started, paused or ended: update immediately, with the screen off too (one repaint, and a stopped service cannot repaint at the next screen-on).
- While an outing is under way, both cards say it in the sentence's place («Brisk walk: 12 of 20 min»), as the tile's line does (Phase 10).
  - Screen off: no updates.
- The widget reads the repository plus the service's in-process buffer (`LiveSteps`), exactly as Today does, so it shows persisted truth and the steps not written yet.

---

## 8. Notifications and Quick Settings tile

- **Channel `tracking`** (low importance, silent, `setOnlyAlertOnce`): the ongoing foreground service notification.
  - Content: collapsed, today's steps; expanded, the way to the goal (or when it was reached), the share of the goal with the active minutes, the estimated distance and calories, and a progress bar to the goal.
  - Updated only while the screen is on, at most every 5 s. Both forms are built in the same update; today's minutes are read again only after a write.
  - How it shows (in full, minimized, off) is the reader's, on the system's channel page: Settings reads it and opens that page (§15).
- **During an outing** (Phase 10) the `tracking` notification is the outing's: its name, its sentence, «12 of 20 min», the pace against the outing's, the steps and the estimates, Pause/Resume and Stop. Android 16+: `ProgressStyle` with the milestones as points, promoted to a Live Update (`POST_PROMOTED_NOTIFICATIONS`); below, `BigTextStyle` and a progress bar. Updated as the day's form is (screen on, every 5 s at most), plus once at each milestone.
- **Channel `sessions`** (default importance, no sound, no vibration of its own): an outing's goal reached, with "Keep going" for 15 minutes; an outing ended by a long stillness, with "Resume" for as long. The milestones on the way are Passo's own vibration patterns, played only while this channel is on.
- **Channel `goals`** (default importance, opt-in): goal reached once per day, the evening reminder (with "Walk now": an outing for the rest of the day), the weekly summary.
  - Goal reached rides on the service's samples. The reminder is an inexact `setAndAllowWhileIdle` with a wakeup, the summary an inexact `RTC` alarm without one; both armed again at every start of the process, every change of what they depend on, every clock or zone change, and every firing. No exact alarm permission (Phase 6, §15).
  - Each alerts when posted (no "only alert once": under a fixed id it silenced today's while yesterday's was still shown). Goal reached and the reminder go by themselves at the end of their day (`setTimeoutAfter`); the reminder also when its "Walk now" is touched or the goal is reached. No reminder during an outing, and no reminder alarm armed while the goals' notifications cannot show (armed again when they can: the app's return, the system's block-state broadcasts) (2 Nov 2026, §15).
- **Channel `ways`** (default importance, Phase 11): a stage reached on the way under way, and its end. Told by the days being written (`WayNotifier`), never by a timer; touching it opens that way's page, on its journey, where the new stamp is (Phase 23; until then, the Ways page).
- **Every touch names the page it opens** (Phase 23, `AppDoors`; ADR 0008 decision 8, amended): the launch intent alone brings the `singleTask` task back on whichever page it was left. The counting notification, the tile, the widgets, goal reached and the evening reminder open Today; while they show an outing, its page, over Today; the weekly summary its week in History; a stage its way, over Today. Each door has its own request code (`DoorRequest`), and an intent handed back from Recents is not a touch.
- If `POST_NOTIFICATIONS` is denied, the foreground service still runs; its notification only shows in the system's task manager. Explain this in onboarding; don't block on it.
- **QS tile (`TileService`):** reads today's steps from `onStartListening()` to `onStopListening()`, following the live count meanwhile. It costs nothing when the panel is closed.

---

## 9. Battery rules (non-negotiable)

1. No accelerometer. No continuous `TYPE_STEP_DETECTOR` unless measurements prove it's cheap.
2. No wakelocks held by the app, except inside `goAsync()` for the shutdown flush, and the one of §9.7: an outing with signals on a phone whose step counter cannot wake it.
3. No exact alarms, no `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, no periodic workers for tracking.
4. Nothing runs on a timer while the screen is off. All screen-on tickers are coroutines tied to the screen state.
5. Database writes are batched (§4.5).
6. Each phase that touches the service ends with a battery check:
   - `adb shell dumpsys batterystats --charged <pkg>`
   - `adb shell dumpsys sensorservice` (confirm batching is active)
   - Doze simulation: `adb shell dumpsys deviceidle force-idle`
   - Battery Historian for the longer field tests
7. **The one exception (Phase 10, `docs/adr/0009-sessions.md`):** while an outing the reader started is counting, the wake-up step counter (if the phone has one besides the usual one) reports within 30 s, and 2 s in the 40 s before a change of an interval outing (Phase 13, `docs/adr/0013-interval-walks.md`), so its signals reach a phone in a pocket on time: about two brief wakes a minute while walking, a few more in each of an interval outing's last 40 s before a change, none while still, no wake lock or timer of the app's own. Paused, over or with no outing, the registration is the usual one. An outing ends by itself (goal, 15 min still, 1 h paused, 4 h), noticed at a step or a screen-on, never by a timer.
   *Amended 4 Oct 2026 (owner, after the first interval walk; `docs/adr/0013-interval-walks.md`):* on a phone with **no** wake-up step counter, an outing that tells its signals (vibration or voice, and the outings' channel on) holds a partial wake lock while it counts, so the counter's steps arrive as they are taken and every signal with them; the screen stays off; let go at a pause, the end or the service's, bounded at 4 h 10 min; logged when taken and let go. No timer: the signals still ride on the steps. An outing with no signal, a paused one, and every phone with a wake-up counter never hold it. About 1 to 3% of a battery an hour, to be measured.
   *Amended 2 Nov 2026 (owner, after a review):* that wake lock is also let go once the outing has been still for 15 minutes (`SignalWake.letGoAt`), when it is over and has nothing left to tell on time; it is closed at the next step or look, as on every phone. A forgotten outing had kept the processor awake until the screen came on, up to the 4 h 10 min bound. The moment is kept by a timer in the service, the one timer that runs with the screen off, and only while the wake lock already keeps the processor awake: it shortens a wake and never makes one.
8. OEM task killers: onboarding shows a battery tip, with a button to the app's own settings page, only when `Build.MANUFACTURER` is on a known list (`OemTips`). Samsung is not on it (§15).

9. **The second exception (Phase 15, `docs/adr/0016-location.md` decisions 5, 10 and 14):** the satellite receiver, for an outing the reader started with «Measure with GPS» on. On while it counts and the reader walks (a fix a second; with the screen off the fixes are held up to 60 s and asked for by each step batch, in the wake that batch already made); asleep 2 minutes after the last step, awake at the next (`GpsDuty`); off at a pause and at the end; for at most 15 minutes after an end that can be taken back, steered the same way, and off when the card is put away. Never for the day's count, never with no outing, never by a timer or a geofence; every part rides on a step batch, a batch of fixes, a touch or the screen. Its cost is to be measured on a phone and written in `docs/battery/` (expected: a few percent of a battery an hour while walking).
   *Rule 7, amended with it (18 Nov 2026):* its wake lock is also held by an outing with GPS, with signals or without, on a phone with no wake-up step counter: there the steps steer the receiver, and nothing else could bring the step that wakes it. Its bound is ten minutes past the outing's own cap, which «Keep going» moves, no longer 4 h 10 min from when it is taken.

---

## 10. Permissions and manifest

| Permission / element | Type | Why |
|---|---|---|
| `ACTIVITY_RECOGNITION` | Runtime | Read the step counter; prerequisite for the `health` foreground service |
| `POST_NOTIFICATIONS` | Runtime | Show the tracking and goal notifications (optional) |
| `FOREGROUND_SERVICE` | Normal | Foreground service |
| `FOREGROUND_SERVICE_HEALTH` | Normal | Foreground service of type `health` |
| `RECEIVE_BOOT_COMPLETED` | Normal | Restart tracking after boot |
| `VIBRATE` | Normal | An outing's signals, in their own patterns (Phase 10) |
| `POST_PROMOTED_NOTIFICATIONS` | Normal | The outing under way as a Live Update on Android 16+ (Phase 10) |
| `<queries>` for `TTS_SERVICE` | Manifest | Lets Android 11+ show Passo the text-to-speech engine, for the outings' voice (ADR 0010). Not a permission |
| `WAKE_LOCK` | Normal | An outing's signals on a phone whose step counter cannot wake it: the processor kept awake while the outing counts (§9.7, ADR 0013). Also brought by WorkManager, held by the job while a widget card is drawn |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Runtime | An outing the reader started with «Measure with GPS» on: the satellites, read from its start to its pause or end (Phase 15, ADR 0016). Asked together, in context, the first time such an outing starts; approximate alone measures no walk, and the outing then counts from the steps |
| `FOREGROUND_SERVICE_LOCATION` | Normal | The service's second type, `location`, added while such an outing counts and dropped at its pause or end |
| `<uses-feature android:name="android.hardware.location.gps" android:required="false"/>` | Feature | So that a phone with no receiver is not filtered out on a future listing: there the switch is absent |
| `ACCESS_NETWORK_STATE` | Normal | Brought by WorkManager, which Glance runs its widget sessions on (Phase 4): it lets nothing leave the phone without `INTERNET`. Passo's own code does not use it |
| `android:allowBackup="true"` + `dataExtractionRules` | Manifest | Android's backup and device transfer carry the step history, the profile and the settings, an allowlist (`docs/adr/0007-backup.md`). No permission: Android sends the copy, not Passo |
| `<uses-feature android:name="android.hardware.sensor.stepcounter" android:required="true"/>` | Feature | Documents the requirement; filters devices on a future Play listing. It does not block APK installs, so the app also checks the sensor at runtime |

**Forbidden, by name:** `INTERNET`, `ACCESS_BACKGROUND_LOCATION`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `HIGH_SAMPLING_RATE_SENSORS`, `BODY_SENSORS`.

**An allowlist** (since Phase 15, 18 Nov 2026; `docs/adr/0016-location.md` decision 2). A CI task checks each variant's merged manifest and fails unless it requests exactly the permissions above, written in `build-logic` (`AllowedPermissions`): one more fails the build, a forbidden one by name, and so does one on the list that nothing requests any longer. Letting location in made the gate stricter than the list of forbidden names it was. A library that adds a permission must be removed or patched with `tools:node="remove"`. Beside the table, the list holds the one permission AndroidX declares for the app itself, named after the application id (`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`).

---

## 11. Delivery phases

Each phase ends with a merged PR, green CI and its acceptance criteria met.
Phases 1 and 4 need **field testing on a physical device**; an emulator is not enough for sensors or batching.

The phases done, with their acceptance and their boxes as they stood, are frozen in the archive:
0 to 8, 10, 11, 13 and 14 in [`docs/planning/01-phases-0-14.md`](docs/planning/01-phases-0-14.md),
12 in [`docs/planning/03-towards-1.1.0.md`](docs/planning/03-towards-1.1.0.md); the boxes they left
open are in «Open» above. Here stay the phases not done yet.

### Phase 9 — Google Play (optional, later)

Kept open, not planned yet. Notes to avoid closing the door:

- [ ] Enroll in Play App Signing **with the existing release key** (upload your own key during enrollment), so builds from GitHub and from Play share the same signature and users can move between them without reinstalling
- [ ] Build an AAB in addition to the APK
- [ ] Privacy policy (e.g. on GitHub Pages): no data collected or shared
- [ ] Play Console:
  - foreground service permission declaration (`health` use case, with a demo video)
  - health apps declaration, if required
  - Data safety form
  - content rating
- [ ] Store listing in Italian and English, with screenshots and a feature graphic
- [ ] Internal test track, then closed testing (check the current tester requirements for personal developer accounts), then production

### Phases 15 to 20 — Location, for an outing that asks for it

Asked for and approved by the owner on 18 Nov 2026 (archive 04, «Location, for an outing that asks for
it»). The decisions, with their reasons and the platform's rules, are in
`docs/adr/0016-location.md`: **read it whole before any of these phases**; the sections below
say what to build and in what order, not why.

In one paragraph: location is a measuring tape, never a tracker. It is read only inside an
outing the reader started with «Measure with GPS» on; the day's count, the widgets and every
screen are untouched for a reader who never turns it on. `INTERNET` and
`ACCESS_BACKGROUND_LOCATION` stay forbidden. The steps and the fixes are fused (the steps say
when the reader moves, the fixes how far); routes live in a database of their own that Android's
cloud backup never carries; the day's distance stays the steps'.

| Phase | Name | Brings | Needs |
|---|---|---|---|
| 15 | The measured outing («Uscita misurata») | Permissions and the gate, the receiver, the fusion, the storage, the switch, the numbers | nothing before it |
| 16 | The outing's page | The route drawn, splits, climb, GPX and image, Your data | 15 |
| 17 | The step that learns («Il passo che impara») | The step length by cadence, from the outings with GPS | 15 |
| 18 | City walks measured with GPS («Passeggiate misurate») | A city walk's distance measured on the reader's own streets, and their real route kept; the walk stays imaginary | 15, 16 |
| 19 | Dropped (owner, 19 Nov 2026) | Was «On the spot: the ways»; the first Phase 18, «On the spot: the cities», went with it | |
| 20 | Your usual round («Il tuo giro solito») | A walk the reader repeats, known without being told and compared with their usual; Your rounds | 16, 21 |

**Phase 21, the open outing, comes first** (owner, 20 Nov 2026; §15, «The open outing
planned»). It is not a location phase: an outing with no goal, with GPS or without. It is planned
beside these because the outings with GPS asked for it and Phase 20 builds on it (a usual round is
walked as an open outing). Phase 23, the doors (the ways in that were missing, and a list of
the outings), follows it. Order: 21, 23, then 18, then 20. Phase 22, the walk already begun, is
later, when the owner asks. «The bar» below holds for 21, 22 and 23 as for these.

**One phase, one session.** Each section is written to be enough for a session that has read
CLAUDE.md, ADR 0016 and that section: it names the files to read first, the steps in order, the
acceptance, the edge cases (each a test) and what only the owner's phone can check. A session
starts in plan mode on its section, builds on a branch, and ends by ticking its boxes here,
writing its entry at the end of §15's log (what was built, what was decided on the way, what was
checked) and opening its device box in «Open». A decision that departs from ADR 0016 is written
there as an amendment, with its date and reason, before the code that needs it.

**Releases.** Phases 15 and 16 are released together or not at all (ADR 0016, consequences);
17 to 20 may each follow in a release of their own, in this order unless the owner changes it;
18, being small, may go out with 17. 21 may go out with 15 to 17, or in a release of its own
before 18.

**The bar** (owner, 18 Nov 2026: "precise work, premium quality as this app is; no compromises,
no shortcuts, complete and feasible features, premium in the code and in the design and
UI/UX"). It holds for every step below and is part of each phase's acceptance:

- *Code.* The logic is pure Kotlin in `:core:domain`, written with its tests: every row of a
  phase's edge-case table is a test that says so in its name. The service stays the one writer;
  a page's ViewModel is a `stateIn(WhileSubscribed)` and collects nothing in `init`. Constants
  carry their reason and, where there is one, their source. No `!!`, no `TODO`, no stub, no
  switch or row for something not built yet, no new dependency. `spotlessCheck`, `test`,
  `:app:lintDebug` and the permission check are green before the phase is called done.
- *Design.* Passo's own language (`docs/adr/0004-design-language.md`): one sentence before any
  number, every number with the line that says what it means, colour, type and shape as roles,
  icons from `PassoIcons`, maps and charts in Canvas, every animation a fade under reduced
  motion. Every state is designed, not only the happy one: nothing yet, waiting for the
  satellites, refused, turned off on the phone, under way, lost and found again, done. The
  screenshots of each new or changed screen are looked at in both themes and both dresses
  before the phase ends, and the README's are regenerated where one changed.
- *Reach.* `assertAccessible()` on every state drawn, `walkPage()` at twice the text size and on
  an open foldable, `pageGutter()` on each side, a sentence for TalkBack for every map and
  chart, the vibrations felt before they are chosen (the editor's "Try them").
- *Words.* Every string a resource in English and Italian, plurals where a count appears.
  «Measured» / «misurati» only for a distance at least nine tenths from fixes (ADR 0016
  decision 6); an estimate still says it is one. A refusal is told once, with what it costs and
  the way back, and never asked again by itself. Nothing that describes the Ways names a faith
  (CLAUDE.md, «The Ways' words»). The sister apps are never named in code or strings.
- *The record.* CHANGELOG's `[Unreleased]`, the guide's chapter, VISION and this file kept true
  in the same change; a README screenshot where the phase brings something worth showing.

### Phase 15 — The measured outing («Uscita misurata»)

An outing whose plan has «Measure with GPS» on reads the satellites while it counts: its
distance is measured, it has a pace per kilometre, and its route is kept. This phase lets
location into the app, so it is also the change that amends the documents and the build's gate.
It ends with numbers on the card and the notification; the route's own page is Phase 16.

**Read first:** `docs/adr/0016-location.md` (all), `docs/adr/0009-sessions.md`,
`docs/adr/0013-interval-walks.md` (the wake lock), `docs/adr/0007-backup.md`; here §3, §4.5, §9,
§10. In the code: `build-logic/.../ForbiddenPermissions.kt` and the plugin that wires it;
`core/tracking/src/main/AndroidManifest.xml`; `StepTrackingService.kt` (`onStartCommand`,
`applyRegistration`, `onSessionSteps`, `onSessionSignals`, `publishSession`),
`StepSensorSource.kt`, `SignalWakeLock.kt`, `StepTracking.kt` (`TrackingReadiness`),
`SessionControl.kt`, `SessionNotifications.kt`, `NotificationText.kt`; `SessionTracker.kt`,
`SessionConstants.kt`, `SessionAnnouncement.kt`, `SessionProgress.kt`; `Session.kt`
(`:core:model`); `Entities.kt`, `PassoDatabase.kt`, `SessionMapping.kt`, `SessionDao.kt`,
`TrackingRepository.kt` (`persist`); `BackupFile.kt`, `BackupCodec.kt`, `BackupMerge.kt`,
`CsvExport.kt`; `app/src/main/res/xml/data_extraction_rules.xml`; `PlanEditorScreen.kt`,
`PlanEditorViewModel.kt`, `SessionsScreen.kt`; `SessionCard.kt`, `WalkList.kt`.

- [x] **The platform, read again.** Android 17's official pages for anything on location (the
  press's "Location Button", the permission's dialog, the `location` service type), since Passo
  targets API 37. What is found is written in this phase's §15 entry; if it changes the
  permission's flow, ADR 0016 is amended first. Read on 18 Nov 2026 (archive 04, «The five answered,
  and the platform read again»): nothing changes the flow; the location button is an alpha
  Jetpack library for momentary access, not for a walk measured by a service (ADR 0016,
  the platform's facts).
- [x] **The gate becomes an allowlist** (ADR 0016 decision 2). `build-logic` holds the exact
  list of permissions the merged manifest may request (today's, read from a green build's
  report, plus the three below); the task fails on any permission not on it, and names a
  forbidden one as forbidden (`INTERNET`, `ACCESS_BACKGROUND_LOCATION`, the exact alarms, the
  battery exemption, the high-rate and body sensors). **Exactly, both ways** (the ADR's word): a
  permission on the list that the manifest no longer asks for fails the build too, so the list
  cannot keep a name nothing needs. A permission named after the application id (a library's
  own, which differs between the debug build and the release by the `.debug` suffix) is written
  relative to the id and resolved per variant. The task keeps its name,
  `checkForbiddenPermissions`: the workflows and the documents call it. The verdict is a pure
  function (requested, allowed, forbidden) with its own tests in `build-logic`. Proven as in
  Phase 0: an unlisted permission and `ACCESS_BACKGROUND_LOCATION`, each added to
  `:core:tracking`'s manifest, turn the build red by name; then removed.
- [x] **The manifest** (`:core:tracking`): `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`,
  `FOREGROUND_SERVICE_LOCATION`; the service's `foregroundServiceType="health|location"`;
  `<uses-feature android:name="android.hardware.location.gps" android:required="false" />`.
  The manifest's opening comment is rewritten to say what each is for and that nothing else is
  held.
- [x] **The documents, in this same change** (ADR 0016, «Amends»): VISION.md (principle 1:
  steps are the core, and location, when the reader asks for it, measures what the steps
  estimate, never replaces them, and nothing depends on it; principle 3; the non-goals' GPS
  lines; the success criterion «Privacy is enforced»; the outings' section; the glossary:
  «measured»); §9 here (rule 9, written like rule 7: when, what bounds it, what it costs once
  measured, the receiver while an ended outing can be taken back included; and rule 7 itself:
  its wake lock is also an outing with GPS's on a phone with no wake-up counter, and is
  bounded ten minutes past the outing's cap, not at 4 h 10 min) and §10 (the table, the
  forbidden list, the allowlist); CLAUDE.md (the two invariants, the battery one saying «for
  an outing with signals or with GPS» where it says «with signals only», the modules table);
  the README (the privacy table and every "no location" line, with no em or en dash); ADR
  0007, ADR 0009 and ADR 0013 (an «Amended» line each, pointing to ADR 0016);
  the strings that promise no location (`ways_intro`, `settings_calibrate_note`, the guide's
  «Your distance, not your location» and the first run's promises: grep the `strings.xml` files
  for "location", "posizione" and "GPS" and rewrite each so that it is true: the ways from home
  still use no location, and say so). Nothing may still say that Passo cannot know where the
  phone is; everything still says that Passo sends nothing.
- [x] **The model** (`:core:model`). `SessionPlan.gps` (false by default); `Session.gps` (copied
  at the start, frozen like the goal), `Session.stepsMeters` (what the steps alone said),
  `Session.measuredShare` (0 to 1, null without GPS). In an outing with GPS
  `SessionTotals.distanceMeters` is the fused distance (ADR 0016 decision 5): the goal, the
  signals, a walk's places and the energy read it unchanged. `Session.capAtMillis` (when it ends
  by itself if nobody says to go on: four hours from the start, moved on by «Keep going») and
  `Session.capAskedForMillis` (the cap the question was last asked for: asked once a cap, and
  not again by a process that restarted). `RoutePoint` (time, latitude, longitude, altitude or
  none, accuracy, whether it is a fix or a point laid from the steps) and `Route` (the outing,
  its points, its box).
- [x] **The domain** (`:core:domain/routes`, pure, tests first):
  - `RouteFilter`: a fix is accepted at 25 m of accuracy or better, later than the one before,
    and no faster from it than the cadence allows (3.5 m/s below `RUNNING_CADENCE`, 8 m/s at or
    above). A mock fix is accepted only in a debug build (the flag is passed in), so a route
    can be replayed on a desk with the system's mock-location setting and never fakes a walk in
    a release.
  - **One clock for the two streams.** A fix's time is its `elapsedRealtimeNanos`, turned to
    wall time by the same snapshot the ledger uses for a step sample (`StepSample.eventElapsedNanos`),
    never `Location.getTime()`: that is the satellites' clock, which can stand seconds from the
    phone's, and steps and fixes laid on two clocks would be interleaved wrongly. «Later than
    the one before» is read on that clock; a clock set by hand mid-outing moves both together.
  - `RouteMeter`: fed the accepted fixes and the accounted steps with their own times, in any
    interleaving, it gives the fused distance, the steps' own, the measured share, the pace now
    (over the last 200 m) and the points to keep (one every 3 m moved or 5 s, whichever first).
    Its rules are ADR 0016 decision 5, one test each: the stretch before the first fix; no
    steps, no distance; movement without steps; a gap covered by the steps and never shorter
    than the straight line across it; the receiver asleep.
    **Steps ahead of their fixes.** The two streams reach the service in batches of their own,
    so the steps of a stretch can be in hand before its fixes. A stretch is *open* until a fix
    later than its last step is in, or until `OPEN_STRETCH_MILLIS` has passed without one (then
    it is a gap: the steps' for good, and never shorter than the straight line once the next
    fix comes). An open stretch is counted from the steps for what the card shows; the goal,
    the milestones and a walk's places are told on the *settled* distance only, so nothing is
    told, and no outing ended, on metres the fixes then take back. The settled distance never
    goes down.
  - `GpsDuty`: when to ask for updates and when to remove them (asleep after 2 minutes without
    a step, awake at the next one, off at a pause and at the end), as `SignalWake` decides the
    wake lock: pure, the service only obeys. It is asked at whatever arrives, a step batch or
    a batch of fixes, never by a timer. While an ended outing can still be taken back
    (`canKeepGoing`), it answers as while the outing counts, and off once it cannot or the
    card is put away (ADR 0016 decision 14, amended).
  - `SignalWake.needed` and `neededFor` gain the outing's `gps`: on a phone whose step counter
    cannot wake it, an outing with GPS keeps the processor awake while it counts, with signals
    or without (ADR 0016 decision 5, amended), since there the steps steer the receiver. Let
    go as before: a pause, the end, 15 minutes still. The editor's line about the wake lock
    follows `neededFor`.
  - `RouteCodec`: points to chunks and back, as offsets in varints (time in seconds, latitude
    and longitude in millionths of a degree, altitude in decimetres, accuracy in metres): exact
    round trip, about 10 KB an hour of walking, a chunk self-contained so a lost one loses only
    itself.
  - `Pace`: time in motion over distance, formatted by `MeasureFormatter` per kilometre or
    mile as the units say; none under 200 m.
  - `StepLengthSample`: for each stretch measured from end to end for a minute or more at a
    steady cadence (within 8 steps a minute), its cadence, steps and metres. `RouteMeter` emits
    them; Phase 17 learns from them, so the first outings already teach.
- [x] **The storage** (`:core:data`). `passo.db` schema v7 by auto-migration: `session_plan.gps`,
  `session.gps`, `session.stepsMeters`, `session.measuredShare`, `session.capAtMillis` and
  `session.capAskedForMillis` (an outing from before v7 reads its cap as four hours from its
  start), and a table
  `step_length_sample` (outing, cadence, steps, metres): numbers, never places. A second Room
  database, `routes.db` (`RouteDatabase`, its schema exported like the first): `route` (the
  outing's id and start instant, the points' count, the box) and `route_chunk` (outing, order,
  bytes). `RouteRepository` appends a chunk after the step batch's transaction and never inside
  it: `passo.db` stays the truth, and a crash costs a route its last chunk, not the outing.
  Deleting an outing deletes its route, **the route first**: two databases share no
  transaction, and a crash between the two must leave an outing without its route (a state
  the app already draws), never places with no outing. That holds for an outing too short to
  be kept (`MIN_KEPT_STEPS`), which the service deletes, and for one a restore brought without
  `routes.db` (the cloud copy: its numbers stay, said as they were). At each start of the
  service, a route whose outing is not in `passo.db` is deleted: one read, no wake of its own.
  `UpgradeFromFirstReleaseTest` walks to v7.
- [x] **The backup** (ADR 0016 decision 9). `data_extraction_rules.xml`: `routes.db` and its
  write-ahead log under `<device-transfer>` only, with the comment saying why. The JSON backup
  and the CSV gain the outing's new numbers as added fields (`gps`, `stepsMeters`,
  `measuredShare`; no new format version), the plan its `gps`; the routes themselves are
  Phase 16's. The JSON also carries each outing's `step_length_sample` rows, as an added field
  of the outing (numbers, no places): what Phase 17 learns from must not be lost by a reader
  who moves phone with their own file; an outing the import leaves out leaves its samples out.
- [x] **The receiver** (`:core:tracking`). `LocationSource`, beside `StepSensorSource`:
  `LocationManager`, `GPS_PROVIDER`, a `LocationRequest` at 1 s, high accuracy, with
  `setMaxUpdateDelayMillis` asked for (60 s with the screen off, none with it on), batches
  taken through `onLocationChanged(List<Location>)`, `requestFlush` when the screen comes on
  **and at each step batch**: the steps lead, as everywhere in Passo. The step batch has
  already woken the processor (ADR 0009's two brief wakes a minute); the fixes are asked for
  in that same wake, and the meter moves when they are in (`onFlushComplete`, or
  `FLUSH_TIMEOUT_MS` as for the sensor's). The receiver's own delay is twice the steps', so by
  itself it wakes the phone only when no step came: two streams, one rhythm of wakes, not two.
  Whether the phone batches at all is measured, never assumed (ADR 0016 decision 10).
  Only the points kept (one every 3 m or 5 s) have their altitude turned to mean sea level by
  `AltitudeConverter`, off the main thread: the others are measured and let go.
  `GpsReadiness`: ready, permission needed, approximate only, turned off on the phone, no
  receiver.
- [x] **The service.** One function adds or drops the `location` type (`startForeground()` with
  `health|location`, or `health`), called when an outing with GPS starts, resumes, pauses or
  ends, and only from where the while-in-use rule allows it (ADR 0016 decision 4); a refusal is
  caught, logged, and the outing goes on from the steps. **`onStartCommand` asks that same
  function which types to pass**: today it calls `startForeground()` with `health` alone every
  time the app is opened and at every command, which in an outing with GPS would take the
  `location` type away at each look at the app. And a refused `location` type must never reach
  the catch that stops the service: it falls back to `health` and the count goes on (no lost
  steps); only a refused `health` stops it, as today. The fixes reach `RouteMeter` on the
  thread that drives `SessionTracker`; the outing's distance in a GPS outing comes from the
  meter; the chunks are written with each batch. A process restarted mid-outing rebuilds the
  meter from the outing's stored numbers and goes on from the steps. **The stop is told, and
  GPS comes back by itself** (ADR 0016 decision 4, amended): once, a notification on the
  `sessions` channel («GPS stopped. Your outing goes on from your steps», with «Resume GPS»)
  and the voice if the outing speaks, no vibration of its own; «Resume GPS» on the counting
  notification and on the card; and with no touch at all the next time the app is brought
  forward (`onStartCommand` from the activity: an outing with GPS whose receiver is off, the
  permission granted, asks for the type again). With the permission gone the button opens
  the app. **"Walk now" keeps the plan's word**: the evening reminder's outing starts with GPS
  when the rest-of-the-day plan has it on, the precise permission is already granted and
  location is on; otherwise on the steps, and the card says so once. It never asks for the
  permission. The diagnostics log gets
  one row an outing with GPS (fixes taken and refused, the measured share, how long the
  receiver slept, the type refused or not), like the interval outing's.
- [x] **The longest outing asks before it ends** (ADR 0016 decision 14; it amends ADR 0009
  decision 6, for every outing, with GPS or not). `Session` keeps its own cap (four hours from
  the start, stored, so an answer survives a restart). At the first step batch within 15
  minutes of it, once per cap: the stillness's two long pulses and a notification on the
  `sessions` channel, «Still walking? This outing ends in 15 minutes», with «Keep going»; the
  card says the same while it is on screen. «Keep going», from either, moves the cap four hours
  on. Unanswered, the outing ends at its cap, is told (it was silent: `SessionEnd.CLOSED` was
  neither told nor reopenable), and can be taken back for `KEEP_GOING_MILLIS` with the steps
  since, as after the goal; in an outing with GPS the receiver stays on through that window
  and its points are kept aside, joined to the route if the outing is taken back and dropped
  if not. **The same after the goal and after an end by stillness** (ADR 0016 decision 14,
  amended): while an ended outing with GPS can be taken back, the receiver is steered as
  while it counts (asleep 2 minutes after the reader stops), the service keeps the `location`
  type, and `RouteMeter` keeps what comes aside, as the tracker keeps the steps; «Keep going»
  or «Resume» joins them, and putting the card away, a new outing or the 15 minutes' end
  drops them and turns the receiver off. `SignalWake` follows the outing's own cap, and so does the bound of its lock
  (`TIMEOUT_MILLIS`, today ten minutes past four hours from when it is taken): the lock is
  taken again at «Keep going» with ten minutes past the new cap, and §9.7's "bounded at 4 h 10
  min" is rewritten as "ten minutes past the outing's cap" in the documents' box above. Whether
  an end can be taken back is the tracker's own knowledge (`reopenableFrom`), never read from
  `SessionEnd.CLOSED`, which an import also gives to an outing that was under way in the file.
  No timer: every part rides on a step
  batch, a screen-on or a touch. The voice, if the outing speaks, says the question too.
- [x] **The switch, and the permission in context** (`:feature:sessions`). In the editor,
  «Measure with GPS» / «Misura con il GPS», with two lines under it: what it gives (a measured
  distance, a pace, the route) and what it costs (the battery, in the words the owner's
  measurement allows; until it is measured, "more battery, only while you walk"). Absent on a
  phone with no receiver. The permission is asked when an outing with GPS is first started,
  both permissions in one request, after one screen in Passo's own words that says when
  location is read and that it is never sent; granted precise, the outing starts with GPS;
  approximate only or refused, it starts on the steps and its card says so once, with the way
  to change it (the upgrade request, or the system's page after a second refusal). Location
  turned off on the phone: the same, with the button to the system's switch. None of the
  four presets has it on.
  **One door for every start.** An outing starts from the Outings page (`SessionsViewModel`),
  from a launcher's shortcut (`MainActivity.startOutingIfAsked`, which goes straight to
  `SessionControl`) and, from Phase 18, from a walk's page: three modules that do not know each
  other. The check (`GpsReadiness`), Passo's own screen and the request live behind one door
  in `:app`, where the activity that owns the permission's launcher is; each start goes through
  it, by a lambda as the pages' other crossings do, so no start with GPS can skip the screen
  and none asks twice.
  **«Only this time»** is a grant: the outing starts with GPS. The system takes it back by
  itself later, and the next outing with GPS asks again, by the system's dialog alone (Passo's
  own screen is shown once). How long it lasts under a service that never stops is not written
  anywhere: proven on the phone, below.
- [x] **What the reader sees while it counts.** The card (`SessionCard`): «Finding the
  satellites» until the first fix, then the distance with «measured» under it (or the share
  from the steps when over a tenth), the pace and the pace now, the cadence as before; the
  counting notification and its Live Update the same in words; the voice, if the outing
  speaks, says the pace at each signal. The widgets' and the tile's line are unchanged. The
  outing's rows in Today and History show the measured distance with its quiet mark. Every
  state of the receiver has its line on the card, and one only: finding the satellites,
  measuring, asleep while the reader is still (nothing said: the numbers stand), the signal
  lost (a tunnel), location turned off on the phone (with the button to the system's switch),
  GPS stopped (with «Resume GPS»), started on the steps (approximate only, refused, or "Walk
  now" without the permission: said once, with the way to change it).
- [x] **Tests.** `RouteFilterTest`, `RouteMeterTest`, `GpsDutyTest`, `RouteCodecTest`,
  `PaceTest`, the tracker's with a meter; Robolectric for the two databases, the migration and
  the service's type change (granted, refused, restarted, the app opened mid-outing); the
  editor's and the card's UI tests with every state above, `assertAccessible()`, `walkPage()`;
  the allowlist's (unlisted, forbidden, listed and missing, named after the application id);
  the backup's round trip with the samples, and an older file without them. Built (archive 04,
  «Phase 15, what the reader sees, and the documents»): the type change is tested on
  `ForegroundTypes`, the class the service changes its types through, not on the service
  whole.
- [x] Strings in English and Italian; CHANGELOG; the guide's outings chapter (a short «With
  GPS» part: when it is on, what is kept, what is never sent).
- [ ] **On a device (owner), the proof first:** start an outing with GPS from the editor, from
  Today and from the launcher's shortcut; the screen off for ten minutes; pause and Resume
  from the notification. The log says the type was added each time and fixes kept coming with
  the screen off. The app opened and closed during the outing: the type is still there. The
  permission given as «Only this time»: whether the outing keeps its fixes to its end with the
  app out of sight, and what the next outing finds. "Walk now" from the evening reminder, the
  plan with GPS on, the app not open: the type added from the notification's touch. The
  process ended mid-outing (the developer options, or `adb shell am kill`): the stop told,
  and GPS back when the app is opened. An outing with GPS and no signal on the owner's phone
  (no wake-up counter): a stop of five minutes, and fixes again from the first steps after
  it. If the second type is refused on a running
  service, the fallback of ADR 0016 decision 4 is built before anything else in Phase 16.
  **This box comes before what depends on its answer** (ADR 0016 decision 4: "Phase 15 proves
  which one it is before building on it"): the gate, the manifest, the plan's `gps`,
  `LocationSource`, the function that changes the type, the log's row, the switch and the
  request were built first, as a thin slice, and its debug APK is the owner's to try. What
  hangs on the answer is one thing, how the service comes by its fixes (a second type, or the
  fallback's second service): "everything above the source is the same" (the same decision).
  So the fusion, the storage and the screens are built meanwhile (owner, 18 Nov 2026, unable
  to test for now: "can you go on safely, and in keeping with the plan?"; archive 04, «Phase 15, the
  domain»), and nothing is released, nor Phase 16 called done, before this box is answered.
- [ ] **On a device (owner), the measure:** a 30-minute and a 2-hour outing with GPS, screen
  off, phone in a pocket: the battery an hour (`dumpsys batterystats`, the system's page), the
  measured share, the receiver asleep at a stop and awake at the first step; a known distance
  (a track, a measured path) against the fused one. Numbers in `docs/battery/`, then the
  editor's cost line and §9's rule 9 say the figure. The record says which phone it was and
  whether it held the wake lock of §9.7 (the owner's has no wake-up counter, so with signals
  on it does, and batching saves nothing there): the figure is that phone's, not every
  phone's, and the cost line is worded so that it stays true on both kinds.

**Acceptance:** with the switch off, no screen, number, permission or wake differs from
1.1.0's; with it on, an outing on a known distance reads within 2% of it under an open sky,
ends by itself as every outing does, and its cost an hour is measured and written; the build
fails on any permission outside the list.

**Edge cases** (`RouteMeterTest`, `RouteFilterTest`, `GpsDutyTest`, the service's tests):

| Case | Expected behavior |
|---|---|
| No fix for the first minute | The outing counts from its first step; that stretch is the steps', and in the share |
| A fix 60 m off between two good ones | Refused; the line goes from good to good |
| Standing ten minutes, fixes wandering | No distance, no points; the receiver asleep after 2 min; the outing ends at 15 as always |
| A tram between two walks, phone in a pocket | Movement without steps adds nothing; the walk resumes where the steps do |
| A tunnel of 300 m | Covered by the steps, never shorter than the straight line across; drawn later as laid from the steps |
| A sensor with no FIFO handing 170 steps at once | The meter lays them over their time in motion, as the tracker does |
| Paused | The receiver off, the type back to `health`; Resume from the notification brings both back |
| The permission revoked mid-outing | Android ends the process: it is the restarted process below, with the permission gone. The outing goes on from the steps and says so; the way back opens the app, where it can be asked, never a button that would be refused |
| The permission given «Only this time» | The outing starts with GPS; the next one asks again, by the system's dialog alone |
| The app opened, or a button of the notification touched, mid-outing | `onStartCommand` passes `health\|location` again: the type and the fixes stay |
| The `location` type refused where the rule does not allow it | Back to `health`, logged; the service and the count go on, the outing from the steps |
| A fix whose satellite time is 5 s from the phone's | Laid by its elapsed time, on the steps' clock: in its place among them |
| The steps of the last 30 s in hand, their fixes not yet | An open stretch: shown from the steps, no goal or place told on it; told once the fixes are in, or the stretch is a gap |
| The goal 20 m away on the steps' estimate, the fixes say 45 m | Not ended: the goal is told on the settled distance |
| An outing with GPS ended under 30 steps | Not kept: its route deleted first, then the outing |
| A crash between deleting a route and its outing | An outing without a route; the next start of the service finds no route without an outing |
| A restore from the cloud copy, with outings measured by GPS | Their numbers as they were, no route: `routes.db` was never in that copy |
| «Keep going» on a phone that keeps the processor awake (§9.7) | The lock taken again, bounded ten minutes past the new cap |
| Approximate only granted | No GPS, said once, with the way to precise |
| Location turned off on the phone, mid-outing | As the tunnel, until it is back; the card says it |
| The process restarted by the system | The outing continues from the steps; told once (notification, voice); «Resume GPS» on the notification and the card |
| The app brought forward after that restart | The receiver is back with no touch; the stretch between is the steps', never shorter than the straight line |
| "Walk now", the plan with GPS on, the precise permission granted | Starts with GPS: a touch on a notification |
| "Walk now", the plan with GPS on, no permission | Starts on the steps; the card says so once; nothing is asked from a notification |
| A phone with no wake-up counter, an outing with GPS and no signal | The processor kept awake while it counts (§9.7, amended): the receiver asleep at a stop, awake at the first step |
| The goal reached with GPS, the reader walks on, «Keep going» 10 minutes later | The receiver never stopped: the route has no hole |
| The goal reached with GPS, the card put away | The receiver off at once, the `location` type dropped, the points kept aside dropped |
| The goal reached with GPS, the reader sits down | The receiver asleep after 2 minutes, off at 15 |
| Ended by stillness, noticed by a step, «Resume» | The receiver awake from that step; the stillness adds nothing, the route goes on from where it stopped |
| An interval outing with GPS | The changes still ride on the steps (ADR 0013); the fixes change nothing of their timing |
| 3 h 45 min into an outing, walking | Asked once, by vibration and notification; nothing stops |
| «Keep going» before the cap | The cap is four hours later; asked again 15 minutes before that one |
| The question unanswered | Ended at four hours and told; «Keep going» for 15 minutes takes it back with the steps and the route since |
| Taken back 10 minutes after its cap, with GPS | The receiver never stopped: the route has no hole |
| Not taken back | The receiver stops 15 minutes after the cap; the points kept aside are dropped |
| 3 h 45 min into an outing, sitting since 3 h 35 | Not asked: no step brings the question, and the stillness ends it at 3 h 50 as always |
| The process restarted after «Keep going» | The moved cap is read from what was stored |
| A city walk from home | Never with GPS (ADR 0016 decision 13, made plain): its outing has `gps` false, and no switch offers it; Phase 18, re-planned on 19 Nov 2026, gives the walk's page a switch of its own |
| A mock fix in a release build | Refused |

### Phase 16 — The outing's page: the route, the splits, the climb

Every outing gets a page of its own; one with GPS shows its route drawn, its kilometres one by
one and how much it climbed, and can be shared as an image or a GPX file. With this phase the
measured outing is whole and can be released (with Phase 15).

**Read first:** ADR 0016 (decisions 5 to 9 and 11), `docs/adr/0014-ways.md` (decisions 8 and 9:
how a map is drawn), `docs/adr/0012-foldables-and-accessibility.md`,
`docs/adr/0011-export-import-and-calibration.md`. In the code: `WayMap.kt`, `ContinentMap.kt`
and `WayProjection.kt` (a map in Canvas, a touch that names), `BarChart.kt`,
`DayTrendChart.kt`, `SessionCard.kt`, `WalkList.kt`; `Ways.kt` and `Geo.kt` (a city's frame and
map); `feature/year/.../YearImage.kt` and that module's manifest (an image shared through the
app's own provider); `DataSection.kt`, `DataViewModel.kt`, `BackupRepository.kt`;
`MetricsCalculator.kt`, `MetricsConstants.kt`; `app/.../shell/PassoNavigation.kt`; the
`ReadmeScreenshots` class of `:feature:sessions`.

- [x] **The climb** (ADR 0016's facts on altitude). `:core:tracking`: `PressureSource`
  (`TYPE_PRESSURE`, no permission), registered only while an outing with GPS counts, batched,
  never waking the processor by itself. `:core:domain/routes`: `Altitude` (the barometer for
  the short term, the satellites' altitude above mean sea level for the long term, blended so
  that the weather's drift over hours does not read as a hill; satellites alone on a phone
  with no barometer) and `Climb` (metres up and down, counted only past a threshold that
  noise does not cross: 3 m with a barometer, 10 m without). Each point keeps the blended
  altitude; `route` keeps which source it was; `session` gains `ascentMeters` and
  `descentMeters` (schema v8, or v7 if Phase 15 is not released yet: one migration a release).
  Said as an estimate («about 240 m up»). Not floors: VISION's non-goal stands.
- [x] **Energy on a slope**, for an outing with GPS only: the walking cost per kilometre gains
  the grade's term from the ACSM walking equation (which on the flat is the 0.5 kcal/kg/km
  `MetricsConstants` already has), no credit downhill, the running cost its own; the formula
  and its sources in `MetricsConstants.kt`, a test at 0, 5 and 10%. The day's energy stays the
  steps' (ADR 0016 decision 6).
- [x] **Splits** (`:core:domain/routes`, `Splits`): one a kilometre or a mile, as the units say,
  each with its time in motion, pace, cadence and climb; the last, partial one says its length.
- [x] **`RouteMapView`** (`:core:designsystem`), Passo's first map that moves: north up, pinch
  and double tap to zoom, drag to pan, a button back to the whole route; under reduced motion
  no glide. The line in the goal's colour, dashed where it was laid from the steps (the honest
  mark), a ring at the start, a bead at the end, a small bead each kilometre, a scale bar in
  the reader's units. The ground is plain, everywhere (amended 20 Nov 2026, §15 «Every route
  on plain ground»: a carried city's map under a route inside its frame came out); under the
  map, one line says what a dash is, only when the route has one. A touch names the kilometre
  and its pace. One sentence to TalkBack («Your route: 5.2 km, from north to south, 3 stops»);
  the splits below are its words. Its gestures are a piece of their own, since Phase 20 reuses
  them on a round's page (Phases 18 and 19 «On the spot», which would have too, were dropped
  on 19 Nov 2026). Drawn in every theme and dress in a gallery test, as the ways' maps are.
- [x] **The outing's page** (`:feature:sessions`, a new entry in `PassoNavigation.kt`), for
  every outing, with or without GPS: the sentence first (what it was and how it went against
  its goal), then its numbers, each with its line; for an interval outing its intervals, as
  the card has them; for one with GPS the map, «5.20 km measured; your steps said 5.05», the
  splits as bars (the fastest marked, the goal's pace as a line when the outing had one), the
  profile of the climb (a Canvas area over the distance), and what the route can do: share,
  save, delete. Without GPS: the same page without those parts and without an empty box in
  their place. Reached from the finished card («See the outing»), from an outing's row in
  Today and in History (a lambda from `:app`: the feature modules still do not know each
  other), and from the goal's notification.
- [x] **Sharing and saving** (ADR 0016 decision 9). `GpxCodec` (`:core:domain`, GPX 1.1: track,
  segments at the pauses, time, elevation), written by hand, no library. «Save as GPX» through
  the Storage Access Framework; «Share» as a GPX or as an image (the map, the sentence and
  three numbers, in the card's dress, as Your year on foot shares a page), through the one
  file provider the app already has: move `YearImage`'s plumbing where both features reach it,
  never a second provider. What is shared is the route as it was walked, whole (ADR 0016
  decision 9, amended 18 Nov 2026: no «Trim the ends»; no switch, no remembered setting). The
  image is a line on Passo's own ground and names no street; the GPX holds every point, and
  its choice says so in one line under it («The file says where you walked, from the first
  step to the last»), once, where the reader chooses, never as a dialog.
- [x] **Your data** (`:feature:settings`, `DataSection`). How many routes are kept and their
  size; the export's «With the routes» (off by default; the JSON then carries each route with
  its outing, as an added field); «Delete all routes», after a dialog that says the outings and
  their numbers stay. On an outing's page, «Delete this route». The import adds routes by their
  outing's start instant and never removes one (`BackupMerge`).
- [x] **Tests.** `ClimbTest`, `AltitudeTest`, `SplitsTest`, `GpxCodecTest` (a file another app
  reads: checked against the schema's shape, and a round trip), the map's gallery, the page's
  UI tests in each of its forms (`assertAccessible()`, `walkPage()`), the export and import
  with and without routes. Built (archive 04, «Phase 16, the outing's page»): the climb's tests are
  in `AltitudeTest` and `RouteHeightsTest`, the map's gallery is `RouteMapGalleryTest`, the
  page's forms `OutingScreenTest`.
- [x] Strings in English and Italian; CHANGELOG; the guide (the outing's page in the outings
  chapter); README: one screenshot, `outing-route.png`, the page of an outing with GPS (on
  plain ground since 20 Nov 2026), with its caption, and the feature list's line.
- [ ] On a device (owner): a walk with a hill and a tunnel or an arcade, then its page: the
  line where it was walked, the dashed stretch, the climb against a known figure; the GPX
  opened in a map app; the image shared to two apps; the page at a large text size, in both
  themes, on the open foldable, with TalkBack.

**Acceptance:** an outing with GPS opens a page that shows where it went, how each kilometre
went and how much it climbed, each number saying what it is; its GPX opens in another app on
the streets actually walked; the choice of a GPX says what the file holds; routes leave the
phone only by the reader's hand or in a phone-to-phone transfer.

**Edge cases** (`ClimbTest`, `SplitsTest`, `GpxCodecTest`, the page's tests):

| Case | Expected behavior |
|---|---|
| A flat walk under a passing weather front | No climb invented: the blend follows the satellites over the long term |
| A phone with no barometer | Satellites alone, the higher threshold, the same sentence, said as an estimate |
| An outing shorter than 1 km | One partial split; no "fastest" mark |
| A route with no accepted fix at all | The page of an outing without a map, with a line saying the satellites never answered |
| A route with every fix answered | No dash, and no line under the map (20 Nov 2026: was «a route half inside a carried city», gone with the city's map) |
| An outing whose route was deleted | Its numbers stay; the page has no map and no empty box |
| An import with routes for outings this phone lacks | The outings come first (as today), their routes with them |
| A pause of ten minutes | A new segment in the GPX; no line drawn across a place not walked |

### Phase 17 — The step that learns («Il passo che impara»)

Every outing with GPS has said, since Phase 15, how long the reader's step is at the cadence
they walked (`step_length_sample`). This phase turns those samples into a step length by
cadence, proposes it, and uses it for every estimate from then on: the distance of a day with
no GPS comes closer to the truth (ADR 0016 decision 12).

**Read first:** ADR 0016 (decisions 6 and 12), `docs/adr/0011-export-import-and-calibration.md`,
`docs/adr/0003-daily-summaries.md` (frozen days); here §5 and §6. In the code: `StepLengths.kt`,
`MetricsCalculator.kt`, `MetricsConstants.kt`, `ProfileLimits.kt`, `StepCalibration.kt`,
`Profile.kt`, `ProfileInputs.kt`, `UserPreferencesDataSource.kt`,
`TrackingRepository.kt` (how a profile change freezes the past first), `BackupFile.kt`
(`ProfileDto`), `CalibrationScreen.kt`, `CalibrationViewModel.kt`, `SettingsScreen.kt` (the
step length's rows).

- [x] **The learner** (`:core:domain/calibration`, `StepLengthLearner`, pure). From the samples:
  bins of 10 steps a minute from 80 to 180; a bin counts with 400 m or more in all, from at
  least two outings; its length is the median of its samples' (metres over steps), each sample
  within `ProfileLimits` for its gait (the walking step's below `RUNNING_CADENCE`, 140, the
  running step's from it); the curve is made non-decreasing with the cadence (a step does not
  shorten as the pace rises: pooled, weighted by steps, where the data says otherwise). It has
  two parts, split at `RUNNING_CADENCE`: walking and running. Within a part that has bins, a
  straight line between two bins and flat beyond its ends, up to the split; a part with no bin
  is not learnt and keeps the profile's own length (review, 19 Nov 2026: «beyond the ends flat»
  and «the profile's own elsewhere» said two things). It also gives the reader's usual walking
  cadence (the median step count of their active minutes, 40 steps or more, over the last 28
  days), the curve's length there, and what the curve would change: the last 28 days' distance
  with it against the lengths in use.
- [x] **The profile.** `StepLengthMode.LEARNED` and the curve in `Profile` and DataStore: its
  bins (cadence and length pairs), its usual cadence and the length there, the kilometres and
  outings it was learnt from. `walkingStepLengthMeters` and `runningStepLengthMeters` keep what
  the reader had: a length measured or typed is not lost by accepting; the curve remembers the
  mode it replaced, for the way back (the proposal's box). `StepLengths.forCadence`
  reads the curve where a part is learnt and the walking and running lengths elsewhere, so
  every caller that measures (the day minute by minute, an outing without GPS, a plan's
  estimate, the ways from home) follows with no change of its own. The callers that read one
  walking length (`walkingMeters`: Today's line under the distance, «at a step of 74 cm»; a
  city walk's steps before it is walked, `WalkScreen`; the step's rows in Settings and on the
  calibration page) read, in `LEARNED`, the curve's length at the usual cadence: the length the
  card named, so every screen says one number. Applied only when the reader accepts it,
  through the one door a profile change has (`TrackingRepository.changeProfile`), which freezes
  the past first; «Apply profile to past data» works with it as with any profile. An outing
  under way keeps the lengths it started with, as today. A running length the reader sets
  after accepting (typed, or the wizard's running walk, which changes no mode) wins over the
  curve's running part: the last one chosen is the one in use, gait by gait.
- [x] **The backup.** `ProfileDto` gains the curve as an added field and writes `LEARNED` as
  its mode. 1.1.0 was read for it (tag `v1.1.0`, `BackupCodec`): it ignores unknown fields and
  reads a mode it does not know as the height's estimate, without failing, so no other mode is
  written in its place; a file from this version imported by 1.1.0 falls back to the height,
  silently, which is what a downgrade may cost. From this version on, a file with a mode it
  does not know falls back to the height's estimate and says so in the import's summary. The
  last curve proposed (the proposal's box) is the phone's state, not the reader's choice: in
  DataStore, never in the file, like the first run; on a new phone the proposal comes once
  more.
- [x] **The proposal** (`:feature:settings`). When the bin holding the reader's usual walking
  cadence counts (the two around it, if it falls between two), and the curve would move the
  last 28 days' distance by 2% or more: a card at the step length's rows and on the
  calibration page, sentence first («Passo has measured your step on 14 km of your outings:
  at your usual pace it is 79 cm, not 74, so distances and calories read 6% higher»; calories
  are distance's (`MetricsCalculator.minute`) and move by the same share, and an honest
  estimate says so), the curve drawn in Canvas (the bins as points, the line through them, the
  length in use as a dashed line named by where it comes from, the height's, the wizard's or
  the one typed, cadence across and centimetres up, one sentence for TalkBack), «Use it» and
  «Not now». Never a notification. After «Use it», one line says what changed and what did not
  («From today, distances use your measured step. Past days keep theirs»), with «Apply to past
  days too»: «Apply profile to past data» offered where it is meant, asking as it does in
  Settings, never done unasked; without it History would show a step of 6% the reader could
  not explain (owner, 19 Nov 2026). The row then says where the length comes from and from how
  many kilometres, with a way back to what was in use before, named as it was: «Go back to
  your measured step (76 cm)», «Go back to the step you typed (76 cm)» or «Go back to the
  estimate from your height»; a calibration is never thrown away by an undo (owner, 19 Nov
  2026). Choosing
  another length while a curve counts (the wizard, or one typed) puts that curve away as «Not
  now» does: the card would otherwise come back at once, the curve differing from the new
  length by 2% or more. It is proposed again only when
  new outings move it by another 2% from the curve last proposed (the one put away with «Not
  now» or gone back from), never from the length in use, which differs by 2% already or nothing
  would have been proposed (owner, 19 Nov 2026). «Not now» puts the card away from Settings;
  the calibration page, where the reader comes on purpose, keeps a quiet row whenever a curve
  counts and is not in use («Passo has learnt your step on 14 km of your outings with GPS»),
  and touching it opens the card again, «Use it» and «Not now» with it: a reader who changes
  their mind does not wait for a new 2% (owner, 19 Nov 2026). The finished page of an outing
  with GPS gains one quiet line, «Your step at this pace: 79 cm»: from that outing's own
  samples (their metres over their steps), not from the curve, and only when it has some. The
  page of the outing whose samples made the proposal ready gains one more under it, «Passo can
  now use your measured step for every estimate», which opens the calibration page on the card
  (through `:app`'s navigation: features do not depend on each other): the moment the reader
  has just measured, where a card in Settings may never be seen. Shown while the proposal
  waits, gone once it is used or put away, on no other outing's page; never a notification
  (owner, 19 Nov 2026).
- [x] The wizard stays: a reader without GPS measures their step on a known distance as today.
  A length measured by the wizard and a learnt curve do not fight: the last one the reader
  chose is the one in use, and the row says which (a choice puts the curve away: the
  proposal's box).
- [x] **Tests.** `StepLengthLearnerTest`, `StepLengthsTest` with a curve (each part learnt or
  not, a running length set after), the repository's (accepting freezes the past first), the
  backup's round trip, an older file and a mode unknown, the settings' UI tests with the card
  in each state and the calibration page's row (shown, not shown, opening the card), Today's
  line and a city walk's steps in `LEARNED`, the way back to each mode, «Apply to past days
  too», the outing page's line to the card (shown, gone after a choice).
- [x] Strings in English and Italian; CHANGELOG; the guide («Estimates, and what they rest on»
  in Today and «With GPS» in Outings gain the step that outings with GPS measure; the guide has
  no «Measure your step», which is the calibration page's title); README: `calibration.png`
  regenerated if its screen changed, and its caption (today it says "no GPS").
- [ ] On a device (owner): after three or more outings with GPS, the proposal with figures
  that make sense against the wizard's own measure; accepted, a walk without GPS on a known
  distance reads closer than before; the card in both themes and at a large text size.

**Acceptance:** with enough outings behind it, a walk without GPS on a known distance reads
within 3% of it; nothing changes for a reader until they accept, and no past day changes unless
they ask.

**Edge cases** (`StepLengthLearnerTest`, `StepLengthsTest`):

| Case | Expected behavior |
|---|---|
| One long outing only | No proposal: a bin needs two outings |
| Samples only at a brisk cadence | A curve of one bin: that length across the walking part, flat up to 140; running keeps its own |
| A bin whose median is shorter than a slower bin's | Pooled: the curve never goes down |
| A sample outside `ProfileLimits` (a bad stretch) | Left out |
| The running bins empty | Running is not learnt: it keeps its own length, or the factor, as today |
| A running length set after accepting | It wins over the curve's running part; walking keeps the curve |
| The wizard or a typed length while a curve counts | That length is in use; the curve is put away as by «Not now», its row stays on the calibration page |
| A file of this version imported by 1.1.0 | The height's estimate, silently: 1.1.0 reads an unknown mode so and cannot say it |
| An outing with GPS and no sample | No «Your step at this pace» line |
| The curve accepted, then the height changed | The curve stays in use: it was measured, the height is only an estimate |
| «Go back» | The length and mode in use before return (measured, typed or the height's); the samples stay, and a new proposal waits for a new 2% from the curve gone back from |
| «Use it» | Today and the days after follow; past days keep theirs unless «Apply to past days too» is touched and confirmed |
| The outing that made the curve ready | Its page carries the line to the card while the proposal waits; no other outing's page does |
| «Not now», then new outings | No card in Settings until the curve moves 2% from the one put away; the calibration page's row stays |
| «Not now», then a change of mind | The calibration page's row opens the card again, with today's curve |
| An outing deleted | Its samples go with it; the curve in use is not recomputed behind the reader's back |

### Phase 18 — City walks measured with GPS («Passeggiate misurate»)

A city walk stays imaginary: the city on its map, its places told by the distance the reader
walks wherever they are. With «Measure with GPS» on, that distance is measured instead of
estimated, and the outing keeps the reader's real route, as every outing with GPS does: one walk
gives the story and the measure, where today the reader must choose one of the two (owner,
19 Nov 2026; ADR 0016 decision 13, amended). It replaces «On the spot: the cities», dropped with
Phase 19 the same day (§15, «The places on the spot dropped, the city walk measured»).

**Read first:** ADR 0016 (decisions 2, 5 and 13, with their amendments),
`docs/adr/0015-city-walks.md`; here Phase 15 (the switch, the one door, `GpsReadiness`) and
Phase 16 (the outing's page). In the code: `WalkScreen.kt`, `WaysViewModel.kt`,
`SessionControl.kt` (`startWalk`), `StepTrackingService.startWalk`, `SessionPlans.startWalk`,
`OutingStarter.kt`, `GpsStart.kt`, `GpsSwitchNote`, `PlanEditorScreen.kt` (the switch and its
note, to reuse, not to copy), `SessionTracker.kt` (`placesPassed`), `WalkPlaces.kt`, `WalkDays`,
`SessionCard.kt`, `OutingScreen.kt`.

- [ ] **The switch** (`:feature:ways`, `WalkScreen`). «Measure with GPS» beside Start on a
  walk's page, the outing editor's own switch and note (`GpsSwitchNote`: what it gives and
  costs, when Android asks, what holds after a refusal): off by default, one choice for every
  city walk, remembered (`walkGps` in DataStore, beside `walkVoice`; in the JSON backup with the
  other settings, an added field). Shown only where GPS can be used on this phone (Phase 15's
  readiness). One line says what changes: the city stays on its map, the distance is measured
  on the reader's own streets, and their route is kept as an outing's.
- [ ] **The start, through the one door** (`OutingStarter`, `:app`). A walk with the switch on
  starts through the door every outing with GPS goes through, never a second way to ask: asked
  in context the first time, a refusal remembered, approximate alone answered as for a plan,
  location off on the phone said with the way to the system's switch; in each of those the walk
  starts on the steps and its card says so once. `SessionControl.startWalk`,
  `StepTrackingService.startWalk` and `SessionPlans.startWalk` gain `gps`; the service's
  `location` type and `GpsDuty` then do for a walk what they do for any outing.
  - **The walk's page and its card.** On a walk's page, the card of the walk under way has no
    `onGps` today (`WalkScreen` draws it, with the `SessionCardActions` that `WayScreen` builds).
    That is harmless while a walk is never
    measured. Once it is, the GPS line's way out («Resume GPS», the permission, location's
    switch) must work there as it does on Today. `onOpen` comes with Phase 21.1 (the doors'
    review, §15 «The open outing's plan reviewed»).
- [ ] **The walk measured.** Nothing new to measure: in an outing with GPS the outing's
  distance is the fused one (ADR 0016 decision 5), and `placesPassed`, `WalkPlaces` and
  `WalkDays` read that distance, so the places are told by the measured kilometres and the walk
  goes on from where it stands, measured or not. Proven by tests, not assumed. The walk's page
  and its map stay the walk's (the city, the place ahead, and the reader's point on the walk
  set by the distance walked, as from home, the measured distance now; never their real
  position: owner, 19 Nov 2026, the sentence made plain); the
  outing's card shows «measured», the pace and the receiver's line as any outing with GPS does;
  the outing's page is Phase 16's (the reader's real route, on their own city's map where Passo
  carries it, on plain ground elsewhere), with one line naming the walk it moved on («From the
  Duomo to the Navigli: 3.2 km of it, measured on your streets»).
- [ ] **The words.** The Ways' sentence «No location, only your steps» stays true of every way
  and of a city walk with the switch off; where a page says it of a city walk, it says what
  holds with the switch on. «Measured» only as Phase 15 allows (nine tenths from fixes). Never
  the reader's real position on the city's map, nor a word that places them in the city: the
  city is imaginary, the measure is real. A reader who is in that very city with the switch on
  sees their point move by the distance, not by the street they take: following the streets
  was «On the spot», dropped; their real route is on the outing's page.
- [ ] **Tests.** The tracker's (a walk replayed with fixes: each place at its measured
  distance; no fix at all: the steps, as today; a walk measured and then continued from home,
  and the reverse, adding up), the start through the door (each answer), `WalkScreen`'s UI
  tests with the switch in each state, Italian on a small phone, the backup's round trip with
  `walkGps`.
- [ ] Strings in English and Italian; CHANGELOG; the guide («City walks», «With GPS»); README:
  the city walks' line if it changes.
- [ ] On a device (owner): a city walk with the switch on, walked in the reader's own streets:
  the places told as the measured kilometres pass, the outing's page with the real route
  afterwards, the walk going on from there the next day with the switch off.

**Acceptance:** a city walk with GPS on tells its places by the measured distance and leaves an
outing with the reader's real route; with the switch off nothing changes; the city's map shows
the reader's point on the walk by the distance walked, never their real position.

**Edge cases** (`SessionTrackerTest`, the door's tests, `WalkScreenTest`):

| Case | Expected behavior |
|---|---|
| The permission refused, or approximate alone | The walk starts on the steps, said once on its card, as any outing with GPS |
| Location off on the phone | The same, with the way to the system's switch |
| No fix in the whole walk | The places told by the steps; the outing's page says the satellites never answered |
| A walk measured, continued from home the next day | Goes on from the place reached; the places ahead told by the steps |
| The measured share under nine tenths | Not called measured; the places told by the fused distance as it is |
| The walk's last place reached | Its goal, as today; the receiver follows Phase 15's rules for an end that can be taken back |

### Phase 19 — Dropped («On the spot: the ways»)

Dropped by the owner on 19 Nov 2026, with the first Phase 18 (§15, «The places on the spot
dropped, the city walk measured»): a way walked from home goes on by the day's steps, which
Phase 17 makes truer, and a way walked in person is not built. The number stays, so that Phase
20 and the documents that cite the phases keep their names. The text of both dropped phases is
in `git show b842547:PLANNING.md`.

### Phase 20 — Your usual round («Il tuo giro solito»)

Most readers repeat a walk: round the park, to the station and back. Passo knows it without
being told, and asks of it its own question, the one Today asks of a day: am I ahead of my usual
or behind it? Rounds are found among the reader's own outings with GPS; nothing is imported,
followed or guided (owner, 19 Nov 2026: following a route, a GPX or «Walk it again», is not
built and not planned; §15, «Phase 20 reviewed: the usual round alone»).

It comes after Phase 21 and builds on it (owner, 20 Nov 2026; §15, «The open outing planned»).
A usual round is walked as an open outing with GPS. The round's line under way is that outing's
sentence, told at 21.2's kilometre. A round's small map is 21.3's still route.

**Read first:** ADR 0016 (decisions 6, 9, 11 and 16), Phase 16 here and its code (`RouteShape`,
`RouteMapView`, `MapViewport`, `Legs`, `Splits`, `RouteRepository`, `RouteFile`); Phase 21 here
(all three steps) and what it built: the open outing's sentences (`SessionHeadline`), the
kilometre's mark (`SessionTracker`, `SessionAnnouncement`), the still route (21.3);
`TypicalDayCalculator.kt` and `TodayOverview.kt` (how "ahead of your usual" is said for a day);
`SessionsScreen.kt` (`CitiesDoor`, the door this phase's mirrors), `OutingScreen.kt`,
`SessionCard.kt`; `BackupCodec.kt`, `DataSection` (Your data).

- [ ] **The match** (`:core:domain/routes`, `RouteMatch`, pure). Two outings with GPS are the
  same round when they start within 100 m of each other, their lengths are within a tenth, four
  fifths of each lies within 40 m of the other, **and in the same direction**: the points of
  one fall in the same order along the other (review, 19 Nov 2026: a loop walked the other way
  round starts at the same door, has the same length and lies wholly within 40 m, and the four
  fifths alone would call it the same round). Only an outing whose route is kept and measured
  (nine tenths from fixes) is matched. A loop started at another point of it is another round:
  rounds begin at the reader's door, not anywhere on the loop. Searched when an outing with GPS
  ends, the routes' boxes first, so a year of outings costs a few reads.
- [ ] **The rounds** (`passo.db`, by auto-migration). `round` (its id, the name the reader gave
  it, if any) and `round_member` (round, outing), and the pairs the reader said are not the
  same: numbers and names, never a place, so they are in `passo.db` and in Android's backup like
  the outings. A round holds no line of its own: its map and its start are read from its
  members' routes in `routes.db`, so no place outlives its outing (CLAUDE.md, «Backup is an
  allowlist»). A member whose route is deleted stays a member for its times (they are its legs,
  in `passo.db`) and no longer gives the round its map; a round whose members all lost their
  routes keeps its history and is found again by no new outing until one is walked with GPS.
  The JSON backup carries the rounds with the outings (added fields; their names and the pairs
  not the same); «Delete all routes» leaves them, and its dialog says the history stays.
- [ ] **The comparison, honest.** A round's usual is the median of its members' times in
  motion before this one, each brought to the round's usual length (time × usual length ÷ its
  length): a shortcut of 300 m does not read as a faster walk (review, 19 Nov 2026). By the
  kilometre, the usual time at each kilometre is the median of the members' times there, from
  their legs (`Splits`), so the comparison holds for a member whose route was deleted.
- [ ] **The outing's page.** From the third walk of a round, the page says it, sentence first
  («Your usual round, the sixth time: 2 minutes faster than usual»), against the usual above,
  with «Not the same round» (the pair is kept, the outing leaves the round) and the round's
  name, touched to open its page.
- [ ] **The card, while it counts.** This is for an open outing with GPS (Phase 21). It must have
  begun within 100 m of where a round with two walks or more begins, and still be on it after
  500 m (four fifths of its points so far within 40 m of the round's line, in order). Then it
  says how far ahead or behind the usual it is, in the day's own words («30 seconds ahead of
  your usual»). That is its sentence, the one Phase 21 gives the open outing, from the first
  kilometre and again at each.
  - It rides on 21.2's kilometre: the batch that tells the kilometre's mark tells the round's
    line.
  - Where the outing speaks, the kilometre's words carry it too.
    - On a round, the comparison takes the place of the kilometre's pace, so the words stay
      short: «Two kilometres, 30 seconds ahead of your usual». The pace stays on the card and on
      the page.
    - Off the round, the next kilometre's words say so once, then go back to the pace.
    - Being ahead or behind has no vibration of its own: the kilometre's one pulse is enough.
  - There is no vibration and no signal of its own, and no wake: nothing is added to §9 (review,
    19 Nov 2026: a line at a kilometre is told at the batch, like an outing's goal).
  - An open outing whose marks are every ten minutes, or none, still shows the line at each
    kilometre, silently.
  - A goal outing keeps its own sentence: its round is told on its page afterwards.
  - Off the round for 200 m, the line says so once and stops.

  *Changed 20 Nov 2026* (§15, «The open outing planned» and «The open outing's plan reviewed»):
  the line was «free» and silent. It is now the open outing's, and it is spoken where the
  kilometre already is. The owner reopened 19 Nov's «no voice» and asked for what gives the most
  value. A reader with the phone in a pocket hears the one thing a round adds, at the moment it
  is true, with no new signal and no wake (as a target pace is told at each cue in the running
  apps).

  The service reads the rounds and their members' routes once, at the outing's start, from
  `routes.db` (never making it: `RouteFile`).
- [ ] **Your rounds** («I tuoi giri», `:feature:sessions`). A door on the Outings page, as the
  cities' is, shown once there is a round. Each round: its name (or «Your round of 3.2 km»
  until it has one), its small map from its latest member's route (Phase 21.3's still route, on
  plain ground as every route since §15 «Every route on plain ground»: no gestures in a list),
  its length, how
  many times it was walked, its usual time. A round's page: the map with Phase 16's gestures,
  each walk with its day and its time brought to the usual length, a small Canvas chart of
  those times over the weeks with the usual as a line (one sentence for TalkBack: «Six walks
  since March; the last three faster than usual»), rename, and «Forget this round» (the
  outings stay, as they are; the round and its pairs go, asked first). Never a record board, no
  fastest mark, no medal, never a segment, never anyone else's walk: one reader and their own
  rounds (ADR 0016 decision 16).
- [ ] **Tests.** `RouteMatchTest` (the cases below, routes written by hand), the comparison's
  (lengths brought to the usual, the median, a member without route), the repository's (the
  migration, a member's outing deleted, «Delete all routes» leaving the rounds), the backup's
  round trip, the card's line (on the round, left, a round of two walks; said with the
  kilometre's mark, with the voice on and off; an open outing with marks every ten minutes,
  silent; a goal outing on a round, no line), the outing page's and
  Your rounds' UI tests, in Italian on a small phone and at twice the text size; the chart in
  both themes.
- [ ] Strings in English and Italian, with plurals; CHANGELOG; the guide (a part in
  «Outings»); README: the feature line, and a screenshot of a round's page if it earns its
  place.
- [ ] On a device (owner): a usual walk taken three times, then the fourth as an open outing
  with GPS, with the card's line and its words at each kilometre; the same walk the other way
  round, not called the same; a shortcut,
  compared honestly; Your rounds in both themes and at a large text size.

**Acceptance:** the third time a round is walked Passo knows it without being told, and is right
about which walks are the same one, direction included; «faster» and «slower» are true of the
pace, not of a shortcut; a reader who never walks with GPS sees nothing new.

**Edge cases** (`RouteMatchTest`, the comparison's tests, the repository's):

| Case | Expected behavior |
|---|---|
| The same streets walked the other way | Not the same round: the order of the points differs |
| A loop started from another point of it | Another round: they begin at the door |
| A round with a detour of 300 m one day | Still the round (four fifths); its time brought to the usual length |
| A shortcut that leaves out a tenth | Within the lengths' tenth, the round; brought to the usual length, not «faster» for it |
| Two rounds from the same door | Told apart by their lines, not by their start |
| A round walked with a long pause | Compared by time in motion, as every outing |
| An outing measured under nine tenths | Not matched: its line is too much the steps' |
| A member's route deleted | Still a member, for its times; the map from another member |
| Every member's route deleted | The history stays; no new walk is matched until one with GPS is kept |
| A member's outing deleted | It leaves the round; under two members, the round is forgotten |
| «Not the same round» | The pair is kept; the two are never matched again |
| The reader leaves the round at the third kilometre | The card says so once and the line stops; the outing goes on as any |
| A goal outing with GPS walked on a round | No line on its card (its sentence is its goal's); its page says the round afterwards |
| An open outing on a round, marks every ten minutes | The line at each kilometre, silent; the ten minutes' words unchanged |

### Phase 21 — The open outing («L'uscita aperta»)

An open outing is an outing with no goal, with GPS or without. The reader arrives at the park,
starts it, and walks as it comes. Halfway they look at the phone: the route so far and its
numbers. Back at the car they stop it. Or they forget it, and it ends by itself at their last
step.

The owner asked for it on 20 Nov 2026, after the outings with GPS: "I miss an outing with no
goal". It was reviewed against the code and against how the known apps do it (§15, «The open
outing planned»). It comes before Phases 18 and 20, and Phase 20 builds on it.

**Read first:**
- ADR 0009, all of it: this phase amends decisions 1, 4, 6 and 8, and 3 with step 21.2.
- ADR 0016, decisions 7 and 14.
- ADR 0013 (the signals' wake).
- «The bar» above, and §9, rules 7 and 9.
- Every row of this phase's edge-case tables is a test that says so in its name.

In the code:
- `:core:model`: `Session.kt` (`SessionGoalKind`, `SessionPlan`, `Session`).
- `:core:domain`: `SessionPlans.kt`, `SessionProgress.kt` (`progress`, `SessionHeadline`),
  `SessionTracker.kt` (the signals, `finish`, the cap, the take-backs), `SessionConstants.kt`,
  `SignalWake.kt`, `SessionAnnouncement.kt`, `BackupCodec.kt`, `BackupFile.kt`,
  `BackupMerge.kt`, `CsvExport.kt`, `YearInReview.kt`.
- `:core:data`: `Entities.kt`, `SessionMapping.kt`, `SessionRepository.kt` (the presets),
  `PassoDatabase.kt`.
- `:core:tracking`: `StepTrackingService.kt` (`publishSession`, `onSessionSignals`),
  `SessionNotifications.kt`, `SessionShortcuts.kt`, `SignalWakeLock.kt`, `StepsTileService.kt`.
- `:core:designsystem`: `SessionText.kt`, `SessionCard.kt`, `WalkList.kt` (`OutingList`),
  `PassoIcons.kt` (`Outing`).
- `:feature:sessions`: `PlanEditorScreen.kt`, `PlanEditorViewModel.kt`, `SessionsScreen.kt`,
  `OutingScreen.kt`, `OutingViewModel.kt`.
- `TodayScreen.kt` and `TodayViewModel.kt`; `WalkScreen.kt` (`:feature:ways`).
- `:widget`: `WidgetText.kt`.

**What the owner decided** (20 Nov 2026):
- **The name.** The editor's «Kind» gains a third choice, «None» / «Nessuno»: «One goal | None |
  Intervals». The documents call it the open outing, «uscita aperta». «Libero» / "Any pace" is
  taken by the pace, and «Passeggiate» by the city walks: neither names it.
- **The signals.** A signal at each kilometre (or mile) with GPS, and every ten minutes in
  motion without. The reader may also choose none.
- **The card under way.** In place of the bar: the route with GPS, the shape of the walk
  minute by minute without, and the walk's share of the day's goal.
- **«See the outing» while it counts**, on every outing's card, not only the open one.
- **The forgotten start** (crediting the walking already done before the start) is Phase 22,
  later. Phase 21 does not build it.
- **Left out:**
  - A turn-around signal: a time goal with its signal at half already gives one.
  - Cropping a route afterwards: ADR 0016 decision 9 already leaves it out, and the end at the
    last step makes it needless.
  - Auto-pause: the time in motion, measured from the steps, already does that job.
  - A goal added on the way: one quantity, frozen at the start, or none.
  - A pace per kilometre without GPS: ADR 0009 decision 1.
- **The number.** Phase 21 is a new number, so the numbers of the phases cited elsewhere stay.

**How it ends.** Recommended when the owner asked "what do you really recommend, what is best for
a premium app", and decided by the owner on 20 Nov 2026, the twelve hours and the short ride
both (§15, «The open outing's plan reviewed»).
- **No end by goal.** It ends in four ways, as every outing does:
  - the reader stops it;
  - after 15 minutes still, at its last step: told when noticed, with «Resume» for 15 minutes;
  - after an hour paused, where it was paused;
  - at its cap.
- **The cap is twelve hours, not four** (ADR 0016 decision 14 says this is where a long
  outing's cap is decided). The question still comes 15 minutes before, and «Keep going» still
  moves the cap four hours on. Why twelve:
  - An open outing has no end of its own. The cap is the only end left besides the reader and
    the stillness.
  - A day's hike is six to ten hours with its stops. A cap inside it would end the walk, and
    with the walk the receiver: the rest of the route would be lost for good.
  - The forgotten outing is the stillness's to end, not the cap's: a reader in a car or on a
    sofa stops stepping.
- **Why not no cap at all.** Twelve hours still bounds two things:
  - the processor kept awake on a phone with no wake-up counter (`SignalWake`, ten minutes past
    the cap);
  - an outing kept open by a day of errands, with the phone forgotten in a pocket.
- **A ride is not an end; the logic stays as it is.** The case is already handled:
  - A ride adds no distance (`RouteMeter` sees it as carried) and almost no time in motion
    (the time comes from the steps).
  - The receiver sleeps two minutes after the last step.
  - The outing ends at that last step, at the car, once the stillness has lasted 15 minutes.

  It leaves one case: a ride shorter than 15 minutes, followed by steps (park, car, ten
  minutes, the steps home). Then the outing goes on through it. The steps home are the
  outing's until the stillness, and with GPS the route gains a stretch at the door. An
  automatic end at the ride was weighed and left out, for three reasons:
  - Only the first fix after the ride would see it. That fix comes after the steps that woke
    the receiver, which are already counted.
  - Indoors, that fix may never come.
  - It would end the walk at every tram, which §11 Phase 15's table keeps inside the walk on
    purpose («A tram between two walks»).

  Step 21.1's device check walks exactly this case. If what the page shows matters, the end at
  a ride is designed then, as a phase of its own.

**Three steps, a session each, in this order.** Each ends green, with its boxes ticked, its
entry at the end of §15's log, and its device box opened in «Open». A reader who never chooses
«None» sees nothing new but three things: «See the outing» on a card under way, the outing's
page opened by what shows the outing (its notification, the widgets, the tile), and the time
paused on an outing's page.

| Step | Brings | Needs |
|---|---|---|
| 21.1 | The open outing itself: the model, its ends, the start, the editor, every place an outing is shown; «See the outing» while it counts; the pauses kept | nothing before it |
| 21.2 | The marks on the way: a kilometre, or ten minutes | 21.1 |
| 21.3 | The shape on the card: the route, the minutes, the day's share; the still route that Phase 20 reuses | 21.1 |

#### Step 21.1 — The open outing

- [x] **The documents first.** Write the amendments before the code, each with its date and
  this phase's reason:
  - ADR 0009, decision 1: one quantity, or none (the open outing).
  - ADR 0009, decision 4: an open outing's notification has no bar.
  - ADR 0009, decision 6: its ends, and its twelve hours.
  - ADR 0009, decision 8: «See the outing» while it counts, and the open preset.
  - ADR 0016, decision 14: an open outing starts with twelve hours.
- [x] **The model** (`:core:model`, `:core:domain`).
  - `SessionGoalKind.OPEN`, with `goalValue` 0 and never read. It is stored by its name, as
    every kind is (`session_plan.goalKind`, `session.goalKind`), so no column changes for it.
  - `Session.open` and `SessionPlan.open`.
  - The compiler lists every `when` on the kind. Each gets its own open branch, and none an
    `else`.
  - In `SessionPlans`:
    - `range(OPEN)` is `0..0`.
    - `convert` gives 0 into OPEN. Out of OPEN it gives the goal last kept for the new kind, or
      `NEW`'s 30 minutes converted.
    - `estimate`: none. The editor shows no estimate line for an open plan.
    - `start` makes an open session with no milestones, and
      `capAtMillis = now + OPEN_MAX_SESSION_MILLIS`.
    - `REST_OF_DAY` is found by its kind, not by its place in `PRESETS`, which moves.
  - `SessionConstants.OPEN_MAX_SESSION_MILLIS` (12 h), with its reason from «How it ends».
  - `SessionProgress`:
    - `progress()` is 0 for an open outing.
    - `done`, `remaining` and `goal` are never asked of one: a test pins that no text is made
      from an open amount.
- [x] **The tracker** (`SessionTracker`). An open outing has no milestone and no goal, so it
  emits no `Milestone` and is never `Finished` by a goal. It ends by stop, by stillness
  (`IDLE`), by a pause past an hour, or at its cap (`CLOSED`), with the same take-backs as every
  outing. The cap's question comes at 11 h 45. It is kept as every outing is (30 steps or more).
  The engine is otherwise untouched: §4.6's cases stand.
- [x] **The wake.** In this step an open outing tells nothing on the way, so it has no signal of
  its own. It holds the lock only for GPS, on a phone with no wake-up counter (§9 rule 7,
  amended). `SignalWake.needed` and `neededFor` learn this. The lock's bound follows the
  outing's own cap (`timeoutMillis`). Check that nothing still bounds it by
  `SignalWake.TIMEOUT_MILLIS`, the fixed four hours (`SignalWakeLock`'s first bound).
- [x] **The pauses kept** (`passo.db` v9, by auto-migration). This is for every outing, not
  only the open one, so that 21.3's minutes are true from the first open outing walked.
  - `session_pause` (`sessionId`, `fromMillis`, `toMillis`): written by the service in the same
    transaction as the session, at each pause and each resume. `toMillis` is null while paused.
    An end by a pause past an hour closes it at the end.
  - Read by the outing's page: «when it was» says the time paused («10:12–11:05 · 53 minutes, 8
    of them paused»). Also read by 21.3's minutes.
  - In the JSON backup with its session: an added field, `pauses`, merged with the session
    (`BackupMerge`). A file from before has none, and its outings have no pauses known.
  - A route's segments at the pauses are untouched.
  - **It does not grow on its own** (owner, 20 Nov 2026: "check it is not a table that grows
    for ever").
    - A row is a Pause the reader touched, and nothing else: no pause is ever made by itself,
      and pausing the count ends the outing instead.
    - It grows with the outings kept, as `session_interval` and `session_leg` do, and far
      slower: a leg is a row every 100 m. Two pauses an outing, two outings a day, is about
      1,500 rows a year, some tens of kilobytes. `minute_steps` alone writes hundreds a day.
    - It is deleted with its outing, in the same transaction (`SessionDao.deleteSession`,
      beside the intervals, the legs and the samples). That includes an outing too short to be
      kept.
    - A test proves it: an outing not kept leaves no pause behind, and an import that skips an
      outing as a duplicate (`BackupMerge`) skips its pauses with it.
- [x] **The start.**
  - **The open preset**, first in `PRESETS`: `SessionPlan(goalKind = OPEN, intensity = FREE,
    position = 0)`. The others move down one.
  - **A reader with plans already** finds it once, at the top of their list, after the update
    (`SessionRepository`). A DataStore flag, `openPresetOffered`, keeps a deleted one from coming
    back. It is not added to a reader who already keeps an open plan.
  - **Every door that starts a plan starts an open one** as any other: the Outings page, the
    launcher's shortcuts, and the one door for GPS (`OutingStarter`). "Walk now" and the city
    walks are unchanged.
- [x] **The editor** (`PlanEditorScreen`, `PlanEditorViewModel`).
  - «Kind» has three choices: «One goal | None | Intervals» («Un obiettivo | Nessuno |
    Intervalli»). Check its width in Italian on a narrow phone and at twice the text size.
  - With «None», there is no goal row, no value, no estimate line, and no milestones. Under
    the row, one line says what it is: «No goal: it counts until you stop it, and ends by itself
    at your last step if you forget it.» / «Nessun obiettivo: conta finché la fermi, e finisce
    da sola al tuo ultimo passo se te ne dimentichi.»
  - The pace, «Measure with GPS», the vibration and the voice stay. They still serve the
    stillness, the cap's question and the GPS lines.
  - Switching back to «One goal» finds the goal last set there, as «Intervals» keeps its sets.
  - **The name** is derived from the pace, as now («Walk», «Brisk walk», «Run»). The plan's line
    on the Outings page: «No goal, at any pace» / «Senza obiettivo, a ritmo libero».
  - There is no signals line for an open plan until 21.2 builds the marks.
- [x] **The card** (`SessionCard`, `SessionText`). An open outing has its own sentences:

  | State | English | Italian |
  |---|---|---|
  | Starting | «Off you go: everything counts from here» | «Si parte: da qui conta tutto» |
  | Under way | «25 minutes on your feet» | «25 minuti in cammino» |
  | Under way, a run | «25 minutes of running» | «25 minuti di corsa» |
  | Under way, measured | «2.3 km measured in 25 minutes» | «2,3 km misurati in 25 minuti» |
  | Paused | as any outing | as any outing |
  | Over | «A walk of 3.2 km in 41 minutes» | «Una camminata di 3,2 km in 41 minuti» |

  - Over by stillness or at the cap, it says that end, with its numbers, as any outing does.
  - There is no bar, no milestone, and no goal at its end, and no `progressBarRangeInfo`.
  - Under the sentence, one row: the time in motion and since when («41 min on your feet · since
    10:12»). 21.3 puts the shape above it.
  - The numbers, the pace line, the GPS line and the cap's question are as for any outing.
- [x] **Its mark.** A new `PassoIcons` mark: a way with no flag at its end, drawn in code, in
  both dresses.
  - Shown on the card, on the Outings page, and on its shortcut: `ic_shortcut_open.xml`, on the
    shortcuts' ground from `tools/draw_launcher_icon.py`, mapped in
    `SessionShortcuts.shortcutIcon` from `sessionIcon`.
  - A run keeps its bolt.
- [x] **«See the outing» while it counts**, for every outing. One touch from the card, under
  way or paused, opens the outing's page, which already follows the outing (archive 04, «An
  outing's page follows the outing while it counts»).
  - **Where:** on Today, on the Outings page, and on a city walk's page.
  - **The card's controls:** Stop, Pause or Resume, and «See the outing», each at 48 dp. They go
    on two lines where one is too narrow, as the finished card's do.
  - **On the page, while it counts:** Stop and Pause or Resume at its foot, the GPS line with
    its way out, and the cap's question, as on the card. A reader who opened it never has to
    go back to stop.
- [x] **What shows the outing opens it**, for every outing (the doors' review, §15 «The open
  outing's plan reviewed»). Today these land on whatever page the app was last left on: a
  launch intent brings the task back as it was. While an outing counts, the following open the
  outing's page (`SessionControl.EXTRA_OPEN_OUTING`, the door the end's notifications already
  use):
  - the body of the counting notification, which is then the outing's;
  - «Still walking?» (`SessionNotifications.capAhead`);
  - «GPS stopped» (`gpsStopped`);
  - both widgets and the tile, while they show the outing's line.

  The page holds what these speak of: the controls, the cap's question, the GPS line with
  «Resume GPS». An outing already over when the touch comes opens on its finished page. With no
  outing, these surfaces open Today (Phase 23.1).
- [x] **The notification, the widgets, the tile.**
  - **The notification:** the name, the sentence, the numbers, Pause or Resume, and Stop, with
    no bar. It uses `BigTextStyle` on every version and still asks to be promoted.
  - **The chip on Android 16:** its short text is the distance («2.3 km»), or the time in
    motion before 100 m. Use `setShortCriticalText` from the compat library if it has it,
    otherwise the platform builder's.
  - **When it updates:** when it updates now. No timer of Passo's; its chronometer is not the
    time in motion, so it is not used.
  - **Both widgets and the tile:** «Walk: 2.3 km, 25 min» / «Camminata: 2,3 km, 25 min», in the
    place of «12 of 20 min». Count Glance's children.
- [x] **The voice** (`SessionAnnouncement`).
  - The start: «Walk started, no goal» / «Camminata avviata, senza obiettivo».
  - The end: what it came to («3.2 km in 41 minutes»).
  - The stillness, the cap's question and GPS: as any outing.
- [x] **Where it is listed and kept.**
  - **The lists of Today and History** (`OutingList`): «3.2 km · 41 min», with no share of a
    goal.
  - **The outing's page:** the sentence, «when it was» with the time paused, and no goal line.
    The rest is as any outing's (map, numbers, kilometres, climb, the step learnt).
  - **Your year on foot:** an open outing counts as an outing, and never as a goal missed.
    Check `YearInReview` for anything said of goals.
  - **The CSV:** `goal_kind` is «open», the goal's columns are empty, and the outcome is its
    end.
  - **The JSON backup:**
    - The kind is stored by its name. The session reader refuses REST_OF_DAY alone, so it
      accepts OPEN.
    - An older Passo leaves the open outings and plans out (`toEnumOrNull`). Say so in the
      CHANGELOG.
  - **The step that learns:** an open outing with GPS teaches the step as any outing with GPS
    (Phase 17).
- [x] **Tests.**
  - `SessionPlansTest`: the preset, `start`, `convert`, the cap.
  - `SessionTrackerTest`: the edge cases below; the engine's §4.6 cases unchanged.
  - `SignalWakeTest`.
  - The repository's tests: the pauses written with the session, the preset offered once,
    the migration to v9 (CI's to prove on Windows).
  - `BackupCodecTest` and the merge's tests: an open outing with its pauses, a file from
    before.
  - `CsvExportTest`.
  - `SessionCard`'s UI tests: every state of an open outing, and «See the outing» under way
    and paused on a goal outing too.
  - `OutingScreenTest`: the controls while it counts, the time paused.
  - The editor's tests: «None» in Italian on a small phone, at twice the text size, and on an
    open foldable.
  - The notification's builder.
  - `WidgetGalleryTest`: the open line in each form.
  - The screenshots looked at, in both themes and both dresses.
- [x] **The record.**
  - Strings in English and Italian, with plurals.
  - The CHANGELOG.
  - The guide: «Outings» gains the open outing, its ends and its twelve hours; «With GPS»
    gains the page while it counts.
  - VISION: outings come with a goal or with none.
  - The README: the outings' line. Its screenshot waits for 21.3's card.
- [ ] **On a device (owner).**
  - **An open outing with GPS at a park:**
    - started from the Outings page and from the shortcut;
    - the card under way (its sentence, its row, no bar);
    - «See the outing» from the card, the route growing on the page, then Pause and Stop
      from the page;
    - the notification, and the chip with the distance on Android 16;
    - both widgets and the tile;
    - stopped at the car.
  - **Then one forgotten:**
    - with a ride of more than 15 minutes: ended at the car, told at the first steps home,
      with «Resume»;
    - with a ride of less than 15 minutes, then the steps home: what the page, the route and
      the numbers show. This is the case that decides whether the end at a ride is designed
      («How it ends»).
  - **The doors to it:** the counting notification touched mid-walk, from the lock screen
    too; «Still walking?» and «GPS stopped» touched; a widget and the tile touched while they
    show the outing. Each opens the outing's page.
  - **One without GPS**, with two pauses: the time paused on its page.
  - **The editor's «None»** in Italian.
  - **Everything** in both themes, at a large text size, and with TalkBack.

**Acceptance (21.1):**
- An open outing starts from «None» or from the preset, and counts with no goal.
- It never ends at a goal, nor at four hours. It ends as every outing does otherwise.
- It is shown everywhere an outing is, without a goal's word or bar.
- Its page opens from the card while it counts, and can stop it.
- An outing paused keeps its pauses.

**Edge cases (21.1):**

| Case | Expected behavior |
|---|---|
| An open outing walked for five hours | Not ended at four; asked at 11 h 45 if the reader is still walking |
| The question at 11 h 45 unanswered | Ended at twelve hours and told; «Keep going» for 15 minutes takes it back with the steps since |
| «Keep going» at 11 h 50 | The cap is four hours later; asked again 15 minutes before it |
| Forgotten, then a ride of 20 minutes | Ended at its last step, at the car; told at the first step home; «Resume» for 15 minutes |
| Forgotten, a ride of 10 minutes, then steps home | Goes on: the ride adds no distance and almost no time in motion; the steps home are its own until the stillness ends it (the device check's case) |
| Paused for more than an hour | Ended where it was paused, its last pause closed there |
| Fewer than 30 steps | Not kept |
| A plan with GPS, the permission refused | Starts on the steps, as any plan with GPS; the card says so once |
| Across midnight | Belongs to the day it started on |
| Paused twice | Both pauses kept; the page says the time paused |
| A plan switched to «None» and back | The goal last set is there again |
| The open preset deleted | Never offered again |
| A reader who already keeps an open plan | No preset added |
| A phone with no wake-up counter, open, no GPS | No wake lock: it has no signal to keep on time |
| The same, with GPS | The lock, bounded ten minutes past its own cap (twelve hours, or moved on) |
| The process restarted during an open outing | Picked up as any; its cap and its open pause read from what was stored |
| A run with no goal | Its bolt; «25 minutes of running» |
| A backup with open outings | Read whole, pauses included; read by an older Passo, they are left out |
| A goal outing under way | «See the outing» on its card; nothing else changes |
| The counting notification touched during an outing | The outing's page, with its controls |
| «Still walking?» or «GPS stopped» touched | The outing's page, with the cap's question or the GPS line on it |
| A widget touched while it shows the outing | The outing's page |
| A notification touched after the outing ended | The outing's page, finished (or reopenable, with its «Keep going») |

#### Step 21.2 — The marks on the way

A mark is the open outing's one signal on the way: a kilometre or a mile, or ten minutes in
motion. It is told by a pulse, and by the voice where the outing speaks.

- [x] **The documents.** ADR 0009 decision 3 is amended: an open outing's signals are its marks.
- [x] **The model** (`passo.db` v10, by auto-migration).
  - **What the reader chooses:** none, the distance, or the time.
  - **The plan** (`session_plan`) keeps the choice, or nothing. Nothing means it follows the
    plan's GPS switch: the distance with GPS on, the time with it off. The editor shows the one
    in force as chosen. A choice made by the reader is kept whatever the switch.
  - **The outing** (`session`) freezes, at its start, the mark in force and its unit (a
    kilometre or a mile, from the reader's units then), as it freezes the goal. It also keeps
    the count of marks told, so none is told twice.
  - **The plan's switch decides, not the phone.** A plan with GPS that starts on the steps still
    tells kilometres, estimated.
  - The JSON backup carries both, as added fields.
- [x] **The tracker.**
  - **Each mark crossed** is a signal (`SessionSignal`, a new case with its count). It is told
    as a distance goal's milestones are: from the settled distance with GPS.
  - **Several marks crossed in one batch** are told once, as the last.
  - **None is told** after the end, or while the outing can be taken back. After «Resume» or
    «Keep going», counting goes on from the next.
  - **Ten minutes** means ten minutes in motion: a pause, or a traffic light, adds nothing.
- [x] **The pulse.** One short pulse, 180 ms: the pattern of a quarter in a goal outing. An
  open outing has no quarters, so it cannot be mistaken for one. It is played as the milestones
  are: as a notification's vibration, only while the outings' channel is on, and silent mode
  applies.
- [x] **The words** (`SessionAnnouncement`).
  - **The distance:** «One kilometre, in 11 minutes» / «Un chilometro, in 11 minuti», with the
    plurals.
  - **Where measured,** the pace of that kilometre is added, from its legs: «pace 11:20» /
    «ritmo 11:20». This applies only where the outing's distance is measured, at nine tenths or
    more (ADR 0016 decision 7).
  - **Where not measured,** it says «about», and gives no pace.
  - **The time:** «Ten minutes, 1,150 steps» / «Dieci minuti, 1.150 passi».
- [x] **The wake.** `SignalWake`: an open outing whose marks are none has no signal. One with
  marks, and with the vibration or the voice on, has signals, as a goal outing has. The 30 s
  latency is as every outing's. There is no 2 s window: that is the intervals'.
- [x] **The editor.**
  - **A group «Signals» for an open plan:** «At each kilometre | Every 10 minutes | None» /
    «A ogni km | Ogni 10 minuti | Nessuno», in miles where the reader walks in miles.
  - **«Try it»** plays the pulse.
  - **Its note** says that the kilometre is measured with GPS and estimated without.
  - **The plan's line on the Outings page:** «A signal at each kilometre» / «Un segnale a ogni
    km», and so on.
- [x] **The notification** is posted again at each mark, as at each milestone.
- [x] **Tests.**
  - `SessionTrackerTest`: the edge cases below.
  - `SignalWakeTest`.
  - `SessionAnnouncementTest`: measured, estimated, the time, Italian plurals.
  - The editor's tests: the group in each state, following the switch, a reader's choice kept.
  - `BackupCodecTest`: the migration to v10, CI's to prove.
- [x] **The record.**
  - Strings in English and Italian, with plurals.
  - The CHANGELOG.
  - The guide's «Outings»: the marks.
- [ ] **On a device (owner).**
  - An open outing with GPS: the pulse at each kilometre in a pocket, with the screen off, and
    the voice in the headphones with the pace.
  - One without GPS: the ten minutes.
  - The editor's «Try it».
  - The battery note of §9 unchanged: no new wake, beyond the signals' own.

**Acceptance (21.2):**
- An open outing tells each kilometre with GPS, and each ten minutes in motion without, unless
  the reader chose otherwise.
- Each mark is told once, on time with the screen off, and never after the end.
- A pace is said only of a measured kilometre.

**Edge cases (21.2):**

| Case | Expected behavior |
|---|---|
| A batch crossing two kilometres | Told once, as the second |
| A kilometre whose fixes are not in yet | Told when its metres are settled, as a distance goal's milestone |
| A plan with GPS started on the steps | Kilometres told, estimated: «about», no pace |
| The distance measured under nine tenths | No pace said |
| Ten minutes with a pause in them | Minutes in motion: the pause adds nothing |
| The units changed during the walk | The unit frozen at the start |
| Marks set to none | No pulse, no words for them, no wake lock for them |
| The outings' channel silenced | No pulse, as for the milestones; the voice as the reader set it |
| Ended, while it can still be taken back | No mark |
| Taken back with «Resume» | Marks go on from the next; none told twice |
| The process restarted | The count of marks told read from what was stored |
| The plan's GPS switch changed, no choice made | The mark follows the switch |
| The plan's GPS switch changed after a choice | The reader's choice stays |

#### Step 21.3 — The shape on the card

- [x] **The minutes** (`OutingMinutes`, `:core:domain`, pure). From `minute_steps`, over the
  outing's span, less its pauses (`session_pause`, kept since 21.1).
  - **One value a minute:** the steps in it.
  - **Grouped** so that a bar stays at least 2 dp wide at the width given: 1, 2, 5, 10 or 15
    minutes a bar.
  - **A bar's height** is its mean steps a minute.
  - **«At the pace»** when at least half its minutes are at or above the outing's cadence
    floor. For an outing at any pace that floor is brisk, 100, the day's own brisk minute.
  - **The pauses are gaps.**
  - **One sentence for TalkBack:** «41 minutes on your feet, 30 of them brisk, one pause».
- [x] **The strip** (`MinuteStrip`, `:core:designsystem`, Canvas).
  - **Its bars**, from the start to now: the accent for those at the pace, the muted ink for
    the others.
  - **It grows to the right.** Under reduced motion it jumps, with no growth.
  - **Drawn only while on screen**, as every chart here.
- [x] **The still route** (`:core:designsystem`). A route drawn as `RouteMapView` draws it
  (plain ground, its dashes), without gestures.
  - On a card, a touch opens the outing's page.
  - **One component**, which Phase 20's list of rounds reuses.
  - **Thinned to the pixels it is drawn in.**
  - **A twelve-hour route,** about 43,000 points at one fix a second, is read, shaped and
    drawn without a dropped frame. Measure it in a test that logs the time, and on the phone.
- [x] **The card of an open outing**, in the bar's place:
  - **With GPS:** the route so far, still. The strip shows until the route has a line (the
    first fixes); then the route takes its place, with a fade.
  - **Without GPS:** the strip.
  - **Under it:** 21.1's row, then one line, the walk's share of its day's goal.
    - «34% of today's goal» / «Il 34% del tuo obiettivo di oggi»: the outing's steps over the
      goal stored for its day.
    - «All of today's goal, by itself» / «Da sola, tutto l'obiettivo di oggi» at 100% or more.
- [x] **How the card reads them.** The card's ViewModels (Today, the Outings page, a city
  walk's page) read only while the card is on screen (`WhileSubscribed`).
  - **The route** is read again only when its point count changes, as `OutingViewModel` does,
    off the main thread.
  - **The minutes** come from the flow the day's chart already reads (Today), or from the
    outing's span elsewhere.
  - **Nothing runs with the screen off** (§9).
- [x] **Past its goal** (owner, 20 Nov 2026, explained in §15 «The open outing's plan
  reviewed»). This is for a goal outing taken on with «Keep going» past its goal: for instance
  30 minutes of brisk walking, and then 20 more.
  - Today its card shows a bar full since the 30th minute, «50 min» at one end and «30 min» at
    the other: nothing that moves as the reader walks on.
  - From the goal on, the shape takes the bar's place: the route with GPS, the minutes without.
    Past its goal an outing is open in all but its name.
  - The sentence stays «Goal reached», with the time beyond it: «Goal reached, and 20 minutes
    more» / «Obiettivo raggiunto, e 20 minuti in più».
  - The goal's mark on the bar is not lost: the strip marks the minute the goal was reached,
    with a thin line in the goal's colour.
- [x] **The outing's page.** A section, «The shape of the walk» / «La forma della camminata»,
  holds the strip and the day's share, for every outing with its pauses kept, that is, every
  outing walked since 21.1.
  - With GPS, it comes after the map.
  - On an outing from before, its pauses unknown, it is not shown.
- [x] **Tests.**
  - `OutingMinutesTest`: the grouping, the pauses, across midnight, a twelve-hour walk.
  - `MinuteStrip` and the still route in the galleries, in both themes.
  - The card's UI tests: no fix yet, then the route; without GPS; past a goal.
  - `OutingScreenTest`: the section shown, and not shown for an old outing.
  - `assertAccessible()` and `walkPage()` on each.
- [x] **The record.**
  - Strings in English and Italian.
  - The CHANGELOG.
  - The guide's «Outings»: the shape.
  - The README: a screenshot of the open card with its route, from sample data
    (`-PupdateScreenshots`), with its caption, if it earns its place among the few.
- [ ] **On a device (owner).**
  - The card at the park, halfway: the route growing; then a walk without GPS and its strip,
    with a pause.
  - The day's share against Today's ring.
  - A long walk's card and page.
  - In both themes, at a large text size, on the open foldable, and with TalkBack on the strip
    and the route.

**Acceptance (21.3):**
- Halfway through an open outing, the card shows the walk's shape: the route with GPS, the
  minutes without, and its share of the day.
- It is drawn only while seen, and a twelve-hour walk draws as smoothly as a short one.

**Edge cases (21.3):**

| Case | Expected behavior |
|---|---|
| With GPS, no fix yet | The strip; the route in its place once it has a line |
| GPS stopped during the walk | The route as it stands; the GPS line says why |
| A pause | A gap in the strip |
| A twelve-hour walk | Bars grouped; the route thinned; no dropped frame |
| Across midnight | The minutes of both days; the share against the start day's goal |
| The day's goal changed during the walk | The share against the day's goal as Today shows it |
| The goal already met before the walk | The share as it is: «All of today's goal, by itself» only from the walk's own steps |
| A goal outing past its goal | The shape in place of the bar; the sentence its goal's |
| An outing walked before 21.1 | No shape section on its page |
| Reduced motion | No growth; the route's arrival a fade |

### Phase 22 — The walk already begun («La camminata già iniziata»), later

Asked by the owner on 20 Nov 2026 as a phase of its own, not to be built with Phase 21: "later,
in a phase apart". It is not planned in detail. The session that takes it up starts in plan mode
on this sketch, with the owner.

The known apps credit the walking done before the start (Apple's reminder, Samsung's walk
recorded from the tenth minute). Passo would do it without detecting anything live:
- **At a start**, the minutes just before are read. If they say the reader was already walking
  (a walk as `WalkDetector` finds one, still under way), the start offers «Count from 10:04,
  when you began» beside «From now».
- **The minutes before** come from `minute_steps`. Their time in motion is the minutes', not
  the steps' gaps. Their distance and energy are priced as the day prices them.
- **With GPS,** the route begins at the start. The minutes before have no fixes, and the page
  says so.
- **Nothing runs to find it:** it is read at the start, from what is stored. §9 is untouched.

To settle when it is taken up:
- every outing, or the open one only;
- with a goal, whether the minutes before count towards it;
- how many minutes make a walk already begun;
- the start's form: a second button, or a line on the card after the start («Walking since
  10:04? Count those minutes»);
- a walk with a pause in it.

### Phase 23 — The doors («Le porte»)

The owner asked on 20 Nov 2026, after «See the outing» was found missing on a card under way:
"if you see other missing buttons or ways in, tell me and put them in the plan; value, not
noise". The whole app was read for them: every page's touches, every notification, the widgets,
the tile, the shortcuts. Each finding was checked in the code, then sorted. The ones that serve
an outing went into Phase 21.1 (what shows the outing opens it), and the walk's card into Phase
18. The rest are here, in two steps. The ones left out are listed with why (§15, «The open
outing's plan reviewed»).

**Order:** after Phase 21, before 18. 23.1 is small and needs only 21.1. 23.2 needs 21.3, for
the still route.

**Read first:**
- `docs/adr/0008-goals-and-system-surfaces.md` (decision 8: «A tap opens Today»).
- `docs/adr/0005-widgets.md`.
- `docs/adr/0015-city-walks.md` (decision 12).
- `app/.../shell/PassoRoot.kt` (the keys, `HistoryTarget`, `showToday`).
- `MainActivity.kt` (`openToday`, `openOutingIfAsked`, `openWaysIfAsked`).
- `NotificationText.kt` (`openAppIntent`), `TrackingNotifications.kt`, `GoalNotifications.kt`,
  `WayNotifier.kt`, `StepsTileService.kt`.
- `widget/.../WidgetCard.kt` (`widgetTapIntent`).
- `SessionsScreen.kt` (`CitiesDoor`), `WaysScreen.kt`, `OutingScreen.kt`, `WalkList.kt`
  (`OutingList`), `DataSection.kt`.

#### Step 23.1 — The doors that lead where they say

- [x] **The tile, the widgets and the counting notification open Today.** ADR 0008 decision 8
  says so, and `widgetTapIntent`'s comment says «lands on Today». They actually open the page
  the app was last left on (a Way, Settings): the launcher's intent brings the `singleTask`
  task back as it was, and only a shortcut's start brings Today forward (`openToday`).
  - **The fix:** an extra that asks for Today, read in `onCreate` and `onNewIntent` as the
    others are, and `nav.showToday()`.
  - **Paused:** the resume extra keeps its meaning, and Today is where its button is.
  - **During an outing:** Phase 21.1's rule applies instead (the outing's page).
- [x] **The goal's notifications open what they speak of** (`GoalNotifications`).
  - The day's goal reached, and the evening reminder: Today.
  - The weekly summary («Last week: 12% more…»): History on that week (`HistoryTarget`, the
    week scale). Its rows say the same numbers.
- [x] **A stage reached opens its way** (`WayNotifier`): that way's page (`WayKey`), where the
  new stamp is, instead of the Ways list. This revisits §8's «touching it opens the Ways page».
  The notification names one way and one place, and the list made the reader look for it.
- [x] **An outing on a city walk leads to the walk.**
  - Its page (`OutingScreen`) has one row naming the walk, which opens the walk's page: its
    places, its stamps, «Continue from…». An example: «On the walk From the Duomo to the
    Navigli».
  - The card's small map of the walk opens the walk's page too.
  - The card's «See the outing» (21.1) still opens the outing.

  Today a half-walked city is four touches deep: Ways, Cities, a continent, the walk.
- [x] **The cities door lands on the cities.** The Outings page's «Walk through a city» opens
  the Ways page at its «Cities» header, which today sits below five continents of ways. It
  uses a target, as History's (`HistoryTarget`); the Ways page's own order is unchanged.
- [x] **An outing's row looks like a door.** In Today's and History's lists, an outing's row
  opens its page but wears no chevron, while every other door on those pages wears one. It
  gains the chevron. A walk found in the steps opens nothing (decided), so it wears none.
- [x] **Tests.**
  - The intents each surface builds, with their extras.
  - `MainActivity`'s doors: Today, a week, a way, the cities, each once per touch.
  - The outing page's walk row.
  - The lists' rows: `assertAccessible()` on each, with the role and the label.
- [x] **The record.** Strings in English and Italian, the CHANGELOG, and ADR 0008 decision 8
  confirmed with what the fix found.
- [ ] **On a device (owner).**
  - **Today:** with the app left on Settings, the tile, each widget and the counting
    notification each open Today.
  - **The week:** the weekly summary opens its week.
  - **A way:** a stage's notification opens its way.
  - **A city walk:** from an outing on a city walk, to the walk.
  - **The cities:** the cities door.

**Acceptance (23.1):**
- Every system surface opens the page that holds what it shows.
- A city walk is one touch from its outing.
- Every row that opens something looks like it does.

#### Step 23.2 — Your outings («Le tue uscite»)

Outings have their own pages, their routes and their GPX, but no list. Finding one from three
weeks ago means paging History day by day, and with the outings measured with GPS that is the
one list a walking app is expected to have. «Delete all routes» counts routes the reader cannot
see one by one.

- [ ] **The door** (`SessionsScreen`). On the Outings page, under the plans, beside the cities'
  door (and Phase 20's rounds): «Your outings» / «Le tue uscite», with their count. It is shown
  from the first outing kept, not before.
- [ ] **The page** (`:feature:sessions`, `OutingsListScreen`, one key in `:app`).
  - **One sentence first:** «48 outings since March; 21 with their route».
  - **Then by month, newest first.** Each row is the one the day's lists draw (`OutingList`):
    its mark, name, day and time, distance and time in motion, «measured» where it is.
  - **One with a route** wears 21.3's still route, small, at its start. It is read only for
    the rows on screen, from `routes.db`, never making it (`RouteFile`).
  - **A row opens the outing's page.**
  - **No paging library.** A few thousand outings are one query of numbers; only the routes are
    read lazily.
- [ ] **A filter:** «All | With their route» («Tutte | Con il percorso»). Your data's routes
  row («21 routes, 3.4 MB», `DataSection`) opens this page with that filter. That is the way to
  see which outings hold a route, and to delete one from its page, rather than all at once.
- [ ] **Tests.**
  - The list's grouping and its sentence (`:core:domain`, pure).
  - The page's UI tests: empty, one month, many, the filter; Italian on a small phone, twice
    the text size, an open foldable.
  - The routes read only for the rows shown.
  - Your data's row opening the filter.
- [ ] **The record.**
  - Strings in English and Italian, with plurals.
  - The CHANGELOG.
  - The guide's «Outings».
  - The README: a screenshot of the list, if it earns its place.
- [ ] **On a device (owner).** The list after a few weeks of outings, scrolled fast; the
  filter; from Your data to the routes, one deleted from its page and the list after. In both
  themes, at a large text size, with TalkBack.

**Acceptance (23.2):**
- Any outing is two touches from the Outings page.
- Which outings hold a route can be seen, and each can be deleted on its own.

**Left out of the doors' review**, each decided or noise:
- Links to the guide's chapters from the pages they explain: the guide stays in Settings after
  the first day (`TodayScreen`), and the map's note was moved to the guide on purpose on
  20 Nov 2026.
- The streak's day marks in Insights: the records already open History.
- An end notification after Stop on the lock screen: the `sessions` channel carries only what
  can still be taken back (§8).
- A walk found in the steps opening a page: decided, it opens nothing.
- Insights' way card opening the way instead of the Ways list: Today's card already does.
- A touch on the continent's map naming more than the city: ADR 0015 decision 12.

---

## 12. Testing strategy

- **Unit (JVM):** `StepAccountant`, calculators, streaks and records, formatting. Use a fake clock and system snapshot. This is where most of the test effort goes.
- **Robolectric:** Room DAOs and transactions, receivers, the notification builder.
- **Compose UI tests:** onboarding, Today, Settings (with its data rows), the step calibration, History, Insights, the Outings page and its editor. Each state they draw also passes `assertAccessible()` (`:core:testing`), and each screen is walked at twice the text size on a 360dp phone and on an open foldable (Phase 7, `docs/adr/0012-foldables-and-accessibility.md`).
- **Contrast:** `ContrastTest`, every text ink on every ground, both dresses and themes, at 4.5:1.
- **Glance:** unit tests for the layout chosen at each size.
- **Manual device protocol** (`docs/testing/device-protocol.md`):
  - reboot
  - full shutdown overnight
  - Doze
  - time-zone change
  - revoking the permission
  - stopping the app from the system's task manager
  - app update
  - low battery
- **Battery:** the §9 checks at the end of Phases 1, 4 and 6, with the numbers logged in `docs/battery/`.

---

## 13. Working with Claude Code

### `CLAUDE.md` (create in Phase 0; keep it short)

Include:

- A one-paragraph project summary and links to `VISION.md` and `PLANNING.md`.
- Commands:
  - `./gradlew assembleDebug`
  - `./gradlew test`
  - `./gradlew lint`
  - `./gradlew spotlessApply`
  - the forbidden-permission check task
- The module map and dependency rules (§2).
- **Invariants:**
  - Never add `INTERNET`, location or other forbidden permissions (§10).
  - Business logic goes in `core:domain`, with unit tests.
  - Never poll or run timers while the screen is off.
  - Don't change the tracking engine without updating the tests for the §4.6 edge cases.
  - All user-facing strings go in resources, in both English and Italian.
  - No new third-party dependency without approval; its license must be GPL-3.0-compatible. Charts are custom Compose `Canvas` code, never a chart library.
- Code style: Kotlin official style, no `!!`, `Flow` over `LiveData`, immutable UI state.

### Workflow

1. One phase at a time. Start each session in **plan mode**, pointing to the phase section: *"Read PLANNING.md §11 Phase 1 and §4, propose a plan."*
2. Use one branch and PR per coherent chunk (e.g. `phase-1/accountant`, `phase-1/service`). Keep PRs small.
3. Write tests first for `core:domain` logic, and ask Claude Code to run them before finishing.
4. After each merge, tick the checkboxes in this file and record decisions as ADRs in `docs/adr/`.
5. Field-test the device-dependent phases yourself; paste the diagnostics logs back to Claude Code when something is off.

---

## 14. Risks and mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| OEM task killers stop the service | Steps lost if a reboot happens while the service is stopped | Onboarding guidance; paused state in the widget and UI; the counter is cumulative, so no loss without a reboot |
| Buggy sensor timestamps on some devices | Wrong minute or day attribution | Timestamp validation and fallback to the arrival time; diagnostics log |
| Small or zero FIFO on low-end devices | Coarser minute attribution | Totals stay correct; the gap distribution heuristic |
| Lost or leaked release signing key | Users can't update without uninstalling (and losing data); a future Play listing can't reuse the key | Keystore created in Phase 0, kept outside the repo with an offline backup, stored only in GitHub Actions secrets |
| Play policy review of the `health` foreground service (only if Phase 9 happens) | Delay of a Play release | Continuous step tracking is the documented use case; prepare the declaration and video early |
| Users dislike the persistent notification | Uninstalls | Low-importance, useful content (live steps); explain why in onboarding; the user can minimize the channel |
| Abrupt power loss | Steps since the last batch are lost | Bounded by the latency window (10 min with the screen off), and only for steps the processor has not been awake for since; the wake-up variant was weighed and rejected (`docs/adr/0002-sensor-reporting.md`) |
| The wake-up step counter during an outing costs more than planned, or a phone lacks it | Battery, or late signals | Only while an outing counts, two wakes a minute at most while walking, ending by itself; without the sensor the page says signals can be late. To measure on a device (§9) |
| Android 17 RemoteViews bitmap cap | Widget crash | One ring bitmap per card, capped at 416 px a side (0.7 MB); the bars are boxes; test on API 37 |

---

## 15. Decisions log and open questions

### Decided

The decisions up to 1 Nov 2026, from Phase 0 on, are in
[`docs/planning/02-decisions-to-1-nov-2026.md`](docs/planning/02-decisions-to-1-nov-2026.md),
those of 2 Nov 2026 (the review, Your year on foot, 1.1.0 prepared) in
[`docs/planning/03-towards-1.1.0.md`](docs/planning/03-towards-1.1.0.md), and those from 1.1.0's
tag to 18 Nov 2026 (the family's circle and icon, the reviews of the pages, location: ADR 0016,
Phases 15 and 16 as built) in
[`docs/planning/04-decisions-2-to-18-nov-2026.md`](docs/planning/04-decisions-2-to-18-nov-2026.md),
all frozen (the index, [`docs/planning/README.md`](docs/planning/README.md), lists each by its
title).

This log starts with Phase 16 built (18 Nov 2026); its entries are written below.

- **18 Nov 2026.** **Four answers after Phase 16, and the plan archived** (owner, on the four
  points left with them at the end of Phase 16: "OK for all four").
  - **The legs are in ADR 0016.** Decision 8 is amended: an outing's numbers also hold its
    legs (`session_leg`) and the climb's two columns, schema 8; `routes.db` is at schema 2.
    Numbers, not places. It closes what archive 04, «Phase 16, the outing's page», left to
    the owner's word.
  - **CLAUDE.md names Phase 16's classes** in its table of modules, and says where the phases
    stand: 15 and 16 built and waiting for a phone, 17 next.
  - **The permission's three behaviours stay as built.** An outing that started from the
    steps stays on the steps, also when the permission is given from its card (the card says
    «your next outing is measured»); the editor's switch does not ask again after a refusal;
    an outing started with location off on the phone stays on the steps. Each keeps an outing
    what it was when the reader started it: what it costs does not change under way, and no
    outing is called measured whose start no satellite saw. One thing was put to the owner
    and is not decided: after a refusal the card says it once, and from then on a plan with
    «Measure with GPS» on counts from the steps without a word; turning that switch on
    again could be read as asking again, so the next start of the plan puts the question
    once more. It would amend what `GpsStart` says of a refusal (ADR 0016 decision 2's
    "asked in context"), so it waits for their word.
  - **The plan archived** (`docs/planning/04-decisions-2-to-18-nov-2026.md`): the log had
    reached some 965 lines, and Phase 17 starts from a short one. Phases 15 and 16 stay in
    §11, not done until a phone has proved them. «Open» read again: nothing closed; where a
    box or a phase's section named an entry of that log, it now names archive 04. The index
    is run again.

- **18 Nov 2026.** **The switch turned on again asks again** (owner, on the one behaviour of
  the three that was put to them as worth changing: "what if you change it, so as to do the
  best thing, one that adds value to the app and keeps it premium?"). It changes the third
  point of the entry above, and ADR 0016 decision 2 is amended with it.
  - **The rule** (`GpsStart.asksAgain`, pure): a plan saved with «Measure with GPS» turned on
    by the reader's own hand, on a phone where the permission is what is missing and Passo
    would not ask by itself, forgets the remembered refusal (and that it was told). The next
    start of an outing with GPS then asks as the first did, through the one door
    (`OutingStarter.planSaved`). The editor never asks; a plan saved with the switch as it
    was asks nothing.
  - **The gesture is kept, not inferred** (`PlanEditorState.gpsTurnedOn`). The first rule
    compared the stored plan with the draft, and a test failed on it rightly: off and on in
    one sitting, which is what the note tells the reader to do, left the two equal.
  - **Approximate alone follows.** It is a refusal of what was asked, so it had the same
    silence. `GpsStart.step` now asks for the precise one, as the first time, where Passo has
    no answer to remember (after the gesture, or the approximate one given in the system's
    page without Passo ever asking); never by itself after a refusal or a grant taken back
    to approximate there.
  - **The switch says what is true** (`GpsSwitchNote`): what it gives and costs; «Passo asks
    for your location when the outing starts» wherever it will, the first time included; and
    after a refusal, with the switch still on, that the outing counts from the steps and that
    off and on is the way to be asked. In that state the estimate's note no longer says
    «measured with GPS», and the note on the phone kept awake is not shown for a receiver that
    will not run (`measuresWithGps`).
  - **Not changed**: an outing that started from the steps stays on the steps, and one
    started with location off too (the entry above). Where Android no longer shows its
    request, the card's «Allow location» still opens the system's page.
  - The guide (a sentence in «With GPS»), CHANGELOG. `GpsStartTest` (the gesture, each note,
    approximate alone), `PlanEditorScreenTest` (each note, off and on, Italian on a small
    phone), screenshots looked at.

- **19 Nov 2026.** **The review of Phases 15 and 16, and its fixes** (owner: a full review of
  the two branches not yet in `main`, then "fix every bug, major and minor, with the tests",
  keeping the app premium). The two branches were reviewed against `main` and against each
  other; the fixes sit on a branch of their own on top of Phase 16, so the three merge in
  order with nothing rewritten. Seventeen findings, sixteen fixed; one was already mended by
  Phase 16 (deleting a short outing no longer makes `routes.db`: `RouteFile`).
  - **No metre lost by the meter** (`RouteMeter`). A fix late for its batch (the receiver's
    own delay behind a settle through now, with the screen on) was read as stillness: the
    line's end jumped to it and the metres to it were lost. It is now measured from the line's
    end as any other. The steps after the last fix are kept span by span (the tail): a late
    fix covers only what was walked before its moment, and a pause or a stop after a few
    seconds under an arcade counts them as the steps' instead of dropping them. The service
    asks the receiver for its fixes in line (`settleRequests`) and settles through the request
    each completed flush answers, never through a later one still on its way. §4.6's spirit:
    `RouteMeterTest` (late fixes, a pause under an arcade).
  - **The cap** (ADR 0016 decision 14, amended the same day). «Still walking?» says the
    minutes really left (`SessionAnnouncement.capAhead(session, now)`): asked late, after a
    sit at a café, it said fifteen. A pause is no longer time towards the cap: `resume` moves
    it on by the pause, and a question already open stays open for it, so an outing paused
    across its cap is asked before it ends instead of ending at the first step, unasked.
  - **The service's last write.** As it goes (stopped by the system, counting turned off, the
    phone shutting down), an outing with GPS has its line cut where it stands
    (`SessionTracker.cutRoute`), its distance written with it and its last points with the
    route; before, the open batches' metres and the points not yet written went with the
    process.
  - **The legs** are written whenever they changed: a batch writes every leg changed since the
    last write (`TrackingRepository`, a cache of what it wrote, lost with the process, so the
    first batch after one writes them all), not the last five; «Keep going» lays a quarter of
    an hour's at once. `Legs.lay` numbers a new leg after the last, never by count.
  - **A deleted route stays deleted.** An outing's page offers «Delete this route» only once
    the outing is over (while it counts it says when it can); `RouteRepository.forget` keeps
    the route deleted in this process, by the outing's id and start (an outing reopened by
    «Keep going» writes nothing to it). «Delete all routes» keeps the one still being written,
    and its dialog says so while there is one. New strings, English and Italian.
  - **The first route seen as it comes.** What watches the routes (the outing's page, Your
    data) watches for `routes.db` too: it decided once whether the file was there, so on the
    first outing with GPS a page already open never showed the route.
  - **The climb.** On a phone with a barometer, a height the satellites gave alone (readings
    lost or late) is counted past the satellites' ten metres, not three
    (`Altitude.Height.fromPressure`); a point waits for the readings to reach its own second;
    a climb is priced at the cadence of the leg it was walked in (`Legs.cadenceOf`), not the
    latest batch's: a hill run up and told after the walk began again cost the walk's rate.
  - **A damaged backup stays harmless**: a leg longer than a leg is dropped on import and never
    cut into splits (`Legs.MAX_METERS`): a figure of thirteen digits hung the outing's page.
    A non-finite number reads as none.
  - **Small ones**: saving or sharing an image says it failed when it could not be encoded
    (`SharedFiles`, its writers say whether they wrote); a height just below the sea reads
    «0 m»; `Fix.altitudeMeters` documented as above the ellipsoid; the manifest's comment
    keeps to the words §10 allows. `:core:designsystem` gets its `robolectric.properties`, for
    its first Robolectric test.
  - **The control review** of these fixes (the same day) found nine more, eight mended:
    an import puts back a route the reader deleted in this process (`RouteRepository.restore`;
    the guard is for the service's writes); the metres a cut of the line settles (a pause, the
    service going) are taken as any settled metres, so what they cross is told and a goal they
    reach ends the outing there (`SessionTracker.takeHeldSignals`; a pause had the same silence
    before); the phone going down asks the receiver for its fixes first, and writes the climb
    of the last points; every request for the fixes is in line with its own answer, a command's
    too (`FixRequest`); «Still walking?» is asked even from a batch handed over after the cap,
    at one minute; a fix at a stop after steps no fix saw measures them instead of moving the
    line's end (a pre-existing case beside the late fix); a climb on a leg too short to say its
    pace takes the one before it (`Legs.cadenceAt`); a damaged leg skipped keeps the next split's
    time right. Not changed: an open question cannot read «15 minutes» for an hour after a long
    pause, since the cap moves by the pause and the minutes left are those before it (pinned by
    a test).
  - Tests for each: `RouteMeterTest`, `SessionTrackerCapTest`, `SessionTrackerRouteTest`,
    `SplitsTest`, `RouteHeightsTest`, `BackupCodecTest`, `MeasureFormatterTest`,
    `SessionRepositoryTest`, `RouteRepositoryTest`, `OutingScreenTest`, `DataSectionTest`,
    `SharedFilesTest`. Phases 15 and 16 stay open for their checks on a device.

- **19 Nov 2026.** **The fixes reviewed for what the reader sees** (owner: a critical review of
  the fixes above for function and UI, then "do points 1, 2 and 4, with premium finishes", and
  point 3, the pause that moves the cap, accepted as decided). Each changes what the entry
  above built:
  - **«Delete all routes» never runs empty.** Its row counts and weighs the routes the reader
    can delete, every one but the route still being written (`DataContents.deletableRoutes`,
    `deletableBytes`; `routes` still counts them all, for «With the routes»), and is not shown
    for that one alone. Before, the row stayed after the deletion with «1 route», and a second
    touch deleted nothing. The dialog's extra sentence goes with the problem it explained.
  - **The question before the cap is never stale.** Its notification states the cap's clock
    time («ends at 14:05»), not minutes counted when it was posted, and the system counts down
    to it in its header (`setUsesChronometer`, `setChronometerCountDown`: drawn by Android, no
    timer of Passo's, §9 untouched); it goes by itself at the cap (`setTimeoutAfter`), is put
    away at a pause, whose time is not passing for the cap, and posted again at the resume
    while the question is open, silent (the channel is). The voice still says minutes: said
    once, at that moment, they are true.
  - **An outing's page follows the outing while it counts**: what a route can do (share, save,
    delete) appears once it is over, all together, and the note saying when deletion would be
    possible goes. The image shared is a summary of an outing that is over, and no longer
    recorded for a live one.
  - **Two finishes found on the way** (screenshots looked at): while the outing counts and no
    satellite has answered yet, the page no longer says «The satellites never answered during
    this outing», but that the route will appear there as soon as one does
    (`outing_no_fix_yet`); and an outing with no fix at all says its distance once, as the
    estimate it is, not «100% of it from your steps» over the same figure.
  - **Point 3 checked to its end**: the wake lock of a phone with no wake-up counter is bounded
    by the cap; it is let go at a pause and taken again at the resume with the moved cap
    (`applyRegistration`), so the moved cap leaves nothing behind. ADR 0016 decision 14's
    amendment says the owner confirmed it, and the notification's new shape.
  - The guide (the end of an outing, a route), CHANGELOG and VISION say so. Tests:
    `SessionNotificationsTest`, `OutingScreenTest`, `DataSectionTest`, `BackupRepositoryTest`.
  - **Its control review** (the same day) found the edges, all mended: «over» is over for good,
    not only not counting: an outing ended a moment ago that «Keep going» or «Resume» can still
    take back offers nothing of its route on its page (`OutingUiState.over`, from `LiveSession`),
    and «Delete all routes» keeps its route too (`BackupRepository`, the outing written as under
    way or held by the service as reopenable); otherwise a route deleted then would have stayed
    cut short when the outing went on. A paused outing with no fix reads «No satellite has
    answered so far», never that the steps are counting. The dialog of «Delete all routes» says
    which lines go when one is kept (`data_delete_routes_text_keeping`), instead of «every
    line». The question before the cap comes back at the resume only if it was on show when the
    pause put it away (one swiped away stays away), comes back after an end taken back before
    the cap, and is not asked at all from a batch handed over after the cap, where nothing can
    answer it and the end that follows offers «Keep going». `DataContents`' counts have no
    default that hides a row.

- **19 Nov 2026.** **Phase 17's proposal: put away, and found again** (owner, reading Phase 17
  before it starts, on the card that once dismissed could come back only by itself: "record
  both decisions"). Two points of §11 Phase 17 settled before the work begins; its proposal's
  box, its tests' box and its edge cases say them.
  - **Another 2% is measured from the curve last proposed**, the one the reader put away with
    «Not now» or went back from with «Go back to the estimate from your height», never from
    the length in use: that one differs by 2% or more already, or nothing would have been
    proposed, and the card would return at the next outing.
  - **The calibration page keeps the way back.** «Not now» puts the card away from Settings,
    where the reader did not ask for it; on the calibration page, where they come to look at
    their step, a quiet row stays whenever a curve counts and is not in use («Passo has learnt
    your step on 14 km of your outings with GPS»), and touching it opens the card again, with
    the curve as it is that day. A reader who changes their mind does not wait for outings
    that may never move a steady step by 2%; that rule is kept for what Passo puts forward
    unasked. Still never a notification.
  - Nothing built yet; the strings come with the phase.

- **19 Nov 2026.** **Phase 17 reviewed against the code before it starts** (owner: a review of
  the phase "to check the other points too", then "yes, all of them; settle the points to
  clarify with your proposals too; always the choices of a premium app"). §11 Phase 17 was read
  against Phase 16's code with its review's fixes (#74); seven points corrected, four made
  exact. Its boxes and edge cases say each; here, why.
  - **The curve has two parts, split at `RUNNING_CADENCE`.** «Beyond the ends flat» and «the
    profile's own elsewhere» (two edge cases) said two things. Now within a gait that has bins
    the curve is flat beyond them up to the split; a gait with none keeps the profile's length.
  - **Five places read one walking length, not the curve** (`walkingMeters`: Today's line under
    the distance, a city walk's steps, the step's rows in Settings and on the calibration page;
    the first run too, where no curve can be). The box said every caller follows unchanged:
    true of those that measure, not of these. In `LEARNED` they read the curve's length at the
    usual cadence, the card's own figure, so the app says one number. The curve carries it; the
    reader's own lengths stay beside it, so accepting loses no length measured or typed.
  - **1.1.0 reads `LEARNED` without failing** (tag `v1.1.0`: `ignoreUnknownKeys`, an unknown mode
    read as the height's), so the mode is written as it is; the fallback the box held in reserve
    is dropped. A downgrade costs the curve, silently.
  - **The dashed line is the length in use**, named by its source: against the height's alone,
    the card would compare a reader who calibrated with a number they do not use.
  - **Another length chosen puts the curve away.** The wizard or a typed length while a curve
    counts would have brought the card back at once, by the 2% of the entry above. The wizard's
    running walk changes no mode, so a running length set after accepting wins over the curve's
    running part, gait by gait.
  - **Calories are named with the distances**: they are computed from the distance and move by
    the same share; the sentence said only distances.
  - **The guide has no «Measure your step»**: that is the calibration page's title. The part on
    the step learnt goes in «Estimates, and what they rest on» and «With GPS».
  - **Made exact**: a bin's 400 m are in all, from two outings or more; each sample within the
    limits of its gait; pooling weighted by steps; the usual cadence is the median step count of
    the active minutes (40 steps or more) over 28 days, and the bin holding it is the one that
    must count; «Your step at this pace» is that outing's own samples, not the curve, and absent
    without one; the last curve proposed is the phone's state, in DataStore and never in the
    backup file. Checked and right as written: the minutes are kept for good (the 28 days can be
    computed), the day's distance is minute by minute through `forCadence`, `changeProfile`
    freezes the past first, «Apply profile to past data» reads the profile in use, the samples
    go with their outing and travel in the JSON, an outing under way keeps its lengths.

- **19 Nov 2026.** **Phase 17 reviewed for what the reader sees** (owner: a functional review of
  the phase as a premium app, "only real added value, no noise"; then "record all three").
  Agreed as planned: the sentence first, an explicit choice and never a notification, the past
  frozen, the wizard kept, and a curve in use that does not update itself (today's distance
  would jump in the middle of a day). Three changes, in §11 Phase 17's proposal box, its tests
  and its edge cases:
  - **The proposal where the reader has just measured.** The page of the outing whose samples
    made the proposal ready says, under «Your step at this pace», that Passo can now use the
    measured step, and opens the calibration page on the card. Settings and the calibration
    page are opened seldom, and a card only there might never be seen. While the proposal
    waits, on that outing's page alone.
  - **The way back keeps a calibration.** «Go back» returns to what was in use before, named as
    it was (the step measured, the one typed, the height's estimate), not always to the
    height's: the reader's own lengths stay beside the curve, and the curve remembers the mode
    it replaced.
  - **«Apply to past days too», offered after «Use it».** Accepting moves today and the days
    after, not History's; the step of 6% between yesterday and today would read as a fault.
    One line says so, with the action that already exists in Settings, asking as it does
    there. Past days change only when the reader asks, as before.

- **19 Nov 2026.** **The places on the spot dropped, the city walk measured** (owner, after a
  technical and a functional review of Phase 18 and a look at Phases 15 to 20 together: "18
  and 19 I leave out; 20 I keep, and we review it later"; then, on the counter-proposal, "I
  agree, go ahead"). ADR 0016 decisions 13 and 14 are amended with it, CLAUDE.md and VISION
  follow.
  - **Phases 18 and 19 «On the spot» are dropped.** Their value is real and rare: a reader must
    be in one of forty cities, or on a way, a few times a year if ever. Their cost was high and
    fell on every reader: the first Phase 18 changed what a city walk's progress is (a sum of
    distances, `WalkDays`, to a place on the line), code every walk from home runs, and with
    the screen off its promises (a place told within 30 m, the route's leaving felt in 20 s)
    needed a widening of §9's exceptions. Phase 19 needed the first Phase 18's engine and a
    fine line for every way.
  - **Not done instead: a way's kilometres from GPS** (the owner's first proposal). A way from
    home goes on by the day's distance, all its steps; an outing with GPS would correct a few
    percent of a small part of them, and make the way and History say two numbers (ADR 0016
    decision 6: the day stays the steps'). Phase 17 corrects every step of every day, the
    way's included: it is the answer to that wish.
  - **Done instead: a city walk measured with GPS** (the owner's second proposal, as the new
    Phase 18). The walk stays imaginary; with «Measure with GPS» on its page, its distance is
    the fused one and the outing keeps the reader's real route. The value is not the precision
    (the places are imagined; after Phase 17 the steps are within a few percent) but not having
    to choose: today a reader who walks 5 km has either the walk (the story, the places, the
    stamp) or an outing with GPS (the map, the kilometres, the pace, the climb), never both.
    Its cost is small: the fused distance is already what `placesPassed`, `WalkPlaces` and
    `WalkDays` read in an outing with GPS (decision 5), so the phase is the switch, the start
    through the one door, and the words. It changes what decision 13 made plain on 18 Nov
    2026 (a city walk from home never reads the satellites): the city's map still shows the
    reader's point on the walk by the distance walked, never their real position, so nothing
    reads as the navigation that decision feared.
  - **Phase 20 stays, to review before it starts**: its section says what it took from the
    dropped phases and must now build or write out itself, and what their review found that
    holds for following any line. Phase 19's number stays, so the documents keep their names;
    the dropped text is in the plan's history (`git show b842547:PLANNING.md`).

- **19 Nov 2026.** **Phase 20 reviewed: the usual round alone** (owner, after a technical and a
  functional review of Phase 20: "only the usual round, as Phase 20; the following of a route I
  never want to build, and not even keep in the plan; one day perhaps something like it,
  designed from nothing"). §11 Phase 20 is rewritten; ADR 0016 decision 14 is amended, CLAUDE.md
  and VISION follow.
  - **Following a route is not built and not planned**: a GPX from another app, «Walk it
    again», «The other way round», off the route and back on it. It carried the costliest part
    of the old plan (`LineSnap`, a line followed with the screen off, a start far from it, the
    signals' own page) for little: Passo draws no ground outside its forty cities, a map app
    follows a GPX far better (decision 11 already sends the reader there), and a reader's own
    streets need no guide. A GPX from elsewhere was also «anyone else's route», which decision
    16 leaves out. The note «To review before it starts» goes with it.
  - **The usual round stays, and is the phase.** What the review changed in it: the match
    asks for the same direction (a loop walked the other way round matched by the four fifths
    alone); the comparison brings each time to the round's usual length (a shortcut read as a
    faster walk); the line at each kilometre needs no widening of §9 (a signal of distance,
    told at the batch like a goal; the dropped Phase 18's window of 2 s was for places along a
    line, which a round has none of); the rounds live in `passo.db` as numbers and names, with
    no line of their own, so no place outlives its outing and the history survives a route
    deleted.
  - **Your rounds** (the functional review): a round found is listed by itself, under a door
    on the Outings page, with its walks and a small chart of their times, so the comparison
    has a home besides the outing's page. No record board, no fastest mark: one reader and
    their own rounds.
  - **The twelve hours** of decision 14 now start no outing: they were for a way walked on the
    spot and a followed route, both dropped. Every outing keeps four, «Keep going» as before.

- **19 Nov 2026.** **Phase 17 built: the step that learns** (owner: "implement Phase 17,
  following CLAUDE.md", in plan mode first, the plan approved). §11 Phase 17's boxes are ticked
  but the phone's, which opens in «Open».
  - **One branch, by the owner's choice.** The phase was built on the branch of PR #74 (the
    review of Phases 15 and 16), not on a branch of its own as «One phase, one session» says:
    the owner wants that branch merged into `main` once, at the end, with all it holds. The PR's
    title and description say it holds Phase 17 too.
  - **Built.** `StepLengthLearner` (`:core:domain/calibration`, pure): the bins, the 400 m from
    two outings, the median, the curve made non-decreasing, the usual cadence, the share of the
    last 28 days' distance, the proposal and the outing that made it ready. `LearnedStep` and
    `StepPoint` in `Profile` (`:core:model`), `StepLengthMode.LEARNED`; `StepLengths` reads the
    curve where a part is learnt, and `walkingMeters` is the curve's length at the usual cadence,
    so Today's line, a city walk's steps, the onboarding's figure and the step's rows follow with
    no change of their own. `LearnedSteps`: accepting, the way back, a length chosen in a dialog,
    the wizard's (`StepCalibration.apply`). `LearnedStepText` writes the curve in DataStore
    (`profile_learned_step`), and the curve put away (`step_curve_put_away`, never in the file).
    `StepLearningRepository` (`:core:data/learning`): what the samples teach, read while a page
    shows it, and the answers, every profile change through `TrackingRepository.changeProfile`.
    The backup's `ProfileDto.learnedStep`; `Backup.stepFallback` and `ImportReport.stepFallback`,
    said in the import's summary. `StepCurveChart` (`:core:designsystem`, Canvas). The card, the
    note after «Use it», the quiet row and the way back (`StepLearningViews`, `:feature:settings`);
    the outing's page's line and its way to the card (`CalibrationKey.card`, through `:app`).
  - **Made exact on the way**, each in the code with its reason:
    - the usual walking cadence is the median of the active minutes below the running cadence
      (a run is not a pace to name a walking step at), over the 28 days that end today; the
      minutes are read when a page opens, not followed, so a card does not change under the
      reader's eyes;
    - a usual cadence under 80 is read in the first bin: the curve is flat there, so the length
      the card names is that bin's, and a slow walker would otherwise never be offered one;
    - a bin's point sits at its samples' mean cadence, weighted by their steps; the pooling runs
      along the whole curve, so the step never shortens across the running cadence either;
    - the length at the usual cadence is read from the curve, not stored beside it: one number
      cannot drift from the other;
    - the quiet row needs the curve to count at the usual pace, not the 2%: a reader who asks
      sees today's curve even when it would change little, and the card says so («close to the
      74 cm in use, so distances and calories would hardly change»);
    - with a curve in use, the reference for another 2% is the curve in use: an updated curve
      is proposed when the outings move it that far, and a curve in use never changes unasked;
    - the outing that made the proposal ready is the first, in the order they were walked, whose
      samples with those before it would have made it; after «Not now» or «Go back» only the
      outings after the curve put away count (`LearnedStep.throughMillis`), so the line never
      lands on an old outing's page; and only once the outing is over for good;
    - the running row names the running part halfway along it; the step's dialogs offer «Learnt
      from your outings» first, and the measured or typed length kept there over a learnt step is
      the way back to it; in Settings the way back is a row under the step's, with what it does
      («From today, distances use it again; past days keep theirs»), on the calibration page a
      button under «Now»;
    - an import whose learnt step cannot be read falls back to the height's estimate and says so,
      like a mode not known; the backup's profile is read through `sanitized()`.
  - **The README.** `calibration.png` showed the wizard's result, which did not change; it is
    redrawn on the calibration page with the learnt step's card, the phase's view, so the set
    stays as small (owner's rule), and its caption no longer says "no GPS"; the feature line says
    both ways of measuring the step. The card says, under the chart, that it is still an
    estimate.
  - **Checked**: `spotlessCheck`, `test`, `:app:lintDebug`, `:app:checkForbiddenPermissions`
    (green here but the seven tests that walk a Room migration, which fail on Windows by a path
    comparison and are CI's to prove; no schema changed). New tests: `StepLengthLearnerTest`, `StepLengthsTest`, `LearnedStepsTest`,
    `LearnedStepTextTest`, `BackupCodecTest` (a curve, a file from before, 1.1.0, a mode unknown),
    `StepLearningRepositoryTest` (accepting freezes the past first, «Apply to past days too», «Not
    now», «Go back», a length chosen, an outing deleted, the settings file),
    `CalibrationScreenTest`, `SettingsScreenTest` and `OutingScreenTest` (the card in each state,
    the row shown, not shown and opening the card, the note and its dialog, the way back, the
    outing's line shown and gone; both themes, twice the text size, Italian, an open foldable);
    the screenshots looked at. CHANGELOG, the guide in English and Italian («Estimates, and what
    they rest on», «With GPS»), VISION, CLAUDE.md.

- **20 Nov 2026.** **Every route on plain ground** (owner, reading PR #74's map: "better not to
  put the maps we have under it in any case, and keep the GPS track always drawn the same, so
  it is coherent for every outing"; and of the line under the map, "only the explanation of
  what is missing can go, which once read seems noise; the legend of the dashes stays, and that
  is important"). ADR 0016 decision 11 is amended; §11 Phase 16's `RouteMapView` box, its
  README box and one edge case follow. It changes archive 04's «Phase 16, the outing's page:
  the route, the splits, the climb» (`docs/planning/04-decisions-2-to-18-nov-2026.md`, the
  `RouteMapView` item), where the city's ground was built.
  - **No city's map under a route.** `RouteGround` came out, and with it the ground's layers
    in `RouteMapView` (land, parks, lakes, canals, streets, the sea), their three colours, and
    the decoding of a city's map when the page opens. Why: that ground was a city walk's map,
    covering the walk's frame (Milan's: some 3 by 2.5 km around the Duomo and the Navigli), not
    the city; it came under a route by a rule the reader could not see (four fifths of the
    points inside that frame), so two outings a street apart looked different, and a fifth of
    a route could run off its edge. It reached few outings anyway: the plain ground is the map
    almost every reader sees, and it is now the only one, the same on the page and in the
    image shared.
  - **Under the map, the dashes' line alone, and only when a stretch is dashed**
    (`RouteShape.hasLaid`): a dash is an estimate drawn among measures, and says so where it
    is. «Passo carries no street map» goes to the guide («An outing's own page», in English and
    Italian), once, with the way to a street map: the GPX, shared with a map app.
  - **Phases 18 and 20 are not touched** (owner: Phase 18 stays as written, and whether to
    build it is for later). Their sections still say a route lies on its city's map where
    Passo carries it (Phase 18, «The walk measured»; Phase 20, «Your rounds»): that line is to
    be read against this entry when either phase is taken up.
  - **The README.** `outing-route.png` redrawn on plain ground, its alt text with it; the
    feature line no longer names a city's map. CHANGELOG, CLAUDE.md (`:core:domain`'s row).
  - **Checked**: `spotlessCheck`, `test`, `:app:lintDebug`, `:app:checkForbiddenPermissions`
    (green here but the seven tests that walk a Room migration, CI's to prove; no schema
    changed). `RouteShapeTest` loses `RouteGround`'s three tests and gains `hasLaid`'s;
    `OutingScreenTest` checks the dashes' line under a route with a laid stretch and none under
    one without; `RouteMapGalleryTest` draws one gallery, plain; the screenshots looked at.

- **20 Nov 2026.** **The open outing planned** (owner, after the outings with GPS: "I miss an
  outing with no goal". At the park: walk, look at the route on the way, stop at the car, or
  forget it. A brainstorm first, against the code and the known apps; then "prepare the plan in
  PLANNING, a first small phase, the whole development in steps, detailed enough that a new
  session starts from it"). §11 gains Phase 21 (three steps) and Phase 22 (later). Phase 20 is
  updated to build on 21. «State» and CLAUDE.md say 21 comes next. Nothing is built yet, and no
  ADR is amended yet: 21.1's first box does that.
  - **What the known apps do** (their help pages, read on 20 Nov 2026: Apple Watch, Strava,
    Garmin, Fitbit and Google, Samsung Health, Nike Run Club, Runkeeper, Komoot, Pacer, Huawei):
    - No goal is the shortest way in, and a goal is an extra step: Apple's «Open goal», Nike's
      «Basic», Samsung's «Basic workout», Huawei's «None».
    - The live screen is numbers, with the route a swipe away.
    - Auto-pause is advised against for walks (Strava) or left out for them (Apple).
    - A forgotten stop gets a reminder that never ends anything (Apple), or a crop afterwards
      (Strava, Komoot, Pacer).
    - A forgotten start is credited (Apple, Samsung).
    - A kilometre is told by a pulse or by the voice.

    Passo's time in motion already does what auto-pause tries to do. Its end at the last step,
    after 15 minutes still, handles a forgotten outing better than any of them.
  - **What the code showed:**
    - The goal is in every outing's shape: about 130 places in 23 files (the sentence, the bar,
      the notification's `ProgressStyle`, the widgets' «12 of 20 min», the end at the goal).
    - An outing's page already follows the outing while it counts, but nothing on the card
      under way opens it: «See the outing» was on the finished card only.
    - «Libero» is the pace («a ritmo libero»), and «Passeggiate» are the city walks, so neither
      can name this.
  - **Decided by the owner:**
    - «Nessuno» / «None» in the editor; «uscita aperta» / "open outing" in the documents.
    - A signal at each kilometre with GPS, every ten minutes without.
    - The card's shape: the route with GPS, the minutes without, and the day's share.
    - «See the outing» while it counts, for every outing.
    - Phase 21 as a new number, before 18 and 20.
    - The forgotten start as Phase 22, later.
    - Left out: Garmin's turn-around (a time goal with its signal at half already gives it),
      the crop, and auto-pause.
  - **Proposed for the owner's review.** The owner asked: "what do you really recommend, what is
    best for a premium app". Both proposals are in §11 Phase 21, «How it ends».
    - *A short ride stays inside the walk, as the ends already are.* An automatic end at a ride
      would see the ride only through the first fix after it, which comes after the steps that
      woke the receiver, and indoors may not come at all. It would also end the walk at every
      tram, which Phase 15 keeps inside the walk on purpose. 21.1's device check walks the one
      case left (a ride under 15 minutes, then steps home) before anything is designed for it.
    - *Twelve hours for an open outing, «Keep going» four more.* This is the place ADR 0016
      decision 14 keeps for a long outing's cap. An open outing has no end of its own, a day's
      hike would otherwise lose the rest of its route at four hours, and a forgotten outing is
      the stillness's to end, not the cap's.
  - **Also in the plan:**
    - The pauses of every outing are kept from 21.1 (`session_pause`, `passo.db` v9), so that
      21.3's minutes are true and an outing's page can say its time paused.
    - The open preset is offered once to readers who have plans already.
    - An open outing with no marks holds no wake lock for signals.
  - **Phase 20 updated to inherit:**
    - The card's line under way is for an open outing with GPS, and is its sentence.
    - It rides on 21.2's kilometre. Where the outing speaks, the kilometre's words carry it.
      This changes 19 Nov's «no voice», for the owner's review: no signal is added, the
      kilometre's words say one thing more.
    - A round's small map is 21.3's still route, on plain ground. This settles, for that one
      line, the reading «Every route on plain ground» left to the phase.

- **20 Nov 2026.** **The open outing's plan reviewed, and the doors** (owner, reading the plan
  above: "I accept everything you told me and agree with everything proposed"; then "check that
  the pauses are not a table that grows for ever"; then "if you see other missing buttons or
  ways in, tell me and put them in the plan; value, not noise"). §11 Phases 18, 20 and 21 are
  updated, and Phase 23 is added. «State» and CLAUDE.md give the order: 21, 23, 18, 20.
  - **Confirmed by the owner:**
    - The twelve hours of an open outing, with the question 15 minutes before and «Keep going»
      four more.
    - A short ride left inside the walk, as the ends already are: «How it ends» is no longer a
      proposal.
    - The open preset, offered once.
    - Past its goal, an outing shows its shape (21.3; explained to the owner with the case of
      30 minutes and 20 more, and accepted as explained).
    - A round's small map on plain ground.
  - **The round's voice** (owner: "we can revise the past decision; there was no idea of this
    phase then; advise what is best and gives the app value"). The comparison is spoken in the
    kilometre's words, where it takes the pace's place so they stay short. Leaving the round is
    said once, at the next kilometre. Being ahead or behind has no pulse of its own. This
    changes 19 Nov's «no voice» (§11 Phase 20, «The card, while it counts»).
  - **The pauses' table, checked** (§11 21.1, «The pauses kept»):
    - A row is a Pause the reader touched, and no pause is ever made by itself.
    - It grows with the outings kept, far slower than `session_leg` (a row every 100 m) or
      `minute_steps`: about 1,500 rows a year at two pauses an outing and two outings a day.
    - It is deleted with its outing in `SessionDao.deleteSession`, an outing not kept
      included.
    - An import that skips an outing already here (`BackupMerge`, by its start) skips its
      pauses with it.
  - **The doors' review.** Every page's touches, every notification, the widgets, the tile and
    the shortcuts were read, then each finding was checked in the code. What was found:
    - **The tile, both widgets and the counting notification** open the page the app was last
      left on, not Today as ADR 0008 decision 8 says. A launch intent brings the `singleTask`
      task back as it was, and only a shortcut's start asks for Today.
    - **«Still walking?» and «GPS stopped»** open that same last page, though what they ask for
      is on the outing's card.
    - **Outings have no list:** they are found day by day in History, and the routes' count in
      Your data names routes no page lists.
    - **An outing on a city walk** has no door to the walk.
    - **The Outings page's cities door** lands above five continents of ways.
    - **A stage's notification** opens the Ways list, not its way.
    - **The goal's notifications** open the last page, and the weekly summary does not open its
      week.
    - **Outing rows** wear no chevron, though every other door on those pages does.
    - **The card of a walk under way, on the walk's page,** has no `onGps`. That is harmless
      until Phase 18 measures a walk.
  - **Where each went:**
    - What shows an outing opens the outing's page: Phase 21.1, beside «See the outing».
    - The walk's card: Phase 18, «The start, through the one door».
    - The rest: Phase 23. Step 23.1 holds the doors that lead where they say: Today, the week,
      the way, the walk, the cities, the chevron. Step 23.2 holds «Your outings», the list,
      with its filter for routes and the way in from Your data.
    - Left out, each with its reason at the foot of Phase 23: links to the guide's chapters,
      the streak's day marks, an end notification after Stop, a page for a walk found in the
      steps, Insights' way card, the continent map's touch.
  - **Order:** 23 after 21 and before 18. The doors are small and need 21.1. The list needs
    21.3's still route, and makes the outings measured with GPS findable before Phase 20
    groups them into rounds.

- **20 Nov 2026.** **Phase 21.1 built: the open outing** (owner: "start here with 21.1 on a new
  branch; do premium work"). §11 Phase 21's step 21.1 boxes are ticked but the phone's, which opens
  in «Open». ADR 0009 is amended (decisions 1, 4, 6 and 8) and ADR 0016 (decision 14) before the
  code, as the step's first box says.
  - **Built.**
    - The model: `SessionGoalKind.OPEN`, `SessionPlan.open` and `Session.open`, `SessionPause`;
      `SessionPlans.OPEN_WALK` first of five presets, `OPEN_MAX_SESSION_MILLIS` (12 h).
    - `SessionHeadline`'s open sentences (`OpenStarting`, `OpenGoing`, `OpenEnded`), with
      `SessionText`'s words: «25 minutes on your feet», «2.30 km measured in 25 minutes», «A walk
      of 41 minutes, about 3.20 km», and their run and end forms.
    - The tracker keeps each pause at the resume; `SignalWake` holds no lock for an open outing
      but for GPS.
    - `session_pause` (`passo.db` v9), written with the outing in the batch and alone, deleted
      with it; the JSON backup's `pauses`; the CSV's `open`.
    - The open walk offered once to a reader with plans (`open_preset_seeded`, `prependPlan`).
    - The editor's «Kind» in three («One goal | None | Intervals», `EditorKind`), the goal kept
      across «None» (`keptGoal`), the open plan's note, its signals' note and its legend.
    - The card: `OpenRow` in the bar's place, «See the outing» while it counts (`SeeTheOuting`),
      and the notices and buttons drawn by the card and the page alike (`SessionNotices`,
      `SessionLiveButtons`).
    - The outing's page while it counts: the service's own state of the outing
      (`LiveSessionState`), the notices under the sentence, Stop and Pause in a bottom bar; the
      time paused in «when it was».
    - The notification: `BigTextStyle` and the chip's distance for an open outing; the counting
      notification, «Still walking?» and «GPS stopped» open the outing's page; so do both widgets
      (`widgetTapIntent`) and the tile while they show an outing; a city walk's card has «See the
      outing» (`WayRoute.onOpenOuting`).
    - The mark `PassoIcons.OpenOuting` and `ic_shortcut_open`; the voice's start «No goal, at any
      pace» and, at the reader's stop, what an open outing came to (`OpenStopped`), in hours
      beyond sixty minutes; the editor's «Try it» plays that sentence.
  - **Made exact on the way**, each in the code with its reason:
    - **The pause under way is not a row.** The plan had `toMillis` null while paused. The pause
      under way is already the outing's `pausedAtMillis`, so a row for it would keep the same
      fact twice. A row is written at the resume, and an outing that ends while paused ends where
      it was paused, outside that pause.
    - **On the page, Stop and Pause sit in a bottom bar**, not at the end of the list. The page of
      an outing with GPS is long (map, numbers, kilometres, climb), and the buttons the reader
      came for stay at hand however far down they scrolled.
    - **The page names its aim only where it has a pace to keep.** «No goal, at a brisk pace»
      tells what the numbers are read against; at any pace, saying there is no goal is noise.
    - **The voice speaks at a stop only for an open outing.** A goal outing's moment is its goal;
      stopping it says nothing, as before.
    - **The chip says the time in motion before the first 100 m**, where a distance would read as
      nothing.
    - **The introduction of the Outings page and the Settings row** said «A walk with a goal»:
      now «with a goal, or with none», in both languages.
    - **Five presets, four shortcuts** (`SessionConstants.SHORTCUTS`, unchanged). A first visit's
      long press now offers the open walk, the brisk walk, the run and the rest of the day; the
      Japanese walk joins them once it is started. Four stays as many as the common launchers
      show.
    - **A card's notices and buttons are shared**, not copied. The page of an outing that counts
      is its card made large, and the two must not drift apart.
  - **The README.** Regenerated where they changed: `outings.png` (the open walk first, the alt
    text with five outings, the caption «with a goal, or none»), `today-outing.png` (the card
    under way with «See the outing»: the picture now builds the card as the app does),
    `intervals.png` (the Kind in three), `intervals-done.png` and `settings-appearance.png`
    (looked at: the Settings row's new words). The feature lines say the open outing and the page
    while it counts. CHANGELOG, VISION, the guide in both languages («With no goal», the twelve
    hours, the page while it counts, the battery), CLAUDE.md.
  - **Checked**: `spotlessCheck`, `:app:checkForbiddenPermissions`, `test` and `:app:lintDebug`,
    as reported with the pull request. On this machine eight tests that walk a Room migration
    fail by a Windows path comparison: the seven before, and the new «version 8 migrates to 9»,
    CI's to prove.
    - New tests: `SessionTrackerOpenTest` (the step's edge cases in the tracker),
      `SessionPlansTest`, `SignalWakeTest`, `SessionAnnouncementTest`, `BackupCodecTest`,
      `CsvExportTest`, `SessionRepositoryTest` (the preset offered once, the pauses written,
      rewritten, deleted, in a batch, the migration), `BackupRepositoryTest` (an open outing
      with its pauses, imported once), `OpenOutingCardTest`, `OutingScreenTest` (the page while
      it counts, the time paused, the aim), `PlanEditorScreenTest` («None», Italian on a small
      phone, twice the text size), `SessionNotificationsTest` (the open notification and its
      doors), `SpokenTextTest`, `SessionShortcutsTest`, `WidgetGalleryTest` (the open line and
      the widget's door).
    - The screenshots looked at.

- **20 Nov 2026.** **Phase 21.2 built: the marks on the way** (owner: "implement the next step,
  21.2; carry on on PR 76, which is part of the same phase; premium work"). §11 Phase 21's step
  21.2 boxes are ticked but the phone's, which opens in «Open». ADR 0009 decision 3 is amended
  before the code, as the step's first box says (decision 1's pointer to it with it).
  - **Built.**
    - The model: `SessionMarks` (`DISTANCE`, `TIME`, `NONE`); a plan's choice, `SessionPlan.marks`,
      null until the reader makes one, and `marksInForce`, which follows the plan's GPS switch
      until then; an outing's `marks`, `markMiles` and `toldMarks`, frozen at the start of an open
      outing by `SessionPlans.start` (with `miles`, the reader's units then), none for every
      other outing.
    - `OutingMarks` (`:core:domain`): how many marks an outing has crossed, the time in motion to
      the last one and that stretch's pace, from its legs.
    - The tracker: `SessionSignal.Mark(count)`, told in `advance` as a milestone is, so a kilometre
      with GPS is told on settled metres; several in a batch told once, as the last; none after
      the end, nor while the end can be taken back.
    - `SignalWake`: an open outing with marks, and the vibration or the voice, has signals to keep
      on time; one with none keeps the lock for GPS alone, as in 21.1.
    - The words (`SessionAnnouncement.DistanceMark`, `TimeMark`; `SessionSpeech`): «2 kilometres,
      in 23 minutes. Pace: 11 minutes 20 seconds a kilometre.», «About 1 kilometre, in 11
      minutes.», «20 minutes, 2,300 steps.», in Italian with their plurals.
    - The service: `tellMark`, one short pulse (`SessionHaptics.playMark`, 180 ms), the words, and
      the notification posted again; the unit frozen at `startSession`.
    - `passo.db` v10 by auto-migration (`marks` on `session_plan`; `marks`, `markMiles`,
      `toldMarks` on `session`), and the JSON backup's `marks`, `markMiles`, `toldMarks`.
    - The editor: under «None», the group «Signals» with «Each km | Every 10 min | None» («A ogni
      km | Ogni 10 min | Nessuno»; «Each mile» in miles), the note of the one chosen, the legend's
      row to feel the pulse, and the voice's «Try it» saying the first mark with the reader's own
      step at the plan's pace. The Outings page: «A signal every 10 minutes», «A signal at each
      kilometre» under an open plan.
  - **Made exact on the way**, each in the code with its reason:
    - **The numbers are said as numbers.** «One kilometre» and «Ten minutes» in the plan are what
      the engine says of «1 kilometre» and «10 minutes»: every spoken amount is written so, with
      its plural. A walk of hours is said in hours («1 hour 21 minutes»), as 21.1's end is.
    - **The pace in Italian is «Andatura», not «ritmo».** «Ritmo» is the cadence in the app («a
      ritmo libero»), and every measured pace already says «Andatura: 11 minuti e 20 secondi al
      chilometro», as the milestones and the goal do.
    - **The voice follows the outings' channel**, as the vibrations do. The edge-case table said
      «the voice as the reader set it» for a silenced channel; ADR 0010 makes every spoken signal
      follow the channel, and the milestones do. A mark is no different.
    - **The segments' labels are short** («Each km», «Every 10 min»): a segment keeps its label on
      one line (`SegmentLabel`), and a third of a narrow phone does not hold «At each kilometre».
      The long words are the legend's row, the Outings page's line and the voice's.
    - **A kilometre's time is its legs'** where it has them: with GPS it is told when its metres
      are settled, by which time the reader may have walked on a little; without GPS it is the
      outing's own time, which the batch that crossed it brought.
    - **Taken back, the marks crossed aside are passed in silence**: `toldMarks` moves on with them
      at «Resume» or «Keep going», and the next is told as it comes. A pulse under the reader's own
      touch would tell them nothing.
    - **A kilometre settled by a pause is told at the pause**, as a milestone settled there is (the
      cut of the line); none is told at the end, whose last metres are absorbed in silence.
    - **A goal outing never has marks**, whatever its plan keeps: the choice is read only for an
      open plan, and a plan switched back to «One goal» keeps it for the next «None».
    - **21.1's row «A phone with no wake-up counter, open, no GPS» now holds for an open outing
      with no marks.** The open walk tells ten minutes unless the reader says otherwise, so it keeps
      the lock as a goal outing with signals does, which is what step 21.2's «The wake» asks; 21.1's
      test names an outing with none.
    - **`UpgradeFromFirstReleaseTest` reaches schema 10.** It still checked schema 8, which 21.1
      had not moved on.
  - **The README.** `outings.png` and `intervals-done.png` regenerated (the open walk's card now
    says «A signal every 10 minutes») and looked at; the outings' feature line names the pulse. CHANGELOG, VISION, the
    guide's «With no goal» and the battery paragraph in both languages, CLAUDE.md.
  - **Checked**: `spotlessCheck`, `:app:checkForbiddenPermissions`, `test` and `:app:lintDebug`,
    as reported with the pull request. On this machine nine tests that walk a Room migration fail
    by a Windows path comparison: the eight before, and the new «version 9 migrates to 10», CI's to
    prove.
    - New tests: `SessionTrackerMarksTest` (every row of the step's edge cases, and the marks'
      own: in order, settled at a pause, none at the end, an outing from before), `SignalWakeTest`,
      `SessionAnnouncementTest` (measured, estimated, the time), `BackupCodecTest` (the marks
      carried, a file from before, an unknown value), `SessionRepositoryTest` (a plan's choice, an
      outing's mark read back, the migration), `BackupRepositoryTest`, `SpokenTextTest` (English
      and Italian, kilometres and miles, plurals), `SessionNotificationsTest` (the pulse),
      `PlanEditorScreenTest` (the group following the switch, a choice kept, «Try it», none, miles,
      the awake note, Italian on a small phone, twice the text size), `SessionsScreenTest`.
    - The screenshots looked at, in both themes.

- **20 Nov 2026.** **Phase 21.3 built: the shape on the card** (owner: "implement the next step,
  21.3; carry on on PR 76, which is part of the same phase; premium work"). §11 Phase 21's step
  21.3 boxes are ticked but the phone's, which opens in «Open». With it Phase 21 is built.
  - **Built.**
    - `OutingMinutes` (`:core:domain`, pure): an outing's minutes from `minute_steps`, over its
      span, less its pauses, the one under way included; grouped by 1, 2, 5, 10 or 15 minutes so a
      bar stays two dp wide with one between (`groupFor`, `bars`); a bar's height its mean steps a
      minute, «at the pace» when half its minutes are at the floor (the outing's own, or brisk,
      100, at any pace), a gap when mostly paused; the goal's minute (`goalAt`), and the count of
      minutes at the pace and of pauses for TalkBack's sentence.
    - `RouteTrace` (`:core:domain`): a route's lines projected into a box's pixels and thinned to
      them in one pass, a line's ends kept and its dashes where they were.
    - `OutingShape`: the minutes, the route so far and the day's goal, and the share
      (`dayShare`); `pastGoal` and `beyond()` beside `progress()`.
    - `MinuteStrip` (Canvas): the bars in the accent at the pace and the quiet ink below it, a stub
      for a minute walked with no steps, nothing for a pause, the goal's line on an edge of the
      card's colour; it grows to the right (a sweep at first, a slide at each minute), and jumps
      under reduced motion.
    - `StillRouteView`: the route as the map draws it, on plain ground, with no gesture, bead,
      scale or compass; thinned once for each size and route; one node for TalkBack, and a door
      where it is given one. Phase 20's list of rounds reuses it.
    - The card: for an open outing the shape in the bar's place (the strip until the route has a
      line, then the route with a fade), then 21.1's row, then the day's share after a small ring
      of the goal's colour (`DayShareLine`); for a goal outing taken on past its goal, the shape in
      place of the full bar, and the sentence «Goal reached, and 20 minutes more».
    - `OutingShapes` (`:core:data`): one source for the card on Today, on the Outings page and the
      outing's page; each part read only while collected, the route again only when its point
      count changes and off the main thread, the minutes with the service's live count, the day's
      goal as Today shows it. Today hands it the query its day's chart reads.
    - The outing's page: «The shape of the walk» («The shape of the run» for a run), after the map
      or where the map would be, with what a bar is, what the gaps and the line are, and the share.
    - `passo.db` v11: `pausesKept` on `session`, and the JSON backup's `pausesKept`.
  - **Made exact on the way**, each in the code with its reason:
    - **Whether an outing's pauses are known is kept, not guessed** (`pausesKept`, v11). The plan
      said the section is for «every outing walked since 21.1», but nothing in an outing told that:
      `pauses` is empty for one never paused and for one from before. The migration's spec
      (`KeptPauses`) marks known the outings that could only come from a Passo that kept them, an
      open one and one with a pause written, and so does a backup file from before the field. The
      goal outings walked on the unreleased 21.1 and 21.2 builds without a pause stay unknown: a
      test phone's, and a page without a shape is honest where one with a pause drawn as walking
      is not.
    - **The beyond is said in the goal's own quantity.** «and 20 minutes more» past a time goal;
      past a goal of steps or of distance, «and 1,240 steps more», «and 0.80 km more»: the time in
      motion at the goal is not kept, and the bar's two ends spoke the goal's quantity too. Never
      for an outing that ended at its goal, whose last batch may run a few steps past.
    - **The bars are the primary colour on every card, the goal's line the goal's.** Past a goal
      the card wears the goal's colour; bars in it would hide the line that marks the goal.
    - **The strip keeps room while it counts**: half an hour wide at least, filled from the left,
      so the first minutes are not drawn as pillars; ten minutes once over.
    - **An open outing's card keeps its shape once over**, with its share and without 21.1's row,
      whose «since when» the state's line already says. A goal outing's finished card keeps its
      bar: what it came to against its goal.
    - **A city walk's card keeps its walk's map**, past its end too: the walk's places say more of
      it than its minutes. So a city walk's page reads no shape, and its ViewModel is unchanged.
    - **The share in Italian takes its article**: «Il 34%», and «L’8%», «L’11%», «L’80%» where the
      number is said with a vowel (`session_share_*_elided`; English says them alike). An outing
      whose day is not today says «of the day's goal», «dell’obiettivo del giorno». A walk of a
      few steps is never «0%».
    - **The legend says the floor in steps a minute** («A bar for each minute, in colour at 100
      steps a minute or more»), and how many minutes a bar holds at the strip's width, rather than
      a pace's name for each intensity.
  - **The twelve-hour walk, measured.** On this machine's JVM: 43,200 points decoded in about 6 ms,
    shaped in 7 ms, thinned to some 6,300 in 4 ms for a 990 by 618 px box (`RouteTraceTest`, which
    logs it); twelve hours of minutes read and grouped in 12 ms (`OutingMinutesTest`). The phone's
    measure is the device box's.
  - **The README.** A new picture, `outing-open.png` (an open walk with GPS on Today, halfway round
    a park: its route so far, since when, 30% of today's goal), beside `outing-route.png`, with its
    caption; the outings' feature line says the shape. The other pictures show nothing that
    changed. CHANGELOG, VISION, the guide's «With no goal», the end and the page in both languages,
    CLAUDE.md.
  - **Checked**: `spotlessCheck`, `:app:checkForbiddenPermissions`, `test` and `:app:lintDebug`,
    as reported with the pull request. On this machine ten tests that walk a Room migration fail by
    a Windows path comparison: the nine before, and the new «version 10 migrates to 11», CI's to
    prove.
    - New tests: `OutingMinutesTest`, `RouteTraceTest`, `PastGoalTest`, `SessionPlansTest` (every
      outing started keeps its pauses), `BackupCodecTest` (the field carried, a file from before),
      `SessionRepositoryTest` (read back, the migration), `OutingShapeCardTest` (no fix then the
      route, without GPS, a touch on the route, GPS stopped, the share in each case, past a goal,
      before it, an outing from before, Italian on a small phone, twice the text size, the
      foldable), `OutingScreenTest` (the section, after the map, none for an outing from before, a
      run's title, Italian).
    - The screenshots looked at, in both themes.

- **21 Nov 2026.** **Phase 23.1 built: the doors that lead where they say** (owner: "implement
  Phase 23.1; premium work. I would like the whole of Phase 23 before 20, which I need more:
  check the order is fine, otherwise stop and tell me why"; then a review of the code, push and
  the pull request). §11 Phase 23's step 23.1 boxes are ticked but the phone's, which opens in
  «Open». ADR 0008 (decision 8) and ADR 0005 (decision 8) are amended.
  - **The order, checked.** The whole of 23, then 20, holds; 18 comes after 20, at the owner's
    wish. Nothing in 20 needs 18 (a city walk measured with GPS), nor 18 anything in 20. 23.2
    before 20 helps it: «Your outings» is the first list of outings with their still route read
    lazily from `routes.db`, which 20's «Your rounds» then reuses, and 20's door stands beside
    23.2's on the Outings page. One caution, not a block: 20 matches measured routes, and Phase
    15's proof on a phone (the box in «Open») is still to be done; the rounds are only as good as
    the routes, so that proof is best walked before 20's own device check.
  - **Built.**
    - `AppDoors` (`:core:tracking`): the extras a touch from outside puts on the launch intent,
      `EXTRA_OPEN_TODAY`, `EXTRA_OPEN_WEEK`, `EXTRA_OPEN_WAY` with `EXTRA_OPEN_JOURNEY`, beside
      21.1's `EXTRA_OPEN_OUTING`; `openingToday()`; `DoorRequest`, one request code per door.
    - The counting notification, the tile (`tileTapIntent`, pure of the service) and both widgets
      (`widgetTapIntent`) ask for Today, or for the outing they show; paused, for Today and the
      resume together.
    - Goal reached and the evening reminder: Today. The weekly summary: its week
      (`EXTRA_OPEN_WEEK`, the week's first day). A stage reached: its way and its journey.
    - `AppDoor` (`:app`): the page an intent asks for, read once (its extras taken off), the most
      particular first; `PassoNavigationState.open(door)` and `show(tab)`; one effect in
      `PassoRoot` in place of three flags (`openWays`, `openToday`, `openOuting`).
    - `WaysKey(cities)`: the Ways page placed at its cities from its first frame
      (`citiesHeaderIndex`), from the Outings page's «Walk through a city» (`openCities`).
    - The outing's page: «On the walk», the walk's small map with where the outing left it, its
      route and how far along it came («2.10 km of 9.32 km», or «The whole walk»), a door to the
      walk as it stands (`WayKey(walk, null)`). The card's small map of a city walk is a door to
      the walk too (`SessionCardActions.onOpenWalk`), on Today and the Outings page; «See the
      outing» is unchanged.
    - `OutingList`: an outing's row that opens its page wears the chevron.
  - **Made exact on the way**, each in the code with its reason:
    - **A door opens over Today.** The plan said «Today» for the day's surfaces and «the
      outing's page» during an outing. 21.1 pushed the outing's page on whichever page the app
      was left: back from it then landed on a Way from yesterday. Every door now clears Today's
      stack first, so back from the outing's page, or from a way's, lands on Today, where the
      card that follows them is. The week opens History's own page, with Today beneath it.
    - **One request code per door.** A stage's notification (16) and an outing's (16) shared one:
      Android keeps one `PendingIntent` per intent and code whatever its extras, and
      `FLAG_UPDATE_CURRENT` gave the later one's extras to both, so an outing's notification in the
      shade opened the Ways once a stage was told. `DoorRequest` is an enum: no two can share one.
      A test posts the two together and fails if they do.
    - **An intent from Recents is not a touch.** Opened again from Recents after the reader left
      by back, Android hands the activity its task's first intent, extras and all
      (`FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`): a shortcut's outing would have started a second
      time, a notification's page opened again. It is read as no touch (`isFreshTouch`), the
      resume and the shortcut's start included.
    - **The stage opens its journey**, not the way alone: once the last stage finishes the way,
      the way's page without a journey shows it ready to start again, and the stamps the
      notification speaks of would be gone from it.
    - **The walk's row says how far the outing came**, not the walk as it stands now: that is the
      walk's page's to say, and the outing's page is about the outing. Its label is «On the walk»
      («Sulla passeggiata»), the route under it, as the walk's row on the Ways page is drawn.
    - **The cities are placed, not scrolled to**: the page arrives where it was asked, with nothing
      passing under the reader's eyes; where what is below the cities is shorter than the screen,
      the list ends at its foot and the edge of the ways above shows, a sign they are there.
    - **The day's list keeps one column for the lengths.** With the chevron at an outing's end, a
      walk's «17 minutes» stood further right than the outing's «18 minutes»: where the list has a
      door, a walk's row keeps the chevron's room free.
    - **Passo's words before the location's request stay.** A door clears what was over Today,
      and the screen that comes before Android's request (`GpsIntroKey`) waits there with an
      outing waiting on its answer: cleared by the counting notification touched from the shade,
      it would have left the outing unstarted. A door keeps it over the page it opens.
    - **A paused card's touch is its resume, as the tile's**: the widget's tap asked for the
      outing's page too when a card showed one, which a paused count cannot have; it now asks for
      Today and the resume alone, as the tile always did.
    - **The old `WayIntents.EXTRA_OPEN_WAYS` is gone**: a stage notification left in the shade by
      the build before opens the app where it was, nothing worse.
  - **The README.** No picture shows what changed (the day's lists and a city walk's card are in
    none of them), so none is regenerated; 23.1 adds none, its doors are touches, not views.
    CHANGELOG, ADR 0008 and ADR 0005, §8 above, CLAUDE.md.
  - **Checked**: `spotlessCheck`, `:app:checkForbiddenPermissions`, `test` and `:app:lintDebug`,
    as reported with the pull request.
    - New tests: `AppDoorsTest` (each surface's intent and its extras; two doors in the shade at
      once), `AppDoorTest` (the extras read once, an unknown way, Recents, each door's pages and
      back, Passo's words before the location's request kept), `WidgetGalleryTest` (Today by name, paused), `OutingScreenTest` (the walk's row, the
      whole walk, none, Italian on a small phone, twice the text size), `WaysScreenTest` (at the
      cities, with and without a way under way), `TodayScreenTest` (the walk's map a door, an
      outing's row), `HistoryScreenTest` (the row's role and label).
    - The screenshots looked at.

### Open

- [ ] **On a device (owner): the shortcuts after an update** (archive 04, 15 Nov 2026, «The shortcuts
  written again at every start of the service»): install the build over the old one, then a long
  press on the icon: four outings, the Japanese walk with its slow and fast line, a run with its
  bolt, the other walks with the flag, without starting or saving a plan first. On the tablet the
  marks are right (owner, 15 Nov 2026); the fourth shortcut and the phone remain.

- [ ] **On a device (owner): Today's premium review** (archive 04, «Today reviewed for a premium first
  screen»): the ring, the row of three, the streak by the date, the chart and the Intensity card,
  in the light and the dark, at a large text size and with TalkBack.

- [ ] **On a device (owner): History's review** (archive 04, «History reviewed, after Today and the
  Outings»): the four scales in Italian, a bar's reading and «Open», the tiles' quiet marks (in
  Insights too), a day's walks on two lines, in the light and the dark, at a large text size and
  with TalkBack.

- [ ] **On a device (owner): Insights' and the year's review** (archive 04, «Insights and Your year on
  foot reviewed, after Today, the Outings and History»): Insights with and without a streak, a way
  under way, the averages' strip and the tiles in Italian, in the light and the dark and at a
  large text size; Your year on foot page by page, a page shared and one saved (the image without
  the hint, the snackbar); a way kept on the Ways page; TalkBack on the averages and on a page.
