import { ChangeDetectionStrategy, Component, ElementRef, effect, inject, viewChild } from '@angular/core';
import { ConfirmService } from './confirm.service';

@Component({
  selector: 'app-confirm-dialog',
  template: `
    <dialog #dialog (cancel)="$event.preventDefault(); service.answer(false)">
      @if (service.question(); as q) {
        <div class="stack">
          <h2>{{ q.title }}</h2>
          <p class="muted">{{ q.text }}</p>
          <div class="row end">
            <button class="btn" type="button" (click)="service.answer(false)">No, volver</button>
            <button class="btn" type="button" [class.primary]="!q.danger" [class.danger]="q.danger" (click)="service.answer(true)">{{ q.yes }}</button>
          </div>
        </div>
      }
    </dialog>
  `,
  styles: `
    dialog { width: min(440px, calc(100vw - 32px)); padding: 24px; border: 0; border-radius: var(--radius); color: inherit; box-shadow: var(--shadow); }
    dialog::backdrop { background: rgb(40 28 16 / 45%); }
    .end { justify-content: flex-end; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConfirmDialog {
  protected readonly service = inject(ConfirmService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    effect(() => {
      const element = this.dialog().nativeElement;
      if (this.service.question() && !element.open) {
        element.showModal();
      } else if (!this.service.question() && element.open) {
        element.close();
      }
    });
  }
}
