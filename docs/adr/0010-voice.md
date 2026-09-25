# ADR 0010: The outings' voice (Phase 10, second iteration)

- Status: accepted
- Date: 2026-09-25
- Builds on: `docs/adr/0009-sessions.md`

## Context

The first iteration of the outings told the reader on the way with vibrations. The owner asked
for spoken signals as a second iteration: the way the phone running apps do it (Nike Run
Club, Strava, Runkeeper, Pacer's coach), useful when the phone is in a pocket and the reader
has earbuds in, and a vibration is easy to miss.

## Decisions

1. **Per outing, three choices: off (the default), headphones, always.** "Headphones" speaks
   only through private audio (wired or USB headphones, Bluetooth audio, BLE headsets, hearing
   aids); "always" also speaks out loud from the phone, but only when the ringer is on: silent
   or vibrate mode means "not out loud", as for every other sound (`SpeechRoute`, pure and
   tested). Plans and outings from before are silent (schema v3, a column with a default).
2. **What is said.** The start ("Brisk walk: off you go. 20 minutes at a brisk pace."), each
   signal the reader chose with what is left and the pace against the outing's own ("Halfway.
   10 minutes to go. 108 steps a minute: on pace."), and the goal with what it came to ("Goal
   reached: 20 minutes. 2,140 steps. 17 of 20 minutes at your pace."). Every amount in words,
   in the app's language, with the Italian verb agreeing ("Manca 1 minuto", "Mancano 10
   minuti"). The vibrations stay; the voice follows the outings' channel as they do.
3. **The system's engine, an offline voice only.** Android's text-to-speech engine, with a
   voice installed on the phone for the app's language (the same country first, then quality,
   then latency); a voice that needs the network is never chosen, and network synthesis is
   refused in the request. Without such a voice nothing is spoken, and the editor says so with
   the button to the system's text-to-speech settings. Passo still sends nothing; no new
   dependency and no new permission (a `<queries>` entry lets Android 11+ show Passo the
   engine; it is not a permission).
4. **Over the music.** Navigation-guidance audio with a transient focus that lets the music
   duck, given back when the sentence ends.
5. **The tone: classic, with a sober warmth; never a coach** (owner's question, 25 Sep 2026).
   Passo speaks as its screens do: the fact first, estimates as estimates, no cheering. Two
   touches of warmth, both earned: below the pace, an invitation rather than a verdict ("pick
   up the pace a little" / "accelera un po'"); at the goal, one "Well done" / "Ben fatto" to
   close. Cheering at every signal would be noise by the third outing.
6. **Variety from what happened, never from chance.** The milestone sentences keep one shape,
   on purpose: in a pocket they are learned like the vibrations, and a listener parses a known
   shape at once. What changes is what is true of this outing: "Almost all of it at your pace"
   from nine tenths of its time in motion at the pace (the minutes otherwise), "Today's goal is
   reached too" when this outing's steps took the day across its goal, the goal said once when
   it was steps. No phrase is drawn at random.
7. **Which voice: the system's choice, no picker in Passo** (owner's question). The engine's
   API does not say whether a voice is male or female; a label guessed from names such as
   "it-it-x-itb-local" would be worse than none. The system's text-to-speech settings let the
   reader pick a voice by ear, with a sample: Passo uses the voice chosen there for the app's
   language (when it is installed and offline; the best offline one otherwise), and the editor's
   "Change voice" opens that page.
8. **Bound only while needed.** The engine is bound when an outing that speaks starts (or is
   picked up after a restart), and released after its last sentence; the editor binds it only
   while it shows a plan that speaks, to say whether a voice is there and to let the reader
   hear one ("Hear it": the plan's own halfway, with its numbers).

## Consequences

- No change to the battery rules: no timer, no wake lock, no new registration. A sentence is
  handed to the engine where a step or a touch already woke the processor.
- **To check on a device:** with the screen off and no music playing, the processor could
  suspend between the sentence being handed over and the engine starting to play it, and the
  sentence would then come with the next wake (the next batch of steps, within 30 s). With
  music playing the audio path keeps the phone awake and this cannot happen. If the field test
  shows late sentences, the fix is a short wake lock around each sentence, a few seconds per
  signal: that would widen ADR 0009's exception and needs the owner's decision first.
- The quality of the voice is the engine's. Which engine, and which voices are installed, are
  the reader's choice in the system's settings.
