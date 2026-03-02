import { Component, OnInit, inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { debounceTime, distinctUntilChanged, Subject } from 'rxjs';
import { ConflictService } from '../../core/services/conflict.service';
import { ConflictMap, Severity, ConflictType, ConflictStatus } from '../../core/models/conflict.model';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge/severity-badge.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ConflictTypeLabelPipe } from '../../shared/pipes/conflict-type-label.pipe';

@Component({
  selector: 'cv-search',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatChipsModule,
    SeverityBadgeComponent, LoadingSpinnerComponent, ConflictTypeLabelPipe
  ],
  template: `
    <div class="search-page">
      <!-- Header -->
      <div class="search-header">
        <div class="header-text">
          <h1>Search Conflicts</h1>
          <p>Search and filter across {{ totalConflicts }} tracked conflicts</p>
        </div>
      </div>

      <!-- Search + filters panel -->
      <div class="search-panel">
        <!-- Search input -->
        <div class="search-input-wrap">
          <mat-icon class="search-icon">search</mat-icon>
          <input class="search-input" type="text"
                 placeholder="Search by conflict name, region, country..."
                 [(ngModel)]="query"
                 (ngModelChange)="onQueryChange($event)">
          <button class="clear-btn" *ngIf="query" (click)="clearQuery()">
            <mat-icon>close</mat-icon>
          </button>
        </div>

        <!-- Filters row -->
        <div class="filters-row">
          <!-- Region -->
          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Region</mat-label>
            <mat-select [(ngModel)]="filters.region" (ngModelChange)="search()">
              <mat-option [value]="null">All Regions</mat-option>
              <mat-option *ngFor="let r of regions" [value]="r">{{ r }}</mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Severity -->
          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Severity</mat-label>
            <mat-select [(ngModel)]="filters.severity" (ngModelChange)="search()">
              <mat-option [value]="null">All Severities</mat-option>
              <mat-option value="CRITICAL">Critical</mat-option>
              <mat-option value="HIGH">High</mat-option>
              <mat-option value="MEDIUM">Medium</mat-option>
              <mat-option value="LOW">Low</mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Type -->
          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Type</mat-label>
            <mat-select [(ngModel)]="filters.type" (ngModelChange)="search()">
              <mat-option [value]="null">All Types</mat-option>
              <mat-option value="WAR">War / Armed Conflict</mat-option>
              <mat-option value="CIVIL_UNREST">Civil Unrest</mat-option>
              <mat-option value="TERRORISM">Terrorism / Insurgency</mat-option>
              <mat-option value="POLITICAL">Political Crisis</mat-option>
              <mat-option value="HUMANITARIAN">Humanitarian Crisis</mat-option>
              <mat-option value="BORDER_DISPUTE">Border Dispute</mat-option>
              <mat-option value="COUP">Coup</mat-option>
            </mat-select>
          </mat-form-field>

          <!-- Status -->
          <mat-form-field appearance="outline" class="filter-field">
            <mat-label>Status</mat-label>
            <mat-select [(ngModel)]="filters.status" (ngModelChange)="search()">
              <mat-option [value]="null">All Statuses</mat-option>
              <mat-option value="ACTIVE">Active</mat-option>
              <mat-option value="MONITORING">Monitoring</mat-option>
              <mat-option value="CEASEFIRE">Ceasefire</mat-option>
              <mat-option value="RESOLVED">Resolved</mat-option>
            </mat-select>
          </mat-form-field>

          <button class="reset-btn" (click)="resetFilters()" *ngIf="hasActiveFilters">
            <mat-icon>filter_list_off</mat-icon>
            Reset
          </button>
        </div>
      </div>

      <!-- Results count -->
      <div class="results-meta">
        <span class="results-count">{{ results.length }} result{{ results.length !== 1 ? 's' : '' }}</span>
        <span class="results-sub" *ngIf="query || hasActiveFilters">
          matching your search
        </span>
      </div>

      <!-- Loading -->
      <cv-loading-spinner *ngIf="loading"></cv-loading-spinner>

      <!-- Results grid -->
      <div class="results-grid" *ngIf="!loading">
        <a class="result-card" *ngFor="let c of results"
           [routerLink]="['/conflict', c.id]">

          <div class="rc-header">
            <cv-severity-badge [severity]="c.severity" [status]="c.status"></cv-severity-badge>
            <span class="rc-type">{{ c.conflictType | conflictTypeLabel }}</span>
          </div>

          <h3 class="rc-name">{{ c.name }}</h3>

          <div class="rc-meta">
            <span class="rc-region">
              <mat-icon>location_on</mat-icon>
              {{ c.region }}
            </span>
            <span *ngIf="c.startDate" class="rc-date">
              <mat-icon>calendar_today</mat-icon>
              Since {{ c.startDate | date:'yyyy' }}
            </span>
          </div>

          <div class="rc-parties" *ngIf="c.involvedParties">
            {{ c.involvedParties }}
          </div>

          <div class="rc-footer">
            <span class="rc-articles">
              <mat-icon>article</mat-icon>
              {{ c.articleCount }} reports
            </span>
            <span class="rc-cta">View Dashboard →</span>
          </div>
        </a>

        <!-- Empty state -->
        <div class="empty-state" *ngIf="results.length === 0 && !loading">
          <mat-icon>search_off</mat-icon>
          <h3>No conflicts found</h3>
          <p>Try a different search term or adjust your filters.</p>
          <button class="reset-link" (click)="resetFilters()">Clear all filters</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .search-page {
      height: 100%;
      overflow-y: auto;
      padding: 24px 32px;
      background: var(--bg-primary);
    }

    .search-header {
      margin-bottom: 24px;
      h1 { font-size: 24px; font-weight: 700; color: var(--text-primary); margin-bottom: 4px; }
      p { color: var(--text-secondary); font-size: 14px; }
    }

    /* Search panel */
    .search-panel {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 20px;
      margin-bottom: 20px;
    }

    .search-input-wrap {
      position: relative;
      display: flex;
      align-items: center;
      background: var(--bg-secondary);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 0 12px;
      margin-bottom: 16px;
      transition: border-color var(--transition-fast);
      &:focus-within { border-color: var(--accent-blue); }
    }
    .search-icon { color: var(--text-muted); font-size: 20px; }
    .search-input {
      flex: 1;
      background: none;
      border: none;
      outline: none;
      padding: 12px 10px;
      color: var(--text-primary);
      font-size: 15px;
      font-family: 'Inter', sans-serif;
      &::placeholder { color: var(--text-muted); }
    }
    .clear-btn {
      background: none;
      border: none;
      cursor: pointer;
      color: var(--text-muted);
      display: flex;
      align-items: center;
      border-radius: 4px;
      padding: 2px;
      &:hover { color: var(--text-primary); }
    }

    .filters-row {
      display: flex;
      gap: 12px;
      align-items: center;
      flex-wrap: wrap;
    }
    .filter-field {
      width: 160px;
      ::ng-deep .mat-mdc-text-field-wrapper { background: var(--bg-secondary) !important; }
    }
    .reset-btn {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 8px 14px;
      border-radius: var(--radius-md);
      border: 1px solid var(--border-color);
      background: transparent;
      color: var(--text-muted);
      font-size: 12px;
      font-family: 'Inter', sans-serif;
      cursor: pointer;
      transition: all var(--transition-fast);
      mat-icon { font-size: 16px; }
      &:hover { border-color: var(--accent-blue); color: var(--accent-blue-light); }
    }

    .results-meta {
      display: flex;
      align-items: baseline;
      gap: 6px;
      margin-bottom: 16px;
    }
    .results-count { font-size: 16px; font-weight: 700; color: var(--text-primary); }
    .results-sub { font-size: 13px; color: var(--text-muted); }

    /* Results grid */
    .results-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 16px;
    }

    .result-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 18px;
      text-decoration: none;
      display: flex;
      flex-direction: column;
      gap: 10px;
      transition: all var(--transition-base);
      &:hover {
        transform: translateY(-3px);
        box-shadow: var(--shadow-lg);
        border-color: rgba(59,130,246,0.3);
        .rc-cta { color: var(--accent-blue-light); }
      }
    }

    .rc-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .rc-type {
      font-size: 11px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .rc-name {
      font-size: 15px;
      font-weight: 700;
      color: var(--text-primary);
      line-height: 1.3;
    }
    .rc-meta {
      display: flex;
      align-items: center;
      gap: 14px;
      font-size: 12px;
      color: var(--text-muted);
    }
    .rc-region, .rc-date {
      display: flex;
      align-items: center;
      gap: 3px;
      mat-icon { font-size: 13px; }
    }
    .rc-parties {
      font-size: 12px;
      color: var(--text-secondary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .rc-footer {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-top: auto;
      padding-top: 10px;
      border-top: 1px solid var(--border-color);
    }
    .rc-articles {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: var(--text-muted);
      mat-icon { font-size: 14px; }
    }
    .rc-cta {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-muted);
      transition: color var(--transition-fast);
    }

    .empty-state {
      grid-column: 1 / -1;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 80px 24px;
      color: var(--text-muted);
      text-align: center;
      mat-icon { font-size: 56px; opacity: 0.3; }
      h3 { font-size: 18px; color: var(--text-secondary); }
      p { font-size: 14px; }
    }
    .reset-link {
      background: none;
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 8px 16px;
      color: var(--accent-blue);
      font-size: 13px;
      cursor: pointer;
      font-family: 'Inter', sans-serif;
      &:hover { border-color: var(--accent-blue); }
    }

    @media (max-width: 768px) {
      .search-page { padding: 16px; }
      .results-grid { grid-template-columns: 1fr; }
      .filters-row { flex-direction: column; align-items: stretch; }
      .filter-field { width: 100%; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SearchComponent implements OnInit {
  private conflictService = inject(ConflictService);
  private cdr = inject(ChangeDetectorRef);

  query = '';
  results: ConflictMap[] = [];
  totalConflicts = 0;
  loading = false;

  filters: {
    region: string | null;
    severity: Severity | null;
    type: ConflictType | null;
    status: ConflictStatus | null;
  } = { region: null, severity: null, type: null, status: null };

  regions = ['Middle East', 'Africa', 'Eastern Europe', 'Asia', 'Americas', 'South Asia', 'Southeast Asia'];

  private searchSubject = new Subject<string>();

  get hasActiveFilters(): boolean {
    return !!(this.filters.region || this.filters.severity || this.filters.type || this.filters.status);
  }

  ngOnInit(): void {
    this.searchSubject.pipe(debounceTime(350), distinctUntilChanged())
      .subscribe(() => this.search());

    this.search();

    // Load total count
    this.conflictService.getAllForMap().subscribe(data => {
      this.totalConflicts = data.length;
      this.cdr.markForCheck();
    });
  }

  onQueryChange(_q: string): void {
    this.searchSubject.next(_q);
  }

  search(): void {
    this.loading = true;
    this.conflictService.search(
      this.query || undefined,
      this.filters.region ?? undefined,
      this.filters.severity ?? undefined,
      this.filters.type ?? undefined,
      this.filters.status ?? undefined
    ).subscribe({
      next: data => { this.results = data; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  clearQuery(): void {
    this.query = '';
    this.search();
  }

  resetFilters(): void {
    this.query = '';
    this.filters = { region: null, severity: null, type: null, status: null };
    this.search();
  }
}
