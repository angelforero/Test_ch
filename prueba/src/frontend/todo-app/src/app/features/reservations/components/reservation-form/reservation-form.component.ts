import { Component } from '@angular/core';

/**
 * Placeholder para el formulario de nueva reserva. Implementación real
 * en commit 11 (R03, R04): Reactive Forms con validadores alineados
 * al backend + manejo de los ProblemDetail específicos
 * (overlap-conflict, active-reservation-limit).
 */
@Component({
  selector: 'app-reservation-form',
  standalone: true,
  template: `
    <section class="placeholder">
      <h2>Nueva reserva</h2>
      <p>Pendiente — llega en el commit 11 (feat(frontend): creación de reservas).</p>
    </section>
  `,
  styles: [`
    .placeholder { padding: 24px; background: #fff; border-radius: 8px; }
    .placeholder h2 { margin-top: 0; color: #1e293b; }
    .placeholder p  { color: #64748b; }
  `],
})
export class ReservationFormComponent {}
