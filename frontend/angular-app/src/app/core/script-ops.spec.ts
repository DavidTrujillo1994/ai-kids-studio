import { describe, expect, it } from 'vitest';
import { Script } from './models';
import { FLIGHTS, estimateSeconds, flightIndex, insertAt, move, newStep, placeLabel, removeAt, setCast, spread } from './script-ops';

const script: Script = {
  cast: [{ character: 'bruno', startX: 0 }],
  ambience: 'dia',
  steps: [
    { type: 'WALK', character: 'bruno', to: 390 },
    { type: 'SAY', character: 'bruno', text: '¡Hola a todos!' },
    { type: 'PAUSE', character: 'bruno', seconds: 2 },
  ],
};
const types = (s: Script) => s.steps.map((x) => x.type);

describe('script-ops', () => {
  it('mueve un paso sin tocar el guion original', () => {
    expect(types(move(script, 0, 2))).toEqual(['SAY', 'PAUSE', 'WALK']);
    expect(types(script)).toEqual(['WALK', 'SAY', 'PAUSE']);
  });

  it('ignora movimientos fuera de la lista', () => {
    expect(move(script, 0, -1)).toBe(script);
    expect(move(script, 2, 3)).toBe(script);
  });

  it('inserta y quita en la posición indicada', () => {
    const more = insertAt(script, 1, newStep('PLAY', 'bruno', '', ['jump']));
    expect(types(more)).toEqual(['WALK', 'PLAY', 'SAY', 'PAUSE']);
    expect(types(removeAt(more, 1))).toEqual(types(script));
  });

  it('usa una animación que existe aunque falte la preferida', () => {
    expect(newStep('SAY', 'bruno', '', ['idle']).animation).toBe('idle');
  });

  it('estima la duración', () => {
    expect(estimateSeconds(script)).toBe(6);
    expect(estimateSeconds({ cast: [], ambience: 'dia', steps: [] })).toBe(0);
  });

  it('al cambiar de lugar reparte al elenco sin encimarlo', () => {
    const cast = [{ character: 'bruno', startX: 0 }, { character: 'luna', startX: 0 }];
    expect(spread(cast).map((c) => c.startX)).toEqual([440, 840]);
    const scene = newStep('SCENE', '', '', [], 'lago', cast);
    const out = setCast({ ...script, steps: [scene] }, [cast[1]]);
    expect(out.steps[0].positions!.map((p) => p.character)).toEqual(['luna']);
  });

  it('correr dura menos que caminar', () => {
    const walk = (gait: string): Script => ({ cast: [{ character: 'bruno', startX: 0 }], ambience: 'dia', steps: [{ type: 'WALK', character: 'bruno', to: 1500, gait }] });
    expect(estimateSeconds(walk('run'))).toBeLessThan(estimateSeconds(walk('walk')));
  });

  it('reconoce lugares y recorridos conocidos', () => {
    expect(placeLabel(520)).toBe('Centro');
    expect(flightIndex(FLIGHTS[1].path)).toBe(1);
    expect(flightIndex([{ second: 0, x: 1, y: 1 }])).toBe(-1);
  });
});
