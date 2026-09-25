# ADR 0005: Two widgets in Chiaro's dress (Phase 4)

- Status: accepted
- Date: 2026-09-25

## Context

PLANNING.md §7 planned one Glance widget, resizable from 1×1 to 4×2. For Phase 4 the owner asked
for **two**, equal in their minimum and maximum size and adaptive in layout, styled after
Chiaro's widgets (typography, the card, and their settings: colour, opacity and the rest) so
that a Passo card beside a Chiaro card on one home screen reads as one ecosystem: one in the
style and spirit of Chiaro's «In parole» (words, small marks if needed), one with a graphic
part in the style of Chiaro's «Colpo d'occhio».

## Decisions

1. **Two cards, named after Chiaro's**: «Colpo d'occhio» / «At a glance» (`GlanceWidget`) and
   «In parole» / «In words» (`WordsWidget`). The glance card is Chiaro's with the ring where the
   weather glyph was: the picture the card is read by across a room. The words card is Chiaro's
   four ranks of type (hero Bold scaled to the grant, sentence 18 sp Medium, facts 16 sp, the
   footnote), with two marks at a line's size: a check before a met goal and a pause.
2. **One size spec for both**, checked by a test: default 4×1 (Chiaro's pair opens there), a
   minimum of one cell (`minResize*` 40 dp) and **no maximum**. A maximum in dp cannot mean "four
   cells": Launcher3 converts it to cells for every grid profile it supports and keeps the
   smallest answer, so a value that allows four cells in portrait refuses them where landscape
   cells are wide, and one that never refuses them is not a cap. Every layout instead has a form
   for any grant: the glance card is DOT (one cell), NARROW, WIDE, TALL or PANEL; the words card
   LINE, ROW, STACK or PANEL. The arithmetic is pure and pinned by `GlanceLayoutTest` and
   `WordsLayoutTest` at Chiaro's reference grants; what only the launcher's face can answer (a
   count's width, a sentence's lines) is measured with `Paint` / `StaticLayout` in the system
   face, which is the one the launcher draws in.
3. **Chiaro's card, value for value**: the 24 dp corner, 14 dp insets (6 above and below a
   one-row card), the grounds (light, dark, the phone's, or one of **Chiaro's six colours**,
   hex for hex, in `WidgetPalette.kt`), the opacity slider in 5% steps, and **Chiaro's ink
   rule** (`widgetInk`, `WidgetInkTest`): white inks on a colour, the scheme's own on light or
   dark, and below 50% solidity the wallpaper's hint decides for the colour and the phone's card.
   The default is Chiaro's: a solid blue card. No sky ground: Passo has none to draw.
4. **Per-widget settings**, from the launcher's reconfigure flow (`configuration_optional`, so a
   card is placed at once): the real card at the top, drawn by the same composition the
   receiver runs through `GlanceRemoteViews`, with chips for every reference size and a line
   saying what that size carries; the background, colour and opacity; the content switches that
   mean something on that card (sentence and hours on the glance card, sentence, goal, distance
   and calories, and the day in figures on the words card); the ring's side on the glance card.
5. **The ring is a bitmap** (the §7 spike). The alternative was 21 vector levels at 5% steps plus
   a notch per level: the arc would stop at the nearest 5% and the notch would put "usual" up to
   400 steps away from where it is, for 40-odd drawables that still could not take the card's
   inks without a layer each. The bitmap is painted at the exact size shown, capped at 416 px a
   side (0.7 MB at most, about 50 KB for a one-row ring), one per card: far under the Android 17
   RemoteViews bitmap cap, which is sized to the screen.
6. **The day hour by hour is boxes**, as planned: 24 bars against the busiest hour, the hour
   under way in the ring's ink and the hours gone in the same ink lighter, the hours to come a
   baseline. Glance drops the eleventh child of a container silently, so the bars are four
   groups of six, and a group's first bar carries the label (00, 06, 12, 18).
7. **Push, never poll** (§7, §9): the service reports what happened through `WidgetUpdates`
   (declared in `:core:data`, bound by `:widget`) and `WidgetUpdatePolicy` in `:core:domain`
   decides: at once at screen-on (after the flush), at most once a minute while the screen stays
   on and only if the count moved, at once for a new day or a goal just reached, never with the
   screen off. Two events repaint with the screen off too, one repaint each: a change in tracking
   (a stopped service cannot repaint at the next screen-on) and a setting (only changed with
   someone in the app). `updatePeriodMillis` is 0.
8. **A count that is not moving says so** (`CountingState`): paused, stopped by the system, or
   without the permission, the card says it in the sentence's place ("Paused · tap to resume"),
   or in a footnote where there is no such place, and the ring goes quiet with a pause mark.
   Tapping a paused card opens the app asking it to resume (`TrackingControl.EXTRA_RESUME`): the
   foreground service is started from the activity, where Android allows it, and the reader sees
   it happen. An `ActionCallback` would have tried to start it from a broadcast.
9. **The same numbers as Today**: the model is `TodayOverview` over the stored minutes plus the
   service's buffer (`LiveSteps`), so the widget and the screen never tell two stories. The usual
   day is computed once per day, not at every repaint.
10. **Previews**: a static `previewLayout` of the default card for the picker, and on Android 15+
    the generated previews (`providePreview` from a seeded sample day), published once per app
    version because the platform rate-limits the call.
11. **Glance brings WorkManager**, which runs Glance's sessions. Its `WAKE_LOCK` stays (it is held
    by the job while a card is drawn, which the policy allows only with the screen on, bar the
    two events above); its `ACCESS_NETWORK_STATE` is removed from the merged manifest, since
    Glance's workers never wait on a network and Passo has none.

## Consequences

- PLANNING.md §7 describes the two cards; the single-widget table is superseded.
- The acceptance checks that need a phone (two launchers, light and dark, a repaint within about
  5 s of the screen coming on, no repaint while it is off) are the owner's field test.
- Chiaro's card colours and ink rule are copied, not shared: a change there is carried here by
  hand, as the schemes are (ADR 0004).
