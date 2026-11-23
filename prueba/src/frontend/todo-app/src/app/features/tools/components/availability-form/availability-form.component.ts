import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import {
  maxDurationValidator,
  noPastValidator,
  orderedRangeValidator,
} from '../../../../core/validators/date-range.validators';
import { CATEGORIAS, CATEGORIA_LABEL, Categoria } from '../../models/categoria';

/** Payload emitido por el formulario cuando el usuario pulsa "Consultar". */
export interface AvailabilityQuery {
  fechaInicio: string;            // YYYY-MM-DD
  fechaFin: string;               // YYYY-MM-DD
  categoria: Categoria | null;    // null \u2192 no enviar query
}

/**
 * Formulario reactivo para la consulta de disponibilidad (R02).
 *
 * Valida cliente-side:
 *   - fechaInicio y fechaFin obligatorios ({@code Validators.required});
 *   - fechaInicio \u2264 fechaFin ({@link orderedRangeValidator});
 *   - fechaInicio \u2265 hoy ({@link noPastValidator});
 *   - duraci\u00f3n m\u00e1xima de 7 d\u00edas ({@link maxDurationValidator}, D11 del backend).
 *
 * El backend valida lo mismo adem\u00e1s de lo suyo (R02 completo). La
 * validaci\u00f3n cliente es solo UX \u2014 el servidor sigue siendo fuente
 * de verdad (defensa en profundidad).
 *
 * Emite {@code (query)} solo cuando el form es v\u00e1lido. El componente
 * padre decide qu\u00e9 hacer con la query (p.ej. llamar al service).
 */
@Component({
  selector: 'app-availability-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="submit()" class="avail-form">
      <div class="row">
        <label>
          <span>Desde</span>
          <input type="date" formControlName="fechaInicio" [min]="today" />
        </label>
        <label>
          <span>Hasta</span>
          <input type="date" formControlName="fechaFin" [min]="today" />
        </label>
        <label>
          <span>Categor\u00eda</span>
          <select formControlName="categoria">
            <option [ngValue]="null">Todas</option>
            @for (c of categorias; track c) {
              <option [ngValue]="c">{{ categoriaLabel[c] }}</option>
            }
          </select>
        </label>
        <button type="submit" [disabled]="form.invalid">Consultar</button>
      </div>

      @if (form.errors?.['range']) {
        <p class="err">La fecha de inicio debe ser anterior o igual a la de fin.</p>
      }
      @if (form.errors?.['past']) {
        <p class="err">La fecha de inicio no puede estar en el pasado.</p>
      }
      @if (form.errors?.['tooLong']) {
        <p class="err">La duraci\u00f3n m\u00e1xima es de 7 d\u00edas.</p>
      }
    </form>
  `,
  styles: [`
    .avail-form { padding: 12px; background: #f8fafc; border-radius: 6px; margin-bottom: 16px; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; align-items: flex-end; }
    label { display: flex; flex-direction: column; font-size: 0.85rem; color: #334155; }
    label span { margin-bottom: 4px; }
    input, select { padding: 6px 8px; border: 1px solid #cbd5e1; border-radius: 4px; font-size: 0.9rem; }
    button {
      padding: 7px 16px; background: #2563eb; color: #fff;
      border: none; border-radius: 4px; cursor: pointer; font-size: 0.9rem;
    }
    button[disabled] { background: #94a3b8; cursor: not-allowed; }
    .err { color: #b91c1c; font-size: 0.85rem; margin: 8px 0 0; }
  `],
})
export class AvailabilityFormComponent {
  private readonly fb = inject(FormBuilder);

  readonly categorias = CATEGORIAS;
  readonly categoriaLabel = CATEGORIA_LABEL;
  readonly today = new Date().toISOString().slice(0, 10); // YYYY-MM-DD

  readonly form = this.fb.group(
    {
      fechaInicio: ['', Validators.required],
      fechaFin:    ['', Validators.required],
      categoria:   [null as Categoria | null],
    },
    { validators: [orderedRangeValidator, noPastValidator, maxDurationValidator] },
  );

  @Output() readonly query = new EventEmitter<AvailabilityQuery>();

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    this.query.emit({
      fechaInicio: v.fechaInicio!,
      fechaFin:    v.fechaFin!,
      categoria:   v.categoria,
    });
  }
}
