# Interval walk, second field test (5 Oct 2026)

The owner's second interval outing, on the same phone as the first
(`docs/battery/2026-10-04-interval-walk.md`: no wake-up step sensor, no FIFO), with the processor
kept awake while the outing counted (ADR 0013, amendment of 4 Oct 2026).

## Setup

3 sets of 3 slow and 3 fast minutes, brisk, voice through headphones, no vibration, screen off.
A phone call came during the second fast interval and was answered while walking; the outing
was stopped by hand 32 s into the third fast one.

## When each change was told

| Change | Told | Screen |
|---|---|---|
| 1, fast | 2.0 s ahead | off |
| 2, slow | 1.7 s ahead | off |
| 3, fast | 1.3 s ahead | off |
| 4, slow | 2.0 s ahead | on (the call) |
| 5, fast | 1.8 s ahead | off |

Every change was told on time, ahead by the 2 s rule, against 33 to 127 s late the day before.
The wake lock was held from the start (11:42:42) to the stop (11:59:24): "released after 1001s",
the outing's own length.

The fourth change, to slow, was told during the call, and not heard: Android gives a call the
audio, and the outing did not vibrate. The third slow interval was walked at 118 steps a minute,
the pace of the fast ones. Since this test, a sentence due during a call is said when it ends,
in the words of the moment for an interval walk («Slow, 2 minutes left»).

## The numbers

- Distance at 0.714 m a step: the walking step, as the day's (the cadence fix of 4 Oct).
- Cadence by interval: slow 88, fast 120, slow 80, fast 119, slow 118 (the missed change),
  fast 108 for 29 s (stopped, not judged).
- Battery: the system's battery page showed Passo at 0% for the day, for a 17-minute outing.
  Not a measurement (the page rounds, and attributes the screen and the radio elsewhere), but
  no sign of a cost worth seeing.
