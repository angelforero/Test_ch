# Pruebas

Por convención de cada framework, las pruebas viven **junto al código** que prueban: en Maven bajo `src/test/java` y en Angular como `*.spec.ts` al lado de cada archivo. Esta carpeta reúne las **referencias** a todas ellas, cómo ejecutarlas y qué requisito cubre cada una.

Estrategia (doc 01, D19): **solo tests unitarios con mocks**.
- Backend: JUnit 5 + Mockito + *slices* `@WebMvcTest`, con un **techo** de cobertura JaCoCo del 75 %.
- Frontend: Jasmine + Karma con `HttpTestingController`, con un **mínimo** de cobertura del 30 %.

No hay E2E: el flujo completo se validó con smoke tests manuales contra el stack arrancado. La trazabilidad completa requisito → test está en [`../especificacion/02-requisitos-y-criterios-de-aceptacion.md`](../especificacion/02-requisitos-y-criterios-de-aceptacion.md) (matriz final).

---

## Backend — 36 tests

Ubicación: [`../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/`](../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/)

| Clase | Tipo | Tests | Cubre |
|-------|------|------:|-------|
| [`service/ReservationServiceTest`](../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/service/ReservationServiceTest.java) | Unitario, Mockito, `Clock` fijo | 13 | R02 (creación, herramienta inexistente, rango, correo normalizado), R03 (solape), R04 (límite, mayúsculas), R05 (cancelar CONFIRMADA / ya cancelada / finalizada / en curso / inexistente, listar normalizado) |
| [`web/controller/ReservationControllerTest`](../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/web/controller/ReservationControllerTest.java) | `@WebMvcTest` + handler real | 7 | POST 201 + `Location`; correo con espacios → normalizado; 422 con `errors[]`; 409 `overlap-conflict`; 409 `active-reservation-limit`; DELETE 200; 409 `cancellation-not-allowed` (R02–R05, R08) |
| [`service/ToolServiceTest`](../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/service/ToolServiceTest.java) | Unitario, Mockito | 6 | R01: catálogo ordenado, filtro, disponibilidad; reglas de rango (invertido, pasado, 8 días) |
| [`web/controller/ToolControllerTest`](../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/web/controller/ToolControllerTest.java) | `@WebMvcTest` + handler real | 5 | R01 + R08: 200, filtro, 422 por rango inválido, 400 si falta `fechaInicio`, propagación de parámetros |
| [`domain/EstadoReservaCalculadorTest`](../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/domain/EstadoReservaCalculadorTest.java) | Unitario puro | 4 | Derivación de los 4 estados, bordes inclusivos (R05) |
| [`HerramientasApplicationTests`](../src/backend/herramientas/src/test/java/com/ch/prueba/herramientas/HerramientasApplicationTests.java) | `@SpringBootTest` | 1 | El contexto arranca |

```powershell
cd prueba\src\backend\herramientas
.\mvnw.cmd clean verify          # tests + regla JaCoCo (máx. 75 % de líneas)
.\mvnw.cmd test -Dtest=ReservationServiceTest   # una sola clase
```
Informe de cobertura: `target/site/jacoco/index.html` (al cierre: 73,6 % de líneas sobre las clases medidas).

---

## Frontend — 35 tests

Ubicación: [`../src/frontend/todo-app/src/app/`](../src/frontend/todo-app/src/app/) (`*.spec.ts`)

| Spec | Tests | Cubre |
|------|------:|-------|
| `features/reservations/components/my-reservations/my-reservations.component.spec.ts` | 7 | R07: correo inválido no llama al API, listado, lista vacía, Cancelar solo si `CONFIRMADA`, cancelación OK, `confirm=false`, 409 con motivo traducido |
| `core/interceptors/problem-detail.interceptor.spec.ts` | 7 | R08: 409, 422 con `fieldErrors`, 500 sin body, error de red, `type` desconocido, error arbitrario, 200 intacto |
| `features/tools/components/availability-form/availability-form.component.spec.ts` | 5 | R06: no emite sin fechas; validadores de orden, pasado y duración ≤ 7 |
| `features/tools/services/tools.service.spec.ts` | 4 | R01: `GET /api/tools`, filtro en cliente, disponibilidad con y sin categoría |
| `features/reservations/components/reservation-form/reservation-form.component.spec.ts` | 3 | R06: no envía vacío, crea y muestra el resultado, muestra el 409 de solape |
| `features/reservations/services/reservations.service.spec.ts` | 3 | Verbo, URL y parámetros de crear / cancelar / listar |
| `shared/components/error-banner/error-banner.component.spec.ts` | 3 | Título, detalle, errores de campo; oculto si no hay error |
| `features/reservations/components/reservation-success/reservation-success.component.spec.ts` | 1 | Tarjeta de confirmación |
| `features/tools/components/tools-list/tools-list.component.spec.ts` | 1 | Carga y pinta el catálogo |
| `src/all-sources.spec.ts` | 1 | Importa todos los módulos para que la cobertura mida la app entera |

```powershell
cd prueba\src\frontend\todo-app
npm install
npm test        # Karma ChromeHeadless + cobertura + check-coverage-min.js (>= 30 %)
```
Informe de cobertura: `coverage/` (al cierre: statements 86 %, branches 70 %, functions 70 %, lines 86 %).

---

## Smoke test manual (integración)

Con backend (`.\mvnw.cmd spring-boot:run`) y frontend (`npm start`) arrancados, se verificaron por API (`curl` vía `http://localhost:4200/api`) y por la interfaz:

| # | Paso | Resultado esperado |
|---|------|--------------------|
| 1 | Catálogo | 200, 10 herramientas |
| 2 | Disponibilidad por rango (+ categoría) | 200, sin las herramientas ocupadas |
| 3 | Crear reserva válida | 201 + `Location`, estado `CONFIRMADA` |
| 4 | Reserva solapada | 409 `overlap-conflict` (banner en la UI) |
| 5 | Mis reservas por correo | 200 con la reserva (también con el correo en mayúsculas) |
| 6 | Cancelar una `CONFIRMADA` | 200, estado `CANCELADA` |
| 7 | Cancelar de nuevo | 409 `cancellation-not-allowed`, `motivo: YA_CANCELADA` |

Extra: la 4.ª reserva activa del mismo correo, aunque se escriba con otras mayúsculas → 409 `active-reservation-limit`; `/v3/api-docs` → 200.

**Sin cubrir:** R09 (latencia con 200 herramientas / 5.000 reservas) y la concurrencia de CA-R03.7. Ver *Observaciones* en el [README raíz](../../README.md).
