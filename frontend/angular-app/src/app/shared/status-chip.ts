import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { STATUS } from '../core/labels';
import { EpisodeStatus } from '../core/models';

@Component({
  selector: 'app-status-chip',
  template: `<span class="chip {{ info().tone }}">{{ info().label }}</span>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class StatusChip {
  readonly status = input.required<EpisodeStatus>();
  protected readonly info = computed(() => STATUS[this.status()]);
}
