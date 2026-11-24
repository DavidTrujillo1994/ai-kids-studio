import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { Episode, EpisodeStatus } from '../../../core/models';
import { ReviewPanel } from './review-panel';

const episode = (status: EpisodeStatus): Episode => ({
  id: '1', title: 'Prueba', idea: '', status, hasVideo: true, createdAt: '2026-11-23T00:00:00Z', updatedAt: '2026-11-23T00:00:00Z', seriesId: 's',
  environment: 'bosque-central', editable: true, script: { cast: [], ambience: 'dia', steps: [] }, problems: [], reviews: [], job: null,
});

describe('ReviewPanel', () => {
  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient()] }));

  function render(status: EpisodeStatus) {
    const fixture = TestBed.createComponent(ReviewPanel);
    fixture.componentRef.setInput('episode', episode(status));
    fixture.detectChanges();
    const find = (name: string) => fixture.nativeElement.querySelector(`[data-test=${name}]`) as HTMLButtonElement | null;
    return { fixture, find };
  }

  it('solo permite aprobar cuando el video espera aprobación', () => {
    expect(render('PENDING_APPROVAL').find('approve')).not.toBeNull();
    for (const status of ['SCRIPT_READY', 'REJECTED', 'APPROVED', 'FAILED'] as EpisodeStatus[]) {
      expect(render(status).find('approve')).toBeNull();
    }
  });

  it('solo exporta lo aprobado', () => {
    expect(render('APPROVED').find('export')).not.toBeNull();
    expect(render('PENDING_APPROVAL').find('export')).toBeNull();
  });

  it('pedir cambios exige decir qué mejorar', () => {
    const { fixture, find } = render('PENDING_APPROVAL');
    const sent: string[] = [];
    fixture.componentInstance.reject.subscribe((text) => sent.push(text));
    find('reject')!.click();
    fixture.detectChanges();
    expect(find('send')!.disabled).toBe(true);
    const area = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    area.value = 'Muy rápido';
    area.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    expect(find('send')!.disabled).toBe(false);
    find('send')!.click();
    expect(sent).toEqual(['Muy rápido']);
  });
});
