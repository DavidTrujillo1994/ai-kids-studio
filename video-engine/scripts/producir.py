"""Produce un episodio: guion.json -> voces -> labios -> escena.json -> Godot -> MP4 validado.

Uso: .venv\\Scripts\\python.exe video-engine\\scripts\\producir.py piloto-01
     --eventos  informa el avance en líneas "@@{json}" (lo usa la aplicación)
     --padre=N  se detiene si el proceso N (la aplicación) termina
     --revisar  dice qué herramientas encuentra, en JSON
"""
import ctypes
import difflib
import hashlib
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
import unicodedata
import wave
from pathlib import Path

import numpy as np

ROOT = Path(__file__).resolve().parents[2]
MEDIA = ROOT / "media"
WINGET = Path(os.environ["LOCALAPPDATA"]) / "Microsoft/WinGet/Packages"
PAUSE = 0.25
TAKES = 3
GOOD, ENOUGH = 0.93, 0.75  # parecido con el guion: aceptar ya / aceptar si no hubo una toma mejor
EVENTS = "--eventos" in sys.argv


def tool(name, env, pattern):
    """Variable de entorno > PATH > carpeta de winget."""
    return os.environ.get(env) or shutil.which(name) or next(map(str, WINGET.glob(pattern)), None)


def stop(message, detail=""):
    """Termina con un mensaje para la persona; el detalle técnico va antes."""
    sys.exit(f"{detail}\nAVISO: {message}")


def event(stage, progress, message):
    if EVENTS:
        print("@@" + json.dumps({"stage": stage, "progress": progress, "message": message}, ensure_ascii=False), flush=True)


GODOT = tool("godot_console", "GODOT_BIN", "GodotEngine.GodotEngine_*/godot_console.exe")
FFMPEG = tool("ffmpeg", "FFMPEG_BIN", "Gyan.FFmpeg_*/*/bin/ffmpeg.exe")
FFPROBE = tool("ffprobe", "FFPROBE_BIN", "Gyan.FFmpeg_*/*/bin/ffprobe.exe")
RHUBARB = next(map(str, (ROOT / "tools/rhubarb").glob("*/rhubarb.exe")), None)


def tools():
    """(nombre, para qué sirve, ¿está?, detalle). Solo el nombre del archivo: no se exponen rutas."""
    def found(path):
        return bool(path) and Path(path).exists()

    def has(module):
        return importlib.util.find_spec(module) is not None

    rows = [("Godot", "Anima los personajes", found(GODOT), Path(GODOT or "").name),
            ("FFmpeg", "Une video, voces, música y subtítulos", found(FFMPEG) and found(FFPROBE), Path(FFMPEG or "").name),
            ("Rhubarb", "Sincroniza la boca con la voz", found(RHUBARB), Path(RHUBARB or "").name),
            ("Chatterbox", "Crea las voces expresivas",
             (ROOT / "tools/chatterbox/onnx/language_model_q4.onnx_data").exists() and has("onnxruntime"), "Modelo local"),
            ("Whisper", "Comprueba que la voz diga el guion", (ROOT / "tools/whisper").exists() and has("faster_whisper"), "Modelo local"),
            ("Letra Andika", "Subtítulos", (MEDIA / "fonts/Andika-Bold.ttf").exists(), "Andika-Bold.ttf")]
    return [{"name": n, "purpose": p, "ready": bool(ok), "detail": d if ok else "No se encontró"} for n, p, ok, d in rows]


def watch_parent(pid):
    """Si la aplicación se cierra a la fuerza, no deja el motor (ni Godot) trabajando solo."""
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.OpenProcess.restype = ctypes.c_void_p
    kernel.WaitForSingleObject.argtypes = [ctypes.c_void_p, ctypes.c_uint32]
    handle = kernel.OpenProcess(0x00100000, False, pid)  # SYNCHRONIZE

    def wait():
        if handle:
            kernel.WaitForSingleObject(handle, 0xFFFFFFFF)
        subprocess.run(["taskkill", "/T", "/F", "/PID", str(os.getpid())], capture_output=True)

    threading.Thread(target=wait, daemon=True).start()


def run(cmd, **kw):
    r = subprocess.run([str(c) for c in cmd], capture_output=True, stdin=subprocess.DEVNULL, text=True, encoding="utf-8", errors="replace", **kw)
    if r.returncode:
        name = Path(str(cmd[0])).stem
        stop(f"Falló la herramienta {name}.", f"ERROR {r.returncode} en {name}:\n{(r.stdout + r.stderr)[-3000:]}")
    return r.stdout + r.stderr


def duration(wav):
    with wave.open(str(wav)) as f:
        return f.getnframes() / f.getframerate()


def piper(text, profile, raw):
    txt = raw.with_suffix(".txt")
    txt.write_text(text, encoding="utf-8")
    cmd = [sys.executable, "-m", "piper", "-m", ROOT / f"tools/voices/{profile['model']}.onnx", "-i", txt, "-f", raw,
           "--length-scale", profile["lengthScale"]]
    if profile["speaker"] is not None:
        cmd += ["-s", profile["speaker"]]
    run(cmd, env={**os.environ, "PYTHONUTF8": "1"})


UNITS = ("cero uno dos tres cuatro cinco seis siete ocho nueve diez once doce trece catorce quince dieciséis diecisiete dieciocho "
         "diecinueve veinte veintiuno veintidós veintitrés veinticuatro veinticinco veintiséis veintisiete veintiocho veintinueve").split()
TENS = dict(zip(range(3, 10), "treinta cuarenta cincuenta sesenta setenta ochenta noventa".split()))
HUNDREDS = ["", "ciento", "doscientos", "trescientos", "cuatrocientos", "quinientos", "seiscientos", "setecientos", "ochocientos", "novecientos"]


def words(n):
    """Un número de 0 a 999 en palabras."""
    if n < 30:
        return UNITS[n]
    if n < 100:
        return TENS[n // 10] + (f" y {UNITS[n % 10]}" if n % 10 else "")
    if n == 100:
        return "cien"
    return HUNDREDS[n // 100] + (f" {words(n % 100)}" if n % 100 else "")


def plain(text):
    """Solo letras, para comparar. El verificador escribe «1, 2, 3» aunque oiga «uno, dos, tres»."""
    text = re.sub(r"\d+", lambda m: words(int(m[0])) if len(m[0]) < 4 else m[0], text)
    text = unicodedata.normalize("NFKD", text.lower())
    return "".join(c for c in text if c.isalpha() and not unicodedata.combining(c))


def heard(wavs):
    """Transcribe audios: sirve para rechazar tomas que no dicen lo que pide el guion."""
    from faster_whisper import WhisperModel
    whisper = WhisperModel("small", device="cpu", compute_type="int8", cpu_threads=4, download_root=str(ROOT / "tools/whisper"))
    texts = []
    for wav in wavs:
        pcm = subprocess.run([FFMPEG, "-v", "error", "-i", str(wav), "-f", "f32le", "-ac", "1", "-ar", "16000", "-"], capture_output=True, check=True).stdout
        segments, _ = whisper.transcribe(np.frombuffer(pcm, np.float32), language="es", beam_size=1, condition_on_previous_text=False)
        texts.append(" ".join(s.text.strip() for s in segments))
    return texts


class Line:
    """Una frase de un personaje y los archivos que le corresponden."""

    def __init__(self, ep, who, text, profile):
        self.who, self.text, self.profile = who, text, profile
        # el nombre sale del contenido: mover o insertar frases no obliga a regenerar las demás
        digest = hashlib.sha256(json.dumps([who, text, profile], sort_keys=True).encode()).hexdigest()[:16]
        self.wav = ep / f"audio/voz-{digest}.wav"
        self.raw = self.wav.with_suffix(".raw.wav")
        self.cues = self.wav.with_suffix(".labios.json")
        self.stamp = self.wav.with_suffix(".key")
        self.key = json.dumps([text, profile], sort_keys=True)
        self.best = (0.0, None)

    def ready(self):
        return all(f.exists() for f in (self.wav, self.cues, self.stamp)) and self.stamp.read_text(encoding="utf-8") == self.key

    def finish(self):
        run([FFMPEG, "-v", "error", "-y", "-i", self.raw, "-af",
             f"rubberband=pitch={self.profile['pitch']}:formant=shifted,loudnorm=I=-18:TP=-2", "-ar", 48000, self.wav])
        run([RHUBARB, "-q", "-r", "phonetic", "-f", "json", "-o", self.cues, self.raw])
        self.stamp.write_text(self.key, encoding="utf-8")


def expressive(lines):
    """Genera todas las frases por etapas, con un solo modelo en memoria, y repite las que no se entiendan."""
    import voz_expresiva as ve
    pending = list(lines)
    for take in range(TAKES):
        voices = {ln.who: ve.Voice(MEDIA / "characters" / ln.who / ln.profile["reference"], FFMPEG) for ln in pending}
        acted = []
        for i, ln in enumerate(pending):
            event("VOICES", 2 + round(10 * i / len(pending)), f"Actuando la frase {i + 1} de {len(pending)}: «{ln.text}»")
            acted.append(voices[ln.who].tokens(ln.text, ln.profile["seed"] + take, exaggeration=ln.profile["exaggeration"], cfg=ln.profile["cfg"]))
        ve.release()
        takes = []
        for i, (ln, tokens) in enumerate(zip(pending, acted)):
            event("VOICES", 12 + round(42 * i / len(pending)), f"Voz {i + 1} de {len(pending)}: «{ln.text}»")
            takes.append(voices[ln.who].sound(tokens))
            ve.write_wav(ln.raw, takes[-1])
        ve.release()
        event("VOICES", 55, "Comprobando que se entienda")
        for ln, x, said in zip(pending, takes, heard([ln.raw for ln in pending])):
            score = difflib.SequenceMatcher(None, plain(ln.text), plain(said)).ratio()
            print(f"    toma {take + 1}: {score:.0%} «{said}»", flush=True)
            if score > ln.best[0]:
                ln.best = (score, x)
            if ln.best[0] >= GOOD or take == TAKES - 1 and ln.best[0] >= ENOUGH:
                ve.write_wav(ln.raw, ln.best[1])
                ln.finish()  # lo bueno se guarda ya: si otra frase falla, no se pierde
        pending = [ln for ln in pending if not ln.ready()]
        if not pending:
            return
    stop(f"No logré una voz clara para «{pending[0].text}». Prueba a cambiar la frase: las demás ya quedaron guardadas.")


def simple(lines):
    for ln in lines:
        piper(ln.text, ln.profile, ln.raw)
        ln.finish()


ENGINES = {"chatterbox": expressive, "piper": simple}


def make_voices(lines):
    """Reúne las frases que faltan y las genera con el motor de cada personaje."""
    todo = list({ln.wav: ln for ln in lines if not ln.ready()}.values())
    for ln in todo:
        ln.stamp.unlink(missing_ok=True)
    for engine in sorted({ln.profile["engine"] for ln in todo}):
        if engine not in ENGINES:
            stop(f"Motor de voz desconocido: {engine}")
        ENGINES[engine]([ln for ln in todo if ln.profile["engine"] == engine])


class Director:
    """Convierte los pasos del guion en una línea de tiempo: lugares, acciones, voces y cámara.

    La duración sale de las voces y los movimientos, nunca al revés.
    """

    CUT = 0.4
    SHOTS = (1.12, 1.22, 1.16)

    def __init__(self, script, ep):
        self.script, self.ep = script, ep
        self.anims = json.loads((MEDIA / "characters/animaciones.json").read_text(encoding="utf-8"))
        self.sheets, self.speed, self.pos, self.facing = {}, {}, {}, {}
        for c in script["characters"]:
            base = MEDIA / "characters" / c["id"]
            self.sheets[c["id"]] = json.loads((base / "ficha.json").read_text(encoding="utf-8"))
            self.speed[c["id"]] = json.loads((base / "rig.json").read_text(encoding="utf-8"))["walkSpeed"]
            self.pos[c["id"]] = c["x"]
            self.facing[c["id"]] = 1 if c["x"] < 900 else -1
        self.t, self.events, self.moves, self.dialogues, self.props = 0.0, [], [], [], []
        self.scenes = [{"t": 0.0, "environment": script["environment"], "ambience": script.get("ambience", "dia"), "positions": dict(self.pos)}]
        self.camera = [{"t": 0.0, "x": 0.0, "zoom": 1.0}]
        for who, side in self.facing.items():
            self.act(who, facing=side)

    def act(self, who, **what):
        self.events.append({"t": self.t, "character": who, **what})

    def rest(self, who):
        self.act(who, animation="idle", lift=0)

    def shot(self, focus, zoom):
        """Mueve la cámara suavemente hacia un punto de interés."""
        last = self.camera[-1]
        x = max(-150.0, min(300.0, (focus - 640) * (0.55 if zoom > 1 else 0.2)))
        start = max(self.t, last["t"])
        self.camera.append({"t": start, "x": last["x"], "zoom": last["zoom"]})
        self.camera.append({"t": start + 0.7, "x": x, "zoom": zoom})

    def turn(self, who, side):
        if self.facing[who] != side:
            self.facing[who] = side
            self.act(who, facing=side)

    def on_stage(self, but=None):
        return [w for w, x in self.pos.items() if w != but and -40 <= x <= 1320]

    def line(self, s):
        return Line(self.ep, s["character"], s["text"], {**self.sheets[s["character"]]["voice"], **s.get("voice", {})})

    def run(self):
        make_voices([self.line(s) for s in self.script["steps"] if s["do"] == "say" and s["character"] in self.sheets])
        for s in self.script["steps"]:
            who = s.get("character")
            if who is not None and who not in self.sheets:
                stop(f"Personaje desconocido: {who}")
            for key in ("animation", "gait"):
                if s.get(key, "idle") not in self.anims:
                    stop(f"Animación desconocida: {s[key]}")
            step = getattr(self, "do_" + str(s["do"]), None)
            if step is None:
                stop(f"Paso desconocido: {s['do']}")
            step(s, who)
        self.t += 0.5
        self.events.sort(key=lambda e: e["t"])
        script = self.script
        return {"episode": script["id"], "fps": script["fps"], "resolution": script["resolution"], "duration": self.t,
                "frames": round(self.t * script["fps"]), "scenes": self.scenes, "characters": script["characters"], "events": self.events,
                "moves": self.moves, "dialogues": self.dialogues, "props": self.props, "camera": self.camera}

    def do_walk(self, s, who):
        gait = s.get("gait", "walk")
        info = self.anims[gait]
        start, end = self.pos[who], s["to"]
        d = max(abs(end - start) / (self.speed[who] * info.get("gait", 1.0)), 0.05)
        self.turn(who, 1 if end >= start else -1)
        self.shot((start + end) / 2, 1.0)
        self.act(who, animation=gait, expression=s.get("expression", "normal"), lift=info.get("lift", 0))
        self.moves.append({"character": who, "start": self.t, "end": self.t + d, "from": start, "to": end})
        self.pos[who] = end
        self.t += d
        self.rest(who)

    def do_say(self, s, who):
        others = self.on_stage(but=who)
        here = self.pos[who]
        if others:
            near = min(others, key=lambda w: abs(self.pos[w] - here))
            self.turn(who, 1 if self.pos[near] >= here else -1)
            for w in others:
                self.turn(w, 1 if here >= self.pos[w] else -1)
            gap = abs(self.pos[near] - here)
            self.shot((here + self.pos[near]) / 2 if gap < 520 else here, 1.1 if gap < 520 else self.SHOTS[len(self.dialogues) % 3])
        else:
            if here > 900:
                self.turn(who, -1)
            self.shot(here, self.SHOTS[len(self.dialogues) % 3])
        animation = s.get("animation", "talk")
        lift = self.anims[animation].get("lift", 0)
        self.act(who, animation=animation, expression=s.get("expression", "normal"), lift=lift)
        self.t += 0.15
        line = self.line(s)
        d, cues = duration(line.wav), json.loads(line.cues.read_text(encoding="utf-8"))["mouthCues"]
        self.dialogues.append({"character": who, "text": s["text"], "audio": line.wav.name, "start": self.t, "end": self.t + d, "cues": cues})
        self.t += d + PAUSE
        self.rest(who)

    def do_play(self, s, who):
        info = self.anims[s["animation"]]
        times = max(1, round(1.8 / info["length"])) if info["loop"] else 1
        if info.get("lift"):
            self.shot(self.pos[who], 1.0)
        self.act(who, animation=s["animation"], expression=s.get("expression", "normal"), lift=info.get("lift", 0))
        self.t += info["length"] * times
        self.rest(who)

    def do_wait(self, s, who):
        info = self.anims[s.get("animation", "idle")]
        self.act(who, animation=s.get("animation", "idle"), expression=s.get("expression", "normal"), lift=info.get("lift", 0))
        self.t += s["seconds"]

    def do_prop(self, s, _):
        self.props.append({"prop": s["prop"], "scene": len(self.scenes) - 1, "path": [[self.t + a, x, y] for a, x, y in s["path"]]})

    def do_scene(self, s, _):
        self.t += self.CUT
        for who in self.pos:
            self.pos[who] = s.get("positions", {}).get(who, -260)
            self.facing[who] = 1 if self.pos[who] < 700 else -1
            self.act(who, animation="idle", expression="normal", lift=0, facing=self.facing[who])
        self.scenes.append({"t": self.t, "environment": s["environment"], "ambience": s.get("ambience", "dia"), "positions": dict(self.pos)})
        last = self.camera[-1]
        self.camera.append({"t": max(self.t - 0.01, last["t"]), "x": last["x"], "zoom": last["zoom"]})
        self.camera.append({"t": max(self.t, last["t"]), "x": 0.0, "zoom": 1.0})
        self.t += self.CUT


def compile_scene(script, ep):
    return Director(script, ep).run()


MOODS = {  # segundos por pulso, escala y acordes según el ambiente
    "alegre": (0.42, [523.25, 587.33, 659.25, 783.99, 880.0, 1046.5], [(130.81, 196.0), (110.0, 164.81), (174.61, 261.63), (98.0, 146.83)]),
    "tranquilo": (0.6, [440.0, 523.25, 587.33, 659.25, 783.99, 880.0], [(110.0, 164.81), (87.31, 130.81), (130.81, 196.0), (98.0, 146.83)]),
    "misterio": (0.52, [440.0, 493.88, 523.25, 659.25, 698.46, 880.0], [(110.0, 130.81), (87.31, 110.0), (110.0, 164.81), (82.41, 123.47)])}
MOOD_OF = {"noche": "tranquilo", "nieve": "tranquilo", "atardecer": "tranquilo", "lluvia": "misterio"}


def music(seconds, out, name="", mood="alegre", sr=48000):
    """Melodía original de caja de música, distinta para cada episodio: sin licencias de terceros."""
    beat, scale, chords = MOODS[mood]
    seed = int(hashlib.sha256(name.encode()).hexdigest()[:8], 16)
    rng, buf = np.random.default_rng(seed), np.zeros(int((seconds + 3) * sr))

    def note(freq, at, length, gain):
        n, i = int(length * sr), int(at * sr)
        if i + n > len(buf):
            return
        x = np.arange(n) / sr
        env = np.exp(-3.2 * x / length) * np.minimum(1, x * 200) * np.minimum(1, (length - x) * 50)
        buf[i:i + n] += gain * env * (np.sin(2 * np.pi * freq * x) + .3 * np.sin(4 * np.pi * freq * x))

    k, idx = 0, 2
    while k * beat < seconds:
        if k % 4 == 0:
            for f in chords[k // 4 % 4]:
                note(f, k * beat, 1.6, .22)
        if rng.random() < .8:
            idx = int(np.clip(idx + rng.integers(-2, 3), 0, 5))
            note(scale[idx], k * beat, 1.1, .3)
        k += 1
    buf = buf[:int(seconds * sr)]
    fade = int(min(1.2, seconds / 2) * sr)
    buf[-fade:] *= np.linspace(1, 0, fade)
    with wave.open(str(out), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(sr)
        f.writeframes((buf / np.abs(buf).max() * .8 * 32767).astype("<i2").tobytes())


def stamp(x):
    ms = round(x * 1000)
    return f"{ms // 3600000:02}:{ms // 60000 % 60:02}:{ms // 1000 % 60:02},{ms % 1000:03}"


def assemble(scene, ep, video):
    srt = ep / "subtitles/subtitulos.srt"
    srt.write_text("\n".join(f"{i}\n{stamp(d['start'])} --> {stamp(d['end'])}\n{d['text']}\n"
                             for i, d in enumerate(scene["dialogues"], 1)), encoding="utf-8")
    track = ep / "audio/musica.wav"
    moods = [MOOD_OF.get(x["ambience"], "alegre") for x in scene["scenes"]]
    music(scene["duration"], track, scene["episode"], max(set(moods), key=moods.count))
    inputs, graph, mix = ["-i", video, "-i", track], ["[1:a]volume=0.10[m]"], "[m]"
    for i, d in enumerate(scene["dialogues"]):
        inputs += ["-i", ep / "audio" / d["audio"]]
        graph.append(f"[{i + 2}:a]adelay={round(d['start'] * 1000)}:all=1[d{i}]")
        mix += f"[d{i}]"
    graph.append(f"{mix}amix=inputs={len(scene['dialogues']) + 1}:normalize=0:duration=longest,"
                 "loudnorm=I=-16:TP=-1.5:LRA=11,aresample=48000[a]")
    style = ("FontName=Andika,FontSize=19,Bold=1,PrimaryColour=&H00FFFFFF,OutlineColour=&H00402A1A,"
             "Outline=2,Shadow=0,MarginV=18")
    subs = f"subtitles=subtitles/subtitulos.srt:fontsdir=fonts:force_style='{style}'" if scene["dialogues"] else "null"
    graph.append(f"[0:v]fade=t=in:d=0.4,fade=t=out:st={scene['duration'] - .6:.3f}:d=0.6,{subs}[v]")
    (ep / "fonts").mkdir(exist_ok=True)
    shutil.copy(MEDIA / "fonts/Andika-Bold.ttf", ep / "fonts")
    out = ep / "render/nuevo.mp4"  # el video anterior se reemplaza solo cuando este se valida
    run([FFMPEG, "-v", "error", "-y", *inputs, "-filter_complex", ";".join(graph), "-map", "[v]", "-map", "[a]",
         "-t", f"{scene['duration']:.3f}", "-c:v", "libx264", "-crf", 20, "-preset", "medium", "-pix_fmt", "yuv420p",
         "-c:a", "aac", "-b:a", "160k", "-movflags", "+faststart", out], cwd=ep)
    at = scene["dialogues"][0]["start"] + .5 if scene["dialogues"] else 0
    run([FFMPEG, "-v", "error", "-y", "-ss", f"{at:.2f}", "-i", out, "-frames:v", 1, ep / "thumbnail/miniatura.png"])
    return out


def validate(out, scene):
    info = json.loads(run([FFPROBE, "-v", "error", "-show_streams", "-show_format", "-of", "json", out]))
    v = next(s for s in info["streams"] if s["codec_type"] == "video")
    a = next(s for s in info["streams"] if s["codec_type"] == "audio")
    checks = {"h264": v["codec_name"] == "h264", "aac": a["codec_name"] == "aac",
              "resolucion": [v["width"], v["height"]] == scene["resolution"], "fps": v["r_frame_rate"] == f"{scene['fps']}/1",
              "duracion": abs(float(info["format"]["duration"]) - scene["duration"]) < 0.2}
    bad = [k for k, ok in checks.items() if not ok]
    if bad:
        stop("El video salió con un formato inesperado.", f"MP4 inválido: {bad}")
    return float(info["format"]["duration"]), int(info["format"]["size"])


def publish(new, final):
    """Reemplaza el video anterior; en Windows puede estar abierto en el reproductor."""
    for _ in range(20):
        try:
            os.replace(new, final)
            return
        except PermissionError:
            time.sleep(.5)
    stop("El video anterior está abierto en otro programa. Ciérralo y vuelve a intentarlo.")


def main(episode):
    missing = [t["name"] for t in tools() if not t["ready"]]
    if missing:
        stop(f"Falta instalar: {', '.join(missing)}.")
    ep = MEDIA / "episodes" / episode
    script = json.loads((ep / "script/guion.json").read_text(encoding="utf-8"))
    for d in ("audio", "scenes", "render", "subtitles", "thumbnail", "logs"):
        (ep / d).mkdir(exist_ok=True)
    times, t0 = {}, time.time()

    def lap(name):
        nonlocal t0
        times[name] = round(time.time() - t0, 1)
        t0 = time.time()
        print(f"[{name}] {times[name]} s", flush=True)

    scene = compile_scene(script, ep)
    path = ep / "scenes/escena.json"
    path.write_text(json.dumps(scene, ensure_ascii=False, indent=1), encoding="utf-8")
    lap("voces y labios")
    event("ANIMATION", 60, "Animando la escena")
    video = ep / "render/escena.avi"
    video.unlink(missing_ok=True)
    log = run([GODOT, "--path", ROOT / "animation-engine/godot-project", "--rendering-method", "gl_compatibility",
               "--write-movie", video, "--fixed-fps", scene["fps"], "--quit-after", scene["frames"] + 1, "--disable-vsync",
               "--", f"--scene={path.as_posix()}", f"--media={MEDIA.as_posix()}"], timeout=900)
    (ep / "logs/godot.log").write_text(log, encoding="utf-8")
    if "ERROR" in log or not video.exists() or video.stat().st_size == 0:
        stop("Godot no pudo animar la escena.", log[-3000:])
    lap("render Godot")
    event("ASSEMBLY", 80, "Uniendo video, voces y música")
    out = assemble(scene, ep, video)
    lap("montaje FFmpeg")
    event("VALIDATION", 95, "Comprobando el video")
    secs, size = validate(out, scene)
    publish(out, ep / "render/final.mp4")
    video.unlink(missing_ok=True)
    (ep / "logs/produccion.json").write_text(json.dumps({"tiempos": times, "duracion": secs, "bytes": size}, indent=1), encoding="utf-8")
    print(f"OK {ep / 'render/final.mp4'}  {secs:.2f} s  {size / 1e6:.1f} MB  (pendiente de aprobación humana)")


if __name__ == "__main__":
    names = [a for a in sys.argv[1:] if not a.startswith("--")]
    for arg in sys.argv[1:]:
        if arg.startswith("--padre="):
            watch_parent(int(arg.split("=")[1]))
    if "--revisar" in sys.argv:
        print(json.dumps(tools(), ensure_ascii=False))
    elif len(names) == 1:
        main(names[0])
    else:
        sys.exit(__doc__)
