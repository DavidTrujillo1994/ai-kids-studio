# Guía: qué debo hacer

Basada en `AI-KIDS-STUDIO-MVP.md`. Regla fija: **todo gratis**. Actualizada el 23-nov-2026.

**Dónde estamos:** la aplicación ya existe y produce videos. Falta tu opinión sobre Bruno y su voz.

## 0. Abrir la aplicación

Doble clic en `Iniciar.cmd`. En unos 15 segundos se abre http://127.0.0.1:8095. Deja abierta la ventana negra mientras la uses.

1. **Crear episodio:** elige una historia, los personajes y el título.
2. **Escribe el guion** con los botones (se guarda solo). Con *Cambia de lugar* la historia sigue en otro sitio, y *Sugerir movimientos* ajusta las acciones a lo que dicen.
3. **Crea el video** y espera.
4. **Revisa y aprueba**, o pide cambios.

---

## 1. Lo que te toca AHORA (unos 15 minutos)

- [ ] **Ver el video:** `media\episodes\piloto-01\render\final.mp4`
- [ ] **Escuchar las voces:** carpeta `pruebas\voces-bruno`
- [ ] **Responder estas cinco decisiones** (§23 del documento):

| # | Decisión | Mi recomendación |
|---|---|---|
| 1 | ¿Te gusta el diseño de Bruno? ¿Qué cambias? | Ajustar ahora; después se bloquea (§4.4) |
| 2 | ¿Qué voz para Bruno? (hoy: la 1, `santa`, provisional) | La que más te haga reír |
| 3 | ¿Te gustan Luna, Nico y Tito, y sus voces? | Verlos en los episodios «Ejemplo» |
| 4 | ¿24 o 30 FPS? | 24 |
| 5 | ¿Para qué es: portafolio, uso personal o monetizar? | Define si vale la pena construir Java + Angular |

**Punto de control:** si el video no te convence, lo corregimos antes de programar nada más.

---

## 2. Producir por consola (opcional)

La aplicación hace esto por ti. Los episodios hechos así solo se incorporan la primera vez que se abre.

Abre una terminal nueva en la carpeta del proyecto y ejecuta:

    .venv\Scripts\python.exe video-engine\scripts\producir.py piloto-01

Cada frase nueva tarda cerca de un minuto en generar su voz; las que no cambian se reutilizan. El resto tarda unos 20 segundos. Se abre una ventana de Godot: **no la cierres ni la minimices**; se cierra sola.

### Cambiar la historia
Edita `media\episodes\piloto-01\script\guion.json`. Cada paso de `steps` es una acción:

| `do` | Qué hace | Ejemplo |
|---|---|---|
| `walk` | Caminar hasta un punto (0 = izquierda, 1280 = derecha) | `{"do": "walk", "character": "bruno", "to": 520}` |
| `say` | Hablar | `{"do": "say", "character": "bruno", "text": "¡Hola!", "animation": "wave"}` |
| `play` | Hacer una animación | `{"do": "play", "character": "bruno", "animation": "jump", "expression": "happy"}` |
| `wait` | Esperar | `{"do": "wait", "character": "bruno", "seconds": 1}` |
| `prop` | Mostrar un objeto que se mueve | ver la mariposa en el guion |

- **Animaciones:** 31 en total; la aplicación las muestra en español
- **Expresiones:** `normal` `happy` `sad` `surprised` `picaro` `scared` `angry` `sleepy`

No pongas duraciones a los diálogos: se calculan solas a partir de la voz.

### Cambiar la voz
En `media\characters\bruno\ficha.json`, sección `voice`:
- `reference`: WAV de unos 8 segundos con el timbre a imitar (está junto a la ficha). Usa solo voces propias o sintéticas.
- `exaggeration`: 0.5 tranquila, 0.9 muy expresiva
- `pitch`: 1 = normal, 1.12 = más infantil
- `seed`: cámbialo para obtener otra actuación de la misma frase

Cada toma se transcribe sola y se rechaza si no dice lo que pide el guion.

### Cambiar el dibujo
Abre con Inkscape (gratis, aún sin instalar) los SVG de `media\characters\bruno`.
Cambia formas y colores, pero **no el tamaño de la página ni el nombre del archivo**.
Haz una copia de la carpeta antes de editar.

---

## 3. El plan completo

| Fase | Qué se hace | Yo | Tú | Estado |
|---|---|---|---|---|
| **0. Prueba técnica** | Bruno camina, saluda, habla y salta | Construir | Ver y opinar | **Hecha; falta tu visto bueno** |
| **1. Piloto** (§3.2) | 45-60 s, varios personajes y lugares | 4 personajes, 7 lugares, 31 movimientos | Elegir la historia, revisar guion, aprobar diseños | **Todo listo para hacerlo** |
| **2. Orquestador** (§7, §14) | Java, SQLite, trabajos en segundo plano | Construir y probar | – | **Hecha** |
| **3. Interfaz** (§15) | Angular: episodios, guion, producción, revisión, personajes, ajustes | Construir y probar | Probarla como usuario | **Hecha; falta tu prueba** |
| **4. IA** (§9) | Ollama propone guiones | Integrar y validar | Corregir los guiones | Pendiente |
| **5. Ampliación** | Episodios de 5 min, más personajes | Optimizar | Aprobar cada episodio | Pendiente |

No se pasa de fase sin tu aprobación.

---

## 4. Avance frente al documento

### Validaciones del piloto (§3.2)
| Requisito | Estado |
|---|---|
| Caminar, hablar, saltar y expresar emociones | Hecho con Bruno |
| Diálogo sincronizado con la boca | Hecho; falta tu opinión |
| MP4 reproducible de 1280 × 720 | Hecho y validado |
| Personajes con el mismo diseño | Hecho: mismos SVG en todas las escenas |
| Voces consistentes | Por referencia fija; falta tu elección |
| Escenarios reutilizables | Hecho: 7 lugares con 5 ambientes, y varios por episodio |

### Animaciones (§10.2)
- Todas las del documento están hechas (`run`, `think`, `sad`, `surprise` incluidas), más 20 extras

### Cambios técnicos frente al documento
1. Godot se ejecuta **con ventana**: con `--headless` se cierra con error (§5.2).
2. La duración sale de las voces, no del guion (§9).
3. La escena usa una línea de tiempo, no una sola animación por personaje (§7.4).
4. Rhubarb para los labios (no estaba en el documento).
5. Propuestos y **sin aprobar**: quitar FastAPI, Testcontainers, SonarQube y MapStruct; añadir Flyway; hacer obligatoria la aprobación del guion.

---

## 5. Antes de publicar (no hace falta todavía)

- [ ] Dar crédito a `sharvard` (CC BY 3.0) si se usa para Luna.
- [ ] Indicar que las voces son hechas con IA si la plataforma lo exige.
- [ ] Guardar la licencia de cada música o efecto que se agregue.
- [ ] Revisar las normas vigentes de YouTube sobre contenido para niños y contenido hecho con IA.
- [ ] Ver el episodio completo y aprobarlo tú. Nunca se publica solo.

Hoy el dibujo, el bosque y la música son propios del proyecto, y la letra de los subtítulos (Andika) es libre (OFL).

---

## 6. Si algo falla

| Mensaje | Qué hacer |
|---|---|
| `Falta godot_console / ffmpeg` | Abre una terminal nueva |
| `Animación desconocida` / `Personaje desconocido` | Revisa el nombre en el guion |
| `JSONDecodeError` | Falta o sobra una coma o comilla en el JSON |
| `Godot reportó errores` | Lee `media\episodes\piloto-01\logs\godot.log` o pégamelo |
