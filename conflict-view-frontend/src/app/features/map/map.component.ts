import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import * as L from 'leaflet';
import { Subscription } from 'rxjs';
import { ConflictService } from '../../core/services/conflict.service';
import { ConflictMap, Severity } from '../../core/models/conflict.model';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge/severity-badge.component';

@Component({
  selector: 'cv-map',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule, MatTooltipModule,
    MatChipsModule, MatSelectModule, MatFormFieldModule,
    SeverityBadgeComponent
  ],
  template: `
    <div class="map-page">
      <!-- Leaflet container -->
      <div #mapContainer class="map-container"></div>

      <!-- Overlay controls -->
      <div class="map-controls">
        <!-- Filter bar -->
        <div class="filter-bar">
          <div class="filter-chips">
            <button class="chip" [class.active]="activeFilter === 'ALL'"
                    (click)="setFilter('ALL')">All</button>
            <button class="chip critical" [class.active]="activeFilter === 'CRITICAL'"
                    (click)="setFilter('CRITICAL')">
              <span class="chip-dot"></span> Critical
            </button>
            <button class="chip high" [class.active]="activeFilter === 'HIGH'"
                    (click)="setFilter('HIGH')">High</button>
            <button class="chip medium" [class.active]="activeFilter === 'MEDIUM'"
                    (click)="setFilter('MEDIUM')">Medium</button>
            <button class="chip low" [class.active]="activeFilter === 'LOW'"
                    (click)="setFilter('LOW')">Low</button>
          </div>
          <div class="conflict-count">
            <span class="count">{{ filteredConflicts.length }}</span>
            <span class="count-label">conflicts</span>
          </div>
        </div>
      </div>

      <!-- Sidebar — conflict list -->
      <aside class="conflict-list" [class.open]="sidebarOpen">
        <div class="list-header">
          <span>Active Conflicts</span>
          <button class="close-btn" (click)="sidebarOpen = false">
            <mat-icon>close</mat-icon>
          </button>
        </div>
        <div class="list-body">
          <div class="conflict-item" *ngFor="let c of filteredConflicts"
               (click)="selectConflict(c)">
            <div class="item-header">
              <span class="item-name">{{ c.name }}</span>
              <cv-severity-badge [severity]="c.severity" [status]="c.status"></cv-severity-badge>
            </div>
            <div class="item-meta">
              <span class="item-region">
                <mat-icon class="meta-icon">location_on</mat-icon>
                {{ c.region }}
              </span>
              <span class="item-articles">
                <mat-icon class="meta-icon">article</mat-icon>
                {{ c.articleCount }}
              </span>
            </div>
          </div>
        </div>
      </aside>

      <!-- Toggle sidebar button -->
      <button class="sidebar-toggle" (click)="sidebarOpen = !sidebarOpen"
              matTooltip="Toggle conflict list">
        <mat-icon>{{ sidebarOpen ? 'chevron_left' : 'list' }}</mat-icon>
        <span *ngIf="!sidebarOpen">Conflicts</span>
      </button>

      <!-- Loading overlay -->
      <div class="loading-overlay" *ngIf="loading">
        <div class="loading-spinner">
          <div class="spinner"></div>
          <span>Loading conflicts...</span>
        </div>
      </div>

      <!-- Stats bar (bottom) -->
      <div class="stats-bar">
        <div class="stat critical">
          <span class="stat-num">{{ countBySeverity('CRITICAL') }}</span>
          <span class="stat-label">Critical</span>
        </div>
        <div class="stat high">
          <span class="stat-num">{{ countBySeverity('HIGH') }}</span>
          <span class="stat-label">High</span>
        </div>
        <div class="stat medium">
          <span class="stat-num">{{ countBySeverity('MEDIUM') }}</span>
          <span class="stat-label">Medium</span>
        </div>
        <div class="stat low">
          <span class="stat-num">{{ countBySeverity('LOW') }}</span>
          <span class="stat-label">Low</span>
        </div>
        <div class="stat-divider"></div>
        <div class="stat">
          <span class="stat-num">{{ conflicts.length }}</span>
          <span class="stat-label">Total Active</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .map-page {
      position: relative;
      width: 100%;
      height: 100%;
      overflow: hidden;
    }

    .map-container {
      width: 100%;
      height: 100%;
      background: #0a0e1a;
    }

    /* Override Leaflet popup styles for dark theme */
    :host ::ng-deep {
      .leaflet-popup-content-wrapper {
        background: rgba(15, 20, 35, 0.95);
        backdrop-filter: blur(12px);
        border: 1px solid rgba(255, 255, 255, 0.08);
        border-radius: 12px;
        box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5);
        color: #e8edf5;
      }
      .leaflet-popup-tip {
        background: rgba(15, 20, 35, 0.95);
        border: 1px solid rgba(255, 255, 255, 0.08);
      }
      .leaflet-popup-close-button {
        color: #6b7280 !important;
        font-size: 18px !important;
        &:hover { color: #e8edf5 !important; }
      }
      .leaflet-control-zoom {
        border: 1px solid rgba(255, 255, 255, 0.08) !important;
        a {
          background: rgba(15, 20, 35, 0.9) !important;
          color: #e8edf5 !important;
          border-color: rgba(255, 255, 255, 0.08) !important;
          &:hover { background: rgba(30, 40, 65, 0.9) !important; }
        }
      }
      .leaflet-control-attribution {
        background: rgba(15, 20, 35, 0.7) !important;
        color: #4a5568 !important;
        font-size: 10px !important;
        a { color: #6b7280 !important; }
      }
    }

    /* ======= Filter bar ======= */
    .map-controls {
      position: absolute;
      top: 16px;
      left: 50%;
      transform: translateX(-50%);
      z-index: 1000;
    }

    .filter-bar {
      display: flex;
      align-items: center;
      gap: 12px;
      background: var(--bg-glass);
      backdrop-filter: blur(12px);
      border: 1px solid var(--border-color);
      border-radius: 30px;
      padding: 8px 16px;
    }

    .filter-chips {
      display: flex;
      gap: 6px;
    }

    .chip {
      padding: 5px 12px;
      border-radius: 20px;
      border: 1px solid var(--border-color);
      background: transparent;
      color: var(--text-secondary);
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      transition: all var(--transition-fast);
      display: flex;
      align-items: center;
      gap: 5px;
      font-family: 'Inter', sans-serif;
      &:hover { background: rgba(255,255,255,0.05); color: var(--text-primary); }
      &.active { background: rgba(59,130,246,0.15); color: var(--accent-blue-light); border-color: var(--accent-blue); }
      &.critical.active { background: rgba(239,68,68,0.15); color: #ef4444; border-color: rgba(239,68,68,0.5); }
      &.high.active { background: rgba(249,115,22,0.15); color: #f97316; border-color: rgba(249,115,22,0.5); }
      &.medium.active { background: rgba(234,179,8,0.15); color: #eab308; border-color: rgba(234,179,8,0.5); }
      &.low.active { background: rgba(34,197,94,0.15); color: #22c55e; border-color: rgba(34,197,94,0.5); }
    }
    .chip-dot { width: 7px; height: 7px; border-radius: 50%; background: currentColor; }

    .conflict-count {
      display: flex;
      align-items: baseline;
      gap: 4px;
      padding-left: 10px;
      border-left: 1px solid var(--border-color);
    }
    .count { font-size: 18px; font-weight: 700; color: var(--text-primary); }
    .count-label { font-size: 11px; color: var(--text-muted); }

    /* ======= Sidebar ======= */
    .conflict-list {
      position: absolute;
      top: 0;
      right: 0;
      height: 100%;
      width: 300px;
      background: var(--bg-glass);
      backdrop-filter: blur(16px);
      border-left: 1px solid var(--border-color);
      z-index: 1000;
      display: flex;
      flex-direction: column;
      transform: translateX(100%);
      transition: transform var(--transition-slow);
      &.open { transform: translateX(0); }
    }

    .list-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      border-bottom: 1px solid var(--border-color);
      font-weight: 600;
      font-size: 14px;
      color: var(--text-primary);
    }
    .close-btn {
      background: none;
      border: none;
      cursor: pointer;
      color: var(--text-secondary);
      display: flex;
      align-items: center;
      border-radius: 6px;
      padding: 4px;
      &:hover { background: rgba(255,255,255,0.06); color: var(--text-primary); }
    }

    .list-body {
      flex: 1;
      overflow-y: auto;
      padding: 8px;
    }

    .conflict-item {
      padding: 12px;
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: background var(--transition-fast);
      margin-bottom: 4px;
      &:hover { background: var(--bg-card-hover); }
    }
    .item-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 8px;
      margin-bottom: 6px;
    }
    .item-name {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-primary);
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    .item-meta {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 11px;
      color: var(--text-muted);
    }
    .item-region, .item-articles {
      display: flex;
      align-items: center;
      gap: 3px;
    }
    .meta-icon { font-size: 12px; width: 12px; height: 12px; }

    /* ======= Toggle button ======= */
    .sidebar-toggle {
      position: absolute;
      top: 50%;
      right: 16px;
      transform: translateY(-50%);
      z-index: 999;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 10px 14px;
      background: var(--bg-glass);
      backdrop-filter: blur(12px);
      border: 1px solid var(--border-color);
      border-radius: 30px;
      color: var(--text-primary);
      font-family: 'Inter', sans-serif;
      font-size: 13px;
      font-weight: 500;
      cursor: pointer;
      transition: all var(--transition-fast);
      &:hover { background: var(--bg-card-hover); border-color: var(--border-color-hover); }
      mat-icon { font-size: 20px; }
    }

    /* ======= Stats bar ======= */
    .stats-bar {
      position: absolute;
      bottom: 24px;
      left: 50%;
      transform: translateX(-50%);
      display: flex;
      align-items: center;
      gap: 16px;
      background: var(--bg-glass);
      backdrop-filter: blur(12px);
      border: 1px solid var(--border-color);
      border-radius: 30px;
      padding: 10px 20px;
      z-index: 1000;
    }

    .stat {
      text-align: center;
      .stat-num {
        display: block;
        font-size: 18px;
        font-weight: 700;
        color: var(--text-primary);
      }
      .stat-label {
        font-size: 10px;
        color: var(--text-muted);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
      &.critical .stat-num { color: #ef4444; }
      &.high .stat-num { color: #f97316; }
      &.medium .stat-num { color: #eab308; }
      &.low .stat-num { color: #22c55e; }
    }
    .stat-divider {
      width: 1px;
      height: 32px;
      background: var(--border-color);
    }

    /* ======= Loading ======= */
    .loading-overlay {
      position: absolute;
      inset: 0;
      background: rgba(10, 14, 26, 0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 2000;
    }
    .loading-spinner {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 12px;
      color: var(--text-secondary);
      font-size: 13px;
    }
    .spinner {
      width: 36px;
      height: 36px;
      border: 3px solid var(--border-color);
      border-top-color: var(--accent-blue);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class MapComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;

  private conflictService = inject(ConflictService);
  private router = inject(Router);

  map!: L.Map;
  conflicts: ConflictMap[] = [];
  filteredConflicts: ConflictMap[] = [];
  activeFilter: 'ALL' | Severity = 'ALL';
  sidebarOpen = false;
  loading = true;
  private markerLayer = L.layerGroup();
  private sub?: Subscription;
  private stylesInjected = false;

  ngOnInit(): void {
    this.sub = this.conflictService.getAllForMap().subscribe({
      next: data => {
        this.conflicts = data;
        this.filteredConflicts = data;
        this.loading = false;
        this.renderMarkers();
      },
      error: () => { this.loading = false; }
    });
  }

  ngAfterViewInit(): void {
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [20, 20],
      zoom: 3,
      minZoom: 2,
      maxZoom: 12,
      zoomControl: true,
      attributionControl: true
    });

    // CartoDB Dark Matter tiles — free, no key
    L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OSM</a> &copy; <a href="https://carto.com/attributions">CARTO</a>',
      subdomains: 'abcd',
      maxZoom: 19
    }).addTo(this.map);

    this.markerLayer.addTo(this.map);
    this.injectMarkerStyles();
  }

  private injectMarkerStyles(): void {
    if (this.stylesInjected) return;
    this.stylesInjected = true;

    const style = document.createElement('style');
    style.id = 'cv-leaflet-marker-styles';
    style.textContent = `
      .cv-marker {
        position: relative;
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
      }
      .cv-marker-dot {
        width: 14px;
        height: 14px;
        border-radius: 50%;
        border: 2px solid rgba(255,255,255,0.4);
        z-index: 1;
        position: relative;
        transition: transform 0.15s;
      }
      .cv-marker:hover .cv-marker-dot {
        transform: scale(1.3);
      }
      .cv-marker-pulse {
        position: absolute;
        width: 24px;
        height: 24px;
        border-radius: 50%;
        opacity: 0.3;
        animation: cvPulse 2s ease-out infinite;
        top: 0;
        left: 0;
      }
      @keyframes cvPulse {
        0% { transform: scale(0.8); opacity: 0.5; }
        100% { transform: scale(2.5); opacity: 0; }
      }
      .cv-marker--critical .cv-marker-dot { background: #ef4444; box-shadow: 0 0 8px #ef4444, 0 0 20px rgba(0,0,0,0.5); }
      .cv-marker--critical .cv-marker-pulse { background: #ef4444; }
      .cv-marker--high .cv-marker-dot { background: #f97316; box-shadow: 0 0 8px #f97316, 0 0 20px rgba(0,0,0,0.5); }
      .cv-marker--high .cv-marker-pulse { background: #f97316; }
      .cv-marker--medium .cv-marker-dot { background: #eab308; box-shadow: 0 0 8px #eab308, 0 0 20px rgba(0,0,0,0.5); }
      .cv-marker--medium .cv-marker-pulse { background: #eab308; }
      .cv-marker--low .cv-marker-dot { background: #22c55e; box-shadow: 0 0 8px #22c55e, 0 0 20px rgba(0,0,0,0.5); }
      .cv-marker--low .cv-marker-pulse { background: #22c55e; }
    `;
    document.head.appendChild(style);
  }

  private renderMarkers(): void {
    this.markerLayer.clearLayers();

    for (const conflict of this.filteredConflicts) {
      const severityClass = conflict.severity.toLowerCase();
      const icon = L.divIcon({
        className: '',
        html: `
          <div class="cv-marker cv-marker--${severityClass}">
            <div class="cv-marker-pulse"></div>
            <div class="cv-marker-dot"></div>
          </div>
        `,
        iconSize: [24, 24],
        iconAnchor: [12, 12],
        popupAnchor: [0, -14]
      });

      const marker = L.marker([conflict.latitude, conflict.longitude], { icon })
        .bindPopup(this.buildPopupHtml(conflict), {
          maxWidth: 280,
          className: 'cv-popup'
        });

      marker.on('popupopen', () => {
        const btn = document.getElementById(`cv-view-btn-${conflict.id}`);
        if (btn) {
          btn.addEventListener('click', () => {
            this.router.navigate(['/conflict', conflict.id]);
          });
        }
      });

      this.markerLayer.addLayer(marker);
    }
  }

  private buildPopupHtml(conflict: ConflictMap): string {
    const severityColors: Record<string, string> = {
      CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#eab308', LOW: '#22c55e'
    };
    const color = severityColors[conflict.severity] ?? '#6366f1';
    const typeLabel = conflict.conflictType.replace('_', ' ');

    return `
      <div style="padding:10px;font-family:'Inter',sans-serif;min-width:220px;">
        <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:8px;">
          <span style="font-size:11px;font-weight:600;text-transform:uppercase;letter-spacing:0.06em;
                       color:${color};background:${color}22;padding:2px 8px;border-radius:20px;
                       border:1px solid ${color}44;">${conflict.severity}</span>
          <span style="font-size:10px;color:#6b7280;">${typeLabel}</span>
        </div>
        <h3 style="font-size:14px;font-weight:700;color:#e8edf5;margin:0 0 4px;line-height:1.3;">${conflict.name}</h3>
        <p style="font-size:11px;color:#8892a4;margin:0 0 10px;">
          ${conflict.region}
          ${conflict.involvedParties ? `<br><span style="margin-top:4px;display:block;">Parties: ${conflict.involvedParties.substring(0, 60)}...</span>` : ''}
        </p>
        <div style="display:flex;align-items:center;gap:12px;margin-bottom:12px;font-size:11px;color:#6b7280;">
          <span>${conflict.articleCount} articles</span>
          ${conflict.startDate ? `<span>Since ${new Date(conflict.startDate).getFullYear()}</span>` : ''}
        </div>
        <button id="cv-view-btn-${conflict.id}"
                style="width:100%;padding:8px;background:rgba(59,130,246,0.15);
                       border:1px solid rgba(59,130,246,0.3);border-radius:8px;
                       color:#60a5fa;font-size:12px;font-weight:600;cursor:pointer;
                       font-family:'Inter',sans-serif;transition:background 0.15s;"
                onmouseover="this.style.background='rgba(59,130,246,0.25)'"
                onmouseout="this.style.background='rgba(59,130,246,0.15)'">
          View Full Dashboard
        </button>
      </div>
    `;
  }

  setFilter(filter: 'ALL' | Severity): void {
    this.activeFilter = filter;
    this.filteredConflicts = filter === 'ALL'
      ? this.conflicts
      : this.conflicts.filter(c => c.severity === filter);
    this.renderMarkers();
  }

  selectConflict(conflict: ConflictMap): void {
    this.map.flyTo([conflict.latitude, conflict.longitude], 5, {
      duration: 1.5
    });
    this.sidebarOpen = false;
    this.router.navigate(['/conflict', conflict.id]);
  }

  countBySeverity(severity: Severity): number {
    return this.conflicts.filter(c => c.severity === severity).length;
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
    this.markerLayer.clearLayers();
    this.map?.remove();
  }
}
