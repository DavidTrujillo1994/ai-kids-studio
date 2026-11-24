import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { Subscription, catchError, of, switchMap, takeWhile, timer } from 'rxjs';
import { EpisodeApi, ProductionApi, errorMessage } from '../../core/api';
import { EpisodeSummary, Job } from '../../core/models';
import { SeriesStore } from '../../core/series.store';
import { ConfirmService } from '../../shared/confirm.service';
import { StatusChip } from '../../shared/status-chip';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-home',
  imports: [RouterLink, StatusChip, DatePipe],
  templateUrl: './home.html',
  styleUrl: './home.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Home {
  protected readonly store = inject(SeriesStore);
  protected readonly api = inject(EpisodeApi);
  private readonly production = inject(ProductionApi);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly episodes = signal<EpisodeSummary[] | null>(null);
  protected readonly jobs = signal<Job[]>([]);
  protected readonly pending = computed(() => this.episodes()?.filter((e) => e.status === 'PENDING_APPROVAL').length ?? 0);
  private polling?: Subscription;

  constructor() {
    this.load();
  }

  protected jobOf(e: EpisodeSummary): Job | undefined {
    return this.jobs().find((j) => j.episodeId === e.id);
  }

  protected async remove(e: EpisodeSummary): Promise<void> {
    const yes = await this.confirm.ask({
      title: `¿Quitar «${e.title}»?`,
      text: 'Desaparece de la lista. Los archivos que ya se crearon quedan guardados en tu computador.',
      yes: 'Quitar',
      danger: true,
    });
    if (yes) {
      this.api.remove(e.id).subscribe({ next: () => this.load(), error: (err) => this.toast.bad(errorMessage(err)) });
    }
  }

  private load(): void {
    const series = this.store.series();
    if (!series) {
      this.episodes.set([]);
      return;
    }
    this.api.ofSeries(series.id).subscribe({
      next: (list) => {
        this.episodes.set(list);
        if (list.some((e) => e.status === 'IN_PRODUCTION')) {
          this.watch();
        }
      },
      error: (err) => {
        this.episodes.set([]);
        this.toast.bad(errorMessage(err));
      },
    });
  }

  private watch(): void {
    this.polling?.unsubscribe();
    this.polling = timer(0, 2000)
      .pipe(
        switchMap(() => this.production.active().pipe(catchError(() => of(this.jobs())))),
        takeWhile((jobs) => jobs.length > 0, true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (jobs) => {
          const before = this.jobs().length;
          this.jobs.set(jobs);
          if (jobs.length < before) {
            this.load();
          }
        },
      });
  }
}
