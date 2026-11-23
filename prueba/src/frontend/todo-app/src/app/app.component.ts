import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

/**
 * Componente raíz de la app. Reescrito desde el scaffold de "todo-app":
 *
 *  - Layout de dos regiones: una nav superior con los 3 enlaces del MVP
 *    y un <router-outlet /> que monta la vista correspondiente.
 *  - Standalone (no hay NgModules). Sin signals todavía: se usan en los
 *    servicios y componentes de feature.
 *  - Estilos inline con CSS vanilla (D15: sin Tailwind ni Material; es
 *    una prueba de 4 h y la UI debe ser legible, no bonita).
 */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="app-header">
      <h1 class="app-title">Biblioteca de herramientas</h1>
      <nav class="app-nav">
        <a routerLink="/herramientas" routerLinkActive="active">Catálogo</a>
        <a routerLink="/nueva-reserva" routerLinkActive="active">Nueva reserva</a>
        <a routerLink="/mis-reservas"  routerLinkActive="active">Mis reservas</a>
      </nav>
    </header>

    <main class="app-main">
      <router-outlet />
    </main>
  `,
  styles: [`
    :host { display: block; min-height: 100vh; background: #f1f5f9; }
    .app-header {
      background: #1e293b;
      color: #f8fafc;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      gap: 32px;
      flex-wrap: wrap;
    }
    .app-title { margin: 0; font-size: 1.25rem; font-weight: 600; }
    .app-nav { display: flex; gap: 16px; }
    .app-nav a {
      color: #cbd5e1;
      text-decoration: none;
      padding: 6px 12px;
      border-radius: 4px;
      font-size: 0.9rem;
    }
    .app-nav a:hover   { background: #334155; color: #f8fafc; }
    .app-nav a.active  { background: #2563eb; color: #fff; }
    .app-main { padding: 24px; max-width: 1100px; margin: 0 auto; }
  `]
})
export class AppComponent {}
