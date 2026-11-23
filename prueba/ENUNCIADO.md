# Enunciado de la prueba

## Contexto

Una biblioteca comunitaria de barrio presta herramientas (taladros, escaleras, lijadoras, etc.) a sus socios. Hoy las reservas se anotan en una hoja de cálculo compartida. La organización es ficticia; no necesitas conocimientos de ningún sector específico.

## Objetivo

Construir una solución integrada para reservar herramientas por rango de fechas: un backend en **Java** que expone un API REST y un frontend en **Angular** que consume ese API para consultar disponibilidad, reservar y gestionar reservas. El frontend debe usar el backend; no se acepta un frontend con datos simulados.

## Requisitos

**R01 — Catálogo y disponibilidad.** El API ofrece el catálogo de herramientas (identificador, nombre y categoría), con filtro opcional por categoría, y permite consultar qué herramientas están disponibles para un rango de fechas. La solución parte de, al menos, 10 herramientas precargadas.

**R02 — Crear reserva.** Una persona, identificada por su nombre y correo electrónico, reserva una herramienta para un rango de fechas. Una reserva ocupa la herramienta desde la fecha de inicio hasta la fecha de fin, ambas incluidas. La duración máxima es de 7 días y la fecha de inicio no puede ser anterior a hoy.

**R03 — Sin solapamientos.** Una herramienta nunca puede tener dos reservas que coincidan en algún día.

**R04 — Límite por persona.** Una persona (identificada por su correo electrónico) puede tener como máximo 3 reservas activas.

**R05 — Cancelar y consultar.** El API permite cancelar una reserva hasta el día anterior a su fecha de inicio, y consultar las reservas de una persona con su estado.

**R06 — Pantalla de búsqueda y reserva.** La interfaz permite elegir un rango de fechas, ver las herramientas disponibles (con filtro por categoría) y reservar una de ellas, mostrando con claridad los errores que devuelva el API.

**R07 — Pantalla «mis reservas».** La interfaz permite a una persona consultar sus reservas por correo electrónico y cancelarlas.

**R08 — Errores y validación (no funcional).** El API valida sus entradas y responde los errores con un formato consistente y códigos HTTP apropiados.

**R09 — Rendimiento (no funcional).** La consulta de disponibilidad responde en menos de 500 ms con un catálogo de 200 herramientas y 5.000 reservas existentes.

## Entrega

Completa esta estructura dentro de tu repositorio público:

- `prueba/especificacion/` — tu especificación, escrita **antes** de implementar: alcance, requisitos con criterios de aceptación, suposiciones, y el contrato entre frontend y backend (endpoints, cuerpos de petición y respuesta, errores).
- `prueba/src/backend/` — el código del backend.
- `prueba/src/frontend/` — el código del frontend.
- `prueba/tests/` — las pruebas (o referencias claras a ellas, si por convención del framework viven junto al código).
- `README.md` (raíz) — completa todas sus secciones, incluidas las instrucciones para ejecutar las pruebas y para ejecutar backend y frontend juntos.

El historial de commits debe reflejar tu proceso (especificación, implementación, pruebas, correcciones). No incluyas tu CV ni información confidencial: el repositorio es público.

## Tiempo y uso de IA

- Tiempo orientativo: unas 4 horas. Prioriza lo que consideres más importante y documenta lo que quede fuera.
- **Debes usar asistencia de IA** y **declararla** en el `README.md`: herramientas utilizadas y correcciones que hiciste a lo que generó.
- La herramienta es de libre elección.
- Eres responsable de todo lo entregado y deberás poder explicarlo en la entrevista.
