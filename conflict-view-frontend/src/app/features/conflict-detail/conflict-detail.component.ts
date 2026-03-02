import { Component, OnInit, OnDestroy, inject, Input, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgxChartsModule, LegendPosition } from '@swimlane/ngx-charts';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { ConflictService } from '../../core/services/conflict.service';
import { ConflictDetail, NewsArticle, ConflictEvent, ConflictStats, PageResponse, OsintResource, OsintSummary, ResourceType } from '../../core/models/conflict.model';
import { SeverityBadgeComponent } from '../../shared/components/severity-badge/severity-badge.component';
import { NewsCardComponent } from '../../shared/components/news-card/news-card.component';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { TimeAgoPipe } from '../../shared/pipes/time-ago.pipe';
import { ConflictTypeLabelPipe } from '../../shared/pipes/conflict-type-label.pipe';

@Component({
  selector: 'cv-conflict-detail',
  standalone: true,
  imports: [
    CommonModule, RouterLink, FormsModule,
    MatTabsModule, MatIconModule, MatButtonModule,
    MatSelectModule, MatFormFieldModule, MatChipsModule, MatTooltipModule,
    NgxChartsModule,
    SeverityBadgeComponent, NewsCardComponent, LoadingSpinnerComponent, TimeAgoPipe, ConflictTypeLabelPipe
  ],
  template: `
    <div class="detail-page">
      <cv-loading-spinner *ngIf="loading"></cv-loading-spinner>

      <ng-container *ngIf="!loading && conflict">
        <!-- Hero section -->
        <div class="hero">
          <!-- Thumbnail overlay -->
          <div class="hero-bg"
               [style.background-image]="conflict.thumbnailUrl ? 'url(' + conflict.thumbnailUrl + ')' : 'none'">
          </div>
          <div class="hero-content">
            <div class="hero-breadcrumb">
              <a routerLink="/map">
                <mat-icon>map</mat-icon>
                Live Map
              </a>
              <mat-icon class="bc-sep">chevron_right</mat-icon>
              <span>{{ conflict.region }}</span>
              <mat-icon class="bc-sep">chevron_right</mat-icon>
              <span>{{ conflict.name }}</span>
            </div>

            <div class="hero-header">
              <div>
                <div class="hero-badges">
                  <cv-severity-badge [severity]="conflict.severity" [status]="conflict.status">
                  </cv-severity-badge>
                  <span class="type-badge">{{ conflict.conflictType | conflictTypeLabel }}</span>
                </div>
                <h1 class="hero-title">{{ conflict.name }}</h1>
                <p class="hero-subtitle">
                  <mat-icon>location_on</mat-icon>
                  {{ conflict.region }}
                  <span *ngIf="conflict.startDate"> · Since {{ conflict.startDate | date:'MMM yyyy' }}</span>
                </p>
              </div>

              <!-- Quick stats -->
              <div class="hero-stats">
                <div class="hero-stat" *ngIf="conflict.casualtyEstimate">
                  <span class="hs-num">{{ conflict.casualtyEstimate | number }}</span>
                  <span class="hs-label">Est. casualties</span>
                </div>
                <div class="hero-stat" *ngIf="conflict.displacedEstimate">
                  <span class="hs-num">{{ conflict.displacedEstimate | number }}</span>
                  <span class="hs-label">Displaced</span>
                </div>
                <div class="hero-stat">
                  <span class="hs-num">{{ conflict.articleCount | number }}</span>
                  <span class="hs-label">Reports</span>
                </div>
                <div class="hero-stat">
                  <span class="hs-num">{{ conflict.eventCount }}</span>
                  <span class="hs-label">Events tracked</span>
                </div>
              </div>
            </div>

            <p class="hero-summary">{{ conflict.summary }}</p>

            <div class="parties" *ngIf="conflict.involvedParties">
              <mat-icon>groups</mat-icon>
              <span><strong>Involved parties:</strong> {{ conflict.involvedParties }}</span>
            </div>
          </div>
        </div>

        <!-- Tabs -->
        <div class="tabs-wrap">
          <mat-tab-group animationDuration="200ms" class="detail-tabs">

            <!-- NEWS FEED -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">feed</mat-icon>
                News Feed
                <span class="tab-badge">{{ newsPage?.totalElements | number }}</span>
              </ng-template>

              <div class="tab-content news-tab">
                <!-- Filters -->
                <div class="news-filters">
                  <div class="filter-label">Filter by:</div>
                  <div class="filter-chips">
                    <button class="fchip" [class.active]="!newsFilter.sentiment"
                            (click)="setSentimentFilter(null)">All</button>
                    <button class="fchip negative" [class.active]="newsFilter.sentiment === 'NEGATIVE'"
                            (click)="setSentimentFilter('NEGATIVE')">
                      <mat-icon>trending_down</mat-icon> Negative
                    </button>
                    <button class="fchip neutral" [class.active]="newsFilter.sentiment === 'NEUTRAL'"
                            (click)="setSentimentFilter('NEUTRAL')">
                      <mat-icon>remove</mat-icon> Neutral
                    </button>
                    <button class="fchip positive" [class.active]="newsFilter.sentiment === 'POSITIVE'"
                            (click)="setSentimentFilter('POSITIVE')">
                      <mat-icon>trending_up</mat-icon> Positive
                    </button>
                  </div>

                  <div class="source-select">
                    <mat-form-field appearance="outline" class="source-field">
                      <mat-label>Source</mat-label>
                      <mat-select [(ngModel)]="newsFilter.source" (ngModelChange)="loadNews(0)">
                        <mat-option [value]="null">All Sources</mat-option>
                        <mat-option *ngFor="let s of availableSources" [value]="s">{{ s }}</mat-option>
                      </mat-select>
                    </mat-form-field>
                  </div>
                </div>

                <!-- News cards grid -->
                <div class="news-grid" *ngIf="!newsLoading">
                  <cv-news-card *ngFor="let article of articles" [article]="article">
                  </cv-news-card>
                </div>
                <cv-loading-spinner *ngIf="newsLoading"></cv-loading-spinner>

                <!-- Pagination -->
                <div class="pagination" *ngIf="newsPage && newsPage.totalPages > 1">
                  <button class="page-btn" [disabled]="newsPage.first"
                          (click)="loadNews(newsPage.number - 1)">
                    <mat-icon>chevron_left</mat-icon>
                  </button>
                  <span class="page-info">
                    Page {{ newsPage.number + 1 }} of {{ newsPage.totalPages }}
                  </span>
                  <button class="page-btn" [disabled]="newsPage.last"
                          (click)="loadNews(newsPage.number + 1)">
                    <mat-icon>chevron_right</mat-icon>
                  </button>
                </div>
              </div>
            </mat-tab>

            <!-- TIMELINE -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">timeline</mat-icon>
                Timeline
              </ng-template>
              <div class="tab-content">
                <div class="timeline" *ngIf="events.length">
                  <div class="timeline-item" *ngFor="let event of events">
                    <div class="timeline-date">
                      <span class="tl-month">{{ event.eventDate | date:'MMM' }}</span>
                      <span class="tl-day">{{ event.eventDate | date:'d' }}</span>
                      <span class="tl-year">{{ event.eventDate | date:'yyyy' }}</span>
                    </div>
                    <div class="timeline-connector">
                      <div class="tl-dot"></div>
                      <div class="tl-line"></div>
                    </div>
                    <div class="timeline-content">
                      <div class="tl-type">{{ event.eventType }}</div>
                      <p class="tl-desc">{{ event.description }}</p>
                      <div class="tl-meta">
                        <span *ngIf="event.fatalitiesReported" class="tl-casualties">
                          <mat-icon>warning</mat-icon>
                          {{ event.fatalitiesReported }} casualties reported
                        </span>
                        <a *ngIf="event.sourceUrl" [href]="event.sourceUrl"
                           target="_blank" rel="noopener noreferrer" class="tl-src">
                          Source ↗
                        </a>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="empty-state" *ngIf="!events.length">
                  <mat-icon>history</mat-icon>
                  <p>Timeline events are being collected for this conflict.</p>
                </div>
              </div>
            </mat-tab>

            <!-- ANALYTICS -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">analytics</mat-icon>
                Analytics
              </ng-template>
              <div class="tab-content analytics-tab" *ngIf="stats">
                <div class="charts-grid">

                  <!-- Coverage trend -->
                  <div class="chart-card">
                    <h3>Monthly Coverage Trend</h3>
                    <ngx-charts-area-chart
                      [results]="incidentTrendData"
                      [xAxis]="true"
                      [yAxis]="true"
                      [legend]="false"
                      [scheme]="chartScheme"
                      [view]="[500, 200]">
                    </ngx-charts-area-chart>
                  </div>

                  <!-- Source breakdown -->
                  <div class="chart-card">
                    <h3>Articles by Source</h3>
                    <ngx-charts-pie-chart
                      [results]="sourceBreakdownData"
                      [legend]="true"
                      [labels]="false"
                      [doughnut]="true"
                      [arcWidth]="0.35"
                      [legendPosition]="legendPos"
                      [scheme]="chartScheme"
                      [view]="[500, 220]">
                    </ngx-charts-pie-chart>
                  </div>

                  <!-- Sentiment breakdown -->
                  <div class="chart-card">
                    <h3>Sentiment Distribution</h3>
                    <ngx-charts-bar-vertical
                      [results]="sentimentData"
                      [xAxis]="true"
                      [yAxis]="true"
                      [roundEdges]="true"
                      [showDataLabel]="true"
                      [scheme]="sentimentScheme"
                      [view]="[500, 200]">
                    </ngx-charts-bar-vertical>
                  </div>

                  <!-- Event types -->
                  <div class="chart-card">
                    <h3>Event Types</h3>
                    <ngx-charts-bar-horizontal
                      [results]="eventTypesData"
                      [xAxis]="true"
                      [yAxis]="true"
                      [roundEdges]="true"
                      [showDataLabel]="true"
                      [scheme]="chartScheme"
                      [view]="[500, 220]">
                    </ngx-charts-bar-horizontal>
                  </div>
                </div>

                <!-- Stats summary row -->
                <div class="stats-summary">
                  <div class="ss-item">
                    <span class="ss-num">{{ stats.totalArticles | number }}</span>
                    <span class="ss-label">Total Articles</span>
                  </div>
                  <div class="ss-item">
                    <span class="ss-num">{{ stats.totalEvents | number }}</span>
                    <span class="ss-label">Events Tracked</span>
                  </div>
                  <div class="ss-item">
                    <span class="ss-num">{{ (stats.averageReliabilityScore || 0) | number:'1.0-0' }}</span>
                    <span class="ss-label">Avg. Source Reliability</span>
                  </div>
                  <div class="ss-item">
                    <span class="ss-num">{{ Object.keys(stats.articlesBySource).length }}</span>
                    <span class="ss-label">Unique Sources</span>
                  </div>
                </div>
              </div>
            </mat-tab>

            <!-- OSINT -->
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon class="tab-icon">policy</mat-icon>
                OSINT
                <span class="tab-badge" *ngIf="osintSummary">{{ osintSummary.totalCount | number }}</span>
              </ng-template>
              <div class="tab-content osint-tab">
                <!-- Type filter chips -->
                <div class="news-filters">
                  <div class="filter-label">Resource type:</div>
                  <div class="filter-chips">
                    <button class="fchip" [class.active]="!osintTypeFilter"
                            (click)="setOsintTypeFilter(null)">
                      All <span class="chip-count" *ngIf="osintSummary">({{ osintSummary.totalCount }})</span>
                    </button>
                    <button class="fchip" [class.active]="osintTypeFilter === 'VIDEO'"
                            (click)="setOsintTypeFilter('VIDEO')">
                      <mat-icon>play_circle</mat-icon> Videos
                      <span class="chip-count" *ngIf="osintSummary">({{ osintSummary.videoCount }})</span>
                    </button>
                    <button class="fchip" [class.active]="osintTypeFilter === 'IMAGE'"
                            (click)="setOsintTypeFilter('IMAGE')">
                      <mat-icon>image</mat-icon> Images
                      <span class="chip-count" *ngIf="osintSummary">({{ osintSummary.imageCount }})</span>
                    </button>
                    <button class="fchip" [class.active]="osintTypeFilter === 'MAP'"
                            (click)="setOsintTypeFilter('MAP')">
                      <mat-icon>map</mat-icon> Maps
                      <span class="chip-count" *ngIf="osintSummary">({{ osintSummary.mapCount }})</span>
                    </button>
                    <button class="fchip" [class.active]="osintTypeFilter === 'INFOGRAPHIC'"
                            (click)="setOsintTypeFilter('INFOGRAPHIC')">
                      <mat-icon>insert_chart</mat-icon> Infographics
                      <span class="chip-count" *ngIf="osintSummary">({{ osintSummary.infographicCount }})</span>
                    </button>
                    <button class="fchip" [class.active]="osintTypeFilter === 'REPORT'"
                            (click)="setOsintTypeFilter('REPORT')">
                      <mat-icon>description</mat-icon> Reports
                      <span class="chip-count" *ngIf="osintSummary">({{ osintSummary.reportCount }})</span>
                    </button>
                  </div>
                </div>

                <!-- OSINT resource grid -->
                <cv-loading-spinner *ngIf="osintLoading"></cv-loading-spinner>
                <div class="osint-grid" *ngIf="!osintLoading && osintResources.length">
                  <div class="osint-card" *ngFor="let res of osintResources" (click)="openResource(res)">
                    <div class="osint-thumb" [class.video-thumb]="res.resourceType === 'VIDEO'">
                      <img *ngIf="res.thumbnailUrl" [src]="res.thumbnailUrl" [alt]="res.title"
                           (error)="onOsintImgError($event, res)" loading="lazy">
                      <div class="osint-no-thumb" *ngIf="!res.thumbnailUrl">
                        <mat-icon>{{ getResourceIcon(res.resourceType) }}</mat-icon>
                      </div>
                      <div class="osint-type-overlay">
                        <mat-icon>{{ getResourceIcon(res.resourceType) }}</mat-icon>
                      </div>
                    </div>
                    <div class="osint-info">
                      <div class="osint-meta">
                        <span class="osint-platform" [attr.data-platform]="res.sourcePlatform">
                          {{ res.sourcePlatform }}
                        </span>
                        <span class="osint-type-label">{{ res.resourceType }}</span>
                        <span class="osint-date" *ngIf="res.publishedAt">{{ res.publishedAt | timeAgo }}</span>
                      </div>
                      <h4 class="osint-title">{{ res.title }}</h4>
                      <p class="osint-desc" *ngIf="res.description">{{ res.description }}</p>
                      <div class="osint-author" *ngIf="res.author">
                        <mat-icon>person</mat-icon> {{ res.author }}
                      </div>
                    </div>
                    <mat-icon class="external-icon">open_in_new</mat-icon>
                  </div>
                </div>

                <!-- Empty state -->
                <div class="empty-state" *ngIf="!osintLoading && !osintResources.length">
                  <mat-icon>policy</mat-icon>
                  <p>No OSINT resources found for this conflict yet.</p>
                  <p style="font-size:12px;color:var(--text-muted)">Resources are aggregated from YouTube, ReliefWeb, Wikimedia, and Internet Archive.</p>
                </div>

                <!-- Pagination -->
                <div class="pagination" *ngIf="osintPage && osintPage.totalPages > 1">
                  <button class="page-btn" [disabled]="osintPage.first"
                          (click)="loadOsint(osintPage.number - 1)">
                    <mat-icon>chevron_left</mat-icon>
                  </button>
                  <span class="page-info">
                    Page {{ osintPage.number + 1 }} of {{ osintPage.totalPages }}
                  </span>
                  <button class="page-btn" [disabled]="osintPage.last"
                          (click)="loadOsint(osintPage.number + 1)">
                    <mat-icon>chevron_right</mat-icon>
                  </button>
                </div>
              </div>
            </mat-tab>

          </mat-tab-group>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .detail-page {
      height: 100%;
      overflow-y: auto;
      background: var(--bg-primary);
    }

    /* ======= Hero ======= */
    .hero {
      position: relative;
      min-height: 280px;
      overflow: hidden;
    }
    .hero-bg {
      position: absolute;
      inset: 0;
      background-size: cover;
      background-position: center;
      filter: blur(8px) brightness(0.25);
      transform: scale(1.05);
    }
    .hero-content {
      position: relative;
      padding: 24px 32px 28px;
    }
    .hero-breadcrumb {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: rgba(255,255,255,0.5);
      margin-bottom: 16px;
      a {
        display: flex;
        align-items: center;
        gap: 4px;
        color: var(--accent-blue-light);
        text-decoration: none;
        &:hover { color: #fff; }
        mat-icon { font-size: 14px; }
      }
      .bc-sep { font-size: 14px; color: rgba(255,255,255,0.3); }
    }
    .hero-header {
      display: flex;
      align-items: flex-start;
      justify-content: space-between;
      gap: 24px;
      margin-bottom: 16px;
    }
    .hero-badges {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 10px;
    }
    .type-badge {
      padding: 3px 10px;
      border-radius: 20px;
      background: rgba(59,130,246,0.12);
      color: var(--accent-blue-light);
      border: 1px solid rgba(59,130,246,0.3);
      font-size: 11px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .hero-title {
      font-size: 28px;
      font-weight: 800;
      color: #fff;
      margin-bottom: 6px;
      letter-spacing: -0.02em;
    }
    .hero-subtitle {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 14px;
      color: rgba(255,255,255,0.6);
      mat-icon { font-size: 15px; }
    }
    .hero-stats {
      display: flex;
      gap: 20px;
      flex-shrink: 0;
    }
    .hero-stat {
      text-align: right;
      .hs-num {
        display: block;
        font-size: 24px;
        font-weight: 700;
        color: #fff;
        line-height: 1.1;
      }
      .hs-label {
        font-size: 11px;
        color: rgba(255,255,255,0.5);
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }
    }
    .hero-summary {
      font-size: 14px;
      color: rgba(255,255,255,0.75);
      line-height: 1.7;
      max-width: 800px;
      margin-bottom: 12px;
    }
    .parties {
      display: flex;
      align-items: flex-start;
      gap: 6px;
      font-size: 13px;
      color: rgba(255,255,255,0.6);
      mat-icon { font-size: 16px; flex-shrink: 0; margin-top: 2px; }
      strong { color: rgba(255,255,255,0.85); }
    }

    /* ======= Tabs ======= */
    .tabs-wrap {
      padding: 0 32px 32px;
    }
    ::ng-deep .detail-tabs {
      .mat-mdc-tab-header {
        background: var(--bg-card);
        border: 1px solid var(--border-color);
        border-radius: var(--radius-lg) var(--radius-lg) 0 0;
      }
      .mat-mdc-tab-body-wrapper {
        background: var(--bg-card);
        border: 1px solid var(--border-color);
        border-top: none;
        border-radius: 0 0 var(--radius-lg) var(--radius-lg);
      }
      .mat-mdc-tab-label-container { padding: 0 16px; }
      .mat-mdc-tab { color: var(--text-secondary) !important; font-family: 'Inter', sans-serif !important; font-weight: 500 !important; }
      .mat-mdc-tab.mdc-tab--active { color: var(--accent-blue-light) !important; }
      .mdc-tab-indicator__content--underline { border-color: var(--accent-blue) !important; }
    }
    .tab-icon { font-size: 16px; margin-right: 6px; vertical-align: middle; }
    .tab-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 22px;
      height: 18px;
      padding: 0 6px;
      border-radius: 20px;
      background: rgba(59,130,246,0.15);
      color: var(--accent-blue-light);
      font-size: 10px;
      font-weight: 700;
      margin-left: 6px;
    }

    .tab-content { padding: 20px; }

    /* ======= News tab ======= */
    .news-tab {}
    .news-filters {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }
    .filter-label { font-size: 12px; color: var(--text-muted); font-weight: 500; }
    .filter-chips { display: flex; gap: 6px; }
    .fchip {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 5px 12px;
      border-radius: 20px;
      border: 1px solid var(--border-color);
      background: transparent;
      color: var(--text-secondary);
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      transition: all var(--transition-fast);
      font-family: 'Inter', sans-serif;
      mat-icon { font-size: 14px; }
      &:hover { background: rgba(255,255,255,0.05); color: var(--text-primary); }
      &.active { border-color: var(--accent-blue); background: rgba(59,130,246,0.1); color: var(--accent-blue-light); }
      &.negative.active { border-color: #ef4444; background: rgba(239,68,68,0.1); color: #ef4444; }
      &.positive.active { border-color: #22c55e; background: rgba(34,197,94,0.1); color: #22c55e; }
    }
    .source-select { margin-left: auto; }
    .source-field {
      width: 160px;
      ::ng-deep .mat-mdc-form-field-wrapper { padding-bottom: 0; }
      ::ng-deep .mat-mdc-text-field-wrapper { background: var(--bg-secondary) !important; }
    }

    .news-grid { display: flex; flex-direction: column; gap: 8px; }

    .pagination {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 16px;
      margin-top: 20px;
      padding-top: 16px;
      border-top: 1px solid var(--border-color);
    }
    .page-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: 8px;
      border: 1px solid var(--border-color);
      background: transparent;
      color: var(--text-secondary);
      cursor: pointer;
      transition: all var(--transition-fast);
      &:hover:not(:disabled) { background: var(--bg-card-hover); color: var(--text-primary); }
      &:disabled { opacity: 0.3; cursor: not-allowed; }
    }
    .page-info { font-size: 13px; color: var(--text-secondary); }

    /* ======= Timeline ======= */
    .timeline { display: flex; flex-direction: column; position: relative; }
    .timeline-item {
      display: flex;
      gap: 16px;
      position: relative;
      min-height: 80px;
      &:last-child .tl-line { display: none; }
    }
    .timeline-date {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 44px;
      flex-shrink: 0;
      padding-top: 4px;
      .tl-month { font-size: 10px; font-weight: 600; color: var(--accent-blue); text-transform: uppercase; letter-spacing: 0.05em; }
      .tl-day { font-size: 20px; font-weight: 700; color: var(--text-primary); line-height: 1; }
      .tl-year { font-size: 10px; color: var(--text-muted); }
    }
    .timeline-connector {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 20px;
      flex-shrink: 0;
    }
    .tl-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
      background: var(--accent-blue);
      border: 2px solid var(--bg-card);
      box-shadow: 0 0 8px rgba(59,130,246,0.5);
      flex-shrink: 0;
      margin-top: 6px;
    }
    .tl-line {
      flex: 1;
      width: 2px;
      background: var(--border-color);
      margin-top: 4px;
    }
    .timeline-content {
      flex: 1;
      padding-bottom: 24px;
    }
    .tl-type {
      font-size: 12px;
      font-weight: 600;
      color: var(--accent-blue-light);
      margin-bottom: 4px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }
    .tl-desc {
      font-size: 14px;
      color: var(--text-secondary);
      line-height: 1.6;
      margin-bottom: 8px;
    }
    .tl-meta {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .tl-casualties {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #ef4444;
      background: rgba(239,68,68,0.1);
      padding: 2px 8px;
      border-radius: 12px;
      mat-icon { font-size: 13px; }
    }
    .tl-src {
      font-size: 12px;
      color: var(--accent-blue);
      text-decoration: none;
      &:hover { color: var(--accent-blue-light); }
    }

    /* ======= Analytics ======= */
    .analytics-tab {}
    .charts-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 16px;
      margin-bottom: 20px;
    }
    .chart-card {
      background: var(--bg-secondary);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      padding: 16px;
      h3 {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-primary);
        margin-bottom: 12px;
      }
    }
    .stats-summary {
      display: flex;
      gap: 0;
      background: var(--bg-secondary);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      overflow: hidden;
    }
    .ss-item {
      flex: 1;
      text-align: center;
      padding: 20px;
      border-right: 1px solid var(--border-color);
      &:last-child { border-right: none; }
      .ss-num { display: block; font-size: 24px; font-weight: 700; color: var(--text-primary); }
      .ss-label { font-size: 11px; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.05em; }
    }

    .empty-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 64px 24px;
      color: var(--text-muted);
      mat-icon { font-size: 48px; opacity: 0.4; }
      p { font-size: 14px; }
    }

    /* ======= OSINT tab ======= */
    .chip-count { font-size: 10px; opacity: 0.7; margin-left: 2px; }
    .osint-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 12px;
    }
    .osint-card {
      display: flex;
      flex-direction: column;
      background: var(--bg-secondary);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      cursor: pointer;
      overflow: hidden;
      transition: all var(--transition-base);
      position: relative;
      &:hover {
        background: var(--bg-card-hover);
        border-color: var(--border-color-hover);
        transform: translateY(-2px);
        box-shadow: var(--shadow-md);
        .external-icon { opacity: 1; }
        .osint-type-overlay { opacity: 1; }
      }
    }
    .osint-thumb {
      position: relative;
      width: 100%;
      height: 160px;
      background: var(--bg-primary);
      overflow: hidden;
      img {
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
    .osint-thumb.video-thumb { height: 180px; }
    .osint-no-thumb {
      width: 100%;
      height: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      mat-icon { font-size: 40px; color: var(--text-muted); opacity: 0.4; }
    }
    .osint-type-overlay {
      position: absolute;
      bottom: 8px;
      left: 8px;
      background: rgba(0,0,0,0.7);
      border-radius: 6px;
      padding: 4px 8px;
      display: flex;
      align-items: center;
      opacity: 0.7;
      transition: opacity var(--transition-fast);
      mat-icon { font-size: 16px; color: #fff; }
    }
    .osint-info {
      padding: 12px;
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    .osint-meta {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 6px;
      font-size: 10px;
      flex-wrap: wrap;
    }
    .osint-platform {
      padding: 2px 8px;
      border-radius: 10px;
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.03em;
      font-size: 9px;
    }
    .osint-platform[data-platform="YouTube"] { background: rgba(239,68,68,0.15); color: #ef4444; }
    .osint-platform[data-platform="ReliefWeb"] { background: rgba(59,130,246,0.15); color: #3b82f6; }
    .osint-platform[data-platform="Wikimedia"] { background: rgba(107,114,128,0.15); color: #9ca3af; }
    .osint-platform[data-platform="InternetArchive"] { background: rgba(249,115,22,0.15); color: #f97316; }
    .osint-type-label {
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.05em;
      font-weight: 500;
    }
    .osint-date { color: var(--text-muted); }
    .osint-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--text-primary);
      line-height: 1.4;
      margin-bottom: 4px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .osint-desc {
      font-size: 11px;
      color: var(--text-secondary);
      margin-bottom: 6px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .osint-author {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 10px;
      color: var(--text-muted);
      margin-top: auto;
      mat-icon { font-size: 12px; }
    }
    .osint-card .external-icon {
      position: absolute;
      top: 10px;
      right: 10px;
      font-size: 16px;
      color: #fff;
      opacity: 0;
      transition: opacity var(--transition-fast);
      background: rgba(0,0,0,0.5);
      border-radius: 50%;
      padding: 4px;
    }

    @media (max-width: 768px) {
      .hero-content { padding: 16px; }
      .tabs-wrap { padding: 0 16px 16px; }
      .hero-header { flex-direction: column; }
      .hero-stats { gap: 12px; }
      .charts-grid { grid-template-columns: 1fr; }
      .osint-grid { grid-template-columns: 1fr; }
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ConflictDetailComponent implements OnInit, OnDestroy {
  @Input() id!: string;

  private conflictService = inject(ConflictService);
  private cdr = inject(ChangeDetectorRef);
  private destroy$ = new Subject<void>();

  conflict: ConflictDetail | null = null;
  articles: NewsArticle[] = [];
  events: ConflictEvent[] = [];
  stats: ConflictStats | null = null;
  newsPage: PageResponse<NewsArticle> | null = null;
  availableSources: string[] = [];

  loading = true;
  newsLoading = false;

  newsFilter: { sentiment: string | null; source: string | null } = {
    sentiment: null,
    source: null
  };

  protected readonly Object = Object;

  chartScheme: any = { domain: ['#3b82f6', '#f97316', '#22c55e', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#eab308'] };
  sentimentScheme: any = { domain: ['#ef4444', '#6b7280', '#22c55e'] };
  legendPos = LegendPosition.Right;

  incidentTrendData: any[] = [];
  sourceBreakdownData: any[] = [];
  sentimentData: any[] = [];
  eventTypesData: any[] = [];

  // OSINT
  osintResources: OsintResource[] = [];
  osintSummary: OsintSummary | null = null;
  osintPage: PageResponse<OsintResource> | null = null;
  osintTypeFilter: string | null = null;
  osintLoading = false;

  ngOnInit(): void {
    this.loadConflict();
    this.loadNews(0);
    this.loadEvents();
    this.loadStats();
    this.loadOsintSummary();
    this.loadOsint(0);
  }

  private loadConflict(): void {
    this.conflictService.getDetail(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.conflict = data; this.loading = false; this.cdr.markForCheck(); },
      error: () => { this.loading = false; this.cdr.markForCheck(); }
    });
  }

  loadNews(page: number): void {
    this.newsLoading = true;
    this.conflictService.getNews(
      this.id, page, 20,
      this.newsFilter.sentiment ?? undefined,
      this.newsFilter.source ?? undefined
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => {
        this.newsPage = data;
        this.articles = data.content;
        if (!this.availableSources.length) {
          this.availableSources = [...new Set(data.content.map(a => a.sourceDomain).filter(Boolean))];
        }
        this.newsLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.newsLoading = false; this.cdr.markForCheck(); }
    });
  }

  private loadEvents(): void {
    this.conflictService.getEvents(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.events = data; this.cdr.markForCheck(); }
    });
  }

  private loadStats(): void {
    this.conflictService.getStats(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => {
        this.stats = data;
        this.incidentTrendData = [{
          name: 'Articles',
          series: data.monthlyTrend.map(d => ({ name: d.month, value: d.incidents }))
        }];
        this.sourceBreakdownData = Object.entries(data.articlesBySource).slice(0, 8).map(([name, value]) => ({ name, value }));
        this.sentimentData = data.sentimentBreakdown ? Object.entries(data.sentimentBreakdown).map(([name, value]) => ({ name, value })) : [];
        this.eventTypesData = data.eventsByType ? Object.entries(data.eventsByType).filter(([_, v]) => v > 0).map(([name, value]) => ({ name, value })) : [];
        this.cdr.markForCheck();
      }
    });
  }

  setSentimentFilter(sentiment: string | null): void {
    this.newsFilter.sentiment = sentiment;
    this.loadNews(0);
  }

  // ---- OSINT ----
  loadOsint(page: number): void {
    this.osintLoading = true;
    this.cdr.markForCheck();
    this.conflictService.getOsint(
      this.id, page, 20,
      this.osintTypeFilter ?? undefined
    ).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => {
        this.osintPage = data;
        this.osintResources = data.content;
        this.osintLoading = false;
        this.cdr.markForCheck();
      },
      error: () => { this.osintLoading = false; this.cdr.markForCheck(); }
    });
  }

  private loadOsintSummary(): void {
    this.conflictService.getOsintSummary(this.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: data => { this.osintSummary = data; this.cdr.markForCheck(); }
    });
  }

  setOsintTypeFilter(type: string | null): void {
    this.osintTypeFilter = type;
    this.loadOsint(0);
  }

  openResource(res: OsintResource): void {
    window.open(res.url, '_blank', 'noopener,noreferrer');
  }

  getResourceIcon(type: string): string {
    switch (type) {
      case 'VIDEO': return 'play_circle';
      case 'IMAGE': return 'image';
      case 'MAP': return 'map';
      case 'INFOGRAPHIC': return 'insert_chart';
      case 'REPORT': return 'description';
      default: return 'link';
    }
  }

  onOsintImgError(event: Event, res: OsintResource): void {
    res.thumbnailUrl = null;
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
