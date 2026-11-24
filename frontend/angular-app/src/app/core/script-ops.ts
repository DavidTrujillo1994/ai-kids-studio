import { CastMember, PathPoint, Script, Step, StepType } from './models';

/** Operaciones puras sobre el guion: nunca modifican el original. */

export const PLACES: { label: string; x: number }[] = [
  { label: 'Fuera, a la izquierda', x: -170 },
  { label: 'Izquierda', x: 250 },
  { label: 'Centro', x: 640 },
  { label: 'Derecha', x: 1000 },
  { label: 'Fuera, a la derecha', x: 1500 },
];

export const FLIGHTS: { label: string; path: PathPoint[] }[] = [
  { label: 'Cruza la escena', path: [{ second: 0, x: 1500, y: 180 }, { second: 2, x: 950, y: 300 }, { second: 4, x: 450, y: 220 }, { second: 6, x: -150, y: 120 }] },
  { label: 'Se queda cerca y se va', path: [{ second: 0, x: 1400, y: 180 }, { second: 1.2, x: 1050, y: 300 }, { second: 2.8, x: 870, y: 250 }, { second: 8, x: 900, y: 270 }, { second: 10, x: 1500, y: 60 }] },
  { label: 'Se queda en el suelo', path: [{ second: 0, x: 980, y: 580 }, { second: 60, x: 980, y: 580 }] },
  { label: 'Cae desde arriba', path: [{ second: 0, x: 860, y: -40 }, { second: 5, x: 760, y: 600 }] },
  { label: 'Cruza por el suelo', path: [{ second: 0, x: -100, y: 600 }, { second: 4.5, x: 1450, y: 600 }] },
  { label: 'Pasa muy rápido', path: [{ second: 0, x: -150, y: 260 }, { second: 2, x: 1500, y: 160 }] },
];

export function placeLabel(x: number): string {
  const place = PLACES.reduce((a, b) => (Math.abs(b.x - x) < Math.abs(a.x - x) ? b : a));
  return place.label;
}

export function flightIndex(path: PathPoint[]): number {
  const text = JSON.stringify(path.map((p) => [p.second, p.x, p.y]));
  return FLIGHTS.findIndex((f) => JSON.stringify(f.path.map((p) => [p.second, p.x, p.y])) === text);
}

export function newStep(type: StepType, character: string, prop: string, animations: string[], environment = '', cast: CastMember[] = []): Step {
  const pick = (wanted: string) => (animations.includes(wanted) ? wanted : 'idle');
  switch (type) {
    case 'SAY': return { type, character, text: '', animation: pick('talk'), expression: 'normal' };
    case 'WALK': return { type, character, to: 640, gait: 'walk', expression: 'normal' };
    case 'PLAY': return { type, character, animation: pick('jump'), expression: 'happy' };
    case 'PAUSE': return { type, character, seconds: 1, animation: 'idle', expression: 'normal' };
    case 'APPEAR': return { type, prop, path: FLIGHTS[0].path };
    case 'SCENE': return { type, environment, ambience: 'dia', positions: spread(cast) };
  }
}

/** Reparte a los personajes por la escena, sin encimarlos. */
export function spread(cast: CastMember[]): CastMember[] {
  return cast.map((c, i) => ({ character: c.character, startX: Math.round(640 + (i - (cast.length - 1) / 2) * 400) }));
}

export const insertAt = (script: Script, index: number, step: Step): Script => ({ ...script, steps: [...script.steps.slice(0, index), step, ...script.steps.slice(index)] });
export const replaceAt = (script: Script, index: number, step: Step): Script => ({ ...script, steps: script.steps.map((s, i) => (i === index ? step : s)) });
export const removeAt = (script: Script, index: number): Script => ({ ...script, steps: script.steps.filter((_, i) => i !== index) });

export function move(script: Script, from: number, to: number): Script {
  if (to < 0 || to >= script.steps.length) {
    return script;
  }
  const steps = [...script.steps];
  steps.splice(to, 0, ...steps.splice(from, 1));
  return { ...script, steps };
}

/** Quien sale del reparto también sale de las posiciones de cada lugar. */
export function setCast(script: Script, cast: CastMember[]): Script {
  const keys = new Set(cast.map((c) => c.character));
  const steps = script.steps.map((s) => (s.type === 'SCENE' ? { ...s, positions: (s.positions ?? []).filter((p) => keys.has(p.character)) } : s));
  return { ...script, cast, steps };
}

const PACE: Record<string, number> = { run: 1.9, fly: 1.5, hop: 1.15, sneak: 0.5, swim: 0.9 };

/** Estimado para orientar: la duración real sale de las voces. */
export function estimateSeconds(script: Script): number {
  const x = new Map(script.cast.map((c) => [c.character, c.startX]));
  let total = 0;
  for (const s of script.steps) {
    if (s.type === 'SAY') {
      total += 0.9 + (s.text?.trim().split(/\s+/).filter(Boolean).length ?? 0) * 0.32;
    } else if (s.type === 'WALK' && s.character !== undefined) {
      total += Math.abs((s.to ?? 0) - (x.get(s.character) ?? 0)) / (195 * (PACE[s.gait ?? 'walk'] ?? 1));
      x.set(s.character, s.to ?? 0);
    } else if (s.type === 'PLAY') {
      total += 0.9;
    } else if (s.type === 'PAUSE') {
      total += s.seconds ?? 1;
    } else if (s.type === 'SCENE') {
      total += 0.8;
      for (const p of s.positions ?? []) {
        x.set(p.character, p.startX);
      }
    }
  }
  return Math.round(total);
}
