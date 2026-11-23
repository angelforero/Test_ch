import { Component } from '@angular/core';

/**
 * Placeholder para "mis reservas". Implementación real en commit 12
 * (R05): input de correo + GET + tabla con estado derivado +
 * botón Cancelar (DELETE).
 */
@Component({
  selector: 'app-my-reservations',
  standalone: true,
  template: `
    <section class="placeholder">
      <h2>Mis reservas</h2>
      <p>Pendiente — llega en el commit 12 (feat(frontend): listado y cancelación de reservas).</p>
    </section>
  `,
  styles: [`
    .placeholder { padding: 24px; background: #fff; border-radius: 8px; }
    .placeholder h2 { margin-top: 0; color: #1e293b; }
    .placeholder p  { color: #64748b; }
  `],
})
export class MyReservationsComponent {}
