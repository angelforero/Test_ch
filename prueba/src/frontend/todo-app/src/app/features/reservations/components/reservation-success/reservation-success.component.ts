import { Component, EventEmitter, Input, Output } from '@angular/core';

import { ESTADO_RESERVA_LABEL } from '../../models/estado-reserva';
import { Reserva } from '../../models/reserva.model';

@Component({
  selector: 'app-reservation-success',
  standalone: true,
  template: `
    @if (reserva) {
      <section class="success" role="status">
        <h3>Reserva creada correctamente</h3>
        <dl>
          <div><dt>Número</dt><dd>#{{ reserva.id }}</dd></div>
          <div><dt>Herramienta</dt><dd>{{ reserva.nombreHerramienta }}</dd></div>
          <div><dt>Fechas</dt><dd>{{ reserva.fechaInicio }} — {{ reserva.fechaFin }}</dd></div>
          <div><dt>Estado</dt><dd>{{ estadoLabel[reserva.estado] }}</dd></div>
        </dl>
        <button type="button" (click)="newReservation.emit()">Hacer otra reserva</button>
      </section>
    }
  `,
  styles: [`
    .success { background: #f0fdf4; border: 1px solid #86efac; color: #166534; padding: 18px; border-radius: 8px; }
    h3 { margin: 0 0 12px; }
    dl { display: grid; gap: 8px; margin: 0 0 14px; }
    dl div { display: grid; grid-template-columns: 110px 1fr; }
    dt { font-weight: 600; }
    dd { margin: 0; }
    button { padding: 8px 14px; border: 0; border-radius: 4px; background: #15803d; color: #fff; cursor: pointer; }
  `],
})
export class ReservationSuccessComponent {
  @Input({ required: true }) reserva: Reserva | null = null;
  @Output() readonly newReservation = new EventEmitter<void>();
  readonly estadoLabel = ESTADO_RESERVA_LABEL;
}
