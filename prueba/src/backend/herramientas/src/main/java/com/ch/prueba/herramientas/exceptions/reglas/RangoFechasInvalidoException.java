package com.ch.prueba.herramientas.exceptions.reglas;

import java.time.LocalDate;

/**
 * Se lanza cuando la fecha de fin es anterior a la fecha de inicio en una
 * reserva (R02: el rango {@code [fechaInicio, fechaFin]} debe ser válido
 * con fin ≥ inicio).
 */
public class RangoFechasInvalidoException extends ReglaValidacionException {

    private final LocalDate fechaInicio;
    private final LocalDate fechaFin;

    public RangoFechasInvalidoException(LocalDate fechaInicio, LocalDate fechaFin) {
        super("Rango inválido: fechaFin " + fechaFin + " es anterior a fechaInicio " + fechaInicio);
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }
}
