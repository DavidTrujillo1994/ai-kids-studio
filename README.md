# AI Kids Studio

Entorno local y gratuito. Instalado y verificado el 23-nov-2026 (Windows 11, i3, Intel UHD).

| Herramienta | Versión | Ubicación | Licencia |
|---|---|---|---|
| Godot | 4.7.2 | winget `GodotEngine.GodotEngine` | MIT |
| FFmpeg | 9.1 | winget `Gyan.FFmpeg` | GPL-3.0 |
| Rhubarb Lip Sync | 1.14.0 | `tools/rhubarb` | MIT |
| piper-tts | 1.8.0 | `.venv` (Python 3.14.3) | GPL-3.0 |

Voces (`tools/voices`, cada una con su MODEL_CARD): es_MX-claude-high (Apache-2.0),
es_ES-davefx-medium (CC0), es_ES-sharvard-medium (CC BY 3.0: requiere atribución).

Voz actual de Bruno: Chatterbox Multilingual ONNX (MIT, proyecto archivado) en `tools/chatterbox`, con referencia
sintética de Kokoro-82M (Apache-2.0) en `tools/kokoro`. Verificación de tomas: faster-whisper `small` (MIT).

## Notas
- Godot NO puede grabar con `--headless` (se cierra con error, verificado). Usar ventana y `--rendering-method gl_compatibility`.
- Rhubarb debe usarse con `-r phonetic` para audio en español.

## Reinstalar Piper
    python -m venv .venv
    .venv\Scripts\python.exe -m pip install --only-binary :all: -r requirements.txt

## Uso

Doble clic en `Iniciar.cmd`. Arquitectura: `documentation/ARQUITECTURA.md`.

Ver `documentation/GUIA-QUE-DEBO-HACER.md`.

    .venv\Scripts\python.exe video-engine\scripts\producir.py piloto-01
