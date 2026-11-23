import { Component, Input } from '@angular/core';

import { AppError } from '../../../core/errors/app-error';

/**
 * Banner visual para un {@link AppError}. Pinta title + detail en un
 * recuadro rojo. Se oculta si {@code error} es {@code null} (patrón
 * "render-else-noop" via *ngIf no es necesario: devolvemos vista vacía).
 *
 * Deliberadamente simple en el commit 10. El commit 11 lo extenderá
 * para mostrar la lista de {@code fieldErrors} cuando llegue un 422
 * {@code validation-error}.
 *
 * Se usa como componente standalone compartido:
 *   &lt;app-error-banner [error]="err()"&gt;&lt;/app-error-banner&gt;
 */
@Component({
  selector: 'app-error-banner',
  standalone: true,
  template: `
    @if (error) {
      <div class="banner" role="alert">
        <strong class="title">{{ error.title }}</strong>
        <p class="detail">{{ error.detail }}</p>
      </div>
    }
  `,
  styles: [`
    .banner {
      background: #fef2f2;
      border: 1px solid #fecaca;
      color: #991b1b;
      padding: 12px 16px;
      border-radius: 6px;
      margin-bottom: 16px;
    }
    .title { display: block; margin-bottom: 4px; font-size: 0.95rem; }
    .detail { margin: 0; font-size: 0.875rem; color: #7f1d1d; }
  `],
})
export class ErrorBannerComponent {
  @Input() error: AppError | null = null;
}
