# ADR 0004: Chiaro's design language, and the Today screen (Phase 3)

- Status: accepted
- Date: 2026-09-25

## Context

Passo, Chiaro and Saldo are one family, and the owner asked for Passo's interface to be
inspired by Chiaro's style (not its content), with the same typeface choices and the same
default typeface. Phase 3 is the first real screen, so this is where the visual language lands
(PLANNING.md §15 had it open until now).

## Decisions

1. **Chiaro's colors, value for value.** Both dresses, Paper (warm white, amber) and Vivid (cool
   white, azure), are Chiaro's generated schemes, copied with their provenance
   (`theme/Scheme.kt`). Vivid is the default and dynamic color is off by default, as in Chiaro:
   the app looks like itself, and the family looks like one. The reader can pick the palette
   and turn wallpaper colors on in Settings.
2. **Semantic colors borrowed where the meaning is the same.** A met goal wears Chiaro's
   "pass" verdict pair (ink and container, per dress and theme); effort (cadence) wears
   Chiaro's warm UV ramp. Never the color alone: a met goal also has a check and a word.
3. **Chiaro's type.** Google Sans (default), Inter, or the phone's sans; both bundled faces are
   Chiaro's variable files (OFL), so the two apps are the same drawing. The same scale and
   adjustments, the same hero (bold, −0.02em, tabular) and tile reading (light, tabular).
   `TypographyTest` checks every Material role follows the setting.
4. **Chiaro's shapes, spacing and motion**: the 4/8/12/16/28dp scale, 24dp group grounds,
   springs with a 100 ms fade under reduced motion (read live from
   `ANIMATOR_DURATION_SCALE`), and the 300 ms page transition that predictive back seeks.
5. **Chiaro's principles, applied to steps.** One sentence before any number (Today's
   headline); every number says what to do with it (each tile's second line); estimates say
   so; the screen does not lie (no dead tabs, no switch for a feature that has not shipped, a
   section with nothing to say is not drawn, the cadence tile included).
6. **Icons drawn in the app** (`PassoIcons`): about twenty line icons on a 24-unit grid, the
   weight of Material Symbols' outlined set. Twenty shapes did not justify a new dependency.
7. **The Today screen's one "wow", chosen for use**: the ring carries a **notch where a usual
   day of the same weekday stands at this hour**, so ahead or behind is visible before the
   headline says it; the day chart can be **read with a finger** (tap to pin, drag to scrub, a
   haptic tick per hour, a vertical move left to the page); the chart **draws itself in along
   time** and the count counts up the first time; a met goal **blooms once**. All of it is off
   under reduced motion, and none of it gates information.
8. **Live while visible.** The service publishes today's count, stored plus buffered, to an
   in-process `LiveSteps`; Today reads it only while collected. No new work with the screen
   off: the service computed that number for its notification already.

## Consequences

- A change of Chiaro's schemes or fonts is copied here by hand; the provenance comment says
  from which commit.
- Two fonts travel in the APK (about 1.2 MB), whatever the setting says; both are credited.
- The home-screen widget (Phase 4) is drawn by the launcher in the system face whatever the
  setting says, as Chiaro's are.
