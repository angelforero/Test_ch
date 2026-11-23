import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProblemType } from '../../../../core/errors/problem-type';
import { problemDetailInterceptor } from '../../../../core/interceptors/problem-detail.interceptor';
import { Categoria } from '../../../tools/models/categoria';
import { EstadoReserva } from '../../models/estado-reserva';
import { ReservationFormComponent } from './reservation-form.component';

describe('ReservationFormComponent', () => {
  let fixture: ComponentFixture<ReservationFormComponent>;
  let component: ReservationFormComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ReservationFormComponent],
      providers: [provideHttpClient(withInterceptors([problemDetailInterceptor])), provideHttpClientTesting()],
    }).compileComponents();
    fixture = TestBed.createComponent(ReservationFormComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
    httpMock.expectOne('/api/tools').flush([
      { id: 1, nombre: 'Taladro percutor', categoria: Categoria.PERFORACION },
    ]);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('no envía si el formulario está vacío', async () => {
    await component.submit();
    expect(component.form.invalid).toBeTrue();
    expect(component.created()).toBeNull();
  });

  it('crea la reserva y muestra el resultado', async () => {
    const start = futureIso(1);
    const end = futureIso(3);
    component.form.setValue({
      herramientaId: 1,
      nombre: 'Ana Pérez',
      correo: 'ana@example.com',
      fechaInicio: start,
      fechaFin: end,
    });

    const submitPromise = component.submit();
    const req = httpMock.expectOne('/api/reservations');
    req.flush({
      id: 12,
      herramientaId: 1,
      nombreHerramienta: 'Taladro percutor',
      nombre: 'Ana Pérez',
      correo: 'ana@example.com',
      fechaInicio: start,
      fechaFin: end,
      estado: EstadoReserva.CONFIRMADA,
      cancelada: false,
    });
    await submitPromise;
    fixture.detectChanges();

    expect(component.created()?.id).toBe(12);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Reserva creada correctamente');
  });

  it('muestra el ProblemDetail de solapamiento', async () => {
    component.form.setValue({
      herramientaId: 1,
      nombre: 'Ana Pérez',
      correo: 'ana@example.com',
      fechaInicio: futureIso(1),
      fechaFin: futureIso(3),
    });

    const submitPromise = component.submit();
    httpMock.expectOne('/api/reservations').flush(
      {
        type: 'http://localhost:8080/problems/overlap-conflict',
        title: 'Reserva solapada',
        status: 409,
        detail: 'La herramienta ya está reservada en ese rango.',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await submitPromise;
    fixture.detectChanges();

    expect(component.error()?.type).toBe(ProblemType.OverlapConflict);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Reserva solapada');
  });
});

function futureIso(days: number): string {
  const date = new Date();
  date.setDate(date.getDate() + days);
  return date.toISOString().slice(0, 10);
}
