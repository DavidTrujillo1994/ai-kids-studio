import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { STEP } from '../../../core/labels';
import { Problem, Script, Step, StepType } from '../../../core/models';
import { PLACES, estimateSeconds, insertAt, move, newStep, placeLabel, removeAt, replaceAt, setCast } from '../../../core/script-ops';
import { SeriesStore } from '../../../core/series.store';
import { StepCard } from '../step-card/step-card';

@Component({
  selector: 'app-script-editor',
  imports: [StepCard],
  templateUrl: './script-editor.html',
  styleUrl: './script-editor.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ScriptEditor {
  readonly script = input.required<Script>();
  readonly problems = input<Problem[]>([]);
  readonly readonly = input(false);
  readonly changed = output<Script>();

  protected readonly store = inject(SeriesStore);
  protected readonly stepInfo = STEP;
  protected readonly types: StepType[] = ['SAY', 'WALK', 'PLAY', 'SCENE', 'APPEAR', 'PAUSE'];
  protected readonly places = PLACES;
  protected readonly placeLabel = placeLabel;
  protected readonly focused = signal(-1);
  protected readonly seconds = computed(() => estimateSeconds(this.script()));
  protected readonly general = computed(() => this.problems().filter((p) => p.step < 0));
  protected readonly availableTypes = computed(() => this.types.filter((t) => t !== 'APPEAR' || this.store.catalog().props.length > 0));

  protected problemsOf(index: number): string[] {
    return this.problems().filter((p) => p.step === index).map((p) => p.message);
  }

  protected add(type: StepType): void {
    const steps = this.script().steps;
    const last = [...steps].reverse().find((s) => s.character)?.character;
    const character = last ?? this.script().cast[0]?.character ?? '';
    const step = newStep(type, character, this.store.catalog().props[0] ?? '', this.store.catalog().animations, this.nextPlace(), this.script().cast);
    this.focused.set(steps.length);
    this.changed.emit(insertAt(this.script(), steps.length, step));
  }

  /** Propone un lugar que la historia todavía no usa. */
  private nextPlace(): string {
    const used = this.script().steps.filter((s) => s.type === 'SCENE').map((s) => s.environment);
    const all = this.store.catalog().environments;
    return all.find((e) => !used.includes(e)) ?? all[0] ?? '';
  }

  protected replace(index: number, step: Step): void {
    this.changed.emit(replaceAt(this.script(), index, step));
  }

  protected duplicate(index: number): void {
    this.changed.emit(insertAt(this.script(), index + 1, { ...this.script().steps[index] }));
  }

  protected remove(index: number): void {
    this.changed.emit(removeAt(this.script(), index));
  }

  protected move(index: number, by: number): void {
    this.changed.emit(move(this.script(), index, index + by));
  }

  protected inCast(key: string): boolean {
    return this.script().cast.some((c) => c.character === key);
  }

  protected toggleCast(key: string): void {
    const cast = this.script().cast;
    this.changed.emit(setCast(this.script(), this.inCast(key) ? cast.filter((c) => c.character !== key) : [...cast, { character: key, startX: -170 }]));
  }

  protected setStart(key: string, x: string): void {
    this.changed.emit(setCast(this.script(), this.script().cast.map((c) => (c.character === key ? { ...c, startX: Number(x) } : c))));
  }

  protected isPlace(x: number): boolean {
    return PLACES.some((p) => p.x === x);
  }
}
