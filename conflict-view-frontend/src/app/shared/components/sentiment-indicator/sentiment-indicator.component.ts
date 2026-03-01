import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SentimentType } from '../../../core/models/conflict.model';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'cv-sentiment-indicator',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatTooltipModule],
  template: `
    <span class="sentiment" [ngClass]="sentiment?.toLowerCase()"
          [matTooltip]="'Sentiment: ' + sentiment">
      <mat-icon class="icon">{{ icon }}</mat-icon>
      <span *ngIf="showLabel">{{ label }}</span>
    </span>
  `,
  styles: [`
    .sentiment {
      display: inline-flex;
      align-items: center;
      gap: 3px;
      font-size: 11px;
      font-weight: 500;
    }
    .icon { font-size: 14px; width: 14px; height: 14px; vertical-align: middle; }
    .positive { color: #22c55e; }
    .neutral { color: #6b7280; }
    .negative { color: #ef4444; }
  `]
})
export class SentimentIndicatorComponent {
  @Input({ required: true }) sentiment!: SentimentType;
  @Input() showLabel = false;

  get icon(): string {
    switch (this.sentiment) {
      case 'POSITIVE': return 'trending_up';
      case 'NEGATIVE': return 'trending_down';
      default: return 'remove';
    }
  }

  get label(): string {
    return this.sentiment ? this.sentiment.charAt(0) + this.sentiment.slice(1).toLowerCase() : '';
  }
}
