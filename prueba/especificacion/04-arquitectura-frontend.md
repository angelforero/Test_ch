# 04 — Arquitectura del Frontend (Angular 19)

> **Versión:** 1.0 · **Fecha:** 2026-11-22
> Guía de arquitectura para la aplicación Angular que consume el API definido en `03-contrato-api.md`. Nivel de detalle suficiente para implementar sin ambigüedad; no incluye código, solo estructura, contratos TS y responsabilidades.
> **Proyecto base:** `prueba/src/frontend/todo-app` (nombre heredado del scaffold, ver D15 en doc 01).

---

## 1. Stack y versiones

| Elemento | Versión / Decisión |
|----------|-------------------|
| Angular | 19.2.x (ya scaffoldeado) |
| TypeScript | 5.7 |
| Estilo de componentes | **Standalone components** (sin NgModule) |
| Control flow | Nuevo: `@if`, `@for`, `@switch` |
| HTTP | `HttpClient` con `provideHttpClient(withFetch())` |
| Formularios | **Reactive Forms** (`ReactiveFormsModule`) |
| Router | `provideRouter(...)` con rutas lazy |
| Estado | **Signals** (`signal`, `computed`, `effect`) en servicios |
| Estilos | CSS por componente (scoped). Un `styles.css` global mínimo. |
| Testing | Jasmine + Karma (default del CLI) |

---

## 2. Estructura de carpetas propuesta

```
src/
├── app/
│   ├── app.component.ts              # Shell: nav + <router-outlet>
│   ├── app.component.html
│   ├── app.component.css
│   ├── app.config.ts                 # providers (HttpClient, Router, errorInterceptor)
│   ├── app.routes.ts                 # 2 rutas lazy + redirect
│   │
│   ├── core/                         # Servicios singleton transversales
│   │   ├── api/
│   │   │   ├── tools.api.ts          # GET /tools, GET /tools/available
│   │   │   └── reservations.api.ts   # POST, GET, DELETE /reservations
│   │   ├── interceptors/
│   │   │   └── problem-detail.interceptor.ts   # mapea 4xx/5xx a ApiError
│   │   └── errors/
│   │       └── api-error.ts          # tipo ApiError + helpers
│   │
│   ├── models/                       # Tipos TS alineados 1:1 con el contrato
│   │   ├── categoria.ts
│   │   ├── estado-reserva.ts
│   │   ├── herramienta.ts
│   │   ├── reserva.ts
│   │   └── problem-detail.ts
│   │
│   ├── shared/                       # Componentes reutilizables
│   │   ├── date-range-picker/
│   │   ├── category-filter/
│   │   ├── error-banner/
│   │   ├── loading-spinner/
│   │   └── confirm-dialog/
│   │
│   └── features/
│       ├── buscar-reservar/
│       │   ├── buscar-reservar.page.ts    # Smart component (señales + API)
│       │   ├── buscar-reservar.page.html
│       │   ├── buscar-reservar.page.css
│       │   ├── buscar-reservar.page.spec.ts
│       │   ├── reserva-form.component.ts  # Dumb: formulario de reserva
│       │   └── herramienta-card.component.ts  # Dumb: tarjeta de herramienta
│       │
│       └── mis-reservas/
│           ├── mis-reservas.page.ts       # Smart component
│           ├── mis-reservas.page.html
│           ├── mis-reservas.page.css
│           ├── mis-reservas.page.spec.ts
│           └── reserva-item.component.ts  # Dumb: fila de reserva
│
├── environments/
│   ├── environment.ts                # apiBaseUrl = '/api' (dev con proxy)
│   └── environment.prod.ts
│
├── index.html
├── main.ts
└── styles.css
```

> La estructura actual del scaffold (`app/components/`, `app/models/`, `app/services/`) se **reorganiza** a `core/`, `models/`, `shared/`, `features/` para separar claramente concerns.

---

## 3. Rutas

Fichero `app.routes.ts`:

| Path | Componente (lazy) | Título | Requisito |
|------|-------------------|--------|-----------|
| `''` | redirect a `/buscar` | — | — |
| `/buscar` | `BuscarReservarPage` | "Buscar y reservar" | R06 |
| `/mis-reservas` | `MisReservasPage` | "Mis reservas" | R07 |
| `**` | redirect a `/buscar` | — | — |

Las dos rutas se cargan con `loadComponent()` para mantener el bundle inicial pequeño.

---

## 4. Modelos TypeScript (contrato con el API)

> Alineación 1:1 con `03-contrato-api.md §2`. Cualquier cambio en el API exige actualizar estos tipos.

```ts
// models/categoria.ts
export type Categoria =
  | 'PERFORACION' | 'CORTE' | 'LIJADO'
  | 'MEDICION' | 'ESCALERAS' | 'JARDIN' | 'OTROS';

export const CATEGORIAS: readonly Categoria[] = [
  'PERFORACION', 'CORTE', 'LIJADO', 'MEDICION', 'ESCALERAS', 'JARDIN', 'OTROS'
] as const;

export const CATEGORIA_LABEL: Record<Categoria, string> = {
  PERFORACION: 'Perforación',
  CORTE:       'Corte',
  LIJADO:      'Lijado',
  MEDICION:    'Medición',
  ESCALERAS:   'Escaleras',
  JARDIN:      'Jardín',
  OTROS:       'Otros',
};
```

```ts
// models/estado-reserva.ts
export type EstadoReserva = 'CONFIRMADA' | 'EN_CURSO' | 'FINALIZADA' | 'CANCELADA';
```

```ts
// models/herramienta.ts
import { Categoria } from './categoria';
export interface Herramienta {
  id: number;
  nombre: string;
  categoria: Categoria;
}
```

```ts
// models/reserva.ts
import { EstadoReserva } from './estado-reserva';
export interface Reserva {
  id: number;
  herramientaId: number;
  herramientaNombre: string;
  nombre: string;
  correo: string;
  fechaInicio: string;   // ISO YYYY-MM-DD
  fechaFin:    string;   // ISO YYYY-MM-DD
  estado: EstadoReserva;
}
export interface CrearReservaRequest {
  herramientaId: number;
  nombre: string;
  correo: string;
  fechaInicio: string;
  fechaFin: string;
}
```

```ts
// models/problem-detail.ts
export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail?: string;
  instance?: string;
  errors?: Array<{ field: string; message: string }>;
}
```

---

## 5. Servicios API

Dos clases inyectables con `providedIn: 'root'`. Cada método devuelve `Observable<T>` (interoperable con `toSignal` cuando se consume en componentes).

### 5.1 `ToolsApi`

| Método | Firma | Endpoint |
|--------|-------|----------|
| `listar` | `(categoria?: Categoria) => Observable<Herramienta[]>` | `GET /api/tools` |
| `disponibles` | `(fechaInicio: string, fechaFin: string, categoria?: Categoria) => Observable<Herramienta[]>` | `GET /api/tools/available` |

### 5.2 `ReservationsApi`

| Método | Firma | Endpoint |
|--------|-------|----------|
| `crear` | `(req: CrearReservaRequest) => Observable<Reserva>` | `POST /api/reservations` |
| `listarPorCorreo` | `(correo: string) => Observable<Reserva[]>` | `GET /api/reservations?correo=...` |
| `cancelar` | `(id: number) => Observable<void>` | `DELETE /api/reservations/{id}` |

**Convenciones:**
- Los params opcionales solo se añaden al `HttpParams` si están definidos.
- No se hace manejo de errores aquí: lo centraliza el **interceptor** (§6).

---

## 6. Manejo de errores centralizado

### 6.1 Interceptor `problem-detail.interceptor.ts`

Responsabilidades:

1. Captura `HttpErrorResponse`.
2. Si `error.error` cumple la estructura `ProblemDetail`, la pasa tal cual.
3. Si es un error de red o sin body (ej. 0, 504), construye un `ProblemDetail` sintético:
   ```ts
   { type: 'network-error', title: 'Error de red', status: 0,
     detail: 'No se pudo contactar con el servidor. Reintenta en unos segundos.' }
   ```
4. Lanza un `throwError(() => new ApiError(problemDetail))`.

### 6.2 Clase `ApiError`

```ts
export class ApiError extends Error {
  constructor(public readonly problem: ProblemDetail) {
    super(problem.detail ?? problem.title);
  }
  get isValidation(): boolean { return this.problem.status === 422; }
  get isConflict(): boolean   { return this.problem.status === 409; }
  get isNotFound(): boolean   { return this.problem.status === 404; }
  fieldErrors(): Record<string, string> {
    return Object.fromEntries(
      (this.problem.errors ?? []).map(e => [e.field, e.message])
    );
  }
}
```

### 6.3 Componente `ErrorBanner`

Dumb component que recibe un `ApiError | null` como input (signal) y muestra:
- `title` como encabezado.
- `detail` como mensaje principal.
- Lista de `errors[]` si existen.
- Cierre manual por parte del usuario.

Todas las pages usan `ErrorBanner` para cumplir R06 CA-R06.5 y R08.

---

## 7. Feature `buscar-reservar` (R06)

### 7.1 `BuscarReservarPage` (smart)

**Estado local con signals:**
```ts
fechaInicio = signal<string>('');
fechaFin    = signal<string>('');
categoria   = signal<Categoria | null>(null);
herramientas = signal<Herramienta[]>([]);
cargando    = signal<boolean>(false);
error       = signal<ApiError | null>(null);
reservaEnCurso = signal<Herramienta | null>(null);  // abre modal
exito       = signal<Reserva | null>(null);
```

**Acciones:**
- `buscar()` — valida rango en cliente → `ToolsApi.disponibles(...)` → actualiza `herramientas`.
- `abrirReserva(h: Herramienta)` → `reservaEnCurso.set(h)`.
- `confirmarReserva(nombre, correo)` → `ReservationsApi.crear(...)` → al éxito, quita la herramienta de la lista y muestra banner de éxito.

**Validación cliente antes de llamar al API (CA-R06.3):**
- `fechaInicio` y `fechaFin` requeridas.
- `fechaInicio ≤ fechaFin`.
- (Opcional) `fechaInicio ≥ hoy` para búsquedas útiles — no bloquea, solo warn.

### 7.2 `ReservaFormComponent` (dumb)

- Reactive form con `nombre` (required, max 120), `correo` (required, Validators.email).
- Emite evento `submit` con `{ nombre, correo }`.
- Recibe `loading` y `fieldErrors` como inputs para pintar mensajes servidor-side.

### 7.3 `HerramientaCardComponent` (dumb)

- Inputs: `herramienta: Herramienta`, `disabled: boolean`.
- Output: `reservar` event.

---

## 8. Feature `mis-reservas` (R07)

### 8.1 `MisReservasPage` (smart)

**Estado:**
```ts
correo     = signal<string>('');
reservas   = signal<Reserva[]>([]);
cargando   = signal<boolean>(false);
error      = signal<ApiError | null>(null);
```

**Acciones:**
- `buscar()` — valida `correo` (regex email simple) → `ReservationsApi.listarPorCorreo(...)`.
- `cancelar(r: Reserva)` — abre `ConfirmDialog`; al aceptar llama `ReservationsApi.cancelar(r.id)` y, al 204, actualiza esa reserva en la lista a `estado = 'CANCELADA'` sin recargar (CA-R07.2).

### 8.2 `ReservaItemComponent` (dumb)

- Input: `reserva: Reserva`.
- Output: `cancelar` event.
- Lógica de UI: botón "Cancelar" solo visible y habilitado si `reserva.estado === 'CONFIRMADA'` (CA-R07.3).
- Pinta el estado con un badge de color:
  - `CONFIRMADA` → azul
  - `EN_CURSO` → verde
  - `FINALIZADA` → gris
  - `CANCELADA` → rojo tenue, tachada

---

## 9. Componentes compartidos (`shared/`)

| Componente | Responsabilidad | Props | Eventos |
|------------|-----------------|-------|---------|
| `DateRangePicker` | Dos `<input type="date">` con validación cruzada | `fechaInicio`, `fechaFin`, `min` | `rangoCambiado` |
| `CategoryFilter` | `<select>` con todas las categorías + "Todas" | `valor` | `cambio` |
| `ErrorBanner` | Muestra `ApiError` de forma uniforme | `error` | `cerrar` |
| `LoadingSpinner` | Indicador visual | `activo` | — |
| `ConfirmDialog` | Diálogo modal de confirmación | `titulo`, `mensaje`, `abierto` | `confirmar`, `cancelar` |

Todos son standalone y se importan donde se usen.

---

## 10. Configuración (`app.config.ts`)

Providers esperados:

```ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withFetch(),
      withInterceptors([problemDetailInterceptor]),
    ),
  ],
};
```

---

## 11. Entornos y proxy

### `environment.ts`
```ts
export const environment = { apiBaseUrl: '/api' };
```

### `proxy.conf.json` (dev)
```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

### `angular.json` — ajuste del target `serve`
```jsonc
"serve": {
  "options": {
    "proxyConfig": "proxy.conf.json"
  }
}
```

Con esto, el frontend llama a `/api/...` y el CLI redirige al backend sin CORS (D16).

---

## 12. UX — mensajes y copys

Catálogo cerrado de mensajes para mantener coherencia:

| Situación | Texto en la UI |
|-----------|----------------|
| Loading búsqueda | "Buscando herramientas disponibles…" |
| Loading reserva | "Reservando…" |
| Loading cancelar | "Cancelando reserva…" |
| Sin resultados (búsqueda) | "No hay herramientas disponibles en el rango seleccionado." |
| Sin resultados (mis reservas) | "No encontramos reservas para este correo." |
| Reserva creada | "✓ Reserva #{id} creada. Recoge la herramienta el {fechaInicio}." |
| Reserva cancelada | "✓ Reserva #{id} cancelada." |
| Error de red | "No se pudo contactar con el servidor. Reintenta." |
| 409 límite activo | (el `detail` del API) |
| 409 solapamiento | (el `detail` del API) |
| 422 campos | lista de `errors` pintados junto a cada campo del form |

---

## 13. Accesibilidad (mínimos)

- Labels asociados a cada input (`<label for>` o wrap).
- Botones con `type="button"` salvo submit.
- Mensajes de error con `aria-live="polite"`.
- Navegable por teclado (tab order, Enter para submit).
- Contraste AA en estados y badges.

No se exige WCAG completo (fuera de alcance) pero sí lo anterior como higiene.

---

## 14. Pruebas frontend

> **Alineado con D19 (doc 01):** solo tests **unitarios** con **mocks**, cobertura **máxima 30 %** medida con el coverage de Karma (`ng test --code-coverage`). No hay E2E (ni Cypress ni Playwright) ni tests de integración multi-componente.

| Qué se prueba | Cómo se mockea | Herramienta |
|---------------|----------------|-------------|
| Servicios API (`ToolsApi`, `ReservationsApi`) — construcción de URL, query params, verbo HTTP, propagación del body | `HttpTestingController` (mock del backend) | `TestBed` + Jasmine |
| Interceptor `problemDetailInterceptor` — mapeo de 422/409/404/500/network a `ApiError`, respeto del body `ProblemDetail` | `HttpTestingController` | `TestBed` + Jasmine |
| Helper `ApiError` — `isValidation`, `isConflict`, `fieldErrors()` | sin dependencias externas | Jasmine puro |
| Al menos **un smart component por feature** (`BuscarReservarPage`, `MisReservasPage`) — happy path + manejo de un error 409 | servicios API inyectados como **mocks** (`jasmine.createSpyObj`) | `TestBed` |

**Lo que NO se prueba** (fuera de alcance por D19):
- Componentes dumb (`DateRangePicker`, `CategoryFilter`, `ErrorBanner`, `LoadingSpinner`, `ConfirmDialog`) — se validan manualmente.
- Flujos E2E navegación+render.
- Pixel-perfect ni visual regression.

**Config de cobertura** (`karma.conf.js`):
```js
coverageReporter: {
  check: {
    global: { statements: 30, branches: 30, functions: 30, lines: 30 }
    // Umbral máximo: superar 30 % indica sobre-testing (D19)
  }
}
```

> Nota: Karma permite configurar un umbral *mínimo* estándar. El objetivo "máximo 30 %" se vigila **en revisión**, no con fallo automático del build frontend. En backend, JaCoCo sí admite la regla `MAXIMUM 0.30`.

---

## 15. Trazabilidad requisito → artefacto

| Requisito | Página / Servicio | Nota |
|-----------|-------------------|------|
| R01 (catálogo, disponibilidad) | `ToolsApi` + `BuscarReservarPage` | Filtro por categoría en UI |
| R02 (crear reserva) | `ReservaFormComponent` + `ReservationsApi.crear` | Validación cliente + server |
| R03 (sin solapamientos) | — | Visible solo como error 409; UI muestra `detail` |
| R04 (límite por persona) | — | Visible como error 409; UI muestra `detail` |
| R05 (cancelar, consultar) | `MisReservasPage` + `ReservationsApi` | Botón condicional por estado |
| R06 (pantalla búsqueda) | `BuscarReservarPage` | Validación + errores visibles |
| R07 (pantalla mis reservas) | `MisReservasPage` | Confirmación antes de cancelar |
| R08 (errores uniformes) | `problem-detail.interceptor` + `ErrorBanner` | Un único punto de mapeo |
| R09 (rendimiento) | — | Responsabilidad del backend; el frontend solo muestra spinner |

---

## 16. Orden recomendado de implementación

1. Modelos TS + `environment.ts` + `proxy.conf.json`.
2. `ToolsApi` + `ReservationsApi` con tests (`HttpTestingController`).
3. Interceptor + `ApiError` + tests.
4. `ErrorBanner` + `LoadingSpinner` + `ConfirmDialog` (shared).
5. `MisReservasPage` (más sencilla, valida flujo GET/DELETE end-to-end).
6. `BuscarReservarPage` (más compleja: dos llamadas + modal).
7. Rutas, nav en `AppComponent`, redirects.
8. Pulido CSS y copys.
9. Tests unitarios de cada page (happy path + un 409) con servicios API mockeados — ver §14.

Este orden maximiza el feedback temprano contra el backend.
