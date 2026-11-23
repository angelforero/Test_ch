# 01 — Alcance y Suposiciones

> **Versión:** 1.0 · **Fecha:** 2026-11-22 · **Autor:** Equipo de desarrollo
> Este documento encuadra la solución antes de escribir código. Las decisiones aquí tomadas son el marco para los documentos 02 (requisitos), 03 (contrato API) y 04 (arquitectura frontend).

---

## 1. Dominio del problema

Una **biblioteca comunitaria de barrio** presta herramientas (taladros, escaleras, lijadoras, etc.) a sus socios. Hoy la gestión se lleva en una hoja de cálculo compartida y se busca reemplazarla por una aplicación web.

Un **socio** se identifica por su **nombre** y **correo electrónico** (no hay registro previo ni contraseñas: el correo actúa como identificador funcional).

Una **reserva** ocupa una herramienta durante un rango de fechas (inicio y fin, ambas incluidas).

---

## 2. Alcance

### 2.1 Dentro del alcance (MVP)

- API REST en Java para gestionar catálogo, disponibilidad y reservas.
- Aplicación web Angular con **dos pantallas**:
  1. **Buscar y reservar** — explorar herramientas disponibles por rango de fechas y categoría, y crear una reserva.
  2. **Mis reservas** — consultar las reservas de un socio por correo y cancelarlas.
- Datos semilla: al menos **10 herramientas** precargadas al arrancar.
- Validación de entradas en API y UI; respuestas de error con formato uniforme.
- Pruebas **unitarias** automatizadas de la lógica de negocio crítica, con **cobertura máxima del 30 %** (ver D19). No se incluyen pruebas E2E ni pruebas de integración que involucren varios servicios reales simultáneamente.

### 2.2 Fuera del alcance (explícito)

- **Autenticación / autorización.** No hay login, roles ni gestión de usuarios. El correo es un identificador, no una credencial.
- **Notificaciones** (email, SMS, push).
- **Pagos, fianzas o depósitos.**
- **Gestión administrativa** (alta/baja de herramientas, reportes, backoffice).
- **Historial de préstamos devueltos** más allá del estado de la reserva.
- **Internacionalización** (todo en español, zona horaria única).
- **Despliegue en la nube, CI/CD, contenedores.** Se ejecuta en local.
- **Recuperación ante fallos / alta disponibilidad.**

---

## 3. Decisiones técnicas

| # | Área | Decisión | Justificación |
|---|------|----------|---------------|
| D1 | Lenguaje backend | **Java 17** (LTS) | Requisito del enunciado; LTS estable y ampliamente soportada. |
| D2 | Framework backend | **Spring Boot 4.2.0** | Versión servida por Spring Initializr al momento del scaffold (la rama 3.x ya no se ofrece desde el endpoint). Soporta Java 17 como mínimo, coherente con D1. Decisión revisada el 2026-11-23 respecto al plan inicial (3.3.x); detalle en §10 "Historial de decisiones". Ecosistema maduro para REST + JPA + Validation + Testing. |
| D3 | Build backend | **Maven** | Estándar de facto, integración directa con Spring Initializr y con IDEs. |
| D4 | Persistencia | **H2 en memoria** (modo `MemoryDB`) | Cero fricción de instalación; suficiente para la prueba y para cargar 200 herramientas + 5.000 reservas del requisito R09. |
| D5 | ORM | **Spring Data JPA + Hibernate** | Estándar con Spring Boot; permite consultas derivadas y JPQL para el chequeo de solapamientos. |
| D6 | Validación | **Jakarta Bean Validation** (`@Valid`, `@NotBlank`, `@Email`, `@Future`, etc.) + `@ControllerAdvice` para formato uniforme | Integración nativa con Spring MVC. |
| D7 | Formato de errores | **RFC 7807 — Problem Details for HTTP APIs** (`application/problem+json`), gestionado **exclusivamente de forma centralizada** mediante un único `@RestControllerAdvice` global (`GlobalExceptionHandler`) que mapea **excepciones personalizadas de dominio** a respuestas HTTP. | Estándar, soportado nativamente desde Spring Boot 3 (incluye 4.x) vía la clase `ProblemDetail`. La centralización elimina `try/catch` + `ResponseEntity` de los controladores y garantiza coherencia. Ver detalle en D7-bis y en el doc 03 §6-bis. |
| D7-bis | Excepciones personalizadas | Jerarquía propia bajo un paquete `exceptions/`, lanzada por los **servicios** (no por los controladores). El `GlobalExceptionHandler` tiene un `@ExceptionHandler` por cada una. Los controladores **nunca** hacen `try/catch` para traducir errores a HTTP. | Los servicios expresan intención de negocio ("no se puede cancelar") en vez de acoplarse a HTTP. El mapeo a código y `ProblemDetail` ocurre en un único punto. |
| D8 | Documentación API | **springdoc-openapi** (Swagger UI en `/swagger-ui.html`) | OpenAPI 3 autogenerado; útil para que el frontend consuma el contrato. |
| D9 | Framework frontend | **Angular 19** (ya scaffoldeado) | Requisito del enunciado; standalone components, control flow nuevo (`@if`, `@for`). |
| D10 | HTTP en frontend | `HttpClient` + `provideHttpClient(withFetch())` | API nativa de Angular, sin librerías extra. |
| D11 | Estado frontend | **Signals** + servicios inyectables | Angular 19 trae signals estables; no se necesita NgRx para este alcance. |
| D12 | Estilos | **CSS plano** en los componentes (sin librería UI pesada) | Prioridad en funcionalidad; se puede añadir Angular Material como mejora. |
| D13 | Testing backend | **JUnit 5 + Spring Boot Test + MockMvc** | Stack por defecto de Spring Boot. |
| D14 | Testing frontend | **Jasmine + Karma** (ya configurado por el CLI) | No se cambia el scaffold. |
| D15 | Nombre de la carpeta frontend | Se mantiene **`todo-app`** (nombre del scaffold inicial) | Renombrar implica reconfigurar `angular.json`, rutas de `dist/`, imports. El riesgo no compensa en una prueba de 4 h. Se documenta como deuda técnica. |
| D16 | Puertos | Backend `:8080`, frontend `:4200` | Defaults; el frontend usa `proxy.conf.json` para redirigir `/api` al backend y evitar CORS. |
| D17 | Zona horaria | **Europe/Madrid** fija, sin `TIME`, solo `LocalDate` | Las reservas son por días completos; no hay horas. Elimina ambigüedad de husos. |
| D18 | Idempotencia | No se requiere en MVP | Se declara explícitamente para no diseñar de más. |
| D19 | Estrategia de testing | **Solo tests unitarios**, con **cobertura máxima del 30 %** medida con **JaCoCo** (backend) y Karma coverage (frontend). Se permite `@SpringBootTest` y slices (`@WebMvcTest`, `@DataJpaTest`) cuando la unidad lo justifique. **Uso obligatorio de mocks** (Mockito: `@Mock`/`@MockBean`) para aislar la unidad bajo prueba: tests de servicio mockean el repositorio, tests de controlador mockean el servicio. En el frontend, `HttpTestingController` para servicios e interceptor, `TestBed` con dependencias mockeadas para componentes. No hay E2E ni tests de integración multi-servicio. **Exclusiones del check de cobertura** (no cuentan contra el 30 %, pero sí aparecen en el reporte completo): `HerramientasApplication`, paquete `config/**` (bootstrap: seeder, configuraciones), paquete `domain/**` (entidades JPA y enums — son POJOs sin lógica, cualquier instanciación al arrancar los inflaría artificialmente), paquete `web/dto/**` (DTOs y mappers). Lo relevante para el 30 % son `service/**` + `web/controller/**` + `web/advice/**`. | Prueba de 4 h: se prioriza probar la **lógica de negocio** (solapamiento, límite de 3 activas, duración ≤ 7 días, derivación de estado, cancelación en ventana) y el **mapeo del `GlobalExceptionHandler`**, no el 100 % del código. El umbral máximo de 30 % se configura en JaCoCo (`MAXIMUM 0.30`) para evitar sobre-testing. |

---

## 4. Suposiciones

Las siguientes afirmaciones se dan por ciertas; si alguna cambia, la solución se revisa.

| # | Suposición | Impacto si no se cumple |
|---|------------|-------------------------|
| S1 | El correo electrónico **identifica unívocamente** a un socio. No hay dos personas distintas con el mismo correo. | Si no se cumple, el límite de 3 reservas por persona (R04) perdería sentido. |
| S2 | El **nombre** del socio puede cambiar entre reservas (p. ej. errores tipográficos); prevalece el último proporcionado. Se guarda tal cual en cada reserva pero no se mantiene una tabla "socio" normalizada. | Permite simplificar el modelo (no hay tabla `Socio`, solo campos en `Reserva`). |
| S3 | El **catálogo de herramientas es estático** durante la ejecución (se precarga al arrancar y no se añaden/retiran en caliente desde la UI). | Si se requiriera CRUD de catálogo, haría falta otro conjunto de endpoints y pantalla de admin. |
| S4 | Una reserva en estado **CANCELADA** no cuenta para el límite de 3 activas (R04) ni para el chequeo de solapamiento (R03). | Clarifica qué significa "activa". |
| S5 | Una reserva **ya finalizada** (fecha de fin < hoy) tampoco cuenta como "activa". Se marca con estado derivado **FINALIZADA**. | Idem S4. |
| S6 | "Hoy" se calcula con la zona horaria **Europe/Madrid** del servidor. El cliente muestra fechas en el mismo huso. | Coherencia entre validaciones de API y UI. |
| S7 | El rango de fechas de una reserva es **inclusivo en ambos extremos**: una reserva del 1 al 3 ocupa los días 1, 2 y 3 (3 días). | Afecta al cálculo de duración máxima (7 días = fin − inicio + 1 ≤ 7). |
| S8 | La cancelación es permitida **hasta el día anterior al inicio**. Si hoy = fecha de inicio o posterior, no se puede cancelar. | Criterio de R05. |
| S9 | No hay **concurrencia hostil** (dos usuarios reservando la misma herramienta en el mismo instante). Aun así, la validación de solapamiento se hace a nivel de base de datos con transacción para evitar carreras. | Robustez básica suficiente para la prueba. |
| S10 | El volumen de carga para medir R09 (200 herramientas, 5.000 reservas) se genera con un script o seeder en perfil `perf`. | Permite demostrar el cumplimiento del requisito no funcional. |

---

## 4-bis. Detalle de D7-bis — Excepciones personalizadas previstas

Lista cerrada que los servicios lanzarán y que el `GlobalExceptionHandler` mapeará a `ProblemDetail`. Sus nombres y códigos HTTP son el contrato interno del backend; el contrato externo (slugs del `type`) vive en el doc 03.

| Excepción | Lanza | HTTP | `type` en ProblemDetail |
|-----------|-------|------|-------------------------|
| `HerramientaNoEncontradaException` | `ToolService` | 404 | `not-found` |
| `ReservaNoEncontradaException` | `ReservationService` | 404 | `not-found` |
| `SolapamientoReservaException` | `ReservationService` | 409 | `overlap-conflict` |
| `LimiteReservasActivasException` | `ReservationService` | 409 | `active-reservation-limit` |
| `CancelacionNoPermitidaException` | `ReservationService` | 409 | `cancellation-not-allowed` |
| `ReglaValidacionException` (base para duración > 7 días, fechaInicio en el pasado, rango inválido, etc.) | servicios | 422 | `validation-error` |

Adicionalmente, el `GlobalExceptionHandler` maneja — **sin crear excepciones propias** — las que ya lanza Spring:

| Excepción Spring | HTTP | `type` |
|------------------|------|--------|
| `MethodArgumentNotValidException` (Bean Validation en `@RequestBody`) | 422 | `validation-error` (con `errors[]`) |
| `ConstraintViolationException` (Bean Validation en query/path params) | 422 | `validation-error` |
| `HttpMessageNotReadableException` (JSON malformado) | 400 | `malformed-request` |
| `MissingServletRequestParameterException` (query param obligatorio ausente) | 400 | `malformed-request` |
| `MethodArgumentTypeMismatchException` (enum/fecha mal formada) | 400 | `malformed-request` |
| `Exception` (fallback) | 500 | `internal-error` (nunca expone stack trace al cliente) |

---

## 5. Estados de una reserva

Se define un vocabulario cerrado para el estado de una reserva:

| Estado | Condición | ¿Cuenta como activa? | ¿Bloquea solapamiento? | ¿Cancelable? |
|--------|-----------|----------------------|------------------------|--------------|
| `CONFIRMADA` | `fechaInicio > hoy` y no cancelada | Sí | Sí | Sí (hasta `fechaInicio − 1`) |
| `EN_CURSO` | `fechaInicio ≤ hoy ≤ fechaFin` y no cancelada | Sí | Sí | No |
| `FINALIZADA` | `fechaFin < hoy` y no cancelada | No | No | No |
| `CANCELADA` | Cancelada antes de `fechaInicio` | No | No | No (ya lo está) |

> El estado **no se almacena como columna mutable**: se **deriva** en tiempo de respuesta a partir de `fechaInicio`, `fechaFin`, `cancelada` y la fecha actual. Solo el booleano `cancelada` se persiste. Esto evita jobs programados para transicionar estados.

---

## 6. Categorías de herramientas

Catálogo cerrado de categorías para el MVP (enum en backend, select en frontend):

- `PERFORACION` (taladros, taladros percutores)
- `CORTE` (sierras, cizallas)
- `LIJADO` (lijadoras orbitales, de banda)
- `MEDICION` (niveles láser, cintas)
- `ESCALERAS` (escaleras, andamios)
- `JARDIN` (motosierras, desbrozadoras)
- `OTROS`

Las 10+ herramientas precargadas (R01) cubrirán al menos 4 categorías distintas para hacer útil el filtro.

---

## 7. Criterios de éxito del MVP

La solución se considera completa cuando:

1. Se arranca el backend (`mvn spring-boot:run`) y el frontend (`ng serve`) con un único comando por parte y sin configuración manual adicional.
2. Un usuario puede, sin ver la consola de red, completar el flujo: **abrir la app → elegir fechas → ver disponibilidad → reservar → ir a "mis reservas" → cancelar**.
3. Los 9 requisitos R01–R09 tienen sus criterios de aceptación (ver documento 02) validados por al menos una prueba automatizada.
4. El contrato OpenAPI expuesto por el backend coincide con los modelos consumidos por el frontend.

---

## 8. Riesgos y deuda técnica aceptada

| Riesgo / Deuda | Mitigación |
|----------------|------------|
| Nombre de carpeta `todo-app` no refleja el dominio. | Documentado en D15; renombrado queda como mejora futura. |
| H2 en memoria pierde datos al reiniciar. | Aceptable para prueba; migración a PostgreSQL requeriría solo cambiar `application.yml`. |
| Sin autenticación → cualquiera puede ver/cancelar reservas de un correo que conozca. | Documentado como fuera de alcance; en producción se exigiría login. |
| Sin paginación en `GET /tools` (10–200 elementos esperados). | Suficiente para el MVP; se añadiría si el catálogo creciera. |
| Sin rate limiting. | Fuera de alcance. |

---

## 10. Historial de decisiones

Registro de cambios materiales sobre las decisiones técnicas (D1–D19) posteriores a la v1 de la especificación. Pensado para dejar rastro defensible en la entrevista.

| Fecha | Decisión | Cambio | Motivo |
|-------|----------|--------|--------|
| 2026-11-23 | **D2** | Spring Boot `3.3.x` → `4.2.0` | Spring Initializr dejó de servir la rama 3.x desde su endpoint público (responde 400 Bad Request ante `bootVersion=3.3.5`). Spring Boot 4.2.0 también soporta Java 17 como mínimo, así que la coherencia con D1 se mantiene. La alternativa (escribir `pom.xml` a mano con 3.3.x) se descartó por aumentar el riesgo de errores típicos del wrapper de Maven en una prueba de 4 h. Cambio visible en los *starters*: `spring-boot-starter-web` → `starter-webmvc`, `spring-boot-starter-test` → starters separados por módulo (`starter-webmvc-test`, `starter-data-jpa-test`, `starter-validation-test`). |
| 2026-11-23 | **D19** | Añadida exclusión de `domain/**` al check de cobertura JaCoCo | Al implementar el seeder (commit del dominio), el enum `Categoria` y los getters de `Herramienta`/`Reserva` quedéron "cubiertos" por el simple arranque del contexto en `contextLoads()`, sin que hubiera tests reales. El bundle con 3 clases pasó a 0.32 de cobertura y el check `MAXIMUM 0.30` empezó a fallar como falso positivo. La exclusión es correcta conceptualmente: POJOs y enums no contienen lógica que amerite medición — la decisión de cobertura topada se reserva para services, controllers y advice, que sí tienen reglas de negocio. El reporte completo (sin exclusiones) sigue generado en `target/site/jacoco/`. |
