import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HeaderComponent } from './layout/header/header.component';

@Component({
  selector: 'cv-root',
  standalone: true,
  imports: [RouterOutlet, HeaderComponent],
  template: `
    <cv-header></cv-header>
    <main class="main-content">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    :host {
      display: flex;
      flex-direction: column;
      height: 100vh;
      overflow: hidden;
    }
    .main-content {
      flex: 1;
      overflow: hidden;
      position: relative;
    }
  `]
})
export class AppComponent {}
