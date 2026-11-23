package com.ch.prueba.herramientas.service;

import com.ch.prueba.herramientas.domain.EstadoReserva;
import com.ch.prueba.herramientas.domain.EstadoReservaCalculador;
import com.ch.prueba.herramientas.domain.Herramienta;
import com.ch.prueba.herramientas.domain.Reserva;
import com.ch.prueba.herramientas.exceptions.CancelacionNoPermitidaException;
import com.ch.prueba.herramientas.exceptions.HerramientaNoEncontradaException;
import com.ch.prueba.herramientas.exceptions.LimiteReservasActivasException;
import com.ch.prueba.herramientas.exceptions.ReservaNoEncontradaException;
import com.ch.prueba.herramientas.exceptions.SolapamientoReservaException;
import com.ch.prueba.herramientas.repository.HerramientaRepository;
import com.ch.prueba.herramientas.repository.ReservaRepository;
import com.ch.prueba.herramientas.web.dto.ReservaCreacionDto;
import com.ch.prueba.herramientas.web.dto.ReservaDto;
import com.ch.prueba.herramientas.web.dto.mapper.ReservaMapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/**
 * Servicio de reservas. Implementa las reglas R02, R03, R04 y R05 del
 * doc 02.
 *
 * <p>Operaciones:
 * <ul>
 *   <li>{@link #crear(ReservaCreacionDto)} \u2014 POST /api/reservations</li>
 *   <li>{@link #cancelar(Long)} \u2014 DELETE /api/reservations/{id}</li>
 *   <li>{@link #listarPorCorreo(String)} \u2014 GET /api/reservations?correo=...</li>
 *   <li>{@link #obtener(Long)} \u2014 utilidad interna (404 consistente)</li>
 * </ul></p>
 *
 * <p>El reloj se inyecta como {@link Clock} \u2014 los tests lo sustituyen
 * por {@code Clock.fixed(…)} para controlar "hoy" sin mockear
 * {@link LocalDate#now()}.</p>
 */
@Service
public class ReservationService {

    /** Máximo de reservas activas simultáneas por correo (R04). */
    public static final int MAX_RESERVAS_ACTIVAS = 3;

    private final ReservaRepository reservaRepository;
    private final HerramientaRepository herramientaRepository;
    private final Clock clock;

    public ReservationService(ReservaRepository reservaRepository,
                              HerramientaRepository herramientaRepository,
                              Clock clock) {
        this.reservaRepository = reservaRepository;
        this.herramientaRepository = herramientaRepository;
        this.clock = clock;
    }

    /**
     * Crea una nueva reserva aplicando todas las reglas de negocio en
     * este orden:
     *
     * <ol>
     *   <li><strong>R02</strong> \u2014 rango válido, no en pasado, duración
     *       ≤ {@value ToolService#MAX_DURACION_DIAS} días
     *       ({@link ValidadorRango}).</li>
     *   <li>Existencia de la herramienta \u2014 si no existe,
     *       {@link HerramientaNoEncontradaException}.</li>
     *   <li><strong>R04</strong> \u2014 el correo no debe tener ya
     *       {@value #MAX_RESERVAS_ACTIVAS} reservas activas.</li>
     *   <li><strong>R03</strong> \u2014 no debe solapar con ninguna reserva
     *       activa existente de esa herramienta.</li>
     * </ol>
     *
     * <p>El orden importa: primero validaciones baratas (en memoria) y
     * luego las que tocan BD. R04 va antes que R03 para dar la respuesta
     * más explicativa ("ya tienes 3 reservas") antes que la más genérica
     * ("hay solape") cuando el socio está saturado.</p>
     */
    @Transactional
    public ReservaDto crear(ReservaCreacionDto dto) {
        LocalDate hoy = LocalDate.now(clock);

        // R02
        ValidadorRango.validar(dto.fechaInicio(), dto.fechaFin(), hoy, ToolService.MAX_DURACION_DIAS);

        // Existencia de la herramienta
        Herramienta herramienta = herramientaRepository.findById(dto.herramientaId())
                .orElseThrow(() -> new HerramientaNoEncontradaException(dto.herramientaId()));

        // R04
        long activas = reservaRepository.countActivasByCorreo(dto.correo(), hoy);
        if (activas >= MAX_RESERVAS_ACTIVAS) {
            throw new LimiteReservasActivasException(dto.correo(), (int) activas, MAX_RESERVAS_ACTIVAS);
        }

        // R03
        List<Reserva> solapadas = reservaRepository.findSolapadas(
                herramienta.getId(), dto.fechaInicio(), dto.fechaFin());
        if (!solapadas.isEmpty()) {
            throw new SolapamientoReservaException(
                    herramienta.getId(), dto.fechaInicio(), dto.fechaFin());
        }

        Reserva reserva = new Reserva(herramienta, dto.nombre(), dto.correo(),
                dto.fechaInicio(), dto.fechaFin());
        Reserva persistida = reservaRepository.save(reserva);
        return ReservaMapper.toDto(persistida, hoy);
    }

    /**
     * Cancela lógicamente una reserva (R05). Admitida solo si está en
     * estado {@link EstadoReserva#CONFIRMADA} \u2014 cualquier otro estado
     * lanza {@link CancelacionNoPermitidaException} con el motivo exacto
     * (enum {@code Motivo}: YA_CANCELADA, YA_FINALIZADA, YA_INICIADA).
     */
    @Transactional
    public ReservaDto cancelar(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));

        LocalDate hoy = LocalDate.now(clock);
        EstadoReserva estado = EstadoReservaCalculador.calcular(reserva, hoy);
        switch (estado) {
            case CANCELADA   -> throw new CancelacionNoPermitidaException(
                                    reservaId, CancelacionNoPermitidaException.Motivo.YA_CANCELADA);
            case FINALIZADA  -> throw new CancelacionNoPermitidaException(
                                    reservaId, CancelacionNoPermitidaException.Motivo.YA_FINALIZADA);
            case EN_CURSO    -> throw new CancelacionNoPermitidaException(
                                    reservaId, CancelacionNoPermitidaException.Motivo.YA_INICIADA);
            case CONFIRMADA  -> { /* OK: cancelable */ }
        }

        reserva.cancelar();
        Reserva guardada = reservaRepository.save(reserva);
        return ReservaMapper.toDto(guardada, hoy);
    }

    /**
     * Lista reservas (todas: activas, finalizadas y canceladas) de un
     * socio, ordenadas de más reciente a más antigua por
     * {@code fechaInicio}.
     */
    @Transactional(readOnly = true)
    public List<ReservaDto> listarPorCorreo(String correo) {
        LocalDate hoy = LocalDate.now(clock);
        return ReservaMapper.toDtoList(
                reservaRepository.findByCorreoOrderByFechaInicioDesc(correo), hoy);
    }

    /**
     * Obtiene una reserva por id. Útil para endpoints que lean reservas
     * individuales (no hay uno expuesto todavía).
     */
    @Transactional(readOnly = true)
    public ReservaDto obtener(Long reservaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaNoEncontradaException(reservaId));
        return ReservaMapper.toDto(reserva, LocalDate.now(clock));
    }
}
