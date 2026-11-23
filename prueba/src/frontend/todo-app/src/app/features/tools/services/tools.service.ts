import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';

import { API_BASE_URL } from '../../../core/api/api.config';
import { Categoria } from '../models/categoria';
import { Herramienta } from '../models/herramienta.model';

/**
 * Servicio de herramientas (R01, R02).
 *
 * Expone estado reactivo basado en signals para que los componentes hagan
 * binding directo sin suscripciones manuales, y métodos imperativos
 * ({@code loadCatalog}, {@code loadAvailable}) que disparan HTTP y
 * actualizan esos signals.
 *
 * Decisiones:
 *   - Dos listas independientes en el servicio: {@code catalog} (todas
 *     las herramientas) y {@code available} (resultado de la consulta
 *     de disponibilidad). El cat\u00e1logo se carga una vez y se filtra
 *     cliente-side; la disponibilidad va al backend porque depende
 *     del rango de fechas y del estado real de reservas.
 *   - El filtro de categor\u00eda en el cat\u00e1logo se resuelve cliente-side
 *     con un {@code computed()} \u2014 evitamos un round-trip HTTP por un
 *     cambio de dropdown.
 *   - Los errores NO se capturan aqu\u00ed: el interceptor ya convierte
 *     cualquier HttpError en {@link AppError}, y el componente decide
 *     mostrarlo. Esto mantiene el servicio puro.
 *   - Signals en vez de BehaviorSubject: Angular 19 moderno, trivial
 *     para test ({@code service.catalog()}) y sin leaks de subscripci\u00f3n.
 */
@Injectable({ providedIn: 'root' })
export class ToolsService {
  private readonly http = inject(HttpClient);

  // --- Estado reactivo -----------------------------------------------------

  private readonly _catalog   = signal<Herramienta[]>([]);
  private readonly _available = signal<Herramienta[]>([]);
  private readonly _loading   = signal(false);
  private readonly _categoryFilter = signal<Categoria | null>(null);

  /** Cat\u00e1logo completo (lectura). */
  readonly catalog = this._catalog.asReadonly();

  /** Resultado de la \u00faltima consulta de disponibilidad. */
  readonly available = this._available.asReadonly();

  /** {@code true} mientras hay una petici\u00f3n en vuelo. */
  readonly loading = this._loading.asReadonly();

  /** Filtro de categor\u00eda aplicado al cat\u00e1logo (null = sin filtro). */
  readonly categoryFilter = this._categoryFilter.asReadonly();

  /**
   * Cat\u00e1logo ya filtrado por categor\u00eda (si hay). Computed \u2192 reactivo
   * autom\u00e1ticamente cuando cambia {@code catalog} o {@code categoryFilter}.
   */
  readonly filteredCatalog = computed<Herramienta[]>(() => {
    const filter = this._categoryFilter();
    const all = this._catalog();
    return filter === null ? all : all.filter(h => h.categoria === filter);
  });

  // --- API p\u00fablica ---------------------------------------------------------

  /** Cambia el filtro cliente-side del cat\u00e1logo. */
  setCategoryFilter(c: Categoria | null): void {
    this._categoryFilter.set(c);
  }

  /**
   * Carga el cat\u00e1logo completo (GET /api/tools). No pasa {@code categoria}
   * en el request \u2014 el filtro es cliente-side. Devuelve el Observable
   * para que el componente pueda suscribirse al error si lo desea.
   */
  loadCatalog(): void {
    this._loading.set(true);
    this.http.get<Herramienta[]>(`${API_BASE_URL}/tools`).subscribe({
      next: (list) => {
        this._catalog.set(list);
        this._loading.set(false);
      },
      error: () => {
        // El error ya viene como AppError (interceptor) y el componente
        // lo captura v\u00eda su propio subscribe en {@link loadCatalogAsync}.
        this._loading.set(false);
      },
    });
  }

  /**
   * Variante que devuelve una Promise para que el componente pueda hacer
   * {@code try/catch} del AppError. Usada desde {@code ngOnInit} y desde
   * el bot\u00f3n "Reintentar" del banner.
   */
  async loadCatalogAsync(): Promise<void> {
    this._loading.set(true);
    try {
      const list = await this.http
        .get<Herramienta[]>(`${API_BASE_URL}/tools`)
        .toPromise();
      this._catalog.set(list ?? []);
    } finally {
      this._loading.set(false);
    }
  }

  /**
   * Consulta de disponibilidad (GET /api/tools/available). Devuelve una
   * Promise para que el componente del formulario pueda esperar el
   * resultado y mostrar error granular.
   *
   * @param fechaInicio YYYY-MM-DD
   * @param fechaFin    YYYY-MM-DD
   * @param categoria   opcional \u2014 si viene, se a\u00f1ade al query string
   */
  async loadAvailable(
    fechaInicio: string,
    fechaFin: string,
    categoria?: Categoria | null,
  ): Promise<void> {
    this._loading.set(true);
    let params = new HttpParams()
      .set('fechaInicio', fechaInicio)
      .set('fechaFin', fechaFin);
    if (categoria) {
      params = params.set('categoria', categoria);
    }
    try {
      const list = await this.http
        .get<Herramienta[]>(`${API_BASE_URL}/tools/available`, { params })
        .toPromise();
      this._available.set(list ?? []);
    } finally {
      this._loading.set(false);
    }
  }

  /** Limpia los resultados de disponibilidad (al salir del form, p.ej.). */
  clearAvailable(): void {
    this._available.set([]);
  }
}
