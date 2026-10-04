# Interval walk, first field test (4 Oct 2026)

The owner's first interval outing on their own phone, read from the backup file's diagnostics
log (PLANNING.md §11 Phase 13, `docs/adr/0013-interval-walks.md`). No `batterystats` was taken:
this records when the changes were told, not what they cost.

## Setup

- Phone: a Samsung whose step counter has **no FIFO and no wake-up variant**, and whose step
  detector is not a wake-up one either (`fifoMax=0 wakeUpVariant=false stepDetector=true
  stepDetectorWakeUp=false`). With the screen off nothing in it can wake the processor.
- Outing: 3 sets of 3 slow and 3 fast minutes, brisk, voice through headphones, no vibration.
  Screen off, phone in a pocket; Bluetooth on for the earbuds, no music, mobile data off.
- Walked 08:40 to 08:59, 2,057 steps, 18 min 49 s in motion.

## When each change was told

| Change | Due (in motion) | Told | Late |
|---|---|---|---|
| 1, fast | 3:00 | +5:12 | **127 s** |
| 2, slow | 6:00 | +6:03 | 2 s early |
| 3, fast | 9:00 | +10:33 | **88 s** |
| 4, slow | 12:00 | +12:03 | 2 s early |
| 5, fast | 15:00 | +15:37 | **33 s** |
| Goal | 18:00 | +18:54 | about 49 s (from the outing's totals; not logged then) |

The changes into a slow interval came on time, told ahead by the 2 s rule; the changes into a
fast one, and the goal, came 30 s to 2 minutes late. Every on-time change followed a late one by
a minute or less, and every late one followed a spoken sentence by more than two minutes. The
likeliest reading: after the voice speaks through the earbuds the phone stays awake for a while
(the Bluetooth audio path), steps arrive as they are taken, and a change within that time is told
on time; once it sleeps, the counter's events are dropped and the steps arrive only when the phone
next wakes for its own reasons, every one to two minutes. Option A cannot do better on this
phone: it asks the counter for a latency the counter cannot honour while the processor sleeps.

## Found on the way

The outing measured its distance with the running step: 1.84 km for 2,057 steps (0.89 m a step,
where the day's own minutes give 0.71). The tracker's cadence counted each clump of steps handed
over at once inside its half-minute window, so 150 steps read as 300 a minute. Fixed in the same
change as this note: the cadence measures each sample's steps against the time in motion they
took (`SessionTracker.cadenceAt`). The day itself was right, after the attribution fix of 3 Oct.

## What is left to decide

On a phone like this one a change is on time only with the screen on, or with ADR 0013's option
C (the processor kept awake by Passo in the 45 s before each change, about 0.3% of a battery for
half an hour, the first wake lock of Passo's own). C is the owner's decision. The goal is now
logged like the changes (`interval=goal`), so a second outing measures all of it.
