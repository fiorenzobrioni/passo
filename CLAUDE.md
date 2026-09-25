# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in
this repository.

## What this repository is

**Passo** ("step") is a private, battery-friendly Android pedometer (Kotlin / Jetpack Compose
Material 3, Glance widget). It counts every step with the phone's own hardware step counter,
even if the app is never opened, keeps the data on the device, and derives everything else
from those steps: distance, active calories, active and brisk minutes, goals, streaks, walks,
trends. No account, no ads, no internet permission at all. It belongs to a small family of
single-purpose apps by callbackdev, with **Chiaro** (weather) and **Saldo** (finance): same
developer identity, same build and release setup, and the same visual language.

Source of truth:

- `VISION.md`: the product. Principles, v1.0 scope, non-goals, key decisions, success criteria,
  glossary.
- `PLANNING.md`: the architecture, the tracking engine design (§4), the data model (§5), the
  metrics (§6), widget (§7), battery rules (§9), permissions (§10), and the phased plan with
  checkable steps (§11). **Keep it updated as work progresses**: tick the boxes, and record
  every decision and deviation with its reason in §15 (and as an ADR in `docs/adr/` when it
  is architectural).

Work one phase at a time, starting from the phase's section in PLANNING.md §11.

## Build and commands

Stack: Kotlin 2.4 (compiled by AGP 9's built-in Kotlin), Compose Material 3, Hilt, Room,
DataStore, Glance; Gradle 9.8 / AGP 9.4, version catalog in `gradle/libs.versions.toml`,
convention plugins in `build-logic/`. Package/applicationId: `com.callbackdev.passo`.
minSdk 34, compile/targetSdk 37. Java 21.

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- All unit tests: `./gradlew test`
- One module: `./gradlew :core:domain:test`; one class: `--tests "com.callbackdev.passo.core.domain.tracking.StepAccountantTest"`
- Lint (every module, via `checkDependencies`): `./gradlew :app:lintDebug`
- Format: `./gradlew spotlessApply` (CI runs `spotlessCheck`; rules in `.editorconfig`)
- Forbidden-permission check on the merged manifests: `./gradlew :app:checkForbiddenPermissions`
- Installable minified build: `./gradlew :app:assembleRelease -PsignReleaseWithDebugKey`
- On a machine with no system JDK, prepend `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`.
- **Maven Central answers HTTP 429** (Too Many Requests; it happens in the Claude Code cloud
  sandbox): add `--init-script gradle/google-maven-mirror.init.gradle.kts` to any command. It
  puts Google's mirror of Maven Central first, and points Robolectric's own `android-all`
  download at it, for that run only; nothing in the build changes.

**Convention plugins** (`build-logic/convention`): `passo.android.application`,
`passo.android.library`, `passo.android.compose`, `passo.android.feature`, `passo.android.hilt`,
`passo.android.room`, `passo.jvm.library`. SDK levels and the Java version live in
`PassoSdk` / `Passo.kt`; a module's own build file only says what is specific to it. The
forbidden-permission list is `ForbiddenPermissionPatterns` in `ForbiddenPermissions.kt`.

**Version**: `passo.versionName` in `gradle.properties` is the only place it is written;
`versionCode` is derived (major * 10000 + minor * 100 + patch). `release.yml` refuses a tag
that does not match it.

## Modules (PLANNING.md §2)

| Module | Kind | Holds |
|---|---|---|
| `:core:model` | pure Kotlin/JVM | data classes shared by everything |
| `:core:domain` | pure Kotlin/JVM | `StepAccountant`, metric calculators, streaks, records, `WalkDetector`, `TypicalDayCalculator` |
| `:core:data` | Android library | Room (steps, tracker state), DataStore (settings, profile), repositories exposing `Flow` |
| `:core:tracking` | Android library | `StepTrackingService` (FGS type `health`), sensor source, receivers, ongoing notification |
| `:core:designsystem` | Android library | M3 theme, typography, shared components, the Canvas charts |
| `:feature:*` | Android library | `today`, `history`, `insights`, `settings`, `onboarding` |
| `:widget` | Android library | the two Glance widgets («At a glance», «In words»), their settings screen, the update coordinator |
| `:app` | application | `Application`, `MainActivity`, navigation, DI entry points; wires everything |

Rules: `:core:model` and `:core:domain` stay pure Kotlin/JVM; if a class there needs a `Context`
or a `Resources`, it is in the wrong module. Feature modules depend on `core:*`, never on each
other. All business logic lives in `:core:domain`, with unit tests.

## Invariants (non-negotiable)

- **Never add a forbidden permission** (PLANNING.md §10): `INTERNET`, any `ACCESS_*LOCATION`,
  `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`,
  `HIGH_SAMPLING_RATE_SENSORS`, `BODY_SENSORS*`. The build fails on the merged manifest; a
  library that adds one is removed or patched with `tools:node="remove"`.
- **The hardware step counter is the only step source.** No Health Connect, Google Fit,
  Samsung Health or Play services for step data; no accelerometer.
- **Battery first** (PLANNING.md §9): never poll or run timers while the screen is off; no
  wakelocks (except inside `goAsync()` for the shutdown flush), no exact alarms, no periodic
  workers for tracking; database writes are batched. Widget, notification and tile update
  only when someone can see them.
- **No lost steps.** Don't change the tracking engine without updating the tests for every
  edge case in PLANNING.md §4.6.
- **Honest estimates.** Distance, calories and active time are shown as estimates; formulas
  live in `MetricsConstants.kt` with their sources.
- **Past days are frozen.** A profile change never silently rewrites history.
- **Every user-facing string is a resource, in English and Italian** (`values/`,
  `values-it/`), with plurals where counts appear. The app language follows the system
  per-app language picker (`locales_config.xml`).
- **No new third-party dependency without approval**, and its license must be
  GPL-3.0-compatible (Apache-2.0, MIT, BSD). Charts are custom Compose `Canvas` code, never
  a chart library.

## Code style

Kotlin official style (ktlint `intellij_idea` via Spotless), no `!!`, `Flow` over `LiveData`,
immutable UI state, MVVM with unidirectional data flow. Code and documentation in English.
Comments explain *why*, not what.

## Design

Passo keeps the same visual language as Chiaro, for the app screens and for the widget, so the
family reads as one (`docs/adr/0004-design-language.md`). `:core:designsystem` holds it as roles
(color, type, shape), never as hexes inside a composable: Chiaro's two generated dresses
(`theme/Scheme.kt`, copied, never hand-edited), Google Sans / Inter / system type, Chiaro's
shapes and springs, and every animation collapses to a fade under reduced motion. Its principles
hold here too: one sentence before any number, every number with the line that says what it
means, estimates that say so, no dead tab and no switch for a feature that has not shipped.
Icons are `PassoIcons`, drawn in code. The Compose UI tests write screenshots to each module's
`build/screenshots`: look at them after changing a screen.

**Widgets** (`docs/adr/0005-widgets.md`): Chiaro's card, colours and ink rule, carried over; the
forms' arithmetic is pure (`GlanceLayout.kt`, `WordsLayout.kt`) and pinned by tests at Chiaro's
reference grants, and `WidgetGalleryTest` draws every form to `widget/build/screenshots`: look at
them after changing a card. Glance drops the eleventh child of a container silently: count them.

**README screenshots** (`docs/screenshots/`, shown in the root `README.md`): drawn by the
`ReadmeScreenshots` test classes of the feature modules and `:widget`, from realistic sample
data, in English, and only on request: `./gradlew test -PupdateScreenshots` (plus the mirror init script in the
sandbox). Two standing rules (owner's):
- **Regenerate them** whenever a change alters what an existing one shows, and look at them
  before committing.
- **Add one** when a phase brings something worth showing (a new screen, the widget), with its
  caption in the README's table; keep the set small, the meaningful views only.

## Signing and CI

- **Debug signing**: `keystore/debug.keystore` is intentionally committed (alias `passo-debug`,
  passwords `android`) so debug APKs from CI and any machine share one signature. Do not
  regenerate it. Debug builds carry `applicationIdSuffix ".debug"`.
- **Release signing**: the real keystore lives OUTSIDE the repo; the `release` signingConfig is
  created only when the four `PASSO_KEYSTORE*` / `PASSO_KEY_*` properties are all set (from
  `~/.gradle/gradle.properties` locally, from `ORG_GRADLE_PROJECT_*` env vars in CI). Without
  them the release build is unsigned; `-PsignReleaseWithDebugKey` signs it with the debug key
  for testing only.
- **Temporary release key**: until the real key exists, `release.yml` signs tags with
  `keystore/temporary-release.keystore` (committed, public passwords) and forces a
  pre-release. `keystore/README.md` lists the steps to retire it. Never commit the real key.
- **CI** (`.github/workflows/android-ci.yml`, every push and PR): formatting, forbidden
  permissions, unit tests and lint run *before* the APKs; a red suite must never produce an
  installable artifact. **Release** (`release.yml`, on `v*` tags): the same gates, then the
  signed APK, its SHA-256 and the R8 mapping on a GitHub Release, with the body taken from
  the tag's `CHANGELOG.md` section (write it before tagging).

## Writing `README.md` (root file only)

**No em dashes (`—`) or en dashes (`–`) in the root `README.md`.** Rewrite the sentence rather
than swapping in a hyphen: use a colon when the clause explains, a full stop when the thoughts
are separate, parentheses for an aside. Same house style as Chiaro, deliberately scoped to
that one file: every other file keeps normal punctuation.
