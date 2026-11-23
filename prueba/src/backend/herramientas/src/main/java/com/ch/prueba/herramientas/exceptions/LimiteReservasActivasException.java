package com.ch.prueba.herramientas.exceptions;

/**
 * Se lanza cuando un socio (identificado por correo) ya tiene el máximo de
 * reservas activas permitidas y trata de crear otra (R04: máximo 3 activas
 * por correo). El handler la mapea a HTTP 409 con type
 * {@code active-reservation-limit}.
 */
public class LimiteReservasActivasException extends DomainException {

    private final String correo;
    private final int activasActuales;
    private final int maximo;

    public LimiteReservasActivasException(String correo, int activasActuales, int maximo) {
        super("Correo '" + correo + "' ya tiene " + activasActuales
                + " reservas activas (máximo permitido: " + maximo + ")");
        this.correo = correo;
        this.activasActuales = activasActuales;
        this.maximo = maximo;
    }

    public String getCorreo() {
        return correo;
    }

    public int getActivasActuales() {
        return activasActuales;
    }

    public int getMaximo() {
        return maximo;
    }
}
