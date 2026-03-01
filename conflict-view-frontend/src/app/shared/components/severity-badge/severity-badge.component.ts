import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Severity, ConflictStatus } from '../../../core/models/conflict.model';

@Component({
  selector: 'cv-severity-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="sev-badge" [ngClass]="cssClass">
      <span class="indicator" [ngClass]="{'pulse': severity === 'CRITICAL'}"></span>
      {{ label }}
    </span>
  `,
  styles: [`
    .sev-badge {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      padding: 3px 10px;
      border-radius: 20px;
      font-size: 11px;
      font-weight: 600;
      letter-spacing: 0.06em;
      text-transform: uppercase;
      white-space: nowrap;
    }
    .indicator {
      width: 7px; height: 7px;
      border-radius: 50%;
      background: currentColor;
      flex-shrink: 0;
    }
    .pulse { animation: blink 1.5s ease-in-out infinite; }
    @keyframes blink {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.3; }
    }
    .critical { background: rgba(239,68,68,0.15); color: #ef4444; border: 1px solid rgba(239,68,68,0.35); }
    .high { background: rgba(249,115,22,0.15); color: #f97316; border: 1px solid rgba(249,115,22,0.35); }
    .medium { background: rgba(234,179,8,0.15); color: #eab308; border: 1px solid rgba(234,179,8,0.35); }
    .low { background: rgba(34,197,94,0.15); color: #22c55e; border: 1px solid rgba(34,197,94,0.35); }
    .monitoring { background: rgba(99,102,241,0.15); color: #818cf8; border: 1px solid rgba(99,102,241,0.35); }
  `]
})
export class SeverityBadgeComponent {
  @Input() severity?: Severity;
  @Input() status?: ConflictStatus;

  get cssClass(): string {
    if (this.status && this.status !== 'ACTIVE') return 'monitoring';
    return (this.severity || 'LOW').toLowerCase();
  }

  get label(): string {
    if (this.status && this.status !== 'ACTIVE') return this.status;
    return this.severity || 'LOW';
  }
}
