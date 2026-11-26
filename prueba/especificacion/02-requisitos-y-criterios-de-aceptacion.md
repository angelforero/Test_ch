# 02 — Requisitos y Criterios de Aceptación

> **Versión:** 1.1 · **Fecha:** 2026-11-25 (v1.0: 2026-11-22)
> Reescritura detallada de los 9 requisitos del enunciado (R01–R09) con criterios de aceptación en formato **Gherkin** (Given/When/Then), casos borde y trazabilidad a endpoints y pantallas.
> Las referencias `[API: …]` apuntan al documento `03-contrato-api.md` y `[UI: …]` al `04-arquitectura-frontend.md`.
>
> **v1.1:** se ajustan los criterios cuyo resultado esperado difería de la implementación verificada (códigos HTTP de CA-R01.3, CA-R01.6, CA-R05.1/2, CA-R07.2; flujo de CA-R06.4; formato de `errors[]`) y la matriz de trazabilidad pasa a listar los tests **reales**. Cada cambio está marcado con *(v1.1)* y justificado en el doc 01 §10.

---

## Convenciones

- **Fechas**: formato ISO `YYYY-MM-DD`, zona horaria `Europe/Madrid`, sin hora.
- **"Hoy"**: fecha del servidor en el momento de la petición.
- **Rango inclusivo**: una reserva del `2026-11-24` al `2026-11-26` ocupa los 3 días completos.
- **Duración**: `fechaFin − fechaInicio + 1` (en días).
- **Correo**: se normaliza a minúsculas y `trim` antes de comparar.

---

## R01 — Catálogo y disponibilidad

**Descripción.** El API ofrece el catálogo de herramientas (id, nombre, categoría), con filtro opcional por categoría, y permite consultar qué herramientas están disponibles para un rango de fechas. La solución parte de al menos **10 herramientas precargadas**.

**Endpoints relacionados:** `[API: GET /api/tools]`, `[API: GET /api/tools/available]`
**Pantallas relacionadas:** `[UI: /herramientas — ToolsListComponent]`

### Criterios de aceptación

**CA-R01.1 — Listar catálogo completo**
```gherkin
Given el catálogo tiene al menos 10 herramientas precargadas
When un cliente hace GET /api/tools
Then la respuesta es 200 OK
And el body es un array con todas las herramientas
And cada herramienta tiene id, nombre y categoria
```

**CA-R01.2 — Filtrar por categoría**
```gherkin
Given existen herramientas de categorías PERFORACION y JARDIN
When un cliente hace GET /api/tools?categoria=PERFORACION
Then la respuesta es 200 OK
And todas las herramientas devueltas tienen categoria = PERFORACION
```

**CA-R01.3 — Categoría inexistente**
```gherkin
When un cliente hace GET /api/tools?categoria=INEXISTENTE
Then la respuesta es 400 Bad Request
And el body es un ProblemDetail con type "malformed-request"
```
*(v1.1: v1.0 decía `validation-error`; un valor que no se puede convertir al enum es una petición mal formada — `MethodArgumentTypeMismatchException` → `malformed-request`, coherente con doc 03 §6-bis.2.)*

**CA-R01.4 — Consultar disponibilidad en un rango**
```gherkin
Given la herramienta T1 tiene una reserva del 2026-12-01 al 2026-12-05
And la herramienta T2 no tiene reservas
When un cliente hace GET /api/tools/available?fechaInicio=2026-12-03&fechaFin=2026-12-04
Then la respuesta es 200 OK
And T2 aparece en el resultado
And T1 NO aparece en el resultado
```

**CA-R01.5 — Disponibilidad + filtro por categoría combinados**
```gherkin
When un cliente hace GET /api/tools/available?fechaInicio=2026-12-10&fechaFin=2026-12-12&categoria=LIJADO
Then solo devuelve herramientas de categoría LIJADO disponibles en ese rango
```

**CA-R01.6 — Validación de fechas en disponibilidad**
```gherkin
When un cliente hace GET /api/tools/available?fechaInicio=2026-12-10&fechaFin=2026-12-05
Then la respuesta es 422 Unprocessable Entity
And el ProblemDetail tiene type "validation-error" e indica que fechaFin es anterior a fechaInicio

# Mismas reglas que al reservar (ValidadorRango compartido):
When el rango empieza antes de hoy o dura más de 7 días
Then la respuesta es 422 Unprocessable Entity
```
*(v1.1: v1.0 decía 400 aquí, en contradicción con doc 03 §4.2 que ya decía 422. Además la consulta aplica ahora las reglas de "no pasado" y "máx. 7 días".)*

**CA-R01.7 — Faltan parámetros de fecha**
```gherkin
When un cliente hace GET /api/tools/available sin fechaInicio o sin fechaFin
Then la respuesta es 400 Bad Request
```

**CA-R01.8 — Catálogo inicial**
```gherkin
Given el backend se arranca por primera vez (base de datos vacía)
When se completa el arranque
Then el catálogo contiene al menos 10 herramientas
And al menos 4 categorías distintas están representadas
```

---

## R02 — Crear reserva

**Descripción.** Una persona (nombre + correo) reserva una herramienta para un rango de fechas. Duración máxima **7 días**. La fecha de inicio **no puede ser anterior a hoy**.

**Endpoints relacionados:** `[API: POST /api/reservations]`
**Pantallas relacionadas:** `[UI: /nueva-reserva — ReservationFormComponent]`

### Criterios de aceptación

**CA-R02.1 — Reserva válida**
```gherkin
Given la herramienta T1 está disponible del 2026-12-01 al 2026-12-05
When se envía POST /api/reservations con
  {
    "herramientaId": T1,
    "nombre": "Ada Lovelace",
    "correo": "ada@example.com",
    "fechaInicio": "2026-12-01",
    "fechaFin": "2026-12-05"
  }
Then la respuesta es 201 Created
And el body contiene el id de la reserva, estado CONFIRMADA y los datos enviados
And el header Location apunta al recurso creado
```

**CA-R02.2 — Fecha de inicio en el pasado**
```gherkin
Given hoy es 2026-11-22
When se envía una reserva con fechaInicio = 2026-11-21
Then la respuesta es 422 Unprocessable Entity
And el ProblemDetail indica "fechaInicio no puede ser anterior a hoy"
```

**CA-R02.3 — Fecha de inicio hoy mismo (permitida)**
```gherkin
Given hoy es 2026-11-22
When se envía una reserva con fechaInicio = 2026-11-22 y fechaFin = 2026-11-24
Then la respuesta es 201 Created
```

**CA-R02.4 — Duración máxima 7 días**
```gherkin
When se envía una reserva con fechaInicio = 2026-12-01 y fechaFin = 2026-12-07
Then la respuesta es 201 Created  # 7 días exactos, permitido

When se envía una reserva con fechaInicio = 2026-12-01 y fechaFin = 2026-12-08
Then la respuesta es 422 Unprocessable Entity  # 8 días, rechazado
And el ProblemDetail indica "duración máxima de 7 días"
```

**CA-R02.5 — fechaFin anterior a fechaInicio**
```gherkin
When se envía una reserva con fechaInicio = 2026-12-05 y fechaFin = 2026-12-01
Then la respuesta es 422 Unprocessable Entity
```

**CA-R02.6 — Campos obligatorios**
```gherkin
When se envía una reserva sin nombre, sin correo, sin herramientaId o sin alguna fecha
Then la respuesta es 400 Bad Request (body malformado) o 422 (campos ausentes en body válido)
And el ProblemDetail lista todos los campos inválidos
```

**CA-R02.7 — Correo malformado**
```gherkin
When se envía una reserva con correo = "no-es-un-email"
Then la respuesta es 422 Unprocessable Entity
And el ProblemDetail indica "correo con formato inválido"
```

**CA-R02.8 — Herramienta inexistente**
```gherkin
When se envía una reserva con herramientaId = 9999 (no existe)
Then la respuesta es 404 Not Found
And el ProblemDetail indica "herramienta no encontrada"
```

**CA-R02.9 — Normalización del correo**
```gherkin
When se envía una reserva con correo = "  Ada@Example.COM  "
Then el correo se persiste como "ada@example.com"
And cuenta igual para el límite de 3 reservas (R04)
```

---

## R03 — Sin solapamientos

**Descripción.** Una herramienta nunca puede tener dos reservas activas que coincidan en algún día.

**Endpoints relacionados:** `[API: POST /api/reservations]` (chequeo), `[API: GET /api/tools/available]` (exclusión)

### Criterios de aceptación

**CA-R03.1 — Solapamiento total**
```gherkin
Given T1 tiene una reserva CONFIRMADA del 2026-12-10 al 2026-12-15
When se intenta reservar T1 del 2026-12-10 al 2026-12-15
Then la respuesta es 409 Conflict
And el ProblemDetail indica "la herramienta no está disponible en el rango solicitado"
```

**CA-R03.2 — Solapamiento parcial por el inicio**
```gherkin
Given T1 ocupada del 2026-12-10 al 2026-12-15
When se intenta reservar T1 del 2026-12-08 al 2026-12-11
Then la respuesta es 409 Conflict
```

**CA-R03.3 — Solapamiento parcial por el fin**
```gherkin
Given T1 ocupada del 2026-12-10 al 2026-12-15
When se intenta reservar T1 del 2026-12-14 al 2026-12-18
Then la respuesta es 409 Conflict
```

**CA-R03.4 — Una reserva contiene a otra**
```gherkin
Given T1 ocupada del 2026-12-10 al 2026-12-15
When se intenta reservar T1 del 2026-12-12 al 2026-12-13
Then la respuesta es 409 Conflict
```

**CA-R03.5 — Reservas adyacentes (sin solapar) → permitidas**
```gherkin
Given T1 ocupada del 2026-12-10 al 2026-12-15
When se intenta reservar T1 del 2026-12-16 al 2026-12-18
Then la respuesta es 201 Created  # el día 16 ya está libre
```

**CA-R03.6 — Reserva cancelada no bloquea**
```gherkin
Given T1 tenía una reserva CANCELADA del 2026-12-10 al 2026-12-15
When se intenta reservar T1 del 2026-12-12 al 2026-12-14
Then la respuesta es 201 Created
```

**CA-R03.7 — Concurrencia: dos reservas simultáneas al mismo rango**
```gherkin
Given dos clientes intentan reservar T1 del 2026-12-20 al 2026-12-22 al mismo tiempo
When ambas peticiones llegan en paralelo
Then exactamente una responde 201 Created
And la otra responde 409 Conflict
# Garantizado por transacción + chequeo dentro de la transacción (ver doc 03, sección "Concurrencia")
```

---

## R04 — Límite por persona

**Descripción.** Una persona (por correo) puede tener como máximo **3 reservas activas** (`CONFIRMADA` o `EN_CURSO`).

**Endpoints relacionados:** `[API: POST /api/reservations]`

### Criterios de aceptación

**CA-R04.1 — Hasta 3 permitidas**
```gherkin
Given ada@example.com tiene 2 reservas CONFIRMADAS
When crea una tercera reserva válida
Then la respuesta es 201 Created
```

**CA-R04.2 — La cuarta es rechazada**
```gherkin
Given ada@example.com tiene 3 reservas activas (CONFIRMADA o EN_CURSO)
When intenta crear una cuarta
Then la respuesta es 409 Conflict
And el ProblemDetail indica "límite de 3 reservas activas alcanzado"
```

**CA-R04.3 — Las canceladas no cuentan**
```gherkin
Given ada@example.com tiene 3 reservas de las cuales 1 está CANCELADA
When intenta crear una nueva
Then la respuesta es 201 Created  # solo 2 activas
```

**CA-R04.4 — Las finalizadas no cuentan**
```gherkin
Given ada@example.com tiene 3 reservas de las cuales 1 ya FINALIZÓ (fechaFin < hoy)
When intenta crear una nueva
Then la respuesta es 201 Created
```

**CA-R04.5 — El correo se compara normalizado**
```gherkin
Given ada@example.com tiene 3 reservas activas
When intenta crear una nueva con correo = "ADA@EXAMPLE.COM"
Then la respuesta es 409 Conflict  # mismo correo, distinto casing
```

---

## R05 — Cancelar y consultar

**Descripción.** El API permite **cancelar** una reserva hasta el día anterior a su fecha de inicio, y **consultar** las reservas de una persona con su estado.

**Endpoints relacionados:** `[API: DELETE /api/reservations/{id}]`, `[API: GET /api/reservations?correo=...]`
**Pantallas relacionadas:** `[UI: /mis-reservas — MyReservationsComponent]`

### Criterios de aceptación

**CA-R05.1 — Cancelar con antelación**
```gherkin
Given hoy es 2026-11-22
And existe una reserva R1 con fechaInicio = 2026-11-25
When se hace DELETE /api/reservations/R1
Then la respuesta es 200 OK
And el body es la Reserva R1 con estado CANCELADA y cancelada = true
```
*(v1.1: v1.0 decía 204 No Content; se devuelve el DTO para que la UI actualice la fila sin otro GET.)*

**CA-R05.2 — Cancelar el día anterior al inicio (permitido)**
```gherkin
Given hoy es 2026-11-24
And R1 tiene fechaInicio = 2026-11-25
When se hace DELETE /api/reservations/R1
Then la respuesta es 200 OK con estado CANCELADA
```

**CA-R05.3 — Cancelar el mismo día del inicio (rechazado)**
```gherkin
Given hoy es 2026-11-25
And R1 tiene fechaInicio = 2026-11-25
When se hace DELETE /api/reservations/R1
Then la respuesta es 409 Conflict
And el ProblemDetail indica "no se puede cancelar una reserva que ya ha comenzado"
```

**CA-R05.4 — Cancelar una reserva EN_CURSO o FINALIZADA (rechazado)**
```gherkin
Given R1 está EN_CURSO
When se hace DELETE /api/reservations/R1
Then la respuesta es 409 Conflict
```

**CA-R05.5 — Cancelar una reserva ya cancelada (idempotente defensivo)**
```gherkin
Given R1 está CANCELADA
When se hace DELETE /api/reservations/R1
Then la respuesta es 409 Conflict
And el ProblemDetail indica "la reserva ya está cancelada"
```

**CA-R05.6 — Cancelar reserva inexistente**
```gherkin
When se hace DELETE /api/reservations/9999
Then la respuesta es 404 Not Found
```

**CA-R05.7 — Consultar reservas de una persona**
```gherkin
Given ada@example.com tiene 2 reservas CONFIRMADAS, 1 CANCELADA y 1 FINALIZADA
When se hace GET /api/reservations?correo=ada@example.com
Then la respuesta es 200 OK
And el body contiene las 4 reservas con su estado derivado
And están ordenadas por fechaInicio descendente
```

**CA-R05.8 — Consultar sin resultados**
```gherkin
When se hace GET /api/reservations?correo=desconocido@example.com
Then la respuesta es 200 OK
And el body es un array vacío []
```

**CA-R05.9 — Consultar sin correo**
```gherkin
When se hace GET /api/reservations sin el parámetro correo
Then la respuesta es 400 Bad Request
```

**CA-R05.10 — Consulta con correo en distinto casing** *(v1.1)*
```gherkin
Given ada@example.com tiene reservas
When se hace GET /api/reservations?correo=ADA@EXAMPLE.COM
Then la respuesta es 200 OK con las mismas reservas
```

---

## R06 — Pantalla de búsqueda y reserva

**Descripción.** La interfaz permite elegir un rango de fechas, ver las herramientas disponibles (con filtro por categoría) y reservar una de ellas, mostrando los errores del API con claridad.

**Pantallas relacionadas:** `[UI: /herramientas + /nueva-reserva]`

### Criterios de aceptación

> **Implementación (v1.1):** R06 se reparte en **dos rutas** en lugar de una sola página: `/herramientas` (catálogo con filtro + formulario de disponibilidad + resultados) y `/nueva-reserva` (formulario de reserva). Ver doc 04 §3.

**CA-R06.1 — Flujo feliz**
```gherkin
Given el usuario está en la pantalla "Catálogo" (/herramientas)
When selecciona Desde = 2026-12-01, Hasta = 2026-12-03 y pulsa "Consultar"
Then se listan en "Disponibles en el rango" las herramientas disponibles
```

**CA-R06.2 — Filtro por categoría**
```gherkin
Given el usuario está en la pantalla "Catálogo"
When elige categoría "Lijado" en el formulario de disponibilidad y pulsa "Consultar"
Then solo se muestran herramientas LIJADO disponibles (filtro enviado al API)
And el selector "Filtrar por categoría" del catálogo filtra la tabla en cliente
```

**CA-R06.3 — Validación local antes de llamar al API**
```gherkin
Given el usuario introduce fechaInicio > fechaFin, una fecha pasada o más de 7 días
Then el botón "Consultar" queda deshabilitado y no se hace llamada al API
And se muestra un mensaje de validación inline
```

**CA-R06.4 — Reservar una herramienta** *(v1.1)*
```gherkin
Given el usuario está en "Nueva reserva" (/nueva-reserva)
When elige una herramienta del catálogo, introduce nombre, correo y fechas válidos y pulsa "Crear reserva"
Then se hace POST /api/reservations
And al recibir 201 se muestra una tarjeta de confirmación con número, herramienta, fechas y estado
And se conservan nombre y correo para la siguiente reserva; herramienta y fechas se limpian
```
*(v1.1: v1.0 preveía un botón "Reservar" en cada fila de la lista de disponibles y retirar la herramienta reservada de la lista. Se implementó como pantalla separada que reutiliza el catálogo; no hay acceso directo desde la fila. Queda como mejora de UX — ver doc 01 §10.)*

**CA-R06.5 — Error del API visible**
```gherkin
Given el usuario intenta reservar
When el API responde 409 Conflict con detail = "límite de 3 reservas activas alcanzado"
Then el frontend muestra ese detail al usuario de forma legible
And no cierra el formulario para que pueda corregir
```

**CA-R06.6 — Estado de carga**
```gherkin
When una petición al API está en curso
Then el botón que la disparó queda deshabilitado
And se muestra un indicador de carga
```
*(Implementado en "Crear reserva" → "Creando reserva…", "Buscar" → "Buscando…", "Cancelar" → "Cancelando…" y en el catálogo "Cargando…". El botón "Consultar" de disponibilidad solo se deshabilita por formulario inválido, no durante la petición.)*

---

## R07 — Pantalla "mis reservas"

**Descripción.** La interfaz permite a una persona consultar sus reservas por correo y cancelarlas.

**Pantallas relacionadas:** `[UI: /mis-reservas — MyReservationsComponent]`

### Criterios de aceptación

**CA-R07.1 — Consulta por correo**
```gherkin
Given el usuario está en la pantalla "Mis reservas"
When introduce "ada@example.com" y pulsa "Buscar"
Then se listan sus reservas con id, herramienta, rango de fechas y estado
```

**CA-R07.2 — Cancelar desde la lista**
```gherkin
Given el usuario ve una reserva CONFIRMADA con botón "Cancelar"
When pulsa "Cancelar" y confirma la acción (window.confirm)
Then se hace DELETE /api/reservations/{id}
And al recibir 200 la fila se sustituye por el DTO devuelto (estado CANCELADA) sin recargar
```

**CA-R07.3 — Botón deshabilitado si no se puede cancelar**
```gherkin
Given una reserva está EN_CURSO, FINALIZADA o CANCELADA
Then el botón "Cancelar" está deshabilitado o no se muestra
```

**CA-R07.4 — Lista vacía**
```gherkin
Given el correo introducido no tiene reservas
Then se muestra un mensaje "No se encontraron reservas para este correo."
```

**CA-R07.5 — Correo malformado**
```gherkin
When el usuario introduce un correo con formato inválido
Then el botón "Buscar" queda deshabilitado y no se llama al API
```

**CA-R07.6 — Cancelación rechazada por el API** *(v1.1)*
```gherkin
Given la reserva ya no es cancelable en el servidor (p. ej. se canceló desde otra sesión)
When el usuario pulsa "Cancelar" y confirma
Then el API responde 409 cancellation-not-allowed
And el ErrorBanner muestra el detail del API y una explicación según motivo
```

---

## R08 — Errores y validación (no funcional)

**Descripción.** El API valida sus entradas y responde errores con **formato consistente** y códigos HTTP apropiados.

### Criterios de aceptación

**CA-R08.1 — Formato uniforme**
```gherkin
Given cualquier respuesta de error (4xx o 5xx) del API
Then el Content-Type es "application/problem+json"
And el body sigue la estructura RFC 7807:
  { type, title, status, detail, errors?, <propiedades extra por tipo> }
```
*(v1.1: `instance` no se incluye en la implementación — es opcional en RFC 7807. Ver doc 03 §3.)*

**CA-R08.2 — Códigos HTTP coherentes**
```gherkin
| Situación                                     | Código |
| Body JSON malformado                          | 400    |
| Validación de campos (longitud, formato)      | 422    |
| Recurso no encontrado                         | 404    |
| Regla de negocio violada (solapamiento, etc.) | 409    |
| Error inesperado del servidor                 | 500    |
```

**CA-R08.3 — Errores de validación enumeran campos**
```gherkin
When se envía un POST con 3 campos inválidos
Then la respuesta 422 incluye un array "errors" con 3 entradas
And cada entrada tiene { campo, mensaje, valorRechazado? }
```

**CA-R08.4 — No se filtran stack traces**
```gherkin
Given ocurre un error interno
Then el body nunca contiene stack traces ni rutas internas
And se registra el detalle completo en los logs del servidor
```

---

## R09 — Rendimiento (no funcional)

**Descripción.** La consulta de disponibilidad responde en **menos de 500 ms** con un catálogo de **200 herramientas** y **5.000 reservas** existentes.

### Criterios de aceptación

**CA-R09.1 — Latencia bajo carga nominal**
```gherkin
Given la base de datos contiene 200 herramientas y 5.000 reservas (perfil "perf")
When se hace GET /api/tools/available?fechaInicio=X&fechaFin=Y
Then el tiempo de respuesta (p95, 20 muestras, en local sin otra carga) es < 500 ms
```

**CA-R09.2 — Índices presentes**
```gherkin
Given el esquema de base de datos está creado
Then existen índices sobre:
  - reservas.herramienta_id
  - reservas.fecha_inicio, reservas.fecha_fin (compuesto)
  - reservas.correo
```

**CA-R09.3 — Consulta eficiente**
```gherkin
Given la consulta de disponibilidad
Then se resuelve en una única query SQL con subconsulta NOT EXISTS (o LEFT JOIN WHERE NULL)
And NO se trae a memoria la totalidad de reservas para filtrar en Java
```

**CA-R09.4 — Prueba automatizada de rendimiento**
```gherkin
Given el perfil "perf" está activo
When se ejecuta el test de performance
Then el assertion verifica latencia < 500 ms
And la prueba se puede excluir del build por defecto (tag @performance)
```

> **Estado de R09 (v1.1):** CA-R09.2 (índices `idx_reservas_herramienta`, `idx_reservas_rango`, `idx_reservas_correo`, declarados en `Reserva`) y CA-R09.3 (`HerramientaRepository.findDisponibles`: una sola query JPQL con `NOT EXISTS`) **están implementados**. CA-R09.1 y CA-R09.4 **no**: no existe el perfil `perf` (S10) ni el test de latencia. El cumplimiento de los 500 ms se argumenta por diseño (query única indexada), no se ha medido. Es la principal pieza pendiente del alcance; ver doc 01 §10.

---

## Matriz de trazabilidad

> Estrategia de pruebas: **solo tests unitarios con mocks** (D19). Backend: JUnit 5 + Mockito + slices `@WebMvcTest`, cobertura JaCoCo ≤ 75 % (73,6 % de líneas al cierre). Frontend: Jasmine/Karma con `HttpTestingController`, cobertura ≥ 30 % (≈ 86 % statements al cierre). **No** hay E2E ni integración multi-servicio; el flujo completo se validó con smoke tests manuales contra el stack arrancado.
>
> *(v1.1: la tabla lista los tests **que existen**. La v1.0 citaba `DataSeederTest`, `GlobalExceptionHandlerTest` y `BuscarReservarPage.spec`, que no se llegaron a crear.)* Totales: **backend 36 tests** (`ReservationServiceTest` 13, `ReservationControllerTest` 7, `ToolServiceTest` 6, `ToolControllerTest` 5, `EstadoReservaCalculadorTest` 4, `HerramientasApplicationTests` 1); **frontend 35 tests**.

| Requisito | Endpoints API | Pantallas UI | Tests que lo validan |
|-----------|---------------|--------------|----------------------|
| R01 | `GET /api/tools`, `GET /api/tools/available` | `/herramientas` | `ToolServiceTest` (catálogo ordenado, filtro por categoría, disponibilidad delega al repo, rango invertido/pasado/8 días → excepción); `ToolControllerTest` (200, filtro, 422 rango inválido, 400 sin `fechaInicio`). Front: `tools.service.spec`, `tools-list.component.spec`. **Seeder (CA-R01.8): sin test dedicado** — verificado en arranque (10 herramientas, 7 categorías). |
| R02 | `POST /api/reservations` | `/nueva-reserva` | `ReservationServiceTest` (creación OK, herramienta inexistente, rango invertido, **correo normalizado** CA-R02.9); `ReservationControllerTest` (201 + `Location`, 422 con `errors[]`, **POST con espacios/mayúsculas** CA-R02.9); `ToolServiceTest` cubre pasado y > 7 días vía `ValidadorRango` compartido. Front: `reservation-form.component.spec`, `reservations.service.spec`. |
| R03 | `POST /api/reservations`, `GET /api/tools/available` | (error visible en `/nueva-reserva`) | `ReservationServiceTest` (solape → `SolapamientoReservaException`); `ReservationControllerTest` (409 `overlap-conflict`). La fórmula de solape vive en la query JPQL (no mockeable): CA-R03.1–R03.6 verificados en smoke test. **CA-R03.7 (concurrencia) no cubierto** — ver doc 03 §7. |
| R04 | `POST /api/reservations` | (error visible en `/nueva-reserva`) | `ReservationServiceTest` (3 activas → `LimiteReservasActivasException`; **mayúsculas cuentan igual** CA-R04.5); `ReservationControllerTest` (409 `active-reservation-limit`). |
| R05 | `DELETE /api/reservations/{id}`, `GET /api/reservations` | `/mis-reservas` | `ReservationServiceTest` (cancelar CONFIRMADA, inexistente, YA_CANCELADA, YA_FINALIZADA, YA_INICIADA; **listar normalizado**); `ReservationControllerTest` (DELETE 200 con DTO, 409 `cancellation-not-allowed`); `EstadoReservaCalculadorTest` (4 estados, bordes inclusivos). |
| R06 | Todos los anteriores | `/herramientas` + `/nueva-reserva` | Front: `availability-form.component.spec` (no emite sin fechas; validadores de orden, pasado y duración), `reservation-form.component.spec` (no envía vacío, crea, muestra 409 de solape), `reservation-success.component.spec`. |
| R07 | `GET /api/reservations`, `DELETE /api/reservations/{id}` | `/mis-reservas` (`MyReservationsComponent`) | Front: `my-reservations.component.spec` — correo inválido no busca, búsqueda pinta filas, lista vacía, Cancelar solo si `CONFIRMADA`, cancelación OK, `confirm=false` no llama, 409 con motivo traducido. |
| R08 | `GlobalExceptionHandler` (`@RestControllerAdvice`) | Interceptor + `ErrorBanner` | Back: vía slices `@WebMvcTest` (400, 422 con y sin `errors[]`, tres 409, `application/problem+json`). Sin test: 404, `ConstraintViolationException`, 500. Front: `problem-detail.interceptor.spec` (409, 422, 500, red, type desconocido, throw arbitrario), `error-banner.component.spec`. |
| R09 | `GET /api/tools/available` | — | **No cubierto con test** (ni perfil `perf`). Diseño: query única `NOT EXISTS` + índices (doc 03 §5.2, §6). |
