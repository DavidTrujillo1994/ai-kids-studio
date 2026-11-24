import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { EpisodeApi } from '../../../core/api';
import { Episode } from '../../../core/models';

@Component({
  selector: 'app-review-panel',
  imports: [FormsModule, DatePipe],
  templateUrl: './review-panel.html',
  styleUrl: './review-panel.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ReviewPanel {
  readonly episode = input.required<Episode>();
  readonly busy = input(false);
  readonly approve = output<void>();
  readonly reject = output<string>();
  readonly export = output<void>();

  private readonly api = inject(EpisodeApi);
  protected readonly rejecting = signal(false);
  protected readonly comments = signal('');
  protected readonly pending = computed(() => this.episode().status === 'PENDING_APPROVAL');
  protected readonly approved = computed(() => this.episode().status === 'APPROVED');
  protected readonly videoUrl = computed(() => this.api.videoUrl(this.episode()));
  protected readonly posterUrl = computed(() => this.api.thumbnailUrl(this.episode()));
  protected readonly downloadUrl = computed(() => this.api.downloadUrl(this.episode()));
  protected readonly history = computed(() => [...this.episode().reviews].reverse());

  protected send(): void {
    this.reject.emit(this.comments());
    this.rejecting.set(false);
    this.comments.set('');
  }
}
