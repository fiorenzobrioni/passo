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
