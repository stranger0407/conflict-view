import { Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'cv-loading-spinner',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    <div class="spinner-wrap">
      <mat-spinner diameter="40" strokeWidth="3"></mat-spinner>
    </div>
  `,
  styles: [`
    .spinner-wrap {
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 48px;
    }
    ::ng-deep .mat-mdc-progress-spinner circle {
      stroke: var(--accent-blue) !important;
    }
  `]
})
export class LoadingSpinnerComponent {}
