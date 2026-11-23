package com.ch.prueba.herramientas.exceptions.reglas;

import java.time.LocalDate;

/**
 * Se lanza cuando la fecha de inicio de una reserva es anterior a hoy
 * (R02: no se pueden hacer reservas en el pasado).
 */
public class FechaEnPasadoException extends ReglaValidacionException {

    private final LocalDate fechaInicio;
    private final LocalDate hoy;

    public FechaEnPasadoException(LocalDate fechaInicio, LocalDate hoy) {
        super("Fecha de inicio " + fechaInicio + " es anterior a hoy " + hoy);
        this.fechaInicio = fechaInicio;
        this.hoy = hoy;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getHoy() {
        return hoy;
    }
}
