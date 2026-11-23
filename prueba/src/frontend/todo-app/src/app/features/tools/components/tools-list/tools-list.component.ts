import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';

import { AppError } from '../../../../core/errors/app-error';
import { ErrorBannerComponent } from '../../../../shared/components/error-banner/error-banner.component';
import { CATEGORIAS, CATEGORIA_LABEL, Categoria } from '../../models/categoria';
import { ToolsService } from '../../services/tools.service';
import {
  AvailabilityFormComponent,
  AvailabilityQuery,
} from '../availability-form/availability-form.component';

/**
 * Vista /herramientas (R01 + R02).
 *
 * Dos secciones independientes:
 *   1. Cat\u00e1logo \u2014 tabla completa con filtro cliente-side por categor\u00eda.
 *   2. Disponibilidad \u2014 formulario que, al enviar, consulta el backend
 *      con rango y categor\u00eda opcional, y muestra los resultados en
 *      su propia tabla debajo.
 *
 * El estado vive en {@link ToolsService} (signals); este componente
 * solo orquesta UI y traduce errores a un signal local para el banner.
 */
@Component({
  selector: 'app-tools-list',
  standalone: true,
  imports: [CommonModule, AvailabilityFormComponent, ErrorBannerComponent],
  template: `
    <app-error-banner [error]="error()" />

    <section class="card">
      <header class="card-head">
        <h2>Cat\u00e1logo de herramientas</h2>
        <label class="filter">
          <span>Filtrar por categor\u00eda:</span>
          <select [value]="service.categoryFilter() ?? ''" (change)="onFilter($event)">
            <option value="">Todas</option>
            @for (c of categorias; track c) {
              <option [value]="c">{{ categoriaLabel[c] }}</option>
            }
          </select>
        </label>
      </header>

      @if (service.loading()) {
        <p class="loading">Cargando\u2026</p>
      } @else if (service.filteredCatalog().length === 0) {
        <p class="empty">No hay herramientas para la categor\u00eda seleccionada.</p>
      } @else {
        <table class="tbl">
          <thead>
            <tr><th>ID</th><th>Nombre</th><th>Categor\u00eda</th></tr>
          </thead>
          <tbody>
            @for (h of service.filteredCatalog(); track h.id) {
              <tr>
                <td>{{ h.id }}</td>
                <td>{{ h.nombre }}</td>
                <td>{{ categoriaLabel[h.categoria] }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>

    <section class="card">
      <h2>Consulta de disponibilidad</h2>
      <app-availability-form (query)="onAvailabilityQuery($event)" />

      @if (service.available().length > 0) {
        <h3>Disponibles en el rango</h3>
        <table class="tbl">
          <thead>
            <tr><th>ID</th><th>Nombre</th><th>Categor\u00eda</th></tr>
          </thead>
          <tbody>
            @for (h of service.available(); track h.id) {
              <tr>
                <td>{{ h.id }}</td>
                <td>{{ h.nombre }}</td>
                <td>{{ categoriaLabel[h.categoria] }}</td>
              </tr>
            }
          </tbody>
        </table>
      } @else if (hasQueried()) {
        <p class="empty">No hay herramientas disponibles en ese rango.</p>
      }
    </section>
  `,
  styles: [`
    .card {
      background: #fff; border-radius: 8px; padding: 20px;
      margin-bottom: 20px; box-shadow: 0 1px 2px rgba(0,0,0,.05);
    }
    .card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; flex-wrap: wrap; gap: 12px; }
    h2 { margin: 0 0 12px; color: #1e293b; font-size: 1.1rem; }
    h3 { margin: 16px 0 8px; color: #334155; font-size: 1rem; }
    .filter { display: flex; align-items: center; gap: 8px; font-size: 0.9rem; color: #475569; }
    .filter select { padding: 4px 8px; border: 1px solid #cbd5e1; border-radius: 4px; }
    .tbl { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
    .tbl th, .tbl td { padding: 8px 10px; border-bottom: 1px solid #e2e8f0; text-align: left; }
    .tbl th { background: #f1f5f9; color: #334155; font-weight: 600; }
    .loading, .empty { color: #64748b; font-size: 0.9rem; margin: 8px 0; }
  `],
})
export class ToolsListComponent implements OnInit {
  readonly service = inject(ToolsService);

  readonly categorias = CATEGORIAS;
  readonly categoriaLabel = CATEGORIA_LABEL;

  readonly error = signal<AppError | null>(null);
  readonly hasQueried = signal(false);

  async ngOnInit(): Promise<void> {
    await this.reloadCatalog();
  }

  async reloadCatalog(): Promise<void> {
    this.error.set(null);
    try {
      await this.service.loadCatalogAsync();
    } catch (e) {
      this.error.set(e as AppError);
    }
  }

  onFilter(evt: Event): void {
    const value = (evt.target as HTMLSelectElement).value;
    this.service.setCategoryFilter((value as Categoria) || null);
  }

  async onAvailabilityQuery(q: AvailabilityQuery): Promise<void> {
    this.error.set(null);
    this.hasQueried.set(true);
    try {
      await this.service.loadAvailable(q.fechaInicio, q.fechaFin, q.categoria);
    } catch (e) {
      this.service.clearAvailable();
      this.error.set(e as AppError);
    }
  }
}
