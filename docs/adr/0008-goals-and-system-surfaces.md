# ADR 0008: Goal notifications and the Quick Settings tile (Phase 6)

- Status: accepted
- Date: 2026-09-25

## Context

Phase 6 (PLANNING.md §11) brings what reaches the reader outside the app: a notification when
the goal is reached, an evening reminder, a weekly summary and a Quick Settings tile (the rich
counting notification was brought forward to Phase 5). The owner asked for a clean
implementation, above all in UI and UX. The constraints are the project's: battery first (no
wake on a timer, no exact alarm, no permission that the build forbids), no duplicate
notifications across reboots or time-zone changes, one sentence before any number, estimates
that say so, and a screen that never promises what Android will drop.

## Decisions

1. **Goal reached rides on the samples.** The tracking service checks the live count against
   the goal on every sample it already receives (`GoalReached.isNews`, a comparison, no read),
   so the notification costs no wake of its own. With the screen off it arrives with the
   sensor's batch, a few minutes late at most; Settings says so.
2. **Once a day, by a claimed day that only moves forward.** Before telling, the day is claimed
   in the settings file in one DataStore edit (`claimGoalNoticeDay`): it succeeds only for a day
   after the last one claimed. A restart that reads the goal met again, a flight west that
   brings yesterday's date back, a second caller, or a goal lowered below today's count cannot
   tell it twice. The day is claimed with the notification off too, so turning it on in the
   evening does not announce the morning. The text says the minute the goal was met (from the
   minutes, not from when the service heard of it) and the streak it extends, from two days.
3. **The evening reminder is one inexact alarm with a wakeup.** `setAndAllowWhileIdle` on the
   wall clock, as Chiaro's sky reminders, rather than the `setWindow` §8 first named: both cost
   one wake, but Doze holds a window alarm until a maintenance window, and a reminder to walk
   that arrives at ten is worth nothing. It is armed only while counting is on and never for a
   day already at its goal (reaching the goal moves it to tomorrow), so a good day wakes
   nothing. When it fires it arms the next one first, then speaks only if the count is live (a
   paused or stopped count would make it lie), only on the evening it was for and at most two
   hours late, and only below the reader's threshold: until the goal is met (default), below
   75%, below half. It asks the running service to flush the sensor first (`TrackerLink`): the
   hub holds up to ten minutes of steps with the screen off, and a reminder that forgot the
   walk just finished would be wrong at the worst moment.
4. **The weekly summary does not wake the phone.** An `RTC` alarm on the first day of the week
   at 9:00, posted the first time the phone is awake after it, and only while that week is
   still the current one. It is the week History shows (`WeeklySummary` over `PeriodOverview`,
   each day against its own goal), led by the one sentence that matters: the goal every day,
   more or fewer steps a day than the week before (5% either way is "about as many", the band
   Insights uses), or, for the first week, the days at the goal. A week with no steps sends
   nothing.
5. **The alarms are re-armed, never trusted.** An alarm is an instant and dies with a reboot, so
   they are armed again from a watch on the settings kept for the life of the process
   (`GoalNotifier.start`, from the application: a boot or an update starts the process), from
   the clock and time-zone broadcasts (exempt from the implicit-broadcast limits), at every
   firing, and when the goal is reached. The times are pure (`GoalSchedule`): local, moved
   forward over the spring gap, tested.
6. **One channel, `goals`, at the default importance**, apart from the quiet counting channel:
   each of the three is something the reader asked for. All three are off by default.
7. **Settings says when Android would drop them.** Turning one on asks for the notification
   permission where Android still can. If the permission was refused, the app's notifications
   are off or the goals' channel is, and something is turned on, a card over the group says so
   and opens the page that fixes it: a switch that is on must not promise what the system
   drops. The reminder's note is its sentence ("At 8:00 PM, if the goal is not met yet: the
   steps left, and the walk they take"), its time is picked on the clock dial, with the note
   that it is not exact, and its time and threshold rows are disabled while it is off.
8. **The tile reads only while the panel is open.** `StepsTileService` opens a scope in
   `onStartListening` and cancels it in `onStopListening`; meanwhile it follows the live count,
   as Today does. Counting, it shows the count and the share of the goal (or "Goal reached");
   otherwise the reason (paused, not counting, no permission), with the count so far where it
   still holds. A tap opens Today, or, on a paused tile, asks the app to resume, as the
   widget's tap does. Settings adds it with the system's own prompt
   (`StatusBarManager.requestAddTileService`), and says where it stands only once the system
   has answered: Passo cannot see the panel, and does not guess.

## Consequences

- No new permission and no new dependency: inexact alarms need none, the tile's binding
  permission is the system's. `checkForbiddenPermissions` is unchanged and green.
- With the evening reminder on, the phone wakes once a day for it, on days below the threshold
  only. Everything else in this phase costs nothing with the screen off. The Phase 6 battery
  check (§9) is still to run on a device.
- A phone that is off at 9:00 on the first day of the week skips that summary: the alarm is
  armed again for the next week at boot. Accepted: a late summary of a week already a day old
  would be the lesser product.
- The reminder is silent when the system has stopped the service, rather than reading a count
  that has stopped moving; the widget and the tile already say "Not counting".
