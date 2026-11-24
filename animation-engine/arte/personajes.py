"""Dibuja personajes nuevos con el mismo esqueleto de Bruno. No toca los que ya existen.

Uso: python animation-engine/arte/personajes.py [--rehacer luna]
"""
import json
import sys
from pathlib import Path

CHARS = Path(__file__).resolve().parents[2] / "media/characters"


def svg(p, w, h, body, defs=""):
    p.parent.mkdir(parents=True, exist_ok=True)
    d = f"<defs>{defs}</defs>\n" if defs else ""
    p.write_text(f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">\n{d}{body}\n</svg>\n', encoding="utf-8")


def rad(i, stops, cx=.4, cy=.3, r=.8):
    return f'<radialGradient id="{i}" cx="{cx}" cy="{cy}" r="{r}">' + "".join(f'<stop offset="{o}" stop-color="{c}"/>' for o, c in stops) + "</radialGradient>"


def clip(path, i="c"):
    return f'<clipPath id="{i}"><path d="{path}"/></clipPath>'


HEAD = "M160 18 C232 16 290 62 295 126 C310 148 308 178 288 188 C293 202 282 217 262 217 C240 246 202 259 160 259 C118 259 80 246 58 217 C38 217 27 202 32 188 C12 178 10 148 25 126 C30 62 88 16 160 18Z"
BODY = "M120 12 C160 12 182 50 198 100 C218 160 196 222 120 222 C44 222 22 160 42 100 C58 50 80 12 120 12Z"

CAST = {
    "luna": dict(name="Luna", role="protagonista", personality="Inteligente, alegre y creativa", scale=0.88,
                 F="#f08a3c", D="#c4601d", LT="#ffb06a", O="#5a2a12", CR="#fff6e6", CRD="#f3dcc0", paw="#6b3a22", iris=("#8fd07a", "#3f8f4a", "#1d5a2c"),
                 scarf=("#8e5bd0", "#55308c", "#b892ea"), ears="zorro", tail="zorro", marks="zorro", tooth=0, lashes=True, freckles=False,
                 voice=dict(reference="dora", exaggeration=0.8, seed=11, pitch=1.10)),
    "tito": dict(name="Tito", role="secundario", personality="Tímido, goloso y muy rápido", scale=0.78,
                 F="#d9d3cf", D="#aaa19c", LT="#f4f0ed", O="#4a4340", CR="#ffffff", CRD="#e6dfda", paw="#f2b3b3", iris=("#8cc6f5", "#3a7fc4", "#1d4c80"),
                 scarf=("#e0564a", "#93291f", "#f58f86"), ears="conejo", tail="conejo", marks="conejo", tooth=2, lashes=False, freckles=True,
                 voice=dict(reference="tito", exaggeration=0.85, seed=23, pitch=1.14)),
    "nico": dict(name="Nico", role="secundario", personality="Travieso, inventor y despistado", scale=0.9,
                 F="#8a8f98", D="#62676f", LT="#b4b9c1", O="#2f3238", CR="#f3f1ec", CRD="#d8d4cc", paw="#4b4f57", iris=("#f3c763", "#b9821f", "#6e4a0e"),
                 scarf=("#3fae6a", "#1f6a3c", "#83d6a2"), ears="mapache", tail="mapache", marks="mapache", tooth=1, lashes=False, freckles=False,
                 voice=dict(reference="alex", exaggeration=0.9, seed=31, pitch=1.04)),
}


def ear(c, kind):
    st = f'stroke="{c["O"]}" stroke-width="5" stroke-linejoin="round" stroke-linecap="round"'
    if kind == "zorro":
        out, inn = "M14 112 C8 70 18 28 40 8 C66 24 92 62 100 108 C76 120 40 122 14 112Z", "M32 100 C28 70 34 46 44 32 C60 46 74 70 78 98 C64 104 46 104 32 100Z"
        return 112, 124, f'<path d="{out}" fill="{c["F"]}" {st}/><path d="M40 8 C66 24 78 40 86 62 C70 52 40 46 22 54 C26 34 32 18 40 8Z" fill="{c["O"]}"/><path d="{inn}" fill="{c["CR"]}"/>'
    if kind == "conejo":
        out = "M36 206 C10 160 4 60 26 18 C34 4 54 4 62 18 C86 62 80 160 60 206Z"
        return 92, 214, f'<path d="{out}" fill="{c["F"]}" {st}/><path d="M42 186 C28 140 26 74 38 38 C44 28 50 30 54 38 C66 74 62 140 54 186Z" fill="#f2b3b3"/>'
    out = "M54 8 C84 8 100 34 98 60 C96 84 78 100 54 100 C30 100 12 84 10 60 C8 34 24 8 54 8Z"
    return 108, 108, f'<path d="{out}" fill="{c["D"]}" {st}/><path d="M54 30 C72 30 80 46 78 62 C76 78 68 88 54 88 C40 88 32 78 30 62 C28 46 36 30 54 30Z" fill="{c["CR"]}"/>'


def marks(c, kind):
    if kind == "zorro":  # mejillas blancas
        return f'<path d="M160 262 C90 262 20 230 10 150 C70 140 104 170 130 178 C150 150 190 150 206 178 C236 168 262 140 312 150 C304 232 230 262 160 262Z" fill="{c["CR"]}"/>'
    if kind == "mapache":  # antifaz
        return (f'<path d="M20 148 C40 96 100 84 142 112 C160 124 176 124 194 112 C226 86 286 96 304 146 C284 186 222 184 196 158 C178 146 150 146 136 160 C104 188 40 190 20 148Z" fill="{c["O"]}" opacity="0.85"/>'
                f'<path d="M150 20 C146 60 148 84 168 100 C186 84 188 60 180 20Z" fill="{c["D"]}" opacity="0.7"/><path d="M40 108 C70 70 110 70 138 92 C104 88 72 92 40 108Z M182 92 C214 70 254 72 286 108 C252 90 220 88 182 92Z" fill="{c["CR"]}"/>')
    return f'<ellipse cx="160" cy="70" rx="40" ry="52" fill="{c["LT"]}" opacity="0.8"/>'


def tail(c, kind):
    st = f'stroke="{c["O"]}" stroke-width="5" stroke-linejoin="round" stroke-linecap="round"'
    if kind == "zorro":
        p = "M150 128 C120 150 60 150 28 104 C2 66 10 24 34 8 C40 40 60 60 92 70 C120 78 144 96 150 128Z"
        return 160, 156, f'<path d="{p}" fill="{c["F"]}"/><g clip-path="url(#c)"><path d="M0 0 L70 0 L64 52 L40 66 L30 88 L0 80Z" fill="{c["CR"]}"/></g><path d="{p}" fill="none" {st}/>', clip(p), (-72, -42)
    if kind == "mapache":
        p = "M146 120 C112 146 56 140 28 100 C6 66 14 26 40 14 C62 8 76 24 74 46 C76 70 124 78 146 120Z"
        rings = "".join(f'<path d="M{x} {y} L{x + 40} {y - 34} L{x + 58} {y - 16} L{x + 18} {y + 22}Z" fill="{c["O"]}"/>' for x, y in [(2, 40), (8, 84), (40, 122)])
        return 156, 146, f'<path d="{p}" fill="{c["F"]}"/><g clip-path="url(#c)">{rings}</g><path d="{p}" fill="none" {st}/>', clip(p), (-70, -38)
    return 66, 66, f'<circle cx="33" cy="33" r="28" fill="{c["CR"]}" {st}/><path d="M20 24 Q28 14 38 18 M18 38 Q24 32 30 36" fill="none" stroke="{c["CRD"]}" stroke-width="5" stroke-linecap="round"/>', "", (-8, 0)


def draw(key, c):
    B = CHARS / key
    F, D, LT, O, CR, CRD = (c[k] for k in ("F", "D", "LT", "O", "CR", "CRD"))
    st = f'stroke="{O}" stroke-width="5" stroke-linejoin="round" stroke-linecap="round"'
    cheeks = '<ellipse cx="62" cy="184" rx="24" ry="15" fill="#f0806a" opacity="0.45"/><ellipse cx="276" cy="182" rx="16" ry="14" fill="#f0806a" opacity="0.45"/>'
    svg(B / "cabeza.svg", 320, 280, f'<path d="{HEAD}" fill="{F}"/><g clip-path="url(#c)">{marks(c, c["marks"])}'
        f'<path d="M0 280 L0 120 C20 214 90 248 170 244 C240 240 290 204 320 120 L320 280Z" fill="{D}" opacity="0.30"/><ellipse cx="128" cy="48" rx="66" ry="24" fill="{LT}" opacity="0.65"/></g>{cheeks}<path d="{HEAD}" fill="none" {st}/>', clip(HEAD))
    ew, eh, e = ear(c, c["ears"])
    svg(B / "oreja.svg", ew, eh, e)
    svg(B / "mechon.svg", 90, 70, f'<path d="M30 66 C20 52 20 34 34 18 C36 30 40 38 46 42 C48 28 54 16 68 10 C64 26 68 44 62 66Z" fill="{F}" {st}/>')
    dots = "".join(f'<circle cx="{x}" cy="{y}" r="3" fill="{D}" opacity="0.7"/>' for x, y in [(34, 62), (44, 74), (31, 78), (140, 62), (132, 74), (145, 78)]) if c["freckles"] else ""
    svg(B / "hocico.svg", 176, 130, f'<path d="M88 8 C136 8 168 34 168 70 C168 104 134 122 88 122 C42 122 8 104 8 70 C8 34 40 8 88 8Z" fill="url(#h)"/>{dots}', rad("h", [(0, "#ffffff"), (.65, CR), (1, CRD)]))
    nose = "#e58a94" if key == "tito" else "#2a150c"
    svg(B / "nariz.svg", 70, 52, f'<path d="M35 6 C56 6 64 14 62 24 C58 36 46 46 35 46 C24 46 12 36 8 24 C6 14 14 6 35 6Z" fill="{nose}" stroke="{O}" stroke-width="3"/><ellipse cx="26" cy="16" rx="9" ry="5" fill="#ffffff" opacity="0.75"/>')
    svg(B / "ceja.svg", 64, 28, f'<path d="M6 20 Q28 0 58 12 Q34 10 12 25Z" fill="{O}" stroke="{O}" stroke-width="5" stroke-linejoin="round"/>')

    def white(rx, ry):
        lash = f'<path d="M12 34 L2 24 M18 26 L10 14" stroke="{O}" stroke-width="5" stroke-linecap="round"/>' if c["lashes"] else ""
        return (f'<ellipse cx="42" cy="50" rx="{rx}" ry="{ry}" fill="#ffffff" stroke="{O}" stroke-width="4"/>'
                f'<path d="M{42 - rx} {50 - ry * .25:.0f} Q42 {50 - ry * 1.55:.0f} {42 + rx} {50 - ry * .25:.0f}" fill="none" stroke="{O}" stroke-width="7" stroke-linecap="round"/>{lash}')

    for n, (rx, ry) in dict(normal=(29, 37), surprised=(34, 44), sad=(28, 35), picaro=(29, 35), scared=(33, 42), angry=(28, 33)).items():
        svg(B / f"ojos/{n}.svg", 84, 100, white(rx, ry))
    shut = CR if c["marks"] == "mapache" else O
    svg(B / "ojos/happy.svg", 84, 100, f'<path d="M12 60 Q42 10 72 60" fill="none" stroke="{shut}" stroke-width="10" stroke-linecap="round"/>')
    for n in ("closed", "sleepy"):
        svg(B / f"ojos/{n}.svg", 84, 100, f'<path d="M12 52 Q42 76 72 52" fill="none" stroke="{shut}" stroke-width="9" stroke-linecap="round"/>')
    lid = O if c["marks"] == "mapache" else F
    svg(B / "ojos/parpado-sad.svg", 84, 100, f'<path d="M8 42 Q30 4 76 26 L74 6 L8 6Z" fill="{lid}"/><path d="M9 43 Q32 26 75 27" fill="none" stroke="{O}" stroke-width="6" stroke-linecap="round"/>')
    svg(B / "ojos/parpado-picaro.svg", 84, 100, f'<path d="M7 44 L77 44 L77 6 L7 6Z" fill="{lid}"/><path d="M11 45 Q42 38 73 45" fill="none" stroke="{O}" stroke-width="7" stroke-linecap="round"/>')
    svg(B / "ojos/parpado-angry.svg", 84, 100, f'<path d="M8 22 L76 46 L76 4 L8 4Z" fill="{lid}"/><path d="M8 24 L76 47" fill="none" stroke="{O}" stroke-width="7" stroke-linecap="round"/>')
    svg(B / "ojos/iris.svg", 52, 60, f'<ellipse cx="26" cy="30" rx="20" ry="24" fill="url(#i)" stroke="{O}" stroke-width="3"/><ellipse cx="26" cy="31" rx="11.5" ry="14.5" fill="#1c0d06"/><ellipse cx="33" cy="20" rx="7.5" ry="8.5" fill="#ffffff"/><circle cx="19" cy="42" r="4" fill="#ffffff" opacity="0.85"/>',
        rad("i", [(0, c["iris"][0]), (.55, c["iris"][1]), (1, c["iris"][2])], .5, .75, .7))
    mouths(B, O, c["tooth"])
    p = BODY
    belly = "M128 84 L140 72 L146 84 L160 78 L161 92 C186 108 192 132 188 156 C182 190 160 206 128 206 C94 206 72 190 68 156 C64 124 80 100 104 88 L106 76 L118 84Z"
    svg(B / "cuerpo.svg", 240, 236, f'<path d="{p}" fill="{F}"/><g clip-path="url(#c)"><path d="M0 240 L0 150 C40 220 150 232 190 150 C204 120 196 80 180 40 L240 40 L240 240Z" fill="{D}" opacity="0.5"/></g><path d="{belly}" fill="url(#p)"/><path d="{p}" fill="none" {st}/>',
        clip(p) + rad("p", [(0, "#ffffff"), (.7, CR), (1, CRD)]))
    tw, th, t, td, off = tail(c, c["tail"])
    svg(B / "cola.svg", tw, th, t, td)
    svg(B / "brazo.svg", 64, 104, f'<path d="M32 5 C54 5 58 26 56 52 C54 80 48 99 32 99 C16 99 10 80 8 52 C6 26 10 5 32 5Z" fill="{F}" {st}/>')
    hand = c["O"] if c["tail"] == "zorro" else F
    arm = "M48 5 C66 5 70 26 68 50 C80 52 88 64 82 72 C86 82 84 92 78 98 C76 112 64 120 50 119 C32 121 18 112 18 94 C14 84 20 66 28 50 C26 26 30 5 48 5Z"
    svg(B / "antebrazo.svg", 96, 128, f'<path d="{arm}" fill="{F}"/><g clip-path="url(#c)"><path d="M0 62 Q48 76 96 58 L96 128 L0 128Z" fill="{hand}"/></g><path d="{arm}" fill="none" {st}/><path d="M38 117 L38 106 M54 119 L54 107 M68 113 L65 103" fill="none" stroke="{O if hand == F else LT}" stroke-width="4.5" stroke-linecap="round"/><ellipse cx="50" cy="84" rx="16" ry="12" fill="{c["paw"]}"/>', clip(arm))
    svg(B / "muslo.svg", 84, 92, f'<path d="M42 5 C68 5 78 26 75 48 C72 72 62 87 42 87 C22 87 12 72 9 48 C6 26 16 5 42 5Z" fill="{F}" {st}/>')
    leg = "M46 5 C68 5 72 22 72 36 C102 34 126 48 125 68 C124 82 110 87 92 87 L34 87 C14 87 8 74 11 56 C13 34 20 5 46 5Z"
    svg(B / "pierna.svg", 132, 92, f'<path d="{leg}" fill="{F}"/><g clip-path="url(#c)"><path d="M0 44 Q50 52 132 34 L132 92 L0 92Z" fill="{hand}"/></g><path d="{leg}" fill="none" {st}/><path d="M92 85 Q88 74 94 66 M109 82 Q105 72 110 65" fill="none" stroke="{O if hand == F else LT}" stroke-width="4.5" stroke-linecap="round"/>', clip(leg))
    svg(B / "codo.svg", 48, 48, f'<circle cx="24" cy="24" r="20.5" fill="{F}"/>')
    svg(B / "rodilla.svg", 60, 60, f'<circle cx="30" cy="30" r="24" fill="{F}"/>')
    S, SD, SH = c["scarf"]
    sb = f'stroke="{SD}" stroke-width="5" stroke-linejoin="round" stroke-linecap="round"'
    svg(B / "panuelo.svg", 220, 92, f'<path d="M12 18 Q110 52 208 18 Q218 34 210 52 Q110 98 10 52 Q2 34 12 18Z" fill="{S}" {sb}/><path d="M24 30 Q110 64 196 30" fill="none" stroke="{SH}" stroke-width="6" stroke-linecap="round"/>')
    svg(B / "panuelo-punta.svg", 130, 110, f'<path d="M10 8 Q65 26 120 8 Q100 60 68 104 Q30 60 10 8Z" fill="{S}" {sb}/>' + "".join(f'<circle cx="{x}" cy="{y}" r="6" fill="{SH}"/>' for x, y in [(44, 34), (84, 34), (65, 56), (66, 80)]))
    svg(B / "panuelo-nudo.svg", 80, 90, f'<path d="M50 20 C30 34 14 56 8 82 C22 80 32 72 38 62 C38 74 42 82 48 86 C58 68 62 44 60 22Z" fill="{S}" {sb}/><circle cx="56" cy="20" r="15" fill="{S}" {sb}/>')
    svg(B / "sombra.svg", 260, 44, '<ellipse cx="130" cy="22" rx="120" ry="18" fill="#1d3b1f" opacity="0.30"/>')
    rig = json.loads((CHARS / "bruno/rig.json").read_text(encoding="utf-8"))
    rig["defaultScale"] = c["scale"]
    rig["walkSpeed"] = round(195 * c["scale"] / 0.92)
    place = {"zorro": ((-6, -40), (6, -40)), "conejo": ((14, -92), (-14, -92)), "mapache": ((-6, -20), (6, -20))}[c["ears"]]
    for s in rig["sprites"]:
        if s[0] == "ear_l":
            s[3], s[4] = place[0]
        elif s[0] == "ear_r":
            s[3], s[4] = place[1]
        elif s[0] == "tail":
            s[3], s[4] = off
    if c["ears"] == "conejo":
        for b in rig["bones"]:
            if b[0] == "ear_l":
                b[2:] = [-58, -206]
            elif b[0] == "ear_r":
                b[2:] = [70, -204]
    if c["tail"] == "mapache" or c["tail"] == "zorro":
        rig["dangles"]["tail"] = 0.6
    (B / "rig.json").write_text(json.dumps(rig, indent=1), encoding="utf-8")
    v = c["voice"]
    sheet = dict(id=key, name=c["name"], role=c["role"], personality=c["personality"], designVersion=1, designStatus="borrador", license="propia",
                 colorPalette=[F, D, CR, S], voice=dict(engine="chatterbox", reference="voz-referencia.wav", exaggeration=v["exaggeration"], cfg=0.4, seed=v["seed"], pitch=v["pitch"],
                                                        license="MIT (Chatterbox) + referencia sintetica Kokoro (Apache-2.0)"))
    (B / "ficha.json").write_text(json.dumps(sheet, indent=2, ensure_ascii=False), encoding="utf-8")


def mouths(B, O, tooth):
    I, T = "#6b1726", "#f07f86"
    ln = f'stroke="{O}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"'

    def up(y):
        return f'<path d="M20 {y} Q70 {y + 9} 120 {y} L120 {y - 30} L20 {y - 30}Z" fill="#ffffff"/>'

    def tong(cy, rx=26, ry=15):
        return f'<ellipse cx="70" cy="{cy}" rx="{rx}" ry="{ry}" fill="{T}"/>'

    shapes = {"B": ("M36 34 Q70 42 104 34 Q100 60 70 60 Q40 60 36 34Z", up(46)), "C": ("M32 30 Q70 38 108 30 Q106 76 70 78 Q34 76 32 30Z", tong(74) + up(41)),
              "D": ("M28 26 Q70 34 112 26 Q116 96 70 98 Q24 96 28 26Z", tong(92, 30, 20) + up(40)),
              "E": ("M70 28 C92 28 102 40 100 54 C98 70 86 78 70 78 C54 78 42 70 40 54 C38 40 48 28 70 28Z", tong(76, 18, 10)),
              "F": ("M70 34 C84 34 88 44 87 53 C86 64 80 68 70 68 C60 68 54 64 53 53 C52 44 56 34 70 34Z", ""), "G": ("M42 38 Q70 45 98 38 Q94 58 70 58 Q46 58 42 38Z", up(52)),
              "H": ("M36 32 Q70 40 104 32 Q102 72 70 74 Q38 72 36 32Z", tong(58, 18, 12) + up(42)), "happy": ("M22 24 Q70 42 118 24 Q118 86 70 88 Q22 86 22 24Z", tong(84, 30, 18) + up(44)),
              "surprised": ("M70 30 C86 30 92 46 91 60 C90 78 82 86 70 86 C58 86 50 78 49 60 C48 46 54 30 70 30Z", tong(84, 16, 9)),
              "sleepy": ("M70 40 C80 40 84 48 83 54 C82 62 78 66 70 66 C62 66 58 62 57 54 C56 48 60 40 70 40Z", "")}
    for k, (p, inner) in shapes.items():
        svg(B / f"bocas/{k}.svg", 140, 108, f'<path d="{p}" fill="{I}"/><g clip-path="url(#m)">{inner}</g><path d="{p}" fill="none" {ln}/>', clip(p, "m"))
    teeth = {0: "", 1: f'<path d="M64 51 L66 61 L76 60 L77 50Z" fill="#ffffff" stroke="{O}" stroke-width="3" stroke-linejoin="round"/>',
             2: f'<path d="M57 51 L58 68 L70 69 L70 52Z M70 52 L70 69 L82 68 L83 50Z" fill="#ffffff" stroke="{O}" stroke-width="3" stroke-linejoin="round"/>'}[tooth]
    svg(B / "bocas/X.svg", 140, 108, f'<path d="M30 34 Q70 70 112 30" fill="none" {ln}/><path d="M106 24 Q114 28 117 36" fill="none" {ln}/>{teeth}')
    svg(B / "bocas/A.svg", 140, 108, f'<path d="M40 46 Q70 54 100 46" fill="none" {ln}/>')
    svg(B / "bocas/sad.svg", 140, 108, f'<path d="M42 62 Q70 34 98 62" fill="none" {ln}/>')
    svg(B / "bocas/picaro.svg", 140, 108, f'<path d="M36 50 Q78 72 114 30" fill="none" {ln}/><path d="M107 25 Q116 27 119 36" fill="none" {ln}/>')
    svg(B / "bocas/angry.svg", 140, 108, f'<path d="M40 58 Q56 44 70 54 Q86 62 100 50" fill="none" {ln}/>')
    svg(B / "bocas/scared.svg", 140, 108, f'<path d="M38 48 Q46 38 54 48 T70 48 T86 48 T102 48" fill="none" {ln}/>')


def more_faces_for_bruno():
    """Caras nuevas para Bruno, sin tocar ninguno de sus archivos anteriores."""
    B, O, F = CHARS / "bruno", "#4f2d18", "#b97a42"
    ln = f'stroke="{O}" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"'
    if (B / "ojos/scared.svg").exists():
        return
    for n, (rx, ry) in dict(scared=(33, 42), angry=(28, 33)).items():
        svg(B / f"ojos/{n}.svg", 84, 100, f'<ellipse cx="42" cy="50" rx="{rx}" ry="{ry}" fill="#ffffff" stroke="{O}" stroke-width="4"/><path d="M{42 - rx} {50 - ry * .25:.0f} Q42 {50 - ry * 1.55:.0f} {42 + rx} {50 - ry * .25:.0f}" fill="none" stroke="{O}" stroke-width="7" stroke-linecap="round"/>')
    svg(B / "ojos/sleepy.svg", 84, 100, f'<path d="M12 52 Q42 76 72 52" fill="none" stroke="{O}" stroke-width="9" stroke-linecap="round"/>')
    svg(B / "ojos/parpado-angry.svg", 84, 100, f'<path d="M8 22 L76 46 L76 4 L8 4Z" fill="{F}"/><path d="M8 24 L76 47" fill="none" stroke="{O}" stroke-width="7" stroke-linecap="round"/>')
    svg(B / "bocas/angry.svg", 140, 108, f'<path d="M40 58 Q56 44 70 54 Q86 62 100 50" fill="none" {ln}/>')
    svg(B / "bocas/scared.svg", 140, 108, f'<path d="M38 48 Q46 38 54 48 T70 48 T86 48 T102 48" fill="none" {ln}/>')
    svg(B / "bocas/sleepy.svg", 140, 108, f'<path d="M70 40 C80 40 84 48 83 54 C82 62 78 66 70 66 C62 66 58 62 57 54 C56 48 60 40 70 40Z" fill="#6b1726" {ln}/>')


if __name__ == "__main__":
    redo = sys.argv[sys.argv.index("--rehacer") + 1:] if "--rehacer" in sys.argv else []
    for name, conf in CAST.items():
        if name in redo or not (CHARS / name / "rig.json").exists():
            draw(name, conf)
            print("dibujado", name)
    more_faces_for_bruno()
