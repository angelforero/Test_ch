# 02 — Requisitos y Criterios de Aceptación

> **Versión:** 1.0 · **Fecha:** 2026-11-22
> Reescritura detallada de los 9 requisitos del enunciado (R01–R09) con criterios de aceptación en formato **Gherkin** (Given/When/Then), casos borde y trazabilidad a endpoints y pantallas.
> Las referencias `[API: …]` apuntan al documento `03-contrato-api.md` y `[UI: …]` al `04-arquitectura-frontend.md`.

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
**Pantallas relacionadas:** `[UI: BuscarYReservarPage]`

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
And el body es un ProblemDetail con type "validation-error"
```

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
Then la respuesta es 400 Bad Request
And el ProblemDetail indica "fechaFin debe ser mayor o igual a fechaInicio"
```

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
**Pantallas relacionadas:** `[UI: BuscarYReservarPage → modal/acción Reservar]`

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
**Pantallas relacionadas:** `[UI: MisReservasPage]`

### Criterios de aceptación

**CA-R05.1 — Cancelar con antelación**
```gherkin
Given hoy es 2026-11-22
And existe una reserva R1 con fechaInicio = 2026-11-25
When se hace DELETE /api/reservations/R1
Then la respuesta es 204 No Content
And el estado de R1 pasa a CANCELADA
```

**CA-R05.2 — Cancelar el día anterior al inicio (permitido)**
```gherkin
Given hoy es 2026-11-24
And R1 tiene fechaInicio = 2026-11-25
When se hace DELETE /api/reservations/R1
Then la respuesta es 204 No Content
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

---

## R06 — Pantalla de búsqueda y reserva

**Descripción.** La interfaz permite elegir un rango de fechas, ver las herramientas disponibles (con filtro por categoría) y reservar una de ellas, mostrando los errores del API con claridad.

**Pantallas relacionadas:** `[UI: BuscarYReservarPage]`

### Criterios de aceptación

**CA-R06.1 — Flujo feliz**
```gherkin
Given el usuario está en la pantalla "Buscar y reservar"
When selecciona fechaInicio = 2026-12-01, fechaFin = 2026-12-03 y pulsa "Buscar"
Then se listan las herramientas disponibles en ese rango
```

**CA-R06.2 — Filtro por categoría**
```gherkin
Given hay resultados visibles
When el usuario elige categoría "LIJADO" en el selector
Then la lista se filtra para mostrar solo herramientas LIJADO disponibles
```

**CA-R06.3 — Validación local antes de llamar al API**
```gherkin
Given el usuario introduce fechaInicio > fechaFin
When pulsa "Buscar"
Then no se hace llamada al API
And se muestra un mensaje de validación inline
```

**CA-R06.4 — Reservar desde la lista**
```gherkin
Given hay herramientas visibles
When el usuario pulsa "Reservar" en una herramienta e introduce nombre y correo válidos
Then se hace POST /api/reservations
And al recibir 201 se muestra confirmación visible ("Reserva #123 creada")
And la herramienta reservada desaparece o se marca como no disponible en la lista
```

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

---

## R07 — Pantalla "mis reservas"

**Descripción.** La interfaz permite a una persona consultar sus reservas por correo y cancelarlas.

**Pantallas relacionadas:** `[UI: MisReservasPage]`

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
When pulsa "Cancelar" y confirma la acción
Then se hace DELETE /api/reservations/{id}
And al recibir 204 la reserva pasa a CANCELADA en la lista sin recargar
```

**CA-R07.3 — Botón deshabilitado si no se puede cancelar**
```gherkin
Given una reserva está EN_CURSO, FINALIZADA o CANCELADA
Then el botón "Cancelar" está deshabilitado o no se muestra
```

**CA-R07.4 — Lista vacía**
```gherkin
Given el correo introducido no tiene reservas
Then se muestra un mensaje "No hay reservas para este correo"
```

**CA-R07.5 — Correo malformado**
```gherkin
When el usuario introduce un correo con formato inválido
Then se muestra validación inline y no se llama al API
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
  { type, title, status, detail, instance, errors? }
```

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
And cada entrada tiene { field, message }
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

---

## Matriz de trazabilidad

> Estrategia de pruebas: **solo tests unitarios con mocks**, cobertura máxima 75 % (ver D19 en doc 01). Se permite `@SpringBootTest` y slices; **no** hay E2E ni integración multi-servicio. Los tests listados son los **unitarios clave** que validan cada requisito — no una enumeración exhaustiva.

| Requisito | Endpoints API | Pantallas UI | Tests unitarios clave (con mocks) |
|-----------|---------------|--------------|-----------------------------------|
| R01 | `GET /api/tools`, `GET /api/tools/available` | BuscarYReservarPage | `ToolServiceTest` (filtro por categoría, consulta de disponibilidad con repo mockeado); `DataSeederTest` (carga ≥ 10 herramientas al arranque en BD vacía) |
| R02 | `POST /api/reservations` | BuscarYReservarPage | `ReservationServiceTest` (fecha en el pasado → `ReglaValidacionException`; duración > 7 días → `ReglaValidacionException`); `ReservationControllerTest` con `@WebMvcTest` y `MockMvc` + servicio mockeado |
| R03 | `POST /api/reservations`, `GET /api/tools/available` | (implícito en disponibilidad) | `ReservationServiceTest` (6 casos de solapamiento 1-6 con repositorio mockeado → `SolapamientoReservaException`); CA-R03.7 (concurrencia) **se documenta pero no se cubre con test** |
| R04 | `POST /api/reservations` | (error visible en BuscarYReservarPage) | `ReservationServiceTest` (contador de activas con repo mockeado; normalización del correo → `LimiteReservasActivasException`) |
| R05 | `DELETE /api/reservations/{id}`, `GET /api/reservations` | MisReservasPage | `ReservationServiceTest` (cancelación válida, cancelación el mismo día → `CancelacionNoPermitidaException`, reserva ya cancelada); `EstadoReservaCalculadorTest` (derivación de los 4 estados) |
| R06 | Todos los anteriores | BuscarYReservarPage | **Frontend unitario**: `BuscarReservarPage.spec` con `HttpTestingController` (happy path + manejo de 409) |
| R07 | `GET /api/reservations`, `DELETE /api/reservations/{id}` | MisReservasPage | **Frontend unitario**: `MisReservasPage.spec` con `HttpTestingController` (happy path + cancelación) |
| R08 | `@RestControllerAdvice` global (`GlobalExceptionHandler`) | Interceptor HTTP + `ErrorBanner` | `GlobalExceptionHandlerTest` (un `@Test` por cada excepción personalizada y por cada excepción Spring → verifica status, `type`, formato `ProblemDetail`); `ProblemDetailInterceptorSpec` en frontend |
| R09 | `GET /api/tools/available` | — | **No se cubre con test unitario** (es un no funcional de latencia que requiere carga). El doc 03 §6 documenta la query y los índices que dan cumplimiento. |
