package com.ch.prueba.herramientas.exceptions;

/**
 * Se lanza cuando se intenta cancelar una reserva que ya no admite
 * cancelación (R05):
 * <ul>
 *   <li>{@link Motivo#YA_INICIADA}: la fecha de inicio ya pasó.</li>
 *   <li>{@link Motivo#YA_FINALIZADA}: la fecha de fin ya pasó.</li>
 *   <li>{@link Motivo#YA_CANCELADA}: la reserva ya está marcada como cancelada.</li>
 * </ul>
 * El handler la mapea a HTTP 409 con type {@code cancellation-not-allowed}.
 *
 * <p>El enum interno {@link Motivo} fuerza al handler a cubrir los 3 casos
 * exhaustivamente (sin magic strings).</p>
 */
public class CancelacionNoPermitidaException extends DomainException {

    /** Razones por las que no se puede cancelar una reserva. */
    public enum Motivo {
        YA_INICIADA,
        YA_FINALIZADA,
        YA_CANCELADA
    }

    private final Long reservaId;
    private final Motivo motivo;

    public CancelacionNoPermitidaException(Long reservaId, Motivo motivo) {
        super("No se puede cancelar reserva id=" + reservaId + ": " + motivo);
        this.reservaId = reservaId;
        this.motivo = motivo;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Motivo getMotivo() {
        return motivo;
    }
}
