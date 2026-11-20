# Passo — Product Vision

> **Passo** (Italian for "step") is part of a small family of focused, single-purpose apps, alongside **Chiaro** (weather), **Tempo** (clock and agenda), **Saldo** (personal finance) and **Tetto** (budget).
> Store title: "Passo – Contapassi" / "Passo – Step Counter". Repository: `fiorenzobrioni/passo`.
> Package name: `com.callbackdev.passo`, the `callbackdev` namespace shared with the sibling apps (decided in Phase 0, see PLANNING §15).

## One-liner

A private, battery-friendly Android pedometer that counts every step, even if you never open the app.
It turns those steps into useful numbers: distance, calories, active time, goals and trends,
and into walks with a goal, started on purpose, that tell you on the way.
Two resizable home-screen widgets keep the numbers visible at a glance.

## Why this app

Most step-counting apps fall into one of two groups:

- **Big health suites** that also track sleep, food, heart rate and workouts. They add accounts, cloud sync and a lot of permissions.
- **Thin front-ends** that read steps from another app (Samsung Health, Google Fit, Health Connect). They break or drift when that app changes.

Passo does one thing and does it well. It counts steps with the phone's own hardware sensor, keeps the data on the device, and derives everything else from those steps.

## Product principles

1. **Steps are the core.** Every feature must come from step data plus the user's profile (height, weight, step length). If a feature needs another data source, it's out of scope.
   - *Amended 18 Nov 2026 (`docs/adr/0016-location.md`):* location, when the reader asks for it, measures what the steps estimate, in an outing that asked for it. It never replaces the steps (the day's count, History, Insights and the ways from home are computed from them as before), and nothing in the app depends on it.
2. **Autonomous.** No dependency on other health apps, Health Connect, Google Fit or Google Play services for step data. The hardware step counter sensor is the only source.
3. **Private by design.**
   - No `INTERNET` permission, no account, no analytics, no ads SDKs: Passo sends nothing, and cannot.
   - Location is a measuring tape, never a tracker (amended 18 Nov 2026, `docs/adr/0016-location.md`): read only inside an outing the reader started with «Measure with GPS» on, from its start to its pause or end. Never for the day's count, never with no outing, never in the background; satellites only, with no Google Play services. Asked in context, the first time such an outing starts; a reader who never turns it on is asked for nothing. The permissions the app may hold are an allowlist, and the build fails on any other.
   - Data never leaves the device, except through a user-initiated export or Android's own system backup. The routes of the outings measured with GPS are never in Android's cloud copy: only in a phone-to-phone transfer.
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
  - The step length is either derived from height, set manually, calibrated, or learnt pace by
    pace from the outings with GPS and accepted by the reader (Phase 17).
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
- **Not real-time:** there is no "walk finished" notification, because detecting the end of a walk live would mean waking the CPU with the screen off. The live case is an outing, which the reader starts on purpose (below).
- Settings: on/off toggle (to keep the UI minimal if the user prefers) and minimum walk duration (5, 10 or 15 minutes).

### Outings (walks with a goal)

Added to v1.0 at the owner's request (Phase 10, `docs/adr/0009-sessions.md`). An outing is a walk (or a run) started on purpose, with a goal, that tells the reader on the way; it is measured from the steps like everything else.

- **One goal, one pace.** The goal is one quantity: steps, a distance (estimated), minutes in motion, or "the rest of the day" (today's goal less today's count). The pace is optional: free, brisk (100 spm), vigorous (130 spm) or running (140 spm). Never two quantities at once, never a calorie target, and never a pace in minutes per kilometre as a goal: without GPS it would be the cadence in disguise, and with it (below) it is a measure the outing shows, not one it is walked against.
- **Signals the reader chooses:** at 25, 50 and 75% (50% by default), and always at the goal, which ends the outing. Each vibrates in its own count (one, two, three short pulses, one long one at the goal), so a phone in a pocket is read without looking; the editor lets you feel them first. They follow the phone's silent mode and the outings' notification channel.
- **The counting notification becomes the outing's** while it lasts, collapsed and expanded, with Pause and Stop; on Android 16 it is a Live Update with the milestones on its bar. The goal reached is a notification of its own, with "Keep going".
- **Time in motion, from the steps:** a stop at a traffic light does not count. The cadence now is said against the outing's own ("on pace", "below your pace").
- **Where it shows:** Today (a button to start one, which the reader can take away in Settings, then its card), the Outings page (from Today or from Settings) with the reader's outings (three to start with: a brisk 20 minutes, a 30-minute run, the rest of the day), the launcher's long press, the evening reminder's "Walk now", both widgets and the Quick Settings tile while one is under way, and History and Today's list of the day's walks, in the place of the walk it was, with its goal and how much of it was done.
- **Battery:** only while an outing is counting, the phone's wake-up step counter reports within 30 seconds, about two brief wakes a minute while walking and none while still, and within 2 seconds in the 40 seconds before each change of an interval walk (`docs/adr/0013-interval-walks.md`). On a phone whose step counter cannot wake it, an outing with signals keeps the processor awake while it counts instead, said in the editor (about 1 to 3% of a battery an hour). Everything else is as before. It ends by itself at its goal, after 15 minutes without a step, or after an hour paused.
- **No outing ends a walk unasked** (Phase 15, `docs/adr/0016-location.md` decision 14): four hours is the longest an outing stays open (the time paused not counted), so that one forgotten by a reader who keeps walking does not run all day. Fifteen minutes before, it asks, «Still walking?», by a vibration, a notification counting down to the end, and its card; «Keep going» moves the end four hours on. Unanswered, it ends there, says so, and can be taken back for fifteen minutes.
- **Measured with GPS, if you ask** (Phase 15, `docs/adr/0016-location.md`; built on 18 Nov 2026, released with Phase 16): a switch in an outing's editor, off unless the reader turns it on. The outing then reads the satellites while it counts, and its distance is measured rather than estimated; it shows its pace by the kilometre or the mile, and keeps its route. The steps and the fixes are fused: the steps say when the reader walks, the fixes how far, so a stop, a bus or a tunnel does not fool it, and the receiver rests two minutes after the reader stops. «Measured» is said only of a distance at least nine tenths from fixes; under that the outing says how much of it the steps gave. The day's distance, History, Insights and the ways from home stay the steps'. Without the permission, or with location off, the outing counts from the steps and its card says so once.
- **A voice, if you want it** (off by default, per outing; `docs/adr/0010-voice.md`): the start, each signal with what is left and your pace, and the goal, spoken through headphones, or out loud too when the phone is not silenced. The system's engine with a voice installed on the phone, never one from the network; the music dims for a moment.

### Widgets (Jetpack Glance)

- Two widgets, in the dress of Chiaro's pair so the family's cards sit side by side as one (decided in Phase 4, `docs/adr/0005-widgets.md`):
  - **At a glance** («A colpo d'occhio»): today's ring (with the usual-day notch), the count, the goal and the day's sentence; on a wide tall card, today's steps hour by hour in 24 bars, built without images.
  - **In words** («In parole»): the same day in type alone, as Chiaro's: the count large, the sentence, the goal, distance and calories, and on a tall card the day in figures.
- The same sizes for both: from one cell to as large as the launcher allows, 4x1 by default, with a layout for every size.
- Per widget: a light, dark or phone-following card, or one of the family's seven colours (the app's green by default), any opacity, and which content it carries.
- Updates quickly when the screen turns on, is throttled while the screen is on, and never updates while the screen is off.
- A clear "tracking paused" state with tap-to-resume, and the same for a missing permission or a stopped service.
- While an outing is under way, the sentence's place says it («Brisk walk: 12 of 20 min», or paused), on both cards.

### System surfaces

- An ongoing, low-importance notification required by the foreground service. It shows today's steps and goal progress, or the outing under way.
- A Quick Settings tile showing today's steps (and the outing under way). It refreshes only while the Quick Settings panel is open.

### Settings and personalization

- Profile, all fields optional with sensible defaults: height, weight, sex (used only for default estimates), walking step length, running step length.
- A step length calibration wizard: walk a known distance and the app computes the step length, without GPS.
- Units, first day of the week, theme (system, light or dark), dynamic color on or off.
- App language, Italian or English, through Android's per-app language setting.
- Walk detection on or off, and minimum walk duration.
- Show or hide the "typical day" line on the Today sparkline.
- Show or hide Today's "Start an outing" button; the Outings page stays one row away in Settings.
- The guide, as in Chiaro: a tour of the three screens and the outings, what each one answers and what a screen cannot say out loud, illustrated with the app's own components. From the top of Settings, and from Today's first-day card.
- A pause or resume tracking toggle.

### Data

- Export to CSV and JSON through the Storage Access Framework, so no storage permission is needed.
- Import from a JSON backup.
- Android Auto Backup and device-to-device transfer, configured with explicit rules.

### Onboarding

- A short flow: welcome, optional profile, daily goal, permissions, and battery tips specific to the phone manufacturer when relevant.

## Out of scope (non-goals)

- Sleep, food, water, heart rate, weight tracking over time.
- A workout suite: a catalogue of sports, cycling, training plans, calorie or pace-per-kilometre targets, several goals at once, segments, rankings, anyone else's routes. Outings (one goal of steps, distance or time, with an optional cadence, measured from the steps and, when the reader asks, with GPS) are in scope since Phase 10; the rest of workout tracking is not.
- Location for anything but an outing that asks for it (amended 18 Nov 2026, `docs/adr/0016-location.md`, decisions 1 and 16): no tracking of the day, no location in the background, no geofence or reminder tied to a place, no outing that starts by itself, no live sharing, no map tiles, no navigation to a start or turn by turn.
- Reading from or writing to Health Connect, Samsung Health, Google Fit, or any other health app in v1.
- Accounts, cloud sync, social features, leaderboards.
- Floors climbed, which would need a barometer and isn't derived from steps.
- Wear OS in v1.
- Ads, analytics, crash-reporting SDKs that need network access.

### Planned after 1.0

Asked for by the owner after the v1.0.0 release (1 Oct 2026); the phases are in PLANNING §11, in an order the owner chooses. All five were built, and are in 1.1.0 (2 Nov 2026), with the last seven days on «At a glance» (an option the widget's plan had left open).

- **The Ways** («I Cammini», Phase 11): the "virtual journeys" idea, as five long ways (Via degli Dei, the Camino Portugués from Porto, Via di Francesco, the Camino Francés, and the Italian part of the Via Francigena). The estimated distance walked since a chosen day moves a point along the way, drawn without any location; each stage reached is a stamp.
- **City walks** («Passeggiate in città», Phase 11, second part): an outing through a city (Milan, Rome, Paris, London, Madrid, Porto, Amsterdam, Prague, Lima and Cusco; about 5 km or about 10), its places told as the reader reaches them, by a vibration and, if the outing speaks, the voice. Imaginary and said so: the route moves with the steps, never with a location.
- **Your year on foot** («Il tuo anno a piedi», Phase 12): a year told in full-screen pages, from the days already stored, reachable by hand at any time and offered in December; a page can be shared as an image, through an app the reader picks.
- **The interval walk** («Camminata a intervalli», Phase 13): the Japanese Interval Walking Training as an outing, in minutes, with each change of interval felt in the pocket (`docs/adr/0013-interval-walks.md`).
- **More ways, beyond Europe** («Altri cammini», Phase 14, planned): the Kumano Kodō, the Inca Trail, the Great Ocean Walk, the John Muir Trail and Kilimanjaro, so that the ways reach every continent the cities do; the words that describe the feature as a whole name no faith, and the ways' stamps are grouped as «Timbri» / "Stamps", on a page that lists the ways by continent.

### After 1.1: location, for an outing that asks for it

Asked for and approved by the owner on 18 Nov 2026, after weighing it a second time
(`docs/adr/0016-location.md`; the phases are in PLANNING §11, 15 to 20). **Phase 15 is built**
(18 Nov 2026; not released: it goes out with Phase 16 or not at all, and its proof on a phone
comes first), and with it the principles, the non-goals and the success criteria above were
amended, each where it changed. Phase 16 is built too, waiting for the same proof; Phases 17,
18 and 20 are planned, and Phase 19 was dropped with the first Phase 18 (owner, 19 Nov 2026).

Location enters as a measuring tape, never as a tracker: it is read only inside an outing the
reader started with «Measure with GPS» on. A reader who never turns it on is asked for nothing,
and the app is exactly as it was. Passo still holds no `INTERNET` permission, so it cannot
send a route anywhere; it never reads location in the background, and routes are left out of
Android's cloud backup.

- **The measured outing** («Uscita misurata», Phases 15 and 16): a distance measured rather
  than estimated, a pace per kilometre, the route drawn in the app's own hand, splits, climb,
  and a GPX file to take elsewhere. The steps and the satellites are fused: the steps say when
  the reader moves, the fixes how far, so a stop, a bus or a tunnel does not fool it.
- **The step that learns** («Il passo che impara», Phase 17): each outing with GPS says how
  long the reader's step is at each pace; Passo proposes the result, and every day without GPS
  reads truer for it. Built (19 Nov 2026): the reader accepts it or not, past days keep theirs
  unless asked, and the way back to the step used before is one touch.
- **City walks measured** («Passeggiate misurate», Phase 18): a city walk stays imaginary, but
  with «Measure with GPS» on its page its distance is measured on the reader's own streets and
  their real route is kept, so one walk gives the story and the measure. The city's map shows
  the reader's point on the walk by the distance walked, never their real position. A way from home stays as it is, with no location; Phase 17 makes
  its kilometres truer. «On the spot», a city walk or a way walked where it is, was weighed and
  dropped (owner, 19 Nov 2026): real value for few readers, a high cost for all
  (`docs/adr/0016-location.md`, decision 13, amended).
- **Your usual round** («Il tuo giro solito», Phase 20): a walk the reader repeats, known
  without being told among their own outings with GPS, and compared with their usual, honestly
  (the same direction, the time brought to the usual length): a sentence on the outing's page,
  a line at each kilometre while it counts, and the rounds listed with their walks.

Still out: a catalogue of sports, cycling, training plans, segments, rankings, friends, live
sharing, an outing that starts by itself, following a route or a GPX from elsewhere (weighed
and left out on 19 Nov 2026), and location in the background for any reason.

### Possible post-1.0 ideas (not committed)

- Optional, opt-in, write-only export to Health Connect. It would never be required and never used as a data source.
- Achievements and badges based on step milestones.
- Manual corrections for a day's total.
- A Wear OS tile.
- Publication on Google Play (see Key decisions: Distribution).

## Key decisions

| Area | Decision | Rationale |
|---|---|---|
| Step source | Hardware `Sensor.TYPE_STEP_COUNTER` | Runs on a low-power coprocessor and is cumulative since boot. The accelerometer would drain the battery, and the Google Play services Recording API or Health Connect would add external dependencies. |
| Background execution | A foreground service of type `health`, with a persistent low-importance notification | Since Android 9, background apps don't receive sensor events. Since Android 9, `ACTION_SHUTDOWN` reaches only receivers registered at runtime, so a live process is needed to save the last count before shutdown. Continuous fitness tracking is exactly the documented use case for the `health` type. |
| Reporting | Non-wake-up sensor with a large `maxReportLatency` while the screen is off, and low latency while the screen is on; the wake-up one with a 30 s latency only while an outing is counting (2 s in the 40 s before a change of an interval walk) | The sensor hub buffers events in its FIFO. Each event keeps its own timestamp, so steps land in the correct minute and day even when they are delivered late. An outing's signals must reach a phone in a pocket on time, at a cost bounded by the walk the reader chose (`docs/adr/0009-sessions.md`). |
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
- **Battery.** The app's share of daily battery use is at most about 1% on reference devices with normal use. This is a target, checked with `batterystats` and Battery Historian. There are no app wakelocks or alarms while the screen is off; the one exception is an outing the reader started, while it counts: the wake-up step counter's reports, every 30 s, and every 2 s in the 40 s before a change of an interval walk, or, on a phone without a wake-up counter and for an outing with signals or with GPS, the processor kept awake (`docs/adr/0009-sessions.md`, `docs/adr/0013-interval-walks.md`); and, for an outing with GPS, the satellite receiver, on while the reader walks, asleep two minutes after they stop, off at a pause and at the end (`docs/adr/0016-location.md`). An outing with GPS is outside the 1% target: its cost an hour is measured on a phone and said beside its switch.
- **Widget freshness.** The widget shows the current count within about 5 seconds of the screen turning on, and is never more than about 60 seconds stale while the screen is on.
- **Privacy is enforced.** The merged manifest requests exactly the permissions on the build's allowlist (PLANNING §10): no `INTERNET`, no `ACCESS_BACKGROUND_LOCATION`, and CI fails on any permission that is not on the list, and on one that is and is no longer requested. *Amended 18 Nov 2026 (`docs/adr/0016-location.md`, decision 2):* the two location permissions and the service's `location` type are on the list, for an outing that asks for it; letting them in made the gate an allowlist, stricter than the list of forbidden names it was.

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
- **Outing:** a walk or run started on purpose, with one goal (steps, distance, minutes in motion or the rest of the day) and an optional cadence, measured from the steps, with signals on the way. Italian «uscita».
- **Time in motion:** an outing's time, made of the gaps between its steps: a stop is not in it.
- **Measured:** said of an outing's distance when at least nine tenths of it came from satellite fixes (an outing with «Measure with GPS» on). Every other distance in the app is an estimate, and says so. Italian «misurati».
- **Pace:** an outing's time in motion over its measured distance, by the kilometre or the mile («9:40 min/km»): shown, never a goal. Italian «andatura». Not the cadence, which the outings' words call "your pace" («ritmo») when they compare it with the outing's own.
- **Fix:** one position from the satellite receiver. A fix is used when it is accurate enough and the steps agree that the reader could have walked there.
- **Typical day:** the average cumulative step curve for the same weekday over the last 4 weeks, used as a reference on the Today sparkline.
