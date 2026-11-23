package com.ch.prueba.herramientas.exceptions;

/**
 * Se lanza cuando se busca una reserva por {@code id} y no existe. El handler
 * la mapea a HTTP 404 con type {@code not-found}.
 */
public class ReservaNoEncontradaException extends DomainException {

    private final Long reservaId;

    public ReservaNoEncontradaException(Long reservaId) {
        super("Reserva no encontrada: id=" + reservaId);
        this.reservaId = reservaId;
    }

    public Long getReservaId() {
        return reservaId;
    }
}
