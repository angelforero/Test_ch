package com.ch.prueba.herramientas.web.dto;

import com.ch.prueba.herramientas.domain.EstadoReserva;

import java.time.LocalDate;

/**
 * Representación de salida de una {@code Reserva} en el API REST.
 *
 * <p>Doc 03 §5.2. Incluye:
 * <ul>
 *   <li>{@code nombreHerramienta} \u2014 para que el frontend no tenga que
 *       hacer un segundo GET al catálogo (ergonomía).</li>
 *   <li>{@code estado} \u2014 enum derivado por
 *       {@link com.ch.prueba.herramientas.domain.EstadoReservaCalculador}
 *       al momento de serializar. Nunca persistido.</li>
 *   <li>{@code cancelada} \u2014 flag primitivo tal como vive en BD, por si
 *       el frontend quiere distinguir "CANCELADA por flag" de
 *       "FINALIZADA por tiempo" sin re-derivar.</li>
 * </ul></p>
 */
public record ReservaDto(
        Long id,
        Long herramientaId,
        String nombreHerramienta,
        String nombre,
        String correo,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        EstadoReserva estado,
        boolean cancelada
) {
}
