# Arquitectura

```text
Navegador (Angular) ──REST──> Java (Spring Boot) ──proceso──> producir.py ──> Chatterbox, Rhubarb, Godot, FFmpeg
                                   │                              │
                                SQLite (data/)                 media/
```

## Módulos del backend (hexagonal)

| Módulo | Contiene | Puede depender de |
|---|---|---|
| `domain` | Serie, personaje, episodio, guion, trabajo y sus reglas | Nada (solo Java) |
| `application` | Casos de uso y puertos | `domain` |
| `infrastructure` | SQLite/JPA, archivos, motor de video | `application`, `domain` |
| `entrypoints` | API REST, errores, pantallas compiladas | `application`, `domain` |
| `bootstrap` | Conecta todo y arranca | Todos |

`ArchitectureTest` (ArchUnit) falla la compilación si alguien rompe estos límites.

## Dónde está cada principio SOLID

| Principio | Ejemplo |
|---|---|
| **S** Responsabilidad única | `EpisodeService` edita, `ProductionService` produce, `ReviewService` aprueba y exporta |
| **O** Abierto/cerrado | Cada tipo de `Step` valida sus propias reglas. El catálogo se lee de `media/`: un personaje o animación nueva aparece sin tocar código |
| **L** Sustitución | El motor real y el simulado de las pruebas cumplen `VideoProducerPort` |
| **I** Interfaces pequeñas | `StartProduction`, `CancelProduction`, `ApproveCharacterDesign`… cada controlador recibe solo lo que usa |
| **D** Inversión de dependencias | Los casos de uso conocen puertos; `UseCaseConfig` es el único lugar que los une con sus adaptadores |

## Ciclo de un episodio

```text
DRAFT -> SCRIPT_READY -> IN_PRODUCTION -> PENDING_APPROVAL -> APPROVED
              ^               |  |                |
              |  (cancelar) --+  +--> FAILED      +--> REJECTED
              +-------------------- corregir ----------+
```

- Nada llega a `APPROVED` sin la decisión explícita de una persona y un video existente.
- Cambiar el guion después de producir invalida el video; cambiar el título, no.
- Un trabajo a la vez. Si la aplicación se cierra, el trabajo queda como fallido y se puede reintentar.
- El video anterior solo se reemplaza cuando el nuevo pasa la validación.
- Las voces se guardan por contenido: mover o insertar frases no regenera las demás.

## Cómo agregar variedad

Todo esto aparece solo en la aplicación, sin programar:

| Qué | Dónde | Cómo |
|---|---|---|
| Historia | `media/plantillas.json` | Copiar una y cambiarla. Los papeles son `rol-1`, `rol-2`; `{1}` y `{2}` son sus nombres |
| Regla de movimiento | `media/direccion.json` | Palabras -> animación, cara y forma de desplazarse |
| Movimiento | `animation-engine/arte/movimientos.py` | Agregar a `NEW` y ejecutar. Con `gait` sirve para desplazarse |
| Personaje | `animation-engine/arte/personajes.py` | Agregar a `CAST` (colores, orejas, cola, voz), ejecutar y poner su `voz-referencia.wav` |
| Lugar u objeto | `animation-engine/arte/lugares.py` | Agregar y ejecutar. No toca los que ya existen |
| Hora o clima | `media/environments/ambientes.json` | Color y partículas |

Después: `.venv\Scripts\python.exe video-engine\scripts\miniaturas.py` crea las imágenes que faltan.
`MediaIntegrityTest` avisa si una historia o regla pide algo que no existe.

## Seguridad local
Solo escucha en `127.0.0.1`, rechaza otros nombres de host, no abre CORS, y los identificadores de recursos no admiten rutas.
El texto de la persona nunca viaja por la línea de comandos.

## Comandos

    backend\ai-kids-backend> gradlew build          compila y prueba (52 pruebas)
    frontend\angular-app>    npm test -- --watch=false   (18 pruebas)
    frontend\angular-app>    npm run build

`Iniciar.cmd` usa lo ya compilado: después de cambiar código, vuelve a ejecutar `gradlew build` o `npm run build`.
Documentación de la API con la aplicación abierta: http://127.0.0.1:8095/swagger-ui.html

## Decisiones y límites
- Java compila para la versión 21 (LTS) y corre en el Java 23 instalado.
- Angular 21: la 22 pide un Node más nuevo que 22.19.
- Progreso por consulta periódica (permitido para el MVP en §5.2).
- `Iniciar.cmd` y la vigilancia del proceso son solo para Windows.
- Pendiente: guion con IA (Ollama), ajustar y escuchar voces desde la aplicación, varias series en pantalla, copia de seguridad, subir dibujos desde la aplicación.
