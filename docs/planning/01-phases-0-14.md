# PLANNING, archive 01: the phases done, 0 to 14 (to 2 Nov 2026)

> Frozen on 2 Nov 2026: a closed part of the plan, moved here from `PLANNING.md` without
> changing a line. Not edited. The live plan is [`PLANNING.md`](../../PLANNING.md); the index
> of every title is [`README.md`](README.md). Here: §11's phases done, from the
> foundations to the ways beyond Europe (Phases 0 to 8, 10, 11, 13 and 14), with their boxes as
> they stood. The ones still open are carried in `PLANNING.md`, «Open». Phase 9 (Google Play)
> and Phase 12 (Your year on foot) were not done, and stay in `PLANNING.md` §11.

---

## 11. Delivery phases

Each phase ends with a merged PR, green CI and its acceptance criteria met.
Phases 1 and 4 need **field testing on a physical device**; an emulator is not enough for sensors or batching.

### Phase 0 — Foundations

- [x] Create the repo; add `README.md`, `LICENSE` (GPL-3.0 full text), `.gitignore`, `.editorconfig`
  - *Deviation:* the repo is `fiorenzobrioni/passo`, not `passo-android`: the sibling repos carry the bare app name.
- [x] Short GPL-3.0 notice in the README and in the app's About screen (added in Phase 3)
  - README in Phase 0; the About group of Settings in Phase 3.
- [x] Gradle setup: version catalog, `build-logic` convention plugins, module skeleton (§2)
- [x] Hilt, Compose, Material 3 theme with dynamic color, and an empty `MainActivity` with an edge-to-edge `Scaffold`
- [x] `strings.xml` for `values/` (English) and `values-it/`; `locales_config.xml` for per-app language
- [x] CI: build, unit tests, lint, formatting, forbidden-permission check
  - Plus the tag-triggered release workflow (Phase 8's, brought forward), so the signing path is exercised from the start.
- [x] Generate the **release keystore** now; store it outside the repo with an offline backup, and add it to GitHub Actions secrets. Never commit it. This key signs every future release and must be reused if the app ever goes on Google Play.
  - Created by the owner on 26 Sep 2026 (RSA 4096, valid to 2056) and stored in the four secrets Chiaro uses (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`). Checked from a one-off workflow run: the secrets decode, the keystore opens, Gradle signs the release APK with it. The temporary key that stood in until then is deleted; no release was ever signed with it. SHA-256 `8B:40:22:8A:8D:EF:E3:E3:F1:6E:FE:1A:DC:C0:4C:C7:F5:B5:82:E4:18:F0:15:E8:27:B9:59:D5:BF:39:7F:B5`.
- [x] `CLAUDE.md` (see §13)
- [x] `docs/adr/0001-foundations.md`: the Phase 0 decisions and their reasons

**Acceptance:**
- CI is green.
- The app launches.
- Adding `INTERNET` to any manifest fails CI.
  - Verified locally: `INTERNET` and `ACCESS_COARSE_LOCATION` added to `:core:tracking`'s manifest made `checkForbiddenPermissions` fail, naming both.

### Phase 1 — Tracking engine (highest risk)

- [x] `core:domain`: `StepAccountant` with test-driven unit tests covering every case in §4.6
  - Plus `StepLedger`, the service's buffer as pure Kotlin: the write triggers of §4.5 and the restore of a batch whose write failed, with its own tests.
- [x] `core:data`: Room schema v1 (§5), DAOs, `TrackingRepository` with the transactional flush
  - One `@Transaction` writes the minute increments, the tracker state, the summaries of the touched days and the log lines; Robolectric tests on an in-memory database.
- [x] `core:tracking`: `StepSensorSource` (wraps `SensorManager` and exposes a `Flow` of events plus `flush()`)
  - Samples and flush completions share one ordered channel, so a flush is complete only once every sample it delivered has been accounted.
- [x] `StepTrackingService` (`health` type): registration strategy (§4.3), screen, shutdown and time receivers (§4.2), in-memory buffer
- [x] `BootReceiver` and `PackageReplacedReceiver`
  - *Deviation:* the start from boot on Android 14 to 17 is established from the documentation, not on devices (§4.1, §15).
- [x] Minimal notification (static text plus today's steps)
- [x] Minimal permission request for `ACTIVITY_RECOGNITION` and `POST_NOTIFICATIONS`
  - With the blocking explanation for a phone without a step counter (§4.6). A placeholder screen in `feature:onboarding`, replaced by the real onboarding in Phase 3.
- *Removed from the plan (owner's decision, §15):* the diagnostics screen and the wake-up vs non-wake-up experiment. The sensor choice is made from the platform documentation (`docs/adr/0002-sensor-reporting.md`); the diagnostics **log** (§5) stays, with no screen.

**Acceptance:**
- [x] All accounting unit tests pass.
- [x] A 3 to 5 day field test on at least one physical device, with nightly full shutdowns and without opening the app, shows no lost steps.
- [x] `batterystats` shows no app wakelocks or alarms while the screen is off.
- [x] Tracking resumes after a reboot without opening the app.
- *Confirmed by the owner on their phone (1 Oct 2026)*, over several days of daily use with nightly full shutdowns, before v1.0.0. By construction the service holds no wakelock and sets no alarm, and nothing in it runs on a timer with the screen off; the boot start follows the documented exemptions (§4.1).

### Phase 2 — Profile, settings, metrics

- [x] DataStore settings and profile repository
  - `UserPreferencesDataSource` stores only the fields that differ from their default and reads back anything out of range as the default; `SettingsRepository` is what the screens use. A profile or goal change goes through `TrackingRepository`, which freezes the past first (§15, `docs/adr/0003-daily-summaries.md`).
- [x] Calculators in `core:domain`: distance, calories, active and brisk minutes, cadence, with unit tests
  - `MetricsCalculator`, `StepLengths`, `ProfileLimits`, `DaySummaries`; every constant in `MetricsConstants.kt` with its source.
- [x] `DailySummary` recomputation for today; freezing past days; "Apply profile to past data"
  - Open days are recomputed at every write; the first write after midnight finalizes the day; late steps on a finalized day add only their own share. "Apply profile to past data" is `SettingsRepository.applyProfileToPastDays()`; its button comes with the Settings screen (Phase 3).
- [x] Units (metric and imperial) with formatting helpers, and locale-aware number formatting
  - `MeasureFormatter` and `UnitConversions` in `core:domain` (numbers only); the unit symbols are string resources in `core:designsystem` (`Measure.text()`, `Resources.format`).

**Acceptance:**
- [x] Calculator tests cover defaults, edge cases (0 steps, missing profile) and unit conversion.
- [x] Changing the weight updates today only, unless "Apply to past data" is used.
  - Both on a real Room database (`TrackingRepositoryTest`), including a day still open at the change and late steps after it.

### Phase 3 — Today screen and onboarding

Built in Chiaro's design language (owner's request): its colors, typefaces, shapes, motion and principles, `docs/adr/0004-design-language.md`.

- [x] Navigation shell: Today, History, Insights, Settings
  - *Deviation:* Today and Settings only (Navigation 3, Settings from the gear, Chiaro's page transition). The bottom bar arrives with its second tab in Phase 5: tabs that lead nowhere would be the screen lying about the app (Chiaro's rule).
- [x] Today: progress ring, metric cards, live updates while visible (low-latency sensor registration while the app is visible)
  - The ring also carries a notch where a usual day stands at this hour. Live: the service publishes today's stored-plus-buffered count to an in-process `LiveSteps`, read only while Today is collected; the sensor already reports within 1 s with the screen on (§4.3). Tiles: distance and calories (estimates, with what they rest on), active and brisk minutes (the brisk ones against the day's share of the WHO's 150 a week), average cadence with its bands (drawn only when there are active minutes).
- [x] Today: day trend sparkline (cumulative steps, goal line, dashed typical-day line, "ahead/behind" label), drawn with a small custom Compose `Canvas`
  - Read with a finger (tap to pin, drag to scrub, a haptic tick per hour); the rest of the day shaded; the usual line goes on past now; draws itself in along time. The "ahead/behind" is the headline sentence and the readout above the chart.
- [x] `TypicalDayCalculator` in `core:domain` with unit tests (§6.2)
  - With `DayCurve` and `TodayOverview` (the headline's choice, the pace, the brisk share, the cadence band), all unit-tested.
- [x] Tracking status banner: permission missing, paused, sensor missing
  - Missing permission and pause are cards on Today with the button that fixes them; a missing sensor is the blocking screen of §4.6, before anything else.
- [x] Onboarding: welcome, profile (skippable), goal, permissions, OEM tips
  - The OEM page only on the makers in `OemTips` (§9 rule 7), with the app's own settings page; never `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (§10). The dontkillmyapp.com link and Samsung were dropped later (§15).
- [x] Settings screen (profile, goal, units, theme, language, typical-day line, pause tracking)
  - Plus palette, typeface and wallpaper colors (Chiaro's appearance group, with its live preview), "Apply profile to past data", privacy, the GPL-3.0 notice and the credits (Phase 0's pending About item). First day of the week, walk detection and notifications wait for the features they drive.
- [x] Complete Italian and English strings; plurals; content descriptions (the sparkline gets a spoken summary, e.g. "6,200 steps, 1,240 more than usual at this time")

**Acceptance:**
- [x] A fresh install through to tracking takes under 1 minute.
  - Confirmed by the owner on a Samsung Galaxy S24 Ultra. The shortest path: Get started, Skip, Continue, Allow (and the two system dialogs), Start counting.
- [x] Every state in the status banner can be reproduced and recovered from.
  - UI tests for permission and pause with their buttons; resuming forgets the baseline (§15).
- [x] The typical-day line is hidden with fewer than 2 valid days and appears automatically afterwards.
  - `TypicalDayCalculatorTest`; the line and the notch follow the value, and the caption says when it will appear.
- [x] UI tests cover onboarding and Today.
  - Compose tests on Robolectric for Today, onboarding and Settings; they also write screenshots to each module's `build/screenshots`.

### Phase 4 — Widget

Two widgets, in Chiaro's dress (owner's request): «At a glance» and «In words», `docs/adr/0005-widgets.md`.

- [x] Glance widget, receiver and `appwidget-provider` XML (sizes from §7)
  - *Deviation:* two widgets, one size spec (default 4×1, minimum 1×1, no maximum: ADR 0005 has why a dp maximum cannot mean four cells).
- [x] Five responsive layouts (1x1, 2x1, 2x2, 3x1/4x1, 4x2) with `GlanceTheme`
  - Five forms for «At a glance» (DOT, NARROW, WIDE, TALL, PANEL), four for «In words» (LINE, ROW, STACK, PANEL), picked from the exact grant, with the budgets pinned by `GlanceLayoutTest` and `WordsLayoutTest`. *Deviation:* colours resolved per card with Chiaro's ink rule rather than `GlanceTheme`, whose day/night the launcher resolves (Chiaro's device reports).
- [x] 4x2 hourly mini-bars (24 `Box`es, heights normalized to the day's busiest hour, current hour highlighted)
  - Four groups of six, because Glance drops the eleventh child of a container; hours to come are a baseline.
- [x] Spike on the progress ring rendering (bitmap vs vector levels), within the Android 17 RemoteViews bitmap limit
  - Bitmap, painted at the size shown and capped at 416 px a side (§7, ADR 0005).
- [x] `WidgetUpdateCoordinator` wired to the service (§7 update strategy)
  - The rules are `WidgetUpdatePolicy` in `:core:domain`, unit-tested; the service reports through `WidgetUpdates`.
- [x] Paused and "permission needed" states; tap actions
  - Plus "stopped by the system" and "not set up yet" (`CountingState`); a paused card's tap resumes from the activity (`TrackingControl`, now also behind Settings' switch and Today's card).
- [x] Generated preview (API 35+) and a static fallback preview
- [x] Per-widget settings (not planned; owner's request): background, Chiaro's six colours, opacity, content switches, the ring's side, with the real card as a live preview.

**Acceptance:**
- [x] All sizes are legible on at least 2 launchers (e.g. Pixel Launcher and One UI), in light and dark themes.
  - Confirmed by the owner on the device (25 Sep 2026). Every form at every reference size, in every dress, is also drawn by `WidgetGalleryTest` into `widget/build/screenshots`.
- [x] The widget updates within about 5 s of the screen turning on.
  - Confirmed by the owner on the device; by construction too (a repaint right after the screen-on flush).
- [x] The logs show no widget updates while the screen is off.
  - Confirmed by the owner on the device; pinned by `WidgetUpdatePolicyTest`.

### Phase 5 — History and insights

Built in Chiaro's design language, like Phase 3 (owner's request: a clean implementation, above all in UI and UX); decisions in `docs/adr/0006-history-and-insights.md`.

- [x] `WalkDetector` in `core:domain` with unit tests (§6.1): gaps bridged, short runs discarded, walk vs run, midnight split, empty day
  - Plus §6.1's optional MIXED type (walking and running minutes each at least 30% of the walk). The average cadence is over the whole walk, pauses included ("35 minutes, 3,420 steps, 98 spm").
- [x] Day detail: hourly bar chart, walks marked on the chart, list of walks with their metrics
  - The walks are shaded over their minutes and marked in a lane under the axis; the list gives time, length, type, steps, distance, cadence and calories. A past day shows the estimates it froze with (§5). Today's walks are also listed on Today.
- [x] Settings: walk detection toggle and minimum duration
  - Plus the first day of the week (follow the phone, Monday, Sunday, Saturday), which History's weeks and the week record now use.
- [x] Week, month and year charts with the goal line; swipe or paging between periods
  - A segmented choice of scale; periods paged with a swipe or the arrows, from the first recorded day to today, with "Latest" to come back. The goal line steps with each day's own goal; a year's bars are each month's average day. A bar opens its day (or, in a year, its month).
- [x] Calendar heatmap of goal completion
  - In the month page: how close each day came to its own goal in one hue, a met day in the goal's color with a check, today ringed; a day opens.
- [x] Insights: averages, totals, lifetime distance, records, streaks
  - One sentence first (`InsightsHeadline`: best day today, a running streak, the last week against the one before, or the average so far); the streak with the last seven days; best day, week, month and longest streak, each opening in History; averages over the last 7 and 30 complete days and since the first; totals.
- [x] Chart components in `core:designsystem`, built with Compose `Canvas`: bar chart (hourly, weekly, monthly, yearly) with goal line, calendar heatmap. Shared axis, label and accessibility helpers (each chart exposes a spoken summary and per-bar semantics)
  - `BarChart`, `CalendarHeatmap` (composable cells rather than a canvas, so each day is a real touch target and node), `WalkList`, shared date formatting (`format/Dates.kt`).
- [x] Navigation shell, completed: the bottom bar with Today, History and Insights (Phase 3's deviation), Material's fade through between tabs, Back to Today.

**Acceptance:**
- [x] The year view with 365 days of data renders in under 16 ms per frame on a mid-range device.
  - Confirmed by the owner on the device (25 Sep 2026). By construction too: twelve bars from at most 366 rows, computed once per change off the main thread.
- [x] Records and streak tests pass, including goal changes across days.
  - `InsightsTest`, `PeriodOverviewTest`.
- [x] Walks detected on field-test days match what the user remembers (start and end within a few minutes).
  - Confirmed by the owner on the device (25 Sep 2026), with the default thresholds.
- [x] With walk detection off, no walk UI appears anywhere.
  - Walks are null all the way to the screen when it is off (`TodayUiState.walks`, `DayDetail.walks`); `HistoryScreenTest` checks the day page.

### Phase 6 — Goals and system surfaces

Built with the owner's request of Phases 3 and 5 (a clean implementation, above all in UI and UX); decisions in `docs/adr/0008-goals-and-system-surfaces.md`.

- [x] Goal-reached notification (opt-in, once per day, triggered from the service)
  - Noticed on the samples the service receives anyway (no wake of its own), told once a day by claiming the day in the settings file first (`GoalReached`, `UserPreferencesDataSource.claimGoalNoticeDay`): days only move forward, so a restart, a time zone that brings back yesterday or a lower goal cannot tell it twice. A day seen reached with the notification off is claimed too. It says the minute the goal was met and the streak it extends.
- [x] Evening reminder (inexact window alarm, configurable time and threshold)
  - Time on the clock dial (20:00 by default); threshold "until the goal is met" (default), below 75%, below half. It says the steps left and the brisk walk they take, after catching up with the sensor's batch (`TrackerLink`). Silent when the count is not live (paused, stopped, no permission), when it comes over two hours late or past midnight, and never armed for a day already at its goal. *Deviation:* `setAndAllowWhileIdle` (as Chiaro's reminders) rather than `setWindow`: one wake either way, and Doze would hold a window alarm until a maintenance window, too late for a walk (§15).
- [x] Weekly summary notification (optional)
  - On the first day of the week at 9:00, the week that ended as History shows it (`WeeklySummary` over `PeriodOverview`): a sentence first (the goal every day, more or fewer steps than the week before, or the days at the goal), then the steps and the daily average, the days at the goal, the best day and the estimates. A non-wakeup alarm: posted the first time the phone is awake after 9:00.
- [x] Quick Settings tile
  - `StepsTileService`: the count and the share of the goal while counting, the reason otherwise (paused, not counting, no permission), read between `onStartListening` and `onStopListening` and live while the panel is open. A tap opens Today; a paused tile's tap asks to resume, as the widget's. Settings adds it with the system's own prompt (`requestAddTileService`).
- [x] Rich ongoing notification (progress bar, distance), throttled while the screen is on
  - Brought forward (owner's request): expanded with `BigTextStyle`, from the same `TodayOverview` as Today and the widgets, without the usual day. Plus the Settings row for how it shows (§15).
- Plus: Settings' Notifications group, with a card when Android would drop what is turned on (the permission refused, the app's notifications or the goals' channel off) and the button to the page that fixes it; turning one on asks for the permission where Android still can.

**Acceptance:**
- [x] No duplicate goal notifications across reboots or time-zone changes.
  - By construction (a claimed day, days only forward) and pinned by `GoalReachedTest`, `UserPreferencesDataSourceTest` and `GoalNotifierTest` (a second process on the same store tells nothing). To confirm on the device.
- [x] The tile reads data only while the Quick Settings panel is open.
  - By construction: its only reads live in a scope opened by `onStartListening` and cancelled by `onStopListening`. To confirm on the device.
- [x] Battery check at the end of the phase (§9, §12). Expected: one alarm a day with a wakeup, only with the evening reminder on; nothing else new with the screen off.
  - Confirmed by the owner on the device (29 Sep 2026), in the field.

### Phase 7 — Data, calibration and polish

Export, import and calibration built with the owner's request of Phases 3, 5 and 6 (a clean
implementation, above all in UI and UX); decisions in `docs/adr/0011-export-import-and-calibration.md`.

- [x] Export to CSV and JSON, import from JSON (Storage Access Framework)
  - A "Your data" group in Settings: a backup to one JSON file (every minute with its day, the frozen summaries, the outings and their plans, the profile and the settings; the diagnostics log out, never back in; no tracker state), three CSV tables for a spreadsheet (days, minutes, outings), and the import. `BackupCodec`, `BackupMerge` and `CsvExport` in `:core:domain`, `BackupRepository` in `:core:data`, the file picker's `CreateDocument`/`OpenDocument` in the screen: no storage permission.
  - *Decided:* the import **merges** and never deletes (every minute at the larger of its two counts, the file's own days as it froze them, this phone's frozen days taking only the added steps' share, plans and outings matched), so a new phone that already counted keeps its steps and the same file imported twice changes nothing. It is previewed before anything is written, with its profile and settings as one choice. Outcomes are cards, not toasts.
- [x] `data_extraction_rules.xml` and `full_backup_content` for Auto Backup and device transfer
  - Brought forward to Phase 5 (owner's request), `docs/adr/0007-backup.md`. `full_backup_content` is not needed: it is read only below Android 12, and minSdk is 34. Backup and restore confirmed by the owner on the device (25 Sep 2026); the export round trip is still Phase 7's.
- [x] Step length calibration wizard (walk a known distance, start/stop, compute and save)
  - "Measure your step" (Settings' profile, and a button in each step-length dialog): the walking or the running step, a distance picked (50 m to 2 km, or yards), Start, the count while the page is looked at, Stop, the length with what it came from and what it changes, Save. The hardware counter is read directly (`StepCounterProbe`) only while the page is on screen, and flushed at Stop; `StepCalibration` refuses fewer than 30 steps or a length the app would not accept, with its arithmetic, and says a pace that does not match the step. A walking step is stored as measured (`StepLengthMode.CALIBRATED`).
- [x] The guide (owner's request, 25 Sep 2026; §15): `:feature:guide`, a tour of the three screens and the outings in Chiaro's shape, from the top of Settings and from Today's first-day card
- [x] Accessibility pass (TalkBack, font scale 200%, contrast, touch targets)
  - `docs/adr/0012-foldables-and-accessibility.md`. A test-only module, `:core:testing`: `assertAccessible()` (every control labelled, every control 48 by 48dp for a finger, as Compose's widened touch areas actually share the space) runs on every state the screen tests draw, and every screen is walked top to bottom at twice the text size on a 360dp phone (`walkPage()`). `ContrastTest` pins every text ink on every ground at 4.5:1, both dresses, both themes.
  - Found and fixed: the calendar's numbers (2.6:1 mid-ramp, 3.8:1 on uncounted days; the ramp's stops now per theme), the widget's colour swatches (36dp, now 48dp targets), and at twice the text size tiles breaking words (`TilePair`), a chart's title squeezed by its key, chart labels overrunning their margins (`chartMargins`), clipped segmented buttons (`SegmentLabel`), a cut date, a time broken in two, the guide's ring spilling its count. Dense targets (a chart's bars, a month's days, Material's clock dial) are exempt from the size only (`DENSE_TARGETS_TAG`).
- [x] Adaptive layouts for foldables
  - *Owner's decision (29 Sep 2026):* **no tablet layout** (Google Fit and Samsung Health have none; Passo is a phone app); foldables yes, at low risk. One centred column at most 640dp wide (`pageGutter()`), grounds, bars and glow still spanning the window, lists still scrolling and swiping from the edge; below 640dp (every phone upright, a folded foldable) the layout is exactly as before. The display cutout and a side navigation bar are now respected on a phone on its side. No second layout, no navigation rail, no new dependency (§15). In the README with its own screenshot (`today-foldable.png`, an open Pixel 9 Pro Fold), at the owner's request.
- [ ] Baseline Profiles; R8 full mode; startup check

**Acceptance:**
- [x] Export followed by import on a clean install reproduces the history exactly.
  - `BackupRepositoryTest`, between two real Room databases and settings files: every summary, minute, outing, plan, the profile and the settings (not the other phone's first-run state or tracker state), through the encoded file. To confirm on devices, through a file app and a cloud folder (owner).
- [ ] The accessibility scanner reports no critical issues.
  - The Scanner's touch-target and label checks run in every screen test and pass, and the theme's contrast is pinned by `ContrastTest`. The Scanner app itself, over the main screens on a device, is the owner's (with a TalkBack walk and the app on a foldable, opened and folded while on screen).

### Phase 8 — Release on GitHub

- [x] App icon (adaptive + monochrome for themed icons), final package name
  - The icon is done (25 Sep 2026, owner's choice among four drawings): Chiaro's ring with a shoe print where Chiaro has its sun, drawn by `tools/draw_launcher_icon.py` (§15). The package name `com.callbackdev.passo` is confirmed by the owner (29 Sep 2026).
- [x] Versioning: semantic version tags `vX.Y.Z`; `versionCode` derived from the version (e.g. `major × 10000 + minor × 100 + patch`)
  - Done in Phase 0: `passo.versionName` in `gradle.properties`, code derived in `app/build.gradle.kts`, and `release.yml` refuses a tag that does not match.
- [x] `CHANGELOG.md` ("Keep a Changelog" format); release notes in English and Italian
  - The file and its format exist since Phase 0. The `## [1.0.0]` section is the release notes, short, in English only (§15); the phase-by-phase record moved to `docs/CHANGELOG-1.0.0.md`.
- [x] Release workflow (GitHub Actions, triggered by a `v*` tag):
  - build the release APK with R8, signed with the keystore from secrets
  - attach the APK, its SHA-256 checksum and the R8 mapping file to the GitHub Release
  - mark `-beta`/`-rc` tags as pre-releases
  - Done in Phase 0; signed with the real key from secrets since 26 Sep 2026 (the temporary-key fallback is gone).
- [x] README: screenshots, features, install instructions (allowing installs from the browser or file manager), how to verify the checksum, and the **signing certificate SHA-256 fingerprint** so users can check the APK is genuine
  - The fingerprint and the checksum check since the key was created (26 Sep 2026); the README rewritten in the family's shared structure on 1 Oct 2026 (§15), with them under "Install".
- [x] README: updates. The app has no network access, so it can't check for updates itself; point users to GitHub's "Watch → Releases" notifications or to Obtainium, an app that tracks GitHub releases
- [ ] Test the full install and update path: install v1.0.0 from the Release, then update to a newer build over it, with data preserved

**Acceptance:**
- Pushing a tag produces a signed release with APK, checksum and notes, with no manual steps.
- Installing a newer release over an older one keeps all data.

### Phase 10 — Outings (walks with a goal)

Added to v1.0 at the owner's request (25 Sep 2026), after Phase 6 and before Phases 7 and 8; decisions in `docs/adr/0009-sessions.md`. VISION.md's workout non-goal narrowed to a workout suite. Name: «Uscite» / "Outings".

- [x] Model and engine (`:core:model`, `:core:domain/sessions`): one goal (steps, distance, minutes in motion, the rest of the day) and an optional cadence (free, 100, 130, 140); `SessionTracker` fed every accounted delta: time in motion from the gaps between steps (at most 1.5 s a step), the cadence of the last 30 s, distance and energy per step at that cadence, milestones told once (the highest of several crossed at once), the goal ending it, "Keep going" for 15 min with the steps since, the ends by itself; `SessionPlans` (presets, estimates for the editor, the start that copies the goal), `SessionHeadline`, `DayOutings`.
- [x] Storage: schema v2 by auto-migration (`session_plan`, `session`), `SessionRepository`, the presets written once, the outing under way written in the step batch's transaction (`TrackingDao.writeBatch`), `LiveSession` for the screens. Migration test from v1.
- [x] Service: commands as intents (`SessionControl`: start, rest of the day, pause, resume, stop, keep going), the wake-up counter during an outing only, the signals (`SessionHaptics`: 1, 2, 3 short, 1 long), the counting notification as the outing's (collapsed and expanded, `ProgressStyle` and Live Update on Android 16), the goal's notification on the `sessions` channel, an outing picked up after a restart, closed when counting is paused.
- [x] The Outings page (`:feature:sessions`): the plans with what they come to and their signals, Start, the outing under way, a paused count or silenced signals stated with the way back, the notification permission asked in context at the first start. The editor: name, goal (picked, never typed; quarter miles in miles), pace, the estimate with the reader's step, the signals, "Try them", save, delete, discard asks.
- [x] Today: "Start an outing", the outing's card (under way, paused, just over with "Keep going" and Close); Today's and History's lists show an outing in place of the walk found in its minutes, with its goal and its outcome, and History's chart marks it.
- [x] The launcher's long press (the three last started), the evening reminder's "Walk now", both widgets and the Quick Settings tile saying the outing while it is under way (owner's request).
- [x] Settings: an Outings group, with the page's own door ("Your outings") and the switch for Today's button (owner's request, 25 Sep 2026; §15).
- [x] Strings in English and Italian.
- [x] On a device (owner): an outing with the screen off (signals on time, the vibrations felt and told apart), the Android 16 Live Update, a reboot and a forgotten outing, and the battery check of an outing (§9).
  - Confirmed by the owner in the field (29 Sep 2026).
- [x] The voice (second iteration, owner's choice; `docs/adr/0010-voice.md`): per outing off, headphones or always (out loud only when the ringer is on); the start, each chosen signal with what is left and the pace, the goal with what it came to, every amount in words in English and Italian; the system's engine with an offline voice only, over ducked music; "Hear it" and a missing voice stated in the editor. Schema v3 (`voice` on both tables, default off).
- [x] On a device (owner): the voice with and without headphones, with music playing, and with the screen off and no music (the one case ADR 0010 leaves to measure).
  - Confirmed by the owner in the field (29 Sep 2026).

**Edge cases** (the engine's in `SessionTrackerTest` and `SessionPlansTest`, the storage's in `SessionRepositoryTest`; a restart and a paused count are the service's, by construction, to check on the device):

| Case | Expected behavior |
|---|---|
| Standing still, then one step | The step adds 1.5 s, not the stillness |
| A merged batch (steps without their own timestamps) | Credited with its steps' worth of time, never the whole gap |
| Several milestones crossed by one batch | Only the highest is told; all are marked told |
| The goal reached | The outing ends there; later steps are counted aside for "Keep going" (15 min) |
| No step for 15 min | Ends at its last step, when the next step or the screen notices; told then (two long pulses, the voice), with "Resume" for 15 min from that moment, the steps since counted aside and the stillness not |
| The steps since an end by stillness cross a milestone or the goal | Told when "Resume" reopens it; the goal ends it again |
| A finished outing's card closed | "Keep going" / "Resume" withdrawn, on the card and in the notification |
| Paused for an hour | Ends where it was paused |
| Open for four hours | Ends at its last step |
| Fewer than 30 steps | Not kept |
| The process killed during an outing | Picked up from the last batch; the steps meanwhile arrive with the next sample |
| Counting paused during an outing | The outing ends |
| The rest of a day already met | Cannot start (under 100 steps left) |
| A plan edited or deleted later | Past outings keep the goal they were walked with |

### After v1.0

Three phases asked for by the owner after the v1.0.0 release (1 Oct 2026). They are written
now so that the order can be chosen; the owner decides which comes first, and each becomes a
minor release (`1.x.0`). None needs a new permission or a new dependency, and none touches the
everyday tracking path. What was weighed and dropped is in §15 (route maps with GPS, the peak
cadence, the weekly rhythm). A fourth, Phase 14 (more ways, beyond Europe), was asked for
on 16 Oct 2026 and built the same day.

### Phase 11 — The Ways («I Cammini»)

VISION.md's "virtual journeys", in the shape of the long ways walked in stages: the reader picks
one of four ways and the day to count from, and the estimated distance walked since then moves
a point along it. Each stage reached is a stamp, with the day it was reached. **A map
with no location:** the way is drawn, and the reader's place on it is their distance, never
where they are. Everything is computed on read from the days already stored; nothing new runs
while the screen is off. Name: «Cammini» / "Ways". Decisions in `docs/adr/0014-ways.md`.

**The four ways**, as mapped on OpenStreetMap (a way's length is its line's, which is what the
point moves along). A spread of lengths, so that one is finished in weeks and one takes
half a year, at an ordinary 5 km a day. The Francigena is its Italian part only (owner, 1 Oct
2026: from Canterbury, 2,000 km, is too long for everyday walkers). The Camino Portugués joined
them later as the fifth (below, «Phase 11, later»):

| Way | From, to | About | At 5 km a day |
|---|---|---|---|
| Via degli Dei | Bologna, Florence | 123 km, 9 stages | 4 weeks |
| Camino Portugués (the fifth, added later) | Porto, Santiago de Compostela | 245 km, 12 stages | 7 weeks |
| Via di Francesco | La Verna, Assisi, Rome | 434 km, 22 stages | 3 months |
| Camino de Santiago, the French Way | Saint-Jean-Pied-de-Port, Santiago de Compostela | 768 km, 34 stages | 5 months |
| Via Francigena, the Italian part | Great St Bernard Pass, Rome | 1,020 km, 46 stages | 7 months |

- [x] The ways' data, written by a script (`tools/build_ways.py`, re-run, never hand-edited, like the launcher icon): for each way its stages (a key, the name's resource, latitude and longitude, the official distance from the start), the line drawn, simplified to a few hundred points, and the outline of the countries behind it (Natural Earth, public domain, simplified). Output: Kotlin in `:core:domain/ways`. The line is the way's OpenStreetMap relation, simplified (owner, §15): the script reads an export saved by hand into `tools/`, never the network at build time; the data is under ODbL, credited in About and in the guide («© OpenStreetMap contributors»), and the derived file says so in its header.
- [x] Domain (`:core:domain/ways`, pure, tested): `WayProgress` (from the day totals since the start: the distance walked, the stage reached, the place between two stages, the day each stage was reached, the day it was finished), `WayProjection` (equirectangular around the way's middle latitude, fitted to a box), `WayForecast` (the arrival at the reader's average of the last 28 days, said as an estimate, only with 7 or more days walked), `WayAnnouncement` (the stage just reached, told once).
- [x] The distance is the days' own: each finished day's frozen `distanceMeters`, today's live one. A way never keeps a total of its own, so it cannot disagree with History; "Apply profile to past data" and an import move it, on read, and the guide says so.
- [x] Storage: a `way_journey` table (id, way key, start day, state `ACTIVE` / `FINISHED` / `LEFT`, finish day, how far the stages were told), schema v4 by auto-migration, with its migration test. The stamps' days are computed, not stored. The backup carries the journeys.
  - *Deviation:* as an added field (`ways`), with no new format version: the codec's rule is that added fields do not raise it (an older reader skips them). An import adds the file's ways by their start instant; one under way comes in under way only if this phone walks none, otherwise as left on the day the file was written.
- [x] The service: a stage reached rides on the step samples, as the goal reached does (today's distance crossing the next stage's mark); one notification, the furthest stage when a batch crosses several, on a new `ways` channel, on for a way the reader started, and theirs to silence. Never a timer.
  - `WayNotifier`, started with the process like the goal alarms, watches the days the service writes, only while a way is under way; the stage is claimed in the database before it is told. Touching it opens the Ways page.
- [x] `:feature:ways`, the Ways page: the four, with their length, stages and the time they would take at the reader's pace. Starting one: from today, from 1 January, from the first day Passo counted, or a chosen day. A start in the past places the reader at once (the "you would already be in Siena" moment), with the stamps of the stages behind and no notification for them.
- [x] The way under way: the map (Canvas: the outline, the whole line faint, the part walked in the accent, the reader's point, the stages as dots, a stage's name on touch), its sentence («Past Siena: 231 km to Rome»), the forecast, the stages with their days (the map's accessible equivalent), the credential (one stamp per stage reached, drawn in code: the place, the day, a tilt fixed for each stage; a generic stamp, never an official one). Leaving a way asks first; a finished or left way keeps its credential in "Your ways".
- [x] Insights: a card for the way under way (the small map and the sentence); with none, the door to the Ways page.
- [x] Today: the way under way closes the page, with what the day did for it (owner, 17 Oct 2026; §15 «The way under way on Today»).
- [x] The places: every stage's name in English and Italian (Florence / Firenze); about ten notable places for each way, with one checked sentence each in both languages, its source noted in the script.
- [x] **A sentence for every stage that has one** (owner, 7 Oct 2026): the long ways had a sentence for one stage in three, the others only a name. The same rules as the others' (two sources, one breath, nothing that goes stale, linked to the way where it can be); a stage with nothing true and worth saying stays without one, never filled.
  - [x] The Via Francigena: 29 of its 32 stages without a sentence.
  - [x] The Camino Francés: 22 of its 23.
  - [x] The Via di Francesco: 12 of its 13.
  - [x] The Via degli Dei (3 of 3) and the Camino Portugués (2 of 3).
- [x] The guide: a chapter (a way moves with the estimated distance; measuring the step makes it truer; what moves it back).
- [x] Strings in English and Italian; UI tests with `assertAccessible()` and `walkPage()`; README screenshots (the map, the credential), CHANGELOG.
- [x] On a device (owner): a backdated start, a stage notification on a walk, the four ways drawn in both themes and on an open foldable. Done by the owner in the week of 5 Oct 2026.

**Acceptance:** starting a way from 1 January places the reader at once with every stamp
behind; a stage reached on a walk is told once, during the walk; History, Insights and the way
agree on the distance for any period.

**Edge cases** (`WayProgressTest`, `WayAnnouncementTest`):

| Case | Expected behavior |
|---|---|
| A start in the past | Placed at once; the stamps behind carry their days; no notification for them |
| A start before the first day counted | Moved to the first day counted (a day without data would add nothing, and the date would read as one walked with Passo) |
| A day without steps | The point does not move |
| A batch, or a backdated start, crossing several stages | One notification, the furthest stage |
| The last stage reached | Finished that day; the distance beyond is not carried to another way |
| The profile applied to past data, an import | Recomputed on read; stamps may change day; a way finished stays finished, its last stops reached on the day it ended |
| The way left | Kept as left with its stamps; another can start |
| A change of time zone | Days are local days, as everywhere else |

#### Phase 11, part two — City walks («Passeggiate in città»)

Asked for by the owner (1 Oct 2026), after the Ways' first part, whose data script and map it
reuses. A walk through a city, in one outing or a few: an outing whose goal is the route, whose
signals are the places, and whose voice, if the outing speaks, says each place as the reader
reaches it. **Imaginary, and said so:** the reader walks where they are (the neighbourhood, a
park, a treadmill), and the route moves with the outing's estimated distance, never with a
location. Nothing new for the battery: it is an ordinary outing (ADR 0009).

**The cities:** Milan, Rome, Paris, London, Madrid (owner). **Milan and London first**; the
others one a release, each when its content is checked.

**A city can have more than one walk** (owner's question): the unit is the *walk*, grouped by
city, from the first line of code, so London's second walk is data, not a feature. On screen a
city with one walk is one row; the second level (the city's walks) appears only when a city has
two. The first release has one walk a city.

- [x] The walks' data, by the same script as the ways (`tools/build_ways.py`): each walk drawn once in a router built on OpenStreetMap data (BRouter), its GPX saved into `tools/` by hand, simplified by the script; its places (a key, the name's resource, latitude and longitude, the distance along the walk, measured by the script); behind it the city's water and largest parks from OpenStreetMap (the Thames, the Tiber, the Seine, the Navigli and the Darsena, the Manzanares), never a street grid (revised by the owner on 2 Oct 2026: the main streets, faint, ADR 0015 decision 3). ODbL, credited as for the ways. Between 8 and 12 km a walk, about twenty places (revised by the owner on 2 Oct 2026: two lengths, about 5 km and about 10 km, the city's to choose; ADR 0015 decision 10).
- [x] Indicative walks, decided with the owner when each is drawn: Milan (the Duomo, the Galleria, La Scala, Brera, the Castello Sforzesco, Parco Sempione, the Arco della Pace, Sant'Ambrogio, the Columns of San Lorenzo, the Darsena and the Navigli); London (Westminster, the Elizabeth Tower, Trafalgar Square, the South Bank, the London Eye, Tate Modern, the Millennium Bridge, St Paul's, Borough Market, Tower Bridge, the Tower of London).
- [x] The places' sentences: written for Passo, never copied (not from Wikipedia either), each checked against two sources and noted in the script; in English and Italian; one sentence of at most about twenty words, made to be heard; nothing that goes stale (no opening hours, prices or "now showing").
- [x] Domain (`:core:domain/ways`): `CityWalkProgress` (the distance done on a walk, summed from the outings walked on it since it was started; the place reached; the next one and how far), the places crossed by a batch (each named once; the voice says the names in order and the last one's sentence).
- [x] Storage: the Ways' table with a kind (way or walk), and on `session` a nullable walk key and the distance the outing started from; the backup carries both. A walk's progress is computed from its outings, never kept apart.
- [x] The outing: a new goal kind, a walk, its goal the distance left; the 25, 50 and 75% signals off (the places are the signals: one short pulse and the voice); the notification's line is the next place («Next: the Duomo, 600 m»); the goal is the walk's end. Stopped halfway, the walk waits: "Continue from Piazza Navona" starts the next outing where the last one ended; starting again from the beginning asks first; leaving it asks too, and puts it back to its start, its outings kept (owner, 2 Oct 2026; ADR 0015 decision 11).
- [x] Screens: the Ways page in two parts, «Cammini» and «Città»; a city's walk with its map (the water, the parks, the route, the places, the point), its places with the day each was reached, Start or Continue. During the outing, its card on Today and the Outings page carries the small map (drawn only while the screen is on). A finished walk stays in "Your ways" with its places and its day.
- [ ] Your year on foot (Phase 12) names the cities walked that year. Built with Phase 12, whose pages read the ways (§11 Phase 12).
- [x] Strings in English and Italian; tests (`CityWalkProgressTest`: a walk over three outings, several places in one batch, the end, a restart); UI tests; a README screenshot (London's map during a walk); CHANGELOG.
- [x] On a device (owner): Milan walked in one outing and London over two, with the voice through headphones and with the screen off; on a treadmill once. Done by the owner in the week of 5 Oct 2026.

Built as `docs/adr/0015-city-walks.md` records: a walk is a `WayId` of kind `WALK`; the routes
come from BRouter and are committed in `tools/walks/`; progress is `WalkDays` over the walk's
outings, computed on read (no `CityWalkProgress` class: `WayProgress` does it, with the outings'
days in place of the days'); the outing is a distance goal with the walk's places as its signals
(`SessionSignal.Places`), schema v5.

**Edge cases:**

| Case | Expected behavior |
|---|---|
| A walk over several outings | Each outing starts where the last ended; the places already reached are not told again |
| Two places crossed by one batch | Both named, in order; the last one's sentence |
| The walk's end | The outing's goal: the goal's long vibration and "Keep going" |
| An outing ended by stillness halfway | The walk waits at that distance; "Resume" or "Continue" later |
| Restart from the beginning | Asks; the earlier outings stay in History, the walk counts from zero |
| The voice off | The pulse, and the place in the notification |

#### Phase 11, later — The fifth way, and the other cities

Asked for by the owner (2 Oct 2026), after the city walks. The set first stopped at five ways
and five cities; the owner lifted that limit the same day, and Lima and Cusco followed, one walk
a city for now.

- [x] **The Camino Portugués from Porto** (about 240 km, the second most walked way to
  Santiago), as the fifth way: its OpenStreetMap relation and its stages in
  `tools/ways_content.py`, the locator of Portugal and Spain, its places' sentences checked as
  the others were. The data shape, the map and the credential need nothing new.
- [x] **Rome, Paris and Madrid**, one a release, each when its walk is drawn and its places'
  sentences checked, after Milan and London.
  - *Deviation:* all three in the same unreleased version (owner, 2 Oct 2026: "implement the
    phase"), each drawn and checked on its own; holding one back is data only (its line in
    `WALKS`, its enum constant and its route). Confirmed by the owner (2 Oct 2026): the three
    cities ship together.
- [x] **Lima and Cusco** (owner, 2 Oct 2026, from a proposal): one walk each. Lima's is its
  historic centre (the owner's other places, Miraflores, Barranco and the Costa Verde, are 10 km
  south: a second walk one day, with the city's second level and the sea the script cannot draw
  yet); Cusco's climbs from the Plaza de Armas to Sacsayhuamán and comes down to the Qorikancha.
- [x] **Porto, Amsterdam and Prague, the first short walks** (owner, 2 Oct 2026, from a
  proposal): three European countries not yet on the page, each about 5 km, after the rule of
  two lengths (ADR 0015 decision 10). Porto's begins at the cathedral, where the Camino
  Portugués begins, and crosses the Douro to the Serra do Pilar; Amsterdam's goes from
  Centraal Station along the canals to the Westerkerk; Prague's comes down from the Castle over
  Charles Bridge to Wenceslas Square. Ten cities: the ceiling of one flat list.
- [x] **The cities by continent** (owner, 5 Oct 2026, from a proposal): past ten cities one
  list no longer reads, and more are planned around the world. The Ways page has a row a
  continent (its small map, its cities, how many walked, the ones under way), each opening the
  continent's page: its map with the cities as points, then the cities as before. Europe (the
  eight European cities) and the Americas (Lima and Cusco); `docs/adr/0015-city-walks.md`
  decision 12.
- [x] **Berlin and Vienna, Europe's ten** (owner, 5 Oct 2026, from the proposal): two long
  walks, so Europe has seven of about 10 km and three of about 5. Berlin's goes from the Wall
  Memorial on Bernauer Straße by Museum Island, Checkpoint Charlie and the Brandenburg Gate to
  the Victory Column (10.5 km, 13 places); Vienna's from the Upper Belvedere round the Ring and
  by St Stephen's, over the Danube Canal to the Prater's wheel (9.7 km, 13 places).
- [x] **The coast, and New York and Rio** (owner, 5 Oct 2026): the script builds a city's land
  from OpenStreetMap's coastline, and the map is cut out of the sea (ADR 0015 decision 13). New
  York's walk goes from Central Park by Times Square, the Empire State and Washington Square to
  the Brooklyn Bridge (11.6 km, 13 places); Rio's from the Museum of Tomorrow through the
  centre and Lapa, along Flamengo Park and Botafogo, to the Sugarloaf's cable car (11.1 km,
  12 places).
- [x] **Mexico City and Buenos Aires** (owner, 6 Oct 2026): Mexico City's walk goes from the
  National Palace on the Zócalo by Bellas Artes and the Alameda, along the Paseo de la Reforma,
  to Chapultepec's castle and the Museum of Anthropology (9.7 km, 12 places); Buenos Aires's
  from the Casa Rosada along the Avenida de Mayo to the Congress, by the Obelisco, the Teatro
  Colón and Plaza San Martín, to Recoleta and the Floralis Genérica (10.4 km, 13 places), on
  the Río de la Plata's coast.
- [x] **San Francisco, Québec, Havana and Cartagena** (owner, 6 Oct 2026), the Americas' ten:
  San Francisco's walk goes from the Ferry Building by the Transamerica Pyramid, Chinatown and
  North Beach, up to Coit Tower, along the Wharf and down Lombard Street, to the Palace of Fine
  Arts (10.4 km, 12 places), on the bay; Québec's from the Parliament by the Plains of Abraham,
  the Citadelle and the Château Frontenac, down the Breakneck Stairs to Place Royale and the
  Musée de la civilisation (4.4 km, 8 places); Havana's from the Capitolio by the Gran Teatro
  and the Prado to La Punta, then through the old squares to the Alameda de Paula (4.5 km, 10
  places); Cartagena's from the Clock Tower through the walled city and along its walls, then
  through Getsemaní to the Castillo San Felipe (5.5 km, 8 places).
- [x] **About ten cities a continent** (owner): about
  seven of 10 km and three of 5 km, the city choosing its length. Done: four continents of ten
  cities each, 40 in all. The Americas, proposed (5 Oct
  2026): all ten done: New York, Rio, Mexico City, Buenos Aires and San Francisco, long;
  Québec, Havana and Cartagena, short (Washington, Montréal, Boston's Freedom Trail and Valparaíso if
  one does not work out). Then Asia and Oceania (all ten done), and Africa (all ten done), each
  continent added with its first cities. Cities only.
- [x] **Asia and Oceania** (owner, 6 Oct 2026: "start the Asia and Oceania group, tell me which
  cities, and do the first two"), proposed: Tokyo, Sydney, Seoul, Beijing, Hong Kong, Singapore
  and Bangkok, long; Kyoto (Higashiyama, from Kiyomizu-dera to Gion), Hanoi (the Hoàn Kiếm lake,
  the Old Quarter and the Temple of Literature) and Melbourne (its centre and laneways to the
  Exhibition Building), short (Shanghai's Bund, Mumbai, Delhi, Auckland and Kathmandu if one
  does not work out). The continent's frame runs from Mumbai to New Zealand, not the Middle
  East (Istanbul is Europe's).
  - [x] **Tokyo and Sydney**, the continent set up with them: Tokyo's walk goes from the
    Kaminarimon and Sensō-ji by Kappabashi, Ueno Park and Shinobazu Pond, Ameyoko, the Kanda
    shrine and Akihabara, to Nihonbashi, Tokyo Station and the Imperial Palace (11.6 km,
    14 places); Sydney's from Luna Park over the Harbour Bridge to The Rocks and Circular Quay,
    by the Opera House, the Botanic Garden and Mrs Macquarie's Chair, then the Domain, Hyde
    Park and the Queen Victoria Building, to Darling Harbour and the Chinese Garden (10.1 km,
    14 places).
  - [x] **Seoul and Beijing** (owner, 6 Oct 2026): Seoul's walk goes from Gwanghwamun Square
    and the palace's gate by Bukchon, Changdeokgung, Jongmyo, Insa-dong and Jogyesa, along the
    Cheonggyecheon to Deoksugung, Sungnyemun and the Namdaemun market, then up Namsan to N Seoul
    Tower (11.1 km, 12 places); Beijing's from Tiananmen Square and its gate through the
    Forbidden City (in by the Meridian Gate, by the Hall of Supreme Harmony and the Palace of
    Heavenly Purity, out by the Gate of Divine Might), then by Jingshan, Beihai, Prince Gong's
    Mansion and Shichahai, the Drum and Bell Towers and Nanluoguxiang, to the Lama Temple
    (10.6 km, 15 places; owner, 6 Oct 2026: "I'd like it to go into the Forbidden City").
  - [x] **Hong Kong and Singapore** (owner, 6 Oct 2026): Hong Kong's walk goes from Victoria
    Park by the Noonday Gun, the Pak Tai Temple and Golden Bauhinia Square, along the harbour to
    the Star Ferry, through Central (Statue Square, the HSBC and Bank of China towers, Flagstaff
    House, St John's, the Peak Tram) and up to Tai Kwun, PMQ and the Man Mo Temple, to Western
    Market (11.0 km, 15 places), on the coast; Singapore's from the Buddha Tooth Relic and Sri
    Mariamman temples in Chinatown by Clarke Quay, Fort Canning, the Raffles Hotel and the Civic
    District, along the river to the Merlion and the Esplanade, over the Helix Bridge to Marina
    Bay Sands and the Supertree Grove (8.4 km, 14 places).
  - [x] **Bangkok and Kyoto** (owner, 6 Oct 2026): Bangkok's walk goes from the Giant Swing by
    the Golden Mount, the Loha Prasat, the Democracy Monument and Khao San Road to Phra Sumen
    Fort, then by the National Museum, Wat Mahathat, Wat Phra Kaew and Wat Pho, over the
    Memorial Bridge to Wat Arun (9.8 km, 12 places); Kyoto's, short, from Kiyomizu-dera down
    Sannenzaka by the Yasaka pagoda, Kōdai-ji, Maruyama Park and the Yasaka Shrine, through Gion
    and by Kennin-ji, over the Kamo to Pontochō and the Nishiki market (4.2 km, 10 places).
  - [x] **Hanoi and Melbourne** (owner, 6 Oct 2026), the continent's ten: Hanoi's short walk
    goes from the Temple of Literature by Hỏa Lò and St Joseph's Cathedral, round Hoàn Kiếm Lake
    to the Ngọc Sơn temple, through the Old Quarter and by the Đồng Xuân market to the Long Biên
    Bridge (5.9 km, 8 places); Melbourne's from Flinders Street Station by St Paul's and
    Federation Square, the State Library and the Old Melbourne Gaol, the Princess Theatre,
    Parliament and St Patrick's, to the Royal Exhibition Building (4.6 km, 9 places).
- [x] **Africa** (owner, 6 Oct 2026: "start the Africa group, tell me which cities, and do the
  first two"), proposed: Cairo, Cape Town, Marrakech (from Jemaa el-Fnaa and the Koutoubia
  through the souks to the Ben Youssef madrasa, the Bahia Palace and the Saadian Tombs), Tunis
  (the Kasbah, the medina and the Zitouna Mosque, out by Bab el Bhar along the Avenue Habib
  Bourguiba), Alexandria (along the Corniche from the Citadel of Qaitbay to the Bibliotheca
  Alexandrina, by the Roman theatre and Pompey's Pillar), Dakar (the Plateau, from the Place de
  l'Indépendance by the cathedral, the Kermel market and the IFAN museum, along the Corniche)
  and Addis Ababa (from Meskel Square by the Holy Trinity Cathedral and the National Museum,
  Lucy's, to the Piazza), long; Fez (the medina, from Bab Bou Jeloud to the Qarawiyyin and the
  tanneries), Zanzibar's Stone Town (from the Old Fort and the House of Wonders through the lanes
  to the old slave market and the Anglican cathedral) and Luxor (from Luxor Temple along the
  Avenue of Sphinxes to Karnak), short (Casablanca, Rabat, Algiers, Nairobi, Accra, Saint-Louis
  and Essaouira if one does not work out). The continent's frame is the whole of it, from
  Dakar to the Horn of Africa and from Tunis to the Cape, so the cities proposed fit without
  moving it.
  - [x] **Cairo and Cape Town**, the continent set up with them: Cairo's walk goes from the
    Egyptian Museum by Talaat Harb Square, the Abdeen Palace and the Museum of Islamic Art, up
    Al-Muizz Street by the Qalawun complex, the Aqmar Mosque and al-Hakim's to Bab al-Nasr,
    back by Khan el-Khalili, Al-Azhar, al-Ghuri's complex and Bab Zuwayla, then by the Blue
    Mosque and Sultan Hasan's to Muhammad Ali's mosque in the Citadel (9.9 km, 15 places); Cape
    Town's from the Green Point Lighthouse by the stadium, the Waterfront and Nobel Square, over
    the Foreshore to the Castle, the City Hall and the District Six Museum, then through the
    Company's Garden by the South African Museum, St George's, the Slave Lodge and Greenmarket
    Square, to the Auwal Mosque and Bo-Kaap (10.8 km, 15 places), on Table Bay's coast.
  - [x] **Marrakech and Fez** (owner, 6 Oct 2026): Marrakech's walk goes from the Majorelle
    Garden by Bab Doukkala and Dar el Bacha into the medina, by the Mouassine mosque, the
    Almoravid Koubba and the Ben Youssef madrasa, through the souks to Jemaa el-Fnaa and the
    Koutoubia, in by Bab Agnaou to the Saadian Tombs and the El Badi Palace, then by the Mellah
    and the Bahia Palace to Dar Si Said (8.2 km, 14 places); Fez's, short, from the Royal
    Palace's doors by the Mellah and the Jnan Sbil gardens, in by Bab Bou Jeloud to the Bou
    Inania madrasa, then by the zawiya of Moulay Idris II and al-Qarawiyyin to the Chouara
    tannery, over the river to the Andalusian Mosque (4.1 km, 8 places).
  - [x] **Tunis and Alexandria** (owner, 6 Oct 2026): Tunis's walk goes from the Clock Tower
    along Avenue Habib Bourguiba by the Municipal Theatre and the cathedral, in by Bab el Bhar
    to the Zitouna Mosque and the Tourbet el Bey, by Dar El Bey on the Kasbah, the Hammouda
    Pacha Mosque, Dar Lasram, the Sidi Mahrez and Saheb Ettabaâ mosques, out by Bab el Khadra,
    by the Great Synagogue, up to the top of the Belvedere Park (8.9 km, 14 places);
    Alexandria's from the Catacombs of Kom el Shoqafa by Pompey's Pillar, Kom el-Dikka and the
    National Museum to the Bibliotheca Alexandrina, back along the Corniche to the Cecil Hotel,
    by the Eliyahu Hanavi synagogue, St Mark's and the Unknown Soldier Memorial, the Terbana
    and Abu al-Abbas mosques, to the Citadel of Qaitbay (11.8 km, 12 places), on the coast.
  - [x] **Dakar and Addis Ababa** (owner, 6 Oct 2026): Dakar's walk goes from the Museum of
    Black Civilisations by the station's Demba and Dupont, the Presidential Palace, the
    National Assembly and the Théodore Monod museum, the cathedral and the Daniel Sorano
    theatre, to the Grand Mosque, through the Médina by the Iba Mar Diop stadium to
    Soumbédioune on the Corniche, and up to the Cheikh Anta Diop University (11.6 km, 12
    places), on the coast; Addis Ababa's from Meskel Square by the Red Terror museum, Africa
    Hall, the National Palace and Menelik's, the Holy Trinity Cathedral, Arat Kilo, the
    National Museum (Lucy), the Yekatit 12 monument and the Guenete Leul Palace, down to St
    George's and the Taitu Hotel (11.9 km, 12 places).
  - [x] **Zanzibar and Luxor** (owner, 6 Oct 2026), the continent's ten, both short: Zanzibar's
    walk goes from Michenzani in Ng'ambo over Creek Road into Stone Town, by Christ Church on
    the old slave market, the Hamamni baths, St Joseph's and Freddie Mercury's house, to the
    Forodhani Gardens, the Old Fort, the House of Wonders and the Sultan's Palace, to the Old
    Dispensary (4.3 km, 11 places), on the coast; Luxor's from Luxor Temple by the Abu Haggag
    Mosque, the Mummification Museum and the Luxor Museum, along the Avenue of Sphinxes by the
    Precinct of Mut and the Temple of Khonsu, to Karnak (4.2 km, 8 places).
- [x] On a device (owner): the Camino Portugués drawn in both themes; Rome, Paris, Madrid,
  Berlin, Vienna, Lima, Cusco, New York, Rio, Mexico City, Buenos Aires, San Francisco, Québec,
  Havana, Cartagena, Tokyo, Sydney, Seoul, Beijing, Hong Kong, Singapore, Bangkok, Kyoto, Hanoi, Melbourne, Cairo, Cape Town, Marrakech, Tunis, Alexandria, Dakar, Addis Ababa, Fez, Zanzibar, Luxor, Porto, Amsterdam and Prague each walked once, with the voice; the continents' maps and the coastal cities in both
  themes. Done by the owner in the week of 5 Oct 2026.

Built as the content of the first two parts, with nothing new in the code: the fifth way is the
Caminho Português's main relation (12786090), whose line the script takes from Porto's
cathedral to Santiago, 245.3 km with 12 stages and Valença as a place; its locator is the
Camino Francés's (Spain and Portugal on one peninsula, `IBERIA` in the script). Rome (9.6 km,
15 places, the Colosseum to St Peter's), Paris (10.4 km, 14, Place des Vosges to the Eiffel
Tower) and Madrid (9.6 km, 14, the Temple of Debod to the Puerta de Alcalá) are routed by
BRouter and committed in `tools/walks/`; behind them the Tiber, the Seine, the Manzanares and the
Retiro's pond, and the cities' largest parks. Every sentence was checked in two Wikipedias where
they agree (English and Italian, French, Spanish or Portuguese), and rewritten where they do not
(the Spanish Steps lose their count of steps: 135 in one, 136 in the other). The Ways page lists
the cities in the owner's order: Milan, Rome, Paris, London, Madrid, then Lima and Cusco. Lima
(10.1 km, 15 places, Plaza San Martín to the Parque de la Reserva) crosses the Rímac to the
Alameda de los Descalzos and back, three places more than proposed so that it reaches 8 km;
Cusco (9.4 km, 12 places) has no water, since its rivers run in channels and the Huatanay begins
south of the map, and one place without a sentence (the San Pedro market: no source to check one
against; it has one since 6 Oct 2026, below). Both share a locator of Peru. Porto (5.3 km, 10 places), Amsterdam (5.2 km, 9) and
Prague (5.2 km, 9) are the same content in the shorter length; the Ways page lists them after
Madrid, so Europe stays together. Their sentences were checked in two Wikipedias each (English
and Portuguese, Dutch or Czech), and where those disagree the number goes (the steps of the
Clérigos tower, the tiles of São Bento, whether the Powder Tower ever held powder); the Ribeira
has none, with no second source found. Porto's first sentence names the Camino Portugués,
which in Passo starts from that cathedral. Amsterdam's canals are hundreds of water areas cut
at every bridge, so the script reads them from the street tiles it already fetches
(`water_from_tiles`); Prague's Old Town adds its longer lanes, as Milan's and Rome's centres
do. The other walks' data is kept as committed: a fresh fetch moved the Via di Francesco by a
metre and a few of Paris's streets (OpenStreetMap edits since), not part of this change.

### Phase 13 — Interval walk («Camminata a intervalli»)

The Japanese Interval Walking Training (Nemoto et al., *Mayo Clinic Proceedings*, 2007):
sets of 3 minutes of slow walking followed by 3 minutes of fast walking, five sets or more, as an
outing. **In minutes** (owner's request), faithful to the protocol. The live part is the hard
one: a change of interval must be felt on time with the phone in a pocket and the screen off,
and Passo has no timer then. The analysis and the options are in
`docs/adr/0013-interval-walks.md` (accepted 1 Oct 2026: option A, adaptive latency; it amends
ADR 0009 and §9.7 in the change that builds it).

- [x] The plan: a new goal kind, intervals: slow minutes and fast minutes (1 to 5, 3 by default), sets (3 to 10, 5 by default), the fast pace (brisk 100 by default, vigorous 130, running 140). It starts slow, as the protocol does. The goal is the end of the last set. The 25, 50 and 75% signals are off for this kind: the changes are the signals. A fourth preset, «Camminata giapponese» / "Japanese walking", 5 × (3 + 3).
- [x] The clock: the minutes are minutes in motion, as every outing's (ADR 0013): a stop at a traffic light does not eat a fast interval.
- [x] Domain (`:core:domain/sessions`): `IntervalSchedule` (where each change falls), the tracker's splits (each interval's steps, time in motion and time at its pace), the change found inside a batch from the steps' own timestamps; several changes in one batch tell the latest only.
- [x] The signals: two new vibrations, "faster" and "slower", unlike the five there are now (1, 2, 3 short; the goal's long one; the stillness's two long ones), chosen with the owner in the editor's "Try them". The voice, if the outing speaks: «Veloce, 3 minuti», «Lento», «Ultima serie veloce», and at the goal how many fast intervals were at pace.
- [x] The sensor during an interval outing (ADR 0013, option A): the wake-up counter at 30 s, and at 2 s from 40 s of motion before each change; a change told at the report that reaches it, or at the one whose predicted change falls before the next report; back to 30 s after. ADR 0009, §9.7, VISION.md's battery criterion and CLAUDE.md's invariant amended in the same change. With the screen on, the ticker the screen already allows (§9.4) shows the countdown to the next change.
- [x] The notification: on Android 16 the `ProgressStyle` bar in segments, slow and fast in two colours, with a point at each change, and the title saying the interval («Veloce · 1:40»); below, the expanded text says the same.
- [x] The result: each fast interval's cadence against its pace, and the sentence («4 fast intervals of 5 at pace»); the card, History and Today's list show it. Storage: a `session_interval` table (outing, index, slow or fast, steps, time in motion, time at pace), by an auto-migration; the backup carries it.
- [x] A phone without a wake-up step counter: the kind stays, and the editor says that the changes come on time only with the screen on (ADR 0013).
- [x] The diagnostics log: one row per change told, with how late it was against the step that crossed it, so the field test measures the delay instead of guessing it.
- [x] Strings in English and Italian; tests (`IntervalScheduleTest`, the tracker's, the editor's UI); CHANGELOG; the guide's outings chapter.
- [ ] On a device (owner): a 30-minute interval outing with the screen off: each change felt, its delay read from the log, the battery check of §9 (numbers in `docs/battery/`).
  - *First run, 4 Oct 2026* (`docs/battery/2026-10-04-interval-walk.md`): 3 sets on the owner's
    Samsung, which has no wake-up step sensor of any kind. The changes into a slow interval came
    on time, those into a fast one 33 to 127 s late, the goal about 49 s late: option A cannot
    do better there. It also showed the outing pricing its distance with the running step on
    clumped samples (fixed: the cadence over the time in motion). The goal is now logged too.
    The owner's decision for such phones (same day): the processor kept awake while an outing
    with signals counts (§9.7 amended, ADR 0013). Still to do: a second walk with it, its delays
    and the battery check.
  - *Second run, 5 Oct 2026* (`docs/battery/2026-10-05-interval-walk.md`): with the processor
    kept awake, every change told 1.3 to 2 s ahead, the wake lock let go at the stop; the
    system's battery page showed Passo at 0% for the day. One change fell during a phone call
    and was not heard: a sentence due during a call is now said when it ends. The log keeps one
    row an interval outing (and one for the wake lock), not one a change.

Built as planned, with these choices made on the way (none changes the ADR):

- *The editor:* a "Kind" choice at the top, «One goal» or «Intervals», rather than a fifth
  segment in the goal row (five labels do not fit a small phone, nor twice the text size).
  Intervals open with two short paragraphs on what the Japanese walk is and how Passo walks it
  (the owner's request, 3 Oct 2026); then the slow minutes, the fast ones and the sets, each with
  a step down and up (three sliders would crowd the page), the outing's shape as blocks, its
  length, and "As the study did it" back to 5 × (3 + 3). The fast pace is never free; the shares
  of the goal are not offered. A plan keeps its sets with any kind (`slowMinutes`, `fastMinutes`,
  `sets`, defaults 3, 3, 5), so switching away and back loses nothing; its value is the minutes
  of all the sets.
- *The preset* is named in the reader's language like the others: «Camminata giapponese» /
  "Japanese walking" for the protocol's three and three minutes, «Camminata a intervalli» /
  "Interval walk" for any other cut. Installations seeded before it get it once, after their
  own plans (`interval_preset_seeded`), unless they already made an interval plan.
- *The vibrations* are proposed, for the owner to confirm in "Try them" on a device: "faster" is
  four quick taps (70 ms on, 80 ms off), "slower" a long pulse and a short one (450, 250, 120 ms).
  Neither is a count, a single long pulse or two long ones.
- *The voice:* «Veloce, 3 minuti», «Lento», «Ultima serie veloce, 3 minuti»; the start says the
  sets and «Si comincia piano»; the goal says how many fast intervals were at pace.
- *At pace* means the fast interval's own cadence (its steps over its time in motion) at or above
  the pace; only fast intervals walked for at least half their length are judged, so the last one
  of an outing stopped seconds into it says nothing either way. The outing's time at pace is its
  fast minutes' only.
- *The countdown* on a screen carries on from the last step for at most 5 s
  (`COUNTDOWN_GLIDE_MILLIS`): the counter hands over steps in clusters, and a countdown that waited
  for each would stutter; a longer stop holds it. It never shows the next interval before its
  change is told.
- *Several changes in one batch:* the tracker cuts the splits at every change and keeps the
  latest; the service tells it once the batch of samples is all in (a job dispatched after it),
  so a burst of samples crossing two changes is felt once.
- *The result* is on the outing's card (a bar per fast interval, its cadence, the pace's line),
  in the outing lists of Today and History («4 fast intervals of 5 at pace»), in the CSV
  (`intervals`, `fast_at_pace`) and in the backup file (`intervals`, `splits`: added fields, no new
  version). Storage: schema v6, the sets on `session_plan` and `session`, the splits in
  `session_interval`, by an auto-migration.

**Acceptance:** with the screen off and the phone in a pocket, every change is felt within the
delay ADR 0013 promises, on the owner's phone; the battery cost of a 30-minute interval outing
is measured and within the ADR's estimate.

**Edge cases** (`IntervalScheduleTest`, `SessionTrackerTest`):

| Case | Expected behavior |
|---|---|
| A stop during a fast interval | The interval waits: minutes in motion |
| One batch crossing two changes (a long stillness of the reports) | Only the latest is told; both splits are right |
| Paused, resumed | The interval goes on where it was |
| 15 min without a step | Ends as every outing, with "Resume" |
| The last change | Is the goal: the goal's long vibration, not "slower" |
| A fast interval below its pace | Counted, said as below pace, never hidden |
| The screen turned on mid-interval | The countdown appears at the right value |

### Phase 14 — More ways, beyond Europe («Altri cammini»)

The owner (16 Oct 2026): the five ways are all pilgrimages in Italy, Spain and
Portugal, while the city walks already cover five continents. This phase widens the ways to
other traditions and to treks walked for the mountains and the sea, and takes "pilgrim" out of
the words that describe the whole feature. Data first, through the same script and the same
rules (`tools/CITY_WALKS.md`: two at a time, every sentence checked in two sources); the code
changes are small. Decisions in `docs/adr/0014-ways.md` (decision 16 onward).

**The name stays.** «Cammini» / "Ways" is already neutral, and it is the page, the
notification channel and the guide's chapter. A louder name ("Epic Trails") does not fit
Passo's voice (a sentence before a number, estimates that say so). What changes is the
sentences that call every way a pilgrim route.

**The new ways** (lengths approximate, the line's own once drawn; at an ordinary 5 km a day):

| Way | From, to | About | At 5 km a day | Kind |
|---|---|---|---|---|
| Kumano Kodō, the Nakahechi | Takijiri-ōji, Kumano Hongū Taisha, Kumano Nachi Taisha | 70 km | 2 weeks | pilgrimage; twinned with the Camino de Santiago |
| Inca Trail, the classic route | Km 82 (Piscacucho), Machu Picchu by the Sun Gate | 42 km | 9 days | historic; ties to the Cusco walk |
| Great Ocean Walk | Apollo Bay, the Twelve Apostles | 105 km | 3 weeks | coastal trek |
| John Muir Trail | Yosemite Valley, Mount Whitney | 340 km | 2½ months | mountain trek, the heart of the Pacific Crest Trail |
| Kilimanjaro, the Machame route | Machame Gate, Uhuru Peak, Mweka Gate | 60 km | 2 weeks | mountain trek, Africa's highest summit |

Not the Appalachian Trail (3,500 km) nor the whole Pacific Crest Trail (4,265 km): about two
years at 5 km a day, twice what the Francigena was cut to avoid (§15, 1 Oct 2026). The John
Muir Trail is the PCT's most walked stretch at a length a reader finishes. Kilimanjaro is the
one more the owner chose (16 Oct 2026) so that every continent of the cities has a way: ten
ways, five in Europe and five beyond, and no more, since each costs its sentences in two
sources and Passo is a step counter, not an atlas. Not the Shikoku
Henro (1,200 km and 88 temples) nor the Kailash kora (a loop, at the edge of a disputed border)
(owner, 16 Oct 2026); nor a "lifetime" way for heavy walkers: Passo is not made for that
reader (owner).

- [x] **Words: no faith named** (owner, 16 Oct 2026, a standing rule, also in CLAUDE.md). What
  describes the Ways never names a religion nor calls a way devotional or the reader a pilgrim:
  no "pilgrim", "pilgrimage", "sacred", "holy", "religious" or a faith's name, in English or in
  Italian («pellegrino», «pellegrinaggio», «sacro»...). The phase rewrites the ones already
  there: the app's strings about the Ways (the Ways page's intro, the guide's chapter, whose
  first paragraph no longer lists the ways by name, so a new one does not make it wrong, as was
  done for the cities; Insights' door; the notifications), the README (feature line and
  screenshot caption), VISION, CHANGELOG's unreleased entries, PLANNING's Phase 11 introduction
  and CLAUDE.md. A grep for those words over the strings and these documents comes back empty
  but for the places' sentences. A place's own sentence may say what the place is (a cathedral,
  a temple, a shrine) and a fact of its history, as a guidebook does, including the pilgrims who
  walked or walk by it, then or today (owner, 17 Oct 2026: the stone left at the Cruz de Ferro,
  the bridge out of Mansilla; written in CLAUDE.md); the new ways' sentences follow the same
  rule. The ADRs and §15 are records and stay as written.
- [x] **The stamps' group.** «Credenziale» / "Credential" is a pilgrim's word and reads oddly on
  a trek: it becomes «Timbri» / "Stamps" for every way (owner, 16 Oct 2026), in the page, the
  guide and the strings that name it.
- [x] **The Ways page by continent**, as the cities are: Europe, Asia, Africa, the Americas,
  Oceania,
  each by length. Done with the cities' own four groups (Asia and Oceania together; §15). Ten ways in one list would hide the new ones under the five old ones; the
  groups show at a glance that the ways are no longer European only.
- [x] **The script**, only if a new way needs it: a way whose relation is incomplete or missing
  is routed by BRouter between its stops, as a walk is (likely the Inca Trail; to be checked on
  Waymarked Trails first). Not needed for any of the five: every relation was whole (§15), so
  BRouter routing for a way was never built. What the ways did need is in: `fetch` and `splice`
  take a way's id as they take a walk's, so a way is added without fetching or moving the others;
  and a way may name stops its line goes `through` (Kilimanjaro's summit, up one route and down
  another; ADR 0014 decision 18).
- [x] **The data, two ways at a time**, each drawn and checked on its own, added to `WayData.kt`
  without moving the other ways (`fetch` and the splice, as for the cities):
  1. [x] Kumano Kodō and the Great Ocean Walk (short, well mapped, sources easy to find): 70.6 and
     96.4 km, nine stages each;
  2. [x] the Inca Trail and the John Muir Trail: 33.2 and 335.7 km, eight and twelve stages;
  3. [x] Kilimanjaro (its camps as the stages: Machame, Shira, Barranco, Karanga, Barafu, the
     summit, Mweka; Lava Tower a place on the way): 56.9 km, up by Machame and down by Mweka.
  Each: its relation (or route), stops snapped within 2 km and in order, rivers, lakes and parks
  for its map, the locator (JP, PE, US, AU, TZ already exist),
  names and sentences in English and Italian. Japanese names in Hepburn romanisation, with
  macrons, as the English Wikipedia writes them.
- [x] Network: the build's `fetch` needs OpenStreetMap, Waymarked Trails, BRouter, Natural Earth
  and Wikipedia; the cloud environment must allow them (the default one does not). Reached from
  the cloud sandbox on 16 Oct 2026 (Wikipedia and OpenStreetMap's API throttle: the scripts
  wait and retry).
- [x] Tests: the data tests count the new ways and stops; `WayMapView`'s gallery draws each new
  map in both themes; the page's UI test with the continent groups (`assertAccessible()`,
  `walkPage()`).
- [x] README: the feature line and the caption without "pilgrim"; one new screenshot if a new
  way is worth showing (proposal: the Kumano Kodō under way). CHANGELOG. Done: `way-kumano.png`,
  the Kumano Kodō two weeks in, and `ways-continents.png`, the Ways page by continent.
- [ ] On a device (owner): the new maps in both themes and on an open foldable; the Ways page
  with its groups at twice the text size.
