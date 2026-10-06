<div align="center">

# 👣 Passo

**Every step, counted. Kept on your phone, sent nowhere.**

A private, battery-friendly Android pedometer that counts every step, even if you never open it.
Free, no account, no ads, no tracking, and no permission to use the internet at all.

![Platform](https://img.shields.io/badge/platform-Android-2E6B3E?labelColor=FCFAF6)
![Release](https://img.shields.io/github/v/release/fiorenzobrioni/passo?label=release&labelColor=FCFAF6&color=2E6B3E)
![CI](https://img.shields.io/github/actions/workflow/status/fiorenzobrioni/passo/android-ci.yml?branch=main&label=CI&labelColor=FCFAF6&color=2E6B3E)
![License](https://img.shields.io/badge/license-GPL--3.0-007DB6?labelColor=FCFAF6)
![minSdk](https://img.shields.io/badge/minSdk-34-70569C?labelColor=FCFAF6)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-F1A000?labelColor=FCFAF6)
![Compose](https://img.shields.io/badge/UI-Compose%20Material%203-007DB6?labelColor=FCFAF6)
![Internet](https://img.shields.io/badge/INTERNET%20permission-none-2E6B3E?labelColor=FCFAF6)

[**⬇️ Download the latest release**](https://github.com/fiorenzobrioni/passo/releases/latest)

</div>

## What Passo is

Most step counters are either a big health suite (accounts, cloud sync, a long list of
permissions) or a thin front end over another app's data. Passo does one thing: it counts
steps with the phone's own low-power step counter, keeps them on the device, and turns them
into distance, calories, active minutes, goals, streaks and the walks you took.

It keeps counting across reboots and full shutdowns without you ever opening it. Distance and
calories are estimates, and Passo says so.

## Screenshots

<table>
  <tr>
    <td align="center" width="33%"><img src="docs/screenshots/today.png" width="250" alt="Today: 7,855 of 10,000 steps, 1,484 ahead of the usual pace, with the day's chart"><br><sub><b>Today</b>: the ring and one sentence on the day</sub></td>
    <td align="center" width="33%"><img src="docs/screenshots/today-chart.png" width="250" alt="The day's chart read with a finger at 12:55 PM"><br><sub><b>Your day</b>: read the chart at any minute</sub></td>
    <td align="center" width="33%"><img src="docs/screenshots/today-goal-dark.png" width="250" alt="Today in the dark theme: 10,772 steps, goal reached at 5:51 PM"><br><sub><b>Goal reached</b>, in the dark theme</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/history-day.png" width="250" alt="History, one day: goal met with 10,415 steps, three walks, the steps hour by hour with the walks marked"><br><sub><b>History</b>: a day, with the walks found for you</sub></td>
    <td align="center"><img src="docs/screenshots/history-month.png" width="250" alt="History, one month: the steps day by day against the goal, and the goal calendar"><br><sub><b>A month</b>, against each day's goal</sub></td>
    <td align="center"><img src="docs/screenshots/insights.png" width="250" alt="Insights: four days in a row at the goal, the last seven days, and the records"><br><sub><b>Insights</b>: streak, records, averages</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/outings.png" width="250" alt="Outings: a brisk walk of 20 minutes, a 30-minute run and the rest of the day, each with what it comes to and its signals"><br><sub><b>Outings</b>: a walk with a goal</sub></td>
    <td align="center"><img src="docs/screenshots/today-outing.png" width="250" alt="Today with a brisk walk under way: 12 of 20 minutes, past halfway, 112 steps a minute on pace"><br><sub><b>Under way</b>: progress and pace</sub></td>
    <td align="center"><img src="docs/screenshots/outing-editor.png" width="250" alt="Editing an outing: the signals at 25, 50 and 75 percent, the vibrations to try, and the voice through headphones"><br><sub><b>Signals</b> you can feel or hear</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/way.png" width="250" alt="The Via Francigena under way: the map from the Great St Bernard Pass to Rome with the part walked, past Monteriggioni, 280 km to Rome"><br><sub><b>Ways</b>: a pilgrim route, walked from home</sub></td>
    <td align="center"><img src="docs/screenshots/way-credential.png" width="250" alt="The credential: a stamp for each stage reached, from the Great St Bernard Pass to Cassio, each with its day"><br><sub><b>The credential</b>, a stamp a stage</sub></td>
    <td align="center"><img src="docs/screenshots/way-start.png" width="250" alt="Starting a way: from today, from 1 January (you would already be past Vetralla), or from your first day with Passo"><br><sub><b>A start in the past</b> places you at once</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/walk.png" width="250" alt="A walk in London under way: the map from Buckingham Palace along the Thames with the part walked, past the London Eye, and the outing's card: next, the Royal Festival Hall, 230 m"><br><sub><b>City walks</b>: London place by place, walked where you are</sub></td>
    <td align="center"><img src="docs/screenshots/intervals.png" width="250" alt="Editing the Japanese interval walk: what it is, then 3 slow minutes, 3 fast minutes and 5 sets, with the outing's shape in slow and fast blocks"><br><sub><b>Japanese interval walk</b>: slow and fast in turns</sub></td>
    <td align="center"><img src="docs/screenshots/intervals-done.png" width="250" alt="An interval walk just over: goal reached, each fast interval's cadence against the pace line, 4 fast intervals of 5 at pace"><br><sub><b>Each fast interval</b>, against its pace</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/widgets.png" width="250" alt="The two widgets on a home screen: At a glance with its ring, In words, a terracotta pair side by side, and the day hour by hour"><br><sub><b>Two widgets</b>: At a glance and In words</sub></td>
    <td align="center"><img src="docs/screenshots/widget-settings.png" width="250" alt="One widget's settings: the card as it will look, its sizes, the background colour, the opacity and the content"><br><sub><b>Widget settings</b>, with a live preview</sub></td>
    <td align="center"><img src="docs/screenshots/settings-notifications.png" width="250" alt="Settings, notifications: goal reached, the evening reminder with its time and threshold, the weekly summary"><br><sub><b>Notifications</b>, each one optional</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/onboarding-welcome.png" width="250" alt="The first run: welcome"><br><sub><b>First run</b>: under a minute to counting</sub></td>
    <td align="center"><img src="docs/screenshots/onboarding-goal.png" width="250" alt="The first run: the daily goal, with what it means in distance and time"><br><sub><b>A goal</b> in kilometres and minutes</sub></td>
    <td align="center"><img src="docs/screenshots/settings-appearance.png" width="250" alt="Settings: appearance, with a live preview"><br><sub><b>Appearance</b>: palettes and typefaces</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/calibration.png" width="250" alt="Measure your step: 131 steps over 100 m make a walking step of 76 cm, against 74 cm estimated from the height, so distances read 3% longer"><br><sub><b>Measure your step</b>, no GPS</sub></td>
    <td align="center"><img src="docs/screenshots/settings-data.png" width="250" alt="Settings, your data: a backup saved with 412 days and 38 outings, and the rows to back up, import and export for a spreadsheet"><br><sub><b>Your data</b>, in a file you keep</sub></td>
    <td align="center"><img src="docs/screenshots/data-import.png" width="250" alt="Importing a backup: when it was written, 413 days of steps and 38 outings, how it joins this phone's days, and the choice to take its profile and settings"><br><sub><b>Import</b>: shown first, nothing deleted</sub></td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/guide.png" width="250" alt="The guide: what Passo does, the three screens with their icons, and how Passo counts"><br><sub><b>The guide</b>, in the app</sub></td>
    <td align="center" colspan="2"><img src="docs/screenshots/today-foldable.png" width="510" alt="Today on the inner screen of an open foldable: the date, the ring, the sentence and the day's chart in a column in the middle, the glow behind them across the whole screen"><br><sub><b>On an open foldable</b>: a centred column</sub></td>
  </tr>
</table>

Drawn by the app's own screens from realistic sample days, in English (the app also speaks
Italian). The phone's status bar is not in the pictures. The command that redraws them is in
[Build](#build).

## Features

- 👟 **Today**: the steps against your goal, where a usual day stands by now, and one sentence on how the day is going.
- 📈 **Your day**: a chart you read with a finger, plus distance, calories, active and brisk minutes, cadence.
- 📅 **History**: a day, week, month or year at a time, and a calendar of how close each day came.
- 🚶 **Walks, found for you**: stretches of walking recognised in the steps already counted.
- 🎯 **Outings**: a walk or run with a goal, with vibrations or a voice at the milestones you choose.
- ⏱️ **Japanese interval walk**: 3 slow minutes and 3 fast ones, five sets or more, each change told with a vibration of its own so the phone stays in your pocket; at the end, how many fast intervals were at pace.
- 🏆 **Insights**: your streak, your best day, week and month, and the averages.
- 🗺️ **Ways**: five pilgrim ways (Via degli Dei, Camino Portugués, Via di Francesco, Camino de Santiago, Via Francigena) walked from home, stage by stage, with a stamp for each. No location: only your distance.
- 🏛️ **City walks**: Milan, Rome, Paris, London, Madrid, Berlin, Vienna, Porto, Amsterdam and Prague in Europe, Lima, Cusco, New York, Rio de Janeiro, Mexico City, Buenos Aires, San Francisco, Québec, Havana and Cartagena in the Americas, Tokyo, Sydney, Seoul, Beijing, Hong Kong, Singapore, Bangkok, Kyoto, Hanoi and Melbourne in Asia and Oceania, walked where you are in one outing or a few, about 5 km or about 10. Each continent has its page, with a map of where its cities are and which you have walked. Each place is told as your steps reach it, with a short vibration and, if you like, a line about it in your headphones.
- 📏 **Measure your step**: walk a distance you know and Passo works out your step length. No GPS.
- 🏠 **Two widgets**: At a glance and In words, resizable, in six colours and any opacity.
- 🔔 **Notifications**: goal reached, an evening reminder, a weekly summary. Each one optional.
- ⚡ **Quick Settings tile**: today's steps and the share of the goal.
- 💾 **Your data**: a backup to a file, a CSV for spreadsheets, and an import that never deletes anything.
- 📖 **The guide**: what each screen answers, and what a screen cannot say out loud.
- 🎨 **Appearance**: light or dark, two palettes, three typefaces, the same as Chiaro's.
- ♿ **For every reader**: TalkBack throughout, text up to twice its size, foldables as a centred column.
- 🇮🇹 🇬🇧 **Italian and English**, through the system per-app language picker.

## Principles

| | |
|---|---|
| 🔒 **Private by design** | no `INTERNET` permission: Passo cannot send anything. The build fails if a network, location, exact-alarm or body-sensor permission appears |
| 👟 **One source of steps** | the phone's hardware step counter. No Health Connect, no Google Fit, no Play services |
| 🔋 **Battery is a feature** | the step counter runs on a low-power chip; no timers with the screen off, widgets updated only when visible. Target: about 1% of a day's battery |
| 📏 **Honest estimates** | distance and calories say they are estimates; every formula is documented in the code with its source |
| 🗓️ **Past days are frozen** | a new goal or profile changes today, never your history (unless you ask) |
| 📁 **Your files are yours** | export and import go through Android's file picker, so no storage permission. Android's own backup carries only your steps, profile and settings |

## Install

Android 14 (API 34) or newer, with a hardware step counter.

1. Download `passo-vX.Y.Z.apk` from the [latest release](https://github.com/fiorenzobrioni/passo/releases/latest).
2. Open it on the phone and allow installs from that source when Android asks.
3. On the first run, grant the "Physical activity" permission: counting starts there.

**Verify the download.** Put the APK and its `.sha256` file in one folder and run
`sha256sum -c passo-vX.Y.Z.apk.sha256`. To check that the APK is genuine, compare its signing
certificate (`apksigner verify --print-certs`, or AppVerifier on the phone) with this SHA-256
fingerprint:

```
8B:40:22:8A:8D:EF:E3:E3:F1:6E:FE:1A:DC:C0:4C:C7:F5:B5:82:E4:18:F0:15:E8:27:B9:59:D5:BF:39:7F:B5
```

**Updates.** Passo has no network access, so it cannot check for updates. Use GitHub's
"Watch, Custom, Releases" notifications, or [Obtainium](https://github.com/ImranR98/Obtainium).
Every release installs over the previous one and keeps your data. The notes of each version
are in [CHANGELOG.md](./CHANGELOG.md).

## Roadmap

- **A faster start**, with Baseline Profiles.
- **Google Play**, possibly, signed with the same key as the GitHub releases.

The phased plan, with every decision and its reason, is in [PLANNING.md](./PLANNING.md).

## Build

Requires JDK 21 (the one bundled with Android Studio works) and the Android SDK.

```bash
./gradlew :app:assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
./gradlew test                      # unit tests, every module
./gradlew :app:lintDebug            # lint, every module
./gradlew spotlessApply             # format the code
./gradlew :app:checkForbiddenPermissions
```

For an installable minified build to test with:
`./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`. It is signed with the debug key
committed in `keystore/`, on purpose, so builds from CI and any machine share one signature.
Debug builds carry `applicationIdSuffix ".debug"` and install side by side with the release.

CI runs formatting, the permission check, the tests and lint **before** building the APKs,
so a red suite never produces an installable artifact. A `vX.Y.Z` tag runs the same gates,
then publishes the signed APK, its checksum and the R8 mapping, with the matching
[CHANGELOG.md](./CHANGELOG.md) section as the release notes.

README screenshots: `./gradlew test -PupdateScreenshots`.

## Tech stack

- **Kotlin** 2.4, **Jetpack Compose** with Material 3, Gradle 9.8 and AGP 9.4, minSdk **34**
  (Android 14), target and compile SDK **37**
- **Room** (steps, outings), **DataStore** (settings, profile), **Hilt**, **Coroutines** and **Flow**
- A foreground service of type `health` for the step counter, **Glance** for the widgets
- **Navigation 3**, and charts drawn on a Compose canvas rather than by a charting library
- Unit tests on the JVM for every edge case of the tracking engine; Compose UI tests with
  Robolectric, accessibility checks included

```text
Compose UI → ViewModel → :core:data (Room, DataStore) ← :core:tracking (step counter service)
                      ↘ :core:domain (pure Kotlin: step accounting, metrics, walks, insights)
```

## Project structure

```text
passo/
├── app/                    # Application, MainActivity, navigation, DI
├── core/
│   ├── model/              # pure Kotlin data classes
│   ├── domain/             # pure Kotlin: step accounting, metrics, walks, insights, ways, backup
│   ├── data/               # Room, DataStore, repositories
│   ├── tracking/           # the foreground service, the sensor, receivers, notification
│   ├── designsystem/       # theme, components, charts
│   └── testing/            # shared UI test helpers (accessibility, page walks)
├── feature/                # today, history, insights, sessions, ways, settings, onboarding, guide
├── widget/                 # the two Glance widgets and their settings
├── build-logic/            # convention plugins, the forbidden-permission check
├── tools/                  # the launcher icon script, the ways' and walks' data script
└── keystore/               # the shared debug key (deliberately committed)
```

`:core:model` and `:core:domain` are pure Kotlin: a class in them that needs a `Context` is in
the wrong module.

## Project documentation

| File | Contents |
|---|---|
| [VISION.md](./VISION.md) | the product: principles, scope, non-goals, key decisions |
| [PLANNING.md](./PLANNING.md) | architecture, tracking engine, data model, metrics, the phased plan and its decisions |
| [docs/adr/](./docs/adr/) | the architecture decision records |
| [CHANGELOG.md](./CHANGELOG.md) | what shipped, per version; a section is written before its tag |
| [CLAUDE.md](./CLAUDE.md) | the operating rules for AI-assisted development in this repo |

## The family

Passo is one of three focused apps with the same look and the same rules:
[Chiaro](https://github.com/fiorenzobrioni/chiaro) (weather) and
[Saldo](https://github.com/fiorenzobrioni/saldo) (personal finance).

## License

[GPL-3.0](./LICENSE) © 2026 Fiorenzo Brioni

[Google Sans](https://fonts.google.com/specimen/Google+Sans) and
[Inter](https://github.com/rsms/inter) under the SIL Open Font License 1.1. Full attributions
in [licenses/](./licenses/).

The ways and the city walks are drawn from [OpenStreetMap](https://www.openstreetmap.org/copyright)
data, © OpenStreetMap contributors, under the Open Database License (the walks routed with
[BRouter](https://brouter.de), the cities' water and parks from the same map); the land and
water behind the ways and the continents' maps from [Natural Earth](https://www.naturalearthdata.com), public domain.
