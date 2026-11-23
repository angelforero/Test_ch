package com.ch.prueba.herramientas.domain;

/**
 * Estado de una {@code Reserva}.
 *
 * <p><strong>Valor derivado</strong> — nunca persistido (D14 doc 01). Lo
 * calcula {@link EstadoReservaCalculador} a partir de las fechas de la
 * reserva, el flag {@code cancelada} y el "hoy" del reloj inyectado.</p>
 *
 * <p>Los 4 estados son mutuamente excluyentes y se derivan como:
 * <ul>
 *   <li>{@link #CANCELADA} \u2014 {@code cancelada == true} (prevalece sobre
 *       todos los demás).</li>
 *   <li>{@link #FINALIZADA} \u2014 {@code hoy > fechaFin}.</li>
 *   <li>{@link #EN_CURSO} \u2014 {@code fechaInicio <= hoy <= fechaFin}.</li>
 *   <li>{@link #CONFIRMADA} \u2014 {@code hoy < fechaInicio}.</li>
 * </ul></p>
 */
public enum EstadoReserva {
    CONFIRMADA,
    EN_CURSO,
    FINALIZADA,
    CANCELADA
}
