# Passo — Product Vision

> **Passo** (Italian for "step") is part of a small family of focused, single-purpose apps, alongside **Chiaro** (weather) and **Saldo** (personal finance).
> Store title: "Passo – Contapassi" / "Passo – Step Counter". Repository: `fiorenzobrioni/passo`.
> Package name: `com.callbackdev.passo`, the `callbackdev` namespace shared with the sibling apps (decided in Phase 0, see PLANNING §15).

## One-liner

A private, battery-friendly Android pedometer that counts every step, even if you never open the app.
It turns those steps into useful numbers: distance, calories, active time, goals and trends.
A resizable home-screen widget keeps the numbers visible at a glance.

## Why this app

Most step-counting apps fall into one of two groups:

- **Big health suites** that also track sleep, food, heart rate and workouts. They add accounts, cloud sync and a lot of permissions.
- **Thin front-ends** that read steps from another app (Samsung Health, Google Fit, Health Connect). They break or drift when that app changes.

Passo does one thing and does it well. It counts steps with the phone's own hardware sensor, keeps the data on the device, and derives everything else from those steps.

## Product principles

1. **Steps are the core.** Every feature must come from step data plus the user's profile (height, weight, step length). If a feature needs another data source, it's out of scope.
2. **Autonomous.** No dependency on other health apps, Health Connect, Google Fit or Google Play services for step data. The hardware step counter sensor is the only source.
3. **Private by design.**
   - No `INTERNET` permission, no location, no account, no analytics, no ads SDKs.
   - Data never leaves the device, except through a user-initiated export or Android's own system backup.
4. **Battery first.**
   - Use the low-power hardware step counter with sensor batching.
   - Wake the CPU only when there is a reason to.
   - Update the UI, notification and widget only when someone can see them.
5. **No lost steps.** Counting continues across reboots and full shutdowns without the user ever opening the app.
6. **Glanceable.** The widget and the ongoing notification answer "how am I doing today?" without opening the app.
7. **Honest estimates.**
   - Distance, calories and active time are shown as estimates.
   - Formulas are documented and the user can tune them (step length calibration, weight, units).

## Target users

- People who want a simple daily step goal and a clear view of their progress.
- Privacy-conscious users who don't want a health platform, an account or cloud sync.
- Users with habits that break naive step counters, for example switching the phone off every night.

## Scope for v1.0

### Tracking

- Continuous step counting with the hardware `TYPE_STEP_COUNTER` sensor, run by a `health`-type foreground service.
- Automatic start at boot and after app updates, with no need to open the app.
- Correct handling of reboots, shutdowns, sensor resets, midnight rollover and time-zone changes.
- A tracking-status indicator for states such as permission revoked, service stopped or sensor missing, with one-tap recovery.

### Today

- Today's steps with a progress ring toward the daily goal.
- Derived metrics: distance, active calories, active minutes, brisk minutes, average cadence.
- **Day trend sparkline:** a compact line of cumulative steps since midnight, with two references:
  - the daily goal line;
  - a dashed "typical day" line, the average cumulative curve of the same weekday over the last 4 weeks, shown once at least 2 such days exist.
  - At a glance it answers "am I ahead or behind my usual pace?", not just "how many steps so far?".
- Live updates while the app is visible.

### Derived metrics (estimates)

- **Distance:** steps × step length.
  - The step length is either derived from height, set manually, or calibrated.
  - A separate running step length is used for high-cadence minutes.
- **Active calories:** net energy from walking and running, based on distance, body weight and cadence.
- **Active minutes:** minutes with sustained stepping.
- **Brisk minutes:** minutes at moderate-intensity cadence, 100 or more steps per minute.
- Metric and imperial units.

### Goals and motivation

- A configurable daily step goal. The goal in effect each day is stored with that day, so history stays correct.
- Streaks of consecutive days with the goal met: current and longest.
- Personal records: best day, best week, best month, longest streak.
- A "goal reached" notification (opt-in, once per day).
- An optional evening reminder when today's progress is below a threshold.
- An optional weekly summary notification.

### History and insights

- Day detail with the hourly bar chart and the list of walks detected that day, marked on the chart.
- Week, month and year charts, with the goal line shown.
- A calendar heatmap of goal completion.
- Averages, totals and lifetime distance.

### Automatic walk detection

- Continuous stretches of walking or running are recognized automatically from the minute-by-minute step data, for example "10:12–10:47 · 3,420 steps · 2.6 km · 98 spm".
- Each walk shows start and end, duration, steps, distance, active calories, average cadence, and whether it was a walk or a run.
- Separates intentional walks from scattered steps around the house or office.
- **Zero battery cost:** walks are computed from data already stored, only when a screen that shows them is opened. No extra sensor, no background work, no Google Activity Recognition API.
- **Not real-time:** there is no "walk finished" notification, because detecting the end of a walk live would mean waking the CPU with the screen off.
- Settings: on/off toggle (to keep the UI minimal if the user prefers) and minimum walk duration (5, 10 or 15 minutes).

### Widget (Jetpack Glance)

- Resizable from 1x1 to 4x2, with a layout that adapts to the actual size.
- The 4x2 size includes a mini chart of today's steps by hour, built without images.
- Material 3 dynamic colors, light and dark.
- Updates quickly when the screen turns on, is throttled while the screen is on, and never updates while the screen is off.
- A clear "tracking paused" state with tap-to-resume.

### System surfaces

- An ongoing, low-importance notification required by the foreground service. It shows today's steps and goal progress.
- A Quick Settings tile showing today's steps. It refreshes only while the Quick Settings panel is open.

### Settings and personalization

- Profile, all fields optional with sensible defaults: height, weight, sex (used only for default estimates), walking step length, running step length.
- A step length calibration wizard: walk a known distance and the app computes the step length, without GPS.
- Units, first day of the week, theme (system, light or dark), dynamic color on or off.
- App language, Italian or English, through Android's per-app language setting.
- Walk detection on or off, and minimum walk duration.
- Show or hide the "typical day" line on the Today sparkline.
- A pause or resume tracking toggle.

### Data

- Export to CSV and JSON through the Storage Access Framework, so no storage permission is needed.
- Import from a JSON backup.
- Android Auto Backup and device-to-device transfer, configured with explicit rules.

### Onboarding

- A short flow: welcome, optional profile, daily goal, permissions, and battery tips specific to the phone manufacturer when relevant.

## Out of scope (non-goals)

- Sleep, food, water, heart rate, weight tracking over time.
- Workouts or exercise sessions started manually (start/stop, workout types, live workout screens). Walks detected automatically from step data are in scope; manual workout tracking is not.
- GPS, routes, maps, or anything that needs location permissions.
- Reading from or writing to Health Connect, Samsung Health, Google Fit, or any other health app in v1.
- Accounts, cloud sync, social features, leaderboards.
- Floors climbed, which would need a barometer and isn't derived from steps.
- Wear OS in v1.
- Ads, analytics, crash-reporting SDKs that need network access.

### Possible post-1.0 ideas (not committed)

- Optional, opt-in, write-only export to Health Connect. It would never be required and never used as a data source.
- "Virtual journeys": lifetime distance compared with well-known routes, for example Milan to Rome.
- Achievements and badges based on step milestones.
- Manual corrections for a day's total.
- A Wear OS tile.
- Publication on Google Play (see Key decisions: Distribution).

## Key decisions

| Area | Decision | Rationale |
|---|---|---|
| Step source | Hardware `Sensor.TYPE_STEP_COUNTER` | Runs on a low-power coprocessor and is cumulative since boot. The accelerometer would drain the battery, and the Google Play services Recording API or Health Connect would add external dependencies. |
| Background execution | A foreground service of type `health`, with a persistent low-importance notification | Since Android 9, background apps don't receive sensor events. Since Android 9, `ACTION_SHUTDOWN` reaches only receivers registered at runtime, so a live process is needed to save the last count before shutdown. Continuous fitness tracking is exactly the documented use case for the `health` type. |
| Reporting | Non-wake-up sensor with a large `maxReportLatency` while the screen is off, and low latency while the screen is on | The sensor hub buffers events in its FIFO. Each event keeps its own timestamp, so steps land in the correct minute and day even when they are delivered late. |
| Platform | `minSdk 34` (Android 14), `targetSdk`/`compileSdk 37` (Android 17) | Foreground service types, the `health` type and per-app languages are all available natively, so no legacy code paths are needed. |
| UI | Jetpack Compose + Material 3; Jetpack Glance for the widget | Modern and declarative, with one design system shared by the app and the widget. |
| Charts | Custom Compose `Canvas` components, no third-party chart library | The app needs only a few chart types (sparkline, bars, heatmap). Owning them keeps dependencies minimal and gives full control over style and accessibility. |
| Languages | Italian and English for the app; English for code and documentation | |
| Name | **Passo** (final) | Consistent with the sibling apps Chiaro and Saldo. |
| License | GPL-3.0 | Keeps the app and its derivatives open. All planned dependencies (AndroidX, Kotlin, Hilt) are Apache-2.0, which is compatible. |
| Distribution | Signed APKs on GitHub Releases. Google Play kept open as a later option. No F-Droid. | GitHub Releases needs no store review and fits an app without network access. The release signing key is kept safe from day one so that a future Play listing can reuse it (see PLANNING §11, Phase 9). |

## Success criteria

- **No lost steps on graceful shutdown.** After a full power-off and power-on cycle, the recorded daily total matches the raw sensor deltas, including the steps taken just before shutdown.
- **Hands-off.** After a reboot or an app update, tracking resumes without the user opening the app.
- **Battery.** The app's share of daily battery use is at most about 1% on reference devices with normal use. This is a target, checked with `batterystats` and Battery Historian. There are no app wakelocks or alarms while the screen is off.
- **Widget freshness.** The widget shows the current count within about 5 seconds of the screen turning on, and is never more than about 60 seconds stale while the screen is on.
- **Privacy is enforced.** The merged release manifest contains no `INTERNET` and no location permissions, and CI fails if one appears.

## Glossary

- **Step:** one foot strike, as counted by the sensor.
- **Step length:** the distance covered by one step. This is what the app stores and uses for distance.
  - In everyday Italian, "falcata" is often used for this.
  - Strictly speaking, a **stride** is two steps (left plus right). The UI should make this clear to avoid off-by-two errors.
- **Cadence:** steps per minute (spm).
- **Active minute:** a minute with at least the active threshold of steps. The default is 40, defined as a constant.
- **Brisk minute:** a minute with 100 or more steps, a widely used cadence marker for moderate-intensity walking.
- **Boot session:** the period between two device boots. The hardware counter restarts from 0 at each boot.
- **Walk:** a stretch of consecutive minutes with sustained stepping, detected automatically from step data. Called a "run" when the average cadence is at or above the running threshold.
- **Typical day:** the average cumulative step curve for the same weekday over the last 4 weeks, used as a reference on the Today sparkline.
