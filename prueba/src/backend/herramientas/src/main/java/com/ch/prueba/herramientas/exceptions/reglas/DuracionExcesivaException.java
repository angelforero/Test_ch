package com.ch.prueba.herramientas.exceptions.reglas;

/**
 * Se lanza cuando la duración solicitada de una reserva supera el máximo
 * permitido (R02: duración = fin − inicio + 1, con rango inclusivo en ambos
 * extremos, debe ser ≤ 7 días).
 */
public class DuracionExcesivaException extends ReglaValidacionException {

    private final int duracionDias;
    private final int maxDias;

    public DuracionExcesivaException(int duracionDias, int maxDias) {
        super("Duración " + duracionDias + " días supera el máximo permitido de " + maxDias);
        this.duracionDias = duracionDias;
        this.maxDias = maxDias;
    }

    public int getDuracionDias() {
        return duracionDias;
    }

    public int getMaxDias() {
        return maxDias;
    }
}
