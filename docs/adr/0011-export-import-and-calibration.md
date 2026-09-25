# ADR 0011: The export, the import, and the step measured by walking (Phase 7)

- Status: accepted
- Date: 2026-09-25

## Context

Phase 7 asks for two things VISION.md promised from the start:

- **Data**: "Export to CSV and JSON through the Storage Access Framework, so no storage
  permission is needed. Import from a JSON backup." ADR 0007 already names the export as "the
  dependable way to move history", since Android's Auto Backup skips an app whose foreground
  service is running, which Passo's is all day. The phase's acceptance: an export followed by
  an import on a clean install reproduces the history exactly.
- **Calibration**: "walk a known distance and the app computes the step length, without GPS."

The owner asked for a clean implementation, above all in UI and UX, as for Phases 3, 5 and 6.

## Decisions

### The backup file

1. **One JSON file, the whole of what a new phone could not rebuild**: every minute with its
   recorded day, every day's summary as it was frozen, the outings and their plans, the profile
   and the settings the reader chose. Written by `BackupCodec` in `:core:domain`, with
   kotlinx.serialization (already in the catalog for the navigation routes: no new
   dependency), over DTOs kept apart from the model so the model can change without changing
   what a file says.
2. **What stays out**: the tracker state (one sensor's counter in one boot session; ADR 0007's
   rule, now for files too), whether counting is on and whether the first run is done (this
   phone's business, not the reader's history). The diagnostics log goes **out**, so a field
   problem can be read from the file (PLANNING.md §15 anticipated "an export in Phase 7"), and
   never back **in**: it is the log of the phone that wrote it.
3. **A readable, versioned format**: `format: "passo-backup"`, `version: 1`, raised only for a
   change an older reader would misread (a reader skips fields it does not know). Instants are
   epoch milliseconds (`…AtMillis`), days ISO dates, a minute `[epochMinute, steps]` nested under
   the day it was recorded on, lengths in metres, enum values by name. A file cut short says it
   is **damaged**; a file from a newer Passo asks for an **update**; anything else is **not a
   Passo backup**. Nothing is written until a file has been read whole and checked.
4. **Reading is lenient where dropping costs no truth** (an unknown setting reads as its default,
   a minute with a negative count is dropped, a repeated day keeps its fuller copy), strict where
   it would (format, version, broken JSON). A file over 64 MB is not read: a year is a few MB.

### The import: merge, never replace

5. **An import adds and never takes away.** The common case is a new phone that has already
   counted a few hours when the old phone's file arrives; replacing would lose them. So
   (`BackupMerge`, pure and tested):
   - a **minute** is the larger of its two counts, never their sum: a minute walked with two
     phones in the pocket is one minute walked. It keeps the day this phone recorded it on;
   - a **day only the file has** comes in as the file had it, summary included: this is what
     makes the acceptance's exact round trip true. A day the file had still open (the export
     day) is frozen with the file's profile, the one it was being measured with;
   - a **day both have**, where the file adds steps: a frozen day of this phone keeps its own
     summary and goal and takes only the added steps' share (`DaySummaries.withLateSteps`, the
     rule for steps that reach a frozen day late); an open one is recomputed, as every write does;
   - **today** follows the current profile and goal;
   - **plans** with the same name, goal, pace, signals and voice are the same plan; **outings**
     started at the same millisecond are the same outing; one under way in the file is closed at
     its last step.
   The merge is idempotent and never deletes, so an import needs no destructive confirmation,
   only a preview; importing the same file twice changes nothing (tested on two real databases).
6. **Profile and settings come with the file by default, as one choice** ("Also take its profile
   and settings", on). They go through the repository's usual path, which freezes this phone's
   past days first: an import never silently rewrites a past day's estimates.
7. The days are written under the tracking repository's lock, in one transaction, after the
   service's buffer has been caught up (`TrackerLink`), so no batch lands between the read and
   the write and the service's pending minutes meet the file's in the database. Widgets repaint
   once afterwards.

### The spreadsheet tables

8. **Three CSV tables, one per file**: days, minutes, outings. One CSV holds one table; a zip of
   three would not open in a phone's spreadsheet app, and a folder would need a directory grant.
9. **RFC 4180, fixed English headers with the unit in the name** (`distance_km`), a dot for
   decimals, CRLF, UTF-8 with a byte-order mark (Excel reads an accented outing name right
   only with it). Distances follow the reader's units, which the header says; times of day are
   local, and the minutes table carries the UTC instant too. A field that starts like a formula
   (`=`, `+`, `-`, `@`) is written as text: an outing's name is never run by a spreadsheet.
10. The JSON is for Passo, the CSV for people: the CSV cannot be imported, and the error says
    so.

### The screen

11. **A "Your data" group in Settings**, three rows, each saying what it carries before it is
    tapped ("412 days and 38 outings, to the minute, with your profile and settings"). The
    system's file picker chooses where a file goes and which one is read (`CreateDocument`,
    `OpenDocument`): no storage permission, and Passo still sends nothing anywhere. The privacy
    note now says "unless you export them to a file yourself".
12. **An outcome is a card, not a toast** (Chiaro §8.2): over the rows, with the file's name and
    what it holds, until the reader puts it away. An import shows, before anything is written,
    when and by which Passo the file was made, its days and their span, its outings, the merge
    in one sentence and the profile-and-settings choice.

### The step calibration

13. **The hardware counter read directly** (`StepCounterProbe`): its value at Start and at Stop.
    The counter is cumulative, so the difference is exact however the steps were delivered in
    between (in a pocket with the screen off, batched in the sensor hub), and independent of the
    tracking service, a pause or midnight. At Stop the sensor is flushed (bounded at 1.5 s, like
    the shutdown flush) before the value is taken.
14. **Battery**: the listener exists only while the page is on screen (the lifecycle's STARTED
    state), the same non-wake-up sensor, no batching because the processor is awake anyway, and
    no wake lock. The clock ticks once a second only while a walk is timed and the page is
    visible. With the screen off nothing of it runs; the steps are in the counter all the same.
    Start, the distance and the step survive the process being stopped in the background
    (`SavedStateHandle`; the counter keeps its count until a reboot, which the result detects).
15. **The rules** (`StepCalibration`, pure and tested): at least 30 steps; the result inside the
    lengths the app accepts (`ProfileLimits`), otherwise it is refused with its arithmetic
    ("100 m in 20 steps would be 500 cm a step"); a counter that went back is a restart, said.
    A walking step measured at a running cadence (140 spm), or a running step below it, is said
    and not refused: the length is still what the reader walked.
16. **Walking or running step**, chosen on the page. The walking one is stored as
    `StepLengthMode.CALIBRATED` (Settings then says "Measured"); the running one as the reader's
    own. Saving goes through the profile path: today follows, past days keep theirs, and the
    page says so.
17. **The distance is picked, never typed** (Chiaro's rule for a value with a range): 50 m to
    2 km by 10 m, or 50 to 2,200 yards by 10, 100 by default, with a track lap and a football
    pitch as references in the reader's units.
18. **Reached from Settings**: a "Measure your step" row in the profile, and a "Measure it by
    walking/running" button in each step-length dialog. Saving an unchanged measured length from
    that dialog keeps it measured and unrounded.

## Consequences

- `:core:domain` applies the kotlinx.serialization plugin; the module stays pure Kotlin/JVM.
- `MeasureUnit` gains `METER` and `YARD` (`MeasureFormatter.shortDistance`).
- No schema change: the import writes the existing tables. No new permission.
- The file's shape is now a contract: a change an older Passo would misread raises `version`.
- To confirm on a device (owner): an export and an import between two phones through a file
  app and a cloud folder, and a calibration walk with the phone in a pocket and the screen off.
