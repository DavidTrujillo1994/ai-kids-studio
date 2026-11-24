"""Voz expresiva local (Chatterbox Multilingual, MIT) sobre onnxruntime, solo CPU.

El timbre sale de un WAV de referencia y la actuación de `exaggeration`.
"""
import gc
import subprocess
import unicodedata
import wave
from pathlib import Path

import numpy as np
import onnxruntime as ort
from tokenizers import Tokenizer

DIR = Path(__file__).resolve().parents[2] / "tools/chatterbox"
SR = 24000
START, STOP = 6561, 6562
LAYERS, HEADS, HEAD_DIM = 30, 16, 64


def session(name):
    opt = ort.SessionOptions()
    opt.log_severity_level = 3
    opt.enable_cpu_mem_arena = False  # devuelve la memoria entre frases
    return ort.InferenceSession(str(DIR / f"onnx/{name}.onnx"), opt, providers=["CPUExecutionProvider"])


def read_wav(path, ffmpeg):
    raw = subprocess.run([ffmpeg, "-v", "error", "-i", str(path), "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"],
                         capture_output=True, check=True).stdout
    return np.frombuffer(raw, np.float32).copy()


def write_wav(path, x, sr=SR):
    with wave.open(str(path), "wb") as f:
        f.setnchannels(1)
        f.setsampwidth(2)
        f.setframerate(sr)
        f.writeframes((np.clip(x, -1, 1) * 32767).astype("<i2").tobytes())


_shared = {}


def shared(name):
    """Los modelos se cargan una sola vez aunque hablen varios personajes: cada copia ocupa más de 1 GB."""
    if name not in _shared:
        _shared[name] = session(name)
    return _shared[name]


class Voice:
    """Un personaje: solo guarda su timbre. Los modelos son compartidos."""

    def __init__(self, reference, ffmpeg):
        audio = read_wav(reference, ffmpeg)[None, :]
        enc = session("speech_encoder")
        self.cond, self.prompt_token, self.xvector, self.prompt_feat = enc.run(None, {"audio_values": audio})
        del enc
        gc.collect()
        self.tok = Tokenizer.from_file(str(DIR / "tokenizer.json"))

    def tokens(self, text, seed, exaggeration=0.7, temperature=0.8, min_p=0.05, penalty=1.2, cfg=0.4, limit=400):
        rng = np.random.default_rng(seed)
        ids = np.array([self.tok.encode("[es]" + unicodedata.normalize("NFKD", text.lower())).ids], dtype=np.int64)
        pos = np.where(ids >= START, 0, np.arange(ids.shape[1])[None, :] - 1).astype(np.int64)
        ex = np.array([exaggeration], dtype=np.float32)
        emb = shared("embed_tokens").run(None, {"input_ids": ids, "position_ids": pos, "exaggeration": ex})[0]
        # guía sin clasificador: la segunda fila es la misma condición sin el texto
        blank = emb.copy()
        blank[:, ids[0] < START] = 0
        x = np.concatenate([np.concatenate([self.cond, e], 1) for e in ([emb, blank] if cfg else [emb])], 0)
        b = x.shape[0]
        past = {f"past_key_values.{i}.{k}": np.zeros((b, HEADS, 0, HEAD_DIM), np.float32) for i in range(LAYERS) for k in ("key", "value")}
        mask = np.ones((b, x.shape[1]), np.int64)
        out = []
        for i in range(limit):
            logits, *present = shared("language_model_q4").run(None, {"inputs_embeds": x, "attention_mask": mask, **past})
            lg = logits[:, -1, :]
            lg = lg[0] + cfg * (lg[0] - lg[1]) if cfg else lg[0]
            lg = lg.astype(np.float64)
            if out:
                seen = np.array(sorted(set(out)))
                lg[seen] = np.where(lg[seen] < 0, lg[seen] * penalty, lg[seen] / penalty)
            lg /= temperature
            p = np.exp(lg - lg.max())
            p /= p.sum()
            p[p < min_p * p.max()] = 0
            t = int(rng.choice(len(p), p=p / p.sum()))
            if t == STOP:
                break
            out.append(t)
            step = shared("embed_tokens").run(None, {"input_ids": np.array([[t]], np.int64), "position_ids": np.array([[i + 1]], np.int64), "exaggeration": ex})[0]
            x = np.repeat(step, b, 0)
            mask = np.concatenate([mask, np.ones((b, 1), np.int64)], 1)
            past = dict(zip(past, present))
        return out

    def say(self, text, seed, **kw):
        t = self.tokens(text, seed, **kw)
        t = [k for k in t if k < START]
        speech = np.concatenate([self.prompt_token, np.array([t], np.int64)], 1)
        wav = shared("conditional_decoder").run(None, {"speech_tokens": speech, "speaker_embeddings": self.xvector, "speaker_features": self.prompt_feat})[0]
        return np.squeeze(wav)
