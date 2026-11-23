import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { AppError } from '../../../../core/errors/app-error';
import { ProblemType } from '../../../../core/errors/problem-type';
import {
  maxDurationValidator,
  noPastValidator,
  orderedRangeValidator,
} from '../../../../core/validators/date-range.validators';
import { ErrorBannerComponent } from '../../../../shared/components/error-banner/error-banner.component';
import { CATEGORIA_LABEL } from '../../../tools/models/categoria';
import { ToolsService } from '../../../tools/services/tools.service';
import { ReservaCreacion } from '../../models/reserva-creacion.model';
import { Reserva } from '../../models/reserva.model';
import { ReservationsService } from '../../services/reservations.service';
import { ReservationSuccessComponent } from '../reservation-success/reservation-success.component';

@Component({
  selector: 'app-reservation-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, ErrorBannerComponent, ReservationSuccessComponent],
  template: `
    <section class="card">
      <h2>Nueva reserva</h2>
      <p class="intro">Selecciona una herramienta y reserva un período máximo de 7 días.</p>

      <app-error-banner [error]="error()" />

      @if (created()) {
        <app-reservation-success
          [reserva]="created()"
          (newReservation)="startAnother()"
        />
      } @else {
        <form [formGroup]="form" (ngSubmit)="submit()">
          <label>
            <span>Herramienta</span>
            <select formControlName="herramientaId">
              <option [ngValue]="null">Selecciona una herramienta</option>
              @for (tool of tools.catalog(); track tool.id) {
                <option [ngValue]="tool.id">{{ tool.nombre }} ({{ categoryLabel[tool.categoria] }})</option>
              }
            </select>
            @if (showError('herramientaId')) { <small>Selecciona una herramienta.</small> }
          </label>

          <div class="grid">
            <label>
              <span>Nombre</span>
              <input type="text" formControlName="nombre" maxlength="120" />
              @if (showError('nombre')) { <small>{{ fieldMessage('nombre', 'Indica tu nombre.') }}</small> }
            </label>
            <label>
              <span>Correo</span>
              <input type="email" formControlName="correo" maxlength="254" />
              @if (showError('correo')) { <small>{{ fieldMessage('correo', 'Introduce un correo válido.') }}</small> }
            </label>
            <label>
              <span>Desde</span>
              <input type="date" formControlName="fechaInicio" [min]="today" />
              @if (showError('fechaInicio')) { <small>{{ fieldMessage('fechaInicio', 'Indica la fecha de inicio.') }}</small> }
            </label>
            <label>
              <span>Hasta</span>
              <input type="date" formControlName="fechaFin" [min]="today" />
              @if (showError('fechaFin')) { <small>{{ fieldMessage('fechaFin', 'Indica la fecha de fin.') }}</small> }
            </label>
          </div>

          @if (form.errors?.['range']) { <p class="form-error">La fecha de inicio debe ser anterior o igual a la de fin.</p> }
          @if (form.errors?.['past']) { <p class="form-error">La fecha de inicio no puede estar en el pasado.</p> }
          @if (form.errors?.['tooLong']) { <p class="form-error">La duración máxima es de 7 días.</p> }

          <button type="submit" [disabled]="submitting() || tools.loading()">
            {{ submitting() ? 'Creando reserva…' : 'Crear reserva' }}
          </button>
        </form>
      }
    </section>
  `,
  styles: [`
    .card { background: #fff; border-radius: 8px; padding: 22px; box-shadow: 0 1px 2px rgba(0,0,0,.05); }
    h2 { margin: 0 0 6px; color: #1e293b; }
    .intro { margin: 0 0 18px; color: #64748b; }
    form { display: grid; gap: 16px; }
    .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
    label { display: grid; gap: 5px; color: #334155; font-size: .9rem; }
    input, select { padding: 9px 10px; border: 1px solid #cbd5e1; border-radius: 4px; font: inherit; }
    small, .form-error { color: #b91c1c; margin: 0; font-size: .82rem; }
    button { justify-self: start; padding: 9px 18px; border: 0; border-radius: 4px; background: #2563eb; color: #fff; cursor: pointer; }
    button[disabled] { background: #94a3b8; cursor: not-allowed; }
    @media (max-width: 640px) { .grid { grid-template-columns: 1fr; } }
  `],
})
export class ReservationFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly reservations = inject(ReservationsService);
  readonly tools = inject(ToolsService);

  readonly categoryLabel = CATEGORIA_LABEL;
  readonly today = new Date().toISOString().slice(0, 10);
  readonly submitting = signal(false);
  readonly error = signal<AppError | null>(null);
  readonly created = signal<Reserva | null>(null);
  private submitted = false;

  readonly form = this.fb.group(
    {
      herramientaId: [null as number | null, Validators.required],
      nombre: ['', [Validators.required, Validators.maxLength(120)]],
      correo: ['', [Validators.required, Validators.email, Validators.maxLength(254)]],
      fechaInicio: ['', Validators.required],
      fechaFin: ['', Validators.required],
    },
    { validators: [orderedRangeValidator, noPastValidator, maxDurationValidator] },
  );

  async ngOnInit(): Promise<void> {
    if (this.tools.catalog().length === 0) {
      try {
        await this.tools.loadCatalogAsync();
      } catch (error) {
        this.error.set(error as AppError);
      }
    }
  }

  async submit(): Promise<void> {
    this.submitted = true;
    this.error.set(null);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    try {
      const value = this.form.getRawValue();
      const payload: ReservaCreacion = {
        herramientaId: value.herramientaId!,
        nombre: value.nombre!.trim(),
        correo: value.correo!.trim(),
        fechaInicio: value.fechaInicio!,
        fechaFin: value.fechaFin!,
      };
      this.created.set(await this.reservations.crear(payload));
    } catch (error) {
      this.error.set(this.friendlyError(error as AppError));
    } finally {
      this.submitting.set(false);
    }
  }

  startAnother(): void {
    const { nombre, correo } = this.form.getRawValue();
    this.form.reset({ herramientaId: null, nombre, correo, fechaInicio: '', fechaFin: '' });
    this.created.set(null);
    this.error.set(null);
    this.submitted = false;
  }

  showError(field: string): boolean {
    const control = this.form.get(field);
    return !!control && control.invalid && (control.touched || this.submitted);
  }

  fieldMessage(field: string, fallback: string): string {
    const currentError = this.error();
    return currentError?.fieldErrors.find(item => item.campo === field)?.mensaje ?? fallback;
  }

  private friendlyError(error: AppError): AppError {
    if (error.type !== ProblemType.ActiveReservationLimit) return error;
    return new AppError(
      error.type,
      error.title,
      `${error.detail} Revisa Mis reservas para cancelar una reserva confirmada.`,
      error.status,
      error.extras,
    );
  }
}
