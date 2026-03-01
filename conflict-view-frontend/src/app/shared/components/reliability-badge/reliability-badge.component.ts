import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'cv-reliability-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="reliability-badge" [ngClass]="colorClass" [title]="tooltip">
      <span class="dot"></span>
      {{ score }}/100 · {{ label }}
    </span>
  `,
  styles: [`
    .reliability-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 11px;
      font-weight: 600;
      white-space: nowrap;
    }
    .dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: currentColor;
      flex-shrink: 0;
    }
    .high { background: rgba(34,197,94,0.12); color: #22c55e; border: 1px solid rgba(34,197,94,0.3); }
    .medium { background: rgba(234,179,8,0.12); color: #eab308; border: 1px solid rgba(234,179,8,0.3); }
    .low { background: rgba(239,68,68,0.12); color: #ef4444; border: 1px solid rgba(239,68,68,0.3); }
  `]
})
export class ReliabilityBadgeComponent {
  @Input({ required: true }) score!: number;
  @Input() label = '';
  @Input() color: 'green' | 'yellow' | 'red' = 'yellow';

  get colorClass(): string {
    if (this.score >= 80) return 'high';
    if (this.score >= 60) return 'medium';
    return 'low';
  }

  get tooltip(): string {
    return `Source reliability: ${this.score}/100 — ${this.label || this.colorClass} credibility`;
  }
}
