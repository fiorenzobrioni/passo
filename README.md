<div align="center">

# 👣 Passo

**Every step, counted. On your phone, and nowhere else.**

A private, battery-friendly Android pedometer that counts every step, even if you never open the app.
Free, no account, no ads, no tracking, and no permission to use the internet at all.

![Platform](https://img.shields.io/badge/platform-Android-2E6B3E?labelColor=FCFAF6)
![CI](https://img.shields.io/github/actions/workflow/status/fiorenzobrioni/passo/android-ci.yml?branch=main&label=CI&labelColor=FCFAF6&color=2E6B3E)
![License](https://img.shields.io/badge/license-GPL--3.0-007DB6?labelColor=FCFAF6)
![minSdk](https://img.shields.io/badge/minSdk-34-70569C?labelColor=FCFAF6)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-F1A000?labelColor=FCFAF6)
![Compose](https://img.shields.io/badge/UI-Compose%20Material%203-007DB6?labelColor=FCFAF6)
![Internet](https://img.shields.io/badge/INTERNET%20permission-none-2E6B3E?labelColor=FCFAF6)

</div>

> [!NOTE]
> Passo is in early development: Phases 0 to 4 of [the plan](./PLANNING.md) are built (the
> step tracking engine, the metrics, the Today screen, the first run, Settings and the two
> home-screen widgets). It has run on a phone, but the multi-day field test of the tracking
> engine is still to do, and there is no usable release yet.

## Screenshots

<table>
  <tr>
    <td align="center"><img src="docs/screenshots/today.png" width="250" alt="Today: 7,855 of 10,000 steps, 1,484 ahead of the usual pace, with the day's chart"></td>
    <td align="center"><img src="docs/screenshots/today-chart.png" width="250" alt="The day's chart read with a finger at 12:55 PM"></td>
    <td align="center"><img src="docs/screenshots/today-goal-dark.png" width="250" alt="Today in the dark theme: 10,772 steps, goal reached at 5:51 PM"></td>
  </tr>
  <tr>
    <td align="center"><b>Today.</b> The ring, with a notch where a usual Thursday stands by now, and one sentence on how the day is going.</td>
    <td align="center"><b>Your day.</b> Touch the chart to read it at any minute, against a usual day.</td>
    <td align="center"><b>Goal reached,</b> in the dark theme.</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/onboarding-welcome.png" width="250" alt="The first run: welcome"></td>
    <td align="center"><img src="docs/screenshots/onboarding-goal.png" width="250" alt="The first run: the daily goal, with what it means in distance and time"></td>
    <td align="center"><img src="docs/screenshots/settings-appearance.png" width="250" alt="Settings: appearance, with a live preview"></td>
  </tr>
  <tr>
    <td align="center"><b>First run.</b> Under a minute from install to counting.</td>
    <td align="center"><b>A goal</b> that says what it means in kilometres and minutes.</td>
    <td align="center"><b>Settings,</b> with the same palettes and typefaces as Chiaro.</td>
  </tr>
  <tr>
    <td align="center"><img src="docs/screenshots/widgets.png" width="250" alt="The two widgets on a home screen: At a glance with its ring, In words, a terracotta pair side by side, and the day hour by hour"></td>
    <td align="center"><img src="docs/screenshots/widget-settings.png" width="250" alt="One widget's settings: the card as it will look, its sizes, the background colour, the opacity and the content"></td>
    <td></td>
  </tr>
  <tr>
    <td align="center"><b>Two widgets,</b> At a glance and In words, in the same dress as Chiaro's.</td>
    <td align="center"><b>Each widget its own look:</b> Chiaro's six colours, any opacity, what it shows.</td>
    <td></td>
  </tr>
</table>

Drawn from the app's own screens with realistic sample days (the phone's status bar is not in
the picture). They are regenerated with `./gradlew test -PupdateScreenshots`.

## What Passo is

Most step counters are either a big health suite (accounts, cloud sync, sleep, food, heart
rate, a long list of permissions) or a thin front end that reads steps from another app and
breaks when that app changes. Passo does one thing. It counts steps with the phone's own
low-power hardware step counter, keeps the data on the device, and turns those steps into
useful numbers: distance, active calories, active and brisk minutes, goals, streaks, records,
and the walks you took, recognized on their own.

It keeps counting across reboots and full shutdowns without you ever opening it, and two
resizable home-screen widgets answer "how am I doing today?" at a glance.

## What it does now

- **Today**: steps and a progress ring toward your daily goal, with a notch where a usual day
  of the same weekday stands at this hour; one sentence on how the day is going and what is
  left to walk; a chart of the day you can read with a finger; distance, active calories,
  active and brisk minutes and average cadence, each with a line that says what it means. It
  counts live while it is open.
- **A short first run**: welcome, an optional profile, a goal, the one permission it needs,
  and a battery tip on the phones whose battery manager stops background apps.
- **Settings**: height, weight and step length, the goal, metric or imperial units, theme,
  palette and typeface, language, and a pause.
- **Two home-screen widgets**, dressed like Chiaro's so the two apps sit side by side:
  **At a glance** (today's ring, the count, the day's sentence and, on a wide tall card, the
  day hour by hour) and **In words** (the same day in type alone: the count large, the
  sentence, the goal, distance and calories, and on a tall card the day in figures). Both go
  from one cell to as large as your launcher allows and lay themselves out for every size.
  Each one can be light, dark, follow the phone or wear one of six colours, at any opacity. A
  paused count says so, and a tap resumes it.

## What is coming (v1.0)

- **Walks, found for you**: continuous stretches of walking or running are recognized from the
  minute-by-minute steps ("10:12 to 10:47, 3,420 steps, 2.6 km, 98 spm"), with no extra
  sensor and no background work.
- **History and insights**: the day hour by hour, week, month and year charts with the goal
  line, a calendar of goal days, streaks, personal records and lifetime distance.
- **Goals**: an optional "goal reached" notification, an evening nudge, a weekly summary.
- **Everywhere else**: an ongoing notification with today's steps, a Quick Settings tile,
  export to CSV and JSON, import from a backup.

Distance and calories are **estimates**, and Passo says so. The formulas are documented and
you can tune them: height, weight, your own step length, measured with a short calibration
walk.

## Private by design

- **No `INTERNET` permission.** Nothing can leave the phone, except through an export you
  start yourself or Android's own backup. The build fails if a network, location,
  exact-alarm or body-sensor permission ever appears in the app, from any library.
- **No account, no analytics, no ads, no crash reporters.**
- **No other health app needed.** No Health Connect, Google Fit or Samsung Health, no Google
  Play services: the phone's step counter is the only source.

## Easy on the battery

The step counter runs on a low-power chip that collects steps while the phone sleeps and hands
them over in batches. Passo keeps a small foreground service alive (Android requires it to
receive sensor data and to save the count before a shutdown), but it does not wake the phone
on a timer, and it updates the widgets and the notification only while the screen is on: the
widgets are told when something changed, at most once a minute, and never poll. The target is
at most about 1% of a day's battery.

## Building

Requirements: JDK 21 (the one bundled with Android Studio works) and the Android SDK.

```sh
./gradlew :app:assembleDebug        # debug APK in app/build/outputs/apk/debug/
./gradlew test                      # unit tests, every module
./gradlew :app:lintDebug            # lint, every module
./gradlew spotlessApply             # format the code
./gradlew :app:checkForbiddenPermissions
```

The architecture, the tracking engine and the phased plan are in [PLANNING.md](./PLANNING.md);
the product is described in [VISION.md](./VISION.md).

## Installing and verifying

Passo will be published as signed APKs on
[GitHub Releases](https://github.com/fiorenzobrioni/passo/releases), each with its SHA-256
checksum. The app has no network access, so it cannot check for updates itself: use GitHub's
"Watch, Custom, Releases" notifications, or [Obtainium](https://github.com/ImranR98/Obtainium).

Until the final signing key exists, releases are signed with a **temporary key** and marked as
pre-releases. A release signed with the final key will not install over one of them: uninstall
first. The signing certificate's SHA-256 fingerprint will be published here with the first
real release.

## The family

Passo is one of a small set of focused, single-purpose apps with the same look and the same
rules: [Chiaro](https://github.com/fiorenzobrioni/chiaro) (weather) and Saldo (personal finance).

## License

[GPL-3.0](./LICENSE) © 2026 Fiorenzo Brioni
