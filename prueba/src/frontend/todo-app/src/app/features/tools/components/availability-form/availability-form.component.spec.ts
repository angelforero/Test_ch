import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Categoria } from '../../models/categoria';
import {
  AvailabilityFormComponent,
  AvailabilityQuery,
  maxDurationValidator,
  noPastValidator,
  orderedRangeValidator,
} from './availability-form.component';
import { FormBuilder } from '@angular/forms';

/**
 * Tests de {@link AvailabilityFormComponent}. Dos niveles:
 *  - Validators puros (sin TestBed): orden, pasado, duraci\u00f3n m\u00e1xima.
 *  - Componente: emite (query) cuando el form es v\u00e1lido; no emite
 *    cuando faltan datos.
 */
describe('AvailabilityFormComponent', () => {
  let fixture: ComponentFixture<AvailabilityFormComponent>;
  let comp: AvailabilityFormComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AvailabilityFormComponent],
    }).compileComponents();
    fixture = TestBed.createComponent(AvailabilityFormComponent);
    comp = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('submit() no emite cuando faltan fechas', () => {
    let emitted: AvailabilityQuery | undefined;
    comp.query.subscribe(q => (emitted = q));
    comp.submit();
    expect(emitted).toBeUndefined();
  });

  it('submit() emite query con los 3 campos cuando el form es v\u00e1lido', () => {
    // Elegimos fechas futuras seguras para pasar noPastValidator.
    const today = new Date();
    const fi = iso(addDays(today, 1));
    const ff = iso(addDays(today, 3));

    comp.form.patchValue({ fechaInicio: fi, fechaFin: ff, categoria: Categoria.MEDICION });
    expect(comp.form.valid).withContext(JSON.stringify(comp.form.errors)).toBeTrue();

    let emitted: AvailabilityQuery | undefined;
    comp.query.subscribe(q => (emitted = q));
    comp.submit();

    expect(emitted).toEqual({ fechaInicio: fi, fechaFin: ff, categoria: Categoria.MEDICION });
  });
});

describe('Validators puros del formulario de disponibilidad', () => {
  const fb = new FormBuilder();
  const mk = (fi: string, ff: string) =>
    fb.group({ fechaInicio: [fi], fechaFin: [ff], categoria: [null] });

  it('orderedRangeValidator rechaza fechaInicio > fechaFin', () => {
    expect(orderedRangeValidator(mk('2026-12-05', '2026-12-01'))).toEqual({ range: true });
    expect(orderedRangeValidator(mk('2026-12-01', '2026-12-05'))).toBeNull();
  });

  it('noPastValidator rechaza fecha en pasado', () => {
    expect(noPastValidator(mk('2000-01-01', '2000-01-02'))).toEqual({ past: true });
  });

  it('maxDurationValidator rechaza duraci\u00f3n > 7 d\u00edas', () => {
    expect(maxDurationValidator(mk('2026-12-01', '2026-12-10'))).toEqual({ tooLong: true });
    expect(maxDurationValidator(mk('2026-12-01', '2026-12-08'))).toBeNull();
  });
});

function addDays(d: Date, n: number): Date {
  const x = new Date(d);
  x.setDate(x.getDate() + n);
  return x;
}
function iso(d: Date): string {
  return d.toISOString().slice(0, 10);
}
