# ADR 0006: History, Insights and walks (Phase 5)

- Status: accepted
- Date: 2026-09-25

## Context

Phase 5 (PLANNING.md §11) brings the second and third tabs: the day in detail with its walks,
the week, month and year with the goal, a calendar of goal days, streaks, records, averages and
lifetime totals. The owner asked for a clean implementation, above all in UI and UX. The
constraints are the project's: battery first (nothing in the background), past days frozen,
every number with the line that says what it means, Chiaro's design language, charts drawn in
Compose `Canvas`.

## Decisions

1. **Everything is computed on read, in `:core:domain`, while a screen is visible.** Walks
   (`WalkDetector`, §6.1), a period's bars and totals (`PeriodOverview`), streaks, records and
   averages (`Insights`) are pure functions of the recorded days, unit-tested, with no table
   and no worker. History and Insights read every `daily_summary` row (one a day: a few
   thousand after years) once per change, on a background dispatcher, only while collected.
2. **Each day is measured against its own goal.** The goal line of a week or a month steps
   with each day's stored goal, a streak is made of days that met *their* goal, and a day with
   no row stands on the current goal only for drawing. A goal change never moves a streak or a
   record (tests cover goal changes across days).
3. **A counted day is any day from the first recorded one to today**, and one with no row
   counts as zero steps: that is what it was (the phone at home, a pause). Averages leave today
   out until it is over, so they do not sink every morning; the page's "so far" says the same
   for a period in progress.
4. **Past days show the estimates they froze with** (§5): a day's distance and calories come
   from its summary, today's from its minutes with the current profile, as on Today. Walks are
   computed from the minutes with the current profile: they are not stored, and their share of
   a frozen day is an estimate like the rest.
5. **Today is live everywhere.** The service's in-process count (`LiveSteps`) raises today's
   row in History and Insights (`byDayWithLive`), so the three tabs never show two counts.
6. **One chart, three uses** (`BarChart` in `:core:designsystem`): hours of a day, days of a
   week or month, months of a year. The scale is the world's (from zero, up to the goal or a
   fixed floor), a met day wears the goal's color and says so in words, a day to come is drawn
   as nothing and a day of zero as a stub. Touch reads it like Today's chart (tap, scrub with a
   tick per bar, vertical moves left to the page); each bar is its own screen-reader node with
   a spoken line, and the plot reads as a summary. Walks are shaded over their minutes and
   marked in a lane under the axis.
7. **The calendar is a grid of real composables**, not a canvas: 42 cells, each a node for
   TalkBack and a touch target that opens the day. One hue in four steps for "how close", the
   goal's color and a check for "met" (never the color alone), today ringed.
8. **Navigation**: a Material bottom bar for Today, History and Insights, with Material's fade
   through between tabs (the 100 ms fade under reduced motion), each tab keeping its place;
   Back from History or Insights goes to Today. In History the scale is a segmented choice and
   the periods page with a swipe or the arrows, bounded by the first recorded day and today;
   "Latest" comes back. The charts are also the way down (a year's month opens the month, a
   bar or a calendar day opens the day), and a record in Insights opens its day, week or month
   in History.
9. **Walk detection off means no walk anywhere**: not in the chart, the sentence, the list or
   Today. Its minimum (5, 10, 15 minutes) and the first day of the week are in Settings now
   that something uses them.

## Consequences

- A cadence of 140 spm or more across a whole walk makes it a run; both walking and running for
  30% of it make it mixed. The thresholds are to be tuned after the field test (§15, Open).
- The year view draws twelve bars from at most 366 rows; the 16 ms frame budget of the phase's
  acceptance is far from reach, though it is still to be confirmed on a mid-range phone.
- The README screenshots do not show the bottom bar, which belongs to the shell and not to the
  screens the tests draw.
