package com.ch.prueba.herramientas.exceptions;

/**
 * Se lanza cuando se busca una herramienta por {@code id} y no existe en el
 * catálogo. El handler la mapea a HTTP 404 con type {@code not-found}.
 */
public class HerramientaNoEncontradaException extends DomainException {

    private final Long herramientaId;

    public HerramientaNoEncontradaException(Long herramientaId) {
        super("Herramienta no encontrada: id=" + herramientaId);
        this.herramientaId = herramientaId;
    }

    public Long getHerramientaId() {
        return herramientaId;
    }
}
