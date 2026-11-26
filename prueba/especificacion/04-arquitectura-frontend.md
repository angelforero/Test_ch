# 04 — Arquitectura del Frontend (Angular 19)

> **Versión:** 1.1 · **Fecha:** 2026-11-25 (v1.0: 2026-11-22)
> Arquitectura de la aplicación Angular que consume el API definido en `03-contrato-api.md`.
> **Proyecto:** `prueba/src/frontend/todo-app` (nombre heredado del scaffold, ver D15 en doc 01).
>
> **v1.1 — refleja la implementación real.** La v1.0 (commit `97a2469`) era un diseño previo: dos páginas `/buscar` y `/mis-reservas`, clases `ToolsApi`/`ReservationsApi`/`ApiError`, `ConfirmDialog` propio y carpetas `models/` y `shared/` con más componentes. Durante la implementación se reorganizó por *feature* y se simplificó. Este documento describe lo que **hay** en el código; las diferencias con la v1.0 se resumen en §13 y se justifican en el doc 01 §10.

---

## 1. Stack y versiones

| Elemento | Versión / Decisión |
|----------|-------------------|
| Angular | **19.2.25** (`@angular/core`, fijada en `package-lock.json`) · CLI `^19.2.27` |
| TypeScript | ~5.7 |
| Node.js | 18.19+/20.11+/22 (verificado con 20.19) |
| Componentes | **Standalone** (sin NgModules) |
| Control flow | `@if`, `@for` |
| HTTP | `HttpClient` vía `provideHttpClient(withInterceptors([...]))` |
| Formularios | **Reactive Forms** (`FormBuilder`, validadores síncronos) |
| Router | `provideRouter(routes, withComponentInputBinding())`, rutas **lazy** con `loadComponent` |
| Estado | **Signals** (`signal`, `computed`) en servicios y componentes |
| Estilos | **CSS vanilla** en línea en cada componente + `styles.css` global mínimo. Sin librería UI |
| Testing | Jasmine + Karma (ChromeHeadless), cobertura con `karma-coverage` |

---

## 2. Estructura de carpetas

```
src/
├── main.ts
├── index.html
├── styles.css
├── all-sources.ts / all-sources.spec.ts   # fuerza a Karma a medir TODOS los módulos (§11)
└── app/
    ├── app.component.ts        # Shell: cabecera con nav (Catálogo · Nueva reserva · Mis reservas) + <router-outlet>
    ├── app.config.ts           # providers: router, HttpClient + interceptor
    ├── app.routes.ts           # 3 rutas lazy + redirects
    │
    ├── core/                   # transversal
    │   ├── api/api.config.ts                     # API_BASE_URL = '/api'
    │   ├── errors/app-error.ts                   # clase AppError
    │   ├── errors/problem-type.ts                # enum ProblemType + problemTypeFromUrl()
    │   ├── interceptors/problem-detail.interceptor.ts (+ .spec)
    │   └── validators/date-range.validators.ts   # orderedRange, noPast, maxDuration
    │
    ├── shared/components/error-banner/           # ErrorBannerComponent (+ .spec)
    │
    └── features/
        ├── tools/
        │   ├── models/   categoria.ts (enum + labels), herramienta.model.ts
        │   ├── services/ tools.service.ts (+ .spec)
        │   └── components/
        │       ├── tools-list/          ToolsListComponent        (ruta /herramientas)
        │       └── availability-form/   AvailabilityFormComponent (hijo de tools-list)
        └── reservations/
            ├── models/   estado-reserva.ts (enum + labels), reserva.model.ts, reserva-creacion.model.ts
            ├── services/ reservations.service.ts (+ .spec)
            └── components/
                ├── reservation-form/     ReservationFormComponent    (ruta /nueva-reserva)
                ├── reservation-success/  ReservationSuccessComponent (tarjeta de confirmación)
                └── my-reservations/      MyReservationsComponent     (ruta /mis-reservas)
```

Los modelos viven dentro de cada *feature* (no en un `models/` global) para que cada carpeta sea autocontenida.

---

## 3. Rutas (`app.routes.ts`)

| Path | Componente (lazy) | Título | Requisito |
|------|-------------------|--------|-----------|
| `''` | redirect a `herramientas` | — | — |
| `herramientas` | `ToolsListComponent` | "Catálogo de herramientas" | R01, R06 |
| `nueva-reserva` | `ReservationFormComponent` | "Nueva reserva" | R02–R04, R06 |
| `mis-reservas` | `MyReservationsComponent` | "Mis reservas" | R05, R07 |
| `**` | redirect a `herramientas` | — | — |

Las tres rutas usan `loadComponent()`: el build genera un chunk por pantalla (`tools-list-component`, `reservation-form-component`, `my-reservations-component`).

---

## 4. Modelos TypeScript (contrato con el API)

Alineados 1:1 con `03-contrato-api.md §2`.

- `Categoria` — **enum** TS con los 7 valores del backend + `CATEGORIA_LABEL` y `CATEGORIAS`. No hay endpoint de categorías (decisión en doc 01 §10).
- `Herramienta` — `{ id, nombre, categoria }`.
- `EstadoReserva` — enum `CONFIRMADA | EN_CURSO | FINALIZADA | CANCELADA` + `ESTADO_RESERVA_LABEL`.
- `Reserva` — `{ id, herramientaId, nombreHerramienta, nombre, correo, fechaInicio, fechaFin, estado, cancelada }` (fechas como string ISO `YYYY-MM-DD`).
- `ReservaCreacion` — `{ herramientaId, nombre, correo, fechaInicio, fechaFin }`.

---

## 5. Servicios

Ambos `@Injectable({ providedIn: 'root' })`, construyen URLs a partir de `API_BASE_URL` y **no** manejan errores: lo centraliza el interceptor (§6).

### 5.1 `ToolsService` (estado con signals)

| Miembro | Descripción |
|---------|-------------|
| `catalog()` / `available()` / `loading()` | signals de solo lectura |
| `filteredCatalog` | `computed` que aplica el filtro de categoría **en cliente** sobre `catalog()` |
| `setCategoryFilter(c)` | cambia el filtro del catálogo |
| `loadCatalogAsync()` | `GET /api/tools` → publica en `catalog()`. Lo reutiliza también `ReservationFormComponent` |
| `loadAvailable(inicio, fin, categoria?)` | `GET /api/tools/available` → publica en `available()`; omite `categoria` si es `null` |

### 5.2 `ReservationsService` (Promises sobre `HttpClient`)

| Método | Endpoint | Devuelve |
|--------|----------|----------|
| `crear(payload)` | `POST /api/reservations` | `Promise<Reserva>` |
| `listarPorCorreo(correo)` | `GET /api/reservations?correo=…` | `Promise<Reserva[]>` |
| `cancelar(id)` | `DELETE /api/reservations/{id}` | `Promise<Reserva>` (DTO actualizado, 200) |

Se usan `Promise` (`firstValueFrom`) porque cada llamada es puntual y los componentes la consumen con `async/await` + signals.

---

## 6. Manejo de errores centralizado

### 6.1 `problemDetailInterceptor`

Interceptor funcional registrado en `app.config.ts`. Convierte **cualquier** error HTTP en un `AppError`:

1. `status === 0` (sin red) → `AppError(Unknown, 'Sin conexión', 'No se pudo contactar con el servidor…', 0)`.
2. Body con forma de ProblemDetail (`title` o `detail` string) → `AppError` con `type` resuelto por `problemTypeFromUrl()`, `title`, `detail`, `status` y el resto de propiedades en `extras` (incluido `errors[]` y `motivo`).
3. Error sin body ProblemDetail → `InternalError` si ≥ 500, `Unknown` en otro caso.
4. Cualquier otra cosa lanzada → `AppError(Unknown, 'Error inesperado', …)`.

### 6.2 `AppError`

```ts
class AppError extends Error {
  type: ProblemType; title: string; detail: string; status: number;
  extras: Record<string, unknown>;
  get fieldErrors(): { campo: string; mensaje: string; valorRechazado?: unknown }[];
}
```

`ProblemType` mapea los slugs del catálogo de `type` (§3.1 del doc 03). **Limitación conocida:** el enum del frontend incluye slugs que el backend no emite (`tool-not-found`, `reservation-not-found`, `rule-violation`, `missing-parameter`) y no incluye `not-found`, de modo que un 404 llega como `ProblemType.Unknown`. No afecta a la UI (se muestran igualmente `title` y `detail` del servidor), pero conviene alinearlo.

### 6.3 `ErrorBannerComponent`

Componente presentacional (`@Input() error: AppError | null`) con `role="alert"`. Pinta `title`, `detail` y, si existen, los `fieldErrors` del 422. Lo usan `ReservationFormComponent` y `MyReservationsComponent` (R06 CA-R06.5, R07, R08).

---

## 7. Pantalla Catálogo — `/herramientas` (R01, R06)

`ToolsListComponent` (smart) con dos bloques:

1. **Catálogo completo**: tabla `ID · Nombre · Categoría` con selector "Filtrar por categoría" (filtro en cliente vía `filteredCatalog`). Estado "Cargando…" y vacío "No hay herramientas para la categoría seleccionada.".
2. **Consulta de disponibilidad**: `AvailabilityFormComponent` (presentacional; `@Output() query`) con *Desde*, *Hasta* y *Categoría*. Validadores compartidos de `core/validators`: `orderedRangeValidator`, `noPastValidator`, `maxDurationValidator(7)`; con el formulario inválido el botón "Consultar" se deshabilita y se muestra el mensaje inline (CA-R06.3). Al emitir, el padre llama `loadAvailable()` y pinta "Disponibles en el rango" (vacío: "No hay herramientas disponibles en ese rango.").

---

## 8. Pantalla Nueva reserva — `/nueva-reserva` (R02–R04, R06)

`ReservationFormComponent` (smart):

- Formulario reactivo: `herramientaId` (select con el catálogo de `ToolsService.loadCatalogAsync()`), `nombre` (required, max 120), `correo` (required, email, max 254), `fechaInicio`, `fechaFin` (required, con `[min]` = hoy en el selector de fecha). Las reglas de orden y duración ≤ 7 días las valida el **backend** (422 mostrado en el banner).
- Envío: botón "Crear reserva" → "Creando reserva…" y deshabilitado mientras dura la petición (CA-R06.6).
- Éxito (201): `ReservationSuccessComponent` muestra número, herramienta, fechas y estado; "Hacer otra reserva" **conserva nombre y correo** y limpia herramienta y fechas. Sin navegación automática ni modal.
- Error: `ErrorBannerComponent` con el `detail` del API (409 solapamiento / límite, 404, 422 con errores por campo). El formulario no se cierra (CA-R06.5).

---

## 9. Pantalla Mis reservas — `/mis-reservas` (R05, R07)

`MyReservationsComponent` (smart):

- Estado con signals: `reservas`, `loading`, `queried`, `cancelingId`, `error`.
- Formulario reactivo con `correo` (`required` + `email`); con correo inválido el botón "Buscar" queda deshabilitado y no se llama al API (CA-R07.5). El correo **no** se guarda en `localStorage`.
- Tabla `# · Herramienta · Desde · Hasta · Estado · Acciones`. Estado con badge de color por `data-estado`; las canceladas aparecen tachadas. Vacío: "No se encontraron reservas para este correo.".
- Botón **Cancelar** solo si `estado === CONFIRMADA` (CA-R07.3). Pide confirmación con `window.confirm()` nativo; al aceptar llama `cancelar(id)` y **sustituye la fila** por el DTO devuelto (CA-R07.2). Durante la petición muestra "Cancelando…".
- 409 `cancellation-not-allowed`: banner con el `detail` del API más una frase según `motivo` (`YA_INICIADA` / `YA_FINALIZADA` / `YA_CANCELADA`). **Mejora pendiente:** tras un 409 la fila no se refresca (sigue mostrándose como Confirmada hasta volver a buscar) y el texto resultante es algo redundante.

---

## 10. Configuración

### `app.config.ts`
```ts
providers: [
  provideZoneChangeDetection({ eventCoalescing: true }),
  provideRouter(routes, withComponentInputBinding()),
  provideHttpClient(withInterceptors([problemDetailInterceptor])),
]
```
*(No se usa `withFetch()` como preveía la v1.0: la app no usa SSR, que es donde aporta; el backend XHR por defecto es suficiente.)*

### Base del API y proxy (D16)
- `core/api/api.config.ts`: `export const API_BASE_URL = '/api'` (sin `environments/`).
- `proxy.conf.json`: `/api` → `http://localhost:8080` (`changeOrigin: true`), referenciado en `angular.json` (`serve.options.proxyConfig`). El backend no configura CORS.

---

## 11. Pruebas frontend

Alineado con D19: solo tests **unitarios** con mocks (`HttpTestingController`, `TestBed`, espías de Jasmine). **35 tests**.

| Spec | Qué valida |
|------|-----------|
| `tools.service.spec` | `GET /api/tools`, filtro en cliente, `available` con/sin categoría |
| `availability-form.component.spec` | no emite sin fechas; emite la query; validadores de orden, pasado y duración |
| `tools-list.component.spec` | carga el catálogo y pinta filas |
| `reservations.service.spec` | verbo, URL y params de crear / cancelar / listar |
| `reservation-form.component.spec` | no envía vacío; crea y muestra resultado; muestra 409 de solape |
| `reservation-success.component.spec` | renderiza id, herramienta, fechas y estado |
| `my-reservations.component.spec` | 7 casos de R07 (validación, listado, vacío, botón condicional, cancelación OK, `confirm=false`, 409 traducido) |
| `problem-detail.interceptor.spec` | 409, 422 con `fieldErrors`, 500 sin body, red, type desconocido, throw arbitrario, 200 intacto |
| `error-banner.component.spec` | title/detail, errores de campo, oculto si `null` |

**Cobertura:** `npm test` ejecuta `ng test --watch=false --browsers=ChromeHeadless --code-coverage` y después `node scripts/check-coverage-min.js`, que **falla si statements, branches, functions o lines < 30 %**. No hay techo máximo (D19). `src/all-sources.ts` importa todos los módulos para que el denominador incluya también los no tocados por los specs. Al cierre: statements 86 %, branches 70 %, functions 70 %, lines 86 %.

**No se prueba:** E2E/navegación real ni regresión visual. El flujo completo se validó manualmente contra el backend (smoke test de los 7 pasos).

---

## 12. UX — textos implementados

| Situación | Texto en la UI |
|-----------|----------------|
| Cargando catálogo | "Cargando…" |
| Creando reserva | "Creando reserva…" |
| Buscando mis reservas | "Buscando…" |
| Cancelando | "Cancelando…" |
| Sin resultados (disponibilidad) | "No hay herramientas disponibles en ese rango." |
| Sin resultados (mis reservas) | "No se encontraron reservas para este correo." |
| Reserva creada | Tarjeta "Reserva creada correctamente" con número, herramienta, fechas y estado |
| Error de red | "Sin conexión — No se pudo contactar con el servidor…" |
| 409 / 422 / 404 | `title` + `detail` del API (y `errors[]` en 422) en el `ErrorBanner` |

Accesibilidad mínima: `<label>` envolviendo cada control, `role="alert"` en el banner, botones `type="button"` salvo los submit, navegación por teclado nativa.

---

## 13. Diferencias con la v1.0 (resumen)

| v1.0 (diseño previo) | v1.1 (implementado) | Motivo |
|----------------------|---------------------|--------|
| Rutas `/buscar` y `/mis-reservas` | `/herramientas`, `/nueva-reserva`, `/mis-reservas` | Separar catálogo/disponibilidad de la creación simplificó cada componente y sus tests |
| Botón "Reservar" en cada herramienta disponible + modal | Pantalla `/nueva-reserva` con select del catálogo | Menos estado compartido; mejora de UX pendiente |
| `ToolsApi` / `ReservationsApi` con `Observable` | `ToolsService` (signals) / `ReservationsService` (`Promise`) | Estado reactivo con signals donde aporta; promesas para llamadas puntuales |
| `ApiError` con `isConflict()`… | `AppError` + enum `ProblemType` | Tipado por `type` del ProblemDetail en lugar de por status |
| `ConfirmDialog`, `LoadingSpinner`, `DateRangePicker`, `CategoryFilter` compartidos | `window.confirm()`, textos de carga en botones, validadores en `core/validators` | No inflar el alcance de una prueba de 4 h |
| `environments/environment.ts` | `core/api/api.config.ts` | Un único valor (`/api`) no justifica la carpeta |
| Karma con umbral 75 % | Script `check-coverage-min.js` ≥ 30 % sin techo | Aclaración del usuario sobre D19 |
| `provideHttpClient(withFetch(), …)` | sin `withFetch()` | Innecesario para el alcance |
