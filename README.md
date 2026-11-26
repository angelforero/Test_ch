# Entrega del proceso de selección

Este repositorio es tu entrega. Contiene un cuestionario y una prueba práctica. Complétalos y envía la URL del repositorio a la persona que te lo indicó.

## Cómo entregar

1. Crea un repositorio **público** nuevo y copia en él el contenido de esta carpeta, conservando su estructura.
2. Completa este `README.md`, `cuestionario/respuestas.md` y la carpeta `prueba/`.
3. Envía la URL del repositorio.

## Tiempo orientativo

| Parte | Tiempo orientativo |
|---|---|
| Cuestionario (`cuestionario/`) | 1,6 a 2,4 horas |
| Prueba práctica (`prueba/`) | unas 4 horas |
| **Total** | **5,6 a 6,4 horas** |

## Reglas

- Las respuestas del cuestionario van en `cuestionario/respuestas.md`. Las preguntas están en `cuestionario/PREGUNTAS.md`.
- El enunciado de la prueba está en `prueba/ENUNCIADO.md`. No lo modifiques.
- El repositorio será **público**: no incluyas información confidencial, credenciales ni datos de terceros.
- **Tu CV no forma parte del repositorio.** Se recibe por separado.
- Eres responsable de todo lo que entregas y deberás poder explicarlo en la entrevista.

---

## Herramientas de IA utilizadas

**Prueba práctica — Claude Code (Anthropic, modelos Claude) en todo el ciclo**, con un enfoque de **desarrollo guiado por especificación (SDD)**:

1. **Especificación primero.** Con Claude Code se redactaron los cuatro documentos de `prueba/especificacion/` (alcance, requisitos con criterios Gherkin, contrato API y arquitectura frontend) y se versionaron **antes** de escribir código (commit `97a2469`).
2. **Implementación guiada por la spec.** Cada commit de backend y frontend se pidió y revisó contra esos documentos: el contrato API (doc 03) fijaba endpoints, códigos y `ProblemDetail`; el doc 02 fijaba qué probar.
3. **Revisión y cierre.** En la sesión final se usó Claude Code para ejecutar los builds, arrancar el stack, hacer smoke tests por API y por la interfaz (navegador integrado), contrastar spec ↔ código ↔ historial y proponer correcciones. Toda modificación, commit y push se hizo con aprobación explícita.

**Cuestionario — sin IA.** Las respuestas de `cuestionario/respuestas.md` se redactaron sin asistencia de IA.

La IA aceleró la redacción, el andamiaje y la verificación; las decisiones, la revisión de cada cambio y su aceptación o rechazo fueron humanas (ver *Correcciones realizadas a la salida de la IA*).

## Resumen de la especificación

Detalle en [`prueba/especificacion/`](prueba/especificacion/) (versión **1.1**, alineada con la implementación verificada; la v1.0 previa a implementar está en el commit `97a2469`).

**Alcance (doc 01).** Biblioteca de barrio que presta herramientas. Un socio se identifica por nombre y correo (sin login). API REST en Java + aplicación Angular con tres pantallas: catálogo y disponibilidad, nueva reserva y mis reservas. Fuera de alcance: autenticación, notificaciones, pagos, backoffice, despliegue.

**Modelo.** `Herramienta(id, nombre, categoria)` con 7 categorías y 10 herramientas precargadas. `Reserva(herramienta, nombre, correo, fechaInicio, fechaFin, cancelada)`. El **estado** (`CONFIRMADA`, `EN_CURSO`, `FINALIZADA`, `CANCELADA`) se **deriva** de las fechas y del flag `cancelada`; no se persiste. Fechas `LocalDate`, rangos inclusivos, "hoy" en `Europe/Madrid`.

**Requisitos y criterios clave (doc 02, formato Gherkin):**

| Req. | Qué exige | Criterios destacados |
|------|-----------|----------------------|
| R01 | Catálogo con filtro y disponibilidad por rango | ≥ 10 herramientas; disponibilidad = sin reservas no canceladas que solapen (`a1 ≤ b2 ∧ b1 ≤ a2`) |
| R02 | Crear reserva | inicio ≥ hoy; duración ≤ 7 días inclusivos; correo normalizado (`trim` + minúsculas); 201 + `Location` |
| R03 | Sin solapamientos | parcial, total, contenida → 409 `overlap-conflict`; adyacente y cancelada no bloquean |
| R04 | Máx. 3 activas por correo | la 4.ª → 409 `active-reservation-limit`; canceladas y finalizadas no cuentan; sin distinguir mayúsculas |
| R05 | Cancelar y consultar | cancelable hasta el día anterior al inicio → 200 + DTO; si no, 409 `cancellation-not-allowed` con `motivo` |
| R06 | Pantallas de búsqueda y reserva | validación local antes de llamar al API; errores del API visibles; estados de carga |
| R07 | Pantalla "mis reservas" | búsqueda por correo; Cancelar solo si `CONFIRMADA`, con confirmación; la fila se actualiza sin recargar |
| R08 | Errores consistentes | RFC 7807 `application/problem+json`; 400 / 404 / 409 / 422 / 500; `errors[]` por campo; sin stack traces |
| R09 | < 500 ms con 200 herramientas y 5.000 reservas | una sola query `NOT EXISTS` + índices (`idx_reservas_rango`, …) |

**Contrato (doc 03).** `GET /api/tools[?categoria]`, `GET /api/tools/available?fechaInicio&fechaFin[&categoria]`, `POST /api/reservations`, `GET /api/reservations?correo`, `DELETE /api/reservations/{id}`. Catálogo cerrado de `type` en `http://localhost:8080/problems/<slug>`. OpenAPI en `/v3/api-docs` y `/swagger-ui.html`.

## Decisiones tomadas

Registro completo en el doc 01 §3 (D1–D19) y §10 (historial con fecha y motivo).

| Decisión | Razón |
|----------|-------|
| **Java 17 + Spring Boot 4.2.0 + Maven + H2 en memoria** | LTS; Boot 4.2 era lo que servía Spring Initializr; H2 no requiere instalación |
| **Errores centralizados**: excepciones de dominio lanzadas por los servicios + un único `@RestControllerAdvice` que devuelve `ProblemDetail` | Los controladores no tienen `try/catch`; un único punto de mapeo a HTTP, coherente y testeable |
| **Estado de la reserva derivado**, no persistido | Evita jobs programados para transicionar estados; una sola fuente de verdad (`cancelada` + fechas + `Clock`) |
| **`Clock` inyectado** (`Europe/Madrid`) | Los tests fijan "hoy" sin mockear `LocalDate.now()` |
| **`ValidadorRango` compartido** por disponibilidad y reserva | Las mismas tres reglas de fechas en ambos sitios; la disponibilidad solo ofrece rangos reservables |
| **Disponibilidad en una sola query JPQL `NOT EXISTS`** + índices compuestos | Requisito R09: no se traen reservas a memoria |
| **Orden de validación al crear**: fechas → existe → límite (R04) → solape (R03) | Validaciones baratas primero; el límite da el mensaje más accionable al socio saturado |
| **`DELETE` devuelve 200 + DTO** (no 204) | La UI sustituye la fila sin un segundo GET |
| **Correo normalizado** en el DTO (antes de Bean Validation) y en el servicio | `ADA@x.com` y ` ada@x.com ` son el mismo socio para R04 y para la consulta |
| **Angular 19 standalone + Signals + Reactive Forms + CSS vanilla**, rutas **lazy** | Sin NgModules ni librerías de UI ni de estado; un chunk por pantalla |
| **Interceptor HTTP → `AppError`** + `ErrorBanner` | Una sola traducción de `ProblemDetail` para toda la app (R08) |
| **Proxy de Angular** (`/api` → `:8080`) en lugar de CORS en el backend | Sin peticiones cross-origin en desarrollo; menos configuración |
| **Solo tests unitarios con mocks** (Mockito, `@WebMvcTest`, `HttpTestingController`) | Prioriza reglas críticas y contratos HTTP en 4 h. **Backend: techo JaCoCo del 75 %** (evitar sobre-testing); **frontend: mínimo del 30 %**, sin techo |
| **Se conserva el nombre `todo-app`** del scaffold Angular | Renombrarlo toca `angular.json`, `package.json` y `tsconfig`; el riesgo no compensaba (deuda documentada) |

## Opciones descartadas

| Alternativa | Por qué se descartó |
|-------------|---------------------|
| Spring Boot 3.3.x con `pom.xml` escrito a mano | Initializr ya no servía 3.x; escribir el pom a mano añadía riesgo sin beneficio |
| Columna `estado` persistida + tareas programadas | Estados desincronizados y más código; derivarlo es más simple y exacto |
| Tabla `Socio` normalizada | Sin login, el correo basta como identificador (S1/S2) |
| `DELETE` → `204 No Content` (plan v1.0) | Obligaba a un GET extra para refrescar la fila |
| Endpoint `GET /api/categories` | Catálogo de 7 valores, estable: un enum TS 1:1 con el backend evita un round-trip |
| `CorsRegistry` / `@CrossOrigin` en el backend | Redundante con el proxy de Angular |
| Bloqueo pesimista (`PESSIMISTIC_WRITE`) al crear | No se podía demostrar sin un test multihilo (fuera de D19) y S9 asume que no hay concurrencia hostil. Documentado como deuda, no como implementado |
| NgRx / Angular Material | Sobredimensionados para tres pantallas; Signals y CSS propio bastan |
| Modal `ConfirmDialog` propio | `window.confirm()` cubre el requisito sin inflar el alcance |
| Guardar el correo en `localStorage` | Datos personales en equipos compartidos; la pantalla se mantiene sin estado |
| Tests E2E (Cypress / Playwright) e integración multiservicio | Fuera de la estrategia D19; el flujo completo se validó con smoke tests manuales |
| PostgreSQL / Docker | Instalación extra sin valor para la prueba; migrar solo requeriría cambiar `application.yml` |

## Correcciones realizadas a la salida de la IA

Casos concretos, con su evidencia en el historial (doc 01 §10 y mensajes de commit):

1. **Versión de Spring Boot.** El plan inicial generado fijaba Spring Boot 3.3.x; Initializr respondía 400 para esa rama. Se corrigió a 4.2.0, con el cambio de *starters* y del paquete de `@WebMvcTest` que eso implica (D2, commit `d5aeabe`).
2. **Cobertura del frontend interpretada al revés.** La IA trató el 30 % como **techo** y creó un comprobador de máximo. Se corrigió: el 30 % es un **mínimo**, sin techo. Se sustituyó `check-coverage-max.js` por `check-coverage-min.js` (D19).
3. **Techo JaCoCo del 30 % en el backend.** Penalizaba los tests legítimos de las reglas de negocio (solape, límite, cancelación). Se elevó a 75 % y se excluyeron `domain/**`, DTOs y configuración, que daban falsos positivos al arrancar el contexto (D19).
4. **`ObjectMapper` en `@WebMvcTest`.** El código generado asumía que se inyectaba como en Boot 3. Falló en Boot 4 y se construyó a mano; después se migró a `JsonMapper` de Jackson 3 (commit `63ca43a`).
5. **springdoc 2.6.0 incompatible.** La versión elegida es de la línea de Boot 3: `/v3/api-docs` devolvía 500 (`NoSuchMethodError`). Los tests no lo detectaban; apareció en el smoke test de cierre. Se subió a 3.2.1, compilada contra Boot 4.2.0 (commit `63ca43a`).
6. **Normalización del correo especificada pero no implementada.** La spec la exigía (CA-R02.9, CA-R04.5), pero el código generado no la aplicaba, de modo que `LIMITE@EXAMPLE.COM` eludía el límite de 3. Se detectó en el smoke test y se corrigió con 4 tests nuevos (commit `86544a3`).
7. **Especificación que sobredeclaraba.** La v1.0 describía un bloqueo pesimista "implementado", `DELETE` 204, `errors[]` con `{field, message}`, CORS en el backend y tests (`GlobalExceptionHandlerTest`, `DataSeederTest`) que nunca existieron. Se rechazó dejarla así: la v1.1 documenta lo que realmente hay y lo que no (commit `698ed2b`).
8. **Documentos de traspaso desactualizados.** Las notas de reanudación generadas daban por pendiente R05 cuando ya estaba en `70c52d4`. Se contrastaron con el código y el historial antes de actuar, en lugar de reimplementar.
9. **Autoría de los commits.** En el equipo de cierre la configuración de git tenía otro correo. Se detectó antes del commit y se forzó el autor correcto, para no publicar un dato distinto en un repositorio público.

## Cómo ejecutar las pruebas

**Requisitos:** JDK 17+, Node.js 18.19+/20.11+/22, Google Chrome (para Karma). Maven no hace falta (se usa el *wrapper*). Las referencias a las suites y su contenido están en [`prueba/tests/README.md`](prueba/tests/README.md).

### Backend — tests unitarios + cobertura JaCoCo
```powershell
cd prueba\src\backend\herramientas
.\mvnw.cmd clean verify
```
Resultado esperado: `Tests run: 36, Failures: 0` · `All coverage checks have been met.` · `BUILD SUCCESS`. Informe HTML en `target/site/jacoco/index.html`. En Linux/macOS: `./mvnw clean verify`.

> Si Maven falla con `PKIX path building failed` (red con proxy que inspecciona TLS), en Windows: `$env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"` y repetir.

### Frontend — tests unitarios + cobertura mínima + build
```powershell
cd prueba\src\frontend\todo-app
npm install
npm test
npm run build
```
`npm test` ejecuta Karma en ChromeHeadless con cobertura y después `scripts/check-coverage-min.js`, que falla si alguna métrica baja del 30 %. Resultado esperado: `TOTAL: 35 SUCCESS` y `OK — todas las métricas son >= 30%`. Si Chrome no está en la ruta por defecto: `$env:CHROME_BIN = "C:\ruta\chrome.exe"`.

### Backend y frontend juntos
Dos terminales:
```powershell
# Terminal 1 — API en http://localhost:8080 (H2 en memoria, 10 herramientas precargadas)
cd prueba\src\backend\herramientas
.\mvnw.cmd spring-boot:run
```
```powershell
# Terminal 2 — app en http://localhost:4200 (proxy /api -> :8080)
cd prueba\src\frontend\todo-app
npm start
```
Abrir `http://localhost:4200`: **Catálogo** (`/herramientas`), **Nueva reserva** (`/nueva-reserva`), **Mis reservas** (`/mis-reservas`). Swagger UI: `http://localhost:8080/swagger-ui.html`. Consola H2: `http://localhost:8080/h2-console` (JDBC `jdbc:h2:mem:biblioteca`, usuario `sa`, sin contraseña).

Smoke test rápido por API (a través del proxy):
```powershell
curl.exe "http://localhost:4200/api/tools"
curl.exe "http://localhost:4200/api/tools/available?fechaInicio=2026-12-10&fechaFin=2026-12-12"
```

## Observaciones

**Verificado al cierre:** backend 36/36 tests (JaCoCo 73,6 %); frontend 35/35 (≈ 86 % statements) y build correcto. Smoke test de los 7 pasos (catálogo, disponibilidad, crear, solape → 409, mis reservas, cancelar → 200, recancelar → 409) **por API y por la interfaz**.

**Límites conocidos** (todos declarados en la especificación v1.1):
- **R09 sin medir.** El diseño apunta a cumplirlo (query única indexada), pero no hay perfil `perf` con 200 herramientas y 5.000 reservas ni test de latencia.
- **Concurrencia (CA-R03.7).** La comprobación de solape es transaccional pero sin bloqueo pesimista: dos reservas simultáneas del mismo rango podrían aceptarse.
- **Sin autenticación.** Quien conozca un correo puede ver y cancelar sus reservas (fuera de alcance por enunciado).
- **H2 en memoria.** Los datos se pierden al reiniciar.
- **Huecos de tests.** Sin test automatizado: los mapeos 404, `ConstraintViolationException` y 500 del handler (verificados a mano) y el seeder.
- **UX menor.** No hay botón "Reservar" directo desde la lista de disponibles (la reserva tiene su propia pantalla). Tras un 409 al cancelar, la fila no se refresca. Un 404 se clasifica como `Unknown` en el frontend, aunque se muestra el mensaje del servidor. `ProblemDetail` no incluye `instance`.
- **Nombre `todo-app`** del proyecto Angular heredado del scaffold.

**Con más tiempo:**
1. Perfil `perf` + test `@Tag("performance")` para medir R09 (p95 < 500 ms).
2. `@Lock(PESSIMISTIC_WRITE)` en la creación + test concurrente con `@DataJpaTest`.
3. Completar los tests del handler (404 / 422 de parámetros / 500) y del seeder, vigilando el techo del 75 %.
4. Botón "Reservar" en la lista de disponibles, refresco de la fila tras un 409 y `ProblemType` alineado con el backend.
5. Autenticación ligera (enlace mágico por correo) para proteger "mis reservas".

**Estructura de la entrega:** especificación en `prueba/especificacion/`, backend en `prueba/src/backend/herramientas`, frontend en `prueba/src/frontend/todo-app`, referencias a las pruebas en `prueba/tests/`. El historial de commits sigue el orden especificación → implementación por capas → correcciones → alineación de la spec.
