import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AppError } from '../../../../core/errors/app-error';
import { ProblemType } from '../../../../core/errors/problem-type';
import { ErrorBannerComponent } from '../../../../shared/components/error-banner/error-banner.component';
import { EstadoReserva, ESTADO_RESERVA_LABEL } from '../../models/estado-reserva';
import { Reserva } from '../../models/reserva.model';
import { ReservationsService } from '../../services/reservations.service';

@Component({
  selector: 'app-my-reservations',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ErrorBannerComponent],
  template: `
    <section class="card">
      <h2>Mis reservas</h2>
      <p class="intro">Introduce tu correo para consultar tus reservas y cancelar las que estén confirmadas.</p>

      <form [formGroup]="form" (ngSubmit)="buscar()" class="search">
        <label>
          <span>Correo</span>
          <input type="email" formControlName="correo" maxlength="254" placeholder="tu@correo.com" />
        </label>
        <button type="submit" [disabled]="form.invalid || loading()">
          {{ loading() ? 'Buscando…' : 'Buscar' }}
        </button>
      </form>

      <app-error-banner [error]="error()" />

      @if (queried() && !loading()) {
        @if (reservas().length === 0) {
          <p class="empty">No se encontraron reservas para este correo.</p>
        } @else {
          <table class="tbl">
            <thead>
              <tr>
                <th>#</th>
                <th>Herramienta</th>
                <th>Desde</th>
                <th>Hasta</th>
                <th>Estado</th>
                <th>Acciones</th>
              </tr>
            </thead>
            <tbody>
              @for (r of reservas(); track r.id) {
                <tr [class.cancelled]="r.estado === estadoReserva.CANCELADA">
                  <td>#{{ r.id }}</td>
                  <td>{{ r.nombreHerramienta }}</td>
                  <td>{{ r.fechaInicio }}</td>
                  <td>{{ r.fechaFin }}</td>
                  <td>
                    <span class="badge" [attr.data-estado]="r.estado">
                      {{ estadoLabel[r.estado] }}
                    </span>
                  </td>
                  <td>
                    @if (r.estado === estadoReserva.CONFIRMADA) {
                      <button type="button"
                              class="btn-cancel"
                              [disabled]="cancelingId() === r.id"
                              (click)="cancelar(r)">
                        {{ cancelingId() === r.id ? 'Cancelando…' : 'Cancelar' }}
                      </button>
                    } @else {
                      <span class="noop">—</span>
                    }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      }
    </section>
  `,
  styles: [`
    .card { background: #fff; border-radius: 8px; padding: 22px;
      box-shadow: 0 1px 2px rgba(0,0,0,.05); }
    h2 { margin: 0 0 6px; color: #1e293b; }
    .intro { margin: 0 0 18px; color: #64748b; }
    .search { display: flex; gap: 12px; align-items: flex-end; flex-wrap: wrap;
      margin-bottom: 16px; }
    .search label { display: grid; gap: 5px; flex: 1 1 260px;
      color: #334155; font-size: .9rem; }
    .search input { padding: 9px 10px; border: 1px solid #cbd5e1;
      border-radius: 4px; font: inherit; }
    .search button { padding: 9px 18px; border: 0; border-radius: 4px;
      background: #2563eb; color: #fff; cursor: pointer; }
    .search button[disabled] { background: #94a3b8; cursor: not-allowed; }
    .empty { color: #64748b; font-size: .9rem; margin: 8px 0; }
    .tbl { width: 100%; border-collapse: collapse; font-size: .9rem; }
    .tbl th, .tbl td { padding: 10px; border-bottom: 1px solid #e2e8f0;
      text-align: left; }
    .tbl th { background: #f1f5f9; color: #334155; font-weight: 600; }
    .cancelled { color: #94a3b8; text-decoration: line-through; }
    .cancelled .badge { text-decoration: none; }
    .badge { display: inline-block; padding: 3px 10px; border-radius: 999px;
      font-size: .78rem; font-weight: 600; background: #e2e8f0; color: #334155; }
    .badge[data-estado="CONFIRMADA"] { background: #dbeafe; color: #1e40af; }
    .badge[data-estado="EN_CURSO"]   { background: #fef3c7; color: #92400e; }
    .badge[data-estado="FINALIZADA"] { background: #e5e7eb; color: #4b5563; }
    .badge[data-estado="CANCELADA"]  { background: #fee2e2; color: #991b1b; }
    .btn-cancel { padding: 6px 12px; border: 0; border-radius: 4px;
      background: #dc2626; color: #fff; cursor: pointer; font-size: .85rem; }
    .btn-cancel[disabled] { background: #fca5a5; cursor: not-allowed; }
    .noop { color: #cbd5e1; }
  `],
})
export class MyReservationsComponent {
  private readonly fb = inject(FormBuilder);
  private readonly reservationsService = inject(ReservationsService);

  readonly estadoLabel = ESTADO_RESERVA_LABEL;
  readonly estadoReserva = EstadoReserva;

  readonly form = this.fb.group({
    correo: ['', [Validators.required, Validators.email]],
  });

  readonly reservas = signal<Reserva[]>([]);
  readonly loading = signal(false);
  readonly cancelingId = signal<number | null>(null);
  readonly error = signal<AppError | null>(null);
  readonly queried = signal(false);

  async buscar(): Promise<void> {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const correo = this.form.value.correo!.trim();

    this.loading.set(true);
    this.error.set(null);
    try {
      const lista = await this.reservationsService.listarPorCorreo(correo);
      this.reservas.set(lista);
      this.queried.set(true);
    } catch (e) {
      this.reservas.set([]);
      this.queried.set(true);
      this.error.set(e as AppError);
    } finally {
      this.loading.set(false);
    }
  }

  async cancelar(reserva: Reserva): Promise<void> {
    const mensaje = `¿Cancelar la reserva #${reserva.id} de "${reserva.nombreHerramienta}"?`;
    if (!window.confirm(mensaje)) return;

    this.cancelingId.set(reserva.id);
    this.error.set(null);
    try {
      const actualizada = await this.reservationsService.cancelar(reserva.id);
      this.reservas.update(list =>
        list.map(r => (r.id === actualizada.id ? actualizada : r)),
      );
    } catch (e) {
      this.error.set(this.friendlyCancellationError(e as AppError));
    } finally {
      this.cancelingId.set(null);
    }
  }

  private friendlyCancellationError(err: AppError): AppError {
    if (err.type !== ProblemType.CancellationNotAllowed) return err;
    const motivo = err.extras['motivo'];
    const explicacion =
      motivo === 'YA_INICIADA'   ? 'La reserva ya está en curso y no se puede cancelar.' :
      motivo === 'YA_FINALIZADA' ? 'La reserva ya terminó.' :
      motivo === 'YA_CANCELADA'  ? 'La reserva ya estaba cancelada.' :
      '';
    return new AppError(
      err.type, err.title,
      explicacion ? `${err.detail} ${explicacion}` : err.detail,
      err.status, err.extras,
    );
  }
}
