package com.ch.prueba.herramientas.repository;

import com.ch.prueba.herramientas.domain.Herramienta;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio del catálogo de herramientas.
 *
 * <p>En este commit solo expone los métodos heredados de {@link JpaRepository}
 * ({@code count()}, {@code saveAll()}, {@code findAll()}, {@code findById()}…).
 * Los métodos derivados específicos (p. ej. {@code findByCategoria}) y la
 * query custom de disponibilidad (R09 doc 03 §6) se añadirán cuando los
 * necesite el {@code ToolService} en commits posteriores — YAGNI.</p>
 */
public interface HerramientaRepository extends JpaRepository<Herramienta, Long> {
    // Sin métodos propios todavía.
}
