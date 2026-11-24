import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SeriesApi, SystemApi, errorMessage } from '../../core/api';
import { Tool } from '../../core/models';
import { SeriesStore } from '../../core/series.store';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-settings',
  imports: [FormsModule],
  template: `
    <div class="page stack">
      <h1>Ajustes</h1>
      <form class="card stack" (ngSubmit)="save()">
        <h2>Tu serie</h2>
        <label class="field">Nombre <input type="text" name="name" maxlength="80" [ngModel]="name()" (ngModelChange)="name.set($event)"></label>
        <label class="field">Descripción <textarea name="description" rows="2" maxlength="500" [ngModel]="description()" (ngModelChange)="description.set($event)"></textarea></label>
        <div><button class="btn primary" type="submit" [disabled]="!name().trim()">Guardar</button></div>
      </form>

      <div class="card stack">
        <div class="spread"><h2>Herramientas</h2><button class="btn" type="button" [disabled]="checking()" (click)="check()">{{ checking() ? 'Revisando…' : 'Revisar otra vez' }}</button></div>
        @if (tools().length && !checking()) {
          <div class="notice" [class.ok]="allReady()" [class.bad]="!allReady()">{{ allReady() ? 'Todo está listo para crear videos.' : 'Falta algo: no se podrán crear videos hasta corregirlo.' }}</div>
        }
        <ul>
          @for (t of tools(); track t.name) {
            <li><span class="chip" [class.ok]="t.ready" [class.bad]="!t.ready">{{ t.ready ? '✓ Listo' : '✕ Falta' }}</span><div><b>{{ t.name }}</b><p class="small muted">{{ t.purpose }} · {{ t.detail }}</p></div></li>
          }
        </ul>
      </div>
      <p class="small muted">Todo se guarda en tu computador: nada se publica ni se envía a internet.</p>
    </div>
  `,
  styles: `ul { display: grid; gap: 12px; margin: 0; padding: 0; list-style: none; } li { display: flex; gap: 14px; align-items: flex-start; }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Settings {
  private readonly store = inject(SeriesStore);
  private readonly seriesApi = inject(SeriesApi);
  private readonly system = inject(SystemApi);
  private readonly toast = inject(ToastService);

  protected readonly name = signal(this.store.series()?.name ?? '');
  protected readonly description = signal(this.store.series()?.description ?? '');
  protected readonly tools = signal<Tool[]>([]);
  protected readonly checking = signal(false);
  protected readonly allReady = computed(() => this.tools().every((t) => t.ready));

  constructor() {
    this.check();
  }

  protected check(): void {
    this.checking.set(true);
    this.system.tools().subscribe({
      next: (tools) => {
        this.tools.set(tools);
        this.checking.set(false);
      },
      error: (err) => {
        this.checking.set(false);
        this.toast.bad(errorMessage(err));
      },
    });
  }

  protected save(): void {
    const series = this.store.series();
    if (series) {
      this.seriesApi.update(series.id, this.name(), this.description()).subscribe({
        next: () => {
          this.store.load();
          this.toast.ok('Guardado');
        },
        error: (err) => this.toast.bad(errorMessage(err)),
      });
    }
  }
}
