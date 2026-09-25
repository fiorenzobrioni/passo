# ADR 0002: Sensor reporting (Phase 1)

- Status: accepted, from the documentation; not measured on a device. Amended for the length of
  an outing by `docs/adr/0009-sessions.md` (the wake-up counter while an outing the reader
  started is counting); unchanged at every other moment.
- Date: 2026-09-24

## Context

PLANNING.md §4.3 planned an experiment for Phase 1: register the **wake-up** step counter
(`getDefaultSensor(TYPE_STEP_COUNTER, true)`) with a 15 to 30 minute latency, compare it with
the **non-wake-up** one, and pick from the measurements. The owner has no device to measure
on for now, and decided to settle the question from the platform documentation instead, and
to drop the experiment (and the diagnostics screen that would have shown its results) from
the plan.

What the documentation says:

- The step counter is an **on-change, low-power** sensor, cumulative since boot and reset only
  by a reboot; `getDefaultSensor(TYPE_STEP_COUNTER)` returns the **non-wake-up** one
  (AOSP, *Sensor types*).
- A **non-wake-up** sensor never wakes the processor. While it sleeps, events wait in the
  hardware FIFO; if the FIFO fills, the oldest events are dropped, **but an on-change sensor
  must keep its latest event outside the FIFO** (AOSP, *Suspend mode*). They are delivered the
  next time the processor is awake, for any reason, or when a flush is asked for.
- A **wake-up** sensor must wake the processor and deliver before `maxReportLatency` elapses
  or the FIFO fills, and the driver holds a 200 ms timeout wake lock for every report
  (AOSP, *Suspend mode*), and the framework keeps the processor awake while it hands the event
  to the app.

## Decision

**Passo uses the non-wake-up step counter**: 10 minute report latency with the screen off,
1 second with the screen on, a flush on screen on, screen off and shutdown (§4.2, §4.3).

A device that exposes only a wake-up step counter gets it rather than nothing; which sensor
was used is written to the diagnostics log at every service start (name, vendor, version,
wake-up mode, FIFO sizes), so that case is visible later.

## Reasons

1. **Totals cannot be lost by batching.** The counter is cumulative and its latest value
   survives a full FIFO, so the next event carries every step. A FIFO overflow can only make
   the minute attribution coarser, and the gap rule of §4.4 spreads such a delta over the gap.
2. **A graceful shutdown loses nothing either way.** `ACTION_SHUTDOWN` reaches the service's
   runtime receiver, which flushes the sensor and writes the buffer before power-off (§4.2).
   The wake-up variant would change nothing there.
3. **The wake-up variant only helps an abrupt power loss** (battery pulled, hard crash, a
   battery at 0% that dies without a shutdown broadcast): it bounds the age of the persisted
   count by the latency window. Even then the non-wake-up sensor loses only steps taken since
   the processor was last awake for any reason, and a phone in someone's pocket wakes many
   times an hour (notifications, network, Doze maintenance windows, the screen).
4. **Its cost is paid every day, by everyone**: 48 to 96 processor wakeups a day at 15 to 30
   minutes, each one caused by Passo's registration and held awake by a wake lock. That goes
   against the battery rules (§9: no wakelocks, nothing woken on a schedule) and the success
   criterion of no app wakelocks with the screen off (VISION.md), for a benefit that exists
   only in the rare abrupt case.

## Consequences

- An abrupt power loss can lose the steps since the processor was last awake, bounded by the
  10 minute window. Documented in PLANNING.md §4.6 and §14.
- The decision rests on the documentation, not on measurements. It is revisited if a field
  report shows missing steps that the log explains by batching (a vendor HAL that drops the
  latest value, contrary to the HAL contract), or if the Phase 1 battery check shows wakeups
  attributed to the sensor.
- No diagnostics screen: the log is in the database (`diagnostics_event`, 500 rows). On a debug
  build it can be read with `adb shell run-as com.callbackdev.passo.debug`; Phase 7's export is
  the user-facing way out.

## Sources

- AOSP, Sensor types: https://source.android.com/docs/core/interaction/sensors/sensor-types
- AOSP, Suspend mode: https://source.android.com/docs/core/interaction/sensors/suspend-mode
- Android, Batching: https://source.android.com/docs/core/interaction/sensors/batching
- Android 15 behavior changes, BOOT_COMPLETED and foreground services:
  https://developer.android.com/about/versions/15/behavior-changes-15#fgs-boot-completed
- Foreground service types, `health`:
  https://developer.android.com/develop/background-work/services/fgs/service-types#health
