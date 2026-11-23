package com.ch.prueba.herramientas.domain;

import java.time.LocalDate;

/**
 * Deriva el {@link EstadoReserva} actual de una {@link Reserva} a partir
 * del "hoy" recibido como parámetro.
 *
 * <p>Lógica pura, sin dependencias de Spring ni de ningún reloj estático
 * \u2014 el "hoy" llega como argumento para que los tests puedan controlarlo
 * sin mockear {@link LocalDate#now()}.</p>
 *
 * <p>Vive en {@code domain/} porque es parte del modelo de negocio, no
 * de la infraestructura. Mantenerla fuera de la entidad JPA evita
 * ensuciarla con lógica temporal.</p>
 */
public final class EstadoReservaCalculador {

    private EstadoReservaCalculador() {
        // Utilidad: no instanciable.
    }

    /**
     * Calcula el estado de una reserva en el instante indicado por
     * {@code hoy}. Los 4 estados son mutuamente excluyentes y
     * priorizan en este orden:
     *
     * <ol>
     *   <li>{@link EstadoReserva#CANCELADA} si {@code r.cancelada} \u2014
     *       incluso si las fechas ya pasaron, una reserva cancelada se
     *       muestra como CANCELADA, no como FINALIZADA.</li>
     *   <li>{@link EstadoReserva#FINALIZADA} si {@code hoy > fechaFin}.</li>
     *   <li>{@link EstadoReserva#EN_CURSO} si {@code hoy} está dentro
     *       del rango inclusivo {@code [fechaInicio, fechaFin]}.</li>
     *   <li>{@link EstadoReserva#CONFIRMADA} en cualquier otro caso
     *       (es decir, {@code hoy < fechaInicio}).</li>
     * </ol>
     */
    public static EstadoReserva calcular(Reserva reserva, LocalDate hoy) {
        if (reserva.isCancelada()) {
            return EstadoReserva.CANCELADA;
        }
        if (hoy.isAfter(reserva.getFechaFin())) {
            return EstadoReserva.FINALIZADA;
        }
        if (!hoy.isBefore(reserva.getFechaInicio())) {
            // hoy >= fechaInicio y (ya sabemos) hoy <= fechaFin
            return EstadoReserva.EN_CURSO;
        }
        return EstadoReserva.CONFIRMADA;
    }
}
