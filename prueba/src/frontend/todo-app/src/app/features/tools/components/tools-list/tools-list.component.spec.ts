import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Categoria } from '../../models/categoria';
import { Herramienta } from '../../models/herramienta.model';
import { ToolsListComponent } from './tools-list.component';

/**
 * Tests de {@link ToolsListComponent}. Usan HttpTestingController en vez
 * de mockear el service para ejercitar tambi\u00e9n la integraci\u00f3n
 * componente \u2194 service (coste mismo, m\u00e1s cobertura \u00fatil).
 *
 * Alineado con D19 (mocks obligatorios para HTTP) y cobertura m\u00e1x 30%
 * (doc 04): un test de carga inicial basta para la l\u00f3gica m\u00e1s cr\u00edtica.
 */
describe('ToolsListComponent', () => {
  let fixture: ComponentFixture<ToolsListComponent>;
  let httpMock: HttpTestingController;

  const data: Herramienta[] = [
    { id: 1, nombre: 'Taladro', categoria: Categoria.PERFORACION },
    { id: 2, nombre: 'Sierra',  categoria: Categoria.CORTE },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ToolsListComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ToolsListComponent);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('carga el cat\u00e1logo al iniciar y pinta las filas', async () => {
    fixture.detectChanges(); // dispara ngOnInit \u2192 loadCatalogAsync()

    const req = httpMock.expectOne('/api/tools');
    req.flush(data);

    await fixture.whenStable();
    fixture.detectChanges();

    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Taladro');
  });
});
