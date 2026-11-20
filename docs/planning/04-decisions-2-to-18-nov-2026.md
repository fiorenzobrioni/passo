# PLANNING, archive 04: the decisions log, §15, 2 to 18 Nov 2026

> Frozen on 18 Nov 2026, with Phase 16 built and before Phase 17: a closed part of the plan,
> moved here from `PLANNING.md` without changing a line. Not edited. The live plan is
> [`PLANNING.md`](../../PLANNING.md); the index of every title is [`README.md`](README.md). Here:
> the log from 1.1.0's tag to Phase 16: no way on a widget, the family's circle, the new icon and
> the outings' shortcuts, the reviews of Today, the Outings, History, Insights and the year, the
> widgets' names, and location (ADR 0016), with Phase 15 (the measured outing) and Phase 16 (the
> outing's page) as they were built. The two phases themselves stay in `PLANNING.md` §11 until
> a phone has proved them. The log goes on in `PLANNING.md` §15.

---

## 15. Decisions log: 2 to 18 Nov 2026

- **No way on a widget** (owner, 2 Nov 2026, after 1.1.0's tag: "the Ways on a widget seems noise
  to me"; the question had stood open since Phase 11). Not built, and no longer asked. A way moves
  a few kilometres a day, so a line about it on a home screen would say the same thing all day,
  every day, beside a count that does move: the reason the way's card on Today shows only what
  the day did for it (archive 02, «The way under way on Today»). Where the reader stands is
  Insights' to say, and the way's page's; a stage reached is a notification already. The widgets
  stay about today.
- **«At a glance» and Tempo's «In words»: one circle at one size** (owner, 4 Nov 2026, with the
  two cards side by side on the phone, two by two: "make the circles of the two cards uniform, so
  that at the same size of card and the same content they are the same size; leave the circles'
  resizing as it is, but at the same size and text the circles must match"; the screenshots the
  owner named did not reach the session, the gallery's were drawn and measured instead). On the
  reference two-by-two (159 × 189 dp) the ring took the height its words left (the number, the
  sentence over two lines, the goal: 108.24 dp of 161), 52.75 dp, and grew where the sentence was
  shorter (73.75 dp for one line, 95 for none); Tempo's dial stood at 60.5 to 63 dp, held by the
  date beside it or by its note, larger on a free day. Now, under three cells wide, both cards'
  circle is the **tall circle** (`tallCircle` in `GlanceLayout.kt`; Tempo's `WordsFit.tallCircle`,
  the same arithmetic): the height the card's inside leaves over a budgeted band of words, a 34 sp
  line and three 16 sp lines at 1.32 em (108.24 dp at the default size, the ring's usual words),
  rounded down to a quarter, never under `TallRingMin`. Budgeted, never measured, so the day's
  words do not move it: on the reference two-by-two both draw 52.75 dp, a long sentence and a
  short one, a free day and a busy one; at 115% and 130% the band grows and both stand at 44 dp
  (the sentence yields a line first, as before). Where the reader hid the sentence the band is the
  number and the goal, and the ring takes that (95 dp on the two-by-two, as before). Each card's
  own limits still hold (here 104 dp and the card's width; on Tempo's the date's column beside the
  dial), so the two match wherever neither binds: on a card two cells wide up to about 199 dp tall,
  the two-by-two on every grid measured; a two-by-three keeps its 104 dp ring, Tempo's dial its 63
  dp. From three cells wide nothing changes: the panel's ring stays the header's 56 dp, and a tall
  card without the hours keeps its ring as before. Where the circle holds the ring under its room,
  the room stays air: the number grows only past the ring's 104 dp ceiling, as before. Tests:
  `GlanceLayoutTest` (the circle at the reference height and text sizes, one ring through the day
  at 159, 175 and 200 dp wide, the hidden sentence, three cells wide unchanged).
- **The family's circle at every size** (owner, 4 Nov 2026, after the entry above: "not only two
  by two: you must check every size; I wrote it badly before"; then, asked about the cards that
  stand in different compositions: two cells wide and three rows high, "Tempo grows", its dial to
  this ring's 104 dp with its date on a line over the title; three and four cells wide, "Passo's
  ring grows", one size per card for both). Measured on both galleries at every reference grant
  (dp; «In words» busy / free, «At a glance» with the hours / without): one cell 69 / 69 both;
  two by one 47 against 44; three and four by one 56 both; two by two 52.75 both; two by three 63
  against 104; three and four by two 78.5 to 82.5 / 101 against 56 / 74; three by three 104
  against 56 / 104; four by three 119.25 against 56 / 104. Now **the family's circle**, three
  functions with the same arithmetic in both apps (`dotRingSize`, `rowRingSize`, `tallCircle`;
  Tempo's `WordsFit.cellCircle`, `rowCircle`, `tallCircle`): one cell, the shorter side less 8 dp
  each side, 36 to 104 dp; one row, the height inside the snug insets with 84 dp for the words,
  44 to 56 dp, as before here; two rows and up, the height left over the budgeted band of words,
  with one line of sentence from three cells wide (where a sentence that takes two lines on two
  cells takes one), between `TallRingMin` and the ceiling `tallRingCeiling` (104 dp, or 45% of the
  inner side, Tempo's step 8.3) or the inner width, rounded down to a quarter. At the reference
  grants: 69; 44, 56, 56; 52.75, 73.75, 73.75; 104, 104, 119.25. Measured again: the same at every
  grant, with the hours or without, with Tempo's events or without. Here: **the panel's ring is
  the circle** (73.75 dp on the three- and four-by-two, from 56; 104 and 119.25 on three rows): two
  rows high it stands inside the height the number over the goal already gave the header, so the
  bars keep their 58.3 dp; three rows high they stay at their 112 dp. Where the ring leaves the
  sentence less than a column at the far edge (the four-by-three: 84 dp, under 96) and its height
  holds lines of it under the number over the goal, the sentence stands under the number, beside
  the ring (`panelStackedLines`: two lines on the four-by-three). Two side effects, accepted: on
  the four-by-two the words beside the larger ring have 18 dp less, so the goal takes its short form
  («of 10,000», the README's picture) and, where the sentence's column (107 dp in the gallery) is
  narrower than its two lines, the sentence takes three; on the three-by-three the goal is the short
  «of 10,000».
  **The tall card without the hours** takes the circle from three cells wide too: 73.75 dp on the
  three- and four-by-two (a sentence over two lines yields it, as before), 119.25 on the
  four-by-three (it was held at 104); the number grows past the larger of the ring and 104 dp, as
  before. Tests: `GlanceLayoutTest` (the circle at every reference grant; one ring through the day
  at five widths and four heights; the panel's header and bars; the stacked sentence's lines).
- **The panel's ring grows without the sentence, as the tall card's** (owner, 5 Nov 2026, with
  Tempo's «In words» kept to the event: "in Passo, when I hide the day's sentence the circle grows,
  and that's just right; I'd like a similar setting on Tempo's widget, so the circle can grow to
  the same size as Passo's"). Tempo's card gains «The day's note»: off, it says the event alone and
  its dial takes the band of a figure's line and one line, this ring's band without its sentence.
  Asked whether the panel should follow too, the owner: yes, so the two match there as well. The
  panel's ring is now `tallCircle(size, scale, sentence)` with the reader's choice, as the tall
  card's: two rows high 95 dp without the sentence (73.75 with it), the bars 37.5 dp (58.3 with it),
  still the panel (`glanceForm` and the preview's line take the choice too); three rows high the
  ceiling holds it, nothing changes. Tests: `GlanceLayoutTest` (the panel's header, bars and form
  without the sentence), `WidgetFitTest` (every card without the sentence, and the owner's
  two-by-two, 188 × 218 dp, measured from the phone's screenshots, added to its grants: nothing cut).

- **The new icon: two prints on the green** (owner, 9 Nov 2026: the four apps' icons redrawn
  from scratch as one series, "icons that say the app and recall what it does, full frame in
  Android's style, clean and readable when small, flat, the light from the same side for all,
  bright colours but not too much, one palette for the series"). Drawn first on a board, three
  symbols per app, two grounds, the home-screen check and the grid; the owner chose the series
  and asked for a margin between the drawing and the badge's edge (the first board let a symbol
  reach the edge). The mark is **two prints walking to the upper right**, the app's own sole:
  the one behind white, the one ahead amber, the step being taken, as the amber print was on the
  ring it replaces (archive 02, «The launcher icon: Chiaro's ring, Passo's step» and «The family's
  icons, made one suite», which this entry changes). The series' rules, the same in the four apps: the
  ground is the app's colour, lit from the upper left (the green `5CCB86 · 27A35A · 17834A`); the
  symbol white with one amber accent (`FFD36A · FDB528 · F28C17`); every shape a ramp from its
  light tone at the upper left to its deep one at the lower right, no shadow; the whole symbol
  within 28 units of the centre (measured 28.1), a 56 dp circle on the 72 that show, so a round
  mask leaves 8 dp of air and none reaches it. `tools/draw_launcher_icon.py` is rewritten: the
  ground is a drawing now (`ic_launcher_background.xml`) where it was the colour `#F7F4EE`, so
  `values/colors.xml`, which held only that colour, is gone and `ic_launcher.xml` points at the
  drawing. Inside the app, asked in the same review: the status-bar icon (`ic_stat_steps`, every
  notification and the Quick Settings tile) is the two prints in one ink, the outing's walker
  (`ic_progress_walker`, now the script's too) the two prints white and amber on a green disc,
  and the Outing shortcut (`ic_shortcut_outing`) its glyph in white on the green. The guide and
  the first run show no app icon, so nothing changes there. Checked: the XML the script writes,
  drawn and compared pixel by pixel with the approved drawings, differs only at the edges
  (0.6% of the badge's pixels, the antialiasing).
- **The new icon checked on the phone** (owner, 9 Nov 2026: "I merged the PRs of the other
  repositories too. I checked the APKs."; then "tick the boxes"). The device check of «The new
  icon: two prints on the green» is done; its box in «Open» is ticked.
- **The Outing shortcut, an adaptive icon** (owner, 9 Nov 2026, seeing the shortcuts of Passo
  and Tempo on the phone: "now I see a circle, and white around it"; then "make them adaptive,
  for Saldo and for the other two repositories too"). The shortcut drawn as a green disc («The new icon: two prints on
  the green», above, which this entry changes) kept its shape only under a round mask: under any
  other the launcher shrank it onto a white plate, as it does with any icon that is not
  adaptive. `ic_shortcut_outing` is now an `<adaptive-icon>`: the launcher icon's ground under
  the Outings glyph in white (`ic_shortcut_outing_foreground`, its 24 units at 55% of the 72
  that show, the share it had on the disc), the glyph again as the themed layer. The ground is
  written by `tools/draw_launcher_icon.py` a second time, into `:core:tracking`
  (`ic_shortcut_ground`), because a library cannot name the app's drawables. The family's rule
  for the launcher's shortcuts is now the white glyph on the app's colour, in the launcher's own
  shape; where an app draws its mark itself, it stays a disc (the walker on the outing's bar).
  Checked: the layers drawn in a round, a rounded-square and a squircle mask and as the themed
  icon; `:app:lintDebug`, `:app:assembleDebug`, `spotlessCheck` and `:core:tracking`'s tests.
- **The adaptive shortcut checked on the phone, and Tetto in the family** (owner, 9 Nov 2026:
  "I did every test on the phone and it is all fine. You can close every box."; then "Did you
  also align the documents with the reference to the family's new app, Tetto, in the various
  repositories?"). The device check of «The Outing shortcut, an adaptive icon» is done; its box
  in «Open» is ticked. Tetto (budget) is the family's fifth app: the README's «The family», this
  file's opening and «The family in the code», and VISION's opening name it beside Chiaro, Tempo
  and Saldo.
- **Each outing's shortcut wears its own mark** (owner, 9 Nov 2026: "the icon used for the
  «Japanese walk» item is not the one that appears in the app. In the app it had been changed on
  purpose to set it apart from the other walks"). Every shortcut wore the way to a flag, the
  Outing mark, while the Outings page draws an interval walk with its slow and fast line and a
  run with its bolt (`sessionIcon`, «the interval walk no longer wore the same mark as the brisk
  walk above it», the owner's review of 2 Nov 2026). `SessionShortcuts.shortcutIcon` now asks
  `sessionIcon` and maps its answer to three adaptive icons on the same ground,
  `ic_shortcut_outing`, `ic_shortcut_intervals` and `ic_shortcut_run`, each glyph placed as the
  Outing glyph is; one rule for the page and the shortcuts, so they cannot drift apart again
  (`SessionShortcutsTest`, every intensity with every goal). The launcher updates a shortcut's
  icon when the list is written again: when a plan is started, saved or deleted.
- **The outings' own marks checked on the phone** (owner, 9 Nov 2026: "I tried it and it works.
  Tick the box and merge straight into main."). The device check of «Each outing's shortcut
  wears its own mark» is done; its box in «Open» is ticked.

- **Today reviewed for a premium first screen** (owner, 10 Nov 2026: "Transform it into a premium
  Play Store app. A graphic review of the main screen. No noise, only added value."; then, on the
  design: "Fine, implement it in Compose", and, asked how to keep the chart whole on the first
  screen: "Values under the sentence"). What changed on Today, and why:
  - **No glow.** The top stands on the page's own ground; the ring is the one thing in the day's
    colour. The gradient tinted the whole top and greyed the text on it (`docs/adr/0004-design-language.md`,
    amended).
  - **A finer ring** (12dp on 256dp, growing with the text size as the guide's does), with a plain
    round cap: the dot of the ground at the arc's head read as a knob to turn, on every ring
    (`ProgressRing`, the guide's and Settings' too). The notch stays dashed, in the chart's ink.
  - **Inside the ring, the count and the goal only.** The distance and the calories went to a
    row of three under the sentence, **distance, active calories, active minutes**, with one line
    on what the estimates rest on; each amount is one node for TalkBack, read with its own
    meaning, so the shared line is hidden from it and the ring no longer reads the estimates.
    Three across while a third holds the text size, else stacked.
  - **The chart begins on the first screen** instead of being whole on it: on the owner's phone in
    Italian, under two sentences of two lines, its readout and the top of its plot show, the rest
    one short scroll away. This changes «Today's chart whole on the first screen» (archived in
    `docs/planning/02-decisions-to-1-nov-2026.md`); the test says the new rule.
  - **The chart without the grey block over the hours ahead**: the usual day goes on past now,
    fainter (`DayTrendChart`, Today's only user). The goal is named on its line («Goal 10,000»),
    so the key holds Today and Usual; the caption is one line shorter.
  - **The way to an outing is a row as wide as the cards**, with the line that says what one is
    («A walk with a goal, or intervals») and a chevron, where a centred pill read as left over.
  - **The four tiles and the cadence's became one card, «Intensity»**: brisk minutes on today's
    share of the WHO's 150 and the cadence on its ramp, each with its meaning. The distance, the
    calories and the active minutes, now under the sentence, are not said twice.
  - **The streak beside the date** («12 days», with the streak's mark; read as "12 days in a row
    at the goal"), from two days: Insights' streak, the same rule (`StreakSoFar`, pinned against
    `Insights` by a test). The days to yesterday are their own flow, distinct, since the past is
    frozen; today joins once its goal is met.
  The guide's paragraph on the ring and its sample ring say the same (no distance inside it).
  README screenshots regenerated (Today, the dark goal, the chart read, the outing, the foldable).
  Checked: `:core:domain` and `:feature:today` tests, `:feature:guide`'s, lint and spotless.
- **Today's three amounts, centred** (owner, 10 Nov 2026: "can distance, active calories and active
  minutes under the sentence be centred? Do you honestly agree, or is it better to leave them as
  they are?"; then "Ok, do it"). Each amount, its label and the line on the estimates are centred,
  in its third or stacked at a large text size: the ring and the sentence above stand on the
  page's middle, and the row left-aligned was the one block off that axis; three amounts read one
  at a time gain nothing from a left edge, which helps a column of numbers compared down. This
  changes «Today reviewed for a premium first screen», above. README screenshots regenerated.

- **The outings, their editor and the city walks' maps, reviewed** (owner, 11 Nov 2026: "review
  the design of the Outings and walks screens and their children, keeping to Material 3: what
  does not respect it, and what would make them better and more premium; the type too, watching
  for unwanted line breaks and for strings out of line; and the walks' maps. Keep it clean,
  avoid noise"; the review went to the owner as a canvas, today's screens beside the proposals;
  then "I want every point", with the start button as its icon alone). Built in three parts.
  - **The Outings page.** An outing's card reads on one line, the name's: the estimates and the
    signals started at the card's edge, under the mark. One way to start it, the icon at the
    card's end (a tonal icon button, 48dp, named for TalkBack: «Start Brisk walk»), and the card
    itself to edit: the «Edit» and «Start» row is gone, the card is a third shorter and the
    presets fit a screen. A new outing is the top bar's action; with no outing kept, the page
    still offers it where the list would be. The introduction is one sentence in the body's
    smaller size. The estimates are shown short with the sign Today uses («≈ 2,000 steps ·
    1.46 km») and how the signals arrive as marks before their line; TalkBack reads the whole
    sentences as before. The city walks' door shows a walk's map. Italian: «Segnali al 50%».
  - **The card of an outing under way** (`SessionCard`: Today, Outings, a walk's page). Its
    mark is the size of the cards' below it (40dp, it was 36dp: the names were 3dp out of line).
    The sentence is the headline, in the title's large size; on a city walk the place ahead,
    with how far it is under it («Royal Festival Hall», «The next place, 230 m ahead»: the
    notification and the voice keep «Next: …»). How far it is and where it ends stand at the two
    ends of its bar, not beside the name, which a long name («Japanese walking», «A walk in
    London») shared the line with. The pace's verdict is a word beside the state («Under way ·
    on pace»), and the numbers three columns between two hairlines (the pace, the steps, the
    distance with the calories beside the word «estimate»; paused or over, the steps and the two
    estimates); TalkBack reads them as the two sentences they were, and at a large text size
    those sentences are what is shown. Stop and Pause share the card's width at 48dp.
  - **The editor.** Its top bar takes the scrolled tone as the settings run under it, as the
    Outings page's does, and Save's bar wears the same: on the page's own ground a line cut by a
    bar read as a fault. «Goal, always», a label in the accent's colour with an icon, read as a
    text button that did nothing: gone, the sentence above the chips says it. One stepper
    (`StepButton`, the goal's tonal one) for the interval rows too. No check mark on any of the
    three segmented rows (two had it, one not). The interval rows are a line each: their notes
    broke in two in the column the steppers left them, and what a set is reads once under the
    group. The Japanese walk is said in two lines on the page's quiet ground, the study and how
    Passo walks it a touch away (it was twelve lines in the accent's container, the tone of a
    choice made and of a warning, and it pushed the sets off the first screen). The vibrations
    are a legend in the switch's own group: each signal with its pattern drawn beside it (drawn
    from `SessionHaptics`' own: one to three short pulses, a long one, two long ones, four taps,
    a long and a short), felt at a touch, said in words to TalkBack; it takes the place of the
    paragraph that listed the patterns and of the chips to try them. This changes how «Outings:
    vibrations in counts» and «The interval walk, built» (archive 02) put them on the page, not
    the signals themselves.
  - **A sentence's last word is not left alone on a line** (`unorphaned()`, `:core:designsystem`):
    its last two words are held together, a number with its unit too, up to 20 characters and
    not above 1.3 times the text size, where a pair held could be wider than the line. On the
    cards' descriptions and the editor's paragraphs («sforzo moderato», «with your step of
    73 cm»). The strings themselves are untouched: the voice says the same ones.
  - **The map** (`WayMapView`). The way still to walk was a grey at 40% over a city's white
    streets and was lost among them: it has more ink (72%), and on a page the whole way lies on
    an edge of the page's colour. The side streets are hairlines at part ink. A name goes on the
    side its own way leaves free (`placeLabels`, pure and tested: fewest names covered, then the
    least of the way and its stops), where beside its point it lay over the line (London, Rome,
    Paris). A city walk under way names the place being walked to, drawn larger in the goal's
    colour, not its two ends (this changes ADR 0014's decision 8 for a walk under way; a way
    keeps its ends' names, and the locator its rule). Before a start, where it begins is a full
    point and where it ends a ring: they were the same ring. The water is a tone deeper in light
    (`PassoColors.water`: a river through a city was almost the land's own blue-grey). A name's
    pill has a hairline around it. On a page a map has the groups' 24dp corner (16dp above
    24dp cards, at the same width), the continents' too; a city walk's map is never taller than
    wide (Rome's, at 1.2, pushed Start past the middle of the screen).
  - **The places** of a walk and the stages of a way: one line through their points, in the
    goal's colour as far as the reader has come, in place of a rule between every two; the stop
    ahead stands out.
  README screenshots regenerated (the outings, the editor, the intervals, the outing on Today,
  the walk, the ways). Checked: every module's tests, lint, spotless, the forbidden permissions.
  Not done on a device: the box is in «Open».

- **An outing's numbers centred, as Today's three amounts are** (owner, 11 Nov 2026, on the
  screenshots of the review above: "can the words in those boxes be centred, as on the main
  screen? Everywhere the app shows this kind of row, so they are the same across the app"). The
  app sets a few amounts in a row between two hairlines in two places, Today's three under the
  sentence and an outing's card (on Today, on the Outings page, on a walk's page): both are now
  drawn by one component, `StatStrip` (`:core:designsystem`), each amount centred on its share,
  with the same hairlines and the same room. Today's is unchanged to the pixel; the card's, left
  aligned when it was made earlier today, follows it. The values keep their two sizes (the
  reading's on the page, a step smaller under a card's headline). History's and Insights' tiles
  are another thing, a card each with its mark and its meaning, and stay as they are. README
  screenshots regenerated (the outing on Today, the intervals over, the walk).

- **History reviewed, after Today and the Outings** (owner, 11 Nov 2026: "review the design of
  History and its child screens, the ones the earlier review did not touch; Material 3, what is
  wrong today and what would make it more premium; the type, the line breaks nobody wanted and
  the alignments; a clean layout, in step with what is already reviewed, no noise", then, on the
  review drawn beside the pages as they were, "apply everything": the eight faults and the three
  things that asked for a yes). The four pages were drawn in Italian at 360dp, where the faults
  were, and read again after.
  - **The scales.** «Settimana» read «Settim…» in its quarter of the row: the segments keep 4dp
    a side for their word, not Material's 12dp, and a label still too long for its room goes a
    step smaller (12sp from 14sp at most) before it is cut, at the standard text size too
    (`FittedLabel`, which `SegmentLabel` now is; at 320dp the four words are whole).
  - **A bar's reading** is two lines that stay two: the day and its steps, and under them what
    came of it («Goal met» with the goal's mark and colour, «1,677 short of the goal»). On one
    line it ran to three in Italian, «10.536» parted from its «passi», and the chart moved
    under the finger. The way in is one word at the line's end, «Open» (a screen reader still
    says «Open day», «Open month»), and the readout keeps a button's height (48dp) whether or
    not the button is there: it was 40dp without it and 48dp with it. A day with no bar touched
    reads its busiest hour the same way, the hour and its steps, «The busiest hour» under them.
  - **Said once.** The days' chart told the reader to touch a bar twice (the readout and the
    line under the plot) and what its line is three times (the key, the number at its end, the
    line under the plot): the line under the plot is gone for a week and a month. A year keeps
    «Each bar is the month's average day», a day its shaded walks and the touch (the walks named
    only on a day that has them), the calendar «Touch a day to open it».
  - **No word alone.** A page's sentence and the line under it, the captions and a tile's
    meaning go through `unorphaned()`, and the sentence's numbers are held to their words in
    the strings («su 4, finora», «20% in più», "of 24"): «…5 giorni su / 7» was a headline.
  - **The tiles.** A label is one line, a step smaller where half a phone is short for it
    (`MetricTile`'s `fitLabel`; "Active calories" broke at 360dp, and a label on two lines set
    its value a line lower than its neighbour's); a day's brisk minutes are «Passo svelto» in
    Italian beside the mark, the whole name still read aloud. The meanings are short («Estimate,
    from your step», «Estimate, movement only», «With 40 steps or more», «From 100 steps a
    minute»): one line or two balanced ones, where they ran from one to three. The mark is
    quiet, the highest container with the variant ink: the accent's container is the tone of
    the scale chosen above, and five marks wore it. This changes one thing in «An outing's
    numbers centred» above, which left History's and Insights' tiles as they were: the tile is
    the same card, its mark another tone, in Insights and the guide too (`MetricTile` is theirs).
  - **Walks and outings in a day's list** (`OutingList`, so on Today as well). The four figures
    on one line broke where the width ran out, «108 passi/» from its «min»: they are two lines
    that never change places, what was counted (the steps, the cadence) and what is estimated
    from it (the distance, the calories) after the «≈» Today uses, each figure held together.
    What came of an outing has a line of its own under its name: beside it, «Obiettivo
    raggiunto» broke in two. TalkBack reads the sentences it read.
  - **Ranges.** A week inside one month says the month once, on the side the locale writes it
    («21 – 27 set», "Sep 21 – 27"; `dayRange`), and the dash stays with the first date. An hour
    is written as the walks under it are, «12:00–13:00» (`clockRange`): the page had both. The
    range has a place to break after its dash that shows nothing: at twice the text size
    «12:35 PM–12:55 PM» broke inside its last «PM».
  - **The period's name** keeps the room of its two lines when it has one, so at a large text
    size the page does not move up and down as the weeks are paged (at the standard size the
    arrows' 48dp already held it: the review said otherwise, wrongly).
  - **The year's pages** are a tonal button at the headline's edge, with the same words: a text
    link between the sentence and the chart read as a caption.
  The goal calendar keeps its ticks on the days at the goal (the review's drawing had left them
  out): they say it without the colour. Tests: a bar's two lines and its «Open», a week in one
  month, the four scales uncut in Italian. README screenshots regenerated (History's day and
  month; no other picture shows a tile or a day's list). Checked: every module's tests, lint,
  spotless, the forbidden permissions. Not done on a device: the box is in «Open». Left as they
  are, outside History: Insights' and the year's own ranges still say the month twice («Sep 7 –
  Sep 13»).

- **Insights and Your year on foot reviewed, after Today, the Outings and History** (owner,
  11 Nov 2026: "review the design of Insights and its child screens, such as Your year on foot,
  the ones the earlier reviews did not touch; Material 3, what is wrong today and what would make
  it more premium; the type, the line breaks nobody wanted and the alignments; a clean layout, in
  step with what is already reviewed, no noise", then, on the review drawn beside the pages as
  they were, "apply everything": Insights' twelve faults and its three things that asked for a
  yes, the year's seven, the Ways' two). The pages were drawn in Italian at 360dp and at 320dp,
  where the faults were, and read again after.
  - **Insights, the streak.** The card stands on the groups' own ground: it still had the
    gradient Today lost on 10 Nov, the goal's colour is the marks'. With no streak it is a title
    and its line («No streak right now», «A day at your goal starts one.»), as the running one
    is: one sentence ran to three lines beside the mark, «una.» alone on the last. In the seven
    days, a day before the first one counted is a point: it was a third empty circle, a hair
    thinner than a missed day's and today's.
  - **Insights, the way under way.** The map (72dp), the way, where the reader stands and the
    stage ahead, with the chevron every other door on the page has; under them the bar the card's
    width, how far the reader has come and how far the end is at its two ends, as an outing's
    card sets them (`wayToEnd`, new beside `wayNextStage`). It was four small lines in 192dp,
    «tra / 14,7 km» broken, the bar under the words only.
  - **Insights, the records.** No mark before a record: three trophies alike said nothing, in
    the titles' colour, and without them every group's words stand on one edge. The amount takes
    the full ink, the date stays quiet. A week or a streak inside one month says the month once
    («7 – 13 set», `dayRange`, History's), each date whole on its line; the month of the best
    month is in the language's own case («agosto 2026» under «dom 13 set»).
  - **Insights, the averages** are Today's strip (`StatStrip`): three amounts side by side
    between two hairlines, «steps a day» said once in the group's title, the last seven days
    against the seven before in a sentence under them («…dei 7 / giorni prima» was a line
    break). An average with too few days yet is a dash, and one line says when it comes. The
    page is about 120dp shorter.
  - **Insights, the years.** A year is a row of one line; what a year's pages are is said once,
    under the group (every row said it, «i / mesi» at a line's end). The group's rule is the
    other groups'.
  - **Insights, the totals.** The title says what the group is («In all, since Jul 26, 2026»):
    «Since Jul 26, 2026» was a date, and the averages' third label besides. The tiles keep their
    label on one line (`fitLabel`, as History's): «Days at your goal» is «At your goal» beside
    the mark, whole for a screen reader, where on two lines it set its value a line lower than
    its neighbour's; «days» is small as «km» and «kcal» are; the meanings are short, in History's
    words («Estimate, movement only», «Estimate, from each day's step»).
  - **Insights, the door to the Ways** is a title and two or three lines (it was seven, in a
    list's row), its mark in the row icons' quiet ink. The page's sentence, the line under it and
    the season card's body go through `unorphaned()` («…per farne / 5.» at 320dp).
  - **Your year on foot.** Whose year it is stands under the marks at one height on every page,
    and the rest is centred in the room under it: the eyebrow moved between 105 and 340dp from
    page to page, and the title jumped at each touch. A page's number is one line, a size
    smaller where a narrow phone has no room for it («3.065.44 / 0» at 320dp). How the pages turn
    is said at the foot of the first page, outside what is drawn into the image: it was inside
    the page, so in the picture shared or saved. The sentence and the lines under it break as a
    title does (`LineBreak.Heading`), a way's name and a stretch of days are not parted, a fact
    has a line of its own where a «·» joined two, and the rhythm's two sentences are a line each.
    The outings' and the ways' pages give their number large, as every page with one does («Out
    with a goal», 25, «outings»; «Along the ways», 1,188, «km», then each way by name with its
    amounts under it): they had it in the title, at the title's size. The year's strings have the
    curved apostrophe the rest of the app has; a weekday after a colon is in the language's own
    case; a year begun late says «· since Oct 1», the year once. A page saved says so in a
    snackbar: it was the app's one toast.
  - **The Ways.** A route's last word is not left alone («…a / Roma»); a way kept says its year
    once, with the day it ended («Walked from Sep 1 to Oct 7, 2025»), and its mark is in the row
    icons' quiet ink. «…Santiago, da / Saint-Jean-Pied-de-Port» stays: in the row's 176dp the
    name does not fit beside its preposition.
  This changes how «Phase 5» and «Phase 12» (archive 01) and «Your year on foot» (archive 03) put
  these pages on the screen, not what they count. Tests: a week in one month and the averages
  side by side, the card with no streak, Insights in Italian at 360dp, the hint at the foot of
  the first page only, the outings' and the ways' numbers, the year in Italian at 320dp. README
  screenshots regenerated (Insights, Your year on foot). Checked: every module's tests, lint,
  spotless, the forbidden permissions. Not done on a device: the box is in «Open».

- **12 Nov 2026.** **The first run's lines balanced, in the four apps** (owner, after the review
  of Tempo's, Tetto's and Saldo's first runs against this one: "if a word alone at the end of a
  line should be fixed in Passo too, we do it in all four repos"). The first run's texts are
  centred and short, where a word alone on the last line («…in the / background.») reads as a
  slip; the budget app already broke them balanced. `:core:designsystem` gains `balanced()`
  (`LineBreak.Heading`: lines of about one width, never a word alone on the last), and every
  centred text of the first run and of the no-sensor screen takes it: the titles, the sentence,
  the promises, the bodies, the goal's note, the permission's refusal. Only the first run: the
  rest of the app keeps its breaks until a review of its own looks at it screen by screen.
  `onboarding-goal.png` regenerated (the welcome's breaks did not change). Checked: the
  onboarding's tests and their screenshots, spotless.

- **14 Nov 2026.** **The widgets swap names** (owner: "I'd like to swap the two widgets' names, so
  the «In parole» widget becomes the same for the whole family"). The ring card (`GlanceWidget`)
  is now «In words» / «In parole», as Tempo's card with the dial is; the card in type alone
  (`WordsWidget`) is «At a glance» / «Colpo d'occhio». Only the names move: each card keeps its
  forms, its description in the launcher's list and its settings. Changed: the two labels
  (`widget_glance_label`, `widget_words_label`, English and Italian), the guide's «Two widgets»,
  the README's caption, VISION, CLAUDE.md, §7, ADR 0005 (amended), the CHANGELOG's Unreleased, and
  the comments and test names that name a card. **The code keeps its names** (`GlanceWidget`,
  `WordsWidget`, `WidgetKind.GLANCE` / `WORDS`, `widget_glance_*`, the `glance/` and `words/`
  packages), now the other way round from the cards': the launcher binds a placed card to its
  receiver's class name, so renaming `GlanceWidgetReceiver` / `WordsWidgetReceiver` would take
  every card off the readers' home screens, and renaming only the rest would leave the code half
  one way and half the other; `WidgetKind`'s comment says so. The README's `widgets.png` draws no
  name, so it was not regenerated. The log above keeps the names of its time.


- **15 Nov 2026.** **The widgets' names swap back, the cards wear green, a seventh colour**
  (owner, for the whole family: in Passo the two names looked swapped; every «A colpo
  d'occhio» first in the list and 4×1; each app's widgets in its own colour; a seventh colour
  that stands apart). The rule is now one for the four apps: «At a
  glance» / «A colpo d'occhio» is the card with a picture (here the ring), «In words» / «In
  parole» the card in type alone, as on the weather app. So 14 Nov's swap is reverted in the
  code, the labels, the guide, the README's alt text, VISION and CLAUDE.md, and the code's names
  agree with the cards' again (`WidgetKind`'s note on the older names went with it); the entry
  of 14 Nov above keeps its names. The Italian label is «A colpo d'occhio» in the whole family:
  the launcher's list sorts an app's widgets by name, and «A colpo d'occhio» / «At a glance»
  sorts first in both languages here; it is also declared first, and opens at 4×1 as before.
  - **A new card wears the app's green** (`WidgetLook.cardColor`, owner: each app's widgets in
    its own colour), and so does a card placed before and never configured, whose colour was the
    default rather than a choice. The picker's static previews follow (`widget_preview_card`,
    and `WidgetProviderTest` now reads it from the default look rather than a hex).
  - **The ochre** (`#5C4A0E`), the family's seventh card colour, after the owner asked for one
    that stands apart from the six in their style and is no shade of violet: the hue the six
    left out, 30 ΔE from its nearest (the terracotta), white ink 8.6:1, the 75% ink 5.7:1, the
    85% one 6.8:1 (measured in the weather app's DESIGN §2.6, the family's table). Seven is where
    it stops (the owner asked; a bright series would need a dark ink measured on every mark, and
    a strip that scrolls hides colours).
  - **The strip of colours starts under the radio buttons** (10 dp, the first disc on the
    buttons' line), not under their labels: seven 48 dp targets want 336 dp: in the settings group (16 dp margins), less the strip's
    own 12, a 384 dp phone leaves 340 and holds them in one row; a 360 dp phone leaves 316, and
    `swatchesPerRow` breaks them into four over three, never one alone (with six, the strip under
    the labels already broke there, five over one). Tested at 384 dp (one row) and in arithmetic.
  - Checked: the widget module's tests, spotless; `widgets.png` and `widget-settings.png`
    regenerated (README).

- **15 Nov 2026.** **The shortcuts written again at every start of the service** (owner: "the
  shortcuts all wear the same drawing for the different kinds of walk, but in the app each walk
  has a different one, at least the Japanese walk"). The rule has matched the Outings page since
  9 Nov 2026 (`SessionShortcuts.shortcutIcon` asks `sessionIcon`; `SessionShortcutsTest`): the
  interval walk its slow and fast line, a run its bolt, every other walk the way to a flag, so
  the brisk walk, the stroll and the reader's own walks share the flag, on the page and on the
  launcher alike. What could still show the same mark everywhere is the launcher's copy of the
  list: it keeps the shortcuts, icons included, as they were last written, and they were written
  only when a plan was started, saved or deleted, so a list written by a version before 9 Nov
  2026 outlived it on the phone. `StepTrackingService` now writes the list again at the end of
  each start (an update, through `MY_PACKAGE_REPLACED`; a reboot; the app reopened after the
  process died): one read of the plans and one write, no wake-up of its own, the launcher's rate
  limit caught as before. Checked: `:core:tracking`'s tests, spotless. On the phone: the box in
  «Open».
  - **Four shortcuts, not three** (owner, the same day, on a tablet: the marks were right, a run
    with its bolt, but the Japanese walk was not there). The long press offered three outings,
    the most recently started first, then the Outings page's order: on a phone where none was
    started yet, the first three presets, and the interval walk is the fourth.
    `SessionConstants.SHORTCUTS` is 4: a first visit finds all four presets, and four is as many
    as the common launchers show. No reason for three was written anywhere.

- **18 Nov 2026.** **Location, for an outing that asks for it** (owner: "at the moment GPS is
  not planned, the rules forbid it; forgetting that rule, analyse how GPS could be integrated,
  turned on by the reader and leaving the app usable as it is now without it"; then, on the
  analysis: "I approve the direction completely and everything you proposed"). **This changes
  an archived decision:** «Route maps with GPS stay out» (1 Oct 2026,
  `docs/planning/02-decisions-to-1-nov-2026.md`, line 120). Its four reasons are answered one
  by one in `docs/adr/0016-location.md`, which is the record of this decision; in short:
  - **What it is.** Location as a measuring tape, never a tracker: read only inside an outing
    the reader started with «Measure with GPS» on. The day's count, the widgets and every
    screen are as before for a reader who never turns it on, who is asked for nothing.
    `INTERNET` and `ACCESS_BACKGROUND_LOCATION` stay forbidden, and the build's gate becomes an
    allowlist, stricter than today's. Satellites only, through the platform's
    `LocationManager`: no Google Play services, no new dependency.
  - **What sets it apart.** The steps and the fixes are fused (no pause guessed from a speed,
    no distance while still or on a bus, no metre lost in a tunnel, the receiver asleep when
    the steps stop); every outing with GPS teaches the step counter the reader's step, so days
    without GPS read truer; the city walks and the ways can be walked on the spot, with the
    content Passo already has; a round the reader repeats is compared with their usual. And a
    recorder of routes that holds no `INTERNET` cannot send one.
  - **The owner's three decisions** (each as recommended):
    1. *The day's distance stays the steps'.* Only the outing shows a measure. A day mixed of
       measured and estimated stretches would break frozen days, the import's merge and "Apply
       profile to past data"; once the step has learnt, the two agree within a few percent.
    2. *Four hours stays the longest outing, but for two kinds, which get twelve.* The cap
       counts from the start by the clock, stops included, and ends the outing without a word
       (`SessionEnd.CLOSED` is neither told nor reopenable). Beside the end after 15 minutes
       still, it is what stops an outing forgotten by a reader who keeps walking all day; with
       GPS on, that forgetting would cost a battery, so the cap matters more, not less. But a
       day's stage on a way is six to eight hours with its stops: the cap would fall in the
       middle of it and end the guidance silently, on a path. So an outing on a way walked on
       the spot (Phase 19) and one that follows a route (Phase 20) start with twelve hours.
       Every other outing, with GPS or not, starts with four.
       *And no cap ends a walk unasked* (owner, the same day, on reading the above: "before
       the GPS is turned off, is the reader warned, and can they say to go on, if they walk
       more than four hours?"). They were not: the plan only told the end after it. Now, for
       every outing: 15 minutes before its cap it asks, by a vibration and a notification with
       «Keep going», which moves the cap four hours on; unanswered, it ends at the cap, says
       so, and can be taken back for 15 minutes, the receiver staying on meanwhile so the
       route has no hole. The question rides on a step batch, so no timer is added and a
       reader who is sitting is not asked. Built in Phase 15, since it is every outing's;
       ADR 0016 decision 14, which amends ADR 0009 decision 6.
    3. *One APK.* A second build without the permission would double every release's tests and
       ask every new reader to choose before knowing the app; the allowlist, the permission
       asked only in context and Android's own pages do that job.
  - **Six phases, 15 to 20** (§11), each written to be built in a session of its own: the
    measured outing; the outing's page (route, splits, climb, GPX); the step that learns; on
    the spot, the cities; on the spot, the ways; your routes. 15 and 16 are released together
    or not at all. The number 15 is free: the «In pictures» plan that once had it was written
    and reverted before anything was built (pull requests 68 and 69), and is in no archive.
  - **Nothing is built, and nothing in force has changed.** Written today: ADR 0016, the phases,
    this entry, the «Planned» notes in «State», §9, §10, VISION.md and CLAUDE.md. The manifest,
    the gate, the invariants and the README change in the change that builds Phase 15, as ADR
    0013 amended §9.7 in the change that built Phase 13.
  - **«Sul posto» is "On the spot"** in English (owner, the same day).
  - **Left for the owner to confirm on the way** (owner: "ask me when it is needed, and give me
    suggestions anyway"): the two new vibrations (Phase 18, in "Try them") and the cost line
    beside the switch, once measured (Phase 15). The session that reaches one asks the owner
    then, with its proposal and an alternative or two, and builds with the proposal meanwhile
    so the work does not wait.
  - **To prove on the phone before building on it** (Phase 15's first device box): that a
    running `health` service takes the `location` type by a second `startForeground()` from a
    page and from the notification's Resume. The documentation says a type is added that way
    and does not say how the second call is checked; ADR 0016 decision 4 has the fallback.

- **18 Nov 2026.** **Phase 15 read against the code before it is built** (owner: "check whether
  the phase has gaps, or whether you have suggestions, and list them apart, before the plan";
  then: "OK for the gaps, write them in the phase"). Nothing built. Written into §11 Phase 15,
  each where it belongs, none changing a decision of ADR 0016:
  - **`onStartCommand` would have taken the `location` type away.** It calls
    `startForeground()` with `health` alone at every opening of the app (`MainActivity.onResume`
    starts the service each time) and at every command; and its catch stops the service on a
    refused type. The one function that knows the types answers there too, and a refused
    `location` falls back to `health`: the count never stops for it.
  - **The cap was said to be stored, and was in no list.** `Session.capAtMillis` and
    `capAskedForMillis`, in the model and in schema v7; the wake lock's own bound follows the
    cap and is taken again at «Keep going» (§9.7's "4 h 10 min" is reworded in Phase 15's
    change); an end that can be taken back is the tracker's knowledge, not `SessionEnd.CLOSED`.
  - **One clock.** A fix is laid by its elapsed time, as a step sample is, never by the
    satellites' clock.
  - **Steps ahead of their fixes.** An open stretch is shown from the steps, and the goal, the
    milestones and the places are told on the settled distance only. The step batch asks for
    the fixes in its own wake (`requestFlush`), and the receiver's own delay is 60 s, not 30:
    one rhythm of wakes for the two streams.
  - **The permission.** «Only this time» is a grant, to be proven on the phone under a service
    that never stops; a revoked permission ends the process, so that row is the restarted
    process with the permission gone. The check, Passo's screen and the request are behind one
    door in `:app`: the outings start from three modules.
  - **Two databases share no transaction.** The route is deleted first, the outing after; the
    service deletes a route without an outing when it starts; a restore from the cloud copy
    has measured outings with no route.
  - **The samples travel with the reader's own file** (`step_length_sample`, an added field of
    the outing): neither Phase 15 nor 17 carried them.
  - **The allowlist is exact both ways**, as the ADR says; a permission named after the
    application id is resolved per variant; the task keeps its name.
  - **The proof on the phone comes first**, on a thin slice, before the fusion, the storage and
    the screens are built on the answer.
  - Smaller: the presets are four, not three; only the points kept get their altitude
    converted; the battery's record says which phone and whether it held the wake lock.
  - **Waiting for the owner, since each changes a decision or a rule** (asked the same day, to
    be judged on what is best, the ADRs and CLAUDE.md to follow): what wakes the receiver on a
    phone with no wake-up counter in an outing without signals; how GPS comes back after a
    restarted process; the city walk from home with GPS on; the receiver while an ended outing
    can still be taken back; "Walk now" on a plan with GPS on.

- **18 Nov 2026.** **The five answered, and the platform read again** (owner, on the five left
  waiting: "re-evaluate them without regard to the ADRs or CLAUDE.md: tell me what is best and
  premium, with respect for the battery too, and we change the ADRs and CLAUDE.md to follow";
  then, on the answers: "I confirm all five, write the amendments and go on"). Written as
  amendments in `docs/adr/0016-location.md`, each under its decision with its reason, and in
  §11 Phase 15; CLAUDE.md, §9.7 and ADR 0013 change in the change that builds Phase 15, as
  every rule in force does, and until then stand as written.
  1. **A phone with no wake-up step counter: an outing with GPS holds §9.7's wake lock**, with
     signals or without (ADR 0016 decision 5). The first proposal, the receiver slowed to a fix
     every half minute during a stop, had been made only to leave §9.7 as it was, and is the
     worse on both counts: more battery than a processor awake with the receiver off, and the
     first step found up to half a minute late. The lock is already built, measured and let go
     after 15 minutes still. It widens an exception CLAUDE.md says never to widen: the owner's
     decision, and the invariant's words change with Phase 15.
  2. **GPS comes back by itself, and its stop is told** (decision 4): told once by a
     notification and the voice, «Resume GPS» on the notification and the card, and with no
     touch when the app is brought forward.
  3. **A city walk from home never reads the satellites** (decision 13, made plain): two ways
     to walk one, each with one meaning. The edge case that had it «with GPS on» contradicted
     the same phase's own sentence, "the ways from home still use no location". Not a matter
     of privacy: an outing of the reader's own, started at their door, is measured from its
     first step.
  4. **The receiver stays while an ended outing can be taken back**, after the goal and the
     stillness as after the cap (decisions 10 and 14): at most 15 minutes, asleep 2 minutes
     after the reader stops, off when the card is put away. The alternative, off at the goal
     and the stretch laid from the steps, costs nothing and can take the word «measured» from
     an outing taken back.
  5. **"Walk now" keeps the plan's word** (decision 4): with GPS when the plan has it on and
     the permission is there, on the steps and said once otherwise.
  - **No «Trim the ends»** (owner, the same day: hiding where a walk starts and ends "seems a
    little extreme; if you agree, we avoid it, and the app is cleaner and clearer"). Agreed:
    Passo has no feed, a share is one act towards someone the reader chose, the image is a
    line on a ground with no streets, and the GPX's choice says what the file holds. §11 Phase
    16 loses the switch, its remembered setting and its edge case; ADR 0016 decision 9.
  - **The platform, read again** (Phase 15's first box, ticked): Android 17's change lists
    say nothing on location, the `location` service type or the while-in-use rule. The
    "Location Button" is real and is not in those lists but on the permissions page: an
    experimental Jetpack library (`androidx.core.locationbutton`, 1.0.0-alpha01, 17 Jun 2026)
    with its own permission, for momentary access; Google Play asks for it of apps that
    target API 37 and have only session-based needs. Passo measures a walk for its whole
    length with the screen off: the classic request stays, no dependency is added, and Phase
    9 reads the policy itself before a listing. The one-time grant gives the same permissions
    as «While using the app»; how long it lasts under a service that never stops is written
    nowhere, and is in the proof on the phone.

- **18 Nov 2026.** **Phase 15, the slice for the proof on the phone** (owner: "write the
  amendments and go on"). On the branch `feature/phase-15-measured-outing`; built so that the
  first device box can be answered before the fusion, the storage of routes and the screens are
  built on it (§11 Phase 15, «This box comes before the rest is built»).
  - **The gate is an allowlist** (box ticked). `AllowedPermissions` in `build-logic`: the ten
    names a green build's report held, plus the three of location; exact both ways; the one
    named after the application id resolved per variant; `permissionVerdict` pure, with nine
    tests, which `./gradlew test` now runs too (the root's `test` reaches the included build).
    Proven as in Phase 0: `CAMERA` and `ACCESS_BACKGROUND_LOCATION` added to `:core:tracking`'s
    manifest turned the build red, the first as not on the list and the second as forbidden,
    each by name; then removed.
  - **The manifest** (box ticked): the three permissions, `health|location`, the `gps` feature
    not required, and its opening comment rewritten.
  - **Schema v7, whole.** Every column of the phase is in it now (`gps` on a plan and an
    outing, `stepsMeters`, `measuredShare`, `capAtMillis`, `capAskedForMillis`,
    `step_length_sample`), not only the plan's `gps` the slice needs: a debug build installed
    for the proof must not meet a second, different v7 at the next build. An outing gets its
    cap when it starts; what reads it is the tracker's question, not built yet.
  - **The receiver and the type.** `LocationSource` (the platform's `LocationManager`,
    `GPS_PROVIDER`, 1 s, batched 60 s with the screen off, flushed by each step batch and at
    the screen coming on); `GpsDuty` (pure: awake while the reader walks, asleep after 2
    minutes, off at a pause and the end, and steered as while it counted for as long as an end
    can be taken back); one function, `enterForeground`, that every `startForeground()` goes
    through, `onStartCommand`'s included, so a look at the app no longer takes the type away,
    and a refused `location` falls back to `health` without stopping the count. The type is
    asked for only from an outing's own start, resume and «Keep going», and from the activity's
    own start of the service (`StepTracking.start(fromApp = true)`); never from a restart by
    the system or a broadcast. `SignalWake` holds the lock for an outing with GPS on a phone
    with no wake-up counter, while the service holds the type.
  - **The log's row** (`DiagnosticsType.GPS`, `GpsLog`): fixes, the good ones, the ones that
    came with the screen off, batches and the largest, the longest gap inside one stretch, the
    first fix's delay, sleeps, the type added and refused and what refused it. Counts and
    delays only. Until the meter is built the fixes are counted and let go: no route is kept
    by this build, and an outing with GPS still shows the steps' distance.
  - **The switch and the door.** The editor's «Measure with GPS» with what it gives and what
    it costs, absent with no receiver, the estimate's note and the wake lock's note true with
    it on; `OutingStarter`, the one door the Outings page and the launcher's shortcut start
    through; `GpsStart` (pure) decides: Passo's own screen and then the system's request the
    first time, the request alone after a grant the system took back, nothing after a refusal;
    `GpsIntroScreen`, its page, and the answer remembered beside the settings.
  - **Not in this slice**, and so not for the proof: the stop told by a notification and
    «Resume GPS» as a button (the receiver does come back when the app is opened), the card's
    lines for each state of the receiver, the cap's question, `RouteMeter` and the routes'
    database, the backup's added fields, the documents of the «Amends» list. CLAUDE.md, §9,
    §10, VISION and the README still say what was true before this branch: they change before
    it is merged, in this same change.
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions` (13 permissions, both variants), `test` in every module and in `build-logic`, `:app:lintDebug`, `:app:assembleDebug`: green, but for the six tests of the next line. The new pages' screenshots looked at (the intro in the light, the dark, Italian, twice the text size and on the open foldable; the editor with the switch). **Not checked: anything on a phone.** No fix has been received by this code yet: that is the proof's first answer.
  - **A machine's limit, not a fault:** on the owner's Windows the six tests that walk a
    migration (`MigrationTestHelper`) fail before reaching Room, on a comparison of paths
    («This driver is configured to open a database named…»), on an untouched checkout as
    here. Schema 7 was generated and `UpgradeFromFirstReleaseTest` walks to it: CI, on Linux,
    is where it runs.

- **18 Nov 2026.** **Phase 15, the domain: the fusion, the route's bytes, the cap's question**
  (owner, asked whether Phase 16 could begin while the phone cannot be tried: "can you go on
  safely, and in keeping with the plan?"). Not Phase 16 yet: it draws and shares routes that
  come from the part of 15 not built. But what the proof on the phone decides is one thing
  only, how the service comes by its fixes, and ADR 0016 decision 4 says the rest is the same
  either way. So Phase 15 goes on from its pure domain, which a phone adds nothing to, and
  Phase 16 follows it. **A departure from the order written the same morning** («This box
  comes before the rest is built»), made at the owner's word and reworded in the phase: the
  proof still comes before any release, and before the fallback is or is not built.
  - **CI is green on the pull request** (#72, both runs): the migration to schema 7 passes
    where it can run.
  - **`RouteMeter`** (pure, 26 tests, one a rule and one a row). The steps say when the reader
    walks, the fixes how far. A batch of steps is laid over its own time in motion; a fix
    outside it moves the line's end and adds nothing (the bench, the tram); a fix inside it
    moves the line by a grain. Open until the fixes are known to be in through its end
    (`settle`), so nothing is told on metres that could be taken back. Decided on the way,
    each with its reason in `RouteConstants` and each to be validated on a device:
    - *The grain grows where the receiver is less sure* (half the fix's own accuracy, three
      metres at the least): the wander between tall buildings stays under it.
    - *After a ride the receiver is believed again once five fixes in a row agree.* Judged
      only against the last fix accepted, every good fix after a bus read as too fast from
      the stop it left, for minutes.
    - *No hop beyond what the steps since the line's end could reach*, and no batch beyond
      what its steps could cover (2.5 m a step, the longest a profile may hold): a reflection
      that lasts, a tram, walking down a moving tram's aisle.
    - *Steps that go nowhere are measured as what they are*: marching on the spot under the
      satellites is no distance, where the steps alone said 165 m.
    - *A point says how it is joined to the one before* (`RoutePointKind`): measured, laid
      from the steps (a hole: dashed, later), or the start of a stretch (the first point, a
      pause, the far side of a ride): a route never draws a line across a place not walked.
      The phase's model had a fix or a laid point only; the third is what a pause in a GPX
      and a tram on a map both need.
  - **`RouteFilter`, `RouteCodec`, `Pace`**, and **`StepLengthSample`** from the meter's clean
    runs. The codec: a point in about six bytes, an hour in about ten kilobytes, a chunk that
    stands by itself and never throws on damaged bytes.
  - **The tracker** takes a meter for an outing with GPS: steps, time in motion and the
    intervals are told with the steps as ever; the distance, and the energy priced on it,
    when it is settled. What is walked after an end that can be taken back waits aside, and
    is the outing's again at «Keep going» (the route with no hole), or nobody's.
  - **The longest outing asks before it ends**, for every outing: `Session.capAtMillis` set at
    the start and read by the tracker, `SessionSignal.CapAhead` at the first steps within 15
    minutes of it, `goOn` moving it four hours, the end at the cap told and taken back as the
    stillness's is. In the service: the notification «Still walking?» with «Keep going» (an
    id of its own), the stillness's two long pulses, the voice; the wake lock's bound follows
    the cap and is renewed. Posted even where the outings' signals are silenced: it is a
    question, and an outing ended unasked is the fault it mends. **Not yet:** the card saying
    it too.
  - **The model's box ticked.** The domain's is not: the pace's words by the kilometre or the
    mile come with the card that shows it.
  - **Still to build in Phase 15:** the meter wired into the service (the flush that settles,
    the fixes on the steps' clock, the points written), `routes.db`, the backup's fields, the
    stop told with «Resume GPS», the card and the notification's lines, the documents.
  - **The routes' database, the same day.** `routes.db` (`RouteDatabase`, schema 1 exported):
    `route` (the outing's id and start, the count, the box) and `route_chunk` (outing, order,
    bytes); `RouteRepository` appends a chunk, reads a route back whole, deletes one. A route
    is its outing's by the outing's start, not by the id alone. `SessionRepository.delete`
    deletes the route first, then the outing with its intervals and its samples;
    `deleteOrphanRoutes` deletes every route whose outing is gone (for the service's start);
    the samples are written and read. `data_extraction_rules.xml` names `routes.db` and its
    write-ahead log under `<device-transfer>` only, and a test reads the file to say so. 13
    tests on real Room databases. **Not yet:** the service calling any of it, and the
    backup's added fields in the JSON and the CSV.
  - **The meter in the service, the same day.** Each fix goes to the tracker on the steps' own
    clock (`Location.toFix`: its elapsed time, by the snapshot a step sample is read with;
    never the satellites' time). After a batch of steps the receiver is asked for its fixes in
    that wake, and the distance settles when they are in (`settleSoon`; at once where the
    receiver holds nothing back; the wait gives up after the sensor's own 1.5 s, the processor
    being awake for the batch). Before a pause, a stop or a resume the fixes up to the touch
    are waited for, as the steps are. The points kept are written as a chunk after the step
    batch's transaction, their altitude turned to mean sea level off the main thread
    (`RouteAltitudes`: a point that cannot be converted loses its altitude rather than keep one
    on another datum); the samples go to `passo.db`. An outing too short to keep has its route
    deleted first. At the service's start the routes with no outing are deleted, only where
    the routes' file exists. **The stop is told**: a restarted process posts «GPS stopped»
    with «Resume GPS» (or "Open Passo" where the permission is gone), says it if the outing
    speaks, and the notification goes when the receiver is back. A mock fix is taken only by a
    debuggable build. The log's row gains what the route was built from and the measured
    share. From here an outing with GPS shows its measured distance and keeps its route.
    **Not yet:** the card's and the notification's lines for the receiver's states, the pace
    in words, the backup's fields, the documents.
  - **The backup's added fields, the same day** (box ticked). The JSON: a plan's `gps`; an
    outing's `gps`, `stepsMeters`, `measuredShare` and its `stepLengthSamples`, added fields
    with no new version, read back exactly, absent from an outing without GPS, and dropped
    where they cannot be a measure (a share is kept between nothing and all, a sample with no
    steps says nothing). No place is in the file. The import brings the samples of the outings
    it adds, under this phone's ids, and none twice. The same walk with GPS and without are
    two plans (`BackupMerge`). The CSV's outings gain three columns at the end (`gps`, the
    steps' own distance in the reader's unit, the measured percent), empty for any other
    outing, so an older sheet's columns stay where they were.
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions`, `test` (the domain's 469
    among them), `:app:lintDebug`, `:app:assembleDebug`: green, but for the six migration
    tests this machine cannot run. Nothing on a phone.

- **18 Nov 2026.** **The rules in force brought in line with what is built** (owner: the pull
  request is to be merged into `main`, and the work to go on in a new session). The manifest
  on the branch asks for location while CLAUDE.md, §9 and §10 still said "never": merged so,
  `main` would have held rules its own code contradicts, and the next session would have read
  them as its instructions. Changed in this same change, as ADR 0016 asks: CLAUDE.md (the
  allowlist; location as a measuring tape, with its bounds; the day stays the steps'; the
  battery's two exceptions, the wake lock for an outing with signals or with GPS; `routes.db`
  in the backup's rule; the modules), §9 (rule 9, and rule 7's amendment), §10 (the three
  rows, the forbidden names, the allowlist), «State».
  - **Still to change, of the «Amends» list** (Phase 15's documents box stays open): VISION.md
    (principles 1 and 3, the non-goals, «Privacy is enforced», the glossary's «measured»), the
    README's privacy lines, an «Amended» line in ADR 0007, 0009 and 0013, the strings that
    promise no location read again one by one, the guide's «With GPS» part, CHANGELOG.
  - **Merged into `main` is not released.** A release is a tag, and Phases 15 and 16 are
    released together or not at all; until then `main` holds a measured outing whose card
    does not yet say what the receiver is doing, and whose proof on a phone is not done.

- **18 Nov 2026.** **Phase 15, what the reader sees, and the documents** (owner, opening a new
  session on the branch, pull request 72 still a draft: "go on with Phase 15, then Phase 16;
  I cannot test on the device for now, so go on with everything that does not depend on the
  proof, and do not build the fallback service"). Phase 15's boxes are ticked but the two on
  a device; nothing here was tried on a phone.
  - **One line for the receiver, decided in the domain** (`GpsLine`, `GpsLines`, pure, 16
    tests): finding the satellites, the signal lost, location off on the phone, GPS stopped,
    the permission gone, and the three ways an outing started on the steps. Measuring has no
    line (the word under the distance says it) and neither has the receiver asleep at a stop
    (the numbers stand). In order: what «Resume GPS» could not mend comes first (the
    permission, the phone's switch), then the receiver lost, then what it is doing. Decided on
    the way:
    - *A tunnel is counted on the steps that were settled*, as the meter counts a hole, not on
      the clock: with the screen off the receiver holds its fixes up to a minute, and a card
      opened then would have said «no signal» until they came.
    - *Waking after a stop is not a tunnel.* The quarter of a minute is counted from the last
      fix or from when the receiver woke, whichever is later; «Finding the satellites» is said
      only before the first fix of a line (a start, a resume), not at every traffic light.
    - *Location turned off or on is heard from the receiver's own listener*
      (`LocationReading.Provider`), so the card changes at once and nothing polls.
  - **The distance says what it is** (`Measured.kt`): «measured» from nine tenths up
    (`RouteConstants.MEASURED_SHARE`), under that the share the steps gave («30% from steps»),
    and before the first fix «from steps»; the calories beside it keep Today's sign for an
    estimate (≈). The stretch whose fixes are not in yet is shown with it
    (`LiveMeasure.openMeters`), never told on.
  - **The pace, in words** (`MeasureFormatter.pace`, two units, «9:40 min/km» and «min/mi»):
    one line under the numbers, «Pace 10:05 min/km · now 9:48 min/km»; said by TalkBack and by
    the outing's voice as «10 minutes 5 seconds a kilometre». The pace of the whole only over
    a measured distance (ADR 0016 decision 7); the pace now only for half a minute after the
    last step (`RouteConstants.PACE_NOW_MILLIS`): a reader who has stopped has none. The
    voice says the outing's pace at each signal and at the goal. In Italian the pace is
    «andatura», which leaves «ritmo» to the cadence against the outing's own.
  - **`LiveSessionState.measure`** carries the three (the open stretch, the pace now, the
    line) from the service to the card and to the counting notification, which says the same
    in words and gains «Resume GPS» as a third button when the receiver was lost.
  - **«Said once», made exact.** A refusal is remembered (`gps_on_steps_told`, beside the
    reader's answer, not in the backup): the first outing it costs says so on its card, with
    the way back, and the ones after it do not. What is not a refusal is said each time it
    happens: location off on the phone, and the reminder's "Walk now" before the permission
    was ever asked. A grant clears it, so a later refusal is told once again.
  - **The way back goes through the outings' one door** (`OutingStarter.onCardAction`):
    «Resume GPS», the system's switch for location, or the system's request for the
    permission, alone (Passo's own screen is shown once). Where Android no longer shows its
    request (after a second refusal) its page for Passo opens instead: known from what it
    says before and after the request, with no timer. Granted from the card, an outing that
    lost its receiver takes it back at once.
  - **The question before the cap is on the card too**, read from the stored outing
    (`capQuestionMinutes`), with «Keep going»: no ticker, the minutes move as the steps do.
  - **The rows** of Today and History: «1.49 km measured», the satellites' mark before it,
    the calories still with their ≈. A distance partly measured stays with the estimates.
  - **The type change, apart from the service** (`ForegroundTypes`, 11 tests): granted,
    refused by the rule, refused in a way no page documents, held and then refused, dropped
    at a pause, a restarted process, the app opened mid-outing. The service itself is not run
    under a test: what its tests would add is what the phone answers.
  - **The documents** (box ticked): VISION (principles 1 and 3, the outings, the non-goals,
    the two success criteria, the glossary's «measured», «pace» and «fix»), the README (the
    outings' line, a row of its own for location, the backup's row; no dash), an «Amended»
    line and its paragraph in ADR 0007, 0009 and 0013, ADR 0016's status, the guide («With
    GPS», «What is kept, and what is never sent», the cap's question, the battery's part),
    Settings' privacy line, CHANGELOG. The strings that promised no location, read one by
    one: the guide's "Passo never knows where you are" was no longer true and now says that a
    way walked from home reads no location; `ways_intro` ("No location, only your steps") and
    the calibration's "no GPS" are true as they are, and stay.
  - **Not changed, and said to the owner as suggestions, since each would change a decision:**
    an outing that started on the steps stays on the steps when the permission is granted
    from its card (its `gps` is frozen at the start, as the model says), so the line promises
    the next outing, not this one; and after a refusal the editor's switch does not ask
    again by itself (ADR 0016 decision 2 asks at a start only).
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions`, `test`, `:app:lintDebug`,
    `:app:assembleDebug`: green, but for the six migration tests this machine cannot run. The
    card's screenshots looked at in every state, in the light and the dark, in Italian, at
    twice the text size and on the open foldable; the row in History. **Nothing on a phone.**

- **18 Nov 2026.** **Phase 16, the outing's page: the route, the splits, the climb** (owner:
  "then go on to Phase 16, from its section, without «Trim the ends»: a new branch from
  Phase 15's, and a draft pull request based on it, not on `main`"). On
  `feature/phase-16-outing-page`, pull request 73, a draft on top of 72. Every box ticked but
  the one on a device; nothing here was tried on a phone.
  - **The climb** (`Altitude`, `Climb`, `RouteHeights`, `PressureTrack`, pure; `PressureSource`).
    A point's height is what the pressure says, less what the weather is known to add, and
    the weather is learnt from the satellites over ten minutes: a front that drops three
    hectopascals over a flat two-hour walk climbs nothing, and a hill is in the pressure at
    once. Decided on the way, each a constant with its reason, to be validated on a device:
    - *The level's settling is kept out of the climb.* For its first five minutes the level is
      still being found, and that correction is the satellites', not the reader's legs.
    - *A point waits for its pressure.* The points come with the step batches and the
      pressure in batches of its own; a point given its height before its reading arrived
      would have had the satellites' alone. It waits at most a minute and a half, and the
      outing's end takes every point with the readings there are.
    - *Three metres with a barometer, ten without*, counted from where it was last counted;
      a pause or a ride cuts the count, so a funicular is not a climb.
    The sensor asks for no permission, is registered only with the satellite receiver and
    removed with it, never wakes the processor by itself, and is flushed in the wake the step
    batch already made.
  - **The splits are cut from legs laid as the outing goes** (`OutingLeg`, `Legs`, `Splits`),
    not from the route's points. A hundred metres at a time, from what the meter settles:
    the distance is the fused one, and the time in motion and the steps are the steps' own
    (ADR 0016 decision 5), so a kilometre's pace is the one the outing's own pace is made of
    and its cadence is counted. Kilometres or miles are cut from them afterwards, whichever
    the reader walks in; and they are numbers, so they stay when a route is deleted (the
    edge case "its numbers stay"). **It adds to ADR 0016 decision 8's list** of what `session`
    keeps (the fused distance, the steps' own, the measured share, the climb): the legs, in a
    table of their own, `session_leg`. Numbers, not places, as that decision asks; said to
    the owner, for a line in the ADR at their word.
  - **Schema 8, not a second 7** (`ascentMeters`, `descentMeters`, `session_leg`), and
    `routes.db` schema 2 (where a route's heights came from). The phase allowed either while
    Phase 15 is unreleased; a debug build installed to try Phase 15 on a phone must not meet
    a v7 that changed under it, which was this morning's own reason for making v7 whole.
  - **Energy on a slope**: 0.009 kcal a kilogram a metre climbed walking, 0.0045 running (the
    grade's term of the ACSM equations, in `MetricsConstants`), added to an outing with GPS
    when its climb is counted; nothing for coming down. The day's energy stays the steps'.
  - **`RouteMapView`**, Passo's first map that moves, and `MapViewport` with its gestures
    apart (Phases 18 and 19 reuse them): two fingers zoom, one drags it once it is zoomed, a
    double tap zooms where it lands, a button brings the whole route back. **One finger on a
    whole map is left to the page**, which scrolls: a map in a list must not trap the finger
    that only meant to read on. North up, a scale bar in round lengths of the reader's units,
    the line dashed where it was laid from the steps and never drawn across a pause, a ring
    where it began, a point where it ended, a bead a kilometre, named with its pace when
    touched (and said to TalkBack). The ground is a city's map where at least four fifths of
    the points lie in its frame (`RouteGround`), the finer of two that qualify; plain
    elsewhere, with the line that says Passo carries no street map.
  - **The page** (`OutingScreen`, `OutingViewModel`, one key in `:app`): the sentence, when it
    was, its numbers as tiles each with its line («Measured by satellite. Your steps said
    4.10 km»), an interval outing's bars, the kilometres as rows, the climb as a shape, and
    what the route can do. Reached from «See the outing» on a finished card, from an outing's
    row in Today and in History (a walk found in the steps opens nothing), and from the
    notification of its end. **No line for "the goal's pace"** among the splits: an outing has
    no pace to reach (ADR 0016 decision 7: shown, never a goal), so there is none to draw.
    The kilometres are rows with a bar, not columns: a walk has as many as it has kilometres,
    and a row still holds its numbers at twice the text size.
  - **Sharing and saving.** `GpxCodec`, GPX 1.1 by hand, a segment a stretch walked, read
    back too. The image is a card of its own (the name, the sentence, the map whole, three
    numbers), recorded out of sight and fitted as the year's pages are. **One file provider**:
    `SharedFiles` and `ShareImage` in `:core:designsystem` (`.share`), which the year's pages
    now go through; the year's own provider and its paths are gone. No «Trim the ends»: the
    GPX's choice says, once, that the file holds the walk from the first step to the last.
  - **Your data.** The backup's file gains an outing's climb and legs, and, only with «With
    the routes» ticked (off each time the page opens), its route: the app's own compact
    points in Base64, an added field. An import lays a route on the outing that started at
    its instant where that outing has none (`BackupMerge.routes`): the ones that came with
    their outings, and the ones of a phone restored from the cloud copy; never one taken
    away. «Delete all routes» says how many and what they weigh, and asks first. Both rows
    only on a phone that keeps routes.
  - **No read makes the routes' file** (`RouteFile`). Counting the routes for Your data, or
    deleting an outing too short to keep, opened `routes.db` and so made it, on a phone that
    had never measured with GPS: the second was Phase 15's own. Every read and every deletion
    now asks whether the file is there first; only a route written makes it.
  - **The documents**: the guide (an outing's own page; its route to take or to delete),
    CHANGELOG, the README (`outing-route.png` with its caption, the feature's line; no dash).
    Not changed, and said to the owner: CLAUDE.md's table of modules, which does not name
    this phase's classes yet.
  - **Checked.** `spotlessCheck`, `:app:checkForbiddenPermissions` (the same 13 permissions:
    the pressure sensor asks for none), `test`, `:app:lintDebug`, `:app:assembleDebug`:
    green, but for the seven migration tests this machine cannot run (the six, and 7 to 8).
    The page's screenshots looked at in each of its forms, in the light and the dark, in
    Italian, at twice the text size and on the open foldable; the map in both dresses and
    both themes, zoomed, with a bead named; Your data with its two rows and the dialog; the
    README's picture. **Nothing on a phone.**

- **18 Nov 2026.** **Phase 16 read again: the last points' pressure, and the shared image's
  test** (owner: "go on"; a second reading of the service and of the sharing, on
  `feature/phase-16-outing-page`, pull request 73).
  - **The pressure sensor outlives the receiver by the moment of a write.** At an outing's
    end the receiver, and the pressure sensor with it, were removed before the end's own
    write asked the sensor for what it held: with the screen off that is up to a minute of
    readings, so the last points of a route took the satellites' height alone and a climb in
    the last minute could go uncounted. The sensor is now kept until the end's harvest is
    written, then removed, unless the receiver is on again (an end taken back at once). A
    pause and a stop from the card ask the sensor first too, as they ask the receiver. No
    new wake: each happens in the wake that ended or touched the outing, and the wait gives
    up after the same bound as the receiver's.
  - **The image to share has its test** (`OutingScreenTest`): the card recorded out of sight
    is 360 dp wide whatever the page's, opaque, and adds no second map to the page; its
    picture is written to `build/screenshots` and was looked at.
