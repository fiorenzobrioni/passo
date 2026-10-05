# Changelog

All notable changes to Passo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Added

- **Ways**: five pilgrim ways walked from wherever you are. The Via degli Dei, the Camino
  Portugués from Porto, the Via di Francesco, the Camino de Santiago (the French Way) and the
  Italian part of the Via Francigena, drawn from OpenStreetMap. The distance of your days
  carries you along, stage by stage, with a stamp in a credential for each and a notification
  when you reach one. Start today or in the past, and find yourself already on the way. No
  location: only your distance. From Insights.
- **City walks**: Milan, from the Duomo to the Navigli; Rome, from the Colosseum to St Peter's;
  Paris, from Place des Vosges to the Eiffel Tower; London, from Buckingham Palace to Tower
  Bridge; Madrid, from the Temple of Debod to the Puerta de Alcalá; Lima, through its historic
  centre; and Cusco, up to Sacsayhuamán and down to the Qorikancha. And three short ones, about
  5 km, an hour or so: Porto, from the cathedral across the Douro to the Serra do Pilar;
  Amsterdam, from Centraal Station along the canals to the Westerkerk; and Prague, from the
  Castle over Charles Bridge to Wenceslas Square. Each map has the city's
  water, parks and main streets, so the area is recognisable at a glance. Walked where you are
  in one outing or a few. Each place is told as your steps reach it, with a short vibration and,
  if you like, its name and a line about it in your headphones; the notification says the next
  one. Stop when you like and continue later from where you were. On the Ways page, and from
  Outings. "Hear it" plays a place as you will hear it, and "Change voice" opens the phone's
  voices.
- **The Japanese interval walk**: slow and fast walking in turns, 3 minutes each, five sets or
  more, as a study at Shinshu University proposed it. Ready to start as "Japanese walking", or
  made to measure in Outings (the slow and fast minutes, the sets, the fast pace), with a short
  explanation of what it is where you choose it, and more in the guide. Each change is told with
  a vibration of its own, "faster" and "slower", and with the voice if you like, on time with
  the phone in your pocket. The minutes are minutes in motion: a traffic light does not eat a
  fast interval. On the screen and in the notification, the interval you are in and what is left
  of it; at the end, each fast interval's cadence against its pace, and how many were at pace.
- **Your ways can be tidied**: a way finished or left, or a walk walked to its end, can be
  deleted, after a warning that it is for good. A way left before you walked any of it is not
  kept. A city walk begun can be left too, between outings: it goes back to its start, and
  its outings stay in History.

### Changed

- **Signals on time on every phone.** On phones whose step counter cannot wake them (some
  Samsungs among them), an outing's vibrations and voice could come a minute or two late with the
  screen off. There, an outing with signals now keeps the phone awake while it counts, for a
  little more battery (about 1 to 3% an hour); the outing editor and a city walk's page say so. Outings without
  vibration or voice, and every other phone, are as before.
- **An outing's distance on phones that hand steps over in clumps.** The same phones made an
  outing read its pace as a run, and measure its distance and calories with the running step
  (about a quarter too long). Its pace is now measured over the time the steps took.
- **Steps land in the minutes they were walked in, on phones that hand them over in clumps.**
  Some phones (some Samsungs among them) deliver the steps of a minute and a half at once while
  the screen is off; Passo put them all in one minute, which could read as a minute of running
  and make the distance and calories of the day too high. They are now spread over the time they
  took. The day's steps were always right, and days already recorded stay as they are.
- **Nothing on the widgets is cut any more.** On some phones (a Samsung among them) both cards
  could cut the count («9.6…») or the end of their sentence («Goal reached at 14:…»): they
  measured their text in a different typeface from the one the home screen draws it in. They now
  measure in the home screen's own, with a little room to spare; a sentence that would not fit
  says the same thing in fewer words («Reached at 2:23 PM»), and the count is made smaller rather
  than cut, past ten thousand steps too.
- **The first-day note** on Today goes as soon as you open the guide from it, instead of
  staying until the end of the day.

## [1.0.0] - 2026-10-01

**The first release.** Passo counts every step with the phone's own step counter, even if you
never open it, and keeps everything on the phone. No account, no ads, no tracking, and no
permission to use the internet at all.

Android 14 (API 34) or newer, with a hardware step counter. Check the download with the
`.sha256` file beside the APK, and the signing certificate against the fingerprint in the
[README](https://github.com/fiorenzobrioni/passo#install).

### What is in it

- **Today**: your steps against the goal, where a usual day stands by now, and one sentence on
  the day.
- **Your day**: a chart you read with a finger, plus distance, calories, active and brisk
  minutes, cadence. All estimates, and they say so.
- **History**: a day, week, month or year at a time, and a calendar of how close each day came.
- **Walks, found for you** in the steps already counted.
- **Outings**: a walk or run with a goal, with vibrations or a voice at the milestones.
- **Insights**: your streak, your records and your averages.
- **Measure your step**: walk a distance you know, no GPS.
- **Two widgets** and a **Quick Settings tile**.
- **Notifications**, each optional: goal reached, an evening reminder, a weekly summary.
- **Your data**: a backup to a file, CSV for spreadsheets, an import that never deletes.
- **The guide**, light and dark themes, two palettes, three typefaces.
- **For every reader**: TalkBack, text up to twice its size, foldables.
- **English and Italian**, through the system per-app language picker.

### Private and light

- No `INTERNET` permission: Passo cannot send anything.
- No Health Connect, Google Fit or Play services: the step counter is the only source.
- No timers with the screen off; about 1% of a day's battery.

The full development record is in
[docs/CHANGELOG-1.0.0.md](https://github.com/fiorenzobrioni/passo/blob/main/docs/CHANGELOG-1.0.0.md).
