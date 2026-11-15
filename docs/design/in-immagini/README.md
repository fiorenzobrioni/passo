# «In pictures» («In immagini»): the design's sources

The boards of the canvas the owner approved on 15 Nov 2026
(https://claude.ai/artifact/VFwVjbxyAyWcWRLcRW9uhY, page «Passo»), **frozen on that day**. They are
the reference of Phase 15 (`PLANNING.md` §11, «Phase 15 — «In pictures», the day on foot drawn»):
where the plan writes no measure, the measure is here.

| File | Board |
|---|---|
| `PassoCard.dc.html` | The card itself, every form: the one component every board draws |
| `PassoForms.dc.html` | The eight forms, 2×1 to 4×4, at their real sizes in dp, and four grounds |
| `PassoConfig.dc.html` | The settings screen (interactive on the canvas) |

**How to read them.** They are pages in the canvas's Design Component format: without its runtime
they do not open in a browser, they are read as source. The inline styles are the measures (`px` =
dp, `font-size` = sp, the strings are the Italian UI); the drawing's geometry (the curve, the usual,
the goal's line, the hour bars, the walks) is computed in the `Component` class at the end of
`PassoCard.dc.html`, by `renderVals()` and its `plate` and `mono` functions. The data (Sunday
15 Nov, 16:40) are a sample: the card reads the real ones.

**Not to be edited.** A measure the implementation changes is written in `PLANNING.md` with its
reason (Phase 15's fidelity rule), not here.
