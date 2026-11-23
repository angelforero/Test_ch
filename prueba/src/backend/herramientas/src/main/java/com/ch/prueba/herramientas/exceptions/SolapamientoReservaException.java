package com.ch.prueba.herramientas.exceptions;

import java.time.LocalDate;

/**
 * Se lanza cuando se intenta crear una reserva cuyo rango de fechas se solapa
 * con otra reserva activa (CONFIRMADA o EN_CURSO) para la misma herramienta
 * (R03). El handler la mapea a HTTP 409 con type {@code overlap-conflict}.
 */
public class SolapamientoReservaException extends DomainException {

    private final Long herramientaId;
    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;

    public SolapamientoReservaException(Long herramientaId, LocalDate fechaInicio, LocalDate fechaFin) {
        super("Solapamiento de reserva para herramienta=" + herramientaId
                + " en rango [" + fechaInicio + ", " + fechaFin + "]");
        this.herramientaId = herramientaId;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public Long getHerramientaId() {
        return herramientaId;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }
}
