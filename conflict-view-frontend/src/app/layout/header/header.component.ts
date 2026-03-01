import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'cv-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, MatButtonModule, MatIconModule, MatTooltipModule],
  template: `
    <header class="header">
      <div class="brand" routerLink="/">
        <div class="logo-mark">
          <span class="pulse-ring"></span>
          <mat-icon>public</mat-icon>
        </div>
        <div class="brand-text">
          <span class="brand-name">ConflictView</span>
          <span class="brand-tagline">Live Global Tracking</span>
        </div>
      </div>

      <nav class="nav">
        <a class="nav-link" routerLink="/map" routerLinkActive="active">
          <mat-icon>map</mat-icon>
          <span>Live Map</span>
        </a>
        <a class="nav-link" routerLink="/dashboard" routerLinkActive="active">
          <mat-icon>dashboard</mat-icon>
          <span>Dashboard</span>
        </a>
        <a class="nav-link" routerLink="/search" routerLinkActive="active">
          <mat-icon>search</mat-icon>
          <span>Search</span>
        </a>
      </nav>

      <div class="header-right">
        <div class="live-badge">
          <span class="live-dot"></span>
          LIVE
        </div>

        <a href="https://github.com/your-org/conflict-view"
           target="_blank" rel="noopener noreferrer"
           class="icon-btn" matTooltip="View on GitHub">
          <mat-icon>code</mat-icon>
        </a>
      </div>
    </header>
  `,
  styles: [`
    .header {
      height: var(--header-height);
      background: var(--bg-secondary);
      border-bottom: 1px solid var(--border-color);
      display: flex;
      align-items: center;
      padding: 0 20px;
      gap: 24px;
      z-index: 100;
      flex-shrink: 0;
    }

    .brand {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      text-decoration: none;
      flex-shrink: 0;
    }

    .logo-mark {
      position: relative;
      width: 36px;
      height: 36px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #1e3a5f, #2563eb);
      border-radius: 10px;
      mat-icon { color: #fff; font-size: 20px; }
    }

    .pulse-ring {
      position: absolute;
      inset: -4px;
      border-radius: 14px;
      border: 2px solid rgba(37, 99, 235, 0.4);
      animation: headerPulse 3s ease-in-out infinite;
    }
    @keyframes headerPulse {
      0%, 100% { opacity: 0.4; transform: scale(1); }
      50% { opacity: 0.8; transform: scale(1.05); }
    }

    .brand-text {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }
    .brand-name {
      font-size: 16px;
      font-weight: 700;
      color: var(--text-primary);
      letter-spacing: -0.02em;
    }
    .brand-tagline {
      font-size: 10px;
      color: var(--text-muted);
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }

    .nav {
      display: flex;
      align-items: center;
      gap: 4px;
      flex: 1;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 14px;
      border-radius: var(--radius-md);
      font-size: 13px;
      font-weight: 500;
      color: var(--text-secondary);
      text-decoration: none;
      transition: all var(--transition-fast);
      mat-icon { font-size: 18px; width: 18px; height: 18px; }
      &:hover {
        color: var(--text-primary);
        background: rgba(255, 255, 255, 0.05);
      }
      &.active {
        color: var(--accent-blue-light);
        background: rgba(59, 130, 246, 0.1);
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-left: auto;
    }

    .live-badge {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 4px 10px;
      border-radius: 20px;
      background: rgba(239, 68, 68, 0.1);
      border: 1px solid rgba(239, 68, 68, 0.3);
      font-size: 10px;
      font-weight: 700;
      color: #ef4444;
      letter-spacing: 0.1em;
    }

    .live-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: #ef4444;
      animation: blink 1.2s ease-in-out infinite;
    }
    @keyframes blink {
      0%, 100% { opacity: 1; }
      50% { opacity: 0.2; }
    }

    .icon-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: 8px;
      color: var(--text-secondary);
      transition: all var(--transition-fast);
      text-decoration: none;
      mat-icon { font-size: 20px; }
      &:hover {
        color: var(--text-primary);
        background: rgba(255, 255, 255, 0.06);
      }
    }

    @media (max-width: 768px) {
      .brand-tagline, .nav-link span { display: none; }
      .nav-link { padding: 8px; }
    }
  `]
})
export class HeaderComponent {}
