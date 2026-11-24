import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { STAGES } from '../../../core/labels';
import { Episode } from '../../../core/models';

@Component({
  selector: 'app-production-panel',
  templateUrl: './production-panel.html',
  styleUrl: './production-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProductionPanel {
  readonly episode = input.required<Episode>();
  readonly running = input(false);
  readonly canStart = input(false);
  readonly start = output<void>();
  readonly cancel = output<void>();
  readonly seeVideo = output<void>();
  readonly editScript = output<void>();

  protected readonly job = computed(() => this.episode().job);
  protected readonly lines = computed(() => this.episode().script.steps.filter((s) => s.type === 'SAY').length);
  protected readonly stages = computed(() => {
    const job = this.job();
    const current = STAGES.findIndex((s) => s.stage === job?.stage);
    return STAGES.map((s, i) => ({ ...s, state: i < current ? 'done' : i === current ? 'now' : 'next' }));
  });
}
