import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { NgxChartsModule, LegendPosition } from '@swimlane/ngx-charts';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardStats } from '../../core/models/dashboard-stats.model';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge/severity-badge.component';
import { NewsCardComponent } from '../../shared/components/news-card/news-card.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';

@Component({
  selector: 'cv-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterLink,
    MatCardModule, MatIconModule, MatButtonModule,
    NgxChartsModule,
    SeverityBadgeComponent, NewsCardComponent, LoadingSpinnerComponent, TimeAgoPipe
  ],
  template: `
    <div class="dashboard">
      <cv-loading-spinner *ngIf="loading"></cv-loading-spinner>

      <ng-container *ngIf="!loading && stats">
        <!-- Page heading -->
        <div class="page-header">
          <div>
            <h1>Global Overview</h1>
            <p>Real-time tracking across {{ stats.totalActiveConflicts }} active conflicts</p>
          </div>
          <a routerLink="/map" class="btn-primary">
            <mat-icon>map</mat-icon>
            View Live Map
          </a>
        </div>

        <!-- Stat cards -->
        <div class="stat-grid">
          <div class="stat-card critical">
            <mat-icon>warning</mat-icon>
            <div class="stat-info">
              <span class="stat-num">{{ stats.criticalConflicts }}</span>
              <span class="stat-label">Critical</span>
            </div>
          </div>
          <div class="stat-card high">
            <mat-icon>trending_up</mat-icon>
            <div class="stat-info">
              <span class="stat-num">{{ stats.highConflicts }}</span>
              <span class="stat-label">High</span>
            </div>
          </div>
          <div class="stat-card medium">
            <mat-icon>remove_circle_outline</mat-icon>
            <div class="stat-info">
              <span class="stat-num">{{ stats.mediumConflicts }}</span>
              <span class="stat-label">Medium</span>
            </div>
          </div>
          <div class="stat-card monitoring">
            <mat-icon>visibility</mat-icon>
            <div class="stat-info">
              <span class="stat-num">{{ stats.monitoringConflicts }}</span>
              <span class="stat-label">Monitoring</span>
            </div>
          </div>
          <div class="stat-card articles">
            <mat-icon>article</mat-icon>
            <div class="stat-info">
              <span class="stat-num">{{ stats.totalArticlesAllTime | number }}</span>
              <span class="stat-label">Articles Indexed</span>
            </div>
          </div>
          <div class="stat-card total">
            <mat-icon>public</mat-icon>
            <div class="stat-info">
              <span class="stat-num">{{ stats.totalActiveConflicts }}</span>
              <span class="stat-label">Active Conflicts</span>
            </div>
          </div>
        </div>

        <!-- Main grid -->
        <div class="main-grid">

          <!-- Left column -->
          <div class="left-col">

            <!-- Top Conflicts -->
            <div class="card">
              <div class="card-header">
                <h3><mat-icon>flash_on</mat-icon> Top Conflicts</h3>
                <a routerLink="/search" class="see-all">See all →</a>
              </div>
              <div class="top-conflicts">
                <a class="top-conflict-item"
                   *ngFor="let c of stats.topConflictsBySeverity; let i = index"
                   [routerLink]="['/conflict', c.id]">
                  <span class="rank">{{ i + 1 }}</span>
                  <div class="conflict-info">
                    <span class="conflict-name">{{ c.name }}</span>
                    <span class="conflict-region">{{ c.region }}</span>
                  </div>
                  <cv-severity-badge [severity]="c.severity"></cv-severity-badge>
                  <mat-icon class="arrow">chevron_right</mat-icon>
                </a>
              </div>
            </div>

            <!-- Region chart -->
            <div class="card chart-card">
              <div class="card-header">
                <h3><mat-icon>pie_chart</mat-icon> Conflicts by Region</h3>
              </div>
              <ngx-charts-pie-chart
                [results]="regionChartData"
                [legend]="true"
                [labels]="false"
                [doughnut]="true"
                [arcWidth]="0.4"
                [legendPosition]="legendPos"
                [scheme]="colorScheme"
                [view]="[400, 220]">
              </ngx-charts-pie-chart>
            </div>
          </div>

          <!-- Right column — latest news -->
          <div class="right-col">
            <div class="card news-card-panel">
              <div class="card-header">
                <h3><mat-icon>feed</mat-icon> Latest Reports</h3>
                <span class="live-tag">
                  <span class="live-dot"></span>
                  LIVE
                </span>
              </div>
              <div class="news-list">
                <cv-news-card
                  *ngFor="let article of stats.latestArticles"
                  [article]="article">
                </cv-news-card>
              </div>
            </div>
          </div>
        </div>

        <!-- Conflict type chart -->
        <div class="card wide-card">
          <div class="card-header">
            <h3><mat-icon>bar_chart</mat-icon> Conflicts by Type</h3>
          </div>
          <ngx-charts-bar-horizontal
            [results]="typeChartData"
            [xAxis]="true"
            [yAxis]="true"
            [showDataLabel]="true"
            [roundEdges]="true"
            [scheme]="colorScheme"
            [view]="[600, 200]"
            class="type-chart">
          </ngx-charts-bar-horizontal>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .dashboard {
      height: 100%;
      overflow-y: auto;
      padding: 24px 32px;
      background: var(--bg-primary);
    }

    .page-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      margin-bottom: 24px;
      h1 {
        font-size: 24px;
        font-weight: 700;
        color: var(--text-primary);
        margin-bottom: 4px;
      }
      p { color: var(--text-secondary); font-size: 14px; }
    }

    .btn-primary {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      padding: 10px 18px;
      background: var(--accent-blue);
      color: #fff;
      border-radius: var(--radius-md);
      font-size: 13px;
      font-weight: 600;
      text-decoration: none;
      transition: background var(--transition-fast);
      mat-icon { font-size: 18px; }
      &:hover { background: var(--accent-blue-dark); }
    }

    .stat-grid {
      display: grid;
      grid-template-columns: repeat(6, 1fr);
      gap: 12px;
      margin-bottom: 24px;
    }

    .stat-card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 16px;
      display: flex;
      align-items: center;
      gap: 12px;
      transition: all var(--transition-fast);
      &:hover { transform: translateY(-2px); box-shadow: var(--shadow-md); }
      mat-icon { font-size: 24px; opacity: 0.9; }
      &.critical { border-color: rgba(239,68,68,0.3); mat-icon { color: #ef4444; } }
      &.high { border-color: rgba(249,115,22,0.3); mat-icon { color: #f97316; } }
      &.medium { border-color: rgba(234,179,8,0.3); mat-icon { color: #eab308; } }
      &.monitoring { border-color: rgba(99,102,241,0.3); mat-icon { color: #818cf8; } }
      &.articles { border-color: rgba(59,130,246,0.3); mat-icon { color: var(--accent-blue); } }
      &.total { border-color: rgba(34,197,94,0.3); mat-icon { color: #22c55e; } }
    }

    .stat-info { display: flex; flex-direction: column; }
    .stat-num { font-size: 22px; font-weight: 700; color: var(--text-primary); line-height: 1.1; }
    .stat-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.06em; }

    .main-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 20px;
      margin-bottom: 20px;
    }

    .left-col, .right-col { display: flex; flex-direction: column; gap: 20px; }

    .card {
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-lg);
      padding: 20px;
    }
    .wide-card { margin-bottom: 24px; }

    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 16px;
      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 15px;
        font-weight: 600;
        color: var(--text-primary);
        mat-icon { font-size: 18px; color: var(--accent-blue); }
      }
    }
    .see-all { font-size: 12px; color: var(--accent-blue); text-decoration: none; &:hover { color: var(--accent-blue-light); } }

    .top-conflicts { display: flex; flex-direction: column; gap: 4px; }
    .top-conflict-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 10px 12px;
      border-radius: var(--radius-md);
      text-decoration: none;
      transition: background var(--transition-fast);
      &:hover { background: var(--bg-card-hover); .arrow { opacity: 1; transform: translateX(2px); } }
    }
    .rank {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--bg-secondary);
      color: var(--text-muted);
      font-size: 11px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;
    }
    .conflict-info { flex: 1; min-width: 0; }
    .conflict-name {
      display: block;
      font-size: 13px;
      font-weight: 600;
      color: var(--text-primary);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .conflict-region { font-size: 11px; color: var(--text-muted); }
    .arrow { font-size: 18px; color: var(--text-muted); opacity: 0; transition: all var(--transition-fast); }

    .chart-card { overflow: hidden; }
    ::ng-deep .chart-card ngx-charts-pie-chart { width: 100% !important; }

    .news-card-panel { overflow: hidden; }
    .news-list { display: flex; flex-direction: column; gap: 8px; max-height: 520px; overflow-y: auto; }

    .live-tag {
      display: flex;
      align-items: center;
      gap: 5px;
      font-size: 10px;
      font-weight: 700;
      color: #ef4444;
      background: rgba(239,68,68,0.1);
      border: 1px solid rgba(239,68,68,0.3);
      padding: 2px 8px;
      border-radius: 20px;
      letter-spacing: 0.1em;
    }
    .live-dot {
      width: 6px; height: 6px; border-radius: 50%; background: #ef4444;
      animation: blink 1.2s ease-in-out infinite;
    }
    @keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.2; } }

    ::ng-deep .type-chart .ngx-charts-bar { fill: var(--accent-blue) !important; }

    @media (max-width: 1200px) {
      .stat-grid { grid-template-columns: repeat(3, 1fr); }
      .main-grid { grid-template-columns: 1fr; }
    }
    @media (max-width: 768px) {
      .dashboard { padding: 16px; }
      .stat-grid { grid-template-columns: repeat(2, 1fr); }
    }
  `]
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  stats: DashboardStats | null = null;
  loading = true;

  colorScheme: any = {
    domain: ['#ef4444', '#f97316', '#eab308', '#22c55e', '#3b82f6', '#6366f1', '#ec4899', '#14b8a6']
  };
  legendPos = LegendPosition.Right;

  get regionChartData() {
    if (!this.stats) return [];
    return Object.entries(this.stats.conflictsByRegion).map(([name, value]) => ({ name, value }));
  }

  get typeChartData() {
    if (!this.stats) return [];
    return Object.entries(this.stats.conflictsByType).map(([name, value]) => ({
      name: name.replace('_', ' '),
      value
    }));
  }

  ngOnInit(): void {
    this.dashboardService.getStats().subscribe({
      next: data => { this.stats = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }
}
