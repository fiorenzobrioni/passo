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
