# ADR 0007: Backup is a decision, and the privacy line says what happens (Phase 5)

- Status: accepted
- Date: 2026-09-25

## Context

The owner asked whether `android:allowBackup="true"` should be declared, as in Chiaro, and if
it changes the "no internet" rule, to change the rule and every place that states it.

What was true before this ADR: the manifest did not mention backup, and `allowBackup` defaults
to **true**. Passo was already backed up, whole, by default: every file it owns, including the
tracker state. That state (boot count, where the hardware counter stood) describes one phone's
sensor in one boot session. Restored onto another phone, or onto this one after a reinstall,
the first sample would have been read as a new boot session and added every step the counter
had since that phone started: steps from before Passo was there.

## Decisions

1. **Backup stays on, declared, with an allowlist** (`res/xml/data_extraction_rules.xml`): the
   database (`passo.db` and its write-ahead log) and the settings file
   (`files/datastore/settings.preferences_pb`), for both the cloud copy and the phone-to-phone
   transfer. The step history is the one thing a new phone cannot rebuild, so it is the thing
   worth carrying. The widgets' looks are left out: they are keyed by widget ids, which a
   restore renews.
2. **Not `allowBackup="false"`**: on Android 12 and later some makers still run the device
   transfer. An instruction whose effect depends on the maker is not a decision (Chiaro's
   reasoning, 21 Sep 2026).
3. **A restored tracker state is dropped.** The tracker state cannot be cut out of the database
   file, so it travels, and the service drops it on the first start of an installation that did
   not write it (`TrackingRepository.adoptTrackerState`): the installation is the app's
   first-install time, which a restore or a reinstall renews and an update keeps, stored next to
   the settings so that it travels with the database. A state from another installation is
   deleted, and the first sample is a baseline, as on a first run (a `RESTORED` line goes to the
   diagnostics log). For a backup made by a build that did not record it, a state written
   before this installation existed cannot be its own either. Robolectric tests cover the four
   cases.
4. **The rule does not change.** Backup needs no permission: Android sends the copy, not Passo,
   and only if the reader turned backup on; since Android 9 it is end-to-end encrypted with the
   phone's screen lock. `INTERNET` stays forbidden and the CI gate stays as it is. What changes
   is the wording: "your steps never leave the phone" was not true to the letter with backup on
   (and had not been since the first build), so the app now says what Passo does ("keeps your
   steps on the phone and sends them nowhere"), and Settings' privacy note says what Android's
   backup does. VISION.md and the README already named Android's backup as the one way out
   besides an export.

## Consequences

- Android's Auto Backup skips an app while it runs in the foreground, and a foreground service
  counts as foreground (`android:backupInForeground`, default false). Passo's counting service
  runs all day, so the cloud copy is taken mostly when counting is paused or stopped. Setting
  `backupInForeground="true"` would let the backup kill the service at night to copy the
  files; with the "no lost steps" principle, not worth it. The export (Phase 7) is the
  dependable way to move history; backup is a convenience on top.
- Auto Backup's quota is 25 MB per app; the database grows by a few MB a year (§5), well under
  it for years. Past it, Android stops the cloud copy, not the app.
- To be verified on a device in Phase 7 with `adb shell bmgr backupnow` and a restore, together
  with the export and import round trip.
