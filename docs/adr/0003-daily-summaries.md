# ADR 0003: Daily summaries, the profile and the frozen past (Phase 2)

- Status: accepted
- Date: 2026-09-24

## Context

Every day has a `daily_summary` row: steps, distance, active calories, active and brisk
minutes, the goal in effect. The estimates depend on the profile (height, weight, step
lengths) and the goal is a setting; both can change at any time. VISION.md and PLANNING.md
§5 ask for two things at once:

- today's numbers follow the profile the reader just typed in;
- **past days are frozen**: a profile or goal change never silently rewrites history, unless
  the reader asks for it ("Apply profile to past data").

And steps can reach a day after it is over: a batch delivered by the sensor a few minutes
after midnight, or a long gap back-filled across it (PLANNING.md §4.4). Phase 1 left every
day unfinalized, with only `steps` and a provisional goal filled.

## Decision

1. **A day is open until it is over, and an open day is recomputed whole** from its minute
   rows, with the current profile and goal, at every write that touches it. A day's minutes
   are at most 1 440 rows on an index, so this costs one small query per flush.
2. **The first write that finds a day over finalizes it**, with the profile in effect until
   then and the goal the day was written with. "Over" means before today's local date in the
   zone in effect at the write. A later day a time-zone change left behind stays open until
   today passes it.
3. **A profile or goal change freezes the past first.** Under one lock with the service's
   writes, `TrackingRepository` finalizes every open day before today with the *old* values,
   then stores the new ones in DataStore, then recomputes the days still open with them. So
   a day never gets a profile that came after it, even when nothing was written between
   midnight and the change.
4. **Late steps on a finalized day add only their own share.** For each minute they touch,
   the difference they make (the minute's metrics after minus before, both with the current
   profile) is added to the frozen summary. With an unchanged profile this equals a full
   recomputation (tested); after a change, only the late steps are priced with the new
   profile, never the rest of the day. The day's goal is untouched.
5. **"Apply profile to past data"** is the one path that rewrites finalized days: every
   summary recomputed from its minutes with the current profile, keeping each day's goal.
6. **Schema v1 is kept.** Average cadence is not stored: it does not depend on the profile, so
   a screen computes it from the day's minutes when it needs it.
7. **Settings changes go through `TrackingRepository`** (behind `SettingsRepository` for the
   screens), never straight to DataStore, so rule 3 cannot be bypassed.

## Consequences

- The profile of a past day is not stored; rule 4 is exact only while the profile is
  unchanged, which is the case for the late steps that actually happen (minutes after
  midnight). A day reached by steps long after a profile change mixes the two profiles, in
  proportion to the steps each priced.
- The days recorded in Phase 1 get their estimates at the first write after Phase 2 lands,
  computed with the profile in effect then (the default one, unless the reader changed it
  before: then rule 3 already froze them with the default).
- DataStore and Room are not written atomically together. A crash between the DataStore write
  and the recomputation of today leaves today with the old estimates until the next write,
  which recomputes it: the error heals itself within a flush.
