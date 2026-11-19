# ADR 0016: Location, for an outing that asks for it (Phases 15 to 20)

- Status: accepted (owner, 18 Nov 2026). Phase 15 built the same day, not released, and not
  yet tried on a phone (decision 4's proof); Phases 16 to 20 not built
- Date: 2026-11-18
- Supersedes: PLANNING.md §15 «Route maps with GPS stay out» (1 Oct 2026; archived in
  `docs/planning/02-decisions-to-1-nov-2026.md`, line 120)
- Amends, in the change that builds Phase 15: VISION.md (principles 1 and 3, the non-goals, the
  success criterion «Privacy is enforced»), PLANNING.md §9 and §10, CLAUDE.md's invariants, the
  README's privacy lines, `docs/adr/0007-backup.md` (a second database, left out of the cloud
  copy), `docs/adr/0009-sessions.md` (decision 1: a pace per kilometre, shown, never a goal;
  decision 6: the longest outing asks before it ends and can be carried on) and
  `docs/adr/0013-interval-walks.md` with PLANNING.md §9.7 (the wake lock of a phone with no
  wake-up step counter: also an outing with GPS's, and bounded by the outing's own cap)
- Amended 18 Nov 2026, before anything was built (owner, on Phase 15 read against the code:
  "let us go by what is best, and change the ADRs and CLAUDE.md to follow"; then "I confirm all
  five"): decisions 4, 5, 9, 10, 13 and 14, each marked where it changed, and the platform's
  facts on the location button

## Context

Passo had ruled location out twice: in VISION.md from the first day (principle 3, the
non-goals, a build that fails on any `ACCESS_*LOCATION`), and again on 1 Oct 2026, when the
owner weighed recording an outing's route and decided against it for four reasons:

1. the permission in the manifest turns "Passo cannot" into "Passo promises not to";
2. the database is in Android's backup allowlist, so routes would travel there;
3. GPS is the most expensive sensor, far beyond ADR 0009's exception;
4. OpenTracks already records routes, offline and without `INTERNET`.

On 18 Nov 2026 the owner asked for the question to be studied again with that rule set aside:
how location could enter Passo, turned on by the reader, with the app still usable exactly as
it is without it; what it would add, how it would sit with the product's philosophy, what the
premium apps do and what could set Passo apart, as the Ways do. The owner approved the
analysis the same day, with its three recommendations (the day stays the steps', a longer
outing only on the spot, one APK), and asked for this record and for the phases.

What changed between the two decisions is the question. In October it was "record a route and
draw it", which every tracker does: location would have cost a principle and brought nothing of
Passo's own. Here location is a measuring tape for what Passo estimates, a teacher for the step
counter, and the thing that makes the ways and the city walks real for a reader who is there.

Facts from the platform that bound the design (sources at the end; the phone's own behaviour is
to be proven on a device in Phase 15):

- A foreground service of type `location` needs `FOREGROUND_SERVICE_LOCATION` and a granted
  `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION`. Location is a while-in-use permission: a
  service created while the app is in the background cannot have it, unless it starts from a
  touch on a notification or a widget, or the app holds `ACCESS_BACKGROUND_LOCATION`.
  `BOOT_COMPLETED`, where Passo's service starts, is not among the exemptions.
- A running service adds a type by calling `startForeground()` again with the combination of its
  types, each declared in the manifest. The documentation does not say in so many words that the
  second call is checked as a creation is; it is read here as if it were.
- Since Android 12 `ACCESS_FINE_LOCATION` is asked together with `ACCESS_COARSE_LOCATION`, and
  the reader may grant the approximate one alone.
- `android.location.LocationManager` gives satellite fixes with no Google Play services
  (`GPS_PROVIDER`, `LocationRequest` since API 31). `AltitudeConverter` (API 34, Passo's
  `minSdk`) turns a fix's altitude into one above mean sea level, from data on the phone.
- The pressure sensor (`TYPE_PRESSURE`) needs no permission.
- Android 17's official change lists (read on 18 Nov 2026) say nothing about location; the
  press wrote of a "Location Button" for a one-time precise fix. Passo targets API 37, so
  Phase 15 reads the official pages again before it writes the permission's flow.
  *Read again on 18 Nov 2026, as Phase 15's first step:* the change lists still say nothing on
  location, the `location` service type or the while-in-use rule. The location button is on
  the permissions page, not in the change lists: an experimental Jetpack library
  (`androidx.core.locationbutton`, 1.0.0-alpha01 of 17 Jun 2026) with a permission of its own
  (`USE_LOCATION_BUTTON`), for "momentary, session-based precise location access"; Google Play
  asks for it of an app that targets API 37 and "only contains features that require
  session-based location access". Passo's need is the other kind, a walk measured for its
  whole length with the screen off, by a foreground service; the button would be a new
  dependency in alpha and one more permission, and its pages do not say that its grant reaches
  a service. So the permission's flow stays the classic request of decision 2. Phase 9 reads
  the Play policy itself before a listing.
- The one-time grant («Only this time») gives the same permissions as «While using the app»;
  the documentation does not say how long it lasts under a foreground service that never
  stops. Proven on the phone in Phase 15.

## Decisions

1. **A measuring tape, never a tracker.** Location is read only inside an outing the reader
   started with «Measure with GPS» on, from its start to its pause or end. Never for the day's
   count, never with no outing, never in the background, never by a timer or a geofence. A
   reader who never turns it on is asked for nothing and pays nothing: every screen, every
   number and the battery are as before. Nothing in the app depends on it.

2. **The permissions, and a stricter gate.** `ACCESS_FINE_LOCATION` and
   `ACCESS_COARSE_LOCATION` (asked together, in context, the first time an outing with GPS
   starts; never in the first run), `FOREGROUND_SERVICE_LOCATION`, the service's type
   `health|location`, and `<uses-feature android:name="android.hardware.location.gps"
   android:required="false" />` so that a phone without it is not filtered out.
   `INTERNET` and `ACCESS_BACKGROUND_LOCATION` stay forbidden, with the others of §10. The
   build's gate becomes an **allowlist**: the merged manifest must hold exactly the permissions
   written in `build-logic` (today's list plus these three), and any other fails the build, a
   forbidden one by name. Letting location in makes the gate stricter than it was, not looser.
   What can be said is then: Passo cannot send anything (no `INTERNET`); it can know where the
   phone is only while an outing that asked for it counts, and Android's own permission page and
   privacy dashboard show it, not Passo's word.

   *Amended 18 Nov 2026 (owner: "do the best thing, one that adds value and keeps the app
   premium"):* **the switch turned on again is the reader asking again.** A refusal is still
   never asked again by Passo itself. But it had one way back, the line its first outing's
   card said once: every outing after it counted from the steps without a word, under a
   switch that stayed on. So:
   - a plan saved with «Measure with GPS» turned on by the reader's own hand (a new plan, or
     off and on in an old one) forgets the refusal, and the next start of an outing with GPS
     asks as the first did: Passo's own screen, then the system's request. In context, at the
     start; the editor itself never asks. A plan saved with the switch as it was (renamed, its
     goal changed) asks nothing;
   - the same holds for the approximate location granted alone, which is a refusal of what
     was asked: it is asked again, for the precise one, only after that gesture or where
     Passo has no answer to remember;
   - under the switch the editor says what is true: what it gives and costs; that Passo asks
     when the outing starts, wherever it will; and, after a refusal with the switch still on,
     that the outing counts from the steps and that turning the switch off and on is the way
     to be asked. The estimate's note and the note on what keeps the phone awake follow it.
   Where Android no longer shows its request (after a second refusal there), Passo's screen is
   shown and the outing starts from the steps; its card says so once more, and its «Allow
   location» opens the system's page for Passo, as before.

3. **Satellites only, through the platform.** `LocationManager` with `GPS_PROVIDER`: no Google
   Play services, no fused provider (on a phone with Google's services it may answer from Wi-Fi
   and cell lookups, which are requests to a network; a satellite fix is received, not asked
   for). No new dependency. The phone's own receiver may fetch assistance data by itself, as it
   does for any app: the words stay "Passo sends nothing", never "nothing leaves the phone"
   (ADR 0007's rule).

4. **One service, a second type while it measures.** `StepTrackingService` keeps starting as
   `health` (boot, update, the app opened). When an outing with GPS starts or resumes it calls
   `startForeground()` again with `health|location`, and goes back to `health` at a pause or the
   end. The while-in-use rule decides where that can happen:
   - a start from a page of the app or from a launcher shortcut (which brings Today forward
     first, ADR 0009 decision 8): allowed;
   - Resume from the notification: a touch on a notification, the documented exemption;
   - the evening reminder's "Walk now": the rest of the day, never with GPS;
   - a process the system restarted mid-outing: the outing goes on from the steps (decision 5),
     and its notification offers «Resume GPS», one touch.

   *Amended 18 Nov 2026 (owner):*
   - **"Walk now" keeps the plan's word.** The reader's plan for the rest of the day is theirs
     to set, GPS included, and a plan that measures from the Outings page and not from the
     reminder, without a word, is one plan with two behaviours. A touch on a notification is
     the same documented exemption Resume stands on. So "Walk now" starts with GPS when that
     plan has it on, the precise permission is already granted and location is on; otherwise
     it starts on the steps and the card says so once, with the way to change it. It never
     asks for the permission: a notification cannot.
   - **GPS comes back by itself, and its stop is told.** A restarted process cannot have the
     receiver, and a button on a silent notification leaves a reader with the phone in a
     pocket to find at home that half the route was laid from the steps. So: the stop is told
     once (a notification on the outings' channel, which alerts as the reader set it, and the
     voice if the outing speaks; no vibration of its own, the patterns being few and each
     with one meaning); «Resume GPS» is on the notification and on the card; and the receiver
     comes back with no touch at all the next time the app is brought forward, which is "a
     start from a page of the app" in the rule's own terms. With the permission gone, the way
     back opens the app, where it can be asked.

   Should a device refuse the second type on a running service, the fallback is a small second
   service of type `location`, created from the same places and sharing the counting
   notification's id, that hands its fixes to the first; everything above the source is the
   same. Phase 15 proves which one it is before building on it.

5. **The steps keep the GPS honest, and the GPS the steps** (`RouteMeter`, pure,
   `:core:domain`). This is what a tracker without a step counter cannot do:
   - **The walk does not wait for the satellites.** An outing starts at once, on the steps;
     the first accepted fix joins it, and the stretch before it stays the steps'.
   - **Time in motion stays the steps'** (ADR 0009 decision 2): no pause guessed from a GPS
     speed, which is where trackers' auto-pause fails.
   - **Still means still.** With no step, fixes add no distance: the drift of a phone on a
     bench is not a walk.
   - **A bus is not a walk.** A stretch with movement and no steps adds nothing.
   - **No lost metres.** While steps come and no fix is accepted (a tunnel, an arcade, tall
     streets), the stretch is the steps' estimate, never less than the straight line between
     the fixes around it; the outing keeps the share that was measured and says so.
   - **A fix is accepted** when its accuracy is 25 m or better, its time moves forward, and the
     speed it implies from the one before is one the cadence allows (3.5 m/s below the running
     cadence, 8 m/s at or above it). The constants live with their reasons in the domain, to be
     validated on the device like `MetricsConstants`.
   - **The GPS sleeps when the steps do.** After 2 minutes without a step the updates are
     removed; the next step asks for them again. The outing's own wake-up step counter (or its
     wake lock, §9.7) already brings that step on time.
     *Amended 18 Nov 2026 (owner):* that wake lock was held only by an outing with signals
     (ADR 0013), so on a phone with no wake-up step counter an outing with GPS and no signal
     had nothing to bring the step: the receiver, once asleep, stayed asleep until the screen
     came on. On such a phone an outing with GPS now holds the same lock while it counts,
     with signals or without, for the reason the lock exists: the steps must arrive as they
     are taken, and here they steer the receiver (its sleep, its waking, the speed a fix may
     imply, "still means still"). A receiver slowed to a fix every half minute during a stop
     was weighed and left out: it costs more than a processor awake with the receiver off,
     and finds the first step up to half a minute late. The lock is let go as it always is:
     at a pause, at the end, after 15 minutes still. It amends ADR 0013 and §9.7 in the
     change that builds Phase 15. The sleep itself is noticed on whatever arrives, a step
     batch or a batch of fixes: no timer.
   In an outing with GPS, the outing's distance is this fused distance: its goal, its signals,
   a walk's places and its energy use it. What the steps alone would have said is kept beside
   it, for the outing's page («5.20 km measured; your steps said 5.05») and for decision 12.

6. **The day stays the steps'.** A day's distance, History, Insights, the ways walked from home
   and Your year on foot are computed from the steps as before, with or without GPS outings in
   the day. A day made of measured stretches and estimated ones would break the rule that a day
   is frozen as it was estimated, the import's merge, and "Apply profile to past data"; and
   once the step has learnt (decision 12) the two agree within a few percent anyway. Only the
   outing shows a measure, and says which number is which: «measured» is a word the app uses
   only for a distance that is at least nine tenths from fixes.

7. **A pace per kilometre, shown.** ADR 0009 refused it because "without GPS it would be the
   cadence in disguise". With a measured distance it is a measure: an outing with GPS shows
   its pace (time in motion over the distance), the pace now, and its splits by kilometre or
   mile. It is never a goal: one quantity and an optional cadence stay the rule, and no
   calorie target.

8. **Routes are kept apart from the days.** The points go in a second database, `routes.db`
   (`RouteDatabase`): a row an outing and its points in compact chunks (offsets of time,
   latitude and longitude in millionths of a degree, altitude and accuracy, as varints; about
   10 KB an hour). The outing's numbers (the fused distance, the steps' own, the measured
   share, the climb) are columns of `session` in `passo.db`, schema v7 by auto-migration: they
   are numbers, not places. A chunk is written after the step batch's transaction; the outing
   in `passo.db` is the truth, and a route that lost its last chunk to a crash is a shorter
   line, never a lost outing.

   *Amended 18 Nov 2026 (owner):* the outing's numbers also hold its **legs**, a hundred
   metres each (the fused metres, the steps' own time in motion and their count, the climb
   and the descent), in a table of their own, `session_leg`, beside the climb's two columns
   of `session`: schema v8, by auto-migration. The splits by kilometre or mile (decision 7)
   are cut from them, so a kilometre's pace is the one the outing's own is made of, and the
   splits stay when a route is deleted. They are numbers, not places, as above: no leg says
   where it was walked. `routes.db` is at schema 2: a route says where its heights came
   from, the barometer or the satellites.

9. **The backup: routes do not go to the cloud.** `routes.db` is named only under
   `<device-transfer>` (the direct move to a new phone), not under `<cloud-backup>`. The
   reader's own export carries the routes only when they tick it; an outing's route can be
   saved as GPX, and deleted alone, and all routes at once, from Your data. An import adds
   routes by their outing's start instant and never takes one away.
   *Amended 18 Nov 2026 (owner: hiding where a walk starts and ends "seems a little extreme";
   the plan of Phase 16 had «Trim the ends», on by default for a share):* **what the reader
   shares is the route as it was walked.** Passo has no network and no feed: a share is one
   act of the reader's, to a person or an app they chose, of something they are looking at.
   The image is a line on Passo's own ground, with no street to read a door from; the GPX
   holds the points, and its choice says so in one line, which is the honest thing a hidden
   default is not. A switch, a remembered setting and a refusal for short routes come out of
   the page.

10. **Battery: a second written exception.** The receiver is on only while an outing with GPS
    counts and the reader walks; it sleeps after 2 minutes still, stops at a pause, and the
    outing ends by itself as every outing does. The cost is expected to be several times an
    ordinary outing's (ADR 0013 measured 1 to 3% an hour for the wake lock): a few percent of a
    battery an hour, to be measured on the owner's phone in Phase 15 and written in
    `docs/battery/`; the editor then says the measured figure beside the switch, as it does for
    the wake lock. Batching (`setMaxUpdateDelayMillis`) is asked for and measured, never
    assumed.
    *Amended 18 Nov 2026 (owner):* two things are added to what the exception allows, each
    bounded. On a phone with no wake-up step counter, the processor kept awake while an
    outing with GPS counts (decision 5). And the receiver while an ended outing can still be
    taken back (decision 14): 15 minutes at most, asleep 2 minutes after the reader stops,
    off when the card is put away.

11. **A route is drawn in code, with no tiles.** There is no network to fetch a map with, and
    no map pack to carry. A route is a line on Passo's own canvas with its scale, north, start,
    end and kilometre beads; where it lies inside a city Passo already carries, the city's own
    map is under it. A reader who wants it on a street map shares the GPX with a map app. An
    offline map library was weighed and left out: a large dependency for a job other apps do
    well, and charts and maps are Passo's own Canvas code.

12. **The step that learns.** Each outing with GPS has clean stretches (measured from end to
    end, at a steady cadence) that say how long the reader's step is at that cadence. From them
    Passo keeps a curve, cadence to step length, and proposes it when it has enough («Your
    brisk step is 81 cm, not 76: distances read 3% longer»). The reader keeps it or not, as with
    the calibration wizard; it applies from then on, never to a frozen day unless they ask for
    «Apply profile to past data». A reader who uses GPS once a month gets better estimates
    every day of it.

13. **On the spot.** The ways and the city walks stay imaginary and say so; a reader who is
    there can walk them for real, with the same content:
    - a city walk with «I'm here»: the point is the reader's place on the route, a place is
      told when it is reached, a vibration of its own says the route was left and another that
      it was found again;
    - a way walked in person, a stage or a week at a time: the reader's place on a line fine
      enough to follow, the same two signals, a stamp in a second ink for each stage reached on
      foot, and the stretches walked in person kept and drawn («37 of 123 km walked there»),
      beside and independent of the journey from home.
    Never a navigation to the start, never a turn spoken with a street's name: Passo has a line
    and where the reader is on it, and says only that.
    *Made plain on 18 Nov 2026 (owner):* a way or a city walk from home never reads the
    satellites. Two ways to walk one, each with one meaning: from wherever the reader is, on
    their steps, and there, with GPS. A third, a far city measured with the satellites of the
    reader's own streets, would read as a navigation that is not there. It is not a matter of
    privacy: an outing of the reader's own, started at their door with GPS on, is measured
    from its first step. The Ways' sentence, "No location, only your steps", stays true of
    every way from home.

14. **The longest outing: asked before it ends, and the reader may go on.** Four hours stays
    the cap for every outing, GPS or not: beside the end by stillness it is what stops an
    outing forgotten by a reader who goes on walking all day, and with GPS on that forgetting
    would cost a battery. But a cap that ends a walk the reader is still on, without a word,
    is a fault, and with the receiver going off it would be a worse one (owner, 18 Nov 2026:
    "before the GPS is turned off, is the reader warned, and can they say to go on?"). So,
    for every outing, with GPS or not:
    - **Fifteen minutes before its cap** the outing asks: a vibration of its own kind (the
      stillness's two long pulses) and a notification, «Still walking? This outing ends in 15
      minutes», with «Keep going». It rides on the first step batch past that moment, as every
      signal does: no timer, and a reader who is not walking is not asked (the stillness has
      ended the outing, or will).
    - **«Keep going» moves the cap four hours on**, from the card too, and can be said again
      before the next one: a reader who answers is not a reader who forgot. Nothing stops, the
      receiver included.
    - **Unanswered, the outing ends at its cap and says so**, and for 15 minutes after it can be
      taken back with «Keep going», with the steps since, as an end by its goal or by stillness
      can (`KEEP_GOING_MILLIS`). In an outing with GPS the receiver stays on through those 15
      minutes, the reader being by definition still walking, so a route taken back has no hole;
      then it stops and the points kept aside are dropped.
      *Amended 18 Nov 2026 (owner):* the same holds after every end that can be taken back,
      not the cap's alone. The goal is crossed by a step and the end by stillness is noticed
      by one: there too the reader is walking, and an outing taken back ten minutes later
      would have a hole in its route and could lose the word «measured» for it. One rule:
      while an ended outing with GPS can still be taken back, the receiver does what it does
      while the outing counts (awake while the reader walks, asleep 2 minutes after they
      stop) and its points are kept aside; putting the card away, a new outing or the end of
      the 15 minutes stops it and drops them. The price is at most 15 minutes of the receiver
      after an outing, paid only by a reader who walks on and leaves the card where it is.
      *Amended 19 Nov 2026 (the review of Phases 15 and 16):* the question says the minutes
      really left, fewer than fifteen when the first steps within them came late. A pause is
      not time towards the cap: resuming moves the cap on by the pause, and a question already
      open stays open, so an outing paused across its cap is asked before it ends, never ended
      at the first step after it.
    Two outings start with twelve hours instead of four, so that a long day is not interrupted
    by the question at all: one on a way walked on the spot, and one that follows a route
    (Phase 20), a day's stage being six to eight hours with its stops; they ask before twelve
    in the same way. The end after 15 minutes still and after an hour paused are unchanged
    everywhere. This amends `docs/adr/0009-sessions.md`, decision 6 ("at four hours"), in the
    change that builds Phase 15.

15. **One APK.** A second build without the permission would keep "cannot" for the reader who
    wants it, at the price of two artifacts to test, sign and explain at every release, and of
    a choice asked of every new reader before they know the app. The allowlist gate, the
    permission asked only in context, and Android's own pages do that job.

16. **What stays out.** A catalogue of sports, cycling, training plans, segments, rankings,
    friends, live sharing, anyone else's routes, a heat map of anything but the reader's own
    walks, an outing that starts by itself where the reader is, reminders tied to a place, and
    `ACCESS_BACKGROUND_LOCATION` for any reason. Passo is a step counter that can measure a
    walk.

## Why this is acceptable now

- **The October objections, answered.** (1) is true for location and is the price, paid in the
  open: the README, VISION and the guide say what changed and what did not; "cannot send"
  stands, and the gate is stricter (decision 2). (2) is closed by decision 9. (3) is bounded as
  ADR 0009's exception is, by a walk the reader started, and the steps switch the receiver off
  whenever they stop (decisions 5 and 10). (4) OpenTracks records a route; it does not fuse it
  with steps, teach a step counter, or walk a way.
- **Nothing is taken from the reader who says no.** No screen asks for the permission before
  an outing with GPS is started; the switch is absent on a phone with no receiver.
- **It sharpens the product's own promises.** Honest estimates gain a measure to be checked
  against; the calibration no longer needs a hundred metres known in advance; the ways gain the
  reader who actually goes.

## Consequences

- New permissions and a manifest type (decision 2); `checkForbiddenPermissions` becomes the
  allowlist check, and its tests cover both an unlisted and a forbidden permission.
- A second Room database and its backup rule; `passo.db` schema v7; the backup file gains
  added fields (no new format version: an older reader skips them).
- The documents listed under «Amends» change in the change that builds Phase 15, not before:
  until then the invariants stand as written, and no location permission is in the manifest.
- Phases 15 and 16 are released together or not at all: an outing with GPS and no page to see
  it on is half a feature, and a permission in a release with nothing to use it is worse.
- A future Play listing (Phase 9) gains the location declarations and a privacy policy that
  says the above.
- To be measured and proven on a device (owner), each written in its phase: the second service
  type on a running service, the battery an hour, the receiver's sleep and wake, the signals on
  the spot.

## Sources

- Restrictions on starting a foreground service from the background (the while-in-use rule and
  its exemptions):
  https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Foreground service types (`location`, `health`, their prerequisites):
  https://developer.android.com/develop/background-work/services/fgs/service-types
- Launch a foreground service (adding a type with a second `startForeground()`):
  https://developer.android.com/develop/background-work/services/fgs/launch
- Location permissions, and asking for them at run time:
  https://developer.android.com/develop/sensors-and-location/location/permissions
  https://developer.android.com/develop/sensors-and-location/location/permissions/runtime
- `LocationRequest.Builder`:
  https://developer.android.com/reference/android/location/LocationRequest.Builder
- `AltitudeConverter`:
  https://developer.android.com/reference/android/location/altitude/AltitudeConverter
- Android 17's changes (nothing on location, 18 Nov 2026):
  https://developer.android.com/about/versions/17/summary
