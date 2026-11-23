import { Component } from '@angular/core';

/**
 * Placeholder para la vista de catálogo de herramientas. La
 * implementación real llega en el commit 10 (R01): tabla con
 * filtro de categoría + consulta de disponibilidad (R02).
 *
 * Existe en este commit solo para que app.routes.ts cargue sin
 * errores y el shell navegable funcione end-to-end.
 */
@Component({
  selector: 'app-tools-list',
  standalone: true,
  template: `
    <section class="placeholder">
      <h2>Catálogo de herramientas</h2>
      <p>Pendiente — llega en el commit 10 (feat(frontend): listado + disponibilidad).</p>
    </section>
  `,
  styles: [`
    .placeholder { padding: 24px; background: #fff; border-radius: 8px; }
    .placeholder h2 { margin-top: 0; color: #1e293b; }
    .placeholder p  { color: #64748b; }
  `],
})
export class ToolsListComponent {}
