"""Dibuja escenarios, objetos y ambientes nuevos. No toca los que ya existen.

Uso: python animation-engine/arte/lugares.py [--rehacer casa pelota]
"""
import json
import math
import random
import sys
from pathlib import Path

MEDIA = Path(__file__).resolve().parents[2] / "media"
W, H = 2200, 720
REDO = sys.argv[sys.argv.index("--rehacer") + 1:] if "--rehacer" in sys.argv else []


def svg(p, body, w=W, h=H, defs=""):
    p.parent.mkdir(parents=True, exist_ok=True)
    d = f"<defs>{defs}</defs>\n" if defs else ""
    p.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">\n{d}{body}\n</svg>\n', encoding="utf-8")


def rect(x, y, w, h, f, r=0, extra=""):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{f}" {extra}/>'


def circ(x, y, r, f, extra=""):
    return f'<circle cx="{x:.0f}" cy="{y:.0f}" r="{r:.0f}" fill="{f}" {extra}/>'


def ell(x, y, rx, ry, f, extra=""):
    return f'<ellipse cx="{x}" cy="{y}" rx="{rx}" ry="{ry}" fill="{f}" {extra}/>'


def path(d, f, extra=""):
    return f'<path d="{d}" fill="{f}" {extra}/>'


def sky(a, b):
    return rect(0, 0, W, H, "url(#s)"), f'<linearGradient id="s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="{a}"/><stop offset="0.8" stop-color="{b}"/></linearGradient>'


def clouds(pts, o=0.95):
    return "".join(ell(x, y, 80 * s, 28 * s, "#ffffff", f'opacity="{o}"') + ell(x - 35 * s, y - 20 * s, 42 * s, 28 * s, "#ffffff", f'opacity="{o}"') + ell(x + 25 * s, y - 26 * s, 48 * s, 34 * s, "#ffffff", f'opacity="{o}"') for x, y, s in pts)


def sun(x=1050, y=120):
    return circ(x, y, 95, "#fff6c8", 'opacity="0.5"') + circ(x, y, 62, "#ffe994")


def hills(c, y, a, n, seed):
    q, d, step = random.Random(seed), f"M0 720 L0 {y}", W / n
    for i in range(n):
        d += f" Q{step * (i + .5):.0f} {y - a * q.uniform(.5, 1.5):.0f} {step * (i + 1):.0f} {y + q.uniform(-10, 10):.0f}"
    return path(f"{d} L{W} 720Z", c)


def tree(x, y, s, c1, c2, tr="#7a5238"):
    def g(a, b, r, c):
        return circ(x + a * s, y + b * s, r * s, c)
    return (path(f"M{x - 20 * s:.0f} {y} Q{x - 8 * s:.0f} {y - 90 * s:.0f} {x - 12 * s:.0f} {y - 200 * s:.0f} L{x + 12 * s:.0f} {y - 200 * s:.0f} Q{x + 8 * s:.0f} {y - 90 * s:.0f} {x + 22 * s:.0f} {y}Z", tr)
            + g(-55, -205, 62, c1) + g(58, -210, 60, c1) + g(0, -262, 78, c1) + g(0, -200, 58, c1) + g(-30, -272, 34, c2) + g(38, -232, 28, c2))


def pine(x, y, s, c, c2):
    out = rect(x - 9 * s, y - 40 * s, 18 * s, 40 * s, "#6d4a33")
    for i, (w, t) in enumerate([(85, 0), (68, 60), (50, 118)]):
        out += path(f"M{x - w * s:.0f} {y - (30 + t) * s:.0f} L{x} {y - (130 + t) * s:.0f} L{x + w * s:.0f} {y - (30 + t) * s:.0f}Z", c if i % 2 == 0 else c2)
    return out


def grass(seed, colors, y1=(555, 585), y2=(655, 712), n=80, flowers=26):
    r, out = random.Random(seed), ""
    for _ in range(n):
        x, y = r.randint(0, W), r.choice([r.randint(*y1), r.randint(*y2)])
        out += path(f"M{x - 7} {y} L{x - 4} {y - 16} L{x} {y - 2} L{x + 4} {y - 18} L{x + 8} {y}Z", r.choice(colors))
    for _ in range(flowers):
        x, y, c = r.randint(0, W), r.choice([r.randint(y1[0], y1[1] - 5), r.randint(y2[0] + 5, y2[1])]), r.choice(["#ff8fb1", "#ffd24d", "#ffffff", "#b79cf0"])
        out += "".join(circ(x + 6 * math.cos(k * 1.2566), y + 6 * math.sin(k * 1.2566), 5, c) for k in range(5)) + circ(x, y, 4, "#f0a030")
    return out


def ground(c1, c2, seed, colors=("#57ab4f", "#7cc95f"), flowers=26):
    return (path("M0 545 Q550 525 1100 545 T2200 545 L2200 720 L0 720Z", c1) + path("M0 588 Q550 572 1100 590 T2200 585 L2200 640 Q1650 652 1100 640 T0 642Z", c2) + grass(seed, colors, flowers=flowers))


def bushes(xs, a="#2c7a45", b="#358a50"):
    return "".join(circ(x + dx, 735 - dy, rr, c) for x in xs for dx, dy, rr, c in ((-70, 0, 60, a), (60, 0, 66, a), (0, 24, 64, b)))


def place(key, layers, fg=(), interior=False, name=None):
    E = MEDIA / "environments" / key
    if (E / "escenario.json").exists() and key not in REDO:
        return
    files, front = [], []
    for i, (body, factor, *defs) in enumerate(layers, 1):
        f = f"{i}.svg"
        svg(E / f, body, defs=defs[0] if defs else "")
        files.append([f, factor])
    for i, (body, factor) in enumerate(fg, 1):
        f = f"frente-{i}.svg"
        svg(E / f, body)
        front.append([f, factor])
    (E / "escenario.json").write_text(json.dumps({"id": key, "version": 1, "license": "propia", "interior": interior, "originX": -300, "layers": files, "foreground": front, "groundY": 622}, indent=1), encoding="utf-8")
    print("lugar", key)


def casa():
    wall = rect(0, 0, W, 560, "#fbe3c0") + "".join(rect(x, 0, 34, 560, "#f7d7ab") for x in range(0, W, 110)) + rect(0, 520, W, 44, "#fff6e6") + rect(0, 516, W, 8, "#e2c49a")

    def window(x):
        return (rect(x, 120, 250, 230, "#8a5a34", 14) + rect(x + 14, 134, 222, 202, "#bfe6fb", 6) + circ(x + 180, 178, 26, "#ffe994") + path(f"M{x + 14} 336 L{x + 14} 282 Q{x + 80} 240 {x + 140} 284 T{x + 236} 270 L{x + 236} 336Z", "#8ccf9c")
                + rect(x + 119, 134, 12, 202, "#8a5a34") + rect(x + 14, 228, 222, 12, "#8a5a34") + path(f"M{x - 30} 104 Q{x + 40} 130 {x + 10} 380 L{x - 34} 380Z", "#e0756b") + path(f"M{x + 280} 104 Q{x + 210} 130 {x + 240} 380 L{x + 284} 380Z", "#e0756b") + rect(x - 44, 96, 338, 14, "#6d4326", 7))

    def frame(x, y, c):
        return rect(x, y, 110, 90, "#8a5a34", 6) + rect(x + 10, y + 10, 90, 70, c) + circ(x + 55, y + 50, 22, "#fff6e6")
    shelf = rect(1240, 190, 260, 16, "#8a5a34", 4) + "".join(rect(1256 + i * 26, 190 - h, 20, h, c, 2) for i, (h, c) in enumerate([(70, "#e0564a"), (84, "#3a7fc4"), (62, "#f2a93b"), (78, "#3fae6a"), (66, "#8e5bd0")])) + path("M1430 190 L1440 140 L1480 140 L1490 190Z", "#d9774a") + circ(1460, 120, 30, "#5bb572")
    floor = rect(0, 556, W, 164, "#c98d55") + "".join(rect(0, y, W, 4, "#b57a45") for y in (592, 632, 676)) + "".join(rect(x, y, 4, 40, "#b57a45") for x, y in [(180, 556), (620, 592), (990, 632), (1430, 592), (1830, 556), (320, 676), (1200, 676)])
    rug = ell(930, 640, 430, 52, "#7a9bd6") + ell(930, 640, 350, 40, "#f3e7cf") + ell(930, 640, 250, 27, "#e0756b")
    sofa = (rect(360, 400, 380, 130, "#5b9c8f", 34) + rect(330, 450, 80, 120, "#4c877b", 28) + rect(690, 450, 80, 120, "#4c877b", 28) + rect(390, 470, 320, 80, "#6fb0a3", 18) + rect(540, 474, 6, 70, "#4c877b") + rect(370, 560, 20, 22, "#6d4326") + rect(710, 560, 20, 22, "#6d4326"))
    lamp = rect(1620, 300, 10, 270, "#6d4326") + ell(1625, 572, 50, 10, "#6d4326") + path("M1560 310 L1590 210 L1660 210 L1690 310Z", "#ffd97a") + circ(1625, 290, 120, "#fff1b8", 'opacity="0.35"')
    table = rect(1180, 470, 230, 18, "#8a5a34", 6) + rect(1200, 486, 14, 90, "#6d4326") + rect(1376, 486, 14, 90, "#6d4326") + path("M1260 470 L1268 430 L1318 430 L1326 470Z", "#3a7fc4") + "".join(circ(x, y, 15, c) for x, y, c in [(1276, 415, "#ff8fb1"), (1308, 408, "#ffd24d"), (1292, 392, "#ffffff")])
    place("casa", [(wall + window(880) + window(1800) + frame(440, 150, "#f2a93b") + frame(600, 190, "#8fd07a") + shelf, 0.9), (floor + rug, 1.0), (sofa + lamp + table, 1.0)], interior=True)


def lago():
    s, d = sky("#6fc3f0", "#dff3fb")
    water = rect(0, 420, W, 180, "#4fa8d8") + "".join(path(f"M{x} {y} q20 -8 40 0 t40 0", "none", 'stroke="#a9dcf5" stroke-width="5" stroke-linecap="round"') for x, y in [(80 + i * 137 % 2100, 440 + (i * 53) % 110) for i in range(30)])
    sail = path("M1180 420 L1180 330 L1250 412Z", "#ffffff") + path("M1172 420 L1172 345 L1120 412Z", "#f6d6d0") + path("M1100 424 L1270 424 L1240 450 L1130 450Z", "#c9573f")
    lilies = "".join(ell(x, y, 46, 13, "#3f9a5c") + path(f"M{x - 12} {y - 6} Q{x} {y - 30} {x + 12} {y - 6}Z", "#ff9fc0") for x, y in [(420, 540), (880, 520), (1530, 548), (1840, 526)])
    shore = path("M0 596 Q300 566 700 590 T1500 584 T2200 592 L2200 720 L0 720Z", "#e9d6a4") + path("M0 660 Q600 640 1200 662 T2200 655 L2200 720 L0 720Z", "#6bbf59") + grass(5, ["#57ab4f", "#7cc95f"], y1=(662, 680), y2=(676, 712), n=60, flowers=16)
    dock = "".join(rect(1290 + i * 44, 548, 40, 46, "#a8744a", 3) for i in range(9)) + "".join(rect(x, 560, 16, 70, "#7a5238") for x in (1310, 1480, 1650))
    reeds = "".join(path(f"M{x} 700 Q{x + k * 8} 620 {x + k * 14} {560 - (k % 3) * 20}", "none", 'stroke="#3f8a4c" stroke-width="7" stroke-linecap="round"') + ell(x + k * 14, 556 - (k % 3) * 20, 8, 24, "#7a4a2c") for x in (150, 1740) for k in (-1, 0, 1, 2))
    place("lago", [(s + sun(700, 110) + clouds([(300, 130, 1), (1250, 160, 1.2), (1750, 100, .9)]), 0.05, d), (hills("#a6cfe0", 420, 160, 5, 11) + hills("#7fbf93", 440, 70, 8, 12), 0.2), (water + sail, 0.5), (lilies, 0.8), (shore + dock, 1.0)], [(reeds, 1.25)])


def escuela():
    s, d = sky("#86cdf4", "#eaf7ea")

    def win(x, y):
        return rect(x, y, 96, 110, "#ffffff", 8) + rect(x + 8, y + 8, 80, 94, "#a9dcf5", 4) + rect(x + 44, y + 8, 8, 94, "#ffffff") + rect(x + 8, y + 50, 80, 8, "#ffffff")
    b = rect(560, 250, 900, 310, "#f4b860") + rect(560, 528, 900, 34, "#c9803a") + path("M530 256 L1010 120 L1490 256Z", "#c9573f") + rect(930, 60, 160, 150, "#f4b860") + path("M910 66 L1010 -10 L1110 66Z", "#c9573f") + circ(1010, 130, 42, "#ffffff") + path("M1010 130 L1010 100 M1010 130 L1032 142", "none", 'stroke="#4a3a2c" stroke-width="7" stroke-linecap="round"')
    b += win(610, 310) + win(750, 310) + win(1180, 310) + win(1320, 310) + rect(940, 380, 140, 182, "#7a5238", 10) + rect(952, 394, 54, 160, "#9c6b48", 6) + rect(1014, 394, 54, 160, "#9c6b48", 6) + circ(1000, 480, 6, "#f2d36b") + circ(1020, 480, 6, "#f2d36b") + rect(880, 330, 260, 40, "#ffffff", 8)
    b += "".join(circ(905 + i * 34, 350, 10, c) for i, c in enumerate(["#e0564a", "#f2a93b", "#3fae6a", "#3a7fc4", "#8e5bd0", "#e0564a", "#f2a93b"]))
    flag = rect(1600, 160, 10, 400, "#b9b9b9") + path("M1610 170 Q1660 150 1700 172 T1770 170 L1770 240 Q1720 260 1690 238 T1610 240Z", "#3fae6a") + circ(1690, 205, 20, "#ffe994")
    fence = "".join(rect(x, 520, 14, 56, "#ffffff", 4) + path(f"M{x} 522 L{x + 7} 508 L{x + 14} 522Z", "#ffffff") for x in list(range(0, 520, 34)) + list(range(1680, 2200, 34))) + rect(0, 538, 520, 8, "#ffffff") + rect(1680, 538, 520, 8, "#ffffff")
    hop = "".join(rect(700 + i * 78, 606, 70, 30, "none", 2, 'stroke="#ffffff" stroke-width="5"') for i in range(6))
    place("escuela", [(s + sun(300, 120) + clouds([(620, 110, 1), (1500, 90, 1.1), (2000, 150, .9)]), 0.05, d), (hills("#a9dcb4", 470, 90, 6, 21), 0.2), (tree(300, 560, 1.2, "#4fa766", "#6dbf7f") + tree(1900, 560, 1.3, "#4fa766", "#6dbf7f"), 0.6), (b + flag, 0.85), (ground("#6bbf59", "#cfc6bb", 8, flowers=14) + fence + hop, 1.0)], [(bushes((40, 2000)), 1.35)])


def parque():
    s, d = sky("#8bd3f7", "#f3f7df")
    slide = rect(1240, 300, 14, 280, "#8a5a34") + rect(1330, 300, 14, 280, "#8a5a34") + "".join(rect(1250, y, 84, 10, "#a8744a") for y in range(340, 570, 42)) + rect(1226, 288, 132, 18, "#e0564a", 6) + path("M1226 296 C1150 320 1120 520 1010 560 L980 560 L982 584 L1030 584 C1150 560 1190 380 1240 330Z", "#f2a93b")
    swing = path("M1540 580 L1600 270 L1660 580 M1860 580 L1920 270 L1980 580", "none", 'stroke="#3a7fc4" stroke-width="16" stroke-linecap="round" stroke-linejoin="round"') + rect(1590, 262, 340, 16, "#3a7fc4", 8) + "".join(rect(x, 276, 5, 220, "#6a6a6a") + rect(x + 70, 276, 5, 220, "#6a6a6a") + rect(x - 6, 494, 88, 14, "#e0564a", 5) for x in (1650, 1800))
    bench = rect(240, 470, 260, 16, "#a8744a", 5) + rect(240, 500, 260, 16, "#a8744a", 5) + rect(240, 530, 260, 20, "#8a5a34", 5) + rect(262, 466, 12, 120, "#4a4a4a") + rect(466, 466, 12, 120, "#4a4a4a")
    fountain = ell(740, 566, 130, 26, "#c9c4bd") + ell(740, 556, 112, 18, "#7cc8f2") + rect(724, 470, 32, 90, "#b5afa7") + ell(740, 470, 56, 12, "#c9c4bd") + "".join(path(f"M740 464 q{dx} -60 {dx * 1.6:.0f} 84", "none", 'stroke="#bfe6fb" stroke-width="6" stroke-linecap="round"') for dx in (-34, -14, 14, 34))
    place("parque", [(s + sun(1500, 110) + clouds([(250, 120, 1.1), (900, 90, .9), (1900, 160, 1)]), 0.05, d), (hills("#bfe3b0", 470, 80, 7, 31), 0.2), ("".join(tree(x, 545, sc, a, b) for x, sc, a, b in [(120, 1.2, "#e89a4a", "#f3b672"), (480, 1.0, "#5fb277", "#7cc98f"), (1000, 1.1, "#e2c14d", "#f2da7c"), (1480, 1.0, "#5fb277", "#7cc98f"), (2080, 1.25, "#e89a4a", "#f3b672")]), 0.5), (fountain + bench + slide + swing, 0.95), (ground("#84cc63", "#f0ddb0", 9), 1.0)], [(bushes((700, 1750), "#3f8f4f", "#52a862"), 1.35)])


def montana():
    s, d = sky("#5fb4ea", "#e6f3fb")

    def peak(x, y, w, h, c, snow):
        return path(f"M{x - w} {y} L{x} {y - h} L{x + w} {y}Z", c) + path(f"M{x - w * .28:.0f} {y - h * .72:.0f} L{x} {y - h} L{x + w * .28:.0f} {y - h * .72:.0f} L{x + w * .12:.0f} {y - h * .62:.0f} L{x} {y - h * .72:.0f} L{x - w * .14:.0f} {y - h * .6:.0f}Z", snow)
    far = "".join(peak(x, 520, w, h, "#9db4cf", "#ffffff") for x, w, h in [(250, 300, 330), (700, 340, 400), (1200, 320, 350), (1700, 380, 430), (2100, 300, 340)])
    mid = "".join(peak(x, 560, w, h, "#7388a8", "#eef4fb") for x, w, h in [(480, 260, 260), (960, 300, 300), (1480, 280, 270), (1950, 300, 290)])
    pines = "".join(pine(x, 566, sc, "#2f7a55", "#3b8f66") for x, sc in [(80, 1.3), (260, 1), (560, 1.1), (1150, 1.2), (1390, .95), (1700, 1.3), (2050, 1.1)])
    rocks = "".join(path(f"M{x} {y} L{x + 18 * k} {y - 44 * k} L{x + 62 * k} {y - 56 * k} L{x + 104 * k} {y - 26 * k} L{x + 118 * k} {y}Z", "#8f8a86") + path(f"M{x + 18 * k} {y - 44 * k} L{x + 62 * k} {y - 56 * k} L{x + 70 * k} {y - 22 * k} L{x + 30 * k} {y - 14 * k}Z", "#aaa5a0") for x, y, k in [(300, 586, 1), (860, 580, .8), (1580, 588, 1.2), (2000, 580, .9)])
    sign = rect(1080, 470, 14, 110, "#7a5238") + path("M1030 440 L1150 440 L1176 466 L1150 492 L1030 492Z", "#a8744a")
    place("montana", [(s + sun(1350, 110) + clouds([(420, 150, 1.1), (1050, 90, .8), (1850, 140, 1.2)]), 0.05, d), (far, 0.15), (mid + hills("#8fbf8f", 560, 50, 7, 41), 0.35), (pines, 0.7), (ground("#8abf6a", "#c7b299", 10, flowers=18) + rocks + sign, 1.0)], [("".join(path(f"M{x} 720 L{x + 30} 660 L{x + 110} 650 L{x + 170} 690 L{x + 190} 720Z", "#77726e") for x in (40, 1650)), 1.35)])


def cielo():
    s, d = sky("#4ea3ea", "#cfeefe")
    rainbow = "".join(path(f"M{300 + i * 18} 620 A{800 - i * 18} {560 - i * 18} 0 0 1 {1900 - i * 18} 620", "none", f'stroke="{c}" stroke-width="18" opacity="0.55"') for i, c in enumerate(["#f26d6d", "#f5a94e", "#f7e26b", "#7fd08a", "#6bb3f0", "#a98be8"]))
    far = clouds([(150, 260, 1.2), (560, 180, .8), (980, 300, 1.1), (1380, 210, .9), (1780, 290, 1.3), (2080, 160, .8)], .8)
    floor = "".join(circ(x, 690 + (x * 7 % 40), 92 + (x * 3 % 40), "#ffffff") for x in range(0, W, 95)) + rect(0, 660, W, 60, "#ffffff") + "".join(circ(x, 700, 60, "#e3f1fb") for x in range(40, W, 150))
    front = "".join(circ(x, 760, 80, "#ffffff", 'opacity="0.9"') for x in range(0, W, 120))
    place("cielo", [(s + sun(420, 130), 0.05, d), (rainbow, 0.15), (far, 0.35), (clouds([(380, 430, 1.4), (1100, 400, 1.2), (1660, 450, 1.5)], .9), 0.7), (floor, 1.0)], [(front, 1.3)])


def prop(key, motion, files):
    P = MEDIA / "props" / key
    if (P / "prop.json").exists() and key not in REDO:
        return
    for f, (body, w, h) in files.items():
        if not (P / f).exists() or key in REDO:
            svg(P / f, body, w, h)
    P.mkdir(parents=True, exist_ok=True)
    (P / "prop.json").write_text(json.dumps({"motion": motion, "license": "propia"}), encoding="utf-8")
    print("objeto", key)


def props():
    st = 'stroke="#4a2740" stroke-width="3.5" stroke-linejoin="round"'
    prop("mariposa", "flap", {})
    prop("pajaro", "flap", {"ala.svg": ('<path d="M4 44 C16 10 46 2 58 14 C54 30 44 44 6 52Z" fill="#5aa9ea" stroke="#235b94" stroke-width="3.5"/>', 62, 62),
                            "cuerpo.svg": ('<path d="M40 14 C62 14 70 34 66 52 C62 78 46 92 38 92 C28 92 14 76 12 52 C10 32 20 14 40 14Z" fill="#7cc1f5" stroke="#235b94" stroke-width="3.5"/><ellipse cx="39" cy="62" rx="16" ry="20" fill="#fff3d6"/><circle cx="31" cy="34" r="5" fill="#1c0d06"/><circle cx="49" cy="34" r="5" fill="#1c0d06"/><path d="M34 42 L46 42 L40 52Z" fill="#f2a93b"/>', 80, 100)})
    prop("pelota", "bounce", {"cuerpo.svg": (f'<circle cx="45" cy="45" r="40" fill="#ffffff" {st}/><path d="M45 45 L45 5 A40 40 0 0 1 80 25Z" fill="#e0564a"/><path d="M45 45 L80 65 A40 40 0 0 1 45 85Z" fill="#3a7fc4"/><path d="M45 45 L10 65 A40 40 0 0 1 10 25Z" fill="#f2c94c"/><circle cx="45" cy="45" r="40" fill="none" {st}/>', 90, 90)})
    prop("globo", "float", {"cuerpo.svg": ('<path d="M50 130 Q44 160 54 190 T48 238" fill="none" stroke="#6a6a6a" stroke-width="3"/><path d="M50 6 C82 6 94 34 92 60 C90 96 64 122 50 124 C36 122 10 96 8 60 C6 34 18 6 50 6Z" fill="#e8575a" stroke="#9c2730" stroke-width="3.5"/><path d="M44 122 L56 122 L60 134 L40 134Z" fill="#9c2730"/><ellipse cx="32" cy="42" rx="10" ry="18" fill="#ffffff" opacity="0.5"/>', 100, 240)})
    prop("hoja", "sway", {"cuerpo.svg": ('<path d="M8 70 C10 24 46 4 84 8 C90 44 66 82 22 76Z" fill="#e8903a" stroke="#97501a" stroke-width="3.5"/><path d="M8 80 Q40 50 76 18" fill="none" stroke="#97501a" stroke-width="3"/>', 94, 88)})
    prop("estrella", "spin", {"cuerpo.svg": ('<path d="M50 4 L62 36 L96 38 L70 60 L80 94 L50 74 L20 94 L30 60 L4 38 L38 36Z" fill="#ffd95a" stroke="#b98412" stroke-width="3.5" stroke-linejoin="round"/>', 100, 100)})
    prop("cometa", "sway", {"cuerpo.svg": ('<path d="M60 130 Q40 160 64 184 T58 236" fill="none" stroke="#6a6a6a" stroke-width="3"/><path d="M60 4 L112 60 L60 132 L8 60Z" fill="#f2a93b" stroke="#8a4f0c" stroke-width="3.5" stroke-linejoin="round"/><path d="M60 4 L60 132 M8 60 L112 60" stroke="#8a4f0c" stroke-width="3"/><path d="M60 4 L112 60 L60 60Z" fill="#e0564a"/><path d="M60 132 L8 60 L60 60Z" fill="#3a7fc4"/>', 120, 240)})
    prop("pez", "bounce", {"cuerpo.svg": ('<path d="M96 40 L128 14 L124 66Z" fill="#f08a3c" stroke="#96480f" stroke-width="3.5" stroke-linejoin="round"/><ellipse cx="58" cy="40" rx="50" ry="30" fill="#ffa95c" stroke="#96480f" stroke-width="3.5"/><circle cx="30" cy="34" r="7" fill="#ffffff"/><circle cx="29" cy="34" r="4" fill="#1c0d06"/><path d="M56 24 Q68 40 56 56" fill="none" stroke="#96480f" stroke-width="3"/>', 134, 80)})
    prop("regalo", "still", {"cuerpo.svg": ('<rect x="10" y="44" width="100" height="76" rx="6" fill="#8e5bd0" stroke="#4b2a7c" stroke-width="4"/><rect x="4" y="30" width="112" height="24" rx="6" fill="#a97ce2" stroke="#4b2a7c" stroke-width="4"/><rect x="50" y="30" width="20" height="90" fill="#ffd95a"/><path d="M60 30 C30 0 14 22 60 30 C106 22 90 0 60 30Z" fill="#ffd95a" stroke="#b98412" stroke-width="3"/>', 120, 124)})


def ambiences():
    f = MEDIA / "environments/ambientes.json"
    f.write_text(json.dumps({
        "dia": {"tint": "#ffffff"},
        "atardecer": {"tint": "#ffc59a"},
        "noche": {"tint": "#6f7fc4", "particles": "stars"},
        "lluvia": {"tint": "#a9bccb", "particles": "rain"},
        "nieve": {"tint": "#dbe8f7", "particles": "snow"}}, indent=1), encoding="utf-8")


if __name__ == "__main__":
    for make in (casa, lago, escuela, parque, montana, cielo, props, ambiences):
        make()
