package com.ch.prueba.herramientas.domain;

/**
 * Catálogo cerrado de categorías de herramientas.
 *
 * <p>Doc 01 §6 — enumerado fijo (no extensible en caliente). Si en el futuro
 * se necesitara ampliar, sería un cambio de código + migración de datos, no
 * una entrada de catálogo en BD. Esto simplifica validación y filtrado.</p>
 *
 * <p>Persistencia: se almacena como {@code VARCHAR(20)} usando
 * {@code @Enumerated(EnumType.STRING)} en {@link Herramienta#getCategoria()},
 * nunca como ordinal — así renombrar o reordenar valores no corrompe datos
 * existentes.</p>
 */
public enum Categoria {
    PERFORACION,
    CORTE,
    LIJADO,
    MEDICION,
    ESCALERAS,
    JARDIN,
    OTROS
}
