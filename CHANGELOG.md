# Changelog

All notable changes to Passo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

## [1.0.0] - 2026-10-01

**The first release.** Passo counts every step with the phone's own step counter, even if you
never open it, and keeps everything on the phone. No account, no ads, no tracking, and no
permission to use the internet at all.

Android 14 (API 34) or newer, with a hardware step counter. Check the download with the
`.sha256` file beside the APK, and the signing certificate against the fingerprint in the
[README](https://github.com/fiorenzobrioni/passo#install).

### What is in it

- **Today**: your steps against the goal, where a usual day stands by now, and one sentence on
  the day.
- **Your day**: a chart you read with a finger, plus distance, calories, active and brisk
  minutes, cadence. All estimates, and they say so.
- **History**: a day, week, month or year at a time, and a calendar of how close each day came.
- **Walks, found for you** in the steps already counted.
- **Outings**: a walk or run with a goal, with vibrations or a voice at the milestones.
- **Insights**: your streak, your records and your averages.
- **Measure your step**: walk a distance you know, no GPS.
- **Two widgets** and a **Quick Settings tile**.
- **Notifications**, each optional: goal reached, an evening reminder, a weekly summary.
- **Your data**: a backup to a file, CSV for spreadsheets, an import that never deletes.
- **The guide**, light and dark themes, two palettes, three typefaces.
- **For every reader**: TalkBack, text up to twice its size, foldables.
- **English and Italian**, through the system per-app language picker.

### Private and light

- No `INTERNET` permission: Passo cannot send anything.
- No Health Connect, Google Fit or Play services: the step counter is the only source.
- No timers with the screen off; about 1% of a day's battery.

The full development record is in
[docs/CHANGELOG-1.0.0.md](https://github.com/fiorenzobrioni/passo/blob/main/docs/CHANGELOG-1.0.0.md).
