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
  location for an outing that asks for it (`docs/adr/0016-location.md`; §11, below; §15, from
  «Location, for an outing that asks for it» on). Built, all of it but what a phone must
  answer: the allowlist, the manifest, the receiver, the fusion, the routes' database, the
  backup's fields, the cap's question, the card's and the notification's lines, the
  documents. Left: the proof on a phone and the measure of what it costs (the boxes in
  «Open»), which come before any release.
- **Built, on a branch of its own from Phase 15's** (18 Nov 2026): Phase 16, the outing's
  page (the route on a map that moves, the kilometres, the climb, GPX and image, Your data;
  §15, «Phase 16, the outing's page»). Left: its check on a phone (the box in «Open»). 15 and
  16 are released together or not at all. Phases 17 to 20: not started.

## Open

What is left to do or decide, in one place: a box opens here, naming the entry it comes from,
and closes here, naming the one that closes it. Phase 12's own boxes stay in its section (§11).

- [ ] **Baseline Profiles; R8 full mode; startup check** (archive 01, «Phase 7»).
- [ ] **The accessibility scanner reports no critical issues** (archive 01, «Phase 7»).
- [x] **The full install and update path**: install the last release, then update to the next
  over it, with the data kept (archive 01, «Phase 8»). Done by the owner on 2 Nov 2026, 1.0.0 to
  1.1.0, before the tag («I did the tests and everything works»); the database's road from schema
  3 to 6 is also tested (`UpgradeFromFirstReleaseTest`).
- [x] **On a device (owner): the new icon** (§15, «The new icon: two prints on the green», 9 Nov
  2026): the launcher icon in the phone's own mask, the themed icon, the status-bar icon and the
  Quick Settings tile, the Outing shortcut and the walker on an outing's bar. Done by the owner on
  9 Nov 2026 (§15, «The new icon checked on the phone»).
- [x] **On a device (owner): the Outing shortcut as an adaptive icon** (§15, «The Outing
  shortcut, an adaptive icon», 9 Nov 2026): long press on the icon, in the phone's own mask and
  with the themed icons on. Done by the owner on 9 Nov 2026 (§15, «The adaptive shortcut checked
  on the phone, and Tetto in the family»).
- [x] **On a device (owner): each outing's shortcut wears its own mark** (§15, «Each outing's
  shortcut wears its own mark», 9 Nov 2026): after starting or saving a plan, a long press on the
  icon shows the interval walk with its slow and fast line, a run with its bolt. Done by the owner
  on 9 Nov 2026 (§15, «The outings' own marks checked on the phone»).
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

- [ ] **On a device (owner): the outings, the editor and the maps reviewed** (§15, «The outings,
  their editor and the city walks' maps, reviewed», 11 Nov 2026): the Outings page and an outing
  under way in both themes and at a large text size; the editor's legend, each signal felt; a
  city walk's map before a start and under way, a way's map; TalkBack on a card and on the legend.

- [ ] **On a device (owner): Phase 15's proof, before the rest is built** (§11 Phase 15, «On a
  device (owner), the proof first»; §15, «Phase 15, the slice for the proof on the phone», 18 Nov
  2026). With the debug build of `feature/phase-15-measured-outing`: an outing with «Measure
  with GPS» started from the Outings page and from the launcher's shortcut; the screen off for
  ten minutes; pause and Resume from the notification; the app opened and closed meanwhile; a
  stop of five minutes; "Walk now" from the evening reminder with GPS on that plan; the
  permission as «Only this time»; the process ended mid-outing and the app opened again. Each
  outing's `GPS` row in the log says whether the type was added, and whether fixes came with
  the screen off, in batches or one by one.
  With the card built (§15, «Phase 15, what the reader sees, and the documents»), also: the
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

- [ ] **On a device (owner): Phase 16's page** (§11 Phase 16; §15, «Phase 16, the outing's
  page», 18 Nov 2026): a walk with a hill and a tunnel or an arcade, then its page: the line
  where it was walked, the dashed stretch, the climb against a known figure, each kilometre's
  pace; the map pinched, dragged and brought back whole, a bead touched, the page scrolled
  with a finger that starts on the map; the GPX opened in a map app; the image shared to two
  apps; «With the routes» in a backup, imported on the other phone; «Delete this route» and
  «Delete all routes»; the page at a large text size, in both themes, on the open foldable,
  with TalkBack. On a phone with no barometer, if one is at hand: the climb said as rough.

Read again on 2 Nov 2026, at archive 01 and 02: the boxes above are the ones the phases done had
left open, and the review's device check. Read again at 1.1.0's tag: the owner's checks of the
update, the outing let go, Your year on foot and the new ways' maps are done; §15 has no open
question.

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

- Profile: height, weight, sex (optional), step length mode (auto, manual or calibrated), walking step length, running step length.
- Goal, units, first day of week, theme, dynamic color, notification opt-ins, reminder time and threshold, tracking enabled.
- The last day whose goal was seen reached (not a setting: the record that keeps "goal reached" once a day).
- Walk detection enabled (default on), minimum walk duration (5, 10 or 15 min; default 10), typical-day line shown (default on), Today's "Start an outing" button shown (default on).
- Whether the outing presets were written once (so deleting them is final), and the last finished outing whose card Today has put away.

---

## 6. Derived metrics (`core:domain`)

All constants live in one file (`MetricsConstants.kt`) with comments that cite their sources. All defaults are to be validated.

- **Default walking step length:** `height × 0.415` (`× 0.413` when the profile says female). If height is unknown, 0.70 m.
- **Running step length:** default `walking × 1.3`, user-editable. It applies to minutes with a cadence of at least `RUNNING_CADENCE` (140 spm).
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
- **Channel `ways`** (default importance, Phase 11): a stage reached on the way under way, and its end. Told by the days being written (`WayNotifier`), never by a timer; touching it opens the Ways page.
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

Asked for and approved by the owner on 18 Nov 2026 (§15, «Location, for an outing that asks for
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
| 18 | On the spot: the cities («Sul posto») | A city walk walked there: real arrivals, off the route, back on it | 15, 16 |
| 19 | On the spot: the ways | A way walked in person: a fine line, stamps in a second ink, the stretches walked there | 18 |
| 20 | Your routes («I tuoi percorsi») | Follow a route (a GPX, a past outing), the usual round | 16, 18 |

**One phase, one session.** Each section is written to be enough for a session that has read
CLAUDE.md, ADR 0016 and that section: it names the files to read first, the steps in order, the
acceptance, the edge cases (each a test) and what only the owner's phone can check. A session
starts in plan mode on its section, builds on a branch, and ends by ticking its boxes here,
writing its entry at the end of §15's log (what was built, what was decided on the way, what was
checked) and opening its device box in «Open». A decision that departs from ADR 0016 is written
there as an amendment, with its date and reason, before the code that needs it.

**Releases.** Phases 15 and 16 are released together or not at all (ADR 0016, consequences);
17 to 20 may each follow in a release of their own, in this order unless the owner changes it.

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
  permission's flow, ADR 0016 is amended first. Read on 18 Nov 2026 (§15, «The five answered,
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
  the backup's round trip with the samples, and an older file without them. Built (§15,
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
  to test for now: "can you go on safely, and in keeping with the plan?"; §15, «Phase 15, the
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
| A city walk from home | Never with GPS (ADR 0016 decision 13, made plain): its outing has `gps` false, and no switch offers it; on the spot is Phase 18 |
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
  the reader's units. Where at least four fifths of the route lies inside the frame of a city
  Passo carries, that city's map is the ground (water, parks, streets, as on the walk's page);
  elsewhere the ground is plain, and the page says in a line that Passo carries no street map.
  A touch names the kilometre and its pace. One sentence to TalkBack («Your route: 5.2 km,
  from north to south, 3 stops»); the splits below are its words. Its gestures are a piece of
  their own, since Phases 18 and 19 reuse them. Drawn in every theme and dress in a gallery
  test, as the ways' maps are.
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
  with and without routes. Built (§15, «Phase 16, the outing's page»): the climb's tests are
  in `AltitudeTest` and `RouteHeightsTest`, the map's gallery is `RouteMapGalleryTest`, the
  page's forms `OutingScreenTest`.
- [x] Strings in English and Italian; CHANGELOG; the guide (the outing's page in the outings
  chapter); README: one screenshot, `outing-route.png`, the page of an outing with GPS inside
  a city Passo carries, with its caption, and the feature list's line.
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
| A route half inside a carried city | The plain ground: under four fifths, a city's map would end mid-route |
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

- [ ] **The learner** (`:core:domain/calibration`, `StepLengthLearner`, pure). From the samples:
  bins of 10 steps a minute from 80 to 180; a bin counts with 400 m or more from at least two
  outings; its length is the median of its samples' (metres over steps), within
  `ProfileLimits`; the curve is made non-decreasing with the cadence (a step does not shorten
  as the pace rises: pooled where the data says otherwise); between two bins it is a straight
  line, beyond the ends flat. It also says what the curve would change: the last four weeks'
  distance with it against without.
- [ ] **The profile.** `StepLengthMode.LEARNED` and the curve (cadence and length pairs) in
  `Profile` and DataStore; `StepLengths.forCadence` reads the curve where it has bins and the
  walking and running lengths elsewhere, so every caller (the day, an outing without GPS, the
  ways from home) follows with no change of its own. Applied only when the reader accepts it,
  through the one door a profile change has, which freezes the past first; «Apply profile to
  past data» works with it as with any profile. The backup's `ProfileDto` gains the curve as
  an added field; a file with a mode this version does not know falls back to the height's
  estimate and says so in the import's summary (check how `BackupCodec` reads a mode today,
  and how 1.1.0 would read `LEARNED`: if it fails, the mode is written as `CALIBRATED` with
  the curve beside it).
- [ ] **The proposal** (`:feature:settings`). When the bins around the reader's usual walking
  cadence (the median of their active minutes over four weeks) count, and the curve would move
  the last four weeks' distance by 2% or more: a card at the step length's rows and on the
  calibration page, sentence first («Passo has measured your step on 14 km of your outings:
  at your usual pace it is 79 cm, not 74, so distances read 6% longer»), the curve drawn in
  Canvas (the bins as points, the line through them, the height's estimate as a dashed line,
  cadence across and centimetres up, one sentence for TalkBack), «Use it» and «Not now». Never
  a notification. After «Use it», the row says where the length comes from and from how many
  kilometres, with «Go back to the estimate from your height». It is proposed again only when
  new outings move it by another 2%. The finished page of an outing with GPS gains one quiet
  line, «Your step at this pace: 79 cm».
- [ ] The wizard stays: a reader without GPS measures their step on a known distance as today.
  A length measured by the wizard and a learnt curve do not fight: the last one the reader
  chose is the one in use, and the row says which.
- [ ] **Tests.** `StepLengthLearnerTest`, `StepLengthsTest` with a curve, the repository's
  (accepting freezes the past first), the backup's round trip and an older file, the
  settings' UI tests with the card in each state.
- [ ] Strings in English and Italian; CHANGELOG; the guide («Measure your step» gains the part
  on outings with GPS); README: `calibration.png` regenerated if its screen changed, and its
  caption (today it says "no GPS").
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
| Samples only at a brisk cadence | A curve of one bin: that length around it, the profile's own elsewhere |
| A bin whose median is shorter than a slower bin's | Pooled: the curve never goes down |
| A sample outside `ProfileLimits` (a bad stretch) | Left out |
| The running bins empty | Running keeps its own length, or the factor, as today |
| The curve accepted, then the height changed | The curve stays in use: it was measured, the height is only an estimate |
| «Go back to the estimate» | The mode returns to the height's; the samples stay, and a new proposal waits for a new 2% |
| An outing deleted | Its samples go with it; the curve in use is not recomputed behind the reader's back |

### Phase 18 — On the spot: the cities («Sul posto»)

A city walk is imaginary and says so. A reader who is in the city can now walk it for real:
«I'm here» / «Sono qui» binds the outing to the route itself, so a place is told when the
reader reaches it, the point on the map is theirs, and a vibration of its own says the route
was left and another that it was found again (ADR 0016 decision 13).

**Read first:** ADR 0016 (decisions 4, 5 and 13), `docs/adr/0015-city-walks.md` (all),
`docs/adr/0010-voice.md`. In the code: `SessionTracker.kt` (`placesPassed`), `WalkPlaces.kt`,
`WalkDays`, `WayProgress.kt`, `Geo.kt` (`WayLine`), `WayProjection.kt`, `Way.kt`;
`WayRepository.kt`, `WayDao.kt`; `SessionControl.kt` (`startWalk`), `SessionHaptics.kt`,
`SessionSpeech.kt`, `SessionAnnouncement.kt`; `WalkScreen.kt`, `WaysViewModel.kt`,
`WayMap.kt`, `WayStamp.kt`, `SessionCard.kt` (the walk's small map); Phase 16's `RouteMapView`
and its gestures; `tools/build_ways.py` (`LINE_TOLERANCE`) and `tools/walks/`.

- [ ] **The line is fine enough, proven.** A city's line is simplified to a 4,000th of its
  frame's diagonal, a metre or two: a data test measures, for every walk, the largest distance
  between the committed route (`tools/walks/*.geojson`) and the line in `WayData.kt`, and fails
  over 5 m. No data changes if it passes.
- [ ] **`LineSnap`** (`:core:domain/routes`, pure): the nearest point of a `WayLine` to a fix,
  as metres along and metres aside; searched near the last place first, so a route that
  crosses itself or comes back along a street does not jump; the whole line at the start and
  after a long gap. Progress along the line only moves forward.
- [ ] **The outing on the spot.** `Session.onTheSpot` and `Session.walkAtMeters` (the furthest
  point reached), stored with each batch. A place is told when the position passes it within
  the corridor, or comes within 30 m of the place itself; the goal is the last place reached,
  not a distance covered. Off the route: more than 40 m aside for three accepted fixes and
  15 s, told once; back within 25 m, told once; while off, the progress waits and the outing's
  own distance still counts. With no fix (an arcade), the position goes on along the line by
  the steps until the fixes return. Never told twice across a restart: the progress is read
  from what was stored.
- [ ] **Two vibrations of their own**, unlike the seven there are (one, two, three short; the
  goal's long one; the stillness's two long; "faster", four quick taps; "slower", a long and a
  short): proposed, "off the route" short, long, short (120, 450, 120 ms) and "back on it" two
  quick taps (70 ms, 80 ms apart); the owner confirms them in the editor's "Try them" on the
  phone. With the voice: «You have left the route», «Back on the route», and each place as
  today.
- [ ] **The start** (`WalkScreen`). «I'm here» beside Start, shown only where GPS can be used
  (Phase 15's readiness), with one line saying what changes. The first fix decides: within the
  corridor of the route, the walk begins at that point of it («You are by San Babila: the walk
  goes on from here»); within 2 km, it waits for the route, the card saying how far the
  nearest place is; farther, the outing says the walk is not near and goes on as from home, by
  the steps, with nothing lost. No way to the start is drawn or spoken: Passo has no streets
  to route on.
- [ ] **The map under way.** The walk's page and the outing's card show the reader's own point
  with its accuracy as a halo, the trace walked over the route, the next place named; the page's
  map moves with Phase 16's gestures and can follow the point. One sentence to TalkBack, kept
  true as the walk goes («On the route, 300 m to the Duomo»).
- [ ] **The record.** A walk finished on the spot is stamped in a second ink, «walked there»
  with its day (`WayStamp`; an ink that holds its contrast in both dresses, in `ContrastTest`);
  a walk's progress (`WalkDays`) reads `walkAtMeters` for an outing on the spot and the
  distance for one from home, so a walk may be continued either way. Schema by
  auto-migration; the backup's added fields.
- [ ] **Tests.** `LineSnapTest`, the tracker's on the spot (routes replayed from
  `tools/walks/`, with noise, a detour, a gap), the data test above, the walk page's UI tests in
  each state, the stamps' gallery.
- [ ] Strings in English and Italian; CHANGELOG; the guide's chapter on the cities; README: a
  screenshot of a walk on the spot if it reads well (`walk-on-the-spot.png`).
- [ ] On a device (owner): the Milan walk on the spot, or part of it: each place told at the
  place, a street left on purpose and found again, an arcade or a gallery crossed, the start
  from the middle of the route, the voice through headphones with the screen off; the battery
  against Phase 15's figure.

**Acceptance:** on the route, every place is told within 30 m of it and none twice; leaving the
route is felt within about 20 s and finding it again as soon; away from the city the same walk
still works from home exactly as before.

**Edge cases** (`LineSnapTest`, `SessionTrackerTest`):

| Case | Expected behavior |
|---|---|
| A route that returns along the same street | The position does not jump to the later stretch: the search stays near the last place |
| A shortcut that skips a place | The place is not told; the next one is, when reached; the walk can still be finished |
| A square crossed 50 m from the line | Told as off the route only after 15 s there; back on it at the far side |
| Fixes lost for two minutes under an arcade | The position goes on by the steps; no "off the route" from silence |
| Started 5 km from the city | Goes on as from home, said once; the flag is cleared in what is stored |
| Paused in a café for forty minutes | As every outing: a pause, or the end by stillness with "Resume"; the next outing continues from the place reached |
| The reader walks the route backwards | Off the route is not said (they are on it); no place is told; the card says which way the walk goes |
| The process restarted | Places already told stay told; the position is found again from the first fix |

### Phase 19 — On the spot: the ways

A way is walked from home over months. A reader who goes there walks it for real, a stage or a
week at a time, perhaps over years: the outing follows a line fine enough to follow on a path,
each stage reached on foot is stamped in the second ink, and the stretches walked in person are
kept and drawn, beside and independent of the journey from home (ADR 0016 decisions 13 and 14).

**Read first:** ADR 0016 (decisions 13 and 14), `docs/adr/0014-ways.md` (all), Phase 18's
section and its code (`LineSnap`, the outing on the spot, the two vibrations),
`tools/CITY_WALKS.md`. In the code: `tools/build_ways.py` (how a way's path is built, the
cache in `tools/ways-cache/`, `LINE_TOLERANCE`, `splice`), `tools/ways_content.py`; `WayData.kt`
(generated, never edited), `Way.kt`, `Polyline.kt`, `WaysDataTest.kt`; `WayScreen.kt`,
`WayMap.kt`, `WayStamp.kt`; `SessionConstants.kt` (`MAX_SESSION_MILLIS`), `SignalWake.kt`,
`WayNotifier.kt`.

- [ ] **A fine line for each way** (the script, then its output). A way's line is simplified to
  a 4,000th of its frame's diagonal, about 250 m on the longest: right for a map of a country,
  useless on a path. `build_ways.py` writes, beside it, the same path simplified to 5 m, with
  the same metres along it, as generated data of its own that is decoded only when a way is
  walked on the spot (a generated Kotlin file in chunks, or a resource of `:core:domain`,
  whichever keeps the build and the first decode fast: measure both). The two lines must come
  from the same fetched path: if `tools/ways-cache/` no longer holds the relations the ways
  were built from, a fetch moves the lines with OpenStreetMap's edits since (ADR 0014 decision
  14), so the coarse line, the stops' metres and the fine line are then rebuilt together, way
  by way, and a test proves that no stage of any way moved by more than 200 m along it. The
  APK's growth is measured and written here; over 2 MB, the tolerance becomes 8 m.
- [ ] **The outing on a way.** `session.walk` may name a way; on the spot only (a way from home
  is the days', as before). Its goal is the next stage, reached when the reader is within
  300 m of its stop or passes it on the line; "Keep going" then makes the stage after it the
  goal, so a day of two stages is one outing. The corridor is 60 m (a path is mapped less
  exactly than a street). Its signals: a stage or a place reached (its name and its sentence,
  as the ways' notifications say them), off the way, back on it. The start needs the first
  fix within 500 m of the way: otherwise the page says how far the way is and offers an
  ordinary outing.
- [ ] **The longest outing** (ADR 0016 decision 14). An outing on a way on the spot (and
  Phase 20's followed route) starts with a cap of twelve hours instead of four, so a day's
  stage is not interrupted by Phase 15's question; before twelve it asks in the same way, and
  «Keep going» moves it four hours on. The end after 15 minutes still and after an hour paused
  are unchanged: a lunch ends the outing, and the next one starts from where the reader is.
- [ ] **What was walked in person** (`passo.db`, by auto-migration; numbers along a public
  line, never coordinates): `way_stretch` (way, from and to in metres, the day) merged as they
  touch, and `way_visit` (way, stop, the day) for each stage reached on foot. The way's page
  says «37 of 123 km walked there» when there is any, draws those stretches in the walked
  colour at full weight over the faint line, and sets the second ink's stamp, with its day, on
  each stage visited, beside the stamp from home if there is one. A journey from home is not
  needed, not started and not moved by a walk on the spot: the two never touch. The backup
  carries both tables as added fields.
- [ ] **The stage's map.** Under way, the page shows the stretch around the reader (about 2 km
  across, Phase 16's gestures, north up): the fine line, the stops, the reader's point and
  halo, the way's own ground (its rivers, lakes, parks and towns, drawn for a country's scale)
  and one line that says what it is: «A line to follow, not a map of the ground».
- [ ] **The words.** Nothing here names a faith or calls the reader a pilgrim (CLAUDE.md, «The
  Ways' words»): «walked there» / «percorso sul posto», «on the way» / «sul cammino».
- [ ] **Tests.** The data tests (both lines of each way agree in length and in each stop's
  metres; the stage test above), the tracker's on a way (a stage replayed with noise, a
  variant taken and left, a day of two stages, the cap told), the stretches' merge, the way
  page's UI tests with stretches and both inks, the stage map's gallery.
- [ ] Strings in English and Italian; CHANGELOG; the guide's chapter on the Ways; README: the
  Ways' line and a screenshot of a way with stretches walked there.
- [ ] On a device (owner): a stretch of a way within reach, or the same replayed with the
  system's mock-location setting on a debug build: a stage reached, the way left and found,
  the stretches and the stamp on the page afterwards; a long outing's battery, written in
  `docs/battery/` with what a full stage would cost.

**Acceptance:** on a way, the reader's place is on the line within what the receiver allows,
leaving the path is felt within about 30 s, each stage reached on foot is stamped once, and
what was walked there over any number of visits adds up on the way's page; a reader at home
sees the ways exactly as before.

**Edge cases** (`SessionTrackerTest`, `WaysDataTest`, the stretches' tests):

| Case | Expected behavior |
|---|---|
| A variant of the way, off the mapped line for 3 km | Off the way, told once; the stretch is not counted as walked there; back on it where it joins |
| The same stretch walked twice, a year apart | One stretch in the total; the earlier day kept |
| Two stretches that meet | Merged into one |
| A stage reached from home and on foot | Two stamps, two inks, two days |
| Kilimanjaro, whose line goes up and down the same path | The search near the last place keeps the climb and the descent apart |
| The outing nears twelve hours | Asked as every outing is before its cap; unanswered, ended and told, the stretches up to there kept |
| Lunch of forty minutes | Ended by stillness as always, with "Resume" for 15 minutes; a new outing continues the same day's stretch |
| A way deleted from Your ways | The journey from home goes, as today; what was walked there stays unless the reader deletes it too, asked apart |

### Phase 20 — Your routes («I tuoi percorsi»)

The same guidance for any line the reader brings: a GPX from elsewhere, or an outing they walked
before. And for the walk most readers repeat, Passo's own question asked of a route: am I ahead
of my usual or behind it?

**Read first:** ADR 0016 (decisions 11, 13, 14 and 16), Phases 16, 18 and 19 here and their code
(`GpxCodec`, `RouteMapView`, `LineSnap`, the outing on the spot, the twelve hours);
`TypicalDayCalculator.kt` and `TodayOverview.kt` (how "ahead of your usual" is said for a day);
`SessionsScreen.kt` (the door to the cities), `SessionPlans.kt`.

- [ ] **A route kept** (`routes.db`: `saved_route`, its name, length, climb and line in chunks;
  simplified to 5 m with its metres along). From an outing's page, «Walk it again»; from a
  file, «Open a GPX» through the system's picker, and Passo offered for a `.gpx` opened from a
  file manager (an intent filter on the activity; no permission). `GpxCodec` reads what the
  world writes: a track or a route, several segments, with or without times and elevations;
  a file with no line, or broken, is told as such; a very large one is simplified, never
  refused for its size alone. The reader names it, or it keeps the file's name.
- [ ] **Following it.** The Outings page gains «Your routes», a door like the cities': each
  route with its small map, its length and climb. Start, or «The other way round». The outing
  is one on the spot (Phase 18's rules, a 60 m corridor, the twelve hours): the distance left,
  the shares of the route as its signals if the reader chose them, off the route and back on
  it, the arrival. Its page afterwards is an outing's, with the route followed drawn faint
  under the one walked.
- [ ] **The usual round** (`:core:domain/routes`, `RouteMatch`, pure). Two outings with GPS are
  the same round when they start within 100 m of each other, their lengths are within a tenth,
  and four fifths of each lies within 40 m of the other; rounds are found when an outing ends,
  from its box first, and kept (`round`, in `routes.db`). From the third time on, the outing's
  page says it, sentence first («Your usual round, the sixth time: 2 minutes faster than
  usual»), against the median of the times in motion before; and while a free outing with GPS
  that began where a round begins is still on it after 500 m, the card says at each kilometre
  how far ahead or behind the usual it is, in the day's own words. The reader may name a round
  or tell Passo that two are not the same. Never a record board, never a segment: one reader,
  their own walks.
- [ ] **Tests.** `GpxCodecTest` (files as other apps write them, kept as test resources: check
  each one's licence, or write them by hand), `RouteMatchTest`, the follow's replays, the
  Outings page's and the route pages' UI tests.
- [ ] Strings in English and Italian; CHANGELOG; the guide; README: the feature line and a
  screenshot if one earns its place.
- [ ] On a device (owner): a GPX from another app followed from start to end; a usual walk
  taken three times, then the fourth with the card's line; the other way round.

**Acceptance:** a GPX made by another app is followed with the same signals as a city walk; the
third time a round is walked Passo knows it without being told, and is right about which walks
are the same one.

**Edge cases** (`GpxCodecTest`, `RouteMatchTest`):

| Case | Expected behavior |
|---|---|
| A GPX with waypoints only | Told: there is no line to follow |
| A GPX of 200 km | Kept, simplified; started from wherever the reader joins it |
| The same streets walked the other way | Not the same round |
| A round with a detour of 300 m one day | Still the round (four fifths), its time compared |
| Two rounds from the same door | Told apart by their lines, not by their start |
| A round walked once with a long pause | Compared by time in motion, as every outing |
| A route's source outing deleted | The kept route stays: it is its own copy |

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
[`docs/planning/02-decisions-to-1-nov-2026.md`](docs/planning/02-decisions-to-1-nov-2026.md), and
those of 2 Nov 2026 (the review, Your year on foot, 1.1.0 prepared) in
[`docs/planning/03-towards-1.1.0.md`](docs/planning/03-towards-1.1.0.md), both frozen (the index,
[`docs/planning/README.md`](docs/planning/README.md), lists each by its title).

This log starts after 1.1.0's preparation (2 Nov 2026); its entries are written below.

- **No way on a widget** (owner, 2 Nov 2026, after 1.1.0's tag: "the Ways on a widget seems noise
  to me"; the question had stood open since Phase 11). Not built, and no longer asked. A way moves
  a few kilometres a day, so a line about it on a home screen would say the same thing all day,
  every day, beside a count that does move: the reason the way's card on Today shows only what
  the day did for it (archive 02, «The way under way on Today»). Where the reader stands is
  Insights' to say, and the way's page's; a stage reached is a notification already. The widgets
  stay about today.
- **«At a glance» and Tempo's «In words»: one circle at one size** (owner, 4 Nov 2026, with the
  two cards side by side on the phone, two by two: "make the circles of the two cards uniform, so
  that at the same size of card and the same content they are the same size; leave the circles'
  resizing as it is, but at the same size and text the circles must match"; the screenshots the
  owner named did not reach the session, the gallery's were drawn and measured instead). On the
  reference two-by-two (159 × 189 dp) the ring took the height its words left (the number, the
  sentence over two lines, the goal: 108.24 dp of 161), 52.75 dp, and grew where the sentence was
  shorter (73.75 dp for one line, 95 for none); Tempo's dial stood at 60.5 to 63 dp, held by the
  date beside it or by its note, larger on a free day. Now, under three cells wide, both cards'
  circle is the **tall circle** (`tallCircle` in `GlanceLayout.kt`; Tempo's `WordsFit.tallCircle`,
  the same arithmetic): the height the card's inside leaves over a budgeted band of words, a 34 sp
  line and three 16 sp lines at 1.32 em (108.24 dp at the default size, the ring's usual words),
  rounded down to a quarter, never under `TallRingMin`. Budgeted, never measured, so the day's
  words do not move it: on the reference two-by-two both draw 52.75 dp, a long sentence and a
  short one, a free day and a busy one; at 115% and 130% the band grows and both stand at 44 dp
  (the sentence yields a line first, as before). Where the reader hid the sentence the band is the
  number and the goal, and the ring takes that (95 dp on the two-by-two, as before). Each card's
  own limits still hold (here 104 dp and the card's width; on Tempo's the date's column beside the
  dial), so the two match wherever neither binds: on a card two cells wide up to about 199 dp tall,
  the two-by-two on every grid measured; a two-by-three keeps its 104 dp ring, Tempo's dial its 63
  dp. From three cells wide nothing changes: the panel's ring stays the header's 56 dp, and a tall
  card without the hours keeps its ring as before. Where the circle holds the ring under its room,
  the room stays air: the number grows only past the ring's 104 dp ceiling, as before. Tests:
  `GlanceLayoutTest` (the circle at the reference height and text sizes, one ring through the day
  at 159, 175 and 200 dp wide, the hidden sentence, three cells wide unchanged).
- **The family's circle at every size** (owner, 4 Nov 2026, after the entry above: "not only two
  by two: you must check every size; I wrote it badly before"; then, asked about the cards that
  stand in different compositions: two cells wide and three rows high, "Tempo grows", its dial to
  this ring's 104 dp with its date on a line over the title; three and four cells wide, "Passo's
  ring grows", one size per card for both). Measured on both galleries at every reference grant
  (dp; «In words» busy / free, «At a glance» with the hours / without): one cell 69 / 69 both;
  two by one 47 against 44; three and four by one 56 both; two by two 52.75 both; two by three 63
  against 104; three and four by two 78.5 to 82.5 / 101 against 56 / 74; three by three 104
  against 56 / 104; four by three 119.25 against 56 / 104. Now **the family's circle**, three
  functions with the same arithmetic in both apps (`dotRingSize`, `rowRingSize`, `tallCircle`;
  Tempo's `WordsFit.cellCircle`, `rowCircle`, `tallCircle`): one cell, the shorter side less 8 dp
  each side, 36 to 104 dp; one row, the height inside the snug insets with 84 dp for the words,
  44 to 56 dp, as before here; two rows and up, the height left over the budgeted band of words,
  with one line of sentence from three cells wide (where a sentence that takes two lines on two
  cells takes one), between `TallRingMin` and the ceiling `tallRingCeiling` (104 dp, or 45% of the
  inner side, Tempo's step 8.3) or the inner width, rounded down to a quarter. At the reference
  grants: 69; 44, 56, 56; 52.75, 73.75, 73.75; 104, 104, 119.25. Measured again: the same at every
  grant, with the hours or without, with Tempo's events or without. Here: **the panel's ring is
  the circle** (73.75 dp on the three- and four-by-two, from 56; 104 and 119.25 on three rows): two
  rows high it stands inside the height the number over the goal already gave the header, so the
  bars keep their 58.3 dp; three rows high they stay at their 112 dp. Where the ring leaves the
  sentence less than a column at the far edge (the four-by-three: 84 dp, under 96) and its height
  holds lines of it under the number over the goal, the sentence stands under the number, beside
  the ring (`panelStackedLines`: two lines on the four-by-three). Two side effects, accepted: on
  the four-by-two the words beside the larger ring have 18 dp less, so the goal takes its short form
  («of 10,000», the README's picture) and, where the sentence's column (107 dp in the gallery) is
  narrower than its two lines, the sentence takes three; on the three-by-three the goal is the short
  «of 10,000».
  **The tall card without the hours** takes the circle from three cells wide too: 73.75 dp on the
  three- and four-by-two (a sentence over two lines yields it, as before), 119.25 on the
  four-by-three (it was held at 104); the number grows past the larger of the ring and 104 dp, as
  before. Tests: `GlanceLayoutTest` (the circle at every reference grant; one ring through the day
  at five widths and four heights; the panel's header and bars; the stacked sentence's lines).
- **The panel's ring grows without the sentence, as the tall card's** (owner, 5 Nov 2026, with
  Tempo's «In words» kept to the event: "in Passo, when I hide the day's sentence the circle grows,
  and that's just right; I'd like a similar setting on Tempo's widget, so the circle can grow to
  the same size as Passo's"). Tempo's card gains «The day's note»: off, it says the event alone and
  its dial takes the band of a figure's line and one line, this ring's band without its sentence.
  Asked whether the panel should follow too, the owner: yes, so the two match there as well. The
  panel's ring is now `tallCircle(size, scale, sentence)` with the reader's choice, as the tall
  card's: two rows high 95 dp without the sentence (73.75 with it), the bars 37.5 dp (58.3 with it),
  still the panel (`glanceForm` and the preview's line take the choice too); three rows high the
  ceiling holds it, nothing changes. Tests: `GlanceLayoutTest` (the panel's header, bars and form
  without the sentence), `WidgetFitTest` (every card without the sentence, and the owner's
  two-by-two, 188 × 218 dp, measured from the phone's screenshots, added to its grants: nothing cut).

- **The new icon: two prints on the green** (owner, 9 Nov 2026: the four apps' icons redrawn
  from scratch as one series, "icons that say the app and recall what it does, full frame in
  Android's style, clean and readable when small, flat, the light from the same side for all,
  bright colours but not too much, one palette for the series"). Drawn first on a board, three
  symbols per app, two grounds, the home-screen check and the grid; the owner chose the series
  and asked for a margin between the drawing and the badge's edge (the first board let a symbol
  reach the edge). The mark is **two prints walking to the upper right**, the app's own sole:
  the one behind white, the one ahead amber, the step being taken, as the amber print was on the
  ring it replaces (archive 02, «The launcher icon: Chiaro's ring, Passo's step» and «The family's
  icons, made one suite», which this entry changes). The series' rules, the same in the four apps: the
  ground is the app's colour, lit from the upper left (the green `5CCB86 · 27A35A · 17834A`); the
  symbol white with one amber accent (`FFD36A · FDB528 · F28C17`); every shape a ramp from its
  light tone at the upper left to its deep one at the lower right, no shadow; the whole symbol
  within 28 units of the centre (measured 28.1), a 56 dp circle on the 72 that show, so a round
  mask leaves 8 dp of air and none reaches it. `tools/draw_launcher_icon.py` is rewritten: the
  ground is a drawing now (`ic_launcher_background.xml`) where it was the colour `#F7F4EE`, so
  `values/colors.xml`, which held only that colour, is gone and `ic_launcher.xml` points at the
  drawing. Inside the app, asked in the same review: the status-bar icon (`ic_stat_steps`, every
  notification and the Quick Settings tile) is the two prints in one ink, the outing's walker
  (`ic_progress_walker`, now the script's too) the two prints white and amber on a green disc,
  and the Outing shortcut (`ic_shortcut_outing`) its glyph in white on the green. The guide and
  the first run show no app icon, so nothing changes there. Checked: the XML the script writes,
  drawn and compared pixel by pixel with the approved drawings, differs only at the edges
  (0.6% of the badge's pixels, the antialiasing).
- **The new icon checked on the phone** (owner, 9 Nov 2026: "I merged the PRs of the other
  repositories too. I checked the APKs."; then "tick the boxes"). The device check of «The new
  icon: two prints on the green» is done; its box in «Open» is ticked.
- **The Outing shortcut, an adaptive icon** (owner, 9 Nov 2026, seeing the shortcuts of Passo
  and Tempo on the phone: "now I see a circle, and white around it"; then "make them adaptive,
  for Saldo and for the other two repositories too"). The shortcut drawn as a green disc («The new icon: two prints on
  the green», above, which this entry changes) kept its shape only under a round mask: under any
  other the launcher shrank it onto a white plate, as it does with any icon that is not
  adaptive. `ic_shortcut_outing` is now an `<adaptive-icon>`: the launcher icon's ground under
  the Outings glyph in white (`ic_shortcut_outing_foreground`, its 24 units at 55% of the 72
  that show, the share it had on the disc), the glyph again as the themed layer. The ground is
  written by `tools/draw_launcher_icon.py` a second time, into `:core:tracking`
  (`ic_shortcut_ground`), because a library cannot name the app's drawables. The family's rule
  for the launcher's shortcuts is now the white glyph on the app's colour, in the launcher's own
  shape; where an app draws its mark itself, it stays a disc (the walker on the outing's bar).
  Checked: the layers drawn in a round, a rounded-square and a squircle mask and as the themed
  icon; `:app:lintDebug`, `:app:assembleDebug`, `spotlessCheck` and `:core:tracking`'s tests.
- **The adaptive shortcut checked on the phone, and Tetto in the family** (owner, 9 Nov 2026:
  "I did every test on the phone and it is all fine. You can close every box."; then "Did you
  also align the documents with the reference to the family's new app, Tetto, in the various
  repositories?"). The device check of «The Outing shortcut, an adaptive icon» is done; its box
  in «Open» is ticked. Tetto (budget) is the family's fifth app: the README's «The family», this
  file's opening and «The family in the code», and VISION's opening name it beside Chiaro, Tempo
  and Saldo.
- **Each outing's shortcut wears its own mark** (owner, 9 Nov 2026: "the icon used for the
  «Japanese walk» item is not the one that appears in the app. In the app it had been changed on
  purpose to set it apart from the other walks"). Every shortcut wore the way to a flag, the
  Outing mark, while the Outings page draws an interval walk with its slow and fast line and a
  run with its bolt (`sessionIcon`, «the interval walk no longer wore the same mark as the brisk
  walk above it», the owner's review of 2 Nov 2026). `SessionShortcuts.shortcutIcon` now asks
  `sessionIcon` and maps its answer to three adaptive icons on the same ground,
  `ic_shortcut_outing`, `ic_shortcut_intervals` and `ic_shortcut_run`, each glyph placed as the
  Outing glyph is; one rule for the page and the shortcuts, so they cannot drift apart again
  (`SessionShortcutsTest`, every intensity with every goal). The launcher updates a shortcut's
  icon when the list is written again: when a plan is started, saved or deleted.
- **The outings' own marks checked on the phone** (owner, 9 Nov 2026: "I tried it and it works.
  Tick the box and merge straight into main."). The device check of «Each outing's shortcut
  wears its own mark» is done; its box in «Open» is ticked.

- **Today reviewed for a premium first screen** (owner, 10 Nov 2026: "Transform it into a premium
  Play Store app. A graphic review of the main screen. No noise, only added value."; then, on the
  design: "Fine, implement it in Compose", and, asked how to keep the chart whole on the first
  screen: "Values under the sentence"). What changed on Today, and why:
  - **No glow.** The top stands on the page's own ground; the ring is the one thing in the day's
    colour. The gradient tinted the whole top and greyed the text on it (`docs/adr/0004-design-language.md`,
    amended).
  - **A finer ring** (12dp on 256dp, growing with the text size as the guide's does), with a plain
    round cap: the dot of the ground at the arc's head read as a knob to turn, on every ring
    (`ProgressRing`, the guide's and Settings' too). The notch stays dashed, in the chart's ink.
  - **Inside the ring, the count and the goal only.** The distance and the calories went to a
    row of three under the sentence, **distance, active calories, active minutes**, with one line
    on what the estimates rest on; each amount is one node for TalkBack, read with its own
    meaning, so the shared line is hidden from it and the ring no longer reads the estimates.
    Three across while a third holds the text size, else stacked.
  - **The chart begins on the first screen** instead of being whole on it: on the owner's phone in
    Italian, under two sentences of two lines, its readout and the top of its plot show, the rest
    one short scroll away. This changes «Today's chart whole on the first screen» (archived in
    `docs/planning/02-decisions-to-1-nov-2026.md`); the test says the new rule.
  - **The chart without the grey block over the hours ahead**: the usual day goes on past now,
    fainter (`DayTrendChart`, Today's only user). The goal is named on its line («Goal 10,000»),
    so the key holds Today and Usual; the caption is one line shorter.
  - **The way to an outing is a row as wide as the cards**, with the line that says what one is
    («A walk with a goal, or intervals») and a chevron, where a centred pill read as left over.
  - **The four tiles and the cadence's became one card, «Intensity»**: brisk minutes on today's
    share of the WHO's 150 and the cadence on its ramp, each with its meaning. The distance, the
    calories and the active minutes, now under the sentence, are not said twice.
  - **The streak beside the date** («12 days», with the streak's mark; read as "12 days in a row
    at the goal"), from two days: Insights' streak, the same rule (`StreakSoFar`, pinned against
    `Insights` by a test). The days to yesterday are their own flow, distinct, since the past is
    frozen; today joins once its goal is met.
  The guide's paragraph on the ring and its sample ring say the same (no distance inside it).
  README screenshots regenerated (Today, the dark goal, the chart read, the outing, the foldable).
  Checked: `:core:domain` and `:feature:today` tests, `:feature:guide`'s, lint and spotless.
- **Today's three amounts, centred** (owner, 10 Nov 2026: "can distance, active calories and active
  minutes under the sentence be centred? Do you honestly agree, or is it better to leave them as
  they are?"; then "Ok, do it"). Each amount, its label and the line on the estimates are centred,
  in its third or stacked at a large text size: the ring and the sentence above stand on the
  page's middle, and the row left-aligned was the one block off that axis; three amounts read one
  at a time gain nothing from a left edge, which helps a column of numbers compared down. This
  changes «Today reviewed for a premium first screen», above. README screenshots regenerated.

- **The outings, their editor and the city walks' maps, reviewed** (owner, 11 Nov 2026: "review
  the design of the Outings and walks screens and their children, keeping to Material 3: what
  does not respect it, and what would make them better and more premium; the type too, watching
  for unwanted line breaks and for strings out of line; and the walks' maps. Keep it clean,
  avoid noise"; the review went to the owner as a canvas, today's screens beside the proposals;
  then "I want every point", with the start button as its icon alone). Built in three parts.
  - **The Outings page.** An outing's card reads on one line, the name's: the estimates and the
    signals started at the card's edge, under the mark. One way to start it, the icon at the
    card's end (a tonal icon button, 48dp, named for TalkBack: «Start Brisk walk»), and the card
    itself to edit: the «Edit» and «Start» row is gone, the card is a third shorter and the
    presets fit a screen. A new outing is the top bar's action; with no outing kept, the page
    still offers it where the list would be. The introduction is one sentence in the body's
    smaller size. The estimates are shown short with the sign Today uses («≈ 2,000 steps ·
    1.46 km») and how the signals arrive as marks before their line; TalkBack reads the whole
    sentences as before. The city walks' door shows a walk's map. Italian: «Segnali al 50%».
  - **The card of an outing under way** (`SessionCard`: Today, Outings, a walk's page). Its
    mark is the size of the cards' below it (40dp, it was 36dp: the names were 3dp out of line).
    The sentence is the headline, in the title's large size; on a city walk the place ahead,
    with how far it is under it («Royal Festival Hall», «The next place, 230 m ahead»: the
    notification and the voice keep «Next: …»). How far it is and where it ends stand at the two
    ends of its bar, not beside the name, which a long name («Japanese walking», «A walk in
    London») shared the line with. The pace's verdict is a word beside the state («Under way ·
    on pace»), and the numbers three columns between two hairlines (the pace, the steps, the
    distance with the calories beside the word «estimate»; paused or over, the steps and the two
    estimates); TalkBack reads them as the two sentences they were, and at a large text size
    those sentences are what is shown. Stop and Pause share the card's width at 48dp.
  - **The editor.** Its top bar takes the scrolled tone as the settings run under it, as the
    Outings page's does, and Save's bar wears the same: on the page's own ground a line cut by a
    bar read as a fault. «Goal, always», a label in the accent's colour with an icon, read as a
    text button that did nothing: gone, the sentence above the chips says it. One stepper
    (`StepButton`, the goal's tonal one) for the interval rows too. No check mark on any of the
    three segmented rows (two had it, one not). The interval rows are a line each: their notes
    broke in two in the column the steppers left them, and what a set is reads once under the
    group. The Japanese walk is said in two lines on the page's quiet ground, the study and how
    Passo walks it a touch away (it was twelve lines in the accent's container, the tone of a
    choice made and of a warning, and it pushed the sets off the first screen). The vibrations
    are a legend in the switch's own group: each signal with its pattern drawn beside it (drawn
    from `SessionHaptics`' own: one to three short pulses, a long one, two long ones, four taps,
    a long and a short), felt at a touch, said in words to TalkBack; it takes the place of the
    paragraph that listed the patterns and of the chips to try them. This changes how «Outings:
    vibrations in counts» and «The interval walk, built» (archive 02) put them on the page, not
    the signals themselves.
  - **A sentence's last word is not left alone on a line** (`unorphaned()`, `:core:designsystem`):
    its last two words are held together, a number with its unit too, up to 20 characters and
    not above 1.3 times the text size, where a pair held could be wider than the line. On the
    cards' descriptions and the editor's paragraphs («sforzo moderato», «with your step of
    73 cm»). The strings themselves are untouched: the voice says the same ones.
  - **The map** (`WayMapView`). The way still to walk was a grey at 40% over a city's white
    streets and was lost among them: it has more ink (72%), and on a page the whole way lies on
    an edge of the page's colour. The side streets are hairlines at part ink. A name goes on the
    side its own way leaves free (`placeLabels`, pure and tested: fewest names covered, then the
    least of the way and its stops), where beside its point it lay over the line (London, Rome,
    Paris). A city walk under way names the place being walked to, drawn larger in the goal's
    colour, not its two ends (this changes ADR 0014's decision 8 for a walk under way; a way
    keeps its ends' names, and the locator its rule). Before a start, where it begins is a full
    point and where it ends a ring: they were the same ring. The water is a tone deeper in light
    (`PassoColors.water`: a river through a city was almost the land's own blue-grey). A name's
    pill has a hairline around it. On a page a map has the groups' 24dp corner (16dp above
    24dp cards, at the same width), the continents' too; a city walk's map is never taller than
    wide (Rome's, at 1.2, pushed Start past the middle of the screen).
  - **The places** of a walk and the stages of a way: one line through their points, in the
    goal's colour as far as the reader has come, in place of a rule between every two; the stop
    ahead stands out.
  README screenshots regenerated (the outings, the editor, the intervals, the outing on Today,
  the walk, the ways). Checked: every module's tests, lint, spotless, the forbidden permissions.
  Not done on a device: the box is in «Open».

- **An outing's numbers centred, as Today's three amounts are** (owner, 11 Nov 2026, on the
  screenshots of the review above: "can the words in those boxes be centred, as on the main
  screen? Everywhere the app shows this kind of row, so they are the same across the app"). The
  app sets a few amounts in a row between two hairlines in two places, Today's three under the
  sentence and an outing's card (on Today, on the Outings page, on a walk's page): both are now
  drawn by one component, `StatStrip` (`:core:designsystem`), each amount centred on its share,
  with the same hairlines and the same room. Today's is unchanged to the pixel; the card's, left
  aligned when it was made earlier today, follows it. The values keep their two sizes (the
  reading's on the page, a step smaller under a card's headline). History's and Insights' tiles
  are another thing, a card each with its mark and its meaning, and stay as they are. README
  screenshots regenerated (the outing on Today, the intervals over, the walk).

- **History reviewed, after Today and the Outings** (owner, 11 Nov 2026: "review the design of
  History and its child screens, the ones the earlier review did not touch; Material 3, what is
  wrong today and what would make it more premium; the type, the line breaks nobody wanted and
  the alignments; a clean layout, in step with what is already reviewed, no noise", then, on the
  review drawn beside the pages as they were, "apply everything": the eight faults and the three
  things that asked for a yes). The four pages were drawn in Italian at 360dp, where the faults
  were, and read again after.
  - **The scales.** «Settimana» read «Settim…» in its quarter of the row: the segments keep 4dp
    a side for their word, not Material's 12dp, and a label still too long for its room goes a
    step smaller (12sp from 14sp at most) before it is cut, at the standard text size too
    (`FittedLabel`, which `SegmentLabel` now is; at 320dp the four words are whole).
  - **A bar's reading** is two lines that stay two: the day and its steps, and under them what
    came of it («Goal met» with the goal's mark and colour, «1,677 short of the goal»). On one
    line it ran to three in Italian, «10.536» parted from its «passi», and the chart moved
    under the finger. The way in is one word at the line's end, «Open» (a screen reader still
    says «Open day», «Open month»), and the readout keeps a button's height (48dp) whether or
    not the button is there: it was 40dp without it and 48dp with it. A day with no bar touched
    reads its busiest hour the same way, the hour and its steps, «The busiest hour» under them.
  - **Said once.** The days' chart told the reader to touch a bar twice (the readout and the
    line under the plot) and what its line is three times (the key, the number at its end, the
    line under the plot): the line under the plot is gone for a week and a month. A year keeps
    «Each bar is the month's average day», a day its shaded walks and the touch (the walks named
    only on a day that has them), the calendar «Touch a day to open it».
  - **No word alone.** A page's sentence and the line under it, the captions and a tile's
    meaning go through `unorphaned()`, and the sentence's numbers are held to their words in
    the strings («su 4, finora», «20% in più», "of 24"): «…5 giorni su / 7» was a headline.
  - **The tiles.** A label is one line, a step smaller where half a phone is short for it
    (`MetricTile`'s `fitLabel`; "Active calories" broke at 360dp, and a label on two lines set
    its value a line lower than its neighbour's); a day's brisk minutes are «Passo svelto» in
    Italian beside the mark, the whole name still read aloud. The meanings are short («Estimate,
    from your step», «Estimate, movement only», «With 40 steps or more», «From 100 steps a
    minute»): one line or two balanced ones, where they ran from one to three. The mark is
    quiet, the highest container with the variant ink: the accent's container is the tone of
    the scale chosen above, and five marks wore it. This changes one thing in «An outing's
    numbers centred» above, which left History's and Insights' tiles as they were: the tile is
    the same card, its mark another tone, in Insights and the guide too (`MetricTile` is theirs).
  - **Walks and outings in a day's list** (`OutingList`, so on Today as well). The four figures
    on one line broke where the width ran out, «108 passi/» from its «min»: they are two lines
    that never change places, what was counted (the steps, the cadence) and what is estimated
    from it (the distance, the calories) after the «≈» Today uses, each figure held together.
    What came of an outing has a line of its own under its name: beside it, «Obiettivo
    raggiunto» broke in two. TalkBack reads the sentences it read.
  - **Ranges.** A week inside one month says the month once, on the side the locale writes it
    («21 – 27 set», "Sep 21 – 27"; `dayRange`), and the dash stays with the first date. An hour
    is written as the walks under it are, «12:00–13:00» (`clockRange`): the page had both. The
    range has a place to break after its dash that shows nothing: at twice the text size
    «12:35 PM–12:55 PM» broke inside its last «PM».
  - **The period's name** keeps the room of its two lines when it has one, so at a large text
    size the page does not move up and down as the weeks are paged (at the standard size the
    arrows' 48dp already held it: the review said otherwise, wrongly).
  - **The year's pages** are a tonal button at the headline's edge, with the same words: a text
    link between the sentence and the chart read as a caption.
  The goal calendar keeps its ticks on the days at the goal (the review's drawing had left them
  out): they say it without the colour. Tests: a bar's two lines and its «Open», a week in one
  month, the four scales uncut in Italian. README screenshots regenerated (History's day and
  month; no other picture shows a tile or a day's list). Checked: every module's tests, lint,
  spotless, the forbidden permissions. Not done on a device: the box is in «Open». Left as they
  are, outside History: Insights' and the year's own ranges still say the month twice («Sep 7 –
  Sep 13»).

- **Insights and Your year on foot reviewed, after Today, the Outings and History** (owner,
  11 Nov 2026: "review the design of Insights and its child screens, such as Your year on foot,
  the ones the earlier reviews did not touch; Material 3, what is wrong today and what would make
  it more premium; the type, the line breaks nobody wanted and the alignments; a clean layout, in
  step with what is already reviewed, no noise", then, on the review drawn beside the pages as
  they were, "apply everything": Insights' twelve faults and its three things that asked for a
  yes, the year's seven, the Ways' two). The pages were drawn in Italian at 360dp and at 320dp,
  where the faults were, and read again after.
  - **Insights, the streak.** The card stands on the groups' own ground: it still had the
    gradient Today lost on 10 Nov, the goal's colour is the marks'. With no streak it is a title
    and its line («No streak right now», «A day at your goal starts one.»), as the running one
    is: one sentence ran to three lines beside the mark, «una.» alone on the last. In the seven
    days, a day before the first one counted is a point: it was a third empty circle, a hair
    thinner than a missed day's and today's.
  - **Insights, the way under way.** The map (72dp), the way, where the reader stands and the
    stage ahead, with the chevron every other door on the page has; under them the bar the card's
    width, how far the reader has come and how far the end is at its two ends, as an outing's
    card sets them (`wayToEnd`, new beside `wayNextStage`). It was four small lines in 192dp,
    «tra / 14,7 km» broken, the bar under the words only.
  - **Insights, the records.** No mark before a record: three trophies alike said nothing, in
    the titles' colour, and without them every group's words stand on one edge. The amount takes
    the full ink, the date stays quiet. A week or a streak inside one month says the month once
    («7 – 13 set», `dayRange`, History's), each date whole on its line; the month of the best
    month is in the language's own case («agosto 2026» under «dom 13 set»).
  - **Insights, the averages** are Today's strip (`StatStrip`): three amounts side by side
    between two hairlines, «steps a day» said once in the group's title, the last seven days
    against the seven before in a sentence under them («…dei 7 / giorni prima» was a line
    break). An average with too few days yet is a dash, and one line says when it comes. The
    page is about 120dp shorter.
  - **Insights, the years.** A year is a row of one line; what a year's pages are is said once,
    under the group (every row said it, «i / mesi» at a line's end). The group's rule is the
    other groups'.
  - **Insights, the totals.** The title says what the group is («In all, since Jul 26, 2026»):
    «Since Jul 26, 2026» was a date, and the averages' third label besides. The tiles keep their
    label on one line (`fitLabel`, as History's): «Days at your goal» is «At your goal» beside
    the mark, whole for a screen reader, where on two lines it set its value a line lower than
    its neighbour's; «days» is small as «km» and «kcal» are; the meanings are short, in History's
    words («Estimate, movement only», «Estimate, from each day's step»).
  - **Insights, the door to the Ways** is a title and two or three lines (it was seven, in a
    list's row), its mark in the row icons' quiet ink. The page's sentence, the line under it and
    the season card's body go through `unorphaned()` («…per farne / 5.» at 320dp).
  - **Your year on foot.** Whose year it is stands under the marks at one height on every page,
    and the rest is centred in the room under it: the eyebrow moved between 105 and 340dp from
    page to page, and the title jumped at each touch. A page's number is one line, a size
    smaller where a narrow phone has no room for it («3.065.44 / 0» at 320dp). How the pages turn
    is said at the foot of the first page, outside what is drawn into the image: it was inside
    the page, so in the picture shared or saved. The sentence and the lines under it break as a
    title does (`LineBreak.Heading`), a way's name and a stretch of days are not parted, a fact
    has a line of its own where a «·» joined two, and the rhythm's two sentences are a line each.
    The outings' and the ways' pages give their number large, as every page with one does («Out
    with a goal», 25, «outings»; «Along the ways», 1,188, «km», then each way by name with its
    amounts under it): they had it in the title, at the title's size. The year's strings have the
    curved apostrophe the rest of the app has; a weekday after a colon is in the language's own
    case; a year begun late says «· since Oct 1», the year once. A page saved says so in a
    snackbar: it was the app's one toast.
  - **The Ways.** A route's last word is not left alone («…a / Roma»); a way kept says its year
    once, with the day it ended («Walked from Sep 1 to Oct 7, 2025»), and its mark is in the row
    icons' quiet ink. «…Santiago, da / Saint-Jean-Pied-de-Port» stays: in the row's 176dp the
    name does not fit beside its preposition.
  This changes how «Phase 5» and «Phase 12» (archive 01) and «Your year on foot» (archive 03) put
  these pages on the screen, not what they count. Tests: a week in one month and the averages
  side by side, the card with no streak, Insights in Italian at 360dp, the hint at the foot of
  the first page only, the outings' and the ways' numbers, the year in Italian at 320dp. README
  screenshots regenerated (Insights, Your year on foot). Checked: every module's tests, lint,
  spotless, the forbidden permissions. Not done on a device: the box is in «Open».

- **12 Nov 2026.** **The first run's lines balanced, in the four apps** (owner, after the review
  of Tempo's, Tetto's and Saldo's first runs against this one: "if a word alone at the end of a
  line should be fixed in Passo too, we do it in all four repos"). The first run's texts are
  centred and short, where a word alone on the last line («…in the / background.») reads as a
  slip; the budget app already broke them balanced. `:core:designsystem` gains `balanced()`
  (`LineBreak.Heading`: lines of about one width, never a word alone on the last), and every
  centred text of the first run and of the no-sensor screen takes it: the titles, the sentence,
  the promises, the bodies, the goal's note, the permission's refusal. Only the first run: the
  rest of the app keeps its breaks until a review of its own looks at it screen by screen.
  `onboarding-goal.png` regenerated (the welcome's breaks did not change). Checked: the
  onboarding's tests and their screenshots, spotless.

- **14 Nov 2026.** **The widgets swap names** (owner: "I'd like to swap the two widgets' names, so
  the «In parole» widget becomes the same for the whole family"). The ring card (`GlanceWidget`)
  is now «In words» / «In parole», as Tempo's card with the dial is; the card in type alone
  (`WordsWidget`) is «At a glance» / «Colpo d'occhio». Only the names move: each card keeps its
  forms, its description in the launcher's list and its settings. Changed: the two labels
  (`widget_glance_label`, `widget_words_label`, English and Italian), the guide's «Two widgets»,
  the README's caption, VISION, CLAUDE.md, §7, ADR 0005 (amended), the CHANGELOG's Unreleased, and
  the comments and test names that name a card. **The code keeps its names** (`GlanceWidget`,
  `WordsWidget`, `WidgetKind.GLANCE` / `WORDS`, `widget_glance_*`, the `glance/` and `words/`
  packages), now the other way round from the cards': the launcher binds a placed card to its
  receiver's class name, so renaming `GlanceWidgetReceiver` / `WordsWidgetReceiver` would take
  every card off the readers' home screens, and renaming only the rest would leave the code half
  one way and half the other; `WidgetKind`'s comment says so. The README's `widgets.png` draws no
  name, so it was not regenerated. The log above keeps the names of its time.


- **15 Nov 2026.** **The widgets' names swap back, the cards wear green, a seventh colour**
  (owner, for the whole family: in Passo the two names looked swapped; every «A colpo
  d'occhio» first in the list and 4×1; each app's widgets in its own colour; a seventh colour
  that stands apart). The rule is now one for the four apps: «At a
  glance» / «A colpo d'occhio» is the card with a picture (here the ring), «In words» / «In
  parole» the card in type alone, as on the weather app. So 14 Nov's swap is reverted in the
  code, the labels, the guide, the README's alt text, VISION and CLAUDE.md, and the code's names
  agree with the cards' again (`WidgetKind`'s note on the older names went with it); the entry
  of 14 Nov above keeps its names. The Italian label is «A colpo d'occhio» in the whole family:
  the launcher's list sorts an app's widgets by name, and «A colpo d'occhio» / «At a glance»
  sorts first in both languages here; it is also declared first, and opens at 4×1 as before.
  - **A new card wears the app's green** (`WidgetLook.cardColor`, owner: each app's widgets in
    its own colour), and so does a card placed before and never configured, whose colour was the
    default rather than a choice. The picker's static previews follow (`widget_preview_card`,
    and `WidgetProviderTest` now reads it from the default look rather than a hex).
  - **The ochre** (`#5C4A0E`), the family's seventh card colour, after the owner asked for one
    that stands apart from the six in their style and is no shade of violet: the hue the six
    left out, 30 ΔE from its nearest (the terracotta), white ink 8.6:1, the 75% ink 5.7:1, the
    85% one 6.8:1 (measured in the weather app's DESIGN §2.6, the family's table). Seven is where
    it stops (the owner asked; a bright series would need a dark ink measured on every mark, and
    a strip that scrolls hides colours).
  - **The strip of colours starts under the radio buttons** (10 dp, the first disc on the
    buttons' line), not under their labels: seven 48 dp targets want 336 dp: in the settings group (16 dp margins), less the strip's
    own 12, a 384 dp phone leaves 340 and holds them in one row; a 360 dp phone leaves 316, and
    `swatchesPerRow` breaks them into four over three, never one alone (with six, the strip under
    the labels already broke there, five over one). Tested at 384 dp (one row) and in arithmetic.
  - Checked: the widget module's tests, spotless; `widgets.png` and `widget-settings.png`
    regenerated (README).

- **15 Nov 2026.** **The shortcuts written again at every start of the service** (owner: "the
  shortcuts all wear the same drawing for the different kinds of walk, but in the app each walk
  has a different one, at least the Japanese walk"). The rule has matched the Outings page since
  9 Nov 2026 (`SessionShortcuts.shortcutIcon` asks `sessionIcon`; `SessionShortcutsTest`): the
  interval walk its slow and fast line, a run its bolt, every other walk the way to a flag, so
  the brisk walk, the stroll and the reader's own walks share the flag, on the page and on the
  launcher alike. What could still show the same mark everywhere is the launcher's copy of the
  list: it keeps the shortcuts, icons included, as they were last written, and they were written
  only when a plan was started, saved or deleted, so a list written by a version before 9 Nov
  2026 outlived it on the phone. `StepTrackingService` now writes the list again at the end of
  each start (an update, through `MY_PACKAGE_REPLACED`; a reboot; the app reopened after the
  process died): one read of the plans and one write, no wake-up of its own, the launcher's rate
  limit caught as before. Checked: `:core:tracking`'s tests, spotless. On the phone: the box in
  «Open».
  - **Four shortcuts, not three** (owner, the same day, on a tablet: the marks were right, a run
    with its bolt, but the Japanese walk was not there). The long press offered three outings,
    the most recently started first, then the Outings page's order: on a phone where none was
    started yet, the first three presets, and the interval walk is the fourth.
    `SessionConstants.SHORTCUTS` is 4: a first visit finds all four presets, and four is as many
    as the common launchers show. No reason for three was written anywhere.

- **18 Nov 2026.** **Location, for an outing that asks for it** (owner: "at the moment GPS is
  not planned, the rules forbid it; forgetting that rule, analyse how GPS could be integrated,
  turned on by the reader and leaving the app usable as it is now without it"; then, on the
  analysis: "I approve the direction completely and everything you proposed"). **This changes
  an archived decision:** «Route maps with GPS stay out» (1 Oct 2026,
  `docs/planning/02-decisions-to-1-nov-2026.md`, line 120). Its four reasons are answered one
  by one in `docs/adr/0016-location.md`, which is the record of this decision; in short:
  - **What it is.** Location as a measuring tape, never a tracker: read only inside an outing
    the reader started with «Measure with GPS» on. The day's count, the widgets and every
    screen are as before for a reader who never turns it on, who is asked for nothing.
    `INTERNET` and `ACCESS_BACKGROUND_LOCATION` stay forbidden, and the build's gate becomes an
    allowlist, stricter than today's. Satellites only, through the platform's
    `LocationManager`: no Google Play services, no new dependency.
  - **What sets it apart.** The steps and the fixes are fused (no pause guessed from a speed,
    no distance while still or on a bus, no metre lost in a tunnel, the receiver asleep when
    the steps stop); every outing with GPS teaches the step counter the reader's step, so days
    without GPS read truer; the city walks and the ways can be walked on the spot, with the
    content Passo already has; a round the reader repeats is compared with their usual. And a
    recorder of routes that holds no `INTERNET` cannot send one.
  - **The owner's three decisions** (each as recommended):
    1. *The day's distance stays the steps'.* Only the outing shows a measure. A day mixed of
       measured and estimated stretches would break frozen days, the import's merge and "Apply
       profile to past data"; once the step has learnt, the two agree within a few percent.
    2. *Four hours stays the longest outing, but for two kinds, which get twelve.* The cap
       counts from the start by the clock, stops included, and ends the outing without a word
       (`SessionEnd.CLOSED` is neither told nor reopenable). Beside the end after 15 minutes
       still, it is what stops an outing forgotten by a reader who keeps walking all day; with
       GPS on, that forgetting would cost a battery, so the cap matters more, not less. But a
       day's stage on a way is six to eight hours with its stops: the cap would fall in the
       middle of it and end the guidance silently, on a path. So an outing on a way walked on
       the spot (Phase 19) and one that follows a route (Phase 20) start with twelve hours.
       Every other outing, with GPS or not, starts with four.
       *And no cap ends a walk unasked* (owner, the same day, on reading the above: "before
       the GPS is turned off, is the reader warned, and can they say to go on, if they walk
       more than four hours?"). They were not: the plan only told the end after it. Now, for
       every outing: 15 minutes before its cap it asks, by a vibration and a notification with
       «Keep going», which moves the cap four hours on; unanswered, it ends at the cap, says
       so, and can be taken back for 15 minutes, the receiver staying on meanwhile so the
       route has no hole. The question rides on a step batch, so no timer is added and a
       reader who is sitting is not asked. Built in Phase 15, since it is every outing's;
       ADR 0016 decision 14, which amends ADR 0009 decision 6.
    3. *One APK.* A second build without the permission would double every release's tests and
       ask every new reader to choose before knowing the app; the allowlist, the permission
       asked only in context and Android's own pages do that job.
  - **Six phases, 15 to 20** (§11), each written to be built in a session of its own: the
    measured outing; the outing's page (route, splits, climb, GPX); the step that learns; on
    the spot, the cities; on the spot, the ways; your routes. 15 and 16 are released together
    or not at all. The number 15 is free: the «In pictures» plan that once had it was written
    and reverted before anything was built (pull requests 68 and 69), and is in no archive.
  - **Nothing is built, and nothing in force has changed.** Written today: ADR 0016, the phases,
    this entry, the «Planned» notes in «State», §9, §10, VISION.md and CLAUDE.md. The manifest,
    the gate, the invariants and the README change in the change that builds Phase 15, as ADR
    0013 amended §9.7 in the change that built Phase 13.
  - **«Sul posto» is "On the spot"** in English (owner, the same day).
  - **Left for the owner to confirm on the way** (owner: "ask me when it is needed, and give me
    suggestions anyway"): the two new vibrations (Phase 18, in "Try them") and the cost line
    beside the switch, once measured (Phase 15). The session that reaches one asks the owner
    then, with its proposal and an alternative or two, and builds with the proposal meanwhile
    so the work does not wait.
  - **To prove on the phone before building on it** (Phase 15's first device box): that a
    running `health` service takes the `location` type by a second `startForeground()` from a
    page and from the notification's Resume. The documentation says a type is added that way
    and does not say how the second call is checked; ADR 0016 decision 4 has the fallback.

- **18 Nov 2026.** **Phase 15 read against the code before it is built** (owner: "check whether
  the phase has gaps, or whether you have suggestions, and list them apart, before the plan";
  then: "OK for the gaps, write them in the phase"). Nothing built. Written into §11 Phase 15,
  each where it belongs, none changing a decision of ADR 0016:
  - **`onStartCommand` would have taken the `location` type away.** It calls
    `startForeground()` with `health` alone at every opening of the app (`MainActivity.onResume`
    starts the service each time) and at every command; and its catch stops the service on a
    refused type. The one function that knows the types answers there too, and a refused
    `location` falls back to `health`: the count never stops for it.
  - **The cap was said to be stored, and was in no list.** `Session.capAtMillis` and
    `capAskedForMillis`, in the model and in schema v7; the wake lock's own bound follows the
    cap and is taken again at «Keep going» (§9.7's "4 h 10 min" is reworded in Phase 15's
    change); an end that can be taken back is the tracker's knowledge, not `SessionEnd.CLOSED`.
  - **One clock.** A fix is laid by its elapsed time, as a step sample is, never by the
    satellites' clock.
  - **Steps ahead of their fixes.** An open stretch is shown from the steps, and the goal, the
    milestones and the places are told on the settled distance only. The step batch asks for
    the fixes in its own wake (`requestFlush`), and the receiver's own delay is 60 s, not 30:
    one rhythm of wakes for the two streams.
  - **The permission.** «Only this time» is a grant, to be proven on the phone under a service
    that never stops; a revoked permission ends the process, so that row is the restarted
    process with the permission gone. The check, Passo's screen and the request are behind one
    door in `:app`: the outings start from three modules.
  - **Two databases share no transaction.** The route is deleted first, the outing after; the
    service deletes a route without an outing when it starts; a restore from the cloud copy
    has measured outings with no route.
  - **The samples travel with the reader's own file** (`step_length_sample`, an added field of
    the outing): neither Phase 15 nor 17 carried them.
  - **The allowlist is exact both ways**, as the ADR says; a permission named after the
    application id is resolved per variant; the task keeps its name.
  - **The proof on the phone comes first**, on a thin slice, before the fusion, the storage and
    the screens are built on the answer.
  - Smaller: the presets are four, not three; only the points kept get their altitude
    converted; the battery's record says which phone and whether it held the wake lock.
  - **Waiting for the owner, since each changes a decision or a rule** (asked the same day, to
    be judged on what is best, the ADRs and CLAUDE.md to follow): what wakes the receiver on a
    phone with no wake-up counter in an outing without signals; how GPS comes back after a
    restarted process; the city walk from home with GPS on; the receiver while an ended outing
    can still be taken back; "Walk now" on a plan with GPS on.

- **18 Nov 2026.** **The five answered, and the platform read again** (owner, on the five left
  waiting: "re-evaluate them without regard to the ADRs or CLAUDE.md: tell me what is best and
  premium, with respect for the battery too, and we change the ADRs and CLAUDE.md to follow";
  then, on the answers: "I confirm all five, write the amendments and go on"). Written as
  amendments in `docs/adr/0016-location.md`, each under its decision with its reason, and in
  §11 Phase 15; CLAUDE.md, §9.7 and ADR 0013 change in the change that builds Phase 15, as
  every rule in force does, and until then stand as written.
  1. **A phone with no wake-up step counter: an outing with GPS holds §9.7's wake lock**, with
     signals or without (ADR 0016 decision 5). The first proposal, the receiver slowed to a fix
     every half minute during a stop, had been made only to leave §9.7 as it was, and is the
     worse on both counts: more battery than a processor awake with the receiver off, and the
     first step found up to half a minute late. The lock is already built, measured and let go
     after 15 minutes still. It widens an exception CLAUDE.md says never to widen: the owner's
     decision, and the invariant's words change with Phase 15.
  2. **GPS comes back by itself, and its stop is told** (decision 4): told once by a
     notification and the voice, «Resume GPS» on the notification and the card, and with no
     touch when the app is brought forward.
  3. **A city walk from home never reads the satellites** (decision 13, made plain): two ways
     to walk one, each with one meaning. The edge case that had it «with GPS on» contradicted
     the same phase's own sentence, "the ways from home still use no location". Not a matter
     of privacy: an outing of the reader's own, started at their door, is measured from its
     first step.
  4. **The receiver stays while an ended outing can be taken back**, after the goal and the
     stillness as after the cap (decisions 10 and 14): at most 15 minutes, asleep 2 minutes
     after the reader stops, off when the card is put away. The alternative, off at the goal
     and the stretch laid from the steps, costs nothing and can take the word «measured» from
     an outing taken back.
  5. **"Walk now" keeps the plan's word** (decision 4): with GPS when the plan has it on and
     the permission is there, on the steps and said once otherwise.
  - **No «Trim the ends»** (owner, the same day: hiding where a walk starts and ends "seems a
    little extreme; if you agree, we avoid it, and the app is cleaner and clearer"). Agreed:
    Passo has no feed, a share is one act towards someone the reader chose, the image is a
    line on a ground with no streets, and the GPX's choice says what the file holds. §11 Phase
    16 loses the switch, its remembered setting and its edge case; ADR 0016 decision 9.
  - **The platform, read again** (Phase 15's first box, ticked): Android 17's change lists
    say nothing on location, the `location` service type or the while-in-use rule. The
    "Location Button" is real and is not in those lists but on the permissions page: an
    experimental Jetpack library (`androidx.core.locationbutton`, 1.0.0-alpha01, 17 Jun 2026)
    with its own permission, for momentary access; Google Play asks for it of apps that
    target API 37 and have only session-based needs. Passo measures a walk for its whole
    length with the screen off: the classic request stays, no dependency is added, and Phase
    9 reads the policy itself before a listing. The one-time grant gives the same permissions
    as «While using the app»; how long it lasts under a service that never stops is written
    nowhere, and is in the proof on the phone.

- **18 Nov 2026.** **Phase 15, the slice for the proof on the phone** (owner: "write the
  amendments and go on"). On the branch `feature/phase-15-measured-outing`; built so that the
  first device box can be answered before the fusion, the storage of routes and the screens are
  built on it (§11 Phase 15, «This box comes before the rest is built»).
  - **The gate is an allowlist** (box ticked). `AllowedPermissions` in `build-logic`: the ten
    names a green build's report held, plus the three of location; exact both ways; the one
    named after the application id resolved per variant; `permissionVerdict` pure, with nine
    tests, which `./gradlew test` now runs too (the root's `test` reaches the included build).
    Proven as in Phase 0: `CAMERA` and `ACCESS_BACKGROUND_LOCATION` added to `:core:tracking`'s
    manifest turned the build red, the first as not on the list and the second as forbidden,
    each by name; then removed.
  - **The manifest** (box ticked): the three permissions, `health|location`, the `gps` feature
    not required, and its opening comment rewritten.
  - **Schema v7, whole.** Every column of the phase is in it now (`gps` on a plan and an
    outing, `stepsMeters`, `measuredShare`, `capAtMillis`, `capAskedForMillis`,
    `step_length_sample`), not only the plan's `gps` the slice needs: a debug build installed
    for the proof must not meet a second, different v7 at the next build. An outing gets its
    cap when it starts; what reads it is the tracker's question, not built yet.
  - **The receiver and the type.** `LocationSource` (the platform's `LocationManager`,
    `GPS_PROVIDER`, 1 s, batched 60 s with the screen off, flushed by each step batch and at
    the screen coming on); `GpsDuty` (pure: awake while the reader walks, asleep after 2
    minutes, off at a pause and the end, and steered as while it counted for as long as an end
    can be taken back); one function, `enterForeground`, that every `startForeground()` goes
    through, `onStartCommand`'s included, so a look at the app no longer takes the type away,
    and a refused `location` falls back to `health` without stopping the count. The type is
    asked for only from an outing's own start, resume and «Keep going», and from the activity's
    own start of the service (`StepTracking.start(fromApp = true)`); never from a restart by
    the system or a broadcast. `SignalWake` holds the lock for an outing with GPS on a phone
    with no wake-up counter, while the service holds the type.
  - **The log's row** (`DiagnosticsType.GPS`, `GpsLog`): fixes, the good ones, the ones that
    came with the screen off, batches and the largest, the longest gap inside one stretch, the
    first fix's delay, sleeps, the type added and refused and what refused it. Counts and
    delays only. Until the meter is built the fixes are counted and let go: no route is kept
    by this build, and an outing with GPS still shows the steps' distance.
  - **The switch and the door.** The editor's «Measure with GPS» with what it gives and what
    it costs, absent with no receiver, the estimate's note and the wake lock's note true with
    it on; `OutingStarter`, the one door the Outings page and the launcher's shortcut start
    through; `GpsStart` (pure) decides: Passo's own screen and then the system's request the
    first time, the request alone after a grant the system took back, nothing after a refusal;
    `GpsIntroScreen`, its page, and the answer remembered beside the settings.
  - **Not in this slice**, and so not for the proof: the stop told by a notification and
    «Resume GPS» as a button (the receiver does come back when the app is opened), the card's
    lines for each state of the receiver, the cap's question, `RouteMeter` and the routes'
    database, the backup's added fields, the documents of the «Amends» list. CLAUDE.md, §9,
    §10, VISION and the README still say what was true before this branch: they change before
    it is merged, in this same change.
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions` (13 permissions, both variants), `test` in every module and in `build-logic`, `:app:lintDebug`, `:app:assembleDebug`: green, but for the six tests of the next line. The new pages' screenshots looked at (the intro in the light, the dark, Italian, twice the text size and on the open foldable; the editor with the switch). **Not checked: anything on a phone.** No fix has been received by this code yet: that is the proof's first answer.
  - **A machine's limit, not a fault:** on the owner's Windows the six tests that walk a
    migration (`MigrationTestHelper`) fail before reaching Room, on a comparison of paths
    («This driver is configured to open a database named…»), on an untouched checkout as
    here. Schema 7 was generated and `UpgradeFromFirstReleaseTest` walks to it: CI, on Linux,
    is where it runs.

- **18 Nov 2026.** **Phase 15, the domain: the fusion, the route's bytes, the cap's question**
  (owner, asked whether Phase 16 could begin while the phone cannot be tried: "can you go on
  safely, and in keeping with the plan?"). Not Phase 16 yet: it draws and shares routes that
  come from the part of 15 not built. But what the proof on the phone decides is one thing
  only, how the service comes by its fixes, and ADR 0016 decision 4 says the rest is the same
  either way. So Phase 15 goes on from its pure domain, which a phone adds nothing to, and
  Phase 16 follows it. **A departure from the order written the same morning** («This box
  comes before the rest is built»), made at the owner's word and reworded in the phase: the
  proof still comes before any release, and before the fallback is or is not built.
  - **CI is green on the pull request** (#72, both runs): the migration to schema 7 passes
    where it can run.
  - **`RouteMeter`** (pure, 26 tests, one a rule and one a row). The steps say when the reader
    walks, the fixes how far. A batch of steps is laid over its own time in motion; a fix
    outside it moves the line's end and adds nothing (the bench, the tram); a fix inside it
    moves the line by a grain. Open until the fixes are known to be in through its end
    (`settle`), so nothing is told on metres that could be taken back. Decided on the way,
    each with its reason in `RouteConstants` and each to be validated on a device:
    - *The grain grows where the receiver is less sure* (half the fix's own accuracy, three
      metres at the least): the wander between tall buildings stays under it.
    - *After a ride the receiver is believed again once five fixes in a row agree.* Judged
      only against the last fix accepted, every good fix after a bus read as too fast from
      the stop it left, for minutes.
    - *No hop beyond what the steps since the line's end could reach*, and no batch beyond
      what its steps could cover (2.5 m a step, the longest a profile may hold): a reflection
      that lasts, a tram, walking down a moving tram's aisle.
    - *Steps that go nowhere are measured as what they are*: marching on the spot under the
      satellites is no distance, where the steps alone said 165 m.
    - *A point says how it is joined to the one before* (`RoutePointKind`): measured, laid
      from the steps (a hole: dashed, later), or the start of a stretch (the first point, a
      pause, the far side of a ride): a route never draws a line across a place not walked.
      The phase's model had a fix or a laid point only; the third is what a pause in a GPX
      and a tram on a map both need.
  - **`RouteFilter`, `RouteCodec`, `Pace`**, and **`StepLengthSample`** from the meter's clean
    runs. The codec: a point in about six bytes, an hour in about ten kilobytes, a chunk that
    stands by itself and never throws on damaged bytes.
  - **The tracker** takes a meter for an outing with GPS: steps, time in motion and the
    intervals are told with the steps as ever; the distance, and the energy priced on it,
    when it is settled. What is walked after an end that can be taken back waits aside, and
    is the outing's again at «Keep going» (the route with no hole), or nobody's.
  - **The longest outing asks before it ends**, for every outing: `Session.capAtMillis` set at
    the start and read by the tracker, `SessionSignal.CapAhead` at the first steps within 15
    minutes of it, `goOn` moving it four hours, the end at the cap told and taken back as the
    stillness's is. In the service: the notification «Still walking?» with «Keep going» (an
    id of its own), the stillness's two long pulses, the voice; the wake lock's bound follows
    the cap and is renewed. Posted even where the outings' signals are silenced: it is a
    question, and an outing ended unasked is the fault it mends. **Not yet:** the card saying
    it too.
  - **The model's box ticked.** The domain's is not: the pace's words by the kilometre or the
    mile come with the card that shows it.
  - **Still to build in Phase 15:** the meter wired into the service (the flush that settles,
    the fixes on the steps' clock, the points written), `routes.db`, the backup's fields, the
    stop told with «Resume GPS», the card and the notification's lines, the documents.
  - **The routes' database, the same day.** `routes.db` (`RouteDatabase`, schema 1 exported):
    `route` (the outing's id and start, the count, the box) and `route_chunk` (outing, order,
    bytes); `RouteRepository` appends a chunk, reads a route back whole, deletes one. A route
    is its outing's by the outing's start, not by the id alone. `SessionRepository.delete`
    deletes the route first, then the outing with its intervals and its samples;
    `deleteOrphanRoutes` deletes every route whose outing is gone (for the service's start);
    the samples are written and read. `data_extraction_rules.xml` names `routes.db` and its
    write-ahead log under `<device-transfer>` only, and a test reads the file to say so. 13
    tests on real Room databases. **Not yet:** the service calling any of it, and the
    backup's added fields in the JSON and the CSV.
  - **The meter in the service, the same day.** Each fix goes to the tracker on the steps' own
    clock (`Location.toFix`: its elapsed time, by the snapshot a step sample is read with;
    never the satellites' time). After a batch of steps the receiver is asked for its fixes in
    that wake, and the distance settles when they are in (`settleSoon`; at once where the
    receiver holds nothing back; the wait gives up after the sensor's own 1.5 s, the processor
    being awake for the batch). Before a pause, a stop or a resume the fixes up to the touch
    are waited for, as the steps are. The points kept are written as a chunk after the step
    batch's transaction, their altitude turned to mean sea level off the main thread
    (`RouteAltitudes`: a point that cannot be converted loses its altitude rather than keep one
    on another datum); the samples go to `passo.db`. An outing too short to keep has its route
    deleted first. At the service's start the routes with no outing are deleted, only where
    the routes' file exists. **The stop is told**: a restarted process posts «GPS stopped»
    with «Resume GPS» (or "Open Passo" where the permission is gone), says it if the outing
    speaks, and the notification goes when the receiver is back. A mock fix is taken only by a
    debuggable build. The log's row gains what the route was built from and the measured
    share. From here an outing with GPS shows its measured distance and keeps its route.
    **Not yet:** the card's and the notification's lines for the receiver's states, the pace
    in words, the backup's fields, the documents.
  - **The backup's added fields, the same day** (box ticked). The JSON: a plan's `gps`; an
    outing's `gps`, `stepsMeters`, `measuredShare` and its `stepLengthSamples`, added fields
    with no new version, read back exactly, absent from an outing without GPS, and dropped
    where they cannot be a measure (a share is kept between nothing and all, a sample with no
    steps says nothing). No place is in the file. The import brings the samples of the outings
    it adds, under this phone's ids, and none twice. The same walk with GPS and without are
    two plans (`BackupMerge`). The CSV's outings gain three columns at the end (`gps`, the
    steps' own distance in the reader's unit, the measured percent), empty for any other
    outing, so an older sheet's columns stay where they were.
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions`, `test` (the domain's 469
    among them), `:app:lintDebug`, `:app:assembleDebug`: green, but for the six migration
    tests this machine cannot run. Nothing on a phone.

- **18 Nov 2026.** **The rules in force brought in line with what is built** (owner: the pull
  request is to be merged into `main`, and the work to go on in a new session). The manifest
  on the branch asks for location while CLAUDE.md, §9 and §10 still said "never": merged so,
  `main` would have held rules its own code contradicts, and the next session would have read
  them as its instructions. Changed in this same change, as ADR 0016 asks: CLAUDE.md (the
  allowlist; location as a measuring tape, with its bounds; the day stays the steps'; the
  battery's two exceptions, the wake lock for an outing with signals or with GPS; `routes.db`
  in the backup's rule; the modules), §9 (rule 9, and rule 7's amendment), §10 (the three
  rows, the forbidden names, the allowlist), «State».
  - **Still to change, of the «Amends» list** (Phase 15's documents box stays open): VISION.md
    (principles 1 and 3, the non-goals, «Privacy is enforced», the glossary's «measured»), the
    README's privacy lines, an «Amended» line in ADR 0007, 0009 and 0013, the strings that
    promise no location read again one by one, the guide's «With GPS» part, CHANGELOG.
  - **Merged into `main` is not released.** A release is a tag, and Phases 15 and 16 are
    released together or not at all; until then `main` holds a measured outing whose card
    does not yet say what the receiver is doing, and whose proof on a phone is not done.

- **18 Nov 2026.** **Phase 15, what the reader sees, and the documents** (owner, opening a new
  session on the branch, pull request 72 still a draft: "go on with Phase 15, then Phase 16;
  I cannot test on the device for now, so go on with everything that does not depend on the
  proof, and do not build the fallback service"). Phase 15's boxes are ticked but the two on
  a device; nothing here was tried on a phone.
  - **One line for the receiver, decided in the domain** (`GpsLine`, `GpsLines`, pure, 16
    tests): finding the satellites, the signal lost, location off on the phone, GPS stopped,
    the permission gone, and the three ways an outing started on the steps. Measuring has no
    line (the word under the distance says it) and neither has the receiver asleep at a stop
    (the numbers stand). In order: what «Resume GPS» could not mend comes first (the
    permission, the phone's switch), then the receiver lost, then what it is doing. Decided on
    the way:
    - *A tunnel is counted on the steps that were settled*, as the meter counts a hole, not on
      the clock: with the screen off the receiver holds its fixes up to a minute, and a card
      opened then would have said «no signal» until they came.
    - *Waking after a stop is not a tunnel.* The quarter of a minute is counted from the last
      fix or from when the receiver woke, whichever is later; «Finding the satellites» is said
      only before the first fix of a line (a start, a resume), not at every traffic light.
    - *Location turned off or on is heard from the receiver's own listener*
      (`LocationReading.Provider`), so the card changes at once and nothing polls.
  - **The distance says what it is** (`Measured.kt`): «measured» from nine tenths up
    (`RouteConstants.MEASURED_SHARE`), under that the share the steps gave («30% from steps»),
    and before the first fix «from steps»; the calories beside it keep Today's sign for an
    estimate (≈). The stretch whose fixes are not in yet is shown with it
    (`LiveMeasure.openMeters`), never told on.
  - **The pace, in words** (`MeasureFormatter.pace`, two units, «9:40 min/km» and «min/mi»):
    one line under the numbers, «Pace 10:05 min/km · now 9:48 min/km»; said by TalkBack and by
    the outing's voice as «10 minutes 5 seconds a kilometre». The pace of the whole only over
    a measured distance (ADR 0016 decision 7); the pace now only for half a minute after the
    last step (`RouteConstants.PACE_NOW_MILLIS`): a reader who has stopped has none. The
    voice says the outing's pace at each signal and at the goal. In Italian the pace is
    «andatura», which leaves «ritmo» to the cadence against the outing's own.
  - **`LiveSessionState.measure`** carries the three (the open stretch, the pace now, the
    line) from the service to the card and to the counting notification, which says the same
    in words and gains «Resume GPS» as a third button when the receiver was lost.
  - **«Said once», made exact.** A refusal is remembered (`gps_on_steps_told`, beside the
    reader's answer, not in the backup): the first outing it costs says so on its card, with
    the way back, and the ones after it do not. What is not a refusal is said each time it
    happens: location off on the phone, and the reminder's "Walk now" before the permission
    was ever asked. A grant clears it, so a later refusal is told once again.
  - **The way back goes through the outings' one door** (`OutingStarter.onCardAction`):
    «Resume GPS», the system's switch for location, or the system's request for the
    permission, alone (Passo's own screen is shown once). Where Android no longer shows its
    request (after a second refusal) its page for Passo opens instead: known from what it
    says before and after the request, with no timer. Granted from the card, an outing that
    lost its receiver takes it back at once.
  - **The question before the cap is on the card too**, read from the stored outing
    (`capQuestionMinutes`), with «Keep going»: no ticker, the minutes move as the steps do.
  - **The rows** of Today and History: «1.49 km measured», the satellites' mark before it,
    the calories still with their ≈. A distance partly measured stays with the estimates.
  - **The type change, apart from the service** (`ForegroundTypes`, 11 tests): granted,
    refused by the rule, refused in a way no page documents, held and then refused, dropped
    at a pause, a restarted process, the app opened mid-outing. The service itself is not run
    under a test: what its tests would add is what the phone answers.
  - **The documents** (box ticked): VISION (principles 1 and 3, the outings, the non-goals,
    the two success criteria, the glossary's «measured», «pace» and «fix»), the README (the
    outings' line, a row of its own for location, the backup's row; no dash), an «Amended»
    line and its paragraph in ADR 0007, 0009 and 0013, ADR 0016's status, the guide («With
    GPS», «What is kept, and what is never sent», the cap's question, the battery's part),
    Settings' privacy line, CHANGELOG. The strings that promised no location, read one by
    one: the guide's "Passo never knows where you are" was no longer true and now says that a
    way walked from home reads no location; `ways_intro` ("No location, only your steps") and
    the calibration's "no GPS" are true as they are, and stay.
  - **Not changed, and said to the owner as suggestions, since each would change a decision:**
    an outing that started on the steps stays on the steps when the permission is granted
    from its card (its `gps` is frozen at the start, as the model says), so the line promises
    the next outing, not this one; and after a refusal the editor's switch does not ask
    again by itself (ADR 0016 decision 2 asks at a start only).
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions`, `test`, `:app:lintDebug`,
    `:app:assembleDebug`: green, but for the six migration tests this machine cannot run. The
    card's screenshots looked at in every state, in the light and the dark, in Italian, at
    twice the text size and on the open foldable; the row in History. **Nothing on a phone.**

- **18 Nov 2026.** **Phase 16, the outing's page: the route, the splits, the climb** (owner:
  "then go on to Phase 16, from its section, without «Trim the ends»: a new branch from
  Phase 15's, and a draft pull request based on it, not on `main`"). On
  `feature/phase-16-outing-page`, pull request 73, a draft on top of 72. Every box ticked but
  the one on a device; nothing here was tried on a phone.
  - **The climb** (`Altitude`, `Climb`, `RouteHeights`, `PressureTrack`, pure; `PressureSource`).
    A point's height is what the pressure says, less what the weather is known to add, and
    the weather is learnt from the satellites over ten minutes: a front that drops three
    hectopascals over a flat two-hour walk climbs nothing, and a hill is in the pressure at
    once. Decided on the way, each a constant with its reason, to be validated on a device:
    - *The level's settling is kept out of the climb.* For its first five minutes the level is
      still being found, and that correction is the satellites', not the reader's legs.
    - *A point waits for its pressure.* The points come with the step batches and the
      pressure in batches of its own; a point given its height before its reading arrived
      would have had the satellites' alone. It waits at most a minute and a half, and the
      outing's end takes every point with the readings there are.
    - *Three metres with a barometer, ten without*, counted from where it was last counted;
      a pause or a ride cuts the count, so a funicular is not a climb.
    The sensor asks for no permission, is registered only with the satellite receiver and
    removed with it, never wakes the processor by itself, and is flushed in the wake the step
    batch already made.
  - **The splits are cut from legs laid as the outing goes** (`OutingLeg`, `Legs`, `Splits`),
    not from the route's points. A hundred metres at a time, from what the meter settles:
    the distance is the fused one, and the time in motion and the steps are the steps' own
    (ADR 0016 decision 5), so a kilometre's pace is the one the outing's own pace is made of
    and its cadence is counted. Kilometres or miles are cut from them afterwards, whichever
    the reader walks in; and they are numbers, so they stay when a route is deleted (the
    edge case "its numbers stay"). **It adds to ADR 0016 decision 8's list** of what `session`
    keeps (the fused distance, the steps' own, the measured share, the climb): the legs, in a
    table of their own, `session_leg`. Numbers, not places, as that decision asks; said to
    the owner, for a line in the ADR at their word.
  - **Schema 8, not a second 7** (`ascentMeters`, `descentMeters`, `session_leg`), and
    `routes.db` schema 2 (where a route's heights came from). The phase allowed either while
    Phase 15 is unreleased; a debug build installed to try Phase 15 on a phone must not meet
    a v7 that changed under it, which was this morning's own reason for making v7 whole.
  - **Energy on a slope**: 0.009 kcal a kilogram a metre climbed walking, 0.0045 running (the
    grade's term of the ACSM equations, in `MetricsConstants`), added to an outing with GPS
    when its climb is counted; nothing for coming down. The day's energy stays the steps'.
  - **`RouteMapView`**, Passo's first map that moves, and `MapViewport` with its gestures
    apart (Phases 18 and 19 reuse them): two fingers zoom, one drags it once it is zoomed, a
    double tap zooms where it lands, a button brings the whole route back. **One finger on a
    whole map is left to the page**, which scrolls: a map in a list must not trap the finger
    that only meant to read on. North up, a scale bar in round lengths of the reader's units,
    the line dashed where it was laid from the steps and never drawn across a pause, a ring
    where it began, a point where it ended, a bead a kilometre, named with its pace when
    touched (and said to TalkBack). The ground is a city's map where at least four fifths of
    the points lie in its frame (`RouteGround`), the finer of two that qualify; plain
    elsewhere, with the line that says Passo carries no street map.
  - **The page** (`OutingScreen`, `OutingViewModel`, one key in `:app`): the sentence, when it
    was, its numbers as tiles each with its line («Measured by satellite. Your steps said
    4.10 km»), an interval outing's bars, the kilometres as rows, the climb as a shape, and
    what the route can do. Reached from «See the outing» on a finished card, from an outing's
    row in Today and in History (a walk found in the steps opens nothing), and from the
    notification of its end. **No line for "the goal's pace"** among the splits: an outing has
    no pace to reach (ADR 0016 decision 7: shown, never a goal), so there is none to draw.
    The kilometres are rows with a bar, not columns: a walk has as many as it has kilometres,
    and a row still holds its numbers at twice the text size.
  - **Sharing and saving.** `GpxCodec`, GPX 1.1 by hand, a segment a stretch walked, read
    back too. The image is a card of its own (the name, the sentence, the map whole, three
    numbers), recorded out of sight and fitted as the year's pages are. **One file provider**:
    `SharedFiles` and `ShareImage` in `:core:designsystem` (`.share`), which the year's pages
    now go through; the year's own provider and its paths are gone. No «Trim the ends»: the
    GPX's choice says, once, that the file holds the walk from the first step to the last.
  - **Your data.** The backup's file gains an outing's climb and legs, and, only with «With
    the routes» ticked (off each time the page opens), its route: the app's own compact
    points in Base64, an added field. An import lays a route on the outing that started at
    its instant where that outing has none (`BackupMerge.routes`): the ones that came with
    their outings, and the ones of a phone restored from the cloud copy; never one taken
    away. «Delete all routes» says how many and what they weigh, and asks first. Both rows
    only on a phone that keeps routes.
  - **No read makes the routes' file** (`RouteFile`). Counting the routes for Your data, or
    deleting an outing too short to keep, opened `routes.db` and so made it, on a phone that
    had never measured with GPS: the second was Phase 15's own. Every read and every deletion
    now asks whether the file is there first; only a route written makes it.
  - **The documents**: the guide (an outing's own page; its route to take or to delete),
    CHANGELOG, the README (`outing-route.png` with its caption, the feature's line; no dash).
    Not changed, and said to the owner: CLAUDE.md's table of modules, which does not name
    this phase's classes yet.
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions` (the same 13 permissions:
    the pressure sensor asks for none), `test`, `:app:lintDebug`, `:app:assembleDebug`:
    green, but for the seven migration tests this machine cannot run (the six, and 7 to 8).
    The page's screenshots looked at in each of its forms, in the light and the dark, in
    Italian, at twice the text size and on the open foldable; the map in both dresses and
    both themes, zoomed, with a bead named; Your data with its two rows and the dialog; the
    README's picture. **Nothing on a phone.**

- **18 Nov 2026.** **Phase 16 read again: the last points' pressure, and the shared image's
  test** (owner: "go on"; a second reading of the service and of the sharing, on
  `feature/phase-16-outing-page`, pull request 73).
  - **The pressure sensor outlives the receiver by the moment of a write.** At an outing's
    end the receiver, and the pressure sensor with it, were removed before the end's own
    write asked the sensor for what it held: with the screen off that is up to a minute of
    readings, so the last points of a route took the satellites' height alone and a climb in
    the last minute could go uncounted. The sensor is now kept until the end's harvest is
    written, then removed, unless the receiver is on again (an end taken back at once). A
    pause and a stop from the card ask the sensor first too, as they ask the receiver. No
    new wake: each happens in the wake that ended or touched the outing, and the wait gives
    up after the same bound as the receiver's.
  - **The image to share has its test** (`OutingScreenTest`): the card recorded out of sight
    is 360 dp wide whatever the page's, opaque, and adds no second map to the page; its
    picture is written to `build/screenshots` and was looked at.

### Open

- [ ] **On a device (owner): the shortcuts after an update** (§15, 15 Nov 2026, «The shortcuts
  written again at every start of the service»): install the build over the old one, then a long
  press on the icon: four outings, the Japanese walk with its slow and fast line, a run with its
  bolt, the other walks with the flag, without starting or saving a plan first. On the tablet the
  marks are right (owner, 15 Nov 2026); the fourth shortcut and the phone remain.

- [ ] **On a device (owner): Today's premium review** (§15, «Today reviewed for a premium first
  screen»): the ring, the row of three, the streak by the date, the chart and the Intensity card,
  in the light and the dark, at a large text size and with TalkBack.

- [ ] **On a device (owner): History's review** (§15, «History reviewed, after Today and the
  Outings»): the four scales in Italian, a bar's reading and «Open», the tiles' quiet marks (in
  Insights too), a day's walks on two lines, in the light and the dark, at a large text size and
  with TalkBack.

- [ ] **On a device (owner): Insights' and the year's review** (§15, «Insights and Your year on
  foot reviewed, after Today, the Outings and History»): Insights with and without a streak, a way
  under way, the averages' strip and the tiles in Italian, in the light and the dark and at a
  large text size; Your year on foot page by page, a page shared and one saved (the image without
  the hint, the snackbar); a way kept on the Ways page; TalkBack on the averages and on a page.
