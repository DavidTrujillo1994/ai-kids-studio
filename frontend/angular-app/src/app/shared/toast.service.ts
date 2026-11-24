import { Injectable, signal } from '@angular/core';

export interface Toast { id: number; text: string; bad: boolean; }

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly toasts = signal<Toast[]>([]);
  private next = 0;

  ok(text: string): void {
    this.show(text, false);
  }

  bad(text: string): void {
    this.show(text, true);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private show(text: string, bad: boolean): void {
    const id = this.next++;
    this.toasts.update((list) => [...list, { id, text, bad }]);
    setTimeout(() => this.dismiss(id), bad ? 8000 : 4000);
  }
}
