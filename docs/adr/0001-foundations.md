# ADR 0001: Foundations (Phase 0)

- Status: accepted
- Date: 2026-09-24

## Context

Passo starts from an empty repository. Its build, identity, CI and release setup should match
the sibling app Chiaro, so the family is maintained one way; PLANNING.md §1-§2 and Phase 0
set the rest.

## Decisions

1. **Identity.** Developer namespace `callbackdev`, as Chiaro: package and applicationId
   `com.callbackdev.passo` (debug: `com.callbackdev.passo.debug`), modules under
   `com.callbackdev.passo.*`. Author Fiorenzo Brioni, license GPL-3.0 (full text in `LICENSE`).
   The repository is `fiorenzobrioni/passo`, not `passo-android` as VISION.md first proposed:
   the sibling repositories carry the bare app name.
2. **Latest stable toolchain at setup.** Gradle 9.8.0, AGP 9.4.1, Kotlin 2.4.20, KSP 2.3.12,
   Compose BOM 2026.09.00, Hilt 2.60.1, Room 2.8.5, Glance 1.2.0, Navigation 3 1.2.0. With
   AGP 9, Kotlin is compiled by AGP itself (built-in Kotlin): there is no `kotlin-android`
   plugin; the Compose compiler plugin brings KGP 2.4.20 onto the classpath and AGP uses it.
3. **Convention plugins in `build-logic/`** (PLANNING §1), unlike Chiaro, which repeats its
   Android block per module. Passo starts with twelve modules; the SDK levels, Java version,
   test libraries and the permission gate are written once.
4. **Java 21 without a Gradle toolchain.** Source/target 21 and `jvmTarget` 21, set directly.
   A toolchain would make every build look for, or download, a separate JDK 21 when the JDK
   running Gradle (Android Studio's, or CI's Temurin 21) already is one. Same reasoning as
   Chiaro, one Java version later.
5. **The permission gate is a Gradle task on the merged manifest** of every variant
   (`checkForbiddenPermissions`, wired to `check`, run by CI and by the release). Checking
   the merged manifest, not the app's, is what catches a library bringing in `INTERNET`.
   Verified by adding `INTERNET` and `ACCESS_COARSE_LOCATION` to `:core:tracking`'s manifest:
   the build failed naming both.
6. **Formatting: ktlint through Spotless**, code style `intellij_idea` (the Kotlin coding
   conventions), composables exempt from the function-naming rule.
7. **Tests: JUnit 4 + Truth + Turbine + coroutines-test**, Robolectric where Android is needed.
   JUnit 4 because Robolectric and the Compose test rule run on it, and it is what Chiaro uses.
8. **Signing, as Chiaro, plus a stand-in release key.** The debug keystore is committed for
   good (`passo-debug` / `android`). The real release key does not exist yet, so tag builds are
   signed with a committed temporary key and forced to pre-release; `keystore/README.md` has
   the steps that retire it once the real key is in GitHub Secrets. *Update, 26 Sep 2026:*
   the real key is in the secrets (the same four names as Chiaro's) and the temporary key
   is deleted; no release was ever signed with it.
9. **Versioning now, not in Phase 8.** `passo.versionName` in `gradle.properties` is the one
   place; `versionCode = major * 10000 + minor * 100 + patch`; the release workflow refuses a
   tag that does not match. It had to exist for the release workflow to be testable at all.
10. **Only English and Italian resources ship** (`localeFilters`): without it the APK carried
    the 80-odd languages of the AndroidX libraries.
11. **`failOnNoDiscoveredTests = false`** on Android unit tests. Hilt generates test-source
    stubs in every module it is applied to, so Gradle 9 read the empty skeleton modules as
    "tests present, none found" and failed.

## Consequences

- A release before the real key exists installs, but cannot be updated in place by a release
  signed with the real key. Such releases are pre-releases and say so in their notes.
  *Moot since 26 Sep 2026:* the real key arrived before any tag, so no such release exists.
- AGP 9 is newer than Chiaro's AGP 8.13: build snippets from Chiaro need translating to the
  new DSL (no `kotlin { }` block from a Kotlin Android plugin, `CommonExtension` without type
  parameters).
