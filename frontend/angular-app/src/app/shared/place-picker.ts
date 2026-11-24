import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { placeUrl } from '../core/api';
import { ambienceLabel, environmentLabel } from '../core/labels';
import { SeriesStore } from '../core/series.store';

/** Elige un lugar por su imagen, y su hora o clima. */
@Component({
  selector: 'app-place-picker',
  template: `
    <div class="places" role="group" aria-label="Lugar">
      @for (e of store.catalog().environments; track e) {
        <button type="button" [disabled]="readonly()" [attr.aria-pressed]="environment() === e" (click)="environmentChange.emit(e)">
          <img [src]="placeUrl(e)" alt="" width="160" height="90" loading="lazy"><span>{{ environmentLabel(e) }}</span>
        </button>
      }
    </div>
    @if (store.catalog().ambiences.length) {
      <div class="moods" role="group" aria-label="Hora y clima">
        @for (a of store.catalog().ambiences; track a) {
          <button type="button" [disabled]="readonly()" [attr.aria-pressed]="ambience() === a" (click)="ambienceChange.emit(a)"><span aria-hidden="true">{{ ambienceLabel(a).icon }}</span> {{ ambienceLabel(a).label }}</button>
        }
      </div>
    }
  `,
  styles: `
    :host { display: grid; gap: 12px; }
    .places { display: grid; gap: 10px; grid-template-columns: repeat(auto-fill, minmax(128px, 1fr)); }
    button { border: 2px solid var(--line); border-radius: 12px; background: #fff; color: inherit; font: inherit; cursor: pointer; }
    button:disabled { cursor: default; opacity: .65; }
    button[aria-pressed=true] { border-color: var(--leaf); background: var(--leaf-soft); font-weight: 600; opacity: 1; }
    .places button { overflow: hidden; padding: 0 0 6px; }
    .places img { width: 100%; height: auto; aspect-ratio: 16 / 9; object-fit: cover; margin-bottom: 5px; background: #dcead9; }
    .moods { display: flex; flex-wrap: wrap; gap: 8px; }
    .moods button { min-height: 42px; padding: 0 14px; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PlacePicker {
  readonly environment = input.required<string>();
  readonly ambience = input.required<string>();
  readonly readonly = input(false);
  readonly environmentChange = output<string>();
  readonly ambienceChange = output<string>();

  protected readonly store = inject(SeriesStore);
  protected readonly placeUrl = placeUrl;
  protected readonly environmentLabel = environmentLabel;
  protected readonly ambienceLabel = ambienceLabel;
}
