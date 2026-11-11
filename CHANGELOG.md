# Changelog

All notable changes to Passo are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions follow
[Semantic Versioning](https://semver.org/).

`release.yml` reads the section whose heading matches the tag being pushed (`## [1.0.0]` for
`v1.0.0`) and uses it as the body of the GitHub Release, so a version's entry is written
**before** its tag, and kept to what somebody arriving at that page wants to read.

## [Unreleased]

### Better

- **Outings, easier to read and to start.** Each outing is a shorter card that reads on one
  line, with one button to start it and a touch on the card to change it; a new one is at the
  top of the page. An outing under way leads with its sentence, shows how far it is and where
  it ends at the two ends of its bar, and its numbers in three columns; Stop and Pause are
  larger, for a hand in motion.

- **The outing's editor, lighter.** The signals are a legend: each with its pattern drawn
  beside it, and a touch to feel it. The interval settings are a line each, and the Japanese
  walk is explained in two lines, with the rest a touch away.

- **City maps you can read at a glance.** The way stands out from the streets, the side streets
  step back, and a name no longer covers the line it names. While you walk, the map names the
  place ahead. The list of places is one line running through them, as far as you have come.

- **Insights, tidier.** The streak on a plain card, the way you are walking with its bar from
  where you are to where it ends, the records without their trophies and with the number in
  full ink, and your averages side by side, as Today's three amounts are. Dates say their month
  once, and no line leaves a word or a number alone.

- **Your year on foot, steadier.** Whose year it is stays at the top of every page, so the
  words no longer jump as you go on. A page's number stays on one line on a narrow phone, the
  outings and the ways show theirs large as the other pages do, and the picture you share or
  save no longer carries the line on how to turn the pages.

- **History, easier to read.** Touch a bar and its reading is two short lines, the day with
  its steps and whether the goal was met, with «Open» beside them; the chart no longer moves
  under your finger. The four scales are whole words on a small phone, in Italian too. A day's
  walks show what was counted and what is estimated on two tidy lines, and no sentence leaves
  its last word alone. The tiles' marks are quieter, in Insights as well.

- **Today, calmer.** A finer ring on a plain page, with only your steps and your goal inside it.
  Under the sentence, the day’s distance, active calories and active minutes in one row, with one
  line on what the estimates rest on. Beside the date, how many days in a row you have met your
  goal. The day’s chart without the grey block over the hours ahead, and with the goal named on
  its line. Brisk minutes and cadence together in one card, «Intensity», and the way to an
  outing as a full row that says what an outing is.

- **A new icon**: two footprints on green, the one behind white and the step you are taking
  amber. The notifications, the Quick Settings tile and the outings' shortcuts follow it, the
  shortcuts in the same shape as your launcher's icons. Each shortcut wears its outing's own
  mark, as on the Outings page: the Japanese interval walk its slow and fast line, a run its
  bolt.

- On «At a glance» the ring keeps one size all day at every size, however long the sentence
  under it. On a widget three or four cells wide and two rows high or more, the ring beside the
  hours is larger, and larger still with the day’s sentence turned off.

## [1.1.0] - 2026-11-02

**Walk somewhere.** Passo still counts every step on your phone and sends nothing; now your steps
can carry you along a long way or through a city, an outing can be a Japanese interval walk, and
your year can be told in pages.

Android 14 (API 34) or newer, with a hardware step counter. Installs over 1.0.0 with everything
kept. Check the download with the `.sha256` file beside the APK, and the signing certificate
against the fingerprint in the [README](https://github.com/fiorenzobrioni/passo#install).

### New

- **Ways**: ten long ways on five continents, from the Via Francigena to the Kumano Kodō and
  Kilimanjaro, walked from home with the distance of your days: stage by stage, a stamp for each,
  a notification at each, and the foot of Today saying how far the day carried you. No location.
- **City walks**: forty walks through as many cities, from Milan to Tokyo and Cairo, walked where
  you are in one outing or a few; each place told as your steps reach it, with a vibration and, if
  you like, a line in your headphones.
- **The Japanese interval walk**: 3 slow minutes and 3 fast ones, five sets or more, each change
  felt in your pocket; at the end, each fast interval against its pace.
- **Your year on foot**: a year told in pages, from Insights or History at any time, and in
  December on Today; share a page as an image if you like.
- **At a glance can show your week**: the last 7 days against the goal under the count.

### Better

- Goal notifications alert every day, and go by themselves at midnight; the evening reminder
  goes once you walk it, and never comes during an outing.
- An outing left running no longer keeps the phone awake; the phone does less at each unlock.
- Steps handed over in clumps land in the minutes they were walked in; nothing on the widgets is
  cut; removing a widget no longer closes Passo.
- Insights' streak card says since when; the interval walk has a mark of its own.

### Private and light

- Still no `INTERNET` permission, and no new permission of any kind.
- The ways, the cities and your year are computed on the phone, from your own steps.

The full development record is in
[docs/CHANGELOG-1.1.0.md](https://github.com/fiorenzobrioni/passo/blob/main/docs/CHANGELOG-1.1.0.md).

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
