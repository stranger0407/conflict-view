import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'map',
    pathMatch: 'full'
  },
  {
    path: 'map',
    loadComponent: () => import('./features/map/map.component').then(m => m.MapComponent),
    title: 'ConflictView — Live Map'
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    title: 'ConflictView — Dashboard'
  },
  {
    path: 'conflict/:id',
    loadComponent: () => import('./features/conflict-detail/conflict-detail.component').then(m => m.ConflictDetailComponent),
    title: 'ConflictView — Conflict Detail'
  },
  {
    path: 'search',
    loadComponent: () => import('./features/search/search.component').then(m => m.SearchComponent),
    title: 'ConflictView — Search'
  },
  {
    path: '**',
    redirectTo: 'map'
  }
];
