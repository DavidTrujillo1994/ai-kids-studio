import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SeriesApi, errorMessage, portraitUrl } from '../../core/api';
import { StoryCharacter } from '../../core/models';
import { SeriesStore } from '../../core/series.store';
import { expressionLabel } from '../../core/labels';
import { ConfirmService } from '../../shared/confirm.service';
import { ToastService } from '../../shared/toast.service';

@Component({
  selector: 'app-characters',
  imports: [FormsModule],
  templateUrl: './characters.html',
  styleUrl: './characters.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Characters {
  protected readonly store = inject(SeriesStore);
  private readonly api = inject(SeriesApi);
  private readonly confirm = inject(ConfirmService);
  private readonly toast = inject(ToastService);

  protected readonly expressionLabel = expressionLabel;
  protected readonly portraitUrl = portraitUrl;
  protected readonly editing = signal<{ id: string; name: string; personality: string } | null>(null);
  protected readonly available = computed(() => this.store.catalog().characters.filter((a) => !this.store.characters().some((c) => c.assetKey === a.key)));

  protected edit(c: StoryCharacter): void {
    this.editing.set({ id: c.id, name: c.name, personality: c.personality });
  }

  protected patch(part: { name?: string; personality?: string }): void {
    this.editing.update((e) => e && { ...e, ...part });
  }

  protected save(): void {
    const e = this.editing();
    if (e) {
      this.api.updateCharacter(e.id, e.name, e.personality).subscribe({
        next: (c) => {
          this.store.replaceCharacter(c);
          this.editing.set(null);
        },
        error: (err) => this.toast.bad(errorMessage(err)),
      });
    }
  }

  protected async approve(c: StoryCharacter): Promise<void> {
    if (await this.confirm.ask({ title: `¿Aprobar el diseño de ${c.name}?`, text: 'Su ficha queda bloqueada. Si el dibujo cambia, llegará como una versión nueva para aprobar.', yes: 'Aprobar diseño' })) {
      this.api.approveDesign(c.id).subscribe({
        next: (updated) => {
          this.store.replaceCharacter(updated);
          this.toast.ok(`Diseño de ${updated.name} aprobado`);
        },
        error: (err) => this.toast.bad(errorMessage(err)),
      });
    }
  }

  protected add(key: string): void {
    const series = this.store.series();
    if (series) {
      this.api.registerCharacter(series.id, key, 'PROTAGONISTA').subscribe({
        next: () => this.store.loadCharacters(),
        error: (err) => this.toast.bad(errorMessage(err)),
      });
    }
  }
}
