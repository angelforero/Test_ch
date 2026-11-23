import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { EstadoReserva } from '../models/estado-reserva';
import { ReservaCreacion } from '../models/reserva-creacion.model';
import { Reserva } from '../models/reserva.model';
import { ReservationsService } from './reservations.service';

describe('ReservationsService', () => {
  let service: ReservationsService;
  let httpMock: HttpTestingController;

  const payload: ReservaCreacion = {
    herramientaId: 1,
    nombre: 'Ana Pérez',
    correo: 'ana@example.com',
    fechaInicio: '2026-12-01',
    fechaFin: '2026-12-03',
  };

  const reserva: Reserva = {
    id: 10,
    herramientaId: 1,
    nombreHerramienta: 'Taladro percutor',
    nombre: payload.nombre,
    correo: payload.correo,
    fechaInicio: payload.fechaInicio,
    fechaFin: payload.fechaFin,
    estado: EstadoReserva.CONFIRMADA,
    cancelada: false,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReservationsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('crear envía POST /api/reservations con el payload', async () => {
    const resultPromise = service.crear(payload);
    const req = httpMock.expectOne('/api/reservations');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(reserva);
    expect(await resultPromise).toEqual(reserva);
  });

  it('cancelar envía DELETE /api/reservations/{id}', async () => {
    const resultPromise = service.cancelar(10);
    const req = httpMock.expectOne('/api/reservations/10');
    expect(req.request.method).toBe('DELETE');
    req.flush({ ...reserva, estado: EstadoReserva.CANCELADA, cancelada: true });
    expect((await resultPromise).estado).toBe(EstadoReserva.CANCELADA);
  });

  it('listarPorCorreo envía correo como query param', async () => {
    const resultPromise = service.listarPorCorreo('ana@example.com');
    const req = httpMock.expectOne(r => r.url === '/api/reservations');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('correo')).toBe('ana@example.com');
    req.flush([reserva]);
    expect((await resultPromise).length).toBe(1);
  });
});
