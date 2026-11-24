"""Genera las imágenes que muestra la aplicación: retratos de personajes y vistas de lugares.

Uso: .venv\\Scripts\\python.exe video-engine\\scripts\\miniaturas.py [--todas]
"""
import json
import subprocess
import sys
import tempfile
from pathlib import Path

import producir as p


def still(scene, out, crop=None, size="480:270"):
    """Graba unos cuadros y conserva el último, ya asentado."""
    with tempfile.TemporaryDirectory() as tmp:
        tmp = Path(tmp)
        (tmp / "s.json").write_text(json.dumps(scene), encoding="utf-8")
        log = p.run([p.GODOT, "--path", p.ROOT / "animation-engine/godot-project", "--rendering-method", "gl_compatibility", "--write-movie", tmp / "f.avi",
                     "--fixed-fps", 24, "--quit-after", scene["frames"], "--disable-vsync", "--", f"--scene={(tmp / 's.json').as_posix()}", f"--media={p.MEDIA.as_posix()}"], timeout=120)
        if "ERROR" in log:
            sys.exit(log[-2000:])
        vf = (f"crop={crop}," if crop else "") + f"scale={size}"
        subprocess.run([p.FFMPEG, "-v", "error", "-y", "-sseof", "-0.1", "-i", str(tmp / "f.avi"), "-frames:v", "1", "-vf", vf, str(out)], check=True)


def scene(environment, ambience="dia", cast=(), events=(), zoom=1.0, frames=8):
    return {"fps": 24, "scenes": [{"t": 0, "environment": environment, "ambience": ambience, "positions": {c: x for c, x in cast}}],
            "characters": [{"id": c, "x": x} for c, x in cast], "events": list(events), "moves": [], "dialogues": [], "props": [],
            "camera": [{"t": 0, "x": 0, "zoom": zoom}], "frames": frames}


if __name__ == "__main__":
    everything = "--todas" in sys.argv
    for env in sorted((p.MEDIA / "environments").glob("*/escenario.json")):
        out = env.parent / "miniatura.png"
        if everything or not out.exists():
            still(scene(env.parent.name), out)
            print("lugar", env.parent.name)
    for rig in sorted((p.MEDIA / "characters").glob("*/rig.json")):
        key, out = rig.parent.name, rig.parent / "retrato.png"
        if everything or not out.exists():
            ev = [{"t": 0, "character": key, "animation": "wave", "expression": "normal"}]
            still(scene("bosque-central", cast=[(key, 600)], events=ev, frames=13), out, crop="470:470:372:95", size="360:360")
            print("personaje", key)
