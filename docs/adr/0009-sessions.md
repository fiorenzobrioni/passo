# ADR 0009: Outings, walks with a goal (Phase 10)

- Status: accepted
- Date: 2026-09-25
- Amends: `docs/adr/0002-sensor-reporting.md`, for the length of an outing only
- Amended: 18 Nov 2026 (`docs/adr/0016-location.md`, decisions 7 and 14; built with Phase 15):
  a pace per kilometre for an outing measured with GPS, shown and never a goal (decision 1,
  below); the longest outing asks before it ends and can be carried on (decision 6, below)
- Amended: 20 Nov 2026 (owner; PLANNING.md §11 Phase 21, step 21.1): the open outing, an outing
  with no goal (decisions 1, 4, 6 and 8, below)

## Context

The owner asked for exercises with goals: set up a walk or a run with one or more targets
(steps, distance, active minutes, pace), start it when going out, be told on the way (with a
notification, and a vibration that says which signal it is without looking), and find it in
History with how much of it was done. VISION.md had "workouts or exercise sessions started
manually" among its non-goals, and walks were detected only after the fact, with no live
signal, so as not to wake the processor with the screen off.

The proposal was reviewed with the owner (25 Sep 2026) against the product's principles and
the way the known step apps do it (Apple Watch, Samsung Health and Fitbit take one goal per
workout, Garmin and Apple vibrate for alerts, the phone running apps speak through the
headphones). The owner approved a narrower form and asked for it now, in v1.0, under the
name **Outings** («Uscite»), with the voice left to a second iteration and a line on the widgets
while one is under way.

## Decisions

1. **One quantity, and an optional pace.** An outing's goal is steps, a distance (estimated)
   or minutes in motion, or "the rest of the day" (today's goal less today's count, fixed at
   the start). Never two quantities: with 3 km *and* 30 minutes "halfway" has no single meaning,
   and neither does its vibration. The pace is a cadence to stay at or above: free, brisk (100),
   vigorous (130), running (140), the CADENCE-adults bands and Passo's own running threshold.
   Not a pace in minutes per kilometre: without GPS it would be the cadence in disguise with
   the step length's error in it. No calorie goal: the weakest estimate should not be a target.
   No outing "type": the step length already follows the cadence minute by minute, so a
   run needs no switch; the name is the reader's, or the pace's.
   *Amended 18 Nov 2026 (Phase 15, `docs/adr/0016-location.md` decision 7):* an outing whose
   plan has «Measure with GPS» on has a measured distance, and over a measure a pace is a
   measure too: it shows its pace by the kilometre or the mile (time in motion over the
   distance) and the pace now. Shown only, and only where at least nine tenths of the distance
   came from fixes: the goal is still one quantity, the pace to keep is still a cadence, and
   an outing without GPS still has no pace per kilometre, for the reason above.
   *Amended 20 Nov 2026 (owner; PLANNING.md §11 Phase 21, §15 «The open outing planned»):* one
   quantity, **or none**. The outings with GPS made a walk worth starting with no target in mind:
   at the park, walked as it comes, looked at on the way, its route and its kilometres seen at the
   end. The known apps make it the shortest way in (Apple's «Open goal», Nike's «Basic», Samsung's
   «Basic workout»). The open outing (`SessionGoalKind.OPEN`, «None» / «Nessuno» in the editor's
   «Kind», beside «One goal» and «Intervals») has no goal and so no share of one: no bar, no
   milestones, no end by goal, and no «12 of 20 min». It keeps everything else an outing has (the
   pace to keep, «Measure with GPS», the vibration and the voice for what is told on the way, its
   numbers). Its sentence is what it has come to («25 minutes on your feet», «2.3 km measured in 25
   minutes»), and «halfway» keeps its single meaning, since an open outing has no halfway. Its own
   signal on the way (a kilometre, or ten minutes) is Phase 21's step 21.2, decision 3's to amend
   then.
2. **Measured from the steps, with no timer.** The tracking service feeds every accounted
   delta to a pure `SessionTracker`. Time in motion is the gaps between steps, each step
   credited with at most 1.5 s (a step at 40 spm, the active-minute threshold): standing at a
   traffic light adds nothing, and a batch the hardware merged cannot count a pause as walking.
   The cadence is the last 30 s. Distance and energy are priced per step at that cadence, with
   the same step lengths and costs as the day.
3. **The signals.** The reader picks among 25, 50 and 75% (50% by default); the goal is always
   told and ends the outing. Several shares crossed by one batch are told as the highest. Each
   is a vibration in its own count: one, two, three short pulses (180 ms, 220 ms apart), one
   long one (900 ms) at the goal, played as notification vibrations (silent mode applies) and
   only while the outings' channel is on: a reader who silenced the channel is not buzzed
   either. The editor lets the reader feel each one before choosing.
4. **Notifications.** No new ongoing notification: the counting notification becomes the
   outing's while it lasts (the name, the sentence, «12 of 20 min», the pace against the
   outing's, the steps and the estimates, Pause/Resume and Stop). On Android 16 it is a
   `ProgressStyle` with the milestones as points and asks to be promoted to a Live Update;
   below, `BigTextStyle` with a progress bar. The milestones on the way are not notifications
   of their own (the shade would fill); the goal is, on a `sessions` channel with no sound and
   no vibration of its own, with "Keep going" for 15 minutes after.
   *Amended 20 Nov 2026 (owner; Phase 21, step 21.1):* an open outing's notification has no bar,
   since it has nothing to fill one with. It is `BigTextStyle` on every version, still asking to
   be promoted to a Live Update, and on Android 16 its chip's short text is the distance (the time
   in motion before the first hundred metres). Touching the counting notification while an outing
   counts opens the outing's own page (decision 8), and so do «Still walking?» and «GPS stopped»:
   what they speak of is on that page.
5. **The sensor, during an outing only.** While an outing is counting, the service registers
   the **wake-up** step counter, where the phone has one besides the usual one, with a 30 s
   report latency; paused, over, or with no outing, it registers exactly as before. This is
   the one change to ADR 0002, see below.
   *Amended 3 Oct 2026 (Phase 13, `docs/adr/0013-interval-walks.md`):* an interval outing
   reports within 2 s in the 40 s of motion before each change of interval, and within 30 s
   otherwise.
   *Amended 4 Oct 2026 (`docs/adr/0013-interval-walks.md`):* on a phone with no wake-up counter, an
   outing with signals keeps the processor awake while it counts (a partial wake lock), so its
   signals are on time there too.
6. **It ends by itself.** At its goal; after 15 minutes without a step (at its last step,
   noticed by the next step or the screen, so a forgotten outing costs nothing); after an
   hour paused; at four hours. One with fewer than 30 steps is not kept. Pausing the count
   ends it. Only one at a time.
   *Amended 27 Sep 2026 (owner's field test):* the end by stillness is told when it is
   noticed, usually as the reader walks on (two long pulses, and the voice if the outing
   speaks), and can be taken back with "Resume" for 15 minutes from then, like the goal's
   "Keep going", with the steps since and without the stillness. Closing the finished card
   withdraws both. Nothing new runs for it: it rides on the step batch that noticed the end.
   *Amended 18 Nov 2026 (Phase 15, `docs/adr/0016-location.md` decision 14):* "at four hours"
   ended a walk the reader was still on without a word, and for good. Now every outing, with
   GPS or not, keeps its own cap (four hours from its start) and asks fifteen minutes before
   it, at the first steps within that quarter of an hour: the stillness's two long pulses, a
   notification and its card, «Still walking?», with «Keep going», which moves the cap four
   hours on and can be said again before the next. Unanswered, the outing ends at its cap,
   says so, and can be taken back for 15 minutes with the steps since, as the other two ends
   can. No timer: a reader who is sitting is not asked, and the stillness ends the outing as
   always.
   *Amended 20 Nov 2026 (owner; Phase 21, step 21.1; `docs/adr/0016-location.md` decision 14,
   amended the same day):* an open outing has no end by goal. It ends when the reader stops it, at
   its last step after 15 minutes still, where it was paused after an hour paused, or at its cap,
   with the same take-backs as every outing. **Its cap is twelve hours**, asked 15 minutes before
   as every cap, and «Keep going» moves it four hours on as every cap: an open outing has no end
   of its own, and a day's hike would otherwise lose the rest of its route at four hours. A ride
   is not an end: the steps stop in a car, the end at the last step follows, and a ride shorter
   than the stillness stays inside the walk, as a tram between two walks already does (the owner
   chose this over an end at the ride, which only the first fix after it could see).
   The pauses are kept (each from its start to the resume), for every outing: an outing's page
   says the time paused, and Phase 21's minutes are drawn true.
7. **Stored as it goes, with the steps.** Two new tables (`session_plan`, `session`), schema v2
   by an auto-migration. The outing under way is written in the same transaction as the step
   batch that moved it, so the counter state and the outing never disagree after a crash; a
   process the system restarts picks it up from there. The goal and the estimates are copied
   into the outing: past outings are frozen like past days.
8. **Where it shows.** Today: a "Start an outing" button under the day's chart, or the
   outing's card above it (under way, paused, or just over, until put away); the button is
   the reader's to take away in Settings, the card is not. The Outings page (from Today, and from Settings) keeps the plans, three
   presets to start with. History and Today list an outing in place of the walk found in its
   minutes, with its goal and how much of it was done. The launcher's long press offers the
   three last started. The evening reminder has "Walk now", which starts the rest of the day.
   Both widgets and the Quick Settings tile say the outing in the sentence's place while it is
   under way («Brisk walk: 12 of 20 min»), repainted when it starts, pauses or ends.
   Started from a page of the app, the outing's card is brought into view there, with a
   confirming tick, and TalkBack says it; started from a launcher's shortcut, Today comes
   forward with the card (PLANNING.md §15, 16 Oct 2026).
   *Amended 20 Nov 2026 (owner; Phase 21, step 21.1):* an outing's own page follows it while it
   counts, and is reached from its card while it counts, not only once it is over («See the
   outing», on Today, the Outings page and a city walk's page); the page then carries the card's
   Stop and Pause or Resume, its line about the satellites and the question before the cap, so a
   reader who opened it never goes back to stop. What shows the outing opens it: the counting
   notification, both widgets and the tile while they show it. The Outings page starts with an
   open walk at any pace among its presets, first; a reader who keeps plans already finds it
   once, at the top of their list, and never again once deleted.

## Why the sensor change is acceptable

ADR 0002 rejected the wake-up counter for everyday tracking because its cost is paid every
day by everyone: 48 to 96 wakeups a day for a benefit that exists only in the rare abrupt power
loss. An outing is the opposite case:

- **The reader asked for it**, for a bounded time, and the benefit is the feature itself: a
  signal felt at 50% while it is 50%, not ten minutes later when the phone next wakes.
- **It costs little and only while walking.** The step counter is an on-change sensor: it
  reports only when there are steps, so a wake-up registration wakes the processor at most once
  per 30 s latency window *while walking*, about two short wakes a minute (the driver's 200 ms
  wake lock each), about 40 for a 20-minute walk, and none while standing still. The service
  holds no wake lock of its own, and no timer or alarm is added: every check rides on a step,
  a screen-on, or the app being opened.
- **Outside an outing nothing changes**: same sensor, same latencies, same code path. The
  battery rules of PLANNING.md §9 stand, with this one exception written into them.
- **Where the phone has no wake-up counter**, nothing changes either; the page says that a
  signal with the screen off can then come a few minutes late.

## Consequences

- New permissions: `VIBRATE` and `POST_PROMOTED_NOTIFICATIONS`, both normal (granted at
  install, never asked). The notification permission is asked in context when the first outing
  starts without it, as Android recommends; the outing starts either way and the page says what
  the refusal costs. `checkForbiddenPermissions` is unchanged and green.
- A new channel, `sessions`, which the reader can silence; the Outings page says so when they
  have, with the button to the system page.
- The Android 16 form (`ProgressStyle`, the Live Update chip) is covered by the documentation,
  not by the tests: Robolectric cannot start API 36 on the build's JRE. To be checked on a
  device, with the battery measurement of an outing (§9).
- Walk detection is unchanged and still computed on read; an outing is simply listed in the
  place of the walk that overlaps it.
- Not in this phase: the voice (the owner's second iteration: spoken signals through the
  headphones, with the system's offline voices), interval walks, outings in Insights.
