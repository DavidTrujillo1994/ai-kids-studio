import { ChangeDetectionStrategy, Component, ElementRef, afterNextRender, computed, inject, input, output, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { STEP, animationLabel, expressionLabel, propLabel } from '../../../core/labels';
import { PlacePicker } from '../../../shared/place-picker';
import { CastMember, Step } from '../../../core/models';
import { FLIGHTS, PLACES, flightIndex, placeLabel } from '../../../core/script-ops';
import { SeriesStore } from '../../../core/series.store';

const MAX_TEXT = 160;

@Component({
  selector: 'app-step-card',
  imports: [FormsModule, PlacePicker],
  templateUrl: './step-card.html',
  styleUrl: './step-card.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StepCard {
  readonly step = input.required<Step>();
  readonly number = input.required<number>();
  readonly cast = input<CastMember[]>([]);
  readonly problems = input<string[]>([]);
  readonly readonly = input(false);
  readonly first = input(false);
  readonly last = input(false);
  readonly focus = input(false);

  readonly changed = output<Step>();
  readonly moved = output<number>();
  readonly duplicated = output<void>();
  readonly removed = output<void>();

  protected readonly store = inject(SeriesStore);
  protected readonly info = computed(() => STEP[this.step().type]);
  protected readonly animations = computed(() => this.store.catalog().animations.filter((a) => !this.store.catalog().gaits.includes(a) || a === 'fly'));
  protected readonly gaits = computed(() => this.store.catalog().gaits);
  protected readonly expressions = computed(() => this.store.expressionsOf(this.step().character));
  protected readonly flight = computed(() => flightIndex(this.step().path ?? []));
  protected readonly left = computed(() => MAX_TEXT - (this.step().text?.length ?? 0));
  protected readonly maxText = MAX_TEXT;
  protected readonly places = PLACES;
  protected readonly flights = FLIGHTS;
  protected readonly placeLabel = placeLabel;
  protected readonly animationLabel = animationLabel;
  protected readonly expressionLabel = expressionLabel;
  protected readonly propLabel = propLabel;
  private readonly text = viewChild<ElementRef<HTMLTextAreaElement>>('text');

  constructor() {
    afterNextRender(() => {
      if (this.focus()) {
        this.text()?.nativeElement.focus();
      }
    });
  }

  protected set(part: Partial<Step>): void {
    this.changed.emit({ ...this.step(), ...part });
  }

  protected startOf(character: string): number | null {
    return this.step().positions?.find((p) => p.character === character)?.startX ?? null;
  }

  protected setStart(character: string, x: string): void {
    const others = (this.step().positions ?? []).filter((p) => p.character !== character);
    this.set({ positions: [...others, { character, startX: Number(x) }] });
  }

  protected isPlace(x: number | null): boolean {
    return PLACES.some((p) => p.x === x);
  }

  protected setFlight(index: number): void {
    if (index >= 0) {
      this.set({ path: this.flights[index].path });
    }
  }
}
