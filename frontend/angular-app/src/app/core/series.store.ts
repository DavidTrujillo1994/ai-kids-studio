import { Injectable, computed, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { SeriesApi, SystemApi } from './api';
import { Catalog, Series, StoryCharacter } from './models';

/** Datos que comparten todas las pantallas: la serie, su reparto y lo que el motor sabe hacer. */
@Injectable({ providedIn: 'root' })
export class SeriesStore {
  private readonly seriesApi = inject(SeriesApi);
  private readonly systemApi = inject(SystemApi);

  private readonly seriesList = signal<Series[]>([]);
  readonly characters = signal<StoryCharacter[]>([]);
  readonly catalog = signal<Catalog>({ characters: [], environments: [], ambiences: [], props: [], animations: [], gaits: [] });
  readonly ready = signal(false);
  readonly failed = signal(false);
  readonly series = computed<Series | null>(() => this.seriesList()[0] ?? null);

  load(): void {
    this.failed.set(false);
    forkJoin({ series: this.seriesApi.all(), catalog: this.systemApi.catalog() }).subscribe({
      next: ({ series, catalog }) => {
        this.seriesList.set(series);
        this.catalog.set(catalog);
        if (series.length === 0) {
          this.ready.set(true);
          return;
        }
        this.loadCharacters();
      },
      error: () => this.failed.set(true),
    });
  }

  loadCharacters(): void {
    const series = this.series();
    if (series) {
      this.seriesApi.characters(series.id).subscribe({
        next: (list) => {
          this.characters.set(list);
          this.ready.set(true);
        },
        error: () => this.failed.set(true),
      });
    }
  }

  replaceCharacter(updated: StoryCharacter): void {
    this.characters.update((list) => list.map((c) => (c.id === updated.id ? updated : c)));
  }

  characterName(assetKey: string | undefined): string {
    return this.characters().find((c) => c.assetKey === assetKey)?.name
      ?? this.catalog().characters.find((c) => c.key === assetKey)?.name ?? assetKey ?? '';
  }

  expressionsOf(assetKey: string | undefined): string[] {
    return this.catalog().characters.find((c) => c.key === assetKey)?.expressions ?? ['normal'];
  }
}
