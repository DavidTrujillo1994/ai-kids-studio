import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ToastService } from './toast.service';

@Component({
  selector: 'app-toasts',
  template: `
    <div class="area" aria-live="polite">
      @for (t of service.toasts(); track t.id) {
        <button type="button" [class.bad]="t.bad" (click)="service.dismiss(t.id)">{{ t.text }}</button>
      }
    </div>
  `,
  styles: `
    .area { position: fixed; right: 20px; bottom: 20px; z-index: 50; display: grid; gap: 10px; max-width: min(420px, calc(100vw - 40px)); }
    button { padding: 14px 18px; border: 0; border-radius: 12px; background: var(--leaf-dark); color: #fff; font: inherit; font-weight: 600; text-align: left; box-shadow: var(--shadow); cursor: pointer; }
    .bad { background: var(--berry); }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Toasts {
  protected readonly service = inject(ToastService);
}
