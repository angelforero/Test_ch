import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ProblemType } from '../../../../core/errors/problem-type';
import { problemDetailInterceptor } from '../../../../core/interceptors/problem-detail.interceptor';
import { EstadoReserva } from '../../models/estado-reserva';
import { Reserva } from '../../models/reserva.model';
import { MyReservationsComponent } from './my-reservations.component';

describe('MyReservationsComponent', () => {
  let fixture: ComponentFixture<MyReservationsComponent>;
  let component: MyReservationsComponent;
  let httpMock: HttpTestingController;

  const reservaConfirmada: Reserva = {
    id: 10, herramientaId: 1, nombreHerramienta: 'Taladro percutor',
    nombre: 'Ana Pérez', correo: 'ana@example.com',
    fechaInicio: '2026-12-01', fechaFin: '2026-12-03',
    estado: EstadoReserva.CONFIRMADA, cancelada: false,
  };

  const reservaFinalizada: Reserva = {
    ...reservaConfirmada,
    id: 11, nombreHerramienta: 'Sierra circular',
    fechaInicio: '2025-01-01', fechaFin: '2025-01-05',
    estado: EstadoReserva.FINALIZADA,
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyReservationsComponent],
      providers: [
        provideHttpClient(withInterceptors([problemDetailInterceptor])),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(MyReservationsComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('no busca si el correo es inválido', async () => {
    component.form.setValue({ correo: 'no-es-email' });
    await component.buscar();
    httpMock.expectNone(r => r.url === '/api/reservations');
    expect(component.queried()).toBeFalse();
  });

  it('buscar() con correo válido llama GET /api/reservations y pinta filas', async () => {
    component.form.setValue({ correo: 'ana@example.com' });
    const p = component.buscar();
    const req = httpMock.expectOne(r => r.url === '/api/reservations');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('correo')).toBe('ana@example.com');
    req.flush([reservaConfirmada, reservaFinalizada]);
    await p;
    fixture.detectChanges();

    expect(component.reservas().length).toBe(2);
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Taladro percutor');
  });

  it('muestra mensaje cuando la lista está vacía', async () => {
    component.form.setValue({ correo: 'nadie@example.com' });
    const p = component.buscar();
    httpMock.expectOne(r => r.url === '/api/reservations').flush([]);
    await p;
    fixture.detectChanges();

    expect(component.queried()).toBeTrue();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('No se encontraron reservas');
  });

  it('botón Cancelar solo aparece en reservas CONFIRMADA', async () => {
    component.form.setValue({ correo: 'ana@example.com' });
    const p = component.buscar();
    httpMock.expectOne(r => r.url === '/api/reservations')
      .flush([reservaConfirmada, reservaFinalizada]);
    await p;
    fixture.detectChanges();

    const buttons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('tbody button.btn-cancel');
    expect(buttons.length).toBe(1);
  });

  it('cancelar() tras confirmación actualiza la fila con estado CANCELADA', async () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.form.setValue({ correo: 'ana@example.com' });
    const p1 = component.buscar();
    httpMock.expectOne(r => r.url === '/api/reservations').flush([reservaConfirmada]);
    await p1;

    const p2 = component.cancelar(reservaConfirmada);
    const req = httpMock.expectOne('/api/reservations/10');
    expect(req.request.method).toBe('DELETE');
    req.flush({ ...reservaConfirmada, estado: EstadoReserva.CANCELADA, cancelada: true });
    await p2;
    fixture.detectChanges();

    expect(component.reservas()[0].estado).toBe(EstadoReserva.CANCELADA);
    expect(component.error()).toBeNull();
  });

  it('cancelar() respeta la cancelación del usuario (confirm=false) y no llama al backend', async () => {
    const confirmSpy = spyOn(window, 'confirm').and.returnValue(false);
    component.form.setValue({ correo: 'ana@example.com' });
    const p = component.buscar();
    httpMock.expectOne(r => r.url === '/api/reservations').flush([reservaConfirmada]);
    await p;

    await component.cancelar(reservaConfirmada);
    expect(confirmSpy).toHaveBeenCalled();
    expect(component.reservas()[0].estado).toBe(EstadoReserva.CONFIRMADA);
    httpMock.expectNone(r => r.method === 'DELETE');
  });

  it('cancelar() con 409 cancellation-not-allowed muestra banner con mensaje específico', async () => {
    spyOn(window, 'confirm').and.returnValue(true);
    component.form.setValue({ correo: 'ana@example.com' });
    const p1 = component.buscar();
    httpMock.expectOne(r => r.url === '/api/reservations').flush([reservaConfirmada]);
    await p1;

    const p2 = component.cancelar(reservaConfirmada);
    httpMock.expectOne('/api/reservations/10').flush(
      {
        type: 'http://localhost:8080/problems/cancellation-not-allowed',
        title: 'Cancelación no permitida',
        detail: 'La reserva no puede cancelarse.',
        status: 409,
        reservaId: 10,
        motivo: 'YA_INICIADA',
      },
      { status: 409, statusText: 'Conflict' },
    );
    await p2;
    fixture.detectChanges();

    const err = component.error();
    expect(err?.type).toBe(ProblemType.CancellationNotAllowed);
    expect(err?.detail).toContain('ya está en curso');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ya está en curso');
  });
});
