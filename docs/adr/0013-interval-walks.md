# ADR 0013: Interval walks, and telling a change on time (Phase 13)

- Status: accepted (owner, 1 Oct 2026): option A, minutes in motion, the editor's warning on a
  phone without a wake-up counter, brisk 100 as the fast pace's default
- Date: 2026-10-01
- Amends, in the change that builds Phase 13: `docs/adr/0009-sessions.md` (decision 5) and
  PLANNING.md §9.7, for an interval outing only

## Context

The owner asked for the Japanese interval walk as an outing, **in minutes**, to be faithful to
the protocol: Interval Walking Training (Nemoto et al., *Mayo Clinic Proceedings* 82(7), 2007),
sets of 3 minutes of slow walking followed by 3 minutes of fast walking, five sets or more.
ADR 0009 had left "interval walks" out of Phase 10.

Everything else of an interval outing is ordinary outing work (a goal kind, the splits, two
vibrations, a segmented notification). The one hard question is **when a change of interval is
felt**, with the phone in a pocket and the screen off, under Passo's battery rules:

- nothing runs on a timer while the screen is off (§9.4);
- no wake lock of the app's own (§9.2), no exact alarm (forbidden permission, §10);
- the one exception (§9.7, ADR 0009): during an outing, the **wake-up** step counter, with a
  30 s report latency. A milestone at 50% of a walk can be 30 s late; a change of interval
  cannot (30 s is a sixth of a 3-minute interval).

Two facts from the platform bound every option:

1. **The step counter has its own delay.** Android documents it as up to **10 seconds**
   ("The step counter has more latency (up to 10 seconds) but more accuracy than the step
   detector", *Motion sensors*; AOSP *Sensor types*: "can have a higher latency (up to 10
   seconds)"). Its event's timestamp is the time of the last step it counts, so the moment a
   change falls is always computed exactly; only the moment it is **told** can be late. The
   step **detector** is below 2 seconds, one event per step.
2. **The counter reports only when there are steps** (on-change). While the reader walks it can
   wake the processor; while they stand, nothing arrives, and nothing needs to.

## The clock: minutes in motion

The interval's minutes are **minutes in motion**, the clock every outing already has
(the gaps between steps, at most 1.5 s each, ADR 0009 decision 2).

- Faithful to what the protocol asks: three minutes of *fast walking*. On the wall clock, a
  minute at a traffic light would be eaten from the fast interval.
- A change can then only fall **while the reader is stepping**, which is exactly when the
  counter reports. The battery design below rests on this.
- The alternative, the wall clock, is the literal timer. A change falling during a stop would be
  told at the first step after it. Not chosen: it changes the screens' words, not the cost, and
  it would let a stop eat a fast interval.

## Options

Counted for a 30-minute session, 5 × (3 + 3): ten moments to tell (nine changes and the goal).
A "wake" is one report of a wake-up sensor: the driver's 200 ms wake lock and the framework's
delivery (ADR 0002). Energy figures are orders of magnitude, to be measured on the device
(§9.6): about 0.1 J a wake, about 0.3 to 0.5 W while the processor is held awake, against a
phone battery of about 17 Wh (61,000 J).

| | How | Told late by | Wakes in 30 min | Energy, order of magnitude | Rules touched |
|---|---|---|---|---|---|
| Today's outing | wake-up counter, 30 s | up to 30 s, plus the counter's own delay | about 60 | 6 J | none |
| **A. Adaptive latency** (chosen) | wake-up counter: 30 s, then 2 s in the 40 s before each change | about 2 s, plus the counter's own delay (documented maximum 10 s) | about 250 | 25 J, under 0.05% | §9.7 widened for interval outings |
| B. A + step detector near a change | A, plus the wake-up step detector at zero latency in the 40 s before each change | under 2 s | processor awake about 400 s | 120 to 200 J, about 0.3% | §9.1 (step detector) and §9.7 |
| C. Short wake lock and a timer | the processor held awake in the 45 s before each change, a coroutine fires the change | about 0 on the wall clock | awake about 450 s | 130 to 230 J, about 0.3% | §9.2 (first wake lock of Passo's own), §9.4 |
| D. Fixed short latency | wake-up counter at 5 s for the whole outing | up to 5 s, plus the counter's own delay | about 360 | 36 J | §9.7 widened |

Rejected outright: an exact alarm (forbidden permission); an inexact alarm or WorkManager
(minutes of slack, 15 minutes at least); keeping the screen on (far the most expensive); a wake
lock for the whole outing (the processor awake 30 minutes for ten moments).

### A, in detail (chosen)

- The outing counts with the wake-up counter at **30 s**, exactly as today.
- At every report, the tracker knows the time in motion left before the next change. When it is
  **40 s or less** (the 30 s window plus a margin), the service registers again at **2 s**.
  Registering again is the path the service already takes at every screen on and off
  (`StepSensorSource.register`), tested and cheap; the counter is cumulative, so no step is lost
  across it.
- The change is told at the first report that reaches it, or at the report whose predicted
  change falls before the next report is due (the cadence of the last 30 s gives the
  prediction): told up to 2 s early rather than up to 2 s late. Then back to 30 s.
- Why 2 s and not less: the counter's own delay is up to 10 s, so a shorter report latency buys
  almost nothing and costs a wake every step.
- Nothing new is held: no wake lock, no timer, no alarm. If the reader stops in the window, the
  reports stop too, and so does the clock in motion; nothing is missed.
- What the reader gets: a change typically felt within a few seconds, at worst about 12 s on a
  counter at its documented maximum delay. For 3 minutes, that is the difference between a
  sports watch and a kitchen timer, not between working and broken. The field test measures it
  (below) before the promise is written in the guide.

### When A is not enough

If the field test shows the owner's counter near its 10 s maximum, B is the step after A, not
C: it keeps the "no wake lock, no timer" rule and confines the step detector to 40 s windows
the reader asked for. C is listed for completeness: it is the only exact one, but it is the
first wake lock of Passo's own code and pairs naturally only with the wall clock.

### Screen on

With the screen on, the service already registers at 1 s and runs a ticker (§9.4 allows
tickers while the screen is on): the countdown to the next change is shown, and the change is
told on time, whatever the option.

### Phones without a wake-up step counter

The usual counter never wakes the processor: with the screen off, a change would come when the
phone next wakes for any reason, possibly minutes late. ADR 0009 already says so for its
milestones. For intervals it defeats the purpose, so the editor says plainly that the changes
come on time only with the screen on. Hiding the kind on such phones was the alternative, not
chosen: the reader who keeps the screen on, or walks on a treadmill, still has it. Which counter a phone has is in the diagnostics log at every service start.

## Measuring it

The service writes one diagnostics row per change told: when the change fell (from the step
timestamps) and when it was told. A 30-minute interval outing on the owner's phone then gives
the real delay, without guessing, and `dumpsys batterystats` gives the wakes (§9.6). The numbers
go to `docs/battery/` and decide between A and B.

## Consequences

- ADR 0009 decision 5 and PLANNING.md §9.7: "30 s" becomes "30 s, and 2 s in the 40 s before a
  change of an interval outing"; VISION.md's battery success criterion and CLAUDE.md's
  invariant ("never widen it") are amended in the same words, in the change that builds it
  (until then they describe the app as it is), and nowhere else changes.
- No new permission. The sensor, the service and the outing's lifecycle are the ones there are.

## Sources

- Nemoto K. et al., *Effects of high-intensity interval walking training on physical fitness and
  blood pressure in middle-aged and older people*, Mayo Clinic Proceedings 82(7), 2007.
- Android, Motion sensors (step counter and step detector latency):
  https://developer.android.com/develop/sensors-and-location/sensors/sensors_motion
- AOSP, Sensor types: https://source.android.com/docs/core/interaction/sensors/sensor-types
- AOSP, Suspend mode (wake-up sensors and their wake lock):
  https://source.android.com/docs/core/interaction/sensors/suspend-mode
