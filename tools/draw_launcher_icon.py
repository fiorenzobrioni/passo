#!/usr/bin/env python3
"""Draws Passo's mark: the adaptive icon's three layers in app/src/main/res/drawable, the
status-bar icon (core/tracking/src/main/res/drawable/ic_stat_steps.xml) and the walker that
rides an outing's progress bar (ic_progress_walker.xml, beside it).

    python3 tools/draw_launcher_icon.py

The mark is TWO PRINTS walking to the upper right (owner, 9 Nov 2026, when the family's marks
were redrawn as one series): the print behind white, the one ahead amber, the step being taken,
as the amber print was on the ring it replaced (25 Sep 2026). The sole is the app's own, the
one the status-bar icon walked with already.

The series' rules, the same in every app of the family:
- the badge's ground is the app's own colour, here the green, from its light tone at the upper
  left to its deep tone at the lower right; the symbol is white with one amber accent;
- every shape is lit from the upper left: a linear ramp from its light tone at the upper left of
  its box to its deep tone at the lower right, over the box's larger side; no shadow (the store
  and the launchers add their own);
- the symbol stays inside the keyline, a circle of KEYLINE around the centre: 56 dp of the 72
  that show, so a round mask leaves 8 dp of air all round and no mask of any launcher reaches it.

The status-bar icon is the same two prints in one ink on the 24-unit grid (the system tints it);
it is also the Quick Settings tile's icon. The walker is the two prints on a green disc, white
and amber, because the system draws a progress tracker's icon as it is, untinted. The badge's
ground is written a second time beside them (ic_shortcut_ground.xml), as the ground of the
Outing shortcut's adaptive icon: a library cannot name the app's drawables.

Re-running this script IS the drawing: the six files are its output and are not edited by hand.
Only the standard library is used, so any Python 3 runs it.
"""

import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
TRACKING = ROOT / "core/tracking/src/main/res/drawable"

CENTRE = 54.0
KEYLINE = 28.0  # the symbol's reach from the centre: measured 28.1

# The family's palette, the tones this mark uses: (light, base, deep).
GREEN = ("5CCB86", "27A35A", "17834A")
SUN = ("FFD36A", "FDB528", "F28C17")
WHITE = ("FFFFFF", "F4F7FB", "E3EAF4")

# The two prints: (middle, mirrored for a left foot, its tones). Both turned the same way,
# PRINT_TURN degrees clockwise from upright, a step apart along the walk.
PRINT_SCALE = 1.22
PRINT_TURN = 24.0
PRINTS = [((42.5, 62.0), True, WHITE), ((64.1, 46.4), False, SUN)]
PRINT_REACH = 12.5  # half a sole's length in its own units: the box its light is laid over

# The status-bar icon and the walker: the same prints, fitted to the 24-unit grid.
STAT_SCALE, STAT_PRINT = 0.415, 1.06  # the drawing's scale, and the prints' own, a little heavier
WALKER_SCALE, WALKER_PRINT = 0.36, 1.1

# A right shoe sole in its own units, toe towards -y, about 24 long, origin mid-length:
# a long forefoot with a flat back edge, and a D-shaped heel with a flat front. Each is a
# list of segments: ("M", p), ("C", c1, c2, p) or ("L", p).
FOREFOOT = [
    ("M", (0.2, -12.4)),
    ("C", (4.4, -12.6), (6.0, -9.0), (5.7, -5.2)),
    ("C", (5.5, -2.6), (4.8, -0.4), (4.2, 1.4)),
    ("L", (-2.9, 1.6)),
    ("C", (-3.9, -0.8), (-4.6, -3.4), (-4.6, -6.0)),
    ("C", (-4.6, -9.6), (-3.2, -12.2), (0.2, -12.4)),
]
HEEL = [
    ("M", (-2.9, 4.0)),
    ("L", (3.9, 3.8)),
    ("C", (4.2, 5.0), (4.2, 6.6), (4.0, 8.2)),
    ("C", (3.7, 10.8), (2.1, 12.2), (0.4, 12.2)),
    ("C", (-1.4, 12.2), (-3.0, 10.9), (-3.3, 8.4)),
    ("C", (-3.5, 6.8), (-3.4, 5.3), (-2.9, 4.0)),
]

HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
     {what} -->
"""


def f(v):
    return f"{v:.2f}"


def lit(box):
    """A shape's light: from the upper-left corner of its box to the lower-right one, along the
    diagonal of a square on the box's larger side, so every shape is lit at the same angle."""
    x0, y0, x1, y1 = box
    cx, cy, r = (x0 + x1) / 2, (y0 + y1) / 2, max(x1 - x0, y1 - y0) / 2
    return (cx - r, cy - r), (cx + r, cy + r)


def ramp(attr, box, light, deep, indent="    "):
    (sx, sy), (ex, ey) = lit(box)
    return (
        f'{indent}<aapt:attr name="android:{attr}">\n'
        f'{indent}    <gradient android:type="linear" android:startX="{f(sx)}" android:startY="{f(sy)}" '
        f'android:endX="{f(ex)}" android:endY="{f(ey)}">\n'
        f'{indent}        <item android:offset="0" android:color="#FF{light}"/>\n'
        f'{indent}        <item android:offset="1" android:color="#FF{deep}"/>\n'
        f"{indent}    </gradient>\n{indent}</aapt:attr>\n"
    )


def shaded(d, box, light, deep):
    return f'<path android:pathData="{d}">\n{ramp("fillColor", box, light, deep)}</path>\n'


def flat(d, colour):
    return f'<path android:pathData="{d}" android:fillColor="#FF{colour}"/>\n'


def vector(body, size=108, aapt=True):
    ns = '\n    xmlns:aapt="http://schemas.android.com/aapt"' if aapt else ""
    return (
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android"{ns}\n'
        f'    android:width="{size}dp"\n    android:height="{size}dp"\n'
        f'    android:viewportWidth="{size}"\n    android:viewportHeight="{size}">\n'
        f"{body}</vector>\n"
    )


def fitted(body, scale):
    """The drawing on the 108 grid, scaled about its centre into the middle of the 24 grid."""
    return (
        f'<group android:pivotX="{f(CENTRE)}" android:pivotY="{f(CENTRE)}" android:scaleX="{scale:g}" '
        f'android:scaleY="{scale:g}" android:translateX="{f(12 - CENTRE)}" android:translateY="{f(12 - CENTRE)}">\n'
        f"{body}</group>\n"
    )


def placed(middle, scale, mirrored):
    """From the sole's units to the grid: scaled, mirrored for a left foot, turned to the walk."""
    t = math.radians(PRINT_TURN)

    def at(p):
        x, y = p[0] * scale * (-1 if mirrored else 1), p[1] * scale
        return middle[0] + x * math.cos(t) - y * math.sin(t), middle[1] + x * math.sin(t) + y * math.cos(t)

    return at


def sole(at):
    out = []
    for piece in (FOREFOOT, HEEL):
        for kind, *points in piece:
            out.append(kind + " " + " ".join(f"{f(at(p)[0])},{f(at(p)[1])}" for p in points))
        out.append("Z")
    return " ".join(out)


def print_box(middle, scale):
    r = PRINT_REACH * scale
    return (middle[0] - r, middle[1] - r, middle[0] + r, middle[1] + r)


def background():
    body = shaded("M 0,0 H 108 V 108 H 0 Z", (18, 18, 90, 90), GREEN[0], GREEN[2])
    return HEADER.format(what="The badge's ground: the app's green, lit from the upper left.") + vector(body)


def foreground():
    body = "".join(
        shaded(sole(placed(middle, PRINT_SCALE, mirrored)), print_box(middle, PRINT_SCALE), tones[0], tones[2])
        for middle, mirrored, tones in PRINTS
    )
    what = (
        "The mark: two prints walking to the upper right, the one behind white, the step being\n"
        f"     taken amber; both inside {KEYLINE:g} units of the centre, the keyline."
    )
    return HEADER.format(what=what) + vector(body)


def monochrome():
    body = "".join(
        flat(sole(placed(middle, PRINT_SCALE, mirrored)), "FFFFFF") for middle, mirrored, _ in PRINTS
    )
    what = (
        "The themed icon (Android 13+): the system reads only the alpha, so the mark is\n"
        "     restated as its two prints in one ink."
    )
    return HEADER.format(what=what) + vector(body, aapt=False)


def status_bar():
    prints = "".join(
        flat(sole(placed(middle, PRINT_SCALE * STAT_PRINT, mirrored)), "FFFFFF") for middle, mirrored, _ in PRINTS
    )
    what = (
        "The status-bar mark of the notifications and the Quick Settings tile: the launcher's\n"
        "     two prints in one ink on the 24-unit grid. White on transparent, as status-bar icons\n"
        "     must be; the system tints it."
    )
    return HEADER.format(what=what) + vector(fitted(prints, STAT_SCALE), size=24, aapt=False)


def walker():
    disc = "M 0,12 A 12,12 0 1,0 24,12 A 12,12 0 1,0 0,12 Z"
    prints = "".join(
        flat(sole(placed(middle, PRINT_SCALE * WALKER_PRINT, mirrored)), "FFFFFF" if tones is WHITE else SUN[0])
        for middle, mirrored, tones in PRINTS
    )
    what = (
        "The walker riding the outing's bar (Android 16's ProgressStyle): the system draws a tracker\n"
        "     icon as it is, untinted, so the launcher's two prints, white and amber, stand on a disc\n"
        "     of its green, which reads on a light shade and on a dark one alike."
    )
    body = shaded(disc, (0, 0, 24, 24), GREEN[0], GREEN[2]) + fitted(prints, WALKER_SCALE)
    return HEADER.format(what=what) + vector(body, size=24)


def shortcut_ground():
    body = shaded("M 0,0 H 108 V 108 H 0 Z", (18, 18, 90, 90), GREEN[0], GREEN[2])
    what = (
        "The ground of the Outing shortcut's adaptive icon (ic_shortcut_outing.xml): the launcher\n"
        "     icon's ground, written here too because a library cannot name the app's drawables."
    )
    return HEADER.format(what=what) + vector(body)


if __name__ == "__main__":
    for path, text in (
        (RES / "ic_launcher_background.xml", background()),
        (RES / "ic_launcher_foreground.xml", foreground()),
        (RES / "ic_launcher_monochrome.xml", monochrome()),
        (TRACKING / "ic_stat_steps.xml", status_bar()),
        (TRACKING / "ic_progress_walker.xml", walker()),
        (TRACKING / "ic_shortcut_ground.xml", shortcut_ground()),
    ):
        path.write_text(text)
        print("wrote", path)
