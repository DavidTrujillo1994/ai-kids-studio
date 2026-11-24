import { Injectable, signal } from '@angular/core';

export interface Question { title: string; text: string; yes: string; danger?: boolean; }

@Injectable({ providedIn: 'root' })
export class ConfirmService {
  readonly question = signal<Question | null>(null);
  private resolve: (answer: boolean) => void = () => undefined;

  ask(question: Question): Promise<boolean> {
    this.resolve(false);
    this.question.set(question);
    return new Promise((resolve) => (this.resolve = resolve));
  }

  answer(value: boolean): void {
    this.question.set(null);
    this.resolve(value);
    this.resolve = () => undefined;
  }
}
