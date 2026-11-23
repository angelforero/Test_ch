package com.ch.prueba.herramientas.repository;

import com.ch.prueba.herramientas.domain.Categoria;
import com.ch.prueba.herramientas.domain.Herramienta;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio del catálogo de herramientas.
 *
 * <p>Exposición mínima requerida por R01 (doc 02):
 * <ul>
 *   <li>Listado completo ordenado por nombre (CA-R01.1).</li>
 *   <li>Filtro por categoría (CA-R01.5).</li>
 *   <li>Consulta de disponibilidad por rango de fechas (CA-R01.6/7),
 *       también combinable con filtro por categoría.</li>
 * </ul></p>
 */
public interface HerramientaRepository extends JpaRepository<Herramienta, Long> {

    List<Herramienta> findAllByOrderByNombreAsc();

    List<Herramienta> findByCategoriaOrderByNombreAsc(Categoria categoria);

    /**
     * Herramientas sin reservas activas que solapen el rango
     * {@code [fechaInicio, fechaFin]} (inclusivo en ambos extremos).
     *
     * <p>Diseño de la query (R09, doc 03 §6):
     * <ul>
     *   <li><strong>Una sola</strong> consulta con {@code NOT EXISTS} \u2014
     *       evita N+1 y permite al optimizador apoyarse en los índices de
     *       {@code reservas (herramienta_id, fecha_inicio, fecha_fin)}
     *       del commit c796c83.</li>
     *   <li>"Activa" = {@code cancelada = false}. Los estados derivados
     *       (CONFIRMADA / EN_CURSO / FINALIZADA) no existen a nivel de
     *       BD; solo el flag {@code cancelada} se persiste. El solape
     *       con reservas FINALIZADAS cuenta <em>a propósito</em>: una
     *       herramienta que acaba de ser devuelta no puede volver a
     *       prestarse ese mismo día si ya hay otra reserva en curso
     *       \u2014 lo filtrarán rangos posteriores al pedir fechas futuras.</li>
     *   <li>{@code :categoria IS NULL OR h.categoria = :categoria}
     *       permite pasar {@code null} para desactivar el filtro sin
     *       duplicar la query.</li>
     *   <li>Solape = {@code r.fechaInicio <= :fechaFin AND
     *       r.fechaFin >= :fechaInicio} \u2014 regla clásica de intersección
     *       de intervalos cerrados.</li>
     * </ul></p>
     */
    @Query("""
            SELECT h FROM Herramienta h
            WHERE (:categoria IS NULL OR h.categoria = :categoria)
              AND NOT EXISTS (
                  SELECT 1 FROM Reserva r
                  WHERE r.herramienta = h
                    AND r.cancelada = false
                    AND r.fechaInicio <= :fechaFin
                    AND r.fechaFin   >= :fechaInicio
              )
            ORDER BY h.nombre ASC
            """)
    List<Herramienta> findDisponibles(@Param("categoria") Categoria categoria,
                                      @Param("fechaInicio") LocalDate fechaInicio,
                                      @Param("fechaFin") LocalDate fechaFin);
}
