import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { AppError } from '../errors/app-error';
import { ProblemType } from '../errors/problem-type';
import { problemDetailInterceptor, toAppError } from './problem-detail.interceptor';
import { HttpErrorResponse } from '@angular/common/http';

/**
 * Tests del interceptor funcional. Ejercitan los 3 caminos de error:
 *  1. ProblemDetail del backend → AppError tipado con extras.
 *  2. Error HTTP sin ProblemDetail → AppError genérico.
 *  3. Error de red (status 0) → AppError.Unknown.
 *
 * Alineado con D19: tests unitarios con mocks (HttpTestingController),
 * sin golpear backend real.
 */
describe('problemDetailInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([problemDetailInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  // --- Camino 1: ProblemDetail -----------------------------------------

  it('mapea un 409 overlap-conflict a AppError con extras preservados', (done) => {
    http.get('/api/reservations').subscribe({
      next: () => done.fail('debería haber fallado'),
      error: (err: unknown) => {
        expect(err).toBeInstanceOf(AppError);
        const ae = err as AppError;
        expect(ae.type).toBe(ProblemType.OverlapConflict);
        expect(ae.title).toBe('Reserva solapada');
        expect(ae.status).toBe(409);
        expect(ae.extras['herramientaId']).toBe(10);
        expect(ae.extras['fechaInicio']).toBe('2026-12-01');
        done();
      },
    });

    const req = httpMock.expectOne('/api/reservations');
    req.flush(
      {
        type: 'http://localhost:8080/problems/overlap-conflict',
        title: 'Reserva solapada',
        detail: 'Ya existe una reserva activa para esta herramienta que se solapa.',
        status: 409,
        herramientaId: 10,
        fechaInicio: '2026-12-01',
        fechaFin: '2026-12-05',
      },
      { status: 409, statusText: 'Conflict' },
    );
  });

  it('mapea un 422 validation-error exponiendo fieldErrors', (done) => {
    http.post('/api/reservations', {}).subscribe({
      error: (err: unknown) => {
        const ae = err as AppError;
        expect(ae.type).toBe(ProblemType.ValidationError);
        expect(ae.fieldErrors.length).toBe(1);
        expect(ae.fieldErrors[0].campo).toBe('correo');
        expect(ae.fieldErrors[0].mensaje).toContain('email');
        done();
      },
    });

    httpMock.expectOne('/api/reservations').flush(
      {
        type: 'http://localhost:8080/problems/validation-error',
        title: 'Error de validación',
        detail: 'Hay 1 error(es) de validación en los campos enviados.',
        status: 422,
        errors: [{ campo: 'correo', mensaje: 'must be a well-formed email address', valorRechazado: 'xx' }],
      },
      { status: 422, statusText: 'Unprocessable Entity' },
    );
  });

  // --- Camino 2: HTTP error sin ProblemDetail --------------------------

  it('mapea un 500 sin body JSON a InternalError', (done) => {
    http.get('/api/tools').subscribe({
      error: (err: unknown) => {
        const ae = err as AppError;
        expect(ae.type).toBe(ProblemType.InternalError);
        expect(ae.status).toBe(500);
        expect(ae.detail).toBeTruthy();
        done();
      },
    });

    httpMock.expectOne('/api/tools').flush('<html>oops</html>', {
      status: 500,
      statusText: 'Internal Server Error',
    });
  });

  // --- Camino 3: error de red ------------------------------------------

  it('mapea un error de red (status 0) a Unknown con mensaje "Sin conexión"', (done) => {
    http.get('/api/tools').subscribe({
      error: (err: unknown) => {
        const ae = err as AppError;
        expect(ae.type).toBe(ProblemType.Unknown);
        expect(ae.status).toBe(0);
        expect(ae.title).toBe('Sin conexión');
        done();
      },
    });

    httpMock.expectOne('/api/tools').error(new ProgressEvent('error'), {
      status: 0,
      statusText: 'Unknown Error',
    });
  });

  // --- Caso feliz pasa a trav\u00e9s -----------------------------------------

  it('respuestas 200 pasan sin tocar nada', (done) => {
    http.get<{ hello: string }>('/api/anything').subscribe({
      next: (body) => {
        expect(body.hello).toBe('world');
        done();
      },
      error: () => done.fail('no debería fallar'),
    });

    httpMock.expectOne('/api/anything').flush({ hello: 'world' });
  });
});

/**
 * Tests de la utilidad toAppError expuesta aparte. Son triviales pero
 * cubren los caminos sin pasar por TestBed/HttpClient.
 */
describe('toAppError (utilidad pura)', () => {
  it('con URL de type desconocida devuelve Unknown pero mantiene title/detail', () => {
    const err = new HttpErrorResponse({
      error: { type: 'https://otra.cosa/foo', title: 'X', detail: 'Y', status: 418 },
      status: 418,
      statusText: "I'm a teapot",
    });
    const ae = toAppError(err);
    expect(ae.type).toBe(ProblemType.Unknown);
    expect(ae.title).toBe('X');
    expect(ae.detail).toBe('Y');
  });

  it('un throw arbitrario (string) se envuelve en AppError.Unknown', () => {
    const ae = toAppError('boom');
    expect(ae).toBeInstanceOf(AppError);
    expect(ae.type).toBe(ProblemType.Unknown);
    expect(ae.detail).toBe('boom');
  });
});
