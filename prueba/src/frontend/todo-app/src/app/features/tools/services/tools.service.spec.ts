import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { Categoria } from '../models/categoria';
import { Herramienta } from '../models/herramienta.model';
import { ToolsService } from './tools.service';

/**
 * Tests unitarios de {@link ToolsService}. Alineado con D19:
 *  - Mocks obligatorios (HttpTestingController \u2014 no backend real).
 *  - Signals ejercitados por lectura directa tras cada operaci\u00f3n.
 *  - Cobertura m\u00e1xima 30% (doc 04): tests representativos, no
 *    exhaustivos de cada rama trivial.
 */
describe('ToolsService', () => {
  let service: ToolsService;
  let httpMock: HttpTestingController;

  const herramientas: Herramienta[] = [
    { id: 1, nombre: 'Taladro percutor', categoria: Categoria.PERFORACION },
    { id: 2, nombre: 'Sierra circular',  categoria: Categoria.CORTE },
  ];

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(ToolsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('loadCatalogAsync llama GET /api/tools y publica en catalog()', async () => {
    const p = service.loadCatalogAsync();

    const req = httpMock.expectOne('/api/tools');
    expect(req.request.method).toBe('GET');
    req.flush(herramientas);

    await p;
    expect(service.catalog().length).toBe(2);
    expect(service.catalog()[0].nombre).toBe('Taladro percutor');
    expect(service.loading()).toBeFalse();
  });

  it('filteredCatalog() reacciona al setCategoryFilter()', async () => {
    const p = service.loadCatalogAsync();
    httpMock.expectOne('/api/tools').flush(herramientas);
    await p;

    expect(service.filteredCatalog().length).toBe(2);
    service.setCategoryFilter(Categoria.CORTE);
    expect(service.filteredCatalog().length).toBe(1);
    expect(service.filteredCatalog()[0].categoria).toBe(Categoria.CORTE);
  });

  it('loadAvailable env\u00eda fechaInicio, fechaFin y categoria como query params', async () => {
    const p = service.loadAvailable('2026-12-01', '2026-12-05', Categoria.PERFORACION);

    const req = httpMock.expectOne(r => r.url === '/api/tools/available');
    expect(req.request.params.get('fechaInicio')).toBe('2026-12-01');
    expect(req.request.params.get('fechaFin')).toBe('2026-12-05');
    expect(req.request.params.get('categoria')).toBe('PERFORACION');
    req.flush([herramientas[0]]);

    await p;
    expect(service.available().length).toBe(1);
  });

  it('loadAvailable omite categoria si viene null', async () => {
    const p = service.loadAvailable('2026-12-01', '2026-12-05', null);

    const req = httpMock.expectOne(r => r.url === '/api/tools/available');
    expect(req.request.params.has('categoria')).toBeFalse();
    req.flush([]);

    await p;
    expect(service.available().length).toBe(0);
  });
});
