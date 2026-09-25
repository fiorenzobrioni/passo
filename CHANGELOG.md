# Changelog

All notable changes to Passo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [0.1.0]` for
`v0.1.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Added

- The project skeleton (PLANNING.md Phase 0): the module layout, the convention plugins, the
  Material 3 theme with dynamic color, English and Italian through the system per-app
  language picker, and a launchable app with nothing in it yet.
- A build that refuses any network, location, exact-alarm or body-sensor permission, checked
  on every push.
- Step counting in the background (Phase 1): the phone's hardware step counter, read by a
  foreground service that starts again by itself after a reboot or an app update, and saves
  the count before a shutdown. Steps are kept per minute, on the phone only, and survive
  reboots, sensor resets, midnight, time-zone and clock changes.
- A notification with today's steps, and a first screen that asks for the "Physical activity"
  permission (or explains a phone without a step counter).
- Distance, active calories, active minutes, brisk minutes and cadence for every day (Phase 2),
  estimated from the steps and an optional profile (height, weight, step length), with the
  sources of every formula in the code. Metric and imperial units, and numbers written the way
  your language writes them.
- A daily goal that is kept with each day. Changing your weight, step length or goal updates
  today only: days already over keep the numbers they had, unless you ask to apply the new
  profile to the past.
- The Today screen (Phase 3): a ring that shows the day's steps against the goal and where a
  usual day of the same weekday stands at this hour, one sentence that says how the day is
  going and what is left, a chart of the day you can read with a finger, and distance,
  calories, active and brisk minutes and cadence, each with what it means. It counts live while
  it is open.
- A short first run (welcome, profile, goal, permissions, and a battery tip on the phones that
  need one), and Settings: profile, goal, units, the usual-day line, theme, palette and
  typeface (Google Sans, Inter or the system's, as in Chiaro), language, pausing the count.
- Two home-screen widgets (Phase 4), in the same dress as Chiaro's so the two apps' cards sit
  side by side: «At a glance» (today's ring, with a notch where a usual day stands by now, the
  count, the goal, the day's sentence, and on a wide tall card today's steps hour by hour) and
  «In words» (the day in type alone: the count large, the sentence, the goal, distance and
  calories, and on a tall card the day in figures). Both resize from one cell up and lay
  themselves out for every size.
- Each widget has its own settings, from a long press on the home screen: light, dark, the
  phone's own or one of six colours, any opacity, what it shows, and which side the ring is on,
  with the card itself as a live preview at every size.
- A widget whose count is not moving says so: paused (a tap resumes it), stopped by the system
  (a tap restarts it) or without the permission (a tap opens the app to allow it).
- The widgets repaint when the screen comes on, at most once a minute while it stays on, and
  never while it is off. If today's steps cannot be read, a widget says so instead of loading
  forever.
- Passo now looks like Chiaro: the same colors, typefaces, shapes and motion.
- The app icon: Chiaro's ring in green, with a footprint where Chiaro has its sun, so the two
  sit side by side on a home screen as one family. Themed icons (Android 13+) get the same
  drawing.
- History (Phase 5): a day, a week, a month or a year at a time, swiped or stepped through
  from the first day Passo counted to today. A day shows its steps hour by hour with the walks
  marked on the chart, its measures and its walks; a week, a month and a year show their bars
  against each day's own goal, how many days met it and how the period compares with the one
  before; a month adds a calendar of how close each day came to its goal. Touch a bar or a day
  to read it and open it.
- Walks, found on their own in the steps already counted (Phase 5): stretches of steady walking
  or running, with their time, length, steps, distance, cadence and calories, on Today and in
  History. No extra sensor and no background work; they can be turned off, and the shortest
  one counted is 5, 10 or 15 minutes.
- Insights (Phase 5): the streak of days at your goal with the last seven days, your best day,
  week and month and longest streak (each opens in History), averages over the last 7 and 30
  complete days, and what it all adds up to since the first day.
- A bottom bar for Today, History and Insights, and the first day of the week in Settings.
- The counting notification, expanded, shows the day (Phase 6): how far the goal is, or when it
  was reached, the share of the goal with the active minutes, the estimated distance and
  calories, and a bar towards the goal. Updated only while the screen is on, as before.
- Settings says how the counting notification shows and opens Android's page for it, where it
  can be minimized (no icon in the status bar) or turned off. Passo keeps counting either way.

### Changed

- Android's own backup and phone-to-phone transfer now carry exactly the step history, the
  profile and the settings, and nothing else. Passo still has no internet permission and sends
  nothing itself; after a restore it starts counting again from zero steps, so the new phone's
  earlier steps are never added in one go.
