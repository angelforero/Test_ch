# Respuestas

Escribe tu respuesta en el bloque de cada pregunta, sin cambiar los identificadores. Las preguntas están en `cuestionario/PREGUNTAS.md`.

---

## Q01
_Extensión orientativa: 200-250 palabras._

Lo primero es mirar, no tocar. Con 18 de 20 conexiones ocupadas y la base al 30 % de CPU, el cuello de botella está en la aplicación: las conexiones se retienen demasiado tiempo. Sospecho de una consulta N+1 por carga perezosa de las líneas de pedido, o de una transacción abierta que incluye trabajo ajeno a la base (una llamada HTTP, serialización).

Primera hora:
1. Revisar métricas de Hikari (activas, pendientes, tiempo de espera) y activar el log de sentencias SQL en un solo nodo para contar consultas por petición.
2. Sacar un thread dump bajo carga para ver dónde esperan los hilos.
3. Mirar el diff del martes solo en el endpoint de pedidos y sus dependencias. Si una de las tres historias lo toca, ahí está el candidato.
4. Si en 30-40 minutos no hay causa clara, revertir esa historia concreta (no todo el despliegue) o desplegar con feature flag apagado.

Pospongo: subir el pool a 50 (solo trasladaría la presión a la base y esconde la causa), añadir caché, reescribir la consulta o tocar índices. También el análisis de causa raíz completo y la prueba de carga: eso va para el miércoles, no para la primera hora.

Cambiaría de plan si el thread dump muestra bloqueos en otro recurso (un lock, un servicio externo lento) o si la CPU de la base subiera, lo que indicaría un problema de consulta y no de conexiones. Con la entrega del viernes, prefiero una reversión parcial limpia a un parche improvisado.

---

## Q02
_Extensión orientativa: 120-200 palabras (el mensaje)._

Asunto: Reservas – estado a las 10:30

Hola,

**Impacto:** desde ayer a las 16:10, cerca del 15 % de los usuarios (los que tienen guardada una versión antigua de la pantalla) recibe un error al confirmar una reserva. Las reservas ya hechas están intactas y el 85 % restante opera con normalidad.

**Causa:** una actualización cambió el formato de una fecha y la pantalla antigua no lo entiende.

**Estado actual:** tenemos dos salidas. Revertir la parte del servidor tarda unos 30 minutos, pero perderíamos la corrección que pidió el cliente importante. Forzar que todos los usuarios recarguen la pantalla es más rápido, pero podría interrumpir a quien esté rellenando un formulario.

**Próximo paso:** mi inclinación es hacer que el servidor acepte ambos formatos de fecha, de modo que nadie tenga que recargar y no se pierda la corrección. Lo confirmo con el equipo en la próxima media hora.

**Hora estimada:** resolución entre las 11:30 y las 12:00. Si surge algún imprevisto en las pruebas, la recarga forzada nos dejaría resueltos antes de las 11:00, con el inconveniente indicado.

Os escribo de nuevo a las 11:00 con novedades.

---

## Q03
_Extensión orientativa: 200-250 palabras._

La arquitectura objetivo no es «microservicios» sino un monolito modular con un único servicio extraído: inventario de asientos. El problema es concreto: 80 % de la carga y de los bloqueos en un módulo, picos de 20 minutos dos veces al año. Con 9 personas, seis microservicios dedicarían la mayor parte del equipo a operar infraestructura, y no lo justifica el riesgo que cuesta 40.000 USD por caída.

Orden:
1. **Semanas 1-4:** pruebas de carga que reproduzcan el pico y medir dónde se bloquea. Muchas veces el problema es de diseño de la reserva (bloqueos pesimistas sobre filas calientes), no de arquitectura. Arreglarlo puede ser suficiente para el próximo evento.
2. **Mientras tanto:** definir fronteras limpias entre módulos (interfaces, sin acceso cruzado a tablas) para poder seguir entregando funcionalidades.
3. **Meses 2-5:** extraer inventario con su propia base de datos, reservas temporales con caducidad, cola de entrada (sala de espera) y escritura idempotente. Estrangulamiento gradual, con el monolito como respaldo.
4. **Mes 6:** evaluar con datos si algún otro módulo (notificaciones, reportes) merece salir.

Descarto la reescritura total (congelaría funcionalidades) y dividir por módulos «por principio».

Cambiaría de decisión si la prueba de carga demuestra que basta con ajustar el modelo de bloqueos, en cuyo caso pospongo la extracción, o si el negocio añade picos más frecuentes, que justificarían extraer más servicios.

---

## Q04
_Extensión orientativa: 200-250 palabras._

Rechazo unificar los dos cálculos de descuento. Es duplicación accidental: el código se parece hoy, pero cambia por razones distintas y lo gobiernan áreas diferentes. Una función genérica con parámetros y condicionales acabaría acoplando a ambas y cada cambio de mayoristas arriesgaría romper minoristas. Dos funciones claras, aunque repetidas, son más baratas de mantener que una abstracción equivocada. Lo que sí es duplicación esencial (redondeo, validación de importes) puede compartirse en una utilidad pequeña.

Qué refactorizo en estas dos semanas, con riesgo acotado:
1. Primero, pruebas de caracterización sobre `PedidoService` en los caminos críticos (precio, persistencia).
2. Extraer el envío de correos a un componente propio, que es lo más desacoplado y fácil de aislar, y puede dispararse por evento tras guardar el pedido.
3. Extraer el cálculo de precios y descuentos a clases separadas por tipo de cliente (estrategia), moviendo código sin cambiar comportamiento.

No toco: la persistencia y la validación, salvo que bloqueen el hito; no empiezo un rediseño de 1.800 líneas en un golpe. Es mejor avanzar en pasos pequeños que dejar una rama larga a dos semanas del hito.

El principio cede cuando las reglas cambian por motivos distintos. Cambiaría de opinión si las áreas de negocio acordaran unificar las políticas, o si en tres meses viéramos que siempre se modifican a la vez.

---

## Q05
_Extensión orientativa: 200-250 palabras._

Mi experiencia está más en backend, así que aquí soy menos preciso, pero propondría lo siguiente.

Lo primero es entender por qué se congela: con 30 mensajes por segundo y 5.000 filas, probablemente cada mensaje dispara una detección de cambios completa y se vuelve a pintar toda la tabla. Abriría el profiler del navegador para confirmarlo antes de cambiar nada.

Estructura que propondría:
1. Un único servicio que reciba el WebSocket y guarde los datos, en lugar de que cada panel tenga su propia suscripción. Los cuatro paneles leerían de ahí con los filtros aplicados.
2. Agrupar los mensajes en lotes (por ejemplo, actualizar la pantalla cada 250-500 ms con lo acumulado) en vez de reaccionar a cada mensaje.
3. Limitar lo que se pinta con paginación o con una tabla que muestre solo las filas visibles.
4. Si es posible, activar la estrategia de detección de cambios más ligera en los componentes de la tabla.

Descarto introducir una librería de estado nueva (como NgRx) con solo dos personas y seis semanas, porque me parece demasiado coste de aprendizaje; y descarto reescribir los paneles.

Cambiaría el plan si el profiler mostrara que el problema no está en la pintura sino en el procesamiento de los datos, en cuyo caso lo movería a un worker. Pediría apoyo de alguien con más experiencia en Angular para validar este enfoque antes de empezar.

---

## Q06
_Extensión orientativa: 120-200 palabras (el mensaje)._

Hola equipo de analítica,

Gracias por explicar la necesidad. Entiendo que el API no cubre lo que necesitáis y quiero que empecéis la próxima semana, pero no puedo daros acceso directo a las tablas de pedidos.

**Por qué:** vamos a cambiar el esquema de esas tablas el próximo trimestre, y otras dos aplicaciones que ya leen directamente han provocado dos incidentes este año. Si construís informes sobre el esquema interno, cada cambio nuestro rompe vuestros informes, y cada consulta pesada vuestra puede ralentizar los pedidos de los clientes.

**Propuesta:**
1. Esta semana: una sesión de 45 minutos para listar los campos y métricas exactos que necesitáis.
2. Próxima semana: os entregamos una vista de solo lectura sobre una réplica de la base, con un contrato estable (nombres y tipos de columnas que no cambiaremos sin avisar con un mes de antelación).
3. Después, si el volumen lo requiere, una exportación diaria a vuestro almacén de datos.

Así empezáis la semana que viene con datos fiables y nosotros no os rompemos nada al migrar. ¿Os viene bien reuniros el miércoles?

Un saludo.

---

## Q07
_Extensión orientativa: 200-250 palabras._

Con 12 % de cobertura (y de poco valor), el autor ausente y un error que obliga a reembolsos, no empiezo por el nuevo tipo de tarifa sino por poder detectar si rompo algo.

Primeros días:
1. Pruebas de caracterización (golden master): capturar entradas y salidas reales del cálculo actual con un conjunto representativo de casos (datos de producción anonimizados, o un volcado de comisiones históricas) y fijar el resultado como referencia. No verifican que el comportamiento sea correcto, solo que no cambia.
2. Pruebas de integración con una base de datos real (Testcontainers) en los caminos de lectura y escritura de comisiones, porque los mocks actuales solo repiten la implementación.
3. Después, implementar la nueva tarifa con sus propias pruebas, a ser posible guiadas por ejemplos con ayuda de negocio (casos límite, redondeos).
4. Despliegue con un indicador (feature flag) y comparación en paralelo: calcular con ambos caminos y registrar diferencias antes de activar.

Difiero: refactorizar las 25 clases, subir la cobertura a un porcentaje objetivo y eliminar los mocks existentes. Quedan registrados como deuda con un ticket concreto.

Descarto reescribir el módulo: sin entender las reglas es más peligroso que tocarlo con red.

Cambiaría de plan si en las primeras 48 horas no consigo datos históricos representativos (entonces reduzco el alcance de la tarifa) o si el golden master revela inconsistencias en el comportamiento actual, que habría que escalar a negocio antes de seguir.

---

## Q08
_Extensión orientativa: 200-250 palabras._

La preocupación que manda es la inmutabilidad: es un requisito legal y no admite excepción. Le sigue la consulta en menos de 3 segundos, y el coste queda como restricción dura de 1.500 USD.

Diseño:
1. Cada servicio publica un evento de cambio (quién, qué, cuándo, valor anterior y nuevo) mediante el patrón outbox, en la misma transacción que el cambio de negocio, para no perder ni duplicar registros. Descarto la llamada HTTP directa a un servicio de auditoría, que perdería eventos si falla.
2. Un servicio de auditoría los consume y los escribe en una tabla solo de inserción (append-only) en una base relacional, particionada por mes y con índices por contrato y fecha. Cinco millones al mes durante siete años son unos 400 millones de filas: manejable con particiones, y se cumplen los 3 segundos porque casi todas las consultas filtran por contrato.
3. Inmutabilidad: el usuario de la aplicación solo tiene permiso de inserción, cada registro lleva un hash encadenado con el anterior, y se archivan periódicamente copias en almacenamiento de objetos con bloqueo de escritura (WORM).
4. Las particiones antiguas pasan a almacenamiento más barato.

Descarto una base de eventos especializada o blockchain: coste y complejidad desproporcionados para el equipo de 5 personas.

Cambiaría de decisión si la estimación de costes superase el presupuesto (el almacenamiento WORM sería lo primero en evaluar) o si el asesor legal exigiera una certificación concreta de inmutabilidad.

---

## Q09
_Extensión orientativa: 120-200 palabras (el mensaje)._

Asunto: Decisión sobre la generación de facturas y contratos en PDF

**Recomendación:** sustituir las plantillas actuales por una librería de plantillas en Java, empezando después de entregar la primera de las dos funcionalidades que habéis pedido.

**Por qué:** hoy solo 2 de las 7 personas del equipo entienden las plantillas. Cada cambio pequeño tarda de 3 a 5 días y este año ha causado 3 errores en producción (en documentos de clientes). Mantenerlas nos cuesta aproximadamente una semana-persona al mes, unas 12 al año.

**Costo:** 6 semanas de 2 personas (12 semanas-persona). Se recupera en alrededor de un año solo con el ahorro de mantenimiento, sin contar los errores evitados.

**Riesgo:** retrasar una de las dos funcionalidades un trimestre. Si no hacemos nada, el riesgo es que, si una de las 2 personas se va, nos quedamos sin quien pueda tocar los documentos.

**Qué necesito de vosotros:** que decidáis cuál de las dos funcionalidades pasa primero y cuál se retrasa unas 6 semanas. Os pido respuesta antes del viernes para planificar el trimestre.

---

## Q10
_Extensión orientativa: 200-250 palabras._

Con la versión 11 sin parches, la actualización es necesaria, pero con una semana de congelación máxima no se puede hacer de golpe.

Lo primero: subir versión a versión (11 → 12 → 13 …), porque Angular lo recomienda así, usando `ng update`. Antes haría un inventario de las librerías de terceros abandonadas y de lo que bloquea cada salto. Aprovecharía para subir la cobertura de pruebas en las pantallas más críticas, porque el 25 % actual es poco para saber si algo se rompe.

Plan:
1. Crear una rama de migración y subir un par de versiones por iteración, con las pruebas pasando y despliegues normales entre saltos.
2. Reemplazar las librerías sin mantenimiento por alternativas, o sustituirlas por código propio si son pequeñas.
3. Dejar para después sacar la lógica de negocio de los componentes: es deuda importante, pero no es necesaria para actualizar.

Descarto reescribir la aplicación en otro framework, y descarto congelar más de una semana.

Pienso que el punto de no retorno llega cuando las ramas de migración y de funcionalidades han divergido tanto que fusionarlas cuesta más que seguir. No tengo muy claro cómo medirlo, así que lo revisaría con el equipo en cada salto. Cambiaría la estrategia si alguna librería crítica no tuviera sustituto, y consultaría a alguien con más experiencia en migraciones grandes de Angular para validar el orden de los saltos.

---

## Q11
_Extensión orientativa: 200-250 palabras._

Simplifico, pero no ahora de golpe. Hay una fecha fija de lanzamiento con cambios de pagos dentro, y una refactorización grande de pagos a tres sprints del lanzamiento es el momento más peligroso para hacerla.

Plan:
1. Sprint 1: medir. Qué partes de las 14 interfaces se usan realmente, qué cambios de pagos exige el lanzamiento y cuánto cuesta cada cambio con la estructura actual. Ahí también reviso por qué la suite tarda 12 minutos (probablemente pruebas de integración que podrían ser unitarias).
2. Sprints 1-2: implementar los cambios de pagos del lanzamiento con la mínima superficie y protegerlos con pruebas, sin ampliar la capa.
3. Aplicar la «regla del boy scout» cuando toquemos un archivo: eliminar interfaces que solo tienen una implementación y configuración propia que pueda reemplazarse por la del framework.
4. Tras el lanzamiento: sustituir la capa por un único puerto de pagos con un adaptador para el proveedor actual (unos 300-400 líneas). Si llega un segundo proveedor, ese puerto es el punto de extensión.

Descarto mantenerla tal cual (el coste lo pagamos en cada cambio) y descarto reescribirla ahora (riesgo en la fecha fija). No construyo una abstracción para un «quizá».

Cambiaría de plan si el negocio confirmara el segundo proveedor con fecha y presupuesto: entonces parte de la capa se aprovecha. O si las pruebas lentas retrasaran el lanzamiento, y entonces haría antes la limpieza.

---

## Q12
_Extensión orientativa: 120-200 palabras (el mensaje)._

Hola,

Te explico las dos opciones en términos de negocio.

**Opción A, estado inmediato.** El cliente siempre ve el estado exacto. A cambio, la pantalla es más lenta y, si otro de nuestros sistemas falla, el cliente puede ver un error en lugar del estado de su pedido. Es como llamar por teléfono a almacén para cada consulta.

**Opción B, retraso de hasta 30 segundos.** La pantalla es rápida y siempre está disponible, aunque otro sistema falle. A veces, durante un máximo de 30 segundos tras un cambio, el cliente verá el estado anterior. Es como consultar un tablón que se actualiza cada medio minuto.

**Datos de hoy:** recibimos unas 2.000 consultas por minuto y 12 reclamaciones al mes por estado desactualizado, es decir, una fracción muy pequeña de las consultas.

**Mi recomendación: opción B**, con dos mejoras: mostrar «actualizado hace X segundos» y, justo después de que el cliente haga una acción (por ejemplo, cancelar), mostrarle el estado nuevo sin esperar. Así reducimos las reclamaciones actuales y ganamos disponibilidad.

Cambiaría de opinión si hubiera pedidos en los que un estado desfasado tuviera consecuencias económicas (por ejemplo, un pago). Dime qué opinas.
