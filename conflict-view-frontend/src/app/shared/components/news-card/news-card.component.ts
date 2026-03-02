import { Component, Input, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NewsArticle } from '../../../core/models/conflict.model';
import { ReliabilityBadgeComponent } from '../reliability-badge/reliability-badge.component';
import { SentimentIndicatorComponent } from '../sentiment-indicator/sentiment-indicator.component';
import { TimeAgoPipe } from '../../pipes/time-ago.pipe';

@Component({
  selector: 'cv-news-card',
  standalone: true,
  imports: [
    CommonModule, MatCardModule, MatIconModule, MatButtonModule, MatTooltipModule,
    ReliabilityBadgeComponent, SentimentIndicatorComponent, TimeAgoPipe
  ],
  template: `
    <article class="news-card" (click)="openArticle()">
      <!-- Image -->
      <div class="image-wrap" *ngIf="article.imageUrl">
        <img [src]="article.imageUrl" [alt]="article.title"
             (error)="onImgError($event)" loading="lazy">
      </div>
      <div class="no-image" *ngIf="!article.imageUrl">
        <mat-icon>article</mat-icon>
      </div>

      <!-- Content -->
      <div class="content">
        <div class="meta">
          <span class="source">{{ article.sourceName }}</span>
          <span class="divider">·</span>
          <span class="time">{{ article.publishedAt | timeAgo }}</span>
        </div>

        <h3 class="title">{{ article.title }}</h3>

        <p class="description" *ngIf="article.description">{{ article.description }}</p>

        <div class="badges">
          <cv-reliability-badge
            [score]="article.reliabilityScore"
            [label]="article.reliabilityLabel">
          </cv-reliability-badge>
          <cv-sentiment-indicator
            [sentiment]="article.sentiment"
            [showLabel]="true">
          </cv-sentiment-indicator>
          <span class="author" *ngIf="article.author">
            <mat-icon class="author-icon">person</mat-icon>
            {{ article.author }}
          </span>
        </div>
      </div>

      <mat-icon class="external-icon">open_in_new</mat-icon>
    </article>
  `,
  styles: [`
    .news-card {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 14px 16px;
      background: var(--bg-card);
      border: 1px solid var(--border-color);
      border-radius: var(--radius-md);
      cursor: pointer;
      transition: all var(--transition-base);
      position: relative;
      overflow: hidden;
      &:hover {
        background: var(--bg-card-hover);
        border-color: var(--border-color-hover);
        transform: translateY(-1px);
        box-shadow: var(--shadow-md);
        .external-icon { opacity: 1; }
      }
    }
    .image-wrap, .no-image {
      width: 80px;
      min-width: 80px;
      height: 60px;
      border-radius: 6px;
      overflow: hidden;
      background: var(--bg-secondary);
      display: flex;
      align-items: center;
      justify-content: center;
    }
    .image-wrap img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
    .no-image mat-icon { color: var(--text-muted); font-size: 24px; }
    .content { flex: 1; min-width: 0; }
    .meta {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 4px;
      font-size: 11px;
      color: var(--text-muted);
    }
    .source { color: var(--accent-blue); font-weight: 500; }
    .title {
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
    .description {
      font-size: 12px;
      color: var(--text-secondary);
      margin-bottom: 8px;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .badges {
      display: flex;
      align-items: center;
      gap: 8px;
      flex-wrap: wrap;
    }
    .author {
      display: flex;
      align-items: center;
      gap: 2px;
      font-size: 10px;
      color: var(--text-muted);
    }
    .author-icon { font-size: 11px; width: 11px; height: 11px; }
    .external-icon {
      position: absolute;
      top: 10px;
      right: 10px;
      font-size: 14px;
      color: var(--text-muted);
      opacity: 0;
      transition: opacity var(--transition-fast);
    }
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class NewsCardComponent {
  @Input({ required: true }) article!: NewsArticle;

  openArticle(): void {
    window.open(this.article.url, '_blank', 'noopener,noreferrer');
  }

  onImgError(event: Event): void {
    const img = event.target as HTMLImageElement;
    img.style.display = 'none';
    img.parentElement!.innerHTML = '<span class="material-icons" style="color:var(--text-muted);font-size:24px">article</span>';
  }
}
