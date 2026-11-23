# Cuestionario

Doce escenarios breves de situaciones técnicas realistas. Todos los contextos son ficticios: no incluyas información confidencial en tus respuestas.

- Responde en `cuestionario/respuestas.md`, en el bloque de cada pregunta.
- No hay una única respuesta correcta. Se valora que decidas, justifiques y indiques qué sacrificas.
- Si algún dato te falta, haz una suposición explícita y continúa.
- La extensión indicada es orientativa: prioriza ser claro y específico.
- Tiempo orientativo: entre 8 y 12 minutos por pregunta.

---

## Q01

**Contexto.** Un servicio Spring Boot de una plataforma de comercio electrónico expone el listado de pedidos de un cliente. Desde el despliegue del martes, la latencia p95 pasó de 350 ms a 5 s, con unas 40 peticiones por segundo entre las 11:00 y las 14:00. No hay errores en los logs. La base de datos relacional está al 30 % de CPU, pero 18 de las 20 conexiones del pool están ocupadas de forma sostenida. El equipo son 4 personas, hay una entrega comprometida el viernes y el despliegue del martes incluyó tres historias distintas.

**La decisión.** Decide qué haces durante la primera hora y qué dejas para después.

**Qué debes responder.** Qué haces primero · qué descartas o pospones, y por qué · qué te haría cambiar de plan.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q02

**Contexto.** Ayer a las 16:10 se desplegó una nueva versión de una aplicación web de reservas (interfaz Angular y API Java). Desde entonces, cerca del 15 % de los usuarios —los que conservan en caché una versión anterior de la interfaz— ve un error al confirmar una reserva; las reservas ya existentes no se pierden. La causa: el API cambió el formato de una fecha y la interfaz antigua no lo entiende. Hay dos salidas: revertir el API (unos 30 minutos, pero se pierde una corrección que pidió un cliente importante) o forzar la recarga de la interfaz de todos los usuarios (más rápido, con riesgo de interrumpir a quien esté completando un formulario). Son las 10:00 y la dirección comercial pidió una actualización para las 10:30.

**La decisión.** Escribe el mensaje de actualización que enviarías a la dirección comercial (perfil no técnico).

**Qué debes responder.** El mensaje mismo, dirigido a la dirección comercial. Debe dejar claros el impacto, el estado actual, el próximo paso y la hora estimada de resolución con su incertidumbre.

**Extensión orientativa:** entre 120 y 200 palabras.

---

## Q03

**Contexto.** Una empresa de venta de entradas tiene un monolito (un solo despliegue y una base de datos) con seis módulos: catálogo, inventario de asientos, pagos, usuarios, notificaciones y reportes. Lo mantienen 9 personas. Dos veces al año, en el lanzamiento de grandes eventos, el tráfico se multiplica por 15 durante unos 20 minutos; el módulo de inventario concentra el 80 % de la carga y de los bloqueos en la base de datos. Cada caída en esos picos cuesta unos 40.000 USD. La dirección pide «pasar a microservicios» en 6 meses, sin detener el desarrollo de funcionalidades.

**La decisión.** Decide qué arquitectura objetivo propones y en qué orden llegas a ella.

**Qué debes responder.** Qué haces primero · qué descartas y por qué · qué te haría cambiar la decisión.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q04

**Contexto.** En un backend Java con Spring Boot, la clase `PedidoService` tiene 1.800 líneas y mezcla validación, cálculo de precios, persistencia y envío de correos. En la última revisión de código, alguien propone unificar «por DRY» en una sola función genérica dos cálculos de descuento muy parecidos (uno para clientes minoristas y otro para mayoristas). Sin embargo, las reglas de cada uno cambian por motivos distintos y las gestionan áreas de negocio diferentes. El equipo son 5 personas y faltan 2 semanas para el próximo hito.

**La decisión.** Decide qué refactorizas ahora, qué no tocas y dónde el principio debe ceder.

**Qué debes responder.** Qué haces primero · qué descartas y por qué · qué te haría cambiar la decisión.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q05

**Contexto.** Una aplicación Angular de monitorización muestra una tabla de hasta 5.000 filas que se actualizan por WebSocket (unos 30 mensajes por segundo) y cuatro paneles que dependen de los mismos datos filtrados. Hoy cada panel mantiene su estado con servicios y suscripciones propios, y la interfaz se congela durante unos segundos cuando llegan ráfagas de mensajes. Trabajan 2 personas de frontend y hay 6 semanas para entregar una nueva vista de comparación. El API no puede modificarse en ese periodo.

**La decisión.** Decide cómo estructuras el estado y la actualización de la interfaz.

**Qué debes responder.** Qué haces primero · qué descartas y por qué · qué te haría cambiar la decisión.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q06

**Contexto.** Eres responsable técnico del servicio de pedidos de tu empresa. El equipo de analítica de datos te pide acceso de lectura directo a las tablas de la base de datos del servicio para construir sus informes, porque «el API no tiene lo que necesitamos», y quiere empezar la próxima semana. Tu equipo (4 personas) tiene previsto cambiar el esquema de esas tablas el próximo trimestre. Otras dos aplicaciones ya leen esa base directamente y han provocado dos incidentes este año.

**La decisión.** Escribe el mensaje que enviarías al equipo de analítica rechazando o reformulando la petición.

**Qué debes responder.** El mensaje mismo, dirigido al equipo de analítica, con tus razones y una alternativa concreta.

**Extensión orientativa:** entre 120 y 200 palabras.

---

## Q07

**Contexto.** Un módulo Java de cálculo de comisiones (unas 25 clases) debe modificarse para incluir un nuevo tipo de tarifa. La cobertura de pruebas es del 12 %, casi todas con mocks que repiten la implementación, y no hay pruebas contra la base de datos. El equipo son 3 personas, la entrega es en 8 días hábiles y un error de cálculo en producción obliga a hacer reembolsos manuales. Quien escribió el módulo ya no está en la empresa.

**La decisión.** Decide qué pruebas escribes y qué entregas ahora, y qué difieres.

**Qué debes responder.** Qué haces primero · qué descartas o pospones, y por qué · qué te haría cambiar la decisión.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q08

**Contexto.** Una plataforma de gestión de contratos de servicios debe registrar cada cambio de un contrato para auditoría (quién, qué, cuándo, valor anterior y nuevo), conservarlo siete años y poder consultarlo en menos de 3 segundos. Hoy se generan un millón de cambios al mes, con previsión de cinco millones en dos años. Un requisito legal exige que el registro no pueda alterarse, y el área financiera no admite más de 1.500 USD mensuales de infraestructura adicional. El sistema son 4 servicios que se comunican por HTTP y lo mantienen 5 personas.

**La decisión.** Decide cómo registras y almacenas los cambios, y qué preocupación manda sobre las demás.

**Qué debes responder.** Qué haces primero · qué descartas y por qué · qué te haría cambiar la decisión.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q09

**Contexto.** La generación de documentos PDF de tu producto (facturas y contratos) se hace con plantillas XML transformadas con XSLT, escritas hace ocho años. Solo 2 de las 7 personas del equipo las entienden; cada cambio pequeño tarda entre 3 y 5 días y ha causado 3 errores en producción este año. Reescribirlas con una librería de plantillas en Java costaría unas 6 semanas de 2 personas y retrasaría dos funcionalidades que el patrocinador pidió para el próximo trimestre. Mantener el esquema actual consume aproximadamente 1 semana-persona al mes.

**La decisión.** Escribe la nota de decisión para el patrocinador (director de producto, perfil no técnico).

**Qué debes responder.** El mensaje mismo, dirigido al patrocinador, con tu recomendación, el costo, el riesgo y la decisión que necesitas de su parte.

**Extensión orientativa:** entre 120 y 200 palabras.

---

## Q10

**Contexto.** Una aplicación Angular de 8 años (versión 11, unos 300 componentes y 40 módulos, con lógica de negocio dentro de muchos componentes) ya no recibe parches de seguridad de su versión. Trabajan 8 personas y se publican dos versiones al mes con funcionalidades comprometidas con clientes. Las pruebas automáticas cubren el 25 % del código, varias librerías de terceros dejaron de mantenerse y la dirección acepta, como máximo, una semana de congelación de funcionalidades.

**La decisión.** Decide la estrategia de actualización y el momento a partir del cual ya no hay vuelta atrás.

**Qué debes responder.** Qué haces primero · qué descartas y por qué · qué te haría cambiar la decisión.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q11

**Contexto.** En un sistema con backend Java y frontend Angular, quien lo construyó antes creó una capa de abstracción genérica «por si en el futuro hay varios proveedores de pago»: 14 interfaces, un motor de configuración propio y 3.000 líneas, de las que hoy solo se usa un proveedor. Cada cambio en pagos exige tocar 5 archivos y su suite de pruebas tarda 12 minutos. Quedan 3 sprints hasta una fecha de lanzamiento fija que incluye cambios de pagos, y el negocio dice que un segundo proveedor «quizá» llegue el año próximo. El equipo son 6 personas.

**La decisión.** Decide si simplificas, mantienes o reemplazas esa capa, y cuándo.

**Qué debes responder.** Qué haces primero · qué descartas y por qué · qué te haría cambiar la decisión.

**Extensión orientativa:** unas 200-250 palabras.

---

## Q12

**Contexto.** Una persona de producto, sin formación técnica, debe decidir cómo muestra el estado de un pedido en la pantalla del cliente. Opción A: actualización inmediata (siempre exacta, pero más lenta y con más riesgo de fallo cuando otro sistema falla). Opción B: retraso de hasta 30 segundos (siempre disponible y más rápida, pero a veces muestra el estado anterior). El sistema se compone de varios servicios; hoy recibe unas 2.000 consultas por minuto y unas 12 reclamaciones al mes de clientes por «estado desactualizado».

**La decisión.** Escribe la explicación que enviarías a esa persona para que pueda decidir.

**Qué debes responder.** El mensaje mismo, dirigido a la persona de producto: qué significa cada opción en términos de negocio, qué se gana y qué se pierde, y tu recomendación.

**Extensión orientativa:** entre 120 y 200 palabras.
