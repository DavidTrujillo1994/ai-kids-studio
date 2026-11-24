"""Agrega movimientos a media/characters/animaciones.json sin modificar los que ya están.

Valores: [segundo, grados]; hip_y en píxeles (negativo = arriba). "gait" marca los que sirven para
desplazarse y su velocidad relativa al caminar; "lift" los despega del suelo.
"""
import json
import sys
from pathlib import Path

FILE = Path(__file__).resolve().parents[2] / "media/characters/animaciones.json"


def hold(v, t):
    return [[0, v], [t, v]]


def wave(a, b, t):
    return [[0, a], [t / 2, b], [t, a]]


NEW = {
    # desplazamientos
    "run": dict(length=0.48, loop=True, gait=1.9, tracks=dict(
        leg_near=wave(-42, 38, .48), shin_near=[[0, 8], [.12, 10], [.24, 40], [.36, 80], [.48, 8]], leg_far=wave(38, -42, .48), shin_far=[[0, 40], [.12, 80], [.24, 8], [.36, 10], [.48, 40]],
        arm_near=wave(48, -48, .48), fore_near=hold(-72, .48), arm_far=wave(-48, 48, .48), fore_far=hold(-72, .48), torso=hold(13, .48), head=hold(-9, .48), hip_y=[[0, 6], [.12, -12], [.24, 6], [.36, -12], [.48, 6]])),
    "hop": dict(length=0.62, loop=True, gait=1.15, tracks=dict(
        hip_y=[[0, 14], [.12, 2], [.31, -62], [.5, 2], [.62, 14]], leg_near=[[0, -34], [.15, 10], [.31, -22], [.62, -34]], shin_near=[[0, 54], [.15, 6], [.31, 44], [.62, 54]],
        leg_far=[[0, -34], [.15, 10], [.31, -16], [.62, -34]], shin_far=[[0, 54], [.15, 6], [.31, 36], [.62, 54]], arm_near=[[0, 30], [.31, 62], [.62, 30]], arm_far=[[0, -30], [.31, -62], [.62, -30]], head=[[0, 4], [.31, -5], [.62, 4]])),
    "fly": dict(length=0.5, loop=True, gait=1.5, lift=150, tracks=dict(
        torso=hold(38, .5), head=hold(-26, .5), arm_near=wave(82, 112, .5), fore_near=wave(10, 30, .5), arm_far=wave(-82, -112, .5), fore_far=wave(-10, -30, .5),
        leg_near=wave(48, 56, .5), shin_near=wave(24, 12, .5), leg_far=wave(58, 46, .5), shin_far=wave(12, 24, .5), hip_y=wave(-6, 8, .5))),
    "sneak": dict(length=1.2, loop=True, gait=0.5, tracks=dict(
        leg_near=wave(-30, 14, 1.2), shin_near=[[0, 22], [.3, 24], [.6, 34], [.9, 66], [1.2, 22]], leg_far=wave(14, -30, 1.2), shin_far=[[0, 34], [.3, 66], [.6, 22], [.9, 24], [1.2, 34]],
        arm_near=hold(-44, 1.2), fore_near=hold(-88, 1.2), arm_far=hold(-58, 1.2), fore_far=hold(-80, 1.2), torso=wave(14, 18, 1.2), head=wave(-10, -4, 1.2), hip_y=wave(16, 10, 1.2))),
    "swim": dict(length=1.0, loop=True, gait=0.9, lift=-10, tracks=dict(
        torso=hold(52, 1), head=hold(-40, 1), arm_near=[[0, -150], [.5, -20], [1, -150]], fore_near=hold(-20, 1), arm_far=[[0, -20], [.5, -150], [1, -20]], fore_far=hold(-20, 1),
        leg_near=wave(60, 80, 1), shin_near=wave(20, 4, 1), leg_far=wave(80, 60, 1), shin_far=wave(4, 20, 1), hip_y=wave(30, 40, 1))),
    # emociones y acciones
    "think": dict(length=2.2, loop=True, tracks=dict(arm_far=hold(-52, 2.2), fore_far=wave(-112, -120, 2.2), arm_near=hold(-34, 2.2), fore_near=hold(-66, 2.2), head=wave(7, 2, 2.2), torso=wave(-2, 1, 2.2))),
    "sad": dict(length=2.4, loop=True, tracks=dict(head=wave(17, 20, 2.4), torso=wave(9, 11, 2.4), arm_near=wave(4, 7, 2.4), arm_far=wave(-2, 2, 2.4), fore_near=hold(-8, 2.4), fore_far=hold(-8, 2.4), hip_y=wave(5, 8, 2.4))),
    "surprise": dict(length=0.9, loop=False, tracks=dict(hip_y=[[0, 0], [.12, 10], [.28, -34], [.5, 4], [.9, 0]], arm_near=[[0, 10], [.28, 96], [.9, 70]], fore_near=[[0, 0], [.28, 40], [.9, 34]], arm_far=[[0, -10], [.28, -96], [.9, -70]], fore_far=[[0, 0], [.28, -40], [.9, -34]], torso=[[0, 0], [.28, -9], [.9, -6]], head=[[0, 0], [.28, -10], [.9, -5]])),
    "celebrate": dict(length=0.8, loop=True, tracks=dict(hip_y=[[0, 10], [.2, 0], [.4, -46], [.6, 0], [.8, 10]], arm_near=wave(146, 158, .8), fore_near=wave(-10, 20, .8), arm_far=wave(-146, -158, .8), fore_far=wave(10, -20, .8),
                      leg_near=[[0, -18], [.4, -8], [.8, -18]], shin_near=[[0, 34], [.4, 22], [.8, 34]], leg_far=[[0, -18], [.4, 6], [.8, -18]], shin_far=[[0, 34], [.4, 14], [.8, 34]], head=wave(-7, -3, .8))),
    "clap": dict(length=0.36, loop=True, tracks=dict(arm_near=wave(-58, -34, .36), fore_near=wave(-62, -44, .36), arm_far=wave(-30, -56, .36), fore_far=wave(-36, -20, .36), head=wave(-3, 2, .36), hip_y=wave(0, 4, .36))),
    "yawn": dict(length=2.0, loop=False, tracks=dict(arm_near=[[0, 8], [.7, 140], [1.4, 146], [2, 10]], fore_near=[[0, 0], [.7, 40], [1.4, 46], [2, 0]], arm_far=[[0, -8], [.7, -60], [1.4, -62], [2, -8]], fore_far=[[0, 0], [.7, -100], [1.4, -104], [2, 0]], head=[[0, 0], [.7, -14], [1.4, -15], [2, 6]], torso=[[0, 0], [.7, -7], [1.4, -8], [2, 2]])),
    "sleep": dict(length=3.0, loop=True, tracks=dict(hip_y=wave(52, 54, 3), leg_near=hold(-82, 3), shin_near=hold(14, 3), leg_far=hold(-74, 3), shin_far=hold(8, 3), torso=wave(7, 10, 3), head=wave(18, 22, 3), arm_near=hold(10, 3), fore_near=hold(-30, 3), arm_far=hold(-12, 3), fore_far=hold(-30, 3))),
    "sit": dict(length=2.4, loop=True, tracks=dict(hip_y=hold(52, 2.4), leg_near=hold(-82, 2.4), shin_near=wave(14, 26, 2.4), leg_far=hold(-74, 2.4), shin_far=wave(24, 10, 2.4), torso=wave(-4, -2, 2.4), head=wave(2, -3, 2.4), arm_near=hold(20, 2.4), arm_far=hold(-14, 2.4))),
    "shrug": dict(length=1.6, loop=True, tracks=dict(arm_near=[[0, 20], [.4, 46], [1.1, 44], [1.6, 20]], fore_near=[[0, 20], [.4, 62], [1.1, 58], [1.6, 20]], arm_far=[[0, -20], [.4, -46], [1.1, -44], [1.6, -20]], fore_far=[[0, -20], [.4, -62], [1.1, -58], [1.6, -20]], head=[[0, 0], [.4, 10], [1.1, 9], [1.6, 0]], hip_y=[[0, 0], [.4, -5], [1.6, 0]])),
    "look": dict(length=2.6, loop=True, tracks=dict(head=[[0, -9], [.6, -12], [1.2, 12], [1.9, 13], [2.6, -9]], torso=[[0, -4], [1.2, 6], [2.6, -4]], arm_far=hold(-64, 2.6), fore_far=hold(-104, 2.6), arm_near=hold(12, 2.6))),
    "shy": dict(length=2.0, loop=True, tracks=dict(head=wave(13, 17, 2), torso=wave(-4, 4, 2), arm_near=hold(-28, 2), fore_near=wave(-74, -80, 2), arm_far=hold(12, 2), fore_far=hold(50, 2), leg_near=hold(0, 2), leg_far=wave(-8, 2, 2), shin_far=wave(14, 2, 2))),
    "eat": dict(length=0.7, loop=True, tracks=dict(arm_far=wave(-66, -58, .7), fore_far=wave(-104, -78, .7), arm_near=hold(-30, .7), fore_near=hold(-64, .7), head=wave(4, 9, .7))),
    "pickup": dict(length=1.3, loop=False, tracks=dict(hip_y=[[0, 0], [.5, 40], [.8, 40], [1.3, 0]], leg_near=[[0, 0], [.5, -52], [.8, -52], [1.3, 0]], shin_near=[[0, 0], [.5, 84], [.8, 84], [1.3, 0]], leg_far=[[0, 0], [.5, -48], [.8, -48], [1.3, 0]], shin_far=[[0, 0], [.5, 80], [.8, 80], [1.3, 0]], torso=[[0, 0], [.5, 34], [.8, 34], [1.3, 0]], head=[[0, 0], [.5, 8], [1.3, 0]], arm_far=[[0, -10], [.5, -62], [.8, -62], [1.3, -70]], fore_far=[[0, 0], [.5, 0], [1.3, -60]], arm_near=[[0, 10], [.5, -20], [1.3, 10]])),
    "scared": dict(length=0.16, loop=True, tracks=dict(torso=wave(-5, -3, .16), head=wave(5, 8, .16), arm_near=hold(-52, .16), fore_near=hold(-104, .16), arm_far=hold(-60, .16), fore_far=hold(-100, .16), leg_near=hold(5, .16), leg_far=hold(-5, .16), hip_y=wave(8, 10, .16))),
    "hug": dict(length=1.6, loop=True, tracks=dict(arm_near=wave(-84, -78, 1.6), fore_near=wave(-34, -40, 1.6), arm_far=wave(-92, -86, 1.6), fore_far=wave(-20, -28, 1.6), torso=wave(6, 9, 1.6), head=wave(8, 11, 1.6))),
    "angry": dict(length=0.6, loop=True, tracks=dict(arm_near=hold(24, .6), fore_near=hold(-92, .6), arm_far=hold(-24, .6), fore_far=hold(-92, .6), torso=hold(6, .6), head=wave(4, 8, .6), leg_far=[[0, 0], [.2, -30], [.4, 0], [.6, 0]], shin_far=[[0, 0], [.2, 50], [.4, 0], [.6, 0]])),
    "spin": dict(length=1.0, loop=False, tracks=dict(hip_y=[[0, 0], [.2, 14], [.5, -70], [.8, 12], [1, 0]], torso=[[0, 0], [.5, -14], [1, 0]], arm_near=[[0, 10], [.5, 88], [1, 10]], arm_far=[[0, -10], [.5, -88], [1, -10]], head=[[0, 0], [.5, 12], [1, 0]], leg_near=[[0, 0], [.2, -30], [.5, 18], [1, 0]], shin_near=[[0, 0], [.2, 50], [.5, 30], [1, 0]])),
    "kick": dict(length=0.9, loop=False, tracks=dict(leg_far=[[0, 0], [.25, 42], [.45, -64], [.9, 0]], shin_far=[[0, 0], [.25, 60], [.45, 4], [.9, 0]], torso=[[0, 0], [.25, 6], [.45, -12], [.9, 0]], arm_near=[[0, 10], [.45, -44], [.9, 10]], arm_far=[[0, -10], [.45, 34], [.9, -10]])),
}

if __name__ == "__main__":
    data = json.loads(FILE.read_text(encoding="utf-8"))
    data["walk"].setdefault("gait", 1.0)
    redo = "--rehacer" in sys.argv
    added = [k for k in NEW if k not in data or redo]
    for k in added:
        data[k] = NEW[k]
    FILE.write_text(json.dumps(data), encoding="utf-8")
    print(len(added), "movimientos agregados;", len(data), "en total")
