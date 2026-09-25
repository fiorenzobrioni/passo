#!/usr/bin/env python3
"""Draws Passo's launcher mark: the two adaptive-icon layers in app/src/main/res/drawable.

    python3 tools/draw_launcher_icon.py

The mark is Chiaro's ring with Passo's step on it (owner's choice, 25 Sep 2026): the same
ring as Chiaro's badge (radius 21, stroke 10, on the same warm white), green instead of
sky, and where Chiaro has its sun (upper right) Passo has a shoe print, mirrored to the
upper left. The print cuts the ring the way the sun does, with a gap around it, and walks
clockwise, the way the ring fills: it is the step that closes the day.

Re-running this script IS the drawing: ic_launcher_foreground.xml and
ic_launcher_monochrome.xml are its output and are not edited by hand. Only the standard
library is used, so any Python 3 runs it.
"""

import math
from pathlib import Path

RES = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable"

CENTRE = 54.0
RING_RADIUS = 21.0  # Chiaro's
RING_WIDTH = 10.0  # Chiaro's
PRINT_ANGLE = 220.0  # degrees clockwise from three o'clock: Chiaro's sun is at -40
PRINT_SCALE = 0.95
GAP = 2.6  # the clear band around the print, where it cuts the ring

# The ring's sweep, from where it starts (just past the print) to where it ends (just
# before it): the day filling up, a fresh green to the deep green of a met goal.
RING_STOPS = [(0.0, "74CF91"), (0.45, "2B9E56"), (1.0, "0B5B33")]
# The print: amber, the warm light of Chiaro's sun, top to heel.
PRINT_TOP, PRINT_HEEL = "FFC658", "EF8618"

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
# The band between the two pieces, so the gap is one shape and no sliver of ring shows
# between forefoot and heel: a capsule along the sole, in the sole's units.
BRIDGE_FROM, BRIDGE_TO, BRIDGE_WIDTH = (0.4, -7.0), (0.3, 8.0), 7.4


def f(v):
    return f"{v:.2f}"


def pt(p):
    return f"{f(p[0])},{f(p[1])}"


def print_origin():
    a = math.radians(PRINT_ANGLE)
    return CENTRE + RING_RADIUS * math.cos(a), CENTRE + RING_RADIUS * math.sin(a)


def place(p):
    """From the sole's units to the canvas: on the ring, pointing clockwise along it."""
    rho = math.radians(PRINT_ANGLE + 180)
    ox, oy = print_origin()
    x, y = p[0] * PRINT_SCALE, p[1] * PRINT_SCALE
    return ox + x * math.cos(rho) - y * math.sin(rho), oy + x * math.sin(rho) + y * math.cos(rho)


def path_data(segments):
    out = []
    for seg in segments:
        kind, *points = seg
        out.append(kind + " " + " ".join(pt(place(p)) for p in points))
    return " ".join(out) + " Z"


def outline(segments, per_curve=24):
    """The shape as a dense polygon on the canvas."""
    points, current = [], None
    for kind, *ps in segments:
        ps = [place(p) for p in ps]
        if kind == "M":
            current = ps[0]
            points.append(current)
        elif kind == "L":
            current = ps[0]
            points.append(current)
        else:
            c1, c2, end = ps
            for i in range(1, per_curve + 1):
                t = i / per_curve
                u = 1 - t
                points.append(
                    tuple(
                        u**3 * current[k] + 3 * u * u * t * c1[k] + 3 * u * t * t * c2[k] + t**3 * end[k]
                        for k in (0, 1)
                    )
                )
            current = end
    if math.dist(points[0], points[-1]) < 1e-6:
        points.pop()
    return points


def signed_area(poly):
    return sum(a[0] * b[1] - b[0] * a[1] for a, b in zip(poly, poly[1:] + poly[:1])) / 2


def offset(poly, d):
    """Grows a convex polygon by d: each vertex moves out along its averaged normal."""
    # For a positive signed area the outward normal of an edge (dx, dy) is (dy, -dx).
    s = 1 if signed_area(poly) > 0 else -1
    n = len(poly)
    out = []
    for i in range(n):
        prev, here, nxt = poly[i - 1], poly[i], poly[(i + 1) % n]
        normals = []
        for a, b in ((prev, here), (here, nxt)):
            dx, dy = b[0] - a[0], b[1] - a[1]
            length = math.hypot(dx, dy) or 1
            normals.append((s * dy / length, -s * dx / length))
        nx, ny = normals[0][0] + normals[1][0], normals[0][1] + normals[1][1]
        length = math.hypot(nx, ny) or 1
        # Keep the band GAP wide across a corner too, not only along the edges.
        cos_half = max(0.5, (normals[0][0] * nx + normals[0][1] * ny) / length)
        out.append((here[0] + nx / length * d / cos_half, here[1] + ny / length * d / cos_half))
    return out


def capsule():
    a, b = place(BRIDGE_FROM), place(BRIDGE_TO)
    r = BRIDGE_WIDTH * PRINT_SCALE / 2 + GAP
    dx, dy = b[0] - a[0], b[1] - a[1]
    length = math.hypot(dx, dy)
    ux, uy = dx / length, dy / length
    points = []
    for centre, start in ((b, math.atan2(uy, ux) - math.pi / 2), (a, math.atan2(uy, ux) + math.pi / 2)):
        for i in range(0, 25):
            t = start + math.pi * i / 24
            points.append((centre[0] + r * math.cos(t), centre[1] + r * math.sin(t)))
    return points


def hole(poly):
    """A clip that keeps everything but poly: the canvas clockwise, the shape the other way."""
    if signed_area(poly) > 0:
        poly = poly[::-1]
    return "M 0,0 H 108 V 108 H 0 Z M " + " L ".join(pt(p) for p in poly) + " Z"


def clips():
    shapes = [offset(outline(FOREFOOT), GAP), offset(outline(HEEL), GAP), capsule()]
    # One group per hole: nested clips intersect, so the ring keeps what is outside all three.
    return [f'<group>\n<clip-path android:pathData="{hole(s)}"/>' for s in shapes]


def ring_gradient():
    """The sweep starts at three o'clock; turn the ring's stops so the seam hides under the print."""
    seam = PRINT_ANGLE / 360

    def colour_at(t):
        for (t0, c0), (t1, c1) in zip(RING_STOPS, RING_STOPS[1:]):
            if t0 <= t <= t1:
                k = (t - t0) / (t1 - t0)
                a, b = int(c0, 16), int(c1, 16)
                return "".join(
                    f"{round(((a >> sh) & 255) * (1 - k) + ((b >> sh) & 255) * k):02X}" for sh in (16, 8, 0)
                )
        raise ValueError(t)

    items = [(0.0, colour_at(1 - seam))]
    for t, c in RING_STOPS:
        o = seam + t
        if o > 1:
            items.append((o - 1, c))
    items.append((seam - 0.0005, RING_STOPS[-1][1]))
    items.append((seam + 0.0005, RING_STOPS[0][1]))
    for t, c in RING_STOPS[1:]:
        o = seam + t
        if o < 1:
            items.append((o, c))
    items.append((1.0, colour_at(1 - seam)))
    items.sort()
    return "\n".join(
        f'                <item android:offset="{o:.4f}" android:color="#FF{c}"/>' for o, c in items
    )


def circle(r):
    x0 = CENTRE - r
    return f"M {f(x0)},{f(CENTRE)} a {f(r)},{f(r)} 0 1,0 {f(2 * r)},0 a {f(r)},{f(r)} 0 1,0 {f(-2 * r)},0 z"


HEADER = """<?xml version="1.0" encoding="utf-8"?>
<!-- Written by tools/draw_launcher_icon.py: change the script and run it, never this file.
{note}-->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
"""

FOREGROUND_NOTE = """
     The mark: Chiaro's ring (radius 21, stroke 10, the same warm white under it) in the
     greens of a day filling up, and where Chiaro has its sun, mirrored to the upper left,
     a shoe print in the sun's amber. The print cuts the ring with a 2.6 gap, as the sun
     does, and walks clockwise, the way the ring fills: the step that closes the day.
     Everything sits inside the 33-unit safe circle of every launcher mask.
"""

MONOCHROME_NOTE = """
     The themed icon (Android 13+): the system reads only the alpha, so the mark is
     restated as shapes, the ring as a filled annulus (evenOdd) with the same cut, and the
     print. See ic_launcher_foreground for the drawing.
"""


def sole(fill):
    return "\n".join(f'<path android:pathData="{path_data(s)}"{fill}' for s in (FOREFOOT, HEEL))


def foreground():
    top, heel = place((0, -12.4)), place((0, 12.2))
    gradient_fill = f""">
    <aapt:attr name="android:fillColor">
        <gradient android:type="linear" android:startX="{f(top[0])}" android:startY="{f(top[1])}" android:endX="{f(heel[0])}" android:endY="{f(heel[1])}">
            <item android:offset="0" android:color="#FF{PRINT_TOP}"/>
            <item android:offset="1" android:color="#FF{PRINT_HEEL}"/>
        </gradient>
    </aapt:attr>
</path>"""
    groups = clips()
    return (
        HEADER.format(note=FOREGROUND_NOTE)
        + "\n".join(groups)
        + f"""
<path android:pathData="{circle(RING_RADIUS)}" android:strokeWidth="{f(RING_WIDTH)}" android:fillColor="#00000000">
    <aapt:attr name="android:strokeColor">
        <gradient android:type="sweep" android:centerX="{f(CENTRE)}" android:centerY="{f(CENTRE)}">
{ring_gradient()}
        </gradient>
    </aapt:attr>
</path>
"""
        + "</group>\n" * len(groups)
        + sole(gradient_fill)
        + "\n</vector>\n"
    )


def monochrome():
    groups = clips()
    outer, inner = RING_RADIUS + RING_WIDTH / 2, RING_RADIUS - RING_WIDTH / 2
    return (
        HEADER.format(note=MONOCHROME_NOTE)
        + "\n".join(groups)
        + f'\n<path android:pathData="{circle(outer)} {circle(inner)}" android:fillColor="#FFFFFFFF" android:fillType="evenOdd"/>\n'
        + "</group>\n" * len(groups)
        + sole(' android:fillColor="#FFFFFFFF"/>')
        + "\n</vector>\n"
    )


if __name__ == "__main__":
    (RES / "ic_launcher_foreground.xml").write_text(foreground())
    (RES / "ic_launcher_monochrome.xml").write_text(monochrome())
    print("wrote", RES / "ic_launcher_foreground.xml", "and", RES / "ic_launcher_monochrome.xml")
