package com.ch.prueba.herramientas.web.dto;

import com.ch.prueba.herramientas.domain.Categoria;

/**
 * Representación de salida de una {@code Herramienta} para el API REST.
 *
 * <p>Doc 03 §5.1. Record inmutable: Jackson lo serializa automáticamente
 * y los tests pueden construir instancias con {@code new HerramientaDto(…)}
 * sin builder adicional.</p>
 *
 * <p>Nunca se usa como DTO de <em>entrada</em> (no hay POST/PUT de
 * herramientas en el alcance — el catálogo se carga por seeder).</p>
 */
public record HerramientaDto(Long id, String nombre, Categoria categoria) {
}
