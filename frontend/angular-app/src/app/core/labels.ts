import { EpisodeStatus, Stage, StepType } from './models';

type Tone = '' | 'ok' | 'info' | 'warn' | 'bad';

export const STATUS: Record<EpisodeStatus, { label: string; tone: Tone }> = {
  DRAFT: { label: 'Borrador', tone: '' },
  SCRIPT_READY: { label: 'Guion listo', tone: 'info' },
  IN_PRODUCTION: { label: 'Creando video', tone: 'info' },
  PENDING_APPROVAL: { label: 'Esperando tu aprobación', tone: 'warn' },
  APPROVED: { label: 'Aprobado', tone: 'ok' },
  REJECTED: { label: 'Por corregir', tone: 'bad' },
  FAILED: { label: 'Falló el video', tone: 'bad' },
};

export const STEP: Record<StepType, { label: string; icon: string; help: string }> = {
  SAY: { label: 'Habla', icon: '💬', help: 'El personaje dice una frase' },
  WALK: { label: 'Se mueve', icon: '👣', help: 'Camina, corre, vuela…' },
  PLAY: { label: 'Acción', icon: '⭐', help: 'Salta, baila, saluda…' },
  PAUSE: { label: 'Pausa', icon: '⏸️', help: 'Espera unos segundos' },
  APPEAR: { label: 'Objeto', icon: '🦋', help: 'Algo cruza la escena' },
  SCENE: { label: 'Cambia de lugar', icon: '🗺️', help: 'La historia sigue en otro sitio' },
};

const ANIMATIONS: Record<string, string> = {
  idle: 'Quieto', walk: 'Caminar', wave: 'Saludar', talk: 'Conversar', point: 'Señalar', jump: 'Saltar', laugh: 'Reír', dance: 'Bailar',
  run: 'Correr', hop: 'Ir a saltitos', fly: 'Volar', sneak: 'Ir en puntillas', swim: 'Nadar', think: 'Pensar', sad: 'Ponerse triste', surprise: 'Sorprenderse',
  celebrate: 'Celebrar', clap: 'Aplaudir', yawn: 'Bostezar', sleep: 'Dormir', sit: 'Sentarse', shrug: 'Encoger hombros', look: 'Buscar con la mirada',
  shy: 'Ser tímido', eat: 'Comer', pickup: 'Recoger algo', scared: 'Tener miedo', hug: 'Abrazar', angry: 'Enojarse', spin: 'Girar', kick: 'Patear',
};

const EXPRESSIONS: Record<string, { label: string; icon: string }> = {
  normal: { label: 'Normal', icon: '🙂' },
  happy: { label: 'Feliz', icon: '😄' },
  sad: { label: 'Triste', icon: '😢' },
  surprised: { label: 'Sorpresa', icon: '😮' },
  picaro: { label: 'Pícaro', icon: '😏' },
  scared: { label: 'Miedo', icon: '😨' },
  angry: { label: 'Enojo', icon: '😠' },
  sleepy: { label: 'Sueño', icon: '😴' },
};

export const STAGES: { stage: Stage; label: string; help: string }[] = [
  { stage: 'VOICES', label: 'Voces', help: 'Cerca de un minuto por frase nueva' },
  { stage: 'ANIMATION', label: 'Animación', help: 'Se abre una ventana que se cierra sola' },
  { stage: 'ASSEMBLY', label: 'Montaje', help: 'Música y subtítulos' },
  { stage: 'VALIDATION', label: 'Revisión', help: 'Comprobación del archivo' },
];

const PLACES: Record<string, string> = { 'bosque-central': 'Bosque', montana: 'Montaña' };

const AMBIENCES: Record<string, { label: string; icon: string }> = {
  dia: { label: 'Día', icon: '☀️' },
  atardecer: { label: 'Atardecer', icon: '🌇' },
  noche: { label: 'Noche', icon: '🌙' },
  lluvia: { label: 'Lluvia', icon: '🌧️' },
  nieve: { label: 'Nieve', icon: '❄️' },
};

const PROPS: Record<string, string> = { pajaro: 'Pájaro' };

/** Un recurso nuevo sin nombre bonito se muestra igual, con su propio identificador. */
export const pretty = (key: string): string => {
  const text = key.replaceAll('-', ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
};

export const environmentLabel = (key: string): string => PLACES[key] ?? pretty(key);
export const propLabel = (key: string): string => PROPS[key] ?? pretty(key);
export const ambienceLabel = (key: string): { label: string; icon: string } => AMBIENCES[key] ?? { label: pretty(key), icon: '🌈' };
export const animationLabel = (key: string): string => ANIMATIONS[key] ?? pretty(key);
export const expressionLabel = (key: string): { label: string; icon: string } => EXPRESSIONS[key] ?? { label: pretty(key), icon: '🎭' };
