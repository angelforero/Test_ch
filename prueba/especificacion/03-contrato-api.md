# 03 — Contrato API REST

> **Versión:** 1.1 · **Fecha:** 2026-11-25 (v1.0: 2026-11-22)
> Contrato cerrado entre el frontend Angular y el backend Spring Boot. Este documento es la **fuente de verdad** para generar modelos TypeScript en el frontend y para implementar controladores en el backend.
> Cualquier cambio aquí requiere actualizar los documentos 02 y 04.
>
> **v1.1 — alineación con la implementación.** Tras implementar y verificar end-to-end (smoke tests contra el backend real), este documento se actualizó para reflejar el contrato **efectivamente implementado**. Las diferencias respecto a la v1.0 y su motivo están en el doc 01 §10 (entradas del 2026-11-25). La v1.0 original sigue disponible en el historial Git (commit `97a2469`).

---

## 1. Convenciones generales

| Aspecto | Valor |
|---------|-------|
| Base URL (local) | `http://localhost:8080` |
| Prefijo de rutas | `/api` |
| Content-Type de peticiones con body | `application/json` |
| Content-Type de respuestas OK | `application/json` |
| Content-Type de respuestas de error | `application/problem+json` |
| Codificación | UTF-8 |
| Formato de fechas | `YYYY-MM-DD` (ISO 8601, sin hora) |
| Formato de timestamps en logs | ISO 8601 con offset |
| Zona horaria para "hoy" | `Europe/Madrid` |
| Versionado | No versionado en MVP (si se añadiera, sería `/api/v1`) |
| Paginación | No aplica en MVP |
| Autenticación | Ninguna |
| CORS | No se configura en el backend: en desarrollo el frontend usa `proxy.conf.json` (`/api` → `:8080`), ver §9 |
| Correo | Se normaliza a `trim()` + minúsculas en toda entrada (body y query) |

---

## 2. Tipos y enums compartidos

### 2.1 `Categoria` (enum)

```
PERFORACION | CORTE | LIJADO | MEDICION | ESCALERAS | JARDIN | OTROS
```

### 2.2 `EstadoReserva` (enum derivado — no se persiste)

```
CONFIRMADA | EN_CURSO | FINALIZADA | CANCELADA
```

Reglas de derivación (ver documento 01, §5):

- `CANCELADA` si el campo persistido `cancelada = true`.
- Si no está cancelada:
  - `CONFIRMADA` si `fechaInicio > hoy`.
  - `EN_CURSO` si `fechaInicio ≤ hoy ≤ fechaFin`.
  - `FINALIZADA` si `fechaFin < hoy`.

### 2.3 Tipos de recurso

**`Herramienta`**
```json
{
  "id": 1,
  "nombre": "Taladro percutor Bosch GSB 13 RE",
  "categoria": "PERFORACION"
}
```

**`Reserva`**
```json
{
  "id": 42,
  "herramientaId": 1,
  "nombreHerramienta": "Taladro percutor Bosch GSB 13 RE",
  "nombre": "Ada Lovelace",
  "correo": "ada@example.com",
  "fechaInicio": "2026-12-01",
  "fechaFin": "2026-12-05",
  "estado": "CONFIRMADA",
  "cancelada": false
}
```

> `nombreHerramienta` se incluye en la respuesta por conveniencia del frontend (evita un GET adicional por cada reserva). En el request de creación solo se envía `herramientaId`.
> `cancelada` refleja el único flag persistido; `estado` es el valor derivado (§2.2). El frontend usa `estado` para decidir qué mostrar.

---

## 3. Formato uniforme de errores (RFC 7807)

Todas las respuestas 4xx y 5xx usan `application/problem+json` con esta estructura:

```json
{
  "type":   "http://localhost:8080/problems/validation-error",
  "title":  "Error de validación",
  "status": 422,
  "detail": "Hay 2 error(es) de validación en los campos enviados.",
  "errors": [
    { "campo": "nombre", "mensaje": "no debe estar vacío", "valorRechazado": "" },
    { "campo": "correo", "mensaje": "debe ser una dirección de correo electrónico con formato correcto", "valorRechazado": "no-email" }
  ]
}
```

- `type`: identificador estable del tipo de problema (ver §3.1).
- `title`: título corto en español, consistente por `type`.
- `detail`: mensaje en español apto para mostrar al usuario.
- `errors`: **opcional**, solo en errores de Bean Validation (`MethodArgumentNotValidException`, `ConstraintViolationException`). Cada entrada es `{ campo, mensaje, valorRechazado? }` (claves en español, coherentes con el resto del dominio; `valorRechazado` se omite si es `null`). Las reglas de negocio de fechas (`ReglaValidacionException`) devuelven 422 **sin** `errors[]`: el motivo va en `detail`.
- **Propiedades extra** por tipo (RFC 7807 §3.2 permite extensiones), útiles para el cliente: `herramientaId`/`fechaInicio`/`fechaFin` en `overlap-conflict`; `correo`/`activasActuales`/`maximo` en `active-reservation-limit`; `reservaId`/`motivo` (`YA_INICIADA` · `YA_FINALIZADA` · `YA_CANCELADA`) en `cancellation-not-allowed`; `herramientaId` o `reservaId` en `not-found`; `parametro` (y `valorRechazado`) en `malformed-request`.
- `instance`: **no se incluye** en la implementación actual (el handler no lo establece y Jackson omite nulos). Es opcional en RFC 7807; queda como mejora menor.

### 3.1 Catálogo cerrado de `type`

Todos los `type` son URLs completas con la base `http://localhost:8080/problems/`. En producción el host cambiaría al dominio real; en desarrollo local esta base coincide con el puerto por defecto del backend (D16). La URL **no tiene por qué resolver a documentación navegable** — el estándar RFC 7807 solo exige que sea un identificador único y estable del tipo de problema.

| `type` (URL completa) | Código | Cuándo se devuelve |
|-----------------------|--------|--------------------|
| `http://localhost:8080/problems/malformed-request` | 400 | Body JSON inválido, query param mal formado |
| `http://localhost:8080/problems/validation-error` | 422 | Fallos de Bean Validation o reglas de formato |
| `http://localhost:8080/problems/not-found` | 404 | Recurso inexistente (herramienta, reserva) |
| `http://localhost:8080/problems/overlap-conflict` | 409 | Solapamiento de reservas (R03) |
| `http://localhost:8080/problems/active-reservation-limit` | 409 | Límite de 3 reservas activas (R04) |
| `http://localhost:8080/problems/cancellation-not-allowed` | 409 | Intento de cancelar reserva ya iniciada o cancelada (R05) |
| `http://localhost:8080/problems/internal-error` | 500 | Error inesperado del servidor |

---

## 4. Endpoints

### 4.1 `GET /api/tools` — Listar catálogo

Lista todas las herramientas del catálogo, opcionalmente filtradas por categoría.

**Cubre:** CA-R01.1, CA-R01.2, CA-R01.3

**Query params**

| Nombre | Tipo | Obligatorio | Descripción |
|--------|------|-------------|-------------|
| `categoria` | `Categoria` | No | Filtra por categoría exacta. Case-sensitive. |

**Respuestas**

| Código | Cuerpo | Caso |
|--------|--------|------|
| 200 | `Herramienta[]` | OK |
| 400 | `ProblemDetail` (`malformed-request`, con `parametro` y `valorRechazado`) | `categoria` no es un valor válido del enum |

**Ejemplo — petición**
```http
GET /api/tools?categoria=PERFORACION HTTP/1.1
Accept: application/json
```

**Ejemplo — respuesta 200**
```json
[
  { "id": 1, "nombre": "Taladro percutor Bosch GSB 13 RE", "categoria": "PERFORACION" },
  { "id": 2, "nombre": "Taladro atornillador Makita DF333D", "categoria": "PERFORACION" }
]
```

---

### 4.2 `GET /api/tools/available` — Disponibilidad

Devuelve las herramientas disponibles (sin solapamientos con reservas activas) para un rango de fechas, opcionalmente filtradas por categoría.

**Cubre:** CA-R01.4, CA-R01.5, CA-R01.6, CA-R01.7 · **R03** (vista de exclusión) · **R09** (rendimiento)

**Query params**

| Nombre | Tipo | Obligatorio | Descripción |
|--------|------|-------------|-------------|
| `fechaInicio` | `LocalDate` | Sí | Formato `YYYY-MM-DD`. |
| `fechaFin` | `LocalDate` | Sí | `fechaFin ≥ fechaInicio`. |
| `categoria` | `Categoria` | No | Filtra por categoría. |

**Reglas**

- El rango se valida con **las mismas reglas que una reserva** (`ValidadorRango`, compartido con `POST /api/reservations`): `fechaFin ≥ fechaInicio`, `fechaInicio ≥ hoy` y duración `≤ 7` días. Cualquier incumplimiento → `422 validation-error`. *(v1.0 decía que aquí no se exigía `fechaInicio ≥ hoy`; se unificó para que la consulta de disponibilidad solo ofrezca rangos que después se puedan reservar — ver doc 01 §10.)*
- Se considera una herramienta **disponible** si **no tiene ninguna reserva activa** (`CONFIRMADA` o `EN_CURSO`) cuyo rango se solape con el rango consultado.

**Fórmula de solapamiento** (dos rangos `[a1, a2]` y `[b1, b2]` se solapan sii):

```
a1 <= b2  AND  b1 <= a2
```

**Respuestas**

| Código | Cuerpo | Caso |
|--------|--------|------|
| 200 | `Herramienta[]` | OK (puede ser `[]`) |
| 400 | `ProblemDetail` (`malformed-request`) | Falta `fechaInicio` o `fechaFin`, formato de fecha o `categoria` inválidos |
| 422 | `ProblemDetail` (`validation-error`) | `fechaFin < fechaInicio`, `fechaInicio < hoy` o duración > 7 días |

**Ejemplo — petición**
```http
GET /api/tools/available?fechaInicio=2026-12-01&fechaFin=2026-12-05&categoria=LIJADO HTTP/1.1
```

**Ejemplo — respuesta 200**
```json
[
  { "id": 7, "nombre": "Lijadora orbital Makita BO5041", "categoria": "LIJADO" }
]
```

**Ejemplo — respuesta 422**
```json
{
  "type": "http://localhost:8080/problems/validation-error",
  "title": "Error de validación",
  "status": 422,
  "detail": "Rango inválido: fechaFin 2026-12-10 es anterior a fechaInicio 2026-12-12"
}
```

---

### 4.3 `POST /api/reservations` — Crear reserva

**Cubre:** R02, R03, R04

**Request body**

```json
{
  "herramientaId": 1,
  "nombre": "Ada Lovelace",
  "correo": "ada@example.com",
  "fechaInicio": "2026-12-01",
  "fechaFin": "2026-12-05"
}
```

**Validaciones**

| Campo | Regla |
|-------|-------|
| `herramientaId` | obligatorio (`@NotNull`) |
| `nombre` | obligatorio, `@NotBlank`, longitud ≤ 120 |
| `correo` | obligatorio, `@Email`, longitud ≤ 254. Se normaliza a `trim().toLowerCase()` **al deserializar, antes de Bean Validation** (así `"  Ada@Example.COM  "` es válido) |
| `fechaInicio` | obligatoria, no anterior a hoy |
| `fechaFin` | obligatoria, `≥ fechaInicio`, `≤ fechaInicio + 6 días` (duración máx. 7 días inclusivo) |

**Reglas de negocio** (validadas tras Bean Validation, dentro de una transacción `@Transactional`), **en este orden**:

1. Reglas de fechas (`ValidadorRango`): orden, no pasado, duración ≤ 7 → `422 validation-error`.
2. La `herramientaId` debe existir → si no, `404 not-found`.
3. El correo (normalizado) no debe tener ya ≥ 3 reservas activas → si las tiene, `409 active-reservation-limit`.
4. No debe solapar con reservas no canceladas de esa herramienta → si solapa, `409 overlap-conflict`.

> El límite (3) se evalúa **antes** que el solapamiento (4) — al revés que en la v1.0 — para dar el mensaje más accionable ("ya tienes 3 reservas") cuando el socio está saturado. Ambos son 409; el cliente los distingue por `type`.

**Respuestas**

| Código | Cuerpo | Headers | Caso |
|--------|--------|---------|------|
| 201 | `Reserva` | `Location: <base>/api/reservations/{id}` (URL absoluta) | Creada |
| 400 | `ProblemDetail` (`malformed-request`) | — | Body no parseable |
| 422 | `ProblemDetail` (`validation-error`) con `errors[]` | — | Bean Validation falla |
| 422 | `ProblemDetail` (`validation-error`) sin `errors[]` | — | Regla de fechas (orden, pasado, duración) |
| 404 | `ProblemDetail` (`not-found`) | — | Herramienta inexistente |
| 409 | `ProblemDetail` (`overlap-conflict`) | — | Solapamiento |
| 409 | `ProblemDetail` (`active-reservation-limit`) | — | ≥ 3 reservas activas del correo |

**Ejemplo — respuesta 201**
```json
{
  "id": 42,
  "herramientaId": 1,
  "nombreHerramienta": "Taladro percutor Bosch GSB 13 RE",
  "nombre": "Ada Lovelace",
  "correo": "ada@example.com",
  "fechaInicio": "2026-12-01",
  "fechaFin": "2026-12-05",
  "estado": "CONFIRMADA",
  "cancelada": false
}
```

**Ejemplo — respuesta 409 (solapamiento)**
```json
{
  "type": "http://localhost:8080/problems/overlap-conflict",
  "title": "Reserva solapada",
  "status": 409,
  "detail": "Ya existe una reserva activa para esta herramienta que se solapa con el rango solicitado.",
  "herramientaId": 1,
  "fechaInicio": "2026-12-01",
  "fechaFin": "2026-12-05"
}
```

**Ejemplo — respuesta 409 (límite de activas)**
```json
{
  "type": "http://localhost:8080/problems/active-reservation-limit",
  "title": "Límite de reservas activas alcanzado",
  "status": 409,
  "detail": "El correo 'ada@example.com' ya tiene 3 reservas activas y el máximo permitido es 3.",
  "correo": "ada@example.com",
  "activasActuales": 3,
  "maximo": 3
}
```

---

### 4.4 `GET /api/reservations` — Consultar reservas de una persona

**Cubre:** CA-R05.7, CA-R05.8, CA-R05.9

**Query params**

| Nombre | Tipo | Obligatorio | Descripción |
|--------|------|-------------|-------------|
| `correo` | string | Sí | `@NotBlank` + `@Email`. El servicio lo normaliza (minúsculas) antes de consultar, así `ADA@EXAMPLE.COM` encuentra las reservas de `ada@example.com`. |

**Reglas**

- Devuelve **todas** las reservas del correo (incluidas `CANCELADA` y `FINALIZADA`), ordenadas por `fechaInicio DESC`.
- El `estado` se deriva en tiempo de respuesta.

**Respuestas**

| Código | Cuerpo | Caso |
|--------|--------|------|
| 200 | `Reserva[]` | OK (puede ser `[]`) |
| 400 | `ProblemDetail` (`malformed-request`) | Falta `correo` |
| 422 | `ProblemDetail` (`validation-error`) con `errors[]` | `correo` con formato inválido |

**Ejemplo — petición**
```http
GET /api/reservations?correo=ada@example.com HTTP/1.1
```

**Ejemplo — respuesta 200**
```json
[
  {
    "id": 42, "herramientaId": 1, "nombreHerramienta": "Taladro percutor Bosch GSB 13 RE",
    "nombre": "Ada Lovelace", "correo": "ada@example.com",
    "fechaInicio": "2026-12-10", "fechaFin": "2026-12-12", "estado": "CONFIRMADA", "cancelada": false
  },
  {
    "id": 30, "herramientaId": 7, "nombreHerramienta": "Lijadora orbital Makita BO5041",
    "nombre": "Ada Lovelace", "correo": "ada@example.com",
    "fechaInicio": "2026-11-05", "fechaFin": "2026-11-07", "estado": "FINALIZADA", "cancelada": false
  }
]
```

---

### 4.5 `DELETE /api/reservations/{id}` — Cancelar reserva

**Cubre:** R05 (CA-R05.1 a CA-R05.6)

**Path params**

| Nombre | Tipo | Descripción |
|--------|------|-------------|
| `id` | `long` | Identificador de la reserva |

**Reglas**

- Solo se cancela si:
  - La reserva existe (si no, `404`).
  - El estado derivado es `CONFIRMADA` **y** `fechaInicio > hoy` (si no, `409 cancellation-not-allowed`).
- La operación marca `cancelada = true` (no borra el registro).

**Respuestas**

| Código | Cuerpo | Caso |
|--------|--------|------|
| 200 | `Reserva` (con `estado: "CANCELADA"`, `cancelada: true`) | Cancelación OK |
| 404 | `ProblemDetail` (`not-found`) | Reserva inexistente |
| 409 | `ProblemDetail` (`cancellation-not-allowed`, con `motivo`) | Ya iniciada, finalizada o cancelada |

> **v1.0 especificaba `204 No Content`.** Se implementó `200 OK` con el DTO actualizado para que el frontend sustituya la fila sin un segundo GET (CA-R07.2). Ver doc 01 §10.

**Ejemplo — respuesta 409**
```json
{
  "type": "http://localhost:8080/problems/cancellation-not-allowed",
  "title": "Cancelación no permitida",
  "status": 409,
  "detail": "No se puede cancelar: la reserva ya está cancelada.",
  "reservaId": 42,
  "motivo": "YA_CANCELADA"
}
```

`detail` según `motivo`: `YA_INICIADA` → "…la reserva ya ha comenzado."; `YA_FINALIZADA` → "…la reserva ya ha finalizado."; `YA_CANCELADA` → "…la reserva ya está cancelada.".

---

## 5. Modelo de datos (backend / H2)

### 5.1 Tabla `herramientas`

| Columna | Tipo | Restricciones |
|---------|------|---------------|
| `id` | `BIGINT` | PK, auto |
| `nombre` | `VARCHAR(120)` | `NOT NULL` |
| `categoria` | `VARCHAR(20)` | `NOT NULL`, enum |

Índices: PK por defecto.

### 5.2 Tabla `reservas`

| Columna | Tipo | Restricciones |
|---------|------|---------------|
| `id` | `BIGINT` | PK, auto |
| `herramienta_id` | `BIGINT` | FK → `herramientas.id`, `NOT NULL` |
| `nombre` | `VARCHAR(120)` | `NOT NULL` |
| `correo` | `VARCHAR(254)` | `NOT NULL`, normalizado a minúsculas |
| `fecha_inicio` | `DATE` | `NOT NULL` |
| `fecha_fin` | `DATE` | `NOT NULL`, `>= fecha_inicio` |
| `cancelada` | `BOOLEAN` | `NOT NULL DEFAULT FALSE` |

**Índices (R09):**
- `idx_reservas_herramienta` sobre `(herramienta_id)`
- `idx_reservas_rango` sobre `(herramienta_id, fecha_inicio, fecha_fin)`
- `idx_reservas_correo` sobre `(correo)`

**Constraint declarativa (opcional, documental):**
```sql
CHECK (fecha_fin >= fecha_inicio)
```

### 5.3 Datos semilla (R01)

Al arrancar, un `CommandLineRunner` carga ≥ 10 herramientas si la tabla está vacía. Ejemplos:

| id | nombre | categoria |
|----|--------|-----------|
| 1 | Taladro percutor Bosch GSB 13 RE | PERFORACION |
| 2 | Taladro atornillador Makita DF333D | PERFORACION |
| 3 | Sierra circular Einhell TE-CS 190 | CORTE |
| 4 | Cizalla de palanca Stanley | CORTE |
| 5 | Lijadora orbital Makita BO5041 | LIJADO |
| 6 | Lijadora de banda Bosch PBS 75 A | LIJADO |
| 7 | Nivel láser Bosch GLL 2-10 | MEDICION |
| 8 | Escalera telescópica 3,8 m | ESCALERAS |
| 9 | Desbrozadora Stihl FS 55 | JARDIN |
| 10 | Multiherramienta oscilante Dewalt | OTROS |

---

## 6. Consulta de disponibilidad (query de referencia)

Para cumplir R09 (<500 ms con 5.000 reservas) la disponibilidad debe resolverse en **una sola query**. Implementación recomendada:

```sql
SELECT h.id, h.nombre, h.categoria
FROM herramientas h
WHERE (:categoria IS NULL OR h.categoria = :categoria)
  AND NOT EXISTS (
    SELECT 1
    FROM reservas r
    WHERE r.herramienta_id = h.id
      AND r.cancelada = FALSE
      AND r.fecha_inicio <= :fechaFin
      AND r.fecha_fin    >= :fechaInicio
  );
```

- Se apoya en `idx_reservas_rango`.
- No carga reservas a memoria.
- La condición `cancelada = FALSE` excluye reservas CANCELADAS; las FINALIZADAS no se excluyen explícitamente porque, si consultas un rango futuro, su `fecha_fin < :fechaInicio` las descarta sola.

---

## 6-bis. Manejo de excepciones backend (D7 + D7-bis)

**Regla dura:** ningún controlador hace `try/catch` para traducir errores a HTTP. Toda respuesta de error se produce en **un único `@RestControllerAdvice`** llamado `GlobalExceptionHandler`, con un `@ExceptionHandler` por excepción.

### 6-bis.1 Excepciones personalizadas del dominio

Lanzadas por los **servicios** (`ToolService`, `ReservationService`). Nunca por controladores.

| Excepción (package `com.ch.prueba.herramientas.exceptions`) | Lanzada cuando | HTTP | `type` del `ProblemDetail` (slug tras `/problems/`) |
|-------------------------------------------------------------|----------------|------|------------------------------------------------------|
| `HerramientaNoEncontradaException` | El `herramientaId` no existe | 404 | `not-found` |
| `ReservaNoEncontradaException` | La `reservaId` no existe | 404 | `not-found` |
| `SolapamientoReservaException` | Nueva reserva solapa con otra activa (R03) | 409 | `overlap-conflict` |
| `LimiteReservasActivasException` | El correo ya tiene ≥ 3 activas (R04) | 409 | `active-reservation-limit` |
| `CancelacionNoPermitidaException` | Intento de cancelar reserva iniciada/finalizada/cancelada (R05) | 409 | `cancellation-not-allowed` |
| `ReglaValidacionException` (base, subpaquete `com.ch.prueba.herramientas.exceptions.reglas` con subclases `FechaEnPasadoException`, `DuracionExcesivaException`, `RangoFechasInvalidoException`) | Reglas de negocio sobre fechas (R02) | 422 | `validation-error` |

Todas heredan de una raíz común `DomainException` (en el mismo paquete) para que el handler pueda agruparlas si conviene. Cada excepción lleva en su constructor los datos necesarios para componer `detail` (ej. `new SolapamientoReservaException(herramientaId, fechaInicio, fechaFin)`) y el handler compone el mensaje legible. Excepción a esta regla: las subclases de `ReglaValidacionException` sí construyen su propio mensaje en español (p. ej. "Duración 8 días supera el máximo permitido de 7"), que el handler usa como `detail` tal cual.

### 6-bis.2 Excepciones Spring mapeadas por el handler

Sin crear excepciones propias, el `GlobalExceptionHandler` también maneja:

| Excepción Spring | HTTP | `type` |
|------------------|------|--------|
| `MethodArgumentNotValidException` (Bean Validation en `@RequestBody`) | 422 | `validation-error` + `errors[]` con `{campo, mensaje, valorRechazado}` por cada `FieldError` |
| `ConstraintViolationException` (Bean Validation en `@RequestParam` / `@PathVariable`) | 422 | `validation-error` + `errors[]` |
| `HttpMessageNotReadableException` | 400 | `malformed-request` |
| `MissingServletRequestParameterException` | 400 | `malformed-request` |
| `MethodArgumentTypeMismatchException` (ej. enum o fecha mal formada en query) | 400 | `malformed-request` |
| `Exception` (fallback) | 500 | `internal-error`. Se registra el stack trace en logs; **no** se expone al cliente. |

### 6-bis.3 Esqueleto del handler

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(HerramientaNoEncontradaException.class)
  public ProblemDetail handleHerramientaNoEncontrada(HerramientaNoEncontradaException ex) { ... }

  @ExceptionHandler(SolapamientoReservaException.class)
  public ProblemDetail handleSolapamiento(SolapamientoReservaException ex) { ... }

  // ... un método por cada excepción de la tabla 6-bis.1

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleBeanValidation(MethodArgumentNotValidException ex) { ... }

  // ... un método por cada excepción de la tabla 6-bis.2

  @ExceptionHandler(Exception.class)
  public ProblemDetail handleUnexpected(Exception ex) { ... }
}
```

Cada handler devuelve directamente un `ProblemDetail` (soporte nativo desde Spring Boot 3, incluye 4.x) con el status, `type`, `title`, `detail` y, cuando aplique, `errors[]`.

> **Testing (D19):** el plan v1.0 preveía un `GlobalExceptionHandlerTest` dedicado. **No se implementó**: el handler se ejercita a través de los slices `@WebMvcTest` (`ToolControllerTest`, `ReservationControllerTest`), que importan el handler real y verifican status, `type` y `application/problem+json` para 400 `malformed-request`, 422 `validation-error` (con y sin `errors[]`), 409 `overlap-conflict`, 409 `active-reservation-limit` y 409 `cancellation-not-allowed`. Quedan sin test automatizado los mapeos 404 `not-found`, `ConstraintViolationException` y el fallback 500 (verificados manualmente en el smoke test). Ver doc 02, matriz de trazabilidad.

---

## 7. Concurrencia (R03 — CA-R03.7)

Riesgo: dos POST simultáneos para la misma herramienta y mismo rango podrían crear dos reservas si el chequeo de solapamiento ocurre antes de persistir.

**Implementado (MVP):** `ReservationService.crear(...)` es `@Transactional` (aislamiento por defecto, `READ_COMMITTED` en H2) y comprueba el solapamiento con `findSolapadas(...)` dentro de la transacción, justo antes de guardar. Esto cubre el caso secuencial (CA-R03.1–R03.6), pero **no** garantiza exclusión entre dos transacciones simultáneas: ambas podrían leer "sin solape" antes de que la otra haga commit.

**Estrategia prevista (no implementada — deuda técnica aceptada, coherente con S9):**

1. Dentro de la misma transacción, bloquear pesimísticamente la herramienta: `SELECT ... FROM herramientas WHERE id = :id FOR UPDATE` (en JPA: `@Lock(LockModeType.PESSIMISTIC_WRITE)` en un método de `HerramientaRepository`).
2. Volver a comprobar el solapamiento y guardar.
3. La segunda transacción espera al commit de la primera; al continuar ve la reserva existente y lanza `SolapamientoReservaException` (→ `409 overlap-conflict`).

> **Por qué no se implementó:** sin un test multi-hilo (`@DataJpaTest`/`@SpringBootTest` + ejecutores concurrentes, fuera de D19) el bloqueo no se podría demostrar, y S9 asume que no hay concurrencia hostil. La v1.0 lo describía como si estuviera implementado; se corrige para no sobredeclarar. Es la primera mejora a abordar si el sistema pasara a uso real.

---

## 8. OpenAPI / Swagger

El backend expone **springdoc-openapi 3.2.1** (línea 3.x, la compatible con Spring Boot 4; la 2.6.0 inicial devolvía 500 en `/v3/api-docs`):

- OpenAPI JSON: `GET /v3/api-docs` (OpenAPI 3.1.0)
- Swagger UI: `GET /swagger-ui.html`

El contrato generado debe coincidir con este documento. Si hay discrepancia, **este documento manda** y se corrige el código (la v1.1 de este documento ya refleja el código verificado).

---

## 9. CORS

**No se configura CORS en el backend.** En desarrollo, Angular sirve todo bajo `http://localhost:4200` y `proxy.conf.json` reenvía `/api/*` a `http://localhost:8080`, de modo que el navegador nunca hace peticiones cross-origin (D16).

La v1.0 preveía además un `CorsRegistry` para `http://localhost:4200` en el perfil `dev`. Se descartó: con el proxy es redundante y añade superficie de configuración. Si el frontend se desplegara en otro origen, habría que añadirlo (o un `@CrossOrigin` acotado).

---

## 10. Cambios futuros previstos (no implementar en MVP)

- `PATCH /api/reservations/{id}` para modificar fechas.
- `GET /api/reservations/{id}` para detalle individual.
- Paginación en `GET /api/reservations` si una persona pudiera tener muchas históricas.
- Autenticación (JWT) + endpoint de usuarios.
