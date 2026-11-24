import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { EpisodeApi, StoryApi, errorMessage, placeUrl, portraitUrl } from '../../core/api';
import { StoryTemplate } from '../../core/models';
import { SeriesStore } from '../../core/series.store';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-new-episode',
  imports: [FormsModule, RouterLink],
  templateUrl: './new-episode.html',
  styleUrl: './new-episode.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NewEpisode {
  protected readonly store = inject(SeriesStore);
  private readonly api = inject(EpisodeApi);
  private readonly story = inject(StoryApi);
  private readonly router = inject(Router);
  private readonly toast = inject(ToastService);

  protected readonly placeUrl = placeUrl;
  protected readonly portraitUrl = portraitUrl;
  protected readonly templates = signal<StoryTemplate[]>([]);
  protected readonly template = signal<StoryTemplate | null>(null);
  protected readonly cast = signal<string[]>([]);
  protected readonly title = signal('');
  protected readonly idea = signal('');
  protected readonly saving = signal(false);
  private titleTouched = false;

  protected readonly missing = computed(() => Math.max(0, (this.template()?.roles ?? 0) - this.cast().length));
  protected readonly ready = computed(() => !!this.template() && this.missing() === 0 && !!this.title().trim() && !this.saving());

  constructor() {
    this.cast.set(this.store.characters().filter((c) => c.role === 'PROTAGONISTA').slice(0, 2).map((c) => c.assetKey));
    this.story.templates().subscribe({ next: (list) => this.templates.set(list), error: (err) => this.toast.bad(errorMessage(err)) });
  }

  protected choose(t: StoryTemplate): void {
    this.template.set(t);
    this.cast.update((list) => list.slice(0, t.roles));
    if (!this.titleTouched) {
      this.title.set(t.id === 'libre' ? '' : t.name);
    }
  }

  protected setTitle(value: string): void {
    this.titleTouched = value.trim() !== '';
    this.title.set(value);
  }

  protected order(key: string): number {
    return this.cast().indexOf(key) + 1;
  }

  /** Con el reparto lleno, elegir otro reemplaza al último. */
  protected toggle(key: string): void {
    const list = this.cast();
    const max = this.template()?.roles ?? 2;
    if (list.includes(key)) {
      this.cast.set(list.filter((k) => k !== key));
    } else {
      this.cast.set([...list.slice(0, max - 1), key]);
    }
  }

  protected create(): void {
    const series = this.store.series();
    const template = this.template();
    if (!series || !template || !this.ready()) {
      return;
    }
    this.saving.set(true);
    this.api.create(series.id, { title: this.title(), idea: this.idea(), environment: template.environment, template: template.id, cast: this.cast() }).subscribe({
      next: (e) => this.router.navigate(['/episodios', e.id]),
      error: (err) => {
        this.saving.set(false);
        this.toast.bad(errorMessage(err));
      },
    });
  }
}
