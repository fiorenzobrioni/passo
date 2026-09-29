# ADR 0012: Foldables and the accessibility pass (Phase 7)

- Status: accepted
- Date: 2026-09-29

## Context

Phase 7 closes with two polish items: "Adaptive layouts for tablets and foldables" and
"Accessibility pass (TalkBack, font scale 200%, contrast, touch targets)", and its acceptance
says "The accessibility scanner reports no critical issues."

The owner's direction (29 Sep 2026): **no tablet layout** (Google Fit and Samsung Health have
none either, and Passo is a phone app: it counts the steps of the phone in the pocket); a
foldable layout **yes**, because a foldable goes on walks and runs, and the accessibility pass
**yes**, both only if the risk of breaking the app is low.

Before this pass nothing in the app looked at the window's width: an open foldable stretched
the ring, the cards and every line of text across the inner screen. The screens were already
built with TalkBack in mind (one node per tile, a spoken summary per chart, a node per bar and
per calendar day), but nothing checked it, and nothing had ever been drawn at a large text size.

## Decisions

### Foldables: one centred column, never a second layout

1. **A page's column is at most 640dp wide, margins included** (`PageMaxWidth`,
   `theme/PageWidth.kt`): Material's bottom-sheet width, a phone's column and a little more.
   Wider windows (a foldable open, a phone on its side) keep the column in the middle. Narrower
   ones, every phone held upright and a folded foldable, are laid out **exactly as before**: the
   gutter is zero there, which is what makes the change low-risk.
2. **Only the content moves in; grounds still span the window.** `pageGutter()` gives each
   page the room to leave on each side, and the page puts it where it belongs: in a list's
   content padding (so a scroll or a swipe still starts from the edge of the screen, and
   History's pager still swipes across the whole width), inside Today's hero (its glow still
   reaches both edges), in the insets of the top bars and of the bottom navigation bar (their
   ground spans the screen, their title and tabs stand over the column), in the bottom action
   bars.
3. **The side insets come with it.** On a page without a `Scaffold`, the gutter includes the
   display cutout and a side navigation bar, which a phone on its side has and nothing handled
   before; a `Scaffold` page already pads for them, so it takes the spare room only
   (`sideInsets = false`).
4. **What is not done**: no list-detail or two-pane layout, no navigation rail, no tabletop
   posture. Each would be a second layout to keep, for a gain the owner did not ask for, and the
   rail and the postures would need `androidx.window` or `material3-adaptive`, new
   dependencies. The window is read with Compose's own `LocalWindowInfo`.

### The accessibility pass: checks in every screen test, and fixes where they found something

5. **A new test-only module, `:core:testing`**, taken by every feature module and the widget as
   `testImplementation`. Nothing in it reaches the app.
   - `assertAccessible()` runs the Accessibility Scanner's checks that the semantics tree can
     answer, on every window on screen: **every control says what it is** (a text, a
     description or a value), and **every control has 48 by 48dp for a finger**. Compose widens
     a smaller control's touch area to 48dp by itself (and reports it that way to TalkBack and
     the Scanner), so the check models what actually happens: where two widened areas overlap, a
     touch goes to the nearer control, and each keeps half the overlap. Its own tests show what
     it catches and what it lets through.
   - `walkPage()` walks a page a screenful at a time, checking each and writing it to
     `build/screenshots`.
   - Every existing screen test runs `assertAccessible()` on each state it draws; every screen
     has a test at **twice the text size on a 360dp phone** and one **on an open foldable**
     (841 by 701dp), which walk the whole page.
6. **Targets packed on purpose are exempt from the size, and only from it**
   (`DENSE_TARGETS_TAG`): a chart's bars, a week of a month's days, and Material's clock dial.
   A 7-day row cannot give each day 48dp on a small phone; each is still its own labelled node,
   reached one by one by swiping in TalkBack, and a bar is also reached by dragging along the
   chart. Their labels are still checked.
7. **Contrast is pinned by a unit test**, `ContrastTest` in `:core:designsystem`: every text ink
   on every ground the screens set it on, in both dresses and both themes, at WCAG's 4.5:1 (the
   Scanner's threshold), including a status card's softer body and every level of the calendar.

### What the checks found, and the fixes

8. **The calendar's numbers.** The day's ink was chosen by a luminance threshold, which picked
   the dark ink at 2.6:1 in the middle of the paper ramp; the days not counted were set in the
   outline colour, 3.8:1 on their card. Now the ink is whichever of the two reads better, the
   uncounted days use `onSurfaceVariant` (they are told apart by their missing ground, not by a
   fainter number), and the ramp's stops are chosen per theme so that every level has an ink at
   4.5:1 or better: 0.28, 0.55, 0.90 of the way to the accent in the light theme, 0.20, 0.35,
   0.75 in the dark one. In neither theme does any ink reach 4.5:1 in the middle of the ramp, so
   each steps over it on its own side.
9. **The widget's colour swatches** were 36dp (28 for the chosen one) with 10dp between them:
   each is now a 48dp target around the same 36dp disc, in a row that wraps on a narrow phone.
10. **At twice the text size**, the tiles broke their labels in the middle of a word
    («Distan-ce»), Today's chart title was squeezed one letter a line by its key, the charts'
    labels overran their 44dp gutter and 20dp axis into the caption and into each other, the
    segmented buttons read «We…» and «Mo…», History's date was cut, a walk's time broke between
    «12:55» and «PM», and the guide's example ring spilled its count over the arc. Now:
    - `TilePair` puts two tiles side by side while each has at least 120dp at the reader's text
      size, and one above the other below that (a foldable keeps them side by side at any size);
    - a title and its key share a line when they fit and wrap when they do not (Today's chart,
      a walk's time and length);
    - `chartMargins` grows a chart's gutter and axis with the text, and a label that would touch
      the one before it is left out (the current one is always kept); at the standard size they
      are 44 and 20dp exactly as before;
    - `SegmentLabel` shrinks a segment's word only as far as it must, never below its size at
      the standard setting;
    - History's period name and dates take a second line rather than an ellipsis, a time never
      breaks inside itself, and the guide's ring grows with the text.

## Consequences

- No new permission and no new dependency: `:core:testing` uses the Compose test library and
  Truth the modules already had.
- Phones held upright look exactly as they did at the standard text size, save the two things
  the checks found wrong: the calendar's colours (README's month screenshot regenerated) and the
  widget swatches' spacing (the widget settings screenshot regenerated).
- A new screen gets the checks by calling `assertAccessible()` or `walkPage()` in its tests,
  and its lists take `pageGutter()`.
- **Still for the owner, on a device**: the Accessibility Scanner app itself over the main
  screens (the checks here are its touch-target and label checks, and the contrast of the
  theme; the Scanner also reads the rendered pixels), a walk through the app with TalkBack, and
  the app on a foldable, opened and folded while it is on screen.
