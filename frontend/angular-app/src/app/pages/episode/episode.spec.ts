import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Episode } from '../../core/models';
import { EpisodePage } from './episode';

const base: Episode = {
  id: '1', title: 'Prueba', idea: '', status: 'SCRIPT_READY', hasVideo: false, createdAt: '2026-11-23T00:00:00Z', updatedAt: '2026-11-23T00:00:00Z',
  seriesId: 's', environment: 'bosque-central', editable: true, script: { cast: [{ character: 'bruno', startX: 0 }], ambience: 'dia', steps: [{ type: 'SAY', character: 'bruno', text: 'Hola', animation: 'talk', expression: 'normal' }] },
  problems: [], reviews: [], job: null,
};
const URL = '/api/v1/episodes/1';

describe('EpisodePage', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => vi.useRealTimers());

  function open(episode: Episode) {
    const fixture = TestBed.createComponent(EpisodePage);
    fixture.componentRef.setInput('id', '1');
    fixture.detectChanges();
    http.expectOne(URL).flush(episode);
    fixture.detectChanges();
    const root = fixture.nativeElement as HTMLElement;
    const button = (text: string) => [...root.querySelectorAll('button')].find((b) => b.textContent?.trim() === text) as HTMLButtonElement | undefined;
    const type = async (selector: string, value: string) => {
      await vi.advanceTimersByTimeAsync(0);
      fixture.detectChanges();
      const field = root.querySelector(selector) as HTMLInputElement;
      field.value = value;
      field.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    };
    return { fixture, root, button, type };
  }

  it('guarda solo, una vez, después de dejar de escribir', async () => {
    const { type, root, fixture } = open(base);
    await type('textarea[aria-label="Lo que dice"]', 'Hola a');
    await vi.advanceTimersByTimeAsync(300);
    await type('textarea[aria-label="Lo que dice"]', 'Hola a todos');
    http.expectNone(URL);
    await vi.advanceTimersByTimeAsync(800);
    const save = http.expectOne((r) => r.method === 'PUT' && r.url === URL);
    expect(save.request.body.script.steps[0].text).toBe('Hola a todos');
    save.flush({ ...base, script: save.request.body.script });
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();
    expect(root.textContent).toContain('Guardado');
  });

  it('no pierde lo que se escribe mientras se está guardando', async () => {
    const { type } = open(base);
    await type('input.title', 'Uno');
    await vi.advanceTimersByTimeAsync(800);
    const first = http.expectOne((r) => r.method === 'PUT');
    await type('input.title', 'Dos');
    first.flush({ ...base, title: 'Uno' });
    await vi.advanceTimersByTimeAsync(800);
    expect(http.expectOne((r) => r.method === 'PUT').request.body.title).toBe('Dos');
  });

  it('no deja crear el video si el guion tiene problemas', () => {
    expect(open({ ...base, problems: [{ step: 0, message: 'Escribe lo que dice el personaje.' }] }).button('Crear video')!.disabled).toBe(true);
    expect(open(base).button('Crear video')!.disabled).toBe(false);
  });

  it('sugiere movimientos y permite deshacer', async () => {
    const { button, root, fixture } = open(base);
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();
    button('✨ Sugerir movimientos')!.click();
    const suggested = { ...base.script, steps: [{ ...base.script.steps[0], animation: 'wave' }] };
    http.expectOne('/api/v1/movement-suggestions').flush(suggested);
    await vi.advanceTimersByTimeAsync(800);
    const save = http.expectOne((r) => r.method === 'PUT');
    expect(save.request.body.script.steps[0].animation).toBe('wave');
    save.flush({ ...base, script: suggested });
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();
    button('Deshacer sugerencia')!.click();
    await vi.advanceTimersByTimeAsync(800);
    expect(http.expectOne((r) => r.method === 'PUT').request.body.script).toEqual(base.script);
    expect(root).toBeTruthy();
  });

  it('un episodio aprobado no se puede editar', async () => {
    const { root, button } = open({ ...base, status: 'APPROVED', editable: false, hasVideo: true });
    await vi.advanceTimersByTimeAsync(0); // ngModel aplica «disabled» en el siguiente turno
    expect((root.querySelector('input.title') as HTMLInputElement).disabled).toBe(true);
    expect(button('Crear video')).toBeUndefined();
    expect(button('Exportar con subtítulos')).toBeDefined();
  });
});
