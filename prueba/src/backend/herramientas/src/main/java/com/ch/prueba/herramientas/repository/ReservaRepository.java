package com.ch.prueba.herramientas.repository;

import com.ch.prueba.herramientas.domain.Reserva;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * Repositorio de reservas. 3 operaciones custom más las de
 * {@link JpaRepository}:
 *
 * <ul>
 *   <li>{@link #findSolapadas} \u2014 detecta conflicto R03 antes de crear
 *       una nueva reserva.</li>
 *   <li>{@link #countActivasByCorreo} \u2014 cuenta las activas de un socio
 *       para hacer cumplir R04 (máximo 3 por correo).</li>
 *   <li>{@link #findByCorreoOrderByFechaInicioDesc} \u2014 listado para el
 *       endpoint GET /api/reservations?correo=...</li>
 * </ul>
 *
 * <p>Las 2 queries se apoyan en los índices compuestos de
 * {@code reservas (herramienta_id, fecha_inicio, fecha_fin)} y
 * {@code reservas (correo)} del commit c796c83.</p>
 */
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    /**
     * Reservas <strong>activas</strong> (no canceladas) de la herramienta
     * dada que solapan con el rango {@code [fechaInicio, fechaFin]}
     * (inclusivo). Solape = {@code r.fechaInicio <= fechaFin AND
     * r.fechaFin >= fechaInicio}.
     *
     * <p>Nota de concurrencia (R03 doc 02): esta query SELECT por sí
     * sola no bloquea. La protección frente a creación concurrente queda
     * a nivel de transacción + constraint (D16). Para esta prueba basta
     * con detectar y rechazar en el caso común secuencial.</p>
     */
    @Query("""
            SELECT r FROM Reserva r
            WHERE r.herramienta.id = :herramientaId
              AND r.cancelada = false
              AND r.fechaInicio <= :fechaFin
              AND r.fechaFin   >= :fechaInicio
            """)
    List<Reserva> findSolapadas(@Param("herramientaId") Long herramientaId,
                                @Param("fechaInicio") LocalDate fechaInicio,
                                @Param("fechaFin") LocalDate fechaFin);

    /**
     * Cuenta reservas <strong>activas</strong> de un socio para aplicar
     * R04. "Activa" aquí = no cancelada y {@code fechaFin >= hoy}
     * (estados CONFIRMADA o EN_CURSO; las FINALIZADAS no cuentan).
     */
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.correo = :correo
              AND r.cancelada = false
              AND r.fechaFin >= :hoy
            """)
    long countActivasByCorreo(@Param("correo") String correo,
                              @Param("hoy") LocalDate hoy);

    /**
     * Listado completo (activas, finalizadas y canceladas) de un socio
     * en orden cronológico descendente para que las más recientes/
     * futuras aparezcan primero.
     */
    List<Reserva> findByCorreoOrderByFechaInicioDesc(String correo);
}
