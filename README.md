<div align="center">

# 👣 Passo

**Every step, counted. On your phone, and nowhere else.**

A private, battery-friendly Android pedometer that counts every step, even if you never open the app.
Free, no account, no ads, no tracking, and no network permission at all.

![Platform](https://img.shields.io/badge/platform-Android-2E6B3E?labelColor=FCFAF6)
![CI](https://img.shields.io/github/actions/workflow/status/fiorenzobrioni/passo/android-ci.yml?branch=main&label=CI&labelColor=FCFAF6&color=2E6B3E)
![License](https://img.shields.io/badge/license-GPL--3.0-007DB6?labelColor=FCFAF6)
![minSdk](https://img.shields.io/badge/minSdk-34-70569C?labelColor=FCFAF6)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-F1A000?labelColor=FCFAF6)
![Compose](https://img.shields.io/badge/UI-Compose%20Material%203-007DB6?labelColor=FCFAF6)
![Internet](https://img.shields.io/badge/INTERNET%20permission-none-2E6B3E?labelColor=FCFAF6)

</div>

> [!NOTE]
> Passo is in early development (Phase 0 of [the plan](./PLANNING.md): the project skeleton).
> There is no usable release yet.

## What Passo is

Most step counters are either a big health suite (accounts, cloud sync, sleep, food, heart
rate, a long list of permissions) or a thin front end that reads steps from another app and
breaks when that app changes. Passo does one thing. It counts steps with the phone's own
low-power hardware step counter, keeps the data on the device, and turns those steps into
useful numbers: distance, active calories, active and brisk minutes, goals, streaks, records,
and the walks you took, recognized on their own.

It keeps counting across reboots and full shutdowns without you ever opening it, and a
resizable home-screen widget answers "how am I doing today?" at a glance.

## What it will do (v1.0)

- **Today**: steps and a progress ring toward your daily goal, the day's distance, active
  calories, active and brisk minutes, average cadence, and a small line of the day so far
  against your typical day for that weekday ("+1,240 vs usual").
- **Walks, found for you**: continuous stretches of walking or running are recognized from the
  minute-by-minute steps ("10:12 to 10:47, 3,420 steps, 2.6 km, 98 spm"), with no extra
  sensor and no background work.
- **History and insights**: the day hour by hour, week, month and year charts with the goal
  line, a calendar of goal days, streaks, personal records and lifetime distance.
- **Goals**: an optional "goal reached" notification, an evening nudge, a weekly summary.
- **Widget**: resizable from 1x1 to 4x2, with today's steps by hour at the largest size, in
  your Material You colors.
- **Everywhere else**: an ongoing notification with today's steps, a Quick Settings tile,
  export to CSV and JSON, import from a backup, metric and imperial units, English and Italian.

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
on a timer, holds no wakelocks, and updates the widget and the notification only while the
screen is on. The target is at most about 1% of a day's battery.

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
