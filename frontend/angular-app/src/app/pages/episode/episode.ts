import { ChangeDetectionStrategy, Component, DestroyRef, HostListener, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subscription, catchError, firstValueFrom, of, switchMap, takeWhile, timer } from 'rxjs';
import { EpisodeApi, ProductionApi, StoryApi, errorMessage } from '../../core/api';
import { Episode, EpisodeEdit, Script, isActive } from '../../core/models';
import { SeriesStore } from '../../core/series.store';
import { ConfirmService } from '../../shared/confirm.service';
import { StatusChip } from '../../shared/status-chip';
import { ToastService } from '../../shared/toast.service';
import { ProductionPanel } from './production-panel/production-panel';
import { PlacePicker } from '../../shared/place-picker';
import { ReviewPanel } from './review-panel/review-panel';
import { ScriptEditor } from './script-editor/script-editor';
import { SavesBeforeLeaving } from './unsaved.guard';

type Tab = 'guion' | 'video' | 'revision';

@Component({
  selector: 'app-episode',
  imports: [FormsModule, RouterLink, StatusChip, PlacePicker, ScriptEditor, ProductionPanel, ReviewPanel],
  templateUrl: './episode.html',
  styleUrl: './episode.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EpisodePage implements SavesBeforeLeaving {
  readonly id = input.required<string>();

  protected readonly store = inject(SeriesStore);
  private readonly api = inject(EpisodeApi);
  private readonly production = inject(ProductionApi);
  private readonly story = inject(StoryApi);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly beforeSuggestion = signal<Script | null>(null);
  protected readonly episode = signal<Episode | null>(null);
  protected readonly notFound = signal(false);
  protected readonly draft = signal<EpisodeEdit | null>(null);
  protected readonly tab = signal<Tab>('guion');
  protected readonly saveState = signal<'saved' | 'dirty' | 'saving' | 'error'>('saved');
  protected readonly busy = signal(false);
  protected readonly running = computed(() => isActive(this.episode()?.job ?? null));
  protected readonly canProduce = computed(() => {
    const e = this.episode();
    return !!e?.editable && e.problems.length === 0 && this.saveState() === 'saved' && !this.busy();
  });

  private saveTimer?: ReturnType<typeof setTimeout>;
  private saving?: Promise<boolean>;
  private polling?: Subscription;

  constructor() {
    effect(() => {
      const id = this.id();
      untracked(() => this.open(id));
    });
    this.destroyRef.onDestroy(() => clearTimeout(this.saveTimer));
  }

  @HostListener('window:beforeunload', ['$event'])
  protected warn(event: BeforeUnloadEvent): void {
    if (this.saveState() !== 'saved') {
      event.preventDefault();
    }
  }

  async canLeave(): Promise<boolean> {
    if (this.saveState() === 'saved' || (await this.flush())) {
      return true;
    }
    return this.confirm.ask({ title: 'Hay cambios sin guardar', text: 'No pude guardarlos. Si sales ahora se pierden.', yes: 'Salir sin guardar', danger: true });
  }

  protected change(part: Partial<EpisodeEdit>): void {
    const current = this.draft();
    if (!current) {
      return;
    }
    this.draft.set({ ...current, ...part });
    this.saveState.set('dirty');
    clearTimeout(this.saveTimer);
    this.saveTimer = setTimeout(() => void this.flush(), 700);
  }

  protected changeScript(script: Script): void {
    this.beforeSuggestion.set(null);
    this.change({ script });
  }

  protected suggest(): void {
    const draft = this.draft();
    if (!draft) {
      return;
    }
    this.story.suggest(draft.script, draft.environment).subscribe({
      next: (script) => {
        this.beforeSuggestion.set(draft.script);
        this.change({ script });
        this.toast.ok('Ajusté los movimientos y las caras según lo que dicen.');
      },
      error: (err) => this.toast.bad(errorMessage(err)),
    });
  }

  protected undoSuggestion(): void {
    const script = this.beforeSuggestion();
    if (script) {
      this.beforeSuggestion.set(null);
      this.change({ script });
    }
  }

  protected async start(): Promise<void> {
    if (!(await this.flush()) || this.busy()) {
      return;
    }
    this.busy.set(true);
    this.production.start(this.id()).subscribe({
      next: (job) => {
        this.busy.set(false);
        this.episode.update((e) => e && { ...e, status: 'IN_PRODUCTION', editable: false, job });
        this.tab.set('video');
        this.watch(job.id);
      },
      error: (err) => {
        this.busy.set(false);
        this.toast.bad(errorMessage(err));
        this.reload(false);
      },
    });
  }

  protected async cancel(): Promise<void> {
    const job = this.episode()?.job;
    if (job && (await this.confirm.ask({ title: '¿Detener el video?', text: 'Las voces que ya se crearon se conservan para la próxima vez.', yes: 'Detener', danger: true }))) {
      this.production.cancel(job.id).subscribe({ error: (err) => this.toast.bad(errorMessage(err)) });
    }
  }

  protected async approve(): Promise<void> {
    if (await this.confirm.ask({ title: '¿Aprobar el episodio?', text: 'Quedará terminado y ya no podrás modificarlo.', yes: 'Sí, aprobar' })) {
      this.review('APPROVE', '', '¡Episodio aprobado!');
    }
  }

  protected reject(comments: string): void {
    this.review('REJECT', comments, 'Listo. Corrige el guion y crea el video otra vez.');
  }

  protected export(): void {
    this.production.export(this.id()).subscribe({
      next: (r) => this.toast.ok(`Guardado en ${r.folder}`),
      error: (err) => this.toast.bad(errorMessage(err)),
    });
  }

  private review(decision: 'APPROVE' | 'REJECT', comments: string, done: string): void {
    this.busy.set(true);
    this.production.review(this.id(), decision, comments).subscribe({
      next: () => {
        this.busy.set(false);
        this.toast.ok(done);
        this.reload(false);
        if (decision === 'REJECT') {
          this.tab.set('guion');
        }
      },
      error: (err) => {
        this.busy.set(false);
        this.toast.bad(errorMessage(err));
      },
    });
  }

  private open(id: string): void {
    this.polling?.unsubscribe();
    this.api.get(id).subscribe({
      next: (e) => {
        this.show(e);
        this.tab.set(this.startTab(e));
        if (isActive(e.job)) {
          this.watch(e.job!.id);
        }
      },
      error: () => this.notFound.set(true),
    });
  }

  private reload(announce: boolean): void {
    this.api.get(this.id()).subscribe({
      next: (e) => {
        this.show(e);
        if (announce && e.status === 'PENDING_APPROVAL') {
          this.toast.ok('¡Tu video está listo!');
          this.tab.set('revision');
        }
      },
      error: (err) => this.toast.bad(errorMessage(err)),
    });
  }

  private show(e: Episode): void {
    this.episode.set(e);
    if (this.saveState() === 'saved') {
      this.draft.set({ title: e.title, idea: e.idea, environment: e.environment, script: e.script });
    }
  }

  private startTab(e: Episode): Tab {
    if (e.status === 'IN_PRODUCTION' || e.status === 'FAILED') {
      return 'video';
    }
    return e.status === 'PENDING_APPROVAL' || e.status === 'APPROVED' ? 'revision' : 'guion';
  }

  private watch(jobId: string): void {
    this.polling?.unsubscribe();
    this.polling = timer(800, 1500)
      .pipe(
        switchMap(() => this.production.job(jobId).pipe(catchError(() => of(this.episode()!.job!)))),
        takeWhile((job) => isActive(job), true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((job) => {
        this.episode.update((e) => e && { ...e, job });
        if (!isActive(job)) {
          this.reload(true);
        }
      });
  }

  /** Guarda de a una vez; si hubo más cambios mientras tanto, guarda otra vez. */
  private async flush(): Promise<boolean> {
    clearTimeout(this.saveTimer);
    if (this.saving) {
      await this.saving;
    }
    if (this.saveState() === 'saved') {
      return true;
    }
    this.saving = this.save();
    const ok = await this.saving;
    this.saving = undefined;
    return ok && this.saveState() === 'dirty' ? this.flush() : ok;
  }

  private async save(): Promise<boolean> {
    const draft = this.draft();
    if (!draft) {
      return true;
    }
    this.saveState.set('saving');
    try {
      const saved = await firstValueFrom(this.api.save(this.id(), draft));
      this.saveState.set(this.draft() === draft ? 'saved' : 'dirty');
      this.episode.set(saved);
      return true;
    } catch (err) {
      this.saveState.set('error');
      this.toast.bad(errorMessage(err));
      return false;
    }
  }
}
